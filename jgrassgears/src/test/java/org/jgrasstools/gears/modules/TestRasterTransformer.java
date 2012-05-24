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

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.io.rasterwriter.RasterWriter;
import org.jgrasstools.gears.io.vectorwriter.VectorWriter;
import org.jgrasstools.gears.modules.r.transformer.RasterTransformer;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.PrintUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test {@link RasterTransformer}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterTransformer extends HMTestCase {

    public void testRasterTransformer() throws Exception {

        // double[][] flowData = HMTestMaps.flowData;
        // HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        // CoordinateReferenceSystem crs = HMTestMaps.crs;
        // GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
        // envelopeParams, crs, true);

        String rasterPath = "/home/moovida/data/dtm_fazzon_caesar_02.asc";
        GridCoverage2D flowCoverage = RasterReader.readRaster(rasterPath);

        // PrintUtilities.printCoverageData(flowCoverage);

        RasterTransformer transformer = new RasterTransformer();
        transformer.inRaster = flowCoverage;
        transformer.pInterpolation = 2;
        transformer.pAngle = 45.0;
        transformer.pTransX = 100.0;
        transformer.pTransY = 100.0;
        transformer.process();
        GridCoverage2D outCoverage = transformer.outRaster;
        SimpleFeatureCollection outBounds = transformer.outBounds;

        // PrintUtilities.printCoverageData(outCoverage);
        String outRasterPath = "/home/moovida/data/dtm_fazzon_caesar_out45_100bc.asc";
        RasterWriter.writeRaster(outRasterPath, outCoverage);
        String outVectorPath = "/home/moovida/data/dtm_fazzon_caesar_out45_100bc.shp";
        VectorWriter.writeVector(outVectorPath, outBounds);
    }
}
