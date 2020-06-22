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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;

/**
 * A logging database.
 * 
 * <p>The database contains a table with [id, timestamp, message type, tag, log message]</p>.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class LogDb implements AutoCloseable, ILogDb {
    public static final String TABLE_MESSAGES = "logmessages";

    // TABLE_EVENTS
    public static final String ID_NAME = "id";
    public static final String TimeStamp_NAME = "ts";
    public static final String type_NAME = "type";
    public static final String tag_NAME = "tag";
    public static final String message_NAME = "msg";

    private ADb logDb;
    private boolean isOpen = false;

    public LogDb() throws Exception {
        this(EDb.SQLITE);
    }

    public LogDb( EDb dbType ) throws Exception {
        logDb = dbType.getDb();
    }

    public LogDb( ADb existingDb ) throws Exception {
        logDb = existingDb;
        if (!logDb.hasTable(TABLE_MESSAGES)) {
            createTable();
            createIndexes();
        }
        isOpen = true;
    }

    public boolean open( String dbPath ) throws Exception {
        if (isOpen) {
            return true;
        } else {
            boolean open = logDb.open(dbPath);
            if (!logDb.hasTable(TABLE_MESSAGES)) {
                createTable();
                createIndexes();
            }
            return open;
        }
    }

    public void createTable() throws Exception {
        if (logDb != null && !logDb.hasTable(TABLE_MESSAGES)) {

            ADatabaseSyntaxHelper helper = logDb.getType().getDatabaseSyntaxHelper();

            String[] fields = { //
                    ID_NAME + " " + helper.LONG_PRIMARYKEY_AUTOINCREMENT(), //
                    TimeStamp_NAME + " " + helper.LONG(), //
                    type_NAME + " " + helper.INTEGER(), //
                    tag_NAME + " " + helper.TEXT(), //
                    message_NAME + " " + helper.TEXT()//
            };

            StringBuilder sb = new StringBuilder();
            sb.append("CREATE TABLE ");
            sb.append(TABLE_MESSAGES).append("(");
            for( int i = 0; i < fields.length; i++ ) {
                if (i != 0) {
                    sb.append(",");
                }
                sb.append(fields[i]);
            }
            sb.append(");");

            String sql = logDb.getType().getDatabaseSyntaxHelper().checkSqlCompatibilityIssues(sb.toString());
            logDb.execOnConnection(connection -> {
                try (IHMStatement stmt = connection.createStatement()) {
                    stmt.execute(sql);
                }
                return null;
            });

        }
    }

    public void createIndexes() throws Exception {
        if (logDb != null && logDb.hasTable(TABLE_MESSAGES)) {
            logDb.createIndex(TABLE_MESSAGES, TimeStamp_NAME, false);
            logDb.createIndex(TABLE_MESSAGES, type_NAME, false);
        }
    }

    public String getQueryFieldsString() {
        String queryFields = ID_NAME + "," + //
                TimeStamp_NAME + "," + //
                type_NAME + "," + //
                tag_NAME + "," + //
                message_NAME //
        ;
        return queryFields;
    }

    public String getInsertFieldsString() {
        String insertFields = TimeStamp_NAME + "," + //
                type_NAME + "," + //
                tag_NAME + "," + //
                message_NAME //
        ;
        return insertFields;
    }

    @Override
    public boolean insert( final Message message ) throws Exception {
        // the id is generated
        String sql = "INSERT INTO " + TABLE_MESSAGES + //
                " (" + getInsertFieldsString() + //
                ") VALUES (?,?,?,?)";

        logDb.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                int i = 1;
                pStmt.setLong(i++, message.ts);
                pStmt.setLong(i++, message.type);
                pStmt.setString(i++, message.tag);
                pStmt.setString(i++, message.msg);

                pStmt.executeUpdate();
            }
            return null;
        });
        return true;
    }

    @Override
    public boolean insert( EMessageType type, final String tag, String msg ) throws Exception {
        // the id is generated
        String sql = "INSERT INTO " + TABLE_MESSAGES + //
                " (" + getInsertFieldsString() + //
                ") VALUES (?,?,?,?)";

        logDb.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                int i = 1;
                pStmt.setLong(i++, new Date().getTime());
                pStmt.setLong(i++, type.getCode());
                pStmt.setString(i++, tag == null ? "" : tag);
                pStmt.setString(i++, msg);
                pStmt.executeUpdate();
            }
            return null;
        });
        return true;
    }

    @Override
    public boolean insertInfo( String tag, String msg ) throws Exception {
        return insert(EMessageType.INFO, tag, msg);
    }
    @Override
    public boolean i( String msg ) throws Exception {
        return insert(EMessageType.INFO, null, msg);
    }

    @Override
    public boolean insertWarning( String tag, String msg ) throws Exception {
        return insert(EMessageType.WARNING, tag, msg);
    }
    @Override
    public boolean w( String msg ) throws Exception {
        return insert(EMessageType.WARNING, null, msg);
    }

    @Override
    public boolean insertDebug( String tag, String msg ) throws Exception {
        return insert(EMessageType.DEBUG, tag, msg);
    }
    @Override
    public boolean d( String msg ) throws Exception {
        return insert(EMessageType.DEBUG, null, msg);
    }

    @Override
    public boolean insertAccess( String tag, String msg ) throws Exception {
        return insert(EMessageType.ACCESS, tag, msg);
    }
    @Override
    public boolean a( String msg ) throws Exception {
        return insert(EMessageType.ACCESS, null, msg);
    }

    @Override
    public boolean e( String msg, Throwable t ) throws Exception {
        return insertError(null, msg, t);
    }
    public boolean e( String msg ) throws Exception {
        return insertError(null, msg, null);
    }
    @Override
    public boolean insertError( String tag, String msg, Throwable t ) throws Exception {
        // the id is generated
        String sql = "INSERT INTO " + TABLE_MESSAGES + //
                " (" + getInsertFieldsString() + //
                ") VALUES (?,?,?,?)";

        if (msg == null) {
            msg = "";
        }
        if (t != null) {
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            String printStackTrace = sw.toString();

            msg += printStackTrace;
        }
        final String _msg = msg;
        logDb.execOnConnection(connection -> {
            try (IHMPreparedStatement pStmt = connection.prepareStatement(sql)) {
                int i = 1;
                pStmt.setLong(i++, new Date().getTime());
                pStmt.setLong(i++, EMessageType.ERROR.getCode());
                pStmt.setString(i++, tag == null ? "" : tag);
                pStmt.setString(i++, _msg);
                pStmt.executeUpdate();
            }
            return null;
        });
        return true;
    }

    @Override
    public List<Message> getList() throws Exception {
        String tableName = TABLE_MESSAGES;
        String sql = "select " + getQueryFieldsString() + " from " + tableName;

        List<Message> messages = new ArrayList<Message>();
        logDb.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql);) {
                while( rs.next() ) {
                    Message event = resultSetToItem(rs);
                    messages.add(event);
                }
            }
            return null;
        });
        return messages;
    }

    /**
     * Get the list of messages, filtered.
     *  
     * @param messageType the type to filter.
     * @param fromTsMillis the start time in millis.
     * @param toTsMillis the end time in millis.
     * @param limit the max number of messages.
     * @return the list of messages.
     * @throws Exception
     */
    public List<Message> getFilteredList( EMessageType messageType, Long fromTsMillis, Long toTsMillis, long limit )
            throws Exception {
        String tableName = TABLE_MESSAGES;
        String sql = "select " + getQueryFieldsString() + " from " + tableName;

        List<String> wheresList = new ArrayList<>();
        if (messageType != null && messageType != EMessageType.ALL) {
            String where = type_NAME + "=" + messageType.getCode();
            wheresList.add(where);
        }
        if (fromTsMillis != null) {
            String where = TimeStamp_NAME + ">" + fromTsMillis;
            wheresList.add(where);
        }
        if (toTsMillis != null) {
            String where = TimeStamp_NAME + "<" + toTsMillis;
            wheresList.add(where);
        }

        if (wheresList.size() > 0) {
            sql += " WHERE ";
            for( int i = 0; i < wheresList.size(); i++ ) {
                if (i > 0) {
                    sql += " AND ";
                }
                sql += wheresList.get(i);
            }
        }

        sql += " order by " + ID_NAME + " desc";
        if (limit > 0) {
            sql += " limit " + limit;
        }

        String _sql = sql;
        List<Message> messages = new ArrayList<Message>();
        logDb.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql);) {
                while( rs.next() ) {
                    Message event = resultSetToItem(rs);
                    messages.add(event);
                }
            }
            return null;
        });
        return messages;
    }

    public Message resultSetToItem( IHMResultSet rs ) throws Exception {
        int i = 1;
        Message message = new Message();
        message.id = rs.getLong(i++);
        message.ts = rs.getLong(i++);
        message.type = rs.getInt(i++);
        message.tag = rs.getString(i++);
        message.msg = rs.getString(i++);
        return message;
    }

    @Override
    public void clearTable() throws Exception {
        logDb.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement()) {
                stmt.execute("delete from " + TABLE_MESSAGES);
            }
            return null;
        });
    }

    @Override
    public void close() throws Exception {
        if (logDb != null)
            logDb.close();
    }

    @Override
    public String getDatabasePath() {
        if (logDb != null)
            return logDb.getDatabasePath();
        return null;
    }

    @Override
    public void setOutPrintStream( PrintStream printStream ) {
    }

    @Override
    public void setErrPrintStream( PrintStream printStream ) {
    }

}
