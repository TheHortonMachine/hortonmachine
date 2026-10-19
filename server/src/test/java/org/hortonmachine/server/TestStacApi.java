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
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.geotools.api.geometry.Position;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.Position2D;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.HMStacCollection;
import org.hortonmachine.gears.io.stac.HMStacItem;
import org.hortonmachine.gears.io.stac.HMStacManager;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;
import org.hortonmachine.gears.io.stac.auth.HMHttpAuthentication;
import org.hortonmachine.gears.io.stac.auth.HMStacAccess;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.server.stac.StacCollectionInfo;
import org.hortonmachine.server.users.Role;
import org.hortonmachine.server.users.User;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import junit.framework.TestCase;

/**
 * Tests of the STAC catalog of the api, and of how it follows the files.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class TestStacApi extends TestCase {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final long WAIT_MILLIS = TimeUnit.SECONDS.toMillis(30);

    /** The collection of the data of the tests, a folder of the shared area. */
    private static final String COLLECTION = SampleData.COLLECTION;
    private static final String ELEVATION_ID = "shared~elevation.tif";

    private File workspace;
    /** The shared area. */
    private File data;
    /** The folder of the collection in the shared area. */
    private File collectionFolder;
    private HmServer server;
    private HttpClient client;
    private String base;
    private User jane;
    private User john;
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
        data = paths.getAreaFolder(WorkspacePaths.SHARED);
        // in a folder: an item of its collection; directly in the area: not in the catalog
        collectionFolder = new File(data, COLLECTION);
        Files.copy(new File(collectionFolder, "elevation.tif").toPath(), new File(data, "elevation.tif").toPath());
        ServerConfig config = new ServerConfig();
        config.port = 0;
        config.workspace = workspace;
        config.slots = 1;
        config.jobMemory = "512m";
        server = new HmServer(config);
        server.start();
        jane = server.getUserManager().createUser("jane", "jane-password".toCharArray(), Role.USER);
        john = server.getUserManager().createUser("john", "john-password".toCharArray(), Role.USER);
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

    /**
     * @return the ids of the collections of a json with collections.
     */
    private static String collectionIds( JsonNode collections ) {
        StringBuilder ids = new StringBuilder();
        for( JsonNode collection : collections.get("collections") ) {
            ids.append(ids.length() > 0 ? "," : "").append(collection.get("id").asText());
        }
        return ids.toString();
    }

    private String itemsOf( String collection ) {
        return "/api/stac/collections/" + collection + "/items";
    }

    public void testCatalog() throws Exception {
        // an empty folder is a collection too, the uploads of another user are not seen
        assertTrue(new File(data, "empty").mkdirs());
        server.getUploadManager().save(john.username, john.id, "johns", "notes.txt", 5,
                new ByteArrayInputStream("notes".getBytes(StandardCharsets.UTF_8)));

        assertEquals(401, send("GET", "/api/stac", null, null).statusCode());
        JsonNode catalog = get("/api/stac", janeToken, 200);
        assertEquals("Catalog", catalog.get("type").asText());
        assertTrue(catalog.get("conformsTo").toString().contains("https://api.stacspec.org/v1.0.0/item-search"));
        String links = catalog.get("links").toString();
        assertTrue(links, links.contains(base + "/api/stac/collections/" + COLLECTION));
        assertTrue(links, links.contains(base + "/api/stac/collections/empty"));
        assertFalse(links, links.contains("johns"));
        assertTrue(links, links.contains("\"method\":\"POST\""));
        assertTrue(get("/api/stac/conformance", janeToken, 200).get("conformsTo").size() > 3);

        assertEquals("empty," + COLLECTION, collectionIds(get("/api/stac/collections", janeToken, 200)));
        assertEquals("empty," + COLLECTION + ",johns", collectionIds(get("/api/stac/collections", johnToken, 200)));
        assertEquals("empty," + COLLECTION + ",johns", collectionIds(get("/api/stac/collections", adminToken, 200)));
        assertEquals(404, send("GET", "/api/stac/collections/nothing", janeToken, null).statusCode());
        assertEquals(404, send("GET", "/api/stac/collections/johns", janeToken, null).statusCode());
        assertEquals(0, get(itemsOf("empty"), janeToken, 200).get("features").size());
        // no collections of the jobs anymore
        assertEquals(404, send("GET", "/api/stac/collections/jobs-Pitfiller", janeToken, null).statusCode());

        // described by the folder name, unless the administrators gave metadata
        JsonNode collection = get("/api/stac/collections/" + COLLECTION, janeToken, 200);
        assertEquals(COLLECTION, collection.get("title").asText());
        assertEquals("other", collection.get("license").asText());
        assertFalse(collection.has("keywords"));
        server.getDb().putStacCollectionInfo(new StacCollectionInfo(COLLECTION, "Flanginec valley", "The elevation of the valley.",
                List.of("dem", "alps"), "CC-BY-4.0"));
        collection = get("/api/stac/collections/" + COLLECTION, janeToken, 200);
        assertEquals("Flanginec valley", collection.get("title").asText());
        assertEquals("The elevation of the valley.", collection.get("description").asText());
        assertEquals("CC-BY-4.0", collection.get("license").asText());
        assertEquals("[\"dem\",\"alps\"]", collection.get("keywords").toString());
        assertTrue(get("/api/stac", janeToken, 200).get("links").toString().contains("Flanginec valley"));

        // the api landing page leads here
        assertTrue(get("/api", null, 200).get("links").toString().contains(base + "/api/stac"));
    }

    public void testDataItems() throws Exception {
        JsonNode items = waitFor(itemsOf(COLLECTION), janeToken, json -> json.get("features").size() == 1);
        JsonNode item = items.get("features").get(0);
        assertEquals(ELEVATION_ID, item.get("id").asText());
        assertEquals(COLLECTION, item.get("collection").asText());
        assertEquals(SampleData.ELEVATION, item.at("/properties/hm:path").asText());
        assertEquals("shared", item.at("/properties/hm:area").asText());
        assertEquals("external", item.at("/properties/hm:origin").asText());
        assertFalse(item.get("properties").has("hm:owner"));
        assertEquals("EPSG:32632", item.at("/assets/data/proj:code").asText());
        assertTrue(item.at("/assets/data/raster:bands/0/statistics").has("mean"));
        String href = item.at("/assets/data/href").asText();
        assertTrue(href, href.startsWith(base + "/api/data/files/shared/" + COLLECTION + "/elevation.tif?expires="));
        assertTrue(item.get("links").toString().contains(base + "/api/stac/collections/" + COLLECTION + "/items/" + ELEVATION_ID));

        // the asset opens without token, in parts
        HttpResponse<byte[]> download = client.send(HttpRequest.newBuilder(URI.create(href)).header("Range", "bytes=0-9").build(),
                HttpResponse.BodyHandlers.ofByteArray());
        assertEquals(206, download.statusCode());
        assertEquals(10, download.body().length);
        assertEquals(401, send("GET", "/api/data/files/shared/elevation.tif", null, null).statusCode());
        assertEquals(404, send("GET", "/api/data/files/../hm-server.sqlite", janeToken, null).statusCode());

        // one item asked alone, only in its collection
        assertEquals(ELEVATION_ID, get(itemsOf(COLLECTION) + "/" + ELEVATION_ID, johnToken, 200).get("id").asText());
        assertEquals(404, send("GET", itemsOf(COLLECTION) + "/elevation.tif", janeToken, null).statusCode());

        // search: the elevation is near 4.5E 0N; the file directly in the data is not in the catalog
        assertEquals(ELEVATION_ID, ids(get("/api/stac/search?bbox=4,-1,5,1", janeToken, 200)));
        assertEquals("", ids(get("/api/stac/search?bbox=10,45,11,46", janeToken, 200)));
        assertEquals("", ids(get("/api/stac/search?datetime=2100-01-01T00:00:00Z/..", janeToken, 200)));
        assertEquals(ELEVATION_ID,
                ids(get("/api/stac/search?datetime=../2100-01-01T00:00:00Z&collections=" + COLLECTION, janeToken, 200)));
        assertEquals("", ids(get("/api/stac/search?collections=nothing", janeToken, 200)));
        assertEquals(400, send("GET", "/api/stac/search?bbox=1,2,3", janeToken, null).statusCode());
        assertEquals(400, send("GET", "/api/stac/search?datetime=yesterday", janeToken, null).statusCode());
        assertEquals(ELEVATION_ID, ids(MAPPER.readTree(send("POST", "/api/stac/search", janeToken,
                "{\"bbox\": [4, -1, 5, 1], \"collections\": [\"" + COLLECTION + "\"]}").body())));
    }

    public void testDataFollowsTheFiles() throws Exception {
        waitFor(itemsOf(COLLECTION), janeToken, json -> json.get("features").size() == 1);
        File elevation = new File(collectionFolder, "elevation.tif");

        // a new folder in the collection with a copy
        File folder = new File(collectionFolder, "dem");
        assertTrue(folder.mkdirs());
        File copy = new File(folder, "copy.tif");
        Files.copy(elevation.toPath(), copy.toPath());
        String itemPath = itemsOf(COLLECTION) + "/shared~dem~copy.tif";
        JsonNode items = waitFor(itemsOf(COLLECTION), janeToken, json -> json.get("features").size() == 2);
        assertTrue(ids(items), ids(items).contains("shared~dem~copy.tif"));
        JsonNode item = get(itemPath, janeToken, 200);
        assertEquals("data/shared/" + COLLECTION + "/dem/copy.tif", item.at("/properties/hm:path").asText());
        assertTrue(item.at("/assets/data/href").asText()
                .startsWith(base + "/api/data/files/shared/" + COLLECTION + "/dem/copy.tif?"));

        // a shapefile is one item, with its parts as assets
        for( String extension : new String[]{"dbf", "shx", "prj"} ) {
            FileUtilities.writeFile("part", new File(folder, "roads." + extension));
        }
        FileUtilities.writeFile("not really a shapefile", new File(folder, "roads.shp"));
        items = waitFor(itemsOf(COLLECTION), janeToken, json -> ids(json).contains("shared~dem~roads.shp")
                && !ids(json).contains("roads.dbf"));
        JsonNode roads = get(itemsOf(COLLECTION) + "/shared~dem~roads.shp", janeToken, 200);
        assertTrue(roads.at("/assets/dbf/href").asText()
                .startsWith(base + "/api/data/files/shared/" + COLLECTION + "/dem/roads.dbf?"));
        assertEquals("metadata", roads.at("/assets/prj/roles/0").asText());

        // changed: read again
        long size = item.at("/assets/data/file:size").asLong();
        FileUtilities.writeFile("not a tiff anymore", copy);
        waitFor(itemPath, janeToken, json -> json.at("/assets/data/file:size").asLong() != size);

        // gone: asked alone it goes at once, before the watcher
        Files.delete(copy.toPath());
        assertEquals(404, send("GET", itemPath, janeToken, null).statusCode());
        FileUtilities.deleteFileOrDir(folder);
        waitFor(itemsOf(COLLECTION), janeToken, json -> ids(json).equals(ELEVATION_ID));

        // a file moved in, as at the end of a copy; one in the data directly stays out
        FileUtilities.writeFile("root", new File(data, "root.txt"));
        File tmp = new File(workspace, "tmp/moved.tif");
        Files.copy(elevation.toPath(), tmp.toPath());
        Files.move(tmp.toPath(), new File(collectionFolder, "moved.tif").toPath(), StandardCopyOption.ATOMIC_MOVE);
        waitFor(itemsOf(COLLECTION), janeToken, json -> ids(json).contains("shared~moved.tif"));

        // a new folder is a new collection
        File other = new File(data, "other");
        assertTrue(other.mkdirs());
        FileUtilities.writeFile("other", new File(other, "other.txt"));
        waitFor("/api/stac/collections", janeToken, json -> collectionIds(json).equals(COLLECTION + ",other"));
        waitFor(itemsOf("other"), janeToken, json -> ids(json).equals("shared~other.txt"));
        assertFalse(get("/api/stac/search?limit=100", janeToken, 200).toString().contains("root.txt"));
    }

    public void testUploads() throws Exception {
        byte[] elevation = Files.readAllBytes(new File(collectionFolder, "elevation.tif").toPath());
        waitFor(itemsOf(COLLECTION), janeToken, json -> json.get("features").size() == 1);
        // in the folder of the shared data: the same collection
        server.getUploadManager().save(jane.username, jane.id,COLLECTION, "mine.tif", elevation.length, new ByteArrayInputStream(elevation));
        server.getUploadManager().save(jane.username, jane.id,"docs", "notes.txt", 5,
                new ByteArrayInputStream("notes".getBytes(StandardCharsets.UTF_8)));
        // directly in the area: not in the catalog
        server.getUploadManager().save(jane.username, jane.id,"", "loose.txt", 5,
                new ByteArrayInputStream("loose".getBytes(StandardCharsets.UTF_8)));

        // at once, only for jane and the administrators
        String itemId = "user~jane~mine.tif";
        assertEquals(ELEVATION_ID + "," + itemId, ids(get(itemsOf(COLLECTION), janeToken, 200)));
        JsonNode item = get(itemsOf(COLLECTION) + "/" + itemId, janeToken, 200);
        assertEquals("data/jane/" + COLLECTION + "/mine.tif", item.at("/properties/hm:path").asText());
        assertEquals("mine.tif", item.at("/properties/title").asText());
        assertEquals("jane", item.at("/properties/hm:area").asText());
        assertEquals("jane", item.at("/properties/hm:owner").asText());
        assertEquals("uploaded", item.at("/properties/hm:origin").asText());
        assertEquals("jane", item.at("/properties/hm:user").asText());
        String filesPath = "/api/data/files/jane/" + COLLECTION + "/mine.tif";
        String href = item.at("/assets/data/href").asText();
        assertTrue(href, href.startsWith(base + filesPath + "?expires="));
        assertEquals(200, send("GET", href, null, null).statusCode());
        JsonNode notes = get(itemsOf("docs") + "/user~jane~notes.txt", janeToken, 200);
        assertTrue(notes.get("geometry").isNull());
        assertFalse(notes.has("bbox"));
        assertFalse(get("/api/stac/search?limit=100", janeToken, 200).toString().contains("loose.txt"));

        assertEquals(ELEVATION_ID, ids(get(itemsOf(COLLECTION), johnToken, 200)));
        assertEquals(COLLECTION, collectionIds(get("/api/stac/collections", johnToken, 200)));
        assertEquals(404, send("GET", itemsOf(COLLECTION) + "/" + itemId, johnToken, null).statusCode());
        assertEquals(404, send("GET", itemsOf("docs"), johnToken, null).statusCode());
        assertEquals(404, send("GET", filesPath, johnToken, null).statusCode());
        assertEquals("", ids(get("/api/stac/search?ids=" + itemId, johnToken, 200)));
        assertEquals(itemId, ids(get("/api/stac/search?ids=" + itemId, adminToken, 200)));
        get(itemsOf("docs") + "/user~jane~notes.txt", adminToken, 200);
        assertEquals(200, send("GET", filesPath, adminToken, null).statusCode());

        // the path of an item is an input of the modules; the output goes next to it, in the same collection
        String run = "{\"inputs\": {\"inElev\": \"" + item.at("/properties/hm:path").asText() + "\"}}";
        HttpResponse<String> response = send("POST", "/api/processes/Pitfiller/execution", janeToken, run);
        assertEquals(response.body(), 201, response.statusCode());
        String jobId = MAPPER.readTree(response.body()).get("jobID").asText();
        assertEquals("successful", waitFinished(janeToken, jobId).get("status").asText());
        assertEquals("docs," + COLLECTION, collectionIds(get("/api/stac/collections", janeToken, 200)));
        assertEquals(4, get("/api/stac/search", janeToken, 200).get("numberMatched").asInt());
        // at once, with where it comes from
        JsonNode pit = get(itemsOf(COLLECTION) + "/user~jane~pit.tif", janeToken, 200);
        assertEquals("processed", pit.at("/properties/hm:origin").asText());
        assertEquals("Pitfiller", pit.at("/properties/hm:module").asText());
        assertEquals(jobId, pit.at("/properties/hm:job").asText());
        assertEquals(item.at("/properties/hm:path").asText(), pit.at("/properties/hm:parameters/inElev").asText());
        assertTrue(pit.get("stac_extensions").toString().contains("processing"));
        assertTrue(pit.at("/properties/processing:software").has("HortonMachine"));
        assertFalse(pit.get("properties").has("hm:history"));
        assertEquals(404, send("GET", itemsOf(COLLECTION) + "/user~jane~pit.tif", johnToken, null).statusCode());

        // replaced: the previous one in the history
        response = send("POST", "/api/processes/Pitfiller/execution", janeToken, run.replace("}}", "}, \"overwrite\": true}"));
        assertEquals(response.body(), 201, response.statusCode());
        String again = MAPPER.readTree(response.body()).get("jobID").asText();
        assertEquals("successful", waitFinished(janeToken, again).get("status").asText());
        pit = get(itemsOf(COLLECTION) + "/user~jane~pit.tif", janeToken, 200);
        assertEquals(again, pit.at("/properties/hm:job").asText());
        assertEquals(jobId, pit.at("/properties/hm:history/0/hm:job").asText());
        assertTrue(pit.at("/properties/hm:history/0").has("hm:replaced"));

        // deleted: at once
        assertTrue(server.getUploadManager().delete(jane.username, COLLECTION));
        assertEquals(ELEVATION_ID, ids(get(itemsOf(COLLECTION), janeToken, 200)));
        server.getUploadManager().deleteAll(jane.username);
        assertEquals(404, send("GET", itemsOf("docs"), janeToken, null).statusCode());
        assertEquals(1, get("/api/stac/search", janeToken, 200).get("numberMatched").asInt());
    }

    /**
     * The catalog read by the stac client of the HortonMachine, as the STAC browser does.
     */
    public void testHortonMachineClient() throws Exception {
        waitFor(itemsOf(COLLECTION), janeToken, json -> json.get("features").size() == 1);
        byte[] elevation = Files.readAllBytes(new File(collectionFolder, "elevation.tif").toPath());
        server.getUploadManager().save(jane.username, jane.id,"mine", "mine.tif", elevation.length, new ByteArrayInputStream(elevation));

        try (HMStacManager manager = new HMStacManager(base + "/api/stac", new DummyProgressMonitor())) {
            manager.setAccess(new HMStacAccess(HMHttpAuthentication.bearer(base + "/api/stac", janeToken)));
            manager.open();
            assertTrue(manager.isItemSearchAvailable());
            List<String> ids = manager.getCollections().stream().map(HMStacCollection::getId).collect(Collectors.toList());
            assertEquals("[" + COLLECTION + ", mine]", ids.toString());

            HMStacCollection dataCollection = manager.getCollectionById(COLLECTION);
            // the time extent of the files, as the browser fills its time filter with
            List<java.util.Date> bounds = dataCollection.getTemporalBounds();
            assertEquals(String.valueOf(bounds), 2, bounds.size());
            assertNotNull(String.valueOf(bounds), bounds.get(0));
            assertNotNull(String.valueOf(bounds), bounds.get(1));
            List<HMStacItem> items = dataCollection.searchItems();
            assertEquals(1, items.size());
            HMStacItem item = items.get(0);
            assertEquals(ELEVATION_ID, item.getId());
            HMStacAsset asset = item.getAssets().stream().filter(a -> a.getId().equals("data")).findFirst().get();
            assertTrue(asset.getNonValidReason(), asset.isValid());

            // the raster, read through the authentication
            IHMStacAssetHandler handler = asset.getHandler();
            assertNotNull(handler);
            GridCoverage2D coverage = handler.read(GridCoverage2D.class, new DummyProgressMonitor());
            assertEquals(5, coverage.getRenderedImage().getWidth());
            assertEquals(5.0, coverage.evaluate((Position) new Position2D(coverage.getCoordinateReferenceSystem(), 25, 25), new double[1])[0],
                    1e-9);

            // the filters are searches of the server
            assertEquals(0, manager.getCollectionById(COLLECTION).setBboxFilter(new double[]{10, 45, 11, 46}).searchItems().size());
            assertEquals(1, manager.getCollectionById(COLLECTION).setBboxFilter(new double[]{4, -1, 5, 1}).searchItems().size());
            assertEquals(1, manager.getCollectionById("mine").searchItems().size());
            // open time ranges
            java.util.Date future = new java.util.Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(1));
            assertEquals(1, manager.getCollectionById(COLLECTION).setTimestampFilter(null, future).searchItems().size());
            assertEquals(0, manager.getCollectionById(COLLECTION).setTimestampFilter(future, null).searchItems().size());
            // the extent of a collection with one item is an instant with milliseconds: the filter keeps them
            HMStacCollection uploads = manager.getCollectionById("mine");
            List<java.util.Date> extent = uploads.getTemporalBounds();
            assertEquals(1, uploads.setTimestampFilter(extent.get(0), extent.get(1)).searchItems().size());
        }

        // another user doesn't see the uploads of jane
        try (HMStacManager manager = new HMStacManager(base + "/api/stac", new DummyProgressMonitor())) {
            manager.setAccess(new HMStacAccess(HMHttpAuthentication.bearer(base, johnToken)));
            manager.open();
            assertNull(manager.getCollectionById("mine"));
            assertEquals(1, manager.getCollectionById(COLLECTION).searchItems().size());
        }

        // without token nothing
        try (HMStacManager manager = new HMStacManager(base + "/api/stac", new DummyProgressMonitor())) {
            manager.open();
            manager.getCollections();
            fail("The catalog needs a token.");
        } catch (Exception e) {
            // refused
        }
    }

    public void testPaging() throws Exception {
        waitFor(itemsOf(COLLECTION), janeToken, json -> json.get("features").size() == 1);
        for( String name : new String[]{"a.txt", "b.txt"} ) {
            FileUtilities.writeFile(name, new File(collectionFolder, name));
        }
        waitFor(itemsOf(COLLECTION), janeToken, json -> json.get("features").size() == 3);

        JsonNode page = get("/api/stac/search?collections=" + COLLECTION + "&limit=2", janeToken, 200);
        assertEquals("shared~a.txt,shared~b.txt", ids(page));
        assertEquals(3, page.get("numberMatched").asInt());
        String next = null;
        for( JsonNode link : page.get("links") ) {
            if (link.get("rel").asText().equals("next")) {
                next = link.get("href").asText();
            }
        }
        assertNotNull(page.toString(), next);
        page = get(next, janeToken, 200);
        assertEquals(ELEVATION_ID, ids(page));
        assertFalse(page.get("links").toString().contains("\"next\""));

        // POST: the next page is the same body with the token
        page = MAPPER.readTree(
                send("POST", "/api/stac/search", janeToken, "{\"collections\": [\"" + COLLECTION + "\"], \"limit\": 2}").body());
        JsonNode nextLink = null;
        for( JsonNode link : page.get("links") ) {
            if (link.get("rel").asText().equals("next")) {
                nextLink = link;
            }
        }
        assertEquals("POST", nextLink.get("method").asText());
        page = MAPPER.readTree(send("POST", nextLink.get("href").asText(), janeToken, nextLink.get("body").toString()).body());
        assertEquals(ELEVATION_ID, ids(page));
    }
}
