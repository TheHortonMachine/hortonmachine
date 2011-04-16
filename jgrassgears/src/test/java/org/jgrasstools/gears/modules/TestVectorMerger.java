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

import java.util.Arrays;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.modules.v.vectormerger.VectorMerger;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
/**
 * Test for the {@link VectorMerger}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorMerger extends HMTestCase {

    @SuppressWarnings("nls")
    public void testVectorMerger() throws Exception {
        SimpleFeatureCollection testFC1 = HMTestMaps.testFC;
        SimpleFeatureCollection testFC2 = HMTestMaps.testFC;

        VectorMerger filter = new VectorMerger();
        filter.pm = pm;
        filter.inVectors = Arrays.asList(testFC1, testFC2);
        filter.process();
        SimpleFeatureCollection outFC = filter.outVector;

        assertTrue(outFC.size() == 6);
    }
}
