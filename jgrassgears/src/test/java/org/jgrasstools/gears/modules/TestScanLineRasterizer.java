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

import java.util.List;

import javax.media.jai.iterator.RandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.Envelope2D;
import org.jgrasstools.gears.modules.r.scanline.OmsScanLineRasterizer;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Polygon;
/**
 * Test for the {@link OmsScanLineRasterizer}
 * <
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestScanLineRasterizer extends HMTestCase {
    private CoordinateReferenceSystem crs = HMTestMaps.getCrs();
    private RegionMap ep = HMTestMaps.getEnvelopeparams();
    private Polygon polygon;
    private GridCoverage2D elevationCoverage;
    private double[][] elevationData;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        elevationData = HMTestMaps.mapData;
        elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, ep, crs, true);
        Envelope2D envelope = elevationCoverage.getEnvelope2D();
        polygon = FeatureUtilities.envelopeToPolygon(envelope);
    }

    @SuppressWarnings("nls")
    public void testScanLineRasterizer() throws Exception {
        RegionMap ep = HMTestMaps.getEnvelopeparams();
        SimpleFeatureCollection newCollection = doCollection(ep);

        OmsScanLineRasterizer raster = new OmsScanLineRasterizer();
        raster.inVector = newCollection;
        raster.pCols = ep.getCols();
        raster.pRows = ep.getRows();
        raster.pNorth = ep.getNorth();
        raster.pSouth = ep.getSouth();
        raster.pEast = ep.getEast();
        raster.pWest = ep.getWest();
        raster.pValue = 2.0;
        raster.process();

        GridCoverage2D outGeodata = raster.outRaster;
        checkMatrixEqual(outGeodata.getRenderedImage(), HMTestMaps.all2Data);

        raster = new OmsScanLineRasterizer();
        raster.inVector = newCollection;
        raster.pCols = ep.getCols();
        raster.pRows = ep.getRows();
        raster.pNorth = ep.getNorth();
        raster.pSouth = ep.getSouth();
        raster.pEast = ep.getEast();
        raster.pWest = ep.getWest();
        raster.fCat = "cat";
        raster.process();

        outGeodata = raster.outRaster;
        checkMatrixEqual(outGeodata.getRenderedImage(), HMTestMaps.all1Data);
    }

    public void testScanLineRasterizerUtilsMethod() throws Exception {
        RandomIter rasterIter = CoverageUtilities.getRandomIterator(elevationCoverage);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(elevationCoverage);

        List<Coordinate> coordinates = FeatureUtilities.extractPolygonOnCoverage(rasterIter, regionMap.getCols(),
                regionMap.getRows(), regionMap.getXres(), elevationCoverage.getGridGeometry(), polygon, 0);

        int index = 0;
        for( int r = 0; r < elevationData.length; r++ ) {
            for( int c = 0; c < elevationData[0].length; c++ ) {
                Coordinate coordinate = coordinates.get(index++);
                assertEquals(elevationData[r][c], coordinate.z, DELTA);
            }
        }
    }

    private SimpleFeatureCollection doCollection( RegionMap envelopeParams ) {

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("typename");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("cat", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Object[] values = new Object[]{polygon, 1.0};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(null);
        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        newCollection.add(feature);
        return newCollection;
    }
}
