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
package org.hortonmachine.nww.utils;

import java.awt.Component;
import java.awt.Cursor;

/**
 * Cursortools
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class CursorUtils {

    public static void makeDefault(Object parent) {
        if (parent instanceof Component) {
            Component comp = (Component) parent;
            comp.setCursor(Cursor.getDefaultCursor());
        }
    }

    public static void makeCrossHair(Object parent) {
        if (parent instanceof Component) {
            Component comp = (Component) parent;
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        }
    }

    public static void makeHand(Object parent) {
        if (parent instanceof Component) {
            Component comp = (Component) parent;
            comp.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
    }

}
