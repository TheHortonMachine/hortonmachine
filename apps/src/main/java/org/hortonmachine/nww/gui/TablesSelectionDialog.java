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
package org.hortonmachine.nww.gui;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.RowFilter;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;

/**
 * A dialog to select the tables to load, as from a geopackage or a database, with a text field
 * that filters the tables shown, for the databases with many tables, and an optional check box
 * for an option of the loading.
 *
 * <p>The tables hidden by the filter keep their selection; the "Select all" and "Select none"
 * buttons act on the tables shown.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class TablesSelectionDialog {

    /**
     * The choice of the user.
     *
     * @param selected the selection state of each table, in the order given.
     * @param optionChecked the state of the option check box, <code>false</code> if there is none.
     */
    public record Result( boolean[] selected, boolean optionChecked ) {

        /**
         * @return <code>true</code> if at least a table is selected.
         */
        public boolean hasSelection() {
            for( boolean s : selected ) {
                if (s) {
                    return true;
                }
            }
            return false;
        }
    }

    private TablesSelectionDialog() {
    }

    /**
     * Let the user select tables.
     *
     * @param parent the parent of the dialog.
     * @param title the title of the dialog.
     * @param message the message above the tables.
     * @param names the names of the tables.
     * @param types the types of the tables, as the geometry type, one per name.
     * @param selectedAtStart if <code>true</code>, all the tables are selected at the start.
     * @param optionText the text of the option check box, <code>null</code> for none.
     * @param optionTooltip the tooltip of the option check box, or <code>null</code>.
     * @param optionAtStart the state of the option check box at the start.
     * @return the choice, or <code>null</code> if cancelled.
     */
    public static Result show( Component parent, String title, String message, List<String> names, List<String> types,
            boolean selectedAtStart, String optionText, String optionTooltip, boolean optionAtStart ) {
        return show(parent, title, message, "Table", names, "Type", types, selectedAtStart, optionText, optionTooltip,
                optionAtStart);
    }

    /**
     * Let the user select tables or layers, with the headers of the columns given, as for the
     * layers of a web service.
     *
     * @param parent the parent of the dialog.
     * @param title the title of the dialog.
     * @param message the message above the tables.
     * @param namesHeader the header of the names, which the filter is applied to.
     * @param names the names of the tables.
     * @param typesHeader the header of the second column.
     * @param types the values of the second column, one per name.
     * @param selectedAtStart if <code>true</code>, all the tables are selected at the start.
     * @param optionText the text of the option check box, <code>null</code> for none.
     * @param optionTooltip the tooltip of the option check box, or <code>null</code>.
     * @param optionAtStart the state of the option check box at the start.
     * @return the choice, or <code>null</code> if cancelled.
     */
    public static Result show( Component parent, String title, String message, String namesHeader, List<String> names,
            String typesHeader, List<String> types, boolean selectedAtStart, String optionText, String optionTooltip,
            boolean optionAtStart ) {
        TablesModel model = new TablesModel(names, types, selectedAtStart, namesHeader, typesHeader);
        JTable table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setFillsViewportHeight(true);
        table.getColumnModel().getColumn(0).setMaxWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(110);
        table.getColumnModel().getColumn(2).setMaxWidth(160);
        TableRowSorter<TableModel> sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        int height = table.getTableHeader().getPreferredSize().height + table.getRowHeight() * names.size() + 6;
        scrollPane.setPreferredSize(new Dimension(480, Math.max(120, Math.min(height, 400))));

        JTextField filterField = new JTextField(20);
        filterField.setToolTipText("Show only the tables whose name contains this text, case insensitive");
        JLabel countLabel = new JLabel();
        Runnable updateCount = () -> countLabel
                .setText(model.selectedCount() + " of " + names.size() + " selected, " + table.getRowCount() + " shown");
        filterField.getDocument().addDocumentListener(new DocumentListener(){
            @Override
            public void insertUpdate( DocumentEvent e ) {
                filter();
            }

            @Override
            public void removeUpdate( DocumentEvent e ) {
                filter();
            }

            @Override
            public void changedUpdate( DocumentEvent e ) {
                filter();
            }

            private void filter() {
                String text = filterField.getText().trim();
                sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1));
                updateCount.run();
            }
        });
        // the text field gets the focus, to type the filter right away
        filterField.addAncestorListener(new AncestorListener(){
            @Override
            public void ancestorAdded( AncestorEvent event ) {
                filterField.requestFocusInWindow();
            }

            @Override
            public void ancestorRemoved( AncestorEvent event ) {
            }

            @Override
            public void ancestorMoved( AncestorEvent event ) {
            }
        });
        model.addTableModelListener(e -> updateCount.run());
        updateCount.run();

        JPanel filterPanel = new JPanel(new BorderLayout(6, 0));
        filterPanel.add(new JLabel("Filter:"), BorderLayout.WEST);
        filterPanel.add(filterField, BorderLayout.CENTER);

        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        JLabel messageLabel = new JLabel(message);
        messageLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        filterPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        northPanel.add(messageLabel);
        northPanel.add(Box.createVerticalStrut(6));
        northPanel.add(filterPanel);

        JButton allButton = new JButton("Select all");
        allButton.setToolTipText("Select the tables shown");
        allButton.addActionListener(e -> setShownSelected(table, model, true));
        JButton noneButton = new JButton("Select none");
        noneButton.setToolTipText("Deselect the tables shown");
        noneButton.addActionListener(e -> setShownSelected(table, model, false));
        JPanel selectionButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        selectionButtons.add(allButton);
        selectionButtons.add(noneButton);
        selectionButtons.add(countLabel);

        JPanel southPanel = new JPanel();
        southPanel.setLayout(new BoxLayout(southPanel, BoxLayout.Y_AXIS));
        selectionButtons.setAlignmentX(Component.LEFT_ALIGNMENT);
        southPanel.add(selectionButtons);
        JCheckBox optionCheck = null;
        if (optionText != null) {
            optionCheck = new JCheckBox(optionText, optionAtStart);
            optionCheck.setToolTipText(optionTooltip);
            optionCheck.setAlignmentX(Component.LEFT_ALIGNMENT);
            southPanel.add(Box.createVerticalStrut(6));
            southPanel.add(optionCheck);
        }

        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        panel.add(northPanel, BorderLayout.NORTH);
        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(southPanel, BorderLayout.SOUTH);

        int result = JOptionPane.showConfirmDialog(parent, panel, title, JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        if (result != JOptionPane.OK_OPTION) {
            return null;
        }
        return new Result(model.selected.clone(), optionCheck != null && optionCheck.isSelected());
    }

    private static void setShownSelected( JTable table, TablesModel model, boolean selected ) {
        if (table.isEditing()) {
            table.getCellEditor().stopCellEditing();
        }
        for( int viewRow = 0; viewRow < table.getRowCount(); viewRow++ ) {
            model.selected[table.convertRowIndexToModel(viewRow)] = selected;
        }
        model.fireTableDataChanged();
    }

    /**
     * The tables: a check column to select them, the name and the type.
     */
    private static class TablesModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;

        private final String[] columns;
        private final List<String> names;
        private final List<String> types;
        private final boolean[] selected;

        TablesModel( List<String> names, List<String> types, boolean selectedAtStart, String namesHeader, String typesHeader ) {
            this.names = names;
            this.types = types;
            columns = new String[]{"Load", namesHeader, typesHeader};
            selected = new boolean[names.size()];
            Arrays.fill(selected, selectedAtStart);
        }

        int selectedCount() {
            int count = 0;
            for( boolean s : selected ) {
                if (s) {
                    count++;
                }
            }
            return count;
        }

        @Override
        public int getRowCount() {
            return names.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName( int column ) {
            return columns[column];
        }

        @Override
        public Class< ? > getColumnClass( int column ) {
            return column == 0 ? Boolean.class : String.class;
        }

        @Override
        public boolean isCellEditable( int row, int column ) {
            return column == 0;
        }

        @Override
        public Object getValueAt( int row, int column ) {
            switch( column ) {
            case 0:
                return selected[row];
            case 1:
                return names.get(row);
            default:
                return types.get(row);
            }
        }

        @Override
        public void setValueAt( Object value, int row, int column ) {
            if (column == 0) {
                selected[row] = Boolean.TRUE.equals(value);
                fireTableCellUpdated(row, column);
            }
        }
    }
}
