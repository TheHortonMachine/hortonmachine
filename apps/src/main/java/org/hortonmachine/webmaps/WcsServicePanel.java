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
import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.wcs.ICoverageSummary;
import org.hortonmachine.gears.io.wcs.IDescribeCoverage;
import org.hortonmachine.gears.io.wcs.Wcs;
import org.hortonmachine.gears.io.wcs.readers.CoverageReaderParameters;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.locationtech.jts.geom.Envelope;

/**
 * The WCS part of the {@link WebServicesBrowser}: describe coverages, preview them on the map and download them.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
class WcsServicePanel extends ServicePanel {
    private static final String NO_RESULT = "Preview or download a coverage to see it here.";
    private static final String AS_SERVED = "";
    private static final String NATIVE = "native";
    private static final int PREVIEW_COLS = 800;

    private Wcs wcs;
    private List<String> serviceFormats;
    private int[] serviceSrids;

    // the selected coverage
    private ICoverageSummary summary;
    private IDescribeCoverage describe;
    private String describeUrl;
    private String describeError;

    private JComboBox<String> formatCombo;
    private JComboBox<String> subsetCrsCombo;
    private JComboBox<String> outputCrsCombo;
    private JCheckBox extendedAxisCheck;
    private JRadioButton servedSizeRadio;
    private JRadioButton colsRadio;
    private JRadioButton scaleRadio;
    private JSpinner colsSpinner;
    private JLabel rowsLabel;
    private JTextField scaleField;
    private JPanel requestPanel;

    private JButton describeButton;
    private JButton previewButton;
    private JButton downloadButton;
    private JPanel actionPanel;

    private WebMapsUi.ImagePanel imagePanel;
    private JEditorPane resultInfoPane;
    private JTextField urlField;
    private JPanel resultPanel;

    WcsServicePanel( WebServicesBrowser browser ) {
        super(browser);
        createRequestPanel();
        createActionPanel();
        createResultPanel();
    }

    @Override
    String getType() {
        return "WCS";
    }

    @Override
    String[] getVersions() {
        return new String[]{"auto", "2.0.1", "1.1.1", "1.1.0", "1.0.0"};
    }

    @Override
    String[] getPresets() {
        return new String[]{//
                "https://maps.isric.org/mapserv?map=/map/nitrogen.map", //
                "https://geo.hazi.eus/S2GEOEUSKADI_RGB/wcs", //
                "https://www.wcs.nrw.de/geobasis/wcs_nw_dgm"//
        };
    }

    // ==================== ui ====================

    private void createRequestPanel() {
        requestPanel = new JPanel();
        requestPanel.setLayout(new BoxLayout(requestPanel, BoxLayout.Y_AXIS));

        JPanel paramsPanel = section("GetCoverage parameters");
        formatCombo = new JComboBox<>();
        formatCombo.setEditable(true);
        formatCombo.setToolTipText("Only GeoTIFF results can be previewed");
        subsetCrsCombo = new JComboBox<>(new String[]{NATIVE, "EPSG:4326"});
        subsetCrsCombo.setToolTipText("The CRS in which the region is sent: the native CRS of the coverage or WGS84");
        subsetCrsCombo.addActionListener(e -> updateRows());
        outputCrsCombo = new JComboBox<>();
        outputCrsCombo.setEditable(true);
        outputCrsCombo.setToolTipText("Reprojection by the service (2.0.1 only), empty to get the coverage as served");
        extendedAxisCheck = new JCheckBox("Use axis urls (2.0.1)");
        extendedAxisCheck.setToolTipText("Some servers want the scaling axes as urls, e.g. http://www.opengis.net/def/axis/OGC/1/i");
        addRow(paramsPanel, 0, new JLabel("Format"), formatCombo);
        addRow(paramsPanel, 1, new JLabel("Region CRS"), subsetCrsCombo);
        addRow(paramsPanel, 2, new JLabel("Output CRS"), outputCrsCombo);
        addRow(paramsPanel, 3, extendedAxisCheck);
        requestPanel.add(paramsPanel);

        JPanel sizePanel = section("Size (Download)");
        servedSizeRadio = new JRadioButton("As served (native resolution)", true);
        colsRadio = new JRadioButton("Columns");
        scaleRadio = new JRadioButton("Scale factor");
        ButtonGroup group = new ButtonGroup();
        group.add(servedSizeRadio);
        group.add(colsRadio);
        group.add(scaleRadio);
        colsSpinner = new JSpinner(new SpinnerNumberModel(1000, 1, 100000, 100));
        colsSpinner.addChangeListener(e -> {
            colsRadio.setSelected(true);
            updateRows();
        });
        rowsLabel = new JLabel("-");
        scaleField = new JTextField("0.5", 6);
        scaleField.setToolTipText("0.5 halves the columns and rows (2.0.1 only)");
        WebMapsUi.onTextChange(scaleField, () -> scaleRadio.setSelected(true));
        addRow(sizePanel, 0, servedSizeRadio);
        addRow(sizePanel, 1, colsRadio, colsSpinner);
        addRow(sizePanel, 2, new JLabel("   Rows"), rowsLabel);
        addRow(sizePanel, 3, scaleRadio, scaleField);
        addRow(sizePanel, 4, WebMapsUi.note("Scaling works only if the service supports it, else the native "
                + "resolution is returned. The preview always asks for " + PREVIEW_COLS + " columns."));
        requestPanel.add(sizePanel);
    }

    private void createActionPanel() {
        actionPanel = section("Requests");
        describeButton = button("Describe", this::describeCoverage);
        describeButton.setToolTipText("DescribeCoverage request, to read formats, CRS and grid of the coverage");
        previewButton = button("Preview", () -> getCoverage(true));
        previewButton.setToolTipText("A small GeoTIFF of the region, drawn on the map");
        downloadButton = button("Download...", () -> getCoverage(false));
        downloadButton.setToolTipText("The coverage of the region, with the chosen format and size, saved to a file");
        downloadButton.setFont(downloadButton.getFont().deriveFont(Font.BOLD));
        JPanel buttons = new JPanel(new GridLayout(1, 3, 4, 0));
        buttons.add(describeButton);
        buttons.add(previewButton);
        buttons.add(downloadButton);
        addRow(actionPanel, 0, buttons);
    }

    private void createResultPanel() {
        resultPanel = new JPanel(new BorderLayout());
        imagePanel = new WebMapsUi.ImagePanel(NO_RESULT);
        resultInfoPane = WebMapsUi.htmlPane();
        urlField = new JTextField();
        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, imagePanel, new JScrollPane(resultInfoPane));
        split.setResizeWeight(0.6);
        resultPanel.add(WebMapsUi.urlPanel(urlField), BorderLayout.NORTH);
        resultPanel.add(split, BorderLayout.CENTER);
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
        boolean enabled = !busy && wcs != null;
        describeButton.setEnabled(enabled);
        previewButton.setEnabled(enabled);
        downloadButton.setEnabled(enabled);
    }

    // ==================== connection ====================

    @Override
    List<LayerEntry> connect( String url, String version ) throws Exception {
        wcs = null;
        Wcs newWcs = new Wcs(url, version);
        String serviceVersion;
        try {
            serviceVersion = newWcs.version();
        } catch (NullPointerException e) {
            throw new IllegalArgumentException("Unsupported WCS version: " + version);
        }
        browser.log("WCS version " + serviceVersion + ", capabilities: " + newWcs.capabilitiesUrl());
        List<String> ids = newWcs.ids();
        List<LayerEntry> layers = new ArrayList<>();
        for( String id : ids ) {
            ICoverageSummary s = newWcs.summary(id);
            String title = s != null ? s.getTitle() : null;
            Envelope wgs84 = null;
            if (s != null && s.getWgs84BoundingBox() != null && !s.getWgs84BoundingBox().isEmpty()) {
                ReferencedEnvelope b = s.getWgs84BoundingBox();
                wgs84 = new Envelope(b.getMinX(), b.getMaxX(), b.getMinY(), b.getMaxY());
            }
            layers.add(new LayerEntry(id, title, wgs84, s));
        }
        serviceFormats = newWcs.formats();
        serviceSrids = newWcs.srids();
        serviceInfoHtml = serviceHtml(url, serviceVersion, newWcs.capabilitiesUrl(), layers.size());
        wcs = newWcs;
        return layers;
    }

    @Override
    void connected() {
        summary = null;
        describe = null;
        setFormats(serviceFormats);
        setOutputSrids(serviceSrids);
        clearResult();
    }

    @Override
    void close() {
        wcs = null;
    }

    // ==================== coverage ====================

    @Override
    void selectLayer( LayerEntry entry ) {
        summary = (ICoverageSummary) entry.data;
        describe = null;
        describeUrl = null;
        describeError = null;
        setFormats(serviceFormats);
        setOutputSrids(serviceSrids);
        updateSubsetCrsLabel();
        updateRows();
        setLayerInfo(coverageHtml(entry));
        if (!browser.isBusy())
            describeCoverage();
    }

    private void describeCoverage() {
        if (currentEntry == null) {
            GuiUtilities.showWarningMessage(browser, "Please select a coverage first.");
            return;
        }
        LayerEntry entry = currentEntry;
        browser.runTask("Describing coverage " + entry.name, () -> {
            Object[] result = new Object[3];
            try {
                result[0] = wcs.describeUrl(entry.name);
                browser.log("DescribeCoverage: " + result[0]);
            } catch (Exception e) {
                browser.logException("Unable to build the DescribeCoverage url", e);
            }
            // Wcs.describe returns null on errors
            result[1] = wcs.describe(entry.name);
            return result;
        }, result -> {
            if (entry != currentEntry)
                return;
            describeUrl = (String) result[0];
            describe = (IDescribeCoverage) result[1];
            describeError = describe == null ? "The DescribeCoverage request failed, see the console for the error." : null;
            if (describe != null) {
                try {
                    List<String> formats = describe.getSupportedFormats();
                    if (formats != null && !formats.isEmpty())
                        setFormats(formats);
                    int[] srids = describe.getSupportedSrids();
                    if (srids != null && srids.length > 0)
                        setOutputSrids(srids);
                } catch (Exception e) {
                    browser.logException("Unable to read the formats of the coverage", e);
                }
                browser.log("Coverage " + entry.name + " described.");
                if (entry.wgs84 == null)
                    setWgs84FromDescribe(entry);
            } else {
                browser.log("DescribeCoverage of " + entry.name + " failed.");
            }
            updateSubsetCrsLabel();
            updateRows();
            setLayerInfo(coverageHtml(entry));
        });
    }

    /**
     * Use the envelope of the DescribeCoverage for coverages that don't declare their WGS84 bounds.
     */
    private void setWgs84FromDescribe( LayerEntry entry ) {
        Envelope env = describe.getCoverageEnvelope();
        Integer srid = describe.getCoverageEnvelopeSrid();
        if (env == null || env.isNull() || !isKnownSrid(srid))
            return;
        try {
            ReferencedEnvelope wgs84 = new ReferencedEnvelope(env, HMCrsRegistry.INSTANCE.getCrs("EPSG:" + srid, true))
                    .transform(DefaultGeographicCRS.WGS84, true);
            entry.wgs84 = new Envelope(wgs84.getMinX(), wgs84.getMaxX(), wgs84.getMinY(), wgs84.getMaxY());
            browser.getMap().setExtents(List.of(entry.wgs84));
            browser.getMap().zoomToEnvelope(entry.wgs84);
            browser.log("WGS84 bounds of " + entry.name + " computed from the DescribeCoverage envelope.");
        } catch (Exception e) {
            browser.logException("Unable to transform the coverage envelope to WGS84", e);
        }
    }

    private void setFormats( List<String> formats ) {
        Object selected = formatCombo.getSelectedItem();
        List<String> list = formats != null ? formats : new ArrayList<>();
        formatCombo.setModel(new DefaultComboBoxModel<>(list.toArray(new String[0])));
        String tiff = tiffFormat(list);
        if (selected != null && list.contains(selected.toString()))
            formatCombo.setSelectedItem(selected);
        else if (tiff != null)
            formatCombo.setSelectedItem(tiff);
        else if (list.isEmpty())
            formatCombo.setSelectedItem("image/tiff");
    }

    private static String tiffFormat( List<String> formats ) {
        if (formats == null)
            return null;
        for( String f : formats ) {
            if (f.toLowerCase().contains("tif"))
                return f;
        }
        return null;
    }

    private void setOutputSrids( int[] srids ) {
        List<String> codes = new ArrayList<>();
        codes.add(AS_SERVED);
        if (srids != null)
            Arrays.stream(srids).distinct().forEach(s -> codes.add("EPSG:" + s));
        outputCrsCombo.setModel(new DefaultComboBoxModel<>(codes.toArray(new String[0])));
    }

    private Integer getNativeSrid() {
        if (summary != null && summary.getBoundingBoxSrid() != null)
            return summary.getBoundingBoxSrid();
        if (describe != null)
            return describe.getCoverageEnvelopeSrid();
        return null;
    }

    private static boolean isKnownSrid( Integer srid ) {
        if (srid == null)
            return false;
        try {
            HMCrsRegistry.INSTANCE.getCrs("EPSG:" + srid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * @return the srid the region is sent in: the native one if chosen and known to HM, else 4326.
     */
    private int getSubsetSrid( boolean subsetNative ) {
        Integer nativeSrid = getNativeSrid();
        return subsetNative && isKnownSrid(nativeSrid) ? nativeSrid : 4326;
    }

    private void updateSubsetCrsLabel() {
        Integer srid = getNativeSrid();
        Object selected = subsetCrsCombo.getSelectedItem();
        String nativeLabel = NATIVE;
        if (srid != null)
            nativeLabel += isKnownSrid(srid) ? " (EPSG:" + srid + ")" : " (EPSG:" + srid + " unknown to HM, WGS84 is used)";
        subsetCrsCombo.setModel(new DefaultComboBoxModel<>(new String[]{nativeLabel, "EPSG:4326"}));
        if ("EPSG:4326".equals(selected))
            subsetCrsCombo.setSelectedItem("EPSG:4326");
    }

    private boolean isSubsetNative() {
        Object selected = subsetCrsCombo.getSelectedItem();
        return selected != null && selected.toString().startsWith(NATIVE);
    }

    @Override
    void regionChanged() {
        updateRows();
    }

    private void updateRows() {
        if (currentEntry == null) {
            rowsLabel.setText("-");
            return;
        }
        try {
            Envelope env = sizeEnvelope(browser.getRegionQuietly(), isSubsetNative());
            rowsLabel.setText(env == null ? "unknown extent" : String.valueOf(rowsFor(env, (Integer) colsSpinner.getValue())));
        } catch (Exception e) {
            rowsLabel.setText("unable to transform the region");
        }
    }

    private static int rowsFor( Envelope env, int cols ) {
        return Math.max(1, (int) Math.round(cols * env.getHeight() / env.getWidth()));
    }

    /**
     * @return the region in the CRS it is sent in (native or 4326), longitude/easting first.
     */
    private Envelope subsetEnvelope( Envelope region, int srid ) throws Exception {
        if (srid == 4326)
            return region;
        return new ReferencedEnvelope(region, DefaultGeographicCRS.WGS84).transform(HMCrsRegistry.INSTANCE.getCrs("EPSG:" + srid, true),
                true);
    }

    /**
     * @return the envelope that defines the proportions of the result.
     */
    private Envelope sizeEnvelope( Envelope region, boolean subsetNative ) throws Exception {
        if (region != null)
            return subsetEnvelope(region, getSubsetSrid(subsetNative));
        if (summary != null && summary.getBoundingBox() != null && !summary.getBoundingBox().isNull())
            return summary.getBoundingBox();
        return currentEntry != null ? currentEntry.wgs84 : null;
    }

    private void clearResult() {
        imagePanel.setMessage(NO_RESULT);
        resultInfoPane.setText("");
        urlField.setText("");
    }

    // ==================== requests ====================

    /** The outcome of a GetCoverage request. */
    private static class CoverageResult {
        File file;
        String url;
        long millis;
        RasterPreview preview;
        String readError;
    }

    private void getCoverage( boolean preview ) {
        if (currentEntry == null) {
            GuiUtilities.showWarningMessage(browser, "Please select a coverage first.");
            return;
        }
        String id = currentEntry.name;
        Envelope region;
        try {
            region = browser.getRegion();
        } catch (IllegalArgumentException e) {
            GuiUtilities.showWarningMessage(browser, e.getMessage());
            return;
        }
        Object formatObj = formatCombo.getEditor().getItem();
        String format = formatObj == null || formatObj.toString().isBlank() ? null : formatObj.toString().trim();
        if (preview) {
            // previews are read by HM: ask for a tiff if possible
            String tiff = tiffFormat(listFormats());
            format = tiff != null ? tiff : format;
        }
        boolean subsetNative = isSubsetNative();
        Object outputCrsObj = outputCrsCombo.getEditor().getItem();
        Integer outputSrid = null;
        if (outputCrsObj != null && !outputCrsObj.toString().isBlank()) {
            try {
                outputSrid = Integer.parseInt(outputCrsObj.toString().trim().toUpperCase().replace("EPSG:", ""));
            } catch (NumberFormatException e) {
                GuiUtilities.showWarningMessage(browser, "The output CRS has to be an EPSG code, e.g. EPSG:4326.");
                return;
            }
        }
        boolean extendedAxis = extendedAxisCheck.isSelected();
        Integer cols = null;
        Double scale = null;
        if (preview) {
            cols = PREVIEW_COLS;
        } else if (colsRadio.isSelected()) {
            cols = (Integer) colsSpinner.getValue();
        } else if (scaleRadio.isSelected()) {
            try {
                scale = Double.parseDouble(scaleField.getText().trim());
                if (scale <= 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                GuiUtilities.showWarningMessage(browser, "The scale factor has to be a positive number.");
                return;
            }
        }

        File file;
        try {
            if (preview) {
                file = Files.createTempFile("hm_wcs_preview_", extensionFor(format)).toFile();
                file.deleteOnExit();
            } else {
                file = GuiUtilities.showSaveFileDialog(browser, "Save the coverage", PreferencesHandler.getLastFile());
                if (file == null)
                    return;
                if (!file.getName().contains("."))
                    file = new File(file.getParentFile(), file.getName() + extensionFor(format));
                PreferencesHandler.setLastPath(file.getAbsolutePath());
            }
        } catch (Exception e) {
            GuiUtilities.handleError(browser, e);
            return;
        }

        File fFile = file;
        String fFormat = format;
        Integer fOutputSrid = outputSrid;
        Integer fCols = cols;
        Double fScale = scale;
        browser.runTask((preview ? "Preview of " : "Downloading ") + id, () -> {
            CoverageReaderParameters parameters = wcs.getReaderParameters(id);
            if (fFormat != null)
                parameters.format(fFormat);
            Integer nativeSrid = getNativeSrid();
            int srid = getSubsetSrid(subsetNative);
            if (region != null && subsetNative && nativeSrid != null && srid != nativeSrid)
                browser.log("The native CRS EPSG:" + nativeSrid + " is unknown to HM, the region is sent in EPSG:4326.");
            if (region != null) {
                Envelope subset = subsetEnvelope(region, srid);
                parameters.bbox(subset, srid);
                browser.log("Region in EPSG:" + srid + ": " + bboxString(subset));
            }
            if (fCols != null) {
                Envelope sizeEnv = sizeEnvelope(region, subsetNative);
                int rows = sizeEnv != null ? rowsFor(sizeEnv, fCols) : fCols;
                parameters.rowsCols(rows, fCols);
            }
            if (fScale != null)
                parameters.scaleFactor(fScale);
            if (fOutputSrid != null)
                parameters.outputSrid(fOutputSrid);
            parameters.useExtendedAxisUrl(extendedAxis);

            CoverageResult result = new CoverageResult();
            result.file = fFile;
            long t0 = System.currentTimeMillis();
            result.url = wcs.dumpCoverage(fFile.getAbsolutePath(), parameters);
            result.millis = System.currentTimeMillis() - t0;
            browser.log("GetCoverage: " + result.url);
            browser.log("Received " + WebMapsUi.humanSize(fFile.length()) + " in " + result.millis + " ms, saved to " + fFile);

            String name = fFile.getName().toLowerCase();
            if (name.endsWith(".tif") || name.endsWith(".tiff")) {
                try {
                    GridCoverage2D coverage = OmsRasterReader.readRaster(fFile.getAbsolutePath());
                    result.preview = RasterPreview.create(coverage);
                } catch (Exception e) {
                    result.readError = "HM is unable to read the result: " + e.getMessage();
                    browser.logException("Unable to read " + fFile, e);
                }
            } else {
                result.readError = "Saved as " + nn(fFormat) + ": only GeoTIFF results are previewed.";
            }
            return result;
        }, result -> {
            if (currentEntry == null || !currentEntry.name.equals(id))
                return;
            showResult(id, result, preview);
            browser.showResultTab();
            browser.setStatus((preview ? "Preview" : "Download") + " of " + id + " received in " + result.millis + " ms.");
        });
    }

    private List<String> listFormats() {
        List<String> formats = new ArrayList<>();
        for( int i = 0; i < formatCombo.getItemCount(); i++ ) {
            formats.add(formatCombo.getItemAt(i));
        }
        return formats;
    }

    private static String nn( String s ) {
        return s == null ? "" : s;
    }

    private static String extensionFor( String format ) {
        String f = format == null ? "" : format.toLowerCase();
        if (f.contains("tif") || f.isEmpty())
            return ".tif";
        if (f.contains("png"))
            return ".png";
        if (f.contains("jpeg") || f.contains("jpg"))
            return ".jpg";
        if (f.contains("netcdf"))
            return ".nc";
        if (f.contains("json"))
            return ".json";
        if (f.contains("xml") || f.contains("gml"))
            return ".xml";
        return ".bin";
    }

    private void showResult( String id, CoverageResult result, boolean preview ) {
        urlField.setText(result.url);
        urlField.setCaretPosition(0);
        RasterPreview p = result.preview;
        StringBuilder sb = new StringBuilder(HTML_START);
        sb.append("<h3>").append(escape(id)).append(preview ? " (preview)" : "").append("</h3><table cellpadding='3'>");
        row(sb, "File", escape(result.file.getAbsolutePath()) + " (" + WebMapsUi.humanSize(result.file.length()) + ")");
        row(sb, "Received in", result.millis + " ms");
        if (p != null) {
            row(sb, "Columns x rows", p.cols + " x " + p.rows);
            row(sb, "Bands", p.bands + " (" + p.dataType + ")");
            row(sb, "CRS", escape(p.crs));
            row(sb, "Bounds", escape(WebMapsUi.axesString(p.envelope)));
            row(sb, "Resolution", String.format(Locale.ROOT, "%.6f x %.6f", p.envelope.getWidth() / p.cols,
                    p.envelope.getHeight() / p.rows));
            row(sb, "Novalue", p.novalue != null ? String.valueOf(p.novalue) : "-");
            if (p.range != null)
                row(sb, "Range (band 1)", String.format(Locale.ROOT, "%.4f - %.4f", p.range[0], p.range[1]));
            if (p.note != null)
                row(sb, "Note", escape(p.note));
            imagePanel.setImage(p.image, id);
            browser.getMap().setOverlay(p.wgs84Image, p.wgs84Image != null ? p.wgs84Envelope : null, false);
            browser.getMap().setFootprints(
                    p.wgs84Envelope != null ? List.of(GeometryUtilities.createPolygonFromEnvelope(p.wgs84Envelope)) : null);
        } else {
            row(sb, "Note", WebMapsUi.colored(WebMapsUi.KO_COLOR, result.readError));
            imagePanel.setMessage(result.readError);
        }
        sb.append("</table>").append(HTML_END);
        resultInfoPane.setText(sb.toString());
        SwingUtilities.invokeLater(() -> resultInfoPane.setCaretPosition(0));
    }

    // ==================== info ====================

    private String serviceHtml( String url, String version, String capabilitiesUrl, int coveragesCount ) {
        StringBuilder sb = new StringBuilder(HTML_START);
        sb.append("<h2>").append(escape(url)).append("</h2><table cellpadding='3'>");
        row(sb, "Version", escape(version));
        row(sb, "Capabilities", escape(capabilitiesUrl));
        row(sb, "Coverages", String.valueOf(coveragesCount));
        row(sb, "Formats", serviceFormats != null ? escape(String.join(", ", serviceFormats))
                : "<i>not in the capabilities, read from the DescribeCoverage of each coverage</i>");
        row(sb, "CRS", serviceSrids != null ? escape(sridList(serviceSrids))
                : "<i>not in the capabilities, read from the DescribeCoverage of each coverage</i>");
        sb.append("</table>").append(HTML_END);
        return sb.toString();
    }

    private static String sridList( int[] srids ) {
        String list = Arrays.stream(srids).limit(100).mapToObj(s -> "EPSG:" + s).collect(Collectors.joining(", "));
        return srids.length > 100 ? list + " ... (" + srids.length + " in total)" : list;
    }

    private String coverageHtml( LayerEntry entry ) {
        StringBuilder sb = new StringBuilder(HTML_START);
        sb.append("<h2>").append(escape(entry.title != null ? entry.title : entry.name)).append("</h2><table cellpadding='3'>");
        row(sb, "Coverage id", escape(entry.name));
        if (summary != null) {
            if (summary.getAbstract() != null)
                row(sb, "Abstract", escape(summary.getAbstract()).replace("\n", "<br>"));
            row(sb, "Bounds", escape(WebMapsUi.axesString(summary.getBoundingBox()))
                    + (summary.getBoundingBoxSrid() != null ? " (EPSG:" + summary.getBoundingBoxSrid() + ")" : ""));
        }
        row(sb, "WGS84 bounds", entry.wgs84 != null ? escape(bboxString(entry.wgs84)) : "<i>not declared</i>");
        sb.append("</table>");

        sb.append("<h3>DescribeCoverage</h3>");
        if (describeUrl != null)
            sb.append("<p>").append(escape(describeUrl)).append("</p>");
        if (describe != null) {
            sb.append("<table cellpadding='3'>");
            row(sb, "Envelope", escape(WebMapsUi.axesString(describe.getCoverageEnvelope()))
                    + (describe.getCoverageEnvelopeSrid() != null ? " (EPSG:" + describe.getCoverageEnvelopeSrid() + ")" : ""));
            row(sb, "World axes", escape(Arrays.toString(describe.getWorldAxisLabels())));
            row(sb, "Grid axes", escape(Arrays.toString(describe.getGridAxisLabels())));
            try {
                row(sb, "Native format", escape(describe.getNativeFormat()));
                List<String> formats = describe.getSupportedFormats();
                row(sb, "Formats", formats != null ? escape(String.join(", ", formats)) : "<i>see the service</i>");
                int[] srids = describe.getSupportedSrids();
                row(sb, "CRS", srids != null ? escape(sridList(srids)) : "<i>see the service</i>");
            } catch (Exception e) {
                row(sb, "Error", escape(e.getMessage()));
            }
            sb.append("</table><h3>Full description</h3><pre>").append(escape(describe.toString())).append("</pre>");
        } else if (describeError != null) {
            sb.append("<p>").append(WebMapsUi.colored(WebMapsUi.KO_COLOR, describeError)).append("</p>");
        } else {
            sb.append("<p><i>Press Describe to read it.</i></p>");
        }
        if (summary != null)
            sb.append("<h3>Capabilities summary</h3><pre>").append(escape(summary.toString())).append("</pre>");
        sb.append(HTML_END);
        return sb.toString();
    }
}
