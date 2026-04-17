package org.hortonmachine.gears.utils.filter;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * Utility methods to create GeoTools filters in a concise way.
 * 
 * <p>Supported filter creation:
 * <ul>
 *   <li>from CQL/ECQL strings</li>
 *   <li>BBOX from geometry or envelope</li>
 *   <li>INTERSECTS from geometry</li>
 * </ul>
 * 
 * <p>The geometry attribute can either be supplied explicitly or derived from
 * a feature collection.
 * 
 * @author Andrea Antonello
 */
public final class HMFilter {

    private static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    private HMFilter() {
    }

    public static Filter fromCql( String cql ) {
        if (cql == null || cql.isBlank()) {
            throw new IllegalArgumentException("The CQL string must not be null or blank.");
        }
        try {
            return ECQL.toFilter(cql);
        } catch (CQLException e) {
            throw new IllegalArgumentException("Unable to parse CQL/ECQL filter: " + cql, e);
        }
    }

    public static Filter bbox( String geomName, Geometry geometry ) {
        checkGeometryName(geomName);
        checkGeometry(geometry);
        return bbox(geomName, geometry.getEnvelopeInternal());
    }

    public static Filter bbox( String geomName, Envelope envelope ) {
        checkGeometryName(geomName);
        checkEnvelope(envelope);
        return FF.bbox(geomName, envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY(), null);
    }

    public static Filter bbox( SimpleFeatureCollection collection, Geometry geometry ) {
        checkCollection(collection);
        return bbox(geometryName(collection), geometry);
    }

    public static Filter bbox( SimpleFeatureCollection collection, Envelope envelope ) {
        checkCollection(collection);
        return bbox(geometryName(collection), envelope);
    }

    public static Filter intersects( String geomName, Geometry geometry ) {
        checkGeometryName(geomName);
        checkGeometry(geometry);
        return FF.intersects(FF.property(geomName), FF.literal(geometry));
    }

    public static Filter intersects( SimpleFeatureCollection collection, Geometry geometry ) {
        checkCollection(collection);
        return intersects(geometryName(collection), geometry);
    }

    public static String geometryName( SimpleFeatureCollection collection ) {
        checkCollection(collection);
        var geometryDescriptor = collection.getSchema().getGeometryDescriptor();
        if (geometryDescriptor == null) {
            throw new IllegalArgumentException("The provided feature collection has no default geometry descriptor.");
        }
        return geometryDescriptor.getLocalName();
    }

    private static void checkCollection( SimpleFeatureCollection collection ) {
        if (collection == null) {
            throw new IllegalArgumentException("The feature collection must not be null.");
        }
    }

    private static void checkGeometryName( String geomName ) {
        if (geomName == null || geomName.isBlank()) {
            throw new IllegalArgumentException("The geometry attribute name must not be null or blank.");
        }
    }

    private static void checkGeometry( Geometry geometry ) {
        if (geometry == null) {
            throw new IllegalArgumentException("The geometry must not be null.");
        }
        if (geometry.isEmpty()) {
            throw new IllegalArgumentException("The geometry must not be empty.");
        }
    }

    private static void checkEnvelope( Envelope envelope ) {
        if (envelope == null) {
            throw new IllegalArgumentException("The envelope must not be null.");
        }
        if (envelope.isNull()) {
            throw new IllegalArgumentException("The envelope must not be null/empty.");
        }
    }
}