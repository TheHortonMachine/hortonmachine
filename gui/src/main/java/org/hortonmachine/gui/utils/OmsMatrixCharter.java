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
package org.hortonmachine.gui.utils;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_DO_CHART_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_DO_CUMULATE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_DO_DUMP_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_DO_LEGEND_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_DO_NORMALIZE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_DO_POINTS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_IN_CHARTPATH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_IN_COLORS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_IN_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_IN_FORMATS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_IN_LABELS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_IN_SERIES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_IN_SUBTITLE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_IN_TITLE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_IN_TYPES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_P_HEIGHT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_P_TYPE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_P_WIDTH_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSMATRIXCHARTER_UI;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.KeyStroke;

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.xy.XYBarDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

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

@Description(OMSMATRIXCHARTER_DESCRIPTION)
@Documentation(OMSMATRIXCHARTER_DOCUMENTATION)
@Author(name = OMSMATRIXCHARTER_AUTHORNAMES, contact = OMSMATRIXCHARTER_AUTHORCONTACTS)
@Keywords(OMSMATRIXCHARTER_KEYWORDS)
@Label(OMSMATRIXCHARTER_LABEL)
@Name(OMSMATRIXCHARTER_NAME)
@Status(OMSMATRIXCHARTER_STATUS)
@License(OMSMATRIXCHARTER_LICENSE)
@UI(OMSMATRIXCHARTER_UI)
public class OmsMatrixCharter extends HMModel {

    @Description(OMSMATRIXCHARTER_IN_DATA_DESCRIPTION)
    @In
    public double[][] inData;

    @Description("A list of data to chart, in the case the xy data are different for each series.")
    @In
    public List<double[][]> inDataXY;

    @Description(OMSMATRIXCHARTER_IN_TITLE_DESCRIPTION)
    @In
    public String inTitle;

    @Description(OMSMATRIXCHARTER_IN_SUBTITLE_DESCRIPTION)
    @In
    public String inSubTitle;

    @Description(OMSMATRIXCHARTER_IN_SERIES_DESCRIPTION)
    @In
    public String[] inSeries;

    @Description(OMSMATRIXCHARTER_IN_COLORS_DESCRIPTION)
    @In
    public String inColors;

    @Description(OMSMATRIXCHARTER_IN_LABELS_DESCRIPTION)
    @In
    public String[] inLabels;

    @Description(OMSMATRIXCHARTER_IN_FORMATS_DESCRIPTION)
    @In
    public String[] inFormats;

    @Description(OMSMATRIXCHARTER_IN_TYPES_DESCRIPTION)
    @In
    public String[] inTypes;

    @Description(OMSMATRIXCHARTER_P_TYPE_DESCRIPTION)
    @In
    public int pType = 0;

    @Description(OMSMATRIXCHARTER_DO_CHART_DESCRIPTION)
    @In
    public boolean doChart;

    @Description(OMSMATRIXCHARTER_DO_DUMP_DESCRIPTION)
    @In
    public boolean doDump;

    @Description(OMSMATRIXCHARTER_DO_LEGEND_DESCRIPTION)
    @In
    public boolean doLegend;

    @Description(OMSMATRIXCHARTER_DO_POINTS_DESCRIPTION)
    @In
    public boolean doPoints;

    @Description(OMSMATRIXCHARTER_DO_CUMULATE_DESCRIPTION)
    @In
    public boolean doCumulate;

    @Description(OMSMATRIXCHARTER_DO_NORMALIZE_DESCRIPTION)
    @In
    public boolean doNormalize;

    @Description("Make chart horizontal.")
    @In
    public boolean doHorizontal;

    @Description(OMSMATRIXCHARTER_P_WIDTH_DESCRIPTION)
    @In
    public int pWidth = 800;

    @Description(OMSMATRIXCHARTER_P_HEIGHT_DESCRIPTION)
    @In
    public int pHeight = 600;

    @Description(OMSMATRIXCHARTER_IN_CHARTPATH_DESCRIPTION)
    @In
    public String inChartPath;

    private double max = Double.NEGATIVE_INFINITY;
    private double min = Double.POSITIVE_INFINITY;
    private double minInterval = 1000;

    @Execute
    public void chart() throws Exception {
        if (inData == null && inDataXY == null) {
            throw new ModelsIllegalargumentException("At least one of the datasets need to be valid.", this, pm);
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

            GuiUtilities.openDialogWithPanel(cp, "", new Dimension(pWidth, pHeight), false);

//            ApplicationFrame af = new ApplicationFrame("");
//            af.setContentPane(cp);
//            af.setPreferredSize(new Dimension(pWidth, pHeight));
//            af.pack();
//            af.setVisible(true);
//            RefineryUtilities.centerFrameOnScreen(af);
//            af.setDefaultCloseOperation(ApplicationFrame.DISPOSE_ON_CLOSE);
        }
    }

    private XYSeriesCollection getSeriesCollection() {
        List<XYSeries> seriesList = new ArrayList<>();
        for( int i = 0; i < inSeries.length; i++ ) {
            int col = i + 1;
            if (inDataXY != null) {
                inData = inDataXY.get(i);
                col = 1;
            }

            String seriesName = inSeries[i];
            XYSeries series = new XYSeries(seriesName);

            double previous = 0;
            int length = inData.length;
            double[] x = new double[length];
            double[] y = new double[length];
            for( int row = 0; row < length; row++ ) {
                double value;
                if (!doCumulate) {
                    value = inData[row][col];
                } else {
                    value = previous + inData[row][col];
                }
                x[row] = inData[row][0];
                y[row] = value;

                max = Math.max(max, value);
                min = Math.min(min, value);
                previous = value;
                if (row > 1) {
                    minInterval = Math.min(minInterval, inData[row - 1][0] - inData[row][0]);
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
            seriesList.add(series);
        }

//        Collections.reverse(seriesList);

        XYSeriesCollection collection = new XYSeriesCollection();
        for( XYSeries xySeries : seriesList ) {
            collection.addSeries(xySeries);
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
        // TODO reactivate if newer jfree is used
        // yAxis.setMinorTickCount(4);
        // yAxis.setMinorTickMarksVisible(true);
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
        // TODO reactivate if newer jfree is used
        // renderer.setBarPainter(new StandardXYBarPainter());
        // renderer.setShadowVisible(false);

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
        // TODO reactivate if newer jfree is used
        // yAxis.setMinorTickCount(4);
        // yAxis.setMinorTickMarksVisible(true);
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
