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
import java.util.List;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.feature.NameImpl;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDParser;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.UserLayer;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.style.sld.SLDHandler;
import org.opengis.filter.FilterFactory;

/**
 * Utilities to handle style.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.0
 */
public class SldUtilities {

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
    public static Style getStyleFromFile(File file) {
        Style style = null;
        try {
            String name = file.getName();
            if (!name.endsWith("sld")) {
                String nameWithoutExtention = FileUtilities.getNameWithoutExtention(file);
                File sldFile = new File(file.getParentFile(), nameWithoutExtention + ".sld");
                if (sldFile.exists()) {
                    file = sldFile;
                } else {
                    // no style file here
                    return null;
                }
            }

            SLDHandler h = new SLDHandler();
            StyledLayerDescriptor sld = h.parse(file, null, null, null);
//            SLDParser stylereader = new SLDParser(sf, file);
//            StyledLayerDescriptor sld = stylereader.parseSLD();
            style = getDefaultStyle(sld);
            return style;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Converts a style to its string representation to be written to file.
     * 
     * @param style the style to convert.
     * @return the style string.
     * @throws Exception
     */
    public static String styleToString(Style style) throws Exception {
        StyledLayerDescriptor sld = sf.createStyledLayerDescriptor();
        UserLayer layer = sf.createUserLayer();
        layer.setLayerFeatureConstraints(new FeatureTypeConstraint[] { null });
        sld.addStyledLayer(layer);
        layer.addUserStyle(style);

        SLDTransformer aTransformer = new SLDTransformer();
        aTransformer.setIndentation(4);
        String xml = aTransformer.transform(sld);
        return xml;
    }

    public static Style getDefaultStyle(StyledLayerDescriptor sld) {
        Style[] styles = SLD.styles(sld);
        for (int i = 0; i < styles.length; i++) {
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
    private static void genericizeftStyles(List<FeatureTypeStyle> ftStyles) {
        for (FeatureTypeStyle featureTypeStyle : ftStyles) {
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
    public static Color colorWithoutAlpha(Color color) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue());
    }

    /**
     * Creates a color with the given alpha.
     *  
     * @param color the color to use.
     * @param alpha an alpha value between 0 and 255.
     * @return the color with alpha.
     */
    public static Color colorWithAlpha(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
