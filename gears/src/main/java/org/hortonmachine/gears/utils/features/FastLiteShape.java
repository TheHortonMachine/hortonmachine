package org.hortonmachine.gears.utils.features;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import org.geotools.geometry.jts.LiteCoordinateSequence;
import org.geotools.geometry.jts.LiteShape;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;

/**
 * A {@link LiteShape} subclass using PreparedGeometry to compute the results of the containment
 * methods. This class is _not_ thread safe
 * 
 * @author Andrea Aime - GeoSolutions
 * 
 */
public final class FastLiteShape extends LiteShape {

    PreparedGeometry prepared;

    LiteCoordinateSequence pointCS;

    Point point;

    LiteCoordinateSequence rectCS;

    Polygon rect;

    public FastLiteShape( Geometry geom ) {
        super(geom, new AffineTransform(), false);
        this.prepared = PreparedGeometryFactory.prepare(geom);
        GeometryFactory gf = new GeometryFactory();
        pointCS = new LiteCoordinateSequence(1, 2);
        point = gf.createPoint(pointCS);
        rectCS = new LiteCoordinateSequence(5, 2);
        rect = gf.createPolygon(gf.createLinearRing(rectCS), null);
        // System.out.println("Crop area: " + geom);
    }

    @Override
    public boolean contains( double x, double y ) {
        pointCS.setX(0, x);
        pointCS.setY(0, y);
        point.geometryChanged();
        final boolean result = prepared.contains(point);
        // System.out.println("Poking " + x + ", " + y + " -> " + result);
        return result;
    }

    @Override
    public boolean contains( Point2D p ) {
        return contains(p.getX(), p.getY());
    }

    @Override
    public boolean contains( double x, double y, double w, double h ) {
        updateRect(x, y, w, h);
        return prepared.contains(rect);
    }

    private void updateRect( double x, double y, double w, double h ) {
        rectCS.setX(0, x);
        rectCS.setY(0, y);
        rectCS.setX(1, x + w);
        rectCS.setY(1, y);
        rectCS.setX(2, x + w);
        rectCS.setY(2, y + h);
        rectCS.setX(3, x);
        rectCS.setY(3, y + h);
        rectCS.setX(4, x);
        rectCS.setY(4, y);
        rect.geometryChanged();
    }

    @Override
    public boolean contains( Rectangle2D r ) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public boolean intersects( double x, double y, double w, double h ) {
        updateRect(x, y, w, h);
        return prepared.intersects(rect);
    }

    @Override
    public boolean intersects( Rectangle2D r ) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

}
