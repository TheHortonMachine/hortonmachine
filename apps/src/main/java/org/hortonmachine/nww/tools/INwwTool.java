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

import javax.swing.ImageIcon;
import javax.swing.JComponent;

/**
 * A tool docked in a tab at the side of the map of the
 * {@link org.hortonmachine.nww.SimpleNwwViewer}.
 *
 * <p>Each viewer gets its own instances of the tools. The panel of a tool is created the first
 * time its tab is selected, and the tool reaches the viewer through the {@link NwwContext} it
 * gets then.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public interface INwwTool {

    /**
     * @return the name of the tool, shown in its tab.
     */
    String getName();

    /**
     * @return the icon of the tab, or <code>null</code> for none.
     */
    ImageIcon getIcon();

    /**
     * Create the panel of the tool, called once, the first time its tab is selected.
     *
     * @param context the access to the viewer.
     * @return the panel shown in the tab.
     */
    JComponent createPanel( NwwContext context );

    /**
     * Called when the tab of the tool is selected, after the panel has been created.
     */
    default void onActivated() {
    }

    /**
     * Called when another tab is selected, as for releasing the interactions with the map the
     * tool started.
     */
    default void onDeactivated() {
    }

    /**
     * Called when the viewer is closed, only if the panel had been created.
     */
    default void onClose() {
    }
}
