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

import org.eclipse.imagen.iterator.RectIter;
import org.eclipse.imagen.iterator.RectIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.subsampling.OmsRasterSubsampler;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.libs.modules.HMRaster.MergeMode;
import org.hortonmachine.gears.modules.r.transformer.OmsRasterResolutionResampler;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
/**
 * Test HMRaster
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestHMRaster extends HMTestCase {
    private static final double NaN = HMConstants.doubleNovalue;
    private static final int intNaN = HMConstants.intNovalue;
    
    public void testRasterMappingSubstitute() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        
        var flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        var flowRaster = HMRaster.fromGridCoverage(flowCoverage);

        double[][] dtmData = HMTestMaps.mapData;
        GridCoverage2D dtmCoverage = CoverageUtilities.buildCoverage("dtm", dtmData, envelopeParams, crs, true);
        
        
        // SUBSTITUTE IGNORING NOVALUES
        var dtmWritableRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(dtmCoverage).setCopyValues(true).build();
        dtmWritableRaster.mapRaster(pm, flowRaster, MergeMode.SUBSTITUTE_IGNORE_NOVALUE);

        GridCoverage2D mappedCoverage = dtmWritableRaster.buildCoverage();
        RenderedImage renderedImage = mappedCoverage.getRenderedImage();
        double[][] expectedData = new double[][]{//
            {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, NaN, 6, 6, 6, 6, 6, 6, 6, 1500}, //
            {500, 7, 6, 6, 6, 6, 6, 7, 7, 1500}, //
            {400, 5, 5, 7, 6, 6, 6, 6, 5, 1500}, //
            {450, 3, 4, 5, 5, 5, 5, 5, 5, 1500}, //
            {500, 2, 3, 3, 4, 4, 4, 3, 3, 1500}, //
            {600, 4, 4, 4, 4, 4, 5, 4, 4, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};
        checkMatrixEqual(renderedImage, expectedData, 0.000000001);
    }

    public void testRasterMappingSubstituteHalf() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        RegionMap envelopeParamsHalf = HMTestMaps.getEnvelopeparamsLeftHalf();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        
        var flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        double[][] dtmHalfData = HMTestMaps.mapDataLeftHalf;
        GridCoverage2D dtmHalfCoverage = CoverageUtilities.buildCoverage("dtm", dtmHalfData, envelopeParamsHalf, crs, true);
        HMRaster dtmHalfRaster = HMRaster.fromGridCoverage(dtmHalfCoverage);
        
        // SUBSTITUTE IGNORING NOVALUES
        var flowWritableRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(flowCoverage).setCopyValues(true).build();
        flowWritableRaster.mapRaster(pm, dtmHalfRaster, MergeMode.SUBSTITUTE_IGNORE_NOVALUE);

        GridCoverage2D mappedCoverage = flowWritableRaster.buildCoverage();
        RenderedImage renderedImage = mappedCoverage.getRenderedImage();
        double[][] expectedData = new double[][]{//
            {800, 900, 1000, 1000, 1200, intNaN, intNaN, intNaN, intNaN, intNaN}, //
            {600, intNaN, 750, 850, 860, 6, 6, 6, 6, intNaN}, //
            {500, 550, 700, 750, 800, 6, 6, 7, 7, intNaN}, //
            {400, 410, 650, 700, 750, 6, 6, 6, 5, intNaN}, //
            {450, 550, 430, 500, 600, 5, 5, 5, 5, intNaN}, //
            {500, 600, 700, 750, 760, 4, 4, 3, 3, intNaN}, //
            {600, 700, 750, 800, 780, 4, 5, 4, 4, intNaN}, //
            {800, 910, 980, 1001, 1150, intNaN, intNaN, intNaN, intNaN, intNaN}};
        checkMatrixEqual(renderedImage, expectedData, 0.000000001);

    }
    
    public void testRasterMappingSum() throws Exception {
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        
        var flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        var flowRaster = HMRaster.fromGridCoverage(flowCoverage);

        double[][] dtmData = HMTestMaps.mapData;
        GridCoverage2D dtmCoverage = CoverageUtilities.buildCoverage("dtm", dtmData, envelopeParams, crs, true);
        
        
        // SUBSTITUTE IGNORING NOVALUES
        var dtmWritableRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(dtmCoverage).setCopyValues(true).build();
        dtmWritableRaster.mapRaster(pm, flowRaster, MergeMode.SUM);

        GridCoverage2D mappedCoverage = dtmWritableRaster.buildCoverage();
        RenderedImage renderedImage = mappedCoverage.getRenderedImage();
        double[][] expectedData = new double[][]{//
            {800, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
            {600, NaN  , 750+6, 850+6, 860+6, 900+6, 1000+6, 1200+6, 1250+6, 1500}, //
            {500, 550+7, 700+6, 750+6, 800+6, 850+6, 900+6, 1000+7, 1100+7, 1500}, //
            {400, 410+5, 650+5, 700+7, 750+6, 800+6, 850+6, 490+6, 450+5, 1500}, //
            {450, 550+3, 430+4, 500+5, 600+5, 700+5, 800+5, 500+5, 450+5, 1500}, //
            {500, 600+2, 700+3, 750+3, 760+4, 770+4, 850+4, 1000+3, 1150+3, 1500}, //
            {600, 700+4, 750+4, 800+4, 780+4, 790+4, 1000+5, 1100+4, 1250+4, 1500}, //
            {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};

        checkMatrixEqual(renderedImage, expectedData, 0.000000001);

    }
    
    

}
