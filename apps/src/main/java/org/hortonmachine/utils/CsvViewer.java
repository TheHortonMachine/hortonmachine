/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
 *
 * The HortonMachine is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.hortonmachine.utils;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.RowFilter;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

import org.hortonmachine.dbs.utils.csv.CsvUtils;
import org.hortonmachine.gui.utils.GuiUtilities;

/**
 * A simple viewer for csv files: a sortable table with a text filter. The delimiter is
 * detected from the header, columns that contain only numbers are sorted as numbers.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
@SuppressWarnings("serial")
public class CsvViewer extends JPanel {
    /** Larger files are shown only up to this number of rows. */
    public static final int MAX_ROWS = 200_000;

    private final JTable table;
    private final TableRowSorter<TableModel> sorter;
    private final JLabel countLabel = new JLabel(" ");
    private final int rowsCount;
    private final boolean truncated;

    /**
     * @param csvFile the file to show.
     * @throws Exception if the file can't be read.
     */
    public CsvViewer( File csvFile ) throws Exception {
        super(new BorderLayout());
        char delimiter = CsvUtils.detectDelimiter(CsvUtils.readRawHeaderLine(csvFile));
        List<String> header = CsvUtils.readHeader(csvFile, delimiter);
        List<List<String>> rows = CsvUtils.readSampleRows(csvFile, delimiter, MAX_ROWS + 1);
        truncated = rows.size() > MAX_ROWS;
        if (truncated)
            rows = rows.subList(0, MAX_ROWS);
        rowsCount = rows.size();

        CsvTableModel model = new CsvTableModel(header, rows);
        table = new JTable(model);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setFillsViewportHeight(true);
        for( int c = 0; c < header.size(); c++ ) {
            table.getColumnModel().getColumn(c).setPreferredWidth(Math.max(80, Math.min(300, header.get(c).length() * 9)));
        }
        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JTextField filterField = new JTextField(25);
        filterField.setToolTipText("Show only the rows containing this text (regular expressions allowed)");
        filterField.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate( DocumentEvent e ) {
                filter(filterField.getText());
            }
            public void removeUpdate( DocumentEvent e ) {
                filter(filterField.getText());
            }
            public void changedUpdate( DocumentEvent e ) {
                filter(filterField.getText());
            }
        });
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        top.add(new JLabel("Filter:"));
        top.add(filterField);
        String delimiterName = delimiter == '\t' ? "tab" : "'" + delimiter + "'";
        JLabel infoLabel = new JLabel(header.size() + " columns, delimiter " + delimiterName);
        infoLabel.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 0));
        top.add(infoLabel);

        countLabel.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        add(top, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(countLabel, BorderLayout.SOUTH);
        updateCount();
    }

    private void filter( String text ) {
        if (text.isBlank()) {
            sorter.setRowFilter(null);
        } else {
            try {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            } catch (PatternSyntaxException e) {
                sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
            }
        }
        updateCount();
    }

    private void updateCount() {
        int shown = table.getRowCount();
        String text = (shown == rowsCount ? rowsCount + " rows" : shown + " of " + rowsCount + " rows");
        if (truncated)
            text += " (only the first " + MAX_ROWS + " rows of the file are loaded)";
        countLabel.setText(text);
    }

    /**
     * Open a csv file in a non modal dialog.
     *
     * @param csvFile the file to show.
     * @param title the title of the dialog, if null the file name is used.
     * @throws Exception if the file can't be read.
     */
    public static void show( File csvFile, String title ) throws Exception {
        CsvViewer viewer = new CsvViewer(csvFile);
        GuiUtilities.openDialogWithPanel(viewer, title != null ? title : csvFile.getName(), new Dimension(1000, 650), false);
    }

    /**
     * The csv rows, numeric columns converted to doubles to sort right.
     */
    private static class CsvTableModel extends AbstractTableModel {
        private final List<String> header;
        private final Object[][] data;
        private final boolean[] numeric;

        CsvTableModel( List<String> header, List<List<String>> rows ) {
            this.header = header;
            int cols = header.size();
            numeric = new boolean[cols];
            for( int c = 0; c < cols; c++ ) {
                numeric[c] = isNumericColumn(rows, c);
            }
            data = new Object[rows.size()][cols];
            for( int r = 0; r < rows.size(); r++ ) {
                List<String> row = rows.get(r);
                for( int c = 0; c < cols && c < row.size(); c++ ) {
                    String value = row.get(c);
                    data[r][c] = numeric[c] ? (value.isBlank() ? null : Double.valueOf(value.trim())) : value;
                }
            }
        }

        private static boolean isNumericColumn( List<List<String>> rows, int column ) {
            boolean hasValues = false;
            for( List<String> row : rows ) {
                if (column >= row.size() || row.get(column).isBlank())
                    continue;
                try {
                    Double.parseDouble(row.get(column).trim());
                    hasValues = true;
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return hasValues;
        }

        @Override
        public int getRowCount() {
            return data.length;
        }

        @Override
        public int getColumnCount() {
            return header.size();
        }

        @Override
        public String getColumnName( int column ) {
            return header.get(column);
        }

        @Override
        public Class< ? > getColumnClass( int column ) {
            return numeric[column] ? Double.class : String.class;
        }

        @Override
        public Object getValueAt( int row, int column ) {
            return data[row][column];
        }
    }
}
