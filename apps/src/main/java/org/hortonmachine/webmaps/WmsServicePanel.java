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
package org.hortonmachine.webmaps;

import static org.hortonmachine.webmaps.WebMapsUi.HTML_END;
import static org.hortonmachine.webmaps.WebMapsUi.HTML_START;
import static org.hortonmachine.webmaps.WebMapsUi.addRow;
import static org.hortonmachine.webmaps.WebMapsUi.bboxString;
import static org.hortonmachine.webmaps.WebMapsUi.button;
import static org.hortonmachine.webmaps.WebMapsUi.escape;
import static org.hortonmachine.webmaps.WebMapsUi.row;
import static org.hortonmachine.webmaps.WebMapsUi.section;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.wms.CRSEnvelope;
import org.geotools.ows.wms.Layer;
import org.geotools.data.ows.OperationType;
import org.geotools.data.ows.Service;
import org.geotools.ows.wms.StyleImpl;
import org.geotools.ows.wms.WMSCapabilities;
import org.geotools.ows.wms.request.GetMapRequest;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.images.WmsWrapper;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.locationtech.jts.geom.Envelope;

/**
 * The WMS part of the {@link WebServicesBrowser}: GetMap requests shown on the map or as image,
 * and saved as GeoTIFF.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
class WmsServicePanel extends ServicePanel {
    private static final String NO_IMAGE = "Get an image to see it here.";
    private static final int MAX_MAP_PREVIEW_SIZE = 4096;

    private WmsWrapper wms;

    private JComboBox<String> styleCombo;
    private JComboBox<String> formatCombo;
    private JComboBox<String> crsCombo;
    private JCheckBox transparentCheck;
    private JSpinner widthSpinner;
    private JLabel heightLabel;
    private JPanel requestPanel;

    private JButton mapPreviewButton;
    private JButton getImageButton;
    private JButton exportButton;
    private JPanel actionPanel;

    private WebMapsUi.ImagePanel imagePanel;
    private JLabel resultInfoLabel;
    private JTextField urlField;
    private JPanel resultPanel;

    WmsServicePanel( WebServicesBrowser browser ) {
        super(browser);
        createRequestPanel();
        createActionPanel();
        createResultPanel();
    }

    @Override
    String getType() {
        return "WMS";
    }

    @Override
    String[] getVersions() {
        // the geotools client negotiates the version
        return new String[]{"auto"};
    }

    @Override
    String[] getPresets() {
        return new String[]{//
                "https://ows.terrestris.de/osm/service?SERVICE=WMS&REQUEST=GetCapabilities", //
                "https://ows.mundialis.de/services/service?SERVICE=WMS&REQUEST=GetCapabilities"//
        };
    }

    // ==================== ui ====================

    private void createRequestPanel() {
        requestPanel = new JPanel();
        requestPanel.setLayout(new BoxLayout(requestPanel, BoxLayout.Y_AXIS));

        JPanel layerPanel = section("GetMap parameters");
        styleCombo = new JComboBox<>();
        styleCombo.setToolTipText("Empty for the default style of the layer");
        formatCombo = new JComboBox<>();
        crsCombo = new JComboBox<>();
        crsCombo.setToolTipText("The CRS of the requested image, as supported by the layer");
        crsCombo.addActionListener(e -> updateHeight());
        transparentCheck = new JCheckBox("Transparent background", true);
        addRow(layerPanel, 0, new JLabel("Style"), styleCombo);
        addRow(layerPanel, 1, new JLabel("Format"), formatCombo);
        addRow(layerPanel, 2, new JLabel("CRS"), crsCombo);
        addRow(layerPanel, 3, transparentCheck);
        requestPanel.add(layerPanel);

        JPanel sizePanel = section("Image size (Get image / Export)");
        widthSpinner = new JSpinner(new SpinnerNumberModel(1024, 1, 20000, 128));
        widthSpinner.addChangeListener(e -> updateHeight());
        heightLabel = new JLabel("-");
        addRow(sizePanel, 0, new JLabel("Width (pixels)"), widthSpinner);
        addRow(sizePanel, 1, new JLabel("Height (pixels)"), heightLabel);
        addRow(sizePanel, 2, WebMapsUi.note("The height follows the proportions of the region in the chosen CRS."));
        requestPanel.add(sizePanel);
    }

    private void createActionPanel() {
        actionPanel = section("Requests");
        mapPreviewButton = button("Map view preview", this::mapPreview);
        mapPreviewButton.setToolTipText("GetMap of the current map view, drawn on the map (needs EPSG:3857 or EPSG:4326)");
        getImageButton = button("Get image", () -> getImage(false));
        getImageButton.setToolTipText("GetMap of the region (or the whole layer) in the chosen CRS and size");
        getImageButton.setFont(getImageButton.getFont().deriveFont(Font.BOLD));
        exportButton = button("Export GeoTIFF...", () -> getImage(true));
        exportButton.setToolTipText("GetMap of the region in the chosen CRS and size, saved as GeoTIFF");
        // two rows, the labels don't fit side by side in the panel width
        JPanel buttons = new JPanel(new GridLayout(2, 2, 4, 4));
        buttons.add(mapPreviewButton);
        buttons.add(getImageButton);
        buttons.add(exportButton);
        addRow(actionPanel, 0, buttons);
    }

    private void createResultPanel() {
        resultPanel = new JPanel(new BorderLayout());
        imagePanel = new WebMapsUi.ImagePanel(NO_IMAGE);
        resultInfoLabel = new JLabel(" ");
        resultInfoLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        urlField = new JTextField();
        JPanel top = new JPanel(new BorderLayout());
        top.add(WebMapsUi.urlPanel(urlField), BorderLayout.NORTH);
        top.add(resultInfoLabel, BorderLayout.SOUTH);
        resultPanel.add(top, BorderLayout.NORTH);
        resultPanel.add(imagePanel, BorderLayout.CENTER);
    }

    @Override
    JComponent getRequestPanel() {
        return requestPanel;
    }

    @Override
    JComponent getActionPanel() {
        return actionPanel;
    }

    @Override
    JComponent getResultPanel() {
        return resultPanel;
    }

    @Override
    void setBusy( boolean busy ) {
        boolean enabled = !busy && wms != null;
        mapPreviewButton.setEnabled(enabled);
        getImageButton.setEnabled(enabled);
        exportButton.setEnabled(enabled);
    }

    // ==================== connection ====================

    @Override
    List<LayerEntry> connect( String url, String version ) throws Exception {
        wms = null;
        WmsWrapper newWms = new WmsWrapper(url);
        WMSCapabilities capabilities = newWms.getCapabilities();
        if (capabilities == null)
            throw new IllegalStateException("No capabilities document returned by the service.");
        List<LayerEntry> layers = new ArrayList<>();
        for( Layer layer : newWms.getLayers() ) {
            layers.add(new LayerEntry(layer.getName(), layer.getTitle(), wgs84Bounds(layer), layer));
        }
        serviceInfoHtml = serviceHtml(url, capabilities, layers.size());
        wms = newWms;
        return layers;
    }

    @Override
    void connected() {
        clearOptions();
        clearResult();
    }

    @Override
    void close() {
        wms = null;
    }

    private static Envelope wgs84Bounds( Layer layer ) {
        CRSEnvelope ll = layer.getLatLonBoundingBox();
        if (ll == null)
            return null;
        Envelope env = new Envelope(ll.getMinX(), ll.getMaxX(), ll.getMinY(), ll.getMaxY());
        return env.isNull() || env.getWidth() <= 0 || env.getHeight() <= 0 ? null : env;
    }

    // ==================== layer ====================

    @Override
    void selectLayer( LayerEntry entry ) {
        Layer layer = (Layer) entry.data;

        List<String> styles = new ArrayList<>();
        styles.add("");
        layer.getStyles().forEach(s -> styles.add(s.getName()));
        styleCombo.setModel(new DefaultComboBoxModel<>(styles.toArray(new String[0])));

        List<String> formats = wms.getFormats();
        formatCombo.setModel(new DefaultComboBoxModel<>(formats.toArray(new String[0])));
        for( String preferred : new String[]{"image/png", "image/png8", "image/jpeg"} ) {
            if (formats.contains(preferred)) {
                formatCombo.setSelectedItem(preferred);
                break;
            }
        }

        List<String> crsCodes = getCrsCodes(layer);
        crsCombo.setModel(new DefaultComboBoxModel<>(crsCodes.toArray(new String[0])));
        for( String preferred : new String[]{"EPSG:3857", "EPSG:4326"} ) {
            if (crsCodes.contains(preferred)) {
                crsCombo.setSelectedItem(preferred);
                break;
            }
        }
        updateHeight();
        setLayerInfo(layerHtml(layer, entry, crsCodes));
    }

    /**
     * @return the supported CRS codes of the layer, uppercase, the most common first.
     */
    private static List<String> getCrsCodes( Layer layer ) {
        Set<String> codes = new TreeSet<>();
        Set<String> srs = layer.getSrs();
        if (srs != null)
            srs.forEach(s -> codes.add(s.trim().toUpperCase(Locale.ROOT)));
        Map<String, CRSEnvelope> bboxes = layer.getBoundingBoxes();
        if (bboxes != null)
            bboxes.keySet().forEach(s -> codes.add(s.trim().toUpperCase(Locale.ROOT)));
        List<String> list = new ArrayList<>();
        for( String common : new String[]{"EPSG:3857", "EPSG:4326", "CRS:84"} ) {
            if (codes.remove(common))
                list.add(common);
        }
        list.addAll(codes);
        return list;
    }

    private void clearOptions() {
        styleCombo.setModel(new DefaultComboBoxModel<>());
        formatCombo.setModel(new DefaultComboBoxModel<>());
        crsCombo.setModel(new DefaultComboBoxModel<>());
        heightLabel.setText("-");
    }

    private void clearResult() {
        imagePanel.setMessage(NO_IMAGE);
        resultInfoLabel.setText(" ");
        urlField.setText("");
    }

    @Override
    void regionChanged() {
        updateHeight();
    }

    /**
     * @return the area of the requests: the region or the whole layer, in WGS84.
     */
    private Envelope getRequestArea() {
        Envelope region = browser.getRegion();
        if (region != null)
            return region;
        if (currentEntry == null || currentEntry.wgs84 == null)
            throw new IllegalArgumentException("The layer doesn't declare its WGS84 bounds: please define a region.");
        return currentEntry.wgs84;
    }

    private void updateHeight() {
        Object crsCode = crsCombo.getSelectedItem();
        if (currentEntry == null || crsCode == null) {
            heightLabel.setText("-");
            return;
        }
        try {
            Envelope area = browser.getRegionQuietly();
            if (area == null)
                area = currentEntry.wgs84;
            if (area == null) {
                heightLabel.setText("define a region");
                return;
            }
            ReferencedEnvelope env = toCrs(area, crsCode.toString());
            int width = (Integer) widthSpinner.getValue();
            heightLabel.setText(String.valueOf(heightFor(env, width)));
        } catch (Exception e) {
            heightLabel.setText("unable to transform to " + crsCode);
        }
    }

    private static int heightFor( Envelope env, int width ) {
        return Math.max(1, (int) Math.round(width * env.getHeight() / env.getWidth()));
    }

    /**
     * @return the CRS with longitude/easting first: geotools puts the bbox in the server axis order.
     */
    private static CoordinateReferenceSystem getCrs( String code ) throws Exception {
        if (code.equals("CRS:84"))
            return DefaultGeographicCRS.WGS84;
        return HMCrsRegistry.INSTANCE.getCrs(code, true);
    }

    private static ReferencedEnvelope toCrs( Envelope lonLat, String code ) throws Exception {
        ReferencedEnvelope env = new ReferencedEnvelope(lonLat, DefaultGeographicCRS.WGS84);
        CoordinateReferenceSystem crs = getCrs(code);
        if (HMCrsRegistry.crsEquals(crs, DefaultGeographicCRS.WGS84))
            return new ReferencedEnvelope(lonLat, crs);
        return env.transform(crs, true);
    }

    // ==================== requests ====================

    /** The outcome of a GetMap request. */
    private static class MapResult {
        BufferedImage image;
        String error;
        String url;
        long millis;
    }

    private MapResult getMap( Layer layer, String style, String format, String crsCode, int width, int height,
            ReferencedEnvelope env, boolean transparent ) throws Exception {
        StyleImpl styleImpl = null;
        if (style != null && !style.isEmpty()) {
            for( StyleImpl s : layer.getStyles() ) {
                if (style.equals(s.getName()))
                    styleImpl = s;
            }
        }
        GetMapRequest request = wms.getMapRequest(layer, format, crsCode, width, height, env, null, styleImpl);
        request.setTransparent(transparent);
        MapResult result = new MapResult();
        result.url = wms.getUrl(request).toString();
        browser.log("GetMap: " + result.url);
        long t0 = System.currentTimeMillis();
        result.image = wms.getImage(request);
        result.millis = System.currentTimeMillis() - t0;
        if (result.image == null) {
            // not an image: most probably an xml exception
            result.error = extractServiceException(wms.getMessage(request));
            browser.log("GetMap failed: " + result.error);
        } else {
            browser.log("GetMap returned " + result.image.getWidth() + "x" + result.image.getHeight() + " pixels in "
                    + result.millis + " ms");
        }
        return result;
    }

    private static String extractServiceException( String message ) {
        if (message == null || message.isBlank())
            return "The service returned no image and no message.";
        Matcher matcher = Pattern.compile("<(?:\\w+:)?(?:ServiceException|ExceptionText)[^>]*>(.+?)</(?:\\w+:)?(?:ServiceException|ExceptionText)>",
                Pattern.DOTALL).matcher(message);
        List<String> messages = new ArrayList<>();
        while( matcher.find() ) {
            messages.add(matcher.group(1).replaceAll("<!\\[CDATA\\[|\\]\\]>", "").trim());
        }
        if (!messages.isEmpty())
            return String.join("\n", messages);
        String trimmed = message.trim();
        return trimmed.length() > 1000 ? trimmed.substring(0, 1000) + "..." : trimmed;
    }

    /**
     * GetMap of the visible map area in a CRS the map can show (web mercator or geographic), as overlay.
     */
    private void mapPreview() {
        if (currentEntry == null) {
            GuiUtilities.showWarningMessage(browser, "Please select a layer first.");
            return;
        }
        Layer layer = (Layer) currentEntry.data;
        List<String> codes = getCrsCodes(layer);
        String code = codes.contains("EPSG:3857") ? "EPSG:3857" : codes.contains("EPSG:4326") ? "EPSG:4326" : null;
        if (code == null) {
            GuiUtilities.showWarningMessage(browser, "The layer supports neither EPSG:3857 nor EPSG:4326, so it can't be drawn on the map.\n"
                    + "Use Get image to see it in the Result tab.");
            return;
        }
        boolean mercator = code.equals("EPSG:3857");
        Envelope view = browser.getMap().getViewEnvelope();
        int[] size = browser.getMap().getPixelSize(view);
        int width = Math.min(size[0], MAX_MAP_PREVIEW_SIZE);
        int height = Math.min(size[1], MAX_MAP_PREVIEW_SIZE);
        String style = (String) styleCombo.getSelectedItem();
        String format = (String) formatCombo.getSelectedItem();
        boolean transparent = transparentCheck.isSelected();
        browser.runTask("Map preview of " + layer.getName() + " in " + code, () -> {
            ReferencedEnvelope env = toCrs(view, code);
            return getMap(layer, style, format, code, width, height, env, transparent);
        }, result -> {
            showResult(result, layer.getName() + " (map view, " + code + ")");
            if (result.image != null) {
                browser.getMap().setOverlay(result.image, view, mercator);
                browser.setStatus("Map preview in " + code + " loaded in " + result.millis + " ms.");
            } else {
                browser.showResultTab();
                GuiUtilities.showWarningMessage(browser, "The service returned an error:\n" + result.error);
            }
        });
    }

    /**
     * GetMap of the region in the chosen CRS and size, shown or exported.
     */
    private void getImage( boolean export ) {
        if (currentEntry == null) {
            GuiUtilities.showWarningMessage(browser, "Please select a layer first.");
            return;
        }
        Layer layer = (Layer) currentEntry.data;
        Envelope area;
        try {
            area = getRequestArea();
        } catch (IllegalArgumentException e) {
            GuiUtilities.showWarningMessage(browser, e.getMessage());
            return;
        }
        String code = (String) crsCombo.getSelectedItem();
        String style = (String) styleCombo.getSelectedItem();
        String format = (String) formatCombo.getSelectedItem();
        boolean transparent = transparentCheck.isSelected();
        int width = (Integer) widthSpinner.getValue();
        File outFile = null;
        if (export) {
            outFile = GuiUtilities.showSaveFileDialog(browser, "Save the image as GeoTIFF", PreferencesHandler.getLastFile());
            if (outFile == null)
                return;
            if (!outFile.getName().toLowerCase().matches(".*\\.tiff?$"))
                outFile = new File(outFile.getParentFile(), outFile.getName() + ".tif");
            PreferencesHandler.setLastPath(outFile.getAbsolutePath());
        }
        File fOutFile = outFile;
        browser.runTask((export ? "Exporting " : "Getting image of ") + layer.getName() + " in " + code, () -> {
            ReferencedEnvelope env = toCrs(area, code);
            int height = heightFor(env, width);
            MapResult result = getMap(layer, style, format, code, width, height, env, transparent);
            if (result.image != null && fOutFile != null) {
                double xRes = env.getWidth() / width;
                double yRes = env.getHeight() / height;
                RegionMap regionMap = RegionMap.fromBoundsAndGrid(env.getMinX(), env.getMaxX(), env.getMinY(), env.getMaxY(),
                        width, height);
                GridCoverage2D coverage = CoverageUtilities.buildCoverage(layer.getName(), result.image, regionMap,
                        env.getCoordinateReferenceSystem());
                OmsRasterWriter.writeRaster(fOutFile.getAbsolutePath(), coverage);
                CoverageUtilities.writeWorldFiles(coverage, fOutFile.getAbsolutePath());
                browser.log("Written " + fOutFile + " (" + width + "x" + height + " pixels, resolution "
                        + String.format(Locale.ROOT, "%.6f x %.6f", xRes, yRes) + ")");
            }
            return result;
        }, result -> {
            showResult(result, layer.getName() + " (" + code + ")");
            browser.showResultTab();
            if (result.image == null) {
                GuiUtilities.showWarningMessage(browser, "The service returned an error:\n" + result.error);
                return;
            }
            // draw on the map, if its CRS allows
            if (code.equals("EPSG:3857")) {
                browser.getMap().setOverlay(result.image, area, true);
            } else if (code.equals("EPSG:4326") || code.equals("CRS:84")) {
                browser.getMap().setOverlay(result.image, area, false);
            } else {
                browser.getMap().setOverlay(null, null, false);
                browser.getMap().setFootprints(List.of(org.hortonmachine.gears.utils.geometry.GeometryUtilities
                        .createPolygonFromEnvelope(area)));
            }
            browser.setStatus(fOutFile != null ? "Saved " + fOutFile : "Image loaded in " + result.millis + " ms.");
        });
    }

    private void showResult( MapResult result, String caption ) {
        urlField.setText(result.url);
        urlField.setCaretPosition(0);
        if (result.image != null) {
            imagePanel.setImage(result.image, caption);
            resultInfoLabel.setText("Received in " + result.millis + " ms.");
        } else {
            imagePanel.setMessage("The service returned an error, see the Log tab.");
            resultInfoLabel.setText("<html><font color='" + WebMapsUi.KO_COLOR + "'>" + escape(result.error).replace("\n", "<br>")
                    + "</font></html>");
        }
    }

    // ==================== info ====================

    private static String serviceHtml( String url, WMSCapabilities capabilities, int layersCount ) {
        StringBuilder sb = new StringBuilder(HTML_START);
        Service service = capabilities.getService();
        sb.append("<h2>").append(escape(service != null && service.getTitle() != null ? service.getTitle() : url)).append("</h2>");
        sb.append("<table cellpadding='3'>");
        row(sb, "Url", escape(url));
        row(sb, "Version", escape(capabilities.getVersion()));
        if (service != null) {
            row(sb, "Name", escape(service.getName()));
            if (service.get_abstract() != null)
                row(sb, "Abstract", escape(service.get_abstract()));
            if (service.getKeywordList() != null)
                row(sb, "Keywords", escape(String.join(", ", service.getKeywordList())));
            if (service.getOnlineResource() != null)
                row(sb, "Online resource", escape(service.getOnlineResource().toString()));
            if (service.getMaxWidth() > 0 || service.getMaxHeight() > 0)
                row(sb, "Max image size", service.getMaxWidth() + " x " + service.getMaxHeight());
            if (service.getLayerLimit() > 0)
                row(sb, "Layer limit", String.valueOf(service.getLayerLimit()));
        }
        row(sb, "Named layers", String.valueOf(layersCount));
        OperationType getMap = capabilities.getRequest().getGetMap();
        if (getMap != null) {
            if (getMap.getGet() != null)
                row(sb, "GetMap url", escape(getMap.getGet().toString()));
            row(sb, "GetMap formats", escape(String.join(", ", getMap.getFormats())));
        }
        OperationType featureInfo = capabilities.getRequest().getGetFeatureInfo();
        row(sb, "GetFeatureInfo", featureInfo != null ? WebMapsUi.colored(WebMapsUi.OK_COLOR, "yes")
                : WebMapsUi.colored(WebMapsUi.KO_COLOR, "no"));
        OperationType legend = capabilities.getRequest().getGetLegendGraphic();
        row(sb, "GetLegendGraphic", legend != null ? WebMapsUi.colored(WebMapsUi.OK_COLOR, "yes")
                : WebMapsUi.colored(WebMapsUi.KO_COLOR, "no"));
        sb.append("</table>").append(HTML_END);
        return sb.toString();
    }

    private static String layerHtml( Layer layer, LayerEntry entry, List<String> crsCodes ) {
        StringBuilder sb = new StringBuilder(HTML_START);
        sb.append("<h2>").append(escape(layer.getTitle() != null ? layer.getTitle() : layer.getName())).append("</h2>");
        sb.append("<table cellpadding='3'>");
        row(sb, "Name", escape(layer.getName()));
        if (layer.get_abstract() != null)
            row(sb, "Abstract", escape(layer.get_abstract()).replace("\n", "<br>"));
        if (layer.getKeywords() != null && layer.getKeywords().length > 0)
            row(sb, "Keywords", escape(String.join(", ", layer.getKeywords())));
        row(sb, "Queryable", layer.isQueryable() ? "yes" : "no");
        row(sb, "WGS84 bounds", entry.wgs84 != null ? escape(bboxString(entry.wgs84)) : "<i>not declared</i>");
        if (!Double.isNaN(layer.getScaleDenominatorMin()) || !Double.isNaN(layer.getScaleDenominatorMax()))
            row(sb, "Scale denominators", layer.getScaleDenominatorMin() + " - " + layer.getScaleDenominatorMax());
        row(sb, "Supported CRS", crsCodes.size() + ": " + escape(String.join(", ", crsCodes)));
        sb.append("</table>");

        Map<String, CRSEnvelope> bboxes = layer.getBoundingBoxes();
        if (bboxes != null && !bboxes.isEmpty()) {
            sb.append("<h3>Declared bounding boxes</h3><table cellpadding='3'>");
            sb.append("<tr><th align='left'>CRS</th><th align='left'>Bounds (minx, miny, maxx, maxy)</th></tr>");
            for( Map.Entry<String, CRSEnvelope> e : bboxes.entrySet() ) {
                CRSEnvelope b = e.getValue();
                sb.append("<tr><td>").append(escape(e.getKey())).append("</td><td>")
                        .append(Arrays.stream(new double[]{b.getMinX(), b.getMinY(), b.getMaxX(), b.getMaxY()})
                                .mapToObj(d -> String.format(Locale.ROOT, "%.6f", d)).collect(Collectors.joining(", ")))
                        .append("</td></tr>");
            }
            sb.append("</table>");
        }

        List<StyleImpl> styles = layer.getStyles();
        if (styles != null && !styles.isEmpty()) {
            sb.append("<h3>Styles</h3><table cellpadding='3'>");
            sb.append("<tr><th align='left'>Name</th><th align='left'>Title</th></tr>");
            for( StyleImpl s : styles ) {
                sb.append("<tr><td>").append(escape(s.getName())).append("</td><td>")
                        .append(escape(s.getTitle() != null ? s.getTitle().toString() : "")).append("</td></tr>");
            }
            sb.append("</table>");
        }
        sb.append(HTML_END);
        return sb.toString();
    }
}
