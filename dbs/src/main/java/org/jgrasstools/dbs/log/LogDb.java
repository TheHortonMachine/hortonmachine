package org.jgrasstools.dbs.log;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import org.jgrasstools.dbs.compat.ADb;
import org.jgrasstools.dbs.compat.EDb;
import org.jgrasstools.dbs.compat.IJGTConnection;
import org.jgrasstools.dbs.compat.IJGTPreparedStatement;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.joda.time.DateTime;

public class LogDb implements AutoCloseable {
    public static final String TABLE_MESSAGES = "logmessages";

    // TABLE_EVENTS
    public static final String ID_NAME = "id";
    public static final String TimeStamp_NAME = "ts";
    public static final String type_NAME = "type";
    public static final String tag_NAME = "tag";
    public static final String message_NAME = "msg";

    private ADb logDb;

    private IJGTConnection mConn;

    public LogDb() throws Exception {
        this(EDb.SQLITE);
    }

    public LogDb( EDb dbType ) throws Exception {
        logDb = dbType.getDb();
    }

    public boolean open( String dbPath ) throws Exception {
        boolean open = logDb.open(dbPath);
        mConn = logDb.getConnection();
        if (!open) {
            createTable();
            createIndexes();
        }
        return open;
    }

    public void createTable() throws Exception {
        if (!logDb.hasTable(TABLE_MESSAGES)) {
            String[] fields = { //
                    ID_NAME + " LONG PRIMARY KEY AUTOINCREMENT", //
                    TimeStamp_NAME + " LONG", //
                    type_NAME + " INTEGER", //
                    tag_NAME + " TEXT", //
                    message_NAME + " TEXT"//
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

            String sql = logDb.checkSqlCompatibilityIssues(sb.toString());
            try (IJGTStatement stmt = mConn.createStatement()) {
                stmt.execute(sql);
            }

        }
    }

    public void createIndexes() throws Exception {
        if (logDb.hasTable(TABLE_MESSAGES)) {
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

    public boolean insert( final Message message ) throws Exception {
        // the id is generated
        String sql = "INSERT INTO " + TABLE_MESSAGES + //
                " (" + getInsertFieldsString() + //
                ") VALUES (?,?,?,?)";

        try (IJGTPreparedStatement pStmt = mConn.prepareStatement(sql)) {
            int i = 1;
            pStmt.setLong(i++, message.ts);
            pStmt.setLong(i++, message.type);
            pStmt.setString(i++, message.tag);
            pStmt.setString(i++, message.msg);

            pStmt.executeUpdate();
        }
        return true;
    }

    public boolean insert( EMessageType type, String tag, String msg ) throws Exception {
        // the id is generated
        String sql = "INSERT INTO " + TABLE_MESSAGES + //
                " (" + getInsertFieldsString() + //
                ") VALUES (?,?,?,?)";

        try (IJGTPreparedStatement pStmt = mConn.prepareStatement(sql)) {
            int i = 1;
            pStmt.setLong(i++, new DateTime().getMillis());
            pStmt.setLong(i++, type.getCode());
            pStmt.setString(i++, tag);
            pStmt.setString(i++, msg);
            pStmt.executeUpdate();
        }
        return true;
    }

    public boolean insertError( String tag, String msg, Throwable t ) throws Exception {
        // the id is generated
        String sql = "INSERT INTO " + TABLE_MESSAGES + //
                " (" + getInsertFieldsString() + //
                ") VALUES (?,?,?,?)";

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        String printStackTrace = sw.toString();
        if (msg == null) {
            msg = "";
        }

        msg += printStackTrace;

        try (IJGTPreparedStatement pStmt = mConn.prepareStatement(sql)) {
            int i = 1;
            pStmt.setLong(i++, new DateTime().getMillis());
            pStmt.setLong(i++, EMessageType.ERROR.getCode());
            pStmt.setString(i++, tag);
            pStmt.setString(i++, msg);
            pStmt.executeUpdate();
        }
        return true;
    }

    public List<Message> getList() throws Exception {
        String tableName = TABLE_MESSAGES;
        String sql = "select " + getQueryFieldsString() + " from " + tableName;

        List<Message> messages = new ArrayList<Message>();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql);) {
            while( rs.next() ) {
                Message event = resultSetToItem(rs);
                messages.add(event);
            }
            return messages;
        }
    }

    public List<Message> getFilteredList( EMessageType messageType, DateTime fromTs, DateTime toTs, long limit )
            throws Exception {
        String tableName = TABLE_MESSAGES;
        String sql = "select " + getQueryFieldsString() + " from " + tableName;

        List<String> wheresList = new ArrayList<>();
        if (messageType != null && messageType != EMessageType.ALL) {
            String where = type_NAME + "=" + messageType.getCode();
            wheresList.add(where);
        }
        if (fromTs != null) {
            String where = TimeStamp_NAME + ">" + fromTs.getMillis();
            wheresList.add(where);
        }
        if (toTs != null) {
            String where = TimeStamp_NAME + "<" + toTs.getMillis();
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

        List<Message> messages = new ArrayList<Message>();
        try (IJGTStatement stmt = mConn.createStatement(); IJGTResultSet rs = stmt.executeQuery(sql);) {
            while( rs.next() ) {
                Message event = resultSetToItem(rs);
                messages.add(event);
            }
            return messages;
        }
    }

    public Message resultSetToItem( IJGTResultSet rs ) throws Exception {
        int i = 1;
        Message message = new Message();
        message.id = rs.getLong(i++);
        message.ts = rs.getLong(i++);
        message.type = rs.getInt(i++);
        message.tag = rs.getString(i++);
        message.msg = rs.getString(i++);
        return message;
    }

    public void clearTable() throws Exception {
        try (IJGTStatement stmt = mConn.createStatement()) {
            stmt.execute("delete from " + TABLE_MESSAGES);
        }
    }

    @Override
    public void close() throws Exception {
        logDb.close();
    }

    public String getDatabasePath() {
        return logDb.getDatabasePath();
    }

}
