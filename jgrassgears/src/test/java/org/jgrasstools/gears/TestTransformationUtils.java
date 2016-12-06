package org.jgrasstools.gears;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import org.jgrasstools.gears.utils.HMTestCase;
import org.jgrasstools.gears.utils.TransformationUtils;

import com.vividsolutions.jts.geom.Envelope;

/**
 * Test transformations.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TestTransformationUtils extends HMTestCase {

    public void testTransformationUtils() throws Exception {
        Envelope env = new Envelope(100, 200, 1000, 5000);
        Rectangle rect = new Rectangle(0, 0, 100, 4000);

        AffineTransform worldToPixel = TransformationUtils.getWorldToPixel(env, rect);

        Point2D srcPt = new Point2D.Double(150.0, 3000.0);
        Point2D transformed = worldToPixel.transform(srcPt, null);
        assertEquals(50, (int) transformed.getX());
        assertEquals(2000, (int) transformed.getY());

        srcPt = new Point2D.Double(100.0, 1000.0);
        transformed = worldToPixel.transform(srcPt, null);
        assertEquals(0, (int) transformed.getX());
        assertEquals(4000, (int) transformed.getY());
    }
}
