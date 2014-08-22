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

import com.vividsolutions.jts.geom.*;

/**
 * Geometry types used by the utility.
 */
public enum GeometryType {
    POINT(Point.class, MultiPoint.class), //
    MULTIPOINT(MultiPoint.class, MultiPoint.class), //
    LINE(LineString.class, MultiLineString.class), //
    MULTILINE(MultiLineString.class, MultiLineString.class), //
    POLYGON(Polygon.class, MultiPolygon.class), //
    MULTIPOLYGON(MultiPolygon.class, MultiPolygon.class), //
    GEOMETRYCOLLECTION(GeometryCollection.class, GeometryCollection.class), //
    UNKNOWN(null, null);

    private Class<?> clazz;
    private Class<?> multiClazz;

    GeometryType(Class<?> clazz, Class<?> multiClazz) {
        this.clazz = clazz;
        this.multiClazz = multiClazz;
    } //

    public Class<?> getClazz() {
        return clazz;
    }

    public Class<?> getMultiClazz() {
        return multiClazz;
    }

    public static GeometryType forClass(Class<?> clazz) {
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
        switch (this) {
            case MULTILINE:
            case MULTIPOINT:
            case MULTIPOLYGON:
                return true;
            default:
                return false;
        }
    }

    public boolean isCompatibleWith(GeometryType geometryType) {
        switch (geometryType) {
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
}