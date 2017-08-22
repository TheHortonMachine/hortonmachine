/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.dbs.compat;

/**
 * Different table types.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public enum ETableType {
    OTHER, TABLE, VIEW, EXTERNAL;

    public static ETableType fromType( String typeStr ) {
        if (typeStr.equalsIgnoreCase("table")) {
            return ETableType.TABLE;
        } else if (typeStr.equalsIgnoreCase("view")) {
            return ETableType.VIEW;
        } else if (typeStr.equalsIgnoreCase("external")) {
            return ETableType.EXTERNAL;
        }
        return ETableType.OTHER;
    }
}
