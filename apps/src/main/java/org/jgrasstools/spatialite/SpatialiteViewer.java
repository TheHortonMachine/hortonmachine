/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.spatialite;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;

import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.dbs.spatialite.objects.ColumnLevel;
import org.jgrasstools.dbs.spatialite.objects.DbLevel;
import org.jgrasstools.dbs.spatialite.objects.TableLevel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gui.console.LogConsoleController;
import org.jgrasstools.gui.utils.DefaultGuiBridgeImpl;
import org.jgrasstools.gui.utils.GuiBridgeHandler;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.jgrasstools.gui.utils.GuiUtilities.IOnCloseListener;
import org.jgrasstools.gui.utils.ImageCache;
import org.jgrasstools.nww.SimpleNwwViewer;
import org.jgrasstools.nww.gui.ToolsPanelController;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The spatialtoolbox view controller.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SpatialiteViewer extends SpatialiteController implements IOnCloseListener {
    private static final Logger logger = LoggerFactory.getLogger(SpatialiteViewer.class);
    private static final long serialVersionUID = 1L;
    private ToolsPanelController toolsPanelController;

    public SpatialiteViewer( GuiBridgeHandler guiBridge ) {
        super(guiBridge);
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
                    hadErrors = viewSpatialQueryResult(sqlText, pm);
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
            }).start();
        });
    }

    boolean viewSpatialQueryResult( String sqlText, IJGTProgressMonitor pm ) {
        boolean hasError = false;
        if (currentConnectedDatabase != null && sqlText.length() > 0) {
            try {
                pm.beginTask("Run query: " + sqlText, IJGTProgressMonitor.UNKNOWN);
                DefaultFeatureCollection fc = currentConnectedDatabase.runRawSqlToFeatureCollection(sqlText);
                ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(fc, NwwUtilities.GPS_CRS);
                if (toolsPanelController == null)
                    toolsPanelController = SimpleNwwViewer.openNww(null, JFrame.DO_NOTHING_ON_CLOSE);

                if (toolsPanelController != null) {
                    toolsPanelController.loadFeatureCollection(null, "QueryLayer", null, rfc, null);
                    addQueryToHistoryCombo(sqlText);
                }

            } catch (Exception e1) {
                String localizedMessage = e1.getLocalizedMessage();
                hasError = true;
                pm.errorMessage("An error occurred: " + localizedMessage);
            } finally {
                pm.done();
            }
        }
        return hasError;
    }

    protected List<Action> makeColumnActions( final ColumnLevel selectedColumn ) {

        List<Action> actions = new ArrayList<>();
        actions.add(SqlTemplatesAndActions.getSelectOnColumnAction(selectedColumn, this));
        actions.add(SqlTemplatesAndActions.getUpdateOnColumnAction(selectedColumn, this));
        actions.add(null);
        actions.add(SqlTemplatesAndActions.getAddGeometryAction(selectedColumn, this));
        actions.add(SqlTemplatesAndActions.getRecoverGeometryAction(selectedColumn, this));

        /*
         * geometry bound stuff
         */

        if (selectedColumn.geomColumn != null) {
            actions.add(SqlTemplatesAndActions.getDiscardGeometryColumnAction(selectedColumn, this));
            actions.add(null);
            actions.add(SqlTemplatesAndActions.getCreateSpatialIndexAction(selectedColumn, this));
            actions.add(SqlTemplatesAndActions.getCheckSpatialIndexAction(selectedColumn, this));
            actions.add(SqlTemplatesAndActions.getRecoverSpatialIndexAction(selectedColumn, this));
            actions.add(SqlTemplatesAndActions.getDisableSpatialIndexAction(selectedColumn, this));
            actions.add(null);
            actions.add(SqlTemplatesAndActions.getShowSpatialMetadataAction(selectedColumn, this));
        }
        /*
         * FK key bound stuff
         */
        if (selectedColumn.references != null) {
            actions.add(null);
            actions.add(SqlTemplatesAndActions.getCombinedSelectAction(selectedColumn, this));
            actions.add(SqlTemplatesAndActions.getQuickViewOtherTableAction(selectedColumn, this));
        }

        return actions;
    }

    protected List<Action> makeDatabaseAction( final DbLevel dbLevel ) {
        List<Action> actions = new ArrayList<>();
        actions.add(SqlTemplatesAndActions.getRefreshDatabaseAction(guiBridge, this));
        actions.add(null);
        actions.add(SqlTemplatesAndActions.getCopyDatabasePathAction(this));
        actions.add(null);
        actions.add(SqlTemplatesAndActions.getCreateTableFromShapefileSchemaAction(guiBridge, this));
        actions.add(null);
        actions.add(SqlTemplatesAndActions.getUpdateLayerStats(guiBridge, this));
        return actions;
    }

    protected List<Action> makeTableAction( final TableLevel selectedTable ) {
        List<Action> actions = new ArrayList<>();
        actions.add(SqlTemplatesAndActions.getCountRowsAction(selectedTable, this));
        actions.add(null);
        actions.add(SqlTemplatesAndActions.getSelectAction(selectedTable, this));
        actions.add(SqlTemplatesAndActions.getDropAction(selectedTable, this));
        actions.add(null);
        actions.add(SqlTemplatesAndActions.getReprojectTableAction(selectedTable, this));
        actions.add(null);
        if (selectedTable.isGeo) {
            actions.add(SqlTemplatesAndActions.getImportShapefileDataAction(guiBridge, selectedTable, this));
            actions.add(SqlTemplatesAndActions.getQuickViewTableAction(selectedTable, this));
        }

        return actions;
    }

    public static void main( String[] args ) throws Exception {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final SpatialiteViewer controller = new SpatialiteViewer(gBridge);
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "JGrasstools' Spatialite Viewer");

        Class<SpatialiteViewer> class1 = SpatialiteViewer.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/jgrasstools/images/hm150.png"));
        frame.setIconImage(icon.getImage());

        GuiUtilities.addClosingListener(frame, controller);
    }

}
