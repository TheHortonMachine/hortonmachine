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
package org.hortonmachine.gears.io.opendap;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;

import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.files.FileUtilities;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

/**
 * Test {@link OpendapSession} with a local server that works as the Earthdata login: the data server, reached as
 * 127.0.0.1, redirects to the login server, reached as localhost, which takes the credentials and redirects back to
 * set a session cookie.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestOpendapSession extends HMTestCase {

    private static final String USER = "hmuser";
    private static final String PASSWORD = "hm secret";
    private static final String TOKEN = "hmtoken";

    /** A request as received by the server: host, path and authorization header. */
    private record Request( String host, String path, String authorization ) {
    }

    private HttpServer server;
    private int port;
    private final List<Request> requests = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void setUp() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        port = server.getAddress().getPort();
        String basic = "Basic " + Base64.getEncoder().encodeToString((USER + ":" + PASSWORD).getBytes(StandardCharsets.UTF_8));

        server.createContext("/", exchange -> {
            String host = exchange.getRequestHeaders().getFirst("Host").replaceAll(":\\d+$", "");
            String path = exchange.getRequestURI().getPath();
            String authorization = exchange.getRequestHeaders().getFirst("Authorization");
            requests.add(new Request(host, path, authorization));
            String cookie = exchange.getRequestHeaders().getFirst("Cookie");
            switch( path ) {
            case "/data":
                if (cookie != null && cookie.contains("session=ok")) {
                    respond(exchange, 200, "text/plain", "DATA");
                } else {
                    redirect(exchange, "http://localhost:" + port + "/oauth");
                }
                break;
            case "/oauth":
                if (basic.equals(authorization)) {
                    redirect(exchange, "http://127.0.0.1:" + port + "/callback");
                } else {
                    exchange.getResponseHeaders().add("WWW-Authenticate", "Basic realm=\"test\"");
                    respond(exchange, 401, "text/plain", "HTTP Basic: Access denied.");
                }
                break;
            case "/callback":
                exchange.getResponseHeaders().add("Set-Cookie", "session=ok; Path=/");
                redirect(exchange, "/data");
                break;
            case "/tokendata":
                if (("Bearer " + TOKEN).equals(authorization)) {
                    respond(exchange, 200, "text/plain", "TOKENDATA");
                } else {
                    respond(exchange, 401, "text/plain", "no token");
                }
                break;
            case "/tokenredirect":
                redirect(exchange, "http://localhost:" + port + "/elsewhere");
                break;
            case "/elsewhere":
                respond(exchange, 200, "text/plain", "ELSEWHERE");
                break;
            case "/html":
                respond(exchange, 200, "text/html", "<html><body>Please log in</body></html>");
                break;
            default:
                respond(exchange, 404, "text/plain", "not found");
            }
        });
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop(0);
    }

    private static void redirect( HttpExchange exchange, String location ) throws IOException {
        exchange.getResponseHeaders().add("Location", location);
        exchange.sendResponseHeaders(302, -1);
        exchange.close();
    }

    private static void respond( HttpExchange exchange, int status, String type, String body ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", type);
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    public void testLoginFlowSendsCredentialsOnlyToTheLoginHost() throws Exception {
        OpendapSession session = OpendapSession.withCredentials("localhost", USER, PASSWORD);
        assertEquals("DATA", session.getText("http://127.0.0.1:" + port + "/data"));

        List<String> paths = requests.stream().map(Request::path).toList();
        assertEquals(List.of("/data", "/oauth", "/callback", "/data"), paths);
        for( Request request : requests ) {
            if (request.host().equals("localhost")) {
                assertNotNull(request.authorization());
            } else {
                assertNull("The credentials must not reach the data server.", request.authorization());
            }
        }

        // the session cookie is kept: no more logins
        requests.clear();
        File file = File.createTempFile("hm_opendap_session", ".txt");
        try {
            assertEquals(4, session.download("http://127.0.0.1:" + port + "/data", file, null));
            assertEquals("DATA", FileUtilities.readFile(file).trim());
            assertEquals(List.of("/data"), requests.stream().map(Request::path).toList());
        } finally {
            file.delete();
        }
    }

    public void testWrongCredentials() throws Exception {
        OpendapSession session = OpendapSession.withCredentials("localhost", USER, "wrong");
        try {
            session.getText("http://127.0.0.1:" + port + "/data");
            fail("Wrong credentials must fail.");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("401"));
        }
        // without credentials the login is never answered
        try {
            OpendapSession.anonymous().getText("http://127.0.0.1:" + port + "/data");
            fail("Missing credentials must fail.");
        } catch (IOException e) {
            assertTrue(e.getMessage(), e.getMessage().contains("401"));
        }
    }

    public void testTokenOnlyToTheRequestedServer() throws Exception {
        OpendapSession session = OpendapSession.withToken(TOKEN);
        assertEquals("TOKENDATA", session.getText("http://127.0.0.1:" + port + "/tokendata"));

        requests.clear();
        assertEquals("ELSEWHERE", session.getText("http://127.0.0.1:" + port + "/tokenredirect"));
        assertEquals("Bearer " + TOKEN, requests.get(0).authorization());
        assertEquals("/elsewhere", requests.get(1).path());
        assertNull("The token must not follow a redirect to another server.", requests.get(1).authorization());
    }

    public void testWebPageInsteadOfData() throws Exception {
        File file = File.createTempFile("hm_opendap_session", ".nc4");
        file.delete();
        try {
            OpendapSession.anonymous().download("http://127.0.0.1:" + port + "/html", file, null);
            fail("A web page must not be taken as data.");
        } catch (IOException e) {
            assertFalse(file.exists());
        }
    }

    public void testNetrc() throws Exception {
        File netrc = File.createTempFile("hm_netrc", "");
        try {
            FileUtilities.writeFile("machine other.host login a password b\n" //
                    + "machine localhost\n  login " + USER + "\n  password hm\n", netrc);
            // the netrc password is not the one of the server: the login fails, but after sending the
            // credentials of the localhost entry to the login host
            OpendapSession session = OpendapSession.fromNetrc(netrc, "localhost");
            try {
                session.getText("http://127.0.0.1:" + port + "/data");
                fail("The password of the netrc is not the right one.");
            } catch (IOException e) {
                // the credentials were sent to the login host
                assertTrue(requests.stream().anyMatch(r -> r.path().equals("/oauth") && r.authorization() != null));
            }

            try {
                OpendapSession.fromNetrc(netrc, "missing.host");
                fail("A missing host must fail.");
            } catch (IOException e) {
                // expected
            }

            // the default entry
            FileUtilities.writeFile("machine other.host login a password b\ndefault login c password d\n", netrc);
            assertNotNull(OpendapSession.fromNetrc(netrc, "missing.host"));
        } finally {
            netrc.delete();
        }
    }
}
