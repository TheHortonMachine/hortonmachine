package org.hortonmachine.gears.io.stac;

import java.net.URL;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.http.commons.MultithreadedHttpClient;
import org.geotools.stac.client.STACClient;
import org.geotools.stac.client.SearchQuery;
import org.hortonmachine.gears.utils.time.UtcTimeUtilities;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class StacAssetInfoTest extends TestVariables {

    @SuppressWarnings({"unchecked"})
    public StacAssetInfoTest() throws Exception {

        STACClient stacClient = new STACClient(new URL(repoUrl), new MultithreadedHttpClient());
        try {
            // i.e. find sentinel data for the day 2022-12-07 in Italy
            SearchQuery search = new SearchQuery();
            search.setCollections(Arrays.asList(collectionQuery));
            search.setDatetime(dateQuery);
            search.setIntersects(intersectionGeometry);
            search.setFilter(CQL.toFilter(CQL_FILTER));
            search.setLimit(limit); // limit doesn't seem to work properly

            System.out.println("Search collection with query:");
            System.out.println(search.toString());
            System.out.println("==============================================================================");

            SimpleFeatureCollection fc = stacClient.search(search, STACClient.SearchMode.GET);
            int size = fc.size();
            System.out.println("Found " + size + " features matching the query.");
            System.out.println();

            if (size > 0) {
                SimpleFeatureIterator iterator = fc.features();
                int featureCount = 0;
                while( iterator.hasNext() ) {
                    if (featureCount == limit) {
                        break;
                    }
                    SimpleFeature f = iterator.next();
                    Map<String, JsonNode> top = (Map<String, JsonNode>) f.getUserData().get(GeoJSONReader.TOP_LEVEL_ATTRIBUTES);

                    String id = top.get("id").textValue();
                    System.out.println(++featureCount + ") id=" + id);
                    System.out.println("------------------------------------");

                    Geometry geometry = (Geometry) f.getDefaultGeometry();
                    System.out.println("\tgeom = " + geometry);

                    String createdDateCet = f.getAttribute("created").toString();
                    Date dateCet = dateFormatter.parse(createdDateCet);
                    System.out.println("\tcreated = " + UtcTimeUtilities.quickToString(dateCet.getTime()));

                    String metadataSummary = getMetadataSummary(f, "\t");

                    System.out.println(metadataSummary);

                    ObjectNode assets = (ObjectNode) top.get("assets");

                    Iterator<JsonNode> assetsIterator = assets.elements();
                    System.out.println("\tList of Assets: ");
                    int count = 1;
                    while( assetsIterator.hasNext() ) {
                        JsonNode assetNode = assetsIterator.next();
                        JsonNode typeNode = assetNode.get("type");
                        if (typeNode != null) {
                            String type = typeNode.textValue();
                            // we only check cloud optimized datasets here
                            JsonNode titleNode = assetNode.get("title");
                            if (titleNode == null) {
                                continue;
                            }
                            System.out.println(
                                    "\t" + featureCount + "." + count++ + ") '" + titleNode.textValue() + "' --> (" + type + ")");
                            if (!shortInfo) {
                                if (type.toLowerCase().contains("profile=cloud-optimized")) {
                                    JsonNode rasterBandNode = assetNode.get("raster:bands");
                                    if (rasterBandNode != null && !rasterBandNode.isEmpty()) {
                                        System.out.println("\t\ttype: " + type);
                                        System.out.println("\t\tURL: " + assetNode.get("href").textValue());
                                        System.out.println("\t\tShape: " + assetNode.get("proj:shape"));
                                        System.out.println("\t\tTransform: " + assetNode.get("proj:transform"));
                                        System.out.println("\t\tBand info:");
                                        JsonNode eoBandNode = assetNode.get("eo:bands");
                                        if (eoBandNode != null && !eoBandNode.isEmpty()) {
                                            eoBandNode.forEach(eoNode -> {
                                                System.out.println("\t\t\tEO name/description: " + eoNode.get("name") + "/"
                                                        + eoNode.get("description"));
                                            });
                                        }
                                        rasterBandNode.forEach(rasterNode -> {
                                            System.out.println("\t\t\tdata_type: " + rasterNode.get("data_type"));
                                            System.out.println("\t\t\tnovalue: " + rasterNode.get("nodata"));
                                            System.out.println("\t\t\tresolution: " + rasterNode.get("spatial_resolution"));
                                            System.out.println("\t\t\tscale: " + rasterNode.get("scale"));
                                            System.out.println("\t\t\toffset: " + rasterNode.get("offset"));
                                        });
                                    } else {
                                        System.out.println("\t--> DOES NOT HAVE RASTER BANDS, IGNORING");
                                    }

                                } else {
                                    System.out.println("\t--> NOT A COG, IGNORING");
                                }
                            }
                        }
                    }
//                    System.out.println(assets);
                }
            }
        } finally {
            stacClient.close();
        }

    }

    private String getMetadataSummary( SimpleFeature f, String indent ) {

        StringBuilder sb = new StringBuilder();

        sb.append(indent + getAttribute(f, "updated"));
        sb.append(indent + getAttribute(f, "datetime"));
        sb.append(indent + getAttribute(f, "platform"));
        sb.append(indent + getAttribute(f, "constellation"));
        sb.append(indent + getAttribute(f, "eo:cloud_cover"));
        sb.append(indent + getAttribute(f, "proj:epsg"));
        sb.append(indent + getAttribute(f, "view:sun_azimuth"));
        sb.append(indent + getAttribute(f, "view:sun_elevation"));
        sb.append(indent + getAttribute(f, "s2:nodata_pixel_percentage"));
        sb.append(indent + getAttribute(f, "s2:cloud_shadow_percentage"));
        sb.append(indent + getAttribute(f, "s2:water_percentage"));

        return sb.toString();
    }

    private String getAttribute( SimpleFeature f, String name ) {
        Object attribute = f.getAttribute(name);
        if (attribute != null) {
            return name + " = " + attribute.toString() + "\n";
        }
        return "";
    }

    public static void main( String[] args ) throws Exception {
        new StacAssetInfoTest();
    }

}
