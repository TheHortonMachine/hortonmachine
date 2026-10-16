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

/**
 * The configuration of the server, from the command line options.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ServerConfig {
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_WORKSPACE = "hm-server-workspace";
    public static final int DEFAULT_SLOTS = 2;
    public static final String DEFAULT_JOB_MEMORY = "2g";
    public static final int DEFAULT_JOB_TTL_DAYS = 7;

    /** The environment variable with the password of the database. */
    public static final String DB_PASSWORD_ENV = "HM_SERVER_DB_PASSWORD";

    /** The prefix of the PostgreSQL databases in the --db option. */
    public static final String POSTGRESQL_PREFIX = "postgresql:";

    /** The port to listen on, 0 for any free port. */
    public int port = DEFAULT_PORT;
    /** The folder of the data, database, jobs and uploads. */
    public File workspace = new File(DEFAULT_WORKSPACE);
    /** The number of jobs running at the same time. */
    public int slots = DEFAULT_SLOTS;
    /** The maximum memory of each job, as for the -Xmx option of java, ex. 2g. */
    public String jobMemory = DEFAULT_JOB_MEMORY;
    /** The days the finished jobs are kept, 0 to keep them forever. */
    public int jobTtlDays = DEFAULT_JOB_TTL_DAYS;
    /** The PostgreSQL database, as postgresql://host:port/database, or <code>null</code> for SQLite in the workspace. */
    public String db;
    /** The user of the PostgreSQL database. */
    public String dbUser;

    /**
     * Read the configuration from the command line arguments.
     *
     * @param args the arguments, as --name=value.
     * @param out where to print the usage and the version.
     * @return the configuration, or <code>null</code> if only the usage or the version was asked.
     * @throws IllegalArgumentException for a wrong argument.
     */
    public static ServerConfig fromArguments( String[] args, PrintStream out ) {
        ServerConfig config = new ServerConfig();
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
            String value = argument.substring(equalsIndex + 1).trim();
            switch( name ) {
            case "port":
                config.port = toInt(name, value, 0, 65535);
                break;
            case "workspace":
                if (value.isEmpty()) {
                    throw new IllegalArgumentException("The workspace folder is missing.");
                }
                config.workspace = new File(value);
                break;
            case "slots":
                config.slots = toInt(name, value, 1, 1000);
                break;
            case "job-memory":
                if (!value.matches("\\d+[kKmMgG]?")) {
                    throw new IllegalArgumentException("The job memory must be as 2g, 512m or 4096m: " + value);
                }
                config.jobMemory = value;
                break;
            case "job-ttl-days":
                config.jobTtlDays = toInt(name, value, 0, 100000);
                break;
            case "db":
                if (!value.startsWith(POSTGRESQL_PREFIX + "//")) {
                    throw new IllegalArgumentException("The database must be as postgresql://host:port/database: " + value);
                }
                config.db = value;
                break;
            case "db-user":
                config.dbUser = value;
                break;
            default:
                throw new IllegalArgumentException("Unknown option: --" + name);
            }
        }
        return config;
    }

    private static int toInt( String name, String value, int min, int max ) {
        int number;
        try {
            number = Integer.parseInt(value);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The value of --" + name + " must be a number: " + value);
        }
        if (number < min || number > max) {
            throw new IllegalArgumentException("The value of --" + name + " must be between " + min + " and " + max + ": " + value);
        }
        return number;
    }

    private static void printUsage( PrintStream out ) {
        out.println("HortonMachine server " + Version.getVersion());
        out.println();
        out.println("Usage: hm-server [--option=value ...]");
        out.println();
        out.println("  --port=" + DEFAULT_PORT + "              the port to listen on");
        out.println("  --workspace=" + DEFAULT_WORKSPACE + "   the folder of the data, jobs and uploads, created if missing");
        out.println("  --slots=" + DEFAULT_SLOTS + "                 the number of jobs running at the same time");
        out.println("  --job-memory=" + DEFAULT_JOB_MEMORY + "           the maximum memory of each job");
        out.println("  --job-ttl-days=" + DEFAULT_JOB_TTL_DAYS + "          the days the finished jobs are kept, 0 for ever");
        out.println("  --db=postgresql://host:port/database");
        out.println("                          use a PostgreSQL database instead of SQLite in the workspace;");
        out.println("                          its password is read from the " + DB_PASSWORD_ENV + " variable");
        out.println("  --db-user=user          the user of the PostgreSQL database");
        out.println("  --version               show the version");
    }
}
