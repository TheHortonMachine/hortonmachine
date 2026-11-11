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
import java.util.Arrays;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
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
import org.hortonmachine.nww.gui.LocalStyleStore;
import org.hortonmachine.nww.gui.NwwLayerLoader;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer.RasterInfo;
import org.hortonmachine.nww.layers.defaults.raster.GridCoverageNwwLayer.RasterKind;
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

    @Override
    public String getName() {
        return "Style";
    }

    @Override
    public boolean canHandle( Layer layer ) {
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
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel reading = new JLabel("Reading " + source.path() + " ...");
        panel.add(reading, BorderLayout.NORTH);

        GridCoverageNwwLayer rasterLayer = (GridCoverageNwwLayer) layer;
        new Thread(() -> {
            RasterInfo readInfo;
            String error = null;
            try {
                readInfo = rasterLayer.isRemote()
                        ? GridCoverageNwwLayer.readRasterInfo(rasterLayer.getRemoteReader())
                        : GridCoverageNwwLayer.readRasterInfo(source.file());
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
        /** The source of the raster: a file, or a remote raster of a web service. */
        private final LayerSource source;
        /** If the raster is remote, as a cloud optimized GeoTIFF, read by the reader of the layer. */
        private final boolean remote;
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
        private final JTextField minField = new JTextField(10);
        private final JTextField maxField = new JTextField(10);
        private final JButton fromDataButton = new JButton("From data");
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
            this.remote = ((GridCoverageNwwLayer) layer).isRemote();
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
                    setIcon(table == null ? null : new GradientIcon(RasterStyleUtilities.getColortableColors(table)));
                    setIconTextGap(8);
                    return this;
                }
            });
            tableCombo.setMaximumRowCount(16);
            tableCombo.addActionListener(e -> onTableChanged());
            fromDataButton.setToolTipText(remote
                    ? "Use the minimum and maximum of the raster, read at a low resolution, as from an overview"
                    : "Use the minimum and maximum of the raster, reading all its cells, or, for the rasters larger "
                            + "than " + GridCoverageNwwLayer.EXACT_STATISTICS_MAX_CELLS / 1_000_000
                            + " million cells, at a low resolution, as from an overview");
            fromDataButton.addActionListener(e -> readRangeFromData());
            tableArea.setToolTipText("<html>One color per line, as <code>r g b</code>, spread on the range, or with its value, "
                    + "as <code>value r g b</code>.<br>Editing the colors makes a custom colortable.</html>");
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
            c.gridy = 3;
            c.gridx = 1;
            c.gridwidth = 3;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            JPanel rangePanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            rangePanel.add(fromDataButton);
            rangePanel.add(rangeNote);
            content.add(rangePanel, c);
            c.gridy = 4;
            c.gridx = 0;
            c.gridwidth = 4;
            c.weightx = 1;
            c.weighty = 1;
            c.fill = GridBagConstraints.BOTH;
            JScrollPane tableScroll = new JScrollPane(tableArea);
            tableScroll.setBorder(BorderFactory.createTitledBorder("Colors"));
            content.add(tableScroll, c);
            c.gridy = 5;
            c.weighty = 0;
            c.fill = GridBagConstraints.NONE;
            JPanel reliefPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
            reliefPanel.add(reliefCheck);
            reliefPanel.add(reliefSpinner);
            content.add(reliefPanel, c);

            add(content, BorderLayout.CENTER);
            add(buttonsPanel(), BorderLayout.SOUTH);

            showStyle(((GridCoverageNwwLayer) layer).getStyle());
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
            String customTable = colorMap == null ? "" : RasterStyleUtilities.toColortableString(colorMap);
            double[] range = colorMap == null ? null : RasterStyleUtilities.getColorMapRange(colorMap);
            boolean known = ruleName != null && tableString(ruleName) != null;

            updating = true;
            try {
                int band = RasterStyleUtilities.getGrayChannel(style);
                bandCombo.setSelectedIndex(band < 0 || band >= info.bandsCount() ? 0 : band);
                if (known) {
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
            return RasterStyleUtilities.colortableHasValues(tableArea.getText());
        }

        /**
         * The range is used only by the colortables without values.
         */
        private void updateRangeState() {
            boolean usesRange = !hasValues();
            minField.setEnabled(usesRange);
            maxField.setEnabled(usesRange);
            fromDataButton.setEnabled(usesRange);
            rangeNote.setText(usesRange ? " " : "The colortable gives the values of its colors.");
        }

        private void onTableChanged() {
            if (updating) {
                return;
            }
            String name = (String) tableCombo.getSelectedItem();
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
            if (!hasValues()) {
                readRangeFromData();
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
            setStatus("Reading the range of the values...", false);
            int band = selectedBand();
            // only the last read is shown, as when the band is changed while reading
            int request = ++rangeRequest;
            // the reader of the layer drawn now, the layer is replaced at each style
            GridCoverageNwwLayer current = (GridCoverageNwwLayer) context.getLatest(layer);
            new Thread(() -> {
                try {
                    double[] stats;
                    boolean lowResolution;
                    if (remote) {
                        stats = GridCoverageNwwLayer.readLowResolutionStatistics(current.getRemoteReader(), band);
                        lowResolution = true;
                    } else {
                        // the large rasters at a low resolution, reading all their cells would take long
                        lowResolution = GridCoverageNwwLayer.isLargeRaster(source.file());
                        stats = GridCoverageNwwLayer.readStatistics(source.file(), band);
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (request != rangeRequest) {
                            return;
                        }
                        if (stats == null) {
                            setStatus("The raster has no valid values.", true);
                        } else {
                            minField.setText(RasterLayerTool.numberText(stats[0]));
                            maxField.setText(RasterLayerTool.numberText(stats[1]));
                            setStatus(lowResolution ? "Range read at a low resolution, the extremes can be a little wider."
                                    : " ", false);
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
            saveButton.setToolTipText(remote
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
         * Go back to the style of the .sld file, else to the default one.
         */
        private void reset() {
            boolean saved = remote ? LocalStyleStore.read(source) != null : SldUtilities.getStyleFile(source.file()) != null;
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
