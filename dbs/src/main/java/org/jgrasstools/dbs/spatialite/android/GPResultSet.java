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

import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTResultSetMetaData;

import jsqlite.Stmt;

/**
 * Resultset wrapper for android.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GPResultSet implements IJGTResultSet {

    private Stmt stmt;

    public GPResultSet( Stmt stmt ) {
        this.stmt = stmt;
    }

    @Override
    public void close() throws Exception {
        stmt.close();
    }

    @Override
    public boolean next() throws Exception {
        return stmt.step();
    }

    @Override
    public String getString( int index ) throws Exception {
        return stmt.column_string(index - 1);
    }

    @Override
    public int getInt( int index ) throws Exception {
        return stmt.column_int(index - 1);
    }

    @Override
    public double getDouble( int index ) throws Exception {
        return stmt.column_double(index - 1);
    }

    @Override
    public float getFloat( int index ) throws Exception {
        return (float) stmt.column_double(index - 1);
    }

    @Override
    public Object getObject( int index ) throws Exception {
        return stmt.column_bytes(index - 1);
    }

    @Override
    public long getLong( int index ) throws Exception {
        return stmt.column_long(index - 1);
    }

    @Override
    public byte[] getBytes( int index ) throws Exception {
        return stmt.column_bytes(index - 1);
    }

    @Override
    public IJGTResultSetMetaData getMetaData() throws Exception {
        return new GPResultSetMetaData(stmt);
    }

    @Override
    public short getShort( int index ) throws Exception {
        return (short) stmt.column_int(index - 1);
    }

    @Override
    public boolean getBoolean( int index ) throws Exception {
        return stmt.column_int(index - 1) == 0 ? false : true;
    }

    @Override
    public boolean wasNull() throws Exception {
        throw new RuntimeException("Function not supported: wasNull()");
    }

}
