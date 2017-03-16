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

package org.jgrasstools.gears.utils.geometry;

import org.jgrasstools.dbs.spatialite.ESpatialiteGeometryType;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.*;

/**
 * Geometry types used by the utility.
 */
public enum EGeometryType {
    POINT(Point.class, MultiPoint.class), //
    MULTIPOINT(MultiPoint.class, MultiPoint.class), //
    LINE(LineString.class, MultiLineString.class), //
    MULTILINE(MultiLineString.class, MultiLineString.class), //
    POLYGON(Polygon.class, MultiPolygon.class), //
    MULTIPOLYGON(MultiPolygon.class, MultiPolygon.class), //
    GEOMETRYCOLLECTION(GeometryCollection.class, GeometryCollection.class), //
    UNKNOWN(null, null);

    private Class< ? > clazz;
    private Class< ? > multiClazz;

    EGeometryType( Class< ? > clazz, Class< ? > multiClazz ) {
        this.clazz = clazz;
        this.multiClazz = multiClazz;
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
        } else if (LINE.getClazz().isAssignableFrom(clazz)) {
            return LINE;
        } else if (MULTILINE.getClazz().isAssignableFrom(clazz)) {
            return MULTILINE;
        } else if (POLYGON.getClazz().isAssignableFrom(clazz)) {
            return POLYGON;
        } else if (MULTIPOLYGON.getClazz().isAssignableFrom(clazz)) {
            return MULTIPOLYGON;
        } else if (GEOMETRYCOLLECTION.getClazz().isAssignableFrom(clazz)) {
            return GEOMETRYCOLLECTION;
        } else {
            return null;
        }
    }

    public boolean isMulti() {
        switch( this ) {
        case MULTILINE:
        case MULTIPOINT:
        case MULTIPOLYGON:
            return true;
        default:
            return false;
        }
    }

    public boolean isCompatibleWith( EGeometryType geometryType ) {
        switch( geometryType ) {
        case LINE:
            return this == LINE;
        case MULTILINE:
            return this == LINE || this == MULTILINE;
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
            return EGeometryType.LINE;
        } else if (geometry instanceof MultiLineString) {
            return EGeometryType.MULTILINE;
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
            return null;
        }
    }

    /**
     * Returns the {@link EGeometryType} for a given {@link org.opengis.feature.type.GeometryType}.
     * 
     * @param geometryType the geometry type to check.
     * @return the type.
     */
    public static EGeometryType forGeometryType( org.opengis.feature.type.GeometryType geometryType ) {
        Class< ? > binding = geometryType.getBinding();

        if (binding == LineString.class) {
            return EGeometryType.LINE;
        } else if (binding == MultiLineString.class) {
            return EGeometryType.MULTILINE;
        } else if (binding == Point.class) {
            return EGeometryType.POINT;
        } else if (binding == MultiPoint.class) {
            return EGeometryType.MULTIPOINT;
        } else if (binding == Polygon.class) {
            return EGeometryType.POLYGON;
        } else if (binding == MultiPolygon.class) {
            return EGeometryType.MULTIPOLYGON;
        } else {
            return null;
        }
    }

    /**
     * Returns the {@link EGeometryType} for a given {@link GeometryDescriptor}.
     * 
     * @param geometryDescriptor the geometry descriptor to check.
     * @return the type.
     */
    public static EGeometryType forGeometryDescriptor( GeometryDescriptor geometryDescriptor ) {
        return forGeometryType(geometryDescriptor.getType());
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
     * Checks if the given {@link GeometryDescriptor} is for {@link LineString} (or {@link MultiLineString}) geometry.
     * 
     * @param geometryDescriptor the descriptor.
     * @return <code>true</code> if there are points in there.
     */
    public static boolean isLine( GeometryDescriptor geometryDescriptor ) {
        org.opengis.feature.type.GeometryType type = geometryDescriptor.getType();
        Class< ? > binding = type.getBinding();
        if (binding == MultiLineString.class || binding == LineString.class) {
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
     * Checks if the given {@link GeometryDescriptor} is for {@link Polygon} (or {@link MultiPolygon}) geometry.
     * 
     * @param geometryDescriptor the descriptor.
     * @return <code>true</code> if there are polygons in there.
     */
    public static boolean isPolygon( GeometryDescriptor geometryDescriptor ) {
        org.opengis.feature.type.GeometryType type = geometryDescriptor.getType();
        Class< ? > binding = type.getBinding();
        if (binding == MultiPolygon.class || binding == Polygon.class) {
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
     * Checks if the given {@link GeometryDescriptor} is for {@link Point} (or {@link MultiPoint}) geometry.
     * 
     * @param geometryDescriptor the descriptor.
     * @return <code>true</code> if there are points in there.
     */
    public static boolean isPoint( GeometryDescriptor geometryDescriptor ) {
        org.opengis.feature.type.GeometryType type = geometryDescriptor.getType();
        Class< ? > binding = type.getBinding();
        if (binding == MultiPoint.class || binding == Point.class) {
            return true;
        }
        return false;
    }
    
    public ESpatialiteGeometryType toSpatialiteGeometryType(){
        switch( this ) {
        case LINE:
            return ESpatialiteGeometryType.LINESTRING_XY;
        case MULTILINE:
            return ESpatialiteGeometryType.MULTILINESTRING_XY;
        case POINT:
            return ESpatialiteGeometryType.POINT_XY;
        case MULTIPOINT:
            return ESpatialiteGeometryType.MULTIPOINT_XY;
        case POLYGON:
            return ESpatialiteGeometryType.POLYGON_XY;
        case MULTIPOLYGON:
            return ESpatialiteGeometryType.MULTIPOLYGON_XY;
        case GEOMETRYCOLLECTION:
            return ESpatialiteGeometryType.GEOMETRYCOLLECTION_XY;
        default:
            return null;
        }
    }
}