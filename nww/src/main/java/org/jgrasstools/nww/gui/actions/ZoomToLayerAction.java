/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.nww.gui.actions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import com.vividsolutions.jts.geom.Coordinate;

import org.jgrasstools.nww.gui.NwwPanel;
import org.jgrasstools.nww.layers.defaults.NwwLayer;

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
        super("", new ImageIcon(ZoomToLayerAction.class.getResource("/org/jgrasstools/images/zoom.gif")));
        this.wwdPanel = wwdPanel;
        this.layer = layer;
    }

    public void actionPerformed(ActionEvent actionEvent) {
        Coordinate center = layer.getCenter();
        wwdPanel.goTo(center.x, center.y, null, true);
    }
}
