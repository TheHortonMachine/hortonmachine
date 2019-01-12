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

import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;

/**
 * Non spatial data types and small syntax for the sqlite db.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SqliteNonSpatialDataType extends ADatabaseSyntaxHelper {

    public String TEXT() {
        return "TEXT";
    }

    public String INTEGER() {
        return "INTEGER";
    }

    public String LONG() {
        return "INTEGER";
    }

    public String REAL() {
        return "DOUBLE";
    }

    public String BLOB() {
        return "BLOB";
    }

    public String CLOB() {
        return "CLOB";
    }

    @Override
    public String PRIMARYKEY() {
        return "PRIMARY KEY";
    }

    @Override
    public String AUTOINCREMENT() {
        return "AUTOINCREMENT";
    }

    @Override
    public String MAKEPOINT2D() {
        return "MakePoint";
    }
    
    public String LONG_PRIMARYKEY_AUTOINCREMENT() {
        return LONG() + " " + PRIMARYKEY() + " " + AUTOINCREMENT();
    }
    
    public String checkSqlCompatibilityIssues( String sql ) {
        return SpatialiteCommonMethods.checkCompatibilityIssues(sql);
    }
}
