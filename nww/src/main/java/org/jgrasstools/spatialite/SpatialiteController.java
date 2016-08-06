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
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
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
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.logging.JGTLogger;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.spatialite.ForeignKey;
import org.jgrasstools.gears.spatialite.QueryResult;
import org.jgrasstools.gears.spatialite.SpatialiteDb;
import org.jgrasstools.gears.spatialite.SpatialiteGeometryColumns;
import org.jgrasstools.gears.spatialite.SpatialiteGeometryType;
import org.jgrasstools.gears.spatialite.SpatialiteTableNames;
import org.jgrasstools.gui.console.LogConsoleController;
import org.jgrasstools.gui.utils.DefaultGuiBridgeImpl;
import org.jgrasstools.gui.utils.GuiBridgeHandler;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.jgrasstools.gui.utils.GuiUtilities.IOnCloseListener;
import org.jgrasstools.gui.utils.ImageCache;
import org.jgrasstools.nww.SimpleNwwViewer;
import org.jgrasstools.nww.gui.ToolsPanelController;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.jgrasstools.spatialite.objects.ColumnLevel;
import org.jgrasstools.spatialite.objects.DbLevel;
import org.jgrasstools.spatialite.objects.TableLevel;
import org.jgrasstools.spatialite.objects.TypeLevel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The spatialtoolbox view controller.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SpatialiteController extends SpatialiteView implements IOnCloseListener {
    private static final Logger logger = LoggerFactory.getLogger(SpatialiteView.class);
    private static final long serialVersionUID = 1L;

    private static final String SHAPEFILE_IMPORT = "import shapefile in selected table";
    private static final String SHAPEFILE_CCREATE_FROM_SCHEMA = "create table from shapefile schema";
    private static final String SHAPEFILE_TOOLTIP = "tools to deal with shapefiles";
    private static final String SHAPEFILE = "shapefile";
    private static final String SQL_TEMPLATES_TOOLTIP = "create a query based on a template";
    private static final String SQL_TEMPLATES = "sql templates";
    private static final String SQL_HISTORY_TOOLTIP = "select queries from the history";
    private static final String SQL_HISTORY = "sql history";
    private static final String DISCONNECT_TOOLTIP = "disconnect from current database";
    private static final String DISCONNECT = "disconnect";
    private static final String RUN_QUERY = "run query";
    private static final String RUN_QUERY_TOOLTIP = "run the query in the SQL Editor";
    private static final String RUN_QUERY_TO_FILE_TOOLTIP = "run the query in the SQL Editor and store result in file";
    private static final String RUN_QUERY_TO_SHAPEFILE_TOOLTIP = "run the query in the SQL Editor and store result in a shapefile";
    private static final String VIEW_QUERY_TOOLTIP = "run spatial query and view the result in the 3D viewer";
    private static final String SQL_EDITOR = "SQL Editor";
    private static final String CLEAR_SQL_EDITOR = "clear SQL editor";
    private static final String DATA_VIEWER = "Data viewer";
    private static final String DATABASE_CONNECTIONS = "Database connection";
    private static final String NEW = "new";
    private static final String NEW_TOOLTIP = "create a new spatialite database";
    private static final String CONNECT = "connect";
    private static final String CONNECT_TOOLTIP = "connect to an existing spatialite database";

    protected HashMap<String, String> prefsMap = new HashMap<>();

    private GuiBridgeHandler guiBridge;
    private IJGTProgressMonitor pm = new LogProgressMonitor();
    protected SpatialiteDb currentConnectedDatabase;
    private DbLevel currentDbLevel;
    protected TableLevel currentSelectedTable;
    protected ColumnLevel currentSelectedColumn;

    private Dimension preferredToolbarButtonSize = new Dimension(80, 50);
    private Dimension preferredSqleditorButtonSize = new Dimension(30, 30);

    private List<String> oldSqlCommands = new ArrayList<String>();
    private ToolsPanelController toolsPanelController;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public SpatialiteController( GuiBridgeHandler guiBridge ) {
        this.guiBridge = guiBridge;
        setPreferredSize(new Dimension(900, 600));

        HashMap<String, String> prefsMapTmp = guiBridge.getSpatialToolboxPreferencesMap();
        if (prefsMapTmp != null) {
            prefsMap = (HashMap) prefsMapTmp;
        }

        init();
    }

    protected void preInit() {

    }

    @SuppressWarnings({"unchecked", "serial"})
    private void init() {
        preInit();

        _sqlEditorArea.setDocument(new SqlDocument());

        _newDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _newDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _newDbButton.setText(NEW);
        _newDbButton.setToolTipText(NEW_TOOLTIP);
        _newDbButton.setPreferredSize(preferredToolbarButtonSize);
        _newDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.NEW_DATABASE));
        _newDbButton.addActionListener(e -> {
            createNewDatabase();
        });

        _connectDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _connectDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _connectDbButton.setText(CONNECT);
        _connectDbButton.setToolTipText(CONNECT_TOOLTIP);
        _connectDbButton.setPreferredSize(preferredToolbarButtonSize);
        _connectDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.CONNECT));
        _connectDbButton.addActionListener(e -> {
            openDatabase();
        });

        _disconnectDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _disconnectDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _disconnectDbButton.setText(DISCONNECT);
        _disconnectDbButton.setToolTipText(DISCONNECT_TOOLTIP);
        _disconnectDbButton.setPreferredSize(preferredToolbarButtonSize);
        _disconnectDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.DISCONNECT));
        _disconnectDbButton.addActionListener(e -> {
            try {
                closeCurrentDb();
            } catch (Exception e1) {
                logger.error("ERROR", e1);
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
                logger.error("ERROR", e1);
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
                String[] sqlHistory = SqlTemplates.templatesMap.keySet().toArray(new String[0]);
                String selected = GuiUtilities.showComboDialog(this, "HISTORY", "", sqlHistory);
                if (selected != null) {
                    String sql = SqlTemplates.templatesMap.get(selected);
                    addTextToQueryEditor(sql);
                }
            } catch (Exception e1) {
                logger.error("ERROR", e1);
            }
        });

        _shpButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _shpButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _shpButton.setText(SHAPEFILE);
        _shpButton.setToolTipText(SHAPEFILE_TOOLTIP);
        _shpButton.setPreferredSize(preferredToolbarButtonSize);
        _shpButton.setIcon(ImageCache.getInstance().getImage(ImageCache.VECTOR));
        _shpButton.addActionListener(e -> {
            try {
                // TODO
            } catch (Exception e1) {
                logger.error("ERROR", e1);
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
                        setIcon(ImageCache.getInstance().getImage(ImageCache.DATABASE));
                    } else if (value instanceof TypeLevel) {
                        setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_FOLDER));
                    } else if (value instanceof TableLevel) {
                        TableLevel tableLevel = (TableLevel) value;
                        if (tableLevel.isGeo) {
                            setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_SPATIAL));
                        } else {
                            setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE));
                        }
                    } else if (value instanceof ColumnLevel) {
                        ColumnLevel columnLevel = (ColumnLevel) value;
                        if (columnLevel.isPK) {
                            setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_PRIMARYKEY));
                        } else if (columnLevel.references != null) {
                            setIcon(ImageCache.getInstance().getImage(ImageCache.TABLE_COLUMN_INDEX));
                        } else if (columnLevel.geomColumn != null) {
                            SpatialiteGeometryType gType = SpatialiteGeometryType.forValue(columnLevel.geomColumn.geometry_type);
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
                    currentSelectedTable = null;
                    currentSelectedColumn = null;
                    if (paths.length > 0) {
                        Object selectedItem = paths[0].getLastPathComponent();
                        if (selectedItem instanceof TableLevel) {
                            currentSelectedTable = (TableLevel) selectedItem;

                            try {
                                QueryResult queryResult = currentConnectedDatabase
                                        .getTableRecordsMapIn(currentSelectedTable.tableName, null, true, 20, -1);
                                loadDataViewer(queryResult);
                            } catch (Exception e) {
                                e.printStackTrace();
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
            JGTLogger.logError(this, "Error", e1);
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
                if (sqlText.toLowerCase().startsWith("select") || sqlText.toLowerCase().startsWith("pragma")) {
                    File[] saveFiles = guiBridge.showSaveFileDialog("Select file to save to", GuiUtilities.getLastFile());
                    if (saveFiles != null && saveFiles.length > 0) {
                        try {
                            GuiUtilities.setLastPath(saveFiles[0].getAbsolutePath());
                        } catch (Exception e1) {
                            logger.error("ERROR", e1);
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
                    File[] saveFiles = guiBridge.showSaveFileDialog("Select shapefile to save to", GuiUtilities.getLastFile());
                    if (saveFiles != null && saveFiles.length > 0) {
                        try {
                            GuiUtilities.setLastPath(saveFiles[0].getAbsolutePath());
                        } catch (Exception e1) {
                            logger.error("ERROR", e1);
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

        _viewQueryButton.setIcon(ImageCache.getInstance().getImage(ImageCache.GLOBE));
        _viewQueryButton.setToolTipText(VIEW_QUERY_TOOLTIP);
        _viewQueryButton.setText("");
        _viewQueryButton.setPreferredSize(preferredSqleditorButtonSize);
        _viewQueryButton.addActionListener(e -> {

            File selectedFile = null;
            String sqlText = _sqlEditorArea.getText().trim();
            if (sqlText.length() > 0) {
                if (!sqlText.toLowerCase().startsWith("select")) {
                    JOptionPane.showMessageDialog(this, "Writing to shapefile is allowed only for SELECT statements.", "WARNING",
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

        _clearSqlEditorbutton.setIcon(ImageCache.getInstance().getImage(ImageCache.TRASH));
        _clearSqlEditorbutton.setToolTipText(CLEAR_SQL_EDITOR);
        _clearSqlEditorbutton.setText("");
        _clearSqlEditorbutton.setPreferredSize(preferredSqleditorButtonSize);
        _clearSqlEditorbutton.addActionListener(e -> {
            _sqlEditorArea.setText("");
        });
    }

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
                    Action[] tableActions = makeTableAction(currentSelectedTable);
                    for( Action action : tableActions ) {
                        JMenuItem item = new JMenuItem(action);
                        popupMenu.add(item);
                        item.setHorizontalTextPosition(JMenuItem.RIGHT);
                    }
                } else if (currentSelectedColumn != null) {
                    if (currentSelectedColumn.references != null) {
                        Action[] columnActions = makeFKColumnAction(currentSelectedColumn);
                        for( Action action : columnActions ) {
                            JMenuItem item = new JMenuItem(action);
                            popupMenu.add(item);
                            item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        }
                    } else {
                        Action[] columnActions = makeColumnAction(currentSelectedColumn);
                        for( Action action : columnActions ) {
                            JMenuItem item = new JMenuItem(action);
                            popupMenu.add(item);
                            item.setHorizontalTextPosition(JMenuItem.RIGHT);
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

    private void loadDataViewer( QueryResult queryResult ) {
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
    }

    private void layoutTree( DbLevel dbLevel, boolean expandNodes ) {
        toggleButtonsEnabling(dbLevel != null);

        String title;
        if (dbLevel != null) {
            _databaseTree.setVisible(true);
            title = dbLevel.dbName;
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
        _shpButton.setEnabled(enable);
        _clearSqlEditorbutton.setEnabled(enable);

        _sqlEditorArea.setEditable(enable);
    }

    private void expandAllNodes( JTree tree, int startingIndex, int rowCount ) {
        for( int i = startingIndex; i < rowCount; ++i ) {
            tree.expandRow(i);
        }

        if (tree.getRowCount() != rowCount) {
            expandAllNodes(tree, rowCount, tree.getRowCount());
        }
    }

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
        // String ramLevel = _heapCombo.getSelectedItem().toString();
        // prefsMap.put(GuiBridgeHandler.DEBUG_KEY, _debugCheckbox.isSelected() + "");
        // prefsMap.put(GuiBridgeHandler.HEAP_KEY, ramLevel);
        // guiBridge.setSpatialToolboxPreferencesMap(prefsMap);
        //
        // removeMouseListenerFromContext(pPanel);
        // if (pPanel != null)
        // pPanel.freeResources();
    }

    private void createNewDatabase() {
        try {
            closeCurrentDb();
        } catch (Exception e1) {
            logger.error("Error closing the database...", e1);
        }

        File[] saveFiles = guiBridge.showSaveFileDialog("Create new database", GuiUtilities.getLastFile());
        if (saveFiles != null && saveFiles.length > 0) {
            try {
                GuiUtilities.setLastPath(saveFiles[0].getAbsolutePath());
            } catch (Exception e1) {
                logger.error("ERROR", e1);
            }
        } else {
            return;
        }

        final File selectedFile = saveFiles[0];
        if (selectedFile != null) {

            final LogConsoleController logConsole = new LogConsoleController(pm);
            JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

            new Thread(() -> {
                logConsole.beginProcess("Create new database");

                try {
                    currentConnectedDatabase = new SpatialiteDb();
                    currentConnectedDatabase.open(selectedFile.getAbsolutePath());
                    currentConnectedDatabase.initSpatialMetadata(null);

                    DbLevel dbLevel = gatherDatabaseLevels(currentConnectedDatabase);

                    layoutTree(dbLevel, false);
                } catch (Exception e) {
                    currentConnectedDatabase = null;
                    logger.error("Error connecting to the database...", e);
                } finally {
                    logConsole.finishProcess();
                    logConsole.stopLogging();
                    logConsole.setVisible(false);
                    window.dispose();
                }
            }).start();

        }
    }

    private void setDbTreeTitle( String title ) {
        Border databaseTreeViewBorder = _databaseTreeView.getBorder();
        if (databaseTreeViewBorder instanceof TitledBorder) {
            TitledBorder tBorder = (TitledBorder) databaseTreeViewBorder;
            tBorder.setTitle(title);
        }
    }

    private void openDatabase() {
        try {
            closeCurrentDb();
        } catch (Exception e1) {
            logger.error("Error closign the database...", e1);
        }

        File[] openFiles = guiBridge.showOpenFileDialog("Open database", GuiUtilities.getLastFile());
        if (openFiles != null && openFiles.length > 0) {
            try {
                GuiUtilities.setLastPath(openFiles[0].getAbsolutePath());
            } catch (Exception e1) {
                logger.error("ERROR", e1);
            }
        } else {
            return;
        }

        final File selectedFile = openFiles[0];
        if (selectedFile != null) {
            final LogConsoleController logConsole = new LogConsoleController(pm);
            JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

            new Thread(() -> {
                logConsole.beginProcess("Open database");

                try {
                    currentConnectedDatabase = new SpatialiteDb();
                    currentConnectedDatabase.open(selectedFile.getAbsolutePath());

                    DbLevel dbLevel = gatherDatabaseLevels(currentConnectedDatabase);
                    setDbTreeTitle(dbLevel.dbName);

                    layoutTree(dbLevel, true);
                } catch (Exception e) {
                    currentConnectedDatabase = null;
                    logger.error("Error connecting to the database...", e);
                } finally {
                    logConsole.finishProcess();
                    logConsole.stopLogging();
                    logConsole.setVisible(false);
                    window.dispose();
                }
            }).start();
        }

    }

    private DbLevel gatherDatabaseLevels( SpatialiteDb db ) throws SQLException {
        currentDbLevel = new DbLevel();
        String databasePath = db.getDatabasePath();
        File dbFile = new File(databasePath);
        currentDbLevel.dbName = dbFile.getName();

        HashMap<String, List<String>> currentDatabaseTablesMap = db.getTablesMap(true);
        for( String typeName : SpatialiteTableNames.ALL_TYPES_LIST ) {
            TypeLevel typeLevel = new TypeLevel();
            typeLevel.typeName = typeName;
            List<String> tablesList = currentDatabaseTablesMap.get(typeName);
            for( String tableName : tablesList ) {
                TableLevel tableLevel = new TableLevel();
                tableLevel.parent = currentDbLevel;
                tableLevel.tableName = tableName;

                SpatialiteGeometryColumns geometryColumns = null;
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
                    if (geometryColumns != null && columnName.equalsIgnoreCase(geometryColumns.f_geometry_column)) {
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

    private void closeCurrentDb() throws Exception {
        layoutTree(null, false);
        loadDataViewer(null);
        if (currentConnectedDatabase != null) {
            currentConnectedDatabase.close();
            currentConnectedDatabase = null;
        }

    }

    private boolean runQuery( String sqlText, IJGTProgressMonitor pm ) {
        boolean hasError = false;
        if (currentConnectedDatabase != null && sqlText.length() > 0) {
            try {
                pm.beginTask("Run query: " + sqlText, IJGTProgressMonitor.UNKNOWN);
                int limit = -1;

                if (sqlText.toLowerCase().startsWith("select") || sqlText.toLowerCase().startsWith("pragma")) {
                    limit = 5000;
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
        return hasError;
    }

    private boolean runQueryToFile( String sqlText, File selectedFile, IJGTProgressMonitor pm ) {
        boolean hasError = false;
        if (currentConnectedDatabase != null && sqlText.length() > 0) {
            try {
                pm.beginTask("Run query: " + sqlText + "\ninto file: " + selectedFile, IJGTProgressMonitor.UNKNOWN);
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

    private boolean runQueryToShapefile( String sqlText, File selectedFile, IJGTProgressMonitor pm ) {
        boolean hasError = false;
        if (currentConnectedDatabase != null && sqlText.length() > 0) {
            try {
                pm.beginTask("Run query: " + sqlText + "\ninto shapefile: " + selectedFile, IJGTProgressMonitor.UNKNOWN);
                DefaultFeatureCollection fc = currentConnectedDatabase.runRawSqlToFeatureCollection(sqlText);
                OmsVectorWriter.writeVector(selectedFile.getAbsolutePath(), fc);
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

    private boolean viewSpatialQueryResult( String sqlText, IJGTProgressMonitor pm ) {
        boolean hasError = false;
        if (currentConnectedDatabase != null && sqlText.length() > 0) {
            try {
                pm.beginTask("Run query: " + sqlText, IJGTProgressMonitor.UNKNOWN);
                DefaultFeatureCollection fc = currentConnectedDatabase.runRawSqlToFeatureCollection(sqlText);
                ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(fc, NwwUtilities.GPS_CRS);
                if (toolsPanelController == null)
                    toolsPanelController = SimpleNwwViewer.openNww(null, JFrame.DO_NOTHING_ON_CLOSE);
                toolsPanelController.loadFeatureCollection(null, "QueryLayer", null, rfc);

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

    private void addTextToQueryEditor( String newText ) {
        String text = _sqlEditorArea.getText();
        if (text.trim().length() != 0) {
            text += "\n";
        }
        text += newText;
        _sqlEditorArea.setText(text);
    }

    private void addQueryToHistoryCombo( String sqlText ) {
        if (oldSqlCommands.contains(sqlText)) {
            oldSqlCommands.remove(sqlText);
        }
        oldSqlCommands.add(0, sqlText);
        if (oldSqlCommands.size() > 20) {
            oldSqlCommands.remove(20);
        }
    }

    @SuppressWarnings("serial")
    private Action[] makeFKColumnAction( final ColumnLevel selectedColumn ) {
        Action[] actions = {//
                new AbstractAction("Create combined select statement"){
                    @Override
                    public void actionPerformed( ActionEvent e ) {

                        String[] tableColsFromFK = selectedColumn.tableColsFromFK();
                        String refTable = tableColsFromFK[0];
                        String refColumn = tableColsFromFK[1];
                        String tableName = selectedColumn.parent.tableName;
                        String query = "SELECT t1.*, t2.* FROM " + tableName + " t1, " + refTable + " t2" + "\nWHERE t1."
                                + selectedColumn.columnName + "=t2." + refColumn;
                        addTextToQueryEditor(query);

                    }
                }, //
                new AbstractAction("Quick view other table"){
                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        try {
                            String[] tableColsFromFK = selectedColumn.tableColsFromFK();
                            String refTable = tableColsFromFK[0];
                            QueryResult queryResult = currentConnectedDatabase.getTableRecordsMapIn(refTable, null, true, 20, -1);
                            loadDataViewer(queryResult);
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                }, //
        };
        return actions;
    }

    @SuppressWarnings("serial")
    private Action[] makeColumnAction( final ColumnLevel selectedColumn ) {
        Action[] actions = {//
                new AbstractAction("Create select statement on column"){
                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        String query;
                        if (selectedColumn.geomColumn != null) {
                            query = "SELECT AsBinary(" + selectedColumn.columnName + ") FROM " + selectedColumn.parent.tableName;
                        } else {
                            query = "SELECT " + selectedColumn.columnName + " FROM " + selectedColumn.parent.tableName;
                        }
                        addTextToQueryEditor(query);

                    }
                }, //
        };
        return actions;
    }

    @SuppressWarnings("serial")
    private Action[] makeTableAction( final TableLevel selectedTable ) {
        boolean hasFK = false;
        for( ColumnLevel col : selectedTable.columnsList ) {
            if (col.references != null) {
                hasFK = true;
                break;
            }
        }

        int size = 2;
        if (hasFK)
            size++;
        if (selectedTable.isGeo)
            size++;

        List<Action> actions = new ArrayList<>();
        int index = 0;
        AbstractAction action = new AbstractAction("Create select statement"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    String query = SpatialiteGuiUtils.getSelectQuery(currentConnectedDatabase, selectedTable, false);
                    addTextToQueryEditor(query);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        };
        actions.add(action);
        action = new AbstractAction("Count table records"){
            @Override
            public void actionPerformed( ActionEvent e ) {
                try {
                    String tableName = selectedTable.tableName;
                    long count = currentConnectedDatabase.getCount(tableName);
                    JOptionPane.showMessageDialog(getParent(), "Count: " + count);
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
        };
        actions.add(action);
        if (selectedTable.isGeo) {
            action = new AbstractAction("Quick View Table"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    try {
                        String query = SpatialiteGuiUtils.getSelectQuery(currentConnectedDatabase, selectedTable, false);
                        viewSpatialQueryResult(query, pm);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                }
            };
            actions.add(action);
        }
        // if (hasFK) {
        // actions[index++] = new Action("Show Foreign Keys Diagram", null){
        // @Override
        // public void run() {
        //
        // try {
        // HashMap<String, TableLevel> name2LevelMap = new HashMap<>();
        // List<TypeLevel> typesList = currentDbLevel.typesList;
        // for( TypeLevel typeLevel : typesList ) {
        // if (typeLevel.typeName.equals(SpatialiteTableNames.USERDATA)) {
        // List<TableLevel> tablesList = typeLevel.tablesList;
        // for( TableLevel tableLevel : tablesList ) {
        // name2LevelMap.put(tableLevel.tableName, tableLevel);
        // }
        // }
        // }
        //
        // List<TableLevel> tablesInvolved = new ArrayList<>();
        // tablesInvolved.add(selectedTable);
        // for( ColumnLevel col : selectedTable.columnsList ) {
        // if (col.references != null) {
        // String[] tableColsFromFK = col.tableColsFromFK();
        // TableLevel tableLevel = name2LevelMap.get(tableColsFromFK[0]);
        // if (tableLevel != null && !tablesInvolved.contains(tableLevel)) {
        // tablesInvolved.add(tableLevel);
        // }
        // }
        // }
        //
        // int tablesNum = tablesInvolved.size();
        //
        // int gridCols = 4;
        // int gridRows = tablesNum / gridCols + 1;
        //
        // int indent = 10;
        // int tableWidth = 350;
        // int tableHeight = 300;
        //
        // JSONArray root = new JSONArray();
        // int tabesIndex = 0;
        // int runningX = 0;
        // int runningY = 10;
        // for( int gridRow = 0; gridRow < gridRows; gridRow++ ) {
        // runningX = indent;
        // for( int gridCol = 0; gridCol < gridCols; gridCol++ ) {
        // if (tabesIndex == tablesNum) {
        // break;
        // }
        //
        // JSONObject tableJson = new JSONObject();
        // root.put(tableJson);
        // TableLevel curTable = tablesInvolved.get(tabesIndex);
        // int id = tabesIndex;
        // tabesIndex++;
        //
        // tableJson.put("id", id);
        // tableJson.put("x", runningX);
        // tableJson.put("y", runningY);
        //
        // String fromTable = curTable.tableName;
        // tableJson.put("name", fromTable);
        // JSONArray fieldsArray = new JSONArray();
        // tableJson.put("fields", fieldsArray);
        // List<ColumnLevel> cols = curTable.columnsList;
        //
        // for( ColumnLevel col : cols ) {
        // JSONObject colObject = new JSONObject();
        // fieldsArray.put(colObject);
        // colObject.put("fname", col.columnName);
        // if (col.references != null) {
        // String[] tableColsFromFK = col.tableColsFromFK();
        // String toTable = tableColsFromFK[0];
        // String toColumn = tableColsFromFK[1];
        // colObject.put("fk_name", toTable);
        // colObject.put("fk_field", toColumn);
        // }
        // }
        //
        // runningX += indent + tableWidth;
        // }
        // runningY += tableHeight + indent;
        // }
        // String json = root.toString();
        // // String string = root.toString(2);
        // TableGraphDialog d = new TableGraphDialog(parentShell, "Table Graph", json);
        // d.open();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        //
        // }
        // };
        // }
        // if (selectedTable.isGeo) {
        // actions[index++] = new AbstractAction("Quick View Table"){
        // @Override
        // public void actionPerformed( ActionEvent e ) {
        // try {
        // String selectQuery = SpatialiteGuiUtils.getSelectQuery(currentConnectedDatabase,
        // selectedTable, true);
        // QueryResult queryResult =
        // currentConnectedDatabase.getTableRecordsMapFromRawSql(selectQuery, -1);
        //
        // List<ColumnLevel> colsList = selectedTable.columnsList;
        // int epsg = 4326;
        // for( ColumnLevel columnLevel : colsList ) {
        // if (columnLevel.geomColumn != null) {
        // epsg = columnLevel.geomColumn.srid;
        // break;
        // }
        // }
        //
        // CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:" + epsg);
        // CoordinateReferenceSystem targetCRS = DefaultGeographicCRS.WGS84;
        //
        // SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        // b.setName("geojson");
        // b.setCRS(sourceCRS);
        // b.add("geometry", Geometry.class);
        //
        // for( int j = 1; j < queryResult.names.size(); j++ ) {
        // String colName = queryResult.names.get(j);
        // b.add(colName, String.class);
        // }
        //
        // SimpleFeatureType type = b.buildFeatureType();
        // SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        //
        // DefaultFeatureCollection fc = new DefaultFeatureCollection();
        // List<Object[]> tableData = queryResult.data;
        // for( Object[] objects : tableData ) {
        // // first needs to be the geometry
        // if (!(objects[0] instanceof Geometry)) {
        // MessageDialog.openError(parentShell, "ERROR",
        // "The first column of the result of the query needs to be a geometry.");
        // return;
        // }
        // builder.addAll(objects);
        // SimpleFeature feature = builder.buildFeature(null);
        // fc.add(feature);
        // }
        //
        // ReprojectingFeatureCollection rfc = new ReprojectingFeatureCollection(fc, targetCRS);
        //
        // FeatureJSON fjson = new FeatureJSON();
        // StringWriter writer = new StringWriter();
        // fjson.writeFeatureCollection(rfc, writer);
        // String geojson = writer.toString();
        //
        // // System.out.println(geojson);
        //
        // QuickGeometryViewDialog d = new QuickGeometryViewDialog(parentShell,
        // "Data Viewer: " + selectedTable.tableName, geojson);
        // d.open();
        // } catch (Exception e) {
        // e.printStackTrace();
        // }
        //
        // }
        // };
        // }
        return actions.toArray(new Action[0]);
    }

    public static void main( String[] args ) throws Exception {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final SpatialiteController controller = new SpatialiteController(gBridge);
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "JGrasstools' Spatialite Viewer");

        Class<SpatialiteController> class1 = SpatialiteController.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/jgrasstools/images/hm150.png"));
        frame.setIconImage(icon.getImage());

        GuiUtilities.addClosingListener(frame, controller);
    }

}
