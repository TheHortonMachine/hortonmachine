/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.modules.v.vectortransformer;

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
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureGeometrySubstitutor;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.operation.MathTransform;

import com.vividsolutions.jts.geom.Geometry;

@Description("Module for vector tranforms. Currently only translation is supported.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("Transform, Vector")
@Label(JGTConstants.VECTORPROCESSING)
@Status(Status.CERTIFIED)
@Name("vtrans")
@License("General Public License Version 3 (GPLv3)")
public class VectorTransformer extends JGTModel {

    @Description("The feature collection that has to be transformed.")
    @In
    public SimpleFeatureCollection inVector;

    @Description("The translation along the X axis.")
    @In
    public double pTransX;

    @Description("The translation along the Y axis.")
    @In
    public double pTransY;

    @Description("The transformed feature collection.")
    @Out
    public SimpleFeatureCollection outVector = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        outVector = FeatureCollections.newCollection();
        SimpleFeatureType featureType = inVector.getSchema();

        FeatureGeometrySubstitutor substitutor = new FeatureGeometrySubstitutor(featureType);

        FeatureIterator<SimpleFeature> inFeatureIterator = inVector.features();
        int id = 0;
        pm.beginTask("Transforming geometries...", inVector.size());
        while( inFeatureIterator.hasNext() ) {
            // copy the contents of each feature and transform the geometry
            SimpleFeature feature = inFeatureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();

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
            outVector.add(newFeature);
            pm.worked(1);
        }
        inFeatureIterator.close();
        pm.done();
    }

}
