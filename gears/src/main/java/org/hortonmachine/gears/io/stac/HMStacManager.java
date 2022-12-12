package org.hortonmachine.gears.io.stac;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.geotools.http.commons.MultithreadedHttpClient;
import org.geotools.stac.client.Collection;
import org.geotools.stac.client.FeaturesConformance;
import org.geotools.stac.client.STACClient;
import org.geotools.stac.client.STACConformance;
import org.geotools.stac.client.STACLandingPage;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * The entrypoint to work with stac in HM.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
@SuppressWarnings("rawtypes")
public class HMStacManager implements AutoCloseable {
    private String catalogUrl;
    private STACClient stacClient;
    private IHMProgressMonitor pm;

    public HMStacManager( String catalogUrl, IHMProgressMonitor pm ) {
        this.catalogUrl = catalogUrl;
        this.pm = pm;
    }

    /**
     * Open the connection to the stac repository.
     * 
     * @throws Exception
     */
    public void open() throws Exception {
        stacClient = new STACClient(new URL(catalogUrl), new MultithreadedHttpClient());
    }

    public String getConformanceSummary() throws Exception {
        checkOpen();
        STACLandingPage landingPage = stacClient.getLandingPage();
        List<String> conformance = landingPage.getConformance();
        if (conformance != null) {
            StringBuilder sb = new StringBuilder();
            sb.append("Collections: " + STACConformance.COLLECTIONS.matches(conformance)).append("\n");
            sb.append("Features: " + STACConformance.FEATURES.matches(conformance)).append("\n");
            sb.append("Item search: " + STACConformance.ITEM_SEARCH.matches(conformance)).append("\n");
            sb.append("Fields: " + STACConformance.FIELDS.matches(conformance)).append("\n");
            sb.append("Query: " + STACConformance.QUERY.matches(conformance)).append("\n");
            sb.append("Sort: " + STACConformance.SORT.matches(conformance)).append("\n");
            sb.append("Filter: " + STACConformance.FILTER.matches(conformance)).append("\n");
            sb.append("Features: " + FeaturesConformance.CORE.matches(conformance)).append("\n");
            sb.append("Features Geojson: " + FeaturesConformance.GEOJSON.matches(conformance));
            return sb.toString();
        } else {
            return "No confromance information available.";
        }
    }

    private void checkOpen() throws Exception {
        if (stacClient == null) {
            throw new IOException("Stac client not available, did you call open?");
        }

    }

    public List<HMStacCollection> getCollections() throws Exception {
        checkOpen();
        List<HMStacCollection> hmCollections = new ArrayList<>();
        List<Collection> collections = stacClient.getCollections();
        for( Collection c : collections ) {
            HMStacCollection hmCollection = new HMStacCollection(stacClient, c, pm);
            hmCollections.add(hmCollection);
        }
        return hmCollections;
    }

    public HMStacCollection getCollectionById( String id ) throws Exception {
        checkOpen();
        List<Collection> collections = stacClient.getCollections();
        for( Collection c : collections ) {
            if (c.getId().equals(id)) {
                HMStacCollection hmCollection = new HMStacCollection(stacClient, c, pm);
                return hmCollection;
            }
        }
        return null;
    }

    public void close() throws Exception {
        if (stacClient != null)
            stacClient.close();
    }

}
