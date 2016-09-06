/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.dbs.spatialite.jgt;

import java.sql.ResultSet;

import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTResultSetMetaData;

/**
 * Resultset wrapper for standard jdbc java.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
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
