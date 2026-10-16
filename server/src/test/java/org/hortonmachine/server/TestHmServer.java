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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

import junit.framework.TestCase;

/**
 * Tests of the web server.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestHmServer extends TestCase {

    private File workspace;

    @Override
    protected void setUp() throws Exception {
        workspace = new File(Files.createTempDirectory("hm-server-test").toFile(), "workspace");
    }

    @Override
    protected void tearDown() throws Exception {
        workspace.delete();
        workspace.getParentFile().delete();
    }

    public void testArguments() {
        PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

        HmServer server = HmServer.fromArguments(new String[0], out);
        assertEquals(HmServer.DEFAULT_PORT, server.getPort());
        assertEquals(HmServer.DEFAULT_WORKSPACE, server.getWorkspace().getPath());

        server = HmServer.fromArguments(new String[]{"--port=9090", "--workspace=" + workspace.getPath()}, out);
        assertEquals(9090, server.getPort());
        assertEquals(workspace, server.getWorkspace());

        assertNull(HmServer.fromArguments(new String[]{"--help"}, out));
        assertNull(HmServer.fromArguments(new String[]{"--version"}, out));

        assertWrongArguments("--port=abc");
        assertWrongArguments("--port=70000");
        assertWrongArguments("--workspace=");
        assertWrongArguments("--foo=1");
        assertWrongArguments("port");
    }

    private void assertWrongArguments( String... args ) {
        try {
            HmServer.fromArguments(args, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
            fail("Accepted: " + String.join(" ", args));
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testStartAndStop() throws Exception {
        HmServer server = new HmServer(0, workspace);
        server.start();
        try {
            assertTrue(workspace.isDirectory());
            assertTrue(server.getPort() > 0);

            HttpResponse<String> response = HttpClient.newHttpClient().send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + server.getPort() + "/")).build(),
                    HttpResponse.BodyHandlers.ofString());
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("HortonMachine server"));
        } finally {
            server.stop();
        }
    }
}
