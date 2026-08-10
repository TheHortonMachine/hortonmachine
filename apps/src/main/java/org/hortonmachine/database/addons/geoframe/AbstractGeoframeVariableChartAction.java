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
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.log.Logger;
import org.hortonmachine.gui.utils.GuiUtilities;

/**
 * Shared behaviour behind the station data and basin data chart actions: load the entity list and
 * the first entity's data off the EDT, then open {@link GeoframeVariableChartDialogBuilder}'s
 * dialog letting the user switch between entities.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
abstract class AbstractGeoframeVariableChartAction extends AbstractAction {
    private static final long serialVersionUID = 1L;

    protected final ADb db;
    private final Component parent;

    protected AbstractGeoframeVariableChartAction( String name, ADb db, Component parent ) {
        super(name);
        this.db = db;
        this.parent = parent;
    }

    /** Entities (stations or basins) that actually have rows in the data table. */
    protected abstract List<GeoframeEntityItem> loadEntities() throws Exception;

    /** Loads the chart data for a single entity id. */
    protected abstract GeoframeVariableChartData loadData( int entityId ) throws Exception;

    /** Label used in the dialog's entity picker, eg. "Station" or "Basin". */
    protected abstract String entityLabel();

    /** Title of the opened chart dialog. */
    protected abstract String dialogTitle();

    /** Message shown in the transient loading dialog. */
    protected abstract String loadingText();

    /** Message used when no entity has any data at all. */
    protected abstract String noDataMessage();

    @Override
    public void actionPerformed( ActionEvent e ) {
        JDialog loadingDialog = showLoadingDialog();

        SwingWorker<Object[], Void> worker = new SwingWorker<>(){
            @Override
            protected Object[] doInBackground() throws Exception {
                List<GeoframeEntityItem> entities = loadEntities();
                if (entities.isEmpty()) {
                    throw new IllegalStateException(noDataMessage());
                }
                GeoframeVariableChartData data = loadData(entities.get(0).id);
                return new Object[]{entities, data};
            }

            @Override
            protected void done() {
                loadingDialog.dispose();
                try {
                    Object[] result = get();
                    @SuppressWarnings("unchecked")
                    List<GeoframeEntityItem> entities = (List<GeoframeEntityItem>) result[0];
                    GeoframeVariableChartData data = (GeoframeVariableChartData) result[1];
                    JPanel dialogPanel = GeoframeVariableChartDialogBuilder.build(entityLabel(), entities, data,
                            AbstractGeoframeVariableChartAction.this::loadData);
                    GuiUtilities.openDialogWithPanel(dialogPanel, dialogTitle(), new Dimension(1100, 800), false);
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
        panel.add(new JLabel(loadingText()), BorderLayout.NORTH);
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
