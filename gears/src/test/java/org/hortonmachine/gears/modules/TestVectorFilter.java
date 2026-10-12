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

import java.util.HashSet;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;
/**
 * Test for the {@link OmsVectorFilter}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorFilter extends HMTestCase {

    @SuppressWarnings("nls")
    public void testVectorFilter() throws Exception {
        SimpleFeatureCollection testFC = HMTestMaps.getTestFC();
        OmsVectorFilter filter = new OmsVectorFilter();
        filter.inVector = testFC;
        filter.pCql = "cat > 2";
        filter.process();
        SimpleFeatureCollection outFC = filter.outVector;

        assertTrue(outFC.size() == 1);

        FeatureIterator<SimpleFeature> featureIterator = outFC.features();
        SimpleFeature feature = featureIterator.next();
        assertNotNull(feature);

        Integer attribute = (Integer) feature.getAttribute("cat");
        assertEquals(3, attribute.intValue());
        featureIterator.close();

    }

    /**
     * The examples of the documentation, on three squares of 100 m side.
     */
    public void testDocumentedFilters() throws Exception {
        assertEquals(Set.of(2), filter("area > 10000 AND avgelev < 1800"));
        assertEquals(Set.of(1), filter("name = 'Rio Torto'"));
        assertEquals(Set.of(1, 3), filter("name LIKE 'Rio%'"));
        assertEquals(Set.of(1, 2), filter("netnum IN (1, 2)"));
        assertEquals(Set.of(1, 2), filter("avgelev BETWEEN 1500 AND 2000"));
        assertEquals(Set.of(2), filter("name IS NULL"));
        assertEquals(Set.of(3), filter("maxelev - minelev > 300"));
        assertEquals(Set.of(1), filter("INTERSECTS(the_geom, POINT(1638050 5112050))"));
        assertEquals(Set.of(1, 2), filter("BBOX(the_geom, 1638000, 5112000, 1638250, 5112100)"));
        assertEquals(Set.of(1, 2), filter("DWITHIN(the_geom, POINT(1638250 5112050), 160, meters)"));
    }

    private static Set<Integer> filter( String cql ) throws Exception {
        OmsVectorFilter filter = new OmsVectorFilter();
        filter.inVector = squares();
        filter.pCql = cql;
        filter.process();
        Set<Integer> netnums = new HashSet<>();
        for( SimpleFeature f : FeatureUtilities.featureCollectionToList(filter.outVector) ) {
            netnums.add((Integer) f.getAttribute("netnum"));
        }
        return netnums;
    }

    /**
     * @return three squares in a row, from 1638000, 5112000, with 100 m side but the second, of 150 m.
     */
    private static SimpleFeatureCollection squares() throws Exception {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("squares");
        b.setCRS(HMCrsRegistry.INSTANCE.getCrs("EPSG:3003"));
        b.add("the_geom", Polygon.class);
        b.add("netnum", Integer.class);
        b.add("name", String.class);
        b.add("area", Double.class);
        b.add("avgelev", Double.class);
        b.add("maxelev", Double.class);
        b.add("minelev", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        WKTReader reader = new WKTReader();
        fc.add(builder.buildFeature(null, new Object[]{
                reader.read("POLYGON ((1638000 5112000, 1638100 5112000, 1638100 5112100, 1638000 5112100, 1638000 5112000))"),
                1, "Rio Torto", 10000.0, 1900.0, 2000.0, 1800.0}));
        fc.add(builder.buildFeature(null, new Object[]{
                reader.read("POLYGON ((1638200 5112000, 1638350 5112000, 1638350 5112150, 1638200 5112150, 1638200 5112000))"),
                2, null, 22500.0, 1700.0, 1750.0, 1650.0}));
        fc.add(builder.buildFeature(null, new Object[]{
                reader.read("POLYGON ((1638500 5112000, 1638600 5112000, 1638600 5112100, 1638500 5112100, 1638500 5112000))"),
                3, "Rio Bianco", 10000.0, 2300.0, 2500.0, 2100.0}));
        return fc;
    }
}
