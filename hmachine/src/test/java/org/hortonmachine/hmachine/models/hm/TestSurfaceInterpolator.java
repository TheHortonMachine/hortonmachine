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
//import org.geotools.coverage.grid.GridGeometry2D;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.hortonmachine.gears.io.gridgeometryreader.OmsGridGeometryReader;
//import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
//import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
//import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
//import org.hortonmachine.gears.modules.r.interpolation2d.OmsSurfaceInterpolator;
//import org.hortonmachine.gears.utils.HMTestCase;
//
///**
// * Test for the {@link OmsSurfaceInterpolator} module.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//@SuppressWarnings("nls")
//public class TestSurfaceInterpolator extends HMTestCase {
//    public void testThinPlateSplineInterpolator() throws Exception {
//
//        String baseFolder = "";
//
//        String inVector = baseFolder + "points_001059_1.shp";
//        String inMask = baseFolder + "mask";
//        String outRaster = baseFolder + "geo_interp_intens01";
//
//        OmsGridGeometryReader ggR = new OmsGridGeometryReader();
//        ggR.pNorth = 5105696.0;
//        ggR.pSouth = 5105560.0;
//        ggR.pWest = 675240.0;
//        ggR.pEast = 675438.0;
//        ggR.pXres = 0.1;
//        ggR.pYres = 0.1;
//        // ggR.pNorth = 5106011.0;
//        // ggR.pSouth = 5105440.0;
//        // ggR.pWest = 674979.0;
//        // ggR.pEast = 675938.0;
//        // ggR.pXres = 0.5;
//        // ggR.pYres = 0.5;
//        ggR.pCode = "EPSG:32632";
//        ggR.process();
//
//        GridGeometry2D gridGeometry2D = ggR.outGridgeom;
//
//        SimpleFeatureCollection readVector = OmsVectorReader.readVector(inVector);
//        GridCoverage2D mask = OmsRasterReader.readRaster(inMask);
//
//        OmsSurfaceInterpolator spliner = new OmsSurfaceInterpolator();
//        spliner.inVector = readVector;
//        spliner.inGrid = gridGeometry2D;
//        spliner.inMask = mask;
//        // spliner.fCat = "elev";
//        spliner.fCat = "intensity";
//        spliner.pMode = 1;
//        spliner.pMaxThreads = 7;
//        spliner.pBuffer = 6.0;
//        spliner.pm = pm;
//        spliner.process();
//
//        GridCoverage2D interpolated = spliner.outRaster;
//
//        OmsRasterWriter.writeRaster(outRaster, interpolated);
//    }
//
//    // public void testIDWInterpolator() throws Exception {
//    //
//    // String baseFolder = "";
//    //
//    // // String inVector = baseFolder + "points_001059_1.shp";
//    // String inVector = baseFolder + "points_001059_1_groundy.shp";
//    // String inMask = baseFolder + "mask";
//    // String outRaster = baseFolder +
//    // "interpolation_idw_elev_ground";
//    //
//    // OmsGridGeometryReader ggR = new OmsGridGeometryReader();
//    // ggR.pNorth = 5105696.0;
//    // ggR.pSouth = 5105560.0;
//    // ggR.pWest = 675240.0;
//    // ggR.pEast = 675438.0;
//    // ggR.pXres = 0.5;
//    // ggR.pYres = 0.5;
//    // // ggR.pNorth = 5106011.0;
//    // // ggR.pSouth = 5105440.0;
//    // // ggR.pWest = 674979.0;
//    // // ggR.pEast = 675938.0;
//    // // ggR.pXres = 0.5;
//    // // ggR.pYres = 0.5;
//    // ggR.pCode = "EPSG:32632";
//    // ggR.process();
//    //
//    // GridGeometry2D gridGeometry2D = ggR.outGridgeom;
//    //
//    // SimpleFeatureCollection readVector = OmsVectorReader.readVector(inVector);
//    // GridCoverage2D mask = OmsRasterReader.readRaster(inMask);
//    //
//    // OmsSurfaceInterpolator spliner = new OmsSurfaceInterpolator();
//    // spliner.inVector = readVector;
//    // spliner.inGrid = gridGeometry2D;
//    // spliner.inMask = mask;
//    // spliner.fCat = "elev";
//    // spliner.pMode = 1;
//    // spliner.pMaxThreads = 7;
//    // spliner.pBuffer = 15.0;
//    // spliner.pm = pm;
//    // spliner.process();
//    //
//    // GridCoverage2D interpolated = spliner.outRaster;
//    //
//    // OmsRasterWriter.writeRaster(outRaster, interpolated);
//    // }
//}
