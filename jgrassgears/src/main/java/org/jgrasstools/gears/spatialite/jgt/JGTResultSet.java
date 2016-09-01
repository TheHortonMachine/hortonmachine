package org.jgrasstools.gears.spatialite.jgt;

import java.sql.ResultSet;

import org.jgrasstools.gears.spatialite.compat.IJGTResultSet;
import org.jgrasstools.gears.spatialite.compat.IJGTResultSetMetaData;

public class JGTResultSet implements IJGTResultSet {

    private ResultSet resultSet;

    public JGTResultSet( ResultSet resultSet ) {
        this.resultSet = resultSet;
    }

    @Override
    public void close() throws Exception {
        resultSet.close();
    }

    @Override
    public boolean next() throws Exception {
        return resultSet.next();
    }

    @Override
    public String getString( int index ) throws Exception {
        return resultSet.getString(index);
    }

    @Override
    public int getInt( int index ) throws Exception {
        return resultSet.getInt(index);
    }

    @Override
    public double getDouble( int index ) throws Exception {
        return resultSet.getDouble(index);
    }

    @Override
    public Object getObject( int index ) throws Exception {
        return resultSet.getObject(index);
    }

    @Override
    public long getLong( int index ) throws Exception {
        return resultSet.getLong(index);
    }

    @Override
    public byte[] getBytes( int index ) throws Exception {
        return resultSet.getBytes(index);
    }

    @Override
    public IJGTResultSetMetaData getMetaData() throws Exception {
        IJGTResultSetMetaData metaData = new JGTResultSetMetaData(resultSet.getMetaData());
        return metaData;
    }

    @Override
    public short getShort( int index ) throws Exception {
        return resultSet.getShort(index);
    }

    @Override
    public boolean getBoolean( int index ) throws Exception {
        return resultSet.getBoolean(index);
    }

    @Override
    public boolean wasNull() throws Exception {
        return resultSet.wasNull();
    }

}
