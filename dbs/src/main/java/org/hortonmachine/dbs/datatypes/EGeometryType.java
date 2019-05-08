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

package org.hortonmachine.dbs.datatypes;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryCollection;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

/**
 * Geometry types used by the utility.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum EGeometryType {
    POINT(Point.class, MultiPoint.class, "Point"), //
    MULTIPOINT(MultiPoint.class, MultiPoint.class, "MultiPoint"), //
    LINESTRING(LineString.class, MultiLineString.class, "LineString"), //
    MULTILINESTRING(MultiLineString.class, MultiLineString.class, "MultiLineString"), //
    POLYGON(Polygon.class, MultiPolygon.class, "Polygon"), //
    MULTIPOLYGON(MultiPolygon.class, MultiPolygon.class, "MultiPolygon"), //
    GEOMETRYCOLLECTION(GeometryCollection.class, GeometryCollection.class, "GeometryCollection"), //
    GEOMETRY(Geometry.class, Geometry.class, "GEOMETRY"), //
    UNKNOWN(null, null, "Unknown");

    private Class< ? > clazz;
    private Class< ? > multiClazz;
    private String typeName;

    EGeometryType( Class< ? > clazz, Class< ? > multiClazz, String typeName ) {
        this.clazz = clazz;
        this.multiClazz = multiClazz;
        this.typeName = typeName;
    } //

    public Class< ? > getClazz() {
        return clazz;
    }

    public Class< ? > getMultiClazz() {
        return multiClazz;
    }

    public static EGeometryType forClass( Class< ? > clazz ) {
        if (POINT.getClazz().isAssignableFrom(clazz)) {
            return POINT;
        } else if (MULTIPOINT.getClazz().isAssignableFrom(clazz)) {
            return MULTIPOINT;
        } else if (LINESTRING.getClazz().isAssignableFrom(clazz)) {
            return LINESTRING;
        } else if (MULTILINESTRING.getClazz().isAssignableFrom(clazz)) {
            return MULTILINESTRING;
        } else if (POLYGON.getClazz().isAssignableFrom(clazz)) {
            return POLYGON;
        } else if (MULTIPOLYGON.getClazz().isAssignableFrom(clazz)) {
            return MULTIPOLYGON;
        } else if (GEOMETRYCOLLECTION.getClazz().isAssignableFrom(clazz)) {
            return GEOMETRYCOLLECTION;
        } else if (GEOMETRY.getClazz().isAssignableFrom(clazz)) {
            return GEOMETRY;
        } else {
            return UNKNOWN;
        }
    }

    public boolean isMulti() {
        switch( this ) {
        case MULTILINESTRING:
        case MULTIPOINT:
        case MULTIPOLYGON:
            return true;
        default:
            return false;
        }
    }

    public boolean isPoint() {
        switch( this ) {
        case MULTIPOINT:
        case POINT:
            return true;
        default:
            return false;
        }
    }

    public boolean isLine() {
        switch( this ) {
        case MULTILINESTRING:
        case LINESTRING:
            return true;
        default:
            return false;
        }
    }

    public boolean isPolygon() {
        switch( this ) {
        case MULTIPOLYGON:
        case POLYGON:
            return true;
        default:
            return false;
        }
    }

    public boolean isCompatibleWith( EGeometryType geometryType ) {
        switch( geometryType ) {
        case LINESTRING:
            return this == LINESTRING;
        case MULTILINESTRING:
            return this == LINESTRING || this == MULTILINESTRING;
        case POINT:
            return this == POINT;
        case MULTIPOINT:
            return this == POINT || this == MULTIPOINT;
        case POLYGON:
            return this == POLYGON;
        case MULTIPOLYGON:
            return this == POLYGON || this == MULTIPOLYGON;
        default:
            return false;
        }
    }

    /**
     * Returns the {@link EGeometryType} for a given {@link Geometry}.
     * 
     * @param geometry the geometry to check.
     * @return the type.
     */
    public static EGeometryType forGeometry( Geometry geometry ) {
        if (geometry instanceof LineString) {
            return EGeometryType.LINESTRING;
        } else if (geometry instanceof MultiLineString) {
            return EGeometryType.MULTILINESTRING;
        } else if (geometry instanceof Point) {
            return EGeometryType.POINT;
        } else if (geometry instanceof MultiPoint) {
            return EGeometryType.MULTIPOINT;
        } else if (geometry instanceof Polygon) {
            return EGeometryType.POLYGON;
        } else if (geometry instanceof MultiPolygon) {
            return EGeometryType.MULTIPOLYGON;
        } else if (geometry instanceof GeometryCollection) {
            return EGeometryType.GEOMETRYCOLLECTION;
        } else {
            return EGeometryType.GEOMETRY;
        }
    }

    public static EGeometryType forWktName( String wktName ) {
        if (wktName.equalsIgnoreCase(POINT.name())) {
            return POINT;
        } else if (wktName.equalsIgnoreCase(MULTIPOINT.name())) {
            return MULTIPOINT;
        } else if (wktName.equalsIgnoreCase(LINESTRING.name())) {
            return LINESTRING;
        } else if (wktName.equalsIgnoreCase(MULTILINESTRING.name())) {
            return MULTILINESTRING;
        } else if (wktName.equalsIgnoreCase(POLYGON.name())) {
            return POLYGON;
        } else if (wktName.equalsIgnoreCase(MULTIPOLYGON.name())) {
            return MULTIPOLYGON;
        } else if (wktName.equalsIgnoreCase(GEOMETRYCOLLECTION.name())) {
            return GEOMETRYCOLLECTION;
        } else if (wktName.equalsIgnoreCase(GEOMETRY.name())) {
            return GEOMETRY;
        }
        return UNKNOWN;
    }
    
    public static EGeometryType forTypeName( String typeName ) {
        return forWktName(typeName);
    }

    /**
     * Checks if the given geometry is a {@link LineString} (or {@link MultiLineString}) geometry.
     * 
     * @param geometry the geometry to check.
     * @return <code>true</code> if there are lines in there.
     */
    public static boolean isLine( Geometry geometry ) {
        if (geometry instanceof LineString || geometry instanceof MultiLineString) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the given geometry is a {@link Polygon} (or {@link MultiPolygon}) geometry.
     * 
     * @param geometry the geometry to check.
     * @return <code>true</code> if there are polygons in there.
     */
    public static boolean isPolygon( Geometry geometry ) {
        if (geometry instanceof Polygon || geometry instanceof MultiPolygon) {
            return true;
        }
        return false;
    }

    /**
     * Checks if the given geometry is a {@link Point} (or {@link MultiPoint}) geometry.
     * 
     * @param geometry the geometry to check.
     * @return <code>true</code> if there are points in there.
     */
    public static boolean isPoint( Geometry geometry ) {
        if (geometry instanceof Point || geometry instanceof MultiPoint) {
            return true;
        }
        return false;
    }

    /**
     * Returns the base geometry type for a spatialite geometries types.
     * 
     * @param value the code.
     * @return the type.
     */
    public static EGeometryType fromGeometryTypeCode( int value ) {

        switch( value ) {
        case 0:
            return GEOMETRY;
        case 1:
            return POINT;
        case 2:
            return LINESTRING;
        case 3:
            return POLYGON;
        case 4:
            return MULTIPOINT;
        case 5:
            return MULTILINESTRING;
        case 6:
            return MULTIPOLYGON;
        case 7:
            return GEOMETRYCOLLECTION;
        /*
         * XYZ
         */
        case 1000:
            return GEOMETRY;
        case 1001:
            return POINT;
        case 1002:
            return LINESTRING;
        case 1003:
            return POLYGON;
        case 1004:
            return MULTIPOINT;
        case 1005:
            return MULTILINESTRING;
        case 1006:
            return MULTIPOLYGON;
        case 1007:
            return GEOMETRYCOLLECTION;
        /*
         * XYM
         */
        case 2000:
            return GEOMETRY;
        case 2001:
            return POINT;
        case 2002:
            return LINESTRING;
        case 2003:
            return POLYGON;
        case 2004:
            return MULTIPOINT;
        case 2005:
            return MULTILINESTRING;
        case 2006:
            return MULTIPOLYGON;
        case 2007:
            return GEOMETRYCOLLECTION;
        /*
         * XYZM
         */
        case 3000:
            return GEOMETRY;
        case 3001:
            return POINT;
        case 3002:
            return LINESTRING;
        case 3003:
            return POLYGON;
        case 3004:
            return MULTIPOINT;
        case 3005:
            return MULTILINESTRING;
        case 3006:
            return MULTIPOLYGON;
        case 3007:
            return GEOMETRYCOLLECTION;
        default:
            break;
        }
        return UNKNOWN;

    }

    public ESpatialiteGeometryType toSpatialiteGeometryType() {
        switch( this ) {
        case LINESTRING:
            return ESpatialiteGeometryType.LINESTRING_XY;
        case MULTILINESTRING:
            return ESpatialiteGeometryType.MULTILINESTRING_XY;
        case POINT:
            return ESpatialiteGeometryType.POINT_XY;
        case MULTIPOINT:
            return ESpatialiteGeometryType.MULTIPOINT_XY;
        case POLYGON:
            return ESpatialiteGeometryType.POLYGON_XY;
        case MULTIPOLYGON:
            return ESpatialiteGeometryType.MULTIPOLYGON_XY;
        case GEOMETRY:
            return ESpatialiteGeometryType.GEOMETRY_XY;
        case GEOMETRYCOLLECTION:
            return ESpatialiteGeometryType.GEOMETRYCOLLECTION_XY;
        default:
            return null;
        }
    }

    public String getTypeName() {
        return typeName;
    }
}