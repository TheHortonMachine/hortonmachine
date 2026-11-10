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

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.hortonmachine.Version;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Envelope;

/**
 * A client of the Nominatim geocoding service of OpenStreetMap, to find places by name.
 *
 * <p>The requests are sent with the project's {@link Version#USER_AGENT} and at most one per
 * second, as asked by the usage policy of the public server, and the last results are cached.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NominatimClient {
    /** The public server of OpenStreetMap. */
    public static final String DEFAULT_URL = "https://nominatim.openstreetmap.org";

    private static final long MIN_REQUEST_INTERVAL_MILLIS = 1000;
    private static final int CACHE_SIZE = 50;
    private static final String SERVICE_NAME = "Nominatim";

    /** The time of the last request to any server, for the one request per second policy. */
    private static long lastRequestMillis = 0;
    private static final Map<String, List<NominatimPlace>> CACHE = OsmHttp.lruCache(CACHE_SIZE);

    private final String baseUrl;

    /**
     * A client of the public server.
     */
    public NominatimClient() {
        this(DEFAULT_URL);
    }

    /**
     * @param baseUrl the url of the server, without the <code>/search</code> path.
     */
    public NominatimClient( String baseUrl ) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    /**
     * Find places by name.
     *
     * @param query the name of the place, as "Roma" or "Piazza Navona, Roma".
     * @param limit the maximum number of places returned.
     * @return the places found, the best first.
     * @throws Exception
     */
    public List<NominatimPlace> search( String query, int limit ) throws Exception {
        String url = baseUrl + "/search?format=jsonv2&limit=" + limit + "&q=" + URLEncoder.encode(query, StandardCharsets.UTF_8);
        synchronized (CACHE) {
            List<NominatimPlace> cached = CACHE.get(url);
            if (cached != null) {
                return cached;
            }
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).header("User-Agent", Version.USER_AGENT)
                .timeout(Duration.ofSeconds(30)).GET().build();
        waitForTurn();
        String body = OsmHttp.send(request, SERVICE_NAME);
        List<NominatimPlace> places = List.copyOf(parse(body));
        synchronized (CACHE) {
            CACHE.put(url, places);
        }
        return places;
    }

    /**
     * Wait until a second has passed since the last request.
     */
    private static synchronized void waitForTurn() throws InterruptedException {
        long wait = lastRequestMillis + MIN_REQUEST_INTERVAL_MILLIS - System.currentTimeMillis();
        if (wait > 0) {
            Thread.sleep(wait);
        }
        lastRequestMillis = System.currentTimeMillis();
    }

    /**
     * Parse the json of a search, in the <code>jsonv2</code> or <code>json</code> format.
     *
     * @param json the answer of the server.
     * @return the places.
     */
    static List<NominatimPlace> parse( String json ) {
        JSONArray array = new JSONArray(json);
        List<NominatimPlace> places = new ArrayList<>();
        for( int i = 0; i < array.length(); i++ ) {
            JSONObject item = array.getJSONObject(i);
            Envelope bbox = null;
            JSONArray bb = item.optJSONArray("boundingbox");
            if (bb != null && bb.length() == 4) {
                // south, north, west, east
                bbox = new Envelope(bb.getDouble(2), bb.getDouble(3), bb.getDouble(0), bb.getDouble(1));
            }
            // jsonv2 calls category what json calls class
            String category = item.optString("category", item.optString("class", ""));
            places.add(new NominatimPlace(item.optString("display_name", ""), item.optString("osm_type", ""),
                    item.optLong("osm_id", -1), category, item.optString("type", ""), item.getDouble("lon"),
                    item.getDouble("lat"), bbox));
        }
        return places;
    }
}
