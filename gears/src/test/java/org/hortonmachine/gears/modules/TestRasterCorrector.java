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

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;

import org.geotools.coverage.grid.GridCoverage2D;
import org.locationtech.jts.geom.Coordinate;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.Variables;
import org.hortonmachine.gears.modules.r.rasterconverter.OmsRasterConverter;
import org.hortonmachine.gears.modules.r.rastercorrector.OmsRasterCorrector;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsRasterCorrector}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterCorrector extends HMTestCase {
    private double NaN = HMConstants.doubleNovalue;
    public void testRasterCorrector() throws Exception {

        double[][] inData = HMTestMaps.mapData;
        RegionMap envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);

        OmsRasterCorrector reprojector = new OmsRasterCorrector();
        reprojector.inRaster = inCoverage;
        reprojector.pCorrections = "0,0,1.0, 5,5, 666";
        reprojector.process();
        GridCoverage2D outGeodata = reprojector.outRaster;

        double[][] correctedMapData = new double[][]{//
        {1.0, 900, 1000, 1000, 1200, 1250, 1300, 1350, 1450, 1500}, //
                {600, NaN, 750, 850, 860, 900, 1000, 1200, 1250, 1500}, //
                {500, 550, 700, 750, 800, 850, 900, 1000, 1100, 1500}, //
                {400, 410, 650, 700, 750, 800, 850, 490, 450, 1500}, //
                {450, 550, 430, 500, 600, 700, 800, 500, 450, 1500}, //
                {500, 600, 700, 750, 760, 666.0, 850, 1000, 1150, 1500}, //
                {600, 700, 750, 800, 780, 790, 1000, 1100, 1250, 1500}, //
                {800, 910, 980, 1001, 1150, 1200, 1250, 1300, 1450, 1500}};

        checkMatrixEqual(outGeodata.getRenderedImage(), correctedMapData);

        // the same with the coordinates of the cell centers, and corrections outside of the raster left out
        Coordinate first = CoverageUtilities.coordinateFromColRow(0, 0, inCoverage.getGridGeometry());
        Coordinate second = CoverageUtilities.coordinateFromColRow(5, 5, inCoverage.getGridGeometry());
        reprojector = new OmsRasterCorrector();
        reprojector.pm = pm;
        reprojector.inRaster = inCoverage;
        reprojector.doCoordinates = true;
        reprojector.pCorrections = first.x + "," + first.y + ",1.0, " + second.x + "," + second.y + ", 666, 0, 0, 5";
        reprojector.process();
        checkMatrixEqual(reprojector.outRaster.getRenderedImage(), correctedMapData);

        reprojector = new OmsRasterCorrector();
        reprojector.pm = pm;
        reprojector.inRaster = inCoverage;
        reprojector.pCorrections = "0,0,1.0, 5,5, 666, -1,0,5, 10,0,5";
        reprojector.process();
        checkMatrixEqual(reprojector.outRaster.getRenderedImage(), correctedMapData);
    }

    public void testIntegerRasterKeepsType() throws Exception {
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("flow", HMTestMaps.flowData,
                HMTestMaps.getEnvelopeparams(), HMTestMaps.getCrs(), true);
        // the int matrix gives a double raster: make it a real integer one
        OmsRasterConverter converter = new OmsRasterConverter();
        converter.pm = pm;
        converter.inRaster = inCoverage;
        converter.pOutType = Variables.TYPE_INT;
        converter.process();

        OmsRasterCorrector corrector = new OmsRasterCorrector();
        corrector.pm = pm;
        corrector.inRaster = converter.outRaster;
        corrector.pCorrections = "1,1,8";
        corrector.process();

        RenderedImage image = corrector.outRaster.getRenderedImage();
        assertEquals(DataBuffer.TYPE_INT, image.getSampleModel().getDataType());
        assertEquals(8, image.getData().getSample(1, 1, 0));
        assertEquals(HMTestMaps.flowData[2][2], image.getData().getSample(2, 2, 0));
    }

}
