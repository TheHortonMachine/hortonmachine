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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_F_CAT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_CAT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_COLS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_EAST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_NORTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_ROWS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_SOUTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_P_WEST_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESRASTERIZER_STATUS;
import static org.hortonmachine.gears.utils.coverage.CoverageUtilities.gridGeometryFromRegionValues;

import java.awt.image.WritableRaster;
import java.util.List;

import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

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

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.DirectPosition2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

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

    @Execute
    public void process() throws Exception {
        checkNull(inVector);

        if (pNorth == null || pSouth == null || pWest == null || pEast == null || pRows == null || pCols == null) {
            throw new ModelsIllegalargumentException(
                    "It is necessary to supply all the information about the processing region. Did you set the boundaries and rows/cols?",
                    this, pm);
        }
        SimpleFeatureType schema = inVector.getSchema();
        CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
        GridGeometry2D inGrid = gridGeometryFromRegionValues(pNorth, pSouth, pEast, pWest, pCols, pRows, crs);

        if (!EGeometryType.isLine(schema.getGeometryDescriptor())) {
            throw new ModelsRuntimeException("The module works only with line vectors.", this);
        }

        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(inGrid);
        double n = regionMap.getNorth();
        double s = regionMap.getSouth();
        double e = regionMap.getEast();
        double w = regionMap.getWest();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();
        double step = Math.min(xRes, yRes);

        WritableRaster outWR = CoverageUtilities.createWritableRaster(regionMap.getCols(), regionMap.getRows(), null, null,
                HMConstants.doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        List<FeatureMate> matesList = FeatureUtilities.featureCollectionToMatesList(inVector);
        pm.beginTask("Rasterizing lines...", matesList.size());
        String fCatChecked = null;
        for( FeatureMate featureMate : matesList ) {
            Geometry geometry = featureMate.getGeometry();
            for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
                Geometry geometryN = geometry.getGeometryN(i);
                List<Coordinate> lineCoordinatesAtStep = GeometryUtilities.getCoordinatesAtInterval((LineString) geometryN, step,
                        true, -1, -1);

                double cat;
                if (fCat == null) {
                    cat = pCat;
                } else {
                    if (fCatChecked == null) {
                        fCatChecked = FeatureUtilities.findAttributeName(featureMate.getFeature().getFeatureType(), fCat);
                        if (fCatChecked == null) {
                            throw new ModelsIllegalargumentException("Could not find an attribute named: " + fCat, this, pm);
                        }
                    }
                    cat = featureMate.getAttribute(fCat, Double.class);
                }

                for( Coordinate lineCoordinate : lineCoordinatesAtStep ) {
                    if (!NumericsUtilities.isBetween(lineCoordinate.x, w, e)
                            || !NumericsUtilities.isBetween(lineCoordinate.y, s, n)) {
                        continue;
                    }

                    GridCoordinates2D onGrid = inGrid.worldToGrid(new DirectPosition2D(lineCoordinate.x, lineCoordinate.y));
                    outIter.setSample(onGrid.x, onGrid.y, 0, cat);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outRaster = CoverageUtilities.buildCoverage("pointsraster", outWR, regionMap, inVector.getSchema()
                .getCoordinateReferenceSystem());
    }
}
