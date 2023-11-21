package org.hortonmachine.gears.utils.geometry;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.overlay.snap.GeometrySnapper;
import org.locationtech.jts.precision.CoordinatePrecisionReducerFilter;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

/**
 * Perform operations on polygon for better use in geospatial analysis:
 * - removing duplicate points
 * - reduce precision
 * - apply a scale factor
 * - simplifying the geometry
 * - removing self-intersections
 * - removing micro polygons
 * - snapping the vetrices to a given grid
 */
public class PolygonHelper {
    private Geometry newPolygon;

    private boolean selfintersectionCleaned = false;

    public PolygonHelper(Geometry polygon) {
        newPolygon = new GeometryFactory().createGeometry(polygon);
    }

    /**
     * Remove duplicate points from the polygon.
     * 
     * This is done by applying the Douglas-Peucker algorithm with a tolerance of 0.
     * A side effect is the cleaning up of self intersections. 
     */
    public void removeDuplicatePoints() {
        newPolygon = DouglasPeuckerSimplifier.simplify(newPolygon, 0);
        selfintersectionCleaned = true;
    }

    /**
     * Apply a scale factor to the polygon.
     * 
     * <p>This can be handy if values are very small as in 4326 coordinates and 
     * operations result in topologyexceptions.
     * 
     * <p>Note that to gain the same result when inverting the operation, a 
     * precision reduction might be necessary before scaling back.
     * 
     * @param scaleFactor the scale factor to apply to each coordinate of the polygon.
     */
    public void applyScaleFactor(double scaleFactor) {
        newPolygon.apply(new CoordinateSequenceFilter() {
            public void filter(CoordinateSequence seq, int i) {
                seq.setOrdinate(i, 0, seq.getOrdinate(i, 0) * scaleFactor);
                seq.setOrdinate(i, 1, seq.getOrdinate(i, 1) * scaleFactor);
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public boolean isGeometryChanged() {
                return false;
            }
        });
    }

    /**
     * Reduce the precision of the polygon.
     * 
     * @param precisionScale the precision scale to apply to each coordinate of the polygon. 1000 
     *                     leads to 3 digits after the decimal point.
     */
    public void reducePrecision(double precisionScale) {
        newPolygon.apply(new CoordinatePrecisionReducerFilter(new PrecisionModel(precisionScale)));
    }

    /**
     * Simplify the polygon using a topology preserving simplifier.
     * 
     * @param simplifyTolerance the distance tolerance to use for the simplification.
     *                      The points will be lying within this tolerance from the original geometry.
     */
    public void simplifyGeometry(double simplifyTolerance) {
        newPolygon = TopologyPreservingSimplifier.simplify(newPolygon, simplifyTolerance);
    }

    /**
     * Remove self-intersections from the polygon.
     * 
     * <p>This is done by applying a buffer of 0.
     */
    public void removeSelfIntersections() {
        if(!newPolygon.isValid() && !selfintersectionCleaned) {
            newPolygon = newPolygon.buffer(0);
        }
    }

    /**
     * Remove small polygons or internal rings based on a length tolerance.
     * 
     * @param lengthTolerance the length below which geometries are being removed.
     * @param onlyInternalRings if true, only internal rings are removed, otherwise
     *                          the whole polygon is removed if it is too small.
     */
    public void removeSmallRings(double lengthTolerance, boolean onlyInternalRings) {
        /// loop over any polygon of the geometry
        List<Polygon> keepPolygons = new ArrayList<>();
        for (int i = 0; i < newPolygon.getNumGeometries(); i++) {
            Polygon polygonN = (Polygon) newPolygon.getGeometryN(i);
            /// loop over the rings of the polygon
            List<LinearRing> keepRings = new ArrayList<>();
            for (int j = 0; j < polygonN.getNumInteriorRing(); j++) {
                LinearRing ring = polygonN.getInteriorRingN(j);
                /// if the ring is too small, remove it
                if (ring.getLength() > lengthTolerance) {
                    keepRings.add(ring);
                }
            }

            if (onlyInternalRings || polygonN.getLength() > lengthTolerance) {
                polygonN = new GeometryFactory().createPolygon(polygonN.getExteriorRing(),
                        keepRings.toArray(new LinearRing[0]));
                keepPolygons.add(polygonN);
            }
        }
        if (newPolygon.getNumGeometries() > 1) {
            newPolygon = new GeometryFactory().createMultiPolygon(keepPolygons.toArray(new Polygon[0]));
        } else {
            newPolygon = keepPolygons.get(0);
        }
    }
    
    /**
     * Remove small internal rings based on a percentage of the parent polygon area.
     * 
     * @param areaPercentageWithParent the percentage of the parent polygon area below which
     *                                the internal ring is removed.
     */
    public void removeSmallInternalRings(double areaPercentageWithParent) {
        /// loop over any polygon of the geometry
        List<Polygon> keepPolygons = new ArrayList<>();
        for (int i = 0; i < newPolygon.getNumGeometries(); i++) {
            Polygon polygonN = (Polygon) newPolygon.getGeometryN(i);
            double area = polygonN.getArea();
            /// loop over the rings of the polygon
            List<LinearRing> keepRings = new ArrayList<>();
            for (int j = 0; j < polygonN.getNumInteriorRing(); j++) {
                LinearRing ring = polygonN.getInteriorRingN(j);
                // make polygon of it to compute area
                Polygon ringPolygon = new GeometryFactory().createPolygon(ring);
                /// if the ring is too small, remove it
                if (ringPolygon.getArea()/area >= areaPercentageWithParent) {
                    keepRings.add(ring);
                }
            }

            polygonN = new GeometryFactory().createPolygon(polygonN.getExteriorRing(),
                    keepRings.toArray(new LinearRing[0]));
            keepPolygons.add(polygonN);
        }
        if (newPolygon.getNumGeometries() > 1) {
            newPolygon = new GeometryFactory().createMultiPolygon(keepPolygons.toArray(new Polygon[0]));
        } else {
            newPolygon = keepPolygons.get(0);
        }
    }

    /**
     * Snap the vertices of the polygon to a grid.
     * 
     * @param gridVertexes the grid vertexes to snap to.
     * @param tolerance the tolerance to use for the snapping. If null, the default
     *                 tolerance of 0 is used.
     */
    public void snapToGrid(MultiPoint gridVertexes, Double tolerance) {
        double tol = 0.0;
        if(tolerance != null) {
            tol = tolerance;
        }
        GeometrySnapper gSnapper = new GeometrySnapper(newPolygon);
        newPolygon = gSnapper.snapTo(gridVertexes, tol);
    }

    /**
     * Get the created geometry at the current state of processing. 
     * 
     * <p>It is possible to call other operations after this.
     * 
     * @return the geometry at the current state of processing.
     */
    public Geometry getGeometry() {
        return newPolygon;
    }

    /**
     * Create a simple grid of points covering the geometry.
     * 
     * @param geom the geometry to cover.
     * @param step the resolution step to use for the grid.
     * @param startX the optional x coordinate of the starting point of the grid. If null, the
     *              minimum x coordinate of the geometry minus the step is used.
     * @param startY the optional y coordinate of the starting point of the grid. If null, the
     *             minimum y coordinate of the geometry minus the step is used.
     * @return  a multipoint geometry containing the grid points.
     */
    public static MultiPoint createDefaultGrid(Geometry geom, double step, Double startX, Double startY) {
        Envelope env = geom.getEnvelopeInternal();
        double minX = startX != null ? startX : env.getMinX() - step;
        double minY = startY != null ? startY : env.getMinY() - step;
        double maxX = env.getMaxX() + step;
        double maxY = env.getMaxY() + step;
        double cols = (maxX - minX) / step;
        double rows = (maxY - minY) / step;
        List<Coordinate> coords = new ArrayList<>();
        for (int r = 0; r < rows; r++) {
            double y = minY + r * step;
            for (int c = 0; c < cols; c++) {
                double x = minX + c * step;
                coords.add(new Coordinate(x, y));
            }
        }
        return new GeometryFactory().createMultiPointFromCoords(coords.toArray(new Coordinate[0]));
    }
}
