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
package org.jgrasstools.gui.spatialite;

import java.awt.Dimension;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JTree;
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.border.TitledBorder;
import javax.swing.event.EventListenerList;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

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
import org.jgrasstools.gui.spatialite.objects.ColumnLevel;
import org.jgrasstools.gui.spatialite.objects.DbLevel;
import org.jgrasstools.gui.spatialite.objects.TableLevel;
import org.jgrasstools.gui.spatialite.objects.TypeLevel;
import org.jgrasstools.gui.utils.DefaultGuiBridgeImpl;
import org.jgrasstools.gui.utils.GuiBridgeHandler;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.jgrasstools.gui.utils.ImageCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The spatialtoolbox view controller.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class SpatialiteController extends SpatialiteView {
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
    private static final String SQL_EDITOR = "SQL Editor";
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

        _newDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _newDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _newDbButton.setText(NEW);
        _newDbButton.setToolTipText(NEW_TOOLTIP);
        _newDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.NEW_DATABASE));
        _newDbButton.addActionListener(e -> {
            createNewDatabase();
        });

        _connectDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _connectDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _connectDbButton.setText(CONNECT);
        _connectDbButton.setToolTipText(CONNECT_TOOLTIP);
        _connectDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.CONNECT));
        _connectDbButton.addActionListener(e -> {
            openDatabase();
        });

        _disconnectDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _disconnectDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _disconnectDbButton.setText(DISCONNECT);
        _disconnectDbButton.setToolTipText(DISCONNECT_TOOLTIP);
        _disconnectDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.DISCONNECT));
        _disconnectDbButton.addActionListener(e -> {
            try {
                closeCurrentDb();
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
                freeResources();
            }
        });

        try {
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

                    for( int i = 0; i < paths.length; i++ ) {
                        Object selectedItem = paths[i].getLastPathComponent();
                        if (selectedItem instanceof TableLevel) {
                            currentSelectedTable = (TableLevel) selectedItem;

                            try {
                                QueryResult queryResult = currentConnectedDatabase
                                        .getTableRecordsMapIn(currentSelectedTable.tableName, null, true, 20, -1);
                                loadDataViewer(queryResult);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        } else if (selectedItem instanceof ColumnLevel) {
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
    }

    private void loadDataViewer( QueryResult queryResult ) {
        Object[] names = queryResult.names.toArray(new String[0]);
        List<Object[]> data = queryResult.data;
        Object[][] values = new Object[queryResult.data.size()][];
        int index = 0;
        for( Object[] objects : data ) {
            values[index++]=objects;
        }

        _dataViewerTable.setModel(new DefaultTableModel(values, names));
    }

    private void layoutTree( DbLevel dbLevel, boolean expandNodes ) {

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

        if (expandNodes)
            expandAllNodes(_databaseTree, 0, _databaseTree.getRowCount());
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

    public void freeResources() {
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

            new Thread(new Runnable(){
                public void run() {
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

            new Thread(new Runnable(){
                public void run() {
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
        if (currentConnectedDatabase != null) {
            currentConnectedDatabase.close();
            currentConnectedDatabase = null;
        }

        layoutTree(null, false);
    }

    public static void main( String[] args ) throws Exception {
        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final SpatialiteController controller = new SpatialiteController(gBridge);
        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "JGrasstools' Spatialite Viewer");

        Class<SpatialiteController> class1 = SpatialiteController.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/jgrasstools/images/hm150.png"));
        frame.setIconImage(icon.getImage());

        WindowListener exitListener = new WindowAdapter(){
            @Override
            public void windowClosing( WindowEvent e ) {
                int confirm = JOptionPane.showOptionDialog(frame, "Are you sure you want to exit?", "Exit Confirmation",
                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null, null, null);
                if (confirm == JOptionPane.YES_OPTION) {
                    controller.freeResources();
                    System.exit(0);
                }
            }
        };
        frame.addWindowListener(exitListener);
    }

}
