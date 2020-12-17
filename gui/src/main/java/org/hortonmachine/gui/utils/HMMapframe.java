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
package org.hortonmachine.gui.utils;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.FilenameFilter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JToolBar;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.map.MapLayerListEvent;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.swing.JMapFrame;
import org.geotools.tile.TileService;
import org.geotools.tile.impl.osm.OSMService;
import org.geotools.tile.util.TileLayer;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.style.StyleUtilities;
import org.hortonmachine.gui.settings.SettingsController;

/**
 * A simple map frame where layers can be set or added with default styles.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
@SuppressWarnings("serial")
public class HMMapframe extends JMapFrame {

    private MapContent content;

    private RasterInfoLayer rasterInfoLayer = new RasterInfoLayer();

    public HMMapframe( String title ) {
        super();
        content = new MapContent();
        content.setTitle(title);
        setMapContent(content);
    }

    public void addLayer( SimpleFeatureCollection featureCollection ) {
        FeatureLayer fl = makeFeatureLayer(featureCollection);
        addLayer(fl);
    }

    public void setLayer( SimpleFeatureCollection featureCollection ) {
        FeatureLayer fl = makeFeatureLayer(featureCollection);
        setLayer(fl);
    }

    public void addLayer( SimpleFeatureCollection featureCollection, Style style ) {
        FeatureLayer fl = new FeatureLayer(featureCollection, style);
        addLayer(fl);
    }

    public void setLayer( SimpleFeatureCollection featureCollection, Style style ) {
        FeatureLayer fl = new FeatureLayer(featureCollection, style);
        setLayer(fl);
    }

    public void addLayer( Layer layer ) {
        content.addLayer(layer);
    }

    public void removeLayer( Layer layer ) {
        content.removeLayer(layer);
    }

    public void addLayerBottom( Layer layer ) {
        content.addLayer(layer);
        int index = content.layers().indexOf(layer);
        content.moveLayer(index, 0);
        getMapPane().layerMoved(new MapLayerListEvent(content, layer, 0));

    }

    public void setLayer( Layer layer ) {
        List<Layer> layers = content.layers();
        for( Layer l : layers ) {
            content.removeLayer(l);
        }
        content.addLayer(layer);
    }

    public List<Layer> getLayers() {
        return content.layers();
    }

    private FeatureLayer makeFeatureLayer( SimpleFeatureCollection featureCollection ) {
        Style style = StyleUtilities.createDefaultStyle(featureCollection);
        FeatureLayer fl = new FeatureLayer(featureCollection, style);
        return fl;
    }

    public static HMMapframe openFolder( File folder ) {
        String title = "HM Viewer of folder: " + folder.getAbsolutePath();
        HMMapframe mapFrame = getBaseFrame(title, false);

        File[] rasterFiles = folder.listFiles(new FilenameFilter(){
            @Override
            public boolean accept( File dir, String name ) {
                return HMConstants.isRaster(new File(dir, name));
            }
        });
        Arrays.sort(rasterFiles);
        File[] vectorFiles = folder.listFiles(new FilenameFilter(){
            @Override
            public boolean accept( File dir, String name ) {
                return HMConstants.isVector(new File(dir, name));
            }
        });
        Arrays.sort(vectorFiles);

        if (rasterFiles != null && rasterFiles.length > 0) {
            for( File rasterFile : rasterFiles ) {
                try {
                    GridCoverage2D raster = OmsRasterReader.readRaster(rasterFile.getAbsolutePath());
                    File styleFile = FileUtilities.substituteExtention(rasterFile, "sld");
                    Style style;
                    if (styleFile.exists()) {
                        style = SldUtilities.getStyleFromFile(styleFile);
                    } else {
                        style = SldUtilities.getStyleFromRasterFile(rasterFile);
                    }
                    GridCoverageLayer layer = new GridCoverageLayer(raster, style, rasterFile.getName());
                    mapFrame.addLayer(layer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (vectorFiles != null && vectorFiles.length > 0) {
            for( File vectorFile : vectorFiles ) {
                try {
                    SimpleFeatureCollection fc = OmsVectorReader.readVector(vectorFile.getAbsolutePath());
                    File styleFile = FileUtilities.substituteExtention(vectorFile, "sld");
                    Style style;
                    if (styleFile.exists()) {
                        style = SldUtilities.getStyleFromFile(styleFile);
                    } else {
                        style = SLD.createSimpleStyle(fc.getSchema());
                    }
                    FeatureLayer layer = new FeatureLayer(fc, style, vectorFile.getName());
                    mapFrame.addLayer(layer);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        final DefaultComboBoxModel<String> layersComboModel = new DefaultComboBoxModel<String>();
        layersComboModel.addElement("");
        HashMap<String, GridCoverageLayer> name2Layermap = new HashMap<>();
        List<Layer> layers = mapFrame.getLayers();
        for( Layer layer : layers ) {
            if (layer instanceof GridCoverageLayer) {
                layersComboModel.addElement(layer.getTitle());
                name2Layermap.put(layer.getTitle(), (GridCoverageLayer) layer);
            }
        }
        final JComboBox<String> layersCombo = new JComboBox<String>(layersComboModel);
        layersCombo.setSelectedIndex(0);
        layersCombo.addActionListener(( e ) -> {
            GridCoverageLayer layer = mapFrame.rasterInfoLayer.getRasterLayer();
            mapFrame.removeLayer(layer);

            String layerName = (String) layersCombo.getSelectedItem();
            if (layerName.length() == 0) {
                mapFrame.rasterInfoLayer.setRasterLayer(null);
            } else {
                GridCoverageLayer gridCoverageLayer = name2Layermap.get(layerName);
                mapFrame.rasterInfoLayer.setRasterLayer(gridCoverageLayer);
                mapFrame.addLayer(mapFrame.rasterInfoLayer);
            }
        });
        mapFrame.getToolBar().add(layersCombo);

        return mapFrame;
    }

    private static HMMapframe getBaseFrame( String title, boolean exitOnClose ) {
        ImageIcon icon = new ImageIcon(ImageCache.getInstance().getBufferedImage(ImageCache.HORTONMACHINE_FRAME_ICON));
        HMMapframe mapFrame = new HMMapframe(title);
        mapFrame.setIconImage(icon.getImage());
        mapFrame.enableToolBar(true);
        mapFrame.enableStatusBar(true);
        mapFrame.enableLayerTable(true);
        mapFrame.enableTool(Tool.PAN, Tool.ZOOM, Tool.RESET, Tool.INFO);
        if (exitOnClose) {
            mapFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        } else {
            mapFrame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        }
        mapFrame.setSize(1200, 900);
        mapFrame.setVisible(true);

        JToolBar toolBar = mapFrame.getToolBar();
        toolBar.add(new AbstractAction("Add OSM background", ImageCache.getInstance().getImage(ImageCache.GLOBE)){
            @Override
            public void actionPerformed( ActionEvent e ) {
                String baseURL = "http://tile.openstreetmap.org/";
                TileService service = new OSMService("OpenStreetMap", baseURL);
                TileLayer layer = new TileLayer(service);
                layer.setTitle("OpenStreetMap");
                mapFrame.addLayerBottom(layer);
            }
        });

        return mapFrame;
    }

    /**
     * Opens a new frame to which layers can be added for fast visualization of data.
     * @param exitOnClose 
     * 
     * @return the map frame.
     */
    public static HMMapframe openFrame( boolean exitOnClose ) {
        return getBaseFrame("HM Viewer", exitOnClose);
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();
        File openFile = null;
        if (args.length > 0 && new File(args[0]).exists()) {
            openFile = new File(args[0]);
            if (!openFile.isDirectory()) {
                openFile = openFile.getParentFile();
            }
        } else {
            File[] openFolder = GuiUtilities.showOpenFolderDialog(null, "Select folder to show", false,
                    PreferencesHandler.getLastFile());
            if (openFolder != null && openFolder.length > 0) {
                openFile = openFolder[0];
            }
        }

        if (openFile != null) {
            HMMapframe mf = openFolder(openFile);
            SettingsController.applySettings(mf);
            mf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        } else {
            GuiUtilities.showWarningMessage(null, "No data folder supplied!");
        }
    }

}
