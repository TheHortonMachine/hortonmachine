package org.hortonmachine.gears.utils.features;

import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;

/**
 * A FeatureCollection wrapper that swaps X and Y of every geometry
 * in returned features (on-the-fly), similar in spirit to ReprojectingFeatureCollection.
 *
 * Schema (attributes and CRS metadata) is preserved as-is; only coordinate values are flipped.
 */
public class CoordinateSwappingFeatureCollection extends DecoratingSimpleFeatureCollection {

    public CoordinateSwappingFeatureCollection(SimpleFeatureCollection delegate) {
        super(delegate);
    }

    @Override
    public SimpleFeatureIterator features() {
        return new SwappingIterator(delegate.features(), delegate.getSchema());
    }

    /** Iterator that returns features with XY-swapped geometries. */
    private static class SwappingIterator extends DecoratingSimpleFeatureIterator {
        private final SimpleFeatureType schema;
        private final SimpleFeatureBuilder builder;

        SwappingIterator(SimpleFeatureIterator delegate, SimpleFeatureType schema) {
            super(delegate);
            this.schema = schema;
            this.builder = new SimpleFeatureBuilder(schema);
        }

        @Override
        public SimpleFeature next() {
            SimpleFeature in = super.next();

            // Copy attributes while swapping only the default geometry
            for (var ad : schema.getAttributeDescriptors()) {
                String name = ad.getLocalName();
                Object val = in.getAttribute(name);
                if (ad == schema.getGeometryDescriptor() && val instanceof Geometry g) {
                    val = swapXY(g);
                }
                builder.set(name, val);
            }
            SimpleFeature out = builder.buildFeature(null);
            // Reuse the same feature id to keep identity stable
//            SimpleFeature out = builder.buildFeature(in.getID());
//            builder.reset();
            return out;
        }
    }

    /* ---------------- Helpers ---------------- */

    /** Swap XY for all coordinates in a deep copy of the geometry. */
    public static Geometry swapXY(Geometry g) {
        if (g == null || g.isEmpty()) return g;
        Geometry copy = (Geometry) g.copy();
        copy.apply(SWAP_FILTER);
        copy.geometryChanged();
        return copy;
    }

    /** One reusable filter instance is fine: it doesn't keep state between calls. */
    private static final CoordinateSequenceFilter SWAP_FILTER = new CoordinateSequenceFilter() {
        @Override
        public void filter(CoordinateSequence seq, int i) {
            double x = seq.getX(i);
            double y = seq.getY(i);
            seq.setOrdinate(i, 0, y); // X <- Y
            seq.setOrdinate(i, 1, x); // Y <- X
            // Z/M (ordinates 2/3) intentionally untouched
        }
        @Override public boolean isDone() { return false; }
        @Override public boolean isGeometryChanged() { return true; }
    };
}
