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
package org.jgrasstools.dbs.h2gis;

import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.ASqlTemplates;
import org.jgrasstools.dbs.compat.objects.ColumnLevel;
import org.jgrasstools.dbs.compat.objects.TableLevel;
import org.jgrasstools.dbs.utils.DbsUtilities;

/**
 * Simple queries templates.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class H2GisSqlTemplates extends ASqlTemplates {
    @Override
    public boolean hasAddGeometryColumn() {
        return false;
    }

    @Override
    public String addGeometryColumn( String tableName, String columnName, String srid, String geomType, String dimension ) {
        return null;
    }

    @Override
    public String recoverGeometryColumn( String tableName, String columnName, String srid, String geomType, String dimension ) {
        return null;
    }

    @Override
    public String discardGeometryColumn( String tableName, String geometryColumnName ) {
        return null;
    }

    @Override
    public String createSpatialIndex( String tableName, String columnName ) {
        String query = "CREATE SPATIAL INDEX ON " + tableName + "(" + columnName + ")";
        return query;
    }

    @Override
    public String checkSpatialIndex( String tableName, String columnName ) {
        return null;
    }

    @Override
    public String recoverSpatialIndex( String tableName, String columnName ) {
        return null;
    }

    @Override
    public String disableSpatialIndex( String tableName, String columnName ) {
        return null;
    }

    @Override
    public String showSpatialMetadata( String tableName, String columnName ) {
        // TODO
        String query = "SELECT * FROM geom_cols_ref_sys WHERE Lower(f_table_name) = Lower('" + tableName
                + "') AND Lower(f_geometry_column) = Lower('" + columnName + "')";
        return query;
    }

    @Override
    public String dropTable( String tableName, String geometryColumnName ) {
        // TODO
        String query = "";
        if (geometryColumnName != null) {
            query = "SELECT DiscardGeometryColumn('" + tableName + "', '" + geometryColumnName + "');";
            query += "\nSELECT DisableSpatialIndex('" + tableName + "', '" + geometryColumnName + "');";
            query += "\nDROP TABLE idx_" + tableName + "_" + geometryColumnName + ";";
        }
        query += "drop table " + tableName + ";";
        return query;
    }

    @Override
    public String reprojectTable( TableLevel table, ASpatialDb db, ColumnLevel geometryColumn, String tableName,
            String newTableName, String newSrid ) throws Exception {
        // TODO
        String letter = tableName.substring(0, 1);
        String columnName = letter + "." + geometryColumn.columnName;
        String query = DbsUtilities.getSelectQuery(db, table, false);
        query = query.replaceFirst(columnName, "TRANSFORM(" + columnName + ", " + newSrid + ")");
        query = "create table " + newTableName + " as " + query + ";\n";
        query += "SELECT RecoverGeometryColumn('" + newTableName + "', '" + geometryColumn.columnName + "'," + newSrid + ",'"
                + geometryColumn.columnType + "'," + geometryColumn.geomColumn.coordinatesDimension + ");";
        return query;
    }
}
