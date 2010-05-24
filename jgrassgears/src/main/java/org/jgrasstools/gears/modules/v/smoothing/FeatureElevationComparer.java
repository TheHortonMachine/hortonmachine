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

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Geometry;

/**
 * A wrapper for {@link SimpleFeature}s so that they are sorted by a numeric field.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureElevationComparer implements Comparable<FeatureElevationComparer> {

    private final SimpleFeature feature;
    private double elevation;
    private Geometry geometry;

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

    public int compareTo( FeatureElevationComparer o ) {
        if (elevation < o.getElevation()) {
            return -1;
        } else if (elevation > o.getElevation()) {
            return 1;
        } else
            return 0;
    }

}
