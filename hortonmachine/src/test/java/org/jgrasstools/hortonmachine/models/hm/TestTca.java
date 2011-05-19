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
package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.tca.Tca;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link Tca} module.
 * 
 * @author Giuseppe Formetta
 */
public class TestTca extends HMTestCase {

    public void testTca() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        Tca tca = new Tca();
        tca.inFlow = flowCoverage;
        tca.pm = pm;
        tca.process();
        GridCoverage2D tcaCoverage = tca.outTca;
        
        checkMatrixEqual(tcaCoverage.getRenderedImage(), HMTestMaps.tcaData);
    }
    
//    GridCoverage2D flowCoverage = RasterReader
//    .readCoverage("/home/moovida/bm_valsole/utm_valsole/cnr_pit/cell/netflow_drain");
//
//GridCoverage2D tcaCoverage = null;
//try {
//Tca tca = new Tca();
//tca.inFlow = flowCoverage;
//tca.pm = pm;
//tca.process();
//tcaCoverage = tca.outTca;
// SimpleFeatureCollection outLoop = tca.outLoop;
// RasterWriter.writeRaster("/home/moovida/bm_valsole/utm_valsole/cnr_pit/cell/netflow_tca2", tcaCoverage);
// VectorWriter.writeVector("/home/moovida/bm_valsole/loop.shp", outLoop);
//} catch (Exception e) {
//e.printStackTrace();
//}

}