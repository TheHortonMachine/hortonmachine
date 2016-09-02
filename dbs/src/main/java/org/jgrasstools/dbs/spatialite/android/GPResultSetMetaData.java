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
package org.jgrasstools.dbs.spatialite.android;


import org.jgrasstools.dbs.compat.IJGTResultSetMetaData;

import jsqlite.Stmt;

/**
 * Resultset metadata wrapper for android.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GPResultSetMetaData implements IJGTResultSetMetaData {

    private Stmt stmt;

    public GPResultSetMetaData(Stmt stmt) {
        this.stmt = stmt;
    }

    @Override
    public int getColumnCount() throws Exception {
        return stmt.column_count();
    }

    @Override
    public String getColumnName(int index) throws Exception {
        return stmt.column_name(index - 1);
    }

    @Override
    public String getColumnTypeName(int index) throws Exception {
        int type = stmt.column_type(index - 1);
        // TODO
        return "" + type;
    }

    @Override
    public int getColumnType(int index) throws Exception {
        return stmt.column_type(index - 1);
    }

}
