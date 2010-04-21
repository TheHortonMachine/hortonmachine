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
package eu.hydrologis.jgrass.jgrassgears.modules;

import java.awt.image.RenderedImage;
import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.modules.r.scanline.ScanLineRasterizer;
import eu.hydrologis.jgrass.jgrassgears.modules.v.marchingsquares.MarchingSquaresVectorializer;
import eu.hydrologis.jgrass.jgrassgears.utils.HMTestCase;
import eu.hydrologis.jgrass.jgrassgears.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;

/**
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestMarchingSquaresAndRasterizer extends HMTestCase {
    public void testMarchingSquaresAndRasterizer() throws Exception {
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        /*
         * extract vectors
         */
        double[][] extractNet1Data = HMTestMaps.extractNet1Data;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", extractNet1Data,
                envelopeParams, crs);
        GridCoverage2D geodata = netCoverage;

        MarchingSquaresVectorializer squares = new MarchingSquaresVectorializer();
        squares.inGeodata = geodata;
        squares.pValue = 2;
        squares.pm = pm;

        squares.process();

        FeatureCollection<SimpleFeatureType, SimpleFeature> outGeodata = squares.outGeodata;

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

}
