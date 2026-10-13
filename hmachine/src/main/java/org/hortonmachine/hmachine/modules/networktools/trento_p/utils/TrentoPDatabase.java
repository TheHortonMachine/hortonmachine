/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.hmachine.modules.networktools.trento_p.utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.AreasTrentoP;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.JunctionsTrentoP;
import org.hortonmachine.hmachine.modules.networktools.trento_p.utils.TrentoPFeatureType.PipesTrentoP;
import org.hortonmachine.dbs.compat.ADatabaseSyntaxHelper;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.hmachine.modules.networktools.trento_p.parameters.IParametersCode;

/**
 * The spatial database of a TrentoP network, a GeoPackage, H2GIS or PostGIS one: the
 * layers of the pipes, areas and junctions, and the tables of the parameters and of the commercial
 * diameters, with the names of {@link Constants}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TrentoPDatabase {

    /**
     * The prefix of the address of a PostGIS database.
     */
    public static final String POSTGIS_PREFIX = "jdbc:postgresql://";

    /**
     * An example series of commercial diameters, of PVC pipes: the diameter and the thickness, in cm.
     */
    public static final double[][] EXAMPLE_DIAMETERS = {//
            {14.0, 0.43}, {16.0, 0.50}, {20.0, 0.62}, {25.0, 0.77}, {31.5, 0.97}, {35.5, 1.09}, //
            {40.0, 1.23}, {45.0, 1.38}, {50.0, 1.53}, {63.0, 1.93}, {80.0, 2.45}, {100.0, 3.06}};

    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String DIAMETER = "diameter";
    private static final String THICKNESS = "thickness";

    /**
     * Open a database, creating it, if it is a file that does not exist.
     *
     * @param database the path of the database file, as .gpkg or .mv.db, or the address of
     *          a PostGIS database, as jdbc:postgresql://host:port/database.
     * @param user the optional user.
     * @param password the optional password.
     * @return the open database.
     * @throws Exception
     */
    public static ASpatialDb open( String database, String user, String password ) throws Exception {
        ASpatialDb db;
        String path;
        if (database.startsWith(POSTGIS_PREFIX)) {
            db = EDb.POSTGIS.getSpatialDb();
            path = database.substring(POSTGIS_PREFIX.length());
        } else {
            EDb type = EDb.fromFileNameDesktop(database);
            if (type != EDb.GEOPACKAGE && type != EDb.H2GIS) {
                throw new ModelsIllegalargumentException(
                        "The database " + database + " is not a supported spatial database: a GeoPackage, H2GIS or PostGIS one.",
                        "TrentoP");
            }
            db = type.getSpatialDb();
            path = database;
        }
        if (user != null) {
            db.setCredentials(user, password);
        }
        db.open(path);
        db.initSpatialMetadata(null);
        return db;
    }

    /**
     * Read a layer.
     *
     * @param db the database.
     * @param layer the name of the layer.
     * @return its features.
     * @throws Exception
     */
    public static SimpleFeatureCollection readLayer( ASpatialDb db, String layer ) throws Exception {
        checkTable(db, layer);
        DefaultFeatureCollection features = SpatialDbsImportUtils.tableToFeatureFCollection(db, SqlName.m(layer), -1, -1, null);
        return withTrentoPNames(features);
    }

    /**
     * Give the fields of the TrentoP names their spelling, as H2GIS and PostGIS change the case of
     * the names of the columns.
     */
    private static SimpleFeatureCollection withTrentoPNames( DefaultFeatureCollection features ) {
        SimpleFeatureType schema = features.getSchema();
        if (schema == null) {
            return features;
        }
        Map<String, String> names = new HashMap<>();
        List<ITrentoPType> types = new ArrayList<>();
        types.addAll(Arrays.asList(PipesTrentoP.values()));
        types.addAll(Arrays.asList(JunctionsTrentoP.values()));
        types.addAll(Arrays.asList(AreasTrentoP.values()));
        for( ITrentoPType type : types ) {
            names.put(type.getAttributeName().toLowerCase(), type.getAttributeName());
        }
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(schema.getName());
        builder.setCRS(schema.getCoordinateReferenceSystem());
        boolean renamed = false;
        for( AttributeDescriptor descriptor : schema.getAttributeDescriptors() ) {
            String name = descriptor.getLocalName();
            String trentoPName = names.getOrDefault(name.toLowerCase(), name);
            renamed = renamed || !trentoPName.equals(name);
            builder.add(trentoPName, descriptor.getType().getBinding());
        }
        if (!renamed) {
            return features;
        }
        SimpleFeatureType renamedType = builder.buildFeatureType();
        DefaultFeatureCollection renamedFeatures = new DefaultFeatureCollection();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(renamedType);
        for( SimpleFeature feature : FeatureUtilities.featureCollectionToList(features) ) {
            featureBuilder.addAll(feature.getAttributes());
            renamedFeatures.add(featureBuilder.buildFeature(feature.getID()));
        }
        return renamedFeatures;
    }

    /**
     * Write a layer, replacing it if it exists.
     *
     * @param db the database.
     * @param features the features to write.
     * @param layer the name of the layer.
     * @param pm the progress monitor.
     * @throws Exception
     */
    public static void writeLayer( ASpatialDb db, SimpleFeatureCollection features, String layer, IHMProgressMonitor pm )
            throws Exception {
        SqlName table = SqlName.m(layer);
        if (db.hasTable(table)) {
            dropLayer(db, table);
        }
        addCrs(db, features.getSchema().getCoordinateReferenceSystem());
        SpatialDbsImportUtils.createTableFromSchema(db, features.getSchema(), table, null, false);
        SpatialDbsImportUtils.importFeatureCollection(db, features, table, -1, false, pm);
        if (db instanceof GeopackageCommonDb) {
            ((GeopackageCommonDb) db).updateFeatureTableBounds(table);
        }
    }

    /**
     * Drop a layer with its geometry metadata.
     */
    private static void dropLayer( ASpatialDb db, SqlName table ) throws Exception {
        if (db instanceof GeopackageCommonDb) {
            ((GeopackageCommonDb) db).deleteGeoTable(table);
        } else {
            GeometryColumn geometryColumn = db.getGeometryColumnsForTable(table);
            String geometryName = geometryColumn != null ? geometryColumn.geometryColumnName : null;
            db.executeInsertUpdateDeleteSql(db.getType().getSqlTemplates().dropTable(table, geometryName));
        }
    }

    /**
     * Add a reference system to the database, where it has to be declared, as in a GeoPackage.
     */
    public static void addCrs( ASpatialDb db, CoordinateReferenceSystem crs ) throws Exception {
        if (db instanceof GeopackageCommonDb) {
            ((GeopackageCommonDb) db).addCRS("EPSG", HMCrsRegistry.getSrid(crs), crs.toWKT());
        }
    }

    /**
     * Read the parameters that have a value.
     *
     * @param db the database.
     * @return the values by parameter name.
     * @throws Exception
     */
    public static HashMap<String, Number> readParameters( ASpatialDb db ) throws Exception {
        checkTable(db, Constants.PARAMETERS_TABLE);
        HashMap<String, Number> parameters = new HashMap<>();
        QueryResult result = db.getTableRecordsMapFromRawSql(
                "select " + NAME + ", " + VALUE + " from " + Constants.PARAMETERS_TABLE + " where " + VALUE + " is not null", -1);
        for( Object[] row : result.data ) {
            parameters.put(row[0].toString().trim(), toNumber(row[1], row[0].toString()));
        }
        return parameters;
    }

    /**
     * Read the commercial diameters, from the smallest.
     *
     * @param db the database.
     * @return the list of diameter and thickness pairs, in cm.
     * @throws Exception
     */
    public static List<double[]> readDiameters( ASpatialDb db ) throws Exception {
        checkTable(db, Constants.DIAMETERS_TABLE);
        List<double[]> diameters = new ArrayList<>();
        QueryResult result = db.getTableRecordsMapFromRawSql("select " + DIAMETER + ", " + THICKNESS + " from "
                + Constants.DIAMETERS_TABLE + " where " + DIAMETER + " is not null order by " + DIAMETER, -1);
        for( Object[] row : result.data ) {
            double thickness = row[1] == null ? 0.0 : toNumber(row[1], THICKNESS).doubleValue();
            diameters.add(new double[]{toNumber(row[0], DIAMETER).doubleValue(), thickness});
        }
        if (diameters.isEmpty()) {
            throw new ModelsIllegalargumentException("The table " + Constants.DIAMETERS_TABLE + " has no diameters.", "TrentoP");
        }
        return diameters;
    }

    /**
     * Create the table of the parameters, if it is not there, and add the parameters it does not
     * have yet, with their default as value: the ones without a default are left without value.
     *
     * @param db the database.
     * @param codes the parameters to add.
     * @throws Exception
     */
    public static void addParameters( ASpatialDb db, List<IParametersCode> codes ) throws Exception {
        ADatabaseSyntaxHelper dsh = db.getType().getDatabaseSyntaxHelper();
        if (!db.hasTable(Constants.PARAMETERS_TABLE)) {
            db.executeInsertUpdateDeleteSql("create table " + Constants.PARAMETERS_TABLE + " (" + NAME + " " + dsh.TEXT() + " "
                    + dsh.PRIMARYKEY() + ", " + VALUE + " " + dsh.REAL() + ", unit " + dsh.TEXT() + ", default_value " + dsh.REAL()
                    + ", min_value " + dsh.REAL() + ", max_value " + dsh.REAL() + ")");
            registerAttributesTable(db, Constants.PARAMETERS_TABLE);
        }
        for( IParametersCode code : codes ) {
            QueryResult existing = db.getTableRecordsMapFromRawSql("select " + NAME + " from " + Constants.PARAMETERS_TABLE
                    + " where " + NAME + " = '" + code.getKey().replace("'", "''") + "'", -1);
            if (!existing.data.isEmpty()) {
                continue;
            }
            db.executeInsertUpdateDeletePreparedSql(
                    "insert into " + Constants.PARAMETERS_TABLE + " (" + NAME + ", " + VALUE
                            + ", unit, default_value, min_value, max_value) values (?, ?, ?, ?, ?, ?)",
                    new Object[]{code.getKey(), toDouble(code.getDefaultValue()), code.getUnit(),
                            toDouble(code.getDefaultValue()), toDouble(code.getMinRange()), toDouble(code.getMaxRange())});
        }
    }

    /**
     * Create the table of the commercial diameters, with the given ones, if it is not there.
     *
     * @param db the database.
     * @param diameters the diameter and thickness pairs, in cm.
     * @throws Exception
     */
    public static void addDiameters( ASpatialDb db, double[][] diameters ) throws Exception {
        if (db.hasTable(Constants.DIAMETERS_TABLE)) {
            return;
        }
        ADatabaseSyntaxHelper dsh = db.getType().getDatabaseSyntaxHelper();
        db.executeInsertUpdateDeleteSql("create table " + Constants.DIAMETERS_TABLE + " (id " + dsh.LONG_PRIMARYKEY_AUTOINCREMENT()
                + ", " + DIAMETER + " " + dsh.REAL() + ", " + THICKNESS + " " + dsh.REAL() + ")");
        registerAttributesTable(db, Constants.DIAMETERS_TABLE);
        for( double[] diameter : diameters ) {
            db.executeInsertUpdateDeletePreparedSql(
                    "insert into " + Constants.DIAMETERS_TABLE + " (" + DIAMETER + ", " + THICKNESS + ") values (?, ?)",
                    new Object[]{diameter[0], diameter[1]});
        }
    }

    /**
     * Register a table without geometries in the contents of a GeoPackage, so that it is seen by
     * the GIS, as an attribute table. The other databases list all their tables.
     */
    private static void registerAttributesTable( ASpatialDb db, String table ) throws Exception {
        if (db instanceof GeopackageCommonDb) {
            db.executeInsertUpdateDeletePreparedSql(
                    "insert into gpkg_contents (table_name, data_type, identifier) values (?, ?, ?)",
                    new Object[]{table, "attributes", table});
        }
    }

    private static void checkTable( ASpatialDb db, String table ) throws Exception {
        if (!db.hasTable(table)) {
            throw new ModelsIllegalargumentException("The database has no table " + table + ".", "TrentoP");
        }
    }

    private static Number toNumber( Object value, String name ) {
        if (value instanceof Number) {
            return (Number) value;
        }
        try {
            return Double.parseDouble(value.toString().trim());
        } catch (NumberFormatException e) {
            throw new ModelsIllegalargumentException("The value '" + value + "' of " + name + " is not a number.", "TrentoP");
        }
    }

    private static Double toDouble( Number value ) {
        return value == null ? null : value.doubleValue();
    }
}
