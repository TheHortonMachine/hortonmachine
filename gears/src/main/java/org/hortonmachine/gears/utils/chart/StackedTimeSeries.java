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
package org.hortonmachine.gears.utils.chart;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;

import org.hortonmachine.gears.utils.colors.ColorBrewer;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.DateAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.CombinedDomainXYPlot;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.time.FixedMillisecond;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * Timeseries charts stacked vertically on one shared time axis.
 *
 * <p>Each panel has its own value axis, so quantities with different units (ex.
 * rainfall and soil water content) can be compared over the same time span.</p>
 *
 * <pre>
 * StackedTimeSeries chart = new StackedTimeSeries("Rain and soil moisture");
 * StackedTimeSeries.Panel rain = chart.addPanel("rain [mm/h]");
 * rain.addSeries("rain", times, rainValues);
 * rain.asBars = true;
 * rain.inverted = true;
 * chart.addPanel("theta [-]").addSeries("theta at 10 cm", times, thetaValues);
 * BufferedImage image = chart.getImage(1200, 800);
 * </pre>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class StackedTimeSeries implements IChart {

    /**
     * One panel of the stack.
     */
    public static class Panel {
        private final String yLabel;
        private final List<String> names = new ArrayList<>();
        private final List<long[]> times = new ArrayList<>();
        private final List<double[]> values = new ArrayList<>();
        /** Draw the series as bars instead of lines, ex. for rainfall. */
        public boolean asBars = false;
        /** Turn the value axis upside down, ex. for rainfall hanging from the top. */
        public boolean inverted = false;
        /** Whether the value axis must include zero. */
        public boolean includeZero = false;
        /** The relative height of the panel. */
        public int weight = 1;
        /** Optional series colors, if <code>null</code> a default palette is used. */
        public Color[] colors;

        private Panel( String yLabel ) {
            this.yLabel = yLabel;
        }

        /**
         * Add a series to the panel.
         *
         * @param name the series name, shown in the legend.
         * @param timestamps the times in epoch milliseconds.
         * @param data the values, same length as the times.
         * @return the panel, to chain calls.
         */
        public Panel addSeries( String name, long[] timestamps, double[] data ) {
            if (timestamps.length != data.length) {
                throw new IllegalArgumentException(
                        "Series " + name + " has " + timestamps.length + " times but " + data.length + " values.");
            }
            names.add(name);
            times.add(timestamps);
            values.add(data);
            return this;
        }
    }

    private final String title;
    private final List<Panel> panels = new ArrayList<>();
    private String xLabel = "Time";
    private TimeZone timeZone = TimeZone.getTimeZone("UTC");
    private JFreeChart chart;

    public StackedTimeSeries( String title ) {
        this.title = title;
    }

    @Override
    public String getTitle() {
        return title;
    }

    public void setXLabel( String xLabel ) {
        this.xLabel = xLabel;
    }

    /**
     * @param timeZone the time zone the time axis is labeled in, UTC by default.
     */
    public void setTimeZone( TimeZone timeZone ) {
        this.timeZone = timeZone;
    }

    /**
     * Add a new panel at the bottom of the stack.
     *
     * @param yLabel the label of the panel's value axis.
     * @return the panel, to add series to.
     */
    public Panel addPanel( String yLabel ) {
        Panel panel = new Panel(yLabel);
        panels.add(panel);
        return panel;
    }

    @Override
    public JFreeChart getChart() {
        if (chart == null) {
            DateAxis timeAxis = new DateAxis(xLabel);
            timeAxis.setTimeZone(timeZone);
            CombinedDomainXYPlot plot = new CombinedDomainXYPlot(timeAxis);
            plot.setGap(8);
            // one palette across all panels, so that every series has its own color in the shared legend
            int seriesCount = panels.stream().mapToInt(p -> p.names.size()).sum();
            Color[] palette = ColorBrewer.getPairedColors(Math.max(1, seriesCount));
            int paletteIndex = 0;
            for( Panel panel : panels ) {
                plot.add(makePlot(panel, palette, paletteIndex), panel.weight);
                paletteIndex += panel.names.size();
            }
            chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
        }
        return chart;
    }

    private XYPlot makePlot( Panel panel, Color[] palette, int paletteIndex ) {
        TimeSeriesCollection dataset = new TimeSeriesCollection(timeZone);
        for( int i = 0; i < panel.names.size(); i++ ) {
            org.jfree.data.time.TimeSeries series = new org.jfree.data.time.TimeSeries(panel.names.get(i));
            long[] ts = panel.times.get(i);
            double[] vs = panel.values.get(i);
            for( int j = 0; j < ts.length; j++ ) {
                series.addOrUpdate(new FixedMillisecond(ts[j]), vs[j]);
            }
            dataset.addSeries(series);
        }

        XYItemRenderer renderer;
        if (panel.asBars) {
            XYBarRenderer barRenderer = new XYBarRenderer();
            barRenderer.setDrawBarOutline(false);
            renderer = barRenderer;
        } else {
            renderer = new XYLineAndShapeRenderer(true, false);
        }
        for( int i = 0; i < panel.names.size(); i++ ) {
            Color color;
            if (panel.colors != null && i < panel.colors.length) {
                color = panel.colors[i];
            } else {
                color = palette[(paletteIndex + i) % palette.length];
            }
            renderer.setSeriesPaint(i, color);
        }

        NumberAxis valueAxis = new NumberAxis(panel.yLabel);
        valueAxis.setAutoRangeIncludesZero(panel.includeZero || panel.asBars);
        valueAxis.setInverted(panel.inverted);
        return new XYPlot(dataset, null, valueAxis, renderer);
    }
}
