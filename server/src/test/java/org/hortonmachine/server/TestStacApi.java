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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.users.Role;
import org.hortonmachine.server.users.User;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

/**
 * Tests of the STAC catalog of the api, and of how it follows the files.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestStacApi extends TestCase {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long WAIT_MILLIS = TimeUnit.SECONDS.toMillis(30);

    private File workspace;
    private File data;
    private HmServer server;
    private HttpClient client;
    private String base;
    private User jane;
    private String janeToken;
    private String johnToken;
    private String adminToken;

    @Override
    protected void setUp() throws Exception {
        workspace = Files.createTempDirectory("hm-stac-api-test").toFile();
        WorkspacePaths paths = new WorkspacePaths(workspace);
        paths.createFolders();
        // there before the start: in the catalog by the first comparison
        SampleData.writeElevation(paths);
        data = paths.getDataFolder();
        ServerConfig config = new ServerConfig();
        config.port = 0;
        config.workspace = workspace;
        config.slots = 1;
        config.jobMemory = "512m";
        server = new HmServer(config);
        server.start();
        jane = server.getUserManager().createUser("jane", "jane-password".toCharArray(), Role.USER);
        User john = server.getUserManager().createUser("john", "john-password".toCharArray(), Role.USER);
        User admin = server.getUserManager().createUser("admin", "admin-password".toCharArray(), Role.ADMIN);
        janeToken = server.getUserManager().createToken(jane.id, "tests");
        johnToken = server.getUserManager().createToken(john.id, "tests");
        adminToken = server.getUserManager().createToken(admin.id, "tests");
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
        assertEquals(path + ": " + response.body(), expectedStatus, response.statusCode());
        return MAPPER.readTree(response.body());
    }

    /**
     * @return the ids of the items found.
     */
    private String ids( JsonNode itemCollection ) {
        StringBuilder ids = new StringBuilder();
        for( JsonNode feature : itemCollection.get("features") ) {
            ids.append(ids.length() > 0 ? "," : "").append(feature.get("id").asText());
        }
        return ids.toString();
    }

    /**
     * Wait until a search gives what is expected, as the watcher of the data reads the files in the background.
     */
    private JsonNode waitFor( String path, String token, Predicate<JsonNode> condition ) throws Exception {
        long end = System.currentTimeMillis() + WAIT_MILLIS;
        JsonNode json = null;
        while( System.currentTimeMillis() < end ) {
            json = get(path, token, 200);
            if (condition.test(json)) {
                return json;
            }
            Thread.sleep(200);
        }
        fail("Not as expected in time: " + path + " " + json);
        return null;
    }

    private JsonNode waitFinished( String token, String jobId ) throws Exception {
        return waitFor("/api/jobs/" + jobId, token, info -> !info.get("status").asText().equals("accepted")
                && !info.get("status").asText().equals("running"));
    }

    public void testCatalog() throws Exception {
        assertEquals(401, send("GET", "/api/stac", null, null).statusCode());
        JsonNode catalog = get("/api/stac", janeToken, 200);
        assertEquals("Catalog", catalog.get("type").asText());
        assertTrue(catalog.get("conformsTo").toString().contains("https://api.stacspec.org/v1.0.0/item-search"));
        String links = catalog.get("links").toString();
        assertTrue(links, links.contains(base + "/api/stac/collections/data"));
        assertTrue(links, links.contains(base + "/api/stac/collections/uploads"));
        assertTrue(links, links.contains("\"method\":\"POST\""));
        assertTrue(get("/api/stac/conformance", janeToken, 200).get("conformsTo").size() > 3);

        JsonNode collections = get("/api/stac/collections", janeToken, 200).get("collections");
        assertEquals("data", collections.get(0).get("id").asText());
        assertEquals("uploads", collections.get(1).get("id").asText());
        assertEquals(404, send("GET", "/api/stac/collections/nothing", janeToken, null).statusCode());
        // a module without jobs is an empty collection
        assertEquals("jobs-Pitfiller", get("/api/stac/collections/jobs-Pitfiller", janeToken, 200).get("id").asText());

        // the api landing page leads here
        assertTrue(get("/api", null, 200).get("links").toString().contains(base + "/api/stac"));
    }

    public void testDataItems() throws Exception {
        JsonNode items = waitFor("/api/stac/collections/data/items", janeToken, json -> json.get("features").size() == 1);
        JsonNode item = items.get("features").get(0);
        assertEquals("elevation.tif", item.get("id").asText());
        assertEquals("data/elevation.tif", item.at("/properties/hm:path").asText());
        assertEquals("EPSG:32632", item.at("/assets/data/proj:code").asText());
        assertTrue(item.at("/assets/data/raster:bands/0/statistics").has("mean"));
        String href = item.at("/assets/data/href").asText();
        assertTrue(href, href.startsWith(base + "/api/data/files/elevation.tif?expires="));
        assertTrue(item.get("links").toString().contains(base + "/api/stac/collections/data/items/elevation.tif"));

        // the asset opens without token, in parts
        HttpResponse<byte[]> download = client.send(HttpRequest.newBuilder(URI.create(href)).header("Range", "bytes=0-9").build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(206, download.statusCode());
        assertEquals(10, download.body().length);
        assertEquals(401, send("GET", "/api/data/files/elevation.tif", null, null).statusCode());
        assertEquals(404, send("GET", "/api/data/files/../hm-server.sqlite", janeToken, null).statusCode());

        // one item asked alone
        assertEquals("elevation.tif", get("/api/stac/collections/data/items/elevation.tif", johnToken, 200).get("id").asText());

        // search: the elevation is near 4.5E 0N
        assertEquals("elevation.tif", ids(get("/api/stac/search?bbox=4,-1,5,1", janeToken, 200)));
        assertEquals("", ids(get("/api/stac/search?bbox=10,45,11,46", janeToken, 200)));
        assertEquals("", ids(get("/api/stac/search?datetime=2100-01-01T00:00:00Z/..", janeToken, 200)));
        assertEquals("elevation.tif", ids(get("/api/stac/search?datetime=../2100-01-01T00:00:00Z&collections=data", janeToken, 200)));
        assertEquals("", ids(get("/api/stac/search?collections=uploads", janeToken, 200)));
        assertEquals(400, send("GET", "/api/stac/search?bbox=1,2,3", janeToken, null).statusCode());
        assertEquals(400, send("GET", "/api/stac/search?datetime=yesterday", janeToken, null).statusCode());
        assertEquals("elevation.tif", ids(MAPPER.readTree(send("POST", "/api/stac/search", janeToken,
                "{\"bbox\": [4, -1, 5, 1], \"collections\": [\"data\"]}").body())));
    }

    public void testDataFollowsTheFiles() throws Exception {
        waitFor("/api/stac/collections/data/items", janeToken, json -> json.get("features").size() == 1);
        File elevation = new File(data, "elevation.tif");

        // a new folder with a copy
        File folder = new File(data, "dem");
        assertTrue(folder.mkdirs());
        File copy = new File(folder, "copy.tif");
        Files.copy(elevation.toPath(), copy.toPath());
        String itemPath = "/api/stac/collections/data/items/dem~copy.tif";
        JsonNode items = waitFor("/api/stac/collections/data/items", janeToken, json -> json.get("features").size() == 2);
        assertTrue(ids(items), ids(items).contains("dem~copy.tif"));
        JsonNode item = get(itemPath, janeToken, 200);
        assertEquals("data/dem/copy.tif", item.at("/properties/hm:path").asText());
        assertTrue(item.at("/assets/data/href").asText().startsWith(base + "/api/data/files/dem/copy.tif?"));

        // a shapefile is one item, with its parts as assets
        for( String extension : new String[]{"dbf", "shx", "prj"} ) {
            FileUtilities.writeFile("part", new File(folder, "roads." + extension));
        }
        FileUtilities.writeFile("not really a shapefile", new File(folder, "roads.shp"));
        items = waitFor("/api/stac/collections/data/items", janeToken, json -> ids(json).contains("dem~roads.shp")
                && !ids(json).contains("roads.dbf"));
        JsonNode roads = get("/api/stac/collections/data/items/dem~roads.shp", janeToken, 200);
        assertTrue(roads.at("/assets/dbf/href").asText().startsWith(base + "/api/data/files/dem/roads.dbf?"));
        assertEquals("metadata", roads.at("/assets/prj/roles/0").asText());

        // changed: read again
        long size = item.at("/assets/data/file:size").asLong();
        FileUtilities.writeFile("not a tiff anymore", copy);
        waitFor(itemPath, janeToken, json -> json.at("/assets/data/file:size").asLong() != size);

        // gone: asked alone it goes at once, before the watcher
        Files.delete(copy.toPath());
        assertEquals(404, send("GET", itemPath, janeToken, null).statusCode());
        FileUtilities.deleteFileOrDir(folder);
        waitFor("/api/stac/collections/data/items", janeToken, json -> ids(json).equals("elevation.tif"));

        // a file moved in, as at the end of a copy
        File tmp = new File(workspace, "tmp/moved.tif");
        Files.copy(elevation.toPath(), tmp.toPath());
        Files.move(tmp.toPath(), new File(data, "moved.tif").toPath(), StandardCopyOption.ATOMIC_MOVE);
        waitFor("/api/stac/collections/data/items", janeToken, json -> ids(json).contains("moved.tif"));
    }

    public void testUploads() throws Exception {
        byte[] elevation = Files.readAllBytes(new File(data, "elevation.tif").toPath());
        server.getUploadManager().save(jane.id, "dem", "mine.tif", elevation.length, new ByteArrayInputStream(elevation));
        server.getUploadManager().save(jane.id, "", "notes.txt", 5, new ByteArrayInputStream("notes".getBytes(StandardCharsets.UTF_8)));

        // at once, only for jane and the administrators
        String itemId = jane.id + "~dem~mine.tif";
        JsonNode items = get("/api/stac/collections/uploads/items", janeToken, 200);
        assertEquals(2, items.get("features").size());
        JsonNode item = get("/api/stac/collections/uploads/items/" + itemId, janeToken, 200);
        assertEquals("uploads/dem/mine.tif", item.at("/properties/hm:path").asText());
        String href = item.at("/assets/data/href").asText();
        assertTrue(href, href.startsWith(base + "/api/uploads/" + jane.id + "/files/dem/mine.tif?expires="));
        assertEquals(200, send("GET", href, null, null).statusCode());
        JsonNode notes = get("/api/stac/collections/uploads/items/" + jane.id + "~notes.txt", janeToken, 200);
        assertTrue(notes.get("geometry").isNull());
        assertFalse(notes.has("bbox"));

        assertEquals(0, get("/api/stac/collections/uploads/items", johnToken, 200).get("features").size());
        assertEquals(404, send("GET", "/api/stac/collections/uploads/items/" + itemId, johnToken, null).statusCode());
        assertEquals(404, send("GET", "/api/uploads/" + jane.id + "/files/dem/mine.tif", johnToken, null).statusCode());
        assertEquals("", ids(get("/api/stac/search?ids=" + itemId, johnToken, 200)));
        assertEquals(itemId, ids(get("/api/stac/search?ids=" + itemId, adminToken, 200)));
        assertEquals(200, send("GET", "/api/uploads/" + jane.id + "/files/dem/mine.tif", adminToken, null).statusCode());

        // a uploaded file used as input gives an item of the job
        HttpResponse<String> response = send("POST", "/api/processes/Pitfiller/execution", janeToken,
                "{\"inputs\": {\"inElev\": \"" + item.at("/properties/hm:path").asText() + "\"}}");
        assertEquals(response.body(), 201, response.statusCode());
        String jobId = MAPPER.readTree(response.body()).get("jobID").asText();
        assertEquals("successful", waitFinished(janeToken, jobId).get("status").asText());
        JsonNode collections = get("/api/stac/collections", janeToken, 200);
        assertTrue(collections.toString().contains("\"id\":\"jobs-Pitfiller\""));
        assertFalse(get("/api/stac/collections", johnToken, 200).toString().contains("jobs-Pitfiller"));
        JsonNode jobItem = get("/api/stac/collections/jobs-Pitfiller/items/" + jobId, janeToken, 200);
        assertTrue(jobItem.at("/assets/outPit/href").asText()
                .startsWith(base + "/api/jobs/" + jobId + "/files/pit.tif?expires="));
        assertTrue(jobItem.at("/properties").has("expires"));
        assertTrue(jobItem.get("links").toString().contains(base + "/jobs/" + jobId));
        assertEquals(404, send("GET", "/api/stac/collections/jobs-Pitfiller/items/" + jobId, johnToken, null).statusCode());

        // deleted: at once
        assertTrue(server.getUploadManager().delete(jane.id, "dem"));
        assertEquals(jane.id + "~notes.txt", ids(get("/api/stac/collections/uploads/items", janeToken, 200)));
        server.getUploadManager().deleteAll(jane.id);
        assertEquals(0, get("/api/stac/collections/uploads/items", janeToken, 200).get("features").size());
    }

    public void testPaging() throws Exception {
        waitFor("/api/stac/collections/data/items", janeToken, json -> json.get("features").size() == 1);
        for( String name : new String[]{"a.txt", "b.txt"} ) {
            FileUtilities.writeFile(name, new File(data, name));
        }
        waitFor("/api/stac/collections/data/items", janeToken, json -> json.get("features").size() == 3);

        JsonNode page = get("/api/stac/search?collections=data&limit=2", janeToken, 200);
        assertEquals("a.txt,b.txt", ids(page));
        assertEquals(3, page.get("numberMatched").asInt());
        String next = null;
        for( JsonNode link : page.get("links") ) {
            if (link.get("rel").asText().equals("next")) {
                next = link.get("href").asText();
            }
        }
        assertNotNull(page.toString(), next);
        page = get(next, janeToken, 200);
        assertEquals("elevation.tif", ids(page));
        assertFalse(page.get("links").toString().contains("\"next\""));

        // POST: the next page is the same body with the token
        page = MAPPER.readTree(send("POST", "/api/stac/search", janeToken, "{\"collections\": [\"data\"], \"limit\": 2}").body());
        JsonNode nextLink = null;
        for( JsonNode link : page.get("links") ) {
            if (link.get("rel").asText().equals("next")) {
                nextLink = link;
            }
        }
        assertEquals("POST", nextLink.get("method").asText());
        page = MAPPER.readTree(send("POST", nextLink.get("href").asText(), janeToken, nextLink.get("body").toString()).body());
        assertEquals("elevation.tif", ids(page));
    }
}
