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

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.util.List;

import org.hortonmachine.gears.utils.colors.ColorBrewer;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYTextAnnotation;
import org.jfree.chart.axis.LogAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.general.Series;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.TextAnchor;

/**
 * A simple scatter plotter.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Scatter implements IChart {

    protected String xLabel = "X";
    protected String yLabel = "Y";
    protected XYSeriesCollection dataset;
    protected boolean xLog = false;
    protected boolean yLog = false;
    protected List<Boolean> showLines;
    protected List<Boolean> showShapes;
    protected JFreeChart chart;
    protected String title;
    private Color[] colors;

    public Scatter( String title ) {
        this.title = title;
        dataset = new XYSeriesCollection();
    }

    public Scatter() {
        this("Scatter");
    }

    @Override
    public String getTitle() {
        return title;
    }

    /**
     * Add a new series by name and data.
     * 
     * @param seriesName the name.
     * @param x the x data array.
     * @param y the y data array.
     */
    public void addSeries( String seriesName, double[] x, double[] y ) {
        XYSeries series = new XYSeries(seriesName);
        for( int i = 0; i < x.length; i++ ) {
            series.add(x[i], y[i]);
        }
        dataset.addSeries(series);
    }

    public void addSeries( XYSeries series ) {
        dataset.addSeries(series);
    }

    /**
     * Get {@link Series} to be populated.
     * 
     * <p>Teh series is added to the dataset.
     * 
     * @param seriesName the name of the series to add.
     * @return the {@link XYSeries} to use.
     */
    public XYSeries getSeries( String seriesName ) {
        XYSeries series = new XYSeries(seriesName);
        dataset.addSeries(series);
        return series;
    }

    public void setLogAxes( boolean xLog, boolean yLog ) {
        this.xLog = xLog;
        this.yLog = yLog;
    }

    public void setShowLines( List<Boolean> showLines ) {
        this.showLines = showLines;
    }

    public void setShowShapes( List<Boolean> showShapes ) {
        this.showShapes = showShapes;
    }

    public void setXLabel( String xLabel ) {
        this.xLabel = xLabel;
    }

    public void setYLabel( String yLabel ) {
        this.yLabel = yLabel;
    }

    public BufferedImage getImage( int width, int height ) {
        JFreeChart chart = getChart();
        BufferedImage bufferedImage = chart.createBufferedImage(width, height);
        return bufferedImage;
    }

    public JFreeChart getChart() {
        if (chart == null) {
            chart = ChartFactory.createXYLineChart(title, // chart title
                    xLabel,
                    // domain axis label
                    yLabel,
                    // range axis label
                    dataset,
                    // data
                    PlotOrientation.VERTICAL,
                    // orientation
                    true,
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

            if (colors == null) {
                colors = ColorBrewer.getMainColors(dataset.getSeriesCount());
            }
            for( int i = 0; i < colors.length; i++ ) {
                plot.getRenderer().setSeriesPaint(i, colors[i]);
            }

            ValueAxis rangeAxis = plot.getRangeAxis();
            if (rangeAxis instanceof NumberAxis) {
                NumberAxis axis = (NumberAxis) rangeAxis;
                axis.setAutoRangeIncludesZero(false);
            }

            XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
            double x = 1.5;
            double w = x * 2;
            renderer.setSeriesShape(0, new Ellipse2D.Double(-x, x, w, w));
            setShapeLinesVisibility(plot);
        }
        return chart;
    }

    private void setShapeLinesVisibility( XYPlot plot ) {
        XYLineAndShapeRenderer renderer = (XYLineAndShapeRenderer) plot.getRenderer();
        int seriesCount = plot.getSeriesCount();
        for( int i = 0; i < seriesCount; i++ ) {
            if (showShapes != null) {
                renderer.setSeriesShapesVisible(i, showShapes.get(i));
            }
            if (showLines != null) {
                renderer.setSeriesLinesVisible(i, showLines.get(i));
            }
        }
    }

    public void setXRange( double min, double max ) {
        XYPlot plot = (XYPlot) getChart().getPlot();
        ValueAxis domainAxis = plot.getDomainAxis();
        domainAxis.setRange(min, max);
    }

    public void setYRange( double min, double max ) {
        XYPlot plot = (XYPlot) getChart().getPlot();
        ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setRange(min, max);
    }

    public void addAnnotation( String text, double x ) {
        XYPlot plot = (XYPlot) getChart().getPlot();
        Color color = new Color(0, 0, 0, 100);
        Marker updateMarker = new ValueMarker(x, color, new BasicStroke(2f));
        plot.addDomainMarker(updateMarker);
        if (text != null) {
            XYTextAnnotation updateLabel = new XYTextAnnotation(text, x, 0);
            updateLabel.setRotationAnchor(TextAnchor.BASELINE_CENTER);
            updateLabel.setTextAnchor(TextAnchor.BASELINE_CENTER);
            updateLabel.setRotationAngle(-3.14 / 2);
            updateLabel.setPaint(Color.black);
            plot.addAnnotation(updateLabel);
        }
        setShapeLinesVisibility(plot);
    }

    public void setColors( Color[] colors ) {
        this.colors = colors;
    }

    // public static void main( String[] args ) {
    // double[] asd = {1, 2};
    // double[] qwe = {1, 2};
    // Scatter scatter = new Scatter("");
    // scatter.addSeries("asd", asd, qwe);
    // scatter.setShowLines(false);
    // scatter.setXLabel("diameter");
    //
    // PlotFrame frame = new PlotFrame(scatter);
    // frame.plot();
    // }

}
