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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.modules.v.vectormerger.OmsVectorMerger;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
/**
 * Test for the {@link OmsVectorMerger}
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorMerger extends HMTestCase {

    @SuppressWarnings("nls")
    public void testVectorMerger() throws Exception {
        SimpleFeatureCollection testFC1 = HMTestMaps.getTestFC();
        SimpleFeatureCollection testFC2 = HMTestMaps.getTestFC();

        OmsVectorMerger filter = new OmsVectorMerger();
        filter.pm = pm;
        filter.inVectors = Arrays.asList(testFC1, testFC2);
        filter.process();
        SimpleFeatureCollection outFC = filter.outVector;

        assertTrue(outFC.size() == 6);
    }
}
