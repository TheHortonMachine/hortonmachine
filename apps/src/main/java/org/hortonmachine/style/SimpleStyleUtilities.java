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
package org.hortonmachine.style;

import java.awt.Color;
import java.io.File;
import java.util.List;

import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.style.FeatureTypeStyleWrapper;
import org.hortonmachine.gears.utils.style.LineSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.PointSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.PolygonSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.RuleWrapper;
import org.hortonmachine.gears.utils.style.StyleUtilities;
import org.hortonmachine.gears.utils.style.StyleWrapper;

/**
 * Simple styles generator.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SimpleStyleUtilities {

    public static final String SPHERE = "gov.nasa.worldwind.render.markers.Sphere";
    public static final String CUBE = "gov.nasa.worldwind.render.markers.Cube";
    public static final String CONE = "gov.nasa.worldwind.render.markers.Cone";
    /**
     * Creates a default {@link Style} for a line.
     * 
     * @param color
     *            the color.
     * @param width
     *            the line width.
     * @return the simple style.
     */
    /**
     * @return
     */
    public static Style createSimpleLineStyle( Color color, float width ) {
        FeatureTypeStyle featureTypeStyle = StyleUtilities.sf.createFeatureTypeStyle();
        featureTypeStyle.rules().add(createSimpleLineRule(color, width));

        Style style = StyleUtilities.sf.createStyle();
        style.featureTypeStyles().add(featureTypeStyle);

        return style;
    }

    /**
     * Creates a simple {@link Rule} for a line.
     * 
     * @param color
     *            the color.
     * @param width
     *            the line width.
     * @return the rule.
     */
    public static Rule createSimpleLineRule( Color color, float width ) {
        LineSymbolizer lineSymbolizer = StyleUtilities.sf.createLineSymbolizer();
        lineSymbolizer.setStroke(
                StyleUtilities.sf.createStroke(StyleUtilities.ff.literal("#" + Integer.toHexString(color.getRGB() & 0xffffff)),
                        StyleUtilities.ff.literal(width)));

        Rule rule = StyleUtilities.sf.createRule();
        rule.setName("New rule");
        rule.symbolizers().add(lineSymbolizer);

        return rule;
    }

    public static SimpleStyle getSimpleStyle( String path, String geomTypeString ) throws Exception {
        SimpleStyle simpleStyle = new SimpleStyle();
        if (path == null) {
            return simpleStyle;
        }

        Style style = SldUtilities.getStyleFromFile(new File(path));
        if (style == null)
            return null;

        return getSimpleStyle(style, geomTypeString);
    }

    public static SimpleStyle getSimpleStyle( Style style, String geomTypeString ) {
        EGeometryType geomType = EGeometryType.forWktName(geomTypeString);
        
        SimpleStyle simpleStyle = new SimpleStyle();
        StyleWrapper styleWrapper = new StyleWrapper(style);
        List<FeatureTypeStyleWrapper> featureTypeStylesWrapperList = styleWrapper.getFeatureTypeStylesWrapperList();
        for( FeatureTypeStyleWrapper featureTypeStyleWrapper : featureTypeStylesWrapperList ) {
            List<RuleWrapper> rulesWrapperList = featureTypeStyleWrapper.getRulesWrapperList();
            for( RuleWrapper ruleWrapper : rulesWrapperList ) {

                switch( geomType ) {
                case POLYGON:
                case MULTIPOLYGON:
                    PolygonSymbolizerWrapper polygonSymbolizerWrapper = ruleWrapper.getGeometrySymbolizersWrapper()
                            .adapt(PolygonSymbolizerWrapper.class);

                    simpleStyle.fillColor = Color.decode(checkColor(polygonSymbolizerWrapper.getFillColor()));
                    simpleStyle.fillOpacity = Double.parseDouble(checkNumeric(polygonSymbolizerWrapper.getFillOpacity()));
                    simpleStyle.strokeColor = Color.decode(checkColor(polygonSymbolizerWrapper.getStrokeColor()));
                    simpleStyle.strokeWidth = Double.parseDouble(checkNumeric(polygonSymbolizerWrapper.getStrokeWidth()));
                    break;
                case LINESTRING:
                case MULTILINESTRING:
                    LineSymbolizerWrapper lineSymbolizerWrapper = ruleWrapper.getGeometrySymbolizersWrapper()
                            .adapt(LineSymbolizerWrapper.class);

                    simpleStyle.strokeColor = Color.decode(checkColor(lineSymbolizerWrapper.getStrokeColor()));
                    simpleStyle.strokeWidth = Double.parseDouble(checkNumeric(lineSymbolizerWrapper.getStrokeWidth()));

                    break;
                case POINT:
                case MULTIPOINT:
                    PointSymbolizerWrapper pointSymbolizerWrapper = ruleWrapper.getGeometrySymbolizersWrapper()
                            .adapt(PointSymbolizerWrapper.class);

                    simpleStyle.fillColor = Color.decode(checkColor(pointSymbolizerWrapper.getFillColor()));
                    simpleStyle.fillOpacity = Double.parseDouble(checkNumeric(pointSymbolizerWrapper.getFillOpacity()));
                    simpleStyle.strokeColor = Color.decode(checkColor(pointSymbolizerWrapper.getStrokeColor()));
                    simpleStyle.strokeWidth = Double.parseDouble(checkNumeric(pointSymbolizerWrapper.getStrokeWidth()));
                    simpleStyle.shapeSize = Double.parseDouble(checkNumeric(pointSymbolizerWrapper.getSize()));
                    String markName = pointSymbolizerWrapper.getMarkName();
                    if (markName != null && markName.trim().length() != 0) {
                        switch( markName ) {
                        case "square":
                            simpleStyle.shapeType = CUBE;
                            break;
                        case "triangle":
                            simpleStyle.shapeType = CONE;
                            break;
                        case "circle":
                        default:
                            simpleStyle.shapeType = SPHERE;
                            break;
                        }
                    }
                    break;

                default:
                    break;
                }

                // only one rule supported for now
                return simpleStyle;
            }
        }

        return null;
    }

    private static String checkNumeric( String numeric ) {
        if (numeric == null) {
            return "0.0";
        }
        return numeric;
    }

    private static String checkColor( String colorString ) {
        if (colorString == null) {
            return "#00000000";
        }
        return colorString;
    }
}
