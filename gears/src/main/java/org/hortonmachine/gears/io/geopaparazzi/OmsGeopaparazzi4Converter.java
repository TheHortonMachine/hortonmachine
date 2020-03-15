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
import static org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_NOTES;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.dbs.compat.IHMConnection;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.spatialite.hm.SqliteDb;
import org.hortonmachine.gears.io.geopaparazzi.forms.Utilities;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.DaoGpsLog;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.DaoGpsLog.GpsLog;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.DaoGpsLog.GpsPoint;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.DaoImages;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.ETimeUtilities;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.Image;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.ImageTableFields;
import org.hortonmachine.gears.io.geopaparazzi.geopap4.TableDescriptions.NotesTableFields;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.StringUtilities;
import org.hortonmachine.gears.utils.chart.Scatter;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.json.JSONObject;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.Point;

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
@Keywords(OmsGeopaparazzi4Converter.OMSGEOPAPARAZZICONVERTER_TAGS)
@Label(HMConstants.MOBILE)
@Name("_" + OmsGeopaparazzi4Converter.OMSGEOPAPARAZZICONVERTER_NAME + "_v4")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
public class OmsGeopaparazzi4Converter extends HMModel {

    @Description(THE_GEOPAPARAZZI_DATABASE_FILE)
    @UI(HMConstants.FILEIN_UI_HINT_GPAP)
    @In
    public String inGeopaparazzi = null;

    @Description(OMSGEOPAPARAZZICONVERTER_DO_NOTES_DESCRIPTION)
    @In
    public boolean doNotes = true;

    @Description(OMSGEOPAPARAZZICONVERTER_DO_LOG_LINES_DESCRIPTION)
    @In
    public boolean doLoglines = true;

    @Description(OMSGEOPAPARAZZICONVERTER_DO_LOG_POINTS_DESCRIPTION)
    @In
    public boolean doLogpoints = false;

    @Description(OMSGEOPAPARAZZICONVERTER_DO_MEDIA_DESCRIPTION)
    @In
    public boolean doMedia = true;

    @Description(OMSGEOPAPARAZZICONVERTER_OUT_DATA_DESCRIPTION)
    @UI(HMConstants.FOLDEROUT_UI_HINT)
    @In
    public String outFolder = null;

    // VARS DOCS START
    public static final String THE_GEOPAPARAZZI_DATABASE_FILE = "The geopaparazzi database file (*.gpap).";
    public static final String DESCRIPTION = "Converts a geopaparazzi 4 project database into shapefiles.";
    public static final String OMSGEOPAPARAZZICONVERTER_LABEL = HMConstants.VECTORPROCESSING;
    public static final String OMSGEOPAPARAZZICONVERTER_TAGS = "geopaparazzi, vector";
    public static final String OMSGEOPAPARAZZICONVERTER_NAME = "geopapconvert";
    public static final String OMSGEOPAPARAZZICONVERTER_OUT_DATA_DESCRIPTION = "The output folder";
    public static final String OMSGEOPAPARAZZICONVERTER_DO_BOOKMARKS_DESCRIPTION = "Flag to create bookmarks points";
    public static final String OMSGEOPAPARAZZICONVERTER_DO_MEDIA_DESCRIPTION = "Flag to create media points";
    public static final String OMSGEOPAPARAZZICONVERTER_DO_LOG_POINTS_DESCRIPTION = "Flag to create log points";
    public static final String OMSGEOPAPARAZZICONVERTER_DO_LOG_LINES_DESCRIPTION = "Flag to create log lines";
    public static final String OMSGEOPAPARAZZICONVERTER_DO_NOTES_DESCRIPTION = "Flag to create notes";
    public static final String OMSGEOPAPARAZZICONVERTER_IN_GEOPAPARAZZI_DESCRIPTION = "The geopaparazzi folder";
    // VARS DOCS END

    public static final String EMPTY_STRING = " - ";

    public static final String MEDIA_FOLDER_NAME = "media";
    public static final String CHARTS_FOLDER_NAME = "charts";

    private File chartsFolderFile;

    private static final GeometryFactory gf = GeometryUtilities.gf();
    private static final String idFN = NotesTableFields.COLUMN_ID.getFieldName();
    private static final String tsFN = NotesTableFields.COLUMN_TS.getFieldName();
    private static final String altimFN = NotesTableFields.COLUMN_ALTIM.getFieldName();
    private static final String dirtyFN = NotesTableFields.COLUMN_ISDIRTY.getFieldName();
    private static final String formFN = NotesTableFields.COLUMN_FORM.getFieldName();
    private static final String latFN = NotesTableFields.COLUMN_LAT.getFieldName();
    private static final String lonFN = NotesTableFields.COLUMN_LON.getFieldName();
    private static final String textFN = NotesTableFields.COLUMN_TEXT.getFieldName();
    private static final String descFN = NotesTableFields.COLUMN_DESCRIPTION.getFieldName();

    @Execute
    public void process() throws Exception {
        checkNull(inGeopaparazzi);

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

        try (SqliteDb db = new SqliteDb()) {
            db.open(geopapDatabaseFile.getAbsolutePath());

            boolean hasMetadata = db.hasTable(TableDescriptions.TABLE_METADATA);

            db.execOnConnection(connection -> {
                if (hasMetadata)
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

                return null;
            });

        }

    }

    private void projectInfo( IHMConnection connection, File outputFolderFile ) throws Exception {
        StringBuilder sb = new StringBuilder();
        sb.append("PROJECT INFO\n");
        sb.append("----------------------\n\n");

        LinkedHashMap<String, String> metadataMap = GeopaparazziUtilities.getProjectMetadata(connection);
        if (metadataMap == null || metadataMap.size() == 0) {
            return;
        }
        for( Entry<String, String> entry : metadataMap.entrySet() ) {
            sb.append(entry.getKey()).append(" = ").append(entry.getValue()).append("\n");
        }

        FileUtilities.writeFile(sb.toString(), new File(outputFolderFile, "project_info.txt"));
    }

    private void simpleNotesToShapefile( IHMConnection connection, File outputFolderFile, IHMProgressMonitor pm )
            throws Exception {
        File outputShapeFile = new File(outputFolderFile, "notes_simple.shp");

        SimpleFeatureCollection newCollection = simpleNotes2featurecollection(connection, pm);
        dumpVector(newCollection, outputShapeFile.getAbsolutePath());
    }

    private void complexNotesToShapefile( IHMConnection connection, File outputFolderFile, IHMProgressMonitor pm )
            throws Exception {
        HashMap<String, SimpleFeatureCollection> name2CollectionMap = complexNotes2featurecollections(connection, pm);

        pm.beginTask("Writing layers to shapefile...", name2CollectionMap.size());
        Set<Entry<String, SimpleFeatureCollection>> entrySet2 = name2CollectionMap.entrySet();
        for( Entry<String, SimpleFeatureCollection> entry : entrySet2 ) {
            String name = entry.getKey();
            int lastUnderscore = name.lastIndexOf('_');
            name = name.substring(0, lastUnderscore);

            SimpleFeatureCollection collection = entry.getValue();

            String fileName = "notes_" + name + ".shp";
            fileName = FileUtilities.getSafeFileName(fileName);
            File outFile = new File(outputFolderFile, fileName);
            if (outFile.exists()) {
                File[] listFiles = outputFolderFile.listFiles();
                List<String> fileNames = new ArrayList<>();
                for( File file : listFiles ) {
                    fileNames.add(FileUtilities.getNameWithoutExtention(file));
                }

                String shpName = FileUtilities.getNameWithoutExtention(outFile);
                String safeShpName = StringUtilities.checkSameName(fileNames, shpName);
                outFile = new File(outputFolderFile, safeShpName + ".shp");
            }
            dumpVector(collection, outFile.getAbsolutePath());
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * Convert the simple notes to a featurecollection.
     * 
     * @param connection the db connection.
     * @param pm the monitor.
     * @return the extracted collection.
     * @throws Exception
     */
    public static SimpleFeatureCollection simpleNotes2featurecollection( IHMConnection connection, IHMProgressMonitor pm )
            throws Exception {

        SimpleFeatureType featureType = GeopaparazziUtilities.getSimpleNotesfeatureType();

        String sql = "select " + //
                latFN + "," + //
                lonFN + "," + //
                altimFN + "," + //
                tsFN + "," + //
                textFN + "," + //
                descFN + "," + //
                dirtyFN + "," + //
                formFN + " from " + //
                TABLE_NOTES + " where " + formFN + " is null or " + formFN + " = ''";

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

        pm.beginTask("Processing notes...", -1);
        SimpleFeatureCollection newCollection = new DefaultFeatureCollection();

        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(sql);) {

            while( rs.next() ) {
                String form = rs.getString(formFN);
                if (form != null && form.trim().length() != 0) {
                    continue;
                }

                double lat = rs.getDouble(latFN);
                double lon = rs.getDouble(lonFN);
                double altim = rs.getDouble(altimFN);
                long ts = rs.getLong(tsFN);
                String dateTimeString = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
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

    /**
     * Convert the complex notes to a map of featurecollection.
     * 
     * @param connection the db connection.
     * @param pm the monitor.
     * @return the extracted collection as name-collection map..
     * @throws Exception
     */
    public static HashMap<String, SimpleFeatureCollection> complexNotes2featurecollections( IHMConnection connection,
            IHMProgressMonitor pm ) throws Exception {
        pm.beginTask("Import complex notes...", -1);

        HashMap<String, BuilderAndCollectionPair> forms2PropertiesMap = new HashMap<>();
        HashMap<String, SimpleFeatureCollection> name2CollectionMap = new HashMap<>();

        String sql = "select " + //
                idFN + "," + //
                latFN + "," + //
                lonFN + "," + //
                altimFN + "," + //
                tsFN + "," + //
                dirtyFN + "," + //
                formFN + " from " + //
                TABLE_NOTES + " where " + formFN + " is not null and " + formFN + " != ''";
        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(sql);) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            while( rs.next() ) {
                String idString = rs.getString(idFN);
                System.out.println(idString);
                String formString = rs.getString(formFN);
                if (formString == null || formString.trim().length() == 0) {
                    continue;
                }

                double lat = rs.getDouble(latFN);
                double lon = rs.getDouble(lonFN);
                double altim = rs.getDouble(altimFN);
                long ts = rs.getLong(tsFN);
                String dateTimeString = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
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
                GeopaparazziUtilities.extractValues(sectionObject, formNames4Section, valuesMap, typesMap);

                Set<Entry<String, String>> entrySet = valuesMap.entrySet();
                TreeMap<String, Integer> namesMap = new TreeMap<String, Integer>();
                // check if there is a builder already
                String uniqueSectionName = sectionName + "_" + entrySet.size();
                BuilderAndCollectionPair builderAndCollectionPair = getBuilderAndCollectionPair(pm, forms2PropertiesMap,
                        sectionName, entrySet, typesMap, namesMap, uniqueSectionName);

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
                    if (Utilities.isMediaType(type)) {
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
                try {
                    builderAndCollectionPair.builder.addAll(values);
                } catch (Exception e) {
                    e.printStackTrace();
                }
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

    private static BuilderAndCollectionPair getBuilderAndCollectionPair( IHMProgressMonitor pm,
            HashMap<String, BuilderAndCollectionPair> forms2PropertiesMap, String sectionName,
            Set<Entry<String, String>> entrySet, LinkedHashMap<String, String> typesMap, TreeMap<String, Integer> namesMap,
            String uniqueSectionName ) {
        BuilderAndCollectionPair builderAndCollectionPair = forms2PropertiesMap.get(uniqueSectionName);
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

                String typeStr = typesMap.get(key);

                key = key.replaceAll("\\s+", "_");
                if (key.length() > 10) {
                    pm.errorMessage("Need to trim key: " + key);
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

                Class< ? > clazz = String.class;
                if (Utilities.isStringType(typeStr)) {
                    clazz = String.class; // redundant just to show that one can check
                } else if (Utilities.isIntegerType(typeStr)) {
                    clazz = Integer.class;
                } else if (Utilities.isDoubleType(typeStr)) {
                    clazz = Double.class;
                }

                b.add(key, clazz);
            }
            SimpleFeatureType featureType = b.buildFeatureType();
            SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);

            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
            builderAndCollectionPair = new BuilderAndCollectionPair();
            builderAndCollectionPair.builder = builder;
            builderAndCollectionPair.collection = newCollection;

            forms2PropertiesMap.put(uniqueSectionName, builderAndCollectionPair);
        }
        return builderAndCollectionPair;
    }

    /**
     * Convert the comples notes identified by a name to a featurecollection.
     * 
     * @param noteName the name of the note to extract.
     * @param connection the db connection.
     * @param pm the monitor.
     * @return the extracted collection.
     * @throws Exception
     */
    public static SimpleFeatureCollection complexNote2featurecollection( String noteName, IHMConnection connection,
            IHMProgressMonitor pm ) throws Exception {
        pm.beginTask("Import complex notes...", -1);

        HashMap<String, BuilderAndCollectionPair> forms2PropertiesMap = new HashMap<>();

        String sql = "select " + //
                idFN + "," + //
                latFN + "," + //
                lonFN + "," + //
                altimFN + "," + //
                tsFN + "," + //
                dirtyFN + "," + //
                formFN + " from " + //
                TABLE_NOTES + " where " + textFN + "='" + noteName + "'";
        try (IHMStatement statement = connection.createStatement(); IHMResultSet rs = statement.executeQuery(sql);) {
            statement.setQueryTimeout(30); // set timeout to 30 sec.
            while( rs.next() ) {
                // String idString = rs.getString(idFN);
                // System.out.println(idString);
                String formString = rs.getString(formFN);
                if (formString == null || formString.trim().length() == 0) {
                    continue;
                }

                double lat = rs.getDouble(latFN);
                double lon = rs.getDouble(lonFN);
                double altim = rs.getDouble(altimFN);
                long ts = rs.getLong(tsFN);
                String dateTimeString = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));
                int isDirty = rs.getInt(dirtyFN);
                if (lat == 0 || lon == 0) {
                    continue;
                }

                // and then create the features
                Coordinate c = new Coordinate(lon, lat);
                Point point = gf.createPoint(c);

                JSONObject sectionObject = new JSONObject(formString);
                String sectionName = sectionObject.getString("sectionname");
                if (!sectionName.equals(noteName)) {
                    continue;
                }

                sectionName = sectionName.replaceAll("\\s+", "_");
                LinkedHashMap<String, String> valuesMap = new LinkedHashMap<>();
                LinkedHashMap<String, String> typesMap = new LinkedHashMap<>();
                List<String> formNames4Section = Utilities.getFormNames4Section(sectionObject);

                GeopaparazziUtilities.extractValues(sectionObject, formNames4Section, valuesMap, typesMap);

                Set<Entry<String, String>> entrySet = valuesMap.entrySet();
                TreeMap<String, Integer> namesMap = new TreeMap<String, Integer>();
                // check if there is a builder already
                String uniqueSectionName = sectionName + "_" + entrySet.size();
                BuilderAndCollectionPair builderAndCollectionPair = getBuilderAndCollectionPair(pm, forms2PropertiesMap,
                        sectionName, entrySet, typesMap, namesMap, uniqueSectionName);

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
                    if (Utilities.isMediaType(type)) {
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

                    if (Utilities.isStringType(type)) {
                        if (value.length() > 253) {
                            pm.errorMessage("Need to trim value: " + value);
                            value = value.substring(0, 252);
                        }
                        values[i] = value;
                    } else if (Utilities.isIntegerType(type)) {
                        int intValue = Integer.parseInt(value);
                        values[i] = intValue;
                    } else if (Utilities.isDoubleType(type)) {
                        double doubleValue = Double.parseDouble(value);
                        values[i] = doubleValue;
                    } else {
                        pm.errorMessage("Unsupported type: " + type);
                    }
                    i++;
                }
                try {
                    builderAndCollectionPair.builder.addAll(values);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                SimpleFeature feature = builderAndCollectionPair.builder.buildFeature(null);
                builderAndCollectionPair.collection.add(feature);
            }

            if (forms2PropertiesMap.size() > 0) {
                BuilderAndCollectionPair builderAndCollectionPair = forms2PropertiesMap.values().iterator().next();
                SimpleFeatureCollection collection = builderAndCollectionPair.collection;
                return collection;
            }
            return null;
        } finally {
            pm.done();
        }
    }

    /**
     * Get the list of gps logs.
     * 
     * @param connection the db connection.
     * @return the list of gps logs.
     * @throws Exception
     */
    public static List<GpsLog> getGpsLogsList( IHMConnection connection ) throws Exception {
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

    /**
     * Convert the logs to a featurecollection.
     * 
     * @param pm the monitor.
     * @param logsList the list of logs as gathered from {@link #getGpsLogsList(IHMConnection)}.
     * @return the extracted collection.
     * @throws Exception
     */
    public static DefaultFeatureCollection getLogLinesFeatureCollection( IHMProgressMonitor pm, List<GpsLog> logsList ) {
        GeometryFactory gf = GeometryUtilities.gf();
        SimpleFeatureType featureType = GeopaparazziUtilities.getGpsLogLinesFeatureType();
        pm.beginTask("Import gps to lines...", logsList.size());
        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        for( GpsLog log : logsList ) {
            List<GpsPoint> points = log.points;

            List<Coordinate> coordList = new ArrayList<>();
            String startDate = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(log.startTime));
            String endDate = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(log.endTime));
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

    private void gpsLogToShapefiles( IHMConnection connection, File outputFolderFile, IHMProgressMonitor pm ) throws Exception {
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

                    String ts = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(gpsPoint.utctime));
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
                scatterProfile.setShowLines(Arrays.asList(true));
                scatterProfile.setXLabel("progressive distance [m]");
                scatterProfile.setYLabel("elevation [m]");
                BufferedImage imageProfile = scatterProfile.getImage(1000, 800);
                ImageIO.write(imageProfile, "png", profileFile);

                Scatter scatterPlanim = new Scatter("Planimetry " + logName);
                scatterPlanim.addSeries("planimetry", xPlanim, yPlanim);
                scatterPlanim.setShowLines(Arrays.asList(false));
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

    private void mediaToShapeFile( IHMConnection connection, File mediaFolderFile, IHMProgressMonitor pm ) throws Exception {
        SimpleFeatureCollection newCollection = media2FeatureCollection(connection, mediaFolderFile, pm);
        File outputPointsShapeFile = new File(mediaFolderFile.getParentFile(), "mediapoints.shp");
        dumpVector(newCollection, outputPointsShapeFile.getAbsolutePath());

    }

    public static SimpleFeatureCollection media2FeatureCollection( IHMConnection connection, File mediaFolderFile,
            IHMProgressMonitor pm ) throws Exception, IOException, FileNotFoundException {
        DefaultFeatureCollection newCollection = new DefaultFeatureCollection();
        try {
            GeometryFactory gf = GeometryUtilities.gf();
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
                String dateTimeString = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));

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

    public static SimpleFeatureCollection media2IdBasedFeatureCollection( IHMConnection connection, IHMProgressMonitor pm )
            throws Exception, IOException, FileNotFoundException {
        try {

            GeometryFactory gf = GeometryUtilities.gf();

            /*
             * create the points fc
             */
            DefaultFeatureCollection newCollection = new DefaultFeatureCollection();

            SimpleFeatureType featureType = GeopaparazziUtilities.getMediaFeaturetype();

            List<Image> imagesList = DaoImages.getImagesList(connection);
            pm.beginTask("Importing media...", imagesList.size());

            for( Image image : imagesList ) {
                Point point = gf.createPoint(new Coordinate(image.getLon(), image.getLat()));
                long ts = image.getTs();
                String dateTimeString = ETimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(ts));

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

    public static void writeImageFromId( IHMConnection connection, long imageId, File newImageFile ) throws Exception {
        byte[] imageData = DaoImages.getImageData(connection, imageId);

        try (OutputStream outStream = new FileOutputStream(newImageFile)) {
            outStream.write(imageData);
        }
    }

}
