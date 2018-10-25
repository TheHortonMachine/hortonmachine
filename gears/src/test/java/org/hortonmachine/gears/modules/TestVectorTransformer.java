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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

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

}
