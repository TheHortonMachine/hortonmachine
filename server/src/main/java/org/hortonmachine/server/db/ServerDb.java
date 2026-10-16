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
package org.hortonmachine.server.db;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.jobs.Job;
import org.hortonmachine.server.jobs.JobState;
import org.hortonmachine.server.users.Role;
import org.hortonmachine.server.users.User;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The database of the server: SQLite in the workspace, or PostgreSQL.
 *
 * <p>The SQL is kept portable between the two: text ids, times as BIGINT milliseconds since the epoch,
 * maps as JSON text. The access is serialized, since the SQLite database has a single connection,
 * used by the threads of the web pages and of the jobs.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class ServerDb implements AutoCloseable {
    /** The version of the tables, to upgrade them when they change. */
    public static final int SCHEMA_VERSION = 1;

    public static final String SQLITE_FILE = "hm-server.sqlite";

    private static final String JOB_COLUMNS = "id, user_id, module, parameters, state, progress, task, message, outputs, "
            + "created, started, finished, exit_code";
    private static final String USER_COLUMNS = "id, username, role, created, disabled";

    private static final TypeReference<Map<String, String>> MAP_TYPE = new TypeReference<Map<String, String>>(){
    };

    private final ADb db;
    private final ObjectMapper mapper = new ObjectMapper();

    private ServerDb( ADb db ) {
        this.db = db;
    }

    /**
     * Open the database of the configuration, creating the tables if missing.
     *
     * @param config the configuration.
     * @return the database.
     * @throws Exception if the database can't be opened or its tables are of a newer version.
     */
    public static ServerDb open( ServerConfig config ) throws Exception {
        ADb db;
        if (config.db == null) {
            db = EDb.SQLITE.getDb();
            db.open(new File(config.workspace, SQLITE_FILE).getAbsolutePath());
        } else {
            db = EDb.POSTGRES.getDb();
            db.open(config.db.substring(ServerConfig.POSTGRESQL_PREFIX.length()), config.dbUser,
                    System.getenv(ServerConfig.DB_PASSWORD_ENV));
        }
        ServerDb serverDb = new ServerDb(db);
        try {
            if (db.getType() == EDb.SQLITE) {
                // readers don't wait for the writers
                serverDb.execute("PRAGMA journal_mode=WAL");
            }
            serverDb.createTables();
        } catch (Exception e) {
            db.close();
            throw e;
        }
        return serverDb;
    }

    private void createTables() throws Exception {
        if (!db.hasTable("schema_version")) {
            execute("CREATE TABLE schema_version (version INTEGER NOT NULL)");
            execute("INSERT INTO schema_version (version) VALUES (" + SCHEMA_VERSION + ")");
        }
        long version = db.getLong("SELECT version FROM schema_version");
        if (version > SCHEMA_VERSION) {
            throw new IllegalStateException("The database is of a newer version of the server (" + version + ").");
        }
        if (!db.hasTable("jobs")) {
            execute("CREATE TABLE jobs (id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36), module VARCHAR(255) NOT NULL, "
                    + "parameters TEXT NOT NULL, state VARCHAR(16) NOT NULL, progress INTEGER NOT NULL, task TEXT, "
                    + "message TEXT, outputs TEXT, created BIGINT NOT NULL, started BIGINT, finished BIGINT, exit_code INTEGER)");
            execute("CREATE INDEX jobs_user_id ON jobs (user_id)");
            execute("CREATE INDEX jobs_state ON jobs (state)");
        }
        if (!db.hasTable("users")) {
            execute("CREATE TABLE users (id VARCHAR(36) PRIMARY KEY, username VARCHAR(64) NOT NULL UNIQUE, "
                    + "password_hash TEXT NOT NULL, role VARCHAR(16) NOT NULL, created BIGINT NOT NULL, disabled INTEGER NOT NULL)");
        }
    }

    private void execute( String sql ) throws Exception {
        db.execOnConnection(connection -> {
            try (IHMStatement statement = connection.createStatement()) {
                statement.execute(sql);
            }
            return null;
        });
    }

    private int update( String sql, Object... values ) throws Exception {
        return db.execOnConnection(connection -> {
            try (IHMPreparedStatement statement = connection.prepareStatement(sql)) {
                setValues(statement, values);
                return statement.executeUpdate();
            }
        });
    }

    private List<Job> queryJobs( String where, Object... values ) throws Exception {
        String sql = "SELECT " + JOB_COLUMNS + " FROM jobs " + where;
        return db.execOnConnection(connection -> {
            List<Job> jobs = new ArrayList<>();
            try (IHMPreparedStatement statement = connection.prepareStatement(sql)) {
                setValues(statement, values);
                try (IHMResultSet resultSet = statement.executeQuery()) {
                    while( resultSet.next() ) {
                        jobs.add(readJob(resultSet));
                    }
                }
            }
            return jobs;
        });
    }

    private static void setValues( IHMPreparedStatement statement, Object[] values ) throws Exception {
        for( int i = 0; i < values.length; i++ ) {
            Object value = values[i];
            if (value instanceof String) {
                statement.setString(i + 1, (String) value);
            } else if (value instanceof Long) {
                statement.setLong(i + 1, (Long) value);
            } else if (value instanceof Integer) {
                statement.setInt(i + 1, (Integer) value);
            } else {
                statement.setObject(i + 1, value);
            }
        }
    }

    private Job readJob( IHMResultSet resultSet ) throws Exception {
        Job job = new Job(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3), toMap(resultSet.getString(4)),
                resultSet.getLong(10));
        job.setState(JobState.valueOf(resultSet.getString(5)));
        job.setProgress(resultSet.getInt(6));
        job.setTask(resultSet.getString(7));
        job.setMessage(resultSet.getString(8));
        job.setOutputs(toMap(resultSet.getString(9)));
        job.setStarted(toLong(resultSet.getObject(11)));
        job.setFinished(toLong(resultSet.getObject(12)));
        Long exitCode = toLong(resultSet.getObject(13));
        job.setExitCode(exitCode != null ? exitCode.intValue() : null);
        return job;
    }

    private static Long toLong( Object value ) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

    private Map<String, String> toMap( String json ) throws Exception {
        if (json == null || json.isEmpty()) {
            return Collections.emptyMap();
        }
        return mapper.readValue(json, MAP_TYPE);
    }

    /**
     * Add a new job.
     */
    public synchronized void insertJob( Job job ) throws Exception {
        update("INSERT INTO jobs (" + JOB_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", job.id, job.userId,
                job.module, mapper.writeValueAsString(job.parameters), job.getState().name(), job.getProgress(), job.getTask(),
                job.getMessage(), mapper.writeValueAsString(job.getOutputs()), job.created, job.getStarted(), job.getFinished(),
                job.getExitCode());
    }

    /**
     * Save the state of a job: what changes while it runs.
     */
    public synchronized void updateJob( Job job ) throws Exception {
        update("UPDATE jobs SET state = ?, progress = ?, task = ?, message = ?, outputs = ?, started = ?, finished = ?, "
                + "exit_code = ? WHERE id = ?", job.getState().name(), job.getProgress(), job.getTask(), job.getMessage(),
                mapper.writeValueAsString(job.getOutputs()), job.getStarted(), job.getFinished(), job.getExitCode(), job.id);
    }

    /**
     * @return the job, or <code>null</code> if it doesn't exist.
     */
    public synchronized Job getJob( String id ) throws Exception {
        List<Job> jobs = queryJobs("WHERE id = ?", id);
        return jobs.isEmpty() ? null : jobs.get(0);
    }

    /**
     * Get the jobs, newest first.
     *
     * @param userId the user whose jobs to get, or <code>null</code> for the jobs of all users.
     * @param limit the maximum number of jobs.
     * @return the jobs.
     */
    public synchronized List<Job> listJobs( String userId, int limit ) throws Exception {
        if (userId == null) {
            return queryJobs("ORDER BY created DESC LIMIT " + limit);
        }
        return queryJobs("WHERE user_id = ? ORDER BY created DESC LIMIT " + limit, userId);
    }

    /**
     * @return the jobs finished before the given time.
     */
    public synchronized List<Job> listJobsFinishedBefore( long time ) throws Exception {
        return queryJobs("WHERE finished IS NOT NULL AND finished < ?", time);
    }

    /**
     * Mark the jobs that are queued or running as failed, as needed when the server starts,
     * since their processes are gone.
     *
     * @param message the error message to give them.
     * @param time the end time to give them.
     * @return the number of jobs marked.
     */
    public synchronized int failUnfinishedJobs( String message, long time ) throws Exception {
        return update("UPDATE jobs SET state = ?, message = ?, finished = ? WHERE state IN (?, ?)", JobState.FAILED.name(), message,
                time, JobState.QUEUED.name(), JobState.RUNNING.name());
    }

    public synchronized void deleteJob( String id ) throws Exception {
        update("DELETE FROM jobs WHERE id = ?", id);
    }

    private List<User> queryUsers( String where, Object... values ) throws Exception {
        String sql = "SELECT " + USER_COLUMNS + " FROM users " + where;
        return db.execOnConnection(connection -> {
            List<User> users = new ArrayList<>();
            try (IHMPreparedStatement statement = connection.prepareStatement(sql)) {
                setValues(statement, values);
                try (IHMResultSet resultSet = statement.executeQuery()) {
                    while( resultSet.next() ) {
                        users.add(new User(resultSet.getString(1), resultSet.getString(2), Role.valueOf(resultSet.getString(3)),
                                resultSet.getLong(4), resultSet.getInt(5) != 0));
                    }
                }
            }
            return users;
        });
    }

    /**
     * Add a new user.
     *
     * @param user the user.
     * @param passwordHash the hash of the password.
     */
    public synchronized void insertUser( User user, String passwordHash ) throws Exception {
        update("INSERT INTO users (" + USER_COLUMNS + ", password_hash) VALUES (?, ?, ?, ?, ?, ?)", user.id, user.username,
                user.role.name(), user.created, user.disabled ? 1 : 0, passwordHash);
    }

    /**
     * @return the user, or <code>null</code> if it doesn't exist.
     */
    public synchronized User getUser( String id ) throws Exception {
        List<User> users = queryUsers("WHERE id = ?", id);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * @return the user with the given name, or <code>null</code> if it doesn't exist.
     */
    public synchronized User getUserByName( String username ) throws Exception {
        List<User> users = queryUsers("WHERE username = ?", username);
        return users.isEmpty() ? null : users.get(0);
    }

    /**
     * @return the users, by name.
     */
    public synchronized List<User> listUsers() throws Exception {
        return queryUsers("ORDER BY username");
    }

    /**
     * @return the hash of the password of the user, or <code>null</code> if it doesn't exist.
     */
    public synchronized String getPasswordHash( String userId ) throws Exception {
        String sql = "SELECT password_hash FROM users WHERE id = ?";
        return db.execOnConnection(connection -> {
            try (IHMPreparedStatement statement = connection.prepareStatement(sql)) {
                statement.setString(1, userId);
                try (IHMResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getString(1) : null;
                }
            }
        });
    }

    public synchronized void updatePasswordHash( String userId, String passwordHash ) throws Exception {
        update("UPDATE users SET password_hash = ? WHERE id = ?", passwordHash, userId);
    }

    public synchronized void updateUser( User user ) throws Exception {
        update("UPDATE users SET role = ?, disabled = ? WHERE id = ?", user.role.name(), user.disabled ? 1 : 0, user.id);
    }

    public synchronized void deleteUser( String id ) throws Exception {
        update("DELETE FROM users WHERE id = ?", id);
    }

    @Override
    public synchronized void close() throws Exception {
        db.close();
    }
}
