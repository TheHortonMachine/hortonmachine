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
package org.hortonmachine.stac;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.imageio.ImageIO;

import com.formdev.flatlaf.util.SystemFileChooser;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JTree;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.border.TitledBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableRowSorter;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.geojson.GeoJSONReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.stac.HMStacAsset;
import org.hortonmachine.gears.io.stac.HMStacCollection;
import org.hortonmachine.gears.io.stac.HMStacItem;
import org.hortonmachine.gears.io.stac.HMStacManager;
import org.hortonmachine.gears.io.stac.PlanetaryComputerMicrosoft;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetHandler;
import org.hortonmachine.gears.io.stac.assets.IHMStacAssetRasterHandler;
import org.hortonmachine.gears.io.stac.assets.handlers.CsvfileHandler;
import org.hortonmachine.gears.io.stac.assets.handlers.GeojsonHandler;
import org.hortonmachine.gears.io.stac.assets.handlers.GeopackageVectorHandler;
import org.hortonmachine.gears.io.stac.assets.handlers.ShapefileHandler;
import org.hortonmachine.gears.io.stac.assets.handlers.StyleFileHandler;
import org.hortonmachine.gears.io.stac.auth.HMS3Authentication;
import org.hortonmachine.gears.io.stac.auth.HMS3Location;
import org.hortonmachine.gears.io.stac.auth.HMStacAccess;
import org.hortonmachine.gears.io.stac.auth.HMStacResponse;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.HMRaster.HMRasterWritableBuilder;
import org.hortonmachine.gears.libs.modules.HMRaster.MergeMode;
import org.hortonmachine.gears.libs.monitor.DummyProgressMonitor;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.GuiUtilities.IOnCloseListener;
import org.hortonmachine.utils.CsvViewer;
import org.hortonmachine.utils.MetadataTree;
import org.hortonmachine.utils.RowTableModel;
import org.hortonmachine.utils.SlippyMapPanel;
import org.hortonmachine.webmaps.RasterPreview;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A browser for STAC catalogs, to check how well a service is supported by the HortonMachine:
 * browse collections and their metadata, run time/region queries and preview which assets
 * would be downloaded and whether HM has a handler to read them.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
@SuppressWarnings("serial")
public class StacBrowser extends JPanel implements IOnCloseListener {
    private static final String PREF_CATALOGS = "STAC_BROWSER_CATALOGS";
    private static final String PREF_AWS_PROFILE = "STAC_BROWSER_AWS_PROFILE";
    private static final String PREF_AWS_REGION = "STAC_BROWSER_AWS_REGION";
    private static final String[] PRESET_CATALOGS = {//
            "https://earth-search.aws.element84.com/v1", //
            "https://planetarycomputer.microsoft.com/api/stac/v1", //
            "https://stac.dataspace.copernicus.eu/v1", //
            "https://stac.terrascope.be"//
    };
    private static final int MAX_ACCESS_CHECKS = 50;
    private static final long MAX_EXPORT_CELLS = 100_000_000L;
    /** Larger vector assets are shown on the map only after confirming the download. */
    private static final long MAX_MAP_VECTOR_BYTES = 100L * 1024 * 1024;
    private static final int MAX_MAP_FEATURES = 100_000;
    private static final String NO_ITEM_SELECTED = "Select an item to see its thumbnail.";
    private static final String UNSUPPORTED = "✗ unsupported";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    /** An item found by a search, with the info shown in the tables precomputed. */
    private static class ItemRow {
        HMStacItem item;
        Geometry footprint;
        String datetime;
        Integer epsg;
        Object cloudCover;
        Object platform;
        List<HMStacAsset> assets;
        int supportedCount;
        /** Version and status from the STAC Version extension, null if the item doesn't use it. */
        String version;
        String versionStatus;
    }

    /** An asset key found in the search results, aggregated over the items. */
    private static class AssetKeyRow {
        String key;
        boolean selected;
        Set<String> types = new LinkedHashSet<>();
        Set<String> handlers = new LinkedHashSet<>();
        int count;
        int supported;
    }

    /** A single asset that would be downloaded. */
    private static class DownloadRow {
        ItemRow itemRow;
        HMStacAsset asset;
        String href;
        String access = "";
        String size = "";
        long bytes = -1;
    }

    private HMStacManager manager;
    private HMStacCollection currentCollection;
    private List<HMStacCollection> allCollections = new ArrayList<>();
    private List<ItemRow> itemRows = new ArrayList<>();
    private final UiMonitor monitor = new UiMonitor();
    private ThumbnailPanel thumbnailPanel;
    private final ExecutorService thumbnailLoader = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "StacBrowser thumbnail loader");
        t.setDaemon(true);
        return t;
    });
    private final Map<String, BufferedImage> thumbnailCache = Collections.synchronizedMap(new LinkedHashMap<>(16, 0.75f, true){
        protected boolean removeEldestEntry( Map.Entry<String, BufferedImage> eldest ) {
            return size() > 50;
        }
    });
    private boolean busy = false;

    // connection
    private JComboBox<String> catalogCombo;
    private JButton connectButton;
    private JPanel awsPanel;
    private JComboBox<String> awsProfileCombo;
    private JTextField awsRegionField;
    /** The access context of the current catalog, null for public ones. */
    private HMStacAccess access;

    // collections
    private JTextField collectionsFilterField;
    private DefaultListModel<HMStacCollection> collectionsListModel = new DefaultListModel<>();
    private JList<HMStacCollection> collectionsList;
    private JLabel collectionsCountLabel;

    // map
    private SlippyMapPanel mapPanel;
    private JToggleButton drawBboxButton;
    private JLabel positionLabel;

    // query
    private JCheckBox bboxCheck;
    private JTextField westField, southField, eastField, northField;
    private JCheckBox timeCheck;
    private JTextField fromField, toField;
    private JCheckBox cqlCheck;
    private JTextField cqlField;
    private JSpinner maxItemsSpinner;
    private JButton searchButton;
    private JLabel searchInfoLabel;
    private TitledBorder searchTitle;
    private boolean updatingBboxFields = false;

    // tabs
    private JTabbedPane tabs;
    private JEditorPane serviceInfoPane;
    private JEditorPane collectionInfoPane;
    private JTree collectionTree;
    private RowTableModel<ItemRow> itemsModel;
    private JTable itemsTable;
    private JTree itemTree;
    private RowTableModel<HMStacAsset> itemAssetsModel;
    private RowTableModel<AssetKeyRow> assetKeysModel;
    private RowTableModel<DownloadRow> downloadsModel;
    private JTable downloadsTable;
    private JLabel downloadSummaryLabel;
    private JButton checkAccessButton;
    private JTextArea logArea;

    // status
    private JLabel statusLabel;
    private JProgressBar progressBar;
    private JButton cancelButton;

    public StacBrowser() {
        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(1500, 950));

        add(createConnectionBar(), BorderLayout.NORTH);

        JSplitPane mapQuerySplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createMapPanel(), createQueryPanel());
        mapQuerySplit.setResizeWeight(1.0);
        JSplitPane centerSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, mapQuerySplit, createTabs());
        centerSplit.setResizeWeight(0.55);
        JSplitPane mainSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, createCollectionsPanel(), centerSplit);
        mainSplit.setDividerLocation(300);
        add(mainSplit, BorderLayout.CENTER);

        add(createStatusBar(), BorderLayout.SOUTH);
        setBusy(false, "Select a catalog and connect.");
    }

    // ==================== UI construction ====================

    private JComponent createConnectionBar() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(new JLabel("STAC catalog:"), BorderLayout.WEST);

        catalogCombo = new JComboBox<>(getCatalogHistory().toArray(new String[0]));
        catalogCombo.setEditable(true);
        catalogCombo.addActionListener(e -> {
            if ("comboBoxEdited".equals(e.getActionCommand()))
                connect();
        });
        panel.add(catalogCombo, BorderLayout.CENTER);

        // credentials, used only for catalogs on S3 (s3://bucket/... or the https addresses of a bucket)
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        List<String> profiles = new ArrayList<>();
        profiles.add("");
        profiles.addAll(HMS3Authentication.listProfiles());
        awsProfileCombo = new JComboBox<>(profiles.toArray(new String[0]));
        awsProfileCombo.setEditable(true);
        awsProfileCombo.setSelectedItem(PreferencesHandler.getPreference(PREF_AWS_PROFILE, ""));
        awsProfileCombo.setToolTipText("Profile of ~/.aws/credentials, used for catalogs on S3 (empty = default profile)");
        awsRegionField = new JTextField(PreferencesHandler.getPreference(PREF_AWS_REGION, ""), 9);
        awsRegionField.setToolTipText("AWS region, needed for s3:// addresses if not in the profile config (e.g. us-west-2)");
        awsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        awsPanel.add(new JLabel("AWS profile:"));
        awsPanel.add(awsProfileCombo);
        awsPanel.add(new JLabel("Region:"));
        awsPanel.add(awsRegionField);
        right.add(awsPanel);
        connectButton = new JButton("Connect");
        connectButton.addActionListener(e -> connect());
        right.add(connectButton);
        panel.add(right, BorderLayout.EAST);

        // the credentials are shown only for catalogs on S3
        catalogCombo.addActionListener(e -> updateAwsPanel());
        if (catalogCombo.getEditor().getEditorComponent() instanceof JTextField editorField)
            onTextChange(editorField, this::updateAwsPanel);
        updateAwsPanel();
        return panel;
    }

    private void updateAwsPanel() {
        Object item = catalogCombo.getEditor().getItem();
        boolean isS3 = item != null && HMS3Location.parse(item.toString().trim()) != null;
        if (awsPanel.isVisible() != isS3) {
            awsPanel.setVisible(isS3);
            awsPanel.getParent().revalidate();
        }
    }

    private JComponent createCollectionsPanel() {
        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        collectionsFilterField = new JTextField();
        collectionsFilterField.setToolTipText("Filter collections by id or title");
        onTextChange(collectionsFilterField, this::filterCollections);
        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.add(new JLabel("Filter:"), BorderLayout.WEST);
        top.add(collectionsFilterField, BorderLayout.CENTER);
        panel.add(top, BorderLayout.NORTH);

        collectionsList = new JList<>(collectionsListModel);
        collectionsList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        collectionsList.setCellRenderer(new DefaultListCellRenderer(){
            @Override
            public Component getListCellRendererComponent( JList< ? > list, Object value, int index, boolean isSelected,
                    boolean cellHasFocus ) {
                HMStacCollection c = (HMStacCollection) value;
                String title = c.getTitle() != null ? escape(c.getTitle()) : "";
                String text = "<html><b>" + escape(c.getId()) + "</b><br><font color='#777777'>" + title + "</font></html>";
                JLabel label = (JLabel) super.getListCellRendererComponent(list, text, index, isSelected, cellHasFocus);
                label.setBorder(BorderFactory.createEmptyBorder(3, 4, 3, 4));
                return label;
            }
        });
        collectionsList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                selectCollection(collectionsList.getSelectedValue());
        });
        panel.add(new JScrollPane(collectionsList), BorderLayout.CENTER);

        collectionsCountLabel = new JLabel(" ");
        panel.add(collectionsCountLabel, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent createMapPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        mapPanel = new SlippyMapPanel();
        mapPanel.setBboxListener(env -> {
            drawBboxButton.setSelected(false);
            mapPanel.setDrawMode(false);
            setBboxFields(env);
            bboxCheck.setSelected(true);
        });
        mapPanel.setFootprintSelectionListener(index -> {
            if (index >= 0) {
                int viewRow = itemsTable.convertRowIndexToView(index);
                itemsTable.getSelectionModel().setSelectionInterval(viewRow, viewRow);
                itemsTable.scrollRectToVisible(itemsTable.getCellRect(viewRow, 0, true));
                tabs.setSelectedIndex(1);
            } else {
                itemsTable.clearSelection();
            }
        });

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        drawBboxButton = new JToggleButton("Draw bbox");
        drawBboxButton.setToolTipText("Drag on the map to draw the query region (or shift+drag anytime)");
        drawBboxButton.addActionListener(e -> mapPanel.setDrawMode(drawBboxButton.isSelected()));
        toolbar.add(drawBboxButton);
        toolbar.add(button("Zoom to collection", this::zoomToCollection));
        toolbar.add(button("Zoom to results", this::zoomToResults));
        toolbar.add(button("Zoom to bbox", () -> mapPanel.zoomToEnvelope(readBboxFields())));
        positionLabel = new JLabel(" ");
        positionLabel.setForeground(Color.GRAY);
        toolbar.add(Box.createHorizontalStrut(12));
        toolbar.add(positionLabel);
        mapPanel.setPositionListener(positionLabel::setText);

        panel.add(toolbar, BorderLayout.NORTH);
        panel.add(mapPanel, BorderLayout.CENTER);
        return panel;
    }

    private JComponent createQueryPanel() {
        JPanel panel = new WidthTrackingPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        // region
        JPanel regionPanel = section("Region (WGS84 lon/lat)");
        bboxCheck = new JCheckBox("Filter by bounding box");
        westField = new JTextField(6);
        southField = new JTextField(6);
        eastField = new JTextField(6);
        northField = new JTextField(6);
        for( JTextField f : new JTextField[]{westField, southField, eastField, northField} ) {
            onTextChange(f, this::bboxFieldsChanged);
        }
        addRow(regionPanel, 0, bboxCheck);
        addRow(regionPanel, 1, new JLabel("West"), westField, new JLabel("East"), eastField);
        addRow(regionPanel, 2, new JLabel("South"), southField, new JLabel("North"), northField);
        addRow(regionPanel, 3, buttons(button("Map view", () -> {
            setBboxFields(mapPanel.getViewEnvelope());
            bboxCheck.setSelected(true);
        }), button("Collection", () -> {
            Envelope env = getCollectionEnvelope(currentCollection);
            if (env != null) {
                setBboxFields(env);
                bboxCheck.setSelected(true);
            }
        }), button("Clear", () -> {
            setBboxFields(null);
            bboxCheck.setSelected(false);
        })));
        panel.add(regionPanel);

        // time
        JPanel timePanel = section("Time range (UTC)");
        timeCheck = new JCheckBox("Filter by time");
        fromField = new JTextField(12);
        toField = new JTextField(12);
        fromField.setToolTipText("yyyy-MM-dd or yyyy-MM-dd HH:mm:ss");
        toField.setToolTipText("yyyy-MM-dd (whole day included) or yyyy-MM-dd HH:mm:ss");
        Runnable timeChanged = () -> {
            if (!fromField.getText().isBlank() && !toField.getText().isBlank())
                timeCheck.setSelected(true);
        };
        onTextChange(fromField, timeChanged);
        onTextChange(toField, timeChanged);
        addRow(timePanel, 0, timeCheck);
        addRow(timePanel, 1, new JLabel("From"), fromField);
        addRow(timePanel, 2, new JLabel("To"), toField);
        addRow(timePanel, 3, buttons(button("Collection extent", this::setTimeFromCollection), button("Last 30 days", () -> {
            long now = System.currentTimeMillis();
            fromField.setText(formatDate(new Date(now - 30L * 24 * 3600 * 1000)));
            toField.setText(formatDate(new Date(now)));
            timeCheck.setSelected(true);
        })));
        panel.add(timePanel);

        // cql
        JPanel cqlPanel = section("Filter (CQL, needs the filter extension)");
        cqlCheck = new JCheckBox("Filter by attributes");
        cqlField = new JTextField(12);
        cqlField.setToolTipText("e.g.: \"eo:cloud_cover\" < 20");
        onTextChange(cqlField, () -> cqlCheck.setSelected(!cqlField.getText().isBlank()));
        addRow(cqlPanel, 0, cqlCheck);
        addRow(cqlPanel, 1, cqlField);
        panel.add(cqlPanel);

        // run
        JPanel runPanel = section("Search");
        searchTitle = (TitledBorder) runPanel.getBorder();
        maxItemsSpinner = new JSpinner(new SpinnerNumberModel(100, 0, 1_000_000, 50));
        maxItemsSpinner.setToolTipText("Stop paging once this many items are fetched (0 = fetch all pages)");
        searchButton = new JButton("Search items");
        searchButton.setFont(searchButton.getFont().deriveFont(Font.BOLD));
        searchButton.addActionListener(e -> search());
        searchInfoLabel = new JLabel(" ");
        addRow(runPanel, 0, new JLabel("Max items (0 = all)"), maxItemsSpinner);
        addRow(runPanel, 1, searchButton);
        addRow(runPanel, 2, searchInfoLabel);
        panel.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(panel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(null);
        // keep the search section always visible
        JPanel queryPanel = new JPanel(new BorderLayout());
        queryPanel.add(scroll, BorderLayout.CENTER);
        queryPanel.add(runPanel, BorderLayout.SOUTH);
        queryPanel.setMinimumSize(new Dimension(360, 100));
        queryPanel.setPreferredSize(new Dimension(400, 100));
        return queryPanel;
    }

    private JComponent createTabs() {
        tabs = new JTabbedPane();

        // collection
        collectionInfoPane = htmlPane();
        collectionTree = MetadataTree.createTree();
        JPanel treePanel = new JPanel(new BorderLayout());
        JPanel treeTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        treeTools.add(new JLabel("Full metadata"));
        treeTools.add(button("Copy as JSON", () -> {
            if (currentCollection != null)
                GuiUtilities.copyToClipboard(toJson(currentCollection.getOtherFields()));
        }));
        treePanel.add(treeTools, BorderLayout.NORTH);
        treePanel.add(new JScrollPane(collectionTree), BorderLayout.CENTER);
        JSplitPane collectionSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(collectionInfoPane), treePanel);
        collectionSplit.setResizeWeight(0.5);
        tabs.addTab("Collection", collectionSplit);

        // items
        itemsModel = new RowTableModel<ItemRow>()//
                .col("Id", String.class, r -> r.item.getId())//
                .col("Datetime (UTC)", String.class, r -> r.datetime)//
                .col("Version", String.class, r -> r.version)//
                .col("Status", String.class, r -> r.versionStatus)//
                .col("EPSG", Integer.class, r -> r.epsg)//
                .col("Cloud %", Double.class, r -> r.cloudCover instanceof Number n ? Math.round(n.doubleValue() * 10) / 10.0 : null)//
                .col("Platform", String.class, r -> r.platform == null ? "" : String.valueOf(r.platform))//
                .col("Assets", Integer.class, r -> r.assets.size())//
                .col("HM readable", Integer.class, r -> r.supportedCount);
        itemsTable = table(itemsModel);
        itemsTable.getColumnModel().getColumn(0).setPreferredWidth(320);
        itemsTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        itemsTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting())
                selectItem();
        });
        itemsTable.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent e ) {
                ItemRow row = getSelectedItemRow();
                if (e.getClickCount() == 2 && row != null && row.footprint != null)
                    mapPanel.zoomToEnvelope(row.footprint.getEnvelopeInternal());
            }
        });
        itemTree = MetadataTree.createTree();
        itemAssetsModel = new RowTableModel<HMStacAsset>()//
                .col("Key", String.class, a -> a.getId())//
                .col("Title", String.class, a -> a.getTitle())//
                .col("Type", String.class, a -> a.getType())//
                .col("HM handler", String.class, a -> handlerName(a))//
                .col("EPSG", Integer.class, a -> a.getEpsg())//
                .col("Href", String.class, a -> href(a));
        JTabbedPane itemTabs = new JTabbedPane();
        thumbnailPanel = new ThumbnailPanel();
        itemTabs.addTab("Preview", thumbnailPanel);
        itemTabs.addTab("Assets", new JScrollPane(table(itemAssetsModel)));
        itemTabs.addTab("Metadata", new JScrollPane(itemTree));
        JSplitPane itemsSplit = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(itemsTable), itemTabs);
        itemsSplit.setResizeWeight(0.5);
        tabs.addTab("Items", itemsSplit);

        // download preview
        assetKeysModel = new RowTableModel<AssetKeyRow>()//
                .col("Use", Boolean.class, r -> r.selected, ( r, v ) -> {
                    r.selected = (Boolean) v;
                    refreshDownloads();
                })//
                .col("Asset key", String.class, r -> r.key)//
                .col("Types", String.class, r -> String.join(", ", r.types))//
                .col("In items", Integer.class, r -> r.count)//
                .col("HM readable", Integer.class, r -> r.supported)//
                .col("HM handler", String.class, r -> String.join(", ", r.handlers));
        JTable assetKeysTable = table(assetKeysModel);
        assetKeysTable.getColumnModel().getColumn(0).setMaxWidth(50);
        downloadsModel = new RowTableModel<DownloadRow>()//
                .col("Item", String.class, r -> r.itemRow.item.getId())//
                .col("Datetime (UTC)", String.class, r -> r.itemRow.datetime)//
                .col("Asset", String.class, r -> r.asset.getId())//
                .col("HM handler", String.class, r -> handlerName(r.asset))//
                .col("EPSG", Integer.class, r -> r.asset.getEpsg() != null ? r.asset.getEpsg() : r.itemRow.epsg)//
                .col("Access", String.class, r -> r.access)//
                .col("Size", String.class, r -> r.size)//
                .col("Href", String.class, r -> r.href);
        downloadsTable = table(downloadsModel);
        downloadsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        JPopupMenu downloadsPopup = new JPopupMenu();
        JMenuItem downloadItem = new JMenuItem("Download whole files...");
        downloadItem.addActionListener(e -> downloadSelected());
        JMenuItem clipItem = new JMenuItem("Export clipped to bbox (GeoTIFF)...");
        clipItem.addActionListener(e -> exportClippedSelected());
        JMenuItem copyUrlItem = new JMenuItem("Copy URL");
        copyUrlItem.addActionListener(e -> GuiUtilities
                .copyToClipboard(getSelectedDownloads().stream().map(r -> r.href).collect(Collectors.joining("\n"))));
        JMenuItem showOnMapItem = new JMenuItem("Show on map");
        showOnMapItem.setToolTipText(
                "Rasters are read on the part of their item visible in the map, at screen resolution; vectors are read whole");
        showOnMapItem.addActionListener(e -> showOnMap(getSelectedDownloads().get(0)));
        JMenuItem removeFromMapItem = new JMenuItem("Remove from map");
        removeFromMapItem.addActionListener(e -> getSelectedDownloads().forEach(r -> mapPanel.removeLayer(mapLayerId(r))));
        JMenuItem removeAllFromMapItem = new JMenuItem("Remove all from map");
        removeAllFromMapItem.addActionListener(e -> mapPanel.removeAllLayers());
        JMenuItem viewTableItem = new JMenuItem("View table...");
        viewTableItem.addActionListener(e -> viewCsv(getSelectedDownloads().get(0)));
        downloadsPopup.add(downloadItem);
        downloadsPopup.add(clipItem);
        downloadsPopup.addSeparator();
        downloadsPopup.add(showOnMapItem);
        downloadsPopup.add(removeFromMapItem);
        downloadsPopup.add(removeAllFromMapItem);
        downloadsPopup.add(viewTableItem);
        downloadsPopup.addSeparator();
        downloadsPopup.add(copyUrlItem);
        downloadsTable.addMouseListener(new MouseAdapter(){
            @Override
            public void mousePressed( MouseEvent e ) {
                showPopup(e);
            }

            @Override
            public void mouseReleased( MouseEvent e ) {
                showPopup(e);
            }

            private void showPopup( MouseEvent e ) {
                if (!e.isPopupTrigger())
                    return;
                // right click on a row not in the selection selects that row
                int row = downloadsTable.rowAtPoint(e.getPoint());
                if (row >= 0 && !downloadsTable.isRowSelected(row))
                    downloadsTable.setRowSelectionInterval(row, row);
                List<DownloadRow> selected = getSelectedDownloads();
                if (!selected.isEmpty()) {
                    long rasterCount = selected.stream().filter(r -> isRasterAsset(r.asset)).count();
                    downloadItem.setText("Download " + selected.size() + " whole file(s)...");
                    downloadItem.setEnabled(!busy);
                    clipItem.setText("Export " + rasterCount + " raster(s) clipped to bbox (GeoTIFF)...");
                    clipItem.setEnabled(!busy && rasterCount > 0 && readBboxFields() != null);
                    clipItem.setToolTipText(readBboxFields() == null ? "Needs a valid query bbox" : null);
                    HMStacAsset first = selected.get(0).asset;
                    showOnMapItem.setEnabled(!busy && selected.size() == 1 && (isRasterAsset(first) || isVectorAsset(first)));
                    removeFromMapItem.setEnabled(selected.stream().anyMatch(r -> mapPanel.hasLayer(mapLayerId(r))));
                    removeAllFromMapItem.setEnabled(mapPanel.hasLayers());
                    viewTableItem.setEnabled(!busy && selected.size() == 1 && isCsvAsset(first));
                    downloadsPopup.show(downloadsTable, e.getX(), e.getY());
                }
            }
        });

        JPanel downloadTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        downloadTools.add(button("Select readable keys", () -> selectAssetKeys(true)));
        downloadTools.add(button("Select none", () -> selectAssetKeys(false)));
        downloadTools.add(Box.createHorizontalStrut(12));
        checkAccessButton = button("Check access", this::checkAccess);
        checkAccessButton.setToolTipText("HTTP HEAD request on the selected assets (or the first " + MAX_ACCESS_CHECKS + ")");
        downloadTools.add(checkAccessButton);
        downloadTools.add(button("Copy URLs", () -> GuiUtilities
                .copyToClipboard(downloadsModel.getRows().stream().map(r -> r.href).collect(Collectors.joining("\n")))));
        downloadTools.add(button("Export CSV...", this::exportCsv));
        downloadSummaryLabel = new JLabel(" ");
        downloadSummaryLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        JPanel downloadHeader = new JPanel(new BorderLayout());
        downloadHeader.add(downloadTools, BorderLayout.NORTH);
        downloadHeader.add(downloadSummaryLabel, BorderLayout.SOUTH);

        JPanel downloadsPanel = new JPanel(new BorderLayout());
        downloadsPanel.add(downloadHeader, BorderLayout.NORTH);
        downloadsPanel.add(new JScrollPane(downloadsTable), BorderLayout.CENTER);
        JSplitPane downloadSplit = new JSplitPane(JSplitPane.VERTICAL_SPLIT, new JScrollPane(assetKeysTable), downloadsPanel);
        downloadSplit.setDividerLocation(150);
        tabs.addTab("Download preview", downloadSplit);

        // service
        serviceInfoPane = htmlPane();
        tabs.addTab("Service", new JScrollPane(serviceInfoPane));

        // log
        logArea = new JTextArea();
        logArea.setEditable(false);
        logArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JPanel logPanel = new JPanel(new BorderLayout());
        JPanel logTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        logTools.add(button("Clear", () -> logArea.setText("")));
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
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        progressBar = new JProgressBar();
        progressBar.setPreferredSize(new Dimension(160, 14));
        cancelButton = button("Cancel", () -> {
            monitor.setCanceled(true);
            log("Cancel requested.");
        });
        right.add(progressBar);
        right.add(cancelButton);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ==================== actions ====================

    private void connect() {
        Object selected = catalogCombo.getEditor().getItem();
        String url = selected == null ? "" : selected.toString().trim();
        if (url.isEmpty()) {
            GuiUtilities.showWarningMessage(this, "Please insert the url of a STAC catalog.");
            return;
        }
        // catalogs on S3 are read with the credentials of the chosen profile, limited to the catalog bucket
        HMStacAccess newAccess = null;
        String authInfo = "none";
        HMS3Location s3Location = HMS3Location.parse(url);
        if (s3Location != null) {
            String profile = String.valueOf(awsProfileCombo.getEditor().getItem()).trim();
            String region = awsRegionField.getText().trim();
            try {
                HMS3Authentication authentication = HMS3Authentication.fromProfile(profile.isEmpty() ? null : profile)
                        .forBuckets(s3Location.getBucket());
                if (!region.isEmpty())
                    authentication.setRegion(region);
                if (authentication.getRegion() == null && s3Location.getRegion() == null) {
                    GuiUtilities.showWarningMessage(this, "Please insert the AWS region of the bucket (e.g. us-west-2).");
                    return;
                }
                newAccess = new HMStacAccess(authentication);
                String usedRegion = s3Location.getRegion() != null ? s3Location.getRegion() : authentication.getRegion();
                authInfo = "AWS S3, profile " + (profile.isEmpty() ? "default" : profile) + ", region " + usedRegion + ", bucket "
                        + s3Location.getBucket();
                PreferencesHandler.setPreference(PREF_AWS_PROFILE, profile);
                PreferencesHandler.setPreference(PREF_AWS_REGION, region);
            } catch (Exception e) {
                GuiUtilities.showWarningMessage(this, "Unable to use the AWS credentials: " + e.getMessage());
                return;
            }
        }
        HMStacAccess fAccess = newAccess;
        String fAuthInfo = authInfo;
        HMStacManager oldManager = manager;
        runTask("Connecting to " + url, () -> {
            if (oldManager != null)
                oldManager.close();
            long t0 = System.currentTimeMillis();
            HMStacManager newManager = new HMStacManager(url, monitor);
            newManager.setAccess(fAccess);
            newManager.open();
            String conformance;
            try {
                conformance = newManager.getConformanceSummary();
            } catch (Exception e) {
                conformance = "Unable to read conformance: " + e.getMessage();
            }
            long t1 = System.currentTimeMillis();
            List<HMStacCollection> collections = newManager.getCollections();
            collections.sort(Comparator.comparing(HMStacCollection::getId, String.CASE_INSENSITIVE_ORDER));
            long t2 = System.currentTimeMillis();
            log("Landing page read in " + (t1 - t0) + " ms, " + collections.size() + " collections listed in " + (t2 - t1) + " ms.");
            boolean searchAvailable = newManager.isItemSearchAvailable();
            if (!searchAvailable)
                log("WARNING: the catalog doesn't support item search, items will be collected following the catalog links.");
            return new Object[]{newManager, conformance, collections, t1 - t0, t2 - t1, searchAvailable};
        }, result -> {
            manager = (HMStacManager) result[0];
            access = fAccess;
            @SuppressWarnings("unchecked")
            List<HMStacCollection> collections = (List<HMStacCollection>) result[2];
            allCollections = collections;
            addCatalogToHistory(url);
            showServiceInfo(url, (String) result[1], collections.size(), (Long) result[3], (Long) result[4], fAuthInfo,
                    (Boolean) result[5]);
            clearResults();
            currentCollection = null;
            filterCollections();
            statusLabel.setText("Connected to " + url + " - " + collections.size() + " collections.");
        });
    }

    private void filterCollections() {
        String filter = collectionsFilterField.getText().trim().toLowerCase();
        collectionsListModel.clear();
        for( HMStacCollection c : allCollections ) {
            String text = (c.getId() + " " + (c.getTitle() != null ? c.getTitle() : "")).toLowerCase();
            if (filter.isEmpty() || text.contains(filter))
                collectionsListModel.addElement(c);
        }
        collectionsCountLabel.setText(collectionsListModel.size() + " of " + allCollections.size() + " collections");
    }

    private void selectCollection( HMStacCollection collection ) {
        if (collection == null || collection == currentCollection)
            return;
        currentCollection = collection;
        clearResults();
        try {
            collectionInfoPane.setText(collectionHtml(collection));
            SwingUtilities.invokeLater(() -> collectionInfoPane.setCaretPosition(0));
        } catch (Exception e) {
            collectionInfoPane.setText("<html><body>Unable to show collection info: " + escape(e.getMessage()) + "</body></html>");
            logException("Unable to show collection info", e);
        }
        MetadataTree.setContent(collectionTree, collection.getOtherFields());
        Envelope env = getCollectionEnvelope(collection);
        mapPanel.setExtents(env != null ? List.of(env) : null);
        if (env != null)
            mapPanel.zoomToEnvelope(env);
        tabs.setSelectedIndex(0);
        statusLabel.setText("Collection: " + collection.getId());
        searchTitle.setTitle("Search in: " + collection.getId());
        searchButton.getParent().repaint();
    }

    private void search() {
        HMStacCollection collection = currentCollection;
        if (collection == null) {
            GuiUtilities.showWarningMessage(this, "Please select a collection first.");
            return;
        }
        // read the query parameters on the EDT
        Envelope bbox = null;
        Date from = null;
        Date to = null;
        String cql = null;
        try {
            if (bboxCheck.isSelected()) {
                bbox = readBboxFields();
                if (bbox == null)
                    throw new IllegalArgumentException("The bounding box is not valid (lon -180..180, lat -90..90, south < north).");
            }
            if (timeCheck.isSelected()) {
                from = parseDate(fromField.getText(), false);
                to = parseDate(toField.getText(), true);
                if (from.after(to))
                    throw new IllegalArgumentException("The start date is after the end date.");
            }
            if (cqlCheck.isSelected() && !cqlField.getText().isBlank()) {
                cql = cqlField.getText().trim();
            }
        } catch (Exception e) {
            GuiUtilities.showWarningMessage(this, e.getMessage());
            return;
        }
        int maxItems = (Integer) maxItemsSpinner.getValue();

        Envelope fBbox = bbox;
        Date fFrom = from;
        Date fTo = to;
        String fCql = cql;
        log("Search on " + collection.getId() + ": bbox=" + (fBbox == null ? "-" : bboxString(fBbox)) + ", time="
                + (fFrom == null ? "-" : formatDate(fFrom) + " / " + formatDate(fTo)) + ", filter=" + (fCql == null ? "-" : fCql)
                + ", max items=" + (maxItems <= 0 ? "all" : maxItems));
        runTask("Searching items of " + collection.getId(), () -> {
            collection.clearFilters();
            if (fBbox != null)
                collection.setBboxFilter(new double[]{fBbox.getMinX(), fBbox.getMinY(), fBbox.getMaxX(), fBbox.getMaxY()});
            if (fFrom != null)
                collection.setTimestampFilter(fFrom, fTo);
            if (fCql != null)
                collection.setCqlFilter(fCql);
            long t0 = System.currentTimeMillis();
            List<HMStacItem> items = collection.searchItems(maxItems);
            long elapsed = System.currentTimeMillis() - t0;
            List<ItemRow> rows = new ArrayList<>();
            for( HMStacItem item : items ) {
                rows.add(toItemRow(item));
            }
            return new Object[]{rows, collection.getLastMatchedCount(), elapsed, monitor.isCanceled()};
        }, result -> {
            @SuppressWarnings("unchecked")
            List<ItemRow> rows = (List<ItemRow>) result[0];
            Integer matched = (Integer) result[1];
            showResults(rows);
            String info = rows.size() + " items fetched";
            if (matched != null)
                info += " of " + matched + " matched";
            info += " in " + String.format("%.1f s", ((Long) result[2]) / 1000.0);
            if ((Boolean) result[3])
                info += " (canceled)";
            else if (maxItems > 0 && rows.size() >= maxItems)
                info += " (max reached)";
            searchInfoLabel.setText(info);
            log(info);
            statusLabel.setText(info);
            tabs.setSelectedIndex(1);
        });
    }

    private ItemRow toItemRow( HMStacItem item ) {
        ItemRow row = new ItemRow();
        row.item = item;
        SimpleFeature feature = item.getFeature();
        Object geom = feature.getDefaultGeometry();
        row.footprint = geom instanceof Geometry g ? g : null;
        row.datetime = item.getTimestamp();
        row.epsg = item.getEpsg();
        row.cloudCover = attribute(feature, "eo:cloud_cover");
        row.platform = attribute(feature, "platform");
        row.assets = item.getAllAssets();
        row.supportedCount = (int) row.assets.stream().filter(HMStacAsset::isValid).count();
        if (item.hasVersionInfo()) {
            // STAC Version extension: deprecated items are superseded by a newer version
            row.version = item.getVersion();
            row.versionStatus = item.isDeprecated() ? "superseded" : "current";
        }
        return row;
    }

    private void showResults( List<ItemRow> rows ) {
        itemRows = rows;
        itemsModel.setRows(rows);
        mapPanel.setFootprints(rows.stream().map(r -> r.footprint).collect(Collectors.toList()));
        itemAssetsModel.setRows(null);
        MetadataTree.setContent(itemTree, null);
        thumbnailPanel.setMessage(NO_ITEM_SELECTED);

        Map<String, AssetKeyRow> keys = new LinkedHashMap<>();
        for( ItemRow row : rows ) {
            for( HMStacAsset asset : row.assets ) {
                AssetKeyRow keyRow = keys.computeIfAbsent(asset.getId(), k -> {
                    AssetKeyRow r = new AssetKeyRow();
                    r.key = k;
                    return r;
                });
                keyRow.count++;
                if (asset.getType() != null)
                    keyRow.types.add(asset.getType());
                if (asset.isValid()) {
                    keyRow.supported++;
                    keyRow.handlers.add(handlerName(asset));
                }
            }
        }
        // by default preview everything HM is able to read
        keys.values().forEach(k -> k.selected = k.supported > 0);
        assetKeysModel.setRows(new ArrayList<>(keys.values()));
        refreshDownloads();
        zoomToResults();
    }

    private void clearResults() {
        itemRows = new ArrayList<>();
        itemsModel.setRows(null);
        itemAssetsModel.setRows(null);
        MetadataTree.setContent(itemTree, null);
        assetKeysModel.setRows(null);
        downloadsModel.setRows(null);
        mapPanel.setFootprints(null);
        mapPanel.removeAllLayers();
        searchInfoLabel.setText(" ");
        downloadSummaryLabel.setText(" ");
        thumbnailPanel.setMessage(NO_ITEM_SELECTED);
    }

    private void selectItem() {
        ItemRow row = getSelectedItemRow();
        if (row == null) {
            mapPanel.setSelectedFootprint(-1);
            itemAssetsModel.setRows(null);
            MetadataTree.setContent(itemTree, null);
            thumbnailPanel.setMessage(NO_ITEM_SELECTED);
            return;
        }
        mapPanel.setSelectedFootprint(itemsModel.indexOf(row));
        itemAssetsModel.setRows(row.assets);
        showThumbnail(row);

        Map<String, Object> metadata = new LinkedHashMap<>();
        SimpleFeature feature = row.item.getFeature();
        Map<String, Object> properties = HMStacItem.getAttributesMap(feature);
        properties.remove(feature.getFeatureType().getGeometryDescriptor() != null
                ? feature.getFeatureType().getGeometryDescriptor().getLocalName()
                : "geometry");
        metadata.put("properties", properties);
        Object top = feature.getUserData().get(GeoJSONReader.TOP_LEVEL_ATTRIBUTES);
        if (top instanceof Map< ? , ? > topMap) {
            topMap.forEach(( k, v ) -> metadata.put(String.valueOf(k), v));
        }
        MetadataTree.setContent(itemTree, metadata);
    }

    /**
     * @return the thumbnail asset of the item, if any.
     */
    private static HMStacAsset findThumbnail( ItemRow row ) {
        HMStacAsset byKey = null;
        for( HMStacAsset asset : row.assets ) {
            String type = asset.getType() != null ? asset.getType().toLowerCase() : "";
            if (!type.isEmpty() && !type.startsWith("image/png") && !type.startsWith("image/jpeg") && !type.startsWith("image/jpg")
                    && !type.startsWith("image/gif"))
                continue;
            JsonNode roles = asset.getAssetNode().get("roles");
            if (roles != null && roles.isArray()) {
                for( JsonNode role : roles ) {
                    if ("thumbnail".equals(role.asText()) || "overview".equals(role.asText()))
                        return asset;
                }
            }
            String key = asset.getId().toLowerCase();
            if (byKey == null && (key.equals("thumbnail") || key.equals("preview") || key.equals("rendered_preview")))
                byKey = asset;
        }
        return byKey;
    }

    private void showThumbnail( ItemRow row ) {
        HMStacAsset thumbnail = findThumbnail(row);
        if (thumbnail == null) {
            thumbnailPanel.setMessage("No thumbnail asset for this item.");
            return;
        }
        String href = href(thumbnail);
        BufferedImage cached = href != null ? thumbnailCache.get(href) : null;
        if (cached != null) {
            thumbnailPanel.setImage(cached, thumbnail.getId());
            return;
        }
        thumbnailPanel.setMessage("Loading thumbnail " + thumbnail.getId() + "...");
        HMStacAccess itemAccess = access;
        thumbnailLoader.submit(() -> {
            String message;
            try {
                BufferedImage image = loadImage(href, itemAccess);
                if (image != null) {
                    thumbnailCache.put(href, image);
                    SwingUtilities.invokeLater(() -> {
                        // only if the item is still the selected one
                        if (getSelectedItemRow() == row)
                            thumbnailPanel.setImage(image, thumbnail.getId());
                    });
                    return;
                }
                message = "Unable to decode the thumbnail (" + thumbnail.getType() + ").";
            } catch (Exception e) {
                message = "Thumbnail not available: " + e.getMessage();
            }
            String msg = message;
            log(msg + " " + href);
            SwingUtilities.invokeLater(() -> {
                if (getSelectedItemRow() == row)
                    thumbnailPanel.setMessage(msg);
            });
        });
    }

    private static BufferedImage loadImage( String href, HMStacAccess access ) throws Exception {
        if (href == null)
            throw new IllegalArgumentException("no href");
        if (access != null && access.isAuthenticated(href)) {
            try (HMStacResponse response = access.get(href)) {
                return ImageIO.read(response.getInputStream());
            }
        }
        if (!href.startsWith("http://") && !href.startsWith("https://"))
            throw new IllegalArgumentException("not an http url: " + href);
        if (PlanetaryComputerMicrosoft.isAzureBlob(href))
            href = PlanetaryComputerMicrosoft.getHrefWithToken(href);
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15))
                .build();
        HttpResponse<byte[]> response = client.send(
                HttpRequest.newBuilder(URI.create(href)).timeout(Duration.ofSeconds(30)).GET().build(),
                HttpResponse.BodyHandlers.ofByteArray());
        if (response.statusCode() >= 400)
            throw new IOException("HTTP " + response.statusCode());
        return ImageIO.read(new java.io.ByteArrayInputStream(response.body()));
    }

    /** Shows an image scaled to fit, or a message. */
    private static class ThumbnailPanel extends JPanel {
        private BufferedImage image;
        private String text = NO_ITEM_SELECTED;

        void setImage( BufferedImage image, String title ) {
            this.image = image;
            this.text = title + " (" + image.getWidth() + "x" + image.getHeight() + ")";
            repaint();
        }

        void setMessage( String message ) {
            this.image = null;
            this.text = message;
            repaint();
        }

        @Override
        protected void paintComponent( Graphics g ) {
            super.paintComponent(g);
            Graphics2D g2 = (Graphics2D) g;
            int textHeight = g2.getFontMetrics().getHeight();
            if (image != null) {
                int w = getWidth() - 8;
                int h = getHeight() - textHeight - 12;
                double scale = Math.min((double) w / image.getWidth(), (double) h / image.getHeight());
                int iw = (int) (image.getWidth() * scale);
                int ih = (int) (image.getHeight() * scale);
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                g2.drawImage(image, (getWidth() - iw) / 2, 4, iw, ih, null);
            }
            g2.setColor(Color.GRAY);
            g2.drawString(text, 6, getHeight() - 6);
        }
    }

    private ItemRow getSelectedItemRow() {
        int viewRow = itemsTable.getSelectedRow();
        if (viewRow < 0)
            return null;
        return itemsModel.getRow(itemsTable.convertRowIndexToModel(viewRow));
    }

    private void selectAssetKeys( boolean onlyReadable ) {
        for( AssetKeyRow r : assetKeysModel.getRows() ) {
            r.selected = onlyReadable && r.supported > 0;
        }
        assetKeysModel.fireTableDataChanged();
        refreshDownloads();
    }

    private void refreshDownloads() {
        Set<String> selectedKeys = assetKeysModel.getRows().stream().filter(r -> r.selected).map(r -> r.key)
                .collect(Collectors.toSet());
        List<DownloadRow> downloads = new ArrayList<>();
        int readable = 0;
        Set<ItemRow> items = new LinkedHashSet<>();
        for( ItemRow itemRow : itemRows ) {
            for( HMStacAsset asset : itemRow.assets ) {
                if (selectedKeys.contains(asset.getId())) {
                    DownloadRow d = new DownloadRow();
                    d.itemRow = itemRow;
                    d.asset = asset;
                    d.href = href(asset);
                    downloads.add(d);
                    items.add(itemRow);
                    if (asset.isValid())
                        readable++;
                }
            }
        }
        downloadsModel.setRows(downloads);
        downloadSummaryLabel.setText(downloads.size() + " assets from " + items.size() + " items: " + readable
                + " readable by HM, " + (downloads.size() - readable) + " without handler");
    }

    private void checkAccess() {
        List<DownloadRow> rows = new ArrayList<>();
        int[] selectedRows = downloadsTable.getSelectedRows();
        if (selectedRows.length > 0) {
            for( int viewRow : selectedRows ) {
                rows.add(downloadsModel.getRow(downloadsTable.convertRowIndexToModel(viewRow)));
            }
        } else {
            rows.addAll(downloadsModel.getRows());
        }
        if (rows.isEmpty())
            return;
        if (rows.size() > MAX_ACCESS_CHECKS) {
            log("Checking access only for the first " + MAX_ACCESS_CHECKS + " of " + rows.size() + " assets.");
            rows = rows.subList(0, MAX_ACCESS_CHECKS);
        }
        List<DownloadRow> toCheck = rows;
        runTask("Checking access of " + toCheck.size() + " assets", () -> checkRows(toCheck), ok -> {
            downloadsModel.fireTableDataChanged();
            String msg = ok + " of " + toCheck.size() + " checked assets are accessible.";
            log(msg);
            statusLabel.setText(msg);
        });
    }

    /**
     * Check access and size of the rows (to run in background).
     *
     * @return the number of accessible assets.
     */
    private int checkRows( List<DownloadRow> rows ) {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(15))
                .build();
        int ok = 0;
        for( DownloadRow row : rows ) {
            if (monitor.isCanceled())
                break;
            String[] result = checkUrl(client, row.href, access);
            row.access = result[0];
            row.size = result[1];
            row.bytes = Long.parseLong(result[2]);
            if (result[0].startsWith("200") || result[0].startsWith("206"))
                ok++;
            SwingUtilities.invokeLater(() -> downloadsModel.fireTableDataChanged());
        }
        return ok;
    }

    /**
     * Ask confirmation showing a summary and a scrollable list of details.
     */
    private boolean confirmWithList( String title, String summary, List<String> lines, JComponent extra ) {
        JTextArea area = new JTextArea(String.join("\n", lines));
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        JScrollPane scroll = new JScrollPane(area);
        scroll.setPreferredSize(new Dimension(760, Math.min(360, 40 + lines.size() * 18)));
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JLabel("<html>" + escape(summary).replace("\n", "<br>") + "</html>"), BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        if (extra != null)
            panel.add(extra, BorderLayout.SOUTH);
        return GuiUtilities.openConfirmDialogWithPanel(this, panel, title);
    }

    /**
     * Check a url with a HEAD request, falling back to a single byte range GET for servers not allowing HEAD.
     *
     * @return the status and the size.
     */
    private static String[] checkUrl( HttpClient client, String href, HMStacAccess access ) {
        if (href == null)
            return new String[]{"no href", "", "-1"};
        if (access != null && access.isAuthenticated(href)) {
            // protected: open the object with the credentials, the stream is closed right away
            try (HMStacResponse response = access.get(href)) {
                long size = response.getContentLength();
                return new String[]{"200 ok (authenticated)", size >= 0 ? humanSize(size) : "?", String.valueOf(size)};
            } catch (IOException e) {
                return new String[]{"denied/failed: " + e.getMessage(), "", "-1"};
            }
        }
        if (!href.startsWith("http://") && !href.startsWith("https://")) {
            int colon = href.indexOf(':');
            return new String[]{"not http" + (colon > 0 ? " (" + href.substring(0, colon) + ")" : ""), "", "-1"};
        }
        String prefix = "";
        if (PlanetaryComputerMicrosoft.isAzureBlob(href)) {
            // same signing done by the HM geotiff handler when reading
            try {
                href = PlanetaryComputerMicrosoft.getHrefWithToken(href);
                prefix = "signed, ";
            } catch (Exception e) {
                return new String[]{"signing failed: " + e.getMessage(), "", "-1"};
            }
        }
        try {
            URI uri = URI.create(href);
            HttpResponse<Void> response = client.send(HttpRequest.newBuilder(uri).method("HEAD", HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.ofSeconds(20)).build(), HttpResponse.BodyHandlers.discarding());
            int code = response.statusCode();
            long size = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (code == 405 || code == 501) {
                response = client.send(HttpRequest.newBuilder(uri).header("Range", "bytes=0-0").timeout(Duration.ofSeconds(20)).GET()
                        .build(), HttpResponse.BodyHandlers.discarding());
                code = response.statusCode();
                size = response.headers().firstValue("Content-Range").map(cr -> {
                    int slash = cr.lastIndexOf('/');
                    try {
                        return Long.parseLong(cr.substring(slash + 1).trim());
                    } catch (Exception e) {
                        return -1L;
                    }
                }).orElse(-1L);
            }
            return new String[]{code + (code >= 400 ? " denied/failed" : " ok") + (prefix.isEmpty() ? "" : " (signed)"),
                    size >= 0 ? humanSize(size) : "?", String.valueOf(size)};
        } catch (Exception e) {
            return new String[]{"error: " + e.getClass().getSimpleName(), "", "-1"};
        }
    }

    private List<DownloadRow> getSelectedDownloads() {
        List<DownloadRow> rows = new ArrayList<>();
        for( int viewRow : downloadsTable.getSelectedRows() ) {
            rows.add(downloadsModel.getRow(downloadsTable.convertRowIndexToModel(viewRow)));
        }
        return rows;
    }

    /**
     * Ask for a folder with the operating system dialog (falls back to the swing one where not available).
     */
    private File askOutputFolder( String title ) {
        SystemFileChooser chooser = new SystemFileChooser();
        chooser.setDialogTitle(title);
        chooser.setFileSelectionMode(SystemFileChooser.DIRECTORIES_ONLY);
        chooser.setApproveButtonText("Select folder");
        File lastFile = PreferencesHandler.getLastFile();
        if (lastFile != null)
            chooser.setCurrentDirectory(lastFile.isDirectory() ? lastFile : lastFile.getParentFile());
        if (chooser.showOpenDialog(this) != SystemFileChooser.APPROVE_OPTION || chooser.getSelectedFile() == null)
            return null;
        File folder = chooser.getSelectedFile();
        PreferencesHandler.setLastPath(folder.getAbsolutePath());
        return folder;
    }

    /**
     * Download the whole files of the selected assets, after checking and confirming the size.
     */
    private void downloadSelected() {
        List<DownloadRow> rows = getSelectedDownloads();
        if (rows.isEmpty())
            return;
        List<DownloadRow> unknown = rows.stream().filter(r -> r.bytes < 0).collect(Collectors.toList());
        runTask("Checking the size of " + unknown.size() + " assets", () -> checkRows(unknown), ok -> {
            downloadsModel.fireTableDataChanged();
            long total = rows.stream().filter(r -> r.bytes >= 0).mapToLong(r -> r.bytes).sum();
            long unknownCount = rows.stream().filter(r -> r.bytes < 0).count();
            List<String> lines = new ArrayList<>();
            for( DownloadRow r : rows ) {
                lines.add(String.format("%-12s %s / %s   [%s]%s", r.bytes >= 0 ? humanSize(r.bytes) : "?", r.itemRow.item.getId(),
                        r.asset.getId(), r.access, stylesInfo(r)));
            }
            String summary = rows.size() + " whole files, total " + humanSize(total)
                    + (unknownCount > 0 ? " + " + unknownCount + " of unknown size (see status)" : "")
                    + ".\nThe whole files are downloaded, independently of the query bbox.";
            StyleChooser styleChooser = new StyleChooser(rows);
            if (!confirmWithList("Download whole files", summary, lines, styleChooser.getPanel()))
                return;
            File folder = askOutputFolder("Folder to download the assets into");
            if (folder != null)
                startDownload(rows, folder, styleChooser.getChosenKeys());
        });
    }

    private void startDownload( List<DownloadRow> rows, File folder, Set<String> styleKeys ) {
        runTask("Downloading " + rows.size() + " assets to " + folder, () -> {
            HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL)
                    .connectTimeout(Duration.ofSeconds(15)).build();
            int ok = 0;
            for( DownloadRow row : rows ) {
                if (monitor.isCanceled())
                    break;
                File outFile = new File(folder, safeFileName(row.itemRow.item.getId() + "_" + hrefFileName(row)));
                boolean viaHandler = row.asset.isValid() && row.asset.getHandler() != null;
                log("Downloading " + row.href + " -> " + outFile.getName() + (viaHandler ? " (HM handler)" : " (plain http)"));
                try {
                    if (viaHandler) {
                        // the HM way, to see how the handlers behave
                        row.asset.getHandler().downloadAsset(outFile.getAbsolutePath(), monitor);
                    } else {
                        downloadPlain(client, row.href, outFile, access);
                    }
                    row.access = "saved" + saveStyles(row, outFile, styleKeys);
                    row.size = humanSize(outFile.length());
                    ok++;
                } catch (Exception e) {
                    outFile.delete();
                    row.access = "download failed: " + e.getMessage();
                    logException("Download of " + row.href + " failed", e);
                }
                SwingUtilities.invokeLater(() -> downloadsModel.fireTableDataChanged());
            }
            return ok;
        }, ok -> {
            downloadsModel.fireTableDataChanged();
            String msg = ok + " of " + rows.size() + " assets downloaded to " + folder;
            log(msg);
            statusLabel.setText(msg);
        });
    }

    /** The grid a clipped raster export would produce. */
    private static class ClipPlan {
        DownloadRow row;
        Integer epsg;
        RegionMap region;
        String skipReason;
    }

    /**
     * Read the selected raster assets through HM, clipped on the query bbox, and write them as GeoTIFFs.
     */
    private void exportClippedSelected() {
        List<DownloadRow> rows = getSelectedDownloads().stream().filter(r -> isRasterAsset(r.asset)).collect(Collectors.toList());
        Envelope bbox = readBboxFields();
        if (rows.isEmpty() || bbox == null)
            return;

        // default to the native resolution, if the catalog declares it
        double nativeRes = rows.stream().mapToDouble(r -> ((IHMStacAssetRasterHandler) r.asset.getHandler()).getResolution())
                .filter(r -> r > 0).findFirst().orElse(10.0);
        String resString = GuiUtilities.showInputDialog(this,
                "Resolution of the exported rasters, in the units of each asset's CRS (e.g. meters for UTM):",
                String.valueOf(nativeRes));
        if (resString == null)
            return;
        double resolution;
        try {
            resolution = Double.parseDouble(resString.trim());
            if (resolution <= 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            GuiUtilities.showWarningMessage(this, "Invalid resolution: " + resString);
            return;
        }

        // compute the grids to show the size before reading anything
        List<ClipPlan> plans = new ArrayList<>();
        List<String> lines = new ArrayList<>();
        long totalCells = 0;
        for( DownloadRow row : rows ) {
            ClipPlan plan = planClip(row, bbox, resolution);
            plans.add(plan);
            String name = row.itemRow.item.getId() + " / " + row.asset.getId();
            if (plan.skipReason != null) {
                lines.add(String.format("%-26s %s", "skipped", name + "   [" + plan.skipReason + "]"));
            } else {
                long cells = (long) plan.region.getCols() * plan.region.getRows();
                totalCells += cells;
                lines.add(String.format("%-26s %s", plan.region.getCols() + " cols x " + plan.region.getRows() + " rows",
                        name + "   [EPSG:" + plan.epsg + ", " + humanCount(cells) + " cells]" + stylesInfo(row)));
            }
        }
        List<ClipPlan> toExport = plans.stream().filter(p -> p.skipReason == null).collect(Collectors.toList());
        String summary = toExport.size() + " clipped rasters at resolution " + resolution + ", total " + humanCount(totalCells)
                + " cells" + (toExport.size() < plans.size() ? ", " + (plans.size() - toExport.size()) + " skipped" : "")
                + ".\nOnly the parts of the files covering the bbox are read.";
        StyleChooser styleChooser = new StyleChooser(toExport.stream().map(p -> p.row).collect(Collectors.toList()));
        if (!confirmWithList("Export clipped rasters", summary, lines, styleChooser.getPanel()) || toExport.isEmpty())
            return;
        Set<String> styleKeys = styleChooser.getChosenKeys();
        File folder = askOutputFolder("Folder to export the clipped rasters into");
        if (folder == null)
            return;

        runTask("Exporting " + toExport.size() + " rasters clipped to " + bboxString(bbox), () -> {
            int ok = 0;
            for( ClipPlan plan : toExport ) {
                if (monitor.isCanceled())
                    break;
                DownloadRow row = plan.row;
                File outFile = new File(folder, safeFileName(row.itemRow.item.getId() + "_" + row.asset.getId() + "_clip.tif"));
                try {
                    row.access = exportClipped(plan, outFile) + saveStyles(row, outFile, styleKeys);
                    row.size = humanSize(outFile.length());
                    ok++;
                } catch (Exception e) {
                    outFile.delete();
                    row.access = "export failed: " + e.getMessage();
                    logException("Clipped export of " + row.href + " failed", e);
                }
                SwingUtilities.invokeLater(() -> downloadsModel.fireTableDataChanged());
            }
            return ok;
        }, ok -> {
            downloadsModel.fireTableDataChanged();
            String msg = ok + " of " + toExport.size() + " clipped rasters exported to " + folder;
            log(msg);
            statusLabel.setText(msg);
        });
    }

    /**
     * Compute the grid of a raster asset clipped to the bbox, in the asset's CRS.
     */
    private static ClipPlan planClip( DownloadRow row, Envelope bboxLL, double resolution ) {
        ClipPlan plan = new ClipPlan();
        plan.row = row;
        plan.epsg = row.asset.getEpsg() != null ? row.asset.getEpsg() : row.itemRow.epsg;
        if (plan.epsg == null) {
            plan.skipReason = "no EPSG for the asset";
            return plan;
        }
        try {
            CoordinateReferenceSystem assetCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:" + plan.epsg, true);

            // bbox in the asset crs, limited to the data of the item
            ReferencedEnvelope bboxAsset = new ReferencedEnvelope(bboxLL, DefaultGeographicCRS.WGS84).transform(assetCrs, true);
            Envelope readEnv = new Envelope(bboxAsset);
            if (row.itemRow.footprint != null) {
                ReferencedEnvelope footprintAsset = new ReferencedEnvelope(row.itemRow.footprint.getEnvelopeInternal(),
                        DefaultGeographicCRS.WGS84).transform(assetCrs, true);
                readEnv = readEnv.intersection(footprintAsset);
            }
            if (readEnv.isNull() || readEnv.getWidth() <= 0 || readEnv.getHeight() <= 0) {
                plan.skipReason = "outside bbox";
                return plan;
            }
            int cols = (int) Math.ceil(readEnv.getWidth() / resolution);
            int rows = (int) Math.ceil(readEnv.getHeight() / resolution);
            if ((long) cols * rows > MAX_EXPORT_CELLS) {
                plan.skipReason = cols + " cols x " + rows + " rows, over the limit of " + humanCount(MAX_EXPORT_CELLS) + " cells";
                return plan;
            }
            plan.region = RegionMap.fromBoundsAndGrid(readEnv.getMinX(), readEnv.getMinX() + cols * resolution,
                    readEnv.getMaxY() - rows * resolution, readEnv.getMaxY(), cols, rows);
        } catch (Exception e) {
            plan.skipReason = "unable to compute the region: " + e.getMessage();
        }
        return plan;
    }

    /**
     * Read a planned raster through HM and write it.
     * 
     * @return the status to show.
     */
    private String exportClipped( ClipPlan plan, File outFile ) throws Exception {
        DownloadRow row = plan.row;
        log("Reading " + row.asset.getId() + " of " + row.itemRow.item.getId() + " on EPSG:" + plan.epsg + " region "
                + plan.region.getCols() + "x" + plan.region.getRows() + " cells");
        IHMStacAssetRasterHandler handler = (IHMStacAssetRasterHandler) row.asset.getHandler();
        // the reader returns the whole file lazily: mapping it on the region, as done in
        // HMStacCollection.readRasterBandOnRegion, fetches only the needed parts
        GridCoverage2D coverage = handler.readRaster(plan.region);
        try {
            HMRaster outRaster = new HMRasterWritableBuilder().setName(row.asset.getId()).setRegion(plan.region)
                    .setCrs(coverage.getCoordinateReferenceSystem()).setNoValue(handler.getNoValue()).build();
            outRaster.mapRaster(null, HMRaster.fromGridCoverage(coverage), MergeMode.SUBSTITUTE);
            OmsRasterWriter.writeRaster(outFile.getAbsolutePath(), outRaster);
            log("Written " + outFile.getName() + " with " + outRaster.getCols() + " cols x " + outRaster.getRows() + " rows");
            return "clipped " + outRaster.getCols() + " x " + outRaster.getRows();
        } finally {
            coverage.dispose(true);
        }
    }

    private void showOnMap( DownloadRow row ) {
        if (isRasterAsset(row.asset))
            showRasterOnMap(row);
        else if (isVectorAsset(row.asset))
            showVectorOnMap(row);
    }

    /**
     * Read a raster asset on the part of its item visible in the map, at about the screen resolution
     * (never finer than the native one), and draw it over the map.
     */
    private void showRasterOnMap( DownloadRow row ) {
        Map<Integer, Integer> classColors = readClassColors(row.asset.getAssetNode());
        Envelope area = mapPanel.getViewEnvelope();
        if (row.itemRow.footprint != null)
            area = area.intersection(row.itemRow.footprint.getEnvelopeInternal());
        if (area.isNull() || area.getWidth() <= 0 || area.getHeight() <= 0) {
            GuiUtilities.showWarningMessage(this, "The item of the asset is not in the map view, zoom to it first.");
            return;
        }
        Integer epsg = row.asset.getEpsg() != null ? row.asset.getEpsg() : row.itemRow.epsg;
        if (epsg == null) {
            GuiUtilities.showWarningMessage(this, "The asset has no EPSG, it can't be placed on the map.");
            return;
        }
        ClipPlan plan;
        try {
            CoordinateReferenceSystem assetCrs = HMCrsRegistry.INSTANCE.getCrs("EPSG:" + epsg, true);
            ReferencedEnvelope areaAsset = new ReferencedEnvelope(area, DefaultGeographicCRS.WGS84).transform(assetCrs, true);
            int[] pixels = mapPanel.getPixelSize(area);
            double resolution = Math.max(areaAsset.getWidth() / pixels[0], areaAsset.getHeight() / pixels[1]);
            double nativeRes = ((IHMStacAssetRasterHandler) row.asset.getHandler()).getResolution();
            if (nativeRes > 0)
                resolution = Math.max(resolution, nativeRes);
            plan = planClip(row, area, resolution);
        } catch (Exception e) {
            logException("Unable to compute the map region of " + row.href, e);
            GuiUtilities.showWarningMessage(this, "Unable to compute the region to read: " + e.getMessage());
            return;
        }
        if (plan.skipReason != null) {
            GuiUtilities.showWarningMessage(this, "Unable to show the asset: " + plan.skipReason);
            return;
        }

        String name = row.itemRow.item.getId() + " / " + row.asset.getId();
        runTask("Reading " + name + " for the map", () -> {
            log("Reading " + row.asset.getId() + " of " + row.itemRow.item.getId() + " for the map on EPSG:" + plan.epsg
                    + " region " + plan.region.getCols() + "x" + plan.region.getRows() + " cells");
            IHMStacAssetRasterHandler handler = (IHMStacAssetRasterHandler) row.asset.getHandler();
            GridCoverage2D coverage = handler.readRaster(plan.region);
            try {
                if (isRgb(coverage))
                    return RasterPreview.create(readRgbOnRegion(coverage, plan.region, handler.getNoValue()));
                try (HMRaster raster = new HMRasterWritableBuilder().setName(row.asset.getId()).setRegion(plan.region)
                        .setCrs(coverage.getCoordinateReferenceSystem()).setNoValue(handler.getNoValue()).build()) {
                    // as for the clipped export, mapping on the region fetches only the needed parts
                    raster.mapRaster(null, HMRaster.fromGridCoverage(coverage), MergeMode.SUBSTITUTE);
                    return RasterPreview.create(raster.buildCoverage(), classColors);
                }
            } finally {
                coverage.dispose(true);
            }
        }, preview -> {
            if (preview.wgs84Image == null) {
                GuiUtilities.showWarningMessage(this, "Unable to show the asset: " + preview.note);
                return;
            }
            mapPanel.addImageLayer(mapLayerId(row), name, preview.wgs84Image, preview.wgs84Envelope, false);
            String msg = "Shown on map: " + name + (preview.range != null
                    ? String.format(Locale.ROOT, " (stretched %.4g - %.4g)", preview.range[0], preview.range[1])
                    : classColors != null ? " (" + classColors.size() + " classes, other values transparent)" : "");
            log(msg);
            statusLabel.setText(msg);
        });
    }

    /**
     * Read the classes of categorical data, declared with the classification extension on the asset or
     * on its first band.
     *
     * @return the argb color of each class value, null if no classes are declared. Classes without a
     *          color hint get a generated color, the ones flagged as nodata are left out.
     */
    static Map<Integer, Integer> readClassColors( JsonNode assetNode ) {
        JsonNode classes = assetNode.get("classification:classes");
        for( String bandsField : new String[]{"bands", "raster:bands"} ) {
            JsonNode bands = assetNode.get(bandsField);
            if (classes == null && bands != null && bands.isArray() && bands.size() > 0)
                classes = bands.get(0).get("classification:classes");
        }
        if (classes == null || !classes.isArray())
            return null;
        Map<Integer, Integer> colors = new LinkedHashMap<>();
        for( JsonNode c : classes ) {
            JsonNode value = c.get("value");
            if (value == null || !value.canConvertToInt() || c.path("nodata").asBoolean(false))
                continue;
            String hint = c.path("color_hint").asText("").trim();
            Integer color = null;
            if (hint.matches("#?[0-9a-fA-F]{6}"))
                color = 0xff000000 | Integer.parseInt(hint.replace("#", ""), 16);
            if (color == null) // golden ratio hues, well apart from each other
                color = Color.HSBtoRGB((colors.size() * 0.618034f) % 1f, 0.65f, 0.9f);
            colors.put(value.intValue(), color);
        }
        return colors.isEmpty() ? null : colors;
    }

    /**
     * @return true for 8 bit images with at least three bands, shown in true colors.
     */
    private static boolean isRgb( GridCoverage2D coverage ) {
        SampleModel sampleModel = coverage.getRenderedImage().getSampleModel();
        return sampleModel.getNumBands() >= 3 && sampleModel.getDataType() == DataBuffer.TYPE_BYTE;
    }

    /**
     * Map the color bands (and the alpha, if any) of an 8 bit image on the region, one at a time to fetch
     * only the needed parts, and compose them into a rgba coverage. The cells that are novalue in all the
     * color bands are transparent.
     */
    static GridCoverage2D readRgbOnRegion( GridCoverage2D coverage, RegionMap region, double noValue ) throws Exception {
        int bandsCount = Math.min(4, coverage.getRenderedImage().getSampleModel().getNumBands());
        // the novalue of the file, in case the catalog declares none
        Double fileNoValue = CoverageUtilities.getNovalue(coverage);
        CoordinateReferenceSystem crs = coverage.getCoordinateReferenceSystem();
        List<HMRaster> bands = new ArrayList<>();
        try {
            for( int b = 0; b < bandsCount; b++ ) {
                HMRaster band = new HMRasterWritableBuilder().setName("band" + b).setRegion(region).setCrs(crs)
                        .setNoValue(noValue).build();
                bands.add(band);
                band.mapRaster(null, HMRaster.fromGridCoverage(HMRaster.extractBand(coverage, b)), MergeMode.SUBSTITUTE);
            }
            int cols = region.getCols();
            int rows = region.getRows();
            BufferedImage image = new BufferedImage(cols, rows, BufferedImage.TYPE_4BYTE_ABGR);
            WritableRaster out = image.getRaster();
            int[] rgba = new int[4];
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    boolean empty = true;
                    for( int b = 0; b < 3; b++ ) {
                        double v = bands.get(b).getValue(c, r);
                        if (!bands.get(b).isNovalue(v) && !(fileNoValue != null && v == fileNoValue))
                            empty = false;
                        rgba[b] = toByte(v);
                    }
                    rgba[3] = empty ? 0 : bandsCount == 4 ? toByte(bands.get(3).getValue(c, r)) : 255;
                    if (rgba[3] != 0)
                        out.setPixel(c, r, rgba);
                }
            }
            ReferencedEnvelope envelope = new ReferencedEnvelope(region.getWest(), region.getEast(), region.getSouth(),
                    region.getNorth(), crs);
            return CoverageFactoryFinder.getGridCoverageFactory(null).create("rgb", image, envelope);
        } finally {
            for( HMRaster band : bands ) {
                band.close();
            }
        }
    }

    private static int toByte( double value ) {
        return Double.isNaN(value) ? 0 : (int) Math.max(0, Math.min(255, Math.round(value)));
    }

    /**
     * Read a vector asset (the whole file, vector formats can't be read partially) and draw its
     * geometries over the map.
     */
    private void showVectorOnMap( DownloadRow row ) {
        if (row.bytes > MAX_MAP_VECTOR_BYTES && !GuiUtilities.showYesNoDialog(this, "The file is " + humanSize(row.bytes)
                + " and needs to be downloaded completely to be shown. Continue?")) {
            return;
        }
        String name = row.itemRow.item.getId() + " / " + row.asset.getId();
        runTask("Reading " + name + " for the map", () -> {
            log("Reading " + row.href + " for the map");
            SimpleFeatureCollection fc = row.asset.getHandler().read(SimpleFeatureCollection.class, monitor);
            if (fc == null)
                throw new IllegalArgumentException("the asset could not be read as vector data.");
            CoordinateReferenceSystem crs = fc.getSchema().getCoordinateReferenceSystem();
            MathTransform toLonLat = null;
            if (crs != null && !HMCrsRegistry.crsEquals(crs, DefaultGeographicCRS.WGS84))
                toLonLat = CRS.findMathTransform(crs, DefaultGeographicCRS.WGS84, true);
            List<Geometry> geometries = new ArrayList<>();
            int total = 0;
            try (SimpleFeatureIterator it = fc.features()) {
                while( it.hasNext() ) {
                    Object geometry = it.next().getDefaultGeometry();
                    if (!(geometry instanceof Geometry g) || g.isEmpty())
                        continue;
                    total++;
                    if (geometries.size() < MAX_MAP_FEATURES)
                        geometries.add(toLonLat != null ? JTS.transform(g, toLonLat) : g);
                }
            }
            String note = total > geometries.size() ? ", only the first " + geometries.size() + " of " + total + " shown" : "";
            log("Read " + total + " geometries of " + name + (crs == null ? " (no CRS, assumed lon/lat)" : "") + note);
            return new Object[]{geometries, total + " geometries" + note};
        }, result -> {
            @SuppressWarnings("unchecked")
            List<Geometry> geometries = (List<Geometry>) result[0];
            if (geometries.isEmpty()) {
                GuiUtilities.showWarningMessage(this, "The asset contains no geometries.");
                return;
            }
            mapPanel.addVectorLayer(mapLayerId(row), name, geometries);
            Envelope env = new Envelope();
            geometries.forEach(g -> env.expandToInclude(g.getEnvelopeInternal()));
            if (!mapPanel.getViewEnvelope().intersects(env))
                mapPanel.zoomToEnvelope(env);
            String msg = "Shown on map: " + name + " (" + result[1] + ")";
            log(msg);
            statusLabel.setText(msg);
        });
    }

    /**
     * Download a csv asset and show it in a table.
     */
    private void viewCsv( DownloadRow row ) {
        String name = row.itemRow.item.getId() + " / " + row.asset.getId();
        runTask("Reading " + name, () -> {
            log("Downloading " + row.href + " to view it");
            File csvFile = row.asset.getHandler().read(File.class, monitor);
            if (csvFile == null)
                throw new IllegalArgumentException("no csv file found in the asset.");
            return csvFile;
        }, csvFile -> {
            try {
                CsvViewer.show(csvFile, name);
                statusLabel.setText("Opened " + name);
            } catch (Exception e) {
                logException("Unable to show " + csvFile, e);
                GuiUtilities.showWarningMessage(this, "Unable to read the csv: " + e.getMessage());
            }
        });
    }

    /**
     * @return the id of the map layer showing the asset of a row.
     */
    private static String mapLayerId( DownloadRow row ) {
        return row.itemRow.item.getId() + "/" + row.asset.getId();
    }

    private static String humanCount( long count ) {
        if (count < 1_000)
            return String.valueOf(count);
        if (count < 1_000_000)
            return String.format("%.1fK", count / 1_000.0);
        if (count < 1_000_000_000)
            return String.format("%.1fM", count / 1_000_000.0);
        return String.format("%.1fG", count / 1_000_000_000.0);
    }

    /**
     * Find the style assets (qml/sld) of the item that belong to a raster asset: for each format, those sharing 
     * the longest name prefix with the raster. More of them are alternatives (e.g. different legend levels).
     */
    private static List<HMStacAsset> findStyles( ItemRow itemRow, HMStacAsset raster ) {
        String rasterFile = fileName(href(raster));
        Map<String, List<HMStacAsset>> bestByFormat = new LinkedHashMap<>();
        Map<String, Integer> bestScore = new LinkedHashMap<>();
        for( HMStacAsset asset : itemRow.assets ) {
            if (!(asset.getHandler() instanceof StyleFileHandler))
                continue;
            String extension = styleExtension(asset);
            int score = Math.max(commonPrefix(asset.getId(), raster.getId()), commonPrefix(fileName(href(asset)), rasterFile));
            Integer best = bestScore.get(extension);
            if (best == null || score > best) {
                bestScore.put(extension, score);
                bestByFormat.put(extension, new ArrayList<>(List.of(asset)));
            } else if (score == best) {
                bestByFormat.get(extension).add(asset);
            }
        }
        return bestByFormat.values().stream().flatMap(List::stream).collect(Collectors.toList());
    }

    private static String styleExtension( HMStacAsset styleAsset ) {
        return ((StyleFileHandler) styleAsset.getHandler()).getFormat().getExtension();
    }

    /**
     * @return the info about the styles available for a raster, for the confirmation dialogs.
     */
    private static String stylesInfo( DownloadRow row ) {
        if (!isRasterAsset(row.asset))
            return "";
        List<HMStacAsset> styles = findStyles(row.itemRow, row.asset);
        return styles.isEmpty() ? "" : " + " + styles.size() + " style(s) available";
    }

    /**
     * Lets the user choose which of the available style files to save with the rasters.
     */
    private static class StyleChooser {
        private final Map<String, JCheckBox> checks = new LinkedHashMap<>();
        private JPanel panel;

        StyleChooser( List<DownloadRow> rows ) {
            // the candidates by asset key, which is the same in all the items of a collection
            Map<String, String> descriptions = new LinkedHashMap<>();
            Map<String, Integer> counts = new LinkedHashMap<>();
            for( DownloadRow row : rows ) {
                if (!isRasterAsset(row.asset))
                    continue;
                for( HMStacAsset style : findStyles(row.itemRow, row.asset) ) {
                    counts.merge(style.getId(), 1, Integer::sum);
                    descriptions.putIfAbsent(style.getId(), styleExtension(style) + "   " + nn(fileName(href(style))));
                }
            }
            if (descriptions.isEmpty())
                return;
            panel = new JPanel();
            panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
            panel.setBorder(BorderFactory.createTitledBorder("Style files to save with the rasters"));
            panel.add(new JLabel("<html><i>The first checked style of each format gets the raster's name "
                    + "(QGIS loads a same named qml automatically), the others get a suffix.</i></html>"));
            descriptions.forEach(( key, description ) -> {
                JCheckBox check = new JCheckBox(key + "  (" + description + ", in " + counts.get(key) + " rasters)", true);
                checks.put(key, check);
                panel.add(check);
            });
        }

        /** @return the panel or null if there are no styles to choose. */
        JComponent getPanel() {
            return panel;
        }

        Set<String> getChosenKeys() {
            return checks.entrySet().stream().filter(e -> e.getValue().isSelected()).map(Map.Entry::getKey)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
        }
    }

    /**
     * Save the chosen styles of a raster next to it. The first of each format gets the same base name 
     * of the raster, which makes QGIS load the qml automatically, the others a suffix from their file name.
     * 
     * @return the info to add to the status.
     */
    private String saveStyles( DownloadRow row, File rasterFile, Set<String> styleKeys ) {
        if (!isRasterAsset(row.asset) || styleKeys.isEmpty())
            return "";
        String name = rasterFile.getName();
        int dot = name.lastIndexOf('.');
        String baseName = dot > 0 ? name.substring(0, dot) : name;
        String rasterHrefFile = fileName(href(row.asset));

        // keep the order in which the user saw them
        List<HMStacAsset> styles = findStyles(row.itemRow, row.asset).stream().filter(s -> styleKeys.contains(s.getId()))
                .sorted(Comparator.comparingInt(s -> new ArrayList<>(styleKeys).indexOf(s.getId()))).collect(Collectors.toList());
        Set<String> formatsDone = new LinkedHashSet<>();
        List<String> saved = new ArrayList<>();
        for( HMStacAsset style : styles ) {
            String extension = styleExtension(style);
            String suffix = "";
            if (!formatsDone.add(extension)) {
                // an alternative style: name it after the part of its file name differing from the raster
                String styleFile = nn(fileName(href(style)));
                int extIndex = styleFile.toLowerCase().lastIndexOf("." + extension);
                String styleName = extIndex > 0 ? styleFile.substring(0, extIndex) : styleFile;
                String differing = styleName.substring(Math.min(styleName.length(), commonPrefix(styleName, nn(rasterHrefFile))));
                suffix = "_" + safeFileName(differing.isEmpty() ? style.getId() : differing);
            }
            File styleFile = new File(rasterFile.getParentFile(), baseName + suffix + "." + extension);
            try {
                style.getHandler().downloadAsset(styleFile.getAbsolutePath(), monitor);
                saved.add(styleFile.getName().substring(baseName.length()));
                log("Saved style " + style.getId() + " as " + styleFile.getName());
            } catch (Exception e) {
                styleFile.delete();
                logException("Unable to download style " + style.getId(), e);
            }
        }
        return saved.isEmpty() ? "" : " + " + String.join(", ", saved);
    }

    private static int commonPrefix( String a, String b ) {
        if (a == null || b == null)
            return 0;
        int n = Math.min(a.length(), b.length());
        int i = 0;
        while( i < n && a.charAt(i) == b.charAt(i) )
            i++;
        return i;
    }

    private static String fileName( String href ) {
        if (href == null)
            return null;
        int queryIndex = href.indexOf('?');
        String path = queryIndex > 0 ? href.substring(0, queryIndex) : href;
        return path.substring(path.lastIndexOf('/') + 1);
    }

    private static boolean isRasterAsset( HMStacAsset asset ) {
        return asset.isValid() && asset.getHandler() instanceof IHMStacAssetRasterHandler;
    }

    /**
     * @return true for the assets HM reads as features (geopackage, geojson, shapefile).
     */
    private static boolean isVectorAsset( HMStacAsset asset ) {
        IHMStacAssetHandler handler = asset.isValid() ? asset.getHandler() : null;
        return handler instanceof GeopackageVectorHandler || handler instanceof GeojsonHandler
                || handler instanceof ShapefileHandler;
    }

    private static boolean isCsvAsset( HMStacAsset asset ) {
        return asset.isValid() && asset.getHandler() instanceof CsvfileHandler;
    }

    /**
     * @return the file name part of the asset href, or the asset id if not available.
     */
    private static String hrefFileName( DownloadRow row ) {
        String name = null;
        if (row.href != null) {
            String path = row.href;
            int queryIndex = path.indexOf('?');
            if (queryIndex > 0)
                path = path.substring(0, queryIndex);
            name = path.substring(path.lastIndexOf('/') + 1);
        }
        return name == null || name.isBlank() ? row.asset.getId() : name;
    }

    /**
     * Names use the item id, since many catalogs reuse file names like B02.tif in each item.
     */
    private static String safeFileName( String name ) {
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }

    private static void downloadPlain( HttpClient client, String href, File outFile, HMStacAccess access ) throws Exception {
        if (href == null)
            throw new IllegalArgumentException("the asset has no href");
        if (access != null && access.isAuthenticated(href)) {
            try (HMStacResponse response = access.get(href)) {
                Files.copy(response.getInputStream(), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
            return;
        }
        if (href.startsWith("http://") || href.startsWith("https://")) {
            HttpResponse<Path> response = client.send(HttpRequest.newBuilder(URI.create(href)).timeout(Duration.ofMinutes(10)).GET().build(),
                    HttpResponse.BodyHandlers.ofFile(outFile.toPath()));
            if (response.statusCode() >= 400)
                throw new IOException("HTTP " + response.statusCode());
        } else if (href.startsWith("file:")) {
            Files.copy(Paths.get(URI.create(href)), outFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            throw new IllegalArgumentException("unsupported protocol: " + href.substring(0, Math.max(0, href.indexOf(':'))));
        }
    }

    private void exportCsv() {
        List<DownloadRow> rows = downloadsModel.getRows();
        if (rows.isEmpty()) {
            GuiUtilities.showWarningMessage(this, "No assets to export.");
            return;
        }
        File file = GuiUtilities.showSaveFileDialog(this, "Export download preview", PreferencesHandler.getLastFile());
        if (file == null)
            return;
        StringBuilder sb = new StringBuilder("item_id;datetime;asset_key;type;hm_handler;epsg;access;size;href\n");
        for( DownloadRow r : rows ) {
            Integer epsg = r.asset.getEpsg() != null ? r.asset.getEpsg() : r.itemRow.epsg;
            sb.append(String.join(";", r.itemRow.item.getId(), nn(r.itemRow.datetime), r.asset.getId(), nn(r.asset.getType()),
                    handlerName(r.asset), epsg == null ? "" : epsg.toString(), r.access, r.size, nn(r.href))).append("\n");
        }
        try {
            Files.writeString(file.toPath(), sb.toString(), StandardCharsets.UTF_8);
            PreferencesHandler.setLastPath(file.getAbsolutePath());
            log("Exported " + rows.size() + " assets to " + file);
        } catch (Exception e) {
            logException("Unable to export", e);
            GuiUtilities.showErrorMessage(this, "Unable to export: " + e.getMessage());
        }
    }

    private void zoomToCollection() {
        Envelope env = getCollectionEnvelope(currentCollection);
        if (env != null)
            mapPanel.zoomToEnvelope(env);
    }

    private void zoomToResults() {
        Envelope env = new Envelope();
        for( ItemRow row : itemRows ) {
            if (row.footprint != null)
                env.expandToInclude(row.footprint.getEnvelopeInternal());
        }
        if (!env.isNull())
            mapPanel.zoomToEnvelope(env);
    }

    private void setTimeFromCollection() {
        if (currentCollection == null)
            return;
        try {
            List<Date> bounds = currentCollection.getTemporalBounds();
            Date start = bounds.size() > 0 ? bounds.get(0) : null;
            Date end = bounds.size() > 1 ? bounds.get(bounds.size() - 1) : null;
            fromField.setText(start != null ? formatDate(start) : "");
            toField.setText(formatDate(end != null ? end : new Date()));
            timeCheck.setSelected(true);
        } catch (Exception e) {
            GuiUtilities.showWarningMessage(this, "The collection has no usable temporal extent.");
        }
    }

    // ==================== info panes ====================

    private void showServiceInfo( String url, String conformance, int collectionsCount, long landingMillis, long collectionsMillis,
            String authInfo, boolean searchAvailable ) {
        StringBuilder sb = new StringBuilder("<html><body style='font-family:sans-serif; padding:6px'>");
        sb.append("<h2>").append(escape(url)).append("</h2>");
        sb.append("<table cellpadding='3'>");
        row(sb, "Collections", String.valueOf(collectionsCount));
        row(sb, "Landing page read in", landingMillis + " ms");
        row(sb, "Collections listed in", collectionsMillis + " ms");
        row(sb, "Authentication", escape(authInfo));
        row(sb, "Item search", searchAvailable ? "<font color='#1a8a3a'>yes</font>"
                : "<font color='#c0392b'>no</font> (static catalog: items are collected following the links and filtered locally)");
        sb.append("</table><h3>Conformance</h3><table cellpadding='3'>");
        for( String line : conformance.split("\n") ) {
            int colon = line.lastIndexOf(':');
            if (colon > 0) {
                String value = line.substring(colon + 1).trim();
                String shown = "true".equals(value) ? "<font color='#1a8a3a'>yes</font>"
                        : "false".equals(value) ? "<font color='#c0392b'>no</font>" : escape(value);
                row(sb, line.substring(0, colon), shown);
            } else {
                sb.append("<tr><td colspan='2'>").append(escape(line)).append("</td></tr>");
            }
        }
        sb.append("</table><p><i>Item search is needed to run queries, filter to use CQL expressions.</i></p>");
        sb.append("</body></html>");
        serviceInfoPane.setText(sb.toString());
        serviceInfoPane.setCaretPosition(0);
    }

    private String collectionHtml( HMStacCollection c ) {
        Map<String, Object> other = c.getOtherFields() != null ? c.getOtherFields() : Map.of();
        StringBuilder sb = new StringBuilder("<html><body style='font-family:sans-serif; padding:6px'>");
        sb.append("<h2>").append(escape(c.getTitle() != null ? c.getTitle() : c.getId())).append("</h2>");
        sb.append("<table cellpadding='3'>");
        row(sb, "Id", escape(c.getId()));
        row(sb, "Type", escape(c.getType()));
        if (other.get("license") != null)
            row(sb, "License", escape(String.valueOf(other.get("license"))));
        Envelope env = getCollectionEnvelope(c);
        row(sb, "Spatial extent", env != null ? escape(bboxString(env)) : "<i>not available</i>");
        String temporal;
        try {
            List<Date> bounds = c.getTemporalBounds();
            Date start = bounds.size() > 0 ? bounds.get(0) : null;
            Date end = bounds.size() > 1 ? bounds.get(bounds.size() - 1) : null;
            temporal = (start != null ? formatDate(start) : "..") + " / " + (end != null ? formatDate(end) : "ongoing");
        } catch (Exception e) {
            temporal = "<i>not available</i>";
        }
        row(sb, "Temporal extent", temporal);
        if (other.get("keywords") instanceof List< ? > keywords)
            row(sb, "Keywords", escape(keywords.stream().map(String::valueOf).collect(Collectors.joining(", "))));
        if (other.get("providers") instanceof List< ? > providers) {
            String names = providers.stream().map(p -> p instanceof Map< ? , ? > m ? String.valueOf(m.get("name")) : String.valueOf(p))
                    .collect(Collectors.joining(", "));
            row(sb, "Providers", escape(names));
        }
        sb.append("</table>");

        // the item_assets extension tells in advance which assets the items will have
        if (other.get("item_assets") instanceof Map< ? , ? > itemAssets && !itemAssets.isEmpty()) {
            sb.append("<h3>Item assets (declared)</h3><table cellpadding='3' border='0'>");
            sb.append("<tr><th align='left'>Key</th><th align='left'>Type</th><th align='left'>HM handler</th></tr>");
            for( Map.Entry< ? , ? > e : itemAssets.entrySet() ) {
                String key = String.valueOf(e.getKey());
                String type = "";
                String handler = "";
                try {
                    ObjectNode node = MAPPER.valueToTree(e.getValue());
                    if (!node.has("href"))
                        node.put("href", ""); // declared assets have no href, handlers need one
                    HMStacAsset asset = new HMStacAsset(key, node);
                    type = asset.getType() != null ? asset.getType() : "";
                    handler = asset.isValid()
                            ? "<font color='#1a8a3a'>" + handlerName(asset) + "</font>"
                            : "<font color='#c0392b'>" + UNSUPPORTED + "</font>";
                } catch (Exception ex) {
                    handler = "?";
                }
                sb.append("<tr><td>").append(escape(key)).append("</td><td>").append(escape(type)).append("</td><td>").append(handler)
                        .append("</td></tr>");
            }
            sb.append("</table>");
        } else {
            sb.append("<p><i>No item_assets declared: run a search to see the assets.</i></p>");
        }

        if (c.getDescription() != null) {
            sb.append("<h3>Description</h3><p>").append(escape(c.getDescription()).replace("\n", "<br>")).append("</p>");
        }
        sb.append("</body></html>");
        return sb.toString();
    }

    // ==================== task handling ====================

    private <T> void runTask( String name, Callable<T> task, Consumer<T> onSuccess ) {
        if (busy)
            return;
        monitor.setCanceled(false);
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
                    GuiUtilities.showErrorMessage(StacBrowser.this,
                            name + " failed:\n" + cause.getClass().getSimpleName() + ": " + cause.getMessage()
                                    + "\n\nSee the Log tab for details.");
                } catch (Exception e) {
                    logException(name + " failed", e);
                }
            }
        }.execute();
    }

    private void setBusy( boolean busy, String message ) {
        this.busy = busy;
        connectButton.setEnabled(!busy);
        searchButton.setEnabled(!busy);
        checkAccessButton.setEnabled(!busy);
        cancelButton.setEnabled(busy);
        progressBar.setIndeterminate(busy);
        statusLabel.setText(message);
    }

    private void log( String message ) {
        String line = new SimpleDateFormat("HH:mm:ss").format(new Date()) + "  " + message + "\n";
        SwingUtilities.invokeLater(() -> {
            logArea.append(line);
            logArea.setCaretPosition(logArea.getDocument().getLength());
        });
    }

    private void logException( String message, Throwable t ) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        log("ERROR: " + message + "\n" + sw);
    }

    /** A monitor that forwards messages to the log and supports cancelation. */
    private class UiMonitor extends DummyProgressMonitor {
        private volatile boolean canceled = false;

        @Override
        public void beginTask( String name, int totalWork ) {
            log(name);
        }

        @Override
        public void message( String message ) {
            log(message);
        }

        @Override
        public void errorMessage( String message ) {
            log("ERROR: " + message);
        }

        @Override
        public void exceptionThrown( String message ) {
            log("EXCEPTION: " + message);
        }

        @Override
        public boolean isCanceled() {
            return canceled;
        }

        @Override
        public void setCanceled( boolean value ) {
            canceled = value;
        }
    }

    // ==================== bbox fields ====================

    private void setBboxFields( Envelope env ) {
        updatingBboxFields = true;
        try {
            westField.setText(env == null ? "" : String.format(java.util.Locale.ROOT, "%.5f", env.getMinX()));
            southField.setText(env == null ? "" : String.format(java.util.Locale.ROOT, "%.5f", env.getMinY()));
            eastField.setText(env == null ? "" : String.format(java.util.Locale.ROOT, "%.5f", env.getMaxX()));
            northField.setText(env == null ? "" : String.format(java.util.Locale.ROOT, "%.5f", env.getMaxY()));
        } finally {
            updatingBboxFields = false;
        }
        mapPanel.setQueryBbox(env);
    }

    private void bboxFieldsChanged() {
        if (!updatingBboxFields) {
            Envelope bbox = readBboxFields();
            mapPanel.setQueryBbox(bbox);
            if (bbox != null)
                bboxCheck.setSelected(true);
        }
    }

    /**
     * @return the bbox from the fields or null if not valid.
     */
    private Envelope readBboxFields() {
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

    // ==================== utilities ====================

    private static Envelope getCollectionEnvelope( HMStacCollection collection ) {
        if (collection == null)
            return null;
        try {
            ReferencedEnvelope bounds = collection.getSpatialBounds();
            if (bounds == null || bounds.isNull() || bounds.isEmpty())
                return null;
            return new Envelope(bounds.getMinX(), bounds.getMaxX(), bounds.getMinY(), bounds.getMaxY());
        } catch (Exception e) {
            return null;
        }
    }

    private static Object attribute( SimpleFeature feature, String name ) {
        return feature.getFeatureType().getDescriptor(name) != null ? feature.getAttribute(name) : null;
    }

    private static String handlerName( HMStacAsset asset ) {
        if (asset.isValid() && asset.getHandler() != null)
            return asset.getHandler().getClass().getSimpleName();
        return UNSUPPORTED;
    }

    private static String href( HMStacAsset asset ) {
        JsonNode hrefNode = asset.getAssetNode().get("href");
        return hrefNode != null ? hrefNode.asText() : null;
    }

    private static String toJson( Object object ) {
        try {
            return MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(object);
        } catch (Exception e) {
            return String.valueOf(object);
        }
    }

    private static SimpleDateFormat utcFormat( String pattern ) {
        SimpleDateFormat f = new SimpleDateFormat(pattern);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        f.setLenient(false);
        return f;
    }

    private static String formatDate( Date date ) {
        return utcFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    /**
     * Parse a UTC date. A date without time is taken as start of day, or end of day if endOfDay is true.
     */
    private static Date parseDate( String text, boolean endOfDay ) {
        String t = text.trim().replace('T', ' ').replace("Z", "");
        for( String pattern : new String[]{"yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd HH:mm"} ) {
            try {
                return utcFormat(pattern).parse(t);
            } catch (ParseException e) {
                // try next
            }
        }
        try {
            Date day = utcFormat("yyyy-MM-dd").parse(t);
            return endOfDay ? new Date(day.getTime() + 24L * 3600 * 1000 - 1000) : day;
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse date: " + text + " (use yyyy-MM-dd or yyyy-MM-dd HH:mm:ss)");
        }
    }

    private static String bboxString( Envelope env ) {
        return String.format(java.util.Locale.ROOT, "W %.4f, S %.4f, E %.4f, N %.4f", env.getMinX(), env.getMinY(), env.getMaxX(),
                env.getMaxY());
    }

    private static String humanSize( long bytes ) {
        if (bytes < 1024)
            return bytes + " B";
        String[] units = {"KB", "MB", "GB", "TB"};
        double v = bytes;
        int u = -1;
        while( v >= 1024 && u < units.length - 1 ) {
            v /= 1024;
            u++;
        }
        return String.format("%.1f %s", v, units[u]);
    }

    private static String nn( String s ) {
        return s == null ? "" : s;
    }

    private static String escape( String s ) {
        if (s == null)
            return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static void row( StringBuilder sb, String key, String value ) {
        sb.append("<tr><td valign='top'><b>").append(escape(key)).append("</b></td><td>").append(value).append("</td></tr>");
    }

    private static JEditorPane htmlPane() {
        JEditorPane pane = new JEditorPane("text/html", "");
        pane.setEditable(false);
        pane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        return pane;
    }

    private static JTable table( RowTableModel< ? > model ) {
        JTable table = new JTable(model){
            @Override
            public String getToolTipText( MouseEvent e ) {
                // show the full content of truncated cells (hrefs, errors...)
                int row = rowAtPoint(e.getPoint());
                int col = columnAtPoint(e.getPoint());
                if (row < 0 || col < 0)
                    return null;
                Object value = getValueAt(row, col);
                return value == null || value instanceof Boolean ? null : value.toString();
            }
        };
        table.setRowSorter(new TableRowSorter<>(model));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        return table;
    }

    private static JButton button( String text, Runnable action ) {
        JButton b = new JButton(text);
        b.addActionListener(e -> action.run());
        return b;
    }

    private static JPanel buttons( JButton... buttons ) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        for( JButton b : buttons ) {
            p.add(b);
        }
        return p;
    }

    /** A panel that, inside a scrollpane, adapts to the viewport width instead of scrolling horizontally. */
    private static class WidthTrackingPanel extends JPanel implements Scrollable {
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }
        public int getScrollableUnitIncrement( Rectangle visibleRect, int orientation, int direction ) {
            return 16;
        }
        public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ) {
            return visibleRect.height;
        }
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    private static JPanel section( String title ) {
        JPanel p = new JPanel(new GridBagLayout());
        p.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), title, TitledBorder.LEFT, TitledBorder.TOP));
        p.setAlignmentX(Component.LEFT_ALIGNMENT);
        return p;
    }

    private static void addRow( JPanel panel, int row, Component... components ) {
        for( int i = 0; i < components.length; i++ ) {
            GridBagConstraints c = new GridBagConstraints();
            c.gridx = i;
            c.gridy = row;
            c.insets = new Insets(2, 3, 2, 3);
            c.anchor = GridBagConstraints.WEST;
            boolean isField = components[i] instanceof JTextField || components[i] instanceof JSpinner;
            c.fill = isField || components.length == 1 ? GridBagConstraints.HORIZONTAL : GridBagConstraints.NONE;
            c.weightx = isField ? 1 : 0;
            if (components.length == 1)
                c.gridwidth = 4;
            else if (i == components.length - 1 && components.length == 2)
                c.gridwidth = 3; // single label + field rows span the full width
            panel.add(components[i], c);
        }
    }

    private static void onTextChange( JTextField field, Runnable action ) {
        field.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate( DocumentEvent e ) {
                action.run();
            }
            public void removeUpdate( DocumentEvent e ) {
                action.run();
            }
            public void changedUpdate( DocumentEvent e ) {
                action.run();
            }
        });
    }

    private static List<String> getCatalogHistory() {
        LinkedHashSet<String> catalogs = new LinkedHashSet<>();
        for( String c : PreferencesHandler.getPreference(PREF_CATALOGS, new String[0]) ) {
            if (c != null && !c.isBlank())
                catalogs.add(c);
        }
        catalogs.addAll(Arrays.asList(PRESET_CATALOGS));
        return new ArrayList<>(catalogs);
    }

    private void addCatalogToHistory( String url ) {
        List<String> history = new ArrayList<>();
        history.add(url);
        for( String c : PreferencesHandler.getPreference(PREF_CATALOGS, new String[0]) ) {
            if (c != null && !c.isBlank() && !c.equals(url) && history.size() < 20)
                history.add(c);
        }
        PreferencesHandler.setPreference(PREF_CATALOGS, history.toArray(new String[0]));
        if (((javax.swing.DefaultComboBoxModel<String>) catalogCombo.getModel()).getIndexOf(url) < 0)
            catalogCombo.insertItemAt(url, 0);
    }

    // ==================== lifecycle ====================

    @Override
    public void onClose() {
        mapPanel.dispose();
        thumbnailLoader.shutdownNow();
        if (manager != null) {
            try {
                manager.close();
            } catch (Exception e) {
                // ignore on exit
            }
        }
    }

    @Override
    public boolean canCloseWithoutPrompt() {
        return true;
    }

    public static void main( String[] args ) {
        GuiUtilities.setDefaultLookAndFeel();
        DefaultGuiBridgeImpl gBridge = new DefaultGuiBridgeImpl();
        StacBrowser browser = new StacBrowser();
        JFrame frame = gBridge.showWindow(browser, "HortonMachine STAC Browser - " + org.hortonmachine.Version.getVersion());
        GuiUtilities.setDefaultFrameIcon(frame);
        GuiUtilities.addClosingListener(frame, browser);
        // optional arguments: catalog url [aws profile [aws region]]
        // applied on the EDT, since the frame is already showing
        if (args.length > 0 && !args[0].isBlank()) {
            SwingUtilities.invokeLater(() -> {
                browser.catalogCombo.setSelectedItem(args[0]);
                if (args.length > 1)
                    browser.awsProfileCombo.setSelectedItem(args[1]);
                if (args.length > 2)
                    browser.awsRegionField.setText(args[2]);
                browser.connect();
            });
        }
    }
}
