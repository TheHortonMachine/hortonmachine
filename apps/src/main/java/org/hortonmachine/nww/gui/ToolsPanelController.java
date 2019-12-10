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

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.TileEntry;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.rasterlite.Rasterlite2Coverage;
import org.hortonmachine.dbs.rasterlite.Rasterlite2Db;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.spatialite.GTSpatialiteThreadsafeDb;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.nww.gui.listeners.GenericSelectListener;
import org.hortonmachine.nww.layers.defaults.annotations.HtmlScreenAnnotation;
import org.hortonmachine.nww.layers.defaults.annotations.HtmlScreenAnnotation.Builder;
import org.hortonmachine.nww.layers.defaults.other.CurrentGpsPointLayer;
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
import org.hortonmachine.nww.layers.defaults.vector.GeopackageVectorLayer;
import org.hortonmachine.nww.layers.defaults.vector.RasterizedFeatureCollectionLayer;
import org.hortonmachine.nww.layers.defaults.vector.RasterizedShapefilesFolderNwwLayer;
import org.hortonmachine.nww.layers.defaults.vector.ShapefilesFolderLayer;
import org.hortonmachine.nww.utils.CursorUtils;
import org.hortonmachine.nww.utils.EGlobeModes;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.nww.utils.cache.CacheUtils;
import org.hortonmachine.nww.utils.selection.ObjectsOnScreenByBoxSelector;
import org.hortonmachine.style.SimpleStyle;
import org.hortonmachine.style.SimpleStyleUtilities;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.Sector;
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

    private GenericSelectListener genericSelectListener;

    private WhiteNwwLayer whiteLayer = new WhiteNwwLayer();

    private NwwPanel wwjPanel;

    private LayerEventsListener layerEventsListener;

    @SuppressWarnings("unchecked")
    public ToolsPanelController( final NwwPanel wwjPanel, LayerEventsListener layerEventsListener ) {
        this.wwjPanel = wwjPanel;
        this.layerEventsListener = layerEventsListener;

        String[] supportedExtensions = NwwUtilities.SUPPORTED_EXTENSIONS;
        StringBuilder sb = new StringBuilder();
        for( String ext : supportedExtensions ) {
            sb.append(",*.").append(ext);
        }
        final String desc = sb.substring(1);

        _loadGpsButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FileFilter fileFilter = new FileFilter(){

                @Override
                public String getDescription() {
                    return "*.shp";
                }

                @Override
                public boolean accept( File f ) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    String name = f.getName();
                    if (name.endsWith("shp")) {
                        return true;
                    }
                    return false;
                }
            };
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setCurrentDirectory(PreferencesHandler.getLastFile());
            int result = fileChooser.showOpenDialog((Component) wwjPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                PreferencesHandler.setLastPath(selectedFile.getAbsolutePath());

                CurrentGpsPointLayer currentGpsPointLayer = new CurrentGpsPointLayer(null, null, null, null, null);
                SimplePointsLayer simplePointsLayer = new SimplePointsLayer("GPS Points");
                simplePointsLayer.setMaxMarkers(10);

                wwjPanel.getWwd().getModel().getLayers().add(currentGpsPointLayer);
                layerEventsListener.onLayerAdded(currentGpsPointLayer);
                wwjPanel.getWwd().getModel().getLayers().add(simplePointsLayer);
                layerEventsListener.onLayerAdded(simplePointsLayer);

                try {
                    new org.hortonmachine.nww.utils.FakeGps(selectedFile, wwjPanel, currentGpsPointLayer, simplePointsLayer);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
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
                if (selectedFiles != null && selectedFiles.length == 1 && selectedFiles[0].isDirectory()) {
                    try {
                        Layer layer;
                        if (_useRasterizedCheckbox.isSelected()) {
                            layer = new RasterizedShapefilesFolderNwwLayer(null, selectedFiles[0], null, true);
                        } else {
                            layer = new ShapefilesFolderLayer(selectedFiles[0].getAbsolutePath());
                        }
                        wwjPanel.getWwd().getModel().getLayers().add(layer);
                        layerEventsListener.onLayerAdded(layer);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        // try to load it as single files
                        File[] listFiles = selectedFiles[0].listFiles(new FilenameFilter(){

                            @Override
                            public boolean accept( File dir, String name ) {
                                for( String ext : supportedExtensions ) {
                                    if (name.endsWith(ext)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });
                        for( File file : listFiles ) {
                            PreferencesHandler.setLastPath(file.getAbsolutePath());
                            loadFile(file);
                        }
                    }

                } else {
                    if (selectedFiles != null && selectedFiles.length > 0) {
                        PreferencesHandler.setLastPath(selectedFiles[0].getAbsolutePath());
                        if (selectedFiles.length == 1) {
                            loadFile(selectedFiles[0]);
                        } else {
                            if (selectedFiles[0].getName().endsWith(".map")) {
                                try {
                                    String layerName = selectedFiles[0].getParentFile().getName() + "-maps";
                                    MapsforgeNwwLayer mbTileLayer = new MapsforgeNwwLayer(layerName, selectedFiles, null, null);
                                    wwjPanel.getWwd().getModel().getLayers().add(mbTileLayer);
                                    layerEventsListener.onLayerAdded(mbTileLayer);
                                } catch (Exception e1) {
                                    e1.printStackTrace();
                                }
                            }
                        }
                    }
                }
            }
        });

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
            switch( modeFromDescription ) {
            case FlatEarth:
                wwjPanel.setFlatGlobe(false);
                break;
            case FlatEarthMercator:
                wwjPanel.setFlatGlobe(true);
                break;
            case Earth:
            default:
                wwjPanel.setSphereGlobe();
                break;
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

    public NwwPanel getWwjPanel() {
        return wwjPanel;
    }

    private void loadFile( File selectedFile ) {
        String name = FileUtilities.getNameWithoutExtention(selectedFile);
        try {
            if (selectedFile.getName().endsWith(".asc") || selectedFile.getName().endsWith(".tiff")) {
                GridCoverageNwwLayer coverageNwwLayer = new GridCoverageNwwLayer(selectedFile, null, Color.WHITE);
                wwjPanel.getWwd().getModel().getLayers().add(coverageNwwLayer);
                layerEventsListener.onLayerAdded(coverageNwwLayer);
            } else if (selectedFile.getName().endsWith(".shp")) {
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
                        final ParameterValue<Boolean> usejai = ImageMosaicFormat.USE_JAI_IMAGEREAD.createValue();
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
            } else if (selectedFile.getName().endsWith(".mbtiles")) {
                MBTilesNwwLayer mbTileLayer = new MBTilesNwwLayer(selectedFile);
                wwjPanel.getWwd().getModel().getLayers().add(mbTileLayer);
                layerEventsListener.onLayerAdded(mbTileLayer);
            } else if (selectedFile.getName().endsWith(".gpkg")) {
                List<String> tilesTables = new ArrayList<>();
                List<String> featureTables = new ArrayList<>();
                try (GeopackageCommonDb db = new GeopackageDb()) {
                    db.open(selectedFile.getAbsolutePath());
                    List<TileEntry> tiles = db.tiles();
                    for( TileEntry tileEntry : tiles ) {
                        tilesTables.add(tileEntry.getTableName());
                    }
                    List<FeatureEntry> features = db.features();
                    for( FeatureEntry featureEntry : features ) {
                        featureTables.add(featureEntry.getTableName());
                    }
                }
                for( String tableName : tilesTables ) {
                    GeopackageTilesNwwLayer gpkgTilesLayer = new GeopackageTilesNwwLayer(selectedFile, tableName);
                    wwjPanel.getWwd().getModel().getLayers().add(gpkgTilesLayer);
                    layerEventsListener.onLayerAdded(gpkgTilesLayer);
                }

                if (_useRasterizedCheckbox.isSelected()) {
                    for( String tableName : featureTables ) {
                        SimpleFeatureCollection readFC = OmsVectorReader
                                .readVector(selectedFile.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + tableName);
                        String sldString = null;
                        try (GeopackageCommonDb db = new GeopackageDb()) {
                            db.open(selectedFile.getAbsolutePath());
                            sldString = db.getSldString(tableName);
                        }
                        Style style;
                        if (sldString != null) {
                            style = SldUtilities.getStyleFromSldString(sldString);
                        } else {
                            style = SLD.createSimpleStyle(readFC.getSchema(), Color.BLUE);
                        }
                        String n = selectedFile.getName() + HMConstants.DB_TABLE_PATH_SEPARATOR + tableName;
                        RasterizedFeatureCollectionLayer collectionLayer = new RasterizedFeatureCollectionLayer(n, readFC, style,
                                null, true);

                        wwjPanel.getWwd().getModel().getLayers().add(collectionLayer);
                        layerEventsListener.onLayerAdded(collectionLayer);
                    }
                } else {
                    for( String tableName : featureTables ) {
                        GeopackageVectorLayer gpkgVectorLayer = new GeopackageVectorLayer(selectedFile.getAbsolutePath(),
                                tableName);
                        wwjPanel.getWwd().getModel().getLayers().add(gpkgVectorLayer);
                        layerEventsListener.onLayerAdded(gpkgVectorLayer);
                    }
                }

            } else if (selectedFile.getName().endsWith(".map")) {
                String layerName = FileUtilities.getNameWithoutExtention(selectedFile);
                MapsforgeNwwLayer mbTileLayer = new MapsforgeNwwLayer(layerName, new File[]{selectedFile}, null, null);
                wwjPanel.getWwd().getModel().getLayers().add(mbTileLayer);
                layerEventsListener.onLayerAdded(mbTileLayer);
            } else if (selectedFile.getName().endsWith(".rl2")) {
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
            } else if (selectedFile.getName().endsWith(".sqlite")) {
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
                    List<String> tableMaps = db.getTables(false);
                    String[] tables = tableMaps.toArray(new String[0]);
                    String tableName = (String) JOptionPane.showInputDialog(this, "Select the table to load", "Table selection",
                            JOptionPane.QUESTION_MESSAGE, null, tables, tables[0]);

                    if (_useRasterizedCheckbox.isSelected()) {
                        RasterizedSpatialiteLayer rasterizedSpatialiteLayer = new RasterizedSpatialiteLayer(name, db, tableName,
                                -1, null, null, true);
                        wwjPanel.getWwd().getModel().getLayers().add(rasterizedSpatialiteLayer);
                        layerEventsListener.onLayerAdded(rasterizedSpatialiteLayer);
                    } else {
                        GeometryColumn geometryColumn = db.getGeometryColumnsForTable(tableName);
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
        if (_useRasterizedCheckbox.isSelected()) {
            Style style = null;
            if (selectedFile != null)
                style = SldUtilities.getStyleFromFile(selectedFile);
            if (style == null)
                style = SLD.createSimpleStyle(readFC.getSchema(), Color.BLUE);
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
                SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(absolutePath, EGeometryType.POLYGON.name());
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
                SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(absolutePath, EGeometryType.LINESTRING.name());
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
                SimpleStyle style = SimpleStyleUtilities.getSimpleStyle(absolutePath, EGeometryType.POINT.name());
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

}
