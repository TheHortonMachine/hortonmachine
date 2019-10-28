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
package org.hortonmachine.dbs.h2gis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

import org.hortonmachine.dbs.compat.ISpatialTableNames;

/**
 * Spatialite table namse and groups.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class H2GisTableNames implements ISpatialTableNames {
    public static final String startsWithIndexTables = "idx_";
    public static final List<String> spatialindexTables = Arrays.asList("spatialindex" // SpatialIndex
    );

    // STYLE
    public static final String startsWithStyleTables = "SE_";

    // METADATA
    public static final List<String> metadataTables = Arrays.asList("geometry_columns", //
            "spatial_ref_sys" //
    );

    // INTERNAL DATA
    public static final List<String> internalDataTables = Arrays.asList(//
    );

    /**
     * Sorts all supplied table names by type.
     * 
     * <p>
     * Supported types are:
     * <ul>
     * <li>{@value ISpatialTableNames#INTERNALDATA} </li>
     * <li>{@value ISpatialTableNames#METADATA} </li>
     * <li>{@value ISpatialTableNames#SPATIALINDEX} </li>
     * <li>{@value ISpatialTableNames#STYLE} </li>
     * <li>{@value ISpatialTableNames#USERDATA} </li>
     * <li></li>
     * <li></li>
     * <li></li>
     * </ul>
     * 
     * @param allTableNames list of all tables.
     * @param doSort if <code>true</code>, table names are alphabetically sorted.
     * @return the {@link LinkedHashMap}.
     */
    public static LinkedHashMap<String, List<String>> getTablesSorted( List<String> allTableNames, boolean doSort ) {
        LinkedHashMap<String, List<String>> tablesMap = new LinkedHashMap<>();
        tablesMap.put(USERDATA, new ArrayList<String>());
        tablesMap.put(SYSTEM, new ArrayList<String>());

        for( String tableName : allTableNames ) {
            tableName = tableName.toLowerCase();
            if (spatialindexTables.contains(tableName) || tableName.startsWith(startsWithIndexTables)
                    || tableName.startsWith(startsWithStyleTables) || metadataTables.contains(tableName)
                    || internalDataTables.contains(tableName)) {
                List<String> list = tablesMap.get(SYSTEM);
                list.add(tableName);
                continue;
            }
            List<String> list = tablesMap.get(USERDATA);
            list.add(tableName);
        }

        if (doSort) {
            for( List<String> values : tablesMap.values() ) {
                Collections.sort(values);
            }
        }

        return tablesMap;
    }
}
