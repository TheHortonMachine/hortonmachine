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

import java.sql.Connection;

import javax.sql.DataSource;

/**
 * A database visitor.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface IDbVisitor {
    /**
     * Visit using teh datasource (case of pooled db for example).
     * 
     * @param dataSource the datasource. Could be <code>null</code>.
     */
    void visit( DataSource dataSource ) throws Exception;

    /**
     * Visit using the single connection (case of sqlite/spatialite db). 
     * 
     * @param singleConnection the single connection, could be <code>null</code>.
     */
    void visit( Connection singleConnection ) throws Exception;
}
