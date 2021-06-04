package org.hortonmachine.gears.modules;
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

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.PrintUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for the {@link OmsContourExtractor}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestContourExtractor extends HMTestCase {
    public void testCountourExtractor() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        double[][] netData = HMTestMaps.contourExtractorData;
        GridCoverage2D cedCoverage = CoverageUtilities.buildCoverage("ced", netData, envelopeParams, crs, true);

        OmsContourExtractor extractor = new OmsContourExtractor();
        extractor.inCoverage = cedCoverage;
        extractor.pMax = 95.0;
        extractor.pMin = 90.0;
        extractor.pInterval = 5.0;
        extractor.process();

        SimpleFeatureCollection contours = extractor.outGeodata;
        assertTrue(contours.size() == 2);

        WKTReader r = new WKTReader();
        Geometry expected1 = r.read(
                "LINESTRING (1640918.5 5139838.5, 1640918.5 5139825, 1640914 5139816, 1640905 5139811.5, 1640695 5139811.5, 1640686 5139816, 1640681.5 5139825, 1640681.5 5139945, 1640686 5139954, 1640695 5139958.5, 1640905 5139958.5, 1640914 5139954, 1640918.5 5139945, 1640918.5 5139838.5)");
        Geometry expected2 = r.read(
                "LINESTRING (1640875 5139838.5, 1640886 5139844, 1640891.5 5139855, 1640891.5 5139915, 1640886 5139926, 1640875 5139931.5, 1640725 5139931.5, 1640714 5139926, 1640708.5 5139915, 1640708.5 5139855, 1640714 5139844, 1640725 5139838.5, 1640875 5139838.5)");

        SimpleFeatureIterator featureIterator = contours.features();
        Geometry geometry = (Geometry) featureIterator.next().getDefaultGeometry();
        assertTrue(geometry.equalsExact(expected1));
        geometry = (Geometry) featureIterator.next().getDefaultGeometry();
        assertTrue(geometry.equalsExact(expected2));
    }

}
