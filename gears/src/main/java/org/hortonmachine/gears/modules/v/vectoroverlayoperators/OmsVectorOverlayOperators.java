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

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.DIFFERENCE;
import static org.hortonmachine.gears.libs.modules.Variables.INTERSECTION;
import static org.hortonmachine.gears.libs.modules.Variables.SYMDIFFERENCE;
import static org.hortonmachine.gears.libs.modules.Variables.UNION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_LABEL;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_LICENSE;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_NAME;
import static org.hortonmachine.gears.modules.v.vectoroverlayoperators.OmsVectorOverlayOperators.OMSVECTOROVERLAYOPERATORS_STATUS;

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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

@Description(OMSVECTOROVERLAYOPERATORS_DESCRIPTION)
@Documentation(OMSVECTOROVERLAYOPERATORS_DOCUMENTATION)
@Author(name = OMSVECTOROVERLAYOPERATORS_AUTHORNAMES, contact = OMSVECTOROVERLAYOPERATORS_AUTHORCONTACTS)
@Keywords(OMSVECTOROVERLAYOPERATORS_KEYWORDS)
@Label(OMSVECTOROVERLAYOPERATORS_LABEL)
@Name(OMSVECTOROVERLAYOPERATORS_NAME)
@Status(OMSVECTOROVERLAYOPERATORS_STATUS)
@License(OMSVECTOROVERLAYOPERATORS_LICENSE)
public class OmsVectorOverlayOperators extends HMModel {


    @Description(OMSVECTOROVERLAYOPERATORS_inMap1_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap1 = null;

    @Description(OMSVECTOROVERLAYOPERATORS_inMap2_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap2 = null;

    @Description(OMSVECTOROVERLAYOPERATORS_pType_DESCRIPTION)
    @UI("combo:" + INTERSECTION + "," + UNION + "," + DIFFERENCE + "," + SYMDIFFERENCE)
    @In
    public String pType = INTERSECTION;

    @Description(doAllowHoles_DESCRIPTION)
    @In
    public boolean doAllowHoles = true;

    @Description(OMSVECTOROVERLAYOPERATORS_outMap_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMap = null;
    
    // VARS DOCS START
    public static final String OMSVECTOROVERLAYOPERATORS_DESCRIPTION = "A module that performs overlay operations on a pure geometric layer. The resulting feature layer does not consider original attributes tables.";
    public static final String OMSVECTOROVERLAYOPERATORS_DOCUMENTATION = "";
    public static final String OMSVECTOROVERLAYOPERATORS_KEYWORDS = "JTS, Overlay, Union, Intersect, SymDifference, Difference";
    public static final String OMSVECTOROVERLAYOPERATORS_LABEL = VECTORPROCESSING;
    public static final String OMSVECTOROVERLAYOPERATORS_NAME = "overlay";
    public static final int OMSVECTOROVERLAYOPERATORS_STATUS = 5;
    public static final String OMSVECTOROVERLAYOPERATORS_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String OMSVECTOROVERLAYOPERATORS_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTOROVERLAYOPERATORS_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSVECTOROVERLAYOPERATORS_inMap1_DESCRIPTION = "The first vector map.";
    public static final String OMSVECTOROVERLAYOPERATORS_inMap2_DESCRIPTION = "The second vector map.";
    public static final String OMSVECTOROVERLAYOPERATORS_pType_DESCRIPTION = "The overlay type to perform.";
    public static final String OMSVECTOROVERLAYOPERATORS_outMap_DESCRIPTION = "The resulting vector map.";
    private static final String doAllowHoles_DESCRIPTION = "Allow holes in the result.";
    // VARS DOCS STOP

    @Execute
    public void process() throws Exception {
        if (pType.equals(UNION)) {
            checkNull(inMap1);
        } else {
            checkNull(inMap1, inMap2);
        }

        CoordinateReferenceSystem crs = inMap1.getSchema().getCoordinateReferenceSystem();

        outMap = new DefaultFeatureCollection();

        SimpleFeatureBuilder builder = null;

        pm.message("Preparing geometry layers...");

        List<Geometry> geoms1 = FeatureUtilities.featureCollectionToGeometriesList(inMap1, false, null);
        GeometryCollection geometryCollection1 = new GeometryCollection(geoms1.toArray(new Geometry[geoms1.size()]), gf);
        Geometry g1 = geometryCollection1.buffer(0);

        Geometry g2 = null;
        if (inMap2 != null) {
            List<Geometry> geoms2 = FeatureUtilities.featureCollectionToGeometriesList(inMap2, false, null);
            GeometryCollection geometryCollection2 = new GeometryCollection(geoms2.toArray(new Geometry[geoms2.size()]), gf);
            g2 = geometryCollection2.buffer(0);
        }

        pm.beginTask("Performing overlay operation...", IHMProgressMonitor.UNKNOWN);
        Geometry resultingGeometryCollection = null;
        switch (pType) {
            case INTERSECTION:
                resultingGeometryCollection = g1.intersection(g2);
                break;
            case UNION:
                if (inMap2 != null) {
                    resultingGeometryCollection = g1.union(g2);
                } else {
                    resultingGeometryCollection = g1.union();
                }
                break;
            case DIFFERENCE:
                resultingGeometryCollection = g1.difference(g2);
                break;
            case SYMDIFFERENCE:
                resultingGeometryCollection = g1.symDifference(g2);
                break;
            default:
                throw new ModelsIllegalargumentException("The overlay type is not supported: " + pType, this, pm);
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
                
                if (geometryN2 instanceof Polygon && !doAllowHoles) {
                    // remove holes
                    Polygon polygon = (Polygon) geometryN2;
                    LineString exteriorRing = polygon.getExteriorRing();
                    
                    Coordinate[] coordinates = exteriorRing.getCoordinates();
                    
                    geometryN2 = gf.createPolygon(coordinates);
                }

                Object[] values = new Object[]{geometryN2, i};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                ((DefaultFeatureCollection) outMap).add(feature);
            }
        }

    }

}
