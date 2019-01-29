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

import java.awt.Dimension;
import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.ImageIcon;

import org.hortonmachine.gui.utils.GuiUtilities;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.gui.style.StylePanelController;
import org.hortonmachine.nww.layers.defaults.NwwVectorLayer;

/**
 * Style layer action.
 * 
 * @author Antonello Andrea (www.hydrologis.com)
 *
 */
public class StyleVectorLayerAction extends AbstractAction {

    protected NwwPanel wwdPanel;
    protected NwwVectorLayer layer;
    protected boolean selected;

    public StyleVectorLayerAction( NwwPanel wwdPanel, NwwVectorLayer layer ) {
        super("", new ImageIcon(StyleVectorLayerAction.class.getResource("/org/hortonmachine/images/palette.png")));
        this.wwdPanel = wwdPanel;
        this.layer = layer;
    }

    public void actionPerformed( ActionEvent actionEvent ) {

        GuiUtilities.openDialogWithPanel(new StylePanelController(layer), "Select the Style", new Dimension(400, 400), false);

    }
}
