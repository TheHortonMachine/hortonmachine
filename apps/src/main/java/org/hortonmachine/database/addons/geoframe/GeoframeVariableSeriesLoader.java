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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMPreparedStatement;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.gears.libs.modules.HMConstants;

/**
 * Shared query behind both the station data and basin data variable charts: loads a single
 * entity's (station or basin) rows out of a "*_data" table shaped like {@code station_data}/
 * {@code basin_data} (a timestamp, an entity foreign key, a var_id and a value column), split by
 * environmental variable, with name/unit looked up from
 * {@link GeoframeSchema#ENVIRONMENTAL_VARIABLES_TABLE} when available. Rows whose value is the
 * novalue marker ({@link HMConstants#doubleNovalue}, -9999.0) are excluded.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
final class GeoframeVariableSeriesLoader {
    private GeoframeVariableSeriesLoader() {
    }

    static GeoframeVariableChartData load( ADb db, String dataTable, String entityIdColumn, int entityId, String title )
            throws Exception {
        String sql = "SELECT d." + GeoframeSchema.COL_VAR_ID + ", ev." + GeoframeSchema.COL_VAR_NAME + ", ev."
                + GeoframeSchema.COL_VAR_UNIT + ", d." + GeoframeSchema.COL_TS + ", d." + GeoframeSchema.COL_VALUE + " FROM "
                + dataTable + " d LEFT JOIN " + GeoframeSchema.ENVIRONMENTAL_VARIABLES_TABLE + " ev ON d."
                + GeoframeSchema.COL_VAR_ID + " = ev." + GeoframeSchema.COL_VAR_ID + " WHERE d." + entityIdColumn
                + " = ? AND d." + GeoframeSchema.COL_VALUE + " <> " + HMConstants.doubleNovalue + " ORDER BY d."
                + GeoframeSchema.COL_VAR_ID + ", d." + GeoframeSchema.COL_TS;

        Map<Integer, GeoframeVariableChartData.VariableSeries> seriesByVar = new LinkedHashMap<>();
        Map<Integer, List<Long>> timesByVar = new LinkedHashMap<>();
        Map<Integer, List<Double>> valuesByVar = new LinkedHashMap<>();

        db.<Void>execOnConnection(connection -> {
            try (IHMPreparedStatement ps = connection.prepareStatement(sql)) {
                ps.setInt(1, entityId);
                try (IHMResultSet rs = ps.executeQuery()) {
                    while( rs.next() ) {
                        int varId = rs.getInt(1);
                        String name = rs.getString(2);
                        String unit = rs.getString(3);
                        long ts = rs.getLong(4);
                        double value = rs.getDouble(5);

                        GeoframeVariableChartData.VariableSeries series = seriesByVar.get(varId);
                        if (series == null) {
                            series = new GeoframeVariableChartData.VariableSeries();
                            series.varId = varId;
                            series.name = name != null ? name : ("Variable " + varId);
                            series.unit = unit;
                            seriesByVar.put(varId, series);
                            timesByVar.put(varId, new ArrayList<>());
                            valuesByVar.put(varId, new ArrayList<>());
                        }
                        timesByVar.get(varId).add(ts);
                        valuesByVar.get(varId).add(value);
                    }
                }
            }
            return null;
        });

        GeoframeVariableChartData data = new GeoframeVariableChartData();
        data.entityId = entityId;
        data.title = title;
        for( Integer varId : seriesByVar.keySet() ) {
            GeoframeVariableChartData.VariableSeries series = seriesByVar.get(varId);
            List<Long> times = timesByVar.get(varId);
            List<Double> values = valuesByVar.get(varId);
            series.times = times.stream().mapToLong(Long::longValue).toArray();
            series.values = values.stream().mapToDouble(Double::doubleValue).toArray();
            data.variableSeries.add(series);
        }
        return data;
    }
}
