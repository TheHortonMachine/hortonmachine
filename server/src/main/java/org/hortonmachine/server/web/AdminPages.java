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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.hortonmachine.Version;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.ServerConfig.Setting;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.jobs.Job;
import org.hortonmachine.server.jobs.JobManager;
import org.hortonmachine.server.jobs.JobState;
import org.hortonmachine.server.uploads.UploadManager;
import org.hortonmachine.server.users.Role;
import org.hortonmachine.server.users.User;
import org.hortonmachine.server.users.UserManager;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.NotFoundResponse;

/**
 * The pages of the administrators: the users and the status of the server.
 *
 * <p>{@link Auth} lets only the administrators in. An administrator doesn't change or delete the own
 * user here, so as not to lock out by mistake: another administrator does, and the password is changed
 * in the account page.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class AdminPages {
    private final ServerConfig config;
    private final WorkspacePaths paths;
    private final UserManager users;
    private final JobManager jobManager;
    private final UploadManager uploads;
    private final long startTime;
    private final Consumer<ServerConfig> applyLiveSettings;

    /**
     * @param config the configuration the server runs with.
     * @param startTime the time the server started.
     * @param applyLiveSettings applies to the running server the settings that don't need a restart.
     */
    public AdminPages( ServerConfig config, WorkspacePaths paths, UserManager users, JobManager jobManager, UploadManager uploads,
            long startTime, Consumer<ServerConfig> applyLiveSettings ) {
        this.config = config;
        this.paths = paths;
        this.users = users;
        this.jobManager = jobManager;
        this.uploads = uploads;
        this.startTime = startTime;
        this.applyLiveSettings = applyLiveSettings;
    }

    public void register( JavalinConfig config ) {
        config.routes.get("/admin", ctx -> ctx.redirect("/admin/users"));
        config.routes.get("/admin/users", ctx -> ctx.html(usersHtml(ctx, Auth.takeFlash(ctx), null)));
        config.routes.post("/admin/users", this::createUser);
        config.routes.post("/admin/users/{id}", this::updateUser);
        config.routes.post("/admin/users/{id}/password", this::setPassword);
        config.routes.post("/admin/users/{id}/delete", this::deleteUser);
        config.routes.get("/admin/status", this::statusPage);
        config.routes.get("/admin/settings", ctx -> ctx.html(settingsHtml(ctx, currentValues(), Map.of(), Auth.takeFlash(ctx))));
        config.routes.post("/admin/settings", this::saveSettings);
    }

    /**
     * @return the values the server runs with.
     */
    private Map<Setting, String> currentValues() {
        Map<Setting, String> values = new EnumMap<>(Setting.class);
        for( Setting setting : Setting.values() ) {
            values.put(setting, config.get(setting));
        }
        return values;
    }

    private static void appendCommandLineField( StringBuilder body, String label, String value, String help ) {
        body.append("<div class=\"field\"><label>").append(label).append("</label><input type=\"text\" disabled value=\"")
                .append(Html.escape(value)).append("\"><p class=\"help muted\">").append(help).append("</p></div>");
    }

    private String settingsHtml( Context ctx, Map<Setting, String> values, Map<Setting, String> errors, String message ) {
        User admin = Auth.user(ctx);
        String csrfToken = Auth.csrfToken(ctx);
        StringBuilder body = new StringBuilder("<h1>Settings</h1>");
        if (message != null) {
            body.append("<p class=\"ok\">").append(Html.escape(message)).append("</p>");
        }
        if (!errors.isEmpty()) {
            body.append("<p class=\"error\">Nothing was saved: correct the values in red.</p>");
        }
        body.append("<p class=\"muted\">They apply at once and are saved in the ").append(ServerConfig.SETTINGS_FILE)
                .append(" file of the workspace. The options given on the command line win over this page.</p>");
        body.append("<form method=\"post\" action=\"/admin/settings\">").append(Html.csrfField(csrfToken));

        for( Setting setting : Setting.values() ) {
            String id = "s-" + setting.option;
            boolean locked = config.fromCommandLine.contains(setting);
            String value = values.get(setting);
            body.append("<div class=\"field\"><label for=\"").append(id).append("\"");
            if (errors.containsKey(setting)) {
                body.append(" class=\"error\"");
            }
            body.append(">").append(Html.escape(setting.label)).append("</label>");
            body.append("<input type=\"text\" id=\"").append(id).append("\" name=\"").append(setting.option).append("\" value=\"")
                    .append(Html.escape(value)).append("\"").append(locked ? " disabled" : "").append(">");
            if (errors.containsKey(setting)) {
                body.append("<p class=\"help error\">").append(Html.escape(errors.get(setting))).append("</p>");
            }
            body.append("<p class=\"help muted\">").append(Html.escape(setting.description));
            if (locked) {
                body.append(" <b>Given on the command line as --").append(setting.option).append("=")
                        .append(Html.escape(config.get(setting))).append(": remove it there to set it here.</b>");
            }
            body.append("</p></div>");
        }
        body.append("<p><button type=\"submit\">Save</button></p></form>");

        body.append("<fieldset><legend>Only on the command line</legend>");
        body.append("<p class=\"help muted\">They change with a restart of the server with other options.</p>");
        appendCommandLineField(body, "Port", String.valueOf(config.port), "The port to listen on, given with --port.");
        appendCommandLineField(body, "Workspace", paths.getWorkspace().getAbsolutePath(),
                "The folder of the data, the jobs and the uploads, given with --workspace: it holds the settings file.");
        appendCommandLineField(body, "Database",
                config.db == null ? "SQLite in the workspace" : config.db + (config.dbUser != null ? ", user " + config.dbUser : ""),
                "Given with --db=postgresql://host:port/database and --db-user, without them SQLite in the workspace. "
                        + "The password is read from the " + ServerConfig.DB_PASSWORD_ENV
                        + " variable of the environment of the server, which is "
                        + (System.getenv(ServerConfig.DB_PASSWORD_ENV) != null ? "set" : "not set") + ".");
        body.append("</fieldset>");
        return Html.page("Settings", admin, csrfToken, body.toString(), null);
    }

    private synchronized void saveSettings( Context ctx ) throws Exception {
        Map<Setting, String> values = currentValues();
        Map<Setting, String> errors = new EnumMap<>(Setting.class);
        ServerConfig candidate = new ServerConfig();
        for( Setting setting : Setting.values() ) {
            String posted = ctx.formParam(setting.option);
            if (posted != null && !config.fromCommandLine.contains(setting)) {
                values.put(setting, posted.trim());
            }
            try {
                candidate.set(setting, values.get(setting));
            } catch (IllegalArgumentException e) {
                errors.put(setting, e.getMessage());
            }
        }
        if (!errors.isEmpty()) {
            ctx.status(HttpStatus.BAD_REQUEST).html(settingsHtml(ctx, values, errors, null));
            return;
        }

        Map<String, String> toSave = new LinkedHashMap<>();
        for( Setting setting : Setting.values() ) {
            if (!config.fromCommandLine.contains(setting)) {
                toSave.put(setting.option, candidate.get(setting));
            }
        }
        ServerConfig.writeSettingsFile(paths.getWorkspace(), toSave);
        applyLiveSettings.accept(candidate);
        Auth.flash(ctx, "The settings were saved and applied.");
        ctx.redirect("/admin/settings");
    }

    private String usersHtml( Context ctx, String message, String error ) throws Exception {
        User admin = Auth.user(ctx);
        String csrfToken = Auth.csrfToken(ctx);
        StringBuilder body = new StringBuilder("<h1>Users</h1>");
        if (message != null) {
            body.append("<p class=\"ok\">").append(Html.escape(message)).append("</p>");
        }
        if (error != null) {
            body.append("<p class=\"error\">").append(Html.escape(error)).append("</p>");
        }

        body.append("<div class=\"scroll\"><table><tr><th>Name</th><th>Created</th><th>Role and state</th><th>Password</th>")
                .append("<th class=\"num\">Uploads</th><th class=\"num\">Jobs</th><th></th></tr>");
        for( User user : users.listUsers() ) {
            String action = "/admin/users/" + Html.escape(user.id);
            body.append("<tr><td>").append(Html.escape(user.username));
            if (user.disabled) {
                body.append(" <span class=\"error\">disabled</span>");
            }
            body.append("</td><td>").append(Html.formatTime(user.created)).append("</td><td>");
            boolean self = user.id.equals(admin.id);
            if (self) {
                body.append(user.role.name().toLowerCase()).append(" <span class=\"muted\">(you)</span></td><td>")
                        .append("<a href=\"/account\">change</a></td>");
            } else {
                body.append("<form class=\"inline\" method=\"post\" action=\"").append(action).append("\">").append(Html.csrfField(csrfToken));
                body.append("<select name=\"role\" aria-label=\"Role\">");
                for( Role role : Role.values() ) {
                    body.append("<option value=\"").append(role.name()).append("\"").append(role == user.role ? " selected" : "")
                            .append(">").append(role.name().toLowerCase()).append("</option>");
                }
                body.append("</select> <label class=\"inline\"><input type=\"checkbox\" name=\"disabled\"")
                        .append(user.disabled ? " checked" : "").append("> disabled</label> ");
                body.append("<button class=\"link\" type=\"submit\">Save</button></form></td><td>");
                body.append("<form class=\"inline\" method=\"post\" action=\"").append(action).append("/password\">")
                        .append(Html.csrfField(csrfToken));
                body.append("<input type=\"password\" name=\"password\" autocomplete=\"new-password\" aria-label=\"New password\" ")
                        .append("placeholder=\"new password\" required minlength=\"").append(UserManager.MIN_PASSWORD_LENGTH).append("\"> ");
                body.append("<button class=\"link\" type=\"submit\">Set</button></form></td>");
            }
            body.append("<td class=\"num\">").append(Html.formatSize(uploads.getUsedBytes(user.id))).append("</td>");
            body.append("<td class=\"num\">").append(Html.formatSize(jobManager.getUsedBytes(user.id))).append("</td><td>");
            if (!self) {
                body.append("<form class=\"inline\" method=\"post\" action=\"").append(action).append("/delete\">")
                        .append(Html.csrfField(csrfToken));
                body.append("<button class=\"link\" type=\"submit\">Delete</button></form>");
            }
            body.append("</td></tr>");
        }
        body.append("</table></div>");
        body.append("<p class=\"muted\">Deleting a user deletes the uploads too; the jobs stay until they expire.</p>");

        body.append("<fieldset><legend>New user</legend>");
        body.append("<form method=\"post\" action=\"/admin/users\">").append(Html.csrfField(csrfToken));
        body.append("<label for=\"username\">User name</label>");
        body.append("<input type=\"text\" id=\"username\" name=\"username\" autocomplete=\"off\" required>");
        body.append("<p class=\"help muted\">3 to 64 letters, digits, dots, dashes or underscores.</p>");
        body.append("<label for=\"password\">Password</label>");
        body.append("<input type=\"password\" id=\"password\" name=\"password\" autocomplete=\"new-password\" required minlength=\"")
                .append(UserManager.MIN_PASSWORD_LENGTH).append("\">");
        body.append("<label for=\"role\">Role</label><select id=\"role\" name=\"role\">");
        for( Role role : Role.values() ) {
            body.append("<option value=\"").append(role.name()).append("\"").append(role == Role.USER ? " selected" : "").append(">")
                    .append(role.name().toLowerCase()).append("</option>");
        }
        body.append("</select><p><button type=\"submit\">Create</button></p></form></fieldset>");
        return Html.page("Users", admin, csrfToken, body.toString(), null);
    }

    /**
     * @return the user of the path, not the logged in one.
     * @throws NotFoundResponse if it doesn't exist.
     * @throws IllegalArgumentException if it is the logged in user.
     */
    private User getOtherUser( Context ctx ) throws Exception {
        String userId = ctx.pathParam("id");
        User user = userId.matches("[A-Za-z0-9_-]+") ? users.getUser(userId) : null;
        if (user == null) {
            throw new NotFoundResponse("The user doesn't exist.");
        }
        if (user.id.equals(Auth.user(ctx).id)) {
            throw new IllegalArgumentException("Your own user is changed by another administrator.");
        }
        return user;
    }

    private static Role toRole( String role ) {
        try {
            return Role.valueOf(role);
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("Not a valid role: " + role);
        }
    }

    /**
     * Run a change and go back to the users page, with the message of the change or of its error.
     */
    private void change( Context ctx, Change change ) throws Exception {
        String message;
        try {
            message = change.run();
        } catch (IllegalArgumentException e) {
            ctx.status(HttpStatus.BAD_REQUEST).html(usersHtml(ctx, null, e.getMessage()));
            return;
        }
        Auth.flash(ctx, message);
        ctx.redirect("/admin/users");
    }

    private interface Change {
        /**
         * @return the message of the done change.
         */
        String run() throws Exception;
    }

    private void createUser( Context ctx ) throws Exception {
        change(ctx, () -> {
            String password = ctx.formParam("password");
            User user = users.createUser(ctx.formParam("username"), password != null ? password.toCharArray() : new char[0],
                    toRole(ctx.formParam("role")));
            return "The user " + user.username + " was created.";
        });
    }

    private void updateUser( Context ctx ) throws Exception {
        change(ctx, () -> {
            User user = getOtherUser(ctx);
            User updated = users.updateUser(user.id, toRole(ctx.formParam("role")), ctx.formParam("disabled") != null);
            return "The user " + updated.username + " was saved.";
        });
    }

    private void setPassword( Context ctx ) throws Exception {
        change(ctx, () -> {
            User user = getOtherUser(ctx);
            String password = ctx.formParam("password");
            users.changePassword(user.id, password != null ? password.toCharArray() : new char[0]);
            return "The password of " + user.username + " was set.";
        });
    }

    private void deleteUser( Context ctx ) throws Exception {
        change(ctx, () -> {
            User user = getOtherUser(ctx);
            users.deleteUser(user.id);
            uploads.deleteAll(user.id);
            return "The user " + user.username + " was deleted.";
        });
    }

    private void statusPage( Context ctx ) throws Exception {
        User admin = Auth.user(ctx);
        String csrfToken = Auth.csrfToken(ctx);
        long now = System.currentTimeMillis();
        StringBuilder body = new StringBuilder("<h1>Status</h1>");

        body.append("<h2>Server</h2><table>");
        appendRow(body, "Version", Html.escape(Version.getVersion()));
        appendRow(body, "Started", Html.formatTime(startTime) + " <span class=\"muted\">(" + Html.formatDuration(now - startTime)
                + " ago)</span>");
        appendRow(body, "Workspace", Html.escape(paths.getWorkspace().getAbsolutePath()));
        appendRow(body, "Database", config.db == null ? "SQLite in the workspace" : "PostgreSQL " + Html.escape(config.db));
        Runtime runtime = Runtime.getRuntime();
        appendRow(body, "Memory", Html.formatSize(runtime.totalMemory() - runtime.freeMemory()) + " used of "
                + Html.formatSize(runtime.maxMemory()) + ", " + runtime.availableProcessors() + " processors");
        appendRow(body, "Jobs", jobManager.getSlots() + " at the same time, " + Html.escape(config.jobMemory) + " of memory each, kept "
                + (config.jobTtlDays > 0 ? config.jobTtlDays + " days" : "for ever"));
        appendRow(body, "Uploads", "at most " + config.maxUploadMb + " MB at a time, "
                + (config.userQuotaMb > 0 ? config.userQuotaMb + " MB per user" : "no limit per user"));
        body.append("</table>");

        Map<JobState, Integer> counts = jobManager.countJobsByState();
        body.append("<h2>Jobs</h2><p>");
        for( JobState state : JobState.values() ) {
            body.append("<span class=\"state-").append(state.name()).append("\">").append(state.name().toLowerCase()).append("</span> ")
                    .append(counts.getOrDefault(state, 0)).append(" &nbsp; ");
        }
        body.append("</p>");
        List<Job> active = jobManager.listUnfinishedJobs();
        if (active.isEmpty()) {
            body.append("<p class=\"muted\">No job is queued or running.</p>");
        } else {
            Map<String, String> usernames = new HashMap<>();
            for( User user : users.listUsers() ) {
                usernames.put(user.id, user.username);
            }
            body.append("<div class=\"scroll\"><table><tr><th>Created</th><th>Module</th><th>User</th><th>State</th><th></th></tr>");
            for( Job job : active ) {
                body.append("<tr><td>").append(Html.formatTime(job.created)).append("</td><td><a href=\"/jobs/").append(Html.escape(job.id))
                        .append("\">").append(Html.escape(job.module)).append("</a></td><td>")
                        .append(Html.escape(job.userId != null ? usernames.getOrDefault(job.userId, "(deleted)") : ""))
                        .append("</td><td class=\"state-").append(job.getState().name()).append("\">")
                        .append(job.getState().name().toLowerCase());
                if (job.getState() == JobState.RUNNING) {
                    body.append(" ").append(job.getProgress()).append("%");
                }
                body.append("</td><td><form class=\"inline\" method=\"post\" action=\"/jobs/").append(Html.escape(job.id))
                        .append("/kill\">").append(Html.csrfField(csrfToken))
                        .append("<input type=\"hidden\" name=\"back\" value=\"/admin/status\">")
                        .append("<button class=\"link\" type=\"submit\">Kill</button></form></td></tr>");
            }
            body.append("</table></div>");
        }

        File workspace = paths.getWorkspace();
        body.append("<h2>Disk</h2><table>");
        appendRow(body, "Free", Html.formatSize(workspace.getUsableSpace()) + " of " + Html.formatSize(workspace.getTotalSpace()));
        appendRow(body, "Data", Html.formatSize(WorkspacePaths.folderSize(paths.getDataFolder().toPath())));
        appendRow(body, "Uploads", Html.formatSize(WorkspacePaths.folderSize(paths.getAllUploadsFolder().toPath())));
        appendRow(body, "Jobs", Html.formatSize(WorkspacePaths.folderSize(paths.getJobsFolder().toPath()))
                + " &nbsp; <a href=\"" + JobPages.CLEANUP_PATH + "?all=1\">delete the finished jobs of all users</a>");
        body.append("</table><p class=\"muted\">The disk used by each user is in the <a href=\"/admin/users\">users</a> page.</p>");
        ctx.html(Html.page("Status", admin, csrfToken, body.toString(), null));
    }

    private static void appendRow( StringBuilder body, String name, String valueHtml ) {
        body.append("<tr><th>").append(name).append("</th><td>").append(valueHtml).append("</td></tr>");
    }
}
