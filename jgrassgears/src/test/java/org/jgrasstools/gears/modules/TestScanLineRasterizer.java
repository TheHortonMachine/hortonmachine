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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.Envelope2D;
import org.jgrasstools.gears.modules.r.scanline.ScanLineRasterizer;
import org.jgrasstools.gears.modules.v.vectorfilter.VectorFilter;
import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.HMTestMaps;
import org.jgrasstools.gears.utils.RegionMap;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Polygon;
/**
 * Test for the {@link ScanLineRasterizer}
 * <
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestScanLineRasterizer extends HMTestCase {

    @SuppressWarnings("nls")
    public void testScanLineRasterizer() throws Exception {
        RegionMap ep = HMTestMaps.envelopeParams;
        SimpleFeatureCollection newCollection = doCollection(ep);

        ScanLineRasterizer raster = new ScanLineRasterizer();
        raster.inGeodata = newCollection;
        raster.cols = ep.getCols();
        raster.rows = ep.getRows();
        raster.north = ep.getNorth();
        raster.south = ep.getSouth();
        raster.east = ep.getEast();
        raster.west = ep.getWest();
        raster.pValue = 2.0;
        raster.process();
        
        GridCoverage2D outGeodata = raster.outGeodata;
        
        checkMatrixEqual(outGeodata.getRenderedImage(), matrix);
        
        
        assertTrue(outFC.size() == 1);

        FeatureIterator<SimpleFeature> featureIterator = outFC.features();
        SimpleFeature feature = featureIterator.next();
        assertNotNull(feature);

        Integer attribute = (Integer) feature.getAttribute("cat");
        assertEquals(3, attribute.intValue());
        featureIterator.close();

    }

    private SimpleFeatureCollection doCollection(RegionMap envelopeParams) {
        double[][] elevationData = HMTestMaps.mapData;
        CoordinateReferenceSystem crs = HMTestMaps.crs;
        GridCoverage2D elevationCoverage = CoverageUtilities.buildCoverage("elevation", elevationData, envelopeParams, crs, true);
        Envelope2D envelope = elevationCoverage.getEnvelope2D();
        Polygon polygon = FeatureUtilities.envelopeToPolygon(envelope);
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("typename");
        b.setCRS(crs);
        b.add("the_geom", Polygon.class);
        b.add("attributename", Double.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Object[] values = new Object[]{polygon, 1.0};
        builder.addAll(values);
        SimpleFeature feature = builder.buildFeature(null);
        SimpleFeatureCollection newCollection = FeatureCollections.newCollection();
        newCollection.add(feature);
        return newCollection;
    }
}
