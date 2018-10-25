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
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

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
}
