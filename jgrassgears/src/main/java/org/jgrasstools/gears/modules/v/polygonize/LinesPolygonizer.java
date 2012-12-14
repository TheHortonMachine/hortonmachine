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
package org.jgrasstools.gears.modules.v.polygonize;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities.GEOMETRYTYPE;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.polygonize.Polygonizer;

@Description("Polygonizes a layer of lines.")
@Author(name = "Antonio Falciano, Andrea Antonello", contact = "afalciano@yahoo.it, http://blog.spaziogis.it/, http://www.hydrologis.com")
@Keywords("Vector, Polygonize")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class LinesPolygonizer extends JGTModel {

    @Description("The map of lines to polygonize.")
    @In
    public SimpleFeatureCollection inMap = null;

    @Description("The map of points containing the id to put in the polygons attributes (optional).")
    @In
    public SimpleFeatureCollection inPoints = null;

    @Description("The field of the points layer containing the id for the polygons (necessary if inPoints is defined).")
    @In
    public String fId = null;

    @Description("The field of the polygonized layer containing the id (default is 'id').")
    @In
    public String fNewId = "id";

    @Description("The map of polygons.")
    @Out
    public SimpleFeatureCollection outMap = null;

    @Execute
    public void process() throws Exception {
        checkNull(inMap);

        outMap = FeatureCollections.newCollection();

        GEOMETRYTYPE geometryType = GeometryUtilities.getGeometryType(inMap.getSchema().getGeometryDescriptor().getType());
        switch( geometryType ) {
        case LINE:
        case MULTILINE:
            break;
        default:
            throw new ModelsIllegalargumentException("The module only works with line layers.", this);
        }

        List<Geometry> linesList = FeatureUtilities.featureCollectionToGeometriesList(inMap, true, null);

        // Polygonization
        final Polygonizer polygonizer = new Polygonizer();
        polygonizer.add(linesList);
        @SuppressWarnings("unchecked")
        final Collection<Polygon> polygonizedLines = polygonizer.getPolygons();

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("polygonized");
        b.setCRS(inMap.getSchema().getCoordinateReferenceSystem());
        b.add("the_geom", Polygon.class);
        b.add(fNewId, String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        List<Geometry> pointGeometries = new ArrayList<Geometry>();
        if (inPoints != null) {
            fId = FeatureUtilities.findAttributeName(inPoints.getSchema(), fId);
            pointGeometries = FeatureUtilities.featureCollectionToGeometriesList(inPoints, false, fId);
        }

        pm.beginTask("Generating polygon features...", polygonizedLines.size());
        int index = 0;
        for( Polygon polygon : polygonizedLines ) {
            String attribute = String.valueOf(index++);
            if (inPoints != null) {
                attribute = "-";
                for( Geometry point : pointGeometries ) {
                    if (polygon.contains(point)) {
                        attribute = point.getUserData().toString();
                        break;
                    }
                }
            }

            Object[] values = new Object[]{polygon, attribute};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);
            outMap.add(feature);

            pm.worked(1);
        }
        pm.done();
    }
}
