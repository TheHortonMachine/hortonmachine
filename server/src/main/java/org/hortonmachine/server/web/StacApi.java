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
package org.hortonmachine.server.web;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.hortonmachine.Version;
import org.hortonmachine.server.WorkspacePaths;
import org.hortonmachine.server.db.ServerDb;
import org.hortonmachine.server.db.ServerDb.StacCollectionExtent;
import org.hortonmachine.server.stac.StacCollectionInfo;
import org.hortonmachine.server.stac.StacIndexer;
import org.hortonmachine.server.stac.StacItems;
import org.hortonmachine.server.stac.StacRecord;
import org.hortonmachine.server.stac.StacSearch;
import org.hortonmachine.server.users.User;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import io.javalin.config.JavalinConfig;
import io.javalin.http.Context;
import io.javalin.http.Handler;

/**
 * The files of the server as STAC API 1.0 (https://github.com/radiantearth/stac-api-spec), under
 * {@value #STAC}: a catalog whose collections are the first level folders of the areas of the data, as found by the
 * {@link StacIndexer}: the uploads and the outputs of the jobs are items as the files put there from outside.
 *
 * <p>A collection is described by the metadata the administrators gave it, else by the name of its folder.
 *
 * <p>A token sees the shared area and the area of its user, the administrators all of them. The hrefs of
 * the assets are links signed by the {@link UrlSigner}, if they are on, and the files can be read in parts, so that
 * QGIS and GDAL open them directly.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class StacApi {
    private static final Logger LOGGER = Logger.getLogger(StacApi.class.getName());

    public static final String STAC = Auth.API_PREFIX + "/stac";
    private static final String JSON = "application/json";
    private static final String GEOJSON = "application/geo+json";
    public static final String STAC_API_VERSION = "1.0.0";
    private static final List<String> CONFORMANCE = List.of( //
            "https://api.stacspec.org/v1.0.0/core", //
            "https://api.stacspec.org/v1.0.0/collections", //
            "https://api.stacspec.org/v1.0.0/ogcapi-features", //
            "https://api.stacspec.org/v1.0.0/item-search", //
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/core", //
            "http://www.opengis.net/spec/ogcapi-features-1/1.0/conf/geojson");
    public static final int DEFAULT_LIMIT = 10;
    public static final int MAX_LIMIT = 1000;

    /** The path of the files of the data, for the assets and the results of the jobs: followed by area/path. */
    public static final String DATA_FILES = Auth.API_PREFIX + "/data" + Auth.FILES_PATH;

    private final ServerDb db;
    private final StacIndexer indexer;
    private final WorkspacePaths paths;
    private final UrlSigner signer;
    private final ObjectMapper mapper = new ObjectMapper();

    public StacApi( ServerDb db, StacIndexer indexer, WorkspacePaths paths, UrlSigner signer ) {
        this.db = db;
        this.indexer = indexer;
        this.paths = paths;
        this.signer = signer;
    }

    public void register( JavalinConfig config ) {
        config.routes.get(STAC, api(this::landingPage));
        config.routes.get(STAC + "/conformance", api(this::conformance));
        config.routes.get(STAC + "/collections", api(this::collections));
        config.routes.get(STAC + "/collections/{collectionId}", api(this::collection));
        config.routes.get(STAC + "/collections/{collectionId}/items", api(this::items));
        config.routes.get(STAC + "/collections/{collectionId}/items/{itemId}", api(this::item));
        config.routes.get(STAC + "/search", api(this::searchGet));
        config.routes.post(STAC + "/search", api(this::searchPost));
        config.routes.get(DATA_FILES + "<path>", api(this::dataFile));
        config.routes.head(DATA_FILES + "<path>", api(this::dataFile));
    }

    private static Handler api( Handler handler ) {
        return ctx -> {
            try {
                handler.handle(ctx);
            } catch (ApiProblem problem) {
                problem.send(ctx);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error in " + ctx.method() + " " + ctx.path(), e);
                new ApiProblem(500, "about:blank", "Internal server error", e.getMessage()).send(ctx);
            }
        };
    }

    private void sendJson( Context ctx, String type, JsonNode json ) throws Exception {
        ctx.status(200).contentType(type).result(mapper.writeValueAsString(json));
    }

    private ObjectNode link( ArrayNode links, String href, String rel, String type, String title ) {
        ObjectNode link = links.addObject().put("rel", rel).put("href", href);
        if (type != null) {
            link.put("type", type);
        }
        if (title != null) {
            link.put("title", title);
        }
        return link;
    }

    private static String base( Context ctx ) {
        return OgcApi.baseUrl(ctx) + STAC;
    }

    private static StacSearch newSearch( Context ctx ) {
        User user = Auth.user(ctx);
        return new StacSearch(user.username, user.isAdmin());
    }

    private static ApiProblem badRequest( String detail ) {
        return new ApiProblem(400, ApiProblem.INVALID_PARAMETER, "Invalid parameter value", detail);
    }

    private static ApiProblem notFound( String detail ) {
        return new ApiProblem(404, "about:blank", "Not found", detail);
    }

    private void landingPage( Context ctx ) throws Exception {
        String base = base(ctx);
        ObjectNode catalog = mapper.createObjectNode().put("type", "Catalog").put("id", "hortonmachine")
                .put("stac_version", StacItems.STAC_VERSION).put("title", "HortonMachine data").put("description",
                        "The shared data and your uploads on the HortonMachine server " + Version.getVersion()
                                + ", a collection per folder.");
        ArrayNode conformsTo = catalog.putArray("conformsTo");
        CONFORMANCE.forEach(conformsTo::add);
        ArrayNode links = catalog.putArray("links");
        link(links, base, "self", JSON, "This catalog");
        link(links, base, "root", JSON, "This catalog");
        link(links, base + "/conformance", "conformance", JSON, "The conformance classes");
        link(links, base + "/collections", "data", JSON, "The collections");
        link(links, base + "/search", "search", GEOJSON, "Search the items").put("method", "GET");
        link(links, base + "/search", "search", GEOJSON, "Search the items").put("method", "POST");
        link(links, OgcApi.baseUrl(ctx) + Auth.API_PREFIX + "/docs", "service-doc", "text/html", "The documentation of the api");
        for( String collection : visibleCollections(ctx) ) {
            link(links, base + "/collections/" + Html.urlPath(collection), "child", JSON,
                    collectionTitle(collection, db.getStacCollectionInfo(collection)));
        }
        sendJson(ctx, JSON, catalog);
    }

    private void conformance( Context ctx ) throws Exception {
        ObjectNode conformance = mapper.createObjectNode();
        ArrayNode classes = conformance.putArray("conformsTo");
        CONFORMANCE.forEach(classes::add);
        sendJson(ctx, JSON, conformance);
    }

    /**
     * @return the ids of the collections the requester sees: the folders of the data and of its uploads, also empty.
     */
    private List<String> visibleCollections( Context ctx ) {
        User user = Auth.user(ctx);
        return indexer.listCollections(user.username, user.isAdmin());
    }

    private static boolean isGiven( String value ) {
        return value != null && !value.isBlank();
    }

    /**
     * @param info the metadata of the administrators, <code>null</code> if none.
     */
    private static String collectionTitle( String collection, StacCollectionInfo info ) {
        return info != null && isGiven(info.title) ? info.title : collection;
    }

    private static String collectionDescription( String collection, StacCollectionInfo info ) {
        return info != null && isGiven(info.description) ? info.description
                : "The files of the folder " + collection + " of the shared data and of the uploads.";
    }

    private ObjectNode collectionJson( Context ctx, String collection, StacCollectionExtent extent ) throws Exception {
        String base = base(ctx);
        String href = base + "/collections/" + Html.urlPath(collection);
        StacCollectionInfo info = db.getStacCollectionInfo(collection);
        ObjectNode json = mapper.createObjectNode().put("type", "Collection").put("stac_version", StacItems.STAC_VERSION)
                .put("id", collection).put("title", collectionTitle(collection, info))
                .put("description", collectionDescription(collection, info))
                .put("license", info != null && isGiven(info.license) ? info.license : "other");
        if (info != null && !info.keywords.isEmpty()) {
            ArrayNode keywords = json.putArray("keywords");
            info.keywords.forEach(keywords::add);
        }
        ObjectNode extentJson = json.putObject("extent");
        ArrayNode bbox = extentJson.putObject("spatial").putArray("bbox").addArray();
        double[] bounds = extent != null && extent.bbox != null ? extent.bbox : new double[]{-180, -90, 180, 90};
        Arrays.stream(bounds).forEach(bbox::add);
        ArrayNode interval = extentJson.putObject("temporal").putArray("interval").addArray();
        if (extent != null && extent.start != null) {
            interval.add(Instant.ofEpochMilli(extent.start).toString());
        } else {
            interval.addNull();
        }
        if (extent != null && extent.end != null) {
            interval.add(Instant.ofEpochMilli(extent.end).toString());
        } else {
            interval.addNull();
        }
        ArrayNode links = json.putArray("links");
        link(links, href, "self", JSON, null);
        link(links, base, "root", JSON, null);
        link(links, base, "parent", JSON, null);
        link(links, href + "/items", "items", GEOJSON, "The items");
        return json;
    }

    private void collections( Context ctx ) throws Exception {
        List<StacCollectionExtent> extents = db.listStacCollections(newSearch(ctx));
        ObjectNode json = mapper.createObjectNode();
        ArrayNode collections = json.putArray("collections");
        for( String collection : visibleCollections(ctx) ) {
            StacCollectionExtent extent = extents.stream().filter(e -> e.collection.equals(collection)).findFirst().orElse(null);
            collections.add(collectionJson(ctx, collection, extent));
        }
        ArrayNode links = json.putArray("links");
        link(links, base(ctx) + "/collections", "self", JSON, null);
        link(links, base(ctx), "root", JSON, null);
        sendJson(ctx, JSON, json);
    }

    private String getCollectionId( Context ctx ) {
        String collection = ctx.pathParam("collectionId");
        if (!visibleCollections(ctx).contains(collection)) {
            throw notFound("Unknown collection: " + collection);
        }
        return collection;
    }

    private void collection( Context ctx ) throws Exception {
        String collection = getCollectionId(ctx);
        StacSearch search = newSearch(ctx);
        search.collections.add(collection);
        List<StacCollectionExtent> extents = db.listStacCollections(search);
        sendJson(ctx, JSON, collectionJson(ctx, collection, extents.isEmpty() ? null : extents.get(0)));
    }

    private void items( Context ctx ) throws Exception {
        String collection = getCollectionId(ctx);
        StacSearch search = newSearch(ctx);
        search.collections.add(collection);
        readCommonParameters(search, ctx.queryParam("bbox"), ctx.queryParam("datetime"), ctx.queryParam("limit"),
                ctx.queryParam("token"));
        String self = base(ctx) + "/collections/" + Html.urlPath(collection) + "/items";
        sendItems(ctx, search, self, null);
    }

    private void item( Context ctx ) throws Exception {
        String collection = getCollectionId(ctx);
        String itemId = ctx.pathParam("itemId");
        StacRecord record = db.getStacItem(collection, itemId);
        if (record == null || !isVisible(ctx, record)) {
            throw notFound("Unknown item: " + itemId);
        }
        // asked alone: as the files are now
        record = indexer.current(record);
        if (record == null) {
            throw notFound("The files of the item are gone: " + itemId);
        }
        sendJson(ctx, GEOJSON, publicItem(ctx, record));
    }

    private static boolean isVisible( Context ctx, StacRecord record ) {
        User user = Auth.user(ctx);
        // the owner of an item is the area of its user, named by the username
        return user.isAdmin() || record.ownerId == null || user.username.equals(record.ownerId);
    }

    private void searchGet( Context ctx ) throws Exception {
        if (ctx.queryParam("intersects") != null) {
            throw badRequest("intersects is not supported, use bbox.");
        }
        StacSearch search = newSearch(ctx);
        addAll(search.collections, ctx.queryParam("collections"));
        addAll(search.ids, ctx.queryParam("ids"));
        readCommonParameters(search, ctx.queryParam("bbox"), ctx.queryParam("datetime"), ctx.queryParam("limit"),
                ctx.queryParam("token"));
        sendItems(ctx, search, base(ctx) + "/search", null);
    }

    private void searchPost( Context ctx ) throws Exception {
        JsonNode body;
        try {
            body = ctx.body().isBlank() ? mapper.createObjectNode() : mapper.readTree(ctx.body());
        } catch (Exception e) {
            throw badRequest("The body is not valid JSON: " + e.getMessage());
        }
        if (!body.isObject()) {
            throw badRequest("The body must be a JSON object.");
        }
        if (body.has("intersects")) {
            throw badRequest("intersects is not supported, use bbox.");
        }
        StacSearch search = newSearch(ctx);
        for( JsonNode collection : body.path("collections") ) {
            search.collections.add(collection.asText());
        }
        for( JsonNode id : body.path("ids") ) {
            search.ids.add(id.asText());
        }
        String bbox = null;
        if (body.has("bbox")) {
            List<String> values = new ArrayList<>();
            body.get("bbox").forEach(value -> values.add(value.asText()));
            bbox = String.join(",", values);
        }
        readCommonParameters(search, bbox, body.has("datetime") ? body.get("datetime").asText() : null,
                body.has("limit") ? body.get("limit").asText() : null, body.has("token") ? body.get("token").asText() : null);
        sendItems(ctx, search, base(ctx) + "/search", (ObjectNode) body);
    }

    private static void addAll( Set<String> set, String commaSeparated ) {
        if (commaSeparated != null && !commaSeparated.isBlank()) {
            Arrays.stream(commaSeparated.split(",")).map(String::trim).filter(v -> !v.isEmpty()).forEach(set::add);
        }
    }

    /**
     * Read bbox, datetime, limit and token into a search.
     */
    static void readCommonParameters( StacSearch search, String bbox, String datetime, String limit, String token ) {
        if (bbox != null && !bbox.isBlank()) {
            String[] parts = bbox.split(",");
            double[] values = new double[parts.length];
            try {
                for( int i = 0; i < parts.length; i++ ) {
                    values[i] = Double.parseDouble(parts[i].trim());
                }
            } catch (NumberFormatException e) {
                throw badRequest("The bbox must be 4 or 6 numbers: " + bbox);
            }
            if (values.length == 4) {
                search.bbox = values;
            } else if (values.length == 6) {
                search.bbox = new double[]{values[0], values[1], values[3], values[4]};
            } else {
                throw badRequest("The bbox must be 4 or 6 numbers: " + bbox);
            }
            if (search.bbox[0] > search.bbox[2] || search.bbox[1] > search.bbox[3]) {
                throw badRequest("The bbox must be minLon,minLat,maxLon,maxLat, across the antimeridian is not supported: " + bbox);
            }
        }
        if (datetime != null && !datetime.isBlank()) {
            String[] parts = datetime.split("/", -1);
            if (parts.length == 1) {
                search.start = parseTime(parts[0]);
                search.end = search.start;
            } else if (parts.length == 2) {
                search.start = parts[0].isEmpty() || parts[0].equals("..") ? null : parseTime(parts[0]);
                search.end = parts[1].isEmpty() || parts[1].equals("..") ? null : parseTime(parts[1]);
            } else {
                throw badRequest("The datetime must be a time or an interval as start/end: " + datetime);
            }
            if (search.start == null && search.end == null && parts.length == 1) {
                throw badRequest("The datetime must be a time or an interval as start/end: " + datetime);
            }
        }
        if (limit != null && !limit.isBlank()) {
            try {
                search.limit = Math.max(1, Math.min(MAX_LIMIT, Integer.parseInt(limit.trim())));
            } catch (NumberFormatException e) {
                throw badRequest("The limit must be a number: " + limit);
            }
        } else {
            search.limit = DEFAULT_LIMIT;
        }
        if (token != null && !token.isBlank()) {
            try {
                search.offset = Math.max(0, Integer.parseInt(token.trim()));
            } catch (NumberFormatException e) {
                throw badRequest("Not a token of this server: " + token);
            }
        }
    }

    private static long parseTime( String time ) {
        try {
            return OffsetDateTime.parse(time.trim()).toInstant().toEpochMilli();
        } catch (DateTimeParseException e) {
            throw badRequest("Not an RFC 3339 time, as 2026-10-18T09:00:00Z: " + time);
        }
    }

    /**
     * Send a page of the items found by a search, as item collection.
     *
     * @param postBody the body of a POST search, for the link to the next page, <code>null</code> for GET.
     */
    private void sendItems( Context ctx, StacSearch search, String self, ObjectNode postBody ) throws Exception {
        List<StacRecord> records = db.searchStacItems(search);
        long matched = db.countStacItems(search);
        ObjectNode json = mapper.createObjectNode().put("type", "FeatureCollection");
        ArrayNode features = json.putArray("features");
        for( StacRecord record : records ) {
            features.add(publicItem(ctx, record));
        }
        json.put("numberMatched", matched);
        json.put("numberReturned", records.size());
        ArrayNode links = json.putArray("links");
        link(links, base(ctx), "root", JSON, null);
        String query = ctx.queryString();
        link(links, self + (postBody == null && query != null ? "?" + query : ""), "self", GEOJSON, null);
        int next = search.offset + records.size();
        if (next < matched) {
            if (postBody == null) {
                String others = query == null ? "" : query.replaceAll("(^|&)token=[^&]*", "");
                others = others.startsWith("&") ? others.substring(1) : others;
                link(links, self + "?" + (others.isEmpty() ? "" : others + "&") + "token=" + next, "next", GEOJSON, "The next page");
            } else {
                ObjectNode nextBody = postBody.deepCopy().put("token", String.valueOf(next));
                ObjectNode nextLink = link(links, self, "next", GEOJSON, "The next page").put("method", "POST");
                nextLink.set("body", nextBody);
            }
        }
        sendJson(ctx, GEOJSON, json);
    }

    /**
     * @return the path of the api of the folder of the files of an item, url encoded, ending with /.
     */
    private static String filesFolder( StacRecord record ) {
        // the path of an item is in an area of the data, data/<area>/...
        String folder = record.path.substring(0, record.path.lastIndexOf('/'));
        return DATA_FILES + Html.urlPath(folder.substring(WorkspacePaths.DATA.length() + 1)) + "/";
    }

    /**
     * @return the item as given to the clients: absolute and signed hrefs and its links.
     */
    private ObjectNode publicItem( Context ctx, StacRecord record ) throws Exception {
        ObjectNode item = (ObjectNode) mapper.readTree(record.json);
        String folder = filesFolder(record);
        for( Map.Entry<String, JsonNode> asset : item.path("assets").properties() ) {
            String href = asset.getValue().path("href").asText();
            if (href.startsWith("./")) {
                ((ObjectNode) asset.getValue()).put("href", OgcApi.fileHref(signer, ctx, folder + href.substring(2)));
            }
        }
        String base = base(ctx);
        String collection = base + "/collections/" + Html.urlPath(record.collection);
        ArrayNode links = item.putArray("links");
        link(links, collection + "/items/" + Html.urlPath(record.id), "self", GEOJSON, null);
        link(links, collection, "parent", JSON, null);
        link(links, collection, "collection", JSON, null);
        link(links, base, "root", JSON, null);
        return item;
    }

    /**
     * @return the file under a folder at a relative path of a request, or <code>null</code> if it is not one.
     */
    private static Path resolveFile( Path root, String relative ) {
        String clean = relative.replace('\\', '/');
        Path file = root.resolve(clean).normalize();
        if (List.of(clean.split("/")).contains("..") || !file.startsWith(root)) {
            return null;
        }
        return file;
    }

    /**
     * A file of the data, as area/path: those of the shared area and of the own area, all for the administrators.
     */
    private void dataFile( Context ctx ) throws Exception {
        User user = Auth.user(ctx);
        String path = ctx.pathParam("path");
        if (!WorkspacePaths.canRead(WorkspacePaths.areaOf(WorkspacePaths.DATA + "/" + path), user.username, user.isAdmin())) {
            throw notFound("The file doesn't exist.");
        }
        Path root = paths.getDataFolder().toPath().toAbsolutePath().normalize();
        sendFile(ctx, resolveFile(root, path));
    }

    private void sendFile( Context ctx, Path file ) throws Exception {
        if (file == null) {
            throw notFound("The file doesn't exist.");
        }
        // gone: out of the catalog at once; else checked soon, in case it changed
        indexer.fileRequested(file);
        if (!Files.isRegularFile(file)) {
            throw notFound("The file doesn't exist.");
        }
        Downloads.send(ctx, file, false);
    }
}
