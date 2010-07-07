package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.skyview.Skyview;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link Skyview} module.
 * 
 * @author Daniele Andreis
 */
public class TestSkyview extends HMTestCase {

    public void testSkyview() throws Exception {

        // Locale.setDefault(Locale.ITALIAN);

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3004");
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        Skyview skyview = new Skyview();
        skyview.inElevation = elevationCoverage;

        skyview.pm = pm;

        skyview.process();

        GridCoverage2D hillshadeCoverage = skyview.outMap;

        checkMatrixEqual(hillshadeCoverage.getRenderedImage(), HMTestMaps.outSkyview, 0.03);
    }

}
