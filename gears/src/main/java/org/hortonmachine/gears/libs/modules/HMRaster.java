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
import java.util.ArrayList;
import java.util.List;

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
    private String name;
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
    private GridCoverage2D originalCoverage;

    /**
     * Raster map that can be initialized and used to count occurrences per cell.
     * This count can then be used to average the summ of multiple occurrences.
     */
    private HMRaster countRaster = null;

    public static interface RasterProcessor {
        void processCell( int col, int row, double value, int cols, int rows ) throws Exception;
    }

    /**
     * Build a raster baking a geotools gridCoverage.
     * 
     * @param coverage the coverage to use.
     * @return the HMRaster instance.
     */
    public static HMRaster fromGridCoverage( String name, GridCoverage2D coverage ) {
        HMRaster hmRaster = new HMRaster();
        hmRaster.originalCoverage = coverage;
        hmRaster.name = name != null ? name : coverage.getName().toString();
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

    public static HMRaster fromGridCoverage( GridCoverage2D coverage ) {
        return fromGridCoverage(null, coverage);
    }

//    public static HMRaster writableFromTemplate( String name, GridCoverage2D template ) {
//        return writableFromTemplate(name, template, false);
//    }
//
//    /**
//     * Build a raster using an existing geotools gridCoverage as template.
//     * 
//     *  <p>Region, crs, novalue and grid geomatry are taken from the template coverage.
//     * 
//     * @param template the coverage to use as template.
//     * @param copyValues if <code>true</code>, also copy the values from the template.
//     * @return the HMRaster instance.
//     */
//    public static HMRaster writableFromTemplate( String name, GridCoverage2D template, boolean copyValues ) {
//        return writableFromTemplate(name, template, null, copyValues);
//    }
//
//    public static HMRaster writableFromTemplate( String name, GridCoverage2D template, Class< ? > type, boolean copyValues ) {
//        if (type == null) {
//            type = Double.class;
//        }
//        HMRaster hmRaster = new HMRaster();
//        hmRaster.isWritable = true;
//        hmRaster.name = name;
//        hmRaster.regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(template);
//        hmRaster.crs = template.getCoordinateReferenceSystem();
//        hmRaster.gridGeometry = template.getGridGeometry();
//        hmRaster.rows = hmRaster.regionMap.getRows();
//        hmRaster.cols = hmRaster.regionMap.getCols();
//        hmRaster.xRes = hmRaster.regionMap.getXres();
//        hmRaster.yRes = hmRaster.regionMap.getYres();
//        hmRaster.novalue = HMConstants.getNovalue(template);
//        hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, type, null,
//                hmRaster.novalue);
//        hmRaster.iter = CoverageUtilities.getWritableRandomIterator(hmRaster.writableRaster);
//
//        if (copyValues) {
//            RandomIter inIter = CoverageUtilities.getRandomIterator(template);
//            if (type.isAssignableFrom(Double.class)) {
//                for( int r = 0; r < hmRaster.rows; r++ ) {
//                    for( int c = 0; c < hmRaster.cols; c++ ) {
//                        ((WritableRandomIter) hmRaster.iter).setSample(c, r, 0, inIter.getSampleDouble(c, r, 0));
//                    }
//                }
//            } else if (type.isAssignableFrom(Integer.class)) {
//                for( int r = 0; r < hmRaster.rows; r++ ) {
//                    for( int c = 0; c < hmRaster.cols; c++ ) {
//                        ((WritableRandomIter) hmRaster.iter).setSample(c, r, 0, inIter.getSample(c, r, 0));
//                    }
//                }
//            }
//        }
//        return hmRaster;
//    }
//
//    public static HMRaster writableIntegerFromTemplate( String name, GridCoverage2D template ) {
//        return writableIntegerFromTemplate(name, template, false);
//    }
//
//    public static HMRaster writableIntegerFromTemplate( String name, GridCoverage2D template, boolean copyValues ) {
//        return writableFromTemplate(name, template, Integer.class, copyValues);
//    }
//
//    /**
//     * Build a raster using region and crs.
//     * 
//     * @param name the name for the raster.
//     * @param region the region to use.
//     * @param crs the crs for the raster.
//     * @param noValue the novalue with which to pre-fill the raster.
//     * @return the HMRaster instance.
//     */
//    public static HMRaster writableFromRegionMap( String name, RegionMap region, CoordinateReferenceSystem crs, double noValue ) {
//        return writableFromRegionMap(name, region, crs, null, noValue);
//    }
//
//    public static HMRaster writableIntegerFromRegionMap( String name, RegionMap region, CoordinateReferenceSystem crs,
//            int noValue ) {
//        return writableFromRegionMap(name, region, crs, Integer.class, noValue);
//    }
//
//    public static HMRaster writableFromRegionMap( String name, RegionMap region, CoordinateReferenceSystem crs, Class< ? > type,
//            double noValue ) {
//        if (type == null) {
//            type = Double.class;
//        }
//        HMRaster hmRaster = new HMRaster();
//        hmRaster.name = name;
//        hmRaster.isWritable = true;
//        hmRaster.regionMap = region;
//        hmRaster.crs = crs;
//        hmRaster.gridGeometry = CoverageUtilities.gridGeometryFromRegionParams(region, crs);
//        hmRaster.rows = hmRaster.regionMap.getRows();
//        hmRaster.cols = hmRaster.regionMap.getCols();
//        hmRaster.xRes = hmRaster.regionMap.getXres();
//        hmRaster.yRes = hmRaster.regionMap.getYres();
//        hmRaster.novalue = noValue;
//        hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, type, null,
//                hmRaster.novalue);
//        hmRaster.iter = CoverageUtilities.getWritableRandomIterator(hmRaster.writableRaster);
//        return hmRaster;
//    }

    public String getName() {
        return name;
    }

    public RegionMap getRegionMap() {
        return RegionMap.fromRegionMap(regionMap);
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
     * Get the value in a given col and row position.
     * 
     * @param col
     * @param row
     * @return the value of the raster or novalue if the point lies outside the bounds.
     */
    public int getIntValue( int col, int row ) {
        if (isContained(col, row)) {
            return iter.getSample(col, row, 0);
        } else {
            return (int) novalue;
        }
    }

    /**
     * Get the value in a given world coordinate. 
     * 
     * @param coordinate the world coordinate, assumed to be in the reference system of the raster.
     * @return the value.
     */
    public int getIntValue( Coordinate coordinate ) {
        int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, null);
        return getIntValue(colRow[0], colRow[1]);
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

    public void setValue( int col, int row, int value ) throws IOException {
        if (!isWritable) {
            throw new IOException("The current HMRaster is not writable.");
        }
        if (isContained(col, row)) {
            ((WritableRandomIter) iter).setSample(col, row, 0, value);
        }
    }

    /**
     * Get the grid space coordinate from a world coordinate.
     * 
     * @param coordinate the world coordinate, assumed to be in the reference system of the raster.
     * @return the grid space point.
     */
    public Point getCell( Coordinate coordinate ) {
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
    public GridCoverage2D buildCoverage() throws IOException {
        if (originalCoverage != null) {
            return originalCoverage;
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

    /**
     * Writes the values of the coverage into the current raster, summing multiple occurrences.
     * 
     * @param pm optional Process monitor.
     * @param otherRaster the raster to map over the current raster.
     * @param valuesCountRaster a bit matrix that tracks how many values per pixels are recorded (they are summed, so the number is needed for averaging).
     * @throws IOException 
     */
    public void mapRaster( IHMProgressMonitor pm, HMRaster otherRaster ) throws IOException {
        mapRaster(pm, otherRaster, false);
    }

    /**
     * Writes the values of the coverage into the current raster, summing multiple occurrences.
     * 
     * @param pm optional Process monitor.
     * @param otherRaster the raster to map over the current raster.
     * @param valuesCountRaster a bit matrix that tracks how many values per pixels are recorded (they are summed, so the number is needed for averaging).
     * @throws IOException
     */
    public void mapRaster( IHMProgressMonitor pm, HMRaster otherRaster, boolean doValuesCountRaster ) throws IOException {
        if (pm == null)
            pm = new DummyProgressMonitor();

        RegionMap otherRegion = otherRaster.getRegionMap();
        Coordinate lowerLeft = otherRegion.getLowerLeft();
        Coordinate upperRight = otherRegion.getUpperRight();

        // find grid coordinates in the current region's space
        Point ll = getCell(lowerLeft);
        int fromCol = ll.x;
        if (fromCol < 0)
            fromCol = 0;
        int toRow = ll.y;
        Point ur = getCell(upperRight);
        int toCol = ur.x;
        int fromRow = ur.y;
        if (fromRow < 0)
            fromRow = 0;

        if (doValuesCountRaster && countRaster == null) {
            countRaster = new HMRasterWritableBuilder().setName("valuescount").setDoInteger(true).setRegion(regionMap).setCrs(crs)
                    .setInitialValue(0).build();
        }

        pm.beginTask("Patch raster...", toRow - fromRow); //$NON-NLS-1$
        // fill the points of the current raster picking form the
        // other raster via nearest neighbor interpolation
        for( int r = fromRow; r <= toRow; r++ ) {
            for( int c = fromCol; c <= toCol; c++ ) {
                if (isContained(c, r)) {
                    Coordinate coordinate = getWorld(c, r);
                    double value = otherRaster.getValue(coordinate);
                    if (!otherRaster.isNovalue(value)) {
                        if (countRaster != null) {
                            int count = countRaster.getIntValue(c, r);
                            count++;
                            countRaster.setValue(c, r, count);
                        }
                        double newValue = getValue(c, r);
                        if (!isNovalue(newValue)) {
                            newValue = newValue + value;
                        } else {
                            newValue = value;
                        }
                        setValue(c, r, newValue);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * @return the raster that contains the occurrences of data per cell.
     */
    public HMRaster getCountRaster() {
        return countRaster;
    }

    /**
     * Apply averaging based on the {@link #countRaster} map. <b>Note that this modifies the original raster.</b>
     * 
     * <p>This only makes sense if multiple mapping of the raster occurred (using {@link #mapRaster(IHMProgressMonitor, HMRaster, boolean)}. 
     * 
     * @param pm and optional progress monitor.
     * @throws Exception
     */
    public void applyCountAverage( IHMProgressMonitor pm ) throws Exception {
        if (pm == null)
            pm = new DummyProgressMonitor();

        pm.beginTask("Averaging raster...", rows);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double value = getValue(c, r);
                if (!isNovalue(value)) {
                    int count = countRaster.getIntValue(c, r);
                    double newValue = value / count;
                    setValue(c, r, newValue);
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * Get the values surrounding the current col/row.
     * 
     * @param distance the radius of the window to take. 
     * @param doCircular if <code>true</code> the window used is circular.
     * @return the read window as a list of coordinates x (col), y (row),z (raster value).
     */
    public List<Coordinate> getSurroundingCells( int currentCol, int currentRow, int distance, boolean doCircular ) {
        List<Coordinate> coords = new ArrayList<>();
        Coordinate current = new Coordinate(currentCol, currentRow);
        for( int r = -distance; r <= distance; r++ ) {
            int tmpRow = currentRow + r;
            for( int c = -distance; c <= distance; c++ ) {
                int tmpCol = currentCol + c;

                if (tmpCol == currentCol && tmpRow == currentRow) {
                    continue;
                }

                if (isContained(tmpCol, tmpRow)) {
                    Coordinate check = new Coordinate(tmpCol, tmpRow);
                    if (doCircular && check.distance(current) > distance) {
                        continue;
                    }
                    double value = getValue(tmpCol, tmpRow);
                    check.z = value;
                    coords.add(check);
                }
            }
        }
        return coords;
    }

    public void printData() {
        for( int row = 0; row < rows; row++ ) {
            for( int col = 0; col < cols; col++ ) {
                double value = getValue(col, row);
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }

    public static class HMRasterWritableBuilder {
        private String name = "newraster";

        private GridCoverage2D template = null;

        private boolean copyValues = false;

        private boolean doInteger = false;

        private RegionMap region;

        private CoordinateReferenceSystem crs;

        private Double noValue = null;

        private Double initialValue = null;

        private Integer initialIntValue = null;

        public HMRasterWritableBuilder setName( String name ) {
            this.name = name;
            return this;
        }

        public HMRasterWritableBuilder setTemplate( GridCoverage2D template ) {
            this.template = template;
            return this;
        }

        public HMRasterWritableBuilder setCopyValues( boolean copyValues ) {
            this.copyValues = copyValues;
            return this;
        }

        public HMRasterWritableBuilder setDoInteger( boolean doInteger ) {
            this.doInteger = doInteger;
            return this;
        }

        public HMRasterWritableBuilder setRegion( RegionMap region ) {
            this.region = region;
            return this;
        }

        public HMRasterWritableBuilder setCrs( CoordinateReferenceSystem crs ) {
            this.crs = crs;
            return this;
        }

        public HMRasterWritableBuilder setNoValue( double noValue ) {
            this.noValue = noValue;
            return this;
        }

        public HMRasterWritableBuilder setInitialValue( double initialNovalue ) {
            this.initialValue = initialNovalue;
            return this;
        }

        public HMRasterWritableBuilder setInitialValue( int initialNovalue ) {
            this.initialIntValue = initialNovalue;
            return this;
        }

        public HMRaster build() {
            if (template != null) {
                HMRaster hmRaster = new HMRaster();
                hmRaster.isWritable = true;
                hmRaster.name = name;
                hmRaster.regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(template);
                hmRaster.crs = template.getCoordinateReferenceSystem();
                hmRaster.gridGeometry = template.getGridGeometry();
                hmRaster.rows = hmRaster.regionMap.getRows();
                hmRaster.cols = hmRaster.regionMap.getCols();
                hmRaster.xRes = hmRaster.regionMap.getXres();
                hmRaster.yRes = hmRaster.regionMap.getYres();
                hmRaster.novalue = noValue != null ? noValue : HMConstants.getNovalue(template);

                if (doInteger) {
                    hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, Integer.class,
                            null, initialIntValue != null ? initialIntValue : hmRaster.novalue);
                } else {
                    hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, Double.class,
                            null, initialValue != null ? initialValue : hmRaster.novalue);
                }
                hmRaster.iter = CoverageUtilities.getWritableRandomIterator(hmRaster.writableRaster);

                if (copyValues) {
                    RandomIter inIter = CoverageUtilities.getRandomIterator(template);
                    if (!doInteger) {
                        for( int r = 0; r < hmRaster.rows; r++ ) {
                            for( int c = 0; c < hmRaster.cols; c++ ) {
                                ((WritableRandomIter) hmRaster.iter).setSample(c, r, 0, inIter.getSampleDouble(c, r, 0));
                            }
                        }
                    } else {
                        for( int r = 0; r < hmRaster.rows; r++ ) {
                            for( int c = 0; c < hmRaster.cols; c++ ) {
                                ((WritableRandomIter) hmRaster.iter).setSample(c, r, 0, inIter.getSample(c, r, 0));
                            }
                        }
                    }
                }
                return hmRaster;
            } else {
                HMRaster hmRaster = new HMRaster();
                hmRaster.name = name;
                hmRaster.isWritable = true;
                hmRaster.regionMap = region;
                hmRaster.crs = crs;
                hmRaster.gridGeometry = CoverageUtilities.gridGeometryFromRegionParams(region, crs);
                hmRaster.rows = hmRaster.regionMap.getRows();
                hmRaster.cols = hmRaster.regionMap.getCols();
                hmRaster.xRes = hmRaster.regionMap.getXres();
                hmRaster.yRes = hmRaster.regionMap.getYres();
                hmRaster.novalue = noValue != null ? noValue : HMConstants.doubleNovalue;
                if (doInteger) {
                    hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, Integer.class,
                            null, initialIntValue != null ? initialIntValue : hmRaster.novalue);
                } else {
                    hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, Double.class,
                            null, initialValue != null ? initialValue : hmRaster.novalue);
                }
                hmRaster.iter = CoverageUtilities.getWritableRandomIterator(hmRaster.writableRaster);
                return hmRaster;
            }
        }
    }

}
