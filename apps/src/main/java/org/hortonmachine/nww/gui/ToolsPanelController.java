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

import java.awt.Dimension;
import java.awt.Point;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.nww.elevation.CoverageElevationModel;
import org.hortonmachine.nww.layers.defaults.annotations.HtmlScreenAnnotation;
import org.hortonmachine.nww.layers.defaults.annotations.HtmlScreenAnnotation.Builder;
import org.hortonmachine.nww.layers.defaults.other.CurrentGpsPointLayer;
import org.hortonmachine.nww.layers.defaults.other.SimplePointsLayer;
import org.hortonmachine.nww.layers.defaults.other.WhiteNwwLayer;
import org.hortonmachine.nww.utils.EGlobeModes;
import org.hortonmachine.nww.utils.FakeGps;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.joda.time.DateTime;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.WorldWindowGLJPanel;
import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.globes.ElevationModel;
import gov.nasa.worldwind.layers.AnnotationLayer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;

/**
 * The general actions of the viewer, called from its menu bar: opening files, globe mode,
 * terrain and background. The layers are loaded by its {@link #getLayerLoader() loader}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 *
 */
public class ToolsPanelController {

    private static final String PREF_GLOBE_MODE = "NWW_GLOBE_MODE";

    private final WhiteNwwLayer whiteLayer = new WhiteNwwLayer();

    private final NwwPanel wwjPanel;
    private final LayerEventsListener layerEventsListener;
    private final NwwLayerLoader layerLoader;

    private EGlobeModes globeMode = EGlobeModes.Earth;
    /** The DEM used as terrain, <code>null</code> for the Copernicus terrain. */
    private File terrainDemFile;
    /** If a DEM is being read to be used as terrain. */
    private boolean terrainDemLoading = false;
    private boolean opaqueBackground = true;

    public ToolsPanelController( final NwwPanel wwjPanel, LayerEventsListener layerEventsListener ) {
        this.wwjPanel = wwjPanel;
        this.layerEventsListener = layerEventsListener;
        this.layerLoader = new NwwLayerLoader(wwjPanel, layerEventsListener);

        if (layerEventsListener instanceof LayersPanelController) {
            ((LayersPanelController) layerEventsListener).setFileDropHandler(layerLoader::loadFiles);
        }

        // restore the mode of the last session
        String lastMode = PreferencesHandler.getPreference(PREF_GLOBE_MODE, EGlobeModes.Earth.name());
        for( EGlobeModes mode : EGlobeModes.values() ) {
            if (mode.name().equals(lastMode) && mode != EGlobeModes.Earth) {
                setGlobeMode(mode);
            }
        }
    }

    /**
     * @return the loader of the layers of the viewer, which also reloads and restyles them.
     */
    public NwwLayerLoader getLayerLoader() {
        return layerLoader;
    }

    public LayerEventsListener getLayerEventsListener() {
        return layerEventsListener;
    }

    public NwwPanel getWwjPanel() {
        return wwjPanel;
    }

    /**
     * Choose a database connection saved in the database viewer and load its spatial tables as
     * layers.
     */
    public void openFromSavedConnection() {
        new NwwDatabaseLoader(wwjPanel, layerLoader).openSavedConnection();
    }

    /**
     * Choose files and folders of the supported formats and load them as layers.
     */
    public void openFiles() {
        StringBuilder sb = new StringBuilder();
        for( String ext : NwwUtilities.SUPPORTED_EXTENSIONS ) {
            sb.append(",*.").append(ext);
        }
        final String desc = sb.substring(1);

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileFilter(new FileFilter(){
            @Override
            public String getDescription() {
                return desc;
            }

            @Override
            public boolean accept( File f ) {
                return f.isDirectory() || NwwLayerLoader.isSupportedFile(f);
            }
        });
        fileChooser.setCurrentDirectory(PreferencesHandler.getLastFile());
        int result = fileChooser.showOpenDialog(wwjPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File[] selectedFiles = fileChooser.getSelectedFiles();
            if (selectedFiles != null && selectedFiles.length > 0) {
                layerLoader.loadFiles(Arrays.asList(selectedFiles));
            }
        }
    }

    /**
     * Choose a shapefile or geopackage and fly along its geometries.
     */
    public void chooseVectorToFlyAlong() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("Vector files (*.shp, *.gpkg)", "shp", "gpkg"));
        fileChooser.setCurrentDirectory(PreferencesHandler.getLastFile());
        int result = fileChooser.showOpenDialog(wwjPanel);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            PreferencesHandler.setLastPath(selectedFile.getAbsolutePath());
            flyAlongVector(selectedFile);
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
                    GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "The geopackage contains no vector table.");
                    return;
                }
                String tableName = featureTables.get(0);
                if (featureTables.size() > 1) {
                    String[] tables = featureTables.toArray(new String[0]);
                    tableName = (String) JOptionPane.showInputDialog(wwjPanel, "Select the table to fly along", "Table selection",
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
            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "Unable to fly along the vector: " + e.getMessage());
        }
    }

    /**
     * Add the WKT geometry in the clipboard, in longitude/latitude, as a new layer.
     */
    public void pasteWkt() {
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
                GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "The clipboard content doesn't seem to be a WKT Geometry.");
                return;
            }

            wwjPanel.getWwd().getModel().getLayers().add(layer);
            layerEventsListener.onLayerAdded(layer);
        } catch (Exception e1) {
            e1.printStackTrace();
        }
    }

    /**
     * @return the globe mode of the map.
     */
    public EGlobeModes getGlobeMode() {
        return globeMode;
    }

    /**
     * Change the globe of the map, the mode is kept for the next session.
     *
     * @param mode the globe mode.
     */
    public void setGlobeMode( EGlobeModes mode ) {
        globeMode = mode;
        PreferencesHandler.setPreference(PREF_GLOBE_MODE, mode.name());
        switch( mode ) {
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
    }

    /**
     * Choose a DEM to use as the 3D terrain of its area, instead of the Copernicus one.
     */
    public void chooseTerrainDem() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setFileFilter(new FileNameExtensionFilter("*.tif,*.tiff,*.asc", "tif", "tiff", "asc"));
        fileChooser.setCurrentDirectory(PreferencesHandler.getLastFile());
        if (fileChooser.showOpenDialog(wwjPanel) == JFileChooser.APPROVE_OPTION) {
            File demFile = fileChooser.getSelectedFile();
            PreferencesHandler.setLastPath(demFile.getAbsolutePath());
            loadTerrainDem(demFile);
        }
    }

    /**
     * Use the Copernicus terrain again, instead of the DEM.
     */
    public void resetTerrain() {
        wwjPanel.setLocalTerrain(null);
        terrainDemFile = null;
        if (globeMode == EGlobeModes.EarthTerrain) {
            wwjPanel.setSphereGlobe(true);
        }
    }

    /**
     * @return the DEM used as terrain, or <code>null</code> if the Copernicus terrain is used.
     */
    public File getTerrainDemFile() {
        return terrainDemFile;
    }

    /**
     * @return <code>true</code> while a DEM is being read to be used as terrain.
     */
    public boolean isTerrainDemLoading() {
        return terrainDemLoading;
    }

    /**
     * Use a DEM as the 3D terrain of its area, with the Copernicus terrain around it, switch to
     * the globe with 3D terrain and zoom to the DEM.
     */
    private void loadTerrainDem( File demFile ) {
        terrainDemLoading = true;
        new Thread(() -> {
            try {
                GridCoverage2D dem = OmsRasterReader.readRaster(demFile.getAbsolutePath());
                ElevationModel terrain = CoverageElevationModel.withSurrounding(dem, NwwPanel.getTerrainElevationModel());
                ReferencedEnvelope demEnvelope = new ReferencedEnvelope(dem.getEnvelope2D()).transform(NwwUtilities.GPS_CRS,
                        true);
                Sector sector = NwwUtilities.envelope2Sector(demEnvelope);
                SwingUtilities.invokeLater(() -> {
                    wwjPanel.setLocalTerrain(terrain);
                    terrainDemFile = demFile;
                    terrainDemLoading = false;
                    if (globeMode == EGlobeModes.EarthTerrain) {
                        wwjPanel.setSphereGlobe(true);
                    } else {
                        setGlobeMode(EGlobeModes.EarthTerrain);
                    }
                    wwjPanel.goTo(sector, false);
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    terrainDemLoading = false;
                    GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "Unable to use the DEM as terrain: " + e.getMessage());
                });
            }
        }, "NWW terrain DEM").start();
    }

    /**
     * @return <code>true</code> if the white layer is shown below all the others.
     */
    public boolean isWhiteBackground() {
        return wwjPanel.getWwd().getModel().getLayers().contains(whiteLayer);
    }

    /**
     * Show or hide a white layer below all the others.
     *
     * @param white <code>true</code> to show it.
     */
    public void setWhiteBackground( boolean white ) {
        if (white == isWhiteBackground()) {
            return;
        }
        LayerList layers = wwjPanel.getWwd().getModel().getLayers();
        if (white) {
            layers.add(0, whiteLayer);
            layerEventsListener.onLayerAdded(whiteLayer);
        } else {
            layers.remove(whiteLayer);
            layerEventsListener.onLayerRemoved(whiteLayer);
        }
    }

    /**
     * @return <code>true</code> if the background of the map is opaque.
     */
    public boolean isOpaqueBackground() {
        return opaqueBackground;
    }

    /**
     * @param opaque <code>true</code> for an opaque background of the map.
     */
    public void setOpaqueBackground( boolean opaque ) {
        opaqueBackground = opaque;
        WorldWindow wwd = wwjPanel.getWwd();
        if (wwd instanceof WorldWindowGLJPanel) {
            ((WorldWindowGLJPanel) wwd).setOpaque(opaque);
        }
    }

    /**
     * Ask for an html text and add it as a screen annotation, in a layer of its own.
     */
    public void addAnnotation() {
        String layerName = "Annotation (" + new Date().toString() + ")";
        int width = 300;
        int yPos = wwjPanel.getHeight() / 2;

        String htmldefaultText = width + "," + yPos + "," + //
                "<p>\n<b><font color=\"#664400\">LA CLAPIÈRE</font></b><br />\n<i>Alt: "
                + "1100-1700m</i>\n</p>\n<p>\n<b>Glissement de terrain majeur</b> dans la haute Tinée, sur "
                + "un flanc du <a href=\"http://www.mercantour.eu\">Parc du Mercantour</a>, Alpes Maritimes.\n</p>\n"
                + "<p>\nRisque aggravé d'<b>inondation</b> du village de <i>Saint Étienne de Tinée</i> "
                + "juste en amont.\n</p><p>Last update:DATE</p>";

        String text = JOptionPane.showInputDialog(wwjPanel,
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
    }
}
