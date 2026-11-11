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
package org.hortonmachine.nww.gui;

import java.awt.Color;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.gce.geotiff.GeoTiffReader;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.assets.handlers.GeotiffHandler;
import org.hortonmachine.gears.io.stac.auth.HMStacAccess;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.ui.progress.ProgressUpdate;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.executor.ExecutorProgressGui;
import org.hortonmachine.nww.layers.defaults.NwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.stac.StacBrowser;
import org.hortonmachine.stac.StacBrowser.SelectedAsset;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.geom.Sector;
import gov.nasa.worldwind.layers.Layer;

/**
 * Adds the assets of STAC catalogs to the viewer, chosen in the STAC browser, which the viewer
 * opens with an action to add the selected assets.
 *
 * <ul>
 * <li>GeoTIFF rasters served over http(s) without authentication are read as cloud optimized
 * GeoTIFFs: each tile of the map reads only its part, from the overview of its zoom.</li>
 * <li>The other rasters, as the protected or S3 ones, are downloaded to a temporary file, then
 * drawn as the raster files.</li>
 * <li>Vectors are read whole and drawn as the other vector layers, with the style saved in the
 * {@link LocalStyleStore}, if any.</li>
 * </ul>
 *
 * <p>The rasters with a style asset of the same file name, as the HortonMachine server gives,
 * are drawn with it.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class NwwStacLoader {

    private static final String CONSUMER_NAME = "3D viewer";

    /**
     * A layer made from an asset, with the source attached, or the reason it could not be made.
     */
    private record LoadedAsset( SelectedAsset selected, Layer layer, SimpleFeatureCollection features, String problem ) {
    }

    private final NwwPanel wwjPanel;
    private final NwwLayerLoader layerLoader;
    /** The window of the browser, if open. */
    private JFrame browserFrame;

    /**
     * @param wwjPanel the map.
     * @param layerLoader the loader adding the layers.
     */
    public NwwStacLoader( NwwPanel wwjPanel, NwwLayerLoader layerLoader ) {
        this.wwjPanel = wwjPanel;
        this.layerLoader = layerLoader;
    }

    /**
     * Open the STAC browser, or bring it to the front if open, with the action to add the
     * selected assets to the viewer.
     *
     * <p>To be called from the event dispatch thread.</p>
     */
    public void openBrowser() {
        if (browserFrame != null && browserFrame.isDisplayable()) {
            browserFrame.toFront();
            return;
        }
        StacBrowser browser = new StacBrowser();
        browser.setAssetsConsumer(CONSUMER_NAME, this::addAssets);
        JFrame frame = new DefaultGuiBridgeImpl().showWindow(browser, "STAC Browser - right click assets to add them to the viewer");
        GuiUtilities.setDefaultFrameIcon(frame);
        // not the closing listener of the applications, which exits: only the browser is closed
        frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        frame.addWindowListener(new WindowAdapter(){
            @Override
            public void windowClosed( WindowEvent e ) {
                browser.onClose();
                browserFrame = null;
            }
        });
        browserFrame = frame;
    }

    /**
     * Make the layers of the assets in the background and add them to the map.
     *
     * @param assets the assets selected in the browser.
     */
    private void addAssets( List<SelectedAsset> assets ) {
        new ExecutorProgressGui(assets.size()){
            private final List<LoadedAsset> loaded = new ArrayList<>();

            @Override
            public void backGroundWork() throws Exception {
                for( int i = 0; i < assets.size(); i++ ) {
                    SelectedAsset selected = assets.get(i);
                    publish(new ProgressUpdate("Reading " + selected.name() + " (" + (i + 1) + "/" + assets.size() + ") ...", i));
                    try {
                        if (selected.raster()) {
                            loaded.add(new LoadedAsset(selected, makeRasterLayer(selected), null, null));
                        } else {
                            SimpleFeatureCollection features = readVector(selected);
                            loaded.add(new LoadedAsset(selected, null, features,
                                    features == null ? "the asset contains no features" : null));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        loaded.add(new LoadedAsset(selected, null, null, e.getMessage()));
                    }
                }
            }

            @Override
            public void done() {
                super.done();
                List<String> problems = new ArrayList<>();
                Envelope addedBounds = new Envelope();
                for( LoadedAsset asset : loaded ) {
                    String name = asset.selected().name();
                    try {
                        if (asset.problem() != null) {
                            problems.add(name + ": " + asset.problem());
                        } else if (asset.layer() != null) {
                            layerLoader.addLayer(asset.layer());
                            expand(addedBounds, asset.layer());
                        } else {
                            LayerSource source = LayerSource.ofWebService(asset.selected().href(), name, null);
                            Layer layer = layerLoader.loadFeatureCollection(source.path(), asset.features(), source,
                                    LocalStyleStore.read(source));
                            if (layer == null) {
                                problems.add(name + ": unsupported geometry type");
                            } else {
                                addedBounds.expandToInclude(asset.features().getBounds());
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        problems.add(name + ": " + e.getMessage());
                    }
                }
                goToIfOutOfView(addedBounds);
                if (!problems.isEmpty()) {
                    GuiUtilities.showWarningMessage(wwjPanel, "WARNING",
                            "Some assets were not added:\n" + String.join("\n", problems));
                }
            }
        }.execute();
    }

    private static void expand( Envelope bounds, Layer layer ) {
        if (layer instanceof NwwLayer) {
            Envelope layerBounds = ((NwwLayer) layer).getBounds();
            if (layerBounds != null) {
                bounds.expandToInclude(layerBounds);
            }
        }
    }

    /**
     * Fly to the assets added if none is in the map view, as when they were chosen in the
     * browser on another area.
     */
    private void goToIfOutOfView( Envelope bounds ) {
        if (bounds.isNull()) {
            return;
        }
        Envelope view = wwjPanel.getViewportBounds();
        if (view == null || !view.intersects(bounds)) {
            wwjPanel.goTo(Sector.fromDegrees(bounds.getMinY(), bounds.getMaxY(), bounds.getMinX(), bounds.getMaxX()), true);
        }
    }

    /**
     * Make the layer of a raster asset: read in parts as a cloud optimized GeoTIFF if it can,
     * else downloaded whole.
     */
    private static Layer makeRasterLayer( SelectedAsset selected ) throws Exception {
        String href = selected.href();
        HMStacAsset asset = selected.asset();
        HMStacAccess access = asset.getAccess();
        boolean http = href.startsWith("http://") || href.startsWith("https://");
        boolean authenticated = access != null && access.isAuthenticated(href);
        if (http && !authenticated && asset.getHandler() instanceof GeotiffHandler) {
            LayerSource source = LayerSource.ofWebService(href, selected.name(), null);
            // the style saved in the viewer, else the one of the catalog
            Style style = LocalStyleStore.read(source);
            if (style == null) {
                style = readStyle(selected.style());
            }
            GeoTiffReader reader = GeotiffHandler.createCogReader(href, null, null);
            GridCoverageNwwLayer layer = new GridCoverageNwwLayer(reader, selected.name(), href, null, Color.WHITE, style);
            layer.setName(source.path());
            source.attachTo(layer);
            return layer;
        }
        // read whole: protected, S3 and non GeoTIFF rasters, as the ASCII grids; the style is the
        // .sld saved next to the download, else the one of the catalog
        File file = download(selected);
        Style style = SldUtilities.getStyleFile(file) != null ? null : readStyle(selected.style());
        GridCoverageNwwLayer layer = new GridCoverageNwwLayer(file, null, Color.WHITE, style);
        layer.setName(selected.name());
        // a file layer, which the raster tools handle
        new LayerSource(file, null).attachTo(layer);
        return layer;
    }

    /**
     * @return the style of a style asset, or <code>null</code> if there is none or it can't be
     *          read.
     */
    private static Style readStyle( HMStacAsset styleAsset ) {
        if (styleAsset == null) {
            return null;
        }
        try {
            return styleAsset.getHandler().read(Style.class, new DummyProgressMonitor());
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Download an asset to a temporary folder, named after its file.
     *
     * @return the downloaded file.
     */
    private static File download( SelectedAsset selected ) throws Exception {
        File folder = new File(System.getProperty("java.io.tmpdir"), "hm-stac-assets");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalArgumentException("Unable to create the folder of the downloads: " + folder);
        }
        String href = selected.href();
        int queryIndex = href.indexOf('?');
        String path = queryIndex >= 0 ? href.substring(0, queryIndex) : href;
        String fileName = path.substring(path.lastIndexOf('/') + 1);
        if (fileName.isBlank()) {
            fileName = selected.asset().getId() + ".tif";
        }
        File file = new File(folder, Integer.toHexString(href.hashCode()) + "_" + fileName.replaceAll("[^A-Za-z0-9._-]", "_"));
        if (!file.exists()) {
            selected.asset().getHandler().downloadAsset(file.getAbsolutePath(), new DummyProgressMonitor());
        }
        return file;
    }

    /**
     * @return the features of a vector asset in WGS84 lon/lat, or <code>null</code> if there are
     *          none.
     */
    private static SimpleFeatureCollection readVector( SelectedAsset selected ) throws Exception {
        SimpleFeatureCollection features = selected.asset().getHandler().read(SimpleFeatureCollection.class,
                new DummyProgressMonitor());
        if (features == null || features.isEmpty()) {
            return null;
        }
        CoordinateReferenceSystem crs = features.getSchema().getCoordinateReferenceSystem();
        if (crs != null && !HMCrsRegistry.crsEquals(crs, NwwUtilities.GPS_CRS)) {
            features = new HMCrsTransformer(crs, NwwUtilities.GPS_CRS).transform(features);
        }
        return features;
    }
}
