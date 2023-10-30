package org.hortonmachine.gears.io.stac;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.stac.client.Collection;
import org.geotools.stac.client.CollectionExtent;
import org.geotools.stac.client.CollectionExtent.TemporalExtents;
import org.geotools.stac.client.STACClient;
import org.geotools.stac.client.SearchQuery;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.time.UtcTimeUtilities;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

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

    HMStacCollection( STACClient stacClient, Collection collection, IHMProgressMonitor pm ) {
        this.stacClient = stacClient;
        this.collection = collection;
        if (pm == null)
            pm = new DummyProgressMonitor();
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

    public List<Date> getTemporalBounds() {
        CollectionExtent extent = collection.getExtent();
        TemporalExtents temporal = extent.getTemporal();
        List<List<Date>> interval = temporal.getInterval();
        return interval.get(0); // TODO check how to make this better
    }

    /**
     * Set temporal filter for search query;
     * 
     * @param startTimestamp
     * @param endTimestamp
     * @return the current collection.
     */
    public HMStacCollection setTimestampFilter( Date startTimestamp, Date endTimestamp ) {
        if (search == null)
            search = new SearchQuery();
        search.setDatetime(HMStacUtils.filterTimestampFormatter.format(startTimestamp) + "/"
                + HMStacUtils.filterTimestampFormatter.format(endTimestamp));
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

    public List<HMStacItem> searchItems() throws Exception {
        if (search == null)
            search = new SearchQuery();
        search.setCollections(Arrays.asList(getId()));

        SimpleFeatureCollection fc = stacClient.search(search, STACClient.SearchMode.GET);
        SimpleFeatureIterator iterator = fc.features();
        pm.beginTask("Extracting STAC items...", -1);
        List<HMStacItem> stacItems = new ArrayList<>();
        try {
            while( iterator.hasNext() ) {
                SimpleFeature f = iterator.next();
                HMStacItem item = HMStacItem.fromSimpleFeature(f);
                if (item != null && item.getId() != null && item.getEpsg() != null) {
                    stacItems.add(item);
                } else if (item.getId() == null) {
                    pm.errorMessage("Unable to get id of item: " + item.toString());
                }
                pm.worked(1);
            }
        } finally {
            iterator.close();
        }
        pm.message("Done.");
        return stacItems;
    }

    /**
     * Read all the raster of a certain band from the items list and merge them to a single raster sized on the given region and resolution.
     * 
     * @param latLongRegionMap the region to use for the final raster.
     * @param bandName the name o the band to extract.
     * @param items the list of items containing the various assets to read from.
     * @param allowTransform if true, allows datasets of different projections to be transformed and merged together.
     * @return the final raster.
     * @throws Exception
     */
    public static HMRaster readRasterBandOnRegion( RegionMap latLongRegionMap, String bandName, List<HMStacItem> items,
            boolean allowTransform, IHMProgressMonitor pm ) throws Exception {

        if (!allowTransform) {
            List<String> epsgs = items.stream().map(( i ) -> i.getEpsg().toString()).distinct().collect(Collectors.toList());
            if (epsgs.size() > 1) {
                throw new IllegalArgumentException(
                        "Multiple epsg detected when no transform allowed: " + epsgs.stream().collect(Collectors.joining(",")));
            }
        }

        // use the first srid as the output srid.
        Integer firstItemSrid = items.get(0).getEpsg();
        CoordinateReferenceSystem outputCrs = CrsUtilities.getCrsFromSrid(firstItemSrid);
        ReferencedEnvelope roiEnvelopeFirstItemCrs = new ReferencedEnvelope(latLongRegionMap.toEnvelope(),
                DefaultGeographicCRS.WGS84).transform(outputCrs, true);
        Polygon roiGeometryFirstItemCrs = GeometryUtilities.createPolygonFromEnvelope(roiEnvelopeFirstItemCrs);

        CoordinateReferenceSystem firstItemCRS = CRS.decode("EPSG:" + firstItemSrid);

        int cols = latLongRegionMap.getCols();
        int rows = latLongRegionMap.getRows();

        HMRaster outRaster = null;

        String fileName = null;
        pm.beginTask("Reading " + bandName + "...", items.size());
        for( HMStacItem item : items ) {
            int currentSrid = item.getEpsg();
            CoordinateReferenceSystem currentItemCRS = CRS.decode("EPSG:" + currentSrid);
            Geometry geometry = item.getGeometry();

            if (firstItemSrid != currentSrid) {
                MathTransform transform = CRS.findMathTransform(currentItemCRS, firstItemCRS);
                geometry = JTS.transform(geometry, transform);
            }
            Geometry intersectionFirstItemCrs = geometry.intersection(roiGeometryFirstItemCrs);
            Envelope currentItemReadEnvelopeFIrstItemCrs = intersectionFirstItemCrs.getEnvelopeInternal();

            ReferencedEnvelope roiEnvCurrentItemCrs = new ReferencedEnvelope(currentItemReadEnvelopeFIrstItemCrs, firstItemCRS)
                    .transform(currentItemCRS, true);

            RegionMap readRegion = RegionMap.fromBoundsAndGrid(roiEnvCurrentItemCrs.getMinX(), roiEnvCurrentItemCrs.getMaxX(),
                    roiEnvCurrentItemCrs.getMinY(), roiEnvCurrentItemCrs.getMaxY(), cols, rows);

            HMStacAsset asset = item.getAssets().stream().filter(as -> as.getId().equals(bandName)).findFirst().get();
            int lastSlash = asset.getAssetUrl().lastIndexOf('/');
            fileName = asset.getAssetUrl().substring(lastSlash + 1);
            if (outRaster == null) {
                outRaster = new HMRasterWritableBuilder().setName(fileName)
                        .setRegion(RegionMap.fromEnvelopeAndGrid(roiEnvelopeFirstItemCrs, cols, rows)).setCrs(outputCrs)
                        .setNoValue(asset.getNoValue()).build();
            }

            GridCoverage2D readRaster = asset.readRaster(readRegion);
            outRaster.mapRasterSum(null, HMRaster.fromGridCoverage(readRaster));
            pm.worked(1);
        }
        pm.done();

        return outRaster;
    }

    public static Geometry getCoveredArea( List<HMStacItem> items ) {
        List<Geometry> geometries = items.stream().map(item -> item.getGeometry()).collect(Collectors.toList());
        Geometry union = CascadedPolygonUnion.union(geometries);
        return union;
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