package org.hortonmachine.gears.io.stac;

import java.net.URL;
import java.util.Date;
import java.util.List;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.http.commons.MultithreadedHttpClient;
import org.geotools.stac.client.Collection;
import org.geotools.stac.client.CollectionExtent;
import org.geotools.stac.client.CollectionExtent.TemporalExtents;
import org.geotools.stac.client.FeaturesConformance;
import org.geotools.stac.client.STACClient;
import org.geotools.stac.client.STACConformance;
import org.geotools.stac.client.STACLandingPage;
import org.hortonmachine.gears.utils.time.UtcTimeUtilities;
import org.locationtech.jts.geom.Envelope;

public class StacInfoTest extends TestVariables {

    @SuppressWarnings({"rawtypes"})
    public StacInfoTest() throws Exception {

        STACClient stacClient = new STACClient(new URL(repoUrl), new MultithreadedHttpClient());
        try {

            STACLandingPage landingPage = stacClient.getLandingPage();
            List<String> conformance = landingPage.getConformance();

            System.out.println("Supported conformance:");
            System.out.println("======================");
            System.out.println("Collections: " + STACConformance.COLLECTIONS.matches(conformance));
            System.out.println("Features: " + STACConformance.FEATURES.matches(conformance));
            System.out.println("Item search: " + STACConformance.ITEM_SEARCH.matches(conformance));
            System.out.println("Fields: " + STACConformance.FIELDS.matches(conformance));
            System.out.println("Query: " + STACConformance.QUERY.matches(conformance));
            System.out.println("Sort: " + STACConformance.SORT.matches(conformance));
            System.out.println("Filter: " + STACConformance.FILTER.matches(conformance));
            System.out.println("Features: " + FeaturesConformance.CORE.matches(conformance));
            System.out.println("Features Geojson: " + FeaturesConformance.GEOJSON.matches(conformance));
            System.out.println("----------------------");
            System.out.println();
            System.out.println("Browse Collections:");
            System.out.println("======================");

            List<Collection> collections = stacClient.getCollections();
            for( Collection c : collections ) {
                System.out.println(c.getId());
                System.out.println("----------------------------------------------");
                System.out.println("\tType: " + c.getType());

                CollectionExtent extent = c.getExtent();

                // The following spatial extent does not work well, leaving it for reference
                // SpatialExtents spatial = extent.getSpatial();
                // List<List<Double>> bbox = spatial.getBbox();
                // String crs = spatial.getCrs();

                // better to get the bounds from the collection directly
                ReferencedEnvelope bounds = c.getBounds();
                String boundsString = simplify(bounds);
                System.out.println("\tSpatial Extent: " + boundsString);

                TemporalExtents temporal = extent.getTemporal();
                List<List<Date>> interval = temporal.getInterval();
                System.out.println("\tTemporal Extent:");
                for( List<Date> datesList : interval ) {
                    for( Date date : datesList ) {
                        String ts = null;
                        if (date != null) {
                            ts = UtcTimeUtilities.quickToString(date.getTime());
                        }
                        System.out.println("\t\t" + ts);
                    }
                }
                System.out.println();
            }
        } finally {
            stacClient.close();
        }

    }

    private String simplify( Object obj ) {
        if (obj instanceof ReferencedEnvelope) {
            ReferencedEnvelope refEnv = (ReferencedEnvelope) obj;
            Envelope env = new Envelope(refEnv);
            return env.toString() + " - " + refEnv.getCoordinateReferenceSystem().getName();
        }
        return null;
    }

    public static void main( String[] args ) throws Exception {
        new StacInfoTest();
    }

}
