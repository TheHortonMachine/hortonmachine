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
package org.hortonmachine.dbs.nosql.mongodb;

import java.io.File;

import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ASqlTemplates;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.utils.SqlName;

/**
 * Simple queries templates.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MongoSqlTemplates extends ASqlTemplates {
    public String selectOnColumn( String columnName, SqlName tableName ) {
        String query = columnName + "=?";
        return query;
    }
    
    @Override
    public boolean hasAddGeometryColumn() {
        return false;
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
    public String addGeometryColumn( SqlName tableName, String columnName, String srid, String geomType, String dimension ) {
        return null;
    }

    @Override
    public String recoverGeometryColumn( SqlName tableName, String columnName, String srid, String geomType, String dimension ) {
        return null;
    }

    @Override
    public String discardGeometryColumn( SqlName tableName, String geometryColumnName ) {
        return null;
    }

    @Override
    public String createSpatialIndex( SqlName tableName, String columnName ) {
        return null;
    }

    @Override
    public String checkSpatialIndex( SqlName tableName, String columnName ) {
        return null;
    }

    @Override
    public String recoverSpatialIndex( SqlName tableName, String columnName ) {
        return null;
    }

    @Override
    public String disableSpatialIndex( SqlName tableName, String columnName ) {
        return null;
    }

    @Override
    public String showSpatialMetadata( SqlName tableName, String columnName ) {
        return null;
    }

    @Override
    public String dropTable( SqlName tableName, String geometryColumnName ) {
        return null;
    }

    @Override
    public String reprojectTable( TableLevel table, ASpatialDb db, ColumnLevel geometryColumn, SqlName tableName,
            String newTableName, String newSrid ) throws Exception {
        return null;
    }

    @Override
    public String attachShapefile( File file ) {
        return null;
    }

    @Override
    public String getGeoJsonSyntax( String geomPart, int precision ) {
        return null;
    }

    @Override
    public String getFormatTimeSyntax( String timestampField, String formatPattern ) {
        return null;
    }

    @Override
    public String addSrid( SqlName tableName, int srid, String geometryColumnName ) {
        return null;
    }

    @Override
    public boolean hasReprojectTable() {
        return false;
    }

    @Override
    public boolean hasCreateSpatialIndex() {
        return false;
    }
}
