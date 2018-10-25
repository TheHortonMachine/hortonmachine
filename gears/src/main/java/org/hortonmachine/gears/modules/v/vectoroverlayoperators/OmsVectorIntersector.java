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
package org.hortonmachine.gears.modules.v.vectoroverlayoperators;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_inMap1_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_inMap2_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_outMap_DESCRIPTION;

import java.util.List;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

@Description(OmsVectorIntersector.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsVectorIntersector.KEYWORDS)
@Label(HMConstants.VECTORPROCESSING)
@Name("_" + OmsVectorIntersector.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsVectorIntersector extends HMModel {

    @Description(OMSVECTOROVERLAYOPERATORS_inMap1_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap1 = null;

    @Description(OMSVECTOROVERLAYOPERATORS_inMap2_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap2 = null;

    @Description(KEEP_FIRST_ATTRIBUTES)
    @In
    public boolean doKeepFirstAttributes = true;

    @Description(OMSVECTOROVERLAYOPERATORS_outMap_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMap = null;

    // START VARS DOC
    public static final String NAME = "vectorintersctor";
    public static final String DESCRIPTION = "Vector layer intersector with maintaining of attributes.";
    public static final String KEYWORDS = "vector, intersect, attributes";
    public static final String KEEP_FIRST_ATTRIBUTES = "If enabled attributes of map 1 are kept, else of map 2.";
    // END VARS DOC

    @Execute
    public void process() throws Exception {
        checkNull(inMap1, inMap2);

        outMap = new DefaultFeatureCollection();

        if (!doKeepFirstAttributes) {
            SimpleFeatureCollection inMapTmp = inMap1;
            inMap1 = inMap2;
            inMap2 = inMapTmp;
        }

        List<Geometry> geometries = FeatureUtilities.featureCollectionToGeometriesList(inMap2, false, null);
        GeometryCollection geometryCollection = new GeometryCollection(geometries.toArray(new Geometry[geometries.size()]), gf);
        Geometry intersectionGeometry = geometryCollection.buffer(0);
        PreparedGeometry preparedIntersectionGeometry = PreparedGeometryFactory.prepare(intersectionGeometry);

        List<SimpleFeature> mainFeatures = FeatureUtilities.featureCollectionToList(inMap1);
        if (mainFeatures.size() == 0) {
            throw new ModelsIllegalargumentException("No features found in the layer.", this);
        }

        EGeometryType geometryType = EGeometryType.forGeometry((Geometry) mainFeatures.get(0).getDefaultGeometry());
        Class< ? > multiClazz = geometryType.getMultiClazz();
        EGeometryType newGeometryType = EGeometryType.forClass(multiClazz);
        FeatureGeometrySubstitutor sub = new FeatureGeometrySubstitutor(inMap1.getSchema(), multiClazz);

        pm.beginTask("Performing intersection...", mainFeatures.size());
        for( SimpleFeature feature : mainFeatures ) {
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            if (preparedIntersectionGeometry.intersects(geometry)) {
                Geometry intersection = geometry.intersection(intersectionGeometry);

                EGeometryType intersectionGeometryType = EGeometryType.forGeometry(intersection);
                if (intersectionGeometryType.isCompatibleWith(newGeometryType)) {
                    SimpleFeature newFeature = sub.substituteGeometry(feature, intersection);
                    ((DefaultFeatureCollection) outMap).add(newFeature);
                } else {
                    pm.errorMessage("Could not add intersection result geometry to layer due to incompatibility: " + intersection);
                }
            }
            pm.worked(1);
        }
        pm.done();

    }

}
