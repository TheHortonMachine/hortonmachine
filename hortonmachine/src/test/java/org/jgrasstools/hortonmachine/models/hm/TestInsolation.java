package org.jgrasstools.hortonmachine.models.hm;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.referencing.CRS;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.insolation.Insolation;
import org.jgrasstools.hortonmachine.utils.HMTestCase;
import org.jgrasstools.hortonmachine.utils.HMTestMaps;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test the {@link Insolation} module.
 * 
 * @author Daniele Andreis
 */
public class TestInsolation extends HMTestCase {

    private final static String START_DATE = "2010-01-01";
    private final static String END_DATE = "2010-01-02";

    public void testInsolation() throws Exception {

        // Locale.setDefault(Locale.ITALIAN);

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3004");
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, envelopeParams, crs, true);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.out);

        Insolation insolation = new Insolation();
        insolation.inElevation = elevationCoverage;
        insolation.tStartDate = START_DATE;
        insolation.tEndDate = END_DATE;
        // insolation.defaultLapse=-.0065;
        // insolation.defaultRH=0.4;
        // insolation.defaultVisibility=60;

        insolation.pm = pm;

        insolation.process();

        GridCoverage2D insolationCoverage = insolation.outMap;

        checkMatrixEqual(insolationCoverage.getRenderedImage(), HMTestMaps.outInsolation, 0.1);
    }

}
