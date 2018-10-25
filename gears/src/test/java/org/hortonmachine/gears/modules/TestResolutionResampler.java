package org.hortonmachine.gears.modules;
///*
// * JGrass - Free Open Source Java GIS http://www.jgrass.org 
// * (C) HydroloGIS - www.hydrologis.com 
// * 
// * This library is free software; you can redistribute it and/or modify it under
// * the terms of the GNU Library General Public License as published by the Free
// * Software Foundation; either version 2 of the License, or (at your option) any
// * later version.
// * 
// * This library is distributed in the hope that it will be useful, but WITHOUT
// * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
// * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
// * details.
// * 
// * You should have received a copy of the GNU Library General Public License
// * along with this library; if not, write to the Free Foundation, Inc., 59
// * Temple Place, Suite 330, Boston, MA 02111-1307 USA
// */
//package org.hortonmachine.gears.modules;
//
//import java.util.HashMap;
//
//import org.geotools.coverage.grid.GridCoverage2D;
//import org.geotools.data.simple.SimpleFeatureCollection;
//import org.geotools.feature.FeatureIterator;
//import org.hortonmachine.gears.libs.monitor.PrintStreamProgressMonitor;
//import org.hortonmachine.gears.modules.r.rasterreprojector.OmsRasterReprojector;
//import org.hortonmachine.gears.modules.r.transformer.OmsRasterResolutionResampler;
//import org.hortonmachine.gears.modules.v.vectorreprojector.OmsVectorReprojector;
//import org.hortonmachine.gears.utils.HMTestCase;
//import org.hortonmachine.gears.utils.HMTestMaps;
//import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
//import org.opengis.feature.simple.SimpleFeature;
//import org.opengis.referencing.crs.CoordinateReferenceSystem;
//
//import org.locationtech.jts.geom.Coordinate;
//import org.locationtech.jts.geom.Geometry;
///**
// * Test for the reprojection modules.
// * 
// * @author Andrea Antonello (www.hydrologis.com)
// */
//public class TestResolutionResampler extends HMTestCase {
//    public void testCoverageReprojector() throws Exception {
//
//        double[][] elevationData = HMTestMaps.mapData;
//        HashMap<String, Double> envelopeParams = HMTestMaps.getEnvelopeparams();
//        int origRows = envelopeParams.get(CoverageUtilities.ROWS).intValue();
//        int origCols = envelopeParams.get(CoverageUtilities.COLS).intValue();
//        CoordinateReferenceSystem crs = HMTestMaps.getCrs();
//        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, envelopeParams, crs, true);
//
//        OmsRasterResolutionResampler reprojector = new OmsRasterResolutionResampler();
//        reprojector.inGeodata = elevationCoverage;
//        reprojector.pInterpolation = 1;
//        reprojector.pXres = 60.0;
//        reprojector.process();
//
//        GridCoverage2D outMap = reprojector.outGeodata;
//
//        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(outMap);
//        double west = regionMap.get(CoverageUtilities.WEST);
//        double south = regionMap.get(CoverageUtilities.SOUTH);
//        double east = regionMap.get(CoverageUtilities.EAST);
//        double north = regionMap.get(CoverageUtilities.NORTH);
//        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();
//        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
//
//        assertEquals(rows, origRows / 2);
//        assertEquals(cols, origCols / 2);
//
//        checkMatrixEqual(outMap.getRenderedImage(), HMTestMaps.mapDataHalf, 0);
//    }
//
//}
