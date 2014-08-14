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
import oms3.annotations.Label;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jgrasstools.gears.io.geopaparazzi.forms.Utilities;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.*;
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

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.*;
import java.util.Date;
import java.util.List;

import static org.jgrasstools.gears.i18n.GearsMessages.*;

@Description("Convert a geopaparazzi 3 folder project into a geopaparazzi 4 database.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OMSGEOPAPARAZZICONVERTER_TAGS)
@Label(JGTConstants.MOBILE)
@Name("geopap3to4")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsGeopaparazziProject3To4Converter extends JGTModel {

    @Description("Geopaparazzi 3 input folder to convert.")
    @UI(JGTConstants.FOLDERIN_UI_HINT)
    @In
    public String inGeopaparazzi = null;

    private static final String TAG_KEY = "key";
    private static final String TAG_VALUE = "value";

    private final DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
    private static boolean hasDriver = false;


    private int imageCount = 0;
    private HashMap<String, Integer> imageName2IdMap = new HashMap<String, Integer>();
    private HashMap<String, Long> imageName2NoteIdMap = new HashMap<String, Long>();

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
            throw new ModelsIllegalargumentException("Can't find any sqlite driver. Check your settings.", this, pm);
        }

        File geopapFolderFile = new File(inGeopaparazzi);
        File geopap3DbFile = new File(geopapFolderFile, "geopaparazzi.db");

        if (!geopap3DbFile.exists()) {
            throw new ModelsIllegalargumentException(
                    "The geopaparazzi database file (geopaparazzi.db) is missing. Check the inserted path.", this, pm);
        }

        String folderName = geopapFolderFile.getName();
        File geopap4DbFile = new File(geopapFolderFile.getParentFile(), folderName + ".gpap");
        if (geopap4DbFile.exists()) {
            throw new ModelsIllegalargumentException("The output database file already exists: " + geopap4DbFile.getAbsolutePath(), this);
        }

        Connection geopap3Connection = null;
        Connection geopap4Connection = null;
        try {
            // create a database connection
            geopap3Connection = DriverManager.getConnection("jdbc:sqlite:" + geopap3DbFile.getAbsolutePath());
            geopap4Connection = DriverManager.getConnection("jdbc:sqlite:" + geopap4DbFile.getAbsolutePath());

            createBaseTables(geopap4Connection);

            importNotes(geopap3Connection, geopap4Connection, pm);
            importImages(geopapFolderFile, geopap4Connection, pm);
            importGpsLog(geopap3Connection, geopap4Connection, pm);

            // create some metadata info
            String notes = "This project has been migrated through JGrasstools from a Geopaparazzi < 4 version. The creation timestamp refers to the conversion instant. The name might contain the original creation timestamp.";
            DaoMetadata.fillProjectMetadata(geopap4Connection, folderName, null, notes, "JGrasstools Geopaparazzi 3 to 4 Converter");

        } catch (Exception e) {
            throw new ModelsRuntimeException("An error occurred while importing from geopaparazzi: " + e.getLocalizedMessage(),
                    this);
        } finally {
            try {
                if (geopap3Connection != null)
                    geopap3Connection.close();
            } catch (SQLException e) {
                // connection close failed.
                throw new ModelsRuntimeException("An error occurred while closing the database connection.", this);
            }
        }

    }

    private void createBaseTables(Connection connection) throws Exception {

        DaoNotes.createTables(connection);
        DaoImages.createTables(connection);
        DaoMetadata.createTables(connection);
        DaoBookmarks.createTables(connection);
        DaoGpsLog.createTables(connection);


    }

    private void importNotes(Connection geopap3Connection, Connection geopap4Connection, IJGTProgressMonitor pm) throws Exception {

        pm.beginTask("Import notes...", -1);

        Statement readStatement = null;
        try {
            readStatement = geopap3Connection.createStatement();
            readStatement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = readStatement.executeQuery("select _id, lat, lon, altim, ts, text, form from notes");
            while (rs.next()) {
                String form = rs.getString("form");

                long id = rs.getLong("_id");
                double lat = rs.getDouble("lat");
                double lon = rs.getDouble("lon");
                double altim = rs.getDouble("altim");
                String dateTimeString = rs.getString("ts");
                String text = rs.getString("text");

                if (lat == 0 || lon == 0) {
                    continue;
                }

                if (form != null && form.trim().length() != 0) {
                    // complex note, first extract images

                    JSONObject sectionObject = new JSONObject(form);
                    List<String> formNames4Section = Utilities.getFormNames4Section(sectionObject);

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

                            if (value != null) {
                                if (isImage(value)) {
                                    // do images
                                    String[] imageSplit = value.split(";");
                                    StringBuilder sb = new StringBuilder();
                                    for (String image : imageSplit) {
                                        int lastSlash = image.lastIndexOf('/');
                                        image = image.substring(lastSlash + 1, image.length());

                                        imageName2IdMap.put(image.trim(), imageCount);
                                        sb.append(",").append(imageCount);
                                        imageCount++;
                                        imageName2NoteIdMap.put(image.trim(), id);
                                    }

                                    value = sb.substring(1);
                                    jsonObject.put(TAG_VALUE, value);
                                }
                            }
                        }
                    }

                    form = sectionObject.toString();
                }

                Date ts = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.parse(dateTimeString);

                DaoNotes.addNote(geopap4Connection, id, lon, lat, altim, ts.getTime(), text, form);
            }

        } finally {
            pm.done();
            if (readStatement != null)
                readStatement.close();
        }
    }

    private boolean isImage(String value) {
        String tmp = value.toLowerCase();
        return tmp.endsWith("png") || tmp.endsWith("jpg");
    }


    private void importGpsLog(Connection geopap3Connection, Connection geopap4Connection, IJGTProgressMonitor pm) throws Exception {
        Statement readStatement = geopap3Connection.createStatement();
        readStatement.setQueryTimeout(30); // set timeout to 30 sec.

        List<GpsLog> logsList = new ArrayList<GpsLog>();
        // first get the logs
        ResultSet rs = readStatement.executeQuery("select _id, startts, endts, text from gpslogs");
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
        readStatement.close();

        try {
            // then the log data
            for (GpsLog log : logsList) {
                long logId = log.id;
                String query = "select _id, lat, lon, altim, ts from gpslog_data where logid = " + logId + " order by ts";

                Statement newStatement = geopap3Connection.createStatement();
                newStatement.setQueryTimeout(30);
                ResultSet result = newStatement.executeQuery(query);

                while (result.next()) {
                    long id = result.getLong("_id");
                    double lat = result.getDouble("lat");
                    double lon = result.getDouble("lon");
                    double altim = result.getDouble("altim");
                    String dateTimeString = result.getString("ts");

                    GpsPoint gPoint = new GpsPoint();
                    gPoint.id = id;
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

        pm.beginTask("Import logs...", logsList.size());
        for (GpsLog log : logsList) {

            DaoGpsLog.addGpsLog(geopap4Connection, log, 8, "red", true);

            pm.worked(1);
        }
        pm.done();
    }

    private void importImages(File geopapFolderFile, Connection geopap4Connection, IJGTProgressMonitor pm) throws Exception {
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
            for (File imageFile : listFiles) {
                String name = imageFile.getName();
                String nameLC = name.toLowerCase();
                if (nameLC.endsWith("jpg") || nameLC.endsWith("png")) {

                    int id = -1;
                    Integer idObj = imageName2IdMap.get(name);
                    if (idObj != null) {
                        id = idObj;
                    } else {
                        id = imageCount++;
                    }

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

                    Date timestamp = TimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.parse(dateString + "_" + timeString);

                    BufferedImage bufferedImage = ImageIO.read(imageFile);
                    WritableRaster raster = bufferedImage.getRaster();
                    DataBufferByte data = (DataBufferByte) raster.getDataBuffer();
                    byte[] imageBytes = data.getData();

                    // create thumb
                    // define sampling for thumbnail
                    int THUMBNAILWIDTH = 100;
                    float sampleSizeF = (float) bufferedImage.getWidth() / (float) THUMBNAILWIDTH;
                    int newHeight = (int) (bufferedImage.getHeight() / sampleSizeF);
                    BufferedImage resized = new BufferedImage(THUMBNAILWIDTH, newHeight, bufferedImage.getType());
                    Graphics2D g = resized.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(bufferedImage, 0, 0, THUMBNAILWIDTH, newHeight, 0, 0, bufferedImage.getWidth(), bufferedImage.getHeight(), null);
                    g.dispose();
                    WritableRaster thumbRaster = resized.getRaster();
                    DataBufferByte thumbData = (DataBufferByte) thumbRaster.getDataBuffer();
                    byte[] thumbImageBytes = thumbData.getData();

                    long noteId = -1;
                    Long noteIdObj = imageName2NoteIdMap.get(name);
                    if (noteIdObj != null) {
                        noteId = noteIdObj;
                    }

                    DaoImages.addImage(geopap4Connection, id, lon, lat, altim, azimuth, timestamp.getTime(), name, imageBytes, thumbImageBytes, noteId);

                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }

        if (nonTakenFilesList.size() > 0) {
            final StringBuilder sB = new StringBuilder();
            sB.append("For the following media no *.properties file could be found:/n");
            for (String p : nonTakenFilesList) {
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

    public static class GpsPoint {
        public long id;
        public double lat;
        public double lon;
        public double altim;
        public String utctime;
    }

    public static class GpsLog {
        public long id;
        public String startTime;
        public String endTime;
        public String text;
        public List<GpsPoint> points = new ArrayList<GpsPoint>();

    }

    public static void main(String[] args) throws Exception {
        OmsGeopaparazziProject3To4Converter conv = new OmsGeopaparazziProject3To4Converter();
        conv.inGeopaparazzi = "/home/hydrologis/TMP/geopap/geopaparazzi_test_4_conversion/";
        conv.process();

    }

}
