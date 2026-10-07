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

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.List;

import org.eclipse.imagen.iterator.RandomIter;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.modules.r.mapcalc.OmsMapcalc;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsMapcalc}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class TestMapcalc extends HMTestCase {

    public void testMapcalc() throws Exception {

        double[][] elevationData = HMTestMaps.pitData;
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
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
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
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
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
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

    /**
     * North is up: y() is the northing of the south edge of the cell, so it decreases
     * from the first row to the last.
     */
    public void testMapcalcNorthUp() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("ele", HMTestMaps.pitData, envelopeParams,
                HMTestMaps.getCrs(), true);

        OmsMapcalc mapcalc = new OmsMapcalc();
        mapcalc.inRasters = Arrays.asList(elevationCoverage);
        mapcalc.pFunction = "images{ele=read; dest=write;} dest = y();";
        mapcalc.process();

        RandomIter iter = CoverageUtilities.getRandomIterator(mapcalc.outRaster);
        double north = envelopeParams.getNorth();
        double south = envelopeParams.getSouth();
        double yres = envelopeParams.getYres();
        int rows = envelopeParams.getRows();
        assertEquals(north - yres, iter.getSampleDouble(0, 0, 0), 0.000001);
        assertEquals(south, iter.getSampleDouble(0, rows - 1, 0), 0.000001);
    }

    /**
     * The offsets are in map units, positive towards east and north.
     */
    public void testMapcalcNeighbours() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        double[][] elevationData = HMTestMaps.pitData;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("ele", elevationData, envelopeParams,
                HMTestMaps.getCrs(), true);

        OmsMapcalc mapcalc = new OmsMapcalc();
        mapcalc.inRasters = Arrays.asList(elevationCoverage);
        mapcalc.pFunction = "options{outside=null;} images{ele=read; dest=write;} dest = ele[xres(), yres()];";
        mapcalc.process();

        RandomIter iter = CoverageUtilities.getRandomIterator(mapcalc.outRaster);
        // the cell to the north east
        assertEquals(elevationData[2][4], iter.getSampleDouble(3, 3, 0), 0.000001);
        // north of the first row there is nothing
        assertTrue(isNovalue(iter.getSampleDouble(3, 0, 0)));
    }

    /**
     * The novalues are the null of Jiffle: they are recognized by isnull, and they stay novalue
     * in the calculations.
     */
    public void testMapcalcNovalue() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        double[][] elevationData = HMTestMaps.pitData;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("ele", elevationData, envelopeParams,
                HMTestMaps.getCrs(), true);

        OmsMapcalc mapcalc = new OmsMapcalc();
        mapcalc.inRasters = Arrays.asList(elevationCoverage);
        mapcalc.pFunction = "images{ele=read; dest=write;} dest = isnull(ele) ? 1 : ele * 0;";
        mapcalc.process();
        RandomIter iter = CoverageUtilities.getRandomIterator(mapcalc.outRaster);
        assertEquals(1.0, iter.getSampleDouble(1, 1, 0), 0.0);
        assertEquals(0.0, iter.getSampleDouble(0, 0, 0), 0.0);

        mapcalc = new OmsMapcalc();
        mapcalc.inRasters = Arrays.asList(elevationCoverage);
        mapcalc.pFunction = "images{ele=read; dest=write;} dest = ele + 1;";
        mapcalc.process();
        iter = CoverageUtilities.getRandomIterator(mapcalc.outRaster);
        assertTrue(isNovalue(iter.getSampleDouble(1, 1, 0)));
        assertEquals(801.0, iter.getSampleDouble(0, 0, 0), 0.0);
    }

    /**
     * A script that never assigns the output map is refused, instead of giving a map of 0.
     */
    public void testMapcalcUnassignedOutput() throws Exception {
        assertTrue(OmsMapcalc.assignsVariable("images{ele=read; dest=write;} dest = ele;", "dest"));
        assertTrue(OmsMapcalc.assignsVariable("images{dest=write;} if (1) { dest += 2; }", "dest"));
        assertTrue(OmsMapcalc.assignsVariable("images{dest=write;} dest++;", "dest"));
        assertFalse(OmsMapcalc.assignsVariable("images{ele=read; dest=write;} other = ele;", "dest"));
        assertFalse(OmsMapcalc.assignsVariable("images{dest=write;} x = 1; // dest = 2;", "dest"));
        assertFalse(OmsMapcalc.assignsVariable("images{dest=write;} /* dest = 2; */ x = 1;", "dest"));
        assertFalse(OmsMapcalc.assignsVariable("images{dest=write;} x = dest == 1;", "dest"));
        assertFalse(OmsMapcalc.assignsVariable("images{dest=write;} mydest = 1;", "dest"));

        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("ele", HMTestMaps.pitData,
                HMTestMaps.getEnvelopeparams(), HMTestMaps.getCrs(), true);
        OmsMapcalc mapcalc = new OmsMapcalc();
        mapcalc.inRasters = Arrays.asList(elevationCoverage);
        mapcalc.pFunction = "images{ele=read; dest=write;} other = ele + 1;";
        try {
            mapcalc.process();
            fail("The unassigned output map should be refused.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("dest"));
        }
    }

    public static void main( String[] args ) throws Exception {
        new TestMapcalc().testMapcalc();
    }
}
