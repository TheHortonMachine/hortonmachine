package org.jgrasstools.gears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.modules.r.mapcalc.Mapcalc;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestMapcalc extends HMTestCase {
    public void testMapcalc() throws Exception {

        double[][] elevationData = HMTestMaps.outPitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation",
                elevationData, envelopeParams, crs);

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        HashMap<String, GridCoverage2D> maps = new HashMap<String, GridCoverage2D>();
        maps.put("map1", elevationCoverage);
        maps.put("map2", elevationCoverage);

        Mapcalc mapcalc = new Mapcalc();
        mapcalc.pm = pm;
        mapcalc.inMaps = maps;
        mapcalc.pFunction = "\"map1\"*2-\"map2\" + sqrt(\"map1\"*\"map1\")-\"map1\"";

        mapcalc.process();

        GridCoverage2D outMap = mapcalc.outMap;
        checkMatrixEqual(outMap.getRenderedImage(), HMTestMaps.outPitData, 0);
    }

}
