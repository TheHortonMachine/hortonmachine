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
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.TransferHandler;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.EventListenerList;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.swing.JMapFrame.Tool;
import org.h2.jdbc.JdbcSQLException;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.ETableType;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.objects.ColumnLevel;
import org.hortonmachine.dbs.compat.objects.DbLevel;
import org.hortonmachine.dbs.compat.objects.ForeignKey;
import org.hortonmachine.dbs.compat.objects.QueryResult;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.compat.objects.TypeLevel;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.spatialite.ESpatialiteGeometryType;
import org.hortonmachine.dbs.spatialite.SpatialiteCommonMethods;
import org.hortonmachine.dbs.spatialite.SpatialiteTableNames;
import org.hortonmachine.dbs.utils.CommonQueries;
import org.hortonmachine.gears.io.dbs.DbsHelper;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;
import org.hortonmachine.gears.spatialite.GTSpatialiteThreadsafeDb;
import org.hortonmachine.gears.ui.HMMapframe;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gui.console.LogConsoleController;
import org.hortonmachine.gui.utils.GuiBridgeHandler;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.ImageCache;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

/**
 * The spatialite/h2gis view controller.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public abstract class DatabaseController extends DatabaseView implements IOnCloseListener {
    private static final long serialVersionUID = 1L;

    // private static final String SHAPEFILE_IMPORT = "import shapefile in selected table";
    // private static final String SHAPEFILE_CCREATE_FROM_SCHEMA = "create table from shapefile
    // schema";
    // private static final String SHAPEFILE_TOOLTIP = "tools to deal with shapefiles";
    // private static final String SHAPEFILE = "shapefile";
    private static final String SQL_TEMPLATES_TOOLTIP = "create a query based on a template";
    private static final String SQL_TEMPLATES = "sql templates";
    private static final String SQL_HISTORY_TOOLTIP = "select queries from the history";
    private static final String SQL_HISTORY = "sql history";
    private static final String DISCONNECT_TOOLTIP = "disconnect from current database";
    private static final String DISCONNECT = "disconnect";
    // private static final String RUN_QUERY = "run query";
    private static final String RUN_QUERY_TOOLTIP = "run the query in the SQL Editor";
    private static final String RUN_QUERY_TO_FILE_TOOLTIP = "run the query in the SQL Editor and store result in file";
    private static final String RUN_QUERY_TO_SHAPEFILE_TOOLTIP = "run the query in the SQL Editor and store result in a shapefile";
    protected static final String VIEW_QUERY_TOOLTIP = "run spatial query and view the result in the 3D viewer";
    // private static final String SQL_EDITOR = "SQL Editor";
    private static final String CLEAR_SQL_EDITOR = "clear SQL editor";
    // private static final String DATA_VIEWER = "Data viewer";
    private static final String DATABASE_CONNECTIONS = "Database connection";
    private static final String NEW = "new";
    private static final String NEW_TOOLTIP = "create a new database";
    private static final String CONNECT = "connect";
    private static final String CONNECT_TOOLTIP = "connect to an existing database";
    private static final String CONNECT_REMOTE = "remote";
    private static final String CONNECT_REMOTE_TOOLTIP = "connect to a remote database";
    private static final String DB_TREE_TITLE = "Database Connection";

    protected GuiBridgeHandler guiBridge;
    protected IHMProgressMonitor pm = new LogProgressMonitor();
    protected ASpatialDb currentConnectedDatabase;
    private DbLevel currentDbLevel;
    protected DbLevel currentSelectedDb;
    protected TableLevel currentSelectedTable;
    protected ColumnLevel currentSelectedColumn;

    private Dimension preferredToolbarButtonSize = new Dimension(120, 65);
    private Dimension preferredSqleditorButtonSize = new Dimension(30, 30);

    private List<String> oldSqlCommands = new ArrayList<String>();
    private JTextPane _sqlEditorArea;
    protected SqlTemplatesAndActions sqlTemplatesAndActions;
    private HMMapframe mapFrame;

    public DatabaseController( GuiBridgeHandler guiBridge ) {
        this.guiBridge = guiBridge;
        setPreferredSize(new Dimension(900, 600));
        init();
    }

    @SuppressWarnings({"serial"})
    private void init() {

        String[] oldSqlCommandsArray = GuiUtilities.getPreference("HM_OLD_SQL_COMMANDS", new String[0]);
        for( String oldSql : oldSqlCommandsArray ) {
            oldSqlCommands.add(oldSql);
        }

        _limitCountTextfield.setText("1000");
        _limitCountTextfield.setToolTipText("1000 is default and used when no valid number is supplied. -1 means no limit.");

        _recordCountTextfield.setEditable(false);

        _dataViewerTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        addDataTableContextMenu();

        _sqlEditorArea = new JTextPane();
        JScrollPane _sqlEditorAreaScrollpane = new JScrollPane(_sqlEditorArea);

        _sqlEditorAreaPanel.setLayout(new BorderLayout());
        _sqlEditorAreaPanel.add(_sqlEditorAreaScrollpane, BorderLayout.CENTER);
        // WrapEditorKit kit = new WrapEditorKit();
        // _sqlEditorArea.setEditorKit(kit);
        SqlDocument doc = new SqlDocument();
        _sqlEditorArea.setDocument(doc);

        _newDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _newDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _newDbButton.setText(NEW);
        _newDbButton.setToolTipText(NEW_TOOLTIP);
        _newDbButton.setPreferredSize(preferredToolbarButtonSize);
        _newDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.NEW_DATABASE));
        _newDbButton.addActionListener(e -> {

            JDialog f = new JDialog();
            f.setModal(true);
            NewDbController newDb = new NewDbController(f, guiBridge, false, null);
            f.add(newDb, BorderLayout.CENTER);
            f.setTitle("Create new database");
            f.pack();
            f.setIconImage(ImageCache.getInstance().getImage(ImageCache.NEW_DATABASE).getImage());
            f.setLocationRelativeTo(_newDbButton);
            f.setVisible(true);
            f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            if (newDb.isOk()) {
                String dbPath = newDb.getDbPath();
                String user = newDb.getDbUser();
                String pwd = newDb.getDbPwd();
                EDb dbType = newDb.getDbType();
                createNewDatabase(dbType, dbPath, user, pwd);
            }
        });

        _connectDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _connectDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _connectDbButton.setText(CONNECT);
        _connectDbButton.setToolTipText(CONNECT_TOOLTIP);
        _connectDbButton.setPreferredSize(preferredToolbarButtonSize);
        _connectDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.CONNECT));
        _connectDbButton.addActionListener(e -> {
            JDialog f = new JDialog();
            f.setModal(true);
            NewDbController newDb = new NewDbController(f, guiBridge, true, null);
            f.add(newDb, BorderLayout.CENTER);
            f.setTitle("Open database");
            f.pack();
            f.setIconImage(ImageCache.getInstance().getImage(ImageCache.CONNECT).getImage());
            f.setLocationRelativeTo(_connectDbButton);
            f.setVisible(true);
            f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            if (newDb.isOk()) {
                String dbPath = newDb.getDbPath();
                String user = newDb.getDbUser();
                String pwd = newDb.getDbPwd();
                EDb dbType = newDb.getDbType();
                openDatabase(dbType, dbPath, user, pwd);
            }
        });

        _connectRemoteDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _connectRemoteDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _connectRemoteDbButton.setText(CONNECT_REMOTE);
        _connectRemoteDbButton.setToolTipText(CONNECT_REMOTE_TOOLTIP);
        _connectRemoteDbButton.setPreferredSize(preferredToolbarButtonSize);
        _connectRemoteDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.CONNECT_REMOTE));
        _connectRemoteDbButton.addActionListener(e -> {
            JDialog f = new JDialog();
            f.setModal(true);

            String lastPath = GuiUtilities.getPreference(DatabaseGuiUtils.HM_JDBC_LAST_URL,
                    "jdbc:h2:tcp://localhost:9092/absolute_dbpath");
            NewDbController newDb = new NewDbController(f, guiBridge, false, lastPath);
            f.add(newDb, BorderLayout.CENTER);
            f.setTitle("Connect to remote database");
            f.pack();
            f.setIconImage(ImageCache.getInstance().getImage(ImageCache.CONNECT_REMOTE).getImage());
            f.setLocationRelativeTo(_newDbButton);
            f.setVisible(true);
            f.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

            if (newDb.isOk()) {
                String dbPath = newDb.getDbPath();
                String user = newDb.getDbUser();
                String pwd = newDb.getDbPwd();
                openRemoteDatabase(dbPath, user, pwd);
            }

        });

        _disconnectDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _disconnectDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _disconnectDbButton.setText(DISCONNECT);
        _disconnectDbButton.setToolTipText(DISCONNECT_TOOLTIP);
        _disconnectDbButton.setPreferredSize(preferredToolbarButtonSize);
        _disconnectDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.DISCONNECT));
        _disconnectDbButton.addActionListener(e -> {
            try {
                closeCurrentDb(true);
            } catch (Exception e1) {
                Logger.INSTANCE.insertError("", "ERROR", e1);
            }
        });

        _historyButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _historyButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _historyButton.setText(SQL_HISTORY);
        _historyButton.setToolTipText(SQL_HISTORY_TOOLTIP);
        _historyButton.setPreferredSize(preferredToolbarButtonSize);
        _historyButton.setIcon(ImageCache.getInstance().getImage(ImageCache.HISTORY_DB));
        _historyButton.addActionListener(e -> {
            try {
                if (oldSqlCommands.size() == 0) {
                    JOptionPane.showMessageDialog(this, "No history available.");
                    return;
                }

                String[] sqlHistory = oldSqlCommands.toArray(new String[0]);
                String selected = GuiUtilities.showComboDialog(this, "HISTORY", "", sqlHistory);
                if (selected != null) {
                    addTextToQueryEditor(selected);
                }

            } catch (Exception e1) {
                Logger.INSTANCE.insertError("", "ERROR", e1);
            }
        });

        _templatesButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _templatesButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _templatesButton.setText(SQL_TEMPLATES);
        _templatesButton.setToolTipText(SQL_TEMPLATES_TOOLTIP);
        _templatesButton.setPreferredSize(preferredToolbarButtonSize);
        _templatesButton.setIcon(ImageCache.getInstance().getImage(ImageCache.TEMPLATE));
        _templatesButton.addActionListener(e -> {
            try {
                String[] sqlTemplates = CommonQueries.templatesMap.keySet().toArray(new String[0]);
                String selected = GuiUtilities.showComboDialog(this, "TEMPLATES", "", sqlTemplates);
                if (selected != null) {
                    String sql = CommonQueries.templatesMap.get(selected);
                    addTextToQueryEditor(sql);
                }
            } catch (Exception e1) {
                Logger.INSTANCE.insertError("", "ERROR", e1);
            }
        });

        addComponentListener(new ComponentListener(){

            public void componentShown( ComponentEvent e ) {
            }

            public void componentResized( ComponentEvent e ) {
            }

            public void componentMoved( ComponentEvent e ) {
            }

            public void componentHidden( ComponentEvent e ) {
                onClose();
            }
        });

        try {
            _databaseTreeView.setMinimumSize(new Dimension(300, 200));

            addJtreeDragNDrop();

            addJtreeContextMenu();

            _databaseTree.setCellRenderer(new DefaultTreeCellRenderer(){
                @Override
                public java.awt.Component getTreeCellRendererComponent( JTree tree, Object value, boolean selected,
                        boolean expanded, boolean leaf, int row, boolean hasFocus ) {

                    super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                    if (value instanceof DbLevel) {
                        if (currentConnectedDatabase != null) {
                            switch( currentConnectedDatabase.getType() ) {
                            case H2GIS:
                                setIcon(ImageCache.getInstance().getImage(ImageCache.H2GIS32));
                                break;
                            case SPATIALITE:
                                setIcon(ImageCache.getInstance().getImage(ImageCache.SPATIALITE32));
                                break;
                            default:
                                setIcon(ImageCache.getInstance().getImage(ImageCache.DATABASE));
                                break;
                            }
                        }
                    } else if (value instanceof TypeLevel) {
                        setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_FOLDER));
                    } else if (value instanceof TableLevel) {
                        TableLevel tableLevel = (TableLevel) value;
                        try {
                            ETableType tableType = currentConnectedDatabase.getTableType(tableLevel.tableName);
                            if (tableLevel.isGeo) {
                                if (tableType == ETableType.EXTERNAL) {
                                    setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_SPATIAL_VIRTUAL));
                                } else {
                                    setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_SPATIAL));
                                }
                            } else {
                                if (tableType == ETableType.VIEW) {
                                    setIcon(ImageCache.getInstance().getImage(ImageCache.VIEW));
                                } else {
                                    setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE));
                                }
                            }
                        } catch (Exception e) {
                            setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE));
                            e.printStackTrace();
                        }
                    } else if (value instanceof ColumnLevel) {
                        ColumnLevel columnLevel = (ColumnLevel) value;
                        if (columnLevel.isPK) {
                            setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_PRIMARYKEY));
                        } else if (columnLevel.references != null) {
                            setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_INDEX));
                        } else if (columnLevel.geomColumn != null) {
                            ESpatialiteGeometryType gType = ESpatialiteGeometryType.forValue(columnLevel.geomColumn.geometryType);
                            switch( gType ) {
                            case POINT_XY:
                            case POINT_XYM:
                            case POINT_XYZ:
                            case POINT_XYZM:
                            case MULTIPOINT_XY:
                            case MULTIPOINT_XYM:
                            case MULTIPOINT_XYZ:
                            case MULTIPOINT_XYZM:
                                setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_POINT));
                                break;
                            case LINESTRING_XY:
                            case LINESTRING_XYM:
                            case LINESTRING_XYZ:
                            case LINESTRING_XYZM:
                            case MULTILINESTRING_XY:
                            case MULTILINESTRING_XYM:
                            case MULTILINESTRING_XYZ:
                            case MULTILINESTRING_XYZM:
                                setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_LINE));
                                break;
                            case POLYGON_XY:
                            case POLYGON_XYM:
                            case POLYGON_XYZ:
                            case POLYGON_XYZM:
                            case MULTIPOLYGON_XY:
                            case MULTIPOLYGON_XYM:
                            case MULTIPOLYGON_XYZ:
                            case MULTIPOLYGON_XYZM:
                                setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_POLYGON));
                                break;
                            default:
                                setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN));
                                break;
                            }
                        } else {
                            setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN));
                        }
                    }

                    return this;
                }

            });

            _databaseTree.addTreeSelectionListener(new TreeSelectionListener(){
                public void valueChanged( TreeSelectionEvent evt ) {
                    TreePath[] paths = evt.getPaths();
                    currentSelectedDb = null;
                    currentSelectedTable = null;
                    currentSelectedColumn = null;
                    if (paths.length > 0) {
                        Object selectedItem = paths[0].getLastPathComponent();
                        if (selectedItem instanceof DbLevel) {
                            currentSelectedDb = (DbLevel) selectedItem;
                        }
                        if (selectedItem instanceof TableLevel) {
                            currentSelectedTable = (TableLevel) selectedItem;

                            try {
                                QueryResult queryResult = currentConnectedDatabase
                                        .getTableRecordsMapIn(currentSelectedTable.tableName, null, true, 20, -1);
                                loadDataViewer(queryResult);
                            } catch (Exception e) {
                                Logger.INSTANCE.insertError("", "ERROR", e);
                            }
                        } else {
                            currentSelectedTable = null;
                            _dataViewerTable.setModel(new DefaultTableModel());
                        }

                        if (selectedItem instanceof ColumnLevel) {
                            currentSelectedColumn = (ColumnLevel) selectedItem;
                        }
                    }
                }
            });

            _databaseTree.setVisible(false);
        } catch (Exception e1) {
            Logger.INSTANCE.insertError("", "Error", e1);
        }

        layoutTree(null, false);

        _runQueryButton.setIcon(ImageCache.getInstance().getImage(ImageCache.RUN));
        _runQueryButton.setToolTipText(RUN_QUERY_TOOLTIP);
        _runQueryButton.setText("");
        _runQueryButton.setPreferredSize(preferredSqleditorButtonSize);
        _runQueryButton.addActionListener(e -> {

            String sqlText = _sqlEditorArea.getText().trim();
            if (sqlText.length() == 0) {
                return;
            }

            final LogConsoleController logConsole = new LogConsoleController(pm);
            JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

            new Thread(() -> {
                boolean hadErrors = false;
                try {
                    logConsole.beginProcess("Run query");
                    hadErrors = runQuery(sqlText, pm);
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

        _runQueryAndStoreButton.setIcon(ImageCache.getInstance().getImage(ImageCache.RUN_TO_FILE));
        _runQueryAndStoreButton.setToolTipText(RUN_QUERY_TO_FILE_TOOLTIP);
        _runQueryAndStoreButton.setText("");
        _runQueryAndStoreButton.setPreferredSize(preferredSqleditorButtonSize);
        _runQueryAndStoreButton.addActionListener(e -> {

            File selectedFile = null;
            String sqlText = _sqlEditorArea.getText().trim();
            if (sqlText.length() > 0) {
                if (isSelectOrPragma(sqlText)) {
                    File[] saveFiles = guiBridge.showSaveFileDialog("Select file to save to", GuiUtilities.getLastFile(), null);
                    if (saveFiles != null && saveFiles.length > 0) {
                        try {
                            GuiUtilities.setLastPath(saveFiles[0].getAbsolutePath());
                        } catch (Exception e1) {
                            Logger.INSTANCE.insertError("", "ERROR", e1);
                        }
                    } else {
                        return;
                    }
                    selectedFile = saveFiles[0];

                } else {
                    JOptionPane.showMessageDialog(this, "Writing to files is allowed only for SELECT statements and PRAGMAs.",
                            "WARNING", JOptionPane.WARNING_MESSAGE, null);
                    return;
                }

            }

            final LogConsoleController logConsole = new LogConsoleController(pm);
            JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");
            final File f_selectedFile = selectedFile;
            new Thread(() -> {
                boolean hadErrors = false;
                try {
                    if (f_selectedFile != null) {
                        logConsole.beginProcess("Run query");
                        hadErrors = runQueryToFile(sqlText, f_selectedFile, pm);
                    }
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

        _runQueryAndStoreShapefileButton.setIcon(ImageCache.getInstance().getImage(ImageCache.RUN_TO_SHAPEFILE));
        _runQueryAndStoreShapefileButton.setToolTipText(RUN_QUERY_TO_SHAPEFILE_TOOLTIP);
        _runQueryAndStoreShapefileButton.setText("");
        _runQueryAndStoreShapefileButton.setPreferredSize(preferredSqleditorButtonSize);
        _runQueryAndStoreShapefileButton.addActionListener(e -> {

            File selectedFile = null;
            String sqlText = _sqlEditorArea.getText().trim();
            if (sqlText.length() > 0) {
                if (sqlText.toLowerCase().startsWith("select")) {
                    File[] saveFiles = guiBridge.showSaveFileDialog("Select shapefile to save to", GuiUtilities.getLastFile(),
                            HMConstants.vectorFileFilter);
                    if (saveFiles != null && saveFiles.length > 0) {
                        try {
                            GuiUtilities.setLastPath(saveFiles[0].getAbsolutePath());
                        } catch (Exception e1) {
                            Logger.INSTANCE.insertError("", "ERROR", e1);
                        }
                    } else {
                        return;
                    }
                    selectedFile = saveFiles[0];

                } else {
                    JOptionPane.showMessageDialog(this, "Writing to shapefile is allowed only for SELECT statements.", "WARNING",
                            JOptionPane.WARNING_MESSAGE, null);
                    return;
                }

            }

            final LogConsoleController logConsole = new LogConsoleController(pm);
            JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");
            final File f_selectedFile = selectedFile;
            new Thread(() -> {
                boolean hadErrors = false;
                try {
                    if (f_selectedFile != null) {
                        logConsole.beginProcess("Run query");
                        hadErrors = runQueryToShapefile(sqlText, f_selectedFile, pm);
                    }
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

        setViewQueryButton(_viewQueryButton, preferredSqleditorButtonSize, _sqlEditorArea);

        _clearSqlEditorbutton.setIcon(ImageCache.getInstance().getImage(ImageCache.TRASH));
        _clearSqlEditorbutton.setToolTipText(CLEAR_SQL_EDITOR);
        _clearSqlEditorbutton.setText("");
        _clearSqlEditorbutton.setPreferredSize(preferredSqleditorButtonSize);
        _clearSqlEditorbutton.addActionListener(e -> {
            _sqlEditorArea.setText("");
        });
    }

    protected abstract void setViewQueryButton( JButton _viewQueryButton, Dimension preferredButtonSize,
            JTextPane sqlEditorArea );

    @SuppressWarnings("serial")
    private void addJtreeDragNDrop() {
        _databaseTree.setDragEnabled(true);
        _databaseTree.setTransferHandler(new TransferHandler(null){
            public int getSourceActions( JComponent c ) {
                return COPY;
            }
            protected Transferable createTransferable( JComponent c ) {
                if (c instanceof JTree) {
                    if (currentSelectedColumn != null) {
                        return new StringSelection(currentSelectedColumn.columnName);
                    } else if (currentSelectedTable != null) {
                        return new StringSelection(currentSelectedTable.tableName);
                    }
                }
                return new StringSelection("");
            }
        });
    }

    private void addJtreeContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new BevelBorder(BevelBorder.RAISED));
        popupMenu.addPopupMenuListener(new PopupMenuListener(){

            @Override
            public void popupMenuWillBecomeVisible( PopupMenuEvent e ) {
                if (currentSelectedTable != null) {
                    List<Action> tableActions = makeTableAction(currentSelectedTable);
                    for( Action action : tableActions ) {
                        if (action != null) {
                            JMenuItem item = new JMenuItem(action);
                            popupMenu.add(item);
                            item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        } else {
                            popupMenu.add(new JSeparator());
                        }
                    }
                } else if (currentSelectedDb != null) {
                    List<Action> tableActions = makeDatabaseAction(currentSelectedDb);
                    for( Action action : tableActions ) {
                        if (action != null) {
                            JMenuItem item = new JMenuItem(action);
                            popupMenu.add(item);
                            item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        } else {
                            popupMenu.add(new JSeparator());
                        }
                    }
                } else if (currentSelectedColumn != null) {
                    List<Action> columnActions = makeColumnActions(currentSelectedColumn);
                    for( Action action : columnActions ) {
                        if (action != null) {
                            JMenuItem item = new JMenuItem(action);
                            popupMenu.add(item);
                            item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        } else {
                            popupMenu.add(new JSeparator());
                        }
                    }
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible( PopupMenuEvent e ) {
                popupMenu.removeAll();
            }

            @Override
            public void popupMenuCanceled( PopupMenuEvent e ) {
                popupMenu.removeAll();
            }
        });

        _databaseTree.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent e ) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = _databaseTree.getClosestRowForLocation(e.getX(), e.getY());
                    _databaseTree.setSelectionRow(row);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }

            }
        });
    }

    private void addDataTableContextMenu() {
        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new BevelBorder(BevelBorder.RAISED));
        popupMenu.addPopupMenuListener(new PopupMenuListener(){

            @Override
            public void popupMenuWillBecomeVisible( PopupMenuEvent e ) {
                int[] selectedRows = _dataViewerTable.getSelectedRows();
                int[] selectedCols = _dataViewerTable.getSelectedColumns();

                boolean isGeom = false;
                if (selectedCols.length == 1 && selectedRows.length > 0) {
                    // check coontent
                    String valueAt = _dataViewerTable.getValueAt(selectedRows[0], selectedCols[0]).toString();
                    String[] split = valueAt.split("\\s+");
                    if (split.length > 0 && ESpatialiteGeometryType.isGeometryName(split[0])) {
                        isGeom = true;
                    }

                }

                JMenuItem item = new JMenuItem(new AbstractAction("Copy cells content"){
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        _dataViewerTable.getTransferHandler().exportToClipboard(_dataViewerTable, clipboard,
                                TransferHandler.COPY);
                    }
                });
                item.setHorizontalTextPosition(JMenuItem.RIGHT);
                popupMenu.add(item);
                if (isGeom) {
                    JMenuItem item1 = new JMenuItem(new AbstractAction("View geometry"){
                        private static final long serialVersionUID = 1L;
                        @Override
                        public void actionPerformed( ActionEvent e ) {

                            WKTReader wktReader = new WKTReader();
                            List<Geometry> geomsList = new ArrayList<>();
                            for( int r : selectedRows ) {
                                try {
                                    String valueAt = _dataViewerTable.getValueAt(r, selectedCols[0]).toString();
                                    Geometry geometry = wktReader.read(valueAt);
                                    geomsList.add(geometry);
                                } catch (ParseException e1) {
                                    e1.printStackTrace();
                                }
                            }

                            if (geomsList.size() > 0) {
                                SimpleFeatureCollection fc = FeatureUtilities.featureCollectionFromGeometry(null,
                                        geomsList.toArray(new Geometry[0]));
                                showInMapFrame(false, fc);
                            }

                        }

                    });
                    item1.setHorizontalTextPosition(JMenuItem.RIGHT);
                    popupMenu.add(item1);
                }
            }

            @Override
            public void popupMenuWillBecomeInvisible( PopupMenuEvent e ) {
                popupMenu.removeAll();
            }

            @Override
            public void popupMenuCanceled( PopupMenuEvent e ) {
                popupMenu.removeAll();
            }
        });

        _dataViewerTable.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent e ) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }

            }
        });
    }

    protected void loadDataViewer( QueryResult queryResult ) {
        if (queryResult == null) {
            _dataViewerTable.setModel(new DefaultTableModel());
            return;
        }
        Object[] names = queryResult.names.toArray(new String[0]);
        List<Object[]> data = queryResult.data;
        Object[][] values = new Object[queryResult.data.size()][];
        int index = 0;
        for( Object[] objects : data ) {
            values[index++] = objects;
        }

        _dataViewerTable.setModel(new DefaultTableModel(values, names));
        _dataViewerTable.setCellSelectionEnabled(true);

        for( int column = 0; column < _dataViewerTable.getColumnCount(); column++ ) {
            TableColumn tableColumn = _dataViewerTable.getColumnModel().getColumn(column);
            int preferredWidth = tableColumn.getMinWidth();
            int maxWidth = tableColumn.getMaxWidth();

            for( int row = 0; row < _dataViewerTable.getRowCount(); row++ ) {
                TableCellRenderer cellRenderer = _dataViewerTable.getCellRenderer(row, column);
                Component c = _dataViewerTable.prepareRenderer(cellRenderer, row, column);
                int width = c.getPreferredSize().width + _dataViewerTable.getIntercellSpacing().width;
                preferredWidth = Math.max(preferredWidth, width);

                if (preferredWidth >= maxWidth) {
                    preferredWidth = maxWidth;
                    break;
                }
            }
            if (preferredWidth < 50) {
                preferredWidth = 50;
            }
            if (preferredWidth > 300) {
                preferredWidth = 300;
            }
            tableColumn.setPreferredWidth(preferredWidth);
        }

        _recordCountTextfield.setText(values.length + " in " + millisToTimeString(queryResult.queryTimeMillis));
    }

    private void layoutTree( DbLevel dbLevel, boolean expandNodes ) {
        toggleButtonsEnabling(dbLevel != null);

        String title;
        if (dbLevel != null) {
            _databaseTree.setVisible(true);
            // title = dbLevel.dbName;

            title = currentConnectedDatabase.getDatabasePath();
        } else {
            dbLevel = new DbLevel();
            _databaseTree.setVisible(false);
            title = DATABASE_CONNECTIONS;
        }
        setDbTreeTitle(title);

        ObjectTreeModel model = new ObjectTreeModel();
        model.setRoot(dbLevel);
        _databaseTree.setModel(model);

        if (expandNodes) {
            _databaseTree.expandRow(0);
            _databaseTree.expandRow(1);
        }
        // expandAllNodes(_databaseTree, 0, 2);

    }

    private void toggleButtonsEnabling( boolean enable ) {
        _runQueryButton.setEnabled(enable);
        _runQueryAndStoreButton.setEnabled(enable);
        _runQueryAndStoreShapefileButton.setEnabled(enable);
        _templatesButton.setEnabled(enable);
        _historyButton.setEnabled(enable);
        _clearSqlEditorbutton.setEnabled(enable);
        _viewQueryButton.setEnabled(enable);

        _recordCountTextfield.setText("");

        _sqlEditorArea.setText("");
        _sqlEditorArea.setEditable(enable);
    }

    // private void expandAllNodes( JTree tree, int startingIndex, int rowCount ) {
    // for( int i = startingIndex; i < rowCount; ++i ) {
    // tree.expandRow(i);
    // }
    //
    // if (tree.getRowCount() != rowCount) {
    // expandAllNodes(tree, rowCount, tree.getRowCount());
    // }
    // }

    class ObjectTreeModel implements TreeModel {

        private DbLevel root;
        private EventListenerList listenerList = new EventListenerList();
        /**
        * Constructs an empty tree.
        */
        public ObjectTreeModel() {
            root = null;
        }

        /**
        * Sets the root to a given variable.
        * @param v the variable that is being described by this tree
        */
        public void setRoot( DbLevel v ) {
            DbLevel oldRoot = v;
            root = v;
            fireTreeStructureChanged(oldRoot);
        }

        public Object getRoot() {
            return root;
        }

        @SuppressWarnings("rawtypes")
        public int getChildCount( Object parent ) {
            if (parent instanceof DbLevel) {
                DbLevel dbLevel = (DbLevel) parent;
                return dbLevel.typesList.size();
            } else if (parent instanceof TypeLevel) {
                TypeLevel typeLevel = (TypeLevel) parent;
                return typeLevel.tablesList.size();
            } else if (parent instanceof TableLevel) {
                TableLevel tableLevel = (TableLevel) parent;
                return tableLevel.columnsList.size();
            } else if (parent instanceof ColumnLevel) {
                return 0;
            } else if (parent instanceof List) {
                List list = (List) parent;
                return list.size();
            }
            return 0;
        }

        @SuppressWarnings("rawtypes")
        public Object getChild( Object parent, int index ) {
            if (parent instanceof DbLevel) {
                DbLevel dbLevel = (DbLevel) parent;
                return dbLevel.typesList.get(index);
            } else if (parent instanceof TypeLevel) {
                TypeLevel typeLevel = (TypeLevel) parent;
                return typeLevel.tablesList.get(index);
            } else if (parent instanceof TableLevel) {
                TableLevel tableLevel = (TableLevel) parent;
                return tableLevel.columnsList.get(index);
            } else if (parent instanceof List) {
                List list = (List) parent;
                Object item = list.get(index);
                return item;
            }
            return null;
        }

        public int getIndexOfChild( Object parent, Object child ) {
            int n = getChildCount(parent);
            for( int i = 0; i < n; i++ )
                if (getChild(parent, i).equals(child))
                    return i;
            return -1;
        }

        public boolean isLeaf( Object node ) {
            return getChildCount(node) == 0;
        }

        public void valueForPathChanged( TreePath path, Object newValue ) {
        }

        public void addTreeModelListener( TreeModelListener l ) {
            listenerList.add(TreeModelListener.class, l);
        }

        public void removeTreeModelListener( TreeModelListener l ) {
            listenerList.remove(TreeModelListener.class, l);
        }

        protected void fireTreeStructureChanged( Object oldRoot ) {
            TreeModelEvent event = new TreeModelEvent(this, new Object[]{oldRoot});
            EventListener[] listeners = listenerList.getListeners(TreeModelListener.class);
            for( int i = 0; i < listeners.length; i++ )
                ((TreeModelListener) listeners[i]).treeStructureChanged(event);
        }

    }

    public JComponent asJComponent() {
        return this;
    }

    public void onClose() {
        if (currentConnectedDatabase != null) {
            GuiUtilities.setPreference(DatabaseGuiUtils.HM_SPATIALITE_LAST_FILE, currentConnectedDatabase.getDatabasePath());
        }
        try {
            closeCurrentDb(false);
        } catch (Exception e) {
            Logger.INSTANCE.insertError("", "Error", e);
        }
    }

    protected void createNewDatabase( EDb dbType, String dbfilePath, String user, String pwd ) {
        try {
            closeCurrentDb(true);
        } catch (Exception e1) {
            Logger.INSTANCE.insertError("", "Error closing the database...", e1);
        }

        final LogConsoleController logConsole = new LogConsoleController(pm);
        JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

        new Thread(() -> {
            logConsole.beginProcess("Create new database");

            try {
                if (dbType == EDb.SPATIALITE) {
                    currentConnectedDatabase = new GTSpatialiteThreadsafeDb();
                } else {
                    currentConnectedDatabase = dbType.getSpatialDb();
                }
                currentConnectedDatabase.setCredentials(user, pwd);
                currentConnectedDatabase.open(dbfilePath);
                currentConnectedDatabase.initSpatialMetadata(null);
                sqlTemplatesAndActions = new SqlTemplatesAndActions(currentConnectedDatabase.getType());

                DbLevel dbLevel = gatherDatabaseLevels(currentConnectedDatabase);

                layoutTree(dbLevel, false);
            } catch (Exception e) {
                currentConnectedDatabase = null;
                Logger.INSTANCE.insertError("", "Error connecting to the database...", e);
            } finally {
                logConsole.finishProcess();
                logConsole.stopLogging();
                logConsole.setVisible(false);
                window.dispose();
            }
        }).start();

    }

    protected void setDbTreeTitle( String title ) {
        Border databaseTreeViewBorder = _databaseTreeView.getBorder();
        if (databaseTreeViewBorder instanceof TitledBorder) {
            TitledBorder tBorder = (TitledBorder) databaseTreeViewBorder;
            tBorder.setTitle(title);
            _databaseTreeView.repaint();
            _databaseTreeView.invalidate();
        }
    }

    protected void openDatabase( EDb dbType, String dbfilePath, String user, String pwd ) {
        if (dbfilePath == null && dbType == null) {
            return;
        }
        try {
            closeCurrentDb(true);
        } catch (Exception e1) {
            Logger.INSTANCE.insertError("", "Error closing the database...", e1);
        }

        final LogConsoleController logConsole = new LogConsoleController(pm);
        JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

        new Thread(() -> {
            logConsole.beginProcess("Open database");

            try {
                if (dbType == EDb.SPATIALITE) {
                    if (SpatialiteCommonMethods.isSqliteFile(new File(dbfilePath))) {
                        currentConnectedDatabase = new GTSpatialiteThreadsafeDb();
                    } else {
                        guiBridge.messageDialog("The selected file is not a Spatialite database.", "WARNING",
                                JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                } else {
                    currentConnectedDatabase = dbType.getSpatialDb();
                }
                currentConnectedDatabase.setCredentials(user, pwd);
                try {
                    currentConnectedDatabase.open(dbfilePath);
                } catch (JdbcSQLException e) {
                    String message = e.getMessage();
                    if (message.contains("Wrong user name or password")) {
                        guiBridge.messageDialog("Wrong user name or password.", "ERROR", JOptionPane.ERROR_MESSAGE);
                        currentConnectedDatabase = null;
                        return;
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                sqlTemplatesAndActions = new SqlTemplatesAndActions(currentConnectedDatabase.getType());

                DbLevel dbLevel = gatherDatabaseLevels(currentConnectedDatabase);

                layoutTree(dbLevel, true);
                setDbTreeTitle(currentConnectedDatabase.getDatabasePath());
            } catch (Exception e) {
                currentConnectedDatabase = null;
                Logger.INSTANCE.insertError("", "Error connecting to the database...", e);
            } finally {
                logConsole.finishProcess();
                logConsole.stopLogging();
                logConsole.setVisible(false);
                window.dispose();
            }
        }).start();
    }

    protected void openRemoteDatabase( String urlString, String user, String pwd ) {
        try {
            closeCurrentDb(true);
        } catch (Exception e1) {
            Logger.INSTANCE.insertError("", "Error closing the database...", e1);
        }

        EDb type = null;
        if (urlString.trim().startsWith(EDb.H2GIS.getJdbcPrefix())) {
            type = EDb.H2GIS;
        }
        // for( EDb edb : EDb.values() ) {
        // if (urlString.trim().startsWith(edb.getJdbcPrefix())) {
        // if (edb.isSpatial()) {
        // type = edb;
        // break;
        // }
        // }
        // }

        if (type == null) {
            guiBridge.messageDialog("Only H2GIS databases are supported in remote connection.", "ERROR",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        GuiUtilities.setPreference(DatabaseGuiUtils.HM_JDBC_LAST_URL, urlString);

        urlString = urlString.replaceFirst(type.getJdbcPrefix(), "");

        final LogConsoleController logConsole = new LogConsoleController(pm);
        JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

        EDb _type = type;
        String _urlString = urlString;
        new Thread(() -> {
            logConsole.beginProcess("Open database");

            try {
                currentConnectedDatabase = _type.getSpatialDb();
                currentConnectedDatabase.open(_urlString);
                sqlTemplatesAndActions = new SqlTemplatesAndActions(currentConnectedDatabase.getType());

                DbLevel dbLevel = gatherDatabaseLevels(currentConnectedDatabase);

                layoutTree(dbLevel, true);
                setDbTreeTitle(currentConnectedDatabase.getDatabasePath());
            } catch (Exception e) {
                currentConnectedDatabase = null;
                Logger.INSTANCE.insertError("", "Error connecting to the database...", e);
            } finally {
                logConsole.finishProcess();
                logConsole.stopLogging();
                logConsole.setVisible(false);
                window.dispose();
            }
        }).start();

    }

    protected DbLevel gatherDatabaseLevels( ASpatialDb db ) throws Exception {
        currentDbLevel = new DbLevel();
        String databasePath = db.getDatabasePath();
        File dbFile = new File(databasePath);

        String dbName = FileUtilities.getNameWithoutExtention(dbFile);
        currentDbLevel.dbName = dbName;

        HashMap<String, List<String>> currentDatabaseTablesMap = db.getTablesMap(true);
        for( String typeName : SpatialiteTableNames.ALL_TYPES_LIST ) {
            TypeLevel typeLevel = new TypeLevel();
            typeLevel.typeName = typeName;
            List<String> tablesList = currentDatabaseTablesMap.get(typeName);
            for( String tableName : tablesList ) {
                TableLevel tableLevel = new TableLevel();
                tableLevel.parent = currentDbLevel;
                tableLevel.tableName = tableName;

                ETableType tableType = db.getTableType(tableName);
                tableLevel.tableType = tableType;

                GeometryColumn geometryColumns = null;
                try {
                    geometryColumns = db.getGeometryColumnsForTable(tableName);
                } catch (Exception e1) {
                    // ignore
                }
                List<ForeignKey> foreignKeys = new ArrayList<>();
                try {
                    foreignKeys = db.getForeignKeys(tableName);
                } catch (Exception e) {
                }
                tableLevel.isGeo = geometryColumns != null;
                List<String[]> tableInfo;
                try {
                    tableInfo = db.getTableColumns(tableName);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                for( String[] columnInfo : tableInfo ) {
                    ColumnLevel columnLevel = new ColumnLevel();
                    columnLevel.parent = tableLevel;
                    String columnName = columnInfo[0];
                    String columnType = columnInfo[1];
                    String columnPk = columnInfo[2];
                    columnLevel.columnName = columnName;
                    columnLevel.columnType = columnType;
                    columnLevel.isPK = columnPk.equals("1") ? true : false;
                    if (geometryColumns != null && columnName.equalsIgnoreCase(geometryColumns.geometryColumnName)) {
                        columnLevel.geomColumn = geometryColumns;
                    }
                    for( ForeignKey fKey : foreignKeys ) {
                        if (fKey.from.equals(columnName)) {
                            columnLevel.setFkReferences(fKey);
                        }
                    }
                    tableLevel.columnsList.add(columnLevel);
                }
                typeLevel.tablesList.add(tableLevel);
            }
            currentDbLevel.typesList.add(typeLevel);
        }
        return currentDbLevel;
    }

    protected void closeCurrentDb( boolean manually ) throws Exception {
        if (currentConnectedDatabase != null) {
            setDbTreeTitle(DB_TREE_TITLE);
            layoutTree(null, false);
            loadDataViewer(null);
            currentConnectedDatabase.close();
            currentConnectedDatabase = null;
            _recordCountTextfield.setText("");

            if (manually)
                GuiUtilities.setPreference(DatabaseGuiUtils.HM_SPATIALITE_LAST_FILE, (String) null);
        }
    }

    protected boolean runQuery( String sqlText, IHMProgressMonitor pm ) {
        if (pm == null) {
            pm = this.pm;
        }
        boolean hasError = false;
        if (currentConnectedDatabase != null && sqlText.length() > 0) {
            try {
                int maxLength = 100;
                String queryForLog;
                if (sqlText.length() > maxLength) {
                    queryForLog = sqlText.substring(0, maxLength) + "...";
                } else {
                    queryForLog = sqlText;
                }
                pm.beginTask("Run query: " + queryForLog, IHMProgressMonitor.UNKNOWN);

                int limit = getLimit();

                if (sqlText.contains(";")) {
                    String trim = sqlText.replaceAll("\n", "").trim();
                    String[] querySplit = trim.split(";");
                    if (querySplit.length > 1) {
                        pm.message("Runnng in multi query mode, since a semicolon has been found.");
                        for( String sql : querySplit ) {
                            if (isSelectOrPragma(sql)) {
                                QueryResult queryResult = currentConnectedDatabase.getTableRecordsMapFromRawSql(sql, limit);
                                loadDataViewer(queryResult);
                            } else {
                                long start = System.currentTimeMillis();
                                int resultCode = currentConnectedDatabase.executeInsertUpdateDeleteSql(sql);
                                QueryResult dummyQueryResult = new QueryResult();
                                long end = System.currentTimeMillis();
                                dummyQueryResult.queryTimeMillis = end - start;
                                dummyQueryResult.names.add(
                                        "Result = " + resultCode + " in " + millisToTimeString(dummyQueryResult.queryTimeMillis));
                                // loadDataViewer(dummyQueryResult);
                            }
                            // addQueryToHistoryCombo(sql);
                        }
                        if (!hasError && _refreshTreeAfterQueryCheckbox.isSelected()) {
                            try {
                                refreshDatabaseTree();
                            } catch (SQLException e) {
                                Logger.INSTANCE.insertError("", "error", e);
                            }
                        }
                        return hasError;
                    }
                }

                if (isSelectOrPragma(sqlText)) {
                    QueryResult queryResult = currentConnectedDatabase.getTableRecordsMapFromRawSql(sqlText, limit);
                    loadDataViewer(queryResult);

                    int size = queryResult.data.size();
                    String msg = "Records: " + size;
                    if (size == limit) {
                        msg += " (table output limited to " + limit + " records)";
                    }
                    pm.message(msg);
                } else {
                    int resultCode = currentConnectedDatabase.executeInsertUpdateDeleteSql(sqlText);
                    QueryResult dummyQueryResult = new QueryResult();
                    dummyQueryResult.names.add("Result = " + resultCode);
                    loadDataViewer(dummyQueryResult);
                }

                addQueryToHistoryCombo(sqlText);

            } catch (Exception e1) {
                String localizedMessage = e1.getLocalizedMessage();
                hasError = true;
                pm.errorMessage("An error occurred: " + localizedMessage);
            } finally {
                pm.done();
            }
        }

        if (!hasError && _refreshTreeAfterQueryCheckbox.isSelected()) {
            try {
                refreshDatabaseTree();
            } catch (Exception e) {
                Logger.INSTANCE.insertError("", "error", e);
            }
        }
        return hasError;
    }

    private String millisToTimeString( long queryTimeMillis ) {
        if (queryTimeMillis < 1000) {
            return queryTimeMillis + " milliseconds";
        } else if (queryTimeMillis < 1000 * 60) {
            return queryTimeMillis / 1000 + " seconds";
        } else if (queryTimeMillis < 1000 * 60 * 60) {
            return queryTimeMillis / 1000 / 60 + " minutes";
        } else {
            return queryTimeMillis / 1000 / 60 / 60 + " hours";
        }
    }

    protected int getLimit() {
        int limit;
        limit = 1000;
        try {
            String limitText = _limitCountTextfield.getText();
            limit = Integer.parseInt(limitText);
        } catch (Exception e) {
            // reset
            _limitCountTextfield.setText("1000");
        }
        return limit;
    }

    protected boolean isSelectOrPragma( String sqlText ) {
        sqlText = sqlText.trim();
        return sqlText.toLowerCase().startsWith("select") || sqlText.toLowerCase().startsWith("pragma");
    }

    protected boolean runQueryToFile( String sqlText, File selectedFile, IHMProgressMonitor pm ) {
        boolean hasError = false;
        if (currentConnectedDatabase != null && sqlText.length() > 0) {
            try {
                pm.beginTask("Run query: " + sqlText + "\ninto file: " + selectedFile, IHMProgressMonitor.UNKNOWN);
                currentConnectedDatabase.runRawSqlToCsv(sqlText, selectedFile, true, ";");
                addQueryToHistoryCombo(sqlText);
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

    protected boolean runQueryToShapefile( String sqlText, File selectedFile, IHMProgressMonitor pm ) {
        boolean hasError = false;
        if (sqlText.trim().length() == 0) {
            return false;
        }
        if (currentConnectedDatabase instanceof GTSpatialiteThreadsafeDb) {
            try {
                pm.beginTask("Run query: " + sqlText + "\ninto shapefile: " + selectedFile, IHMProgressMonitor.UNKNOWN);
                DefaultFeatureCollection fc = DbsHelper.runRawSqlToFeatureCollection(null, currentConnectedDatabase, sqlText);
                OmsVectorWriter.writeVector(selectedFile.getAbsolutePath(), fc);
                addQueryToHistoryCombo(sqlText);
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

    protected void addTextToQueryEditor( String newText ) {
        String text = _sqlEditorArea.getText();
        if (text.trim().length() != 0) {
            text += "\n";
        }
        text += newText;
        _sqlEditorArea.setText(text);
    }

    protected void addQueryToHistoryCombo( String sqlText ) {
        if (oldSqlCommands.contains(sqlText)) {
            oldSqlCommands.remove(sqlText);
        }
        oldSqlCommands.add(0, sqlText);
        if (oldSqlCommands.size() > 20) {
            oldSqlCommands.remove(20);
        }

        GuiUtilities.setPreference("HM_OLD_SQL_COMMANDS", oldSqlCommands.toArray(new String[0]));
    }

    protected abstract List<Action> makeColumnActions( final ColumnLevel selectedColumn );

    protected abstract List<Action> makeDatabaseAction( final DbLevel dbLevel );

    protected abstract List<Action> makeTableAction( final TableLevel selectedTable );

    protected void refreshDatabaseTree() throws Exception {
        DbLevel dbLevel = gatherDatabaseLevels(currentConnectedDatabase);
        setDbTreeTitle(currentConnectedDatabase.getDatabasePath());
        layoutTree(dbLevel, true);
    }

    protected void showInMapFrame( boolean withLayers, SimpleFeatureCollection fc ) {
        if (mapFrame == null || !mapFrame.isVisible()) {
            Class<DatabaseController> class1 = DatabaseController.class;
            ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
            mapFrame = new HMMapframe("Geometries Viewer");
            mapFrame.setIconImage(icon.getImage());
            mapFrame.enableToolBar(true);
            mapFrame.enableStatusBar(false);
            mapFrame.enableLayerTable(withLayers);
            mapFrame.enableTool(Tool.PAN, Tool.ZOOM, Tool.RESET);
            mapFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            if (withLayers) {
                mapFrame.setSize(900, 600);
            } else {
                mapFrame.setSize(600, 600);
            }
            mapFrame.setVisible(true);
        }
        mapFrame.addLayer(fc);
    }

}
