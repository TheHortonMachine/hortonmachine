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
//package org.jgrasstools.gears.modules;
//
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.jgrasstools.gears.io.las.LasConverter;
//import org.jgrasstools.gears.io.vectorreader.VectorReader;
//import org.jgrasstools.gears.utils.HMTestCase;
//@SuppressWarnings("nls")
//public class TestLasConverter extends HMTestCase {
//    public void testLasConverter() throws Exception {
//
//        String baseFolder = "/some/basefolder/";
//        String polygonFilter = baseFolder + "filter_polygons.shp";
//
//        String lasFile1 = baseFolder + "001059_1.las";
//        String outCsv = baseFolder + "ground.csv";
//        String outputShpfile1 = baseFolder + "points_001059_1.shp";
//
//        SimpleFeatureCollection polygons = VectorReader.readVector(polygonFilter);
//
//        LasConverter v = new LasConverter();
//        v.inFile = lasFile1;
//        v.outFile = outputShpfile1;
//        v.pCode = "EPSG:32632";
//        v.pClasses = "2";
//        // v.pImpulses = "1";
//        // v.pIndexrange = range;
//        // v.doInfo = true;
//        v.inPolygons = polygons;
//        v.pm = pm;
//        v.process();
//    }
//}
