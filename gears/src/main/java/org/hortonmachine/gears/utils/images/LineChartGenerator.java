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
package org.hortonmachine.gears.utils.images;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.hortonmachine.gears.utils.files.FileUtilities;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

/**
 * An utility class for simple chart image generation. 
 *
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.3
 */
public class LineChartGenerator {

    private XYSeriesCollection collection = new XYSeriesCollection();
    private final String xLabel;
    private final String yLabel;

    private static final int IMAGEWIDTH = 500;
    private static final int IMAGEHEIGHT = 300;

    private double max = Double.NEGATIVE_INFINITY;
    private double min = Double.POSITIVE_INFINITY;
    private final String title;

    public LineChartGenerator( String title, String xLabel, String yLabel ) {
        this.title = title;
        this.xLabel = xLabel;
        this.yLabel = yLabel;
    }

    public void addDatasetMatrix( double[][] data, String seriesName ) {
        XYSeries series = new XYSeries(seriesName);

        for( int i = 0; i < data.length; i++ ) {
            max = Math.max(max, data[i][1]);
            min = Math.min(min, data[i][1]);
        }

        for( int i = 0; i < data.length; i++ ) {
            series.add(data[i][0], data[i][1]);
        }

        collection.addSeries(series);
    }

    /**
     * Creates the chart image and dumps it to file.
     * 
     * @param chartFile the file to which to write to.
     * @param autoRange flag to define if to auto define the range from the bounds.
     * @param withLegend flag to define the legend presence.
     * @param imageWidth the output image width (if -1 default is used).
     * @param imageHeight the output image height (if -1 default is used).
     * @throws IOException
     */
    @SuppressWarnings("nls")
    public void dumpChart( File chartFile, boolean autoRange, boolean withLegend, int imageWidth, int imageHeight )
            throws IOException {
        JFreeChart chart = ChartFactory.createXYLineChart(title, xLabel, yLabel, collection, PlotOrientation.VERTICAL, withLegend,
                false, false);
        XYPlot plot = (XYPlot) chart.getPlot();
        // plot.setDomainPannable(true);
        // plot.setRangePannable(true);

        // plot.setForegroundAlpha(0.85f);
        NumberAxis yAxis = (NumberAxis) plot.getRangeAxis();
        yAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits());

        if (autoRange) {
            double delta = (max - min) * 0.1;
            yAxis.setRange(min - delta, max + delta);
            // TODO reactivate if newer jfree is used
            // yAxis.setMinorTickCount(4);
            // yAxis.setMinorTickMarksVisible(true);
        }
        // ValueAxis xAxis = plot.getDomainAxis();
        // xAxis.setStandardTickUnits(NumberAxis.createStandardTickUnits(Locale.US));

        // XYItemRenderer renderer = plot.getRenderer();
        // renderer.setDrawBarOutline(false);
        // // flat bars look best...
        // renderer.setBarPainter(new StandardXYBarPainter());
        // renderer.setShadowVisible(false);

        if (!chartFile.getName().endsWith(".png")) {
            chartFile = FileUtilities.substituteExtention(chartFile, "png");
        }
        if (imageWidth == -1) {
            imageWidth = IMAGEWIDTH;
        }
        if (imageHeight == -1) {
            imageHeight = IMAGEHEIGHT;
        }
        BufferedImage bufferedImage = chart.createBufferedImage(imageWidth, imageHeight);
        ImageIO.write(bufferedImage, "png", chartFile);
    }

}
