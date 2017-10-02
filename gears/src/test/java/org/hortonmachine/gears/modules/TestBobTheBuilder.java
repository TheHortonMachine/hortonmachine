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
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
//import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
//import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
//import org.hortonmachine.gears.modules.r.bobthebuilder.OmsBobTheBuilder;
//import org.hortonmachine.gears.utils.HMTestCase;
//
///**
// * Test for {@link OmsBobTheBuilder}
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestBobTheBuilder extends HMTestCase {
//
//    public void testBobTheBuilder() throws Exception {
//        String area = "/home/moovida/data/hydrocareworkspace/grassdb/utm32n_etrf89/bobthebuilder/shapefiles/polygon.shp";
//        String points = "/home/moovida/data/hydrocareworkspace/grassdb/utm32n_etrf89/bobthebuilder/shapefiles/elevpoints.shp";
//        String dtm = "/home/moovida/data/hydrocareworkspace/grassdb/utm32n_etrf89/bobthebuilder/cell/dtm";
//        String outdtm = "/home/moovida/data/hydrocareworkspace/grassdb/utm32n_etrf89/bobthebuilder/cell/dtm_bob_erode";
//
//        GridCoverage2D inRaster = OmsRasterReader.readRaster(dtm);
//        SimpleFeatureCollection inArea = OmsVectorReader.readVector(area);
//        SimpleFeatureCollection inPoints = OmsVectorReader.readVector(points);
//
//        OmsBobTheBuilder bob = new OmsBobTheBuilder();
//        bob.pm = pm;
//        bob.inRaster = inRaster;
//        bob.inArea = inArea;
//        bob.inElevations = inPoints;
//        bob.doErode = true;
//        bob.fElevation = "elev";
//        bob.process();
//        GridCoverage2D outRaster = bob.outRaster;
//
//        OmsRasterWriter.writeRaster(outdtm, outRaster);
//    }
//
//}
