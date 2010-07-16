package org.jgrasstools.gears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.modules.r.cutout.CutOut;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestCutout extends HMTestCase {

    private GridCoverage2D inCoverage;
    private GridCoverage2D inMask;
    private PrintStreamProgressMonitor pm;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        double[][] inData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);

        double[][] maskData = HMTestMaps.extractNet0Data;
        inMask = CoverageUtilities.buildCoverage("data", maskData, envelopeParams, crs, true);

        pm = new PrintStreamProgressMonitor(System.out, System.err);
    }

    public void testCutout() throws Exception {
        CutOut cutout = new CutOut();
        cutout.pm = pm;
        cutout.inGeodata = inCoverage;
        cutout.inMask = inMask;
        cutout.process();
        GridCoverage2D out = cutout.outGeodata;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.cutoutData, 0);
    }

    public void testCutoutInverse() throws Exception {
        CutOut cutout = new CutOut();
        cutout.pm = pm;
        cutout.inGeodata = inCoverage;
        cutout.inMask = inMask;
        cutout.doInverse = true;
        cutout.process();
        GridCoverage2D out = cutout.outGeodata;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.cutoutDataInverse, 0);
    }

    public void testCutoutInverseWithThresholds() throws Exception {
        CutOut cutout = new CutOut();
        cutout.pm = pm;
        cutout.inGeodata = inCoverage;
        cutout.inMask = inMask;
        cutout.doInverse = true;
        cutout.pMax = 1400.0;
        cutout.pMin = 800.0;
        cutout.process();
        GridCoverage2D out = cutout.outGeodata;
        checkMatrixEqual(out.getRenderedImage(), HMTestMaps.cutoutDataMaxMinInverse800_1400, 0);
    }

}
