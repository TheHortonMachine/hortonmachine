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

import java.awt.Dimension;

import javax.swing.WindowConstants;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RefineryUtilities;

/**
 * A frame for jfreecharts.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PlotFrame extends ApplicationFrame {

    private static final long serialVersionUID = 1L;
    private IChart ichart;
    private int chartWidth = 500;
    private int chartHeight = 270;

    public PlotFrame( IChart chart ) {
        super(chart.getTitle());
        this.ichart = chart;
    }

    public void setDimension( int chartWidth, int chartHeight ) {
        this.chartWidth = chartWidth;
        this.chartHeight = chartHeight;
    }

    public void plot() {
        JFreeChart chart = ichart.getChart();
        ChartPanel chartPanel = new ChartPanel(chart, true);
        chartPanel.setPreferredSize(new Dimension(chartWidth, chartHeight));
        setContentPane(chartPanel);

        pack();
        RefineryUtilities.centerFrameOnScreen(this);
        setVisible(true);
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    }

}
