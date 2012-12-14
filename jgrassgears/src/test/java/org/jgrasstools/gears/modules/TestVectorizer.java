/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.jgrasstools.gears.modules.v.vectorize.Vectorizer;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Geometry;

/**
 * Test for {@link Vectorizer}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestVectorizer extends HMTestCase {
    public void testCoverageSummary() throws Exception {

        double[][] inData = HMTestMaps.extractNet0Data;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D inCoverage = CoverageUtilities.buildCoverage("data", inData, envelopeParams, crs, true);

        Vectorizer vectorizer = new Vectorizer();
        vectorizer.pm = pm;
        vectorizer.inRaster = inCoverage;
        vectorizer.pValue = 2.0;
        vectorizer.pThres = 1;
        vectorizer.fDefault = "rast";
        vectorizer.process();

        SimpleFeatureCollection outGeodata = vectorizer.outVector;
        assertEquals(1, outGeodata.size());

        SimpleFeatureIterator featureIterator = outGeodata.features();
        assertTrue(featureIterator.hasNext());

        SimpleFeature feature = featureIterator.next();
        double value = ((Number) feature.getAttribute("rast")).doubleValue();
        assertEquals(1.0, value, 0.0000001);
        Geometry geometry = (Geometry) feature.getDefaultGeometry();
        double area = geometry.getArea();
        assertEquals(6300.0, area, 0.0000001);
    }

}
