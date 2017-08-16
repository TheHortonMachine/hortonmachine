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
package org.jgrasstools.dbs.compat;

import org.jgrasstools.dbs.compat.objects.ColumnLevel;
import org.jgrasstools.dbs.compat.objects.TableLevel;

/**
 * Simple queries templates interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class ASqlTemplates {

    public String selectOnColumn( String columnName, String tableName ) {
        String query = "SELECT " + columnName + " FROM " + tableName;
        return query;
    }

    public String updateOnColumn( String tableName, String columnName ) {
        String query = "UPDATE " + tableName + " SET " + columnName + " = XXX";
        return query;
    }

    public abstract boolean hasAddGeometryColumn();

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
        String query = "SELECT t1.*, t2.* FROM " + tableName + " t1, " + refTable + " t2" + "\nWHERE t1." + columnName + "=t2."
                + refColumn;
        return query;
    }

    public abstract String dropTable( String tableName, String geometryColumnName );

    public abstract String reprojectTable( TableLevel table, ASpatialDb db, ColumnLevel geometryColumn, String tableName,
            String newTableName, String newSrid ) throws Exception;

}
