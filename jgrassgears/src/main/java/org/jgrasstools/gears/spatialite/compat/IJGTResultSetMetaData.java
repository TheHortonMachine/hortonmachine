package org.jgrasstools.gears.spatialite.compat;

public interface IJGTResultSetMetaData {

    int getColumnCount() throws Exception;

    String getColumnName( int index ) throws Exception;

    String getColumnTypeName( int index ) throws Exception;

    int getColumnType( int index ) throws Exception;

}
