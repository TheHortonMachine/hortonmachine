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

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Exact statistics of the valid values of a raster (min, max, mean, population standard deviation),
 * accumulated while the raster is written, possibly from several threads.
 *
 * <p>Mean and standard deviation are accumulated with Welford's algorithm and partial results
 * are merged with Chan's formula, which stay precise also on billions of values.</p>
 */
public class HMRasterStatistics {
    private long count = 0;
    private double min = Double.POSITIVE_INFINITY;
    private double max = Double.NEGATIVE_INFINITY;
    private double mean = 0;
    private double m2 = 0;

    /**
     * Add a valid value. Not thread safe, use one instance per thread or tile and {@link #merge(HMRasterStatistics)}.
     */
    public void add( double value ) {
        count++;
        if (value < min) {
            min = value;
        }
        if (value > max) {
            max = value;
        }
        double delta = value - mean;
        mean += delta / count;
        m2 += delta * (value - mean);
    }

    /**
     * Merge partial statistics into these. Thread safe.
     */
    public synchronized void merge( HMRasterStatistics other ) {
        synchronized (other) {
            if (other.count == 0) {
                return;
            }
            if (count == 0) {
                count = other.count;
                min = other.min;
                max = other.max;
                mean = other.mean;
                m2 = other.m2;
                return;
            }
            long total = count + other.count;
            double delta = other.mean - mean;
            mean += delta * other.count / total;
            m2 += other.m2 + delta * delta * ((double) count * other.count / total);
            count = total;
            min = Math.min(min, other.min);
            max = Math.max(max, other.max);
        }
    }

    public synchronized long getCount() {
        return count;
    }

    public synchronized double getMin() {
        return min;
    }

    public synchronized double getMax() {
        return max;
    }

    public synchronized double getMean() {
        return mean;
    }

    /**
     * @return the population standard deviation, as GDAL reports it.
     */
    public synchronized double getStdDev() {
        return count > 0 ? Math.sqrt(m2 / count) : Double.NaN;
    }

    /**
     * @param totalCells the number of cells of the raster.
     * @return the GDAL_METADATA tag content with the statistics of band 1, or <code>null</code> if
     *          there are no valid values.
     */
    public synchronized String toGdalMetadataTag( long totalCells ) {
        if (count == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder("<GDALMetadata>\n");
        for( String[] item : items(totalCells) ) {
            sb.append("  <Item name=\"").append(item[0]).append("\" sample=\"0\">").append(item[1]).append("</Item>\n");
        }
        return sb.append("</GDALMetadata>").toString();
    }

    /**
     * Write the statistics of band 1 as GDAL sidecar file (<code>raster.tif.aux.xml</code>),
     * replacing an existing one. If there are no valid values, an existing sidecar is only removed.
     *
     * @param rasterFile the raster file.
     * @param totalCells the number of cells of the raster.
     */
    public synchronized void writeAuxXml( File rasterFile, long totalCells ) throws IOException {
        File auxFile = getAuxXmlFile(rasterFile);
        if (count == 0) {
            auxFile.delete();
            return;
        }
        StringBuilder sb = new StringBuilder("<PAMDataset>\n  <PAMRasterBand band=\"1\">\n    <Metadata>\n");
        for( String[] item : items(totalCells) ) {
            sb.append("      <MDI key=\"").append(item[0]).append("\">").append(item[1]).append("</MDI>\n");
        }
        sb.append("    </Metadata>\n  </PAMRasterBand>\n</PAMDataset>\n");
        Files.write(auxFile.toPath(), sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    /**
     * @return the GDAL sidecar file of a raster.
     */
    public static File getAuxXmlFile( File rasterFile ) {
        return new File(rasterFile.getAbsolutePath() + ".aux.xml");
    }

    private String[][] items( long totalCells ) {
        double validPercent = totalCells > 0 ? 100.0 * count / totalCells : 0;
        return new String[][]{//
                {"STATISTICS_MAXIMUM", String.valueOf(max)}, //
                {"STATISTICS_MEAN", String.valueOf(mean)}, //
                {"STATISTICS_MINIMUM", String.valueOf(min)}, //
                {"STATISTICS_STDDEV", String.valueOf(getStdDev())}, //
                {"STATISTICS_VALID_PERCENT", String.valueOf(validPercent)}};
    }
}
