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
package org.hortonmachine.server;

import java.io.Console;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.hortonmachine.Version;
import org.hortonmachine.server.db.ServerDb;
import org.hortonmachine.server.files.FileOrigin;
import org.hortonmachine.server.files.FileOrigins;
import org.hortonmachine.server.jobs.JobManager;
import org.hortonmachine.server.jobs.CogOutputs;
import org.hortonmachine.server.stac.StacIndexer;
import org.hortonmachine.server.uploads.UploadManager;
import org.hortonmachine.server.users.Role;
import org.hortonmachine.server.users.User;
import org.hortonmachine.server.users.UserManager;
import org.hortonmachine.server.web.AccountPages;
import org.hortonmachine.server.web.AdminPages;
import org.hortonmachine.server.web.Auth;
import org.hortonmachine.server.web.FileSuggestions;
import org.hortonmachine.server.web.JobPages;
import org.hortonmachine.server.web.ModulePages;
import org.hortonmachine.server.web.OgcApi;
import org.hortonmachine.server.web.StacApi;
import org.hortonmachine.server.web.Templates;
import org.hortonmachine.server.web.UploadPages;
import org.hortonmachine.server.web.UrlSigner;

import org.hortonmachine.server.ServerConfig.Setting;

import io.javalin.Javalin;
import io.javalin.config.MultipartConfig;
import io.javalin.config.SizeUnit;

/**
 * The HortonMachine web server: runs the modules for the users of a browser.
 *
 * <p>The options are described in {@link ServerConfig}; the workspace folder holds the data,
 * the database, the jobs and the data of the server.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class HmServer {
    private static final Logger LOGGER = Logger.getLogger(HmServer.class.getName());

    public static final int EXIT_OK = 0;
    public static final int EXIT_FAILURE = 1;
    public static final int EXIT_USAGE = 2;

    private static final long MB = 1024L * 1024L;
    /** The unpacked files of a zip file can be this many times larger than the largest upload. */
    private static final int UNPACKED_FACTOR = 10;

    private final ServerConfig config;
    private ServerDb db;
    private JobManager jobManager;
    private UserManager userManager;
    private UploadManager uploadManager;
    private UploadPages uploadPages;
    private FileSuggestions fileSuggestions;
    private UrlSigner urlSigner;
    private StacIndexer stacIndexer;
    private MultipartConfig multipartConfig;
    private Javalin app;

    public HmServer( ServerConfig config ) {
        this.config = config;
    }

    public static void main( String[] args ) {
        ServerConfig config;
        try {
            config = ServerConfig.fromArguments(args, System.out);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Run 'hm-server --help' for the usage.");
            System.exit(EXIT_USAGE);
            return;
        }
        if (config == null) {
            System.exit(EXIT_OK);
            return;
        }
        if (config.createAdmin != null) {
            char[] password = readAdminPassword();
            System.exit(password != null ? createAdmin(config, password, System.out, System.err) : EXIT_USAGE);
            return;
        }
        HmServer server = new HmServer(config);
        try {
            server.start();
        } catch (Exception e) {
            System.err.println("Error: the server can't start: " + e.getMessage());
            server.stop();
            System.exit(EXIT_FAILURE);
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        System.out.println("HortonMachine server " + Version.getVersion() + " listening on port " + server.getPort()
                + ", workspace " + config.workspace.getAbsolutePath());
        try {
            if (!server.userManager.hasUsers()) {
                System.out.println("There are no users yet: create the first administrator with --create-admin=name.");
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The users can't be read.", e);
        }
    }

    /**
     * Read the password of the administrator to create, from the environment or the console.
     *
     * @return the password, or <code>null</code> if it can't be read or the two typed differ.
     */
    private static char[] readAdminPassword() {
        String fromEnvironment = System.getenv(ServerConfig.ADMIN_PASSWORD_ENV);
        if (fromEnvironment != null && !fromEnvironment.isEmpty()) {
            return fromEnvironment.toCharArray();
        }
        Console console = System.console();
        if (console == null) {
            System.err.println("Error: there is no console to type the password: set the " + ServerConfig.ADMIN_PASSWORD_ENV
                    + " variable.");
            return null;
        }
        char[] password = console.readPassword("Password: ");
        char[] again = console.readPassword("Password again: ");
        if (password == null || !Arrays.equals(password, again)) {
            System.err.println("Error: the passwords differ.");
            return null;
        }
        return password;
    }

    /**
     * Create an administrator in the workspace of the configuration.
     *
     * @return the exit code.
     */
    static int createAdmin( ServerConfig config, char[] password, PrintStream out, PrintStream err ) {
        new WorkspacePaths(config.workspace).createFolders();
        try (ServerDb db = ServerDb.open(config)) {
            User user = new UserManager(db).createUser(config.createAdmin, password, Role.ADMIN);
            out.println("The administrator " + user.username + " was created.");
            return EXIT_OK;
        } catch (IllegalArgumentException e) {
            err.println("Error: " + e.getMessage());
            return EXIT_USAGE;
        } catch (Exception e) {
            err.println("Error: the administrator can't be created: " + e.getMessage());
            return EXIT_FAILURE;
        } finally {
            Arrays.fill(password, '\0');
        }
    }

    /**
     * Start the server.
     *
     * @throws Exception if the workspace or the database can't be opened or the port is not available.
     */
    public void start() throws Exception {
        long startTime = System.currentTimeMillis();
        WorkspacePaths paths = new WorkspacePaths(config.workspace);
        if (!config.workspace.isDirectory() && !config.workspace.mkdirs()) {
            throw new IllegalStateException("The workspace can't be created: " + config.workspace);
        }
        config.applySettingsFile();
        db = ServerDb.open(config);
        // the files of the older layouts go in the areas, named by the users
        Map<String, String> usernames = new LinkedHashMap<>();
        for( User user : db.listUsers() ) {
            usernames.put(user.id, user.username);
        }
        Map<String, String> migratedUsers = paths.migrateLegacyLayout(usernames);
        for( Map.Entry<String, String> user : usernames.entrySet() ) {
            if (!user.getKey().equals(user.getValue())) {
                db.renameAreaPaths(WorkspacePaths.DATA + "/" + user.getKey(), WorkspacePaths.DATA + "/" + user.getValue());
            }
        }
        paths.createFolders();
        FileOrigins fileOrigins = new FileOrigins(db, paths);
        recordMigratedUploads(paths, fileOrigins, migratedUsers);
        jobManager = new JobManager(db, paths, fileOrigins, config.slots, config.jobMemory, config.jobTtlDays);
        jobManager.setListener(new CogOutputs(config));
        jobManager.start();
        userManager = new UserManager(db);
        uploadManager = new UploadManager(paths, fileOrigins, config.userQuotaMb * MB, config.maxUploadMb * MB * UNPACKED_FACTOR);
        uploadManager.cleanTmpFolder();
        stacIndexer = new StacIndexer(db, paths);
        // the catalog follows the changes of the server at once
        uploadManager.setChangeListener(stacIndexer::syncArea);
        jobManager.setOutputsListener(stacIndexer::syncFolder);
        stacIndexer.start(config.stacRescanMinutes);
        Templates templates = new Templates(config.workspace);
        urlSigner = new UrlSigner(config.workspace, config.signedUrlMinutes);
        Auth auth = new Auth(userManager, urlSigner, templates);
        fileSuggestions = new FileSuggestions(paths, fileOrigins);
        fileSuggestions.setMaxSuggestions(config.maxSuggestions);
        ModulePages modulePages = new ModulePages(jobManager, fileSuggestions, templates);
        JobPages jobPages = new JobPages(jobManager, userManager, templates);
        uploadPages = new UploadPages(uploadManager, fileOrigins, userManager, config.maxUploadMb, templates);
        AccountPages accountPages = new AccountPages(userManager, templates);
        AdminPages adminPages = new AdminPages(config, paths, userManager, jobManager, uploadManager, urlSigner, startTime,
                this::applyLiveSettings, templates);
        OgcApi ogcApi = new OgcApi(jobManager, urlSigner, templates);
        StacApi stacApi = new StacApi(db, stacIndexer, paths, urlSigner);

        app = Javalin.create(javalinConfig -> {
            javalinConfig.startup.showJavalinBanner = false;
            javalinConfig.startup.showOldJavalinVersionWarning = false;
            // the uploaded files are written on disk, next to the uploads, not kept in memory
            multipartConfig = javalinConfig.jetty.multipartConfig;
            multipartConfig.cacheDirectory(paths.getTmpFolder().getAbsolutePath());
            multipartConfig.maxInMemoryFileSize(1, SizeUnit.MB);
            setMultipartLimits(config.maxUploadMb);
            uploadPages.registerSizeCheck(javalinConfig);
            auth.register(javalinConfig);
            templates.register(javalinConfig);
            javalinConfig.routes.get("/", ctx -> ctx.redirect("/modules"));
            modulePages.register(javalinConfig);
            jobPages.register(javalinConfig);
            uploadPages.register(javalinConfig);
            accountPages.register(javalinConfig);
            adminPages.register(javalinConfig);
            ogcApi.register(javalinConfig);
            stacApi.register(javalinConfig);
        }).start(config.port);
    }

    /**
     * Record the files of the uploads moved into the areas of their users as uploaded by them, at the time of the file.
     */
    private static void recordMigratedUploads( WorkspacePaths paths, FileOrigins fileOrigins, Map<String, String> areas )
            throws Exception {
        for( Map.Entry<String, String> user : areas.entrySet() ) {
            String userId = user.getKey();
            Path area = paths.getAreaFolder(user.getValue()).toPath();
            List<Path> files;
            try (Stream<Path> walk = Files.walk(area)) {
                files = walk.filter(Files::isRegularFile).collect(Collectors.toList());
            }
            for( Path file : files ) {
                String path = paths.toWorkspacePath(file.toString());
                if (fileOrigins.get(path) == null) {
                    fileOrigins.record(FileOrigin.uploaded(path, userId, Files.getLastModifiedTime(file).toMillis()));
                }
            }
            LOGGER.info("The uploads of the user " + userId + " were moved into " + area + ".");
        }
    }

    private void setMultipartLimits( int maxUploadMb ) {
        // read by Javalin at every upload
        multipartConfig.maxFileSize(maxUploadMb, SizeUnit.MB);
        multipartConfig.maxTotalRequestSize(maxUploadMb, SizeUnit.MB);
    }

    /**
     * Apply the settings of the settings page to the running server, and keep them in its configuration.
     *
     * @param settings the new settings.
     */
    private synchronized void applyLiveSettings( ServerConfig settings ) {
        jobManager.setSlots(settings.slots);
        jobManager.setJobMemory(settings.jobMemory);
        jobManager.setTtlDays(settings.jobTtlDays);
        uploadManager.setLimits(settings.userQuotaMb * MB, settings.maxUploadMb * MB * UNPACKED_FACTOR);
        uploadPages.setMaxUploadMb(settings.maxUploadMb);
        setMultipartLimits(settings.maxUploadMb);
        fileSuggestions.setMaxSuggestions(settings.maxSuggestions);
        urlSigner.setMinutes(settings.signedUrlMinutes);
        stacIndexer.setRescanMinutes(settings.stacRescanMinutes);
        for( Setting setting : Setting.values() ) {
            config.set(setting, settings.get(setting));
        }
    }

    /**
     * Stop the server, killing the running jobs.
     */
    public synchronized void stop() {
        if (app != null) {
            app.stop();
            app = null;
        }
        try {
            if (stacIndexer != null) {
                stacIndexer.close();
                stacIndexer = null;
            }
            if (jobManager != null) {
                jobManager.close();
                jobManager = null;
            }
            if (db != null) {
                db.close();
                db = null;
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "The server didn't stop cleanly.", e);
        }
    }

    /**
     * @return the port the server listens on, once started.
     */
    public int getPort() {
        return app != null ? app.port() : config.port;
    }

    public ServerConfig getConfig() {
        return config;
    }

    /**
     * @return the jobs, once started.
     */
    public JobManager getJobManager() {
        return jobManager;
    }

    /**
     * @return the database, once started.
     */
    public ServerDb getDb() {
        return db;
    }

    /**
     * @return the users, once started.
     */
    public UserManager getUserManager() {
        return userManager;
    }

    /**
     * @return the uploads, once started.
     */
    public UploadManager getUploadManager() {
        return uploadManager;
    }
}
