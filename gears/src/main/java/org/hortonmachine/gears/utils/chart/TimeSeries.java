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

import java.util.Date;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.data.time.Day;
import org.jfree.data.time.TimeSeriesCollection;

/**
 * A simple timeseries chart plotter.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TimeSeries implements IChart {

    private List<long[]> timesList;
    private List<double[]> valuesList;
    private TimeSeriesCollection dataset = new TimeSeriesCollection();
    private String title;
    private JFreeChart chart;
    private String yLabel = "Value";
    private String xLabel = "Time";
    private List<String> seriesNames;

    public TimeSeries( List<String> seriesNames, List<long[]> times, List<double[]> values ) {
        this("Time Series", seriesNames, times, values);
    }

    public TimeSeries( String title, List<String> seriesNames, List<long[]> times, List<double[]> values ) {
        this.title = title;
        this.timesList = times;
        this.valuesList = values;
        this.seriesNames = seriesNames;
    }

    public String getTitle() {
        return title;
    }

    private void createDataset() {
        for( int i = 0; i < timesList.size(); i++ ) {
            long[] ts = timesList.get(i);
            double[] values = valuesList.get(i);
            String name = seriesNames.get(i);
            org.jfree.data.time.TimeSeries series = new org.jfree.data.time.TimeSeries(name);
            for( int j = 0; j < ts.length; j++ ) {
                series.add(new Day(new Date(ts[j])), values[j]);
            }
            dataset.addSeries(series);
        }
    }

    public void setXLabel( String xLabel ) {
        this.xLabel = xLabel;
    }

    public void setYLabel( String yLabel ) {
        this.yLabel = yLabel;
    }

    public JFreeChart getChart() {
        if (chart == null) {
            createDataset();
            chart = ChartFactory.createTimeSeriesChart(title,
                    // chart title
                    xLabel,
                    // domain axis label
                    yLabel,
                    // range axis label
                    dataset,
                    // data
                    true,
                    // include legend
                    true,
                    // tooltips?
                    false
            // URLs?
            );
        }
        return chart;
    }
}
