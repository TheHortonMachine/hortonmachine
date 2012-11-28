/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.utils.colors;

import java.awt.Color;
import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.jgrasstools.gears.utils.SldUtilities;
import org.jgrasstools.gears.utils.StringUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.opengis.filter.expression.Expression;

/**
 * A class to help with raster styling. 
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class RasterStyleUtilities {

    private static StyleFactory sf = CommonFactoryFinder.getStyleFactory(null);

    public static void dumpRasterStyle( String path, double min, double max, Color[] colors, double opacity ) throws Exception {
        String styleStr = createRasterStyleString(min, max, colors, opacity);
        FileUtilities.writeFile(styleStr, new File(path));
    }

    private static String createRasterStyleString( double min, double max, Color[] colors, double opacity ) throws Exception {
        Style newStyle = createRasterStyle(min, max, colors, opacity);
        String styleStr = SldUtilities.styleToString(newStyle);
        return styleStr;
    }

    private static Style createRasterStyle( double min, double max, Color[] colors, double opacity ) {
        StyleBuilder sB = new StyleBuilder(sf);
        RasterSymbolizer rasterSym = sf.createRasterSymbolizer();

        int bins = colors.length - 1;
        double interval = (max - min) / bins;
        double runningValue = min;

        ColorMap colorMap = sf.createColorMap();

        for( int i = 0; i < colors.length - 1; i++ ) {
            Color fromColor = colors[i];
            Color toColor = colors[i + 1];

            double start = runningValue;
            runningValue = runningValue + interval;
            double end = runningValue;

            Expression fromColorExpr = sB.colorExpression(fromColor);
            Expression toColorExpr = sB.colorExpression(toColor);
            Expression fromExpr = sB.literalExpression(start);
            Expression toExpr = sB.literalExpression(end);
            Expression opacityExpr = sB.literalExpression(opacity);

            ColorMapEntry entry = sf.createColorMapEntry();
            entry.setQuantity(fromExpr);
            entry.setColor(fromColorExpr);
            entry.setOpacity(opacityExpr);
            colorMap.addColorMapEntry(entry);

            entry = sf.createColorMapEntry();
            entry.setQuantity(toExpr);
            entry.setOpacity(opacityExpr);
            entry.setColor(toColorExpr);
            colorMap.addColorMapEntry(entry);
        }

        rasterSym.setColorMap(colorMap);

        /*
         * set global transparency for the map
         */
        rasterSym.setOpacity(sB.literalExpression(opacity));

        Style newStyle = SLD.wrapSymbolizers(rasterSym);
        return newStyle;
    }

    public static String createStyleForColortable( String colorTableName, double min, double max, double opacity )
            throws Exception {

        String name = "org/jgrasstools/gears/utils/colors/" + colorTableName + ".clr";
        URL resource2 = Thread.currentThread().getContextClassLoader().getResource(name);
        URL resource = RasterStyleUtilities.class.getResource(name);
        InputStream colorTableStream = resource.openStream();
        if (colorTableStream != null) {
            Scanner colorTableScanner = StringUtilities.streamToString(colorTableStream, "\n");
            List<Color> colorList = new ArrayList<Color>();
            while( colorTableScanner.hasNext() ) {
                String line = colorTableScanner.next();
                if (line.startsWith("#")) { //$NON-NLS-1$
                    continue;
                }
                String[] lineSplit = line.trim().split("\\s+"); //$NON-NLS-1$

                int r = Integer.parseInt(lineSplit[0]);
                int g = Integer.parseInt(lineSplit[1]);
                int b = Integer.parseInt(lineSplit[2]);

                colorList.add(new Color(r, g, b));
            }
            Color[] colorsArray = colorList.toArray(new Color[0]);
            return createRasterStyleString(min, max, colorsArray, opacity);
        }
        return null;
    }

    public static void main( String[] args ) throws Exception {
        String createStyleForColortable = createStyleForColortable("aspect", 100, 400, 128);
        System.out.println(createStyleForColortable);
    }
}
