package org.jgrasstools.dbs.spatialite.jgt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.jgrasstools.dbs.compat.IJGTConnection;
import org.jgrasstools.dbs.compat.IJGTPreparedStatement;
import org.jgrasstools.dbs.compat.IJGTStatement;


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
