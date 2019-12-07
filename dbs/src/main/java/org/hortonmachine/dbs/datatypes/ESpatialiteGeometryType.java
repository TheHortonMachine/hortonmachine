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
 * Geometry types.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum ESpatialiteGeometryType {
    /*
     * XY
     */
    GEOMETRY_XY(0, "geometry_xy", null, "CastToXY", null, Geometry.class), //
    POINT_XY(1, "point_xy", "CastToPoint", "CastToXY", "CastToSingle", Point.class), //
    LINESTRING_XY(2, "linestring_xy", "CastToLinestring", "CastToXY", "CastToSingle", LineString.class), //
    POLYGON_XY(3, "polygon_xy", "CastToPolygon", "CastToXY", "CastToSingle", Polygon.class), //
    MULTIPOINT_XY(4, "multipoint_xy", "CastToMultiPoint", "CastToXY", "CastToMulti", MultiPoint.class), //
    MULTILINESTRING_XY(5, "multilinestring_xy", "CastToMultiLinestring", "CastToXY", "CastToMulti", MultiLineString.class), //
    MULTIPOLYGON_XY(6, "multipolygon_xy", "CastToMultiPolygon", "CastToXY", "CastToMulti", MultiPolygon.class), //
    GEOMETRYCOLLECTION_XY(7, "geometrycollection_xy", "CastToGeometyCollection", "CastToXY", null, GeometryCollection.class), //
    /*
     * XYZ
     */
    GEOMETRY_XYZ(1000, "geometry_xyz", null, "CastToXYZ", null, Geometry.class), //
    POINT_XYZ(1001, "point_xyz", "CastToPoint", "CastToXYZ", "CastToSingle", Point.class), //
    LINESTRING_XYZ(1002, "linestring_xyz", "CastToLinestring", "CastToXYZ", "CastToSingle", LineString.class), //
    POLYGON_XYZ(1003, "polygon_xyz", "CastToPolygon", "CastToXYZ", "CastToSingle", Polygon.class), //
    MULTIPOINT_XYZ(1004, "multipoint_xyz", "CastToMultiPoint", "CastToXYZ", "CastToMulti", MultiPoint.class), //
    MULTILINESTRING_XYZ(1005, "multilinestring_xyz", "CastToMultiLinestring", "CastToXYZ", "CastToMulti", MultiLineString.class), //
    MULTIPOLYGON_XYZ(1006, "multipolygon_xyz", "CastToMultiPolygon", "CastToXYZ", "CastToMulti", MultiPolygon.class), //
    GEOMETRYCOLLECTION_XYZ(1007, "geometrycollection_xyz", "CastToGeometyCollection", "CastToXYZ", null,
            GeometryCollection.class), //
    /*
     * XYM
     */
    GEOMETRY_XYM(2000, "geometry_xym", null, "CastToXYM", null, Geometry.class), //
    POINT_XYM(2001, "point_xym", "CastToPoint", "CastToXYM", "CastToSingle", Point.class), //
    LINESTRING_XYM(2002, "linestring_xym", "CastToLinestring", "CastToXYM", "CastToSingle", LineString.class), //
    POLYGON_XYM(2003, "polygon_xym", "CastToPolygon", "CastToXYM", "CastToSingle", Polygon.class), //
    MULTIPOINT_XYM(2004, "multipoint_xym", "CastToMultiPoint", "CastToXYM", "CastToMulti", MultiPoint.class), //
    MULTILINESTRING_XYM(2005, "multilinestring_xym", "CastToMultiLinestring", "CastToXYM", "CastToMulti", MultiLineString.class), //
    MULTIPOLYGON_XYM(2006, "multipolygon_xym", "CastToMultiPolygon", "CastToXYM", "CastToMulti", MultiPolygon.class), //
    GEOMETRYCOLLECTION_XYM(2007, "geometrycollection_xym", "CastToGeometyCollection", "CastToXYM", null,
            GeometryCollection.class), //
    /*
     * XYZM
     */
    GEOMETRY_XYZM(3000, "geometry_xyzm", null, "CastToXYZM", null, Geometry.class), //
    POINT_XYZM(3001, "point_xyzm", "CastToPoint", "CastToXYZM", "CastToSingle", Point.class), //
    LINESTRING_XYZM(3002, "linestring_xyzm", "CastToLinestring", "CastToXYZM", "CastToSingle", LineString.class), //
    POLYGON_XYZM(3003, "polygon_xyzm", "CastToPolygon", "CastToXYZM", "CastToSingle", Polygon.class), //
    MULTIPOINT_XYZM(3004, "multipoint_xyzm", "CastToMultiPoint", "CastToXYZM", "CastToMulti", MultiPoint.class), //
    MULTILINESTRING_XYZM(3005, "multilinestring_xyzm", "CastToMultiLinestring", "CastToXYZM", "CastToMulti",
            MultiLineString.class), //
    MULTIPOLYGON_XYZM(3006, "multipolygon_xyzm", "CastToMultiPolygon", "CastToXYZM", "CastToMulti", MultiPolygon.class), //
    GEOMETRYCOLLECTION_XYZM(3007, "geometrycollection_xyzm", "CastToGeometyCollection", "CastToXYZM", null,
            GeometryCollection.class);//

    private final int type;
    private final String description;
    private String geometryTypeCast;
    private String spaceDimensionsCast;
    private String multiSingleCast;
    private Class< ? > geometryClass;

    /**
     * Create the type.
     *
     * @param type                the geometry type.
     * @param description         the human readable description.
     * @param geometryTypeCast    the geometry cast sql piece.
     * @param spaceDimensionsCast the space dimension cast sql piece.
     * @param multiSingleCast     the cast sql piece for single or multi geom.
     */
    ESpatialiteGeometryType( int type, String description, String geometryTypeCast, String spaceDimensionsCast,
            String multiSingleCast, Class< ? > geometryClass ) {
        this.type = type;
        this.description = description;
        this.geometryTypeCast = geometryTypeCast;
        this.spaceDimensionsCast = spaceDimensionsCast;
        this.multiSingleCast = multiSingleCast;
        this.geometryClass = geometryClass;
    }

    /**
     * @return the geometry type.
     */
    public int getType() {
        return type;
    }

    /**
     * @return the human readable description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the geometry cast sql piece.
     */
    public String getGeometryTypeCast() {
        return geometryTypeCast;
    }

    /**
     * @return the space dimension cast sql piece.
     */
    public String getSpaceDimensionsCast() {
        return spaceDimensionsCast;
    }

    /**
     * @return the cast sql piece for single or multi geom.
     */
    public String getMultiSingleCast() {
        return multiSingleCast;
    }

    public Class< ? > getGeometryClass() {
        return geometryClass;
    }

    /**
     * Get the type from the int value in spatialite 4.
     *
     * @param value the type.
     * @return the {@link ESpatialiteGeometryType}.
     */
    public static ESpatialiteGeometryType forValue( int value ) {
        switch( value ) {
        case 0:
            return GEOMETRY_XY;
        case 1:
            return POINT_XY;
        case 2:
            return LINESTRING_XY;
        case 3:
            return POLYGON_XY;
        case 4:
            return MULTIPOINT_XY;
        case 5:
            return MULTILINESTRING_XY;
        case 6:
            return MULTIPOLYGON_XY;
        case 7:
            return GEOMETRYCOLLECTION_XY;
        /*
         * XYZ
         */
        case 1000:
            return GEOMETRY_XYZ;
        case 1001:
            return POINT_XYZ;
        case 1002:
            return LINESTRING_XYZ;
        case 1003:
            return POLYGON_XYZ;
        case 1004:
            return MULTIPOINT_XYZ;
        case 1005:
            return MULTILINESTRING_XYZ;
        case 1006:
            return MULTIPOLYGON_XYZ;
        case 1007:
            return GEOMETRYCOLLECTION_XYZ;
        /*
         * XYM
         */
        case 2000:
            return GEOMETRY_XYM;
        case 2001:
            return POINT_XYM;
        case 2002:
            return LINESTRING_XYM;
        case 2003:
            return POLYGON_XYM;
        case 2004:
            return MULTIPOINT_XYM;
        case 2005:
            return MULTILINESTRING_XYM;
        case 2006:
            return MULTIPOLYGON_XYM;
        case 2007:
            return GEOMETRYCOLLECTION_XYM;
        /*
         * XYZM
         */
        case 3000:
            return GEOMETRY_XYZM;
        case 3001:
            return POINT_XYZM;
        case 3002:
            return LINESTRING_XYZM;
        case 3003:
            return POLYGON_XYZM;
        case 3004:
            return MULTIPOINT_XYZM;
        case 3005:
            return MULTILINESTRING_XYZM;
        case 3006:
            return MULTIPOLYGON_XYZM;
        case 3007:
            return GEOMETRYCOLLECTION_XYZM;
        default:
            break;
        }
        return null;
    }

    /**
     * Checks if the given geometry is compatible with this type.
     * <p/>
     * <p>Compatible means that the type is the same and a cast from multi to
     * single is not required.<p/>
     *
     * @param geometry the geometry to check.
     * @return <code>true</code>, if the geometry is compatible.
     */
    public boolean isGeometryCompatible( Geometry geometry ) {
        String geometryType = geometry.getGeometryType().toLowerCase();

        String description = getDescription().toLowerCase();
        if (!description.startsWith(geometryType)) {
            /*
             * Geometry is compatible if the type is multi
             * and the geometry is single.
             */
            String multiSingleCast = getMultiSingleCast().toLowerCase();
            if (multiSingleCast.contains("tomulti")) {
                // layer is multi geometry
                if (!description.startsWith("multi" + geometryType)) {
                    return false;
                }
            }
        }
        /*
         * Geometry is not compatible if the type is single
         * and the geometry is multi.
         */
        String multiSingleCast = getMultiSingleCast().toLowerCase();
        if (multiSingleCast.contains("tosingle")) {
            // layer is single geometry
            if (geometryType.contains("multi")) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given geometry type is compatible with this type.
     * <p/>
     * <p>Compatible means that the type is the same and a cast from multi to
     * single is not required.<p/>
     *
     * @param geometryType the geometry type to check.
     * @return <code>true</code>, if the geometry is compatible.
     */
    public boolean isGeometryTypeCompatible( ESpatialiteGeometryType geometryType ) {
        String otherDescription = geometryType.getDescription();
        String thisDescription = getDescription();
        /*
         * Geometry is not compatible if the type is single
         * and the geometry is multi.
         */
        String multiSingleCast = getMultiSingleCast().toLowerCase();
        if (multiSingleCast.contains("tosingle")) {
            // layer is single geometry
            if (otherDescription.contains("multi")) {
                return false;
            }
        }

        String otherBaseDescription = otherDescription.split("\\_")[0].replaceFirst("multi", "");
        String baseDescription = thisDescription.split("\\_")[0].replaceFirst("multi", "");

        return baseDescription.equals(otherBaseDescription);
    }

    /**
     * @return <code>true</code>, if it is of the line type (also multi)
     */
    public boolean isLine() {
        switch( this ) {
        case LINESTRING_XY:
        case LINESTRING_XYM:
        case LINESTRING_XYZ:
        case LINESTRING_XYZM:
        case MULTILINESTRING_XY:
        case MULTILINESTRING_XYM:
        case MULTILINESTRING_XYZ:
        case MULTILINESTRING_XYZM:
            return true;
        default:
            return false;
        }
    }

    /**
     * @return <code>true</code>, if it is of the polygon type (also multi)
     */
    public boolean isPolygon() {
        switch( this ) {
        case POLYGON_XY:
        case POLYGON_XYM:
        case POLYGON_XYZ:
        case POLYGON_XYZM:
        case MULTIPOLYGON_XY:
        case MULTIPOLYGON_XYM:
        case MULTIPOLYGON_XYZ:
        case MULTIPOLYGON_XYZM:
            return true;
        default:
            return false;
        }
    }
    /**
     * @return <code>true</code>, if it is of the point type (also multi)
     */
    public boolean isPoint() {
        switch( this ) {
        case POINT_XY:
        case POINT_XYM:
        case POINT_XYZ:
        case POINT_XYZM:
        case MULTIPOINT_XY:
        case MULTIPOINT_XYM:
        case MULTIPOINT_XYZ:
        case MULTIPOINT_XYZM:
            return true;
        default:
            return false;
        }
    }

    /**
     * @return <code>true</code>, if it is of type multi.
     */
    public boolean isMulti() {
        switch( this ) {
        case MULTIPOINT_XY:
        case MULTIPOINT_XYM:
        case MULTIPOINT_XYZ:
        case MULTIPOINT_XYZM:
        case MULTILINESTRING_XY:
        case MULTILINESTRING_XYM:
        case MULTILINESTRING_XYZ:
        case MULTILINESTRING_XYZM:
        case MULTIPOLYGON_XY:
        case MULTIPOLYGON_XYM:
        case MULTIPOLYGON_XYZ:
        case MULTIPOLYGON_XYZM:
            return true;
        default:
            return false;
        }
    }

    public boolean hasZ() {
        switch( this ) {
        case POINT_XYZ:
        case POINT_XYZM:
        case LINESTRING_XYZ:
        case LINESTRING_XYZM:
        case POLYGON_XYZ:
        case POLYGON_XYZM:
        case MULTIPOINT_XYZ:
        case MULTIPOINT_XYZM:
        case MULTILINESTRING_XYZ:
        case MULTILINESTRING_XYZM:
        case MULTIPOLYGON_XYZ:
        case MULTIPOLYGON_XYZM:
        case GEOMETRY_XYZ:
        case GEOMETRY_XYZM:
        case GEOMETRYCOLLECTION_XYZ:
        case GEOMETRYCOLLECTION_XYZM:
            return true;
        default:
            return false;
        }
    }

    public boolean hasM() {
        switch( this ) {
        case POINT_XYM:
        case POINT_XYZM:
        case LINESTRING_XYM:
        case LINESTRING_XYZM:
        case POLYGON_XYM:
        case POLYGON_XYZM:
        case MULTIPOINT_XYM:
        case MULTIPOINT_XYZM:
        case MULTILINESTRING_XYM:
        case MULTILINESTRING_XYZM:
        case MULTIPOLYGON_XYM:
        case MULTIPOLYGON_XYZM:
        case GEOMETRY_XYM:
        case GEOMETRY_XYZM:
        case GEOMETRYCOLLECTION_XYM:
        case GEOMETRYCOLLECTION_XYZM:
            return true;
        default:
            return false;
        }
    }

    /**
     * Get the {@link ESpatialiteGeometryType} int value from the geometry type name as of spatialite 3.
     * <p/>
     * @param name the geometry type name.
     * @return the type or -1 if type is unknown.
     */
    public static int forValue( String name ) {
        ESpatialiteGeometryType type = forName(name);
        if (type != null) {
            return type.getType();
        }
        return -1;
    }

    /**
     * Returns the type for the given name.
     * 
     * @param name the geometry type name.
     * @return the type or <code>null</code> if unknown.
     */
    public static ESpatialiteGeometryType forName( String name ) {
        for( ESpatialiteGeometryType type : values() ) {
            if (type.getDescription().startsWith(name.toLowerCase()))
                return type;
        }
        return null;
    }

    /**
     * Returns <code>true</code> if the given name is a geometry name.
     * 
     * @param name the geometry type name.
     * @return <code>true</code> if the name represents a geometry.
     */
    public static boolean isGeometryName( String name ) {
        for( ESpatialiteGeometryType type : values() ) {
            if (type.getDescription().startsWith(name.toLowerCase()))
                return true;
        }
        return false;
    }
}