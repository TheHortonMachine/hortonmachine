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
import java.util.List;

import org.geotools.api.style.ColorMap;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Style;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.colors.DefaultTables;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;

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

    public void testTooFewColors() throws Exception {
        try {
            RasterStyleUtilities.createStyleForColortableString("255 0 0\n", 0, 1, null, 1.0);
            fail("a single color is not a colortable");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
