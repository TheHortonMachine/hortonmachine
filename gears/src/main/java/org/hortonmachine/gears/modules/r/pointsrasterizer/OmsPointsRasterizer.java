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
package org.hortonmachine.gears.modules.r.pointsrasterizer;

import static org.hortonmachine.gears.libs.modules.HMConstants.RASTERPROCESSING;
import static org.hortonmachine.gears.modules.r.pointsrasterizer.OmsPointsRasterizer.OMSPOINTSRASTERIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.r.pointsrasterizer.OmsPointsRasterizer.OMSPOINTSRASTERIZER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.r.pointsrasterizer.OmsPointsRasterizer.OMSPOINTSRASTERIZER_DESCRIPTION;
import static org.hortonmachine.gears.modules.r.pointsrasterizer.OmsPointsRasterizer.OMSPOINTSRASTERIZER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.r.pointsrasterizer.OmsPointsRasterizer.OMSPOINTSRASTERIZER_KEYWORDS;
import static org.hortonmachine.gears.modules.r.pointsrasterizer.OmsPointsRasterizer.OMSPOINTSRASTERIZER_LABEL;
import static org.hortonmachine.gears.modules.r.pointsrasterizer.OmsPointsRasterizer.OMSPOINTSRASTERIZER_LICENSE;
import static org.hortonmachine.gears.modules.r.pointsrasterizer.OmsPointsRasterizer.OMSPOINTSRASTERIZER_NAME;
import static org.hortonmachine.gears.modules.r.pointsrasterizer.OmsPointsRasterizer.OMSPOINTSRASTERIZER_STATUS;

import java.awt.Point;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
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

@Description(OMSPOINTSRASTERIZER_DESCRIPTION)
@Documentation(OMSPOINTSRASTERIZER_DOCUMENTATION)
@Author(name = OMSPOINTSRASTERIZER_AUTHORNAMES, contact = OMSPOINTSRASTERIZER_AUTHORCONTACTS)
@Keywords(OMSPOINTSRASTERIZER_KEYWORDS)
@Label(OMSPOINTSRASTERIZER_LABEL)
@Name(OMSPOINTSRASTERIZER_NAME)
@Status(OMSPOINTSRASTERIZER_STATUS)
@License(OMSPOINTSRASTERIZER_LICENSE)
public class OmsPointsRasterizer extends HMModel {

    @Description(OMSPOINTSRASTERIZER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector = null;

    @Description(OMSPOINTSRASTERIZER_F_CAT_DESCRIPTION)
    @In
    public String fCat;

    @Description(OMSPOINTSRASTERIZER_P_CAT_DESCRIPTION)
    @In
    public double pCat = 1.0;

    @Description(OMSPOINTSRASTERIZER_IN_RASTER_DESCRIPTION)
    @In
    public GridCoverage2D inRaster;

    @Description(OMSPOINTSRASTERIZER_IN_GRID_DESCRIPTION)
    @In
    public GridGeometry2D inGrid;

    @Description(OMSPOINTSRASTERIZER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    public static final String OMSPOINTSRASTERIZER_DESCRIPTION = "Converts points to a raster: the cell of each point gets its value, the others are novalue.";
    public static final String OMSPOINTSRASTERIZER_DOCUMENTATION = """
            Each point gives its value to the cell that contains it; the points on the border of \
            two cells go to the one east or south of it, the ones on the east or south border of \
            the grid to its last column or row, and the ones outside of the grid are left out. When more points fall in the same cell, the cell keeps the largest value. The \
            rasterized points, as 1 where there is a point, give their density through \
            KernelDensity; to interpolate the values of the points on all the cells, \
            SurfaceInterpolator is the tool.

            The value of the cells is the one of the numeric field of the point, or the constant \
            category without a field. The grid is the one of the optional raster, or the one of \
            the bounds, resolution and reference system.
            """;
    public static final String OMSPOINTSRASTERIZER_KEYWORDS = "Raster, Vector, Points, Rasterize";
    public static final String OMSPOINTSRASTERIZER_LABEL = RASTERPROCESSING;
    public static final String OMSPOINTSRASTERIZER_NAME = "rasterizepoints";
    public static final int OMSPOINTSRASTERIZER_STATUS = 40;
    public static final String OMSPOINTSRASTERIZER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSPOINTSRASTERIZER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSPOINTSRASTERIZER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSPOINTSRASTERIZER_IN_VECTOR_DESCRIPTION = "The points to rasterize.";
    public static final String OMSPOINTSRASTERIZER_F_CAT_DESCRIPTION = "The optional numeric field of the points with the value of their cells.";
    public static final String OMSPOINTSRASTERIZER_P_CAT_DESCRIPTION = "The value of the cells if no field is given.";
    public static final String OMSPOINTSRASTERIZER_IN_RASTER_DESCRIPTION = "An optional raster that gives the grid, instead of bounds, resolution and reference system.";
    public static final String OMSPOINTSRASTERIZER_IN_GRID_DESCRIPTION = "The grid on which to place the values, if no raster is given.";
    public static final String OMSPOINTSRASTERIZER_OUT_RASTER_DESCRIPTION = "The rasterized points.";

    @Execute
    public void process() throws Exception {
        checkNull(inVector);
        if (inRaster == null && inGrid == null) {
            throw new ModelsIllegalargumentException("It is necessary to supply a raster or the grid of the output.", this, pm);
        }

        SimpleFeatureType schema = inVector.getSchema();
        if (!EGeometryType.isPoint(schema.getGeometryDescriptor())) {
            throw new ModelsIllegalargumentException("The module works only with point vectors.", this, pm);
        }

        String fCatChecked = null;
        if (fCat != null) {
            fCatChecked = FeatureUtilities.findAttributeName(schema, fCat);
            if (fCatChecked == null) {
                throw new ModelsIllegalargumentException("Could not find an attribute named: " + fCat, this, pm);
            }
        }

        HMRasterWritableBuilder builder = new HMRasterWritableBuilder().setName("rasterized")
                .setNoValue(HMConstants.doubleNovalue).setInitialValue(HMConstants.doubleNovalue);
        if (inRaster != null) {
            builder.setTemplate(inRaster);
        } else {
            builder.setRegion(RegionMap.fromGridGeometry(inGrid)).setCrs(inGrid.getCoordinateReferenceSystem());
        }

        try (HMRaster outData = builder.build()) {
            List<FeatureMate> matesList = FeatureUtilities.featureCollectionToMatesList(inVector);
            pm.beginTask("Rasterizing points...", matesList.size());
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

                for( Coordinate coordinate : featureMate.getGeometry().getCoordinates() ) {
                    Point cell = outData.getCell(coordinate);
                    if (outData.isContained(cell.x, cell.y)) {
                        double current = outData.getValue(cell.x, cell.y);
                        if (outData.isNovalue(current) || value > current) {
                            outData.setValue(cell.x, cell.y, value);
                        }
                    }
                }
                pm.worked(1);
            }
            pm.done();

            outRaster = outData.buildCoverage();
        }
    }
}
