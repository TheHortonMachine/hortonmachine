package org.hortonmachine.gears.io.stac.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPResponse;
import org.geotools.stac.client.Collection;
import org.geotools.stac.client.CollectionList;
import org.geotools.stac.client.HttpMethod;
import org.geotools.stac.client.Link;
import org.geotools.stac.client.STACClient;
import org.geotools.stac.client.STACConformance;
import org.geotools.stac.client.SearchQuery;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class HMSTACClient extends STACClient {

	static ObjectMapper OBJECT_MAPPER;

	/**
	 * Initialize an ObjectMapper that's tolerant, won't generate missing fields,
	 * and can parse GeoJSON geometries
	 */
	static {
		OBJECT_MAPPER = new ObjectMapper();
		OBJECT_MAPPER.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
		OBJECT_MAPPER.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
		OBJECT_MAPPER.registerModule(new JtsModule());
		OBJECT_MAPPER.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
	}

	/** Maximum depth of nested catalogs followed through child links. */
	private static final int MAX_CHILD_DEPTH = 5;

	private final URL landingPageURL;

	/** The documents the collections found following child links have been read from. */
	private final Map<String, URL> collectionUrls = new ConcurrentHashMap<>();

	public HMSTACClient(URL landingPageURL, HTTPClient http) throws IOException {
		super(landingPageURL, http);
		this.landingPageURL = landingPageURL;
	}

	/**
	 * More lenient check: accept both application/geo+json and application/json
	 */
	private void checkGeoOrJsonResponse(HTTPResponse response) {
		String mime = response.getContentType();
		if (mime == null) {
			throw new IllegalArgumentException("Was expecting a GeoJSON response, got a null mime type");
		}
		if (!mime.startsWith(GEOJSON_MIME) && !mime.startsWith(JSON_MIME)) {
			throw new IllegalArgumentException(
					"Was expecting a GeoJSON (or JSON) response, got a different mime type: " + mime);
		}
	}

	@Override
	public SimpleFeatureCollection search(SearchQuery search, SearchMode mode, SimpleFeatureType schema)
			throws IOException {

		// Same conformance check as base class
		if (!STACConformance.ITEM_SEARCH.matches(getLandingPage().getConformance())) {
			throw new IllegalStateException(
					"The server does not support the item-search conformance class, cannot query it");
		}

		try {
			HTTPResponse response = null;
			HTTPClient http = getHttp();

			if (mode == SearchMode.GET) {
				URL getURL = new HMSearchGetBuilder(getLandingPage()).toGetURL(search);

//                LOGGER.log(Level.FINE, () -> "STAC GET search request: " + getURL);
				response = http.get(getURL);
			} else {
				String url = getLandingPage().getSearchLink(HttpMethod.POST);
				if (url == null) {
					throw new IllegalArgumentException("Cannot find GeoJSON search POST link");
				}
				URL postURL = new URL(url);

				String body = OBJECT_MAPPER.writeValueAsString(search);

//                LOGGER.log(Level.FINE, () -> "STAC POST search request: " + postURL + " with body:\n" + body);
				response = http.post(postURL, new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
						"application/json");
			}

			// <<< this is the only semantic change: lenient mime check
			checkGeoOrJsonResponse(response);

			try (HMSTACGeoJSONReader reader = new HMSTACGeoJSONReader(
					new BufferedInputStream(response.getResponseStream(), 1024 * 32), http)) {
				if (schema != null) {
					reader.setSchema(schema);
				}
				return reader.getFeatures();
			}
		} catch (URISyntaxException e) {
			throw new IOException("Failed to build the search query URL", e);
		}
	}

	/**
	 * Get the collections of the catalog.
	 *
	 * <p>Same discovery logic as base class: look for rel=data, fallback to rel=children.
	 * If that doesn't find any collection, the rel=child links of the catalog are followed,
	 * as done for static catalogs: children can be collections or nested catalogs.</p>
	 */
	@Override
	public List<Collection> getCollections() throws IOException {
		List<Link> links = getLandingPage().getLinks();
		List<Collection> collections = getCollectionsFromDataLinks(links, landingPageURL);
		if (collections.isEmpty()) {
			Set<String> visited = new HashSet<>();
			visited.add(landingPageURL.toString());
			collections = new ArrayList<>();
			collectFromChildLinks(links, landingPageURL, 0, visited, collections);
		}
		return collections;
	}

	private List<Collection> getCollectionsFromDataLinks(List<Link> links, URL baseUrl) throws IOException {
		Optional<Link> maybeData = links.stream().filter(this::isDataJSONLink).findFirst();
		if (maybeData.isEmpty()) {
			maybeData = links.stream().filter(this::isChildrenJSONLink).findFirst();
		}
		if (maybeData.isEmpty())
			return Collections.emptyList();

		return readCollectionPages(resolve(baseUrl, maybeData.get().getHref()));
	}

	/**
	 * Follow the child links, adding the collections found and recursing into the catalogs.
	 */
	private void collectFromChildLinks(List<Link> links, URL baseUrl, int depth, Set<String> visited,
			List<Collection> collections) throws IOException {
		if (links == null || depth > MAX_CHILD_DEPTH)
			return;
		for (Link link : links) {
			if (!isChildJSONLink(link))
				continue;
			URL childUrl = resolve(baseUrl, link.getHref());
			if (childUrl == null || !visited.add(childUrl.toString()))
				continue;

			// no mime check here, static catalogs are often served with generic mime types
			JsonNode node;
			try (InputStream is = getHttp().get(childUrl).getResponseStream()) {
				node = OBJECT_MAPPER.readTree(is);
			}
			if (node == null || !node.isObject())
				continue;
			String type = node.hasNonNull("type") ? node.get("type").asText() : "";
			if ("collection".equalsIgnoreCase(type)) {
				Collection collection = OBJECT_MAPPER.treeToValue(node, Collection.class);
				boolean isNew = collections.stream().noneMatch(c -> c.getId().equals(collection.getId()));
				if (isNew) {
					collections.add(collection);
					// static collections often have no self link, keep where they come from
					collectionUrls.put(collection.getId(), childUrl);
				}
			} else if ("catalog".equalsIgnoreCase(type)) {
				List<Link> catalogLinks = node.has("links")
						? Arrays.asList(OBJECT_MAPPER.treeToValue(node.get("links"), Link[].class))
						: Collections.emptyList();
				List<Collection> dataCollections = getCollectionsFromDataLinks(catalogLinks, childUrl);
				if (!dataCollections.isEmpty()) {
					for (Collection collection : dataCollections) {
						if (collections.stream().noneMatch(c -> c.getId().equals(collection.getId())))
							collections.add(collection);
					}
				} else {
					collectFromChildLinks(catalogLinks, childUrl, depth + 1, visited, collections);
				}
			}
		}
	}

	private List<Collection> readCollectionPages(URL pageUrl) throws IOException {
		HTTPClient http = getHttp();

		List<Collection> all = new ArrayList<>();

		while (pageUrl != null) {
			HTTPResponse response = http.get(pageUrl);
			checkJSONResponse(response);

			CollectionList page;
			try (InputStream is = response.getResponseStream()) {
				page = OBJECT_MAPPER.readValue(is, CollectionList.class);
			}

			if (page.getCollections() != null) {
				all.addAll(page.getCollections().stream()
						// discard eventual sub-catalogs
						.filter(c -> c.getType() == null || "collection".equalsIgnoreCase(c.getType()))
						.collect(Collectors.toList()));
			}

			// Follow pagination: look for rel="next"
			String nextHref = null;
			if (page.getLinks() != null) {
				nextHref = page.getLinks().stream().filter(l -> "next".equalsIgnoreCase(l.getRel())).map(Link::getHref)
						.findFirst().orElse(null);
			}

			pageUrl = resolveNext(pageUrl, nextHref);
		}

		return all;
	}

	/**
	 * @return <code>true</code> if the catalog declares the item search conformance.
	 */
	public boolean supportsItemSearch() throws IOException {
		List<String> conformance = getLandingPage().getConformance();
		return conformance != null && STACConformance.ITEM_SEARCH.matches(conformance);
	}

	/**
	 * Search the items of a collection of a catalog without item search (e.g. a static catalog),
	 * following the item and child links of the collection and filtering the items locally.
	 *
	 * <p>Supported filters: bbox, intersects, datetime and filter (evaluated on the item features).</p>
	 *
	 * @param collection the collection to search.
	 * @param search the query.
	 * @param maxItems the maximum number of items to return, all if <= 0.
	 * @param pm the monitor, used also to stop the search when canceled.
	 * @return the item features, with asset hrefs resolved to absolute addresses.
	 * @throws IOException
	 */
	public List<SimpleFeature> searchStatic(Collection collection, SearchQuery search, int maxItems, IHMProgressMonitor pm)
			throws IOException {
		URL collectionUrl = collectionUrls.get(collection.getId());
		if (collectionUrl == null) {
			String self = collection.getLinks() == null ? null
					: collection.getLinks().stream().filter(l -> "self".equals(l.getRel())).map(Link::getHref).findFirst()
							.orElse(null);
			if (self == null)
				throw new IOException("Unable to find the document of collection " + collection.getId());
			collectionUrl = resolve(landingPageURL, self);
		}

		StaticItemFilter filter = new StaticItemFilter(search);
		List<SimpleFeature> items = new ArrayList<>();
		Set<String> visited = new HashSet<>();
		visited.add(collectionUrl.toString());
		collectStaticItems(readJson(collectionUrl), collectionUrl, filter, maxItems, pm, 0, visited, items);
		return items;
	}

	private void collectStaticItems(JsonNode document, URL documentUrl, StaticItemFilter filter, int maxItems,
			IHMProgressMonitor pm, int depth, Set<String> visited, List<SimpleFeature> items) throws IOException {
		if (depth > MAX_CHILD_DEPTH || !document.has("links"))
			return;
		for (JsonNode link : document.get("links")) {
			if (pm.isCanceled() || (maxItems > 0 && items.size() >= maxItems))
				return;
			String rel = link.path("rel").asText();
			if (!"item".equals(rel) && !"child".equals(rel))
				continue;
			URL url = resolve(documentUrl, link.path("href").asText());
			if (url == null || !visited.add(url.toString()))
				continue;
			JsonNode node = readJson(url);
			if ("Feature".equals(node.path("type").asText())) {
				SimpleFeature feature = toFeature(node, url);
				if (feature != null && filter.accepts(node, feature)) {
					items.add(feature);
					pm.worked(1);
				}
			} else {
				// nested catalogs of items
				collectStaticItems(node, url, filter, maxItems, pm, depth + 1, visited, items);
			}
		}
	}

	/**
	 * Convert an item to a feature, the same way the items of a search response are read.
	 */
	private SimpleFeature toFeature(JsonNode item, URL itemUrl) throws IOException {
		// asset hrefs are often relative to the item document
		JsonNode assets = item.get("assets");
		if (assets != null && assets.isObject()) {
			Iterator<String> names = assets.fieldNames();
			while (names.hasNext()) {
				JsonNode asset = assets.get(names.next());
				if (asset.isObject() && asset.hasNonNull("href")) {
					URL absolute = resolve(itemUrl, asset.get("href").asText());
					if (absolute != null)
						((ObjectNode) asset).put("href", absolute.toString());
				}
			}
		}
		ObjectNode featureCollection = OBJECT_MAPPER.createObjectNode();
		featureCollection.put("type", "FeatureCollection");
		featureCollection.putArray("features").add(item);
		byte[] bytes = OBJECT_MAPPER.writeValueAsBytes(featureCollection);
		try (HMSTACGeoJSONReader reader = new HMSTACGeoJSONReader(new ByteArrayInputStream(bytes), getHttp());
				SimpleFeatureIterator iterator = reader.getFeatures().features()) {
			return iterator.hasNext() ? iterator.next() : null;
		}
	}

	private JsonNode readJson(URL url) throws IOException {
		HTTPResponse response = getHttp().get(url);
		try (InputStream is = response.getResponseStream()) {
			return OBJECT_MAPPER.readTree(is);
		} finally {
			response.dispose();
		}
	}

	/**
	 * The filters of a search query, evaluated locally on the items.
	 */
	private static class StaticItemFilter {
		private final Envelope bbox;
		private final Geometry intersects;
		private final Instant from;
		private final Instant to;
		private final Filter filter;

		StaticItemFilter(SearchQuery search) {
			double[] b = search.getBbox();
			bbox = b != null && b.length >= 4 ? new Envelope(b[0], b[b.length / 2], b[1], b[b.length / 2 + 1]) : null;
			intersects = search.getIntersects();
			Instant f = null;
			Instant t = null;
			String datetime = search.getDatetime();
			if (datetime != null) {
				String[] split = datetime.split("/");
				f = parseInstant(split[0]);
				t = split.length > 1 ? parseInstant(split[1]) : f;
			}
			from = f;
			to = t;
			filter = search.getFilter();
		}

		boolean accepts(JsonNode item, SimpleFeature feature) {
			Geometry geometry = (Geometry) feature.getDefaultGeometry();
			if (bbox != null && (geometry == null || !bbox.intersects(geometry.getEnvelopeInternal())))
				return false;
			if (intersects != null && (geometry == null || !intersects.intersects(geometry)))
				return false;
			if (from != null || to != null) {
				JsonNode properties = item.path("properties");
				Instant start = parseInstant(properties.path("start_datetime").asText(null));
				Instant end = parseInstant(properties.path("end_datetime").asText(null));
				Instant datetime = parseInstant(properties.path("datetime").asText(null));
				if (start == null)
					start = datetime;
				if (end == null)
					end = datetime;
				if (start == null && end == null)
					return false;
				// intervals overlap, open ends are unbounded
				if (to != null && start != null && start.isAfter(to))
					return false;
				if (from != null && end != null && end.isBefore(from))
					return false;
			}
			return filter == null || filter.evaluate(feature);
		}

		private static Instant parseInstant(String text) {
			if (text == null || text.isBlank() || text.equals("..") || text.equals("null"))
				return null;
			try {
				return OffsetDateTime.parse(text).toInstant();
			} catch (DateTimeParseException e) {
				try {
					return Instant.parse(text);
				} catch (DateTimeParseException e2) {
					return null;
				}
			}
		}
	}

	public Collection getCollectionByURL(String collectionURL) throws IOException {

		HTTPClient http = getHttp();
		HTTPResponse response = http.get(new URL(collectionURL));
		checkJSONResponse(response);

		try (InputStream is = response.getResponseStream()) {
			return OBJECT_MAPPER.readValue(is, Collection.class);
		}
	}

	private boolean isDataJSONLink(Link l) {
		return "data".equals(l.getRel()) && (isBlank(l.getType()) || JSON_MIME.equals(l.getType()));
	}

	private boolean isChildrenJSONLink(Link l) {
		return "children".equals(l.getRel()) && (isBlank(l.getType()) || JSON_MIME.equals(l.getType()));
	}

	private boolean isChildJSONLink(Link l) {
		return "child".equals(l.getRel()) && (isBlank(l.getType()) || l.getType().startsWith(JSON_MIME));
	}

	/**
	 * Resolve a possibly relative href (common in static catalogs) against the document it comes from.
	 */
	private static URL resolve(URL base, String href) throws IOException {
		if (isBlank(href))
			return null;
		try {
			return new URL(base, href);
		} catch (MalformedURLException e) {
			throw new IOException("Invalid link in STAC document " + base + ": " + href, e);
		}
	}

	private static boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	/**
	 * Resolve next link against current page URL. 
	 * 
	 * Handles: 
	 * <ul>
	 * 	<li>null nextHref => end of paging 
	 *  <li>absolute nextHref 
	 *  <li>relative nextHref
	 * </ul>
	 */
	private URL resolveNext(URL currentPageUrl, String nextHref) throws IOException {
		if (nextHref == null || nextHref.isBlank())
			return null;

		try {
			// Absolute
			if (nextHref.startsWith("http://") || nextHref.startsWith("https://")) {
				return new URL(nextHref);
			}
			// Relative: resolve against current page
			return new URL(currentPageUrl, nextHref);
		} catch (MalformedURLException e) {
			throw new IOException("Invalid 'next' link in STAC collections response: " + nextHref, e);
		}
	}

	/**
	 * Same JSON check as base STACClient (needed because it's private there).
	 */
	private void checkJSONResponse(HTTPResponse response) {
		String mime = response.getContentType();
		if (mime == null || !mime.startsWith(JSON_MIME)) {
			throw new IllegalArgumentException("Was expecting a JSON response, got a different mime type: " + mime);
		}
	}
}
