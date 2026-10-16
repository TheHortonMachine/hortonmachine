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

import java.io.File;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.concurrent.TimeUnit;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.users.Role;

import junit.framework.TestCase;

/**
 * Tests of the pages of the modules and the jobs, as a browser uses them.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestWebPages extends TestCase {

    private File workspace;
    private HmServer server;

    @Override
    protected void setUp() throws Exception {
        workspace = Files.createTempDirectory("hm-web-test").toFile();
        ServerConfig config = new ServerConfig();
        config.port = 0;
        config.workspace = workspace;
        config.slots = 1;
        config.jobMemory = "512m";
        server = new HmServer(config);
        server.start();
        server.getUserManager().createUser("jane", "jane-password".toCharArray(), Role.USER);
        server.getUserManager().createUser("john", "john-password".toCharArray(), Role.USER);
        server.getUserManager().createUser("boss", "boss-password".toCharArray(), Role.ADMIN);
        SampleData.writeElevation(new WorkspacePaths(workspace));
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop();
        FileUtilities.deleteFileOrDir(workspace);
    }

    private Browser login( String username ) throws Exception {
        Browser browser = new Browser(server.getPort());
        assertEquals(302, browser.login(username, username + "-password").statusCode());
        return browser;
    }

    /**
     * Submit the form of the Pitfiller.
     *
     * @return the response.
     */
    private static HttpResponse<String> runPitfiller( Browser browser, String inElev, String outPit ) throws Exception {
        String csrf = Browser.csrf(browser.get("/modules/Pitfiller"));
        return browser.post("/modules/Pitfiller", "csrf", csrf, "inElev", inElev, "outPit", outPit);
    }

    /**
     * @return the id of the job the response redirects to.
     */
    private static String jobId( HttpResponse<String> response ) {
        assertEquals(response.body(), 302, response.statusCode());
        String location = response.headers().firstValue("Location").orElse("");
        assertTrue(location, location.startsWith("/jobs/"));
        return location.substring("/jobs/".length());
    }

    private static HttpResponse<String> waitFinished( Browser browser, String jobId ) throws Exception {
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(120);
        while( System.currentTimeMillis() < end ) {
            HttpResponse<String> page = browser.get("/jobs/" + jobId);
            if (!page.body().contains("http-equiv=\"refresh\"")) {
                return page;
            }
            Thread.sleep(100);
        }
        fail("The job didn't finish: " + jobId);
        return null;
    }

    public void testModulePages() throws Exception {
        Browser jane = login("jane");
        String list = jane.get("/modules").body();
        assertTrue(list.contains("href=\"/modules/Pitfiller\""));
        assertTrue(list.contains("href=\"/modules/Gradient\""));
        String found = jane.get("/modules?q=pitfill").body();
        assertTrue(found.contains("href=\"/modules/Pitfiller\""));
        assertFalse(found.contains("href=\"/modules/Gradient\""));

        String form = jane.get("/modules/Pitfiller").body();
        // the data is suggested for the input, the single output has a name
        assertTrue(form.contains("<option value=\"" + SampleData.ELEVATION + "\">"));
        assertTrue(form.contains("name=\"outPit\" placeholder=\"\" value=\"pit.tif\""));
        assertEquals(404, jane.get("/modules/NoSuchModule").statusCode());

        // choices and checkboxes
        form = jane.get("/modules/Gradient").body();
        assertTrue(form.contains("<option selected>Finite Differences</option>"));
        assertTrue(form.contains("type=\"checkbox\" id=\"p-doDegrees\""));
    }

    public void testRunJob() throws Exception {
        Browser jane = login("jane");
        String jobId = jobId(runPitfiller(jane, SampleData.ELEVATION, "pit.tif"));
        String page = waitFinished(jane, jobId).body();
        assertTrue(page, page.contains("state-DONE"));
        assertTrue(page.contains("href=\"/jobs/" + jobId + "/files/pit.tif\""));
        assertTrue(page.contains("Download all as zip"));

        // the downloads
        File pit = new File(workspace, "jobs/" + jobId + "/pit.tif");
        HttpResponse<byte[]> download = jane.getBytes("/jobs/" + jobId + "/files/pit.tif");
        assertEquals(200, download.statusCode());
        assertEquals(pit.length(), download.body().length);
        assertTrue(download.headers().firstValue("Content-Disposition").orElse("").contains("attachment"));
        HttpResponse<byte[]> zip = jane.getBytes("/jobs/" + jobId + "/zip");
        assertEquals(200, zip.statusCode());
        assertEquals('P', zip.body()[0]);
        assertEquals('K', zip.body()[1]);
        assertEquals(404, jane.get("/jobs/" + jobId + "/files/params.json").statusCode());
        assertEquals(404, jane.get("/jobs/" + jobId + "/files/nothing.tif").statusCode());
        assertFalse(200 == jane.get("/jobs/" + jobId + "/files/%2e%2e/%2e%2e/hm-server.sqlite").statusCode());

        assertTrue(jane.get("/jobs").body().contains("href=\"/jobs/" + jobId + "\""));

        // the output is suggested for the next jobs, and the job can run again
        assertTrue(jane.get("/modules/Pitfiller").body().contains("<option value=\"jobs/" + jobId + "/pit.tif\">"));
        String again = jane.get("/modules/Pitfiller?from=" + jobId).body();
        assertTrue(again.contains("value=\"" + SampleData.ELEVATION + "\""));

        // other users don't see it, administrators do
        Browser john = login("john");
        assertEquals(404, john.get("/jobs/" + jobId).statusCode());
        assertEquals(404, john.get("/jobs/" + jobId + "/files/pit.tif").statusCode());
        assertFalse(john.get("/jobs").body().contains(jobId));
        assertFalse(john.get("/modules/Pitfiller?from=" + jobId).body().contains(SampleData.ELEVATION + "\"" + " placeholder"));
        String csrf = Browser.csrf(john.get("/jobs"));
        assertEquals(404, john.post("/jobs/" + jobId + "/kill", "csrf", csrf).statusCode());
        Browser boss = login("boss");
        assertEquals(200, boss.get("/jobs/" + jobId).statusCode());
        String all = boss.get("/jobs").body();
        assertTrue(all.contains(jobId));
        assertTrue(all.contains("<td>jane</td>"));
    }

    public void testWrongValues() throws Exception {
        Browser jane = login("jane");
        HttpResponse<String> response = runPitfiller(jane, "/etc/passwd", "<b>pit</b>.tif");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("relative to the workspace"));
        // the values are shown again, escaped
        assertTrue(response.body().contains("value=\"&lt;b&gt;pit&lt;/b&gt;.tif\""));
        assertFalse(response.body().contains("<b>pit</b>"));
        assertTrue(server.getJobManager().listJobs(null, 10).isEmpty());
    }

    public void testKill() throws Exception {
        Browser jane = login("jane");
        // with one slot the second job waits
        String first = jobId(runPitfiller(jane, SampleData.ELEVATION, "pit.tif"));
        String second = jobId(runPitfiller(jane, SampleData.ELEVATION, "pit.tif"));
        String page = jane.get("/jobs/" + second).body();
        assertTrue(page.contains("http-equiv=\"refresh\""));

        HttpResponse<String> response = jane.post("/jobs/" + second + "/kill", "csrf", Browser.csrf(jane.get("/jobs")), "back",
                "/jobs/" + second);
        assertEquals(302, response.statusCode());
        assertEquals("/jobs/" + second, response.headers().firstValue("Location").orElse(""));
        assertTrue(waitFinished(jane, second).body().contains("state-KILLED"));

        // no redirect elsewhere
        response = jane.post("/jobs/" + first + "/kill", "csrf", Browser.csrf(jane.get("/jobs")), "back", "https://example.org");
        assertEquals("/jobs", response.headers().firstValue("Location").orElse(""));
        waitFinished(jane, first);
    }
}
