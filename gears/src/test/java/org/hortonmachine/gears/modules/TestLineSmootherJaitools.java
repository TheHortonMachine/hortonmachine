/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gears.modules;

import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherJaitools;
import org.hortonmachine.gears.modules.v.smoothing.OmsLineSmootherMcMaster;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.io.WKTReader;
/**
 * Test {@link OmsLineSmootherMcMaster}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestLineSmootherJaitools extends HMTestCase {

    public void testVectorReader() throws Exception {

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", LineString.class);
        b.add("id", Integer.class);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        SimpleFeatureType type = b.buildFeatureType();

        Geometry line = new WKTReader().read("LINESTRING (0 0, 1 1, 2 0)");//2 2, 3 3, 4 4, 5 3, 6 2, 7 1, 8 0)");
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Object[] values = new Object[]{line, 0};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(type.getTypeName() + ".0");
        newCollection.add(feature);

        OmsLineSmootherJaitools smoother = new OmsLineSmootherJaitools();
        smoother.inVector = newCollection;
        smoother.pAlpha = 1;
        smoother.process();
        SimpleFeatureCollection outFeatures = smoother.outVector;

        List<Geometry> geomList = FeatureUtilities.featureCollectionToGeometriesList(outFeatures, false, null);
        Geometry geometry = geomList.get(0);
        
        int newLength = geometry.getCoordinates().length;

        Geometry densifiedline = new WKTReader()
                .read("LINESTRING (0 0, 0.0342935528120713 0.0342935528120713, 0.1262002743484225 0.1262002743484225, 0.2592592592592592 0.2592592592592592, 0.4170096021947873 0.4170096021947873, 0.5829903978052127 0.5829903978052127, 0.7407407407407407 0.7407407407407407, 0.8737997256515775 0.8737997256515775, 0.9657064471879286 0.9657064471879286, 1 1, 1.0342935528120714 0.9657064471879288, 1.1262002743484225 0.8737997256515775, 1.2592592592592593 0.7407407407407408, 1.4170096021947873 0.5829903978052127, 1.5829903978052127 0.4170096021947874, 1.7407407407407407 0.2592592592592593, 1.8737997256515775 0.1262002743484225, 1.9657064471879289 0.0342935528120714, 2 0)");
        int expectedLength = densifiedline.getCoordinates().length;

        assertEquals(expectedLength, newLength);

    }

    /**
     * The curve passes through all the vertices, with 10 vertices for each segment: with alpha 1 it
     * stays on the segments, with alpha 0 it is rounded.
     */
    public void testAlpha() throws Exception {
        Geometry original = new WKTReader().read("LINESTRING (0 0, 1 1, 2 0, 3 1)");

        Geometry tight = smooth(original, 1.0);
        Geometry loose = smooth(original, 0.0);
        for( Geometry smoothed : new Geometry[]{tight, loose} ) {
            assertEquals(3 * 9 + 1, smoothed.getNumPoints());
            for( Coordinate vertex : original.getCoordinates() ) {
                assertEquals(0.0, smoothed.distance(original.getFactory().createPoint(vertex)), 1E-9);
            }
        }
        assertEquals(0.0, tight.difference(original.buffer(1E-9)).getLength(), 1E-9);
        assertTrue(original.distance(loose) < 1E-9);
        assertTrue(loose.difference(original.buffer(0.05)).getLength() > 0.5);
    }

    private Geometry smooth( Geometry line, double alpha ) throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", LineString.class);
        SimpleFeatureType type = b.buildFeatureType();
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        builder.addAll(new Object[]{line});
        fc.add(builder.buildFeature(null));

        OmsLineSmootherJaitools smoother = new OmsLineSmootherJaitools();
        smoother.inVector = fc;
        smoother.pAlpha = alpha;
        smoother.process();
        return FeatureUtilities.featureCollectionToGeometriesList(smoother.outVector, false, null).get(0);
    }
}
