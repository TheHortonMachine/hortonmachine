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

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.web.NotebookAuth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

/**
 * Tests of the notebook server: the token access, the localhost protection, the pages, the runs of
 * the cells in a kernel process and the saved files.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestNotebookPages extends TestCase {

    private static final String NOTEBOOK = "/notebooks/first%20test";

    private final ObjectMapper mapper = new ObjectMapper();
    private File tmpFolder;
    private File notebooksFolder;
    private HmServer server;

    @Override
    protected void setUp() throws Exception {
        tmpFolder = Files.createTempDirectory("hm-notebook-test").toFile();
        notebooksFolder = new File(tmpFolder, "my notebooks");
        ServerConfig config = ServerConfig.fromArguments(new String[]{"--notebooks=true", "--port=0", "--job-memory=512m",
                "--workspace=" + new File(tmpFolder, "workspace").getPath(), "--notebooks-folder=" + notebooksFolder.getPath()},
                new PrintStream(new ByteArrayOutputStream(), true, StandardCharsets.UTF_8));
        server = new HmServer(config);
        server.start();
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop();
        FileUtilities.deleteFileOrDir(tmpFolder);
    }

    /**
     * @return a browser that opened the link with the token.
     */
    private Browser open() throws Exception {
        Browser browser = new Browser(server.getPort());
        HttpResponse<String> response = browser.get("/notebooks?" + NotebookAuth.TOKEN_PARAM + "=" + server.getNotebookToken());
        assertEquals(302, response.statusCode());
        // the token doesn't stay in the address
        assertEquals("/notebooks", response.headers().firstValue("Location").orElse(""));
        return browser;
    }

    /**
     * @return the first event of a type for a cell, skipping the others.
     */
    private JsonNode waitFor( BlockingQueue<String> events, String type, String cell ) throws Exception {
        while( true ) {
            String data = events.poll(60, TimeUnit.SECONDS);
            assertNotNull("no event " + type + " for " + cell, data);
            JsonNode event = mapper.readTree(data);
            if (event.path("type").asText().equals(type) && (cell == null || event.path("cell").asText().equals(cell))) {
                return event;
            }
        }
    }

    public void testToken() throws Exception {
        assertTrue(server.getNotebookUrl().startsWith("http://localhost:" + server.getPort() + "/?token="));

        Browser stranger = new Browser(server.getPort());
        assertEquals(401, stranger.get("/notebooks").statusCode());
        assertEquals(401, stranger.get("/?token=wrong").statusCode());
        assertEquals(401, stranger.get("/notebooks").statusCode());

        Browser owner = open();
        HttpResponse<String> list = owner.get("/notebooks");
        assertEquals(200, list.statusCode());
        // only the notebooks in the menu
        assertTrue(list.body().contains("href=\"/notebooks\">Notebooks</a>"));
        assertFalse(list.body().contains("/modules"));
        assertEquals(302, owner.get("/").statusCode());

        // the pages of the server of the modules don't exist
        for( String path : new String[]{"/modules", "/jobs", "/uploads", "/account", "/admin/users", "/api", "/api/stac",
                "/login"} ) {
            assertEquals(path, 404, owner.get(path).statusCode());
        }
        // the static files are open, as the style
        assertEquals(200, stranger.get("/static/hm.css").statusCode());
    }

    public void testOnlyLocalRequests() throws Exception {
        // a request for another host, as a rebound dns name, even with the token
        try (Socket socket = new Socket("127.0.0.1", server.getPort())) {
            OutputStream out = socket.getOutputStream();
            out.write(("GET /?token=" + server.getNotebookToken() + " HTTP/1.1\r\nHost: evil.example.com\r\nConnection: close\r\n\r\n")
                    .getBytes(StandardCharsets.US_ASCII));
            out.flush();
            String status = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
                    .readLine();
            assertTrue(status, status.contains(" 403 "));
        }

        // a request from another site
        Browser owner = open();
        assertEquals(403, owner.getWithHeaders("/notebooks", "Origin", "http://evil.example.com").statusCode());
        assertEquals(200, owner.getWithHeaders("/notebooks", "Origin", "http://localhost:" + server.getPort()).statusCode());

        // nothing listens on the other addresses of the computer
        for( NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces()) ) {
            for( InetAddress address : Collections.list(networkInterface.getInetAddresses()) ) {
                if (address instanceof Inet4Address && !address.isLoopbackAddress()) {
                    try (Socket socket = new Socket()) {
                        socket.connect(new InetSocketAddress(address, server.getPort()), 2000);
                        fail("The server answers on " + address);
                    } catch (java.io.IOException e) {
                        // refused, as it should
                    }
                }
            }
        }
    }

    public void testPagesAndSecurityHeaders() throws Exception {
        Browser owner = open();
        HttpResponse<String> list = owner.get("/notebooks");
        assertEquals(302, owner.post("/notebooks", "csrf", Browser.csrf(list), "name", "first test").statusCode());
        assertTrue(owner.get("/notebooks").body().contains(">first test</a>"));
        assertTrue(new File(notebooksFolder, "first test.ipynb").isFile());

        // the editor is the only page with scripts, all from the server
        HttpResponse<String> editor = owner.get(NOTEBOOK);
        assertEquals(200, editor.statusCode());
        String csp = editor.headers().firstValue("Content-Security-Policy").orElse("");
        assertTrue(csp, csp.contains("script-src 'self'") && !csp.contains("unsafe"));
        assertTrue(list.headers().firstValue("Content-Security-Policy").orElse("").startsWith("default-src 'none'"));
        HttpResponse<String> script = owner.get("/static/notebook/codemirror.js");
        assertEquals(200, script.statusCode());
        assertTrue(script.headers().firstValue("Content-Type").orElse("").startsWith("text/javascript"));
        assertEquals(404, owner.get("/static/notebook/other.js").statusCode());

        // names that could leave the folder
        assertEquals(400, owner.post("/notebooks", "csrf", Browser.csrf(list), "name", "../escape").statusCode());
        // the posts need the token of the forms
        assertEquals(403, owner.post("/notebooks", "name", "other").statusCode());
    }

    public void testRunSaveAndDownload() throws Exception {
        Browser owner = open();
        String csrf = Browser.csrf(owner.get("/notebooks"));
        owner.post("/notebooks", "csrf", csrf, "name", "first test");

        BlockingQueue<String> events = owner.events(NOTEBOOK + "/events");
        assertEquals("off", waitFor(events, "status", null).path("state").asText());

        assertEquals(202, owner.post(NOTEBOOK + "/run", "csrf", csrf, "cell", "c1", "code", "def a = 20\nprintln 'set'").statusCode());
        assertEquals(202, owner.post(NOTEBOOK + "/run", "csrf", csrf, "cell", "c2", "code", "a * 2 + 2").statusCode());
        JsonNode output = waitFor(events, "output", "c1");
        assertEquals("set\n", output.path("output").path("text").asText());
        assertEquals("ok", waitFor(events, "done", "c1").path("status").asText());
        output = waitFor(events, "output", "c2");
        assertEquals("42", output.path("output").path("data").path("text/plain").asText());
        assertEquals("ok", waitFor(events, "done", "c2").path("status").asText());

        // the relative paths are those of the notebooks folder
        owner.post(NOTEBOOK + "/run", "csrf", csrf, "cell", "c5", "code", "new File('.').canonicalPath");
        output = waitFor(events, "output", "c5");
        assertEquals(notebooksFolder.getCanonicalPath(), output.path("output").path("data").path("text/plain").asText());

        // saved with the outputs, in the order of the page
        String cells = "[{\"id\":\"m1\",\"type\":\"markdown\",\"source\":\"# Title\"},"
                + "{\"id\":\"c1\",\"type\":\"code\",\"source\":\"def a = 20\\nprintln 'set'\"},"
                + "{\"id\":\"c2\",\"type\":\"code\",\"source\":\"a * 2 + 2\"}]";
        assertEquals(204, owner.post(NOTEBOOK + "/save", "csrf", csrf, "cells", cells).statusCode());
        JsonNode saved = mapper.readTree(new File(notebooksFolder, "first test.ipynb"));
        assertEquals(4, saved.path("nbformat").asInt());
        assertEquals("groovy", saved.path("metadata").path("kernelspec").path("language").asText());
        assertEquals("markdown", saved.path("cells").get(0).path("cell_type").asText());
        JsonNode c2 = saved.path("cells").get(2);
        assertEquals(2, c2.path("execution_count").asInt());
        assertEquals("42", c2.path("outputs").get(0).path("data").path("text/plain").asText());

        HttpResponse<String> download = owner.get(NOTEBOOK + "/download");
        assertEquals(200, download.statusCode());
        assertTrue(download.headers().firstValue("Content-Disposition").orElse("").contains("first%20test.ipynb"));
        assertEquals(saved, mapper.readTree(download.body()));

        JsonNode content = mapper.readTree(owner.get(NOTEBOOK + "/content").body());
        assertEquals("idle", content.path("state").asText());
        assertEquals(3, content.path("cells").size());

        // the completion knows the variables of the cells that ran
        HttpResponse<String> completions = owner.post(NOTEBOOK + "/complete", "csrf", csrf, "code", "a.intV", "cursor", "6");
        assertEquals(200, completions.statusCode());
        JsonNode result = mapper.readTree(completions.body());
        assertEquals(2, result.path("from").asInt());
        assertEquals("intValue", result.path("items").get(0).path("name").asText());
        assertEquals(400, owner.post(NOTEBOOK + "/complete", "csrf", csrf, "code", "a", "cursor", "9").statusCode());

        // a cell that never ends
        owner.post(NOTEBOOK + "/run", "csrf", csrf, "cell", "c3", "code", "while( true ) { }");
        waitFor(events, "start", "c3");
        assertEquals(204, owner.post(NOTEBOOK + "/interrupt", "csrf", csrf).statusCode());
        assertEquals("interrupted", waitFor(events, "done", "c3").path("status").asText());

        // a restart forgets the variables
        assertEquals(204, owner.post(NOTEBOOK + "/restart", "csrf", csrf).statusCode());
        owner.post(NOTEBOOK + "/run", "csrf", csrf, "cell", "c4", "code", "a");
        output = waitFor(events, "output", "c4");
        assertEquals("error", output.path("output").path("output_type").asText());

        // the delete stops the kernel and removes the file
        assertEquals(302, owner.post(NOTEBOOK + "/delete", "csrf", csrf).statusCode());
        assertFalse(new File(notebooksFolder, "first test.ipynb").exists());
    }

    public void testNotInTheServerOfTheModules() throws Exception {
        server.stop();
        ServerConfig config = new ServerConfig();
        config.port = 0;
        config.workspace = new File(tmpFolder, "workspace");
        server = new HmServer(config);
        server.start();
        assertNull(server.getNotebookToken());
        Browser browser = new Browser(server.getPort());
        // the login page, not the notebooks
        assertEquals(302, browser.get("/notebooks").statusCode());
        assertEquals("/login", browser.get("/notebooks").headers().firstValue("Location").orElse(""));

        try {
            ServerConfig.fromArguments(new String[]{"--notebooks-folder=x"}, System.out);
            fail();
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("--notebooks=true"));
        }
    }
}
