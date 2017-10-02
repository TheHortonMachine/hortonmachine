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
package org.hortonmachine.nww.layers.defaults.annotations;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.render.AnnotationAttributes;
import gov.nasa.worldwind.render.ScreenAnnotation;

/**
 * Html text screen annotation builder.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class HtmlScreenAnnotation extends ScreenAnnotation {

    public HtmlScreenAnnotation( String text, Point position ) {
        super(text, position);

    }

    public static class Builder {
        private String htmlText = "";
        private Color textColor = Color.BLACK;

        private Color backgroundColor = new Color(1f, 1f, 1f, .5f);

        private Insets insets = new Insets(8, 8, 8, 8);
        private int cornerRadius = 10;

        private Point drawOffset = new Point(0, 0);
        private Point position = new Point(0, 0);
        private Dimension size = new Dimension(200, 0);

        private double hightlightScale = 1;

        public Builder htmlText( String htmlText ) {
            this.htmlText = htmlText;
            return this;
        }

        public Builder textColor( Color textColor ) {
            this.textColor = textColor;
            return this;
        }
        public Builder backgroundColor( Color backgroundColor ) {
            this.backgroundColor = backgroundColor;
            return this;
        }
        public Builder insets( Insets insets ) {
            this.insets = insets;
            return this;
        }
        public Builder cornerRadius( int cornerRadius ) {
            this.cornerRadius = cornerRadius;
            return this;
        }
        public Builder drawOffset( Point drawOffset ) {
            this.drawOffset = drawOffset;
            return this;
        }
        public Builder position( Point position ) {
            this.position = position;
            return this;
        }
        public Builder size( Dimension size ) {
            this.size = size;
            return this;
        }
        public Builder hightlightScale( double hightlightScale ) {
            this.hightlightScale = hightlightScale;
            return this;
        }

        public HtmlScreenAnnotation build() {
            AnnotationAttributes defaultAttributes = new AnnotationAttributes();
            defaultAttributes.setCornerRadius(cornerRadius);
            defaultAttributes.setInsets(insets);
            defaultAttributes.setBackgroundColor(backgroundColor);
            defaultAttributes.setTextColor(textColor);
            defaultAttributes.setDrawOffset(drawOffset);
            defaultAttributes.setDistanceMinScale(.5);
            defaultAttributes.setDistanceMaxScale(2);
            defaultAttributes.setDistanceMinOpacity(.5);
            defaultAttributes.setLeaderGapWidth(14);

            HtmlScreenAnnotation sa = new HtmlScreenAnnotation(htmlText, position);
            sa.getAttributes().setDefaults(defaultAttributes);
            sa.getAttributes().setSize(size);
            sa.getAttributes().setAdjustWidthToText(AVKey.SIZE_FIXED);
            sa.getAttributes().setHighlightScale(hightlightScale);

            return sa;
        }
    }

}
