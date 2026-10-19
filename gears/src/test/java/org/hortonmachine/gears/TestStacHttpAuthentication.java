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
package org.hortonmachine.gears;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.geotools.http.HTTPResponse;
import org.geotools.http.SimpleHttpClient;
import org.hortonmachine.gears.io.stac.auth.HMHttpAuthentication;
import org.hortonmachine.gears.io.stac.auth.HMStacAccess;
import org.hortonmachine.gears.io.stac.auth.HMStacResponse;
import org.hortonmachine.gears.io.stac.client.HMStacAuthHttpClient;
import org.hortonmachine.gears.utils.HMTestCase;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Tests of the http authentications of the stac catalogs: which requests get the credentials and how.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestStacHttpAuthentication extends HMTestCase {
    private HttpServer server;
    private HttpServer otherServer;
    private String base;
    private String otherBase;
    /** The requests seen by the servers: method, path and query, the Authorization and X-API-Key headers. */
    private final List<String> requests = new ArrayList<>();

    @Override
    protected void setUp() throws Exception {
        server = start();
        otherServer = start();
        base = "http://localhost:" + server.getAddress().getPort();
        otherBase = "http://localhost:" + otherServer.getAddress().getPort();
        server.createContext("/moved", exchange -> {
            exchange.getResponseHeaders().add("Location", otherBase + "/catalog.json");
            exchange.sendResponseHeaders(302, -1);
            exchange.close();
        });
    }

    private HttpServer start() throws IOException {
        HttpServer httpServer = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        httpServer.createContext("/", this::answer);
        httpServer.start();
        return httpServer;
    }

    private synchronized void answer( HttpExchange exchange ) throws IOException {
        String query = exchange.getRequestURI().getRawQuery();
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        String apiKey = exchange.getRequestHeaders().getFirst("X-API-Key");
        requests.add(exchange.getRequestMethod() + " " + exchange.getRequestURI().getPath() + (query != null ? "?" + query : "")
                + " " + authorization + " " + apiKey);
        exchange.getRequestBody().readAllBytes();
        boolean allowed = (authorization != null && !authorization.endsWith("wrong")) || apiKey != null || (query != null && query.contains("api_key="))
                || !exchange.getRequestURI().getPath().startsWith("/private");
        byte[] body = (allowed ? "{\"type\": \"Catalog\"}" : "{\"error\": \"no token\"}").getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(allowed ? 200 : 401, body.length);
        try (OutputStream output = exchange.getResponseBody()) {
            output.write(body);
        }
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop(0);
        otherServer.stop(0);
    }

    private synchronized String lastRequest() {
        return requests.get(requests.size() - 1);
    }

    private String read( HMStacAccess access, String href ) throws IOException {
        try (HMStacResponse response = access.get(href)) {
            return new String(response.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    public void testAppliesOnlyToItsServer() throws Exception {
        HMHttpAuthentication bearer = HMHttpAuthentication.bearer(base + "/api/stac", "secret");
        assertEquals(base, bearer.getBaseUrl());
        assertTrue(bearer.appliesTo(base + "/api/data/files/dtm.tif"));
        assertTrue(bearer.appliesTo(base));
        assertFalse(bearer.appliesTo(base + "0/api"));
        assertFalse(bearer.appliesTo(otherBase + "/api"));
        assertFalse(bearer.appliesTo(base.replace("http:", "https:")));
        assertFalse(bearer.toString().contains("secret"));
        assertTrue(bearer.getHeaders(otherBase + "/x").isEmpty());

        try {
            HMHttpAuthentication.bearer("s3://bucket", "secret");
            fail();
        } catch (IllegalArgumentException e) {
            // only http
        }
        try {
            HMHttpAuthentication.header(base, "Bad Header", "x");
            fail();
        } catch (IllegalArgumentException e) {
            // not a header name
        }
    }

    public void testCredentials() throws Exception {
        HMStacAccess access = new HMStacAccess(HMHttpAuthentication.bearer(base, "secret"));
        assertEquals("{\"type\": \"Catalog\"}", read(access, base + "/private/catalog.json"));
        assertEquals("GET /private/catalog.json Bearer secret null", lastRequest());
        // other servers: without
        read(access, otherBase + "/catalog.json");
        assertEquals("GET /catalog.json null null", lastRequest());

        read(new HMStacAccess(HMHttpAuthentication.basic(base, "jane", "pass:word")), base + "/private/a");
        assertEquals("GET /private/a Basic amFuZTpwYXNzOndvcmQ= null", lastRequest());

        read(new HMStacAccess(HMHttpAuthentication.header(base, "X-API-Key", "key1")), base + "/private/b");
        assertEquals("GET /private/b null key1", lastRequest());

        HMStacAccess parameter = new HMStacAccess(HMHttpAuthentication.queryParameter(base, "api_key", "k&2"));
        read(parameter, base + "/private/c?limit=2#top");
        assertEquals("GET /private/c?limit=2&api_key=k%262 null null", lastRequest());
        assertEquals(base + "/x?api_key=k%262", parameter.toHttpUrl(base + "/x"));
        assertEquals(otherBase + "/x", parameter.toHttpUrl(otherBase + "/x"));

        // refused: the answer of the server in the message
        try {
            read(new HMStacAccess(HMHttpAuthentication.bearer(base, "wrong")), base + "/private/d");
            fail();
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("HTTP 401") && e.getMessage().contains("no token"));
        }
    }

    public void testRedirectToAnotherServer() throws Exception {
        HMStacAccess access = new HMStacAccess(HMHttpAuthentication.bearer(base, "secret"));
        assertEquals("{\"type\": \"Catalog\"}", read(access, base + "/moved"));
        // the token is not given to the other server
        assertEquals("GET /catalog.json null null", lastRequest());
    }

    public void testHttpClient() throws Exception {
        HMStacAuthHttpClient client = new HMStacAuthHttpClient(new SimpleHttpClient(),
                new HMStacAccess(HMHttpAuthentication.bearer(base, "secret")));
        HTTPResponse response = client.get(new URL(base + "/private/search?limit=1"));
        response.getResponseStream().readAllBytes();
        response.dispose();
        assertEquals("GET /private/search?limit=1 Bearer secret null", lastRequest());

        // the POST searches too
        response = client.post(new URL(base + "/private/search"),
                new ByteArrayInputStream("{\"limit\": 1}".getBytes(StandardCharsets.UTF_8)), "application/json");
        response.getResponseStream().readAllBytes();
        response.dispose();
        assertEquals("POST /private/search Bearer secret null", lastRequest());

        response = client.post(new URL(otherBase + "/search"),
                new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8)), "application/json");
        response.getResponseStream().readAllBytes();
        response.dispose();
        assertEquals("POST /search null null", lastRequest());
    }
}
