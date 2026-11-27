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
package org.hortonmachine.database.addons.geospace;

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
 * Action that shows the geometry and the parameters of a WHETGEO 1D column,
 * from an input ({@code grid_geometry}) or an output
 * ({@code geoframe_whetgeo1d_output_grid}) gpkg.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GeospaceGridAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final ADb db;
    private final Component parent;
    private final boolean fromInput;

    /**
     * @param fromInput true to read the input tables, false for the output grid.
     */
    public GeospaceGridAction( ADb db, Component parent, boolean fromInput ) {
        super("Explain WHETGEO 1D grid");
        this.db = db;
        this.parent = parent;
        this.fromInput = fromInput;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        SwingWorker<GeospaceGridData, Void> worker = new SwingWorker<>(){
            @Override
            protected GeospaceGridData doInBackground() throws Exception {
                return fromInput ? GeospaceGridDataLoader.loadInput(db) : GeospaceGridDataLoader.loadOutput(db);
            }

            @Override
            protected void done() {
                try {
                    JPanel panel = GeospaceGridPanelBuilder.build(get());
                    GuiUtilities.openDialogWithPanel(panel, "WHETGEO 1D Grid", new Dimension(1200, 850), false);
                } catch (Exception ex) {
                    Logger.INSTANCE.insertError("", "ERROR", ex);
                    GuiUtilities.showErrorMessage(parent, ex.getMessage());
                }
            }
        };
        worker.execute();
    }
}
