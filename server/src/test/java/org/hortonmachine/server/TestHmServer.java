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
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.ServerConfig.Setting;
import org.hortonmachine.server.db.ServerDb;
import org.hortonmachine.server.users.Role;
import org.hortonmachine.server.users.User;

import junit.framework.TestCase;

/**
 * Tests of the web server and its configuration.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
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

    public void testSettingsFile() throws Exception {
        PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        assertTrue(workspace.mkdirs());
        Map<String, String> settings = new LinkedHashMap<>();
        settings.put("slots", "3");
        settings.put("job-memory", "1g");
        settings.put("unknown", "ignored");
        // only on the command line
        settings.put("db", "postgresql://localhost:5432/hm");
        settings.put("port", "9999");
        ServerConfig.writeSettingsFile(workspace, settings);

        // the command line wins over the file, the file over the defaults
        ServerConfig config = ServerConfig.fromArguments(new String[]{"--workspace=" + workspace.getPath(), "--slots=1"}, out);
        config.applySettingsFile();
        assertEquals(1, config.slots);
        assertEquals("1g", config.jobMemory);
        assertEquals(ServerConfig.DEFAULT_PORT, config.port);
        assertNull(config.db);
        assertEquals(Set.of(Setting.SLOTS), config.fromCommandLine);

        settings.put("job-ttl-days", "-3");
        ServerConfig.writeSettingsFile(workspace, settings);
        try {
            config.applySettingsFile();
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith(ServerConfig.SETTINGS_FILE));
        }
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

    public void testCreateAdmin() throws Exception {
        ServerConfig config = new ServerConfig();
        config.workspace = workspace;
        config.createAdmin = "Boss";
        PrintStream out = new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8);
        assertEquals(HmServer.EXIT_OK, HmServer.createAdmin(config, "boss-password".toCharArray(), out, out));
        // the name is taken now
        assertEquals(HmServer.EXIT_USAGE, HmServer.createAdmin(config, "boss-password".toCharArray(), out, out));
        assertEquals(HmServer.EXIT_USAGE, HmServer.createAdmin(config, "short".toCharArray(), out, out));
    }

    public void testLogin() throws Exception {
        ServerConfig config = new ServerConfig();
        config.port = 0;
        config.workspace = workspace;
        HmServer server = new HmServer(config);
        server.start();
        try {
            assertTrue(new File(workspace, ServerDb.SQLITE_FILE).exists());
            assertTrue(new File(workspace, WorkspacePaths.DATA).isDirectory());
            server.getUserManager().createUser("jane", "jane-password".toCharArray(), Role.USER);
            server.getUserManager().createUser("boss", "boss-password".toCharArray(), Role.ADMIN);

            Browser browser = new Browser(server.getPort());
            // the pages need a login
            HttpResponse<String> response = browser.get("/");
            assertEquals(302, response.statusCode());
            assertEquals("/login", response.headers().firstValue("Location").orElse(""));
            assertTrue(response.headers().firstValue("Content-Security-Policy").orElse("").contains("default-src 'none'"));

            response = browser.get("/login");
            assertEquals(200, response.statusCode());
            String cookie = response.headers().firstValue("Set-Cookie").orElse("");
            assertTrue(cookie, cookie.contains("HttpOnly"));
            assertTrue(cookie, cookie.contains("SameSite=Strict"));

            // forms without the token are refused
            assertEquals(403, browser.post("/login", "username", "jane", "password", "jane-password").statusCode());
            assertEquals(401, browser.login("jane", "wrong-password").statusCode());

            String sessionBefore = browser.sessionId();
            response = browser.login("jane", "jane-password");
            assertEquals(302, response.statusCode());
            assertFalse(sessionBefore.equals(browser.sessionId()));
            assertEquals("/modules", browser.get("/").headers().firstValue("Location").orElse(""));
            response = browser.get("/modules");
            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains(">jane<"));
            // no administration for users
            assertFalse(response.body().contains("/admin/users"));
            assertEquals(403, browser.get("/admin/users").statusCode());

            // log out
            String csrf = Browser.csrf(response);
            assertEquals(302, browser.post("/logout", "csrf", csrf).statusCode());
            assertEquals("/login", browser.get("/modules").headers().firstValue("Location").orElse(""));

            // an administrator gets further, to a page that doesn't exist yet
            assertEquals(302, browser.login("boss", "boss-password").statusCode());
            assertTrue(browser.get("/modules").body().contains("/admin/users"));
            assertEquals(404, browser.get("/admin/nothing").statusCode());

            // a disabled user is logged out at the next request
            User boss = server.getUserManager().listUsers().get(0);
            server.getUserManager().createUser("boss2", "boss-password".toCharArray(), Role.ADMIN);
            server.getUserManager().updateUser(boss.id, Role.ADMIN, true);
            assertEquals("/login", browser.get("/modules").headers().firstValue("Location").orElse(""));
        } finally {
            server.stop();
        }
    }
}
