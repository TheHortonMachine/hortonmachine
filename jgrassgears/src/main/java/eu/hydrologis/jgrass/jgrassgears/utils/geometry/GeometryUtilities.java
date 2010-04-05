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
package eu.hydrologis.jgrass.jgrassgears.utils.geometry;

import static java.lang.Math.atan;
import static java.lang.Math.toDegrees;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineSegment;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.geom.PrecisionModel;

/**
 * Utilities related to {@link Geometry}.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeometryUtilities {

    private static GeometryFactory geomFactory;
    private static PrecisionModel precModel;

    public static PrecisionModel basicPrecisionModel() {
        return (pm());
    }

    public static GeometryFactory gf() {
        if (geomFactory == null) {
            geomFactory = new GeometryFactory();
        }
        return (geomFactory);
    }

    public static PrecisionModel pm() {
        if (precModel == null) {
            precModel = new PrecisionModel();
        }
        return (precModel);
    }
    
    /**
     * Create a simple polygon (no holes).
     * 
     * @param coords the coords of the polygon.
     * @return the {@link Polygon}.
     */
    public static Polygon createSimplePolygon(Coordinate[] coords){
        LinearRing linearRing = gf().createLinearRing(coords);
        return gf().createPolygon(linearRing, null);
    }

    /**
     * Creates a polygon that may help out as placeholder.
     * 
     * @return a dummy {@link Polygon}.
     */
    public static Polygon createDummyPolygon(){
        Coordinate[] c = new Coordinate[]{
                new Coordinate(0.0,0.0),
                new Coordinate(1.0,1.0),
                new Coordinate(1.0,0.0),
                new Coordinate(0.0,0.0)
        };
        LinearRing linearRing = gf().createLinearRing(c);
        return gf().createPolygon(linearRing, null);
    }

    public static Polygon createPolygonFromEnvelope(Envelope env){
        Coordinate[] c = new Coordinate[]{
                new Coordinate(env.getMinX(),env.getMinY()),
                new Coordinate(env.getMinX(),env.getMaxY()),
                new Coordinate(env.getMaxX(),env.getMaxY()),
                new Coordinate(env.getMaxX(),env.getMinY()),
                new Coordinate(env.getMinX(),env.getMinY())
        };
        LinearRing linearRing = gf().createLinearRing(c);
        return gf().createPolygon(linearRing, null);
    }

    public static double angleBetween( LineSegment l1, LineSegment l2 ) {
        double tol = 0.00001;
        // analyze slopes
        double s1 = (l1.p1.y - l1.p0.y) / (l1.p1.x - l1.p0.x);
        double s2 = (l2.p1.y - l2.p0.y) / (l2.p1.x - l2.p0.x);

        if (Math.abs(s1 - s2) < tol)
            return (0);
        if (Math.abs(s1 + s2) < tol)
            return (Math.PI);

        // not of equal slope, transform lines so that they are tail to tip and
        // use the cosine law to calculate angle between

        // transform line segments tail to tail, originating at (0,0)
        LineSegment tls1 = new LineSegment(new Coordinate(0, 0), new Coordinate(l1.p1.x - l1.p0.x, l1.p1.y - l1.p0.y));
        LineSegment tls2 = new LineSegment(new Coordinate(0, 0), new Coordinate(l2.p1.x - l2.p0.x, l2.p1.y - l2.p0.y));

        // line segment for third side of triangle
        LineSegment ls3 = new LineSegment(tls1.p1, tls2.p1);

        double c = ls3.getLength();
        double a = tls1.getLength();
        double b = tls2.getLength();

        return (Math.acos((a * a + b * b - c * c) / (2 * a * b)));
    }

    /**
     * Calculates the azimuth in degrees given two {@link Coordinate} composing a line.
     * 
     * Note that the coords order is important and will differ of 180.
     * 
     * @param c1 first coordinate (used as origin).
     * @param c2 second coordinate.
     * @return the azimuth angle.
     */
    public static double azimuth( Coordinate c1, Coordinate c2 ) {
        // vertical
        if (c1.x == c2.x) {
            if (c1.y == c2.y) {
                // same point
                return Double.NaN;
            } else if (c1.y < c2.y) {
                return 0.0;
            } else if (c1.y > c2.y) {
                return 180.0;
            }
        }
        // horiz
        if (c1.y == c2.y) {
            if (c1.x < c2.x) {
                return 90.0;
            } else if (c1.x > c2.x) {
                return 270.0;
            }
        }
        // -> /
        if (c1.x < c2.x && c1.y < c2.y) {
            double tanA = (c2.x - c1.x) / (c2.y - c1.y);
            double atan = atan(tanA);
            return toDegrees(atan);
        }
        // -> \
        if (c1.x < c2.x && c1.y > c2.y) {
            double tanA = (c1.y - c2.y) / (c2.x - c1.x);
            double atan = atan(tanA);
            return toDegrees(atan) + 90.0;
        }
        // <- /
        if (c1.x > c2.x && c1.y > c2.y) {
            double tanA = (c1.x - c2.x) / (c1.y - c2.y);
            double atan = atan(tanA);
            return toDegrees(atan) + 180;
        }
        // <- \
        if (c1.x > c2.x && c1.y < c2.y) {
            double tanA = (c2.y - c1.y) / (c1.x - c2.x);
            double atan = atan(tanA);
            return toDegrees(atan) + 270;
        }

        return Double.NaN;
    }

}
