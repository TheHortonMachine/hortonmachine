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
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hortonmachine.cli.CliProgressMonitor;
import org.hortonmachine.cli.HmCli;
import org.hortonmachine.cli.ModuleDescriptor;
import org.hortonmachine.cli.ModuleDescriptor.Parameter;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMParameterKind;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.db.ServerDb;
import org.hortonmachine.server.files.FileOrigin;
import org.hortonmachine.server.files.FileOrigins;
import org.hortonmachine.server.users.User;
import org.hortonmachine.server.web.Html;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Runs the jobs, each in its own process of the command line ({@link HmCli}), at most a given number at the same time.
 *
 * <p>Each job has a folder with the parameters file given to the command line and the log of its output. The module
 * writes its outputs in the {@link WorkspacePaths#getJobOutputsFolder(String) outputs folder of the job}; when it
 * succeeded, they are moved in the output folder of the job, in the data, with their {@link FileOrigin origin}: a failed
 * or killed job leaves nothing in the data. The files of the output folder are replaced only if the job is asked to.
 *
 * <p>The progress, the outputs and the errors are read from the lines of the command line output.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class JobManager implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(JobManager.class.getName());

    public static final String PARAMS_FILE = "params.json";
    public static final String LOG_FILE = "job.log";
    /** The java arguments file with the classpath of the jobs, in the jobs folder. */
    public static final String ARGS_FILE = "hm-cli.args";
    /** The name of the output folder among the errors of the values of a job. */
    public static final String OUTPUT_FOLDER = "outputFolder";

    /** The seconds a killed job has to end before it is forced. */
    private static final int KILL_GRACE_SECONDS = 10;

    private static final String ERROR_PREFIX = "Error: ";

    private final ServerDb db;
    private final WorkspacePaths paths;
    private final FileOrigins origins;
    private final File jobsFolder;
    // changed by the settings page while running
    private volatile String jobMemory;
    private volatile int ttlDays;
    private final ThreadPoolExecutor executor;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final Map<String, ActiveJob> activeJobs = new ConcurrentHashMap<>();
    private volatile JobListener listener;
    private volatile Consumer<String> outputsListener;

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
     * @param origins the origins of the files, recorded for the outputs.
     * @param slots the number of jobs running at the same time.
     * @param jobMemory the maximum memory of each job, as for the -Xmx option of java.
     * @param ttlDays the days the finished jobs are kept, 0 to keep them forever; their outputs in the data stay.
     */
    public JobManager( ServerDb db, WorkspacePaths paths, FileOrigins origins, int slots, String jobMemory, int ttlDays ) {
        this.db = db;
        this.paths = paths;
        this.origins = origins;
        this.jobsFolder = paths.getJobsFolder();
        this.jobMemory = jobMemory;
        this.ttlDays = ttlDays;
        executor = new ThreadPoolExecutor(slots, slots, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>());
    }

    /**
     * @param listener told of the successful ends of the jobs, or <code>null</code>.
     */
    public void setListener( JobListener listener ) {
        this.listener = listener;
    }

    /**
     * @param listener told of the output folder, as path in the workspace, after the outputs of a job were moved there,
     *          or <code>null</code>.
     */
    public void setOutputsListener( Consumer<String> listener ) {
        this.outputsListener = listener;
    }

    /**
     * Prepare the jobs folder, mark the jobs interrupted by a stop of the server as failed and
     * start the hourly removal of the old jobs.
     */
    public void start() throws Exception {
        if (!jobsFolder.isDirectory() && !jobsFolder.mkdirs()) {
            throw new IllegalStateException("The jobs folder can't be created: " + jobsFolder.getAbsolutePath());
        }
        JavaCommand.writeArgsFile(new File(jobsFolder, ARGS_FILE));

        db.failUnfinishedJobs("The server stopped while the job was queued or running.", System.currentTimeMillis());
        // also without days to keep the jobs, since they can be set later
        scheduler.scheduleWithFixedDelay(this::removeOldJobs, 0, 1, TimeUnit.HOURS);
    }

    /**
     * Queue a job, with its outputs in the default output folder, never replacing files.
     *
     * @see #submit(String, boolean, String, Map, String, boolean)
     */
    public Job submit( String userId, boolean isAdmin, String moduleName, Map<String, String> values ) throws Exception {
        return submit(userId, isAdmin, moduleName, values, null, false);
    }

    /**
     * Queue a job.
     *
     * @param userId the user launching it, or <code>null</code>; its area is the one named by its username.
     * @param isAdmin if the user is an administrator, who can read and write all the areas.
     * @param moduleName the name of the module.
     * @param values the values of the parameters by name, as for {@link ModuleDescriptor#check(Map)},
     *          with the paths in the workspace as described in {@link WorkspacePaths}.
     * @param outputFolder the folder of the data where the outputs go, as path in the workspace, or <code>null</code> for
     *          the {@link WorkspacePaths#defaultOutputFolder(String, String) default one}.
     * @param overwrite <code>true</code> to replace the files of the output folder with the names of the outputs.
     * @return the queued job.
     * @throws JobValidationException if a value is not valid, or, as {@link JobValidationException#isConflict()
     *         conflict}, if an output would replace a file without overwrite; the error of the output folder is
     *         under {@link #OUTPUT_FOLDER}.
     * @throws IllegalArgumentException if the module doesn't exist.
     */
    public synchronized Job submit( String userId, boolean isAdmin, String moduleName, Map<String, String> values,
            String outputFolder, boolean overwrite ) throws Exception {
        Class< ? extends HMModel> moduleClass = ModuleDescriptor.getAvailableModules().get(moduleName);
        if (moduleClass == null) {
            throw new IllegalArgumentException("Unknown module: " + moduleName);
        }
        ModuleDescriptor module = ModuleDescriptor.of(moduleClass);
        String jobId = UUID.randomUUID().toString();
        File jobFolder = getJobFolder(jobId);
        String ownArea = areaOfUser(userId);

        // the paths of the user become real paths
        Map<String, String> resolved = new LinkedHashMap<>();
        Map<String, String> errors = new LinkedHashMap<>();
        for( Map.Entry<String, String> entry : values.entrySet() ) {
            Parameter parameter = module.getParameter(entry.getKey());
            String value = entry.getValue();
            if (parameter != null && parameter.isPath() && !value.isBlank()) {
                try {
                    value = paths.resolve(parameter, value, ownArea, isAdmin, jobId);
                } catch (IllegalArgumentException e) {
                    errors.put(entry.getKey(), e.getMessage());
                }
            }
            resolved.put(entry.getKey(), value);
        }

        // the outputs go next to the first input, in the area of the user, unless told otherwise
        String folder = null;
        if (outputFolder != null && !outputFolder.isBlank()) {
            try {
                folder = paths.checkOutputFolder(outputFolder, ownArea, isAdmin);
            } catch (IllegalArgumentException e) {
                errors.put(OUTPUT_FOLDER, e.getMessage());
            }
        } else {
            folder = WorkspacePaths.defaultOutputFolder(firstInput(module, values), ownArea);
        }
        Map<String, String> conflicts = new LinkedHashMap<>();
        if (folder != null) {
            for( Parameter parameter : module.getParameters() ) {
                String value = values.get(parameter.name);
                if (parameter.kind == HMParameterKind.OUTPUT && parameter.isPath() && value != null && !value.isBlank()
                        && !errors.containsKey(parameter.name)) {
                    String target = folder + "/" + WorkspacePaths.filePath(value.trim().replace('\\', '/'));
                    File targetFile = paths.toFile(target);
                    if (targetFile.isDirectory()) {
                        errors.put(parameter.name, "There is a folder with the name of the output: " + target);
                    } else if (targetFile.isFile() && !overwrite) {
                        conflicts.put(parameter.name, existingMessage(target, targetFile));
                    }
                }
            }
        }
        if (errors.isEmpty() && !conflicts.isEmpty()) {
            throw new JobValidationException(conflicts, true);
        }
        errors.putAll(conflicts);
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

        Job job = new Job(jobId, userId, moduleName, values, System.currentTimeMillis(), folder, overwrite);
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
     * @return the area of a user, its name; the id itself if it is not a user; <code>null</code> without user.
     */
    private String areaOfUser( String userId ) throws Exception {
        if (userId == null) {
            return null;
        }
        User user = db.getUser(userId);
        return user != null ? user.username : userId;
    }

    /**
     * @return the value of the first input path of a module that is given, in the order of the parameters, or
     *         <code>null</code>.
     */
    private static String firstInput( ModuleDescriptor module, Map<String, String> values ) {
        for( Parameter parameter : module.getParameters() ) {
            String value = values.get(parameter.name);
            if (parameter.kind != HMParameterKind.OUTPUT && parameter.isPath() && value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    /**
     * @return the message telling that an output would replace a file, with where the file comes from.
     */
    private String existingMessage( String target, File file ) {
        String what;
        try {
            FileOrigin origin = origins.get(target);
            if (origin == null) {
                what = "put in the data";
            } else if (origin.origin.equals(FileOrigin.PROCESSED)) {
                what = "written by " + origin.module;
            } else {
                what = "uploaded";
            }
            what += " " + Html.formatTime(origin != null ? origin.created : file.lastModified());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The origin of " + target + " can't be read.", e);
            what = "existing";
        }
        return target + " exists (" + what + "): it is replaced only with overwrite.";
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
            List<String> command = javaCommand();
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
            if (state == JobState.DONE && listener != null) {
                afterRun(activeJob, jobFolder);
                synchronized (activeJob) {
                    if (activeJob.killed) {
                        state = JobState.KILLED;
                    }
                }
            }
            String message = null;
            if (state == JobState.DONE) {
                try {
                    publishOutputs(job);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "The outputs of the job " + job.id + " can't be moved.", e);
                    state = JobState.FAILED;
                    message = e.getMessage();
                }
            }
            finish(activeJob, state, message);
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

    /**
     * Let the listener work on the outputs of a successful job, as the job, writing its problems to the log of the job.
     */
    private void afterRun( ActiveJob activeJob, File jobFolder ) {
        Job job = activeJob.job;
        long endTime = System.currentTimeMillis();
        try {
            listener.afterRun(job, jobFolder, endTime, task -> {
                job.setTask(task);
                job.setProgress(0);
                try {
                    db.updateJob(job);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "The task of the job " + job.id + " can't be saved.", e);
                }
            });
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The end of the job " + job.id + " had problems.", e);
            // the log is a file of the job: appended as the lines of the module, which are not text of the server
            try (Writer log = Files.newBufferedWriter(new File(jobFolder, LOG_FILE).toPath(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
                log.write(e.getMessage() + "\n");
            } catch (Exception logException) {
                LOGGER.log(Level.WARNING, "The log of the job " + job.id + " can't be written.", logException);
            }
        }
        job.setTask(null);
    }

    /**
     * Move the outputs of a successful job from its folder to its output folder, recording their origin, and tell the
     * outputs listener. Either all are moved, or none.
     *
     * @throws Exception if they can't be moved, as when a file appeared meanwhile and the job doesn't replace files:
     *         the outputs stay in the folder of the job.
     */
    private void publishOutputs( Job job ) throws Exception {
        File staging = paths.getJobOutputsFolder(job.id);
        if (job.outputFolder == null || !staging.isDirectory()) {
            return;
        }
        Path stagingPath = staging.toPath();
        List<Path> files;
        try (Stream<Path> walk = Files.walk(stagingPath)) {
            files = walk.filter(Files::isRegularFile).sorted().collect(Collectors.toList());
        }
        File target = paths.toFile(job.outputFolder);
        for( Path file : files ) {
            String relative = stagingPath.relativize(file).toString().replace('\\', '/');
            File destination = new File(target, relative);
            if (destination.isDirectory() || (destination.exists() && !job.overwrite)) {
                throw new IllegalStateException(job.outputFolder + "/" + relative
                        + " appeared while the job ran: the outputs are kept in the job, run it again with overwrite.");
            }
        }

        long now = System.currentTimeMillis();
        for( Path file : files ) {
            String relative = stagingPath.relativize(file).toString().replace('\\', '/');
            origins.record(FileOrigin.processed(job.outputFolder + "/" + relative, job.userId, now, job.id, job.module,
                    job.parameters));
            Path destination = target.toPath().resolve(relative);
            Files.createDirectories(destination.getParent());
            try {
                Files.move(file, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                Files.move(file, destination, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        FileUtilities.deleteFileOrDir(staging);

        // the users see where the outputs are now
        String stagingPrefix = WorkspacePaths.JOBS + "/" + job.id + "/" + WorkspacePaths.JOB_OUTPUTS + "/";
        Map<String, String> outputs = new LinkedHashMap<>();
        for( Map.Entry<String, String> entry : job.getOutputs().entrySet() ) {
            String value = entry.getValue();
            outputs.put(entry.getKey(),
                    value.startsWith(stagingPrefix) ? job.outputFolder + "/" + value.substring(stagingPrefix.length()) : value);
        }
        job.setOutputs(outputs);

        Consumer<String> listener = outputsListener;
        if (listener != null) {
            listener.accept(job.outputFolder);
        }
    }

    /**
     * @return the start of the command of a job process, see {@link JavaCommand}.
     */
    private List<String> javaCommand() {
        return JavaCommand.build(new File(jobsFolder, ARGS_FILE), jobMemory);
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
