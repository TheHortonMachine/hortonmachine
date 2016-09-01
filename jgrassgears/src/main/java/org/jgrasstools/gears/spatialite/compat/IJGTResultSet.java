package org.jgrasstools.gears.spatialite.compat;

public interface IJGTResultSet extends AutoCloseable {

    boolean next() throws Exception;

    String getString( int index ) throws Exception;

    int getInt( int index ) throws Exception;

    double getDouble( int index ) throws Exception;

    Object getObject( int index ) throws Exception;

    long getLong( int index ) throws Exception;

    byte[] getBytes( int index ) throws Exception;

    IJGTResultSetMetaData getMetaData() throws Exception;

    short getShort( int index )throws Exception;

    boolean getBoolean( int index )throws Exception;

    boolean wasNull() throws Exception;
}
