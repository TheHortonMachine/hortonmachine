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

import static org.hortonmachine.gears.utils.style.StyleUtilities.ff;
import static org.hortonmachine.gears.utils.style.StyleUtilities.sf;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.filter.Filter;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.Symbolizer;
import org.geotools.api.style.TextSymbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;

/**
 * The rules of a vector style in one of the simple forms that a style panel can edit, read from
 * and written to a GeoTools {@link Style}:
 *
 * <ul>
 * <li>{@link Kind#SINGLE}: one rule without filter, all features look the same;</li>
 * <li>{@link Kind#UNIQUE_VALUES}: one rule per value of a field, with a filter
 * <code>field = value</code>, plus optionally an "else" rule for the other values;</li>
 * <li>{@link Kind#CUSTOM}: any other style, as one with ranges or scale limits, which is kept as
 * it is.</li>
 * </ul>
 *
 * <p>In both simple forms all rules have the same label, if any.</p>
 *
 * <p>The rules are the ones of the style, not copies, so the symbolizer panels of the style
 * editor can edit them in place.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class StyleRules {

    /**
     * The forms of a style.
     */
    public enum Kind {
        SINGLE, UNIQUE_VALUES, CUSTOM
    }

    /** The name of the rule of the values that have no rule of their own. */
    public static final String OTHER_RULE_NAME = "other";

    private final Kind kind;
    private final Style customStyle;
    private Rule singleRule;
    private String field;
    private final LinkedHashMap<Object, Rule> valueRules = new LinkedHashMap<>();
    private Rule otherRule;
    private TextSymbolizer label;

    private StyleRules( Kind kind, Style customStyle ) {
        this.kind = kind;
        this.customStyle = customStyle;
    }

    /**
     * Create a style with a single rule.
     *
     * @param rule the rule, its filter and labels are removed.
     * @return the style rules.
     */
    public static StyleRules single( Rule rule ) {
        StyleRules styleRules = new StyleRules(Kind.SINGLE, null);
        styleRules.singleRule = withoutLabels(rule);
        styleRules.singleRule.setFilter(null);
        styleRules.singleRule.setElseFilter(false);
        return styleRules;
    }

    /**
     * Create a style with one rule per value of a field.
     *
     * <p><code>null</code> values get no rule of their own: they are drawn by the "else" rule,
     * if there is one.</p>
     *
     * @param template the rule copied for each value, as well as for the other values.
     * @param field the field.
     * @param values the values, in the order of the rules.
     * @param palette the colors of the values, used again from the start if they are fewer.
     * @param withOtherRule if <code>true</code>, an "else" rule draws the features whose value
     *          has no rule, with the colors of the template.
     * @return the style rules.
     */
    public static StyleRules uniqueValues( Rule template, String field, List< ? > values, Color[] palette,
            boolean withOtherRule ) {
        StyleRules styleRules = new StyleRules(Kind.UNIQUE_VALUES, null);
        styleRules.field = field;
        Rule cleanTemplate = withoutLabels(template);
        int index = 0;
        for( Object value : values ) {
            if (value == null || styleRules.valueRules.containsKey(value)) {
                continue;
            }
            Rule rule = copy(cleanTemplate);
            rule.setName(String.valueOf(value));
            rule.setFilter(ff.equals(ff.property(field), ff.literal(value)));
            rule.setElseFilter(false);
            if (palette != null && palette.length > 0) {
                StyleUtilities.setRuleColor(rule, palette[index % palette.length]);
            }
            styleRules.valueRules.put(value, rule);
            index++;
        }
        if (withOtherRule) {
            styleRules.setOtherRule(cleanTemplate);
        }
        return styleRules;
    }

    /**
     * Read the rules of a style.
     *
     * @param style the style.
     * @return the rules, of {@link Kind#CUSTOM} kind if the style has none of the simple forms.
     */
    public static StyleRules read( Style style ) {
        List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
        if (featureTypeStyles.size() != 1) {
            return custom(style);
        }
        List<Rule> rules = featureTypeStyles.get(0).rules();
        if (rules.isEmpty()) {
            return custom(style);
        }

        // all rules must have one geometry symbolizer, at most the same label and no scale limits
        TextSymbolizer label = null;
        for( Rule rule : rules ) {
            int geometrySymbolizers = 0;
            int textSymbolizers = 0;
            for( Symbolizer symbolizer : rule.symbolizers() ) {
                if (symbolizer instanceof TextSymbolizer) {
                    textSymbolizers++;
                    TextSymbolizer textSymbolizer = (TextSymbolizer) symbolizer;
                    if (label == null) {
                        label = textSymbolizer;
                    } else if (!label.equals(textSymbolizer)) {
                        return custom(style);
                    }
                } else {
                    geometrySymbolizers++;
                }
            }
            if (geometrySymbolizers != 1 || textSymbolizers > 1 || hasScaleLimits(rule)) {
                return custom(style);
            }
        }
        // a label in some rules only can't be shown as one label of the style
        if (label != null) {
            for( Rule rule : rules ) {
                if (getLabel(rule) == null) {
                    return custom(style);
                }
            }
        }

        if (rules.size() == 1 && !rules.get(0).isElseFilter() && isAll(rules.get(0).getFilter())) {
            StyleRules styleRules = new StyleRules(Kind.SINGLE, null);
            styleRules.singleRule = rules.get(0);
            styleRules.label = label;
            return styleRules;
        }

        StyleRules styleRules = new StyleRules(Kind.UNIQUE_VALUES, null);
        for( Rule rule : rules ) {
            if (rule.isElseFilter()) {
                if (styleRules.otherRule != null) {
                    return custom(style);
                }
                styleRules.otherRule = rule;
                continue;
            }
            Filter filter = rule.getFilter();
            if (!(filter instanceof PropertyIsEqualTo)) {
                return custom(style);
            }
            PropertyIsEqualTo equalTo = (PropertyIsEqualTo) filter;
            String ruleField = null;
            Object value = null;
            for( Expression expression : new Expression[]{equalTo.getExpression1(), equalTo.getExpression2()} ) {
                if (expression instanceof PropertyName) {
                    ruleField = ((PropertyName) expression).getPropertyName();
                } else if (expression instanceof Literal) {
                    value = ((Literal) expression).getValue();
                }
            }
            if (ruleField == null || value == null || (styleRules.field != null && !styleRules.field.equals(ruleField))
                    || styleRules.valueRules.containsKey(value)) {
                return custom(style);
            }
            styleRules.field = ruleField;
            styleRules.valueRules.put(value, rule);
        }
        if (styleRules.field == null) {
            return custom(style);
        }
        styleRules.label = label;
        return styleRules;
    }

    private static StyleRules custom( Style style ) {
        return new StyleRules(Kind.CUSTOM, style);
    }

    private static boolean isAll( Filter filter ) {
        return filter == null || filter == Filter.INCLUDE;
    }

    private static boolean hasScaleLimits( Rule rule ) {
        return rule.getMinScaleDenominator() > 0 || rule.getMaxScaleDenominator() < Double.POSITIVE_INFINITY;
    }

    private static TextSymbolizer getLabel( Rule rule ) {
        for( Symbolizer symbolizer : rule.symbolizers() ) {
            if (symbolizer instanceof TextSymbolizer) {
                return (TextSymbolizer) symbolizer;
            }
        }
        return null;
    }

    private static Rule copy( Rule rule ) {
        DuplicatingStyleVisitor visitor = new DuplicatingStyleVisitor();
        visitor.visit(rule);
        return (Rule) visitor.getCopy();
    }

    private static Rule withoutLabels( Rule rule ) {
        Rule copy = copy(rule);
        copy.symbolizers().removeIf(s -> s instanceof TextSymbolizer);
        return copy;
    }

    /**
     * @return the kind of the style.
     */
    public Kind getKind() {
        return kind;
    }

    /**
     * @return the rule of a {@link Kind#SINGLE} style, else <code>null</code>.
     */
    public Rule getSingleRule() {
        return singleRule;
    }

    /**
     * @return the field of a {@link Kind#UNIQUE_VALUES} style, else <code>null</code>.
     */
    public String getField() {
        return field;
    }

    /**
     * @return the rules of a {@link Kind#UNIQUE_VALUES} style by value, in their order. The
     *          values read from a style file are strings.
     */
    public Map<Object, Rule> getValueRules() {
        return Collections.unmodifiableMap(valueRules);
    }

    /**
     * @return the "else" rule of a {@link Kind#UNIQUE_VALUES} style, or <code>null</code>.
     */
    public Rule getOtherRule() {
        return otherRule;
    }

    /**
     * Set the "else" rule of a {@link Kind#UNIQUE_VALUES} style, which draws the features whose
     * value has no rule.
     *
     * @param template the rule to copy as "else" rule, <code>null</code> to have none.
     */
    public void setOtherRule( Rule template ) {
        if (kind != Kind.UNIQUE_VALUES) {
            throw new IllegalStateException("Only unique values styles have a rule for the other values.");
        }
        if (template == null) {
            otherRule = null;
            return;
        }
        otherRule = withoutLabels(template);
        otherRule.setName(OTHER_RULE_NAME);
        otherRule.setFilter(null);
        otherRule.setElseFilter(true);
    }

    /**
     * @return all the rules: the single one, or the ones of the values and then the "else" one.
     */
    public List<Rule> getRules() {
        List<Rule> rules = new ArrayList<>();
        if (singleRule != null) {
            rules.add(singleRule);
        }
        rules.addAll(valueRules.values());
        if (otherRule != null) {
            rules.add(otherRule);
        }
        return rules;
    }

    /**
     * @return the label of all the rules, or <code>null</code> if there is none.
     */
    public TextSymbolizer getLabel() {
        return label;
    }

    /**
     * @param label the label of all the rules, <code>null</code> to have none.
     */
    public void setLabel( TextSymbolizer label ) {
        if (kind == Kind.CUSTOM) {
            throw new IllegalStateException("The label of a custom style can't be set.");
        }
        this.label = label;
    }

    /**
     * Build the style, with the label in all rules.
     *
     * @param name the name of the style.
     * @return the style, the original one for a {@link Kind#CUSTOM} style.
     */
    public Style toStyle( String name ) {
        if (kind == Kind.CUSTOM) {
            return customStyle;
        }
        FeatureTypeStyle featureTypeStyle = sf.createFeatureTypeStyle();
        for( Rule rule : getRules() ) {
            rule.symbolizers().removeIf(s -> s instanceof TextSymbolizer);
            if (label != null) {
                rule.symbolizers().add(label);
            }
            featureTypeStyle.rules().add(rule);
        }
        Style style = sf.createStyle();
        style.setName(name);
        style.featureTypeStyles().add(featureTypeStyle);
        return style;
    }
}
