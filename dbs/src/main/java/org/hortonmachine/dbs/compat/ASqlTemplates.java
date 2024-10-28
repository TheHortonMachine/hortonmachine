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
package org.hortonmachine.dbs.compat;

import java.io.File;

import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * Simple queries templates interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class ASqlTemplates {

    public String selectOnColumn( String columnName, SqlName tableName ) {
        String query = "SELECT * FROM " + tableName.fixedDoubleName + " WHERE " + columnName + "=?";
        return query;
    }

    public String selectGroupCountOnColumn(String columnName, SqlName m){
        String query = "SELECT " + columnName + ", COUNT(*) FROM " + m.fixedDoubleName + " GROUP BY " + columnName;
        return query;
    }


    public String updateOnColumn( SqlName tableName, String columnName ) {
        String query = "UPDATE " + tableName + " SET " + columnName + " = XXX";
        return query;
    }

    public abstract boolean hasAddGeometryColumn();

    public abstract boolean hasRecoverGeometryColumn();

    public abstract boolean hasRecoverSpatialIndex();

    public abstract boolean hasCreateSpatialIndex();

    public abstract boolean hasAttachShapefile();

    public abstract boolean hasReprojectTable();

    public abstract String addSrid( SqlName tableName, int srid, String geometryColumnName );

    public abstract String addGeometryColumn( SqlName tableName, String columnName, String srid, String geomType,
            String dimension );

    public abstract String recoverGeometryColumn( SqlName tableName, String columnName, String srid, String geomType,
            String dimension );

    public abstract String discardGeometryColumn( SqlName tableName, String geometryColumnName );

    public abstract String createSpatialIndex( SqlName tableName, String columnName );

    public abstract String checkSpatialIndex( SqlName tableName, String columnName );

    public abstract String recoverSpatialIndex( SqlName tableName, String columnName );

    public abstract String disableSpatialIndex( SqlName tableName, String columnName );

    public abstract String showSpatialMetadata( SqlName tableName, String columnName );

    public String combinedSelect( SqlName refTable, String refColumn, SqlName tableName, String columnName ) {
        String query = "SELECT t1.*, t2.* FROM " + tableName.fixedDoubleName + " t1, " + refTable.fixedDoubleName
                + " t2" + "\nWHERE t1." + columnName + "=t2." + refColumn;
        return query;
    }

    public abstract String dropTable( SqlName tableName, String geometryColumnName );

    public abstract String reprojectTable( TableLevel table, ASpatialDb db, ColumnLevel geometryColumn, SqlName tableName,
            String newTableName, String newSrid ) throws Exception;

    public abstract String attachShapefile( File file );

    /**
     * Get the syntax for geojson.
     * 
     * @param geomPart the geom part with srid and transforms if necessary. ex. ST_Collect(ST_Transform(the_geom,4326))
     * @param precision the precision, if db supports it.
     * @return the right string to use.
     */
    public abstract String getGeoJsonSyntax( String geomPart, int precision );

    /**
     * Get the format time syntax for the db.
     * 
     * @param timestampField the field of timestamp (unix epoch).
     * @param formatPattern the pattern. In java format: YYYY-MM-dd HH:mm:ss
     * @return the string for the current db type.
     */
    public abstract String getFormatTimeSyntax( String timestampField, String formatPattern );


}
