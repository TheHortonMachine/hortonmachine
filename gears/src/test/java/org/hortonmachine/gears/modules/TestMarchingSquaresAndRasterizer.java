package org.hortonmachine.gears.modules;
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
//package org.hortonmachine.gears.modules;
//
//import java.awt.image.RenderedImage;
//import java.util.HashMap;
//
//import javax.media.jai.iterator.RectIter;
//import javax.media.jai.iterator.RectIterFactory;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.coverage.grid.GridGeometry2D;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer;
//import org.hortonmachine.gears.modules.v.marchingsquares.OmsMarchingSquaresVectorializer;
//import org.hortonmachine.gears.utils.HMTestCase;
//import org.hortonmachine.gears.utils.HMTestMaps;
//import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.*;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//
///**
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestMarchingSquaresAndRasterizer extends HMTestCase {
//
//    private void setBounds( OmsScanLineRasterizer rast, GridGeometry2D gridGeom ) {
//        HashMap<String, Double> envelopeParams = gridGeometry2RegionParamsMap(gridGeom);
//        double west = envelopeParams.get(WEST);
//        double south = envelopeParams.get(SOUTH);
//        double east = envelopeParams.get(EAST);
//        double north = envelopeParams.get(NORTH);
//        int rows = envelopeParams.get(ROWS).intValue();
//        int cols = envelopeParams.get(COLS).intValue();
//
//        rast.north = north;
//        rast.south = south;
//        rast.east = east;
//        rast.west = west;
//        rast.cols = cols;
//        rast.rows = rows;
//    }
//
//    public void testMarchingSquaresAndRasterizer1() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        /*
//         * extract vectors
//         */
//        double[][] extractNet1Data = HMTestMaps.marchingSq1;
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        GridCoverage2D netCoverage = buildCoverage("net", extractNet1Data, envelopeParams, crs, true);
//        GridCoverage2D geodata = netCoverage;
//
//        OmsMarchingSquaresVectorializer squares = new OmsMarchingSquaresVectorializer();
//        squares.inGeodata = geodata;
//        squares.pValue = 2.0;
//        squares.pm = pm;
//
//        squares.process();
//
//        SimpleFeatureCollection outGeodata = squares.outGeodata;
//
//        /*
//         * and rasterize back again
//         */
//        OmsScanLineRasterizer rasterizer = new OmsScanLineRasterizer();
//        rasterizer.inGeodata = outGeodata;
//        rasterizer.pm = pm;
//        setBounds(rasterizer, geodata.getGridGeometry());
//        rasterizer.pValue = 2.0;
//        rasterizer.process();
//
//        GridCoverage2D outGeodata2 = rasterizer.outGeodata;
//
//        RenderedImage renderedImage = outGeodata2.getRenderedImage();
//
//        // should be the same as before
//        checkMatrixEqual(renderedImage, extractNet1Data, 0);
//
//    }
//
//    public void testMarchingSquaresAndRasterizer2() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        /*
//         * extract vectors
//         */
//        double[][] extractNet1Data = HMTestMaps.marchingSq2;
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        GridCoverage2D netCoverage = buildCoverage("net", extractNet1Data, envelopeParams, crs, true);
//        GridCoverage2D geodata = netCoverage;
//
//        OmsMarchingSquaresVectorializer squares = new OmsMarchingSquaresVectorializer();
//        squares.inGeodata = geodata;
//        squares.pValue = 2.0;
//        squares.pm = pm;
//
//        squares.process();
//
//        SimpleFeatureCollection outGeodata = squares.outGeodata;
//
//        /*
//         * and rasterize back again
//         */
//        OmsScanLineRasterizer rasterizer = new OmsScanLineRasterizer();
//        rasterizer.inGeodata = outGeodata;
//        rasterizer.pm = pm;
//        setBounds(rasterizer, geodata.getGridGeometry());
//        rasterizer.pValue = 2.0;
//        rasterizer.process();
//
//        GridCoverage2D outGeodata2 = rasterizer.outGeodata;
//
//        RenderedImage renderedImage = outGeodata2.getRenderedImage();
//
//        // should be the same as before
//        checkMatrixEqual(renderedImage, extractNet1Data, 0);
//
//    }
//
//    public void testMarchingSquaresAndRasterizer3() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        /*
//         * extract vectors
//         */
//        double[][] extractData = HMTestMaps.marchingSq3;
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        GridCoverage2D netCoverage = buildCoverage("net", extractData, envelopeParams, crs, true);
//        GridCoverage2D geodata = netCoverage;
//
//        OmsMarchingSquaresVectorializer squares = new OmsMarchingSquaresVectorializer();
//        squares.inGeodata = geodata;
//        squares.pValue = 2.0;
//        squares.pm = pm;
//
//        squares.process();
//
//        SimpleFeatureCollection outGeodata = squares.outGeodata;
//
//        /*
//         * and rasterize back again
//         */
//        OmsScanLineRasterizer rasterizer = new OmsScanLineRasterizer();
//        rasterizer.inGeodata = outGeodata;
//        rasterizer.pm = pm;
//        setBounds(rasterizer, geodata.getGridGeometry());
//        rasterizer.pValue = 2.0;
//        rasterizer.process();
//
//        GridCoverage2D outGeodata2 = rasterizer.outGeodata;
//
//        RenderedImage renderedImage = outGeodata2.getRenderedImage();
//
//        RectIter iter = RectIterFactory.create(renderedImage, null);
//        do {
//            do {
//                System.out.print(iter.getSampleDouble() + " ");
//            } while( !iter.nextPixelDone() );
//            iter.startPixels();
//            System.out.println();
//        } while( !iter.nextLineDone() );
//
//        // should be the same as before
//        checkMatrixEqual(renderedImage, extractData, 0);
//
//    }
//
//    public void testMarchingSquaresAndRasterizer4() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        /*
//         * extract vectors
//         */
//        double[][] extractData = HMTestMaps.marchingSq4;
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        GridCoverage2D netCoverage = buildCoverage("net", extractData, envelopeParams, crs, true);
//        GridCoverage2D geodata = netCoverage;
//
//        OmsMarchingSquaresVectorializer squares = new OmsMarchingSquaresVectorializer();
//        squares.inGeodata = geodata;
//        squares.pValue = 2.0;
//        squares.pm = pm;
//
//        squares.process();
//
//        SimpleFeatureCollection outGeodata = squares.outGeodata;
//
//        /*
//         * and rasterize back again
//         */
//        OmsScanLineRasterizer rasterizer = new OmsScanLineRasterizer();
//        rasterizer.inGeodata = outGeodata;
//        rasterizer.pm = pm;
//        setBounds(rasterizer, geodata.getGridGeometry());
//        rasterizer.pValue = 2.0;
//        rasterizer.process();
//
//        GridCoverage2D outGeodata2 = rasterizer.outGeodata;
//
//        RenderedImage renderedImage = outGeodata2.getRenderedImage();
//
//        RectIter iter = RectIterFactory.create(renderedImage, null);
//        do {
//            do {
//                System.out.print(iter.getSampleDouble() + " ");
//            } while( !iter.nextPixelDone() );
//            iter.startPixels();
//            System.out.println();
//        } while( !iter.nextLineDone() );
//
//        // should be the same as before
//        checkMatrixEqual(renderedImage, extractData, 0);
//
//    }
//
//    public void testMarchingSquaresAndRasterizer5() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        /*
//         * extract vectors
//         */
//        double[][] extractData = HMTestMaps.marchingSq3;
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        GridCoverage2D netCoverage = buildCoverage("net", extractData, envelopeParams, crs, true);
//        GridCoverage2D geodata = netCoverage;
//
//        OmsMarchingSquaresVectorializer squares = new OmsMarchingSquaresVectorializer();
//        squares.inGeodata = geodata;
//        squares.pValue = 2.0;
//        squares.pm = pm;
//
//        squares.process();
//
//        SimpleFeatureCollection outGeodata = squares.outGeodata;
//
//        /*
//         * and rasterize back again
//         */
//        OmsScanLineRasterizer rasterizer = new OmsScanLineRasterizer();
//        rasterizer.inGeodata = outGeodata;
//        rasterizer.pm = pm;
//        setBounds(rasterizer, geodata.getGridGeometry());
//        rasterizer.pValue = 2.0;
//        rasterizer.process();
//
//        GridCoverage2D outGeodata2 = rasterizer.outGeodata;
//
//        RenderedImage renderedImage = outGeodata2.getRenderedImage();
//
//        RectIter iter = RectIterFactory.create(renderedImage, null);
//        do {
//            do {
//                System.out.print(iter.getSampleDouble() + " ");
//            } while( !iter.nextPixelDone() );
//            iter.startPixels();
//            System.out.println();
//        } while( !iter.nextLineDone() );
//
//        // should be the same as before
//        checkMatrixEqual(renderedImage, extractData, 0);
//
//    }
//
//    public void testMarchingSquaresAndRasterizer6() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        /*
//         * extract vectors
//         */
//        double[][] extractData = HMTestMaps.marchingSq6;
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        GridCoverage2D netCoverage = buildCoverage("net", extractData, envelopeParams, crs, true);
//        GridCoverage2D geodata = netCoverage;
//
//        OmsMarchingSquaresVectorializer squares = new OmsMarchingSquaresVectorializer();
//        squares.inGeodata = geodata;
//        squares.pValue = null;
//        squares.pm = pm;
//
//        squares.process();
//
//        SimpleFeatureCollection outGeodata = squares.outGeodata;
//
//        /*
//         * and rasterize back again
//         */
//        OmsScanLineRasterizer rasterizer = new OmsScanLineRasterizer();
//        rasterizer.inGeodata = outGeodata;
//        rasterizer.pm = pm;
//        setBounds(rasterizer, geodata.getGridGeometry());
//        rasterizer.pValue = null;
//        rasterizer.fCat = squares.defaultFeatureField;
//        rasterizer.process();
//
//        GridCoverage2D outGeodata2 = rasterizer.outGeodata;
//
//        RenderedImage renderedImage = outGeodata2.getRenderedImage();
//
//        // should be the same as before
//        checkMatrixEqual(renderedImage, extractData, 0);
//
//    }
//
//    public void testMarchingSquaresAndRasterizer7() throws Exception {
//        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);
//
//        /*
//         * extract vectors
//         */
//        double[][] extractData = HMTestMaps.marchingSq7;
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        GridCoverage2D netCoverage = buildCoverage("net", extractData, envelopeParams, crs, true);
//        GridCoverage2D geodata = netCoverage;
//
//        OmsMarchingSquaresVectorializer squares = new OmsMarchingSquaresVectorializer();
//        squares.inGeodata = geodata;
//        squares.pValue = null;
//        squares.pm = pm;
//
//        squares.process();
//
//        SimpleFeatureCollection outGeodata = squares.outGeodata;
//
//        /*
//         * and rasterize back again
//         */
//        OmsScanLineRasterizer rasterizer = new OmsScanLineRasterizer();
//        rasterizer.inGeodata = outGeodata;
//        rasterizer.pm = pm;
//        setBounds(rasterizer, geodata.getGridGeometry());
//        rasterizer.pValue = null;
//        rasterizer.fCat = squares.defaultFeatureField;
//        rasterizer.process();
//
//        GridCoverage2D outGeodata2 = rasterizer.outGeodata;
//
//        RenderedImage renderedImage = outGeodata2.getRenderedImage();
//
//        // should be the same as before
//        checkMatrixEqual(renderedImage, extractData, 0);
//    }
//}
