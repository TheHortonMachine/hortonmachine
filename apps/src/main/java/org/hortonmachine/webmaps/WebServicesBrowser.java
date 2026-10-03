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

import static org.hortonmachine.webmaps.WebMapsUi.addRow;
import static org.hortonmachine.webmaps.WebMapsUi.button;
import static org.hortonmachine.webmaps.WebMapsUi.buttons;
import static org.hortonmachine.webmaps.WebMapsUi.htmlPane;
import static org.hortonmachine.webmaps.WebMapsUi.onTextChange;
import static org.hortonmachine.webmaps.WebMapsUi.section;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.settings.SettingsController;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.utils.SlippyMapPanel;
import org.hortonmachine.webmaps.ServicePanel.LayerEntry;
import org.locationtech.jts.geom.Envelope;

/**
 * A testbed for OGC web services (WMS, WCS and WFS), to check how well a service is supported by
 * the HortonMachine: browse the layers and their metadata, run requests on a region, see the
 * results on the map and save them.
 *
 * <p>The connection, the layer list, the region, the map and the log are shared, while each
 * service type has its own request panel and result panel.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
@SuppressWarnings("serial")
public class WebServicesBrowser extends JPanel implements IOnCloseListener {
    private static final String PREF_HISTORY = "WEBMAPS_HISTORY_";
    private static final String PREF_TYPE = "WEBMAPS_SERVICE_TYPE";
    private static final int MAX_HISTORY = 20;

    private final Map<String, ServicePanel> servicePanels = new LinkedHashMap<>();
    private ServicePanel current;
    private boolean busy = false;
    private boolean switchingType = false;

    // connection
    private JComboBox<String> typeCombo;
    private JComboBox<String> urlCombo;
    private JComboBox<String> versionCombo;
    private JButton connectButton;

    // layers
    private JTextField layersFilterField;
    private DefaultListModel<LayerEntry> layersListModel = new DefaultListModel<>();
    private JList<LayerEntry> layersList;
    private JLabel layersCountLabel;

    // map
    private SlippyMapPanel mapPanel;
    private JToggleButton drawBboxButton;
    private JLabel positionLabel;

    // region
    private JCheckBox regionCheck;
    private JTextField westField, southField, eastField, northField;
    private boolean updatingRegionFields = false;

    // service specific
    private JPanel requestCards;
    private JPanel actionCards;
    private JPanel resultCards;

    // tabs
    private JTabbedPane tabs;
    private JEditorPane layerInfoPane;
    private JEditorPane serviceInfoPane;
    private JTextArea logArea;
    private static final int TAB_LAYER = 0;
    private static final int TAB_RESULT = 1;

    // status
    private JLabel statusLabel;
    private JProgressBar progressBar;

    public WebServicesBrowser() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1500, 950));

        for( ServicePanel panel : new ServicePanel[]{new WmsServicePanel(this), new WcsServicePanel(this),
                new WfsServicePanel(this)} ) {
            servicePanels.put(panel.getType(), panel);
        }

        add(createConnectionBar(), BorderLayout.NORTH);

        JSplitPane mapRequestSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createMapPanel(), createRequestPanel());
        mapRequestSplit.setResizeWeight(1.0);
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mapRequestSplit, createTabs());
        centerSplit.setResizeWeight(0.55);
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createLayersPanel(), centerSplit);
        mainSplit.setDividerLocation(300);
        add(mainSplit, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);

        String type = PreferencesHandler.getPreference(PREF_TYPE, "WMS");
        if (!servicePanels.containsKey(type))
            type = "WMS";
        typeCombo.setSelectedItem(type);
        switchType(type);
        setBusy(false, "Select a service type, insert the url of the service and connect.");
    }

    // ==================== UI construction ====================

    private JComponent createConnectionBar() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        left.add(new JLabel("Service:"));
        typeCombo = new JComboBox<>(servicePanels.keySet().toArray(new String[0]));
        typeCombo.setFont(typeCombo.getFont().deriveFont(Font.BOLD));
        typeCombo.addActionListener(e -> {
            if (!switchingType)
                switchType((String) typeCombo.getSelectedItem());
        });
        left.add(typeCombo);
        left.add(new JLabel("URL:"));
        panel.add(left, BorderLayout.WEST);

        urlCombo = new JComboBox<>();
        urlCombo.setEditable(true);
        urlCombo.setToolTipText("The address of the service, with or without the GetCapabilities parameters");
        urlCombo.addActionListener(e -> {
            if ("comboBoxEdited".equals(e.getActionCommand()))
                connect();
        });
        panel.add(urlCombo, BorderLayout.CENTER);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        right.add(new JLabel("Version:"));
        versionCombo = new JComboBox<>();
        versionCombo.setToolTipText("Force a protocol version, or let client and service negotiate it");
        right.add(versionCombo);
        connectButton = button("Connect", this::connect);
        right.add(connectButton);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    private JComponent createLayersPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        layersFilterField = new JTextField();
        layersFilterField.setToolTipText("Filter layers by name or title");
        onTextChange(layersFilterField, this::filterLayers);
        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.add(new JLabel("Filter:"), BorderLayout.WEST);
        top.add(layersFilterField, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);

        layersList = new JList<>(layersListModel);
        layersList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        layersList.setCellRenderer(new LayerCellRenderer());
        layersList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                selectLayer(layersList.getSelectedValue());
        });
        panel.add(new JScrollPane(layersList), BorderLayout.CENTER);

        layersCountLabel = new JLabel(" ");
        panel.add(layersCountLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent createMapPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        mapPanel = new SlippyMapPanel();
        mapPanel.setBboxListener(env -> {
            drawBboxButton.setSelected(false);
            mapPanel.setDrawMode(false);
            setRegionFields(env);
            regionCheck.setSelected(true);
        });
        mapPanel.setFootprintSelectionListener(index -> current.footprintSelected(index));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        drawBboxButton = new JToggleButton("Draw region");
        drawBboxButton.setToolTipText("Drag on the map to draw the region of the requests (or shift+drag anytime)");
        drawBboxButton.addActionListener(e -> mapPanel.setDrawMode(drawBboxButton.isSelected()));
        toolbar.add(drawBboxButton);
        toolbar.add(button("Zoom to layer", () -> {
            if (current.currentEntry != null)
                mapPanel.zoomToEnvelope(current.currentEntry.wgs84);
        }));
        toolbar.add(button("Zoom to region", () -> mapPanel.zoomToEnvelope(readRegionFields())));
        toolbar.add(Box.createHorizontalStrut(8));
        toolbar.add(new JLabel("Overlay"));
        JSlider opacitySlider = new JSlider(0, 100, 80);
        opacitySlider.setPreferredSize(new Dimension(90, opacitySlider.getPreferredSize().height));
        opacitySlider.setToolTipText("Opacity of the image results drawn on the map");
        opacitySlider.addChangeListener(e -> mapPanel.setOverlayOpacity(opacitySlider.getValue() / 100f));
        mapPanel.setOverlayOpacity(0.8f);
        toolbar.add(opacitySlider);
        toolbar.add(button("Clear", this::clearMapResults));
        positionLabel = new JLabel(" ");
        positionLabel.setForeground(Color.GRAY);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(positionLabel);
        mapPanel.setPositionListener(positionLabel::setText);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(mapPanel, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createRequestPanel() {
        JPanel panel = new WebMapsUi.WidthTrackingPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // region, shared by all the services
        JPanel regionPanel = section("Region (WGS84 lon/lat)");
        regionCheck = new JCheckBox("Restrict the requests to the region");
        regionCheck.setToolTipText("If not checked, the whole layer is requested");
        regionCheck.addActionListener(e -> regionChanged());
        westField = new JTextField(6);
        southField = new JTextField(6);
        eastField = new JTextField(6);
        northField = new JTextField(6);
        for( JTextField f : new JTextField[]{westField, southField, eastField, northField} ) {
            onTextChange(f, this::regionFieldsChanged);
        }
        addRow(regionPanel, 0, regionCheck);
        addRow(regionPanel, 1, new JLabel("West"), westField, new JLabel("East"), eastField);
        addRow(regionPanel, 2, new JLabel("South"), southField, new JLabel("North"), northField);
        addRow(regionPanel, 3, buttons(button("Map view", () -> {
            setRegionFields(mapPanel.getViewEnvelope());
            regionCheck.setSelected(true);
            regionChanged();
        }), button("Layer", () -> {
            if (current.currentEntry != null && current.currentEntry.wgs84 != null) {
                setRegionFields(current.currentEntry.wgs84);
                regionCheck.setSelected(true);
                regionChanged();
            }
        }), button("File...", this::regionFromFile), button("Clear", () -> {
            setRegionFields(null);
            regionCheck.setSelected(false);
            regionChanged();
        })));
        panel.add(regionPanel);

        // the options of each service
        requestCards = new JPanel(new CardLayout());
        actionCards = new JPanel(new CardLayout());
        for( ServicePanel servicePanel : servicePanels.values() ) {
            // keep each card at its height, the cardlayout would stretch it to the tallest one
            JPanel topAligned = new JPanel(new BorderLayout());
            topAligned.add(servicePanel.getRequestPanel(), BorderLayout.NORTH);
            requestCards.add(topAligned, servicePanel.getType());
            actionCards.add(servicePanel.getActionPanel(), servicePanel.getType());
        }
        requestCards.setAlignmentX(Component.LEFT_ALIGNMENT);
        panel.add(requestCards);
        panel.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        // keep the actions always visible
        JPanel requestPanel = new JPanel(new BorderLayout());
        requestPanel.add(scroll, BorderLayout.CENTER);
        requestPanel.add(actionCards, BorderLayout.SOUTH);
        requestPanel.setMinimumSize(new Dimension(380, 100));
        requestPanel.setPreferredSize(new Dimension(420, 100));
        return requestPanel;
    }

    private JComponent createTabs() {
        tabs = new JTabbedPane();

        layerInfoPane = htmlPane();
        tabs.addTab("Layer", new JScrollPane(layerInfoPane));

        resultCards = new JPanel(new CardLayout());
        for( ServicePanel servicePanel : servicePanels.values() ) {
            resultCards.add(servicePanel.getResultPanel(), servicePanel.getType());
        }
        tabs.addTab("Result", resultCards);

        serviceInfoPane = htmlPane();
        tabs.addTab("Service", new JScrollPane(serviceInfoPane));

        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel logPanel = new JPanel(new BorderLayout());
        JPanel logTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        logTools.add(button("Clear", () -> logArea.setText("")));
        logTools.add(button("Copy", () -> GuiUtilities.copyToClipboard(logArea.getText())));
        logPanel.add(logTools, BorderLayout.NORTH);
        logPanel.add(new JScrollPane(logArea), BorderLayout.CENTER);
        tabs.addTab("Log", logPanel);
        return tabs;
    }

    private JComponent createStatusBar() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        statusLabel = new JLabel(" ");
        panel.add(statusLabel, BorderLayout.CENTER);
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(160, 14));
        panel.add(progressBar, BorderLayout.EAST);
        return panel;
    }

    // ==================== actions ====================

    private void switchType( String type ) {
        if (busy) {
            // a task of the current service is running, stay on it
            switchingType = true;
            typeCombo.setSelectedItem(current.getType());
            switchingType = false;
            return;
        }
        current = servicePanels.get(type);
        PreferencesHandler.setPreference(PREF_TYPE, type);
        ((CardLayout) requestCards.getLayout()).show(requestCards, type);
        ((CardLayout) actionCards.getLayout()).show(actionCards, type);
        ((CardLayout) resultCards.getLayout()).show(resultCards, type);

        versionCombo.setModel(new DefaultComboBoxModel<>(current.getVersions()));
        urlCombo.setModel(new DefaultComboBoxModel<>(getHistory(current).toArray(new String[0])));
        if (current.connectedUrl != null)
            urlCombo.setSelectedItem(current.connectedUrl);

        clearMapResults();
        filterLayers();
        if (current.currentEntry != null) {
            layersList.setSelectedValue(current.currentEntry, true);
            mapPanel.setExtents(current.currentEntry.wgs84 != null ? List.of(current.currentEntry.wgs84) : null);
        } else {
            mapPanel.setExtents(null);
        }
        showLayerInfo(current.layerInfoHtml);
        showServiceInfo(current.serviceInfoHtml);
        current.regionChanged();
        statusLabel.setText(current.connectedUrl != null ? "Connected to " + current.connectedUrl : " ");
    }

    private void connect() {
        Object selected = urlCombo.getEditor().getItem();
        String url = selected == null ? "" : selected.toString().trim();
        if (url.isEmpty()) {
            GuiUtilities.showWarningMessage(this, "Please insert the url of a " + current.getType() + " service.");
            return;
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            GuiUtilities.showWarningMessage(this, "The url of the service has to start with http:// or https://");
            return;
        }
        String version = (String) versionCombo.getSelectedItem();
        String forcedVersion = version == null || version.equals(current.getVersions()[0]) ? null : version;
        ServicePanel panel = current;
        runTask("Connecting to " + current.getType() + " " + url + (forcedVersion != null ? " (version " + forcedVersion + ")" : ""),
                () -> {
                    panel.close();
                    long t0 = System.currentTimeMillis();
                    List<LayerEntry> layers = panel.connect(url, forcedVersion);
                    return new Object[]{layers, System.currentTimeMillis() - t0};
                }, result -> {
                    @SuppressWarnings("unchecked")
                    List<LayerEntry> layers = (List<LayerEntry>) result[0];
                    panel.entries = layers;
                    panel.currentEntry = null;
                    panel.connectedUrl = url;
                    panel.layerInfoHtml = null;
                    addToHistory(panel, url);
                    panel.connected();
                    clearMapResults();
                    mapPanel.setExtents(null);
                    filterLayers();
                    showLayerInfo(null);
                    showServiceInfo(panel.serviceInfoHtml);
                    tabs.setSelectedIndex(2);
                    String msg = "Connected to " + url + ": " + layers.size() + " layers, read in " + result[1] + " ms.";
                    log(msg);
                    statusLabel.setText(msg);
                });
    }

    private void filterLayers() {
        String filter = layersFilterField.getText().trim().toLowerCase();
        LayerEntry selected = current.currentEntry;
        layersListModel.clear();
        for( LayerEntry e : current.entries ) {
            String text = (e.name + " " + (e.title != null ? e.title : "")).toLowerCase();
            if (filter.isEmpty() || text.contains(filter))
                layersListModel.addElement(e);
        }
        if (selected != null && layersListModel.contains(selected))
            layersList.setSelectedValue(selected, true);
        layersCountLabel.setText(layersListModel.size() + " of " + current.entries.size() + " layers");
    }

    private void selectLayer( LayerEntry entry ) {
        if (entry == null || entry == current.currentEntry)
            return;
        current.currentEntry = entry;
        clearMapResults();
        mapPanel.setExtents(entry.wgs84 != null ? List.of(entry.wgs84) : null);
        if (entry.wgs84 != null)
            mapPanel.zoomToEnvelope(entry.wgs84);
        tabs.setSelectedIndex(TAB_LAYER);
        statusLabel.setText(current.getType() + " layer: " + entry.name);
        current.selectLayer(entry);
    }

    private void regionFromFile() {
        File[] files = GuiUtilities.showOpenFilesDialog(this, "Select a vector or raster file", false,
                PreferencesHandler.getLastFile(), null);
        if (files == null || files.length == 0)
            return;
        String path = files[0].getAbsolutePath();
        runTask("Reading the bounds of " + files[0].getName(), () -> {
            String lower = path.toLowerCase();
            boolean isRaster = false;
            for( String ext : HMConstants.SUPPORTED_RASTER_EXTENSIONS ) {
                if (lower.endsWith("." + ext) && !lower.endsWith("." + HMConstants.GPKG))
                    isRaster = true;
            }
            ReferencedEnvelope env = isRaster ? OmsRasterReader.readEnvelope(path) : OmsVectorReader.readEnvelope(path);
            if (env.getCoordinateReferenceSystem() == null)
                throw new IllegalArgumentException("The file has no CRS: unable to compute its WGS84 bounds.");
            return env.transform(DefaultGeographicCRS.WGS84, true);
        }, env -> {
            PreferencesHandler.setLastPath(path);
            setRegionFields(env);
            regionCheck.setSelected(true);
            regionChanged();
            mapPanel.zoomToEnvelope(env);
            log("Region from " + path + ": " + WebMapsUi.bboxString(env));
        });
    }

    private void clearMapResults() {
        mapPanel.setOverlay(null, null, false);
        mapPanel.setFootprints(null);
    }

    // ==================== shared services for the panels ====================

    SlippyMapPanel getMap() {
        return mapPanel;
    }

    /**
     * @return the region of the requests in WGS84 lon/lat, or null if the requests are not restricted to a region.
     * @throws IllegalArgumentException if the region is enabled but not valid.
     */
    Envelope getRegion() {
        if (!regionCheck.isSelected())
            return null;
        Envelope region = readRegionFields();
        if (region == null)
            throw new IllegalArgumentException("The region is not valid (lon -180..180, lat -90..90, west < east, south < north).");
        return region;
    }

    /**
     * @return the region if enabled and valid, else null, without complaining.
     */
    Envelope getRegionQuietly() {
        return regionCheck.isSelected() ? readRegionFields() : null;
    }

    void showLayerInfo( String html ) {
        layerInfoPane.setText(html != null ? html : WebMapsUi.HTML_START + "<i>Select a layer.</i>" + WebMapsUi.HTML_END);
        SwingUtilities.invokeLater(() -> layerInfoPane.setCaretPosition(0));
    }

    void showServiceInfo( String html ) {
        serviceInfoPane.setText(html != null ? html : WebMapsUi.HTML_START + "<i>Not connected.</i>" + WebMapsUi.HTML_END);
        SwingUtilities.invokeLater(() -> serviceInfoPane.setCaretPosition(0));
    }

    void showResultTab() {
        tabs.setSelectedIndex(TAB_RESULT);
    }

    /** Repaint the layer list after the info of an entry changed. */
    void layerEntryChanged() {
        layersList.repaint();
    }

    void setStatus( String message ) {
        statusLabel.setText(message);
    }

    boolean isBusy() {
        return busy;
    }

    /**
     * Run a task in background, one at a time, reporting errors in the log and in a dialog.
     */
    <T> void runTask( String name, Callable<T> task, Consumer<T> onSuccess ) {
        if (busy)
            return;
        setBusy(true, name + "...");
        log(name + "...");
        new SwingWorker<T, Void>(){
            @Override
            protected T doInBackground() throws Exception {
                return task.call();
            }

            @Override
            protected void done() {
                setBusy(false, " ");
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    logException(name + " failed", cause);
                    statusLabel.setText(name + " failed: " + cause.getMessage());
                    GuiUtilities.showErrorMessage(WebServicesBrowser.this, name + " failed:\n" + cause.getClass().getSimpleName()
                            + ": " + cause.getMessage() + "\n\nSee the Log tab for details.");
                } catch (Exception e) {
                    logException(name + " failed", e);
                }
            }
        }.execute();
    }

    private void setBusy( boolean busy, String message ) {
        this.busy = busy;
        connectButton.setEnabled(!busy);
        typeCombo.setEnabled(!busy);
        for( ServicePanel panel : servicePanels.values() ) {
            panel.setBusy(busy);
        }
        progressBar.setIndeterminate(busy);
        statusLabel.setText(message);
    }

    void log( String message ) {
        String line = new SimpleDateFormat("HH:mm:ss").format(new Date()) + "  " + message + "\n";
        SwingUtilities.invokeLater(() -> {
            logArea.append(line);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    void logException( String message, Throwable t ) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        log("ERROR: " + message + "\n" + sw);
    }

    // ==================== region fields ====================

    private void setRegionFields( Envelope env ) {
        updatingRegionFields = true;
        try {
            westField.setText(env == null ? "" : String.format(Locale.ROOT, "%.5f", env.getMinX()));
            southField.setText(env == null ? "" : String.format(Locale.ROOT, "%.5f", env.getMinY()));
            eastField.setText(env == null ? "" : String.format(Locale.ROOT, "%.5f", env.getMaxX()));
            northField.setText(env == null ? "" : String.format(Locale.ROOT, "%.5f", env.getMaxY()));
        } finally {
            updatingRegionFields = false;
        }
        mapPanel.setQueryBbox(env);
    }

    private void regionFieldsChanged() {
        if (!updatingRegionFields) {
            Envelope region = readRegionFields();
            mapPanel.setQueryBbox(region);
            if (region != null)
                regionCheck.setSelected(true);
            regionChanged();
        }
    }

    private void regionChanged() {
        current.regionChanged();
    }

    private Envelope readRegionFields() {
        try {
            double w = Double.parseDouble(westField.getText().trim());
            double s = Double.parseDouble(southField.getText().trim());
            double e = Double.parseDouble(eastField.getText().trim());
            double n = Double.parseDouble(northField.getText().trim());
            if (w < -180 || e > 180 || s < -90 || n > 90 || s >= n || w >= e)
                return null;
            return new Envelope(w, e, s, n);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    // ==================== history ====================

    private static List<String> getHistory( ServicePanel panel ) {
        LinkedHashSet<String> urls = new LinkedHashSet<>();
        for( String u : PreferencesHandler.getPreference(PREF_HISTORY + panel.getType(), new String[0]) ) {
            if (u != null && !u.isBlank())
                urls.add(u);
        }
        urls.addAll(List.of(panel.getPresets()));
        return new ArrayList<>(urls);
    }

    private void addToHistory( ServicePanel panel, String url ) {
        List<String> history = new ArrayList<>();
        history.add(url);
        for( String u : PreferencesHandler.getPreference(PREF_HISTORY + panel.getType(), new String[0]) ) {
            if (u != null && !u.isBlank() && !u.equals(url) && history.size() < MAX_HISTORY)
                history.add(u);
        }
        PreferencesHandler.setPreference(PREF_HISTORY + panel.getType(), history.toArray(new String[0]));
        if (panel == current && ((DefaultComboBoxModel<String>) urlCombo.getModel()).getIndexOf(url) < 0)
            urlCombo.insertItemAt(url, 0);
    }

    /**
     * Name and title on two lines. Two plain labels instead of an html one, since html labels
     * are not measured properly in list cells and long names got clipped.
     */
    private static class LayerCellRenderer extends JPanel implements ListCellRenderer<LayerEntry> {
        private final JLabel nameLabel = new JLabel();
        private final JLabel titleLabel = new JLabel();

        LayerCellRenderer() {
            setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
            setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
            nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
            add(nameLabel);
            add(titleLabel);
        }

        @Override
        public Component getListCellRendererComponent( JList< ? extends LayerEntry> list, LayerEntry entry, int index,
                boolean isSelected, boolean cellHasFocus ) {
            nameLabel.setText(entry.name);
            boolean hasTitle = entry.title != null && !entry.title.isBlank() && !entry.title.equals(entry.name);
            titleLabel.setText(hasTitle ? entry.title : " ");
            setToolTipText(hasTitle ? entry.name + " - " + entry.title : entry.name);
            setBackground(isSelected ? list.getSelectionBackground() : list.getBackground());
            nameLabel.setForeground(isSelected ? list.getSelectionForeground() : list.getForeground());
            titleLabel.setForeground(isSelected ? list.getSelectionForeground() : Color.GRAY);
            return this;
        }
    }

    // ==================== lifecycle ====================

    @Override
    public void onClose() {
        mapPanel.dispose();
        for( ServicePanel panel : servicePanels.values() ) {
            panel.close();
        }
        SettingsController.onCloseHandleSettings();
    }

    @Override
    public boolean canCloseWithoutPrompt() {
        return true;
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();
        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        WebServicesBrowser browser = new WebServicesBrowser();
        SettingsController.applySettings(browser);
        JFrame frame = gBridge.showWindow(browser, "HortonMachine Web Services Browser - " + org.hortonmachine.Version.getVersion());
        GuiUtilities.setDefaultFrameIcon(frame);
        GuiUtilities.addClosingListener(frame, browser);
        // optional arguments: service type (WMS, WCS, WFS) and url
        if (args.length > 1 && browser.servicePanels.containsKey(args[0].toUpperCase())) {
            SwingUtilities.invokeLater(() -> {
                browser.typeCombo.setSelectedItem(args[0].toUpperCase());
                browser.urlCombo.setSelectedItem(args[1]);
                browser.connect();
            });
        }
    }
}
