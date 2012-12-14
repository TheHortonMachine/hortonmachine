///*
// * This file is part of JGrasstools (http://www.jgrasstools.org)
// * (C) HydroloGIS - www.hydrologis.com 
// * 
// * JGrasstools is free software: you can redistribute it and/or modify
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
//package org.jgrasstools.gears.modules;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.jgrasstools.gears.io.rasterreader.RasterReader;
//import org.jgrasstools.gears.io.rasterwriter.RasterWriter;
//import org.jgrasstools.gears.io.vectorreader.VectorReader;
//import org.jgrasstools.gears.modules.r.bobthebuilder.BobTheBuilder;
//import org.jgrasstools.gears.utils.HMTestCase;
//
///**
// * Test for {@link BobTheBuilder}
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
//        GridCoverage2D inRaster = RasterReader.readRaster(dtm);
//        SimpleFeatureCollection inArea = VectorReader.readVector(area);
//        SimpleFeatureCollection inPoints = VectorReader.readVector(points);
//
//        BobTheBuilder bob = new BobTheBuilder();
//        bob.pm = pm;
//        bob.inRaster = inRaster;
//        bob.inArea = inArea;
//        bob.inElevations = inPoints;
//        bob.doErode = true;
//        bob.fElevation = "elev";
//        bob.process();
//        GridCoverage2D outRaster = bob.outRaster;
//
//        RasterWriter.writeRaster(outdtm, outRaster);
//    }
//
//}
