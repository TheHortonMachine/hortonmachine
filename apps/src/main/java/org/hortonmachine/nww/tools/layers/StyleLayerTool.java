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
package org.hortonmachine.nww.tools.layers;

import static org.hortonmachine.gears.utils.style.StyleUtilities.ff;
import static org.hortonmachine.gears.utils.style.StyleUtilities.sf;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JColorChooser;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.Scrollable;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.style.Fill;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.LineSymbolizer;
import org.geotools.api.style.PointSymbolizer;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Stroke;
import org.geotools.api.style.Style;
import org.geotools.api.style.Symbolizer;
import org.geotools.api.style.TextSymbolizer;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.spatialite.SpatialDbsImportUtils;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.colors.ColorBrewer;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.style.LineSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.PointSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.PolygonSymbolizerWrapper;
import org.hortonmachine.gears.utils.style.RuleWrapper;
import org.hortonmachine.gears.utils.style.StyleRules;
import org.hortonmachine.gears.utils.style.StyleUtilities;
import org.hortonmachine.gears.utils.style.SymbolizerWrapper;
import org.hortonmachine.gears.utils.style.TextSymbolizerWrapper;
import org.hortonmachine.gui.utils.DefaultGuiBridgeImpl;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.nww.gui.ToolsPanelController;
import org.hortonmachine.nww.layers.defaults.NwwVectorLayer;
import org.hortonmachine.nww.layers.defaults.vector.RasterizedFeatureCollectionLayer;
import org.hortonmachine.nww.tools.INwwLayerTool;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.tools.NwwContext;
import org.hortonmachine.style.IStyleChangeListener;
import org.hortonmachine.style.LineSymbolizerController;
import org.hortonmachine.style.MainController;
import org.hortonmachine.style.PointMarkSymbolizerController;
import org.hortonmachine.style.PolygonSymbolizerController;
import org.hortonmachine.style.SimpleStyle;
import org.hortonmachine.style.TextSymbolizerController;

import gov.nasa.worldwind.layers.Layer;

/**
 * The style of the vector layers read from a shapefile or a geopackage table, shown in the style
 * tab: a single symbol for all the features, or one per value of a field, with labels.
 *
 * <p>The symbols and labels are edited with the panels of the style editor. Styles with other
 * rules, as ranges, are kept as they are and can be edited in the style editor.</p>
 *
 * <p>Apply draws the layer with the style, Save also writes it into the data, in the geopackage
 * table or in the .sld file next to the shapefile.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class StyleLayerTool implements INwwLayerTool {

    /** The most values that get a symbol of their own. */
    private static final int MAX_VALUES = 100;

    private static final String SINGLE = "Single symbol";
    private static final String UNIQUE = "Unique values";
    private static final String CUSTOM = "Custom style";

    private static final String PAIRED = "Paired";
    private static final String SET3 = "Set3";
    private static final String MAIN = "Main colors";

    @Override
    public String getName() {
        return "Style";
    }

    @Override
    public boolean canHandle( Layer layer ) {
        LayerSource source = LayerSource.of(layer);
        return source != null && (source.isShapefile() || source.isGeopackageTable())
                && (layer instanceof RasterizedFeatureCollectionLayer || layer instanceof NwwVectorLayer);
    }

    @Override
    public JComponent createPanel( NwwContext context, Layer layer ) {
        LayerSource source = LayerSource.of(layer);
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel reading = new JLabel("Reading " + source.path() + " ...");
        panel.add(reading, BorderLayout.NORTH);

        new Thread(() -> {
            try {
                DataInfo info = readDataInfo(source);
                SwingUtilities.invokeLater(() -> {
                    panel.removeAll();
                    panel.add(new StylePanel(context, layer, source, info), BorderLayout.CENTER);
                    panel.revalidate();
                    panel.repaint();
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> reading.setText("Unable to read " + source.path() + ": " + e.getMessage()));
            }
        }, "hm-style-layer-info").start();
        return panel;
    }

    /**
     * What the style needs of the data: the geometry type and the fields.
     */
    private record DataInfo( EGeometryType geometryType, List<String> fields ) {
    }

    /**
     * The values of a field, with their number of features.
     *
     * @param counts the number of features of each value, in the order of the values, the
     *          <code>null</code> value excluded.
     * @param nullCount the number of features without value.
     */
    private record ValueCounts( Map<Object, Long> counts, long nullCount ) {
    }

    private static DataInfo readDataInfo( LayerSource source ) throws Exception {
        SimpleFeatureType schema;
        if (source.isGeopackageTable()) {
            try (GeopackageCommonDb db = new GeopackageDb()) {
                db.open(source.file().getAbsolutePath());
                schema = SpatialDbsImportUtils.tableToFeatureFCollection(db, SqlName.m(source.table()), 1, -1, null).getSchema();
            }
            if (schema == null) {
                throw new IllegalArgumentException("the table has no features");
            }
        } else {
            FileDataStore store = FileDataStoreFinder.getDataStore(source.file());
            try {
                schema = store.getSchema();
            } finally {
                store.dispose();
            }
        }
        GeometryDescriptor geometryDescriptor = schema.getGeometryDescriptor();
        EGeometryType geometryType;
        if (EGeometryType.isPoint(geometryDescriptor)) {
            geometryType = EGeometryType.POINT;
        } else if (EGeometryType.isLine(geometryDescriptor)) {
            geometryType = EGeometryType.LINESTRING;
        } else if (EGeometryType.isPolygon(geometryDescriptor)) {
            geometryType = EGeometryType.POLYGON;
        } else {
            throw new IllegalArgumentException("unsupported geometry type");
        }
        List<String> fields = new ArrayList<>();
        for( AttributeDescriptor descriptor : schema.getAttributeDescriptors() ) {
            if (!(descriptor instanceof GeometryDescriptor)) {
                fields.add(descriptor.getLocalName());
            }
        }
        return new DataInfo(geometryType, fields);
    }

    /**
     * Count the features of each value of a field, in all the data, also those hidden by a
     * filter, which may be shown later.
     */
    private static ValueCounts readValueCounts( LayerSource source, String field ) throws Exception {
        // the values sorted as numbers or texts, mixed types as texts
        Comparator<Object> comparator = ( a, b ) -> {
            if (a instanceof Number && b instanceof Number) {
                return Double.compare(((Number) a).doubleValue(), ((Number) b).doubleValue());
            }
            if (a instanceof Comparable && a.getClass().equals(b.getClass())) {
                @SuppressWarnings("unchecked")
                Comparable<Object> comparable = (Comparable<Object>) a;
                return comparable.compareTo(b);
            }
            return String.valueOf(a).compareTo(String.valueOf(b));
        };
        TreeMap<Object, Long> counts = new TreeMap<>(comparator);
        long nullCount = 0;
        if (source.isGeopackageTable()) {
            String column = "\"" + field.replace("\"", "\"\"") + "\"";
            String sql = "select " + column + ", count(*) from " + SqlName.m(source.table()).fixedDoubleName + " group by "
                    + column;
            try (GeopackageCommonDb db = new GeopackageDb()) {
                db.open(source.file().getAbsolutePath());
                nullCount = db.execOnConnection(connection -> {
                    long nulls = 0;
                    try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                        while( rs.next() ) {
                            Object value = rs.getObject(1);
                            if (value == null) {
                                nulls = rs.getLong(2);
                            } else {
                                counts.merge(value, rs.getLong(2), Long::sum);
                            }
                        }
                    }
                    return nulls;
                });
            }
        } else {
            FileDataStore store = FileDataStoreFinder.getDataStore(source.file());
            try (SimpleFeatureIterator iterator = store.getFeatureSource().getFeatures().features()) {
                while( iterator.hasNext() ) {
                    SimpleFeature feature = iterator.next();
                    Object value = feature.getAttribute(field);
                    if (value == null) {
                        nullCount++;
                    } else {
                        counts.merge(value, 1L, Long::sum);
                    }
                }
            } finally {
                store.dispose();
            }
        }
        return new ValueCounts(new LinkedHashMap<>(counts), nullCount);
    }

    /**
     * @return the style saved in the data, <code>null</code> if there is none.
     */
    private static Style readSavedStyle( LayerSource source ) throws Exception {
        if (source.isGeopackageTable()) {
            try (GeopackageCommonDb db = new GeopackageDb()) {
                db.open(source.file().getAbsolutePath());
                return SldUtilities.getStyleFromSldString(db.getSldString(SqlName.m(source.table())));
            }
        }
        return SldUtilities.getStyleFromFile(source.file());
    }

    private static Style copy( Style style ) {
        DuplicatingStyleVisitor visitor = new DuplicatingStyleVisitor();
        visitor.visit(style);
        return (Style) visitor.getCopy();
    }

    private static Color errorColor() {
        Color color = UIManager.getColor("Component.error.focusedBorderColor");
        return color != null ? color : Color.RED;
    }

    /**
     * The content of a vertical scroll pane, as wide as the tab: the panels of the style editor
     * are wider than the tab, they shrink instead of being cut.
     */
    private static class WidthTrackingPanel extends JPanel implements Scrollable {
        private static final long serialVersionUID = 1L;

        WidthTrackingPanel() {
            super(new BorderLayout());
        }

        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement( Rectangle visibleRect, int orientation, int direction ) {
            return 16;
        }

        @Override
        public int getScrollableBlockIncrement( Rectangle visibleRect, int orientation, int direction ) {
            return visibleRect.height;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        }

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        }
    }

    /**
     * A square of a color, for the table of the values.
     */
    private static class ColorIcon implements Icon {
        private final Color color;

        ColorIcon( Color color ) {
            this.color = color;
        }

        @Override
        public void paintIcon( Component c, Graphics g, int x, int y ) {
            g.setColor(color != null ? color : Color.LIGHT_GRAY);
            g.fillRect(x, y, getIconWidth(), getIconHeight());
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, getIconWidth() - 1, getIconHeight() - 1);
        }

        @Override
        public int getIconWidth() {
            return 28;
        }

        @Override
        public int getIconHeight() {
            return 14;
        }
    }

    /**
     * The panel of the style of a layer.
     */
    private static class StylePanel extends JPanel implements IStyleChangeListener {
        private static final long serialVersionUID = 1L;

        private final NwwContext context;
        /** The layer of the panel when it is created, it may be replaced at each change. */
        private final Layer layer;
        private final LayerSource source;
        private final DataInfo info;

        private StyleRules rules;
        /** The number of features of the values of the classified field, or <code>null</code>. */
        private ValueCounts valueCounts;
        /** If the components are being filled, so that their listeners do nothing. */
        private boolean updating;

        private final JComboBox<String> modeCombo = new JComboBox<>();
        private final CardLayout cards = new CardLayout();
        private final JPanel cardsPanel = new JPanel(cards);
        private final JPanel singleHolder = new JPanel(new BorderLayout());

        private final JComboBox<String> fieldCombo = new JComboBox<>();
        private final JComboBox<String> paletteCombo = new JComboBox<>(new String[]{PAIRED, SET3, MAIN});
        private final DefaultTableModel valuesModel = new DefaultTableModel(new String[]{"Color", "Value", "Features"}, 0){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable( int row, int column ) {
                return false;
            }
        };
        /** The rules of the rows of the values table. */
        private final List<Rule> rowRules = new ArrayList<>();
        private final JCheckBox otherCheck = new JCheckBox("Draw the other values");
        private final JButton otherEditButton = new JButton("Edit...");
        private final JLabel valuesStatus = new JLabel(" ");

        private final JCheckBox labelsCheck = new JCheckBox("Labels from");
        private final JComboBox<String> labelFieldCombo = new JComboBox<>();
        private final JSpinner labelSizeSpinner = new JSpinner(new SpinnerNumberModel(12, 4, 96, 1));
        private final JButton labelEditButton = new JButton("Edit...");

        private final JButton applyButton = new JButton("Apply");
        private final JButton saveButton = new JButton("Save");
        private final JButton resetButton = new JButton("Reset");
        private final JLabel statusLabel = new JLabel(" ");

        StylePanel( NwwContext context, Layer layer, LayerSource source, DataInfo info ) {
            super(new BorderLayout(0, 6));
            this.context = context;
            this.layer = layer;
            this.source = source;
            this.info = info;
            for( String field : info.fields() ) {
                fieldCombo.addItem(field);
                labelFieldCombo.addItem(field);
            }

            JPanel modePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            modePanel.add(new JLabel("Style"));
            modePanel.add(modeCombo);
            modeCombo.addActionListener(e -> onModeChanged());

            cardsPanel.add(singleHolder, SINGLE);
            cardsPanel.add(uniquePanel(), UNIQUE);
            cardsPanel.add(customPanel(), CUSTOM);

            JPanel content = new JPanel();
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.add(cardsPanel);
            content.add(Box.createVerticalStrut(6));
            content.add(labelsPanel());
            JPanel contentHolder = new WidthTrackingPanel();
            contentHolder.add(content, BorderLayout.NORTH);
            JScrollPane scroll = new JScrollPane(contentHolder);
            scroll.setBorder(BorderFactory.createEmptyBorder());
            scroll.getVerticalScrollBar().setUnitIncrement(16);

            add(modePanel, BorderLayout.NORTH);
            add(scroll, BorderLayout.CENTER);
            add(buttonsPanel(), BorderLayout.SOUTH);

            setRules(StyleRules.read(initialStyle()));
            if (rules.getKind() == StyleRules.Kind.UNIQUE_VALUES) {
                loadValueCounts(rules.getField(), false);
            }
        }

        /**
         * @return the style the layer is drawn with, made from its simple style for the 3D layers
         *          without SLD style.
         */
        private Style initialStyle() {
            Style style = ToolsPanelController.getSldStyle(layer);
            if (style != null) {
                return copy(style);
            }
            Rule rule = StyleUtilities.createDefaultRule(info.geometryType());
            if (layer instanceof NwwVectorLayer) {
                SimpleStyle simpleStyle = ((NwwVectorLayer) layer).getStyle();
                for( Symbolizer symbolizer : rule.symbolizers() ) {
                    if (symbolizer instanceof PolygonSymbolizer) {
                        PolygonSymbolizer polygonSymbolizer = (PolygonSymbolizer) symbolizer;
                        setFill(polygonSymbolizer.getFill(), simpleStyle.fillColor, simpleStyle.fillOpacity);
                        setStroke(polygonSymbolizer.getStroke(), simpleStyle.strokeColor, simpleStyle.strokeWidth);
                    } else if (symbolizer instanceof LineSymbolizer) {
                        setStroke(((LineSymbolizer) symbolizer).getStroke(), simpleStyle.strokeColor, simpleStyle.strokeWidth);
                    } else if (symbolizer instanceof PointSymbolizer) {
                        StyleUtilities.setRuleColor(rule, simpleStyle.fillColor);
                        Graphic graphic = ((PointSymbolizer) symbolizer).getGraphic();
                        if (graphic != null) {
                            graphic.setSize(ff.literal(simpleStyle.shapeSize));
                        }
                    }
                }
            }
            return StyleUtilities.rulesToStyle(List.of(rule), layer.getName(), false);
        }

        private static void setFill( Fill fill, Color color, double opacity ) {
            if (fill != null && color != null) {
                fill.setColor(ff.literal(ColorUtilities.asHex(color)));
                fill.setOpacity(ff.literal(opacity));
            }
        }

        private static void setStroke( Stroke stroke, Color color, double width ) {
            if (stroke != null && color != null) {
                stroke.setColor(ff.literal(ColorUtilities.asHex(color)));
                stroke.setWidth(ff.literal(width));
            }
        }

        /**
         * Show the rules in the panel.
         */
        private void setRules( StyleRules newRules ) {
            rules = newRules;
            updating = true;
            try {
                modeCombo.removeAllItems();
                if (rules.getKind() == StyleRules.Kind.CUSTOM) {
                    modeCombo.addItem(CUSTOM);
                }
                modeCombo.addItem(SINGLE);
                modeCombo.addItem(UNIQUE);
                switch( rules.getKind() ) {
                case SINGLE:
                    modeCombo.setSelectedItem(SINGLE);
                    singleHolder.removeAll();
                    singleHolder.add(symbolizerPanel(rules.getSingleRule(), false), BorderLayout.CENTER);
                    cards.show(cardsPanel, SINGLE);
                    break;
                case UNIQUE_VALUES:
                    modeCombo.setSelectedItem(UNIQUE);
                    if (!hasItem(fieldCombo, rules.getField())) {
                        fieldCombo.addItem(rules.getField());
                    }
                    fieldCombo.setSelectedItem(rules.getField());
                    otherCheck.setSelected(rules.getOtherRule() != null);
                    fillValuesTable();
                    cards.show(cardsPanel, UNIQUE);
                    break;
                default:
                    modeCombo.setSelectedItem(CUSTOM);
                    cards.show(cardsPanel, CUSTOM);
                    break;
                }

                TextSymbolizer label = rules.getKind() == StyleRules.Kind.CUSTOM ? null : rules.getLabel();
                labelsCheck.setSelected(label != null);
                showLabel(label);
                boolean labelsEditable = rules.getKind() != StyleRules.Kind.CUSTOM;
                labelsCheck.setEnabled(labelsEditable);
                labelFieldCombo.setEnabled(labelsEditable && label != null);
                labelSizeSpinner.setEnabled(labelsEditable && label != null);
                labelEditButton.setEnabled(labelsEditable && label != null);
            } finally {
                updating = false;
            }
            revalidate();
            repaint();
        }

        private static boolean hasItem( JComboBox<String> combo, String item ) {
            for( int i = 0; i < combo.getItemCount(); i++ ) {
                if (combo.getItemAt(i).equals(item)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Show the field and size of a label, to be called while updating.
         */
        private void showLabel( TextSymbolizer label ) {
            String labelField = labelField(label);
            if (labelField != null) {
                labelFieldCombo.setSelectedItem(labelField);
            }
            if (label != null) {
                labelSizeSpinner.setValue(labelSize(label));
            }
        }

        /**
         * @return the font size of a label, 12 if not given or given by an expression.
         */
        private static int labelSize( TextSymbolizer label ) {
            org.geotools.api.style.Font font = label.getFont();
            if (font != null && font.getSize() != null) {
                try {
                    Double size = font.getSize().evaluate(null, Double.class);
                    if (size != null) {
                        return (int) Math.round(size);
                    }
                } catch (Exception e) {
                    // not a number
                }
            }
            return 12;
        }

        private static void setLabelSize( TextSymbolizer label, int size ) {
            org.geotools.api.style.Font font = label.getFont();
            if (font == null) {
                label.setFont(StyleUtilities.sb.createFont("SansSerif", size));
            } else {
                font.setSize(ff.literal(size));
            }
        }

        private static String labelField( TextSymbolizer label ) {
            if (label == null) {
                return null;
            }
            Expression expression = label.getLabel();
            return expression instanceof PropertyName ? ((PropertyName) expression).getPropertyName() : null;
        }

        /**
         * @return the rule used as model of a new style: the current symbol, if there is one.
         */
        private Rule templateRule() {
            if (rules != null) {
                if (rules.getKind() == StyleRules.Kind.SINGLE) {
                    return rules.getSingleRule();
                } else if (rules.getKind() == StyleRules.Kind.UNIQUE_VALUES) {
                    if (rules.getOtherRule() != null) {
                        return rules.getOtherRule();
                    } else if (!rules.getValueRules().isEmpty()) {
                        return rules.getValueRules().values().iterator().next();
                    }
                }
            }
            return StyleUtilities.createDefaultRule(info.geometryType());
        }

        /**
         * @return the label to keep in a new style.
         */
        private TextSymbolizer currentLabel() {
            return rules != null && rules.getKind() != StyleRules.Kind.CUSTOM ? rules.getLabel() : null;
        }

        private void onModeChanged() {
            if (updating) {
                return;
            }
            Object mode = modeCombo.getSelectedItem();
            if (SINGLE.equals(mode) && rules.getKind() != StyleRules.Kind.SINGLE) {
                TextSymbolizer label = currentLabel();
                StyleRules single = StyleRules.single(templateRule());
                single.setLabel(label);
                setRules(single);
            } else if (UNIQUE.equals(mode) && rules.getKind() != StyleRules.Kind.UNIQUE_VALUES) {
                if (fieldCombo.getItemCount() == 0) {
                    statusLabel.setText("The data have no fields to classify.");
                    updating = true;
                    modeCombo.setSelectedItem(rules.getKind() == StyleRules.Kind.SINGLE ? SINGLE : CUSTOM);
                    updating = false;
                    return;
                }
                classify();
            }
        }

        /**
         * Make a rule for each value of the selected field.
         */
        private void classify() {
            String field = (String) fieldCombo.getSelectedItem();
            if (field != null) {
                loadValueCounts(field, true);
            }
        }

        /**
         * Count the features of the values of a field.
         *
         * @param field the field.
         * @param classify if <code>true</code>, the style gets a rule for each value, else only
         *          the counts are shown.
         */
        private void loadValueCounts( String field, boolean classify ) {
            valuesStatus.setText("Reading the values of " + field + "...");
            new Thread(() -> {
                try {
                    ValueCounts counts = readValueCounts(source, field);
                    SwingUtilities.invokeLater(() -> {
                        valueCounts = counts;
                        if (classify) {
                            List<Object> values = new ArrayList<>(counts.counts().keySet());
                            boolean tooMany = values.size() > MAX_VALUES;
                            List<Object> classified = tooMany ? values.subList(0, MAX_VALUES) : values;
                            TextSymbolizer label = currentLabel();
                            StyleRules unique = StyleRules.uniqueValues(templateRule(), field, classified,
                                    palette(classified.size()), otherCheck.isSelected() || tooMany || counts.nullCount() > 0);
                            unique.setLabel(label);
                            setRules(unique);
                        } else {
                            fillValuesTable();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        valuesStatus.setForeground(errorColor());
                        valuesStatus.setText("Unable to read the values: " + e.getMessage());
                    });
                }
            }, "hm-style-values").start();
        }

        private Color[] palette( int count ) {
            int size = Math.max(1, count);
            Object palette = paletteCombo.getSelectedItem();
            if (SET3.equals(palette)) {
                return ColorBrewer.getSet3Colors(size);
            } else if (MAIN.equals(palette)) {
                return ColorBrewer.getMainColors(size);
            }
            return ColorBrewer.getPairedColors(size);
        }

        private void fillValuesTable() {
            valuesModel.setRowCount(0);
            rowRules.clear();
            if (rules.getKind() != StyleRules.Kind.UNIQUE_VALUES) {
                return;
            }
            // the counts by text, since the values read from a style file are texts
            Map<String, Long> countsByText = new LinkedHashMap<>();
            if (valueCounts != null) {
                for( Map.Entry<Object, Long> entry : valueCounts.counts().entrySet() ) {
                    countsByText.put(String.valueOf(entry.getKey()), entry.getValue());
                }
            }
            long otherCount = 0;
            if (valueCounts != null) {
                otherCount = valueCounts.nullCount();
                for( Map.Entry<String, Long> entry : countsByText.entrySet() ) {
                    boolean hasRule = false;
                    for( Object value : rules.getValueRules().keySet() ) {
                        if (String.valueOf(value).equals(entry.getKey())) {
                            hasRule = true;
                            break;
                        }
                    }
                    if (!hasRule) {
                        otherCount += entry.getValue();
                    }
                }
            }
            for( Map.Entry<Object, Rule> entry : rules.getValueRules().entrySet() ) {
                Long count = countsByText.get(String.valueOf(entry.getKey()));
                valuesModel.addRow(new Object[]{new ColorIcon(StyleUtilities.getRuleColor(entry.getValue())),
                        String.valueOf(entry.getKey()), count != null ? String.format(Locale.ROOT, "%,d", count) : ""});
                rowRules.add(entry.getValue());
            }
            otherEditButton.setEnabled(rules.getOtherRule() != null);
            valuesStatus.setForeground(UIManager.getColor("Label.foreground"));
            String text = String.format(Locale.ROOT, "%d values", rules.getValueRules().size());
            if (valueCounts != null && otherCount > 0) {
                text += String.format(Locale.ROOT, ", %,d features with other or no value%s", otherCount,
                        rules.getOtherRule() != null ? "" : ", not drawn");
            }
            valuesStatus.setText(text);
        }

        private JPanel uniquePanel() {
            JButton classifyButton = new JButton("Classify");
            classifyButton.setToolTipText("Give a color to each value of the field, at most " + MAX_VALUES);
            classifyButton.addActionListener(e -> classify());
            fieldCombo.addActionListener(e -> {
                if (!updating && UNIQUE.equals(modeCombo.getSelectedItem())) {
                    classify();
                }
            });
            paletteCombo.addActionListener(e -> {
                if (updating || rules.getKind() != StyleRules.Kind.UNIQUE_VALUES) {
                    return;
                }
                Color[] colors = palette(rowRules.size());
                for( int i = 0; i < rowRules.size(); i++ ) {
                    StyleUtilities.setRuleColor(rowRules.get(i), colors[i % colors.length]);
                }
                fillValuesTable();
            });
            otherCheck.addActionListener(e -> {
                if (updating || rules.getKind() != StyleRules.Kind.UNIQUE_VALUES) {
                    return;
                }
                rules.setOtherRule(otherCheck.isSelected() ? StyleUtilities.createDefaultRule(info.geometryType()) : null);
                fillValuesTable();
            });
            otherEditButton.addActionListener(e -> {
                if (rules.getOtherRule() != null) {
                    openRuleDialog(rules.getOtherRule(), "Other values");
                }
            });

            JTable table = new JTable(valuesModel);
            table.setRowHeight(Math.max(table.getRowHeight(), 18));
            table.getColumnModel().getColumn(0).setMaxWidth(60);
            table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer(){
                private static final long serialVersionUID = 1L;

                @Override
                public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected,
                        boolean hasFocus, int row, int column ) {
                    super.getTableCellRendererComponent(table, "", isSelected, hasFocus, row, column);
                    setIcon((Icon) value);
                    setHorizontalAlignment(CENTER);
                    return this;
                }
            });
            table.setToolTipText("Click a color to change it, double click a value to edit its symbol");
            table.addMouseListener(new MouseAdapter(){
                @Override
                public void mouseClicked( MouseEvent e ) {
                    int row = table.rowAtPoint(e.getPoint());
                    int column = table.columnAtPoint(e.getPoint());
                    if (row < 0 || row >= rowRules.size()) {
                        return;
                    }
                    Rule rule = rowRules.get(row);
                    if (column == 0 && e.getClickCount() == 1) {
                        Color color = JColorChooser.showDialog(table, "Color of " + valuesModel.getValueAt(row, 1),
                                StyleUtilities.getRuleColor(rule));
                        if (color != null) {
                            StyleUtilities.setRuleColor(rule, color);
                            fillValuesTable();
                        }
                    } else if (column > 0 && e.getClickCount() == 2) {
                        openRuleDialog(rule, String.valueOf(valuesModel.getValueAt(row, 1)));
                    }
                }
            });
            JScrollPane tableScroll = new JScrollPane(table);
            tableScroll.setPreferredSize(new Dimension(300, 220));

            JPanel panel = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(2, 4, 2, 4);
            c.anchor = GridBagConstraints.WEST;
            c.gridy = 0;
            c.gridx = 0;
            panel.add(new JLabel("Field"), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(fieldCombo, c);
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            panel.add(classifyButton, c);
            c.gridy = 1;
            c.gridx = 0;
            panel.add(new JLabel("Palette"), c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            panel.add(paletteCombo, c);
            c.gridy = 2;
            c.gridx = 0;
            c.gridwidth = 3;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            panel.add(tableScroll, c);
            c.gridy = 3;
            c.weighty = 0;
            c.fill = GridBagConstraints.HORIZONTAL;
            JPanel otherPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            otherPanel.add(otherCheck);
            otherPanel.add(otherEditButton);
            panel.add(otherPanel, c);
            c.gridy = 4;
            panel.add(valuesStatus, c);
            panel.setBorder(BorderFactory.createTitledBorder("Unique values"));
            return panel;
        }

        private JPanel customPanel() {
            JLabel text = new JLabel("<html>The style has rules that can't be edited here, as ranges, scale limits or "
                    + "several symbols per rule. It is drawn as it is. Edit it in the style editor, save it there and "
                    + "press Reset to use it, or start a new style.</html>");
            JButton editorButton = new JButton("Open in the style editor");
            editorButton.addActionListener(e -> openStyleEditor());
            JButton singleButton = new JButton("New single symbol style");
            singleButton.addActionListener(e -> modeCombo.setSelectedItem(SINGLE));
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            buttons.add(editorButton);
            buttons.add(singleButton);
            JPanel panel = new JPanel(new BorderLayout(0, 6));
            panel.setBorder(BorderFactory.createTitledBorder("Custom style"));
            panel.add(text, BorderLayout.CENTER);
            panel.add(buttons, BorderLayout.SOUTH);
            return panel;
        }

        private JPanel labelsPanel() {
            labelsCheck.addActionListener(e -> {
                if (updating || rules.getKind() == StyleRules.Kind.CUSTOM) {
                    return;
                }
                if (labelsCheck.isSelected()) {
                    String field = (String) labelFieldCombo.getSelectedItem();
                    if (field == null) {
                        labelsCheck.setSelected(false);
                        return;
                    }
                    TextSymbolizer label = StyleUtilities.createDefaultTextSymbolizer(field, info.geometryType());
                    setLabelSize(label, ((Number) labelSizeSpinner.getValue()).intValue());
                    rules.setLabel(label);
                } else {
                    rules.setLabel(null);
                }
                labelFieldCombo.setEnabled(labelsCheck.isSelected());
                labelSizeSpinner.setEnabled(labelsCheck.isSelected());
                labelEditButton.setEnabled(labelsCheck.isSelected());
            });
            labelSizeSpinner.setToolTipText("The size of the labels");
            labelSizeSpinner.addChangeListener(e -> {
                TextSymbolizer label = currentLabel();
                if (!updating && label != null) {
                    setLabelSize(label, ((Number) labelSizeSpinner.getValue()).intValue());
                }
            });
            labelFieldCombo.addActionListener(e -> {
                TextSymbolizer label = currentLabel();
                String field = (String) labelFieldCombo.getSelectedItem();
                if (!updating && label != null && field != null) {
                    label.setLabel(ff.property(field));
                }
            });
            labelEditButton.addActionListener(e -> {
                TextSymbolizer label = currentLabel();
                if (label == null) {
                    return;
                }
                // the text panel needs a rule with the geometry symbolizer, to know the placement
                Rule rule = sf.createRule();
                for( Symbolizer symbolizer : templateRule().symbolizers() ) {
                    if (!(symbolizer instanceof TextSymbolizer)) {
                        rule.symbolizers().add(symbolizer);
                        break;
                    }
                }
                rule.symbolizers().add(label);
                TextSymbolizerWrapper wrapper = new RuleWrapper(rule, null).getTextSymbolizersWrapper();
                TextSymbolizerController controller = new TextSymbolizerController(wrapper, info.fields().toArray(new String[0]),
                        this);
                GuiUtilities.openDialogWithPanel(controller, "Labels of " + layer.getName(), null, false);
            });
            JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            panel.setBorder(BorderFactory.createTitledBorder("Labels"));
            panel.add(labelsCheck);
            panel.add(labelFieldCombo);
            panel.add(new JLabel("size"));
            panel.add(labelSizeSpinner);
            panel.add(labelEditButton);
            return panel;
        }

        private JPanel buttonsPanel() {
            applyButton.setToolTipText("Draw the layer with the style");
            saveButton.setToolTipText("Draw the layer with the style and save it in the data, used when they are loaded again");
            resetButton.setToolTipText("Go back to the saved style, or to a default style if none is saved");
            applyButton.addActionListener(e -> apply(false));
            saveButton.addActionListener(e -> apply(true));
            resetButton.addActionListener(e -> reset());
            JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
            buttons.add(applyButton);
            buttons.add(saveButton);
            buttons.add(resetButton);
            JPanel panel = new JPanel(new BorderLayout(0, 4));
            panel.add(buttons, BorderLayout.NORTH);
            panel.add(statusLabel, BorderLayout.SOUTH);
            return panel;
        }

        /**
         * @param rule the rule.
         * @param withApplyButton if <code>false</code>, the Apply button of the panel is hidden, as
         *          in the tab, which has its own.
         * @return the panel of the style editor for the symbol of a rule.
         */
        private JComponent symbolizerPanel( Rule rule, boolean withApplyButton ) {
            try {
                SymbolizerWrapper wrapper = new RuleWrapper(rule, null).getGeometrySymbolizersWrapper();
                if (wrapper instanceof PolygonSymbolizerWrapper) {
                    PolygonSymbolizerController controller = new PolygonSymbolizerController((PolygonSymbolizerWrapper) wrapper,
                            this);
                    controller.setApplyButtonVisible(withApplyButton);
                    return controller;
                } else if (wrapper instanceof LineSymbolizerWrapper) {
                    LineSymbolizerController controller = new LineSymbolizerController((LineSymbolizerWrapper) wrapper, this);
                    controller.setApplyButtonVisible(withApplyButton);
                    return controller;
                } else if (wrapper instanceof PointSymbolizerWrapper) {
                    PointMarkSymbolizerController controller = new PointMarkSymbolizerController((PointSymbolizerWrapper) wrapper,
                            this);
                    controller.setApplyButtonVisible(withApplyButton);
                    return controller;
                }
            } catch (Exception e) {
                // as for values the panels can't read, given by expressions
                e.printStackTrace();
            }
            return new JLabel("The symbol of the rule can't be edited here.");
        }

        private void openRuleDialog( Rule rule, String name ) {
            JPanel panel = new JPanel(new BorderLayout());
            panel.add(symbolizerPanel(rule, true), BorderLayout.CENTER);
            GuiUtilities.openDialogWithPanel(panel, "Symbol of " + name, null, false);
        }

        private void openStyleEditor() {
            try {
                MainController controller = new MainController(source.file(), source.table());
                JFrame frame = new DefaultGuiBridgeImpl().showWindow(controller.asJComponent(), "HortonMachine SLD Editor");
                GuiUtilities.setDefaultFrameIcon(frame);
                GuiUtilities.addClosingListener(frame, controller);
            } catch (Exception e) {
                e.printStackTrace();
                setStatus("Unable to open the style editor: " + e.getMessage(), true);
            }
        }

        /**
         * Called by the Apply buttons of the panels of the style editor.
         */
        @Override
        public void applyStyle() {
            // a symbol or label panel may have changed a color or the label field and size
            updating = true;
            try {
                showLabel(currentLabel());
            } finally {
                updating = false;
            }
            fillValuesTable();
            apply(false);
        }

        /**
         * Draw the layer with the style, in a new layer if it can't take it.
         *
         * @param save if <code>true</code>, the style is also saved in the data.
         */
        private void apply( boolean save ) {
            Style style = copy(rules.toStyle(layer.getName()));
            setButtonsEnabled(false);
            setStatus(save ? "Applying and saving..." : "Applying...", false);
            Layer current = context.getLatest(layer);
            new Thread(() -> {
                try {
                    Layer styled = context.getToolsPanel().restyleVectorLayer(current, style);
                    File savedTo = save ? ToolsPanelController.saveStyle(source, style) : null;
                    SwingUtilities.invokeLater(() -> {
                        if (styled != current) {
                            context.replaceLayer(current, styled);
                        }
                        setStatus(savedTo != null ? "Applied and saved in " + savedTo.getName() : "Applied", false);
                        setButtonsEnabled(true);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        setStatus("Unable to apply the style: " + e.getMessage(), true);
                        setButtonsEnabled(true);
                    });
                }
            }, "hm-style-apply").start();
        }

        /**
         * Go back to the saved style, else to a default one, and draw the layer with it.
         */
        private void reset() {
            setButtonsEnabled(false);
            new Thread(() -> {
                Style saved = null;
                try {
                    saved = readSavedStyle(source);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                Style style = saved != null ? saved
                        : StyleUtilities.rulesToStyle(List.of(StyleUtilities.createDefaultRule(info.geometryType())), layer.getName(),
                                false);
                boolean fromData = saved != null;
                SwingUtilities.invokeLater(() -> {
                    valueCounts = null;
                    setRules(StyleRules.read(style));
                    if (rules.getKind() == StyleRules.Kind.UNIQUE_VALUES) {
                        loadValueCounts(rules.getField(), false);
                    }
                    setButtonsEnabled(true);
                    apply(false);
                    if (!fromData) {
                        setStatus("No saved style, a default one is used", false);
                    }
                });
            }, "hm-style-reset").start();
        }

        private void setButtonsEnabled( boolean enabled ) {
            applyButton.setEnabled(enabled);
            saveButton.setEnabled(enabled);
            resetButton.setEnabled(enabled);
        }

        private void setStatus( String text, boolean error ) {
            statusLabel.setForeground(error ? errorColor() : UIManager.getColor("Label.foreground"));
            statusLabel.setText(text);
        }
    }
}
