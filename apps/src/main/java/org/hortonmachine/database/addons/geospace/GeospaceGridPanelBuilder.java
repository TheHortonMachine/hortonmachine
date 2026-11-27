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
package org.hortonmachine.database.addons.geospace;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.geom.Ellipse2D;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.hortonmachine.database.addons.geospace.GeospaceGridData.Layer;
import org.hortonmachine.database.addons.geospace.GeospaceGridData.PseudoCell;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYLineAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedRangeXYPlot;
import org.jfree.chart.plot.IntervalMarker;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

/**
 * Builds the WHETGEO 1D grid view: a to-scale soil column (layers, cell
 * interfaces, pseudo cells), the initial water suction and temperature and the
 * cell size against depth, all on one depth axis, and a table with the
 * geometry and the parameters of every layer.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GeospaceGridPanelBuilder {

    /** Layer colors by parameter set, in order of appearance. */
    private static final Color[] LAYER_COLORS = {//
            ColorUtilities.fromHex("#c9a66bff"), // sand
            ColorUtilities.fromHex("#8fb5a0ff"), // sage
            ColorUtilities.fromHex("#b48a9eff"), // mauve
            ColorUtilities.fromHex("#7fa7c9ff"), // steel blue
            ColorUtilities.fromHex("#d4b483ff"), // wheat
            ColorUtilities.fromHex("#a3a36bff"), // olive
            ColorUtilities.fromHex("#c98b6bff"), // terracotta
            ColorUtilities.fromHex("#9a8fc9ff"), // lavender
    };
    private static final Color PSEUDO_COLOR = ColorUtilities.fromHex("#9ecae1ff");
    private static final Color PSI_COLOR = ColorUtilities.fromHex("#3a7ecfff");
    private static final Color TEMPERATURE_COLOR = ColorUtilities.fromHex("#b35806ff");
    private static final Color DZ_COLOR = ColorUtilities.fromHex("#525252ff");
    private static final Color WARNING_COLOR = ColorUtilities.fromHex("#b35806ff");

    /** Height of a pseudo cell band above the surface, as a fraction of the column depth. */
    private static final double PSEUDO_BAND_FRACTION = 0.04;

    private static final DecimalFormat DEPTH_FORMAT = new DecimalFormat("0.00###");

    private GeospaceGridPanelBuilder() {
    }

    public static JPanel build( GeospaceGridData data ) {
        Map<Integer, Color> colors = layerColors(data);
        double depth = data.topEta() - data.bottomEta();
        double pseudoBand = depth * PSEUDO_BAND_FRACTION;
        double plotTop = data.topEta() + pseudoBand * data.pseudoCells.size();

        NumberAxis depthAxis = new NumberAxis("Depth [m]");
        depthAxis.setAutoRangeIncludesZero(false);
        depthAxis.setRange(data.bottomEta() - depth * 0.01, plotTop + depth * 0.01);
        CombinedRangeXYPlot combined = new CombinedRangeXYPlot(depthAxis);
        combined.setGap(10);

        combined.add(buildColumnPlot(data, colors, pseudoBand, plotTop), 3);
        if (data.psiIC != null) {
            combined.add(buildProfilePlot(data, colors, "Initial ψ [m]", data.psiIC, PSI_COLOR, 1, false), 2);
        }
        if (data.temperatureIC != null) {
            combined.add(buildProfilePlot(data, colors, "Initial T [K]", data.temperatureIC, TEMPERATURE_COLOR, 2, false), 2);
        }
        combined.add(buildProfilePlot(data, colors, "Cell size dz [m]", data.controlVolume, DZ_COLOR, 0, true), 2);

        JFreeChart chart = new JFreeChart(null, JFreeChart.DEFAULT_TITLE_FONT, combined, false);
        ChartPanel chartPanel = new ChartPanel(chart, true);
        chartPanel.setDisplayToolTips(true);
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMinimumDrawHeight(0);

        JTable table = buildTable(data, colors);
        JScrollPane tableScroll = new JScrollPane(table);

        JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanel, tableScroll);
        split.setResizeWeight(0.75);
        split.setOneTouchExpandable(true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(buildSummary(data), BorderLayout.NORTH);
        panel.add(split, BorderLayout.CENTER);
        return panel;
    }

    private static Map<Integer, Color> layerColors( GeospaceGridData data ) {
        Map<Integer, Color> colors = new LinkedHashMap<>();
        for( Layer layer : data.layers ) {
            Integer key = layer.parameterID;
            if (!colors.containsKey(key)) {
                colors.put(key, LAYER_COLORS[colors.size() % LAYER_COLORS.length]);
            }
        }
        return colors;
    }

    private static JLabel buildSummary( GeospaceGridData data ) {
        StringBuilder html = new StringBuilder("<html>");
        html.append("<b>").append(data.cellCount()).append(" cells</b> from ").append(DEPTH_FORMAT.format(data.topEta()))
                .append(" to ").append(DEPTH_FORMAT.format(data.bottomEta())).append(" m");
        html.append(" &nbsp;·&nbsp; ").append(plural(data.layers.size(), "layer"));
        double minDz = Double.POSITIVE_INFINITY;
        double maxDz = 0;
        for( double dz : data.controlVolume ) {
            minDz = Math.min(minDz, dz);
            maxDz = Math.max(maxDz, dz);
        }
        if (data.cellCount() > 0) {
            html.append(" &nbsp;·&nbsp; dz ").append(dzText(minDz, maxDz)).append(" m");
        }
        if (!data.pseudoCells.isEmpty()) {
            html.append(" &nbsp;·&nbsp; ").append(plural(data.pseudoCells.size(), "pseudo cell"));
        }
        html.append(" &nbsp;·&nbsp; <i>").append(data.source).append("</i>");
        String warningHex = String.format("#%02x%02x%02x", WARNING_COLOR.getRed(), WARNING_COLOR.getGreen(),
                WARNING_COLOR.getBlue());
        for( String warning : data.warnings ) {
            html.append("<br><font color='").append(warningHex).append("'>⚠ ").append(warning).append("</font>");
        }
        html.append("</html>");
        JLabel label = new JLabel(html.toString());
        label.setFont(label.getFont().deriveFont(Font.PLAIN));
        label.setBorder(BorderFactory.createEmptyBorder(8, 10, 8, 10));
        return label;
    }

    /**
     * The soil column to scale: one band per layer, a tick per cell interface on the
     * right edge (dense ticks = small cells), the pseudo cells as bands above the
     * surface.
     */
    private static XYPlot buildColumnPlot( GeospaceGridData data, Map<Integer, Color> colors, double pseudoBand,
            double plotTop ) {
        // two invisible points span the plot, markers and annotations do the drawing
        XYSeries span = new XYSeries("span");
        span.add(0, plotTop);
        span.add(1, data.bottomEta());
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(false, false);

        NumberAxis xAxis = new NumberAxis("Column");
        xAxis.setRange(0, 1);
        xAxis.setTickLabelsVisible(false);
        xAxis.setTickMarksVisible(false);

        XYPlot plot = new XYPlot(new XYSeriesCollection(span), xAxis, null, renderer);
        plot.setDomainGridlinesVisible(false);
        plot.setRangeGridlinesVisible(false);

        for( int i = 0; i < data.layers.size(); i++ ) {
            Layer layer = data.layers.get(i);
            IntervalMarker band = new IntervalMarker(layer.bottomEta, layer.topEta);
            band.setPaint(colors.get(layer.parameterID));
            band.setAlpha(0.55f);
            band.setLabel(layerLabel(i, layer));
            band.setLabelFont(band.getLabelFont().deriveFont(Font.PLAIN, 11f));
            band.setLabelAnchor(RectangleAnchor.LEFT);
            band.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
            band.setLabelOffset(new RectangleInsets(2, 6, 2, 6));
            plot.addRangeMarker(band, org.jfree.ui.Layer.BACKGROUND);
        }

        // cell interfaces as ticks on the right, layer boundaries across the column
        BasicStroke thin = new BasicStroke(0.6f);
        for( double face : data.etaDual ) {
            plot.addAnnotation(new XYLineAnnotation(0.82, face, 1.0, face, thin, Color.DARK_GRAY));
        }
        BasicStroke thick = new BasicStroke(1.5f);
        for( Layer layer : data.layers ) {
            plot.addAnnotation(new XYLineAnnotation(0, layer.topEta, 1.0, layer.topEta, thick, Color.BLACK));
        }
        plot.addAnnotation(new XYLineAnnotation(0, data.bottomEta(), 1.0, data.bottomEta(), thick, Color.BLACK));

        double bandBottom = data.topEta();
        for( PseudoCell cell : data.pseudoCells ) {
            IntervalMarker band = new IntervalMarker(bandBottom, bandBottom + pseudoBand);
            band.setPaint(PSEUDO_COLOR);
            band.setAlpha(0.8f);
            band.setOutlinePaint(Color.GRAY);
            band.setLabel(pseudoLabel(cell));
            band.setLabelFont(band.getLabelFont().deriveFont(Font.ITALIC, 10f));
            band.setLabelAnchor(RectangleAnchor.LEFT);
            band.setLabelTextAnchor(TextAnchor.CENTER_LEFT);
            band.setLabelOffset(new RectangleInsets(2, 6, 2, 6));
            plot.addRangeMarker(band, org.jfree.ui.Layer.BACKGROUND);
            bandBottom += pseudoBand;
        }

        ValueMarker surface = new ValueMarker(data.topEta());
        surface.setPaint(ColorUtilities.fromHex("#2e7d32ff"));
        surface.setStroke(new BasicStroke(2f));
        surface.setLabel("surface " + DEPTH_FORMAT.format(data.topEta()) + " m");
        surface.setLabelFont(surface.getLabelFont().deriveFont(Font.BOLD, 10f));
        // below the line on the left: above it are the pseudo cells, on the right the ticks
        surface.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
        surface.setLabelTextAnchor(TextAnchor.TOP_LEFT);
        surface.setLabelOffset(new RectangleInsets(4, 6, 4, 6));
        plot.addRangeMarker(surface, org.jfree.ui.Layer.FOREGROUND);
        return plot;
    }

    /**
     * A per-cell value against depth, with the layer bands behind it.
     *
     * @param icPointIndex the column of the raw initial condition points to show as
     *                     dots (1 psi, 2 temperature), or 0 for none.
     * @param fromZero     true to start the value axis at zero, ex. for sizes.
     */
    private static XYPlot buildProfilePlot( GeospaceGridData data, Map<Integer, Color> colors, String label,
            double[] values, Color color, int icPointIndex, boolean fromZero ) {
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries(label, false, true);
        for( int i = 0; i < data.cellCount(); i++ ) {
            series.add(values[i], data.eta[i]);
        }
        dataset.addSeries(series);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, data.cellCount() <= 200);
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        renderer.setSeriesShape(0, new Ellipse2D.Double(-2, -2, 4, 4));
        renderer.setBaseToolTipGenerator(( ds, s, item ) -> s == 0 ? cellTip(data, item, label, values[item])
                : "initial_condition point: eta " + DEPTH_FORMAT.format(ds.getYValue(s, item)) + " m, "
                        + formatValue(ds.getXValue(s, item)));

        if (icPointIndex > 0 && !data.icPoints.isEmpty()) {
            XYSeries points = new XYSeries("initial_condition", false, true);
            for( double[] p : data.icPoints ) {
                points.add(p[icPointIndex], p[0]);
            }
            dataset.addSeries(points);
            renderer.setSeriesLinesVisible(1, false);
            renderer.setSeriesShapesVisible(1, true);
            renderer.setSeriesShape(1, new Ellipse2D.Double(-5, -5, 10, 10));
            renderer.setSeriesPaint(1, Color.BLACK);
        }

        NumberAxis xAxis = new NumberAxis(label);
        xAxis.setAutoRangeIncludesZero(false);
        xAxis.setLabelPaint(color);
        xAxis.setTickLabelPaint(color);
        if (fromZero) {
            double max = 0;
            for( double v : values ) {
                max = Math.max(max, v);
            }
            xAxis.setRange(0, max > 0 ? max * 1.15 : 1);
        } else if (isConstant(values)) {
            // a flat profile would otherwise get a degenerate axis
            double v = values.length > 0 ? values[0] : 0;
            double pad = v != 0 ? Math.abs(v) * 0.02 : 0.5;
            xAxis.setRange(v - pad, v + pad);
        }

        XYPlot plot = new XYPlot(dataset, xAxis, null, renderer);
        for( Layer layer : data.layers ) {
            IntervalMarker band = new IntervalMarker(layer.bottomEta, layer.topEta);
            band.setPaint(colors.get(layer.parameterID));
            band.setAlpha(0.18f);
            plot.addRangeMarker(band, org.jfree.ui.Layer.BACKGROUND);
        }
        return plot;
    }

    private static JTable buildTable( GeospaceGridData data, Map<Integer, Color> colors ) {
        // every parameter column of the sets in use, in table order
        Set<String> parameterColumns = new LinkedHashSet<>();
        for( Map<String, Double> set : data.parameterSets.values() ) {
            parameterColumns.addAll(set.keySet());
        }
        List<String> headers = new ArrayList<>(
                List.of("Layer", "Top [m]", "Bottom [m]", "Cells", "dz [m]", "Eq. state ID", "Parameter ID"));
        for( String column : parameterColumns ) {
            String slot = data.parameterSlots.get(column);
            headers.add(slot != null ? column + " (" + slot + ")" : column);
        }

        List<Object[]> rows = new ArrayList<>();
        List<Color> rowColors = new ArrayList<>();
        for( int p = data.pseudoCells.size() - 1; p >= 0; p-- ) {
            PseudoCell cell = data.pseudoCells.get(p);
            List<Object> row = new ArrayList<>(List.of("pseudo " + cell.stackOrder
                    + (cell.note.isEmpty() ? "" : ": " + cell.note), DEPTH_FORMAT.format(data.topEta()),
                    DEPTH_FORMAT.format(data.topEta()), 1, "cv " + formatValue(cell.controlVolume), idText(cell.equationStateID),
                    idText(cell.parameterID)));
            addParameters(row, data.parameterSets.get(cell.parameterID), parameterColumns);
            rows.add(row.toArray());
            rowColors.add(PSEUDO_COLOR);
        }
        for( int i = 0; i < data.layers.size(); i++ ) {
            Layer layer = data.layers.get(i);
            String name = String.valueOf(i + 1)
                    + (layer.sourceRows > 1 ? " (" + layer.sourceRows + " grid_geometry rows)" : "");
            List<Object> row = new ArrayList<>(List.of(name, DEPTH_FORMAT.format(layer.topEta),
                    DEPTH_FORMAT.format(layer.bottomEta), layer.cells, dzText(layer.minDz, layer.maxDz),
                    idText(layer.equationStateID), idText(layer.parameterID)));
            addParameters(row, layer.parameterID == null ? null : data.parameterSets.get(layer.parameterID),
                    parameterColumns);
            rows.add(row.toArray());
            rowColors.add(colors.get(layer.parameterID));
        }

        DefaultTableModel model = new DefaultTableModel(rows.toArray(new Object[0][]), headers.toArray()){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable( int row, int column ) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer(){
            private static final long serialVersionUID = 1L;

            @Override
            public Component getTableCellRendererComponent( JTable t, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column ) {
                Component c = super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                if (!isSelected) {
                    Color base = rowColors.get(row);
                    c.setBackground(base == null ? t.getBackground() : blendWithWhite(base, 0.6));
                }
                return c;
            }
        };
        for( int c = 0; c < table.getColumnCount(); c++ ) {
            table.getColumnModel().getColumn(c).setCellRenderer(renderer);
            int width = Math.max(90, headers.get(c).length() * 8);
            table.getColumnModel().getColumn(c).setPreferredWidth(c == 0 ? 220 : width);
        }
        return table;
    }

    private static void addParameters( List<Object> row, Map<String, Double> set, Set<String> columns ) {
        for( String column : columns ) {
            Double value = set == null ? null : set.get(column);
            row.add(value == null ? "–" : formatValue(value));
        }
    }

    private static String layerLabel( int index, Layer layer ) {
        StringBuilder sb = new StringBuilder();
        if (layer.parameterID != null) {
            sb.append("Layer ").append(index + 1).append(" · parameter set ").append(layer.parameterID).append(" · ");
        }
        sb.append(plural(layer.cells, "cell")).append(" · dz ").append(dzText(layer.minDz, layer.maxDz)).append(" m");
        if (layer.sourceRows > 1) {
            sb.append(" · ").append(layer.sourceRows).append(" grid rows");
        }
        return sb.toString();
    }

    private static String pseudoLabel( PseudoCell cell ) {
        StringBuilder sb = new StringBuilder("pseudo cell");
        if (!cell.note.isEmpty()) {
            sb.append(": ").append(cell.note);
        }
        sb.append(" (cv ").append(formatValue(cell.controlVolume));
        if (cell.parameterID != null) {
            sb.append(", parameter set ").append(cell.parameterID);
        }
        return sb.append(")").toString();
    }

    private static String cellTip( GeospaceGridData data, int i, String label, double value ) {
        StringBuilder tip = new StringBuilder("cell ").append(i + 1).append(" from surface");
        tip.append("  |  eta ").append(DEPTH_FORMAT.format(data.eta[i])).append(" m");
        tip.append("  |  dz ").append(formatValue(data.controlVolume[i])).append(" m");
        if (data.parameterID != null) {
            tip.append("  |  parameter set ").append(data.parameterID[i]);
        }
        tip.append("  |  ").append(label).append(" = ").append(formatValue(value));
        return tip.toString();
    }

    private static String dzText( double minDz, double maxDz ) {
        if (Math.abs(maxDz - minDz) <= 1e-12 * Math.max(1, maxDz)) {
            return formatValue(minDz);
        }
        return formatValue(minDz) + "–" + formatValue(maxDz);
    }

    private static String idText( Integer id ) {
        return id == null ? "–" : String.valueOf(id);
    }

    private static String plural( int n, String word ) {
        return n + " " + word + (n == 1 ? "" : "s");
    }

    static String formatValue( double value ) {
        double abs = Math.abs(value);
        if (value != 0 && (abs < 1e-3 || abs >= 1e5)) {
            return String.format("%.4e", value);
        }
        return new DecimalFormat("0.#####").format(value);
    }

    private static boolean isConstant( double[] values ) {
        for( double v : values ) {
            if (Math.abs(v - values[0]) > 1e-12 * Math.max(1, Math.abs(values[0]))) {
                return false;
            }
        }
        return true;
    }

    private static Color blendWithWhite( Color color, double whiteFraction ) {
        int r = (int) Math.round(color.getRed() + (255 - color.getRed()) * whiteFraction);
        int g = (int) Math.round(color.getGreen() + (255 - color.getGreen()) * whiteFraction);
        int b = (int) Math.round(color.getBlue() + (255 - color.getBlue()) * whiteFraction);
        return new Color(r, g, b);
    }
}
