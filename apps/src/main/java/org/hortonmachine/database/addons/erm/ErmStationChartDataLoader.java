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
package org.hortonmachine.database.addons.erm;

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;

/**
 * Loads the data needed for the GeoFrame station data chart out of a connected
 * {@link ADb}: the list of stations that actually have rows in
 * {@link ErmSchema#STATION_DATA_TABLE}, and, for a given station, its
 * data split by environmental variable (var_id), with name/unit looked up
 * from {@link ErmSchema#ENVIRONMENTAL_VARIABLES_TABLE} when available.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ErmStationChartDataLoader {
    private ErmStationChartDataLoader() {
    }

    /** Stations (id + type) known to have at least one row in station_data. */
    public static List<ErmEntityItem> loadStationsWithData( ADb db ) throws Exception {
        String sql = "SELECT DISTINCT st." + ErmSchema.COL_ID + ", st." + ErmSchema.COL_TYPE + " FROM "
                + ErmSchema.STATION_TABLE + " st JOIN " + ErmSchema.STATION_DATA_TABLE + " sd ON sd."
                + ErmSchema.COL_STATION_ID + " = st." + ErmSchema.COL_ID + " ORDER BY st." + ErmSchema.COL_ID;

        List<ErmEntityItem> items = new ArrayList<>();
        db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    int id = rs.getInt(1);
                    String type = rs.getString(2);
                    String label = "Station " + id + (type != null ? " (" + type + ")" : "");
                    items.add(new ErmEntityItem(id, label));
                }
            }
            return null;
        });
        return items;
    }

    public static ErmVariableChartData load( ADb db, int stationId ) throws Exception {
        return ErmVariableSeriesLoader.load(db, ErmSchema.STATION_DATA_TABLE, ErmSchema.COL_STATION_ID,
                stationId, "Station " + stationId);
    }
}
