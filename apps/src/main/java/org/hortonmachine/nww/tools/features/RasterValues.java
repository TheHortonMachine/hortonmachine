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
package org.hortonmachine.nww.tools.features;

import java.io.File;

import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.geometry.Position2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.locationtech.jts.geom.Coordinate;

/**
 * The values of the bands of a raster at a point.
 *
 * @param file the raster file, <code>null</code> for the remote rasters.
 * @param x the easting of the point, in the CRS of the raster.
 * @param y the northing of the point, in the CRS of the raster.
 * @param col the column of the cell, from 0.
 * @param row the row of the cell, from 0.
 * @param values the values of the bands, <code>null</code> if the point is outside of the raster.
 * @param novalue the novalue of the raster, <code>null</code> if it has none.
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record RasterValues( File file, double x, double y, int col, int row, double[] values, Double novalue ) {

    /**
     * @return <code>true</code> if the point is on the raster.
     */
    public boolean isInside() {
        return values != null;
    }

    /**
     * @param value a value of a band.
     * @return <code>true</code> if it is the novalue of the raster, or NaN.
     */
    public boolean isNovalue( double value ) {
        return Double.isNaN(value) || (novalue != null && value == novalue);
    }

    /**
     * Reads the values of rasters at points, keeping the last raster open, since the cells are read
     * lazily: only the tile of the point is read, also of the remote rasters, as the cloud
     * optimized GeoTIFFs.
     */
    public static class Reader {
        /** The raster open: its file, or the reader of a remote raster. */
        private Object key;
        private GridCoverage2D coverage;
        private HMCrsTransformer transformer;

        /**
         * @param rasterFile the raster file.
         * @param lon the longitude of the point.
         * @param lat the latitude of the point.
         * @return the values at the point.
         * @throws Exception if the raster can't be read.
         */
        public synchronized RasterValues read( File rasterFile, double lon, double lat ) throws Exception {
            if (!rasterFile.equals(key)) {
                open(rasterFile, OmsRasterReader.readRaster(rasterFile.getAbsolutePath()));
            }
            return evaluate(rasterFile, lon, lat);
        }

        /**
         * @param remoteReader the reader of a remote raster, as the one of a layer.
         * @param lon the longitude of the point.
         * @param lat the latitude of the point.
         * @return the values at the point, without file.
         * @throws Exception if the raster can't be read.
         */
        public synchronized RasterValues read( AbstractGridCoverage2DReader remoteReader, double lon, double lat )
                throws Exception {
            if (remoteReader != key) {
                // read lazily: the cells of the point are read when evaluated
                open(remoteReader, remoteReader.read((GeneralParameterValue[]) null));
            }
            return evaluate(null, lon, lat);
        }

        private void open( Object newKey, GridCoverage2D newCoverage ) throws Exception {
            close();
            coverage = newCoverage;
            key = newKey;
            CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
            transformer = crs == null || HMCrsRegistry.crsEquals(crs, NwwUtilities.GPS_CRS)
                    ? null
                    : new HMCrsTransformer(NwwUtilities.GPS_CRS, crs);
        }

        private RasterValues evaluate( File rasterFile, double lon, double lat ) throws Exception {
            Coordinate point = new Coordinate(lon, lat);
            if (transformer != null) {
                point = transformer.transform(point);
            }
            GridGeometry2D gridGeometry = coverage.getGridGeometry();
            GridCoordinates2D cell = gridGeometry
                    .worldToGrid(new Position2D(coverage.getCoordinateReferenceSystem(), point.x, point.y));
            GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
            double[] values = null;
            if (gridRange.contains(cell.x, cell.y)) {
                values = coverage.evaluate(cell, (double[]) null);
            }
            return new RasterValues(rasterFile, point.x, point.y, cell.x, cell.y, values, CoverageUtilities.getNovalue(coverage));
        }

        /**
         * Close the open raster.
         */
        public synchronized void close() {
            if (coverage != null) {
                coverage.dispose(true);
            }
            coverage = null;
            key = null;
            transformer = null;
        }
    }
}
