/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.modules;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * A simple raster for scripting environment.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Raster {

    private RegionMap regionMap;
    private CoordinateReferenceSystem crs;
    private RandomIter iter;
    private int cols;
    private int rows;
    private WritableRaster newWR;
    private boolean makeNew;

    public Raster( GridCoverage2D raster ) {
        this(raster, false);
    }

    public Raster( GridCoverage2D raster, boolean makeNew ) {
        this.makeNew = makeNew;
        regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(raster);
        crs = raster.getCoordinateReferenceSystem();
        cols = regionMap.getCols();
        rows = regionMap.getRows();

        if (makeNew) {
            newWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, JGTConstants.doubleNovalue);
            iter = RandomIterFactory.createWritable(newWR, null);
        } else {
            iter = RandomIterFactory.create(raster.getRenderedImage(), null);
        }
    }

    public int getRows() {
        return rows;
    }

    public int getCols() {
        return cols;
    }

    public double valueAt( int col, int row ) {
        if (isInRaster(col, row)) {
            double value = iter.getSampleDouble(col, row, 0);
            return value;
        }
        return JGTConstants.doubleNovalue;
    }

    public boolean isNoValue( double value ) {
        return JGTConstants.isNovalue(value);
    }

    public double novalue() {
        return JGTConstants.doubleNovalue;
    }

    public static boolean valuesEqual( double value1, double value2 ) {
        return NumericsUtilities.dEq(value1, value2);
    }

    public void setValueAt( int col, int row, double value ) {
        if (makeNew) {
            if (isInRaster(col, row)) {
                ((WritableRandomIter) iter).setSample(col, row, 0, value);
            }
        } else {
            throw new RuntimeException("Writing not allowed.");
        }
    }

    public GridCoverage2D buildRaster() {
        if (makeNew) {
            GridCoverage2D coverage = CoverageUtilities.buildCoverage("raster", newWR, regionMap, crs);
            return coverage;
        } else {
            throw new RuntimeException("The raster is readonly, so no new raster can be built.");
        }
    }

    public void dumpRaster( String path ) throws Exception {
        if (makeNew) {
            RasterWriter.writeRaster(path, buildRaster());
        } else {
            throw new RuntimeException("Only new rasters can be dumped.");
        }
    }

    private boolean isInRaster( int col, int row ) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return false;
        }
        return true;
    }

}
