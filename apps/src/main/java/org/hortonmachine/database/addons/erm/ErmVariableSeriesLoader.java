/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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
 * {@link ErmSchema#ENVIRONMENTAL_VARIABLES_TABLE} when available. Rows whose value is the
 * novalue marker ({@link HMConstants#doubleNovalue}, -9999.0) are excluded.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
final class ErmVariableSeriesLoader {
    private ErmVariableSeriesLoader() {
    }

    static ErmVariableChartData load( ADb db, String dataTable, String entityIdColumn, int entityId, String title )
            throws Exception {
        String sql = "SELECT d." + ErmSchema.COL_VAR_ID + ", ev." + ErmSchema.COL_VAR_NAME + ", ev."
                + ErmSchema.COL_VAR_UNIT + ", d." + ErmSchema.COL_TS + ", d." + ErmSchema.COL_VALUE + " FROM "
                + dataTable + " d LEFT JOIN " + ErmSchema.ENVIRONMENTAL_VARIABLES_TABLE + " ev ON d."
                + ErmSchema.COL_VAR_ID + " = ev." + ErmSchema.COL_VAR_ID + " WHERE d." + entityIdColumn
                + " = ? AND d." + ErmSchema.COL_VALUE + " <> " + HMConstants.doubleNovalue + " ORDER BY d."
                + ErmSchema.COL_VAR_ID + ", d." + ErmSchema.COL_TS;

        Map<Integer, ErmVariableChartData.VariableSeries> seriesByVar = new LinkedHashMap<>();
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

                        ErmVariableChartData.VariableSeries series = seriesByVar.get(varId);
                        if (series == null) {
                            series = new ErmVariableChartData.VariableSeries();
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

        ErmVariableChartData data = new ErmVariableChartData();
        data.entityId = entityId;
        data.title = title;
        for( Integer varId : seriesByVar.keySet() ) {
            ErmVariableChartData.VariableSeries series = seriesByVar.get(varId);
            List<Long> times = timesByVar.get(varId);
            List<Double> values = valuesByVar.get(varId);
            series.times = times.stream().mapToLong(Long::longValue).toArray();
            series.values = values.stream().mapToDouble(Double::doubleValue).toArray();
            data.variableSeries.add(series);
        }
        return data;
    }
}
