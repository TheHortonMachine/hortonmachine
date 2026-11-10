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
 * The opacities of the symbolizer panels, which the wrappers give as text.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
final class OpacityValues {

    private OpacityValues() {
    }

    /**
     * @param opacity the opacity as text, from 0 to 1.
     * @return the opacity, 1 if missing or not a number, as SLD does.
     */
    static double parse( String opacity ) {
        if (opacity == null || opacity.isBlank()) {
            return 1;
        }
        try {
            return Double.parseDouble(opacity);
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * @param opacity the opacity, from 0 to 1.
     * @return the opacity as text for the wrappers.
     */
    static String text( double opacity ) {
        return String.valueOf((float) opacity);
    }
}
