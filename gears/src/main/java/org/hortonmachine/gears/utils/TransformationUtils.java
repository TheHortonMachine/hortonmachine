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

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.util.AffineTransformation;
import org.locationtech.jts.geom.util.NoninvertibleTransformationException;

/**
 * Utils to do transformations.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TransformationUtils {

    /**
     * Get the affine transform that brings from the world envelope to the rectangle. 
     * 
     * @param worldEnvelope the envelope.
     * @param pixelRectangle the destination rectangle.
     * @return the transform.
     */
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

    /**
     * Get the affine transform that brings from the world envelope to the rectangle. 
     * 
     * @param worldEnvelope the envelope.
     * @param pixelRectangle the destination rectangle.
     * @return the transform.
     */
    public static AffineTransformation getWorldToRectangle( Envelope worldEnvelope, Rectangle pixelRectangle ) {
        int cols = (int) pixelRectangle.getWidth();
        int rows = (int) pixelRectangle.getHeight();
        double worldWidth = worldEnvelope.getWidth();
        double worldHeight = worldEnvelope.getHeight();

        double x = -worldEnvelope.getMinX();
        double y = -worldEnvelope.getMinY();
        AffineTransformation translate = AffineTransformation.translationInstance(x, y);
        double xScale = cols / worldWidth;
        double yScale = rows / worldHeight;
        AffineTransformation scale = AffineTransformation.scaleInstance(xScale, yScale);

        int m00 = 1;
        int m10 = 0;
        int m01 = 0;
        int m11 = -1;
        int m02 = 0;
        int m12 = rows;
        AffineTransformation mirror_y = new AffineTransformation(m00, m01, m02, m10, m11, m12);

        AffineTransformation world2pixel = new AffineTransformation(translate);
        world2pixel.compose(scale);
        world2pixel.compose(mirror_y);
        return world2pixel;
    }

    public static AffineTransformation getRectangleToWorld( Rectangle pixelRectangle, Envelope worldEnvelope )
            throws NoninvertibleTransformationException {
        return getWorldToRectangle(worldEnvelope, pixelRectangle).getInverse();
    }

    /**
     * Scale an envelope to have a given width.
     * 
     * @param original the envelope.
     * @param newWidth the new width to use.
     * @return the scaled envelope placed in the original lower left corner position.
     */
    public static Envelope scaleToWidth( Envelope original, double newWidth ) {
        double width = original.getWidth();
        double factor = newWidth / width;

        double newHeight = original.getHeight() * factor;

        return new Envelope(original.getMinX(), original.getMinX() + newWidth, original.getMinY(),
                original.getMinY() + newHeight);
    }

    /**
     * Scale an envelope to have a given height.
     * 
     * @param original the envelope.
     * @param newHeight the new height to use.
     * @return the scaled envelope placed in the original lower left corner position.
     */
    public static Envelope scaleToHeight( Envelope original, double newHeight ) {
        double height = original.getHeight();
        double factor = newHeight / height;

        double newWidth = original.getWidth() * factor;

        return new Envelope(original.getMinX(), original.getMinX() + newWidth, original.getMinY(),
                original.getMinY() + newHeight);
    }

    /**
     * Extend an envelope to have the same ratio as the given width and height.
     * 
     * @param original the envelope.
     * @param width the reference width.
     * @param height the reference height.
     * @return the extended envelope, centered on the original one.
     */
    public static Envelope expandToFitRatio( Envelope original, double width, double height ) {

        double oh = height * original.getWidth() / width;
        double ow;
        if (oh < original.getHeight()) {
            ow = width * original.getHeight() / height;
            oh = original.getHeight();
        } else {
            ow = original.getWidth();
        }

        double expandX = (ow - original.getWidth()) / 2.0;
        double expandY = (oh - original.getHeight()) / 2.0;

        Envelope newEnv = new Envelope(original);
        newEnv.expandBy(expandX, expandY);

        return newEnv;
    }

    /**
     * Given a transformation, transform an envelope by it.
     * 
     * @param transformation the AffineTransform to use.
     * @param env the envelope to transform.
     * @return the transformed envelope.
     */
    public static Envelope transformEnvelope( AffineTransform transformation, Envelope env ) {
        Point2D llFromPoint = new Point2D.Double(env.getMinX(), env.getMinY());
        Point2D urFromPoint = new Point2D.Double(env.getMaxX(), env.getMaxY());
        Point2D ll = new Point2D.Double();
        Point2D ur = new Point2D.Double();
        transformation.transform(llFromPoint, ll);
        transformation.transform(urFromPoint, ur);
        return new Envelope(ll.getX(), ur.getX(), ll.getY(), ur.getY());
    }

    public static Coordinate transformCoordinate( AffineTransform transformation, Coordinate coordinate ) {
        Point2D fromPoint = new Point2D.Double(coordinate.x, coordinate.y);
        Point2D toPoint = new Point2D.Double();
        transformation.transform(fromPoint, toPoint);
        return new Coordinate(toPoint.getX(), toPoint.getY());
    }
    
    /**
     * Given a transformation, transform an envelope by it.
     * 
     * @param transformation the AffineTransformation to use.
     * @param env the envelope to transform.
     * @return the transformed envelope.
     */
    public static Envelope transformEnvelope( AffineTransformation transformation, Envelope env ) {
        Coordinate llFromPoint = new Coordinate(env.getMinX(), env.getMinY());
        Coordinate urFromPoint = new Coordinate(env.getMaxX(), env.getMaxY());
        Coordinate ll = new Coordinate();
        Coordinate ur = new Coordinate();
        transformation.transform(llFromPoint, ll);
        transformation.transform(urFromPoint, ur);
        return new Envelope(ll.getX(), ur.getX(), ll.getY(), ur.getY());
    }

}
