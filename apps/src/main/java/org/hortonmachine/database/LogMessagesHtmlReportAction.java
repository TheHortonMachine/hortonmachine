/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.database;

import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.AbstractAction;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.log.EMessageType;
import org.hortonmachine.dbs.log.LogDb;
import org.hortonmachine.dbs.log.Message;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.simplereport.HtmlReport;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.ImageCache;
import org.joda.time.DateTime;

/**
 * Action that reads a table of log messages and opens them as an HTML report, one row per
 * message grouped by day+hour.
 *
 * <p>
 * Handles two table shapes: the standard {@link LogDb#TABLE_MESSAGES} written by {@link LogDb},
 * and the legacy "debug" ("smash"/GPLog) table, which has a different column layout and encodes
 * the message type as a text label rather than {@link LogDb}'s numeric code.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class LogMessagesHtmlReportAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final ADb db;
    private final boolean isSmash;

    public LogMessagesHtmlReportAction( ADb db, boolean isSmash ) {
        super("Show HTML report", ImageCache.get(ImageCache.BROWSER));
        this.db = db;
        this.isSmash = isSmash;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        try {
            String sql = "select  " + LogDb.type_NAME + ", " + LogDb.TimeStamp_NAME + ", " + LogDb.tag_NAME + ","
                    + LogDb.message_NAME + " from " + LogDb.TABLE_MESSAGES + " order by " + LogDb.TimeStamp_NAME + " desc";
            if (isSmash) {
                sql = "select level, ts, \"GPLOG\", msg from debug order by ts desc";
            }
            String _sql = sql;
            LinkedHashMap<String, List<Message>> day2MessageMap = new LinkedHashMap<>();
            db.execOnConnection(connection -> {
                try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(_sql)) {
                    while( rs.next() ) {

                        int type;
                        if (isSmash) {
                            String typeStr = rs.getString(1);
                            if (typeStr.contains("info")) {
                                type = EMessageType.INFO.getCode();
                            } else if (typeStr.contains("debug")) {
                                type = EMessageType.DEBUG.getCode();
                            } else if (typeStr.contains("warning")) {
                                type = EMessageType.WARNING.getCode();
                            } else if (typeStr.contains("error")) {
                                type = EMessageType.ERROR.getCode();
                            } else if (typeStr.contains("access")) {
                                type = EMessageType.ACCESS.getCode();
                            } else {
                                type = EMessageType.INFO.getCode();
                            }
                        } else {
                            type = rs.getInt(1);
                        }
                        long ts = rs.getLong(2);
                        String tag = rs.getString(3);
                        if (tag == null)
                            tag = "";
                        String msg = rs.getString(4);
                        String tsString = new DateTime(ts).toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS);
                        String[] split = tsString.split(":");
                        String dayHour = split[0];

                        Message logMsg = new Message();
                        logMsg.tag = tag;
                        logMsg.msg = msg;
                        logMsg.ts = ts;
                        logMsg.type = type;

                        List<Message> messages = day2MessageMap.get(dayHour);
                        if (messages == null) {
                            messages = new ArrayList<Message>();
                            day2MessageMap.put(dayHour, messages);
                        }
                        messages.add(logMsg);
                    }
                    return "";
                }
            });

            HtmlReport rep = new HtmlReport();
            StringBuilder sb = new StringBuilder();
            rep.openReport(sb, "Log Messages");
            rep.openTable(sb, 98);
            String white = "#FFFFFF";
            String warning = "#ffb380";
            String debug = "#afe9af";
            String error = "#ff5555";
            String header = "#e6e6e6";
            String oddRow = "#f2f2f2";
            String evenRow = "#d5f9fe";

            rep.openTableRow(sb);
            rep.openTableCell(sb, header, "11", null, null);
            sb.append("DAY + HOUR ");
            rep.closeTableCell(sb);

            rep.openTableCell(sb, header, "6", null, null);
            sb.append("MIN:SEC");
            rep.closeTableCell(sb);

            rep.openTableCell(sb, header, "13", null, null);
            sb.append("TAG");
            rep.closeTableCell(sb);

            rep.openTableCell(sb, header, "80", null, null);
            sb.append("MESSAGE");
            rep.closeTableCell(sb);

            rep.closeTableRow(sb);

            boolean odd = false;
            for( Entry<String, List<Message>> entry : day2MessageMap.entrySet() ) {
                String day = entry.getKey();
                List<Message> msgList = entry.getValue();

                rep.openTableRow(sb);

                String firstColor = evenRow;
                if (odd) {
                    firstColor = oddRow;
                }
                odd = !odd;

                int rowSpan = msgList.size() + 1;
                rep.openTableCell(sb, firstColor, "11", null, rowSpan + "");
                sb.append(day);
                rep.closeTableCell(sb);

                rep.closeTableRow(sb);

                for( Message message : msgList ) {
                    String color = white;
                    if (message.type == EMessageType.DEBUG.getCode()) {
                        color = debug;
                    } else if (message.type == EMessageType.WARNING.getCode()) {
                        color = warning;
                    } else if (message.type == EMessageType.ERROR.getCode()) {
                        color = error;
                    }
                    String tsString = new DateTime(message.ts).toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS);
                    String[] split = tsString.split(":");
                    String time = split[1] + ":" + split[2];

                    rep.openTableRow(sb);

                    rep.openTableCell(sb, color, "6", null, null);
                    sb.append(time);
                    rep.closeTableCell(sb);

                    if (message.tag.length() == 0) {
                        rep.openTableCell(sb, color, "80", "2", null);
                        sb.append("<pre>").append(message.msg).append("</pre>");
                        rep.closeTableCell(sb);
                    } else {
                        rep.openTableCell(sb, color, "13", null, null);
                        sb.append("<b>").append(message.tag).append("<b>");
                        rep.closeTableCell(sb);

                        rep.openTableCell(sb, color, "70", null, null);
                        sb.append("<pre>").append(message.msg).append("</pre>");
                        rep.closeTableCell(sb);
                    }

                    rep.closeTableRow(sb);
                }

            }

            rep.closeTable(sb);
            rep.closeReport(sb);

            File tmpFile = File.createTempFile("HM-", "_debug.html");
            FileUtilities.writeFile(sb.toString(), tmpFile);
            GuiUtilities.openFile(tmpFile);

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
