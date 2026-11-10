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
package org.hortonmachine.nww.tools.features;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableModel;

import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.Converters;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.io.vectorwriter.OmsVectorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.utils.PreferencesHandler;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.layers.defaults.NwwEditableVectorLayer;
import org.hortonmachine.nww.layers.defaults.NwwFeatureLayer;
import org.hortonmachine.nww.tools.INwwTool;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.tools.NwwContext;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;

import gov.nasa.worldwind.WorldWind;
import gov.nasa.worldwind.geom.Position;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;
import gov.nasa.worldwind.layers.RenderableLayer;
import gov.nasa.worldwind.render.BasicShapeAttributes;
import gov.nasa.worldwind.render.Material;
import gov.nasa.worldwind.render.PointPlacemark;
import gov.nasa.worldwind.render.PointPlacemarkAttributes;
import gov.nasa.worldwind.render.ShapeAttributes;
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfacePolyline;

/**
 * The tab with the selected features of the viewer, as selected at a point or by box on the
 * current layer, all highlighted on the map.
 *
 * <p>The features are listed when there are more; the attributes of the chosen one are shown
 * below the list and can be edited if the layer has a feature store, as the shapefiles. The
 * selection can be zoomed to, exported to a geopackage table and used as filter of its layer.</p>
 *
 * <p>For a raster layer, the tab shows the values of its bands at the clicked point.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class FeatureInfoTool implements INwwTool {

    private static final String HINT = "<html>Choose Select at the side of the map, then click a feature or drag "
            + "a box on the current layer, the one selected in the layers panel. Shift adds to the selection. "
            + "On a raster layer, a click shows its values.</html>";
    private static final Color SELECTED_COLOR = Color.YELLOW;
    private static final Color CHOSEN_COLOR = new Color(0xff8c00);
    /** The most selected features highlighted on the map. */
    private static final int MAX_HIGHLIGHTED = 5000;
    private static final String PREF_EXPORT_TABLE = "NWW_SELECTION_EXPORT";

    private NwwContext context;

    private final JLabel infoLabel = new JLabel(HINT);
    private final JButton zoomButton = new JButton("Zoom to");
    private final JButton exportButton = new JButton("Export...");
    private final JButton filterButton = new JButton("Use as filter");
    private final JButton clearButton = new JButton("Clear");
    private final JButton saveButton = new JButton("Save");
    private final JButton revertButton = new JButton("Revert");
    private final JLabel editLabel = new JLabel();

    /** The selected features, one per row. */
    private final DefaultTableModel listModel = new DefaultTableModel(){
        private static final long serialVersionUID = 1L;
        @Override
        public boolean isCellEditable( int row, int column ) {
            return false;
        }
    };
    private final JTable listTable = new JTable(listModel);
    private JScrollPane listScroll;

    /** The attributes of the chosen feature. */
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Field", "Value"}, 0){
        private static final long serialVersionUID = 1L;
        @Override
        public boolean isCellEditable( int row, int column ) {
            return column == 1 && isEditable();
        }
    };
    private final JTable table = new JTable(model);
    /** The highlight of the selected features, not in the layers panel. */
    private final RenderableLayer highlightLayer = new RenderableLayer();

    private FeatureSelection selection;
    private int index = 0;
    /** The values of the chosen feature as shown in the table, to find the edited ones. */
    private final List<String> shownValues = new ArrayList<>();
    private final List<String> shownNames = new ArrayList<>();
    private boolean updatingList = false;

    @Override
    public String getName() {
        return "Info";
    }

    @Override
    public ImageIcon getIcon() {
        return ImageCache.get(ImageCache.INFO16);
    }

    @Override
    public JComponent createPanel( NwwContext context ) {
        this.context = context;
        // not listed in the layers panel
        highlightLayer.setName("hide selected features");
        highlightLayer.setPickEnabled(false);

        zoomButton.setIcon(ImageCache.get(ImageCache.ZOOM_TO_ALL));
        zoomButton.setToolTipText("Zoom the map to the selected features");
        zoomButton.addActionListener(e -> zoomToSelection());
        exportButton.setIcon(ImageCache.get(ImageCache.EXPORT));
        exportButton.setToolTipText("Write the selected features to a table of a geopackage, in WGS84 lon/lat");
        exportButton.addActionListener(e -> exportSelection());
        filterButton.setToolTipText("Show only the selected features in the layer, as a filter of its layer tab");
        filterButton.addActionListener(e -> useAsFilter());
        clearButton.setIcon(ImageCache.get(ImageCache.TRASH));
        clearButton.setToolTipText("Clear the selection");
        clearButton.addActionListener(e -> context.clearSelection());
        saveButton.setIcon(ImageCache.get(ImageCache.SAVE));
        saveButton.setToolTipText("Write the edited values into the data of the layer");
        saveButton.addActionListener(e -> save());
        revertButton.setToolTipText("Show the values of the data again, dropping the edits");
        revertButton.addActionListener(e -> showFeature(index));

        listTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        listTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !updatingList && listTable.getSelectedRow() >= 0) {
                showFeature(listTable.convertRowIndexToModel(listTable.getSelectedRow()));
            }
        });
        listScroll = new JScrollPane(listTable);
        listScroll.setPreferredSize(new Dimension(100, 160));

        table.setFillsViewportHeight(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        // two rows, to fit the narrow tab
        JPanel actions = new JPanel(new GridLayout(2, 2, 4, 4));
        actions.add(zoomButton);
        actions.add(exportButton);
        actions.add(filterButton);
        actions.add(clearButton);
        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(infoLabel, BorderLayout.NORTH);
        north.add(actions, BorderLayout.CENTER);
        north.add(listScroll, BorderLayout.SOUTH);

        JPanel south = new JPanel(new BorderLayout(4, 0));
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttons.add(revertButton);
        buttons.add(saveButton);
        south.add(editLabel, BorderLayout.CENTER);
        south.add(buttons, BorderLayout.EAST);

        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        panel.add(north, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        panel.add(south, BorderLayout.SOUTH);

        context.addSelectionListener(this::showSelection);
        showSelection(context.getSelection());
        return panel;
    }

    /**
     * Show a message instead of features, as when there is no layer to identify on. The
     * selection is cleared.
     *
     * @param message the message.
     */
    public void showMessage( String message ) {
        if (context != null) {
            context.clearSelection();
            clearView();
        }
        infoLabel.setText("<html>" + message + "</html>");
    }

    /**
     * Remove the rows of the table and the highlight, as the values of a raster.
     */
    private void clearView() {
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }
        model.setRowCount(0);
        shownNames.clear();
        shownValues.clear();
        highlightLayer.removeAllRenderables();
        context.getNwwPanel().getWwd().getModel().getLayers().remove(highlightLayer);
        context.getNwwPanel().getWwd().redraw();
    }

    /**
     * Show the values of a raster at a point instead of features, with a mark on the map. The
     * selection is cleared.
     *
     * @param layer the raster layer.
     * @param point the clicked point.
     * @param values the values at the point.
     */
    public void showRasterValues( Layer layer, Position point, RasterValues values ) {
        if (context == null) {
            return;
        }
        context.clearSelection();
        // also the values of a former click, which is not a selection
        clearView();
        String where = String.format(Locale.ROOT, "%.6f, %.6f", point.getLongitude().degrees, point.getLatitude().degrees);
        if (!values.isInside()) {
            infoLabel.setText("<html><b>" + layer.getName() + "</b>: " + where + " is outside of the raster</html>");
            return;
        }
        infoLabel.setText("<html><b>" + layer.getName() + "</b>: values at " + where + "</html>");
        double[] bandValues = values.values();
        for( int band = 0; band < bandValues.length; band++ ) {
            double value = bandValues[band];
            String text = values.isNovalue(value) ? "novalue" : numberText(value);
            model.addRow(new Object[]{bandValues.length == 1 ? "Value" : "Band " + (band + 1), text});
        }
        model.addRow(new Object[]{"Column, row", values.col() + ", " + values.row()});
        model.addRow(new Object[]{"X, Y", numberText(values.x()) + ", " + numberText(values.y())});

        PointPlacemarkAttributes attributes = new PointPlacemarkAttributes();
        attributes.setUsePointAsDefaultImage(true);
        attributes.setLineMaterial(new Material(CHOSEN_COLOR));
        attributes.setScale(12.0);
        PointPlacemark placemark = new PointPlacemark(Position.fromDegrees(point.getLatitude().degrees,
                point.getLongitude().degrees, 0));
        placemark.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
        placemark.setAttributes(attributes);
        highlightLayer.addRenderable(placemark);
        LayerList layers = context.getNwwPanel().getWwd().getModel().getLayers();
        layers.remove(highlightLayer);
        layers.add(highlightLayer);
        context.getNwwPanel().getWwd().redraw();
    }

    /**
     * @return whole numbers without decimals, the others with up to 6 decimals.
     */
    private static String numberText( double value ) {
        if (Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        return String.format(Locale.ROOT, "%.6f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /**
     * Show a new selection, keeping the chosen feature if it is still selected.
     */
    private void showSelection( FeatureSelection newSelection ) {
        String chosenId = chosenFeature() == null ? null : chosenFeature().getID();
        selection = newSelection;
        List<SimpleFeature> features = getFeatures();

        updatingList = true;
        listModel.setRowCount(0);
        listModel.setColumnCount(0);
        int newIndex = 0;
        if (!features.isEmpty()) {
            List<String> names = attributeNames(features.get(0).getFeatureType());
            listModel.addColumn("#");
            for( String name : names ) {
                listModel.addColumn(name);
            }
            for( int i = 0; i < features.size(); i++ ) {
                SimpleFeature feature = features.get(i);
                Object[] row = new Object[names.size() + 1];
                row[0] = i + 1;
                for( int c = 0; c < names.size(); c++ ) {
                    row[c + 1] = feature.getAttribute(names.get(c));
                }
                listModel.addRow(row);
                if (feature.getID().equals(chosenId)) {
                    newIndex = i;
                }
            }
        }
        updatingList = false;
        if (listScroll != null) {
            listScroll.setVisible(features.size() > 1);
            listScroll.revalidate();
        }

        if (selection == null) {
            infoLabel.setText(HINT);
        } else {
            Position point = selection.getPoint();
            String where = point == null ? "" : String.format(" at %.6f, %.6f", point.getLongitude().degrees,
                    point.getLatitude().degrees);
            String count = features.isEmpty()
                    ? "no feature"
                    : features.size() + (features.size() == 1 ? " feature" : " features") + (point == null ? " selected" : "");
            infoLabel.setText("<html><b>" + selection.getLayer().getName() + "</b>: " + count + where + "</html>");
        }
        boolean any = !features.isEmpty();
        zoomButton.setEnabled(any);
        exportButton.setEnabled(any);
        clearButton.setEnabled(selection != null);
        String filterProblem = any ? filterProblem() : "No feature is selected";
        filterButton.setEnabled(filterProblem == null);
        filterButton.setToolTipText(filterProblem == null
                ? "Show only the selected features in the layer, as a filter of its layer tab"
                : filterProblem);
        showFeature(newIndex);
    }

    private List<SimpleFeature> getFeatures() {
        return selection == null ? Collections.emptyList() : selection.getFeatures();
    }

    private SimpleFeature chosenFeature() {
        List<SimpleFeature> features = getFeatures();
        return features.isEmpty() || index >= features.size() ? null : features.get(index);
    }

    private static List<String> attributeNames( SimpleFeatureType type ) {
        List<String> names = new ArrayList<>();
        for( AttributeDescriptor descriptor : type.getAttributeDescriptors() ) {
            if (descriptor != type.getGeometryDescriptor()) {
                names.add(descriptor.getLocalName());
            }
        }
        return names;
    }

    private Layer getLayer() {
        return selection == null ? null : selection.getLayer();
    }

    private boolean isEditable() {
        return getFeatureStore() != null;
    }

    private SimpleFeatureStore getFeatureStore() {
        Layer layer = getLayer();
        return layer instanceof NwwFeatureLayer ? ((NwwFeatureLayer) layer).getFeatureStore() : null;
    }

    /**
     * Show the attributes of a selected feature, choose it in the list and highlight it.
     */
    private void showFeature( int newIndex ) {
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }
        List<SimpleFeature> features = getFeatures();
        index = features.isEmpty() ? 0 : Math.max(0, Math.min(newIndex, features.size() - 1));
        model.setRowCount(0);
        shownNames.clear();
        shownValues.clear();
        SimpleFeature feature = chosenFeature();
        if (feature != null) {
            for( String name : attributeNames(feature.getFeatureType()) ) {
                Object value = feature.getAttribute(name);
                String text = value == null ? "" : value.toString();
                shownNames.add(name);
                shownValues.add(text);
                model.addRow(new Object[]{name, text});
            }
            int viewRow = listTable.convertRowIndexToView(index);
            if (viewRow >= 0 && listTable.getSelectedRow() != viewRow) {
                updatingList = true;
                listTable.setRowSelectionInterval(viewRow, viewRow);
                listTable.scrollRectToVisible(listTable.getCellRect(viewRow, 0, true));
                updatingList = false;
            }
        }
        boolean editable = feature != null && isEditable();
        saveButton.setEnabled(editable);
        revertButton.setEnabled(editable);
        editLabel.setText(feature == null || editable ? "" : "Read only: the layer has no editable source");
        highlight();
    }

    private void zoomToSelection() {
        Envelope envelope = selection == null ? new Envelope() : selection.getEnvelope();
        if (envelope.isNull()) {
            return;
        }
        // single points and small features get some room around
        double margin = Math.max(Math.max(envelope.getWidth(), envelope.getHeight()) * 0.2, 0.001);
        envelope.expandBy(margin);
        try {
            context.goTo(new ReferencedEnvelope(envelope, NwwUtilities.GPS_CRS));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Write the selected features to a table of a geopackage, asking file and table.
     */
    private void exportSelection() {
        if (selection == null || selection.isEmpty()) {
            return;
        }
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Geopackage to export the selection to, new or existing");
        fileChooser.setFileFilter(new FileNameExtensionFilter("GeoPackage (*.gpkg)", "gpkg"));
        fileChooser.setCurrentDirectory(PreferencesHandler.getLastFile());
        if (fileChooser.showSaveDialog(table) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File file = fileChooser.getSelectedFile();
        if (!file.getName().toLowerCase().endsWith(".gpkg")) {
            file = new File(file.getParentFile(), file.getName() + ".gpkg");
        }
        PreferencesHandler.setLastPath(file.getAbsolutePath());

        String defaultTable = selection.getLayer().getName().replaceAll(".*" + HMConstants.DB_TABLE_PATH_SEPARATOR, "")
                .replaceAll("\\.[a-zA-Z0-9]+$", "").replaceAll("[^a-zA-Z0-9_]", "_") + "_selection";
        String tableName = (String) JOptionPane.showInputDialog(table, "Name of the table", "Export selection",
                JOptionPane.QUESTION_MESSAGE, null, null, PreferencesHandler.getPreference(PREF_EXPORT_TABLE, defaultTable));
        if (tableName == null || tableName.isBlank()) {
            return;
        }
        tableName = tableName.trim();
        try {
            if (file.exists()) {
                try (GeopackageCommonDb db = new GeopackageDb()) {
                    db.open(file.getAbsolutePath());
                    if (db.hasTable(SqlName.m(tableName))) {
                        int answer = JOptionPane.showConfirmDialog(table,
                                "The table " + tableName + " exists in " + file.getName() + ": replace it?", "Export selection",
                                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                        if (answer != JOptionPane.YES_OPTION) {
                            return;
                        }
                    }
                }
            }
            OmsVectorWriter.writeVector(file.getAbsolutePath() + HMConstants.DB_TABLE_PATH_SEPARATOR + tableName,
                    selection.toFeatureCollection());
            GuiUtilities.showInfoMessage(table, "Export selection",
                    "Exported " + selection.getFeatures().size() + " features to " + file.getName() + ", table " + tableName + ".");
        } catch (Exception e) {
            e.printStackTrace();
            GuiUtilities.showWarningMessage(table, "WARNING", "Unable to export the selection: " + e.getMessage());
        }
    }

    /**
     * @return why the selection can't be used as filter of its layer, <code>null</code> if it can.
     */
    private String filterProblem() {
        LayerSource source = LayerSource.of(getLayer());
        if (source == null || !(source.isShapefile() || source.isGeopackageTable())) {
            return "Only layers read from a shapefile or a geopackage table can be filtered";
        }
        return null;
    }

    /**
     * Show only the selected features in the layer: the filter of the layer is replaced by one
     * on the ids of the features, for the shapefiles, or on the primary key, for the geopackage
     * tables.
     */
    private void useAsFilter() {
        Layer layer = getLayer();
        LayerSource source = LayerSource.of(layer);
        if (source == null || selection.isEmpty()) {
            return;
        }
        String where;
        try {
            if (source.isShapefile()) {
                where = "IN (" + selection.getFeatures().stream().map(f -> "'" + f.getID().replace("'", "''") + "'")
                        .collect(Collectors.joining(", ")) + ")";
            } else {
                String pk;
                try (GeopackageCommonDb db = new GeopackageDb()) {
                    db.open(source.file().getAbsolutePath());
                    pk = db.getPrimaryKey(SqlName.m(source.table()));
                }
                if (pk == null || selection.getFeatures().get(0).getFeatureType().getDescriptor(pk) == null) {
                    GuiUtilities.showWarningMessage(table, "WARNING",
                            "The table has no primary key among its columns: its features can't be told apart.");
                    return;
                }
                where = "\"" + pk + "\" IN (" + selection.getFeatures().stream().map(f -> sqlValue(f.getAttribute(pk)))
                        .collect(Collectors.joining(", ")) + ")";
            }
            Layer replacement = context.getLayerLoader().reloadVectorLayer(layer, where);
            Position point = selection.getPoint();
            context.replaceLayer(layer, replacement);
            // the layer shows just the selected features, also for the geopackage features, whose
            // ids change at each reading
            List<SimpleFeature> all = new ArrayList<>();
            try (SimpleFeatureIterator iterator = ((NwwFeatureLayer) replacement).getFeatureCollection().features()) {
                while( iterator.hasNext() ) {
                    all.add(iterator.next());
                }
            }
            context.setSelection(new FeatureSelection(replacement, all, point));
            // the filter field of its tab shows the new filter
            context.refreshLayerPanel(replacement);
            context.openTool(this);
        } catch (Exception e) {
            e.printStackTrace();
            GuiUtilities.showWarningMessage(table, "WARNING", "Unable to filter the layer: " + e.getMessage());
        }
    }

    private static String sqlValue( Object value ) {
        if (value instanceof Number) {
            return value.toString();
        }
        return "'" + String.valueOf(value).replace("'", "''") + "'";
    }

    /**
     * Write the edited values of the chosen feature into the store of the layer and reload the
     * layer, so that the map shows them.
     */
    private void save() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        SimpleFeatureStore store = getFeatureStore();
        SimpleFeature feature = chosenFeature();
        if (store == null || feature == null) {
            return;
        }
        List<String> names = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for( int row = 0; row < shownNames.size(); row++ ) {
            String text = String.valueOf(model.getValueAt(row, 1));
            if (text.equals(shownValues.get(row))) {
                continue;
            }
            String name = shownNames.get(row);
            Class< ? > binding = feature.getFeatureType().getDescriptor(name).getType().getBinding();
            Object value = null;
            if (!text.isBlank()) {
                value = Converters.convert(text.trim(), binding);
                if (value == null) {
                    GuiUtilities.showWarningMessage(table, "WARNING",
                            "The value of " + name + " is not a valid " + binding.getSimpleName() + ": " + text);
                    return;
                }
            }
            names.add(name);
            values.add(value);
        }
        if (names.isEmpty()) {
            return;
        }

        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Filter idFilter = ff.id(Collections.singleton(ff.featureId(feature.getID())));
        try (Transaction transaction = new DefaultTransaction("edit attributes")) {
            store.setTransaction(transaction);
            try {
                store.modifyFeatures(names.toArray(new String[0]), values.toArray(), idFilter);
                transaction.commit();
            } catch (Exception e) {
                transaction.rollback();
                throw e;
            } finally {
                store.setTransaction(Transaction.AUTO_COMMIT);
            }
        } catch (Exception e) {
            e.printStackTrace();
            GuiUtilities.showWarningMessage(table, "WARNING", "Unable to save the values: " + e.getMessage());
            return;
        }

        try {
            // the selection follows the layer read again, with the saved values
            refreshLayer();
            // the replaced current layer selects its own tab
            context.openTool(this);
        } catch (Exception e) {
            e.printStackTrace();
            GuiUtilities.showWarningMessage(table, "WARNING",
                    "The values are saved, but the layer could not be read again: " + e.getMessage());
        }
    }

    /**
     * Draw the layer again with the saved values: a layer with a source is read again, as for a
     * filter, since its style may depend on them.
     */
    private void refreshLayer() throws Exception {
        Layer layer = getLayer();
        LayerSource source = LayerSource.of(layer);
        if (source != null && (source.isShapefile() || source.isGeopackageTable())) {
            Layer replacement = context.getLayerLoader().reloadVectorLayer(layer, source.filter());
            context.replaceLayer(layer, replacement);
        } else if (layer instanceof NwwEditableVectorLayer) {
            ((NwwEditableVectorLayer) layer).reload();
            context.setSelection(selection.onReplacement(layer));
        }
    }

    /**
     * Draw the selected features over the map, the chosen one stronger, or remove the highlight.
     */
    private void highlight() {
        if (context == null) {
            return;
        }
        highlightLayer.removeAllRenderables();
        LayerList layers = context.getNwwPanel().getWwd().getModel().getLayers();
        List<SimpleFeature> features = getFeatures();
        if (features.isEmpty()) {
            layers.remove(highlightLayer);
            context.getNwwPanel().getWwd().redraw();
            return;
        }
        SimpleFeature chosen = chosenFeature();
        int count = 0;
        for( SimpleFeature feature : features ) {
            if (feature != chosen && count++ < MAX_HIGHLIGHTED) {
                addHighlight(feature, SELECTED_COLOR, 2);
            }
        }
        // drawn last, over the others
        addHighlight(chosen, CHOSEN_COLOR, 3);
        // always on top of the other layers
        layers.remove(highlightLayer);
        layers.add(highlightLayer);
        context.getNwwPanel().getWwd().redraw();
    }

    private void addHighlight( SimpleFeature feature, Color color, double width ) {
        Geometry geometry = feature == null ? null : (Geometry) feature.getDefaultGeometry();
        if (geometry == null) {
            return;
        }
        ShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setOutlineMaterial(new Material(color));
        attributes.setOutlineWidth(width);
        attributes.setInteriorMaterial(new Material(color));
        attributes.setInteriorOpacity(0.2);
        PointPlacemarkAttributes pointAttributes = new PointPlacemarkAttributes();
        pointAttributes.setUsePointAsDefaultImage(true);
        pointAttributes.setLineMaterial(new Material(color));
        pointAttributes.setScale(width * 4);

        for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
            Geometry part = geometry.getGeometryN(i);
            if (part instanceof org.locationtech.jts.geom.Polygon) {
                org.locationtech.jts.geom.Polygon polygon = (org.locationtech.jts.geom.Polygon) part;
                SurfacePolygon surface = new SurfacePolygon(attributes, positions(polygon.getExteriorRing()));
                for( int k = 0; k < polygon.getNumInteriorRing(); k++ ) {
                    surface.addInnerBoundary(positions(polygon.getInteriorRingN(k)));
                }
                highlightLayer.addRenderable(surface);
            } else if (part instanceof LineString) {
                highlightLayer.addRenderable(new SurfacePolyline(attributes, positions((LineString) part)));
            } else {
                Coordinate c = part.getCoordinate();
                if (c != null) {
                    PointPlacemark placemark = new PointPlacemark(Position.fromDegrees(c.y, c.x, 0));
                    placemark.setAltitudeMode(WorldWind.CLAMP_TO_GROUND);
                    placemark.setAttributes(pointAttributes);
                    highlightLayer.addRenderable(placemark);
                }
            }
        }
    }

    private static List<Position> positions( LineString line ) {
        Coordinate[] coordinates = line.getCoordinates();
        List<Position> positions = new ArrayList<>(coordinates.length);
        for( Coordinate c : coordinates ) {
            positions.add(Position.fromDegrees(c.y, c.x, 0));
        }
        return positions;
    }

    @Override
    public void onTabClosed() {
        // closing the tab ends the selection
        context.clearSelection();
    }

    @Override
    public void onClose() {
        selection = null;
        highlight();
    }
}
