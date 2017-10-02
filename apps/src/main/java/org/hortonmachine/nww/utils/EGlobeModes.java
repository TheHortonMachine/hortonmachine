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

public enum EGlobeModes {
    Earth("Earth"), FlatEarth("Flat Earth (lat/long)"), FlatEarthMercator("Flat Earth (Mercator)");

    private String description;

    private EGlobeModes(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public static EGlobeModes getModeFromDescription(String description) {
        for (EGlobeModes mode : values()) {
            if (mode.getDescription().equals(description)) {
                return mode;
            }
        }
        return EGlobeModes.Earth;
    }

    public static String[] getModesDescriptions() {
        return new String[] { Earth.description, FlatEarthMercator.description, FlatEarth.description };
    }

}
