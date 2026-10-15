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
package org.hortonmachine.gears;

import java.util.Arrays;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.geometry.EPrecision;
import org.hortonmachine.gears.utils.geometry.HMPrecision;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.TopologyException;
import org.locationtech.jts.io.WKTReader;

/**
 * Test {@link HMPrecision}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestHMPrecision extends HMTestCase {

    private static final WKTReader WKT = new WKTReader();

    public void testScales() throws Exception {
        CoordinateReferenceSystem utm = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632");
        assertEquals(1000.0, new HMPrecision(EPrecision.MM, utm).getScale());
        assertEquals(100.0, new HMPrecision(EPrecision.CM, utm).getScale());
        assertEquals(10.0, new HMPrecision(EPrecision.DM, utm).getScale());
        assertEquals(1.0, new HMPrecision(EPrecision.M, utm).getScale());
        assertEquals(0.01, new HMPrecision(EPrecision.CM, utm).getGridSize(), DELTA);

        // in degrees, the power of ten nearest to the size at the equator
        CoordinateReferenceSystem wgs84 = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326");
        assertEquals(1e8, new HMPrecision(EPrecision.MM, wgs84).getScale());
        assertEquals(1e7, new HMPrecision(EPrecision.CM, wgs84).getScale());
        assertEquals(1e6, new HMPrecision(EPrecision.DM, wgs84).getScale());
        assertEquals(1e5, new HMPrecision(EPrecision.M, wgs84).getScale());

        // US survey feet: a mm is 0.0033 feet, nearer to 0.001 than to 0.01, a m 3.28 feet, nearer to 1
        CoordinateReferenceSystem feet = HMCrsRegistry.INSTANCE.getCrs("EPSG:2249");
        assertEquals(0.3048, HMPrecision.metersPerUnit(feet), 0.0001);
        assertEquals(1000.0, new HMPrecision(EPrecision.MM, feet).getScale());
        assertEquals(1.0, new HMPrecision(EPrecision.M, feet).getScale());

        // no crs is metric
        assertEquals(1000.0, new HMPrecision(EPrecision.MM, null).getScale());

        assertEquals(EPrecision.CM, EPrecision.forName(" cm"));
        assertEquals(EPrecision.M, EPrecision.forName("M"));
        try {
            EPrecision.forName("km");
            fail("Not a precision.");
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().contains("km"));
        }
    }

    /**
     * Two valid polygons on which the floating point overlays of JTS fail: a triangle with a segment
     * that crosses the segment of a hole very near its end (https://trac.osgeo.org/postgis/ticket/2055,
     * from the JTS robustness tests).
     */
    public void testFloatingPointFailure() throws Exception {
        Geometry a = WKT.read("POLYGON ((184315 319325, 184315 319290, 184276 319290, 184276 319325, 184315 319325), "
                + "(184311.5625999868 319321, 184304.75 319308.59379998595, 184295.64059999958 319292.90599998087, "
                + "184304.75 319308.5937999822, 184311.5625999868 319321))");
        Geometry b = WKT.read("POLYGON ((184299.5210452684 319312.2400717032, 184311.5626000017 319321, "
                + "184306.59797855775 319312.2400717032, 184299.5210452684 319312.2400717032))");
        assertTrue(a.isValid());
        assertTrue(b.isValid());

        try {
            a.intersection(b);
            fail("The floating point intersection fails.");
        } catch (TopologyException e) {
            // expected
        }
        try {
            a.union(b);
            fail("The floating point union fails.");
        } catch (TopologyException e) {
            // expected
        }
        try {
            a.difference(b);
            fail("The floating point difference fails.");
        } catch (TopologyException e) {
            // expected
        }

        HMPrecision mm = new HMPrecision(EPrecision.MM, HMCrsRegistry.INSTANCE.getCrs("EPSG:32632"));
        Geometry intersection = mm.intersection(a, b);
        Geometry union = mm.union(a, b);
        Geometry difference = mm.difference(a, b);
        assertTrue(intersection.isValid());
        assertTrue(union.isValid());
        assertTrue(difference.isValid());
        // the areas add up, but for the rounding to the mm
        double tolerance = 0.01;
        assertEquals(31.0, intersection.getArea(), tolerance);
        assertEquals(a.getArea(), intersection.getArea() + difference.getArea(), tolerance);
        assertEquals(a.getArea() + b.getArea() - intersection.getArea(), union.getArea(), tolerance);
    }

    public void testOverlays() throws Exception {
        HMPrecision mm = new HMPrecision(EPrecision.MM, null);
        // two squares whose shared border is digitized twice, a tenth of a mm apart
        Geometry a = WKT.read("POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0))");
        Geometry b = WKT.read("POLYGON ((10.0001 0, 20 0, 20 10, 10.0001 10, 10.0001 0))");

        // without precision, a sliver of a gap stays
        assertEquals(2, a.union(b).getNumGeometries());
        Geometry union = mm.union(a, b);
        assertTrue(union instanceof Polygon);
        assertEquals(200.0, union.getArea(), DELTA);
        assertEquals(union.getArea(), mm.union(Arrays.asList(a, b)).getArea(), DELTA);
        assertEquals(union.getArea(), mm.union(a.getFactory().createGeometryCollection(new Geometry[]{a, b})).getArea(),
                DELTA);

        // the overlapping parts
        Geometry c = WKT.read("POLYGON ((5 5, 15 5, 15 15, 5 15, 5 5))");
        assertEquals(25.0, mm.intersection(a, c).getArea(), DELTA);
        assertEquals(75.0, mm.difference(a, c).getArea(), DELTA);
        assertEquals(150.0, mm.symDifference(a, c).getArea(), DELTA);

        // the coordinates of the results are on the grid
        Geometry reduced = mm.reduce(WKT.read("POLYGON ((0.00012 0, 10.0004 0, 10 10.0006, 0 10, 0.00012 0))"));
        assertEquals(0.0, reduced.getEnvelopeInternal().getMinX());
        assertEquals(10.0, reduced.getEnvelopeInternal().getMaxX());
        assertEquals(10.001, reduced.getEnvelopeInternal().getMaxY(), DELTA);
    }
}
