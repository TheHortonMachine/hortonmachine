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
package org.hortonmachine.dbs.spatialite.android;

import java.sql.Date;
import java.util.HashMap;

import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMResultSetMetaData;

import jsqlite.Stmt;

/**
 * Resultset wrapper for android.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GPResultSet implements IHMResultSet {

    private Stmt stmt;

    private HashMap<String, Integer> name2Index = new HashMap<>();

    public GPResultSet( Stmt stmt ) throws Exception {
        this.stmt = stmt;
        int columnCount = stmt.column_count();
        for( int i = 0; i < columnCount; i++ ) {
            String columnName = stmt.column_name(i);
            name2Index.put(columnName, i);
        }
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
    public String getString( String name ) throws Exception {
        return stmt.column_string(name2Index.get(name));
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
    public IHMResultSetMetaData getMetaData() throws Exception {
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

    public boolean getBoolean( String name ) throws Exception {
        return stmt.column_int(name2Index.get(name)) == 0 ? false : true;
    }

    @Override
    public boolean wasNull() throws Exception {
        throw new RuntimeException("Function not supported: wasNull()");
    }

    @Override
    public int getInt( String name ) throws Exception {
        return stmt.column_int(name2Index.get(name));
    }

    @Override
    public double getDouble( String name ) throws Exception {
        return stmt.column_double(name2Index.get(name));
    }

    @Override
    public long getLong( String name ) throws Exception {
        return stmt.column_long(name2Index.get(name));
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws Exception {
        throw new RuntimeException("Function not supported: unwrap( Class<T> iface )");
    }

    @Override
    public Date getDate( int index ) throws Exception {
        return new Date(getLong(index));
    }

    @Override
    public Date getDate( String name ) throws Exception {
        return new Date(getLong(name));
    }

}
