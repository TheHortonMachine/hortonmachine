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
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.modules.r.raster2xyz.Raster2Xyz;
import org.jgrasstools.gears.modules.r.summary.RasterSummary;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link RasterSummary}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRaster2Xyz extends HMTestCase {
    public void testCoverageSummary() throws Exception {

        // String raster = "/home/moovida/TMP/dtm_utm12201/dtm_utm12201.asc";
        // String outFile = "/home/moovida/TMP/dtm_utm12201/dtm_utm12201.xyz";
        // GridCoverage2D inCoverage = RasterReader.readRaster(raster);
        //
        // Raster2Xyz raster2Xyz = new Raster2Xyz();
        // raster2Xyz.pm = pm;
        // raster2Xyz.inRaster = inCoverage;
        // raster2Xyz.inFile = outFile;
        // raster2Xyz.process();

    }

}
