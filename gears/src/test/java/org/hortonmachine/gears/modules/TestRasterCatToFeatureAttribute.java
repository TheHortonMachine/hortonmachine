/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.gears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.modules.v.rastercattofeatureattribute.OmsRasterCatToFeatureAttribute;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Test for {@link OmsRasterCatToFeatureAttribute}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterCatToFeatureAttribute extends HMTestCase {
    public void testRasterCatToFeatureAttribute() throws Exception {

        double[][] elevationData = HMTestMaps.outPitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, envelopeParams, crs, true);

        SimpleFeatureCollection inFC = HMTestMaps.getTestFC();

        OmsRasterCatToFeatureAttribute rc2fa = new OmsRasterCatToFeatureAttribute();
        rc2fa.pm = pm;
        rc2fa.inRaster = elevationCoverage;
        rc2fa.inVector = inFC;
        rc2fa.fNew = "elev";
        rc2fa.process();

        SimpleFeatureCollection outMap = rc2fa.outVector;

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

    public void testRasterCatToFeatureAttributePolygon() throws Exception {

        double[][] elevationData = HMTestMaps.outPitData;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, envelopeParams, crs, true);

        SimpleFeatureCollection inFC = HMTestMaps.getTestLeftFC();

        OmsRasterCatToFeatureAttribute rc2fa = new OmsRasterCatToFeatureAttribute();
        rc2fa.pm = pm;
        rc2fa.inRaster = elevationCoverage;
        rc2fa.inVector = inFC;
        rc2fa.fNew = "elev";
        rc2fa.process();

        SimpleFeatureCollection outMap = rc2fa.outVector;

        FeatureIterator<SimpleFeature> features = outMap.features();
        while( features.hasNext() ) {
            SimpleFeature feature = features.next();
            String attr = "elev_min";
            double value = getAttr(feature, attr);
            System.out.println(value);
            attr = "elev_max";
            value = getAttr(feature, attr);
            System.out.println(value);
            attr = "elev_avg";
            value = getAttr(feature, attr);
            System.out.println(value);
            attr = "elev_sum";
            value = getAttr(feature, attr);
            System.out.println(value);
        }
    }

    private double getAttr( SimpleFeature feature, String attr ) {
        Object attribute = feature.getAttribute(attr);
        double value = ((Number) attribute).doubleValue();
        return value;
    }

}
