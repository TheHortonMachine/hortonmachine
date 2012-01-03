package org.omscentral.modules.analysis.esp;

import java.util.ArrayList;

public class EnsembleData {
    
    static public int VOLUME = 1;
    static public int PEAK = 2;
    static public int YEAR = 3;
    
    private TimeSeriesCookie initialization; //  contains data for plotting
    private ArrayList<ESPTimeSeries> forecasts;           //  array of TimeSeries with data for plotting
    private ArrayList<ESPTimeSeries> historic;            //  array of TimeSeries with no data; used to keep track of historic years
    private ArrayList<ESPTimeSeries> input;               //  array of TimeSeries with no data; used to keep track of input files
    private ArrayList<ESPTimeSeries> output;              //  array of TimeSeries with init + forecast data for analysis
    private ArrayList<EnsembleListLabel> stats;               //  array of EnsembleListLabel
    private ArrayList<EnsembleListLabel> statsInVolumeOrder;
    private ArrayList<EnsembleListLabel> statsInPeakOrder;
    private String name;
    private int sortOrder = VOLUME;
    
    public EnsembleData(String name, ESPTimeSeries initialization,
            ArrayList<ESPTimeSeries> forecasts,  ArrayList<ESPTimeSeries> output, ArrayList<ESPTimeSeries> historic) {
        this.name = name;
        this.initialization = initialization;
        this.historic = historic;
        this.output = output;
        setForecasts(forecasts);
    }
    
    public EnsembleData(String name, ESPTimeSeries init, ArrayList<ESPTimeSeries> forecasts, ArrayList<ESPTimeSeries> historic) {
        this.name = name;
        this.initialization = init;
        this.historic = historic;
        setForecasts(forecasts);
    }
    
    private void PROCESS_trace(ModelDateTime analysisStart, ModelDateTime analysisEnd) {
//        int analysis_dateCount = (int)(analysisEnd.getJulian() - analysisStart.getJulian()) + 1;
        for (int j = 0; j < forecasts.size(); j++) {
            EnsembleListLabel ts = (stats.get(j));
            ESPTimeSeries trace = (output.get(j));
            double[] trace_data = trace.getVals();
            double[] trace_dates = trace.getDates();
            
            ts.setTraceVolume(0.0);
            ts.setTracePeak(0.0);
            
            // danger: Olaf changed this!!!!!!!!!!!!!
            int offset = (int)(analysisStart.getJulian()) - (int)(trace.getStart().getJulian())-1;
            int length = (int)(analysisEnd.getJulian()) - (int)(analysisStart.getJulian());
//            System.out.println("length " + length + " offset " + offset + " trace.length " + trace_data.length + " " +
//                    trace);
            
            for (int k = 0; k < length; k++) {
                double tmp = trace_data[k + offset];
                ts.setTraceVolume(tmp + ts.getTraceVolume());
                if (ts.getTracePeak() < tmp) {
                    ts.setTracePeak(tmp);
                    ts.setTimeToPeak(trace_dates[k + offset]);
                }
            }
        }
        
        statsInVolumeOrder = new ArrayList<EnsembleListLabel> (stats.size());
        statsInPeakOrder = new ArrayList<EnsembleListLabel> (stats.size());
/*
 *  Make copies
 */
        for (int i = 0; i < stats.size(); i++) {
            statsInVolumeOrder.add(i, stats.get(i));
            statsInPeakOrder.add(i, stats.get(i));
        }
        sort(statsInVolumeOrder, statsInPeakOrder);
    }
    
    public static void sort(ArrayList<EnsembleListLabel> statsInVolumeOrder,
            ArrayList<EnsembleListLabel> statsInPeakOrder) {
    /*
     *  Sort by volumes
     */
        for (int i = 0; i < statsInVolumeOrder.size() - 1; i++) {
            EnsembleListLabel tsi = (statsInVolumeOrder.get(i));
            for (int j = i+1; j < statsInVolumeOrder.size(); j++) {
                EnsembleListLabel tsj = (statsInVolumeOrder.get(j));
                if (tsj.getTraceVolume() > tsi.getTraceVolume()) {
                    statsInVolumeOrder.set(j, tsi);
                    statsInVolumeOrder.set(i, tsj);
                    tsi = tsj;
                }
            }
        }
        
        /*
         *  Generate volume probabilities
         *  Formula is   P = (100.0) m / (n + 1.0)  (Linsley, Kohler, and Paulhus page 249)
         */
        double n = (double)(statsInVolumeOrder.size() + 1);
        for (int i = 0; i < statsInVolumeOrder.size(); i++) {
            double m = (double)(i + 1);
            EnsembleListLabel tsi = (statsInVolumeOrder.get(i));
            double prob = 100 * m / n;
            tsi.setVolumeRank(i+1);
            tsi.setActVolumeProb(prob);
            tsi.setRoundVolumeProb(fivePercentRound(prob));
        }
        
/*
        System.out.println("\nVolume");
        for (int i = 0; i < stats.size(); i++) {
            EnsembleListLabel tsi = (EnsembleListLabel)(statsInVolumeOrder.get(i));
            System.out.println ("EnsembleData.Sort statsInVolumeOrder = " + tsi + " volume = " + tsi.getTraceVolume() + " peak = " + tsi.getTracePeak() + " time to peak = " + tsi.getTimeToPeak());
        }
 */
        /*
         *  Sort by peak
         */
        for (int i = 0; i < statsInPeakOrder.size() - 1; i++) {
            EnsembleListLabel tsi = (statsInPeakOrder.get(i));
            for (int j = i+1; j < statsInPeakOrder.size(); j++) {
                EnsembleListLabel tsj = (statsInPeakOrder.get(j));
                if (tsj.getTracePeak() > tsi.getTracePeak()) {
                    statsInPeakOrder.set(j, tsi);
                    statsInPeakOrder.set(i, tsj);
                    tsi = tsj;
                }
            }
        }
/*
        System.out.println("\nPeak");
        for (int i = 0; i < stats.size(); i++) {
            EnsembleListLabel tsi = (EnsembleListLabel)(statsInPeakOrder.get(i));
            System.out.println ("EnsembleData.Sort statsInPeakOrder = " + tsi + " volume = " + tsi.getTraceVolume() + " peak = " + tsi.getTracePeak() + " time to peak = " + tsi.getTimeToPeak());
        }
 */
/*
 *  Generate peak probabilities
 *  Formula is   P = (100.0) m / (n + 1.0)  (Linsley, Kohler, and Paulhus page 249)
 */
        n = (double)(statsInPeakOrder.size() + 1);
        for (int i = 0; i < statsInPeakOrder.size(); i++) {
            double m = (double)(i + 1);
            EnsembleListLabel tsi = statsInPeakOrder.get(i);
            double prob = 100 * m / n;
            tsi.setPeakRank(i+1);
            tsi.setActPeakProb(prob);
            tsi.setRoundPeakProb(fivePercentRound(prob));
        }
        
    }
    
    public static double fivePercentRound(double p) {
        if (p > 92.5) {
            return (95.0);
        } else if (p > 87.5) {
            return (90.0);
        } else if (p > 82.5) {
            return (85.0);
        } else if (p > 77.5) {
            return (80.0);
        } else if (p > 72.5) {
            return (75.0);
        } else if (p > 67.5) {
            return (70.0);
        } else if (p > 62.5) {
            return (65.0);
        } else if (p > 57.5) {
            return (60.0);
        } else if (p > 52.5) {
            return (55.0);
        } else if (p > 47.5) {
            return (50.0);
        } else if (p > 42.5) {
            return (45.0);
        } else if (p > 37.5) {
            return (40.0);
        } else if (p > 32.5) {
            return (35.0);
        } else if (p > 27.5) {
            return (30.0);
        } else if (p > 22.5) {
            return (25.0);
        } else if (p > 17.5) {
            return (20.0);
        } else if (p > 12.5) {
            return (15.0);
        } else if (p > 7.5) {
            return (10.0);
        } else {
            return (5.0);
        }
    }
    
    public ModelDateTime getInitializationStart() {
        return this.initialization.getStart();
    }
    
    public ModelDateTime getInitializationEnd() {
        return this.initialization.getEnd();
    }
    
    public ModelDateTime getForecastStart() {
        ESPTimeSeries forecast = forecasts.get(0);
        return forecast.getStart();
    }
    
    public ModelDateTime getForecastEnd() {
        ESPTimeSeries forecast = forecasts.get(0);
        return forecast.getEnd();
    }
    
    public TimeSeriesCookie getInitialization() {
        return this.initialization;
    }
    
    public void setInitialization(TimeSeriesCookie initialization) {
        this.initialization = initialization;
    }
    
    public ArrayList<ESPTimeSeries> getHistoric() {
        return this.historic;
    }
    
    public void setHistoric(ArrayList<ESPTimeSeries> historic) {
        this.historic = historic;
    }
    
    public String getName() {
        return this.name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public ArrayList<ESPTimeSeries> getForecasts() {
        return this.forecasts;
    }
    
    public void setForecasts(ArrayList<ESPTimeSeries> forecasts) {
        this.forecasts = forecasts;
        if (forecasts != null) {
            int size = forecasts.size();
            this.stats = new ArrayList<EnsembleListLabel>(size);
            for (int i = 0; i < size; i++) {
                ESPTimeSeries ts = forecasts.get(i);
                this.stats.add(new EnsembleListLabel(ts, this));
            }
        }
    }
    
    public ArrayList<ESPTimeSeries> getInput() {
        return this.input;
    }
    
    /**
     * Setter for property input.
     * @param input New value of property input.
     */
    public void setInput(ArrayList<ESPTimeSeries> input) {
        this.input = input;
    }
    
    /**
     * Getter for property output.
     * @return Value of property output.
     */
    public ArrayList<ESPTimeSeries> getOutput() {
        return this.output;
    }
    
    /**
     * Setter for property output.
     * @param output New value of property output.
     */
    public void setOutput(ArrayList<ESPTimeSeries> output) {
        this.output = output;
    }
    
    
    public void setAnalysisPeriod(ModelDateTime start_date, ModelDateTime end_date) {
        PROCESS_trace(start_date, end_date);
    }
    
    /**
     * Getter for property stats.
     * @return Value of property stats.
     */
    public ArrayList<EnsembleListLabel> getStats() {
        return this.stats;
    }
    
    /**
     * Getter for property statsInVolumeOrder.
     * @return Value of property statsInVolumeOrder.
     */
    public ArrayList<EnsembleListLabel> getStatsInVolumeOrder() {
        return this.statsInVolumeOrder;
    }
    
    /**
     * Getter for property statsInPeakOrder.
     * @return Value of property statsInPeakOrder.
     */
    public ArrayList<EnsembleListLabel> getStatsInPeakOrder() {
        return this.statsInPeakOrder;
    }
    
//    public static EnsembleData load (String file_name) {
//        EnsembleData ed = null;
//
//        try {
//            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
//            factory.setNamespaceAware(true);
//            DocumentBuilder builder = factory.newDocumentBuilder();
//            Document doc = builder.parse(file_name);
//
//            Node node = OuiProjectXml.selectSingleNode(doc, "/ESP");
//            ed = new EnsembleData(OuiProjectXml.getElementContent(node, "@name", null), null, null, null);
//
//
///*
// *  Read ESP meta data from the xml file
// */
//            ed.setInitialization(TimeSeries.getTimeSeriesFromXmlNode(OuiProjectXml.selectSingleNode(doc, "/ESP/initialization/TimeSeries")));
////            initialization.dump();
//
//            ed.setHistoric(getArrayListFromXmlNode (OuiProjectXml.selectSingleNode(doc, "/ESP/historic")));
//            ed.setInput(getArrayListFromXmlNode (OuiProjectXml.selectSingleNode(doc, "/ESP/input")));
//            ed.setOutput(getArrayListFromXmlNode (OuiProjectXml.selectSingleNode(doc, "/ESP/output")));
//            ed.setForecasts(getArrayListFromXmlNode (OuiProjectXml.selectSingleNode(doc, "/ESP/forecasts")));
///*
// *  Read the trace data
// */
//            MmsOuiEspReader.readEsp(ed);
//
//        } catch (Exception E) {
//            System.err.println(E.getMessage());
//        }
//
//        return ed;
//    }
//
//    public static ArrayList<TimeSeries> getArrayListFromXmlNode (Node node) {
//        NodeList nl = OuiProjectXml.selectNodes(node, "TimeSeries");
//
//        ArrayList<TimeSeries> al = new ArrayList<TimeSeries>(nl.getLength());
//        for (int i = 0; i < nl.getLength(); i++) {
//            TimeSeries ts = TimeSeries.getTimeSeriesFromXmlNode(nl.item(i));
////            ts.dump();
//            al.add(i, ts);
//        }
//
//        return al;
//    }
//
//    public void save (String file_name) {
//        Format format = new Format("%.2f");
//        PrintWriter out = null;
//
//        System.out.println ("EnsembleData.save: writing file " + file_name);
//
//        try {
//            out = new PrintWriter(new FileWriter(file_name));
//
//            out.println ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
//            out.println ("<ESP name=\"" + this.name + "\">");
//
//            out.println ("   <initialization>");
//            out.println ("      " + initialization.getXmlBlock());
//            out.println ("   </initialization>");
//
//            writeArrayListBlock(out, "forecasts", forecasts);
//            writeArrayListBlock(out, "historic", historic);
//            writeArrayListBlock(out, "input", input);
//            writeArrayListBlock(out, "output", output);
//
//            out.println ("</ESP>");
//
//        } catch (Exception e) {
//            e.printStackTrace();
//
//        } finally {
//            if (out!= null) out.close();
//        }
//    }
    
//    private void writeArrayListBlock(PrintWriter out, String name, ArrayList list) {
//        out.println("   <" + name + ">");
//        Iterator it = list.iterator();
//        while (it.hasNext()) {
//            TimeSeries ts = (TimeSeries)(it.next());
//            out.println("      " + ts.getXmlBlock());
//        }
//        out.println("   </" + name + ">");
//    }
    
    /**
     * Getter for property sortOrder.
     * @return Value of property sortOrder.
     */
    public int getSortOrder() {
        return this.sortOrder;
    }
    
    /**
     * Setter for property sortOrder.
     * @param sortOrder New value of property sortOrder.
     */
    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
    
}
