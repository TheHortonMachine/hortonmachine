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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.modules.v.vectorfieldrounder.OmsVectorFieldRounder;
import org.hortonmachine.gears.modules.v.vectortablejoiner.OmsVectorTableJoiner;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Point;
/**
 * Test {@link OmsVectorFieldRounder}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorTableJoiner extends HMTestCase {

    public void testVectorTableJoiner() throws Exception {

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

        HashMap<String, List<Object>> tabledata = new HashMap<String, List<Object>>();
        List<Object> id = Arrays.asList(new Object[]{1});
        tabledata.put("id", id);
        List<Object> area = Arrays.asList(new Object[]{123.45});
        tabledata.put("area", area);
        List<Object> area2 = Arrays.asList(new Object[]{67.89});
        tabledata.put("area2", area2);

        OmsVectorTableJoiner joiner = new OmsVectorTableJoiner();
        joiner.inVector = newCollection;
        joiner.tabledata = tabledata;
        joiner.fCommon = "id";
        joiner.pFields = "area";
        joiner.process();
        SimpleFeatureCollection outFeatures = joiner.outVector;

        SimpleFeature f = FeatureUtilities.featureCollectionToList(outFeatures).get(0);
        String areaStr = f.getAttribute("area").toString();

        assertTrue(areaStr.equals("123.45"));
    }
}
