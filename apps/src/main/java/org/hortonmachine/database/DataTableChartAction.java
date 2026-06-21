package org.hortonmachine.database;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Paint;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;

import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.gears.utils.CyclicSupplier;
import org.hortonmachine.gears.utils.chart.CategoryHistogram;
import org.hortonmachine.gears.utils.chart.Scatter;
import org.hortonmachine.gears.utils.chart.TimeSeries;
import org.hortonmachine.gears.utils.colors.DefaultTables;
import org.hortonmachine.gears.utils.colors.EColorTables;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.AbstractRenderer;
import org.jfree.chart.renderer.xy.XYItemRenderer;

/**
 * Action that builds a chart from selected table cells and opens it in a dialog.
 *
 * <p>The first selected column is used as the X axis; all remaining selected
 * columns become Y series.  The dialog includes a scrollable legend panel so
 * that many series never overflow the chart area, and the chart re-renders at
 * full resolution on window resize instead of scaling a fixed bitmap.
 */
public class DataTableChartAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final JTable table;
    private final int[] selectedCols;
    private final int[] selectedRows;
    private final String timePatterns;
    private final Component parent;

    public DataTableChartAction( JTable table, int[] selectedCols, int[] selectedRows,
            String timePatterns, Component parent ) {
        super("Chart values");
        this.table = table;
        this.selectedCols = selectedCols;
        this.selectedRows = selectedRows;
        this.timePatterns = timePatterns;
        this.parent = parent;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        try {
            int chartsCount = selectedCols.length - 1;
            String xLabel = table.getColumnName(selectedCols[0]);

            Scatter scatterChart = null;
            CategoryHistogram categoryHistogram = null;
            TimeSeries timeSeriesChart = null;
            List<double[]> timeSeriesValuesList = new ArrayList<>();
            List<long[]> timeSeriesTimesList = new ArrayList<>();
            List<String> timeSeriesNames = new ArrayList<>();
            List<String> scatterSeriesNames = new ArrayList<>();

            for( int i = 0; i < chartsCount; i++ ) {
                Object tmpX = table.getValueAt(0, selectedCols[0]);
                boolean doCat = !(tmpX instanceof Number);
                boolean doTime = timePatterns.contains(xLabel);

                Object tmpY = table.getValueAt(0, selectedCols[i + 1]);
                if (!(tmpY instanceof Number)) {
                    break;
                }

                if (doTime) {
                    int index = 0;
                    long[] x = new long[selectedRows.length];
                    double[] y = new double[selectedRows.length];
                    long previousTime = -1;
                    for( int r : selectedRows ) {
                        Object xObj = table.getValueAt(r, selectedCols[0]);
                        Object yObj = table.getValueAt(r, selectedCols[i + 1]);
                        long tmp;
                        if (xObj instanceof Number) {
                            tmp = ((Number) xObj).longValue();
                            if (tmp == previousTime) tmp += 1;
                            previousTime = tmp;
                        } else {
                            tmp = DbsUtilities.dbDateFormatter.parse(xObj.toString()).getTime();
                            if (tmp == previousTime) tmp += 1;
                            previousTime = tmp;
                        }
                        x[index] = tmp;
                        y[index] = ((Number) yObj).doubleValue();
                        index++;
                    }
                    timeSeriesTimesList.add(x);
                    timeSeriesValuesList.add(y);
                    timeSeriesNames.add(table.getColumnName(selectedCols[i + 1]));
                } else if (doCat) {
                    if (categoryHistogram == null) {
                        String[] xStr = new String[selectedRows.length];
                        double[] y = new double[selectedRows.length];
                        int index = 0;
                        for( int r : selectedRows ) {
                            Object xObj = table.getValueAt(r, selectedCols[0]);
                            Object yObj = table.getValueAt(r, selectedCols[i + 1]);
                            xStr[index] = xObj.toString();
                            y[index] = ((Number) yObj).doubleValue();
                            index++;
                        }
                        categoryHistogram = new CategoryHistogram(xStr, y);
                    }
                } else {
                    if (scatterChart == null) {
                        scatterChart = new Scatter("");
                        List<Boolean> showLines = new ArrayList<>();
                        for( int j = 0; j < chartsCount; j++ ) {
                            showLines.add(true);
                        }
                        scatterChart.setShowLines(showLines);
                        scatterChart.setXLabel(xLabel);
                        scatterChart.setYLabel("");
                    }
                    double[] x = new double[selectedRows.length];
                    double[] y = new double[selectedRows.length];
                    String seriesName = table.getColumnName(selectedCols[i + 1]);
                    scatterSeriesNames.add(seriesName);
                    int index = 0;
                    for( int r : selectedRows ) {
                        Object xObj = table.getValueAt(r, selectedCols[0]);
                        Object yObj = table.getValueAt(r, selectedCols[i + 1]);
                        x[index] = ((Number) xObj).doubleValue();
                        y[index] = ((Number) yObj).doubleValue();
                        index++;
                    }
                    scatterChart.addSeries(seriesName, x, y);
                }
            }

            Color[] timeColors = null;
            if (!timeSeriesNames.isEmpty()) {
                timeSeriesChart = new TimeSeries("", timeSeriesNames, timeSeriesTimesList, timeSeriesValuesList);
                timeSeriesChart.setXLabel(xLabel);
                timeSeriesChart.setYLabel("");
                List<Color> colorList = new ArrayList<>();
                colorList.add(Color.BLUE);                   // 1. blue
                colorList.add(Color.RED);                    // 2. red
                colorList.add(Color.GREEN);                  // 3. green
                colorList.add(new Color(255, 127,   0));     // 4. orange
                colorList.add(new Color(148,   0, 211));     // 5. violet
                colorList.add(new Color(  0, 206, 209));     // 6. dark turquoise
                colorList.add(new Color(255, 215,   0));     // 7. gold
                colorList.add(new Color(255,  20, 147));     // 8. deep pink
                colorList.add(new Color(139,  69,  19));     // 9. saddle brown
                colorList.add(new Color( 64,  64,  64));     // 10. dark gray

                List<Color> tableColors = new DefaultTables().getTableColors(EColorTables.contrasting130.name());
                int tableColorCount = tableColors.size();
                // stride by ~tableColorCount/4 so early picks come from different visual regions
                // tableColorCount=131 is prime so any stride 1..130 visits all entries exactly once
                int colorStride = Math.max(1, tableColorCount / 4);
                for (int ci = 0; ci < tableColorCount; ci++) {
                    colorList.add(tableColors.get((ci * colorStride) % tableColorCount));
                }
                var cyclicColors = new CyclicSupplier<Color>(colorList);
                timeColors = new Color[timeSeriesNames.size()];
                for( int i = 0; i < timeSeriesNames.size(); i++ ) {
                    timeColors[i] = cyclicColors.next();
                }
                timeSeriesChart.setColors(timeColors);
            }

            JFreeChart chart = null;
            List<String> legendNames = null;
            Color[] legendColors = null;

            if (scatterChart != null) {
                chart = scatterChart.getChart();
                legendNames = scatterSeriesNames;
            } else if (categoryHistogram != null) {
                chart = categoryHistogram.getChart();
            } else if (timeSeriesChart != null) {
                chart = timeSeriesChart.getChart();
                legendNames = timeSeriesNames;
                legendColors = timeColors;
            } else {
                GuiUtilities.showWarningMessage(parent, "Charting of selected data not possible.");
                return;
            }

            if (chart != null) {
                ChartPanel chartPanel = new ChartPanel(chart, true);
                // re-render at actual size on every resize instead of scaling the buffer
                chartPanel.setMaximumDrawWidth(Integer.MAX_VALUE);
                chartPanel.setMaximumDrawHeight(Integer.MAX_VALUE);
                chartPanel.setMinimumDrawWidth(0);
                chartPanel.setMinimumDrawHeight(0);

                JPanel p = new JPanel(new BorderLayout());
                p.add(chartPanel, BorderLayout.CENTER);

                if (legendNames != null && !legendNames.isEmpty()) {
                    p.add(buildLegendPanel(chart, legendNames, legendColors), BorderLayout.EAST);
                    chart.removeLegend();
                }

                Dimension dimension = new Dimension(1100, 800);
                GuiUtilities.openDialogWithPanel(p, "Chart from cells", dimension, false);
            }
        } catch (Exception ex) {
            Logger.INSTANCE.insertError("", "ERROR", ex);
            GuiUtilities.showErrorMessage(parent, ex.getMessage());
        }
    }

    private JScrollPane buildLegendPanel( JFreeChart chart, List<String> names, Color[] explicitColors ) {
        JPanel items = new JPanel();
        items.setLayout(new BoxLayout(items, BoxLayout.Y_AXIS));
        items.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        for( int i = 0; i < names.size(); i++ ) {
            Color c = Color.DARK_GRAY;
            if (explicitColors != null && i < explicitColors.length) {
                c = explicitColors[i];
            } else if (chart.getPlot() instanceof XYPlot) {
                XYItemRenderer renderer = ((XYPlot) chart.getPlot()).getRenderer();
                if (renderer instanceof AbstractRenderer) {
                    Paint paint = ((AbstractRenderer) renderer).lookupSeriesPaint(i);
                    if (paint instanceof Color) c = (Color) paint;
                }
            }
            String hex = String.format("#%02x%02x%02x", c.getRed(), c.getGreen(), c.getBlue());
            JLabel entry = new JLabel(
                    "<html><font color='" + hex + "'>&#9632;</font>&nbsp;" + names.get(i) + "</html>");
            entry.setAlignmentX(Component.LEFT_ALIGNMENT);
            entry.setBorder(BorderFactory.createEmptyBorder(1, 0, 1, 0));
            items.add(entry);
        }
        items.add(Box.createVerticalGlue());

        JScrollPane scroll = new JScrollPane(items,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setBorder(BorderFactory.createTitledBorder("Series"));
        scroll.setPreferredSize(new Dimension(210, 100));
        return scroll;
    }
}
