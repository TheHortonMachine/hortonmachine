package org.jgrasstools.gears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.filter.text.cql2.CQL;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.modules.r.mapcalc.Mapcalc;
import org.jgrasstools.gears.modules.v.rastercattofeatureattribute.RasterCatToFeatureAttribute;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class TestRasterCatToFeatureAttribute extends HMTestCase {
    public void testMapcalc() throws Exception {

        double[][] elevationData = HMTestMaps.outPitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation",
                elevationData, envelopeParams, crs, true);

        FeatureCollection<SimpleFeatureType, SimpleFeature> inFC = HMTestMaps.testFC;

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        RasterCatToFeatureAttribute rc2fa = new RasterCatToFeatureAttribute();
        rc2fa.pm = pm;
        rc2fa.inCoverage = elevationCoverage;
        rc2fa.inFC = inFC;
        rc2fa.fNew = "elev";
        rc2fa.process();

        FeatureCollection<SimpleFeatureType, SimpleFeature> outMap = rc2fa.outGeodata;

        FeatureIterator<SimpleFeature> features = outMap.features();
        while( features.hasNext() ) {
            SimpleFeature feature = features.next();
            Object attribute = feature.getAttribute("elev");
            double value = ((Number) attribute).doubleValue();

            Object catObj = feature.getAttribute("cat");
            int cat = ((Number) catObj).intValue();
            if (cat == 1) {
                assertEquals(800.0, value, 0.000001);
            } else if (cat == 2) {
                assertEquals(1500.0, value, 0.000001);
            } else if (cat == 3) {
                assertEquals(700.0, value, 0.000001);
            }
        }

    }

}
