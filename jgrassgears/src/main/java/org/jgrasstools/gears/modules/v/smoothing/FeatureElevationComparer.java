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
package org.jgrasstools.gears.modules.v.smoothing;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A wrapper for {@link SimpleFeature}s so that they are sorted by a numeric field.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureElevationComparer implements Comparable<FeatureElevationComparer> {

    private SimpleFeature feature;
    private double elevation;
    private Geometry geometry;
    private boolean isDirty = false;

    public FeatureElevationComparer( SimpleFeature feature, String field ) {
        this.feature = feature;

        elevation = ((Number) feature.getAttribute(field)).doubleValue();
        geometry = (Geometry) feature.getDefaultGeometry();
    }

    public SimpleFeature getFeature() {
        return feature;
    }

    public Geometry getGeometry() {
        return geometry;
    }

    public double getElevation() {
        return elevation;
    }
    
    public boolean isDirty() {
        return isDirty;
    }
    
    public void setDirty( boolean isDirty ) {
        this.isDirty = isDirty;
    }

    /**
     * @param newGeometry new geometry to insert.
     */
    public void substituteGeometry( Geometry newGeometry ) {
        Object[] attributes = feature.getAttributes().toArray();
        Object[] newAttributes = new Object[attributes.length];
        System.arraycopy(attributes, 0, newAttributes, 0, attributes.length);
        newAttributes[0] = newGeometry;

        SimpleFeatureType featureType = feature.getFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        builder.addAll(newAttributes);
        feature = builder.buildFeature(feature.getID());
        geometry = newGeometry;
    }

    public int compareTo( FeatureElevationComparer o ) {
        if (elevation < o.getElevation()) {
            return -1;
        } else if (elevation > o.getElevation()) {
            return 1;
        } else
            return 0;
    }

}
