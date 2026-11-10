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
package org.hortonmachine.nww.tools.modes;

import javax.swing.Icon;
import javax.swing.JOptionPane;

import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.tools.INwwMapMode;
import org.hortonmachine.nww.tools.NwwContext;
import org.hortonmachine.nww.utils.CursorUtils;
import org.hortonmachine.nww.utils.selection.ObjectsOnScreenByBoxSelector;

/**
 * Drag a box on the map to select the objects in it.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SelectByBoxMode implements INwwMapMode {

    private ObjectsOnScreenByBoxSelector selector;

    @Override
    public String getName() {
        return "Select";
    }

    @Override
    public String getDescription() {
        return "Drag a box to select the objects in it";
    }

    @Override
    public Icon getIcon() {
        return ImageCache.get(ImageCache.SELECTION_MODE, ImageCache.SIZE_TOOLBAR);
    }

    @Override
    public void activate( NwwContext context ) {
        NwwPanel nwwPanel = context.getNwwPanel();
        if (selector == null) {
            selector = new ObjectsOnScreenByBoxSelector(nwwPanel.getWwd());
            selector.addListener(( selectedArea, selectedObjs ) -> JOptionPane.showMessageDialog(nwwPanel,
                    "Selected objects: " + selectedObjs.size() + "\nin region:\n" + selectedArea.toText()));
        }
        selector.enable();
        CursorUtils.makeCrossHair(nwwPanel.getWwd());
    }

    @Override
    public void deactivate( NwwContext context ) {
        if (selector != null) {
            selector.disable();
        }
        CursorUtils.makeDefault(context.getNwwPanel().getWwd());
    }
}
