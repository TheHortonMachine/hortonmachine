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

import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_GPSLOGS;
import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_GPSLOG_DATA;
import static org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.TABLE_GPSLOG_PROPERTIES;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.EventListener;
import java.util.HashMap;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
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

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.GeodeticCalculator;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog.GpsLog;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoGpsLog.GpsPoint;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.GpsLogsDataTableFields;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.GpsLogsPropertiesTableFields;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TableDescriptions.GpsLogsTableFields;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoImages;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.DaoNotes;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.Image;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.Note;
import org.jgrasstools.gears.io.geopaparazzi.geopap4.TimeUtilities;
import org.jgrasstools.gears.libs.logging.JGTLogger;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.modules.v.smoothing.FeatureSlidingAverage;
import org.jgrasstools.gears.utils.ColorUtilities;
import org.jgrasstools.gears.utils.chart.Scatter;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gui.utils.GuiBridgeHandler;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.jgrasstools.gui.utils.GuiUtilities.IOnCloseListener;
import org.jgrasstools.nww.gui.NwwPanel;
import org.jgrasstools.nww.shapes.FeatureLine;
import org.jgrasstools.nww.shapes.FeaturePoint;
import org.jgrasstools.nww.shapes.InfoPoint;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.jgrasstools.gui.utils.ImageCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.LineString;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Path;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;

/**
 * The spatialtoolbox view controller.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public abstract class GeopaparazziController extends GeopaparazziView implements IOnCloseListener {
    private static final String RED_HEXA = "#FF0000";

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

    protected ProjectInfo currentSelectedProject = null;
    protected Image currentSelectedImage = null;
    protected GpsLog currentSelectedGpsLog = null;
    protected Note currentSelectedNote = null;

    private Dimension preferredButtonSize = new Dimension(30, 30);
    private JTextPane _infoArea;
    private RenderableLayer geopapDataLayer;
    private NwwPanel wwjPanel;

    private ProjectInfo currentLoadedProject;

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
        // _infoScroll.setMinimumSize(new Dimension(10, 200));

        _chartHolder.setLayout(new BorderLayout());

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
                        setIcon(ImageCache.getInstance().getImage(ImageCache.GEOM_LINE));
                    } else if (value instanceof Note) {
                        setIcon(ImageCache.getInstance().getImage(ImageCache.NOTE));
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
                    _chartHolder.removeAll();
                    if (paths.length > 0) {
                        Object selectedItem = paths[0].getLastPathComponent();
                        if (selectedItem instanceof ProjectInfo) {
                            currentSelectedProject = (ProjectInfo) selectedItem;
                            selectProjectInfo(currentSelectedProject);
                        }
                        if (selectedItem instanceof Image) {
                            currentSelectedImage = (Image) selectedItem;
                            currentSelectedProject = getProjectForImage(currentSelectedImage);
                            selectImage(currentSelectedImage);
                        }
                        if (selectedItem instanceof GpsLog) {
                            currentSelectedGpsLog = (GpsLog) selectedItem;
                            currentSelectedProject = getProjectForGpsLog(currentSelectedGpsLog);
                            selectGpsLog(currentSelectedGpsLog);
                        }
                        if (selectedItem instanceof Note) {
                            currentSelectedNote = (Note) selectedItem;
                            currentSelectedProject = getProjectForNote(currentSelectedNote);
                            selectNote(currentSelectedNote);
                        }
                    }
                }

            });

            _databaseTree.setVisible(false);
        } catch (Exception e1) {
            JGTLogger.logError(this, "Error", e1);
        }

        layoutTree(null, false);

        wwjPanel = new NwwPanel(true);
        wwjPanel.addOsmLayer();
        geopapDataLayer = new RenderableLayer();
        wwjPanel.addLayer(geopapDataLayer);
        _nwwHolder.setLayout(new BorderLayout());
        _nwwHolder.add(wwjPanel, BorderLayout.CENTER);

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

                List<Note> notesList = DaoNotes.getNotesList(connection, null);
                info.notes = notesList;

                List<GpsLog> logsList = DaoGpsLog.getLogsList(connection);
                info.logs = logsList;
                infoList.add(info);
            }
        }
        return infoList;
    }

    private ProjectInfo getProjectForImage( Image currentSelectedImage ) {
        boolean doBreak = false;
        ProjectInfo selectedProject = null;
        for( ProjectInfo projectInfo : projectInfos ) {
            for( org.jgrasstools.gears.io.geopaparazzi.geopap4.Image tmpImage : projectInfo.images ) {
                if (tmpImage.equals(currentSelectedImage)) {
                    selectedProject = projectInfo;
                    doBreak = true;
                    break;
                }
            }
            if (doBreak) {
                break;
            }
        }
        return selectedProject;
    }

    private ProjectInfo getProjectForGpsLog( GpsLog currentSelectedGpsLog ) {
        ProjectInfo selectedProject = null;
        for( ProjectInfo projectInfo : projectInfos ) {
            if (projectInfo.logs.contains(currentSelectedGpsLog)) {
                selectedProject = projectInfo;
                break;
            }
        }
        return selectedProject;
    }

    private ProjectInfo getProjectForNote( Note currentSelectedNote ) {
        ProjectInfo selectedProject = null;
        for( ProjectInfo projectInfo : projectInfos ) {
            if (projectInfo.notes.contains(currentSelectedNote)) {
                selectedProject = projectInfo;
                break;
            }
        }
        return selectedProject;
    }

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
                    List<Action> tableActions = makeImageAction(currentSelectedImage);
                    if (tableActions != null)
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
                    List<Action> dbActions = makeProjectAction(currentSelectedProject);
                    if (dbActions != null)
                        for( Action action : dbActions ) {
                            if (action != null) {
                                JMenuItem item = new JMenuItem(action);
                                popupMenu.add(item);
                                item.setHorizontalTextPosition(JMenuItem.RIGHT);
                            } else {
                                popupMenu.add(new JSeparator());
                            }
                        }
                } else if (currentSelectedGpsLog != null) {
                    List<Action> logActions = makeGpsLogActions(currentSelectedGpsLog);
                    if (logActions != null)
                        for( Action action : logActions ) {
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
                return projectInfo.images.length + projectInfo.logs.size() + projectInfo.notes.size();
            } else if (parent instanceof Image) {
                return 0;
            } else if (parent instanceof GpsLog) {
                return 0;
            } else if (parent instanceof Note) {
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
                int logsCount = projectInfo.logs.size();

                if (index > imagesCount + logsCount - 1) {
                    return projectInfo.notes.get(index - imagesCount - logsCount);
                } else if (index > imagesCount - 1) {
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

    private void selectProjectInfo( ProjectInfo selectedProject ) {
        try {
            String titleName = selectedProject.fileName;
            titleName = titleName.replace('_', ' ').replaceFirst("\\.gpap", "");
            String text = titleName + "<br/><br/>" + selectedProject.metadata;

            _infoArea.setText(text);

            loadProjectData(selectedProject, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void selectImage( org.jgrasstools.gears.io.geopaparazzi.geopap4.Image selectedImage ) {
        try {
            checkLoadedProject();
            String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(selectedImage.getTs()));
            String picInfo = "<b>Image:</b> " + GeopaparazziWorkspaceUtilities.escapeHTML(selectedImage.getName()) + "<br/>" //
                    + "<b>Timestamp:</b> " + dateTimeString + "<br/>" //
                    + "<b>Azimuth:</b> " + (int) selectedImage.getAzim() + " deg<br/>" //
                    + "<b>Altim:</b> " + (int) selectedImage.getAltim() + " m<br/>";
            _infoArea.setText(picInfo);

            wwjPanel.goTo(selectedImage.getLon(), selectedImage.getLat(), 1000.0, null, false);

        } catch (Exception e) {
            e.printStackTrace();
            setNoProjectLabel();
        }
    }

    private void checkLoadedProject() throws Exception {
        if (currentLoadedProject == null || currentLoadedProject != currentSelectedProject) {
            loadProjectData(currentSelectedProject, false);
        }
    }

    private void selectNote( Note selectedNote ) {
        try {
            checkLoadedProject();
            String dateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(selectedNote.timeStamp));
            String picInfo = "<b>Text:</b> " + GeopaparazziWorkspaceUtilities.escapeHTML(selectedNote.simpleText) + "<br/>" //
                    + "<b>Description:</b> " + GeopaparazziWorkspaceUtilities.escapeHTML(selectedNote.description) + "<br/>" //
                    + "<b>Timestamp:</b> " + dateTimeString + "<br/>" //
                    + "<b>Altim:</b> " + (int) selectedNote.altim + " m<br/>";
            _infoArea.setText(picInfo);

            wwjPanel.goTo(selectedNote.lon, selectedNote.lat, 1000.0, null, false);

        } catch (Exception e) {
            e.printStackTrace();
            setNoProjectLabel();
        }
    }

    private void selectGpsLog( GpsLog selectedLog ) {
        try {
            checkLoadedProject();
            String startDateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(selectedLog.startTime));
            String endDateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(selectedLog.endTime));
            String picInfo = "<b>Gps log:</b> " + GeopaparazziWorkspaceUtilities.escapeHTML(selectedLog.text) + "<br/>" //
                    + "<b>Start time:</b> " + startDateTimeString + "<br/>" //
                    + "<b>End time:</b> " + endDateTimeString + "<br/>";
            _infoArea.setText(picInfo);

            loadGpsLogChart(selectedLog, currentSelectedProject.databaseFile);

            Envelope env = new Envelope();
            for( GpsPoint gpsPoint : selectedLog.points ) {
                env.expandToInclude(gpsPoint.lon, gpsPoint.lat);
            }
            Sector sector = NwwUtilities.envelope2Sector(new ReferencedEnvelope(env, NwwUtilities.GPS_CRS));
            wwjPanel.goTo(sector, false);

        } catch (Exception e) {
            logger.error("error", e);
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

    private void loadGpsLogChart( GpsLog log, File dbFile ) throws Exception {
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {
            log.points.clear();
            DaoGpsLog.collectDataForLog(connection, log);

            String logName = log.text;
            GeodeticCalculator gc = new GeodeticCalculator(DefaultGeographicCRS.WGS84);
            int size = log.points.size();

            List<Coordinate> coords = new ArrayList<>();
            double runningDistance = 0;
            for( int i = 0; i < size - 1; i++ ) {
                GpsPoint p1 = log.points.get(i);
                GpsPoint p2 = log.points.get(i + 1);
                double lon1 = p1.lon;
                double lat1 = p1.lat;
                double altim1 = p1.altim;
                double lon2 = p2.lon;
                double lat2 = p2.lat;
                double altim2 = p2.altim;

                gc.setStartingGeographicPoint(lon1, lat1);
                gc.setDestinationGeographicPoint(lon2, lat2);
                double distance = gc.getOrthodromicDistance();
                runningDistance += distance;

                if (i == 0) {
                    coords.add(new Coordinate(0.0, altim1));
                }
                coords.add(new Coordinate(runningDistance, altim2));
            }

            LineString lineString = GeometryUtilities.gf().createLineString(coords.toArray(new Coordinate[0]));

            int lookAhead = 20;
            double slide = 1;
            FeatureSlidingAverage fsaElev = new FeatureSlidingAverage(lineString);
            List<Coordinate> smoothedElev = fsaElev.smooth(lookAhead, false, slide);
            double[] xProfile = new double[smoothedElev.size()];
            double[] yProfile = new double[smoothedElev.size()];
            for( int i = 0; i < xProfile.length; i++ ) {
                Coordinate c = smoothedElev.get(i);
                xProfile[i] = c.x;
                yProfile[i] = c.y;
            }

            Scatter scatterProfile = new Scatter("Profile " + logName);
            scatterProfile.addSeries("profile", xProfile, yProfile);
            scatterProfile.setShowLines(true);
            String colorQuery = "select " //
                    + GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName() + " from " + TABLE_GPSLOG_PROPERTIES
                    + " where " + //
                    GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName() + " = " + log.id;

            String colorStr = RED_HEXA;
            try (Statement newStatement = connection.createStatement()) {
                newStatement.setQueryTimeout(30);
                ResultSet result = newStatement.executeQuery(colorQuery);

                if (result.next()) {
                    colorStr = result.getString(1);
                    if (colorStr.equalsIgnoreCase("red")) {
                        colorStr = RED_HEXA;
                    }
                }
                if (colorStr == null || colorStr.length() == 0) {
                    colorStr = RED_HEXA;
                }
            }

            Color color = Color.RED;
            try {
                color = Color.decode(colorStr);
            } catch (Exception e) {
                // ignore logger.error("Could not convert color: " + colorStr, e);
            }

            scatterProfile.setColors(new Color[]{color});
            scatterProfile.setXLabel("progressive distance [m]");
            scatterProfile.setYLabel("elevation [m]");
            JFreeChart chart = scatterProfile.getChart();
            ChartPanel chartPanel = new ChartPanel(chart, true);

            _chartHolder.add(chartPanel, BorderLayout.CENTER);
        }
    }

    /**
     * Extract data from the db and add them to the map view.
     * 
     * @param projectTemplate
     * @return
     * @throws Exception 
     */
    private void loadProjectData( ProjectInfo currentSelectedProject, boolean zoomTo ) throws Exception {
        geopapDataLayer.removeAllRenderables();

        Envelope bounds = new Envelope();

        File dbFile = currentSelectedProject.databaseFile;
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + dbFile.getAbsolutePath())) {

            // NOTES
            List<String[]> noteDataList = GeopaparazziWorkspaceUtilities.getNotesText(connection);
            StringBuilder sb = new StringBuilder();
            sb.append("\n\n// GP NOTES\n");
            int index = 0;
            PointPlacemarkAttributes notesAttributes = new PointPlacemarkAttributes();
            // notesAttributes.setLabelMaterial(mFillMaterial);
            // notesAttributes.setLineMaterial(mFillMaterial);
            // notesAttributes.setUsePointAsDefaultImage(true);
            notesAttributes.setImage(ImageCache.getInstance().getBufferedImage(ImageCache.NOTE));
            notesAttributes.setLabelMaterial(new Material(Color.BLACK));
            // notesAttributes.setScale(mMarkerSize);
            for( String[] noteData : noteDataList ) {
                // [lon, lat, altim, dateTimeString, text, descr]
                double lon = Double.parseDouble(noteData[0]);
                double lat = Double.parseDouble(noteData[1]);
                String altim = noteData[2];
                String date = noteData[3];
                String text = noteData[4];
                String descr = noteData[5];

                PointPlacemark marker = new PointPlacemark(Position.fromDegrees(lat, lon, 0));
                marker.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                marker.setLabelText(text + " (" + date + ")");
                marker.setAttributes(notesAttributes);

                geopapDataLayer.addRenderable(marker);

                bounds.expandToInclude(lon, lat);
            }

            /*
             * IMAGES
             */
            PointPlacemarkAttributes imageAttributes = new PointPlacemarkAttributes();
            imageAttributes.setImage(ImageCache.getInstance().getBufferedImage(ImageCache.DBIMAGE));
            imageAttributes.setLabelMaterial(new Material(Color.GRAY));
            for( org.jgrasstools.gears.io.geopaparazzi.geopap4.Image image : currentSelectedProject.images ) {
                double lon = image.getLon();
                double lat = image.getLat();

                PointPlacemark marker = new PointPlacemark(Position.fromDegrees(lat, lon, 0));
                marker.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                marker.setLabelText(image.getName());
                marker.setAttributes(imageAttributes);

                geopapDataLayer.addRenderable(marker);
                bounds.expandToInclude(lon, lat);
            }

            try (Statement statement = connection.createStatement()) {
                statement.setQueryTimeout(30); // set timeout to 30 sec.

                String sql = "select " + //
                        GpsLogsTableFields.COLUMN_ID.getFieldName() + "," + //
                        GpsLogsTableFields.COLUMN_LOG_STARTTS.getFieldName() + "," + //
                        GpsLogsTableFields.COLUMN_LOG_ENDTS.getFieldName() + "," + //
                        GpsLogsTableFields.COLUMN_LOG_TEXT.getFieldName() + //
                        " from " + TABLE_GPSLOGS; //

                // first get the logs
                ResultSet rs = statement.executeQuery(sql);
                while( rs.next() ) {
                    long id = rs.getLong(1);

                    long startDateTime = rs.getLong(2);
                    String startDateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(startDateTime));
                    long endDateTime = rs.getLong(3);
                    String endDateTimeString = TimeUtilities.INSTANCE.TIME_FORMATTER_LOCAL.format(new Date(endDateTime));
                    String text = rs.getString(4);

                    // points
                    String query = "select " //
                            + GpsLogsDataTableFields.COLUMN_DATA_LAT.getFieldName() + ","
                            + GpsLogsDataTableFields.COLUMN_DATA_LON.getFieldName() + ","
                            + GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName()//
                            + " from " + TABLE_GPSLOG_DATA + " where " + //
                            GpsLogsDataTableFields.COLUMN_LOGID.getFieldName() + " = " + id + " order by "
                            + GpsLogsDataTableFields.COLUMN_DATA_TS.getFieldName();

                    List<Position> verticesList = new ArrayList<>();
                    try (Statement newStatement = connection.createStatement()) {
                        newStatement.setQueryTimeout(30);
                        ResultSet result = newStatement.executeQuery(query);

                        while( result.next() ) {
                            double lat = result.getDouble(1);
                            double lon = result.getDouble(2);
                            Position pos = Position.fromDegrees(lat, lon, 0);
                            verticesList.add(pos);
                            bounds.expandToInclude(lon, lat);
                        }
                    }

                    // color
                    String colorQuery = "select " //
                            + GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_COLOR.getFieldName() + ","
                            + GpsLogsPropertiesTableFields.COLUMN_PROPERTIES_WIDTH.getFieldName() + " from "
                            + TABLE_GPSLOG_PROPERTIES + " where " + //
                            GpsLogsPropertiesTableFields.COLUMN_LOGID.getFieldName() + " = " + id;

                    String colorStr = RED_HEXA;
                    int lineWidth = 3;
                    try (Statement newStatement = connection.createStatement()) {
                        newStatement.setQueryTimeout(30);
                        ResultSet result = newStatement.executeQuery(colorQuery);

                        if (result.next()) {
                            colorStr = result.getString(1);
                            lineWidth = result.getInt(2);
                            if (colorStr.equalsIgnoreCase("red")) {
                                colorStr = RED_HEXA;
                            }
                        }
                        if (colorStr == null || colorStr.length() == 0) {
                            colorStr = RED_HEXA;
                        }
                    }

                    Color color = Color.RED;
                    try {
                        color = Color.decode(colorStr);
                    } catch (Exception e) {
                        // ignore logger.error("Could not convert color: " + colorStr, e);
                    }

                    BasicShapeAttributes lineAttributes = new BasicShapeAttributes();
                    lineAttributes.setOutlineMaterial(new Material(color));
                    lineAttributes.setOutlineWidth(lineWidth);
                    Path path = new Path(verticesList);
                    path.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                    path.setFollowTerrain(true);
                    path.setAttributes(lineAttributes);

                    geopapDataLayer.addRenderable(path);
                }

            }

        }

        if (zoomTo) {
            Sector sector = NwwUtilities.envelope2Sector(new ReferencedEnvelope(bounds, NwwUtilities.GPS_CRS));
            wwjPanel.goTo(sector, false);
        }

        currentLoadedProject = currentSelectedProject;
    }

    protected abstract List<Action> makeGpsLogActions( final GpsLog selectedLog );

    protected abstract List<Action> makeProjectAction( final ProjectInfo project );

    protected abstract List<Action> makeImageAction( final Image selectedImage );

}
