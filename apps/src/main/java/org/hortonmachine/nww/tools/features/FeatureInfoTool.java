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
import java.awt.FlowLayout;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.Converters;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.layers.defaults.NwwEditableVectorLayer;
import org.hortonmachine.nww.layers.defaults.NwwFeatureLayer;
import org.hortonmachine.nww.tools.INwwTool;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.tools.NwwContext;
import org.locationtech.jts.geom.Coordinate;
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
import gov.nasa.worldwind.render.SurfacePolygon;
import gov.nasa.worldwind.render.SurfacePolyline;

/**
 * The tab with the features identified on the current layer, one at a time, the shown one
 * highlighted on the map. The attributes can be edited if the layer has a feature store, as
 * the shapefiles.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureInfoTool implements INwwTool {

    private static final String HINT = "<html>Choose the Identify mode at the side of the map and click a feature "
            + "of the current layer, the one selected in the layers panel.</html>";
    private static final Color HIGHLIGHT_COLOR = Color.YELLOW;

    private NwwContext context;

    private final JLabel infoLabel = new JLabel(HINT);
    private final JLabel countLabel = new JLabel();
    private final JButton previousButton = new JButton("<");
    private final JButton nextButton = new JButton(">");
    private final JButton saveButton = new JButton("Save");
    private final JButton revertButton = new JButton("Revert");
    private final JLabel editLabel = new JLabel();
    private final DefaultTableModel model = new DefaultTableModel(new Object[]{"Field", "Value"}, 0){
        private static final long serialVersionUID = 1L;
        @Override
        public boolean isCellEditable( int row, int column ) {
            return column == 1 && isEditable();
        }
    };
    private final JTable table = new JTable(model);
    /** The highlight of the shown feature, not in the layers panel. */
    private final RenderableLayer highlightLayer = new RenderableLayer();

    /** The layer of the features, <code>null</code> if none was identified. */
    private Layer layer;
    private Position position;
    private List<SimpleFeature> features = Collections.emptyList();
    private int index = 0;
    /** The values of the shown feature as shown in the table, to find the edited ones. */
    private final List<String> shownValues = new ArrayList<>();
    private final List<String> shownNames = new ArrayList<>();

    @Override
    public String getName() {
        return "Feature info";
    }

    @Override
    public ImageIcon getIcon() {
        return ImageCache.get(ImageCache.INFO16);
    }

    @Override
    public JComponent createPanel( NwwContext context ) {
        this.context = context;
        highlightLayer.setName("Identified feature");
        highlightLayer.setPickEnabled(false);

        previousButton.setToolTipText("The previous feature found at the clicked point");
        previousButton.addActionListener(e -> showFeature(index - 1));
        nextButton.setToolTipText("The next feature found at the clicked point");
        nextButton.addActionListener(e -> showFeature(index + 1));
        saveButton.setIcon(ImageCache.get(ImageCache.SAVE));
        saveButton.setToolTipText("Write the edited values into the data of the layer");
        saveButton.addActionListener(e -> save());
        revertButton.setToolTipText("Show the values of the data again, dropping the edits");
        revertButton.addActionListener(e -> showFeature(index));

        table.setFillsViewportHeight(true);
        table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);

        JPanel navigation = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        navigation.add(previousButton);
        navigation.add(countLabel);
        navigation.add(nextButton);
        JPanel north = new JPanel(new BorderLayout(0, 4));
        north.add(infoLabel, BorderLayout.CENTER);
        north.add(navigation, BorderLayout.SOUTH);

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
        showFeature(index);
        return panel;
    }

    /**
     * Show the features identified at a point of a layer, the first one highlighted.
     *
     * @param layer the layer.
     * @param position the clicked point.
     * @param features the features found there, possibly none.
     */
    public void showFeatures( Layer layer, Position position, List<SimpleFeature> features ) {
        this.layer = layer;
        this.position = position;
        this.features = new ArrayList<>(features);
        showFeature(0);
    }

    /**
     * Show a message instead of features, as when there is no layer to identify on.
     *
     * @param message the message.
     */
    public void showMessage( String message ) {
        layer = null;
        position = null;
        features = Collections.emptyList();
        showFeature(0);
        infoLabel.setText("<html>" + message + "</html>");
    }

    private boolean isEditable() {
        return getFeatureStore() != null;
    }

    private SimpleFeatureStore getFeatureStore() {
        return layer instanceof NwwFeatureLayer ? ((NwwFeatureLayer) layer).getFeatureStore() : null;
    }

    private void showFeature( int newIndex ) {
        if (table.isEditing()) {
            table.getCellEditor().cancelCellEditing();
        }
        index = features.isEmpty() ? 0 : Math.max(0, Math.min(newIndex, features.size() - 1));
        model.setRowCount(0);
        shownNames.clear();
        shownValues.clear();
        SimpleFeature feature = features.isEmpty() ? null : features.get(index);
        if (layer == null) {
            infoLabel.setText(HINT);
        } else {
            String where = position == null ? "" : String.format(" at %.6f, %.6f", position.getLongitude().degrees,
                    position.getLatitude().degrees);
            infoLabel.setText("<html><b>" + layer.getName() + "</b>: " + (features.isEmpty() ? "no feature" : features.size()
                    + (features.size() == 1 ? " feature" : " features")) + where + "</html>");
        }
        if (feature != null) {
            for( AttributeDescriptor descriptor : feature.getFeatureType().getAttributeDescriptors() ) {
                if (descriptor == feature.getFeatureType().getGeometryDescriptor()) {
                    continue;
                }
                String name = descriptor.getLocalName();
                Object value = feature.getAttribute(name);
                String text = value == null ? "" : value.toString();
                shownNames.add(name);
                shownValues.add(text);
                model.addRow(new Object[]{name, text});
            }
        }
        countLabel.setText(features.isEmpty() ? "" : (index + 1) + " / " + features.size());
        previousButton.setVisible(features.size() > 1);
        nextButton.setVisible(features.size() > 1);
        previousButton.setEnabled(index > 0);
        nextButton.setEnabled(index < features.size() - 1);
        boolean editable = feature != null && isEditable();
        saveButton.setEnabled(editable);
        revertButton.setEnabled(editable);
        editLabel.setText(feature == null || editable ? "" : "Read only: the layer has no editable source");
        highlight(feature);
    }

    /**
     * Write the edited values of the shown feature into the store of the layer and reload the
     * layer, so that the map shows them.
     */
    private void save() {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        SimpleFeatureStore store = getFeatureStore();
        if (store == null || features.isEmpty()) {
            return;
        }
        SimpleFeature feature = features.get(index);
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
            refreshLayer();
            // the replaced current layer selects its own tab
            context.openTool(this);
            SimpleFeature saved = readFeature(idFilter);
            if (saved != null) {
                features.set(index, saved);
            }
        } catch (Exception e) {
            e.printStackTrace();
            GuiUtilities.showWarningMessage(table, "WARNING",
                    "The values are saved, but the layer could not be read again: " + e.getMessage());
        }
        showFeature(index);
    }

    /**
     * Draw the layer again with the saved values: a layer with a source is read again, as for a
     * filter, since its style may depend on them.
     */
    private void refreshLayer() throws Exception {
        LayerSource source = LayerSource.of(layer);
        if (source != null && (source.isShapefile() || source.isGeopackageTable())) {
            Layer replacement = context.getLayerLoader().reloadVectorLayer(layer, source.filter());
            context.replaceLayer(layer, replacement);
            layer = replacement;
        } else if (layer instanceof NwwEditableVectorLayer) {
            ((NwwEditableVectorLayer) layer).reload();
        }
    }

    private SimpleFeature readFeature( Filter idFilter ) {
        if (!(layer instanceof NwwFeatureLayer)) {
            return null;
        }
        SimpleFeatureCollection collection = ((NwwFeatureLayer) layer).getFeatureCollection().subCollection(idFilter);
        try (SimpleFeatureIterator iterator = collection.features()) {
            return iterator.hasNext() ? iterator.next() : null;
        }
    }

    /**
     * Draw the geometry of a feature over the map, or remove the highlight.
     */
    private void highlight( SimpleFeature feature ) {
        if (context == null) {
            return;
        }
        highlightLayer.removeAllRenderables();
        LayerList layers = context.getNwwPanel().getWwd().getModel().getLayers();
        Geometry geometry = feature == null ? null : (Geometry) feature.getDefaultGeometry();
        if (geometry == null) {
            layers.remove(highlightLayer);
            context.getNwwPanel().getWwd().redraw();
            return;
        }

        BasicShapeAttributes attributes = new BasicShapeAttributes();
        attributes.setOutlineMaterial(new Material(HIGHLIGHT_COLOR));
        attributes.setOutlineWidth(3);
        attributes.setInteriorMaterial(new Material(HIGHLIGHT_COLOR));
        attributes.setInteriorOpacity(0.25);
        PointPlacemarkAttributes pointAttributes = new PointPlacemarkAttributes();
        pointAttributes.setUsePointAsDefaultImage(true);
        pointAttributes.setLineMaterial(new Material(HIGHLIGHT_COLOR));
        pointAttributes.setScale(14d);

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
        // always on top of the other layers
        layers.remove(highlightLayer);
        layers.add(highlightLayer);
        context.getNwwPanel().getWwd().redraw();
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
        highlight(null);
    }

    @Override
    public void onClose() {
        highlight(null);
    }
}
