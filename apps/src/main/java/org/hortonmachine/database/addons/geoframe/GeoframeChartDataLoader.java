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
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;

/**
 * Loads the data needed for the GeoFrame water budget chart out of a connected
 * {@link ADb}: the most downstream basin (the one whose topology row has
 * downstream_basin_id = 0), its simulated discharge from the given simulation
 * table, and its precipitation/temperature (basin_data) and observed discharge
 * (station_data, joined to station on basin_id). The simulated discharge time
 * range rules: precipitation, temperature and observed discharge are all clipped
 * to the [first, last] timestamp of the simulated series.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeChartDataLoader {
    private GeoframeChartDataLoader() {
    }

    public static GeoframeChartData load( ADb db, String simDischargeTableName ) throws Exception {
        Integer basinId = findMostDownstreamBasinId(db);
        if (basinId == null) {
            throw new IllegalStateException("No basin found in the '" + GeoframeSchema.TOPOLOGY_TABLE + "' table with "
                    + GeoframeSchema.TOPOLOGY_DOWNSTREAM_BASIN + " = 0 (most downstream basin).");
        }

        GeoframeChartData data = new GeoframeChartData();
        data.basinId = basinId;

        Series simulated = querySimulatedDischargeSeries(db, simDischargeTableName, basinId);
        data.simulatedDischargeTimes = simulated.times;
        data.simulatedDischargeValues = simulated.values;

        Long minTs = simulated.times.length > 0 ? simulated.times[0] : null;
        Long maxTs = simulated.times.length > 0 ? simulated.times[simulated.times.length - 1] : null;

        Series precipitation = queryBasinDataSeries(db, basinId, GeoframeSchema.VAR_PRECIPITATION, minTs, maxTs);
        data.precipitationTimes = precipitation.times;
        data.precipitationValues = precipitation.values;

        Series temperature = queryBasinDataSeries(db, basinId, GeoframeSchema.VAR_TEMPERATURE, minTs, maxTs);
        data.temperatureTimes = temperature.times;
        data.temperatureValues = temperature.values;

        Series observed = queryObservedDischargeSeries(db, basinId, minTs, maxTs);
        data.observedDischargeTimes = observed.times;
        data.observedDischargeValues = observed.values;

        return data;
    }

    private static Integer findMostDownstreamBasinId( ADb db ) throws Exception {
        String sql = "SELECT " + GeoframeSchema.TOPOLOGY_UPSTREAM_BASIN + " FROM " + GeoframeSchema.TOPOLOGY_TABLE + " WHERE "
                + GeoframeSchema.TOPOLOGY_DOWNSTREAM_BASIN + " = 0";
        return db.execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
                return null;
            }
        });
    }

    private static Series queryBasinDataSeries( ADb db, int basinId, int varId, Long minTs, Long maxTs ) throws Exception {
        List<Object> params = new ArrayList<>();
        params.add(basinId);
        params.add(varId);
        String sql = "SELECT " + GeoframeSchema.COL_TS + ", " + GeoframeSchema.COL_VALUE + " FROM "
                + GeoframeSchema.BASIN_DATA_TABLE + " WHERE " + GeoframeSchema.COL_BASIN_ID + " = ? AND "
                + GeoframeSchema.COL_VAR_ID + " = ?" + timeBoundClause(GeoframeSchema.COL_TS, minTs, maxTs, params)
                + " ORDER BY " + GeoframeSchema.COL_TS;
        return queryTimeSeries(db, sql, params.toArray());
    }

    private static Series querySimulatedDischargeSeries( ADb db, String simTableName, int basinId ) throws Exception {
        String sql = "SELECT " + GeoframeSchema.COL_TS + ", " + GeoframeSchema.COL_VALUE + " FROM " + simTableName
                + " WHERE " + GeoframeSchema.COL_BASIN_ID + " = ? ORDER BY " + GeoframeSchema.COL_TS;
        return queryTimeSeries(db, sql, basinId);
    }

    /**
     * Observed discharge normally comes from the stream gauge station tied to this basin
     * (station.basin_id = basinId). Some datasets only carry a single, whole-catchment
     * discharge station that is not tied to any sub-basin (station.basin_id IS NULL); in
     * that case fall back to that station's data. Both branches are clipped to
     * [minTs, maxTs] (the simulated discharge's time range).
     */
    private static Series queryObservedDischargeSeries( ADb db, int basinId, Long minTs, Long maxTs ) throws Exception {
        List<Object> params = new ArrayList<>();
        params.add(basinId);
        params.add(GeoframeSchema.VAR_DISCHARGE);
        String sql = "SELECT sd." + GeoframeSchema.COL_TS + ", sd." + GeoframeSchema.COL_VALUE + " FROM "
                + GeoframeSchema.STATION_DATA_TABLE + " sd JOIN " + GeoframeSchema.STATION_TABLE + " st ON sd."
                + GeoframeSchema.COL_STATION_ID + " = st." + GeoframeSchema.COL_ID + " WHERE st." + GeoframeSchema.COL_BASIN_ID
                + " = ? AND sd." + GeoframeSchema.COL_VAR_ID + " = ? AND sd." + GeoframeSchema.COL_VALUE + " >= 0"
                + timeBoundClause("sd." + GeoframeSchema.COL_TS, minTs, maxTs, params) + " ORDER BY sd." + GeoframeSchema.COL_TS;
        Series series = queryTimeSeries(db, sql, params.toArray());
        if (series.times.length > 0) {
            return series;
        }

        List<Object> fallbackParams = new ArrayList<>();
        fallbackParams.add(GeoframeSchema.VAR_DISCHARGE);
        String fallbackSql = "SELECT sd." + GeoframeSchema.COL_TS + ", sd." + GeoframeSchema.COL_VALUE + " FROM "
                + GeoframeSchema.STATION_DATA_TABLE + " sd JOIN " + GeoframeSchema.STATION_TABLE + " st ON sd."
                + GeoframeSchema.COL_STATION_ID + " = st." + GeoframeSchema.COL_ID + " WHERE st." + GeoframeSchema.COL_BASIN_ID
                + " IS NULL AND sd." + GeoframeSchema.COL_VAR_ID + " = ? AND sd." + GeoframeSchema.COL_VALUE + " >= 0"
                + timeBoundClause("sd." + GeoframeSchema.COL_TS, minTs, maxTs, fallbackParams) + " ORDER BY sd."
                + GeoframeSchema.COL_TS;
        return queryTimeSeries(db, fallbackSql, fallbackParams.toArray());
    }

    private static String timeBoundClause( String tsColumn, Long minTs, Long maxTs, List<Object> params ) {
        StringBuilder sb = new StringBuilder();
        if (minTs != null) {
            sb.append(" AND ").append(tsColumn).append(" >= ?");
            params.add(minTs);
        }
        if (maxTs != null) {
            sb.append(" AND ").append(tsColumn).append(" <= ?");
            params.add(maxTs);
        }
        return sb.toString();
    }

    private static Series queryTimeSeries( ADb db, String sql, Object... params ) throws Exception {
        List<Long> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        db.<Void>execOnConnection(connection -> {
            try (IHMPreparedStatement ps = connection.prepareStatement(sql)) {
                for( int i = 0; i < params.length; i++ ) {
                    Object param = params[i];
                    if (param instanceof Long) {
                        ps.setLong(i + 1, (Long) param);
                    } else {
                        ps.setInt(i + 1, ((Number) param).intValue());
                    }
                }
                try (IHMResultSet rs = ps.executeQuery()) {
                    while( rs.next() ) {
                        times.add(rs.getLong(1));
                        values.add(rs.getDouble(2));
                    }
                }
                return null;
            }
        });

        Series series = new Series();
        series.times = times.stream().mapToLong(Long::longValue).toArray();
        series.values = values.stream().mapToDouble(Double::doubleValue).toArray();
        return series;
    }

    private static class Series {
        long[] times = new long[0];
        double[] values = new double[0];
    }
}
