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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.hortonmachine.Version;
import org.hortonmachine.gears.utils.files.FileUtilities;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * The configuration of the server, from the command line options and the settings file of the workspace.
 *
 * <p>The {@link Setting settings} are taken, in order, from the command line, from the
 * {@value #SETTINGS_FILE} file of the workspace, written by the settings page of the administrators,
 * and from the defaults. The port, the workspace, since the file is in it, and the database are only given
 * on the command line; the password of the database only in the environment, never written.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ServerConfig {
    public static final int DEFAULT_PORT = 8080;
    public static final String DEFAULT_WORKSPACE = "hm-server-workspace";
    public static final int DEFAULT_SLOTS = 2;
    public static final String DEFAULT_JOB_MEMORY = "2g";
    public static final int DEFAULT_JOB_TTL_DAYS = 7;
    public static final int DEFAULT_MAX_UPLOAD_MB = 500;
    public static final int DEFAULT_USER_QUOTA_MB = 5120;

    /** The file of the settings, in the workspace. */
    public static final String SETTINGS_FILE = "hm-server-settings.json";

    /** The environment variable with the password of the database. */
    public static final String DB_PASSWORD_ENV = "HM_SERVER_DB_PASSWORD";
    /** The environment variable with the password of the administrator to create, when there is no console. */
    public static final String ADMIN_PASSWORD_ENV = "HM_SERVER_ADMIN_PASSWORD";

    /** The prefix of the PostgreSQL databases in the --db option. */
    public static final String POSTGRESQL_PREFIX = "postgresql:";

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<Map<String, String>>(){
    };

    /**
     * The settings that can also be changed in the settings page, applied at once to the running server.
     */
    public enum Setting {
        SLOTS("slots", "Jobs at the same time", "The number of jobs running at the same time; the others wait."), //
        JOB_MEMORY("job-memory", "Memory of a job", "The maximum memory of each job, as 2g, 512m or 4096m, for the jobs started from now on."), //
        JOB_TTL_DAYS("job-ttl-days", "Days to keep the jobs",
                "The days the finished jobs are kept, with their files, 0 to keep them for ever. Checked every hour."), //
        MAX_UPLOAD_MB("max-upload-mb", "Largest upload (MB)", "The maximum size of an upload, all its files together."), //
        USER_QUOTA_MB("user-quota-mb", "Uploads of a user (MB)", "The maximum size of the uploads of each user, 0 for no limit.");

        /** The name, as in the command line option --name=value and in the settings file. */
        public final String option;
        public final String label;
        public final String description;

        Setting( String option, String label, String description ) {
            this.option = option;
            this.label = label;
            this.description = description;
        }

        public static Setting of( String option ) {
            for( Setting setting : values() ) {
                if (setting.option.equals(option)) {
                    return setting;
                }
            }
            return null;
        }
    }

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
    /** The maximum size of an upload in MB, of all its files together. */
    public int maxUploadMb = DEFAULT_MAX_UPLOAD_MB;
    /** The maximum size of the uploads of each user in MB, 0 for no limit. */
    public int userQuotaMb = DEFAULT_USER_QUOTA_MB;
    /** The PostgreSQL database, as postgresql://host:port/database, or <code>null</code> for SQLite in the workspace. */
    public String db;
    /** The user of the PostgreSQL database. */
    public String dbUser;
    /** The name of an administrator to create, instead of starting the server, or <code>null</code>. */
    public String createAdmin;

    /** The settings given on the command line, which the settings file doesn't change. */
    public final Set<Setting> fromCommandLine = new LinkedHashSet<>();

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
            case "db":
                if (!value.startsWith(POSTGRESQL_PREFIX + "//")) {
                    throw new IllegalArgumentException("The database must be as postgresql://host:port/database: " + value);
                }
                config.db = value;
                break;
            case "db-user":
                config.dbUser = value;
                break;
            case "create-admin":
                if (value.isEmpty()) {
                    throw new IllegalArgumentException("The name of the administrator is missing.");
                }
                config.createAdmin = value;
                break;
            default:
                Setting setting = Setting.of(name);
                if (setting == null) {
                    throw new IllegalArgumentException("Unknown option: --" + name);
                }
                config.set(setting, value);
                config.fromCommandLine.add(setting);
            }
        }
        return config;
    }

    /**
     * Set a setting.
     *
     * @param setting the setting.
     * @param value the value, as in the command line.
     * @throws IllegalArgumentException if the value is not valid.
     */
    public void set( Setting setting, String value ) {
        String name = setting.option;
        value = value == null ? "" : value.trim();
        switch( setting ) {
        case SLOTS:
            slots = toInt(name, value, 1, 1000);
            break;
        case JOB_MEMORY:
            if (!value.matches("\\d+[kKmMgG]?")) {
                throw new IllegalArgumentException("The job memory must be as 2g, 512m or 4096m: " + value);
            }
            jobMemory = value;
            break;
        case JOB_TTL_DAYS:
            jobTtlDays = toInt(name, value, 0, 100000);
            break;
        case MAX_UPLOAD_MB:
            maxUploadMb = toInt(name, value, 1, 1000000);
            break;
        case USER_QUOTA_MB:
            userQuotaMb = toInt(name, value, 0, 100000000);
            break;
        default:
            throw new IllegalArgumentException("Unknown setting: " + name);
        }
    }

    /**
     * @return the value of a setting, as in the command line; empty for none.
     */
    public String get( Setting setting ) {
        switch( setting ) {
        case SLOTS:
            return String.valueOf(slots);
        case JOB_MEMORY:
            return jobMemory;
        case JOB_TTL_DAYS:
            return String.valueOf(jobTtlDays);
        case MAX_UPLOAD_MB:
            return String.valueOf(maxUploadMb);
        case USER_QUOTA_MB:
            return String.valueOf(userQuotaMb);
        default:
            throw new IllegalArgumentException("Unknown setting: " + setting);
        }
    }

    /**
     * Apply the settings of the settings file of the workspace, but those given on the command line.
     *
     * @throws IllegalArgumentException if the file has a value not valid.
     */
    public void applySettingsFile() throws Exception {
        for( Map.Entry<String, String> entry : readSettingsFile(workspace).entrySet() ) {
            Setting setting = Setting.of(entry.getKey());
            if (setting != null && !fromCommandLine.contains(setting)) {
                try {
                    set(setting, entry.getValue());
                } catch (IllegalArgumentException e) {
                    throw new IllegalArgumentException(SETTINGS_FILE + ": " + e.getMessage(), e);
                }
            }
        }
    }

    /**
     * @return the settings of the settings file of a workspace, by option name; empty if there is no file.
     */
    public static Map<String, String> readSettingsFile( File workspace ) throws Exception {
        File file = new File(workspace, SETTINGS_FILE);
        if (!file.isFile()) {
            return new LinkedHashMap<>();
        }
        return new ObjectMapper().readValue(FileUtilities.readFile(file), MAP_TYPE);
    }

    /**
     * Write the settings file of a workspace.
     *
     * @param settings the settings by option name.
     */
    public static void writeSettingsFile( File workspace, Map<String, String> settings ) throws Exception {
        String json = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValueAsString(settings);
        FileUtilities.writeFile(json, new File(workspace, SETTINGS_FILE));
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
        out.println("  --max-upload-mb=" + DEFAULT_MAX_UPLOAD_MB + "       the maximum size of an upload");
        out.println("  --user-quota-mb=" + DEFAULT_USER_QUOTA_MB + "      the maximum size of the uploads of each user, 0 for no limit");
        out.println("  --db=postgresql://host:port/database");
        out.println("                          use a PostgreSQL database instead of SQLite in the workspace;");
        out.println("                          its password is read from the " + DB_PASSWORD_ENV + " variable");
        out.println("  --db-user=user          the user of the PostgreSQL database");
        out.println("  --create-admin=name     create an administrator in the workspace and exit, without starting;");
        out.println("                          the password is asked, or read from the " + ADMIN_PASSWORD_ENV + " variable");
        out.println("  --version               show the version");
        out.println();
        out.println("The options but the port, the workspace and the database can also be set by the administrators");
        out.println("in the settings page, saved in the " + SETTINGS_FILE + " file of the workspace; the options");
        out.println("given here win over that file.");
    }
}
