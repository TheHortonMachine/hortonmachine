/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.SLD;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.TileEntry;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.rasterlite.Rasterlite2Coverage;
import org.hortonmachine.dbs.rasterlite.Rasterlite2Db;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.dbs.utils.TableName;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.Las;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.spatialite.GTSpatialiteThreadsafeDb;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.executor.ExecutorProgressGui;
import org.hortonmachine.nww.elevation.CoverageElevationModel;
import org.hortonmachine.nww.gui.listeners.GenericSelectListener;
import org.hortonmachine.nww.layers.defaults.annotations.HtmlScreenAnnotation;
import org.hortonmachine.nww.layers.defaults.annotations.HtmlScreenAnnotation.Builder;
import org.hortonmachine.nww.layers.defaults.other.CurrentGpsPointLayer;
import org.hortonmachine.nww.layers.defaults.other.LasPointCloudLayer;
import org.hortonmachine.nww.layers.defaults.other.LasPointCloudLayer.ColorMode;
import org.hortonmachine.nww.layers.defaults.other.SimplePointsLayer;
import org.hortonmachine.nww.layers.defaults.other.WhiteNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.GeopackageTilesNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.ImageMosaicNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.MBTilesNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.MapsforgeNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.RL2NwwLayer;
import org.hortonmachine.nww.layers.defaults.spatialite.RasterizedSpatialiteLasLayer;
import org.hortonmachine.nww.layers.defaults.spatialite.RasterizedSpatialiteLayer;
import org.hortonmachine.nww.layers.defaults.spatialite.SpatialiteLinesLayer;
import org.hortonmachine.nww.layers.defaults.spatialite.SpatialitePointsLayer;
import org.hortonmachine.nww.layers.defaults.spatialite.SpatialitePolygonLayer;
import org.hortonmachine.nww.layers.defaults.vector.FeatureCollectionLinesLayer;
import org.hortonmachine.nww.layers.defaults.vector.FeatureCollectionPointsLayer;
import org.hortonmachine.nww.layers.defaults.vector.FeatureCollectionPolygonLayer;
import org.hortonmachine.nww.layers.defaults.vector.RasterizedFeatureCollectionLayer;
import org.hortonmachine.nww.layers.defaults.vector.RasterizedShapefilesFolderNwwLayer;
import org.hortonmachine.nww.layers.defaults.vector.ShapefilesFolderLayer;
import org.hortonmachine.nww.utils.CursorUtils;
import org.hortonmachine.nww.utils.EGlobeModes;
import org.hortonmachine.nww.utils.FakeGps;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.nww.utils.cache.CacheUtils;
import org.hortonmachine.nww.utils.selection.ObjectsOnScreenByBoxSelector;
import org.hortonmachine.style.SimpleStyle;
import org.hortonmachine.style.SimpleStyleUtilities;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;

/**
 * The tools panel.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ToolsPanelController extends ToolsPanelView {

    private static final long serialVersionUID = 1L;
    private static final String PREF_GLOBE_MODE = "NWW_GLOBE_MODE";

    /**
     * Colors given in turn to the vector layers without a style: distinct hues that stay visible
     * on both the street maps and the satellite imagery.
     */
    private static final Color[] DEFAULT_LAYER_COLORS = {//
            new Color(0x1f77b4), // blue
            new Color(0xff7f0e), // orange
            new Color(0x2ca02c), // green
            new Color(0xd62728), // red
            new Color(0x9467bd), // purple
            new Color(0x17becf), // cyan
            new Color(0xe377c2), // pink
            new Color(0xbcbd22), // olive
            new Color(0x8c564b) // brown
    };
    private int nextDefaultColorIndex = 0;

    private GenericSelectListener genericSelectListener;

    private WhiteNwwLayer whiteLayer = new WhiteNwwLayer();

    /** The points of a las file loaded by default, the sampling proposed keeps under them. */
    private static final long LAS_DEFAULT_MAX_POINTS = 20_000_000;

    private NwwPanel wwjPanel;

    private LayerEventsListener layerEventsListener;

    private static final String NO_TERRAIN_DEM = "Terrain from a DEM";
    private JLabel _terrainDemLabel = new JLabel(NO_TERRAIN_DEM);
    private JButton _terrainDemButton = new JButton("...");
    private JButton _terrainDemClearButton = new JButton("x");

    @SuppressWarnings("unchecked")
    public ToolsPanelController( final NwwPanel wwjPanel, LayerEventsListener layerEventsListener ) {
        this.wwjPanel = wwjPanel;
        this.layerEventsListener = layerEventsListener;
        compactLayout();

        String[] supportedExtensions = NwwUtilities.SUPPORTED_EXTENSIONS;
        StringBuilder sb = new StringBuilder();
        for( String ext : supportedExtensions ) {
            sb.append(",*.").append(ext);
        }
        final String desc = sb.substring(1);

        _loadGpsLabel.setText("Load vector to fly along");
        _loadGpsButton.setToolTipText("Fly along the geometries of a shapefile or geopackage table");
        _loadGpsButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("Vector files (*.shp, *.gpkg)", "shp", "gpkg"));
            fileChooser.setCurrentDirectory(PreferencesHandler.getLastFile());
            int result = fileChooser.showOpenDialog((Component) wwjPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                PreferencesHandler.setLastPath(selectedFile.getAbsolutePath());
                flyAlongVector(selectedFile);
            }
        });

        _loadFileButton.addActionListener(e -> {

            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
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
            int result = fileChooser.showOpenDialog((Component) wwjPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File[] selectedFiles = fileChooser.getSelectedFiles();
                if (selectedFiles != null) {
                    loadFiles(Arrays.asList(selectedFiles));
                }
            }
        });

        if (layerEventsListener instanceof LayersPanelController) {
            ((LayersPanelController) layerEventsListener).setFileDropHandler(this::loadFiles);
        }

        _pasteWkt.addActionListener(e -> {
            try {
                RenderableLayer layer = new RenderableLayer(){
                    @Override
                    public String toString() {
                        return "WKT PASTE " + DateTime.now().toString(HMConstants.dateTimeFormatterYYYYMMDDHHMMSS);
                    }
                };
                String fromClipboard = GuiUtilities.getFromClipboard();
                WKTReader r = new WKTReader();
                Geometry geometry = null;
                try {
                    geometry = r.read(fromClipboard);
                    NwwUtilities.addGeometries(layer, geometry);
                } catch (Exception e1) {
                    GuiUtilities.showWarningMessage(wwjPanel, "WARNING",
                            "The clipoboard content doesn't seem to be a WKT Geometry.");
                    return;
                }

                wwjPanel.getWwd().getModel().getLayers().add(layer);
                layerEventsListener.onLayerAdded(layer);
            } catch (Exception e1) {
                e1.printStackTrace();
            }
        });

        String[] names = EGlobeModes.getModesDescriptions();
        _globeModeCombo.setModel(new DefaultComboBoxModel<String>(names));
        _globeModeCombo.setSelectedItem(names[0]);
        _globeModeCombo.addActionListener(e -> {
            String selected = _globeModeCombo.getSelectedItem().toString();
            EGlobeModes modeFromDescription = EGlobeModes.getModeFromDescription(selected);
            PreferencesHandler.setPreference(PREF_GLOBE_MODE, modeFromDescription.name());
            switch( modeFromDescription ) {
            case FlatEarth:
                wwjPanel.setFlatGlobe(false);
                break;
            case FlatEarthMercator:
                wwjPanel.setFlatGlobe(true);
                break;
            case EarthTerrain:
                wwjPanel.setSphereGlobe(true);
                break;
            case Earth:
            default:
                wwjPanel.setSphereGlobe(false);
                break;
            }
        });
        // restore the mode of the last session
        String lastMode = PreferencesHandler.getPreference(PREF_GLOBE_MODE, EGlobeModes.Earth.name());
        for( EGlobeModes mode : EGlobeModes.values() ) {
            if (mode.name().equals(lastMode) && mode != EGlobeModes.Earth) {
                _globeModeCombo.setSelectedItem(mode.getDescription());
            }
        }

        _terrainDemButton.setToolTipText("Use a DEM as the 3D terrain of its area, instead of the Copernicus one");
        _terrainDemButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setFileFilter(new FileNameExtensionFilter("*.tif,*.tiff,*.asc", "tif", "tiff", "asc"));
            fileChooser.setCurrentDirectory(PreferencesHandler.getLastFile());
            if (fileChooser.showOpenDialog((Component) wwjPanel) == JFileChooser.APPROVE_OPTION) {
                File demFile = fileChooser.getSelectedFile();
                PreferencesHandler.setLastPath(demFile.getAbsolutePath());
                loadTerrainDem(demFile);
            }
        });
        _terrainDemClearButton.setToolTipText("Use the Copernicus terrain again");
        _terrainDemClearButton.setEnabled(false);
        _terrainDemClearButton.addActionListener(e -> {
            wwjPanel.setLocalTerrain(null);
            _terrainDemLabel.setText(NO_TERRAIN_DEM);
            _terrainDemClearButton.setEnabled(false);
            if (EGlobeModes.getModeFromDescription(_globeModeCombo.getSelectedItem().toString()) == EGlobeModes.EarthTerrain) {
                wwjPanel.setSphereGlobe(true);
            }
        });

        _infoEditingButton.addActionListener(e -> {
            if (_infoEditingButton.isSelected()) {
                genericSelectListener = new GenericSelectListener(wwjPanel);
                wwjPanel.getWwd().addSelectListener(genericSelectListener);

                CursorUtils.makeHand(wwjPanel.getWwd());
            } else {
                if (genericSelectListener != null) {
                    wwjPanel.getWwd().removeSelectListener(genericSelectListener);
                }
                genericSelectListener = null;
                CursorUtils.makeDefault(wwjPanel.getWwd());
            }
        });

        final ObjectsOnScreenByBoxSelector selectByBoxSelector = new ObjectsOnScreenByBoxSelector(wwjPanel.getWwd());
        selectByBoxSelector.addListener(new ObjectsOnScreenByBoxSelector.IBoxScreenSelectionListener(){

            @Override
            public void onSelectionFinished( Geometry selectedArea, List< ? > selectedObjs ) {
                int size = selectedObjs.size();
                JOptionPane.showMessageDialog(wwjPanel, "Selected objects: " + size + "\nin region:\n" + selectedArea.toText());
            }
        });
        _selectByBoxButton.addActionListener(e -> {
            // _infoButton.setSelected(false);

            if (_selectByBoxButton.isSelected()) {
                selectByBoxSelector.enable();
                CursorUtils.makeCrossHair(wwjPanel.getWwd());
            } else {
                selectByBoxSelector.disable();
                CursorUtils.makeDefault(wwjPanel.getWwd());
            }
        });

        final ObjectsOnScreenByBoxSelector zoomByBoxSelector = new ObjectsOnScreenByBoxSelector(wwjPanel.getWwd());
        zoomByBoxSelector.addListener(new ObjectsOnScreenByBoxSelector.IBoxScreenSelectionListener(){
            @Override
            public void onSelectionFinished( Geometry selectedArea, List< ? > selectedObjs ) {
                try {
                    Sector sector = NwwUtilities
                            .envelope2Sector(new ReferencedEnvelope(selectedArea.getEnvelopeInternal(), NwwUtilities.GPS_CRS));
                    wwjPanel.goTo(sector, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }

            }
        });
        _zoomByBoxButton.addActionListener(e -> {
            // _infoButton.setSelected(false);

            if (_zoomByBoxButton.isSelected()) {
                zoomByBoxSelector.enable();
                CursorUtils.makeCrossHair(wwjPanel.getWwd());
            } else {
                zoomByBoxSelector.disable();
                CursorUtils.makeDefault(wwjPanel.getWwd());
            }
        });

        _openCacheButton.addActionListener(e -> {
            CacheUtils.openCacheManager();
        });

        _whiteBackgroundCheckbox.addActionListener(e -> {
            LayerList layers = wwjPanel.getWwd().getModel().getLayers();
            if (_whiteBackgroundCheckbox.isSelected()) {
                layers.add(0, whiteLayer);
                layerEventsListener.onLayerAdded(whiteLayer);
            } else {
                layers.remove(whiteLayer);
                layerEventsListener.onLayerRemoved(whiteLayer);
            }
        });

        _opaqueBackgroundCheckbox.setSelected(true);
        _opaqueBackgroundCheckbox.addActionListener(e -> {
            WorldWindow wwd = wwjPanel.getWwd();
            if (wwd instanceof WorldWindowGLJPanel) {
                ((WorldWindowGLJPanel) wwd).setOpaque(_opaqueBackgroundCheckbox.isSelected());
            }
        });

        _addAnnotationButton.addActionListener(e -> {

            String layerName = "Annotation (" + new Date().toString() + ")";
            int width = 300;
            int yPos = wwjPanel.getHeight() / 2;

            String htmldefaultText = width + "," + yPos + "," + //
            "<p>\n<b><font color=\"#664400\">LA CLAPI\u00c8RE</font></b><br />\n<i>Alt: "
                    + "1100-1700m</i>\n</p>\n<p>\n<b>Glissement de terrain majeur</b> dans la haute Tin\u00e9e, sur "
                    + "un flanc du <a href=\"http://www.mercantour.eu\">Parc du Mercantour</a>, Alpes Maritimes.\n</p>\n"
                    + "<p>\nRisque aggrav\u00e9 d'<b>inondation</b> du village de <i>Saint \u00c9tienne de Tin\u00e9e</i> "
                    + "juste en amont.\n</p><p>Last update:DATE</p>";

            String text = JOptionPane.showInputDialog("Enter the annotation's position and html text as: width,y position,text",
                    htmldefaultText);
            if (text == null) {
                return;
            }

            int firstComma = text.indexOf(',');
            String widthStr = text.substring(0, firstComma);
            int secondComma = text.indexOf(',', firstComma + 1);
            String yStr = text.substring(firstComma + 1, secondComma);

            String htmltext = text.substring(secondComma + 1);

            width = Integer.parseInt(widthStr);
            int xPos = width / 2 + 10;

            Builder builder = new HtmlScreenAnnotation.Builder();

            HtmlScreenAnnotation htmlScreenAnnotation = builder.size(new Dimension(width, 0)).htmlText(htmltext)
                    .position(new Point(xPos, Integer.parseInt(yStr))).build();

            AnnotationLayer layer = new AnnotationLayer(){
                @Override
                public String toString() {
                    return layerName;
                }
            };
            layer.addAnnotation(htmlScreenAnnotation);

            new Thread(new Runnable(){
                public void run() {
                    while( wwjPanel.getWwd().getModel().getLayers().contains(layer) ) {
                        // htmlScreenAnnotation
                        String newText = htmltext.replaceFirst("DATE", new Date().toString());
                        htmlScreenAnnotation.setText(newText);
                        wwjPanel.getWwd().redraw();
                        try {
                            Thread.sleep(300);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }).start();

            wwjPanel.getWwd().getModel().getLayers().add(layer);
            layerEventsListener.onLayerAdded(layer);

        });

    }

    public LayerEventsListener getLayerEventsListener() {
        return layerEventsListener;
    }

    /**
     * Use a DEM as the 3D terrain of its area, with the Copernicus terrain around it, switch to
     * the globe with 3D terrain and zoom to the DEM.
     */
    private void loadTerrainDem( File demFile ) {
        _terrainDemButton.setEnabled(false);
        new Thread(() -> {
            try {
                GridCoverage2D dem = OmsRasterReader.readRaster(demFile.getAbsolutePath());
                ElevationModel terrain = CoverageElevationModel.withSurrounding(dem, NwwPanel.getTerrainElevationModel());
                ReferencedEnvelope demEnvelope = new ReferencedEnvelope(dem.getEnvelope2D()).transform(NwwUtilities.GPS_CRS,
                        true);
                Sector sector = NwwUtilities.envelope2Sector(demEnvelope);
                SwingUtilities.invokeLater(() -> {
                    wwjPanel.setLocalTerrain(terrain);
                    _terrainDemLabel.setText("Terrain: " + FileUtilities.getNameWithoutExtention(demFile));
                    _terrainDemClearButton.setEnabled(true);
                    _terrainDemButton.setEnabled(true);
                    if (EGlobeModes.getModeFromDescription(_globeModeCombo.getSelectedItem().toString()) == EGlobeModes.EarthTerrain) {
                        wwjPanel.setSphereGlobe(true);
                    } else {
                        // applied by the listener of the combo
                        _globeModeCombo.setSelectedItem(EGlobeModes.EarthTerrain.getDescription());
                    }
                    wwjPanel.goTo(sector, false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    _terrainDemButton.setEnabled(true);
                    GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "Unable to use the DEM as terrain: " + e.getMessage());
                });
            }
        }, "NWW terrain DEM").start();
    }

    public NwwPanel getWwjPanel() {
        return wwjPanel;
    }

    /**
     * @return <code>true</code> if the file has one of the {@link NwwUtilities#SUPPORTED_EXTENSIONS}.
     */
    private static boolean isSupportedFile( File file ) {
        String name = file.getName().toLowerCase();
        for( String ext : NwwUtilities.SUPPORTED_EXTENSIONS ) {
            if (name.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Load files as layers, as selected in the file chooser or dropped on the layers panel.
     *
     * <p>Folders are loaded as shapefiles folders, more mapsforge files are loaded in a single
     * layer, the other files one by one. Files of not supported formats are reported.</p>
     *
     * @param files the files and folders to load.
     */
    public void loadFiles( List<File> files ) {
        if (files == null || files.isEmpty()) {
            return;
        }
        PreferencesHandler.setLastPath(files.get(0).getAbsolutePath());

        boolean allMaps = files.size() > 1 && files.stream().allMatch(f -> f.getName().toLowerCase().endsWith(".map"));
        if (allMaps) {
            try {
                File[] mapFiles = files.toArray(new File[0]);
                String layerName = mapFiles[0].getParentFile().getName() + "-maps";
                MapsforgeNwwLayer mbTileLayer = new MapsforgeNwwLayer(layerName, mapFiles, null, null);
                wwjPanel.getWwd().getModel().getLayers().add(mbTileLayer);
                layerEventsListener.onLayerAdded(mbTileLayer);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        List<String> notSupported = new ArrayList<>();
        for( File file : files ) {
            if (file.isDirectory()) {
                loadFolder(file);
            } else if (isSupportedFile(file)) {
                loadFile(file);
            } else {
                notSupported.add(file.getName());
            }
        }
        if (!notSupported.isEmpty()) {
            GuiUtilities.showWarningMessage(this, "WARNING", "The format of these files is not supported:\n"
                    + String.join("\n", notSupported) + "\n\nSupported are: " + String.join(", ", NwwUtilities.SUPPORTED_EXTENSIONS));
        }
    }

    /**
     * Load a folder of shapefiles in a single layer or, if that fails, its supported files one by one.
     */
    private void loadFolder( File folder ) {
        try {
            Layer layer;
            if (_useRasterizedCheckbox.isSelected()) {
                layer = new RasterizedShapefilesFolderNwwLayer(null, folder, null, true);
            } else {
                layer = new ShapefilesFolderLayer(folder.getAbsolutePath());
            }
            wwjPanel.getWwd().getModel().getLayers().add(layer);
            layerEventsListener.onLayerAdded(layer);
        } catch (Exception e) {
            e.printStackTrace();
            // try to load it as single files
            File[] listFiles = folder.listFiles(f -> f.isFile() && isSupportedFile(f));
            if (listFiles != null) {
                for( File file : listFiles ) {
                    loadFile(file);
                }
            }
        }
    }

    /**
     * Fly along the geometries of a shapefile or of a geopackage table, chosen if there are more.
     */
    private void flyAlongVector( File vectorFile ) {
        try {
            String vectorPath = vectorFile.getAbsolutePath();
            if (vectorFile.getName().toLowerCase().endsWith(".gpkg")) {
                List<String> featureTables = new ArrayList<>();
                try (GeopackageCommonDb db = new GeopackageDb()) {
                    db.open(vectorFile.getAbsolutePath());
                    for( FeatureEntry featureEntry : db.features() ) {
                        featureTables.add(featureEntry.getTableName());
                    }
                }
                if (featureTables.isEmpty()) {
                    GuiUtilities.showWarningMessage(this, "WARNING", "The geopackage contains no vector table.");
                    return;
                }
                String tableName = featureTables.get(0);
                if (featureTables.size() > 1) {
                    String[] tables = featureTables.toArray(new String[0]);
                    tableName = (String) JOptionPane.showInputDialog(this, "Select the table to fly along", "Table selection",
                            JOptionPane.QUESTION_MESSAGE, null, tables, tables[0]);
                    if (tableName == null) {
                        return;
                    }
                }
                vectorPath = vectorPath + HMConstants.DB_TABLE_PATH_SEPARATOR + tableName;
            }

            CurrentGpsPointLayer currentGpsPointLayer = new CurrentGpsPointLayer(null, null, null, null, null);
            SimplePointsLayer simplePointsLayer = new SimplePointsLayer("GPS Points");
            simplePointsLayer.setMaxMarkers(10);

            // read the vector before adding the layers, so that they are not added if it fails
            new FakeGps(vectorPath, wwjPanel, currentGpsPointLayer, simplePointsLayer);

            wwjPanel.getWwd().getModel().getLayers().add(currentGpsPointLayer);
            layerEventsListener.onLayerAdded(currentGpsPointLayer);
            wwjPanel.getWwd().getModel().getLayers().add(simplePointsLayer);
            layerEventsListener.onLayerAdded(simplePointsLayer);
        } catch (Exception e) {
            e.printStackTrace();
            GuiUtilities.showWarningMessage(this, "WARNING", "Unable to fly along the vector: " + e.getMessage());
        }
    }

    /**
     * Load the tiles and vector tables of a geopackage. If there are more, the user selects
     * those to load.
     */
    private void loadGeopackage( File gpkgFile ) throws Exception {
        List<String> tilesTables = new ArrayList<>();
        List<String> featureTables = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        try (GeopackageCommonDb db = new GeopackageDb()) {
            db.open(gpkgFile.getAbsolutePath());
            for( TileEntry tileEntry : db.tiles() ) {
                tilesTables.add(tileEntry.getTableName());
                labels.add(tileEntry.getTableName() + "  (tiles)");
            }
            for( FeatureEntry featureEntry : db.features() ) {
                featureTables.add(featureEntry.getTableName());
                String type = featureEntry.getGeometryType() != null
                        ? featureEntry.getGeometryType().name().toLowerCase()
                        : "vector";
                labels.add(featureEntry.getTableName() + "  (" + type + ")");
            }
        }
        if (labels.isEmpty()) {
            GuiUtilities.showWarningMessage(this, "WARNING", "The geopackage contains no tiles or vector table.");
            return;
        }

        boolean[] selected = new boolean[labels.size()];
        Arrays.fill(selected, true);
        if (labels.size() > 1) {
            selected = showTablesSelectionDialog(gpkgFile.getName(), labels);
            if (selected == null) {
                return;
            }
        }

        for( int i = 0; i < tilesTables.size(); i++ ) {
            if (selected[i]) {
                GeopackageTilesNwwLayer gpkgTilesLayer = new GeopackageTilesNwwLayer(gpkgFile, tilesTables.get(i));
                wwjPanel.getWwd().getModel().getLayers().add(gpkgTilesLayer);
                layerEventsListener.onLayerAdded(gpkgTilesLayer);
            }
        }

        // the tables are loaded as the other vectors, reprojected from their CRS
        // and styled with the table's style, if available
        for( int i = 0; i < featureTables.size(); i++ ) {
            if (!selected[tilesTables.size() + i]) {
                continue;
            }
            String tableName = featureTables.get(i);
            SimpleFeatureCollection readFC = NwwUtilities
                    .readAndReproject(gpkgFile.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + tableName);
            Style sldStyle = null;
            try (GeopackageCommonDb db = new GeopackageDb()) {
                db.open(gpkgFile.getAbsolutePath());
                String sldString = db.getSldString(SqlName.m(tableName));
                if (sldString != null) {
                    sldStyle = SldUtilities.getStyleFromSldString(sldString);
                }
            }
            String layerName = gpkgFile.getName() + HMConstants.DB_TABLE_PATH_SEPARATOR + tableName;
            loadFeatureCollection(null, layerName, null, readFC, null, sldStyle);
        }
    }

    /**
     * Let the user select the tables of a geopackage to load, all selected at the start.
     *
     * @param fileName the name of the geopackage.
     * @param labels the labels of the tables.
     * @return the selection state of each table or <code>null</code> if cancelled.
     */
    private boolean[] showTablesSelectionDialog( String fileName, List<String> labels ) {
        JPanel checksPanel = new JPanel(new GridLayout(0, 1));
        List<JCheckBox> checks = new ArrayList<>();
        for( String label : labels ) {
            JCheckBox check = new JCheckBox(label, true);
            checks.add(check);
            checksPanel.add(check);
        }
        JScrollPane scrollPane = new JScrollPane(checksPanel);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        Dimension size = scrollPane.getPreferredSize();
        scrollPane.setPreferredSize(new Dimension(Math.max(size.width + 20, 300), Math.min(size.height, 400)));

        JButton allButton = new JButton("Select all");
        allButton.addActionListener(e -> checks.forEach(c -> c.setSelected(true)));
        JButton noneButton = new JButton("Select none");
        noneButton.addActionListener(e -> checks.forEach(c -> c.setSelected(false)));
        JPanel selectionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        selectionButtons.add(allButton);
        selectionButtons.add(noneButton);

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("Select the layers to load from " + fileName + ":"), BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(selectionButtons, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(this, panel, "Geopackage layers", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        boolean[] selected = new boolean[checks.size()];
        for( int i = 0; i < selected.length; i++ ) {
            selected[i] = checks.get(i).isSelected();
        }
        return selected;
    }

    /**
     * Load a las/laz file as point cloud, asking sampling and coloring, and the CRS if the file has
     * none. The points are read in the background.
     */
    private void loadLasFile( File lasFile ) throws Exception {
        ALasReader reader = Las.getReader(lasFile);
        boolean started = false;
        try {
            ILasHeader header = reader.getHeader();
            long recordsCount = header.getRecordsCount();
            CoordinateReferenceSystem fileCrs = header.getCrs();

            long defaultSampling = Math.max(1, (recordsCount + LAS_DEFAULT_MAX_POINTS - 1) / LAS_DEFAULT_MAX_POINTS);
            JTextField samplingField = new JTextField(String.valueOf(defaultSampling), 8);
            samplingField.setToolTipText("Read one point every this: 1 reads all the points");
            JComboBox<ColorMode> colorCombo = new JComboBox<>(ColorMode.values());
            JTextField crsField = new JTextField(12);
            crsField.setToolTipText("The EPSG code of the coordinates of the file, e.g. EPSG:32632");

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(3, 3, 3, 3);
            c.anchor = GridBagConstraints.WEST;
            c.gridx = 0;
            c.gridy = 0;
            c.gridwidth = 2;
            String info = String.format("<html><b>%s</b>: %,d points", lasFile.getName(), recordsCount);
            if (fileCrs == null) {
                info += "<br>The file has no CRS: enter the one of its coordinates.";
            }
            panel.add(new JLabel(info + "</html>"), c);
            c.gridwidth = 1;
            c.gridy++;
            panel.add(new JLabel("Sampling"), c);
            c.gridx = 1;
            panel.add(samplingField, c);
            c.gridx = 0;
            c.gridy++;
            panel.add(new JLabel("Color by"), c);
            c.gridx = 1;
            panel.add(colorCombo, c);
            if (fileCrs == null) {
                c.gridx = 0;
                c.gridy++;
                panel.add(new JLabel("CRS"), c);
                c.gridx = 1;
                panel.add(crsField, c);
            }
            int answer = JOptionPane.showConfirmDialog(this, panel, "Load point cloud", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }

            int sampling;
            try {
                sampling = Math.max(1, Integer.parseInt(samplingField.getText().trim()));
            } catch (NumberFormatException e) {
                GuiUtilities.showWarningMessage(this, "WARNING", "The sampling needs to be an integer number.");
                return;
            }
            CoordinateReferenceSystem crs = fileCrs;
            if (crs == null) {
                String code = crsField.getText().trim();
                if (code.matches("\\d+")) {
                    code = "EPSG:" + code;
                }
                try {
                    crs = HMCrsRegistry.INSTANCE.getCrs(code, true);
                } catch (Exception e) {
                    crs = null;
                }
                if (crs == null) {
                    GuiUtilities.showWarningMessage(this, "WARNING", "Unable to use the CRS: " + code);
                    return;
                }
            }

            ColorMode mode = (ColorMode) colorCombo.getSelectedItem();
            CoordinateReferenceSystem _crs = crs;
            String layerName = lasFile.getName();
            new ExecutorProgressGui((int) (recordsCount / 1000)){
                @Override
                public void backGroundWork() throws Exception {
                    LasPointCloudLayer layer;
                    try {
                        layer = LasPointCloudLayer.fromReader(layerName, reader, _crs, sampling, mode, this);
                    } finally {
                        reader.close();
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (layer.getPointsCount() == 0) {
                            GuiUtilities.showWarningMessage(ToolsPanelController.this, "WARNING",
                                    "No point read from " + layerName);
                            return;
                        }
                        wwjPanel.getWwd().getModel().getLayers().add(layer);
                        layerEventsListener.onLayerAdded(layer);
                                        org.locationtech.jts.geom.Envelope bounds = layer.getBounds();
                        wwjPanel.goTo(Sector.fromDegrees(bounds.getMinY(), bounds.getMaxY(), bounds.getMinX(), bounds.getMaxX()),
                                true);
                    });
                }
            }.execute();
            started = true;
        } finally {
            if (!started) {
                reader.close();
            }
        }
    }

    private void loadFile( File selectedFile ) {
        String name = FileUtilities.getNameWithoutExtention(selectedFile);
        String lowerName = selectedFile.getName().toLowerCase();
        try {
            if (lowerName.endsWith(".asc") || lowerName.endsWith(".tiff") || lowerName.endsWith(".tif")) {
                GridCoverageNwwLayer coverageNwwLayer = new GridCoverageNwwLayer(selectedFile, null, Color.WHITE);
                wwjPanel.getWwd().getModel().getLayers().add(coverageNwwLayer);
                layerEventsListener.onLayerAdded(coverageNwwLayer);
            } else if (lowerName.endsWith(".shp")) {
                // shp or image mosaic?

                File parentFolder = selectedFile.getParentFile();
                String fileName = FileUtilities.getNameWithoutExtention(selectedFile);
                File imageMosaicPropertiesFile = new File(parentFolder, fileName + ".properties");
                try {
                    if (imageMosaicPropertiesFile.exists()) {
                        final ParameterValue<Color> inTransp = AbstractGridFormat.INPUT_TRANSPARENT_COLOR.createValue();
                        inTransp.setValue(Color.white);
                        final ParameterValue<Boolean> fading = ImageMosaicFormat.FADING.createValue();
                        fading.setValue(true);
                        final ParameterValue<Boolean> multiThread = ImageMosaicFormat.ALLOW_MULTITHREADING.createValue();
                        multiThread.setValue(true);
                        final ParameterValue<Boolean> usejai = ImageMosaicFormat.USE_IMAGEN_IMAGEREAD.createValue();
                        usejai.setValue(true);
                        GeneralParameterValue[] gp = new GeneralParameterValue[]{inTransp, usejai, multiThread};

                        ImageMosaicNwwLayer imageMosaicNwwLayer = new ImageMosaicNwwLayer(selectedFile, null, gp, true);
                        wwjPanel.getWwd().getModel().getLayers().add(imageMosaicNwwLayer);
                        layerEventsListener.onLayerAdded(imageMosaicNwwLayer);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // ignore and handle as shapefile
                }

                HashMap<String, String[]> field2ValuesMap = null;
                File codesFile = new File(selectedFile.getParentFile(), fileName + ".codes");
                if (codesFile.exists()) {
                    List<String> linesList = FileUtilities.readFileToLinesList(codesFile);
                    field2ValuesMap = new HashMap<>();
                    for( String line : linesList ) {
                        String[] split = line.split("=");
                        String key = split[0];
                        String[] values = split[1].split(";");
                        field2ValuesMap.put(key, values);
                    }
                }

                SimpleFeatureSource featureSource = NwwUtilities.readFeatureSource(selectedFile.getAbsolutePath());
                SimpleFeatureStore featureStore = null;
                if (featureSource instanceof SimpleFeatureStore) {
                    featureStore = (SimpleFeatureStore) featureSource;
                }
                SimpleFeatureCollection readFC = NwwUtilities.readAndReproject(featureSource);
                loadFeatureCollection(selectedFile, name, featureStore, readFC, field2ValuesMap);
            } else if (lowerName.endsWith(".las") || lowerName.endsWith(".laz")) {
                loadLasFile(selectedFile);
            } else if (lowerName.endsWith(".mbtiles")) {
                MBTilesNwwLayer mbTileLayer = new MBTilesNwwLayer(selectedFile);
                wwjPanel.getWwd().getModel().getLayers().add(mbTileLayer);
                layerEventsListener.onLayerAdded(mbTileLayer);
            } else if (lowerName.endsWith(".gpkg")) {
                loadGeopackage(selectedFile);
            } else if (lowerName.endsWith(".map")) {
                String layerName = FileUtilities.getNameWithoutExtention(selectedFile);
                MapsforgeNwwLayer mbTileLayer = new MapsforgeNwwLayer(layerName, new File[]{selectedFile}, null, null);
                wwjPanel.getWwd().getModel().getLayers().add(mbTileLayer);
                layerEventsListener.onLayerAdded(mbTileLayer);
            } else if (lowerName.endsWith(".rl2")) {
                GTSpatialiteThreadsafeDb db = new GTSpatialiteThreadsafeDb();
                db.open(selectedFile.getAbsolutePath());
                Rasterlite2Db rldb = new Rasterlite2Db(db);
                List<Rasterlite2Coverage> rasterCoverages = rldb.getRasterCoverages(false);
                if (rasterCoverages.size() > 0) {
                    Rasterlite2Coverage rasterCoverage = rasterCoverages.get(0);
                    RL2NwwLayer rl2Layer = new RL2NwwLayer(rasterCoverage, null);

                    wwjPanel.getWwd().getModel().getLayers().add(rl2Layer);
                    layerEventsListener.onLayerAdded(rl2Layer);
                }
            } else if (lowerName.endsWith(".sqlite")) {
                ASpatialDb db = new GTSpatialiteThreadsafeDb();
                db.open(selectedFile.getAbsolutePath());

                if (RasterizedSpatialiteLasLayer.isLasDb(db)) {
                    String[] options = {"elevation", "intensity"};
                    String option = (String) JOptionPane.showInputDialog(this, "Select data to view", "Data selection",
                            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    boolean doIntensity = false;
                    if (option.equals(options[1])) {
                        doIntensity = true;
                    }

                    RasterizedSpatialiteLasLayer rasterizedSpatialiteLayer = new RasterizedSpatialiteLasLayer(name, db, null,
                            true, doIntensity);
                    wwjPanel.getWwd().getModel().getLayers().add(rasterizedSpatialiteLayer);
                    layerEventsListener.onLayerAdded(rasterizedSpatialiteLayer);
                } else {
                    List<TableName> tableMaps = db.getTables();
                    String[] tables = new String[tableMaps.size()];
                    for( int i = 0; i < tableMaps.size(); i++ ) {
                        tables[i] = tableMaps.get(i).getName();
                    }
                    String tableName = (String) JOptionPane.showInputDialog(this, "Select the table to load", "Table selection",
                            JOptionPane.QUESTION_MESSAGE, null, tables, tables[0]);

                    if (_useRasterizedCheckbox.isSelected()) {
                        RasterizedSpatialiteLayer rasterizedSpatialiteLayer = new RasterizedSpatialiteLayer(name, db, tableName,
                                -1, null, null, true);
                        wwjPanel.getWwd().getModel().getLayers().add(rasterizedSpatialiteLayer);
                        layerEventsListener.onLayerAdded(rasterizedSpatialiteLayer);
                    } else {
                        GeometryColumn geometryColumn = db.getGeometryColumnsForTable(SqlName.m(tableName));
                        if (geometryColumn != null) {
                            org.hortonmachine.dbs.datatypes.EGeometryType geomType = geometryColumn.geometryType;
                            if (geomType.isPolygon()) {
                                SpatialitePolygonLayer layer = new SpatialitePolygonLayer(db, tableName, 10000);
                                wwjPanel.getWwd().getModel().getLayers().add(layer);
                                layerEventsListener.onLayerAdded(layer);
                            } else if (geomType.isLine()) {
                                SpatialiteLinesLayer layer = new SpatialiteLinesLayer(db, tableName, 10000);
                                wwjPanel.getWwd().getModel().getLayers().add(layer);
                                layerEventsListener.onLayerAdded(layer);
                            } else if (geomType.isPoint()) {
                                SpatialitePointsLayer layer = new SpatialitePointsLayer(db, tableName, 10000);
                                wwjPanel.getWwd().getModel().getLayers().add(layer);
                                layerEventsListener.onLayerAdded(layer);
                            }
                        }
                    }
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * Load a feature collection to layer.
     * 
     * @param selectedFile
     *            can be null, used for the style.
     * @param name
     *            name oof the layer.
     * @param featureStore
     *            can be null, used for editing attributes.
     * @param readFC
     *            the feature collection to load.
     * @param field2ValuesMap
     * @throws Exception
     */
    public void loadFeatureCollection( File selectedFile, String name, SimpleFeatureStore featureStore,
            SimpleFeatureCollection readFC, HashMap<String, String[]> field2ValuesMap ) throws Exception {
        loadFeatureCollection(selectedFile, name, featureStore, readFC, field2ValuesMap, null);
    }

    /**
     * Load a feature collection to layer.
     *
     * @param selectedFile can be null, used for the style.
     * @param name name of the layer.
     * @param featureStore can be null, used for editing attributes.
     * @param readFC the feature collection to load, in WGS84 lon/lat.
     * @param field2ValuesMap can be null.
     * @param sldStyle an optional style to use instead of the one of the file, as for example
     *          the one stored in a geopackage table.
     * @throws Exception
     */
    public void loadFeatureCollection( File selectedFile, String name, SimpleFeatureStore featureStore,
            SimpleFeatureCollection readFC, HashMap<String, String[]> field2ValuesMap, Style sldStyle ) throws Exception {
        loadFeatureCollection(selectedFile, name, featureStore, readFC, field2ValuesMap, sldStyle, null);
    }

    /**
     * Load a feature collection to layer with a given color, as for virtual layers that
     * have neither a file nor a style.
     *
     * @param name name of the layer.
     * @param readFC the feature collection to load, in WGS84 lon/lat.
     * @param color the color of the layer.
     * @throws Exception
     */
    public void loadFeatureCollection( String name, SimpleFeatureCollection readFC, Color color ) throws Exception {
        SimpleStyle simpleStyle = new SimpleStyle();
        simpleStyle.fillColor = color;
        simpleStyle.strokeColor = color;
        loadFeatureCollection(null, name, null, readFC, null, null, simpleStyle);
    }

    /**
     * Load a feature collection to layer with the next of the default colors, as the layers
     * without a style.
     *
     * @param name name of the layer.
     * @param readFC the feature collection to load, in WGS84 lon/lat.
     * @throws Exception
     */
    public void loadFeatureCollection( String name, SimpleFeatureCollection readFC ) throws Exception {
        loadFeatureCollection(null, name, null, readFC, null, null, null);
    }

    private void loadFeatureCollection( File selectedFile, String name, SimpleFeatureStore featureStore,
            SimpleFeatureCollection readFC, HashMap<String, String[]> field2ValuesMap, Style sldStyle,
            SimpleStyle simpleStyle ) throws Exception {
        if (_useRasterizedCheckbox.isSelected()) {
            Style style = sldStyle;
            if (style == null && selectedFile != null)
                style = SldUtilities.getStyleFromFile(selectedFile);
            if (style == null)
                style = SLD.createSimpleStyle(readFC.getSchema(), simpleStyle != null ? simpleStyle.strokeColor : nextDefaultColor());
            RasterizedFeatureCollectionLayer collectionLayer = new RasterizedFeatureCollectionLayer(name, readFC, style, null,
                    true);

            wwjPanel.getWwd().getModel().getLayers().add(collectionLayer);
            layerEventsListener.onLayerAdded(collectionLayer);
        } else {

            GeometryDescriptor geometryDescriptor = readFC.getSchema().getGeometryDescriptor();
            String absolutePath = null;
            if (selectedFile != null)
                absolutePath = selectedFile.getAbsolutePath();
            if (EGeometryType.isPolygon(geometryDescriptor)) {
                FeatureCollectionPolygonLayer featureCollectionPolygonLayer = new FeatureCollectionPolygonLayer(name, readFC,
                        featureStore, field2ValuesMap);

                featureCollectionPolygonLayer.setElevationMode(WorldWind.RELATIVE_TO_GROUND);
                featureCollectionPolygonLayer.setExtrusionProperties(5.0, null, null, true);
                SimpleStyle style = simpleStyle != null
                        ? simpleStyle
                        : getSimpleStyle(sldStyle, absolutePath, EGeometryType.POLYGON.name());
                if (style != null) {
                    featureCollectionPolygonLayer.setStyle(style);
                }

                wwjPanel.getWwd().getModel().getLayers().add(featureCollectionPolygonLayer);
                layerEventsListener.onLayerAdded(featureCollectionPolygonLayer);
            } else if (EGeometryType.isLine(geometryDescriptor)) {
                FeatureCollectionLinesLayer featureCollectionLinesLayer = new FeatureCollectionLinesLayer(name, readFC,
                        featureStore, field2ValuesMap);
                featureCollectionLinesLayer.setElevationMode(WorldWind.RELATIVE_TO_GROUND);
                featureCollectionLinesLayer.setExtrusionProperties(5.0, null, null, true);
                SimpleStyle style = simpleStyle != null
                        ? simpleStyle
                        : getSimpleStyle(sldStyle, absolutePath, EGeometryType.LINESTRING.name());
                if (style != null) {
                    featureCollectionLinesLayer.setStyle(style);
                }

                wwjPanel.getWwd().getModel().getLayers().add(featureCollectionLinesLayer);
                layerEventsListener.onLayerAdded(featureCollectionLinesLayer);
            } else if (EGeometryType.isPoint(geometryDescriptor)) {
                String imagePath = null;
                if (selectedFile != null) {
                    // check if there is an image
                    File imageFile = new File(selectedFile.getParentFile(), name + ".png");
                    imagePath = null;
                    if (imageFile.exists()) {
                        imagePath = imageFile.getAbsolutePath();
                    }
                }
                FeatureCollectionPointsLayer featureCollectionPointsLayer = new FeatureCollectionPointsLayer(name, readFC,
                        featureStore, field2ValuesMap, imagePath);
                SimpleStyle style = simpleStyle != null
                        ? simpleStyle
                        : getSimpleStyle(sldStyle, absolutePath, EGeometryType.POINT.name());
                if (style != null) {
                    featureCollectionPointsLayer.setStyle(style);
                }

                wwjPanel.getWwd().getModel().getLayers().add(featureCollectionPointsLayer);
                layerEventsListener.onLayerAdded(featureCollectionPointsLayer);

            } else {
                System.err.println("?????");
            }
        }
    }

    /**
     * @return the style from the given sld style, if available, else the one of the file, else
     *          the next one of the default colors.
     */
    private SimpleStyle getSimpleStyle( Style sldStyle, String absolutePath, String geomTypeName ) throws Exception {
        if (sldStyle != null) {
            SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(sldStyle, geomTypeName);
            if (style != null) {
                return style;
            }
        }
        if (absolutePath != null) {
            SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(absolutePath, geomTypeName);
            if (style != null) {
                return style;
            }
        }
        Color color = nextDefaultColor();
        SimpleStyle style = new SimpleStyle();
        style.fillColor = color;
        // polygons get a darker outline, to separate the adjacent ones
        style.strokeColor = EGeometryType.POLYGON.name().equals(geomTypeName) ? color.darker() : color;
        return style;
    }

    /**
     * @return the next color of the rotation used for the layers without a style.
     */
    private Color nextDefaultColor() {
        Color color = DEFAULT_LAYER_COLORS[nextDefaultColorIndex % DEFAULT_LAYER_COLORS.length];
        nextDefaultColorIndex++;
        return color;
    }


    /**
     * Arrange the tools in a single narrow column, to leave the space to the map.
     */
    private void compactLayout() {
        JPanel panel = new JPanel(new GridBagLayout());
        // no title: the panel is in the "Tools" tab of the viewer
        panel.setBorder(BorderFactory.createEmptyBorder(4, 2, 4, 2));
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(2, 4, 2, 4);

        for( Object[] labelAndButton : new Object[][]{{_loadFileLabel, _loadFileButton}, {_loadGpsLabel, _loadGpsButton},
                {_openCacheLabel, _openCacheButton}} ) {
            JPanel row = new JPanel(new BorderLayout(4, 0));
            row.add((JComponent) labelAndButton[0], BorderLayout.CENTER);
            row.add((JComponent) labelAndButton[1], BorderLayout.EAST);
            panel.add(row, c);
            c.gridy++;
        }

        c.insets = new Insets(8, 4, 0, 4);
        panel.add(_globeModeLabel, c);
        c.gridy++;
        c.insets = new Insets(2, 4, 2, 4);
        panel.add(_globeModeCombo, c);
        c.gridy++;
        JPanel terrainButtons = new JPanel(new GridLayout(1, 2, 2, 0));
        terrainButtons.add(_terrainDemButton);
        terrainButtons.add(_terrainDemClearButton);
        JPanel terrainRow = new JPanel(new BorderLayout(4, 0));
        terrainRow.add(_terrainDemLabel, BorderLayout.CENTER);
        terrainRow.add(terrainButtons, BorderLayout.EAST);
        panel.add(terrainRow, c);
        c.gridy++;

        c.insets = new Insets(6, 4, 0, 4);
        for( JComponent check : new JComponent[]{_useRasterizedCheckbox, _whiteBackgroundCheckbox, _opaqueBackgroundCheckbox} ) {
            panel.add(check, c);
            c.gridy++;
            c.insets = new Insets(0, 4, 0, 4);
        }

        JPanel buttons = new JPanel(new GridLayout(0, 2, 4, 4));
        for( JComponent button : new JComponent[]{_infoEditingButton, _selectByBoxButton, _zoomByBoxButton,
                _addAnnotationButton, _pasteWkt} ) {
            buttons.add(button);
        }
        c.insets = new Insets(8, 4, 4, 4);
        panel.add(buttons, c);

        removeAll();
        setLayout(new BorderLayout());
        add(panel, BorderLayout.NORTH);
    }

}
