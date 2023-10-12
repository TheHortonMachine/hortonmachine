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
import java.util.stream.IntStream;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

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
    private int intNovalue = HMConstants.intNovalue;
    private short shortNovalue = HMConstants.shortNovalue;
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

    public static interface RasterCellProcessor {
        void processCell( int col, int row, double value, int cols, int rows ) throws Exception;
    }

    public static interface RasterRowProcessor {
        void processRow( int row, int cols, int rows ) throws Exception;
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
        hmRaster.intNovalue = (int) HMConstants.getNovalue(coverage);
        hmRaster.shortNovalue = (short) HMConstants.getNovalue(coverage);
        hmRaster.iter = CoverageUtilities.getRandomIterator(coverage);
        return hmRaster;
    }

    public static HMRaster fromGridCoverage( GridCoverage2D coverage ) {
        return fromGridCoverage(null, coverage);
    }

    public String getName() {
        return name;
    }

    public RegionMap getRegionMap() {
        return RegionMap.fromRegionMap(regionMap);
    }

    public GridGeometry2D getGridGeometry() {
        return gridGeometry;
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
     * @return the {@link CoordinateReferenceSystem} of the raster.
     */
    public CoordinateReferenceSystem getCrs() {
        return crs;
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

    public boolean isNovalue( int valueToCheck ) {
        return HMConstants.isNovalue(valueToCheck, intNovalue);
    }

    public boolean isNovalue( short valueToCheck ) {
        return HMConstants.isNovalue(valueToCheck, shortNovalue);
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
            return intNovalue;
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
     * Get the value in a given col and row position.
     * 
     * @param col
     * @param row
     * @return the value of the raster or novalue if the point lies outside the bounds.
     */
    public short getShortValue( int col, int row ) {
        if (isContained(col, row)) {
            return (short) iter.getSample(col, row, 0);
        } else {
            return shortNovalue;
        }
    }

    /**
     * Get the value in a given world coordinate. 
     * 
     * @param coordinate the world coordinate, assumed to be in the reference system of the raster.
     * @return the value.
     */
    public short getShortValue( Coordinate coordinate ) {
        int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, null);
        return getShortValue(colRow[0], colRow[1]);
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

    public void setValue( int col, int row, short value ) throws IOException {
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
    public void process( IHMProgressMonitor pm, String processName, RasterCellProcessor processor ) throws Exception {
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

    public void processByRow( IHMProgressMonitor pm, String processName, RasterRowProcessor processor, boolean doParallel )
            throws Exception {
        if (processName == null)
            processName = "Processing...";
        if (pm == null)
            pm = new DummyProgressMonitor();
        pm.beginTask(processName, rows);

        IntStream rowsStream = IntStream.range(0, rows);
        if (doParallel) {
            rowsStream = rowsStream.parallel();
        }
        IHMProgressMonitor _pm = pm;
        rowsStream.forEach(row -> {
            if (!_pm.isCanceled()) {
                try {
                    processor.processRow(row, cols, rows);
                    _pm.worked(1);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
        });
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
     * <p> In this case a bit matrix that tracks how many values per pixels are recorded is created 
     * and accessible through {@link #getCountRaster()}. 
     * Sine the values are summed, the number is needed for averaging.
     * 
     * @param pm optional Process monitor.
     * @param otherRaster the raster to map over the current raster.
     * @throws IOException 
     */
    public void mapRasterSum( IHMProgressMonitor pm, HMRaster otherRaster ) throws Exception {
        mapRaster(pm, otherRaster, 0);
    }

    /**
     * Writes the values of the coverage into the current raster, only where the current does not have values.
     * 
     * @param pm optional Process monitor.
     * @param otherRaster the raster to map over the current raster.
     * @throws IOException 
     */
    public void mapRasterSubst( IHMProgressMonitor pm, HMRaster otherRaster ) throws Exception {
        mapRaster(pm, otherRaster, 1);
    }

    /**
     * Writes the values of the coverage into the current raster, summing multiple occurrences.
     * 
     * @param pm optional Process monitor.
     * @param otherRaster the raster to map over the current raster.
     * @param valuesCountRaster a bit matrix that tracks how many values per pixels are recorded (they are summed, so the number is needed for averaging).
     * @param mergeMode optional merge mode parameter. null/0 = sum of both, 1 = place other only in novalues
     * @throws IOException
     */
    public void mapRaster( IHMProgressMonitor pm, HMRaster otherRaster, Integer mergeMode ) throws Exception {
        if (pm == null)
            pm = new DummyProgressMonitor();

        int _mergeMode = 0;
        if (mergeMode != null) {
            _mergeMode = mergeMode;
        }

        RegionMap otherRegion = otherRaster.getRegionMap();
        Coordinate lowerLeft = otherRegion.getLowerLeft();
        Coordinate upperRight = otherRegion.getUpperRight();

        // convert to current region crs
        CoordinateReferenceSystem sourceCRS = otherRaster.getCrs();
        CoordinateReferenceSystem targetCRS = getCrs();

        if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
            lowerLeft = JTS.transform(lowerLeft, null, transform);
            upperRight = JTS.transform(upperRight, null, transform);
        }

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

        if (_mergeMode == 0 && countRaster == null) {
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
                    double otherRasterValue = otherRaster.getValue(coordinate);
                    if (!otherRaster.isNovalue(otherRasterValue)) {
                        if (countRaster != null) {
                            int count = countRaster.getIntValue(c, r);
                            count++;
                            countRaster.setValue(c, r, count);
                        }
                        double thisRasterValue = getValue(c, r);

                        boolean thisIsNovalue = isNovalue(thisRasterValue);
                        if (thisIsNovalue) {
                            // if the current is novalue, then use the other
                            thisRasterValue = otherRasterValue;
                        } else if (_mergeMode == 0) {
                            // thisvalue is NOT novalue + mergemode is sum
                            thisRasterValue = thisRasterValue + otherRasterValue;
                        } else {
                            thisRasterValue = otherRasterValue;
//                            throw new ModelsRuntimeException("This should never happen.", this);
                        }
                        setValue(c, r, thisRasterValue);
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

    /**
     * In the case this is a flowdirections map gets all surrounding cells that <b>DO</b> flow into the given cell.
     * 
     * @return the [x,y] of the cells that flow into the given cell.
     */
    public List<int[]> getEnteringFlowCells( int col, int row ) {
        ArrayList<int[]> enteringNodes = new ArrayList<>();
        Direction[] orderedDirs = Direction.getOrderedDirs();
        for( Direction direction : orderedDirs ) {
            int newCol = col + direction.col;
            int newRow = row + direction.row;
            short flowValue = getShortValue(newCol, newRow);
            if (flowValue == direction.getEnteringFlow()) {
                enteringNodes.add(new int[]{newCol, newRow});
            }
        }
        return enteringNodes;
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

        private HMRaster template = null;

        private boolean copyValues = false;

        private boolean doInteger = false;

        private boolean doShort = false;

        private RegionMap region;

        private CoordinateReferenceSystem crs;

        private Double noValue = null;

        private Double initialValue = null;

        private Integer initialIntValue = null;

        private Short initialShortValue = null;

        private double[][] dataMatrix = null;

        public HMRasterWritableBuilder setName( String name ) {
            this.name = name;
            return this;
        }

        public HMRasterWritableBuilder setTemplate( GridCoverage2D template ) {
            this.template = HMRaster.fromGridCoverage(template);
            return this;
        }

        public HMRasterWritableBuilder setTemplate( HMRaster template ) {
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

        public HMRasterWritableBuilder setDoShort( boolean doShort ) {
            this.doShort = doShort;
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

        public HMRasterWritableBuilder setInitialValue( double initialValue ) {
            this.initialValue = initialValue;
            return this;
        }

        public HMRasterWritableBuilder setInitialValue( int initialValue ) {
            this.initialIntValue = initialValue;
            return this;
        }

        public HMRasterWritableBuilder setInitialValue( short initialValue ) {
            this.initialShortValue = initialValue;
            return this;
        }

        public HMRasterWritableBuilder setData( double[][] dataMatrix ) {
            this.dataMatrix = dataMatrix;
            return this;
        }

        public HMRaster build() {
            if (template != null) {
                HMRaster hmRaster = new HMRaster();
                hmRaster.isWritable = true;
                hmRaster.name = name;
                hmRaster.regionMap = template.getRegionMap();
                hmRaster.crs = template.getCrs();
                hmRaster.gridGeometry = template.getGridGeometry();
                hmRaster.rows = hmRaster.regionMap.getRows();
                hmRaster.cols = hmRaster.regionMap.getCols();
                hmRaster.xRes = hmRaster.regionMap.getXres();
                hmRaster.yRes = hmRaster.regionMap.getYres();
                hmRaster.novalue = noValue != null ? noValue : template.getNovalue();
                hmRaster.intNovalue = noValue != null ? noValue.intValue() : (int) template.getNovalue();
                hmRaster.shortNovalue = noValue != null ? noValue.shortValue() : (short) template.getNovalue();

                if (doInteger) {
                    hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, Integer.class,
                            null, initialIntValue != null ? initialIntValue : hmRaster.intNovalue);
                } else if (doShort) {
                    hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, Short.class,
                            null, initialShortValue != null ? initialShortValue : hmRaster.shortNovalue);
                } else {
                    hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, Double.class,
                            null, initialValue != null ? initialValue : hmRaster.novalue);
                }
                hmRaster.iter = CoverageUtilities.getWritableRandomIterator(hmRaster.writableRaster);

                if (copyValues) {
//                    RandomIter inIter = CoverageUtilities.getRandomIterator(template);
                    if (doInteger) {
                        for( int r = 0; r < hmRaster.rows; r++ ) {
                            for( int c = 0; c < hmRaster.cols; c++ ) {
                                ((WritableRandomIter) hmRaster.iter).setSample(c, r, 0, template.getValue(c, r));
                            }
                        }
                    } else if (doShort) {
                        for( int r = 0; r < hmRaster.rows; r++ ) {
                            for( int c = 0; c < hmRaster.cols; c++ ) {
                                ((WritableRandomIter) hmRaster.iter).setSample(c, r, 0, (short) template.getValue(c, r));
                            }
                        }
                    } else {
                        for( int r = 0; r < hmRaster.rows; r++ ) {
                            for( int c = 0; c < hmRaster.cols; c++ ) {
                                ((WritableRandomIter) hmRaster.iter).setSample(c, r, 0, template.getValue(c, r));
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
                hmRaster.intNovalue = noValue != null ? noValue.intValue() : HMConstants.intNovalue;
                hmRaster.shortNovalue = noValue != null ? noValue.shortValue() : HMConstants.shortNovalue;

                if (doInteger) {
                    hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, Integer.class,
                            null, initialIntValue != null ? initialIntValue : hmRaster.intNovalue);
                } else if (doShort) {
                    hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, Short.class,
                            null, initialShortValue != null ? initialShortValue : hmRaster.shortNovalue);
                } else {
                    hmRaster.writableRaster = CoverageUtilities.createWritableRaster(hmRaster.cols, hmRaster.rows, Double.class,
                            null, initialValue != null ? initialValue : hmRaster.novalue);
                }
                hmRaster.iter = CoverageUtilities.getWritableRandomIterator(hmRaster.writableRaster);

                if (dataMatrix != null) {
                    for( int r = 0; r < hmRaster.rows; r++ ) {
                        for( int c = 0; c < hmRaster.cols; c++ ) {
                            ((WritableRandomIter) hmRaster.iter).setSample(c, r, 0, dataMatrix[r][c]);
                        }
                    }
                }
                return hmRaster;
            }
        }
    }

}
