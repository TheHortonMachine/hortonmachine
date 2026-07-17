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
import org.jfree.chart.axis.AxisLocation;
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
 * Builds the two-panel GeoFrame water budget chart: a top panel with
 * precipitation and temperature on a dual scale, and a bottom panel with
 * simulated vs observed discharge, sharing a common time axis.
 *
 * @author Andrea Antonello
 */
public class GeoframeChartPanelBuilder {
    private static final Color PRECIPITATION_COLOR = ColorUtilities.fromHex("#0096ffff");
    private static final Color TEMPERATURE_COLOR = ColorUtilities.fromHex("#ab080cff");
    private static final Color SIMULATED_DISCHARGE_COLOR = ColorUtilities.fromHex("#009945ff");
    private static final Color OBSERVED_DISCHARGE_COLOR = ColorUtilities.fromHex("#0006ceff");

    private GeoframeChartPanelBuilder() {
    }

    public static JPanel build( GeoframeChartData data, String simTableName ) {
        DateAxis sharedTimeAxis = new DateAxis("Time");

        CombinedDomainXYPlot combinedPlot = new CombinedDomainXYPlot(sharedTimeAxis);
        combinedPlot.add(buildMeteoPlot(data), 1);
        combinedPlot.add(buildDischargePlot(data), 1);
        combinedPlot.setGap(12);

        JFreeChart chart = new JFreeChart(simTableName + " - basin " + data.basinId,
                JFreeChart.DEFAULT_TITLE_FONT, combinedPlot, true);

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

    private static XYPlot buildMeteoPlot( GeoframeChartData data ) {
        NumberAxis precipitationAxis = new NumberAxis("Precipitation (mm)");
        precipitationAxis.setAutoRangeIncludesZero(true);
        precipitationAxis.setLabelPaint(PRECIPITATION_COLOR);
        precipitationAxis.setTickLabelPaint(PRECIPITATION_COLOR);

        NumberAxis temperatureAxis = new NumberAxis("Temperature (°C)");
        temperatureAxis.setAutoRangeIncludesZero(false);
        temperatureAxis.setLabelPaint(TEMPERATURE_COLOR);
        temperatureAxis.setTickLabelPaint(TEMPERATURE_COLOR);

        XYPlot plot = new XYPlot();
        plot.setDataset(0, toBarDataset("Precipitation", data.precipitationTimes, data.precipitationValues));
        plot.setRenderer(0, barRenderer(PRECIPITATION_COLOR));
        plot.setRangeAxis(0, precipitationAxis);
        plot.mapDatasetToRangeAxis(0, 0);

        plot.setDataset(1, toDataset("Temperature", data.temperatureTimes, data.temperatureValues));
        plot.setRenderer(1, lineRenderer(TEMPERATURE_COLOR));
        plot.setRangeAxis(1, temperatureAxis);
        plot.mapDatasetToRangeAxis(1, 1);
        plot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);

        return plot;
    }

    private static XYPlot buildDischargePlot( GeoframeChartData data ) {
        NumberAxis dischargeAxis = new NumberAxis("Discharge (m³/s)");
        dischargeAxis.setAutoRangeIncludesZero(true);

        XYPlot plot = new XYPlot();
        plot.setDataset(0, toDataset("Simulated discharge", data.simulatedDischargeTimes, data.simulatedDischargeValues));
        plot.setRenderer(0, lineRenderer(SIMULATED_DISCHARGE_COLOR));
        plot.setDataset(1, toDataset("Observed discharge", data.observedDischargeTimes, data.observedDischargeValues));
        plot.setRenderer(1, lineRenderer(OBSERVED_DISCHARGE_COLOR));
        plot.setRangeAxis(0, dischargeAxis);
        plot.mapDatasetToRangeAxis(0, 0);
        plot.mapDatasetToRangeAxis(1, 0);

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
