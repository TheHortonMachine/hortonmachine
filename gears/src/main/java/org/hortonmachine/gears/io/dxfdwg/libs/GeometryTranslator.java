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
package org.hortonmachine.gears.io.dxfdwg.libs;

import java.awt.geom.Point2D;

import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgArc;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgAttrib;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgCircle;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgLine;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgLwPolyline;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgMText;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgPoint;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgPolyline2D;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgPolyline3D;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgSolid;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.objects.DwgText;
import org.hortonmachine.gears.io.dxfdwg.libs.dwg.utils.GisModelCurveCalculator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class GeometryTranslator {
    private static final String LAYER = "layer";
    private static final String THE_GEOM = "the_geom";
    private static GeometryFactory gF = new GeometryFactory();
    private final CoordinateReferenceSystem crs;
    public GeometryTranslator( CoordinateReferenceSystem crs ) {
        this.crs = crs;
    }

    /**
     * Builds a point feature from a dwg text.
     */
    public SimpleFeature convertDwgMText( String typeName, String layerName, DwgMText text, int id ) {
        double[] p = text.getInsertionPoint();
        Point2D pto = new Point2D.Double(p[0], p[1]);
        Coordinate coord = new Coordinate(pto.getX(), pto.getY(), 0.0);
        String textString = text.getText();

        return createPointTextFeature(typeName, layerName, id, coord, textString);
    }

    /**
     * Builds a point feature from a dwg text.
     * 
     */
    public SimpleFeature convertDwgText( String typeName, String layerName, DwgText text, int id ) {
        Point2D pto = text.getInsertionPoint();
        Coordinate coord = new Coordinate(pto.getX(), pto.getY(), 0.0);
        String textString = text.getText();

        return createPointTextFeature(typeName, layerName, id, coord, textString);
    }

    /**
     * Builds a point feature from a dwg attribute.
     */
    public SimpleFeature convertDwgAttribute( String typeName, String layerName,
            DwgAttrib attribute, int id ) {

        Point2D pto = attribute.getInsertionPoint();
        Coordinate coord = new Coordinate(pto.getX(), pto.getY(), attribute.getElevation());
        String textString = attribute.getText();

        return createPointTextFeature(typeName, layerName, id, coord, textString);
    }

    private SimpleFeature createPointTextFeature( String typeName, String layerName, int id,
            Coordinate coord, String textString ) {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(typeName);
        b.setCRS(crs);
        b.add(THE_GEOM, Point.class);
        b.add("text", String.class);
        b.add(LAYER, String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Geometry point = gF.createPoint(coord);
        Object[] values = new Object[]{point, textString, layerName};
        builder.addAll(values);
        return builder.buildFeature(typeName + "." + id);
    }

    /**
     * Builds a line feature from a dwg polyline 3D.
     * 
     * TODO handle these as contourlines
     * 
     */
    public SimpleFeature convertDwgPolyline3D( String typeName, String layerName,
            DwgPolyline3D polyline3d, int id ) {
        double[][] ptos = polyline3d.getPts();
        CoordinateList coordList = new CoordinateList();
        if (ptos != null) {
            for( int j = 0; j < ptos.length; j++ ) {
                Coordinate coord = new Coordinate(ptos[j][0], ptos[j][1], ptos[j][2]);
                coordList.add(coord);
            }

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName(typeName);
            b.setCRS(crs);
            b.add(THE_GEOM, LineString.class);
            b.add(LAYER, String.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Geometry lineString = gF.createLineString(coordList.toCoordinateArray());
            Object[] values = new Object[]{lineString, layerName};
            builder.addAll(values);
            return builder.buildFeature(typeName + "." + id);
        }
        return null;
    }

    /**
     * Builds a line feature from a dwg polyline 2D.
     * 
     */
    public SimpleFeature convertDwgPolyline2D( String typeName, String layerName,
            DwgPolyline2D polyline2d, int id ) {
        Point2D[] ptos = polyline2d.getPts();
        CoordinateList coordList = new CoordinateList();
        if (ptos != null) {
            for( int j = 0; j < ptos.length; j++ ) {
                Coordinate coord = new Coordinate(ptos[j].getX(), ptos[j].getY(), 0.0);
                coordList.add(coord);
            }

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName(typeName);
            b.setCRS(crs);
            b.add(THE_GEOM, LineString.class);
            b.add(LAYER, String.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Geometry lineString = gF.createLineString(coordList.toCoordinateArray());
            Object[] values = new Object[]{lineString, layerName};
            builder.addAll(values);
            return builder.buildFeature(typeName + "." + id);
        }
        return null;
    }

    /**
     * Builds a line feature from a dwg polyline 2D.
     * 
     */
    public SimpleFeature convertDwgLwPolyline( String typeName, String layerName,
            DwgLwPolyline lwPolyline, int id ) {
        Point2D[] ptos = lwPolyline.getVertices();
        if (ptos != null) {
            CoordinateList coordList = new CoordinateList();
            for( int j = 0; j < ptos.length; j++ ) {
                Coordinate coord = new Coordinate(ptos[j].getX(), ptos[j].getY(), 0.0);
                coordList.add(coord);
            }

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName(typeName);
            b.setCRS(crs);
            b.add(THE_GEOM, LineString.class);
            b.add(LAYER, String.class);
            SimpleFeatureType type = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
            Geometry lineString = gF.createLineString(coordList.toCoordinateArray());
            Object[] values = new Object[]{lineString, layerName};
            builder.addAll(values);
            return builder.buildFeature(typeName + "." + id);
        }
        return null;
    }

    /**
     * Builds a point feature from a dwg point.
     */
    public SimpleFeature convertDwgPoint( String typeName, String layerName, DwgPoint point, int id ) {
        double[] p = point.getPoint();
        Point2D pto = new Point2D.Double(p[0], p[1]);

        CoordinateList coordList = new CoordinateList();
        Coordinate coord = new Coordinate(pto.getX(), pto.getY(), 0.0);
        coordList.add(coord);

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(typeName);
        b.setCRS(crs);
        b.add(THE_GEOM, MultiPoint.class);
        b.add(LAYER, String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Geometry points = gF.createMultiPoint(coordList.toCoordinateArray());
        Object[] values = new Object[]{points, layerName};
        builder.addAll(values);
        return builder.buildFeature(typeName + "." + id);
    }

    /**
     * Builds a line feature from a dwg line.
     * 
     */
    public SimpleFeature convertDwgLine( String typeName, String layerName, DwgLine line, int id ) {
        double[] p1 = line.getP1();
        double[] p2 = line.getP2();
        Point2D[] ptos = new Point2D[]{new Point2D.Double(p1[0], p1[1]),
                new Point2D.Double(p2[0], p2[1])};
        CoordinateList coordList = new CoordinateList();
        for( int j = 0; j < ptos.length; j++ ) {
            Coordinate coord = new Coordinate(ptos[j].getX(), ptos[j].getY(), 0.0);
            coordList.add(coord);
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(typeName);
        b.setCRS(crs);
        b.add(THE_GEOM, LineString.class);
        b.add(LAYER, String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Geometry lineString = gF.createLineString(coordList.toCoordinateArray());
        Object[] values = new Object[]{lineString, layerName};
        builder.addAll(values);
        return builder.buildFeature(typeName + "." + id);
    }

    /**
     * Builds a polygon feature from a dwg circle.
     * 
     */
    public SimpleFeature convertDwgCircle( String typeName, String layerName, DwgCircle circle,
            int id ) {
        double[] center = circle.getCenter();
        double radius = circle.getRadius();
        Point2D[] ptos = GisModelCurveCalculator.calculateGisModelCircle(new Point2D.Double(
                center[0], center[1]), radius);
        CoordinateList coordList = new CoordinateList();
        for( int j = 0; j < ptos.length; j++ ) {
            Coordinate coord = new Coordinate(ptos[j].getX(), ptos[j].getY(), 0.0);
            coordList.add(coord);
        }
        // close to create a polygon
        if ((ptos[ptos.length - 1].getX() != ptos[0].getX())
                || (ptos[ptos.length - 1].getY() != ptos[0].getY())) {
            Coordinate coord = new Coordinate(ptos[0].getX(), ptos[0].getY(), 0.0);
            coordList.add(coord);
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(typeName);
        b.setCRS(crs);
        b.add(THE_GEOM, Polygon.class);
        b.add(LAYER, String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        LinearRing linearRing = gF.createLinearRing(coordList.toCoordinateArray());
        Geometry polygon = gF.createPolygon(linearRing, null);
        Object[] values = new Object[]{polygon, layerName};
        builder.addAll(values);
        return builder.buildFeature(typeName + "." + id);
    }

    /**
     * Builds a polygon feature from a dwg solid.
     * 
     */
    public SimpleFeature convertDwgSolid( String typeName, String layerName, DwgSolid solid, int id ) {
        double[] p1 = solid.getCorner1();
        double[] p2 = solid.getCorner2();
        double[] p3 = solid.getCorner3();
        double[] p4 = solid.getCorner4();
        Point2D[] ptos = new Point2D[]{new Point2D.Double(p1[0], p1[1]),
                new Point2D.Double(p2[0], p2[1]), new Point2D.Double(p3[0], p3[1]),
                new Point2D.Double(p4[0], p4[1])};
        CoordinateList coordList = new CoordinateList();
        for( int j = 0; j < ptos.length; j++ ) {
            Coordinate coord = new Coordinate(ptos[j].getX(), ptos[j].getY());
            coordList.add(coord);
        }
        coordList.closeRing();

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(typeName);
        b.setCRS(crs);
        b.add(THE_GEOM, Polygon.class);
        b.add(LAYER, String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        LinearRing linearRing = gF.createLinearRing(coordList.toCoordinateArray());
        Geometry polygon = gF.createPolygon(linearRing, null);
        Object[] values = new Object[]{polygon, layerName};
        builder.addAll(values);
        return builder.buildFeature(typeName + "." + id);
    }

    /**
     * Builds a line feature from a dwg arc.
     */
    public SimpleFeature convertDwgArc( String typeName, String layerName, DwgArc arc, int id ) {
        double[] c = arc.getCenter();
        Point2D center = new Point2D.Double(c[0], c[1]);
        double radius = (arc).getRadius();
        double initAngle = Math.toDegrees((arc).getInitAngle());
        double endAngle = Math.toDegrees((arc).getEndAngle());
        Point2D[] ptos = GisModelCurveCalculator.calculateGisModelArc(center, radius, initAngle,
                endAngle);
        CoordinateList coordList = new CoordinateList();
        for( int j = 0; j < ptos.length; j++ ) {
            Coordinate coord = new Coordinate(ptos[j].getX(), ptos[j].getY(), 0.0);
            coordList.add(coord);
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName(typeName);
        b.setCRS(crs);
        b.add(THE_GEOM, LineString.class);
        b.add(LAYER, String.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        Geometry lineString = gF.createLineString(coordList.toCoordinateArray());
        Object[] values = new Object[]{lineString, layerName};
        builder.addAll(values);
        return builder.buildFeature(typeName + "." + id);
    }
}
