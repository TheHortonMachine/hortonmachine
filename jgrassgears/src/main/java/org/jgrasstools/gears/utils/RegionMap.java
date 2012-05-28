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
package org.jgrasstools.gears.utils;

import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.COLS;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.EAST;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.NORTH;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.ROWS;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.SOUTH;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.WEST;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.XRES;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.YRES;

import java.util.HashMap;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

import com.vividsolutions.jts.geom.Coordinate;

/**
 * Map containing a region definition, having utility methods to get the values.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.2
 */
public class RegionMap extends HashMap<String, Double> {
    private static final long serialVersionUID = 1L;

    /**
     * Getter for the region cols.
     * 
     * @return the region cols or -1.
     */
    public int getCols() {
        Double cols = get(COLS);
        if (cols != null) {
            return cols.intValue();
        }
        return -1;
    }

    /**
     * Getter for the region rows.
     * 
     * @return the region rows or -1.
     */
    public int getRows() {
        Double rows = get(ROWS);
        if (rows != null) {
            return rows.intValue();
        }
        return -1;
    }

    /**
     * Getter for the region's north bound.
     * 
     * @return the region north bound or {@link JGTConstants#doubleNovalue}
     */
    public double getNorth() {
        Double n = get(NORTH);
        if (n != null) {
            return n;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Getter for the region's south bound.
     * 
     * @return the region south bound or {@link JGTConstants#doubleNovalue}
     */
    public double getSouth() {
        Double s = get(SOUTH);
        if (s != null) {
            return s;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Getter for the region's east bound.
     * 
     * @return the region east bound or {@link JGTConstants#doubleNovalue}
     */
    public double getEast() {
        Double e = get(EAST);
        if (e != null) {
            return e;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Getter for the region's west bound.
     * 
     * @return the region west bound or {@link JGTConstants#doubleNovalue}
     */
    public double getWest() {
        Double w = get(WEST);
        if (w != null) {
            return w;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Getter for the region's X resolution.
     * 
     * @return the region's X resolution or {@link JGTConstants#doubleNovalue}
     */
    public double getXres() {
        Double xres = get(XRES);
        if (xres != null) {
            return xres;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Getter for the region's Y resolution.
     * 
     * @return the region's Y resolution or {@link JGTConstants#doubleNovalue}
     */
    public double getYres() {
        Double yres = get(YRES);
        if (yres != null) {
            return yres;
        }
        return JGTConstants.doubleNovalue;
    }

    /**
     * Snaps a geographic point to be on the region grid.
     * 
     * <p>
     * Moves the point given by X and Y to be on the grid of the supplied
     * region.
     * </p>
     * 
     * @param x the easting of the arbitrary point.
     * @param y the northing of the arbitrary point.
     * @return the snapped coordinate.
     */
    public Coordinate snapToNextHigherInRegionResolution( double x, double y ) {

        double minx = getWest();
        double ewres = getXres();
        double xsnap = minx + (Math.ceil((x - minx) / ewres) * ewres);

        double miny = getSouth();
        double nsres = getYres();
        double ysnap = miny + (Math.ceil((y - miny) / nsres) * nsres);

        return new Coordinate(xsnap, ysnap);
    }

    /**
     * Creates a new {@link RegionMap} cropped on the new bounds and snapped on the original grid.
     * 
     * @param n the new north.
     * @param s the new south.
     * @param w the new west.
     * @param e the new east.
     * @return the new {@link RegionMap}.
     */
    public RegionMap toSubRegion( double n, double s, double w, double e ) {
        double originalXres = getXres();
        double originalYres = getYres();
        double originalWest = getWest();
        double originalSouth = getSouth();

        double envWest = w;
        double deltaX = (envWest - originalWest) % originalXres;
        double newWest = envWest - deltaX;

        double envSouth = s;
        double deltaY = (envSouth - originalSouth) % originalYres;
        double newSouth = envSouth - deltaY;

        double newWidth = e - w;
        double deltaW = newWidth % originalXres;
        newWidth = newWidth - deltaW + originalXres;

        double newHeight = n - s;
        double deltaH = newHeight % originalYres;
        newHeight = newHeight - deltaH + originalYres;

        double newNorth = newSouth + newHeight;
        double newEast = newWest + newWidth;

        int rows = (int) ((newHeight) / originalYres);
        int cols = (int) ((newWidth) / originalXres);

        double newXres = (newWidth) / cols;
        double newYres = (newHeight) / rows;

        RegionMap regionMap = CoverageUtilities.makeRegionParamsMap(newNorth, newSouth, newWest, newEast, newXres, newYres, cols,
                rows);
        return regionMap;
    }

    public String toStringJGT() {
        StringBuilder sb = new StringBuilder();
        sb.append("North = ").append(getNorth()).append("\n");
        sb.append("South = ").append(getSouth()).append("\n");
        sb.append("East = ").append(getEast()).append("\n");
        sb.append("West = ").append(getWest()).append("\n");
        sb.append("Rows = ").append(getRows()).append("\n");
        sb.append("Cols = ").append(getCols()).append("\n");
        sb.append("Xres = ").append(getXres()).append("\n");
        sb.append("Yres = ").append(getYres());

        return sb.toString();
    }
}
