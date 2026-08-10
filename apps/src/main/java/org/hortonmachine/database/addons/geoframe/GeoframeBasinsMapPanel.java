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
package org.hortonmachine.database.addons.geoframe;

import java.awt.BorderLayout;
import java.awt.Graphics2D;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JPanel;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ReprojectingFeatureCollection;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.renderer.GTRenderer;
import org.geotools.swing.JMapPane;
import org.geotools.swing.RenderingExecutor;
import org.geotools.swing.RenderingExecutorEvent;
import org.geotools.swing.RenderingExecutorListener;
import org.geotools.swing.RenderingOperands;
import org.geotools.swing.RenderingTask;
import org.geotools.swing.action.NoToolAction;
import org.geotools.swing.action.PanAction;
import org.geotools.swing.action.ZoomInAction;
import org.geotools.swing.action.ZoomOutAction;
import org.geotools.swing.event.MapMouseEvent;
import org.geotools.swing.tool.CursorTool;
import org.geotools.swing.tool.ScrollWheelTool;
import org.hortonmachine.gears.utils.crs.CrsUtilities;
import org.hortonmachine.gears.utils.style.HMStyle;
import org.hortonmachine.gui.utils.HMMapRenderer;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

/**
 * A small interactive map showing all basins of a GeoFrame run (numbered by their id) and the
 * stream network over an OSM basemap, with the currently displayed basin highlighted. Activating
 * the arrow ("Select") tool and clicking a basin polygon notifies the supplied listener with the
 * clicked basin id.
 * <p>
 * The {@link MapContent} (and its OSM tile layer) is built exactly once: switching the selected
 * basin only swaps the basins layer's {@link Style} in place, so it neither re-fetches OSM tiles
 * nor resets whatever pan/zoom the user has set up on the map. That restyle is additionally
 * rendered synchronously (see {@link SynchronousRenderingExecutor}) so the pane never shows a
 * cleared/blank frame while GeoTools' normal asynchronous renderer catches up.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeBasinsMapPanel extends JPanel {
    private static final long serialVersionUID = 1L;

    /**
     * The id column of the 'basin' table is renamed to 'origid' by
     * {@link org.hortonmachine.gears.io.dbs.DbsHelper#runRawSqlToFeatureCollection}, which avoids
     * clashing with the geometry-column-derived feature id.
     */
    private static final String BASIN_ID_FIELD = "origid";

    private static final GeometryFactory GF = new GeometryFactory();

    private final SimpleFeatureCollection basins;
    private final JMapPane mapPane;
    private final FeatureLayer basinsLayer;
    private int selectedBasinId;

    public GeoframeBasinsMapPanel( SimpleFeatureCollection basins, SimpleFeatureCollection network,
            SimpleFeatureCollection streamGauges, SimpleFeatureCollection meteoStations, int initialSelectedBasinId,
            IntConsumer onBasinSelected ) {
        super(new BorderLayout());
        // reproject to the OSM basemap's CRS (EPSG:3857) so basins/network/stations line up with
        // the tiles and click hit-testing (which works in the map pane's world coordinates) is
        // consistent
        this.basins = new ReprojectingFeatureCollection(basins, CrsUtilities.getCrsFromSrid(3857));
        this.selectedBasinId = initialSelectedBasinId;

        HMMapRenderer renderer = new HMMapRenderer();
        // renderer.addOsmBackground();
        // the basins layer must be added before the others so it ends up first in the feature
        // layer list (see findBasinsLayer()); network and stations are added on top so they stay
        // visible over the (semi-transparent) basin fills
        renderer.addLayer(this.basins, buildStyle(selectedBasinId));
        if (network != null) {
            renderer.addLayer(reproject(network), HMStyle.line().stroke("#0b3d91", 2.2));
        }
        if (streamGauges != null) {
            renderer.addLayer(reproject(streamGauges),
                    HMStyle.point().type("circle").size(13).color("#d40000").stroke("#7a0000", 1.8));
        }
        if (meteoStations != null) {
            renderer.addLayer(reproject(meteoStations),
                    HMStyle.point().type("circle").size(13).color("#0055ff").stroke("#00266b", 1.8));
        }
        MapContent mapContent = renderer.createMapContent("Basins");
        basinsLayer = findBasinsLayer(mapContent);

        mapPane = new JMapPane(mapContent);
        mapPane.addMouseListener(new ScrollWheelTool(mapPane));

        CursorTool selectTool = new CursorTool(){
            @Override
            public void onMouseClicked( MapMouseEvent event ) {
                Integer basinId = findBasinAt(event);
                if (basinId != null && onBasinSelected != null) {
                    onBasinSelected.accept(basinId);
                }
            }
        };
        mapPane.setCursorTool(selectTool);

        add(buildToolBar(selectTool), BorderLayout.NORTH);
        add(mapPane, BorderLayout.CENTER);
    }

    /**
     * Updates the highlighted basin. Only the basins layer's style is swapped in place, rendered
     * synchronously so the pane never flashes a blank frame - the map's pan/zoom position and the
     * OSM tiles are left untouched.
     */
    public void setSelectedBasin( int basinId ) {
        if (basinId == selectedBasinId) {
            return;
        }
        selectedBasinId = basinId;
        if (basinsLayer != null) {
            RenderingExecutor liveExecutor = mapPane.getRenderingExecutor();
            mapPane.setRenderingExecutor(new SynchronousRenderingExecutor());
            try {
                basinsLayer.setStyle(buildStyle(basinId).build());
            } finally {
                mapPane.setRenderingExecutor(liveExecutor);
            }
        }
    }

    private static SimpleFeatureCollection reproject( SimpleFeatureCollection featureCollection ) {
        return new ReprojectingFeatureCollection(featureCollection, CrsUtilities.getCrsFromSrid(3857));
    }

    private static FeatureLayer findBasinsLayer( MapContent mapContent ) {
        for( Layer layer : mapContent.layers() ) {
            if (layer instanceof FeatureLayer) {
                return (FeatureLayer) layer;
            }
        }
        return null;
    }

    /**
     * HMStyle only emits a rule for filters registered via where(...): a style built from a
     * single conditional rule (e.g. just "id = X") renders ONLY the features matching it, since
     * unmatched features fall through every rule in the FeatureTypeStyle. To make sure every
     * basin is drawn, the two filters here are exhaustive and mutually exclusive, so each basin
     * matches exactly one of the two rules. The label is set unconditionally (no where() call
     * after it) so it is used as the fallback text symbolizer for both rules.
     */
    private HMStyle buildStyle( int selectedBasinId ) {
        return HMStyle.polygon() //
                .fill("#33cc66", 0.35).stroke("#1a8a3f", 1.3).where(BASIN_ID_FIELD + " <> " + selectedBasinId) //
                .fill("#ff8800", 0.65).stroke("#cc3300", 2.5).where(BASIN_ID_FIELD + " = " + selectedBasinId) //
                .label(HMStyle.label(BASIN_ID_FIELD).fill("black").halo("white", 1.5));
    }

    private Integer findBasinAt( MapMouseEvent event ) {
        var worldPos = event.getWorldPos();
        Point point = GF.createPoint(new Coordinate(worldPos.x, worldPos.y));
        try (SimpleFeatureIterator iterator = basins.features()) {
            while( iterator.hasNext() ) {
                SimpleFeature feature = iterator.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                if (geometry != null && geometry.contains(point)) {
                    Object idValue = feature.getAttribute(BASIN_ID_FIELD);
                    if (idValue instanceof Number) {
                        return ((Number) idValue).intValue();
                    }
                }
            }
        }
        return null;
    }

    private JToolBar buildToolBar( CursorTool selectTool ) {
        JToolBar toolBar = new JToolBar();
        toolBar.setFloatable(false);

        ButtonGroup group = new ButtonGroup();

        Action selectAction = new AbstractAction("", new ImageIcon(NoToolAction.class.getResource(NoToolAction.ICON_IMAGE))){
            private static final long serialVersionUID = 1L;

            @Override
            public void actionPerformed( java.awt.event.ActionEvent e ) {
                mapPane.setCursorTool(selectTool);
            }
        };
        selectAction.putValue(Action.SHORT_DESCRIPTION, "Select a basin");
        JToggleButton selectButton = new JToggleButton(selectAction);
        selectButton.setSelected(true);
        group.add(selectButton);
        toolBar.add(selectButton);

        JToggleButton panButton = new JToggleButton(new PanAction(mapPane));
        group.add(panButton);
        toolBar.add(panButton);

        JToggleButton zoomInButton = new JToggleButton(new ZoomInAction(mapPane));
        group.add(zoomInButton);
        toolBar.add(zoomInButton);

        JToggleButton zoomOutButton = new JToggleButton(new ZoomOutAction(mapPane));
        group.add(zoomOutButton);
        toolBar.add(zoomOutButton);

        return toolBar;
    }

    /**
     * Runs the render task on the calling thread instead of GeoTools' default background-thread
     * pool (polled every ~20ms). {@link org.geotools.swing.AbstractMapPane#layerChanged} clears
     * the pane's buffer to the background color and calls {@code repaint()} *before* the
     * asynchronous render finishes, which is what causes a visible blank flash when only a small,
     * already-cached restyle is involved. Running synchronously means the buffer already holds
     * the finished image by the time that repaint is processed, so no blank frame is ever shown.
     * Only used for the (fast, local, no-network) basin highlight restyle - normal map
     * interaction keeps using the default asynchronous executor.
     */
    private static class SynchronousRenderingExecutor implements RenderingExecutor {
        private long pollingInterval = 20L;
        private final AtomicLong nextId = new AtomicLong(1);
        private volatile boolean shutdown;

        @Override
        public long getPollingInterval() {
            return pollingInterval;
        }

        @Override
        public void setPollingInterval( long interval ) {
            if (interval > 0) {
                pollingInterval = interval;
            }
        }

        @Override
        public long submit( MapContent mapContent, GTRenderer renderer, Graphics2D graphics, RenderingExecutorListener listener ) {
            if (shutdown) {
                throw new IllegalStateException("Calling submit after the executor has been shutdown");
            }
            long id = nextId.getAndIncrement();
            listener.onRenderingStarted(new RenderingExecutorEvent(this, id));
            boolean ok;
            try {
                ok = new RenderingTask(mapContent, graphics, renderer).call();
            } catch (Exception e) {
                ok = false;
            }
            RenderingExecutorEvent doneEvent = new RenderingExecutorEvent(this, id);
            if (ok) {
                listener.onRenderingCompleted(doneEvent);
            } else {
                listener.onRenderingFailed(doneEvent);
            }
            return id;
        }

        @Override
        public long submit( MapContent mapContent, List<RenderingOperands> operands, RenderingExecutorListener listener ) {
            throw new UnsupportedOperationException("Not used by GeoframeBasinsMapPanel");
        }

        @Override
        public void cancel( long taskId ) {
        }

        @Override
        public void cancelAll() {
        }

        @Override
        public void shutdown() {
            shutdown = true;
        }

        @Override
        public boolean isShutdown() {
            return shutdown;
        }
    }
}
