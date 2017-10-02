package org.hortonmachine.gears.modules;
//package org.hortonmachine.gears.modules;
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
//
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
//import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
//import org.hortonmachine.gears.modules.v.grids.OmsGridsGenerator;
//import org.hortonmachine.gears.utils.HMTestCase;
//
///**
// * Test for {@link OmsGridsGenerator}
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestGridGenerator extends HMTestCase {
//
//    public void testGridGenerator() throws Exception {
//        String inVector = "/home/moovida/geologico2012/geologico_2012_data/THEBIG_PROCESSING/pl_10714_bosco.shp";
//        String outVector = "/home/moovida/geologico2012/geologico_2012_data/THEBIG_PROCESSING/divisione5x5.shp";
//
//        SimpleFeatureCollection inFC = OmsVectorReader.readVector(inVector);
//
//        OmsGridsGenerator grid = new OmsGridsGenerator();
//        grid.pm = pm;
//        grid.inVector = inFC;
//
//        grid.pCols = 5;
//        grid.pRows = 5;
//
//        grid.pType = 0;
//        grid.process();
//        SimpleFeatureCollection outMap = grid.outMap;
//        OmsVectorWriter.writeVector(outVector, outMap);
//
//        // OmsGridsGenerator grid = new OmsGridsGenerator();
//        // grid.pm = pm;
//        // grid.pCode = "EPSG:32632";
//        //
//        // double w = 653000.0;
//        // double s = 5100000.0;
//        // grid.pLon = w;
//        // grid.pLat = s;
//        // grid.pWidth = 20.0;
//        // grid.pHeight = 10.0;
//        // grid.pCols = 50;
//        // grid.pRows = 100;
//        // grid.pSpacing = 5.0;
//        // grid.pType = 2;
//        // grid.process();
//        // SimpleFeatureCollection outMap = grid.outMap;
//
//    }
//
//}
