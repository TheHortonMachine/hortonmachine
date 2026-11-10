/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.utils.features;

import java.io.IOException;

import org.geotools.api.feature.FeatureVisitor;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.util.ProgressListener;
import org.geotools.data.DataUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.collection.DecoratingSimpleFeatureCollection;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.feature.collection.SortedSimpleFeatureCollection;
import org.geotools.geometry.jts.GeometryCoordinateSequenceTransformer;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.locationtech.jts.geom.Geometry;

/**
 * A feature collection reprojected on the fly, as {@link ReprojectingFeatureCollection}, that
 * can be read by more threads at the same time.
 *
 * <p>The {@link ReprojectingFeatureCollection} shares one geometry transformer among all its
 * iterators, so iterations running together in different threads, as the drawing of a layer
 * and a query of its features, corrupt each other's geometries. Here each iteration gets its
 * own reprojecting collection, and with it its own transformer.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ConcurrentReprojectingFeatureCollection extends DecoratingSimpleFeatureCollection {

    private final SimpleFeatureCollection source;
    private final CoordinateReferenceSystem targetCrs;
    private final MathTransform transform;

    /**
     * @param source the features to reproject.
     * @param targetCrs the CRS to reproject to.
     * @param transform the transform from the CRS of the source to the target one, or
     *          <code>null</code> to let the collection find a lenient one.
     */
    public ConcurrentReprojectingFeatureCollection( SimpleFeatureCollection source, CoordinateReferenceSystem targetCrs,
            MathTransform transform ) {
        super(newReprojecting(source, targetCrs, transform));
        this.source = source;
        this.targetCrs = targetCrs;
        this.transform = transform;
    }

    private static ReprojectingFeatureCollection newReprojecting( SimpleFeatureCollection source,
            CoordinateReferenceSystem targetCrs, MathTransform transform ) {
        ReprojectingFeatureCollection reprojected = new ReprojectingFeatureCollection(source, targetCrs);
        if (transform != null) {
            GeometryCoordinateSequenceTransformer transformer = new GeometryCoordinateSequenceTransformer();
            transformer.setMathTransform(transform);
            transformer.setCoordinateReferenceSystem(targetCrs);
            reprojected.setTransformer(transformer);
        }
        return reprojected;
    }

    @Override
    public SimpleFeatureIterator features() {
        return newReprojecting(source, targetCrs, transform).features();
    }

    @Override
    public ReferencedEnvelope getBounds() {
        // from the reprojected geometries, read with an iterator of its own
        ReferencedEnvelope bounds = new ReferencedEnvelope(targetCrs);
        try (SimpleFeatureIterator iterator = features()) {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                Object geometry = feature.getDefaultGeometry();
                if (geometry instanceof Geometry) {
                    bounds.expandToInclude(((Geometry) geometry).getEnvelopeInternal());
                }
            }
        }
        return bounds;
    }

    @Override
    public SimpleFeatureCollection subCollection( Filter filter ) {
        if (filter == Filter.INCLUDE) {
            return this;
        }
        // the filter is on the reprojected features
        return new FilteringSimpleFeatureCollection(this, filter);
    }

    @Override
    public SimpleFeatureCollection sort( SortBy order ) {
        return new SortedSimpleFeatureCollection(this, new SortBy[]{order});
    }

    @Override
    public void accepts( FeatureVisitor visitor, ProgressListener progress ) throws IOException {
        DataUtilities.visit(this, visitor, progress);
    }
}
