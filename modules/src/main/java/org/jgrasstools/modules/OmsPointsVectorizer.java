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

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.utils.coverage.CoverageUtilities.getRegionParamsFromGridCoverage;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.RegionMap;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.geometry.DirectPosition;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

@Description("Module that creates a points vector layer from raster values.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Raster, Vector")
@Status(Status.EXPERIMENTAL)
@Label(JGTConstants.VECTORPROCESSING)
@Name("_pvectorizer")
@License("General Public License Version 3 (GPLv3)")
public class OmsPointsVectorizer extends JGTModel {

    @Description("The raster that has to be converted.")
    @In
    public GridCoverage2D inRaster;

    @Description("The field name to use as a name for the raster value in the vector.")
    @In
    public String fDefault = "value";

    @Description("The extracted vector.")
    @Out
    public SimpleFeatureCollection outVector = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        checkNull(inRaster);
        GridGeometry2D gridGeometry = inRaster.getGridGeometry();

        RegionMap regionMap = getRegionParamsFromGridCoverage(inRaster);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("pointtype");
        b.setCRS(inRaster.getCoordinateReferenceSystem());
        b.add("the_geom", Point.class);
        b.add(fDefault, Double.class);

        SimpleFeatureType type = b.buildFeatureType();

        GeometryFactory gF = new GeometryFactory();

        outVector = FeatureCollections.newCollection();

        RandomIter rasterIter = RandomIterFactory.create(inRaster.getRenderedImage(), null);

        pm.beginTask("Extracting points...", rows);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double value = rasterIter.getSampleDouble(c, r, 0);
                if (isNovalue(value)) {
                    continue;
                }
                DirectPosition world = gridGeometry.gridToWorld(new GridCoordinates2D(c, r));
                double[] coordinate = world.getCoordinate();
                Coordinate coord = new Coordinate(coordinate[0], coordinate[1]);
                Point point = gF.createPoint(coord);
                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
                Object[] values = new Object[]{point, value};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                outVector.add(feature);
            }
        }
    }

}
