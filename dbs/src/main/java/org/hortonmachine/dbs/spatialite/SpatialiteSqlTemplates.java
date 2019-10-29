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
package org.hortonmachine.dbs.spatialite;
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
public class SpatialiteSqlTemplates extends ASqlTemplates {

    @Override
    public boolean hasAddGeometryColumn() {
        return true;
    }
    
    @Override
    public boolean hasRecoverGeometryColumn() {
        return true;
    }

    @Override
    public boolean hasAttachShapefile() {
        return true;
    }
    
    @Override
    public boolean hasRecoverSpatialIndex() {
        return true;
    }

    @Override
    public String addGeometryColumn( String tableName, String columnName, String srid, String geomType, String dimension ) {
        String query = "SELECT AddGeometryColumn('" + tableName + "', '" + columnName + "',  " + srid + ", '" + geomType + "', '"
                + dimension + "')";
        return query;
    }

    @Override
    public String recoverGeometryColumn( String tableName, String columnName, String srid, String geomType, String dimension ) {
        String query = "SELECT RecoverGeometryColumn('" + tableName + "', '" + columnName + "',  " + srid + ", '" + geomType
                + "', '" + dimension + "')";
        return query;
    }

    @Override
    public String discardGeometryColumn( String tableName, String geometryColumnName ) {
        String query = "SELECT DiscardGeometryColumn('" + tableName + "', '" + geometryColumnName + "');";
        return query;
    }

    @Override
    public String createSpatialIndex( String tableName, String columnName ) {
        String query = "SELECT CreateSpatialIndex('" + tableName + "','" + columnName + "');";
        return query;
    }

    @Override
    public String checkSpatialIndex( String tableName, String columnName ) {
        String query = "SELECT CheckSpatialIndex('" + tableName + "','" + columnName + "');";
        return query;
    }

    @Override
    public String recoverSpatialIndex( String tableName, String columnName ) {
        String query = "SELECT RecoverSpatialIndex('" + tableName + "','" + columnName + "');";
        return query;
    }

    @Override
    public String disableSpatialIndex( String tableName, String columnName ) {
        String query = "SELECT DisableSpatialIndex('" + tableName + "','" + columnName + "');";
        return query;
    }

    @Override
    public String showSpatialMetadata( String tableName, String columnName ) {
        String query = "SELECT * FROM geom_cols_ref_sys WHERE Lower(f_table_name) = Lower('" + tableName
                + "') AND Lower(f_geometry_column) = Lower('" + columnName + "')";
        return query;
    }

    @Override
    public String dropTable( String tableName, String geometryColumnName ) {
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
        String letter = tableName.substring(0, 1);
        String columnName = letter + "." + geometryColumn.columnName;
        String query = DbsUtilities.getSelectQuery(db, table, false);
        query = query.replaceFirst(columnName, "ST_Transform(" + columnName + ", " + newSrid + ")");
        query = "create table " + newTableName + " as " + query + ";\n";
        query += "SELECT RecoverGeometryColumn('" + newTableName + "', '" + geometryColumn.columnName + "'," + newSrid + ",'"
                + geometryColumn.columnType + "'," + geometryColumn.geomColumn.coordinatesDimension + ");";
        return query;
    }

    @Override
    public String attachShapefile( File file ) {
        String absolutePath = file.getAbsolutePath();
        String name = file.getName();
        name = name.substring(0, name.length() - 4);
        String tableName = name.replace('.', '_');
        absolutePath = absolutePath.substring(0, absolutePath.length() - 4);
        String query = "CREATE VIRTUAL TABLE '" + tableName + "' USING VirtualShape('" + absolutePath + "', 'UTF-8', 4326) ";
        return query;
    }

    @Override
    public String getGeoJsonSyntax( String geomPart, int precision ) {
        return "AsGeoJson(" + geomPart + "," + precision + ",0)";
    }

    @Override
    public String getFormatTimeSyntax( String timestampField, String formatPattern ) {
        String pattern = "%Y-%m-%d %H:%M:%S";
        if (formatPattern != null) {
            pattern = formatPattern;
            // supported for now YYYY-dd-MM HH:mm:ss
            pattern = pattern.replaceAll("YYYY", "%Y");
            pattern = pattern.replaceAll("dd", "%d");
            pattern = pattern.replaceAll("MM", "%m");
            pattern = pattern.replaceAll("HH", "%H");
            pattern = pattern.replaceAll("mm", "%M");
            pattern = pattern.replaceAll("ss", "%S");
        }

        String sql = "strftime('" + pattern + "'," + timestampField + " / 1000, 'unixepoch')";
        return sql;
    }

    @Override
    public String addSrid( String tableName, int srid, String geometryColumnName ) {
        return "-- added during geometry column creation";
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
