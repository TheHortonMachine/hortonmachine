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
package org.jgrasstools.gears.utils.style;

import java.awt.Color;

import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;

/**
 * Simple styles generator.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("nls")
public class SimpleStyles {
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
    public static Style createSimpleLineStyle(Color color, float width) {
        FeatureTypeStyle featureTypeStyle = Utilities.sf.createFeatureTypeStyle();
        featureTypeStyle.rules().add(createSimpleLineRule(color, width));

        Style style = Utilities.sf.createStyle();
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
    public static Rule createSimpleLineRule(Color color, float width) {
        LineSymbolizer lineSymbolizer = Utilities.sf.createLineSymbolizer();
        lineSymbolizer.setStroke(
                Utilities.sf.createStroke(Utilities.ff.literal("#" + Integer.toHexString(color.getRGB() & 0xffffff)),
                        Utilities.ff.literal(width)));

        Rule rule = Utilities.sf.createRule();
        rule.setName("New rule");
        rule.symbolizers().add(lineSymbolizer);

        return rule;
    }
}
