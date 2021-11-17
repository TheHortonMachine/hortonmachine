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
package org.hortonmachine.database;

import java.awt.event.ActionEvent;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.geotools.coverage.grid.io.AbstractGridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ASqlTemplates;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IHmExtrasDb;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.datatypes.EDataType;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.nosql.INosqlCollection;
import org.hortonmachine.dbs.nosql.INosqlDb;
import org.hortonmachine.dbs.postgis.PGDb;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.ITilesProducer;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gforms.DbFormHandler;
import org.hortonmachine.gforms.FormBuilderController;
import org.hortonmachine.gforms.IFormHandler;
import org.hortonmachine.gui.console.LogConsoleController;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiBridgeHandler;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.style.MainController;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * Simple queries templates.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("serial")
public class SqlTemplatesAndActions {

    public static final String HM_SAVED_DATABASES = "HM-SAVED-DATABASES";
    private ASqlTemplates sqlTemplates;
    private boolean isNosql;

    public SqlTemplatesAndActions( EDb dbType ) throws Exception {
        sqlTemplates = dbType.getSqlTemplates();
        isNosql = dbType.isNosql();
    }

    private static final Logger logger = Logger.INSTANCE;

    public Action getSelectOnColumnAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        return new AbstractAction("Select on column"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                String columnName = column.columnName;
                String tableName = column.parent.tableName;
                String query = sqlTemplates.selectOnColumn(columnName, tableName);
                spatialiteViewer.addTextToQueryEditor(query);
            }
        };
    }

    public Action getUpdateOnColumnAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        return new AbstractAction("Update on column"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                String tableName = column.parent.tableName;
                String columnName = column.columnName;
                String query = sqlTemplates.updateOnColumn(DbsUtilities.fixTableName(tableName), columnName);
                spatialiteViewer.addTextToQueryEditor(query);
            }
        };
    }

    public Action getAddGeometryAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (sqlTemplates.hasAddGeometryColumn()) {
            String title = "Add geometry column";
            return new AbstractAction(title){
                @Override
                public void actionPerformed( ActionEvent e ) {

                    String[] labels = {"Table name", "Column name", "SRID", "Geometry type", "Dimension"};
                    String[] values = {column.parent.tableName, column.columnName, "4326", "POINT", "XY"};
                    String[] result = GuiUtilities.showMultiInputDialog(spatialiteViewer, title, labels, values, null);

                    String tableName = result[0];
                    String columnName = result[1];
                    String srid = result[2];
                    String geomType = result[3];
                    String dimension = result[4];
                    String query = sqlTemplates.addGeometryColumn(tableName, columnName, srid, geomType, dimension);
                    spatialiteViewer.addTextToQueryEditor(query);
                }
            };
        } else {
            return null;
        }
    }

    public Action getRecoverGeometryAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (sqlTemplates.hasRecoverGeometryColumn()) {
            String title = "Recover geometry column";
            return new AbstractAction(title){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    String[] labels = {"Table name", "Column name", "SRID", "Geometry type", "Dimension"};
                    String[] values = {column.parent.tableName, column.columnName, "4326", "POINT", "XY"};
                    String[] result = GuiUtilities.showMultiInputDialog(spatialiteViewer, title, labels, values, null);

                    String tableName = result[0];
                    String columnName = result[1];
                    String srid = result[2];
                    String geomType = result[3];
                    String dimension = result[4];
                    String query = sqlTemplates.recoverGeometryColumn(tableName, columnName, srid, geomType, dimension);
                    spatialiteViewer.addTextToQueryEditor(query);
                }
            };
        } else {
            return null;
        }
    }

    public Action getDiscardGeometryColumnAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (sqlTemplates.hasAddGeometryColumn()) {
            return new AbstractAction("Discard geometry column"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    String tableName = column.parent.tableName;
                    String geometryColumnName = column.geomColumn.geometryColumnName;
                    String query = sqlTemplates.discardGeometryColumn(tableName, geometryColumnName);
                    spatialiteViewer.addTextToQueryEditor(query);
                }

            };
        } else {
            return null;
        }
    }

    public Action getCreateSpatialIndexAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (!sqlTemplates.hasCreateSpatialIndex()) {
            return null;
        }
        return new AbstractAction("Create spatial index"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                String tableName = column.parent.tableName;
                String columnName = column.columnName;
                String query = sqlTemplates.createSpatialIndex(tableName, columnName);
                spatialiteViewer.addTextToQueryEditor(query);
            }

        };
    }

    public Action getCheckSpatialIndexAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (sqlTemplates.hasRecoverSpatialIndex()) {
            return new AbstractAction("Check spatial index"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    String tableName = column.parent.tableName;
                    String columnName = column.columnName;
                    String query = sqlTemplates.checkSpatialIndex(tableName, columnName);
                    spatialiteViewer.addTextToQueryEditor(query);
                }

            };
        } else {
            return null;
        }
    }

    public Action getRecoverSpatialIndexAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (sqlTemplates.hasRecoverSpatialIndex()) {
            return new AbstractAction("Recover spatial index"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    String tableName = column.parent.tableName;
                    String columnName = column.columnName;
                    String query = sqlTemplates.recoverSpatialIndex(tableName, columnName);
                    spatialiteViewer.addTextToQueryEditor(query);
                }
            };
        } else {
            return null;
        }
    }

    public Action getDisableSpatialIndexAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (sqlTemplates.hasRecoverSpatialIndex()) {
            return new AbstractAction("Disable spatial index"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    String tableName = column.parent.tableName;
                    String columnName = column.columnName;
                    String query = sqlTemplates.disableSpatialIndex(tableName, columnName);
                    spatialiteViewer.addTextToQueryEditor(query);
                }

            };
        } else {
            return null;
        }
    }

    public Action getShowSpatialMetadataAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        return new AbstractAction("Show spatial metadata"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                String tableName = column.parent.tableName;
                String columnName = column.columnName;
                String query = sqlTemplates.showSpatialMetadata(tableName, columnName);
                spatialiteViewer.addTextToQueryEditor(query);
            }
        };
    }

    public Action getCombinedSelectAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        return new AbstractAction("Create combined select statement"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                String[] tableColsFromFK = column.tableColsFromFK();
                String refTable = tableColsFromFK[0];
                String refColumn = tableColsFromFK[1];
                String tableName = column.parent.tableName;
                String columnName = column.columnName;
                String query = sqlTemplates.combinedSelect(refTable, refColumn, tableName, columnName);
                spatialiteViewer.addTextToQueryEditor(query);
            }

        };
    }

    public Action getQuickViewOtherTableAction( ColumnLevel column, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        return new AbstractAction("Quick view other table"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    String[] tableColsFromFK = column.tableColsFromFK();
                    String refTable = tableColsFromFK[0];
                    QueryResult queryResult;
                    if (spatialiteViewer.currentConnectedSqlDatabase instanceof ASpatialDb) {
                        queryResult = ((ASpatialDb) spatialiteViewer.currentConnectedSqlDatabase).getTableRecordsMapIn(refTable,
                                null, 1000, -1, null);
                    } else {
                        queryResult = spatialiteViewer.currentConnectedSqlDatabase
                                .getTableRecordsMapFromRawSql("select * from " + refTable, 100);
                    }
                    spatialiteViewer.loadDataViewer(queryResult);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    public Action getRefreshDatabaseAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
        return new AbstractAction("Refresh"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                final LogConsoleController logConsole = new LogConsoleController(databaseViewer.pm);
                JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

                new Thread(() -> {
                    logConsole.beginProcess("Refresh database");
                    try {
                        databaseViewer.refreshDatabaseTree();
                    } catch (Exception ex) {
                        databaseViewer.currentConnectedSqlDatabase = null;
                        databaseViewer.currentConnectedNosqlDatabase = null;
                        logger.insertError("SqlTemplatesAndActions", "Error refreshing database...", ex);
                    } finally {
                        logConsole.finishProcess();
                        logConsole.stopLogging();
                        logConsole.setVisible(false);
                        window.dispose();
                    }
                }, "SqlTemplates->refresh database").start();
            }
        };
    }

    public Action getCopyDatabasePathAction( DatabaseViewer spatialiteViewer ) {
        return new AbstractAction("Copy path"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                if (spatialiteViewer.currentConnectedSqlDatabase != null) {
                    String databasePath = spatialiteViewer.currentConnectedSqlDatabase.getDatabasePath();
                    GuiUtilities.copyToClipboard(databasePath);
                } else if (spatialiteViewer.currentConnectedNosqlDatabase != null) {
                    String databasePath = spatialiteViewer.currentConnectedNosqlDatabase.getDbEngineUrl();
                    GuiUtilities.copyToClipboard(databasePath);
                }
            }
        };
    }

    public Action getCreateTableFromShapefileSchemaAction( GuiBridgeHandler guiBridge, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        return new AbstractAction("Create table from shapefile"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                if (!(spatialiteViewer.currentConnectedSqlDatabase instanceof ASpatialDb)) {
                    GuiUtilities.showWarningMessage(spatialiteViewer,
                            DatabaseController.THIS_ACTION_IS_AVAILABLE_ONLY_FOR_SPATIAL_DATABASES);
                    return;
                }
                File[] openFiles = guiBridge.showOpenFileDialog("Open shapefile", PreferencesHandler.getLastFile(),
                        HMConstants.vectorFileFilter);
                if (openFiles != null && openFiles.length > 0) {
                    try {
                        PreferencesHandler.setLastPath(openFiles[0].getAbsolutePath());
                    } catch (Exception e1) {
                        logger.insertError("SqlTemplatesAndActions", "ERROR", e1);
                    }
                } else {
                    return;
                }
                try {
                    String nameWithoutExtention = FileUtilities.getNameWithoutExtention(openFiles[0]);
                    String newTableName = null;
                    String sridStr = null;

                    try {
                        ReferencedEnvelope env = OmsVectorReader.readEnvelope(openFiles[0].getAbsolutePath());
                        int srid = CrsUtilities.getSrid(env.getCoordinateReferenceSystem());

                        String[] results = GuiUtilities.showMultiInputDialog(spatialiteViewer, "Set parameters",
                                new String[]{"Set new table name (can't start with numbers)", "Choose the import EPSG code"},
                                new String[]{nameWithoutExtention, String.valueOf(srid)}, null);
                        if (results == null) {
                            return;
                        }
                        newTableName = results[0];
                        sridStr = results[1];

                        try {
                            Integer.parseInt(sridStr);
                        } catch (Exception e1) {
                            sridStr = null;
                        }

                    } catch (Exception e1) {
                        Logger.INSTANCE.insertError("getCreateTableFromShapefileSchemaAction", "ERROR", e1);
                        newTableName = GuiUtilities.showInputDialog(spatialiteViewer,
                                "Set new table name (can't start with numbers)", nameWithoutExtention);
                    }
                    if (newTableName.trim().length() == 0) {
                        newTableName = null;
                    }
                    if (newTableName == null) {
                        return;
                    }

                    SpatialDbsImportUtils.createTableFromShp((ASpatialDb) spatialiteViewer.currentConnectedSqlDatabase,
                            openFiles[0], newTableName, sridStr, false);
                    spatialiteViewer.refreshDatabaseTree();
                } catch (Exception e1) {
                    GuiUtilities.handleError(spatialiteViewer, e1);
                }
            }
        };
    }

    public Action getAttachShapefileAction( GuiBridgeHandler guiBridge, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        if (sqlTemplates.hasAttachShapefile()) {
            return new AbstractAction("Attach readonly shapefile"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    File[] openFiles = guiBridge.showOpenFileDialog("Open shapefile", PreferencesHandler.getLastFile(),
                            HMConstants.vectorFileFilter);
                    if (openFiles != null && openFiles.length > 0) {
                        try {
                            PreferencesHandler.setLastPath(openFiles[0].getAbsolutePath());
                        } catch (Exception e1) {
                            logger.insertError("SqlTemplatesAndActions", "ERROR", e1);
                        }
                    } else {
                        return;
                    }
                    try {
                        String query = sqlTemplates.attachShapefile(openFiles[0]);
                        spatialiteViewer.addTextToQueryEditor(query);
                    } catch (Exception e1) {
                        logger.insertError("SqlTemplatesAndActions", "ERROR", e1);
                    }

                }
            };
        } else {
            return null;
        }
    }

    public Action getSelectAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        return new AbstractAction("Select statement"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    String query = DbsUtilities.getSelectQuery(spatialiteViewer.currentConnectedSqlDatabase, table, false);
                    spatialiteViewer.addTextToQueryEditor(query);
                } catch (Exception e1) {
                    logger.insertError("SqlTemplatesAndActions", "Error", e1);
                }
            }
        };
    }

    public Action getInsertAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        // TODO add json to collection if nosql?
        return new AbstractAction("Insert statement"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    List<String[]> tableColumns = spatialiteViewer.currentConnectedSqlDatabase.getTableColumns(table.tableName);

                    String cols = tableColumns.stream().map(tc -> tc[0]).collect(Collectors.joining(","));
                    String quest = tableColumns.stream().map(tc -> "?").collect(Collectors.joining(","));

                    String query = "INSERT INTO " + DbsUtilities.fixTableName(table.tableName) + " (" + cols + ") VALUES ("
                            + quest + ");";
                    spatialiteViewer.addTextToQueryEditor(query);
                } catch (Exception e1) {
                    logger.insertError("SqlTemplatesAndActions", "Error", e1);
                }
            }
        };
    }

    public Action getDropAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return new AbstractAction("Drop collection " + table.tableName){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    try {

                        boolean doDrop = GuiUtilities.showYesNoDialog(spatialiteViewer,
                                "Are you sure you want to drop collection '" + table.tableName + "'?");
                        if (doDrop) {
                            INosqlCollection collection = spatialiteViewer.currentConnectedNosqlDatabase
                                    .getCollection(table.tableName);
                            collection.drop();
                            spatialiteViewer.refreshDatabaseTree();
                        }
                    } catch (Exception ex) {
                        logger.insertError("SqlTemplatesAndActions", "Error", ex);
                    }
                }
            };
        }
        return new AbstractAction("Drop table statement"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    List<ColumnLevel> columnsList = table.columnsList;
                    String tableName = table.tableName;
                    String geometryColumnName = null;
                    for( ColumnLevel columnLevel : columnsList ) {
                        if (columnLevel.geomColumn != null) {
                            geometryColumnName = columnLevel.geomColumn.geometryColumnName;
                            break;
                        }
                    }
                    String query = sqlTemplates.dropTable(tableName, geometryColumnName);
                    spatialiteViewer.addTextToQueryEditor(query);
                } catch (Exception ex) {
                    logger.insertError("SqlTemplatesAndActions", "Error", ex);
                }
            }
        };
    }

    public Action getCountRowsAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        return new AbstractAction("Count table records"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    long count = 0;
                    if (spatialiteViewer.currentConnectedSqlDatabase != null) {
                        String tableName = table.tableName;
                        count = spatialiteViewer.currentConnectedSqlDatabase.getCount(tableName);
                    } else if (spatialiteViewer.currentConnectedNosqlDatabase != null) {
                        String tableName = table.tableName;
                        INosqlCollection collection = spatialiteViewer.currentConnectedNosqlDatabase.getCollection(tableName);
                        count = collection.getCount();
                    }
                    JOptionPane.showMessageDialog(spatialiteViewer, "Count: " + count);
                } catch (Exception ex) {
                    logger.insertError("SqlTemplatesAndActions", "Error", ex);
                }
            }
        };
    }

    public Action getImportShapefileDataAction( GuiBridgeHandler guiBridge, TableLevel table, DatabaseViewer spatialiteViewer,
            boolean useFromTextForGeom ) {
        if (isNosql) {
            return null;
        }
        return new AbstractAction("Import data from shapefile"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                File[] openFiles = guiBridge.showOpenFileDialog("Open shapefile", PreferencesHandler.getLastFile(),
                        HMConstants.vectorFileFilter);
                if (openFiles != null && openFiles.length > 0) {
                    try {
                        PreferencesHandler.setLastPath(openFiles[0].getAbsolutePath());
                    } catch (Exception e1) {
                        logger.insertError("SqlTemplatesAndActions", "ERROR", e1);
                    }
                } else {
                    return;
                }

                final LogConsoleController logConsole = new LogConsoleController(spatialiteViewer.pm);
                JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

                new Thread(() -> {
                    boolean hasErrors = false;
                    logConsole.beginProcess("Importing data...");
                    try {
                        hasErrors = !SpatialDbsImportUtils.importShapefile(
                                (ASpatialDb) spatialiteViewer.currentConnectedSqlDatabase, openFiles[0],
                                spatialiteViewer.currentSelectedTable.tableName, -1, useFromTextForGeom, spatialiteViewer.pm);
                    } catch (Exception ex) {
                        logger.insertError("SqlTemplatesAndActions", "Error importing data from shapefile", ex);
                        hasErrors = true;
                    } finally {
                        logConsole.finishProcess();
                        logConsole.stopLogging();
                        if (!hasErrors) {
                            logConsole.setVisible(false);
                            window.dispose();
                        }
                    }
                }, "SqlTemplates->import shapefile").start();
            }
        };
    }

    public Action getReprojectTableAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        if (!sqlTemplates.hasReprojectTable())
            return null;
        return new AbstractAction("Reproject table"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    ColumnLevel geometryColumn = table.getFirstGeometryColumn();
                    if (geometryColumn == null) {
                        GuiUtilities.showInfoMessage(spatialiteViewer, null, "Only spatial tables can be reprojected.");
                    }

                    String[] labels = {"New table name", "New SRID"};
                    String[] values = {table.tableName + "4326", "4326"};
                    String[] result = GuiUtilities.showMultiInputDialog(spatialiteViewer, "Reprojection parameters", labels,
                            values, null);

                    String tableName = table.tableName;
                    String newTableName = result[0];
                    String newSrid = result[1];

                    String query = sqlTemplates.reprojectTable(table, (ASpatialDb) spatialiteViewer.currentConnectedSqlDatabase,
                            geometryColumn, tableName, newTableName, newSrid);

                    spatialiteViewer.addTextToQueryEditor(query);
                } catch (Exception ex) {
                    logger.insertError("SqlTemplatesAndActions", "Error", ex);
                }
            }
        };
    }

    public Action getQuickViewTableAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        return new AbstractAction("Quick View Table in 3D"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    if (!(spatialiteViewer.currentConnectedSqlDatabase instanceof ASpatialDb)) {
                        GuiUtilities.showWarningMessage(spatialiteViewer,
                                DatabaseController.THIS_ACTION_IS_AVAILABLE_ONLY_FOR_SPATIAL_DATABASES);
                        return;
                    }
                    String query = DbsUtilities.getSelectQuery((ASpatialDb) spatialiteViewer.currentConnectedSqlDatabase, table,
                            false);
                    spatialiteViewer.viewSpatialQueryResult3D(table.tableName, query, spatialiteViewer.pm);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    public Action getQuickViewTableGeometriesAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        return new AbstractAction("Quick View Table Geometries"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    if (!(spatialiteViewer.currentConnectedSqlDatabase instanceof ASpatialDb)) {
                        GuiUtilities.showWarningMessage(spatialiteViewer,
                                DatabaseController.THIS_ACTION_IS_AVAILABLE_ONLY_FOR_SPATIAL_DATABASES);
                        return;
                    }
                    String query = DbsUtilities.getSelectQuery((ASpatialDb) spatialiteViewer.currentConnectedSqlDatabase, table,
                            false);
                    spatialiteViewer.viewSpatialQueryResult(table.tableName, query, spatialiteViewer.pm, true);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    public Action getOpenInSldEditorAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        if (spatialiteViewer.currentConnectedSqlDatabase.getType() == EDb.GEOPACKAGE
                || spatialiteViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGRES
                || spatialiteViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGIS) {
            return new AbstractAction("Open in SLD editor"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    try {

                        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
                        String databasePath = spatialiteViewer.currentConnectedSqlDatabase.getDatabasePath();

                        final MainController controller = new MainController(new File(databasePath), table.tableName);
                        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine SLD Editor");
                        Class<DatabaseViewer> class1 = DatabaseViewer.class;
                        ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
                        frame.setIconImage(icon.getImage());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
        } else {
            return null;
        }
    }

    public Action getOpenInGformsEditorAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        if (spatialiteViewer.currentConnectedSqlDatabase.getType() == EDb.GEOPACKAGE
                || spatialiteViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGRES
                || spatialiteViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGIS) {
            return new AbstractAction("Open in FORMS editor"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    try {
                        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
                        IFormHandler formHandler = new DbFormHandler(spatialiteViewer.currentConnectedSqlDatabase,
                                table.tableName);
                        final FormBuilderController controller = new FormBuilderController(formHandler);
                        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine FORMS Editor");
                        Class<DatabaseViewer> class1 = DatabaseViewer.class;
                        ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
                        frame.setIconImage(icon.getImage());
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            };
        } else {
            return null;
        }
    }

    public Action getGenerateInsertExportAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }

        return new AbstractAction("Generate insert sql statements"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
                    File[] saveFiles = gBridge.showSaveFileDialog("Save sql file", PreferencesHandler.getLastFile(), null);
                    if (saveFiles != null && saveFiles.length > 0) {
                        try {
                            PreferencesHandler.setLastPath(saveFiles[0].getAbsolutePath());
                        } catch (Exception e1) {
                            logger.insertError("SqlTemplatesAndActions", "ERROR", e1);
                        }
                    } else {
                        return;
                    }
                    File saveFile = saveFiles[0];

                    ADb db = spatialiteViewer.currentConnectedSqlDatabase;
                    QueryResult result = db.getTableRecordsMapFromRawSql("select * from " + table.tableName, -1);
                    List<String> colNames = result.names;
                    List<String> types = result.types;
                    List<Object[]> data = result.data;
                    StringBuilder sb = new StringBuilder();
                    for( int i = 0; i < data.size(); i++ ) {
                        Object[] record = data.get(i);

                        sb.append("INSERT INTO ").append(table.tableName).append("(");

                        StringBuilder namesSb = new StringBuilder();
                        StringBuilder valuesSb = new StringBuilder();

                        boolean firstDone = false;
                        for( int j = 0; j < record.length; j++ ) {
                            if (record[j] != null) {
                                String name = colNames.get(j);
                                String type = types.get(j);
                                EDataType etype = EDataType.getType4Name(type);
                                if (etype == EDataType.BLOB || etype == EDataType.GEOMETRY) {
                                    continue;
                                } else {
                                    if (firstDone) {
                                        namesSb.append(",");
                                        valuesSb.append(",");
                                    }

                                    namesSb.append(name);

                                    String value = record[j].toString();
                                    value = ADb.escapeSql(value);
                                    if (etype == EDataType.TEXT) {
                                        valuesSb.append("'").append(value).append("'");
                                    }else {
                                        valuesSb.append(value);
                                    }
                                    firstDone = true;
                                }
                            }
                        }

                        sb.append(namesSb.toString());
                        sb.append(") VALUES (");
                        sb.append(valuesSb.toString());
                        sb.append(");\n");
                    }

                    FileUtilities.writeFile(sb.toString(), saveFile);

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };

    }

    public Action getUpdateLayerStats( GuiBridgeHandler guiBridge, DatabaseViewer spatialiteViewer ) {
        if (sqlTemplates.hasRecoverGeometryColumn()) {
            return new AbstractAction("Update Layer Statistics"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    String query = "SELECT UpdateLayerStatistics();";
                    spatialiteViewer.addTextToQueryEditor(query);
                }
            };
        } else {
            return null;
        }
    }

    public Action getImportSqlFileAction( GuiBridgeHandler guiBridge, DatabaseViewer spatialiteViewer ) {
        if (isNosql) {
            return null;
        }
        return new AbstractAction("Import sql file"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                File[] openFiles = guiBridge.showOpenFileDialog("Open sql file", PreferencesHandler.getLastFile(), null);
                if (openFiles != null && openFiles.length > 0) {
                    try {
                        PreferencesHandler.setLastPath(openFiles[0].getAbsolutePath());
                    } catch (Exception e1) {
                        logger.insertError("SqlTemplatesAndActions", "ERROR", e1);
                    }
                } else {
                    return;
                }

                final LogConsoleController logConsole = new LogConsoleController(spatialiteViewer.pm);
                JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

                new Thread(() -> {
                    boolean hasErrors = false;
                    logConsole.beginProcess("Running sql from file...");
                    try {
                        File sqlFile = openFiles[0];
                        String readFile = FileUtilities.readFile(sqlFile);
                        spatialiteViewer.runQuery(readFile, spatialiteViewer.pm);
                        spatialiteViewer.refreshDatabaseTree();
                    } catch (Exception ex) {
                        logger.insertError("SqlTemplatesAndActions", "Error importing sql from file", ex);
                    } finally {
                        logConsole.finishProcess();
                        logConsole.stopLogging();
                        if (!hasErrors) {
                            logConsole.setVisible(false);
                            window.dispose();
                        }
                    }
                }, "SqlTemplates->import sql file").start();

            }
        };
    }

    public Action getNewCollectionAction( GuiBridgeHandler guiBridge, DatabaseViewer spatialiteViewer ) {
        if (!isNosql) {
            return null;
        }
        return new AbstractAction("Create new collection"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                String newName = GuiUtilities.showInputDialog(spatialiteViewer, "Insert the name for the new collection",
                        "newcollection");
                if (newName.trim().length() > 0) {
                    try {
                        if (!spatialiteViewer.currentConnectedNosqlDatabase.hasCollection(newName)) {
                            spatialiteViewer.currentConnectedNosqlDatabase.createCollection(newName);
                            spatialiteViewer.refreshDatabaseTree();
                        } else {
                            GuiUtilities.showWarningMessage(spatialiteViewer,
                                    "A collection named '" + newName + "' already exists.");
                        }
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                }
            }
        };
    }

    public Action getSaveConnectionAction( DatabaseViewer databaseViewer ) {
        return new AbstractAction("Save Connection"){
            @SuppressWarnings("unchecked")
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    ConnectionData connectionData = null;
                    if (databaseViewer.currentConnectedSqlDatabase != null) {
                        ADb db = databaseViewer.currentConnectedSqlDatabase;
                        connectionData = db.getConnectionData();
                    } else if (databaseViewer.currentConnectedNosqlDatabase != null) {
                        INosqlDb db = databaseViewer.currentConnectedNosqlDatabase;
                        connectionData = db.getConnectionData();
                    }

                    String newName = GuiUtilities.showInputDialog(databaseViewer, "Enter a name for the saved connection",
                            "db connection " + new DateTime().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS));
                    connectionData.connectionLabel = newName;

                    byte[] savedDbs = PreferencesHandler.getPreference(HM_SAVED_DATABASES, new byte[0]);
                    List<ConnectionData> connectionDataList = new ArrayList<>();
                    try {
                        connectionDataList = (List<ConnectionData>) convertFromBytes(savedDbs);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                    }
                    connectionDataList.add(connectionData);

                    byte[] inBytes = convertObjectToBytes(connectionDataList);
                    PreferencesHandler.setPreference(HM_SAVED_DATABASES, inBytes);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        };
    }

    public static byte[] convertObjectToBytes( Object object ) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream(); ObjectOutput out = new ObjectOutputStream(bos)) {
            out.writeObject(object);
            return bos.toByteArray();
        }
    }

    public static Object convertFromBytes( byte[] bytes ) throws Exception {
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes); ObjectInput in = new ObjectInputStream(bis)) {
            return in.readObject();
        }
    }

    public Action getImportRaster2TilesTableAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
        if (isNosql) {
            return null;
        }
        if (databaseViewer.currentConnectedSqlDatabase.getType() == EDb.GEOPACKAGE) {
            return new AbstractAction("Import raster to tileset"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    doTiles(guiBridge, databaseViewer, true);
                }
            };
        } else {
            return null;
        }
    }
    public Action getImportVector2TilesTableAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
        if (isNosql) {
            return null;
        }
        if (databaseViewer.currentConnectedSqlDatabase.getType() == EDb.GEOPACKAGE) {
            return new AbstractAction("Import vector to tileset"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    doTiles(guiBridge, databaseViewer, false);
                }
            };
        } else {
            return null;
        }
    }

    private static void doTiles( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer, boolean isRaster ) {
        try {

            String label = isRaster ? "raster" : "vector";
            FileFilter fileFilter = isRaster ? HMConstants.rasterFileFilter : HMConstants.vectorFileFilter;

            String title = "Open " + label + " file";
            File[] openFiles = GuiUtilities.showOpenFilesDialog(databaseViewer, title, true, PreferencesHandler.getLastFile(),
                    fileFilter);
            if (openFiles != null && openFiles.length > 0) {
                try {
                    PreferencesHandler.setLastPath(openFiles[0].getAbsolutePath());
                } catch (Exception e1) {
                    logger.insertError("SqlTemplatesAndActions", "ERROR", e1);
                }
            } else {
                return;
            }
            try {
                String targetEpsg = "epsg:" + GeopackageCommonDb.MERCATOR_SRID;
                CoordinateReferenceSystem mercatorCrs = CrsUtilities.getCrsFromEpsg(targetEpsg, null);
                GeopackageCommonDb db = (GeopackageCommonDb) databaseViewer.currentConnectedSqlDatabase;
                List<FeatureEntry> features4326 = db.features();
                PreparedGeometry limitsGeom3857 = null;
                if (features4326.size() > 0) {
                    List<String> names = features4326.stream().map(f -> f.getTableName()).collect(Collectors.toList());
                    String selectedTable = GuiUtilities.showComboDialog(databaseViewer, "Area of interest",
                            "It is possible to use one of the vector tables as area of interest. Tiles outside the area will be ignored.",
                            names.toArray(new String[0]), "");
                    if (selectedTable != null) {
                        GeometryColumn gc = db.getGeometryColumnsForTable(selectedTable);
                        List<Geometry> geometries = db.getGeometriesIn(selectedTable, (Envelope) null, (String[]) null);
                        int dataSrid = db.feature(selectedTable).getSrid();
                        List<Geometry> geometries3857;
                        if (dataSrid == GeopackageCommonDb.MERCATOR_SRID) {
                            geometries3857 = geometries;
                        } else {
                            String sourceEpsg = "epsg:" + dataSrid;
                            CoordinateReferenceSystem sourceCRS = CrsUtilities.getCrsFromEpsg(sourceEpsg, null);;
                            MathTransform transform = CRS.findMathTransform(sourceCRS, mercatorCrs);
                            geometries3857 = geometries.stream().map(g -> {
                                try {
                                    return JTS.transform(g, transform);
                                } catch (Exception e) {
                                    e.printStackTrace();
                                    return null;
                                }
                            }).collect(Collectors.toList());
                        }

                        if (gc.geometryType.isPolygon()) {
                            // use polygons
                            Geometry union = CascadedPolygonUnion.union(geometries3857);
                            limitsGeom3857 = PreparedGeometryFactory.prepare(union);
                        } else {
                            // use envelopes
                            Envelope env = new Envelope();
                            for( Geometry geometry : geometries3857 ) {
                                env.expandToInclude(geometry.getEnvelopeInternal());
                            }
                            Polygon polyEnv = GeometryUtilities.createPolygonFromEnvelope(env);
                            limitsGeom3857 = PreparedGeometryFactory.prepare(polyEnv);
                        }

                    }
                }

                String nameForTable = FileUtilities.getNameWithoutExtention(openFiles[0]);

                String[] zoomLevels = new String[]{"1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14",
                        "15", "16", "17", "18", "19"};
                HashMap<String, String[]> fields2ValuesMap = new HashMap<>();
                String newTableLabel = "name of the new table";
                String minZoomLevelLabel = "min zoom level";
                String maxZoomLevelLabel = "max zoom level";
                fields2ValuesMap.put(minZoomLevelLabel, zoomLevels);
                fields2ValuesMap.put(maxZoomLevelLabel, zoomLevels);

                String[] result = GuiUtilities.showMultiInputDialog(databaseViewer, "Select parameters", //
                        new String[]{newTableLabel, minZoomLevelLabel, maxZoomLevelLabel}, //
                        new String[]{nameForTable, "8", "16"}, //
                        null);
                if (result == null) {
                    return;
                }

                PreparedGeometry _limitsGeom3857 = limitsGeom3857;
                final LogConsoleController logConsole = new LogConsoleController(null);
                IHMProgressMonitor pm = logConsole.getProgressMonitor();
                Logger.INSTANCE.setOutPrintStream(logConsole.getLogAreaPrintStream());
                Logger.INSTANCE.setErrPrintStream(logConsole.getLogAreaPrintStream());
                guiBridge.showWindow(logConsole.asJComponent(), "Console Log");
                new Thread(() -> {
                    try {
                        logConsole.beginProcess("Import " + label + " to tileset");

                        pm.message("Checking input parameters...");
                        String _nameForTable = result[0];
                        int minZoom = 8;
                        int maxZoom = 16;
                        try {
                            minZoom = Integer.parseInt(result[1]);
                            maxZoom = Integer.parseInt(result[2]);
                        } catch (Exception e1) {
                            pm.errorMessage("The min or max zoomlevel were not entered correctly, exiting.");
                            return;
                        }

                        for( File file : openFiles ) {
                            String dataPath = file.getAbsolutePath();

                            Envelope envelopeInternal = null;

                            if (isRaster) {
                                AbstractGridFormat format = GridFormatFinder.findFormat(file);
                                AbstractGridCoverage2DReader reader = format.getReader(file, null);
                                GeneralEnvelope originalEnvelope = reader.getOriginalEnvelope();
                                CoordinateReferenceSystem crs = reader.getCoordinateReferenceSystem();
                                double[] ll = originalEnvelope.getLowerCorner().getCoordinate();
                                double[] ur = originalEnvelope.getUpperCorner().getCoordinate();
                                ReferencedEnvelope renv = new ReferencedEnvelope(ll[0], ur[0], ll[1], ur[1], crs);
                                envelopeInternal = renv.transform(mercatorCrs, true);
                            } else {
                                ReferencedEnvelope renv = OmsVectorReader.readEnvelope(file.getAbsolutePath());
                                envelopeInternal = renv.transform(mercatorCrs, true);
                            }

                            ITilesProducer tileProducer = new GeopackageTilesProducer(pm, dataPath, isRaster, minZoom, maxZoom,
                                    256, _limitsGeom3857);
                            String description = "HM import of " + openFiles[0].getName();
                            int addedTiles = ((GeopackageCommonDb) databaseViewer.currentConnectedSqlDatabase)
                                    .addTilestable(_nameForTable, description, envelopeInternal, tileProducer);

                            pm.message("Inserted " + addedTiles + " new tiles.");
                        }

                        databaseViewer.refreshDatabaseTree();

                    } catch (Exception ex) {
                        pm.errorMessage(ex.getLocalizedMessage());
                    } finally {
                        logConsole.finishProcess();
                        logConsole.stopLogging();
                        Logger.INSTANCE.resetStreams();
                    }
                }, "DatabaseController->Import " + label + " to tileset").start();

            } catch (Exception e1) {
                GuiUtilities.handleError(databaseViewer, e1);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public Action getSwitchDatabaseAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
        if (isNosql) {
            return new AbstractAction("Switch database"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    INosqlDb db = databaseViewer.currentConnectedNosqlDatabase;
                    List<String> databasesNames = db.getDatabasesNames();
                    String selectedName = GuiUtilities.showComboDialog(databaseViewer, "Select database",
                            "Select the database to switch to", databasesNames.toArray(new String[0]), db.getDbName());
                    if (selectedName != null && !selectedName.equals(db.getDbName())) {
                        ConnectionData connectionData = db.getConnectionData();
                        connectionData.connectionLabel = selectedName;
                        connectionData.connectionUrl = db.getDbEngineUrl() + "/" + selectedName;
                        databaseViewer.openDatabase(connectionData, false);
                    }
                }
            };

        } else if (databaseViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGIS
                || databaseViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGRES) {
            return new AbstractAction("Switch database"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    ADb db = databaseViewer.currentConnectedSqlDatabase;
                    try {
                        List<String> databasesNames = PGDb.getDatabases(db);
                        ConnectionData connectionData = db.getConnectionData();
                        int lastSlash = connectionData.connectionUrl.lastIndexOf('/');
                        String engineUrl = connectionData.connectionUrl.substring(0, lastSlash);
                        String dbName = connectionData.connectionUrl.substring(lastSlash + 1);

                        String selectedName = GuiUtilities.showComboDialog(databaseViewer, "Select database",
                                "Select the database to switch to", databasesNames.toArray(new String[0]), dbName);
                        if (selectedName != null && !selectedName.equals(dbName)) {
                            connectionData.connectionUrl = engineUrl + "/" + selectedName;
                            connectionData.connectionLabel = selectedName;
                            databaseViewer.openDatabase(connectionData, false);
                        }
                    } catch (Exception ex) {
                        GuiUtilities.handleError(databaseViewer, ex);
                        Logger.INSTANCE.e("Error", ex);
                    }
                }
            };
        }
        return null;
    }

    public Action getDropDatabaseAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
        if (isNosql) {
            return new AbstractAction("Drop current database"){
                @Override
                public void actionPerformed( ActionEvent e ) {

                    INosqlDb db = databaseViewer.currentConnectedNosqlDatabase;

                    try {
                        boolean doDrop = GuiUtilities.showYesNoDialog(databaseViewer,
                                "Are you sure you want to drop the current database '" + db.getDbName()
                                        + "'?\nThis can't be undone!");
                        if (doDrop) {
                            db.drop();
                            databaseViewer.closeCurrentDb(true);
                        }
                    } catch (Exception e1) {
                        GuiUtilities.handleError(databaseViewer, e1);
                        Logger.INSTANCE.e("Error", e1);
                    }
                }
            };

//        } else if (databaseViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGIS
//                || databaseViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGRES) {
//            return new AbstractAction("Switch database"){
//                @Override
//                public void actionPerformed( ActionEvent e ) {
//                    // TODO?
//                }
//            };
        }
        return null;
    }

    public Action getInsertCollectionDocumentAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
        if (isNosql) {
            return new AbstractAction("Add document"){
                @Override
                public void actionPerformed( ActionEvent e ) {

                    try {
                        if (databaseViewer.currentSelectedTable != null) {
                            INosqlDb db = databaseViewer.currentConnectedNosqlDatabase;
                            String json = GuiUtilities.showInputAreaDialog(databaseViewer,
                                    "Add json to convert to document here.", "");
                            if (json != null) {
                                INosqlCollection collection = db.getCollection(databaseViewer.currentSelectedTable.tableName);
                                collection.insert(json);
                            }
                        } else {
                            GuiUtilities.showWarningMessage(databaseViewer, "Select a collection to insert the document to.");
                        }
                    } catch (Exception e1) {
                        GuiUtilities.handleError(databaseViewer, e1);
                        Logger.INSTANCE.e("Error", e1);
                    }
                }
            };
        }
        return null;
    }

    public Action getUpdateCollectionDocumentAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
        if (isNosql) {
            return new AbstractAction("Update document by OID"){
                @Override
                public void actionPerformed( ActionEvent e ) {

                    try {
                        if (databaseViewer.currentSelectedTable != null) {
                            INosqlDb db = databaseViewer.currentConnectedNosqlDatabase;
                            String oid = GuiUtilities.showInputDialog(databaseViewer, "Insert the document OID", "");
                            if (oid != null && oid.trim().length() > 0) {
                                oid = oid.trim();

                                String json = GuiUtilities.showInputAreaDialog(databaseViewer,
                                        "Add json to convert to document here.", "");
                                if (json != null) {
                                    INosqlCollection collection = db.getCollection(databaseViewer.currentSelectedTable.tableName);
                                    collection.updateByOid(oid, json);
                                }
                            }

                        } else {
                            GuiUtilities.showWarningMessage(databaseViewer, "Select a collection to insert the document to.");
                        }
                    } catch (Exception e1) {
                        GuiUtilities.handleError(databaseViewer, e1);
                        Logger.INSTANCE.e("Error", e1);
                    }
                }
            };
        }
        return null;
    }

    public Action getDeleteCollectionDocumentByIdAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
        if (isNosql) {
            return new AbstractAction("Delete document by OID"){
                @Override
                public void actionPerformed( ActionEvent e ) {

                    try {
                        if (databaseViewer.currentSelectedTable != null) {
                            String oid = GuiUtilities.showInputDialog(databaseViewer, "Insert the document OID", "");
                            if (oid != null && oid.trim().length() > 0) {
                                oid = oid.trim();
                                INosqlDb db = databaseViewer.currentConnectedNosqlDatabase;
                                INosqlCollection collection = db.getCollection(databaseViewer.currentSelectedTable.tableName);
                                collection.deleteByOid(oid);
                            }

                        } else {
                            GuiUtilities.showWarningMessage(databaseViewer, "Select a collection to delete the document from.");
                        }
                    } catch (Exception e1) {
                        GuiUtilities.handleError(databaseViewer, e1);
                        Logger.INSTANCE.e("Error", e1);
                    }
                }
            };
        }
        return null;
    }

    public Action getUpdateValueAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
//        if (isNosql) {
//            return new AbstractAction("Update value by OID"){
//                @Override
//                public void actionPerformed( ActionEvent e ) {
//
//                    try {
//                      // TODO
//                    } catch (Exception e1) {
//                        GuiUtilities.handleError(databaseViewer, e1);
//                        Logger.INSTANCE.e("Error", e1);
//                    }
//                }
//            };
//        }
        return null;
    }

    public Action getViewActiveSessionsAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
        if (!isNosql && (databaseViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGRES
                || databaseViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGIS)) {
            return new AbstractAction("List active connections"){
                @Override
                public void actionPerformed( ActionEvent e ) {

                    try {
                        databaseViewer.runQuery("SELECT * FROM pg_stat_activity;", null);
                    } catch (Exception e1) {
                        GuiUtilities.handleError(databaseViewer, e1);
                        Logger.INSTANCE.e("Error", e1);
                    }
                }
            };
        }
        return null;
    }

    public Action getCleanIdleSessionsAction( GuiBridgeHandler guiBridge, DatabaseViewer databaseViewer ) {
        if (!isNosql && (databaseViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGRES
                || databaseViewer.currentConnectedSqlDatabase.getType() == EDb.POSTGIS)) {
            return new AbstractAction("Clean up idle connections"){
                @Override
                public void actionPerformed( ActionEvent e ) {

                    try {
                        String sql = "SELECT pid, datname, usename FROM  pg_stat_activity "
                                + "WHERE  state in ('idle', 'idle in transaction', 'idle in transaction (aborted)', 'disabled') "
                                + "order by datname";
                        QueryResult result = databaseViewer.currentConnectedSqlDatabase.getTableRecordsMapFromRawSql(sql, 0);

                        String killSql = "SELECT pg_terminate_backend(PID);";

                        int pidIndex = result.names.indexOf("pid");
                        int datnameIndex = result.names.indexOf("datname");
                        int usenameIndex = result.names.indexOf("usename");
                        StringBuilder sb = new StringBuilder();
                        for( int i = 0; i < result.data.size(); i++ ) {
                            Object[] objects = result.data.get(i);
                            String pid = objects[pidIndex].toString();
                            String datname = objects[datnameIndex].toString();
                            String usename = objects[usenameIndex].toString();

                            sb.append("-- db: ").append(datname).append(" user:").append(usename).append(" pid:").append(pid)
                                    .append("\n");
                            sb.append(killSql.replaceFirst("PID", pid)).append("\n");
                        }

                        databaseViewer.addTextToQueryEditor(sb.toString());
                    } catch (Exception e1) {
                        GuiUtilities.handleError(databaseViewer, e1);
                        Logger.INSTANCE.e("Error", e1);
                    }
                }
            };
        }
        return null;
    }
}
