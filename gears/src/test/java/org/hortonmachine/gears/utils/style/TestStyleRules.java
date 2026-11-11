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
import java.util.Arrays;
import java.util.List;

import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.TextSymbolizer;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.style.StyleRules.Kind;

public class TestStyleRules extends HMTestCase {

    /**
     * Write the style to SLD and read it back, as when it is saved and loaded again.
     */
    private static Style roundTrip( Style style ) throws Exception {
        return SldUtilities.getStyleFromSldString(StyleUtilities.styleToString(style));
    }

    public void testSingleWithLabel() throws Exception {
        StyleRules rules = StyleRules.single(StyleUtilities.createDefaultRule(EGeometryType.MULTIPOLYGON));
        StyleUtilities.setRuleColor(rules.getSingleRule(), Color.GREEN);
        rules.setLabel(StyleUtilities.createDefaultTextSymbolizer("NAME", EGeometryType.MULTIPOLYGON));

        StyleRules read = StyleRules.read(roundTrip(rules.toStyle("test")));
        assertEquals(Kind.SINGLE, read.getKind());
        assertEquals(Color.GREEN, StyleUtilities.getRuleColor(read.getSingleRule()));
        assertNotNull(read.getLabel());
        StyleWrapper wrapper = new StyleWrapper(read.toStyle("test"));
        assertEquals("NAME", wrapper.getFirstTextSymbolizer().getLabelName());
    }

    public void testUniqueValues() throws Exception {
        Rule template = StyleUtilities.createDefaultRule(EGeometryType.LINESTRING);
        Color[] palette = {Color.RED, Color.BLUE};
        // nulls and duplicates get no rule, the palette starts again for the third value
        List<Object> values = Arrays.asList("a", null, "b", "c", "a");
        StyleRules rules = StyleRules.uniqueValues(template, "TYPE", values, palette, true);
        assertEquals(3, rules.getValueRules().size());
        assertEquals(Color.BLUE, StyleUtilities.getRuleColor(rules.getValueRules().get("b")));
        assertEquals(Color.RED, StyleUtilities.getRuleColor(rules.getValueRules().get("c")));
        // the template is copied, not changed
        assertEquals(Color.BLACK, StyleUtilities.getRuleColor(template));

        TextSymbolizer label = StyleUtilities.createDefaultTextSymbolizer("TYPE", EGeometryType.LINESTRING);
        rules.setLabel(label);
        Style style = rules.toStyle("test");
        assertEquals(4, style.featureTypeStyles().get(0).rules().size());

        StyleRules read = StyleRules.read(roundTrip(style));
        assertEquals(Kind.UNIQUE_VALUES, read.getKind());
        assertEquals("TYPE", read.getField());
        assertEquals(Arrays.asList("a", "b", "c"), List.copyOf(read.getValueRules().keySet()));
        assertEquals(Color.BLUE, StyleUtilities.getRuleColor(read.getValueRules().get("b")));
        assertNotNull(read.getOtherRule());
        assertTrue(read.getOtherRule().isElseFilter());
        assertNotNull(read.getLabel());
    }

    public void testNumericValuesAndNoOther() throws Exception {
        Rule template = StyleUtilities.createDefaultRule(EGeometryType.POINT);
        StyleRules rules = StyleRules.uniqueValues(template, "CODE", List.of(1, 2), new Color[]{Color.CYAN}, false);
        StyleRules read = StyleRules.read(roundTrip(rules.toStyle("test")));
        assertEquals(Kind.UNIQUE_VALUES, read.getKind());
        assertNull(read.getOtherRule());
        assertNull(read.getLabel());
        assertEquals(2, read.getValueRules().size());
        assertEquals(Color.CYAN, StyleUtilities.getRuleColor(read.getRules().get(1)));
    }

    public void testCustomStylesAreKept() throws Exception {
        // a range rule
        Style ranges = HMStyle.polygon().fill("red", 0.5).where("AREA > 10").fill("blue", 0.5).where("AREA <= 10").build();
        StyleRules read = StyleRules.read(roundTrip(ranges));
        assertEquals(Kind.CUSTOM, read.getKind());
        assertNotNull(read.toStyle("ignored"));

        // a scale limit
        Rule rule = StyleUtilities.createDefaultRule(EGeometryType.POLYGON);
        rule.setMaxScaleDenominator(10000);
        Style limited = StyleUtilities.rulesToStyle(List.of(rule), "limited", false);
        assertEquals(Kind.CUSTOM, StyleRules.read(roundTrip(limited)).getKind());
    }
}
