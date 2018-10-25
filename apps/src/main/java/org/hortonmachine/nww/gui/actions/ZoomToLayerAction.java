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

import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.layers.defaults.NwwLayer;

import org.locationtech.jts.geom.Coordinate;

/**
 * Zoom to layer action.
 * 
 * @author Antonello Andrea (www.hydrologis.com)
 *
 */
public class ZoomToLayerAction extends AbstractAction {

    protected NwwPanel wwdPanel;
    protected NwwLayer layer;
    protected boolean selected;

    public ZoomToLayerAction(NwwPanel wwdPanel, NwwLayer layer) {
        super("", new ImageIcon(ZoomToLayerAction.class.getResource("/org/hortonmachine/images/zoom.gif")));
        this.wwdPanel = wwdPanel;
        this.layer = layer;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        Coordinate center = layer.getCenter();
        wwdPanel.goTo(center.x, center.y, null, null, true);
    }
}
