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

import java.sql.ResultSet;

import org.jgrasstools.dbs.compat.IJGTPreparedStatement;

import jsqlite.Database;
import jsqlite.Exception;
import jsqlite.Stmt;

/**
 * PreparedStatement wrapper for android.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GPPreparedStatement implements IJGTPreparedStatement {

    private final Stmt preparedStmt;

    private int batchCount = 0;

    public GPPreparedStatement(Database database, String sql) throws Exception {
        preparedStmt = database.prepare(sql);
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public void setString(int index, String text) throws Exception {
        preparedStmt.bind(index, text);
    }

    @Override
    public void executeUpdate() throws Exception {
        try {
            preparedStmt.step();
        } finally {
            preparedStmt.close();
        }
    }

    @Override
    public void setDouble(int index, double value) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void setFloat(int index, float value) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void setInt(int index, int value) throws Exception {
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
    public void setLong(int index, long value) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void setBytes(int index, byte[] value) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public void setShort(int index, short value) throws Exception {
        preparedStmt.bind(index, value);
    }

    @Override
    public ResultSet getGeneratedKeys() throws Exception {
        throw new RuntimeException("Function not supported: getGeneratedKeys()");
    }

}
