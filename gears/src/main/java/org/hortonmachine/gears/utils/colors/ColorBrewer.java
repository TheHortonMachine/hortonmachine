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

    public static Color[] getPairedColors( int num ) {
        if (num <= twelveClassPaired.length) {
            Color[] colors = new Color[num];
            for( int i = 0; i < num; i++ ) {
                colors[i] = ColorUtilities.fromHex(twelveClassPaired[i]);
            }
            return colors;
        } else {
            Color[] allColors = new Color[twelveClassPaired.length];
            for( int i = 0; i < twelveClassPaired.length; i++ ) {
                allColors[i] = ColorUtilities.fromHex(twelveClassPaired[i]);
            }
            double[] values = new double[twelveClassPaired.length];
            double delta = num / (double) twelveClassPaired.length;
            for( int i = 0; i < twelveClassPaired.length; i++ ) {
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
