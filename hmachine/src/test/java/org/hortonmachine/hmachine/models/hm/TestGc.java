package org.hortonmachine.hmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.utils.PrintUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.modules.geomorphology.gc.OmsGc;
import org.hortonmachine.hmachine.utils.HMTestCase;
import org.hortonmachine.hmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link OmsGc} models 
 * @author Daniele Andreis.
 *
 */
public class TestGc extends HMTestCase {

    @SuppressWarnings("nls")
    public void testGc() {
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();

        double[][] slopeData = HMTestMaps.slopeData;
        double[][] networkData = HMTestMaps.extractNet1Data;
        double[][] cp9Data = HMTestMaps.cp9Data;

        GridCoverage2D slopeGC = CoverageUtilities.buildCoverage("slope", slopeData, envelopeParams, crs, true);
        GridCoverage2D networkGC = CoverageUtilities.buildCoverage("net", networkData, envelopeParams, crs, true);
        GridCoverage2D cp9GC = CoverageUtilities.buildCoverage("cp9", cp9Data, envelopeParams, crs, true);

        OmsGc gc = new OmsGc();
        gc.inSlope = slopeGC;
        gc.inNetwork = networkGC;
        gc.inCp9 = cp9GC;
        gc.pTh=7;
        gc.process();
        
        GridCoverage2D outGCClasses= gc.outClasses;
        GridCoverage2D outGcAggregate = gc.outAggregateClasses;

        checkMatrixEqual(outGCClasses.getRenderedImage(), HMTestMaps.cp9GCData);
        checkMatrixEqual(outGcAggregate.getRenderedImage(), HMTestMaps.cp3GCData);

    }
}
