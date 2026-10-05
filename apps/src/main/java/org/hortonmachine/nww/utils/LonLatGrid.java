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
package org.hortonmachine.nww.utils;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;

import gov.nasa.worldwind.geom.Sector;

/**
 * A coverage resampled on a regular longitude/latitude grid, as NWW wants elevations and surface
 * images, rows from north to south.
 */
public class LonLatGrid {
    private static final double METERS_PER_DEGREE = 111320.0;

    /**
     * Where the samples are placed in the sector.
     */
    public enum Registration {
        /** On the edges of the sector, as for NWW elevations. */
        GRID,
        /** In the centers of the pixels, as for images covering the sector. */
        PIXEL
    }

    private final int width;
    private final int height;
    private final Sector sector;
    private final double[] values;

    private LonLatGrid( int width, int height, Sector sector, double[] values ) {
        this.width = width;
        this.height = height;
        this.sector = sector;
        this.values = values;
    }

    /**
     * Resample a coverage.
     *
     * @param coverage the coverage, its CRS units are assumed to be meters.
     * @param registration where the samples are placed.
     * @param bilinear if <code>true</code>, bilinear interpolation between the cell centers, else
     *            the nearest cell.
     * @param oversampling how many samples per cell size, e.g. 1 for about the resolution of the
     *            coverage, 2 for half of it.
     * @param maxSize the maximum number of samples per side.
     * @return the grid, with NaN for novalues and outside of the coverage.
     */
    public static LonLatGrid resample( GridCoverage2D coverage, Registration registration, boolean bilinear,
            double oversampling, int maxSize ) throws Exception {
        try (HMRaster raster = HMRaster.fromGridCoverage(coverage)) {
            CoordinateReferenceSystem crs = raster.getCrs();
            CoordinateReferenceSystem lonLatCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);
            double west = raster.getRegionMap().getWest();
            double north = raster.getRegionMap().getNorth();
            double xRes = raster.getXRes();
            double yRes = raster.getYRes();
            int cols = raster.getCols();
            int rows = raster.getRows();

            ReferencedEnvelope extent = new ReferencedEnvelope(west, west + cols * xRes, north - rows * yRes, north, crs);
            ReferencedEnvelope lonLat = extent.transform(lonLatCrs, true, 20);
            double minLon = lonLat.getMinX();
            double maxLon = lonLat.getMaxX();
            double minLat = lonLat.getMinY();
            double maxLat = lonLat.getMaxY();

            double resolution = Math.min(xRes, yRes) / oversampling;
            double centerLat = Math.toRadians((minLat + maxLat) / 2);
            int width = (int) Math.ceil((maxLon - minLon) * METERS_PER_DEGREE * Math.cos(centerLat) / resolution) + 1;
            int height = (int) Math.ceil((maxLat - minLat) * METERS_PER_DEGREE / resolution) + 1;
            width = Math.max(2, Math.min(maxSize, width));
            height = Math.max(2, Math.min(maxSize, height));

            MathTransform toCoverage = CRS.findMathTransform(lonLatCrs, crs, true);
            double[] coverageValues = raster.getValues(0, 0, cols, rows, null);
            for( int i = 0; i < coverageValues.length; i++ ) {
                if (raster.isNovalue(coverageValues[i])) {
                    coverageValues[i] = Double.NaN;
                }
            }

            double[] values = new double[width * height];
            double[] rowCoordinates = new double[width * 2];
            boolean grid = registration == Registration.GRID;
            for( int j = 0; j < height; j++ ) {
                double lat = grid
                        ? maxLat - j * (maxLat - minLat) / (height - 1)
                        : maxLat - (j + 0.5) * (maxLat - minLat) / height;
                for( int i = 0; i < width; i++ ) {
                    rowCoordinates[i * 2] = grid
                            ? minLon + i * (maxLon - minLon) / (width - 1)
                            : minLon + (i + 0.5) * (maxLon - minLon) / width;
                    rowCoordinates[i * 2 + 1] = lat;
                }
                toCoverage.transform(rowCoordinates, 0, rowCoordinates, 0, width);
                for( int i = 0; i < width; i++ ) {
                    double col = (rowCoordinates[i * 2] - west) / xRes - 0.5;
                    double row = (north - rowCoordinates[i * 2 + 1]) / yRes - 0.5;
                    values[j * width + i] = bilinear
                            ? interpolate(coverageValues, cols, rows, col, row)
                            : nearest(coverageValues, cols, rows, col, row);
                }
            }
            return new LonLatGrid(width, height, Sector.fromDegrees(minLat, maxLat, minLon, maxLon), values);
        }
    }

    /**
     * Bilinear interpolation between the cell centers, NaN if outside or touching novalues. Up to
     * a cell beyond the outer cell centers the edge values are used, else the samples near the
     * edges of a coverage rotated with respect to longitude/latitude would be missing.
     */
    private static double interpolate( double[] values, int cols, int rows, double col, double row ) {
        if (col < -1 || row < -1 || col > cols || row > rows) {
            return Double.NaN;
        }
        col = Math.max(0, Math.min(cols - 1, col));
        row = Math.max(0, Math.min(rows - 1, row));
        int c0 = Math.min((int) Math.floor(col), Math.max(0, cols - 2));
        int r0 = Math.min((int) Math.floor(row), Math.max(0, rows - 2));
        int c1 = Math.min(c0 + 1, cols - 1);
        int r1 = Math.min(r0 + 1, rows - 1);
        double fc = col - c0;
        double fr = row - r0;
        double top = values[r0 * cols + c0] + fc * (values[r0 * cols + c1] - values[r0 * cols + c0]);
        double bottom = values[r1 * cols + c0] + fc * (values[r1 * cols + c1] - values[r1 * cols + c0]);
        // NaN propagates for novalues
        return top + fr * (bottom - top);
    }

    private static double nearest( double[] values, int cols, int rows, double col, double row ) {
        int c = (int) Math.floor(col + 0.5);
        int r = (int) Math.floor(row + 0.5);
        if (c < 0 || r < 0 || c >= cols || r >= rows) {
            return Double.NaN;
        }
        return values[r * cols + c];
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public Sector getSector() {
        return sector;
    }

    /**
     * @return the value of the sample of column i and row j (row 0 is north), NaN for novalues.
     */
    public double getValue( int i, int j ) {
        return values[j * width + i];
    }
}
