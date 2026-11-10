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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.geotools.api.data.FileDataStore;
import org.geotools.api.data.FileDataStoreFinder;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.hortonmachine.dbs.geopackage.FeatureEntry;
import org.hortonmachine.dbs.geopackage.GeopackageCommonDb;
import org.hortonmachine.dbs.geopackage.hm.GeopackageDb;
import org.hortonmachine.dbs.utils.SqlName;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.gears.utils.crs.HMCrsRegistry;
import org.hortonmachine.nww.tools.INwwLayerTool;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.tools.NwwContext;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.layers.Layer;

/**
 * The panel of the vector layers read from a shapefile or a geopackage table: what the data
 * are, as their geometry type, CRS, number of features, extent and columns.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class VectorLayerTool implements INwwLayerTool {

    @Override
    public String getName() {
        return "Vector layer";
    }

    @Override
    public boolean canHandle( Layer layer ) {
        LayerSource source = LayerSource.of(layer);
        return source != null && (source.isShapefile() || source.isGeopackageTable());
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
                VectorInfo info = source.isGeopackageTable() ? readGeopackageTable(source) : readShapefile(source.file());
                SwingUtilities.invokeLater(() -> {
                    panel.removeAll();
                    panel.add(infoPanel(info.properties()), BorderLayout.NORTH);
                    panel.add(columnsPanel(info), BorderLayout.CENTER);
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
     * What is shown of a vector: the properties, by label, and the columns.
     */
    private record VectorInfo( Map<String, String> properties, List<String[]> columns, boolean withPrimaryKey ) {
    }

    private static VectorInfo readShapefile( File file ) throws Exception {
        Map<String, String> properties = new LinkedHashMap<>();
        List<String[]> columns = new ArrayList<>();
        FileDataStore store = FileDataStoreFinder.getDataStore(file);
        try {
            SimpleFeatureSource featureSource = store.getFeatureSource();
            SimpleFeatureType schema = featureSource.getSchema();
            properties.put("File", file.getAbsolutePath());
            GeometryDescriptor geometry = schema.getGeometryDescriptor();
            properties.put("Geometry", geometry == null ? "none" : geometry.getType().getBinding().getSimpleName());
            properties.put("CRS", crsText(schema.getCoordinateReferenceSystem()));
            int count = featureSource.getCount(Query.ALL);
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
        return new VectorInfo(properties, columns, false);
    }

    private static VectorInfo readGeopackageTable( LayerSource source ) throws Exception {
        Map<String, String> properties = new LinkedHashMap<>();
        List<String[]> columns = new ArrayList<>();
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
            properties.put("Features", String.format(Locale.ROOT, "%,d", db.getCount(table)));
            Envelope bounds = entry != null && entry.getBounds() != null ? entry.getBounds() : db.getTableBounds(table);
            properties.put("Extent", envelopeText(bounds));
            properties.put("Spatial index", db.hasSpatialIndex(table) ? "yes" : "no");
            for( String[] column : db.getTableColumns(table) ) {
                boolean primaryKey = column.length > 2 && "1".equals(column[2]);
                columns.add(new String[]{column[0], column[1], primaryKey ? "yes" : ""});
            }
        }
        return new VectorInfo(properties, columns, true);
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

    private static JComponent columnsPanel( VectorInfo info ) {
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
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createTitledBorder("Columns (" + info.columns().size() + ")"));
        return scroll;
    }
}
