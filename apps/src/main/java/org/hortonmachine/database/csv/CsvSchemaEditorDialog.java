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
package org.hortonmachine.database.csv;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;

import org.hortonmachine.dbs.utils.csv.CsvColumnSchema;
import org.hortonmachine.dbs.utils.csv.CsvColumnType;
import org.hortonmachine.dbs.utils.csv.CsvPrimaryKeyOption;
import org.hortonmachine.dbs.utils.csv.CsvSchemaGuesser;

/**
 * Modal dialog letting the user review and correct a {@link CsvColumnSchema}
 * guessed by {@link org.hortonmachine.dbs.utils.csv.CsvSchemaGuesser}: column
 * names are fixed, the type is a combobox (the guesser might, for example,
 * call a whole-numbers column {@code INTEGER} when the user knows later rows
 * carry decimals and wants {@code DOUBLE}), and the date pattern is editable
 * text, relevant only when the type is {@code DATE}. When creating a new
 * table (as opposed to importing into an existing one) it also offers a
 * primary key choice: none, one of the CSV's own columns, or a new
 * auto-increment column.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class CsvSchemaEditorDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private static final String PK_NONE = "None";
    private static final String PK_GENERATED = "Generated (auto-increment)";

    private boolean confirmed = false;
    private final List<CsvColumnSchema> schema;
    private final List<List<String>> previewRows;
    private final boolean showPrimaryKeyOptions;
    private JComboBox<String> primaryKeyCombo;
    private JTextField generatedColumnNameField;

    private CsvSchemaEditorDialog( Component parent, List<CsvColumnSchema> schema, List<String> previewHeader,
            List<List<String>> previewRows, boolean showPrimaryKeyOptions ) {
        super(SwingUtilities.getWindowAncestor(parent) instanceof Frame ? (Frame) SwingUtilities.getWindowAncestor(parent)
                : null, "CSV column schema", true);
        this.schema = schema;
        this.previewRows = previewRows;
        this.showPrimaryKeyOptions = showPrimaryKeyOptions;

        JPanel mainPanel = new JPanel(new BorderLayout(8, 8));
        mainPanel.setBorder(javax.swing.BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel previewPanel = new JPanel(new BorderLayout(4, 4));
        previewPanel.add(new JLabel("Preview (first " + previewRows.size() + " rows):"), BorderLayout.NORTH);
        DefaultTableModel previewModel = new DefaultTableModel(previewHeader.toArray(), 0){
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isCellEditable( int row, int column ) {
                return false;
            }
        };
        for( List<String> row : previewRows ) {
            previewModel.addRow(row.toArray());
        }
        JTable previewTable = new JTable(previewModel);
        JScrollPane previewScroll = new JScrollPane(previewTable);
        previewScroll.setPreferredSize(new Dimension(600, 120));
        previewPanel.add(previewScroll, BorderLayout.CENTER);

        JPanel schemaPanel = new JPanel(new BorderLayout(4, 4));
        schemaPanel.add(new JLabel("Column schema:"), BorderLayout.NORTH);
        JTable schemaTable = new JTable(new SchemaTableModel());
        schemaTable.getColumnModel().getColumn(1).setCellEditor(new javax.swing.DefaultCellEditor(
                new JComboBox<>(CsvColumnType.values())));
        schemaTable.getColumnModel().getColumn(2)
                .setCellRenderer(new DateColumnRenderer());
        JScrollPane schemaScroll = new JScrollPane(schemaTable);
        schemaScroll.setPreferredSize(new Dimension(600, 200));
        schemaPanel.add(schemaScroll, BorderLayout.CENTER);
        JLabel datePatternHint = new JLabel(
                "<html>Date pattern uses Java's SimpleDateFormat syntax (case-sensitive): "
                        + "y=year, M=month, d=day, H=hour(0-23), m=minute, s=second - e.g. yyyy-MM-dd HH:mm</html>");
        datePatternHint.setFont(datePatternHint.getFont().deriveFont(datePatternHint.getFont().getSize2D() - 1f));
        schemaPanel.add(datePatternHint, BorderLayout.SOUTH);

        JPanel centerPanel = new JPanel(new BorderLayout(4, 12));
        centerPanel.add(previewPanel, BorderLayout.NORTH);
        centerPanel.add(schemaPanel, BorderLayout.CENTER);

        if (showPrimaryKeyOptions) {
            centerPanel.add(buildPrimaryKeyPanel(), BorderLayout.SOUTH);
        }
        mainPanel.add(centerPanel, BorderLayout.CENTER);

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        JButton okButton = new JButton("OK");
        okButton.addActionListener(e -> {
            // commit any in-progress table cell edit (e.g. a typed date pattern the user never
            // pressed Enter/Tab on) before reading schema/primaryKey off of it
            if (schemaTable.isEditing()) {
                schemaTable.getCellEditor().stopCellEditing();
            }
            if (validateDatePatterns() && validatePrimaryKeyChoice()) {
                confirmed = true;
                setVisible(false);
            }
        });
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> setVisible(false));
        buttonsPanel.add(cancelButton);
        buttonsPanel.add(okButton);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(parent);
    }

    private JPanel buildPrimaryKeyPanel() {
        JPanel pkPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        pkPanel.add(new JLabel("Primary key:"));

        String[] items = new String[schema.size() + 2];
        items[0] = PK_NONE;
        items[1] = PK_GENERATED;
        for( int i = 0; i < schema.size(); i++ ) {
            items[i + 2] = schema.get(i).name;
        }
        primaryKeyCombo = new JComboBox<>(items);
        pkPanel.add(primaryKeyCombo);

        generatedColumnNameField = new JTextField(defaultGeneratedColumnName(), 10);
        generatedColumnNameField.setEnabled(false);
        pkPanel.add(new JLabel("Column name:"));
        pkPanel.add(generatedColumnNameField);

        primaryKeyCombo.addActionListener(
                e -> generatedColumnNameField.setEnabled(PK_GENERATED.equals(primaryKeyCombo.getSelectedItem())));
        return pkPanel;
    }

    private String defaultGeneratedColumnName() {
        String name = "id";
        outer: while( true ) {
            for( CsvColumnSchema col : schema ) {
                if (col.name.equalsIgnoreCase(name)) {
                    name = name + "_pk";
                    continue outer;
                }
            }
            return name;
        }
    }

    private boolean validatePrimaryKeyChoice() {
        if (!showPrimaryKeyOptions || !PK_GENERATED.equals(primaryKeyCombo.getSelectedItem())) {
            return true;
        }
        String name = generatedColumnNameField.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Set a name for the generated primary key column.", "Missing column name",
                    JOptionPane.WARNING_MESSAGE);
            return false;
        }
        for( CsvColumnSchema col : schema ) {
            if (col.name.equalsIgnoreCase(name)) {
                JOptionPane.showMessageDialog(this,
                        "The generated primary key column name clashes with an existing CSV column: " + name, "Name clash",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
        }
        return true;
    }

    /**
     * Validates every DATE-typed column's pattern against the preview rows already fetched from
     * the file, so a wrong pattern (most often a case mistake, e.g. {@code MM} used for minutes
     * instead of {@code mm}) is caught here rather than failing deep into the actual import.
     */
    private boolean validateDatePatterns() {
        for( int col = 0; col < schema.size(); col++ ) {
            CsvColumnSchema colSchema = schema.get(col);
            if (colSchema.type != CsvColumnType.DATE) {
                continue;
            }
            String pattern = colSchema.datePattern == null ? "" : colSchema.datePattern.trim();
            if (pattern.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Set a date pattern for column: " + colSchema.name, "Missing date pattern",
                        JOptionPane.WARNING_MESSAGE);
                return false;
            }
            for( List<String> row : previewRows ) {
                if (col >= row.size() || row.get(col).isEmpty()) {
                    continue;
                }
                String value = row.get(col);
                if (!CsvSchemaGuesser.matchesPattern(value, pattern)) {
                    JOptionPane.showMessageDialog(this,
                            "<html>Date pattern <b>" + pattern + "</b> does not match value <b>" + value + "</b> in column <b>"
                                    + colSchema.name
                                    + "</b>.<br>SimpleDateFormat is case-sensitive: y=year, M=month, d=day, "
                                    + "H=hour(0-23), m=minute, s=second.</html>",
                            "Invalid date pattern", JOptionPane.WARNING_MESSAGE);
                    return false;
                }
            }
        }
        return true;
    }

    private CsvPrimaryKeyOption buildPrimaryKeyOption() {
        if (!showPrimaryKeyOptions || primaryKeyCombo.getSelectedItem() == null) {
            return CsvPrimaryKeyOption.none();
        }
        String selected = (String) primaryKeyCombo.getSelectedItem();
        if (PK_NONE.equals(selected)) {
            return CsvPrimaryKeyOption.none();
        }
        if (PK_GENERATED.equals(selected)) {
            return CsvPrimaryKeyOption.generated(generatedColumnNameField.getText().trim());
        }
        return CsvPrimaryKeyOption.existingColumn(selected);
    }

    /** The dialog's result: the (possibly user-edited) schema and primary key choice. */
    public static class Result {
        public final List<CsvColumnSchema> schema;
        public final CsvPrimaryKeyOption primaryKey;

        Result( List<CsvColumnSchema> schema, CsvPrimaryKeyOption primaryKey ) {
            this.schema = schema;
            this.primaryKey = primaryKey;
        }
    }

    /**
     * Shows the dialog modally, without primary key options (for importing into an already
     * existing table, whose primary key is already set).
     *
     * @return the (possibly user-edited) schema, or {@code null} if the user cancelled.
     */
    public static List<CsvColumnSchema> show( Component parent, List<CsvColumnSchema> schema, List<String> previewHeader,
            List<List<String>> previewRows ) {
        Result result = show(parent, schema, previewHeader, previewRows, false);
        return result == null ? null : result.schema;
    }

    /**
     * Shows the dialog modally.
     *
     * @param showPrimaryKeyOptions whether to offer a primary key choice (only meaningful when
     *          creating a new table).
     * @return the (possibly user-edited) schema and primary key choice, or {@code null} if the
     *          user cancelled.
     */
    public static Result show( Component parent, List<CsvColumnSchema> schema, List<String> previewHeader,
            List<List<String>> previewRows, boolean showPrimaryKeyOptions ) {
        CsvSchemaEditorDialog dialog = new CsvSchemaEditorDialog(parent, schema, previewHeader, previewRows,
                showPrimaryKeyOptions);
        dialog.setVisible(true);
        CsvPrimaryKeyOption primaryKey = dialog.buildPrimaryKeyOption();
        dialog.dispose();
        return dialog.confirmed ? new Result(schema, primaryKey) : null;
    }

    /** Renders the date pattern cell as disabled-looking text when the row's type isn't DATE. */
    private class DateColumnRenderer extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;
        @Override
        public Component getTableCellRendererComponent( JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column ) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setEnabled(schema.get(row).type == CsvColumnType.DATE);
            return c;
        }
    }

    private class SchemaTableModel extends AbstractTableModel {
        private static final long serialVersionUID = 1L;
        private final String[] columnNames = {"Column", "Type", "Date pattern (if type is DATE)"};

        @Override
        public int getRowCount() {
            return schema.size();
        }

        @Override
        public int getColumnCount() {
            return columnNames.length;
        }

        @Override
        public String getColumnName( int column ) {
            return columnNames[column];
        }

        @Override
        public boolean isCellEditable( int row, int column ) {
            return column != 0;
        }

        @Override
        public Object getValueAt( int row, int column ) {
            CsvColumnSchema col = schema.get(row);
            switch( column ) {
            case 0:
                return col.name;
            case 1:
                return col.type;
            case 2:
                return col.datePattern == null ? "" : col.datePattern;
            default:
                return null;
            }
        }

        @Override
        public void setValueAt( Object value, int row, int column ) {
            CsvColumnSchema col = schema.get(row);
            if (column == 1) {
                col.type = (CsvColumnType) value;
            } else if (column == 2) {
                col.datePattern = value == null ? null : value.toString();
            }
            fireTableRowsUpdated(row, row);
        }
    }
}
