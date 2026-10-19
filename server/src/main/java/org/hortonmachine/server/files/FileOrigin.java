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
package org.hortonmachine.server.files;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Where a file of the data comes from: uploaded by a user or written by a job, with the previous origins of the files it
 * replaced at the same path. The files without origin were put in the data from outside the server.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class FileOrigin {
    public static final String UPLOADED = "uploaded";
    public static final String PROCESSED = "processed";
    /** The origin of the files put in the data from outside, which have no record. */
    public static final String EXTERNAL = "external";

    /** The path of the file in the workspace, as data/shared/dem/dtm.tif. */
    public final String path;
    /** {@link #UPLOADED}, {@link #PROCESSED} or, in the history only, {@link #EXTERNAL}. */
    public final String origin;
    /** The user who uploaded the file or ran the job, <code>null</code> if not known. */
    public final String userId;
    /** When the file was written, in milliseconds since the epoch. */
    public final long created;
    /** The job, its module and its parameters, for the processed files, else <code>null</code>. */
    public final String jobId;
    public final String module;
    public final Map<String, String> parameters;
    /** When the file was replaced, in the history only, else <code>null</code>. */
    public final Long replaced;
    /** The origins of the files replaced, newest first; empty in the history. */
    public final List<FileOrigin> history;

    public FileOrigin( String path, String origin, String userId, long created, String jobId, String module,
            Map<String, String> parameters, Long replaced, List<FileOrigin> history ) {
        this.path = path;
        this.origin = origin;
        this.userId = userId;
        this.created = created;
        this.jobId = jobId;
        this.module = module;
        this.parameters = parameters != null ? Collections.unmodifiableMap(new LinkedHashMap<>(parameters)) : null;
        this.replaced = replaced;
        this.history = history != null ? List.copyOf(history) : List.of();
    }

    public static FileOrigin uploaded( String path, String userId, long created ) {
        return new FileOrigin(path, UPLOADED, userId, created, null, null, null, null, null);
    }

    public static FileOrigin processed( String path, String userId, long created, String jobId, String module,
            Map<String, String> parameters ) {
        return new FileOrigin(path, PROCESSED, userId, created, jobId, module, parameters, null, null);
    }

    /**
     * @return this origin with another history.
     */
    public FileOrigin withHistory( List<FileOrigin> history ) {
        return new FileOrigin(path, origin, userId, created, jobId, module, parameters, replaced, history);
    }

    /**
     * @return this origin as entry of the history, replaced at the given time, without its own history.
     */
    public FileOrigin asReplaced( long replacedTime ) {
        return new FileOrigin(path, origin, userId, created, jobId, module, parameters, replacedTime, null);
    }

    /**
     * @return the origin as JSON, without path and history.
     */
    public ObjectNode toJson( ObjectMapper mapper ) {
        ObjectNode json = mapper.createObjectNode().put("origin", origin);
        if (userId != null) {
            json.put("userId", userId);
        }
        json.put("created", created);
        if (jobId != null) {
            json.put("jobId", jobId);
            json.put("module", module);
            json.set("parameters", mapper.valueToTree(parameters));
        }
        if (replaced != null) {
            json.put("replaced", replaced);
        }
        return json;
    }

    /**
     * @return the origin of a JSON written by {@link #toJson(ObjectMapper)}, without history.
     */
    public static FileOrigin fromJson( String path, JsonNode json ) {
        Map<String, String> parameters = null;
        if (json.has("parameters")) {
            parameters = new LinkedHashMap<>();
            for( Map.Entry<String, JsonNode> entry : json.get("parameters").properties() ) {
                parameters.put(entry.getKey(), entry.getValue().asText());
            }
        }
        return new FileOrigin(path, json.path("origin").asText(EXTERNAL), json.hasNonNull("userId") ? json.get("userId").asText() : null,
                json.path("created").asLong(), json.hasNonNull("jobId") ? json.get("jobId").asText() : null,
                json.hasNonNull("module") ? json.get("module").asText() : null, parameters,
                json.hasNonNull("replaced") ? json.get("replaced").asLong() : null, null);
    }
}
