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
package org.hortonmachine.nww.tools.layers;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.table.DefaultTableModel;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.filter.text.ecql.ECQL;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.ConnectionData;
import org.hortonmachine.dbs.compat.GeometryColumn;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.nww.gui.NwwDatabaseLoader;
import org.hortonmachine.nww.tools.INwwLayerTool;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.tools.NwwContext;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.layers.Layer;

/**
 * The panel of the vector layers read from a shapefile, a geopackage table or a table of a
 * database connection: what the data are, as their geometry type, CRS, number of features, extent
 * and columns, with a filter of the features shown.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class VectorLayerTool implements INwwLayerTool {

    @Override
    public String getName() {
        return "Vector layer";
    }

    @Override
    public boolean canHandle( Layer layer ) {
        LayerSource source = LayerSource.of(layer);
        return source != null && (source.isShapefile() || source.isGeopackageTable() || source.isDatabaseTable());
    }

    @Override
    public JComponent createPanel( NwwContext context, Layer layer ) {
        LayerSource source = LayerSource.of(layer);
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel reading = new JLabel("Reading " + source.path() + " ...");
        panel.add(reading, BorderLayout.NORTH);

        new Thread(() -> {
            try {
                VectorInfo info;
                if (source.isDatabaseTable()) {
                    info = readDatabaseTable(source);
                } else if (source.isGeopackageTable()) {
                    info = readGeopackageTable(source);
                } else {
                    info = readShapefile(source.file());
                }
                SwingUtilities.invokeLater(() -> {
                    JTextField filterField = new JTextField(source.filter() == null ? "" : source.filter());
                    JPanel northPanel = new JPanel(new BorderLayout(0, 6));
                    northPanel.add(infoPanel(info.properties()), BorderLayout.NORTH);
                    northPanel.add(filterPanel(context, layer, info.count(), filterField), BorderLayout.CENTER);
                    panel.removeAll();
                    panel.add(northPanel, BorderLayout.NORTH);
                    panel.add(columnsPanel(info, filterField), BorderLayout.CENTER);
                    panel.revalidate();
                    panel.repaint();
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> reading.setText("Unable to read " + source.path() + ": " + e.getMessage()));
            }
        }, "hm-vector-layer-info").start();
        return panel;
    }

    /**
     * What is shown of a vector: the properties, by label, the columns and the number of all
     * the features.
     */
    private record VectorInfo( Map<String, String> properties, List<String[]> columns, boolean withPrimaryKey, long count ) {
    }

    /**
     * The filter of the features shown by the layer: applying it replaces the layer by one with
     * the matching features only.
     *
     * @param layer the layer of the panel when it is created.
     * @param total the number of all the features.
     * @param filterField the field of the filter, into which the columns table inserts names.
     */
    private static JPanel filterPanel( NwwContext context, Layer layer, long total, JTextField filterField ) {
        LayerSource source = LayerSource.of(layer);

        String hint = source.isGeopackageTable() || source.isDatabaseTable()
                ? "SQL WHERE condition, e.g.  \"ID\" > 10 AND \"name\" LIKE 'A%'"
                : "ECQL condition, e.g.  \"AREA\" > 1000 AND \"NAME\" LIKE 'A%'  (quote ID, a keyword)";
        JLabel hintLabel = new JLabel(hint);
        hintLabel.setEnabled(false);
        JLabel statusLabel = new JLabel(countText(source.filter() == null ? total : -1, total));
        JButton applyButton = new JButton("Apply");
        JButton clearButton = new JButton("Clear");

        Runnable apply = () -> {
            String filter = filterField.getText().trim();
            String where = filter.isEmpty() ? null : filter;
            applyButton.setEnabled(false);
            clearButton.setEnabled(false);
            statusLabel.setForeground(UIManager.getColor("Label.foreground"));
            statusLabel.setText(where == null ? "Loading all the features..." : "Filtering...");
            // the layer is replaced by each filter applied, as well as by a new style
            Layer currentLayer = context.getLatest(layer);
            new Thread(() -> {
                try {
                    // counting first also checks the filter, before the layer is rebuilt
                    long count = where == null ? total : countFeatures(source, where);
                    Layer replacement = context.getLayerLoader().reloadVectorLayer(currentLayer, where);
                    SwingUtilities.invokeLater(() -> {
                        context.replaceLayer(currentLayer, replacement);
                        statusLabel.setText(countText(count, total));
                        applyButton.setEnabled(true);
                        clearButton.setEnabled(true);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setForeground(UIManager.getColor("Component.error.focusedBorderColor") != null
                                ? UIManager.getColor("Component.error.focusedBorderColor")
                                : Color.RED);
                        statusLabel.setText("Wrong filter: " + errorText(e));
                        applyButton.setEnabled(true);
                        clearButton.setEnabled(true);
                    });
                }
            }, "hm-vector-layer-filter").start();
        };
        applyButton.addActionListener(e -> apply.run());
        filterField.addActionListener(e -> apply.run());
        clearButton.addActionListener(e -> {
            filterField.setText("");
            if (LayerSource.of(context.getLatest(layer)).filter() != null) {
                apply.run();
            } else {
                // only an error of a filter not applied to clear
                statusLabel.setForeground(UIManager.getColor("Label.foreground"));
                statusLabel.setText(countText(total, total));
            }
        });

        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        buttonsPanel.add(applyButton);
        buttonsPanel.add(clearButton);
        JPanel fieldPanel = new JPanel(new BorderLayout(4, 0));
        fieldPanel.add(filterField, BorderLayout.CENTER);
        fieldPanel.add(buttonsPanel, BorderLayout.EAST);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Filter"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.gridx = 0;
        c.weightx = 1;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.anchor = GridBagConstraints.WEST;
        c.gridy = 0;
        panel.add(fieldPanel, c);
        c.gridy = 1;
        panel.add(hintLabel, c);
        c.gridy = 2;
        panel.add(statusLabel, c);
        return panel;
    }

    /**
     * @param count the features shown, -1 if not known.
     * @param total all the features.
     */
    private static String countText( long count, long total ) {
        if (count < 0) {
            return "Filtered";
        }
        if (count == total) {
            return String.format(Locale.ROOT, "Showing all the %,d features", total);
        }
        return String.format(Locale.ROOT, "Showing %,d of %,d features", count, total);
    }

    /**
     * @return the message of the deepest cause, which tells what is wrong in the filter.
     */
    private static String errorText( Throwable e ) {
        Throwable cause = e;
        while( cause.getCause() != null && cause.getCause() != cause ) {
            cause = cause.getCause();
        }
        String message = cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName();
        // the ECQL parser lists all the tokens it expected, too many to be of help
        int expectingIndex = message.indexOf("Was expecting");
        if (expectingIndex > 0) {
            message = message.substring(0, expectingIndex);
        }
        // the parser messages span lines
        return message.replaceAll("\\s+", " ").trim();
    }

    /**
     * Count the features that match a filter, which fails if the filter is wrong.
     */
    private static long countFeatures( LayerSource source, String where ) throws Exception {
        if (source.isDatabaseTable()) {
            // in the extent the table was loaded in, as the map view
            try (ASpatialDb db = NwwDatabaseLoader.openDb(source.connection())) {
                return NwwDatabaseLoader.countFeatures(db, SqlName.m(source.table()), source.extent(), where);
            }
        }
        if (source.isGeopackageTable()) {
            try (GeopackageCommonDb db = new GeopackageDb()) {
                db.open(source.file().getAbsolutePath());
                String sql = "select count(*) from " + SqlName.m(source.table()).fixedDoubleName + " where " + where;
                return db.execOnConnection(connection -> {
                    try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                        return rs.next() ? rs.getLong(1) : 0L;
                    }
                });
            }
        }
        FileDataStore store = FileDataStoreFinder.getDataStore(source.file());
        try {
            SimpleFeatureSource featureSource = store.getFeatureSource();
            Filter filter = ECQL.toFilter(where);
            int count = featureSource.getCount(new Query(featureSource.getSchema().getTypeName(), filter));
            return count >= 0 ? count : featureSource.getFeatures(filter).size();
        } finally {
            store.dispose();
        }
    }

    private static VectorInfo readShapefile( File file ) throws Exception {
        Map<String, String> properties = new LinkedHashMap<>();
        List<String[]> columns = new ArrayList<>();
        int count;
        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        try {
            SimpleFeatureSource featureSource = store.getFeatureSource();
            SimpleFeatureType schema = featureSource.getSchema();
            properties.put("File", file.getAbsolutePath());
            GeometryDescriptor geometry = schema.getGeometryDescriptor();
            properties.put("Geometry", geometry == null ? "none" : geometry.getType().getBinding().getSimpleName());
            properties.put("CRS", crsText(schema.getCoordinateReferenceSystem()));
            count = featureSource.getCount(Query.ALL);
            if (count < 0) {
                count = featureSource.getFeatures().size();
            }
            properties.put("Features", String.format(Locale.ROOT, "%,d", count));
            properties.put("Extent", envelopeText(featureSource.getBounds()));
            File cpgFile = new File(file.getParentFile(), FileUtilities.getNameWithoutExtention(file) + ".cpg");
            properties.put("Encoding", cpgFile.exists() ? FileUtilities.readFile(cpgFile).trim() : "not given (no .cpg file)");
            for( AttributeDescriptor descriptor : schema.getAttributeDescriptors() ) {
                if (descriptor instanceof GeometryDescriptor) {
                    continue;
                }
                columns.add(new String[]{descriptor.getLocalName(), descriptor.getType().getBinding().getSimpleName()});
            }
        } finally {
            store.dispose();
        }
        return new VectorInfo(properties, columns, false, count);
    }

    private static VectorInfo readGeopackageTable( LayerSource source ) throws Exception {
        Map<String, String> properties = new LinkedHashMap<>();
        List<String[]> columns = new ArrayList<>();
        long count;
        try (GeopackageCommonDb db = new GeopackageDb()) {
            db.open(source.file().getAbsolutePath());
            SqlName table = SqlName.m(source.table());
            FeatureEntry entry = db.feature(table);
            properties.put("File", source.file().getAbsolutePath());
            properties.put("Table", source.table());
            if (entry != null) {
                String geometry = entry.getGeometryType() == null ? "unknown" : entry.getGeometryType().name();
                properties.put("Geometry", geometry + " (column " + entry.getGeometryColumn() + ")");
                properties.put("CRS", entry.getSrid() == null ? "unknown" : sridText(entry.getSrid()));
            }
            count = db.getCount(table);
            properties.put("Features", String.format(Locale.ROOT, "%,d", count));
            Envelope bounds = entry != null && entry.getBounds() != null ? entry.getBounds() : db.getTableBounds(table);
            properties.put("Extent", envelopeText(bounds));
            properties.put("Spatial index", db.hasSpatialIndex(table) ? "yes" : "no");
            for( String[] column : db.getTableColumns(table) ) {
                boolean primaryKey = column.length > 2 && "1".equals(column[2]);
                columns.add(new String[]{column[0], column[1], primaryKey ? "yes" : ""});
            }
        }
        return new VectorInfo(properties, columns, true, count);
    }

    /**
     * The properties of a table of a database connection. For the tables loaded in an extent, as
     * the map view, the number of features is the one in the extent, the filter works in it.
     */
    private static VectorInfo readDatabaseTable( LayerSource source ) throws Exception {
        Map<String, String> properties = new LinkedHashMap<>();
        List<String[]> columns = new ArrayList<>();
        long count;
        ConnectionData connection = source.connection();
        try (ASpatialDb db = NwwDatabaseLoader.openDb(connection)) {
            SqlName table = SqlName.m(source.table());
            properties.put("Connection", connection.connectionLabel);
            properties.put("Database", db.getType().name() + ", " + connection.connectionUrl);
            properties.put("Table", source.table());
            GeometryColumn geometryColumn = db.getGeometryColumnsForTable(table);
            if (geometryColumn != null) {
                String geometry = geometryColumn.geometryType == null ? "unknown" : geometryColumn.geometryType.name();
                properties.put("Geometry", geometry + " (column " + geometryColumn.geometryColumnName + ")");
                properties.put("CRS", geometryColumn.srid > 0 ? sridText(geometryColumn.srid) : "unknown");
            }
            long tableCount = db.getCount(table);
            properties.put("Features", String.format(Locale.ROOT, "%,d", tableCount));
            if (source.extent() != null) {
                count = NwwDatabaseLoader.countFeatures(db, table, source.extent(), null);
                properties.put("Loaded", String.format(Locale.ROOT, "%,d features, in the map view of the loading", count));
                properties.put("Loaded extent", envelopeText(source.extent()));
            } else {
                count = tableCount;
            }
            Envelope bounds;
            try {
                bounds = db.getTableBounds(table);
            } catch (Exception e) {
                bounds = null;
            }
            properties.put("Extent", bounds == null ? "unknown" : envelopeText(bounds));
            if (geometryColumn != null) {
                properties.put("Spatial index", geometryColumn.isSpatialIndexEnabled == 1 ? "yes" : "no");
            }
            for( String[] column : db.getTableColumns(table) ) {
                boolean primaryKey = column.length > 2 && "1".equals(column[2]);
                columns.add(new String[]{column[0], column[1], primaryKey ? "yes" : ""});
            }
        }
        return new VectorInfo(properties, columns, true, count);
    }

    static String crsText( CoordinateReferenceSystem crs ) {
        if (crs == null) {
            return "unknown (no .prj file)";
        }
        String code = null;
        try {
            code = HMCrsRegistry.getCodeFromCrs(crs);
        } catch (Exception e) {
            // no code
        }
        return crs.getName().getCode() + (code != null ? " (" + code + ")" : "");
    }

    private static String sridText( int srid ) {
        try {
            CoordinateReferenceSystem crs = HMCrsRegistry.INSTANCE.getCrs("EPSG:" + srid, true);
            return crs.getName().getCode() + " (EPSG:" + srid + ")";
        } catch (Exception e) {
            return "EPSG:" + srid;
        }
    }

    static String envelopeText( Envelope envelope ) {
        if (envelope == null || envelope.isNull()) {
            return "empty";
        }
        return String.format(Locale.ROOT, "x %.6f to %.6f, y %.6f to %.6f", envelope.getMinX(), envelope.getMaxX(),
                envelope.getMinY(), envelope.getMaxY());
    }

    /**
     * @param properties the values by label.
     * @return the "Data" panel showing the values as read only fields.
     */
    static JPanel infoPanel( Map<String, String> properties ) {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createTitledBorder("Data"));
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(2, 4, 2, 4);
        c.gridy = 0;
        for( Map.Entry<String, String> property : properties.entrySet() ) {
            c.gridx = 0;
            c.weightx = 0;
            c.fill = GridBagConstraints.NONE;
            c.anchor = GridBagConstraints.NORTHWEST;
            panel.add(new JLabel(property.getKey()), c);
            c.gridx = 1;
            c.weightx = 1;
            c.fill = GridBagConstraints.HORIZONTAL;
            // read only fields, so that the values can be selected and copied
            JTextField value = new JTextField(property.getValue());
            value.setEditable(false);
            value.setBorder(BorderFactory.createEmptyBorder());
            value.setOpaque(false);
            value.setCaretPosition(0);
            panel.add(value, c);
            c.gridy++;
        }
        return panel;
    }

    /**
     * @param filterField the field into which a double click inserts the name of the column.
     */
    private static JComponent columnsPanel( VectorInfo info, JTextField filterField ) {
        String[] headers = info.withPrimaryKey() ? new String[]{"Column", "Type", "Primary key"} : new String[]{"Column", "Type"};
        DefaultTableModel model = new DefaultTableModel(headers, 0){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable( int row, int column ) {
                return false;
            }
        };
        for( String[] column : info.columns() ) {
            model.addRow(column);
        }
        JTable table = new JTable(model);
        table.setAutoCreateRowSorter(true);
        table.setFillsViewportHeight(true);
        table.setToolTipText("Double click a column to add its name to the filter");
        table.addMouseListener(new MouseAdapter(){
            @Override
            public void mouseClicked( MouseEvent e ) {
                int row = table.rowAtPoint(e.getPoint());
                if (e.getClickCount() != 2 || row < 0) {
                    return;
                }
                String name = (String) model.getValueAt(table.convertRowIndexToModel(row), 0);
                // always quoted, which both SQL and ECQL take: needed for the names that aren't
                // plain words and for those that are keywords, as ID in ECQL
                String text = "\"" + name.replace("\"", "\"\"") + "\"";
                filterField.replaceSelection(text);
                filterField.requestFocusInWindow();
            }
        });
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Columns (" + info.columns().size() + ")"));
        return scroll;
    }
}
