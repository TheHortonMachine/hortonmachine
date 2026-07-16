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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import org.hortonmachine.dbs.compat.ADb;
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
        SwingWorker<GeoframeChartData, Void> worker = new SwingWorker<>(){
            @Override
            protected GeoframeChartData doInBackground() throws Exception {
                return GeoframeChartDataLoader.load(db, simDischargeTableName);
            }

            @Override
            protected void done() {
                try {
                    GeoframeChartData data = get();
                    JPanel chartPanel = GeoframeChartPanelBuilder.build(data, simDischargeTableName);
                    GuiUtilities.openDialogWithPanel(chartPanel, "ERM Simulation Chart", new Dimension(1200, 800), false);
                } catch (Exception ex) {
                    Logger.INSTANCE.insertError("", "ERROR", ex);
                    GuiUtilities.showErrorMessage(parent, ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
