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
import org.hortonmachine.dbs.utils.DbsUtilities;

/**
 * Simple queries templates interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class ASqlTemplates {

    public String selectOnColumn( String columnName, String tableName ) {
        String query = "SELECT * FROM " + DbsUtilities.fixTableName(tableName) + " WHERE " + columnName + "=?";
        return query;
    }

    public String updateOnColumn( String tableName, String columnName ) {
        String query = "UPDATE " + tableName + " SET " + columnName + " = XXX";
        return query;
    }

    public abstract boolean hasAddGeometryColumn();

    public abstract boolean hasRecoverGeometryColumn();

    public abstract boolean hasRecoverSpatialIndex();

    public abstract boolean hasCreateSpatialIndex();

    public abstract boolean hasAttachShapefile();

    public abstract boolean hasReprojectTable();

    public abstract String addSrid( String tableName, int srid, String geometryColumnName );

    public abstract String addGeometryColumn( String tableName, String columnName, String srid, String geomType,
            String dimension );

    public abstract String recoverGeometryColumn( String tableName, String columnName, String srid, String geomType,
            String dimension );

    public abstract String discardGeometryColumn( String tableName, String geometryColumnName );

    public abstract String createSpatialIndex( String tableName, String columnName );

    public abstract String checkSpatialIndex( String tableName, String columnName );

    public abstract String recoverSpatialIndex( String tableName, String columnName );

    public abstract String disableSpatialIndex( String tableName, String columnName );

    public abstract String showSpatialMetadata( String tableName, String columnName );

    public String combinedSelect( String refTable, String refColumn, String tableName, String columnName ) {
        String query = "SELECT t1.*, t2.* FROM " + DbsUtilities.fixTableName(tableName) + " t1, "
                + DbsUtilities.fixTableName(refTable) + " t2" + "\nWHERE t1." + columnName + "=t2." + refColumn;
        return query;
    }

    public abstract String dropTable( String tableName, String geometryColumnName );

    public abstract String reprojectTable( TableLevel table, ASpatialDb db, ColumnLevel geometryColumn, String tableName,
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
