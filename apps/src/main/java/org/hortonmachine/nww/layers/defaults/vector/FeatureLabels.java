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
package org.hortonmachine.nww.layers.defaults.vector;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.utils.style.FeatureStyler;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygonal;
import org.locationtech.jts.geom.Puntal;
import org.locationtech.jts.linearref.LengthIndexedLine;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.render.DrawContext;
import gov.nasa.worldwind.render.GeographicText;
import gov.nasa.worldwind.render.GeographicTextRenderer;
import gov.nasa.worldwind.render.Renderable;
import gov.nasa.worldwind.render.UserFacingText;

/**
 * The labels of the features of a layer, drawn on the terrain, facing the viewer. Labels that
 * would overlap others are not drawn.
 *
 * <p>Each label is placed on its point, at the middle of the longest line, or inside the largest
 * polygon of its feature.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureLabels implements Renderable {

    private final List<GeographicText> texts = new ArrayList<>();
    private final GeographicTextRenderer renderer = new GeographicTextRenderer();

    public FeatureLabels() {
        renderer.setCullTextEnabled(true);
        renderer.setCullTextMargin(2);
        renderer.setEffect(AVKey.TEXT_EFFECT_OUTLINE);
        // the default fades and shrinks distant labels, which makes them unreadable
        renderer.setDistanceMinOpacity(1);
        renderer.setDistanceMinScale(1);
        renderer.setDistanceMaxScale(1);
    }

    /**
     * Add the label of a feature.
     *
     * @param geometry the geometry of the feature, in lat/long.
     * @param label the label.
     */
    public void add( Geometry geometry, FeatureStyler.Label label ) {
        Coordinate anchor = anchor(geometry);
        if (anchor == null) {
            return;
        }
        UserFacingText text = new UserFacingText(label.text(), Position.fromDegrees(anchor.y, anchor.x, 0));
        text.setFont(label.font());
        text.setColor(label.color());
        // the outline effect draws the background color around the text: without a halo it is a
        // thin outline of the text color, which looks as a slightly bold text
        Color halo = label.haloColor();
        text.setBackgroundColor(halo != null ? halo : label.color());
        texts.add(text);
    }

    /**
     * @return <code>true</code> if there are no labels.
     */
    public boolean isEmpty() {
        return texts.isEmpty();
    }

    private static Coordinate anchor( Geometry geometry ) {
        if (geometry == null || geometry.isEmpty()) {
            return null;
        }
        if (geometry instanceof Puntal) {
            return geometry.getCoordinate();
        }
        Geometry largest = null;
        for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
            Geometry part = geometry.getGeometryN(i);
            double size = geometry instanceof Polygonal ? part.getArea() : part.getLength();
            if (largest == null || size > (geometry instanceof Polygonal ? largest.getArea() : largest.getLength())) {
                largest = part;
            }
        }
        if (largest instanceof LineString) {
            LengthIndexedLine indexedLine = new LengthIndexedLine(largest);
            return indexedLine.extractPoint(largest.getLength() / 2);
        }
        return largest.getInteriorPoint().getCoordinate();
    }

    @Override
    public void render( DrawContext dc ) {
        if (!texts.isEmpty()) {
            renderer.render(dc, texts);
        }
    }
}
