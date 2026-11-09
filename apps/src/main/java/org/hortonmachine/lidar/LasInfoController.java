package org.hortonmachine.lidar;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.ReferencedEnvelope3D;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.Las;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.utils.LasConstraints;
import org.hortonmachine.gears.io.las.utils.LasPointColumns;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.ui.progress.ProgressUpdate;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.crs.CrsUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.gui.utils.VerticalRangeSlider;
import org.hortonmachine.gui.utils.executor.ExecutorIndeterminateGui;
import org.hortonmachine.gui.utils.executor.ExecutorProgressGui;
import org.hortonmachine.nww.elevation.ConstantElevationModel;
import org.hortonmachine.nww.elevation.CoverageElevationModel;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.ViewControlsLayer;
import org.hortonmachine.nww.layers.defaults.other.LasPointCloudLayer;
import org.hortonmachine.nww.layers.defaults.other.LasPointCloudLayer.ColorMode;
import org.hortonmachine.nww.layers.defaults.raster.EsriSatellite;
import org.hortonmachine.nww.layers.defaults.raster.OSMMapnikLayer;
import org.hortonmachine.nww.utils.selection.SectorSelector;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Box;
import gov.nasa.worldwind.geom.LatLon;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.view.orbit.OrbitView;

/**
 * An app to inspect las/laz files: header information, filtered loading of the points and their
 * 3D view, with interactive slicing by value and selection of the bounds to load.
 */
public class LasInfoController extends LasInfoView implements IOnCloseListener, KeyListener {
    private static final long serialVersionUID = 1L;

    /** The meters of a degree, used to place the data on a local plane if they have no CRS. */
    private static final double METERS_PER_DEGREE = 111320.0;
    /** The points of each side of the rectangles transformed to longitude/latitude. */
    private static final int POINTS_PER_SIDE = 10;

    private static final String BASEMAP_OSM = "OpenStreetMap";
    private static final String BASEMAP_SATELLITE = "Satellite";
    private static final String BASEMAP_NONE = "No base map";

    private static final Color FILE_EXTENT_COLOR = new Color(255, 255, 255);
    private static final Color BOUNDS_COLOR = new Color(230, 30, 30);

    /** The mode of the view: map or 3D, and the terrain under the points. */
    private enum ViewMode {
        MAP_2D("2D map"), FLAT("3D, flat ground"), COPERNICUS("3D, Copernicus terrain"), DTM("3D, terrain from the DTM");

        final String description;

        ViewMode( String description ) {
            this.description = description;
        }

        boolean hasTerrain() {
            return this == COPERNICUS || this == DTM;
        }

        @Override
        public String toString() {
            return description;
        }
    }

    private ALasReader lasReader;

    private LasConstraints constraints = new LasConstraints();

    private GridCoverage2D dtm;

    /*
     * The 3D viewer, null if the native libraries of NWW could not be loaded.
     */
    private NwwPanel nwwPanel;
    private final LasPointCloudLayer pointsLayer = new LasPointCloudLayer("Las points");
    /** The outlines of the extent of the file and of the bounds to load. */
    private final RenderableLayer extentLayer = new RenderableLayer();
    private Layer osmLayer;
    private Layer satelliteLayer;
    private SectorSelector sectorSelector;
    /** The description of the globe and terrain set, to set them again only if they change. */
    private String appliedTerrain;
    /** The terrain of the DTM, created once per DTM. */
    private gov.nasa.worldwind.globes.ElevationModel dtmElevationModel;
    /** The range of the elevations of the points as drawn, absolute or normalized. */
    private double drawnMinZ = Double.NaN;
    private double drawnMaxZ = Double.NaN;
    /** The mode selected before the current one, to go back to if the current is not available. */
    private ViewMode lastViewMode = ViewMode.FLAT;

    private final VerticalRangeSlider rangeSlider = new VerticalRangeSlider();
    private final JTextField highField = new JTextField(6);
    private final JTextField lowField = new JTextField(6);
    private final JToggleButton lockToggle = new JToggleButton("lock");
    private final JTextField sliceWidthField = new JTextField(6);
    private final JSpinner pointSizeSpinner = new JSpinner(new SpinnerNumberModel(2, 1, 20, 1));
    private final JLabel statusLabel = new JLabel(" ");
    private final JComboBox<String> basemapCombo = new JComboBox<>(
            new String[]{BASEMAP_SATELLITE, BASEMAP_OSM, BASEMAP_NONE});
    private final JComboBox<ViewMode> viewModeCombo = new JComboBox<>(ViewMode.values());
    private final JCheckBox normalizeCheck = new JCheckBox("normalize with DTM");
    private final JCheckBox overTerrainCheck = new JCheckBox("points over terrain", true);
    private final JButton fitColorsButton = new JButton("fit colors");
    private final JButton autoColorsButton = new JButton("auto colors");
    /**
     * The range of the colors fitted to the slider, <code>null</code> for the default range,
     * which leaves out the extreme values.
     */
    private volatile double[] fittedColorRange;

    /*
     * The georeferencing of the file: the transforms from and to longitude/latitude, or the origin
     * of the local plane if the file has no CRS.
     */
    private CoordinateReferenceSystem lasCrs;
    private MathTransform toLonLat;
    private MathTransform fromLonLat;
    private double localOriginX;
    private double localOriginY;
    private ReferencedEnvelope3D fileEnvelope;

    public LasInfoController() {
        setPreferredSize(new Dimension(1800, 1100));

        createViewer();
        init();
    }

    /**
     * Create the 3D viewer, with its toolbar and the range slider at its side.
     */
    private void createViewer() {
        Component nwwComponent = NwwPanel.createNwwPanel(false);
        if (!(nwwComponent instanceof NwwPanel)) {
            // the panel with the error message
            if (nwwComponent != null) {
                _viewerPanel.add(nwwComponent, BorderLayout.CENTER);
            }
            return;
        }
        nwwPanel = (NwwPanel) nwwComponent;
        osmLayer = new OSMMapnikLayer();
        osmLayer.setEnabled(false);
        nwwPanel.addLayer(osmLayer);
        satelliteLayer = new EsriSatellite();
        nwwPanel.addLayer(satelliteLayer);
        extentLayer.setName("Extents");
        extentLayer.setPickEnabled(false);
        nwwPanel.addLayer(extentLayer);
        nwwPanel.addLayer(pointsLayer);
        ViewControlsLayer viewControls = nwwPanel.addViewControls();
        viewControls.setScale(1.5);

        sectorSelector = new SectorSelector(nwwPanel.getWwd());
        sectorSelector.setInteriorColor(BOUNDS_COLOR);
        sectorSelector.setInteriorOpacity(0.15);
        sectorSelector.setBorderColor(BOUNDS_COLOR);
        sectorSelector.setBorderWidth(2);
        sectorSelector.addPropertyChangeListener(SectorSelector.SECTOR_PROPERTY, evt -> {
            // fired while dragging, and with a null value when the mouse is released
            Sector sector = sectorSelector.getSector();
            if (sector != null) {
                SwingUtilities.invokeLater(() -> setBoundsFromSector(sector));
            }
        });

        // the toolbar on top of the viewer
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);
        JButton zoomButton = new JButton(ImageCache.getInstance().getImage(ImageCache.ZOOM_TO_ALL));
        zoomButton.setToolTipText("Zoom to the data");
        zoomButton.addActionListener(e -> zoomToData());
        toolBar.add(zoomButton);
        basemapCombo.setToolTipText("The base map under the points");
        basemapCombo.setMaximumSize(basemapCombo.getPreferredSize());
        basemapCombo.addActionListener(e -> applyBasemap());
        applyBasemap();
        toolBar.add(basemapCombo);
        viewModeCombo.setToolTipText("<html>The view: a 2D map, or 3D with flat ground under the points,<br>"
                + "the Copernicus terrain or the terrain of the DTM</html>");
        viewModeCombo.setSelectedItem(ViewMode.FLAT);
        viewModeCombo.setMaximumSize(viewModeCombo.getPreferredSize());
        viewModeCombo.addActionListener(e -> onViewModeChanged());
        toolBar.add(viewModeCombo);
        normalizeCheck.setToolTipText("<html>Use the heights over the DTM for coloring and slicing,<br>"
                + "and in the 3D view with flat ground draw the points at their height over a flat terrain</html>");
        normalizeCheck.setEnabled(false);
        normalizeCheck.addActionListener(e -> {
            // other values: back to their default range
            fittedColorRange = null;
            recolorInBackground();
        });
        toolBar.add(normalizeCheck);
        overTerrainCheck.setToolTipText("<html>Never hide the points under the terrain, useful when<br>"
                + "the elevations of points and terrain do not match</html>");
        overTerrainCheck.setEnabled(false);
        overTerrainCheck.addActionListener(e -> {
            pointsLayer.setDrawOverTerrain(overTerrainCheck.isSelected() && getViewMode().hasTerrain());
            redraw();
        });
        toolBar.add(overTerrainCheck);
        toolBar.addSeparator();
        toolBar.add(new JLabel("point size "));
        pointSizeSpinner.setToolTipText("The size of the points, in pixels");
        pointSizeSpinner.setMaximumSize(pointSizeSpinner.getPreferredSize());
        pointSizeSpinner.addChangeListener(e -> {
            pointsLayer.setPointSize(((Number) pointSizeSpinner.getValue()).doubleValue());
            redraw();
        });
        pointsLayer.setPointSize(((Number) pointSizeSpinner.getValue()).doubleValue());
        toolBar.add(pointSizeSpinner);
        toolBar.addSeparator();
        fitColorsButton.setToolTipText("<html>Stretch the colors over the range of the slider, which then spans it:<br>"
                + "useful to see the details of the points between outliers</html>");
        fitColorsButton.setEnabled(false);
        fitColorsButton.addActionListener(e -> {
            fittedColorRange = new double[]{rangeSlider.getLowValue(), rangeSlider.getHighValue()};
            // the slider then spans the slice: a locked slice could not move any more
            lockToggle.setSelected(false);
            rangeSlider.setLocked(false);
            recolorInBackground();
        });
        toolBar.add(fitColorsButton);
        autoColorsButton.setToolTipText(String.format(
                "<html>Colors and slider over all the values, leaving out the extreme %s%% at each end<br>"
                        + "for elevation and intensity, so that a few outliers do not flatten the colors</html>",
                LasPointCloudLayer.AUTO_CUT_PERCENT));
        autoColorsButton.setEnabled(false);
        autoColorsButton.addActionListener(e -> {
            fittedColorRange = null;
            recolorInBackground();
        });
        toolBar.add(autoColorsButton);
        toolBar.addSeparator();
        toolBar.add(statusLabel);

        // the range slider at the side, with the exact values of its ends
        JPanel slicePanel = new JPanel(new BorderLayout(0, 4));
        slicePanel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 2));
        highField.setToolTipText("The highest value shown, press enter to apply");
        lowField.setToolTipText("The lowest value shown, press enter to apply");
        slicePanel.add(highField, BorderLayout.NORTH);
        slicePanel.add(rangeSlider, BorderLayout.CENTER);
        // the lock of the knobs, to move a slice of fixed width
        lockToggle.setToolTipText("<html>Lock the knobs together: the range keeps its width and moves as a slice,<br>"
                + "dragging it, clicking on the track or with the mouse wheel (by half of the width)</html>");
        lockToggle.setEnabled(false);
        lockToggle.addActionListener(e -> {
            if (lockToggle.isSelected()) {
                applySliceWidth();
            } else {
                rangeSlider.setLocked(false);
            }
        });
        sliceWidthField.setToolTipText("The width of the locked slice, press enter to lock with it");
        sliceWidthField.setEnabled(false);
        sliceWidthField.addActionListener(e -> {
            lockToggle.setSelected(true);
            applySliceWidth();
        });
        JPanel southPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        southPanel.add(lowField);
        southPanel.add(lockToggle);
        southPanel.add(sliceWidthField);
        slicePanel.add(southPanel, BorderLayout.SOUTH);
        rangeSlider.setEnabled(false);
        highField.setEnabled(false);
        lowField.setEnabled(false);
        rangeSlider.addChangeListener(e -> onRangeChanged());
        highField.addActionListener(e -> applyRangeFields(highField));
        lowField.addActionListener(e -> applyRangeFields(lowField));
        FocusAdapter applyOnFocusLost = new FocusAdapter(){
            @Override
            public void focusLost( FocusEvent e ) {
                applyRangeFields(e.getComponent());
            }
        };
        highField.addFocusListener(applyOnFocusLost);
        lowField.addFocusListener(applyOnFocusLost);

        _viewerPanel.add(toolBar, BorderLayout.NORTH);
        _viewerPanel.add(nwwPanel, BorderLayout.CENTER);
        _viewerPanel.add(slicePanel, BorderLayout.EAST);
    }

    private void init() {
        _inputPathField.setEditable(false);
        GuiUtilities.setFileBrowsingOnWidgets(_inputPathField, _loadButton, new String[]{"las", "laz"}, () -> {
            new ExecutorIndeterminateGui(){
                @Override
                public void backGroundWork() throws Exception {
                    loadNewFile();
                }
            }.execute();
        });
        _dtmInputPathField.setEditable(false);
        GuiUtilities.setFileBrowsingOnWidgets(_dtmInputPathField, _loadDtmButton, new String[]{"asc", "tif", "tiff"}, () -> {
            new ExecutorIndeterminateGui(){
                @Override
                public void backGroundWork() throws Exception {
                    loadDtm();
                }
            }.execute();
        });

        _boundsLoadButton.addActionListener(e -> {
            File[] shpFile = GuiUtilities.showOpenFilesDialog(_boundsLoadButton, "Select bounds file...", false,
                    PreferencesHandler.getLastFile(), new GuiUtilities.ShpFileFilter());
            if (shpFile != null) {
                new ExecutorIndeterminateGui(){
                    @Override
                    public void backGroundWork() throws Exception {
                        loadBoundsFromFile(shpFile[0]);
                    }
                }.execute();
            }
        });
        _boundsSelectButton.setEnabled(sectorSelector != null);
        _boundsSelectButton.addActionListener(e -> {
            if (sectorSelector == null) {
                return;
            }
            if (_boundsSelectButton.isSelected()) {
                sectorSelector.enable();
            } else {
                sectorSelector.disable();
            }
            updateExtentLayer();
        });
        _boundsClearButton.addActionListener(e -> {
            setBoundsFields(null);
            if (sectorSelector != null && _boundsSelectButton.isSelected()) {
                // start a new selection
                sectorSelector.disable();
                sectorSelector.enable();
            }
        });

        _samplingField.setText("1000");
        constraints.setSampling(1000);
        _samplingField.addKeyListener(this);
        _classesField.addKeyListener(this);
        _impulsesField.addKeyListener(this);
        _intensityRangeField.addKeyListener(this);
        _westField.addKeyListener(this);
        _eastField.addKeyListener(this);
        _southField.addKeyListener(this);
        _northField.addKeyListener(this);
        _minZField.addKeyListener(this);
        _maxZField.addKeyListener(this);
        _lowerThresField.addKeyListener(this);
        _upperThresField.addKeyListener(this);

        _elevationRadio.setSelected(true);
        for( javax.swing.JRadioButton radio : new javax.swing.JRadioButton[]{_elevationRadio, _intensityRadio, _classRadio,
                _impulseRadio, _ownColorRadio} ) {
            radio.addActionListener(e -> {
                // other values: back to their default range
                fittedColorRange = null;
                recolorInBackground();
            });
        }

        _loadPreviewButton.addActionListener(e -> {
            if (checkReader()) {
                new ExecutorIndeterminateGui(){
                    @Override
                    public void backGroundWork() throws Exception {
                        constraints.applyConstraints(lasReader, false, null);
                        showPoints(constraints.getFilteredColumns(), false);
                    }
                }.execute();
            }
        });
        _loadDataButton.addActionListener(e -> {
            if (checkReader()) {
                loadDataAction();
            }
        });
    }

    private boolean checkReader() {
        if (lasReader == null) {
            GuiUtilities.showWarningMessage(this, "No data reader seems to be available. Load some data!");
            return false;
        } else {
            return true;
        }
    }

    private void loadDtm() {
        String dtmFilePath = _dtmInputPathField.getText();
        try {
            if (dtmFilePath.trim().length() > 0) {
                dtm = OmsRasterReader.readRaster(dtmFilePath);
            } else {
                dtm = null;
            }
            constraints.setDtm(dtm);
            SwingUtilities.invokeLater(() -> {
                dtmElevationModel = null;
                normalizeCheck.setEnabled(dtm != null);
                normalizeCheck.setSelected(dtm != null);
                if (getViewMode() == ViewMode.DTM) {
                    if (dtm == null) {
                        viewModeCombo.setSelectedItem(ViewMode.FLAT);
                    } else {
                        // the terrain of the new DTM
                        appliedTerrain = null;
                        applyViewMode();
                    }
                }
                if (pointsLayer.getPointsCount() > 0 && dtm != null) {
                    statusLabel.setText("DTM set: refilter or load the data to compute the heights over it");
                }
            });
        } catch (Exception e) {
            GuiUtilities.handleError(_boundsLoadButton, e);
        }
    }

    private void loadDataAction() {
        ILasHeader header = lasReader.getHeader();
        long recordsCount = header.getRecordsCount();
        int work = (int) (recordsCount / 1000);
        // release the points shown, so that they are not in memory together with the new ones
        clearPoints();

        new ExecutorProgressGui(work){
            @Override
            public void backGroundWork() throws Exception {
                constraints.applyConstraints(lasReader, true, this);
                publish(new ProgressUpdate("Preparing the 3D view...", work));
                showPoints(constraints.getFilteredColumns(), true);
            }
        }.execute();
    }

    private void loadBoundsFromFile( File shpFile ) {
        String boundsFilePath = shpFile.getAbsolutePath();
        try {
            if (boundsFilePath.trim().length() > 0) {
                ReferencedEnvelope env = OmsVectorReader.readEnvelope(boundsFilePath);
                CoordinateReferenceSystem envCrs = env.getCoordinateReferenceSystem();
                Envelope bounds = env;
                if (envCrs != null && lasCrs != null && !HMCrsRegistry.crsEquals(envCrs, lasCrs)) {
                    bounds = new HMCrsTransformer(envCrs, lasCrs).transformDensified(env, POINTS_PER_SIDE);
                }
                Envelope _bounds = bounds;
                SwingUtilities.invokeLater(() -> setBoundsFields(_bounds));
            } else {
                SwingUtilities.invokeLater(() -> setBoundsFields(null));
            }
        } catch (Exception e) {
            GuiUtilities.handleError(_boundsLoadButton, e);
        }
    }

    /**
     * Set the bounds to load, in the CRS of the file, updating fields, constraints and map.
     *
     * @param bounds the bounds, or <code>null</code> to remove them.
     */
    private void setBoundsFields( Envelope bounds ) {
        if (bounds != null) {
            _westField.setText(String.valueOf(bounds.getMinX()));
            _eastField.setText(String.valueOf(bounds.getMaxX()));
            _southField.setText(String.valueOf(bounds.getMinY()));
            _northField.setText(String.valueOf(bounds.getMaxY()));
        } else {
            _westField.setText("");
            _eastField.setText("");
            _southField.setText("");
            _northField.setText("");
        }
        checkConstraint(_westField);
        checkConstraint(_eastField);
        checkConstraint(_southField);
        checkConstraint(_northField);
        updateExtentLayer();
    }

    /**
     * Set the bounds from a sector selected on the map.
     */
    private void setBoundsFromSector( Sector sector ) {
        if (fileEnvelope == null) {
            return;
        }
        try {
            double[] ring = rectangleRing(sector.getMinLongitude().degrees, sector.getMinLatitude().degrees,
                    sector.getMaxLongitude().degrees, sector.getMaxLatitude().degrees);
            fromLonLat(ring);
            Envelope bounds = new Envelope();
            for( int i = 0; i < ring.length; i += 2 ) {
                bounds.expandToInclude(ring[i], ring[i + 1]);
            }
            setBoundsFields(bounds);
        } catch (Exception e) {
            GuiUtilities.handleError(_boundsLoadButton, e);
        }
    }

    private void loadNewFile() {
        String inputPath = _inputPathField.getText();
        try {
            if (inputPath.trim().length() > 0) {
                lasReader = Las.getReader(new File(inputPath));
                ILasHeader header = lasReader.getHeader();

                List<String[]> headerInfoList = getHeaderInfo(header);
                List<String[]> firstPointInfoList = getFirstPointInfo();
                File lasFile = new File(inputPath);
                SwingUtilities.invokeLater(() -> {
                    _headerTable.setModel(
                            new DefaultTableModel(headerInfoList.toArray(new String[0][0]), new String[]{"Property", "Value"}));
                    _firstPointTable.setModel(
                            new DefaultTableModel(firstPointInfoList.toArray(new String[0][0]), new String[]{"Property", "Value"}));
                    clearPoints();
                    CoordinateReferenceSystem crs = header.getCrs();
                    if (crs == null) {
                        crs = askForCrs(lasFile);
                    }
                    try {
                        setupGeoreference(header.getDataEnvelope(), crs);
                    } catch (Exception e) {
                        // e.g. coordinates that do not fit the CRS: shown on a local plane
                        GuiUtilities.showWarningMessage(this,
                                "Unable to place the data with their CRS, they are shown on a local plane: " + e.getMessage());
                        try {
                            setupGeoreference(header.getDataEnvelope(), null);
                        } catch (Exception e1) {
                            GuiUtilities.handleError(_boundsLoadButton, e1);
                            return;
                        }
                    }
                    String status = String.format("%,d points in the file, none loaded", header.getRecordsCount());
                    if (lasCrs == null) {
                        status += " - no CRS, shown on a local plane";
                    }
                    statusLabel.setText(status);
                    if (nwwPanel != null) {
                        applyBasemap();
                        if (getViewMode().hasTerrain() && lasCrs == null) {
                            // the terrain needs the data in their place
                            viewModeCombo.setSelectedItem(ViewMode.FLAT);
                        }
                        applyViewMode();
                        updateExtentLayer();
                        zoomTo(fileEnvelope, fileEnvelope.getMinZ(), fileEnvelope.getMaxZ(), 0);
                    }
                });
            } else {
                lasReader = null;
                fileEnvelope = null;
                SwingUtilities.invokeLater(() -> {
                    _headerTable.setModel(new DefaultTableModel(new String[0][0], new String[]{"Property", "Value"}));
                    _firstPointTable.setModel(new DefaultTableModel(new String[0][0], new String[]{"Property", "Value"}));
                    clearPoints();
                    updateExtentLayer();
                });
            }
        } catch (Exception e) {
            GuiUtilities.handleError(_boundsLoadButton, e);
        }
    }

    /**
     * Ask the CRS of a file that has none, optionally saving it in a prj file next to it, so that
     * it is found next time. Has to be called in the UI thread.
     *
     * @param lasFile the las file.
     * @return the CRS chosen, or <code>null</code> to show the data on a local plane.
     */
    private CoordinateReferenceSystem askForCrs( File lasFile ) {
        JTextField codeField = new JTextField(14);
        JCheckBox savePrjCheck = new JCheckBox("Save it as prj file next to the las file");
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel("<html>The file <b>" + lasFile.getName() + "</b> has no CRS.<br>"
                + "Enter the EPSG code of its coordinates (e.g. EPSG:32632),<br>"
                + "or leave it empty for local coordinates, shown on a plane without base map.</html>"),
                BorderLayout.NORTH);
        panel.add(codeField, BorderLayout.CENTER);
        panel.add(savePrjCheck, BorderLayout.SOUTH);
        while( true ) {
            int answer = JOptionPane.showConfirmDialog(this, panel, "Select the CRS", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            String code = codeField.getText().trim();
            if (answer != JOptionPane.OK_OPTION || code.isEmpty()) {
                return null;
            }
            if (code.matches("\\d+")) {
                code = "EPSG:" + code;
            }
            try {
                CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs(code, true);
                if (savePrjCheck.isSelected()) {
                    String name = lasFile.getName();
                    int dot = name.lastIndexOf('.');
                    File prjFile = new File(lasFile.getParentFile(), (dot > 0 ? name.substring(0, dot) : name) + ".prj");
                    CrsUtilities.writeProjectionFile(prjFile.getAbsolutePath(), null, crs);
                }
                return crs;
            } catch (Exception e) {
                GuiUtilities.showWarningMessage(this, "Unable to use the CRS " + code + ": " + e.getMessage());
            }
        }
    }

    /**
     * Prepare the transforms between the CRS of the file and longitude/latitude.
     *
     * @param envelope the extent of the data.
     * @param crs the CRS of the data, or <code>null</code> to place them on a local plane.
     */
    private void setupGeoreference( ReferencedEnvelope3D envelope, CoordinateReferenceSystem crs ) throws Exception {
        fileEnvelope = envelope;
        lasCrs = crs;
        if (lasCrs != null) {
            HMCrsTransformer transformer = new HMCrsTransformer(lasCrs, HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true));
            toLonLat = transformer.getMathTransform();
            fromLonLat = toLonLat.inverse();
            // check that the data fit the CRS, as for a wrong CRS chosen for local coordinates
            double[] ring = rectangleRing(envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY());
            boolean valid;
            try {
                toLonLat(ring);
                valid = true;
                for( int i = 0; i < ring.length; i += 2 ) {
                    if (!Double.isFinite(ring[i]) || !Double.isFinite(ring[i + 1]) || Math.abs(ring[i]) > 180
                            || Math.abs(ring[i + 1]) > 90) {
                        valid = false;
                        break;
                    }
                }
            } catch (Exception e) {
                valid = false;
            }
            if (!valid) {
                lasCrs = null;
                toLonLat = null;
                fromLonLat = null;
                throw new IllegalArgumentException("the extent of the data is not valid in the CRS " + crs.getName());
            }
        } else {
            toLonLat = null;
            fromLonLat = null;
            localOriginX = (fileEnvelope.getMinX() + fileEnvelope.getMaxX()) / 2.0;
            localOriginY = (fileEnvelope.getMinY() + fileEnvelope.getMaxY()) / 2.0;
        }
    }

    /**
     * Transform interleaved x/y coordinates of the file to longitude/latitude, in place.
     */
    private void toLonLat( double[] xy ) throws Exception {
        if (toLonLat != null) {
            toLonLat.transform(xy, 0, xy, 0, xy.length / 2);
        } else {
            for( int i = 0; i < xy.length; i += 2 ) {
                xy[i] = (xy[i] - localOriginX) / METERS_PER_DEGREE;
                xy[i + 1] = (xy[i + 1] - localOriginY) / METERS_PER_DEGREE;
            }
        }
    }

    /**
     * Transform interleaved longitude/latitude coordinates to the x/y of the file, in place.
     */
    private void fromLonLat( double[] lonLat ) throws Exception {
        if (fromLonLat != null) {
            fromLonLat.transform(lonLat, 0, lonLat, 0, lonLat.length / 2);
        } else {
            for( int i = 0; i < lonLat.length; i += 2 ) {
                lonLat[i] = lonLat[i] * METERS_PER_DEGREE + localOriginX;
                lonLat[i + 1] = lonLat[i + 1] * METERS_PER_DEGREE + localOriginY;
            }
        }
    }

    /**
     * @return the interleaved coordinates of a closed ring along the sides of a rectangle, with
     *          {@link #POINTS_PER_SIDE} points per side, so that it stays accurate when transformed.
     */
    private static double[] rectangleRing( double minX, double minY, double maxX, double maxY ) {
        double[][] corners = {{minX, minY}, {maxX, minY}, {maxX, maxY}, {minX, maxY}};
        double[] ring = new double[(4 * POINTS_PER_SIDE + 1) * 2];
        int index = 0;
        for( int side = 0; side < 4; side++ ) {
            double[] from = corners[side];
            double[] to = corners[(side + 1) % 4];
            for( int i = 0; i < POINTS_PER_SIDE; i++ ) {
                double t = i / (double) POINTS_PER_SIDE;
                ring[index++] = from[0] + t * (to[0] - from[0]);
                ring[index++] = from[1] + t * (to[1] - from[1]);
            }
        }
        ring[index++] = minX;
        ring[index] = minY;
        return ring;
    }

    /**
     * @return the outline on the map of a rectangle in the CRS of the file.
     */
    private SurfacePolygon outline( Envelope env, Color color ) throws Exception {
        double[] ring = rectangleRing(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
        toLonLat(ring);
        List<LatLon> positions = new ArrayList<>();
        for( int i = 0; i < ring.length; i += 2 ) {
            positions.add(LatLon.fromDegrees(ring[i + 1], ring[i]));
        }
        BasicShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setDrawInterior(false);
        attributes.setOutlineMaterial(new Material(color));
        attributes.setOutlineWidth(2);
        return new SurfacePolygon(attributes, positions);
    }

    /**
     * @return the sector of a rectangle in the CRS of the file.
     */
    private Sector toSector( Envelope env ) throws Exception {
        double[] ring = rectangleRing(env.getMinX(), env.getMinY(), env.getMaxX(), env.getMaxY());
        toLonLat(ring);
        double minLon = Double.POSITIVE_INFINITY, maxLon = Double.NEGATIVE_INFINITY;
        double minLat = Double.POSITIVE_INFINITY, maxLat = Double.NEGATIVE_INFINITY;
        for( int i = 0; i < ring.length; i += 2 ) {
            minLon = Math.min(minLon, ring[i]);
            maxLon = Math.max(maxLon, ring[i]);
            minLat = Math.min(minLat, ring[i + 1]);
            maxLat = Math.max(maxLat, ring[i + 1]);
        }
        return Sector.fromDegrees(minLat, maxLat, minLon, maxLon);
    }

    /**
     * Show the outlines of the extent of the file and of the bounds to load. Has to be called in
     * the UI thread.
     */
    private void updateExtentLayer() {
        if (nwwPanel == null) {
            return;
        }
        extentLayer.removeAllRenderables();
        try {
            if (fileEnvelope != null) {
                extentLayer.addRenderable(outline(fileEnvelope, FILE_EXTENT_COLOR));
                Double[] bounds = constraints.checkBounds();
                // while selecting, the selector shows the bounds
                boolean selecting = _boundsSelectButton.isSelected();
                if (!selecting && bounds[0] != null && bounds[1] != null && bounds[2] != null && bounds[3] != null) {
                    extentLayer.addRenderable(outline(new Envelope(bounds[0], bounds[1], bounds[2], bounds[3]), BOUNDS_COLOR));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        redraw();
    }

    /**
     * @return the view mode selected.
     */
    private ViewMode getViewMode() {
        Object selected = viewModeCombo.getSelectedItem();
        return selected instanceof ViewMode ? (ViewMode) selected : ViewMode.FLAT;
    }

    /**
     * Check that the selected view mode is available, going back to the last one if not.
     */
    private void onViewModeChanged() {
        ViewMode mode = getViewMode();
        String problem = null;
        if (mode.hasTerrain() && fileEnvelope != null && lasCrs == null) {
            problem = "The terrain can't be used with data without CRS, shown on a local plane.";
        } else if (mode == ViewMode.DTM && dtm == null) {
            problem = "Set a DTM first, in the optional DTM field.";
        }
        if (problem != null) {
            GuiUtilities.showWarningMessage(this, problem);
            viewModeCombo.setSelectedItem(lastViewMode);
            return;
        }
        // with normalized heights, the points are drawn at their height only on flat ground
        boolean drawnElevationsChange = (mode == ViewMode.FLAT) != (lastViewMode == ViewMode.FLAT)
                && normalizeCheck.isSelected();
        lastViewMode = mode;
        if (drawnElevationsChange && pointsLayer.getPointsCount() > 0) {
            recolorInBackground();
        } else {
            applyViewMode();
        }
    }

    /**
     * Set the globe and terrain of the selected view mode. Has to be called in the UI thread.
     */
    private void applyViewMode() {
        if (nwwPanel == null) {
            return;
        }
        ViewMode mode = getViewMode();
        overTerrainCheck.setEnabled(mode.hasTerrain());
        pointsLayer.setDrawOverTerrain(mode.hasTerrain() && overTerrainCheck.isSelected());

        String terrain;
        switch( mode ) {
        case MAP_2D:
            terrain = "2D";
            if (!terrain.equals(appliedTerrain)) {
                nwwPanel.setFlatGlobe(true);
            }
            break;
        case COPERNICUS:
            terrain = "copernicus";
            if (!terrain.equals(appliedTerrain)) {
                nwwPanel.setSphereGlobe(NwwPanel.getTerrainElevationModel());
            }
            break;
        case DTM:
            terrain = "dtm";
            if (!terrain.equals(appliedTerrain)) {
                if (dtmElevationModel == null) {
                    createDtmTerrainInBackground();
                    return;
                }
                nwwPanel.setSphereGlobe(dtmElevationModel);
            }
            break;
        default:
            // a flat terrain just under the lowest point, so that the base map is under the points
            // and they are not hidden by the terrain
            double minZ = Double.isFinite(drawnMinZ) ? drawnMinZ : fileEnvelope != null ? fileEnvelope.getMinZ() : 0;
            double maxZ = Double.isFinite(drawnMaxZ) ? drawnMaxZ : fileEnvelope != null ? fileEnvelope.getMaxZ() : 0;
            if (!Double.isFinite(minZ) || !Double.isFinite(maxZ)) {
                minZ = 0;
                maxZ = 0;
            }
            double elevation = minZ - Math.max(0.5, 0.01 * (maxZ - minZ));
            terrain = "flat " + elevation;
            if (!terrain.equals(appliedTerrain)) {
                nwwPanel.setSphereGlobe(new ConstantElevationModel(elevation));
            }
            break;
        }
        appliedTerrain = terrain;

        // the 2D map is seen only from the top
        nwwPanel.setPitchLimits(Angle.ZERO, mode == ViewMode.MAP_2D ? Angle.ZERO : Angle.POS90);
        View view = nwwPanel.getWwd().getView();
        if (mode == ViewMode.MAP_2D && view instanceof OrbitView) {
            ((OrbitView) view).setPitch(Angle.ZERO);
        }
        redraw();
    }

    /**
     * Create the terrain of the DTM, with the Copernicus one around it, and show it.
     */
    private void createDtmTerrainInBackground() {
        GridCoverage2D currentDtm = dtm;
        new ExecutorIndeterminateGui(){
            @Override
            public void backGroundWork() throws Exception {
                try {
                    gov.nasa.worldwind.globes.ElevationModel model = CoverageElevationModel.withSurrounding(currentDtm,
                            NwwPanel.getTerrainElevationModel());
                    SwingUtilities.invokeLater(() -> {
                        if (currentDtm == dtm) {
                            dtmElevationModel = model;
                            applyViewMode();
                        }
                    });
                } catch (Exception e) {
                    SwingUtilities.invokeLater(() -> viewModeCombo.setSelectedItem(ViewMode.FLAT));
                    throw e;
                }
            }
        }.execute();
    }

    /**
     * Look at a rectangle in the CRS of the file. Has to be called in the UI thread.
     *
     * @param pitch the pitch of the view, in degrees, 0 to look from the top.
     */
    private void zoomTo( Envelope env, double minZ, double maxZ, double pitch ) {
        if (nwwPanel == null || env == null || env.isNull()) {
            return;
        }
        try {
            if (!Double.isFinite(minZ) || !Double.isFinite(maxZ)) {
                minZ = 0;
                maxZ = 0;
            }
            if (getViewMode() == ViewMode.MAP_2D) {
                pitch = 0;
            }
            WorldWindow wwd = nwwPanel.getWwd();
            View view = wwd.getView();
            if (!(view instanceof OrbitView)) {
                return;
            }
            Sector sector = toSector(env);
            double exaggeration = wwd.getSceneController().getVerticalExaggeration();
            Box box = Sector.computeBoundingBox(wwd.getModel().getGlobe(), exaggeration, sector, minZ, maxZ);
            Angle fov = view.getFieldOfView();
            double zoom = box.getRadius() / fov.cosHalfAngle() / fov.tanHalfAngle();
            OrbitView orbitView = (OrbitView) view;
            orbitView.stopAnimations();
            orbitView.stopMovement();
            orbitView.setCenterPosition(new Position(sector.getCentroid(), (minZ + maxZ) / 2.0 * exaggeration));
            orbitView.setZoom(zoom);
            orbitView.setHeading(Angle.ZERO);
            orbitView.setPitch(Angle.fromDegrees(pitch));
            redraw();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Zoom to the loaded points, or to the file if none is loaded.
     */
    private void zoomToData() {
        if (pointsLayer.getPointsCount() > 0) {
            zoomTo(constraints.getFilteredEnvelope(), drawnMinZ, drawnMaxZ, 45);
        } else if (fileEnvelope != null) {
            zoomTo(fileEnvelope, fileEnvelope.getMinZ(), fileEnvelope.getMaxZ(), 0);
        }
    }

    /**
     * Show the base map selected, if the file has a CRS, since without one the points are on a
     * local plane, not in their place on the map.
     */
    private void applyBasemap() {
        if (nwwPanel == null) {
            return;
        }
        Object basemap = basemapCombo.getSelectedItem();
        boolean georeferenced = fileEnvelope == null || lasCrs != null;
        satelliteLayer.setEnabled(georeferenced && BASEMAP_SATELLITE.equals(basemap));
        osmLayer.setEnabled(georeferenced && BASEMAP_OSM.equals(basemap));
        redraw();
    }

    private void redraw() {
        if (nwwPanel != null) {
            nwwPanel.getWwd().redraw();
        }
    }

    /**
     * Remove the points from the view. Has to be called in the UI thread.
     */
    private void clearPoints() {
        drawnMinZ = Double.NaN;
        drawnMaxZ = Double.NaN;
        fittedColorRange = null;
        pointsLayer.clear();
        rangeSlider.setEnabled(false);
        highField.setEnabled(false);
        lowField.setEnabled(false);
        fitColorsButton.setEnabled(false);
        autoColorsButton.setEnabled(false);
        lockToggle.setEnabled(false);
        sliceWidthField.setEnabled(false);
        highField.setText("");
        lowField.setText("");
        statusLabel.setToolTipText(null);
        redraw();
    }

    /**
     * Show points in the 3D view. Called out of the UI thread.
     *
     * @param points the points to show.
     * @param zoom if <code>true</code>, the view zooms to the points.
     */
    private void showPoints( LasPointColumns points, boolean zoom ) throws Exception {
        if (points == null || points.size() == 0) {
            SwingUtilities.invokeLater(() -> {
                clearPoints();
                statusLabel.setText("No point matches the filters");
            });
            return;
        }
        // new points: colors over their default range
        fittedColorRange = null;
        ColorMode mode = getColorMode();
        boolean normalized = isNormalized();
        pointsLayer.setLasPoints(points, this::toLonLat, mode, normalized, isDrawNormalized(normalized), null);
        updateAfterColoring(mode, zoom);
    }

    /**
     * @return <code>true</code> if the heights over the DTM are asked and were computed when the data were loaded.
     */
    private boolean isNormalized() {
        return normalizeCheck.isSelected() && constraints.hasDtm() && Double.isFinite(constraints.getStats()[8]);
    }

    /**
     * @return <code>true</code> if the points are drawn at their height over the DTM: only on flat
     *          ground, on a terrain they stay where they are.
     */
    private boolean isDrawNormalized( boolean normalized ) {
        return normalized && getViewMode() == ViewMode.FLAT;
    }

    /**
     * Color and draw again the shown points, if any, e.g. after a change of the coloring.
     */
    private void recolorInBackground() {
        if (pointsLayer.getPointsCount() == 0) {
            return;
        }
        new ExecutorIndeterminateGui(){
            @Override
            public void backGroundWork() throws Exception {
                colorPoints(false);
            }
        }.execute();
    }

    /**
     * @return the mode of coloring and slicing selected.
     */
    private ColorMode getColorMode() {
        if (_intensityRadio.isSelected()) {
            return ColorMode.INTENSITY;
        } else if (_classRadio.isSelected()) {
            return ColorMode.CLASSIFICATION;
        } else if (_impulseRadio.isSelected()) {
            return ColorMode.IMPULSE;
        } else if (_ownColorRadio.isSelected()) {
            return ColorMode.RGB;
        }
        return ColorMode.ELEVATION;
    }

    /**
     * Color the shown points with the selected mode, and set the slider to the range of their
     * values. Called out of the UI thread.
     *
     * @param zoom if <code>true</code>, the view zooms to the points.
     */
    private void colorPoints( boolean zoom ) {
        if (pointsLayer.getPointsCount() == 0) {
            return;
        }
        ColorMode mode = getColorMode();
        boolean normalized = isNormalized();
        pointsLayer.setColoring(mode, normalized, isDrawNormalized(normalized), fittedColorRange);
        updateAfterColoring(mode, zoom);
    }

    /**
     * Set slider, status and view after a coloring of the points. Called out of the UI thread.
     *
     * @param mode the coloring.
     * @param zoom if <code>true</code>, the view zooms to the points.
     */
    private void updateAfterColoring( ColorMode mode, boolean zoom ) {
        // the slider spans the range of the colors, not the extreme values
        double[] colorRange = pointsLayer.getColorRange();
        double[] drawnRange = pointsLayer.getDrawnElevationRange();
        String statsTooltip = getStatsTooltip(pointsLayer.getPointsCount());
        SwingUtilities.invokeLater(() -> {
            rangeSlider.setValueFormat(mode.format);
            rangeSlider.setEnabled(true);
            highField.setEnabled(true);
            lowField.setEnabled(true);
            fitColorsButton.setEnabled(true);
            autoColorsButton.setEnabled(true);
            lockToggle.setEnabled(true);
            sliceWidthField.setEnabled(true);
            // resets the range of the layer too, keeping the width of a locked slice
            rangeSlider.setLimits(colorRange[0], colorRange[1]);
            statusLabel.setToolTipText(statsTooltip);
            drawnMinZ = drawnRange[0];
            drawnMaxZ = drawnRange[1];
            applyViewMode();
            if (zoom) {
                zoomTo(constraints.getFilteredEnvelope(), drawnMinZ, drawnMaxZ, 45);
            }
            onRangeChanged();
        });
    }

    /**
     * Apply the range of the slider to the points.
     */
    private void onRangeChanged() {
        if (rangeSlider.isFullRange() && fittedColorRange == null) {
            // with the default colors the whole slider shows all, also the extreme values
            pointsLayer.resetValueRange();
        } else {
            // with fitted colors the whole slider shows only the fitted range
            pointsLayer.setValueRange(rangeSlider.getLowValue(), rangeSlider.getHighValue());
        }
        String format = getColorMode().format;
        if (!highField.isFocusOwner()) {
            highField.setText(String.format(format, rangeSlider.getHighValue()));
        }
        if (!lowField.isFocusOwner()) {
            lowField.setText(String.format(format, rangeSlider.getLowValue()));
        }
        if (pointsLayer.getPointsCount() > 0) {
            int visible = pointsLayer.getVisiblePointsCount();
            String status = String.format("%,d of %,d loaded points shown", visible, pointsLayer.getPointsCount());
            int moving = pointsLayer.getVisibleMovingPointsCount();
            if (moving < visible) {
                status += String.format(", %,d while moving", moving);
            }
            statusLabel.setText(status);
        }
        redraw();
    }

    /**
     * Apply the values typed in the fields of the range. With the knobs locked, the slice keeps
     * its width and moves to the value of the field edited.
     *
     * @param source the field edited.
     */
    private void applyRangeFields( Object source ) {
        if (!rangeSlider.isEnabled()) {
            return;
        }
        try {
            double low = Double.parseDouble(lowField.getText().trim().replace(',', '.'));
            double high = Double.parseDouble(highField.getText().trim().replace(',', '.'));
            if (rangeSlider.isLocked()) {
                double width = rangeSlider.getHighValue() - rangeSlider.getLowValue();
                double newLow = source == highField ? high - width : low;
                rangeSlider.setRange(newLow, newLow + width);
            } else {
                rangeSlider.setRange(low, high);
            }
        } catch (NumberFormatException e) {
            // keep the range of the slider
        }
        // show the values as clamped by the slider
        String format = getColorMode().format;
        highField.setText(String.format(format, rangeSlider.getHighValue()));
        lowField.setText(String.format(format, rangeSlider.getLowValue()));
    }

    /**
     * Lock the knobs of the slider with the width typed, or with the current one if none is valid.
     */
    private void applySliceWidth() {
        if (!rangeSlider.isEnabled()) {
            return;
        }
        double width;
        try {
            width = Double.parseDouble(sliceWidthField.getText().trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            width = Double.NaN;
        }
        if (width > 0) {
            rangeSlider.setRangeWidth(width);
        }
        rangeSlider.setLocked(true);
        // show the width as clamped by the slider
        sliceWidthField.setText(String.format(getColorMode().format, rangeSlider.getHighValue() - rangeSlider.getLowValue()));
    }

    /**
     * @return the statistics of the filtered data, as html.
     */
    private String getStatsTooltip( int pointsCount ) {
        double[] stats = constraints.getStats();
        Envelope fenv = constraints.getFilteredEnvelope();
        StringBuilder toolTip = new StringBuilder("<html>");
        int i = 0;
        toolTip.append("Filtered data stats ").append("<br>");
        toolTip.append("Points count: ").append(pointsCount).append("<br>");
        toolTip.append("Min Elevation: ").append(stats[i++]).append("<br>");
        toolTip.append("Max Elevation: ").append(stats[i++]).append("<br>");
        toolTip.append("Min Intensity: ").append(stats[i++]).append("<br>");
        toolTip.append("Max Intensity: ").append(stats[i++]).append("<br>");
        toolTip.append("Min Classification: ").append(stats[i++]).append("<br>");
        toolTip.append("Max Classification: ").append(stats[i++]).append("<br>");
        toolTip.append("Min Impulse: ").append(stats[i++]).append("<br>");
        toolTip.append("Max Impulse: ").append(stats[i++]).append("<br>");
        double minGroundHeight = stats[i++];
        if (!Double.isInfinite(minGroundHeight)) {
            toolTip.append("Min ground height: ").append(minGroundHeight).append("<br>");
            toolTip.append("Max ground height: ").append(stats[i++]).append("<br>");
        }
        toolTip.append("West: ").append(fenv.getMinX()).append("<br>");
        toolTip.append("East: ").append(fenv.getMaxX()).append("<br>");
        toolTip.append("South: ").append(fenv.getMinY()).append("<br>");
        toolTip.append("North: ").append(fenv.getMaxY()).append("<br>");
        toolTip.append("Width: ").append(fenv.getWidth()).append("<br>");
        toolTip.append("Height: ").append(fenv.getHeight()).append("<br>");
        toolTip.append("</html>");
        return toolTip.toString();
    }

    private List<String[]> getHeaderInfo( ILasHeader header ) {
        List<String[]> headerInfoList = new ArrayList<>();
        headerInfoList.add(new String[]{"File signature", new String(header.getFileSignature())});
        headerInfoList.add(new String[]{"File source ID", "" + header.getFileSourceID()});
        headerInfoList.add(new String[]{"Project ID - data 1", "" + header.getProjectID_GUIDData1()});
        headerInfoList.add(new String[]{"Project ID - data 2", "" + header.getProjectID_GUIDData2()});
        headerInfoList.add(new String[]{"Project ID - data 3", "" + header.getProjectID_GUIDData3()});
        headerInfoList.add(new String[]{"Project ID - data 4", new String(header.getProjectID_GUIDData4())});
        headerInfoList.add(new String[]{"Version", header.getVersion()});
        headerInfoList.add(new String[]{"System identifier", new String(header.getSystemIdentifier())});
        headerInfoList.add(new String[]{"Generating software", header.getGeneratingSoftware()});
        try {
            short fileCreationYear = (short) header.getFileCreationYear();
            short fileCreationDayOfYear = (short) header.getFileCreationDayOfYear();
            String dtString = " - nv - ";
            if (fileCreationYear != 0 && fileCreationDayOfYear != 0) {
                DateTime dateTime = new DateTime();
                dateTime = dateTime.withYear(fileCreationYear).withDayOfYear(fileCreationDayOfYear);
                dtString = dateTime.toString(LasUtils.dateTimeFormatterYYYYMMDD);
            }
            headerInfoList.add(new String[]{"File creation date", dtString});
        } catch (Exception e) {
            e.printStackTrace();
        }
        headerInfoList.add(new String[]{"Header size", "" + (int) header.getHeaderSize()}); //
        headerInfoList.add(new String[]{"Offset to data", "" + header.getOffset()}); //
        headerInfoList.add(new String[]{"Variable length records", "" + header.getNumberOfVariableLengthRecords()}); //
        headerInfoList.add(new String[]{"Point data format ID (0-99 for spec)", "" + header.getPointDataRecordFormat()}); //
        headerInfoList.add(new String[]{"Number of point records", "" + header.getRecordsCount()}); //
        headerInfoList.add(new String[]{"Record length", "" + header.getRecordLength()}); //
        double[] xyzScale = header.getXYZScale();
        headerInfoList.add(new String[]{"Scale", xyzScale[0] + ", " + xyzScale[1] + ", " + xyzScale[2]});
        double[] xyzOffset = header.getXYZOffset();
        headerInfoList.add(new String[]{"Offset", xyzOffset[0] + ", " + xyzOffset[1] + ", " + xyzOffset[2]});
        ReferencedEnvelope3D dataEnvelope3D = header.getDataEnvelope();
        headerInfoList.add(new String[]{"X Range", dataEnvelope3D.getMinX() + ", " + dataEnvelope3D.getMaxX()});
        headerInfoList.add(new String[]{"Y Range", dataEnvelope3D.getMinY() + ", " + dataEnvelope3D.getMaxY()});
        headerInfoList.add(new String[]{"Z Range", dataEnvelope3D.getMinZ() + ", " + dataEnvelope3D.getMaxZ()});
        return headerInfoList;
    }

    private List<String[]> getFirstPointInfo() throws IOException {
        List<String[]> firstPointInfoList = new ArrayList<>();
        if (lasReader.hasNextPoint()) {
            LasRecord lr = lasReader.getNextPoint();
            firstPointInfoList.add(new String[]{"X", "" + lr.x});
            firstPointInfoList.add(new String[]{"Y", "" + lr.y});
            firstPointInfoList.add(new String[]{"Z", "" + lr.z});
            firstPointInfoList.add(new String[]{"Intensity", "" + lr.intensity});
            firstPointInfoList.add(new String[]{"Return", "" + lr.returnNumber});
            firstPointInfoList.add(new String[]{"Number of returns", "" + lr.numberOfReturns});
            firstPointInfoList.add(new String[]{"Classification", "" + lr.classification});
            firstPointInfoList.add(new String[]{"GPS Time", "" + lr.gpsTime});
            firstPointInfoList.add(new String[]{"Color", lr.color[0] + ", " + lr.color[1] + ", " + lr.color[2]});
        }
        lasReader.rewind();
        return firstPointInfoList;
    }

    public JComponent asJComponent() {
        return this;
    }

    @Override
    public void onClose() {
        if (sectorSelector != null) {
            sectorSelector.disable();
        }
        pointsLayer.freeResources();
    }

    @Override
    public boolean canCloseWithoutPrompt() {
        return _inputPathField.getText().trim().length() == 0;
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();

        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();

        final LasInfoController controller = new LasInfoController();

        final JFrame frame = gBridge.showWindow(controller.asJComponent(), "HortonMachine Las Info Viewer - " + org.hortonmachine.Version.getVersion());

        GuiUtilities.setDefaultFrameIcon(frame);

        GuiUtilities.addClosingListener(frame, controller);

        // an optional las file to open
        if (args.length > 0 && new File(args[0]).exists()) {
            controller._inputPathField.setText(new File(args[0]).getAbsolutePath());
            new ExecutorIndeterminateGui(){
                @Override
                public void backGroundWork() throws Exception {
                    controller.loadNewFile();
                }
            }.execute();
        }
    }
    @Override
    public void keyTyped( KeyEvent e ) {
    }
    @Override
    public void keyPressed( KeyEvent e ) {
    }
    @Override
    public void keyReleased( KeyEvent e ) {
        Object source = e.getSource();
        checkConstraint(source);
        if (source == _westField || source == _eastField || source == _southField || source == _northField) {
            updateExtentLayer();
        }
    }

    private void checkConstraint( Object source ) {
        if (source == _samplingField) {
            String text = _samplingField.getText();
            Integer sampling;
            try {
                sampling = Integer.parseInt(text.trim());
                if (sampling == 0) {
                    sampling = null;
                }
            } catch (NumberFormatException e1) {
                sampling = null;
            }
            constraints.setSampling(sampling);
        } else if (source == _classesField) {
            final String[] split = _classesField.getText().trim().split(","); //$NON-NLS-1$
            int[] classes;
            try {
                if (split.length != 0) {
                    classes = new int[split.length];
                    for( int i = 0; i < split.length; i++ ) {
                        classes[i] = Integer.parseInt(split[i]);
                    }
                } else {
                    classes = null;
                }
            } catch (final Exception ex) {
                classes = null;
            }
            constraints.setClassifications(classes);
        } else if (source == _impulsesField) {
            final String[] split = _impulsesField.getText().trim().split(","); //$NON-NLS-1$
            int[] impulses;
            try {
                if (split.length != 0) {
                    impulses = new int[split.length];
                    for( int i = 0; i < split.length; i++ ) {
                        impulses[i] = Integer.parseInt(split[i]);
                    }
                } else {
                    impulses = null;
                }
            } catch (final Exception ex) {
                impulses = null;
            }
            constraints.setImpulses(impulses);
        } else if (source == _intensityRangeField) {
            final String[] split = _intensityRangeField.getText().trim().split(","); //$NON-NLS-1$
            if (split.length == 2) {
                try {
                    double minInt = Double.parseDouble(split[0].trim());
                    double maxInt = Double.parseDouble(split[1].trim());
                    constraints.setMinIntensity(minInt);
                    constraints.setMaxIntensity(maxInt);
                } catch (final Exception ex) {
                    constraints.setMinIntensity(null);
                    constraints.setMaxIntensity(null);
                }
            } else {
                constraints.setMinIntensity(null);
                constraints.setMaxIntensity(null);
            }
        } else if (source == _westField) {
            String text = _westField.getText();
            Double west;
            try {
                west = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                west = null;
            }
            constraints.setWest(west);
        } else if (source == _eastField) {
            String text = _eastField.getText();
            Double east;
            try {
                east = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                east = null;
            }
            constraints.setEast(east);
        } else if (source == _southField) {
            String text = _southField.getText();
            Double south;
            try {
                south = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                south = null;
            }
            constraints.setSouth(south);
        } else if (source == _northField) {
            String text = _northField.getText();
            Double north;
            try {
                north = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                north = null;
            }
            constraints.setNorth(north);
        } else if (source == _minZField) {
            String text = _minZField.getText();
            Double minZ;
            try {
                minZ = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                minZ = null;
            }
            constraints.setMinZ(minZ);
        } else if (source == _maxZField) {
            String text = _maxZField.getText();
            Double maxZ;
            try {
                maxZ = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                maxZ = null;
            }
            constraints.setMaxZ(maxZ);
        } else if (source == _lowerThresField) {
            String text = _lowerThresField.getText();
            Double lowerThres;
            try {
                lowerThres = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                lowerThres = null;
            }
            constraints.setLowerThres(lowerThres);
        } else if (source == _upperThresField) {
            String text = _upperThresField.getText();
            Double upperThres;
            try {
                upperThres = Double.parseDouble(text);
            } catch (NumberFormatException e1) {
                upperThres = null;
            }
            constraints.setUpperThres(upperThres);
        }
    }
}
