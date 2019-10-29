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

import java.io.File;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ASqlTemplates;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.utils.DbsUtilities;

/**
 * Simple queries templates.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PostgisSqlTemplates extends ASqlTemplates {
    @Override
    public boolean hasAddGeometryColumn() {
        return true;
    }

    @Override
    public boolean hasRecoverGeometryColumn() {
        return false;
    }

    @Override
    public boolean hasAttachShapefile() {
        return false;
    }

    @Override
    public boolean hasRecoverSpatialIndex() {
        return false;
    }

    @Override
    public String addGeometryColumn( String tableName, String columnName, String srid, String geomType, String dimension ) {
        // SELECT AddGeometryColumn ('my_schema','my_spatial_table','geom',4326,'POINT',2);
        if (dimension.toLowerCase().equals("XY")) {
            dimension = "2";
        }

        String sql = "SELECT AddGeometryColumn('" + tableName + "','" + columnName + "', " + srid + ", '" + geomType + "', "
                + dimension + ")";
        return sql;
    }

    @Override
    public String recoverGeometryColumn( String tableName, String columnName, String srid, String geomType, String dimension ) {
        return null;
    }

    @Override
    public String discardGeometryColumn( String tableName, String geometryColumnName ) {
        return "select DropGeometryColumn('public','" + tableName + "', '" + geometryColumnName + "')";
    }

    @Override
    public String createSpatialIndex( String tableName, String columnName ) {
        String sql = "CREATE INDEX " + tableName + "__" + columnName + "_spx ON " + tableName + " USING GIST (" + columnName
                + ");";
        return sql;
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
        String query = "SELECT * FROM GEOMETRY_COLUMNS WHERE Lower(f_table_name) = Lower('" + tableName
                + "') AND Lower(f_geometry_column) = Lower('" + columnName + "')";
        return query;
    }

    @Override
    public String dropTable( String tableName, String geometryColumnName ) {
//        String query = "select DropGeometryTable('" + tableName + "')";
        String query = discardGeometryColumn(tableName, geometryColumnName) + ";\n";
        query += "drop table if exists " + tableName + " cascade;";
        return query;
    }

    @Override
    public String reprojectTable( TableLevel table, ASpatialDb db, ColumnLevel geometryColumn, String tableName,
            String newTableName, String newSrid ) throws Exception {
        String letter = tableName.substring(0, 1);
        String columnName = letter + "." + geometryColumn.columnName;
        String query = DbsUtilities.getSelectQuery(db, table, false);
        query = query.replaceFirst(columnName,
                "ST_Transform(" + columnName + ", " + newSrid + ")::geometry(" + geometryColumn.columnType + "," + newSrid + ")");
        query = "create table " + newTableName + " as " + query + ";\n";

        return query;
    }

    @Override
    public String attachShapefile( File file ) {
        return null;
    }

    @Override
    public String getGeoJsonSyntax( String geomPart, int precision ) {
        return "ST_AsGeoJson(" + geomPart + ")";
    }

    @Override
    public String getFormatTimeSyntax( String timestampField, String formatPattern ) {
        String pattern = "YYYY-mm-DD HH24:MI:SS";
        if (formatPattern != null) {
            pattern = formatPattern;
            // supported for now YYYY-dd-MM HH:mm:ss
            pattern = pattern.replaceAll("YYYY", "YYYY");
            pattern = pattern.replaceAll("dd", "DD");
            pattern = pattern.replaceAll("mm", "MI");// minutes before month
            pattern = pattern.replaceAll("MM", "mm");
            pattern = pattern.replaceAll("HH", "HH24");
            pattern = pattern.replaceAll("ss", "SS");
        }

        String sql = "to_char(TIMESTAMP 'epoch' + " + timestampField + "/1000 * interval '1 second', '" + pattern + "')";
        // with timezone to_char(to_timestamp(system_ts/1000), 'YYYY-mm-DD HH:MI:SS')

        return sql;
    }

    @Override
    public String addSrid( String tableName, int srid, String geometryColumnName ) {
        return "-- SRID is added during addGeometryColumn";
    }

    @Override
    public boolean hasReprojectTable() {
        return true;
    }

    @Override
    public boolean hasCreateSpatialIndex() {
        return true;
    }
}
