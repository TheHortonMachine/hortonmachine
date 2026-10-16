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

import java.io.File;
import java.io.PrintStream;

import org.hortonmachine.Version;

import io.javalin.Javalin;

/**
 * The HortonMachine web server: runs the modules for the users of a browser.
 *
 * <pre>
 * hm-server [--port=8080] [--workspace=folder]
 * </pre>
 *
 * <p>The workspace folder holds the data, the users, the jobs and the uploads of the server.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class HmServer {
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_WORKSPACE = "hm-server-workspace";

    public static final int EXIT_OK = 0;
    public static final int EXIT_USAGE = 2;

    private final int port;
    private final File workspace;
    private Javalin app;

    /**
     * @param port the port to listen on, 0 for any free port.
     * @param workspace the workspace folder, created if missing.
     */
    public HmServer( int port, File workspace ) {
        this.port = port;
        this.workspace = workspace;
    }

    public static void main( String[] args ) {
        HmServer server;
        try {
            server = fromArguments(args, System.out);
        } catch (IllegalArgumentException e) {
            System.err.println("Error: " + e.getMessage());
            System.err.println("Run 'hm-server --help' for the usage.");
            System.exit(EXIT_USAGE);
            return;
        }
        if (server == null) {
            System.exit(EXIT_OK);
            return;
        }
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        System.out.println("HortonMachine server " + Version.getVersion() + " listening on port " + server.getPort()
                + ", workspace " + server.workspace.getAbsolutePath());
    }

    /**
     * Create the server from the command line arguments.
     *
     * @param args the arguments, as --name=value.
     * @param out where to print the usage and the version.
     * @return the server, or <code>null</code> if only the usage or the version was asked.
     * @throws IllegalArgumentException for a wrong argument.
     */
    static HmServer fromArguments( String[] args, PrintStream out ) {
        int port = DEFAULT_PORT;
        File workspace = new File(DEFAULT_WORKSPACE);
        for( String argument : args ) {
            if (argument.equals("--help") || argument.equals("-h")) {
                printUsage(out);
                return null;
            } else if (argument.equals("--version") || argument.equals("version")) {
                out.println("HortonMachine server " + Version.getVersion());
                return null;
            }
            int equalsIndex = argument.indexOf('=');
            if (!argument.startsWith("--") || equalsIndex < 0) {
                throw new IllegalArgumentException("Unexpected argument: " + argument + ". Options are given as --option=value.");
            }
            String name = argument.substring(2, equalsIndex);
            String value = argument.substring(equalsIndex + 1);
            switch( name ) {
            case "port":
                try {
                    port = Integer.parseInt(value.trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("The port must be a number: " + value);
                }
                if (port < 0 || port > 65535) {
                    throw new IllegalArgumentException("The port must be between 0 and 65535: " + value);
                }
                break;
            case "workspace":
                if (value.isBlank()) {
                    throw new IllegalArgumentException("The workspace folder is missing.");
                }
                workspace = new File(value);
                break;
            default:
                throw new IllegalArgumentException("Unknown option: --" + name);
            }
        }
        return new HmServer(port, workspace);
    }

    private static void printUsage( PrintStream out ) {
        out.println("HortonMachine server " + Version.getVersion());
        out.println();
        out.println("Usage:");
        out.println("  hm-server [--port=" + DEFAULT_PORT + "] [--workspace=" + DEFAULT_WORKSPACE + "]");
        out.println();
        out.println("  --port        the port to listen on");
        out.println("  --workspace   the folder of the data, users, jobs and uploads, created if missing");
        out.println("  --version     show the version");
    }

    /**
     * Start the server.
     *
     * @throws IllegalStateException if the workspace can't be created or the port is not available.
     */
    public void start() {
        if (!workspace.isDirectory() && !workspace.mkdirs()) {
            throw new IllegalStateException("The workspace folder can't be created: " + workspace.getAbsolutePath());
        }
        app = Javalin.create(config -> {
            config.startup.showJavalinBanner = false;
            config.startup.showOldJavalinVersionWarning = false;
            config.routes.get("/", ctx -> ctx.html("<!doctype html><html><head><meta charset=\"utf-8\"><title>HortonMachine</title>"
                    + "</head><body><h1>HortonMachine server " + Version.getVersion() + "</h1></body></html>"));
        }).start(port);
    }

    /**
     * Stop the server, if started.
     */
    public void stop() {
        if (app != null) {
            app.stop();
            app = null;
        }
    }

    /**
     * @return the port the server listens on, once started.
     */
    public int getPort() {
        return app != null ? app.port() : port;
    }

    public File getWorkspace() {
        return workspace;
    }
}
