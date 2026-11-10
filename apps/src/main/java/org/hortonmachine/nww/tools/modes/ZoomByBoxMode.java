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

import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.gui.NwwPanel;
import org.hortonmachine.nww.tools.INwwMapMode;
import org.hortonmachine.nww.tools.NwwContext;
import org.hortonmachine.nww.utils.CursorUtils;
import org.hortonmachine.nww.utils.NwwUtilities;
import org.hortonmachine.nww.utils.selection.ObjectsOnScreenByBoxSelector;

import gov.nasa.worldwind.geom.Sector;

/**
 * Drag a box on the map to zoom to it.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ZoomByBoxMode implements INwwMapMode {

    private ObjectsOnScreenByBoxSelector selector;

    @Override
    public String getName() {
        return "Zoom";
    }

    @Override
    public String getDescription() {
        return "Drag a box to zoom to it";
    }

    @Override
    public Icon getIcon() {
        return ImageCache.get(ImageCache.MAP_ZOOM_IN, ImageCache.SIZE_TOOLBAR);
    }

    @Override
    public void activate( NwwContext context ) {
        NwwPanel nwwPanel = context.getNwwPanel();
        if (selector == null) {
            selector = new ObjectsOnScreenByBoxSelector(nwwPanel.getWwd());
            selector.addListener(( selectedArea, selectedObjs ) -> {
                try {
                    Sector sector = NwwUtilities
                            .envelope2Sector(new ReferencedEnvelope(selectedArea.getEnvelopeInternal(), NwwUtilities.GPS_CRS));
                    nwwPanel.goTo(sector, false);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
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
