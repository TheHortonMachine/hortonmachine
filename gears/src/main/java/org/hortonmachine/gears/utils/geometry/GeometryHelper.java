package org.hortonmachine.gears.utils.geometry;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateArrays;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.overlay.snap.GeometrySnapper;
import org.locationtech.jts.precision.CoordinatePrecisionReducerFilter;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;

/**
 * Perform operations on geometries for better use in geospatial analysis:
 * - removing duplicate points
 * - reduce precision
 * - apply a scale factor
 * - simplifying the geometry
 * - removing self-intersections
 * - removing micro polygons
 * - snapping the vetrices to a given grid
 * 
 */
public class GeometryHelper {
    private Geometry newGeometry;

    private boolean selfintersectionCleaned = false;

    private GeometryFactory gf = new GeometryFactory();

    public GeometryHelper(Geometry geometry) {
        newGeometry = gf.createGeometry(geometry);
    }

    public GeometryHelper(Geometry geometry, GeometryFactory geomFactory) {
        gf = geomFactory;
        newGeometry = gf.createGeometry(geometry);
    }

    /**
     * Remove duplicate points from the geometry.
     * 
     * This is done by applying the Douglas-Peucker algorithm with a tolerance of 0.
     * A side effect is the cleaning up of self intersections, if the geometry is a
     * Polygon.
     */
    public void removeDuplicatePoints() {
        if (EGeometryType.isPolygon(newGeometry)) {
            newGeometry = DouglasPeuckerSimplifier.simplify(newGeometry, 0);
            selfintersectionCleaned = true;
        } else if (EGeometryType.isLine(newGeometry)) {
            newGeometry = DouglasPeuckerSimplifier.simplify(newGeometry, 0);
        } else if (EGeometryType.isPoint(newGeometry)) {
            int num = newGeometry.getNumGeometries();
            if (num > 1) {
                Coordinate[] singleCoords = CoordinateArrays.removeRepeatedPoints(newGeometry.getCoordinates());
                newGeometry = gf.createMultiPointFromCoords(singleCoords);
            } else {
                // do nothing
            }
        }
    }

    /**
     * Apply a scale factor to the polygon.
     * 
     * <p>
     * This can be handy if values are very small as in 4326 coordinates and
     * operations result in topologyexceptions.
     * 
     * <p>
     * Note that to gain the same result when inverting the operation, a
     * precision reduction might be necessary before scaling back.
     * 
     * @param scaleFactor the scale factor to apply to each coordinate of the
     *                    polygon.
     */
    public void applyScaleFactor(double scaleFactor) {
        newGeometry.apply(new CoordinateSequenceFilter() {
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
                return true;
            }
        });
    }

    /**
     * Reduce the precision of the polygon.
     * 
     * @param precisionScale the precision scale to apply to each coordinate of the
     *                       polygon. 1000
     *                       leads to 3 digits after the decimal point.
     */
    public void reducePrecision(double precisionScale) {
        newGeometry.apply(new CoordinatePrecisionReducerFilter(new PrecisionModel(precisionScale)));
    }

    /**
     * Simplify the geometry using a topology preserving simplifier.
     * 
     * <p>
     * Simplification is done only for polygonal and linear geometries.
     * 
     * @param simplifyTolerance the distance tolerance to use for the
     *                          simplification.
     *                          The points will be lying within this tolerance from
     *                          the original geometry.
     */
    public void simplifyGeometry(double simplifyTolerance) {
        if (EGeometryType.isPoint(newGeometry)) {
            return;
        }
        newGeometry = TopologyPreservingSimplifier.simplify(newGeometry, simplifyTolerance);
    }

    /**
     * Remove self-intersections from the geometry.
     * 
     * <p>
     * This is done by applying a buffer of 0.
     * <p>
     * This does nothing if the geometry is not a polygonal one.
     */
    public void removeSelfIntersections() {
        if (EGeometryType.isPolygon(newGeometry) && (!newGeometry.isValid() || !newGeometry.isSimple())
                && !selfintersectionCleaned) {
            newGeometry = newGeometry.buffer(0);
        }
    }

    /**
     * Remove small polygons or internal rings based on a length tolerance.
     * 
     * <p>
     * This does nothing if the geometry is not a polygonal one.
     * 
     * @param lengthTolerance   the length below which geometries are being removed.
     * @param onlyInternalRings if true, only internal rings are removed, otherwise
     *                          the whole polygon is removed if it is too small.
     */
    public void removeSmallRings(double lengthTolerance, boolean onlyInternalRings) {
        if (!EGeometryType.isPolygon(newGeometry)) {
            return;
        }
        /// loop over any polygon of the geometry
        List<Polygon> keepPolygons = new ArrayList<>();
        for (int i = 0; i < newGeometry.getNumGeometries(); i++) {
            Polygon polygonN = (Polygon) newGeometry.getGeometryN(i);
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
                polygonN = gf.createPolygon(polygonN.getExteriorRing(),
                        keepRings.toArray(new LinearRing[0]));
                keepPolygons.add(polygonN);
            }
        }
        if (newGeometry.getNumGeometries() > 1) {
            newGeometry = gf.createMultiPolygon(keepPolygons.toArray(new Polygon[0]));
        } else {
            newGeometry = keepPolygons.get(0);
        }
    }

    /**
     * Remove small internal rings based on a percentage of the parent polygon area.
     * 
     * <p>
     * This does nothing if the geometry is not a polygonal one.
     * 
     * @param areaPercentageWithParent the percentage of the parent polygon area
     *                                 below which
     *                                 the internal ring is removed.
     */
    public void removeSmallInternalRings(double areaPercentageWithParent) {
        if (!EGeometryType.isPolygon(newGeometry)) {
            return;
        }
        /// loop over any polygon of the geometry
        List<Polygon> keepPolygons = new ArrayList<>();
        for (int i = 0; i < newGeometry.getNumGeometries(); i++) {
            Polygon polygonN = (Polygon) newGeometry.getGeometryN(i);
            double area = polygonN.getArea();
            /// loop over the rings of the polygon
            List<LinearRing> keepRings = new ArrayList<>();
            for (int j = 0; j < polygonN.getNumInteriorRing(); j++) {
                LinearRing ring = polygonN.getInteriorRingN(j);
                // make polygon of it to compute area
                Polygon ringPolygon = gf.createPolygon(ring);
                /// if the ring is too small, remove it
                if (ringPolygon.getArea() / area >= areaPercentageWithParent) {
                    keepRings.add(ring);
                }
            }

            polygonN = gf.createPolygon(polygonN.getExteriorRing(),
                    keepRings.toArray(new LinearRing[0]));
            keepPolygons.add(polygonN);
        }
        if (newGeometry.getNumGeometries() > 1) {
            newGeometry = gf.createMultiPolygon(keepPolygons.toArray(new Polygon[0]));
        } else {
            newGeometry = keepPolygons.get(0);
        }
    }

    /**
     * Snap the vertices of the geometry to a grid.
     * 
     * @param gridVertexes the grid vertexes to snap to.
     * @param tolerance    the tolerance to use for the snapping. If null, the
     *                     default
     *                     tolerance of 0 is used.
     */
    public void snapToGrid(MultiPoint gridVertexes, Double tolerance) {
        double tol = 0.0;
        if (tolerance != null) {
            tol = tolerance;
        }
        GeometrySnapper gSnapper = new GeometrySnapper(newGeometry);
        newGeometry = gSnapper.snapTo(gridVertexes, tol);
    }

    /**
     * Get the created geometry at the current state of processing.
     * 
     * <p>
     * It is possible to call other operations after this.
     * 
     * @return the geometry at the current state of processing.
     */
    public Geometry getGeometry() {
        return newGeometry;
    }

    /**
     * Create a simple grid of points covering the geometry.
     * 
     * @param geom   the geometry to cover.
     * @param step   the resolution step to use for the grid.
     * @param startX the optional x coordinate of the starting point of the grid. If
     *               null, the
     *               minimum x coordinate of the geometry minus the step is used.
     * @param startY the optional y coordinate of the starting point of the grid. If
     *               null, the
     *               minimum y coordinate of the geometry minus the step is used.
     * @return a multipoint geometry containing the grid points.
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

    /**
     * Perform an intersection between a (possibly) single polygon and a complex
     * multipolygon.
     * The operation tries to fight against all the odds of a topologyexception,
     * being it due
     * to precision, self-intersections, or other issues.
     * 
     * @param referenceGeometry the single geometry to use.
     * @param multiGeometry     the multipolygon to intersect with.
     * @param gridResolution    an optional grid resolution. If available also
     *                          snapping the vertices
     *                          to a grid with that cell size and of the bounds of
     *                          the reference
     *                          geometry is tried.
     * @return the created intersection geometry or null if no intersection is
     *         found.
     */
    public static Geometry multiPolygonIntersection(Geometry referenceGeometry, Geometry multiGeometry,
            Double gridResolution) {
        GeometryHelper referenceHelper = new GeometryHelper(referenceGeometry);
        int scaleFactor = 100000;
        int precisionScale = 100;
        referenceHelper.applyScaleFactor(scaleFactor);
        referenceHelper.reducePrecision(precisionScale);
        Geometry referenceGeometryScaled = referenceHelper.getGeometry();

        PreparedGeometry preparedReferenceGeometry = PreparedGeometryFactory.prepare(referenceGeometry);

        if (EGeometryType.isPolygon(referenceGeometry) && EGeometryType.isPolygon(multiGeometry)) {
            List<Polygon> intersectionPolygons = new ArrayList<>();
            for (int j = 0; j < multiGeometry.getNumGeometries(); j++) {
                Polygon polygonN = (Polygon) multiGeometry.getGeometryN(j);
                if (!preparedReferenceGeometry.intersects(polygonN)) {
                    continue;
                }
                Geometry intersection = null;
                try {
                    intersection = referenceGeometry.intersection(polygonN);
                } catch (Exception e) {
                    // try to use some strategies

                    // clean the geometry up
                    GeometryHelper helper = new GeometryHelper(polygonN);
                    helper.removeDuplicatePoints();
                    helper.removeSelfIntersections();
                    try {
                        intersection = referenceGeometry.intersection(helper.getGeometry());
                    } catch (Exception e1) {
                        // still not there, try to check if a different scale might work
                        Coordinate[] coords = polygonN.getCoordinates();
                        // are we talking about lat lon geometries?
                        boolean possibleLatLong = true;
                        for (Coordinate c : coords) {
                            if (c.x > 180 || c.x < -180 || c.y > 90 || c.y < -90) {
                                possibleLatLong = false;
                                break;
                            }
                        }
                        if (possibleLatLong) {
                            // change scale and reduce precision
                            helper.applyScaleFactor(scaleFactor);
                            helper.reducePrecision(precisionScale);
                            Geometry scaledPolygonN = helper.getGeometry();

                            // try now to intersect
                            try {
                                Geometry scaledIntersection = referenceGeometryScaled
                                        .intersection(scaledPolygonN);
                                // scale back
                                GeometryHelper scaledIntersectionHelper = new GeometryHelper(scaledIntersection);
                                scaledIntersectionHelper.applyScaleFactor(1.0 / scaleFactor);
                                intersection = scaledIntersectionHelper.getGeometry();
                            } catch (Exception e2) {
                                // revert scale factor
                                helper.applyScaleFactor(1 / scaleFactor);
                                // try to use simplification
                                if (gridResolution != null) {
                                    helper.simplifyGeometry(gridResolution / 2.0);
                                    Geometry simplifiedMultiGeometry = helper.getGeometry();
                                    try {
                                        Geometry simplifiedIntersection = referenceGeometryScaled
                                                .intersection(simplifiedMultiGeometry);
                                        intersection = simplifiedIntersection;
                                    } catch (Exception e3) {
                                        // ignore and hope for later strategies
                                    }

                                }
                            }
                        }
                        if (gridResolution != null && intersection == null) {
                            // as last resort try to snap to a grid first
                            Envelope refEnv = referenceGeometry.getEnvelopeInternal();
                            MultiPoint grid = GeometryHelper.createDefaultGrid(referenceGeometry, refEnv.getMinX(),
                                    refEnv.getMinY(), gridResolution);
                            helper.snapToGrid(grid, gridResolution);
                            Geometry snappedGeometry = helper.getGeometry();
                            try {
                                Geometry snappedfIntersection = referenceGeometryScaled
                                        .intersection(snappedGeometry);
                                intersection = snappedfIntersection;
                            } catch (Exception e2) {
                                // give up and print error
                                e2.printStackTrace();
                            }
                        }
                    }
                }
                if (intersection == null) {
                    continue;
                }
                // loop over geoms of the intersection
                for (int i = 0; i < intersection.getNumGeometries(); i++) {
                    Geometry geomN = intersection.getGeometryN(i);
                    if (geomN instanceof Polygon) {
                        intersectionPolygons.add((Polygon) geomN);
                    }
                }
            }

            // put together the polygons
            if (intersectionPolygons.size() > 1 || EGeometryType.forGeometry(multiGeometry).isMulti()) {
                return referenceGeometry.getFactory().createMultiPolygon(
                        intersectionPolygons.toArray(new Polygon[intersectionPolygons.size()]));
            } else if (intersectionPolygons.size() == 1) {
                return intersectionPolygons.get(0);
            } else {
                return null;
            }

        } else {
            throw new IllegalArgumentException("Only polygonal geometries are supported");
        }
    }
}
