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
package org.hortonmachine.gears.utils.images;

/**
 * Enumeration of dimensions of the A series paper sizes, as defined by ISO 216.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 * @since 0.7.6
 *
 */
public enum EPaperFormat {
    A0Landscape(1189, 841), //
    A0Portrait(841, 1189), //
    A1Landscape(841, 594), //
    A1Portrait(594, 841), //
    A2Landscape(594, 420), //
    A2Portrait(420, 594), //
    A3Landscape(420, 297), //
    A3Portrait(297, 420), //
    A4Landscape(297, 210), //
    A4Portrait(210, 297), //
    A5Landscape(210, 148), //
    A5Portrait(148, 210); //

    private final int width;
    private final int height;

    /**
     * @param width in millimeters.
     * @param height in millimeters.
     */
    EPaperFormat( int width, int height ) {
        this.width = width;
        this.height = height;
    }

    public int width() {
        return width;
    }

    public int height() {
        return height;
    }
}
