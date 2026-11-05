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
package org.hortonmachine.gui.themes;

import com.formdev.flatlaf.FlatDarkLaf;

/**
 * The HortonMachine dark theme.
 *
 * <p>The colors are defined in the HMDarkLaf.properties file in the same package.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class HMDarkLaf extends FlatDarkLaf {
    private static final long serialVersionUID = 1L;

    public static final String NAME = "HortonMachine Dark";

    @Override
    public String getName() {
        return NAME;
    }
}
