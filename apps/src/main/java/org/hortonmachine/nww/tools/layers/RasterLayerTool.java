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
import java.awt.FlowLayout;
import java.awt.image.DataBuffer;
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
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.nww.tools.INwwLayerTool;
import org.hortonmachine.nww.tools.LayerSource;
import org.hortonmachine.nww.tools.NwwContext;
import org.locationtech.jts.geom.Envelope;

import gov.nasa.worldwind.layers.Layer;

/**
 * The panel of the raster layers read from a GeoTIFF or an ESRI ASCII grid: what the data are,
 * as their CRS, grid, resolution, bands and novalue, and on request the statistics of the bands.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class RasterLayerTool implements INwwLayerTool {

    private static final List<String> EXTENSIONS = List.of("tif", "tiff", "asc");

    @Override
    public String getName() {
        return "Raster layer";
    }

    @Override
    public boolean canHandle( Layer layer ) {
        LayerSource source = LayerSource.of(layer);
        return source != null && source.table() == null && EXTENSIONS.contains(source.extension());
    }

    @Override
    public JComponent createPanel( NwwContext context, Layer layer ) {
        File file = LayerSource.of(layer).file();
        JPanel panel = new JPanel(new BorderLayout(0, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        JLabel reading = new JLabel("Reading " + file.getAbsolutePath() + " ...");
        panel.add(reading, BorderLayout.NORTH);

        new Thread(() -> {
            try {
                Map<String, String> properties = readHeader(file);
                SwingUtilities.invokeLater(() -> {
                    panel.removeAll();
                    panel.add(VectorLayerTool.infoPanel(properties), BorderLayout.NORTH);
                    panel.add(statisticsPanel(file), BorderLayout.CENTER);
                    panel.revalidate();
                    panel.repaint();
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> reading.setText("Unable to read " + file.getAbsolutePath() + ": " + e.getMessage()));
            }
        }, "hm-raster-layer-info").start();
        return panel;
    }

    /**
     * Read what the raster is, without reading its cells: the coverage is loaded lazily.
     */
    private static Map<String, String> readHeader( File file ) throws Exception {
        Map<String, String> properties = new LinkedHashMap<>();
        GridCoverage2D coverage = OmsRasterReader.readRaster(file.getAbsolutePath());
        try {
            RegionMap region = RegionMap.fromGridGeometry(coverage.getGridGeometry());
            properties.put("File", file.getAbsolutePath());
            properties.put("Format", file.getName().toLowerCase().endsWith(".asc") ? "ESRI ASCII grid" : "GeoTIFF");
            properties.put("CRS", VectorLayerTool.crsText(coverage.getCoordinateReferenceSystem()));
            properties.put("Size", String.format(Locale.ROOT, "%,d cols x %,d rows", region.getCols(), region.getRows()));
            properties.put("Resolution", String.format(Locale.ROOT, "x %s, y %s", numberText(region.getXres()),
                    numberText(region.getYres())));
            Envelope envelope = new Envelope(region.getWest(), region.getEast(), region.getSouth(), region.getNorth());
            properties.put("Extent", VectorLayerTool.envelopeText(envelope));
            properties.put("Bands", String.valueOf(coverage.getNumSampleDimensions()));
            properties.put("Data type", dataTypeText(coverage.getRenderedImage().getSampleModel().getDataType()));
            Double novalue = CoverageUtilities.getNovalue(coverage);
            properties.put("Novalue", novalue == null ? "not set" : numberText(novalue));
        } finally {
            coverage.dispose(true);
        }
        return properties;
    }

    /**
     * The statistics of the bands, computed only on request, since they need to read all the
     * cells.
     */
    private static JComponent statisticsPanel( File file ) {
        DefaultTableModel model = new DefaultTableModel(new String[]{"Band", "Min", "Max", "Mean", "Valid cells"}, 0){
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable( int row, int column ) {
                return false;
            }
        };
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);

        JButton computeButton = new JButton("Compute statistics");
        JLabel statusLabel = new JLabel("Reads all the cells of the raster.");
        computeButton.addActionListener(e -> {
            computeButton.setEnabled(false);
            statusLabel.setText("Computing...");
            model.setRowCount(0);
            new Thread(() -> {
                try {
                    List<Object[]> rows = computeStatistics(file);
                    SwingUtilities.invokeLater(() -> {
                        for( Object[] row : rows ) {
                            model.addRow(row);
                        }
                        statusLabel.setText(" ");
                        computeButton.setEnabled(true);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        statusLabel.setText("Unable to compute the statistics: " + ex.getMessage());
                        computeButton.setEnabled(true);
                    });
                }
            }, "hm-raster-layer-statistics").start();
        });

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        buttonPanel.add(computeButton);
        buttonPanel.add(statusLabel);

        JPanel panel = new JPanel(new BorderLayout(0, 4));
        panel.setBorder(BorderFactory.createTitledBorder("Statistics"));
        panel.add(buttonPanel, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }

    private static List<Object[]> computeStatistics( File file ) throws Exception {
        GridCoverage2D coverage = OmsRasterReader.readRaster(file.getAbsolutePath());
        try {
            int bands = coverage.getNumSampleDimensions();
            List<Object[]> rows = new ArrayList<>();
            for( int band = 0; band < bands; band++ ) {
                HMRaster raster = HMRaster.fromGridCoverage(coverage, bands > 1 ? band : null);
                // [min, max, avg, sum, count], null if no valid cell
                double[] stats = raster.getStatistics();
                if (stats == null) {
                    rows.add(new Object[]{band + 1, "-", "-", "-", "0"});
                } else {
                    rows.add(new Object[]{band + 1, numberText(stats[0]), numberText(stats[1]), numberText(stats[2]),
                            String.format(Locale.ROOT, "%,d", (long) stats[4])});
                }
            }
            return rows;
        } finally {
            coverage.dispose(true);
        }
    }

    private static String dataTypeText( int dataType ) {
        switch( dataType ) {
        case DataBuffer.TYPE_BYTE:
            return "byte (8 bit)";
        case DataBuffer.TYPE_SHORT:
            return "short (16 bit)";
        case DataBuffer.TYPE_USHORT:
            return "unsigned short (16 bit)";
        case DataBuffer.TYPE_INT:
            return "int (32 bit)";
        case DataBuffer.TYPE_FLOAT:
            return "float (32 bit)";
        case DataBuffer.TYPE_DOUBLE:
            return "double (64 bit)";
        default:
            return "unknown";
        }
    }

    /**
     * @return whole numbers without decimals, the others with up to 6 decimals.
     */
    static String numberText( double value ) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            return String.valueOf(value);
        }
        if (value == Math.rint(value) && Math.abs(value) < 1e15) {
            return String.valueOf((long) value);
        }
        String text = String.format(Locale.ROOT, "%.6f", value);
        return text.replaceAll("0+$", "").replaceAll("\\.$", "");
    }
}
