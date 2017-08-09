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
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPopupMenu;
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

import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.dbs.compat.ASpatialDb;
import org.jgrasstools.dbs.compat.GeometryColumn;
import org.jgrasstools.dbs.compat.objects.ColumnLevel;
import org.jgrasstools.dbs.compat.objects.DbLevel;
import org.jgrasstools.dbs.compat.objects.ForeignKey;
import org.jgrasstools.dbs.compat.objects.QueryResult;
import org.jgrasstools.dbs.compat.objects.TableLevel;
import org.jgrasstools.dbs.compat.objects.TypeLevel;
import org.jgrasstools.dbs.spatialite.ESpatialiteGeometryType;
import org.jgrasstools.dbs.spatialite.SpatialiteTableNames;
import org.jgrasstools.dbs.utils.CommonQueries;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.logging.JGTLogger;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.spatialite.GTSpatialiteThreadsafeDb;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gui.console.LogConsoleController;
import org.jgrasstools.gui.utils.GuiBridgeHandler;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.jgrasstools.gui.utils.GuiUtilities.IOnCloseListener;
import org.jgrasstools.gui.utils.ImageCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The spatialtoolbox view controller.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public abstract class SpatialiteController extends SpatialiteView implements IOnCloseListener {
    private static final Logger logger = LoggerFactory.getLogger(SpatialiteView.class);
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
    private static final String NEW_TOOLTIP = "create a new spatialite database";
    private static final String CONNECT = "connect";
    private static final String CONNECT_TOOLTIP = "connect to an existing spatialite database";
    private static final String DB_TREE_TITLE = "Database Connection";

    protected GuiBridgeHandler guiBridge;
    protected IJGTProgressMonitor pm = new LogProgressMonitor();
    protected ASpatialDb currentConnectedDatabase;
    private DbLevel currentDbLevel;
    protected DbLevel currentSelectedDb;
    protected TableLevel currentSelectedTable;
    protected ColumnLevel currentSelectedColumn;

    private Dimension preferredToolbarButtonSize = new Dimension(120, 65);
    private Dimension preferredSqleditorButtonSize = new Dimension(30, 30);

    private List<String> oldSqlCommands = new ArrayList<String>();

    public SpatialiteController( GuiBridgeHandler guiBridge ) {
        this.guiBridge = guiBridge;
        setPreferredSize(new Dimension(900, 600));
        init();
    }

    @SuppressWarnings({"serial"})
    private void init() {

        String[] oldSqlCommandsArray = GuiUtilities.getPreference("JGT_OLD_SQL_COMMANDS", new String[0]);
        for( String oldSql : oldSqlCommandsArray ) {
            oldSqlCommands.add(oldSql);
        }

        _limitCountTextfield.setText("1000");
        _limitCountTextfield.setToolTipText("1000 is default and used when no valid number is supplied. -1 means no limit.");

        _recordCountTextfield.setEditable(false);

        _dataViewerTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        addDataTableContextMenu();

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
            createNewDatabase();
        });

        _connectDbButton.setVerticalTextPosition(SwingConstants.BOTTOM);
        _connectDbButton.setHorizontalTextPosition(SwingConstants.CENTER);
        _connectDbButton.setText(CONNECT);
        _connectDbButton.setToolTipText(CONNECT_TOOLTIP);
        _connectDbButton.setPreferredSize(preferredToolbarButtonSize);
        _connectDbButton.setIcon(ImageCache.getInstance().getImage(ImageCache.CONNECT));
        _connectDbButton.addActionListener(e -> {
            openDatabase(null);
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
                String[] sqlTemplates = CommonQueries.templatesMap.keySet().toArray(new String[0]);
                String selected = GuiUtilities.showComboDialog(this, "TEMPLATES", "", sqlTemplates);
                if (selected != null) {
                    String sql = CommonQueries.templatesMap.get(selected);
                    addTextToQueryEditor(sql);
                }
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
                if (isSelectOrPragma(sqlText)) {
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
                JMenuItem item = new JMenuItem(new AbstractAction("Copy cells content"){
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                        _dataViewerTable.getTransferHandler().exportToClipboard(_dataViewerTable, clipboard,
                                TransferHandler.COPY);
                    }
                });
                popupMenu.add(item);
                item.setHorizontalTextPosition(JMenuItem.RIGHT);
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
            tableColumn.setPreferredWidth(preferredWidth);
        }

        _recordCountTextfield.setText(values.length + "");
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
            GuiUtilities.setPreference(SpatialiteGuiUtils.JGT_SPATIALITE_LAST_FILE, currentConnectedDatabase.getDatabasePath());
        }
        try {
            closeCurrentDb();
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }

    protected void createNewDatabase() {
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
                    currentConnectedDatabase = new GTSpatialiteThreadsafeDb();
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

    protected void setDbTreeTitle( String title ) {
        Border databaseTreeViewBorder = _databaseTreeView.getBorder();
        if (databaseTreeViewBorder instanceof TitledBorder) {
            TitledBorder tBorder = (TitledBorder) databaseTreeViewBorder;
            tBorder.setTitle(title);
            _databaseTreeView.repaint();
            _databaseTreeView.invalidate();
        }
    }

    protected void openDatabase( File file ) {
        try {
            closeCurrentDb();
        } catch (Exception e1) {
            logger.error("Error closing the database...", e1);
        }

        final File[] selectedFile = new File[1];
        if (file == null) {

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

            selectedFile[0] = openFiles[0];
        } else {
            selectedFile[0] = file;
        }
        if (selectedFile[0] != null) {
            final LogConsoleController logConsole = new LogConsoleController(pm);
            JFrame window = guiBridge.showWindow(logConsole.asJComponent(), "Console Log");

            new Thread(() -> {
                logConsole.beginProcess("Open database");

                try {
                    currentConnectedDatabase = new GTSpatialiteThreadsafeDb();
                    currentConnectedDatabase.open(selectedFile[0].getAbsolutePath());

                    DbLevel dbLevel = gatherDatabaseLevels(currentConnectedDatabase);

                    layoutTree(dbLevel, true);
                    setDbTreeTitle(currentConnectedDatabase.getDatabasePath());
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

    protected void closeCurrentDb() throws Exception {
        if (currentConnectedDatabase != null) {
            setDbTreeTitle(DB_TREE_TITLE);
            layoutTree(null, false);
            loadDataViewer(null);
            currentConnectedDatabase.close();
            currentConnectedDatabase = null;
        }
    }

    protected boolean runQuery( String sqlText, IJGTProgressMonitor pm ) {
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
                pm.beginTask("Run query: " + queryForLog, IJGTProgressMonitor.UNKNOWN);

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
                                int resultCode = currentConnectedDatabase.executeInsertUpdateDeleteSql(sql);
                                QueryResult dummyQueryResult = new QueryResult();
                                dummyQueryResult.names.add("Result = " + resultCode);
                                // loadDataViewer(dummyQueryResult);
                            }
                            // addQueryToHistoryCombo(sql);
                        }
                        if (!hasError && _refreshTreeAfterQueryCheckbox.isSelected()) {
                            try {
                                refreshDatabaseTree();
                            } catch (SQLException e) {
                                logger.error("error", e);
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
                logger.error("error", e);
            }
        }
        return hasError;
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

    protected boolean runQueryToFile( String sqlText, File selectedFile, IJGTProgressMonitor pm ) {
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

    protected boolean runQueryToShapefile( String sqlText, File selectedFile, IJGTProgressMonitor pm ) {
        boolean hasError = false;
        if (sqlText.trim().length() == 0) {
            return false;
        }
        if (currentConnectedDatabase instanceof GTSpatialiteThreadsafeDb) {
            try {
                pm.beginTask("Run query: " + sqlText + "\ninto shapefile: " + selectedFile, IJGTProgressMonitor.UNKNOWN);
                DefaultFeatureCollection fc = ((GTSpatialiteThreadsafeDb) currentConnectedDatabase)
                        .runRawSqlToFeatureCollection(sqlText);
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

        GuiUtilities.setPreference("JGT_OLD_SQL_COMMANDS", oldSqlCommands.toArray(new String[0]));
    }

    protected abstract List<Action> makeColumnActions( final ColumnLevel selectedColumn );

    protected abstract List<Action> makeDatabaseAction( final DbLevel dbLevel );

    protected abstract List<Action> makeTableAction( final TableLevel selectedTable );

    protected void refreshDatabaseTree() throws Exception {
        DbLevel dbLevel = gatherDatabaseLevels(currentConnectedDatabase);
        setDbTreeTitle(currentConnectedDatabase.getDatabasePath());
        layoutTree(dbLevel, true);
    }

}
