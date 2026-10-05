/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
package org.hortonmachine.rockfall;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.DoubleFunction;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.WindowConstants;

import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.colors.ColorInterpolator;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gui.utils.ColorRampLegend;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.monitor.ActionWithProgress;
import org.hortonmachine.gui.utils.monitor.ProgressMonitor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.StoneTrajectory;
import org.hortonmachine.nww.elevation.CoverageElevationModel;
import org.hortonmachine.nww.gui.LayersPanelController;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.ViewControlsLayer;
import org.hortonmachine.nww.layers.defaults.raster.ColoredGridSurfaceLayer;
import org.hortonmachine.nww.layers.defaults.raster.EsriSatellite;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.geom.Angle;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.geom.Vec4;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Ellipsoid;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.Offset;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.ScreenImage;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.view.orbit.OrbitView;

/**
 * An app to run the STONE rockfall model and see the results in 3D, on the terrain of the DEM.
 *
 * <p>Besides the run on all the sources of the sources map, boulders can be thrown from any cell
 * clicked on the terrain, with all their trajectories shown.</p>
 */
public class RockfallViewer extends JPanel {
    private static final long serialVersionUID = 1L;

    public static final String APPNAME = "HortonMachine Rockfall Viewer";

    private static final String TERRAIN_FLAT = "Flat at the lowest DEM elevation";
    private static final String TERRAIN_COPERNICUS = "Copernicus DEM (online)";
    /** The value of the sources map for the cells where boulders stop. */
    private static final double STOP_CELL = -1;
    /** A press and release closer than this, in pixels and milliseconds, are a click. */
    private static final int CLICK_TOLERANCE_PIXELS = 5;
    private static final long CLICK_MAX_MILLIS = 600;

    private final NwwPanel nwwPanel;
    private final LayersPanelController layersPanel;

    private final JTextField demField = new JTextField(14);
    private final JTextField sourcesField = new JTextField(14);
    private final JTextField normalRestitutionField = new JTextField(14);
    private final JTextField tangentialRestitutionField = new JTextField(14);
    private final JTextField frictionField = new JTextField(14);
    private final JTextField startVelocityField = new JTextField("1.0", 6);
    private final JTextField stopVelocityField = new JTextField("3.0", 6);
    private final JTextField stepField = new JTextField("5.0", 6);
    private final JTextField trajectoriesField = new JTextField("5000", 6);
    private final JTextField bouldersPerClickField = new JTextField("100", 6);
    private final JComboBox<String> terrainCombo = new JComboBox<>(new String[]{TERRAIN_FLAT, TERRAIN_COPERNICUS});
    private final JButton runButton = new JButton("Run");
    private final JButton saveButton = new JButton("Save results...");
    private final JButton prepareButton = new JButton("Prepare from DEM...");
    private final JToggleButton pickButton = new JToggleButton("Pick start points");
    /** The status, a text area to wrap its lines at the width of the panel. */
    private final JTextArea statusArea = new JTextArea(" ", 2, 20);

    /** The DEM shown as terrain, and the path and terrain setting it was loaded with. */
    private GridCoverage2D dem;
    private String demPath;
    private String demTerrain;
    /** The maps read, by path. */
    private final Map<String, GridCoverage2D> mapsCache = new HashMap<>();

    private OmsStone lastRun;
    /** The colors of the velocities of the last run, shared by the picked trajectories. */
    private ColorInterpolator velocityColors;
    /** True if the velocity colors come from a run, false if from a pick before any run. */
    private boolean velocityColorsFromRun;

    /*
     * The legends, overlaid at the bottom center of the map, part of the layers they describe.
     */
    private static final String COUNTER_LEGEND_TITLE = "Counter [trajectories per cell]";
    private static final String VELOCITY_LEGEND_TITLE = "Velocity [m/s]";
    private static final String HEIGHT_LEGEND_TITLE = "Max height over the ground [m]";
    /** The distance of the legends from the bottom of the map, stacked. */
    private static final int COUNTER_LEGEND_BOTTOM = 10;
    private static final int VELOCITY_LEGEND_BOTTOM = 100;
    private static final int HEIGHT_LEGEND_BOTTOM = 190;
    private static final int LEGEND_WIDTH = 340;
    private static final Color LEGEND_BACKGROUND = new Color(255, 255, 255, 200);

    /** The layers of the app, shown in this order from bottom to top. */
    private Layer demLayer;
    private List<Layer> resultLayers = new ArrayList<>();
    private Layer sourcesLayer;
    private String sourcesLayerPath;
    private Layer pickedLayer;
    private final List<Layer> shownLayers = new ArrayList<>();

    private final AtomicBoolean picking = new AtomicBoolean(false);

    /** The sphere following the terrain under the mouse while picking, in a layer hidden from the list. */
    private final RenderableLayer cursorLayer = new RenderableLayer();
    private final Ellipsoid cursorSphere = new Ellipsoid();
    /** The radius of the cursor sphere, as a fraction of its distance from the eye. */
    private static final double CURSOR_SIZE = 0.005;

    public RockfallViewer() {
        super(new BorderLayout());
        nwwPanel = (NwwPanel) NwwPanel.createNwwPanel(false);
        nwwPanel.addOsmLayer();
        nwwPanel.getWwd().getModel().getLayers().get(nwwPanel.getWwd().getModel().getLayers().size() - 1).setEnabled(false);
        nwwPanel.addLayer(new EsriSatellite());
        ViewControlsLayer viewControls = nwwPanel.addViewControls();
        viewControls.setScale(1.5);
        layersPanel = new LayersPanelController(nwwPanel);

        JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.add(createControlsPanel(), BorderLayout.NORTH);
        leftPanel.add(layersPanel, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(420, 800));

        add(leftPanel, BorderLayout.WEST);
        add(nwwPanel, BorderLayout.CENTER);

        saveButton.setEnabled(false);
        runButton.setAction(new RunAction(this));
        saveButton.addActionListener(e -> saveResults());
        prepareButton.setToolTipText("Create the missing input maps from the DEM: sources from the slope, coefficients from a lithology or default class");
        prepareButton.addActionListener(e -> prepareFromDem());
        terrainCombo.addActionListener(e -> loadDemInBackground());
        pickButton.setToolTipText("When active, a click on the terrain throws boulders from the clicked cell");

        // the sphere showing where a click would throw the boulders
        cursorLayer.setName("hide pick cursor");
        cursorLayer.setPickEnabled(false);
        ShapeAttributes cursorAttributes = new BasicShapeAttributes();
        cursorAttributes.setInteriorMaterial(Material.YELLOW);
        cursorAttributes.setInteriorOpacity(0.8);
        cursorAttributes.setDrawOutline(false);
        cursorSphere.setAttributes(cursorAttributes);
        cursorSphere.setAltitudeMode(WorldWind.ABSOLUTE);
        cursorSphere.setVisible(false);
        cursorLayer.addRenderable(cursorSphere);
        nwwPanel.addLayer(cursorLayer);
        nwwPanel.getWwd().addPositionListener(event -> updateCursor(event.getPosition()));
        pickButton.addActionListener(e -> {
            if (!pickButton.isSelected()) {
                // leaving the picking removes the cursor and the picked trajectories
                cursorSphere.setVisible(false);
                pickedLayer = null;
                showLayers();
                nwwPanel.getWwd().redraw();
            }
        });

        // NWW centers the view on a clicked point: not while picking, to keep the view when testing
        // several cells. The listeners of the NWW input handler come before the view navigation,
        // which skips consumed events.
        nwwPanel.getWwd().getInputHandler().addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent e ) {
                if (pickButton.isSelected() && SwingUtilities.isLeftMouseButton(e)) {
                    e.consume();
                }
            }
        });

        // a click, not a drag, on the terrain throws the boulders when picking. The click events of
        // Java are lost with the slightest movement of the mouse, so clicks are detected here.
        ((Component) nwwPanel.getWwd()).addMouseListener(new MouseAdapter(){
            private int pressX;
            private int pressY;
            private long pressTime;

            @Override
            public void mousePressed( MouseEvent e ) {
                pressX = e.getX();
                pressY = e.getY();
                pressTime = System.currentTimeMillis();
            }

            @Override
            public void mouseReleased( MouseEvent e ) {
                boolean isClick = Math.abs(e.getX() - pressX) <= CLICK_TOLERANCE_PIXELS
                        && Math.abs(e.getY() - pressY) <= CLICK_TOLERANCE_PIXELS
                        && System.currentTimeMillis() - pressTime <= CLICK_MAX_MILLIS;
                if (isClick && pickButton.isSelected() && SwingUtilities.isLeftMouseButton(e)) {
                    // the position on the terrain under the cursor, as picked by NWW (the positions
                    // computed by the view from screen points are on the ellipsoid, not on the terrain)
                    Position position = nwwPanel.getWwd().getCurrentPosition();
                    if (position == null) {
                        setStatus("No terrain under the click.");
                    } else {
                        pickInBackground(position);
                    }
                }
            }
        });
    }

    private JPanel createControlsPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        int row = 0;
        row = addSection(panel, row, "Input maps");
        row = addFileRow(panel, row, "DEM", demField);
        row = addFileRow(panel, row, "Sources", sourcesField);
        row = addFileRow(panel, row, "Normal restitution [%]", normalRestitutionField);
        row = addFileRow(panel, row, "Tangential restitution [%]", tangentialRestitutionField);
        row = addFileRow(panel, row, "Friction", frictionField);
        // with the maps it creates, in a row of its own: a row of buttons too wide for the panel
        // would wrap them out of sight
        panel.add(prepareButton, constraints(0, row++, 3));
        row = addSection(panel, row, "Parameters");
        row = addFieldRow(panel, row, "Start velocity [m/s]", startVelocityField);
        row = addFieldRow(panel, row, "Stop velocity [m/s]", stopVelocityField);
        row = addFieldRow(panel, row, "Step [m]", stepField);
        row = addFieldRow(panel, row, "Trajectories to save", trajectoriesField);
        row = addSection(panel, row, "View");
        row = addFieldRow(panel, row, "Terrain around the DEM", terrainCombo);

        JPanel buttons = new JPanel();
        buttons.add(runButton);
        buttons.add(saveButton);
        GridBagConstraints c = constraints(0, row++, 3);
        c.insets = new Insets(10, 0, 4, 0);
        panel.add(buttons, c);

        row = addSection(panel, row, "Boulders from a clicked cell");
        row = addFieldRow(panel, row, "Boulders per click", bouldersPerClickField);
        panel.add(pickButton, constraints(0, row++, 3));

        statusArea.setEditable(false);
        statusArea.setFocusable(false);
        statusArea.setLineWrap(true);
        statusArea.setWrapStyleWord(true);
        statusArea.setOpaque(false);
        statusArea.setBorder(null);
        statusArea.setFont(UIManager.getFont("Label.font"));
        panel.add(statusArea, constraints(0, row++, 3));
        return panel;
    }

    private static GridBagConstraints constraints( int x, int y, int width ) {
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = x;
        c.gridy = y;
        c.gridwidth = width;
        c.anchor = GridBagConstraints.WEST;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(2, 2, 2, 2);
        c.weightx = x == 1 ? 1 : 0;
        return c;
    }

    private static int addSection( JPanel panel, int row, String title ) {
        JLabel label = new JLabel("<html><b>" + title + "</b></html>");
        GridBagConstraints c = constraints(0, row, 3);
        c.insets = new Insets(10, 2, 2, 2);
        panel.add(label, c);
        return row + 1;
    }

    private static int addFieldRow( JPanel panel, int row, String label, Component field ) {
        panel.add(new JLabel(label), constraints(0, row, 1));
        panel.add(field, constraints(1, row, 2));
        return row + 1;
    }

    private int addFileRow( JPanel panel, int row, String label, JTextField field ) {
        panel.add(new JLabel(label), constraints(0, row, 1));
        panel.add(field, constraints(1, row, 1));
        JButton browse = new JButton("...");
        browse.addActionListener(e -> {
            File[] files = GuiUtilities.showOpenFilesDialog(this, "Select the " + label + " map", false,
                    PreferencesHandler.getLastFile(), null);
            if (files != null && files.length > 0 && files[0] != null) {
                field.setText(files[0].getAbsolutePath());
                if (field == demField) {
                    autoloadMaps(files[0]);
                    loadDemInBackground();
                    loadSourcesInBackground();
                } else if (field == sourcesField) {
                    loadSourcesInBackground();
                }
            }
        });
        panel.add(browse, constraints(2, row, 1));
        return row + 1;
    }

    /**
     * Fill the fields of the other maps with the files named in the standard way in the folder of
     * the DEM: e.g. sources, nrest, trest and friction, also with the prefix of the DEM name, as
     * crop_sources.tif for crop_dem.tif. Fields with a map of the same folder are kept.
     */
    private void autoloadMaps( File demFile ) {
        File folder = demFile.getParentFile();
        String demName = demFile.getName();
        int dot = demName.lastIndexOf('.');
        String extension = dot > 0 ? demName.substring(dot + 1) : "tif";
        String prefix = demPrefix(demFile);

        List<String> loaded = new ArrayList<>();
        autoloadMap(sourcesField, folder, prefix, extension, loaded, "sources", "source");
        autoloadMap(normalRestitutionField, folder, prefix, extension, loaded, "nrest", "normal_restitution", "vrest");
        autoloadMap(tangentialRestitutionField, folder, prefix, extension, loaded, "trest", "tangential_restitution", "hrest");
        autoloadMap(frictionField, folder, prefix, extension, loaded, "friction", "frict");
        if (!loaded.isEmpty()) {
            setStatus("Maps found next to the DEM: " + String.join(", ", loaded) + ".");
        }
    }

    /**
     * @return the prefix of the name of a DEM before "dem" or "dtm", e.g. "crop_" for crop_dem.tif,
     *          empty if none.
     */
    private static String demPrefix( File demFile ) {
        String demName = demFile.getName();
        int dot = demName.lastIndexOf('.');
        String baseName = dot > 0 ? demName.substring(0, dot) : demName;
        String lowerBase = baseName.toLowerCase();
        for( String demWord : new String[]{"dem", "dtm"} ) {
            if (lowerBase.endsWith(demWord)) {
                return baseName.substring(0, baseName.length() - demWord.length());
            }
        }
        return "";
    }

    /**
     * Prepare the input maps from the DEM with {@link OmsStoneInputs}, asking the options, and write
     * them next to the DEM with the standard names, so that they are also found next time.
     */
    private void prepareFromDem() {
        String demPathText = demField.getText().trim();
        if (demPathText.isEmpty() || !new File(demPathText).exists()) {
            JOptionPane.showMessageDialog(this, "Select a DEM first.", "Prepare from DEM", JOptionPane.WARNING_MESSAGE);
            return;
        }
        File demFile = new File(demPathText);

        JCheckBox createSources = new JCheckBox("Create the sources from the slope",
                sourcesField.getText().trim().isEmpty());
        JTextField slopeField = new JTextField("45", 6);
        JTextField bouldersField = new JTextField("1", 6);
        JTextField stopAreasField = new JTextField(14);
        JCheckBox createCoefficients = new JCheckBox("Create friction and restitutions", normalRestitutionField.getText()
                .trim().isEmpty() || tangentialRestitutionField.getText().trim().isEmpty() || frictionField.getText().trim().isEmpty());
        JTextField lithologyField = new JTextField(14);
        JTextField tableField = new JTextField(14);
        JComboBox<String> defaultClassCombo = new JComboBox<>(OmsStoneInputs.LITHOLOGY_NAMES_COMBO.split(","));
        defaultClassCombo.setSelectedItem(OmsStoneInputs.UNCLASSIFIED);

        JPanel panel = new JPanel(new GridBagLayout());
        int row = 0;
        panel.add(createSources, constraints(0, row++, 3));
        row = addFieldRow(panel, row, "Slope over [degrees]", slopeField);
        row = addFieldRow(panel, row, "Boulders per source", bouldersField);
        row = addFileRow(panel, row, "Stop areas (optional)", stopAreasField);
        GridBagConstraints c = constraints(0, row++, 3);
        c.insets = new Insets(12, 2, 2, 2);
        panel.add(createCoefficients, c);
        row = addFileRow(panel, row, "Lithology map (optional)", lithologyField);
        row = addFileRow(panel, row, "Lithology table CSV (optional)", tableField);
        row = addFieldRow(panel, row, "Default lithology", defaultClassCombo);
        JLabel note = new JLabel("<html>The maps are written next to the DEM, with the standard names.</html>");
        GridBagConstraints noteConstraints = constraints(0, row++, 3);
        noteConstraints.insets = new Insets(12, 2, 2, 2);
        panel.add(note, noteConstraints);

        int answer = JOptionPane.showConfirmDialog(this, panel, "Prepare from DEM", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (answer != JOptionPane.OK_OPTION || (!createSources.isSelected() && !createCoefficients.isSelected())) {
            return;
        }

        String prefix = demPrefix(demFile);
        File folder = demFile.getAbsoluteFile().getParentFile();
        File sourcesFile = new File(folder, prefix + "sources.tif");
        File normalFile = new File(folder, prefix + "nrest.tif");
        File tangentialFile = new File(folder, prefix + "trest.tif");
        File frictionFile = new File(folder, prefix + "friction.tif");
        List<File> toWrite = new ArrayList<>();
        if (createSources.isSelected()) {
            toWrite.add(sourcesFile);
        }
        if (createCoefficients.isSelected()) {
            toWrite.add(normalFile);
            toWrite.add(tangentialFile);
            toWrite.add(frictionFile);
        }
        List<String> existing = new ArrayList<>();
        for( File file : toWrite ) {
            if (file.exists()) {
                existing.add(file.getName());
            }
        }
        if (!existing.isEmpty()) {
            int overwrite = JOptionPane.showConfirmDialog(this,
                    "These files exist already and will be overwritten:\n" + String.join("\n", existing), "Prepare from DEM",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (overwrite != JOptionPane.OK_OPTION) {
                return;
            }
        }

        new Thread(() -> {
            try {
                setStatus("Preparing the maps from the DEM...");
                OmsStoneInputs inputs = new OmsStoneInputs();
                inputs.inElev = readMap(demFile.getAbsolutePath());
                inputs.pSourceSlope = parse(slopeField, "slope");
                inputs.pBouldersPerSource = (int) parse(bouldersField, "number of boulders per source");
                String stopAreas = stopAreasField.getText().trim();
                inputs.inStopAreas = stopAreas.isEmpty() ? null : OmsRasterReader.readRaster(stopAreas);
                String lithology = lithologyField.getText().trim();
                inputs.inLithology = lithology.isEmpty() ? null : OmsRasterReader.readRaster(lithology);
                String table = tableField.getText().trim();
                inputs.inLithologyTable = table.isEmpty() ? null : table;
                inputs.pDefaultLithology = (String) defaultClassCombo.getSelectedItem();
                inputs.pm = new DummyProgressMonitor();
                inputs.process();

                if (createSources.isSelected()) {
                    writeMap(sourcesFile, inputs.outSources);
                }
                if (createCoefficients.isSelected()) {
                    writeMap(normalFile, inputs.outNormalRestitution);
                    writeMap(tangentialFile, inputs.outTangentialRestitution);
                    writeMap(frictionFile, inputs.outFriction);
                }
                SwingUtilities.invokeLater(() -> {
                    if (createSources.isSelected()) {
                        sourcesField.setText(sourcesFile.getAbsolutePath());
                        sourcesLayerPath = null;
                        loadSourcesInBackground();
                    }
                    if (createCoefficients.isSelected()) {
                        normalRestitutionField.setText(normalFile.getAbsolutePath());
                        tangentialRestitutionField.setText(tangentialFile.getAbsolutePath());
                        frictionField.setText(frictionFile.getAbsolutePath());
                    }
                    List<String> names = new ArrayList<>();
                    for( File file : toWrite ) {
                        names.add(file.getName());
                    }
                    showStatus("Prepared from the DEM: " + String.join(", ", names) + ".");
                });
            } catch (Exception e) {
                e.printStackTrace();
                setStatus("Unable to prepare the maps: " + e.getMessage());
            }
        }, "Rockfall viewer prepare").start();
    }

    /**
     * Write a map, forgetting its cached version.
     */
    private synchronized void writeMap( File file, GridCoverage2D map ) throws Exception {
        mapsCache.remove(file.getAbsolutePath());
        OmsRasterWriter.writeRaster(file.getAbsolutePath(), map);
    }

    private static void autoloadMap( JTextField field, File folder, String prefix, String extension, List<String> loaded,
            String... names ) {
        String current = field.getText().trim();
        if (!current.isEmpty() && folder.equals(new File(current).getAbsoluteFile().getParentFile())) {
            return;
        }
        File[] files = folder.listFiles();
        if (files == null) {
            return;
        }
        List<String> prefixes = prefix.isEmpty() ? List.of("") : List.of(prefix, "");
        for( String candidatePrefix : prefixes ) {
            for( String name : names ) {
                for( String candidateExtension : new String[]{extension, "tif", "tiff", "asc"} ) {
                    String wanted = candidatePrefix + name + "." + candidateExtension;
                    for( File file : files ) {
                        if (file.isFile() && file.getName().equalsIgnoreCase(wanted)) {
                            field.setText(file.getAbsolutePath());
                            loaded.add(file.getName());
                            return;
                        }
                    }
                }
            }
        }
    }

    /**
     * Read a map, once.
     */
    private synchronized GridCoverage2D readMap( String path ) throws Exception {
        GridCoverage2D map = mapsCache.get(path);
        if (map == null) {
            map = OmsRasterReader.readRaster(path);
            mapsCache.put(path, map);
        }
        return map;
    }

    /**
     * Load the DEM and show it as terrain, if it changed.
     */
    private void loadDemInBackground() {
        String path = demField.getText().trim();
        if (path.isEmpty() || !new File(path).exists()) {
            return;
        }
        new Thread(() -> {
            try {
                setStatus("Loading the terrain...");
                ensureTerrain(path);
                setStatus("Terrain loaded.");
            } catch (Exception e) {
                e.printStackTrace();
                setStatus("Unable to load the DEM: " + e.getMessage());
            }
        }, "Rockfall viewer terrain").start();
    }

    /**
     * Read the DEM and set it as terrain, if not already done with the same settings.
     */
    private synchronized void ensureTerrain( String path ) throws Exception {
        String terrain = (String) terrainCombo.getSelectedItem();
        boolean newDem = !path.equals(demPath);
        if (!newDem && terrain.equals(demTerrain)) {
            return;
        }
        GridCoverage2D readDem = readMap(path);
        ElevationModel surrounding = TERRAIN_COPERNICUS.equals(terrain) ? NwwPanel.getTerrainElevationModel() : null;
        ElevationModel elevationModel = CoverageElevationModel.withSurrounding(readDem, surrounding);
        if (newDem) {
            double[] minMax = minMax(readDem);
            ColorInterpolator elevationColors = new ColorInterpolator(EColorTables.elev.name(), minMax[0], minMax[1], 200);
            ColoredGridSurfaceLayer newDemLayer = new ColoredGridSurfaceLayer("DEM", readDem, elevationColors::getColorFor, 1);
            newDemLayer.setEnabled(false);
            demLayer = newDemLayer;
        }
        dem = readDem;
        demPath = path;
        demTerrain = terrain;
        SwingUtilities.invokeLater(() -> {
            nwwPanel.setSphereGlobe(elevationModel);
            if (newDem) {
                showLayers();
                zoomToDem();
            }
        });
    }

    /**
     * Load the sources and show them, if they changed.
     */
    private void loadSourcesInBackground() {
        String path = sourcesField.getText().trim();
        if (path.isEmpty() || !new File(path).exists()) {
            return;
        }
        new Thread(() -> {
            try {
                setStatus("Loading the sources...");
                ensureSourcesLayer(path);
                setStatus("Sources loaded.");
            } catch (Exception e) {
                e.printStackTrace();
                setStatus("Unable to load the sources: " + e.getMessage());
            }
        }, "Rockfall viewer sources").start();
    }

    /**
     * Create the layer of the sources, if they changed: source cells red, stop cells blue.
     */
    private synchronized void ensureSourcesLayer( String path ) throws Exception {
        if (path.equals(sourcesLayerPath)) {
            return;
        }
        Color sourceColor = new Color(220, 0, 0, 230);
        Color stopColor = new Color(0, 90, 255, 230);
        sourcesLayer = new ColoredGridSurfaceLayer("Sources", readMap(path),
                v -> v > 0 ? sourceColor : v == STOP_CELL ? stopColor : null, 2);
        sourcesLayerPath = path;
        SwingUtilities.invokeLater(this::showLayers);
    }

    /**
     * Show the layers of the app in their order, from bottom to top: DEM colors, results, sources,
     * trajectories, picked trajectories. Has to be called in the UI thread.
     */
    private void showLayers() {
        for( Layer layer : shownLayers ) {
            nwwPanel.removeLayer(layer);
        }
        shownLayers.clear();
        List<Layer> layers = new ArrayList<>();
        layers.add(demLayer);
        layers.addAll(resultLayers);
        layers.add(sourcesLayer);
        layers.add(pickedLayer);
        // the cursor stays on top
        nwwPanel.removeLayer(cursorLayer);
        layers.add(cursorLayer);
        for( Layer layer : layers ) {
            if (layer != null) {
                nwwPanel.addLayer(layer);
                shownLayers.add(layer);
            }
        }
        layersPanel.refreshLayersList();
    }

    private void zoomToDem() {
        try {
            ReferencedEnvelope extent = new ReferencedEnvelope(dem.getEnvelope2D())
                    .transform(HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true), true);
            nwwPanel.goTo(Sector.fromDegrees(extent.getMinY(), extent.getMaxY(), extent.getMinX(), extent.getMaxX()), false);
            if (nwwPanel.getWwd().getView() instanceof OrbitView) {
                ((OrbitView) nwwPanel.getWwd().getView()).setPitch(Angle.fromDegrees(55));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setStatus( String status ) {
        SwingUtilities.invokeLater(() -> showStatus(status));
    }

    /**
     * Show a status, wrapped in the width of the panel. Has to be called in the UI thread.
     */
    private void showStatus( String status ) {
        statusArea.setText(status);
    }

    private static double parse( JTextField field, String name ) {
        try {
            return Double.parseDouble(field.getText().trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("The " + name + " is not a number: " + field.getText());
        }
    }

    private static String checkFile( JTextField field, String name ) {
        String path = field.getText().trim();
        if (path.isEmpty() || !new File(path).exists()) {
            throw new IllegalArgumentException("The " + name + " map is missing.");
        }
        return path;
    }

    /**
     * Create the module with the maps of the coefficients and the parameters of the panel.
     */
    private OmsStone createStone() throws Exception {
        OmsStone stone = new OmsStone();
        stone.inElev = dem;
        stone.inNormalRestitution = readMap(checkFile(normalRestitutionField, "normal restitution"));
        stone.inTangentialRestitution = readMap(checkFile(tangentialRestitutionField, "tangential restitution"));
        stone.inFriction = readMap(checkFile(frictionField, "friction"));
        stone.pStartVelocity = parse(startVelocityField, "start velocity");
        stone.pStopVelocity = parse(stopVelocityField, "stop velocity");
        stone.pStep = parse(stepField, "step");
        return stone;
    }

    /**
     * The run of the model on all the sources and the creation of its layers, with a progress dialog.
     */
    private class RunAction extends ActionWithProgress {
        private static final long serialVersionUID = 1L;
        private List<Layer> newResultLayers;

        RunAction( Component parent ) {
            super(parent, "Running the rockfall simulation...", 100, false);
            putValue(NAME, "Run");
        }

        @Override
        public void onError( Exception e ) {
            super.onError(e);
            SwingUtilities.invokeLater(() -> saveButton.setEnabled(lastRun != null));
        }

        @Override
        public void backGroundWork( ProgressMonitor monitor ) throws Exception {
            String demFile = checkFile(demField, "DEM");
            String sourcesFile = checkFile(sourcesField, "sources");
            int maxTrajectories = (int) parse(trajectoriesField, "number of trajectories");

            SwingUtilities.invokeLater(() -> saveButton.setEnabled(false));
            monitor.message("Reading the maps...");
            ensureTerrain(demFile);
            ensureSourcesLayer(sourcesFile);
            OmsStone stone = createStone();
            stone.inSources = readMap(sourcesFile);
            stone.pMaxTrajectories = maxTrajectories;
            stone.pm = monitor;
            stone.process();
            lastRun = stone;

            // the layers are built here, out of the UI thread
            double maxCount = percentile(stone.outCounter, 1.0);
            double maxVelocity = percentile(stone.outMaxVelocity, 0.99);
            double maxHeight = percentile(stone.outMaxDz, 0.99);
            ColorInterpolator countColors = new ColorInterpolator(EColorTables.extrainbow.name(), 0,
                    Math.log10(Math.max(2, maxCount)), 220);
            velocityColors = new ColorInterpolator(EColorTables.rainbow.name(), 0, Math.max(1, maxVelocity), 220);
            velocityColorsFromRun = true;
            ColorInterpolator heightColors = new ColorInterpolator(EColorTables.reds.name(), 0, Math.max(1, maxHeight),
                    220);

            newResultLayers = new ArrayList<>();
            ColoredGridSurfaceLayer counter = new ColoredGridSurfaceLayer("Counter", stone.outCounter,
                    v -> v >= 1 ? countColors.getColorFor(Math.log10(v)) : null, 2);
            ColorInterpolator _velocityColors = velocityColors;
            ColoredGridSurfaceLayer velocity = new ColoredGridSurfaceLayer("Max velocity", stone.outMaxVelocity,
                    _velocityColors::getColorFor, 2);
            velocity.setEnabled(false);
            ColoredGridSurfaceLayer height = new ColoredGridSurfaceLayer("Max height", stone.outMaxDz,
                    v -> v > 0 ? heightColors.getColorFor(v) : null, 2);
            height.setEnabled(false);
            // the legends are part of their layers, so they are shown with them
            counter.addRenderable(legendOverlay(COUNTER_LEGEND_TITLE, v -> countColors.getColorFor(Math.log10(v)), 1,
                    Math.max(2, maxCount), true, false, COUNTER_LEGEND_BOTTOM));
            velocity.addRenderable(legendOverlay(VELOCITY_LEGEND_TITLE, _velocityColors::getColorFor, 0,
                    _velocityColors.getMax(), false, true, VELOCITY_LEGEND_BOTTOM));
            height.addRenderable(legendOverlay(HEIGHT_LEGEND_TITLE, heightColors::getColorFor, 0, heightColors.getMax(),
                    false, true, HEIGHT_LEGEND_BOTTOM));
            newResultLayers.add(counter);
            newResultLayers.add(velocity);
            newResultLayers.add(height);
        }

        @Override
        public void postWork() throws Exception {
            SwingUtilities.invokeLater(() -> {
                resultLayers = newResultLayers;
                showLayers();
                saveButton.setEnabled(true);
                // the trajectories of the run are only saved, the ones of clicked cells are shown
                showStatus("Done. " + lastRun.getTrajectories().size() + " trajectories are kept for saving.");
            });
        }
    }

    /**
     * Move the cursor sphere to the terrain position under the mouse, when picking.
     */
    private void updateCursor( Position position ) {
        if (!pickButton.isSelected() || position == null) {
            if (cursorSphere.isVisible()) {
                cursorSphere.setVisible(false);
                nwwPanel.getWwd().redraw();
            }
            return;
        }
        WorldWindow wwd = nwwPanel.getWwd();
        Vec4 point = wwd.getModel().getGlobe().computePointFromPosition(position);
        double distance = wwd.getView().getEyePoint().distanceTo3(point);
        double radius = Math.max(0.5, distance * CURSOR_SIZE);
        cursorSphere.setCenterPosition(position);
        cursorSphere.setNorthSouthRadius(radius);
        cursorSphere.setEastWestRadius(radius);
        cursorSphere.setVerticalRadius(radius);
        cursorSphere.setVisible(true);
        wwd.redraw();
    }

    /**
     * Throw boulders from the cell of a clicked position, in the background.
     */
    private void pickInBackground( Position position ) {
        if (!picking.compareAndSet(false, true)) {
            setStatus("Still simulating the last click...");
            return;
        }
        new Thread(() -> {
            try {
                pick(position);
            } catch (Exception e) {
                e.printStackTrace();
                setStatus("Unable to throw the boulders: " + e.getMessage());
            } finally {
                picking.set(false);
            }
        }, "Rockfall viewer pick").start();
    }

    private void pick( Position position ) throws Exception {
        ensureTerrain(checkFile(demField, "DEM"));
        int boulders = (int) parse(bouldersPerClickField, "number of boulders per click");
        if (boulders < 1) {
            throw new IllegalArgumentException("The number of boulders per click has to be at least 1.");
        }

        // the cell of the DEM under the click
        MathTransform toDem = CRS.findMathTransform(HMCrsRegistry.INSTANCE.getCrs("EPSG:4326", true),
                dem.getCoordinateReferenceSystem(), true);
        double[] xy = {position.getLongitude().degrees, position.getLatitude().degrees};
        toDem.transform(xy, 0, xy, 0, 1);
        RegionMap region = CoverageUtilities.getRegionParamsFromGridCoverage(dem);
        int col = (int) Math.floor((xy[0] - region.getWest()) / region.getXres());
        int row = (int) Math.floor((region.getNorth() - xy[1]) / region.getYres());
        int rows = region.getRows();
        int cols = region.getCols();
        if (col < 0 || row < 0 || col >= cols || row >= rows) {
            setStatus("The clicked point is outside of the DEM.");
            return;
        }
        try (HMRaster demRaster = HMRaster.fromGridCoverage(dem)) {
            if (demRaster.isNovalue(demRaster.getValue(col, row))) {
                setStatus("The clicked cell has no elevation.");
                return;
            }
        }
        setStatus("Throwing " + boulders + " boulders from row " + row + ", col " + col + "...");

        double[][] sources = new double[rows][cols];
        for( double[] sourcesRow : sources ) {
            java.util.Arrays.fill(sourcesRow, Double.NaN);
        }
        sources[row][col] = boulders;
        OmsStone stone = createStone();
        stone.inSources = CoverageUtilities.buildCoverageWithNovalue("sources", sources, region,
                dem.getCoordinateReferenceSystem(), true, Double.NaN);
        stone.pMaxTrajectories = boulders;
        stone.pm = new DummyProgressMonitor();
        long start = System.currentTimeMillis();
        stone.process();
        long millis = System.currentTimeMillis() - start;

        List<StoneTrajectory> trajectories = stone.getTrajectories();
        ColorInterpolator colors = velocityColors;
        boolean openEnded = velocityColorsFromRun;
        if (colors == null) {
            // no run yet: a scale of the picked boulders
            double maxSpeed = 1;
            for( StoneTrajectory trajectory : trajectories ) {
                maxSpeed = Math.max(maxSpeed, trajectory.getMaxSpeed());
            }
            colors = new ColorInterpolator(EColorTables.rainbow.name(), 0, maxSpeed, 220);
            openEnded = false;
        }
        ColorInterpolator _colors = colors;
        TrajectoriesLayer layer = new TrajectoriesLayer("Picked trajectories", trajectories,
                dem.getCoordinateReferenceSystem(), _colors::getColorFor);
        layer.addRenderable(legendOverlay(VELOCITY_LEGEND_TITLE, _colors::getColorFor, 0, _colors.getMax(), false, openEnded,
                VELOCITY_LEGEND_BOTTOM));
        // the boulders start from the center of the cell, not from the clicked point
        double[] cellCenter = {region.getWest() + (col + 0.5) * region.getXres(),
                region.getNorth() - (row + 0.5) * region.getYres()};
        toDem.inverse().transform(cellCenter, 0, cellCenter, 0, 1);
        PointPlacemark start_point = new PointPlacemark(Position.fromDegrees(cellCenter[1], cellCenter[0], 0));
        start_point.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        PointPlacemarkAttributes attributes = new PointPlacemarkAttributes();
        attributes.setUsePointAsDefaultImage(true);
        attributes.setLineMaterial(Material.WHITE);
        attributes.setScale(10d);
        start_point.setAttributes(attributes);
        layer.addRenderable(start_point);

        double maxLength = 0;
        for( StoneTrajectory trajectory : trajectories ) {
            double length = 0;
            for( int i = 1; i < trajectory.size(); i++ ) {
                length += Math.hypot(trajectory.getX(i) - trajectory.getX(i - 1), trajectory.getY(i) - trajectory.getY(i - 1));
            }
            maxLength = Math.max(maxLength, length);
        }
        String status = String.format("%d boulders from row %d, col %d in %d ms, longest runout %.0f m.", trajectories.size(),
                row, col, millis, maxLength);
        SwingUtilities.invokeLater(() -> {
            // picking could have been disabled meanwhile
            if (pickButton.isSelected()) {
                pickedLayer = layer;
                showLayers();
            }
            showStatus(status);
        });
    }

    /**
     * Create a legend overlaid at the bottom center of the map, to add to the layer it describes.
     *
     * @param bottom the distance from the bottom of the map, in pixels.
     */
    private static ScreenImage legendOverlay( String title, DoubleFunction<Color> colors, double min, double max,
            boolean logarithmic, boolean openEnded, int bottom ) {
        ColorRampLegend legend = new ColorRampLegend();
        legend.setRamp(title, colors, min, max, logarithmic, openEnded);
        ScreenImage overlay = new ScreenImage();
        overlay.setImageSource(legend.toImage(LEGEND_WIDTH, LEGEND_BACKGROUND));
        overlay.setScreenOffset(new Offset(0.5, (double) bottom, AVKey.FRACTION, AVKey.PIXELS));
        overlay.setImageOffset(new Offset(0.5, 0.0, AVKey.FRACTION, AVKey.FRACTION));
        return overlay;
    }

    /**
     * @return the minimum and maximum of the valid values of a coverage.
     */
    private static double[] minMax( GridCoverage2D coverage ) throws Exception {
        double min = Double.POSITIVE_INFINITY;
        double max = Double.NEGATIVE_INFINITY;
        try (HMRaster raster = HMRaster.fromGridCoverage(coverage)) {
            for( int r = 0; r < raster.getRows(); r++ ) {
                for( int c = 0; c < raster.getCols(); c++ ) {
                    double value = raster.getValue(c, r);
                    if (!raster.isNovalue(value)) {
                        min = Math.min(min, value);
                        max = Math.max(max, value);
                    }
                }
            }
        }
        return min <= max ? new double[]{min, max} : new double[]{0, 1};
    }

    /**
     * @return the value at the given percentile of the valid values of a coverage.
     */
    private static double percentile( GridCoverage2D coverage, double percentile ) throws Exception {
        List<Double> values = new ArrayList<>();
        try (HMRaster raster = HMRaster.fromGridCoverage(coverage)) {
            for( int r = 0; r < raster.getRows(); r++ ) {
                for( int c = 0; c < raster.getCols(); c++ ) {
                    double value = raster.getValue(c, r);
                    if (!raster.isNovalue(value)) {
                        values.add(value);
                    }
                }
            }
        }
        if (values.isEmpty()) {
            return 1;
        }
        values.sort(null);
        return values.get((int) Math.round(percentile * (values.size() - 1)));
    }

    private void saveResults() {
        if (lastRun == null) {
            return;
        }
        File[] folders = GuiUtilities.showOpenFolderDialog(this, "Select the folder for the results", false,
                PreferencesHandler.getLastFile());
        if (folders == null || folders.length == 0 || folders[0] == null) {
            return;
        }
        File folder = folders[0];
        new Thread(() -> {
            try {
                setStatus("Saving the results...");
                OmsRasterWriter.writeRaster(new File(folder, "rockfall_counter.tif").getAbsolutePath(), lastRun.outCounter);
                OmsRasterWriter.writeRaster(new File(folder, "rockfall_maxvel.tif").getAbsolutePath(), lastRun.outMaxVelocity);
                OmsRasterWriter.writeRaster(new File(folder, "rockfall_maxdz.tif").getAbsolutePath(), lastRun.outMaxDz);
                if (lastRun.outTrajectories != null) {
                    OmsVectorWriter.writeVector(new File(folder, "rockfall_trajectories.gpkg").getAbsolutePath() + "#trajectories",
                            lastRun.outTrajectories);
                }
                setStatus("Results saved in " + folder.getAbsolutePath());
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Unable to save the results: " + e.getMessage(),
                        "ERROR", JOptionPane.ERROR_MESSAGE));
            }
        }, "Rockfall viewer save").start();
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();
        SwingUtilities.invokeLater(() -> {
            RockfallViewer viewer = new RockfallViewer();
            JFrame frame = new JFrame(APPNAME + " - " + org.hortonmachine.Version.getVersion());
            GuiUtilities.setDefaultFrameIcon(frame);
            frame.getContentPane().add(viewer);
            frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
            frame.setPreferredSize(new Dimension(1400, 900));
            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
            if (args.length >= 5) {
                viewer.demField.setText(args[0]);
                viewer.sourcesField.setText(args[1]);
                viewer.normalRestitutionField.setText(args[2]);
                viewer.tangentialRestitutionField.setText(args[3]);
                viewer.frictionField.setText(args[4]);
                viewer.loadDemInBackground();
                viewer.loadSourcesInBackground();
            }
        });
    }
}
