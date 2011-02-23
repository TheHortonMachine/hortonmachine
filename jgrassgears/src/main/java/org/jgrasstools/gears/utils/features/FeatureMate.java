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
package org.jgrasstools.gears.utils.features;

import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometry;
import com.vividsolutions.jts.geom.prep.PreparedGeometryFactory;

/**
 * A wrapper for features that helps out with lots of stuff.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureMate {

    private final SimpleFeature feature;
    private Geometry geometry;
    private PreparedGeometry preparedGeometry;
    private Envelope envelope;

    /**
     * Constructor.
     * 
     * @param feature the feature to wrap.
     */
    public FeatureMate( SimpleFeature feature ) {
        this.feature = feature;
    }

    /**
     * Getter for the geometry.
     * 
     * @return the geometry of the feature.
     */
    public Geometry getGeometry() {
        if (geometry == null)
            geometry = (Geometry) feature.getDefaultGeometry();
        return geometry;
    }

    /**
     * Getter for the {@link Envelope}.
     * 
     * @return the envelope.
     */
    public Envelope getEnvelope() {
        if (envelope == null)
            envelope = getGeometry().getEnvelopeInternal();
        return envelope;
    }

    /**
     * Check for intersection.
     * 
     * @param geometry the geometry to check against.
     * @param usePrepared use prepared geometry.
     * @return true if the geometries intersect.
     */
    public boolean intersects( Geometry geometry, boolean usePrepared ) {
        if (!getEnvelope().intersects(geometry.getEnvelopeInternal())) {
            return false;
        }
        if (usePrepared) {
            if (preparedGeometry == null) {
                preparedGeometry = PreparedGeometryFactory.prepare(getGeometry());
            }
            return preparedGeometry.intersects(geometry);
        } else {
            return getGeometry().intersects(geometry);
        }
    }

    /**
     * Proxy for the intersection method.
     * 
     * @param geometry the geometry to intersect.
     * @return the intersection geometry.
     */
    public Geometry intersection( Geometry geometry ) {
        return getGeometry().intersection(geometry);
    }

}