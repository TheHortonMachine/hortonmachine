/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.libs.modules.hmraster;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMRaster;

/**
 * A read only multiband {@link HMRaster}.
 *
 * <p>The cell accessors inherited from {@link HMRaster} work on the first band, the blocks of
 * every band are read with {@link #getValues(int, int, int, int, int, double[])}.</p>
 */
public class HMMBRaster extends HMRaster {

    /**
     * Build a multiband raster on a coverage.
     *
     * @param coverage the coverage, with all its bands.
     * @return the raster.
     */
    public static HMMBRaster fromGridCoverage( GridCoverage2D coverage ) {
        HMMBRaster raster = new HMMBRaster();
        raster.initFromCoverage(null, coverage);
        return raster;
    }

    /**
     * Build a multiband raster from a file, keeping all its bands.
     *
     * <p>For GeoTIFF files too large to be loaded in memory use {@link HMRasterFileWindowed}.</p>
     *
     * @param path the path to the file.
     * @return the raster.
     */
    public static HMMBRaster fromFile( String path ) throws Exception {
        return fromGridCoverage(OmsRasterReader.readRaster(new File(path).getAbsolutePath()));
    }

    /**
     * @return the number of bands.
     */
    public int getBandsCount() {
        return getRenderedImage().getSampleModel().getNumBands();
    }

    /**
     * Get the values of a block of cells of a band.
     *
     * <p>Does not use the shared iterator, so it can be called concurrently from multiple threads.</p>
     *
     * @param col the first col of the block.
     * @param row the first row of the block.
     * @param width the number of cols of the block.
     * @param height the number of rows of the block.
     * @param band the band, from 0 to {@link #getBandsCount()} - 1.
     * @param buffer an optional array of at least width*height to fill. If null, a new one is created.
     * @return the values of the block in row-major order.
     */
    public double[] getValues( int col, int row, int width, int height, int band, double[] buffer ) {
        if (buffer == null) {
            buffer = new double[width * height];
        }
        RenderedImage image = getRenderedImage();
        Raster data = image.getData(new Rectangle(col, row, width, height));
        return data.getSamples(col, row, width, height, band, buffer);
    }

    @Override
    public double[] getValues( int col, int row, int width, int height, double[] buffer ) {
        return getValues(col, row, width, height, 0, buffer);
    }
}
