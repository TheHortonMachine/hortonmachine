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

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Savepoint;

import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMStatement;

import jsqlite.Database;

/**
 * Connection wrapper for android.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GPConnection implements IHMConnection {

    private Database database;

    public GPConnection( Database database ) {
        this.database = database;
    }

    public Connection getOriginalConnection() {
        return null;
    }

    public IHMStatement createStatement() throws SQLException {
        IHMStatement statement = new GPStatement(database);
        return statement;
    }

    @Override
    public IHMPreparedStatement prepareStatement( String sql ) throws Exception {
        IHMPreparedStatement preparedStatement = new GPPreparedStatement(database, sql);
        return preparedStatement;
    }

    @Override
    public IHMPreparedStatement prepareStatement( String sql, int returnGeneratedKeys ) throws SQLException {
        throw new RuntimeException("Function not supported: prepareStatement()");
    }

    @Override
    public void close() throws Exception {
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return true;
    }

    @Override
    public void setAutoCommit( boolean b ) throws SQLException {
    }

    @Override
    public void commit() throws SQLException {
    }

    @Override
    public Savepoint setSavepoint() throws Exception {
        throw new RuntimeException("Function not supported: setSavepoint()");
    }

    @Override
    public void rollback( Savepoint savepoint ) throws Exception {
    }

    @Override
    public void rollback() throws Exception {
    }

    public void enableAutocommit( boolean enable ) throws Exception {
    }

    @Override
    public void release() throws Exception {
    }

}
