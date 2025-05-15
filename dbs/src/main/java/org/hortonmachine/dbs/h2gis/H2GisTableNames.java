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

import java.util.Arrays;
import java.util.List;


/**
 * Spatialite table namse and groups.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class H2GisTableNames{
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

}
