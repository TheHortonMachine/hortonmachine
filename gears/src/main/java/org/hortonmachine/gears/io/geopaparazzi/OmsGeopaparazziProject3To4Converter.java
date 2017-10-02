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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import javax.imageio.ImageIO;

import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.geopaparazzi.forms.Utilities;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.DaoBookmarks;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.DaoGpsLog;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.DaoImages;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.DaoLog;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.DaoMetadata;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.DaoNotes;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.ETimeUtilities;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.json.JSONArray;
import org.json.JSONObject;

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

@Description(OmsGeopaparazziProject3To4Converter.CONVERT_A_GEOPAPARAZZI_3_FOLDER_PROJECT_INTO_A_GEOPAPARAZZI_4_DATABASE)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsGeopaparazziProject3To4Converter.OMSGEOPAPARAZZICONVERTER_TAGS)
@Label(HMConstants.MOBILE)
@Name("_" + OmsGeopaparazziProject3To4Converter.GEOPAP3TO4)
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsGeopaparazziProject3To4Converter extends HMModel {

    @Description(GEOPAPARAZZI_3_INPUT_FOLDER_TO_CONVERT)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inGeopaparazzi = null;

    // VARS DOCS START
    public static final String OMSGEOPAPARAZZICONVERTER_TAGS = "geopaparazzi, vector";
    public static final String CONVERT_A_GEOPAPARAZZI_3_FOLDER_PROJECT_INTO_A_GEOPAPARAZZI_4_DATABASE = "Convert a geopaparazzi 3 folder project into a geopaparazzi 4 database.";
    public static final String GEOPAPARAZZI_3_INPUT_FOLDER_TO_CONVERT = "Geopaparazzi 3 input folder to convert.";
    public static final String TABLE_GPSLOGS = "gpslogs";
    // VARS DOCS END

    public static final String GEOPAP3TO4 = "geopap3to4";
    public static final String TABLE_GPSLOG_DATA = "gpslog_data";
    public static final String TABLE_NOTES = "notes";

    public static final String LAT = "lat";
    public static final String ID = "_id";
    public static final String LON = "lon";
    public static final String ALTIM = "altim";
    public static final String TS = "ts";
    public static final String TEXT = "text";
    public static final String FORM = "form";
    public static final String JPG = "jpg";
    public static final String PNG = "png";
    public static final String AZIMUTH = "azimuth";
    public static final String LATITUDE = "latitude";
    public static final String LONGITUDE = "longitude";
    public static final String ENDTS = "endts";
    public static final String STARTTS = "startts";
    public static final String LOGID = "logid";

    public static final String FOLDER_MEDIA = "media";
    public static final String FOLDER_MEDIA_OLD = "pictures";

    private static final String TAG_KEY = "key";
    private static final String TAG_VALUE = "value";

    private final DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
    private static boolean hasDriver = false;

    public static final String IMAGE_ID_SEPARATOR = ";";

    private int imageCount = 0;
    private HashMap<String, Integer> imageName2IdMap = new HashMap<String, Integer>();
    private HashMap<String, double[]> imageName2DataMap = new HashMap<String, double[]>();
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
            // try version 3 name
            geopap3DbFile = new File(geopapFolderFile, "geopaparazzi3.db");
            if (!geopap3DbFile.exists()) {
                throw new ModelsIllegalargumentException(
                        "The geopaparazzi database file (geopaparazzi.db) is missing. Check the inserted path.", this, pm);
            }
        }

        String folderName = geopapFolderFile.getName();
        File geopap4DbFile = new File(geopapFolderFile.getParentFile(), folderName + ".gpap");
        if (geopap4DbFile.exists()) {
            throw new ModelsIllegalargumentException("The output database file already exists: "
                    + geopap4DbFile.getAbsolutePath(), this);
        }

        try (Connection geopap3Connection = DriverManager.getConnection("jdbc:sqlite:" + geopap3DbFile.getAbsolutePath());
                Connection geopap4Connection = DriverManager.getConnection("jdbc:sqlite:" + geopap4DbFile.getAbsolutePath())) {

            createBaseTables(geopap4Connection);

            importNotes(geopap3Connection, geopap4Connection, pm);
            importImages(geopapFolderFile, geopap4Connection, pm);
            importGpsLog(geopap3Connection, geopap4Connection, pm);

            // create some metadata info
            String notes = "This project has been migrated through HortonMachine from a Geopaparazzi < 4 version. The creation timestamp refers to the conversion instant. The name might contain the original creation timestamp.";
            DaoMetadata.fillProjectMetadata(geopap4Connection, folderName, null, notes,
                    "HortonMachine Geopaparazzi 3 to 4 Converter");

        } catch (Exception e) {
            throw new ModelsRuntimeException("An error occurred while importing from geopaparazzi: " + e.getLocalizedMessage(),
                    this);
        }

    }

    private void createBaseTables( Connection connection ) throws Exception {

        DaoNotes.createTables(connection);
        DaoImages.createTables(connection);
        DaoMetadata.createTables(connection);
        DaoBookmarks.createTables(connection);
        DaoGpsLog.createTables(connection);
        DaoLog.createTables(connection);

    }

    private void importNotes( Connection geopap3Connection, Connection geopap4Connection, IHMProgressMonitor pm )
            throws Exception {

        pm.beginTask("Import " + TABLE_NOTES + "...", -1);

        try (Statement readStatement = geopap3Connection.createStatement()) {
            readStatement.setQueryTimeout(30); // set timeout to 30 sec.

            ResultSet rs = readStatement.executeQuery("select " + ID + ", " + LAT + ", " + LON + ", " + ALTIM + ", " + TS + ", "
                    + TEXT + ", " + FORM + " from " + TABLE_NOTES);
            while( rs.next() ) {
                String form = rs.getString(FORM);

                long id = rs.getLong(ID);
                double lat = rs.getDouble(LAT);
                double lon = rs.getDouble(LON);
                double altim = rs.getDouble(ALTIM);
                String dateTimeString = rs.getString(TS);
                String text = rs.getString(TEXT);
                try {

                    if (lat == 0 || lon == 0) {
                        continue;
                    }

                    if (form != null && form.trim().length() != 0) {
                        // complex note, first extract images

                        JSONObject sectionObject = new JSONObject(form);
                        List<String> formNames4Section = Utilities.getFormNames4Section(sectionObject);

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

                                if (value != null) {
                                    if (isImage(value)) {
                                        // do images
                                        String[] imageSplit = value.split(IMAGE_ID_SEPARATOR);
                                        StringBuilder sb = new StringBuilder();
                                        for( String image : imageSplit ) {
                                            int lastSlash = image.lastIndexOf('/');
                                            image = image.substring(lastSlash + 1, image.length()).trim();

                                            imageName2IdMap.put(image, imageCount);
                                            sb.append(IMAGE_ID_SEPARATOR).append(imageCount);
                                            imageCount++;
                                            imageName2NoteIdMap.put(image, id);

                                            imageName2DataMap.put(image, new double[]{lon, lat});
                                        }

                                        value = sb.substring(1);
                                        jsonObject.put(TAG_VALUE, value);
                                    }
                                }
                            }
                        }

                        form = sectionObject.toString();
                    }

                    Date ts = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.parse(dateTimeString);

                    DaoNotes.addNote(geopap4Connection, id, lon, lat, altim, ts.getTime(), text, form);
                } catch (Exception e) {
                    System.err.println("Problems importing note: " + text + "\n" + form);
                    e.printStackTrace();
                }
            }

        } finally {
            pm.done();
        }
    }

    private boolean isImage( String value ) {
        String tmp = value.toLowerCase();
        return tmp.endsWith(PNG) || tmp.endsWith(JPG);
    }

    private void importGpsLog( Connection geopap3Connection, Connection geopap4Connection, IHMProgressMonitor pm )
            throws Exception {
        List<GpsLog> logsList;
        try (Statement readStatement = geopap3Connection.createStatement()) {
            readStatement.setQueryTimeout(30); // set timeout to 30 sec.

            logsList = new ArrayList<GpsLog>();
            // first get the logs
            ResultSet rs = readStatement.executeQuery("select " + ID + ", " + STARTTS + ", " + ENDTS + ", " + TEXT + " from "
                    + TABLE_GPSLOGS);
            while( rs.next() ) {
                long id = rs.getLong(ID);

                String startDateTimeString = rs.getString(STARTTS);
                String endDateTimeString = rs.getString(ENDTS);
                String text = rs.getString(TEXT);

                GpsLog log = new GpsLog();
                log.id = id;
                log.startTime = startDateTimeString;
                log.endTime = endDateTimeString;
                log.text = text;
                logsList.add(log);
            }
        }

        try {
            // then the log data
            for( GpsLog log : logsList ) {
                long logId = log.id;
                String query = "select " + ID + ", " + LAT + ", " + LON + ", " + ALTIM + ", " + TS + " from " + TABLE_GPSLOG_DATA
                        + " where " + LOGID + " = " + logId + " order by " + TS;

                try (Statement newStatement = geopap3Connection.createStatement()) {
                    newStatement.setQueryTimeout(30);
                    ResultSet result = newStatement.executeQuery(query);

                    while( result.next() ) {
                        long id = result.getLong(ID);
                        double lat = result.getDouble(LAT);
                        double lon = result.getDouble(LON);
                        double altim = result.getDouble(ALTIM);
                        String dateTimeString = result.getString(TS);

                        GpsPoint gPoint = new GpsPoint();
                        gPoint.id = id;
                        gPoint.lon = lon;
                        gPoint.lat = lat;
                        gPoint.altim = altim;
                        gPoint.utctime = dateTimeString;
                        log.points.add(gPoint);

                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new ModelsRuntimeException("An error occurred while reading the gps logs.", this);
        }

        pm.beginTask("Import logs...", logsList.size());
        for( GpsLog log : logsList ) {

            DaoGpsLog.addGpsLog(geopap4Connection, log, 8, "red", true);

            pm.worked(1);
        }
        pm.done();
    }

    private void importImages( File geopapFolderFile, Connection geopap4Connection, IHMProgressMonitor pm ) throws Exception {
        File folder = new File(geopapFolderFile, FOLDER_MEDIA);
        if (!folder.exists()) {
            // try to see if it is an old version of geopaparazzi
            folder = new File(geopapFolderFile, FOLDER_MEDIA_OLD);
            if (!folder.exists()) {
                // ignoring non existing things
                return;
            }
        }
        File[] listFiles = folder.listFiles();
        List<String> nonTakenFilesList = new ArrayList<String>();

        pm.message("Import media: ");
        for( File imageFile : listFiles ) {
            String name = imageFile.getName();
            String nameLC = name.toLowerCase();
            if (nameLC.endsWith(JPG) || nameLC.endsWith(PNG)) {
                try {
                    pm.message("\t" + name);
                    int lastDot = name.lastIndexOf('.');
                    if (lastDot == -1)
                        continue;
                    String ext = name.substring(lastDot + 1);

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

                    double lat = 0.0;
                    double lon = 0.0;
                    double altim = -1;
                    double azimuth = -9999.0;

                    Properties locationProperties = new Properties();
                    String mediaPath = imageFile.getAbsolutePath();
                    lastDot = mediaPath.lastIndexOf("."); //$NON-NLS-1$
                    if (lastDot == -1)
                        continue;
                    String nameNoExt = mediaPath.substring(0, lastDot);
                    String infoPath = nameNoExt + ".properties"; //$NON-NLS-1$
                    File infoFile = new File(infoPath);
                    if (!infoFile.exists()) {
                        double[] data = imageName2DataMap.get(name);
                        if (data == null) {
                            nonTakenFilesList.add(mediaPath);
                            continue;
                        }
                        lon = data[0];
                        lat = data[1];

                    } else {
                        locationProperties.load(new FileInputStream(infoFile));

                        String azimuthString = locationProperties.getProperty(AZIMUTH); //$NON-NLS-1$
                        String latString = locationProperties.getProperty(LATITUDE); //$NON-NLS-1$
                        String lonString = locationProperties.getProperty(LONGITUDE); //$NON-NLS-1$
                        String altimString = locationProperties.getProperty(ALTIM); //$NON-NLS-1$

                        if (azimuthString != null)
                            azimuth = Double.parseDouble(azimuthString);

                        if (latString.contains("/")) {
                            // this is an exif string
                            lat = exifFormat2degreeDecimal(latString);
                            lon = exifFormat2degreeDecimal(lonString);
                        } else {
                            lat = Double.parseDouble(latString);
                            lon = Double.parseDouble(lonString);
                        }
                        altim = Double.parseDouble(altimString);
                    }

                    Date timestamp = ETimeUtilities.INSTANCE.TIMESTAMPFORMATTER_LOCAL.parse(dateString + "_" + timeString);

                    /*
                     * image bytes read as they are on disk, so that they can be decoded in geopap
                     */
                    byte[] imageBytes = FileUtilities.readFileToBytes(imageFile.getAbsolutePath());

                    BufferedImage bufferedImage = ImageIO.read(imageFile);
                    // create thumb
                    // define sampling for thumbnail
                    int THUMBNAILWIDTH = 100;
                    float sampleSizeF = (float) bufferedImage.getWidth() / (float) THUMBNAILWIDTH;
                    int newHeight = (int) (bufferedImage.getHeight() / sampleSizeF);
                    BufferedImage resized = new BufferedImage(THUMBNAILWIDTH, newHeight, bufferedImage.getType());
                    Graphics2D g = resized.createGraphics();
                    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                    g.drawImage(bufferedImage, 0, 0, THUMBNAILWIDTH, newHeight, 0, 0, bufferedImage.getWidth(),
                            bufferedImage.getHeight(), null);
                    g.dispose();

                    Path tempPath = Files.createTempFile("jgt-", name);
                    File tempFile = tempPath.toFile();
                    ImageIO.write(resized, ext, tempFile);

                    byte[] thumbImageBytes = FileUtilities.readFileToBytes(tempFile.getAbsolutePath());

                    long noteId = -1;
                    Long noteIdObj = imageName2NoteIdMap.get(name);
                    if (noteIdObj != null) {
                        noteId = noteIdObj;
                    }

                    DaoImages.addImage(geopap4Connection, id, lon, lat, altim, azimuth, timestamp.getTime(), name, imageBytes,
                            thumbImageBytes, noteId);

                    Files.delete(tempPath);
                } catch (Exception e) {
                    System.err.println("Problems with image: " + imageFile);
                    e.printStackTrace();
                }
            }
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
     *
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
     *
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

}
