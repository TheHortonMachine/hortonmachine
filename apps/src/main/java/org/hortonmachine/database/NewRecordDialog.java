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
package org.hortonmachine.database;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.text.JTextComponent;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.EDb;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.utils.DbsUtilities;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gui.utils.GuiUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;

/**
 * Modal form to insert a new record into a database table.
 *
 * <p>One field per column, built from {@link ADb#getTableColumns(SqlName)}. An empty field
 * is left out of the INSERT, so that the database default (or the auto-increment of the
 * primary key) applies; the text <code>NULL</code> inserts an explicit null. Values are
 * converted following the declared column type, the geometry column takes WKT in the
 * SRID of the column and binary columns can't be filled here.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class NewRecordDialog extends JDialog {
    private static final long serialVersionUID = 1L;

    private static class ColumnField {
        String name;
        String type;
        boolean isPk;
        boolean isGeometry;
        boolean isBinary;
        JTextComponent field;
    }

    private final ADb db;
    private final SqlName tableName;
    private final List<ColumnField> columnFields = new ArrayList<>();
    private GeometryColumn geometryColumn;
    private boolean inserted = false;

    /**
     * Open the form and insert the record on confirmation.
     *
     * @param parent the parent component.
     * @param db the database.
     * @param tableName the table to insert into.
     * @param prefill optional initial values by column name (case insensitive), as shown in
     *          the data viewer. Primary key values are ignored. Can be <code>null</code>.
     * @return <code>true</code> if a record was inserted.
     * @throws Exception if the table columns can't be read.
     */
    public static boolean showDialog( Component parent, ADb db, SqlName tableName, Map<String, String> prefill )
            throws Exception {
        NewRecordDialog dialog = new NewRecordDialog(parent, db, tableName, prefill);
        dialog.setVisible(true);
        return dialog.inserted;
    }

    private NewRecordDialog( Component parent, ADb db, SqlName tableName, Map<String, String> prefill ) throws Exception {
        super(parent instanceof Window ? (Window) parent : SwingUtilities.getWindowAncestor(parent),
                "New record in " + tableName.getFullname(), ModalityType.APPLICATION_MODAL);
        this.db = db;
        this.tableName = tableName;

        if (db instanceof ASpatialDb) {
            try {
                geometryColumn = ((ASpatialDb) db).getGeometryColumnsForTable(tableName);
            } catch (Exception e) {
                // not a spatial table
            }
        }

        List<String[]> tableColumns = db.getTableColumns(tableName);
        if (tableColumns == null || tableColumns.isEmpty()) {
            throw new IllegalArgumentException("No columns found for table " + tableName.getFullname());
        }

        JPanel fieldsPanel = new JPanel(new GridBagLayout());
        fieldsPanel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        int row = 0;
        for( String[] tc : tableColumns ) {
            ColumnField cf = new ColumnField();
            cf.name = tc[0];
            cf.type = tc[1] == null ? "" : tc[1];
            cf.isPk = "1".equals(tc[2]);
            cf.isGeometry = geometryColumn != null && geometryColumn.geometryColumnName.equalsIgnoreCase(cf.name);
            String lowerType = cf.type.toLowerCase(Locale.ROOT);
            cf.isBinary = !cf.isGeometry && (lowerType.contains("blob") || lowerType.equals("bytea")
                    || lowerType.contains("binary"));

            JLabel nameLabel = new JLabel(cf.name + (cf.isPk ? "  (PK)" : ""));
            if (cf.isPk) {
                nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
            }
            JLabel typeLabel = new JLabel(cf.type);
            typeLabel.setFont(typeLabel.getFont().deriveFont(typeLabel.getFont().getSize2D() - 1f));
            typeLabel.setForeground(UIManager.getColor("Label.disabledForeground"));
            JPanel labelPanel = new JPanel(new BorderLayout());
            labelPanel.setOpaque(false);
            labelPanel.add(nameLabel, BorderLayout.NORTH);
            labelPanel.add(typeLabel, BorderLayout.SOUTH);

            JComponent fieldComponent;
            if (cf.isGeometry) {
                JTextArea area = new JTextArea(3, 40);
                area.setLineWrap(true);
                area.putClientProperty("JTextField.placeholderText", "WKT, e.g. POINT (11.12 46.07)");
                cf.field = area;
                fieldComponent = new JScrollPane(area);
                typeLabel.setText(cf.type + "  WKT, SRID " + geometryColumn.srid);
            } else {
                JTextField textField = new JTextField(40);
                cf.field = textField;
                fieldComponent = textField;
                if (cf.isBinary) {
                    textField.setEnabled(false);
                    textField.putClientProperty("JTextField.placeholderText", "binary, can't be set here");
                } else if (cf.isPk) {
                    textField.putClientProperty("JTextField.placeholderText", "empty for the automatic value");
                } else if (isDateType(lowerType)) {
                    textField.putClientProperty("JTextField.placeholderText", "yyyy-MM-dd HH:mm:ss");
                }
            }

            if (prefill != null && !cf.isPk && !cf.isBinary) {
                String value = getIgnoreCase(prefill, cf.name);
                if (value != null) {
                    if (cf.isGeometry && value.startsWith("SRID=") && value.contains(";")) {
                        value = value.substring(value.indexOf(';') + 1);
                    }
                    cf.field.setText(value);
                }
            }
            columnFields.add(cf);

            GridBagConstraints lc = new GridBagConstraints();
            lc.gridx = 0;
            lc.gridy = row;
            lc.anchor = GridBagConstraints.NORTHWEST;
            lc.insets = new Insets(4, 4, 4, 12);
            fieldsPanel.add(labelPanel, lc);

            GridBagConstraints fc = new GridBagConstraints();
            fc.gridx = 1;
            fc.gridy = row;
            fc.weightx = 1;
            fc.fill = GridBagConstraints.HORIZONTAL;
            fc.anchor = GridBagConstraints.NORTHWEST;
            fc.insets = new Insets(4, 4, 4, 4);
            fieldsPanel.add(fieldComponent, fc);
            row++;
        }
        GridBagConstraints filler = new GridBagConstraints();
        filler.gridx = 0;
        filler.gridy = row;
        filler.weighty = 1;
        fieldsPanel.add(new JPanel(), filler);

        JLabel hintLabel = new JLabel("<html>Empty fields are left out of the insert, so the database default "
                + "(or the automatic primary key) applies. Type NULL for an explicit null value.</html>");
        hintLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        JScrollPane fieldsScroll = new JScrollPane(fieldsPanel);
        fieldsScroll.getVerticalScrollBar().setUnitIncrement(16);
        fieldsScroll.setBorder(BorderFactory.createEmptyBorder());

        JButton insertButton = new JButton("Insert");
        insertButton.addActionListener(e -> insert());
        JButton cancelButton = new JButton("Cancel");
        cancelButton.addActionListener(e -> dispose());
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonsPanel.add(insertButton);
        buttonsPanel.add(cancelButton);

        JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(hintLabel, BorderLayout.NORTH);
        mainPanel.add(fieldsScroll, BorderLayout.CENTER);
        mainPanel.add(buttonsPanel, BorderLayout.SOUTH);
        setContentPane(mainPanel);
        getRootPane().setDefaultButton(insertButton);

        pack();
        Dimension size = getSize();
        setSize(new Dimension(Math.max(560, Math.min(size.width, 900)), Math.min(size.height, 700)));
        setLocationRelativeTo(parent);
    }

    private void insert() {
        EDb dbType = db.getType();
        boolean isPostgres = dbType == EDb.POSTGRES || dbType == EDb.POSTGIS;

        List<String> columns = new ArrayList<>();
        List<String> placeholders = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for( ColumnField cf : columnFields ) {
            if (cf.isBinary) {
                continue;
            }
            String text = cf.field.getText().trim();
            if (text.isEmpty()) {
                continue;
            }
            String lowerType = cf.type.toLowerCase(Locale.ROOT);
            String placeholder = "?";
            Object value;
            try {
                if (text.equalsIgnoreCase("NULL")) {
                    value = null;
                } else if (cf.isGeometry) {
                    Geometry geometry = new WKTReader().read(text);
                    geometry.setSRID(geometryColumn.srid);
                    if (dbType == EDb.GEOPACKAGE) {
                        value = dbType.getGeometryParser().toSqlObject(geometry);
                    } else {
                        value = geometry.toText();
                        placeholder = "ST_GeomFromText(?, " + geometryColumn.srid + ")";
                    }
                } else if (isIntegerType(lowerType)) {
                    value = parseInteger(text);
                } else if (isRealType(lowerType)) {
                    value = Double.parseDouble(text);
                } else if (lowerType.startsWith("bool")) {
                    if (text.equals("1") || text.equalsIgnoreCase("true")) {
                        value = Boolean.TRUE;
                    } else if (text.equals("0") || text.equalsIgnoreCase("false")) {
                        value = Boolean.FALSE;
                    } else {
                        throw new IllegalArgumentException("not a boolean");
                    }
                } else {
                    value = text;
                    if (isPostgres && !isTextType(lowerType) && !lowerType.equals("user-defined")
                            && !lowerType.equals("array")) {
                        // dates, uuid, json... are converted by the database
                        placeholder = "CAST(? AS " + cf.type + ")";
                    }
                }
            } catch (Exception e) {
                GuiUtilities.showWarningMessage(this, null,
                        "Invalid value for " + cf.name + " (" + cf.type + "): " + text + "\n" + e.getMessage());
                cf.field.requestFocusInWindow();
                return;
            }
            columns.add("\"" + cf.name + "\"");
            placeholders.add(placeholder);
            values.add(value);
        }

        String sql;
        if (columns.isEmpty()) {
            sql = "INSERT INTO " + tableName.fixedDoubleName + " DEFAULT VALUES";
        } else {
            sql = "INSERT INTO " + tableName.fixedDoubleName + " (" + String.join(", ", columns) + ") VALUES ("
                    + String.join(", ", placeholders) + ")";
        }
        try {
            db.executeInsertUpdateDeletePreparedSql(sql, values.toArray());
            inserted = true;
            dispose();
        } catch (Exception e) {
            GuiUtilities.showErrorMessage(this, "Error inserting the record: " + e.getMessage());
        }
    }

    /**
     * Integers, also accepting a formatted date for columns holding epoch millis (as
     * shown by the data viewer when formatting dates).
     */
    private static Long parseInteger( String text ) throws Exception {
        try {
            return Long.parseLong(text);
        } catch (NumberFormatException e) {
            try {
                return DbsUtilities.dbDateFormatter.parse(text).getTime();
            } catch (Exception e1) {
                throw e;
            }
        }
    }

    private static boolean isIntegerType( String lowerType ) {
        return (lowerType.contains("int") && !lowerType.contains("interval")) || lowerType.contains("serial");
    }

    private static boolean isRealType( String lowerType ) {
        return lowerType.contains("real") || lowerType.contains("double") || lowerType.contains("float")
                || lowerType.contains("numeric") || lowerType.contains("decimal");
    }

    private static boolean isTextType( String lowerType ) {
        return lowerType.contains("char") || lowerType.contains("text") || lowerType.isEmpty();
    }

    private static boolean isDateType( String lowerType ) {
        return lowerType.contains("date") || lowerType.contains("time");
    }

    private static String getIgnoreCase( Map<String, String> map, String key ) {
        String value = map.get(key);
        if (value != null) {
            return value;
        }
        for( Map.Entry<String, String> entry : map.entrySet() ) {
            if (entry.getKey().equalsIgnoreCase(key)) {
                return entry.getValue();
            }
        }
        return null;
    }
}
