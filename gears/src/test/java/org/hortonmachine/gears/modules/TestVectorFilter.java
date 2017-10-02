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
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.modules.v.vectorfilter.OmsVectorFilter;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.opengis.feature.simple.SimpleFeature;
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
}
