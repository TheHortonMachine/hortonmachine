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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import org.jgrasstools.dbs.compat.IJGTConnection;
import org.jgrasstools.dbs.compat.IJGTPreparedStatement;
import org.jgrasstools.dbs.compat.IJGTStatement;

import jsqlite.Database;

/**
 * Connection wrapper for android.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GPConnection implements IJGTConnection {

    private Database database;

    public GPConnection(Database database) {
        this.database = database;
    }

    public Connection getOriginalConnection() {
        return null;
    }

    public IJGTStatement createStatement() throws SQLException {
        IJGTStatement statement = new GPStatement(database);
        return statement;
    }

    @Override
    public IJGTPreparedStatement prepareStatement(String sql) throws Exception {
        IJGTPreparedStatement preparedStatement = new GPPreparedStatement(database, sql);
        return preparedStatement;
    }

    @Override
    public IJGTPreparedStatement prepareStatement(String sql, int returnGeneratedKeys) throws SQLException {
        throw new RuntimeException("Function not supported: prepareStatement()");
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        throw new RuntimeException("Function not supported: getAutoCommit()");
    }

    @Override
    public void setAutoCommit(boolean b) throws SQLException {
        throw new RuntimeException("Function not supported: setAutoCommit()");
    }

    @Override
    public void commit() throws SQLException {
        throw new RuntimeException("Function not supported: commit()");
    }

    @Override
    public Savepoint setSavepoint() throws Exception {
        throw new RuntimeException("Function not supported: setSavepoint()");
    }

    @Override
    public void rollback( Savepoint savepoint ) throws Exception {
        throw new RuntimeException("Function not supported: rollback()");
    }

}
