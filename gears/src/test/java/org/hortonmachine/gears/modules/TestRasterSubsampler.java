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
import java.util.HashMap;
import java.util.Map;

import org.eclipse.imagen.iterator.RectIter;
import org.eclipse.imagen.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterResolutionResampler;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
/**
 * Test {@link OmsRasterResolutionResampler}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterSubsampler extends HMTestCase {

    public void testRasterSubsampler() throws Exception {
        
        double[][] dtmData = HMTestMaps.mapData;
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D raster = CoverageUtilities.buildCoverage("dtm", dtmData, envelopeParams, crs, true);
        
        OmsRasterSubsampler ss = new OmsRasterSubsampler();
        ss.inRaster = raster;
        ss.pFactor = 2;
        ss.pMode = OmsRasterSubsampler.NEAREST;
        ss.process();
        GridCoverage2D outCoverage = ss.outRaster;
        
        double[][] expected = new double[][]{
            {-9999.0,850.0,900.0,1200.0,1500.0},
            {410.0,700.0,800.0,490.0,1500.0},
            {600.0,750.0,770.0,1000.0,1500.0},
            {910.0,1001.0,1200.0,1300.0,1500.0}};
            
        checkMatrixEqual(outCoverage.getRenderedImage(), expected, DELTA);
    }
    
    public void testRasterSubsamplerNonFitting() throws Exception {

        double[][] dtmData = HMTestMaps.mapData;
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D raster = CoverageUtilities.buildCoverage("dtm", dtmData, envelopeParams, crs, true);

        OmsRasterSubsampler ss = new OmsRasterSubsampler();
        ss.inRaster = raster;
        ss.pFactor = 3;
        ss.pMode = OmsRasterSubsampler.NEAREST;
        ss.process();
        GridCoverage2D outCoverage = ss.outRaster;
        
        double[][] expected = new double[][]{
            {-9999.0,860.0,1200.0},
            {550.0, 600.0, 500.0}
        };
        
        checkMatrixEqual(outCoverage.getRenderedImage(), expected, DELTA);
        
        RegionMap newRegionMap = HMRaster.fromGridCoverage(outCoverage).getRegionMap();
        
        assertEquals(2, newRegionMap.getRows());
        assertEquals(3, newRegionMap.getCols());
        assertEquals(3, newRegionMap.getCols());
        assertEquals(5139840.0, newRegionMap.getSouth());
        assertEquals(1640920.0, newRegionMap.getEast());

    }

    /**
     * The aggregations of the 2x2 blocks: the novalue of the first block is left out, on ties the
     * most frequent value is the smallest.
     */
    public void testAggregations() throws Exception {
        GridCoverage2D raster = CoverageUtilities.buildCoverage("dtm", HMTestMaps.mapData, HMTestMaps.getEnvelopeparams(),
                HMTestMaps.getCrs(), true);

        double[][] expectedAvg = new double[][]{//
                {2300.0 / 3, 900, 1052.5, 1212.5, 1425}, //
                {465, 700, 800, 810, 1137.5}, //
                {525, 595, 707.5, 787.5, 1150}, //
                {752.5, 882.75, 980, 1162.5, 1425}};
        checkMatrixEqual(subsample(raster, 2, Variables.AVG).getRenderedImage(), expectedAvg, DELTA);

        Map<String, double[]> firstRows = new HashMap<>();
        firstRows.put(Variables.MIN, new double[]{600, 750, 860, 1000, 1250});
        firstRows.put(Variables.MAX, new double[]{900, 1000, 1250, 1350, 1500});
        firstRows.put(Variables.SUM, new double[]{2300, 3600, 4210, 4850, 5700});
        firstRows.put(OmsRasterSubsampler.MAJORITY, new double[]{600, 1000, 860, 1000, 1500});
        for( Map.Entry<String, double[]> entry : firstRows.entrySet() ) {
            GridCoverage2D out = subsample(raster, 2, entry.getKey());
            for( int c = 0; c < 5; c++ ) {
                assertEquals(entry.getKey() + " col " + c, entry.getValue()[c], CoverageUtilities.getValue(out, c, 0), DELTA);
            }
        }

        RegionMap region = HMRaster.fromGridCoverage(subsample(raster, 2, Variables.AVG)).getRegionMap();
        RegionMap inRegion = HMTestMaps.getEnvelopeparams();
        assertEquals(inRegion.getXres() * 2, region.getXres(), DELTA);
        assertEquals(inRegion.getYres() * 2, region.getYres(), DELTA);
        assertEquals(inRegion.getWest(), region.getWest(), DELTA);
        assertEquals(inRegion.getNorth(), region.getNorth(), DELTA);
    }

    public void testBlockWithoutValues() throws Exception {
        double NaN = HMConstants.doubleNovalue;
        double[][] data = new double[][]{//
                {NaN, NaN, 1, 2}, //
                {NaN, NaN, 3, 3}};
        RegionMap region = RegionMap.fromBoundsAndGrid(0, 40, 0, 20, 4, 2);
        GridCoverage2D raster = CoverageUtilities.buildCoverage("data", data, region, HMTestMaps.getCrs(), true);

        checkMatrixEqual(subsample(raster, 2, Variables.AVG).getRenderedImage(), new double[][]{{NaN, 2.25}}, DELTA);
        checkMatrixEqual(subsample(raster, 2, OmsRasterSubsampler.MAJORITY).getRenderedImage(), new double[][]{{NaN, 3}},
                DELTA);
    }

    private GridCoverage2D subsample( GridCoverage2D raster, int factor, String mode ) throws Exception {
        OmsRasterSubsampler ss = new OmsRasterSubsampler();
        ss.pm = pm;
        ss.inRaster = raster;
        ss.pFactor = factor;
        ss.pMode = mode;
        ss.process();
        return ss.outRaster;
    }

}
