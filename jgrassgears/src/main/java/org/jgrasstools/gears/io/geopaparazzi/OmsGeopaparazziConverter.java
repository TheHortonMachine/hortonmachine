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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_TAGS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doBookmarks_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doLoglines_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doLogpoints_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doMedia_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_doNotes_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_inGeopaparazzi_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSGEOPAPARAZZICONVERTER_outData_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.geopaparazzi.forms.Utilities;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.exceptions.ModelsRuntimeException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.json.JSONArray;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;

@Description(OMSGEOPAPARAZZICONVERTER_DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OMSGEOPAPARAZZICONVERTER_TAGS)
@Label(OMSGEOPAPARAZZICONVERTER_LABEL)
@Name(OMSGEOPAPARAZZICONVERTER_NAME)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsGeopaparazziConverter extends JGTModel {

    @Description(OMSGEOPAPARAZZICONVERTER_inGeopaparazzi_DESCRIPTION)
    @UI(JGTConstants.FOLDERIN_UI_HINT)
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

    @Description(OMSGEOPAPARAZZICONVERTER_doBookmarks_DESCRIPTION)
    @In
    public boolean doBookmarks = true;

    @Description(OMSGEOPAPARAZZICONVERTER_outData_DESCRIPTION)
    @UI(JGTConstants.FOLDEROUT_UI_HINT)
    @In
    public String outData = null;

    private static final String TAG_KEY = "key";
    private static final String TAG_VALUE = "value";

    private final DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
    private static boolean hasDriver = false;

    static {
        try {
            // make sure sqlite driver are there
            Class.forName("org.sqlite.JDBC");
            hasDriver = true;
        } catch (Exception e) {
        }
    }

    @Execute
    public void process() throws IOException {
        checkNull(inGeopaparazzi);

        if (!hasDriver) {
            throw new ModelsIllegalargumentException("Can't find any sqlite driver. Check your settings.", this);
        }

        File geopapFolderFile = new File(inGeopaparazzi);
        File geopapDatabaseFile = new File(geopapFolderFile, "geopaparazzi.db");

        if (!geopapDatabaseFile.exists()) {
            throw new ModelsIllegalargumentException(
                    "The geopaparazzi database file (geopaparazzi.db) is missing. Check the inserted path.", this);
        }

        File outputFolderFile = new File(outData);

        Connection connection = null;
        try {
            // create a database connection
            connection = DriverManager.getConnection("jdbc:sqlite:" + geopapDatabaseFile.getAbsolutePath());
            if (geopapDatabaseFile.exists()) {
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
            }
            /*
             * import media as point shapefile, containing the path
             */
            mediaToShapeFile(geopapFolderFile, outputFolderFile, pm);

        } catch (Exception e) {
            throw new ModelsRuntimeException("An error occurred while importing from geopaparazzi: " + e.getLocalizedMessage(),
                    this);
        } finally {
            try {
                if (connection != null)
                    connection.close();
            } catch (SQLException e) {
                // connection close failed.
                throw new ModelsRuntimeException("An error occurred while closing the database connection.", this);
            }
        }

    }

    private void simpleNotesToShapefile( Connection connection, File outputFolderFile, IJGTProgressMonitor pm ) throws Exception {
        File outputShapeFile = new File(outputFolderFile, "simplenotes.shp");

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("geopaparazzisimplenotes"); //$NON-NLS-1$
        b.setCRS(crs);
        b.add("the_geom", Point.class); //$NON-NLS-1$
        b.add("DESCRIPTION", String.class);
        b.add("TIMESTAMP", String.class);
        b.add("ALTIM", Double.class);

        SimpleFeatureType featureType = b.buildFeatureType();
        pm.beginTask("Import notes...", -1);
        SimpleFeatureCollection newCollection = new DefaultFeatureCollection();

        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery("select lat, lon, altim, ts, text, form from notes");
            while( rs.next() ) {
                String form = rs.getString("form");
                if (form != null && form.trim().length() != 0) {
                    continue;
                }

                double lat = rs.getDouble("lat");
                double lon = rs.getDouble("lon");
                double altim = rs.getDouble("altim");
                String dateTimeString = rs.getString("ts");
                String text = rs.getString("text");

                if (lat == 0 || lon == 0) {
                    continue;
                }

                // and then create the features
                Coordinate c = new Coordinate(lon, lat);
                Point point = gf.createPoint(c);

                SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
                Object[] values = new Object[]{point, text, dateTimeString, String.valueOf(altim)};
                builder.addAll(values);
                SimpleFeature feature = builder.buildFeature(null);
                ((DefaultFeatureCollection) newCollection).add(feature);
            }

        } finally {
            pm.done();
            if (statement != null)
                statement.close();
        }
        OmsVectorWriter.writeVector(outputShapeFile.getAbsolutePath(), newCollection);
    }

    private void complexNotesToShapefile( Connection connection, File outputFolderFile, IJGTProgressMonitor pm ) throws Exception {
        pm.beginTask("Import complex notes...", -1);

        HashMap<String, BuilderAndCollectionPair> forms2PropertiesMap = new HashMap<String, BuilderAndCollectionPair>();

        Statement statement = null;
        try {
            statement = connection.createStatement();
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery("select lat, lon, altim, ts, text, form from notes");
            while( rs.next() ) {
                String formString = rs.getString("form");
                if (formString == null || formString.trim().length() == 0) {
                    continue;
                }

                double lat = rs.getDouble("lat");
                double lon = rs.getDouble("lon");
                double altim = rs.getDouble("altim");
                String dateTimeString = rs.getString("ts");
                // String text = rs.getString("text");

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

                LinkedHashMap<String, String> valuesMap = new LinkedHashMap<String, String>();
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

                        if (key != null && value != null)
                            valuesMap.put(key, value);
                    }
                }

                Set<Entry<String, String>> entrySet = valuesMap.entrySet();
                // check if there is a builder already
                BuilderAndCollectionPair builderAndCollectionPair = forms2PropertiesMap.get(sectionName);
                if (builderAndCollectionPair == null) {
                    SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
                    b.setName(sectionName); //$NON-NLS-1$
                    b.setCRS(crs);
                    b.add("the_geom", Point.class); //$NON-NLS-1$
                    b.add("ts", String.class); //$NON-NLS-1$
                    b.add("altim", String.class); //$NON-NLS-1$
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
                Object[] values = new Object[size + 3];
                values[0] = point;
                values[1] = dateTimeString;
                values[2] = "" + altim;
                int i = 3;
                for( Entry<String, String> entry : entrySet ) {
                    String value = entry.getValue();
                    if (value.toLowerCase().endsWith(".jpg") || value.toLowerCase().endsWith(".png")) {
                        int lastIndexOf = value.lastIndexOf("media");
                        value = value.substring(lastIndexOf);
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

                File outFile = new File(outputFolderFile, name + ".shp");
                OmsVectorWriter.writeVector(outFile.getAbsolutePath(), collection);
            }
        } finally {
            pm.done();
            if (statement != null)
                statement.close();
        }
    }

    private static class BuilderAndCollectionPair {
        SimpleFeatureBuilder builder;
        DefaultFeatureCollection collection;
    }

    private void gpsLogToShapefiles( Connection connection, File outputFolderFile, IJGTProgressMonitor pm ) throws Exception {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30); // set timeout to 30 sec.

        List<GpsLog> logsList = new ArrayList<GpsLog>();
        // first get the logs
        ResultSet rs = statement.executeQuery("select _id, startts, endts, text from gpslogs");
        while( rs.next() ) {
            long id = rs.getLong("_id");

            String startDateTimeString = rs.getString("startts");
            String endDateTimeString = rs.getString("endts");
            String text = rs.getString("text");

            GpsLog log = new GpsLog();
            log.id = id;
            log.startTime = startDateTimeString;
            log.endTime = endDateTimeString;
            log.text = text;
            logsList.add(log);
        }
        statement.close();

        try {
            // then the log data
            for( GpsLog log : logsList ) {
                long logId = log.id;
                String query = "select lat, lon, altim, ts from gpslog_data where logid = " + logId + " order by ts";

                Statement newStatement = connection.createStatement();
                newStatement.setQueryTimeout(30);
                ResultSet result = newStatement.executeQuery(query);

                while( result.next() ) {
                    double lat = result.getDouble("lat");
                    double lon = result.getDouble("lon");
                    double altim = result.getDouble("altim");
                    String dateTimeString = result.getString("ts");

                    GpsPoint gPoint = new GpsPoint();
                    gPoint.lon = lon;
                    gPoint.lat = lat;
                    gPoint.altim = altim;
                    gPoint.utctime = dateTimeString;
                    log.points.add(gPoint);

                }

                newStatement.close();

            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ModelsRuntimeException("An error occurred while reading the gps logs.", this);
        }

        /*
         * create the lines shapefile
         */
        SimpleFeatureTypeBuilder b;
        SimpleFeatureType featureType;

        if (doLoglines) {
            b = new SimpleFeatureTypeBuilder();
            b.setName("geopaparazzinotes");
            b.setCRS(crs);
            b.add("the_geom", MultiLineString.class);
            b.add("STARTDATE", String.class);
            b.add("ENDDATE", String.class);
            b.add("DESCR", String.class);
            featureType = b.buildFeatureType();
            pm.beginTask("Import gps to lines...", logsList.size());
            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
            for( GpsLog log : logsList ) {
                List<GpsPoint> points = log.points;

                List<Coordinate> coordList = new ArrayList<Coordinate>();
                String startDate = log.startTime;
                String endDate = log.endTime;
                for( GpsPoint gpsPoint : points ) {
                    Coordinate c = new Coordinate(gpsPoint.lon, gpsPoint.lat);
                    coordList.add(c);
                }
                Coordinate[] coordArray = (Coordinate[]) coordList.toArray(new Coordinate[coordList.size()]);
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
            File outputLinesShapeFile = new File(outputFolderFile, "gpslines.shp");
            OmsVectorWriter.writeVector(outputLinesShapeFile.getAbsolutePath(), newCollection);
        }

        if (doLogpoints) {
            /*
                 * create the points shapefile
                 */
            b = new SimpleFeatureTypeBuilder();
            b.setName("geopaparazzinotes");
            b.setCRS(crs);
            b.add("the_geom", Point.class);
            b.add("ALTIMETRY", String.class);
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
                    Object[] values = new Object[]{point, String.valueOf(gpsPoint.altim), gpsPoint.utctime};

                    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
                    builder.addAll(values);
                    SimpleFeature feature = builder.buildFeature(featureType.getTypeName() + "." + index++);
                    newCollection.add(feature);
                }
                pm.worked(1);
            }
            pm.done();
            File outputPointsShapeFile = new File(outputFolderFile, "gpspoints.shp");
            OmsVectorWriter.writeVector(outputPointsShapeFile.getAbsolutePath(), newCollection);
        }
    }

    private void mediaToShapeFile( File geopapFolderFile, File outputFolderFile, IJGTProgressMonitor pm ) throws Exception {
        File folder = new File(geopapFolderFile, "media");
        if (!folder.exists()) {
            // try to see if it is an old version of geopaparazzi
            folder = new File(geopapFolderFile, "pictures");
            if (!folder.exists()) {
                // ignoring non existing things
                return;
            }
        }

        // create destination folder
        String imageFolderName = "media";

        File[] listFiles = folder.listFiles();
        List<String> nonTakenFilesList = new ArrayList<String>();

        pm.beginTask("Importing media...", listFiles.length);
        try {

            /*
             * create the points shapefile
             */

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("geopaparazzinotes");
            b.setCRS(crs);
            b.add("the_geom", Point.class);
            b.add("ALTIMETRY", String.class);
            b.add("DATE", String.class);
            b.add("AZIMUTH", Double.class);
            b.add("IMAGE", String.class);
            SimpleFeatureType featureType = b.buildFeatureType();

            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
            for( File imageFile : listFiles ) {
                String name = imageFile.getName();
                if (name.endsWith("jpg") || imageFile.getName().endsWith("JPG") || imageFile.getName().endsWith("png")
                        || imageFile.getName().endsWith("PNG") || imageFile.getName().endsWith("3gp")) {

                    String[] nameSplit = name.split("[_//|.]"); //$NON-NLS-1$
                    String dateString = nameSplit[1];
                    String timeString = nameSplit[2];

                    Properties locationProperties = new Properties();
                    String mediaPath = imageFile.getAbsolutePath();
                    int lastDot = mediaPath.lastIndexOf("."); //$NON-NLS-1$
                    String nameNoExt = mediaPath.substring(0, lastDot);
                    String infoPath = nameNoExt + ".properties"; //$NON-NLS-1$
                    File infoFile = new File(infoPath);
                    if (!infoFile.exists()) {
                        nonTakenFilesList.add(mediaPath);
                        continue;
                    }
                    locationProperties.load(new FileInputStream(infoFile));
                    String azimuthString = locationProperties.getProperty("azimuth"); //$NON-NLS-1$
                    String latString = locationProperties.getProperty("latitude"); //$NON-NLS-1$
                    String lonString = locationProperties.getProperty("longitude"); //$NON-NLS-1$
                    String altimString = locationProperties.getProperty("altim"); //$NON-NLS-1$

                    Double azimuth = -9999.0;
                    if (azimuthString != null)
                        azimuth = Double.parseDouble(azimuthString);
                    double lat = 0.0;
                    double lon = 0.0;
                    if (latString.contains("/")) {
                        // this is an exif string
                        lat = exifFormat2degreeDecimal(latString);
                        lon = exifFormat2degreeDecimal(lonString);
                    } else {
                        lat = Double.parseDouble(latString);
                        lon = Double.parseDouble(lonString);
                    }
                    double altim = Double.parseDouble(altimString);

                    Coordinate c = new Coordinate(lon, lat);
                    Point point = gf.createPoint(c);

                    String imageRelativePath = imageFolderName + "/" + imageFile.getName();
                    File newImageFile = new File(outputFolderFile, imageRelativePath);
                    if (!newImageFile.getParentFile().exists()) {
                        newImageFile.getParentFile().mkdir();
                    }
                    FileUtilities.copyFile(imageFile, newImageFile);

                    String dateTime = dateString + timeString;
                    Object[] values = new Object[]{point, String.valueOf(altim), dateTime, azimuth, imageRelativePath};

                    SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
                    builder.addAll(values);
                    SimpleFeature feature = builder.buildFeature(null);
                    newCollection.add(feature);
                }
                pm.worked(1);
            }

            File outputPointsShapeFile = new File(outputFolderFile, "mediapoints.shp");
            OmsVectorWriter.writeVector(outputPointsShapeFile.getAbsolutePath(), newCollection);
        } finally {
            pm.done();
        }

        if (nonTakenFilesList.size() > 0) {
            final StringBuilder sB = new StringBuilder();
            sB.append("For the following media no *.properties file could be found:/n");
            for( String p : nonTakenFilesList ) {
                sB.append(p).append("/n");
            }
            pm.errorMessage(sB.toString());
        } else {
            pm.message("All media were successfully imported.");
        }

    }

    /**
     * Convert decimal degrees to exif format.
     * 
     * @param decimalDegree the angle in decimal format.
     * @return the exif format string.
     */
    @SuppressWarnings("nls")
    public static String degreeDecimal2ExifFormat( double decimalDegree ) {
        StringBuilder sb = new StringBuilder();
        sb.append((int) decimalDegree);
        sb.append("/1,");
        decimalDegree = (decimalDegree - (int) decimalDegree) * 60;
        sb.append((int) decimalDegree);
        sb.append("/1,");
        decimalDegree = (decimalDegree - (int) decimalDegree) * 60000;
        sb.append((int) decimalDegree);
        sb.append("/1000");
        return sb.toString();
    }

    /**
     * Convert exif format to decimal degree.
     * 
     * @param exifFormat the exif string of the gps position.
     * @return the decimal degree.
     */
    @SuppressWarnings("nls")
    public static double exifFormat2degreeDecimal( String exifFormat ) {
        // latitude=44/1,10/1,28110/1000
        String[] exifSplit = exifFormat.trim().split(",");

        String[] value = exifSplit[0].split("/");

        double tmp1 = Double.parseDouble(value[0]);
        double tmp2 = Double.parseDouble(value[1]);
        double degree = tmp1 / tmp2;

        value = exifSplit[1].split("/");
        tmp1 = Double.parseDouble(value[0]);
        tmp2 = Double.parseDouble(value[1]);
        double minutes = tmp1 / tmp2;

        value = exifSplit[2].split("/");
        tmp1 = Double.parseDouble(value[0]);
        tmp2 = Double.parseDouble(value[1]);
        double seconds = tmp1 / tmp2;

        double result = degree + (minutes / 60.0) + (seconds / 3600.0);
        return result;
    }
    private static class GpsPoint {
        public double lat;
        public double lon;
        public double altim;
        public String utctime;
    }

    private static class GpsLog {
        public long id;
        public String startTime;
        public String endTime;
        public String text;
        public List<GpsPoint> points = new ArrayList<GpsPoint>();

    }

}
