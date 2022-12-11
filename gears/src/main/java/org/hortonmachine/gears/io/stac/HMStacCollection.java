package org.hortonmachine.gears.io.stac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.stac.client.Collection;
import org.geotools.stac.client.CollectionExtent;
import org.geotools.stac.client.CollectionExtent.TemporalExtents;
import org.geotools.stac.client.STACClient;
import org.geotools.stac.client.SearchQuery;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.time.UtcTimeUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.feature.simple.SimpleFeature;

@SuppressWarnings({"rawtypes"})
/**
 * A stac collection.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class HMStacCollection {
    private STACClient stacClient;
    private Collection collection;
    private SearchQuery search;
    private IHMProgressMonitor pm;
    private int limit = -1;

    HMStacCollection( STACClient stacClient, Collection collection, IHMProgressMonitor pm ) {
        this.stacClient = stacClient;
        this.collection = collection;
        this.pm = pm;
    }

    public String getId() {
        return collection.getId();
    }

    public String getType() {
        return collection.getType();
    }

    public ReferencedEnvelope getSpatialBounds() {
        // CollectionExtent extent = c.getExtent();
        // The following spatial extent does not work well, leaving it for reference
        // SpatialExtents spatial = extent.getSpatial();
        // List<List<Double>> bbox = spatial.getBbox();
        // String crs = spatial.getCrs();

        // better to get the bounds from the collection directly
        return collection.getBounds();
    }

    /**
     * Set days filter for search query;
     * 
     * @param startDay
     * @param endDay
     * @return the current collection.
     */
    public HMStacCollection setDayFilter( String startDay, String endDay ) {
        if (search == null)
            search = new SearchQuery();
        if (endDay == null)
            endDay = startDay;
        search.setDatetime(startDay + "T00:00:00.000000Z/" + endDay + "T23:59:59.999999Z");
        return this;
    }

    /**
     * Set geometry intersection filter for search query;
     * 
     * @param intersectionGeometry
     * @return the current collection.
     */
    public HMStacCollection setGeometryFilter( Geometry intersectionGeometry ) {
        if (search == null)
            search = new SearchQuery();
        search.setIntersects(intersectionGeometry);
        return this;
    }

    /**
     * Set cql filter for search query;
     * 
     * @param cqlFilter
     * @return the current collection.
     * @throws CQLException
     */
    public HMStacCollection setCqlFilter( String cqlFilter ) throws CQLException {
        if (search == null)
            search = new SearchQuery();
        search.setFilter(CQL.toFilter(cqlFilter));
        return this;
    }

    /**
     * Set a limit for the search query.
     * 
     * @param limit
     * @return
     */
    public HMStacCollection setLimit( int limit ) {
        this.limit = limit;
        if (search == null)
            search = new SearchQuery();
        search.setLimit(limit);
        return this;
    }

    public List<HMStacItem> searchItems() throws Exception {
        if (search == null)
            search = new SearchQuery();
        search.setCollections(Arrays.asList(getId()));

        SimpleFeatureCollection fc = stacClient.search(search, STACClient.SearchMode.GET);
        SimpleFeatureIterator iterator = fc.features();
        pm.beginTask("Extracting unique features...", IHMProgressMonitor.UNKNOWN);
        List<HMStacItem> stacItems = new ArrayList<>();
        TreeSet<String> uniqueItems = new TreeSet<>();
        int count = 0;
        while( iterator.hasNext() ) {
            if (limit > 0 && count > limit) {
                break;
            }
            SimpleFeature f = iterator.next();
            HMStacItem item = new HMStacItem(f);
            if (item.getEpsg() != null && uniqueItems.add(item.getTimestamp() + " " + item.getId())) {
                stacItems.add(item);
            }
            pm.worked(1);
        }
        iterator.close();
        pm.done();
        return stacItems;
    }

    public static Geometry getCoveredArea( List<HMStacItem> items ) {
        List<Geometry> geometries = items.stream().map(item -> item.getGeometry()).collect(Collectors.toList());
        Geometry union = CascadedPolygonUnion.union(geometries);
        return union;
    }

    public List<Date> getTemporalBounds() {
        CollectionExtent extent = collection.getExtent();
        TemporalExtents temporal = extent.getTemporal();
        List<List<Date>> interval = temporal.getInterval();
        return interval.get(0); // TODO check how to make this better
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("id = ").append(getId()).append("\n");
        sb.append("type = ").append(getType()).append("\n");
        String boundsString = HMStacUtils.simplify(getSpatialBounds());
        sb.append("spatial extent: " + boundsString).append("\n");

        List<Date> temporalBounds = getTemporalBounds();
        int size = temporalBounds.size();
        if (size > 0) {
            String from = UtcTimeUtilities.quickToString(temporalBounds.get(0).getTime());
            String to = " - ";
            if (size > 1) {
                Date toDate = temporalBounds.get(size - 1);
                if (toDate != null) {
                    to = UtcTimeUtilities.quickToString(toDate.getTime());
                }
            }
            sb.append("temporal extent: from " + from + " to " + to);
        }
        return sb.toString();
    }

}