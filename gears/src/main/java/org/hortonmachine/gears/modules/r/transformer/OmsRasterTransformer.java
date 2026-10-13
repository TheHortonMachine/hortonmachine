/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org
 * (C) HydroloGIS - www.hydrologis.com
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.modules.r.transformer;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.BILINEAR;
import static org.hortonmachine.gears.libs.modules.Variables.NEAREST_NEIGHTBOUR;
import static org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer.OMSRASTERTRANSFORMER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer.OMSRASTERTRANSFORMER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer.OMSRASTERTRANSFORMER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer.OMSRASTERTRANSFORMER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer.OMSRASTERTRANSFORMER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer.OMSRASTERTRANSFORMER_LABEL;
import static org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer.OMSRASTERTRANSFORMER_LICENSE;
import static org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer.OMSRASTERTRANSFORMER_NAME;
import static org.hortonmachine.gears.modules.r.transformer.OmsRasterTransformer.OMSRASTERTRANSFORMER_STATUS;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Polygon;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description(OMSRASTERTRANSFORMER_DESCRIPTION)
@Documentation(OMSRASTERTRANSFORMER_DOCUMENTATION)
@Author(name = OMSRASTERTRANSFORMER_AUTHORNAMES, contact = OMSRASTERTRANSFORMER_AUTHORCONTACTS)
@Keywords(OMSRASTERTRANSFORMER_KEYWORDS)
@Label(OMSRASTERTRANSFORMER_LABEL)
@Name(OMSRASTERTRANSFORMER_NAME)
@Status(OMSRASTERTRANSFORMER_STATUS)
@License(OMSRASTERTRANSFORMER_LICENSE)
public class OmsRasterTransformer extends HMModel {
    @Description(OMSRASTERTRANSFORMER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSRASTERTRANSFORMER_P_INTERPOLATION_DESCRIPTION)
    @UI("combo:" + NEAREST_NEIGHTBOUR + "," + BILINEAR)
    @In
    public String pInterpolation = NEAREST_NEIGHTBOUR;

    @Description(OMSRASTERTRANSFORMER_P_TRANS_X_DESCRIPTION)
    @Unit("m")
    @In
    public Double pTransX;

    @Description(OMSRASTERTRANSFORMER_P_TRANS_Y_DESCRIPTION)
    @Unit("m")
    @In
    public Double pTransY;

    @Description(OMSRASTERTRANSFORMER_P_SCALE_X_DESCRIPTION)
    @In
    public Double pScaleX;

    @Description(OMSRASTERTRANSFORMER_P_SCALE_Y_DESCRIPTION)
    @In
    public Double pScaleY;

    @Description(OMSRASTERTRANSFORMER_DO_FLIP_HORIZONTAL_DESCRIPTION)
    @In
    public boolean doFlipHorizontal;

    @Description(OMSRASTERTRANSFORMER_DO_FLIP_VERTICAL_DESCRIPTION)
    @In
    public boolean doFlipVertical;

    @Description(OMSRASTERTRANSFORMER_P_NORTH_DESCRIPTION)
    @UI(HMConstants.NORTHING_UI_HINT)
    @In
    public Double pNorth;

    @Description(OMSRASTERTRANSFORMER_P_EAST_DESCRIPTION)
    @UI(HMConstants.EASTING_UI_HINT)
    @In
    public Double pEast;

    @Description(OMSRASTERTRANSFORMER_P_ANGLE_DESCRIPTION)
    @Unit("degrees")
    @In
    public Double pAngle;

    @Description(OMSRASTERTRANSFORMER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster = null;

    @Description(OMSRASTERTRANSFORMER_OUT_BOUNDS_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outBounds = null;

    public static final String OMSRASTERTRANSFORMER_DESCRIPTION = "Flips, rotates, scales and translates a raster.";
    public static final String OMSRASTERTRANSFORMER_DOCUMENTATION = """
            The transformations are applied in this order: the flips, the rotation, the scale, \
            the translation. The flips mirror the columns (horizontal) or the rows (vertical) on \
            the same grid. The rotation, counterclockwise in degrees, turns the raster around the \
            pivot, which is the given point or else the center of the raster: the result covers \
            the whole rotated raster, on cells of the same size, and the cells outside of it are \
            novalue. Their values come from the nearest input cell, or are interpolated between \
            the four nearest cell centers with the bilinear interpolation, that suits the \
            continuous values but not the classes. The other transformations don't touch the \
            values.

            The scale multiplies the size of the cells around the same pivot, and the translation \
            moves the raster, in the units of its reference system: they change only where the \
            raster lies, as to fix a raster with a wrong georeference or a scan placed by hand. \
            The bounds output is the outline of the transformed raster, rotated with it.
            """;
    public static final String OMSRASTERTRANSFORMER_KEYWORDS = "Transform, Rotate, Flip, Scale, Translate, Raster";
    public static final String OMSRASTERTRANSFORMER_LABEL = RASTERPROCESSING;
    public static final String OMSRASTERTRANSFORMER_NAME = "rtrans";
    public static final int OMSRASTERTRANSFORMER_STATUS = 40;
    public static final String OMSRASTERTRANSFORMER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSRASTERTRANSFORMER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSRASTERTRANSFORMER_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSRASTERTRANSFORMER_IN_RASTER_DESCRIPTION = "The raster to transform.";
    public static final String OMSRASTERTRANSFORMER_P_INTERPOLATION_DESCRIPTION = "The interpolation of the values of the rotated raster.";
    public static final String OMSRASTERTRANSFORMER_P_TRANS_X_DESCRIPTION = "The translation along the X axis.";
    public static final String OMSRASTERTRANSFORMER_P_TRANS_Y_DESCRIPTION = "The translation along the Y axis.";
    public static final String OMSRASTERTRANSFORMER_P_SCALE_X_DESCRIPTION = "The scale factor along the X axis.";
    public static final String OMSRASTERTRANSFORMER_P_SCALE_Y_DESCRIPTION = "The scale factor along the Y axis.";
    public static final String OMSRASTERTRANSFORMER_DO_FLIP_HORIZONTAL_DESCRIPTION = "Flip horizontally, mirroring the columns.";
    public static final String OMSRASTERTRANSFORMER_DO_FLIP_VERTICAL_DESCRIPTION = "Flip vertically, mirroring the rows.";
    public static final String OMSRASTERTRANSFORMER_P_NORTH_DESCRIPTION = "The northing of the pivot of rotation and scale (default the center of the raster).";
    public static final String OMSRASTERTRANSFORMER_P_EAST_DESCRIPTION = "The easting of the pivot of rotation and scale (default the center of the raster).";
    public static final String OMSRASTERTRANSFORMER_P_ANGLE_DESCRIPTION = "The counterclockwise rotation angle.";
    public static final String OMSRASTERTRANSFORMER_OUT_RASTER_DESCRIPTION = "The transformed raster.";
    public static final String OMSRASTERTRANSFORMER_OUT_BOUNDS_DESCRIPTION = "The outline of the transformed raster.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outRaster == null, doReset)) {
            return;
        }
        checkNull(inRaster);

        double scaleX = pScaleX != null ? pScaleX : 1.0;
        double scaleY = pScaleY != null ? pScaleY : 1.0;
        if (scaleX <= 0 || scaleY <= 0) {
            throw new ModelsIllegalargumentException("The scale factors need to be positive: to mirror use the flips.", this,
                    pm);
        }
        double transX = pTransX != null ? pTransX : 0.0;
        double transY = pTransY != null ? pTransY : 0.0;
        double angle = pAngle != null ? Math.toRadians(pAngle) : 0.0;
        boolean doBilinear = BILINEAR.equals(pInterpolation);

        try (HMRaster inData = HMRaster.fromGridCoverage(inRaster)) {
            RegionMap inRegion = inData.getRegionMap();
            double west = inRegion.getWest();
            double north = inRegion.getNorth();
            double xRes = inRegion.getXres();
            double yRes = inRegion.getYres();
            int inCols = inRegion.getCols();
            int inRows = inRegion.getRows();
            Coordinate pivot = new Coordinate(pEast != null ? pEast : (west + inRegion.getEast()) / 2.0,
                    pNorth != null ? pNorth : (north + inRegion.getSouth()) / 2.0);
            double cos = Math.cos(angle);
            double sin = Math.sin(angle);

            // the rotated input corners, before scale and translation
            Coordinate[] corners = {new Coordinate(west, north), new Coordinate(inRegion.getEast(), north),
                    new Coordinate(inRegion.getEast(), inRegion.getSouth()), new Coordinate(west, inRegion.getSouth())};
            Coordinate[] rotatedCorners = new Coordinate[corners.length];
            double minX = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            for( int i = 0; i < corners.length; i++ ) {
                rotatedCorners[i] = rotate(corners[i], pivot, cos, sin);
                minX = Math.min(minX, rotatedCorners[i].x);
                maxX = Math.max(maxX, rotatedCorners[i].x);
                minY = Math.min(minY, rotatedCorners[i].y);
                maxY = Math.max(maxY, rotatedCorners[i].y);
            }

            // the grid of the rotated raster, on cells of the input size; without rotation it is the input grid
            int outCols = inCols;
            int outRows = inRows;
            double outWest = west;
            double outNorth = north;
            if (angle != 0.0) {
                // the tolerance keeps the exact multiples, as the 90 degrees rotations, from getting one cell more
                outCols = (int) Math.ceil((maxX - minX) / xRes - 1E-9);
                outRows = (int) Math.ceil((maxY - minY) / yRes - 1E-9);
                outWest = minX;
                outNorth = maxY;
            }

            // scale and translation change only the georeference of that grid
            double finalWest = pivot.x + (outWest - pivot.x) * scaleX + transX;
            double finalNorth = pivot.y + (outNorth - pivot.y) * scaleY + transY;
            double finalXRes = xRes * scaleX;
            double finalYRes = yRes * scaleY;
            RegionMap outRegion = RegionMap.fromBoundsAndGrid(finalWest, finalWest + outCols * finalXRes,
                    finalNorth - outRows * finalYRes, finalNorth, outCols, outRows);

            try (HMRaster outData = new HMRasterWritableBuilder().setName("transformed").setRegion(outRegion)
                    .setCrs(inData.getCrs()).setNoValue(HMConstants.doubleNovalue).build()) {
                pm.beginTask("Transforming raster...", outRows);
                for( int r = 0; r < outRows; r++ ) {
                    for( int c = 0; c < outCols; c++ ) {
                        double value;
                        if (angle == 0.0) {
                            value = flippedValue(inData, c, r, inCols, inRows);
                        } else {
                            // the center of the output cell, rotated back onto the input
                            Coordinate center = new Coordinate(outWest + (c + 0.5) * xRes, outNorth - (r + 0.5) * yRes);
                            Coordinate source = rotate(center, pivot, cos, -sin);
                            double col = (source.x - west) / xRes;
                            double row = (north - source.y) / yRes;
                            if (doBilinear) {
                                value = bilinearValue(inData, col - 0.5, row - 0.5, inCols, inRows);
                            } else {
                                value = flippedValue(inData, (int) Math.floor(col), (int) Math.floor(row), inCols, inRows);
                            }
                        }
                        if (!inData.isNovalue(value)) {
                            outData.setValue(c, r, value);
                        }
                    }
                    pm.worked(1);
                }
                pm.done();
                outRaster = outData.buildCoverage();
            }

            Coordinate[] outline = new Coordinate[rotatedCorners.length + 1];
            for( int i = 0; i < rotatedCorners.length; i++ ) {
                Coordinate corner = rotatedCorners[i];
                outline[i] = new Coordinate(pivot.x + (corner.x - pivot.x) * scaleX + transX,
                        pivot.y + (corner.y - pivot.y) * scaleY + transY);
            }
            outline[rotatedCorners.length] = outline[0];
            Polygon outlinePolygon = GeometryUtilities.gf().createPolygon(outline);
            outBounds = FeatureUtilities.featureCollectionFromGeometry(inData.getCrs(), outlinePolygon);
        }
    }

    private static Coordinate rotate( Coordinate coordinate, Coordinate pivot, double cos, double sin ) {
        double dx = coordinate.x - pivot.x;
        double dy = coordinate.y - pivot.y;
        return new Coordinate(pivot.x + dx * cos - dy * sin, pivot.y + dx * sin + dy * cos);
    }

    /**
     * The value of a cell of the input, after the flips, or novalue outside of it.
     */
    private double flippedValue( HMRaster inData, int col, int row, int cols, int rows ) {
        if (col < 0 || col >= cols || row < 0 || row >= rows) {
            return inData.getNovalue();
        }
        int sourceCol = doFlipHorizontal ? cols - 1 - col : col;
        int sourceRow = doFlipVertical ? rows - 1 - row : row;
        return inData.getValue(sourceCol, sourceRow);
    }

    /**
     * The bilinear interpolation between the four cell centers around a point in grid space, measured from the
     * center of the first cell. Near the novalues and the borders it falls back to the nearest cell.
     */
    private double bilinearValue( HMRaster inData, double col, double row, int cols, int rows ) {
        int col0 = (int) Math.floor(col);
        int row0 = (int) Math.floor(row);
        double fCol = col - col0;
        double fRow = row - row0;
        double v00 = flippedValue(inData, col0, row0, cols, rows);
        double v10 = flippedValue(inData, col0 + 1, row0, cols, rows);
        double v01 = flippedValue(inData, col0, row0 + 1, cols, rows);
        double v11 = flippedValue(inData, col0 + 1, row0 + 1, cols, rows);
        if (inData.isNovalue(v00) || inData.isNovalue(v10) || inData.isNovalue(v01) || inData.isNovalue(v11)) {
            return flippedValue(inData, (int) Math.round(col), (int) Math.round(row), cols, rows);
        }
        double top = v00 + (v10 - v00) * fCol;
        double bottom = v01 + (v11 - v01) * fCol;
        return top + (bottom - top) * fRow;
    }
}
