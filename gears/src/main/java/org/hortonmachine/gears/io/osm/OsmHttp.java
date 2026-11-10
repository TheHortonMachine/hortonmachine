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
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The http requests and the small caches shared by the OpenStreetMap clients.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
class OsmHttp {
    private static final int MAX_ERROR_LENGTH = 500;

    static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(20))
            .followRedirects(HttpClient.Redirect.NORMAL).build();

    private OsmHttp() {
    }

    /**
     * Send a request and return the body of the response.
     *
     * @param request the request.
     * @param serviceName the name of the service, for the messages.
     * @return the body of the response, if its status is 200.
     * @throws IOException with a readable message for the other status codes.
     */
    static String send( HttpRequest request, String serviceName ) throws IOException {
        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException(serviceName + " request interrupted.", e);
        } catch (HttpTimeoutException e) {
            throw new IOException(serviceName + " did not answer in time: it is too busy or the request is too big, "
                    + "retry later, ask for less data or use another server.", e);
        }
        int status = response.statusCode();
        if (status == 200) {
            return response.body();
        }
        String text = toPlainText(response.body());
        switch( status ) {
        case 400:
            throw new IOException(serviceName + " rejected the request: " + text);
        case 429:
            throw new IOException(serviceName + " refused the request because of too many requests, retry later.");
        case 502:
        case 503:
        case 504:
            // the public Overpass servers answer 502 when all their workers are busy
            throw new IOException(serviceName + " is too busy or the request took too long, retry later or ask for less data.");
        default:
            throw new IOException(serviceName + " answered with status " + status + (text.isEmpty() ? "." : ": " + text));
        }
    }

    /**
     * @return the text of an html or plain error page, without tags, on a single line and shortened.
     */
    static String toPlainText( String body ) {
        if (body == null) {
            return "";
        }
        String text = body.replaceAll("(?s)<(script|style|head)[^>]*>.*?</\\1>", " ").replaceAll("<[^>]+>", " ")
                .replace("&quot;", "\"").replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&")
                .replaceAll("\\s+", " ").trim();
        if (text.length() > MAX_ERROR_LENGTH) {
            text = text.substring(0, MAX_ERROR_LENGTH) + "...";
        }
        return text;
    }

    /**
     * @return a map that keeps its last <code>maxSize</code> accessed entries.
     */
    static <K, V> Map<K, V> lruCache( int maxSize ) {
        return new LinkedHashMap<K, V>(16, 0.75f, true){
            private static final long serialVersionUID = 1L;

            @Override
            protected boolean removeEldestEntry( Map.Entry<K, V> eldest ) {
                return size() > maxSize;
            }
        };
    }
}
