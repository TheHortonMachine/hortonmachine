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

import java.util.Arrays;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * A simple category histogram plotter.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CategoryHistogram implements IChart {

    private String[] categories;
    private List<double[]> values;
    private DefaultCategoryDataset dataset;
    private String title;
    private JFreeChart chart;
    private String yLabel = "Value";
    private String xLabel = "Category";
    private List<String> series;

    public CategoryHistogram( String[] categories, double[] values ) {
        this("Histogram", null, categories, Arrays.asList(values));
    }

    public CategoryHistogram( List<String> series, String[] categories, List<double[]> values ) {
        this("Histogram", series, categories, values);
    }

    public CategoryHistogram( String title, List<String> series, String[] categories, List<double[]> values ) {
        this.title = title;
        this.series = series;
        this.categories = categories;
        this.values = values;

        if (this.series == null) {
            this.series = Arrays.asList("");
        }
    }

    public String getTitle() {
        return title;
    }

    private void createDataset() {
        dataset = new DefaultCategoryDataset();

        for( int j = 0; j < series.size(); j++ ) {

            double[] seriesValues = values.get(j);
            for( int i = 0; i < categories.length; i++ ) {
                dataset.addValue(seriesValues[i], series.get(j), categories[i]);
            }
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
            
            boolean doLegend = series.size() > 1 ? true:false;
            chart = ChartFactory.createBarChart(
                    // chart title
                    title,
                    // domain axis label
                    xLabel,
                    // range axis label
                    yLabel,
                    // data
                    dataset,
                    // orientation
                    PlotOrientation.VERTICAL,
                    // include legend
                    doLegend,
                    // tooltips?
                    true,
                    // URLs?
                    false
            );

            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            CategoryAxis rangeAxis = plot.getDomainAxis();
            rangeAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
        }
        return chart;
    }

    public static void main( String[] args ) {
        String[] asd = {"a", "b", "c", "d", "e", "f", "g"};
        double[] qwe = {1, 2, 3, 2.5, 5.5, 1, 2};
        double[] zxc = {2, 2, 0, 0, 1, 1, 5};
        CategoryHistogram categoryHistogram = new CategoryHistogram("test", Arrays.asList("series A", "series B"), asd,
                Arrays.asList(qwe, zxc));
        categoryHistogram.setXLabel("letters");
        categoryHistogram.setYLabel("numbers");
        PlotFrame frame = new PlotFrame(categoryHistogram);
        frame.setDimension(1600, 1000);
        frame.plot();
    }

}
