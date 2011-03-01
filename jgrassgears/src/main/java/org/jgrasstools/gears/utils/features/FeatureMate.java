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

import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities.GEOMETRYTYPE;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
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

    /**
     * Tries to convert the internal geometry to a {@link LineString}.
     * 
     * <p>This works only for Polygon and Lines features. 
     * <p>From this moment on the internal geometry (as got by the {@link #getGeometry()})
     * will be the line type.
     * <p>To get the original geometry one can simply call {@link #resetGeometry()}. 
     *
     * @throws IllegalArgumentException in the case the geometry is a point.
     */
    public void convertToLine() throws IllegalArgumentException {
        GEOMETRYTYPE geometryType = GeometryUtilities.getGeometryType(getGeometry());
        switch( geometryType ) {
        case MULTIPOLYGON:
        case POLYGON:
            // convert to line
            Coordinate[] tmpCoords = geometry.getCoordinates();
            geometry = GeometryUtilities.gf().createLineString(tmpCoords);
            // reset prepared geometry
            preparedGeometry = null;
            break;
        case LINE:
        case MULTILINE:
            // do nothing, is already line
            break;
        default:
            throw new IllegalArgumentException("Points not supported");
        }
    }

    /**
     * Tries to convert the internal geometry to a {@link Point}.
     * 
     * <p>From this moment on the internal geometry (as got by the {@link #getGeometry()})
     * will be the point type.
     * <p>To get the original geometry one can simply call {@link #resetGeometry()}. 
     */
    public void convertToPoint() {
        GEOMETRYTYPE geometryType = GeometryUtilities.getGeometryType(getGeometry());
        switch( geometryType ) {
        case MULTIPOLYGON:
        case POLYGON:
        case LINE:
        case MULTILINE:
            // convert to line
            Coordinate[] tmpCoords = geometry.getCoordinates();
            geometry = GeometryUtilities.gf().createMultiPoint(tmpCoords);
            // reset prepared geometry
            preparedGeometry = null;
            break;
        default:
            break;
        }
    }

    /**
     * Resets the geometry, so that at the next call of {@link #getGeometry()} the original geometry is reread.
     */
    public void resetGeometry() {
        geometry = null;
        preparedGeometry = null;
    }

}