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
package org.hortonmachine.gears.utils;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gears.utils.features.ConcurrentReprojectingFeatureCollection;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.util.GeometricShapeFactory;

/**
 * Test {@link ConcurrentReprojectingFeatureCollection}: iterations running together in more
 * threads give the same geometries as a single one.
 */
public class TestConcurrentReprojectingFeatureCollection {

    private static final int FEATURES = 200;
    private static final int THREADS = 4;
    private static final int ROUNDS = 30;

    private static SimpleFeatureCollection polygonsUtm() throws Exception {
        CoordinateReferenceSystem utm = HMCrsRegistry.INSTANCE.getCrs("EPSG:32632");
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("circles");
        typeBuilder.setCRS(utm);
        typeBuilder.add("the_geom", Polygon.class);
        typeBuilder.add("index", Integer.class);
        SimpleFeatureType type = typeBuilder.buildFeatureType();

        ListFeatureCollection collection = new ListFeatureCollection(type);
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        GeometricShapeFactory shapes = new GeometricShapeFactory(new GeometryFactory());
        shapes.setNumPoints(64);
        shapes.setSize(500);
        for( int i = 0; i < FEATURES; i++ ) {
            shapes.setCentre(new Coordinate(650000 + (i % 20) * 1000, 5150000 + (i / 20) * 1000));
            builder.add(shapes.createCircle());
            builder.add(i);
            collection.add(builder.buildFeature("circles." + i));
        }
        return collection;
    }

    private static Map<String, Geometry> read( SimpleFeatureCollection collection ) {
        Map<String, Geometry> geometries = new HashMap<>();
        try (SimpleFeatureIterator iterator = collection.features()) {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                geometries.put(feature.getID(), (Geometry) feature.getDefaultGeometry());
            }
        }
        return geometries;
    }

    @Test
    public void testConcurrentIterations() throws Exception {
        SimpleFeatureCollection utm = polygonsUtm();
        CoordinateReferenceSystem utmCrs = utm.getSchema().getCoordinateReferenceSystem();
        CoordinateReferenceSystem lonLat = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);
        SimpleFeatureCollection reprojected = new HMCrsTransformer(utmCrs, lonLat).transform(utm);
        assertTrue(reprojected instanceof ConcurrentReprojectingFeatureCollection);

        Map<String, Geometry> reference = read(reprojected);
        assertEquals(FEATURES, reference.size());

        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for( int t = 0; t < THREADS; t++ ) {
                results.add(executor.submit(() -> {
                    int differences = 0;
                    for( int round = 0; round < ROUNDS; round++ ) {
                        Map<String, Geometry> read = read(reprojected);
                        for( Map.Entry<String, Geometry> entry : reference.entrySet() ) {
                            if (!entry.getValue().equalsExact(read.get(entry.getKey()), 1E-12)) {
                                differences++;
                            }
                        }
                    }
                    return differences;
                }));
            }
            for( Future<Integer> result : results ) {
                assertEquals("geometries differ from the single thread reading", 0, (int) result.get());
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void testBoundsAndFilter() throws Exception {
        SimpleFeatureCollection utm = polygonsUtm();
        CoordinateReferenceSystem lonLat = HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true);
        SimpleFeatureCollection reprojected = new ConcurrentReprojectingFeatureCollection(utm, lonLat, null);

        ReferencedEnvelope bounds = reprojected.getBounds();
        // eastings 650-670 km of UTM 32N (central meridian 9E) at 46.5N: about 11 degrees east
        assertTrue(bounds.getMinX() > 10.5 && bounds.getMaxX() < 11.5);
        assertTrue(bounds.getMinY() > 46 && bounds.getMaxY() < 47);
        assertEquals(FEATURES, reprojected.size());

        SimpleFeatureCollection filtered = reprojected
                .subCollection(org.geotools.filter.text.ecql.ECQL.toFilter("index < 10"));
        assertEquals(10, read(filtered).size());
    }
}
