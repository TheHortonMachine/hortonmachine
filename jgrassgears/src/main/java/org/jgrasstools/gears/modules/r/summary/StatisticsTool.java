package org.jgrasstools.gears.modules.r.summary;
import static java.lang.Math.round;
import jaitools.media.jai.zonalstats.ZonalStats;
import jaitools.media.jai.zonalstats.ZonalStatsDescriptor;
import jaitools.media.jai.zonalstats.ZonalStatsRIF;
import jaitools.numeric.Statistic;

import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.image.renderable.RenderedImageFactory;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.ROI;
import javax.media.jai.ROIShape;
import javax.media.jai.registry.RIFRegistry;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.AbstractProcessor;
import org.geotools.coverage.processing.OperationJAI;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.DirectPosition2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.ProjectiveTransform;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.BoundingBox;
import org.opengis.geometry.coordinate.Position;
import org.opengis.metadata.spatial.PixelOrientation;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequence;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


/**
 * The statistic tool. 
 * 
 * Example usage:
 * <pre>
 *       GridCoverage2D gridCoverage2D = <your coverage>; 
 *       FeatureCollection<SimpleFeatureType, SimpleFeature> polygonsCollection = <your collection>;
 *       
 *       // choose the stats 
 *       Set<Statistic> statsSet = new LinkedHashSet<Statistic>();
 *       statsSet.add(Statistic.MIN);
 *       statsSet.add(Statistic.MAX);
 *       statsSet.add(Statistic.MEAN);
 *       statsSet.add(Statistic.MEDIAN);
 *       statsSet.add(Statistic.VARIANCE);
 *       statsSet.add(Statistic.SDEV);
 *       statsSet.add(Statistic.RANGE);
 *       statsSet.add(Statistic.APPROX_MEDIAN);
 *       statsSet.add(Statistic.SUM);
 *       statsSet.add(Statistic.ACTIVECELLS);
 *       
 *       // select the bands to work on
 *       Integer[] bands = new Integer[]{0, 1, 2};
 *       
 *       // create the proper instance
 *       StatisticsTool statisticsTool = null;
 *       if (isPolygonMode) {
 *           statisticsTool = StatisticsTool.getInstance(statsSet, coverage2D, bands,
 *                   featureCollection);
 *       } else if (isPointMode) {
 *           statisticsTool = StatisticsTool.getInstance(statsSet, coverage2D, bands,
 *                   featureCollection, buffer);
 *       }
 *       
 *       // do analysis
 *       statisticsTool.run();
 *       
 *       // get the results
 *       featureIterator = featureCollection.features();
 *       while( featureIterator.hasNext() ) {
 *           SimpleFeature feature = featureIterator.next();
 *           String id = feature.getID();
 *           Map<Statistic, Double[]> statistics = statisticsTool.getStatistics(id);
 *
 *           System.out.println();
 *           System.out.println("Stats for feature of fid: " + id);
 *           for( Statistic statistic : statsSet ) {
 *               System.out.println(statistic.toString() + ":" + statistics.get(statistic)[0]);
 *           }
 *       }
 *       featureCollection.close(featureIterator);
 * </pre>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings({"unchecked", "nls"})
public class StatisticsTool {
    // currently needed to make it work in uDig
    static {
        OperationRegistry or = JAI.getDefaultInstance().getOperationRegistry();
        or.registerDescriptor(new ZonalStatsDescriptor());
        RenderedImageFactory rif = new ZonalStatsRIF();
        RIFRegistry.register(or, "ZonalStats", "ZonalStatsOperation", rif);
    }

    private enum Type {
        POLYGON, BUFFER
    };

    /*
     * external user params
     */
    private Set<Statistic> statisticsSet;
    private Integer[] bands;
    private Double[] buffers;
    private GridCoverage2D gridCoverage2D;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;

    /*
     * results
     */
    private Map<String, Map<Statistic, Double[]>> feature2StatisticsMap = new HashMap<String, Map<Statistic, Double[]>>();

    private Type analysisType = null;

    private StatisticsTool( Set<Statistic> statisticsSet, GridCoverage2D gridCoverage2D,
            Integer[] bands, FeatureCollection<SimpleFeatureType, SimpleFeature> polygonsCollection ) {
        this.statisticsSet = statisticsSet;
        this.gridCoverage2D = gridCoverage2D;
        this.bands = bands;
        this.featureCollection = polygonsCollection;
        analysisType = Type.POLYGON;
    }

    private StatisticsTool( Set<Statistic> statisticsSet, GridCoverage2D gridCoverage2D,
            Integer[] bands, FeatureCollection<SimpleFeatureType, SimpleFeature> pointsCollection,
            Double... buffers ) {
        this.statisticsSet = statisticsSet;
        this.gridCoverage2D = gridCoverage2D;
        this.bands = bands;
        this.featureCollection = pointsCollection;
        this.buffers = buffers;
        analysisType = Type.BUFFER;
    }

    /**
     * Creates an instance of the {@link StatisticsTool}. 
     * 
     * <p>
     * In this case the statistics are calculated on the supplied
     * {@link GridCoverage2D coverage} in the areas of interest
     * defined by the polygonal features contained in the supplied 
     * {@link FeatureCollection feature collection}.
     * 
     * @param statisticsSet the {@link Set set} of requested {@link Statistic}s.
     *          If set to null, it will do all available ones.
     * @param gridCoverage2D the coverage on which to perform the analysis.
     * @param bands the array of bands to consider in the analysis. If null, 0 is used.
     * @param polygonsCollection the {@link FeatureCollection collection} of
     *          {@link Polygon}s or {@link MultiPolygon}s to use as region
     *          of interest. 
     * @return the instance of {@link StatisticsTool} on which 
     *          {@link #getStatisticProperty(String, Statistic)}
     *          can be called to gather the results.
     */
    public static StatisticsTool getInstance( Set<Statistic> statisticsSet,
            GridCoverage2D gridCoverage2D, Integer[] bands,
            FeatureCollection<SimpleFeatureType, SimpleFeature> polygonsCollection ) {
        return new StatisticsTool(statisticsSet, gridCoverage2D, bands, polygonsCollection);
    }

    /**
     * Creates an instance of the {@link StatisticsTool}. 
     * 
     * <p>
     * In this case the statistics are calculated on the supplied
     * {@link GridCoverage2D coverage} in the areas of interest
     * defined by the circular buffer (given by the buffers parameter)
     * applied to the point features contained in the supplied 
     * {@link FeatureCollection feature collection}.
     * <p>
     * There are two ways to supply buffers: 
     * <ul>
     *  <li> 
     *      buffers values are of the same number and same ordering of 
     *      the feature collection, in which case each buffer is applied
     *      to each point. 
     *  </li>
     *  <li> 
     *      a single buffer value is passed, in which case that value is 
     *      applied to each point. 
     *  </li>
     * </ul>
     * 
     * 
     * @param statisticsSet the {@link Set set} of requested {@link Statistic}s.
     *          If set to null, it will do all available ones.
     * @param gridCoverage2D the coverage on which to perform the analysis.
     * @param bands the array of bands to consider in the analysis. If null, 0 is used.
     * @param pointsCollection the {@link FeatureCollection collection} of
     *          {@link Point}s or {@link MultiPoint}s to use as center point
     *          for the region of interest, which is created applying the
     *          buffer to the point. 
     * @param buffers the buffer value to be taken around the points to create the 
     *          region of analysis in <b>meters</b>. The buffer parameter can be:
     *          <ul>
     *            <li>
     *              a single value, in which case it is taken constant for all the
     *              points.
     *            </li>
     *            <li>
     *              an value for every point geometry, i.e. an array of the same size
     *              as the passed features.
     *            </li>
     *          </ul>
     * @return the instance of {@link StatisticsTool} on which 
     *          {@link #getStatisticProperty(String, Statistic)}
     *          can be called to gather the results.
     */
    public static StatisticsTool getInstance( Set<Statistic> statisticsSet,
            GridCoverage2D gridCoverage2D, Integer[] bands,
            FeatureCollection<SimpleFeatureType, SimpleFeature> pointsCollection, Double... buffers ) {
        return new StatisticsTool(statisticsSet, gridCoverage2D, bands, pointsCollection, buffers);
    }

    /**
     * Run the requested analysis.
     * 
     * <p>
     * This is the moment in which the analysis takes place. This method
     * is intended to give the user the possibility to choose the moment
     * in which the workload is done.  
     * @throws Exception 
     */
    public void run() throws Exception {
        switch( analysisType ) {
        case POLYGON:
            processPolygonMode();
            break;
        case BUFFER:
            processPointAndBufferMode();
            break;
        default:
            break;
        }
    }

    private void processPolygonMode() throws TransformException {
        final AffineTransform gridToWorldTransformCorrected = new AffineTransform(
                (AffineTransform) ((GridGeometry2D) gridCoverage2D.getGridGeometry())
                        .getGridToCRS2D(PixelOrientation.UPPER_LEFT));
        final MathTransform worldToGridTransform;
        try {
            worldToGridTransform = ProjectiveTransform.create(gridToWorldTransformCorrected
                    .createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new ModelsIllegalargumentException(e.getLocalizedMessage(), this.getClass().getSimpleName());
        }

        FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            String fid = feature.getID();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
                BoundingBox bbox = feature.getBounds();
                ReferencedEnvelope rEnvelope = new ReferencedEnvelope(bbox);

                /*
                 * crop on region of interest
                 */
                final AbstractProcessor processor = AbstractProcessor.getInstance();
                ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
                param.parameter("Source").setValue(gridCoverage2D);
                param.parameter("Envelope").setValue(rEnvelope);
                GridCoverage2D cropped = (GridCoverage2D) processor.doOperation(param);

                ROI roi = null;
                int numGeometries = geometry.getNumGeometries();
                for( int i = 0; i < numGeometries; i++ ) {
                    Geometry geometryN = geometry.getGeometryN(i);
                    java.awt.Polygon awtPolygon = toAWTPolygon((Polygon) geometryN,
                            worldToGridTransform);
                    if (roi == null) {
                        roi = new ROIShape(awtPolygon);
                    } else {
                        ROI newRoi = new ROIShape(awtPolygon);
                        roi.add(newRoi);
                    }
                }

                Statistic[] statistis = statisticsSet.toArray(new Statistic[statisticsSet.size()]);

                final OperationJAI op = new OperationJAI("ZonalStats");
                ParameterValueGroup params = op.getParameters();
                params.parameter("dataImage").setValue(cropped);
                // params.parameter("zoneImage").setValue(constantImage);
                params.parameter("stats").setValue(statistis);
                params.parameter("bands").setValue(bands);
                params.parameter("roi").setValue(roi);

                GridCoverage2D coverage = (GridCoverage2D) op.doOperation(params, null);
                Map<Integer, ZonalStats> statsPerBand = (Map<Integer, ZonalStats>) coverage
                        .getProperty(ZonalStatsDescriptor.ZONAL_STATS_PROPERTY);
                int numBands = coverage.getNumSampleDimensions();

                Map<Statistic, Double[]> statsMap = new HashMap<Statistic, Double[]>();
                for( Statistic statistic : statistis ) {
                    Double[] statsResults = new Double[statistis.length];
                    for( int i = 0; i < numBands; i++ ) {
                        ZonalStats zonalStats = statsPerBand.get(i);
                        Map<Statistic, Double> zoneStats = zonalStats.getZoneStats(0);
                        statsResults[i] = zoneStats.get(statistic).doubleValue();
                    }
                    statsMap.put(statistic, statsResults);
                }
                feature2StatisticsMap.put(fid, statsMap);
            }
        }
        featureCollection.close(featureIterator);

    }

    private void processPointAndBufferMode() throws TransformException, FactoryException {

        final AffineTransform gridToWorldTransformCorrected = new AffineTransform(
                (AffineTransform) ((GridGeometry2D) gridCoverage2D.getGridGeometry())
                        .getGridToCRS2D(PixelOrientation.UPPER_LEFT));
        final MathTransform worldToGridTransform;
        try {
            worldToGridTransform = ProjectiveTransform.create(gridToWorldTransformCorrected
                    .createInverse());
        } catch (NoninvertibleTransformException e) {
            throw new ModelsIllegalargumentException(e.getLocalizedMessage(), this.getClass().getSimpleName());
        }

        CoordinateReferenceSystem crs = gridCoverage2D.getCoordinateReferenceSystem();
        boolean isDegree = false;
        if (crs instanceof DefaultGeographicCRS) {
            isDegree = true;
        }

        CoordinateReferenceSystem featuresCrs = featureCollection.getSchema()
                .getCoordinateReferenceSystem();

        MathTransform transform = CRS.findMathTransform(featuresCrs, crs);
        GeodeticCalculator gCalc = new GeodeticCalculator(crs);

        FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();

        int featureIndex = 0;
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            String fid = feature.getID();
            double buffer = buffers.length == 1 ? buffers[0] : buffers[featureIndex];
            double radiusX = buffer;
            double radiusY = buffer;

            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            if (!transform.isIdentity()) {
                geometry = JTS.transform(geometry, transform);
            }
            ReferencedEnvelope rEnvelope = null;
            if (geometry instanceof Point || geometry instanceof MultiPoint) {

                Coordinate coordinate = geometry.getCoordinate();
                /*
                 * create an envelope around the point of the size 
                 * of the buffer in the crs of the coverage to
                 * analyze.
                 */
                if (isDegree) {
                    // double convertedBuffer = converter.convert(buffer);
                    Position position = new DirectPosition2D(crs, coordinate.x, coordinate.y);
                    gCalc.setStartingPosition(position);
                    gCalc.setDirection(0, buffer);
                    Point2D destinationPoint = gCalc.getDestinationGeographicPoint();
                    radiusY = Math.abs(coordinate.y - destinationPoint.getY());

                    gCalc.setStartingPosition(position);
                    gCalc.setDirection(90, buffer);
                    destinationPoint = gCalc.getDestinationGeographicPoint();
                    radiusX = Math.abs(coordinate.x - destinationPoint.getX());
                }

                rEnvelope = new ReferencedEnvelope(coordinate.x - radiusX, coordinate.x + radiusX,
                        coordinate.y - radiusY, coordinate.y + radiusY, crs);

                final AbstractProcessor processor = AbstractProcessor.getInstance();
                ParameterValueGroup param = processor.getOperation("CoverageCrop").getParameters();
                param.parameter("Source").setValue(gridCoverage2D);
                param.parameter("Envelope").setValue(rEnvelope);
                GridCoverage2D cropped = (GridCoverage2D) processor.doOperation(param);

                java.awt.Shape awtEllipse = toAWTEllipse(coordinate, radiusX, radiusY,
                        worldToGridTransform);
                ROIShape roi = new ROIShape(awtEllipse);
                Statistic[] statistis = statisticsSet.toArray(new Statistic[statisticsSet.size()]);

                final OperationJAI op = new OperationJAI("ZonalStats");
                ParameterValueGroup params = op.getParameters();
                params.parameter("dataImage").setValue(cropped);
                // params.parameter("zoneImage").setValue(constantImage);
                params.parameter("stats").setValue(statistis);
                params.parameter("bands").setValue(bands);
                params.parameter("roi").setValue(roi);

                GridCoverage2D coverage = (GridCoverage2D) op.doOperation(params, null);
                Map<Integer, ZonalStats> statsPerBand = (Map<Integer, ZonalStats>) coverage
                        .getProperty(ZonalStatsDescriptor.ZONAL_STATS_PROPERTY);
                int numBands = coverage.getNumSampleDimensions();

                Map<Statistic, Double[]> statsMap = new HashMap<Statistic, Double[]>();
                for( Statistic statistic : statistis ) {
                    Double[] statsResults = new Double[statistis.length];
                    for( int i = 0; i < numBands; i++ ) {
                        ZonalStats zonalStats = statsPerBand.get(i);
                        Map<Statistic, Double> zoneStats = zonalStats.getZoneStats(0);
                        statsResults[i] = zoneStats.get(statistic).doubleValue();
                    }
                    statsMap.put(statistic, statsResults);
                }
                feature2StatisticsMap.put(fid, statsMap);
            }

            featureIndex++;
        }
        featureCollection.close(featureIterator);

    }

    private java.awt.Polygon toAWTPolygon( final Polygon roiInput,
            MathTransform worldToGridTransform ) throws TransformException {
        final boolean isIdentity = worldToGridTransform.isIdentity();
        final java.awt.Polygon retValue = new java.awt.Polygon();
        final double coords[] = new double[2];
        final LineString exteriorRing = roiInput.getExteriorRing();
        final CoordinateSequence exteriorRingCS = exteriorRing.getCoordinateSequence();
        final int numCoords = exteriorRingCS.size();
        for( int i = 0; i < numCoords; i++ ) {
            coords[0] = exteriorRingCS.getX(i);
            coords[1] = exteriorRingCS.getY(i);
            if (!isIdentity)
                worldToGridTransform.transform(coords, 0, coords, 0, 1);
            retValue.addPoint((int) round(coords[0] + 0.5d), (int) round(coords[1] + 0.5d));
        }
        return retValue;
    }

    private java.awt.Shape toAWTEllipse( final Coordinate center, double radiusX, double radiusY,
            MathTransform worldToGridTransform ) throws TransformException {
        final boolean isIdentity = worldToGridTransform.isIdentity();
        final double ll[] = new double[2];
        ll[0] = center.x - radiusX;
        ll[1] = center.y - radiusY;
        final double ur[] = new double[2];
        ur[0] = center.x + radiusX;
        ur[1] = center.y + radiusY;
        if (!isIdentity) {
            worldToGridTransform.transform(ll, 0, ll, 0, 1);
            worldToGridTransform.transform(ur, 0, ur, 0, 1);
        }
        Ellipse2D circle = new Ellipse2D.Double(round(ll[0] + 0.5d), round(ur[1] + 0.5d), ur[0]
                - ll[0], Math.abs(ur[1] - ll[1]));
        return circle;
    }

    /**
     * Gets the performed statistics.
     *
     * @param fId the id of the feature used as region for the analysis.
     * @return the {@link Map} of results of the analysis for all the 
     *          requested {@link Statistic} for the requested bands. Note 
     *          that the result contains for every {@link Statistic} a result
     *          value for every band.
     */
    public Map<Statistic, Double[]> getStatistics( String fId ) {
        return feature2StatisticsMap.get(fId);
    }

}
