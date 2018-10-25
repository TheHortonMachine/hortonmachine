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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_F_CAT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_IN_GRID_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_OUT_RASTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSPOINTSRASTERIZER_STATUS;

import java.awt.image.WritableRaster;
import java.util.List;

import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.DirectPosition2D;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.feature.simple.SimpleFeatureType;

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

    @Description(OMSPOINTSRASTERIZER_IN_GRID_DESCRIPTION)
    @In
    public GridGeometry2D inGrid;

    @Description(OMSPOINTSRASTERIZER_F_CAT_DESCRIPTION)
    @In
    public String fCat;

    @Description(OMSPOINTSRASTERIZER_OUT_RASTER_DESCRIPTION)
    @Out
    public GridCoverage2D outRaster;

    @Execute
    public void process() throws Exception {
        checkNull(inGrid, inVector);

        SimpleFeatureType schema = inVector.getSchema();
        if (!EGeometryType.isPoint(schema.getGeometryDescriptor())) {
            throw new ModelsRuntimeException("The module works only with point vectors.", this);
        }

        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(inGrid);
        double n = regionMap.getNorth();
        double s = regionMap.getSouth();
        double e = regionMap.getEast();
        double w = regionMap.getWest();

        WritableRaster outWR = CoverageUtilities.createWritableRaster(regionMap.getCols(), regionMap.getRows(), null, null,
                HMConstants.doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        List<FeatureMate> matesList = FeatureUtilities.featureCollectionToMatesList(inVector);
        double value = 0;
        pm.beginTask("Rasterizing points...", matesList.size());
        for( FeatureMate featureMate : matesList ) {
            Geometry geometry = featureMate.getGeometry();

            if (fCat == null) {
                Double cat = featureMate.getAttribute(fCat, Double.class);
                if (cat != null) {
                    value = cat;
                }
            }

            Coordinate[] coordinates = geometry.getCoordinates();

            for( Coordinate coordinate : coordinates ) {
                if (!NumericsUtilities.isBetween(coordinate.x, w, e) || !NumericsUtilities.isBetween(coordinate.y, s, n)) {
                    continue;
                }

                GridCoordinates2D onGrid = inGrid.worldToGrid(new DirectPosition2D(coordinate.x, coordinate.y));
                outIter.setSample(onGrid.x, onGrid.y, 0, value);
            }
            pm.worked(1);
        }
        pm.done();

        outRaster = CoverageUtilities.buildCoverage("pointsraster", outWR, regionMap, inVector.getSchema()
                .getCoordinateReferenceSystem());
    }
}
