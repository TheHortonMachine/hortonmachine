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
import java.util.Locale;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
/**
 * Test {@link OmsVectorFieldRounder}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorFieldRounder extends HMTestCase {

    public void testVectorFieldRounder() throws Exception {

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        b.add("area", Double.class);

        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Point point = GeometryUtilities.gf().createPoint(new Coordinate(0, 0));
        Object[] values = new Object[]{point, 123.456789};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(type.getTypeName() + ".0");
        newCollection.add(feature);

        OmsVectorFieldRounder rounder = new OmsVectorFieldRounder();
        rounder.inVector = newCollection;
        rounder.fRound = "area";
        rounder.pPattern = ".##";
        rounder.process();
        SimpleFeatureCollection outFeatures = rounder.outVector;

        SimpleFeature f = FeatureUtilities.featureCollectionToList(outFeatures).get(0);
        String areaStr = f.getAttribute("area").toString();

        assertTrue(areaStr.equals("123.46"));
    }

    /**
     * Half up, with a dot pattern also where the comma is the decimal separator, with the empty
     * values left as they are.
     */
    public void testRoundingInItalian() throws Exception {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.ITALY);
        try {
            SimpleFeatureCollection rounded = round(new Double[]{2.25, 1234.56789, null, -0.04}, "0.0");
            List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(rounded);
            assertEquals(2.3, (Double) features.get(0).getAttribute("value"), 1E-12);
            assertEquals(1234.6, (Double) features.get(1).getAttribute("value"), 1E-12);
            assertNull(features.get(2).getAttribute("value"));
            assertEquals(-0.0, (Double) features.get(3).getAttribute("value"), 1E-12);

            rounded = round(new Double[]{2.5, 3.5}, "#");
            features = FeatureUtilities.featureCollectionToList(rounded);
            assertEquals(3.0, (Double) features.get(0).getAttribute("value"), 1E-12);
            assertEquals(4.0, (Double) features.get(1).getAttribute("value"), 1E-12);
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }

    public void testMissingField() throws Exception {
        OmsVectorFieldRounder rounder = new OmsVectorFieldRounder();
        rounder.inVector = values(new Double[]{1.0});
        rounder.fRound = "area";
        rounder.pPattern = "0.0";
        try {
            rounder.process();
            fail("A missing field must be reported.");
        } catch (ModelsIllegalargumentException e) {
            assertTrue(e.getMessage().contains("area"));
        }
    }

    private SimpleFeatureCollection round( Double[] values, String pattern ) throws Exception {
        OmsVectorFieldRounder rounder = new OmsVectorFieldRounder();
        rounder.inVector = values(values);
        rounder.fRound = "VALUE";
        rounder.pPattern = pattern;
        rounder.process();
        return rounder.outVector;
    }

    private static SimpleFeatureCollection values( Double[] values ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("test");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);
        b.add("value", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        for( int i = 0; i < values.length; i++ ) {
            Point point = GeometryUtilities.gf().createPoint(new Coordinate(i, 0));
            builder.addAll(new Object[]{point, values[i]});
            // the collection is sorted by id: keep the order of the values
            fc.add(builder.buildFeature("test." + i));
        }
        return fc;
    }
}
