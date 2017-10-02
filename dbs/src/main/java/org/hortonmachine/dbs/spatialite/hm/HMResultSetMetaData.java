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

import java.sql.ResultSetMetaData;

import org.hortonmachine.dbs.compat.IHMResultSetMetaData;

/**
 * Resultset metadata wrapper for standard jdbc java.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class HMResultSetMetaData implements IHMResultSetMetaData {

    private ResultSetMetaData resultSetMetaData;

    public HMResultSetMetaData( ResultSetMetaData resultSetMetaData ) {
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
