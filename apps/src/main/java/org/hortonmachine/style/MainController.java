package org.hortonmachine.style;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.geotools.map.MapContent;
import org.geotools.styling.Style;
import org.geotools.swing.JMapPane;
import org.hortonmachine.database.DatabaseViewer;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.style.FeatureTypeStyleWrapper;
import org.hortonmachine.gears.utils.style.LineSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.PointSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.PolygonSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.RuleWrapper;
import org.hortonmachine.gears.utils.style.StyleWrapper;
import org.hortonmachine.gears.utils.style.SymbolizerWrapper;
import org.hortonmachine.gears.utils.style.TextSymbolizerWrapper;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.modules.VectorReader;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class MainController extends MainView implements IOnCloseListener, TreeSelectionListener {
    public static final int COLOR_IMAGE_SIZE = 15;

    private MapContent mapContent;
    private List<SimpleFeature> currentFeaturesList;
    private int currentFeatureIndex = 0;
    private FeatureLayer currentFeaturesLayer;
    private JMapPane mapPane;
    private StyleWrapper styleWrapper;

    private String[] featureCollectionFieldNames;

    /**
    * Default constructor
    */
    public MainController() {
        setPreferredSize(new Dimension(1100, 800));
        init();
    }

    private void init() {
        _rulesTree.setModel(new DefaultTreeModel(null));
        _stylePanel.setLayout(new BorderLayout());

        mapContent = new MapContent();

        mapPane = new JMapPane(mapContent);
        _mapPaneHolder.setLayout(new BorderLayout());
        _mapPaneHolder.add(mapPane, BorderLayout.CENTER);

        String[] supportedExtensions = HMConstants.SUPPORTED_VECTOR_EXTENSIONS;
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
            fileChooser.setCurrentDirectory(GuiUtilities.getLastFile());
            int result = fileChooser.showOpenDialog(this);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                if (selectedFiles != null && selectedFiles.length > 0) {
                    GuiUtilities.setLastPath(selectedFiles[0].getAbsolutePath());
                    _filepathField.setText(selectedFiles[0].getAbsolutePath());
                    try {
                        if (currentFeaturesLayer != null) {
                            mapContent.removeLayer(currentFeaturesLayer);
                        }

                        SimpleFeatureCollection currentFeatureCollection = VectorReader
                                .readVector(selectedFiles[0].getAbsolutePath());

                        featureCollectionFieldNames = FeatureUtilities.featureCollectionFieldNames(currentFeatureCollection);
                        currentFeaturesList = FeatureUtilities.featureCollectionToList(currentFeatureCollection);

                        CoordinateReferenceSystem currentCRS = currentFeatureCollection.getSchema()
                                .getCoordinateReferenceSystem();
                        mapContent.getViewport().setCoordinateReferenceSystem(currentCRS);

                        Style style = SldUtilities.getStyleFromFile(selectedFiles[0]);

                        currentFeaturesLayer = new FeatureLayer(currentFeatureCollection, style);
                        mapContent.addLayer(currentFeaturesLayer);

                        styleWrapper = new StyleWrapper(style);

                        loadFeatureCollection();
                        reloadGroupsAndRules();

                        _stylePanel.removeAll();
                        _stylePanel.revalidate();
                        _stylePanel.repaint();
                    } catch (IOException e1) {

                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    }

                }
            }
        });

        _nextButton.addActionListener(e -> {
            currentFeatureIndex++;
            loadFeatureCollection();
        });
        _previousButton.addActionListener(e -> {
            currentFeatureIndex--;
            loadFeatureCollection();
        });

    }

    private void reloadGroupsAndRules() {
        List<FeatureTypeStyleWrapper> featureTypeStylesWrapperList = styleWrapper.getFeatureTypeStylesWrapperList();
        DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("FeatureTypeStyle");
        DefaultTreeModel model = new DefaultTreeModel(rootNode);

        for( FeatureTypeStyleWrapper featureTypeStyle : featureTypeStylesWrapperList ) {
            DefaultMutableTreeNode featureStyleNode = new DefaultMutableTreeNode(featureTypeStyle);
            rootNode.add(featureStyleNode);

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

        _rulesTree.setModel(model);
        for( int i = 0; i < _rulesTree.getRowCount(); i++ ) {
            _rulesTree.expandRow(i);
        }
        _rulesTree.addTreeSelectionListener(this);
    }

    private void loadFeatureCollection() {
        if (currentFeaturesList != null) {
            int size = currentFeaturesList.size();
            if (currentFeatureIndex < 0) {
                currentFeatureIndex = 0;
            } else if (currentFeatureIndex > size - 1) {
                currentFeatureIndex = size - 1;
            }

            SimpleFeature simpleFeature = currentFeaturesList.get(currentFeatureIndex);
            Envelope env = ((Geometry) simpleFeature.getDefaultGeometry()).getEnvelopeInternal();
            if (env.getWidth() == 0) {
                env.expandBy(0.1);
            } else {
                env.expandBy(env.getWidth() * 0.05);
            }

            mapPane.setDisplayArea(new ReferencedEnvelope(env, simpleFeature.getFeatureType().getCoordinateReferenceSystem()));
        }

    }

    public JComponent asJComponent() {
        return this;
    }

    @Override
    public void onClose() {
        // TODO Auto-generated method stub

    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        final MainController controller = new MainController();

        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine SLD Editor");

        Class<DatabaseViewer> class1 = DatabaseViewer.class;
        ImageIcon icon = new ImageIcon(class1.getResource("/org/hortonmachine/images/hm150.png"));
        frame.setIconImage(icon.getImage());

        GuiUtilities.addClosingListener(frame, controller);

    }

    @Override
    public void valueChanged( TreeSelectionEvent e ) {
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) _rulesTree.getLastSelectedPathComponent();

        if (node == null)
            return;
        _stylePanel.removeAll();

        Object nodeInfo = node.getUserObject();
        if (node.isLeaf()) {
            if (nodeInfo instanceof PolygonSymbolizerWrapper) {
                PolygonSymbolizerWrapper symbolizerWrapper = (PolygonSymbolizerWrapper) nodeInfo;
                _stylePanel.add(new PolygonSymbolizerController(symbolizerWrapper, this), BorderLayout.CENTER);
            } else if (nodeInfo instanceof LineSymbolizerWrapper) {
                LineSymbolizerWrapper symbolizerWrapper = (LineSymbolizerWrapper) nodeInfo;
                _stylePanel.add(new LineSymbolizerController(symbolizerWrapper, this), BorderLayout.CENTER);
            } else if (nodeInfo instanceof PointSymbolizerWrapper) {
                PointSymbolizerWrapper symbolizerWrapper = (PointSymbolizerWrapper) nodeInfo;
                _stylePanel.add(new PointMarkSymbolizerController(symbolizerWrapper, this), BorderLayout.CENTER);
            } else if (nodeInfo instanceof TextSymbolizerWrapper) {
                TextSymbolizerWrapper symbolizerWrapper = (TextSymbolizerWrapper) nodeInfo;
                _stylePanel.add(new TextSymbolizerController(symbolizerWrapper, featureCollectionFieldNames, this),
                        BorderLayout.CENTER);
            }
        }
        _stylePanel.revalidate();
        _stylePanel.repaint();
    }

    public void applyStyle() {
        Style style = styleWrapper.getStyle();
        currentFeaturesLayer.setStyle(style);
    }

}
