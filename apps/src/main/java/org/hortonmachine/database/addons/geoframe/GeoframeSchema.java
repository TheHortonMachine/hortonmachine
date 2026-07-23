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
package org.hortonmachine.database.addons.geoframe;

import java.util.regex.Pattern;

/**
 * Table/column names and variable ids for the GeoFrame simulation database schema,
 * as written by the hmachine geoframe io classes (basin_data, station_data, topology,
 * station and the per-run sim&lt;timestamp&gt;_water_budget_simulation_discharge tables).
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeSchema {
    private GeoframeSchema() {
    }

    /**
     * Matches the dynamic per-simulation-run discharge table name, eg.
     * <code>sim20260710_160320_water_budget_simulation_discharge</code>.
     */
    public static final Pattern SIM_DISCHARGE_TABLE_PATTERN = Pattern
            .compile("^sim.*_water_budget_simulation_discharge$");

    public static final String TOPOLOGY_TABLE = "topology";
    public static final String TOPOLOGY_UPSTREAM_BASIN = "upstream_basin_id";
    public static final String TOPOLOGY_DOWNSTREAM_BASIN = "downstream_basin_id";

    public static final String BASIN_TABLE = "basin";
    public static final String NET_TABLE = "net";
    public static final String COL_GEOM = "the_geom";

    public static final String BASIN_DATA_TABLE = "basin_data";
    public static final String STATION_DATA_TABLE = "station_data";
    public static final String STATION_TABLE = "station";
    public static final String ENVIRONMENTAL_VARIABLES_TABLE = "environmental_variables";

    public static final String COL_TS = "ts";
    public static final String COL_BASIN_ID = "basin_id";
    public static final String COL_STATION_ID = "station_id";
    public static final String COL_VAR_ID = "var_id";
    public static final String COL_VALUE = "value";
    public static final String COL_ID = "id";
    public static final String COL_TYPE = "type";
    public static final String COL_VAR_NAME = "name";
    public static final String COL_VAR_UNIT = "unit";

    /** environmental_variables var_id for precipitation. */
    public static final int VAR_PRECIPITATION = 2;
    /** environmental_variables var_id for temperature. */
    public static final int VAR_TEMPERATURE = 4;
    /** environmental_variables var_id for discharge. */
    public static final int VAR_DISCHARGE = 5;

    /** station.type value (see hmachine's StationSchema.StationType) for a discharge gauge. */
    public static final String STATION_TYPE_STREAM_GAUGE = "STREAM_GAUGE";
    /** station.type value (see hmachine's StationSchema.StationType) for a meteo station. */
    public static final String STATION_TYPE_METEO = "METEO";

    public static boolean isSimulationDischargeTable( String tableName ) {
        return tableName != null && SIM_DISCHARGE_TABLE_PATTERN.matcher(tableName).matches();
    }
}
