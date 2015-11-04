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
package org.jgrasstools.gears.io.geopaparazzi;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_TAGS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doLoglines_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doLogpoints_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doMedia_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doNotes_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_outData_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_METADATA;
import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_NOTES;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import javax.imageio.ImageIO;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.geopaparazzi.forms.Utilities;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog.GpsLog;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog.GpsPoint;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoImages;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.Image;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.ImageTableFields;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.MetadataTableFields;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.NotesTableFields;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TimeUtilities;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.chart.Scatter;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OmsGeopaparazzi4Converter.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OMSGEOPAPARAZZICONVERTER_TAGS)
@Label(JGTConstants.MOBILE)
@Name("_" + OMSGEOPAPARAZZICONVERTER_NAME + "_v4")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsGeopaparazzi4Converter extends JGTModel {

    @Description(THE_GEOPAPARAZZI_DATABASE_FILE)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inGeopaparazzi = null;

    @Description(OMSGEOPAPARAZZICONVERTER_doNotes_DESCRIPTION)
    @In
    public boolean doNotes = true;

    @Description(OMSGEOPAPARAZZICONVERTER_doLoglines_DESCRIPTION)
    @In
    public boolean doLoglines = true;

    @Description(OMSGEOPAPARAZZICONVERTER_doLogpoints_DESCRIPTION)
    @In
    public boolean doLogpoints = false;

    @Description(OMSGEOPAPARAZZICONVERTER_doMedia_DESCRIPTION)
    @In
    public boolean doMedia = true;

    @Description(OMSGEOPAPARAZZICONVERTER_outData_DESCRIPTION)
    @UI(JGTConstants.FOLDEROUT_UI_HINT)
    @In
    public String outFolder = null;

    // VARS DOCS START
    public static final String THE_GEOPAPARAZZI_DATABASE_FILE = "The geopaparazzi database file (*.gpap).";
    public static final String DESCRIPTION = "Converts a geopaparazzi 4 project database into shapefiles.";
    // VARS DOCS END

    public static final String EMPTY_STRING = " - ";

    public static final String MEDIA_FOLDER_NAME = "media";
    public static final String CHARTS_FOLDER_NAME = "charts";

    private static final String TAG_KEY = "key";
    private static final String TAG_VALUE = "value";
    private static final String TAG_TYPE = "type";

    private static boolean hasDriver = false;

    static {
        try {
            // make sure sqlite driver are there
            Class.forName("org.sqlite.JDBC");
            hasDriver = true;
        } catch (Exception e) {
        }
    }

    private File chartsFolderFile;

    @Execute
    public void process() throws Exception {
        checkNull(inGeopaparazzi);

        if (!hasDriver) {
            throw new ModelsIllegalargumentException("Can't find any sqlite driver. Check your settings.", this, pm);
        }

        File geopapDatabaseFile = new File(inGeopaparazzi);
        if (!geopapDatabaseFile.exists()) {
            throw new ModelsIllegalargumentException(
                    "The geopaparazzi database file (*.gpap) is missing. Check the inserted path.", this, pm);
        }

        File outputFolderFile = new File(outFolder);
        if (!outputFolderFile.exists()) {
            outputFolderFile.mkdirs();
        }
        File mediaFolderFile = new File(outputFolderFile, MEDIA_FOLDER_NAME);
        mediaFolderFile.mkdir();
        chartsFolderFile = new File(outputFolderFile, CHARTS_FOLDER_NAME);
        chartsFolderFile.mkdir();

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + geopapDatabaseFile.getAbsolutePath())) {
            projectInfo(connection, outputFolderFile);

            /*
             * import notes as shapefile
             */
            if (doNotes) {
                simpleNotesToShapefile(connection, outputFolderFile, pm);
                complexNotesToShapefile(connection, outputFolderFile, pm);
            }
            /*
             * import gps logs as shapefiles, once as lines and once as points
             */
            gpsLogToShapefiles(connection, outputFolderFile, pm);
            /*
             * import media as point shapefile, containing the path
             */
            mediaToShapeFile(connection, mediaFolderFile, pm);
        }

    }

    private void projectInfo( Connection connection, File outputFolderFile ) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            String sql = "select " + MetadataTableFields.COLUMN_KEY.getFieldName() + ", " + //
                    MetadataTableFields.COLUMN_VALUE.getFieldName() + " from " + TABLE_METADATA;

            StringBuilder sb = new StringBuilder();
            sb.append("PROJECT INFO\n");
            sb.append("----------------------\n\n");

            ResultSet rs = statement.executeQuery(sql);
            while( rs.next() ) {
                String key = rs.getString(MetadataTableFields.COLUMN_KEY.getFieldName());
                String value = rs.getString(MetadataTableFields.COLUMN_VALUE.getFieldName());

                if (!key.endsWith("ts")) {
                    sb.append(key).append(" = ").append(value).append("\n");
                } else {
                    try {
                        long ts = Long.parseLong(value);
                        String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
                        sb.append(key).append(" = ").append(dateTimeString).append("\n");
                    } catch (Exception e) {
                        sb.append(key).append(" = ").append(value).append("\n");
                    }
                }
            }

            FileUtilities.writeFile(sb.toString(), new File(outputFolderFile, "project_info.txt"));
        }
    }

    private void simpleNotesToShapefile( Connection connection, File outputFolderFile, IJGTProgressMonitor pm ) throws Exception {
        File outputShapeFile = new File(outputFolderFile, "notes_simple.shp");

        SimpleFeatureCollection newCollection = simpleNotes2featurecollection(connection, pm);
        dumpVector(newCollection, outputShapeFile.getAbsolutePath());
    }

    private void complexNotesToShapefile( Connection connection, File outputFolderFile, IJGTProgressMonitor pm )
            throws Exception {
        HashMap<String, SimpleFeatureCollection> name2CollectionMap = complexNotes2featurecollections(connection, pm);

        pm.beginTask("Writing layers to shapefile...", name2CollectionMap.size());
        Set<Entry<String, SimpleFeatureCollection>> entrySet2 = name2CollectionMap.entrySet();
        for( Entry<String, SimpleFeatureCollection> entry : entrySet2 ) {
            String name = entry.getKey();
            SimpleFeatureCollection collection = entry.getValue();

            File outFile = new File(outputFolderFile, "notes_" + name + ".shp");
            dumpVector(collection, outFile.getAbsolutePath());
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * 
     * 
     * @param connection
     * @param pm
     * @return
     * @throws SQLException
     */
    public static SimpleFeatureCollection simpleNotes2featurecollection( Connection connection, IJGTProgressMonitor pm )
            throws SQLException {
        String textFN = NotesTableFields.COLUMN_TEXT.getFieldName();
        String descFN = NotesTableFields.COLUMN_DESCRIPTION.getFieldName();
        String tsFN = NotesTableFields.COLUMN_TS.getFieldName();
        String altimFN = NotesTableFields.COLUMN_ALTIM.getFieldName();
        String dirtyFN = NotesTableFields.COLUMN_ISDIRTY.getFieldName();
        String formFN = NotesTableFields.COLUMN_FORM.getFieldName();
        String latFN = NotesTableFields.COLUMN_LAT.getFieldName();
        String lonFN = NotesTableFields.COLUMN_LON.getFieldName();

        GeometryFactory gf = GeometryUtilities.gf();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("gpsimplenotes"); //$NON-NLS-1$
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", Point.class); //$NON-NLS-1$
        b.add(textFN, String.class);
        b.add(descFN, String.class);
        b.add(tsFN, String.class);
        b.add(altimFN, Double.class);
        b.add(dirtyFN, Integer.class);

        String sql = "select " + //
                latFN + "," + //
                lonFN + "," + //
                altimFN + "," + //
                tsFN + "," + //
                textFN + "," + //
                descFN + "," + //
                dirtyFN + "," + //
                formFN + " from " + //
                TABLE_NOTES;

        SimpleFeatureType featureType = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        pm.beginTask("Processing simple notes...", -1);
        SimpleFeatureCollection newCollection = new DefaultFeatureCollection();

        try (Statement statement = connection.createStatement()) {

            statement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery(sql);
            while( rs.next() ) {
                String form = rs.getString(formFN);
                if (form != null && form.trim().length() != 0) {
                    continue;
                }

                double lat = rs.getDouble(latFN);
                double lon = rs.getDouble(lonFN);
                double altim = rs.getDouble(altimFN);
                long ts = rs.getLong(tsFN);
                String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
                String text = rs.getString(textFN);
                String descr = rs.getString(descFN);
                if (descr == null)
                    descr = EMPTY_STRING;
                int isDirty = rs.getInt(dirtyFN);

                if (lat == 0 || lon == 0) {
                    continue;
                }

                // and then create the features
                Coordinate c = new Coordinate(lon, lat);
                Point point = gf.createPoint(c);

                Object[] values = new Object[]{point, text, descr, dateTimeString, altim, isDirty};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                ((DefaultFeatureCollection) newCollection).add(feature);
            }

        }
        return newCollection;
    }

    public static HashMap<String, SimpleFeatureCollection> complexNotes2featurecollections( Connection connection,
            IJGTProgressMonitor pm ) throws SQLException, Exception {
        pm.beginTask("Import complex notes...", -1);

        GeometryFactory gf = GeometryUtilities.gf();
        String tsFN = NotesTableFields.COLUMN_TS.getFieldName();
        String altimFN = NotesTableFields.COLUMN_ALTIM.getFieldName();
        String dirtyFN = NotesTableFields.COLUMN_ISDIRTY.getFieldName();
        String formFN = NotesTableFields.COLUMN_FORM.getFieldName();
        String latFN = NotesTableFields.COLUMN_LAT.getFieldName();
        String lonFN = NotesTableFields.COLUMN_LON.getFieldName();

        HashMap<String, BuilderAndCollectionPair> forms2PropertiesMap = new HashMap<>();
        HashMap<String, SimpleFeatureCollection> name2CollectionMap = new HashMap<>();

        String sql = "select " + //
                latFN + "," + //
                lonFN + "," + //
                altimFN + "," + //
                tsFN + "," + //
                dirtyFN + "," + //
                formFN + " from " + //
                TABLE_NOTES;
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery(sql);
            while( rs.next() ) {
                String formString = rs.getString(formFN);
                if (formString == null || formString.trim().length() == 0) {
                    continue;
                }

                double lat = rs.getDouble(latFN);
                double lon = rs.getDouble(lonFN);
                double altim = rs.getDouble(altimFN);
                long ts = rs.getLong(tsFN);
                String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
                int isDirty = rs.getInt(dirtyFN);
                if (lat == 0 || lon == 0) {
                    continue;
                }

                // and then create the features
                Coordinate c = new Coordinate(lon, lat);
                Point point = gf.createPoint(c);

                JSONObject sectionObject = new JSONObject(formString);
                String sectionName = sectionObject.getString("sectionname");
                sectionName = sectionName.replaceAll("\\s+", "_");
                List<String> formNames4Section = Utilities.getFormNames4Section(sectionObject);

                LinkedHashMap<String, String> valuesMap = new LinkedHashMap<>();
                LinkedHashMap<String, String> typesMap = new LinkedHashMap<>();
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
                            value = jsonObject.getString(TAG_VALUE).trim();
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

                Set<Entry<String, String>> entrySet = valuesMap.entrySet();
                // check if there is a builder already
                BuilderAndCollectionPair builderAndCollectionPair = forms2PropertiesMap.get(sectionName);
                if (builderAndCollectionPair == null) {
                    SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
                    b.setName(sectionName); // $NON-NLS-1$
                    b.setCRS(DefaultGeographicCRS.WGS84);
                    b.add("the_geom", Point.class); //$NON-NLS-1$
                    b.add(tsFN, String.class); // $NON-NLS-1$
                    b.add(altimFN, Double.class); // $NON-NLS-1$
                    b.add(dirtyFN, Integer.class); // $NON-NLS-1$
                    for( Entry<String, String> entry : entrySet ) {
                        String key = entry.getKey();
                        key = key.replaceAll("\\s+", "_");
                        if (key.length() > 10) {
                            pm.errorMessage("Need to trim key: " + key);
                            key = key.substring(0, 10);
                        }
                        b.add(key, String.class);
                    }
                    SimpleFeatureType featureType = b.buildFeatureType();
                    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

                    DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
                    builderAndCollectionPair = new BuilderAndCollectionPair();
                    builderAndCollectionPair.builder = builder;
                    builderAndCollectionPair.collection = newCollection;

                    forms2PropertiesMap.put(sectionName, builderAndCollectionPair);
                }

                int size = entrySet.size();
                Object[] values = new Object[size + 4];
                values[0] = point;
                values[1] = dateTimeString;
                values[2] = altim;
                values[3] = isDirty;
                int i = 4;
                for( Entry<String, String> entry : entrySet ) {
                    String key = entry.getKey();

                    String value = entry.getValue();

                    String type = typesMap.get(key);
                    if (isMedia(type)) {
                        // extract images to media folder
                        String[] imageSplit = value.split(OmsGeopaparazziProject3To4Converter.IMAGE_ID_SEPARATOR);
                        StringBuilder sb = new StringBuilder();
                        for( String image : imageSplit ) {
                            image = image.trim();
                            if (image.length() == 0)
                                continue;
                            long imageId = Long.parseLong(image);
                            String imageName = DaoImages.getImageName(connection, imageId);
                            sb.append(OmsGeopaparazziProject3To4Converter.IMAGE_ID_SEPARATOR);
                            sb.append(MEDIA_FOLDER_NAME + "/").append(imageName);
                        }
                        if (sb.length() > 0) {
                            value = sb.substring(1);
                        } else {
                            value = "";
                        }
                    }

                    if (value.length() > 253) {
                        pm.errorMessage("Need to trim value: " + value);
                        value = value.substring(0, 252);
                    }
                    values[i] = value;
                    i++;
                }
                builderAndCollectionPair.builder.addAll(values);
                SimpleFeature feature = builderAndCollectionPair.builder.buildFeature(null);
                builderAndCollectionPair.collection.add(feature);
            }

            Set<Entry<String, BuilderAndCollectionPair>> entrySet = forms2PropertiesMap.entrySet();
            for( Entry<String, BuilderAndCollectionPair> entry : entrySet ) {
                String name = entry.getKey();
                SimpleFeatureCollection collection = entry.getValue().collection;
                name2CollectionMap.put(name, collection);
            }
        } finally {
            pm.done();
        }
        return name2CollectionMap;
    }

    /**
     * Get the list of gps logs.
     * 
     * @param connection the db connection.
     * @return the list of gps logs.
     * @throws SQLException
     */
    public static List<GpsLog> getGpsLogsList( Connection connection ) throws SQLException {
        List<GpsLog> logsList = DaoGpsLog.getLogsList(connection);

        try {
            // then the log data
            for( GpsLog log : logsList ) {
                DaoGpsLog.collectDataForLog(connection, log);
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ModelsRuntimeException("An error occurred while reading the gps logs.",
                    OmsGeopaparazzi4Converter.class.getSimpleName());
        }
        return logsList;
    }

    public static DefaultFeatureCollection getLogLinesFeatureCollection( IJGTProgressMonitor pm, List<GpsLog> logsList ) {
        GeometryFactory gf = GeometryUtilities.gf();
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("geopaparazzilogs");
        b.setCRS(DefaultGeographicCRS.WGS84);
        b.add("the_geom", MultiLineString.class);
        b.add("STARTDATE", String.class);
        b.add("ENDDATE", String.class);
        b.add("DESCR", String.class);
        SimpleFeatureType featureType = b.buildFeatureType();
        pm.beginTask("Import gps to lines...", logsList.size());
        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        for( GpsLog log : logsList ) {
            List<GpsPoint> points = log.points;

            List<Coordinate> coordList = new ArrayList<>();
            String startDate = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(log.startTime));
            String endDate = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(log.endTime));
            for( GpsPoint gpsPoint : points ) {
                Coordinate c = new Coordinate(gpsPoint.lon, gpsPoint.lat);
                coordList.add(c);
            }
            Coordinate[] coordArray = coordList.toArray(new Coordinate[coordList.size()]);
            if (coordArray.length < 2) {
                continue;
            }
            LineString lineString = gf.createLineString(coordArray);
            MultiLineString multiLineString = gf.createMultiLineString(new LineString[]{lineString});

            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
            Object[] values = new Object[]{multiLineString, startDate, endDate, log.text};
            builder.addAll(values);
            SimpleFeature feature = builder.buildFeature(null);

            newCollection.add(feature);
            pm.worked(1);
        }
        pm.done();
        return newCollection;
    }

    public static boolean isMedia( String type ) {
        return type.equals("pictures") || type.equals("map") || type.equals("sketch");
    }

    /**
     * Extracts profile information from logs.
     * 
     * @param log the log to analyze.
     * @param size the number of points in the log (as off: int size = log.points.size(); )
     * @param xProfile the array of to put the progressive distance in.
     * @param yProfile the array of to put the elevation in.
     * @param xPlanim the array of to put the x coord in.
     * @param yPlanim the array of to put the y coord in.
     * @param timestampArray  the array of to put the times in.
     */
    public static void populateProfilesForSingleLog( GpsLog log, int size, double[] xProfile, double[] yProfile, double[] xPlanim,
            double[] yPlanim, long[] timestampArray ) {
        GeodeticCalculator gc = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
        double runningDistance = 0;
        for( int i = 0; i < size - 1; i++ ) {
            GpsPoint p1 = log.points.get(i);
            GpsPoint p2 = log.points.get(i + 1);
            double lon1 = p1.lon;
            double lat1 = p1.lat;
            double altim1 = p1.altim;
            long utc1 = p1.utctime;
            double lon2 = p2.lon;
            double lat2 = p2.lat;
            double altim2 = p2.altim;
            long utc2 = p2.utctime;

            gc.setStartingGeographicPoint(lon1, lat1);
            gc.setDestinationGeographicPoint(lon2, lat2);
            double distance = gc.getOrthodromicDistance();
            runningDistance += distance;

            if (i == 0) {
                xProfile[i] = 0.0;
                yProfile[i] = altim1;

                xPlanim[i] = lon1;
                yPlanim[i] = lat1;

                timestampArray[i] = utc1;
            }
            xProfile[i + 1] = runningDistance;
            yProfile[i + 1] = altim2;

            xPlanim[i + 1] = lon2;
            yPlanim[i + 1] = lat2;
            timestampArray[i + 1] = utc2;
        }
    }

    private static class BuilderAndCollectionPair {
        SimpleFeatureBuilder builder;
        DefaultFeatureCollection collection;
    }

    private void gpsLogToShapefiles( Connection connection, File outputFolderFile, IJGTProgressMonitor pm ) throws Exception {
        List<GpsLog> logsList = getGpsLogsList(connection);

        /*
         * create the lines shapefile
         */
        SimpleFeatureTypeBuilder b;
        SimpleFeatureType featureType;

        if (doLoglines) {
            DefaultFeatureCollection newCollection = getLogLinesFeatureCollection(pm, logsList);
            File outputLinesShapeFile = new File(outputFolderFile, "gpslines.shp");
            dumpVector(newCollection, outputLinesShapeFile.getAbsolutePath());
        }

        if (doLogpoints) {
            /*
             * create the points shapefile
             */
            b = new SimpleFeatureTypeBuilder();
            b.setName("geopaparazzinotes");
            b.setCRS(DefaultGeographicCRS.WGS84);
            b.add("the_geom", Point.class);
            b.add("ALTIMETRY", Double.class);
            b.add("DATE", String.class);
            featureType = b.buildFeatureType();
            pm.beginTask("Import gps to points...", logsList.size());
            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
            int index = 0;
            for( GpsLog log : logsList ) {
                List<GpsPoint> gpsPointList = log.points;
                for( GpsPoint gpsPoint : gpsPointList ) {
                    Coordinate c = new Coordinate(gpsPoint.lon, gpsPoint.lat);
                    Point point = gf.createPoint(c);

                    String ts = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(gpsPoint.utctime));
                    Object[] values = new Object[]{point, gpsPoint.altim, ts};

                    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
                    builder.addAll(values);
                    SimpleFeature feature = builder.buildFeature(featureType.getTypeName() + "." + index++);
                    newCollection.add(feature);
                }
                pm.worked(1);
            }
            pm.done();
            File outputPointsShapeFile = new File(outputFolderFile, "gpspoints.shp");
            dumpVector(newCollection, outputPointsShapeFile.getAbsolutePath());
        }

        if (doLoglines) {
            /*
             * create charts for logs
             */
            pm.beginTask("Create log charts...", logsList.size());
            for( GpsLog log : logsList ) {
                String logName = log.text;
                int size = log.points.size();
                pm.message("Processing log: " + logName + " with " + size + " points.");

                String fileName = FileUtilities.getSafeFileName(logName);

                File profileFile = new File(chartsFolderFile, fileName + "_profile.png");
                File planimetricFile = new File(chartsFolderFile, fileName + "_planimetric.png");
                File csvFile = new File(chartsFolderFile, fileName + ".csv");

                double[] xProfile = new double[size];
                double[] yProfile = new double[size];
                double[] xPlanim = new double[size];
                double[] yPlanim = new double[size];
                long[] timestampArray = new long[size];
                populateProfilesForSingleLog(log, size, xProfile, yProfile, xPlanim, yPlanim, timestampArray);

                Scatter scatterProfile = new Scatter("Profile " + logName);
                scatterProfile.addSeries("profile", xProfile, yProfile);
                scatterProfile.setShowLines(true);
                scatterProfile.setXLabel("progressive distance [m]");
                scatterProfile.setYLabel("elevation [m]");
                BufferedImage imageProfile = scatterProfile.getImage(1000, 800);
                ImageIO.write(imageProfile, "png", profileFile);

                Scatter scatterPlanim = new Scatter("Planimetry " + logName);
                scatterPlanim.addSeries("planimetry", xPlanim, yPlanim);
                scatterPlanim.setShowLines(false);
                scatterPlanim.setXLabel("longitude");
                scatterPlanim.setYLabel("latitude");
                BufferedImage imagePlanim = scatterPlanim.getImage(1000, 800);
                ImageIO.write(imagePlanim, "png", planimetricFile);

                StringBuilder csvBuilder = new StringBuilder();
                csvBuilder.append("#x,y,progressive,elevation,utctimestamp\n");
                for( int j = 0; j < timestampArray.length; j++ ) {
                    String line = String.valueOf(xPlanim[j]).replace(',', '.');
                    line = line + "," + String.valueOf(yPlanim[j]).replace(',', '.');
                    line = line + "," + String.valueOf(xProfile[j]).replace(',', '.');
                    line = line + "," + String.valueOf(yProfile[j]).replace(',', '.');
                    line = line + "," + timestampArray[j] + "\n";
                    csvBuilder.append(line);
                }
                FileUtilities.writeFile(csvBuilder.toString(), csvFile);

                pm.worked(1);
            }
            pm.done();
        }

    }

    private void mediaToShapeFile( Connection connection, File mediaFolderFile, IJGTProgressMonitor pm ) throws Exception {
        SimpleFeatureCollection newCollection = media2FeatureCollection(connection, mediaFolderFile, pm);
        File outputPointsShapeFile = new File(mediaFolderFile.getParentFile(), "mediapoints.shp");
        dumpVector(newCollection, outputPointsShapeFile.getAbsolutePath());

    }

    private SimpleFeatureCollection media2FeatureCollection( Connection connection, File mediaFolderFile, IJGTProgressMonitor pm )
            throws Exception, IOException, FileNotFoundException {
        DefaultFeatureCollection newCollection;
        try {

            /*
             * create the points shapefile
             */
            newCollection = new DefaultFeatureCollection();

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("geopaparazzimediapoints");
            b.setCRS(DefaultGeographicCRS.WGS84);
            b.add("the_geom", Point.class);
            String altimFN = ImageTableFields.COLUMN_ALTIM.getFieldName();
            String tsFN = ImageTableFields.COLUMN_TS.getFieldName();
            String azimFN = ImageTableFields.COLUMN_AZIM.getFieldName();
            String imageNameFN = ImageTableFields.COLUMN_TEXT.getFieldName();
            b.add(altimFN, String.class);
            b.add(tsFN, String.class);
            b.add(azimFN, Double.class);
            b.add(imageNameFN, String.class);
            SimpleFeatureType featureType = b.buildFeatureType();

            List<Image> imagesList = DaoImages.getImagesList(connection);
            pm.beginTask("Importing media...", imagesList.size());

            for( Image image : imagesList ) {
                File newImageFile = new File(mediaFolderFile, image.getName());

                byte[] imageData = DaoImages.getImageData(connection, image.getImageDataId());

                try (OutputStream outStream = new FileOutputStream(newImageFile)) {
                    outStream.write(imageData);
                }

                Point point = gf.createPoint(new Coordinate(image.getLon(), image.getLat()));
                long ts = image.getTs();
                String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));

                String imageRelativePath = mediaFolderFile.getName() + "/" + image.getName();
                Object[] values = new Object[]{point, image.getAltim(), dateTimeString, image.getAzim(), imageRelativePath};

                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                newCollection.add(feature);
                pm.worked(1);
            }

        } finally {
            pm.done();
        }
        return newCollection;
    }

    public static SimpleFeatureCollection media2IdBasedFeatureCollection( Connection connection, IJGTProgressMonitor pm )
            throws Exception, IOException, FileNotFoundException {
        try {

            GeometryFactory gf = GeometryUtilities.gf();

            /*
             * create the points fc
             */
            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("geopaparazzimediapoints");
            b.setCRS(DefaultGeographicCRS.WGS84);
            b.add("the_geom", Point.class);
            String altimFN = ImageTableFields.COLUMN_ALTIM.getFieldName();
            String tsFN = ImageTableFields.COLUMN_TS.getFieldName();
            String azimFN = ImageTableFields.COLUMN_AZIM.getFieldName();
            b.add(altimFN, String.class);
            b.add(tsFN, String.class);
            b.add(azimFN, Double.class);
            b.add("imageid", Long.class);
            SimpleFeatureType featureType = b.buildFeatureType();

            List<Image> imagesList = DaoImages.getImagesList(connection);
            pm.beginTask("Importing media...", imagesList.size());

            for( Image image : imagesList ) {
                Point point = gf.createPoint(new Coordinate(image.getLon(), image.getLat()));
                long ts = image.getTs();
                String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));

                Object[] values = new Object[]{point, image.getAltim(), dateTimeString, image.getAzim(), image.getImageDataId()};

                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                newCollection.add(feature);
                pm.worked(1);
            }
            return newCollection;

        } finally {
            pm.done();
        }
    }

    public static void writeImageFromId( Connection connection, long imageId, File newImageFile ) throws Exception {
        byte[] imageData = DaoImages.getImageData(connection, imageId);

        try (OutputStream outStream = new FileOutputStream(newImageFile)) {
            outStream.write(imageData);
        }
    }

}
