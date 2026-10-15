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

import java.awt.Dimension;
import java.awt.Window;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;

import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.DbLevel;
import org.hortonmachine.dbs.compat.objects.LeafLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.log.LogDb;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.postgis.PGDb;
import org.hortonmachine.dbs.spatialite.SpatialiteCommonMethods;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.TimeseriesTableUtils;
import org.hortonmachine.database.csv.ImportCsvAsNewTableAction;
import org.hortonmachine.database.csv.ImportCsvIntoTableAction;
import org.hortonmachine.gears.io.dbs.DbsHelper;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gui.console.LogConsoleController;
import org.hortonmachine.gui.settings.SettingsController;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiBridgeHandler;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.SimpleNwwViewer;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.ToolsPanelController;
import org.hortonmachine.nww.utils.NwwUtilities;

import gov.nasa.worldwind.geom.Sector;

/**
 * The database viewer.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class DatabaseViewer extends DatabaseController {
    // private static final Logger logger = LoggerFactory.getLogger(SpatialiteViewer.class);
    private static final long serialVersionUID = 1L;
    private ToolsPanelController toolsPanelController;

    public DatabaseViewer( GuiBridgeHandler guiBridge ) {
        super(guiBridge);
        // makes the viewer sessions identifiable in pg_stat_activity
        PGDb.setApplicationName("HortonMachine DbViewer");
    }

    protected void setViewQueryButton( JButton _viewQueryButton, Dimension preferredButtonSize, JTextPane sqlEditorArea ) {
        _viewQueryButton.setIcon(ImageCache.getInstance().getImage(ImageCache.GLOBE));
        _viewQueryButton.setToolTipText(VIEW_QUERY_TOOLTIP);
        _viewQueryButton.setText("");
        _viewQueryButton.setPreferredSize(preferredButtonSize);
        _viewQueryButton.addActionListener(e -> {

            String sqlText = sqlEditorArea.getText().trim();
            if (sqlText.length() > 0) {
                if (!sqlText.toLowerCase().startsWith("select")) {
                    JOptionPane.showMessageDialog(this, "Viewing of data is allowed only for SELECT statements.", "WARNING",
                            JOptionPane.WARNING_MESSAGE, null);
                    return;
                }
            }

            final LogConsoleController logConsole = new LogConsoleController(pm);
            JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");
            new Thread(() -> {
                boolean hadErrors = false;
                try {
                    logConsole.beginProcess("Run query");
                    hadErrors = viewSpatialQueryResult(null, sqlText, pm, false);
                } catch (Exception ex) {
                    pm.errorMessage(ex.getLocalizedMessage());
                    hadErrors = true;
                } finally {
                    logConsole.finishProcess();
                    logConsole.stopLogging();
                    if (!hadErrors) {
                        logConsole.setVisible(false);
                        window.dispose();
                    }
                }
            }, "DatabaseViewer->run query and view geometries").start();
        });
    }

    public boolean viewSpatialQueryResult3D( String title, String sqlText, IHMProgressMonitor pm ) {
        boolean hasError = false;
        if (sqlText.trim().length() == 0) {
            return false;
        }
        try {
            if (toolsPanelController == null) {
                openNww();
            }
            pm.beginTask("Run query: " + sqlText, IHMProgressMonitor.UNKNOWN);
            DefaultFeatureCollection fc = DbsHelper.runRawSqlToFeatureCollection(title, (ASpatialDb) currentConnectedSqlDatabase,
                    sqlText, null);
            ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(fc, NwwUtilities.GPS_CRS);

            if (toolsPanelController != null) {
                if (title == null) {
                    title = "QueryLayer";
                }
                toolsPanelController.loadFeatureCollection(null, title, null, rfc, null);
                zoomNwwTo(fc.getBounds());
                addQueryToHistoryCombo(sqlText);
            }

        } catch (Exception e1) {
            String localizedMessage = e1.getLocalizedMessage();
            hasError = true;
            e1.printStackTrace();
            pm.errorMessage("An error occurred: " + localizedMessage);
        } finally {
            pm.done();
        }
        return hasError;
    }

    public boolean viewSpatialQueryResult( String tableName, String sqlText, IHMProgressMonitor pm, boolean isTableQuery ) {
        boolean hasError = false;
        if (sqlText.trim().length() == 0) {
            return false;
        }
        try {
            pm.beginTask("Run query: " + sqlText, IHMProgressMonitor.UNKNOWN);
            // no reprojection needed, the map reprojects the layers to its CRS while rendering
            DefaultFeatureCollection fc = DbsHelper.runRawSqlToFeatureCollection(tableName,
                    (ASpatialDb) currentConnectedSqlDatabase, sqlText, null);

            Style[] styles = null;
            if (isTableQuery && tableName != null) {
                // try to get some style
                if (currentConnectedSqlDatabase.getType() == EDb.GEOPACKAGE) {
                    String sldString = ((GeopackageCommonDb) currentConnectedSqlDatabase).getSldString(SqlName.m(tableName));
                    if (sldString != null) {
                        Style style = SldUtilities.getStyleFromSldString(sldString);
                        styles = new Style[]{style};
                    }
                }
            }

            showInMapFrame(true, new SimpleFeatureCollection[]{fc}, styles);

            addQueryToHistoryCombo(sqlText);

        } catch (Exception e1) {
            String localizedMessage = e1.getLocalizedMessage();
            hasError = true;
            pm.errorMessage("An error occurred: " + localizedMessage);
        } finally {
            pm.done();
        }
        return hasError;
    }

    @Override
    protected boolean canShowGeometriesIn3D() {
        return true;
    }

    @Override
    protected void showGeometriesIn3D( String title, List<SimpleFeatureCollection> fcs ) throws Exception {
        if (toolsPanelController == null) {
            openNww();
        }
        if (toolsPanelController == null) {
            return;
        }
        ReferencedEnvelope bounds = null;
        for( SimpleFeatureCollection fc : fcs ) {
            ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(fc, NwwUtilities.GPS_CRS);
            toolsPanelController.loadFeatureCollection(null, title, null, rfc, null);
            ReferencedEnvelope fcBounds = fc.getBounds().transform(NwwUtilities.GPS_CRS, true);
            if (bounds == null) {
                bounds = fcBounds;
            } else {
                bounds.expandToInclude(fcBounds);
            }
        }
        zoomNwwTo(bounds);
    }

    /**
     * Zoom the 3D view to the given bounds.
     *
     * <p>The bounds are transformed from the source collections: the bounds of a reprojecting
     * collection iterate the features, which is not safe while the layer is loading them in
     * its own thread.</p>
     */
    private void zoomNwwTo( ReferencedEnvelope bounds ) {
        if (toolsPanelController == null || bounds == null || bounds.isEmpty()) {
            return;
        }
        try {
            ReferencedEnvelope llBounds = bounds.transform(NwwUtilities.GPS_CRS, true);
            // keep a minimum size, a single point would otherwise zoom to the ground
            double minHalfSize = 0.001;
            double halfWidth = Math.max(llBounds.getWidth() / 2.0, minHalfSize);
            double halfHeight = Math.max(llBounds.getHeight() / 2.0, minHalfSize);
            double centerX = llBounds.getMedian(0);
            double centerY = llBounds.getMedian(1);
            Sector sector = Sector.fromDegrees(centerY - halfHeight, centerY + halfHeight, centerX - halfWidth,
                    centerX + halfWidth);
            NwwPanel wwjPanel = toolsPanelController.getWwjPanel();
            // after a new window has been shown
            SwingUtilities.invokeLater(() -> wwjPanel.goTo(sector, true));
        } catch (Exception e) {
            Logger.INSTANCE.insertError("", "Unable to zoom to the geometries", e);
        }
    }

    /**
     * Open the 3D viewer as the NWW viewer application does: a single window with layers
     * and tools panels and the background maps to switch between.
     */
    private void openNww() {
        toolsPanelController = SimpleNwwViewer.openNww("HortonMachine Database Viewer", JFrame.DISPOSE_ON_CLOSE);
        if (toolsPanelController == null) {
            return;
        }
        Window nwwWindow = SwingUtilities.getWindowAncestor(toolsPanelController);
        if (nwwWindow != null) {
            nwwWindow.addWindowListener(new WindowAdapter(){
                @Override
                public void windowClosed( WindowEvent e ) {
                    toolsPanelController = null;
                }
            });
        }
    }

    protected List<Action> makeLeafActions( final LeafLevel selectedLeaf ) {
        List<Action> actions = new ArrayList<>();
        addIfNotNull(actions, sqlTemplatesAndActions.getUpdateValueAction(guiBridge, this));

        appendSpiActions(actions, getSpiLeafActions(selectedLeaf));
        return actions;
    }

    private void appendSpiActions( List<Action> actions, List<Action> spiActions ) {
        if (spiActions.isEmpty()) {
            return;
        }
        if (!actions.isEmpty()) {
            addSeparator(actions);
        }
        actions.addAll(spiActions);
    }

    protected List<Action> makeColumnActions( final ColumnLevel selectedColumn ) {

        List<Action> actions = new ArrayList<>();
        addIfNotNull(actions, sqlTemplatesAndActions.getSelectOnColumnAction(selectedColumn, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getSelectOrderedAscOnColumnAction(selectedColumn, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getSelectOrderedDescOnColumnAction(selectedColumn, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getSelectGroupCountOnColumnAction(selectedColumn, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getUpdateOnColumnAction(selectedColumn, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getUpdateValueAction(guiBridge, this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getAddGeometryAction(selectedColumn, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getRecoverGeometryAction(selectedColumn, this));

        /*
         * geometry bound stuff
         */

        if (selectedColumn.geomColumn != null) {
            addIfNotNull(actions, sqlTemplatesAndActions.getDiscardGeometryColumnAction(selectedColumn, this));
            addSeparator(actions);
            addIfNotNull(actions, sqlTemplatesAndActions.getCreateSpatialIndexAction(selectedColumn, this));
            addIfNotNull(actions, sqlTemplatesAndActions.getCheckSpatialIndexAction(selectedColumn, this));
            addIfNotNull(actions, sqlTemplatesAndActions.getRecoverSpatialIndexAction(selectedColumn, this));
            addIfNotNull(actions, sqlTemplatesAndActions.getDisableSpatialIndexAction(selectedColumn, this));
            addSeparator(actions);
            addIfNotNull(actions, sqlTemplatesAndActions.getShowSpatialMetadataAction(selectedColumn, this));
        }
        /*
         * FK key bound stuff
         */
        if (selectedColumn.references != null) {
            addSeparator(actions);
            addIfNotNull(actions, sqlTemplatesAndActions.getCombinedSelectAction(selectedColumn, this));
            addIfNotNull(actions, sqlTemplatesAndActions.getQuickViewOtherTableAction(selectedColumn, this));
        }

        appendSpiActions(actions, getSpiColumnActions(selectedColumn));
        return actions;
    }

    private void addIfNotNull( List<Action> actions, Action selectOnColumnAction ) {
        if (selectOnColumnAction != null) {
            actions.add(selectOnColumnAction);
        }
    }

    protected List<Action> makeDatabaseAction( final DbLevel dbLevel ) {
        List<Action> actions = new ArrayList<>();
        addIfNotNull(actions, sqlTemplatesAndActions.getRefreshDatabaseAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getReconnectDatabaseAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getSwitchDatabaseAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getDropDatabaseAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getViewActiveSessionsAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getCleanIdleSessionsAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getBlockingSessionsAction(this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getCopyDatabasePathAction(this));
        addIfNotNull(actions, sqlTemplatesAndActions.getSaveConnectionAction(this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getImportSqlFileAction(guiBridge, this));
        if (currentConnectedSqlDatabase instanceof ASpatialDb) {
            addSeparator(actions);
            addIfNotNull(actions, sqlTemplatesAndActions.getCreateTableFromShapefileSchemaAction(guiBridge, this));
            addIfNotNull(actions, sqlTemplatesAndActions.getAttachShapefileAction(guiBridge, this));
            addSeparator(actions);
            addIfNotNull(actions, sqlTemplatesAndActions.getUpdateLayerStats(guiBridge, this));
            addSeparator(actions);
            addIfNotNull(actions, sqlTemplatesAndActions.getImportRaster2TilesTableAction(guiBridge, this));
            addIfNotNull(actions, sqlTemplatesAndActions.getImportVector2TilesTableAction(guiBridge, this));
        }
        addSeparator(actions);
        actions.add(new ImportCsvAsNewTableAction(currentConnectedSqlDatabase, guiBridge, pm, this, this::refreshDatabaseTreeQuietly));
        appendSpiActions(actions, getSpiDatabaseActions(dbLevel));
        return actions;
    }

    private void refreshDatabaseTreeQuietly() {
        try {
            refreshDatabaseTree();
        } catch (Exception ex) {
            GuiUtilities.handleError(this, ex);
        }
    }

    private void addSeparator( List<Action> actions ) {
        int size = actions.size();
        if (actions.get(size - 1) != null) {
            actions.add(null);
        }
    }

    @SuppressWarnings("serial")
    protected List<Action> makeTableAction( final TableLevel selectedTable ) {
        List<Action> actions = new ArrayList<>();
        addIfNotNull(actions, sqlTemplatesAndActions.getCountRowsAction(selectedTable, this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getSelectAction(selectedTable, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getInsertAction(selectedTable, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getGenerateInsertExportAction(selectedTable, this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getDropAction(selectedTable, this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getReprojectTableAction(selectedTable, this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getTableHealthAction(selectedTable, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getVacuumAnalyzeAction(selectedTable, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getVacuumFullAction(selectedTable, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getBlockingSessionsAction(this));
        addSeparator(actions);
        if (selectedTable.isGeo) {
            EDb type = currentConnectedSqlDatabase.getType();
            boolean useToWktBridge = true;
            if (type == EDb.GEOPACKAGE) {
                useToWktBridge = false;
            }
            addIfNotNull(actions,
                    sqlTemplatesAndActions.getImportShapefileDataAction(guiBridge, selectedTable, this, useToWktBridge));
            addIfNotNull(actions, sqlTemplatesAndActions.getQuickViewTableAction(selectedTable, this));
            addIfNotNull(actions, sqlTemplatesAndActions.getQuickViewTableGeometriesAction(selectedTable, this));
            addIfNotNull(actions, sqlTemplatesAndActions.getOpenInSldEditorAction(selectedTable, this));
            addIfNotNull(actions, sqlTemplatesAndActions.getOpenInGformsEditorAction(selectedTable, this));
        }
        boolean isSmash = selectedTable.tableName.equals("debug");
        if (selectedTable.tableName.equals(LogDb.TABLE_MESSAGES) || isSmash) {
            actions.add(new LogMessagesHtmlReportAction(currentConnectedSqlDatabase, isSmash));
        }

        addSeparator(actions);
        if (TimeseriesTableUtils.isTimeseriesTable(selectedTable)) {
            actions.add(new ImportCsvIntoTableAction("Import timeseries from CSV", currentConnectedSqlDatabase, selectedTable,
                    guiBridge, pm, this, this::refreshDatabaseTreeQuietly));
        }
        actions.add(new ImportCsvIntoTableAction("Import CSV into table", currentConnectedSqlDatabase, selectedTable, guiBridge,
                pm, this, this::refreshDatabaseTreeQuietly));

        appendSpiActions(actions, getSpiTableActions(selectedTable));

        return actions;
    }

    @Override
    public void onClose() {
        SettingsController.onCloseHandleSettings();
        super.onClose();
    }

    public static void openInNewWindow( ConnectionData connectionData ) {
        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final DatabaseViewer controller = new DatabaseViewer(gBridge);
        SettingsController.applySettings(controller);
        String title = "HortonMachine Database Viewer - " + connectionData.connectionLabel;
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), title);
        GuiUtilities.setDefaultFrameIcon(frame);
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosing( WindowEvent e ) {
                controller.onClose();
                frame.dispose();
            }
        });
        controller.openDatabase(connectionData, false);
    }

    public static void main( String[] args ) throws Exception {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final DatabaseViewer controller = new DatabaseViewer(gBridge);
        SettingsController.applySettings(controller);
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Database Viewer - " + org.hortonmachine.Version.getVersion());

        GuiUtilities.setDefaultFrameIcon(frame);
        GuiUtilities.addClosingListener(frame, controller);

        File openFile = null;
        if (args.length > 0 && new File(args[0]).exists()) {
            openFile = new File(args[0]);
        } else {
            String lastPath = PreferencesHandler.getPreference(DatabaseGuiUtils.HM_SPATIALITE_LAST_FILE, (String) null);
            if (lastPath != null) {
                File tmp = new File(lastPath);
                if (tmp.exists()) {
                    openFile = tmp;
                }
            }
        }

        if (openFile != null) {
            String absolutePath = openFile.getAbsolutePath();
            EDb dbType = null;
            if (SpatialiteCommonMethods.isSqliteFile(openFile)) {
                if (absolutePath.endsWith(EDb.GEOPACKAGE.getExtension())) {
                    dbType = EDb.GEOPACKAGE;
                } else {
                    dbType = EDb.SPATIALITE;
                }
            } else if (absolutePath.endsWith(EDb.H2GIS.getExtension())) {
                dbType = EDb.H2GIS;
            } else {
                absolutePath = null;
            }

            controller.openDatabase(dbType, absolutePath, null, null);
        }
    }

}
