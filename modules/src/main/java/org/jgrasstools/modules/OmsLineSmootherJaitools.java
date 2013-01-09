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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_inVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_outVector_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSLINESMOOTHERJAITOOLS_pAlpha_DESCRIPTION;

import java.util.ArrayList;
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
import org.geotools.feature.FeatureCollections;
import org.jaitools.jts.LineSmoother;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;

@Description(OMSLINESMOOTHERJAITOOLS_DESCRIPTION)
@Documentation(OMSLINESMOOTHERJAITOOLS_DOCUMENTATION)
@Author(name = OMSLINESMOOTHERJAITOOLS_AUTHORNAMES, contact = OMSLINESMOOTHERJAITOOLS_AUTHORCONTACTS)
@Keywords(OMSLINESMOOTHERJAITOOLS_KEYWORDS)
@Label(OMSLINESMOOTHERJAITOOLS_LABEL)
@Name(OMSLINESMOOTHERJAITOOLS_NAME)
@Status(OMSLINESMOOTHERJAITOOLS_STATUS)
@License(OMSLINESMOOTHERJAITOOLS_LICENSE)
public class OmsLineSmootherJaitools extends JGTModel {

    @Description(OMSLINESMOOTHERJAITOOLS_inVector_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSLINESMOOTHERJAITOOLS_pAlpha_DESCRIPTION)
    @In
    public double pAlpha = 0;

    @Description(OMSLINESMOOTHERJAITOOLS_outVector_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector;

    private GeometryFactory gF = GeometryUtilities.gf();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        outVector = FeatureCollections.newCollection();

        pm.message("Collecting geometries...");
        List<SimpleFeature> linesList = FeatureUtilities.featureCollectionToList(inVector);
        int size = inVector.size();
        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(inVector.getSchema());
        pm.beginTask("Smoothing features...", size);
        LineSmoother smoother = new LineSmoother(gF);
        for( SimpleFeature line : linesList ) {
            Geometry geometry = (Geometry) line.getDefaultGeometry();
            int numGeometries = geometry.getNumGeometries();
            List<LineString> smoothedList = new ArrayList<LineString>();
            for( int i = 0; i < numGeometries; i++ ) {
                Geometry geometryN = geometry.getGeometryN(i);
                if (geometryN instanceof LineString) {
                    LineString lineString = (LineString) geometryN;
                    LineString smoothed = smoother.smooth(lineString, pAlpha);
                    smoothedList.add(smoothed);
                }
            }
            if (smoothedList.size() != 0) {
                LineString[] lsArray = (LineString[]) smoothedList.toArray(new LineString[smoothedList.size()]);
                MultiLineString multiLineString = gF.createMultiLineString(lsArray);
                SimpleFeature newFeature = fGS.substituteGeometry(line, multiLineString);
                outVector.add(newFeature);
            }
            pm.worked(1);
        }
        pm.done();
    }

}
