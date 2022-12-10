package org.hortonmachine.gears.io.stac;

import java.net.URL;
import java.util.Arrays;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.http.commons.MultithreadedHttpClient;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.stac.client.STACClient;
import org.geotools.stac.client.SearchQuery;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class StacAssetFootprint extends TestVariables {
    @SuppressWarnings({"unchecked"})
    public StacAssetFootprint() throws Exception {

        try (STACClient stacClient = new STACClient(new URL(repoUrl), new MultithreadedHttpClient())) {
            // i.e. find sentinel data for the day 2022-12-07 in Italy
            SearchQuery search = new SearchQuery();
            search.setCollections(Arrays.asList(collectionQuery));
            search.setDatetime(dateQuery);
            search.setIntersects(intersectionGeometry);// GeometryUtilities.createPolygonFromEnvelope(italyEnv));
            search.setFilter(CQL.toFilter(CQL_FILTER));
            search.setLimit(limit); // limit doesn't seem to work properly

            System.out.println("Search collection with query:");
            System.out.println(search.toString());
            System.out.println("==============================================================================");

            SimpleFeatureCollection fc = stacClient.search(search, STACClient.SearchMode.GET);

            // TODO the following doesn't work, the size is not retrieved properly
            // int size = fc.size();
            // System.out.println("Found " + size + " features matching the query.");
            // System.out.println();

            StacFeatures stacfeatures = getUniqueFeatures(fc);
            int size = stacfeatures.getSize();
            Geometry coveredAreas = stacfeatures.getCoveredAreas();
            Geometry commonArea = coveredAreas.intersection(intersectionGeometry);
            double coveredArea = commonArea.getArea();
            double roiArea = intersectionGeometry.getArea();
            int percentage = (int) Math.round(coveredArea * 100 / roiArea);
            System.out.println("Found " + size + " unique features matching the query.");
            System.out.println("Region of interest is covered by data in amout of " + percentage + "%");
            System.out.println();

            if (size > 0) {
                SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
                b.setName("typename");
                b.setCRS(DefaultGeographicCRS.WGS84);
                b.add("the_geom", Polygon.class);
                b.add("stacid", String.class);
                b.add("timestamp", String.class);
                SimpleFeatureType type = b.buildFeatureType();
                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
                DefaultFeatureCollection outFC = new DefaultFeatureCollection();

                int featureCount = 0;
                System.out.println("Processing features...");
                for( int i = 0; i < size; i++ ) {
                    if (featureCount++ % 10 == 0) {
                        System.out.println(featureCount + "/" + size);
                    }

                    Geometry geometry = stacfeatures.geometries.get(i);
                    String id = stacfeatures.ids.get(i);
                    String ts = stacfeatures.timestamp.get(i);

                    Object[] values = new Object[]{geometry, id, ts};
                    builder.addAll(values);
                    SimpleFeature feature = builder.buildFeature(null);
                    outFC.add(feature);
                }
                System.out.println("Done");

                Object[] values = new Object[]{intersectionGeometry, "ROI", ""};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                outFC.add(feature);

                OmsVectorWriter.writeVector(overviewShp, outFC);
            }
        }

    }

    public static void main( String[] args ) throws Exception {
        new StacAssetFootprint();
    }

}
