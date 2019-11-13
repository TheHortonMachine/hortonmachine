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
package org.hortonmachine.gears.modules.v.vectorsimplifier;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.libs.modules.Variables.DOUGLAS_PEUCKER;
import static org.hortonmachine.gears.libs.modules.Variables.PRECISION_REDUCER;
import static org.hortonmachine.gears.libs.modules.Variables.TOPOLOGYPRESERVINGSIMPLIFIER;
import static org.hortonmachine.gears.modules.v.vectorsimplifier.OmsVectorSimplifier.OMSVECTORSIMPLIFIER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectorsimplifier.OmsVectorSimplifier.OMSVECTORSIMPLIFIER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectorsimplifier.OmsVectorSimplifier.OMSVECTORSIMPLIFIER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectorsimplifier.OmsVectorSimplifier.OMSVECTORSIMPLIFIER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectorsimplifier.OmsVectorSimplifier.OMSVECTORSIMPLIFIER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectorsimplifier.OmsVectorSimplifier.OMSVECTORSIMPLIFIER_LABEL;
import static org.hortonmachine.gears.modules.v.vectorsimplifier.OmsVectorSimplifier.OMSVECTORSIMPLIFIER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectorsimplifier.OmsVectorSimplifier.OMSVECTORSIMPLIFIER_NAME;
import static org.hortonmachine.gears.modules.v.vectorsimplifier.OmsVectorSimplifier.OMSVECTORSIMPLIFIER_STATUS;

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.PrecisionModel;
import org.locationtech.jts.precision.GeometryPrecisionReducer;
import org.locationtech.jts.simplify.DouglasPeuckerSimplifier;
import org.locationtech.jts.simplify.TopologyPreservingSimplifier;
import org.opengis.feature.simple.SimpleFeature;

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

@Description(OMSVECTORSIMPLIFIER_DESCRIPTION)
@Documentation(OMSVECTORSIMPLIFIER_DOCUMENTATION)
@Author(name = OMSVECTORSIMPLIFIER_AUTHORNAMES, contact = OMSVECTORSIMPLIFIER_AUTHORCONTACTS)
@Keywords(OMSVECTORSIMPLIFIER_KEYWORDS)
@Label(OMSVECTORSIMPLIFIER_LABEL)
@Name(OMSVECTORSIMPLIFIER_NAME)
@Status(OMSVECTORSIMPLIFIER_STATUS)
@License(OMSVECTORSIMPLIFIER_LICENSE)
public class OmsVectorSimplifier extends HMModel {

    @Description(OMSVECTORSIMPLIFIER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSVECTORSIMPLIFIER_P_TYPE_DESCRIPTION)
    @UI("combo:" + TOPOLOGYPRESERVINGSIMPLIFIER + "," + DOUGLAS_PEUCKER + "," + PRECISION_REDUCER)
    @In
    public String pType = TOPOLOGYPRESERVINGSIMPLIFIER;

    @Description(OMSVECTORSIMPLIFIER_P_TOLERANCE_DESCRIPTION)
    @In
    public double pTolerance = 0.2;

    @Description(OMSVECTORSIMPLIFIER_P_SCALE_DESCRIPTION)
    @In
    public Double pScale = 1000.0;

    @Description(OMSVECTORSIMPLIFIER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector;

    // PARAM DESC START
    public static final String OMSVECTORSIMPLIFIER_DESCRIPTION = "Collection of vector simplification algorithms.";
    public static final String OMSVECTORSIMPLIFIER_DOCUMENTATION = "OmsVectorSimplifier.html";
    public static final String OMSVECTORSIMPLIFIER_KEYWORDS = "Simplify, OmsLineSmootherMcMaster, OmsLineSmootherJaitools";
    public static final String OMSVECTORSIMPLIFIER_LABEL = VECTORPROCESSING;
    public static final String OMSVECTORSIMPLIFIER_NAME = "vsimplify";
    public static final int OMSVECTORSIMPLIFIER_STATUS = 40;
    public static final String OMSVECTORSIMPLIFIER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSVECTORSIMPLIFIER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORSIMPLIFIER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSVECTORSIMPLIFIER_IN_VECTOR_DESCRIPTION = "The vector to be simplified.";
    public static final String OMSVECTORSIMPLIFIER_P_TYPE_DESCRIPTION = "The simplification type: TopologyPreservingSimplifier = 0, Douglas Peucker = 1 (default = 0).";
    public static final String OMSVECTORSIMPLIFIER_P_TOLERANCE_DESCRIPTION = "The distance tolerance for the simplification.";
    public static final String OMSVECTORSIMPLIFIER_P_SCALE_DESCRIPTION = "The amount by which to multiply a coordinate after subtracting the offset, to obtain a precise coordinate";
    public static final String OMSVECTORSIMPLIFIER_OUT_VECTOR_DESCRIPTION = "The simplified vector.";
    // PARAM DESC END

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }
        FeatureIterator<SimpleFeature> inFeatureIterator = inVector.features();

        outVector = new DefaultFeatureCollection();

        FeatureGeometrySubstitutor fGS = new FeatureGeometrySubstitutor(inVector.getSchema());

        GeometryPrecisionReducer geometryReducer = null;

        int size = inVector.size();
        pm.beginTask("Simplifing features...", size);
        while( inFeatureIterator.hasNext() ) {
            SimpleFeature feature = inFeatureIterator.next();

            Geometry geometry = (Geometry) feature.getDefaultGeometry();

            List<Geometry> geomList = new ArrayList<Geometry>();

            int numGeometries = geometry.getNumGeometries();
            for( int i = 0; i < numGeometries; i++ ) {
                Geometry geometryN = geometry.getGeometryN(i);
                switch( pType ) {
                case TOPOLOGYPRESERVINGSIMPLIFIER:
                    TopologyPreservingSimplifier tpSimplifier = new TopologyPreservingSimplifier(geometryN);
                    tpSimplifier.setDistanceTolerance(pTolerance);
                    Geometry tpsGeometry = tpSimplifier.getResultGeometry();
                    geomList.add(tpsGeometry);
                    break;
                case DOUGLAS_PEUCKER:
                    DouglasPeuckerSimplifier dpSimplifier = new DouglasPeuckerSimplifier(geometryN);
                    dpSimplifier.setDistanceTolerance(pTolerance);
                    Geometry dpsGeometry = dpSimplifier.getResultGeometry();
                    geomList.add(dpsGeometry);
                    break;
                case PRECISION_REDUCER:
                    if (geometryReducer == null) {
                        if (pScale == null) {
                            throw new ModelsIllegalargumentException(
                                    "To reduce the precision the scale parameter needs to be set.", this);
                        }
                        geometryReducer = new GeometryPrecisionReducer(new PrecisionModel(pScale));
                    }
                    geomList.add(geometryReducer.reduce(geometryN));
                default:
                }

            }

            Geometry newGeometry = null;
            if (geomList.size() == 1) {
                newGeometry = geomList.get(0);
            } else {
                Geometry[] geomArray = (Geometry[]) geomList.toArray(new Geometry[geomList.size()]);
                newGeometry = new GeometryCollection(geomArray, gf);
            }

            SimpleFeature newFeature = fGS.substituteGeometry(feature, newGeometry);

            ((DefaultFeatureCollection) outVector).add(newFeature);
            pm.worked(1);
        }
        pm.done();

        inFeatureIterator.close();

    }

}
