package org.hortonmachine.webmaps;

import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.filechooser.FileFilter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wms.CRSEnvelope;
import org.geotools.ows.wms.Layer;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wms.request.GetMapRequest;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.database.DatabaseViewer;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.images.WmsWrapper;
import org.hortonmachine.gui.settings.SettingsController;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.executor.ExecutorIndeterminateGui;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

@SuppressWarnings("unchecked")
public class WebMapsController extends WebMapsView implements IOnCloseListener {
    private WmsWrapper currentWms;

    private LinkedHashMap<String, Layer> name2LayersMap = new LinkedHashMap<>();

    private Map<String, CRSEnvelope> crsMap;

    private Map<String, StyleImpl> stylesMap;

    private ReferencedEnvelope readEnvelope;

    private CRSEnvelope selectedCrsEnv;

    public WebMapsController() {
        setPreferredSize(new Dimension(1400, 750));

        init();
    }
    private void init() {
        _loadButton.addActionListener(e -> {
            new ExecutorIndeterminateGui(){
                @Override
                public void backGroundWork() throws Exception {
                    loadService();
                }
            }.execute();
        });

        _layerNameFilterField.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped( KeyEvent e ) {
            }

            @Override
            public void keyReleased( KeyEvent e ) {
                Set<String> layerNames = name2LayersMap.keySet();
                String filterStr = _layerNameFilterField.getText().trim().toLowerCase();
                if (filterStr.length() > 0) {
                    layerNames = layerNames.stream().filter(n -> {
                        n = n.toLowerCase();
                        if (n.contains(filterStr)) {
                            return true;
                        }
                        return false;
                    }).collect(Collectors.toSet());
                }
                String[] names = layerNames.toArray(new String[0]);
                _layersCombo.setModel(new DefaultComboBoxModel<>(names));
            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });

        _boundsFileField.setEditable(false);
        _boundsLoadButton.addActionListener(e -> {

            if (selectedCrsEnv == null) {
                GuiUtilities.showWarningMessage(this, "Please select a CRS first.");
                return;
            }

            File[] selFile = GuiUtilities.showOpenFilesDialog(this, "Select file", false, PreferencesHandler.getLastFile(),
                    new FileFilter(){

                        @Override
                        public String getDescription() {
                            return "Shapefiles";
                        }

                        @Override
                        public boolean accept( File f ) {
                            if (f.isDirectory()) {
                                return true;
                            }
                            String n = f.getName();
                            for( String ext : HMConstants.SUPPORTED_VECTOR_EXTENSIONS ) {
                                if (n.toLowerCase().endsWith(ext)) {
                                    return true;
                                }
                            }
                            return false;
                        }
                    });
            if (selFile != null && selFile.length > 0) {
                _boundsFileField.setText(selFile[0].getAbsolutePath());
                new ExecutorIndeterminateGui(){
                    @Override
                    public void backGroundWork() throws Exception {
                        try {
                            readEnvelope = OmsVectorReader.readEnvelope(selFile[0].getAbsolutePath());
                            updateFileExportBounds();
                        } catch (Exception e1) {
                            GuiUtilities.handleError(WebMapsController.this, e1);
                        }
                    }
                }.execute();
            }

        });

        _outputSaveButton.addActionListener(e -> {
            File saveFile = GuiUtilities.showSaveFileDialog(this, "Save to geotiff", PreferencesHandler.getLastFile());
            if (saveFile != null) {
                _outputFileField.setText(saveFile.getAbsolutePath());
            }
        });

        _outputWithField.addKeyListener(new KeyListener(){

            @Override
            public void keyTyped( KeyEvent e ) {

            }

            @Override
            public void keyReleased( KeyEvent e ) {
                if (readEnvelope == null) {
                    _outputHeightField.setText("First select ROI shapefile.");

                    return;
                }
                String widthStr = _outputWithField.getText();
                try {
                    int width = Integer.parseInt(widthStr);

                    double sw = readEnvelope.getWidth();
                    double sh = readEnvelope.getHeight();

                    int height = (int) (width * sh / sw);

                    _outputHeightField.setText(height + "");

                } catch (NumberFormatException e1) {
                    _outputHeightField.setText("Width has to be integer.");
                }

            }

            @Override
            public void keyPressed( KeyEvent e ) {
            }
        });
        _outputHeightField.setEditable(false);

        _wms2tiffButton.addActionListener(e -> {
            new ExecutorIndeterminateGui(){
                @Override
                public void backGroundWork() throws Exception {
                    getFinalImage();
                }
            }.execute();

        });

        _loadPreviewButton.addActionListener(ev -> {
            new ExecutorIndeterminateGui(){
                @Override
                public void backGroundWork() throws Exception {
                    getPreviewImage();
                }
            }.execute();

        });
    }
    private void getFinalImage() {
        int imageWidth = Integer.parseInt(_outputWithField.getText());
        int imageHeight = Integer.parseInt(_outputHeightField.getText());

//            String filePath = _boundsFileField.getText();
        if (readEnvelope == null) {
            GuiUtilities.showWarningMessage(this, "A bounds file has to be loaded to export to geotiff.");
            return;
        }
        ReferencedEnvelope envelope = readEnvelope;

//            try {
//                if (filePath.endsWith(HMConstants.SUPPORTED_VECTOR_EXTENSIONS[0])) {
//                    envelope = OmsVectorReader.readEnvelope(filePath);
//                } else {
//                    GridCoverage2D raster = OmsRasterReader.readRaster(filePath);
//                    Polygon regionPolygon = CoverageUtilities.getRegionPolygon(raster);
//                    envelope = new ReferencedEnvelope(regionPolygon.getEnvelopeInternal(), raster.getCoordinateReferenceSystem());
//                }
//            } catch (Exception e2) {
//                e2.printStackTrace();
//                GuiUtilities.showErrorMessage(this, "Could not load bounds from file: " + e2.getLocalizedMessage());
//                return;
//            }

        try {

            String style = "";
            Object selectedStyleObj = _stylesCombo.getSelectedItem();
            if (selectedStyleObj != null) {
                style = selectedStyleObj.toString();
            }
            StyleImpl styleImpl = stylesMap.get(style);

            String selectedLayer = _layersCombo.getSelectedItem().toString();
            Layer layer = name2LayersMap.get(selectedLayer);

            String selectedFormat = _formatsCombo.getSelectedItem().toString();

            String epsg = _crsCombo.getSelectedItem().toString();
            CoordinateReferenceSystem crs = getCrs(epsg);

            ReferencedEnvelope env = envelope.transform(crs, true);

            GetMapRequest mapRequest = currentWms.getMapRequest(layer, selectedFormat, epsg, imageWidth, imageHeight, env, null,
                    styleImpl);
            GuiUtilities.copyToClipboard(currentWms.getUrl(mapRequest).toString());
            BufferedImage image = currentWms.getImage(mapRequest);
            if (image != null) {
                double xRes = env.getWidth() / imageWidth;
                double yRes = env.getHeight() / imageHeight;
                RegionMap envParams = CoverageUtilities.makeRegionParamsMap(env.getMaxY(), env.getMinY(), env.getMinX(),
                        env.getMaxX(), xRes, yRes, imageHeight, imageHeight);
                GridCoverage2D coverage = CoverageUtilities.buildCoverage("wms2tiff", image, envParams, crs);
                String outPath = _outputFileField.getText();
                OmsRasterWriter.writeRaster(outPath, coverage);

                CoverageUtilities.writeWorldFiles(coverage, outPath);

//                    ImageIO.write(image, "png", new File(outPath));

            } else {
                String message = currentWms.getMessage(mapRequest);
                if (message.contains("ServiceException")) {
                    final Pattern pattern = Pattern.compile("<ServiceException>(.+?)</ServiceException>", Pattern.DOTALL);
                    final Matcher matcher = pattern.matcher(message);
                    matcher.find();
                    message = matcher.group(1);
                    if (message != null) {
                        message = message.trim();
                        GuiUtilities.showWarningMessage(this, message);
                        return;
                    }
                }
                GuiUtilities.showWarningMessage(this, "Could not retrieve image for given parameters.");
            }

        } catch (Exception e1) {
            e1.printStackTrace();
            GuiUtilities.handleError(this, e1);
        }
    }
    private void loadService() {
        try {
            String url = _getCapabilitiesField.getText();

            boolean doProceed = simpleCheck(url);
            if (!doProceed) {
                doProceed = GuiUtilities.showYesNoDialog(this,
                        "The url doesn't contain the usual WMS service parameters (service=wms, request=getcapabilities).\nDo you want to proceed?");
                if (!doProceed) {
                    return;
                }
            }

            currentWms = new WmsWrapper(url);
            name2LayersMap.clear();

            WMSCapabilities capabilities = currentWms.getCapabilities();
            String serverName = capabilities.getService().getName() + "   (version: " + capabilities.getVersion() + ")";
            String serverTitle = capabilities.getService().getTitle();

            _serverNameLabel.setText(serverName);
            _serverTitleLabel.setText(serverTitle);

            String first = null;
            Layer[] layers = currentWms.getLayers();
            String[] names = new String[layers.length];
            for( int i = 0; i < layers.length; i++ ) {
                String layerName = layers[i].getName();
                name2LayersMap.put(layerName, layers[i]);
                if (first == null) {
                    first = layerName;
                }
                names[i] = layerName;
            }

            _layersCombo.setModel(new DefaultComboBoxModel<>(names));
            _layersCombo.addActionListener(ev -> {
                String selectedLayer = _layersCombo.getSelectedItem().toString();
                loadLayerInfo(selectedLayer);
            });
            loadLayerInfo(first);
        } catch (Exception e1) {
            e1.printStackTrace();
            GuiUtilities.handleError(this, e1);
        }
    }
    private void getPreviewImage() {
        int imageWidth = 256;
        int imageHeight = 256;

        try {

            String style = "";
            Object selectedStyleObj = _stylesCombo.getSelectedItem();
            if (selectedStyleObj != null) {
                style = selectedStyleObj.toString();
            }
            StyleImpl styleImpl = stylesMap.get(style);

            String selectedLayer = _layersCombo.getSelectedItem().toString();
            Layer layer = name2LayersMap.get(selectedLayer);

            String selectedFormat = _formatsCombo.getSelectedItem().toString();

            String epsg = _crsCombo.getSelectedItem().toString();
            CoordinateReferenceSystem crs = getCrs(epsg);

            CRSEnvelope crsEnvelope = crsMap.get(epsg);
            double w = crsEnvelope.getMinX();
            double e = crsEnvelope.getMaxX();
            double s = crsEnvelope.getMinY();
            double n = crsEnvelope.getMaxY();
            ReferencedEnvelope env = new ReferencedEnvelope(w, e, s, n, crs);
            if (readEnvelope != null) {
                env = readEnvelope;
            }

            GetMapRequest mapRequest = currentWms.getMapRequest(layer, selectedFormat, epsg, imageWidth, imageHeight, env, null,
                    styleImpl);
            GuiUtilities.copyToClipboard(currentWms.getUrl(mapRequest).toString());
            BufferedImage image = currentWms.getImage(mapRequest);
            if (image != null) {
                _previewImageLabel.setIcon(new ImageIcon(image));
            } else {
                String message = currentWms.getMessage(mapRequest);
                if (message.contains("ServiceException")) {
                    message = message.replace("ServiceExceptionReport", "");
                    String[] split = message.split("<ServiceException");
                    if (split.length == 2) {
                        message = split[1];
                        int indexOf = message.indexOf('>');
                        message = message.substring(indexOf + 1).trim();
                        indexOf = message.indexOf('<');
                        message = message.substring(0, indexOf);
                    } else {
                        final Pattern pattern = Pattern.compile("<ServiceException(.+?)>(.+?)</ServiceException>",
                                Pattern.DOTALL);
                        final Matcher matcher = pattern.matcher(message);
                        matcher.find();
                        message = matcher.group(1);
                    }
                    if (message != null) {
                        message = message.trim();
                        GuiUtilities.showWarningMessage(this, message);
                        return;
                    }
                }
                GuiUtilities.showWarningMessage(this, "Could not retrieve image for given parameters.");
            }

        } catch (Exception e1) {
            e1.printStackTrace();
            GuiUtilities.handleError(this, e1);
        }
    }
    private void updateFileExportBounds() throws Exception {
        if (readEnvelope != null) {
            CoordinateReferenceSystem selectedCrs = getCrs(selectedCrsEnv.getEPSGCode());
            readEnvelope = readEnvelope.transform(selectedCrs, true);

            double north = readEnvelope.getMaxY();
            double south = readEnvelope.getMinY();
            double east = readEnvelope.getMaxX();
            double west = readEnvelope.getMinX();

            _northCrsFileLabel.setText(String.valueOf(north));
            _southCrsFileLabel.setText(String.valueOf(south));
            _westCrsFileLabel.setText(String.valueOf(west));
            _eastCrsFileLabel.setText(String.valueOf(east));
        }
    }

    private CoordinateReferenceSystem getCrs( String epsg ) throws Exception {
        if (epsg.toUpperCase().equals("EPSG:4326")) {
            return DefaultGeographicCRS.WGS84;
        }
        return CRS.decode(epsg);
    }
    private void loadLayerInfo( String layerName ) {
        Layer layer = name2LayersMap.get(layerName);

        _layerNameLabel.setText(layerName);
        _layerTitleLabel.setText(layerName);

        stylesMap = layer.getStyles().stream().collect(Collectors.toMap(s -> s.getName(), Function.identity()));
        stylesMap.put("", null);
        String[] styleNames = stylesMap.keySet().toArray(new String[0]);
        _stylesCombo.setModel(new DefaultComboBoxModel<String>(styleNames));

        List<String> formats = currentWms.getFormats();
        _formatsCombo.setModel(new DefaultComboBoxModel<>(formats.toArray(new String[0])));

        crsMap = layer.getBoundingBoxes();
        _crsCombo.setModel(new DefaultComboBoxModel<>(crsMap.keySet().toArray(new String[0])));
        _crsCombo.addActionListener(e -> {
            String crsName = _crsCombo.getSelectedItem().toString();
            loadCrsInfo(crsName);
            try {
                updateFileExportBounds();
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });
        loadCrsInfo(_crsCombo.getSelectedItem().toString());

    }

    private void loadCrsInfo( String crsName ) {
        selectedCrsEnv = crsMap.get(crsName);
        double north = selectedCrsEnv.getMaxY();
        double south = selectedCrsEnv.getMinY();
        double east = selectedCrsEnv.getMaxX();
        double west = selectedCrsEnv.getMinX();

        _northCrsLabel.setText(String.valueOf(north));
        _southCrsLabel.setText(String.valueOf(south));
        _westCrsLabel.setText(String.valueOf(west));
        _eastCrsLabel.setText(String.valueOf(east));
    }

    private boolean simpleCheck( String url ) {
        url = url.toLowerCase();
        return url.startsWith("http") && url.contains("service=wms") && url.contains("request=getcapabilities");
    }

    public JComponent asJComponent() {
        return this;
    }

    @Override
    public void onClose() {
        SettingsController.onCloseHandleSettings();
    }

    public boolean canCloseWithoutPrompt() {
        return currentWms == null;
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();

        final WebMapsController controller = new WebMapsController();
        SettingsController.applySettings(controller);

        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Web Maps Downloader");

        GuiUtilities.setDefaultFrameIcon(frame);

        GuiUtilities.addClosingListener(frame, controller);

    }

}
