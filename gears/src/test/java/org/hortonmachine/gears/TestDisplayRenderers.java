/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.HMTestMaps;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.display.DisplayImage;
import org.hortonmachine.gears.utils.display.DisplayOptions;
import org.hortonmachine.gears.utils.display.DisplayPart;
import org.hortonmachine.gears.utils.display.DisplayRenderers;
import org.hortonmachine.gears.utils.display.DisplayTable;
import org.hortonmachine.gears.utils.display.DisplayText;
import org.hortonmachine.gears.utils.display.IDisplayRenderer;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

/**
 * Test the {@link DisplayRenderers} and the built in renderers.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TestDisplayRenderers extends HMTestCase {

    private static final DisplayOptions OPTIONS = new DisplayOptions(200, 150, 2);

    private GridCoverage2D elevation() {
        return CoverageUtilities.buildCoverage("elevation", HMTestMaps.mapData, HMTestMaps.getEnvelopeparams(),
                HMTestMaps.getCrs(), true);
    }

    public void testNullAndFallback() throws Exception {
        assertTrue(DisplayRenderers.render(null, OPTIONS).isEmpty());

        List<DisplayPart> parts = DisplayRenderers.render(42, OPTIONS);
        assertEquals(1, parts.size());
        assertEquals("42", ((DisplayText) parts.get(0)).text());
    }

    public void testRaster() throws Exception {
        List<DisplayPart> parts = DisplayRenderers.render(elevation(), OPTIONS);
        assertEquals(2, parts.size());
        String info = ((DisplayText) parts.get(0)).text();
        assertTrue(info, info.startsWith("Raster 10 x 8 cells"));
        assertTrue(info, info.contains("EPSG:32632"));
        // the novalue cell is not considered
        assertTrue(info, info.contains("min 400,"));
        assertTrue(info, info.contains("max 1500,"));
        assertTrue(info, info.contains("res 30 x 30"));
        assertPainted(((DisplayImage) parts.get(1)).image());

        parts = DisplayRenderers.render(HMRaster.fromGridCoverage(elevation()), OPTIONS);
        assertEquals(2, parts.size());
        assertTrue(parts.get(1) instanceof DisplayImage);
    }

    public void testFeatures() throws Exception {
        List<DisplayPart> parts = DisplayRenderers.render(HMTestMaps.getTestFC(), OPTIONS);
        assertEquals(3, parts.size());
        assertEquals("3 features, Point, EPSG:32632", ((DisplayText) parts.get(0)).text());
        assertPainted(((DisplayImage) parts.get(1)).image());

        DisplayTable table = (DisplayTable) parts.get(2);
        assertEquals(List.of("fid", "cat"), table.header());
        // limited by the options
        assertEquals(2, table.rows().size());
        assertEquals(3, table.totalRows());
        assertTrue(table.isTruncated());
    }

    public void testSingleFeatureIsTransposed() throws Exception {
        DefaultFeatureCollection single = new DefaultFeatureCollection();
        try (SimpleFeatureIterator iterator = HMTestMaps.getTestFC().features()) {
            single.add(iterator.next());
        }
        List<DisplayPart> parts = DisplayRenderers.render(single, OPTIONS);
        assertEquals(3, parts.size());
        assertEquals("1 feature, Point, EPSG:32632", ((DisplayText) parts.get(0)).text());

        DisplayTable table = (DisplayTable) parts.get(2);
        assertEquals(List.of("Name", "Value"), table.header());
        // fid and cat, no geometry
        assertEquals(2, table.rows().size());
        assertEquals("fid", table.rows().get(0).get(0));
        assertEquals(List.of("cat", "1"), table.rows().get(1));
    }

    public void testGeometries() throws Exception {
        WKTReader reader = new WKTReader();
        Geometry polygon = reader.read("POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0))");
        List<DisplayPart> parts = DisplayRenderers.render(polygon, OPTIONS);
        assertEquals(2, parts.size());
        assertEquals("POLYGON ((0 0, 10 0, 10 10, 0 10, 0 0))", ((DisplayText) parts.get(0)).text());
        assertPainted(((DisplayImage) parts.get(1)).image());

        // a single point is just its text
        parts = DisplayRenderers.render(reader.read("POINT (5 5)"), OPTIONS);
        assertEquals(1, parts.size());
        assertEquals("POINT (5 5)", ((DisplayText) parts.get(0)).text());

        // two equal points have no extent
        parts = DisplayRenderers.render(List.of(reader.read("POINT (5 5)"), reader.read("POINT (5 5)")), OPTIONS);
        assertPainted(((DisplayImage) parts.get(1)).image());

        // mixed types and collections
        List<Geometry> mixed = List.of(polygon, reader.read("LINESTRING (0 0, 20 20)"),
                reader.read("GEOMETRYCOLLECTION (POINT (1 1), POINT (2 2))"));
        parts = DisplayRenderers.render(mixed, OPTIONS);
        assertEquals("3 geometries", ((DisplayText) parts.get(0)).text());
        assertPainted(((DisplayImage) parts.get(1)).image());
    }

    public void testTables() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("a", 1);
        map.put("b", "text");
        DisplayTable table = (DisplayTable) DisplayRenderers.render(map, OPTIONS).get(0);
        assertEquals(List.of("Key", "Value"), table.header());
        assertEquals(List.of("a", "1"), table.rows().get(0));
        assertEquals(List.of("b", "'text'"), table.rows().get(1));

        table = (DisplayTable) DisplayRenderers.render(List.of(List.of(1, 2), List.of(3)), OPTIONS).get(0);
        assertEquals(List.of("Value 1", "Value 2"), table.header());
        assertEquals(List.of("3", ""), table.rows().get(1));

        table = (DisplayTable) DisplayRenderers.render(new double[][]{{1, 2}, {3, 4}, {5, 6}}, OPTIONS).get(0);
        assertEquals(2, table.rows().size());
        assertEquals(3, table.totalRows());

        // an empty list is text
        assertTrue(DisplayRenderers.render(List.of(), OPTIONS).get(0) instanceof DisplayText);
    }

    public void testFiles() throws Exception {
        File tif = File.createTempFile("hm_display_", ".tif");
        File gpkg = File.createTempFile("hm_display_", ".gpkg");
        try {
            OmsRasterWriter.writeRaster(tif.getAbsolutePath(), elevation());
            List<DisplayPart> parts = DisplayRenderers.render(tif, OPTIONS);
            assertTrue(((DisplayText) parts.get(0)).text().startsWith("Raster 10 x 8 cells"));
            assertTrue(parts.get(1) instanceof DisplayImage);

            gpkg.delete();
            OmsVectorWriter.writeVector(gpkg.getAbsolutePath() + "#points", HMTestMaps.getTestFC());
            OmsVectorWriter.writeVector(gpkg.getAbsolutePath() + "#morepoints", HMTestMaps.getTestFC());
            parts = DisplayRenderers.render(gpkg.toPath(), OPTIONS);
            String tables = ((DisplayText) parts.get(0)).text();
            assertTrue(tables, tables.startsWith("Vector tables: "));
            assertTrue(((DisplayText) parts.get(1)).text().startsWith("3 features"));
            assertTrue(parts.get(2) instanceof DisplayImage);

            parts = DisplayRenderers.render(new File(gpkg.getAbsolutePath() + "#morepoints"), OPTIONS);
            assertTrue(((DisplayText) parts.get(0)).text().startsWith("3 features"));
        } finally {
            tif.delete();
            gpkg.delete();
        }

        // a missing file is just text
        assertTrue(DisplayRenderers.render(new File("/not/existing.tif"), OPTIONS).get(0) instanceof DisplayText);
    }

    public void testRegisteredRendererComesFirst() throws Exception {
        IDisplayRenderer custom = new IDisplayRenderer(){
            public boolean canRender( Object value ) {
                return value instanceof GridCoverage2D;
            }
            public List<DisplayPart> render( Object value, DisplayOptions options ) {
                return List.of(new DisplayText("custom"));
            }
        };
        DisplayRenderers.register(custom);
        try {
            List<DisplayPart> parts = DisplayRenderers.render(elevation(), OPTIONS);
            assertEquals("custom", ((DisplayText) parts.get(0)).text());
        } finally {
            DisplayRenderers.unregister(custom);
        }
        assertEquals(2, DisplayRenderers.render(elevation(), OPTIONS).size());
    }

    public void testFailingRendererFallsBackToText() throws Exception {
        IDisplayRenderer failing = new IDisplayRenderer(){
            public boolean canRender( Object value ) {
                return value instanceof String;
            }
            public List<DisplayPart> render( Object value, DisplayOptions options ) throws Exception {
                throw new Exception("boom");
            }
        };
        DisplayRenderers.register(failing);
        try {
            List<DisplayPart> parts = DisplayRenderers.render("value", OPTIONS);
            assertEquals(2, parts.size());
            assertEquals("value", ((DisplayText) parts.get(0)).text());
            assertTrue(((DisplayText) parts.get(1)).text().contains("boom"));
        } finally {
            DisplayRenderers.unregister(failing);
        }
    }

    private void assertPainted( BufferedImage image ) {
        assertEquals(200, image.getWidth());
        assertEquals(150, image.getHeight());
        for( int x = 0; x < image.getWidth(); x++ ) {
            for( int y = 0; y < image.getHeight(); y++ ) {
                if (((image.getRGB(x, y) >>> 24) & 0xFF) > 0) {
                    return;
                }
            }
        }
        fail("the image is empty");
    }
}
