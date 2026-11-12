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
package org.hortonmachine.nww.gui;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.database.SqlTemplatesAndActions;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.TableName;
import org.hortonmachine.gears.spatialite.GTSpatialiteThreadsafeDb;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.ui.progress.ProgressUpdate;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.executor.ExecutorProgressGui;
import org.hortonmachine.nww.layers.defaults.vector.DbTiledFeatureLayer;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.locationtech.jts.geom.Envelope;

/**
 * Loads the spatial tables of the database connections saved in the database viewer as layers
 * of the viewer.
 *
 * <p>The user chooses a connection and, in a list that can be filtered by name, the tables to
 * load: whole, or only the features in the current map view. The tables are read in the
 * background and drawn as the other vector layers, with the style saved in the
 * {@link LocalStyleStore}, else with the next of the default colors.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class NwwDatabaseLoader {

    /** The spatial database types the viewer can read from a desktop. */
    private static final Set<EDb> SUPPORTED_TYPES = Set.of(EDb.POSTGIS, EDb.H2GIS, EDb.GEOPACKAGE, EDb.SPATIALITE);

    /** The PostGIS schema whose tables are shown without the schema. */
    private static final String DEFAULT_PG_SCHEMA = "public";

    /** The points added on each side of the view extent, to reproject it to the table CRS. */
    private static final int ENVELOPE_DENSIFICATION_POINTS = 10;

    /**
     * A spatial table.
     *
     * @param sqlName the name to query the table with.
     * @param label the name shown, with the schema if not the default one.
     * @param type the geometry type.
     */
    private record DbTable( SqlName sqlName, String label, String type ) {
    }

    /**
     * The features read from a table.
     */
    private record ReadTable( DbTable table, SimpleFeatureCollection features ) {
    }

    private final NwwPanel wwjPanel;
    private final NwwLayerLoader layerLoader;

    /**
     * @param wwjPanel the map, giving the extent of the view.
     * @param layerLoader the loader adding the layers.
     */
    public NwwDatabaseLoader( NwwPanel wwjPanel, NwwLayerLoader layerLoader ) {
        this.wwjPanel = wwjPanel;
        this.layerLoader = layerLoader;
    }

    /**
     * @return the connections saved in the database viewer that the viewer can read, by label.
     * @throws Exception if the saved connections can't be read.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, ConnectionData> getSavedConnections() throws Exception {
        byte[] saved = PreferencesHandler.getPreference(SqlTemplatesAndActions.HM_SAVED_DATABASES, new byte[0]);
        Map<String, ConnectionData> connections = new LinkedHashMap<>();
        if (saved.length == 0) {
            return connections;
        }
        List<ConnectionData> list = (List<ConnectionData>) SqlTemplatesAndActions.convertFromBytes(saved);
        list.stream()//
                .filter(c -> c.connectionLabel != null && isSupported(c))//
                .sorted((c1, c2) -> c1.connectionLabel.compareToIgnoreCase(c2.connectionLabel))//
                .forEach(c -> connections.putIfAbsent(c.connectionLabel, c));
        return connections;
    }

    private static boolean isSupported( ConnectionData connection ) {
        try {
            return SUPPORTED_TYPES.contains(EDb.forCode(connection.dbType));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Let the user choose a saved connection and its tables, and load them as layers.
     *
     * <p>To be called from the event dispatch thread.</p>
     */
    public void openSavedConnection() {
        Map<String, ConnectionData> connections;
        try {
            connections = getSavedConnections();
        } catch (Exception e) {
            e.printStackTrace();
            GuiUtilities.showErrorMessage(wwjPanel, "Unable to read the saved connections: " + e.getMessage());
            return;
        }
        if (connections.isEmpty()) {
            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "There are no saved spatial database connections.\n"
                    + "Connections are saved in the database viewer, from the context menu of the connect button.");
            return;
        }
        String selected = GuiUtilities.showComboDialog(wwjPanel, "Saved connection",
                "Select the connection to load the layers from", connections.keySet().toArray(new String[0]), null);
        if (selected == null || selected.isEmpty()) {
            return;
        }
        ConnectionData connection = connections.get(selected);
        if (connection == null) {
            return;
        }

        new ExecutorProgressGui(1){
            private List<DbTable> tables;

            @Override
            public void backGroundWork() throws Exception {
                publish(new ProgressUpdate("Connecting to " + connection.connectionLabel + " ...", 0));
                try (ASpatialDb db = openDb(connection)) {
                    tables = listSpatialTables(db);
                }
            }

            @Override
            public void done() {
                super.done();
                if (tables != null) {
                    chooseAndLoadTables(connection, tables);
                }
            }
        }.execute();
    }

    /**
     * Let the user select the tables to load, and the extent, and read them in the background.
     */
    private void chooseAndLoadTables( ConnectionData connection, List<DbTable> tables ) {
        if (tables.isEmpty()) {
            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "The database " + connection.connectionLabel
                    + " contains no spatial table.");
            return;
        }
        List<String> names = tables.stream().map(DbTable::label).toList();
        List<String> types = tables.stream().map(DbTable::type).toList();
        TablesSelectionDialog.Result result = TablesSelectionDialog.show(wwjPanel, "Database layers",
                "Select the tables to load from " + connection.connectionLabel + ":", names, types, false,
                "Load only the features in the current map view",
                "If not selected, all the features of the tables are loaded; the tables with more than "
                        + NwwLayerLoader.STREAMING_FEATURES_THRESHOLD
                        + " features are read only where the map is drawn, as image tiles",
                false);
        if (result == null || !result.hasSelection()) {
            return;
        }
        ReferencedEnvelope viewEnvelope = null;
        if (result.optionChecked()) {
            viewEnvelope = wwjPanel.getViewportBounds();
            if (viewEnvelope == null) {
                GuiUtilities.showWarningMessage(wwjPanel, "WARNING",
                        "The extent of the map view can't be computed, as when the sky is visible.\n"
                                + "Tilt the view down, or load the whole tables.");
                return;
            }
        }
        List<DbTable> selectedTables = new ArrayList<>();
        for( int i = 0; i < tables.size(); i++ ) {
            if (result.selected()[i]) {
                selectedTables.add(tables.get(i));
            }
        }

        Envelope _viewEnvelope = viewEnvelope;
        new ExecutorProgressGui(selectedTables.size()){
            private final List<ReadTable> readTables = new ArrayList<>();
            /** The tables too big to be loaded in memory, read only where the map is drawn. */
            private final List<DbTable> tiledTables = new ArrayList<>();
            private final List<String> problems = new ArrayList<>();

            @Override
            public void backGroundWork() throws Exception {
                try (ASpatialDb db = openDb(connection)) {
                    for( int i = 0; i < selectedTables.size(); i++ ) {
                        DbTable table = selectedTables.get(i);
                        publish(new ProgressUpdate(
                                "Reading " + table.label() + " (" + (i + 1) + "/" + selectedTables.size() + ") ...", i));
                        try {
                            if (_viewEnvelope == null && DbTiledFeatureLayer.hasMoreFeaturesThan(db, table.sqlName(),
                                    NwwLayerLoader.STREAMING_FEATURES_THRESHOLD)) {
                                tiledTables.add(table);
                                continue;
                            }
                            SimpleFeatureCollection features = readTable(db, table, _viewEnvelope);
                            if (features == null) {
                                problems.add(table.label() + ": no features" + (_viewEnvelope != null ? " in the map view" : ""));
                            } else {
                                readTables.add(new ReadTable(table, features));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            problems.add(table.label() + ": " + e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void done() {
                super.done();
                for( ReadTable readTable : readTables ) {
                    LayerSource source = LayerSource.ofDatabase(connection, readTable.table().label(), null, _viewEnvelope);
                    try {
                        // the style saved locally, if any: the styles are not saved in the database
                        Style style = LocalStyleStore.read(source);
                        if (layerLoader.loadFeatureCollection(source.path(), readTable.features(), source, style) == null) {
                            problems.add(readTable.table().label() + ": unsupported geometry type");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        problems.add(readTable.table().label() + ": " + e.getMessage());
                    }
                }
                for( DbTable table : tiledTables ) {
                    LayerSource source = LayerSource.ofDatabase(connection, table.label(), null, null);
                    try {
                        layerLoader.loadTiledTable(source, source.path(), LocalStyleStore.read(source));
                    } catch (Exception e) {
                        e.printStackTrace();
                        problems.add(table.label() + ": " + e.getMessage());
                    }
                }
                if (!problems.isEmpty()) {
                    GuiUtilities.showWarningMessage(wwjPanel, "WARNING",
                            "Some tables were not loaded:\n" + String.join("\n", problems));
                }
            }
        }.execute();
    }

    /**
     * Open the database of a connection, as the database viewer does.
     *
     * @param connection the connection.
     * @return the open database, to be closed by the caller.
     * @throws Exception if the database can't be opened.
     */
    public static ASpatialDb openDb( ConnectionData connection ) throws Exception {
        EDb type = EDb.forCode(connection.dbType);
        if ((type == EDb.SPATIALITE || type == EDb.GEOPACKAGE) && !new File(connection.connectionUrl).exists()) {
            // opening would create a new empty database
            throw new IllegalArgumentException("The database file doesn't exist: " + connection.connectionUrl);
        }
        ASpatialDb db = type == EDb.SPATIALITE ? new GTSpatialiteThreadsafeDb() : type.getSpatialDb();
        db.setCredentials(connection.user, connection.password);
        db.open(connection.connectionUrl);
        return db;
    }

    /**
     * @return the tables with a geometry column, sorted by name. The PostGIS ones are read from
     *          the geometry_columns view in one query, since the databases can have many tables.
     */
    private static List<DbTable> listSpatialTables( ASpatialDb db ) throws Exception {
        List<DbTable> tables = new ArrayList<>();
        if (db.getType() == EDb.POSTGIS) {
            String sql = "SELECT f_table_schema, f_table_name, type FROM geometry_columns ORDER BY f_table_schema, f_table_name";
            Set<String> labels = new LinkedHashSet<>();
            db.execOnConnection(connection -> {
                try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                    while( rs.next() ) {
                        String schema = rs.getString(1);
                        String name = rs.getString(2);
                        String type = rs.getString(3);
                        String label = DEFAULT_PG_SCHEMA.equals(schema) ? name : schema + "." + name;
                        // a table with more geometry columns is listed once
                        if (labels.add(label)) {
                            tables.add(new DbTable(SqlName.qualified(schema, name), label, typeName(type)));
                        }
                    }
                    return null;
                }
            });
        } else {
            for( TableName tableName : db.getTables() ) {
                SqlName sqlName = tableName.toSqlName();
                GeometryColumn geometryColumn = db.getGeometryColumnsForTable(sqlName);
                if (geometryColumn != null) {
                    String type = geometryColumn.geometryType != null ? geometryColumn.geometryType.name() : null;
                    tables.add(new DbTable(sqlName, tableName.getFullName(), typeName(type)));
                }
            }
            tables.sort(( t1, t2 ) -> t1.label().compareToIgnoreCase(t2.label()));
        }
        return tables;
    }

    private static String typeName( String type ) {
        return type == null ? "vector" : type.toLowerCase();
    }

    /**
     * Read the features of a table, in WGS84 lon/lat.
     *
     * @param db the database.
     * @param table the table.
     * @param viewEnvelope the extent of the map view in WGS84 lon/lat, to read only the features
     *          in it, or <code>null</code> to read all of them.
     * @return the features, or <code>null</code> if there are none.
     */
    private static SimpleFeatureCollection readTable( ASpatialDb db, DbTable table, Envelope viewEnvelope ) throws Exception {
        SimpleFeatureCollection readFC = readFeatures(db, table.sqlName(), viewEnvelope, null);
        if (readFC.getSchema() == null || readFC.isEmpty()) {
            return null;
        }
        CoordinateReferenceSystem crs = readFC.getSchema().getCoordinateReferenceSystem();
        if (crs != null && !HMCrsRegistry.crsEquals(crs, NwwUtilities.GPS_CRS)) {
            readFC = new HMCrsTransformer(crs, NwwUtilities.GPS_CRS).transform(readFC);
        }
        return readFC;
    }

    /**
     * @param db the database.
     * @param table the table.
     * @param extent a WGS84 lon/lat extent.
     * @return the extent in the CRS of the table: the envelope of its densified borders, since
     *          the borders are not straight in a projected CRS.
     */
    public static Envelope toTableEnvelope( ASpatialDb db, SqlName table, Envelope extent ) throws Exception {
        GeometryColumn geometryColumn = db.getGeometryColumnsForTable(table);
        int srid = geometryColumn != null ? geometryColumn.srid : -1;
        if (srid > 0 && srid != 4326) {
            return new HMCrsTransformer("EPSG:4326", "EPSG:" + srid, true, true).transformDensified(extent,
                    ENVELOPE_DENSIFICATION_POINTS);
        }
        return extent;
    }

    /**
     * Read the features of a table, in its CRS.
     *
     * @param db the database.
     * @param table the table.
     * @param extent the WGS84 lon/lat extent to read the features in, <code>null</code> for all.
     * @param where a SQL WHERE condition, <code>null</code> for all the features.
     * @return the features, without type if there are none.
     */
    public static SimpleFeatureCollection readFeatures( ASpatialDb db, SqlName table, Envelope extent, String where )
            throws Exception {
        Envelope envelope = extent != null ? toTableEnvelope(db, table, extent) : null;
        return SpatialDbsImportUtils.tableToFeatureFCollection(db, table, -1, -1, envelope, where);
    }

    /**
     * Count the features of a table, which fails if the condition is wrong.
     *
     * @param db the database.
     * @param table the table.
     * @param extent the WGS84 lon/lat extent to count the features in, <code>null</code> for all.
     * @param where a SQL WHERE condition, <code>null</code> for all the features.
     * @return the number of features.
     */
    public static long countFeatures( ASpatialDb db, SqlName table, Envelope extent, String where ) throws Exception {
        List<String> conditions = new ArrayList<>();
        if (extent != null) {
            Envelope envelope = toTableEnvelope(db, table, extent);
            String bboxPiece = db.getSpatialindexBBoxWherePiece(table, null, envelope.getMinX(), envelope.getMinY(),
                    envelope.getMaxX(), envelope.getMaxY());
            if (bboxPiece != null) {
                conditions.add(bboxPiece);
            }
        }
        if (where != null) {
            conditions.add("(" + where + ")");
        }
        String sql = "select count(*) from " + table.fixedDoubleName
                + (conditions.isEmpty() ? "" : " where " + String.join(" AND ", conditions));
        return db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                return rs.next() ? rs.getLong(1) : 0L;
            }
        });
    }
}
