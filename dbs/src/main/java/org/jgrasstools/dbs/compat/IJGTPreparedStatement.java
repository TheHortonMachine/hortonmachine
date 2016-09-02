package org.jgrasstools.dbs.compat;

import java.sql.ResultSet;

public interface IJGTPreparedStatement extends AutoCloseable {

    void setString( int index, String text ) throws Exception;

    void executeUpdate() throws Exception;

    void setDouble( int index, double value ) throws Exception;

    void setFloat( int index, float value ) throws Exception;

    void setInt( int index, int value ) throws Exception;

    void addBatch() throws Exception;

    int[] executeBatch() throws Exception;

    void setLong( int index, long value ) throws Exception;

    void setBytes( int index, byte[] value ) throws Exception;

    void setShort( int index, short value ) throws Exception;

    ResultSet getGeneratedKeys() throws Exception;

}
