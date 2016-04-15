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
package org.jgrasstools.gears.modules.v.smoothing;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_IN_VECTOR_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_OUT_VECTOR_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSPOLYGONSMOOTHERJAITOOLS_P_ALPHA_DESCRIPTION;

import java.util.ArrayList;
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
import org.jaitools.jts.PolygonSmoother;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

@Description(OMSPOLYGONSMOOTHERJAITOOLS_DESCRIPTION)
@Author(name = OMSPOLYGONSMOOTHERJAITOOLS_AUTHORNAMES, contact = OMSPOLYGONSMOOTHERJAITOOLS_AUTHORCONTACTS)
@Keywords(OMSPOLYGONSMOOTHERJAITOOLS_KEYWORDS)
@Label(OMSPOLYGONSMOOTHERJAITOOLS_LABEL)
@Name(OMSPOLYGONSMOOTHERJAITOOLS_NAME)
@Status(OMSPOLYGONSMOOTHERJAITOOLS_STATUS)
@License(OMSPOLYGONSMOOTHERJAITOOLS_LICENSE)
public class OmsPolygonSmootherJaitools extends JGTModel {

    @Description(OMSPOLYGONSMOOTHERJAITOOLS_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSPOLYGONSMOOTHERJAITOOLS_P_ALPHA_DESCRIPTION)
    @In
    public double pAlpha = 0;

    @Description(OMSPOLYGONSMOOTHERJAITOOLS_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector;

    private GeometryFactory gF = GeometryUtilities.gf();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        outVector = new DefaultFeatureCollection();

        pm.message("Collecting geometries...");
        List<SimpleFeature> polygonsList = FeatureUtilities.featureCollectionToList(inVector);
        int size = inVector.size();
        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(inVector.getSchema());
        pm.beginTask("Smoothing features...", size);
        PolygonSmoother smoother = new PolygonSmoother(gF);
        for( SimpleFeature polygonsFeature : polygonsList ) {
            Geometry geometry = (Geometry) polygonsFeature.getDefaultGeometry();
            int numGeometries = geometry.getNumGeometries();
            List<Polygon> smoothedList = new ArrayList<Polygon>();
            for( int i = 0; i < numGeometries; i++ ) {
                Geometry geometryN = geometry.getGeometryN(i);
                if (geometryN instanceof Polygon) {
                    Polygon polygon = (Polygon) geometryN;
                    Polygon smoothed = smoother.smooth(polygon, pAlpha);
                    smoothedList.add(smoothed);
                }
            }
            if (smoothedList.size() != 0) {
                Polygon[] lsArray = (Polygon[]) smoothedList.toArray(new Polygon[smoothedList.size()]);
                MultiPolygon multiPolygonString = gF.createMultiPolygon(lsArray);
                SimpleFeature newFeature = fGS.substituteGeometry(polygonsFeature, multiPolygonString);
                ((DefaultFeatureCollection) outVector).add(newFeature);
            }
            pm.worked(1);
        }
        pm.done();
    }

}
