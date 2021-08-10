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
import java.util.ArrayList;
import java.util.List;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.opengis.filter.expression.Expression;

/**
 * A class to help with raster styling. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
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

        return createRasterStyle(min, max, values, colorsArray, opacity);
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
