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
package org.hortonmachine.server.web;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.jobs.Job;
import org.hortonmachine.server.jobs.JobManager;
import org.hortonmachine.server.jobs.JobState;
import org.hortonmachine.server.users.User;
import org.hortonmachine.server.users.UserManager;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.ForbiddenResponse;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

/**
 * The pages of the jobs: the list, the details with the log and the outputs, the kill and the downloads.
 *
 * <p>The users see their own jobs, the administrators all jobs. The pages of jobs that are queued or running
 * reload themselves. A job that can't be seen answers as not existing.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class JobPages {
    /** The jobs shown in the list. */
    public static final int LIST_LIMIT = 200;
    /** The seconds between reloads of the pages of active jobs. */
    public static final int REFRESH_SECONDS = 5;
    /** The page to delete all the finished jobs; out of /jobs/, not to be taken for a job id. */
    public static final String CLEANUP_PATH = "/jobs-cleanup";
    /** The bytes of the end of the log shown in the page of a job. */
    private static final int LOG_TAIL_BYTES = 64 * 1024;

    private final JobManager jobManager;
    private final UserManager userManager;
    private final Templates templates;

    public JobPages( JobManager jobManager, UserManager userManager, Templates templates ) {
        this.jobManager = jobManager;
        this.userManager = userManager;
        this.templates = templates;
    }

    public void register( JavalinConfig config ) {
        config.routes.get("/jobs", this::listPage);
        config.routes.get("/jobs/{id}", this::jobPage);
        config.routes.post("/jobs/{id}/kill", this::kill);
        config.routes.post("/jobs/{id}/delete", this::delete);
        config.routes.get(CLEANUP_PATH, this::cleanupPage);
        config.routes.post(CLEANUP_PATH, this::cleanup);
        config.routes.get("/jobs/{id}/files/<path>", this::download);
        config.routes.head("/jobs/{id}/files/<path>", this::download);
        config.routes.get("/jobs/{id}/zip", this::downloadZip);
    }

    /**
     * @return the job of the path, if the user can see it.
     * @throws NotFoundResponse if it doesn't exist or the user can't see it.
     */
    private Job getJob( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String jobId = ctx.pathParam("id");
        if (!jobId.matches("[A-Za-z0-9_-]+") || !jobManager.canRead(user.id, user.isAdmin(), jobId)) {
            throw new NotFoundResponse("The job doesn't exist.");
        }
        return jobManager.getJob(jobId);
    }

    /**
     * @return the values of a job shown in the lists and in its page, as for the job-state.html template.
     */
    static Model jobModel( Job job ) {
        return Model.of("id", job.id, "module", job.module, "created", Html.formatTime(job.created), //
                "stateName", job.getState().name(), "stateLabel", job.getState().name().toLowerCase(), //
                "isRunning", job.getState() == JobState.RUNNING, "progress", job.getProgress(), "task", job.getTask(), //
                "isActive", !job.getState().isFinished(), "duration", duration(job));
    }

    private static String duration( Job job ) {
        if (job.getStarted() == null) {
            return "";
        }
        long end = job.getFinished() != null ? job.getFinished() : System.currentTimeMillis();
        return Html.formatDuration(end - job.getStarted());
    }

    private void listPage( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        List<Job> jobs = jobManager.listJobs(user.isAdmin() ? null : user.id, LIST_LIMIT);
        Map<String, String> usernames = new HashMap<>();
        if (user.isAdmin()) {
            for( User other : userManager.listUsers() ) {
                usernames.put(other.id, other.username);
            }
        }

        boolean active = false;
        boolean finished = false;
        List<Model> jobModels = new ArrayList<>();
        for( Job job : jobs ) {
            active |= !job.getState().isFinished();
            finished |= job.getState().isFinished();
            jobModels.add(jobModel(job).with("username",
                    job.userId != null ? usernames.getOrDefault(job.userId, "(deleted)") : ""));
        }
        Model model = Model.of("message", Auth.takeFlash(ctx), "hasFinished", finished, "cleanupPath", CLEANUP_PATH, //
                "jobs", jobModels, "hasJobs", !jobs.isEmpty(), "showUsers", user.isAdmin(), //
                "isLimited", jobs.size() == LIST_LIMIT, "limit", LIST_LIMIT);
        ctx.html(templates.page(ctx, "jobs.html", "Jobs", model, active ? REFRESH_SECONDS : null));
    }

    private void jobPage( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        String jobPath = "/jobs/" + job.id;
        Model model = jobModel(job).with("started", Html.formatTime(job.getStarted()))
                .with("finished", Html.formatTime(job.getFinished())).with("jobError", job.getMessage());

        List<Model> parameters = new ArrayList<>();
        for( Map.Entry<String, String> entry : job.parameters.entrySet() ) {
            parameters.add(Model.of("name", entry.getKey(), "value", entry.getValue()));
        }
        model.with("parameters", parameters);

        // the outputs are in the data once the job succeeded, before in the folder of the job
        String ownFolder = WorkspacePaths.JOBS + "/" + job.id + "/";
        String dataFolder = WorkspacePaths.DATA + "/";
        List<Model> outputs = new ArrayList<>();
        for( Map.Entry<String, String> entry : job.getOutputs().entrySet() ) {
            String value = entry.getValue();
            String href = "";
            if (value.startsWith(ownFolder)) {
                href = jobPath + "/files/" + Html.urlPath(WorkspacePaths.filePath(value.substring(ownFolder.length())));
            } else if (value.startsWith(dataFolder)) {
                href = "/uploads/files/" + Html.urlPath(WorkspacePaths.filePath(value.substring(dataFolder.length())));
            }
            outputs.add(Model.of("name", entry.getKey(), "value", value, "isFile", !href.isEmpty(), "href", href));
        }
        model.with("outputs", outputs).with("hasOutputs", !outputs.isEmpty()).with("outputFolder", job.outputFolder);

        Path root = jobManager.getJobFolder(job.id).toPath();
        List<Model> files = new ArrayList<>();
        for( Path file : listFiles(root.toFile()) ) {
            long size;
            try {
                size = Files.size(file);
            } catch (IOException e) {
                // a temporary file of the running job, gone meanwhile
                continue;
            }
            String relative = root.relativize(file).toString().replace('\\', '/');
            files.add(Model.of("path", relative, "href", jobPath + "/files/" + Html.urlPath(relative), "size", Html.formatSize(size)));
        }
        model.with("files", files).with("hasFiles", !files.isEmpty());

        File log = new File(root.toFile(), JobManager.LOG_FILE);
        model.with("hasLog", log.isFile()).with("logCut", log.length() > LOG_TAIL_BYTES)
                .with("logHref", jobPath + "/files/" + JobManager.LOG_FILE).with("log", log.isFile() ? readTail(log) : "");
        ctx.html(templates.page(ctx, "job.html", job.module, Model.of("job", model),
                job.getState().isFinished() ? null : REFRESH_SECONDS));
    }

    private static List<Path> listFiles( File folder ) throws IOException {
        List<Path> files = new ArrayList<>();
        if (folder.isDirectory()) {
            try (Stream<Path> walk = Files.walk(folder.toPath())) {
                walk.filter(Files::isRegularFile).filter(path -> !path.getFileName().toString().equals(JobManager.PARAMS_FILE))
                        .sorted().forEach(files::add);
            }
        }
        return files;
    }

    private static String readTail( File file ) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            long start = Math.max(0, raf.length() - LOG_TAIL_BYTES);
            byte[] bytes = new byte[(int) (raf.length() - start)];
            raf.seek(start);
            raf.readFully(bytes);
            String text = new String(bytes, StandardCharsets.UTF_8);
            if (start > 0) {
                // from the first whole line
                int newline = text.indexOf('\n');
                text = newline >= 0 ? text.substring(newline + 1) : text;
            }
            return text;
        }
    }

    private void kill( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        jobManager.kill(job.id);
        String back = ctx.formParam("back");
        ctx.redirect(back != null && back.matches("/jobs(/[A-Za-z0-9_-]+)?|/admin/status") ? back : "/jobs");
    }

    private void delete( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        try {
            jobManager.delete(job.id);
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.CONFLICT).html(templates.messagePage(ctx, job.module, "The job can't be deleted: " + e.getMessage(),
                    "/jobs/" + job.id, "Back to the job"));
            return;
        }
        Auth.flash(ctx, "The job " + job.module + " of " + Html.formatTime(job.created) + " was deleted.");
        ctx.redirect("/jobs");
    }

    /**
     * @return <code>true</code> if the cleanup is of the jobs of all users.
     * @throws ForbiddenResponse if it is asked by a user who is not an administrator.
     */
    private static boolean isCleanupOfAll( Context ctx, String all ) {
        if (all == null) {
            return false;
        }
        if (!Auth.user(ctx).isAdmin()) {
            throw new ForbiddenResponse("Only for administrators.");
        }
        return true;
    }

    private void cleanupPage( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        boolean all = isCleanupOfAll(ctx, ctx.queryParam("all"));
        int count = jobManager.listFinishedJobs(all ? null : user.id).size();
        Model model = Model.of("count", count, "hasJobs", count > 0, "whose", all ? "all users" : "yours", "all", all, //
                "cleanupPath", CLEANUP_PATH, "cancelHref", all ? "/admin/status" : "/jobs");
        ctx.html(templates.page(ctx, "jobs-cleanup.html", "Delete the finished jobs", model));
    }

    private void cleanup( Context ctx ) throws Exception {
        boolean all = isCleanupOfAll(ctx, ctx.formParam("all"));
        int count = jobManager.deleteFinished(all ? null : Auth.user(ctx).id);
        Auth.flash(ctx, count + " finished job(s) deleted.");
        ctx.redirect("/jobs");
    }

    private void download( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        Path file = resolveJobFile(jobManager, job.id, ctx.pathParam("path"));
        if (file == null) {
            throw new NotFoundResponse("The file doesn't exist.");
        }
        Downloads.send(ctx, file, file.getFileName().toString().equals(JobManager.LOG_FILE));
    }

    /**
     * Find a file of a job that can be downloaded: in its folder, not the file of its parameters.
     *
     * @param relative the path in the folder of the job.
     * @return the file, or <code>null</code> if it is not one.
     */
    static Path resolveJobFile( JobManager jobManager, String jobId, String relative ) {
        Path root = jobManager.getJobFolder(jobId).toPath().toAbsolutePath().normalize();
        String clean = relative.replace('\\', '/');
        Path file = root.resolve(clean).normalize();
        if (List.of(clean.split("/")).contains("..") || !file.startsWith(root) || !Files.isRegularFile(file)
                || file.getFileName().toString().equals(JobManager.PARAMS_FILE)) {
            return null;
        }
        return file;
    }

    private void downloadZip( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        Path root = jobManager.getJobFolder(job.id).toPath();
        List<Path> files = listFiles(root.toFile());
        String name = job.module.toLowerCase() + "-" + job.id.substring(0, 8) + ".zip";
        ctx.contentType("application/zip");
        ctx.header("Content-Disposition", Downloads.contentDisposition(name, true));
        try (ZipOutputStream zip = new ZipOutputStream(ctx.outputStream())) {
            for( Path file : files ) {
                zip.putNextEntry(new ZipEntry(root.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }
}
