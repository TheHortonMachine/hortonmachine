/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.gears.utils.colors;

import java.awt.Color;

/**
 * An utilities class for handling colors.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.4
 */
public class ColorUtilities {

    /**
     * Converts a color string.
     * 
     * @param rbgString the string in the form "r,g,b,a" as integer values between 0 and 255.
     * @return the {@link Color}.
     */
    public static Color colorFromRbgString( String rbgString ) {
        String[] split;
        if (rbgString.contains(",")) {
            split = rbgString.split(",");
        }else {
            split = rbgString.split(" ");
        }
        if (split.length < 3 || split.length > 4) {
            throw new IllegalArgumentException("Color string has to be of type r,g,b.");
        }
        int r = (int) Double.parseDouble(split[0].trim());
        int g = (int) Double.parseDouble(split[1].trim());
        int b = (int) Double.parseDouble(split[2].trim());
        Color c = null;
        if (split.length == 4) {
            // alpha
            int a = (int) Double.parseDouble(split[3].trim());
            c = new Color(r, g, b, a);
        } else {
            c = new Color(r, g, b);
        }

        return c;
    }

    /**
     * Convert a color to its hex representation.
     * 
     * @param color the color to convert.
     * @return the hex.
     */
    public static String asHexWithAlpha( Color color ) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();
        int a = color.getAlpha();

        String hex = String.format("#%02x%02x%02x%02x", r, g, b, a);
        return hex;
    }

    /**
     * Convert a color to its hex representation.
     * 
     * @param color the color to convert.
     * @return the hex.
     */
    public static String asHex( Color color ) {
        int r = color.getRed();
        int g = color.getGreen();
        int b = color.getBlue();

        String hex = String.format("#%02x%02x%02x", r, g, b);
        return hex;
    }

    /**
     * Convert hex color to Color.
     * 
     * @return the Color object. 
     */
    public static Color fromHex( String hex ) {
        if (hex.startsWith("#")) {
            hex = hex.substring(1);
        }
        int length = hex.length();
        int total = 6;
        if (length < total) {
            // we have a shortened version
            String token = hex;
            int tokenLength = token.length();
            for( int i = 0; i < total; i = i + tokenLength ) {
                hex += token;
            }
        }

        int index = 0;
        String r = hex.substring(index, index + 2);
        String g = hex.substring(index + 2, index + 4);
        String b = hex.substring(index + 4, index + total);
        return new Color(Integer.valueOf(r, 16), Integer.valueOf(g, 16), Integer.valueOf(b, 16));
    }

    /**
     * Add alpha to a color.
     * 
     * @param color the color to make transparent.
     * @return the new color.
     */
    public static Color makeTransparent( Color color, int alpha ) {
        Color transparentColor = new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
        return transparentColor;
    }

}
