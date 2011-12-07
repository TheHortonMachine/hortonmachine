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

import java.util.ArrayList;
import java.util.List;

import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities.GEOMETRYTYPE;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

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

    public SimpleFeature getFeature() {
        return feature;
    }
    
    /**
     * Apply a buffer to the geometry and use that as new {@link Geometry}.
     * 
     * @param buffer the buffer to apply.
     */
    public void useBuffer(double buffer){
        Geometry tmpGeometry = getGeometry();
        geometry = tmpGeometry.buffer(buffer);
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
     * Getter for the list of attribute names.
     * 
     * @return the list of attribute names.
     */
    public List<String> getAttributesNames() {
        SimpleFeatureType featureType = feature.getFeatureType();
        List<AttributeDescriptor> attributeDescriptors = featureType.getAttributeDescriptors();

        List<String> attributeNames = new ArrayList<String>();
        for( AttributeDescriptor attributeDescriptor : attributeDescriptors ) {
            String name = attributeDescriptor.getLocalName();
            attributeNames.add(name);
        }
        return attributeNames;
    }

    /**
     * Gets an attribute from the feature table, adapting to the supplied class.
     * 
     * @param attrName the attribute name to pick.
     * @param adaptee the class to adapt to.
     * @return the adapted value if possible.
     */
    public <T> T getAttribute( String attrName, Class<T> adaptee ) {
        if (attrName == null) {
            return null;
        }
        if (adaptee == null) {
            adaptee = (Class<T>) String.class;
        }

        Object attribute = feature.getAttribute(attrName);
        if (attribute == null) {
            return null;
        }
        if (attribute instanceof Number) {
            Number num = (Number) attribute;
            if (adaptee.isAssignableFrom(Double.class)) {
                return adaptee.cast(num.doubleValue());
            } else if (adaptee.isAssignableFrom(Float.class)) {
                return adaptee.cast(num.floatValue());
            } else if (adaptee.isAssignableFrom(Integer.class)) {
                return adaptee.cast(num.intValue());
            } else if (adaptee.isAssignableFrom(String.class)) {
                return adaptee.cast(num.toString());
            } else {
                throw new IllegalArgumentException();
            }
        } else if (attribute instanceof String) {
            if (adaptee.isAssignableFrom(Double.class)) {
                try {
                    Double parsed = Double.parseDouble((String) attribute);
                    return adaptee.cast(parsed);
                } catch (Exception e) {
                    return null;
                }
            } else if (adaptee.isAssignableFrom(Float.class)) {
                try {
                    Float parsed = Float.parseFloat((String) attribute);
                    return adaptee.cast(parsed);
                } catch (Exception e) {
                    return null;
                }
            } else if (adaptee.isAssignableFrom(Integer.class)) {
                try {
                    Integer parsed = Integer.parseInt((String) attribute);
                    return adaptee.cast(parsed);
                } catch (Exception e) {
                    return null;
                }
            } else if (adaptee.isAssignableFrom(String.class)) {
                return adaptee.cast(attribute);
            } else {
                throw new IllegalArgumentException();
            }
        } else if (attribute instanceof Geometry) {
            return null;
        } else {
            throw new IllegalArgumentException("Can't adapt attribute of type: " + attribute.getClass().getCanonicalName());
        }
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
     * Check for cover.
     * 
     * @param geometry the geometry to check against.
     * @param usePrepared use prepared geometry.
     * @return true if the current geometries covers the supplied one.
     */
    public boolean covers( Geometry geometry, boolean usePrepared ) {
        if (!getEnvelope().covers(geometry.getEnvelopeInternal())) {
            return false;
        }
        if (usePrepared) {
            if (preparedGeometry == null) {
                preparedGeometry = PreparedGeometryFactory.prepare(getGeometry());
            }
            return preparedGeometry.covers(geometry);
        } else {
            return getGeometry().covers(geometry);
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