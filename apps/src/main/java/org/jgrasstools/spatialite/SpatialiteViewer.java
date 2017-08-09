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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.dbs.compat.objects.ColumnLevel;
import org.jgrasstools.dbs.compat.objects.DbLevel;
import org.jgrasstools.dbs.compat.objects.TableLevel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.spatialite.GTSpatialiteThreadsafeDb;
import org.jgrasstools.gui.console.LogConsoleController;
import org.jgrasstools.gui.utils.DefaultGuiBridgeImpl;
import org.jgrasstools.gui.utils.GuiBridgeHandler;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.jgrasstools.gui.utils.GuiUtilities.IOnCloseListener;
import org.jgrasstools.gui.utils.ImageCache;
import org.jgrasstools.nww.SimpleNwwViewer;
import org.jgrasstools.nww.gui.LayersPanelController;
import org.jgrasstools.nww.gui.NwwPanel;
import org.jgrasstools.nww.gui.ToolsPanelController;
import org.jgrasstools.nww.gui.ViewControlsLayer;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;

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
                    hadErrors = viewSpatialQueryResult(null, sqlText, pm);
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

    boolean viewSpatialQueryResult( String title, String sqlText, IJGTProgressMonitor pm ) {
        boolean hasError = false;
        if (sqlText.trim().length() == 0) {
            return false;
        }
        if (currentConnectedDatabase instanceof GTSpatialiteThreadsafeDb) {
            try {
                pm.beginTask("Run query: " + sqlText, IJGTProgressMonitor.UNKNOWN);
                DefaultFeatureCollection fc = ((GTSpatialiteThreadsafeDb) currentConnectedDatabase)
                        .runRawSqlToFeatureCollection(sqlText);
                ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(fc, NwwUtilities.GPS_CRS);
                if (toolsPanelController == null) {
                    openNww();
                }

                if (toolsPanelController != null) {
                    if (title == null) {
                        title = "QueryLayer";
                    }
                    toolsPanelController.loadFeatureCollection(null, title, null, rfc, null);
                    addQueryToHistoryCombo(sqlText);
                }

            } catch (Exception e1) {
                String localizedMessage = e1.getLocalizedMessage();
                hasError = true;
                pm.errorMessage("An error occurred: " + localizedMessage);
            } finally {
                pm.done();
            }
        } else {
            guiBridge.messageDialog("WARNING", "This operation is not yet supported for this database type.",
                    JOptionPane.WARNING_MESSAGE);
        }
        return hasError;
    }

    private void openNww() {
        String appName = "JGrasstools Quick Viewer";
        try {
            Class<SimpleNwwViewer> class1 = SimpleNwwViewer.class;
            ImageIcon icon = new ImageIcon(class1.getResource("/org/jgrasstools/images/hm150.png"));

            Component nwwComponent = NwwPanel.createNwwPanel(true);
            NwwPanel wwjPanel = null;
            LayersPanelController layerPanel = null;

            if (nwwComponent instanceof NwwPanel) {
                wwjPanel = (NwwPanel) nwwComponent;
                wwjPanel.addOsmLayer();
                ViewControlsLayer viewControls = wwjPanel.addViewControls();
                viewControls.setScale(1.5);

                layerPanel = new LayersPanelController(wwjPanel);
                toolsPanelController = new ToolsPanelController(wwjPanel, layerPanel);
            }

            final JFrame nwwFrame = new JFrame();
            nwwFrame.setTitle(appName + ": map view");
            nwwFrame.setIconImage(icon.getImage());
            nwwFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            java.awt.EventQueue.invokeLater(() -> nwwFrame.setVisible(true));
            JPanel mapPanel = new JPanel(new BorderLayout());
            mapPanel.add(nwwComponent, BorderLayout.CENTER);
            nwwFrame.getContentPane().add(mapPanel, BorderLayout.CENTER);
            nwwFrame.setResizable(true);
            nwwFrame.setPreferredSize(new Dimension(800, 800));
            nwwFrame.pack();
            WWUtil.alignComponent(null, nwwFrame, AVKey.CENTER);

            JFrame layersFrame = null;
            JFrame toolsFrame = null;
            if (wwjPanel != null) {
                layersFrame = new JFrame();
                layersFrame.setTitle(appName + ": layers view");
                layersFrame.setIconImage(icon.getImage());
                layersFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JFrame _layersFrame = layersFrame;
                java.awt.EventQueue.invokeLater(() -> _layersFrame.setVisible(true));
                layersFrame.getContentPane().add(layerPanel, BorderLayout.CENTER);
                layersFrame.setResizable(true);
                layersFrame.setPreferredSize(new Dimension(400, 500));
                layersFrame.setLocation(0, 0);
                layersFrame.pack();
                toolsFrame = new JFrame();
                toolsFrame.setTitle(appName + ": tools view");
                toolsFrame.setIconImage(icon.getImage());
                toolsFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
                JFrame _toolsFrame = toolsFrame;
                java.awt.EventQueue.invokeLater(() -> _toolsFrame.setVisible(true));
                toolsFrame.getContentPane().add(toolsPanelController, BorderLayout.CENTER);
                toolsFrame.setResizable(true);
                toolsFrame.setPreferredSize(new Dimension(400, 400));
                toolsFrame.setLocation(0, 510);
                toolsFrame.pack();
            }
            JFrame _toolsFrame = toolsFrame;
            JFrame _layersFrame = layersFrame;
            nwwFrame.addWindowListener(new WindowAdapter(){
                public void windowClosed( java.awt.event.WindowEvent e ) {
                    toolsPanelController = null;
                    if (_toolsFrame != null) {
                        _toolsFrame.setVisible(false);
                        _toolsFrame.dispose();
                    }
                    if (_layersFrame != null) {
                        _layersFrame.setVisible(false);
                        _layersFrame.dispose();
                    }

                };
            });
        } catch (Exception e) {
            Logging.logger().log(java.util.logging.Level.SEVERE, "Exception at application start", e);
        }
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
        actions.add(SqlTemplatesAndActions.getImportSqlFileAction(guiBridge, this));
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

        File openFile = null;
        if (args.length > 0 && new File(args[0]).exists()) {
            openFile = new File(args[0]);
        } else {
            String lastPath = GuiUtilities.getPreference(SpatialiteGuiUtils.JGT_SPATIALITE_LAST_FILE, (String) null);
            if (lastPath != null) {
                File tmp = new File(lastPath);
                if (tmp.exists()) {
                    openFile = tmp;
                }
            }
        }

        if (openFile != null) {
            controller.openDatabase(openFile);
        }
    }

}
