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

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextPane;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.styling.Style;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.DbLevel;
import org.hortonmachine.dbs.compat.objects.LeafLevel;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.log.EMessageType;
import org.hortonmachine.dbs.log.LogDb;
import org.hortonmachine.dbs.log.Message;
import org.hortonmachine.dbs.spatialite.SpatialiteCommonMethods;
import org.hortonmachine.gears.io.dbs.DbsHelper;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.simplereport.HtmlReport;
import org.hortonmachine.gui.console.LogConsoleController;
import org.hortonmachine.gui.settings.SettingsController;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiBridgeHandler;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.SimpleNwwViewer;
import org.hortonmachine.nww.gui.LayersPanelController;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.ToolsPanelController;
import org.hortonmachine.nww.gui.ViewControlsLayer;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.joda.time.DateTime;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.util.Logging;
import gov.nasa.worldwind.util.WWUtil;

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
            DefaultFeatureCollection fc = DbsHelper.runRawSqlToFeatureCollection(tableName,
                    (ASpatialDb) currentConnectedSqlDatabase, sqlText, null);
            ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(fc, NwwUtilities.GPS_CRS);

            Style[] styles = null;
            if (isTableQuery && tableName != null) {
                // try to get some style
                if (currentConnectedSqlDatabase.getType() == EDb.GEOPACKAGE) {
                    String sldString = ((GeopackageCommonDb) currentConnectedSqlDatabase).getSldString(tableName);
                    if (sldString != null) {
                        Style style = SldUtilities.getStyleFromSldString(sldString);
                        styles = new Style[]{style};
                    }
                }
            }

            showInMapFrame(true, new SimpleFeatureCollection[]{rfc}, styles);

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

    private void openNww() {
        String appName = "HortonMachine Database Viewer";
        try {
            Class<SimpleNwwViewer> class1 = SimpleNwwViewer.class;
            ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));

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
            throw e;
        }
    }

    protected List<Action> makeLeafActions( final LeafLevel selectedLeaf ) {
        List<Action> actions = new ArrayList<>();
        addIfNotNull(actions, sqlTemplatesAndActions.getUpdateValueAction(guiBridge, this));
        
        return actions;
    }
    
    
    protected List<Action> makeColumnActions( final ColumnLevel selectedColumn ) {

        List<Action> actions = new ArrayList<>();
        addIfNotNull(actions, sqlTemplatesAndActions.getSelectOnColumnAction(selectedColumn, this));
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
        addIfNotNull(actions, sqlTemplatesAndActions.getSwitchDatabaseAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getDropDatabaseAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getViewActiveSessionsAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getCleanIdleSessionsAction(guiBridge, this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getCopyDatabasePathAction(this));
        addIfNotNull(actions, sqlTemplatesAndActions.getSaveConnectionAction(this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getImportSqlFileAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getNewCollectionAction(guiBridge, this));
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
        return actions;
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
        addIfNotNull(actions, sqlTemplatesAndActions.getInsertCollectionDocumentAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getUpdateCollectionDocumentAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getDeleteCollectionDocumentByIdAction(guiBridge, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getSelectAction(selectedTable, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getInsertAction(selectedTable, this));
        addIfNotNull(actions, sqlTemplatesAndActions.getGenerateInsertExportAction(selectedTable, this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getDropAction(selectedTable, this));
        addSeparator(actions);
        addIfNotNull(actions, sqlTemplatesAndActions.getReprojectTableAction(selectedTable, this));
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
        if (selectedTable.tableName.equals(LogDb.TABLE_MESSAGES)) {
            actions.add(new AbstractAction("Show HTML report"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    try {
                        String sql = "select  " + LogDb.type_NAME + ", " + LogDb.TimeStamp_NAME + ", " + LogDb.tag_NAME + ","
                                + LogDb.message_NAME + " from " + LogDb.TABLE_MESSAGES + " order by " + LogDb.TimeStamp_NAME
                                + " desc";

                        LinkedHashMap<String, List<Message>> day2MessageMap = new LinkedHashMap<>();
                        currentConnectedSqlDatabase.execOnConnection(connection -> {
                            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                                while( rs.next() ) {
                                    int type = rs.getInt(1);
                                    long ts = rs.getLong(2);
                                    String tag = rs.getString(3);
                                    if (tag == null)
                                        tag = "";
                                    String msg = rs.getString(4);
                                    String tsString = new DateTime(ts).toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS);
                                    String[] split = tsString.split(":");
                                    String dayHour = split[0];

                                    Message logMsg = new Message();
                                    logMsg.tag = tag;
                                    logMsg.msg = msg;
                                    logMsg.ts = ts;
                                    logMsg.type = type;

                                    List<Message> messages = day2MessageMap.get(dayHour);
                                    if (messages == null) {
                                        messages = new ArrayList<Message>();
                                        day2MessageMap.put(dayHour, messages);
                                    }
                                    messages.add(logMsg);
                                }
                                return "";
                            }
                        });

                        HtmlReport rep = new HtmlReport();
                        StringBuilder sb = new StringBuilder();
                        rep.openReport(sb, "Log Messages");
                        rep.openTable(sb, 98);
                        String white = "#FFFFFF";
                        String warning = "#ffb380";
                        String debug = "#afe9af";
                        String error = "#ff5555";
                        String header = "#e6e6e6";
                        String oddRow = "#f2f2f2";
                        String evenRow = "#d5f9fe";

                        rep.openTableRow(sb);
                        rep.openTableCell(sb, header, "11", null, null);
                        sb.append("DAY + HOUR ");
                        rep.closeTableCell(sb);

                        rep.openTableCell(sb, header, "6", null, null);
                        sb.append("MIN:SEC");
                        rep.closeTableCell(sb);

                        rep.openTableCell(sb, header, "13", null, null);
                        sb.append("TAG");
                        rep.closeTableCell(sb);

                        rep.openTableCell(sb, header, "80", null, null);
                        sb.append("MESSAGE");
                        rep.closeTableCell(sb);

                        rep.closeTableRow(sb);

                        boolean odd = false;
                        for( Entry<String, List<Message>> entry : day2MessageMap.entrySet() ) {
                            String day = entry.getKey();
                            List<Message> msgList = entry.getValue();

                            rep.openTableRow(sb);

                            String firstColor = evenRow;
                            if (odd) {
                                firstColor = oddRow;
                            }
                            odd = !odd;

                            int rowSpan = msgList.size() + 1;
                            rep.openTableCell(sb, firstColor, "11", null, rowSpan + "");
                            sb.append(day);
                            rep.closeTableCell(sb);

                            rep.closeTableRow(sb);

                            for( Message message : msgList ) {
                                String color = white;
                                if (message.type == EMessageType.DEBUG.getCode()) {
                                    color = debug;
                                } else if (message.type == EMessageType.WARNING.getCode()) {
                                    color = warning;
                                } else if (message.type == EMessageType.ERROR.getCode()) {
                                    color = error;
                                }
                                String tsString = new DateTime(message.ts).toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS);
                                String[] split = tsString.split(":");
                                String time = split[1] + ":" + split[2];

                                rep.openTableRow(sb);

                                rep.openTableCell(sb, color, "6", null, null);
                                sb.append(time);
                                rep.closeTableCell(sb);

                                if (message.tag.length() == 0) {
                                    rep.openTableCell(sb, color, "80", "2", null);
                                    sb.append("<pre>").append(message.msg).append("</pre>");
                                    rep.closeTableCell(sb);
                                } else {
                                    rep.openTableCell(sb, color, "13", null, null);
                                    sb.append("<b>").append(message.tag).append("<b>");
                                    rep.closeTableCell(sb);

                                    rep.openTableCell(sb, color, "70", null, null);
                                    sb.append("<pre>").append(message.msg).append("</pre>");
                                    rep.closeTableCell(sb);
                                }

                                rep.closeTableRow(sb);
                            }

                        }

                        rep.closeTable(sb);
                        rep.closeReport(sb);

                        File tmpFile = File.createTempFile("HM-", "_debug.html");
                        FileUtilities.writeFile(sb.toString(), tmpFile);
                        GuiUtilities.openFile(tmpFile);

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            });
        }

        return actions;
    }

    @Override
    public void onClose() {
        SettingsController.onCloseHandleSettings();
        super.onClose();
    }

    public static void main( String[] args ) throws Exception {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final DatabaseViewer controller = new DatabaseViewer(gBridge);
        SettingsController.applySettings(controller);
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Database Viewer");

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
