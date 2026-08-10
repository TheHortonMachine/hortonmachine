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
import java.awt.CardLayout;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSplitPane;
import javax.swing.SwingWorker;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.log.Logger;
import org.jfree.data.Range;

/**
 * Assembles the GeoFrame simulation chart dialog: the water budget chart on the left and, when
 * basin geometries are available, an interactive basins map on the right that lets the user
 * switch which basin the chart is showing, both sides resizable via a shared split pane. A thin
 * progress bar at the top of the dialog shows while a basin switch is being loaded.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeChartDialogBuilder {
    private GeoframeChartDialogBuilder() {
    }

    public static JPanel build( ADb db, String simDischargeTableName, GeoframeChartData initialData,
            SimpleFeatureCollection basins, SimpleFeatureCollection network, SimpleFeatureCollection streamGauges,
            SimpleFeatureCollection meteoStations ) {
        // CardLayout lets a basin switch atomically swap the visible chart panel (show()) instead
        // of removeAll()+add(), which can otherwise expose an empty container for a frame
        CardLayout cardLayout = new CardLayout();
        JPanel chartHolder = new JPanel(cardLayout);
        JPanel initialChartPanel = GeoframeChartPanelBuilder.build(initialData, simDischargeTableName);
        chartHolder.add(initialChartPanel, "chart-0");

        if (basins == null) {
            return chartHolder;
        }

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setString("Loading basin data...");
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        // mutable state threaded through repeated basin selections: the panel currently shown
        // (to read back its zoomed time range before rebuilding) and a growing card name counter
        JPanel[] currentChartPanelHolder = {initialChartPanel};
        int[] cardCounter = {1};

        // the map panel needs to reference itself (to update the highlight) from within the
        // selection callback it is constructed with, hence the one-element holder trick
        GeoframeBasinsMapPanel[] mapPanelHolder = new GeoframeBasinsMapPanel[1];
        GeoframeBasinsMapPanel mapPanel = new GeoframeBasinsMapPanel(basins, network, streamGauges, meteoStations,
                initialData.basinId, basinId -> {
                    mapPanelHolder[0].setSelectedBasin(basinId);
                    onBasinSelected(db, simDischargeTableName, initialData, chartHolder, cardLayout, currentChartPanelHolder,
                            cardCounter, basinId, progressBar);
                });
        mapPanelHolder[0] = mapPanel;

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, chartHolder, mapPanel);
        splitPane.setResizeWeight(0.5);
        splitPane.setContinuousLayout(true);
        splitPane.setDividerLocation(0.5);
        splitPane.setOneTouchExpandable(true);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(progressBar, BorderLayout.NORTH);
        panel.add(splitPane, BorderLayout.CENTER);
        return panel;
    }

    private static void onBasinSelected( ADb db, String simDischargeTableName, GeoframeChartData referenceData,
            JPanel chartHolder, CardLayout cardLayout, JPanel[] currentChartPanelHolder, int[] cardCounter, int basinId,
            JProgressBar progressBar ) {
        Range currentDomainRange = GeoframeChartPanelBuilder.getCurrentDomainRange(currentChartPanelHolder[0]);
        progressBar.setVisible(true);

        SwingWorker<GeoframeChartData, Void> worker = new SwingWorker<>(){
            @Override
            protected GeoframeChartData doInBackground() throws Exception {
                return GeoframeChartDataLoader.loadForBasin(db, simDischargeTableName, basinId, referenceData);
            }

            @Override
            protected void done() {
                try {
                    GeoframeChartData data = get();
                    JPanel newChartPanel = GeoframeChartPanelBuilder.build(data, simDischargeTableName, currentDomainRange);
                    String newCardName = "chart-" + cardCounter[0]++;
                    chartHolder.add(newChartPanel, newCardName);
                    cardLayout.show(chartHolder, newCardName);

                    JPanel previousChartPanel = currentChartPanelHolder[0];
                    currentChartPanelHolder[0] = newChartPanel;
                    chartHolder.remove(previousChartPanel);
                } catch (Exception ex) {
                    Logger.INSTANCE.insertError("", "ERROR", ex);
                } finally {
                    progressBar.setVisible(false);
                }
            }
        };
        worker.execute();
    }
}
