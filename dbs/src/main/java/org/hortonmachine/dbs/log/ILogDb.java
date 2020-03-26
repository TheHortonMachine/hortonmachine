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
package org.hortonmachine.dbs.log;

import java.io.PrintStream;
import java.util.List;

/**
 * A log db interface.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface ILogDb {

    boolean insert( Message message ) throws Exception;

    boolean insert( EMessageType type, String tag, String msg ) throws Exception;

    boolean insertInfo( String tag, String msg ) throws Exception;
    boolean i( String msg ) throws Exception;

    boolean insertWarning( String tag, String msg ) throws Exception;
    boolean w( String msg ) throws Exception;

    boolean insertDebug( String tag, String msg ) throws Exception;
    boolean d( String msg ) throws Exception;

    boolean insertAccess( String tag, String msg ) throws Exception;
    boolean a( String msg ) throws Exception;

    boolean insertError( String tag, String msg, Throwable t ) throws Exception;
    boolean e( String msg, Throwable t ) throws Exception;
    boolean e( String msg ) throws Exception;

    List<Message> getList() throws Exception;

    void clearTable() throws Exception;

    void close() throws Exception;

    String getDatabasePath();

    public void setOutPrintStream( PrintStream printStream );

    public void setErrPrintStream( PrintStream printStream );
}