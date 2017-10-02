package org.hortonmachine.hmachine.models.hm;
///*
// * JGrass - Free Open Source Java GIS http://www.jgrass.org 
// * (C) HydroloGIS - www.hydrologis.com 
// * 
// * This library is free software; you can redistribute it and/or modify it under
// * the terms of the GNU Library General Public License as published by the Free
// * Software Foundation; either version 2 of the License, or (at your option) any
// * later version.
// * 
// * This library is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
// * details.
// * 
// * You should have received a copy of the GNU Library General Public License
// * along with this library; if not, write to the Free Foundation, Inc., 59
// * Temple Place, Suite 330, Boston, MA 02111-1307 USA
// */
//package org.hortonmachine.hmachine.models.hm;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
//import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
//import org.hortonmachine.hmachine.modules.statistics.kerneldensity.OmsKernelDensity;
//import org.hortonmachine.hmachine.utils.HMTestCase;
///**
// * Test for the {@link OmsKernelDensity} module.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestKernelDensity extends HMTestCase {
//    public void testDebrisTrigger() throws Exception {
//
//        String baseFolder = "";
//        String inRasterPath1 = baseFolder + "net_triggers";
//        String outRasterPath = baseFolder + "triggers_density_triang";
//
//        GridCoverage2D elev = OmsRasterReader.readRaster(inRasterPath1);
//
//        OmsKernelDensity dt = new OmsKernelDensity();
//        dt.inMap = elev;
//        dt.pKernel = 7;
//        dt.pRadius = 10;
//        dt.doConstant = true;
//        dt.pm = pm;
//        dt.process();
//
//        GridCoverage2D density = dt.outDensity;
//
//        OmsRasterWriter.writeRaster(outRasterPath, density);
//
//    }
//}
