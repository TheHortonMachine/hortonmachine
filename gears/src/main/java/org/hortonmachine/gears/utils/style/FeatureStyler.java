/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.gears.utils.style;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Fill;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.GraphicalSymbol;
import org.geotools.api.style.Halo;
import org.geotools.api.style.LineSymbolizer;
import org.geotools.api.style.Mark;
import org.geotools.api.style.PointSymbolizer;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Stroke;
import org.geotools.api.style.Style;
import org.geotools.api.style.Symbolizer;
import org.geotools.api.style.TextSymbolizer;

/**
 * Gives each feature the look that an SLD style gives it, for renderers that draw each feature as
 * a shape with simple attributes, as the 3D layers of the viewer, instead of using the GeoTools
 * renderer.
 *
 * <p>The rules of all the feature type styles are taken in order: a feature is drawn with the
 * first rule whose filter it matches, else with the first "else" rule. Its label is the one of
 * the first matching rule that has one. Scale limits are ignored, the shapes don't change with
 * the scale. Values given by expressions, as a color read from a field, are evaluated for each
 * feature.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class FeatureStyler {

    /**
     * The look of a feature, records with the same values can share the shape attributes.
     *
     * @param fill the fill color, <code>null</code> if not filled.
     * @param fillOpacity the fill opacity, from 0 to 1.
     * @param stroke the border or line color, <code>null</code> if not stroked.
     * @param strokeOpacity the stroke opacity, from 0 to 1.
     * @param strokeWidth the stroke width in pixels.
     * @param size the point size in pixels.
     */
    public record Symbol( Color fill, double fillOpacity, Color stroke, double strokeOpacity, double strokeWidth, double size ) {
    }

    /**
     * The label of a feature.
     *
     * @param text the text.
     * @param font the font.
     * @param color the text color.
     * @param haloColor the color of the halo around the text, <code>null</code> if none.
     */
    public record Label( String text, Font font, Color color, Color haloColor ) {
    }

    /**
     * The look and label of a feature.
     *
     * @param symbol the look, <code>null</code> if the feature is not drawn.
     * @param label the label, <code>null</code> if it has none.
     */
    public record Styled( Symbol symbol, Label label ) {
    }

    private final Style style;
    private final List<Rule> rules = new ArrayList<>();
    private final List<Rule> elseRules = new ArrayList<>();

    /**
     * @param style the style.
     */
    public FeatureStyler( Style style ) {
        this.style = style;
        for( FeatureTypeStyle featureTypeStyle : style.featureTypeStyles() ) {
            for( Rule rule : featureTypeStyle.rules() ) {
                if (rule.isElseFilter()) {
                    elseRules.add(rule);
                } else {
                    rules.add(rule);
                }
            }
        }
    }

    /**
     * @return the style.
     */
    public Style getStyle() {
        return style;
    }

    /**
     * @param feature the feature.
     * @return its look and label, <code>null</code> if no rule matches it.
     */
    public Styled style( SimpleFeature feature ) {
        List<Rule> matching = new ArrayList<>();
        for( Rule rule : rules ) {
            Filter filter = rule.getFilter();
            if (filter == null || filter.evaluate(feature)) {
                matching.add(rule);
            }
        }
        if (matching.isEmpty()) {
            matching = elseRules;
        }
        if (matching.isEmpty()) {
            return null;
        }
        Symbol symbol = null;
        Label label = null;
        for( Rule rule : matching ) {
            for( Symbolizer symbolizer : rule.symbolizers() ) {
                if (symbolizer instanceof TextSymbolizer) {
                    if (label == null) {
                        label = label((TextSymbolizer) symbolizer, feature);
                    }
                } else if (symbol == null) {
                    symbol = symbol(symbolizer, feature);
                }
            }
            if (symbol != null && label != null) {
                break;
            }
        }
        if (symbol == null && label == null) {
            return null;
        }
        return new Styled(symbol, label);
    }

    private static Symbol symbol( Symbolizer symbolizer, SimpleFeature feature ) {
        if (symbolizer instanceof PolygonSymbolizer) {
            PolygonSymbolizer polygonSymbolizer = (PolygonSymbolizer) symbolizer;
            Fill fill = polygonSymbolizer.getFill();
            Stroke stroke = polygonSymbolizer.getStroke();
            return new Symbol(fillColor(fill, feature), fillOpacity(fill, feature), strokeColor(stroke, feature),
                    strokeOpacity(stroke, feature), strokeWidth(stroke, feature), 0);
        } else if (symbolizer instanceof LineSymbolizer) {
            Stroke stroke = ((LineSymbolizer) symbolizer).getStroke();
            return new Symbol(null, 0, strokeColor(stroke, feature), strokeOpacity(stroke, feature),
                    strokeWidth(stroke, feature), 0);
        } else if (symbolizer instanceof PointSymbolizer) {
            Graphic graphic = ((PointSymbolizer) symbolizer).getGraphic();
            if (graphic == null) {
                return null;
            }
            double size = number(graphic.getSize(), feature, 6);
            Mark mark = null;
            for( GraphicalSymbol graphicalSymbol : graphic.graphicalSymbols() ) {
                if (graphicalSymbol instanceof Mark) {
                    mark = (Mark) graphicalSymbol;
                    break;
                }
            }
            if (mark == null) {
                // an external graphic: a gray point of the same size
                return new Symbol(Color.GRAY, 1, null, 0, 0, size);
            }
            Fill fill = mark.getFill();
            Stroke stroke = mark.getStroke();
            return new Symbol(fillColor(fill, feature), fillOpacity(fill, feature), strokeColor(stroke, feature),
                    strokeOpacity(stroke, feature), strokeWidth(stroke, feature), size);
        }
        return null;
    }

    private static Label label( TextSymbolizer textSymbolizer, SimpleFeature feature ) {
        Expression labelExpression = textSymbolizer.getLabel();
        if (labelExpression == null) {
            return null;
        }
        String text = labelExpression.evaluate(feature, String.class);
        if (text == null || text.isBlank()) {
            return null;
        }
        String family = Font.SANS_SERIF;
        int fontStyle = Font.PLAIN;
        int size = 12;
        org.geotools.api.style.Font font = textSymbolizer.getFont();
        if (font != null) {
            if (!font.getFamily().isEmpty()) {
                String fontFamily = font.getFamily().get(0).evaluate(feature, String.class);
                if (fontFamily != null) {
                    family = fontFamily;
                }
            }
            if ("bold".equalsIgnoreCase(string(font.getWeight(), feature))) {
                fontStyle |= Font.BOLD;
            }
            String styleName = string(font.getStyle(), feature);
            if ("italic".equalsIgnoreCase(styleName) || "oblique".equalsIgnoreCase(styleName)) {
                fontStyle |= Font.ITALIC;
            }
            size = (int) Math.round(number(font.getSize(), feature, size));
        }
        Color color = fillColor(textSymbolizer.getFill(), feature);
        Halo halo = textSymbolizer.getHalo();
        Color haloColor = halo != null ? fillColor(halo.getFill(), feature) : null;
        return new Label(text, new Font(family, fontStyle, size), color != null ? color : Color.BLACK, haloColor);
    }

    private static Color fillColor( Fill fill, SimpleFeature feature ) {
        return fill == null ? null : color(fill.getColor(), feature);
    }

    private static double fillOpacity( Fill fill, SimpleFeature feature ) {
        return fill == null ? 0 : number(fill.getOpacity(), feature, 1);
    }

    private static Color strokeColor( Stroke stroke, SimpleFeature feature ) {
        return stroke == null ? null : color(stroke.getColor(), feature);
    }

    private static double strokeOpacity( Stroke stroke, SimpleFeature feature ) {
        return stroke == null ? 0 : number(stroke.getOpacity(), feature, 1);
    }

    private static double strokeWidth( Stroke stroke, SimpleFeature feature ) {
        return stroke == null ? 0 : number(stroke.getWidth(), feature, 1);
    }

    private static Color color( Expression expression, SimpleFeature feature ) {
        if (expression == null) {
            return null;
        }
        try {
            return expression.evaluate(feature, Color.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static double number( Expression expression, SimpleFeature feature, double defaultValue ) {
        if (expression == null) {
            return defaultValue;
        }
        try {
            Double value = expression.evaluate(feature, Double.class);
            return value != null ? value : defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String string( Expression expression, SimpleFeature feature ) {
        return expression == null ? null : expression.evaluate(feature, String.class);
    }
}
