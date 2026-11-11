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
package org.hortonmachine.nww.gui;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.Symbolizer;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
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
import org.geotools.gce.geotiff.GeoTiffReader;
import org.hortonmachine.gears.io.las.core.ALasReader;
import org.hortonmachine.gears.io.stac.assets.handlers.GeotiffHandler;
import org.hortonmachine.gears.io.las.core.ILasHeader;
import org.hortonmachine.gears.io.las.core.Las;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.spatialite.GTSpatialiteThreadsafeDb;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.ui.progress.ProgressUpdate;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.style.StyleUtilities;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.executor.ExecutorProgressGui;
import org.hortonmachine.nww.layers.defaults.NwwVectorLayer;
import org.hortonmachine.nww.layers.defaults.other.LasPointCloudLayer;
import org.hortonmachine.nww.layers.defaults.other.LasPointCloudLayer.ColorMode;
import org.hortonmachine.nww.layers.defaults.raster.GeopackageTilesNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.ImageMosaicNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.MBTilesNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.MapsforgeNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.RL2NwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.TemporalRasterNwwLayer;
import org.hortonmachine.gears.utils.time.DatedFilesScanner.DatedFile;
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
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.style.SimpleStyle;
import org.hortonmachine.style.SimpleStyleUtilities;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;

/**
 * Loads files and feature collections as layers of the viewer, and reloads and restyles the
 * vector layers.
 *
 * <p>The vector layers are drawn either as 3D shapes with one simple style, or, if
 * {@link #setPreferRasterized(boolean) preferred}, as image tiles with the full SLD style. The
 * layers loaded without a style get the next of a rotation of colors.</p>
 *
 * <p>The loading of some formats asks the user, as for the tables of a geopackage or the sampling
 * of a point cloud, so the methods are called from the event dispatch thread.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class NwwLayerLoader {

    /** The key of the SLD style of a rasterized vector layer, in its attributes. */
    private static final String RASTERIZED_STYLE_KEY = "org.hortonmachine.nww.gui.NwwLayerLoader.rasterizedStyle";

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

    /** The points of a las file loaded by default, the sampling proposed keeps under them. */
    private static final long LAS_DEFAULT_MAX_POINTS = 20_000_000;

    private final NwwPanel wwjPanel;
    private final LayerEventsListener layerEventsListener;
    private int nextDefaultColorIndex = 0;
    private boolean preferRasterized = false;

    /**
     * @param wwjPanel the map the layers are added to.
     * @param layerEventsListener the listener told of the layers added, as the layers panel.
     */
    public NwwLayerLoader( NwwPanel wwjPanel, LayerEventsListener layerEventsListener ) {
        this.wwjPanel = wwjPanel;
        this.layerEventsListener = layerEventsListener;
    }

    /**
     * @return <code>true</code> if the vectors loaded are drawn as image tiles.
     */
    public boolean isPreferRasterized() {
        return preferRasterized;
    }

    /**
     * @param rasterized <code>true</code> to draw the vectors loaded from now on as image tiles,
     *          instead of shapes.
     */
    public void setPreferRasterized( boolean rasterized ) {
        preferRasterized = rasterized;
    }

    /**
     * @param file a file.
     * @return <code>true</code> if the file has one of the {@link NwwUtilities#SUPPORTED_EXTENSIONS}.
     */
    public static boolean isSupportedFile( File file ) {
        String name = file.getName().toLowerCase();
        for( String ext : NwwUtilities.SUPPORTED_EXTENSIONS ) {
            if (name.endsWith("." + ext)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Add a layer on top of the map and to the layers panel. A {@link LayerSource} is attached
     * before, for the layers panel to show the name as the one of a table or a service layer.
     *
     * @param layer the layer.
     */
    public void addLayer( Layer layer ) {
        wwjPanel.getWwd().getModel().getLayers().add(layer);
        layerEventsListener.onLayerAdded(layer);
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
                addLayer(new MapsforgeNwwLayer(layerName, mapFiles, null, null));
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
            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "The format of these files is not supported:\n"
                    + String.join("\n", notSupported) + "\n\nSupported are: " + String.join(", ", NwwUtilities.SUPPORTED_EXTENSIONS));
        }
    }

    /**
     * Load a folder of shapefiles in a single layer or, if that fails, its supported files one by one.
     */
    private void loadFolder( File folder ) {
        try {
            Layer layer;
            if (preferRasterized) {
                layer = new RasterizedShapefilesFolderNwwLayer(null, folder, null, true);
            } else {
                layer = new ShapefilesFolderLayer(folder.getAbsolutePath());
            }
            addLayer(layer);
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
     * Load the tiles and vector tables of a geopackage. If there are more, the user selects
     * those to load.
     */
    private void loadGeopackage( File gpkgFile ) throws Exception {
        List<String> tilesTables = new ArrayList<>();
        List<String> featureTables = new ArrayList<>();
        List<String> names = new ArrayList<>();
        List<String> types = new ArrayList<>();
        try (GeopackageCommonDb db = new GeopackageDb()) {
            db.open(gpkgFile.getAbsolutePath());
            for( TileEntry tileEntry : db.tiles() ) {
                tilesTables.add(tileEntry.getTableName());
                names.add(tileEntry.getTableName());
                types.add("tiles");
            }
            for( FeatureEntry featureEntry : db.features() ) {
                featureTables.add(featureEntry.getTableName());
                String type = featureEntry.getGeometryType() != null
                        ? featureEntry.getGeometryType().name().toLowerCase()
                        : "vector";
                names.add(featureEntry.getTableName());
                types.add(type);
            }
        }
        if (names.isEmpty()) {
            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "The geopackage contains no tiles or vector table.");
            return;
        }

        boolean[] selected = new boolean[names.size()];
        Arrays.fill(selected, true);
        if (names.size() > 1) {
            TablesSelectionDialog.Result result = TablesSelectionDialog.show(wwjPanel, "Geopackage layers",
                    "Select the layers to load from " + gpkgFile.getName() + ":", names, types, true, null, null, false);
            if (result == null) {
                return;
            }
            selected = result.selected();
        }

        for( int i = 0; i < tilesTables.size(); i++ ) {
            if (selected[i]) {
                addLayer(new GeopackageTilesNwwLayer(gpkgFile, tilesTables.get(i)));
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
            Layer layer = loadFeatureCollection(null, layerName, null, readFC, null, sldStyle, null);
            if (layer != null) {
                new LayerSource(gpkgFile, tableName).attachTo(layer);
            }
        }
    }

    /**
     * Load a raster file in the background: the layer reads the header and, without .sld file,
     * the value range of the default style, which can take a while on large rasters or slow
     * disks. The tiles are then read when the map shows them.
     */
    private void loadRasterFile( File rasterFile ) {
        new ExecutorProgressGui(1){
            private GridCoverageNwwLayer layer;

            @Override
            public void backGroundWork() throws Exception {
                publish(new ProgressUpdate("Reading " + rasterFile.getName() + " ...", 0));
                GridCoverageNwwLayer rasterLayer = new GridCoverageNwwLayer(rasterFile, null, Color.WHITE);
                new LayerSource(rasterFile, null).attachTo(rasterLayer);
                layer = rasterLayer;
            }

            @Override
            public void done() {
                super.done();
                if (layer != null) {
                    addLayer(layer);
                }
            }
        }.execute();
    }

    /**
     * Load the dated rasters of a folder as a time series, in the background: the style of the
     * series saved before is used, else a default one on the value range of a sample of the
     * files, the same for all dates.
     *
     * @param name the name of the layer.
     * @param folder the folder of the files.
     * @param pattern the pattern of the dates in the names of the files.
     * @param files the files, sorted by date.
     */
    public void loadTemporalRasters( String name, File folder, String pattern, List<DatedFile> files ) {
        new ExecutorProgressGui(1){
            private TemporalRasterNwwLayer layer;

            @Override
            public void backGroundWork() throws Exception {
                Style style = LocalStyleStore.readTemporal(folder);
                if (style == null) {
                    publish(new ProgressUpdate("Reading the value range of " + name + " ...", 0));
                    style = TemporalRasterNwwLayer.createDefaultStyle(files);
                }
                publish(new ProgressUpdate("Reading " + files.get(0).file().getName() + " ...", 0));
                layer = new TemporalRasterNwwLayer(name, folder, pattern, files, style);
            }

            @Override
            public void done() {
                super.done();
                if (layer != null) {
                    addLayer(layer);
                }
            }
        }.execute();
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
            int answer = JOptionPane.showConfirmDialog(wwjPanel, panel, "Load point cloud", JOptionPane.OK_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) {
                return;
            }

            int sampling;
            try {
                sampling = Math.max(1, Integer.parseInt(samplingField.getText().trim()));
            } catch (NumberFormatException e) {
                GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "The sampling needs to be an integer number.");
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
                    GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "Unable to use the CRS: " + code);
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
                            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "No point read from " + layerName);
                            return;
                        }
                        new LayerSource(lasFile, null).attachTo(layer);
                        addLayer(layer);
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
            if (isRasterFile(selectedFile)) {
                loadRasterFile(selectedFile);
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

                        addLayer(new ImageMosaicNwwLayer(selectedFile, null, gp, true));
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // ignore and handle as shapefile
                }

                HashMap<String, String[]> field2ValuesMap = readCodes(selectedFile);

                SimpleFeatureSource featureSource = NwwUtilities.readFeatureSource(selectedFile.getAbsolutePath());
                SimpleFeatureStore featureStore = null;
                if (featureSource instanceof SimpleFeatureStore) {
                    featureStore = (SimpleFeatureStore) featureSource;
                }
                SimpleFeatureCollection readFC = NwwUtilities.readAndReproject(featureSource);
                Layer layer = loadFeatureCollection(selectedFile, name, featureStore, readFC, field2ValuesMap, null, null);
                if (layer != null) {
                    new LayerSource(selectedFile, null).attachTo(layer);
                }
            } else if (lowerName.endsWith(".las") || lowerName.endsWith(".laz")) {
                loadLasFile(selectedFile);
            } else if (lowerName.endsWith(".mbtiles")) {
                addLayer(new MBTilesNwwLayer(selectedFile));
            } else if (lowerName.endsWith(".gpkg")) {
                loadGeopackage(selectedFile);
            } else if (lowerName.endsWith(".map")) {
                String layerName = FileUtilities.getNameWithoutExtention(selectedFile);
                addLayer(new MapsforgeNwwLayer(layerName, new File[]{selectedFile}, null, null));
            } else if (lowerName.endsWith(".rl2")) {
                GTSpatialiteThreadsafeDb db = new GTSpatialiteThreadsafeDb();
                db.open(selectedFile.getAbsolutePath());
                Rasterlite2Db rldb = new Rasterlite2Db(db);
                List<Rasterlite2Coverage> rasterCoverages = rldb.getRasterCoverages(false);
                if (rasterCoverages.size() > 0) {
                    Rasterlite2Coverage rasterCoverage = rasterCoverages.get(0);
                    addLayer(new RL2NwwLayer(rasterCoverage, null));
                }
            } else if (lowerName.endsWith(".sqlite")) {
                ASpatialDb db = new GTSpatialiteThreadsafeDb();
                db.open(selectedFile.getAbsolutePath());

                if (RasterizedSpatialiteLasLayer.isLasDb(db)) {
                    String[] options = {"elevation", "intensity"};
                    String option = (String) JOptionPane.showInputDialog(wwjPanel, "Select data to view", "Data selection",
                            JOptionPane.QUESTION_MESSAGE, null, options, options[0]);
                    boolean doIntensity = false;
                    if (option.equals(options[1])) {
                        doIntensity = true;
                    }

                    addLayer(new RasterizedSpatialiteLasLayer(name, db, null, true, doIntensity));
                } else {
                    List<TableName> tableMaps = db.getTables();
                    String[] tables = new String[tableMaps.size()];
                    for( int i = 0; i < tableMaps.size(); i++ ) {
                        tables[i] = tableMaps.get(i).getName();
                    }
                    String tableName = (String) JOptionPane.showInputDialog(wwjPanel, "Select the table to load", "Table selection",
                            JOptionPane.QUESTION_MESSAGE, null, tables, tables[0]);

                    if (preferRasterized) {
                        addLayer(new RasterizedSpatialiteLayer(name, db, tableName, -1, null, null, true));
                    } else {
                        GeometryColumn geometryColumn = db.getGeometryColumnsForTable(SqlName.m(tableName));
                        if (geometryColumn != null) {
                            org.hortonmachine.dbs.datatypes.EGeometryType geomType = geometryColumn.geometryType;
                            if (geomType.isPolygon()) {
                                addLayer(new SpatialitePolygonLayer(db, tableName, 10000));
                            } else if (geomType.isLine()) {
                                addLayer(new SpatialiteLinesLayer(db, tableName, 10000));
                            } else if (geomType.isPoint()) {
                                addLayer(new SpatialitePointsLayer(db, tableName, 10000));
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

    /**
     * Load a feature collection to layer, keeping where its data come from, as for the tables of
     * a database connection.
     *
     * <p>The source is attached before the layer is added, so that the layers panel can show the
     * name as the one of a table.</p>
     *
     * @param name name of the layer.
     * @param readFC the feature collection to load, in WGS84 lon/lat.
     * @param source the source of the data, attached to the layer.
     * @param sldStyle the style of the layer, <code>null</code> for the next of the default colors.
     * @return the layer added, or <code>null</code> for an unknown geometry type.
     * @throws Exception
     */
    public Layer loadFeatureCollection( String name, SimpleFeatureCollection readFC, LayerSource source, Style sldStyle )
            throws Exception {
        Layer layer = createFeatureCollectionLayer(preferRasterized, null, name, null, readFC, null, sldStyle, null);
        if (layer != null) {
            if (source != null) {
                source.attachTo(layer);
            }
            addLayer(layer);
        }
        return layer;
    }

    /**
     * @return the layer added, or <code>null</code> for an unknown geometry type.
     */
    private Layer loadFeatureCollection( File selectedFile, String name, SimpleFeatureStore featureStore,
            SimpleFeatureCollection readFC, HashMap<String, String[]> field2ValuesMap, Style sldStyle,
            SimpleStyle simpleStyle ) throws Exception {
        Layer layer = createFeatureCollectionLayer(preferRasterized, selectedFile, name, featureStore, readFC, field2ValuesMap,
                sldStyle, simpleStyle);
        if (layer != null) {
            addLayer(layer);
        }
        return layer;
    }

    /**
     * Read again the data of a vector layer loaded from a shapefile, a geopackage table or a table
     * of a database connection, keeping only the features that match a filter, and make a new
     * layer of the same kind, with the same name and style. The database tables are read in the
     * same extent as before, if they were read in one, as the map view.
     *
     * <p>The new layer is not added to the viewer: it is meant to replace the given one, as with
     * {@link org.hortonmachine.nww.tools.NwwContext#replaceLayer(Layer, Layer)}.</p>
     *
     * @param layer the layer, which needs a {@link LayerSource}.
     * @param filter the filter of the features, as a SQL WHERE condition for the geopackage and
     *          database tables and as ECQL for the shapefiles, <code>null</code> or blank to keep
     *          all of them.
     * @return the new layer, with its {@link LayerSource} giving the filter.
     * @throws Exception if the data can't be read, as for a wrong filter.
     */
    public Layer reloadVectorLayer( Layer layer, String filter ) throws Exception {
        return reloadVectorLayer(layer, filter, null);
    }

    /**
     * Read again the data of a vector layer loaded from a shapefile or a geopackage table, as
     * {@link #reloadVectorLayer(Layer, String)}, with a new style.
     *
     * @param layer the layer, which needs a {@link LayerSource}.
     * @param filter the filter of the features, <code>null</code> or blank to keep all of them.
     * @param style the new style, <code>null</code> to keep the one the layer has.
     * @return the new layer, with its {@link LayerSource} giving the filter.
     * @throws Exception if the data can't be read, as for a wrong filter.
     */
    public Layer reloadVectorLayer( Layer layer, String filter, Style style ) throws Exception {
        LayerSource source = LayerSource.of(layer);
        if (source == null || !(source.isShapefile() || source.isGeopackageTable() || source.isDatabaseTable())) {
            throw new IllegalArgumentException(
                    "The layer is not read from a shapefile, a geopackage table or a database table: " + layer.getName());
        }
        String where = filter == null || filter.isBlank() ? null : filter.trim();
        boolean rasterized = layer instanceof RasterizedFeatureCollectionLayer;
        // the style the layer has now, which may have been changed after the loading
        Style sldStyle = style != null ? style : getSldStyle(layer);
        SimpleStyle simpleStyle = layer instanceof NwwVectorLayer ? ((NwwVectorLayer) layer).getStyle() : null;

        File styleFile = null;
        SimpleFeatureStore featureStore = null;
        HashMap<String, String[]> field2ValuesMap = null;
        SimpleFeatureCollection readFC;
        if (source.isShapefile()) {
            SimpleFeatureSource featureSource = NwwUtilities.readFeatureSource(source.file().getAbsolutePath());
            if (featureSource instanceof SimpleFeatureStore) {
                featureStore = (SimpleFeatureStore) featureSource;
            }
            readFC = where == null ? featureSource.getFeatures() : featureSource.getFeatures(ECQL.toFilter(where));
            field2ValuesMap = readCodes(source.file());
            styleFile = source.file();
        } else if (source.isDatabaseTable()) {
            // read in the same extent as before, as the map view the table was loaded in
            try (ASpatialDb db = NwwDatabaseLoader.openDb(source.connection())) {
                SqlName table = SqlName.m(source.table());
                readFC = NwwDatabaseLoader.readFeatures(db, table, source.extent(), where);
                if (readFC.getSchema() == null) {
                    // no feature matches: the type of a record read without conditions makes an
                    // empty layer
                    SimpleFeatureType schema = SpatialDbsImportUtils.tableToFeatureFCollection(db, table, 1, -1, null)
                            .getSchema();
                    if (schema == null) {
                        throw new IllegalArgumentException("The table has no features: " + source.path());
                    }
                    readFC = new ListFeatureCollection(schema);
                }
            }
        } else {
            try (GeopackageCommonDb db = new GeopackageDb()) {
                db.open(source.file().getAbsolutePath());
                SqlName table = SqlName.m(source.table());
                readFC = SpatialDbsImportUtils.tableToFeatureFCollection(db, table, -1, -1, where);
                if (readFC.getSchema() == null) {
                    // no feature matches and the type is built from the read records: take it
                    // from a record read without filter, to make an empty layer
                    SimpleFeatureType schema = SpatialDbsImportUtils.tableToFeatureFCollection(db, table, 1, -1, null)
                            .getSchema();
                    if (schema == null) {
                        throw new IllegalArgumentException("The table has no features: " + source.path());
                    }
                    readFC = new ListFeatureCollection(schema);
                }
            }
        }
        CoordinateReferenceSystem crs = readFC.getSchema().getCoordinateReferenceSystem();
        if (crs != null && !HMCrsRegistry.crsEquals(crs, NwwUtilities.GPS_CRS)) {
            readFC = new HMCrsTransformer(crs, NwwUtilities.GPS_CRS).transform(readFC);
        }

        Layer replacement = createFeatureCollectionLayer(rasterized, styleFile, layer.getName(), featureStore, readFC,
                field2ValuesMap, sldStyle, simpleStyle);
        if (replacement == null) {
            throw new IllegalArgumentException("Unsupported geometry type in: " + source.path());
        }
        source.withFilter(where).attachTo(replacement);
        return replacement;
    }

    /**
     * @param layer a vector layer.
     * @return the SLD style the layer is drawn with, <code>null</code> if it has none, as a 3D
     *          layer drawn with one simple style.
     */
    public static Style getSldStyle( Layer layer ) {
        if (layer instanceof RasterizedFeatureCollectionLayer) {
            return (Style) layer.getValue(RASTERIZED_STYLE_KEY);
        } else if (layer instanceof NwwVectorLayer) {
            return ((NwwVectorLayer) layer).getSldStyle();
        }
        return null;
    }

    /**
     * Draw a vector layer with a new style.
     *
     * <p>The 3D layers take the style as they are. The rasterized layers keep their style, so a
     * new layer is made, with the same filter: it is not added to the viewer, it is meant to
     * replace the given one, as with
     * {@link org.hortonmachine.nww.tools.NwwContext#replaceLayer(Layer, Layer)}.</p>
     *
     * @param layer the layer, a rasterized one needs a {@link LayerSource}.
     * @param style the style.
     * @return the given layer if it took the style, else the new layer.
     * @throws Exception if the data can't be read again.
     */
    public Layer restyleVectorLayer( Layer layer, Style style ) throws Exception {
        if (!(layer instanceof RasterizedFeatureCollectionLayer) && layer instanceof NwwVectorLayer) {
            ((NwwVectorLayer) layer).setSldStyle(style);
            wwjPanel.getWwd().redraw();
            return layer;
        }
        LayerSource source = LayerSource.of(layer);
        if (source != null && (source.isDatabaseTable() || source.isWebService())
                && layer instanceof RasterizedFeatureCollectionLayer) {
            // the features in memory are drawn again: reading the table or the service again
            // would cost the round trip to the server
            RasterizedFeatureCollectionLayer rasterized = (RasterizedFeatureCollectionLayer) layer;
            Layer replacement = createFeatureCollectionLayer(true, null, layer.getName(), rasterized.getFeatureStore(),
                    rasterized.getFeatureCollection(), null, style, null);
            source.attachTo(replacement);
            return replacement;
        }
        return reloadVectorLayer(layer, source != null ? source.filter() : null, style);
    }

    /**
     * Make a new layer of a raster file, or of a remote cloud optimized GeoTIFF, drawn with a
     * style. The layer is not added to the viewer: it is meant to replace the given one, as with
     * {@link org.hortonmachine.nww.tools.NwwContext#replaceLayer(Layer, Layer)}.
     *
     * @param layer the raster layer, which needs a {@link LayerSource}.
     * @param style the style, <code>null</code> for the one of the .sld file next to the raster,
     *          or saved in the {@link LocalStyleStore} for the remote rasters, if there is one,
     *          else the default one.
     * @return the new layer, with the same source.
     * @throws Exception if the raster can't be read.
     */
    public Layer restyleRasterLayer( Layer layer, Style style ) throws Exception {
        LayerSource source = LayerSource.of(layer);
        if (source == null || !(layer instanceof GridCoverageNwwLayer)) {
            throw new IllegalArgumentException("The layer is not read from a raster file: " + layer.getName());
        }
        if (((GridCoverageNwwLayer) layer).isRemote() && source.isWebService()) {
            // a new reader, since the replaced layer disposes its own; without style, the one
            // saved locally, else the default one
            Style remoteStyle = style != null ? style : LocalStyleStore.read(source);
            GeoTiffReader reader = GeotiffHandler.createCogReader(source.service(), null, null);
            GridCoverageNwwLayer replacement = new GridCoverageNwwLayer(reader, source.table(), source.service(), null,
                    Color.WHITE, remoteStyle);
            replacement.setName(layer.getName());
            source.attachTo(replacement);
            return replacement;
        }
        GridCoverageNwwLayer replacement = new GridCoverageNwwLayer(source.file(), null, Color.WHITE, style);
        replacement.setName(layer.getName());
        source.attachTo(replacement);
        return replacement;
    }

    /**
     * Save the style of a layer in its data: in the geopackage table, or in the .sld file next to
     * the shapefile or raster file, which is replaced. The styles of the tables of database
     * connections and of the web service layers are saved in the {@link LocalStyleStore}.
     * The style is used when the data are loaded again.
     *
     * @param source the source of the layer, a shapefile, a geopackage table, a table of a
     *          database connection or a raster file.
     * @param style the style.
     * @return the file written: the geopackage or the .sld file.
     * @throws Exception if the style can't be written.
     */
    public static File saveStyle( LayerSource source, Style style ) throws Exception {
        if (LocalStyleStore.handles(source)) {
            return LocalStyleStore.save(source, style);
        }
        String sld = SldUtilities.styleToString(style);
        if (source.isGeopackageTable()) {
            try (GeopackageCommonDb db = new GeopackageDb()) {
                db.open(source.file().getAbsolutePath());
                db.updateSldStyle(SqlName.m(source.table()), sld);
            }
            return source.file();
        } else if (source.isShapefile() || isRasterFile(source.file())) {
            File sldFile = new File(source.file().getParentFile(),
                    FileUtilities.getNameWithoutExtention(source.file()) + "." + SldUtilities.SLD_EXTENSION);
            FileUtilities.writeFile(sld, sldFile);
            return sldFile;
        }
        throw new IllegalArgumentException(
                "The style can be saved only for shapefiles, geopackage tables and raster files: " + source.path());
    }

    /**
     * @param file a file, or <code>null</code>.
     * @return <code>true</code> for the raster files loaded as {@link GridCoverageNwwLayer}:
     *          GeoTIFF, ESRI ASCII grids and GDAL VRTs, read as image mosaics.
     */
    public static boolean isRasterFile( File file ) {
        if (file == null) {
            return false;
        }
        String lowerName = file.getName().toLowerCase();
        return lowerName.endsWith(".asc") || lowerName.endsWith(".tiff") || lowerName.endsWith(".tif")
                || lowerName.endsWith(".vrt");
    }

    /**
     * @return the values of the fields given in the .codes file next to a shapefile, or
     *          <code>null</code> if there is none.
     */
    private static HashMap<String, String[]> readCodes( File shapeFile ) throws Exception {
        File codesFile = new File(shapeFile.getParentFile(), FileUtilities.getNameWithoutExtention(shapeFile) + ".codes");
        if (!codesFile.exists()) {
            return null;
        }
        HashMap<String, String[]> field2ValuesMap = new HashMap<>();
        for( String line : FileUtilities.readFileToLinesList(codesFile) ) {
            String[] split = line.split("=");
            field2ValuesMap.put(split[0], split[1].split(";"));
        }
        return field2ValuesMap;
    }

    /**
     * @param rasterized if <code>true</code>, a layer drawn as images with the full SLD style, else
     *          a layer of 3D shapes with one simple style.
     * @return the layer, not added to the viewer, or <code>null</code> for an unknown geometry
     *          type.
     */
    private Layer createFeatureCollectionLayer( boolean rasterized, File selectedFile, String name,
            SimpleFeatureStore featureStore, SimpleFeatureCollection readFC, HashMap<String, String[]> field2ValuesMap,
            Style sldStyle, SimpleStyle simpleStyle ) throws Exception {
        if (rasterized) {
            Style style = sldStyle;
            if (style == null && selectedFile != null)
                style = SldUtilities.getStyleFromFile(selectedFile);
            if (style == null)
                style = createDefaultStyle(readFC.getSchema(), simpleStyle != null ? simpleStyle.strokeColor : nextDefaultColor());
            RasterizedFeatureCollectionLayer collectionLayer = new RasterizedFeatureCollectionLayer(name, readFC, style, null,
                    true);
            // to edit the attributes of the identified features
            collectionLayer.setFeatureStore(featureStore);
            // kept to draw the layer again with the same style
            collectionLayer.setValue(RASTERIZED_STYLE_KEY, style);
            return collectionLayer;
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
                applySldStyle(featureCollectionPolygonLayer, sldStyle, simpleStyle, selectedFile);
                return featureCollectionPolygonLayer;
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
                applySldStyle(featureCollectionLinesLayer, sldStyle, simpleStyle, selectedFile);
                return featureCollectionLinesLayer;
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
                applySldStyle(featureCollectionPointsLayer, sldStyle, simpleStyle, selectedFile);
                return featureCollectionPointsLayer;
            } else {
                System.err.println("?????");
                return null;
            }
        }
    }

    /**
     * Create the style of a layer that has none, with the colors as hex literals, which the
     * panels of the style editor can read, unlike the colors of {@link SLD#createSimpleStyle}.
     *
     * @param schema the type of the features.
     * @param color the color of the features, polygons get a darker border.
     * @return the style.
     */
    private static Style createDefaultStyle( SimpleFeatureType schema, Color color ) {
        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
        EGeometryType geometryType;
        if (EGeometryType.isPoint(geometryDescriptor)) {
            geometryType = EGeometryType.POINT;
        } else if (EGeometryType.isLine(geometryDescriptor)) {
            geometryType = EGeometryType.LINESTRING;
        } else if (EGeometryType.isPolygon(geometryDescriptor)) {
            geometryType = EGeometryType.POLYGON;
        } else {
            return SLD.createSimpleStyle(schema, color);
        }
        Rule rule = StyleUtilities.createDefaultRule(geometryType);
        StyleUtilities.setRuleColor(rule, color);
        if (geometryType == EGeometryType.POLYGON) {
            for( Symbolizer symbolizer : rule.symbolizers() ) {
                if (symbolizer instanceof PolygonSymbolizer && ((PolygonSymbolizer) symbolizer).getStroke() != null) {
                    ((PolygonSymbolizer) symbolizer).getStroke()
                            .setColor(StyleUtilities.ff.literal(ColorUtilities.asHex(color.darker())));
                }
            }
        }
        return StyleUtilities.rulesToStyle(List.of(rule), schema.getTypeName(), false);
    }

    /**
     * Draw a 3D layer with an SLD style: the given one, else, if no simple style is given either,
     * the one of the .sld file next to the data. Without SLD style the layer keeps its simple
     * style.
     */
    private static void applySldStyle( NwwVectorLayer layer, Style sldStyle, SimpleStyle simpleStyle, File selectedFile ) {
        Style style = sldStyle;
        if (style == null && simpleStyle == null && selectedFile != null) {
            style = SldUtilities.getStyleFromFile(selectedFile);
        }
        if (style != null) {
            layer.setSldStyle(style);
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
}
