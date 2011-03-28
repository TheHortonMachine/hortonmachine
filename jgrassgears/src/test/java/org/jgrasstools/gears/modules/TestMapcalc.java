package org.jgrasstools.gears.modules;

import java.awt.image.RenderedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.modules.r.mapcalc.Mapcalc;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestMapcalc extends HMTestCase {
    @SuppressWarnings("nls")
    public void testMapcalc() throws Exception {

        double[][] elevationData = HMTestMaps.outPitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("ele", elevationData, envelopeParams, crs, true);

        List<GridCoverage2D> maps = Arrays.asList(elevationCoverage);

        Mapcalc mapcalc = new Mapcalc();
        mapcalc.inMaps = maps;
        mapcalc.pFunction = "ele*2-ele + sqrt(ele)^2-exp(log(ele));";

        mapcalc.process();

        GridCoverage2D outMap = mapcalc.outMap;
        RenderedImage renderedImage = outMap.getRenderedImage();
        // printImage(renderedImage);
        checkMatrixEqual(renderedImage, HMTestMaps.outPitData, 0.000000001);
    }

    public static void main( String[] args ) throws Exception {
        new TestMapcalc().testMapcalc();
    }
}
