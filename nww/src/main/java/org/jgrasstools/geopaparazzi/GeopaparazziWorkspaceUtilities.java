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
package org.jgrasstools.geopaparazzi;

import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_METADATA;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.MetadataTableFields;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TimeUtilities;

/**
 * Workspace utils.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class GeopaparazziWorkspaceUtilities {

    public static List<HashMap<String, String>> readProjectMetadata( File[] projectFiles ) throws Exception {
        List<HashMap<String, String>> infoList = new ArrayList<HashMap<String, String>>();
        for( File geopapDatabaseFile : projectFiles ) {
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + geopapDatabaseFile.getAbsolutePath())) {
                HashMap<String, String> projectInfo = getProjectMetadata(connection);
                infoList.add(projectInfo);
            }
        }
        return infoList;
    }

    private static LinkedHashMap<String, String> getProjectMetadata( Connection connection ) throws Exception {
        LinkedHashMap<String, String> infoMap = new LinkedHashMap<String, String>();
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            String sql = "select " + MetadataTableFields.COLUMN_KEY.getFieldName() + ", " + //
                    MetadataTableFields.COLUMN_VALUE.getFieldName() + " from " + TABLE_METADATA;

            ResultSet rs = statement.executeQuery(sql);
            while( rs.next() ) {
                String key = rs.getString(MetadataTableFields.COLUMN_KEY.getFieldName());
                String value = rs.getString(MetadataTableFields.COLUMN_VALUE.getFieldName());

                if (!key.endsWith("ts")) {
                    infoMap.put(key, value);
                } else {
                    try {
                        long ts = Long.parseLong(value);
                        String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
                        infoMap.put(key, dateTimeString);
                    } catch (Exception e) {
                        infoMap.put(key, value);
                    }
                }
            }

        }
        return infoMap;
    }

    public static String getProjectInfo( Connection connection ) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            String sql = "select " + MetadataTableFields.COLUMN_KEY.getFieldName() + ", " + //
                    MetadataTableFields.COLUMN_VALUE.getFieldName() + " from " + TABLE_METADATA;

            ResultSet rs = statement.executeQuery(sql);
            while( rs.next() ) {
                String key = rs.getString(MetadataTableFields.COLUMN_KEY.getFieldName());
                String value = rs.getString(MetadataTableFields.COLUMN_VALUE.getFieldName());

                if (!key.endsWith("ts")) {
                    sb.append("<b>").append(key).append(":</b> ").append(escapeHTML(value)).append("<br/>");
                } else {
                    try {
                        long ts = Long.parseLong(value);
                        String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
                        sb.append("<b>").append(key).append(":</b> ").append(dateTimeString).append("<br/>");
                    } catch (Exception e) {
                        sb.append("<b>").append(key).append(":</b> ").append(escapeHTML(value)).append("<br/>");
                    }
                }
            }

        }
        return sb.toString();
    }

    public static String escapeHTML( String s ) {
        StringBuilder out = new StringBuilder(Math.max(16, s.length()));
        for( int i = 0; i < s.length(); i++ ) {
            char c = s.charAt(i);
            if (c > 127 || c == '"' || c == '<' || c == '>' || c == '&') {
                out.append("&#");
                out.append((int) c);
                out.append(';');
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

}
