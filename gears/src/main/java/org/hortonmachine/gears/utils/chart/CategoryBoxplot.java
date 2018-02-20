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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;

/**
 * A simple category boxplot chart.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CategoryBoxplot implements IChart {

    private String[] categories;
    private List<double[]> values;
    private DefaultBoxAndWhiskerCategoryDataset dataset;
    private boolean isMeanVisible;
    private JFreeChart chart;
    private String title;

    public CategoryBoxplot( String[] categories, List<double[]> values, boolean isMeanVisible ) {
        this("Boxplot", categories, values, isMeanVisible);
    }

    public CategoryBoxplot( String title, String[] categories, List<double[]> values, boolean isMeanVisible ) {
        this.title = title;
        this.categories = categories;
        this.values = values;
        this.isMeanVisible = isMeanVisible;
    }

    @Override
    public String getTitle() {
        return title;
    }

    private void createDataset() {
        dataset = new DefaultBoxAndWhiskerCategoryDataset();

        for( int i = 0; i < categories.length; i++ ) {
            final List<Double> list = new ArrayList<Double>();
            double[] catValues = values.get(i);
            for( int j = 0; j < catValues.length; j++ ) {
                list.add(catValues[j]);
            }
            dataset.add(list, "", categories[i]);
        }

    }

    public JFreeChart getChart() {
        if (chart == null) {
            createDataset();
            chart = ChartFactory.createBarChart(getTitle(),
                    // chart title
                    "Category",
                    // domain axis label
                    "Value",
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
            CategoryPlot plot = (CategoryPlot) chart.getPlot();
            CategoryAxis rangeAxis = plot.getDomainAxis();
            rangeAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_90);
            final BoxAndWhiskerRenderer renderer = new BoxAndWhiskerRenderer();
            renderer.setFillBox(true);
            renderer.setToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
            // TODO reactivate if newer jfree is used
            // renderer.setMeanVisible(isMeanVisible);
            plot.setRenderer(renderer);
        }

        return chart;
    }

    public static void main( String[] args ) {
        String[] asd = {"a", "b"};
        double[] qwe1 = {1, 2, 3, 2.5, 5.5, 1, 2};
        double[] qwe2 = {10, 11, 15, 12.5, 15.5, 11, 12};

        List<double[]> asList = Arrays.asList(qwe1, qwe2);
        CategoryBoxplot categoryHistogram = new CategoryBoxplot(asd, asList, false);

        PlotFrame frame = new PlotFrame(categoryHistogram);
        frame.setDimension(1600, 1000);
        frame.plot();
    }

}
