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
package org.jgrasstools.examples;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.io.rasterwriter.RasterWriter;
import org.jgrasstools.gears.modules.r.rasterreprojector.RasterReprojector;

public class RasterReprojectionExample {

    public static void main( String[] args ) throws Exception {

        String inputRaster = "your input path here";
        String outputRaster = "your output path here";

        // read the raster
        GridCoverage2D readCoverage = RasterReader.readRaster(inputRaster);

        // reproject
        RasterReprojector rasterReprojector = new RasterReprojector();
        rasterReprojector.inRaster = readCoverage;
        rasterReprojector.pCode = "EPSG:32632";
        rasterReprojector.process();
        GridCoverage2D outRaster = rasterReprojector.outRaster;

        // write the raster
        RasterWriter.writeRaster(outputRaster, outRaster);

    }

}
