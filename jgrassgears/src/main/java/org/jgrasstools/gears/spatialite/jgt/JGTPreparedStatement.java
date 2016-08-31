package org.jgrasstools.gears.spatialite.jgt;

import java.sql.PreparedStatement;
import java.sql.ResultSet;

import org.jgrasstools.gears.spatialite.compat.IJGTPreparedStatement;

public class JGTPreparedStatement implements IJGTPreparedStatement {

    private PreparedStatement preparedStatement;

    public JGTPreparedStatement( PreparedStatement preparedStatement ) {
        this.preparedStatement = preparedStatement;
    }

    @Override
    public void close() throws Exception {
        preparedStatement.close();
    }

    @Override
    public void setString( int index, String text ) throws Exception {
        preparedStatement.setString(index, text);
    }

    @Override
    public void executeUpdate() throws Exception {
        preparedStatement.executeUpdate();
    }

    @Override
    public void setDouble( int index, double value ) throws Exception {
        preparedStatement.setDouble(index, value);
    }

    @Override
    public void setFloat( int index, float value ) throws Exception {
        preparedStatement.setFloat(index, value);
    }

    @Override
    public void setInt( int index, int value ) throws Exception {
        preparedStatement.setInt(index, value);
    }

    @Override
    public void addBatch() throws Exception {
        preparedStatement.addBatch();
    }

    @Override
    public int[] executeBatch() throws Exception {
        return preparedStatement.executeBatch();
    }

    @Override
    public void setLong( int index, long value ) throws Exception {
        preparedStatement.setLong(index, value);
    }

    @Override
    public void setBytes( int index, byte[] value ) throws Exception {
        preparedStatement.setBytes(index, value);
    }

    @Override
    public void setShort( int index, short value ) throws Exception {
        preparedStatement.setShort(index, value);
    }

    @Override
    public ResultSet getGeneratedKeys() throws Exception {
        return preparedStatement.getGeneratedKeys();
    }

}
