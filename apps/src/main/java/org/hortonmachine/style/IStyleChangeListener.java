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
package org.hortonmachine.style;

/**
 * Told by the rule and symbolizer panels when their Apply button is pressed, after they wrote
 * their values into the style they edit.
 *
 * <p>Implemented by the style editor and by the style tab of the viewer, which both show the
 * same panels.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public interface IStyleChangeListener {

    /**
     * Use the edited style, as for example redrawing the layer with it.
     */
    void applyStyle();
}
