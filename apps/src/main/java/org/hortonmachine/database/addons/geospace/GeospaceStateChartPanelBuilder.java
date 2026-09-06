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

import org.hortonmachine.database.addons.geospace.GeospaceStateChartData.DepthSeries;
import org.hortonmachine.database.addons.geospace.GeospaceStateChartData.SwrcParams;
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
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.PaintScaleLegend;
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.DefaultXYZDataset;
import org.jfree.data.xy.IntervalXYDataset;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
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
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GeospaceStateChartPanelBuilder {
    private static final Color TOP_BC_COLOR = ColorUtilities.fromHex("#0096ffff");
    private static final Color BOTTOM_BC_COLOR = ColorUtilities.fromHex("#8a5a00ff");

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
        HEATMAP_RAMPS.put("Root water uptake",
                new Color[]{ColorUtilities.fromHex("#edf7e9ff"), ColorUtilities.fromHex("#2e7d32ff")}); // green
    }

    private static final Color ET_COLOR = ColorUtilities.fromHex("#6a3d9aff");
    private static final Color EVAPORATION_COLOR = ColorUtilities.fromHex("#1f9e89ff");
    private static final Color TRANSPIRATION_COLOR = ColorUtilities.fromHex("#33a02cff");

    private static final double RANGE_AXIS_FIXED_DIMENSION = 55;

    private GeospaceStateChartPanelBuilder() {
    }

    public static JPanel build( GeospaceStateChartData data, String title ) {
        JPanel constantRows = new JPanel();
        constantRows.setLayout(new BoxLayout(constantRows, BoxLayout.Y_AXIS));

        DateAxis sharedTimeAxis = new DateAxis("Time");
        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(sharedTimeAxis);
        combinedPlot.setGap(12);
        boolean hasChartRow = false;

        String topLabel = "Top BC";
        double[] topDistinct = distinctSorted(data.topBCTimes.length > 0 ? data.topBCValues : new double[0]);
        if (data.topBCTimes.length > 0 && topDistinct.length > 1) {
            combinedPlot.add(buildBCPlot(topLabel, data.topBCTimes, data.topBCValues, TOP_BC_COLOR), 1);
            hasChartRow = true;
        } else if (topDistinct.length == 1) {
            addConstantValueRow(constantRows, topLabel, topDistinct[0]);
        }

        String bottomLabel = "Bottom BC";
        double[] bottomDistinct = distinctSorted(data.bottomBCTimes.length > 0 ? data.bottomBCValues : new double[0]);
        if (data.bottomBCTimes.length > 0 && bottomDistinct.length > 1) {
            combinedPlot.add(buildBCPlot(bottomLabel, data.bottomBCTimes, data.bottomBCValues, BOTTOM_BC_COLOR), 1);
            hasChartRow = true;
        } else if (bottomDistinct.length == 1) {
            addConstantValueRow(constantRows, bottomLabel, bottomDistinct[0]);
        }

        // GEOET's own optional addition: split evaporation/transpiration when the model produced
        // them (see GeospaceStateChartData.EtSeries), otherwise fall back to the combined total.
        if (data.etSeries != null) {
            GeospaceStateChartData.EtSeries et = data.etSeries;
            if (et.evaporation.length > 0 && et.transpiration.length > 0) {
                hasChartRow |= addLineForcingRow(combinedPlot, constantRows, "Evaporation [mm]", et.times, et.evaporation,
                        EVAPORATION_COLOR);
                hasChartRow |= addLineForcingRow(combinedPlot, constantRows, "Transpiration [mm]", et.times,
                        et.transpiration, TRANSPIRATION_COLOR);
            } else {
                hasChartRow |= addLineForcingRow(combinedPlot, constantRows, "Evapotransp [mm]", et.times,
                        et.evapoTranspiration, ET_COLOR);
            }
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

    private static boolean addLineForcingRow( CombinedDomainXYPlot combinedPlot, JPanel constantRows, String label,
            long[] times, double[] values, Color color ) {
        double[] distinct = distinctSorted(times.length > 0 ? values : new double[0]);
        if (times.length > 0 && distinct.length > 1) {
            combinedPlot.add(buildLinePlot(label, times, values, color), 1);
            return true;
        } else if (distinct.length == 1) {
            addConstantValueRow(constantRows, label, distinct[0]);
        }
        return false;
    }

    private static XYPlot buildLinePlot( String axisLabel, long[] times, double[] values, Color color ) {
        NumberAxis lineAxis = new NumberAxis(axisLabel);
        lineAxis.setAutoRangeIncludesZero(true);
        lineAxis.setLabelPaint(color);
        lineAxis.setTickLabelPaint(color);
        applyUniformAxisSizing(lineAxis);

        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator("{1}:  {2}",
                new SimpleDateFormat("dd-MMM HH:mm"), new DecimalFormat("0.###")));

        XYSeries series = new XYSeries(axisLabel);
        for( int i = 0; i < times.length; i++ ) {
            series.add((double) times[i], values[i]);
        }

        XYPlot plot = new XYPlot();
        plot.setDataset(0, new XYSeriesCollection(series));
        plot.setRenderer(0, renderer);
        plot.setRangeAxis(0, lineAxis);
        plot.mapDatasetToRangeAxis(0, 0);
        return plot;
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

    private static List<LayerBoundary> computeLayerBoundaries( GeospaceStateChartData data ) {
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

    /**
     * Hover tooltip for a heatmap cell: shows the time, depth, and value of the cell, with the time formatted
     * as a date and the value formatted to 3 decimal places. 
     */
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
