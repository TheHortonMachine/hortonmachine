package org.jgrasstools.gears.spatialite.jgt;

import java.sql.ResultSetMetaData;

import org.jgrasstools.gears.spatialite.compat.IJGTResultSetMetaData;

public class JGTResultSetMetaData implements IJGTResultSetMetaData {

    private ResultSetMetaData resultSetMetaData;

    public JGTResultSetMetaData( ResultSetMetaData resultSetMetaData ) {
        this.resultSetMetaData = resultSetMetaData;
    }

    @Override
    public int getColumnCount() throws Exception {
        return resultSetMetaData.getColumnCount();
    }

    @Override
    public String getColumnName( int index ) throws Exception {
        return resultSetMetaData.getColumnName(index);
    }

    @Override
    public String getColumnTypeName( int index ) throws Exception {
        return resultSetMetaData.getColumnTypeName(index);
    }

    @Override
    public int getColumnType( int index ) throws Exception {
        return resultSetMetaData.getColumnType(index);
    }

}
