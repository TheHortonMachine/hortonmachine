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
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * An utilities class for handling colors.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.4
 */
public class ColorUtilities {

    private static final Map<String, Color> NAMED_COLORS = new HashMap<>();

    static {
        registerColorConstants();
        NAMED_COLORS.put("grey", Color.GRAY);
        NAMED_COLORS.put("darkgrey", Color.DARK_GRAY);
        NAMED_COLORS.put("lightgrey", Color.LIGHT_GRAY);
    }

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
     * Convert a color definition to {@link Color}.
     *
     * <p>Supports hex strings, rgb/rgba strings in the form {@code r,g,b[,a]} or
     * {@code r g b [a]}, and well known named colors such as {@code green},
     * {@code red}, {@code darkGray} and aliases like {@code grey}.</p>
     *
     * @param colorString the color definition.
     * @return the parsed color.
     */
    public static Color fromString( String colorString ) {
        if (colorString == null) {
            throw new IllegalArgumentException("Color string can't be null.");
        }
        String normalized = colorString.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Color string can't be empty.");
        }

        if (normalized.startsWith("#") || normalized.matches("(?i)[0-9a-f]{1,6}")) {
            return fromHex(normalized);
        }
        if (normalized.contains(",") || normalized.matches(".*\\d+\\s+\\d+.*")) {
            return colorFromRbgString(normalized);
        }

        String key = normalized.toLowerCase(Locale.ROOT).replaceAll("[\\s_-]+", "");
        Color color = NAMED_COLORS.get(key);
        if (color != null) {
            return color;
        }

        throw new IllegalArgumentException("Unsupported color: " + colorString);
    }

    /**
     * Convert a color definition to a normalized hex representation.
     *
     * @param colorString the color definition.
     * @return the normalized hex string.
     */
    public static String asHex( String colorString ) {
        return asHex(fromString(colorString));
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

    private static void registerColorConstants() {
        for( Field field : Color.class.getFields() ) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != Color.class) {
                continue;
            }
            try {
                Color color = (Color) field.get(null);
                NAMED_COLORS.put(field.getName().toLowerCase(Locale.ROOT), color);
            } catch (IllegalAccessException e) {
                // Ignore inaccessible constants and continue with the remaining ones.
            }
        }
    }

}
