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

import org.hortonmachine.gui.utils.ImageCache;
import org.hortonmachine.nww.tools.INwwMapMode;
import org.hortonmachine.nww.tools.NwwContext;
import org.hortonmachine.nww.utils.CursorUtils;

/**
 * The default mode: the map is only navigated, with the WorldWind mouse controls.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PanMode implements INwwMapMode {

    @Override
    public String getName() {
        return "Pan";
    }

    @Override
    public String getDescription() {
        return "Navigate the map (Esc)";
    }

    @Override
    public Icon getIcon() {
        return ImageCache.get(ImageCache.MAP_PAN, ImageCache.SIZE_TOOLBAR);
    }

    @Override
    public void activate( NwwContext context ) {
        CursorUtils.makeDefault(context.getNwwPanel().getWwd());
    }

    @Override
    public void deactivate( NwwContext context ) {
        // nothing added
    }
}
