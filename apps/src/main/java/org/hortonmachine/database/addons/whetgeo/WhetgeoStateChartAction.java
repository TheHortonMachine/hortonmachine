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
package org.hortonmachine.database.addons.whetgeo;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gears.io.geoframe.whetgeo.Whetgeo1DOutputSchema;
import org.hortonmachine.gui.utils.GuiUtilities;

/**
 * Action that loads a WHETGEO 1D run's state output (theta, and whichever
 * optional depth series are present) plus its top boundary condition forcing,
 * and opens them as a Hovmoller-style chart: one gpkg = one run = one grid,
 * so unlike the GeoFrame basin/station addon there is no entity to pick.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WhetgeoStateChartAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    private final ADb db;
    private final Component parent;

    public WhetgeoStateChartAction( ADb db, Component parent ) {
        super("Open WHETGEO 1D State Chart");
        this.db = db;
        this.parent = parent;
    }

    /**
     * Recognizes whether {@code selectedTable} is a WHETGEO 1D state output table and, if so,
     * appends a separator and a {@link WhetgeoStateChartAction} to {@code actions} - the single
     * call site a table-action provider (e.g. {@code DatabaseViewer.makeTableAction}) needs,
     * keeping the recognition logic colocated with the action itself rather than duplicated
     * inline at every call site.
     */
    public static void addIfApplicable( ADb db, TableLevel selectedTable, List<Action> actions, Component parent ) {
        if (!Whetgeo1DOutputSchema.TABLE_OUTPUT_STATE.equals(selectedTable.tableName.getName())) {
            return;
        }
        try {
            if (db.hasTable(Whetgeo1DOutputSchema.TABLE_OUTPUT_GRID)) {
                actions.add(null); // separator, same convention as DatabaseViewer.addSeparator(actions)
                actions.add(new WhetgeoStateChartAction(db, parent));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        JDialog loadingDialog = showLoadingDialog();

        SwingWorker<WhetgeoStateChartData, Void> worker = new SwingWorker<>(){
            @Override
            protected WhetgeoStateChartData doInBackground() throws Exception {
                return WhetgeoStateChartDataLoader.load(db);
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    WhetgeoStateChartData data = get();
                    JPanel dialogPanel = WhetgeoStateChartPanelBuilder.build(data, "");
                    GuiUtilities.openDialogWithPanel(dialogPanel, "WHETGEO 1D State", new Dimension(1100, 800), false);
                } catch (Exception ex) {
                    Logger.INSTANCE.insertError("", "ERROR", ex);
                    GuiUtilities.showErrorMessage(parent, ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    /**
     * A small non-modal "loading" indicator shown while the chart data is fetched, since that
     * happens before the real dialog (which would otherwise host the indicator) exists yet.
     */
    private JDialog showLoadingDialog() {
        Component windowParent = SwingUtilities.getWindowAncestor(parent) != null ? SwingUtilities.getWindowAncestor(parent)
                : parent;
        JDialog dialog = new JDialog((java.awt.Frame) (windowParent instanceof java.awt.Frame ? windowParent : null),
                "Loading...", false);
        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(javax.swing.BorderFactory.createEmptyBorder(12, 16, 12, 16));
        panel.add(new JLabel("Loading WHETGEO 1D state chart..."), BorderLayout.NORTH);
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
