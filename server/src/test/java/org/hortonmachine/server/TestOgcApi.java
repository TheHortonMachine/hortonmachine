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
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.stac.StacFiles;
import org.hortonmachine.server.stac.StacJobs;
import org.hortonmachine.server.stac.StacRecord;
import org.hortonmachine.server.users.Role;
import org.hortonmachine.server.web.MediaTypes;
import org.hortonmachine.server.users.User;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

/**
 * Tests of the OGC API - Processes and of its tokens.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestOgcApi extends TestCase {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String RUN_PIT = "{\"inputs\": {\"inElev\": \"" + SampleData.ELEVATION + "\"}}";

    private File workspace;
    private HmServer server;
    private HttpClient client;
    private String base;
    private String janeToken;
    private String johnToken;

    @Override
    protected void setUp() throws Exception {
        workspace = Files.createTempDirectory("hm-api-test").toFile();
        ServerConfig config = new ServerConfig();
        config.port = 0;
        config.workspace = workspace;
        config.slots = 1;
        config.jobMemory = "512m";
        server = new HmServer(config);
        server.start();
        User jane = server.getUserManager().createUser("jane", "jane-password".toCharArray(), Role.USER);
        User john = server.getUserManager().createUser("john", "john-password".toCharArray(), Role.USER);
        janeToken = server.getUserManager().createToken(jane.id, "tests");
        johnToken = server.getUserManager().createToken(john.id, "tests");
        SampleData.writeElevation(new WorkspacePaths(workspace));
        client = HttpClient.newHttpClient();
        base = "http://localhost:" + server.getPort();
    }

    @Override
    protected void tearDown() throws Exception {
        server.stop();
        FileUtilities.deleteFileOrDir(workspace);
    }

    private HttpResponse<String> send( String method, String path, String token, String body ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(path.startsWith("http") ? path : base + path));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        if (body != null) {
            builder.header("Content-Type", "application/json").method(method, HttpRequest.BodyPublishers.ofString(body));
        } else {
            builder.method(method, HttpRequest.BodyPublishers.noBody());
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode get( String path, String token, int expectedStatus ) throws Exception {
        HttpResponse<String> response = send("GET", path, token, null);
        assertEquals(response.body(), expectedStatus, response.statusCode());
        return MAPPER.readTree(response.body());
    }

    /**
     * @return the id of the job created.
     */
    private String execute( String token, String body ) throws Exception {
        return execute(token, body, "Pitfiller");
    }

    private String execute( String token, String body, String process ) throws Exception {
        HttpResponse<String> response = send("POST", "/api/processes/" + process + "/execution", token, body);
        assertEquals(response.body(), 201, response.statusCode());
        JsonNode info = MAPPER.readTree(response.body());
        String jobId = info.get("jobID").asText();
        assertEquals(base + "/api/jobs/" + jobId, response.headers().firstValue("Location").orElse(""));
        assertEquals(process, info.get("processID").asText());
        return jobId;
    }

    private JsonNode waitFinished( String token, String jobId ) throws Exception {
        long end = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(120);
        while( System.currentTimeMillis() < end ) {
            JsonNode info = get("/api/jobs/" + jobId, token, 200);
            String status = info.get("status").asText();
            if (!status.equals("accepted") && !status.equals("running")) {
                return info;
            }
            Thread.sleep(100);
        }
        fail("The job didn't finish: " + jobId);
        return null;
    }

    public void testAccess() throws Exception {
        // open without token
        JsonNode landing = get("/api", null, 200);
        assertTrue(landing.toString().contains("\"rel\":\"service-doc\""));
        assertTrue(landing.toString().contains("http://www.opengis.net/def/rel/ogc/1.0/processes"));
        assertTrue(get("/api/conformance", null, 200).toString()
                .contains("http://www.opengis.net/spec/ogcapi-processes-1/1.0/conf/core"));
        assertEquals(200, send("GET", "/api/docs", null, null).statusCode());

        // else only with a valid token
        HttpResponse<String> response = send("GET", "/api/processes", null, null);
        assertEquals(401, response.statusCode());
        assertEquals("Bearer", response.headers().firstValue("WWW-Authenticate").orElse(""));
        assertTrue(response.headers().firstValue("Content-Type").orElse("").startsWith("application/problem+json"));
        assertEquals(401, send("GET", "/api/processes", "hm_wrong", null).statusCode());
        assertEquals(401, send("POST", "/api/processes/Pitfiller/execution", "hm_wrong", RUN_PIT).statusCode());

        // the session of the pages is not used by the api
        Browser browser = new Browser(server.getPort());
        assertFalse(browser.get("/api/docs").body().contains("Log out"));
        assertEquals(302, browser.login("jane", "jane-password").statusCode());
        assertEquals(401, browser.get("/api/processes").statusCode());
        // but the documentation has the menu, which leads to it
        String docs = browser.get("/api/docs").body();
        assertTrue(docs.contains("Log out"));
        assertTrue(docs.contains("<a href=\"/api/docs\">API</a>"));
        assertTrue(docs.contains("/api/stac"));
        assertTrue(browser.get("/modules").body().contains("<a href=\"/api/docs\">API</a>"));

        // revoked and disabled
        User jane = server.getUserManager().listUsers().stream().filter(u -> u.username.equals("jane")).findFirst().get();
        String other = server.getUserManager().createToken(jane.id, "other");
        assertEquals(200, send("GET", "/api/processes", other, null).statusCode());
        String tokenId = server.getUserManager().listTokens(jane.id).get(0).id;
        assertTrue(server.getUserManager().revokeToken(jane.id, tokenId));
        assertEquals(401, send("GET", "/api/processes", other, null).statusCode());
        assertEquals(200, send("GET", "/api/processes", janeToken, null).statusCode());
        assertNotNull(server.getUserManager().listTokens(jane.id).get(0).lastUsed);
        server.getUserManager().updateUser(jane.id, Role.USER, true);
        assertEquals(401, send("GET", "/api/processes", janeToken, null).statusCode());
    }

    public void testTokensPage() throws Exception {
        Browser browser = new Browser(server.getPort());
        assertEquals(302, browser.login("jane", "jane-password").statusCode());
        String csrf = Browser.csrf(browser.get("/account"));
        assertEquals(302, browser.post("/account/tokens", "csrf", csrf, "name", "qgis").statusCode());
        String page = browser.get("/account").body();
        Matcher matcher = Pattern.compile("<pre>(hm_[A-Za-z0-9_-]+)</pre>").matcher(page);
        assertTrue(page, matcher.find());
        String secret = matcher.group(1);
        assertTrue(page.contains("<td>qgis</td>"));
        // shown once
        assertFalse(browser.get("/account").body().contains(secret));
        assertEquals(200, send("GET", "/api/processes", secret, null).statusCode());
        assertEquals(400, browser.post("/account/tokens", "csrf", csrf, "name", " ").statusCode());

        // a user can't revoke the tokens of others
        User john = server.getUserManager().listUsers().stream().filter(u -> u.username.equals("john")).findFirst().get();
        String johnTokenId = server.getUserManager().listTokens(john.id).get(0).id;
        assertEquals(404, browser.post("/account/tokens/" + johnTokenId + "/delete", "csrf", csrf).statusCode());
        assertEquals(200, send("GET", "/api/processes", johnToken, null).statusCode());
    }

    public void testProcesses() throws Exception {
        JsonNode list = get("/api/processes", janeToken, 200);
        assertTrue(list.get("processes").toString().contains("\"id\":\"Pitfiller\""));
        JsonNode pitfiller = get("/api/processes/Pitfiller", janeToken, 200);
        assertEquals("string", pitfiller.at("/inputs/inElev/schema/type").asText());
        assertEquals(1, pitfiller.at("/inputs/inElev/minOccurs").asInt());
        assertEquals("image/tiff; application=geotiff", pitfiller.at("/outputs/outPit/schema/contentMediaType").asText());
        assertTrue(pitfiller.get("jobControlOptions").toString().contains("async-execute"));
        JsonNode gradient = get("/api/processes/Gradient", janeToken, 200);
        assertEquals("boolean", gradient.at("/inputs/doDegrees/schema/type").asText());
        assertTrue(gradient.at("/inputs/pMode/schema/enum").toString().contains("Finite Differences"));

        JsonNode problem = get("/api/processes/NoSuchModule", janeToken, 404);
        assertEquals("http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-process", problem.get("type").asText());
    }

    public void testExecuteAndResults() throws Exception {
        String jobId = execute(janeToken, RUN_PIT);
        JsonNode info = waitFinished(janeToken, jobId);
        assertEquals("successful", info.get("status").asText());
        assertEquals(100, info.get("progress").asInt());
        assertTrue(info.get("links").toString().contains("/api/jobs/" + jobId + "/results"));

        JsonNode results = get("/api/jobs/" + jobId + "/results", janeToken, 200);
        String href = results.at("/outPit/href").asText();
        assertTrue(href, href.startsWith(base + "/api/jobs/" + jobId + "/files/pit.tif?expires="));
        assertEquals("image/tiff; application=geotiff", results.at("/outPit/type").asText());
        HttpResponse<byte[]> download = client.send(
                HttpRequest.newBuilder(URI.create(href)).header("Authorization", "Bearer " + janeToken).build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(200, download.statusCode());
        assertEquals(new File(workspace, "jobs/" + jobId + "/pit.tif").length(), download.body().length);

        // a result is the input of the next job
        String next = execute(janeToken, "{\"inputs\": {\"inElev\": {\"href\": \"" + href + "\"}}}");
        assertEquals("successful", waitFinished(janeToken, next).get("status").asText());
        assertEquals(2, get("/api/jobs?processID=Pitfiller&status=successful", janeToken, 200).get("jobs").size());
        assertEquals(1, get("/api/jobs?limit=1", janeToken, 200).get("jobs").size());

        // the jobs of others don't exist
        for( String path : new String[]{"/api/jobs/" + jobId, "/api/jobs/" + jobId + "/results",
                "/api/jobs/" + jobId + "/files/pit.tif"} ) {
            assertEquals(path, "http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/no-such-job",
                    get(path, johnToken, 404).get("type").asText());
        }
        assertEquals(404, send("DELETE", "/api/jobs/" + jobId, johnToken, null).statusCode());
        assertEquals(0, get("/api/jobs", johnToken, 200).get("jobs").size());
        HttpResponse<String> response = send("POST", "/api/processes/Pitfiller/execution", johnToken,
                "{\"inputs\": {\"inElev\": {\"href\": \"" + href + "\"}}}");
        assertEquals(400, response.statusCode());

        // dismissed: deleted with the files
        response = send("DELETE", "/api/jobs/" + jobId, janeToken, null);
        assertEquals(200, response.statusCode());
        assertEquals("dismissed", MAPPER.readTree(response.body()).get("status").asText());
        get("/api/jobs/" + jobId, janeToken, 404);
        assertFalse(new File(workspace, "jobs/" + jobId).exists());
    }

    private HttpResponse<byte[]> getFile( String url, String token, String... headers ) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url.startsWith("http") ? url : base + url));
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        for( int i = 0; i < headers.length; i += 2 ) {
            builder.header(headers[i], headers[i + 1]);
        }
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofByteArray());
    }

    public void testRangesAndSignedLinks() throws Exception {
        String jobId = execute(janeToken, RUN_PIT);
        assertEquals("successful", waitFinished(janeToken, jobId).get("status").asText());
        String path = "/api/jobs/" + jobId + "/files/pit.tif";
        byte[] content = Files.readAllBytes(new File(workspace, "jobs/" + jobId + "/pit.tif").toPath());
        int size = content.length;

        // ranges
        HttpResponse<byte[]> response = getFile(path, janeToken, "Range", "bytes=10-109");
        assertEquals(206, response.statusCode());
        assertEquals("bytes 10-109/" + size, response.headers().firstValue("Content-Range").orElse(""));
        assertTrue(Arrays.equals(Arrays.copyOfRange(content, 10, 110), response.body()));
        response = getFile(path, janeToken, "Range", "bytes=-16");
        assertEquals(206, response.statusCode());
        assertTrue(Arrays.equals(Arrays.copyOfRange(content, size - 16, size), response.body()));
        response = getFile(path, janeToken, "Range", "bytes=" + size + "-");
        assertEquals(416, response.statusCode());
        assertEquals("bytes */" + size, response.headers().firstValue("Content-Range").orElse(""));
        response = getFile(path, janeToken, "Range", "bytes=0-9,20-29");
        assertEquals(200, response.statusCode());
        assertEquals(size, response.body().length);
        // even if the client accepts compression, the file is sent as it is
        response = getFile(path, janeToken, "Accept-Encoding", "gzip");
        assertEquals(200, response.statusCode());
        assertTrue(Arrays.equals(content, response.body()));
        assertEquals("bytes", response.headers().firstValue("Accept-Ranges").orElse(""));
        String etag = response.headers().firstValue("ETag").orElse("");
        assertEquals(304, getFile(path, janeToken, "If-None-Match", etag).statusCode());
        assertEquals(206, getFile(path, janeToken, "Range", "bytes=0-9", "If-Range", etag).statusCode());
        assertEquals(200, getFile(path, janeToken, "Range", "bytes=0-9", "If-Range", "\"changed\"").statusCode());

        // head: the size without the content
        HttpResponse<String> head = send("HEAD", path, janeToken, null);
        assertEquals(200, head.statusCode());
        assertEquals(String.valueOf(size), head.headers().firstValue("Content-Length").orElse(""));
        assertEquals("", head.body());
        assertEquals(401, send("HEAD", path, null, null).statusCode());

        // signed links work without the token, for the file only
        String href = get("/api/jobs/" + jobId + "/results", janeToken, 200).at("/outPit/href").asText();
        String query = href.substring(href.indexOf('?'));
        response = getFile(href, null, "Range", "bytes=0-99");
        assertEquals(206, response.statusCode());
        assertTrue(Arrays.equals(Arrays.copyOf(content, 100), response.body()));
        assertEquals(200, send("HEAD", href, null, null).statusCode());
        assertEquals(401, getFile(href.replace("signature=", "signature=x"), null).statusCode());
        assertEquals(401, getFile(base + "/api/jobs/" + jobId + query, null).statusCode());
        assertEquals(401, getFile(base + "/api/jobs/" + jobId + "/files/job.log" + query, null).statusCode());
        // a wrong token in the header is not replaced by the signature
        assertEquals(401, getFile(href, "hm_wrong").statusCode());
        // the user must still be allowed to read the file
        assertEquals(404, getFile(href, johnToken).statusCode());

        // the administrators can end all the links
        server.getUserManager().createUser("admin", "admin-password".toCharArray(), Role.ADMIN);
        Browser browser = new Browser(server.getPort());
        assertEquals(302, browser.login("admin", "admin-password").statusCode());
        String csrf = Browser.csrf(browser.get("/admin/settings"));
        assertEquals(302, browser.post("/admin/settings/signing-key", "csrf", csrf).statusCode());
        assertEquals(401, getFile(href, null).statusCode());

        // revoking the token ends its links
        href = get("/api/jobs/" + jobId + "/results", janeToken, 200).at("/outPit/href").asText();
        assertEquals(200, getFile(href, null).statusCode());
        User jane = server.getUserManager().listUsers().stream().filter(u -> u.username.equals("jane")).findFirst().get();
        assertTrue(server.getUserManager().revokeToken(jane.id, server.getUserManager().listTokens(jane.id).get(0).id));
        assertEquals(401, getFile(href, null).statusCode());
    }

    public void testCatalogItemOfJob() throws Exception {
        String jobId = execute(janeToken, RUN_PIT);
        assertEquals("successful", waitFinished(janeToken, jobId).get("status").asText());
        File jobFolder = new File(workspace, "jobs/" + jobId);
        File pit = new File(jobFolder, "pit.tif");
        assertTrue(StacFiles.isCog(pit));

        JsonNode item = MAPPER.readTree(FileUtilities.readFile(new File(jobFolder, StacJobs.ITEM_FILE)));
        assertEquals(jobId, item.get("id").asText());
        assertEquals("jobs-Pitfiller", item.get("collection").asText());
        assertEquals("Polygon", item.at("/geometry/type").asText());
        assertEquals(4, item.get("bbox").size());
        assertEquals("./pit.tif", item.at("/assets/outPit/href").asText());
        assertEquals(StacFiles.COG, item.at("/assets/outPit/type").asText());
        assertEquals("EPSG:32632", item.at("/assets/outPit/proj:code").asText());
        assertEquals(pit.length(), item.at("/assets/outPit/file:size").asLong());
        assertTrue(item.at("/assets/outPit/raster:bands/0/statistics").has("mean"));
        assertEquals("Pitfiller", item.at("/properties/hm:module").asText());
        assertEquals(SampleData.ELEVATION, item.at("/properties/hm:parameters/inElev").asText());
        assertTrue(item.at("/properties").has("start_datetime"));
        assertTrue(item.at("/properties").has("expires"));

        StacRecord record = server.getDb().getStacItem(jobId);
        assertEquals("jobs-Pitfiller", record.collection);
        assertEquals(server.getUserManager().listTokens(record.ownerId).size(), 1);
        assertEquals(item.get("bbox").get(0).asDouble(), record.minLon, 1e-12);
        assertNotNull(record.startTime);

        // the item is a file of the job, not the parameters
        assertEquals(200, getFile("/api/jobs/" + jobId + "/files/" + StacJobs.ITEM_FILE, janeToken).statusCode());

        // gone with the job
        assertEquals(200, send("DELETE", "/api/jobs/" + jobId, janeToken, null).statusCode());
        assertNull(server.getDb().getStacItem(jobId));

        // the outputs as the modules write them
        server.getConfig().cogOutputs = false;
        jobId = execute(janeToken, RUN_PIT);
        assertEquals("successful", waitFinished(janeToken, jobId).get("status").asText());
        assertFalse(StacFiles.isCog(new File(workspace, "jobs/" + jobId + "/pit.tif")));
        assertEquals(MediaTypes.GEOTIFF, MAPPER.readTree(server.getDb().getStacItem(jobId).json).at("/assets/outPit/type").asText());
    }

    public void testVectorOutput() throws Exception {
        String jobId = execute(janeToken, "{\"inputs\": {\"inRaster\": \"" + SampleData.ELEVATION + "\"}}", "Vectorizer");
        JsonNode info = waitFinished(janeToken, jobId);
        assertEquals(info.toString(), "successful", info.get("status").asText());
        assertTrue(new File(workspace, "jobs/" + jobId + "/vector.gpkg").isFile());

        // the file as href, the table as fragment
        String href = get("/api/jobs/" + jobId + "/results", janeToken, 200).at("/outVector/href").asText();
        assertTrue(href, href.startsWith(base + "/api/jobs/" + jobId + "/files/vector.gpkg?expires="));
        assertTrue(href, href.endsWith("#vector"));
        assertEquals(200, getFile(href, null).statusCode());

        JsonNode asset = MAPPER.readTree(server.getDb().getStacItem(jobId).json).at("/assets/outVector");
        assertEquals("./vector.gpkg", asset.get("href").asText());
        assertEquals("vector", asset.at("/hm:layers/0/name").asText());
        assertEquals("EPSG:32632", asset.get("proj:code").asText());
    }

    public void testWrongInputs() throws Exception {
        HttpResponse<String> response = send("POST", "/api/processes/Pitfiller/execution", janeToken,
                "{\"inputs\": {\"inElev\": \"/etc/passwd\", \"noSuchInput\": 1}, \"outputs\": {\"noSuchOutput\": {}}}");
        assertEquals(400, response.statusCode());
        JsonNode problem = MAPPER.readTree(response.body());
        assertTrue(problem.at("/errors/noSuchInput").asText().contains("Not an input"));
        assertTrue(problem.at("/errors/noSuchOutput").asText().contains("Not an output"));
        response = send("POST", "/api/processes/Pitfiller/execution", janeToken, RUN_PIT.replace(SampleData.ELEVATION, "/etc/passwd"));
        assertEquals(400, response.statusCode());
        assertTrue(MAPPER.readTree(response.body()).at("/errors/inElev").asText().contains("relative to the workspace"));
        assertEquals(400, send("POST", "/api/processes/Pitfiller/execution", janeToken, "{not json").statusCode());
        assertEquals(400, send("POST", "/api/processes/Pitfiller/execution", janeToken,
                "{\"inputs\": {\"inElev\": {\"href\": \"https://example.org/dem.tif\"}}}").statusCode());
        assertTrue(server.getJobManager().listJobs(null, 10).isEmpty());
    }

    public void testNotReadyAndKill() throws Exception {
        // with one slot the second job waits
        String first = execute(janeToken, RUN_PIT);
        String second = execute(janeToken, RUN_PIT);
        assertEquals("http://www.opengis.net/def/exceptions/ogcapi-processes-1/1.0/result-not-ready",
                get("/api/jobs/" + second + "/results", janeToken, 404).get("type").asText());
        HttpResponse<String> response = send("DELETE", "/api/jobs/" + second, janeToken, null);
        assertEquals(200, response.statusCode());
        assertEquals("dismissed", waitFinished(janeToken, second).get("status").asText());
        assertEquals(410, send("GET", "/api/jobs/" + second + "/results", janeToken, null).statusCode());
        waitFinished(janeToken, first);
    }
}
