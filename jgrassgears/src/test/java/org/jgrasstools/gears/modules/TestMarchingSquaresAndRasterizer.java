/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.modules;

import java.awt.image.RenderedImage;
import java.util.HashMap;

import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.modules.r.scanline.ScanLineRasterizer;
import org.jgrasstools.gears.modules.v.marchingsquares.MarchingSquaresVectorializer;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestMarchingSquaresAndRasterizer extends HMTestCase {

    public void testMarchingSquaresAndRasterizer1() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        /*
         * extract vectors
         */
        double[][] extractNet1Data = HMTestMaps.marchingSq1;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", extractNet1Data, envelopeParams, crs, true);
        GridCoverage2D geodata = netCoverage;

        MarchingSquaresVectorializer squares = new MarchingSquaresVectorializer();
        squares.inGeodata = geodata;
        squares.pValue = 2.0;
        squares.pm = pm;

        squares.process();

        SimpleFeatureCollection outGeodata = squares.outGeodata;

        /*
         * and rasterize back again
         */
        ScanLineRasterizer rasterizer = new ScanLineRasterizer();
        rasterizer.inGeodata = outGeodata;
        rasterizer.pm = pm;
        rasterizer.pGrid = geodata.getGridGeometry();
        rasterizer.pValue = 2.0;
        rasterizer.process();

        GridCoverage2D outGeodata2 = rasterizer.outGeodata;

        RenderedImage renderedImage = outGeodata2.getRenderedImage();

        // should be the same as before
        checkMatrixEqual(renderedImage, extractNet1Data, 0);

    }

    public void testMarchingSquaresAndRasterizer2() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        /*
         * extract vectors
         */
        double[][] extractNet1Data = HMTestMaps.marchingSq2;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", extractNet1Data, envelopeParams, crs, true);
        GridCoverage2D geodata = netCoverage;

        MarchingSquaresVectorializer squares = new MarchingSquaresVectorializer();
        squares.inGeodata = geodata;
        squares.pValue = 2.0;
        squares.pm = pm;

        squares.process();

        SimpleFeatureCollection outGeodata = squares.outGeodata;

        /*
         * and rasterize back again
         */
        ScanLineRasterizer rasterizer = new ScanLineRasterizer();
        rasterizer.inGeodata = outGeodata;
        rasterizer.pm = pm;
        rasterizer.pGrid = geodata.getGridGeometry();
        rasterizer.pValue = 2.0;
        rasterizer.process();

        GridCoverage2D outGeodata2 = rasterizer.outGeodata;

        RenderedImage renderedImage = outGeodata2.getRenderedImage();

        // should be the same as before
        checkMatrixEqual(renderedImage, extractNet1Data, 0);

    }

    public void testMarchingSquaresAndRasterizer3() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        /*
         * extract vectors
         */
        double[][] extractData = HMTestMaps.marchingSq3;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", extractData, envelopeParams, crs, true);
        GridCoverage2D geodata = netCoverage;

        MarchingSquaresVectorializer squares = new MarchingSquaresVectorializer();
        squares.inGeodata = geodata;
        squares.pValue = 2.0;
        squares.pm = pm;

        squares.process();

        SimpleFeatureCollection outGeodata = squares.outGeodata;

        /*
         * and rasterize back again
         */
        ScanLineRasterizer rasterizer = new ScanLineRasterizer();
        rasterizer.inGeodata = outGeodata;
        rasterizer.pm = pm;
        rasterizer.pGrid = geodata.getGridGeometry();
        rasterizer.pValue = 2.0;
        rasterizer.process();

        GridCoverage2D outGeodata2 = rasterizer.outGeodata;

        RenderedImage renderedImage = outGeodata2.getRenderedImage();

        RectIter iter = RectIterFactory.create(renderedImage, null);
        do {
            do {
                System.out.print(iter.getSampleDouble() + " ");
            } while( !iter.nextPixelDone() );
            iter.startPixels();
            System.out.println();
        } while( !iter.nextLineDone() );

        // should be the same as before
        checkMatrixEqual(renderedImage, extractData, 0);

    }

    public void testMarchingSquaresAndRasterizer4() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        /*
         * extract vectors
         */
        double[][] extractData = HMTestMaps.marchingSq4;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", extractData, envelopeParams, crs, true);
        GridCoverage2D geodata = netCoverage;

        MarchingSquaresVectorializer squares = new MarchingSquaresVectorializer();
        squares.inGeodata = geodata;
        squares.pValue = 2.0;
        squares.pm = pm;

        squares.process();

        SimpleFeatureCollection outGeodata = squares.outGeodata;

        /*
         * and rasterize back again
         */
        ScanLineRasterizer rasterizer = new ScanLineRasterizer();
        rasterizer.inGeodata = outGeodata;
        rasterizer.pm = pm;
        rasterizer.pGrid = geodata.getGridGeometry();
        rasterizer.pValue = 2.0;
        rasterizer.process();

        GridCoverage2D outGeodata2 = rasterizer.outGeodata;

        RenderedImage renderedImage = outGeodata2.getRenderedImage();

        RectIter iter = RectIterFactory.create(renderedImage, null);
        do {
            do {
                System.out.print(iter.getSampleDouble() + " ");
            } while( !iter.nextPixelDone() );
            iter.startPixels();
            System.out.println();
        } while( !iter.nextLineDone() );

        // should be the same as before
        checkMatrixEqual(renderedImage, extractData, 0);

    }

    public void testMarchingSquaresAndRasterizer5() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        /*
         * extract vectors
         */
        double[][] extractData = HMTestMaps.marchingSq3;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", extractData, envelopeParams, crs, true);
        GridCoverage2D geodata = netCoverage;

        MarchingSquaresVectorializer squares = new MarchingSquaresVectorializer();
        squares.inGeodata = geodata;
        squares.pValue = 2.0;
        squares.pm = pm;

        squares.process();

        SimpleFeatureCollection outGeodata = squares.outGeodata;

        /*
         * and rasterize back again
         */
        ScanLineRasterizer rasterizer = new ScanLineRasterizer();
        rasterizer.inGeodata = outGeodata;
        rasterizer.pm = pm;
        rasterizer.pGrid = geodata.getGridGeometry();
        rasterizer.pValue = 2.0;
        rasterizer.process();

        GridCoverage2D outGeodata2 = rasterizer.outGeodata;

        RenderedImage renderedImage = outGeodata2.getRenderedImage();

        RectIter iter = RectIterFactory.create(renderedImage, null);
        do {
            do {
                System.out.print(iter.getSampleDouble() + " ");
            } while( !iter.nextPixelDone() );
            iter.startPixels();
            System.out.println();
        } while( !iter.nextLineDone() );

        // should be the same as before
        checkMatrixEqual(renderedImage, extractData, 0);

    }

    public void testMarchingSquaresAndRasterizer6() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        /*
         * extract vectors
         */
        double[][] extractData = HMTestMaps.marchingSq6;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", extractData, envelopeParams, crs, true);
        GridCoverage2D geodata = netCoverage;

        MarchingSquaresVectorializer squares = new MarchingSquaresVectorializer();
        squares.inGeodata = geodata;
        squares.pValue = null;
        squares.pm = pm;

        squares.process();

        SimpleFeatureCollection outGeodata = squares.outGeodata;

        /*
         * and rasterize back again
         */
        ScanLineRasterizer rasterizer = new ScanLineRasterizer();
        rasterizer.inGeodata = outGeodata;
        rasterizer.pm = pm;
        rasterizer.pGrid = geodata.getGridGeometry();
        rasterizer.pValue = null;
        rasterizer.fValueToRasterize = squares.defaultFeatureField;
        rasterizer.process();

        GridCoverage2D outGeodata2 = rasterizer.outGeodata;

        RenderedImage renderedImage = outGeodata2.getRenderedImage();

        // should be the same as before
        checkMatrixEqual(renderedImage, extractData, 0);

    }

    public void testMarchingSquaresAndRasterizer7() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        /*
         * extract vectors
         */
        double[][] extractData = HMTestMaps.marchingSq7;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", extractData, envelopeParams, crs, true);
        GridCoverage2D geodata = netCoverage;

        MarchingSquaresVectorializer squares = new MarchingSquaresVectorializer();
        squares.inGeodata = geodata;
        squares.pValue = null;
        squares.pm = pm;

        squares.process();

        SimpleFeatureCollection outGeodata = squares.outGeodata;

        /*
         * and rasterize back again
         */
        ScanLineRasterizer rasterizer = new ScanLineRasterizer();
        rasterizer.inGeodata = outGeodata;
        rasterizer.pm = pm;
        rasterizer.pGrid = geodata.getGridGeometry();
        rasterizer.pValue = null;
        rasterizer.fValueToRasterize = squares.defaultFeatureField;
        rasterizer.process();

        GridCoverage2D outGeodata2 = rasterizer.outGeodata;

        RenderedImage renderedImage = outGeodata2.getRenderedImage();

        // should be the same as before
        checkMatrixEqual(renderedImage, extractData, 0);
    }
}
