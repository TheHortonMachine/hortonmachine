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
package org.hortonmachine.dbs.spatialite;

import org.hortonmachine.dbs.compat.GeometryColumn;

/**
 * Class representing a geometry_columns record.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class SpatialiteGeometryColumns extends GeometryColumn{
    // COLUMN NAMES
    public static final String TABLENAME = "geometry_columns";
    public static final String F_TABLE_NAME = "f_table_name";
    public static final String F_GEOMETRY_COLUMN = "f_geometry_column";
    public static final String GEOMETRY_TYPE = "geometry_type";
    public static final String COORD_DIMENSION = "coord_dimension";
    public static final String SRID = "srid";
    public static final String SPATIAL_INDEX_ENABLED = "spatial_index_enabled";

    // virtual tables
    public static final String VIRT_TABLENAME = "virts_geometry_columns";
    public static final String VIRT_F_TABLE_NAME = "virt_name";
    public static final String VIRT_F_GEOMETRY_COLUMN = "virt_geometry";
    public static final String VIRT_GEOMETRY_TYPE = "geometry_type";
    public static final String VIRT_COORD_DIMENSION = "coord_dimension";
    public static final String VIRT_SRID = "srid";

}
