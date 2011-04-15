/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.modules.v.vectorreshaper.VectorReshaper;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.opengis.feature.simple.SimpleFeature;
/**
 * Test for the {@link VectorReshaper}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorReshaper extends HMTestCase {

    @SuppressWarnings("nls")
    public void testFeatureReshaper() throws Exception {

        SimpleFeatureCollection testFC = HMTestMaps.testFC;

        VectorReshaper reshaper = new VectorReshaper();
        reshaper.inFeatures = testFC;
        reshaper.pCql = "newcat=cat*2";
        reshaper.process();
        SimpleFeatureCollection outFC = reshaper.outFeatures;

        FeatureIterator<SimpleFeature> featureIterator = outFC.features();
        SimpleFeature feature = featureIterator.next();
        assertNotNull(feature);

        Integer attribute = (Integer) feature.getAttribute("cat");
        Double newAttribute = (Double) feature.getAttribute("newcat");
        assertEquals(attribute.intValue() * 2, newAttribute.intValue());
        featureIterator.close();

    }
}
