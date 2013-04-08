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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_UI;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_doChart_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_doCumulate_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_doDump_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_doLegend_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_doNormalize_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_doPoints_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_inChartPath_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_inColors_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_inData_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_inFormats_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_inLabels_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_inSeries_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_inSubTitle_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_inTitle_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_inTypes_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_pHeight_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_pType_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSMATRIXCHARTER_pWidth_DESCRIPTION;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import java.util.List;

import javax.imageio.ImageIO;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
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
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.files.FileUtilities;

@Description(OMSMATRIXCHARTER_DESCRIPTION)
@Documentation(OMSMATRIXCHARTER_DOCUMENTATION)
@Author(name = OMSMATRIXCHARTER_AUTHORNAMES, contact = OMSMATRIXCHARTER_AUTHORCONTACTS)
@Keywords(OMSMATRIXCHARTER_KEYWORDS)
@Label(OMSMATRIXCHARTER_LABEL)
@Name(OMSMATRIXCHARTER_NAME)
@Status(OMSMATRIXCHARTER_STATUS)
@License(OMSMATRIXCHARTER_LICENSE)
@UI(OMSMATRIXCHARTER_UI)
public class OmsMatrixCharter extends JGTModel {

    @Description(OMSMATRIXCHARTER_inData_DESCRIPTION)
    @In
    public double[][] inData;

    @Description("A list of data to chart, in the case the xy data ar different for each series.")
    @In
    public List<double[][]> inDataXY;

    @Description(OMSMATRIXCHARTER_inTitle_DESCRIPTION)
    @In
    public String inTitle;

    @Description(OMSMATRIXCHARTER_inSubTitle_DESCRIPTION)
    @In
    public String inSubTitle;

    @Description(OMSMATRIXCHARTER_inSeries_DESCRIPTION)
    @In
    public String[] inSeries;

    @Description(OMSMATRIXCHARTER_inColors_DESCRIPTION)
    @In
    public String inColors;

    @Description(OMSMATRIXCHARTER_inLabels_DESCRIPTION)
    @In
    public String[] inLabels;

    @Description(OMSMATRIXCHARTER_inFormats_DESCRIPTION)
    @In
    public String[] inFormats;

    @Description(OMSMATRIXCHARTER_inTypes_DESCRIPTION)
    @In
    public String[] inTypes;

    @Description(OMSMATRIXCHARTER_pType_DESCRIPTION)
    @In
    public int pType = 0;;

    @Description(OMSMATRIXCHARTER_doChart_DESCRIPTION)
    @In
    public boolean doChart;

    @Description(OMSMATRIXCHARTER_doDump_DESCRIPTION)
    @In
    public boolean doDump;

    @Description(OMSMATRIXCHARTER_doLegend_DESCRIPTION)
    @In
    public boolean doLegend;

    @Description(OMSMATRIXCHARTER_doPoints_DESCRIPTION)
    @In
    public boolean doPoints;

    @Description(OMSMATRIXCHARTER_doCumulate_DESCRIPTION)
    @In
    public boolean doCumulate;

    @Description(OMSMATRIXCHARTER_doNormalize_DESCRIPTION)
    @In
    public boolean doNormalize;

    @Description("Make chart horizontal.")
    @In
    public boolean doHorizontal;

    @Description(OMSMATRIXCHARTER_pWidth_DESCRIPTION)
    @In
    public int pWidth = 800;

    @Description(OMSMATRIXCHARTER_pHeight_DESCRIPTION)
    @In
    public int pHeight = 600;

    @Description(OMSMATRIXCHARTER_inChartPath_DESCRIPTION)
    @In
    public String inChartPath;

    private double max = Double.NEGATIVE_INFINITY;
    private double min = Double.POSITIVE_INFINITY;
    private double minInterval = 1000;

    @Execute
    public void chart() throws Exception {
        if (inData == null && inDataXY == null) {
            throw new ModelsIllegalargumentException("At least one of the datasets need to be valid.", this);
        }

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
            af.setPreferredSize(new Dimension(pWidth, pHeight));
            af.pack();
            af.setVisible(true);
            RefineryUtilities.centerFrameOnScreen(af);
        }
    }

    private XYSeriesCollection getSeriesCollection() {
        XYSeriesCollection collection = new XYSeriesCollection();
        for( int i = 0; i < inSeries.length; i++ ) {
            int col = i + 1;
            if (inDataXY != null) {
                inData = inDataXY.get(i);
                col = 1;
            }

            String seriesName = inSeries[i];
            XYSeries series = new XYSeries(seriesName);

            double previous = 0;
            double[] x = new double[inData.length];
            double[] y = new double[inData.length];
            for( int j = 0; j < inData.length; j++ ) {
                double value;
                if (!doCumulate) {
                    value = inData[j][col];
                } else {
                    value = previous + inData[j][col];
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
        PlotOrientation orientation = PlotOrientation.VERTICAL;
        if (doHorizontal) {
            orientation = PlotOrientation.HORIZONTAL;
        }
        JFreeChart chart = ChartFactory.createHistogram(inTitle, inLabels[0], inLabels[1], xyBarDataset, orientation, doLegend,
                true, false);
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
        PlotOrientation orientation = PlotOrientation.VERTICAL;
        if (doHorizontal) {
            orientation = PlotOrientation.HORIZONTAL;
        }

        JFreeChart chart = ChartFactory.createXYLineChart(inTitle, inLabels[0], inLabels[1], collection, orientation, doLegend,
                true, false);
        XYPlot plot = (XYPlot) chart.getPlot();

        // plot.setDomainGridlinePaint(Color.red);
        // plot.setRangeGridlinePaint(Color.cyan);
        // plot.setAxisOffset(new RectangleInsets(5.0, 5.0, 5.0, 5.0));
        plot.setDomainCrosshairVisible(true);
        plot.setRangeCrosshairVisible(true);

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
