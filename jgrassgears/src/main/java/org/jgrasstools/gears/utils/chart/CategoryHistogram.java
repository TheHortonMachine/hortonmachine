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
import java.awt.image.BufferedImage;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * A simple category histogram plotter.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CategoryHistogram extends ApplicationFrame {

    private String[] categories;
    private double[] values;
    private DefaultCategoryDataset dataset;

    public CategoryHistogram( String[] categories, double[] values ) {
        this("Histogram", categories, values);
    }

    public CategoryHistogram( String title, String[] categories, double[] values ) {
        super(title);
        this.categories = categories;
        this.values = values;
    }

    private void createDataset() {
        dataset = new DefaultCategoryDataset();
        for( int i = 0; i < categories.length; i++ ) {
            dataset.addValue(values[i], "", categories[i]);
        }
    }

    public void plot() {
        createDataset();

        JFreeChart chart = ChartFactory.createBarChart(getTitle(),
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

        ChartPanel chartPanel = new ChartPanel(chart, false);
        chartPanel.setPreferredSize(new Dimension(500, 270));
        setContentPane(chartPanel);

        pack();
        RefineryUtilities.centerFrameOnScreen(this);
        setVisible(true);
    }

    public static void main( String[] args ) {
        String[] asd = {"a", "b", "c", "d", "e", "f", "g"};
        double[] qwe = {1, 2, 3, 2.5, 5.5, 1, 2};
        CategoryHistogram categoryHistogram = new CategoryHistogram(asd, qwe);
        categoryHistogram.plot();
    }

}
