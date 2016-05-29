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
import java.awt.Cursor;
import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.spatialite.RL2CoverageHandler;
import org.jgrasstools.gears.spatialite.RasterCoverage;
import org.jgrasstools.gears.spatialite.SpatialiteDb;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.gui.utils.GuiUtilities;
import org.jgrasstools.nww.gui.listeners.GenericSelectListener;
import org.jgrasstools.nww.layers.defaults.FeatureCollectionLinesLayer;
import org.jgrasstools.nww.layers.defaults.FeatureCollectionPointsLayer;
import org.jgrasstools.nww.layers.defaults.FeatureCollectionPolygonLayer;
import org.jgrasstools.nww.layers.defaults.MBTilesNwwLayer;
import org.jgrasstools.nww.layers.defaults.MapsforgeNwwLayer;
import org.jgrasstools.nww.layers.defaults.RL2NwwLayer;
import org.jgrasstools.nww.utils.CursorUtils;
import org.jgrasstools.nww.utils.EGlobeModes;
import org.jgrasstools.nww.utils.NwwUtilities;
import org.opengis.feature.type.GeometryDescriptor;

import gov.nasa.worldwind.WorldWind;

/**
 * The tools panel.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ToolsPanelController extends ToolsPanelView {

    private GenericSelectListener genericSelectListener;

    public ToolsPanelController(final NwwPanel wwjPanel, LayerEventsListener layerEventsListener) {

        String[] supportedExtensions = NwwUtilities.SUPPORTED_EXTENSIONS;
        StringBuilder sb = new StringBuilder();
        for (String ext : supportedExtensions) {
            sb.append(",*.").append(ext);
        }
        final String desc = sb.substring(1);

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

        _selectByBoxButton.addActionListener(e -> {
            _infoButton.setSelected(false);

            if (_selectByBoxButton.isSelected()) {
                genericSelectListener = new GenericSelectListener(wwjPanel);
                wwjPanel.getWwd().addSelectListener(genericSelectListener);
                CursorUtils.makeCrossHair(wwjPanel.getWwd());
            } else {
                if (genericSelectListener != null) {
                    wwjPanel.getWwd().removeSelectListener(genericSelectListener);
                }
                genericSelectListener = null;
                CursorUtils.makeDefault(wwjPanel.getWwd());
            }
        });

        _infoButton.addActionListener(e -> {
            _selectByBoxButton.setSelected(false);

            if (_infoButton.isSelected()) {
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

    }

    private void loadFile(final NwwPanel wwjPanel, LayerEventsListener layerEventsListener, File selectedFile) {

        String name = FileUtilities.getNameWithoutExtention(selectedFile);
        try {
            if (selectedFile.getName().endsWith(".shp")) {
                SimpleFeatureCollection readFC = NwwUtilities.readAndReproject(selectedFile.getAbsolutePath());
                GeometryDescriptor geometryDescriptor = readFC.getSchema().getGeometryDescriptor();
                if (GeometryUtilities.isPolygon(geometryDescriptor)) {
                    FeatureCollectionPolygonLayer featureCollectionPolygonLayer =
                        new FeatureCollectionPolygonLayer(name, readFC);

                    featureCollectionPolygonLayer.setElevationMode(WorldWind.RELATIVE_TO_GROUND);
                    featureCollectionPolygonLayer.setExtrusionProperties(5.0, null, null, true);

                    wwjPanel.getWwd().getModel().getLayers().add(featureCollectionPolygonLayer);
                    layerEventsListener.onLayerAdded(featureCollectionPolygonLayer);
                } else if (GeometryUtilities.isLine(geometryDescriptor)) {
                    FeatureCollectionLinesLayer featureCollectionLinesLayer =
                        new FeatureCollectionLinesLayer(name, readFC);
                    featureCollectionLinesLayer.setElevationMode(WorldWind.RELATIVE_TO_GROUND);
                    featureCollectionLinesLayer.setExtrusionProperties(5.0, null, null, true);
                    wwjPanel.getWwd().getModel().getLayers().add(featureCollectionLinesLayer);
                    layerEventsListener.onLayerAdded(featureCollectionLinesLayer);
                } else if (GeometryUtilities.isPoint(geometryDescriptor)) {
                    FeatureCollectionPointsLayer featureCollectionPointsLayer =
                        new FeatureCollectionPointsLayer(name, readFC);
                    wwjPanel.getWwd().getModel().getLayers().add(featureCollectionPointsLayer);
                    layerEventsListener.onLayerAdded(featureCollectionPointsLayer);

                } else {
                    System.err.println("?????");
                }
            } else if (selectedFile.getName().endsWith(".mbtiles")) {
                MBTilesNwwLayer mbTileLayer = new MBTilesNwwLayer(selectedFile);
                wwjPanel.getWwd().getModel().getLayers().add(mbTileLayer);
                layerEventsListener.onLayerAdded(mbTileLayer);
            } else if (selectedFile.getName().endsWith(".map")) {
                MapsforgeNwwLayer mbTileLayer = new MapsforgeNwwLayer(selectedFile);
                wwjPanel.getWwd().getModel().getLayers().add(mbTileLayer);
                layerEventsListener.onLayerAdded(mbTileLayer);
            } else if (selectedFile.getName().endsWith(".rl2")) {
                SpatialiteDb db = new SpatialiteDb();
                db.open(selectedFile.getAbsolutePath());
                List<RasterCoverage> rasterCoverages = db.getRasterCoverages(false);
                if (rasterCoverages.size() > 0) {
                    RL2CoverageHandler handler = new RL2CoverageHandler(db, rasterCoverages.get(0));
                    RL2NwwLayer rl2Layer = new RL2NwwLayer(handler);

                    wwjPanel.getWwd().getModel().getLayers().add(rl2Layer);
                    layerEventsListener.onLayerAdded(rl2Layer);
                }
            }
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

}
