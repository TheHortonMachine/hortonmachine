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
import java.io.File;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.geotools.api.filter.expression.Expression;
import org.geotools.api.style.ChannelSelection;
import org.geotools.api.style.ColorMap;
import org.geotools.api.style.ColorMapEntry;
import org.geotools.api.style.ContrastEnhancement;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.SelectedChannelType;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyleFactory;
import org.geotools.api.style.Symbolizer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.SLD;
import org.geotools.styling.StyleBuilder;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;

/**
 * A class to help with raster styling. 
 * 
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class RasterStyleUtilities {

    private static StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);

    public static void dumpRasterStyle( String path, double min, double max, double[] values, Color[] colors, double opacity )
            throws Exception {
        String styleStr = createRasterStyleString(min, max, values, colors, opacity);
        FileUtilities.writeFile(styleStr, new File(path));
    }

    private static String createRasterStyleString( double min, double max, double[] values, Color[] colors, double opacity )
            throws Exception {
        Style newStyle = createRasterStyle(min, max, values, colors, opacity);
        String styleStr = SldUtilities.styleToString(newStyle);
        return styleStr;
    }

    /**
     * Create a style that gives each of the values its color, interpolating the colors in between.
     *
     * @param values the increasing values.
     * @param colors the colors of the values.
     * @param opacity the opacity.
     * @return the style.
     */
    public static Style createStyleForValues( double[] values, Color[] colors, double opacity ) {
        return createRasterStyle(values[0], values[values.length - 1], values, colors, opacity);
    }

    private static Style createRasterStyle( double min, double max, double[] values, Color[] colors, double opacity ) {
        StyleBuilder sB = new StyleBuilder(sf);
        RasterSymbolizer rasterSym = sf.createRasterSymbolizer();

        int colorsNum = colors.length;
        boolean hasAllValues = false;
        if (values != null) {
            // we take first and last and interpolate in the middle
            hasAllValues = true;
        }
        double interval = (max - min) / (colorsNum - 1);
        double runningValue = min;

        ColorMap colorMap = sf.createColorMap();

        // add -9999 as novalue first
        Expression whiteColorExpr = sB.colorExpression(Color.white);
        Expression novalExpr = sB.literalExpression(-9999);
        Expression zeroOpacityExpr = sB.literalExpression(0);
        ColorMapEntry novalueEntry = sf.createColorMapEntry();
        novalueEntry.setQuantity(novalExpr);
        novalueEntry.setColor(whiteColorExpr);
        novalueEntry.setOpacity(zeroOpacityExpr);
        colorMap.addColorMapEntry(novalueEntry);

        // add other stuff
        for( int i = 0; i < colors.length - 1; i++ ) {
            Color fromColor = colors[i];
            Color toColor = colors[i + 1];

            double start;
            double end;
            if (hasAllValues) {
                start = values[i];
                end = values[i + 1];
            } else {
                start = runningValue;
                runningValue = runningValue + interval;
                end = runningValue;
            }

            Expression opacityExpr = sB.literalExpression(opacity);

            if (i == 0) {
                Expression fromColorExpr = sB.colorExpression(fromColor);
                Expression fromExpr = sB.literalExpression(start);
                ColorMapEntry entry = sf.createColorMapEntry();
                entry.setQuantity(fromExpr);
                entry.setColor(fromColorExpr);
                entry.setOpacity(opacityExpr);
                colorMap.addColorMapEntry(entry);
            }

            if (!NumericsUtilities.dEq(start, end)) {
                Expression toColorExpr = sB.colorExpression(toColor);
                Expression toExpr = sB.literalExpression(end);
                ColorMapEntry entry = sf.createColorMapEntry();
                entry.setQuantity(toExpr);
                entry.setOpacity(opacityExpr);
                entry.setColor(toColorExpr);
                colorMap.addColorMapEntry(entry);
            }
            // i++;
        }

        rasterSym.setColorMap(colorMap);

        /*
         * set global transparency for the map
         */
        rasterSym.setOpacity(sB.literalExpression(opacity));

        Style newStyle = SLD.wrapSymbolizers(rasterSym);
        return newStyle;
    }

    public static String createQGISRasterStyle( String colorTableName, double min, double max, double[] values,
            int labelDecimals ) throws Exception {

        boolean isCategories = false;
        List<Color> colorList = new ArrayList<Color>();
        String tableString = new DefaultTables().getTableString(colorTableName);
        if (tableString == null) {
            return null;
        }
        String[] split = tableString.split("\n");
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

                colorList.add(new Color(r, g, b));
            } else if (lineSplit.length == 8) {
                if (newValues == null) {
                    newValues = new ArrayList<Double>();
                }

                // also value are provided, rewrite input values
                double v1 = Double.parseDouble(lineSplit[0]);
                int r1 = Integer.parseInt(lineSplit[1]);
                int g1 = Integer.parseInt(lineSplit[2]);
                int b1 = Integer.parseInt(lineSplit[3]);

                colorList.add(new Color(r1, g1, b1));
                newValues.add(v1);

                double v2 = Double.parseDouble(lineSplit[4]);
                int r2 = Integer.parseInt(lineSplit[5]);
                int g2 = Integer.parseInt(lineSplit[6]);
                int b2 = Integer.parseInt(lineSplit[7]);

                colorList.add(new Color(r2, g2, b2));
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

                colorList.add(new Color(r1, g1, b1));
                newValues.add(v1);

                isCategories = true;
            }
        }

        Color[] colorsArray = colorList.toArray(new Color[0]);
        if (newValues != null) {
            // redefine values
            values = new double[newValues.size()];
            for( int i = 0; i < newValues.size(); i++ ) {
                values[i] = newValues.get(i);
            }
        }

        if (isCategories) {
            return getQgisStyleCategories(min, max, values, colorsArray, labelDecimals);
        } else {
            return getQgisStyleContinuous(min, max, values, colorsArray, labelDecimals);
        }

    }

    private static String getQgisStyleCategories( double min, double max, double[] values, Color[] colors, int labelDecimals )
            throws Exception {

        String fPattern = "0.#################";
        if (labelDecimals >= 0) {
            fPattern = "0.";
            for( int i = 0; i < labelDecimals; i++ ) {
                fPattern += "#";
            }
        }
        DecimalFormat f = new DecimalFormat(fPattern);

        String ind = "\t";
        StringBuilder sb = new StringBuilder();
        sb.append("<qgis>\n");
        sb.append(ind).append("<pipe>\n");
        sb.append(ind).append(ind)
                .append("<rasterrenderer band=\"1\" type=\"paletted\" alphaBand=\"-1\" opacity=\"1\" nodataColor=\"\">\n");
        sb.append(ind).append(ind).append(ind).append("<colorPalette>\n");
        for( int i = 0; i < colors.length; i++ ) {
            double value = values[i];
            sb.append(ind).append(ind).append(ind).append(ind);

            String label = f.format(value);
            String color = ColorUtilities.asHex(colors[i]);

            // <paletteEntry value="0" alpha="255" color="#7e7fef" label="cat0"/>
            sb.append("<paletteEntry value=\"" + value + "\" alpha=\"255\" color=\"" + color + "\" label=\"" + label + "\"/>\n");
        }
        sb.append(ind).append(ind).append(ind).append("</colorPalette>\n");
        sb.append(ind).append(ind).append("</rasterrenderer>\n");
        sb.append(ind).append("</pipe>\n");
        sb.append("</qgis>\n");

        return sb.toString();
    }

    private static String getQgisStyleContinuous( double min, double max, double[] values, Color[] colors, int labelDecimals )
            throws Exception {

        double delta = (max - min) / (colors.length - 1);
        if (values == null) {
            values = new double[colors.length];

            for( int i = 0; i < values.length; i++ ) {
                values[i] = min + delta * i;
            }
        }

        String fPattern = "0.#################";
        if (labelDecimals >= 0) {
            fPattern = "0.";
            for( int i = 0; i < labelDecimals; i++ ) {
                fPattern += "#";
            }
        }
        DecimalFormat f = new DecimalFormat(fPattern);

        String ind = "\t";
        StringBuilder sb = new StringBuilder();
        sb.append("<qgis>\n");
        sb.append(ind).append("<pipe>\n");
        sb.append(ind).append(ind) //
                .append("<rasterrenderer band=\"1\" type=\"singlebandpseudocolor\"")//
                .append(" classificationMax=\"").append(max).append("\"")//
                .append(" classificationMin=\"").append(min).append("\"")//
                .append(" alphaBand=\"-1\" opacity=\"1\" nodataColor=\"\">\n");

        sb.append(ind).append(ind).append(ind).append("<rastershader>\n");
        sb.append(ind).append(ind).append(ind).append(ind).append("<colorrampshader ")//
                .append(" minimumValue=\"").append(min).append("\"")//
                .append(" maximumValue=\"").append(max).append("\"")//
                .append(" colorRampType=\"INTERPOLATED\"")//
                .append(" classificationMode=\"1\"")//
                .append(" clip=\"0\"")//
                .append(">\n");
        for( int i = 0; i < values.length; i++ ) {
            sb.append(ind).append(ind).append(ind).append(ind).append(ind);

            String label = f.format(values[i]);
            String color = ColorUtilities.asHex(colors[i]);

            // <item color="#d7191c" value="846.487670898438" label="846,4877" alpha="255"/>
            sb.append("<item color=\"" + color + "\" value=\"" + values[i] + "\" label=\"" + label + "\" alpha=\"255\"/>\n");
        }
        sb.append(ind).append(ind).append(ind).append(ind).append("</colorrampshader>\n");
        sb.append(ind).append(ind).append(ind).append("</rastershader>\n");

        sb.append(ind).append(ind).append("</rasterrenderer>\n");
        sb.append(ind).append("</pipe>\n");
        sb.append("</qgis>\n");

        return sb.toString();
    }

    public static Style createDefaultRasterStyle() {
        RasterSymbolizer rasterSym = sf.createRasterSymbolizer();
        Style newStyle = SLD.wrapSymbolizers(rasterSym);
        return newStyle;
    }

    public static Style createStyleForColortable( String colorTableName, double min, double max, double opacity )
            throws Exception {
        return createStyleForColortable(colorTableName, min, max, null, opacity);
    }

    /**
     * Create style for a given colortable.
     *
     *
     * @param colorTableName the name of the colortable (has to be available in {@link org.hortonmachine.gears.utils.colors.DefaultTables).
     * @param min
     * @param max
     * @param values
     * @param opacity
     * @return the style.
     * @throws Exception
     */
    public static Style createStyleForColortable( String colorTableName, double min, double max, double[] values, double opacity )
            throws Exception {
        String tableString = new DefaultTables().getTableString(colorTableName);
        if (tableString == null) {
            return null;
        }
        return createStyleForColortableString(tableString, min, max, values, opacity);
    }

    /**
     * Create a style for the text of a colortable, as given by {@link DefaultTables#getTableString(String)}.
     *
     * <p>The lines of the text are colors as <code>r g b</code>, spread evenly between min and
     * max, or colors with their value, as <code>value r g b</code> or
     * <code>value1 r1 g1 b1 value2 r2 g2 b2</code>, which ignore min and max. Lines starting
     * with # are comments.</p>
     *
     * @param tableString the text of the colortable.
     * @param min the value of the first color.
     * @param max the value of the last color.
     * @param values the values of the colors, <code>null</code> to spread them between min and max.
     * @param opacity the opacity.
     * @return the style.
     * @throws Exception if the text is not a valid colortable.
     */
    public static Style createStyleForColortableString( String tableString, double min, double max, double[] values,
            double opacity ) throws Exception {
        List<Color> colorList = new ArrayList<Color>();
        String[] split = tableString.split("\n");
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

                colorList.add(new Color(r, g, b));
            } else if (lineSplit.length == 8) {
                if (newValues == null) {
                    newValues = new ArrayList<Double>();
                }

                // also value are provided, rewrite input values
                double v1 = Double.parseDouble(lineSplit[0]);
                int r1 = Integer.parseInt(lineSplit[1]);
                int g1 = Integer.parseInt(lineSplit[2]);
                int b1 = Integer.parseInt(lineSplit[3]);

                colorList.add(new Color(r1, g1, b1));
                newValues.add(v1);

                double v2 = Double.parseDouble(lineSplit[4]);
                int r2 = Integer.parseInt(lineSplit[5]);
                int g2 = Integer.parseInt(lineSplit[6]);
                int b2 = Integer.parseInt(lineSplit[7]);

                colorList.add(new Color(r2, g2, b2));
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

                colorList.add(new Color(r1, g1, b1));
                newValues.add(v1);

            }
        }

        Color[] colorsArray = colorList.toArray(new Color[0]);
        if (colorsArray.length < 2) {
            throw new IllegalArgumentException("A colortable needs at least two colors.");
        }
        if (newValues != null) {
            // redefine values
            values = new double[newValues.size()];
            for( int i = 0; i < newValues.size(); i++ ) {
                values[i] = newValues.get(i);
            }
        }

        return createRasterStyle(min, max, values, colorsArray, opacity);
    }

    /**
     * A class of a unique values style: a value with its color and label.
     *
     * @param value the value of the cells of the class.
     * @param color the color, <code>null</code> if not given yet.
     * @param label the label, as the name of a land cover class, <code>null</code> if none.
     */
    public record ValueClass( double value, Color color, String label ) {
        /**
         * @return the class with another color.
         */
        public ValueClass withColor( Color newColor ) {
            return new ValueClass(value, newColor, label);
        }
    }

    /**
     * Create a style that gives each of some values its own color, as the classes of a land cover:
     * a color map of type values, without interpolation, the other values are not drawn. The labels
     * of the classes are the labels of the entries of the color map.
     *
     * @param classes the classes, all with a color.
     * @param opacity the opacity.
     * @return the style.
     */
    public static Style createStyleForUniqueValues( List<ValueClass> classes, double opacity ) {
        if (classes.isEmpty()) {
            throw new IllegalArgumentException("The unique values need at least one value with its color.");
        }
        StyleBuilder sB = new StyleBuilder(sf);
        ColorMap colorMap = sf.createColorMap();
        colorMap.setType(ColorMap.TYPE_VALUES);
        Expression opacityExpr = sB.literalExpression(opacity);
        for( ValueClass valueClass : classes ) {
            if (valueClass.color() == null) {
                throw new IllegalArgumentException("The value " + valueClass.value() + " has no color.");
            }
            ColorMapEntry entry = sf.createColorMapEntry();
            entry.setQuantity(sB.literalExpression(valueClass.value()));
            entry.setColor(sB.colorExpression(valueClass.color()));
            entry.setOpacity(opacityExpr);
            if (valueClass.label() != null && !valueClass.label().isBlank()) {
                entry.setLabel(valueClass.label());
            }
            colorMap.addColorMapEntry(entry);
        }
        RasterSymbolizer rasterSym = sf.createRasterSymbolizer();
        rasterSym.setColorMap(colorMap);
        rasterSym.setOpacity(sB.literalExpression(opacity));
        return SLD.wrapSymbolizers(rasterSym);
    }

    /**
     * Read the classes of a unique values style from a text, one class per line, as written by
     * {@link #toUniqueValuesString(List)}, or as a CSV or a properties file of the classes:
     * <ul>
     * <li><code>value r g b label</code>, the label being optional;</li>
     * <li>fields separated by tabs, semicolons or commas, as <code>value,label</code>,
     * <code>value,r,g,b,label</code> or <code>value,label,#rrggbb</code>: the first field is the
     * value, the color is a <code>#rrggbb</code> field or three integer fields in a row, the other
     * fields make the label; a first line that is not a value, as a header, is skipped;</li>
     * <li><code>value=label</code> or <code>value: label</code>, as a properties file, the label
     * optionally starting with a color.</li>
     * </ul>
     * The empty lines and the lines starting with # or ! are skipped.
     *
     * @param text the text.
     * @return the classes in the order of the text, without color if it is not given.
     * @throws IllegalArgumentException if a line, besides a header, does not start with a value.
     */
    public static List<ValueClass> parseValueClasses( String text ) {
        List<ValueClass> classes = new ArrayList<>();
        boolean first = true;
        for( String line : text.split("\\r?\\n") ) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("!")) {
                continue;
            }
            List<String> fields = new ArrayList<>();
            String delimiter = fieldDelimiter(trimmed);
            if (delimiter != null) {
                fields.addAll(splitCsv(trimmed, delimiter.charAt(0)));
            } else {
                int separator = indexOfKeySeparator(trimmed);
                if (separator > 0) {
                    fields.add(trimmed.substring(0, separator).trim());
                    trimmed = trimmed.substring(separator + 1).trim();
                } else {
                    String[] split = trimmed.split("\\s+", 2);
                    fields.add(split[0]);
                    trimmed = split.length > 1 ? split[1] : "";
                }
                // the rest: an optional color and the label
                for( String token : trimmed.split("\\s+") ) {
                    if (!token.isEmpty()) {
                        fields.add(token);
                    }
                }
            }
            double value;
            try {
                value = Double.parseDouble(fields.get(0));
            } catch (NumberFormatException e) {
                if (first) {
                    // a header
                    first = false;
                    continue;
                }
                throw new IllegalArgumentException("The line does not start with a value: " + line.trim());
            }
            first = false;
            Color color = null;
            int colorStart = -1;
            int colorLength = 0;
            for( int i = 1; i < fields.size() && color == null; i++ ) {
                String field = fields.get(i);
                if (field.matches("#[0-9a-fA-F]{6}")) {
                    color = Color.decode(field);
                    colorStart = i;
                    colorLength = 1;
                } else if (i + 2 < fields.size() && isColorComponent(field) && isColorComponent(fields.get(i + 1))
                        && isColorComponent(fields.get(i + 2))) {
                    color = new Color(Integer.parseInt(field), Integer.parseInt(fields.get(i + 1)),
                            Integer.parseInt(fields.get(i + 2)));
                    colorStart = i;
                    colorLength = 3;
                }
            }
            StringBuilder label = new StringBuilder();
            for( int i = 1; i < fields.size(); i++ ) {
                if (i >= colorStart && i < colorStart + colorLength || fields.get(i).isEmpty()) {
                    continue;
                }
                if (label.length() > 0) {
                    label.append(' ');
                }
                label.append(fields.get(i));
            }
            classes.add(new ValueClass(value, color, label.length() == 0 ? null : label.toString()));
        }
        return classes;
    }

    /**
     * @return the tab, semicolon or comma separating the fields of a CSV line, <code>null</code>
     *          if the line is not one: the first field, the value, is a single word, so that the
     *          commas in a label of a <code>value r g b label</code> line are not delimiters.
     */
    private static String fieldDelimiter( String line ) {
        for( String delimiter : new String[]{"\t", ";", ","} ) {
            int index = line.indexOf(delimiter);
            if (index > 0 && !line.substring(0, index).trim().contains(" ")) {
                return delimiter;
            }
        }
        return null;
    }

    /**
     * @return the index of the = or : separating the key of a properties line, -1 if none: the
     *          key, the value, is a single word.
     */
    private static int indexOfKeySeparator( String line ) {
        int equals = line.indexOf('=');
        int colon = line.indexOf(':');
        int index = equals < 0 ? colon : colon < 0 ? equals : Math.min(equals, colon);
        if (index > 0 && line.substring(0, index).trim().contains(" ")) {
            return -1;
        }
        return index;
    }

    private static boolean isColorComponent( String field ) {
        if (!field.matches("\\d{1,3}")) {
            return false;
        }
        return Integer.parseInt(field) <= 255;
    }

    /**
     * @return the trimmed fields of a CSV line, the delimiters inside double quotes being part of
     *          the field, the quotes removed.
     */
    private static List<String> splitCsv( String line, char delimiter ) {
        List<String> fields = new ArrayList<>();
        StringBuilder field = new StringBuilder();
        boolean quoted = false;
        for( int i = 0; i < line.length(); i++ ) {
            char ch = line.charAt(i);
            if (ch == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (ch == delimiter && !quoted) {
                fields.add(field.toString().trim());
                field.setLength(0);
            } else {
                field.append(ch);
            }
        }
        fields.add(field.toString().trim());
        return fields;
    }

    /**
     * Write the classes of a unique values style as text, one <code>value r g b label</code> line
     * per class, as read by {@link #parseValueClasses(String)}.
     *
     * @param classes the classes, all with a color.
     * @return the text.
     */
    public static String toUniqueValuesString( List<ValueClass> classes ) {
        StringBuilder sb = new StringBuilder();
        DecimalFormat format = new DecimalFormat("0.##########", DecimalFormatSymbols.getInstance(Locale.ROOT));
        for( ValueClass valueClass : classes ) {
            Color color = valueClass.color() == null ? Color.GRAY : valueClass.color();
            sb.append(format.format(valueClass.value())).append(' ').append(color.getRed()).append(' ')
                    .append(color.getGreen()).append(' ').append(color.getBlue());
            if (valueClass.label() != null && !valueClass.label().isBlank()) {
                sb.append(' ').append(valueClass.label().trim());
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * @param colorMap the color map of a raster symbolizer, as one of a unique values style.
     * @return its entries with a literal value and color as classes, with their labels, leaving
     *          out the fully transparent entries, which mark the novalue.
     */
    public static List<ValueClass> getValueClasses( ColorMap colorMap ) {
        List<ValueClass> classes = new ArrayList<>();
        for( ColorMapEntry entry : colorMap.getColorMapEntries() ) {
            Double opacity = entry.getOpacity() == null ? null : entry.getOpacity().evaluate(null, Double.class);
            if (opacity != null && opacity == 0.0) {
                continue;
            }
            Double quantity = entry.getQuantity() == null ? null : entry.getQuantity().evaluate(null, Double.class);
            Color color = entry.getColor() == null ? null : entry.getColor().evaluate(null, Color.class);
            if (quantity == null || color == null) {
                continue;
            }
            classes.add(new ValueClass(quantity, color, entry.getLabel()));
        }
        return classes;
    }

    /**
     * The colors of a number of classes taken from a colortable: its own colors spread over it
     * when it has enough of them, else colors interpolated between its colors.
     *
     * @param tableString the text of a colortable.
     * @param count the number of classes.
     * @return the colors, one per class.
     */
    public static Color[] getClassColors( String tableString, int count ) {
        List<Color> tableColors = getColortableColors(tableString);
        Color[] colors = new Color[count];
        int size = tableColors.size();
        for( int i = 0; i < count; i++ ) {
            if (size == 0) {
                colors[i] = Color.GRAY;
            } else if (size == 1 || count == 1) {
                colors[i] = tableColors.get(0);
            } else if (count <= size) {
                colors[i] = tableColors.get((int) Math.round(i * (size - 1) / (double) (count - 1)));
            } else {
                double scaled = i * (size - 1) / (double) (count - 1);
                int index = Math.min((int) scaled, size - 2);
                double t = scaled - index;
                Color a = tableColors.get(index);
                Color b = tableColors.get(index + 1);
                colors[i] = new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                        (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                        (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
            }
        }
        return colors;
    }

    /**
     * @param colorMap the color map of a raster symbolizer.
     * @return <code>true</code> if it gives some values their own color, without interpolation.
     */
    public static boolean isUniqueValues( ColorMap colorMap ) {
        return colorMap != null && colorMap.getType() == ColorMap.TYPE_VALUES;
    }

    /**
     * @param tableString the text of a colortable.
     * @return <code>true</code> if the colortable gives the values of its colors, so that a min and
     *          max are not used.
     */
    public static boolean colortableHasValues( String tableString ) {
        for( String line : tableString.split("\n") ) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            int count = trimmed.split("\\s+").length;
            if (count == 4 || count == 8) {
                return true;
            }
        }
        return false;
    }

    /**
     * @param tableString the text of a colortable.
     * @return the colors of the colortable, in their order, as for a preview of it.
     */
    public static List<Color> getColortableColors( String tableString ) {
        List<Color> colors = new ArrayList<>();
        for( String line : tableString.split("\n") ) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                continue;
            }
            String[] split = trimmed.split("\\s+");
            try {
                if (split.length == 3) {
                    colors.add(new Color(Integer.parseInt(split[0]), Integer.parseInt(split[1]), Integer.parseInt(split[2])));
                } else if (split.length == 4 || split.length == 8) {
                    colors.add(new Color(Integer.parseInt(split[1]), Integer.parseInt(split[2]), Integer.parseInt(split[3])));
                    if (split.length == 8) {
                        colors.add(
                                new Color(Integer.parseInt(split[5]), Integer.parseInt(split[6]), Integer.parseInt(split[7])));
                    }
                }
            } catch (IllegalArgumentException e) {
                // not a color line
            }
        }
        return colors;
    }

    /**
     * Write the color map of a raster symbolizer as the text of a colortable with values, one
     * <code>value r g b</code> line per entry, leaving out the fully transparent entries, which
     * mark the novalue.
     *
     * @param colorMap the color map.
     * @return the text, empty if the map has no entry with a literal value and color.
     */
    public static String toColortableString( ColorMap colorMap ) {
        StringBuilder sb = new StringBuilder();
        DecimalFormat format = new DecimalFormat("0.##########", DecimalFormatSymbols.getInstance(Locale.ROOT));
        for( ColorMapEntry entry : colorMap.getColorMapEntries() ) {
            Double opacity = entry.getOpacity() == null ? null : entry.getOpacity().evaluate(null, Double.class);
            if (opacity != null && opacity == 0.0) {
                continue;
            }
            Double quantity = entry.getQuantity() == null ? null : entry.getQuantity().evaluate(null, Double.class);
            Color color = entry.getColor() == null ? null : entry.getColor().evaluate(null, Color.class);
            if (quantity == null || color == null) {
                continue;
            }
            sb.append(format.format(quantity)).append(' ').append(color.getRed()).append(' ').append(color.getGreen())
                    .append(' ').append(color.getBlue()).append('\n');
        }
        return sb.toString();
    }

    /**
     * @param colorMap the color map of a raster symbolizer.
     * @return the values of the first and last entries, leaving out the fully transparent entries,
     *          which mark the novalue, <code>null</code> if there are not two of them.
     */
    public static double[] getColorMapRange( ColorMap colorMap ) {
        Double first = null;
        Double last = null;
        for( ColorMapEntry entry : colorMap.getColorMapEntries() ) {
            Double opacity = entry.getOpacity() == null ? null : entry.getOpacity().evaluate(null, Double.class);
            if (opacity != null && opacity == 0.0) {
                continue;
            }
            Double quantity = entry.getQuantity() == null ? null : entry.getQuantity().evaluate(null, Double.class);
            if (quantity != null) {
                if (first == null) {
                    first = quantity;
                }
                last = quantity;
            }
        }
        if (first == null || last.equals(first)) {
            return null;
        }
        return new double[]{first, last};
    }

    /**
     * Set the band that the raster symbolizers of a style draw, as the gray channel of their
     * channel selection, so that the colortable of a multi band raster shows one of its bands.
     *
     * @param style the style to change.
     * @param band the 0 based index of the band, written as the 1 based channel name of SLD, or a
     *          negative value to remove the channel selection.
     */
    public static void setGrayChannel( Style style, int band ) {
        for( FeatureTypeStyle featureTypeStyle : style.featureTypeStyles() ) {
            for( Rule rule : featureTypeStyle.rules() ) {
                for( Symbolizer symbolizer : rule.symbolizers() ) {
                    if (symbolizer instanceof RasterSymbolizer rasterSymbolizer) {
                        if (band < 0) {
                            rasterSymbolizer.setChannelSelection(null);
                        } else {
                            SelectedChannelType gray = sf.createSelectedChannelType(String.valueOf(band + 1),
                                    (ContrastEnhancement) null);
                            rasterSymbolizer.setChannelSelection(sf.channelSelection(gray));
                        }
                    }
                }
            }
        }
    }

    /**
     * @param style a raster style.
     * @return the 0 based index of the band set as gray channel in the first raster symbolizer of
     *          the style, -1 if it has none.
     */
    public static int getGrayChannel( Style style ) {
        if (style == null) {
            return -1;
        }
        for( FeatureTypeStyle featureTypeStyle : style.featureTypeStyles() ) {
            for( Rule rule : featureTypeStyle.rules() ) {
                for( Symbolizer symbolizer : rule.symbolizers() ) {
                    if (symbolizer instanceof RasterSymbolizer rasterSymbolizer) {
                        ChannelSelection channelSelection = rasterSymbolizer.getChannelSelection();
                        SelectedChannelType gray = channelSelection == null ? null : channelSelection.getGrayChannel();
                        Integer channel = gray == null || gray.getChannelName() == null
                                ? null
                                : gray.getChannelName().evaluate(null, Integer.class);
                        return channel == null ? -1 : channel - 1;
                    }
                }
            }
        }
        return -1;
    }

    public static String styleToString( Style style ) throws Exception {
        String styleStr = SldUtilities.styleToString(style);
        return styleStr;
    }

    public static void main( String[] args ) throws Exception {
        double[] values = {0, 360};
        // String createStyleForColortable = createStyleForColortable("aspect", 0.0, 360.0, null,
        // 0.5);
        // System.out.println(createStyleForColortable);
        String createStyleForColortable = styleToString(createStyleForColortable(EColorTables.elev.name(), 73.835, 144.889, 0.8));
        System.out.println(createStyleForColortable);
        // String createStyleForColortable = createStyleForColortable(DefaultTables.SLOPE, 0.0,
        // 0.9656, null, 1.0);
        // System.out.println(createStyleForColortable);
        // String createStyleForColortable = createStyleForColortable("flow", 0, 0, null, 1);
        // System.out.println(createStyleForColortable);
    }
}
