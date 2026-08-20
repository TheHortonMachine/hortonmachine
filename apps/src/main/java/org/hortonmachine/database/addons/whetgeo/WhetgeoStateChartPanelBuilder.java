/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com
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
package org.hortonmachine.database.addons.whetgeo;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;

import org.hortonmachine.database.addons.whetgeo.WhetgeoStateChartData.DepthSeries;
import org.hortonmachine.database.addons.whetgeo.WhetgeoStateChartData.SwrcParams;
import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.labels.XYZToolTipGenerator;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.PaintScale;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYBlockRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYZDataset;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.RectangleEdge;
import org.jfree.ui.RectangleInsets;
import org.jfree.ui.TextAnchor;

/**
 * Builds the WHETGEO 1D state (Hovmoller-style) chart: an optional top/bottom
 * boundary condition forcing panel, and one depth-vs-time heatmap panel per
 * state variable present in the data.
 *
 * <p>
 * All time-varying rows share one {@link CombinedDomainXYPlot} (single time
 * axis) with each heatmap's {@link PaintScaleLegend} added as a chart
 * subtitle. Two per-row layout variants (separate charts with a fixed-width
 * legend column; a hand-drawn legend component) were tried and reverted:
 * both either failed to reliably render the legend text or failed to
 * actually honor an explicit preferred width, which in turn desynced the
 * rows' plot columns and broke the shared time axis - this combined-plot
 * version is the one that has actually rendered correctly. Its one real
 * limitation - the legends stack together at the top-right instead of lining
 * up with their own row - is addressed by giving each heatmap a visually
 * distinct color ramp (light blue for water content, dark blue for water
 * suction), so which legend belongs to which panel is unambiguous by color
 * even without positional alignment.
 *
 * <p>
 * Every range (Y) axis in the combined plot - the forcing axis and every
 * heatmap's depth axis - is given the same {@link
 * org.jfree.chart.axis.Axis#setFixedDimension(double)} and the same tick
 * number format, so they all reserve identical space regardless of their
 * own tick label content; without that, a row needing wider tick labels
 * (e.g. "-3.00") than another (e.g. "4") ends up with its plot area
 * starting at a different pixel column, breaking the shared time axis
 * alignment on the left side the same way an unequal legend width broke it
 * on the right.
 *
 * <p>
 * A boundary condition series that never changes (typically the bottom BC
 * under free drainage, which the solver ignores) is rendered as a plain
 * "label = value" text row above the chart instead of a whole bar-chart row
 * with a flat line and a single-tick axis - not useful, and harder to read
 * than text.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WhetgeoStateChartPanelBuilder {
    private static final Color TOP_BC_COLOR = ColorUtilities.fromHex("#0096ffff");
    private static final Color BOTTOM_BC_COLOR = ColorUtilities.fromHex("#8a5a00ff");

    // one distinct sequential ramp per depth series, so each heatmap and its
    // (positionally unaligned) legend are still visually matched by color alone.
    // Keyed by DepthSeries.name (see WhetgeoStateChartDataLoader) rather than list
    // position: every optional column is independently present or absent per run
    // (see WHETGEO-1D own output handler), so a series' index in the list shifts from run
    // to run and can't be used to pick a stable color - e.g. a Richards output with
    // no temperature column would otherwise put theta at index 0 and get painted
    // with the ramp meant for temperature.
    private static final Map<String, Color[]> HEATMAP_RAMPS = new HashMap<>();
    private static final Color[] FALLBACK_RAMP = //
            {ColorUtilities.fromHex("#f0f0f0ff"), ColorUtilities.fromHex("#525252ff")}; // gray - unrecognized series
    static {
        HEATMAP_RAMPS.put("Temperature",
                new Color[]{ColorUtilities.fromHex("#fee8c8ff"), ColorUtilities.fromHex("#b35806ff")}); // orange
        HEATMAP_RAMPS.put("Water content",
                new Color[]{ColorUtilities.fromHex("#eaf2fbff"), ColorUtilities.fromHex("#3a7ecfff")}); // light blue
        HEATMAP_RAMPS.put("Water suction",
                new Color[]{ColorUtilities.fromHex("#eaf2fbff"), ColorUtilities.fromHex("#08306bff")}); // dark blue
        HEATMAP_RAMPS.put("Internal energy",
                new Color[]{ColorUtilities.fromHex("#f0e6f7ff"), ColorUtilities.fromHex("#54278fff")}); // purple
        HEATMAP_RAMPS.put("Ice content",
                new Color[]{ColorUtilities.fromHex("#e0f3f0ff"), ColorUtilities.fromHex("#00695cff")}); // teal
    }

    /** Reserved range-axis width, in Java2D units, identical for every sub-plot - see class javadoc. */
    private static final double RANGE_AXIS_FIXED_DIMENSION = 55;

    private WhetgeoStateChartPanelBuilder() {
    }

    public static JPanel build( WhetgeoStateChartData data, String title ) {
        JPanel constantRows = new JPanel();
        constantRows.setLayout(new BoxLayout(constantRows, BoxLayout.Y_AXIS));

        DateAxis sharedTimeAxis = new DateAxis("Time");
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(sharedTimeAxis);
        combinedPlot.setGap(12);
        boolean hasChartRow = false;

        String topLabel = bcLabel("Top Boundary Condition", data.topBCType);
        double[] topDistinct = distinctSorted(data.topBCTimes.length > 0 ? data.topBCValues : new double[0]);
        if (data.topBCTimes.length > 0 && topDistinct.length > 1) {
            combinedPlot.add(buildBCPlot(topLabel, data.topBCTimes, data.topBCValues, TOP_BC_COLOR), 1);
            hasChartRow = true;
        } else if (topDistinct.length == 1) {
            addConstantValueRow(constantRows, topLabel, topDistinct[0]);
        }

        String bottomLabel = bcLabel("Bottom Boundary Condition", data.bottomBCType);
        double[] bottomDistinct = distinctSorted(data.bottomBCTimes.length > 0 ? data.bottomBCValues : new double[0]);
        if (data.bottomBCTimes.length > 0 && bottomDistinct.length > 1) {
            combinedPlot.add(buildBCPlot(bottomLabel, data.bottomBCTimes, data.bottomBCValues, BOTTOM_BC_COLOR), 1);
            hasChartRow = true;
        } else if (bottomDistinct.length == 1) {
            addConstantValueRow(constantRows, bottomLabel, bottomDistinct[0]);
        }

        List<LayerBoundary> layerBoundaries = computeLayerBoundaries(data);
        List<XYPlot> heatmapPlots = new ArrayList<>();

        List<PaintScaleLegend> legends = new ArrayList<>();
        for( int i = 0; i < data.depthSeries.size(); i++ ) {
            DepthSeries series = data.depthSeries.get(i);
            Color[] ramp = HEATMAP_RAMPS.getOrDefault(series.name, FALLBACK_RAMP);
            double[] bounds = valueBounds(series.values);
            PaintScale scale = new TwoColorPaintScale(bounds[0], bounds[1], ramp[0], ramp[1]);

            XYPlot heatmapPlot = buildHeatmapPlot(series, scale, layerBoundaries);
            heatmapPlots.add(heatmapPlot);
            combinedPlot.add(heatmapPlot, 2);
            hasChartRow = true;
            legends.add(buildLegend(series, scale, bounds));
        }

        JPanel topArea = new JPanel();
        topArea.setLayout(new BoxLayout(topArea, BoxLayout.Y_AXIS));
        if (!layerBoundaries.isEmpty()) {
            JCheckBox showAnnotationsCheck = new JCheckBox("Show layer annotations", true);
            showAnnotationsCheck.setAlignmentX(JCheckBox.LEFT_ALIGNMENT);
            showAnnotationsCheck.addActionListener(e -> {
                boolean show = showAnnotationsCheck.isSelected();
                for( XYPlot plot : heatmapPlots ) {
                    plot.clearRangeMarkers();
                    if (show) {
                        addLayerBoundaryMarkers(plot, layerBoundaries);
                    }
                }
            });
            JPanel checkRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
            checkRow.add(showAnnotationsCheck);
            topArea.add(checkRow);
        }
        if (constantRows.getComponentCount() > 0) {
            topArea.add(constantRows);
        }

        JPanel panel = new JPanel(new BorderLayout());
        if (topArea.getComponentCount() > 0) {
            panel.add(topArea, BorderLayout.NORTH);
        }
        if (hasChartRow) {
            JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, false);
            for( PaintScaleLegend legend : legends ) {
                chart.addSubtitle(legend);
            }
            ChartPanel chartPanel = new ChartPanel(chart, true);
            chartPanel.setDisplayToolTips(true);
            // re-render at actual size on every resize instead of scaling the buffer
            chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
            chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
            chartPanel.setMinimumDrawWidth(0);
            chartPanel.setMinimumDrawHeight(0);
            panel.add(chartPanel, BorderLayout.CENTER);
        }
        return panel;
    }

    private static XYPlot buildBCPlot( String axisLabel, long[] times, double[] values, Color color ) {
        NumberAxis bcAxis = new NumberAxis(axisLabel);
        bcAxis.setAutoRangeIncludesZero(true);
        bcAxis.setLabelPaint(color);
        bcAxis.setTickLabelPaint(color);
        applyUniformAxisSizing(bcAxis);

        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setSeriesPaint(0, color);
        renderer.setUseYInterval(false);
        renderer.setDrawBarOutline(false);
        renderer.setMargin(0.1);
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator("{1}:  {2}",
                new SimpleDateFormat("dd-MMM HH:mm"), new DecimalFormat("0.###")));

        XYPlot plot = new XYPlot();
        plot.setDataset(0, toBarDataset(axisLabel, times, values));
        plot.setRenderer(0, renderer);
        plot.setRangeAxis(0, bcAxis);
        plot.mapDatasetToRangeAxis(0, 0);
        return plot;
    }

    private static XYPlot buildHeatmapPlot( DepthSeries series, PaintScale scale, List<LayerBoundary> layerBoundaries ) {
        double[] xValues = series.times.length > 0 ? toDoubleArray(series.times) : new double[0];
        double[] yValues = series.eta;
        double[] zValues = series.values;

        DefaultXYZDataset dataset = new DefaultXYZDataset();
        dataset.addSeries(series.name, new double[][]{xValues, yValues, zValues});

        double blockWidth = medianGap(distinctSorted(xValues));
        double blockHeight = medianGap(distinctSorted(yValues));

        XYBlockRenderer renderer = new XYBlockRenderer();
        renderer.setBlockWidth(blockWidth);
        renderer.setBlockHeight(blockHeight);
        renderer.setPaintScale(scale);
        renderer.setBaseToolTipGenerator(new DepthHeatmapToolTipGenerator(series.axisLabel));

        NumberAxis depthAxis = new NumberAxis("Depth [m]");
        depthAxis.setAutoRangeIncludesZero(false);
        applyUniformAxisSizing(depthAxis);

        XYPlot plot = new XYPlot();
        plot.setDataset(0, dataset);
        plot.setRenderer(0, renderer);
        plot.setRangeAxis(0, depthAxis);
        plot.mapDatasetToRangeAxis(0, 0);

        addLayerBoundaryMarkers(plot, layerBoundaries);

        return plot;
    }

    /** Draws one dashed, labeled {@link ValueMarker} per layer boundary onto {@code plot} -
     *  factored out so the "Show layer annotations" checkbox can call it again after {@code
     *  plot.clearRangeMarkers()} without rebuilding the whole chart. */
    private static void addLayerBoundaryMarkers( XYPlot plot, List<LayerBoundary> layerBoundaries ) {
        for( LayerBoundary boundary : layerBoundaries ) {
            ValueMarker marker = new ValueMarker(boundary.topEta);
            marker.setPaint(Color.DARK_GRAY);
            marker.setStroke(
                    new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 1f, new float[]{4f, 4f}, 0f));
            marker.setLabel(boundary.label);
            marker.setLabelFont(marker.getLabelFont().deriveFont(Font.PLAIN, 9f));
            marker.setLabelPaint(Color.DARK_GRAY);
            marker.setLabelAnchor(RectangleAnchor.TOP_LEFT);
            marker.setLabelTextAnchor(TextAnchor.BOTTOM_LEFT);
            marker.setLabelOffset(new RectangleInsets(2, 4, 2, 4));
            plot.addRangeMarker(marker);
        }
    }

    /**
     * One value per internal transition between parameter sets in {@link
     * WhetgeoStateChartData#gridEta}, each labeled with the SWRC parameters of the
     * layer above that boundary - empty if the output wasn't written with {@code
     * parameter_id}/{@code output_swrc_parameters} (see WHETGEO-1D's own output
     * handler), so older outputs just render without annotations.
     *
     * <p>
     * {@code gridEta} holds cell *centers*, not layer edges, so the boundary itself
     * is the midpoint between the last cell of the lower layer and the first cell
     * of the layer above it - not either cell's own eta - otherwise the drawn line
     * lands half a cell-thickness away from the real boundary (e.g. -1.01 instead
     * of the true -1.00 for two 0.02 m-thick layers).
     */
    private static List<LayerBoundary> computeLayerBoundaries( WhetgeoStateChartData data ) {
        List<LayerBoundary> boundaries = new ArrayList<>();
        if (data.gridParameterID.length != data.gridEta.length || data.swrcParameters.isEmpty()) {
            return boundaries;
        }
        Map<Integer, SwrcParams> byId = new HashMap<>();
        for( SwrcParams p : data.swrcParameters ) {
            byId.put(p.id, p);
        }

        // gridEta is ascending; a boundary exists wherever parameterID changes
        // between two consecutive cells
        for( int i = 1; i < data.gridEta.length; i++ ) {
            int lowerID = data.gridParameterID[i - 1];
            int upperID = data.gridParameterID[i];
            if (lowerID == upperID) {
                continue;
            }
            SwrcParams p = byId.get(upperID);
            if (p == null) {
                continue;
            }
            double boundaryEta = (data.gridEta[i - 1] + data.gridEta[i]) / 2.0;
            String label = String.format("θS=%.3f  θR=%.3f  Ks=%.2e", p.thetaS, p.thetaR, p.ks);
            boundaries.add(new LayerBoundary(boundaryEta, label));
        }
        return boundaries;
    }

    private static class LayerBoundary {
        final double topEta;
        final String label;

        LayerBoundary( double topEta, String label ) {
            this.topEta = topEta;
            this.label = label;
        }
    }

    /**
     * Forces the same reserved width and the same tick number format on every sub-plot's range
     * axis - see the class javadoc for why that's needed for the shared time axis to actually
     * line up across rows.
     */
    private static void applyUniformAxisSizing( NumberAxis axis ) {
        axis.setFixedDimension(RANGE_AXIS_FIXED_DIMENSION);
        axis.setNumberFormatOverride(new DecimalFormat("0.00"));
    }

    private static PaintScaleLegend buildLegend( DepthSeries series, PaintScale scale, double[] bounds ) {
        // pad the *displayed* axis range a bit beyond the true value bounds, purely so the
        // topmost/bottommost tick label has room to draw instead of sitting flush against the
        // legend's own edge and getting clipped by the chart canvas; the color strip itself
        // still maps colors against the true bounds (via `scale`), so this just adds a sliver
        // of flat top/bottom color, not a change to what the scale represents
        double span = bounds[1] - bounds[0];
        double rangePad = span > 0 ? span * 0.08 : 0.1;
        NumberAxis scaleAxis = new NumberAxis(series.axisLabel);
        scaleAxis.setRange(bounds[0] - rangePad, bounds[1] + rangePad);

        PaintScaleLegend legend = new PaintScaleLegend(scale, scaleAxis);
        legend.setPosition(RectangleEdge.RIGHT);
        legend.setMargin(new RectangleInsets(16, 8, 16, 8));
        legend.setPadding(new RectangleInsets(8, 4, 8, 8));
        legend.setStripWidth(16);
        return legend;
    }

    private static void addConstantValueRow( JPanel constantRows, String label, double value ) {
        JLabel textLabel = new JLabel(label + " = " + formatConstant(value), SwingConstants.CENTER);
        textLabel.setAlignmentX(JLabel.CENTER_ALIGNMENT);
        textLabel.setFont(textLabel.getFont().deriveFont(Font.PLAIN));
        textLabel.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
        constantRows.add(textLabel);
    }

    private static String formatConstant( double value ) {
        if (value == Math.rint(value) && !Double.isInfinite(value)) {
            return String.valueOf((long) value);
        }
        return String.valueOf(value);
    }

    /** Appends the BC type (e.g. "TOP_COUPLED") to the label if the output was written with
     *  one (see {@code Whetgeo1DOutputSchema.TABLE_OUTPUT_METADATA}); a plain fallback
     *  label otherwise, so a value alone doesn't have to stand in for what kind of condition
     *  produced it. */
    private static String bcLabel( String base, String bcType ) {
        return bcType == null ? base : base + " (" + bcType + ")";
    }

    private static double[] valueBounds( double[] values ) {
        double lowerBound = Arrays.stream(values).min().orElse(0);
        double upperBound = Arrays.stream(values).max().orElse(1);
        if (upperBound <= lowerBound) {
            upperBound = lowerBound + 1;
        }
        return new double[]{lowerBound, upperBound};
    }

    private static double[] toDoubleArray( long[] values ) {
        double[] result = new double[values.length];
        for( int i = 0; i < values.length; i++ ) {
            result[i] = values[i];
        }
        return result;
    }

    private static double[] distinctSorted( double[] values ) {
        return Arrays.stream(values).distinct().sorted().toArray();
    }

    /**
     * Block dimension for a value axis in a heatmap: JFreeChart's
     * {@link XYBlockRenderer} draws every block at one fixed size, so a
     * (typically near-uniform, but not perfectly so - e.g. half-thickness
     * boundary cells) sampling grid is approximated by the median gap between
     * consecutive distinct values.
     */
    private static double medianGap( double[] distinctSortedValues ) {
        int n = distinctSortedValues.length;
        if (n < 2) {
            return 1.0;
        }
        double[] gaps = new double[n - 1];
        for( int i = 1; i < n; i++ ) {
            gaps[i - 1] = distinctSortedValues[i] - distinctSortedValues[i - 1];
        }
        Arrays.sort(gaps);
        int mid = gaps.length / 2;
        return gaps.length % 2 == 0 ? (gaps[mid - 1] + gaps[mid]) / 2.0 : gaps[mid];
    }

    /**
     * Builds a bar-friendly interval dataset: each bar spans from the midpoint with the
     * previous sample to the midpoint with the next one, so the bar width follows the
     * actual (possibly variable) sampling interval instead of an assumed fixed period.
     */
    private static IntervalXYDataset toBarDataset( String name, long[] times, double[] values ) {
        int n = times.length;
        double[] xValues = new double[n];
        double[] xStart = new double[n];
        double[] xEnd = new double[n];
        double[] yValues = new double[n];
        for( int i = 0; i < n; i++ ) {
            xValues[i] = times[i];
            yValues[i] = values[i];
            double beforeGap = i > 0 ? (times[i] - times[i - 1]) / 2.0 : Double.NaN;
            double afterGap = i < n - 1 ? (times[i + 1] - times[i]) / 2.0 : Double.NaN;
            if (Double.isNaN(beforeGap)) {
                beforeGap = afterGap;
            }
            if (Double.isNaN(afterGap)) {
                afterGap = beforeGap;
            }
            xStart[i] = times[i] - beforeGap;
            xEnd[i] = times[i] + afterGap;
        }
        DefaultIntervalXYDataset dataset = new DefaultIntervalXYDataset();
        dataset.addSeries(name, new double[][]{xValues, xStart, xEnd, yValues, yValues, yValues});
        return dataset;
    }

    /** Hover tooltip for a heatmap cell: time, depth and the cell's value, labeled with the
     *  series' own axis label (e.g. "Water content - theta [-]") so the number is never shown
     *  unitless. Implements both {@link #generateToolTip(XYDataset, int, int)} (the interface
     *  method the renderer's generic tooltip path is statically bound to call) and {@link
     *  #generateToolTip(XYZDataset, int, int)} (in case the z-aware overload is used instead) by
     *  routing both through the same z-extracting logic, so the tooltip is correct regardless of
     *  which one JFreeChart actually invokes. */
    private static class DepthHeatmapToolTipGenerator implements XYZToolTipGenerator {
        private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd-MMM HH:mm");
        private final DecimalFormat valueFormat = new DecimalFormat("0.###");
        private final String axisLabel;

        DepthHeatmapToolTipGenerator( String axisLabel ) {
            this.axisLabel = axisLabel;
        }

        @Override
        public String generateToolTip( XYZDataset dataset, int series, int item ) {
            return buildTip(dataset, series, item);
        }

        @Override
        public String generateToolTip( XYDataset dataset, int series, int item ) {
            return buildTip(dataset, series, item);
        }

        private String buildTip( XYDataset dataset, int series, int item ) {
            double x = dataset.getXValue(series, item);
            double y = dataset.getYValue(series, item);
            StringBuilder tip = new StringBuilder();
            tip.append(dateFormat.format(new Date((long) x)));
            tip.append("  |  depth ").append(valueFormat.format(y)).append(" m");
            if (dataset instanceof XYZDataset) {
                double z = ((XYZDataset) dataset).getZValue(series, item);
                tip.append("  |  ").append(axisLabel).append(" = ").append(valueFormat.format(z));
            }
            return tip.toString();
        }
    }

    /** Continuous linear interpolation between two colors - a proper sequential ramp,
     *  unlike {@code LookupPaintScale} (discrete lookup table) or {@code GrayPaintScale}
     *  (fixed to grayscale). */
    private static class TwoColorPaintScale implements PaintScale {
        private final double lowerBound;
        private final double upperBound;
        private final Color lowColor;
        private final Color highColor;

        TwoColorPaintScale( double lowerBound, double upperBound, Color lowColor, Color highColor ) {
            this.lowerBound = lowerBound;
            this.upperBound = upperBound;
            this.lowColor = lowColor;
            this.highColor = highColor;
        }

        @Override
        public double getLowerBound() {
            return lowerBound;
        }

        @Override
        public double getUpperBound() {
            return upperBound;
        }

        @Override
        public java.awt.Paint getPaint( double value ) {
            double t = (value - lowerBound) / (upperBound - lowerBound);
            t = Math.max(0.0, Math.min(1.0, t));
            int r = (int) Math.round(lowColor.getRed() + t * (highColor.getRed() - lowColor.getRed()));
            int g = (int) Math.round(lowColor.getGreen() + t * (highColor.getGreen() - lowColor.getGreen()));
            int b = (int) Math.round(lowColor.getBlue() + t * (highColor.getBlue() - lowColor.getBlue()));
            return new Color(r, g, b);
        }
    }
}
