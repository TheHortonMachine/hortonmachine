package oms3.ngmf.ui.graph;
///*
// * To change this template, choose Tools | Templates
// * and open the template in the editor.
// */
//package ngmf.ui.graph;
//
//import java.awt.Color;
//import java.io.File;
//import java.io.FileReader;
//import java.io.IOException;
//import java.util.ArrayList;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.GregorianCalendar;
//import java.util.List;
//import javax.swing.JComponent;
//import oms3.Conversions;
//import oms3.io.CSProperties;
//import oms3.io.CSTable;
//import oms3.io.DataIO;
//import org.jfree.chart.ChartFactory;
//import org.jfree.chart.ChartPanel;
//import org.jfree.chart.JFreeChart;
//import org.jfree.chart.axis.AxisLocation;
//import org.jfree.chart.axis.DateAxis;
//import org.jfree.chart.axis.NumberAxis;
//import org.jfree.chart.labels.StandardXYToolTipGenerator;
//import org.jfree.chart.plot.CombinedDomainXYPlot;
//import org.jfree.chart.plot.PlotOrientation;
//import org.jfree.chart.plot.XYPlot;
//import org.jfree.chart.renderer.xy.StandardXYItemRenderer;
//import org.jfree.chart.renderer.xy.XYItemRenderer;
//import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
//import org.jfree.data.general.SeriesException;
//import org.jfree.data.time.TimeSeries;
//import org.jfree.data.time.TimeSeriesCollection;
//import org.jfree.data.xy.XYDataset;
//import org.jfree.data.time.Millisecond;
//import org.jfree.data.xy.XYSeries;
//import org.jfree.data.xy.XYSeriesCollection;
//import org.omscentral.modules.analysis.esp.ESPTimeSeries;
//import org.omscentral.modules.analysis.esp.ESPToolPanel;
//import org.omscentral.modules.analysis.esp.EnsembleData;
//import org.omscentral.modules.analysis.esp.ModelDateTime;
//
///**
// *
// * @author od
// */
//public class PlotView {
//
//    static XYDataset createDatasetCombined(Date[] date, List<String> label, List<Double[]> vals) {
//        TimeSeriesCollection tsc = new TimeSeriesCollection();
//
//        for (int i = 0; i < label.size(); i++) {
//            String n = label.get(i);
//            Double[] v = vals.get(i);
//
//            TimeSeries series = new TimeSeries(n, Millisecond.class);
//            for (int j = 0; j < v.length; j++) {
//                try {
////                    series.add(new Millisecond(date[j]), v[j]);
//                     series.add(new Millisecond(date[j]), v[j] == -9999 ? null : v[j]);
//                } catch (SeriesException E) {
//                    System.err.println("Error adding to series " + date[j] + " " + v[j]);
//                }
//            }
//            tsc.addSeries(series);
//        }
//        return tsc;
//    }
//
//    static XYDataset createDataset(Date[] date, List<String> label, List<Double[]> vals) {
//        TimeSeriesCollection tsc = new TimeSeriesCollection();
//        for (int i = 0; i < label.size(); i++) {
//            String n = label.get(i);
//            Double[] v = vals.get(i);
//            TimeSeries series = new TimeSeries(n, Millisecond.class);
//            for (int j = 0; j < v.length; j++) {
//                try {
////                    series.add(new Millisecond(date[j]), v[j]);
//                     series.add(new Millisecond(date[j]), v[j] == -9999 ? null : v[j]);
//                } catch (SeriesException E) {
//                    System.err.println("Error adding to series " + date[j] + " " + v[j]);
//                }
//            }
//            tsc.addSeries(series);
//        }
//        return tsc;
//    }
//    static Color[] col = {Color.RED, Color.BLUE, Color.GREEN, Color.PINK, Color.MAGENTA, Color.YELLOW, Color.ORANGE};
//
//    static XYDataset[] createDatasets(Date[] date, List<String> label, List<Double[]> vals) {
//        XYDataset[] sets = new XYDataset[vals.size()];
//        for (int i = 0; i < sets.length; i++) {
//            sets[i] = new TimeSeriesCollection();
//            String n = label.get(i);
//            Double[] v = vals.get(i);
//            TimeSeries series = new TimeSeries(n, Millisecond.class);
//            for (int j = 0; j < v.length; j++) {
//                try {
////                    series.add(new Millisecond(date[j]), v[j]);
//                    series.add(new Millisecond(date[j]), v[j] == -9999 ? null : v[j]);
//                } catch (SeriesException E) {
//                    System.err.println("Error adding to series " + date[j] + " " + v[j]);
//                }
//            }
//            ((TimeSeriesCollection) sets[i]).addSeries(series);
//        }
//        return sets;
//    }
//
//    static XYDataset createXYDataset(Integer[] x, List<String> label, List<Double[]> vals) {
//        XYSeriesCollection tsc = new XYSeriesCollection();
//        for (int i = 0; i < label.size(); i++) {
//            String n = label.get(i);
//            Double[] v = vals.get(i);
//            XYSeries series = new XYSeries(n);
//            for (int j = 0; j < v.length; j++) {
//                try {
////                    series.add(x[j], v[j]);
//                     series.add(x[j], v[j] == -9999 ? null : v[j]);
//                } catch (SeriesException E) {
//                    System.err.println("Error adding to series " + x[j] + " " + v[j]);
//                }
//            }
//            tsc.addSeries(series);
//        }
//        return tsc;
//    }
//
//    static XYDataset createXYScatterDataset(Double[] x, Double[] y) {
//        XYSeriesCollection tsc = new XYSeriesCollection();
//        XYSeries series = new XYSeries("");
//        for (int j = 0; j < x.length; j++) {
//            try {
////                series.add(x[j], y[j]);
//                     series.add(x[j], y[j] == -9999 ? null : y[j]);
//            } catch (SeriesException E) {
//                System.err.println("Error adding to series " + x[j] + " " + y[j]);
//            }
//        }
//        tsc.addSeries(series);
//        return tsc;
//    }
//
//    static JComponent comp(JFreeChart chart) {
//        chart.getPlot().setBackgroundPaint(new Color(233, 232, 226));
//        chart.setTextAntiAlias(true);
//        ChartPanel cp = new ChartPanel(chart);
//        cp.setDomainZoomable(true);
//        cp.setRangeZoomable(true);
//        return cp;
//    }
//
//    public static JComponent createTSChart(String title, Date[] date, List<String> label,
//            List<Double[]> vals, int view, List<ValueSet> y) {
//        
//        XYDataset[] dataset = createDatasets(date, label, vals);
//
//        if (view == 0) {  // stacked
//            DateAxis timeAxis = new DateAxis(label.get(0));
//            CombinedDomainXYPlot plot = new CombinedDomainXYPlot(timeAxis);
//            plot.setGap(10.0);
//            for (int i = 0; i < dataset.length; i++) {
//                NumberAxis valueAxis = new NumberAxis(label.get(i));
//                XYPlot subplot = new XYPlot(dataset[i], timeAxis, valueAxis, null);
//                subplot.setRangeAxisLocation(AxisLocation.TOP_OR_LEFT);
//                
//                XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
//                renderer.setSeriesLinesVisible(0, y.get(i).isLine());
//                renderer.setSeriesShapesVisible(0, y.get(i).isShape());
//                renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
//                subplot.setRenderer(renderer);
//                plot.add(subplot, 1);
//            }
//            plot.setOrientation(PlotOrientation.VERTICAL);
//            JFreeChart chart = new JFreeChart(title, JFreeChart.DEFAULT_TITLE_FONT, plot, true);
//            return comp(chart);
//        } else if (view == 1) {  // multi
//            JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Date", label.get(0), dataset[0], true, true, false);
//            XYPlot plot = (XYPlot) chart.getPlot();
//            
//            plot.setOrientation(PlotOrientation.VERTICAL);
//            plot.getRangeAxis().setFixedDimension(15.0);
//            plot.getRangeAxis().setLabelPaint(col[0]);
//
//
//            for (int i = 0; i < dataset.length; i++) {
//                NumberAxis axis = new NumberAxis(label.get(i));
//                axis.setLabelPaint(col[i % col.length]);
//                plot.setRangeAxis(i, axis);
//                plot.setRangeAxisLocation(i, AxisLocation.BOTTOM_OR_LEFT);
//                plot.setDataset(i, dataset[i]);
//                plot.mapDatasetToRangeAxis(i, i);
////                XYItemRenderer rend = new StandardXYItemRenderer();
////                rend.setSeriesPaint(0, col[i % col.length]);
////                plot.setRenderer(i, rend);
//
//                XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
////                renderer.setSeriesPaint(i, col[i % col.length]);
////                renderer.setSeriesLinesVisible(i, y.get(i).isLine());
////                renderer.setSeriesShapesVisible(i, y.get(i).isShape());
//                renderer.setSeriesPaint(0, col[i % col.length]);
//                renderer.setSeriesLinesVisible(0, y.get(i).isLine());
//                renderer.setSeriesShapesVisible(0, y.get(i).isShape());
//                renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
//                plot.setRenderer(i, renderer);
//            }
//            
//            return comp(chart);
//        } else if (view == 2) {  // combined
//            XYDataset dataset1 = createDatasetCombined(date, label, vals);
//            JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Date", null, dataset1, true, false, false);
//            XYPlot plot = (XYPlot) chart.getPlot();
//            XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer();
//            for (int i = 0; i < dataset.length; i++) {
//                renderer.setSeriesLinesVisible(i, y.get(i).isLine());
//                renderer.setSeriesShapesVisible(i, y.get(i).isShape());
//            }
//            renderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());
//            plot.setRenderer(renderer);
//
//            return comp(chart);
//        } else {
//            throw new IllegalArgumentException("Illegal view type");
//        }
//    }
//
////    public static JComponent createTSChart(String title, Date[] date, List<String> label, List<Double[]> vals) {
////        XYDataset dataset = createDataset(date, label, vals);
////        JFreeChart chart = ChartFactory.createTimeSeriesChart(title, "Date", null, dataset, true, true, false);
////        return comp(chart);
////    }
//    public static JComponent createLineChart(String title, Integer[] date, List<String> label, List<Double[]> vals) {
//        XYDataset dataset = createXYDataset(date, label, vals);
//        JFreeChart chart = ChartFactory.createXYLineChart(title, "%", null, dataset, PlotOrientation.VERTICAL, true, true, false);
//        return comp(chart);
//    }
//
//    public static JComponent createScatterChart(String title, String labelX, String labelY, Double[] x, Double[] y) {
//        XYDataset dataset = createXYScatterDataset(x, y);
//        JFreeChart chart = ChartFactory.createScatterPlot(title, labelX, labelY, dataset, PlotOrientation.VERTICAL, false, true, false);
//        return comp(chart);
//    }
//
//    public static ESPToolPanel createESPTraces(String title, String dir, String var) throws Exception {
//        ESPResultInfo info = load(dir);
//        ESPTimeSeries init = getInitTimeSeries(info, dir, var);
//        ArrayList<ESPTimeSeries> output = new ArrayList<ESPTimeSeries>();
//        for (int i = 0; i < info.output.length; i++) {
//            ESPTimeSeries ts = getOutputTimeSeries(info, dir, i, var);
//            output.add(ts);
//        }
//        ArrayList<ESPTimeSeries> forecasts = new ArrayList<ESPTimeSeries>();
//        for (int i = 0; i < info.output.length; i++) {
//            ESPTimeSeries ts = getForecastTimeSeries(info, dir, i, var);
//            forecasts.add(ts);
//        }
//        EnsembleData ed = new EnsembleData(var, init, forecasts, output, null);
//        ESPToolPanel p = new ESPToolPanel(ed);
//        p.setResult(new File(dir, "result.csv").toString());
//        return p;
//    }
//
//    private static ESPResultInfo load(String dir) throws IOException {
//        File result = new File(dir, "result.csv");
//        if (!result.exists()) {
//            throw new IllegalArgumentException("Not found: " + result);
//        }
//        FileReader fr = new FileReader(result);
//        CSProperties p = DataIO.properties(fr, "Result");
//        fr.close();
//        ESPResultInfo info = new ESPResultInfo();
//        info.initstart = Conversions.convert(p.getInfo().get("initstart"), Date.class);
//        info.initend = Conversions.convert(p.getInfo().get("initend"), Date.class);
//        info.forecastend = Conversions.convert(p.getInfo().get("forecastend"), Date.class);
//        info.firsthistoricalyear = Conversions.convert(p.getInfo().get("firstyear"), Integer.class);
//        info.lasthistoricalyear = Conversions.convert(p.getInfo().get("lastyear"), Integer.class);
//        Trace[] t = new Trace[info.lasthistoricalyear - info.firsthistoricalyear + 1];
//        for (int i = info.firsthistoricalyear; i <= info.lasthistoricalyear; i++) {
//            int idx = i - info.firsthistoricalyear;
//            t[idx] = new Trace();
//            t[idx].year = i;
//            t[idx].file = p.getInfo().get("trace." + i);
//        }
//        info.output = t;
//        return info;
//    }
//
//    private static ESPTimeSeries getInitTimeSeries(ESPResultInfo info, String dir, String var) throws IOException {
//        String file = info.output[0].file;
//
////        File filename = new File(dir,file);
//        File filename = new File(file);
//        CSTable table = DataIO.table(filename, "efc");
//
//        double[] values = DataIO.getColumnDoubleValuesInterval(info.initstart, info.initend, table, var, DataIO.DAILY);
//        double dates[] = new double[values.length];
//
//        ModelDateTime start = new ModelDateTime();
//        start.setTime(info.initstart);
//        ModelDateTime end = new ModelDateTime();
//        end.setTime(info.initend);
//
//        ModelDateTime current = new ModelDateTime();
//        GregorianCalendar cal = new GregorianCalendar();
//        cal.setTime(info.initstart);
//        for (int i = 0; i < values.length; i++) {
//            current.setTime(cal.getTime());
//            dates[i] = current.getJulian();
//            cal.add(Calendar.DATE, 1);
////            values[i] = (Double) table.getValueAt(i, col);
//        }
//        ESPTimeSeries ts = new ESPTimeSeries("init", dates, values, start, end, "", filename.toString(), "");
//        return ts;
//    }
//
//    private static ESPTimeSeries getOutputTimeSeries(ESPResultInfo info, String dir, int trace, String var) throws IOException {
//        int year = info.output[trace].year;
//        String file = info.output[trace].file;
//
//        File filename = new File(file);
////        File filename = new File(dir, file);
//        CSTable table = DataIO.table(filename, "efc");
//
//        Double v[] = DataIO.getColumnDoubleValues(table, var);
//        Date d[] = DataIO.getColumnDateValues(table, "date");
//        double values[] = new double[v.length];
//        double dates[] = new double[v.length];
//
//        ModelDateTime start = new ModelDateTime();
//        start.setTime(info.initstart);
//
//        ModelDateTime end = new ModelDateTime();
//        end.setTime(info.forecastend);
//
//        ModelDateTime current = new ModelDateTime();
//        for (int i = 0; i < dates.length; i++) {
//            current.setTime(d[i]);
//            dates[i] = current.getJulian();
//            values[i] = v[i].doubleValue();
//        }
//
//        return new ESPTimeSeries(Integer.toString(year), dates, values, start, end, "", filename.toString(), "");
//    }
//
//    private static ESPTimeSeries getForecastTimeSeries(ESPResultInfo info, String dir, int trace, String var) throws IOException {
//        int year = info.output[trace].year;
//        String file = info.output[trace].file;
//
//        File filename = new File(file);
////        File filename = new File(dir, file);
//        CSTable table = DataIO.table(filename, "efc");
//
//        GregorianCalendar cal = new GregorianCalendar();
//        cal.setTime(info.initend);
//        cal.add(Calendar.DATE, 1);
//
//        double[] values = DataIO.getColumnDoubleValuesInterval(cal.getTime(), info.forecastend, table, var, DataIO.DAILY);
//        double dates[] = new double[values.length];
//
//        ModelDateTime start = new ModelDateTime();
//        start.setTime(info.initend);
//        start.setJul2Greg(start.getJulian() + 1.0);
//
//        ModelDateTime end = new ModelDateTime();
//        end.setTime(info.forecastend);
//
//        ModelDateTime current = new ModelDateTime();
//        for (int i = 0; i < dates.length; i++) {
//            current.setTime(cal.getTime());
//            dates[i] = current.getJulian();
//            cal.add(Calendar.DATE, 1);
//        }
//
//        return new ESPTimeSeries(Integer.toString(year), dates, values, start, end, "", filename.toString(), "");
//    }
//
//    public static class Trace {
//
//        int year;
//        String file;
//    }
//
//    public static class ESPResultInfo {
//
//        Date initstart;
//        Date initend;
//        Date forecastend;
//        int firsthistoricalyear;
//        int lasthistoricalyear;
//        Trace[] output;
//    }
////    public static void main(String[] args) throws Exception {
////        UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
////        final String title = "\u20A2\u20A2\u20A2\u20A3\u20A4\u20A5\u20A6\u20A7\u20A8\u20A9\u20AA";
////        JFrame f = new JFrame("test");
////        f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
////        final XYDataset dataset = createDataset();
////        final JFreeChart chart = createChart(dataset);
////        final ChartPanel chartPanel = new ChartPanel(chart);
////        chartPanel.setPreferredSize(new java.awt.Dimension(500, 270));
////        chartPanel.setMouseZoomable(true, false);
////        f.setContentPane(chartPanel);
////        f.pack();
////        f.setVisible(true);
////    }
//}
