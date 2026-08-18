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
package org.hortonmachine.database.addons.whetgeo;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hortonmachine.database.addons.whetgeo.WhetgeoStateChartData.DepthSeries;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.gears.io.geoframe.whetgeo.Whetgeo1DOutputsHandler;

/**
 * Loads the data needed for the WHETGEO 1D state chart out of a connected
 * {@link ADb}: the grid's eta coordinates ({@code output_grid}), the top
 * boundary condition forcing timeseries ({@code output_scalars.top_bc}, if
 * present), and one depth/time/value series per state variable actually
 * present in {@code output_state} — discovered dynamically via
 * {@code PRAGMA table_info}, the same optional-column convention already used
 * by {@link Whetgeo1DOutputsHandler} itself, since not every solver run
 * writes the same optional columns.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WhetgeoStateChartDataLoader {

    // optional output_state column -> [display name, axis label]; theta is
    // handled separately since it's the one mandatory depth series.
    private static final Map<String, String[]> OPTIONAL_DEPTH_COLUMNS = new LinkedHashMap<>();
    static {
        OPTIONAL_DEPTH_COLUMNS.put(Whetgeo1DOutputsHandler.COL_WATER_SUCTION,
                new String[]{"Water suction", "Water suction - psi [m]"});
        OPTIONAL_DEPTH_COLUMNS.put(Whetgeo1DOutputsHandler.COL_INTERNAL_ENERGY,
                new String[]{"Internal energy", "internal energy"});
        OPTIONAL_DEPTH_COLUMNS.put(Whetgeo1DOutputsHandler.COL_ICE_CONTENT,
                new String[]{"Ice content", "ice content [-]"});
    }

    private WhetgeoStateChartDataLoader() {
    }

    public static WhetgeoStateChartData load( ADb db ) throws Exception {
        WhetgeoStateChartData data = new WhetgeoStateChartData();

        data.gridEta = loadGridEta(db);

        if (hasColumn(db, Whetgeo1DOutputsHandler.TABLE_OUTPUT_SCALARS, Whetgeo1DOutputsHandler.COL_TOP_BC)) {
            ScalarSeries topBC = loadScalarSeries(db, Whetgeo1DOutputsHandler.COL_TOP_BC);
            data.topBCTimes = topBC.times;
            data.topBCValues = topBC.values;
        }
        if (hasColumn(db, Whetgeo1DOutputsHandler.TABLE_OUTPUT_SCALARS, Whetgeo1DOutputsHandler.COL_BOTTOM_BC)) {
            ScalarSeries bottomBC = loadScalarSeries(db, Whetgeo1DOutputsHandler.COL_BOTTOM_BC);
            data.bottomBCTimes = bottomBC.times;
            data.bottomBCValues = bottomBC.values;
        }

        data.depthSeries
                .add(loadDepthSeries(db, Whetgeo1DOutputsHandler.COL_THETA, "Water content", "Water content - theta [-]"));
        for( Map.Entry<String, String[]> entry : OPTIONAL_DEPTH_COLUMNS.entrySet() ) {
            String column = entry.getKey();
            if (hasColumn(db, Whetgeo1DOutputsHandler.TABLE_OUTPUT_STATE, column)) {
                data.depthSeries.add(loadDepthSeries(db, column, entry.getValue()[0], entry.getValue()[1]));
            }
        }

        return data;
    }

    private static double[] loadGridEta( ADb db ) throws Exception {
        String sql = "SELECT " + Whetgeo1DOutputsHandler.COL_ETA + " FROM " + Whetgeo1DOutputsHandler.TABLE_OUTPUT_GRID
                + " ORDER BY " + Whetgeo1DOutputsHandler.COL_ETA;
        List<Double> eta = new ArrayList<>();
        db.<Void>execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    eta.add(rs.getDouble(1));
                }
            }
            return null;
        });
        return eta.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static ScalarSeries loadScalarSeries( ADb db, String column ) throws Exception {
        String sql = "SELECT " + Whetgeo1DOutputsHandler.COL_TIMESTAMP + ", " + column + " FROM "
                + Whetgeo1DOutputsHandler.TABLE_OUTPUT_SCALARS + " ORDER BY " + Whetgeo1DOutputsHandler.COL_TIMESTAMP;
        List<Long> times = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        db.<Void>execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    times.add(rs.getLong(1));
                    values.add(rs.getDouble(2));
                }
            }
            return null;
        });
        ScalarSeries series = new ScalarSeries();
        series.times = times.stream().mapToLong(Long::longValue).toArray();
        series.values = values.stream().mapToDouble(Double::doubleValue).toArray();
        return series;
    }

    private static class ScalarSeries {
        long[] times = new long[0];
        double[] values = new double[0];
    }

    private static DepthSeries loadDepthSeries( ADb db, String column, String name, String axisLabel ) throws Exception {
        DepthSeries series = new DepthSeries(name, axisLabel);
        String sql = "SELECT " + Whetgeo1DOutputsHandler.COL_TIMESTAMP + ", " + Whetgeo1DOutputsHandler.COL_ETA + ", "
                + column + " FROM " + Whetgeo1DOutputsHandler.TABLE_OUTPUT_STATE + " ORDER BY "
                + Whetgeo1DOutputsHandler.COL_TIMESTAMP + ", " + Whetgeo1DOutputsHandler.COL_ETA;
        List<Long> times = new ArrayList<>();
        List<Double> etas = new ArrayList<>();
        List<Double> values = new ArrayList<>();
        db.<Void>execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    times.add(rs.getLong(1));
                    etas.add(rs.getDouble(2));
                    values.add(rs.getDouble(3));
                }
            }
            return null;
        });
        series.times = times.stream().mapToLong(Long::longValue).toArray();
        series.eta = etas.stream().mapToDouble(Double::doubleValue).toArray();
        series.values = values.stream().mapToDouble(Double::doubleValue).toArray();
        return series;
    }

    private static boolean hasColumn( ADb db, String table, String column ) throws Exception {
        String sql = "PRAGMA table_info(\"" + table + "\")";
        return db.<Boolean>execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    if (column.equalsIgnoreCase(rs.getString(2))) {
                        return true;
                    }
                }
            }
            return false;
        });
    }
}
