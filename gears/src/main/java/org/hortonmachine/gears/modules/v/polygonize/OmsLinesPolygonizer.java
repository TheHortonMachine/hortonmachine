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
package org.hortonmachine.gears.modules.v.polygonize;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_F_ID_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_F_NEW_ID_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_IN_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_IN_POINTS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_OUT_MAP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSLINESPOLYGONIZER_STATUS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.polygonize.Polygonizer;

@Description(OMSLINESPOLYGONIZER_DESCRIPTION)
@Documentation(OMSLINESPOLYGONIZER_DOCUMENTATION)
@Author(name = OMSLINESPOLYGONIZER_AUTHORNAMES, contact = OMSLINESPOLYGONIZER_AUTHORCONTACTS)
@Keywords(OMSLINESPOLYGONIZER_KEYWORDS)
@Label(OMSLINESPOLYGONIZER_LABEL)
@Name(OMSLINESPOLYGONIZER_NAME)
@Status(OMSLINESPOLYGONIZER_STATUS)
@License(OMSLINESPOLYGONIZER_LICENSE)
public class OmsLinesPolygonizer extends HMModel {

    @Description(OMSLINESPOLYGONIZER_IN_MAP_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap = null;

    @Description(OMSLINESPOLYGONIZER_IN_POINTS_DESCRIPTION)
    @In
    public SimpleFeatureCollection inPoints = null;

    @Description(OMSLINESPOLYGONIZER_F_ID_DESCRIPTION)
    @In
    public String fId = null;

    @Description(OMSLINESPOLYGONIZER_F_NEW_ID_DESCRIPTION)
    @In
    public String fNewId = "id";

    @Description(OMSLINESPOLYGONIZER_OUT_MAP_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMap = null;

    @Execute
    public void process() throws Exception {
        checkNull(inMap);

        outMap = new DefaultFeatureCollection();

        EGeometryType geometryType = EGeometryType.forGeometryDescriptor(inMap.getSchema().getGeometryDescriptor());
        switch( geometryType ) {
        case LINESTRING:
        case MULTILINESTRING:
            break;
        default:
            throw new ModelsIllegalargumentException("The module only works with line layers.", this, pm);
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
            ((DefaultFeatureCollection) outMap).add(feature);

            pm.worked(1);
        }
        pm.done();
    }
}
