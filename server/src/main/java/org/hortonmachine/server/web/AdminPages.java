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
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

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
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class AdminPages {
    private final ServerConfig config;
    private final WorkspacePaths paths;
    private final UserManager users;
    private final JobManager jobManager;
    private final UploadManager uploads;
    private final UrlSigner signer;
    private final long startTime;
    private final Consumer<ServerConfig> applyLiveSettings;
    private final Templates templates;

    /**
     * @param config the configuration the server runs with.
     * @param startTime the time the server started.
     * @param applyLiveSettings applies the settings of the settings page to the running server.
     */
    public AdminPages( ServerConfig config, WorkspacePaths paths, UserManager users, JobManager jobManager, UploadManager uploads,
            UrlSigner signer, long startTime, Consumer<ServerConfig> applyLiveSettings, Templates templates ) {
        this.config = config;
        this.paths = paths;
        this.users = users;
        this.jobManager = jobManager;
        this.uploads = uploads;
        this.signer = signer;
        this.startTime = startTime;
        this.applyLiveSettings = applyLiveSettings;
        this.templates = templates;
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
        config.routes.post("/admin/settings/signing-key", this::renewSigningKey);
    }

    private void renewSigningKey( Context ctx ) throws Exception {
        signer.renewKey();
        Auth.flash(ctx, "The signing key was renewed: the signed links given until now don't work anymore.");
        ctx.redirect("/admin/settings");
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

    private String settingsHtml( Context ctx, Map<Setting, String> values, Map<Setting, String> errors, String message ) {
        List<Model> settings = new ArrayList<>();
        for( Setting setting : Setting.values() ) {
            settings.add(Model.of("id", "s-" + setting.option, "option", setting.option, "label", setting.label, //
                    "description", setting.description, "value", values.get(setting), //
                    "locked", config.fromCommandLine.contains(setting), "commandLineValue", config.get(setting), //
                    "hasError", errors.containsKey(setting), "settingError", errors.get(setting)));
        }
        Model model = Model.of("message", message, "hasErrors", !errors.isEmpty(), "settingsFile", ServerConfig.SETTINGS_FILE, //
                "signingKeyFile", UrlSigner.KEY_FILE, //
                "settings", settings, "port", String.valueOf(config.port), "workspace", paths.getWorkspace().getAbsolutePath(), //
                "database", config.db == null ? "SQLite in the workspace" : config.db + (config.dbUser != null ? ", user " + config.dbUser : ""), //
                "passwordVariable", ServerConfig.DB_PASSWORD_ENV, //
                "passwordState", System.getenv(ServerConfig.DB_PASSWORD_ENV) != null ? "set" : "not set");
        return templates.page(ctx, "admin-settings.html", "Settings", model);
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
        List<Model> userModels = new ArrayList<>();
        for( User user : users.listUsers() ) {
            userModels.add(Model.of("id", user.id, "username", user.username, "created", Html.formatTime(user.created), //
                    "disabled", user.disabled, "isSelf", user.id.equals(admin.id), "roleLabel", user.role.name().toLowerCase(), //
                    "roles", roleModels(user.role), "filesSize", Html.formatSize(uploads.getUsedBytes(user.username)), //
                    "jobsSize", Html.formatSize(jobManager.getUsedBytes(user.id))));
        }
        Model model = Model.of("message", message, "error", error, "users", userModels, "newUserRoles", roleModels(Role.USER), //
                "minLength", UserManager.MIN_PASSWORD_LENGTH);
        return templates.page(ctx, "admin-users.html", "Users", model);
    }

    /**
     * @return the roles for a choice, with the given one selected.
     */
    private static List<Model> roleModels( Role selected ) {
        List<Model> roles = new ArrayList<>();
        for( Role role : Role.values() ) {
            roles.add(Model.of("name", role.name(), "label", role.name().toLowerCase(), "selected", role == selected));
        }
        return roles;
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
            uploads.deleteAll(user.username);
            return "The user " + user.username + " was deleted.";
        });
    }

    private void statusPage( Context ctx ) throws Exception {
        Runtime runtime = Runtime.getRuntime();
        File workspace = paths.getWorkspace();
        Model model = Model.of("started", Html.formatTime(startTime), //
                "uptime", Html.formatDuration(System.currentTimeMillis() - startTime), //
                "workspace", workspace.getAbsolutePath(), //
                "database", config.db == null ? "SQLite in the workspace" : "PostgreSQL " + config.db, //
                "memoryUsed", Html.formatSize(runtime.totalMemory() - runtime.freeMemory()), //
                "memoryMax", Html.formatSize(runtime.maxMemory()), "processors", runtime.availableProcessors(), //
                "slots", jobManager.getSlots(), "jobMemory", config.jobMemory, //
                "jobsKept", config.jobTtlDays > 0 ? config.jobTtlDays + " days" : "for ever", //
                "maxUploadMb", config.maxUploadMb, //
                "quota", config.userQuotaMb > 0 ? config.userQuotaMb + " MB per user" : "no limit per user", //
                "diskFree", Html.formatSize(workspace.getUsableSpace()), "diskTotal", Html.formatSize(workspace.getTotalSpace()), //
                "dataSize", Html.formatSize(WorkspacePaths.folderSize(paths.getDataFolder().toPath())), //
                "sharedSize", Html.formatSize(WorkspacePaths.folderSize(paths.getAreaFolder(WorkspacePaths.SHARED).toPath())), //
                "jobsSize", Html.formatSize(WorkspacePaths.folderSize(paths.getJobsFolder().toPath())), //
                "cleanupPath", JobPages.CLEANUP_PATH);

        Map<JobState, Integer> counts = jobManager.countJobsByState();
        List<Model> stateCounts = new ArrayList<>();
        for( JobState state : JobState.values() ) {
            stateCounts.add(Model.of("state", state.name(), "label", state.name().toLowerCase(), "count", counts.getOrDefault(state, 0)));
        }
        model.with("stateCounts", stateCounts);

        Map<String, String> usernames = new HashMap<>();
        for( User user : users.listUsers() ) {
            usernames.put(user.id, user.username);
        }
        List<Model> active = new ArrayList<>();
        for( Job job : jobManager.listUnfinishedJobs() ) {
            active.add(JobPages.jobModel(job).with("username",
                    job.userId != null ? usernames.getOrDefault(job.userId, "(deleted)") : ""));
        }
        model.with("active", active).with("hasActive", !active.isEmpty());
        ctx.html(templates.page(ctx, "admin-status.html", "Status", model));
    }
}
