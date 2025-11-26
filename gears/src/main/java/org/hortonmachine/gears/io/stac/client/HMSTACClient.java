package org.hortonmachine.gears.io.stac.client;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.http.HTTPClient;
import org.geotools.http.HTTPResponse;
import org.geotools.stac.client.HttpMethod;
import org.geotools.stac.client.STACClient;
import org.geotools.stac.client.STACConformance;
import org.geotools.stac.client.SearchQuery;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class HMSTACClient extends STACClient {
	
    static ObjectMapper OBJECT_MAPPER;

    /** Initialize an ObjectMapper that's tolerant, won't generate missing fields, and can parse GeoJSON geometries */
    static {
        OBJECT_MAPPER = new ObjectMapper();
        OBJECT_MAPPER.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
        OBJECT_MAPPER.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
        OBJECT_MAPPER.registerModule(new JtsModule());
        OBJECT_MAPPER.setDefaultPropertyInclusion(JsonInclude.Include.NON_EMPTY);
    }

    public HMSTACClient(URL landingPageURL, HTTPClient http) throws IOException {
        super(landingPageURL, http);
    }

    /**
     * More lenient check: accept both application/geo+json and application/json
     */
    private void checkGeoOrJsonResponse(HTTPResponse response) {
        String mime = response.getContentType();
        if (mime == null) {
            throw new IllegalArgumentException(
                    "Was expecting a GeoJSON response, got a null mime type");
        }
        if (!mime.startsWith(GEOJSON_MIME) && !mime.startsWith(JSON_MIME)) {
            throw new IllegalArgumentException(
                    "Was expecting a GeoJSON (or JSON) response, got a different mime type: "
                            + mime);
        }
    }

    @Override
    public SimpleFeatureCollection search(
            SearchQuery search,
            SearchMode mode,
            SimpleFeatureType schema) throws IOException {

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
                response = http.post(
                        postURL,
                        new ByteArrayInputStream(body.getBytes(StandardCharsets.UTF_8)),
                        "application/json");
            }

            // <<< this is the only semantic change: lenient mime check
            checkGeoOrJsonResponse(response);

            try (HMSTACGeoJSONReader reader = new HMSTACGeoJSONReader(
                    new BufferedInputStream(response.getResponseStream(), 1024 * 32),
                    http)) {
                if (schema != null) {
                    reader.setSchema(schema);
                }
                return reader.getFeatures();
            }
        } catch (URISyntaxException e) {
            throw new IOException("Failed to build the search query URL", e);
        }
    }
}
