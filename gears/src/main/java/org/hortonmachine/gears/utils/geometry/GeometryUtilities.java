/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.utils.geometry;

import static java.lang.Math.abs;
import static java.lang.Math.acos;
import static java.lang.Math.atan;
import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static java.lang.Math.toRadians;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.hortonmachine.gears.utils.sorting.QuickSortAlgorithm;

import org.locationtech.jts.algorithm.PointLocator;
import org.locationtech.jts.algorithm.RobustLineIntersector;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineSegment;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Location;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.index.chain.MonotoneChain;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.linearref.LengthIndexedLine;
import org.locationtech.jts.noding.IntersectionAdder;
import org.locationtech.jts.noding.MCIndexNoder;
import org.locationtech.jts.noding.NodedSegmentString;
import org.locationtech.jts.operation.linemerge.LineMerger;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

/**
 * Utilities related to {@link Geometry}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeometryUtilities {

    public static Geometry[] TYPE_GEOMETRY = new Geometry[0];
    public static Polygon[] TYPE_POLYGON = new Polygon[0];
    public static MultiPolygon[] TYPE_MULTIPOLYGON = new MultiPolygon[0];
    public static LineString[] TYPE_LINESTRING = new LineString[0];
    public static MultiLineString[] TYPE_MULTILINESTRING = new MultiLineString[0];
    public static Point[] TYPE_POINT = new Point[0];
    public static MultiPoint[] TYPE_MULTIPOINT = new MultiPoint[0];

    private static GeometryFactory geomFactory;
    private static PrecisionModel precModel;

    public static PrecisionModel basicPrecisionModel() {
        return (pm());
    }

    public static GeometryFactory gf() {
        if (geomFactory == null) {
            geomFactory = new GeometryFactory(pm());
        }
        return (geomFactory);
    }

    public static PrecisionModel pm() {
        if (precModel == null) {
            precModel = new PrecisionModel(PrecisionModel.FLOATING);
        }
        return (precModel);
    }

    /**
     * Create a simple polygon (no holes).
     * 
     * @param coords the coords of the polygon.
     * @return the {@link Polygon}.
     */
    public static Polygon createSimplePolygon( Coordinate[] coords ) {
        LinearRing linearRing = gf().createLinearRing(coords);
        return gf().createPolygon(linearRing, null);
    }

    /**
     * Creates a polygon that may help out as placeholder.
     * 
     * @return a dummy {@link Polygon}.
     */
    public static Polygon createDummyPolygon() {
        Coordinate[] c = new Coordinate[]{new Coordinate(0.0, 0.0), new Coordinate(1.0, 1.0), new Coordinate(1.0, 0.0),
                new Coordinate(0.0, 0.0)};
        LinearRing linearRing = gf().createLinearRing(c);
        return gf().createPolygon(linearRing, null);
    }

    /**
     * Creates a line that may help out as placeholder.
     * 
     * @return a dummy {@link LineString}.
     */
    public static LineString createDummyLine() {
        Coordinate[] c = new Coordinate[]{new Coordinate(0.0, 0.0), new Coordinate(1.0, 1.0), new Coordinate(1.0, 0.0)};
        LineString lineString = gf().createLineString(c);
        return lineString;
    }

    /**
     * Creates a point that may help out as placeholder.
     * 
     * @return a dummy {@link Point}.
     */
    public static Point createDummyPoint() {
        Point point = gf().createPoint(new Coordinate(0.0, 0.0));
        return point;
    }

    public static Polygon createPolygonFromEnvelope( Envelope env ) {
        double minX = env.getMinX();
        double minY = env.getMinY();
        double maxY = env.getMaxY();
        double maxX = env.getMaxX();
        Coordinate[] c = new Coordinate[]{new Coordinate(minX, minY), new Coordinate(minX, maxY), new Coordinate(maxX, maxY),
                new Coordinate(maxX, minY), new Coordinate(minX, minY)};
        return gf().createPolygon(c);
    }

    public static Geometry createPolygonsFromRanges( double[] xRanges, double[] yRanges ) {
        List<Geometry> geomsList = new ArrayList<>();
        int cols = xRanges.length - 1;
        int rows = yRanges.length - 1;
        for( int x = 0; x < cols - 1; x++ ) {
            double x1 = xRanges[x];
            double x2 = xRanges[x + 1];
            for( int y = 0; y < rows - 1; y++ ) {
                double y1 = xRanges[y];
                double y2 = xRanges[y + 1];

                Envelope env = new Envelope(x1, x2, y1, y2);
                Polygon poly = GeometryUtilities.createPolygonFromEnvelope(env);
                geomsList.add(poly);
            }
        }
        Geometry union = CascadedPolygonUnion.union(geomsList);
        return union;
    }

    public static List<Geometry> extractSubGeometries( Geometry geometry ) {
        List<Geometry> geometriesList = new ArrayList<Geometry>();
        int numGeometries = geometry.getNumGeometries();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometryN = geometry.getGeometryN(i);
            geometriesList.add(geometryN);
        }
        return geometriesList;
    }

    /**
     * Calculates the angle between two {@link LineSegment}s.
     * 
     * @param l1 the first segment.
     * @param l2 the second segment.
     * @return the angle between the two segments, starting from the first segment 
     *                  moving clockwise.
     */
    public static double angleBetween( LineSegment l1, LineSegment l2 ) {
        double azimuth1 = azimuth(l1.p0, l1.p1);
        double azimuth2 = azimuth(l2.p0, l2.p1);

        if (azimuth1 < azimuth2) {
            return azimuth2 - azimuth1;
        } else {
            return 360 - azimuth1 + azimuth2;
        }
    }

    /**
     * Calculates the azimuth in degrees given two {@link Coordinate} composing a line.
     * 
     * Note that the coords order is important and will differ of 180.
     * 
     * @param c1 first coordinate (used as origin).
     * @param c2 second coordinate.
     * @return the azimuth angle.
     */
    public static double azimuth( Coordinate c1, Coordinate c2 ) {
        // vertical
        if (c1.x == c2.x) {
            if (c1.y == c2.y) {
                // same point
                return Double.NaN;
            } else if (c1.y < c2.y) {
                return 0.0;
            } else if (c1.y > c2.y) {
                return 180.0;
            }
        }
        // horiz
        if (c1.y == c2.y) {
            if (c1.x < c2.x) {
                return 90.0;
            } else if (c1.x > c2.x) {
                return 270.0;
            }
        }
        // -> /
        if (c1.x < c2.x && c1.y < c2.y) {
            double tanA = (c2.x - c1.x) / (c2.y - c1.y);
            double atan = atan(tanA);
            return toDegrees(atan);
        }
        // -> \
        if (c1.x < c2.x && c1.y > c2.y) {
            double tanA = (c1.y - c2.y) / (c2.x - c1.x);
            double atan = atan(tanA);
            return toDegrees(atan) + 90.0;
        }
        // <- /
        if (c1.x > c2.x && c1.y > c2.y) {
            double tanA = (c1.x - c2.x) / (c1.y - c2.y);
            double atan = atan(tanA);
            return toDegrees(atan) + 180;
        }
        // <- \
        if (c1.x > c2.x && c1.y < c2.y) {
            double tanA = (c2.y - c1.y) / (c1.x - c2.x);
            double atan = atan(tanA);
            return toDegrees(atan) + 270;
        }

        return Double.NaN;
    }

    public static Coordinate getCoordinateAtAzimuthDistance( Coordinate startPoint, double azimuthDeg, double distance ) {

        double x1 = startPoint.x + distance * Math.sin(Math.toRadians(azimuthDeg));
        double y1 = startPoint.y + distance * Math.cos(Math.toRadians(azimuthDeg));

        return new Coordinate(x1, y1);
    }

    /**
     * Calculates the area of a polygon from its vertices.
     * 
     * @param x the array of x coordinates.
     * @param y the array of y coordinates.
     * @param N the number of sides of the polygon.
     * @return the area of the polygon.
     */
    public static double getPolygonArea( int[] x, int[] y, int N ) {
        int i, j;
        double area = 0;

        for( i = 0; i < N; i++ ) {
            j = (i + 1) % N;
            area += x[i] * y[j];
            area -= y[i] * x[j];
        }

        area /= 2;
        return (area < 0 ? -area : area);
    }

    /**
     * Calculates the 3d distance between two coordinates that have an elevation information.
     * 
     * <p>Note that the {@link Coordinate#distance(Coordinate)} method does only 2d.
     * 
     * @param c1 coordinate 1.
     * @param c2 coordinate 2.
     * @param geodeticCalculator If supplied it will be used for planar distance calculation.
     * @return the distance considering also elevation.
     */
    public static double distance3d( Coordinate c1, Coordinate c2, GeodeticCalculator geodeticCalculator ) {
        if (Double.isNaN(c1.z) || Double.isNaN(c2.z)) {
            throw new IllegalArgumentException("Missing elevation information in the supplied coordinates.");
        }
        double deltaElev = Math.abs(c1.z - c2.z);
        double projectedDistance;
        if (geodeticCalculator != null) {
            geodeticCalculator.setStartingGeographicPoint(c1.x, c1.y);
            geodeticCalculator.setDestinationGeographicPoint(c2.x, c2.y);
            projectedDistance = geodeticCalculator.getOrthodromicDistance();
        } else {
            projectedDistance = c1.distance(c2);
        }

        double distance = NumericsUtilities.pythagoras(projectedDistance, deltaElev);
        return distance;
    }

    public static void sortHorizontal( Coordinate[] coordinates ) {
        QuickSortAlgorithm sorter = new QuickSortAlgorithm(new DummyProgressMonitor());

        double[] x = new double[coordinates.length];
        double[] y = new double[coordinates.length];
        for( int i = 0; i < x.length; i++ ) {
            x[i] = coordinates[i].x;
            y[i] = coordinates[i].y;
        }

        sorter.sort(x, y);

        for( int i = 0; i < x.length; i++ ) {
            coordinates[i].x = x[i];
            coordinates[i].y = y[i];
        }
    }

    /**
     * Joins two lines to a polygon.
     * 
     * @param checkValid checks if the resulting polygon is valid.
     * @param lines the lines to use.
     * @return the joined polygon or <code>null</code> if something ugly happened. 
     */
    public static Polygon lines2Polygon( boolean checkValid, LineString... lines ) {
        List<Coordinate> coordinatesList = new ArrayList<Coordinate>();

        List<LineString> linesList = new ArrayList<LineString>();
        for( LineString tmpLine : lines ) {
            linesList.add(tmpLine);
        }

        LineString currentLine = linesList.get(0);
        linesList.remove(0);
        while( linesList.size() > 0 ) {
            Coordinate[] coordinates = currentLine.getCoordinates();
            List<Coordinate> tmpList = Arrays.asList(coordinates);
            coordinatesList.addAll(tmpList);

            Point thePoint = currentLine.getEndPoint();

            double minDistance = Double.MAX_VALUE;
            LineString minDistanceLine = null;
            boolean needFlip = false;
            for( LineString tmpLine : linesList ) {
                Point tmpStartPoint = tmpLine.getStartPoint();
                double distance = thePoint.distance(tmpStartPoint);
                if (distance < minDistance) {
                    minDistance = distance;
                    minDistanceLine = tmpLine;
                    needFlip = false;
                }

                Point tmpEndPoint = tmpLine.getEndPoint();
                distance = thePoint.distance(tmpEndPoint);
                if (distance < minDistance) {
                    minDistance = distance;
                    minDistanceLine = tmpLine;
                    needFlip = true;
                }
            }

            linesList.remove(minDistanceLine);

            if (needFlip) {
                minDistanceLine = (LineString) minDistanceLine.reverse();
            }

            currentLine = minDistanceLine;

        }
        // add last
        Coordinate[] coordinates = currentLine.getCoordinates();
        List<Coordinate> tmpList = Arrays.asList(coordinates);
        coordinatesList.addAll(tmpList);

        coordinatesList.add(coordinatesList.get(0));
        LinearRing linearRing = gf().createLinearRing(coordinatesList.toArray(new Coordinate[0]));
        Polygon polygon = gf().createPolygon(linearRing, null);

        if (checkValid) {
            if (!polygon.isValid()) {
                return null;
            }
        }
        return polygon;
    }

    /**
     * Returns the coordinates at a given interval along the line.
     * 
     * <p>
     * Note that first and last coordinate are also added, making it
     * likely that the interval between the last two coordinates is less 
     * than the supplied interval.
     * </p>
     * 
     * 
     * @param line the line to use.
     * @param interval the interval to use as distance between coordinates. Has to be > 0.
     * @param keepExisting if <code>true</code>, keeps the existing coordinates in the generated list.
     *          Aslo in this case the interval around the existing points will not reflect the asked interval.
     * @param startFrom if > 0, it defines the initial distance to jump.
     * @param endAt if > 0, it defines where to end, even if the line is longer.
     * @return the list of extracted coordinates.
     */
    public static List<Coordinate> getCoordinatesAtInterval( LineString line, double interval, boolean keepExisting,
            double startFrom, double endAt ) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval needs to be > 0.");
        }
        double length = line.getLength();
        if (startFrom < 0) {
            startFrom = 0.0;
        }
        if (endAt < 0) {
            endAt = length;
        }

        List<Coordinate> coordinatesList = new ArrayList<Coordinate>();

        LengthIndexedLine indexedLine = new LengthIndexedLine(line);
        Coordinate[] existingCoordinates = null;
        double[] indexOfExisting = null;
        if (keepExisting) {
            existingCoordinates = line.getCoordinates();
            indexOfExisting = new double[existingCoordinates.length];
            int i = 0;
            for( Coordinate coordinate : existingCoordinates ) {
                double indexOf = indexedLine.indexOf(coordinate);
                indexOfExisting[i] = indexOf;
                i++;
            }
        }

        double runningLength = startFrom;
        int currentIndexOfexisting = 1; // jump first
        while( runningLength < endAt ) {
            if (keepExisting && currentIndexOfexisting < indexOfExisting.length - 1
                    && runningLength > indexOfExisting[currentIndexOfexisting]) {
                // add the existing
                coordinatesList.add(existingCoordinates[currentIndexOfexisting]);
                currentIndexOfexisting++;
                continue;
            }

            Coordinate extractedPoint = indexedLine.extractPoint(runningLength);
            coordinatesList.add(extractedPoint);
            runningLength = runningLength + interval;
        }
        Coordinate extractedPoint = indexedLine.extractPoint(endAt);
        coordinatesList.add(extractedPoint);

        return coordinatesList;
    }

    /**
     * Extracts traversal sections of a given with from the supplied {@link Coordinate}s.
     * 
     * @param coordinates the list of coordinates.
     * @param width the total with of the sections.
     * @return the list of {@link LineString sections}. 
     */
    public static List<LineString> getSectionsFromCoordinates( List<Coordinate> coordinates, double width ) {

        if (coordinates.size() < 3) {
            throw new IllegalArgumentException("This method works only on lines with at least 3 coordinates.");
        }
        double halfWidth = width / 2.0;
        List<LineString> linesList = new ArrayList<LineString>();
        // first section
        Coordinate centerCoordinate = coordinates.get(0);
        LineSegment l1 = new LineSegment(centerCoordinate, coordinates.get(1));
        Coordinate leftCoordinate = l1.pointAlongOffset(0.0, halfWidth);
        Coordinate rightCoordinate = l1.pointAlongOffset(0.0, -halfWidth);
        LineString lineString = geomFactory.createLineString(new Coordinate[]{leftCoordinate, centerCoordinate, rightCoordinate});
        linesList.add(lineString);

        for( int i = 1; i < coordinates.size() - 1; i++ ) {
            Coordinate previous = coordinates.get(i - 1);
            Coordinate current = coordinates.get(i);
            Coordinate after = coordinates.get(i + 1);

            double firstAngle = azimuth(current, previous);
            double secondAngle = azimuth(current, after);

            double a1 = min(firstAngle, secondAngle);
            double a2 = max(firstAngle, secondAngle);

            double centerAngle = a1 + (a2 - a1) / 2.0;

            AffineTransformation rotationInstance = AffineTransformation.rotationInstance(-toRadians(centerAngle), current.x,
                    current.y);

            LineString vertical = geomFactory.createLineString(new Coordinate[]{new Coordinate(current.x, current.y + halfWidth),
                    current, new Coordinate(current.x, current.y - halfWidth)});
            Geometry transformed = rotationInstance.transform(vertical);
            linesList.add((LineString) transformed);
        }

        // last section
        centerCoordinate = coordinates.get(coordinates.size() - 1);
        LineSegment l2 = new LineSegment(centerCoordinate, coordinates.get(coordinates.size() - 2));
        leftCoordinate = l2.pointAlongOffset(0.0, halfWidth);
        rightCoordinate = l2.pointAlongOffset(0.0, -halfWidth);
        lineString = geomFactory.createLineString(new Coordinate[]{leftCoordinate, centerCoordinate, rightCoordinate});
        linesList.add(lineString);

        return linesList;
    }

    /**
     * Returns the section line at a given interval along the line.
     * 
     * <p>
     * The returned lines are digitized from left to right and contain also the 
     * center point.
     * </p>
     * <p>
     * Note that first and last coordinate's section are also added, making it
     * likely that the interval between the last two coordinates is less 
     * than the supplied interval.
     * </p>
     * 
     * 
     * @param line the line to use.
     * @param interval the interval to use as distance between coordinates. Has to be > 0.
     * @param width the total width of the section.
     * @return the list of coordinates.
     * @param startFrom if > 0, it defines the initial distance to jump.
     * @param endAt if > 0, it defines where to end, even if the line is longer.
     * @return the list of sections lines at a given interval.
     */
    public static List<LineString> getSectionsAtInterval( LineString line, double interval, double width, double startFrom,
            double endAt ) {
        if (interval <= 0) {
            throw new IllegalArgumentException("Interval needs to be > 0.");
        }
        double length = line.getLength();
        if (startFrom < 0) {
            startFrom = 0.0;
        }
        if (endAt < 0) {
            endAt = length;
        }

        double halfWidth = width / 2.0;
        List<LineString> linesList = new ArrayList<LineString>();

        LengthIndexedLine indexedLine = new LengthIndexedLine(line);
        double runningLength = startFrom;
        while( runningLength < endAt ) {
            Coordinate centerCoordinate = indexedLine.extractPoint(runningLength);
            Coordinate leftCoordinate = indexedLine.extractPoint(runningLength, -halfWidth);
            Coordinate rightCoordinate = indexedLine.extractPoint(runningLength, halfWidth);
            LineString lineString = geomFactory
                    .createLineString(new Coordinate[]{leftCoordinate, centerCoordinate, rightCoordinate});
            linesList.add(lineString);
            runningLength = runningLength + interval;
        }
        Coordinate centerCoordinate = indexedLine.extractPoint(endAt);
        Coordinate leftCoordinate = indexedLine.extractPoint(endAt, -halfWidth);
        Coordinate rightCoordinate = indexedLine.extractPoint(endAt, halfWidth);
        LineString lineString = geomFactory.createLineString(new Coordinate[]{leftCoordinate, centerCoordinate, rightCoordinate});
        linesList.add(lineString);

        return linesList;
    }

    /**
     * Pack a list of geometries in a {@link STRtree}.
     * 
     * <p>Note that the tree can't be modified once the query method has been called first.</p>
     * 
     * @param geometries the list of geometries.
     * @return the {@link STRtree}.
     */
    public static STRtree geometriesToSRTree( List< ? extends Geometry> geometries ) {
        STRtree tree = new STRtree();
        for( Geometry geometry : geometries ) {
            tree.insert(geometry.getEnvelopeInternal(), geometry);
        }
        return tree;
    }

    /**
     * Query and test intersection on the result of an STRtree index containing geometries.
     * 
     * @param tree the index.
     * @param intersectionGeometry the geometry to check;
     * @return the intersecting geometries.
     */
    public static List<Geometry> queryAndIntersectGeometryTree( STRtree tree, Geometry intersectionGeometry ) {
        @SuppressWarnings("unchecked")
        List<Geometry> result = tree.query(intersectionGeometry.getEnvelopeInternal());
        result.removeIf(item -> {
            Geometry g = (Geometry) item;
            if (g.intersects(intersectionGeometry)) {
                return false;
            }
            return true;
        });
        return result;
    }

    public static Quadtree geometriesToQuadTree( List< ? extends Geometry> geometries ) {
        Quadtree tree = new Quadtree();
        for( Geometry geometry : geometries ) {
            tree.insert(geometry.getEnvelopeInternal(), geometry);
        }
        return tree;
    }

    /**
     * {@link Polygon} by {@link LineString} split.
     * 
     * <p>From JTS ml: http://lists.refractions.net/pipermail/jts-devel/2008-September/002666.html</p> 
     * 
     * @param polygon the input polygon.
     * @param line the input line to use to split.
     * @return the list of split polygons.
     */
    public static List<Polygon> splitPolygon( Polygon polygon, LineString line ) {
        /*
         * Use MCIndexNoder to node the polygon and linestring together, 
         * Polygonizer to polygonize the noded edges, and then PointLocater 
         * to determine which of the resultant polygons correspond to 
         * the input polygon. 
         */
        IntersectionAdder _intersector = new IntersectionAdder(new RobustLineIntersector());
        MCIndexNoder mci = new MCIndexNoder();
        mci.setSegmentIntersector(_intersector);
        NodedSegmentString pSeg = new NodedSegmentString(polygon.getCoordinates(), null);
        NodedSegmentString lSeg = new NodedSegmentString(line.getCoordinates(), null);
        List<NodedSegmentString> nodesSegmentStringList = new ArrayList<NodedSegmentString>();
        nodesSegmentStringList.add(pSeg);
        nodesSegmentStringList.add(lSeg);
        mci.computeNodes(nodesSegmentStringList);
        Polygonizer polygonizer = new Polygonizer();
        List<LineString> lsList = new ArrayList<LineString>();
        for( Object o : mci.getMonotoneChains() ) {
            MonotoneChain mtc = (MonotoneChain) o;
            LineString l = gf().createLineString(mtc.getCoordinates());
            lsList.add(l);
        }
        Geometry nodedLineStrings = lsList.get(0);
        for( int i = 1; i < lsList.size(); i++ ) {
            nodedLineStrings = nodedLineStrings.union(lsList.get(i));
        }
        polygonizer.add(nodedLineStrings);
        @SuppressWarnings("unchecked")
        Collection<Polygon> polygons = polygonizer.getPolygons();
        List<Polygon> newPolygons = new ArrayList<Polygon>();
        PointLocator pl = new PointLocator();
        for( Polygon p : polygons ) {
            if (pl.locate(p.getInteriorPoint().getCoordinate(), p) == Location.INTERIOR) {
                newPolygons.add(p);
            }
        }
        return newPolygons;
    }

    /**
     * Extends or shrinks a rectangle following the ration of a fixed one.
     * 
     * <p>This keeps the center point of the rectangle fixed.</p>
     * 
     * @param fixed the fixed {@link Rectangle2D} to use for the ratio.
     * @param toScale the {@link Rectangle2D} to adapt to the ratio of the fixed one. 
     * @param doShrink if <code>true</code>, the adapted rectangle is shrinked as 
     *          opposed to extended.
     */
    public static void scaleToRatio( Rectangle2D fixed, Rectangle2D toScale, boolean doShrink ) {
        double origWidth = fixed.getWidth();
        double origHeight = fixed.getHeight();
        double toAdaptWidth = toScale.getWidth();
        double toAdaptHeight = toScale.getHeight();

        double scaleWidth = 0;
        double scaleHeight = 0;

        scaleWidth = toAdaptWidth / origWidth;
        scaleHeight = toAdaptHeight / origHeight;
        double scaleFactor;
        if (doShrink) {
            scaleFactor = Math.min(scaleWidth, scaleHeight);
        } else {
            scaleFactor = Math.max(scaleWidth, scaleHeight);
        }

        double newWidth = origWidth * scaleFactor;
        double newHeight = origHeight * scaleFactor;

        double dw = (toAdaptWidth - newWidth) / 2.0;
        double dh = (toAdaptHeight - newHeight) / 2.0;

        double newX = toScale.getX() + dw;
        double newY = toScale.getY() + dh;
        double newW = toAdaptWidth - 2 * dw;
        double newH = toAdaptHeight - 2 * dh;
        toScale.setRect(newX, newY, newW, newH);
    }

    /**
     * Scales a rectangle down to fit inside the given one, keeping the ratio.
     * 
     * @param rectToFitIn the fixed rectangle to fit in.
     * @param toScale the rectangle to scale.
     */
    public static void scaleDownToFit( Rectangle2D rectToFitIn, Rectangle2D toScale ) {
        double fitWidth = rectToFitIn.getWidth();
        double fitHeight = rectToFitIn.getHeight();

        double toScaleWidth = toScale.getWidth();
        double toScaleHeight = toScale.getHeight();

        if (toScaleWidth > fitWidth) {
            double factor = toScaleWidth / fitWidth;
            toScaleWidth = fitWidth;
            toScaleHeight = toScaleHeight / factor;
        }
        if (toScaleHeight > fitHeight) {
            double factor = toScaleHeight / fitHeight;
            toScaleHeight = fitHeight;
            toScaleWidth = toScaleWidth / factor;
        }
        toScale.setRect(0, 0, toScaleWidth, toScaleHeight);
    }

    /**
     * Calculates the coeffs of the plane equation: ax+by+cz+d=0 given 3 coordinates.
     * 
     * @param c1 coordinate 1.
     * @param c2 coordinate 2.
     * @param c3 coordinate 3.
     * @return the array of the coeffs [a, b, c, d]
     */
    public static double[] getPlaneCoefficientsFrom3Points( Coordinate c1, Coordinate c2, Coordinate c3 ) {
        double a = (c2.y - c1.y) * (c3.z - c1.z) - (c3.y - c1.y) * (c2.z - c1.z);
        double b = (c2.z - c1.z) * (c3.x - c1.x) - (c3.z - c1.z) * (c2.x - c1.x);
        double c = (c2.x - c1.x) * (c3.y - c1.y) - (c3.x - c1.x) * (c2.y - c1.y);
        double d = -1.0 * (a * c1.x + b * c1.y + c * c1.z);
        return new double[]{a, b, c, d};
    }

    /**
     * Get the intersection coordinate between a line and plane.
     * 
     * <p>The line is defined by 2 3d coordinates and the plane by 3 3d coordinates.</p>
     * 
     * <p>from http://paulbourke.net/geometry/pointlineplane/</p>
     * 
     * @param lC1 line coordinate 1.
     * @param lC2 line coordinate 2.
     * @param pC1 plane coordinate 1.
     * @param pC2 plane coordinate 2.
     * @param pC3 plane coordinate 3.
     * @return the intersection coordinate or <code>null</code> if the line is parallel to the plane.
     */
    public static Coordinate getLineWithPlaneIntersection( Coordinate lC1, Coordinate lC2, Coordinate pC1, Coordinate pC2,
            Coordinate pC3 ) {
        double[] p = getPlaneCoefficientsFrom3Points(pC1, pC2, pC3);

        double denominator = p[0] * (lC1.x - lC2.x) + p[1] * (lC1.y - lC2.y) + p[2] * (lC1.z - lC2.z);
        if (denominator == 0.0) {
            return null;
        }
        double u = (p[0] * lC1.x + p[1] * lC1.y + p[2] * lC1.z + p[3]) / //
                denominator;
        double x = lC1.x + (lC2.x - lC1.x) * u;
        double y = lC1.y + (lC2.y - lC1.y) * u;
        double z = lC1.z + (lC2.z - lC1.z) * u;
        return new Coordinate(x, y, z);
    }

    /**
     * Calculates the angle between line and plane.
     * 
     * http://geogebrawiki.wikispaces.com/3D+Geometry
     * 
     * @param a the 3d point.
     * @param d the point of intersection between line and plane.
     * @param b the second plane coordinate.
     * @param c the third plane coordinate.
     * @return the angle in degrees between line and plane.
     */
    public static double getAngleBetweenLinePlane( Coordinate a, Coordinate d, Coordinate b, Coordinate c ) {

        double[] rAD = {d.x - a.x, d.y - a.y, d.z - a.z};
        double[] rDB = {b.x - d.x, b.y - d.y, b.z - d.z};
        double[] rDC = {c.x - d.x, c.y - d.y, c.z - d.z};

        double[] n = {//
                /*    */rDB[1] * rDC[2] - rDC[1] * rDB[2], //
                -1 * (rDB[0] * rDC[2] - rDC[0] * rDB[2]), //
                rDB[0] * rDC[1] - rDC[0] * rDB[1]//
        };

        double cosNum = n[0] * rAD[0] + n[1] * rAD[1] + n[2] * rAD[2];
        double cosDen = sqrt(n[0] * n[0] + n[1] * n[1] + n[2] * n[2]) * sqrt(rAD[0] * rAD[0] + rAD[1] * rAD[1] + rAD[2] * rAD[2]);
        double cos90MinAlpha = abs(cosNum / cosDen);
        double alpha = 90.0 - toDegrees(acos(cos90MinAlpha));
        return alpha;
    }

    /**
     * Get shortest distance from a point in 3d to a plane defined by 3 coordinates.
     * 
     * @param c the point in 3d.
     * @param pC1 plane coordinate 1.
     * @param pC2 plane coordinate 2.
     * @param pC3 plane coordinate 3.
     * @return the shortest distance from the point to the plane.
     */
    public static double getShortestDistanceFromTriangle( Coordinate c, Coordinate pC1, Coordinate pC2, Coordinate pC3 ) {
        double[] p = getPlaneCoefficientsFrom3Points(pC1, pC2, pC3);
        double result = (p[0] * c.x + p[1] * c.y + p[2] * c.z + p[3]) / Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
        return result;
    }

    /**
     * Uses the cosine rule to find an angle in radiants of a triangle defined by the length of its sides. 
     * 
     * <p>The calculated angle is the one between the two adjacent sides a and b.</p> 
     * 
     * @param a adjacent side 1 length.
     * @param b adjacent side 2 length.
     * @param c opposite side length.
     * @return the angle in radiants.
     */
    public static double getAngleInTriangle( double a, double b, double c ) {
        double angle = Math.acos((a * a + b * b - c * c) / (2.0 * a * b));
        return angle;
    }

    /**
     * Calculates the angle in degrees between 3 3D coordinates.
     * 
     * <p>The calculated angle is the one placed in vertex c2.</p>
     * 
     * @param c1 first 3D point.
     * @param c2 central 3D point.
     * @param c3 last 3D point.
     * @return the angle between the coordinates in degrees.
     */
    public static double angleBetween3D( Coordinate c1, Coordinate c2, Coordinate c3 ) {
        double a = distance3d(c2, c1, null);
        double b = distance3d(c2, c3, null);
        double c = distance3d(c1, c3, null);

        double angleInTriangle = getAngleInTriangle(a, b, c);
        double degrees = toDegrees(angleInTriangle);
        return degrees;
    }

    /**
     * Get the winding rule of a triangle by their coordinates (given in digitized order).
     * 
     * @param A coordinate 1.
     * @param B coordinate 2.
     * @param C coordinate 3.
     * @return -1 if the digitalization is clock wise, else 1.
     */
    public static int getTriangleWindingRule( Coordinate A, Coordinate B, Coordinate C ) {
        double[] rBA = {B.x - A.x, B.y - A.y, B.z - A.z};
        double[] rCA = {C.x - A.x, C.y - A.y, C.z - A.z};

        double[] crossProduct = {//
                /*    */rBA[1] * rCA[2] - rBA[2] * rCA[1], //
                -1 * (rBA[0] * rCA[2] - rBA[2] * rCA[0]), //
                rBA[0] * rCA[1] - rBA[1] * rCA[0] //
        };

        return crossProduct[2] > 0 ? 1 : -1;
    }

    /**
     * Get the 3d centroid {@link Coordinate} of a triangle.
     * 
     * @param A coordinate 1.
     * @param B coordinate 2.
     * @param C coordinate 3.
     * @return the centroid coordinate.
     */
    public static Coordinate getTriangleCentroid( Coordinate A, Coordinate B, Coordinate C ) {
        double cx = (A.x + B.x + C.x) / 3.0;
        double cy = (A.y + B.y + C.y) / 3.0;
        double cz = (A.z + B.z + C.z) / 3.0;
        return new Coordinate(cx, cy, cz);
    }

    /**
     * Scales a {@link Polygon} to have an unitary area.
     * 
     * @param polygon the geometry to scale.
     * @return a copy of the scaled geometry.
     * @throws Exception 
     */
    public static Geometry scaleToUnitaryArea( Geometry polygon ) throws Exception {
        double area = polygon.getArea();
        double scale = sqrt(1.0 / area);
        AffineTransform scaleAT = new AffineTransform();
        scaleAT.scale(scale, scale);
        AffineTransform2D scaleTransform = new AffineTransform2D(scaleAT);
        polygon = JTS.transform(polygon, scaleTransform);
        return polygon;
    }

    /**
     * Tries to merge multilines when they are snapped properly.
     * 
     * @param multiLines the lines to merge.
     * @return the list of lines, ideally containing a single line,merged. 
     */
    @SuppressWarnings("unchecked")
    public static List<LineString> mergeLinestrings( List<LineString> multiLines ) {
        LineMerger lineMerger = new LineMerger();
        for( int i = 0; i < multiLines.size(); i++ ) {
            Geometry line = multiLines.get(i);
            lineMerger.add(line);
        }
        Collection<Geometry> merged = lineMerger.getMergedLineStrings();
        List<LineString> mergedList = new ArrayList<>();
        for( Geometry geom : merged ) {
            mergedList.add((LineString) geom);
        }
        return mergedList;
    }

    /**
     * Get the position of a point (left, right, on line) for a given line.
     * 
     * @param point the point to check.
     * @param lineStart the start coordinate of the line.
     * @param lineEnd the end coordinate of the line.
     * @return 1 if the point is left of the line, -1 if it is right, 0 if it is on the line.
     */
    public static int getPointPositionAgainstLine( Coordinate point, Coordinate lineStart, Coordinate lineEnd ) {
        double value = (lineEnd.x - lineStart.x) * (point.y - lineStart.y) - (point.x - lineStart.x) * (lineEnd.y - lineStart.y);
        if (value > 0) {
            return 1;
        } else if (value < 0) {
            return -1;
        } else {
            return 0;
        }
    }

    /**
     * Creates simple arrow polygons in the direction of the coordinates.
     * 
     * @param geometries the geometries (lines and polygons) for which to create the arrows.
     * @return the list of polygon arrows.
     */
    public static List<Polygon> createSimpleDirectionArrow( Geometry... geometries ) {
        List<Polygon> polygons = new ArrayList<>();
        for( Geometry geometry : geometries ) {
            for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
                Geometry geometryN = geometry.getGeometryN(i);
                if (geometryN instanceof LineString) {
                    LineString line = (LineString) geometryN;
                    polygons.addAll(makeArrows(line));
                } else if (geometryN instanceof Polygon) {
                    Polygon polygonGeom = (Polygon) geometryN;
                    LineString exteriorRing = polygonGeom.getExteriorRing();
                    polygons.addAll(makeArrows(exteriorRing));
                    int numInteriorRing = polygonGeom.getNumInteriorRing();
                    for( int j = 0; j < numInteriorRing; j++ ) {
                        LineString interiorRingN = polygonGeom.getInteriorRingN(j);
                        polygons.addAll(makeArrows(interiorRingN));
                    }
                }
            }
        }
        return polygons;
    }

    private static List<Polygon> makeArrows( LineString line ) {
        List<Polygon> polygons = new ArrayList<>();
        Coordinate[] coordinates = line.getCoordinates();
        for( int i = 0; i < coordinates.length - 1; i++ ) {
            LineSegment ls = new LineSegment(coordinates[i], coordinates[i + 1]);
            double length = ls.getLength();
            double delta = length / 10.0;
            Coordinate c1 = ls.pointAlongOffset(0.3, delta);
            Coordinate c2 = ls.pointAlongOffset(0.3, -delta);
            Coordinate c3 = ls.pointAlong(0.7);
            Polygon polygon = gf().createPolygon(new Coordinate[]{c1, c2, c3, c1});
            polygons.add(polygon);
        }
        return polygons;
    }

}
