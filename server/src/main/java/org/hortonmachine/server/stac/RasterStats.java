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
package org.hortonmachine.server.stac;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterStatistics;
import org.hortonmachine.gears.utils.files.FileUtilities;

/**
 * The statistics of the first band of a raster, as given in the raster:bands of the STAC items.
 *
 * <p>They are read, as GDAL does, from the <code>.aux.xml</code> sidecar or from the GDAL_METADATA tag of a
 * GeoTIFF, as the COGs of the jobs have them, else computed reading the raster by blocks.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class RasterStats {
    private static final int TAG_GDAL_METADATA = 42112;
    private static final int TYPE_ASCII = 2;
    /** The largest GDAL_METADATA tag read, the statistics are at most a few hundred bytes. */
    private static final int MAX_TAG_BYTES = 1024 * 1024;
    /** The cells read at once when computing. */
    private static final int BLOCK_CELLS = 1 << 20;

    private static final Pattern AUX_ITEM = Pattern.compile("<MDI key=\"(STATISTICS_[A-Z_]+)\">([^<]*)</MDI>");
    /** The items of the first band, sample 0. */
    private static final Pattern TAG_ITEM = Pattern.compile("<Item name=\"(STATISTICS_[A-Z_]+)\"(?: sample=\"0\")?>([^<]*)</Item>");

    public final double minimum;
    public final double maximum;
    public final double mean;
    public final double stddev;
    /** The percentage of valid cells, <code>null</code> if unknown. */
    public final Double validPercent;

    public RasterStats( double minimum, double maximum, double mean, double stddev, Double validPercent ) {
        this.minimum = minimum;
        this.maximum = maximum;
        this.mean = mean;
        this.stddev = stddev;
        this.validPercent = validPercent;
    }

    /**
     * Read the statistics kept with a raster file.
     *
     * @return the statistics, or <code>null</code> if the file has none.
     */
    public static RasterStats read( File rasterFile ) throws IOException {
        File auxFile = HMRasterStatistics.getAuxXmlFile(rasterFile);
        if (auxFile.isFile()) {
            // only the first band
            String aux = FileUtilities.readFile(auxFile);
            int secondBand = aux.indexOf("<PAMRasterBand band=\"2\"");
            RasterStats stats = fromItems(AUX_ITEM, secondBand > 0 ? aux.substring(0, secondBand) : aux);
            if (stats != null) {
                return stats;
            }
        }
        String name = rasterFile.getName().toLowerCase();
        if (name.endsWith(".tif") || name.endsWith(".tiff")) {
            String metadata = readAsciiTag(rasterFile, TAG_GDAL_METADATA);
            if (metadata != null) {
                return fromItems(TAG_ITEM, metadata);
            }
        }
        return null;
    }

    /**
     * @param pattern the pattern of an item, with the name as first group and the value as second.
     */
    private static RasterStats fromItems( Pattern pattern, String text ) {
        Map<String, Double> values = new HashMap<>();
        Matcher matcher = pattern.matcher(text);
        while( matcher.find() ) {
            try {
                values.putIfAbsent(matcher.group(1), Double.parseDouble(matcher.group(2).trim()));
            } catch (NumberFormatException e) {
                // not usable
            }
        }
        Double minimum = values.get("STATISTICS_MINIMUM");
        Double maximum = values.get("STATISTICS_MAXIMUM");
        Double mean = values.get("STATISTICS_MEAN");
        Double stddev = values.get("STATISTICS_STDDEV");
        if (minimum == null || maximum == null || mean == null || stddev == null) {
            return null;
        }
        return new RasterStats(minimum, maximum, mean, stddev, values.get("STATISTICS_VALID_PERCENT"));
    }

    /**
     * Compute the statistics of the valid cells of a raster, reading it by blocks of rows, so that a raster read by
     * windows is never all in memory.
     *
     * @return the statistics, or <code>null</code> if there are no valid cells.
     */
    public static RasterStats compute( HMRaster raster ) {
        int cols = raster.getCols();
        int rows = raster.getRows();
        int blockRows = Math.max(1, Math.min(rows, BLOCK_CELLS / Math.max(1, cols)));
        HMRasterStatistics statistics = new HMRasterStatistics();
        double[] buffer = null;
        for( int row = 0; row < rows; row += blockRows ) {
            int height = Math.min(blockRows, rows - row);
            buffer = raster.getValues(0, row, cols, height, buffer != null && buffer.length == cols * height ? buffer : null);
            for( int i = 0; i < cols * height; i++ ) {
                double value = buffer[i];
                if (!Double.isNaN(value) && !raster.isNovalue(value)) {
                    statistics.add(value);
                }
            }
        }
        if (statistics.getCount() == 0) {
            return null;
        }
        long cells = (long) cols * rows;
        return new RasterStats(statistics.getMin(), statistics.getMax(), statistics.getMean(), statistics.getStdDev(),
                100.0 * statistics.getCount() / cells);
    }

    /**
     * Read an ASCII tag of the first image of a TIFF or BigTIFF file, without reading the image.
     *
     * @return the text of the tag, or <code>null</code> if the file has no such tag or is not a TIFF.
     */
    static String readAsciiTag( File file, int tag ) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(file, "r")) {
            if (raf.length() < 16) {
                return null;
            }
            byte[] header = new byte[16];
            raf.readFully(header);
            ByteBuffer buffer = ByteBuffer.wrap(header);
            if (header[0] == 'I' && header[1] == 'I') {
                buffer.order(ByteOrder.LITTLE_ENDIAN);
            } else if (header[0] != 'M' || header[1] != 'M') {
                return null;
            }
            int version = buffer.getShort(2) & 0xFFFF;
            boolean bigTiff = version == 43;
            if (version != 42 && !bigTiff) {
                return null;
            }
            long ifdOffset = bigTiff ? buffer.getLong(8) : buffer.getInt(4) & 0xFFFFFFFFL;
            int entrySize = bigTiff ? 20 : 12;
            if (ifdOffset <= 0 || ifdOffset >= raf.length()) {
                return null;
            }
            raf.seek(ifdOffset);
            byte[] countBytes = new byte[bigTiff ? 8 : 2];
            raf.readFully(countBytes);
            ByteBuffer countBuffer = ByteBuffer.wrap(countBytes).order(buffer.order());
            long count = bigTiff ? countBuffer.getLong() : countBuffer.getShort() & 0xFFFF;
            if (count <= 0 || count > 10000) {
                return null;
            }
            byte[] entries = new byte[(int) count * entrySize];
            raf.readFully(entries);
            ByteBuffer entryBuffer = ByteBuffer.wrap(entries).order(buffer.order());
            for( int i = 0; i < count; i++ ) {
                int start = i * entrySize;
                int entryTag = entryBuffer.getShort(start) & 0xFFFF;
                if (entryTag != tag) {
                    continue;
                }
                int type = entryBuffer.getShort(start + 2) & 0xFFFF;
                long length = bigTiff ? entryBuffer.getLong(start + 4) : entryBuffer.getInt(start + 4) & 0xFFFFFFFFL;
                if (type != TYPE_ASCII || length <= 0 || length > MAX_TAG_BYTES) {
                    return null;
                }
                byte[] value = new byte[(int) length];
                int inline = bigTiff ? 8 : 4;
                int valueStart = start + (bigTiff ? 12 : 8);
                if (length <= inline) {
                    entryBuffer.position(valueStart);
                    entryBuffer.get(value);
                } else {
                    long offset = bigTiff ? entryBuffer.getLong(valueStart) : entryBuffer.getInt(valueStart) & 0xFFFFFFFFL;
                    if (offset + length > raf.length()) {
                        return null;
                    }
                    raf.seek(offset);
                    raf.readFully(value);
                }
                // without the closing zero
                int end = value.length;
                while( end > 0 && value[end - 1] == 0 ) {
                    end--;
                }
                return new String(value, 0, end, StandardCharsets.UTF_8);
            }
            return null;
        }
    }
}
