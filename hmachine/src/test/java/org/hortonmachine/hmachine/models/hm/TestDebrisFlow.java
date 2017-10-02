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
//import org.hortonmachine.gears.utils.HMTestCase;
//import org.hortonmachine.hmachine.modules.hydrogeomorphology.debrisflow.OmsDebrisFlow;
///**
// * Test for the {@link OmsDebrisFlow} module.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestDebrisFlow extends HMTestCase {
//    public void testDebrisTrigger() throws Exception {
//
//        int m = 200;
//        int c = 50;
//        for( int i = 0; i < 10; i++ ) {
//            String inRasterPath = "";
//            String flowPath = "";
//            String depoPath = "";
//            GridCoverage2D elev = OmsRasterReader.readRaster(inRasterPath);
//            OmsDebrisFlow dt = new OmsDebrisFlow();
//            dt.inElev = elev;
//            dt.pMontecarlo = m;
//            dt.pMcoeff = c;
//            dt.pVolume = 25000;
//            dt.pEasting = 624826.2537;
//            dt.pNorthing = 5133433.7523;
//            dt.pm = pm;
//            dt.process();
//            GridCoverage2D outMcs = dt.outMcs;
//            GridCoverage2D outDepo = dt.outDepo;
//            OmsRasterWriter.writeRaster(flowPath, outMcs);
//            OmsRasterWriter.writeRaster(depoPath, outDepo);
//        }
//    }
//}
