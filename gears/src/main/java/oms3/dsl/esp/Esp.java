//package oms3.dsl.esp;
//
//import oms3.dsl.*;
//import java.io.File;
//import java.io.PrintWriter;
//import java.util.Calendar;
//import java.util.Date;
//import java.util.GregorianCalendar;
//import java.util.Map;
//import java.util.logging.Level;
//import java.util.logging.Logger;
//import ngmf.util.OutputStragegy;
//import oms3.ComponentAccess;
//import oms3.Compound;
//import oms3.Conversions;
//import oms3.annotations.Execute;
//import oms3.annotations.Finalize;
//import oms3.annotations.Initialize;
//import oms3.io.CSTable;
//import oms3.io.DataIO;
//import oms3.util.Times;
//
//public class Esp extends AbstractSimulation {
//
//    // esp stuff
//    int first_year;
//    int last_year;
//    //
//    int fc;
//    Calendar fc_end;
//
//    @Override
//    public Buildable create(Object name, Object value) {
//        if (name.equals("forecast_end")) {
//            Date d = Conversions.convert(value, Date.class);
//            fc_end = new GregorianCalendar();
//            fc_end.setTime(d);
//        } else if (name.equals("forecast_days")) {
//            fc = (Integer) value;
//            if (fc < 1) {
//                throw new IllegalArgumentException("forecast_days < 1");
//            }
//        } else if (name.equals("first_year")) {
//            first_year = (Integer) value;
//        } else if (name.equals("last_year")) {
//            last_year = (Integer) value;
//        } else {
//            return super.create(name, value);
//        }
//        return LEAF;
//    }
//
//    @Override
//    public Object run() throws Exception {
//        Compound.reload();
//        Logger.getLogger("oms3.model").setLevel(Level.WARNING);
//
//        if (getModel() == null) {
//            throw new IllegalArgumentException("missing model definition.");
//        }
//        Calendar now = new GregorianCalendar();
//        int thisYear = now.get(Calendar.YEAR);
//
//        if (fc == 0 && fc_end == null) {
//            throw new IllegalArgumentException("set either 'forecast_days' or 'forecast_end'");
//        }
//        if (fc > 0 && fc_end != null) {
//            throw new IllegalArgumentException("set 'forecast_days' or 'forecast_end', not both");
//        }
//
//        if (fc_end == null && (fc > 366 || fc < 1)) {
//            throw new IllegalArgumentException("forecast_days: 1...366");
//        }
//
//        if (first_year < 1900 || last_year < 1900 || first_year >= last_year || first_year > thisYear || last_year > thisYear) {
//            throw new IllegalArgumentException("invalid/missing first_year or/and last_year!");
//        }
//
//        OutputStragegy st = getOutput().getOutputStrategy(getName());
//        File lastFolder = st.nextOutputFolder();
//        if (log.isLoggable(Level.CONFIG)) {
//            log.config("Simulation output folder: " + lastFolder);
//        }
//        lastFolder.mkdirs();
//        PrintWriter res = new PrintWriter(new File(lastFolder, "result.csv"));
//        res.println("@S, Result");
//
//        Calendar start = null;
//        Calendar end = null;
//
//        System.out.print(" Running ESP Traces ");
//        for (int year = first_year; year <= last_year; year++) {
//            System.out.print(" " + year);
//
//            Object comp = getModel().getComponent();
//            log.config("Init ...");
//            ComponentAccess.callAnnotated(comp, Initialize.class, true);
//
//            // setting the input data;
//            Map<String, Object> parameter = getModel().getParameter();
//            boolean success = ComponentAccess.setInputData(parameter, comp, log);
//            if (!success) {
//                System.out.println("There are Parameter problems. Simulation exits.");
//                return null;
//            }
//
//            boolean adjusted = ComponentAccess.adjustOutputPath(lastFolder, comp, log);
//            if (adjusted) {
//                lastFolder.mkdirs();
//            }
//
//            start = (Calendar) comp.getClass().getField("startTime").get(comp);
//            end = (Calendar) comp.getClass().getField("endTime").get(comp);
//            File input_file = (File) comp.getClass().getField("inputFile").get(comp);
//            File out_file = (File) comp.getClass().getField("outFile").get(comp);
//            if (start.after(end)) {
//                throw new IllegalArgumentException("illegal startTime/endTime.");
//            }
//
//            if (fc == 0) {
//                fc = (int) Times.diffDayPeriods(end, fc_end);
//            } else {
//                fc_end = new GregorianCalendar();
//                fc_end.setTime(end.getTime());
//                fc_end.add(Calendar.DATE, fc);
//            }
//
//            CSTable t = DataIO.table(input_file, "obs");
//            CSTable esp = DataIO.synthESPInput(t, start.getTime(), end.getTime(), fc, year);
//            File new_input_file = new File(lastFolder, "esp-" + year + "-" + input_file.getName());
//            PrintWriter w = new PrintWriter(new_input_file);
//            DataIO.print(esp, w);
//            w.close();
//
//            File new_outFile = new File(out_file.getParent(), "esp-" + year + "-" + out_file.getName());
//
//            comp.getClass().getField("inputFile").set(comp, new_input_file);
//            comp.getClass().getField("outFile").set(comp, new_outFile);
//            comp.getClass().getField("endTime").set(comp, fc_end);
//
//            // execute phases and be done.
//            log.config("Exec ...");
//            ComponentAccess.callAnnotated(comp, Execute.class, false);
//            log.config("Finalize ...");
//            ComponentAccess.callAnnotated(comp, Finalize.class, true);
//            res.println(" trace." + year + ", \"" + new_outFile.toString() + "\"");
//        }
//        res.println(" initstart, " + Conversions.formatISO(start.getTime()));
//        res.println(" initend, " + Conversions.formatISO(end.getTime()));
//        res.println(" forecastend, " + Conversions.formatISO(fc_end.getTime()));
//        res.println(" firstyear, " + first_year);
//        res.println(" lastyear, " + last_year);
//        res.close();
//        System.out.println();
//        Compound.shutdown();
//        return null;
//    }
//}
