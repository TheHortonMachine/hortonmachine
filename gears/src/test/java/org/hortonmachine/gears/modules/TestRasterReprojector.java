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

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.PrintUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

///**
// * Test for {@link OmsRasterReprojector}.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestRasterReprojector extends HMTestCase {
//    public void testRasterReprojector() throws Exception {
//
//        double[][] inData = HMTestMaps.mapData;
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);
//
//        OmsRasterReprojector reprojector = new OmsRasterReprojector();
//        reprojector.inRaster = inCoverage;
//        reprojector.pCode = "EPSG:4326";
//        reprojector.process();
//        GridCoverage2D outGeodata = reprojector.outRaster;
//        PrintUtilities.printCoverageDataAsMatrix(outGeodata);
//
//        checkMatrixEqual(outGeodata.getRenderedImage(), HMTestMaps.mapData4326);
//
//        reprojector = new OmsRasterReprojector();
//        reprojector.inRaster = outGeodata;
//        reprojector.pCode = "EPSG:32632";
//        reprojector.process();
//        outGeodata = reprojector.outRaster;
//
//        PrintUtilities.printCoverageDataAsMatrix(outGeodata);
//
//        checkMatrixEqual(outGeodata.getRenderedImage(), HMTestMaps.mapData);
//    }
//
//}
