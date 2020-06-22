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
import java.util.Collections;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;

/**
 * A logger that logs to database.
 * 
 * <p>If the database is not initialized, it can either not log or to standard out and err.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public enum Logger implements ILogDb {
    INSTANCE;

    private static LogDb db;
    public static PrintStream out = System.out;
    public static PrintStream err = System.err;

    private static boolean doDbLog = false;
    private static final boolean doLogStd = true;

    public String EMPTY = "";

    /**
     * Initialize to log to standard out/err.
     * 
     * @throws Exception
     */
    public void init() throws Exception {
        doDbLog = false;
    }

    /**
     * Initialize to log to db.
     * 
     * @param dbPath
     * @throws Exception
     */
    public void init( String dbPath ) throws Exception {
        if (db == null) {
            db = new LogDb(EDb.SPATIALITE4ANDROID);
            db.open(dbPath);
            doDbLog = true;
        }
    }

    /**
     * Initialize to log to db, defining the type.
     * 
     * @param dbPath
     * @param type
     * @throws Exception
     */
    public void init( String dbPath, EDb type ) throws Exception {
        if (db == null) {
            db = new LogDb(type);
            db.open(dbPath);
            doDbLog = true;
        }
    }
    /**
     * Initialize to log to an existing db.
     * 
     * @param db the database to log to.
     * @throws Exception
     */
    public void init( ADb existingDb ) throws Exception {
        if (db == null) {
            db = new LogDb(existingDb);
            doDbLog = true;
        }
    }

    public boolean i( String msg ) {
        return insertInfo(null, msg);
    }
    public boolean insertInfo( String tag, String msg ) {
        if (doLogStd) {
            out.println("INFO:: " + getTag(tag) + msg);
        }
        if (!doDbLog) {
            return true;
        }
        try {
            return db.insertInfo(tag, msg);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean w( String msg ) {
        return insertWarning(null, msg);
    }
    public boolean insertWarning( String tag, String msg ) {
        if (doLogStd) {
            out.println("WARNING:: " + getTag(tag) + msg);
        }
        if (!doDbLog) {
            return true;
        }
        try {
            return db.insertWarning(tag, msg);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean d( String msg ) {
        return insertDebug(null, msg);
    }
    public boolean insertDebug( String tag, String msg ) {
        if (doLogStd) {
            out.println("DEBUG:: " + getTag(tag) + msg);
        }
        if (!doDbLog) {
            return true;
        }
        try {
            return db.insertDebug(tag, msg);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean a( String msg ) {
        return insertAccess(null, msg);
    }
    public boolean insertAccess( String tag, String msg ) {
        if (doLogStd) {
            out.println("ACCESS:: " + getTag(tag) + msg);
        }
        if (!doDbLog) {
            return true;
        }
        try {
            return db.insertAccess(tag, msg);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean e( String msg ) {
        return insertError(null, msg, null);
    }
    public boolean e( String msg, Throwable t ) {
        return insertError(null, msg, t);
    }
    public boolean insertError( String tag, String msg, Throwable t ) {
        if (doLogStd) {
            out.println("ERROR:: " + getTag(tag) + msg);
            if (t != null)
                t.printStackTrace();
        }
        if (!doDbLog) {
            return true;
        }

        if (t == null) {
            t = new RuntimeException(msg);
        }
        try {
            return db.insertError("", msg, t);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean insert( Message message ) throws Exception {
        if (doLogStd) {
            out.println(EMessageType.fromCode(message.type) + ":: " + message.tag + ":: " + message.msg);
        }
        if (!doDbLog)
            return true;
        try {
            return db.insert(message);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public boolean insert( EMessageType type, String tag, String msg ) throws Exception {
        if (!doDbLog)
            return true;
        if (doLogStd) {
            out.println(type.name() + ":: " + tag + ":: " + msg);
        }
        if (!doDbLog)
            return true;
        try {
            return db.insert(type, tag, msg);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Message> getList() throws Exception {
        if (db == null)
            return Collections.emptyList();
        return db.getList();
    }

    public List<Message> getFilteredList( EMessageType messageType, Long fromTsMillis, Long toTsMillis, long limit )
            throws Exception {
        if (db == null)
            return Collections.emptyList();
        return db.getFilteredList(messageType, fromTsMillis, toTsMillis, limit);
    }

    public void clearTable() throws Exception {
        if (db != null)
            db.clearTable();
    }

    public void close() throws Exception {
        if (db != null)
            db.close();
    }

    public String getDatabasePath() {
        if (db == null)
            return "";
        return db.getDatabasePath();
    }

    public void setOutPrintStream( PrintStream logAreaPrintStream ) {
        out = logAreaPrintStream;
    }

    public void setErrPrintStream( PrintStream logAreaPrintStream ) {
        err = logAreaPrintStream;
    }

    private String getTag( String tag ) {
        if (tag != null) {
            tag = tag + ":: ";
        } else {
            tag = EMPTY;
        }
        return tag;
    }

    public void resetStreams() {
        out = System.out;
        err = System.err;
    }
}
