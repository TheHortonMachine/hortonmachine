package org.hortonmachine.gears.libs.modules;
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

import java.awt.Point;
import java.awt.image.WritableRaster;
import java.io.IOException;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A generic HM single band raster object.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class HMRaster implements AutoCloseable {
    private RegionMap regionMap;
    private int rows;
    private int cols;
    private double novalue = HMConstants.doubleNovalue;
    private RandomIter iter;
    private boolean isWritable = false;
    private GridGeometry2D gridGeometry;
    private WritableRaster writableRaster;
    private CoordinateReferenceSystem crs;
    private double xRes;
    private double yRes;

    public static interface RasterProcessor {
        void processCell( int col, int row, double value, int cols, int rows ) throws Exception;
    }

    /**
     * Build a raster baking a geotools gridCoverage.
     * 
     * @param coverage the coverage to use.
     * @return the HMRaster instance.
     */
    public static HMRaster fromGridCoverage( GridCoverage2D coverage ) {
        HMRaster hmRaster = new HMRaster();
        hmRaster.regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage);
        hmRaster.crs = coverage.getCoordinateReferenceSystem();
        hmRaster.gridGeometry = coverage.getGridGeometry();
        hmRaster.rows = hmRaster.regionMap.getRows();
        hmRaster.cols = hmRaster.regionMap.getCols();
        hmRaster.xRes = hmRaster.regionMap.getXres();
        hmRaster.yRes = hmRaster.regionMap.getYres();
        hmRaster.novalue = HMConstants.getNovalue(coverage);
        hmRaster.iter = CoverageUtilities.getRandomIterator(coverage);
        return hmRaster;
    }

    public static HMRaster writableFromTemplate( GridCoverage2D template ) {
        return writableFromTemplate(template, false);
    }

    /**
     * Build a raster using an existing geotools gridCoverage as template.
     * 
     *  <p>Region, crs, novalue and grid geomatry are taken from the template coverage.
     * 
     * @param template the coverage to use as template.
     * @param copyValues if <code>true</code>, also copy the values from the template.
     * @return the HMRaster instance.
     */
    public static HMRaster writableFromTemplate( GridCoverage2D template, boolean copyValues ) {
        HMRaster hmRaster = new HMRaster();
        hmRaster.isWritable = true;
        hmRaster.regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(template);
        hmRaster.crs = template.getCoordinateReferenceSystem();
        hmRaster.gridGeometry = template.getGridGeometry();
        hmRaster.rows = hmRaster.regionMap.getRows();
        hmRaster.cols = hmRaster.regionMap.getCols();
        hmRaster.xRes = hmRaster.regionMap.getXres();
        hmRaster.yRes = hmRaster.regionMap.getYres();
        hmRaster.novalue = HMConstants.getNovalue(template);
        hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, null, null,
                hmRaster.novalue);
        hmRaster.iter = CoverageUtilities.getWritableRandomIterator(hmRaster.writableRaster);

        if (copyValues) {
            RandomIter inIter = CoverageUtilities.getRandomIterator(template);
            for( int r = 0; r < hmRaster.rows; r++ ) {
                for( int c = 0; c < hmRaster.cols; c++ ) {
                    ((WritableRandomIter) hmRaster.iter).setSample(c, r, 0, inIter.getSampleDouble(c, r, 0));
                }
            }
        }
        return hmRaster;
    }

    /**
     * @return the columns of this raster.
     */
    public int getCols() {
        return cols;
    }

    /**
     * @return the rowsof this raster.
     */
    public int getRows() {
        return rows;
    }

    /**
     * @return the X resolution.
     */
    public double getXRes() {
        return xRes;
    }

    /**
     * @return the Y resolution.
     */
    public double getYRes() {
        return yRes;
    }

    /**
     * @return the novalue of this raster.
     */
    public double getNovalue() {
        return novalue;
    }

    /**
     * Check if a given value is a novalue for this raster.
     * 
     * @param valueToCheck the value to check.
     * @return <code>true</code>, if the value is a novalue.
     */
    public boolean isNovalue( double valueToCheck ) {
        return HMConstants.isNovalue(valueToCheck, novalue);
    }

    /**
     * Check if a given grid coordinate is inside the raster bounds.
     * 
     * @param col the column to check.
     * @param row the row to check.
     * @return <code>true</code> if the col/row position is contained.
     */
    public boolean isContained( int col, int row ) {
        return col >= 0 && col < cols && row >= 0 && row < rows;
    }

    /**
     * Get the value in a given col and row position.
     * 
     * @param col
     * @param row
     * @return the value of the raster or novalue if the point lies outside the bounds.
     */
    public double getValue( int col, int row ) {
        if (isContained(col, row)) {
            return iter.getSampleDouble(col, row, 0);
        } else {
            return novalue;
        }
    }

    /**
     * If the raster is writable, set a value in a given col and row position.
     * 
     * @param col
     * @param row
     * @param value
     * @throws IOException
     */
    public void setValue( int col, int row, double value ) throws IOException {
        if (!isWritable) {
            throw new IOException("The current HMRaster is not writable.");
        }
        if (isContained(col, row)) {
            ((WritableRandomIter) iter).setSample(col, row, 0, value);
        }
    }

    /**
     * Get the value in a given world coordinate. 
     * 
     * @param coordinate the world coordinate, assumed to be in the reference system of the raster.
     * @return the value.
     */
    public double getValue( Coordinate coordinate ) {
        int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, null);
        return getValue(colRow[0], colRow[1]);
    }

    /**
     * Get the grid space coordinate from a world coordinate.
     * 
     * @param coordinate the world coordinate, assumed to be in the reference system of the raster.
     * @return the grid space point.
     */
    public Point getPixel( Coordinate coordinate ) {
        Point p = new Point();
        CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, p);
        return p;
    }

    /**
     * Get the world coordinate from a col and row.
     * 
     * @param col
     * @param row
     * @return the world coordinate.
     */
    public Coordinate getWorld( int col, int row ) {
        Coordinate coordinate = CoverageUtilities.coordinateFromColRow(col, row, gridGeometry);
        return coordinate;
    }

    /**
     * Process the raster cell by cell.
     * 
     * @param pm optional progress monitor.
     * @param processName optional process name.
     * @param processor the processor object.
     */
    public void process( IHMProgressMonitor pm, String processName, RasterProcessor processor ) throws Exception {
        if (processName == null)
            processName = "Processing...";
        if (pm == null)
            pm = new DummyProgressMonitor();
        pm.beginTask(processName, rows);
        for( int row = 0; row < rows; row++ ) {
            for( int col = 0; col < cols; col++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                processor.processCell(col, row, getValue(col, row), cols, rows);
            }
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * Build a geotools gridCoverage.
     * 
     * @param name an optional name to give the coverage.
     * @return the gridCoverage.
     * @throws IOException
     */
    public GridCoverage2D buildCoverage( String name ) throws IOException {
        if (name == null) {
            name = "hmraster";
        }
        if (!isWritable) {
            throw new IOException("The current HMRaster is not writable.");
        }
        return CoverageUtilities.buildCoverageWithNovalue(name, writableRaster, regionMap, crs, novalue);
    }

    @Override
    public void close() throws Exception {
        if (iter != null) {
            iter.done();
        }
    }

}
