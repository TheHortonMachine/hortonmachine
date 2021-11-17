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

import org.hortonmachine.dbs.datatypes.EDataType;
import org.locationtech.jts.geom.Geometry;

/**
 * A simple table info.
 * 
 * <p>If performance is needed, this should not be used.</p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class QueryResult {
    /**
     * Index of the primary key (if available), that can be used for updates.
     */
    public int pkIndex = -1;

    /**
     * The index of the geometry, if available.
     */
    public int geometryIndex = -1;
    
    /**
     * The names of the columns of the result.
     */
    public List<String> names = new ArrayList<>();
    
    /**
     * The types of the columns of the result.
     * 
     * @see EDataType
     */
    public List<String> types = new ArrayList<>();
    
    /**
     * The records of data of each column.
     */
    public List<Object[]> data = new ArrayList<>();
    
    /**
     * The optional geometries for each record.
     */
    public List<Geometry> geometries = new ArrayList<>();
    
    /**
     * The time taken to complete the query.
     */
    public long queryTimeMillis = 0;
}
