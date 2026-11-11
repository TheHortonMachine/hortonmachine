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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import org.geotools.api.data.ResourceInfo;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.Style;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.wcs.ICoverageSummary;
import org.hortonmachine.gears.io.wcs.Wcs;
import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.hortonmachine.gears.io.wfs.Wfs;
import org.hortonmachine.gears.ui.progress.ProgressUpdate;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.executor.ExecutorProgressGui;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.webmaps.WebServicesBrowser;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.avlist.AVKey;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.avlist.AVList;
import gov.nasa.worldwind.avlist.AVListImpl;
import gov.nasa.worldwind.ogc.wms.WMSCapabilities;
import gov.nasa.worldwind.ogc.wms.WMSLayerCapabilities;
import gov.nasa.worldwind.wms.WMSTiledImageLayer;

/**
 * Loads the layers of web services as layers of the viewer, from the services used in the web
 * services browser, or from a new address, which is then also proposed by the browser.
 *
 * <ul>
 * <li>WMS: the layers are images requested from the service as the map moves.</li>
 * <li>WFS: the features are read once, whole or in the current map view, and drawn as the other
 * vector layers, with the style saved in the {@link LocalStyleStore}, if any.</li>
 * <li>WCS: the coverages are downloaded once in the current map view, at about the resolution
 * of the screen, to temporary GeoTIFFs drawn as the raster files.</li>
 * </ul>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class NwwWebServicesLoader {

    private static final String WMS = "WMS";
    private static final String WFS = "WFS";
    private static final String WCS = "WCS";

    /** The fewest and most cells across a downloaded coverage. */
    private static final int MIN_COVERAGE_CELLS = 64;
    private static final int MAX_COVERAGE_CELLS = 4096;

    /** The timeouts of the requests to the WMS services, some are slow. */
    private static final int WMS_TIMEOUT_MILLIS = 30000;

    /** The points added on each side of the view extent, to reproject it to the layer CRS. */
    private static final int ENVELOPE_DENSIFICATION_POINTS = 10;

    /**
     * The features read from a WFS layer.
     */
    private record ReadLayer( String typeName, SimpleFeatureCollection features ) {
    }

    private final NwwPanel wwjPanel;
    private final NwwLayerLoader layerLoader;

    /**
     * @param wwjPanel the map, giving the extent of the view.
     * @param layerLoader the loader adding the layers.
     */
    public NwwWebServicesLoader( NwwPanel wwjPanel, NwwLayerLoader layerLoader ) {
        this.wwjPanel = wwjPanel;
        this.layerLoader = layerLoader;
    }

    /**
     * Let the user choose a service among those used in the web services browser, or type a new
     * one.
     *
     * @param type the type of the service.
     * @return the address of the service, or <code>null</code> if cancelled.
     */
    private String chooseService( String type ) {
        JComboBox<String> urlCombo = new JComboBox<>(WebServicesBrowser.getSavedUrls(type).toArray(new String[0]));
        urlCombo.setEditable(true);
        urlCombo.setPreferredSize(new Dimension(560, urlCombo.getPreferredSize().height));
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.add(new JLabel("The address of the " + type + " service, as the ones used in the web services browser:"),
                BorderLayout.NORTH);
        panel.add(urlCombo, BorderLayout.CENTER);
        int result = JOptionPane.showConfirmDialog(wwjPanel, panel, type + " service", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        Object item = urlCombo.getEditor().getItem();
        String url = item == null ? "" : item.toString().trim();
        return url.isEmpty() ? null : url;
    }

    // ==================== WMS ====================

    /**
     * Let the user choose a WMS service and its layers, and add them to the map.
     *
     * <p>To be called from the event dispatch thread.</p>
     */
    public void openWms() {
        String url = chooseService(WMS);
        if (url == null) {
            return;
        }
        new ExecutorProgressGui(1){
            private WMSCapabilities caps;

            @Override
            public void backGroundWork() throws Exception {
                publish(new ProgressUpdate("Reading the capabilities of " + LayerSource.serviceHost(url) + " ...", 0));
                WMSCapabilities capabilities = WMSCapabilities.retrieve(new URI(url));
                capabilities.parse();
                caps = capabilities;
            }

            @Override
            public void done() {
                super.done();
                if (caps != null) {
                    WebServicesBrowser.saveUrl(WMS, url);
                    chooseWmsLayers(url, caps);
                }
            }
        }.execute();
    }

    private void chooseWmsLayers( String url, WMSCapabilities caps ) {
        List<WMSLayerCapabilities> layers = caps.getNamedLayers();
        if (layers == null || layers.isEmpty()) {
            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "The service has no layers: " + url);
            return;
        }
        List<String> titles = new ArrayList<>();
        List<String> names = new ArrayList<>();
        for( WMSLayerCapabilities layer : layers ) {
            titles.add(title(layer));
            names.add(layer.getName());
        }
        TablesSelectionDialog.Result result = TablesSelectionDialog.show(wwjPanel, "WMS layers",
                "Select the layers to add from " + LayerSource.serviceHost(url) + ":", "Layer", titles, "Name", names, false,
                null, null, false);
        if (result == null || !result.hasSelection()) {
            return;
        }
        List<String> problems = new ArrayList<>();
        for( int i = 0; i < layers.size(); i++ ) {
            if (!result.selected()[i]) {
                continue;
            }
            WMSLayerCapabilities layerCaps = layers.get(i);
            try {
                AVList params = new AVListImpl();
                params.setValue(AVKey.LAYER_NAMES, layerCaps.getName());
                params.setValue(AVKey.URL_CONNECT_TIMEOUT, WMS_TIMEOUT_MILLIS);
                params.setValue(AVKey.URL_READ_TIMEOUT, WMS_TIMEOUT_MILLIS);
                params.setValue(AVKey.RETRIEVAL_QUEUE_STALE_REQUEST_LIMIT, 2 * WMS_TIMEOUT_MILLIS);
                WMSTiledImageLayer layer = new WMSTiledImageLayer(caps, params);
                // the title, with the host smaller, as the tables
                LayerSource source = LayerSource.ofWebService(url, title(layerCaps), null);
                layer.setName(source.path());
                source.attachTo(layer);
                layerLoader.addLayer(layer);
            } catch (Exception e) {
                e.printStackTrace();
                problems.add(title(layerCaps) + ": " + e.getMessage());
            }
        }
        if (!problems.isEmpty()) {
            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "Some layers were not added:\n" + String.join("\n", problems));
        }
    }

    private static String title( WMSLayerCapabilities layer ) {
        String title = layer.getTitle();
        return title != null && !title.isBlank() ? title : layer.getName();
    }

    // ==================== WFS ====================

    /**
     * Let the user choose a WFS service and its layers, and load their features, whole or in the
     * current map view.
     *
     * <p>To be called from the event dispatch thread.</p>
     */
    public void openWfs() {
        String url = chooseService(WFS);
        if (url == null) {
            return;
        }
        new ExecutorProgressGui(1){
            private List<String> typeNames;
            private List<String> titles;

            @Override
            public void backGroundWork() throws Exception {
                publish(new ProgressUpdate("Reading the capabilities of " + LayerSource.serviceHost(url) + " ...", 0));
                try (Wfs wfs = connectWfs(url)) {
                    List<String> names = wfs.getTypeNames();
                    List<String> layerTitles = new ArrayList<>();
                    for( String name : names ) {
                        layerTitles.add(wfsTitle(wfs, name));
                    }
                    titles = layerTitles;
                    typeNames = names;
                }
            }

            @Override
            public void done() {
                super.done();
                if (typeNames != null) {
                    WebServicesBrowser.saveUrl(WFS, url);
                    chooseAndLoadWfsLayers(url, typeNames, titles);
                }
            }
        }.execute();
    }

    private static Wfs connectWfs( String url ) throws Exception {
        Wfs wfs = new Wfs(url);
        // the geometry of the features gets the type of the actual geometries, as the drawing needs
        wfs.forceNormalizeGeometryName();
        wfs.connect();
        return wfs;
    }

    /**
     * @return the title of a layer, from the capabilities read, empty if it has none other than
     *          the name.
     */
    private static String wfsTitle( Wfs wfs, String typeName ) {
        try {
            ResourceInfo info = wfs.getTypeInfo(typeName);
            String title = info != null ? info.getTitle() : null;
            return title != null && !title.equals(typeName) ? title : "";
        } catch (Exception e) {
            return "";
        }
    }

    private void chooseAndLoadWfsLayers( String url, List<String> typeNames, List<String> titles ) {
        if (typeNames.isEmpty()) {
            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "The service has no layers: " + url);
            return;
        }
        TablesSelectionDialog.Result result = TablesSelectionDialog.show(wwjPanel, "WFS layers",
                "Select the layers to load from " + LayerSource.serviceHost(url) + ":", "Layer", typeNames, "Title", titles,
                false, "Load only the features in the current map view",
                "If not selected, all the features of the layers are loaded, which can take long for big layers", false);
        if (result == null || !result.hasSelection()) {
            return;
        }
        ReferencedEnvelope viewEnvelope = null;
        if (result.optionChecked()) {
            viewEnvelope = wwjPanel.getViewportBounds();
            if (viewEnvelope == null) {
                GuiUtilities.showWarningMessage(wwjPanel, "WARNING",
                        "The extent of the map view can't be computed, as when the sky is visible.\n"
                                + "Tilt the view down, or load the whole layers.");
                return;
            }
        }
        List<String> selectedTypes = new ArrayList<>();
        for( int i = 0; i < typeNames.size(); i++ ) {
            if (result.selected()[i]) {
                selectedTypes.add(typeNames.get(i));
            }
        }

        Envelope _viewEnvelope = viewEnvelope;
        new ExecutorProgressGui(selectedTypes.size()){
            private final List<ReadLayer> readLayers = new ArrayList<>();
            private final List<String> problems = new ArrayList<>();

            @Override
            public void backGroundWork() throws Exception {
                try (Wfs wfs = connectWfs(url)) {
                    for( int i = 0; i < selectedTypes.size(); i++ ) {
                        String typeName = selectedTypes.get(i);
                        publish(new ProgressUpdate("Reading " + typeName + " (" + (i + 1) + "/" + selectedTypes.size() + ") ...",
                                i));
                        try {
                            SimpleFeatureCollection features = readWfsLayer(wfs, typeName, _viewEnvelope);
                            if (features == null) {
                                problems.add(typeName + ": no features" + (_viewEnvelope != null ? " in the map view" : ""));
                            } else {
                                readLayers.add(new ReadLayer(typeName, features));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            problems.add(typeName + ": " + e.getMessage());
                        }
                    }
                }
            }

            @Override
            public void done() {
                super.done();
                for( ReadLayer readLayer : readLayers ) {
                    LayerSource source = LayerSource.ofWebService(url, readLayer.typeName(), _viewEnvelope);
                    try {
                        Style style = LocalStyleStore.read(source);
                        if (layerLoader.loadFeatureCollection(source.path(), readLayer.features(), source, style) == null) {
                            problems.add(readLayer.typeName() + ": unsupported geometry type");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        problems.add(readLayer.typeName() + ": " + e.getMessage());
                    }
                }
                if (!problems.isEmpty()) {
                    GuiUtilities.showWarningMessage(wwjPanel, "WARNING",
                            "Some layers were not loaded:\n" + String.join("\n", problems));
                }
            }
        }.execute();
    }

    // ==================== WCS ====================

    /**
     * A coverage of a WCS service, as listed in its capabilities.
     *
     * @param id the id of the coverage.
     * @param title the title, empty if it has none.
     * @param wgs84 the extent in WGS84 lon/lat, <code>null</code> if not given.
     * @param srid the native srid, <code>null</code> if not given.
     */
    private record WcsCoverage( String id, String title, Envelope wgs84, Integer srid ) {
    }

    /**
     * A coverage downloaded in the map view, drawn as a raster file.
     */
    private record ReadCoverage( WcsCoverage coverage, Layer layer ) {
    }

    /**
     * Let the user choose a WCS service and its coverages, and download them in the current map
     * view, at about the resolution of the screen, to temporary GeoTIFFs drawn as raster files.
     *
     * <p>To be called from the event dispatch thread.</p>
     */
    public void openWcs() {
        String url = chooseService(WCS);
        if (url == null) {
            return;
        }
        new ExecutorProgressGui(1){
            private Wcs wcs;
            private List<WcsCoverage> coverages;

            @Override
            public void backGroundWork() throws Exception {
                publish(new ProgressUpdate("Reading the capabilities of " + LayerSource.serviceHost(url) + " ...", 0));
                Wcs service = new Wcs(url);
                List<WcsCoverage> list = new ArrayList<>();
                for( String id : service.ids() ) {
                    ICoverageSummary summary = service.summary(id);
                    String title = summary != null && summary.getTitle() != null && !summary.getTitle().equals(id)
                            ? summary.getTitle()
                            : "";
                    Envelope wgs84 = null;
                    if (summary != null && summary.getWgs84BoundingBox() != null && !summary.getWgs84BoundingBox().isEmpty()) {
                        ReferencedEnvelope b = summary.getWgs84BoundingBox();
                        wgs84 = new Envelope(b.getMinX(), b.getMaxX(), b.getMinY(), b.getMaxY());
                    }
                    list.add(new WcsCoverage(id, title, wgs84, summary != null ? summary.getBoundingBoxSrid() : null));
                }
                coverages = list;
                wcs = service;
            }

            @Override
            public void done() {
                super.done();
                if (coverages != null) {
                    WebServicesBrowser.saveUrl(WCS, url);
                    chooseAndDownloadCoverages(url, wcs, coverages);
                }
            }
        }.execute();
    }

    private void chooseAndDownloadCoverages( String url, Wcs wcs, List<WcsCoverage> coverages ) {
        if (coverages.isEmpty()) {
            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "The service has no coverages: " + url);
            return;
        }
        ReferencedEnvelope view = wwjPanel.getViewportBounds();
        if (view == null) {
            GuiUtilities.showWarningMessage(wwjPanel, "WARNING", "The coverages are downloaded in the map view, whose extent "
                    + "can't be computed, as when the sky is visible.\nTilt the view down and try again.");
            return;
        }
        List<String> ids = coverages.stream().map(WcsCoverage::id).toList();
        List<String> titles = coverages.stream().map(WcsCoverage::title).toList();
        TablesSelectionDialog.Result result = TablesSelectionDialog.show(wwjPanel, "WCS coverages",
                "<html>Select the coverages to download from " + LayerSource.serviceHost(url)
                        + ".<br>They are read in the current map view, at about the resolution of the screen.</html>",
                "Coverage", ids, "Title", titles, false, null, null, false);
        if (result == null || !result.hasSelection()) {
            return;
        }
        List<WcsCoverage> selected = new ArrayList<>();
        for( int i = 0; i < coverages.size(); i++ ) {
            if (result.selected()[i]) {
                selected.add(coverages.get(i));
            }
        }
        // the cells across the view, as the pixels of the map
        int viewCols = Math.max(1, wwjPanel.getWidth());

        new ExecutorProgressGui(selected.size()){
            private final List<ReadCoverage> read = new ArrayList<>();
            private final List<String> problems = new ArrayList<>();

            @Override
            public void backGroundWork() throws Exception {
                List<String> formats = wcs.formats();
                String tiffFormat = formats == null ? null
                        : formats.stream().filter(f -> f.toLowerCase().contains("tif")).findFirst().orElse(null);
                for( int i = 0; i < selected.size(); i++ ) {
                    WcsCoverage coverage = selected.get(i);
                    publish(new ProgressUpdate(
                            "Downloading " + coverage.id() + " (" + (i + 1) + "/" + selected.size() + ") ...", i));
                    try {
                        Layer layer = downloadCoverage(url, wcs, coverage, view, viewCols, tiffFormat);
                        if (layer == null) {
                            problems.add(coverage.id() + ": not in the map view");
                        } else {
                            read.add(new ReadCoverage(coverage, layer));
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        problems.add(coverage.id() + ": " + e.getMessage());
                    }
                }
            }

            @Override
            public void done() {
                super.done();
                for( ReadCoverage coverage : read ) {
                    layerLoader.addLayer(coverage.layer());
                }
                if (!problems.isEmpty()) {
                    GuiUtilities.showWarningMessage(wwjPanel, "WARNING",
                            "Some coverages were not added:\n" + String.join("\n", problems));
                }
            }
        }.execute();
    }

    /**
     * Download a coverage in the part of the map view it covers, at about the resolution of the
     * screen, to a temporary GeoTIFF, and make its layer.
     *
     * @return the layer, not added to the map, or <code>null</code> if the coverage is not in the
     *          view.
     */
    private static Layer downloadCoverage( String url, Wcs wcs, WcsCoverage coverage, Envelope view, int viewCols,
            String tiffFormat ) throws Exception {
        Envelope area = coverage.wgs84() != null ? view.intersection(coverage.wgs84()) : view;
        if (area.isNull() || area.getWidth() <= 0 || area.getHeight() <= 0) {
            return null;
        }
        // the region in the native CRS, if known, else in WGS84
        int srid = 4326;
        Envelope subset = area;
        if (coverage.srid() != null && coverage.srid() != 4326) {
            try {
                CoordinateReferenceSystem nativeCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:" + coverage.srid(), true);
                subset = new HMCrsTransformer(NwwUtilities.GPS_CRS, nativeCrs).transformDensified(area,
                        ENVELOPE_DENSIFICATION_POINTS);
                srid = coverage.srid();
            } catch (Exception e) {
                // unknown to HM: the region is sent in WGS84
            }
        }
        int cols = (int) Math.round(viewCols * area.getWidth() / view.getWidth());
        cols = Math.max(MIN_COVERAGE_CELLS, Math.min(MAX_COVERAGE_CELLS, cols));
        int rows = Math.max(1, (int) Math.round(cols * subset.getHeight() / subset.getWidth()));
        rows = Math.min(MAX_COVERAGE_CELLS, rows);

        CoverageReaderParameters parameters = wcs.getReaderParameters(coverage.id());
        if (tiffFormat != null) {
            parameters.format(tiffFormat);
        }
        parameters.bbox(subset, srid);
        parameters.rowsCols(rows, cols);

        File folder = new File(System.getProperty("java.io.tmpdir"), "hm-wcs");
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalArgumentException("Unable to create the folder of the downloads: " + folder);
        }
        // a name per coverage and area, so that each download is its own raster and tiles cache
        String key = url + "|" + coverage.id() + "|" + subset + "|" + cols + "x" + rows;
        File file = new File(folder,
                coverage.id().replaceAll("[^A-Za-z0-9._-]", "_") + "_" + Integer.toHexString(key.hashCode()) + ".tif");
        wcs.dumpCoverage(file.getAbsolutePath(), parameters);

        GridCoverageNwwLayer layer = new GridCoverageNwwLayer(file, null, Color.WHITE);
        layer.setName(coverage.id() + " (" + LayerSource.serviceHost(url) + ")");
        // a raster file, which the raster tools handle
        new LayerSource(file, null).attachTo(layer);
        return layer;
    }

    /**
     * Read the features of a WFS layer, in WGS84 lon/lat.
     *
     * @param wfs the connected service.
     * @param typeName the layer.
     * @param viewEnvelope the extent of the map view in WGS84 lon/lat, to read only the features
     *          in it, or <code>null</code> to read all of them.
     * @return the features, or <code>null</code> if there are none.
     */
    private static SimpleFeatureCollection readWfsLayer( Wfs wfs, String typeName, Envelope viewEnvelope ) throws Exception {
        wfs.setTypeName(typeName);
        SimpleFeatureType type = wfs.getSimpleFeatureType();
        CoordinateReferenceSystem crs = type.getCoordinateReferenceSystem();
        Envelope envelope = null;
        if (viewEnvelope != null) {
            // the filter is in the CRS of the layer, the borders of the view are not straight in it
            envelope = crs != null && !HMCrsRegistry.crsEquals(crs, NwwUtilities.GPS_CRS)
                    ? new HMCrsTransformer(NwwUtilities.GPS_CRS, crs).transformDensified(viewEnvelope,
                            ENVELOPE_DENSIFICATION_POINTS)
                    : viewEnvelope;
        }
        SimpleFeatureCollection readFC = wfs.getFeatureCollection(envelope);
        if (readFC.isEmpty()) {
            return null;
        }
        CoordinateReferenceSystem dataCrs = readFC.getSchema().getCoordinateReferenceSystem();
        if (dataCrs == null) {
            dataCrs = crs;
        }
        if (dataCrs != null && !HMCrsRegistry.crsEquals(dataCrs, NwwUtilities.GPS_CRS)) {
            readFC = new HMCrsTransformer(dataCrs, NwwUtilities.GPS_CRS).transform(readFC);
        }
        return readFC;
    }
}
