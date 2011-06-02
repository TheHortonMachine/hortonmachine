/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jgrasstools.gears.modules;

import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.io.rasterwriter.RasterWriter;
import org.jgrasstools.gears.modules.r.mapcalc.Mapcalc;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link Mapcalc}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TestMapcalc extends HMTestCase {
    
    public void testMapcalc0() throws Exception {
        GridCoverage2D elevationCoverage = RasterReader.readCoverage("/home/moovida/TMP/formetta/newLocation/newMapset/cell/netshp2fl_net");
        
        List<GridCoverage2D> maps = Arrays.asList(elevationCoverage);
        
        Mapcalc mapcalc = new Mapcalc();
        mapcalc.inRasters = maps;
        mapcalc.pFunction = "images { netshp2fl_net = read; result = write; } if (!isnull(netshp2fl_net)) { result = 2.0; } else { result = null; }";
        mapcalc.process();
        
        GridCoverage2D outMap = mapcalc.outRaster;
        
        RasterWriter.writeRaster("/home/moovida/TMP/formetta/newLocation/newMapset/cell/netshp2fl_net2", outMap);
    }
    
//    public void testMapcalc() throws Exception {
//
//        double[][] elevationData = HMTestMaps.pitData;
//        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
//        CoordinateReferenceSystem crs = HMTestMaps.crs;
//        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("ele", elevationData, envelopeParams, crs, true);
//
//        List<GridCoverage2D> maps = Arrays.asList(elevationCoverage);
//
//        Mapcalc mapcalc = new Mapcalc();
//        mapcalc.inRasters = maps;
//        mapcalc.pFunction = "images{ele=read; dest=write;} dest=ele*2-ele + sqrt(ele)^2-exp(log(ele));";
//        mapcalc.process();
//
//        GridCoverage2D outMap = mapcalc.outRaster;
//
//        RenderedImage renderedImage = outMap.getRenderedImage();
//        // printImage(renderedImage);
//        checkMatrixEqual(renderedImage, HMTestMaps.pitData, 0.000000001);
//    }
//
//    public void testMapcalc2() throws Exception {
//
//        double[][] elevationData = HMTestMaps.flowData;
//        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
//        CoordinateReferenceSystem crs = HMTestMaps.crs;
//        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("flow", elevationData, envelopeParams, crs, true);
//
//        List<GridCoverage2D> maps = Arrays.asList(elevationCoverage);
//
//        Mapcalc mapcalc = new Mapcalc();
//        mapcalc.inRasters = maps;
//        mapcalc.pFunction = "images{flow=read; dest=write;} dest = (flow+flow)/2;";
//
//        mapcalc.process();
//
//        GridCoverage2D outMap = mapcalc.outRaster;
//        RenderedImage renderedImage = outMap.getRenderedImage();
//        // printImage(renderedImage);
//        checkMatrixEqual(renderedImage, HMTestMaps.flowData, 0.000000001);
//    }
//
//    public void testMapcalc3() throws Exception {
//        double[][] elevationData = HMTestMaps.pitData;
//        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
//        CoordinateReferenceSystem crs = HMTestMaps.crs;
//        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("ele", elevationData, envelopeParams, crs, true);
//
//        List<GridCoverage2D> maps = Arrays.asList(elevationCoverage);
//
//        Mapcalc mapcalc = new Mapcalc();
//        mapcalc.inRasters = maps;
//        mapcalc.pFunction = "images{ele=read; dest=write;} dest = xres()*yres();";
//        mapcalc.process();
//
//        GridCoverage2D outMap = mapcalc.outRaster;
//
//        RenderedImage renderedImage = outMap.getRenderedImage();
//        // printImage(renderedImage);
//
//        checkEqualsSinlgeValue(renderedImage, 900.0, 0.000000001);
//    }
//
    public static void main( String[] args ) throws Exception {
        new TestMapcalc().testMapcalc0();
    }
}
