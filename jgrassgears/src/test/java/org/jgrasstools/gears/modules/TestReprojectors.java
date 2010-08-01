/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.gears.modules;

import java.util.HashMap;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;
import org.jgrasstools.gears.modules.r.coveragereprojector.CoverageReprojector;
import org.jgrasstools.gears.modules.v.featurereprojector.FeatureReprojector;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
/**
 * Test for the reprojection modules.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestReprojectors extends HMTestCase {
    public void testCoverageReprojector() throws Exception {

        double[][] elevationData = HMTestMaps.mapData;
        HashMap<String, Double> envelopeParams = HMTestMaps.envelopeParams;
        int origRows = envelopeParams.get(CoverageUtilities.ROWS).intValue();
        int origCols = envelopeParams.get(CoverageUtilities.COLS).intValue();
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation",
                elevationData, envelopeParams, crs, true);

        CoverageReprojector reprojector = new CoverageReprojector();
        reprojector.inGeodata = elevationCoverage;
        reprojector.pCode = "EPSG:4326";
        reprojector.process();

        GridCoverage2D outMap = reprojector.outGeodata;

        HashMap<String, Double> regionMap = CoverageUtilities
                .getRegionParamsFromGridCoverage(outMap);
        double west = regionMap.get(CoverageUtilities.WEST);
        double south = regionMap.get(CoverageUtilities.SOUTH);
        double east = regionMap.get(CoverageUtilities.EAST);
        double north = regionMap.get(CoverageUtilities.NORTH);
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();

        assertEquals(rows, origRows);
        assertEquals(cols, origCols);

        assertEquals(45.468, west, 0.001);
        assertEquals(45.471, east, 0.001);
        assertEquals(23.6, north, 0.001);
        assertEquals(23.595, south, 0.001);

        checkMatrixEqual(outMap.getRenderedImage(), HMTestMaps.mapData4326, 0);
    }

    public void testFeatureReprojector() throws Exception {

        PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

        FeatureCollection<SimpleFeatureType, SimpleFeature> testFC = HMTestMaps.testFC;

        FeatureReprojector reprojector = new FeatureReprojector();
        reprojector.inGeodata = testFC;
        reprojector.pCode = "EPSG:4326";
        reprojector.pm = pm;
        reprojector.process();

        FeatureCollection<SimpleFeatureType, SimpleFeature> outFC = reprojector.outGeodata;

        FeatureIterator<SimpleFeature> featureIterator = outFC.features();
        while( featureIterator.hasNext() ) {
            SimpleFeature feature = featureIterator.next();
            Geometry geometry = (Geometry) feature.getDefaultGeometry();
            Coordinate coordinate = geometry.getCoordinate();
            Integer attribute = (Integer) feature.getAttribute("cat");
            if (attribute == 1) {
                assertEquals(45.471, coordinate.x, 0.01);
                assertEquals(23.595, coordinate.y, 0.01);
            }
            if (attribute == 2) {
                assertEquals(45.468, coordinate.x, 0.01);
                assertEquals(23.6, coordinate.y, 0.01);
            }

        }
        featureIterator.close();

    }
}
