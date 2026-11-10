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
package org.hortonmachine.nww.tools;

import javax.swing.Icon;

/**
 * A way of interacting with the map with the mouse, as identifying features or selecting by
 * box, chosen in the {@link NwwMapModesBar}. A single mode is active at a time.
 *
 * <p>The mode owns the listeners and the cursor it sets on the map: it adds them when activated
 * and removes them when deactivated.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public interface INwwMapMode {

    /**
     * @return the name of the mode, shown when there is no icon.
     */
    String getName();

    /**
     * @return the tooltip of the mode.
     */
    String getDescription();

    /**
     * @return the icon of the mode, or <code>null</code>.
     */
    Icon getIcon();

    /**
     * The mode becomes the active one: add its listeners and cursor to the map.
     *
     * @param context the access to the viewer.
     */
    void activate( NwwContext context );

    /**
     * Another mode becomes the active one, or the viewer closes: remove the listeners of the mode
     * and its cursor.
     *
     * @param context the access to the viewer.
     */
    void deactivate( NwwContext context );
}
