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

import org.hortonmachine.gears.libs.modules.hmraster.HMRasterFileWindowed;
import org.hortonmachine.gears.libs.modules.hmraster.HMRasterTile;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.awt.image.WritableRenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;

import org.eclipse.imagen.iterator.RandomIter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.modules.r.scanline.OmsScanLineRasterizer;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

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
    private byte byteNovalue = HMConstants.byteNovalue;
    
    private RandomIter iter;
    /**
     * The thread that uses {@link #iter}, the one that built the raster.
     *
     * <p>The image iterators keep the tile of the last accessed cell, so they can't be shared
     * between threads: the other threads get their own, see {@link #readIter()}. They are kept
     * here, not in thread locals, so that they don't keep the image alive after the raster is
     * gone, through the threads of the pools.</p>
     */
    private Thread iterOwner;
    private final Map<Thread, RandomIter> otherThreadsIters = new ConcurrentHashMap<>();
    /**
     * The tiles of the writable image, [tileRow][tileCol], written directly.
     *
     * <p>No writable iterators: they check tiles out of the image with
     * {@link WritableRenderedImage#getWritableTile(int, int)}, whose writers count is not thread
     * safe, so writes from several threads end up with the tiles locked (null). Writing into
     * the tiles is safe as long as the threads write different cells.</p>
     */
    private WritableRaster[][] writableTiles;
    private int tileWidth;
    private int tileHeight;
    private int tileGridXOffset;
    private int tileGridYOffset;
    private int minTileX;
    private int minTileY;
    private boolean isWritable = false;
    private GridGeometry2D gridGeometry;
    private WritableRenderedImage writableImage;
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

    public static interface RasterTileProcessor {
        void processTile( HMRasterTile tile ) throws Exception;
    }

    /**
     * Build a raster backing a geotools gridCoverage.
     * 
     * @param name an optional name to give to the raster. If null, the name of the coverage is used.
     * @param coverage the coverage to use.
     * @return the HMRaster instance.
     */
    public static HMRaster fromGridCoverage( String name, GridCoverage2D coverage ) {
        HMRaster hmRaster = new HMRaster();
        hmRaster.initFromCoverage(name, coverage);
        return hmRaster;
    }

    /**
     * Initialize the raster as a read only view on a coverage.
     * 
     * @param name an optional name to give to the raster. If null, the name of the coverage is used.
     * @param coverage the coverage to use.
     */
    protected void initFromCoverage( String name, GridCoverage2D coverage ) {
        originalCoverage = coverage;
        this.name = name != null ? name : coverage.getName().toString();
        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage);
        crs = coverage.getCoordinateReferenceSystem();
        gridGeometry = coverage.getGridGeometry();
        startRow = regionMap.startRow;
        startCol = regionMap.startCol;
        rows = regionMap.getRows();
        cols = regionMap.getCols();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();
        novalue = HMConstants.getNovalue(coverage);
        intNovalue = (int) HMConstants.getNovalue(coverage);
        shortNovalue = (short) HMConstants.getNovalue(coverage);
        byteNovalue = (byte) HMConstants.getNovalue(coverage);
        iter = CoverageUtilities.getRandomIterator(coverage);
        iterOwner = Thread.currentThread();
    }

    /**
     * @return the read iterator of the calling thread.
     */
    private RandomIter readIter() {
        Thread thread = Thread.currentThread();
        if (thread == iterOwner) {
            return iter;
        }
        RandomIter threadIter = otherThreadsIters.get(thread);
        if (threadIter == null) {
            threadIter = otherThreadsIters.computeIfAbsent(thread, t -> CoverageUtilities.getRandomIterator(getRenderedImage()));
        }
        return threadIter;
    }

    /**
     * Set the writable image of the raster, with the iterator to read it and its tiles to write it.
     *
     * @param image the image.
     */
    private void initWritableImage( WritableRenderedImage image ) {
        writableImage = image;
        iter = CoverageUtilities.getRandomIterator(image);
        iterOwner = Thread.currentThread();
        tileWidth = image.getTileWidth();
        tileHeight = image.getTileHeight();
        tileGridXOffset = image.getTileGridXOffset();
        tileGridYOffset = image.getTileGridYOffset();
        minTileX = image.getMinTileX();
        minTileY = image.getMinTileY();
        writableTiles = new WritableRaster[image.getNumYTiles()][image.getNumXTiles()];
        for( int ty = 0; ty < writableTiles.length; ty++ ) {
            for( int tx = 0; tx < writableTiles[ty].length; tx++ ) {
                // getTile does not touch the writers count; tiles of writable images are writable
                Raster tile = image.getTile(minTileX + tx, minTileY + ty);
                writableTiles[ty][tx] = tile instanceof WritableRaster
                        ? (WritableRaster) tile
                        : image.getWritableTile(minTileX + tx, minTileY + ty);
            }
        }
    }

    /**
     * @param col the col of a cell of the image.
     * @param row the row of a cell of the image.
     * @return the writable tile containing the cell.
     */
    private WritableRaster writableTile( int col, int row ) {
        int tileX = Math.floorDiv(col - tileGridXOffset, tileWidth) - minTileX;
        int tileY = Math.floorDiv(row - tileGridYOffset, tileHeight) - minTileY;
        return writableTiles[tileY][tileX];
    }

    /**
     * Build a read only raster on a GeoTIFF file too large to be loaded in memory.
     * 
     * <p>Nothing is read at creation. Blocks read with {@link #getValues(int, int, int, int, double[])} 
     * are decoded directly from the file, in parallel when called from several threads.</p>
     * 
     * @param path the path to the GeoTIFF file.
     * @return the file backed raster.
     * @throws Exception
     */
    public static HMRaster fromFileWindowed( String path ) throws Exception {
        return new HMRasterFileWindowed(path);
    }

    /**
     * Build a raster backing a geotools gridCoverage.
     * 
     * @param coverage the coverage to use.
     * @return the HMRaster instance.
     */
    public static HMRaster fromGridCoverage( GridCoverage2D coverage ) {
        return fromGridCoverage(coverage, null);
    }

    /**
	 * Build a raster backing a geotools gridCoverage, optionally extracting a single band.
	 * 
	 * @param coverage the coverage to use.
	 * @param bandIndex optional index of the band to extract. If null, the first band is used.
	 * @return the HMRaster instance.
	 */
    public static HMRaster fromGridCoverage( GridCoverage2D coverage, Integer bandIndex ) {
    	if(bandIndex != null) {
			coverage = extractBand(coverage, bandIndex);
		}
        return fromGridCoverage(null, coverage);
    }
    
    /**
	 * Build a writable raster using a gridcoverage as template.
	 * 
	 * @param name an optional name to give to the raster. If null, the name of the coverage is used.
	 * @param coverage the coverage to use as template for the writable raster.
	 * @return the HMRaster instance.
	 */
    public static HMRaster fromGridCoverageWritable( String name, GridCoverage2D coverage ) {
    	return new HMRaster.HMRasterWritableBuilder().
        		setName(name != null ? name : coverage.getName().toString()).setTemplate(coverage).setCopyValues(true).build();
	}
    
    /**
     * Build a writable raster using a gridcoverage as template.
     * 
	 * @param coverage the coverage to use as template for the writable raster.
	 * @return the HMRaster instance.
     */
    public static HMRaster fromGridCoverageWritable(GridCoverage2D coverage ) {
    	return fromGridCoverageWritable(null, coverage);
    }
    
    /**
     * Build a raster from a file path.
     * 
     * @param path the path to the file.
     * @return the HMRaster instance.
     */
    public static HMRaster fromFile(String path) throws Exception {
    	return fromFile(new File(path));
    }

    /**
	 * Build a raster from a file, optionally extracting a single band.
	 * 
	 * @param path the path to the file.
	 * @param bandIndex optional index of the band to extract. If null, the first band is used.
	 * @return the HMRaster instance.
	 */
    public static HMRaster fromFile(String path, Integer bandIndex) throws Exception {
    	return fromFile(new File(path), bandIndex);
    }
    
    /**
	 * Build a raster from a file.
	 * 
	 * @param file the file to read.
	 * @return the HMRaster instance.
	 */
    public static HMRaster fromFile(File file) throws Exception {
    	return fromFile(file, null);
    }
    
    /**
	 * Build a raster from a file, optionally extracting a single band.
	 * 	
	 * @param file the file to read.
	 * @param bandIndex optional index of the band to extract. If null, the first band is used.
	 * @return the HMRaster instance.
	 */
    public static HMRaster fromFile(File file, Integer bandIndex) throws Exception {
    	var gc = OmsRasterReader.readRaster(file.getAbsolutePath());
    	if(bandIndex != null) {
			gc = extractBand(gc, bandIndex);
    	}
    	return fromGridCoverage(gc);
    }
    
    public String getName() {
        return name;
    }

    /**
     * Get the region map of this raster.
     * 
     * @return the region map of this raster.
     */
    public RegionMap getRegionMap() {
        return RegionMap.fromRegionMap(regionMap);
    }
    
	/**
	 * Get the region map of the actual data contained in this raster, i.e. the bounding box of the valid values.
	 *
	 * @return the region map of the actual data contained in this raster, snapped on its grid,
	 * 		or <code>null</code> if there are no valid values.
	 */
	public RegionMap getDataRegionMap() {
		// loop over the region map and find the actual data region
		// [minCol, maxCol, minRow, maxRow]
		int[] bounds = {Integer.MAX_VALUE, -Integer.MAX_VALUE, Integer.MAX_VALUE, -Integer.MAX_VALUE};
		try {
			visitStrips(startCol, startCol + cols - 1, startRow, startRow + rows - 1, (fromRow, height, values) -> {
				for (int r = 0; r < height; r++) {
					for (int c = 0; c < cols; c++) {
						if (!isNovalue(values[r * cols + c])) {
							int col = startCol + c;
							int row = fromRow + r;
							bounds[0] = Math.min(bounds[0], col);
							bounds[1] = Math.max(bounds[1], col);
							bounds[2] = Math.min(bounds[2], row);
							bounds[3] = Math.max(bounds[3], row);
						}
					}
				}
				return true;
			});
		} catch (Exception e) {
			throw new ModelsRuntimeException("Error reading the raster: " + e.getMessage(), this);
		}
		int minCol = bounds[0];
		int maxCol = bounds[1];
		int minRow = bounds[2];
		int maxRow = bounds[3];
		if (maxCol < minCol) {
			// no valid value at all
			return null;
		}
		Coordinate ul = getWorld(minCol, minRow); // centre of the upper-left valid cell
		Coordinate lr = getWorld(maxCol, maxRow); // centre of the lower-right valid cell
		double west = ul.x - xRes / 2;
		double east = lr.x + xRes / 2;
		double north = ul.y + yRes / 2;
		double south = lr.y - yRes / 2;
		return RegionMap.fromBoundsAndGrid(west, east, south, north, maxCol - minCol + 1, maxRow - minRow + 1);
	}

    public GridGeometry2D getGridGeometry() {
        return gridGeometry;
    }
    
    /**
     * @return the read iterator of the calling thread (iterators can't be shared between threads).
     */
    public RandomIter getIter() {
        return readIter();
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
     * @return the image backing this raster.
     */
    public RenderedImage getRenderedImage() {
        if (originalCoverage != null) {
            return originalCoverage.getRenderedImage();
        }
        return writableImage;
    }

    /**
     * @return the {@link DataBuffer} data type (eg. {@link DataBuffer#TYPE_BYTE}) backing this raster's pixels.
     */
    public int getDataType() {
        if (originalCoverage != null) {
            return originalCoverage.getRenderedImage().getSampleModel().getDataType();
        }
        if (writableImage != null) {
            return writableImage.getSampleModel().getDataType();
        }
        return DataBuffer.TYPE_DOUBLE;
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
    
	public boolean isNovalue(byte valueToCheck) {
		return HMConstants.isNovalue(valueToCheck, byteNovalue);
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
            try {
                return readIter().getSampleDouble(col, row, 0);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw outOfImageBounds(col, row, e);
            }
        } else {
            return novalue;
        }
    }

    /**
     * Describe a cell that is inside the region of the raster, but outside of its image, which
     * is otherwise reported by the image iterators without any position.
     *
     * @param col the col of the cell.
     * @param row the row of the cell.
     * @param cause the exception of the iterator.
     * @return the exception to throw, with the cell in grid and world space and the bounds of
     *          region and image.
     */
    private ArrayIndexOutOfBoundsException outOfImageBounds( int col, int row, ArrayIndexOutOfBoundsException cause ) {
        StringBuilder sb = new StringBuilder("Cell out of the image bounds: col=").append(col).append(", row=").append(row);
        try {
            Coordinate world = getWorld(col, row);
            sb.append(" (cell center x=").append(world.x).append(", y=").append(world.y).append(")");
        } catch (Exception e) {
            // the grid position is still useful
        }
        sb.append(". Raster region: cols ").append(startCol).append("..").append(startCol + cols - 1);
        sb.append(", rows ").append(startRow).append("..").append(startRow + rows - 1);
        RenderedImage image = getRenderedImage();
        if (image != null) {
            sb.append(". Image: cols ").append(image.getMinX()).append("..").append(image.getMinX() + image.getWidth() - 1);
            sb.append(", rows ").append(image.getMinY()).append("..").append(image.getMinY() + image.getHeight() - 1);
        }
        sb.append(".");
        ArrayIndexOutOfBoundsException exception = new ArrayIndexOutOfBoundsException(sb.toString());
        exception.initCause(cause);
        return exception;
    }

    /**
     * Get the values of a block of cells.
     *
     * <p>Does not use the shared iterator, so it can be called concurrently from multiple threads.
     *
     * @param col the first col of the block.
     * @param row the first row of the block.
     * @param width the number of cols of the block.
     * @param height the number of rows of the block.
     * @param buffer an optional array of at least width*height to fill. If null, a new one is created.
     * @return the values of the block in row-major order.
     */
    public double[] getValues( int col, int row, int width, int height, double[] buffer ) {
        if (buffer == null) {
            buffer = new double[width * height];
        }
        RenderedImage image = originalCoverage != null ? originalCoverage.getRenderedImage() : writableImage;
        Raster data = image.getData(new Rectangle(col, row, width, height));
        return data.getSamples(col, row, width, height, 0, buffer);
    }

    /**
     * Maximum number of cells read at once by the strip reads.
     */
    private static final long STRIP_MAX_CELLS = 16_000_000L;

    /**
     * Visitor of the strips of a raster, see {@link HMRaster#visitStrips(int, int, int, int, StripVisitor)}.
     */
    @FunctionalInterface
    interface StripVisitor {
        /**
         * @param fromRow the first row of the strip.
         * @param height the number of rows of the strip.
         * @param values the values of the strip in row-major order, as wide as the visited columns range.
         * @return <code>false</code> to stop the visit.
         */
        boolean visit( int fromRow, int height, double[] values ) throws Exception;
    }

    /**
     * Read a range of cells by horizontal strips, from top to bottom.
     *
     * <p>This is the way to scan large rasters: the strips are aligned to the tiles of the image,
     * so each tile is decoded about once. Reading cell by cell instead decodes a tile again for
     * each of its rows, as soon as a row of tiles does not fit the tile cache.</p>
     *
     * @param fromCol the first col.
     * @param toCol the last col (inclusive).
     * @param fromRow the first row.
     * @param toRow the last row (inclusive).
     * @param visitor the visitor of the strips.
     */
    void visitStrips( int fromCol, int toCol, int fromRow, int toRow, StripVisitor visitor ) throws Exception {
        if (fromCol > toCol || fromRow > toRow) {
            return;
        }
        RenderedImage image = getRenderedImage();
        int tileHeight = image != null ? image.getTileHeight() : 1;
        int yOffset = image != null ? image.getTileGridYOffset() : 0;
        int width = toCol - fromCol + 1;
        int maxRows = (int) Math.max(1, Math.min(tileHeight, STRIP_MAX_CELLS / width));
        double[] buffer = null;
        for( int row = fromRow; row <= toRow; ) {
            int toTileEnd = tileHeight - Math.floorMod(row - yOffset, tileHeight);
            int height = Math.min(Math.min(toTileEnd, maxRows), toRow - row + 1);
            if (buffer == null || buffer.length < width * height) {
                buffer = new double[width * height];
            }
            if (!visitor.visit(row, height, getValues(fromCol, row, width, height, buffer))) {
                return;
            }
            row += height;
        }
    }

    /**
     * A cache of the strip of rows last read, for cell reads that proceed roughly row by row,
     * as for example the resampling of another grid.
     */
    private class StripCache {
        private int fromRow = -1;
        private int height = 0;
        private double[] values;

        double getValue( int col, int row ) {
            if (!isContained(col, row)) {
                return novalue;
            }
            if (row < fromRow || row >= fromRow + height) {
                RenderedImage image = getRenderedImage();
                int tileHeight = image != null ? image.getTileHeight() : 1;
                int yOffset = image != null ? image.getTileGridYOffset() : 0;
                int maxRows = (int) Math.max(1, Math.min(tileHeight, STRIP_MAX_CELLS / cols));
                // the part of the row of tiles containing the row, starting at the row if it doesn't all fit
                int tileStart = row - Math.floorMod(row - yOffset, tileHeight);
                fromRow = Math.max(startRow, maxRows == tileHeight ? tileStart : row);
                height = Math.min(maxRows, startRow + rows - fromRow);
                if (values == null || values.length < cols * height) {
                    values = new double[cols * height];
                }
                values = getValues(startCol, fromRow, cols, height, values);
            }
            return values[(row - fromRow) * cols + col - startCol];
        }

        double getValue( Coordinate coordinate ) {
            int[] colRow = CoverageUtilities.colRowFromCoordinate(coordinate, gridGeometry, null);
            return getValue(colRow[0], colRow[1]);
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
            try {
                return readIter().getSample(col, row, 0);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw outOfImageBounds(col, row, e);
            }
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
            try {
                return (short) readIter().getSample(col, row, 0);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw outOfImageBounds(col, row, e);
            }
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
            try {
                writableTile(col, row).setSample(col, row, 0, value);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw outOfImageBounds(col, row, e);
            }
        }
    }

    public void setValue( int col, int row, int value ) throws IOException {
        if (!isWritable) {
            throw new IOException("The current HMRaster is not writable.");
        }
        if (isContained(col, row)) {
            try {
                writableTile(col, row).setSample(col, row, 0, value);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw outOfImageBounds(col, row, e);
            }
        }
    }

    public void setValue( int col, int row, short value ) throws IOException {
        if (!isWritable) {
            throw new IOException("The current HMRaster is not writable.");
        }
        if (isContained(col, row)) {
            try {
                writableTile(col, row).setSample(col, row, 0, value);
            } catch (ArrayIndexOutOfBoundsException e) {
                throw outOfImageBounds(col, row, e);
            }
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
    
	public void makeNullBorders() {
		try {
			// top and bottom rows
			for (int col = startCol; col < cols + startCol; col++) {
				setValue(col, startRow, novalue);
				setValue(col, rows + startRow - 1, novalue);
			}
			// left and right columns
			for (int row = startRow; row < rows + startRow; row++) {
				setValue(startCol, row, novalue);
				setValue(cols + startCol - 1, row, novalue);
			}
		} catch (IOException e) {
			throw new ModelsRuntimeException("Error setting null borders.", this);
		}
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
        IHMProgressMonitor _pm = pm;
        // read by strips, the processor still gets the cells one by one in row-major order
        visitStrips(startCol, startCol + cols - 1, startRow, startRow + rows - 1, ( fromRow, height, values ) -> {
            for( int r = 0; r < height; r++ ) {
                int row = fromRow + r;
                for( int c = 0; c < cols; c++ ) {
                    if (_pm.isCanceled()) {
                        return false;
                    }
                    processor.processCell(startCol + c, row, values[r * cols + c], cols, rows);
                }
                _pm.worked(1);
            }
            return true;
        });
        if (pm.isCanceled()) {
            return;
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
     * Split the raster in tiles.
     * 
     * @param tileSize the size of the side of the tiles.
     * @return the tiles in row-major order.
     */
    public List<HMRasterTile> getTiles( int tileSize ) {
        return HMRasterTile.createGrid(cols, rows, tileSize);
    }

    /**
     * Process the raster tile by tile, in parallel.
     * 
     * @param pm optional progress monitor.
     * @param processName optional process name.
     * @param tileSize the size of the side of the tiles.
     * @param threads the number of threads to use.
     * @param processor the processor of a single tile.
     * @throws Exception the first exception thrown by the processor.
     * @see #processTiles(IHMProgressMonitor, String, List, int, RasterTileProcessor)
     */
    public void processTiles( IHMProgressMonitor pm, String processName, int tileSize, int threads,
            RasterTileProcessor processor ) throws Exception {
        processTiles(pm, processName, getTiles(tileSize), threads, processor);
    }

    /**
     * Process a list of tiles in parallel.
     * 
     * <p>Unlike {@link #processByRow(IHMProgressMonitor, String, RasterRowProcessor, boolean)}, 
     * the first exception of the processor stops the processing and is thrown back.</p>
     * 
     * <p>The processor is called concurrently, so it has to be thread safe. Reads through 
     * {@link #getValues(int, int, int, int, double[])} are, cell by cell access is not.</p>
     * 
     * @param pm optional progress monitor.
     * @param processName optional process name.
     * @param tiles the tiles to process.
     * @param threads the number of threads to use.
     * @param processor the processor of a single tile.
     * @throws Exception the first exception thrown by the processor.
     */
    public static void processTiles( IHMProgressMonitor pm, String processName, List<HMRasterTile> tiles, int threads,
            RasterTileProcessor processor ) throws Exception {
        if (processName == null)
            processName = "Processing tiles...";
        if (pm == null)
            pm = new DummyProgressMonitor();
        IHMProgressMonitor _pm = pm;
        _pm.beginTask(processName, tiles.size());
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, threads));
        try {
            List<Future< ? >> futures = new ArrayList<>(tiles.size());
            for( HMRasterTile tile : tiles ) {
                futures.add(executor.submit(() -> {
                    if (!_pm.isCanceled()) {
                        processor.processTile(tile);
                        _pm.worked(1);
                    }
                    return null;
                }));
            }
            for( Future< ? > future : futures ) {
                try {
                    future.get();
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause();
                    if (cause instanceof Exception) {
                        throw (Exception) cause;
                    }
                    throw e;
                }
            }
        } finally {
            executor.shutdownNow();
            _pm.done();
        }
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
        return CoverageUtilities.buildCoverageWithNovalue(name, writableImage, regionMap, crs, novalue);
    }

    @Override
    public void close() throws Exception {
        if (iter != null) {
            iter.done();
        }
        otherThreadsIters.values().forEach(RandomIter::done);
        otherThreadsIters.clear();
    }
    
    /**
     * Build a new raster that is a sub raster of the current one, using the given sub region.
     * @param pm optional Process monitor.
     * @param subRegion the subregion to extract. If snapping to the origial raster is needed, 
     * 		use {@link RegionMap#toSubRegion(Envelope)}.
     * 
     * @return the new generated raster.
     * @throws IOException
     */
	public HMRaster toSubRaster(IHMProgressMonitor pm, RegionMap subRegion) throws IOException {
		if (pm == null)
			pm = new DummyProgressMonitor();
		var outRasterBuilder = new HMRaster.HMRasterWritableBuilder().setName("subraster").setRegion(subRegion)
				.setCrs(crs).setNoValue(novalue);
		switch (getDataType()) {
		case DataBuffer.TYPE_BYTE:
			outRasterBuilder.setDoByte(true);
			break;
		case DataBuffer.TYPE_SHORT:
		case DataBuffer.TYPE_USHORT:
			outRasterBuilder.setDoShort(true);
			break;
		case DataBuffer.TYPE_INT:
			outRasterBuilder.setDoInteger(true);
			break;
		default:
			break;
		}
		HMRaster outHMRaster = outRasterBuilder.build();
		var cols = subRegion.getCols();
		var rows = subRegion.getRows();

		// when subRegion has this raster's resolution and its origin is cell-aligned
		// to it the col/row mapping between the two grids is a plain integer offset
		boolean useOffset = false;
		int colOffset = 0;
		int rowOffset = 0;
		if (NumericsUtilities.dEq(subRegion.getXres(), xRes) && NumericsUtilities.dEq(subRegion.getYres(), yRes)) {
			double colOffsetD = (subRegion.getWest() - regionMap.getWest()) / xRes;
			double rowOffsetD = (regionMap.getNorth() - subRegion.getNorth()) / yRes;
			int co = (int) Math.round(colOffsetD);
			int ro = (int) Math.round(rowOffsetD);
			if (NumericsUtilities.dEq(colOffsetD, co, 1e-6) && NumericsUtilities.dEq(rowOffsetD, ro, 1e-6)) {
				useOffset = true;
				colOffset = co;
				rowOffset = ro;
			}
		}

		pm.beginTask("Extracting raster on region", rows);
		if (useOffset) {
			// read the overlapping source window by strips, cells outside it stay novalue
			int _colOffset = colOffset;
			int _rowOffset = rowOffset;
			IHMProgressMonitor _pm = pm;
			int fromCol = Math.max(startCol, colOffset);
			int toCol = Math.min(startCol + this.cols - 1, colOffset + cols - 1);
			int width = toCol - fromCol + 1;
			try {
				visitStrips(fromCol, toCol, Math.max(startRow, rowOffset), Math.min(startRow + this.rows - 1, rowOffset + rows - 1),
						(fromRow, height, values) -> {
							for (int r = 0; r < height; r++) {
								int outRow = fromRow + r - _rowOffset;
								for (int c = 0; c < width; c++) {
									int outCol = fromCol + c - _colOffset;
									if (isContained(outCol, outRow)) {
										outHMRaster.setValue(outCol, outRow, values[r * width + c]);
									}
								}
							}
							_pm.worked(height);
							return true;
						});
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else {
			StripCache cache = new StripCache();
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (!isContained(c, r)) {
						continue;
					}
					outHMRaster.setValue(c, r, cache.getValue(outHMRaster.getWorld(c, r)));
				}
				pm.worked(1);
			}
		}
		pm.done();

		return outHMRaster;
	}
    
    /**
     * Extract a new raster that is a sub raster of the current one, masked by the
     * given polygon geometry.
     * 
     * @param pm optional Process monitor.
     * @param polygon the polygon geometry to use for masking. It is assumed to be in the same CRS as the raster.
     * @return the new generated raster.
     * @throws IOException
     */
	public HMRaster extractOnPolygon(IHMProgressMonitor pm, Geometry polygon) throws IOException {
		if (pm == null)
			pm = new DummyProgressMonitor();

		GeometryFactory gf = new GeometryFactory();
		var subRegion = regionMap.toSubRegion(polygon.getEnvelopeInternal());
		var outRasterBuilder = new HMRaster.HMRasterWritableBuilder().setName("subraster").setRegion(subRegion)
				.setCrs(crs).setNoValue(novalue).setInitialValue(novalue);
		switch (getDataType()) {
		case DataBuffer.TYPE_BYTE:
			outRasterBuilder.setDoByte(true);
			break;
		case DataBuffer.TYPE_SHORT:
		case DataBuffer.TYPE_USHORT:
			outRasterBuilder.setDoShort(true);
			break;
		case DataBuffer.TYPE_INT:
			outRasterBuilder.setDoInteger(true);
			break;
		default:
			break;
		}
		HMRaster outRaster = outRasterBuilder.build();
		PreparedGeometry prepGeom = PreparedGeometryFactory.prepare(polygon);
		var cols = subRegion.getCols();
		var rows = subRegion.getRows();

		// when subRegion has this raster's resolution and its origin is cell-aligned
		// to it the col/row mapping between the two grids is a plain integer offset
		boolean useOffset = false;
		int colOffset = 0;
		int rowOffset = 0;
		if (NumericsUtilities.dEq(subRegion.getXres(), xRes) && NumericsUtilities.dEq(subRegion.getYres(), yRes)) {
			double colOffsetD = (subRegion.getWest() - regionMap.getWest()) / xRes;
			double rowOffsetD = (regionMap.getNorth() - subRegion.getNorth()) / yRes;
			int co = (int) Math.round(colOffsetD);
			int ro = (int) Math.round(rowOffsetD);
			if (NumericsUtilities.dEq(colOffsetD, co, 1e-6) && NumericsUtilities.dEq(rowOffsetD, ro, 1e-6)) {
				useOffset = true;
				colOffset = co;
				rowOffset = ro;
			}
		}
		double subWest = subRegion.getWest();
		double subNorth = subRegion.getNorth();
		double subXres = subRegion.getXres();
		double subYres = subRegion.getYres();

		pm.beginTask("Extracting raster on polygon", rows);
		if (useOffset) {
			// read the overlapping source window by strips, cells outside it or the polygon stay novalue
			int _colOffset = colOffset;
			int _rowOffset = rowOffset;
			IHMProgressMonitor _pm = pm;
			int fromCol = Math.max(startCol, colOffset);
			int toCol = Math.min(startCol + this.cols - 1, colOffset + cols - 1);
			int width = toCol - fromCol + 1;
			try {
				visitStrips(fromCol, toCol, Math.max(startRow, rowOffset), Math.min(startRow + this.rows - 1, rowOffset + rows - 1),
						(fromRow, height, values) -> {
							for (int r = 0; r < height; r++) {
								int outRow = fromRow + r - _rowOffset;
								for (int c = 0; c < width; c++) {
									int outCol = fromCol + c - _colOffset;
									if (!isContained(outCol, outRow)) {
										continue;
									}
									Coordinate worldCoord = new Coordinate(subWest + (outCol + 0.5) * subXres,
											subNorth - (outRow + 0.5) * subYres);
									if (prepGeom.contains(gf.createPoint(worldCoord))) {
										outRaster.setValue(outCol, outRow, values[r * width + c]);
									}
								}
							}
							_pm.worked(height);
							return true;
						});
			} catch (IOException e) {
				throw e;
			} catch (Exception e) {
				throw new IOException(e);
			}
		} else {
			StripCache cache = new StripCache();
			for (int r = 0; r < rows; r++) {
				for (int c = 0; c < cols; c++) {
					if (!isContained(c, r)) {
						continue;
					}
					var worldCoord = outRaster.getWorld(c, r);
					if (!prepGeom.contains(gf.createPoint(worldCoord))) {
						continue;
					}
					outRaster.setValue(c, r, cache.getValue(worldCoord));
				}
				pm.worked(1);
			}
		}
		pm.done();
		return outRaster;
	}
	
	
	/**
	 * Calculates zonal statistics for the given feature collection, using the specified id field name to identify zones.
	 * 
	 * @param pm optional Process monitor.
	 * @param fc the feature collection containing the zones.
	 * @param idFieldName the name of the field in the feature collection that contains the zone identifiers.
	 * @return a HashMap where the key is the zone identifier and the value is an array of statistics: [min, max, avg, sum, count].
	 * @throws Exception
	 */
	public HashMap<Integer, double[]> getZonalStats(IHMProgressMonitor pm, SimpleFeatureCollection fc, String idFieldName) throws Exception {
		if (pm == null)
			pm = new DummyProgressMonitor();
		Envelope totalEnv = fc.getBounds();
		var procRaster = toSubRaster(null, getRegionMap().toSubRegion(totalEnv));
		OmsScanLineRasterizer rasterizer = new OmsScanLineRasterizer();
		rasterizer.inRaster = procRaster.buildCoverage();
		rasterizer.inVector = fc;
		rasterizer.fCat = idFieldName;
		rasterizer.pm = pm;
		rasterizer.process();
		HMRaster idsRaster = HMRaster.fromGridCoverage(rasterizer.outRaster);
		// hashmap containing the id and the stats array: [min, max, avg, sum, count]
		HashMap<Integer, double[]> statsMap = new HashMap<>();
		idsRaster.process(pm, "zonal stats", (col, row, idValue, cols, rows) -> {
			if (idsRaster.isNovalue(idValue)) {
				return;
			}
			double value = procRaster.getValue(col, row);
			if (procRaster.isNovalue(value)) {
				return;
			}
			double[] statsArray = statsMap.get((int) idValue);
			if (statsArray == null) {
				statsArray = new double[] { Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 0, 0, 0 };
				statsMap.put((int) idValue, statsArray);
			}
			statsArray[0] = Math.min(statsArray[0], value); // min
			statsArray[1] = Math.max(statsArray[1], value); // max
			statsArray[3] += value; // sum
			statsArray[4]++; // count
		});
		
		// calculate the average
		for (double[] statsArray : statsMap.values()) {
			if (statsArray[4] > 0) {
				statsArray[2] = statsArray[3] / statsArray[4]; // avg
			} else {
				statsArray[2] = Double.NaN; // avg
			}
		}
		return statsMap;
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
        // the other raster is read row after row, through a strip cache to not decode its tiles again and again
        StripCache otherCache = otherRaster.new StripCache();
        // fill the points of the current raster picking form the
        // other raster via nearest neighbor interpolation
        for( int r = fromRow; r <= toRow; r++ ) {
            for( int c = fromCol; c <= toCol; c++ ) {
                if (isContained(c, r)) {
                    Coordinate coordinate = getWorld(c, r);
                    double otherRasterValue = otherCache.getValue(coordinate);
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
    
    public GridNodeNG getGridNodeNG( int col, int row ) {
		return new GridNodeNG(this, col, row);
	}
    
    /**
     * Calculates simple stats for valid values of the raster.
     * 
     * @return an array containing [min, max, avg, sum, count], or null if no valid cell was found.
     */
    public double[] getStatistics() {
        return getStatistics(null);
    }

    /**
     * Calculates simple stats for valid values of the raster, optionally
     * restricted to a world-region envelope.
     * 
     * <p>The envelope is assumed to be in the same CRS as the raster.</p>
     * 
     * @param envelope optional JTS envelope. If null, the full raster is scanned.
     * @return an array containing [min, max, avg, sum, count], or null if no valid cell was found.
     */
    public double[] getStatistics( Envelope envelope ) {
        int fromRow = startRow;
        int toRow = startRow + rows - 1;
        int fromCol = startCol;
        int toCol = startCol + cols - 1;

        if (envelope != null) {
            Point ul = getCell(new Coordinate(envelope.getMinX(), envelope.getMaxY()));
            Point lr = getCell(new Coordinate(envelope.getMaxX(), envelope.getMinY()));

            fromCol = Math.max(startCol, Math.min(ul.x, lr.x));
            toCol = Math.min(startCol + cols - 1, Math.max(ul.x, lr.x));
            fromRow = Math.max(startRow, Math.min(ul.y, lr.y));
            toRow = Math.min(startRow + rows - 1, Math.max(ul.y, lr.y));

            if (fromCol > toCol || fromRow > toRow) {
                return null;
            }
        }

        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        double sum = 0;
        long count = 0;

        /*
         * read by blocks aligned to the image tiles: cell by cell reads decode a tile
         * again for each of its rows as soon as a row of tiles doesn't fit the tile cache
         */
        RenderedImage image = getRenderedImage();
        int tileWidth = image != null ? image.getTileWidth() : cols;
        int tileHeight = image != null ? image.getTileHeight() : rows;
        int xOffset = image != null ? image.getTileGridXOffset() : 0;
        int yOffset = image != null ? image.getTileGridYOffset() : 0;
        // at most ~16M cells per block, whole tiles
        int blockTiles = Math.max(1, (int) (16_000_000L / ((long) tileWidth * tileHeight)));
        double[] buffer = null;
        for( int r0 = fromRow; r0 <= toRow; ) {
            int h = Math.min(tileHeight - Math.floorMod(r0 - yOffset, tileHeight), toRow - r0 + 1);
            for( int c0 = fromCol; c0 <= toCol; ) {
                int w = Math.min(blockTiles * tileWidth - Math.floorMod(c0 - xOffset, tileWidth), toCol - c0 + 1);
                if (buffer == null || buffer.length < w * h) {
                    buffer = new double[w * h];
                }
                double[] values = getValues(c0, r0, w, h, buffer);
                for( int r = 0; r < h; r++ ) {
                    for( int c = 0; c < w; c++ ) {
                        double value = values[r * w + c];
                        if (isNovalue(value)) {
                            continue;
                        }
                        if (envelope != null) {
                            Coordinate world = getWorld(c0 + c, r0 + r);
                            if (!envelope.contains(world.x, world.y)) {
                                continue;
                            }
                        }
                        min = Math.min(min, value);
                        max = Math.max(max, value);
                        sum += value;
                        count++;
                    }
                }
                c0 += w;
            }
            r0 += h;
        }

        if (count == 0) {
            return null;
        }

        return new double[]{min, max, sum / count, sum, count};
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
    
    public void writeToFile( String path ) throws Exception {
		GridCoverage2D coverage = buildCoverage();
		OmsRasterWriter.writeRaster(path,coverage);
	}
    
	public static GridCoverage2D extractBand(GridCoverage2D coverage, int bandIndex) {
		coverage = (GridCoverage2D) Operations.DEFAULT.selectSampleDimension(coverage, new int[]{bandIndex});
		return coverage;
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

        private boolean doByte = false;

        private RegionMap region;

        private CoordinateReferenceSystem crs;

        private Double noValue = null;

        private Double initialValue = null;

        private Integer initialIntValue = null;

        private Short initialShortValue = null;

        private double[][] dataMatrix = null;
        
        private boolean doNullBorder = false;

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

        public HMRasterWritableBuilder setDoByte( boolean doByte ) {
            this.doByte = doByte;
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
        
        public HMRasterWritableBuilder setDoNullBorder() {
        	this.doNullBorder = true;
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
                hmRaster.byteNovalue = noValue != null ? noValue.byteValue() : (byte) template.getNovalue();

                if (doInteger) {
                    hmRaster.writableImage = CoverageUtilities.createWritableImage(hmRaster.cols, hmRaster.rows, Integer.class,
                            initialIntValue != null ? initialIntValue : hmRaster.intNovalue);
                } else if (doShort) {
                    hmRaster.writableImage = CoverageUtilities.createWritableImage(hmRaster.cols, hmRaster.rows, Short.class,
                            initialShortValue != null ? initialShortValue : hmRaster.shortNovalue);
                } else if (doByte) {
                    hmRaster.writableImage = CoverageUtilities.createWritableImage(hmRaster.cols, hmRaster.rows, Byte.class,
                            initialValue != null ? initialValue.byteValue() : hmRaster.byteNovalue);
                } else {
                    hmRaster.writableImage = CoverageUtilities.createWritableImage(hmRaster.cols, hmRaster.rows, Double.class,
                            initialValue != null ? initialValue : hmRaster.novalue);
                }
                hmRaster.initWritableImage(hmRaster.writableImage);
//                if (nullBorders) {
//                    for( int c = 0; c < width; c++ ) {
//                        writableRaster.setSample(c, 0, 0, doubleNovalue);
//                        writableRaster.setSample(c, height - 1, 0, doubleNovalue);
//                    }
//                    for( int r = 0; r < height; r++ ) {
//                        writableRaster.setSample(0, r, 0, doubleNovalue);
//                        writableRaster.setSample(width - 1, r, 0, doubleNovalue);
//                    }
//                }
                if (copyValues) {
                    // read the template by strips: cell by cell reads of a large file decode its tiles again and again
                    int _cols = hmRaster.cols;
                    int _rows = hmRaster.rows;
                    try {
                        template.visitStrips(0, _cols - 1, 0, _rows - 1, ( fromRow, height, values ) -> {
                            for( int rr = 0; rr < height; rr++ ) {
                                int r = fromRow + rr;
                                for( int c = 0; c < _cols; c++ ) {
                                    boolean isBorder = doNullBorder && (c == 0 || r == 0 || c == _cols - 1 || r == _rows - 1);
                                    double value = values[rr * _cols + c];
                                    WritableRaster tile = hmRaster.writableTile(c, r);
                                    if (doInteger) {
                                        if (isBorder) {
                                            tile.setSample(c, r, 0, template.intNovalue);
                                        } else {
                                            tile.setSample(c, r, 0, value);
                                        }
                                    } else if (doShort) {
                                        tile.setSample(c, r, 0, isBorder ? template.shortNovalue : (short) value);
                                    } else if (doByte) {
                                        tile.setSample(c, r, 0, isBorder ? (byte) template.novalue : (byte) value);
                                    } else {
                                        tile.setSample(c, r, 0, isBorder ? template.novalue : value);
                                    }
                                }
                            }
                            return true;
                        });
                    } catch (Exception e) {
                        throw new ModelsRuntimeException("Error copying the template values: " + e.getMessage(), this);
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
                    hmRaster.writableImage = CoverageUtilities.createWritableImage(hmRaster.cols, hmRaster.rows, Integer.class,
                            initialIntValue != null ? initialIntValue : hmRaster.intNovalue);
                } else if (doShort) {
                    hmRaster.writableImage = CoverageUtilities.createWritableImage(hmRaster.cols, hmRaster.rows, Short.class,
                            initialShortValue != null ? initialShortValue : hmRaster.shortNovalue);
                } else if (doByte) {
                    hmRaster.writableImage = CoverageUtilities.createWritableImage(hmRaster.cols, hmRaster.rows, Byte.class,
                            initialValue != null ? initialValue.byteValue() : (byte) hmRaster.novalue);
                } else {
                    hmRaster.writableImage = CoverageUtilities.createWritableImage(hmRaster.cols, hmRaster.rows, Double.class,
                            initialValue != null ? initialValue : hmRaster.novalue);
                }
                hmRaster.initWritableImage(hmRaster.writableImage);

                if (dataMatrix != null) {
                    for( int r = 0; r < hmRaster.rows; r++ ) {
                        for( int c = 0; c < hmRaster.cols; c++ ) {
                            hmRaster.writableTile(c, r).setSample(c, r, 0, dataMatrix[r][c]);
                        }
                    }
                }
                return hmRaster;
            }
        }
    }



}
