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

import static org.jgrasstools.gears.libs.modules.Variables.DIFFERENCE;
import static org.jgrasstools.gears.libs.modules.Variables.INTERSECTION;
import static org.jgrasstools.gears.libs.modules.Variables.SYMDIFFERENCE;
import static org.jgrasstools.gears.libs.modules.Variables.UNION;

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
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

@Description("A module that performs overlay operations on a pure geometric layer. The resulting feature layer does not consider original attributes tables.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("JTS, Overlay, Union, Intersect, SymDifference, Difference")
@Status(Status.EXPERIMENTAL)
@Name("overlay")
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class VectorOverlayOperators extends JGTModel {

    @Description("The first vector map.")
    @In
    public SimpleFeatureCollection inMap1 = null;

    @Description("The second vector map.")
    @In
    public SimpleFeatureCollection inMap2 = null;

    @Description("The overlay type to perform.")
    @UI("combo:" + INTERSECTION + "," + UNION + "," + DIFFERENCE + "," + SYMDIFFERENCE)
    @In
    public String pType = INTERSECTION;

    @Description("The resulting vector map.")
    @Out
    public SimpleFeatureCollection outMap = null;

    @Execute
    public void process() throws Exception {
        checkNull(inMap1, inMap2);

        CoordinateReferenceSystem crs = inMap1.getSchema().getCoordinateReferenceSystem();

        outMap = FeatureCollections.newCollection();

        SimpleFeatureBuilder builder = null;

        pm.message("Preparing geometry layers...");

        GeometryFactory gf = GeometryUtilities.gf();
        List<Geometry> geoms1 = FeatureUtilities.featureCollectionToGeometriesList(inMap1, false, null);
        List<Geometry> geoms2 = FeatureUtilities.featureCollectionToGeometriesList(inMap2, false, null);

        GeometryCollection geometryCollection1 = new GeometryCollection(geoms1.toArray(new Geometry[0]), gf);
        GeometryCollection geometryCollection2 = new GeometryCollection(geoms2.toArray(new Geometry[0]), gf);
        Geometry g1 = geometryCollection1.buffer(0);
        Geometry g2 = geometryCollection2.buffer(0);

        pm.beginTask("Performing overlay operation...", IJGTProgressMonitor.UNKNOWN);
        Geometry resultingGeometryCollection = null;
        if (pType.equals(INTERSECTION)) {
            resultingGeometryCollection = g1.intersection(g2);
        } else if (pType.equals(UNION)) {
            resultingGeometryCollection = g1.union(g2);
        } else if (pType.equals(DIFFERENCE)) {
            resultingGeometryCollection = g1.difference(g2);
        } else if (pType.equals(SYMDIFFERENCE)) {
            resultingGeometryCollection = g1.symDifference(g2);
        } else {
            throw new ModelsIllegalargumentException("The overlay type is not supported: " + pType, this);
        }
        pm.done();

        pm.message("Preparing final layer...");
        int numGeometries = resultingGeometryCollection.getNumGeometries();
        for( int i = 0; i < numGeometries; i++ ) {
            Geometry geometryN = resultingGeometryCollection.getGeometryN(i);
            int numGeometries2 = geometryN.getNumGeometries();
            for( int j = 0; j < numGeometries2; j++ ) {
                Geometry geometryN2 = geometryN.getGeometryN(j);

                if (builder == null) {
                    SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
                    b.setName("overlay");
                    b.setCRS(crs);
                    if (geometryN2 instanceof Polygon) {
                        b.add("the_geom", Polygon.class);
                    } else if (geometryN2 instanceof LineString) {
                        b.add("the_geom", LineString.class);
                    } else if (geometryN2 instanceof Point) {
                        b.add("the_geom", Point.class);
                    } else {
                        throw new ModelsRuntimeException("An unexpected geometry type has been created: "
                                + geometryN2.getGeometryType(), this);
                    }
                    b.add("id", Integer.class);
                    SimpleFeatureType type = b.buildFeatureType();
                    builder = new SimpleFeatureBuilder(type);
                }

                Object[] values = new Object[]{geometryN2, i};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                outMap.add(feature);
            }
        }

    }

}
