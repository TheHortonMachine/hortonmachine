/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.lesto.modules.raster.adaptivetinfilter;

import static org.hortonmachine.gears.utils.geometry.GeometryUtilities.distance3d;
import static org.hortonmachine.gears.utils.geometry.GeometryUtilities.getAngleBetweenLinePlane;
import static org.hortonmachine.gears.utils.geometry.GeometryUtilities.getLineWithPlaneIntersection;
import static org.hortonmachine.gears.utils.geometry.GeometryUtilities.getTriangleCentroid;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.libs.modules.ThreadedRunnable;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.algorithm.locate.SimplePointInAreaLocator;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

/**
 * A helper class for tin handling.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TinHandler {
    public static final double POINTENVELOPE_EXPAND = 0.1;

    /**
     * The list of coordinates that will be used at the next tin generation.
     */
    private List<Coordinate> tinCoordinateList = new ArrayList<Coordinate>();

    /**
     * The list of coordinates that are left out as non ground points at each filtering.
     */
    private List<Coordinate> leftOverCoordinateList = new ArrayList<Coordinate>();

    /**
     * The geometries of the last calculated tin.
     */
    private Geometry[] tinGeometries = null;

    private GeometryFactory gf = GeometryUtilities.gf();

    private final IHMProgressMonitor pm;

    private boolean didInitialize = false;

    private boolean isFirstStatsCalculation = true;

    private final CoordinateReferenceSystem crs;

    private final double distanceThreshold;

    private final int threadsNum;

    private double calculatedAngleThreshold;

    private double calculatedDistanceThreshold;

    private final ReadWriteLock monitor = new ReentrantReadWriteLock();

    private final Double maxEdgeLength;
    private double maxEdgeLengthThreshold;

    private final double angleThreshold;

    /**
     * Constructor.
     *  
     * @param pm the monitor.
     * @param crs the crs to use for feature creation.
     * @param distanceThreshold the fixed maximum distance threshold to use.
     * @param maxEdgeLength the max edge length for the longest edge of a triangle (if larger, it is ignored)
     * @param threadsNum the number of threads to use for parallel operations.
     */
    public TinHandler( IHMProgressMonitor pm, CoordinateReferenceSystem crs, double angleThreshold, double distanceThreshold,
            Double maxEdgeLength, int threadsNum ) {
        this.pm = pm;
        this.crs = crs;
        this.angleThreshold = angleThreshold;
        this.distanceThreshold = distanceThreshold;
        this.maxEdgeLength = maxEdgeLength;
        this.threadsNum = threadsNum;
    }

    /**
     * Sets the initial coordinates to start with.
     * 
     * <p>Generates the tin on the first set of coordinates and adds the 
     * coordinates to the {@link #tinCoordinateList} for future use.</p>
     * 
     * <p><b>Note that it is mandatory to call this method to initialize.</b></p>
     * 
     * @param coordinateList the initial list of coordinates.
     */
    public void setStartCoordinates( List<Coordinate> coordinateList ) {
        generateTin(coordinateList);
        for( int i = 0; i < tinGeometries.length; i++ ) {
            Coordinate[] coordinates = tinGeometries[i].getCoordinates();
            if (!tinCoordinateList.contains(coordinates[0])) {
                tinCoordinateList.add(coordinates[0]);
            }
            if (!tinCoordinateList.contains(coordinates[1])) {
                tinCoordinateList.add(coordinates[1]);
            }
            if (!tinCoordinateList.contains(coordinates[2])) {
                tinCoordinateList.add(coordinates[2]);
            }
        }
        didInitialize = true;
    }

    /**
     * Get the triangles of the current active tin.
     * 
     * @return the array of polygons.
     */
    public Geometry[] getTriangles() {
        checkTinGeometries();
        return tinGeometries;
    }

    /**
     * Returns the current size of the {@link #tinCoordinateList} representing teh current ground points.
     * 
     * @return the size of the tin coords list.
     */
    public int getCurrentGroundPointsNum() {
        return tinCoordinateList.size();
    }

    /**
     * Returns the size of the points currently defined as being non-ground.
     * 
     * @return the size of the non ground points list.
     */
    public int getCurrentNonGroundPointsNum() {
        synchronized (leftOverCoordinateList) {
            return leftOverCoordinateList.size();
        }
    }

    /**
     * Filter data on thresholds of all available data on the tin.
     * 
     * <p><b>Note: At the first run of this method, only thresholds are calculated.</b></p>
     * 
     * @param lasHandler the las data handler of all data.
     * @throws Exception
     */
    public void filterOnAllData( final ALasDataManager lasHandler ) throws Exception {
        final ConcurrentSkipListSet<Double> angleSet = new ConcurrentSkipListSet<Double>();
        final ConcurrentSkipListSet<Double> distanceSet = new ConcurrentSkipListSet<Double>();

        if (isFirstStatsCalculation) {
            pm.beginTask("Calculating initial statistics...", tinGeometries.length);
        } else {
            pm.beginTask("Filtering all data on seeds tin...", tinGeometries.length);
        }
        try {
            final List<Coordinate> newTotalLeftOverCoordinateList = new ArrayList<Coordinate>();
            if (threadsNum > 1) {
                // multithreaded
                ThreadedRunnable tRun = new ThreadedRunnable(threadsNum, null);
                for( final Geometry tinGeom : tinGeometries ) {
                    tRun.executeRunnable(new Runnable(){
                        public void run() {
                            List<Coordinate> leftOverList = runFilterOnAllData(lasHandler, angleSet, distanceSet, tinGeom);
                            synchronized (newTotalLeftOverCoordinateList) {
                                newTotalLeftOverCoordinateList.addAll(leftOverList);
                            }
                        }
                    });
                }
                tRun.waitAndClose();
            } else {
                for( final Geometry tinGeom : tinGeometries ) {
                    List<Coordinate> leftOverList = runFilterOnAllData(lasHandler, angleSet, distanceSet, tinGeom);
                    newTotalLeftOverCoordinateList.addAll(leftOverList);
                }
            }
            pm.done();

            leftOverCoordinateList.clear();
            leftOverCoordinateList.addAll(newTotalLeftOverCoordinateList);

            /*
             * now recalculate the thresholds
             */
            if (angleSet.size() > 1) {
                calculatedAngleThreshold = getMedianFromSet(angleSet);
                pm.message("Calculated angle threshold: " + calculatedAngleThreshold + " (range: " + angleSet.first() + " to "
                        + angleSet.last() + ")");
            } else if (angleSet.size() == 0) {
                return;
            } else {
                calculatedAngleThreshold = angleSet.first();
                pm.message("Single angle left: " + calculatedAngleThreshold);
            }

            if (distanceSet.size() > 1) {
                calculatedDistanceThreshold = getMedianFromSet(distanceSet);
                pm.message("Calculated distance threshold: " + calculatedDistanceThreshold + " (range: " + distanceSet.first()
                        + " to " + distanceSet.last() + ")");
            } else if (distanceSet.size() == 0) {
                return;
            } else {
                calculatedDistanceThreshold = distanceSet.first();
                pm.message("Single distance left: " + calculatedDistanceThreshold);
            }
            if (isFirstStatsCalculation) {
                isFirstStatsCalculation = false;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private List<Coordinate> runFilterOnAllData( final ALasDataManager lasHandler, final ConcurrentSkipListSet<Double> angleSet,
            final ConcurrentSkipListSet<Double> distanceSet, final Geometry tinGeom ) {
        final List<Coordinate> newTotalLeftOverCoordinateList = new ArrayList<Coordinate>();
        try {
            Coordinate[] tinCoords = tinGeom.getCoordinates();
            Coordinate triangleCentroid = getTriangleCentroid(tinCoords[0], tinCoords[1], tinCoords[2]);

            List<LasRecord> pointsInGeom = lasHandler.getPointsInGeometry(tinGeom, false);
            /*
             * now sort the points in the triangle in distance order
             * from the triangle centroid, nearest first
             */
            TreeSet<Coordinate> centroidNearestSet = new TreeSet<Coordinate>(new PointsToCoordinateComparator(triangleCentroid));
            for( LasRecord pointInGeom : pointsInGeom ) {
                Coordinate c = new Coordinate(pointInGeom.x, pointInGeom.y, pointInGeom.z);
                if (c.equals(tinCoords[0]) || c.equals(tinCoords[1]) || c.equals(tinCoords[2])) {
                    // the seed point was reread
                    continue;
                }
                centroidNearestSet.add(c);
            }

            // find first possible ground coordinate
            boolean foundOne = false;
            for( Coordinate c : centroidNearestSet ) {
                if (foundOne && !isFirstStatsCalculation) {
                    if (!newTotalLeftOverCoordinateList.contains(c))
                        newTotalLeftOverCoordinateList.add(c);
                } else {
                    /*
                     * find the nearest node and distance
                     */
                    Coordinate[] nodes = getOrderedNodes(c, tinCoords[0], tinCoords[1], tinCoords[2]);
                    double nearestDistance = distance3d(nodes[0], c, null);
                    if (!isFirstStatsCalculation) {
                        /*
                         * if we are here, we are doing filtering and calc of thresholds 
                         * for the next round only on the kept data.
                         */
                        if (nearestDistance > calculatedDistanceThreshold) {
                            if (!newTotalLeftOverCoordinateList.contains(c))
                                newTotalLeftOverCoordinateList.add(c);
                            continue;
                        }
                    }
                    /*
                     * calculate the angle between the facet normal and the line
                     * connecting the point and the nearest facet node.
                     */
                    double angle = getAngleBetweenLinePlane(c, nodes[0], nodes[1], nodes[2]);
                    if (Double.isNaN(angle)) {
                        pm.errorMessage("Found NaN angle, set to 0...");
                        angle = 0.0;
                    }
                    if (!isFirstStatsCalculation) {
                        if (angle > calculatedAngleThreshold) {
                            if (!newTotalLeftOverCoordinateList.contains(c))
                                newTotalLeftOverCoordinateList.add(c);
                            continue;
                        } else {
                            // add it to the next tin
                            synchronized (tinCoordinateList) {
                                if (!tinCoordinateList.contains(c)) {
                                    tinCoordinateList.add(c);
                                    foundOne = true;
                                    angleSet.add(angle);
                                    distanceSet.add(nearestDistance);
                                }
                            }
                        }
                    } else {
                        angleSet.add(angle);
                        distanceSet.add(nearestDistance);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        pm.worked(1);
        return newTotalLeftOverCoordinateList;
    }

    public void filterOnLeftOverData() {
        if (isFirstStatsCalculation) {
            throw new IllegalArgumentException("The first round needs to be filtered on all data.");
        }
        pm.beginTask("Creating points indexes...", leftOverCoordinateList.size());
        final STRtree leftOverCoordinatesTree = new STRtree(leftOverCoordinateList.size());
        for( Coordinate c : leftOverCoordinateList ) {
            leftOverCoordinatesTree.insert(new Envelope(c), c);
        }
        pm.done();

        if (maxEdgeLength != null) {
            maxEdgeLengthThreshold = maxEdgeLength;
        }

        Geometry[] triangles = getTriangles();
        final ConcurrentSkipListSet<Double> angleSet = new ConcurrentSkipListSet<Double>();
        final ConcurrentSkipListSet<Double> distanceSet = new ConcurrentSkipListSet<Double>();
        final List<Coordinate> newTotalLeftOverCoordinateList = new ArrayList<Coordinate>();
        pm.beginTask("Filtering leftover coordinates on previous tin...", triangles.length);
        if (threadsNum > 1) {
            ThreadedRunnable tRun = new ThreadedRunnable(threadsNum, null);
            for( int i = 0; i < triangles.length; i++ ) {
                final Geometry triangle = triangles[i];
                tRun.executeRunnable(new Runnable(){
                    public void run() {
                        if (maxEdgeLength != null && triangle.getLength() < maxEdgeLengthThreshold * 3.0) {
                            return;
                        }
                        List<Coordinate> leftOverList = runfilterOnLeftOverData(leftOverCoordinatesTree, angleSet, distanceSet,
                                triangle);
                        synchronized (newTotalLeftOverCoordinateList) {
                            newTotalLeftOverCoordinateList.addAll(leftOverList);
                        }
                    }
                });
            }
            tRun.waitAndClose();
        } else {
            for( int i = 0; i < triangles.length; i++ ) {
                Geometry triangle = triangles[i];
                List<Coordinate> leftOverList = runfilterOnLeftOverData(leftOverCoordinatesTree, angleSet, distanceSet, triangle);
                newTotalLeftOverCoordinateList.addAll(leftOverList);
            }
        }
        pm.done();

        leftOverCoordinateList.clear();
        leftOverCoordinateList.addAll(newTotalLeftOverCoordinateList);

        /*
         * now recalculate the thresholds
         */
        if (angleSet.size() > 1) {
            calculatedAngleThreshold = getMedianFromSet(angleSet);
            pm.message("Calculated angle threshold: " + calculatedAngleThreshold + " (range: " + angleSet.first() + " to "
                    + angleSet.last() + ")");
        } else if (angleSet.size() == 0) {
            return;
        } else {
            calculatedAngleThreshold = angleSet.first();
            pm.message("Single angle left: " + calculatedAngleThreshold);
        }
        if (distanceSet.size() > 1) {
            calculatedDistanceThreshold = getMedianFromSet(distanceSet);
            pm.message("Calculated distance threshold: " + calculatedDistanceThreshold + " (range: " + distanceSet.first()
                    + " to " + distanceSet.last() + ")");
        } else if (distanceSet.size() == 0) {
            return;
        } else {
            calculatedDistanceThreshold = distanceSet.first();
            pm.message("Single distance left: " + calculatedDistanceThreshold);
        }
        if (calculatedDistanceThreshold < distanceThreshold) {
            calculatedDistanceThreshold = distanceThreshold;
            pm.message("Corrected calculated distance threshold to be: " + calculatedDistanceThreshold);
        }

    }

    @SuppressWarnings("rawtypes")
    private List<Coordinate> runfilterOnLeftOverData( final STRtree leftOverCoordsTree,
            final ConcurrentSkipListSet<Double> angleSet, final ConcurrentSkipListSet<Double> distanceSet, final Geometry triangle ) {

        List<Coordinate> newLeftOverCoordinateList = new ArrayList<Coordinate>();
        Coordinate[] tinCoords = triangle.getCoordinates();
        Coordinate triangleCentroid = getTriangleCentroid(tinCoords[0], tinCoords[1], tinCoords[2]);

        // get left coords around triangle
        List triangleCoordinates = null;
        monitor.readLock().lock();
        try {
            triangleCoordinates = leftOverCoordsTree.query(triangle.getEnvelopeInternal());
        } finally {
            monitor.readLock().unlock();
        }

        /*
         * now sort the points in the triangle in distance order
         * from the triangle centroid, nearest first
         */
        TreeSet<Coordinate> centroidNearestSet = new TreeSet<Coordinate>(new PointsToCoordinateComparator(triangleCentroid));
        for( Object coordinateObj : triangleCoordinates ) {
            Coordinate c = (Coordinate) coordinateObj;
            if (c.equals(tinCoords[0]) || c.equals(tinCoords[1]) || c.equals(tinCoords[2])) {
                // the seed point was reread
                continue;
            }
            int loc = SimplePointInAreaLocator.locate(c, triangle);
            if (loc == Location.INTERIOR) {
                centroidNearestSet.add(c);
            }
        }

        // find first possible ground coordinate
        boolean foundOne = false;
        for( Coordinate c : centroidNearestSet ) {
            if (foundOne && !isFirstStatsCalculation) {
                if (!newLeftOverCoordinateList.contains(c))
                    newLeftOverCoordinateList.add(c);
            } else {
                /*
                 * find the nearest node and distance
                 */
                Coordinate[] nodes = getOrderedNodes(c, tinCoords[0], tinCoords[1], tinCoords[2]);
                double nearestDistance = distance3d(nodes[0], c, null);
                /*
                 * if we are here, we are doing filtering and calc of thresholds 
                 * for the next round only on the kept data.
                 */
                if (nearestDistance > calculatedDistanceThreshold) {
                    if (!newLeftOverCoordinateList.contains(c))
                        newLeftOverCoordinateList.add(c);
                    continue;
                }
                /*
                 * calculate the angle between the facet normal and the line
                 * connecting the point and the nearest facet node.
                 */
                double angle = getAngleBetweenLinePlane(c, nodes[0], nodes[1], nodes[2]);
                if (Double.isNaN(angle)) {
                    pm.errorMessage("Found NaN angle, set to 0...");
                    angle = 0.0;
                }
                if (angle > calculatedAngleThreshold && angle > angleThreshold) { // TODO
                    if (!newLeftOverCoordinateList.contains(c))
                        newLeftOverCoordinateList.add(c);
                    continue;
                } else {
                    // add it to the next tin
                    synchronized (tinCoordinateList) {
                        if (!tinCoordinateList.contains(c)) {
                            tinCoordinateList.add(c);
                            foundOne = true;
                            angleSet.add(angle);
                            distanceSet.add(nearestDistance);
                        }
                    }
                }
            }
        }

        pm.worked(1);
        return newLeftOverCoordinateList;
    }

    public void finalCleanup( final double pFinalCleanupDist ) {
        if (isFirstStatsCalculation) {
            throw new IllegalArgumentException("The first round needs to be filtered on all data.");
        }
        pm.beginTask("Creating points indexes...", leftOverCoordinateList.size());
        final STRtree leftOverCoordinatesTree = new STRtree(leftOverCoordinateList.size());
        for( Coordinate c : leftOverCoordinateList ) {
            leftOverCoordinatesTree.insert(new Envelope(c), c);
        }
        pm.done();

        final AtomicInteger removedCount = new AtomicInteger();
        Geometry[] triangles = getTriangles();
        final List<Coordinate> newLeftOverCoordinateList = new ArrayList<Coordinate>();
        pm.beginTask("Final cleanup through triangle to point distance filter...", triangles.length);
        ThreadedRunnable tRun = new ThreadedRunnable(threadsNum, null);
        for( int i = 0; i < triangles.length; i++ ) {
            final Geometry triangle = triangles[i];
            tRun.executeRunnable(new Runnable(){
                public void run() {
                    runFinalFilter(leftOverCoordinatesTree, newLeftOverCoordinateList, triangle, pFinalCleanupDist, removedCount);
                }
            });
        }
        tRun.waitAndClose();
        pm.done();

        pm.message("Final points removed from non ground: " + removedCount.get());
        pm.message("Final points left as non ground: " + newLeftOverCoordinateList.size());
        leftOverCoordinateList.clear();
        leftOverCoordinateList.addAll(newLeftOverCoordinateList);
    }

    private void runFinalFilter( final STRtree leftOverCoordsTree, List<Coordinate> newLeftOverCoordinateList,
            final Geometry triangle, double pFinalCleanupDist, AtomicInteger removedCount ) {

        Coordinate[] tinCoords = triangle.getCoordinates();

        // get left coords around triangle
        List triangleCoordinates = null;
        monitor.readLock().lock();
        try {
            triangleCoordinates = leftOverCoordsTree.query(triangle.getEnvelopeInternal());
        } finally {
            monitor.readLock().unlock();
        }

        /*
         * now sort the points in the triangle in distance order
         * from the triangle centroid, nearest first
         */
        int count = 0;
        for( Object coordinateObj : triangleCoordinates ) {
            Coordinate c = (Coordinate) coordinateObj;
            int loc = SimplePointInAreaLocator.locate(c, triangle);
            if (loc == Location.INTERIOR) {
                Coordinate c1 = new Coordinate(c.x, c.y, 1E6);
                Coordinate c2 = new Coordinate(c.x, c.y, -1E6);
                Coordinate intersection = getLineWithPlaneIntersection(c1, c2, tinCoords[0], tinCoords[1], tinCoords[2]);
                double distance = distance3d(intersection, c, null);
                if (distance > pFinalCleanupDist) {
                    count++;
                    synchronized (newLeftOverCoordinateList) {
                        newLeftOverCoordinateList.add(c);
                    }
                }
            }
        }
        removedCount.addAndGet(count);
        pm.worked(1);
    }

    public void resetTin() {
        tinGeometries = null;
    }

    /**
     * Generate a tin from a given coords list. The internal tin geoms array is set from the result.
     * 
     * @param coordinateList the coords to use for the tin generation.
     */
    private void generateTin( List<Coordinate> coordinateList ) {
        pm.beginTask("Generate tin...", -1);
        DelaunayTriangulationBuilder b = new DelaunayTriangulationBuilder();
        b.setSites(coordinateList);
        Geometry tinTriangles = b.getTriangles(gf);
        tinGeometries = new Geometry[tinTriangles.getNumGeometries()];
        for( int i = 0; i < tinTriangles.getNumGeometries(); i++ ) {
            tinGeometries[i] = tinTriangles.getGeometryN(i);
        }
        pm.done();
    }

    /**
     * Generate a spatial index on the tin geometries.
     */
    public STRtree generateTinIndex( Double maxEdgeLength ) {
        double maxEdge = maxEdgeLength != null ? maxEdgeLength : 0.0;
        pm.beginTask("Creating tin indexes...", tinGeometries.length);
        final STRtree tinTree = new STRtree(tinGeometries.length);
        for( Geometry geometry : tinGeometries ) {
            if (maxEdgeLength != null) {
                Coordinate[] coordinates = geometry.getCoordinates();
                double maxLength = distance3d(coordinates[0], coordinates[1], null);
                double tmpLength = distance3d(coordinates[1], coordinates[2], null);
                if (tmpLength > maxLength) {
                    maxLength = tmpLength;
                }
                tmpLength = distance3d(coordinates[2], coordinates[0], null);
                if (tmpLength > maxLength) {
                    maxLength = tmpLength;
                }
                // triangles below a certain edge length are not adapted
                if (maxLength < maxEdge) {
                    continue;
                }
            }
            tinTree.insert(geometry.getEnvelopeInternal(), geometry);
        }
        pm.done();
        return tinTree;
    }

    /**
     * Checks if the tin is done. If not, it generates it with the available {@link #tinCoordinateList}.  
     */
    private void checkTinGeometries() {
        if (!didInitialize) {
            throw new IllegalArgumentException("Not initialized properly. Did you call setStartCoordinates?");
        }
        if (tinGeometries == null) {
            generateTin(tinCoordinateList);
        }
    }

    /**
     * Order coordinates to have the first coordinate in the array as the nearest to a given 
     * coordinate 'c'. The second and third are not ordered, but randomly added. 
     * 
     * @param c
     * @param coordinate1
     * @param coordinate2
     * @param coordinate3
     * @return
     */
    private Coordinate[] getOrderedNodes( Coordinate c, Coordinate coordinate1, Coordinate coordinate2, Coordinate coordinate3 ) {
        double d = distance3d(c, coordinate1, null);
        Coordinate nearest = coordinate1;
        Coordinate c2 = coordinate2;
        Coordinate c3 = coordinate3;

        double d2 = distance3d(c, coordinate2, null);
        if (d2 < d) {
            nearest = coordinate2;
            d = d2;
            c2 = coordinate1;
            c3 = coordinate3;
        }
        double d3 = distance3d(c, coordinate3, null);
        if (d3 < d) {
            nearest = coordinate3;
            c2 = coordinate1;
            c3 = coordinate2;
        }
        return new Coordinate[]{nearest, c2, c3};
    }

    private double getMedianFromSet( final ConcurrentSkipListSet<Double> set ) {
        double threshold = 0;
        int halfNum = set.size() / 2;
        int count = 0;
        for( double value : set ) {
            if (count == halfNum) {
                threshold = value;
                break;
            }
            count++;
        }
        return threshold;
    }

    private double getAverageFromSet( final ConcurrentSkipListSet<Double> set ) {
        double sum = 0;
        int count = 0;
        for( double value : set ) {
            sum = sum + value;
            count++;
        }
        return sum / count;
    }

    private double getCenterFromSet( final ConcurrentSkipListSet<Double> set ) {
        return (set.last() - set.first()) / 2;
    }

    /**
     * Create a {@link SimpleFeatureCollection FeatureCollection} from the current tin triangles
     * with information about the vertexes elevation.
     * 
     * @return the feature collection of the tin.
     */
    public SimpleFeatureCollection toFeatureCollection() {
        checkTinGeometries();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("triangle");
        b.setCRS(crs);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        b.add("the_geom", Polygon.class);
        b.add("elev0", Double.class);
        b.add("elev1", Double.class);
        b.add("elev2", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for( Geometry g : tinGeometries ) {
            Coordinate[] coordinates = g.getCoordinates();

            Object[] values;
            int windingRule = GeometryUtilities.getTriangleWindingRule(coordinates[0], coordinates[1], coordinates[2]);
            if (windingRule > 0) {
                // need to reverse the triangle
                values = new Object[]{g, coordinates[0].z, coordinates[2].z, coordinates[1].z};
            } else {
                values = new Object[]{g, coordinates[0].z, coordinates[1].z, coordinates[2].z};
            }

            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            newCollection.add(feature);
        }
        return newCollection;
    }

    public SimpleFeatureCollection toFeatureCollectionOthers() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("points");
        b.setCRS(crs);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        b.add("the_geom", Point.class);
        b.add("elev", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for( Coordinate c : leftOverCoordinateList ) {
            Object[] values = new Object[]{gf.createPoint(c), c.z};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            newCollection.add(feature);
        }
        return newCollection;
    }

    public SimpleFeatureCollection toFeatureCollectionTinPoints() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("points");
        b.setCRS(crs);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        b.add("the_geom", Point.class);
        b.add("elev", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        for( Coordinate c : tinCoordinateList ) {
            Object[] values = new Object[]{gf.createPoint(c), c.z};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            newCollection.add(feature);
        }
        return newCollection;
    }

    public double[] getMinMaxElev() {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        for( Coordinate coordinate : tinCoordinateList ) {
            max = Math.max(max, coordinate.z);
            min = Math.min(min, coordinate.z);
        }
        return new double[]{min, max};
    }

    // private AtomicInteger count = new AtomicInteger();
    // private void dumpPointsInGeom( Geometry tinGeom, List<LasRecord> pointsInGeom,
    // List<Coordinate> addedPoints )
    // throws IOException {
    // String path = "/home/moovida/dati_unibz/outshape/tinfilter/triangles/";
    // int i = count.getAndIncrement();
    // {
    // SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
    // b.setName("t" + i);
    // b.setCRS(crs);
    //
    // DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
    // b.add("the_geom", Polygon.class);
    // b.add("elev1", Double.class);
    // b.add("elev2", Double.class);
    // b.add("elev3", Double.class);
    // SimpleFeatureType type = b.buildFeatureType();
    // SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
    // Coordinate[] coordinates = tinGeom.getCoordinates();
    // Object[] values = new Object[]{tinGeom, coordinates[0].z, coordinates[1].z,
    // coordinates[2].z};
    // builder.addAll(values);
    // SimpleFeature feature = builder.buildFeature(null);
    // newCollection.add(feature);
    //
    // OmsVectorWriter.writeVector(path + "t" + i + ".shp", newCollection);
    // }
    // {
    // SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
    // b.setName("p" + i);
    // b.setCRS(crs);
    //
    // DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
    // b.add("the_geom", Point.class);
    // b.add("elev", Double.class);
    // SimpleFeatureType type = b.buildFeatureType();
    // SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
    //
    // for( LasRecord lasRecord : pointsInGeom ) {
    // Point point = gf.createPoint(new Coordinate(lasRecord.x, lasRecord.y, lasRecord.z));
    // Object[] values = new Object[]{point, lasRecord.z};
    // builder.addAll(values);
    // SimpleFeature feature = builder.buildFeature(null);
    // newCollection.add(feature);
    // }
    //
    // OmsVectorWriter.writeVector(path + "p" + i + ".shp", newCollection);
    // }
    // {
    // SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
    // b.setName("a" + i);
    // b.setCRS(crs);
    //
    // DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
    // b.add("the_geom", Point.class);
    // b.add("elev", Double.class);
    // SimpleFeatureType type = b.buildFeatureType();
    // SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
    //
    // for( Coordinate coord : addedPoints ) {
    // Point point = gf.createPoint(coord);
    // Object[] values = new Object[]{point, coord.z};
    // builder.addAll(values);
    // SimpleFeature feature = builder.buildFeature(null);
    // newCollection.add(feature);
    // }
    //
    // OmsVectorWriter.writeVector(path + "a" + i + ".shp", newCollection);
    // }
    // }
}
