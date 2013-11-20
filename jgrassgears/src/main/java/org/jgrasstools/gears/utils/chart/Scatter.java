/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.utils.chart;

import java.awt.Dimension;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.plot.Plot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.util.LogFormat;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * A simple scatter plotter.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Scatter extends ApplicationFrame {

    private XYSeriesCollection dataset;
    private boolean xLog = false;
    private boolean yLog = false;

    public Scatter( String title ) {
        super(title);
        dataset = new XYSeriesCollection();
    }

    public Scatter() {
        this("Scatter");
    }

    public void addSeries( String seriesName, double[] x, double[] y ) {
        XYSeries series = new XYSeries(seriesName);
        for( int i = 0; i < x.length; i++ ) {
            series.add(x[i], y[i]);
        }
        dataset.addSeries(series);
    }

    public void setLogAxes( boolean xLog, boolean yLog ) {
        this.xLog = xLog;
        this.yLog = yLog;
    }

    public void plot() {

        JFreeChart chart = ChartFactory.createXYLineChart(getTitle(),
        // chart title
                "X",
                // domain axis label
                "Y",
                // range axis label
                dataset,
                // data
                PlotOrientation.VERTICAL,
                // orientation
                false,
                // include legend
                true,
                // tooltips?
                false
        // URLs?
                );

        XYPlot plot = (XYPlot) chart.getPlot();
        if (xLog) {
            LogAxis xAxis = new LogAxis("");
            xAxis.setBase(10);
            plot.setDomainAxis(xAxis);
        }
        if (yLog) {
            LogAxis yAxis = new LogAxis("");
            yAxis.setBase(10);
            plot.setRangeAxis(yAxis);
        }

        ChartPanel chartPanel = new ChartPanel(chart, true);
        chartPanel.setPreferredSize(new Dimension(500, 270));
        setContentPane(chartPanel);

        pack();
        RefineryUtilities.centerFrameOnScreen(this);
        setVisible(true);
    }

    public static void main( String[] args ) {
        double[] asd = {1, 2};
        double[] qwe = {1, 2};
        Scatter scatter = new Scatter();
        scatter.addSeries("asd", asd, qwe);
        scatter.plot();
    }

}
