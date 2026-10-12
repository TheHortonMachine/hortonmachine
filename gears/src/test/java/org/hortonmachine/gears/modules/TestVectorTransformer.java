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

import java.util.ArrayList;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
/**
 * Test {@link OmsVectorTransformer}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorTransformer extends HMTestCase {

    public void testVectorTransformer() throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        b.add("id", Integer.class);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Point point = GeometryUtilities.gf().createPoint(new Coordinate(0, 0));
        Object[] values = new Object[]{point, 1};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(type.getTypeName() + ".0");
        newCollection.add(feature);

        OmsVectorTransformer transformer = new OmsVectorTransformer();
        transformer.inVector = newCollection;
        transformer.pTransX = 1.0;
        transformer.pTransY = -1.0;
        transformer.process();
        SimpleFeatureCollection outFeatures = transformer.outVector;

        Geometry g = FeatureUtilities.featureCollectionToGeometriesList(outFeatures, false, null).get(0);
        Coordinate coord = g.getCoordinate();

        assertEquals(coord.x, 1.0, 0.00001);
        assertEquals(coord.y, -1.0, 0.00001);
    }

    /**
     * Without center, clockwise around the center of the bounds of the vector, then the translation.
     */
    public void testRotationAroundBoundsCenter() throws Exception {
        // around 5,0: the west point goes north, the east one south
        List<Coordinate> c = rotate(null, null);
        assertEquals(6.0, c.get(0).x, 1E-9);
        assertEquals(4.0, c.get(0).y, 1E-9);
        assertEquals(6.0, c.get(1).x, 1E-9);
        assertEquals(-6.0, c.get(1).y, 1E-9);
    }

    /**
     * With a center, the rotation is around it.
     */
    public void testRotationAroundCenter() throws Exception {
        // around 0,0: the first point stays, the second goes south
        List<Coordinate> c = rotate(0.0, 0.0);
        assertEquals(1.0, c.get(0).x, 1E-9);
        assertEquals(-1.0, c.get(0).y, 1E-9);
        assertEquals(1.0, c.get(1).x, 1E-9);
        assertEquals(-11.0, c.get(1).y, 1E-9);
    }

    /**
     * @return the points 0,0 and 10,0 rotated by 90 degrees and moved by 1,-1, in their order.
     */
    private List<Coordinate> rotate( Double centerX, Double centerY ) throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        b.add("id", Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        builder.addAll(new Object[]{GeometryUtilities.gf().createPoint(new Coordinate(0, 0)), 1});
        fc.add(builder.buildFeature("test.1"));
        builder.addAll(new Object[]{GeometryUtilities.gf().createPoint(new Coordinate(10, 0)), 2});
        fc.add(builder.buildFeature("test.2"));

        OmsVectorTransformer transformer = new OmsVectorTransformer();
        transformer.inVector = fc;
        transformer.pRotate = 90.0;
        transformer.pCenterX = centerX;
        transformer.pCenterY = centerY;
        transformer.pTransX = 1.0;
        transformer.pTransY = -1.0;
        transformer.process();

        List<Coordinate> coordinates = new ArrayList<>();
        for( SimpleFeature f : FeatureUtilities.featureCollectionToList(transformer.outVector) ) {
            coordinates.add(((Geometry) f.getDefaultGeometry()).getCoordinate());
        }
        return coordinates;
    }

}
