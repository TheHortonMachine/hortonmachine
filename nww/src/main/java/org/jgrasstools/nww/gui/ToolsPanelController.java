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
package org.jgrasstools.nww.gui;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Date;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.jgrasstools.gears.spatialite.RL2CoverageHandler;
import org.jgrasstools.gears.spatialite.RasterCoverage;
import org.jgrasstools.gears.spatialite.SpatialiteDb;
import org.jgrasstools.gears.spatialite.SpatialiteGeometryColumns;
import org.jgrasstools.gears.spatialite.SpatialiteGeometryType;
import org.jgrasstools.gears.utils.SldUtilities;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryType;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.jgrasstools.nww.gui.listeners.GenericSelectListener;
import org.jgrasstools.nww.gui.style.SimpleStyle;
import org.jgrasstools.nww.layers.defaults.annotations.HtmlScreenAnnotation;
import org.jgrasstools.nww.layers.defaults.annotations.HtmlScreenAnnotation.Builder;
import org.jgrasstools.nww.layers.defaults.other.CurrentGpsPointLayer;
import org.jgrasstools.nww.layers.defaults.other.SimplePointsLayer;
import org.jgrasstools.nww.layers.defaults.other.WhiteNwwLayer;
import org.jgrasstools.nww.layers.defaults.raster.ImageMosaicNwwLayer;
import org.jgrasstools.nww.layers.defaults.raster.MBTilesNwwLayer;
import org.jgrasstools.nww.layers.defaults.raster.MapsforgeNwwLayer;
import org.jgrasstools.nww.layers.defaults.raster.RL2NwwLayer;
import org.jgrasstools.nww.layers.defaults.spatialite.RasterizedSpatialiteLayer;
import org.jgrasstools.nww.layers.defaults.spatialite.SpatialiteLinesLayer;
import org.jgrasstools.nww.layers.defaults.spatialite.SpatialitePointsLayer;
import org.jgrasstools.nww.layers.defaults.spatialite.SpatialitePolygonLayer;
import org.jgrasstools.nww.layers.defaults.vector.FeatureCollectionLinesLayer;
import org.jgrasstools.nww.layers.defaults.vector.FeatureCollectionPointsLayer;
import org.jgrasstools.nww.layers.defaults.vector.FeatureCollectionPolygonLayer;
import org.jgrasstools.nww.layers.defaults.vector.RasterizedFeatureCollectionLayer;
import org.jgrasstools.nww.layers.defaults.vector.RasterizedShapefilesFolderNwwLayer;
import org.jgrasstools.nww.layers.defaults.vector.ShapefilesFolderLayer;
import org.jgrasstools.nww.utils.CursorUtils;
import org.jgrasstools.nww.utils.EGlobeModes;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.jgrasstools.nww.utils.cache.CacheUtils;
import org.jgrasstools.nww.utils.selection.ObjectsOnScreenByBoxSelector;
import org.jgrasstools.nww.utils.selection.SectorByBoxSelector;
import org.opengis.feature.type.GeometryDescriptor;

import com.vividsolutions.jts.geom.Geometry;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

/**
 * The tools panel.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ToolsPanelController extends ToolsPanelView {

    private GenericSelectListener genericSelectListener;

    private WhiteNwwLayer whiteLayer = new WhiteNwwLayer();

    public ToolsPanelController(final NwwPanel wwjPanel, LayerEventsListener layerEventsListener) {

        String[] supportedExtensions = NwwUtilities.SUPPORTED_EXTENSIONS;
        StringBuilder sb = new StringBuilder();
        for (String ext : supportedExtensions) {
            sb.append(",*.").append(ext);
        }
        final String desc = sb.substring(1);

        _loadGpsButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            FileFilter fileFilter = new FileFilter() {

                @Override
                public String getDescription() {
                    return "*.shp";
                }

                @Override
                public boolean accept(File f) {
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
            fileChooser.setCurrentDirectory(GuiUtilities.getLastFile());
            int result = fileChooser.showOpenDialog((Component) wwjPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                GuiUtilities.setLastPath(selectedFile.getAbsolutePath());

                CurrentGpsPointLayer currentGpsPointLayer = new CurrentGpsPointLayer(null, null, null, null, null);
                SimplePointsLayer simplePointsLayer = new SimplePointsLayer("GPS Points");
                simplePointsLayer.setMaxMarkers(10);

                wwjPanel.getWwd().getModel().getLayers().add(currentGpsPointLayer);
                layerEventsListener.onLayerAdded(currentGpsPointLayer);
                wwjPanel.getWwd().getModel().getLayers().add(simplePointsLayer);
                layerEventsListener.onLayerAdded(simplePointsLayer);

                try {
                    new org.jgrasstools.nww.utils.FakeGps(selectedFile, wwjPanel, currentGpsPointLayer,
                            simplePointsLayer);
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }
        });

        _loadFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
            FileFilter fileFilter = new FileFilter() {

                @Override
                public String getDescription() {
                    return desc;
                }

                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    String name = f.getName();
                    for (String ext : supportedExtensions) {
                        if (name.endsWith(ext)) {
                            return true;
                        }
                    }
                    return false;
                }
            };
            fileChooser.setFileFilter(fileFilter);
            fileChooser.setCurrentDirectory(GuiUtilities.getLastFile());
            int result = fileChooser.showOpenDialog((Component) wwjPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                if (selectedFile.isDirectory()) {
                    try {
                        Layer layer;
                        if (_useRasterizedCheckbox.isSelected()) {
                            layer = new RasterizedShapefilesFolderNwwLayer(null, selectedFile, null, true);
                        } else {
                            layer = new ShapefilesFolderLayer(selectedFile.getAbsolutePath());
                        }
                        wwjPanel.getWwd().getModel().getLayers().add(layer);
                        layerEventsListener.onLayerAdded(layer);
                    } catch (Exception e1) {
                        e1.printStackTrace();
                        // try to load it as single files
                        File[] listFiles = selectedFile.listFiles(new FilenameFilter() {

                            @Override
                            public boolean accept(File dir, String name) {
                                for (String ext : supportedExtensions) {
                                    if (name.endsWith(ext)) {
                                        return true;
                                    }
                                }
                                return false;
                            }
                        });
                        for (File file : listFiles) {
                            GuiUtilities.setLastPath(file.getAbsolutePath());
                            loadFile(wwjPanel, layerEventsListener, file);
                        }
                    }

                } else {
                    GuiUtilities.setLastPath(selectedFile.getAbsolutePath());
                    loadFile(wwjPanel, layerEventsListener, selectedFile);
                }
            }
        });

        String[] names = EGlobeModes.getModesDescriptions();
        _globeModeCombo.setModel(new DefaultComboBoxModel<String>(names));
        _globeModeCombo.setSelectedItem(names[0]);
        _globeModeCombo.addActionListener(e -> {
            String selected = _globeModeCombo.getSelectedItem().toString();
            EGlobeModes modeFromDescription = EGlobeModes.getModeFromDescription(selected);
            switch (modeFromDescription) {
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

        final ObjectsOnScreenByBoxSelector byBoxSelector = new ObjectsOnScreenByBoxSelector(wwjPanel.getWwd());
        byBoxSelector.addListener(new ObjectsOnScreenByBoxSelector.IBoxScreenSelectionListener() {

            @Override
            public void onSelectionFinished(Geometry selectedArea, List<?> selectedObjs) {
                int size = selectedObjs.size();
                JOptionPane.showMessageDialog(wwjPanel,
                        "Selected objects: " + size + "\nin region:\n" + selectedArea.toText());
            }
        });
        _selectByBoxButton.addActionListener(e -> {
            // _infoButton.setSelected(false);

            if (_selectByBoxButton.isSelected()) {
                byBoxSelector.enable();
                CursorUtils.makeCrossHair(wwjPanel.getWwd());
            } else {
                byBoxSelector.disable();
                CursorUtils.makeDefault(wwjPanel.getWwd());
            }
        });
        final SectorByBoxSelector zoomBoxSelector = new SectorByBoxSelector(wwjPanel.getWwd());
        zoomBoxSelector.addListener(new SectorByBoxSelector.IBoxSelectionListener() {

            @Override
            public void onSelectionFinished(Sector selectedSector) {
                wwjPanel.goTo(selectedSector, false);
                zoomBoxSelector.disable();
                zoomBoxSelector.enable();
            }
        });
        _zoomByBoxButton.addActionListener(e -> {
            // _infoButton.setSelected(false);

            if (_zoomByBoxButton.isSelected()) {
                zoomBoxSelector.enable();
                CursorUtils.makeCrossHair(wwjPanel.getWwd());
            } else {
                zoomBoxSelector.disable();
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

            String text = JOptionPane.showInputDialog(
                    "Enter the annotation's position and html text as: width,y position,text", htmldefaultText);
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

            AnnotationLayer layer = new AnnotationLayer() {
                @Override
                public String toString() {
                    return layerName;
                }
            };
            layer.addAnnotation(htmlScreenAnnotation);

            new Thread(new Runnable() {
                public void run() {
                    while (wwjPanel.getWwd().getModel().getLayers().contains(layer)) {
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

    private void loadFile(final NwwPanel wwjPanel, LayerEventsListener layerEventsListener, File selectedFile) {

        String name = FileUtilities.getNameWithoutExtention(selectedFile);
        try {
            if (selectedFile.getName().endsWith(".shp")) {
                // shp or image mosaic?

                String parentFolderName = selectedFile.getParentFile().getName();
                String fileName = FileUtilities.getNameWithoutExtention(selectedFile);
                try {
                    if (parentFolderName.equals(fileName)) {
                        ImageMosaicNwwLayer imageMosaicNwwLayer = new ImageMosaicNwwLayer(selectedFile, null);
                        wwjPanel.getWwd().getModel().getLayers().add(imageMosaicNwwLayer);
                        layerEventsListener.onLayerAdded(imageMosaicNwwLayer);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // ignore and handle as shapefile
                }

                SimpleFeatureSource featureSource = NwwUtilities.readFeatureSource(selectedFile.getAbsolutePath());
                SimpleFeatureStore featureStore = null;
                if (featureSource instanceof SimpleFeatureStore) {
                    featureStore = (SimpleFeatureStore) featureSource;
                }
                SimpleFeatureCollection readFC = NwwUtilities.readAndReproject(featureSource);
                if (_useRasterizedCheckbox.isSelected()) {
                    Style style = SldUtilities.getStyleFromFile(selectedFile);
                    if (style == null)
                        style = SLD.createSimpleStyle(readFC.getSchema());
                    RasterizedFeatureCollectionLayer collectionLayer = new RasterizedFeatureCollectionLayer(name,
                            readFC, style, null, true);

                    wwjPanel.getWwd().getModel().getLayers().add(collectionLayer);
                    layerEventsListener.onLayerAdded(collectionLayer);
                } else {

                    GeometryDescriptor geometryDescriptor = readFC.getSchema().getGeometryDescriptor();
                    if (GeometryUtilities.isPolygon(geometryDescriptor)) {
                        FeatureCollectionPolygonLayer featureCollectionPolygonLayer = new FeatureCollectionPolygonLayer(
                                name, readFC, featureStore);

                        featureCollectionPolygonLayer.setElevationMode(WorldWind.RELATIVE_TO_GROUND);
                        featureCollectionPolygonLayer.setExtrusionProperties(5.0, null, null, true);
                        SimpleStyle style = NwwUtilities.getStyle(selectedFile.getAbsolutePath(), GeometryType.POLYGON);
                        if (style != null) {
                            featureCollectionPolygonLayer.setStyle(style);
                        }

                        wwjPanel.getWwd().getModel().getLayers().add(featureCollectionPolygonLayer);
                        layerEventsListener.onLayerAdded(featureCollectionPolygonLayer);
                    } else if (GeometryUtilities.isLine(geometryDescriptor)) {
                        FeatureCollectionLinesLayer featureCollectionLinesLayer = new FeatureCollectionLinesLayer(name,
                                readFC, featureStore);
                        featureCollectionLinesLayer.setElevationMode(WorldWind.RELATIVE_TO_GROUND);
                        featureCollectionLinesLayer.setExtrusionProperties(5.0, null, null, true);
                        SimpleStyle style = NwwUtilities.getStyle(selectedFile.getAbsolutePath(), GeometryType.LINE);
                        if (style != null) {
                            featureCollectionLinesLayer.setStyle(style);
                        }

                        wwjPanel.getWwd().getModel().getLayers().add(featureCollectionLinesLayer);
                        layerEventsListener.onLayerAdded(featureCollectionLinesLayer);
                    } else if (GeometryUtilities.isPoint(geometryDescriptor)) {
                        // check if there is an image
                        File imageFile = new File(selectedFile.getParentFile(), name + ".png");
                        String imagePath = null;
                        if (imageFile.exists()) {
                            imagePath = imageFile.getAbsolutePath();
                        }
                        FeatureCollectionPointsLayer featureCollectionPointsLayer = new FeatureCollectionPointsLayer(
                                name, readFC, featureStore, imagePath);
                        SimpleStyle style = NwwUtilities.getStyle(selectedFile.getAbsolutePath(), GeometryType.POINT);
                        if (style != null) {
                            featureCollectionPointsLayer.setStyle(style);
                        }

                        wwjPanel.getWwd().getModel().getLayers().add(featureCollectionPointsLayer);
                        layerEventsListener.onLayerAdded(featureCollectionPointsLayer);

                    } else {
                        System.err.println("?????");
                    }
                }
            } else if (selectedFile.getName().endsWith(".mbtiles")) {
                MBTilesNwwLayer mbTileLayer = new MBTilesNwwLayer(selectedFile);
                wwjPanel.getWwd().getModel().getLayers().add(mbTileLayer);
                layerEventsListener.onLayerAdded(mbTileLayer);
            } else if (selectedFile.getName().endsWith(".map")) {
                MapsforgeNwwLayer mbTileLayer = new MapsforgeNwwLayer(selectedFile, null);
                wwjPanel.getWwd().getModel().getLayers().add(mbTileLayer);
                layerEventsListener.onLayerAdded(mbTileLayer);
            } else if (selectedFile.getName().endsWith(".rl2")) {
                SpatialiteDb db = new SpatialiteDb();
                db.open(selectedFile.getAbsolutePath());
                List<RasterCoverage> rasterCoverages = db.getRasterCoverages(false);
                if (rasterCoverages.size() > 0) {
                    RL2CoverageHandler handler = new RL2CoverageHandler(db, rasterCoverages.get(0));
                    RL2NwwLayer rl2Layer = new RL2NwwLayer(handler, null);

                    wwjPanel.getWwd().getModel().getLayers().add(rl2Layer);
                    layerEventsListener.onLayerAdded(rl2Layer);
                }
            } else if (selectedFile.getName().endsWith(".sqlite")) {
                SpatialiteDb db = new SpatialiteDb();
                db.open(selectedFile.getAbsolutePath());
                List<String> tableMaps = db.getTables(false);
                String[] tables = tableMaps.toArray(new String[0]);
                String tableName = (String) JOptionPane.showInputDialog(this, "Select the table to load",
                        "Table selection", JOptionPane.QUESTION_MESSAGE, null, tables, tables[0]);

                if (_useRasterizedCheckbox.isSelected()) {
                    RasterizedSpatialiteLayer rasterizedSpatialiteLayer = new RasterizedSpatialiteLayer(name, db,
                            tableName, -1, null, null, true);
                    wwjPanel.getWwd().getModel().getLayers().add(rasterizedSpatialiteLayer);
                    layerEventsListener.onLayerAdded(rasterizedSpatialiteLayer);
                } else {
                    SpatialiteGeometryColumns geometryColumn = db.getGeometryColumnsForTable(tableName);
                    if (geometryColumn != null) {
                        SpatialiteGeometryType geomType = SpatialiteGeometryType.forValue(geometryColumn.geometry_type);
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
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}
