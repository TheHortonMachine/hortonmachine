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
package org.jgrasstools.dbs.compat;

/**
 * Interface wrapping db connections.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public interface IJGTConnection extends AutoCloseable {
    public IJGTStatement createStatement() throws Exception;

    public boolean getAutoCommit() throws Exception;

    public void setAutoCommit( boolean b ) throws Exception;

    public void commit() throws Exception;

    public IJGTPreparedStatement prepareStatement( String sql ) throws Exception;

    public IJGTPreparedStatement prepareStatement( String sql, int returnGeneratedKeys ) throws Exception;
}
