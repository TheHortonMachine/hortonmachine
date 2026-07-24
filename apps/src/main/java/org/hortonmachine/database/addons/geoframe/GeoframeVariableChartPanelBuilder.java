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
package org.hortonmachine.database.addons.geoframe;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;

import javax.swing.JPanel;

import org.hortonmachine.gears.utils.colors.ColorUtilities;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeriesCollection;
import org.jfree.data.xy.DefaultIntervalXYDataset;
import org.jfree.data.xy.IntervalXYDataset;

/**
 * Builds the GeoFrame variable chart shared by the station data and basin data chart actions:
 * one sub-plot per environmental variable (var_id) found for the selected entity, stacked on a
 * shared time axis. Precipitation is rendered as a bar chart, every other variable as a
 * line/time series.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeVariableChartPanelBuilder {
    private static final Color COLOR_TEMPERATURE = ColorUtilities.fromHex("#ab080cff");
    private static final Color COLOR_PRECIPITATION = ColorUtilities.fromHex("#0096ffff");
    private static final Color COLOR_EVAPOTRANSPIRATION = ColorUtilities.fromHex("#009945ff");
    private static final Color COLOR_RADIATION = ColorUtilities.fromHex("#8f00b3ff");
    /** Colors for any variable other than the four fixed ones above, cycled in order. */
    private static final Color[] FALLBACK_COLORS = {ColorUtilities.fromHex("#ff8c00ff"), ColorUtilities.fromHex("#00b3b3ff")};

    private GeoframeVariableChartPanelBuilder() {
    }

    public static JPanel build( GeoframeVariableChartData data ) {
        DateAxis sharedTimeAxis = new DateAxis("Time");

        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(sharedTimeAxis);
        int fallbackIndex = 0;
        for( int i = 0; i < data.variableSeries.size(); i++ ) {
            GeoframeVariableChartData.VariableSeries series = data.variableSeries.get(i);
            Color color;
            if (series.varId == GeoframeSchema.VAR_TEMPERATURE) {
                color = COLOR_TEMPERATURE;
            } else if (series.varId == GeoframeSchema.VAR_PRECIPITATION) {
                color = COLOR_PRECIPITATION;
            } else if (series.varId == GeoframeSchema.VAR_EVAPOTRANSPIRATION) {
                color = COLOR_EVAPOTRANSPIRATION;
            } else if (series.varId == GeoframeSchema.VAR_RADIATION) {
                color = COLOR_RADIATION;
            } else {
                color = FALLBACK_COLORS[fallbackIndex % FALLBACK_COLORS.length];
                fallbackIndex++;
            }
            combinedPlot.add(buildVariablePlot(series, color), 1);
        }
        combinedPlot.setGap(12);

        JFreeChart chart = new JFreeChart(data.title, JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);

        ChartPanel chartPanel = new ChartPanel(chart, true);
        // re-render at actual size on every resize instead of scaling the buffer
        chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
        chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
        chartPanel.setMinimumDrawWidth(0);
        chartPanel.setMinimumDrawHeight(0);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(chartPanel, BorderLayout.CENTER);
        return panel;
    }

    private static XYPlot buildVariablePlot( GeoframeVariableChartData.VariableSeries series, Color color ) {
        String label = series.name;
        String axisLabel = series.unit != null && !series.unit.isEmpty() ? label + " (" + series.unit + ")" : label;

        NumberAxis axis = new NumberAxis(axisLabel);
        boolean isPrecipitation = series.varId == GeoframeSchema.VAR_PRECIPITATION;
        axis.setAutoRangeIncludesZero(isPrecipitation);
        axis.setLabelPaint(color);
        axis.setTickLabelPaint(color);

        XYPlot plot = new XYPlot();
        if (isPrecipitation) {
            plot.setDataset(0, toBarDataset(label, series.times, series.values));
            plot.setRenderer(0, barRenderer(color));
        } else {
            plot.setDataset(0, toDataset(label, series.times, series.values));
            plot.setRenderer(0, lineRenderer(color));
        }
        plot.setRangeAxis(0, axis);
        plot.mapDatasetToRangeAxis(0, 0);
        return plot;
    }

    private static XYLineAndShapeRenderer lineRenderer( Color color ) {
        XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer(true, false);
        renderer.setSeriesPaint(0, color);
        renderer.setSeriesStroke(0, new BasicStroke(1.5f));
        return renderer;
    }

    private static XYBarRenderer barRenderer( Color color ) {
        XYBarRenderer renderer = new XYBarRenderer();
        renderer.setSeriesPaint(0, color);
        renderer.setUseYInterval(false);
        renderer.setDrawBarOutline(false);
        renderer.setMargin(0.1);
        return renderer;
    }

    private static TimeSeriesCollection toDataset( String name, long[] times, double[] values ) {
        org.jfree.data.time.TimeSeries series = new org.jfree.data.time.TimeSeries(name);
        for( int i = 0; i < times.length; i++ ) {
            series.addOrUpdate(new FixedMillisecond(times[i]), values[i]);
        }
        return new TimeSeriesCollection(series);
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
}
