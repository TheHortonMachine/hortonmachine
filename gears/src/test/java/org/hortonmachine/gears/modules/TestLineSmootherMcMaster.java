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
public class TestLineSmootherMcMaster extends HMTestCase {

    public void testVectorReader() throws Exception {

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", LineString.class);
        b.add("id", Integer.class);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        SimpleFeatureType type = b.buildFeatureType();

        Geometry line = new WKTReader().read("LINESTRING (0 0, 1 1, 2 2, 3 3, 4 4, 5 3, 6 2, 7 1, 8 0)");
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Object[] values = new Object[]{line, 0};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(type.getTypeName() + ".0");
        newCollection.add(feature);

        OmsLineSmootherMcMaster smoother = new OmsLineSmootherMcMaster();
        smoother.inVector = newCollection;
        smoother.pLookahead = 3;
        smoother.pSlide = 0.9;
        smoother.pDensify = 0.9;
        smoother.process();
        SimpleFeatureCollection outFeatures = smoother.outVector;

        List<Geometry> geomList = FeatureUtilities.featureCollectionToGeometriesList(outFeatures, false, null);
        Geometry geometry = geomList.get(0);

        int newLength = geometry.getCoordinates().length;

        Geometry densifiedline = new WKTReader()
                .read("LINESTRING (0 0, 0.5 0.5, 1 1, 1.5 1.5, 2 2, 2.5 2.5, 3 3, 3.5 3.5, 4 4, 4.5 3.5, 5 3, 5.5 2.5, 6 2, 6.5 1.5, 7 1, 7.5 0.5, 8 0)");
        int expectedLength = densifiedline.getCoordinates().length;

        assertEquals(expectedLength, newLength);

    }

    /**
     * The smoothing of a symmetric line is symmetric, keeps the ends and lowers the peak.
     */
    public void testSymmetricSmoothing() throws Exception {
        OmsLineSmootherMcMaster smoother = new OmsLineSmootherMcMaster();
        smoother.inVector = lines("LINESTRING (0 0, 1 1, 2 2, 3 3, 4 4, 5 3, 6 2, 7 1, 8 0)");
        smoother.pLookahead = 5;
        smoother.pSlide = 1.0;
        smoother.pDensify = 0.25;
        smoother.process();

        Coordinate[] c = smoothedLines(smoother).get(0).getCoordinates();
        int n = c.length;
        // each side of 1.41 m densified in 6 parts
        assertEquals(8 * 6 + 1, n);
        assertTrue(c[0].equals2D(new Coordinate(0, 0)));
        assertTrue(c[n - 1].equals2D(new Coordinate(8, 0)));
        double maxY = 0;
        for( int i = 0; i < n; i++ ) {
            assertEquals(8.0, c[i].x + c[n - 1 - i].x, 1E-9);
            assertEquals(c[i].y, c[n - 1 - i].y, 1E-9);
            maxY = Math.max(maxY, c[i].y);
        }
        // the average of the 5 vertices around the peak, 1/6 m lower one from the other
        assertEquals(4.0 - 2 * (1 / 6.0 + 2 / 6.0) / 5.0, maxY, 1E-9);
    }

    /**
     * On a curved line all the vertices but the first and the last are averaged, at both ends.
     */
    public void testCurvedEnds() throws Exception {
        OmsLineSmootherMcMaster smoother = new OmsLineSmootherMcMaster();
        // y = (x - 5)^2
        smoother.inVector = lines("LINESTRING (0 25, 1 16, 2 9, 3 4, 4 1, 5 0, 6 1, 7 4, 8 9, 9 16, 10 25)");
        smoother.pLookahead = 3;
        smoother.pSlide = 1.0;
        smoother.process();

        Coordinate[] c = smoothedLines(smoother).get(0).getCoordinates();
        assertEquals(11, c.length);
        assertEquals(25.0, c[0].y, 1E-9);
        assertEquals((25 + 16 + 9) / 3.0, c[1].y, 1E-9);
        assertEquals((25 + 16 + 9) / 3.0, c[9].y, 1E-9);
        assertEquals(25.0, c[10].y, 1E-9);
    }

    /**
     * With a slide of 0 the vertices stay where they are.
     */
    public void testNoSlide() throws Exception {
        OmsLineSmootherMcMaster smoother = new OmsLineSmootherMcMaster();
        smoother.inVector = lines("LINESTRING (0 0, 1 1, 2 2, 3 3, 4 4, 5 3, 6 2, 7 1, 8 0)");
        smoother.pLookahead = 5;
        smoother.pSlide = 0.0;
        smoother.pDensify = 0.25;
        smoother.process();

        Geometry smoothed = smoothedLines(smoother).get(0);
        Geometry original = new WKTReader().read("LINESTRING (0 0, 4 4, 8 0)");
        assertTrue(smoothed.getCoordinates().length > 3);
        assertEquals(0.0, smoothed.difference(original.buffer(1E-9)).getLength(), 1E-9);
    }

    /**
     * The lines not longer than the limit are not smoothed. Among them, the closed ones and the ones not
     * connected to other lines are removed.
     */
    public void testLimit() throws Exception {
        OmsLineSmootherMcMaster smoother = new OmsLineSmootherMcMaster();
        smoother.inVector = lines(//
                "LINESTRING (0 0, 10 10, 20 20, 30 30, 40 40, 50 30, 60 20, 70 10, 80 0)", // long
                "LINESTRING (80 0, 82 2, 84 0)", // short, connected to the long one
                "LINESTRING (200 200, 202 202, 204 200)", // short, alone
                "LINESTRING (300 300, 302 300, 302 302, 300 300)" // short, closed
        );
        smoother.pLookahead = 3;
        smoother.pLimit = 10;
        smoother.process();

        List<Geometry> smoothed = smoothedLines(smoother);
        assertEquals(2, smoothed.size());
        // the short connected line is kept as it is
        Geometry connected = new WKTReader().read("LINESTRING (80 0, 82 2, 84 0)");
        boolean hasConnected = smoothed.stream().anyMatch(g -> g.getGeometryN(0).equalsExact(connected));
        assertTrue(hasConnected);
    }

    private static SimpleFeatureCollection lines( String... wkts ) throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", LineString.class);
        b.add("id", Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        for( int i = 0; i < wkts.length; i++ ) {
            builder.addAll(new Object[]{new WKTReader().read(wkts[i]), i});
            fc.add(builder.buildFeature(type.getTypeName() + "." + i));
        }
        return fc;
    }

    private static List<Geometry> smoothedLines( OmsLineSmootherMcMaster smoother ) {
        return FeatureUtilities.featureCollectionToGeometriesList(smoother.outVector, false, null);
    }
}
