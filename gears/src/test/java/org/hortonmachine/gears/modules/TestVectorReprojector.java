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
package org.hortonmachine.gears.modules;

import org.geotools.data.crs.ForceCoordinateSystemFeatureResults;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.FeatureIterator;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.crs.CrsUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * Test for the {@link OmsVectorReprojector}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorReprojector extends HMTestCase {
    private double delta = 0.0000001;

    public void testVectorReprojector() throws Exception {

        SimpleFeatureCollection testFC = HMTestMaps.getTestFC();

        OmsVectorReprojector reprojector = new OmsVectorReprojector();
        reprojector.inVector = testFC;
        reprojector.pCode = "EPSG:4326";
        reprojector.pm = pm;
        reprojector.process();

        CoordinateReferenceSystem sourceCRS = HMTestMaps.getCrs();
        // the output has the longitude first
        CoordinateReferenceSystem targetCRS = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);
        HMCrsTransformer transformer = new HMCrsTransformer(sourceCRS, targetCRS);

        SimpleFeatureCollection outFC = reprojector.outVector;
        FeatureIterator<SimpleFeature> featureIterator = outFC.features();
        SimpleFeatureIterator originalFeatureIterator = testFC.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();

            SimpleFeature originalFeature = originalFeatureIterator.next();
            Coordinate origCoord = ((Geometry) originalFeature.getDefaultGeometry()).getCoordinate();
            Coordinate reprojected = transformer.transform(origCoord);

            assertEquals(reprojected.x, coordinate.x, delta);
            assertEquals(reprojected.y, coordinate.y, delta);
        }
        featureIterator.close();

    }

    /**
     * To lat/long the longitude comes first, by default and when asked, whatever the referencing
     * hints set by the tests run before.
     */
    public void testLongitudeFirst() throws Exception {
        for( Boolean longitudeFirst : new Boolean[]{null, true} ) {
            OmsVectorReprojector reprojector = new OmsVectorReprojector();
            reprojector.inVector = HMTestMaps.getTestFC();
            reprojector.pCode = "EPSG:4326";
            reprojector.doLongitudeFirst = longitudeFirst;
            reprojector.pm = pm;
            reprojector.process();

            // the test data are in UTM 32N, around 23.6E 45.5N
            Coordinate c = ((Geometry) reprojector.outVector.features().next().getDefaultGeometry()).getCoordinate();
            assertTrue("longitudeFirst = " + longitudeFirst + ": " + c, c.x > 23 && c.x < 24);
            assertTrue("longitudeFirst = " + longitudeFirst + ": " + c, c.y > 45 && c.y < 46);
        }
    }

    /**
     * A datum without the parameters to WGS84 can be reprojected only when lenient.
     */
    public void testLenient() throws Exception {
        String wkt = "PROJCS[\"Local TM\", GEOGCS[\"Local\", DATUM[\"Local datum\", SPHEROID[\"Bessel 1841\", 6377397.155, 299.1528128]], "
                + "PRIMEM[\"Greenwich\", 0.0], UNIT[\"degree\", 0.017453292519943295], AXIS[\"Lon\", EAST], AXIS[\"Lat\", NORTH]], "
                + "PROJECTION[\"Transverse_Mercator\"], PARAMETER[\"central_meridian\", 9.0], PARAMETER[\"latitude_of_origin\", 0.0], "
                + "PARAMETER[\"scale_factor\", 0.9996], PARAMETER[\"false_easting\", 500000.0], PARAMETER[\"false_northing\", 0.0], "
                + "UNIT[\"m\", 1.0], AXIS[\"x\", EAST], AXIS[\"y\", NORTH]]";
        CoordinateReferenceSystem localCrs = CRS.parseWKT(wkt);

        OmsVectorReprojector reprojector = new OmsVectorReprojector();
        reprojector.inVector = new ForceCoordinateSystemFeatureResults(HMTestMaps.getTestFC(), localCrs);
        reprojector.pCode = "EPSG:4326";
        reprojector.doLenient = false;
        reprojector.pm = pm;
        try {
            reprojector.process();
            reprojector.outVector.features().next();
            fail("Without datum parameters the strict reprojection must fail.");
        } catch (Exception e) {
            // expected
        }

        reprojector = new OmsVectorReprojector();
        reprojector.inVector = new ForceCoordinateSystemFeatureResults(HMTestMaps.getTestFC(), localCrs);
        reprojector.pCode = "EPSG:4326";
        reprojector.pm = pm;
        reprojector.process();
        Coordinate c = ((Geometry) reprojector.outVector.features().next().getDefaultGeometry()).getCoordinate();
        assertTrue(c.toString(), c.x > 22 && c.x < 25);
    }
}
