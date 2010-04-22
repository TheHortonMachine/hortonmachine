package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.network.netnumbering.NetNumbering;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
/**
 * Test netnumbering.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestNetnumbering extends HMTestCase {

    public void testNetnumberingMode0() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs);
        double[][] netData = HMTestMaps.extractNet1Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        NetNumbering netNumbering = new NetNumbering();
        netNumbering.inFlow = flowCoverage;
        netNumbering.inNet = netCoverage;
        netNumbering.pMode = 0;
        netNumbering.pm = pm;

        netNumbering.process();

        GridCoverage2D netnumberingCoverage = netNumbering.outNetnum;
        GridCoverage2D subbasinsCoverage = netNumbering.outBasins;

        checkMatrixEqual(netnumberingCoverage.getRenderedImage(), HMTestMaps.netNumberingChannelDataNN0, 0);
        checkMatrixEqual(subbasinsCoverage.getRenderedImage(), HMTestMaps.basinDataNN0, 0);
    }

    public void testNetnumberingMode1() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        
        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs);
        double[][] netData = HMTestMaps.extractNet1Data;
        GridCoverage2D netCoverage = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);
        
        NetNumbering netNumbering = new NetNumbering();
        netNumbering.inFlow = flowCoverage;
        netNumbering.inNet = netCoverage;
        netNumbering.inTca = tcaCoverage;
        netNumbering.pMode = 1;
        netNumbering.pThres = 2.0;
        netNumbering.pm = pm;
        
        netNumbering.process();
        
        GridCoverage2D netnumberingCoverage = netNumbering.outNetnum;
        GridCoverage2D subbasinsCoverage = netNumbering.outBasins;
        
        checkMatrixEqual(netnumberingCoverage.getRenderedImage(), HMTestMaps.netNumberingChannelDataNN1, 0);
        checkMatrixEqual(subbasinsCoverage.getRenderedImage(), HMTestMaps.basinDataNN1, 0);
    }

}
