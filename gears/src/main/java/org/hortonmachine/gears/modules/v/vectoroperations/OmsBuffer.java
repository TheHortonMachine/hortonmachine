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
package org.hortonmachine.gears.modules.v.vectoroperations;

import static org.hortonmachine.gears.modules.v.vectoroperations.OmsBuffer.*;
import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.CAP_FLAT;
import static org.hortonmachine.gears.libs.modules.Variables.CAP_ROUND;
import static org.hortonmachine.gears.libs.modules.Variables.CAP_SQUARE;
import static org.hortonmachine.gears.libs.modules.Variables.JOIN_BEVEL;
import static org.hortonmachine.gears.libs.modules.Variables.JOIN_MITRE;
import static org.hortonmachine.gears.libs.modules.Variables.JOIN_ROUND;

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
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.buffer.BufferOp;
import org.locationtech.jts.operation.buffer.BufferParameters;

@Description(OMSBUFFER_DESCRIPTION)
@Documentation(OMSBUFFER_DOCUMENTATION)
@Author(name = OMSBUFFER_AUTHORNAMES, contact = OMSBUFFER_AUTHORCONTACTS)
@Keywords(OMSBUFFER_KEYWORDS)
@Label(OMSBUFFER_LABEL)
@Name(OMSBUFFER_NAME)
@Status(OMSBUFFER_STATUS)
@License(OMSBUFFER_LICENSE)
public class OmsBuffer extends HMModel {

    @Description(OMSBUFFER_IN_MAP_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap = null;

    @Description(OMSBUFFER_P_BUFFER_DESCRIPTION)
    @In
    public double pBuffer = 10.0;

    @Description(OMSBUFFER_P_BUFFERFIELD_DESCRIPTION)
    @In
    public String pBufferField;

    @Description(OMSBUFFER_DO_SINGLE_SIDED_DESCRIPTION)
    @In
    public boolean doSinglesided = false;

    @Description(OMSBUFFER_P_JOIN_STYLE_DESCRIPTION)
    @UI("combo:" + JOIN_ROUND + "," + JOIN_MITRE + "," + JOIN_BEVEL)
    @In
    public String pJoinstyle = JOIN_ROUND;

    @Description(OMSBUFFER_P_CAP_STYLE_DESCRIPTION)
    @UI("combo:" + CAP_ROUND + "," + CAP_FLAT + "," + CAP_SQUARE)
    @In
    public String pCapstyle = CAP_ROUND;

    @Description(OMSBUFFER_OUT_MAP_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMap = null;

    // PARAM NAMES START
    public static final String OMSBUFFER_DESCRIPTION = "A module that performs a buffer operation on a vector layer.";
    public static final String OMSBUFFER_DOCUMENTATION = "";
    public static final String OMSBUFFER_KEYWORDS = "JTS, OmsBuffer";
    public static final String OMSBUFFER_LABEL = VECTORPROCESSING;
    public static final String OMSBUFFER_NAME = "vbuffer";
    public static final int OMSBUFFER_STATUS = 5;
    public static final String OMSBUFFER_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String OMSBUFFER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSBUFFER_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSBUFFER_IN_MAP_DESCRIPTION = "The input vector map.";
    public static final String OMSBUFFER_P_BUFFER_DESCRIPTION = "The buffer distance.";
    public static final String OMSBUFFER_P_BUFFERFIELD_DESCRIPTION = "The field to use as buffer distance.";
    public static final String OMSBUFFER_DO_SINGLE_SIDED_DESCRIPTION = "Flag to toggle singlesided buffer.";
    public static final String OMSBUFFER_P_JOIN_STYLE_DESCRIPTION = "The join style to use.";
    public static final String OMSBUFFER_P_CAP_STYLE_DESCRIPTION = "The cap style to use.";
    public static final String OMSBUFFER_OUT_MAP_DESCRIPTION = "The buffered vector map.";
    // PARAM NAMES ENd

    private double mitreLimit = BufferParameters.DEFAULT_MITRE_LIMIT;
    private int quadrantSegments = BufferParameters.DEFAULT_QUADRANT_SEGMENTS;

    @Execute
    public void process() throws Exception {
        checkNull(inMap);

        int joinStyle;
        if (pJoinstyle.equals(JOIN_MITRE)) {
            joinStyle = BufferParameters.JOIN_MITRE;
        } else if (pJoinstyle.equals(JOIN_BEVEL)) {
            joinStyle = BufferParameters.JOIN_BEVEL;
        } else {
            joinStyle = BufferParameters.JOIN_ROUND;
        }
        int endCapStyle;
        if (pCapstyle.equals(CAP_FLAT)) {
            endCapStyle = BufferParameters.CAP_FLAT;
        } else if (pCapstyle.equals(CAP_SQUARE)) {
            endCapStyle = BufferParameters.CAP_SQUARE;
        } else {
            endCapStyle = BufferParameters.CAP_ROUND;
        }

        FeatureGeometrySubstitutor fgs = new FeatureGeometrySubstitutor(inMap.getSchema(), MultiPolygon.class);

        DefaultFeatureCollection outMaptmp = new DefaultFeatureCollection("new", fgs.getNewFeatureType());

        GeometryFactory gf = GeometryUtilities.gf();

        List<SimpleFeature> featuresList = FeatureUtilities.featureCollectionToList(inMap);
        pm.beginTask("Buffering geometries...", featuresList.size());
        for( SimpleFeature feature : featuresList ) {
            Geometry geometry = (Geometry) feature.getDefaultGeometry();

            double buf = pBuffer;
            if (pBufferField != null) {
                Object bFieldObj = feature.getAttribute(pBufferField);
                if (bFieldObj instanceof Number) {
                    buf = ((Number) bFieldObj).doubleValue();
                }
            }

            BufferParameters bP = new BufferParameters(quadrantSegments, endCapStyle, joinStyle, mitreLimit);
            Geometry bufferedGeom = BufferOp.bufferOp(geometry, buf, bP);
            List<Polygon> polygons = new ArrayList<Polygon>(bufferedGeom.getNumGeometries());
            for( int i = 0; i < bufferedGeom.getNumGeometries(); i++ ) {
                Geometry geometryN = bufferedGeom.getGeometryN(i);
                if (geometryN instanceof Polygon) {
                    polygons.add((Polygon) geometryN);
                } else {
                    pm.errorMessage("Ignored non polygonal geometry in: " + geometryN.toText());
                }
            }
            MultiPolygon multiPolygon = gf.createMultiPolygon(polygons.toArray(GeometryUtilities.TYPE_POLYGON));
            SimpleFeature newFeature = fgs.substituteGeometry(feature, multiPolygon);
            outMaptmp.add(newFeature);
            pm.worked(1);
        }
        pm.done();

        outMap = outMaptmp;

    }

}
