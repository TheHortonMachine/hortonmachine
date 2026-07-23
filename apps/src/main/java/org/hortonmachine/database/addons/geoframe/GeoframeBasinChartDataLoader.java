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

import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;

/**
 * Loads the data needed for the GeoFrame basin data chart out of a connected
 * {@link ADb}: the list of basins that actually have rows in
 * {@link GeoframeSchema#BASIN_DATA_TABLE}, and, for a given basin, its data
 * split by environmental variable (var_id) - typically evapotranspiration,
 * precipitation, radiation and temperature - with name/unit looked up from
 * {@link GeoframeSchema#ENVIRONMENTAL_VARIABLES_TABLE} when available.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeBasinChartDataLoader {
    private GeoframeBasinChartDataLoader() {
    }

    /** Basins known to have at least one row in basin_data. */
    public static List<GeoframeEntityItem> loadBasinsWithData( ADb db ) throws Exception {
        String sql = "SELECT DISTINCT b." + GeoframeSchema.COL_ID + " FROM " + GeoframeSchema.BASIN_TABLE + " b JOIN "
                + GeoframeSchema.BASIN_DATA_TABLE + " bd ON bd." + GeoframeSchema.COL_BASIN_ID + " = b." + GeoframeSchema.COL_ID
                + " ORDER BY b." + GeoframeSchema.COL_ID;

        List<GeoframeEntityItem> items = new ArrayList<>();
        db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    int id = rs.getInt(1);
                    items.add(new GeoframeEntityItem(id, "Basin " + id));
                }
            }
            return null;
        });
        return items;
    }

    public static GeoframeVariableChartData load( ADb db, int basinId ) throws Exception {
        return GeoframeVariableSeriesLoader.load(db, GeoframeSchema.BASIN_DATA_TABLE, GeoframeSchema.COL_BASIN_ID, basinId,
                "Basin " + basinId);
    }
}
