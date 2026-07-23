/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com
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
package org.hortonmachine.database.addons.geoframe;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gui.utils.GuiUtilities;

/**
 * Action that loads the precipitation, temperature and simulated/observed
 * discharge of the most downstream basin and opens them as a composed,
 * two-panel chart.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeChartAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final ADb db;
    private final String simDischargeTableName;
    private final Component parent;

    public GeoframeChartAction( ADb db, String simDischargeTableName, Component parent ) {
        super("Open ERM Simulation Chart");
        this.db = db;
        this.simDischargeTableName = simDischargeTableName;
        this.parent = parent;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        JDialog loadingDialog = showLoadingDialog();

        SwingWorker<Object[], Void> worker = new SwingWorker<>(){
            @Override
            protected Object[] doInBackground() throws Exception {
                GeoframeChartData data = GeoframeChartDataLoader.load(db, simDischargeTableName);

                SimpleFeatureCollection basins = null;
                SimpleFeatureCollection network = null;
                SimpleFeatureCollection streamGauges = null;
                SimpleFeatureCollection meteoStations = null;
                if (db instanceof ASpatialDb) {
                    ASpatialDb spatialDb = (ASpatialDb) db;
                    try {
                        basins = GeoframeChartDataLoader.loadBasinPolygons(spatialDb);
                    } catch (Exception ex) {
                        // the basins map is a nice-to-have: degrade to the chart-only dialog
                        Logger.INSTANCE.insertError("", "Unable to load basin geometries for the basins map", ex);
                    }
                    try {
                        network = GeoframeChartDataLoader.loadNetworkLines(spatialDb);
                    } catch (Exception ex) {
                        // the network overlay is a nice-to-have: degrade to basins-only map
                        Logger.INSTANCE.insertError("", "Unable to load the stream network for the basins map", ex);
                    }
                    try {
                        streamGauges = GeoframeChartDataLoader.loadStreamGaugeStations(spatialDb);
                    } catch (Exception ex) {
                        Logger.INSTANCE.insertError("", "Unable to load stream gauge stations for the basins map", ex);
                    }
                    try {
                        meteoStations = GeoframeChartDataLoader.loadMeteoStations(spatialDb);
                    } catch (Exception ex) {
                        Logger.INSTANCE.insertError("", "Unable to load meteo stations for the basins map", ex);
                    }
                }
                return new Object[]{data, basins, network, streamGauges, meteoStations};
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    Object[] result = get();
                    GeoframeChartData data = (GeoframeChartData) result[0];
                    SimpleFeatureCollection basins = (SimpleFeatureCollection) result[1];
                    SimpleFeatureCollection network = (SimpleFeatureCollection) result[2];
                    SimpleFeatureCollection streamGauges = (SimpleFeatureCollection) result[3];
                    SimpleFeatureCollection meteoStations = (SimpleFeatureCollection) result[4];
                    JPanel dialogPanel = GeoframeChartDialogBuilder.build(db, simDischargeTableName, data, basins, network,
                            streamGauges, meteoStations);
                    GuiUtilities.openDialogWithPanel(dialogPanel, "ERM Simulation Chart", new Dimension(1500, 850), false);
                } catch (Exception ex) {
                    Logger.INSTANCE.insertError("", "ERROR", ex);
                    GuiUtilities.showErrorMessage(parent, ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * A small non-modal "loading" indicator shown while the chart/map data is fetched, since
     * that happens before the real dialog (which would otherwise host the indicator) exists yet.
     */
    private JDialog showLoadingDialog() {
        Component windowParent = SwingUtilities.getWindowAncestor(parent) != null ? SwingUtilities.getWindowAncestor(parent)
                : parent;
        JDialog dialog = new JDialog((java.awt.Frame) (windowParent instanceof java.awt.Frame ? windowParent : null),
                "Loading...", false);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));
        panel.add(new JLabel("Loading GeoFrame simulation chart..."), BorderLayout.NORTH);
        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        panel.add(progressBar, BorderLayout.CENTER);
        dialog.setContentPane(panel);
        dialog.setUndecorated(false);
        dialog.pack();
        dialog.setSize(new Dimension(320, dialog.getHeight()));
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
        return dialog;
    }
}
