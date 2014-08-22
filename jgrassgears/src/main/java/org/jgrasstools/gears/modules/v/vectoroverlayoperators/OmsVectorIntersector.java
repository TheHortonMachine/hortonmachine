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
package org.jgrasstools.gears.modules.v.vectoroverlayoperators;

import com.vividsolutions.jts.geom.*;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;
import oms3.annotations.*;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryType;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.List;

import static org.jgrasstools.gears.i18n.GearsMessages.*;
import static org.jgrasstools.gears.libs.modules.Variables.*;

@Description(OmsVectorIntersector.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsVectorIntersector.KEYWORDS)
@Label(JGTConstants.VECTORPROCESSING)
@Name("_" + OmsVectorIntersector.NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsVectorIntersector extends JGTModel {

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

        GeometryType geometryType = GeometryUtilities.getGeometryType((Geometry) mainFeatures.get(0).getDefaultGeometry());
        Class<?> multiClazz = geometryType.getMultiClazz();
        FeatureGeometrySubstitutor sub = new FeatureGeometrySubstitutor(inMap1.getSchema(), multiClazz);


        pm.beginTask("Performing intersection...", mainFeatures.size());
        for (SimpleFeature feature : mainFeatures) {
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            if (preparedIntersectionGeometry.intersects(geometry)) {
                Geometry intersection = geometry.intersection(intersectionGeometry);

                GeometryType intersectionGeometryType = GeometryUtilities.getGeometryType(intersection);
                if (intersectionGeometryType.isCompatibleWith(geometryType)) {
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
