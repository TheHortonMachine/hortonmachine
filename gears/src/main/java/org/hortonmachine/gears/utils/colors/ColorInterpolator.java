/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
import java.util.ArrayList;
import java.util.List;

/**
 * A color interpolation helper class.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ColorInterpolator {

    private Color[] colors;
    private double[] values;
    private double min;
    private double max;

    public ColorInterpolator( Color[] colors, double[] values, int alpha ) {
        this.colors = colors;
        this.values = values;
        min = values[0];
        max = values[values.length - 1];
    }

    public ColorInterpolator( String colorTableName, double min, double max, Integer alpha ) {
        this.min = min;
        this.max = max;
        createColortableArrays(colorTableName, min, max, alpha);
    }

    /**
     * Get the color of the defined table by its value.
     * 
     * @param value the value.
     * @return the interpolated color.
     */
    public Color getColorFor( double value ) {
        if (value <= min) {
            return colors[0];
        } else if (value >= max) {
            return colors[colors.length - 1];
        } else {
            for( int i = 1; i < colors.length; i++ ) {
                double v1 = values[i - 1];
                double v2 = values[i];
                if (value < v2) {

                    double v = (value - v1) / (v2 - v1);
                    Color interpolateColor = interpolateColor(colors[i - 1], colors[i], (float) v);
                    return interpolateColor;
                }
            }
            return colors[colors.length - 1];
        }
    }

    private void createColortableArrays( String colorTableName, double min, double max, Integer alpha ) {
        int a = 255;
        if (alpha != null) {
            a = alpha;
        }

        List<Color> colorList = new ArrayList<Color>();
        String tableString = new DefaultTables().getTableString(colorTableName);
        String[] split = tableString.split("\n");

        int length = split.length - 1;
        double delta = (max - min) / length;
        values = new double[split.length];
        for( int i = 0; i < split.length; i++ ) {
            values[i] = min + i * delta;
        }

        List<Double> newValues = null; // if necessary
        for( String line : split ) {
            if (line.startsWith("#")) { //$NON-NLS-1$
                continue;
            }
            String[] lineSplit = line.trim().split("\\s+"); //$NON-NLS-1$

            if (lineSplit.length == 3) {
                int r = Integer.parseInt(lineSplit[0]);
                int g = Integer.parseInt(lineSplit[1]);
                int b = Integer.parseInt(lineSplit[2]);

                colorList.add(new Color(r, g, b, a));
            } else if (lineSplit.length == 8) {
                if (newValues == null) {
                    newValues = new ArrayList<Double>();
                }

                // also value are provided, rewrite input values
                double v1 = Double.parseDouble(lineSplit[0]);
                int r1 = Integer.parseInt(lineSplit[1]);
                int g1 = Integer.parseInt(lineSplit[2]);
                int b1 = Integer.parseInt(lineSplit[3]);

                colorList.add(new Color(r1, g1, b1, a));
                newValues.add(v1);

                double v2 = Double.parseDouble(lineSplit[4]);
                int r2 = Integer.parseInt(lineSplit[5]);
                int g2 = Integer.parseInt(lineSplit[6]);
                int b2 = Integer.parseInt(lineSplit[7]);

                colorList.add(new Color(r2, g2, b2, a));
                newValues.add(v2);
            } else if (lineSplit.length == 4) {
                if (newValues == null) {
                    newValues = new ArrayList<Double>();
                }

                // also value are provided, rewrite input values
                double v1 = Double.parseDouble(lineSplit[0]);
                int r1 = Integer.parseInt(lineSplit[1]);
                int g1 = Integer.parseInt(lineSplit[2]);
                int b1 = Integer.parseInt(lineSplit[3]);

                colorList.add(new Color(r1, g1, b1, a));
                newValues.add(v1);

            }
        }

        colors = colorList.toArray(new Color[0]);
        if (newValues != null) {
            // redefine values
            values = new double[newValues.size()];
            for( int i = 0; i < newValues.size(); i++ ) {
                values[i] = newValues.get(i);
            }
        }
    }

    /**
     * Interpolate a color at a given fraction between 0 and 1.
     * 
     * @param color1 start color.
     * @param color2 end color.
     * @param fraction the fraction to interpolate.
     * @return the new color.
     */
    public static Color interpolateColor( Color color1, Color color2, float fraction ) {
        float int2Float = 1f / 255f;
        fraction = Math.min(fraction, 1f);
        fraction = Math.max(fraction, 0f);

        float r1 = color1.getRed() * int2Float;
        float g1 = color1.getGreen() * int2Float;
        float b1 = color1.getBlue() * int2Float;
        float a1 = color1.getAlpha() * int2Float;

        float r2 = color2.getRed() * int2Float;
        float g2 = color2.getGreen() * int2Float;
        float b2 = color2.getBlue() * int2Float;
        float a2 = color2.getAlpha() * int2Float;

        float deltaR = r2 - r1;
        float deltaG = g2 - g1;
        float deltaB = b2 - b1;
        float deltaA = a2 - a1;

        float red = r1 + (deltaR * fraction);
        float green = g1 + (deltaG * fraction);
        float blue = b1 + (deltaB * fraction);
        float alpha = a1 + (deltaA * fraction);

        red = Math.min(red, 1f);
        red = Math.max(red, 0f);
        green = Math.min(green, 1f);
        green = Math.max(green, 0f);
        blue = Math.min(blue, 1f);
        blue = Math.max(blue, 0f);
        alpha = Math.min(alpha, 1f);
        alpha = Math.max(alpha, 0f);

        return new Color(red, green, blue, alpha);
    }

}
