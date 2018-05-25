/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package oms3.io;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

/** Table, that can be *fully* managed in Memory.
 *
 * @author Olaf David
 */
public class MemoryTable implements CSTable, TableModel {

    String name;
    Map<Integer, Map<String, String>> info = new HashMap<Integer, Map<String, String>>();
    List<String[]> rows = new ArrayList<String[]>();
    List<String> columnNames = new ArrayList<String>();

    public MemoryTable(CSTable src, boolean skipContent) {
        name = src.getName();
        info.put(-1, new LinkedHashMap<String, String>(src.getInfo()));
        for (int i = 1; i <= src.getColumnCount(); i++) {
            columnNames.add(src.getColumnName(i));
            info.put(i, new LinkedHashMap<String, String>(src.getColumnInfo(i)));
        }
        if (skipContent) {
            return;
        }

        for (String[] row : src.rows()) {
            rows.add(row);
        }
    }

    public MemoryTable(CSTable src) {
        this(src, false);
    }

    public MemoryTable() {
        info.put(-1, new LinkedHashMap<String, String>(new HashMap<String, String>()));
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
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public String getColumnName(int column) {
        return columnNames.get(column-1);
    }

    public void setColumns(String ... columns) {
        columnNames = Arrays.asList(columns);
        for (int i = 0; i < columnNames.size(); i++) {
            info.put(i+1, new LinkedHashMap<String, String>(new HashMap<String, String>()));
        }
    }

    public Iterator<String[]> iterator() {
        return rows.iterator();
    }

    // TableModel interface implementation
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
        return true;
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return rows.get(rowIndex)[columnIndex];
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        rows.get(rowIndex)[columnIndex] = (String) aValue;
    }

    @Override
    public void addTableModelListener(TableModelListener l) {
    }

    @Override
    public void removeTableModelListener(TableModelListener l) {
    }

    //  additional methods
    public void setName(String name) {
        this.name = name;
    }

    public void addRow(Object... row) {
        if (row.length != columnNames.size()) {
            throw new IllegalArgumentException("row data != column count : " + row.length + "!=" + columnNames.size());
        }
        String[] s = new String[columnNames.size() + 1];
        s[0] = Integer.toString(rows.size());
        for (int i = 1; i < s.length; i++) {
            s[i] = row[i-1].toString();
        }
        rows.add(s);
    }

    public void addRows(List<String[]> r) {
        for (String[] s : r) {
            String[] ns = new String[s.length];
            for (int i = 0; i < ns.length; i++) {
                ns[i] = new String(s[i]);
            }
            rows.add(ns);
        }
    }

    public void clearRows() {
        rows.clear();
    }

    public List<String[]> getRows(int from, int to) {
        return rows.subList(from, to + 1);
    }

    public List<String[]> getRows() {
        return rows;
    }

    @Override
    public Map<String, String> getColumnInfo(int column) {
        return info.get(column);
    }

    @Override
    public Iterable<String[]> rows() {
        return rows;
    }

    @Override
    public Iterable<String[]> rows(int skipRow) {
        return rows.subList(skipRow, rows.size()-1);
    }

}
