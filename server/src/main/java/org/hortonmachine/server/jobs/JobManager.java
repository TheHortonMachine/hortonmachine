/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.server.jobs;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hortonmachine.cli.CliProgressMonitor;
import org.hortonmachine.cli.HmCli;
import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMParameterKind;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.db.ServerDb;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Runs the jobs, each in its own process of the command line ({@link HmCli}), at most a given number at the same time.
 *
 * <p>Each job has a folder with the parameters file given to the command line, the log of its output
 * and, if the paths say so, the outputs. The progress, the outputs and the errors are read
 * from the lines of the command line output.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class JobManager implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(JobManager.class.getName());

    public static final String PARAMS_FILE = "params.json";
    public static final String LOG_FILE = "job.log";
    /** The java arguments file with the classpath of the jobs, in the jobs folder. */
    public static final String ARGS_FILE = "hm-cli.args";

    /** The seconds a killed job has to end before it is forced. */
    private static final int KILL_GRACE_SECONDS = 10;

    private static final String ERROR_PREFIX = "Error: ";

    private final ServerDb db;
    private final WorkspacePaths paths;
    private final File jobsFolder;
    // changed by the settings page while running
    private volatile String jobMemory;
    private volatile int ttlDays;
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ActiveJob> activeJobs = new ConcurrentHashMap<>();

    /**
     * A job queued or running.
     */
    private static class ActiveJob {
        final Job job;
        Future< ? > future;
        Process process;
        boolean killed;

        ActiveJob( Job job ) {
            this.job = job;
        }
    }

    /**
     * @param db the database.
     * @param paths the folders of the workspace, the jobs folder is created if missing.
     * @param slots the number of jobs running at the same time.
     * @param jobMemory the maximum memory of each job, as for the -Xmx option of java.
     * @param ttlDays the days the finished jobs are kept, 0 to keep them forever.
     */
    public JobManager( ServerDb db, WorkspacePaths paths, int slots, String jobMemory, int ttlDays ) {
        this.db = db;
        this.paths = paths;
        this.jobsFolder = paths.getJobsFolder();
        this.jobMemory = jobMemory;
        this.ttlDays = ttlDays;
        executor = new ThreadPoolExecutor(slots, slots, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /**
     * Prepare the jobs folder, mark the jobs interrupted by a stop of the server as failed and
     * start the hourly removal of the old jobs.
     */
    public void start() throws Exception {
        if (!jobsFolder.isDirectory() && !jobsFolder.mkdirs()) {
            throw new IllegalStateException("The jobs folder can't be created: " + jobsFolder.getAbsolutePath());
        }
        // in a file, since the classpath can be longer than the command line allows on Windows
        String classpath = System.getProperty("java.class.path").replace("\\", "\\\\");
        FileUtilities.writeFile("-cp\n\"" + classpath + "\"\n", new File(jobsFolder, ARGS_FILE));

        db.failUnfinishedJobs("The server stopped while the job was queued or running.", System.currentTimeMillis());
        // also without days to keep the jobs, since they can be set later
        scheduler.scheduleWithFixedDelay(this::removeOldJobs, 0, 1, TimeUnit.HOURS);
    }

    /**
     * Queue a job.
     *
     * @param userId the user launching it, or <code>null</code>.
     * @param isAdmin if the user is an administrator, who can use the outputs of the jobs of all users.
     * @param moduleName the name of the module.
     * @param values the values of the parameters by name, as for {@link ModuleDescriptor#check(Map)},
     *          with the paths in the workspace as described in {@link WorkspacePaths}.
     * @return the queued job.
     * @throws JobValidationException if a value is not valid.
     * @throws IllegalArgumentException if the module doesn't exist.
     */
    public synchronized Job submit( String userId, boolean isAdmin, String moduleName, Map<String, String> values ) throws Exception {
        Class< ? extends HMModel> moduleClass = ModuleDescriptor.getAvailableModules().get(moduleName);
        if (moduleClass == null) {
            throw new IllegalArgumentException("Unknown module: " + moduleName);
        }
        ModuleDescriptor module = ModuleDescriptor.of(moduleClass);
        String jobId = UUID.randomUUID().toString();
        File jobFolder = getJobFolder(jobId);
        Predicate<String> canReadJob = otherJobId -> canRead(userId, isAdmin, otherJobId);

        // the paths of the user become real paths
        Map<String, String> resolved = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for( Map.Entry<String, String> entry : values.entrySet() ) {
            Parameter parameter = module.getParameter(entry.getKey());
            String value = entry.getValue();
            if (parameter != null && parameter.isPath() && !value.isBlank()) {
                try {
                    value = paths.resolve(parameter, value, userId, jobId, canReadJob);
                } catch (IllegalArgumentException e) {
                    errors.put(entry.getKey(), e.getMessage());
                }
            }
            resolved.put(entry.getKey(), value);
        }
        if (errors.isEmpty()) {
            for( Map.Entry<String, String> entry : resolved.entrySet() ) {
                Parameter parameter = module.getParameter(entry.getKey());
                if (parameter != null && parameter.isPath() && parameter.kind == HMParameterKind.OUTPUT
                        && !entry.getValue().isBlank()) {
                    File parent = new File(entry.getValue()).getParentFile();
                    if (!parent.isDirectory() && !parent.mkdirs()) {
                        throw new IllegalStateException("The job folder can't be created: " + parent);
                    }
                }
            }
            errors.putAll(module.check(resolved));
        }
        if (!errors.isEmpty()) {
            FileUtilities.deleteFileOrDir(jobFolder);
            throw new JobValidationException(errors);
        }

        Job job = new Job(jobId, userId, moduleName, values, System.currentTimeMillis());
        if (!jobFolder.isDirectory() && !jobFolder.mkdirs()) {
            throw new IllegalStateException("The job folder can't be created: " + jobFolder.getAbsolutePath());
        }
        new ObjectMapper().writeValue(new File(jobFolder, PARAMS_FILE), resolved);
        db.insertJob(job);

        ActiveJob activeJob = new ActiveJob(job);
        activeJobs.put(job.id, activeJob);
        synchronized (activeJob) {
            activeJob.future = executor.submit(() -> run(activeJob));
        }
        return job;
    }

    /**
     * Check if a user can see a job and use its outputs: the jobs of the user or, for an administrator, all jobs.
     *
     * @return <code>false</code> also if the job doesn't exist.
     */
    public boolean canRead( String userId, boolean isAdmin, String jobId ) {
        try {
            Job job = getJob(jobId);
            return job != null && (isAdmin || (userId != null && userId.equals(job.userId)));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The job " + jobId + " can't be read.", e);
            return false;
        }
    }

    /**
     * Kill a job: a queued job doesn't run, a running job is stopped.
     *
     * @param jobId the id of the job.
     * @return <code>false</code> if the job is not queued or running.
     */
    public boolean kill( String jobId ) throws Exception {
        ActiveJob activeJob = activeJobs.get(jobId);
        if (activeJob == null) {
            return false;
        }
        synchronized (activeJob) {
            activeJob.killed = true;
            if (activeJob.process != null) {
                destroy(activeJob.process);
                // run() saves the end of the job
                return true;
            }
            if (!activeJob.future.cancel(false)) {
                // run() just started and will see the kill
                return true;
            }
        }
        finish(activeJob, JobState.KILLED, null);
        return true;
    }

    private void destroy( Process process ) {
        process.descendants().forEach(ProcessHandle::destroy);
        process.destroy();
        scheduler.schedule(() -> {
            process.descendants().forEach(ProcessHandle::destroyForcibly);
            process.destroyForcibly();
        }, KILL_GRACE_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Get a job, with its current state.
     *
     * @return the job, or <code>null</code> if it doesn't exist.
     */
    public Job getJob( String jobId ) throws Exception {
        ActiveJob activeJob = activeJobs.get(jobId);
        if (activeJob != null) {
            return activeJob.job;
        }
        return db.getJob(jobId);
    }

    /**
     * Get the jobs, newest first, with their current state.
     *
     * @param userId the user whose jobs to get, or <code>null</code> for the jobs of all users.
     * @param limit the maximum number of jobs.
     */
    public List<Job> listJobs( String userId, int limit ) throws Exception {
        List<Job> jobs = new ArrayList<>();
        for( Job job : db.listJobs(userId, limit) ) {
            ActiveJob activeJob = activeJobs.get(job.id);
            jobs.add(activeJob != null ? activeJob.job : job);
        }
        return jobs;
    }

    /**
     * @return the jobs queued or running, oldest first, with their current state.
     */
    public List<Job> listUnfinishedJobs() throws Exception {
        List<Job> jobs = new ArrayList<>();
        for( Job job : db.listUnfinishedJobs() ) {
            ActiveJob activeJob = activeJobs.get(job.id);
            jobs.add(activeJob != null ? activeJob.job : job);
        }
        return jobs;
    }

    /**
     * @return the number of jobs in each state, as saved: the running jobs can already be finished.
     */
    public Map<JobState, Integer> countJobsByState() throws Exception {
        return db.countJobsByState();
    }

    /**
     * @return the number of jobs running at the same time.
     */
    public int getSlots() {
        return executor.getMaximumPoolSize();
    }

    /**
     * Change the number of jobs running at the same time. With less, the running jobs end normally
     * and the queued wait longer.
     */
    public synchronized void setSlots( int slots ) {
        if (slots > executor.getMaximumPoolSize()) {
            executor.setMaximumPoolSize(slots);
            executor.setCorePoolSize(slots);
        } else {
            executor.setCorePoolSize(slots);
            executor.setMaximumPoolSize(slots);
        }
    }

    /**
     * Change the maximum memory of the jobs started from now on.
     *
     * @param jobMemory the memory, as for the -Xmx option of java.
     */
    public void setJobMemory( String jobMemory ) {
        this.jobMemory = jobMemory;
    }

    /**
     * Change the days the finished jobs are kept, from the next hourly removal on.
     *
     * @param ttlDays the days, 0 to keep them forever.
     */
    public void setTtlDays( int ttlDays ) {
        this.ttlDays = ttlDays;
    }

    /**
     * @return the folder of a job.
     */
    public File getJobFolder( String jobId ) {
        return paths.getJobFolder(jobId);
    }

    private void run( ActiveJob activeJob ) {
        Job job = activeJob.job;
        File jobFolder = getJobFolder(job.id);
        try {
            List<String> command = new ArrayList<>();
            command.add(Paths.get(System.getProperty("java.home"), "bin", "java").toString());
            command.add("-Xmx" + jobMemory);
            command.add("-Xss64m");
            command.add("-Djava.awt.headless=true");
            // from java 18 the default charset, used to read the output, is UTF-8 and the console one may differ
            command.add("-Dstdout.encoding=" + Charset.defaultCharset().name());
            command.add("-Dstderr.encoding=" + Charset.defaultCharset().name());
            copyProperty(command, "java.util.logging.config.file");
            copyProperty(command, "java.library.path");
            command.add("@" + new File(jobsFolder, ARGS_FILE).getAbsolutePath());
            command.add(HmCli.class.getName());
            command.add("run");
            command.add(job.module);
            command.add("--params=" + new File(jobFolder, PARAMS_FILE).getAbsolutePath());

            Process process;
            synchronized (activeJob) {
                if (activeJob.killed) {
                    finish(activeJob, JobState.KILLED, null);
                    return;
                }
                process = new ProcessBuilder(command).directory(jobFolder).redirectErrorStream(true).start();
                activeJob.process = process;
                job.setState(JobState.RUNNING);
                job.setStarted(System.currentTimeMillis());
            }
            db.updateJob(job);

            readOutput(job, process);
            int exitCode = process.waitFor();
            job.setExitCode(exitCode);
            JobState state;
            synchronized (activeJob) {
                state = activeJob.killed ? JobState.KILLED : exitCode == HmCli.EXIT_OK ? JobState.DONE : JobState.FAILED;
            }
            finish(activeJob, state, null);
        } catch (Exception e) {
            synchronized (activeJob) {
                if (activeJob.killed) {
                    // killing the process closes its output while it is read
                    finish(activeJob, JobState.KILLED, null);
                    return;
                }
                if (activeJob.process != null) {
                    destroy(activeJob.process);
                }
            }
            LOGGER.log(Level.WARNING, "The job " + job.id + " failed to run.", e);
            finish(activeJob, JobState.FAILED, "The job failed to run: " + e.getMessage());
        }
    }

    private static void copyProperty( List<String> command, String name ) {
        String value = System.getProperty(name);
        if (value != null && !value.isEmpty()) {
            command.add("-D" + name + "=" + value);
        }
    }

    /**
     * Write the output of the process to the log of the job, reading the progress, outputs and errors.
     */
    private void readOutput( Job job, Process process ) throws Exception {
        File logFile = new File(getJobFolder(job.id), LOG_FILE);
        Charset charset = Charset.defaultCharset();
        Map<String, String> outputs = new LinkedHashMap<>();
        List<String> errors = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), charset));
                Writer log = new BufferedWriter(
                        new OutputStreamWriter(Files.newOutputStream(logFile.toPath()), StandardCharsets.UTF_8))) {
            String line;
            while( (line = reader.readLine()) != null ) {
                log.write(line);
                log.write('\n');
                log.flush();
                if (line.startsWith(CliProgressMonitor.TASK_PREFIX)) {
                    job.setTask(line.substring(CliProgressMonitor.TASK_PREFIX.length()).trim());
                    job.setProgress(0);
                    db.updateJob(job);
                } else if (line.startsWith(CliProgressMonitor.PROGRESS_PREFIX)) {
                    String percentage = line.substring(CliProgressMonitor.PROGRESS_PREFIX.length()).replace("%", "").trim();
                    try {
                        job.setProgress(Integer.parseInt(percentage));
                        db.updateJob(job);
                    } catch (NumberFormatException e) {
                        // a message of the module
                    }
                } else if (line.startsWith(HmCli.OUTPUT_PREFIX)) {
                    String output = line.substring(HmCli.OUTPUT_PREFIX.length());
                    int equalsIndex = output.indexOf(" = ");
                    if (equalsIndex > 0) {
                        // the users see the paths in the workspace, not the real ones
                        String value = output.substring(equalsIndex + 3);
                        String workspacePath = paths.toWorkspacePath(value);
                        outputs.put(output.substring(0, equalsIndex), workspacePath != null ? workspacePath : value);
                        job.setOutputs(outputs);
                    }
                } else if (line.startsWith(ERROR_PREFIX)) {
                    errors.add(line.substring(ERROR_PREFIX.length()));
                    job.setMessage(String.join("\n", errors));
                }
            }
        }
    }

    /**
     * Save the end of a job.
     */
    private void finish( ActiveJob activeJob, JobState state, String message ) {
        Job job = activeJob.job;
        job.setState(state);
        job.setFinished(System.currentTimeMillis());
        if (message != null) {
            job.setMessage(message);
        }
        try {
            db.updateJob(job);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The end of the job " + job.id + " can't be saved.", e);
        }
        activeJobs.remove(job.id);
    }

    /**
     * Delete a finished job with its folder.
     *
     * @param jobId the id of the job.
     * @return <code>false</code> if the job doesn't exist.
     * @throws IllegalArgumentException if the job is queued or running, or a job queued or running
     *         reads its outputs.
     */
    public synchronized boolean delete( String jobId ) throws Exception {
        if (activeJobs.containsKey(jobId)) {
            throw new IllegalArgumentException("The job is queued or running: kill it first.");
        }
        if (db.getJob(jobId) == null) {
            return false;
        }
        String readerId = findActiveReader(jobId);
        if (readerId != null) {
            throw new IllegalArgumentException(
                    "The outputs of the job are inputs of the job " + readerId + ", which is queued or running.");
        }
        File jobFolder = getJobFolder(jobId);
        // the result of deleteFileOrDir is false for the folders it deleted
        FileUtilities.deleteFileOrDir(jobFolder);
        if (jobFolder.exists()) {
            throw new IllegalStateException("The folder of the job can't be deleted: " + jobFolder);
        }
        db.deleteJob(jobId);
        return true;
    }

    /**
     * @param userId the user whose jobs to get, or <code>null</code> for the jobs of all users.
     * @return the jobs not queued or running.
     */
    public List<Job> listFinishedJobs( String userId ) throws Exception {
        return db.listFinishedJobs(userId);
    }

    /**
     * Delete the finished jobs, but those whose outputs are read by a job queued or running.
     *
     * @param userId the user whose jobs to delete, or <code>null</code> for the jobs of all users.
     * @return the number of jobs deleted.
     */
    public synchronized int deleteFinished( String userId ) throws Exception {
        int count = 0;
        for( Job job : db.listFinishedJobs(userId) ) {
            if (!activeJobs.containsKey(job.id) && findActiveReader(job.id) == null && delete(job.id)) {
                count++;
            }
        }
        return count;
    }

    /**
     * @return the id of a job queued or running that has a path into the folder of the given job, or <code>null</code>.
     */
    private String findActiveReader( String jobId ) {
        String folder = WorkspacePaths.JOBS + "/" + jobId;
        for( ActiveJob activeJob : activeJobs.values() ) {
            for( String value : activeJob.job.parameters.values() ) {
                String path = value.trim().replace('\\', '/');
                if (path.equals(folder) || path.startsWith(folder + "/")) {
                    return activeJob.job.id;
                }
            }
        }
        return null;
    }

    /**
     * @return the bytes used by the folders of the jobs of a user.
     */
    public long getUsedBytes( String userId ) throws Exception {
        long size = 0;
        for( Job job : db.listJobs(userId, Integer.MAX_VALUE) ) {
            size += WorkspacePaths.folderSize(getJobFolder(job.id).toPath());
        }
        return size;
    }

    /**
     * Remove the jobs finished since longer than the days to keep them, with their folders.
     * The jobs whose outputs are read by a running job wait for the next time.
     */
    private void removeOldJobs() {
        if (ttlDays <= 0) {
            return;
        }
        try {
            long limit = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(ttlDays);
            for( Job job : db.listJobsFinishedBefore(limit) ) {
                try {
                    delete(job.id);
                } catch (IllegalArgumentException e) {
                    // still in use
                }
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The old jobs can't be removed.", e);
        }
    }

    /**
     * Kill the jobs queued and running and stop.
     */
    @Override
    public void close() throws Exception {
        for( String jobId : new ArrayList<>(activeJobs.keySet()) ) {
            kill(jobId);
        }
        executor.shutdown();
        if (!executor.awaitTermination(KILL_GRACE_SECONDS + 5, TimeUnit.SECONDS)) {
            for( ActiveJob activeJob : activeJobs.values() ) {
                synchronized (activeJob) {
                    if (activeJob.process != null) {
                        activeJob.process.descendants().forEach(ProcessHandle::destroyForcibly);
                        activeJob.process.destroyForcibly();
                    }
                }
            }
        }
        scheduler.shutdownNow();
    }
}
