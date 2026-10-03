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
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.TableRowSorter;

import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.io.wfs.Wfs;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.gears.utils.crs.HMCrsTransformer;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.utils.RowTableModel;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

/**
 * The WFS part of the {@link WebServicesBrowser}: read the schema of the feature types, load features
 * of a region, show them on the map and in a table and save them.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
class WfsServicePanel extends ServicePanel {
    /** More features are loaded, but not drawn on the map. */
    private static final int MAX_MAP_FEATURES = 20_000;

    private Wfs wfs;
    private SimpleFeatureType schema;
    private SimpleFeatureCollection loadedFeatures;
    private String loadedTypeName;

    private JCheckBox arcgisCheck;
    private JSpinner maxFeaturesSpinner;
    private JCheckBox swapXYCheck;
    private JPanel requestPanel;

    private JButton loadButton;
    private JButton exportButton;
    private JPanel actionPanel;

    private RowTableModel<SimpleFeature> featuresModel = new RowTableModel<>();
    private JTable featuresTable;
    private JLabel resultInfoLabel;
    private JPanel resultPanel;

    WfsServicePanel( WebServicesBrowser browser ) {
        super(browser);
        createRequestPanel();
        createActionPanel();
        createResultPanel();
    }

    @Override
    String getType() {
        return "WFS";
    }

    @Override
    String[] getVersions() {
        return new String[]{"auto", "2.0.0", "1.1.0", "1.0.0"};
    }

    @Override
    String[] getPresets() {
        return new String[]{//
                "https://visualizador.ideam.gov.co/gisserver/services/Vulnerabilidad_Susceptibilidad_Ambiental/MapServer/WFSServer?service=WFS&request=GetCapabilities"//
        };
    }

    // ==================== ui ====================

    private void createRequestPanel() {
        requestPanel = new JPanel();
        requestPanel.setLayout(new BoxLayout(requestPanel, BoxLayout.Y_AXIS));

        JPanel connectionPanel = section("Connection options (applied on Connect)");
        arcgisCheck = new JCheckBox("ArcGIS server compatibility");
        arcgisCheck.setToolTipText("Use the ArcGIS strategy and lenient parsing, for ArcGIS WFS servers");
        addRow(connectionPanel, 0, arcgisCheck);
        requestPanel.add(connectionPanel);

        JPanel queryPanel = section("GetFeature parameters");
        maxFeaturesSpinner = new JSpinner(new SpinnerNumberModel(1000, 0, 10_000_000, 500));
        maxFeaturesSpinner.setToolTipText("Maximum number of features to request (0 = no limit, the service may still apply one)");
        swapXYCheck = new JCheckBox("Swap the coordinates (x <-> y)");
        swapXYCheck.setToolTipText("Last resort for services returning lat/lon where lon/lat is expected: swaps the region and the geometries");
        addRow(queryPanel, 0, new JLabel("Max features (0 = all)"), maxFeaturesSpinner);
        addRow(queryPanel, 1, swapXYCheck);
        addRow(queryPanel, 2, WebMapsUi.note("The region is transformed to the CRS of the feature type and used as bbox filter."));
        requestPanel.add(queryPanel);
    }

    private void createActionPanel() {
        actionPanel = section("Requests");
        loadButton = button("Load features", this::loadFeatures);
        loadButton.setToolTipText("GetFeature of the region (or of the whole type), shown on the map and in the Result tab");
        loadButton.setFont(loadButton.getFont().deriveFont(Font.BOLD));
        exportButton = button("Export...", this::exportFeatures);
        exportButton.setToolTipText("Save the loaded features to a GeoPackage or shapefile");
        JPanel buttons = new JPanel(new GridLayout(1, 2, 4, 0));
        buttons.add(loadButton);
        buttons.add(exportButton);
        addRow(actionPanel, 0, buttons);
    }

    private void createResultPanel() {
        resultPanel = new JPanel(new BorderLayout());
        featuresTable = WebMapsUi.table(featuresModel);
        featuresTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        featuresTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting())
                return;
            int viewRow = featuresTable.getSelectedRow();
            int index = viewRow < 0 ? -1 : featuresTable.convertRowIndexToModel(viewRow);
            browser.getMap().setSelectedFootprint(index < MAX_MAP_FEATURES ? index : -1);
        });
        resultInfoLabel = new JLabel("Load features to see them here.");
        resultInfoLabel.setBorder(BorderFactory.createEmptyBorder(4, 6, 4, 6));
        resultPanel.add(resultInfoLabel, BorderLayout.NORTH);
        resultPanel.add(new JScrollPane(featuresTable), BorderLayout.CENTER);
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
        loadButton.setEnabled(!busy && wfs != null);
        exportButton.setEnabled(!busy && loadedFeatures != null);
    }

    @Override
    void footprintSelected( int index ) {
        if (index >= 0 && index < featuresModel.getRowCount()) {
            int viewRow = featuresTable.convertRowIndexToView(index);
            featuresTable.getSelectionModel().setSelectionInterval(viewRow, viewRow);
            featuresTable.scrollRectToVisible(featuresTable.getCellRect(viewRow, 0, true));
            browser.showResultTab();
        } else {
            featuresTable.clearSelection();
        }
    }

    // ==================== connection ====================

    @Override
    List<LayerEntry> connect( String url, String version ) throws Exception {
        Wfs newWfs = new Wfs(url);
        if (version != null)
            newWfs.forceVersion(version);
        if (arcgisCheck.isSelected())
            newWfs.forceArcgisCompatibility();
        // makes the features writable by the geotools file writers
        newWfs.forceNormalizeGeometryName();
        newWfs.connect();
        List<LayerEntry> layers = new ArrayList<>();
        for( String typeName : newWfs.getTypeNames() ) {
            layers.add(new LayerEntry(typeName, null, null, null));
        }
        serviceInfoHtml = serviceHtml(url, newWfs, layers.size());
        wfs = newWfs;
        return layers;
    }

    @Override
    void connected() {
        schema = null;
        clearResult();
    }

    @Override
    void close() {
        if (wfs != null) {
            try {
                wfs.close();
            } catch (Exception e) {
                // ignore
            }
            wfs = null;
        }
    }

    // ==================== feature type ====================

    @Override
    void selectLayer( LayerEntry entry ) {
        schema = null;
        setLayerInfo(HTML_START + "<h2>" + escape(entry.name) + "</h2><p><i>Reading the schema...</i></p>" + HTML_END);
        browser.runTask("Reading the schema of " + entry.name, () -> {
            wfs.setTypeName(entry.name);
            SimpleFeatureType type = wfs.getSimpleFeatureType();
            ReferencedEnvelope bounds = null;
            try {
                bounds = wfs.getBounds();
            } catch (Exception e) {
                browser.logException("Unable to read the bounds of " + entry.name, e);
            }
            ResourceInfo info = null;
            try {
                info = wfs.getTypeInfo(entry.name);
            } catch (Exception e) {
                browser.logException("Unable to read the info of " + entry.name, e);
            }
            Envelope wgs84 = null;
            if (bounds != null && !bounds.isEmpty() && bounds.getCoordinateReferenceSystem() != null) {
                try {
                    ReferencedEnvelope b = bounds.transform(DefaultGeographicCRS.WGS84, true);
                    wgs84 = new Envelope(b.getMinX(), b.getMaxX(), b.getMinY(), b.getMaxY());
                } catch (Exception e) {
                    browser.logException("Unable to transform the bounds of " + entry.name + " to WGS84", e);
                }
            }
            return new Object[]{type, bounds, info, wgs84};
        }, result -> {
            if (entry != currentEntry)
                return;
            schema = (SimpleFeatureType) result[0];
            ResourceInfo info = (ResourceInfo) result[2];
            entry.wgs84 = (Envelope) result[3];
            if (info != null && info.getTitle() != null && !info.getTitle().equals(entry.name)) {
                entry.title = info.getTitle();
                browser.layerEntryChanged();
            }
            if (entry.wgs84 != null) {
                browser.getMap().setExtents(List.of(entry.wgs84));
                browser.getMap().zoomToEnvelope(entry.wgs84);
            }
            setLayerInfo(typeHtml(entry, schema, (ReferencedEnvelope) result[1], info));
            browser.log("Schema of " + entry.name + ": " + schema.getAttributeCount() + " attributes.");
        });
    }

    private void clearResult() {
        loadedFeatures = null;
        loadedTypeName = null;
        featuresModel = new RowTableModel<>();
        featuresTable.setModel(featuresModel);
        featuresTable.setRowSorter(new TableRowSorter<>(featuresModel));
        resultInfoLabel.setText("Load features to see them here.");
        setBusy(browser.isBusy());
    }

    // ==================== requests ====================

    private void loadFeatures() {
        if (currentEntry == null || schema == null) {
            GuiUtilities.showWarningMessage(browser, "Please select a feature type and wait for its schema to be read.");
            return;
        }
        Envelope region;
        try {
            region = browser.getRegion();
        } catch (IllegalArgumentException e) {
            GuiUtilities.showWarningMessage(browser, e.getMessage());
            return;
        }
        String typeName = currentEntry.name;
        SimpleFeatureType type = schema;
        int maxFeatures = (Integer) maxFeaturesSpinner.getValue();
        boolean swap = swapXYCheck.isSelected();
        browser.runTask("Loading features of " + typeName, () -> {
            wfs.setMaxFeatures(maxFeatures);
            wfs.setCoordinateSwapping(swap);
            CoordinateReferenceSystem crs = type.getCoordinateReferenceSystem();
            Envelope filter = null;
            if (region != null) {
                filter = crs != null ? new ReferencedEnvelope(region, DefaultGeographicCRS.WGS84).transform(crs, true) : region;
                browser.log("Bbox filter in " + crsName(crs) + ": " + WebMapsUi.axesString(filter) + (swap ? " (swapped by the client)" : ""));
            }
            long t0 = System.currentTimeMillis();
            SimpleFeatureCollection fc = wfs.getFeatureCollection(filter);
            long millis = System.currentTimeMillis() - t0;

            List<SimpleFeature> features = new ArrayList<>();
            List<Geometry> footprints = new ArrayList<>();
            CoordinateReferenceSystem dataCrs = fc.getSchema().getCoordinateReferenceSystem();
            if (dataCrs == null)
                dataCrs = crs;
            HMCrsTransformer toWgs84 = dataCrs != null && !HMCrsRegistry.crsEquals(dataCrs, DefaultGeographicCRS.WGS84)
                    ? new HMCrsTransformer(dataCrs, DefaultGeographicCRS.WGS84)
                    : null;
            int transformErrors = 0;
            try (SimpleFeatureIterator it = fc.features()) {
                while( it.hasNext() ) {
                    SimpleFeature f = it.next();
                    features.add(f);
                    if (footprints.size() < MAX_MAP_FEATURES) {
                        Geometry g = (Geometry) f.getDefaultGeometry();
                        try {
                            footprints.add(g == null ? null : toWgs84 != null ? toWgs84.transform(g) : g);
                        } catch (Exception e) {
                            footprints.add(null);
                            transformErrors++;
                        }
                    }
                }
            }
            if (transformErrors > 0)
                browser.log("WARNING: " + transformErrors + " geometries could not be transformed to WGS84 for the map.");
            return new Object[]{fc, features, footprints, millis};
        }, result -> {
            @SuppressWarnings("unchecked")
            List<SimpleFeature> features = (List<SimpleFeature>) result[1];
            @SuppressWarnings("unchecked")
            List<Geometry> footprints = (List<Geometry>) result[2];
            loadedFeatures = (SimpleFeatureCollection) result[0];
            loadedTypeName = typeName;
            showFeatures(features, ((SimpleFeatureCollection) result[0]).getSchema());
            browser.getMap().setOverlay(null, null, false);
            browser.getMap().setFootprints(footprints);
            String info = features.size() + " features of " + typeName + " loaded in " + result[3] + " ms"
                    + (maxFeatures > 0 && features.size() >= maxFeatures ? " (max features reached)" : "")
                    + (features.size() > MAX_MAP_FEATURES ? ", the first " + MAX_MAP_FEATURES + " drawn on the map" : "") + ".";
            resultInfoLabel.setText(info);
            browser.log(info);
            browser.setStatus(info);
            browser.showResultTab();
            setBusy(false);
        });
    }

    private void showFeatures( List<SimpleFeature> features, SimpleFeatureType type ) {
        RowTableModel<SimpleFeature> model = new RowTableModel<>();
        model.col("Id", String.class, f -> f.getID());
        GeometryDescriptor geometryDescriptor = type.getGeometryDescriptor();
        if (geometryDescriptor != null) {
            model.col("Geometry", String.class, f -> {
                Object g = f.getDefaultGeometry();
                return g instanceof Geometry geometry ? geometry.getGeometryType() + " (" + geometry.getNumPoints() + " pts)" : "";
            });
        }
        for( AttributeDescriptor ad : type.getAttributeDescriptors() ) {
            if (ad instanceof GeometryDescriptor)
                continue;
            String name = ad.getLocalName();
            Class< ? > binding = ad.getType().getBinding();
            Class< ? > columnClass = Number.class.isAssignableFrom(binding) || binding == Boolean.class ? binding : String.class;
            model.col(name, columnClass, f -> {
                Object v = f.getAttribute(name);
                return v == null || columnClass != String.class ? v : v.toString();
            });
        }
        model.setRows(features);
        featuresModel = model;
        featuresTable.setModel(model);
        featuresTable.setRowSorter(new TableRowSorter<>(model));
        for( int i = 0; i < featuresTable.getColumnCount(); i++ ) {
            featuresTable.getColumnModel().getColumn(i).setPreferredWidth(i == 0 ? 180 : 120);
        }
    }

    private void exportFeatures() {
        if (loadedFeatures == null) {
            GuiUtilities.showWarningMessage(browser, "Load some features first.");
            return;
        }
        File file = GuiUtilities.showSaveFileDialog(browser, "Save the features (.gpkg or .shp)", PreferencesHandler.getLastFile());
        if (file == null)
            return;
        String name = file.getName().toLowerCase();
        if (!name.endsWith(".gpkg") && !name.endsWith(".shp"))
            file = new File(file.getParentFile(), file.getName() + ".gpkg");
        PreferencesHandler.setLastPath(file.getAbsolutePath());
        String path = file.getAbsolutePath();
        // a geopackage needs the table name
        String tableName = loadedTypeName.replaceAll("[^A-Za-z0-9_]", "_");
        String writerPath = path.toLowerCase().endsWith(".gpkg") ? path + "#" + tableName : path;
        SimpleFeatureCollection fc = loadedFeatures;
        browser.runTask("Exporting " + fc.size() + " features to " + path, () -> {
            OmsVectorWriter.writeVector(writerPath, fc);
            return path;
        }, p -> {
            browser.log("Features saved to " + writerPath);
            browser.setStatus("Features saved to " + p);
        });
    }

    // ==================== info ====================

    private static String crsName( CoordinateReferenceSystem crs ) {
        if (crs == null)
            return "unknown CRS";
        try {
            String code = HMCrsRegistry.getCodeFromCrs(crs);
            if (code != null)
                return code;
        } catch (Exception e) {
            // use the name
        }
        return crs.getName().toString();
    }

    private static String serviceHtml( String url, Wfs wfs, int typesCount ) {
        StringBuilder sb = new StringBuilder(HTML_START);
        ServiceInfo info = wfs.getServiceInfo();
        sb.append("<h2>").append(escape(info != null && info.getTitle() != null ? info.getTitle() : url)).append("</h2>");
        sb.append("<table cellpadding='3'>");
        row(sb, "Url", escape(url));
        row(sb, "Version", escape(wfs.getVersion()));
        row(sb, "Feature types", String.valueOf(typesCount));
        if (info != null) {
            if (info.getDescription() != null)
                row(sb, "Description", escape(info.getDescription()).replace("\n", "<br>"));
            Set<String> keywords = info.getKeywords();
            if (keywords != null && !keywords.isEmpty())
                row(sb, "Keywords", escape(String.join(", ", keywords)));
            if (info.getPublisher() != null)
                row(sb, "Publisher", escape(info.getPublisher().toString()));
            if (info.getSource() != null)
                row(sb, "Source", escape(info.getSource().toString()));
        }
        sb.append("</table>").append(HTML_END);
        return sb.toString();
    }

    private static String typeHtml( LayerEntry entry, SimpleFeatureType type, ReferencedEnvelope bounds, ResourceInfo info ) {
        StringBuilder sb = new StringBuilder(HTML_START);
        sb.append("<h2>").append(escape(entry.title != null ? entry.title : entry.name)).append("</h2>");
        sb.append("<table cellpadding='3'>");
        row(sb, "Type name", escape(entry.name));
        if (info != null) {
            if (info.getDescription() != null)
                row(sb, "Description", escape(info.getDescription()).replace("\n", "<br>"));
            if (info.getKeywords() != null && !info.getKeywords().isEmpty())
                row(sb, "Keywords", escape(String.join(", ", info.getKeywords())));
        }
        CoordinateReferenceSystem crs = type.getCoordinateReferenceSystem();
        row(sb, "CRS", escape(crsName(crs)));
        if (crs != null)
            row(sb, "Axes", escape(crs.getCoordinateSystem().getAxis(0).getDirection() + ", "
                    + crs.getCoordinateSystem().getAxis(1).getDirection()));
        row(sb, "Bounds", bounds != null ? escape(WebMapsUi.axesString(bounds)) : "<i>not available</i>");
        row(sb, "WGS84 bounds", entry.wgs84 != null ? escape(bboxString(entry.wgs84)) : "<i>not available</i>");
        GeometryDescriptor gd = type.getGeometryDescriptor();
        row(sb, "Geometry", gd != null
                ? escape(gd.getLocalName() + " (" + gd.getType().getBinding().getSimpleName() + ")")
                : WebMapsUi.colored(WebMapsUi.KO_COLOR, "none"));
        sb.append("</table><h3>Attributes</h3><table cellpadding='3'>");
        sb.append("<tr><th align='left'>Name</th><th align='left'>Type</th></tr>");
        for( AttributeDescriptor ad : type.getAttributeDescriptors() ) {
            sb.append("<tr><td>").append(escape(ad.getLocalName())).append("</td><td>")
                    .append(escape(ad.getType().getBinding().getSimpleName())).append("</td></tr>");
        }
        sb.append("</table>").append(HTML_END);
        return sb.toString();
    }
}
