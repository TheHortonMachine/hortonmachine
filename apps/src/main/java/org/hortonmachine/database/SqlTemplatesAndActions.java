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
import java.io.File;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import javax.swing.JOptionPane;

import org.hortonmachine.dbs.compat.ASqlTemplates;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gui.console.LogConsoleController;
import org.hortonmachine.gui.utils.GuiBridgeHandler;
import org.hortonmachine.gui.utils.GuiUtilities;

/**
 * Simple queries templates.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("serial")
public class SqlTemplatesAndActions {

    private ASqlTemplates sqlTemplates;

    public SqlTemplatesAndActions( EDb dbType ) throws Exception {
        sqlTemplates = dbType.getSqlTemplates();
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
        return new AbstractAction("Update on column"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                String tableName = column.parent.tableName;
                String columnName = column.columnName;
                String query = sqlTemplates.updateOnColumn(tableName, columnName);
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
        if (sqlTemplates.hasAddGeometryColumn()) {
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
        if (sqlTemplates.hasAddGeometryColumn()) {
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
        if (sqlTemplates.hasAddGeometryColumn()) {
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
        if (sqlTemplates.hasAddGeometryColumn()) {
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
        return new AbstractAction("Quick view other table"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    String[] tableColsFromFK = column.tableColsFromFK();
                    String refTable = tableColsFromFK[0];
                    QueryResult queryResult = spatialiteViewer.currentConnectedDatabase.getTableRecordsMapIn(refTable, null, true,
                            20, -1);
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
                        databaseViewer.currentConnectedDatabase = null;
                        logger.insertError("SqlTemplatesAndActions", "Error refreshing database...", ex);
                    } finally {
                        logConsole.finishProcess();
                        logConsole.stopLogging();
                        logConsole.setVisible(false);
                        window.dispose();
                    }
                }).start();
            }
        };
    }

    public Action getCopyDatabasePathAction( DatabaseViewer spatialiteViewer ) {
        return new AbstractAction("Copy path"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                String databasePath = spatialiteViewer.currentConnectedDatabase.getDatabasePath();
                GuiUtilities.copyToClipboard(databasePath);
            }
        };
    }

    public Action getCreateTableFromShapefileSchemaAction( GuiBridgeHandler guiBridge, DatabaseViewer spatialiteViewer ) {
        return new AbstractAction("Create table from shapefile"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                File[] openFiles = guiBridge.showOpenFileDialog("Open shapefile", GuiUtilities.getLastFile(),
                        HMConstants.vectorFileFilter);
                if (openFiles != null && openFiles.length > 0) {
                    try {
                        GuiUtilities.setLastPath(openFiles[0].getAbsolutePath());
                    } catch (Exception e1) {
                        logger.insertError("SqlTemplatesAndActions", "ERROR", e1);
                    }
                } else {
                    return;
                }
                try {
                    SpatialDbsImportUtils.createTableFromShp(spatialiteViewer.currentConnectedDatabase, openFiles[0]);
                    spatialiteViewer.refreshDatabaseTree();
                } catch (Exception e1) {
                    logger.insertError("SqlTemplatesAndActions", "ERROR", e1);
                }

            }
        };
    }

    public Action getAttachShapefileAction( GuiBridgeHandler guiBridge, DatabaseViewer spatialiteViewer ) {
        return new AbstractAction("Attach readonly shapefile"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                File[] openFiles = guiBridge.showOpenFileDialog("Open shapefile", GuiUtilities.getLastFile(),
                        HMConstants.vectorFileFilter);
                if (openFiles != null && openFiles.length > 0) {
                    try {
                        GuiUtilities.setLastPath(openFiles[0].getAbsolutePath());
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
    }

    public Action getSelectAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        return new AbstractAction("Select statement"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    String query = DbsUtilities.getSelectQuery(spatialiteViewer.currentConnectedDatabase, table, false);
                    spatialiteViewer.addTextToQueryEditor(query);
                } catch (Exception e1) {
                    logger.insertError("SqlTemplatesAndActions", "Error", e1);
                }
            }
        };
    }

    public Action getDropAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
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
                    String tableName = table.tableName;
                    long count = spatialiteViewer.currentConnectedDatabase.getCount(tableName);
                    JOptionPane.showMessageDialog(spatialiteViewer, "Count: " + count);
                } catch (Exception ex) {
                    logger.insertError("SqlTemplatesAndActions", "Error", ex);
                }
            }
        };
    }

    public Action getImportShapefileDataAction( GuiBridgeHandler guiBridge, TableLevel table, DatabaseViewer spatialiteViewer ) {
        return new AbstractAction("Import data from shapefile"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                File[] openFiles = guiBridge.showOpenFileDialog("Open shapefile", GuiUtilities.getLastFile(),
                        HMConstants.vectorFileFilter);
                if (openFiles != null && openFiles.length > 0) {
                    try {
                        GuiUtilities.setLastPath(openFiles[0].getAbsolutePath());
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
                        hasErrors = !SpatialDbsImportUtils.importShapefile(spatialiteViewer.currentConnectedDatabase,
                                openFiles[0], spatialiteViewer.currentSelectedTable.tableName, -1, spatialiteViewer.pm);
                    } catch (Exception ex) {
                        logger.insertError("SqlTemplatesAndActions", "Error importing data from shapefile", ex);
                    } finally {
                        logConsole.finishProcess();
                        logConsole.stopLogging();
                        if (!hasErrors) {
                            logConsole.setVisible(false);
                            window.dispose();
                        }
                    }
                }).start();
            }
        };
    }

    public Action getReprojectTableAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
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

                    String query = sqlTemplates.reprojectTable(table, spatialiteViewer.currentConnectedDatabase, geometryColumn,
                            tableName, newTableName, newSrid);

                    spatialiteViewer.addTextToQueryEditor(query);
                } catch (Exception ex) {
                    logger.insertError("SqlTemplatesAndActions", "Error", ex);
                }
            }
        };
    }

    public Action getQuickViewTableAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        return new AbstractAction("Quick View Table in 3D"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    String query = DbsUtilities.getSelectQuery(spatialiteViewer.currentConnectedDatabase, table, false);
                    spatialiteViewer.viewSpatialQueryResult3D(table.tableName, query, spatialiteViewer.pm);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    public Action getQuickViewTableGeometriesAction( TableLevel table, DatabaseViewer spatialiteViewer ) {
        return new AbstractAction("Quick View Table Geometries"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    String query = DbsUtilities.getSelectQuery(spatialiteViewer.currentConnectedDatabase, table, false);
                    spatialiteViewer.viewSpatialQueryResult(table.tableName, query, spatialiteViewer.pm);
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        };
    }

    public Action getUpdateLayerStats( GuiBridgeHandler guiBridge, DatabaseViewer spatialiteViewer ) {
        if (sqlTemplates.hasAddGeometryColumn()) {
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
        return new AbstractAction("Import sql file"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                File[] openFiles = guiBridge.showOpenFileDialog("Open sql file", GuiUtilities.getLastFile(), null);
                if (openFiles != null && openFiles.length > 0) {
                    try {
                        GuiUtilities.setLastPath(openFiles[0].getAbsolutePath());
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
                }).start();

            }
        };
    }
}
