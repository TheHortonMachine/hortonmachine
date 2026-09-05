/*
 * $Id: DataIO.java 7cba5ba59d73 2018-11-29 francesco.serafin.3@gmail.com $
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
package oms3.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Arrays;
import javax.swing.event.TableModelListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import oms3.Conversions;

/**
 * Data Input/Output management.
 *
 * @author od
 */
public class DataIO {

    private static final String P = "@";
    public static final String TABLE = P + "T";
    public static final String HEADER = P + "H";
    public static final String PROPERTIES = P + "S";
    public static final String PROPERTY = P + "P";
    public static final String TABLE1 = P + "Table";
    public static final String HEADER1 = P + "Header";
    public static final String PROPERTIES1 = P + "Properties";
    public static final String PROPERTY1 = P + "Property";
    //
    //
    public static final String CSPROPERTIES_EXT = "csp";
    public static final String CSTABLE_EXT = "cst";
    //
    private static final String ROOT_ANN = "___root___";
    private static final String COMMENT = "#";
    private static final Pattern varPattern = Pattern.compile("\\$\\{([^$}]+)\\}");
    /*
     * some static helpers, might have to go somewhere else
     */
    private static final String ISO8601 = "yyyy-MM-dd'T'hh:mm:ss";
    //
    // all meta data keys
    public static final String KEY_CONVERTED_FROM = "converted_from";
    public static final String DATE_FORMAT = "date_format";
    public static final String DATE_START = "date_start";
    public static final String DATE_END = "date_end";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String KEY_MODIFIED_AT = "modifed_at";
    public static final String KEY_CREATED_BY = "created_by";
    public static final String KEY_UNIT = "unit";
    public static final String KEY_FORMAT = "format";
    public static final String KEY_TYPE = "type";
    public static final String KEY_NAME = "name";
    public static final String KEY_MISSING_VAL = "missing_value";
    public static final String KEY_FC_START = "forecast_start";
    public static final String KEY_FC_DAYS = "forecast_days";
    public static final String KEY_HIST_YEAR = "historical_year";
    public static final String KEY_HIST_YEARS = "historical_years";
    public static final String KEY_ESP_DATES = "esp_dates";
    public static final String KEY_DIGEST = "digest";
    public static final String VAL_DATE = "Date";
    //
    //TimeStep Enumerations
    public static final int DAILY = 0;
    public static final int MEAN_MONTHLY = 1;
    public static final int MONTHLY_MEAN = 2;
    public static final int ANNUAL_MEAN = 3;
    public static final int PERIOD_MEAN = 4;
    public static final int PERIOD_MEDIAN = 5;
    public static final int PERIOD_STANDARD_DEVIATION = 6;
    public static final int PERIOD_MIN = 7;
    public static final int PERIOD_MAX = 8;
    public static final int RAW = 9;
    public static final int TIME_STEP = 10;


    public static double[] getColumnDoubleValuesInterval(Date start, Date end, CSTable t, String columnName, int timeStep) {
        // Uses calendar year with all months as default.
        boolean[] periodMask = {true, true, true, true, true, true, true, true, true, true, true, true};
        boolean[] subDivideMask = null;
        return getColumnDoubleValuesInterval(start, end, t, columnName, timeStep, 0, periodMask, subDivideMask);
    }


    public static double[] getColumnDoubleValuesInterval(Date start, Date end, CSTable t, String columnName, int timeStep, boolean[] periodMask) {
        // Uses calendar year with all months as default.
        boolean[] subDivideMask = null;
        return getColumnDoubleValuesInterval(start, end, t, columnName, timeStep, 0, periodMask, subDivideMask);
    }


    public static double[] getColumnDoubleValuesInterval(Date start, Date end, CSTable t, String columnName, int timeStep, int startMonth,
            boolean[] periodMask, boolean[] subDivideMask) {

        int col = columnIndex(t, columnName);
        if (col == -1) {
            throw new IllegalArgumentException("No such column: " + columnName);
        }

        if (periodMask != null) {
            boolean pmCheck = false;
            for (int i = 0; i < periodMask.length; i++) {
                pmCheck = pmCheck | periodMask[i];
            }
            if (pmCheck == false) {
                throw new IllegalArgumentException("PeriodMask (i.e. Mask of months to include in data analysis) is all false, so no data would be used.");
            }
        }

        // do this before the switch
        if (timeStep == RAW) {
            Double[] a = getColumnDoubleValues(t, columnName);
            double[] arr = Conversions.convert(a, double[].class);
            return arr;
        }

        DateFormat fmt = lookupDateFormat(t, 1);
        boolean hasYear = DateFormatHasYear(t, 1);

        switch (timeStep) {

            case TIME_STEP: {

                List<Double> l = new ArrayList<Double>();
                double sum = 0;
                for (String[] row : t.rows()) {
                    try {
                        Date d = fmt.parse(row[1]);
                        if ((d.equals(start) || d.after(start))
                                && (d.equals(end) || d.before(end))) {
                            int month = d.getMonth();
                            double data = Double.parseDouble(row[col]);
                            boolean periodValid = (periodMask == null) || (periodMask[month] == true);
                            if (periodValid) {
                                l.add(data);
                            }
                        }
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                // Copy the List to the output array. 
                double[] arr = new double[l.size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = l.get(i);
                }
                return arr;
            }

            case DAILY:
            case MONTHLY_MEAN: {

                int previousMonth = -1;
                int previousYear = -1;
                int previousDay = -1;
                boolean previousValid = false;

                boolean useYear = (timeStep == DAILY) || (timeStep == MONTHLY_MEAN) || (timeStep == ANNUAL_MEAN);
                boolean useMonth = (timeStep == DAILY) || (timeStep == MONTHLY_MEAN);
                boolean useDay = (timeStep == DAILY);

                List<Double> l = new ArrayList<Double>();
                double sum = 0;
                int count = 0;
                int sdindx = 0;

                for (String[] row : t.rows()) {
                    try {
                        Date d = fmt.parse(row[1]);
                        if ((d.equals(start) || d.after(start)) && (d.equals(end) || d.before(end))) {
                            int month = d.getMonth();
                            int year = d.getYear();
                            int day = d.getDate();
                            double data = Double.parseDouble(row[col]);

                            boolean newYear = (year != previousYear);

                            boolean newEntry = (previousValid
                                    && ((useYear && newYear)
                                    || (useMonth && (month != previousMonth))
                                    || (useDay && (day != previousDay))));

                            if (newEntry && (count != 0)) {
                                l.add(sum / count);
                                sum = 0;
                                count = 0;
                            }

                            boolean periodValid = (periodMask == null) || (periodMask[month] == true);
                            boolean subDivideValid = (subDivideMask == null) || subDivideMask[sdindx++];
                            if (periodValid && subDivideValid) {
                                sum += data;
                                count++;
                            }

                            previousValid = true;
                            previousDay = day;
                            previousMonth = month;
                            previousYear = year;
                        }
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }

                }
                if (count != 0) {
                    l.add(sum / count);
                } // add the final entry which wasn't yet added
                // since it never hit a newEntry.

                // Copy the List to the output array.
                double[] arr = new double[l.size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = l.get(i);
                }

                return arr;
                // break;
            }

            case MEAN_MONTHLY: {

                double[] arr = new double[12]; // 1 per month

                int[] count = new int[12];
                int sdindx = 0;

                for (int i = 0; i < 12; i++) {
                    arr[i] = 0; // initialize data to 0
                    count[i] = 0;
                }

                // Create month if we don't have a date. Assumes 1 month/row.
                for (String[] row : t.rows()) {
                    try {
                        Date d = fmt.parse(row[1]);

                        if (!hasYear || ((d.equals(start) || d.after(start)) && (d.equals(end) || d.before(end)))) {
                            int month = d.getMonth();
                            if (month > 11) {
                                throw new RuntimeException("Month > 11 = " + month);
                            }
                            double data = Double.parseDouble(row[col]);

                            boolean periodValid = (periodMask == null) || (periodMask[month] == true);
                            boolean subDivideValid = (subDivideMask == null) || subDivideMask[sdindx++];
                            if (periodValid && subDivideValid) {
                                arr[month] = arr[month] + data;
                                count[month] = count[month] + 1;
                            }
                        }
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                for (int i = 0; i < 12; i++) {
                    arr[i] = (count[i] == 0) ? 0 : (arr[i] / count[i]);
                    //System.out.println("Arr["+i+"] = " + arr[i]);
                }

                return arr;
                // break;
            }

            case ANNUAL_MEAN:
            case PERIOD_MEAN:
            case PERIOD_MIN:
            case PERIOD_MAX:
            case PERIOD_MEDIAN:
            case PERIOD_STANDARD_DEVIATION: {
                int previousMonth = -1;
                int previousYear = -1;
                int previousDay = -1;
                boolean previousValid = false;

                List<Double> l = new ArrayList<Double>();
                List<Double> l_median = new ArrayList<Double>();

                double sum = 0;
                double sq_sum = 0;
                int count = 0;
                double min = Double.MAX_VALUE;
                double max = Double.MIN_VALUE;
                int sdindx = 0;

                for (String[] row : t.rows()) {
                    try {
                        Date d = fmt.parse(row[1]);
                        if ((d.equals(start) || d.after(start)) && (d.equals(end) || d.before(end))) {
                            int month = d.getMonth();
                            int year = d.getYear();
                            int day = d.getDate();
                            double data = Double.parseDouble(row[col]);

                            boolean newYear = (month != previousMonth) && (month == startMonth);
                            boolean newEntry = (previousValid && newYear);

                            if (newEntry && count != 0) {
                                // Add new data to list
                                l.add(selectResult(timeStep, sum, sq_sum, count, min, max, l_median));
                                // Reset stats for new year
                                sum = 0;
                                sq_sum = 0;
                                count = 0;
                                min = Double.MAX_VALUE;
                                max = Double.MIN_VALUE;
                                l_median.clear();
                            }

                            boolean periodValid = (periodMask == null) || (periodMask[month] == true);
                            boolean subDivideValid = (subDivideMask == null) || subDivideMask[sdindx++];
                            if (periodValid && subDivideValid) {
                                sum += data;
                                sq_sum += (data * data);
                                count++;
                                if (data < min) {
                                    min = data;
                                }
                                if (data > max) {
                                    max = data;
                                }
                                l_median.add(data);
                            }

                            previousValid = true;
                            previousDay = day;
                            previousMonth = month;
                            previousYear = year;
                        }
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }

                }
                if (count != 0) {
                    double selVal = selectResult(timeStep, sum, sq_sum, count, min, max, l_median);
                    l.add(selVal);
                } // add the final entry which wasn't yet added
                // since it never hit a newEntry.

                // Copy the List to the output array.
                double[] arr = new double[l.size()];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = l.get(i);
                }
                return arr;
                // break;
            }

            default: {
                throw new IllegalArgumentException("timeStep " + timeStep + "not supported.");
            }
        }
    }


    private static double selectResult(int timeStep, double sum, double sq_sum, int count, double min, double max, List<Double> l) {
        if ((timeStep == ANNUAL_MEAN) || (timeStep == PERIOD_MEAN)) {
            return (count == 0) ? 0 : (sum / count);
        } else if (timeStep == PERIOD_MIN) {
            return min;
        } else if (timeStep == PERIOD_MAX) {
            return max;
        } else if (timeStep == PERIOD_MEDIAN) {
            //Copy to array of double
            int lSize = l.size();
            if (lSize == 0) {
                throw new RuntimeException("No data in file matched the specified period ");
            }
            double[] arr = new double[lSize];
            for (int i = 0; i < arr.length; i++) {
                arr[i] = l.get(i);
            }

            // Sort the Array
            Arrays.sort(arr);
            double median;

            // Pull out the Median
            if (lSize % 2 == 1) {
                median = arr[(lSize + 1) / 2 - 1];
            } else {
                double lower = arr[(lSize / 2) - 1];
                double upper = arr[lSize / 2];
                median = (lower + upper) / 2.0;
            }
            return median;
        } else if (timeStep == PERIOD_STANDARD_DEVIATION) {
            double mean = (count == 0) ? 0 : (sum / count);
            double variance = sq_sum / count - (mean * mean);
            double standardDeviation = Math.sqrt(variance);
            return standardDeviation;
        } else {
            throw new RuntimeException("TimeStep " + timeStep + " not supported here.");
        }

    }


    public static SimpleDateFormat lookupDateFormat(CSTable table, int col) {
        if (col < 0 || col > table.getColumnCount()) {
            throw new IllegalArgumentException("invalid column: " + col);
        }
        String format = table.getColumnInfo(col).get(KEY_FORMAT);
        if (format == null) {
            format = table.getInfo().get(DATE_FORMAT);
        }
        if (format == null) {
            format = Conversions.ISO().toPattern();
        }
        return new SimpleDateFormat(format);
    }


    public static boolean DateFormatHasYear(CSTable table, int col) {
        if (col < 0 || col > table.getColumnCount()) {
            throw new IllegalArgumentException("invalid column: " + col);
        }
        String format = table.getColumnInfo(col).get(KEY_FORMAT);
        if (format == null) {
            format = table.getInfo().get(DATE_FORMAT);
        }
        if (format == null) {
            format = Conversions.ISO().toPattern();
        }
        return (format.contains("YY") || format.contains("yy"));
    }


    public static int findRowByDate(Date date, int dateColumn, CSTable table) {
        long start = System.currentTimeMillis();

        String type = table.getColumnInfo(dateColumn).get(KEY_TYPE);
        if ((type == null) || !type.equalsIgnoreCase(VAL_DATE)) {
            throw new IllegalArgumentException("type " + type);
        }

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date);

        String year = Integer.toString(cal.get(Calendar.YEAR));
        DateFormat fmt = lookupDateFormat(table, dateColumn);

        int rowNo = 0;

        TableIterator<String[]> rows = (TableIterator<String[]>) table.rows().iterator();
        while (rows.hasNext()) {
            String[] row = rows.next();
            try {
                if (row[dateColumn].contains(year)) {
                    Date d = fmt.parse(row[dateColumn]);
                    if (d.equals(date)) {
                        long end = System.currentTimeMillis();
//                        System.out.println("Found row in " + (end - start) + " ms  " + rowNo);
                        return rowNo;
                    }
                }
                rowNo++;
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
        }
        rows.close();

//        for (String[] row : table.rows()) {
//            try {
//                Date d = fmt.parse(row[dateColumn]);
//                if (d.equals(date)) {
//                    return rowNo;
//                }
//                rowNo++;
//            } catch (ParseException ex) {
//                throw new RuntimeException(ex);
//            }
//        }
        throw new IllegalArgumentException(date.toString());
    }


    /**
     * Extract all rows from the table, inclusive startRow and endRow.
     *
     * @param table the src table
     * @param startRow the first row to include
     * @param endRow the second row to include.
     * @return the rows.
     */
    public static List<String[]> extractRows(CSTable table, int startRow, int endRow) {
        if (endRow <= startRow || startRow < 0 || endRow < 0) {
            throw new IllegalArgumentException("invalid start/end Row : " + startRow + "/" + endRow);
        }
        List<String[]> l = new ArrayList<String[]>();
        for (String[] r : table.rows(startRow)) {
            l.add(r);
            if (Integer.parseInt(r[0]) > endRow) {
                break;
            }
        }
        return l;
    }


    /**
     * Get a slice of rows out of the table matching the time window
     *
     * @param table the src table
     * @param timeCol the column index of the timestamps
     * @param start the start date of the window
     * @param end the end date of the window
     * @return the first and last row that matches the time window {@literal start->end}
     */
    public static int[] sliceByTime(CSTable table, int timeCol, Date start, Date end) {
        if (end.before(start)) {
            throw new IllegalArgumentException("end < start:" + end + "  <  " + start);
        }
        if (timeCol < 0) {
            throw new IllegalArgumentException("timeCol :" + timeCol);
        }
        int s = -1;
        int e = -1;
        int i = -1;
        SimpleDateFormat df = lookupDateFormat(table, timeCol);
        for (String[] col : table.rows()) {
            i++;
            Date d = Conversions.convert(col[timeCol], Date.class, df);
            if (s == -1 && (start.before(d) || start.equals(d))) {
                s = i;
            }
            if (e == -1 && (end.before(d) || end.equals(d))) {
                e = i;
                break;
            }
        }
        return new int[]{s, e};
    }


    /**
     * Create a r/o data tablemodel
     *
     * @param src the src table
     * @return a table model to the CSTable
     */
    public static TableModel createTableModel(final CSTable src) {
        final List<String[]> rows = new ArrayList<String[]>();
        for (String[] row : src.rows()) {
            rows.add(row);
        }

        return new TableModel() {
            @Override
            public int getColumnCount() {
                return src.getColumnCount();
            }


            @Override
            public String getColumnName(int column) {
                return src.getColumnName(column);
            }


            @Override
            public int getRowCount() {
                return rows.size();
            }


            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }


            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return false;
            }


            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return rows.get(rowIndex)[columnIndex];
            }


            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
//        rows.get(rowIndex)[columnIndex] = (String) aValue;
            }


            @Override
            public void addTableModelListener(TableModelListener l) {
            }


            @Override
            public void removeTableModelListener(TableModelListener l) {
            }
        };
    }


    /**
     * Get the KVP as table.
     *
     * @param p the src comma separated properties
     * @return an AbstractTableModel for properties (KVP)
     */
    public static AbstractTableModel getProperties(final CSProperties p) {

        return new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return p.keySet().size();
            }


            @Override
            public int getColumnCount() {
                return 2;
            }


            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return " " + p.keySet().toArray()[rowIndex];
                } else {
                    return p.values().toArray()[rowIndex];
                }
            }


            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 1;
            }


            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                if (columnIndex == 1) {
                    String[] keys = p.keySet().toArray(new String[0]);
                    p.put(keys[rowIndex], aValue.toString());
                }
            }


            @Override
            public String getColumnName(int column) {
                return column == 0 ? "Name" : "Value";
            }


            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };
    }


    public static AbstractTableModel get2DBounded(final CSProperties p, final String pname) throws ParseException {

        String m = p.getInfo(pname).get("bound");

        String[] dims = m.split(",");
        final int rows = DataIO.getInt(p, dims[0].trim());
        final int cols = DataIO.getInt(p, dims[1].trim());

        return new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return rows;
            }


            @Override
            public int getColumnCount() {
                return cols;
            }


            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return true;
            }


            @Override
            public Object getValueAt(int row, int col) {
                String[][] d = Conversions.convert(p.get(pname), String[][].class);
                return d[row][col].trim();
            }


            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                String[][] d = Conversions.convert(p.get(pname), String[][].class);
                d[rowIndex][columnIndex] = aValue.toString().trim();
                String s = toArrayString(d);
                p.put(pname, s);
            }


            @Override
            public String getColumnName(int column) {
                return Integer.toString(column);
            }


            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };
    }


    static public boolean playsRole(final CSProperties p, String key, String role) {
        String r = p.getInfo(key).get("role");
        if (r == null) {
            return false;
        }
        return r.contains(role);
    }


    static public int getBound(final CSProperties p, String key) {
        String bound = p.getInfo(key).get("bound");
        if (bound == null) {
            return 0;
        }
        StringTokenizer t = new StringTokenizer(bound, ",");
        return t.countTokens();
    }


    static public boolean isBound(final CSProperties p, String key, int dim) {
        String bound = p.getInfo(key).get("bound");
        if (bound == null) {
            return false;
        }
        StringTokenizer t = new StringTokenizer(bound, ",");
        if (t.countTokens() == dim) {
            return true;
        }
        return false;
    }


    /**
     * Copy a property with meta data
     *
     * @param key the key to copy
     * @param from the src comma separated properties
     * @param to the dest comma separated properties
     */
    public static void copyProperty(String key, CSProperties from, CSProperties to) {
        Object v = from.get(key);
        Map<String, String> meta = from.getInfo(key);
        to.put(key, v);
        to.getInfo(key).putAll(meta);
    }


    // 1D arrays
    public static AbstractTableModel getBoundProperties(final CSProperties p, String boundName) throws ParseException {

        final int rows = DataIO.getInt(p, boundName);
        final List<String> arr = keysByMeta(p, "bound", boundName);

        return new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return rows;
            }


            @Override
            public int getColumnCount() {
                return arr.size();
            }


            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                String colname = arr.get(columnIndex);
                String[] d = Conversions.convert(p.get(colname), String[].class);
                return d[rowIndex].trim();
            }


            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return true;
            }


            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                String colname = arr.get(columnIndex);
                String[] d = Conversions.convert(p.get(colname), String[].class);
                d[rowIndex] = aValue.toString().trim();
                String s = toArrayString(d);
                p.put(colname, s);
            }


            @Override
            public String getColumnName(int column) {
                String cname = arr.get(column);
                return cname.substring(cname.indexOf('$') + 1);
            }


            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };
    }


    // unbound
    public static AbstractTableModel getUnBoundProperties(final CSProperties p) throws ParseException {

        final List<String> arr = keysByNotMeta(p, "bound");

        return new AbstractTableModel() {
            @Override
            public int getRowCount() {
                return arr.size();
            }


            @Override
            public int getColumnCount() {
                return 2;
            }


            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (columnIndex == 0) {
                    return arr.get(rowIndex);
                } else {
                    return p.get(arr.get(rowIndex));
                }
            }


            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return columnIndex == 1;
            }


            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
                p.put(arr.get(rowIndex), aValue.toString());
            }


            @Override
            public String getColumnName(int column) {
                return (column == 0) ? "Key" : "Value";
            }


            @Override
            public Class<?> getColumnClass(int columnIndex) {
                return String.class;
            }
        };
    }


    /**
     * Create array string.
     *
     * @param arr the input array of strings
     * @return an array String.
     */
    public static String toArrayString(String[] arr) {
        StringBuilder b = new StringBuilder();
        b.append('{');
        for (int i = 0; i < arr.length; i++) {
            b.append(arr[i]);
            if (i < arr.length - 1) {
                b.append(',');
            }
        }
        b.append('}');
        return b.toString();
    }


    public static String toArrayString(String[][] arr) {
        StringBuilder b = new StringBuilder();
        b.append('{');
        for (int i = 0; i < arr.length; i++) {
            b.append('{');
            for (int j = 0; j < arr[i].length; j++) {
                b.append(arr[i][j]);
                if (j < arr[i].length - 1) {
                    b.append(',');
                }
            }
            b.append('}');
            if (i < arr.length - 1) {
                b.append(',');
            }
        }
        b.append('}');
        return b.toString();
    }


    /**
     * Returns a r/o table from a CSP file
     *
     * @param p the src comma separated properties
     * @param dim dimension of the comma separated properties
     * @return a table model for properties with dimension.
     */
    public static TableModel fromCSP(CSProperties p, final int dim) {
        List<String> dims = keysByMeta(p, "role", "dimension");
        if (dims.isEmpty()) {
            return null;
        }
        for (String d : dims) {
            if (Integer.parseInt(p.get(d).toString()) == dim) {
                final List<String> bounds = keysByMeta(p, "bound", d);
                final List<Object> columns = new ArrayList<Object>(bounds.size());
                for (String bound : bounds) {
                    columns.add(Conversions.convert(p.get(bound), double[].class));
                }

                return new AbstractTableModel() {
                    @Override
                    public int getRowCount() {
                        return dim;
                    }


                    @Override
                    public int getColumnCount() {
                        return bounds.size();
                    }


                    @Override
                    public Object getValueAt(int rowIndex, int columnIndex) {
                        return Array.get(columns.get(columnIndex), rowIndex);
                    }


                    @Override
                    public String getColumnName(int column) {
                        return bounds.get(column);
                    }


                    @Override
                    public Class<?> getColumnClass(int columnIndex) {
                        return Double.class;
                    }
                };
            }
        }
        return null;
    }


    /**
     *
     * @param csp the src comma separated properties
     * @param mkey the meta data key
     * @param mval the meta data value
     * @return the list of property keys that have a meta data value.
     */
    public static List<String> keysByMeta(CSProperties csp, String mkey, String mval) {
        List<String> l = new ArrayList<String>();
        for (String key : csp.keySet()) {
            if (csp.getInfo(key).keySet().contains(mkey)) {
                String role = csp.getInfo(key).get(mkey);
                if (role.equals(mval)) {
                    l.add(key);
                }
            }
        }
        return l;
    }


    public static List<String> keysForBounds(CSProperties csp, int boundCount) {
        List<String> l = new ArrayList<String>();
        for (String key : csp.keySet()) {
            if (csp.getInfo(key).keySet().contains("bound")) {
                String bound = csp.getInfo(key).get("bound");
                StringTokenizer t = new StringTokenizer(bound, ",");
                if (t.countTokens() == boundCount) {
                    l.add(key);
                }
            }
        }
        return l;
    }


    public static List<String> keysByNotMeta(CSProperties csp, String mkey) {
        List<String> l = new ArrayList<String>();
        for (String key : csp.keySet()) {
            if (!csp.getInfo(key).keySet().contains(mkey)) {
                l.add(key);
            }
        }
        return l;
    }


    public static Date[] getColumnDateValues(CSTable t, String columnName) {
        int col = columnIndex(t, columnName);
        if (col == -1) {
            throw new IllegalArgumentException("No such column: " + columnName);
        }

        Conversions.Params p = new Conversions.Params();
        p.add(String.class, Date.class, lookupDateFormat(t, col));

        List<Date> l = new ArrayList<Date>();
        for (String[] s : t.rows()) {
            l.add(Conversions.convert(s[col], Date.class, p));
        }
        return l.toArray(new Date[0]);
    }


    /**
     * Get a column as an int array.
     *
     * @param t the src comma separated table
     * @param columnName the column name
     * @return the column data as doubles.
     */
    public static Double[] getColumnDoubleValues(CSTable t, String columnName) {
        int col = columnIndex(t, columnName);
        if (col == -1) {
            throw new IllegalArgumentException("No such column: " + columnName);
        }
        List<Double> l = new ArrayList<Double>();
        for (String[] s : t.rows()) {
            l.add(new Double(s[col]));
        }
        return l.toArray(new Double[0]);
    }


    /**
     * Get a column as an int array.
     *
     * @param t the src comma separated table
     * @param columnName the column name
     * @return the column data as doubles.
     */
    public static double[] getColumnValues(CSTable t, String columnName) {
        int col = columnIndex(t, columnName);
        if (col == -1) {
            throw new IllegalArgumentException("No such column: " + columnName);
        }
        List<Double> l = new ArrayList<Double>();
        for (String[] s : t.rows()) {
            l.add(new Double(s[col]));
        }
        double[] f = new double[l.size()];
        for (int i = 0; i < f.length; i++) {
            f[i] = l.get(i);
        }
        return f;
    }


    /**
     * Get a value as date.
     *
     * @param p the src comma separated properties
     * @param key the key of the value to parse
     * @return a property as Date
     * @throws ParseException unexpected parsing error
     */
    public static Date getDate(CSProperties p, String key) throws ParseException {
        String val = p.get(key).toString();
        if (val == null) {
            throw new IllegalArgumentException(key);
        }
        String f = p.getInfo(key).get(KEY_FORMAT);
        DateFormat fmt = new SimpleDateFormat(f == null ? ISO8601 : f);
        return fmt.parse(val);
    }


    /**
     * Get a value as integer.
     *
     * @param p the src comma separated properties
     * @param key the key of the value to parse
     * @return a property value as integer.
     * @throws ParseException unexpected parsing error
     */
    public static int getInt(CSProperties p, String key) throws ParseException {
        String val = p.get(key).toString();
        if (val == null) {
            throw new IllegalArgumentException(key);
        }
        return Integer.parseInt(val);
    }


    public static void save(CSProperties csp, File f, String title) {
        PrintWriter w = null;
        try {
            if (csp instanceof BasicCSProperties) {
                BasicCSProperties c = (BasicCSProperties) csp;
                c.setName(title);
            }
            w = new PrintWriter(f);
            DataIO.print(csp, w);
            w.close();
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } finally {
            if (w != null) {
                w.close();
            }
        }
    }


    /**
     * Print CSProperties.
     *
     * @param props the Properties to print
     * @param out the output writer to print to.
     */
    public static void print(CSProperties props, PrintWriter out) {
        out.println(PROPERTIES + "," + CSVParser.printLine(props.getName()));
        for (String key : props.getInfo().keySet()) {
            out.println(" " + CSVParser.printLine(key, props.getInfo().get(key)));
        }
        out.println();
        for (String key : props.keySet()) {
            out.println(PROPERTY + "," + CSVParser.printLine(key, props.get(key).toString()));
            for (String key1 : props.getInfo(key).keySet()) {
                out.println(" " + CSVParser.printLine(key1, props.getInfo(key).get(key1)));
            }
            out.println();
        }
        out.println();
        out.flush();
    }


    /**
     * Print CSProperties and embed tables if there are any.
     *
     * @param props the Properties to print
     * @param out the output writer to print to.
     */
    public static void printWithTables(CSProperties props, PrintWriter out) {
        out.println(PROPERTIES + "," + CSVParser.printLine(props.getName()));
        for (String key : props.getInfo().keySet()) {
            if (!key.startsWith("oms")) {
                out.println(" " + CSVParser.printLine(key, props.getInfo().get(key)));
            }
        }
        out.println();

        List<String> vars = new ArrayList<>();
        String[] _1D = null;
        String[] _2D = null;

        if (props.getInfo().containsKey("oms.1D")) {
            _1D = Conversions.convert(props.getInfo().get("oms.1D"), String[].class);
            for (String dim : _1D) {
                List<String> arrays = DataIO.keysByMeta(props, "bound", dim.trim());
                vars.addAll(arrays);
            }
        }
        if (props.getInfo().containsKey("oms.2D")) {
            _2D = Conversions.convert(props.getInfo().get("oms.2D"), String[].class);
            for (String var : _2D) {
                vars.add(var.trim());
            }
        }

        // normal properties
        for (String key : props.keySet()) {
            if (!vars.contains(key)) {
                out.println(PROPERTY + "," + CSVParser.printLine(key, props.get(key).toString()));
                for (String key1 : props.getInfo(key).keySet()) {
                    out.println(" " + CSVParser.printLine(key1, props.getInfo(key).get(key1)));
                }
                out.println();
            }
        }

        // tables
        if (_1D != null) {
            for (String dim : _1D) {
                List<String> arrays = DataIO.keysByMeta(props, "bound", dim.trim());
                vars.addAll(arrays);
                CSTable table = asTable(props, dim.trim());
                if (table == null) {
                    continue;
                }
                DataIO.print(table, out);
                out.println();
            }
        }

        if (_2D != null) {
            for (String var : _2D) {
                vars.add(var.trim());
                CSTable table = as2DTable(props, var.trim());
                if (table == null) {
                    continue;
                }
                DataIO.print(table, out);
                out.println();
            }
        }
        out.flush();
    }


    public static void print(Map<String, Object> props, String header, PrintWriter out) {
        out.println(PROPERTIES + "," + header);
        out.println();
        for (String key : props.keySet()) {
            out.println(PROPERTY + "," + CSVParser.printLine(key, props.get(key).toString()));
        }
        out.println();
        out.flush();
    }


    /**
     * Print a CSTable to a PrintWriter
     *
     * @param table the table to print
     * @param out the writer to write to
     */
    public static void print(CSTable table, PrintWriter out) {
        out.println(TABLE + "," + CSVParser.printLine(table.getName()));
        for (String key : table.getInfo().keySet()) {
            out.println(CSVParser.printLine(key, table.getInfo().get(key)));
        }
        if (table.getColumnCount() < 1) {
            out.flush();
            return;
        }
        out.print(HEADER);
        for (int i = 1; i <= table.getColumnCount(); i++) {
            out.print("," + table.getColumnName(i));
        }
        out.println();
        Map<String, String> m = table.getColumnInfo(1);
        for (String key : m.keySet()) {
            out.print(key);
            for (int i = 1; i <= table.getColumnCount(); i++) {
                out.print("," + table.getColumnInfo(i).get(key));
            }
            out.println();
        }
        for (String[] row : table.rows()) {
            for (int i = 1; i < row.length; i++) {
                out.print("," + row[i]);
            }
            out.println();
        }
        out.println();
        out.flush();
    }


    /**
     * Saves a table to a file.
     *
     * @param table the table to save
     * @param file the file to store it in (overwritten, if exists)
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static void save(CSTable table, File file) throws IOException {
        PrintWriter w = new PrintWriter(file);
        print(table, w);
        w.close();
    }


    /**
     * Parse properties from a reader
     *
     * @param r the Reader
     * @param name the name of the properties
     * @return properties from a file.
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static CSProperties properties(Reader r, String name) throws IOException {
        return new CSVProperties(r, name);
    }


    public static CSProperties properties(File r, String name) throws IOException {
        return new CSVProperties(new FileReader(r), name);
    }


    /**
     * Create a CSProperty from an array of reader.
     *
     * @param r the array of reader
     * @param name the of the comma separated properties object
     * @return merged properties.
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static CSProperties properties(Reader[] r, String name) throws IOException {
        CSVProperties p = new CSVProperties(r[0], name);
        for (int i = 1; i < r.length; i++) {
            CSVParser csv = new CSVParser(r[i], CSVStrategy.DEFAULT_STRATEGY);
            locate(csv, name, PROPERTIES, PROPERTIES1);
            p.readProps(csv);
            r[i].close();
        }
        return p;
    }


    /**
     * Merges two Properties, respects permissions
     *
     * @param base the base comma separated properties to merge to
     * @param overlay the overlay comma separated properties that gets merged
     */
    public static void merge(CSProperties base, CSProperties overlay) {
        for (String key : overlay.keySet()) {
            if (base.getInfo(key).containsKey("public")) {
                base.put(key, overlay.get(key));
            } else {
                throw new IllegalArgumentException("Not public: " + key);
            }
        }
    }


    /**
     * Merge two tables together under a new name.
     *
     * @param t1 the first table
     * @param t2 the second table
     * @param name the new name of the table
     * @return the merged table
     */
    public static CSTable merge(CSTable t1, CSTable t2, String name) {

        MemoryTable t = new MemoryTable();
        t.setName(name);
        t.getInfo().putAll(t1.getInfo());
        t.getInfo().putAll(t2.getInfo());

        // header
        List<String> t1_col = columnNames(t1);
        List<String> t2_col = columnNames(t2);

        int t1_c = t1_col.size();
        int t2_c = t2_col.size();

        t1_col.addAll(t2_col);

        t.setColumns(t1_col.toArray(new String[0]));

        int col = 1;
        for (int i = 0; i < t1_c; i++) {
            t.getColumnInfo(col).putAll(t1.getColumnInfo(col));
            col++;
        }
        for (int i = 1; i <= t2_c; i++) {
            t.getColumnInfo(col).putAll(t2.getColumnInfo(i));
            col++;
        }

        String[] r = new String[t1_col.size()];
        Iterator<String[]> row_1 = t1.rows().iterator();
        Iterator<String[]> row_2 = t2.rows().iterator();

        while (row_1.hasNext() && row_2.hasNext()) {
            String[] s1 = row_1.next();
            String[] s2 = row_2.next();
            System.arraycopy(s1, 1, r, 0, s1.length - 1);
            System.arraycopy(s2, 1, r, s1.length - 1, s2.length - 1);
            t.addRow((Object[]) r);
        }
        return t;
    }


    /**
     * Convert CSProperties into Properties
     *
     * @param p the src comma separated properties
     * @return the Properties.
     */
    public static Properties properties(CSProperties p) {
        Properties pr = new Properties();
        pr.putAll(p);
        return pr;
    }


    /**
     * Convert Properties to CSProperties.
     *
     * @param p the Properties
     * @return CSProperties
     */
    public static CSProperties properties(Properties p) {
        return new BasicCSProperties(p);
    }


    /**
     * Convert from a Map to properties.
     *
     * @param p the source map
     * @return CSProperties
     */
    public static CSProperties properties(Map<String, Object> p) {
        return new BasicCSProperties(p);
    }


    /**
     * Create Empty properties
     *
     * @return get some empty properties.
     */
    public static CSProperties properties() {
        return new BasicCSProperties();
    }


    /**
     * Parse the first table from a file
     *
     * @param file the file to parse
     * @return the CSTable
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static CSTable table(File file) throws IOException {
        return table(file, null);
    }


    /**
     * Parse a table from a given File.
     *
     * @param file the input file
     * @param name the table name
     * @return a CSTable.
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static CSTable table(File file, String name) throws IOException {
        return new FileTable(file, name);
    }


    /**
     * Parse a table from a Reader. Find the first table
     *
     * @param s the Reader to read from
     * @return the CSTable
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static CSTable table(String s) throws IOException {
        return table(s, null);
    }


    /**
     * Parse a table from a Reader.
     *
     * @param s the Reader to read from
     * @param name the name of the table
     * @return the CSTable
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static CSTable table(String s, String name) throws IOException {
        return new StringTable(s, name);
    }


    /**
     * Opens the first table found at the URL
     *
     * @param url the URL
     * @return the CSTable
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static CSTable table(URL url) throws IOException {
        return table(url, null);
    }


    /**
     * Create a CSTable from a URL source.
     *
     * @param url the table URL
     * @param name the name of the table
     * @return a new CSTable
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static CSTable table(URL url, String name) throws IOException {
        return new URLTable(url, name);
    }


    /**
     * Check if a column exist in table.
     *
     * @param table the table to check
     * @param name the name of the column
     * @return true is the column exists, false otherwise
     */
    public static boolean columnExist(CSTable table, String name) {
        for (int i = 1; i <= table.getColumnCount(); i++) {
            if (table.getColumnName(i).startsWith(name)) {
                return true;
            }
        }
        return false;
    }


    /**
     * Gets a column index by name
     *
     * @param table The table to check
     * @param name the column name
     * @return the index of the column
     */
    public static int columnIndex(CSTable table, String name) {
        for (int i = 1; i <= table.getColumnCount(); i++) {
            if (table.getColumnName(i).equals(name)) {
                return i;
            }
        }
        return -1;
    }


    /**
     * Get the column indexes for a given column name. (e.g. use tmin to fetch
     * tmin[0], tmin[1]...)
     *
     * @param table the table
     * @param name the column name
     * @return indexes of matching column names
     */
    public static int[] columnIndexes(CSTable table, String name) {
        List<Integer> l = new ArrayList<Integer>();
        for (int i = 1; i <= table.getColumnCount(); i++) {
            if (table.getColumnName(i).startsWith(name)) {
                l.add(i);
            }
        }
        if (l.isEmpty()) {
            return null;
        }
        int[] idx = new int[l.size()];
        for (int i = 0; i < idx.length; i++) {
            idx[i] = l.get(i);
        }
        return idx;
    }


    public static List<String> columnNames(CSTable table) {
        List<String> l = new ArrayList<String>();
        for (int i = 1; i <= table.getColumnCount(); i++) {
            l.add(table.getColumnName(i));
        }
        return l;
    }


    public static List<String> columnNames(CSTable table, String name) {
        List<String> l = new ArrayList<String>();
        for (int i = 1; i <= table.getColumnCount(); i++) {
            if (table.getColumnName(i).startsWith(name)) {
                l.add(table.getColumnName(i));
            }
        }
        if (l.isEmpty()) {
            throw new IllegalArgumentException("No column(s) '" + name + "' in table: " + table.getName());
        }

        return l;
    }


    public static Map<String, String[]> columnMetaData(CSTable table) {
        Map<String, String[]> meta = new HashMap<String, String[]>();
        int n = table.getColumnCount();
        for (int i = 1; i <= table.getColumnCount(); i++) {
            Map<String, String> m = table.getColumnInfo(i);
            for (String k : m.keySet()) {
                if (!meta.containsKey(k)) {
                    String[] s = new String[n];
                    meta.put(k, s);
                }
                String[] s = meta.get(k);
                s[i - 1] = m.get(k);
            }
        }
        return meta;
    }


    public static void rowStringValues(String row[], int[] idx, String[] vals) {
        for (int i = 0; i < vals.length; i++) {
            vals[i] = row[idx[i]];
        }
    }


    public static double[] rowDoubleValues(String row[], int[] idx, double[] vals) {
        for (int i = 0; i < vals.length; i++) {
            vals[i] = Double.parseDouble(row[idx[i]]);
        }
        return vals;
    }


    public static double[] rowDoubleValues(String row[], int[] idx) {
        double[] vals = new double[idx.length];
        return rowDoubleValues(row, idx, vals);
    }


    /**
     * Extract the columns and create another table.
     *
     * @param table the table
     * @param colNames the names of the columns to extract.
     * @return A new Table with the Columns.
     */
    public static CSTable extractColumns(CSTable table, String... colNames) {
        int[] idx = {};
        for (String name : colNames) {
            idx = add(idx, columnIndexes(table, name));
        }
        if (idx.length == 0) {
            throw new IllegalArgumentException("No column names: " + Arrays.toString(colNames));
        }
        return extractColumns(table, idx);
    }


    public static CSTable extractColumns(CSTable table, int[] idx) {
        String[] colNames = new String[idx.length];
        for (int i = 0; i < colNames.length; i++) {
            colNames[i] = table.getColumnName(idx[i]);
        }

        List<String> cols = new ArrayList<String>();
        for (String name : colNames) {
            cols.addAll(columnNames(table, name));
        }

        MemoryTable t = new MemoryTable();
        t.setName(table.getName());
        t.getInfo().putAll(table.getInfo());

        // header
        t.setColumns(cols.toArray(new String[0]));
        for (int i = 0; i < idx.length; i++) {
            t.getColumnInfo(i + 1).putAll(table.getColumnInfo(idx[i]));
        }

        String[] r = new String[idx.length];
        for (String[] row : table.rows()) {
            rowStringValues(row, idx, r);
            t.addRow((Object[]) r);
        }

        return t;
    }


    public static String diff(double[] o, double[] p) {
        String status = "ok.";
        if (o.length != p.length) {
            status = "o.length != p.length";
        } else {
            for (int i = 0; i < o.length; i++) {
                if (o[i] != p[i]) {
                    status += "error";
                }
            }
        }
        return status;
    }


    /**
     * Converts a table t a 2D array. The 2D array is stored in row/column
     * order.
     *
     * @param table the src comma separated table
     * @return the 2 D array.
     */
    public static double[][] asArray(CSTable table) {
        List<double[]> l = new ArrayList<double[]>();
        for (String[] row : table.rows()) {
            double[] r = new double[row.length - 1];
            for (int i = 1; i < row.length; i++) {
                r[i - 1] = Double.parseDouble(row[i]);
            }
            l.add(r);
        }
        double[][] d = new double[l.size()][];
        for (int i = 0; i < d.length; i++) {
            d[i] = l.get(i);
        }
        return d;
    }


    public static CSProperties from2DTable(CSTable t) {

        String n = t.getName();
        if (n == null) {
            throw new RuntimeException("2D variable name missing");
        }

        double[][] d = asArray(t);
        BasicCSProperties p = new BasicCSProperties();
        Map<String, String> m = new LinkedHashMap<String, String>();

        m.put("bound", t.getInfo().get("bound"));
        m.put("len", d.length + "," + d[0].length);
        p.put(n, Conversions.convert(d, String.class));
        p.setInfo(n, m);
        return p;
    }


    public static CSTable toTable(CSProperties p, String dim) {
        return asTable(p, dim);
    }


    /**
     * @deprecated replaced by {@link #asTable(CSProperties, String, String) asTable}
     * @param props the src comma separated properties
     * @param dim the dimension
     * @return the comma separated table
     */
    public static CSTable asTable(CSProperties props, String dim) {
        List<String> arrays = DataIO.keysByMeta(props, "bound", dim);
        if (arrays.isEmpty()) {
            // nothing is bound to this
            return null;
        }
        int len = 0;
        List<String[]> m = new ArrayList<>();
        for (String arr : arrays) {
            String[] d = Conversions.convert(props.get(arr), String[].class);
            len = d.length;
            m.add(d);
        }

        List<Map<String, String>> meta = new ArrayList<>();
        for (String arr : arrays) {
            meta.add(new HashMap<>(props.getInfo(arr)));
        }

        String[] cols = new String[m.size()];
        for (int i = 0; i < arrays.size(); i++) {
            String a = arrays.get(i);
            cols[i] = a.substring(a.indexOf('$') + 1);
        }
//        System.out.println("Cols " + Arrays.toString(cols));

        MemoryTable table = new MemoryTable();
        table.getInfo().put("description", "Parameter bound by '" + dim + "'");
        table.getInfo().put("len", Integer.toString(len));
        table.setName(dim);

//      table.setColumns(arrays.toArray(new String[m.size()]));
        table.setColumns(cols);

        for (int i = 0; i < cols.length; i++) {
            Map<String, String> me = meta.get(i);
            me.remove("bound");
            me.remove("len");
            table.getColumnInfo(i + 1).putAll(meta.get(i));
        }

        String row[] = new String[m.size()];
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < m.size(); j++) {
                row[j] = m.get(j)[i].trim();
            }
            table.addRow((Object[]) row);
        }
        return table;
    }


    //TODO: this should go away
    public static CSTable asTable(CSProperties props, String dim, String tableName) {
        List<String> arrays = DataIO.keysByMeta(props, "bound", dim);
        if (arrays.isEmpty()) {
            // nothing is bound to this
            return null;
        }
        int len = 0;
        List<String[]> m = new ArrayList<String[]>();
        for (String arr : arrays) {
            String[] d = Conversions.convert(props.get(arr), String[].class);
            len = d.length;
            m.add(d);
        }
        MemoryTable table = new MemoryTable();
        table.getInfo().put("info", "Parameter bound by " + dim);
        table.setName(tableName);
        table.setColumns(arrays.toArray(new String[m.size()]));
        String row[] = new String[m.size()];
        for (int i = 0; i < len; i++) {
            for (int j = 0; j < m.size(); j++) {
                row[j] = m.get(j)[i].trim();
            }
            table.addRow((Object[]) row);
        }
        return table;
    }


    public static CSTable as2DTable(CSProperties props, String key) {
        Object val = props.get(key);
        String[][] vals = Conversions.convert(val, String[][].class);
        MemoryTable table = new MemoryTable();
        table.setName(key);
        table.getInfo().putAll(props.getInfo(key));
        if (!table.getInfo().containsKey("len")) {
            table.getInfo().put("len", vals.length + "," + vals[0].length);
        }

        String[] header = new String[vals[0].length];
        for (int i = 0; i < header.length; i++) {
            header[i] = Integer.toString(i);
        }
        table.setColumns(header);
        for (int row = 0; row < vals.length; row++) {
//            System.out.println("Row >>>>>>>>>  " + Arrays.toString(vals[row]));
            table.addRow((Object[]) vals[row]);
        }
        return table;
    }


    public static CSTable as2DTable(CSProperties p, String dim, String tableName) {
        List<String> arrays = DataIO.keysByMeta(p, "bound", dim);
//        System.out.println(">>>> " + tableName + "  " + arrays);
        if (arrays.isEmpty()) {
            // nothing is bound to this
            return null;
        }

        return as2DTable(p, arrays.get(0));
    }


    public static CSProperties toProperties(CSTable t) {
        String name = t.getName();
        if (t.getInfo().get("bound") != null && t.getInfo().get("bound").indexOf(',') != -1) {
            return from2DTable(t);
        } else {
            return from1DTable(t);
        }
    }


    /**
     * @param t the src comma separated table
     * @return the comma separated properties
     * @deprecated replaced by {@link #toProperties(CSTable) toProperties}
     */
    public static CSProperties fromTable(CSTable t) {
        String name = t.getName();
        if (t.getInfo().get("bound") != null && t.getInfo().get("bound").indexOf(',') != -1) {
            return from2DTable(t);
        } else {
            return from1DTable(t);
        }
    }


    public static CSProperties from1DTable(CSTable t) {
        BasicCSProperties p = new BasicCSProperties();

        Map<Integer, Map<String, String>> tableinfo = new HashMap<>();
        Map<Integer, List<String>> table = new HashMap<>();
        for (int i = 1; i <= t.getColumnCount(); i++) {
            table.put(i, new ArrayList<String>());
            tableinfo.put(i, t.getColumnInfo(i));
        }
//        System.out.println("reading table: " + t.getName());
        for (String[] row : t.rows()) {
            for (int i = 1; i < row.length; i++) {
                table.get(i).add(row[i].trim());
            }
        }

//        Map<String, String> m = new LinkedHashMap<String, String>();
        String tname = t.getName();

        for (int i = 1; i <= t.getColumnCount(); i++) {
            String name = t.getColumnName(i);
            p.put(tname + "$" + name, table.get(i).toString().replace('[', '{').replace(']', '}'));
            Map<String, String> m = tableinfo.get(i);
            Map<String, String> m1 = new LinkedHashMap<>();
            m1.putAll(m);
            m1.put("bound", tname);
            m1.put("len", Integer.toString(table.get(i).size()));
            p.setInfo(tname + "$" + name, m1);

//            System.out.println("DataIO.from1DTable: Properties set to " + tname + "$" + name);           
//            new Throwable().printStackTrace(System.out);
        }
        return p;
    }


    /**
     * Find all table names in a file.
     *
     * @param f the file to search in
     * @return a list of table names found in that file.
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static List<String> tables(File f) throws IOException {
        return findCSVElements(f, "@T");
    }


    /**
     * Find all properties section names in a file.
     *
     * @param table the name of the table
     * @param f the file to search in
     * @return a list of section names found in that file.
     * @throws IOException exception for failed or interrupted I/O operations
     */
    public static boolean tableExists(String table, File f) throws IOException {
        List<String> tables = tables(f);
        return (tables.contains(table));
    }


    public static List<String> properties(File f) throws IOException {
        return findCSVElements(f, "@S");
    }


    public static boolean propertyExists(String property, File f) throws IOException {
        List<String> properties = properties(f);
        return (properties.contains(property));
    }


    static List<String> findCSVElements(File f, String tag) throws IOException {
        List<String> l = new ArrayList<String>();
        Reader r = new FileReader(f);
        CSVParser csv = new CSVParser(r, CSVStrategy.DEFAULT_STRATEGY);
        String[] line = null;
        while ((line = csv.getLine()) != null) {
            if (line.length >= 2 && line[0].equals(tag)) {
                l.add(line[1]);
            }
        }
        r.close();
        return l;
    }


    /////////////////////////////////////////////////////////////////////////////
    /// private 
    private static int[] add(int[] a, int[] b) {
        int[] c = new int[a.length + b.length];
        System.arraycopy(a, 0, c, 0, a.length);
        System.arraycopy(b, 0, c, a.length, b.length);
        return c;
    }


    private static String locate(CSVParser csv, String name, String... type) throws IOException {
        if (name == null) {
            // match anything
            name = ".+";
        }
        Pattern p = Pattern.compile(name);
        String[] line = null;
        while ((line = csv.getLine()) != null) {
            if (line[0].startsWith(COMMENT) || !line[0].startsWith(P)) {
//            if (line.length != 2 || line[0].startsWith(COMMENT) || !line[0].startsWith(P)) {
                continue;
            }
            for (String s : type) {
                if (line[0].equalsIgnoreCase(s) && p.matcher(line[1].trim()).matches()) {
                    return line[1];
                }
            }
        }
        throw new IllegalArgumentException("Not found : " + Arrays.toString(type) + ", " + name);
    }

//    @SuppressWarnings("serial")
    private static class BasicCSProperties extends LinkedHashMap<String, Object> implements CSProperties {

        private static final long serialVersionUID = 1L;

        String name = "";
        Map<String, Map<String, String>> info = new HashMap<String, Map<String, String>>();


        BasicCSProperties(Properties p) {
            this();
            for (Object key : p.keySet()) {
                put(key.toString(), p.getProperty(key.toString()));
            }
        }


        BasicCSProperties(Map<String, Object> p) {
            this();
            for (String key : p.keySet()) {
                put(key, p.get(key));
            }
        }


        BasicCSProperties() {
            info.put(ROOT_ANN, new LinkedHashMap<String, String>());
        }


        @Override
        public void putAll(CSProperties p) {
            for (String key : p.keySet()) {
                if (containsKey(key)) {
                    throw new IllegalArgumentException("Duplicate key in parameter sets: " + key);
                }
            }
            super.putAll(p);
            for (String s : p.keySet()) {
                Map<String, String> m = p.getInfo(s);
                setInfo(s, m);
            }
            getInfo().putAll(p.getInfo());
        }


        @Override
        public String getName() {
            return name;
        }


        @Override
        public void setName(String name) {
            this.name = name;
        }


        @Override
        public synchronized Map<String, String> getInfo(String property) {
            Map<String, String> im = info.get(property);
//            return (im == null) ? NOINFO : im;
            if (im == null) {
                im = new HashMap<String, String>();
                info.put(property, im);
            }
            return im;
        }


        @Override
        public Map<String, String> getInfo() {
            return getInfo(ROOT_ANN);
        }


        @Override
        public void setInfo(String propertyname, Map<String, String> inf) {
            info.put(propertyname, inf);
        }

//        @Override
//        public String get(Object key) {
//            Object val = super.get(key.toString());
//            return val.toString();
////            return resolve(val != null ? val.toString() : null);
//        }
//        

        @Override
        public Object get(Object key) {
            Object val = super.get(key);
            if (val != null && val.getClass() == String.class) {
                return resolve((String) val);
            }
            return val;
        }


        /**
         * Resolve variable substitution.
         *
         * @P, dir, "/tmp/input"
         * @P, file, "${dir}/test.txt"
         *
         * - The referenced key has to be in the same properties set. - there
         * could be a chain of references, however, no recursion testing is
         * implemented.
         *
         * @param str
         * @return
         */
        private String resolve(String str) {
            if (str != null && str.contains("${")) {
                Matcher ma = null;
                while ((ma = varPattern.matcher(str)).find()) {
                    String key = ma.group(1);
                    String val = (String) get(key);
                    if (val == null) {
                        throw new IllegalArgumentException("value substitution failed for " + key);
                    }
                    Pattern repl = Pattern.compile("\\$\\{" + key + "\\}");
                    str = repl.matcher(str).replaceAll(val);
                }
            }
            return str;
        }
    }

    /**
     * Note: to keep the order of properties, it is sub-classed from
     * LinkedHashMap
     */
    @SuppressWarnings("serial")
    private static class CSVProperties extends BasicCSProperties implements CSProperties {

        CSVProperties(Reader reader, String name) throws IOException {
            super();
            CSVParser csv = new CSVParser(reader, CSVStrategy.DEFAULT_STRATEGY);
            this.name = locate(csv, name, PROPERTIES, PROPERTIES1);
            readProps(csv);
            reader.close();
        }


        private void readProps(CSVParser csv) throws IOException {
            Map<String, String> propInfo = null;
            String[] line = null;
            String propKey = ROOT_ANN;
            while ((line = csv.getLine()) != null
                    && !line[0].equalsIgnoreCase(PROPERTIES)
                    && !line[0].equalsIgnoreCase(PROPERTIES1)
                    && !line[0].equalsIgnoreCase(TABLE)
                    && !line[0].equalsIgnoreCase(TABLE1)) {
                if (line[0].startsWith(COMMENT) || line[0].isEmpty()) {
                    continue;
                }
                if (line[0].equalsIgnoreCase(PROPERTY) || line[0].equalsIgnoreCase(PROPERTY1)) {
                    if (line.length < 2) {
                        throw new IOException("Expected property name in line " + csv.getLineNumber());
                    }
                    propKey = line[1];
                    // maybe there is no value for the property, so we add null
                    put(propKey, (line.length > 2) ? line[2] : null);
                    propInfo = null;
                } else {
                    if (propInfo == null) {
                        info.put(propKey, propInfo = new HashMap<String, String>());
                    }
                    propInfo.put(line[0], (line.length > 1) ? line[1] : null);
                }
            }
        }
    }

    /**
     * CSVTable implementation
     */
    private static abstract class CSVTable implements CSTable {

        Map<Integer, Map<String, String>> info = new HashMap<>();
        String name;
        int colCount;
        String columnNames[];
        int firstline;
        static final CSVStrategy strategy = CSVStrategy.DEFAULT_STRATEGY;


        protected abstract Reader newReader();


        protected void init(String tableName) throws IOException {
            Reader r = newReader();
            BufferedReader br = new BufferedReader(r);
            if (r == null) {
                throw new NullPointerException("reader");
            }
            CSVParser csv = new CSVParser(br, strategy);
            name = locate(csv, tableName, TABLE, TABLE1);
            firstline = readTableHeader(csv);
            br.close();
        }


        private void skip0(BufferedReader csv, int lines) {
            try {
                while (lines-- > 0) {
                    csv.readLine();
                }
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private ThreadLocal<String[]> tempArray = new ThreadLocal<>();


        public String[] tokenize(String string, char delimiter) {
            String[] temp = tempArray.get();
            int tempLength = (string.length() / 2) + 2;
            if (temp == null || temp.length < tempLength) {
                temp = new String[tempLength];
                tempArray.set(temp);
            }
            int wordCount = 0;
            int i = 0;
            int j = string.indexOf(delimiter);
            while (j >= 0) {
                temp[wordCount++] = string.substring(i, j).trim();
                i = j + 1;
                j = string.indexOf(delimiter, i);
            }
            temp[wordCount++] = string.substring(i).trim();
            String[] result = new String[wordCount];
            System.arraycopy(temp, 0, result, 0, wordCount);
            return result;
        }


        private String[] readRow(BufferedReader csv) {
            try {
                String s = csv.readLine();
                if (s == null) {
                    return null;
                }
                while (s.trim().startsWith("#")) {
                    s = csv.readLine();
                    if (s == null) {
                        return null;
                    }
                }
                return tokenize(s.trim(), ',');

//                String s = csv.readLine();
//                return (s == null) ? null : tokenize(s.trim(), ',');
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }


        /**
         * Gets a row iterator.
         *
         * @return
         */
        @Override
        public Iterable<String[]> rows() {
            return rows(0);
        }


        /**
         * Gets a row iterator that starts at a give row.
         *
         * @param startRow the row to start parsing.
         * @return
         */
        @Override
        public Iterable<String[]> rows(final int startRow) {
            if (startRow < 0) {
                throw new IllegalArgumentException("startRow<0 :" + startRow);
            }

            return new Iterable<String[]>() {
                @Override
                public TableIterator<String[]> iterator() {
                    final Reader r = newReader();
                    if (r == null) {
                        throw new NullPointerException("reader");
                    }
                    final BufferedReader csv = new BufferedReader(r, 4096 * 4);

                    skip0(csv, firstline);
                    skip0(csv, startRow);

                    return new TableIterator<String[]>() {
                        String[] line = readRow(csv);
                        int row = startRow;


                        @Override
                        public void close() {
                            try {
                                csv.close();
                            } catch (IOException E) {
                            }
                        }


                        @Override
                        public boolean hasNext() {
                            boolean hn = (line != null && line.length > 1 && line[0].isEmpty());
                            if (!hn) {
                                close();
                            }
                            return hn;
                        }


                        @Override
                        public String[] next() {
                            String[] s = line;
                            s[0] = Integer.toString(++row);
                            line = readRow(csv);
                            return s;
                        }


                        @Override
                        public void remove() {
                            throw new UnsupportedOperationException();
                        }


                        @Override
                        public void skip(int n) {
                            if (n < 1) {
                                throw new IllegalArgumentException("n<1 : " + n);
                            }
                            skip0(csv, n - 1);
                            line = readRow(csv);
                            row += n;
                        }


                        @Override
                        protected void finalize() throws Throwable {
                            try {
                                close();
                            } finally {
                                super.finalize();
                            }
                        }
                    };
                }
            };
        }


        private int readTableHeader(CSVParser csv) throws IOException {
            Map<String, String> tableInfo = new LinkedHashMap<String, String>();
            info.put(-1, tableInfo);
            String[] line = null;
            while ((line = csv.getLine()) != null && !line[0].equalsIgnoreCase(HEADER)) {
                if (line[0].startsWith(COMMENT)) {
                    continue;
                }
                tableInfo.put(line[0], line.length > 1 ? line[1] : null);
            }
            if (line == null) {
                throw new IOException("Invalid table structure.");
            }
            colCount = line.length - 1;
            columnNames = new String[line.length];
            columnNames[0] = "ROW";
            for (int i = 1; i < line.length; i++) {
                columnNames[i] = line[i];
                info.put(i, new LinkedHashMap<String, String>());
            }
            while ((line = csv.getLine()) != null && !line[0].isEmpty()) {
                // avoid running into the next element.
                if (line[0].startsWith("@")) {
                    break;
                }
                if (line[0].startsWith(COMMENT)) {
                    continue;
                }
                for (int i = 1; i < line.length; i++) {
                    info.get(i).put(line[0], line[i]);
                }
            }
            assert (line != null && line[0].isEmpty());
            return csv.getLineNumber() - 1;
        }


        @Override
        public String getName() {
            return name;
        }


        @Override
        public Map<String, String> getInfo() {
            return getColumnInfo(-1);
        }


        @Override
        public Map<String, String> getColumnInfo(int column) {
            return Collections.unmodifiableMap(info.get(column));
        }


        @Override
        public int getColumnCount() {
            return colCount;
        }


        @Override
        public String getColumnName(int column) {
            return columnNames[column];
        }
    }

    private static class FileTable extends CSVTable {

        File file;


        FileTable(File f, String name) throws IOException {
            this.file = f;
            init(name);
        }


        @Override
        protected Reader newReader() {
            try {
                return new FileReader(file);
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static class StringTable extends CSVTable {

        String str;


        StringTable(String str, String name) throws IOException {
            this.str = str;
            init(name);
        }


        @Override
        protected Reader newReader() {
            return new StringReader(str);
        }
    }

    private static class URLTable extends CSVTable {

        URL url;


        URLTable(URL url, String name) throws IOException {
            this.url = url;
            init(name);
        }


        @Override
        protected Reader newReader() {
            try {
                return new InputStreamReader(url.openStream());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }


    public static String[] parseCsvFilename(String file) {
        String f = file.toLowerCase();
        if (!f.contains(".csv")) {
            throw new IllegalArgumentException("Not a csv file: " + file);
        }
        if (f.endsWith(".csv")) {
            return new String[]{file};
        }
        file = file.replace('\\', '/');
        int fpos = f.indexOf(".csv") + 4;
        String base = file.substring(0, fpos);
        String info = file.substring(fpos + 1);
        String[] parts = info.split("/");
        if (parts.length == 1) {
            return new String[]{base, parts[0]};
        } else if (parts.length == 2) {
            return new String[]{base, parts[0], parts[1]};
        } else {
            throw new IllegalArgumentException("Not a valid descriptior:" + file);
        }
    }

    private static ThreadLocal<String[]> tempArray = new ThreadLocal<String[]>();


    public static String[] tokenize(String string, char delimiter) {
        String[] temp = tempArray.get();
        int tempLength = (string.length() / 2) + 2;
        if (temp == null || temp.length < tempLength) {
            temp = new String[tempLength];
            tempArray.set(temp);
        }
        int wordCount = 0;
        int i = 0;
        int j = string.indexOf(delimiter);
        while (j >= 0) {
            temp[wordCount++] = string.substring(i, j).trim();
            i = j + 1;
            j = string.indexOf(delimiter, i);
        }
        temp[wordCount++] = string.substring(i).trim();
        String[] result = new String[wordCount];
        System.arraycopy(temp, 0, result, 0, wordCount);
        return result;
    }


    public static void main(String[] args) throws IOException, ParseException {

//        FileReader fr = new FileReader("/od/tmp/topo.csv");
//        BufferedReader br = new BufferedReader(fr);
//        String l = null;
//        while ((l=br.readLine()) != null) {
//            System.out.println("l " + l);
//        }
//        br.close();
        CSTable t = DataIO.table(new File("/od/tmp/reach.csv"));
//        CSTable t = DataIO.table(new File("/od/tmp/topo.csv"), "routing");

        if (true) {
            return;
        }

//        System.out.println(Arrays.toString(parseCsvFilename("/od/test/a.csv/name/col")));
        System.out.println(Arrays.toString(tokenize(",,test,a.csv,name,col", ',')));

//        DateFormat f = new SimpleDateFormat("yyyy-MM-dd HH:mm");
//        Date d = f.parse("2002-01-01 00:00");
//        System.out.println("date " + d);
//        
        String table = "@T, a\n"
                + "createdby, od\n"
                + "date, today\n"
                + "@H,idx, hru_coeff, area, me\n"
                + "type, Double, Double, Double\n"
                + ",1,1.3,3.5,5.6\n"
                + ",2,1.3,3.5,5.6\n"
                + ",3,1.3,3.5,5.6\n"
                + "\n";

        String table1 = "@T, a\n"
                + "bound, \"nhru,month\"\n"
                + "createdby, od\n"
                + "date, today\n"
                + "@H, 1, 2, 3\n"
                + "type, Double, Double, Double\n"
                + ",1.3,3.5,5.6\n"
                + ",1.3,3.5,5.6\n"
                + ",1.3,3.5,5.6\n"
                + ",1.3,3.5,5.6\n"
                + ",1.3,3.5,5.6\n"
                + "\n";
//
        CSTable t1 = table(table, "a");
        print(t1, new PrintWriter(System.out));
//
        CSProperties csp1 = fromTable(t1);
        print(csp1, new PrintWriter(System.out));

//      double[][] a = {{1.2,2.3,4.4},{3.2,5.5,6.6}};
//      double[][] a = asArray(t);
//      String s = Conversions.convert(a, String.class);
//      System.out.println("'" + s + "'");
        String a = "llsl~lslsl~lslsllslslsl";
        String[] o = a.split("\\~");
        System.out.println(Arrays.toString(o));

    }
}
