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

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;

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
    private int startRow;
    private int startCol;
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
     * Support Rasters for the aggregation methods.
     */
    private HMRaster sumRaster = null;
    private HMRaster countRaster = null;
    private CategoriesInCell[][] categoriesRaster = null;


    /**
     * Enumeration representing the merge modes for combining raster values.
     * 
     * <p>These are used in the {@link #mapRaster(IHMProgressMonitor, HMRaster, MergeMode)} method.
     */
    public static enum MergeMode {
        /**
         * Sum the values of the mapped rasters.
         */
        SUM, 
        
        /**
         * Average the values of the mapped rasters.
         */
        AVG, 
        
        /**
         * Substitute the values everytime. Last values wins.
         */
        SUBSTITUTE,
        
        /**
         * Substitute the values, if they are valid values. Last values wins.
         */
        SUBSTITUTE_IGNORE_NOVALUE,

        /**
         * Insert the values only if the cell contains novalue. First value wins.
         */
        INSERT_ON_NOVALUE,

        /**
         * Collects all the values in the cell and keeps track of the post present one.
         * 
         * <p>This requires the call of {@link #applyMostPopular()} to perform the proper substitution.
         */
        MOST_POPULAR_VALUE
    }

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
        hmRaster.startRow = hmRaster.regionMap.startRow;
        hmRaster.startCol = hmRaster.regionMap.startCol;
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
    
    public RandomIter getIter() {
        return iter;
    }

    /**
     * @return the start row of this raster.
     */
    public int getStartRow() {
        return startRow;
    }

    /**
     * @return the start column of this raster.
     */
    public int getStartCol() {
        return startCol;
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
        return col >= startCol && col < cols && row >= startRow && row < rows;
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
        for( int row = startRow; row < rows + startRow; row++ ) {
            for( int col = startCol; col < cols + startCol; col++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                processor.processCell(col, row, getValue(col, row), cols, rows);
            }
            pm.worked(1);
        }
        pm.done();
    }

    public void processWindow( int col, int row, int windowSize, RasterCellProcessor processor ) throws Exception {
        if ( windowSize % 2 == 0 ) {
            windowSize++;
        }
        int delta = (windowSize - 1) / 2;

        for( int wRow = row - delta; wRow < row + delta; wRow++ ) {
            for( int wCol = col - delta; wCol < col + delta; wCol++ ) {
                if(!isContained(wCol, wRow)){
                    continue;
                }
                processor.processCell(wCol, wRow, getValue(wCol, wRow), windowSize, windowSize);
            }
        }
    }

    public void processByRow( IHMProgressMonitor pm, String processName, RasterRowProcessor processor, boolean doParallel )
            throws Exception {
        if (processName == null)
            processName = "Processing...";
        if (pm == null)
            pm = new DummyProgressMonitor();
        pm.beginTask(processName, rows);

        IntStream rowsStream = IntStream.range(startRow, rows + startRow);
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
     * @param pm optional Process monitor.
     * @param otherRaster the raster to map over the current raster.
     * @param valuesCountRaster a bit matrix that tracks how many values per pixels are recorded (they are summed, so the number is needed for averaging).
     * @param mergeMode optional merge mode parameter. If null MergeMode.SUM is used.
     * @throws IOException
     */
    public void mapRaster( IHMProgressMonitor pm, HMRaster otherRaster, MergeMode mergeMode ) throws Exception {
        if (pm == null)
            pm = new DummyProgressMonitor();

        MergeMode _mergeMode = MergeMode.SUM;
        if (mergeMode != null) {
            _mergeMode = mergeMode;
        }

        RegionMap otherRegion = otherRaster.getRegionMap();
        Coordinate lowerLeft = otherRegion.getLowerLeft();
        Coordinate upperRight = otherRegion.getUpperRight();
        Coordinate lowerLeftCellCenter = new Coordinate(lowerLeft.x + otherRegion.getXres() / 2, lowerLeft.y + otherRegion.getYres() / 2);
        Coordinate upperRightCellCenter = new Coordinate(upperRight.x - otherRegion.getXres() / 2, upperRight.y - otherRegion.getYres() / 2);

        // convert to current region crs
        CoordinateReferenceSystem sourceCRS = otherRaster.getCrs();
        CoordinateReferenceSystem targetCRS = getCrs();

        if (!CRS.equalsIgnoreMetadata(sourceCRS, targetCRS)) {
            MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
            lowerLeftCellCenter = JTS.transform(lowerLeftCellCenter, null, transform);
            upperRightCellCenter = JTS.transform(upperRightCellCenter, null, transform);
        }

        // find grid coordinates in the current region's space
        Point ll = getCell(lowerLeftCellCenter);
        int fromCol = ll.x;
        if (fromCol < 0)
            fromCol = 0;
        int toRow = ll.y;
        Point ur = getCell(upperRightCellCenter);
        int toCol = ur.x;
        int fromRow = ur.y;
        if (fromRow < 0)
            fromRow = 0;

        if (_mergeMode == MergeMode.AVG && sumRaster == null) {
            // set the current sum and count to the current raster to start with
            sumRaster = new HMRasterWritableBuilder().setTemplate(this).setCopyValues(true).setName("sum").build();
            countRaster = new HMRasterWritableBuilder().setName("valuescount").setDoShort(true).setRegion(regionMap).setCrs(crs)
                    .setInitialValue((short) 1).build();
        } else if (_mergeMode == MergeMode.MOST_POPULAR_VALUE && categoriesRaster == null) {
            categoriesRaster = new CategoriesInCell[rows][cols];
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
                        double thisRasterValue = getValue(c, r);

                        boolean thisIsNovalue = isNovalue(thisRasterValue);
                        if (!thisIsNovalue && mergeMode == MergeMode.INSERT_ON_NOVALUE){
                            // value exists already, ignore the new one
                            continue;
                        } 

                        if (_mergeMode == MergeMode.SUBSTITUTE || mergeMode == MergeMode.INSERT_ON_NOVALUE) {
                            // you want to always substitute, use the other
                            thisRasterValue = otherRasterValue;
                        } else if (_mergeMode == MergeMode.SUBSTITUTE_IGNORE_NOVALUE) {
                            // substitute only if the other value is not a novalue
                            if (!otherRaster.isNovalue(otherRasterValue)) {
                                thisRasterValue = otherRasterValue;
                            }
                        } else if (_mergeMode == MergeMode.INSERT_ON_NOVALUE) {
                            // do nothing, just insert the value if it is a novalue
                            if (thisIsNovalue) {
                                thisRasterValue = otherRasterValue;
                            }
                        } else if (_mergeMode == MergeMode.SUM) {
                            // just sum with any previous value
                            if (thisIsNovalue) {
                                thisRasterValue = 0;
                            }
                            thisRasterValue = thisRasterValue + otherRasterValue;
                        } else if (_mergeMode == MergeMode.AVG) {
                            double sum = sumRaster.getValue(c, r);
                            short count = countRaster.getShortValue(c, r);
                            if(sumRaster.isNovalue(sum)){
                                sum = 0;
                                count = 0;
                            }
                            sum += otherRasterValue;
                            sumRaster.setValue(c, r, sum);
                            count++;
                            countRaster.setValue(c, r, count);
                            thisRasterValue = sum / count;
                        } else if (_mergeMode == MergeMode.MOST_POPULAR_VALUE) {
                            CategoriesInCell categoriesInCell = categoriesRaster[r][c];
                            if(categoriesInCell == null){
                                categoriesInCell = new CategoriesInCell();
                                if(!thisIsNovalue){
                                    categoriesInCell.addValue((int) thisRasterValue);
                                }
                                categoriesRaster[r][c] = categoriesInCell;
                            }
                            categoriesInCell.addValue((int) otherRasterValue);
                            thisRasterValue = categoriesInCell.mostPresentValue;
                        } else {
                            // thisRasterValue = otherRasterValue;
                           throw new ModelsRuntimeException("This should never happen.", this);
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
        for( int row = startRow; row < rows + startRow; row++ ) {
            for( int col = startCol; col < cols + startCol; col++ ) {
                double value = getValue(col, row);
                System.out.print(value + " ");
            }
            System.out.println();
        }
    }

    private static class CategoriesInCell {
        int mostPresentValue = 0;
        private int valuesCount = 0;

        private int arraySize = 3;
        private int[] allValues = new int[arraySize];

        void addValue(int value) {
            allValues[valuesCount] = value;
            valuesCount++;
            if (valuesCount == arraySize) {
                // we need to grow the array
                int[] newArray = new int[arraySize + 2];
                System.arraycopy(allValues, 0, newArray, 0, arraySize);
                arraySize = arraySize + 2;
                allValues = newArray;
            }
            if(valuesCount == 1){
                mostPresentValue = value;
            }else{
                // find the most present value in the array
                mostPresentValue = NumericsUtilities.getMostPopular(allValues, valuesCount);
            }
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
