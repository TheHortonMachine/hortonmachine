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
package org.hortonmachine.dbs.spatialite.hm;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Savepoint;

import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMStatement;

/**
 * Connection wrapper for standard jdbc java.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class HMConnection implements IHMConnection {

    private Connection connection;
    private boolean closeOnRelease;

    public HMConnection( Connection connection, boolean closeOnRelease ) {
        this.connection = connection;
        this.closeOnRelease = closeOnRelease;
    }

    public Connection getOriginalConnection() {
        return connection;
    }

    public IHMStatement createStatement() throws SQLException {
        IHMStatement statement = new HMStatement(connection.createStatement());
        return statement;
    }

    @Override
    public IHMPreparedStatement prepareStatement( String sql ) throws SQLException {
        PreparedStatement tmp = connection.prepareStatement(sql);
        IHMPreparedStatement preparedStatement = new HMPreparedStatement(tmp);
        return preparedStatement;
    }

    @Override
    public IHMPreparedStatement prepareStatement( String sql, int returnGeneratedKeys ) throws SQLException {
        PreparedStatement tmp = connection.prepareStatement(sql, returnGeneratedKeys);
        IHMPreparedStatement preparedStatement = new HMPreparedStatement(tmp);
        return preparedStatement;
    }

    @Override
    public void close() throws Exception {
        connection.close();
    }

    @Override
    public boolean getAutoCommit() throws SQLException {
        return connection.getAutoCommit();
    }

    @Override
    public void setAutoCommit( boolean b ) throws SQLException {
        connection.setAutoCommit(b);
    }

    @Override
    public void commit() throws SQLException {
        connection.commit();
    }

    @Override
    public Savepoint setSavepoint() throws SQLException {
        return connection.setSavepoint();
    }

    @Override
    public void rollback( Savepoint savepoint ) throws SQLException {
        connection.rollback(savepoint);
    }

    @Override
    public void rollback() throws Exception {
        connection.rollback();
    }

    public void enableAutocommit( boolean enable ) throws Exception {
        boolean autoCommitEnabled = getAutoCommit();
        if (enable && !autoCommitEnabled) {
            // do enable if not already enabled
            setAutoCommit(true);
        } else if (!enable && autoCommitEnabled) {
            // disable if not already disabled
            setAutoCommit(false);
        }
    }

    @Override
    public void release() throws Exception {
        if (closeOnRelease) {
            connection.close();
        }
    }

}
