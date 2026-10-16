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

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.db.ServerDb;

import junit.framework.TestCase;

/**
 * Tests of the web server and its configuration.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestHmServer extends TestCase {

    private File tmpFolder;
    private File workspace;

    @Override
    protected void setUp() throws Exception {
        tmpFolder = Files.createTempDirectory("hm-server-test").toFile();
        workspace = new File(tmpFolder, "workspace");
    }

    @Override
    protected void tearDown() throws Exception {
        FileUtilities.deleteFileOrDir(tmpFolder);
    }

    public void testArguments() {
        PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);

        ServerConfig config = ServerConfig.fromArguments(new String[0], out);
        assertEquals(ServerConfig.DEFAULT_PORT, config.port);
        assertEquals(ServerConfig.DEFAULT_WORKSPACE, config.workspace.getPath());
        assertEquals(ServerConfig.DEFAULT_SLOTS, config.slots);
        assertEquals(ServerConfig.DEFAULT_JOB_MEMORY, config.jobMemory);
        assertEquals(ServerConfig.DEFAULT_JOB_TTL_DAYS, config.jobTtlDays);
        assertNull(config.db);

        config = ServerConfig.fromArguments(new String[]{"--port=9090", "--workspace=" + workspace.getPath(), "--slots=4",
                "--job-memory=512m", "--job-ttl-days=0", "--db=postgresql://localhost:5432/hm", "--db-user=hm"}, out);
        assertEquals(9090, config.port);
        assertEquals(workspace, config.workspace);
        assertEquals(4, config.slots);
        assertEquals("512m", config.jobMemory);
        assertEquals(0, config.jobTtlDays);
        assertEquals("postgresql://localhost:5432/hm", config.db);
        assertEquals("hm", config.dbUser);

        assertNull(ServerConfig.fromArguments(new String[]{"--help"}, out));
        assertNull(ServerConfig.fromArguments(new String[]{"--version"}, out));

        assertWrongArguments("--port=abc");
        assertWrongArguments("--port=70000");
        assertWrongArguments("--workspace=");
        assertWrongArguments("--slots=0");
        assertWrongArguments("--job-memory=lots");
        assertWrongArguments("--job-ttl-days=-1");
        assertWrongArguments("--db=jdbc:postgresql://localhost/hm");
        assertWrongArguments("--foo=1");
        assertWrongArguments("port");
    }

    private void assertWrongArguments( String... args ) {
        try {
            ServerConfig.fromArguments(args, new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
            fail("Accepted: " + String.join(" ", args));
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testStartAndStop() throws Exception {
        ServerConfig config = new ServerConfig();
        config.port = 0;
        config.workspace = workspace;
        HmServer server = new HmServer(config);
        server.start();
        try {
            assertTrue(workspace.isDirectory());
            assertTrue(new File(workspace, ServerDb.SQLITE_FILE).exists());
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
