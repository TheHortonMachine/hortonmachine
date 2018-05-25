/*
 * $Id:$
 * 
 * This software is provided 'as-is', without any express or implied
 * warranty. In no event will the authors be held liable for any damages
 * arising from the use of this software.
 * 
 * Permission is granted to anyone to use this software for any purpose,
 * including commercial applications, and to alter it and redistribute it
 * freely, subject to the following restrictions:
 * 
 *  1. The origin of this software must not be misrepresented; you must not
 *     claim that you wrote the original software. If you use this software
 *     in a product, an acknowledgment in the product documentation would be
 *     appreciated but is not required.
 * 
 *  2. Altered source versions must be plainly marked as such, and must not be
 *     misrepresented as being the original software.
 * 
 *  3. This notice may not be removed or altered from any source
 *     distribution.
 */
package oms3.io;

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

/** Data Input/Output management.
 *
 * @author Olaf David
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
    private static final Map<String, String> NOINFO = Collections.unmodifiableMap(new HashMap<String, String>());
    private static final Pattern varPattern = Pattern.compile("\\$\\{([^$}]+)\\}");
    /* some static helpers, might have to go somewhere else */
    private static final String ISO8601 = "yyyy-MM-dd'T'hh:mm:ss";
    //
    // all meta data keys
    public static final String KEY_CONVERTED_FROM = "converted_from";
    public static final String DATE_FORMAT = "date_format";
    public static final String DATE_START = "date_start";
    public static final String DATE_END = "date_end";
    public static final String KEY_CREATED_AT = "created_at";
    public static final String KEY_CREATED_BY = "created_by";
    public static final String KEY_UNIT = "unit";
    public static final String KEY_FORMAT = "format";
    public static final String KEY_TYPE = "type";
    public static final String KEY_NAME = "name";
    public static final String KEY_MISSING_VAL = "missing_value";
    public static final String KEY_FC_START = "forecast_start";
    public static final String KEY_FC_DAYS = "forecast_days";
    public static final String KEY_HIST_YEAR = "historical_year";
    public static final String KEY_DIGEST = "digest";
    public static final String VAL_DATE = "Date";
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

    public static double[] getColumnDoubleValuesInterval(Date start, Date end, CSTable t, String columnName, int timeStep) {

        int col = columnIndex(t, columnName);
        if (col == -1) {
            throw new IllegalArgumentException("No such column: " + columnName);
        }

        DateFormat fmt = lookupDateFormat(t, 1);

        boolean useOrigDaily = false;
        switch (timeStep) {
            case DAILY:
                if (useOrigDaily) {
                    List<Double> l = new ArrayList<Double>();
                    for (String[] row : t.rows()) {
                        try {
                            Date d = fmt.parse(row[1]);
                            if ((d.equals(start) || d.after(start)) && (d.equals(end) || d.before(end))) {
                                l.add(new Double(row[col]));
                            }
                        } catch (ParseException ex) {
                            throw new RuntimeException(ex);
                        }
                    }
                    double[] arr = new double[l.size()];
                    for (int i = 0; i < arr.length; i++) {
                        arr[i] = l.get(i);
                    }
                    return arr;
                }

            case ANNUAL_MEAN:
            case MONTHLY_MEAN:
            case PERIOD_MEAN: {


                int previousMonth = -1;
                int previousYear = -1;
                int previousDay = -1;
                boolean previousValid = false;
                boolean lastRow = false;

                boolean useYear = (timeStep == DAILY) || (timeStep == MONTHLY_MEAN) || (timeStep == ANNUAL_MEAN);
                boolean useMonth = (timeStep == DAILY) || (timeStep == MONTHLY_MEAN);
                boolean useDay = (timeStep == DAILY);

                List<Double> l = new ArrayList<Double>();
                double sum = 0;
                int count = 0;


                for (String[] row : t.rows()) {
                    try {
                        Date d = fmt.parse(row[1]);
                        if ((d.equals(start) || d.after(start)) && (d.equals(end) || d.before(end))) {
                            int month = d.getMonth();
                            int year = d.getYear();
                            int day = d.getDay();
                            double data = Double.parseDouble(row[col]);



                            boolean newEntry = (previousValid && ((useYear && (year != previousYear))
                                    || (useMonth && (month != previousMonth))
                                    || (useDay && (day != previousDay))));



                            if (newEntry) {
                                l.add(sum / count);
                                sum = 0;
                                count = 0;
                            }

                            sum += data;
                            count++;

                            previousValid = true;
                            previousDay = day;
                            previousMonth = month;
                            previousYear = year;
                        }
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }

                }
                l.add(sum / count); // add the final entry which wasn't yet added
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

                for (int i = 0; i < 12; i++) {
                    arr[i] = 0; // initialize data to 0
                    count[i] = 0;
                }


                for (String[] row : t.rows()) {
                    try {
                        Date d = fmt.parse(row[1]);
                        if ((d.equals(start) || d.after(start)) && (d.equals(end) || d.before(end))) {
                            int month = d.getMonth();
                            double data = Double.parseDouble(row[col]);
                            arr[month] = arr[month] + data;
                            count[month] = count[month] + 1;
                            if (month > 11) {
                                throw new RuntimeException("Month > 11 = " + month);
                            }
                        }
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                for (int i = 0; i < 12; i++) {
                    arr[i] = arr[i] / count[i];
                }

                return arr;
                // break;
            }

            case PERIOD_MIN:
            case PERIOD_MAX: {
                double min = -1;
                double max = -1;
                boolean previousValid = false;


                for (String[] row : t.rows()) {
                    try {
                        Date d = fmt.parse(row[1]);
                        if ((d.equals(start) || d.after(start)) && (d.equals(end) || d.before(end))) {
                            double data = Double.parseDouble(row[col]);
                            if (!previousValid) {
                                min = data;
                                max = data;
                            } else if ((timeStep == PERIOD_MIN) && (data < min)) {
                                min = data;
                            } else if ((timeStep == PERIOD_MAX) && (data > max)) {
                                max = data;
                            }
                            previousValid = true;
                        }
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                double[] arr = new double[1];
                arr[0] = (timeStep == PERIOD_MIN) ? min : max;
                return arr;
                // break;
            }

            case PERIOD_MEDIAN: {
                // Put entire table into ArrayList
                List<Double> l = new ArrayList<Double>();
                for (String[] row : t.rows()) {
                    try {
                        Date d = fmt.parse(row[1]);
                        if ((d.equals(start) || d.after(start)) && (d.equals(end) || d.before(end))) {
                            l.add(new Double(row[col]));
                        }
                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                //Copy to array of double
                int lSize = l.size();
                if (lSize == 0) {
                    throw new RuntimeException("No data in file matched the specified period " + start + " to " + end);
                }
                double[] arr = new double[lSize];
                for (int i = 0; i < arr.length; i++) {
                    arr[i] = l.get(i);
                }

                l.clear();  // Don't need l anymore so clear it.

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

                // return as an array with 1 entry. 
                double[] arr2 = new double[1];
                arr2[0] = median;
                return arr2;

                //break;
            }

            case PERIOD_STANDARD_DEVIATION: {
                // kmolson TODO- is it better to read the file twice, or read
                //   it once and store the data for the 2nd pass while
                //   computing the mean?

                // Read file to get mean.
                double sum = 0;
                double sq_sum = 0;
                double data = 0;
                int count = 0;
                for (String[] row : t.rows()) {
                    try {
                        Date d = fmt.parse(row[1]);
                        if ((d.equals(start) || d.after(start)) && (d.equals(end) || d.before(end))) {
                            data = Double.parseDouble(row[col]);
                            sum += data;
                            sq_sum += (data * data);
                            count++;
                        }

                    } catch (ParseException ex) {
                        throw new RuntimeException(ex);
                    }
                }

                double mean = sum / count;
                double variance = sq_sum / count - (mean * mean);
                double standardDeviation = Math.sqrt(variance);

                double[] arr = new double[1];
                arr[0] = standardDeviation;
                return arr;
            }

            default: {
                throw new IllegalArgumentException("timeStep " + timeStep + "not supported.");
            }
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

    public static int findRowByDate(Date date, int dateColumn, CSTable table) {
        String type = table.getColumnInfo(dateColumn).get(KEY_TYPE);
        if ((type == null) || !type.equalsIgnoreCase(VAL_DATE)) {
            throw new IllegalArgumentException();
        }

        DateFormat fmt = lookupDateFormat(table, dateColumn);

        int rowNo = 0;
        for (String[] row : table.rows()) {
            try {
                Date d = fmt.parse(row[dateColumn]);
                if (d.equals(date)) {
                    return rowNo;
                }
                rowNo++;
            } catch (ParseException ex) {
                throw new RuntimeException(ex);
            }
        }
        throw new IllegalArgumentException(date.toString());
    }

    public static CSTable synthESPInput(CSTable table, Date iniStart, Date iniEnd, int fcDays, int year) {

        int dateColumn = 1;

        DateFormat hfmt = lookupDateFormat(table, dateColumn);

        // Forecast start = end of initialzation + 1 day
        Calendar fcStartCal = new GregorianCalendar();
        fcStartCal.setTime(iniEnd);
        fcStartCal.add(Calendar.DATE, 1);
        Date fcStart = fcStartCal.getTime();

        // get the initialization period
        MemoryTable t = new MemoryTable(table);
        int iniStartRow = findRowByDate(iniStart, dateColumn, t);
        int iniEndRow = findRowByDate(iniEnd, dateColumn, t);
        List<String[]> iniRows = t.getRows(iniStartRow, iniEndRow);

        // set the historical date to the forcast date, but use the
        // historical year.
        Calendar histStart = new GregorianCalendar();
        histStart.setTime(fcStart);
        histStart.set(Calendar.YEAR, year);

        // get the historical data
        int histStartRow = findRowByDate(histStart.getTime(), dateColumn, t);
        int histEndRow = histStartRow + (fcDays - 1);
        List<String[]> histRows = t.getRows(histStartRow, histEndRow);

        // create the new Table.
        MemoryTable espTable = new MemoryTable(table);
        espTable.getInfo().put(DATE_START, hfmt.format(iniStart));
        espTable.getInfo().put(KEY_FC_START, hfmt.format(fcStart));
        espTable.getInfo().put(KEY_FC_DAYS, Integer.toString(fcDays));
        espTable.getInfo().put(KEY_HIST_YEAR, Integer.toString(year));
        espTable.clearRows();
        espTable.addRows(iniRows);
        espTable.addRows(histRows);

        // historical date -> forecast date.
        Calendar fcCurrent = new GregorianCalendar();
        fcCurrent.setTime(fcStart);

        List<String[]> espRows = espTable.getRows();
        int start = iniRows.size();
        for (int i = start; i <= start + (fcDays - 1); i++) {
            espRows.get(i)[1] = hfmt.format(fcCurrent.getTime());
            fcCurrent.add(Calendar.DATE, 1);
        }
        fcCurrent.add(Calendar.DATE, -1);
        espTable.getInfo().put(DATE_END, hfmt.format(fcCurrent.getTime()));

        return espTable;
    }

    /** Get a slice of rows out of the table matching the time window
     *
     * @param table
     * @param timeCol
     * @param start
     * @param end
     * @return the first and last row that matches the time window start->end
     */
    public static int[] sliceByTime(CSTable table, int timeCol, Date start, Date end) {
        if (end.before(start)) {
            throw new IllegalArgumentException("end<start");
        }
        if (timeCol < 0) {
            throw new IllegalArgumentException("timeCol :" + timeCol);
        }
        int s = -1;
        int e = -1;
        int i = -1;
        for (String[] col : table.rows()) {
            i++;
            Date d = Conversions.convert(col[timeCol], Date.class);
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

    /** Create a r/o data tablemodel
     * 
     * @param src
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
     * @param p
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

    public static CSTable getTable(final CSProperties p, String boundName) {
        MemoryTable m = new MemoryTable();
        List<String> arr = keysByMeta(p, "bound", boundName);
        for (String a : arr) {
        }
        return m;
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
                return arr.get(column);
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
     * @param arr
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

    /** Returns a r/o table from a CSP file
     *
     * @param p
     * @param dim
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
     * @param csp
     * @param mkey
     * @param mval
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
     * @param t
     * @param columnName
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
     * Get a value as date.
     * 
     * @param p
     * @param key
     * @return a property as Date
     * @throws java.text.ParseException
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
     * @param p
     * @param key
     * @return a property value as integer.
     * @throws java.text.ParseException
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

    /** Saves a table to a file.
     * 
     * @param table the table to save
     * @param file the file to store it in (overwritten, if exists)
     * @throws IOException 
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
     * @throws java.io.IOException
     */
    public static CSProperties properties(Reader r, String name) throws IOException {
        return new CSVProperties(r, name);
    }

    /**
     * Create a CSProperty from an array of reader.
     * @param r
     * @param name
     * @return merged properties.
     * @throws java.io.IOException
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
     * @param base
     * @param overlay
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
     * Convert CSProperties into Properties
     * @param p
     * @return the Properties.
     */
    public static Properties properties(CSProperties p) {
        Properties pr = new Properties();
        pr.putAll(p);
        return pr;
    }

    /** 
     * Convert Properties to CSProperties.
     * @param p the Properties
     * @return CSProperties
     */
    public static CSProperties properties(Properties p) {
        return new BasicCSProperties(p);
    }

    /** Convert from a Map to properties.
     * 
     * @param p the source map
     * @return  CSProperties
     */
    public static CSProperties properties(Map<String, Object> p) {
        return new BasicCSProperties(p);
    }

    /** Create Empty properties
     * @return get some empty properties.
     */
    public static CSProperties properties() {
        return new BasicCSProperties();
    }

    /** Parse the first table from a file
     * 
     * @param file the file to parse
     * @return the CSTable
     * @throws IOException 
     */
    public static CSTable table(File file) throws IOException {
        return table(file, null);
    }

    /** Parse a table from a given File.
     * @param file
     * @param name
     * @return a CSTable.
     * @throws java.io.IOException
     */
    public static CSTable table(File file, String name) throws IOException {
        return new FileTable(file, name);
    }

    /** Parse a table from a Reader. Find the first table
     * 
     * @param s the Reader to read from
     * @return the CSTable
     * @throws IOException 
     */
    public static CSTable table(String s) throws IOException {
        return table(s, null);
    }

    /** Parse a table from a Reader.
     * 
     * @param s the Reader to read from
     * @param name the name of the table
     * @return the CSTable
     * @throws IOException 
     */
    public static CSTable table(String s, String name) throws IOException {
        return new StringTable(s, name);
    }

    /** Opens the first table found at the URL
     * 
     * @param url the URL
     * @return the CSTable
     * @throws IOException 
     */
    public static CSTable table(URL url) throws IOException {
        return table(url, null);
    }

    /** Create a CSTable from a URL source.
     * 
     * @param url the table URL
     * @param name the name of the table
     * @return a new CSTable
     * @throws IOException 
     */
    public static CSTable table(URL url, String name) throws IOException {
        return new URLTable(url, name);
    }

    /** Check if a column exist in table.
     * 
     * @param table the table to check
     * @param name the name of the column
     * @return 
     */
    public static boolean columnExist(CSTable table, String name) {
        for (int i = 1; i <= table.getColumnCount(); i++) {
            if (table.getColumnName(i).startsWith(name)) {
                return true;
            }
        }
        return false;
    }

    /** Gets a column index by name
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
    

    /** Get the column indexes for a given column name.
     *  (e.g. use tmin to fetch tmin[0], tmin[1]...)
     * @param table
     * @param name
     * @return 
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

    /** Extract the columns and create another table.
     * 
     * @param table the table 
     * @param colName the names of the columns to extract.
     * @return A new Table with the Columns.
     */
    public static CSTable extractColumns(CSTable table, String... colNames) {
        int[] idx = {};

        for (String name : colNames) {
            idx = add(idx, columnIndexes(table, name));
        }

        if (idx.length == 0) {
            throw new IllegalArgumentException("No such column names: " + Arrays.toString(colNames));
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

    public static CSTable asTable(CSProperties p, String dim) {
        List<String> arrays = DataIO.keysByMeta(p, "bound", dim);
        if (arrays.isEmpty()) {
            // nothing is bound to this
            return null;
        }
        int len = 0;
        List<String[]> m = new ArrayList<String[]>();
        for (String arr : arrays) {
            String[] d = Conversions.convert(p.get(arr), String[].class);
            len = d.length;
            m.add(d);
        }
        MemoryTable table = new MemoryTable();
        table.getInfo().put("info", "Parameter bound by " + dim);
        table.setName(dim);
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

    public static CSProperties fromTable(CSTable t) {
        BasicCSProperties p = new BasicCSProperties();

        Map<Integer, List<String>> table = new HashMap<Integer, List<String>>();
        for (int i = 1; i <= t.getColumnCount(); i++) {
            table.put(i, new ArrayList<String>());
        }

        for (String[] row : t.rows()) {
            for (int i = 1; i < row.length; i++) {
                table.get(i).add(row[i]);
            }
        }

        Map<String, String> m = new HashMap<String, String>();
        m.put("bound", t.getName());

        for (int i = 1; i <= t.getColumnCount(); i++) {
            String name = t.getColumnName(i);
            p.put(name, table.get(i).toString().replace('[', '{').replace(']', '}'));
            p.setInfo(name, m);
        }
        return p;
    }

    /** Find all table names in a file.
     * 
     * @param f the file to search in
     * @return a list of table names found in that file.
     */
    public static List<String> tables(File f) throws IOException {
        return findCSVElements(f, "@T");
    }

    /** Find all properties section names in a file.
     * 
     * @param f the file to search in
     * @return a list of section names found in that file.
     */
    public static List<String> properties(File f) throws IOException {
        return findCSVElements(f, "@S");
    }

    static List<String> findCSVElements(File f, String tag) throws IOException {
        List<String> l = new ArrayList<String>();
        Reader r = new FileReader(f);
        CSVParser csv = new CSVParser(r, CSVStrategy.DEFAULT_STRATEGY);
        String[] line = null;
        while ((line = csv.getLine()) != null) {
            if (line.length == 2 && line[0].equals(tag)) {
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
        throw new IllegalArgumentException("Not found : " + type + ", " + name);
    }

    @SuppressWarnings("serial")
    private static class BasicCSProperties extends LinkedHashMap<String, Object> implements CSProperties {

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
            info.put(ROOT_ANN, new HashMap<String, String>());
        }

        @Override
        public void putAll(CSProperties p) {
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
        public Map<String, String> getInfo(String property) {
            Map<String, String> im = info.get(property);
            return (im == null) ? NOINFO : im;
        }

        @Override
        public Map<String, String> getInfo() {
            return getInfo(ROOT_ANN);
        }

        @Override
        public void setInfo(String propertyname, Map<String, String> inf) {
            info.put(propertyname, inf);
        }

        @Override
        public String get(Object key) {
            Object val = super.get(key.toString());
            return resolve(val != null ? val.toString() : null);
        }

        /**
         * Resolve variable substitution.
         *
         *   @P, dir, "/tmp/input"
         *   @P, file,  "${dir}/test.txt"
         *
         * - The referenced key has to be in the same properties set.
         * - there could be a chain of references, however, no recursion
         *   testing is implemented.
         *
         * @param str
         * @return
         */
        private String resolve(String str) {
            if (str != null && str.contains("${")) {
                Matcher ma = null;
                while ((ma = varPattern.matcher(str)).find()) {
                    String key = ma.group(1);
                    String val = get(key);
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
                    put(propKey, (line.length == 3) ? line[2] : null);
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

        Map<Integer, Map<String, String>> info = new HashMap<Integer, Map<String, String>>();
        String name;
        int colCount;
        String columnNames[];
        int firstline;
        CSVStrategy strategy = CSVStrategy.DEFAULT_STRATEGY;

        protected abstract Reader newReader();

        protected void init(String tableName) throws IOException {
            CSVParser csv = new CSVParser(newReader(), strategy);
            name = locate(csv, tableName, TABLE, TABLE1);
            firstline = readTableHeader(csv);
        }

        private void skip0(CSVParser csv, int lines) {
            try {
                csv.skipLines(lines);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        private String[] readRow(CSVParser csv) {
            try {
                String[] r = csv.getLine();
                return r;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        /**
         * Gets a row iterator.
         * @return
         */
        @Override
        public Iterable<String[]> rows() {
            return rows(0);
        }

        /**
         * Gets a row iterator that starts at a give row.
         * @param startRow the row to start parsing.
         * @return
         */
        @Override
        public Iterable<String[]> rows(final int startRow) {
            if (startRow < 0) {
                throw new IllegalArgumentException("startRow<0");
            }

            return new Iterable<String[]>() {

                @Override
                public Iterator<String[]> iterator() {
                    final Reader r = newReader();
                    final CSVParser csv = new CSVParser(r, strategy);

                    skip0(csv, firstline);
                    skip0(csv, startRow);

                    return new TableIterator<String[]>() {

                        String[] line = readRow(csv);
                        int row = startRow;
                        
                        public void close() throws IOException {
                                r.close();
                        }

                        @Override
                        public boolean hasNext() {
                            boolean hn = (line != null && line.length > 1 && line[0].isEmpty());
                            if (!hn) {
                                try {
                                    r.close();
                                } catch (IOException E) {
                                }
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

        File f;

        FileTable(File f, String name) throws IOException {
            this.f = f;
            init(name);
        }

        @Override
        protected Reader newReader() {
            try {
                return new FileReader(f);
            } catch (FileNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    private static class StringTable extends CSVTable {

        String s;

        StringTable(String s, String name) throws IOException {
            this.s = s;
            init(name);
        }

        @Override
        protected Reader newReader() {
            return new StringReader(s);
        }
    }

    private static class URLTable extends CSVTable {

        URL s;

        URLTable(URL s, String name) throws IOException {
            this.s = s;
            init(name);
        }

        @Override
        protected Reader newReader() {
            try {
                return new InputStreamReader(s.openStream());
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public static void main(String[] args) throws IOException {
//        String table = "@T, nhru\n"
//                + "createdby, od\n"
//                + "date, today\n"
//                + "@H, hru_coeff, area, me\n"
//                + "type, Double, Double, Double\n"
//                + ",1.3,3.5,5.6\n"
//                + ",1.3,3.5,5.6\n"
//                + ",1.3,3.5,5.6\n"
//                + "\n";
//
//        CSTable t = table(table, "nhru");
//        print(t, new PrintWriter(System.out));
//
//        CSProperties csp = fromTable(t);
//        print(csp, new PrintWriter(System.out));
    }
}
