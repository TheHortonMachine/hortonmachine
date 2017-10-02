package org.hortonmachine.gears.modules;
///*
// * This file is part of HortonMachine (http://www.hortonmachine.org)
// * (C) HydroloGIS - www.hydrologis.com 
// * 
// * The HortonMachine is free software: you can redistribute it and/or modify
// * it under the terms of the GNU General Public License as published by
// * the Free Software Foundation, either version 3 of the License, or
// * (at your option) any later version.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program.  If not, see <http://www.gnu.org/licenses/>.
// */
//package org.hortonmachine.gears.modules;
//
//import java.util.HashMap;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.geotools.data.simple.SimpleFeatureIterator;
//import org.hortonmachine.gears.modules.v.contoursextractor.OmsContourExtractor;
//import org.hortonmachine.gears.utils.HMTestCase;
//import org.hortonmachine.gears.utils.HMTestMaps;
//import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//
//import com.vividsolutions.jts.geom.Geometry;
//
///**
// * Test for the {@link OmsContourExtractor}.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestContourExtractor extends HMTestCase {
//    public void testCountourExtractor() throws Exception {
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        double[][] netData = HMTestMaps.contourExtractorData;
//        GridCoverage2D cedCoverage = CoverageUtilities.buildCoverage("ced", netData, envelopeParams, crs, true);
//
//        OmsContourExtractor extractor = new OmsContourExtractor();
//        extractor.inCoverage = cedCoverage;
//        extractor.pMax = 95.0;
//        extractor.pMin = 90.0;
//        extractor.pInterval = 5.0;
//        extractor.process();
//
//        SimpleFeatureCollection contours = extractor.outGeodata;
//        assertTrue(contours.size() == 2);
//
//        SimpleFeatureIterator featureIterator = contours.features();
//        Geometry geometry = (Geometry) featureIterator.next().getDefaultGeometry();
//        System.out.println(geometry.toText());
//        geometry = (Geometry) featureIterator.next().getDefaultGeometry();
//        System.out.println(geometry.toText());
//    }
//
//}
