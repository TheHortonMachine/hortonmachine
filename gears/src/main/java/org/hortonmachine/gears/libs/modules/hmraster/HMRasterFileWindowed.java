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

import org.hortonmachine.gears.libs.modules.HMRaster;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.FileImageInputStream;

import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReaderSpi;

/**
 * A read only {@link HMRaster} on a GeoTIFF file, for rasters too large to be loaded in memory.
 *
 * <p>The metadata come from a lazily read coverage, so nothing is decoded at creation.
 * The cell by cell access of {@link HMRaster} still works, through the single shared reader
 * of the coverage, but the fast path is {@link #getValues(int, int, int, int, double[])}:
 * every thread gets its own TIFF reader, so that blocks are decoded in parallel. A
 * GeoTools coverage decodes through one reader only, which serializes the decoding of
 * compressed files.</p>
 *
 * <p>Read blocks aligned to the internal tiles of the file for best performance.</p>
 */
public class HMRasterFileWindowed extends HMRaster {
    private final File file;
    private final ThreadLocal<WindowReader> readers;
    private final List<WindowReader> allReaders = new ArrayList<>();

    /**
     * @param path the path to the GeoTIFF file.
     * @throws Exception
     */
    public HMRasterFileWindowed( String path ) throws Exception {
        String lowerPath = path.toLowerCase();
        if (!lowerPath.endsWith(".tif") && !lowerPath.endsWith(".tiff")) {
            throw new ModelsIllegalargumentException("Only GeoTIFF files can be read by windows: " + path, this);
        }
        file = new File(path);
        initFromCoverage(null, OmsRasterReader.readRaster(path));
        readers = ThreadLocal.withInitial(() -> {
            WindowReader reader = new WindowReader(file);
            synchronized (allReaders) {
                allReaders.add(reader);
            }
            return reader;
        });
    }

    /**
     * @return the file backing this raster.
     */
    public File getFile() {
        return file;
    }

    @Override
    public double[] getValues( int col, int row, int width, int height, double[] buffer ) {
        try {
            double[] values = readers.get().read(col, row, width, height, buffer);
            return values;
        } catch (IOException e) {
            throw new RuntimeException("Error reading block " + col + "/" + row + " of " + file, e);
        }
    }

    @Override
    public void close() throws Exception {
        synchronized (allReaders) {
            for( WindowReader reader : allReaders ) {
                reader.close();
            }
            allReaders.clear();
        }
        super.close();
    }

    /**
     * A TIFF reader reading windows of the first image of a file.
     */
    private static class WindowReader {
        private final FileImageInputStream stream;
        private final ImageReader reader;

        WindowReader( File file ) {
            try {
                stream = new FileImageInputStream(file);
                reader = new TIFFImageReaderSpi().createReaderInstance();
                reader.setInput(stream, true, true);
            } catch (IOException e) {
                throw new RuntimeException("Unable to open the TIFF file: " + file, e);
            }
        }

        double[] read( int col, int row, int width, int height, double[] buffer ) throws IOException {
            ImageReadParam param = reader.getDefaultReadParam();
            param.setSourceRegion(new Rectangle(col, row, width, height));
            Raster raster = reader.read(0, param).getRaster();
            return raster.getSamples(raster.getMinX(), raster.getMinY(), width, height, 0, buffer);
        }

        void close() {
            try {
                reader.dispose();
                stream.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }
}
