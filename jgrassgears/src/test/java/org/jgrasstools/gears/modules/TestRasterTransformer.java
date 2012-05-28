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

import java.awt.image.Raster;
import java.util.HashMap;
import java.util.List;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.Envelope2D;
import org.jgrasstools.gears.io.rasterreader.RasterReader;
import org.jgrasstools.gears.io.rasterwriter.RasterWriter;
import org.jgrasstools.gears.io.vectorwriter.VectorWriter;
import org.jgrasstools.gears.modules.r.transformer.RasterTransformer;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.PrintUtilities;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
/**
 * Test {@link RasterTransformer}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestRasterTransformer extends HMTestCase {

    public void testRasterTransformer() throws Exception {

        double[][] flowData = HMTestMaps.flowData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D flowCoverage = CoverageUtilities.buildCoverage("flow", flowData, envelopeParams, crs, true);

        RasterTransformer transformer = new RasterTransformer();
        transformer.inRaster = flowCoverage;
        transformer.pInterpolation = 2;
        transformer.pAngle = 90.0;
        transformer.pTransX = 100.0;
        transformer.pTransY = 100.0;
        transformer.process();
        // GridCoverage2D outCoverage = transformer.outRaster;
        SimpleFeatureCollection outBounds = transformer.outBounds;
        Geometry bound = FeatureUtilities.featureCollectionToGeometriesList(outBounds, false, null).get(0);

        String expected = "POLYGON ((1640780 5140150, 1641020 5140150, 1641020 5139850, 1640780 5139850, 1640780 5140150))";
        assertEquals(expected, bound.toText());

    }
}
