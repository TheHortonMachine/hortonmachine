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
package org.hortonmachine.dbs.postgis;

import org.hortonmachine.dbs.compat.IGeometryParser;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.postgis.GeometryBuilder;
import org.postgis.PGgeometry;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateXY;
import org.locationtech.jts.geom.CoordinateXYM;
import org.locationtech.jts.geom.CoordinateXYZM;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.WKTReader;

public class PostgisGeometryParser implements IGeometryParser {
    private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();

    @Override
    public Geometry fromResultSet( IHMResultSet rs, int index ) throws Exception {
        Object object = rs.getObject(index);
        if (object instanceof PGgeometry) {
            PGgeometry pgGeometry = (PGgeometry) object;
            Geometry geometry = getGeom(pgGeometry);
            return geometry;
        }
        return null;
    }

    private Geometry getGeom( PGgeometry pgGeometry ) throws Exception {
        try {
            return getGeomFromWkt(pgGeometry);
        } catch (Exception e) {
            return getGeomFromPostgisGeometry(pgGeometry);
        }
    }

    private Geometry getGeomFromWkt( PGgeometry pgGeometry ) throws Exception {
        String wkt = pgGeometry.toString();
        int srid = pgGeometry.getGeometry().getSrid();
        if (wkt.startsWith(GeometryBuilder.SRIDPREFIX)) {
            String[] splitSRID = GeometryBuilder.splitSRID(wkt);
            wkt = splitSRID[1];
            srid = Integer.parseInt(splitSRID[0].substring(GeometryBuilder.SRIDPREFIX.length()));
        }
        Geometry geometry = new WKTReader().read(wkt);
        geometry.setSRID(srid);
        return geometry;
    }

    private Geometry getGeomFromPostgisGeometry( PGgeometry pgGeometry ) {
        org.postgis.Geometry postgisGeometry = pgGeometry.getGeometry();
        Geometry geometry = toJtsGeometry(postgisGeometry);
        geometry.setSRID(postgisGeometry.getSrid());
        return geometry;
    }

    private Geometry toJtsGeometry( org.postgis.Geometry geometry ) {
        switch( geometry.getType() ) {
        case org.postgis.Geometry.POINT:
            return GEOMETRY_FACTORY.createPoint(toCoordinate((org.postgis.Point) geometry));
        case org.postgis.Geometry.LINESTRING:
            return GEOMETRY_FACTORY.createLineString(toCoordinates(((org.postgis.LineString) geometry).getPoints()));
        case org.postgis.Geometry.POLYGON:
            return toJtsPolygon((org.postgis.Polygon) geometry);
        case org.postgis.Geometry.MULTIPOINT:
            return GEOMETRY_FACTORY.createMultiPointFromCoords(toCoordinates(((org.postgis.MultiPoint) geometry).getPoints()));
        case org.postgis.Geometry.MULTILINESTRING:
            return toJtsMultiLineString((org.postgis.MultiLineString) geometry);
        case org.postgis.Geometry.MULTIPOLYGON:
            return toJtsMultiPolygon((org.postgis.MultiPolygon) geometry);
        case org.postgis.Geometry.GEOMETRYCOLLECTION:
            return toJtsGeometryCollection((org.postgis.GeometryCollection) geometry);
        default:
            throw new IllegalArgumentException("Unsupported PostGIS geometry type: " + geometry.getTypeString());
        }
    }

    private Coordinate toCoordinate( org.postgis.Point point ) {
        if (point.haveMeasure) {
            if (point.dimension == 3) {
                return new CoordinateXYZM(point.x, point.y, point.z, point.m);
            }
            return new CoordinateXYM(point.x, point.y, point.m);
        }
        if (point.dimension == 3) {
            return new Coordinate(point.x, point.y, point.z);
        }
        return new CoordinateXY(point.x, point.y);
    }

    private Coordinate[] toCoordinates( org.postgis.Point[] points ) {
        Coordinate[] coordinates = new Coordinate[points.length];
        for( int i = 0; i < points.length; i++ ) {
            coordinates[i] = toCoordinate(points[i]);
        }
        return coordinates;
    }

    private Polygon toJtsPolygon( org.postgis.Polygon polygon ) {
        int numRings = polygon.numRings();
        if (numRings == 0) {
            return GEOMETRY_FACTORY.createPolygon();
        }

        LinearRing shell = toJtsLinearRing(polygon.getRing(0));
        LinearRing[] holes = new LinearRing[numRings - 1];
        for( int i = 1; i < numRings; i++ ) {
            holes[i - 1] = toJtsLinearRing(polygon.getRing(i));
        }
        return GEOMETRY_FACTORY.createPolygon(shell, holes);
    }

    private LinearRing toJtsLinearRing( org.postgis.LinearRing ring ) {
        return GEOMETRY_FACTORY.createLinearRing(toCoordinates(ring.getPoints()));
    }

    private org.locationtech.jts.geom.MultiLineString toJtsMultiLineString( org.postgis.MultiLineString multiLineString ) {
        org.postgis.LineString[] postgisLines = multiLineString.getLines();
        LineString[] lines = new LineString[postgisLines.length];
        for( int i = 0; i < postgisLines.length; i++ ) {
            lines[i] = GEOMETRY_FACTORY.createLineString(toCoordinates(postgisLines[i].getPoints()));
        }
        return GEOMETRY_FACTORY.createMultiLineString(lines);
    }

    private org.locationtech.jts.geom.MultiPolygon toJtsMultiPolygon( org.postgis.MultiPolygon multiPolygon ) {
        org.postgis.Polygon[] postgisPolygons = multiPolygon.getPolygons();
        Polygon[] polygons = new Polygon[postgisPolygons.length];
        for( int i = 0; i < postgisPolygons.length; i++ ) {
            polygons[i] = toJtsPolygon(postgisPolygons[i]);
        }
        return GEOMETRY_FACTORY.createMultiPolygon(polygons);
    }

    private org.locationtech.jts.geom.GeometryCollection toJtsGeometryCollection( org.postgis.GeometryCollection geometryCollection ) {
        org.postgis.Geometry[] postgisGeometries = geometryCollection.getGeometries();
        Geometry[] geometries = new Geometry[postgisGeometries.length];
        for( int i = 0; i < postgisGeometries.length; i++ ) {
            geometries[i] = toJtsGeometry(postgisGeometries[i]);
        }
        return GEOMETRY_FACTORY.createGeometryCollection(geometries);
    }

    @Override
    public Geometry fromSqlObject( Object geomObject ) throws Exception {
        if (geomObject instanceof Geometry) {
            return (Geometry) geomObject;
        } else if (geomObject instanceof PGgeometry) {
            return getGeom((PGgeometry) geomObject);
        }
        throw new IllegalArgumentException("Geom object needs to be a JTS/PGGeometry geometry.");
    }

    @Override
    public Object toSqlObject( Geometry geometry ) throws Exception {
        org.postgis.Geometry pgGeometry = GeometryBuilder.geomFromString(geometry.toText());
        pgGeometry.srid = geometry.getSRID();
        return pgGeometry;
    }

}
