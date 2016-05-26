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
import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.utils.files.FileUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.jgrasstools.nww.layers.defaults.FeatureCollectionLinesLayer;
import org.jgrasstools.nww.layers.defaults.FeatureCollectionPointsLayer;
import org.jgrasstools.nww.layers.defaults.FeatureCollectionPolygonLayer;
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

    public ToolsPanelController(NwwPanel wwjPanel, LayerEventsListener layerEventsListener) {

        _loadFileButton.addActionListener(e -> {
            JFileChooser fileChooser = new JFileChooser();
            fileChooser.setFileFilter(new FileFilter() {

                @Override
                public String getDescription() {
                    return ".shp";
                }

                @Override
                public boolean accept(File f) {
                    if (f.isDirectory()) {
                        return true;
                    }
                    String name = f.getName();
                    return name.endsWith(".shp");
                }
            });
            fileChooser.setCurrentDirectory(new File(System.getProperty("user.home")));
            int result = fileChooser.showOpenDialog((Component) wwjPanel);
            if (result == JFileChooser.APPROVE_OPTION) {
                File selectedFile = fileChooser.getSelectedFile();
                String name = FileUtilities.getNameWithoutExtention(selectedFile);
                try {
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
                            new FeatureCollectionPointsLayer(name, readFC, null);
                        wwjPanel.getWwd().getModel().getLayers().add(featureCollectionPointsLayer);
                        layerEventsListener.onLayerAdded(featureCollectionPointsLayer);

                    } else {
                        System.err.println("?????");
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
            }

        });

        _flatModeCheckBox.addActionListener(e -> {

            boolean selected = _flatModeCheckBox.isSelected();
            if (selected) {
                wwjPanel.setFlatGlobe(false);
            } else {
                wwjPanel.setSphereGlobe();
            }
        });

    }

}
