/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.utils;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Utils to do transformations.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TransformationUtils {

    public static AffineTransform getWorldToPixel( Envelope worldEnvelope, Rectangle pixelRectangle ) {
        double width = pixelRectangle.getWidth();
        double worldWidth = worldEnvelope.getWidth();
        double height = pixelRectangle.getHeight();
        double worldHeight = worldEnvelope.getHeight();

        AffineTransform translate = AffineTransform.getTranslateInstance(-worldEnvelope.getMinX(), -worldEnvelope.getMinY());
        AffineTransform scale = AffineTransform.getScaleInstance(width / worldWidth, height / worldHeight);
        AffineTransform mirror_y = new AffineTransform(1, 0, 0, -1, 0, pixelRectangle.getHeight());
        AffineTransform world2pixel = new AffineTransform(mirror_y);
        world2pixel.concatenate(scale);
        world2pixel.concatenate(translate);
        return world2pixel;
    }

    public static AffineTransform getPixelToWorld( Rectangle pixelRectangle, Envelope worldEnvelope )
            throws NoninvertibleTransformException {
        return getWorldToPixel(worldEnvelope, pixelRectangle).createInverse();
    }


}
