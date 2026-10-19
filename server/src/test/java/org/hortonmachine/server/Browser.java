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

import static junit.framework.TestCase.assertTrue;

import java.io.ByteArrayOutputStream;
import java.net.CookieManager;
import java.net.HttpCookie;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hortonmachine.server.web.Auth;

/**
 * A browser for the tests: keeps the cookies, doesn't follow redirects.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class Browser {
    private final HttpClient client = HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    private final String base;

    public Browser( int port ) {
        base = "http://localhost:" + port;
    }

    public HttpResponse<String> get( String path ) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofString());
    }

    public HttpResponse<byte[]> getBytes( String path ) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    /**
     * Post a form.
     *
     * @param path the path.
     * @param namesAndValues the names and values of the fields, a name can repeat.
     */
    public HttpResponse<String> post( String path, String... namesAndValues ) throws Exception {
        StringBuilder form = new StringBuilder();
        for( int i = 0; i < namesAndValues.length; i += 2 ) {
            if (form.length() > 0) {
                form.append('&');
            }
            form.append(URLEncoder.encode(namesAndValues[i], StandardCharsets.UTF_8)).append('=')
                    .append(URLEncoder.encode(namesAndValues[i + 1], StandardCharsets.UTF_8));
        }
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/x-www-form-urlencoded").POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Post a multipart form, as the upload forms.
     *
     * @param path the path.
     * @param fields the names and values of the text fields.
     * @param fileField the name of the files field.
     * @param files the names and contents of the files.
     */
    public HttpResponse<String> postMultipart( String path, Map<String, String> fields, String fileField, Map<String, byte[]> files )
            throws Exception {
        String boundary = "----hm-test-" + UUID.randomUUID();
        ByteArrayOutputStream body = new ByteArrayOutputStream();
        for( Map.Entry<String, String> field : fields.entrySet() ) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + field.getKey() + "\"\r\n\r\n"
                    + field.getValue() + "\r\n").getBytes(StandardCharsets.UTF_8));
        }
        for( Map.Entry<String, byte[]> file : files.entrySet() ) {
            body.write(("--" + boundary + "\r\nContent-Disposition: form-data; name=\"" + fileField + "\"; filename=\"" + file.getKey()
                    + "\"\r\nContent-Type: application/octet-stream\r\n\r\n").getBytes(StandardCharsets.UTF_8));
            body.write(file.getValue());
            body.write("\r\n".getBytes(StandardCharsets.UTF_8));
        }
        body.write(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())).build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * @return the token of the forms of a page.
     */
    public static String csrf( HttpResponse<String> page ) {
        Matcher matcher = Pattern.compile("name=\"csrf\" value=\"([^\"]+)\"").matcher(page.body());
        assertTrue(page.body(), matcher.find());
        return matcher.group(1);
    }

    public String sessionId() {
        for( HttpCookie cookie : ((CookieManager) client.cookieHandler().get()).getCookieStore().getCookies() ) {
            if (cookie.getName().equals(Auth.SESSION_COOKIE)) {
                return cookie.getValue();
            }
        }
        return null;
    }

    public HttpResponse<String> login( String username, String password ) throws Exception {
        String csrf = csrf(get("/login"));
        return post("/login", "csrf", csrf, "username", username, "password", password);
    }
}
