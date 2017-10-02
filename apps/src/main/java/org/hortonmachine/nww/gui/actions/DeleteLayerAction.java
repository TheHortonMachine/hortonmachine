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
import javax.swing.ImageIcon;

import org.hortonmachine.nww.gui.LayerEventsListener;

import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.layers.Layer;
import gov.nasa.worldwind.layers.LayerList;

/**
 * Delete layer action.
 * 
 * @author Antonello Andrea (www.hydrologis.com)
 *
 */
public class DeleteLayerAction extends AbstractAction {

    protected WorldWindow wwd;
    protected Layer layer;
    protected boolean selected;
    private LayerEventsListener eventsListener;

    public DeleteLayerAction(LayerEventsListener eventsListener, WorldWindow wwd, Layer layer) {
        super("", new ImageIcon(DeleteLayerAction.class.getResource("/org/hortonmachine/images/trash.gif")));

        this.eventsListener = eventsListener;
        this.wwd = wwd;
        this.layer = layer;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        LayerList layers = wwd.getModel().getLayers();
        layers.remove(layer);
        eventsListener.onLayerRemoved(layer);
        wwd.redraw();
    }
}
