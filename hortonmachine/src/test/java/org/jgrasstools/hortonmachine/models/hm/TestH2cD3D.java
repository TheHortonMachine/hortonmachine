package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.hillslopeanalyses.h2c.H2cd;
import org.jgrasstools.hortonmachine.modules.hillslopeanalyses.h2cd3d.H2cD3D;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Tests the {@link H2cD3D} module.
 * 
 * @author Daniele Andreis
 */
public class TestH2cD3D extends HMTestCase {
    public void testH2cd3d() throws Exception {
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        double[][] elevData = HMTestMaps.pitData;
        GridCoverage2D elevRaster = CoverageUtilities.buildCoverage("pit", elevData, envelopeParams, crs, true);
        double[][] flowData = HMTestMaps.flowData;
        GridCoverage2D flowRaster = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);
        double[][] netData = HMTestMaps.extractNet1Data;
        GridCoverage2D netRaster = CoverageUtilities.buildCoverage("net", netData, envelopeParams, crs, true);

        H2cD3D h2cd = new H2cD3D();
        h2cd.inFlow = flowRaster;
        h2cd.inNet = netRaster;
        h2cd.inElev = elevRaster;
        h2cd.process();
        GridCoverage2D outH2cd = h2cd.outH2cD3D;

        checkMatrixEqual(outH2cd.getRenderedImage(), HMTestMaps.h2cd3dData);

    }
}
