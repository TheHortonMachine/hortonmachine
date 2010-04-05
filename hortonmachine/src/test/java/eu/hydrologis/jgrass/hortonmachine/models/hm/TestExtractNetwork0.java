package eu.hydrologis.jgrass.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import eu.hydrologis.jgrass.hortonmachine.modules.network.extractnetwork.ExtractNetwork;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestCase;
import eu.hydrologis.jgrass.hortonmachine.utils.HMTestMaps;
import eu.hydrologis.jgrass.jgrassgears.libs.monitor.PrintStreamProgressMonitor;
import eu.hydrologis.jgrass.jgrassgears.utils.coverage.CoverageUtilities;
/**
 * It test the {@link ExtractNetwork} module with mode=0.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestExtractNetwork0 extends HMTestCase {
    public void testExtractNetwork() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;

        double[][] flowData = HMTestMaps.mflowDataBorder;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs);
        double[][] tcaData = HMTestMaps.tcaData;
        GridCoverage2D tcaCoverage = CoverageUtilities.buildCoverage("tca", tcaData, envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out); 
        
        ExtractNetwork extractNetwork = new ExtractNetwork();
        extractNetwork.pm = pm;
        extractNetwork.inFlow = flowCoverage;
        extractNetwork.inTca = tcaCoverage;
        extractNetwork.pMode = 0;
        extractNetwork.pThres = 5;
        
        extractNetwork.process();
        
        GridCoverage2D networkCoverage = extractNetwork.outNet;
        checkMatrixEqual(networkCoverage.getRenderedImage(), HMTestMaps.extractNet0Data, 0.01);
    }

}
