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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_IN_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_OUT_VECTOR_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_P_TRANS_X_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_P_TRANS_Y_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSVECTORTRANSFORMER_STATUS;

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
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform;

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

    @Description("The rotation angle in clockwise degrees.")
    @In
    public Double pRotate;

    @Description(OMSVECTORTRANSFORMER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        outVector = new DefaultFeatureCollection();
        SimpleFeatureType featureType = inVector.getSchema();
        ReferencedEnvelope vectorBounds = inVector.getBounds();
        double centerX = vectorBounds.getMinX() + (vectorBounds.getMaxX() - vectorBounds.getMinX()) / 2.0;
        double centerY = vectorBounds.getMinY() + (vectorBounds.getMaxY() - vectorBounds.getMinY()) / 2.0;
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
            ((DefaultFeatureCollection) outVector).add(newFeature);
            pm.worked(1);
        }
        inFeatureIterator.close();
        pm.done();
    }

}
