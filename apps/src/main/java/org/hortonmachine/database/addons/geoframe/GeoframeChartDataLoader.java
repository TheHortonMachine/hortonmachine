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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.ASpatialDb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.gears.io.dbs.DbsHelper;

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

        GeoframeChartData data = loadDischargeAndMeteo(db, simDischargeTableName, basinId, null, null);

        Long minTs = data.simulatedDischargeTimes.length > 0 ? data.simulatedDischargeTimes[0] : null;
        Long maxTs = data.simulatedDischargeTimes.length > 0
                ? data.simulatedDischargeTimes[data.simulatedDischargeTimes.length - 1]
                : null;
        Series observed = queryObservedDischargeSeries(db, basinId, minTs, maxTs);
        data.observedDischargeTimes = observed.times;
        data.observedDischargeValues = observed.values;

        return data;
    }

    /**
     * Reloads simulated discharge, precipitation/temperature and observed discharge for a
     * different basin, clipped to the same time range as the reference load. Observed discharge
     * is re-queried (not copied from the reference) since it depends on whether THIS basin has
     * its own stream gauge - see {@link #queryObservedDischargeSeries}.
     */
    public static GeoframeChartData loadForBasin( ADb db, String simDischargeTableName, int basinId,
            GeoframeChartData reference ) throws Exception {
        Long minTs = reference.simulatedDischargeTimes.length > 0 ? reference.simulatedDischargeTimes[0] : null;
        Long maxTs = reference.simulatedDischargeTimes.length > 0
                ? reference.simulatedDischargeTimes[reference.simulatedDischargeTimes.length - 1]
                : null;

        GeoframeChartData data = loadDischargeAndMeteo(db, simDischargeTableName, basinId, minTs, maxTs);
        Series observed = queryObservedDischargeSeries(db, basinId, minTs, maxTs);
        data.observedDischargeTimes = observed.times;
        data.observedDischargeValues = observed.values;
        return data;
    }

    /**
     * Loads the basin polygons ({@link GeoframeSchema#COL_ID}, {@link GeoframeSchema#COL_GEOM})
     * as a feature collection, for the basins-selector map.
     */
    public static SimpleFeatureCollection loadBasinPolygons( ASpatialDb db ) throws Exception {
        String sql = "SELECT " + GeoframeSchema.COL_ID + ", " + GeoframeSchema.COL_GEOM + " FROM " + GeoframeSchema.BASIN_TABLE;
        return DbsHelper.runRawSqlToFeatureCollection("basins", db, sql, null);
    }

    /**
     * Loads the stream network lines, drawn on the basins map to make the basin layout easier to
     * read.
     */
    public static SimpleFeatureCollection loadNetworkLines( ASpatialDb db ) throws Exception {
        String sql = "SELECT " + GeoframeSchema.COL_GEOM + " FROM " + GeoframeSchema.NET_TABLE;
        return DbsHelper.runRawSqlToFeatureCollection("network", db, sql, null);
    }

    /**
     * Loads the stream/discharge gauge station points, drawn on the basins map.
     */
    public static SimpleFeatureCollection loadStreamGaugeStations( ASpatialDb db ) throws Exception {
        return loadStations(db, GeoframeSchema.STATION_TYPE_STREAM_GAUGE, "streamGauges");
    }

    /**
     * Loads the meteo station points, drawn on the basins map.
     */
    public static SimpleFeatureCollection loadMeteoStations( ASpatialDb db ) throws Exception {
        return loadStations(db, GeoframeSchema.STATION_TYPE_METEO, "meteoStations");
    }

    private static SimpleFeatureCollection loadStations( ASpatialDb db, String stationType, String name ) throws Exception {
        String sql = "SELECT " + GeoframeSchema.COL_ID + ", " + GeoframeSchema.COL_GEOM + " FROM " + GeoframeSchema.STATION_TABLE
                + " WHERE " + GeoframeSchema.COL_TYPE + " = '" + stationType + "'";
        return DbsHelper.runRawSqlToFeatureCollection(name, db, sql, null);
    }

    private static GeoframeChartData loadDischargeAndMeteo( ADb db, String simDischargeTableName, int basinId, Long minTs,
            Long maxTs ) throws Exception {
        GeoframeChartData data = new GeoframeChartData();
        data.basinId = basinId;

        Series simulated = querySimulatedDischargeSeries(db, simDischargeTableName, basinId);
        data.simulatedDischargeTimes = simulated.times;
        data.simulatedDischargeValues = simulated.values;

        Long effectiveMinTs = minTs != null ? minTs : (simulated.times.length > 0 ? simulated.times[0] : null);
        Long effectiveMaxTs = maxTs != null ? maxTs
                : (simulated.times.length > 0 ? simulated.times[simulated.times.length - 1] : null);

        Series precipitation = queryBasinDataSeries(db, basinId, GeoframeSchema.VAR_PRECIPITATION, effectiveMinTs,
                effectiveMaxTs);
        data.precipitationTimes = precipitation.times;
        data.precipitationValues = precipitation.values;

        Series temperature = queryBasinDataSeries(db, basinId, GeoframeSchema.VAR_TEMPERATURE, effectiveMinTs, effectiveMaxTs);
        data.temperatureTimes = temperature.times;
        data.temperatureValues = temperature.values;

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
     * Observed discharge is only plotted if the given basin actually has its own stream gauge:
     * a station row with basin_id = basinId and type = STREAM_GAUGE. If no such station (or no
     * data for it) exists, an empty series is returned and no observed discharge line is drawn.
     */
    private static Series queryObservedDischargeSeries( ADb db, int basinId, Long minTs, Long maxTs ) throws Exception {
        List<Object> params = new ArrayList<>();
        params.add(basinId);
        params.add(GeoframeSchema.STATION_TYPE_STREAM_GAUGE);
        params.add(GeoframeSchema.VAR_DISCHARGE);
        String sql = "SELECT sd." + GeoframeSchema.COL_TS + ", sd." + GeoframeSchema.COL_VALUE + " FROM "
                + GeoframeSchema.STATION_DATA_TABLE + " sd JOIN " + GeoframeSchema.STATION_TABLE + " st ON sd."
                + GeoframeSchema.COL_STATION_ID + " = st." + GeoframeSchema.COL_ID + " WHERE st." + GeoframeSchema.COL_BASIN_ID
                + " = ? AND st." + GeoframeSchema.COL_TYPE + " = ? AND sd." + GeoframeSchema.COL_VAR_ID + " = ? AND sd."
                + GeoframeSchema.COL_VALUE + " >= 0" + timeBoundClause("sd." + GeoframeSchema.COL_TS, minTs, maxTs, params)
                + " ORDER BY sd." + GeoframeSchema.COL_TS;
        return queryTimeSeries(db, sql, params.toArray());
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
                    } else if (param instanceof String) {
                        ps.setString(i + 1, (String) param);
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
