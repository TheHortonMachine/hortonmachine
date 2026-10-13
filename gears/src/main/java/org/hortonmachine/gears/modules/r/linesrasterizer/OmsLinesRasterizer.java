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
package org.hortonmachine.gears.modules.r.linesrasterizer;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer.OMSLINESRASTERIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer.OMSLINESRASTERIZER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer.OMSLINESRASTERIZER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer.OMSLINESRASTERIZER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer.OMSLINESRASTERIZER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer.OMSLINESRASTERIZER_LABEL;
import static org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer.OMSLINESRASTERIZER_LICENSE;
import static org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer.OMSLINESRASTERIZER_NAME;
import static org.hortonmachine.gears.modules.r.linesrasterizer.OmsLinesRasterizer.OMSLINESRASTERIZER_STATUS;

import java.awt.Point;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

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

@Description(OMSLINESRASTERIZER_DESCRIPTION)
@Documentation(OMSLINESRASTERIZER_DOCUMENTATION)
@Author(name = OMSLINESRASTERIZER_AUTHORNAMES, contact = OMSLINESRASTERIZER_AUTHORCONTACTS)
@Keywords(OMSLINESRASTERIZER_KEYWORDS)
@Label(OMSLINESRASTERIZER_LABEL)
@Name(OMSLINESRASTERIZER_NAME)
@Status(OMSLINESRASTERIZER_STATUS)
@License(OMSLINESRASTERIZER_LICENSE)
public class OmsLinesRasterizer extends HMModel {

    @Description(OMSLINESRASTERIZER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector = null;

    @Description(OMSLINESRASTERIZER_F_CAT_DESCRIPTION)
    @In
    public String fCat;

    @Description(OMSLINESRASTERIZER_P_CAT_DESCRIPTION)
    @In
    public double pCat = 1.0;

    @Description(OMSLINESRASTERIZER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSLINESRASTERIZER_P_NORTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_NORTH_UI_HINT)
    @In
    public Double pNorth = null;

    @Description(OMSLINESRASTERIZER_P_SOUTH_DESCRIPTION)
    @UI(HMConstants.PROCESS_SOUTH_UI_HINT)
    @In
    public Double pSouth = null;

    @Description(OMSLINESRASTERIZER_P_WEST_DESCRIPTION)
    @UI(HMConstants.PROCESS_WEST_UI_HINT)
    @In
    public Double pWest = null;

    @Description(OMSLINESRASTERIZER_P_EAST_DESCRIPTION)
    @UI(HMConstants.PROCESS_EAST_UI_HINT)
    @In
    public Double pEast = null;

    @Description(OMSLINESRASTERIZER_P_ROWS_DESCRIPTION)
    @UI(HMConstants.PROCESS_ROWS_UI_HINT)
    @In
    public Integer pRows = null;

    @Description(OMSLINESRASTERIZER_P_COLS_DESCRIPTION)
    @UI(HMConstants.PROCESS_COLS_UI_HINT)
    @In
    public Integer pCols = null;

    @Description(OMSLINESRASTERIZER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSLINESRASTERIZER_DESCRIPTION = "Converts lines to a raster: the cells crossed by a line get its value, the others are novalue.";
    public static final String OMSLINESRASTERIZER_DOCUMENTATION = """
            Every segment of the lines becomes the chain of cells that joins the cells of its two \
            vertices, with the Bresenham algorithm: the chain is 8-connected, one cell wide, as a \
            path of the flow directions, so a rasterized network can be used where the network \
            extracted from the elevation would be. A segment that touches a cell only on a corner \
            can leave it out. Where lines cross, the cell keeps the largest value, as the higher \
            order of two streams that join. The parts of the lines outside of the grid are left \
            out.

            The value of the cells is the one of the numeric field of the line, or the constant \
            category without a field. The grid is the one of the optional raster, or the one of \
            the bounds, rows and columns.
            """;
    public static final String OMSLINESRASTERIZER_KEYWORDS = "Raster, Vector, Lines, Rasterize";
    public static final String OMSLINESRASTERIZER_LABEL = RASTERPROCESSING;
    public static final String OMSLINESRASTERIZER_NAME = "rasterizelines";
    public static final int OMSLINESRASTERIZER_STATUS = 40;
    public static final String OMSLINESRASTERIZER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSLINESRASTERIZER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSLINESRASTERIZER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSLINESRASTERIZER_IN_VECTOR_DESCRIPTION = "The lines to rasterize.";
    public static final String OMSLINESRASTERIZER_F_CAT_DESCRIPTION = "The optional numeric field of the lines with the value of their cells.";
    public static final String OMSLINESRASTERIZER_P_CAT_DESCRIPTION = "The value of the cells if no field is given.";
    public static final String OMSLINESRASTERIZER_IN_RASTER_DESCRIPTION = "An optional raster that gives the grid, instead of bounds, rows and columns.";
    public static final String OMSLINESRASTERIZER_P_NORTH_DESCRIPTION = "The north bound of the region to consider";
    public static final String OMSLINESRASTERIZER_P_SOUTH_DESCRIPTION = "The south bound of the region to consider";
    public static final String OMSLINESRASTERIZER_P_WEST_DESCRIPTION = "The west bound of the region to consider";
    public static final String OMSLINESRASTERIZER_P_EAST_DESCRIPTION = "The east bound of the region to consider";
    public static final String OMSLINESRASTERIZER_P_ROWS_DESCRIPTION = "The rows of the region to consider";
    public static final String OMSLINESRASTERIZER_P_COLS_DESCRIPTION = "The cols of the region to consider";
    public static final String OMSLINESRASTERIZER_OUT_RASTER_DESCRIPTION = "The rasterized lines.";

    @Execute
    public void process() throws Exception {
        checkNull(inVector);

        SimpleFeatureType schema = inVector.getSchema();
        if (!EGeometryType.isLine(schema.getGeometryDescriptor())) {
            throw new ModelsIllegalargumentException("The module works only with line vectors.", this, pm);
        }

        String fCatChecked = null;
        if (fCat != null) {
            fCatChecked = FeatureUtilities.findAttributeName(schema, fCat);
            if (fCatChecked == null) {
                throw new ModelsIllegalargumentException("Could not find an attribute named: " + fCat, this, pm);
            }
        }

        try (HMRaster outData = buildRaster(schema)) {
            List<FeatureMate> matesList = FeatureUtilities.featureCollectionToMatesList(inVector);
            pm.beginTask("Rasterizing lines...", matesList.size());
            for( FeatureMate featureMate : matesList ) {
                double value = pCat;
                if (fCatChecked != null) {
                    Double attribute = featureMate.getAttribute(fCatChecked, Double.class);
                    if (attribute == null) {
                        pm.worked(1);
                        continue;
                    }
                    value = attribute;
                }

                Geometry geometry = featureMate.getGeometry();
                for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
                    Coordinate[] coordinates = geometry.getGeometryN(i).getCoordinates();
                    for( int j = 0; j < coordinates.length - 1; j++ ) {
                        Point from = outData.getCell(coordinates[j]);
                        Point to = outData.getCell(coordinates[j + 1]);
                        rasterizeSegment(outData, from.x, from.y, to.x, to.y, value);
                    }
                    if (coordinates.length == 1) {
                        Point cell = outData.getCell(coordinates[0]);
                        rasterizeSegment(outData, cell.x, cell.y, cell.x, cell.y, value);
                    }
                }
                pm.worked(1);
            }
            pm.done();

            outRaster = outData.buildCoverage();
        }
    }

    private HMRaster buildRaster( SimpleFeatureType schema ) throws Exception {
        if (inRaster != null) {
            return new HMRasterWritableBuilder().setName("rasterized").setTemplate(inRaster)
                    .setNoValue(HMConstants.doubleNovalue).setInitialValue(HMConstants.doubleNovalue).build();
        }
        if (pNorth == null || pSouth == null || pWest == null || pEast == null || pRows == null || pCols == null) {
            throw new ModelsIllegalargumentException(
                    "It is necessary to supply a raster or all the information about the processing region. Did you set the boundaries and rows/cols?",
                    this, pm);
        }
        RegionMap regionMap = RegionMap.fromBoundsAndGrid(pWest, pEast, pSouth, pNorth, pCols, pRows);
        return new HMRasterWritableBuilder().setName("rasterized").setRegion(regionMap)
                .setCrs(schema.getCoordinateReferenceSystem()).setNoValue(HMConstants.doubleNovalue)
                .setInitialValue(HMConstants.doubleNovalue).build();
    }

    /**
     * Sets the cells of the Bresenham line between two cells, skipping the ones outside of the grid.
     */
    private static void rasterizeSegment( HMRaster raster, int col1, int row1, int col2, int row2, double value )
            throws Exception {
        int dCol = Math.abs(col2 - col1);
        int dRow = -Math.abs(row2 - row1);
        int stepCol = col1 < col2 ? 1 : -1;
        int stepRow = row1 < row2 ? 1 : -1;
        int error = dCol + dRow;
        int col = col1;
        int row = row1;
        while( true ) {
            if (raster.isContained(col, row)) {
                double current = raster.getValue(col, row);
                if (raster.isNovalue(current) || value > current) {
                    raster.setValue(col, row, value);
                }
            }
            if (col == col2 && row == row2) {
                break;
            }
            int error2 = 2 * error;
            if (error2 >= dRow) {
                error += dRow;
                col += stepCol;
            }
            if (error2 <= dCol) {
                error += dCol;
                row += stepRow;
            }
        }
    }
}
