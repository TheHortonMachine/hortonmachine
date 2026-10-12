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
package org.hortonmachine.gears.modules.v.vectortransformer;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer.OMSVECTORTRANSFORMER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer.OMSVECTORTRANSFORMER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer.OMSVECTORTRANSFORMER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer.OMSVECTORTRANSFORMER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer.OMSVECTORTRANSFORMER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer.OMSVECTORTRANSFORMER_LABEL;
import static org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer.OMSVECTORTRANSFORMER_LICENSE;
import static org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer.OMSVECTORTRANSFORMER_NAME;
import static org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer.OMSVECTORTRANSFORMER_STATUS;

import java.awt.geom.AffineTransform;

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
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.operation.MathTransform;

import org.locationtech.jts.geom.Geometry;

@Description(OMSVECTORTRANSFORMER_DESCRIPTION)
@Documentation(OMSVECTORTRANSFORMER_DOCUMENTATION)
@Author(name = OMSVECTORTRANSFORMER_AUTHORNAMES, contact = OMSVECTORTRANSFORMER_AUTHORCONTACTS)
@Keywords(OMSVECTORTRANSFORMER_KEYWORDS)
@Label(OMSVECTORTRANSFORMER_LABEL)
@Name(OMSVECTORTRANSFORMER_NAME)
@Status(OMSVECTORTRANSFORMER_STATUS)
@License(OMSVECTORTRANSFORMER_LICENSE)
public class OmsVectorTransformer extends HMModel {

    @Description(OMSVECTORTRANSFORMER_IN_VECTOR_DESCRIPTION)
    @In
    public SimpleFeatureCollection inVector;

    @Description(OMSVECTORTRANSFORMER_P_TRANS_X_DESCRIPTION)
    @In
    public double pTransX;

    @Description(OMSVECTORTRANSFORMER_P_TRANS_Y_DESCRIPTION)
    @In
    public double pTransY;

    @Description(OMSVECTORTRANSFORMER_P_ROTATE_DESCRIPTION)
    @In
    public Double pRotate;

    @Description(OMSVECTORTRANSFORMER_P_CENTER_X_DESCRIPTION)
    @In
    public Double pCenterX;

    @Description(OMSVECTORTRANSFORMER_P_CENTER_Y_DESCRIPTION)
    @In
    public Double pCenterY;

    @Description(OMSVECTORTRANSFORMER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector = null;

    public static final String OMSVECTORTRANSFORMER_DESCRIPTION = "Moves the geometries of a vector by a translation and rotates them around the center of the vector.";
    public static final String OMSVECTORTRANSFORMER_DOCUMENTATION = """
            The rotation, if given, comes first: all the geometries rotate together, clockwise by the angle, \
            around the same point, so the vector keeps its shape. The point is the given center or, without \
            it, the center of the bounds of the whole vector, which changes with the features that the \
            vector has: give the center to rotate around a known point, as a survey around its station. \
            The translation then moves them by the given amounts, in the units of the crs, east and north for \
            positive values. The crs doesn't change: the module moves the geometries, as to put in place a \
            survey in local coordinates or a design to try in another position, it doesn't reproject them.
            """;
    public static final String OMSVECTORTRANSFORMER_KEYWORDS = "Transform, Vector, Translate, Rotate";
    public static final String OMSVECTORTRANSFORMER_LABEL = VECTORPROCESSING;
    public static final String OMSVECTORTRANSFORMER_NAME = "vtrans";
    public static final int OMSVECTORTRANSFORMER_STATUS = 40;
    public static final String OMSVECTORTRANSFORMER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSVECTORTRANSFORMER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORTRANSFORMER_AUTHORCONTACTS = "www.hydrologis.com";
    public static final String OMSVECTORTRANSFORMER_IN_VECTOR_DESCRIPTION = "The feature collection that has to be transformed.";
    public static final String OMSVECTORTRANSFORMER_P_TRANS_X_DESCRIPTION = "The translation along the X axis.";
    public static final String OMSVECTORTRANSFORMER_P_TRANS_Y_DESCRIPTION = "The translation along the Y axis.";
    public static final String OMSVECTORTRANSFORMER_P_ROTATE_DESCRIPTION = "The optional rotation angle, in clockwise degrees.";
    public static final String OMSVECTORTRANSFORMER_P_CENTER_X_DESCRIPTION = "The optional X of the center of the rotation, with its Y (default: the center of the bounds of the vector).";
    public static final String OMSVECTORTRANSFORMER_P_CENTER_Y_DESCRIPTION = "The optional Y of the center of the rotation, with its X (default: the center of the bounds of the vector).";
    public static final String OMSVECTORTRANSFORMER_OUT_VECTOR_DESCRIPTION = "The transformed feature collection.";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        SimpleFeatureType featureType = inVector.getSchema();
        ReferencedEnvelope vectorBounds = inVector.getBounds();
        if ((pCenterX == null) != (pCenterY == null)) {
            throw new ModelsIllegalargumentException("The center of the rotation needs both X and Y.", this, pm);
        }
        double centerX;
        double centerY;
        if (pCenterX != null) {
            centerX = pCenterX;
            centerY = pCenterY;
        } else {
            centerX = vectorBounds.getMinX() + (vectorBounds.getMaxX() - vectorBounds.getMinX()) / 2.0;
            centerY = vectorBounds.getMinY() + (vectorBounds.getMaxY() - vectorBounds.getMinY()) / 2.0;
        }
        double pAngle = 0;
        MathTransform rotationTransform = null;
        if (pRotate != null) {
            pAngle = pRotate;
            AffineTransform rotationAT = new AffineTransform();
            rotationAT.translate(centerX, centerY);
            rotationAT.rotate(Math.toRadians(-pAngle));
            rotationAT.translate(-centerX, -centerY);
            rotationTransform = new AffineTransform2D(rotationAT);
        }
        FeatureGeometrySubstitutor substitutor = new FeatureGeometrySubstitutor(featureType);
        // a list keeps the features in their order
        ListFeatureCollection outFC = new ListFeatureCollection(substitutor.getNewFeatureType());
        outVector = outFC;

        FeatureIterator<SimpleFeature> inFeatureIterator = inVector.features();
        pm.beginTask("Transforming geometries...", inVector.size());
        while( inFeatureIterator.hasNext() ) {
            // copy the contents of each feature and transform the geometry
            SimpleFeature feature = inFeatureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();

            if (pRotate != null) {
                geometry = JTS.transform(geometry, rotationTransform);
            }

            // m00 the X coordinate scaling element of the 3x3 matrix
            // m10 the Y coordinate shearing element of the 3x3 matrix
            // m01 the X coordinate shearing element of the 3x3 matrix
            // m11 the Y coordinate scaling element of the 3x3 matrix
            // m02 the X coordinate translation element of the 3x3 matrix
            // m12 the Y coordinate translation element of the 3x3 matrix
            // m00, m10, m01, m11, m02, m12
            MathTransform transform = new AffineTransform2D(1.0, 0.0, 0.0, 1.0, pTransX, pTransY);
            Geometry transformedGeometry = JTS.transform(geometry, transform);

            SimpleFeature newFeature = substitutor.substituteGeometry(feature, transformedGeometry);
            outFC.add(newFeature);
            pm.worked(1);
        }
        inFeatureIterator.close();
        pm.done();
    }

}
