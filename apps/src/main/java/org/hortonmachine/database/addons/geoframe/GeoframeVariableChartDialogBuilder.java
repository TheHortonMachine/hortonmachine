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
import java.awt.FlowLayout;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingWorker;

import org.hortonmachine.dbs.log.Logger;

/**
 * Assembles the GeoFrame variable chart dialog, shared by the station data and basin data chart
 * actions: an entity picker (station or basin) at the top, since the underlying data table can
 * hold rows for many entities, and below it the per-variable chart for whichever entity is
 * currently selected.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeVariableChartDialogBuilder {
    private GeoframeVariableChartDialogBuilder() {
    }

    /** Loads the chart data for a single entity (station or basin) id, off the EDT. */
    @FunctionalInterface
    public interface EntityDataLoader {
        GeoframeVariableChartData load( int entityId ) throws Exception;
    }

    public static JPanel build( String entityLabel, List<GeoframeEntityItem> entities, GeoframeVariableChartData initialData,
            EntityDataLoader loader ) {
        // CardLayout lets an entity switch atomically swap the visible chart panel (show()) instead
        // of removeAll()+add(), which can otherwise expose an empty container for a frame
        CardLayout cardLayout = new CardLayout();
        JPanel chartHolder = new JPanel(cardLayout);
        JPanel initialChartPanel = GeoframeVariableChartPanelBuilder.build(initialData);
        chartHolder.add(initialChartPanel, "chart-0");

        JComboBox<GeoframeEntityItem> entityCombo = new JComboBox<>(entities.toArray(new GeoframeEntityItem[0]));
        for( GeoframeEntityItem item : entities ) {
            if (item.id == initialData.entityId) {
                entityCombo.setSelectedItem(item);
                break;
            }
        }

        JProgressBar progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setString("Loading data...");
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);

        // mutable state threaded through repeated entity selections: the panel currently shown
        // (so it can be removed once the new one is swapped in) and a growing card name counter
        JPanel[] currentChartPanelHolder = {initialChartPanel};
        int[] cardCounter = {1};

        entityCombo.addActionListener(e -> {
            GeoframeEntityItem selected = (GeoframeEntityItem) entityCombo.getSelectedItem();
            if (selected == null) {
                return;
            }
            onEntitySelected(loader, chartHolder, cardLayout, currentChartPanelHolder, cardCounter, selected.id, progressBar);
        });

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT));
        controls.add(new JLabel(entityLabel + ":"));
        controls.add(entityCombo);

        JPanel topPanel = new JPanel(new BorderLayout());
        topPanel.add(controls, BorderLayout.WEST);
        topPanel.add(progressBar, BorderLayout.SOUTH);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(topPanel, BorderLayout.NORTH);
        panel.add(chartHolder, BorderLayout.CENTER);
        return panel;
    }

    private static void onEntitySelected( EntityDataLoader loader, JPanel chartHolder, CardLayout cardLayout,
            JPanel[] currentChartPanelHolder, int[] cardCounter, int entityId, JProgressBar progressBar ) {
        progressBar.setVisible(true);

        SwingWorker<GeoframeVariableChartData, Void> worker = new SwingWorker<>(){
            @Override
            protected GeoframeVariableChartData doInBackground() throws Exception {
                return loader.load(entityId);
            }

            @Override
            protected void done() {
                try {
                    GeoframeVariableChartData data = get();
                    JPanel newChartPanel = GeoframeVariableChartPanelBuilder.build(data);
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
