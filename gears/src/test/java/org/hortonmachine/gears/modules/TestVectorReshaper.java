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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.modules.v.vectorreshaper.OmsVectorReshaper;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.MultiPoint;
/**
 * Test for the {@link OmsVectorReshaper}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorReshaper extends HMTestCase {

    @SuppressWarnings("nls")
    public void testFeatureReshaper() throws Exception {

        SimpleFeatureCollection testFC = HMTestMaps.getTestFC();

        OmsVectorReshaper reshaper = new OmsVectorReshaper();
        reshaper.inVector = testFC;
        reshaper.pCql = "newcat=cat*2 \n newcat2=cat*4";
        reshaper.process();
        SimpleFeatureCollection outFC = reshaper.outVector;

        FeatureIterator<SimpleFeature> featureIterator = outFC.features();
        SimpleFeature feature = featureIterator.next();
        assertNotNull(feature);

        Integer attribute = (Integer) feature.getAttribute("cat");
        Double newAttribute = (Double) feature.getAttribute("newcat");
        Double newAttribute2 = (Double) feature.getAttribute("newcat2");
        assertEquals(attribute.intValue() * 2, newAttribute.intValue());
        assertEquals(attribute.intValue() * 4, newAttribute2.intValue());
        featureIterator.close();

    }

    public void testBuffer() throws Exception {
        String cql = "the_geom=buffer(the_geom, 20.0)";

        SimpleFeatureCollection testFC = HMTestMaps.getTestFC();
        OmsVectorReshaper reshaper = new OmsVectorReshaper();
        reshaper.inVector = testFC;
        reshaper.pCql = cql;
        reshaper.process();
        SimpleFeatureCollection outFC = reshaper.outVector;
        FeatureIterator<SimpleFeature> featureIterator = outFC.features();
        SimpleFeature feature = featureIterator.next();
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        String geometryType = geometry.getGeometryType();
        assertTrue(geometryType.toUpperCase().equals("POLYGON"));
        featureIterator.close();
    }

    public void testConvexHull() throws Exception {
        String cql = "the_geom=convexHull(the_geom)";

        GeometryFactory gf = GeometryUtilities.gf();
        MultiPoint multiPoint = gf.createMultiPoint(new Coordinate[]{//
                HMTestMaps.getWestNorth(), HMTestMaps.getEastSouth(),
                HMTestMaps.getEastNorth()});
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(HMTestMaps.getCrs());
        b.add("the_geom", MultiPoint.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Object[] values = new Object[]{multiPoint};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(null);
        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        newCollection.add(feature);

        OmsVectorReshaper reshaper = new OmsVectorReshaper();
        reshaper.inVector = newCollection;
        reshaper.pCql = cql;
        reshaper.process();
        SimpleFeatureCollection outFC = reshaper.outVector;
        FeatureIterator<SimpleFeature> featureIterator = outFC.features();
        SimpleFeature newFeature = featureIterator.next();
        Geometry geometry = (Geometry) newFeature.getDefaultGeometry();
        String geometryType = geometry.getGeometryType();
        assertTrue(geometryType.toUpperCase().equals("POLYGON"));
        featureIterator.close();
    }

    public void testCentroid() throws Exception {
        String cql = "the_geom=centroid(the_geom)";

        SimpleFeatureCollection testFC = HMTestMaps.getTestLeftFC();
        OmsVectorReshaper reshaper = new OmsVectorReshaper();
        reshaper.inVector = testFC;
        reshaper.pCql = cql;
        reshaper.process();
        SimpleFeatureCollection outFC = reshaper.outVector;
        FeatureIterator<SimpleFeature> featureIterator = outFC.features();
        SimpleFeature feature = featureIterator.next();
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        String geometryType = geometry.getGeometryType();
        assertTrue(geometryType.toUpperCase().equals("POINT"));
        featureIterator.close();
    }
}
