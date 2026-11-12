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
package org.hortonmachine.gears.utils.colors;

import java.awt.Color;
import java.util.HashSet;
import java.util.Set;

/**
 * Palettes of distinct colors for categories, as the unique values of a field, whose order has
 * no meaning: unlike the color tables of {@link EColorTables}, made for continuous values,
 * neighbouring colors are as different as possible.
 *
 * <p>The palettes are the qualitative ones of ColorBrewer (Cynthia Brewer, colorbrewer2.org)
 * and of Tableau, and a generated one for any number of categories. When more colors are asked
 * than a palette has, the palette is continued with generated distinct colors, not with shades
 * between its colors, which would be hard to tell apart.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public enum ECategoricalPalette {

    PAIRED("Paired", ColorBrewer.twelveClassPaired), //
    SET1("Set1", "#e41a1c", "#377eb8", "#4daf4a", "#984ea3", "#ff7f00", "#ffff33", "#a65628", "#f781bf", "#999999"), //
    SET2("Set2", "#66c2a5", "#fc8d62", "#8da0cb", "#e78ac3", "#a6d854", "#ffd92f", "#e5c494", "#b3b3b3"), //
    SET3("Set3", ColorBrewer.twelveClassSet3), //
    DARK2("Dark2", "#1b9e77", "#d95f02", "#7570b3", "#e7298a", "#66a61e", "#e6ab02", "#a6761d", "#666666"), //
    ACCENT("Accent", "#7fc97f", "#beaed4", "#fdc086", "#ffff99", "#386cb0", "#f0027f", "#bf5b17", "#666666"), //
    PASTEL1("Pastel1", "#fbb4ae", "#b3cde3", "#ccebc5", "#decbe4", "#fed9a6", "#ffffcc", "#e5d8bd", "#fddaec", "#f2f2f2"), //
    PASTEL2("Pastel2", "#b3e2cd", "#fdcdac", "#cbd5e8", "#f4cae4", "#e6f5c9", "#fff2ae", "#f1e2cc", "#cccccc"), //
    TABLEAU10("Tableau 10", "#4e79a7", "#f28e2b", "#e15759", "#76b7b2", "#59a14f", "#edc948", "#b07aa1", "#ff9da7", "#9c755f",
            "#bab0ac"), //
    TABLEAU20("Tableau 20", "#1f77b4", "#aec7e8", "#ff7f0e", "#ffbb78", "#2ca02c", "#98df8a", "#d62728", "#ff9896", "#9467bd",
            "#c5b0d5", "#8c564b", "#c49c94", "#e377c2", "#f7b6d2", "#7f7f7f", "#c7c7c7", "#bcbd22", "#dbdb8d", "#17becf",
            "#9edae5"), //
    MAIN("Main colors", ColorBrewer.mainColors), //
    /** Generated for any number of categories. */
    DISTINCT("Distinct (many values)");

    /** The golden ratio conjugate: hues stepped by it never repeat and stay spread. */
    private static final double GOLDEN_RATIO_CONJUGATE = 0.618033988749895;

    /** Saturation and brightness taken in turn, so that also close hues differ. */
    private static final float[][] SATURATION_BRIGHTNESS = {{0.70f, 0.95f}, {0.90f, 0.70f}, {0.45f, 0.90f}, {0.85f, 0.50f}};

    /** The change of brightness that makes a generated color differ from one already given. */
    private static final float BRIGHTNESS_NUDGE = 0.012f;

    private final String label;
    private final String[] hexColors;

    private ECategoricalPalette( String label, String... hexColors ) {
        this.label = label;
        this.hexColors = hexColors;
    }

    /**
     * @return the name to show.
     */
    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }

    /**
     * @param label a name, as given by {@link #getLabel()}.
     * @return the palette with that name, <code>null</code> if there is none.
     */
    public static ECategoricalPalette forLabel( String label ) {
        for( ECategoricalPalette palette : values() ) {
            if (palette.label.equals(label)) {
                return palette;
            }
        }
        return null;
    }

    /**
     * @param num the number of colors.
     * @return the colors: those of the palette, followed by generated distinct ones if more
     *          are needed.
     */
    public Color[] getColors( int num ) {
        Color[] colors = new Color[Math.max(0, num)];
        int fixed = Math.min(colors.length, hexColors.length);
        for( int i = 0; i < fixed; i++ ) {
            colors[i] = ColorUtilities.fromHex(hexColors[i]);
        }
        if (fixed < colors.length) {
            Color[] generated = generate(colors.length - fixed, fixed, saturationBrightness());
            System.arraycopy(generated, 0, colors, fixed, generated.length);
        }
        return colors;
    }

    /**
     * @return the saturations and brightnesses of the colors generated after those of the
     *          palette: around their averages, so that the colors added look like the palette,
     *          as pastel or dark.
     */
    private float[][] saturationBrightness() {
        if (hexColors.length == 0) {
            return SATURATION_BRIGHTNESS;
        }
        float saturation = 0;
        float brightness = 0;
        for( String hex : hexColors ) {
            Color color = ColorUtilities.fromHex(hex);
            float[] hsb = Color.RGBtoHSB(color.getRed(), color.getGreen(), color.getBlue(), null);
            saturation += hsb[1];
            brightness += hsb[2];
        }
        float s = saturation / hexColors.length;
        float b = brightness / hexColors.length;
        return new float[][]{{s, b}, {clamp(s + 0.2f), clamp(b - 0.2f)}, {clamp(s - 0.15f), clamp(b + 0.05f)},
                {clamp(s + 0.1f), clamp(b - 0.35f)}};
    }

    private static float clamp( float value ) {
        return Math.max(0.2f, Math.min(1f, value));
    }

    /**
     * Generate distinct colors for any number of categories: the hues are stepped by the golden
     * ratio, which keeps them spread however many they are, and the saturation and brightness
     * change in turn. With many colors, a color equal to one already given is made a bit darker
     * or lighter, so that all are different.
     *
     * @param num the number of colors.
     * @param start the index of the first color in the sequence, to continue another palette.
     * @return the colors.
     */
    public static Color[] getDistinctColors( int num, int start ) {
        return generate(num, start, SATURATION_BRIGHTNESS);
    }

    /**
     * Generate distinct colors, as {@link #getDistinctColors(int, int)}, with the saturations
     * and brightnesses given in turn.
     */
    private static Color[] generate( int num, int start, float[][] saturationBrightness ) {
        Color[] colors = new Color[Math.max(0, num)];
        Set<Integer> given = new HashSet<>();
        // a start that gives a pleasant first color, not a pure red
        double hue = 0.08;
        for( int i = 0; i < start; i++ ) {
            hue = (hue + GOLDEN_RATIO_CONJUGATE) % 1.0;
        }
        for( int i = 0; i < colors.length; i++ ) {
            float[] sb = saturationBrightness[(start + i) % saturationBrightness.length];
            Color color = Color.getHSBColor((float) hue, sb[0], sb[1]);
            // darker and lighter in turn, by growing steps, until the color is a new one
            for( int k = 1; !given.add(color.getRGB()) && k < 200; k++ ) {
                float step = BRIGHTNESS_NUDGE * ((k + 1) / 2) * (k % 2 == 0 ? 1 : -1);
                float brightness = Math.max(0.2f, Math.min(1f, sb[1] + step));
                color = Color.getHSBColor((float) hue, sb[0], brightness);
            }
            colors[i] = color;
            hue = (hue + GOLDEN_RATIO_CONJUGATE) % 1.0;
        }
        return colors;
    }

    /**
     * Take colors along a color table, as for categories having an order, as classes of a
     * number.
     *
     * @param colorTableName the name of one of the {@link EColorTables}.
     * @param num the number of colors.
     * @return the colors, from the start to the end of the table.
     */
    public static Color[] getRampColors( String colorTableName, int num ) {
        Color[] colors = new Color[Math.max(0, num)];
        if (colors.length == 0) {
            return colors;
        }
        ColorInterpolator interpolator = new ColorInterpolator(colorTableName, 0, Math.max(1, colors.length - 1), null);
        for( int i = 0; i < colors.length; i++ ) {
            colors[i] = interpolator.getColorFor(colors.length == 1 ? 0.5 : i);
        }
        return colors;
    }
}
