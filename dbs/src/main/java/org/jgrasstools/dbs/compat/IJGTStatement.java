package org.jgrasstools.dbs.compat;

public interface IJGTStatement extends AutoCloseable {

    void execute( String sql ) throws Exception;

    IJGTResultSet executeQuery( String sql ) throws Exception;

    void setQueryTimeout( int seconds ) throws Exception;

    int executeUpdate( String sql ) throws Exception;

    void addBatch( String sqlLine ) throws Exception;

    int[] executeBatch() throws Exception;

}
