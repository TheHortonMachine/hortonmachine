package org.hortonmachine.style;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.swing.AbstractAction;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.JToolBar;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.MapContent;
import org.geotools.map.RasterLayer;
import org.geotools.map.StyleLayer;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.swing.JMapPane;
import org.geotools.swing.action.InfoAction;
import org.geotools.swing.action.PanAction;
import org.geotools.swing.action.ResetAction;
import org.geotools.swing.action.ZoomInAction;
import org.geotools.swing.action.ZoomOutAction;
import org.geotools.swing.tool.ScrollWheelTool;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.CrsUtilities;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.features.FilterUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.style.FeatureTypeStyleWrapper;
import org.hortonmachine.gears.utils.style.LineSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.PointSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.PolygonSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.RasterSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.RuleWrapper;
import org.hortonmachine.gears.utils.style.StyleUtilities;
import org.hortonmachine.gears.utils.style.StyleWrapper;
import org.hortonmachine.gears.utils.style.SymbolizerWrapper;
import org.hortonmachine.gears.utils.style.TextSymbolizerWrapper;
import org.hortonmachine.gui.settings.SettingsController;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.modules.VectorReader;
import org.hortonmachine.style.objects.FileWithStyle;
import org.hortonmachine.style.objects.GpkgWithStyle;
import org.hortonmachine.style.objects.IObjectWithStyle;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@SuppressWarnings("serial")
public class MainController extends MainView implements IOnCloseListener, TreeSelectionListener {
    public static final String VECTOR_BOUNDS = "Vector bounds";
    public static final String RASTER_BOUNDS = "Raster Bounds";
    public static final String PROJECTION = "Projection: ";
    public static final String STYLE_GROUPS_AND_RULES = "Style: Groups and Rules";
    public static final String ATTRIBUTES = "Attributes";
    public static final String DATASTORE_INFORMATION = "Datastore information";
    public static final String KEY_PREF_TEMPLATES = "HM_KEY_PREF_TEMPLATES";

    public static final int COLOR_IMAGE_SIZE = 15;

    private MapContent mapContent;
    private List<SimpleFeature> currentFeaturesList;
    private int currentFeatureIndex = 0;
    private StyleLayer currentLayer;
    private JMapPane mapPane;
    private StyleWrapper styleWrapper;

    private String[] featureCollectionFieldNames;
    private FeatureTypeStyleWrapper currentSelectedFSW;
    private StyleWrapper currentSelectedSW;
    private RuleWrapper currentSelectedRW;
    private SymbolizerWrapper currentSelectedSymW;

    private GeometryDescriptor geometryDescriptor;

    private IObjectWithStyle objectWithStyle;

    private SimpleFeatureCollection currentFeatureCollection;

    private GridCoverage2D currentRaster;
    private FeatureAttributeNode currentSelectedFeatureAttributeNode;

    /**
     * Default constructor
     * 
     * @param fileToOpen        optional file to open.
     * @param optionalTableName
     */
    public MainController( File fileToOpen, String optionalTableName ) {
        setPreferredSize(new Dimension(1400, 800));

        _rulesTree.setCellRenderer(new CustomTreeCellRenderer());
        _rulesTree.expandRow(0);
        _rulesTree.setRootVisible(false);
        _rulesTree.setModel(new DefaultTreeModel(null));
        _stylePanel.setLayout(new BorderLayout());

        addJtreeContextMenu();

        mapContent = new MapContent();

        mapPane = new JMapPane(mapContent);
        _mapPaneHolder.setLayout(new BorderLayout());
        _mapPaneHolder.add(mapPane, BorderLayout.CENTER);

        JToolBar mapTools = makeToolBar();
        _mapPaneHolder.add(mapTools, BorderLayout.SOUTH);

        List<String> supportedExtensions = Stream
                .of(HMConstants.SUPPORTED_VECTOR_EXTENSIONS, HMConstants.SUPPORTED_RASTER_EXTENSIONS).flatMap(Stream::of)
                .collect(Collectors.toList());
        StringBuilder sb = new StringBuilder();
        for( String ext : supportedExtensions ) {
            sb.append(",*.").append(ext);
        }
        final String desc = sb.substring(1);

        _filepathField.setEditable(false);
        _browseButton.addActionListener(e -> {

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setMultiSelectionEnabled(true);
            FileFilter fileFilter = new FileFilter(){

                @Override
                public String getDescription() {
                    return desc;
                }

                @Override
                public boolean accept( File f ) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    String name = f.getName();
                    for( String ext : supportedExtensions ) {
                        if (name.endsWith(ext)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setCurrentDirectory(PreferencesHandler.getLastFile());
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                if (selectedFiles != null && selectedFiles.length > 0) {
                    File file = selectedFiles[0];
                    if (file.getName().toLowerCase().endsWith(HMConstants.GPKG)) {
                        String tableName = promptTableName(file);
                        if (tableName != null) {
                            objectWithStyle = new GpkgWithStyle();
                            try {
                                objectWithStyle.setDataFile(file, tableName);
                            } catch (Exception e1) {
                                e1.printStackTrace();
                            }
                        }

                    } else {
                        objectWithStyle = new FileWithStyle();
                        String nameWithoutExtention = FileUtilities.getNameWithoutExtention(file);
                        try {
                            objectWithStyle.setDataFile(file, nameWithoutExtention);
                        } catch (Exception e1) {
                            e1.printStackTrace();
                        }
                    }

                    openSelectedFile();
                }
            }
        });

        _saveButton.setText("");
        _saveButton.setToolTipText("Save SLD to file.");
        _saveButton.setIcon(ImageCache.getInstance().getImage(ImageCache.SAVE));
        _saveButton.addActionListener(e -> {
            try {
                if (styleWrapper == null) {
                    return;
                }
                String xml = styleWrapper.toXml();

                objectWithStyle.saveSld(xml);

            } catch (Exception e1) {
                Logger.INSTANCE.e("Error saving style to file.", e1);
            }
        });

        // _saveTemplateButton.setIcon(ImageCache.getInstance().getImage(ImageCache.DATABASE));
        _saveTemplateButton.setToolTipText("Save SLD to templated database.");
        _saveTemplateButton.addActionListener(e -> {
            try {
                if (styleWrapper == null) {
                    GuiUtilities.showInfoMessage(this, "No input style loaded.");
                    return;
                }

                String ts = DateTime.now().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSScompact);
                String name = objectWithStyle.getName();
                String styleName = GuiUtilities.showInputDialog(this, "Please set a name for the style.", ts + " " + name);
                if (styleName != null && styleName.trim().length() > 0) {
                    String xml = styleWrapper.toXml();
                    String[] stylesArray = PreferencesHandler.getPreference(KEY_PREF_TEMPLATES, new String[0]);
                    List<String> stylesList = new ArrayList<>();
                    stylesList.add(styleName);
                    stylesList.add(xml);
                    for( String item : stylesArray ) {
                        stylesList.add(item);
                    }
                    PreferencesHandler.setPreference(KEY_PREF_TEMPLATES, stylesList.toArray(new String[0]));
                }

            } catch (Exception e1) {
                Logger.INSTANCE.e("Error saving style to file.", e1);
            }
        });

        // _loadTemplateButton.setIcon(ImageCache.getInstance().getImage(ImageCache.DATABASE));
        _loadTemplateButton.setToolTipText("Load SLD from templated database.");
        _loadTemplateButton.addActionListener(e -> {
            try {
                String[] stylesArray = PreferencesHandler.getPreference(KEY_PREF_TEMPLATES, new String[0]);
                if (stylesArray.length == 0) {
                    GuiUtilities.showInfoMessage(this, "No templates saved yet.");
                    return;
                }
                HashMap<String, String> name2SldMap = new HashMap<>();
                for( int i = 0; i < stylesArray.length - 1; i = i + 2 ) {
                    name2SldMap.put(stylesArray[i], stylesArray[i + 1]);
                }

                Set<String> namesSet = name2SldMap.keySet();
                String[] names = namesSet.toArray(new String[0]);
                Arrays.sort(names);
                String selectedStyleName = GuiUtilities.showComboDialog(this, "TEMPLATES", "Select the template to load.", names,
                        names[0]);
                if (selectedStyleName == null) {
                    return;
                }
                String sldString = name2SldMap.get(selectedStyleName);
                loadFromStyle(sldString);

            } catch (Exception e1) {
                Logger.INSTANCE.e("Error loading style from templates.", e1);
            }
        });

        _loadSldButton.setToolTipText("Load SLD from templated database.");
        _loadSldButton.addActionListener(e -> {
            try {
                File lastFile = PreferencesHandler.getLastFile();
                FileFilter filter = new FileFilter(){

                    @Override
                    public String getDescription() {
                        return "sld";
                    }

                    @Override
                    public boolean accept( File f ) {
                        if (f.isDirectory()) {
                            return true;
                        }
                        String name = f.getName();
                        return name.toLowerCase().endsWith("sld");
                    }
                };
                File[] res = GuiUtilities.showOpenFilesDialog(this, "Select SLD file", false, lastFile, filter);
                if (res != null && res.length == 1) {
                    String absolutePath = res[0].getAbsolutePath();
                    String sldString = FileUtilities.readFile(absolutePath);
                    loadFromStyle(sldString);
                }
            } catch (Exception e1) {
                Logger.INSTANCE.e("Error loading style from SLD fileÏ.", e1);
            }
        });

        _deleteTemplateButton.setToolTipText("Delete SLD from templated database.");
        _deleteTemplateButton.addActionListener(e -> {
            try {
                String[] stylesArray = PreferencesHandler.getPreference(KEY_PREF_TEMPLATES, new String[0]);
                if (stylesArray.length == 0) {
                    GuiUtilities.showInfoMessage(this, "No templates saved yet.");
                    return;
                }
                HashMap<String, String> name2SldMap = new HashMap<>();
                for( int i = 0; i < stylesArray.length - 1; i = i + 2 ) {
                    name2SldMap.put(stylesArray[i], stylesArray[i + 1]);
                }

                Set<String> namesSet = name2SldMap.keySet();
                String[] names = namesSet.toArray(new String[0]);
                Arrays.sort(names);
                String selectedStyleName = GuiUtilities.showComboDialog(this, "TEMPLATES", "Select the template to delete.",
                        names, names[0]);
                if (selectedStyleName == null) {
                    return;
                }

                name2SldMap.remove(selectedStyleName);

                List<String> stylesList = new ArrayList<>();
                for( Entry<String, String> item : name2SldMap.entrySet() ) {
                    stylesList.add(item.getKey());
                    stylesList.add(item.getValue());
                }
                PreferencesHandler.setPreference(KEY_PREF_TEMPLATES, stylesList.toArray(new String[0]));
            } catch (Exception e1) {
                Logger.INSTANCE.e("Error loading style from templates.", e1);
            }
        });

        if (fileToOpen != null && fileToOpen.exists()) {
            try {
                if (fileToOpen.getName().toLowerCase().endsWith(HMConstants.GPKG)) {
                    objectWithStyle = new GpkgWithStyle();
                    objectWithStyle.setDataFile(fileToOpen, optionalTableName);
                } else {
                    objectWithStyle = new FileWithStyle();
                    String nameWithoutExtention = FileUtilities.getNameWithoutExtention(fileToOpen);
                    objectWithStyle.setDataFile(fileToOpen, nameWithoutExtention);
                }
            } catch (Exception e1) {

                Logger.INSTANCE.e("Error opening style.", e1);
            }
            openSelectedFile();
        } else {
            enableTemplateButtons(false);
        }

    }

    private JToolBar makeToolBar() {
        JToolBar toolBar = new JToolBar();
        toolBar.setOrientation(JToolBar.HORIZONTAL);
        toolBar.setFloatable(false);

        JButton btn;
        ButtonGroup cursorToolGrp = new ButtonGroup();
        mapPane.addMouseListener(new ScrollWheelTool(mapPane));
        // if (toolSet.contains(Tool.POINTER)) {
        // btn = new JButton(new NoToolAction(mapPane));
        // btn.setName(TOOLBAR_POINTER_BUTTON_NAME);
        // toolBar.add(btn);
        // cursorToolGrp.add(btn);
        // }

        btn = new JButton(new ZoomInAction(mapPane));
        btn.setName(JMapFrame.TOOLBAR_ZOOMIN_BUTTON_NAME);
        toolBar.add(btn);
        cursorToolGrp.add(btn);

        btn = new JButton(new ZoomOutAction(mapPane));
        btn.setName(JMapFrame.TOOLBAR_ZOOMOUT_BUTTON_NAME);
        toolBar.add(btn);
        cursorToolGrp.add(btn);

        toolBar.addSeparator();

        btn = new JButton(new PanAction(mapPane));
        btn.setName(JMapFrame.TOOLBAR_PAN_BUTTON_NAME);
        toolBar.add(btn);
        cursorToolGrp.add(btn);

        toolBar.addSeparator();

        btn = new JButton(new InfoAction(mapPane));
        btn.setName(JMapFrame.TOOLBAR_INFO_BUTTON_NAME);
        toolBar.add(btn);

        toolBar.addSeparator();

        btn = new JButton(new ResetAction(mapPane));
        btn.setName(JMapFrame.TOOLBAR_RESET_BUTTON_NAME);
        toolBar.add(btn);
        return toolBar;
    }

    private static String promptTableName( File geopackageFile ) {
        String tableName = null;
        try (GeopackageCommonDb db = new GeopackageDb()) {
            db.open(geopackageFile.getAbsolutePath());
            List<FeatureEntry> features = db.features();
            if (features.size() == 0) {
                GuiUtilities.showWarningMessage(null, "No feature tables found in geopackage.");
                return null;
            } else if (features.size() == 1) {
                tableName = features.get(0).getTableName();
            } else {
                List<String> tableNames = features.stream().map(f -> f.getTableName()).collect(Collectors.toList());
                String selection = GuiUtilities.showComboDialog(null, "Select", "Select the table to style",
                        tableNames.toArray(new String[0]), tableNames.get(0));
                if (selection == null) {
                    return null;
                }
                tableName = selection;
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
        return tableName;
    }

    private void loadFromStyle( String sldString ) throws IOException {
        Style style = SldUtilities.getStyleFromSldString(sldString);
        if (style == null) {
            GuiUtilities.showWarningMessage(this, "Unable to load style from SLD.");
            return;
        }

        styleWrapper = new StyleWrapper(style);
        TextSymbolizerWrapper firstTextSymbolizer = styleWrapper.getFirstTextSymbolizer();
        if (firstTextSymbolizer != null) {
            String labelName = firstTextSymbolizer.getLabelName();
            if (labelName != null) {
                String[] fields = FeatureUtilities.featureCollectionFieldNames(currentFeatureCollection);
                List<String> fieldsList = Arrays.asList(fields);
                if (!fieldsList.contains(labelName)) {
                    GuiUtilities.showWarningMessage(this, "The loaded style references a field '" + labelName
                            + "', but no such field is in the loaded attributes. Make sure to check the textsymbolizer's label.");
                }
            }
        }

        if (currentLayer != null) {
            mapContent.removeLayer(currentLayer);
        }
        currentSelectedFSW = null;
        currentSelectedSW = null;
        currentSelectedRW = null;
        currentSelectedSymW = null;

        currentLayer = new FeatureLayer(currentFeatureCollection, style);
        mapContent.addLayer(currentLayer);

        reloadGroupsAndRules();

        _stylePanel.removeAll();
        _stylePanel.revalidate();
        _stylePanel.repaint();

    }

    private void openSelectedFile() {
        if (objectWithStyle == null)
            return;
        String absolutePath = objectWithStyle.getDataFile().getAbsolutePath();
        PreferencesHandler.setLastPath(absolutePath);
        _filepathField.setText(objectWithStyle.getNormalizedPath());

        if (currentLayer != null) {
            mapContent.removeLayer(currentLayer);
        }
        currentSelectedFSW = null;
        currentSelectedSW = null;
        currentSelectedRW = null;
        currentSelectedSymW = null;
        currentRaster = null;
        currentFeaturesList = null;
        currentFeatureCollection = null;

        try {
            if (objectWithStyle.isVector()) {

                currentFeatureCollection = VectorReader.readVector(objectWithStyle.getNormalizedPath());

                geometryDescriptor = currentFeatureCollection.getSchema().getGeometryDescriptor();

                featureCollectionFieldNames = FeatureUtilities.featureCollectionFieldNames(currentFeatureCollection);
                currentFeaturesList = FeatureUtilities.featureCollectionToList(currentFeatureCollection);

                CoordinateReferenceSystem currentCRS = currentFeatureCollection.getSchema().getCoordinateReferenceSystem();
                mapContent.getViewport().setCoordinateReferenceSystem(currentCRS);

                Style style = SldUtilities.getStyleFromSldString(objectWithStyle.getSldString());
                if (style == null) {
                    style = StyleUtilities.createDefaultStyle(currentFeatureCollection);
                }

                currentLayer = new FeatureLayer(currentFeatureCollection, style);
                mapContent.addLayer(currentLayer);

                styleWrapper = new StyleWrapper(style);

                zoomToAll();
                reloadGroupsAndRules();

                _stylePanel.removeAll();
                _stylePanel.revalidate();
                _stylePanel.repaint();
            } else if (objectWithStyle.isRaster()) {
                currentRaster = OmsRasterReader.readRaster(absolutePath);
                Style style = SldUtilities.getStyleFromSldString(objectWithStyle.getSldString());
                if (style == null) {
                    style = RasterStyleUtilities.createDefaultRasterStyle();
                }
                mapContent.getViewport().setCoordinateReferenceSystem(currentRaster.getCoordinateReferenceSystem());
                currentLayer = new GridCoverageLayer(currentRaster, style);
                mapContent.addLayer(currentLayer);

                styleWrapper = new StyleWrapper(style);

                zoomToAll();
                reloadGroupsAndRules();

            }

            enableTemplateButtons(true);
        } catch (Exception e1) {
            enableTemplateButtons(false);
            Logger.INSTANCE.e("Error parsing style data.", e1);
        }

    }

    private void enableTemplateButtons( boolean enable ) {
        _saveTemplateButton.setEnabled(enable);
        _loadTemplateButton.setEnabled(enable);
        _loadSldButton.setEnabled(enable);
        _deleteTemplateButton.setEnabled(enable);
    }

    private void reloadGroupsAndRules() {
        _stylePanel.removeAll();
        _stylePanel.revalidate();
        _stylePanel.repaint();

        List<FeatureTypeStyleWrapper> featureTypeStylesWrapperList = styleWrapper.getFeatureTypeStylesWrapperList();
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode();
        DefaultTreeModel model = new DefaultTreeModel(rootNode);

        DefaultMutableTreeNode styleInfoNode = new DefaultMutableTreeNode(STYLE_GROUPS_AND_RULES);
        rootNode.add(styleInfoNode);
        DefaultMutableTreeNode styleNode = new DefaultMutableTreeNode(styleWrapper);
        styleInfoNode.add(styleNode);

        for( FeatureTypeStyleWrapper featureTypeStyle : featureTypeStylesWrapperList ) {
            DefaultMutableTreeNode featureStyleNode = new DefaultMutableTreeNode(featureTypeStyle);
            styleNode.add(featureStyleNode);

            List<RuleWrapper> rulesWrapperList = featureTypeStyle.getRulesWrapperList();
            for( RuleWrapper ruleWrapper : rulesWrapperList ) {
                DefaultMutableTreeNode ruleNode = new DefaultMutableTreeNode(ruleWrapper);
                featureStyleNode.add(ruleNode);

                SymbolizerWrapper geometrySymbolizers = ruleWrapper.getGeometrySymbolizersWrapper();
                if (geometrySymbolizers != null) {
                    DefaultMutableTreeNode geomSymbolizerNode = new DefaultMutableTreeNode(geometrySymbolizers);
                    ruleNode.add(geomSymbolizerNode);
                }
                SymbolizerWrapper textSymbolizers = ruleWrapper.getTextSymbolizersWrapper();
                if (textSymbolizers != null) {
                    DefaultMutableTreeNode textSymbolizerNode = new DefaultMutableTreeNode(textSymbolizers);
                    ruleNode.add(textSymbolizerNode);
                }

            }
        }

        DefaultMutableTreeNode datastoreNode = new DefaultMutableTreeNode(DATASTORE_INFORMATION);
        rootNode.add(datastoreNode);

        if (currentFeatureCollection != null) {
            SimpleFeatureType schema = currentFeatureCollection.getSchema();
            CoordinateReferenceSystem crs = schema.getCoordinateReferenceSystem();
            try {
                String epsgString = CrsUtilities.getCodeFromCrs(crs);
                DefaultMutableTreeNode crsNode = new DefaultMutableTreeNode(PROJECTION + epsgString);
                datastoreNode.add(crsNode);
            } catch (Exception e) {
                e.printStackTrace();
            }

            DefaultMutableTreeNode countNode = new DefaultMutableTreeNode("Feature count: " + currentFeatureCollection.size());
            datastoreNode.add(countNode);

            DecimalFormat f = HMConstants.DEGREE6_FORMATTER;
            ReferencedEnvelope bounds = currentFeatureCollection.getBounds();
            DefaultMutableTreeNode boundsInfoNode = new DefaultMutableTreeNode(VECTOR_BOUNDS);
            datastoreNode.add(boundsInfoNode);
            DefaultMutableTreeNode northNode = new DefaultMutableTreeNode("North: " + f.format(bounds.getMaxY()));
            boundsInfoNode.add(northNode);
            DefaultMutableTreeNode southNode = new DefaultMutableTreeNode("South: " + f.format(bounds.getMinY()));
            boundsInfoNode.add(southNode);
            DefaultMutableTreeNode westNode = new DefaultMutableTreeNode("West: " + f.format(bounds.getMinX()));
            boundsInfoNode.add(westNode);
            DefaultMutableTreeNode eastNode = new DefaultMutableTreeNode("East: " + f.format(bounds.getMaxX()));
            boundsInfoNode.add(eastNode);
            DefaultMutableTreeNode widthNode = new DefaultMutableTreeNode(
                    "Width: " + f.format((bounds.getMaxX() - bounds.getMinX())));
            boundsInfoNode.add(widthNode);
            DefaultMutableTreeNode heightNode = new DefaultMutableTreeNode(
                    "Height: " + f.format((bounds.getMaxY() - bounds.getMinY())));
            boundsInfoNode.add(heightNode);

            List<AttributeDescriptor> attributeDescriptors = schema.getAttributeDescriptors();
            DefaultMutableTreeNode attributesInfoNode = new DefaultMutableTreeNode(ATTRIBUTES);
            for( AttributeDescriptor attributeDescriptor : attributeDescriptors ) {
                String name = attributeDescriptor.getLocalName();
                String type = attributeDescriptor.getType().getBinding().getSimpleName();
                DefaultMutableTreeNode singleAttrtibuteInfoNode = new DefaultMutableTreeNode(
                        new FeatureAttributeNode(name, type));
                attributesInfoNode.add(singleAttrtibuteInfoNode);
            }
            datastoreNode.add(attributesInfoNode);
        } else if (currentRaster != null) {
            CoordinateReferenceSystem crs = currentRaster.getCoordinateReferenceSystem();
            try {
                String epsgString = CrsUtilities.getCodeFromCrs(crs);
                DefaultMutableTreeNode crsNode = new DefaultMutableTreeNode(PROJECTION + epsgString);
                datastoreNode.add(crsNode);
            } catch (Exception e) {
                e.printStackTrace();
            }
            RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(currentRaster);

            DefaultMutableTreeNode boundsInfoNode = new DefaultMutableTreeNode(RASTER_BOUNDS);
            datastoreNode.add(boundsInfoNode);
            DefaultMutableTreeNode northNode = new DefaultMutableTreeNode("North: " + regionMap.getNorth());
            boundsInfoNode.add(northNode);
            DefaultMutableTreeNode southNode = new DefaultMutableTreeNode("South: " + regionMap.getSouth());
            boundsInfoNode.add(southNode);
            DefaultMutableTreeNode westNode = new DefaultMutableTreeNode("West: " + regionMap.getWest());
            boundsInfoNode.add(westNode);
            DefaultMutableTreeNode eastNode = new DefaultMutableTreeNode("East: " + regionMap.getEast());
            boundsInfoNode.add(eastNode);
            DefaultMutableTreeNode colsNode = new DefaultMutableTreeNode("Cols: " + regionMap.getCols());
            boundsInfoNode.add(colsNode);
            DefaultMutableTreeNode rowsNode = new DefaultMutableTreeNode("Rows: " + regionMap.getRows());
            boundsInfoNode.add(rowsNode);
            DefaultMutableTreeNode xresNode = new DefaultMutableTreeNode("X Res: " + regionMap.getXres());
            boundsInfoNode.add(xresNode);
            DefaultMutableTreeNode yresNode = new DefaultMutableTreeNode("Y Res: " + regionMap.getYres());
            boundsInfoNode.add(yresNode);
            DefaultMutableTreeNode widthNode = new DefaultMutableTreeNode(
                    "Width: " + (regionMap.getEast() - regionMap.getWest()));
            boundsInfoNode.add(widthNode);
            DefaultMutableTreeNode heightNode = new DefaultMutableTreeNode(
                    "Height: " + (regionMap.getNorth() - regionMap.getSouth()));
            boundsInfoNode.add(heightNode);

        }
        _rulesTree.expandRow(0);
        _rulesTree.setRootVisible(false);
        _rulesTree.setModel(model);
        for( int i = 0; i < _rulesTree.getRowCount(); i++ ) {
            _rulesTree.expandRow(i);
        }
        _rulesTree.addTreeSelectionListener(this);

    }

    private void addJtreeContextMenu() {
        _rulesTree.addTreeSelectionListener(new TreeSelectionListener(){

            public void valueChanged( TreeSelectionEvent evt ) {
                TreePath[] paths = evt.getPaths();
                if (paths.length > 0) {
                    Object selectedItem = paths[0].getLastPathComponent();

                    currentSelectedSW = null;
                    currentSelectedFSW = null;
                    currentSelectedRW = null;
                    currentSelectedSymW = null;
                    currentSelectedFeatureAttributeNode = null;

                    if (selectedItem instanceof DefaultMutableTreeNode) {
                        DefaultMutableTreeNode node = (DefaultMutableTreeNode) selectedItem;
                        Object userObject = node.getUserObject();
                        if (userObject instanceof StyleWrapper) {
                            currentSelectedSW = (StyleWrapper) userObject;
                        } else if (userObject instanceof FeatureTypeStyleWrapper) {
                            currentSelectedFSW = (FeatureTypeStyleWrapper) userObject;
                        } else if (userObject instanceof RuleWrapper) {
                            currentSelectedRW = (RuleWrapper) userObject;
                        } else if (userObject instanceof SymbolizerWrapper) {
                            currentSelectedSymW = (SymbolizerWrapper) userObject;
                        } else if (userObject instanceof FeatureAttributeNode) {
                            currentSelectedFeatureAttributeNode = (FeatureAttributeNode) userObject;
                        }

                    }

                }
            }
        });

        JPopupMenu popupMenu = new JPopupMenu();
        popupMenu.setBorder(new BevelBorder(BevelBorder.RAISED));
        popupMenu.addPopupMenuListener(new PopupMenuListener(){

            @Override
            public void popupMenuWillBecomeVisible( PopupMenuEvent e ) {
                createMenuActions(popupMenu);
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

        _rulesTree.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent e ) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    int row = _rulesTree.getClosestRowForLocation(e.getX(), e.getY());
                    _rulesTree.setSelectionRow(row);
                    popupMenu.show(e.getComponent(), e.getX(), e.getY());
                }

            }
        });

    }

    private void zoomToAll() {
        if (currentLayer != null) {
            ReferencedEnvelope bounds = currentLayer.getBounds();
            mapPane.setDisplayArea(bounds);
        }
    }

    public JComponent asJComponent() {
        return this;
    }

    @Override
    public void onClose() {
        SettingsController.onCloseHandleSettings();
    }

    public boolean canCloseWithoutPrompt() {
        return false;
    }

    @Override
    public void valueChanged( TreeSelectionEvent e ) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) _rulesTree.getLastSelectedPathComponent();

        if (node == null)
            return;
        _stylePanel.removeAll();

        Object nodeObject = node.getUserObject();
        if (nodeObject instanceof PolygonSymbolizerWrapper) {
            PolygonSymbolizerWrapper symbolizerWrapper = (PolygonSymbolizerWrapper) nodeObject;
            _stylePanel.add(new PolygonSymbolizerController(symbolizerWrapper, this), BorderLayout.CENTER);
        } else if (nodeObject instanceof LineSymbolizerWrapper) {
            LineSymbolizerWrapper symbolizerWrapper = (LineSymbolizerWrapper) nodeObject;
            _stylePanel.add(new LineSymbolizerController(symbolizerWrapper, this), BorderLayout.CENTER);
        } else if (nodeObject instanceof PointSymbolizerWrapper) {
            PointSymbolizerWrapper symbolizerWrapper = (PointSymbolizerWrapper) nodeObject;
            _stylePanel.add(new PointMarkSymbolizerController(symbolizerWrapper, this), BorderLayout.CENTER);
        } else if (nodeObject instanceof TextSymbolizerWrapper) {
            TextSymbolizerWrapper symbolizerWrapper = (TextSymbolizerWrapper) nodeObject;
            _stylePanel.add(new TextSymbolizerController(symbolizerWrapper, featureCollectionFieldNames, this),
                    BorderLayout.CENTER);
        } else if (nodeObject instanceof RasterSymbolizerWrapper) {
            RasterSymbolizerWrapper symbolizerWrapper = (RasterSymbolizerWrapper) nodeObject;
            _stylePanel.add(new RasterStyleController(symbolizerWrapper, currentRaster, this), BorderLayout.CENTER);
        } else if (nodeObject instanceof RuleWrapper) {
            _stylePanel.add(new RuleParametersController((RuleWrapper) nodeObject, this));
        } else if (nodeObject instanceof FeatureTypeStyleWrapper) {
            _stylePanel.add(new FeatureTypeParametersController((FeatureTypeStyleWrapper) nodeObject, this));
        }
        _stylePanel.revalidate();
        _stylePanel.repaint();
    }

    public void applyStyle() {
        Style style = styleWrapper.getStyle();
        currentLayer.setStyle(style);
    }

    private void createMenuActions( JPopupMenu popupMenu ) {
        boolean isRaster = false;
        if (currentLayer instanceof RasterLayer) {
            isRaster = true;
        }

        if (currentSelectedSW != null && !isRaster) {
            // add featureStyle
            AbstractAction action = new AbstractAction("Add new FeatureTypeStyle"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    FeatureTypeStyle featureTypeStyle = StyleUtilities.sf.createFeatureTypeStyle();
                    FeatureTypeStyleWrapper ftsw = new FeatureTypeStyleWrapper(featureTypeStyle, currentSelectedSW);

                    String tmpName = "New Group";
                    tmpName = WrapperUtilities.checkSameNameFeatureTypeStyle(styleWrapper.getFeatureTypeStylesWrapperList(),
                            tmpName);
                    ftsw.setName(tmpName);
                    styleWrapper.addFeatureTypeStyle(ftsw);

                    reloadGroupsAndRules();
                }
            };
            JMenuItem item = new JMenuItem(action);
            popupMenu.add(item);
            item.setHorizontalTextPosition(JMenuItem.RIGHT);
        } else if (currentSelectedFSW != null && !isRaster) {
            // add rule
            AbstractAction action = new AbstractAction("Add new Rule"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    Rule rule = StyleUtilities.sf.createRule();
                    RuleWrapper rw = new RuleWrapper(rule, currentSelectedFSW);

                    currentSelectedFSW.addRule(rw);

                    String tmpName = "New Rule";
                    tmpName = WrapperUtilities.checkSameNameRule(currentSelectedFSW.getRulesWrapperList(), tmpName);
                    rw.setName(tmpName);

                    reloadGroupsAndRules();
                }
            };
            JMenuItem item = new JMenuItem(action);
            popupMenu.add(item);
            item.setHorizontalTextPosition(JMenuItem.RIGHT);

            action = new AbstractAction("Remove all Rules"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    currentSelectedFSW.clear();
                    reloadGroupsAndRules();
                }
            };
            item = new JMenuItem(action);
            popupMenu.add(item);
            item.setHorizontalTextPosition(JMenuItem.RIGHT);

            List<FeatureTypeStyleWrapper> featureTypeStylesWrapperList = styleWrapper.getFeatureTypeStylesWrapperList();
            int ftIndex = featureTypeStylesWrapperList.indexOf(currentSelectedFSW);
            int ftSize = featureTypeStylesWrapperList.size();
            if (ftSize > 1 && ftIndex > 0) {
                action = new AbstractAction("Move up"){
                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        int from = ftIndex;
                        int to = ftIndex - 1;
                        if (to >= 0) {
                            styleWrapper.swap(from, to);
                            reloadGroupsAndRules();
                        }

                    }
                };
                item = new JMenuItem(action);
                popupMenu.add(item);
                item.setHorizontalTextPosition(JMenuItem.RIGHT);
            }

            popupMenu.add(new JSeparator());

            // remove featureStyle
            action = new AbstractAction("Remove selected FeatureTypeStyle"){
                @Override
                public void actionPerformed( ActionEvent e ) {

                    StyleWrapper p = currentSelectedFSW.getParent();
                    p.removeFeatureTypeStyle(currentSelectedFSW);

                    reloadGroupsAndRules();
                }
            };
            item = new JMenuItem(action);
            popupMenu.add(item);
            item.setHorizontalTextPosition(JMenuItem.RIGHT);
        } else if (currentSelectedRW != null && !isRaster) {

            // add rule
            AbstractAction action;
            JMenuItem item;
            if (currentSelectedRW.getGeometrySymbolizersWrapper() == null) {
                action = new AbstractAction("Add Geometry Symbolizer"){
                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        EGeometryType type = EGeometryType.forGeometryDescriptor(geometryDescriptor);
                        switch( type ) {
                        case POINT:
                        case MULTIPOINT:
                            currentSelectedRW.addSymbolizer(null, PointSymbolizerWrapper.class);
                            break;
                        case LINESTRING:
                        case MULTILINESTRING:
                            currentSelectedRW.addSymbolizer(null, LineSymbolizerWrapper.class);
                            break;
                        case POLYGON:
                        case MULTIPOLYGON:
                            currentSelectedRW.addSymbolizer(null, PolygonSymbolizerWrapper.class);
                            break;
                        default:
                            break;
                        }
                        reloadGroupsAndRules();
                    }
                };
                item = new JMenuItem(action);
                popupMenu.add(item);
                item.setHorizontalTextPosition(JMenuItem.RIGHT);
            }
            if (currentSelectedRW.getTextSymbolizersWrapper() == null
                    && currentSelectedRW.getGeometrySymbolizersWrapper() != null) {
                action = new AbstractAction("Add Text Symbolizer"){
                    @Override
                    public void actionPerformed( ActionEvent e ) {
                        currentSelectedRW.addSymbolizer(null, TextSymbolizerWrapper.class);
                        reloadGroupsAndRules();
                    }
                };
                item = new JMenuItem(action);
                popupMenu.add(item);
                item.setHorizontalTextPosition(JMenuItem.RIGHT);
            }
            popupMenu.add(new JSeparator());

            // remove featureStyle
            action = new AbstractAction("Remove selected Rule"){
                @Override
                public void actionPerformed( ActionEvent e ) {

                    FeatureTypeStyleWrapper f = currentSelectedRW.getParent();
                    f.removeRule(currentSelectedRW);

                    reloadGroupsAndRules();
                }
            };
            item = new JMenuItem(action);
            popupMenu.add(item);
            item.setHorizontalTextPosition(JMenuItem.RIGHT);
        } else if (currentSelectedSymW != null && !isRaster) {
            // remove featureStyle
            AbstractAction action = new AbstractAction("Remove selected Symbolizer"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    RuleWrapper rw = currentSelectedSymW.getParent();
                    rw.removeSymbolizerWrapper(currentSelectedSymW);
                    reloadGroupsAndRules();
                }
            };
            JMenuItem item = new JMenuItem(action);
            popupMenu.add(item);
            item.setHorizontalTextPosition(JMenuItem.RIGHT);
        } else if (currentSelectedFeatureAttributeNode != null && !isRaster) {
            AbstractAction action1 = new AbstractAction("View field stats"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    String fieldName = currentSelectedFeatureAttributeNode.getFieldName();
                    String geomName = geometryDescriptor.getLocalName();
                    String[][] dataMatrix = null;

                    if (geomName.equals(fieldName)) {

                        int count = 0;
                        double length = 0;
                        double area = 0;
                        for( SimpleFeature simpleFeature : currentFeaturesList ) {
                            Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
                            length += geom.getLength();
                            area += geom.getArea();
                            count++;
                        }

                        SimpleFeatureCollection fl3857FC = new ReprojectingFeatureCollection(currentFeatureCollection,
                                CrsUtilities.getCrsFromSrid(3857));
                        List<SimpleFeature> fl3857List = FeatureUtilities.featureCollectionToList(fl3857FC);

                        double length3857 = 0;
                        double area3857 = 0;
                        for( SimpleFeature simpleFeature : fl3857List ) {
                            Geometry geom = (Geometry) simpleFeature.getDefaultGeometry();
                            length3857 += geom.getLength();
                            area3857 += geom.getArea();
                        }

                        DecimalFormat f = new DecimalFormat("0.0");
                        DecimalFormat fd = new DecimalFormat("0.000000");
                        dataMatrix = new String[5][2];
                        dataMatrix[0][0] = "Features count";
                        dataMatrix[0][1] = "" + count;
                        dataMatrix[1][0] = "Length (data CRS/units)";
                        dataMatrix[1][1] = fd.format(length);
                        dataMatrix[2][0] = "Area (data CRS/units)";
                        dataMatrix[2][1] = fd.format(area);
                        dataMatrix[3][0] = "Length (3857 CRS)";
                        dataMatrix[3][1] = f.format(length3857) + "m";
                        dataMatrix[4][0] = "Area (3857)";
                        dataMatrix[4][1] = f.format(area3857) + "m";

                        Dimension dimension = new Dimension(400, 200);
                        boolean modal = true;
                        String title = "Geometry stats";
                        String[] columnNames = new String[]{"", ""};

                        GuiUtilities.openDialogWithTable(title, dataMatrix, columnNames, dimension, modal);

                    } else {
                        TreeMap<String, Integer> map = new TreeMap<>();
                        for( SimpleFeature simpleFeature : currentFeaturesList ) {
                            Object attribute = simpleFeature.getAttribute(fieldName);
                            if (attribute != null) {
                                String attrStr = attribute.toString();
                                Integer count = map.get(attrStr);
                                if (count == null) {
                                    count = 1;
                                } else {
                                    count = count + 1;
                                }
                                map.put(attrStr, count);
                            }
                        }
                        int row = 0;
                        dataMatrix = new String[map.size()][2];
                        for( Entry<String, Integer> entry : map.entrySet() ) {
                            String name = entry.getKey();
                            String count = String.valueOf(entry.getValue());
                            dataMatrix[row][0] = name;
                            dataMatrix[row][1] = count;
                            row++;
                        }
                        Dimension dimension = new Dimension(400, 600);
                        boolean modal = true;
                        String title = "Field stats";
                        String[] columnNames = new String[]{"value", "count"};

                        GuiUtilities.openDialogWithTable(title, dataMatrix, columnNames, dimension, modal);
                    }

                }

            };
            JMenuItem item1 = new JMenuItem(action1);
            popupMenu.add(item1);
            item1.setHorizontalTextPosition(JMenuItem.RIGHT);
            AbstractAction action2 = new AbstractAction("Create unique rules based on this attribute"){
                @Override
                public void actionPerformed( ActionEvent e ) {
                    FeatureTypeStyleWrapper fsw = currentSelectedFSW;
                    if (fsw == null) {
                        // use the first available
                        List<FeatureTypeStyleWrapper> featureTypeStylesWrapperList = styleWrapper
                                .getFeatureTypeStylesWrapperList();
                        fsw = featureTypeStylesWrapperList.get(0);
                    }

                    String fieldName = currentSelectedFeatureAttributeNode.getFieldName();
                    TreeSet<String> set = new TreeSet<>();
                    boolean isStringAttr = false;
                    for( SimpleFeature simpleFeature : currentFeaturesList ) {
                        Object attribute = simpleFeature.getAttribute(fieldName);
                        if (attribute != null) {
                            String attrStr = attribute.toString();
                            set.add(attrStr);
                        }
                        if (attribute instanceof String) {
                            isStringAttr = true;
                        }
                    }

                    int fillAlpha = 100;
                    float fillAlphaf = fillAlpha / 255f;
                    ColorInterpolator interpFill = new ColorInterpolator(EColorTables.rainbow.name(), 0, set.size(), null);
                    ColorInterpolator interpStroke = new ColorInterpolator(EColorTables.rainbow.name(), 0, set.size(), null);

                    EGeometryType type = EGeometryType.forGeometryDescriptor(geometryDescriptor);
                    // create new rules for each attribute value
                    int index = 0;
                    for( String value : set ) {
                        try {
                            Rule rule = StyleUtilities.sf.createRule();
                            RuleWrapper rw = new RuleWrapper(rule, fsw);
                            fsw.addRule(rw);

                            String tmpName = fieldName + " == " + value;
                            tmpName = WrapperUtilities.checkSameNameRule(fsw.getRulesWrapperList(), tmpName);
                            rw.setName(tmpName);

                            String escapedValue = value.replaceAll("'", "''");
                            String filterValue = escapedValue;
                            if (isStringAttr) {
                                filterValue = "'" + escapedValue + "'";
                            }
                            Filter filter = FilterUtilities.getCQLFilter(fieldName + "=" + filterValue);
                            rule.setFilter(filter);

                            Color fill = interpFill.getColorFor(index);
                            Color stroke = interpStroke.getColorFor(index);
                            String fillHex = ColorUtilities.asHex(fill);
                            String strokeHex = ColorUtilities.asHex(stroke);

                            switch( type ) {
                            case POINT:
                            case MULTIPOINT:
                                PointSymbolizerWrapper pointSW = rw.addSymbolizer(null, PointSymbolizerWrapper.class);
                                pointSW.setFillColor(fillHex);
                                pointSW.setFillOpacity(fillAlphaf + "", false);
                                pointSW.setStrokeColor(strokeHex);
                                break;
                            case LINESTRING:
                            case MULTILINESTRING:
                                LineSymbolizerWrapper lineSW = rw.addSymbolizer(null, LineSymbolizerWrapper.class);
                                lineSW.setStrokeColor(strokeHex, false);
                                break;
                            case POLYGON:
                            case MULTIPOLYGON:
                                PolygonSymbolizerWrapper polygonSW = rw.addSymbolizer(null, PolygonSymbolizerWrapper.class);
                                polygonSW.setFillColor(fillHex, false);
                                polygonSW.setFillOpacity(fillAlphaf + "", false);
                                polygonSW.setStrokeColor(strokeHex, false);
                                break;
                            default:
                                break;
                            }

                            index++;
                        } catch (Exception e1) {
                            // TODO Auto-generated catch block
                            e1.printStackTrace();
                        }
                    }

                    reloadGroupsAndRules();
                    applyStyle();
                }

            };
            JMenuItem item2 = new JMenuItem(action2);
            popupMenu.add(item2);
            item2.setHorizontalTextPosition(JMenuItem.RIGHT);
        }
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();

        File openFile = null;
        if (args.length > 0 && new File(args[0]).exists()) {
            openFile = new File(args[0]);
        }
        String selectedTableName = null;
        if (openFile != null && openFile.getName().toLowerCase().endsWith(HMConstants.GPKG)) {
            selectedTableName = promptTableName(openFile);
        }

        final MainController controller = new MainController(openFile, selectedTableName);
        SettingsController.applySettings(controller);

        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine SLD Editor");

        GuiUtilities.setDefaultFrameIcon(frame);

        GuiUtilities.addClosingListener(frame, controller);

    }
}
