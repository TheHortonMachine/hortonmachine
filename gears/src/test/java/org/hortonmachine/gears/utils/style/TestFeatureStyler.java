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
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.style.Style;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.utils.HMTestCase;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class TestFeatureStyler extends HMTestCase {

    private SimpleFeature feature( SimpleFeatureType type, String kind, String name ) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Point point = new GeometryFactory().createPoint(new Coordinate(11, 46));
        return builder.buildFeature(null, new Object[]{point, kind, name});
    }

    public void testUniqueValuesAndLabels() throws Exception {
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("test");
        typeBuilder.add("the_geom", Point.class);
        typeBuilder.add("KIND", String.class);
        typeBuilder.add("NAME", String.class);
        SimpleFeatureType type = typeBuilder.buildFeatureType();

        StyleRules rules = StyleRules.uniqueValues(StyleUtilities.createDefaultRule(EGeometryType.POINT), "KIND",
                List.of("a", "b"), new Color[]{Color.RED, Color.BLUE}, false);
        rules.setLabel(StyleUtilities.createDefaultTextSymbolizer("NAME", EGeometryType.POINT));
        Style style = rules.toStyle("test");

        FeatureStyler styler = new FeatureStyler(style);
        FeatureStyler.Styled a = styler.style(feature(type, "a", "first"));
        assertEquals(Color.RED, a.symbol().fill());
        assertEquals(5.0, a.symbol().size(), 1E-9);
        assertEquals("first", a.label().text());
        assertEquals(12, a.label().font().getSize());
        assertEquals(Color.WHITE, a.label().haloColor());

        assertEquals(Color.BLUE, styler.style(feature(type, "b", "second")).symbol().fill());
        // no rule for "c" and no else rule: not drawn
        assertNull(styler.style(feature(type, "c", "third")));
        // an empty label is no label
        assertNull(styler.style(feature(type, "a", "")).label());

        // with the else rule the other values get the template look
        rules = StyleRules.uniqueValues(StyleUtilities.createDefaultRule(EGeometryType.POINT), "KIND", List.of("a"),
                new Color[]{Color.GREEN}, true);
        styler = new FeatureStyler(rules.toStyle("test"));
        assertEquals(Color.GREEN, styler.style(feature(type, "a", null)).symbol().fill());
        assertEquals(Color.RED, styler.style(feature(type, "c", null)).symbol().fill());
        assertNull(styler.style(feature(type, "c", null)).label());
    }
}
