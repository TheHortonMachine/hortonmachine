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
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.*;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_NAME;
import static org.hortonmachine.gears.io.vectorreader.OmsVectorReader.OMSVECTORREADER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.FEATUREREADER;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.geotools.data.FileDataStore;
import org.geotools.data.FileDataStoreFinder;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.hortonmachine.gears.io.properties.OmsPropertiesFeatureReader;
import org.hortonmachine.gears.io.shapefile.OmsShapefileFeatureReader;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;

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

    @Description(OMSVECTORREADER_TABLE_DESCRIPTION)
    @In
    public String table = null;

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
    public void process() throws IOException {
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
        } else if (name.toLowerCase().endsWith(HMConstants.GPKG)) {
            GeoPackage geopkg = new GeoPackage(new File(file));
            try {
                geopkg.init();

                List<FeatureEntry> features = geopkg.features();
                if (features.size() == 1) {
                    FeatureEntry featureEntry = features.get(0);
                    outVector = extractFeatures(geopkg, featureEntry);
                } else {
                    if (table == null || table.length() == 0) {
                        throw new ModelsIllegalargumentException(
                                "The geopackage contains several tables, the table neame needs to be specified.", this);
                    }
                    for( FeatureEntry featureEntry : features ) {
                        String tableName = featureEntry.getTableName();
                        if (table.equalsIgnoreCase(tableName)) {
                            outVector = extractFeatures(geopkg, featureEntry);
                            break;
                        }
                    }
                }
            } finally {
                geopkg.close();
            }
        } else if (name.toLowerCase().endsWith("properties")) {
            outVector = OmsPropertiesFeatureReader.readPropertiesfile(vectorFile.getAbsolutePath());
        } else {
            throw new IOException("Format is currently not supported for file: " + name);
        }
    }

    private DefaultFeatureCollection extractFeatures( GeoPackage geopkg, FeatureEntry featureEntry ) throws IOException {
        SimpleFeatureReader reader = geopkg.reader(featureEntry, null, null);
        DefaultFeatureCollection fc = new DefaultFeatureCollection();
        while( reader.hasNext() ) {
            fc.add(reader.next());
        }
        return fc;
    }

    /**
     * Fast read access mode. 
     * 
     * @param path the vector file path.
     * @return the read {@link FeatureCollection}.
     * @throws IOException
     */
    public static SimpleFeatureCollection readVector( String path ) throws IOException {
        SimpleFeatureCollection fc = getFC(path);
        return fc;
    }

    private static SimpleFeatureCollection getFC( String path ) throws IOException {
        OmsVectorReader reader = new OmsVectorReader();
        reader.file = path;
        reader.process();
        SimpleFeatureCollection fc = reader.outVector;
        return fc;
    }

    public static ReferencedEnvelope readEnvelope( String filePath ) throws IOException {
        File shapeFile = new File(filePath);
        FileDataStore store = FileDataStoreFinder.getDataStore(shapeFile);
        SimpleFeatureSource featureSource = store.getFeatureSource();
        return featureSource.getBounds();
    }

    public static SimpleFeatureCollection readVector( String path, String table ) throws IOException {
        OmsVectorReader reader = new OmsVectorReader();
        reader.file = path;
        reader.table = table;
        reader.process();
        SimpleFeatureCollection fc = reader.outVector;
        return fc;
    }

}
