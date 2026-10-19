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
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.server.ServerConfig;
import org.hortonmachine.server.files.FileOrigin;
import org.hortonmachine.server.jobs.Job;
import org.hortonmachine.server.jobs.JobState;
import org.hortonmachine.server.stac.StacCollectionInfo;
import org.hortonmachine.server.stac.StacRecord;
import org.hortonmachine.server.stac.StacSearch;
import org.hortonmachine.server.users.ApiToken;
import org.hortonmachine.server.users.Role;
import org.hortonmachine.server.users.User;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
    public static final int SCHEMA_VERSION = 7;

    public static final String SQLITE_FILE = "hm-server.sqlite";

    private static final String JOB_COLUMNS = "id, user_id, module, parameters, state, progress, task, message, outputs, "
            + "created, started, finished, exit_code, output_folder, overwrite";
    private static final String FILE_COLUMNS = "path, origin, user_id, created, job_id, module, parameters, history";
    private static final String USER_COLUMNS = "id, username, role, created, disabled";
    private static final String STAC_COLUMNS = "id, collection, owner_id, path, size, modified, min_lon, min_lat, max_lon, max_lat, "
            + "start_time, end_time, item";

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
                    + "message TEXT, outputs TEXT, created BIGINT NOT NULL, started BIGINT, finished BIGINT, exit_code INTEGER, "
                    + "output_folder TEXT, overwrite INTEGER NOT NULL DEFAULT 0)");
            execute("CREATE INDEX jobs_user_id ON jobs (user_id)");
            execute("CREATE INDEX jobs_state ON jobs (state)");
        } else if (version < 5) {
            // version 5: the jobs write their outputs in a folder of the data
            execute("ALTER TABLE jobs ADD COLUMN output_folder TEXT");
            execute("ALTER TABLE jobs ADD COLUMN overwrite INTEGER NOT NULL DEFAULT 0");
        }
        if (!db.hasTable("users")) {
            execute("CREATE TABLE users (id VARCHAR(36) PRIMARY KEY, username VARCHAR(64) NOT NULL UNIQUE, "
                    + "password_hash TEXT NOT NULL, role VARCHAR(16) NOT NULL, created BIGINT NOT NULL, disabled INTEGER NOT NULL)");
        }
        // version 2: the tokens of the api
        if (!db.hasTable("api_tokens")) {
            execute("CREATE TABLE api_tokens (id VARCHAR(36) PRIMARY KEY, user_id VARCHAR(36) NOT NULL, name VARCHAR(100) NOT NULL, "
                    + "token_hash VARCHAR(64) NOT NULL UNIQUE, created BIGINT NOT NULL, last_used BIGINT)");
            execute("CREATE INDEX api_tokens_user_id ON api_tokens (user_id)");
        }
        // version 4: the index of the STAC catalog by folder; it is rebuilt from the files, so the one of version 3 goes
        if (version < 4 && db.hasTable("stac_items")) {
            execute("DROP TABLE stac_items");
        }
        if (!db.hasTable("stac_items")) {
            execute("CREATE TABLE stac_items (path VARCHAR(2048) PRIMARY KEY, collection VARCHAR(255) NOT NULL, id VARCHAR(2048) NOT NULL, "
                    + "owner_id VARCHAR(36), size BIGINT NOT NULL, modified BIGINT NOT NULL, min_lon DOUBLE PRECISION, "
                    + "min_lat DOUBLE PRECISION, max_lon DOUBLE PRECISION, max_lat DOUBLE PRECISION, start_time BIGINT, "
                    + "end_time BIGINT, item TEXT NOT NULL)");
            execute("CREATE UNIQUE INDEX stac_items_collection_id ON stac_items (collection, id)");
            execute("CREATE INDEX stac_items_owner_id ON stac_items (owner_id)");
        }
        // version 4: the metadata of the collections, given by the administrators
        if (!db.hasTable("stac_collections")) {
            execute("CREATE TABLE stac_collections (id VARCHAR(255) PRIMARY KEY, title TEXT, description TEXT, keywords TEXT, "
                    + "license VARCHAR(255), updated BIGINT NOT NULL)");
        }
        // version 5: where the files of the data come from; the index is rebuilt with the areas
        if (!db.hasTable("data_files")) {
            execute("CREATE TABLE data_files (path VARCHAR(2048) PRIMARY KEY, origin VARCHAR(16) NOT NULL, user_id VARCHAR(36), "
                    + "created BIGINT NOT NULL, job_id VARCHAR(36), module VARCHAR(255), parameters TEXT, history TEXT)");
        }
        // version 6: the items are titled by their path in the collection; 7: the areas of the users by username
        if (version < 7) {
            execute("DELETE FROM stac_items");
        }
        if (version < SCHEMA_VERSION) {
            execute("UPDATE schema_version SET version = " + SCHEMA_VERSION);
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
            } else if (value instanceof Double) {
                statement.setDouble(i + 1, (Double) value);
            } else {
                statement.setObject(i + 1, value);
            }
        }
    }

    private Job readJob( IHMResultSet resultSet ) throws Exception {
        Job job = new Job(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3), toMap(resultSet.getString(4)),
                resultSet.getLong(10), resultSet.getString(14), resultSet.getInt(15) != 0);
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
        update("INSERT INTO jobs (" + JOB_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", job.id, job.userId,
                job.module, mapper.writeValueAsString(job.parameters), job.getState().name(), job.getProgress(), job.getTask(),
                job.getMessage(), mapper.writeValueAsString(job.getOutputs()), job.created, job.getStarted(), job.getFinished(),
                job.getExitCode(), job.outputFolder, job.overwrite ? 1 : 0);
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
     * @return the jobs queued or running, oldest first.
     */
    public synchronized List<Job> listUnfinishedJobs() throws Exception {
        return queryJobs("WHERE state IN (?, ?) ORDER BY created", JobState.QUEUED.name(), JobState.RUNNING.name());
    }

    /**
     * @param userId the user whose jobs to get, or <code>null</code> for the jobs of all users.
     * @return the jobs not queued or running.
     */
    public synchronized List<Job> listFinishedJobs( String userId ) throws Exception {
        if (userId == null) {
            return queryJobs("WHERE state NOT IN (?, ?)", JobState.QUEUED.name(), JobState.RUNNING.name());
        }
        return queryJobs("WHERE state NOT IN (?, ?) AND user_id = ?", JobState.QUEUED.name(), JobState.RUNNING.name(), userId);
    }

    /**
     * @return the number of jobs in each state, without the states that have none.
     */
    public synchronized Map<JobState, Integer> countJobsByState() throws Exception {
        return db.execOnConnection(connection -> {
            Map<JobState, Integer> counts = new EnumMap<>(JobState.class);
            try (IHMStatement statement = connection.createStatement();
                    IHMResultSet resultSet = statement.executeQuery("SELECT state, COUNT(*) FROM jobs GROUP BY state")) {
                while( resultSet.next() ) {
                    counts.put(JobState.valueOf(resultSet.getString(1)), resultSet.getInt(2));
                }
            }
            return counts;
        });
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

    private List<ApiToken> queryTokens( String where, Object... values ) throws Exception {
        String sql = "SELECT id, user_id, name, created, last_used FROM api_tokens " + where;
        return db.execOnConnection(connection -> {
            List<ApiToken> tokens = new ArrayList<>();
            try (IHMPreparedStatement statement = connection.prepareStatement(sql)) {
                setValues(statement, values);
                try (IHMResultSet resultSet = statement.executeQuery()) {
                    while( resultSet.next() ) {
                        tokens.add(new ApiToken(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
                                resultSet.getLong(4), toLong(resultSet.getObject(5))));
                    }
                }
            }
            return tokens;
        });
    }

    /**
     * Add a token of the api.
     *
     * @param token the token.
     * @param tokenHash the hash of the secret of the token.
     */
    public synchronized void insertToken( ApiToken token, String tokenHash ) throws Exception {
        update("INSERT INTO api_tokens (id, user_id, name, token_hash, created, last_used) VALUES (?, ?, ?, ?, ?, ?)", token.id,
                token.userId, token.name, tokenHash, token.created, token.lastUsed);
    }

    /**
     * @return the tokens of a user, newest first.
     */
    public synchronized List<ApiToken> listTokens( String userId ) throws Exception {
        return queryTokens("WHERE user_id = ? ORDER BY created DESC", userId);
    }

    /**
     * @return the token with the given hash of its secret, or <code>null</code>.
     */
    public synchronized ApiToken getTokenByHash( String tokenHash ) throws Exception {
        List<ApiToken> tokens = queryTokens("WHERE token_hash = ?", tokenHash);
        return tokens.isEmpty() ? null : tokens.get(0);
    }

    /**
     * @return the token with the given id, or <code>null</code>.
     */
    public synchronized ApiToken getToken( String tokenId ) throws Exception {
        List<ApiToken> tokens = queryTokens("WHERE id = ?", tokenId);
        return tokens.isEmpty() ? null : tokens.get(0);
    }

    public synchronized void updateTokenLastUsed( String tokenId, long lastUsed ) throws Exception {
        update("UPDATE api_tokens SET last_used = ? WHERE id = ?", lastUsed, tokenId);
    }

    /**
     * Delete a token of a user.
     *
     * @return <code>false</code> if the user has no such token.
     */
    public synchronized boolean deleteToken( String userId, String tokenId ) throws Exception {
        return update("DELETE FROM api_tokens WHERE id = ? AND user_id = ?", tokenId, userId) > 0;
    }

    public synchronized void deleteTokensOfUser( String userId ) throws Exception {
        update("DELETE FROM api_tokens WHERE user_id = ?", userId);
    }

    /**
     * @return the origin of a file of the data, with its history, or <code>null</code> if it has none.
     */
    public synchronized FileOrigin getFileOrigin( String path ) throws Exception {
        return db.execOnConnection(connection -> {
            try (IHMPreparedStatement statement = connection.prepareStatement("SELECT " + FILE_COLUMNS + " FROM data_files WHERE path = ?")) {
                statement.setString(1, path);
                try (IHMResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    List<FileOrigin> history = new ArrayList<>();
                    String historyJson = resultSet.getString(8);
                    if (historyJson != null && !historyJson.isEmpty()) {
                        for( JsonNode entry : mapper.readTree(historyJson) ) {
                            history.add(FileOrigin.fromJson(path, entry));
                        }
                    }
                    String parameters = resultSet.getString(7);
                    return new FileOrigin(path, resultSet.getString(2), resultSet.getString(3), resultSet.getLong(4),
                            resultSet.getString(5), resultSet.getString(6), parameters != null ? toMap(parameters) : null, null,
                            history);
                }
            }
        });
    }

    /**
     * Set the origin of a file of the data, replacing the previous one.
     */
    public synchronized void putFileOrigin( FileOrigin origin ) throws Exception {
        ArrayNode history = mapper.createArrayNode();
        origin.history.forEach(entry -> history.add(entry.toJson(mapper)));
        update("DELETE FROM data_files WHERE path = ?", origin.path);
        update("INSERT INTO data_files (" + FILE_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?)", origin.path, origin.origin,
                origin.userId, origin.created, origin.jobId, origin.module,
                origin.parameters != null ? mapper.writeValueAsString(origin.parameters) : null, mapper.writeValueAsString(history));
    }

    /**
     * Change the paths of an area moved to another name, in the origins of its files and in the jobs.
     *
     * @param from the path of the old area, as data/&lt;user id&gt;.
     * @param to the path of the new one, as data/&lt;username&gt;.
     */
    public synchronized void renameAreaPaths( String from, String to ) throws Exception {
        String escaped = from.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        update("UPDATE data_files SET path = ? || SUBSTR(path, ?) WHERE path LIKE ? ESCAPE '!'", to, from.length() + 1,
                escaped + "/%");
        // the paths are also inside the JSON of the parameters and outputs
        update("UPDATE jobs SET output_folder = REPLACE(output_folder, ?, ?), parameters = REPLACE(parameters, ?, ?), "
                + "outputs = REPLACE(outputs, ?, ?)", from + "/", to + "/", from + "/", to + "/", from + "/", to + "/");
        update("UPDATE jobs SET output_folder = ? WHERE output_folder = ?", to, from);
    }

    /**
     * Delete the origins of the files of a path or under it, as data/shared/dem for data/shared/dem/dtm.tif.
     */
    public synchronized void deleteFileOriginsUnder( String path ) throws Exception {
        String escaped = path.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        update("DELETE FROM data_files WHERE path = ? OR path LIKE ? ESCAPE '!'", path, escaped + "/%");
    }

    /**
     * Add an item of the STAC catalog, or replace the one of the same file.
     */
    public synchronized void putStacItem( StacRecord item ) throws Exception {
        update("DELETE FROM stac_items WHERE path = ?", item.path);
        update("INSERT INTO stac_items (" + STAC_COLUMNS + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)", item.id,
                item.collection, item.ownerId, item.path, item.size, item.modified, item.minLon, item.minLat, item.maxLon,
                item.maxLat, item.startTime, item.endTime, item.json);
    }

    /**
     * @return the item of a collection of the STAC catalog, or <code>null</code>.
     */
    public synchronized StacRecord getStacItem( String collection, String id ) throws Exception {
        List<StacRecord> items = queryStacItems("WHERE collection = ? AND id = ?", collection, id);
        return items.isEmpty() ? null : items.get(0);
    }

    /**
     * Delete the item of a file of the STAC catalog.
     *
     * @param path the path of the file in the workspace.
     * @return <code>false</code> if there is no such item.
     */
    public synchronized boolean deleteStacItem( String path ) throws Exception {
        return update("DELETE FROM stac_items WHERE path = ?", path) > 0;
    }

    /**
     * @return the items of the STAC catalog whose path is the given one or is under it, as data/dem for data/dem/dtm.tif.
     */
    public synchronized List<StacRecord> listStacItemsUnder( String path ) throws Exception {
        String escaped = path.replace("!", "!!").replace("%", "!%").replace("_", "!_");
        return queryStacItems("WHERE path = ? OR path LIKE ? ESCAPE '!' ORDER BY path", path, escaped + "/%");
    }

    /**
     * @return the users that have files in the STAC catalog.
     */
    public synchronized List<String> listStacOwners() throws Exception {
        return db.execOnConnection(connection -> {
            List<String> owners = new ArrayList<>();
            try (IHMStatement statement = connection.createStatement();
                    IHMResultSet resultSet = statement.executeQuery("SELECT DISTINCT owner_id FROM stac_items WHERE owner_id IS NOT NULL")) {
                while( resultSet.next() ) {
                    owners.add(resultSet.getString(1));
                }
            }
            return owners;
        });
    }

    /**
     * @return the metadata of a collection of the STAC catalog, or <code>null</code> if it has none.
     */
    public synchronized StacCollectionInfo getStacCollectionInfo( String collection ) throws Exception {
        return db.execOnConnection(connection -> {
            try (IHMPreparedStatement statement = connection
                    .prepareStatement("SELECT title, description, keywords, license FROM stac_collections WHERE id = ?")) {
                statement.setString(1, collection);
                try (IHMResultSet resultSet = statement.executeQuery()) {
                    if (!resultSet.next()) {
                        return null;
                    }
                    String keywords = resultSet.getString(3);
                    return new StacCollectionInfo(collection, resultSet.getString(1), resultSet.getString(2),
                            keywords == null || keywords.isEmpty() ? List.of() : List.of(mapper.readValue(keywords, String[].class)),
                            resultSet.getString(4));
                }
            }
        });
    }

    /**
     * Set the metadata of a collection of the STAC catalog, replacing the previous.
     */
    public synchronized void putStacCollectionInfo( StacCollectionInfo info ) throws Exception {
        update("DELETE FROM stac_collections WHERE id = ?", info.id);
        update("INSERT INTO stac_collections (id, title, description, keywords, license, updated) VALUES (?, ?, ?, ?, ?, ?)", info.id,
                info.title, info.description, mapper.writeValueAsString(info.keywords), info.license, System.currentTimeMillis());
    }

    /**
     * Build the conditions of a search, without paging.
     *
     * @param values filled with the values of the conditions.
     */
    private static String searchWhere( StacSearch search, List<Object> values ) {
        StringBuilder where = new StringBuilder("WHERE 1 = 1");
        if (!search.isAdmin) {
            // the shared area and the own one
            where.append(" AND (owner_id IS NULL OR owner_id = ?)");
            values.add(search.ownArea);
        }
        appendIn(where, values, "collection", search.collections);
        appendIn(where, values, "id", search.ids);
        if (search.bbox != null) {
            where.append(" AND min_lon IS NOT NULL AND max_lon >= ? AND min_lon <= ? AND max_lat >= ? AND min_lat <= ?");
            values.add(search.bbox[0]);
            values.add(search.bbox[2]);
            values.add(search.bbox[1]);
            values.add(search.bbox[3]);
        }
        if (search.start != null) {
            where.append(" AND end_time >= ?");
            values.add(search.start);
        }
        if (search.end != null) {
            where.append(" AND start_time <= ?");
            values.add(search.end);
        }
        return where.toString();
    }

    private static void appendIn( StringBuilder where, List<Object> values, String column, Set<String> wanted ) {
        if (wanted.isEmpty()) {
            return;
        }
        where.append(" AND ").append(column).append(" IN (");
        where.append(String.join(", ", Collections.nCopies(wanted.size(), "?"))).append(")");
        values.addAll(wanted);
    }

    /**
     * @return the items of the STAC catalog found by a search, by collection and id.
     */
    public synchronized List<StacRecord> searchStacItems( StacSearch search ) throws Exception {
        List<Object> values = new ArrayList<>();
        String where = searchWhere(search, values);
        return queryStacItems(where + " ORDER BY collection, id LIMIT " + search.limit + " OFFSET " + search.offset,
                values.toArray());
    }

    /**
     * @return the number of the items found by a search, without paging.
     */
    public synchronized long countStacItems( StacSearch search ) throws Exception {
        List<Object> values = new ArrayList<>();
        String sql = "SELECT COUNT(*) FROM stac_items " + searchWhere(search, values);
        return db.execOnConnection(connection -> {
            try (IHMPreparedStatement statement = connection.prepareStatement(sql)) {
                setValues(statement, values.toArray());
                try (IHMResultSet resultSet = statement.executeQuery()) {
                    return resultSet.next() ? resultSet.getLong(1) : 0L;
                }
            }
        });
    }

    /**
     * The extent of a collection of the STAC catalog, as seen by a requester.
     */
    public static class StacCollectionExtent {
        public final String collection;
        public final long count;
        /** The bounds, <code>null</code> if no item has a geometry. */
        public final double[] bbox;
        public final Long start;
        public final Long end;

        StacCollectionExtent( String collection, long count, double[] bbox, Long start, Long end ) {
            this.collection = collection;
            this.count = count;
            this.bbox = bbox;
            this.start = start;
            this.end = end;
        }
    }

    /**
     * @return the collections that have items found by the search, ignoring its paging, with their extents, by id.
     */
    public synchronized List<StacCollectionExtent> listStacCollections( StacSearch search ) throws Exception {
        List<Object> values = new ArrayList<>();
        String sql = "SELECT collection, COUNT(*), MIN(min_lon), MIN(min_lat), MAX(max_lon), MAX(max_lat), MIN(start_time), "
                + "MAX(end_time) FROM stac_items " + searchWhere(search, values) + " GROUP BY collection ORDER BY collection";
        return db.execOnConnection(connection -> {
            List<StacCollectionExtent> extents = new ArrayList<>();
            try (IHMPreparedStatement statement = connection.prepareStatement(sql)) {
                setValues(statement, values.toArray());
                try (IHMResultSet resultSet = statement.executeQuery()) {
                    while( resultSet.next() ) {
                        Object minLon = resultSet.getObject(3);
                        double[] bbox = minLon instanceof Number ? new double[]{((Number) minLon).doubleValue(),
                                resultSet.getDouble(4), resultSet.getDouble(5), resultSet.getDouble(6)} : null;
                        extents.add(new StacCollectionExtent(resultSet.getString(1), resultSet.getLong(2), bbox,
                                toLong(resultSet.getObject(7)), toLong(resultSet.getObject(8))));
                    }
                }
            }
            return extents;
        });
    }

    private List<StacRecord> queryStacItems( String where, Object... values ) throws Exception {
        String sql = "SELECT " + STAC_COLUMNS + " FROM stac_items " + where;
        return db.execOnConnection(connection -> {
            List<StacRecord> items = new ArrayList<>();
            try (IHMPreparedStatement statement = connection.prepareStatement(sql)) {
                setValues(statement, values);
                try (IHMResultSet resultSet = statement.executeQuery()) {
                    while( resultSet.next() ) {
                        Object minLon = resultSet.getObject(7);
                        double[] bbox = minLon instanceof Number ? new double[]{((Number) minLon).doubleValue(),
                                resultSet.getDouble(8), resultSet.getDouble(9), resultSet.getDouble(10)} : null;
                        items.add(new StacRecord(resultSet.getString(1), resultSet.getString(2), resultSet.getString(3),
                                resultSet.getString(4), resultSet.getLong(5), resultSet.getLong(6), bbox,
                                toLong(resultSet.getObject(11)), toLong(resultSet.getObject(12)), resultSet.getString(13)));
                    }
                }
            }
            return items;
        });
    }

    @Override
    public synchronized void close() throws Exception {
        db.close();
    }
}
