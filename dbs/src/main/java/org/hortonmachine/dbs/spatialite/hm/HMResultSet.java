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

import java.sql.Date;
import java.sql.ResultSet;

import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMResultSetMetaData;

/**
 * Resultset wrapper for standard jdbc java.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class HMResultSet implements IHMResultSet {

    private ResultSet resultSet;

    public HMResultSet( ResultSet resultSet ) {
        this.resultSet = resultSet;
    }

    public ResultSet getResultSet() {
        return resultSet;
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
    public String getString( String name ) throws Exception {
        return resultSet.getString(name);
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
    public float getFloat( int index ) throws Exception {
        return resultSet.getFloat(index);
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
    public IHMResultSetMetaData getMetaData() throws Exception {
        IHMResultSetMetaData metaData = new HMResultSetMetaData(resultSet.getMetaData());
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

    public boolean getBoolean( String name ) throws Exception {
        return resultSet.getBoolean(name);
    }

    @Override
    public boolean wasNull() throws Exception {
        return resultSet.wasNull();
    }

    @Override
    public int getInt( String name ) throws Exception {
        return resultSet.getInt(name);
    }

    @Override
    public double getDouble( String name ) throws Exception {
        return resultSet.getDouble(name);
    }

    @Override
    public long getLong( String name ) throws Exception {
        return resultSet.getLong(name);
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws Exception {
        return resultSet.unwrap(iface);
    }

    @Override
    public Date getDate( int index ) throws Exception {
        return resultSet.getDate(index);
    }

    @Override
    public Date getDate( String name ) throws Exception {
        throw new RuntimeException("Function not supported: getDate( String name)");
    }

}
