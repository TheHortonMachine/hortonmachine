package org.hortonmachine.hmachine.models.hm;
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
//package org.hortonmachine.hmachine.models.hm;
//
//import java.awt.image.RenderedImage;
//import java.util.HashMap;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
//import org.hortonmachine.hmachine.modules.network.strahler.OmsStrahler;
//import org.hortonmachine.hmachine.utils.HMTestCase;
//import org.hortonmachine.hmachine.utils.HMTestMaps;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//
///**
// * Test the {@link OmsStrahler} module.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestStrahler extends HMTestCase {
//
//    public void testStrahler() throws Exception {
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        double[][] flowData = HMTestMaps.mflowDataBorder;
//        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
//        double[][] netData = HMTestMaps.extractNet1Data;
//        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);
//
//        OmsStrahler strahler = new OmsStrahler();
//        strahler.inFlow = flowCoverage;
//        strahler.inNet = netCoverage;
//        strahler.pm = pm;
//        strahler.process();
//        GridCoverage2D outStrahler = strahler.outStrahler;
//
//        RenderedImage renderedImage = outStrahler.getRenderedImage();
//        // printImage(renderedImage);
//        checkMatrixEqual(renderedImage, HMTestMaps.strahlerData, 0.000001);
//    }
//
//}
