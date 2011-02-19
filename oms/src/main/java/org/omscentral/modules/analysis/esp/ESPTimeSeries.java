/*
 * TimeSeries.java
 *
 * Created on November 19, 2004, 8:23 AM
 */
package org.omscentral.modules.analysis.esp;

/**
 *
 * @author  markstro
 */
public class ESPTimeSeries implements TimeSeriesCookie {

    private double[] dates;
    private double[] vals;
    private ModelDateTime start;
    private ModelDateTime end;
    private String description;
    private String name;
    private String source;
    private String units;

    public ESPTimeSeries(String name, double[] dates, double[] vals, ModelDateTime start, ModelDateTime end, String description, String source, String units) {
        this.name = name;
        this.dates = dates;
        this.vals = vals;
        this.start = start;
        this.end = end;
        this.description = description;
        this.source = source;
        this.units = units;
    }

    public ESPTimeSeries(String name, double[] dates, double[] vals) {
        this.name = name;
        this.dates = dates;
        this.vals = vals;
        setDates(dates);
        this.description = name;
        this.source = "unknown";
        this.units = "unknown";
    }

    public double[] getDates() {
        return dates;
    }

    public void setDates(double[] dates) {
        this.dates = dates;
        this.start = new ModelDateTime(dates[0]);
        this.end = new ModelDateTime(dates[dates.length - 1]);
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ModelDateTime getEnd() {
        return end;
    }

    public void setEnd(ModelDateTime end) {
        this.end = end;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public ModelDateTime getStart() {
        return start;
    }

    public void setStart(ModelDateTime start) {
        this.start = start;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public double[] getVals() {
        return vals;
    }

    public void setVals(double[] vals) {
        this.vals = vals;
    }

    @Override
    public String toString() {
        return name;
    }

    public String getXmlBlock() {
        return "<TimeSeries name=\"" + name + "\" description=\"" + description + "\" source=\""
                + source + "\" units=\"" + units + "\" start =\"" + start.getJulian() + "\" end =\""
                + end.getJulian() + "\"/>";
    }

    public void dump() {
        System.out.println("TimeSeries name = " + name);
        System.out.println("    description = " + description);
        System.out.println("         source = " + source);
        System.out.println("          units = " + units);
        System.out.println("          start = " + start);
        System.out.println("            end = " + end);
    }

    public void trim(ModelDateTime start, ModelDateTime end) {
        double start_jd = start.getJulian();
        double end_jd = end.getJulian();

        int count = 0;
        for (int i = 0; i < dates.length; i++) {
            if ((dates[i] >= start_jd) && (dates[i] <= end_jd)) {
                count++;
            }
        }

        double[] new_dates = new double[count];
        double[] new_data = new double[count];

        count = 0;
        for (int i = 0; i < dates.length; i++) {
            if ((dates[i] >= start_jd) && (dates[i] <= end_jd)) {
                new_dates[count] = dates[i];
                new_data[count] = vals[i];
                count++;
            }
        }

        dates = new_dates;
        vals = new_data;
        this.start = new ModelDateTime(dates[0]);
        this.end = new ModelDateTime(dates[count - 1]);
    }
}
