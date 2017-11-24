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

import java.util.ArrayList;
import java.util.List;

/**
 * Class representing an index.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class Index {

    /**
     * The table.
     */
    public String table;
    /**
     * The name of the index
     */
    public String name;

    /**
     * Flag of unique.
     */
    public boolean isUnique;

    /**
     * The involved columns.
     */
    public List<String> columns = new ArrayList<>();

    @Override
    public String toString() {
        return name + (isUnique ? "(unique)" : "");
    }
}
