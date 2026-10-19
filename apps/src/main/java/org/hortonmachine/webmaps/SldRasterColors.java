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
package org.hortonmachine.webmaps;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.DoubleToIntFunction;

import org.geotools.api.filter.expression.Expression;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.Symbolizer;

/**
 * The colors of the values of a single band raster as the color map of the raster symbolizer of a SLD gives them:
 * <ul>
 * <li>ramp: interpolated between the entries, the values beyond the first and last entry get their colors;</li>
 * <li>intervals: the color of the first entry whose quantity is larger than the value, none beyond the last;</li>
 * <li>values: the color of the entry of the same quantity, none for the others.</li>
 * </ul>
 * The opacity of the entries and of the symbolizer is kept; the values without color are transparent.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class SldRasterColors implements DoubleToIntFunction {
    private final int type;
    private final double[] quantities;
    private final int[] colors;

    private SldRasterColors( int type, double[] quantities, int[] colors ) {
        this.type = type;
        this.quantities = quantities;
        this.colors = colors;
    }

    /**
     * @param style a style, as read from a SLD.
     * @return the colors of the first raster symbolizer with a color map, or <code>null</code> if it has none.
     */
    public static SldRasterColors fromStyle( Style style ) {
        if (style == null) {
            return null;
        }
        for( FeatureTypeStyle featureTypeStyle : style.featureTypeStyles() ) {
            for( Rule rule : featureTypeStyle.rules() ) {
                for( Symbolizer symbolizer : rule.symbolizers() ) {
                    if (symbolizer instanceof RasterSymbolizer raster && raster.getColorMap() != null) {
                        SldRasterColors colors = fromColorMap(raster.getColorMap(), number(raster.getOpacity(), 1.0));
                        if (colors != null) {
                            return colors;
                        }
                    }
                }
            }
        }
        return null;
    }

    private static SldRasterColors fromColorMap( ColorMap colorMap, double symbolizerOpacity ) {
        record Entry(double quantity, int argb) {
        }
        List<Entry> entries = new ArrayList<>();
        for( ColorMapEntry entry : colorMap.getColorMapEntries() ) {
            Double quantity = entry.getQuantity() != null ? entry.getQuantity().evaluate(null, Double.class) : null;
            Color color = entry.getColor() != null ? entry.getColor().evaluate(null, Color.class) : null;
            if (quantity == null || color == null) {
                continue;
            }
            double opacity = number(entry.getOpacity(), 1.0) * symbolizerOpacity;
            int alpha = (int) Math.round(Math.max(0, Math.min(1, opacity)) * 255);
            entries.add(new Entry(quantity, (alpha << 24) | (color.getRGB() & 0xffffff)));
        }
        if (entries.isEmpty()) {
            return null;
        }
        entries.sort(Comparator.comparingDouble(Entry::quantity));
        double[] quantities = entries.stream().mapToDouble(Entry::quantity).toArray();
        int[] colors = entries.stream().mapToInt(Entry::argb).toArray();
        return new SldRasterColors(colorMap.getType(), quantities, colors);
    }

    private static double number( Expression expression, double defaultValue ) {
        if (expression == null) {
            return defaultValue;
        }
        Double value = expression.evaluate(null, Double.class);
        return value != null ? value : defaultValue;
    }

    /**
     * @return the argb color of a value, 0 (transparent) if the color map gives none.
     */
    @Override
    public int applyAsInt( double value ) {
        int last = quantities.length - 1;
        switch( type ) {
        case ColorMap.TYPE_VALUES:
            for( int i = 0; i <= last; i++ ) {
                if (quantities[i] == value) {
                    return colors[i];
                }
            }
            return 0;
        case ColorMap.TYPE_INTERVALS:
            for( int i = 0; i <= last; i++ ) {
                if (value < quantities[i]) {
                    return colors[i];
                }
            }
            return 0;
        default:
            if (value <= quantities[0]) {
                return colors[0];
            }
            if (value >= quantities[last]) {
                return colors[last];
            }
            int i = 1;
            while( value > quantities[i] ) {
                i++;
            }
            double f = (value - quantities[i - 1]) / (quantities[i] - quantities[i - 1]);
            return interpolate(colors[i - 1], colors[i], f);
        }
    }

    private static int interpolate( int from, int to, double f ) {
        int result = 0;
        for( int shift = 0; shift <= 24; shift += 8 ) {
            int a = (from >>> shift) & 0xff;
            int b = (to >>> shift) & 0xff;
            result |= ((int) Math.round(a + f * (b - a)) & 0xff) << shift;
        }
        return result;
    }
}
