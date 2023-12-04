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

import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterResolutionResampler;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
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
    
    

}
