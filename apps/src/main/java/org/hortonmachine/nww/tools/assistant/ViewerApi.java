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
package org.hortonmachine.nww.tools.assistant;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Callable;

import javax.swing.SwingUtilities;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.osm.OverpassResult;
import org.hortonmachine.nww.tools.NwwContext;
import org.hortonmachine.nww.tools.features.FeatureSelection;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.locationtech.jts.geom.Envelope;

/**
 * The map of the viewer for its scripts, bound as <code>viewer</code>.
 *
 * <p>The scripts run in a thread of their own, the changes of the map are made in the event
 * thread.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ViewerApi {

    private final NwwContext context;

    /**
     * @param context the access to the viewer.
     */
    public ViewerApi( NwwContext context ) {
        this.context = context;
    }

    /**
     * Show features as a layer.
     *
     * @param features the features, in any CRS.
     * @param name the name of the layer.
     * @throws Exception
     */
    public void addLayer( SimpleFeatureCollection features, String name ) throws Exception {
        onEdt(() -> {
            context.loadFeatureCollection(name, features);
            return null;
        });
    }

    /**
     * Show the points, lines and polygons of an Overpass result, as one layer each, leaving out
     * the empty ones.
     *
     * @param result the result.
     * @param name the name of the layers, followed by the geometry type if there are more.
     * @throws Exception
     */
    public void addResult( OverpassResult result, String name ) throws Exception {
        List<SimpleFeatureCollection> parts = new ArrayList<>();
        List<String> types = new ArrayList<>();
        for( Object[] part : new Object[][]{{result.getPolygons(), "polygons"}, {result.getLines(), "lines"},
                {result.getPoints(), "points"}} ) {
            SimpleFeatureCollection features = (SimpleFeatureCollection) part[0];
            if (!features.isEmpty()) {
                parts.add(features);
                types.add((String) part[1]);
            }
        }
        for( int i = 0; i < parts.size(); i++ ) {
            addLayer(parts.get(i), parts.size() > 1 ? name + " (" + types.get(i) + ")" : name);
        }
    }

    /**
     * Zoom to features.
     *
     * @param features the features, in any CRS.
     * @throws Exception
     */
    public void zoomTo( SimpleFeatureCollection features ) throws Exception {
        if (!features.isEmpty()) {
            zoomTo(features.getBounds());
        }
    }

    /**
     * Zoom to the features of an Overpass result.
     *
     * @param result the result.
     * @throws Exception
     */
    public void zoomTo( OverpassResult result ) throws Exception {
        Envelope envelope = new Envelope();
        for( SimpleFeatureCollection features : new SimpleFeatureCollection[]{result.getPoints(), result.getLines(),
                result.getPolygons()} ) {
            if (!features.isEmpty()) {
                envelope.expandToInclude(features.getBounds());
            }
        }
        if (!envelope.isNull()) {
            zoomTo(envelope);
        }
    }

    /**
     * Zoom to an envelope.
     *
     * @param envelope the envelope, in any CRS: without one it is taken as longitude/latitude.
     * @throws Exception
     */
    public void zoomTo( ReferencedEnvelope envelope ) throws Exception {
        onEdt(() -> {
            context.goTo(envelope);
            return null;
        });
    }

    /**
     * Zoom to an envelope in longitude/latitude.
     *
     * @param lonLatEnvelope the envelope.
     * @throws Exception
     */
    public void zoomTo( Envelope lonLatEnvelope ) throws Exception {
        zoomTo(new ReferencedEnvelope(lonLatEnvelope, NwwUtilities.GPS_CRS));
    }

    /**
     * @return the area shown, in longitude/latitude, or <code>null</code> when the whole globe is
     *          visible.
     * @throws Exception
     */
    public ReferencedEnvelope visibleArea() throws Exception {
        return onEdt(() -> context.getViewportBounds());
    }

    /**
     * @return the area shown as an Overpass bounding box: <code>(south,west,north,east)</code>.
     * @throws IllegalStateException when the whole globe is visible.
     * @throws Exception
     */
    public String overpassBbox() throws Exception {
        ReferencedEnvelope area = visibleArea();
        if (area == null) {
            throw new IllegalStateException("The whole globe is visible: zoom in to the area of interest first.");
        }
        return String.format(Locale.ROOT, "(%.6f,%.6f,%.6f,%.6f)", area.getMinY(), area.getMinX(), area.getMaxY(),
                area.getMaxX());
    }

    /**
     * Load a file as a layer, as if it were dropped on the layers panel.
     *
     * @param path the path of the file.
     * @throws Exception
     */
    public void loadFile( String path ) throws Exception {
        File file = new File(path);
        if (!file.exists()) {
            throw new IllegalArgumentException("The file doesn't exist: " + path);
        }
        onEdt(() -> {
            context.loadFiles(List.of(file));
            return null;
        });
    }

    /**
     * @return the features selected by the user, with a click or a box, in longitude/latitude,
     *          <code>null</code> if none is selected.
     * @throws Exception
     */
    public SimpleFeatureCollection selection() throws Exception {
        return onEdt(() -> {
            FeatureSelection selection = context.getSelection();
            return selection == null ? null : selection.toFeatureCollection();
        });
    }

    /**
     * @return the name of the layer of the selected features, <code>null</code> if none is
     *          selected.
     * @throws Exception
     */
    public String selectionLayer() throws Exception {
        return onEdt(() -> {
            FeatureSelection selection = context.getSelection();
            return selection == null || selection.isEmpty() ? null : selection.getLayer().getName();
        });
    }

    /**
     * Clear the selected features.
     *
     * @throws Exception
     */
    public void clearSelection() throws Exception {
        onEdt(() -> {
            context.clearSelection();
            return null;
        });
    }

    /**
     * Run in the event thread and wait, throwing the exceptions of the call.
     */
    private static <T> T onEdt( Callable<T> callable ) throws Exception {
        if (SwingUtilities.isEventDispatchThread()) {
            return callable.call();
        }
        Object[] result = new Object[1];
        try {
            SwingUtilities.invokeAndWait(() -> {
                try {
                    result[0] = callable.call();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException && cause.getCause() instanceof Exception) {
                throw (Exception) cause.getCause();
            }
            throw cause instanceof Exception ? (Exception) cause : new RuntimeException(cause);
        }
        @SuppressWarnings("unchecked")
        T value = (T) result[0];
        return value;
    }

    @Override
    public String toString() {
        return "viewer: the map of the 3D viewer";
    }
}
