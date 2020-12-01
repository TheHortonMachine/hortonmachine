package org.hortonmachine.gears.utils.colors;

import java.awt.Color;

public class ColorBrewer {

    public static final String[] twelveClassPaired = {"#a6cee3", //
            "#1f78b4", //
            "#b2df8a", //
            "#33a02c", //
            "#fb9a99", //
            "#e31a1c", //
            "#fdbf6f", //
            "#ff7f00", //
            "#cab2d6", //
            "#6a3d9a", //
            "#b15928", //
            "#ffff99"//
    };
    public static final String[] twelveClassSet3 = {"#8dd3c7", //
            "#ffffb3", //
            "#bebada", //
            "#fb8072", //
            "#80b1d3", //
            "#fdb462", //
            "#b3de69", //
            "#fccde5", //
            "#d9d9d9", //
            "#bc80bd", //
            "#ccebc5", //
            "#ffed6f" //
    };
    public static final String[] mainColors = {"#FF0000", //
            "#00FF00", //
            "#0000FF", //
            "#FFFF00", //
            "#00FFFF", //
            "#FF00FF" //
    };

    public static Color[] getPairedColors( int num ) {
        return getColors(num, twelveClassPaired);
    }

    public static Color[] getSet3Colors( int num ) {
        return getColors(num, twelveClassSet3);
    }

    public static Color[] getMainColors( int num ) {
        return getColors(num, mainColors);
    }

    private static Color[] getColors( int num, String[] colorHexes ) {
        if (num <= colorHexes.length) {
            Color[] colors = new Color[num];
            for( int i = 0; i < num; i++ ) {
                colors[i] = ColorUtilities.fromHex(colorHexes[i]);
            }
            return colors;
        } else {
            Color[] allColors = new Color[colorHexes.length];
            for( int i = 0; i < colorHexes.length; i++ ) {
                allColors[i] = ColorUtilities.fromHex(colorHexes[i]);
            }
            double[] values = new double[colorHexes.length];
            double delta = num / (double) colorHexes.length;
            for( int i = 0; i < colorHexes.length; i++ ) {
                values[i] = i * delta;
            }

            Color[] colors = new Color[num];
            ColorInterpolator ci = new ColorInterpolator(allColors, values, 1);
            for( int i = 0; i < num; i++ ) {
                colors[i] = ci.getColorFor(i);
            }
            return colors;
        }
    }
}
