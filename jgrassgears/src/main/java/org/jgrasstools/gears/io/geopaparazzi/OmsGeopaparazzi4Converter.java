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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.Point;
import oms3.annotations.*;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.geopaparazzi.forms.Utilities;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoImages;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.Image;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TimeUtilities;
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

import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.Map.Entry;

import static org.jgrasstools.gears.i18n.GearsMessages.*;
import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.*;

@Description(OMSGEOPAPARAZZICONVERTER_DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OMSGEOPAPARAZZICONVERTER_TAGS)
@Label(JGTConstants.MOBILE)
@Name(OMSGEOPAPARAZZICONVERTER_NAME + "_v4")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsGeopaparazzi4Converter extends JGTModel {

    @Description("The geopaparazzi database file (*.gpap).")
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

    @Description(OMSGEOPAPARAZZICONVERTER_doBookmarks_DESCRIPTION)
    @In
    public boolean doBookmarks = true;

    @Description(OMSGEOPAPARAZZICONVERTER_outData_DESCRIPTION)
    @UI(JGTConstants.FOLDEROUT_UI_HINT)
    @In
    public String outFolder = null;

    public static final String EMPTY_STRING = " - ";

    public static final String MEDIA_FOLDER_NAME = "media";

    private static final String TAG_KEY = "key";
    private static final String TAG_VALUE = "value";
    private static final String TAG_TYPE = "type";

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

    private File mediaFolderFile;

    public static void main(String[] args) throws Exception {
        OmsGeopaparazzi4Converter conv = new OmsGeopaparazzi4Converter();
        conv.inGeopaparazzi = "/home/hydrologis/TMP/geopap/export4/geopaparazzi_test_4_conversion.gpap";
        conv.outFolder = "/home/hydrologis/TMP/geopap/export4/outfolder/";
        conv.process();

    }

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
        mediaFolderFile = new File(outputFolderFile, MEDIA_FOLDER_NAME);
        mediaFolderFile.mkdir();

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + geopapDatabaseFile.getAbsolutePath())) {
            projectInfo(connection, outputFolderFile, pm);

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
            //            gpsLogToShapefiles(connection, outputFolderFile, pm);
            /*
             * import media as point shapefile, containing the path
             */
            mediaToShapeFile(connection, mediaFolderFile, pm);
        }

    }

    private void projectInfo(Connection connection, File outputFolderFile, IJGTProgressMonitor pm) throws Exception {
        try (Statement statement = connection.createStatement()) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.

            String sql = "select " + MetadataTableFields.COLUMN_KEY.getFieldName() + ", " +//
                    MetadataTableFields.COLUMN_VALUE.getFieldName() + " from " + TABLE_METADATA;

            StringBuilder sb = new StringBuilder();
            sb.append("PROJECT INFO\n");
            sb.append("----------------------\n\n");

            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
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

    private void simpleNotesToShapefile(Connection connection, File outputFolderFile, IJGTProgressMonitor pm) throws Exception {
        File outputShapeFile = new File(outputFolderFile, "simplenotes.shp");

        String textFN = NotesTableFields.COLUMN_TEXT.getFieldName();
        String descFN = NotesTableFields.COLUMN_DESCRIPTION.getFieldName();
        String tsFN = NotesTableFields.COLUMN_TS.getFieldName();
        String altimFN = NotesTableFields.COLUMN_ALTIM.getFieldName();
        String dirtyFN = NotesTableFields.COLUMN_ISDIRTY.getFieldName();
        String formFN = NotesTableFields.COLUMN_FORM.getFieldName();
        String latFN = NotesTableFields.COLUMN_LAT.getFieldName();
        String lonFN = NotesTableFields.COLUMN_LON.getFieldName();

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("gpsimplenotes"); //$NON-NLS-1$
        b.setCRS(crs);
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


        pm.beginTask("Import simple notes...", -1);
        SimpleFeatureCollection newCollection = new DefaultFeatureCollection();

        try (Statement statement = connection.createStatement()) {

            statement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = statement.executeQuery(sql);
            while (rs.next()) {
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
                if (descr == null) descr = EMPTY_STRING;
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
        dumpVector(newCollection, outputShapeFile.getAbsolutePath());
    }

    private void complexNotesToShapefile(Connection connection, File outputFolderFile, IJGTProgressMonitor pm) throws Exception {
        pm.beginTask("Import complex notes...", -1);

        String tsFN = NotesTableFields.COLUMN_TS.getFieldName();
        String altimFN = NotesTableFields.COLUMN_ALTIM.getFieldName();
        String dirtyFN = NotesTableFields.COLUMN_ISDIRTY.getFieldName();
        String formFN = NotesTableFields.COLUMN_FORM.getFieldName();
        String latFN = NotesTableFields.COLUMN_LAT.getFieldName();
        String lonFN = NotesTableFields.COLUMN_LON.getFieldName();

        HashMap<String, BuilderAndCollectionPair> forms2PropertiesMap = new HashMap<String, BuilderAndCollectionPair>();

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
            while (rs.next()) {
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

                LinkedHashMap<String, String> valuesMap = new LinkedHashMap<String, String>();
                LinkedHashMap<String, String> typesMap = new LinkedHashMap<String, String>();
                for (String formName : formNames4Section) {
                    JSONObject form4Name = Utilities.getForm4Name(formName, sectionObject);
                    JSONArray formItems = Utilities.getFormItems(form4Name);

                    int length = formItems.length();
                    for (int i = 0; i < length; i++) {
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
                    b.setName(sectionName); //$NON-NLS-1$
                    b.setCRS(crs);
                    b.add("the_geom", Point.class); //$NON-NLS-1$
                    b.add(tsFN, String.class); //$NON-NLS-1$
                    b.add(altimFN, Double.class); //$NON-NLS-1$
                    b.add(dirtyFN, Integer.class); //$NON-NLS-1$
                    for (Entry<String, String> entry : entrySet) {
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
                for (Entry<String, String> entry : entrySet) {
                    String key = entry.getKey();

                    String value = entry.getValue();

                    String type = typesMap.get(key);
                    if (isMedia(type)) {
                        // extract images to media folder
                        String[] imageSplit = value.split(OmsGeopaparazziProject3To4Converter.IMAGE_ID_SEPARATOR);
                        StringBuilder sb = new StringBuilder();
                        for (String image : imageSplit) {
                            image = image.trim();
                            if (image.length() == 0) continue;
                            long imageId = Long.parseLong(image);
                            String imageName = DaoImages.getImageName(connection, imageId);
                            sb.append(OmsGeopaparazziProject3To4Converter.IMAGE_ID_SEPARATOR);
                            sb.append(MEDIA_FOLDER_NAME + "/" + imageName);
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
            for (Entry<String, BuilderAndCollectionPair> entry : entrySet) {
                String name = entry.getKey();
                SimpleFeatureCollection collection = entry.getValue().collection;

                File outFile = new File(outputFolderFile, name + ".shp");
                dumpVector(collection, outFile.getAbsolutePath());
            }
        } finally {
            pm.done();
        }
    }

    private boolean isMedia(String type) {
        return type.equals("pictures") || type.equals("map") || type.equals("sketch");
    }

    private static class BuilderAndCollectionPair {
        SimpleFeatureBuilder builder;
        DefaultFeatureCollection collection;
    }

    private void gpsLogToShapefiles(Connection connection, File outputFolderFile, IJGTProgressMonitor pm) throws Exception {
        Statement statement = connection.createStatement();
        statement.setQueryTimeout(30); // set timeout to 30 sec.

        List<GpsLog> logsList = new ArrayList<GpsLog>();
        // first get the logs
        ResultSet rs = statement.executeQuery("select _id, startts, endts, text from gpslogs");
        while (rs.next()) {
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
            for (GpsLog log : logsList) {
                long logId = log.id;
                String query = "select lat, lon, altim, ts from gpslog_data where logid = " + logId + " order by ts";

                Statement newStatement = connection.createStatement();
                newStatement.setQueryTimeout(30);
                ResultSet result = newStatement.executeQuery(query);

                while (result.next()) {
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
            for (GpsLog log : logsList) {
                List<GpsPoint> points = log.points;

                List<Coordinate> coordList = new ArrayList<Coordinate>();
                String startDate = log.startTime;
                String endDate = log.endTime;
                for (GpsPoint gpsPoint : points) {
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
            dumpVector(newCollection, outputLinesShapeFile.getAbsolutePath());
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
            for (GpsLog log : logsList) {
                List<GpsPoint> gpsPointList = log.points;
                for (GpsPoint gpsPoint : gpsPointList) {
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
            dumpVector(newCollection, outputPointsShapeFile.getAbsolutePath());
        }
    }

    private void mediaToShapeFile(Connection connection, File mediaFolderFile, IJGTProgressMonitor pm) throws Exception {
        try {

            /*
             * create the points shapefile
             */

            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName("geopaparazzinotes");
            b.setCRS(crs);
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

            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();


            List<Image> imagesList = DaoImages.getImagesList(connection);
            pm.beginTask("Importing media...", imagesList.size());

            for (Image image : imagesList) {
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

            File outputPointsShapeFile = new File(mediaFolderFile.getParentFile(), "mediapoints.shp");
            dumpVector(newCollection, outputPointsShapeFile.getAbsolutePath());
        } finally {
            pm.done();
        }


    }

    /**
     * Convert decimal degrees to exif format.
     *
     * @param decimalDegree the angle in decimal format.
     *
     * @return the exif format string.
     */
    @SuppressWarnings("nls")
    public static String degreeDecimal2ExifFormat(double decimalDegree) {
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
     *
     * @return the decimal degree.
     */
    @SuppressWarnings("nls")
    public static double exifFormat2degreeDecimal(String exifFormat) {
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
