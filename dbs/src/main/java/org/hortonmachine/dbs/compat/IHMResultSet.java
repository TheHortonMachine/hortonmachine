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
package org.hortonmachine.dbs.compat;

import java.sql.Date;

/**
 * Interface wrapping a resultset.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface IHMResultSet extends AutoCloseable {

    boolean next() throws Exception;

    String getString( int index ) throws Exception;

    String getString( String name ) throws Exception;

    int getInt( int index ) throws Exception;

    int getInt( String name ) throws Exception;

    double getDouble( int index ) throws Exception;

    double getDouble( String name ) throws Exception;

    Object getObject( int index ) throws Exception;

    long getLong( int index ) throws Exception;

    long getLong( String name ) throws Exception;

    Date getDate( int index ) throws Exception;
    
    Date getDate( String name ) throws Exception;

    byte[] getBytes( int index ) throws Exception;

    IHMResultSetMetaData getMetaData() throws Exception;

    short getShort( int index )throws Exception;

    boolean getBoolean( int index )throws Exception;

    boolean getBoolean( String name)throws Exception;

    boolean wasNull() throws Exception;

    float getFloat( int index ) throws Exception;

    <T> T unwrap(java.lang.Class<T> iface) throws Exception;
}
