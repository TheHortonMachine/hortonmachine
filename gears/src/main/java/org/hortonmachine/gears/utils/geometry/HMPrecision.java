/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.utils.geometry;

import java.util.Collection;

import javax.measure.Unit;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.crs.GeographicCRS;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.operation.overlayng.OverlayNG;
import org.locationtech.jts.operation.overlayng.UnaryUnionNG;
import org.locationtech.jts.precision.GeometryPrecisionReducer;

/**
 * Overlays of geometries that are robust against the floating point arithmetic.
 *
 * <p>Two edges that are almost on top of each other, as the shared border of two parcels digitized
 * twice, make the usual overlays fail with a topology exception (non-noded intersection) or give
 * slivers. Here the overlays round the coordinates to a fixed grid while they compute (snap-rounding),
 * which makes them always succeed: what is closer than the grid becomes the same.</p>
 *
 * <p>The grid is given as a size on the ground, an {@link EPrecision}, and is turned into one of the
 * units of the coordinate reference system: in a metric one, a centimeter is a grid of 0.01. For one
 * in degrees, as EPSG:4326, it is the power of ten of the degrees nearest to the size at the equator,
 * where a degree is about 111 km: a millimeter becomes 1e-8 degrees, a centimeter 1e-7, a decimeter
 * 1e-6 and a meter 1e-5. Other linear units, as feet, are converted from the meters and rounded to
 * the nearest power of ten too: a meter becomes 1 foot.</p>
 *
 * <pre>
 * HMPrecision precision = new HMPrecision(EPrecision.MM, crs);
 * Geometry union = precision.union(a, b);
 * </pre>
 *
 * <p>The results can contain parts of a lower dimension, as the line where two polygons touch, or the
 * pieces collapsed by the rounding.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class HMPrecision {

    /** The meters of a degree at the equator, on the WGS84 ellipsoid. */
    public static final double METERS_PER_DEGREE = 111_320.0;

    private final PrecisionModel precisionModel;

    /**
     * The precision in a coordinate reference system.
     *
     * @param precision the size of the grid on the ground.
     * @param crs the coordinate reference system of the geometries. If <code>null</code>, they are
     *        taken as metric.
     */
    public HMPrecision( EPrecision precision, CoordinateReferenceSystem crs ) {
        this(scaleFor(precision.getMeters() / metersPerUnit(crs)));
    }

    /**
     * The precision of a {@link PrecisionModel} scale, as 1000 for three decimals.
     *
     * @param scale the scale: the coordinates are rounded to 1/scale.
     */
    public HMPrecision( double scale ) {
        precisionModel = new PrecisionModel(scale);
    }

    /**
     * @return the precision model, for the JTS operations that take one.
     */
    public PrecisionModel getPrecisionModel() {
        return precisionModel;
    }

    /**
     * @return the scale of the precision model: the coordinates are rounded to 1/scale.
     */
    public double getScale() {
        return precisionModel.getScale();
    }

    /**
     * @return the size of the grid, in the units of the coordinate reference system.
     */
    public double getGridSize() {
        return precisionModel.gridSize();
    }

    /**
     * Rounds the coordinates of a geometry to the grid, keeping it valid.
     *
     * <p>The parts of a polygon narrower than the grid are removed.</p>
     *
     * @param geometry the geometry.
     * @return the rounded geometry.
     */
    public Geometry reduce( Geometry geometry ) {
        return GeometryPrecisionReducer.reduce(geometry, precisionModel);
    }

    /**
     * @param a a geometry.
     * @param b another geometry.
     * @return the union of the two.
     */
    public Geometry union( Geometry a, Geometry b ) {
        return OverlayNG.overlay(a, b, OverlayNG.UNION, precisionModel);
    }

    /**
     * @param a a geometry.
     * @param b another geometry.
     * @return the part of a that is not in b.
     */
    public Geometry difference( Geometry a, Geometry b ) {
        return OverlayNG.overlay(a, b, OverlayNG.DIFFERENCE, precisionModel);
    }

    /**
     * @param a a geometry.
     * @param b another geometry.
     * @return the part shared by the two.
     */
    public Geometry intersection( Geometry a, Geometry b ) {
        return OverlayNG.overlay(a, b, OverlayNG.INTERSECTION, precisionModel);
    }

    /**
     * @param a a geometry.
     * @param b another geometry.
     * @return the parts that are in only one of the two.
     */
    public Geometry symDifference( Geometry a, Geometry b ) {
        return OverlayNG.overlay(a, b, OverlayNG.SYMDIFFERENCE, precisionModel);
    }

    /**
     * Merges the parts of a geometry, as the polygons of a collection that overlap.
     *
     * @param geometry the geometry.
     * @return the union of its parts.
     */
    public Geometry union( Geometry geometry ) {
        return UnaryUnionNG.union(geometry, precisionModel);
    }

    /**
     * Merges geometries.
     *
     * @param geometries the geometries, not empty.
     * @return the union of all of them.
     */
    public Geometry union( Collection< ? extends Geometry> geometries ) {
        GeometryFactory gf = geometries.iterator().next().getFactory();
        return UnaryUnionNG.union(gf.buildGeometry(geometries), precisionModel);
    }

    @Override
    public String toString() {
        return "HMPrecision[grid " + getGridSize() + ", scale " + getScale() + "]";
    }

    /**
     * The meters of a unit of the coordinate reference system.
     *
     * @param crs the coordinate reference system, or <code>null</code> for a metric one.
     * @return the meters of a unit: {@link #METERS_PER_DEGREE} for one in degrees.
     */
    public static double metersPerUnit( CoordinateReferenceSystem crs ) {
        if (crs == null) {
            return 1.0;
        }
        if (crs instanceof GeographicCRS) {
            return METERS_PER_DEGREE;
        }
        Unit< ? > unit = crs.getCoordinateSystem().getAxis(0).getUnit();
        if (unit == null || !"m".equals(unit.getSystemUnit().toString())) {
            return 1.0;
        }
        try {
            return unit.getConverterToAny(unit.getSystemUnit()).convert(1.0);
        } catch (Exception e) {
            return 1.0;
        }
    }

    /**
     * @param gridSize the size of the grid.
     * @return the scale of the power of ten nearest to the grid.
     */
    private static double scaleFor( double gridSize ) {
        int exponent = (int) Math.floor(Math.log10(gridSize));
        double lower = Math.pow(10, exponent);
        if (10 * lower - gridSize < gridSize - lower) {
            exponent++;
        }
        // from the exponent, to not get 999.9999999999999 for a mm
        return Math.pow(10, -exponent);
    }
}
