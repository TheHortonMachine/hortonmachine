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
package org.hortonmachine.gears.utils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.NameImpl;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.geotools.util.factory.GeoTools;
import org.geotools.xml.styling.SLDTransformer;
import org.hortonmachine.gears.io.grasslegacy.map.color.ColorRule;
import org.hortonmachine.gears.io.grasslegacy.map.color.GrassColorTable;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.style.sld.SLDHandler;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;

/**
 * Utilities to handle style.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.0
 */
public class SldUtilities {
    public static final String SLD_EXTENSION = "sld";

    /**
     * The default {@link StyleFactory} to use.
     */
    public static StyleFactory sf = CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());

    /**
     * The default {@link FilterFactory} to use.
     */
    public static FilterFactory ff = CommonFactoryFinder.getFilterFactory(GeoTools.getDefaultHints());

    /**
     * The default {@link StyleBuilder} to use.
     */
    public static StyleBuilder sb = new StyleBuilder(sf, ff);

    /**
     * The type name that can be used in an SLD in the featuretypestyle that matches all feature types.
     */
    public static final String GENERIC_FEATURE_TYPENAME = "Feature";

    /**
     * Get the style from an sld file.
     * 
     * @param file the SLD file or a companion file.
     * @return the {@link Style} object.
     * @throws IOException
     */
    public static Style getStyleFromFile( File file ) {
        try {
            File styleFile = getStyleFile(file);
            if (styleFile == null)
                return null;

            String sldString = FileUtilities.readFile(styleFile);
            return getStyleFromSldString(sldString);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Get the style file related to a given datafile.
     * 
     * @param dataFile the file that should have the style file as sidecar file.
     * @return the style file, if available.
     */
    public static File getStyleFile( File dataFile ) {
        String name = dataFile.getName();
        if (!name.toLowerCase().endsWith(SLD_EXTENSION)) {
            String nameWithoutExtention = FileUtilities.getNameWithoutExtention(dataFile);
            File sldFile = new File(dataFile.getParentFile(), nameWithoutExtention + "." + SLD_EXTENSION);
            if (sldFile.exists()) {
                return sldFile;
            } else {
                // no style file here
                return null;
            }
        } else {
            return dataFile;
        }
    }

    public static Style getStyleFromSldString( String sldString ) throws IOException {
        if (sldString == null)
            return null;
        SLDHandler h = new SLDHandler();
        StyledLayerDescriptor sld = h.parse(sldString, null, null, null);
        Style style = getDefaultStyle(sld);
        return style;
    }

    public static Style getStyleFromRasterFile( File file ) throws Exception {
        String coveragePath = file.getAbsolutePath();
        if (CoverageUtilities.isGrass(coveragePath)) {
            return getGrassStyle(coveragePath);
        } else {
            RasterSymbolizer sym = sf.getDefaultRasterSymbolizer();
            return SLD.wrapSymbolizers(sym);
        }
    }

    public static Style getGrassStyle( String path ) throws Exception {
        List<String> valuesList = new ArrayList<String>();
        List<Color> colorsList = new ArrayList<Color>();

        StyleBuilder sB = new StyleBuilder(sf);
        RasterSymbolizer rasterSym = sf.createRasterSymbolizer();

        File grassFile = new File(path);
        String mapName = grassFile.getName();
        String mapsetPath = grassFile.getParentFile().getParent();

        GrassColorTable ctable = new GrassColorTable(mapsetPath, mapName, null);
        Enumeration<ColorRule> rules = ctable.getColorRules();

        while( rules.hasMoreElements() ) {
            ColorRule element = (ColorRule) rules.nextElement();

            float fromValue = element.getLowCategoryValue();
            float toValue = element.getLowCategoryValue() + element.getCategoryRange();
            byte[] lowcatcol = element.getColor(fromValue);
            byte[] highcatcol = element.getColor(toValue);
            Color fromColor = new Color((int) (lowcatcol[0] & 0xff), (int) (lowcatcol[1] & 0xff), (int) (lowcatcol[2] & 0xff));
            Color toColor = new Color((int) (highcatcol[0] & 0xff), (int) (highcatcol[1] & 0xff), (int) (highcatcol[2] & 0xff));

            String from = String.valueOf(fromValue);
            if (!valuesList.contains(from)) {
                valuesList.add(from);
                colorsList.add(fromColor);
            }

            String to = String.valueOf(toValue);
            if (!valuesList.contains(to)) {
                valuesList.add(to);
                colorsList.add(toColor);
            }
        }

        ColorMap colorMap = sf.createColorMap();

        if (valuesList.size() > 1) {
            for( int i = 0; i < valuesList.size(); i++ ) {
                String fromValueStr = valuesList.get(i);
                // String toValueStr = valuesList.get(i + 1);
                Color fromColor = colorsList.get(i);
                // Color toColor = colorsList.get(i + 1);
                // double[] values = {Double.parseDouble(fromValueStr),
                // Double.parseDouble(toValueStr)};
                // double opacity = 1.0;

                Expression fromColorExpr = sB
                        .colorExpression(new java.awt.Color(fromColor.getRed(), fromColor.getGreen(), fromColor.getBlue(), 255));
                // Expression toColorExpr = sB.colorExpression(new java.awt.Color(toColor.getRed(),
                // toColor.getGreen(), toColor
                // .getBlue(), 255));
                Expression fromExpr = sB.literalExpression(Double.parseDouble(fromValueStr));
                // Expression toExpr = sB.literalExpression(values[1]);
                // Expression opacityExpr = sB.literalExpression(opacity);

                ColorMapEntry entry = sf.createColorMapEntry();
                entry.setQuantity(fromExpr);
                entry.setColor(fromColorExpr);
                // entry.setOpacity(opacityExpr);
                colorMap.addColorMapEntry(entry);

                // entry = sf.createColorMapEntry();
                // entry.setQuantity(toExpr);
                // entry.setOpacity(opacityExpr);
                // entry.setColor(toColorExpr);
                // colorMap.addColorMapEntry(entry);
            }
        } else if (valuesList.size() == 1) {
            String fromValueStr = valuesList.get(0);
            Color fromColor = colorsList.get(0);
            // double opacity = 1.0;

            Expression fromColorExpr = sB
                    .colorExpression(new java.awt.Color(fromColor.getRed(), fromColor.getGreen(), fromColor.getBlue(), 255));
            Expression fromExpr = sB.literalExpression(Double.parseDouble(fromValueStr));
            // Expression opacityExpr = sB.literalExpression(opacity);

            ColorMapEntry entry = sf.createColorMapEntry();
            entry.setQuantity(fromExpr);
            entry.setColor(fromColorExpr);
            // entry.setOpacity(opacityExpr);
            colorMap.addColorMapEntry(entry);
            colorMap.addColorMapEntry(entry);
        } else {
            throw new IllegalArgumentException();
        }

        rasterSym.setColorMap(colorMap);

        /*
         * set global transparency for the map
         */
        int alpha = ctable.getAlpha();
        rasterSym.setOpacity(sB.literalExpression(alpha / 255.0));

        Style newStyle = SLD.wrapSymbolizers(rasterSym);

        return newStyle;
    }

    /**
     * Converts a style to its string representation to be written to file.
     * 
     * @param style the style to convert.
     * @return the style string.
     * @throws Exception
     */
    public static String styleToString( Style style ) throws Exception {
        StyledLayerDescriptor sld = sf.createStyledLayerDescriptor();
        UserLayer layer = sf.createUserLayer();
        layer.setLayerFeatureConstraints(new FeatureTypeConstraint[]{null});
        sld.addStyledLayer(layer);
        layer.addUserStyle(style);

        SLDTransformer aTransformer = new SLDTransformer();
        aTransformer.setIndentation(4);
        String xml = aTransformer.transform(sld);
        return xml;
    }

    public static Style getDefaultStyle( StyledLayerDescriptor sld ) {
        Style[] styles = SLD.styles(sld);
        for( int i = 0; i < styles.length; i++ ) {
            Style style = styles[i];
            List<FeatureTypeStyle> ftStyles = style.featureTypeStyles();
            genericizeftStyles(ftStyles);
            if (style.isDefault()) {
                return style;
            }
        }
        // no default, so just grab the first one
        return styles[0];
    }

    /**
     * Converts the type name of all FeatureTypeStyles to Feature so that the all apply to any feature type.  This is admittedly dangerous
     * but is extremely useful because it means that the style can be used with any feature type.
     *
     * @param ftStyles
     */
    private static void genericizeftStyles( List<FeatureTypeStyle> ftStyles ) {
        for( FeatureTypeStyle featureTypeStyle : ftStyles ) {
            featureTypeStyle.featureTypeNames().clear();
            featureTypeStyle.featureTypeNames().add(new NameImpl(GENERIC_FEATURE_TYPENAME));
        }
    }

    /**
     * REmoves the alpha channel from a color.
     * 
     * @param color the color.
     * @return the color without alpha.
     */
    public static Color colorWithoutAlpha( Color color ) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Creates a color with the given alpha.
     *  
     * @param color the color to use.
     * @param alpha an alpha value between 0 and 255.
     * @return the color with alpha.
     */
    public static Color colorWithAlpha( Color color, int alpha ) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
