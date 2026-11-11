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
package org.hortonmachine.nww.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.nww.gui.LayerEventsListener;
import org.hortonmachine.nww.gui.LayersPanelController;
import org.hortonmachine.nww.gui.NwwLayerLoader;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.ToolsPanelController;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer;
import org.hortonmachine.nww.tools.features.FeatureSelection;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

/**
 * The access of the {@link INwwTool}s to the viewer: its map, layers and loaders.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class NwwContext {

    /** The points of each side of an envelope transformed to longitude/latitude. */
    private static final int DENSIFICATION_POINTS = 10;
    /** The key of the layer that replaced a layer, kept in the replaced one. */
    private static final String REPLACED_BY_KEY = "org.hortonmachine.nww.tools.NwwContext.replacedBy";

    private final NwwPanel nwwPanel;
    private final LayerEventsListener layerEventsListener;
    private final ToolsPanelController toolsPanel;
    private NwwToolsTabs toolsTabs;
    private Consumer<INwwTool> toolOpener;
    /** The selected features, <code>null</code> if none. */
    private FeatureSelection selection;
    private final List<Consumer<FeatureSelection>> selectionListeners = new ArrayList<>();

    /**
     * @param nwwPanel the map.
     * @param layerEventsListener the listener told of the layers added and removed, as the
     *          layers panel.
     * @param toolsPanel the general actions of the viewer, which load files and feature collections.
     */
    public NwwContext( NwwPanel nwwPanel, LayerEventsListener layerEventsListener, ToolsPanelController toolsPanel ) {
        this.nwwPanel = nwwPanel;
        this.layerEventsListener = layerEventsListener;
        this.toolsPanel = toolsPanel;
    }

    /**
     * @param toolsTabs the tabs created with this context.
     */
    void setToolsTabs( NwwToolsTabs toolsTabs ) {
        this.toolsTabs = toolsTabs;
    }

    /**
     * @param toolOpener opens the tab of a tool as the menu of the viewer does, also showing the
     *          panel of the tabs if hidden; without it the tab is only opened.
     */
    public void setToolOpener( Consumer<INwwTool> toolOpener ) {
        this.toolOpener = toolOpener;
    }

    /**
     * Open the tab of a tool and select it, as from the menu of the viewer.
     *
     * @param tool a tool of the viewer.
     */
    public void openTool( INwwTool tool ) {
        if (toolOpener != null) {
            toolOpener.accept(tool);
        } else if (toolsTabs != null) {
            toolsTabs.openTool(tool);
        }
    }

    /**
     * @return the selected features, <code>null</code> if none.
     */
    public FeatureSelection getSelection() {
        return selection;
    }

    /**
     * Replace the selected features and tell the selection listeners.
     *
     * @param selection the new selection, <code>null</code> to clear it.
     */
    public void setSelection( FeatureSelection selection ) {
        this.selection = selection;
        for( Consumer<FeatureSelection> listener : new ArrayList<>(selectionListeners) ) {
            listener.accept(selection);
        }
    }

    /**
     * Clear the selected features.
     */
    public void clearSelection() {
        setSelection(null);
    }

    /**
     * @param listener told of each new selection, <code>null</code> when it is cleared.
     */
    public void addSelectionListener( Consumer<FeatureSelection> listener ) {
        selectionListeners.add(listener);
    }

    /**
     * Make the panel of a layer in the layer tab again, as after a change of the layer made
     * outside of the panel, as its filter.
     *
     * @param layer the layer.
     */
    public void refreshLayerPanel( Layer layer ) {
        if (toolsTabs != null) {
            toolsTabs.refreshLayerPanel(layer);
        }
    }

    /**
     * Show the panel of a layer in the layer tab and select the tab, as when the layer is
     * clicked in the layers panel.
     *
     * @param layer the layer.
     */
    public void showLayerPanel( Layer layer ) {
        if (toolsTabs != null) {
            toolsTabs.showLayer(layer);
        }
    }

    /**
     * @param type the class of a tool.
     * @return the first tool of the viewer of the class, open or not, or <code>null</code>.
     */
    public <T extends INwwTool> T findTool( Class<T> type ) {
        if (toolsTabs != null) {
            for( INwwTool tool : toolsTabs.getTools() ) {
                if (type.isInstance(tool)) {
                    return type.cast(tool);
                }
            }
        }
        return null;
    }

    /**
     * @return the map.
     */
    public NwwPanel getNwwPanel() {
        return nwwPanel;
    }

    /**
     * @return the general actions of the viewer, as globe mode, terrain and background.
     */
    public ToolsPanelController getToolsPanel() {
        return toolsPanel;
    }

    /**
     * @return the loader of the layers, which also reloads and restyles them.
     */
    public NwwLayerLoader getLayerLoader() {
        return toolsPanel.getLayerLoader();
    }

    /**
     * @return the layer selected with a click in the layers panel, or <code>null</code>.
     */
    public Layer getCurrentLayer() {
        if (layerEventsListener instanceof LayersPanelController) {
            return ((LayersPanelController) layerEventsListener).getCurrentLayer();
        }
        return null;
    }

    /**
     * Add a layer on top of the map and to the layers panel.
     *
     * @param layer the layer to add.
     */
    public void addLayer( Layer layer ) {
        nwwPanel.addLayer(layer);
        layerEventsListener.onLayerAdded(layer);
    }

    /**
     * Remove a layer from the map and from the layers panel.
     *
     * @param layer the layer to remove.
     */
    public void removeLayer( Layer layer ) {
        nwwPanel.removeLayer(layer);
        layerEventsListener.onLayerRemoved(layer);
        if (selection != null && selection.getLayer() == layer) {
            clearSelection();
        }
        // the tiles of the rasters, if the setting is on
        GridCoverageNwwLayer.clearCacheOfRemoved(layer, nwwPanel.getWwd().getModel().getLayers());
    }

    /**
     * Replace a layer by another, as a filtered version of it: the replacement takes its
     * position, visibility and opacity, its panel in the layer tab and, if it was the current
     * layer, its place as current layer. The replaced layer is disposed.
     *
     * <p>The panels that keep a layer find the one that replaced it with
     * {@link #getLatest(Layer)}.</p>
     *
     * @param layer the layer to replace.
     * @param replacement the layer that replaces it.
     */
    public void replaceLayer( Layer layer, Layer replacement ) {
        LayerList layers = nwwPanel.getWwd().getModel().getLayers();
        int index = layers.indexOf(layer);
        if (index < 0) {
            throw new IllegalArgumentException("The layer is not in the viewer: " + layer.getName());
        }
        replacement.setEnabled(layer.isEnabled());
        replacement.setOpacity(layer.getOpacity());
        if (toolsTabs != null) {
            toolsTabs.replaceLayer(layer, replacement);
        }
        layers.set(index, replacement);
        if (layerEventsListener instanceof LayersPanelController) {
            ((LayersPanelController) layerEventsListener).onLayerReplaced(layer, replacement);
        } else {
            layerEventsListener.onLayerRemoved(layer);
            layerEventsListener.onLayerAdded(replacement);
        }
        layer.dispose();
        // after the disposal, which might clear the values of the layer
        layer.setValue(REPLACED_BY_KEY, replacement);
        nwwPanel.getWwd().redraw();
        if (selection != null && selection.getLayer() == layer) {
            // the same features, if the replacement still has them
            setSelection(selection.onReplacement(replacement));
        }
    }

    /**
     * @param layer a layer.
     * @return the layer that replaced it, after any number of replacements, as by a filter and
     *          then a style, or the layer itself if it was not replaced.
     */
    public Layer getLatest( Layer layer ) {
        Layer latest = layer;
        Object replacement;
        while( (replacement = latest.getValue(REPLACED_BY_KEY)) instanceof Layer ) {
            latest = (Layer) replacement;
        }
        return latest;
    }

    /**
     * Load a feature collection as a layer, with the next of the default colors, as the
     * layers loaded without a style.
     *
     * @param name the name of the layer.
     * @param featureCollection the features, in any CRS: they are reprojected to longitude/latitude
     *          if needed.
     * @throws Exception
     */
    public void loadFeatureCollection( String name, SimpleFeatureCollection featureCollection ) throws Exception {
        CoordinateReferenceSystem crs = featureCollection.getSchema().getCoordinateReferenceSystem();
        if (crs != null && !HMCrsRegistry.crsEquals(crs, NwwUtilities.GPS_CRS)) {
            featureCollection = new HMCrsTransformer(crs, NwwUtilities.GPS_CRS).transform(featureCollection);
        }
        getLayerLoader().loadFeatureCollection(name, featureCollection);
    }

    /**
     * Load files as layers, as if they were dropped on the layers panel.
     *
     * @param files the files and folders to load.
     */
    public void loadFiles( List<File> files ) {
        getLayerLoader().loadFiles(files);
    }

    /**
     * Zoom the map to an envelope.
     *
     * @param envelope the envelope, in any CRS: without one it is taken as longitude/latitude.
     * @throws Exception
     */
    public void goTo( ReferencedEnvelope envelope ) throws Exception {
        Envelope envelopeLL = envelope;
        CoordinateReferenceSystem crs = envelope.getCoordinateReferenceSystem();
        if (crs != null && !HMCrsRegistry.crsEquals(crs, NwwUtilities.GPS_CRS)) {
            envelopeLL = new HMCrsTransformer(crs, NwwUtilities.GPS_CRS).transformDensified(envelope, DENSIFICATION_POINTS);
        }
        Sector sector = NwwUtilities.envelope2Sector(new ReferencedEnvelope(envelopeLL, NwwUtilities.GPS_CRS));
        nwwPanel.goTo(sector, false);
    }

    /**
     * @return the area shown by the map in longitude/latitude, or <code>null</code> if the map
     *          does not reach the corners of the view, as when the whole globe is visible.
     */
    public ReferencedEnvelope getViewportBounds() {
        return nwwPanel.getViewportBounds();
    }
}
