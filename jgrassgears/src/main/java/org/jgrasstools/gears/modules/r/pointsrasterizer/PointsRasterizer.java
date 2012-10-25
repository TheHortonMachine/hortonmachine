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
package org.jgrasstools.gears.modules.r.pointsrasterizer;

import static org.jgrasstools.gears.utils.geometry.GeometryUtilities.getGeometryType;

import java.awt.image.WritableRaster;
import java.util.List;

import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.DirectPosition2D;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureMate;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities.GEOMETRYTYPE;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.GeometryType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

@Description("Module to convert vector points to raster. Currently this does simply put the point in the nearest cell, without check.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Raster, Vector, Points")
@Label(JGTConstants.RASTERPROCESSING)
@Status(Status.EXPERIMENTAL)
@Name("rasterizepoints")
@License("General Public License Version 3 (GPLv3)")
public class PointsRasterizer extends JGTModel {

    @Description("The points vector.")
    @In
    public SimpleFeatureCollection inVector = null;

    @Description("The grid on which to place the values.")
    @In
    public GridGeometry2D inGrid;

    @Description("The field of the vector to take the category from.")
    @In
    public String fCat;

    @Description("The output raster.")
    @Out
    public GridCoverage2D outRaster;

    @Execute
    public void process() throws Exception {
        checkNull(inGrid, inVector, fCat);

        SimpleFeatureType schema = inVector.getSchema();
        GeometryType type = schema.getGeometryDescriptor().getType();
        if (getGeometryType(type) != GEOMETRYTYPE.POINT && getGeometryType(type) != GEOMETRYTYPE.POINT) {
            throw new ModelsRuntimeException("The module works only with point vectors.", this);
        }

        RegionMap regionMap = CoverageUtilities.gridGeometry2RegionParamsMap(inGrid);
        double n = regionMap.getNorth();
        double s = regionMap.getSouth();
        double e = regionMap.getEast();
        double w = regionMap.getWest();

        WritableRaster outWR = CoverageUtilities.createDoubleWritableRaster(regionMap.getCols(), regionMap.getRows(), null, null,
                JGTConstants.doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        List<FeatureMate> matesList = FeatureUtilities.featureCollectionToMatesList(inVector);
        pm.beginTask("Rasterizing points...", matesList.size());
        for( FeatureMate featureMate : matesList ) {
            Geometry geometry = featureMate.getGeometry();

            Double cat = featureMate.getAttribute(fCat, Double.class);

            Coordinate[] coordinates = geometry.getCoordinates();

            for( Coordinate coordinate : coordinates ) {
                if (!NumericsUtilities.isBetween(coordinate.x, w, e) || !NumericsUtilities.isBetween(coordinate.y, s, n)) {
                    continue;
                }

                GridCoordinates2D onGrid = inGrid.worldToGrid(new DirectPosition2D(coordinate.x, coordinate.y));
                outIter.setSample(onGrid.x, onGrid.y, 0, cat);
            }
            pm.worked(1);
        }
        pm.done();

        outRaster = CoverageUtilities.buildCoverage("pointsraster", outWR, regionMap, inVector.getSchema()
                .getCoordinateReferenceSystem());
    }
}
