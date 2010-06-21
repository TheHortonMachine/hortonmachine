/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.geomorphology.tca.Tca;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link Tca} module.
 * 
 * @author Giuseppe Formetta ()
 */
public class TestTca extends HMTestCase {

    public void testTca() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

       
        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData,
                envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        Tca tca = new Tca();
        
        tca.inFlow = flowCoverage;
        tca.pm = pm;

        tca.process();
        
        GridCoverage2D tcaCoverage = tca.outTca;

        checkMatrixEqual(tcaCoverage.getRenderedImage(), HMTestMaps.tcaData);

    }

}