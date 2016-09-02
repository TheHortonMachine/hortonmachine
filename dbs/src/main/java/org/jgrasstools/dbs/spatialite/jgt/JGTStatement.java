package org.jgrasstools.dbs.spatialite.jgt;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTStatement;


public class JGTStatement implements IJGTStatement {

    private Statement statement;

    public JGTStatement( Statement statement ) {
        this.statement = statement;
    }

    @Override
    public void close() throws SQLException {
        statement.close();
    }

    @Override
    public void execute( String sql ) throws SQLException {
        statement.execute(sql);
    }

    @Override
    public IJGTResultSet executeQuery( String sql ) throws SQLException {
        ResultSet resultSet = statement.executeQuery(sql);
        IJGTResultSet ijgtResultSet = new JGTResultSet(resultSet);
        return ijgtResultSet;
    }

    @Override
    public void setQueryTimeout( int seconds ) throws SQLException {
        statement.setQueryTimeout(seconds);
    }

    @Override
    public int executeUpdate( String sql ) throws Exception {
        return statement.executeUpdate(sql);
    }

    @Override
    public void addBatch( String sqlLine ) throws Exception {
        statement.addBatch(sqlLine);
    }

    @Override
    public int[] executeBatch() throws Exception {
        return statement.executeBatch();
    }

}
