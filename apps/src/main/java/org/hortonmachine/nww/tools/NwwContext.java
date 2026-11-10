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
import java.util.List;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.nww.gui.LayerEventsListener;
import org.hortonmachine.nww.gui.LayersPanelController;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.ToolsPanelController;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;

/**
 * The access of the {@link INwwTool}s to the viewer: its map, layers and loaders.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NwwContext {

    /** The points of each side of an envelope transformed to longitude/latitude. */
    private static final int DENSIFICATION_POINTS = 10;

    private final NwwPanel nwwPanel;
    private final LayerEventsListener layerEventsListener;
    private final ToolsPanelController toolsPanel;

    /**
     * @param nwwPanel the map.
     * @param layerEventsListener the listener told of the layers added and removed, as the
     *          layers panel.
     * @param toolsPanel the panel of the general tools, which loads files and feature collections.
     */
    public NwwContext( NwwPanel nwwPanel, LayerEventsListener layerEventsListener, ToolsPanelController toolsPanel ) {
        this.nwwPanel = nwwPanel;
        this.layerEventsListener = layerEventsListener;
        this.toolsPanel = toolsPanel;
    }

    /**
     * @return the map.
     */
    public NwwPanel getNwwPanel() {
        return nwwPanel;
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
        toolsPanel.loadFeatureCollection(name, featureCollection);
    }

    /**
     * Load files as layers, as if they were dropped on the layers panel.
     *
     * @param files the files and folders to load.
     */
    public void loadFiles( List<File> files ) {
        toolsPanel.loadFiles(files);
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
