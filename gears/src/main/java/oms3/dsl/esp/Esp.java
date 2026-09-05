/*
 * $Id: Esp.java 98fef14c747e 2014-12-26 odavid@colostate.edu $
 * 
 * This file is part of the Object Modeling System (OMS),
 * 2007-2012, Olaf David and others, Colorado State University.
 *
 * OMS is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, version 2.1.
 *
 * OMS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with OMS.  If not, see <http://www.gnu.org/licenses/lgpl.txt>.
 */
package oms3.dsl.esp;

import java.io.File;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import oms3.ngmf.util.UnifiedParams;
import oms3.ComponentAccess;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.Initialize;
import oms3.dsl.*;
import oms3.io.CSTable;
import oms3.io.CSVTableWriter;
import oms3.io.DataIO;
import oms3.util.Dates;

public class Esp extends AbstractSimulation {

    String hist_years;
    String esp_dates;
    boolean timing = false;

    @Override
    public Buildable create(Object name, Object value) {
        if (name.equals("historical_years")) {
            hist_years = value.toString();
        } else if (name.equals("esp_dates")) {
            esp_dates = value.toString();
        } else if (name.equals("timing")) {
            timing = (Boolean) value;
        } else {
            return super.create(name, value);
        }
        return LEAF;
    }

    @Override
    public Object run() throws Exception {
        super.run();
        if (hist_years == null) {
            throw new IllegalArgumentException("missing 'historical_years'.");
        }

        if (esp_dates == null) {
            throw new IllegalArgumentException("missing 'esp_dates'.");
        }

        int[] h_years = Dates.parseHistoricalYears(hist_years);
        Date[] dates = Dates.parseESPDates(esp_dates);

        final Calendar init_start = new GregorianCalendar();
        init_start.setTime(dates[0]);

        final Calendar init_end = new GregorianCalendar();
        init_end.setTime(dates[1]);
        init_end.add(Calendar.DATE, -1);

        final Calendar fcst_end = new GregorianCalendar();
        fcst_end.setTime(dates[2]);

        final int fc = (int) Dates.diffDayPeriods(init_end, fcst_end);
        if (fc > 365 || fc < 1) {
            throw new IllegalArgumentException("adjust the dates, forecast days must be: 1..365");
        }

        // setup component logging
        Logger.getLogger("oms3.model").setLevel(Level.OFF);
        Logging l = getModelElement().getComponentLogging();
        Map<String, String> cl = l.getCompLevels();
        for (String compname : cl.keySet()) {
            String cll = cl.get(compname);
            Level level = Level.parse(cll);
            Logger logger = Logger.getLogger("oms3.model." + compname);
            logger.setUseParentHandlers(false);
            logger.setLevel(level);
            ConsoleHandler ch = new ConsoleHandler();
            ch.setLevel(level);
            ch.setFormatter(new GenericBuilderSupport.CompLR());
            logger.addHandler(ch);
        }

        final File lastFolder = getOutputPath();
        lastFolder.mkdirs();

        PrintWriter res = new PrintWriter(new File(lastFolder, "result.csv"));
        res.println("@S, Result");

        // setting parameter;
        final UnifiedParams parameter = getModelElement().getParameter();
        parameter.putNewParamValue("startTime", init_start);
        parameter.putNewParamValue("endTime", fcst_end);

        System.out.print("Running ESP Traces ");

        long start = System.currentTimeMillis();

        // create all input traces files
        createTraces(h_years[0], h_years[1], lastFolder, parameter, init_start.getTime(), init_end.getTime(), fc);

        // parallel
        final CountDownLatch latch = new CountDownLatch(h_years[1] - h_years[0] + 1);
        int th = Integer.getInteger("esp.threads", 2);
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(th, 1));
        List<Future<String>> traces = new ArrayList<Future<String>>();

        for (int year = h_years[0]; year <= h_years[1]; year++) {
            final int y = year;
            Future<String> f = executor.submit(new Callable<String>() {
                @Override
                public String call() {
                    try {
                        String fn = runTrace(y, lastFolder, parameter);
                        return y + ", \"" + fn + "\"";
                    } catch (Throwable ex) {
                        System.err.println("Error in ESP run in year : " + y + " (" + ex.getMessage() + ")");
                        return y + ", Error in ESP run (" + ex.getMessage() + ")";
                    } finally {
                        Runtime.getRuntime().gc();
                        latch.countDown();
                    }
                }
            });
            traces.add(f);
        }
        latch.await();
        long end = System.currentTimeMillis();

        for (Future<String> trace : traces) {
            res.println(" trace." + trace.get());
        }
        executor.shutdown();

        res.println(" " + DataIO.KEY_ESP_DATES + ", " + esp_dates);
        res.println(" " + DataIO.KEY_HIST_YEARS + ", " + hist_years);
        res.close();
        System.out.println();
        if (timing) {
            System.out.println("Total ESP time: " + (double) (end - start) / 1000 + " seconds");
        }
        return null;
    }

    private void createTraces(int firstyear, int lastyear, final File lastFolder, UnifiedParams parameter,
            final Date init_start, Date init_end, final int fc) throws Exception {

        Object comp = getModelElement().newModelComponent();
        Class comp_class = comp.getClass();

        log.config("Init ...");
        ComponentAccess.callAnnotated(comp, Initialize.class, true);

//        boolean success = ComponentAccess.setInputData(parameter.getParams(), comp, log);
        boolean success = parameter.setInputData(comp, log);
        if (!success) {
            throw new Exception("There are Parameter problems. Simulation exits.");
        }

        ComponentAccess.adjustOutputPath(lastFolder, comp, log);

        final File input_file = (File) comp_class.getField("inputFile").get(comp);
        
        final CSTable table = DataIO.table(input_file, "obs");
        final int dateColumn = 1;

        // Forecast start = end of initialzation + 1 day
        Calendar fcStartCal = new GregorianCalendar();
        fcStartCal.setTime(init_end);
        fcStartCal.add(Calendar.DATE, 1);
        final Date fcStart = fcStartCal.getTime();

        // get the initialization period
        int[] it = DataIO.sliceByTime(table, dateColumn, init_start, init_end);
        final List<String[]> iniRows = DataIO.extractRows(table, it[0], it[1]);

        final Map<String, String[]> table_meta = DataIO.columnMetaData(table);
        final List<String> table_col_names = DataIO.columnNames(table);

        final CountDownLatch latch = new CountDownLatch(lastyear - firstyear + 1);
        ExecutorService executor = Executors.newFixedThreadPool(Integer.getInteger("esp.numthreads", 2));

        for (int y = firstyear; y <= lastyear; y++) {
            final int year = y;
            executor.submit(new Callable<Void>() {
                @Override
                public Void call() {
                    try {
                        DateFormat hfmt = DataIO.lookupDateFormat(table, dateColumn);

                        System.out.print(".");
                        System.out.flush();

                        // set the historical date to the forcast date, but use the
                        // historical year.
                        Calendar histStart = new GregorianCalendar();
                        histStart.setTime(fcStart);
                        histStart.set(Calendar.YEAR, year);

                        // get the historical data

                        int histStartRow = DataIO.findRowByDate(histStart.getTime(), dateColumn, table);
                        int histEndRow = histStartRow + (fc - 1);
                        List<String[]> histRows = DataIO.extractRows(table, histStartRow, histEndRow);

                        Calendar fcCurrent = new GregorianCalendar();
                        fcCurrent.setTime(fcStart);
                        for (int i = 0; i <= (fc - 1); i++) {
                            histRows.get(i)[1] = hfmt.format(fcCurrent.getTime());
                            fcCurrent.add(Calendar.DATE, 1);
                        }
                        fcCurrent.add(Calendar.DATE, -1);

                        Map<String, String> meta = new LinkedHashMap<String, String>(table.getInfo());
                        meta.put(DataIO.DATE_START, hfmt.format(init_start));
                        meta.put(DataIO.KEY_FC_START, hfmt.format(fcStart));
                        meta.put(DataIO.KEY_FC_DAYS, Integer.toString(fc));
                        meta.put(DataIO.KEY_HIST_YEAR, Integer.toString(year));
                        meta.put(DataIO.DATE_END, hfmt.format(fcCurrent.getTime()));

                        File new_inputFile = new File(lastFolder, "esp-" + year + "-" + input_file.getName());
                        PrintWriter w = new PrintWriter(new_inputFile);

                        CSVTableWriter tw = new CSVTableWriter(w, table.getName(), meta);
                        tw.writeHeader(table_meta, table_col_names);

                        // historical date -> forecast date.
                        tw.writeRows(iniRows, 1);
                        tw.writeRows(histRows, 1);

                        w.close();
                        return null;
                    } catch (Throwable ex) {
                        System.err.println("Error Data year : " + year + " (" + ex.getMessage() + ")");
                    } finally {
                        latch.countDown();
                    }
                    return null;
                }
            });
        }
        latch.await();
        executor.shutdown();
        Runtime.getRuntime().gc();
    }

    /**
     * Run a single trace.
     *
     * @param year
     * @param lastFolder
     * @param parameter
     * @param init_start
     * @param init_end
     * @param fc
     * @return
     * @throws Exception
     */
    private String runTrace(int year, File lastFolder, UnifiedParams parameter) throws Exception {
        System.out.print(" " + year);
        System.out.flush();

        Object comp = getModelElement().newModelComponent();
        Class comp_class = comp.getClass();

        log.config("Init ...");
        ComponentAccess.callAnnotated(comp, Initialize.class, true);

//        boolean success = ComponentAccess.setInputData(parameter.getParams(), comp, log);
        boolean success = parameter.setInputData(comp, log);
        if (!success) {
            throw new Exception("There are Parameter problems. Simulation exits.");
        }

        ComponentAccess.adjustOutputPath(lastFolder, comp, log);

        File input_file = (File) comp_class.getField("inputFile").get(comp);
        File out_file = (File) comp_class.getField("outFile").get(comp);

        File new_outFile = new File(out_file.getParent(), "esp-" + year + "-" + out_file.getName());
        File new_inputFile = new File(lastFolder, "esp-" + year + "-" + input_file.getName());

        comp_class.getField("inputFile").set(comp, new_inputFile);
        comp_class.getField("outFile").set(comp, new_outFile);

        // execute phases and be done.
        log.config("Exec ...");
        ComponentAccess.callAnnotated(comp, Execute.class, false);

        log.config("Finalize ...");
        ComponentAccess.callAnnotated(comp, Finalize.class, true);

        return new_outFile.toString();
    }
}