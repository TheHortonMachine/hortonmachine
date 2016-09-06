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
package org.jgrasstools.dbs.spatialite.jgt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jgrasstools.dbs.compat.IJGTConnection;
import org.jgrasstools.dbs.compat.IJGTPreparedStatement;
import org.jgrasstools.dbs.compat.IJGTStatement;

/**
 * Connection wrapper for standard jdbc java.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class JGTConnection implements IJGTConnection {

    private Connection connection;

    public JGTConnection( Connection connection ) {
        this.connection = connection;
    }

    public Connection getOriginalConnection() {
        return connection;
    }

    public IJGTStatement createStatement() throws SQLException {
        IJGTStatement statement = new JGTStatement(connection.createStatement());
        return statement;
    }

    @Override
    public IJGTPreparedStatement prepareStatement( String sql ) throws SQLException {
        PreparedStatement tmp = connection.prepareStatement(sql);
        IJGTPreparedStatement preparedStatement = new JGTPreparedStatement(tmp);
        return preparedStatement;
    }

    @Override
    public IJGTPreparedStatement prepareStatement( String sql, int returnGeneratedKeys ) throws SQLException {
        PreparedStatement tmp = connection.prepareStatement(sql, returnGeneratedKeys);
        IJGTPreparedStatement preparedStatement = new JGTPreparedStatement(tmp);
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

}
