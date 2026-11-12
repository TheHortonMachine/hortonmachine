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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.geotools.api.style.ColorMap;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.ShadedRelief;
import org.geotools.api.style.Style;
import org.geotools.api.style.Symbolizer;
import org.geotools.styling.ShadedReliefImpl;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.SldUtilities;
import org.hortonmachine.gears.utils.colors.DefaultTables;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities;
import org.hortonmachine.gears.utils.colors.RasterStyleUtilities.ValueClass;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.nww.gui.LocalStyleStore;
import org.hortonmachine.nww.gui.NwwLayerLoader;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer.RasterInfo;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer.RasterKind;
import org.hortonmachine.nww.layers.defaults.raster.TemporalRasterNwwLayer;
import org.hortonmachine.nww.tools.INwwLayerTool;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.tools.NwwContext;

import gov.nasa.worldwind.layers.Layer;

/**
 * The style of the raster layers read from a GeoTIFF or an ESRI ASCII grid, or from a remote
 * cloud optimized GeoTIFF, shown in the style tab: one of the colortables of the style editor, or
 * one written by hand, on a range of values, with an optional shaded relief. The styles of the
 * remote rasters are saved locally, in the {@link LocalStyleStore}.
 *
 * <p>Known issue: the tiles are drawn one by one, so the shaded relief of each is computed on its
 * own and shows dark borders.</p>
 *
 * <p>Apply draws the layer with the style, Save also writes it into the .sld file next to the
 * raster, used when it is loaded again. RGB and palette images have their own colors and are
 * drawn as they are. Of the rasters with more bands of values, as the classes of a land cover,
 * the colortable shows the chosen band.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class RasterStyleLayerTool implements INwwLayerTool {

    /** The item of a colortable written by hand or read from a style made elsewhere. */
    private static final String CUSTOM = "custom";

    /** The classification that spreads the colors on a range of values. */
    private static final String CONTINUOUS = "Continuous";
    /** The classification that gives each distinct value its color. */
    private static final String UNIQUE_VALUES = "Unique values";
    /** The most distinct values of a unique values classification. */
    private static final int MAX_UNIQUE_VALUES = 256;

    @Override
    public String getName() {
        return "Style";
    }

    @Override
    public boolean canHandle( Layer layer ) {
        if (layer instanceof TemporalRasterNwwLayer) {
            return true;
        }
        LayerSource source = LayerSource.of(layer);
        if (source == null || !(layer instanceof GridCoverageNwwLayer)) {
            return false;
        }
        if (((GridCoverageNwwLayer) layer).isRemote()) {
            // the remote rasters, as the cloud optimized GeoTIFFs of the STAC catalogs
            return source.isWebService();
        }
        return source.table() == null && NwwLayerLoader.isRasterFile(source.file());
    }

    @Override
    public JComponent createPanel( NwwContext context, Layer layer ) {
        LayerSource source = LayerSource.of(layer);
        // the series of rasters have no source, their structure is the one of their files
        TemporalRasterNwwLayer temporalLayer = layer instanceof TemporalRasterNwwLayer t ? t : null;
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel reading = new JLabel("Reading " + (temporalLayer != null ? temporalLayer.getFolder() : source.path()) + " ...");
        panel.add(reading, BorderLayout.NORTH);

        new Thread(() -> {
            RasterInfo readInfo;
            String error = null;
            try {
                if (temporalLayer != null) {
                    readInfo = GridCoverageNwwLayer.readRasterInfo(temporalLayer.getCurrentFile());
                } else if (((GridCoverageNwwLayer) layer).isRemote()) {
                    readInfo = GridCoverageNwwLayer.readRasterInfo(((GridCoverageNwwLayer) layer).getRemoteReader());
                } else {
                    readInfo = GridCoverageNwwLayer.readRasterInfo(source.file());
                }
            } catch (Exception e) {
                e.printStackTrace();
                readInfo = null;
                error = e.getMessage();
            }
            RasterInfo info = readInfo;
            String readError = error;
            SwingUtilities.invokeLater(() -> {
                panel.removeAll();
                if (info == null) {
                    JLabel label = new JLabel("<html>Unable to read the raster: " + readError + "</html>");
                    label.setForeground(errorColor());
                    panel.add(label, BorderLayout.NORTH);
                } else if (info.kind() == RasterKind.VALUES) {
                    panel.add(new RasterStylePanel(context, layer, source, info), BorderLayout.CENTER);
                } else {
                    String what = info.kind() == RasterKind.PALETTE ? "a palette image" : "an RGB image";
                    panel.add(new JLabel("<html>The raster has its own colors, as " + what + ": it is drawn "
                            + "as it is. Its opacity is set in the layers panel.</html>"), BorderLayout.NORTH);
                }
                panel.revalidate();
                panel.repaint();
            });
        }, "hm-raster-style-info").start();
        return panel;
    }

    private static Color errorColor() {
        Color color = UIManager.getColor("Component.error.focusedBorderColor");
        return color != null ? color : Color.RED;
    }

    /**
     * The colors of a colortable side by side, for the list of the colortables.
     */
    private static class GradientIcon implements Icon {
        private final List<Color> colors;

        GradientIcon( List<Color> colors ) {
            this.colors = colors;
        }

        @Override
        public void paintIcon( Component c, Graphics g, int x, int y ) {
            int width = getIconWidth();
            int height = getIconHeight();
            if (!colors.isEmpty()) {
                for( int i = 0; i < width; i++ ) {
                    g.setColor(colorAt(i / (double) (width - 1)));
                    g.drawLine(x + i, y, x + i, y + height - 1);
                }
            }
            g.setColor(Color.DARK_GRAY);
            g.drawRect(x, y, width - 1, height - 1);
        }

        /**
         * @return the color at a position from 0 to 1, interpolated between the colors.
         */
        private Color colorAt( double position ) {
            if (colors.size() == 1) {
                return colors.get(0);
            }
            double scaled = position * (colors.size() - 1);
            int index = Math.min((int) scaled, colors.size() - 2);
            double t = scaled - index;
            Color a = colors.get(index);
            Color b = colors.get(index + 1);
            return new Color((int) Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                    (int) Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                    (int) Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
        }

        @Override
        public int getIconWidth() {
            return 90;
        }

        @Override
        public int getIconHeight() {
            return 12;
        }
    }

    /**
     * The panel of the style of a raster of values, of one of its bands.
     */
    private static class RasterStylePanel extends JPanel {
        private static final long serialVersionUID = 1L;

        private final NwwContext context;
        /** The layer of the panel when it is created, it is replaced at each change. */
        private final Layer layer;
        /**
         * The source of the raster: a file, or a remote raster of a web service, <code>null</code>
         * for the series of rasters.
         */
        private final LayerSource source;
        /** If the raster is remote, as a cloud optimized GeoTIFF, read by the reader of the layer. */
        private final boolean remote;
        /**
         * The series of rasters, styled all together and not replaced, <code>null</code> for the
         * single rasters.
         */
        private final TemporalRasterNwwLayer temporal;
        /** The structure of the raster, with its bands. */
        private final RasterInfo info;

        /** The band the colortable shows, if the raster has more of them. */
        private final JComboBox<String> bandCombo = new JComboBox<>();

        /** If the components are being filled, so that their listeners do nothing. */
        private boolean updating;
        /** The opacity of the colors of the style, kept as read. */
        private double opacity = 1.0;
        /** The count of the reads of the range, of which only the last is shown. */
        private int rangeRequest;

        private final JComboBox<String> tableCombo = new JComboBox<>();
        private final JComboBox<String> classificationCombo = new JComboBox<>(new String[]{CONTINUOUS, UNIQUE_VALUES});
        private final JTextField minField = new JTextField(10);
        private final JTextField maxField = new JTextField(10);
        private final JButton fromDataButton = new JButton("From data");
        private final JButton readValuesButton = new JButton("Read values");
        private final JButton importButton = new JButton("Import...");
        private final JLabel rangeNote = new JLabel(" ");
        private final JTextArea tableArea = new JTextArea(8, 20);
        private final JCheckBox reliefCheck = new JCheckBox("Shaded relief, factor");
        private final JSpinner reliefSpinner = new JSpinner(new SpinnerNumberModel(1.0, 0.1, 100.0, 0.5));

        private final JButton applyButton = new JButton("Apply");
        private final JButton saveButton = new JButton("Save");
        private final JButton resetButton = new JButton("Reset");
        private final JLabel statusLabel = new JLabel(" ");

        RasterStylePanel( NwwContext context, Layer layer, LayerSource source, RasterInfo info ) {
            super(new BorderLayout(0, 6));
            this.context = context;
            this.layer = layer;
            this.source = source;
            this.temporal = layer instanceof TemporalRasterNwwLayer t ? t : null;
            this.remote = temporal == null && ((GridCoverageNwwLayer) layer).isRemote();
            this.info = info;

            for( String bandName : info.bandNames() ) {
                bandCombo.addItem(bandName);
            }
            bandCombo.setToolTipText("The band of the raster that the colortable shows");
            bandCombo.addActionListener(e -> onBandChanged());

            tableCombo.setRenderer(new DefaultListCellRenderer(){
                private static final long serialVersionUID = 1L;

                @Override
                public Component getListCellRendererComponent( JList< ? > list, Object value, int index, boolean isSelected,
                        boolean cellHasFocus ) {
                    super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                    String name = (String) value;
                    String table = CUSTOM.equals(name) ? tableArea.getText() : tableString(name);
                    setIcon(table == null ? null : new GradientIcon(previewColors(name, table)));
                    setIconTextGap(8);
                    return this;
                }
            });
            tableCombo.setMaximumRowCount(16);
            tableCombo.addActionListener(e -> onTableChanged());
            if (temporal != null) {
                fromDataButton.setToolTipText("Use the minimum and maximum of all the dates, reading all their cells, or, for the "
                        + "rasters larger than " + GridCoverageNwwLayer.EXACT_STATISTICS_MAX_CELLS / 1_000_000
                        + " million cells, at a low resolution");
            } else {
                fromDataButton.setToolTipText(remote
                        ? "Use the minimum and maximum of the raster, read at a low resolution, as from an overview"
                        : "Use the minimum and maximum of the raster, reading all its cells, or, for the rasters larger "
                                + "than " + GridCoverageNwwLayer.EXACT_STATISTICS_MAX_CELLS / 1_000_000
                                + " million cells, at a low resolution, as from an overview");
            }
            fromDataButton.addActionListener(e -> readRangeFromData());
            classificationCombo.setToolTipText("<html>Continuous: the colors spread on a range of values.<br>Unique values: "
                    + "each distinct value, as a class of a land cover, gets its own color, at most " + MAX_UNIQUE_VALUES
                    + " of them.</html>");
            classificationCombo.addActionListener(e -> onClassificationChanged());
            readValuesButton.setToolTipText("<html>Read the distinct values of the raster and give them the colors of the "
                    + "colortable.<br>The rasters larger than " + GridCoverageNwwLayer.EXACT_STATISTICS_MAX_CELLS / 1_000_000
                    + " million cells" + (remote ? " and the remote ones" : "")
                    + " are read at a low resolution:<br>the classes covering few cells can be missing.</html>");
            readValuesButton.addActionListener(e -> readUniqueValues(true));
            readValuesButton.setVisible(false);
            importButton.setToolTipText("<html>Read the classes from a CSV or a properties file, one per line, as<br>"
                    + "<code>value,label</code>, <code>value,r,g,b,label</code>, <code>value,label,#rrggbb</code> or "
                    + "<code>value=label</code>.<br>The classes without a color get the colors of the colortable.</html>");
            importButton.addActionListener(e -> importClasses());
            importButton.setVisible(false);
            reliefCheck.addActionListener(e -> reliefSpinner.setEnabled(reliefCheck.isSelected()));

            JPanel content = new JPanel(new GridBagLayout());
            GridBagConstraints c = new GridBagConstraints();
            c.insets = new Insets(2, 4, 2, 4);
            c.anchor = GridBagConstraints.WEST;
            c.gridy = 0;
            if (info.bandsCount() > 1) {
                c.gridx = 0;
                content.add(new JLabel("Band"), c);
                c.gridx = 1;
                c.gridwidth = 3;
                c.fill = GridBagConstraints.HORIZONTAL;
                c.weightx = 1;
                content.add(bandCombo, c);
                c.gridwidth = 1;
                c.weightx = 0;
                c.fill = GridBagConstraints.NONE;
            }
            c.gridy = 1;
            c.gridx = 0;
            content.add(new JLabel("Colortable"), c);
            c.gridx = 1;
            c.gridwidth = 3;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            content.add(tableCombo, c);
            c.gridwidth = 1;
            c.weightx = 0;
            c.gridy = 2;
            c.gridx = 0;
            c.fill = GridBagConstraints.NONE;
            content.add(new JLabel("Classes"), c);
            c.gridx = 1;
            c.gridwidth = 3;
            content.add(classificationCombo, c);
            c.gridwidth = 1;
            c.gridy = 3;
            c.gridx = 0;
            content.add(new JLabel("Min"), c);
            c.gridx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            content.add(minField, c);
            c.gridx = 2;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            content.add(new JLabel("Max"), c);
            c.gridx = 3;
            c.fill = GridBagConstraints.HORIZONTAL;
            c.weightx = 1;
            content.add(maxField, c);
            c.gridy = 4;
            c.gridx = 1;
            c.gridwidth = 3;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            rangePanel.add(fromDataButton);
            rangePanel.add(readValuesButton);
            rangePanel.add(importButton);
            rangePanel.add(rangeNote);
            content.add(rangePanel, c);
            c.gridy = 5;
            c.gridx = 0;
            c.gridwidth = 4;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            JScrollPane tableScroll = new JScrollPane(tableArea);
            tableScroll.setBorder(BorderFactory.createTitledBorder("Colors"));
            content.add(tableScroll, c);
            c.gridy = 6;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            JPanel reliefPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            reliefPanel.add(reliefCheck);
            reliefPanel.add(reliefSpinner);
            content.add(reliefPanel, c);

            add(content, BorderLayout.CENTER);
            add(buttonsPanel(), BorderLayout.SOUTH);

            showStyle(styleOf(layer));
        }

        /**
         * @return the style a raster layer or a series is drawn with.
         */
        private static Style styleOf( Layer layer ) {
            return layer instanceof TemporalRasterNwwLayer t ? t.getStyle() : ((GridCoverageNwwLayer) layer).getStyle();
        }

        /**
         * @return the colors of a colortable for its preview: the custom classes of a unique values
         *          style, with their labels, are read as classes.
         */
        private List<Color> previewColors( String name, String table ) {
            if (CUSTOM.equals(name) && isUniqueValues()) {
                try {
                    List<Color> colors = new ArrayList<>();
                    for( ValueClass valueClass : RasterStyleUtilities.parseValueClasses(table) ) {
                        if (valueClass.color() != null) {
                            colors.add(valueClass.color());
                        }
                    }
                    return colors;
                } catch (IllegalArgumentException e) {
                    // not classes
                }
            }
            return RasterStyleUtilities.getColortableColors(table);
        }

        private static String tableString( String name ) {
            return name == null ? null : new DefaultTables().getTableString(name);
        }

        /**
         * Fill the list of the colortables, with the custom item if needed.
         */
        private void fillTables( boolean withCustom ) {
            tableCombo.removeAllItems();
            if (withCustom) {
                tableCombo.addItem(CUSTOM);
            }
            String[] names = DefaultTables.getTableNames();
            Arrays.sort(names);
            for( String name : names ) {
                tableCombo.addItem(name);
            }
        }

        /**
         * Show a style in the panel: its colortable, if it is one of the known ones, else its
         * colors as a custom table, its range and its shaded relief.
         */
        private void showStyle( Style style ) {
            RasterSymbolizer symbolizer = null;
            String ruleName = null;
            if (style != null && !style.featureTypeStyles().isEmpty()) {
                for( Rule rule : style.featureTypeStyles().get(0).rules() ) {
                    for( Symbolizer s : rule.symbolizers() ) {
                        if (s instanceof RasterSymbolizer && symbolizer == null) {
                            symbolizer = (RasterSymbolizer) s;
                            ruleName = rule.getName();
                        }
                    }
                }
            }
            ColorMap colorMap = symbolizer == null ? null : symbolizer.getColorMap();
            boolean unique = RasterStyleUtilities.isUniqueValues(colorMap);
            String customTable;
            if (colorMap == null) {
                customTable = "";
            } else if (unique) {
                customTable = RasterStyleUtilities.toUniqueValuesString(RasterStyleUtilities.getValueClasses(colorMap));
            } else {
                customTable = RasterStyleUtilities.toColortableString(colorMap);
            }
            double[] range = colorMap == null || unique ? null : RasterStyleUtilities.getColorMapRange(colorMap);
            boolean known = ruleName != null && tableString(ruleName) != null;

            updating = true;
            try {
                int band = RasterStyleUtilities.getGrayChannel(style);
                bandCombo.setSelectedIndex(band < 0 || band >= info.bandsCount() ? 0 : band);
                classificationCombo.setSelectedItem(unique ? UNIQUE_VALUES : CONTINUOUS);
                if (unique) {
                    // the classes as they are, the colortable they took their colors from if known
                    fillTables(!known);
                    tableCombo.setSelectedItem(known ? ruleName : CUSTOM);
                    tableArea.setText(customTable);
                } else if (known) {
                    fillTables(false);
                    tableCombo.setSelectedItem(ruleName);
                    tableArea.setText(tableString(ruleName));
                } else if (!customTable.isEmpty()) {
                    fillTables(true);
                    tableCombo.setSelectedItem(CUSTOM);
                    tableArea.setText(customTable);
                } else {
                    // no colortable, as the default style: start from the elevation one
                    fillTables(false);
                    tableCombo.setSelectedItem(EColorTables.elev.name());
                    tableArea.setText(tableString(EColorTables.elev.name()));
                }
                tableArea.setCaretPosition(0);
                if (range != null) {
                    minField.setText(RasterLayerTool.numberText(range[0]));
                    maxField.setText(RasterLayerTool.numberText(range[1]));
                } else {
                    minField.setText("");
                    maxField.setText("");
                }

                Double symbolizerOpacity = symbolizer == null || symbolizer.getOpacity() == null
                        ? null
                        : symbolizer.getOpacity().evaluate(null, Double.class);
                opacity = symbolizerOpacity == null ? 1.0 : symbolizerOpacity;

                ShadedRelief relief = symbolizer == null ? null : symbolizer.getShadedRelief();
                Double factor = relief == null || relief.getReliefFactor() == null
                        ? null
                        : relief.getReliefFactor().evaluate(null, Double.class);
                reliefCheck.setSelected(relief != null);
                reliefSpinner.setValue(factor == null ? 1.0 : Math.max(0.1, Math.min(100, factor)));
                reliefSpinner.setEnabled(relief != null);
            } finally {
                updating = false;
            }
            updateRangeState();
            if (range == null && !hasValues()) {
                readRangeFromData();
            }
        }

        private boolean hasValues() {
            return isUniqueValues() || RasterStyleUtilities.colortableHasValues(tableArea.getText());
        }

        private boolean isUniqueValues() {
            return UNIQUE_VALUES.equals(classificationCombo.getSelectedItem());
        }

        /**
         * The range is used only by the continuous colortables without values.
         */
        private void updateRangeState() {
            boolean unique = isUniqueValues();
            boolean usesRange = !hasValues();
            minField.setEnabled(usesRange);
            maxField.setEnabled(usesRange);
            fromDataButton.setEnabled(usesRange);
            fromDataButton.setVisible(!unique);
            readValuesButton.setVisible(unique);
            importButton.setVisible(unique);
            if (unique) {
                rangeNote.setText(" ");
                tableArea.setToolTipText("<html>One class per line, as <code>value r g b label</code>, the label being "
                        + "optional.<br>The values not listed are not drawn.</html>");
            } else {
                rangeNote.setText(usesRange ? " " : "The colortable gives the values of its colors.");
                tableArea.setToolTipText("<html>One color per line, as <code>r g b</code>, spread on the range, or with its "
                        + "value, as <code>value r g b</code>.<br>Editing the colors makes a custom colortable.</html>");
            }
        }

        /**
         * Switching to unique values reads the values of the raster, back to continuous shows the
         * colortable again.
         */
        private void onClassificationChanged() {
            if (updating) {
                return;
            }
            String name = (String) tableCombo.getSelectedItem();
            if (isUniqueValues()) {
                updateRangeState();
                // the text is a continuous colortable, not classes with labels
                readUniqueValues(false);
                return;
            }
            if (name != null && !CUSTOM.equals(name)) {
                tableArea.setText(tableString(name));
                tableArea.setCaretPosition(0);
            } else {
                // the custom classes have no colortable to go back to: start from the elevation one
                updating = true;
                try {
                    fillTables(false);
                    tableCombo.setSelectedItem(EColorTables.elev.name());
                    tableArea.setText(tableString(EColorTables.elev.name()));
                    tableArea.setCaretPosition(0);
                } finally {
                    updating = false;
                }
            }
            updateRangeState();
            if (minField.getText().isBlank() || maxField.getText().isBlank()) {
                readRangeFromData();
            }
        }

        private void onTableChanged() {
            if (updating) {
                return;
            }
            String name = (String) tableCombo.getSelectedItem();
            if (isUniqueValues()) {
                // the same classes with the colors of the new colortable
                if (name != null && !CUSTOM.equals(name)) {
                    try {
                        List<ValueClass> classes = RasterStyleUtilities.parseValueClasses(tableArea.getText());
                        if (classes.isEmpty()) {
                            readUniqueValues(true);
                        } else {
                            showClasses(recolor(classes, true));
                        }
                    } catch (IllegalArgumentException e) {
                        setStatus(e.getMessage(), true);
                    }
                }
                return;
            }
            if (name != null && !CUSTOM.equals(name)) {
                tableArea.setText(tableString(name));
                tableArea.setCaretPosition(0);
            }
            updateRangeState();
            if (!hasValues() && (minField.getText().isBlank() || maxField.getText().isBlank())) {
                readRangeFromData();
            }
        }

        /**
         * A new band has a range of its own: it is read again.
         */
        private void onBandChanged() {
            if (updating) {
                return;
            }
            minField.setText("");
            maxField.setText("");
            if (isUniqueValues()) {
                readUniqueValues(true);
            } else if (!hasValues()) {
                readRangeFromData();
            }
        }

        /**
         * @return the colors of the colortable chosen, or, for the custom one, of the colors text.
         */
        private String colorsSource() {
            String name = (String) tableCombo.getSelectedItem();
            return name == null || CUSTOM.equals(name) ? tableArea.getText() : tableString(name);
        }

        /**
         * Give classes the colors of the colortable.
         *
         * @param all if <code>true</code> all the classes get new colors, else only the ones
         *          without a color.
         */
        private List<ValueClass> recolor( List<ValueClass> classes, boolean all ) {
            Color[] colors = RasterStyleUtilities.getClassColors(colorsSource(), classes.size());
            List<ValueClass> recolored = new ArrayList<>();
            for( int i = 0; i < classes.size(); i++ ) {
                ValueClass valueClass = classes.get(i);
                recolored.add(all || valueClass.color() == null ? valueClass.withColor(colors[i]) : valueClass);
            }
            return recolored;
        }

        private void showClasses( List<ValueClass> classes ) {
            tableArea.setText(RasterStyleUtilities.toUniqueValuesString(classes));
            tableArea.setCaretPosition(0);
        }

        /**
         * Put the distinct values of the band of the raster in the colors text, with the colors of
         * the colortable. The large and remote rasters are read at a low resolution.
         *
         * @param keepLabels if <code>true</code>, the values keep the labels of the classes of the
         *          colors text.
         */
        private void readUniqueValues( boolean keepLabels ) {
            readValuesButton.setEnabled(false);
            setStatus(temporal != null
                    ? "Reading the values of all the " + temporal.getFiles().size() + " dates, it can take a while..."
                    : "Reading the values...", false);
            int band = selectedBand();
            int request = ++rangeRequest;
            Layer current = context.getLatest(layer);
            new Thread(() -> {
                try {
                    double[] values;
                    boolean lowResolution;
                    if (temporal != null) {
                        values = TemporalRasterNwwLayer.readUniqueValues(temporal.getFiles(), band, MAX_UNIQUE_VALUES,
                                Integer.MAX_VALUE);
                        lowResolution = GridCoverageNwwLayer.isLargeRaster(temporal.getCurrentFile());
                    } else if (remote) {
                        values = GridCoverageNwwLayer.readLowResolutionUniqueValues(
                                ((GridCoverageNwwLayer) current).getRemoteReader(), band, MAX_UNIQUE_VALUES);
                        lowResolution = true;
                    } else {
                        lowResolution = GridCoverageNwwLayer.isLargeRaster(source.file());
                        values = GridCoverageNwwLayer.readUniqueValues(source.file(), band, MAX_UNIQUE_VALUES);
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (request != rangeRequest) {
                            return;
                        }
                        readValuesButton.setEnabled(true);
                        if (values.length == 0) {
                            setStatus("The raster has no valid values.", true);
                        } else if (values.length > MAX_UNIQUE_VALUES) {
                            setStatus("More than " + MAX_UNIQUE_VALUES + " distinct values: use a continuous colortable.",
                                    true);
                        } else {
                            showClasses(recolor(withKnownLabels(values, keepLabels), true));
                            setStatus(values.length + " values" + (lowResolution
                                    ? ", read at a low resolution: the classes covering few cells can be missing."
                                    : "."), false);
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        if (request != rangeRequest) {
                            return;
                        }
                        readValuesButton.setEnabled(true);
                        setStatus("Unable to read the values: " + e.getMessage(), true);
                    });
                }
            }, "hm-raster-style-values").start();
        }

        /**
         * @return the values as classes, optionally keeping the labels of the classes in the colors
         *          text.
         */
        private List<ValueClass> withKnownLabels( double[] values, boolean keepLabels ) {
            Map<Double, String> labels = new HashMap<>();
            if (keepLabels) {
                try {
                    for( ValueClass valueClass : RasterStyleUtilities.parseValueClasses(tableArea.getText()) ) {
                        labels.put(valueClass.value(), valueClass.label());
                    }
                } catch (IllegalArgumentException e) {
                    // not classes
                }
            }
            List<ValueClass> classes = new ArrayList<>();
            for( double value : values ) {
                classes.add(new ValueClass(value, null, labels.get(value)));
            }
            return classes;
        }

        /**
         * Read the classes from a CSV or properties file, the ones without a color get the colors of
         * the colortable.
         */
        private void importClasses() {
            JFileChooser chooser = new JFileChooser(source != null && source.file() != null ? source.file().getParentFile() : null);
            chooser.setFileFilter(new FileNameExtensionFilter("Classes (csv, txt, properties)", "csv", "txt", "properties"));
            if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
                return;
            }
            File file = chooser.getSelectedFile();
            try {
                List<ValueClass> classes = RasterStyleUtilities.parseValueClasses(FileUtilities.readFile(file));
                if (classes.isEmpty()) {
                    setStatus("No classes in " + file.getName(), true);
                    return;
                }
                showClasses(recolor(classes, false));
                setStatus(classes.size() + " classes read from " + file.getName(), false);
            } catch (Exception e) {
                setStatus("Unable to read the classes: " + e.getMessage(), true);
            }
        }

        /**
         * @return the 0 based index of the band the colortable shows.
         */
        private int selectedBand() {
            return Math.max(0, bandCombo.getSelectedIndex());
        }

        /**
         * Put the minimum and maximum of the band of the raster in the range, reading all its
         * cells, or, for the remote rasters, reading it at a low resolution.
         */
        private void readRangeFromData() {
            fromDataButton.setEnabled(false);
            setStatus(temporal != null
                    ? "Reading the range of the values of all the " + temporal.getFiles().size() + " dates, it can take a while..."
                    : "Reading the range of the values...", false);
            int band = selectedBand();
            // only the last read is shown, as when the band is changed while reading
            int request = ++rangeRequest;
            // the reader of the layer drawn now, the layer is replaced at each style
            Layer current = context.getLatest(layer);
            new Thread(() -> {
                try {
                    double[] stats;
                    boolean lowResolution;
                    String note = "Range read at a low resolution, the extremes can be a little wider.";
                    if (temporal != null) {
                        TemporalRasterNwwLayer.Range range = TemporalRasterNwwLayer.readRange(temporal.getFiles(), band,
                                Integer.MAX_VALUE);
                        stats = range == null ? null : new double[]{range.min(), range.max()};
                        // the note tells the dates read also for the small rasters, read exactly
                        lowResolution = true;
                        if (range != null) {
                            note = range.note();
                        }
                    } else if (remote) {
                        stats = GridCoverageNwwLayer.readLowResolutionStatistics(((GridCoverageNwwLayer) current).getRemoteReader(),
                                band);
                        lowResolution = true;
                    } else {
                        // the large rasters at a low resolution, reading all their cells would take long
                        lowResolution = GridCoverageNwwLayer.isLargeRaster(source.file());
                        stats = GridCoverageNwwLayer.readStatistics(source.file(), band);
                    }
                    String rangeNote = note;
                    SwingUtilities.invokeLater(() -> {
                        if (request != rangeRequest) {
                            return;
                        }
                        if (stats == null) {
                            setStatus("The raster has no valid values.", true);
                        } else {
                            minField.setText(RasterLayerTool.numberText(stats[0]));
                            maxField.setText(RasterLayerTool.numberText(stats[1]));
                            setStatus(lowResolution ? rangeNote : " ", false);
                        }
                        updateRangeState();
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        if (request != rangeRequest) {
                            return;
                        }
                        setStatus("Unable to read the values: " + e.getMessage(), true);
                        updateRangeState();
                    });
                }
            }, "hm-raster-style-range").start();
        }

        private JPanel buttonsPanel() {
            applyButton.setToolTipText("Draw the raster with the style");
            saveButton.setToolTipText(remote || temporal != null
                    ? "Draw the raster with the style and save it locally, used when it is loaded again"
                    : "Draw the raster with the style and save it in the .sld file next to it, used when it is loaded again");
            resetButton.setToolTipText("Go back to the saved style, or to the default style if none is saved");
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
         * @return the style of the panel.
         * @throws IllegalArgumentException with a message for the user, if a value is wrong.
         */
        private Style buildStyle() throws Exception {
            String table = tableArea.getText();
            String name = (String) tableCombo.getSelectedItem();
            if (isUniqueValues()) {
                List<ValueClass> classes = RasterStyleUtilities.parseValueClasses(table);
                if (classes.size() > MAX_UNIQUE_VALUES) {
                    throw new IllegalArgumentException("At most " + MAX_UNIQUE_VALUES + " unique values.");
                }
                Style style = RasterStyleUtilities.createStyleForUniqueValues(classes, opacity);
                Rule rule = style.featureTypeStyles().get(0).rules().get(0);
                // the colortable the colors came from, the classes are read from the color map
                rule.setName(name == null ? CUSTOM : name);
                if (reliefCheck.isSelected()) {
                    ShadedRelief relief = new ShadedReliefImpl();
                    relief.setBrightnessOnly(false);
                    relief.setReliefFactor(ff.literal(((Number) reliefSpinner.getValue()).doubleValue()));
                    ((RasterSymbolizer) rule.symbolizers().get(0)).setShadedRelief(relief);
                }
                if (info.bandsCount() > 1) {
                    RasterStyleUtilities.setGrayChannel(style, selectedBand());
                }
                return style;
            }
            if (name == null || CUSTOM.equals(name) || !table.trim().equals(tableString(name).trim())) {
                // edited colors
                name = CUSTOM;
            }
            double min = 0;
            double max = 1;
            if (!RasterStyleUtilities.colortableHasValues(table)) {
                try {
                    min = Double.parseDouble(minField.getText().trim());
                    max = Double.parseDouble(maxField.getText().trim());
                } catch (NumberFormatException e) {
                    throw new IllegalArgumentException("The min and max need to be numbers.");
                }
                if (max <= min) {
                    throw new IllegalArgumentException("The max needs to be greater than the min.");
                }
            }
            Style style = RasterStyleUtilities.createStyleForColortableString(table, min, max, null, opacity);
            Rule rule = style.featureTypeStyles().get(0).rules().get(0);
            // the name of the colortable, as the style editor does, to show it again
            rule.setName(name);
            if (reliefCheck.isSelected()) {
                ShadedRelief relief = new ShadedReliefImpl();
                relief.setBrightnessOnly(false);
                relief.setReliefFactor(ff.literal(((Number) reliefSpinner.getValue()).doubleValue()));
                ((RasterSymbolizer) rule.symbolizers().get(0)).setShadedRelief(relief);
            }
            if (info.bandsCount() > 1) {
                RasterStyleUtilities.setGrayChannel(style, selectedBand());
            }
            return style;
        }

        /**
         * Draw the raster with the style, in a new layer.
         *
         * @param save if <code>true</code>, the style is also saved in the .sld file.
         */
        private void apply( boolean save ) {
            Style style;
            try {
                style = buildStyle();
            } catch (Exception e) {
                setStatus(e.getMessage(), true);
                return;
            }
            if (CUSTOM.equals(style.featureTypeStyles().get(0).rules().get(0).getName())
                    && !CUSTOM.equals(tableCombo.getSelectedItem())) {
                updating = true;
                try {
                    String text = tableArea.getText();
                    fillTables(true);
                    tableCombo.setSelectedItem(CUSTOM);
                    tableArea.setText(text);
                } finally {
                    updating = false;
                }
            }
            restyle(style, save, save ? "Applying and saving..." : "Applying...", "Applied");
        }

        /**
         * Draw the raster again with a style, in a new layer.
         *
         * @param style the style, <code>null</code> for the saved or default one, then shown.
         * @param save if <code>true</code>, the style is also saved in the .sld file.
         * @param message the status while working.
         * @param doneMessage the status when done, if not saved.
         */
        private void restyle( Style style, boolean save, String message, String doneMessage ) {
            setButtonsEnabled(false);
            setStatus(message, false);
            if (temporal != null) {
                restyleTemporal(style, save, doneMessage);
                return;
            }
            Layer current = context.getLatest(layer);
            LayerSource source = LayerSource.of(current);
            new Thread(() -> {
                try {
                    Layer styled = context.getLayerLoader().restyleRasterLayer(current, style);
                    File savedTo = save ? NwwLayerLoader.saveStyle(source, style) : null;
                    SwingUtilities.invokeLater(() -> {
                        context.replaceLayer(current, styled);
                        if (style == null) {
                            showStyle(((GridCoverageNwwLayer) styled).getStyle());
                        }
                        setStatus(savedTo != null ? "Applied and saved in " + savedTo.getName() : doneMessage, false);
                        setButtonsEnabled(true);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        setStatus("Unable to apply the style: " + e.getMessage(), true);
                        setButtonsEnabled(true);
                    });
                }
            }, "hm-raster-style-apply").start();
        }

        /**
         * Draw all the dates of the series with a style: the layer is not replaced, it draws its
         * dates again. The styles of the series are saved locally.
         *
         * @param style the style, <code>null</code> for the saved or default one, then shown.
         */
        private void restyleTemporal( Style style, boolean save, String doneMessage ) {
            new Thread(() -> {
                try {
                    Style newStyle = style;
                    if (newStyle == null) {
                        newStyle = LocalStyleStore.readTemporal(temporal.getFolder());
                        if (newStyle == null) {
                            newStyle = TemporalRasterNwwLayer.createDefaultStyle(temporal.getFiles());
                        }
                    }
                    temporal.setStyle(newStyle);
                    File savedTo = save ? LocalStyleStore.saveTemporal(temporal.getFolder(), newStyle) : null;
                    SwingUtilities.invokeLater(() -> {
                        if (style == null) {
                            showStyle(temporal.getStyle());
                        }
                        setStatus(savedTo != null ? "Applied to all dates and saved locally" : doneMessage, false);
                        setButtonsEnabled(true);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        setStatus("Unable to apply the style: " + e.getMessage(), true);
                        setButtonsEnabled(true);
                    });
                }
            }, "hm-raster-style-apply").start();
        }

        /**
         * Go back to the style of the .sld file, else to the default one.
         */
        private void reset() {
            boolean saved;
            if (temporal != null) {
                saved = LocalStyleStore.getTemporalStyleFile(temporal.getFolder()).exists();
            } else {
                saved = remote ? LocalStyleStore.read(source) != null : SldUtilities.getStyleFile(source.file()) != null;
            }
            restyle(null, false, "Reading the saved style...",
                    saved ? "Back to the saved style" : "No saved style, the default one is used");
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
