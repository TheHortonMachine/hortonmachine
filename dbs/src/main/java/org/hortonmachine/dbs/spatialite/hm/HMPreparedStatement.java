/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * The HortonMachine is free software: you can redistribute it and/or modify
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
package org.hortonmachine.dbs.spatialite.hm;

import java.io.ByteArrayInputStream;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;

/**
 * Prepared Statement wrapper for standard jdbc java.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class HMPreparedStatement implements IHMPreparedStatement {

    private PreparedStatement preparedStatement;

    public HMPreparedStatement( PreparedStatement preparedStatement ) {
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
    public int executeUpdate() throws Exception {
        return preparedStatement.executeUpdate();
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
    public void setBlob( int index, byte[] value ) throws Exception {
        preparedStatement.setBlob(index, new ByteArrayInputStream(value));
    }

    @Override
    public void setShort( int index, short value ) throws Exception {
        preparedStatement.setShort(index, value);
    }

    @Override
    public void setBoolean( int index, boolean value ) throws Exception {
        preparedStatement.setBoolean(index, value);
    }

    @Override
    public IHMResultSet getGeneratedKeys() throws Exception {
        ResultSet generatedKeysRs = preparedStatement.getGeneratedKeys();
        return new HMResultSet(generatedKeysRs);
    }

    @Override
    public void setObject( int index, Object value ) throws Exception {
        preparedStatement.setObject(index, value);
    }

    @Override
    public IHMResultSet executeQuery() throws Exception {
        return new HMResultSet(preparedStatement.executeQuery());
    }

}
