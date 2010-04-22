package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.statistics.cb.Cb;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for the Cb module.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestCb extends HMTestCase {

            private double[][] expected = new double[][]{
                    { 400.0, 1.0, 400.0}, 
                    { 420.0, 2.0, 420.0}, 
                    { 450.0, 3.0, 450.0},
                    { 498.0, 5.0, 498.0}, 
                    { 550.0, 2.0, 550.0}, 
                    { 600.0, 4.0, 600.0}, 
                    { 691.6666666666666, 6.0, 691.6666666666666},
                    { 751.6666666666666, 6.0, 751.6666666666666}, 
                    { 775.0, 2.0, 775.0}, 
                    { 798.5714285714286, 7.0, 798.5714285714286},
                    { 852.0, 5.0, 852.0}, 
                    { 900.0, 3.0, 900.0}, 
                    { 945.0, 2.0, 945.0}, 
                    { 1000.1428571428571, 7.0, 1000.1428571428571},
                    { 1100.0, 2.0, 1100.0}, 
                    { 1150.0, 2.0, 1150.0}, 
                    { 1200.0, 3.0, 1200.0}, 
                    { 1250.0, 4.0, 1250.0}, 
                    { 1300.0, 2.0, 1300.0},
                    { 1416.6666666666667, 3.0, 1416.6666666666667}, 
                    { 1500.0, 7.0, 1500.0}
                    };

    public void testCb() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        
        GridCoverage2D map1 = CoverageUtilities.buildCoverage("map1", HMTestMaps.mapData, envelopeParams, crs);
        GridCoverage2D map2 = map1;
        
        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        Cb cb = new Cb();
        cb.pBins = 100;
        cb.pFirst = 1;
        cb.pLast = 1;
        cb.inMap1 = map1;
        cb.inMap2 = map2;
        cb.pm = pm;
        
        cb.process();
        
        double[][] moments = cb.outCb;
        
        for( int i = 0; i < moments.length; i++ ) {
            for( int j = 0; j < moments[0].length; j++ ) {
                double value = moments[i][j];
                assertEquals(value, expected[i][j], 0.01);
            }
        }
    }

}
