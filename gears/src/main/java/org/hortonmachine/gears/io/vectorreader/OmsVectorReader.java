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
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.filter.BinaryLogicOperator;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.Not;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.spatial.BBOX;
import org.geotools.api.filter.spatial.BinarySpatialOperator;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.collection.FilteringSimpleFeatureCollection;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.TableName;
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
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

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
    
    /**
     * An optional filter to apply to the read features, where applicable.
     */
    public Filter filter;

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
        	checkExistence(vectorFile);
            OmsShapefileFeatureReader reader = new OmsShapefileFeatureReader();
            reader.file = vectorFile.getAbsolutePath();
            reader.pm = pm;
            reader.readFeatureCollection();
            outVector = reader.geodata;
            if (outVector.getSchema().getCoordinateReferenceSystem() == null) {
                pm.errorMessage("The coordinate reference system could not be defined for: " + reader.file);
            }
        } else if (name.toLowerCase().contains("." + HMConstants.GPKG)) {
            SqlName spatialTable = null;
            String dbPath = null;
            if (!name.contains(HMConstants.DB_TABLE_PATH_SEPARATOR)) {
            	checkExistence(vectorFile);
                try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
                    db.open(vectorFile.getAbsolutePath());
                    db.initSpatialMetadata(null);
                    List<TableName> tables = db.getTables();
                    for( TableName t : tables ) {
                        GeometryColumn gc = db.getGeometryColumnsForTable(t.toSqlName());
                        if (gc != null) {
                            if (spatialTable != null) {
                                throw new ModelsIllegalargumentException(
                                        "The geopackage contains several tables, the table name needs to be specified in the path after the #.",
                                        this);
                            }
                            spatialTable = t.toSqlName();
                            dbPath = vectorFile.getAbsolutePath();
                        }
                    }
                    if (spatialTable == null) {
                        throw new ModelsIllegalargumentException(
                                "The table name needs to be specified in the geopackage path after the #.", this);
                    }
                }
            } else {
                String[] split = file.split(HMConstants.DB_TABLE_PATH_SEPARATOR);
                if (split.length == 1 || split[1].trim().length() == 0) {
                    throw new ModelsIllegalargumentException(
                            "The geopackage contains several tables, the table neame needs to be specified in the path after the #.",
                            this);
                }
                spatialTable = SqlName.m(split[1]);
                dbPath = split[0];
                checkExistence(new File(dbPath));
            }
            try (GeopackageCommonDb db = (GeopackageCommonDb) EDb.GEOPACKAGE.getSpatialDb()) {
                db.open(dbPath);
                db.initSpatialMetadata(null);
                if (filter != null) {
                    Envelope spatialEnvelope = extractSpatialEnvelope(filter);
                    if (spatialEnvelope != null) {
                        SimpleFeatureCollection prefiltered = SpatialDbsImportUtils.tableToFeatureFCollection(db, spatialTable, -1,
                                -1, spatialEnvelope, null);
                        outVector = new FilteringSimpleFeatureCollection(prefiltered, filter);
                    } else {
                        String whereStr = CQL.toCQL(filter);
                        outVector = SpatialDbsImportUtils.tableToFeatureFCollection(db, spatialTable, -1, -1, whereStr);
                    }
                } else {
                    outVector = SpatialDbsImportUtils.tableToFeatureFCollection(db, spatialTable, -1, -1, null);
                }
            }
        } else if (name.toLowerCase().endsWith("properties")) {
        	checkExistence(vectorFile);
            outVector = OmsPropertiesFeatureReader.readPropertiesfile(vectorFile.getAbsolutePath());
        } else if (name.toLowerCase().endsWith("geojson") || name.toLowerCase().endsWith("json")) {
        	checkExistence(vectorFile);
        	FeatureJSON fjson = new FeatureJSON();
        	try (Reader r = new FileReader(vectorFile)) {
        	    outVector = (SimpleFeatureCollection) fjson.readFeatureCollection(r);
        	}
        } else {
        	Map<String, Object> params = new HashMap<>();
        	// check geopackage with table name
        	String typeName = null;
        	if (name.toLowerCase().contains("." + HMConstants.GPKG) && name.toLowerCase().contains(HMConstants.DB_TABLE_PATH_SEPARATOR)) {
        		String[] split = file.split(HMConstants.DB_TABLE_PATH_SEPARATOR);
        		checkExistence(new File(split[0]));
				params.put("dbtype", "geopkg");
				params.put("database", split[0]);
				if (split.length > 1 && split[1].trim().length() != 0) {
					typeName = split[1];
				}
        	} else {
        		checkExistence(vectorFile);
        		params.put("url", vectorFile.toURI().toURL());
        	}
        	
        	DataStore store = DataStoreFinder.getDataStore(params);
        	if (store== null)
        		throw new IOException("Format is currently not supported for file: " + name);
        	SimpleFeatureSource fs = store.getFeatureSource(typeName!=null? typeName : store.getTypeNames()[0]);
        	if (filter != null) {
				outVector = fs.getFeatures(filter);
			} else {
				outVector = fs.getFeatures();
			}
        }
    }

	private void checkExistence(File vectorFile) {
		if (!vectorFile.exists()) {
			throw new ModelsIllegalargumentException("The input file does not exist: " + vectorFile, this);
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
        SimpleFeatureCollection fc = getFC(path, null);
        return fc;
    }
    
    public  static SimpleFeatureCollection readVector( String path, Filter filter ) throws Exception {
    	return getFC(path, filter);
    }

    private static SimpleFeatureCollection getFC( String path, Filter filter  ) throws Exception {
        OmsVectorReader reader = new OmsVectorReader();
        reader.file = path;
        reader.filter = filter;
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

    private static Envelope extractSpatialEnvelope( Filter filter ) {
        if (filter == null || filter == Filter.INCLUDE || filter == Filter.EXCLUDE) {
            return null;
        }
        if (filter instanceof BBOX bbox) {
            var bounds = bbox.getBounds();
            if (bounds != null) {
                return new Envelope(bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY());
            }
            return null;
        }
        if (filter instanceof BinarySpatialOperator spatialOperator) {
            Geometry geometry = extractLiteralGeometry(spatialOperator.getExpression1());
            if (geometry == null) {
                geometry = extractLiteralGeometry(spatialOperator.getExpression2());
            }
            if (geometry != null && !geometry.isEmpty()) {
                return geometry.getEnvelopeInternal();
            }
            return null;
        }
        if (filter instanceof BinaryLogicOperator logicOperator) {
            Envelope merged = null;
            for( Filter child : logicOperator.getChildren() ) {
                Envelope childEnvelope = extractSpatialEnvelope(child);
                if (childEnvelope != null) {
                    if (merged == null) {
                        merged = new Envelope(childEnvelope);
                    } else {
                        merged.expandToInclude(childEnvelope);
                    }
                }
            }
            return merged;
        }
        if (filter instanceof Not) {
            return null;
        }
        return null;
    }

    private static Geometry extractLiteralGeometry( Expression expression ) {
        if (!(expression instanceof Literal)) {
            return null;
        }
        Object value = expression.evaluate(null);
        if (value instanceof Geometry) {
            return (Geometry) value;
        }
        return expression.evaluate(null, Geometry.class);
    }

}
