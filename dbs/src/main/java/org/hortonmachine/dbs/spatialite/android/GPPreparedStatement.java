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

import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * PreparedStatement wrapper for android.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GPPreparedStatement implements IHMPreparedStatement {

    private final Stmt preparedStmt;

    private int batchCount = 0;

    public GPPreparedStatement( Database database, String sql ) throws Exception {
        preparedStmt = database.prepare(sql);
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void setString( int index, String text ) throws Exception {
        preparedStmt.bind(index, text);
    }

    @Override
    public int executeUpdate() throws Exception {
        try {
            preparedStmt.step();
        } finally {
            preparedStmt.close();
        }
        return -1;
    }

    @Override
    public void setDouble( int index, double value ) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void setFloat( int index, float value ) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void setInt( int index, int value ) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void addBatch() throws Exception {
        batchCount++;
    }

    @Override
    public int[] executeBatch() throws Exception {
        try {
            preparedStmt.step();
            return new int[batchCount];
        } finally {
            preparedStmt.close();
        }
    }

    @Override
    public void setLong( int index, long value ) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void setBytes( int index, byte[] value ) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void setBlob( int index, byte[] value ) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void setShort( int index, short value ) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void setBoolean( int index, boolean value ) throws java.lang.Exception {
        preparedStmt.bind(index, value ? 1 : 0);
    }

    @Override
    public void setObject( int index, Object value ) throws Exception {
        throw new RuntimeException("Function not supported: setObject()");
    }

    @Override
    public IHMResultSet getGeneratedKeys() throws Exception {
        throw new RuntimeException("Function not supported: getGeneratedKeys()");
    }

    @Override
    public IHMResultSet executeQuery() throws java.lang.Exception {
        return new GPResultSet(preparedStmt);
    }

}
