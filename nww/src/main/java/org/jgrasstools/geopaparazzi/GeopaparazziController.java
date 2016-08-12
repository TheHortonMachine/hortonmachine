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
package org.jgrasstools.geopaparazzi;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JTextPane;
import javax.swing.JTree;
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
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog.GpsLog;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoImages;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.Image;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TimeUtilities;
import org.jgrasstools.gears.libs.logging.JGTLogger;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
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
public abstract class GeopaparazziController extends GeopaparazziView implements IOnCloseListener {
    private static final Logger logger = LoggerFactory.getLogger(GeopaparazziView.class);
    private static final long serialVersionUID = 1L;
    private static boolean hasDriver = false;

    static {
        try {
            // make sure sqlite driver are there
            Class.forName("org.sqlite.JDBC");
            hasDriver = true;
        } catch (Exception e) {
        }
    }

    protected HashMap<String, String> prefsMap = new HashMap<>();

    protected GuiBridgeHandler guiBridge;
    protected IJGTProgressMonitor pm = new LogProgressMonitor();
    private List<ProjectInfo> projectInfos = new ArrayList<>();

    private ProjectInfo currentSelectedProject = null;
    private Image currentSelectedImage = null;
    private GpsLog currentSelectedGpsLog = null;

    private Dimension preferredButtonSize = new Dimension(30, 30);
    private JTextPane _infoArea;

    @SuppressWarnings({"unchecked", "rawtypes"})
    public GeopaparazziController( GuiBridgeHandler guiBridge ) {
        this.guiBridge = guiBridge;
        setPreferredSize(new Dimension(900, 600));

        HashMap<String, String> prefsMapTmp = guiBridge.getGeopaparazziProjectViewerPreferencesMap();
        if (prefsMapTmp != null) {
            prefsMap = (HashMap) prefsMapTmp;
        }

        init();
    }

    @SuppressWarnings({"serial"})
    private void init() {

        _infoArea = new JTextPane();
        // _infoArea.setDocument(new SqlDocument());
        _infoArea.setContentType("text/html");
        _infoArea.setEditable(false);
        _infoScroll.setViewportView(_infoArea);
//        _infoScroll.setMinimumSize(new Dimension(10, 200));

        _loadFolderButton.setIcon(ImageCache.getInstance().getImage(ImageCache.REFRESH));
        _loadFolderButton.setText("");
        _loadFolderButton.setPreferredSize(preferredButtonSize);
        _loadFolderButton.addActionListener(e -> {
            final File geopaparazziFolder = new File(_projectsFolderTextfield.getText());
            if (!geopaparazziFolder.exists()) {
                GuiUtilities.showWarningMessage(this, null, "The projects folder doesn't exist.");
                return;
            }
            File[] projectFiles = geopaparazziFolder.listFiles(new FilenameFilter(){
                @Override
                public boolean accept( File dir, String name ) {
                    return name.endsWith(".gpap");
                }
            });
            Arrays.sort(projectFiles, Collections.reverseOrder());

            try {
                projectInfos = readProjectInfos(projectFiles);
                layoutTree(projectInfos, false);
            } catch (Exception e1) {
                e1.printStackTrace();
            }

        });

        _projectsFolderBrowseButton.setPreferredSize(preferredButtonSize);
        _projectsFolderBrowseButton.addActionListener(e -> {
            File[] openFiles = guiBridge.showOpenDirectoryDialog("Open projects folder", GuiUtilities.getLastFile());
            if (openFiles != null && openFiles.length > 0) {
                try {
                    GuiUtilities.setLastPath(openFiles[0].getAbsolutePath());
                } catch (Exception e1) {
                    logger.error("ERROR", e1);
                }
            } else {
                return;
            }

            _projectsFolderTextfield.setText(openFiles[0].getAbsolutePath());
        });

        String lastSavedPath = prefsMap.get(GuiBridgeHandler.LAST_GP_PROJECTS_PATH);
        _projectsFolderTextfield.setText(lastSavedPath);
        
        
        _filterTextfield.addKeyListener(new KeyAdapter(){
            @Override
            public void keyReleased( KeyEvent e ) {
                String filterText = _filterTextfield.getText();

                final List<ProjectInfo> filtered = new ArrayList<ProjectInfo>();
                if (filterText == null) {
                    filtered.addAll(projectInfos);
                } else {
                    for( ProjectInfo projectInfo : projectInfos ) {
                        if (projectInfo.fileName.contains(filterText) || projectInfo.metadata.contains(filterText)) {
                            filtered.add(projectInfo);
                        }
                    }
                }

                layoutTree(filtered, false);
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

            _databaseTree.setRootVisible(false);
            _databaseTree.setCellRenderer(new DefaultTreeCellRenderer(){
                @Override
                public java.awt.Component getTreeCellRendererComponent( JTree tree, Object value, boolean selected,
                        boolean expanded, boolean leaf, int row, boolean hasFocus ) {

                    super.getTreeCellRendererComponent(tree, value, selected, expanded, leaf, row, hasFocus);

                    if (value instanceof ProjectInfo) {
                        setIcon(ImageCache.getInstance().getImage(ImageCache.DATABASE));
                    } else if (value instanceof Image) {
                        setIcon(ImageCache.getInstance().getImage(ImageCache.DBIMAGE));
                    } else if (value instanceof GpsLog) {
                        setIcon(ImageCache.getInstance().getImage(ImageCache.LOG));
                    }

                    return this;
                }

            });

            _databaseTree.addTreeSelectionListener(new TreeSelectionListener(){
                public void valueChanged( TreeSelectionEvent evt ) {
                    TreePath[] paths = evt.getPaths();
                    currentSelectedProject = null;
                    currentSelectedImage = null;
                    currentSelectedGpsLog = null;
                    if (paths.length > 0) {
                        Object selectedItem = paths[0].getLastPathComponent();
                        if (selectedItem instanceof ProjectInfo) {
                            currentSelectedProject = (ProjectInfo) selectedItem;
                            selectProjectInfo(currentSelectedProject);
                        }
                        if (selectedItem instanceof Image) {
                            currentSelectedImage = (Image) selectedItem;
                            selectImage(currentSelectedImage);
                        }
                        if (selectedItem instanceof GpsLog) {
                            currentSelectedGpsLog = (GpsLog) selectedItem;
                            selectGpsLog(currentSelectedGpsLog);
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

    private List<ProjectInfo> readProjectInfos( File[] projectFiles ) throws Exception {
        List<ProjectInfo> infoList = new ArrayList<ProjectInfo>();
        for( File geopapDatabaseFile : projectFiles ) {
            try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + geopapDatabaseFile.getAbsolutePath())) {
                String projectInfo = GeopaparazziWorkspaceUtilities.getProjectInfo(connection);
                ProjectInfo info = new ProjectInfo();
                info.databaseFile = geopapDatabaseFile;
                info.fileName = geopapDatabaseFile.getName();
                info.metadata = projectInfo;

                List<org.jgrasstools.gears.io.geopaparazzi.geopap4.Image> imagesList = DaoImages.getImagesList(connection);
                info.images = imagesList.toArray(new org.jgrasstools.gears.io.geopaparazzi.geopap4.Image[0]);

                List<GpsLog> logsList = DaoGpsLog.getLogsList(connection);
                info.logs = logsList;
                infoList.add(info);
            }
        }
        return infoList;
    }

    protected abstract void setViewQueryButton( JButton _viewQueryButton, Dimension preferredButtonSize,
            JTextPane sqlEditorArea );

    private void addJtreeDragNDrop() {
        _databaseTree.setDragEnabled(true);
        _databaseTree.setTransferHandler(new TransferHandler(null){
            public int getSourceActions( JComponent c ) {
                return COPY;
            }
            protected Transferable createTransferable( JComponent c ) {
                if (c instanceof JTree) {
                    if (currentSelectedImage != null) {
                        return new StringSelection(currentSelectedImage.getName());
                    } else if (currentSelectedGpsLog != null) {
                        return new StringSelection(currentSelectedGpsLog.text);
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
                if (currentSelectedImage != null) {
                    List<Action> tableActions = makeTableAction(currentSelectedImage);
                    for( Action action : tableActions ) {
                        if (action != null) {
                            JMenuItem item = new JMenuItem(action);
                            popupMenu.add(item);
                            item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        } else {
                            popupMenu.add(new JSeparator());
                        }
                    }
                } else if (currentSelectedProject != null) {
                    List<Action> tableActions = makeDatabaseAction(currentSelectedProject);
                    for( Action action : tableActions ) {
                        if (action != null) {
                            JMenuItem item = new JMenuItem(action);
                            popupMenu.add(item);
                            item.setHorizontalTextPosition(JMenuItem.RIGHT);
                        } else {
                            popupMenu.add(new JSeparator());
                        }
                    }
                } else if (currentSelectedGpsLog != null) {
                    List<Action> columnActions = makeColumnActions(currentSelectedGpsLog);
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

    private void layoutTree( List<ProjectInfo> projectInfos, boolean expandNodes ) {

        if (projectInfos != null) {
            _databaseTree.setVisible(true);
        } else {
            projectInfos = new ArrayList<>();
            _databaseTree.setVisible(false);
        }

        ObjectTreeModel model = new ObjectTreeModel();
        model.setRoot(projectInfos);
        _databaseTree.setModel(model);

        if (expandNodes) {
            _databaseTree.expandRow(0);
            // _databaseTree.expandRow(1);
        }
        // expandAllNodes(_databaseTree, 0, 2);

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

        private List<ProjectInfo> root;
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
        public void setRoot( List<ProjectInfo> v ) {
            List<ProjectInfo> oldRoot = v;
            root = v;
            fireTreeStructureChanged(oldRoot);
        }

        public Object getRoot() {
            return root;
        }

        @SuppressWarnings("rawtypes")
        public int getChildCount( Object parent ) {
            if (parent instanceof ProjectInfo) {
                ProjectInfo projectInfo = (ProjectInfo) parent;
                return projectInfo.images.length + projectInfo.logs.size();
            } else if (parent instanceof Image) {
                return 0;
            } else if (parent instanceof GpsLog) {
                return 0;
            } else if (parent instanceof List) {
                List list = (List) parent;
                return list.size();
            }
            return 0;
        }

        @SuppressWarnings("rawtypes")
        public Object getChild( Object parent, int index ) {
            if (parent instanceof ProjectInfo) {
                ProjectInfo projectInfo = (ProjectInfo) parent;

                int imagesCount = projectInfo.images.length;
                if (index > imagesCount - 1) {
                    return projectInfo.logs.get(index - imagesCount);
                } else {
                    return projectInfo.images[index];
                }

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

        try {
            String lastPath = _projectsFolderTextfield.getText();
            File file = new File(lastPath);
            if (file.exists() && file.isDirectory()) {
                prefsMap.put(GuiBridgeHandler.LAST_GP_PROJECTS_PATH, lastPath);
                guiBridge.setGeopaparazziProjectViewerPreferencesMap(prefsMap);
            }
        } catch (Exception e) {
            logger.error("Error", e);
        }
    }

    protected void setDbTreeTitle( String title ) {
        Border databaseTreeViewBorder = _databaseTreeView.getBorder();
        if (databaseTreeViewBorder instanceof TitledBorder) {
            TitledBorder tBorder = (TitledBorder) databaseTreeViewBorder;
            tBorder.setTitle(title);
        }
    }

    private void selectProjectInfo( Object selectedItem ) {
        currentSelectedProject = (ProjectInfo) selectedItem;
        try {
            /*
             * set the info view
             */
            String titleName = currentSelectedProject.fileName;
            titleName = titleName.replace('_', ' ').replaceFirst("\\.gpap", "");
            String text = titleName + "<br/><br/>" + currentSelectedProject.metadata;

            _infoArea.setText(text);

            // /*
            // * set the project view
            // */
            // String projectTemplate = getProjectTemplate();
            // // substitute the notes info
            // String projectHtml = setData(projectTemplate, currentSelectedProject);
            // if (CACHE_HTML_TO_FILE) {
            // projectHtml = FileUtilities.readFile(projectHtml);
            // }
            // dataBrowser.setText(projectHtml);
            //
            // if (projectHtml.contains("openGpImage")) {
            // new OpenImageFunction(dataBrowser, "openGpImage",
            // currentSelectedProject.databaseFile);
            // }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectImage( Object selectedItem ) {
        org.jgrasstools.gears.io.geopaparazzi.geopap4.Image selectedImage = (org.jgrasstools.gears.io.geopaparazzi.geopap4.Image) selectedItem;
        for( ProjectInfo projectInfo : projectInfos ) {
            for( org.jgrasstools.gears.io.geopaparazzi.geopap4.Image tmpImage : projectInfo.images ) {
                if (tmpImage.equals(selectedImage)) {
                    currentSelectedProject = projectInfo;
                    break;
                }
            }
        }

        try {
            String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(selectedImage.getTs()));
            String picInfo = "<b>Image:</b> " + GeopaparazziWorkspaceUtilities.escapeHTML(selectedImage.getName()) + "<br/>" //
                    + "<b>Timestamp:</b> " + dateTimeString + "<br/>" //
                    + "<b>Azimuth:</b> " + (int) selectedImage.getAzim() + " deg<br/>" //
                    + "<b>Altim:</b> " + (int) selectedImage.getAltim() + " m<br/>";
            _infoArea.setText(picInfo);

            // GeopaparazziUtilities.setImageInBrowser(dataBrowser, selectedImage.getId(),
            // selectedImage.getName(),
            // currentSelectedProject.databaseFile, IMAGE_KEY, SERVICE_HANDLER);
        } catch (Exception e) {
            e.printStackTrace();
            setNoProjectLabel();
        }
    }

    private void selectGpsLog( Object selectedItem ) {
        GpsLog selectedLog = (GpsLog) selectedItem;

        for( ProjectInfo projectInfo : projectInfos ) {
            if (projectInfo.logs.contains(selectedLog)) {
                currentSelectedProject = projectInfo;
                break;
            }
        }

        try {
            String startDateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(selectedLog.startTime));
            String endDateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(selectedLog.endTime));
            String picInfo = "<b>Gps log:</b> " + GeopaparazziWorkspaceUtilities.escapeHTML(selectedLog.text) + "<br/>" //
                    + "<b>Start time:</b> " + startDateTimeString + "<br/>" //
                    + "<b>End time:</b> " + endDateTimeString + "<br/>";
            _infoArea.setText(picInfo);

            // GeopaparazziUtilities.setLogChartInBrowser(dataBrowser, selectedLog,
            // currentSelectedProject.databaseFile, IMAGE_KEY,
            // SERVICE_HANDLER);
        } catch (Exception e) {
            e.printStackTrace();
            setNoProjectLabel();
        }
    }

    private void setNoProjectLabel() {
        _infoArea.setText("<h1>No project selected</h1>");
        // Label noModuleLabel = new Label(projectViewComposite, SWT.NONE);
        // noModuleLabel.setLayoutData(new GridData(SWT.CENTER, SWT.CENTER, true, true));
        // noModuleLabel.setData(RWT.MARKUP_ENABLED, Boolean.TRUE);
        // noModuleLabel.setText("<span style='font:bold 26px Arial;'>" + NO_MODULE_SELECTED +
        // "</span>");
        // return noModuleLabel;
    }

    protected abstract List<Action> makeColumnActions( final GpsLog selectedLog );

    protected abstract List<Action> makeDatabaseAction( final ProjectInfo project );

    protected abstract List<Action> makeTableAction( final Image selectedImage );

}
