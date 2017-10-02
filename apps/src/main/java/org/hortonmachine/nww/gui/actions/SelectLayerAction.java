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
package org.hortonmachine.nww.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.JCheckBox;

import org.hortonmachine.nww.gui.LayerEventsListener;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;

/**
 * Select layer action.
 * 
 * @author Antonello Andrea (www.hydrologis.com)
 *
 */
public class SelectLayerAction extends AbstractAction {

    protected WorldWindow wwd;
    protected Layer layer;
    protected boolean selected;
    private LayerEventsListener eventsListener;

    public SelectLayerAction(LayerEventsListener eventsListener, WorldWindow wwd, Layer layer, boolean selected) {
        super(layer.getName());
        this.eventsListener = eventsListener;

        this.wwd = wwd;
        this.layer = layer;
        this.selected = selected;
        this.layer.setEnabled(this.selected);
    }

    public boolean isSelected() {
        return selected;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        boolean isSelected = ((JCheckBox) actionEvent.getSource()).isSelected();
        this.layer.setEnabled(isSelected);
        eventsListener.onLayerSelected(layer, isSelected);
        wwd.redraw();
    }
}
