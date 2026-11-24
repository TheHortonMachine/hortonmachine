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
package org.hortonmachine.gears.io.stac.auth;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Function;

/**
 * Range requests over http(s), as used to read the blocks of cloud optimized GeoTIFFs.
 *
 * <p>The redirects are followed by hand, asking the headers again for each address, so that credentials
 * are never sent to a server they don't apply to.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMHttpRanges {
    private static final int CONNECT_TIMEOUT = 30_000;
    private static final int READ_TIMEOUT = 60_000;
    private static final int MAX_REDIRECTS = 5;
    private static final int MAX_ERROR_CHARS = 500;

    private HMHttpRanges() {
    }

    /**
     * Read a range of bytes.
     *
     * @param url the http(s) address.
     * @param start the first byte.
     * @param end the last byte, inclusive.
     * @param headersForUrl the headers to send to an address (the first one or the target of a redirect), can be null.
     * @return the bytes, fewer than asked if the object ends before.
     * @throws IOException if the request fails.
     */
    public static byte[] readRange( String url, long start, long end, Function<String, Map<String, String>> headersForUrl )
            throws IOException {
        if (end < start)
            throw new IllegalArgumentException("Invalid range " + start + "-" + end);
        String current = url;
        for( int redirects = 0; redirects <= MAX_REDIRECTS; redirects++ ) {
            HttpURLConnection connection = (HttpURLConnection) URI.create(current).toURL().openConnection();
            connection.setConnectTimeout(CONNECT_TIMEOUT);
            connection.setReadTimeout(READ_TIMEOUT);
            connection.setInstanceFollowRedirects(false);
            if (headersForUrl != null) {
                Map<String, String> headers = headersForUrl.apply(current);
                if (headers != null) {
                    for( Map.Entry<String, String> header : headers.entrySet() ) {
                        connection.setRequestProperty(header.getKey(), header.getValue());
                    }
                }
            }
            connection.setRequestProperty("Range", "bytes=" + start + "-" + end);
            int code = connection.getResponseCode();
            if (code >= 300 && code < 400) {
                String location = connection.getHeaderField("Location");
                connection.disconnect();
                if (location == null)
                    throw new IOException("HTTP " + code + " without location reading " + url);
                current = URI.create(current).resolve(location).toString();
                continue;
            }
            if (code >= 400) {
                String message = "";
                try (InputStream error = connection.getErrorStream()) {
                    if (error != null)
                        message = new String(error.readNBytes(MAX_ERROR_CHARS), StandardCharsets.UTF_8);
                }
                throw new HttpRangeException(code, "HTTP " + code + " reading bytes " + start + "-" + end + " of " + url
                        + (message.isEmpty() ? "" : ": " + message));
            }
            int length = (int) (end - start + 1);
            try (InputStream is = connection.getInputStream()) {
                if (code == HttpURLConnection.HTTP_PARTIAL) {
                    return is.readNBytes(length);
                }
                // the server ignored the range and sends the whole object: keep only the part asked
                is.skipNBytes(start);
                return is.readNBytes(length);
            } catch (java.io.EOFException e) {
                return new byte[0];
            }
        }
        throw new IOException("Too many redirects reading " + url);
    }

    /**
     * The error of a range request, with its http status code.
     */
    public static class HttpRangeException extends IOException {
        private static final long serialVersionUID = 1L;
        private final int statusCode;

        public HttpRangeException( int statusCode, String message ) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
