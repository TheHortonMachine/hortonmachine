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
package org.hortonmachine.gears.utils.style;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.eclipse.imagen.RasterFactory;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.GridReaderLayer;
import org.geotools.map.MapContent;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.StreamingRenderer;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.colors.DefaultTables;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities.ValueClass;

public class TestRasterStyleUtilities extends HMTestCase {

    private static ColorMap colorMap( Style style ) throws Exception {
        // through SLD, as when the style is saved and read again
        Style read = SldUtilities.getStyleFromSldString(SldUtilities.styleToString(style));
        RasterSymbolizer symbolizer = (RasterSymbolizer) read.featureTypeStyles().get(0).rules().get(0).symbolizers().get(0);
        return symbolizer.getColorMap();
    }

    public void testColorsSpreadOnRange() throws Exception {
        String table = "255 0 0\n0 255 0\n0 0 255\n";
        assertFalse(RasterStyleUtilities.colortableHasValues(table));
        assertEquals(List.of(Color.RED, Color.GREEN, Color.BLUE), RasterStyleUtilities.getColortableColors(table));

        ColorMap map = colorMap(RasterStyleUtilities.createStyleForColortableString(table, 10, 30, null, 1.0));
        double[] range = RasterStyleUtilities.getColorMapRange(map);
        assertEquals(10.0, range[0], 1E-9);
        assertEquals(30.0, range[1], 1E-9);
        // the transparent novalue entry is left out
        assertEquals("10 255 0 0\n20 0 255 0\n30 0 0 255\n", RasterStyleUtilities.toColortableString(map));
    }

    public void testTableWithValuesIgnoresRange() throws Exception {
        String table = "# a comment\n1 255 255 0\n5 0 0 255\n";
        assertTrue(RasterStyleUtilities.colortableHasValues(table));
        assertEquals(2, RasterStyleUtilities.getColortableColors(table).size());

        ColorMap map = colorMap(RasterStyleUtilities.createStyleForColortableString(table, 100, 200, null, 1.0));
        assertEquals("1 255 255 0\n5 0 0 255\n", RasterStyleUtilities.toColortableString(map));
    }

    public void testDefaultTableRoundTrip() throws Exception {
        String elev = new DefaultTables().getTableString(EColorTables.elev.name());
        ColorMap map = colorMap(RasterStyleUtilities.createStyleForColortable(EColorTables.elev.name(), 0, 1000, 1.0));
        String written = RasterStyleUtilities.toColortableString(map);
        // the written table has values and gives the same colors and style
        assertTrue(RasterStyleUtilities.colortableHasValues(written));
        assertEquals(RasterStyleUtilities.getColortableColors(elev), RasterStyleUtilities.getColortableColors(written));
        ColorMap again = colorMap(RasterStyleUtilities.createStyleForColortableString(written, -5, 5, null, 1.0));
        assertEquals(written, RasterStyleUtilities.toColortableString(again));
    }

    public void testGrayChannelRoundTrip() throws Exception {
        Style style = RasterStyleUtilities.createStyleForColortable(EColorTables.elev.name(), 0, 100, 1.0);
        assertEquals(-1, RasterStyleUtilities.getGrayChannel(style));

        RasterStyleUtilities.setGrayChannel(style, 2);
        String sld = SldUtilities.styleToString(style);
        // SLD channel names count from 1
        assertTrue(sld.contains("SourceChannelName>3<"));
        Style read = SldUtilities.getStyleFromSldString(sld);
        assertEquals(2, RasterStyleUtilities.getGrayChannel(read));
        // the colortable is kept next to the channel selection
        assertEquals(RasterStyleUtilities.toColortableString(colorMap(style)), RasterStyleUtilities.toColortableString(colorMap(read)));

        RasterStyleUtilities.setGrayChannel(read, -1);
        assertEquals(-1, RasterStyleUtilities.getGrayChannel(read));
    }

    public void testGrayChannelDrawsTheSelectedBand() throws Exception {
        // three bands of constant values: 10, 50 and 90
        int size = 4;
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, size, size, 3, null);
        for( int band = 0; band < 3; band++ ) {
            for( int y = 0; y < size; y++ ) {
                for( int x = 0; x < size; x++ ) {
                    raster.setSample(x, y, band, 10 + 40 * band);
                }
            }
        }
        ReferencedEnvelope envelope = new ReferencedEnvelope(0, size, 0, size, DefaultGeographicCRS.WGS84);
        GridCoverage2D coverage = new GridCoverageFactory().create("bands", raster, envelope);

        /*
         * drawn from a GeoTIFF reader, as the raster layers do: the band select of an in memory
         * multi band coverage fails in ImageN
         */
        File tiff = File.createTempFile("hm_graychannel_", ".tif");
        GeoTiffReader reader = null;
        try {
            GeoTiffWriter writer = new GeoTiffWriter(tiff);
            writer.write(coverage, null);
            writer.dispose();
            reader = new GeoTiffReader(tiff);

            String table = "0 0 0\n100 100 100\n";
            for( int band = 0; band < 3; band++ ) {
                Style style = RasterStyleUtilities.createStyleForColortableString(table, 0, 100, null, 1.0);
                RasterStyleUtilities.setGrayChannel(style, band);
                int expected = 10 + 40 * band;
                Color drawn = new Color(draw(reader, style, envelope).getRGB(size / 2, size / 2));
                assertEquals("band " + band, expected, drawn.getRed(), 2);
            }
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            tiff.delete();
        }
    }

    private static BufferedImage draw( GeoTiffReader reader, Style style, ReferencedEnvelope envelope ) {
        MapContent mapContent = new MapContent();
        try {
            mapContent.addLayer(new GridReaderLayer(reader, style));
            StreamingRenderer renderer = new StreamingRenderer();
            renderer.setMapContent(mapContent);
            int size = (int) envelope.getWidth();
            BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = image.createGraphics();
            renderer.paint(g, new Rectangle(0, 0, size, size), envelope);
            g.dispose();
            return image;
        } finally {
            mapContent.dispose();
        }
    }

    public void testParseValueClasses() throws Exception {
        // the text of the style panel, with labels holding commas and colons
        List<ValueClass> classes = RasterStyleUtilities.parseValueClasses("# comment\n1 255 0 0 Forest, mixed\n2 0 0 255\n3 0 255 0 Note: wet\n");
        assertEquals(List.of(new ValueClass(1, Color.RED, "Forest, mixed"), new ValueClass(2, Color.BLUE, null),
                new ValueClass(3, Color.GREEN, "Note: wet")), classes);

        // csv with a header, labels only and colors as hex or components
        classes = RasterStyleUtilities.parseValueClasses("value,label\n10,Urban\n20,\"Water, lakes\",#0000ff\n30,0,255,0,Grass\n");
        assertEquals(List.of(new ValueClass(10, null, "Urban"), new ValueClass(20, Color.BLUE, "Water, lakes"),
                new ValueClass(30, Color.GREEN, "Grass")), classes);

        // semicolons and tabs
        classes = RasterStyleUtilities.parseValueClasses("1;Rock\n2\tIce sheet\n");
        assertEquals(List.of(new ValueClass(1, null, "Rock"), new ValueClass(2, null, "Ice sheet")), classes);

        // properties
        classes = RasterStyleUtilities.parseValueClasses("! comment\n1=Forest\n2 = #ff0000 Burnt area\n3: Bare\n");
        assertEquals(List.of(new ValueClass(1, null, "Forest"), new ValueClass(2, Color.RED, "Burnt area"),
                new ValueClass(3, null, "Bare")), classes);

        try {
            RasterStyleUtilities.parseValueClasses("1 255 0 0\nnot a value\n");
            fail("only the first line can be a header");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }

    public void testUniqueValuesRoundTrip() throws Exception {
        List<ValueClass> classes = List.of(new ValueClass(1, Color.RED, "Forest"), new ValueClass(5, Color.BLUE, null),
                new ValueClass(7.5, Color.GREEN, "Wet, meadow"));
        Style style = RasterStyleUtilities.createStyleForUniqueValues(classes, 1.0);
        ColorMap map = colorMap(style);
        assertTrue(RasterStyleUtilities.isUniqueValues(map));
        List<ValueClass> read = RasterStyleUtilities.getValueClasses(map);
        assertEquals(classes, read);
        String text = RasterStyleUtilities.toUniqueValuesString(read);
        assertEquals("1 255 0 0 Forest\n5 0 0 255\n7.5 0 255 0 Wet, meadow\n", text);
        assertEquals(classes, RasterStyleUtilities.parseValueClasses(text));

        assertFalse(RasterStyleUtilities.isUniqueValues(colorMap(RasterStyleUtilities.createStyleForColortable(
                EColorTables.elev.name(), 0, 100, 1.0))));
    }

    public void testClassColors() throws Exception {
        String table = "255 0 0\n0 255 0\n0 0 255\n";
        // fewer classes than colors: the colors of the table, spread
        assertEquals(List.of(Color.RED, Color.BLUE), List.of(RasterStyleUtilities.getClassColors(table, 2)));
        // more classes: interpolated
        Color[] colors = RasterStyleUtilities.getClassColors(table, 5);
        assertEquals(Color.RED, colors[0]);
        assertEquals(new Color(128, 128, 0), colors[1]);
        assertEquals(Color.GREEN, colors[2]);
        assertEquals(Color.BLUE, colors[4]);
    }

    public void testUniqueValuesDrawing() throws Exception {
        // classes 1, 2 and 3 in columns, 3 is not in the style
        int size = 3;
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, size, size, 1, null);
        for( int y = 0; y < size; y++ ) {
            for( int x = 0; x < size; x++ ) {
                raster.setSample(x, y, 0, x + 1);
            }
        }
        ReferencedEnvelope envelope = new ReferencedEnvelope(0, size, 0, size, DefaultGeographicCRS.WGS84);
        GridCoverage2D coverage = new GridCoverageFactory().create("classes", raster, envelope);
        File tiff = File.createTempFile("hm_uniquevalues_", ".tif");
        GeoTiffReader reader = null;
        try {
            GeoTiffWriter writer = new GeoTiffWriter(tiff);
            writer.write(coverage, null);
            writer.dispose();
            reader = new GeoTiffReader(tiff);
            Style style = RasterStyleUtilities
                    .createStyleForUniqueValues(RasterStyleUtilities.parseValueClasses("1 255 0 0 one\n2 0 0 255 two\n"), 1.0);
            BufferedImage image = draw(reader, style, envelope);
            assertEquals(Color.RED, new Color(image.getRGB(0, 1)));
            assertEquals(Color.BLUE, new Color(image.getRGB(1, 1)));
            assertEquals("not listed, not drawn", 0, new Color(image.getRGB(2, 1), true).getAlpha());
        } finally {
            if (reader != null) {
                reader.dispose();
            }
            tiff.delete();
        }
    }

    public void testRasterUniqueValues() throws Exception {
        int size = 4;
        WritableRaster raster = RasterFactory.createBandedRaster(DataBuffer.TYPE_FLOAT, size, size, 1, null);
        for( int y = 0; y < size; y++ ) {
            for( int x = 0; x < size; x++ ) {
                raster.setSample(x, y, 0, x == 0 ? -9999 : x * 10);
            }
        }
        ReferencedEnvelope envelope = new ReferencedEnvelope(0, size, 0, size, DefaultGeographicCRS.WGS84);
        GridCoverage2D coverage = new GridCoverageFactory().create("values", raster, envelope);
        try (HMRaster hmRaster = HMRaster.fromGridCoverage(coverage)) {
            assertTrue(Arrays.equals(new double[]{10, 20, 30}, hmRaster.getUniqueValues(10)));
            // more than the given count: the scan stops with one value more
            assertEquals(3, hmRaster.getUniqueValues(2).length);
        }
    }

    public void testTooFewColors() throws Exception {
        try {
            RasterStyleUtilities.createStyleForColortableString("255 0 0\n", 0, 1, null, 1.0);
            fail("a single color is not a colortable");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
