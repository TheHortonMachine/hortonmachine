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
package org.jgrasstools.gears.modules.v.vectoroperations;

import static org.jgrasstools.gears.libs.modules.Variables.CAP_FLAT;
import static org.jgrasstools.gears.libs.modules.Variables.CAP_ROUND;
import static org.jgrasstools.gears.libs.modules.Variables.CAP_SQUARE;
import static org.jgrasstools.gears.libs.modules.Variables.JOIN_BEVEL;
import static org.jgrasstools.gears.libs.modules.Variables.JOIN_MITRE;
import static org.jgrasstools.gears.libs.modules.Variables.JOIN_ROUND;

import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.gears.io.vectorreader.VectorReader;
import org.jgrasstools.gears.io.vectorwriter.VectorWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.buffer.BufferOp;
import com.vividsolutions.jts.operation.buffer.BufferParameters;

@Description("A module that performs a buffer operation on a vector layer.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("JTS, Buffer")
@Status(Status.EXPERIMENTAL)
@Name("vbuffer")
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Buffer extends JGTModel {

    @Description("The input vector map.")
    @In
    public SimpleFeatureCollection inMap = null;

    @Description("The buffer distance.")
    @In
    public double pBuffer = 10.0;

    @Description("Flag to toggle singlesided buffer.")
    @In
    public boolean doSinglesided = false;

    @Description("The join style to use.")
    @UI("combo:" + JOIN_ROUND + "," + JOIN_MITRE + "," + JOIN_BEVEL)
    @In
    private String pJoinstyle = JOIN_ROUND;

    @Description("Flag to toggle singlesided buffer.")
    @UI("combo:" + CAP_ROUND + "," + CAP_FLAT + "," + CAP_SQUARE)
    @In
    private String pCapstyle = CAP_ROUND;

    @Description("The buffered vector map.")
    @Out
    public SimpleFeatureCollection outMap = null;

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

            BufferParameters bP = new BufferParameters(quadrantSegments, endCapStyle, joinStyle, mitreLimit);
            Geometry bufferedGeom = BufferOp.bufferOp(geometry, pBuffer, bP);
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

    public static void main( String[] args ) throws Exception {
        String in = "D:/Dropbox/hydrologis/lavori/2012_11_ponte_arche/idraulico2d/ciresa/shape/ciresa_canale_intubato.shp";
        String out = "D:/Dropbox/hydrologis/lavori/2012_11_ponte_arche/idraulico2d/ciresa/shape/ciresa_canale_intubato_buffer06.shp";

        Buffer b = new Buffer();
        b.inMap = VectorReader.readVector(in);
        b.pBuffer = 0.6;
        b.pCapstyle = CAP_FLAT;
        b.process();
        SimpleFeatureCollection outMap2 = b.outMap;
        VectorWriter.writeVector(out, outMap2);
    }

}
