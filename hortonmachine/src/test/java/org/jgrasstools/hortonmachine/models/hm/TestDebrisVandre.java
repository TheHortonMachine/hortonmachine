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
//package org.jgrasstools.hortonmachine.models.hm;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.jgrasstools.gears.io.rasterreader.RasterReader;
//import org.jgrasstools.gears.io.rasterwriter.RasterWriter;
//import org.jgrasstools.gears.io.vectorwriter.VectorWriter;
//import org.jgrasstools.gears.utils.HMTestCase;
//import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.debrisvandre.OmsDebrisVandre;
//@SuppressWarnings("nls")
//public class TestDebrisVandre extends HMTestCase {
//    public void testVandre() throws Exception {
//
//
//        GridCoverage2D pit = RasterReader.readRaster("pit");
//        GridCoverage2D netflow = RasterReader.readRaster("mflow");
//        GridCoverage2D slope = RasterReader.readRaster("slope_grad");
//        GridCoverage2D triggers = RasterReader.readRaster("triggers");
//        GridCoverage2D soil = RasterReader.readRaster("soilthick");
////        SimpleFeatureCollection obstacles = VectorReader.readVector("");
//
//        OmsDebrisVandre v = new OmsDebrisVandre();
//        v.inElev = pit;
//        v.inFlow = netflow;
//        v.inSlope = slope;
//        v.inTriggers = triggers;
//        v.inSoil = soil;
////        v.inObstacles = obstacles;
////        v.pMode= 1;
//        v.process();
//        SimpleFeatureCollection outPaths = v.outPaths;
//        SimpleFeatureCollection outIndexedTriggers = v.outIndexedTriggers;
//        GridCoverage2D outSoil = v.outSoil;
//        
//        VectorWriter.writeVector("triggers.shp", outIndexedTriggers);
//        VectorWriter.writeVector("paths.shp", outPaths);
//        RasterWriter.writeRaster("basin_tr30_cumulated", outSoil);
//    }
//}
