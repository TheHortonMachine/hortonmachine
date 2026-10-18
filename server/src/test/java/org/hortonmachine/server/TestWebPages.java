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
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.ServerConfig.Setting;
import org.hortonmachine.server.users.Role;
import org.hortonmachine.server.web.JobPages;
import org.hortonmachine.server.web.UploadPages;

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
        config.maxUploadMb = 1;
        config.userQuotaMb = 2;
        config.maxSuggestions = 10;
        // as if given on the command line
        config.fromCommandLine.add(Setting.JOB_TTL_DAYS);
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
            assertEquals(page.body(), 200, page.statusCode());
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
        // grouped by folder, or alphabetic keeping the search
        assertTrue(found.contains("<b>by folder</b>"));
        assertTrue(found, found.contains("href=\"/modules?view&#61;list&amp;q&#61;pitfill\""));
        String alphabetic = jane.get("/modules?view=list&q=pitfill").body();
        assertTrue(alphabetic.contains("<b>alphabetic</b>"));
        assertTrue(alphabetic.contains("name=\"view\" value=\"list\""));
        assertTrue(alphabetic.contains("href=\"/modules/Pitfiller\""));
        assertFalse(alphabetic.contains("<h2>"));

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

        // the output is suggested for the next jobs, with its module, also with much data
        for( int i = 0; i < 20; i++ ) {
            Files.write(new File(workspace, "data/a" + i + ".tif").toPath(), new byte[1]);
        }
        String form = jane.get("/modules/Pitfiller").body();
        assertTrue(form, form.contains("<option value=\"jobs/" + jobId + "/pit.tif\" label=\"Pitfiller, "));
        assertTrue(form.contains("<option value=\"data/a0.tif\">"));
        // the job can run again
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

    public void testDeleteJobs() throws Exception {
        Browser jane = login("jane");
        String first = jobId(runPitfiller(jane, SampleData.ELEVATION, "pit.tif"));
        String page = waitFinished(jane, first).body();
        assertTrue(page, page.contains("action=\"/jobs/" + first + "/delete\""));

        // only by the owner or an administrator
        Browser john = login("john");
        assertEquals(404, john.post("/jobs/" + first + "/delete", "csrf", Browser.csrf(john.get("/jobs"))).statusCode());
        HttpResponse<String> response = jane.post("/jobs/" + first + "/delete", "csrf", Browser.csrf(jane.get("/jobs")));
        assertEquals(302, response.statusCode());
        assertTrue(jane.get("/jobs").body().contains("was deleted."));
        assertEquals(404, jane.get("/jobs/" + first).statusCode());
        assertFalse(new File(workspace, "jobs/" + first).exists());

        // all the finished jobs, after a confirmation
        String second = jobId(runPitfiller(jane, SampleData.ELEVATION, "pit.tif"));
        waitFinished(jane, second);
        String johns = jobId(runPitfiller(john, SampleData.ELEVATION, "pit.tif"));
        waitFinished(john, johns);
        page = jane.get(JobPages.CLEANUP_PATH).body();
        assertTrue(page, page.contains("Delete 1 finished job(s) of yours"));
        assertEquals(403, jane.get(JobPages.CLEANUP_PATH + "?all=1").statusCode());
        assertEquals(403, jane.post(JobPages.CLEANUP_PATH, "csrf", Browser.csrf(jane.get("/jobs")), "all", "1").statusCode());
        assertEquals(302, jane.post(JobPages.CLEANUP_PATH, "csrf", Browser.csrf(jane.get("/jobs"))).statusCode());
        assertTrue(jane.get("/jobs").body().contains("1 finished job(s) deleted."));
        assertEquals(200, john.get("/jobs/" + johns).statusCode());

        Browser boss = login("boss");
        assertTrue(boss.get(JobPages.CLEANUP_PATH + "?all=1").body().contains("of all users"));
        assertEquals(302, boss.post(JobPages.CLEANUP_PATH, "csrf", Browser.csrf(boss.get("/jobs")), "all", "1").statusCode());
        assertEquals(404, john.get("/jobs/" + johns).statusCode());
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

    private String userId( String username ) throws Exception {
        return server.getUserManager().listUsers().stream().filter(u -> u.username.equals(username)).findFirst().get().id;
    }

    private static byte[] zip( String name, byte[] content ) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content);
            zip.closeEntry();
        }
        return out.toByteArray();
    }

    private static HttpResponse<String> upload( Browser browser, String folder, boolean unzip, Map<String, byte[]> files )
            throws Exception {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("csrf", Browser.csrf(browser.get("/uploads")));
        fields.put("folder", folder);
        if (unzip) {
            fields.put("unzip", "on");
        }
        return browser.postMultipart("/uploads", fields, UploadPages.FILES_FIELD, files);
    }

    public void testUploads() throws Exception {
        Browser jane = login("jane");
        byte[] elevation = Files.readAllBytes(new File(workspace, SampleData.ELEVATION).toPath());
        HttpResponse<String> response = upload(jane, "dtm", false, Map.of("elev.tif", elevation));
        assertEquals(response.body(), 302, response.statusCode());
        String page = jane.get("/uploads").body();
        assertTrue(page, page.contains("1 file(s) uploaded."));
        assertTrue(page.contains("href=\"/uploads/files/dtm/elev.tif\""));
        // the message is shown once
        assertFalse(jane.get("/uploads").body().contains("uploaded."));

        // zip files are unpacked
        response = upload(jane, "", true, Map.of("more.zip", zip("zipped/elev.tif", elevation)));
        assertEquals(response.body(), 302, response.statusCode());
        assertTrue(new WorkspacePaths(workspace).getUploadsFolder(userId("jane")).toPath().resolve("zipped/elev.tif").toFile().isFile());
        assertTrue(jane.get("/modules/Pitfiller").body().contains("<option value=\"uploads/zipped/elev.tif\">"));

        // an upload is an input of the modules
        String jobId = jobId(runPitfiller(jane, "uploads/dtm/elev.tif", "pit.tif"));
        assertTrue(waitFinished(jane, jobId).body().contains("state-DONE"));

        // downloads, only by the owner
        HttpResponse<byte[]> download = jane.getBytes("/uploads/files/dtm/elev.tif");
        assertEquals(200, download.statusCode());
        assertEquals(elevation.length, download.body().length);
        Browser john = login("john");
        assertEquals(404, john.get("/uploads/files/dtm/elev.tif").statusCode());
        assertEquals(404, jane.get("/uploads/files/..%2F..%2Fhm-server.sqlite").statusCode());
        assertFalse(john.get("/uploads").body().contains("elev.tif"));

        // delete
        response = jane.post("/uploads/delete", "csrf", Browser.csrf(jane.get("/uploads")), "path", "dtm");
        assertEquals(302, response.statusCode());
        assertEquals(404, jane.get("/uploads/files/dtm/elev.tif").statusCode());
        assertEquals(404, jane.post("/uploads/delete", "csrf", Browser.csrf(jane.get("/uploads")), "path", "../jobs").statusCode());
    }

    public void testUploadsRefused() throws Exception {
        Browser jane = login("jane");
        // without the token of the form
        HttpResponse<String> response = jane.postMultipart("/uploads", Map.of("folder", ""), UploadPages.FILES_FIELD,
                Map.of("a.txt", new byte[10]));
        assertEquals(403, response.statusCode());

        // larger than the quota of 2 MB, in two uploads of less than 1 MB
        assertEquals(302, upload(jane, "", false, Map.of("a.bin", new byte[1000 * 1024])).statusCode());
        assertEquals(302, upload(jane, "", false, Map.of("b.bin", new byte[1000 * 1024])).statusCode());
        response = upload(jane, "", false, Map.of("c.bin", new byte[200 * 1024]));
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("limit of 2 MB"));

        // larger than an upload can be
        response = upload(jane, "", false, Map.of("d.bin", new byte[2 * 1024 * 1024]));
        assertEquals(response.body(), 413, response.statusCode());
        assertTrue(response.body().contains("larger than 1 MB"));
        assertFalse(new File(new WorkspacePaths(workspace).getUploadsFolder(userId("jane")), "d.bin").exists());
    }

    public void testAdminUsers() throws Exception {
        Browser jane = login("jane");
        assertEquals(403, jane.get("/admin/users").statusCode());
        assertEquals(403, jane.get("/admin/status").statusCode());

        Browser boss = login("boss");
        String page = boss.get("/admin/users").body();
        assertTrue(page.contains("<td>jane"));
        String csrf = Browser.csrf(boss.get("/admin/users"));
        HttpResponse<String> response = boss.post("/admin/users", "csrf", csrf, "username", "Mary", "password", "mary-password", "role",
                "USER");
        assertEquals(response.body(), 302, response.statusCode());
        assertTrue(boss.get("/admin/users").body().contains("The user mary was created."));
        assertEquals(400, boss.post("/admin/users", "csrf", csrf, "username", "mary", "password", "mary-password", "role", "USER")
                .statusCode());
        assertEquals(400, boss.post("/admin/users", "csrf", csrf, "username", "x", "password", "short", "role", "USER").statusCode());
        login("mary");

        String janeId = userId("jane");
        String bossId = userId("boss");

        // disabled: the session ends
        assertEquals(302, boss.post("/admin/users/" + janeId, "csrf", csrf, "role", "USER", "disabled", "on").statusCode());
        assertEquals(302, jane.get("/jobs").statusCode());
        assertEquals(401, new Browser(server.getPort()).login("jane", "jane-password").statusCode());
        assertEquals(302, boss.post("/admin/users/" + janeId, "csrf", csrf, "role", "USER").statusCode());

        // a new password: the sessions end
        jane = login("jane");
        assertEquals(302, boss.post("/admin/users/" + janeId + "/password", "csrf", csrf, "password", "jane-new-password").statusCode());
        assertEquals(302, jane.get("/jobs").statusCode());
        assertEquals(302, new Browser(server.getPort()).login("jane", "jane-new-password").statusCode());

        // not the own user
        assertEquals(400, boss.post("/admin/users/" + bossId + "/delete", "csrf", csrf).statusCode());
        assertEquals(400, boss.post("/admin/users/" + bossId, "csrf", csrf, "role", "USER").statusCode());
        assertEquals(404, boss.post("/admin/users/nobody/delete", "csrf", csrf).statusCode());

        // deleted with the uploads
        File janeUploads = new WorkspacePaths(workspace).getUploadsFolder(janeId);
        assertTrue(new File(janeUploads, "x").mkdirs());
        assertEquals(302, boss.post("/admin/users/" + janeId + "/delete", "csrf", csrf).statusCode());
        assertNull(server.getUserManager().getUser(janeId));
        assertFalse(janeUploads.exists());
    }

    public void testAccount() throws Exception {
        Browser jane = login("jane");
        Browser janeElsewhere = login("jane");
        String csrf = Browser.csrf(jane.get("/account"));
        HttpResponse<String> response = jane.post("/account/password", "csrf", csrf, "current", "wrong-password", "password",
                "jane-new-password", "again", "jane-new-password");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("current password is wrong"));
        response = jane.post("/account/password", "csrf", csrf, "current", "jane-password", "password", "jane-new-password", "again",
                "other");
        assertTrue(response.body().contains("differ"));

        response = jane.post("/account/password", "csrf", csrf, "current", "jane-password", "password", "jane-new-password", "again",
                "jane-new-password");
        assertEquals(302, response.statusCode());
        // this session continues, with a new id; the other ends
        assertTrue(jane.get("/account").body().contains("The password was changed."));
        assertEquals(302, janeElsewhere.get("/account").statusCode());
        assertEquals(302, new Browser(server.getPort()).login("jane", "jane-new-password").statusCode());
    }

    public void testSettings() throws Exception {
        Browser jane = login("jane");
        assertEquals(403, jane.get("/admin/settings").statusCode());

        Browser boss = login("boss");
        String page = boss.get("/admin/settings").body();
        assertTrue(page.contains("Given on the command line as --job-ttl-days=7"));
        assertTrue(page.contains("name=\"job-ttl-days\" value=\"7\" disabled"));
        // port, workspace and database only on the command line
        assertTrue(page.contains("Only on the command line"));
        assertTrue(page.contains("value=\"SQLite in the workspace\""));
        assertFalse(page.contains("name=\"db\""));
        assertFalse(page.contains("name=\"port\""));
        String csrf = Browser.csrf(boss.get("/admin/settings"));

        // a wrong value: nothing is saved
        HttpResponse<String> response = boss.post("/admin/settings", "csrf", csrf, "slots", "3", "job-memory", "lots");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("The job memory must be"));
        assertFalse(new File(workspace, ServerConfig.SETTINGS_FILE).exists());
        assertEquals(1, server.getJobManager().getSlots());

        // the live settings apply at once
        response = boss.post("/admin/settings", "csrf", csrf, "port", "9999", "slots", "3", "job-memory", "512m", "job-ttl-days", "1",
                "max-upload-mb", "3", "user-quota-mb", "10", "db", "postgresql://localhost:5432/hm");
        assertEquals(response.body(), 302, response.statusCode());
        assertEquals(3, server.getJobManager().getSlots());
        assertEquals(3, server.getConfig().maxUploadMb);
        page = boss.get("/admin/settings").body();
        assertTrue(page, page.contains("The settings were saved and applied."));
        Map<String, String> saved = ServerConfig.readSettingsFile(workspace);
        assertEquals("3", saved.get("slots"));
        // the command line ones are not saved nor changed, port and database are not settings
        assertFalse(saved.containsKey("job-ttl-days"));
        assertEquals(7, server.getConfig().jobTtlDays);
        assertFalse(saved.containsKey("port"));
        assertEquals(0, server.getConfig().port);
        assertFalse(saved.containsKey("db"));
        assertNull(server.getConfig().db);
        assertEquals(302, upload(jane, "", false, Map.of("big.bin", new byte[2 * 1024 * 1024])).statusCode());
    }

    public void testStatus() throws Exception {
        Browser jane = login("jane");
        String jobId = jobId(runPitfiller(jane, SampleData.ELEVATION, "pit.tif"));
        Browser boss = login("boss");
        String page = boss.get("/admin/status").body();
        assertTrue(page, page.contains("SQLite in the workspace"));
        assertTrue(page.contains("1 at the same time"));
        waitFinished(jane, jobId);
        page = boss.get("/admin/status").body();
        assertTrue(page.contains("No job is queued or running."));
        assertTrue(page.contains("<span class=\"state-DONE\">done</span> 1"));
    }
}
