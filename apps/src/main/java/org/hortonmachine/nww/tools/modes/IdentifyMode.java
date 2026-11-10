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

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;

import javax.swing.Icon;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.listeners.GenericSelectListener;
import org.hortonmachine.nww.layers.defaults.NwwFeatureLayer;
import org.hortonmachine.nww.shapes.IFeatureShape;
import org.hortonmachine.nww.shapes.IInfoShape;
import org.hortonmachine.nww.tools.INwwMapMode;
import org.hortonmachine.nww.tools.NwwContext;
import org.hortonmachine.nww.tools.features.FeatureInfoTool;
import org.hortonmachine.nww.utils.CursorUtils;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

import gov.nasa.worldwind.View;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.event.SelectEvent;
import gov.nasa.worldwind.event.SelectListener;
import gov.nasa.worldwind.geom.Intersection;
import gov.nasa.worldwind.geom.Line;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.pick.PickedObject;
import gov.nasa.worldwind.terrain.SectorGeometryList;

/**
 * Click the map to see the features of the current layer at the point, in the
 * {@link FeatureInfoTool} tab. The features are queried from the data of the layer, so this
 * works also for the layers drawn as image tiles.
 *
 * <p>If the current layer has no features, as a raster or a point cloud, the clicked object is
 * described as before, in a dialog.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class IdentifyMode implements INwwMapMode {

    /** The distance from the clicked point within which features are found, in pixels. */
    private static final int TOLERANCE_PIXELS = 6;

    private static final GeometryFactory GF = new GeometryFactory();

    private SelectListener selectListener;

    @Override
    public String getName() {
        return "Identify";
    }

    @Override
    public String getDescription() {
        return "Click a feature of the current layer to see and edit its attributes";
    }

    @Override
    public Icon getIcon() {
        return ImageCache.get(ImageCache.MAP_INFO, ImageCache.SIZE_TOOLBAR);
    }

    @Override
    public void activate( NwwContext context ) {
        NwwPanel nwwPanel = context.getNwwPanel();
        GenericSelectListener fallback = new GenericSelectListener(nwwPanel);
        selectListener = event -> {
            if (event.getEventAction().equals(SelectEvent.LEFT_CLICK)) {
                identify(context, event, fallback);
            }
        };
        nwwPanel.getWwd().addSelectListener(selectListener);
        CursorUtils.makeHand(nwwPanel.getWwd());
    }

    @Override
    public void deactivate( NwwContext context ) {
        NwwPanel nwwPanel = context.getNwwPanel();
        if (selectListener != null) {
            nwwPanel.getWwd().removeSelectListener(selectListener);
            selectListener = null;
        }
        CursorUtils.makeDefault(nwwPanel.getWwd());
    }

    private void identify( NwwContext context, SelectEvent event, GenericSelectListener fallback ) {
        Layer layer = context.getCurrentLayer();
        FeatureInfoTool tool = context.findTool(FeatureInfoTool.class);
        if (!(layer instanceof NwwFeatureLayer) || tool == null) {
            PickedObject top = event.getTopPickedObject();
            Object object = top == null ? null : top.getObject();
            if (object instanceof IFeatureShape || object instanceof IInfoShape) {
                fallback.selected(event);
            } else if (tool != null) {
                context.openTool(tool);
                tool.showMessage(layer == null
                        ? "Select a layer in the layers panel: Identify shows the features of the current layer."
                        : "The current layer, " + layer.getName() + ", has no features to identify.");
            }
            return;
        }

        WorldWindow wwd = context.getNwwPanel().getWwd();
        Point point = event.getPickPoint();
        if (point == null) {
            return;
        }
        Position position = positionAt(wwd, point.x, point.y);
        if (position == null) {
            // outside of the globe
            return;
        }
        Position near = positionAt(wwd, point.x + TOLERANCE_PIXELS, point.y);
        double tolerance = near == null
                ? 1E-5
                : Math.max(Math.abs(near.getLongitude().degrees - position.getLongitude().degrees),
                        Math.abs(near.getLatitude().degrees - position.getLatitude().degrees));
        double lon = position.getLongitude().degrees;
        double lat = position.getLatitude().degrees;
        Envelope box = new Envelope(lon - tolerance, lon + tolerance, lat - tolerance, lat + tolerance);

        List<SimpleFeature> features = query(((NwwFeatureLayer) layer).getFeatureCollection(), box);
        context.openTool(tool);
        tool.showFeatures(layer, position, features);
        event.consume();
    }

    /**
     * @return the point of the terrain at a screen point, else, as when the terrain is not drawn
     *          there yet, of the ellipsoid, <code>null</code> outside of the globe.
     */
    private static Position positionAt( WorldWindow wwd, int x, int y ) {
        View view = wwd.getView();
        SectorGeometryList terrain = wwd.getSceneController().getTerrain();
        if (terrain != null) {
            Line ray = view.computeRayFromScreenPoint(x, y);
            Intersection[] intersections = ray == null ? null : terrain.intersect(ray);
            if (intersections != null && intersections.length > 0) {
                return wwd.getModel().getGlobe().computePositionFromPoint(intersections[0].getIntersectionPoint());
            }
        }
        return view.computePositionFromScreenPoint(x, y);
    }

    /**
     * @param collection features in longitude/latitude.
     * @param box the area around the clicked point, in longitude/latitude.
     * @return the features whose geometry touches the area.
     */
    static List<SimpleFeature> query( SimpleFeatureCollection collection, Envelope box ) {
        Geometry area = GF.toGeometry(box);
        List<SimpleFeature> found = new ArrayList<>();
        try (SimpleFeatureIterator iterator = collection.features()) {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                Object geometry = feature.getDefaultGeometry();
                if (geometry instanceof Geometry && ((Geometry) geometry).getEnvelopeInternal().intersects(box)
                        && ((Geometry) geometry).intersects(area)) {
                    found.add(feature);
                }
            }
        }
        return found;
    }
}
