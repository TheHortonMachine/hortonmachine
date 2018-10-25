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
package org.hortonmachine.gears.io.geopaparazzi;

import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_GPSLOGS;
import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_IMAGES;
import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_METADATA;
import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_NOTES;

import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.spatialite.hm.SqliteDb;
import org.hortonmachine.gears.io.geopaparazzi.forms.Utilities;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.ETimeUtilities;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.ImageTableFields;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.MetadataTableFields;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.NotesTableFields;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;

/**
 * Geopaparazzi utils.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeopaparazziUtilities {

    public static final String PROJECT_NAME = "name";
    public static final String PROJECT_CREATION_USER = "creationuser";
    public static final String PROJECT_CREATION_TS = "creationts";
    public static final String PROJECT_DESCRIPTION = "description";
    public static final String GPAP_EXTENSION = "gpap";

    public static final String GPS_LOGS = "GPS logs";
    public static final String MEDIA_NOTES = "Media Notes";
    public static final String SIMPLE_NOTES = "Simple Notes";

    public static final String TAG_KEY = "key";
    public static final String TAG_VALUE = "value";
    public static final String TAG_TYPE = "type";

    public static final String NOTES_tsFN = NotesTableFields.COLUMN_TS.getFieldName();
    public static final String NOTES_altimFN = NotesTableFields.COLUMN_ALTIM.getFieldName();
    public static final String NOTES_dirtyFN = NotesTableFields.COLUMN_ISDIRTY.getFieldName();
    public static final String NOTES_textFN = NotesTableFields.COLUMN_TEXT.getFieldName();
    public static final String NOTES_descFN = NotesTableFields.COLUMN_DESCRIPTION.getFieldName();
    public static final String NOTES_formFN = NotesTableFields.COLUMN_FORM.getFieldName();

    public static final String GPSLOG_descrFN = "DESCR";
    public static final String GPSLOG_enddateFN = "ENDDATE";
    public static final String GPSLOG_startdateFN = "STARTDATE";

    public static final String IMAGES_altimFN = ImageTableFields.COLUMN_ALTIM.getFieldName();
    public static final String IMAGES_tsFN = ImageTableFields.COLUMN_TS.getFieldName();
    public static final String IMAGES_azimFN = ImageTableFields.COLUMN_AZIM.getFieldName();
    public static final String IMAGES_imageidFN = "imageid";

    public static List<HashMap<String, String>> readProjectMetadata( File[] projectFiles ) throws Exception {
        List<HashMap<String, String>> infoList = new ArrayList<HashMap<String, String>>();
        for( File geopapDatabaseFile : projectFiles ) {
            try (SqliteDb db = new SqliteDb()) {
                db.open(geopapDatabaseFile.getAbsolutePath());
                HashMap<String, String> projectInfo = db.execOnConnection(connection -> {
                    return getProjectMetadata(connection);
                });
                infoList.add(projectInfo);
            }
        }
        return infoList;
    }

    public static File[] getGeopaparazziFiles( final File geopaparazziFolder ) {
        File[] projectFiles = geopaparazziFolder.listFiles(new FilenameFilter(){
            @Override
            public boolean accept( File dir, String name ) {
                return name.endsWith(GPAP_EXTENSION);
            }
        });
        Arrays.sort(projectFiles, Collections.reverseOrder());
        return projectFiles;
    }

    /**
     * Get the map of metadata of the project.
     * 
     * @param connection the db connection. 
     * @return the map of metadata.
     * @throws SQLException
     */
    public static LinkedHashMap<String, String> getProjectMetadata( IHMConnection connection ) throws Exception {
        LinkedHashMap<String, String> metadataMap = new LinkedHashMap<>();
        try (IHMStatement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            String sql = "select " + MetadataTableFields.COLUMN_KEY.getFieldName() + ", " + //
                    MetadataTableFields.COLUMN_VALUE.getFieldName() + " from " + TABLE_METADATA;
            IHMResultSet rs = statement.executeQuery(sql);
            while( rs.next() ) {
                String key = rs.getString(MetadataTableFields.COLUMN_KEY.getFieldName());
                String value = rs.getString(MetadataTableFields.COLUMN_VALUE.getFieldName());
                if (!key.endsWith("ts")) {
                    metadataMap.put(key, value);
                } else {
                    try {
                        long ts = Long.parseLong(value);
                        String dateTimeString = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
                        metadataMap.put(key, dateTimeString);
                    } catch (Exception e) {
                        metadataMap.put(key, value);
                    }
                }
            }

        }
        return metadataMap;
    }

    /**
     * @return the list of potential layers.
     * @throws SQLException 
     */
    public static List<String> getLayerNamesList( IHMConnection connection ) throws Exception {
        String formFN = NotesTableFields.COLUMN_FORM.getFieldName();
        String textFN = NotesTableFields.COLUMN_TEXT.getFieldName();
        List<String> layerNames = new ArrayList<>();
        String sql = "select count(*) from " + TABLE_NOTES + " where " + formFN + " is null or " + formFN + " = ''";
        int count = countRows(connection, sql);
        if (count > 0)
            layerNames.add(SIMPLE_NOTES);

        sql = "select count(*) from " + TABLE_IMAGES;
        count = countRows(connection, sql);
        if (count > 0)
            layerNames.add(MEDIA_NOTES);

        sql = "select count(*) from " + TABLE_GPSLOGS;
        count = countRows(connection, sql);
        if (count > 0)
            layerNames.add(GPS_LOGS);

        sql = "select distinct " + textFN + " from " + TABLE_NOTES + " where " + formFN + " is not null and " + formFN + "<>''";
        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(sql);) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            while( rs.next() ) {
                String formName = rs.getString(1);
                layerNames.add(formName);
            }
        }

        return layerNames;
    }

    private static int countRows( IHMConnection connection, String sql ) throws Exception {
        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(sql);) {
            if (rs.next()) {
                int notesCount = rs.getInt(1);
                return notesCount;
            }
        }
        return 0;
    }

    public static String getProjectInfo( IHMConnection connection, boolean doHtml ) throws Exception {
        StringBuilder sb = new StringBuilder();
        String sql = "select " + MetadataTableFields.COLUMN_KEY.getFieldName() + ", " + //
                MetadataTableFields.COLUMN_VALUE.getFieldName() + " from " + TABLE_METADATA;
        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(sql);) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            while( rs.next() ) {
                String key = rs.getString(MetadataTableFields.COLUMN_KEY.getFieldName());
                String value = rs.getString(MetadataTableFields.COLUMN_VALUE.getFieldName());

                String openBold = "<b>";
                String closeBold = "</b>";
                String nl = "<br/>";
                if (!doHtml) {
                    openBold = "";
                    closeBold = "";
                    nl = "\n";
                }
                if (!key.endsWith("ts")) {
                    sb.append(openBold).append(key).append(":" + closeBold + " ").append(escapeHTML(value)).append(nl);
                } else {
                    try {
                        long ts = Long.parseLong(value);
                        String dateTimeString = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
                        sb.append(openBold).append(key).append(":" + closeBold + " ").append(dateTimeString).append(nl);
                    } catch (Exception e) {
                        sb.append(openBold).append(key).append(":" + closeBold + " ").append(escapeHTML(value)).append(nl);
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
                String dateTimeString = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
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
            File[] geopaparazziProjectFiles = GeopaparazziUtilities.getGeopaparazziFiles(gpapProjectsFolder);
            List<HashMap<String, String>> projectMetadataList = GeopaparazziUtilities
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
                sb.append("    \"title\": \"" + metadataMap.get(PROJECT_DESCRIPTION) + "\",");
                sb.append("    \"date\": \"" + metadataMap.get(PROJECT_CREATION_TS) + "\",");
                sb.append("    \"author\": \"" + metadataMap.get(PROJECT_CREATION_USER) + "\",");
                sb.append("    \"name\": \"" + metadataMap.get(PROJECT_NAME) + "\",");
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

    public static SimpleFeatureType getSimpleNotesfeatureType() {

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("gpsimplenotes"); //$NON-NLS-1$
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class); //$NON-NLS-1$
        b.add(NOTES_textFN, String.class);
        b.add(NOTES_descFN, String.class);
        b.add(NOTES_tsFN, String.class);
        b.add(NOTES_altimFN, Double.class);
        b.add(NOTES_dirtyFN, Integer.class);
        SimpleFeatureType featureType = b.buildFeatureType();
        return featureType;
    }

    public static SimpleFeatureType getGpsLogLinesFeatureType() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("geopaparazzilogs");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", MultiLineString.class);
        b.add(GPSLOG_startdateFN, String.class);
        b.add(GPSLOG_enddateFN, String.class);
        b.add(GPSLOG_descrFN, String.class);
        SimpleFeatureType featureType = b.buildFeatureType();
        return featureType;
    }

    public static SimpleFeatureType getMediaFeaturetype() {
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("geopaparazzimediapoints");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class);

        b.add(IMAGES_altimFN, String.class);
        b.add(IMAGES_tsFN, String.class);
        b.add(IMAGES_azimFN, Double.class);
        b.add(IMAGES_imageidFN, Long.class);
        SimpleFeatureType featureType = b.buildFeatureType();
        return featureType;
    }

    public static SimpleFeatureType getComplexNotefeatureType( String noteName, IHMConnection connection ) throws Exception {
        String sql = "select " + //
                NOTES_formFN + " from " + //
                TABLE_NOTES + " where " + NOTES_textFN + "='" + noteName + "'";
        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(sql);) {
            while( rs.next() ) {
                String formString = rs.getString(NOTES_formFN);
                if (formString == null || formString.trim().length() == 0) {
                    continue;
                }
                JSONObject sectionObject = new JSONObject(formString);
                String sectionName = sectionObject.getString("sectionname");
                if (!sectionName.equals(noteName)) {
                    continue;
                }

                sectionName = sectionName.replaceAll("\\s+", "_");
                LinkedHashMap<String, String> valuesMap = new LinkedHashMap<>();
                LinkedHashMap<String, String> typesMap = new LinkedHashMap<>();
                List<String> formNames4Section = Utilities.getFormNames4Section(sectionObject);

                extractValues(sectionObject, formNames4Section, valuesMap, typesMap);

                Set<Entry<String, String>> entrySet = valuesMap.entrySet();
                TreeMap<String, Integer> namesMap = new TreeMap<String, Integer>();

                SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
                b.setName(sectionName); // $NON-NLS-1$
                b.setCRS(DefaultGeographicCRS.WGS84);
                b.add("the_geom", Point.class); //$NON-NLS-1$
                b.add(NOTES_tsFN, String.class); // $NON-NLS-1$
                b.add(NOTES_altimFN, Double.class); // $NON-NLS-1$
                b.add(NOTES_dirtyFN, Integer.class); // $NON-NLS-1$
                for( Entry<String, String> entry : entrySet ) {
                    String key = entry.getKey();
                    key = key.replaceAll("\\s+", "_");
                    if (key.length() > 10) {
                        key = key.substring(0, 10);
                    }
                    Integer nCount = namesMap.get(key);
                    if (nCount == null) {
                        nCount = 1;
                        namesMap.put(key, 1);
                    } else {
                        nCount++;
                        namesMap.put(key, nCount);
                        if (nCount < 10) {
                            key = key.substring(0, key.length() - 1) + nCount;
                        } else {
                            key = key.substring(0, key.length() - 2) + nCount;
                        }
                    }
                    b.add(key, String.class);
                }
                SimpleFeatureType featureType = b.buildFeatureType();
                return featureType;
            }
            return null;
        }
    }

    public static void extractValues( JSONObject sectionObject, List<String> formNames4Section,
            LinkedHashMap<String, String> valuesMap, LinkedHashMap<String, String> typesMap ) {
        for( String formName : formNames4Section ) {
            JSONObject form4Name = Utilities.getForm4Name(formName, sectionObject);
            JSONArray formItems = Utilities.getFormItems(form4Name);

            int length = formItems.length();
            for( int i = 0; i < length; i++ ) {
                JSONObject jsonObject = formItems.getJSONObject(i);

                if (!jsonObject.has(TAG_KEY)) {
                    continue;
                }
                String key = jsonObject.getString(TAG_KEY).trim();

                String value = null;
                if (jsonObject.has(TAG_VALUE)) {
                    value = jsonObject.get(TAG_VALUE).toString().trim();
                }
                String type = null;
                if (jsonObject.has(TAG_TYPE)) {
                    type = jsonObject.getString(TAG_TYPE).trim();
                }

                if (value != null) {
                    valuesMap.put(key, value);
                    typesMap.put(key, type);
                }
            }
        }
    }

}
