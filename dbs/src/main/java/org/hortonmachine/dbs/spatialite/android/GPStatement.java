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

import java.sql.SQLException;

import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;

import jsqlite.Database;
import jsqlite.Stmt;

/**
 * Statement wrapper for android.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GPStatement implements IHMStatement {

    private Database database;

    public GPStatement( Database database ) {
        this.database = database;
    }

    @Override
    public void close() throws SQLException {
    }

    @Override
    public void execute( String sql ) throws Exception {
        Stmt stmt = database.prepare(sql);
        try {
            stmt.step();
        } finally {
            stmt.close();
        }
    }

    @Override
    public IHMResultSet executeQuery( String sql ) throws Exception {
        Stmt stmt = database.prepare(sql);
        IHMResultSet ijgtResultSet = new GPResultSet(stmt);
        return ijgtResultSet;
    }

    @Override
    public void setQueryTimeout( int seconds ) throws SQLException {
    }

    @Override
    public int executeUpdate( String sql ) throws Exception {
        Stmt stmt = database.prepare(sql);
        try {
            stmt.step();
            return 1;
        } finally {
            stmt.close();
        }
    }

    @Override
    public void addBatch( String sqlLine ) throws Exception {
        throw new RuntimeException("Function not supported: addBatch()");

    }

    @Override
    public int[] executeBatch() throws Exception {
        throw new RuntimeException("Function not supported: executeBatch()");
    }

}
