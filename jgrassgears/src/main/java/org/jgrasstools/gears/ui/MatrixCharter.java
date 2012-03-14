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
package org.jgrasstools.gears.ui;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.files.FileUtilities;

@Description("Utility class for charting matrix data.")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("Viewer, UI, Chart")
@Status(Status.EXPERIMENTAL)
@UI(JGTConstants.HIDE_UI_HINT)
@Name("matrixcharter")
@License("General Public License Version 3 (GPLv3)")
public class MatrixCharter extends JGTModel {
    @Description("The matrix to chart.")
    @In
    public double[][] inData;

    @Description("The data title.")
    @In
    public String inTitle;

    @Description("The subtitle.")
    @In
    public String inSubTitle;

    @Description("The data series names.")
    @In
    public String[] inSeries;

    @Description("The axis labels (x, y1, y2, ...).")
    @In
    public String[] inLabels;

    @Description("The data formats (dates and numeric formatting patterns).")
    @In
    public String[] inFormats;

    @Description("The data types (dates or numerics like double, int).")
    @In
    public String[] inTypes;

    @Description("Chart type: 0 = line, 1 = histogram (default is 0).")
    @In
    public int pType = 0;;

    @Description("Chart the data.")
    @In
    public boolean doChart;

    @Description("Dump the chart to disk.")
    @In
    public boolean doDump;

    @Description("Show the legend.")
    @In
    public boolean doLegend;

    @Description("Chart image width (in case of doDump=true, defaults to 500 px).")
    @In
    public int pWidth = 500;

    @Description("Chart image height (in case of doDump=true, defaults to 300 px).")
    @In
    public int pHeight = 300;

    @Description("Chart dump path (in case of doDump=true).")
    @In
    public String inChartPath;

    private double max = Double.NEGATIVE_INFINITY;
    private double min = Double.POSITIVE_INFINITY;

    @Execute
    public void chart() throws Exception {
        checkNull((Object) inData);

        if (doDump) {
            checkNull(inChartPath);
        }

        JFreeChart chart = null;
        if (pType == 0) {
            chart = doLineChart();
        } else {
            chart = doBarChart();
        }
        chart.setTextAntiAlias(true);

        if (doDump) {
            File chartFile = new File(inChartPath);
            if (!chartFile.getName().endsWith(".png")) {
                chartFile = FileUtilities.substituteExtention(chartFile, "png");
            }
            BufferedImage bufferedImage = chart.createBufferedImage(pWidth, pHeight);
            ImageIO.write(bufferedImage, "png", chartFile);
        }

        if (doChart) {
            ChartPanel cp = new ChartPanel(chart);
            cp.setDomainZoomable(true);
            cp.setRangeZoomable(true);
            cp.setRangeZoomable(true);

            ApplicationFrame af = new ApplicationFrame("");
            af.setContentPane(cp);
            af.pack();
            af.setVisible(true);
            RefineryUtilities.centerFrameOnScreen(af);
        }

    }

    private JFreeChart doBarChart() {
        JFreeChart chart;
        double minInterval = 1000;
        XYSeriesCollection collection = new XYSeriesCollection();
        for( int i = 0; i < inSeries.length; i++ ) {
            String seriesName = inSeries[i];
            XYSeries series = new XYSeries(seriesName);
            for( int j = 0; j < inData.length; j++ ) {
                max = Math.max(max, inData[j][i + 1]);
                min = Math.min(min, inData[j][i + 1]);
                series.add(inData[j][0], inData[j][i + 1]);
            }
            collection.addSeries(series);
            if (i > 1) {
                minInterval = Math.min(minInterval, inData[i + 1][0] - inData[i][0]);
            }
        }

        XYBarDataset xyBarDataset = new XYBarDataset(collection, minInterval);
        chart = ChartFactory.createHistogram(inTitle, inLabels[0], inLabels[1], xyBarDataset, PlotOrientation.VERTICAL, doLegend,
                false, false);
        XYPlot plot = (XYPlot) chart.getPlot();
        // plot.setDomainPannable(true);
        // plot.setRangePannable(true);
        plot.setForegroundAlpha(0.85f);
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        yAxis.setRange(0, 1);
        yAxis.setMinorTickCount(4);
        yAxis.setMinorTickMarksVisible(true);

        // ValueAxis xAxis = plot.getDomainAxis();
        // xAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits(Locale.US));

        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        // flat bars look best...
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);
        return chart;
    }

    private JFreeChart doLineChart() {
        JFreeChart chart;
        XYSeriesCollection collection = new XYSeriesCollection();
        for( int i = 0; i < inSeries.length; i++ ) {
            String seriesName = inSeries[i];
            XYSeries series = new XYSeries(seriesName);
            for( int j = 0; j < inData.length; j++ ) {
                max = Math.max(max, inData[j][i + 1]);
                min = Math.min(min, inData[j][i + 1]);
                series.add(inData[j][0], inData[j][i + 1]);
            }
            collection.addSeries(series);
        }

        chart = ChartFactory.createXYLineChart(inTitle, inLabels[0], inLabels[1], collection, PlotOrientation.VERTICAL, doLegend,
                true, false);
        XYPlot plot = (XYPlot) chart.getPlot();
        // plot.setDomainPannable(true);
        // plot.setRangePannable(true);
        // plot.setForegroundAlpha(0.85f);
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        double delta = (max - min) * 0.1;
        yAxis.setRange(min - delta, max + delta);
        yAxis.setMinorTickCount(4);
        yAxis.setMinorTickMarksVisible(true);

        // ValueAxis xAxis = plot.getDomainAxis();
        // xAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits(Locale.US));

        // XYItemRenderer renderer = plot.getRenderer();
        // renderer.setDrawBarOutline(false);
        // // flat bars look best...
        // renderer.setBarPainter(new StandardXYBarPainter());
        // renderer.setShadowVisible(false);
        return chart;
    }

}
