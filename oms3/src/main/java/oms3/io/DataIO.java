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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.lang.reflect.Array;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.NumberFormat;
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

    //
    public static double[] getColumnDoubleValuesInterval(Date start, Date end, CSTable t, String columnName) {

        int col = findColumnByName(t, columnName);
        if (col == -1) {
            throw new IllegalArgumentException("No such column: " + columnName);
        }
        DateFormat fmt = lookupDateFormat(t, 1);
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
                // System.out.println("Date : " + row[dateColumn]);
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
//                System.out.println(s);
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
//                System.out.println(s);
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

//                String colname = arr.get(columnIndex);
//                String[] d = Conversions.convert(p.get(colname), String[].class);
//                return d[rowIndex].trim();
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return true;
            }

            @Override
            public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
//                String colname = arr.get(columnIndex);
//                String[] d = Conversions.convert(p.get(colname), String[].class);
//                d[rowIndex] = aValue.toString().trim();
//                String s = toArrayString(d);
//                System.out.println(s);
//                p.put(colname, s);
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
        StringBuffer b = new StringBuffer();
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
        StringBuffer b = new StringBuffer();
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
        if (dims.size() == 0) {
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

    public static int findColumnByName(CSTable t, String columnName) {
        int col = -1;
        for (int i = 1; i <= t.getColumnCount(); i++) {
            if (t.getColumnName(i).equals(columnName)) {
                col = i;
                break;
            }
        }
        return col;
    }

    public static Date[] getColumnDateValues(CSTable t, String columnName) {
        int col = findColumnByName(t, columnName);
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
        int col = findColumnByName(t, columnName);
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
     * Get a value as int.
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
     * @param out the outputwriter to print to.
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

    public static void print(CSTable table, File f) throws IOException {
        PrintWriter w = new PrintWriter(f);
        print(table, w);
        w.close();
    }

    /**
     * Print a CSTable
     * 
     * @param table
     * @param out
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
        for (int i = 0; i < table.getColumnCount(); i++) {
            out.print("," + table.getColumnName(i));
        }
        out.println();
        Map<String, String> m = table.getColumnInfo(1);
        for (String key : m.keySet()) {
            out.print(key);
            for (int i = 0; i < table.getColumnCount(); i++) {
                out.print("," + table.getColumnInfo(i + 1).get(key));
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
     * @return mergesd properties.
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
     * Convert Properties to CSProperties
     * @param p
     * @return CVSProperties
     */
    public static CSProperties properties(Properties p) {
        return new BasicCSProperties(p);
    }

    public static CSProperties properties(Map<String, Object> p) {
        return new BasicCSProperties(p);
    }

    /**
     * Create Empty properties
     * @return get some empty properties.
     */
    public static CSProperties properties() {
        return new BasicCSProperties();
    }

    /**
     * Parse a table from a fiven File
     * @param r
     * @param name
     * @return a CSTable.
     * @throws java.io.IOException
     */
    public static CSTable table(File r, String name) throws IOException {
        return new CSVTable(r, name);
    }

//////////////////////
// private
//////////////////////   
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
     * Note: to keep the order of properties, it is subclassed from
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

        void readProps(CSVParser csv) throws IOException {
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

    private static String locate(CSVParser r, String name, String... type) throws IOException {
        if (name == null) {
            // match anything
            name = ".+";
        }
        Pattern p = Pattern.compile(name);
        String[] line = null;
        while ((line = r.getLine()) != null) {
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

    public static void dispose(Iterator i) {
        if (i instanceof TableIterator) {
            ((TableIterator) i).close();
        }
    }

    private static class CSVTable implements CSTable {

        Map<Integer, Map<String, String>> info;
        String name;
        int colCount;
        int firstLine;
        String columnNames[];
        File file;

        private CSVTable(File file, String name) throws IOException {
            Reader r = new FileReader(file);
            CSVParser csv = new CSVParser(r, CSVStrategy.DEFAULT_STRATEGY);
            this.name = locate(csv, name, TABLE, TABLE1);
            this.info = new HashMap<Integer, Map<String, String>>();
            this.file = file;
            this.firstLine = readTableHeader(csv);
            if (firstLine < 2) {
                throw new IllegalArgumentException("invalid First row line: " + firstLine);
            }
            r.close();
        }

        /**
         * Gets a row iterator.
         * @return
         */
        @Override
        public Iterable<String[]> rows() {
            return rows(0);
        }

        private static void skip0(BufferedReader r, int lines) throws IOException {
            while (lines-- > 0) {
                r.readLine();
            }
        }

        private static String[] readRow(BufferedReader r, int cols) throws IOException {
            String line = r.readLine();
            if (line == null || line.isEmpty() || line.indexOf(',') == -1) {
                return null;
            }

            String[] t = new String[cols];
            int start = 0;
            for (int i = 0; i < cols; i++) {
                int end = line.indexOf(',', start);
                t[i] = line.substring(start, end != -1 ? end : line.length()).trim();
                start = end + 1;
            }
            return t;
        }
        static final int BUFF_16K = 2 * 8192;
        static final int BUFF_32K = 2 * BUFF_16K;

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
                    try {
                        final BufferedReader p = new BufferedReader(new FileReader(file), BUFF_32K);
                        skip0(p, firstLine);
                        skip0(p, startRow);

                        return new TableIterator<String[]>() {

                            String[] line = readLine();
                            int row = startRow;

                            private String[] readLine() {
                                try {
                                    String[] l = readRow(p, columnNames.length);
                                    if (l == null) {
                                        close();
                                    }
                                    return l;
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                            }

                            @Override
                            public boolean hasNext() {
                                return line != null && line.length > 1 && line[0].isEmpty();
                            }

                            @Override
                            public String[] next() {
                                String[] s = line;
                                s[0] = Integer.toString(++row);
                                line = readLine();
                                return s;
                            }

                            @Override
                            public void remove() {
                                throw new UnsupportedOperationException();
                            }

                            @Override
                            public void skip(int n) {
                                if (n < 1) {
                                    throw new IllegalArgumentException("n<1");
                                }
                                try {
                                    skip0(p, n - 1);
                                } catch (IOException ex) {
                                    throw new RuntimeException(ex);
                                }
                                line = readLine();
                                row += n;
                            }

                            @Override
                            public void close() {
                                try {
                                    if (p != null) {
                                        p.close();
                                    }
                                } catch (IOException E) {
                                }
                            }
                        };
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
            };

//            return new Iterable<String[]>() {
//
//                @Override
//                public Iterator<String[]> iterator() {
//                    try {
//                        final FileReader r = new FileReader(file);
//                        final CSVParser p = new CSVParser(r, CSVStrategy.DEFAULT_STRATEGY, BUF_SZ);
//                        p.skipLines(firstLine);
//                        p.skipLines(startRow);
//
//                        return new TableIterator<String[]>() {
//
//                            String[] line = readLine();
//                            int row = startRow;
//
//                            private String[] readLine() {
//                                try {
//                                    String[] l = p.getLine();
//                                    if (l == null) {
//                                        close();
//                                    }
//                                    return l;
//                                } catch (IOException ex) {
//                                    throw new RuntimeException(ex);
//                                }
//                            }
//
//                            @Override
//                            public boolean hasNext() {
//                                return line != null && line.length > 1 && line[0].isEmpty();
//                            }
//
//                            @Override
//                            public String[] next() {
//                                String[] s = line;
//                                s[0] = Integer.toString(++row);
//                                line = readLine();
//                                return s;
//                            }
//
//                            @Override
//                            public void remove() {
//                                throw new UnsupportedOperationException();
//                            }
//
//                            @Override
//                            public void skip(int n) {
//                                if (n < 1) {
//                                    throw new IllegalArgumentException("n<1");
//                                }
//                                try {
//                                    p.skipLines(n - 1);
//                                } catch (IOException ex) {
//                                    throw new RuntimeException(ex);
//                                }
//                                line = readLine();
//                                row += n;
//                            }
//
//                            @Override
//                            public void close() {
//                                try {
//                                    if (r != null) {
//                                        r.close();
//                                    }
//                                } catch (IOException E) {
//                                }
//                            }
//                        };
//                    } catch (IOException ex) {
//                        throw new RuntimeException(ex);
//                    }
//                }
//            };
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
}
