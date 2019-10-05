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
package org.hortonmachine.dbs.h2gis;

import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;

/**
 * Non spatial data types and small syntax for the h2 db.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class H2SyntaxHelper extends ADatabaseSyntaxHelper {

    public String TEXT() {
        return "VARCHAR";
    }

    public String INTEGER() {
        return "INT";
    }

    public String LONG() {
        return "BIGINT";
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
        return "AUTO_INCREMENT";
    }

    @Override
    public String MAKEPOINT2D() {
        return "ST_MakePoint";
    }

    public String LONG_PRIMARYKEY_AUTOINCREMENT() {
        return LONG() + " " + PRIMARYKEY() + " " + AUTOINCREMENT();
    }

    public String checkSqlCompatibilityIssues( String sql ) {
        String lowerCase = sql.toLowerCase().trim();
        if (lowerCase.startsWith("create table")) {
            sql = sql.replaceAll("AUTOINCREMENT", "AUTO_INCREMENT");
            sql = sql.replaceAll("TEXT", "varchar");
        }
        return sql;
    }

}