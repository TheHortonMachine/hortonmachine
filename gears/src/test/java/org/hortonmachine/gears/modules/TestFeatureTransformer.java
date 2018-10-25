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
import org.hortonmachine.gears.modules.v.vectortransformer.OmsVectorTransformer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.features.FeatureMate;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
/**
 * Test for the transformer module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestFeatureTransformer extends HMTestCase {

    @SuppressWarnings("nls")
    public void testFeatureTransformer() throws Exception {

        SimpleFeatureCollection testFC = HMTestMaps.getTestFC();

        OmsVectorTransformer transformer = new OmsVectorTransformer();
        transformer.inVector = testFC;
        transformer.pTransX = 10.0;
        transformer.pTransY = 10.0;
        transformer.process();
        SimpleFeatureCollection outFC = transformer.outVector;

        List<FeatureMate> inMates = FeatureUtilities.featureCollectionToMatesList(testFC);
        List<FeatureMate> outMates = FeatureUtilities.featureCollectionToMatesList(outFC);
        
        
        Geometry inG = null;
        for( FeatureMate featureMate : inMates ) {
            Integer cat = featureMate.getAttribute("cat", Integer.class);
            if (cat == 1) {
                inG = featureMate.getGeometry();
            }
        }

        Geometry outG = null;
        for( FeatureMate featureMate : outMates ) {
            Integer cat = featureMate.getAttribute("cat", Integer.class);
            if (cat == 1) {
                outG = featureMate.getGeometry();
            }
        }
        
        Coordinate inCoord = inG.getCoordinate();
        Coordinate outCoord = outG.getCoordinate();

        double distance = inCoord.distance(outCoord);
        double checkDistance = NumericsUtilities.pythagoras(10, 10);
        assertEquals(distance, checkDistance, 0.001);

    }
}
