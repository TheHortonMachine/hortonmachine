/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.gears.modules;

import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.modules.r.mapcalc.OmsMapcalc;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsMapcalc}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TestMapcalc extends HMTestCase {

    public void testMapcalc() throws Exception {

        double[][] elevationData = HMTestMaps.pitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("ele", elevationData, envelopeParams, crs, true);

        List<GridCoverage2D> maps = Arrays.asList(elevationCoverage);

        OmsMapcalc mapcalc = new OmsMapcalc();
        mapcalc.inRasters = maps;
        mapcalc.pFunction = "images{ele=read; dest=write;} dest=ele*2-ele + sqrt(ele)^2-exp(log(ele));";
        mapcalc.process();

        GridCoverage2D outMap = mapcalc.outRaster;

        RenderedImage renderedImage = outMap.getRenderedImage();
        // printImage(renderedImage);
        double[][] expectedData = new double[][]{//
                {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
                {600, Double.NaN, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
                {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
                {400, 410, 650, 700, 750, 800, 850, 800, 800, 1500}, //
                {450, 550, 430, 500, 600, 700, 800, 800, 800, 1500}, //
                {500, 600, 700, 750, 760, 770, 850, 1000, 1150, 1500}, //
                {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
                {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};
        checkMatrixEqual(renderedImage, expectedData, 0.000000001);
    }

    public void testMapcalc2() throws Exception {

        int[][] elevationData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("flow", elevationData, envelopeParams, crs, true);

        List<GridCoverage2D> maps = Arrays.asList(elevationCoverage);

        OmsMapcalc mapcalc = new OmsMapcalc();
        mapcalc.inRasters = maps;
        mapcalc.pFunction = "images{flow=read; dest=write;} dest = (flow+flow)/2;";

        mapcalc.process();

        GridCoverage2D outMap = mapcalc.outRaster;
        RenderedImage renderedImage = outMap.getRenderedImage();
        printImage(renderedImage);
        checkMatrixEqual(renderedImage, HMTestMaps.flowData, 0);
    }

    public void testMapcalc3() throws Exception {
        double[][] elevationData = HMTestMaps.pitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("ele", elevationData, envelopeParams, crs, true);

        List<GridCoverage2D> maps = Arrays.asList(elevationCoverage);

        OmsMapcalc mapcalc = new OmsMapcalc();
        mapcalc.inRasters = maps;
        mapcalc.pFunction = "images{ele=read; dest=write;} dest = xres()*yres();";
        mapcalc.process();

        GridCoverage2D outMap = mapcalc.outRaster;

        RenderedImage renderedImage = outMap.getRenderedImage();
        // printImage(renderedImage);

        checkEqualsSinlgeValue(renderedImage, 900.0, 0.000000001);
    }

    public static void main( String[] args ) throws Exception {
        new TestMapcalc().testMapcalc();
    }
}
