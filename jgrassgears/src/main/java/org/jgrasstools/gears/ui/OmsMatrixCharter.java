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

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;

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
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
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
public class OmsMatrixCharter extends JGTModel {
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

    @Description("The optional data series colors. Format is rbg triplets delimited by semicolon: ex. 0,0,255;0,255,0;255,0,0. The colors have to be the same number as the series.")
    @In
    public String inColors;

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

    @Description("Show shapes in line charts.")
    @In
    public boolean doPoints;

    @Description("Cumulate data.")
    @In
    public boolean doCumulate;

    @Description("Normalize data.")
    @In
    public boolean doNormalize;

    @Description("Chart image width (in case of doDump=true, defaults to 800 px).")
    @In
    public int pWidth = 800;

    @Description("Chart image height (in case of doDump=true, defaults to 600 px).")
    @In
    public int pHeight = 600;

    @Description("Chart dump path (in case of doDump=true).")
    @In
    public String inChartPath;

    private double max = Double.NEGATIVE_INFINITY;
    private double min = Double.POSITIVE_INFINITY;
    private double minInterval = 1000;

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
        if (inSubTitle != null) {
            TextTitle subTitle = new TextTitle(inSubTitle);
            chart.addSubtitle(subTitle);
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

            ApplicationFrame af = new ApplicationFrame("");
            af.setContentPane(cp);
            af.pack();
            af.setVisible(true);
            RefineryUtilities.centerFrameOnScreen(af);
        }
    }

    private XYSeriesCollection getSeriesCollection() {
        XYSeriesCollection collection = new XYSeriesCollection();
        for( int i = 0; i < inSeries.length; i++ ) {
            String seriesName = inSeries[i];
            XYSeries series = new XYSeries(seriesName);

            double previous = 0;
            double[] x = new double[inData.length];
            double[] y = new double[inData.length];
            for( int j = 0; j < inData.length; j++ ) {
                double value;
                if (!doCumulate) {
                    value = inData[j][i + 1];
                } else {
                    value = previous + inData[j][i + 1];
                }
                x[j] = inData[j][0];
                y[j] = value;
                max = Math.max(max, value);
                min = Math.min(min, value);
                previous = value;
                if (j > 1) {
                    minInterval = Math.min(minInterval, inData[j - 1][0] - inData[j][0]);
                }
            }
            if (doNormalize) {
                for( int k = 0; k < y.length; k++ ) {
                    y[k] = y[k] / max;
                }
                max = 1.0;
                min = 0.0;
            }
            for( int k = 0; k < y.length; k++ ) {
                series.add(x[k], y[k]);
            }
            collection.addSeries(series);
        }
        return collection;
    }

    private JFreeChart doBarChart() {
        XYSeriesCollection collection = getSeriesCollection();
        XYBarDataset xyBarDataset = new XYBarDataset(collection, minInterval);
        JFreeChart chart = ChartFactory.createHistogram(inTitle, inLabels[0], inLabels[1], xyBarDataset,
                PlotOrientation.VERTICAL, doLegend, true, false);
        XYPlot plot = (XYPlot) chart.getPlot();
        plot.setForegroundAlpha(0.85f);
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        double delta = (max - min) * 0.1;
        yAxis.setRange(min, max + delta);
        yAxis.setMinorTickCount(4);
        yAxis.setMinorTickMarksVisible(true);
        if (inFormats != null && inFormats.length > 0 && inFormats[1].trim().length() > 0) {
            yAxis.setNumberFormatOverride(new DecimalFormat(inFormats[1]));
        }

        if (inFormats != null && inFormats.length > 0 && inFormats[0].trim().length() > 0) {
            ValueAxis domainAxis = plot.getDomainAxis();
            if (domainAxis instanceof NumberAxis) {
                NumberAxis xAxis = (NumberAxis) domainAxis;
                xAxis.setNumberFormatOverride(new DecimalFormat(inFormats[0]));
            }
        }

        XYBarRenderer renderer = (XYBarRenderer) plot.getRenderer();
        renderer.setDrawBarOutline(false);
        renderer.setBarPainter(new StandardXYBarPainter());
        renderer.setShadowVisible(false);

        if (inColors != null) {
            String[] colorSplit = inColors.split(";");
            for( int i = 0; i < colorSplit.length; i++ ) {
                String[] split = colorSplit[i].split(",");
                int r = (int) Double.parseDouble(split[0]);
                int g = (int) Double.parseDouble(split[1]);
                int b = (int) Double.parseDouble(split[2]);
                renderer.setSeriesPaint(i, new Color(r, g, b));
            }
        }

        return chart;
    }

    @SuppressWarnings("deprecation")
    private JFreeChart doLineChart() {
        XYSeriesCollection collection = getSeriesCollection();
        JFreeChart chart = ChartFactory.createXYLineChart(inTitle, inLabels[0], inLabels[1], collection,
                PlotOrientation.VERTICAL, doLegend, true, false);
        XYPlot plot = (XYPlot) chart.getPlot();

        XYItemRenderer plotRenderer = plot.getRenderer();
        if (plotRenderer instanceof XYLineAndShapeRenderer) {
            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plotRenderer;
            if (doPoints) {
                renderer.setShapesVisible(true);
                renderer.setShapesFilled(true);
            }

            if (inColors != null) {
                String[] colorSplit = inColors.split(";");
                for( int i = 0; i < colorSplit.length; i++ ) {
                    String[] split = colorSplit[i].split(",");
                    int r = (int) Double.parseDouble(split[0]);
                    int g = (int) Double.parseDouble(split[1]);
                    int b = (int) Double.parseDouble(split[2]);
                    renderer.setSeriesPaint(i, new Color(r, g, b));
                }
            }
        }

        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());
        double delta = (max - min) * 0.1;
        yAxis.setRange(min, max + delta);
        yAxis.setMinorTickCount(4);
        yAxis.setMinorTickMarksVisible(true);
        if (inFormats != null && inFormats.length > 1 && inFormats[1].trim().length() > 0) {
            yAxis.setNumberFormatOverride(new DecimalFormat(inFormats[1]));
        }

        if (inFormats != null && inFormats.length > 0 && inFormats[0].trim().length() > 0) {
            ValueAxis domainAxis = plot.getDomainAxis();
            if (domainAxis instanceof NumberAxis) {
                NumberAxis xAxis = (NumberAxis) domainAxis;
                xAxis.setNumberFormatOverride(new DecimalFormat(inFormats[0]));
            }
        }
        return chart;
    }

}
