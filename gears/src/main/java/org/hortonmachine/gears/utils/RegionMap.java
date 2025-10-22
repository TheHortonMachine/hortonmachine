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
package org.hortonmachine.gears.utils;

import java.awt.geom.AffineTransform;

import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.geometry.Envelope2D;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opengis.geometry.DirectPosition;

/**
 * Map containing a region definition, having utility methods to get the values.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.2
 */
public class RegionMap {
    public double west;
    public double east;
    public double south;
    public double north;
    public double xres;
    public double yres;
    public int cols;
    public int rows;
    public int startCol = 0;
    public int startRow = 0;
    
    /*
     * optionally some modules can set what is assured to be a metric resolution
     * based on the crs. If not available, these values are null and the x/yres 
     * will be in the units of the implicit crs.
     */
    public Double xresMetric;
    public Double yresMetric;


    public static RegionMap fromEnvelopeAndGrid( Envelope envelope, int cols, int rows ) {
        return fromBoundsAndGrid(envelope.getMinX(), envelope.getMaxX(), envelope.getMinY(), envelope.getMaxY(), cols, rows);
    }

    public static RegionMap fromBoundsAndGrid( double west, double east, double south, double north, int cols, int rows ) {
        double width = east - west;
        double height = north - south;
        double xRes = width / cols;
        double yRes = height / rows;

        RegionMap region = new RegionMap();
        region.north = north;
        region.south = south;
        region.west = west;
        region.east = east;
        region.xres = xRes;
        region.yres = yRes;
        region.rows = rows;
        region.cols = cols;
        return region;
    }

    public static RegionMap fromEnvelopeAndResolution( Envelope envelope, double xRes, double yRes ) {
        return fromBoundsAndResolution(envelope.getMinX(), envelope.getMaxX(), envelope.getMinY(), envelope.getMaxY(), xRes,
                yRes);
    }

    public static RegionMap fromBoundsAndResolution( double west, double east, double south, double north, double xRes,
            double yRes ) {
        int cols = (int) Math.round((east - west) / xRes);
        if (cols < 1)
            cols = 1;
        int rows = (int) Math.round((north - south) / yRes);
        if (rows < 1)
            rows = 1;

        RegionMap region = new RegionMap();
        region.north = north;
        region.south = south;
        region.west = west;
        region.east = east;
        region.xres = xRes;
        region.yres = yRes;
        region.rows = rows;
        region.cols = cols;
        return region;
    }

    /**
     * Clone a ReagionMap.
     * 
     * @param other the region to duplicate.
     * @return the duplicated region.
     */
    public static RegionMap fromRegionMap( RegionMap other ) {
        RegionMap region = new RegionMap();
        region.north = other.getNorth();
        region.south = other.getSouth();
        region.west = other.getWest();
        region.east = other.getEast();
        region.xres = other.getXres();
        region.yres = other.getYres();
        region.rows = other.getRows();
        region.cols = other.getCols();
        return region;
    }

    /**
     * Create the region from a coverage {@link GridGeometry2D}.
     * 
     * @param gridGeometry the grid geometry to use.
     * @return the generated region.
     */
    public static RegionMap fromGridGeometry( GridGeometry2D gridGeometry ) {
        RegionMap envelopeParams = new RegionMap();

        Envelope2D envelope = gridGeometry.getEnvelope2D();
        DirectPosition lowerCorner = envelope.getLowerCorner();
        double[] westSouth = lowerCorner.getCoordinate();
        DirectPosition upperCorner = envelope.getUpperCorner();
        double[] eastNorth = upperCorner.getCoordinate();

        GridEnvelope2D gridRange = gridGeometry.getGridRange2D();
        int height = gridRange.height;
        int width = gridRange.width;

        AffineTransform gridToCRS = (AffineTransform) gridGeometry.getGridToCRS();
        double xRes = XAffineTransform.getScaleX0(gridToCRS);
        double yRes = XAffineTransform.getScaleY0(gridToCRS);

        envelopeParams.north = eastNorth[1];
        envelopeParams.south = westSouth[1];
        envelopeParams.west = westSouth[0];
        envelopeParams.east = eastNorth[0];
        envelopeParams.xres = xRes;
        envelopeParams.yres = yRes;
        envelopeParams.rows = height;
        envelopeParams.cols = width;

        return envelopeParams;
    }

    /**
     * Getter for the region cols.
     */
    public int getCols() {
        return cols;
    }

    /**
     * Getter for the region rows.
     */
    public int getRows() {
        return rows;
    }

    /**
     * Getter for the region's north bound.
     */
    public double getNorth() {
        return north;
    }

    /**
     * Getter for the region's south bound.
     */
    public double getSouth() {
        return south;
    }

    /**
     * Getter for the region's east bound.
     */
    public double getEast() {
        return east;
    }

    /**
     * Getter for the region's west bound.
     */
    public double getWest() {
        return west;
    }

    /**
     * Getter for the region's X resolution.
     * 
     * @return the region's X resolution or {@link HMConstants#doubleNovalue}
     */
    public double getXres() {
        return xres;
    }

    /**
     * Getter for the region's Y resolution.
     * 
     * @return the region's Y resolution or {@link HMConstants#doubleNovalue}
     */
    public double getYres() {
        return yres;
    }

    /**
     * Getter for the region width.
     * 
     * @return the region's width or {@link HMConstants#doubleNovalue}
     */
    public double getWidth() {
        return getEast() - getWest();
    }

    /**
     * Getter for the region height.
     * 
     * @return the region's height or {@link HMConstants#doubleNovalue}
     */
    public double getHeight() {
        return getNorth() - getSouth();
    }

    /**
     * @return the coordinate of the lower left corner.
     */
    public Coordinate getLowerLeft() {
        return new Coordinate(getWest(), getSouth());
    }

    /**
     * @return the coordinate of the upper right corner.
     */
    public Coordinate getUpperRight() {
        return new Coordinate(getEast(), getNorth());
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
     * Create the envelope of the region borders.
     * 
     * @return the envelope of the region borders.
     */
    public Envelope toEnvelope() {
        Envelope env = new Envelope(getWest(), getEast(), getSouth(), getNorth());
        return env;
    }

    /**
     * Creates a new {@link RegionMap} cropped on the new bounds and snapped on the original grid.
     * 
     * <p><b>The supplied bounds are contained in the resulting region.</b></p>
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
        if (deltaW > 0) {
            newWidth = newWidth - deltaW + originalXres;
        }

        double newHeight = n - s;
        double deltaH = newHeight % originalYres;
        if (deltaH > 0) {
            newHeight = newHeight - deltaH + originalYres;
        }

        double newNorth = newSouth + newHeight;
        double newEast = newWest + newWidth;

        int rows = (int) ((newHeight) / originalYres);
        int cols = (int) ((newWidth) / originalXres);

        RegionMap regionMap = RegionMap.fromBoundsAndGrid(newWest, newEast, newSouth, newNorth,  cols, rows);
        return regionMap;
    }

    /**
     * Creates a new {@link RegionMap} cropped on the new bounds and snapped on the original grid.
     * 
     * <p><b>The supplied bounds are contained in the resulting region.</b></p>
     * 
     * @param envelope the envelope to snap.
     * @return the new {@link RegionMap}.
     */
    public RegionMap toSubRegion( Envelope envelope ) {
        double w = envelope.getMinX();
        double s = envelope.getMinY();
        double e = envelope.getMaxX();
        double n = envelope.getMaxY();
        return toSubRegion(n, s, w, e);
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

    @Override
    public boolean equals( Object o ) {
        if (o instanceof RegionMap) {
            RegionMap otherMap = (RegionMap) o;
            return NumericsUtilities.dEq(getNorth(), otherMap.getNorth())
                    && NumericsUtilities.dEq(getSouth(), otherMap.getSouth())
                    && NumericsUtilities.dEq(getEast(), otherMap.getEast())
                    && NumericsUtilities.dEq(getWest(), otherMap.getWest())
                    && NumericsUtilities.dEq(getXres(), otherMap.getXres())
                    && NumericsUtilities.dEq(getYres(), otherMap.getYres())
                    && NumericsUtilities.dEq(getCols(), otherMap.getCols())
                    && NumericsUtilities.dEq(getRows(), otherMap.getRows());
        }
        return false;
    }

    public boolean equalsBounds( Object o ) {
        if (o instanceof RegionMap) {
            RegionMap otherMap = (RegionMap) o;
            return NumericsUtilities.dEq(getNorth(), otherMap.getNorth())
                    && NumericsUtilities.dEq(getSouth(), otherMap.getSouth())
                    && NumericsUtilities.dEq(getEast(), otherMap.getEast())
                    && NumericsUtilities.dEq(getWest(), otherMap.getWest());
        }
        return false;
    }

    public boolean equalsResolution( Object o ) {
        if (o instanceof RegionMap) {
            RegionMap otherMap = (RegionMap) o;
            return NumericsUtilities.dEq(getXres(), otherMap.getXres()) && NumericsUtilities.dEq(getYres(), otherMap.getYres());
        }
        return false;
    }

    public boolean equalsColsRows( Object o ) {
        if (o instanceof RegionMap) {
            RegionMap otherMap = (RegionMap) o;
            return NumericsUtilities.dEq(getCols(), otherMap.getCols()) && NumericsUtilities.dEq(getRows(), otherMap.getRows());
        }
        return false;
    }

}
