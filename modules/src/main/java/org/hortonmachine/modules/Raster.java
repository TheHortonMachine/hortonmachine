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
package org.hortonmachine.modules;

import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.COLS;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.EAST;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.NORTH;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.ROWS;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.SOUTH;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.WEST;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.XRES;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.YRES;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.buildCoverage;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.createWritableRaster;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.getRegionParamsFromGridCoverage;

import java.awt.image.WritableRaster;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.geometry.Envelope;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A simple raster wrapper for scripting environment.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Raster {

    private RegionMap regionMap;
    private CoordinateReferenceSystem crs;
    private RandomIter iter;
    private WritableRaster newWR;
    private final boolean makeNew;
    private final int cols;
    private final int rows;
    private final double west;
    private final double south;
    private final double east;
    private final double north;
    private final double xRes;
    private final double yRes;
    private GridGeometry2D gridGeometry;
    private final double novalue;

    /**
     * Create a raster for reading purposes.
     * 
     * @param raster the raster to access.
     */
    public Raster( GridCoverage2D raster ) {
        this(raster, false);
    }

    /**
     * Create a new raster using a given raster as template.
     * 
     * @param coverage
     * @param makeNew
     */
    public Raster( GridCoverage2D raster, boolean makeNew ) {
        this.makeNew = makeNew;

        crs = raster.getCoordinateReferenceSystem();
        regionMap = getRegionParamsFromGridCoverage(raster);
        cols = regionMap.getCols();
        rows = regionMap.getRows();
        west = regionMap.getWest();
        south = regionMap.getSouth();
        east = regionMap.getEast();
        north = regionMap.getNorth();
        xRes = (east - west) / cols;
        yRes = (north - south) / rows;

        if (makeNew) {
            newWR = createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
            iter = RandomIterFactory.createWritable(newWR, null);
            novalue = HMConstants.doubleNovalue;
        } else {
            iter = RandomIterFactory.create(raster.getRenderedImage(), null);
            novalue = HMConstants.getNovalue(raster);
        }
    }

    /**
     * Create a new raster using a given raster as template.
     * 
     * @param raster the raster to copy the properties from.
     */
    public Raster( Raster raster ) {
        makeNew = true;

        crs = raster.getCrs();
        cols = raster.getCols();
        rows = raster.getRows();
        west = raster.getWest();
        south = raster.getSouth();
        east = raster.getEast();
        north = raster.getNorth();
        xRes = (east - west) / cols;
        yRes = (north - south) / rows;

        regionMap = new RegionMap();
        regionMap.put(NORTH, north);
        regionMap.put(SOUTH, south);
        regionMap.put(WEST, west);
        regionMap.put(EAST, east);
        regionMap.put(XRES, xRes);
        regionMap.put(YRES, yRes);
        regionMap.put(ROWS, (double) rows);
        regionMap.put(COLS, (double) cols);

        newWR = createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        iter = RandomIterFactory.createWritable(newWR, null);
        novalue = HMConstants.doubleNovalue;
    }

    /**
     * Create a new raster based on the region properties.
     * 
     * @param cols
     * @param rows
     * @param res
     * @param ulEasting
     * @param ulNorthing
     * @param epsg
     */
    public Raster( int cols, int rows, double res, double ulEasting, double ulNorthing, String epsg ) {
        this.cols = cols;
        this.rows = rows;
        xRes = res;
        yRes = res;
        try {
            crs = CrsUtilities.getCrsFromEpsg(epsg);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to get CRS from the given epsg: " + epsg);
        }

        double width = cols * res;
        double height = rows * res;
        west = ulEasting;
        east = ulEasting + width;
        north = ulNorthing;
        south = ulNorthing - height;

        regionMap = new RegionMap();
        regionMap.put(NORTH, north);
        regionMap.put(SOUTH, south);
        regionMap.put(WEST, west);
        regionMap.put(EAST, east);
        regionMap.put(XRES, res);
        regionMap.put(YRES, res);
        regionMap.put(ROWS, (double) rows);
        regionMap.put(COLS, (double) cols);

        makeNew = true;
        newWR = createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        iter = RandomIterFactory.createWritable(newWR, null);
        novalue = HMConstants.doubleNovalue;
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public double getNorth() {
        return north;
    }

    public double getSouth() {
        return south;
    }

    public double getWest() {
        return west;
    }
    public double getEast() {
        return east;
    }

    public double getxRes() {
        return xRes;
    }

    public double getyRes() {
        return yRes;
    }

    public double getRes() {
        return xRes;
    }

    public CoordinateReferenceSystem getCrs() {
        return crs;
    }

    /**
     * Get the raster value at a given col/row.
     * 
     * @param col
     * @param row
     * @return the value.
     */
    public double valueAt( int col, int row ) {
        if (isInRaster(col, row)) {
            double value = iter.getSampleDouble(col, row, 0);
            return value;
        }
        return novalue;
    }

    /**
     * Get world position from col, row.
     * 
     * @param col
     * @param row
     * @return the [x, y] position or <code>null</code> if outside the bounds.
     */
    public double[] positionAt( int col, int row ) {
        if (isInRaster(col, row)) {
            GridGeometry2D gridGeometry = getGridGeometry();
            Coordinate coordinate = CoverageUtilities.coordinateFromColRow(col, row, gridGeometry);
            return new double[]{coordinate.x, coordinate.y};
        }
        return null;
    }

    /**
     * Get grid col and row from a world coordinate.
     * 
     * @param x
     * @param y
     * @return the [col, row] or <code>null</code> if the position is outside the bounds.
     */
    public int[] gridAt( double x, double y ) {
        if (isInRaster(x, y)) {
            GridGeometry2D gridGeometry = getGridGeometry();
            int[] colRowFromCoordinate = CoverageUtilities.colRowFromCoordinate(new Coordinate(x, y), gridGeometry, null);
            return colRowFromCoordinate;
        }
        return null;
    }

    /**
     * Sets a raster value if the raster is writable.
     * 
     * @param col
     * @param row
     * @param value
     */
    public void setValueAt( int col, int row, double value ) {
        if (makeNew) {
            if (isInRaster(col, row)) {
                ((WritableRandomIter) iter).setSample(col, row, 0, value);
            } else {
                throw new RuntimeException("Setting value outside of raster.");
            }
        } else {
            throw new RuntimeException("Writing not allowed.");
        }
    }

    /**
     * Get the values of the surrounding cells.
     * 
     * <b>The order of the values is [E, EN, N, NW, W, WS, S, SE].</b>
     * 
     * @param col the col of the center cell.
     * @param row the row of the center cell.
     * @return the array of cell values around them as [E, EN, N, NW, W, WS, S, SE].
     */
    public double[] surrounding( int col, int row ) {
        GridNode node = new GridNode(iter, cols, rows, xRes, yRes, col, row, novalue);
        List<GridNode> surroundingNodes = node.getSurroundingNodes();
        double[] surr = new double[8];
        for( int i = 0; i < surroundingNodes.size(); i++ ) {
            GridNode gridNode = surroundingNodes.get(i);
            if (gridNode != null) {
                surr[i] = gridNode.elevation;
            } else {
                surr[i] = novalue;
            }
        }
        return surr;
    }

    public boolean isNoValue( double value ) {
        return HMConstants.isNovalue(value);
    }

    public double novalue() {
        return novalue;
    }

    public static boolean valuesEqual( double value1, double value2 ) {
        return NumericsUtilities.dEq(value1, value2);
    }

    public GridGeometry2D getGridGeometry() {
        if (gridGeometry == null) {
            Envelope envelope = new Envelope2D(crs, west, south, east - west, north - south);
            GridEnvelope2D gridRange = new GridEnvelope2D(0, 0, cols, rows);
            gridGeometry = new GridGeometry2D(gridRange, envelope);
        }
        return gridGeometry;
    }

    /**
     * Creates a {@link GridCoverage2D} from the {@link Raster}.
     * 
     * @return the coverage.
     */
    public GridCoverage2D buildRaster() {
        if (makeNew) {
            GridCoverage2D coverage = buildCoverage("raster", newWR, regionMap, crs);
            return coverage;
        } else {
            throw new RuntimeException("The raster is readonly, so no new raster can be built.");
        }
    }

    /**
     * Write the raster to file.
     * 
     * @param path th epath to write to.
     * @throws Exception
     */
    public void write( String path ) throws Exception {
        if (makeNew) {
            RasterWriter.writeRaster(path, buildRaster());
        } else {
            throw new RuntimeException("Only new rasters can be dumped.");
        }
    }

    /**
     * Read a raster from file.
     * 
     * @param path the path to the raster to read.
     * @return the read raster in readonly mode.
     * @throws Exception
     */
    public static Raster read( String path ) throws Exception {
        GridCoverage2D coverage2d = RasterReader.readRaster(path);
        Raster raster = new Raster(coverage2d);
        return raster;
    }

    private boolean isInRaster( int col, int row ) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return false;
        }
        return true;
    }

    private boolean isInRaster( double easting, double northing ) {
        if (easting < west || easting > east || northing < south || northing > north) {
            return false;
        }
        return true;
    }

}
