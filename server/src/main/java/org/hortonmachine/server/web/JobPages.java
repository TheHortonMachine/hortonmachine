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
import java.io.InputStream;
import java.io.OutputStream;
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
    /** The bytes of the end of the log shown in the page of a job. */
    private static final int LOG_TAIL_BYTES = 64 * 1024;

    private final JobManager jobManager;
    private final UserManager userManager;

    public JobPages( JobManager jobManager, UserManager userManager ) {
        this.jobManager = jobManager;
        this.userManager = userManager;
    }

    public void register( JavalinConfig config ) {
        config.routes.get("/jobs", this::listPage);
        config.routes.get("/jobs/{id}", this::jobPage);
        config.routes.post("/jobs/{id}/kill", this::kill);
        config.routes.get("/jobs/{id}/files/<path>", this::download);
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

    private static String refresh() {
        return "<meta http-equiv=\"refresh\" content=\"" + REFRESH_SECONDS + "\">";
    }

    private static String stateHtml( Job job ) {
        StringBuilder sb = new StringBuilder("<span class=\"state-").append(job.getState().name()).append("\">")
                .append(job.getState().name().toLowerCase()).append("</span>");
        if (job.getState() == JobState.RUNNING) {
            sb.append(" ").append(job.getProgress()).append("%");
            if (job.getTask() != null) {
                sb.append(" <span class=\"muted\">").append(Html.escape(job.getTask())).append("</span>");
            }
        }
        return sb.toString();
    }

    private static String duration( Job job ) {
        if (job.getStarted() == null) {
            return "";
        }
        long end = job.getFinished() != null ? job.getFinished() : System.currentTimeMillis();
        return Html.formatDuration(end - job.getStarted());
    }

    private static String killButton( Job job, String csrfToken, String back ) {
        return "<form class=\"inline\" method=\"post\" action=\"/jobs/" + Html.escape(job.id) + "/kill\">" + Html.csrfField(csrfToken)
                + "<input type=\"hidden\" name=\"back\" value=\"" + Html.escape(back) + "\">"
                + "<button type=\"submit\">Kill</button></form>";
    }

    private void listPage( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String csrfToken = Auth.csrfToken(ctx);
        List<Job> jobs = jobManager.listJobs(user.isAdmin() ? null : user.id, LIST_LIMIT);
        Map<String, String> usernames = new HashMap<>();
        if (user.isAdmin()) {
            for( User other : userManager.listUsers() ) {
                usernames.put(other.id, other.username);
            }
        }

        boolean active = false;
        StringBuilder body = new StringBuilder("<h1>Jobs</h1>");
        if (jobs.isEmpty()) {
            body.append("<p class=\"muted\">No jobs yet: start one from the <a href=\"/modules\">modules</a>.</p>");
        } else {
            body.append("<div class=\"scroll\"><table><tr><th>Created</th><th>Module</th>");
            if (user.isAdmin()) {
                body.append("<th>User</th>");
            }
            body.append("<th>State</th><th>Duration</th><th></th></tr>");
            for( Job job : jobs ) {
                body.append("<tr><td>").append(Html.formatTime(job.created)).append("</td>");
                body.append("<td><a href=\"/jobs/").append(Html.escape(job.id)).append("\">").append(Html.escape(job.module))
                        .append("</a></td>");
                if (user.isAdmin()) {
                    String username = job.userId != null ? usernames.getOrDefault(job.userId, "(deleted)") : "";
                    body.append("<td>").append(Html.escape(username)).append("</td>");
                }
                body.append("<td>").append(stateHtml(job)).append("</td><td>").append(duration(job)).append("</td><td>");
                if (!job.getState().isFinished()) {
                    active = true;
                    body.append(killButton(job, csrfToken, "/jobs"));
                }
                body.append("</td></tr>");
            }
            body.append("</table></div>");
            if (jobs.size() == LIST_LIMIT) {
                body.append("<p class=\"muted\">The latest ").append(LIST_LIMIT).append(" jobs.</p>");
            }
        }
        ctx.html(Html.page("Jobs", user, csrfToken, body.toString(), active ? refresh() : null));
    }

    private void jobPage( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        User user = Auth.user(ctx);
        String csrfToken = Auth.csrfToken(ctx);
        String jobPath = "/jobs/" + Html.escape(job.id);

        StringBuilder body = new StringBuilder();
        body.append("<p><a href=\"/jobs\">Jobs</a></p>");
        body.append("<h1>").append(Html.escape(job.module)).append(": ").append(stateHtml(job)).append("</h1>");
        body.append("<div class=\"actions\">");
        if (!job.getState().isFinished()) {
            body.append(killButton(job, csrfToken, "/jobs/" + job.id));
        }
        body.append("<a href=\"/modules/").append(Html.escape(job.module)).append("?from=").append(Html.escape(job.id))
                .append("\">Run again</a>");
        body.append("</div>");

        body.append("<table>");
        appendRow(body, "Created", Html.formatTime(job.created));
        appendRow(body, "Started", Html.formatTime(job.getStarted()));
        appendRow(body, "Finished", Html.formatTime(job.getFinished()));
        appendRow(body, "Duration", duration(job));
        body.append("</table>");
        if (job.getMessage() != null) {
            body.append("<pre class=\"error\">").append(Html.escape(job.getMessage())).append("</pre>");
        }

        body.append("<h2>Parameters</h2><div class=\"scroll\"><table>");
        for( Map.Entry<String, String> entry : job.parameters.entrySet() ) {
            body.append("<tr><th>").append(Html.escape(entry.getKey())).append("</th><td>").append(Html.escape(entry.getValue()))
                    .append("</td></tr>");
        }
        body.append("</table></div>");

        if (!job.getOutputs().isEmpty()) {
            String ownFolder = WorkspacePaths.JOBS + "/" + job.id + "/";
            body.append("<h2>Outputs</h2><div class=\"scroll\"><table>");
            for( Map.Entry<String, String> entry : job.getOutputs().entrySet() ) {
                String value = entry.getValue();
                body.append("<tr><th>").append(Html.escape(entry.getKey())).append("</th><td>");
                if (value.startsWith(ownFolder)) {
                    body.append("<a href=\"").append(jobPath).append("/files/")
                            .append(Html.escape(Html.urlPath(value.substring(ownFolder.length())))).append("\">")
                            .append(Html.escape(value)).append("</a>");
                } else {
                    body.append(Html.escape(value));
                }
                body.append("</td></tr>");
            }
            body.append("</table></div>");
        }

        List<Path> files = listFiles(jobManager.getJobFolder(job.id));
        if (!files.isEmpty()) {
            Path root = jobManager.getJobFolder(job.id).toPath();
            body.append("<h2>Files</h2><p><a href=\"").append(jobPath).append("/zip\">Download all as zip</a></p>");
            body.append("<div class=\"scroll\"><table>");
            for( Path file : files ) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                body.append("<tr><td><a href=\"").append(jobPath).append("/files/").append(Html.escape(Html.urlPath(relative)))
                        .append("\">")
                        .append(Html.escape(relative)).append("</a></td><td>").append(Html.formatSize(Files.size(file)))
                        .append("</td></tr>");
            }
            body.append("</table></div>");
        }

        File log = new File(jobManager.getJobFolder(job.id), JobManager.LOG_FILE);
        if (log.isFile()) {
            body.append("<h2>Log</h2>");
            if (log.length() > LOG_TAIL_BYTES) {
                body.append("<p class=\"muted\">The end of the log: <a href=\"").append(jobPath).append("/files/")
                        .append(JobManager.LOG_FILE).append("\">download it all</a>.</p>");
            }
            body.append("<pre>").append(Html.escape(readTail(log))).append("</pre>");
        }
        ctx.html(Html.page(job.module, user, csrfToken, body.toString(), job.getState().isFinished() ? null : refresh()));
    }

    private static void appendRow( StringBuilder body, String name, String value ) {
        body.append("<tr><th>").append(name).append("</th><td>").append(value).append("</td></tr>");
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
        ctx.redirect(back != null && back.matches("/jobs(/[A-Za-z0-9_-]+)?") ? back : "/jobs");
    }

    private void download( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        Path root = jobManager.getJobFolder(job.id).toPath().toAbsolutePath().normalize();
        String relative = ctx.pathParam("path").replace('\\', '/');
        Path file = root.resolve(relative).normalize();
        if (List.of(relative.split("/")).contains("..") || !file.startsWith(root) || !Files.isRegularFile(file)
                || file.getFileName().toString().equals(JobManager.PARAMS_FILE)) {
            throw new NotFoundResponse("The file doesn't exist.");
        }
        String name = file.getFileName().toString();
        ctx.contentType(name.equals(JobManager.LOG_FILE) ? "text/plain; charset=utf-8" : "application/octet-stream");
        ctx.header("Content-Disposition", contentDisposition(name, !name.equals(JobManager.LOG_FILE)));
        ctx.header("Content-Length", String.valueOf(Files.size(file)));
        try (InputStream input = Files.newInputStream(file); OutputStream output = ctx.outputStream()) {
            input.transferTo(output);
        }
    }

    private void downloadZip( Context ctx ) throws Exception {
        Job job = getJob(ctx);
        Path root = jobManager.getJobFolder(job.id).toPath();
        List<Path> files = listFiles(root.toFile());
        String name = job.module.toLowerCase() + "-" + job.id.substring(0, 8) + ".zip";
        ctx.contentType("application/zip");
        ctx.header("Content-Disposition", contentDisposition(name, true));
        try (ZipOutputStream zip = new ZipOutputStream(ctx.outputStream())) {
            for( Path file : files ) {
                zip.putNextEntry(new ZipEntry(root.relativize(file).toString().replace('\\', '/')));
                Files.copy(file, zip);
                zip.closeEntry();
            }
        }
    }

    private static String contentDisposition( String name, boolean attachment ) {
        // the names are of the workspace, but keep the header safe anyway
        String safe = name.replaceAll("[^A-Za-z0-9._-]", "_");
        return (attachment ? "attachment" : "inline") + "; filename=\"" + safe + "\"";
    }
}
