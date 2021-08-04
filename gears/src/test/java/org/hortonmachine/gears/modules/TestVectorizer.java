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
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.gears.modules.v.vectorize.OmsVectorizer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Geometry;

/**
 * Test for {@link OmsVectorizer}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorizer extends HMTestCase {
    public void testVectorizer1() throws Exception {

        double[][] inData = HMTestMaps.extractNet0Data;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);

        OmsVectorizer vectorizer = new OmsVectorizer();
        vectorizer.pm = pm;
        vectorizer.inRaster = inCoverage;
        vectorizer.pValue = 2.0;
        vectorizer.pThres = 2;
        vectorizer.fDefault = "rast";
        vectorizer.process();

        SimpleFeatureCollection outGeodata = vectorizer.outVector;
        List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(outGeodata);
        SimpleFeature f1 = features.get(0);
        Geometry g1 = (Geometry) f1.getDefaultGeometry();

        assertEquals(2, outGeodata.size());
        SimpleFeature f2 = features.get(1);
        Geometry g2 = (Geometry) f2.getDefaultGeometry();

        // SimpleFeature nvFeature = f1;
        SimpleFeature valuesFeature = f2;
        if (g1.getArea() < g2.getArea()) {
            // nvFeature = f2;
            valuesFeature = f1;
        }

        double value = ((Number) valuesFeature.getAttribute("rast")).doubleValue();
        assertEquals(2.0, value, 0.0000001);
        Geometry geometry = (Geometry) valuesFeature.getDefaultGeometry();
        double area = geometry.getArea();
        assertEquals(6300.0, area, 0.0000001);
    }

    public void testVectorizer2() throws Exception {
        double[][] inData = HMTestMaps.extractNet0Data;
        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);

        OmsVectorizer vectorizer = new OmsVectorizer();
        vectorizer.pm = pm;
        vectorizer.inRaster = inCoverage;
        vectorizer.pValue = null;
        vectorizer.pThres = 1;
        vectorizer.fDefault = "rast";
        vectorizer.process();

        SimpleFeatureCollection outGeodata = vectorizer.outVector;
        assertEquals(2, outGeodata.size());

        List<SimpleFeature> features = FeatureUtilities.featureCollectionToList(outGeodata);
        SimpleFeature f1 = features.get(0);
        SimpleFeature f2 = features.get(1);
        Geometry g1 = (Geometry) f1.getDefaultGeometry();
        Geometry g2 = (Geometry) f2.getDefaultGeometry();

        // SimpleFeature nvFeature = f1;
        SimpleFeature valuesFeature = f2;
        if (g1.getArea() < g2.getArea()) {
            // nvFeature = f2;
            valuesFeature = f1;
        }

        double value = ((Number) valuesFeature.getAttribute("rast")).doubleValue();
        assertEquals(2.0, value, 0.0000001);
        Geometry geometry = (Geometry) valuesFeature.getDefaultGeometry();
        double area = geometry.getArea();
        assertEquals(6300.0, area, 0.0000001);
    }

}
