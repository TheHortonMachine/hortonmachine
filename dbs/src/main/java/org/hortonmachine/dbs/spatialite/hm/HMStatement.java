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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;

/**
 * Statement wrapper for standard jdbc java.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class HMStatement implements IHMStatement {

    private Statement statement;

    public HMStatement( Statement statement ) {
        this.statement = statement;
    }

    @Override
    public void close() throws SQLException {
        statement.close();
    }

    @Override
    public void execute( String sql ) throws SQLException {
        statement.execute(sql);
    }

    @Override
    public IHMResultSet executeQuery( String sql ) throws SQLException {
        ResultSet resultSet = statement.executeQuery(sql);
        IHMResultSet ijgtResultSet = new HMResultSet(resultSet);
        return ijgtResultSet;
    }

    @Override
    public void setQueryTimeout( int seconds ) throws SQLException {
        statement.setQueryTimeout(seconds);
    }

    @Override
    public int executeUpdate( String sql ) throws Exception {
        return statement.executeUpdate(sql);
    }

    @Override
    public void addBatch( String sqlLine ) throws Exception {
        statement.addBatch(sqlLine);
    }

    @Override
    public int[] executeBatch() throws Exception {
        return statement.executeBatch();
    }

}
