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
package org.hortonmachine.dbs.compat.objects;

/**
 * Class representing a foreign key.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class ForeignKey {

    /**
     * The current table.
     */
    public String fromTable;
    /**
     * field of the current table
     */
    public String from;

    /**
     * The referenced table.
     */
    public String toTable;
    /**
     * References field in the connected table.
     */
    public String to;

    @Override
    public String toString() {
        return fromTable + " (" + from + ") --> " + toTable + " (" + to + ")";
    }
}
