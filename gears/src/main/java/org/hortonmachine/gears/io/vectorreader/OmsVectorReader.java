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
package org.hortonmachine.gears.io.vectorreader;

import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_AUTHORCONTACTS;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_AUTHORNAMES;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_DESCRIPTION;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_DOCUMENTATION;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_KEYWORDS;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_LABEL;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_LICENSE;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_NAME;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.FEATUREREADER;

import java.io.File;
import java.io.IOException;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.gears.io.properties.OmsPropertiesFeatureReader;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSVECTORREADER_DESCRIPTION)
@Documentation(OMSVECTORREADER_DOCUMENTATION)
@Author(name = OMSVECTORREADER_AUTHORNAMES, contact = OMSVECTORREADER_AUTHORCONTACTS)
@Keywords(OMSVECTORREADER_KEYWORDS)
@Label(OMSVECTORREADER_LABEL)
@Name(OMSVECTORREADER_NAME)
@Status(OMSVECTORREADER_STATUS)
@License(OMSVECTORREADER_LICENSE)
public class OmsVectorReader extends HMModel {

    @Description(OMSVECTORREADER_P_TYPE_DESCRIPTION)
    @In
    // currently not used, for future compatibility
    public String pType = null;

    @Description(OMSVECTORREADER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String file = null;

    @Description(OMSVECTORREADER_OUT_VECTOR_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outVector = null;

    // PARAM NAMES START
    public static final String OMSVECTORREADER_DESCRIPTION = "Vectors features reader module.";
    public static final String OMSVECTORREADER_DOCUMENTATION = "OmsVectorReader.html";
    public static final String OMSVECTORREADER_KEYWORDS = "IO, Shapefile, Feature, Vector, Reading";
    public static final String OMSVECTORREADER_LABEL = FEATUREREADER;
    public static final String OMSVECTORREADER_NAME = "vectorreader";
    public static final int OMSVECTORREADER_STATUS = 40;
    public static final String OMSVECTORREADER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSVECTORREADER_AUTHORNAMES = "Andrea Antonello";
    public static final String OMSVECTORREADER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSVECTORREADER_P_TYPE_DESCRIPTION = "The vector type to read (Supported is: shp, properties).";
    public static final String OMSVECTORREADER_FILE_DESCRIPTION = "The vector file to read.";
    public static final String OMSVECTORREADER_TABLE_DESCRIPTION = "The table name (where applicable).";
    public static final String OMSVECTORREADER_OUT_VECTOR_DESCRIPTION = "The read feature collection.";
    // PARAM NAMES STOP

    @Execute
    public void process() throws Exception {
        if (!concatOr(outVector == null, doReset)) {
            return;
        }

        checkNull(file);

        File vectorFile = new File(file);
        String name = vectorFile.getName();
        if (name.toLowerCase().endsWith(HMConstants.SHP)) {
            OmsShapefileFeatureReader reader = new OmsShapefileFeatureReader();
            reader.file = vectorFile.getAbsolutePath();
            reader.pm = pm;
            reader.readFeatureCollection();
            outVector = reader.geodata;
            if (outVector.getSchema().getCoordinateReferenceSystem() == null) {
                pm.errorMessage("The coordinate reference system could not be defined for: " + reader.file);
            }
        } else if (name.toLowerCase().contains("." + HMConstants.GPKG)) {
            if (!name.contains(HMConstants.DB_TABLE_PATH_SEPARATOR)) {
                throw new ModelsIllegalargumentException(
                        "The table name needs to be specified in the geopackage path after the #.", this);
            }
            String[] split = file.split(HMConstants.DB_TABLE_PATH_SEPARATOR);
            if (split.length == 1 || split[1].trim().length() == 0) {
                throw new ModelsIllegalargumentException(
                        "The geopackage contains several tables, the table neame needs to be specified in the path after the #.",
                        this);
            }
            String table = split[1];
            String dbPath = split[0];
            try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
                db.open(dbPath);
                db.initSpatialMetadata(null);
                outVector = SpatialDbsImportUtils.tableToFeatureFCollection(db, table, -1, -1, null);
            }
        } else if (name.toLowerCase().endsWith("properties")) {
            outVector = OmsPropertiesFeatureReader.readPropertiesfile(vectorFile.getAbsolutePath());
        } else {
            throw new IOException("Format is currently not supported for file: " + name);
        }
    }

    /**
     * Fast read access mode. 
     * 
     * @param path the vector file path.
     * @return the read {@link FeatureCollection}.
     * @throws IOException
     */
    public static SimpleFeatureCollection readVector( String path ) throws Exception {
        SimpleFeatureCollection fc = getFC(path);
        return fc;
    }

    private static SimpleFeatureCollection getFC( String path ) throws Exception {
        OmsVectorReader reader = new OmsVectorReader();
        reader.file = path;
        reader.process();
        SimpleFeatureCollection fc = reader.outVector;
        return fc;
    }

    public static ReferencedEnvelope readEnvelope( String filePath ) throws Exception {
        File shapeFile = new File(filePath);
        FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        return featureSource.getBounds();
    }

}
