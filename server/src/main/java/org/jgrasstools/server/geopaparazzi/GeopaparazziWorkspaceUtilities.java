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
package org.jgrasstools.server.geopaparazzi;

import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_METADATA;
import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_NOTES;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.jgrasstools.dbs.compat.IJGTConnection;
import org.jgrasstools.dbs.compat.IJGTResultSet;
import org.jgrasstools.dbs.compat.IJGTStatement;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.MetadataTableFields;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.NotesTableFields;
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

    public static File[] getGeopaparazziFiles( final File geopaparazziFolder ) {
        File[] projectFiles = geopaparazziFolder.listFiles(new FilenameFilter(){
            @Override
            public boolean accept( File dir, String name ) {
                return name.endsWith(".gpap");
            }
        });
        Arrays.sort(projectFiles, Collections.reverseOrder());
        return projectFiles;
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

    public static String getProjectInfo( IJGTConnection connection ) throws Exception {
        StringBuilder sb = new StringBuilder();
        String sql = "select " + MetadataTableFields.COLUMN_KEY.getFieldName() + ", " + //
                MetadataTableFields.COLUMN_VALUE.getFieldName() + " from " + TABLE_METADATA;
        try (IJGTStatement statement = connection.createStatement(); IJGTResultSet rs = statement.executeQuery(sql);) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

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

    /**
     * @param connection
     * @return the list of [lon, lat, altim, dateTimeString, text, descr]
     * @throws Exception
     */
    public static List<String[]> getNotesText( Connection connection ) throws Exception {
        String textFN = NotesTableFields.COLUMN_TEXT.getFieldName();
        String descFN = NotesTableFields.COLUMN_DESCRIPTION.getFieldName();
        String tsFN = NotesTableFields.COLUMN_TS.getFieldName();
        String altimFN = NotesTableFields.COLUMN_ALTIM.getFieldName();
        String latFN = NotesTableFields.COLUMN_LAT.getFieldName();
        String lonFN = NotesTableFields.COLUMN_LON.getFieldName();

        String sql = "select " + //
                latFN + "," + //
                lonFN + "," + //
                altimFN + "," + //
                tsFN + "," + //
                textFN + "," + //
                descFN + " from " + //
                TABLE_NOTES;

        List<String[]> notesDescriptionList = new ArrayList<>();
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            ResultSet rs = statement.executeQuery(sql);
            while( rs.next() ) {
                double lat = rs.getDouble(latFN);
                double lon = rs.getDouble(lonFN);
                double altim = rs.getDouble(altimFN);
                long ts = rs.getLong(tsFN);
                String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
                String text = rs.getString(textFN);
                String descr = rs.getString(descFN);
                if (descr == null)
                    descr = "";

                if (lat == 0 || lon == 0) {
                    continue;
                }

                notesDescriptionList.add(new String[]{//
                        String.valueOf(lon), //
                        String.valueOf(lat), //
                        String.valueOf(altim), //
                        dateTimeString, //
                        text, //
                        descr//
                });

            }

        }
        return notesDescriptionList;
    }

    public static String escapeHTML( String s ) {
        if (s == null) {
            return "";
        }
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

    public static String loadProjectsList( File gpapProjectsFolder ) {
        try {
            File[] geopaparazziProjectFiles = GeopaparazziWorkspaceUtilities.getGeopaparazziFiles(gpapProjectsFolder);
            List<HashMap<String, String>> projectMetadataList = GeopaparazziWorkspaceUtilities
                    .readProjectMetadata(geopaparazziProjectFiles);

            StringBuilder sb = new StringBuilder();
            sb.append("{");
            sb.append("\"projects\": [");

            for( int i = 0; i < projectMetadataList.size(); i++ ) {
                HashMap<String, String> metadataMap = projectMetadataList.get(i);
                long fileSize = geopaparazziProjectFiles[i].length();
                if (i > 0)
                    sb.append(",");
                sb.append("{");
                sb.append("    \"id\": \"" + geopaparazziProjectFiles[i].getName() + "\",");
                sb.append("    \"title\": \"" + metadataMap.get("description") + "\",");
                sb.append("    \"date\": \"" + metadataMap.get("creationts") + "\",");
                sb.append("    \"author\": \"" + metadataMap.get("creationuser") + "\",");
                sb.append("    \"name\": \"" + metadataMap.get("name") + "\",");
                sb.append("    \"size\": \"" + fileSize + "\"");
                sb.append("}");
            }

            sb.append("]");
            sb.append("}");

            return sb.toString();
        } catch (Exception e) {
            return "An error occurred: " + e.getMessage();
        }
    }

}
