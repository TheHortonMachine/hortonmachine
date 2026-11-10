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
package org.hortonmachine.gears.io.osm;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;

import org.hortonmachine.Version;

/**
 * A client of the Overpass API, to download OpenStreetMap data as features.
 *
 * <p>The queries are written in Overpass QL and must ask for json, as with
 * <code>[out:json];</code> at their start. The geometries are read from <code>out geom;</code>
 * or from the nodes and ways of <code>out body; &gt;; out skel qt;</code>. The answers of the
 * last queries are cached, to not load the public servers again with the same query.</p>
 *
 * <pre>
 * [out:json][timeout:60];
 * area["name"="Roma"]["admin_level"="8"]-&gt;.rome;
 * relation["boundary"="administrative"]["admin_level"="9"](area.rome);
 * out geom;
 * </pre>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OverpassClient {
    /** The main public server. */
    public static final String DEFAULT_URL = "https://overpass-api.de/api/interpreter";

    private static final int CACHE_SIZE = 20;
    private static final String SERVICE_NAME = "Overpass";

    private static final Map<String, String> CACHE = OsmHttp.lruCache(CACHE_SIZE);

    private final String url;
    private int timeoutSeconds = 180;

    /**
     * A client of the main public server.
     */
    public OverpassClient() {
        this(DEFAULT_URL);
    }

    /**
     * @param url the url of the interpreter of the server.
     */
    public OverpassClient( String url ) {
        this.url = url;
    }

    /**
     * @param timeoutSeconds the time to wait for the answer of a query, 180 seconds by default.
     *          The time the server gives to the query is set in the query itself, as with
     *          <code>[timeout:60]</code>.
     */
    public void setTimeoutSeconds( int timeoutSeconds ) {
        this.timeoutSeconds = timeoutSeconds;
    }

    /**
     * Run a query.
     *
     * @param overpassQl the query, in Overpass QL with json output.
     * @return the features read from the answer.
     * @throws Exception if the server rejects the query, or if it stops it with an error and
     *          without returning data, as for a timeout.
     */
    public OverpassResult query( String overpassQl ) throws Exception {
        String cacheKey = url + "\n" + overpassQl;
        String json;
        synchronized (CACHE) {
            json = CACHE.get(cacheKey);
        }
        if (json == null) {
            String body = "data=" + URLEncoder.encode(overpassQl, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", Version.USER_AGENT)
                    .header("Content-Type", "application/x-www-form-urlencoded").timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
            json = OsmHttp.send(request, SERVICE_NAME);
            if (!json.trim().startsWith("{")) {
                throw new IOException(
                        "Overpass did not answer with json: start the query with [out:json]; " + OsmHttp.toPlainText(json));
            }
        }
        OverpassResult result = OverpassResult.parse(json);
        if (result.isEmpty() && result.getRemark() != null && result.getRemark().contains("error")) {
            throw new IOException("Overpass stopped the query: " + result.getRemark());
        }
        // only the good answers are kept
        synchronized (CACHE) {
            CACHE.put(cacheKey, json);
        }
        return result;
    }
}
