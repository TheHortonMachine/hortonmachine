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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hortonmachine.cli.CliProgressMonitor;
import org.hortonmachine.cli.HmCli;
import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.files.FileUtilities;
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
    private final File jobsFolder;
    private final String jobMemory;
    private final int ttlDays;
    private final ExecutorService executor;
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
     * @param jobsFolder the folder of the job folders, created if missing.
     * @param slots the number of jobs running at the same time.
     * @param jobMemory the maximum memory of each job, as for the -Xmx option of java.
     * @param ttlDays the days the finished jobs are kept, 0 to keep them forever.
     */
    public JobManager( ServerDb db, File jobsFolder, int slots, String jobMemory, int ttlDays ) {
        this.db = db;
        this.jobsFolder = jobsFolder;
        this.jobMemory = jobMemory;
        this.ttlDays = ttlDays;
        executor = Executors.newFixedThreadPool(slots);
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
        if (ttlDays > 0) {
            scheduler.scheduleWithFixedDelay(this::removeOldJobs, 0, 1, TimeUnit.HOURS);
        }
    }

    /**
     * Queue a job.
     *
     * @param userId the user launching it, or <code>null</code>.
     * @param moduleName the name of the module.
     * @param values the values of the parameters by name, as for {@link ModuleDescriptor#check(Map)}.
     * @return the queued job.
     * @throws IllegalArgumentException if the module doesn't exist or a value is not valid.
     */
    public Job submit( String userId, String moduleName, Map<String, String> values ) throws Exception {
        Class< ? extends HMModel> moduleClass = ModuleDescriptor.getAvailableModules().get(moduleName);
        if (moduleClass == null) {
            throw new IllegalArgumentException("Unknown module: " + moduleName);
        }
        Map<String, String> errors = ModuleDescriptor.of(moduleClass).check(values);
        if (!errors.isEmpty()) {
            throw new IllegalArgumentException(String.join(" ", errors.values()));
        }

        Job job = new Job(UUID.randomUUID().toString(), userId, moduleName, values, System.currentTimeMillis());
        File jobFolder = getJobFolder(job.id);
        if (!jobFolder.mkdirs()) {
            throw new IllegalStateException("The job folder can't be created: " + jobFolder.getAbsolutePath());
        }
        new ObjectMapper().writeValue(new File(jobFolder, PARAMS_FILE), values);
        db.insertJob(job);

        ActiveJob activeJob = new ActiveJob(job);
        activeJobs.put(job.id, activeJob);
        synchronized (activeJob) {
            activeJob.future = executor.submit(() -> run(activeJob));
        }
        return job;
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
     * @return the folder of a job.
     */
    public File getJobFolder( String jobId ) {
        return new File(jobsFolder, jobId);
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
                        outputs.put(output.substring(0, equalsIndex), output.substring(equalsIndex + 3));
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
     * Remove the jobs finished since longer than the days to keep them, with their folders.
     */
    private void removeOldJobs() {
        try {
            long limit = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(ttlDays);
            for( Job job : db.listJobsFinishedBefore(limit) ) {
                FileUtilities.deleteFileOrDir(getJobFolder(job.id));
                db.deleteJob(job.id);
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
