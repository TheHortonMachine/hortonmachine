package org.hortonmachine.gears.io.stac;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.geotools.http.HTTPClient;
import org.geotools.http.commons.MultithreadedHttpClient;
import org.geotools.stac.client.Collection;
import org.geotools.stac.client.FeaturesConformance;
import org.geotools.stac.client.STACConformance;
import org.geotools.stac.client.STACLandingPage;
import org.hortonmachine.gears.io.stac.client.HMSTACClient;
import org.hortonmachine.gears.io.stac.client.HMStacAuthHttpClient;
import org.hortonmachine.gears.io.stac.auth.HMStacAccess;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;

/**
 * The entrypoint to work with stac in HM.
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 *
 */
@SuppressWarnings("rawtypes")
public class HMStacManager implements AutoCloseable {
    private String catalogUrl;
    private HMSTACClient stacClient;
    private IHMProgressMonitor pm;
    private HMStacAccess access;
    private boolean forceLatLon = false;
    private boolean reportInvalidAssets = true;

    public HMStacManager( String catalogUrl, IHMProgressMonitor pm ) {
        this.catalogUrl = catalogUrl;
        this.pm = pm;
    }

    /**
     * Set the access context, for example to use authentication on protected catalogs. 
     * Has to be called before {@link #open()}.
     * 
     * @param access the access context.
     * @return the current manager.
     */
    public HMStacManager setAccess( HMStacAccess access ) {
        this.access = access;
        return this;
    }

    /**
     * @return the access context or null.
     */
    public HMStacAccess getAccess() {
        return access;
    }

    /**
     * Some catalogs write the item geometries and bboxes in latitude/longitude order instead of
     * the longitude/latitude required by STAC. That is detected and fixed for those reaching
     * latitudes over +-90 (global layers), this forces the fix also where it can't be detected.
     *
     * @param forceLatLon if <code>true</code>, geometries and bboxes are always swapped.
     * @return the current manager.
     */
    public HMStacManager setForceLatLon( boolean forceLatLon ) {
        this.forceLatLon = forceLatLon;
        if (stacClient != null)
            stacClient.setForceLatLon(forceLatLon);
        return this;
    }

    /**
     * Whether the items of the collections of this manager report on the error stream the assets
     * without a handler (metadata files, previews, ...) when listing their assets. Default true.
     *
     * <p>Applies to the collections got from the manager after the call.</p>
     *
     * @param report false to not report them.
     * @return the current manager.
     */
    public HMStacManager setReportInvalidAssets( boolean report ) {
        this.reportInvalidAssets = report;
        return this;
    }

    private HMStacCollection newCollection( Collection collection ) {
        HMStacCollection hmCollection = new HMStacCollection(stacClient, collection, pm, access);
        hmCollection.setReportInvalidAssets(reportInvalidAssets);
        return hmCollection;
    }

    /**
     * Open the connection to the stac repository.
     * 
     * @throws Exception
     */
    public void open() throws Exception {
        HTTPClient http = new MultithreadedHttpClient();
        String url = catalogUrl;
        if (access == null && HMStacAccess.isS3Scheme(catalogUrl)) {
            // a static catalog on a public bucket
            access = new HMStacAccess();
        }
        if (access != null) {
            // e.g. s3://bucket/catalog.json to its https address
            url = access.toHttpUrl(catalogUrl);
            http = new HMStacAuthHttpClient(http, access);
        }
        try {
            stacClient = new HMSTACClient(new URL(url), http);
        } catch (Exception e) {
            stacClient = null;
            for( String fallbackUrl : getFallbackUrls(url) ) {
                try {
                    stacClient = new HMSTACClient(new URL(fallbackUrl), http);
                    break;
                } catch (Exception e2) {
                    e.addSuppressed(e2);
                }
            }
            if (stacClient == null) {
                throw e;
            }
        }
        stacClient.setForceLatLon(forceLatLon);
    }

    /**
     * The addresses to try when the given one is not a landing page.
     *
     * @param url the address given by the user.
     * @return the list of alternative addresses, in order of preference.
     */
    static List<String> getFallbackUrls( String url ) {
        List<String> urls = new ArrayList<>();
        String noSlash = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        // the OpenAPI definition (rel=service-desc, e.g. https://host/v1/api) is often
        // mistaken for the landing page, which is its parent
        if (noSlash.toLowerCase().endsWith("/api")) {
            urls.add(noSlash.substring(0, noSlash.length() - "/api".length()));
        }
        // static catalogs on object storage (e.g. https://host/bucket/catalog) have no
        // index document on the folder, the root is the catalog.json inside it
        if (!noSlash.toLowerCase().endsWith(".json")) {
            urls.add(noSlash + "/catalog.json");
        }
        return urls;
    }

    /**
     * @return <code>true</code> if the catalog supports item search. If not (e.g. static catalogs), 
     *          searches are done following the catalog links and filtering the items locally.
     * @throws Exception
     */
    public boolean isItemSearchAvailable() throws Exception {
        checkOpen();
        return stacClient.supportsItemSearch();
    }

    /**
     * @return <code>true</code> if the catalog supports item search with the filter extension, so that
     *          {@link HMStacCollection#setCqlFilter(String)} is evaluated by the server (e.g. on the item id).
     * @throws Exception
     */
    public boolean isFilterAvailable() throws Exception {
        checkOpen();
        List<String> conformance = stacClient.getLandingPage().getConformance();
        return conformance != null && stacClient.supportsItemSearch() && STACConformance.FILTER.matches(conformance);
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
            hmCollections.add(newCollection(c));
        }
        return hmCollections;
    }

    public HMStacCollection getCollectionById( String id ) throws Exception {
        checkOpen();
        List<Collection> collections = stacClient.getCollections();
        for( Collection c : collections ) {
            if (c.getId().equals(id)) {
                return newCollection(c);
            }
        }
        return null;
    }

    /**
     * Read again a collection, to get its current metadata, as an extent grown with new items.
     *
     * <p>Only the document of the collection is read, if its address is known, else the collections of the catalog are
     * listed again, which on large catalogs can take long.</p>
     *
     * @param collection the collection to read again.
     * @return the current collection, or <code>null</code> if not found anymore.
     * @throws Exception
     */
    public HMStacCollection reloadCollection( HMStacCollection collection ) throws Exception {
        checkOpen();
        Collection current = stacClient.reloadCollection(collection.getCollection());
        if (current == null)
            return getCollectionById(collection.getId());
        return newCollection(current);
    }

    public HMStacCollection getCollectionByURL(String collectionUrl) throws Exception {
        checkOpen();
        return newCollection(stacClient.getCollectionByURL(access != null ? access.toHttpUrl(collectionUrl) : collectionUrl));
    }

    public void close() throws Exception {
        if (stacClient != null)
            stacClient.close();
    }

}
