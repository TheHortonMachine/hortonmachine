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
package org.hortonmachine.nww.tools.modes;

import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;
import javax.swing.SwingUtilities;

import org.geotools.api.feature.simple.SimpleFeature;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.gui.NwwLayerLoader;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.listeners.GenericSelectListener;
import org.hortonmachine.nww.layers.defaults.NwwFeatureLayer;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer;
import org.hortonmachine.nww.shapes.IFeatureShape;
import org.hortonmachine.nww.shapes.IInfoShape;
import org.hortonmachine.nww.tools.INwwMapMode;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.tools.NwwContext;
import org.hortonmachine.nww.tools.features.FeatureInfoTool;
import org.hortonmachine.nww.tools.features.FeatureSelection;
import org.hortonmachine.nww.tools.features.RasterValues;
import org.hortonmachine.nww.utils.CursorUtils;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.nww.utils.selection.ScreenSelector;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.pick.PickedObjectList;

/**
 * Select features of the current layer, shown in the {@link FeatureInfoTool} tab: click to
 * select the features at a point, drag a box to select those in it. With Shift the features
 * are added to the selection; a click where there is no feature clears it.
 *
 * <p>The features are queried from the data of the layer, so this works also for the layers
 * drawn as image tiles. On a raster layer read from a file, a click shows the values of its bands
 * at the point. If the current layer has no features, as a point cloud, a click describes the
 * clicked object in a dialog, as before.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class SelectMode implements INwwMapMode {

    /** The distance from the clicked point within which features are found, in pixels. */
    private static final int TOLERANCE_PIXELS = 6;
    /** A press and release closer than this, in pixels, is a click, not a box. */
    private static final int CLICK_PIXELS = 4;

    private static final GeometryFactory GF = new GeometryFactory();

    private Selector selector;
    private GenericSelectListener fallback;
    /** Keeps the last clicked raster open while the mode is active. */
    private final RasterValues.Reader rasterReader = new RasterValues.Reader();

    @Override
    public String getName() {
        return "Select";
    }

    @Override
    public String getDescription() {
        return "<html>Click a feature or drag a box to select features of the current layer<br>"
                + "Shift adds to the selection, a click on no feature clears it<br>"
                + "On a raster layer, a click shows its values</html>";
    }

    @Override
    public Icon getIcon() {
        return ImageCache.get(ImageCache.SELECTION_MODE, ImageCache.SIZE_TOOLBAR);
    }

    @Override
    public void activate( NwwContext context ) {
        NwwPanel nwwPanel = context.getNwwPanel();
        if (selector == null) {
            selector = new Selector(context);
            fallback = new GenericSelectListener(nwwPanel);
        }
        selector.enable();
        CursorUtils.makeCrossHair(nwwPanel.getWwd());
    }

    @Override
    public void deactivate( NwwContext context ) {
        if (selector != null) {
            selector.disable();
        }
        rasterReader.close();
        CursorUtils.makeDefault(context.getNwwPanel().getWwd());
    }

    /**
     * The box of the screen selector, which also tells clicks apart and accepts Shift.
     */
    private class Selector extends ScreenSelector {
        private final NwwContext context;
        private int startX;
        private int startY;
        private boolean add;

        Selector( NwwContext context ) {
            super(context.getNwwPanel().getWwd());
            this.context = context;
        }

        @Override
        public void mousePressed( MouseEvent mouseEvent ) {
            int modifiers = mouseEvent.getModifiersEx();
            if (modifiers != MouseEvent.BUTTON1_DOWN_MASK
                    && modifiers != (MouseEvent.BUTTON1_DOWN_MASK | MouseEvent.SHIFT_DOWN_MASK)) {
                return;
            }
            add = mouseEvent.isShiftDown();
            startX = mouseEvent.getX();
            startY = mouseEvent.getY();
            armed = true;
            selectionStarted(mouseEvent);
            mouseEvent.consume();
        }

        @Override
        public void mouseClicked( MouseEvent mouseEvent ) {
            // the click is handled at the release: consumed, else the view moves the clicked
            // point to the center
            if (mouseEvent.getButton() == MouseEvent.BUTTON1) {
                mouseEvent.consume();
            }
        }

        @Override
        public void mouseReleased( MouseEvent mouseEvent ) {
            boolean wasArmed = armed;
            super.mouseReleased(mouseEvent);
            if (!wasArmed) {
                return;
            }
            int endX = mouseEvent.getX();
            int endY = mouseEvent.getY();
            if (Math.abs(endX - startX) < CLICK_PIXELS && Math.abs(endY - startY) < CLICK_PIXELS) {
                onClick(context, mouseEvent, add);
            } else {
                onBox(context, startX, startY, endX, endY, add);
            }
        }
    }

    private void onClick( NwwContext context, MouseEvent mouseEvent, boolean add ) {
        WorldWindow wwd = context.getNwwPanel().getWwd();
        Layer layer = context.getCurrentLayer();
        if (isRasterLayer(layer)) {
            Position position = NwwUtilities.getTerrainPosition(wwd, mouseEvent.getX(), mouseEvent.getY());
            if (position != null) {
                showRasterValues(context, layer, position);
            }
            return;
        }
        if (!(layer instanceof NwwFeatureLayer)) {
            PickedObjectList picked = wwd.getObjectsAtCurrentPosition();
            PickedObject top = picked == null ? null : picked.getTopPickedObject();
            Object object = top == null ? null : top.getObject();
            if (object instanceof IFeatureShape || object instanceof IInfoShape) {
                fallback.selected(new SelectEvent(wwd, SelectEvent.LEFT_CLICK, mouseEvent, picked));
            } else {
                showNoLayer(context, layer);
            }
            return;
        }

        int x = mouseEvent.getX();
        int y = mouseEvent.getY();
        Position position = NwwUtilities.getTerrainPosition(wwd, x, y);
        if (position == null) {
            // outside of the globe
            return;
        }
        Position near = NwwUtilities.getTerrainPosition(wwd, x + TOLERANCE_PIXELS, y);
        double tolerance = near == null
                ? 1E-5
                : Math.max(Math.abs(near.getLongitude().degrees - position.getLongitude().degrees),
                        Math.abs(near.getLatitude().degrees - position.getLatitude().degrees));
        double lon = position.getLongitude().degrees;
        double lat = position.getLatitude().degrees;
        Geometry area = GF.toGeometry(new Envelope(lon - tolerance, lon + tolerance, lat - tolerance, lat + tolerance));
        apply(context, layer, FeatureSelection.query(((NwwFeatureLayer) layer).getFeatureCollection(), area), position, add);
    }

    private void onBox( NwwContext context, int x1, int y1, int x2, int y2, boolean add ) {
        Layer layer = context.getCurrentLayer();
        if (isRasterLayer(layer)) {
            FeatureInfoTool tool = context.findTool(FeatureInfoTool.class);
            if (tool != null) {
                context.openTool(tool);
                tool.showMessage("A box selects features. Click the raster " + layer.getName() + " to read its values.");
            }
            return;
        }
        if (!(layer instanceof NwwFeatureLayer)) {
            showNoLayer(context, layer);
            return;
        }
        Geometry area = NwwUtilities.getScreenPointsPolygon(context.getNwwPanel().getWwd(), x1, y1, x2, y2);
        if (area == null) {
            // a corner of the box is outside of the globe
            return;
        }
        apply(context, layer, FeatureSelection.query(((NwwFeatureLayer) layer).getFeatureCollection(), area), null, add);
    }

    /**
     * Make the found features the selection, or add them to it, and show them.
     */
    private static void apply( NwwContext context, Layer layer, List<SimpleFeature> found, Position point, boolean add ) {
        FeatureSelection current = context.getSelection();
        if (add && current != null && current.getLayer() == layer) {
            if (found.isEmpty()) {
                return;
            }
            List<SimpleFeature> union = new ArrayList<>(current.getFeatures());
            Set<String> ids = new HashSet<>();
            for( SimpleFeature feature : union ) {
                ids.add(feature.getID());
            }
            for( SimpleFeature feature : found ) {
                if (ids.add(feature.getID())) {
                    union.add(feature);
                }
            }
            found = union;
            point = null;
        }
        if (found.isEmpty()) {
            context.clearSelection();
            return;
        }
        FeatureInfoTool tool = context.findTool(FeatureInfoTool.class);
        if (tool != null) {
            context.openTool(tool);
        }
        context.setSelection(new FeatureSelection(layer, found, point));
    }

    private static boolean isRasterLayer( Layer layer ) {
        if (layer instanceof GridCoverageNwwLayer && ((GridCoverageNwwLayer) layer).isRemote()) {
            return true;
        }
        LayerSource source = LayerSource.of(layer);
        return layer instanceof GridCoverageNwwLayer && source != null && NwwLayerLoader.isRasterFile(source.file());
    }

    /**
     * Read the values of a raster layer at a point, out of the event thread, and show them.
     */
    private void showRasterValues( NwwContext context, Layer layer, Position position ) {
        FeatureInfoTool tool = context.findTool(FeatureInfoTool.class);
        if (tool == null) {
            return;
        }
        // the remote rasters, as the cloud optimized GeoTIFFs, are read by the reader of the layer
        GridCoverageNwwLayer rasterLayer = (GridCoverageNwwLayer) layer;
        File file = rasterLayer.isRemote() ? null : LayerSource.of(layer).file();
        new Thread(() -> {
            try {
                double lon = position.getLongitude().degrees;
                double lat = position.getLatitude().degrees;
                RasterValues values = file != null
                        ? rasterReader.read(file, lon, lat)
                        : rasterReader.read(rasterLayer.getRemoteReader(), lon, lat);
                SwingUtilities.invokeLater(() -> {
                    context.openTool(tool);
                    tool.showRasterValues(layer, position, values);
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    context.openTool(tool);
                    tool.showMessage("Unable to read the values of " + layer.getName() + ": " + e.getMessage());
                });
            }
        }, "hm-raster-values").start();
    }

    private static void showNoLayer( NwwContext context, Layer layer ) {
        FeatureInfoTool tool = context.findTool(FeatureInfoTool.class);
        if (tool == null) {
            return;
        }
        context.openTool(tool);
        tool.showMessage(layer == null
                ? "Select a layer in the layers panel: Select works on the features of the current layer."
                : "The current layer, " + layer.getName() + ", has no features to select.");
    }
}
