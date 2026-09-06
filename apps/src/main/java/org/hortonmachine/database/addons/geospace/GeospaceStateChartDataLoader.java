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
package org.hortonmachine.database.addons.geospace;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.hortonmachine.database.addons.geospace.GeospaceStateChartData.DepthSeries;
import org.hortonmachine.database.addons.geospace.GeospaceStateChartData.SwrcParams;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.gears.io.geoframe.whetgeo.Whetgeo1DOutputSchema;

/**
 * Loads the data needed for the GEOSPACE state chart out of a connected
 * {@link ADb}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GeospaceStateChartDataLoader {

    // optional output_state column -> [display name, axis label]. Every depth
    // series is independently optional: a Richards run with no thermal model never writes temperature, a plain
    // HeatDiffusionSolver1D run never writes theta, etc.
    private static final Map<String, String[]> OPTIONAL_DEPTH_COLUMNS = new LinkedHashMap<>();
    static {
        OPTIONAL_DEPTH_COLUMNS.put(Whetgeo1DOutputSchema.COL_TEMPERATURE,
                new String[]{"Temperature", "Temperature [K]"});
        OPTIONAL_DEPTH_COLUMNS.put(Whetgeo1DOutputSchema.COL_THETA,
                new String[]{"Water content", "Water content - theta [-]"});
        OPTIONAL_DEPTH_COLUMNS.put(Whetgeo1DOutputSchema.COL_WATER_SUCTION,
                new String[]{"Water suction", "Water suction - psi [m]"});
        OPTIONAL_DEPTH_COLUMNS.put(Whetgeo1DOutputSchema.COL_INTERNAL_ENERGY,
                new String[]{"Internal energy", "internal energy"});
        OPTIONAL_DEPTH_COLUMNS.put(Whetgeo1DOutputSchema.COL_ICE_CONTENT,
                new String[]{"Ice content", "ice content [-]"});
    }

    // BrokerGEO's and GEOET's own output tables are optional additions to a WHETGEO-1D output
    // gpkg, written only when this run went through the full GEOSPACE-1D coupled stack (see
    // BrokerGEO's BrokerGeoOutputsHandler / GEOET's GeoetOutputsHandler, in their own repos -
    // inlined here rather than taken as a compile dependency, since this loader only ever needs
    // their table/column *names*, not their writer logic, and apps has no reason to depend on
    // either project's runtime). stressedETs is BrokerGEO's own output (its
    // ETsBrokerOneFluxSolverMain computes it) even though GEOSPACE-1D's coupled test is what
    // drives it step by step - GEOSPACE-1D itself is a pure orchestrator with no output schema
    // of its own.
    private static final String TABLE_BROKERGEO_UPTAKE = "geoframe_brokergeo_output_uptake";
    private static final String COL_STRESSED_ETS = "stressed_ets";
    private static final String TABLE_GEOET_RESULTS = "geoframe_geoet_output_results";
    private static final String COL_EVAPO_TRANSPIRATION = "evapo_transpiration";
    private static final String COL_EVAPORATION = "evaporation";
    private static final String COL_TRANSPIRATION = "transpiration";

    private GeospaceStateChartDataLoader() {
    }

    public static GeospaceStateChartData load( ADb db ) throws Exception {
        GeospaceStateChartData data = new GeospaceStateChartData();

        boolean withParameterID = hasColumn(db, Whetgeo1DOutputSchema.TABLE_OUTPUT_GRID,
                Whetgeo1DOutputSchema.COL_PARAMETER_ID);
        loadGrid(db, data, withParameterID);

        if (withParameterID && db.hasTable(Whetgeo1DOutputSchema.TABLE_OUTPUT_SWRC_PARAMETERS)) {
            data.swrcParameters = loadSwrcParameters(db);
        }

        if (db.hasTable(Whetgeo1DOutputSchema.TABLE_OUTPUT_METADATA)) {
            loadBCTypes(db, data);
        }

        if (hasColumn(db, Whetgeo1DOutputSchema.TABLE_OUTPUT_SCALARS, Whetgeo1DOutputSchema.COL_TOP_BC)) {
            ScalarSeries topBC = loadScalarSeries(db, Whetgeo1DOutputSchema.COL_TOP_BC);
            data.topBCTimes = topBC.times;
            data.topBCValues = topBC.values;
        }
        if (hasColumn(db, Whetgeo1DOutputSchema.TABLE_OUTPUT_SCALARS, Whetgeo1DOutputSchema.COL_BOTTOM_BC)) {
            ScalarSeries bottomBC = loadScalarSeries(db, Whetgeo1DOutputSchema.COL_BOTTOM_BC);
            data.bottomBCTimes = bottomBC.times;
            data.bottomBCValues = bottomBC.values;
        }

        for( Map.Entry<String, String[]> entry : OPTIONAL_DEPTH_COLUMNS.entrySet() ) {
            String column = entry.getKey();
            if (hasColumn(db, Whetgeo1DOutputSchema.TABLE_OUTPUT_STATE, column)) {
                data.depthSeries.add(loadDepthSeries(db, Whetgeo1DOutputSchema.TABLE_OUTPUT_STATE,
                        Whetgeo1DOutputSchema.COL_TIMESTAMP, Whetgeo1DOutputSchema.COL_ETA, column, entry.getValue()[0],
                        entry.getValue()[1]));
            }
        }

        // BrokerGEO's own optional addition: root water uptake is a depth/time series just like
        // theta or water suction above, so it's rendered the same way - just appended to the same
        // list rather than kept in a dedicated field.
        if (db.hasTable(TABLE_BROKERGEO_UPTAKE)) {
            data.depthSeries.add(loadDepthSeries(db, TABLE_BROKERGEO_UPTAKE, Whetgeo1DOutputSchema.COL_TIMESTAMP,
                    Whetgeo1DOutputSchema.COL_ETA, COL_STRESSED_ETS, "Root water uptake",
                    "Root water uptake - stressedETs [mm]"));
        }

        // GEOET's own optional addition: one scalar-per-timestep ET series (not depth-resolved).
        if (db.hasTable(TABLE_GEOET_RESULTS)) {
            data.etSeries = loadEtSeries(db);
        }

        return data;
    }

    private static void loadGrid( ADb db, GeospaceStateChartData data, boolean withParameterID ) throws Exception {
        String sql = "SELECT " + Whetgeo1DOutputSchema.COL_ETA + (withParameterID
                ? ", " + Whetgeo1DOutputSchema.COL_PARAMETER_ID
                : "") + " FROM " + Whetgeo1DOutputSchema.TABLE_OUTPUT_GRID + " ORDER BY "
                + Whetgeo1DOutputSchema.COL_ETA;
        List<Double> eta = new ArrayList<>();
        List<Integer> parameterID = new ArrayList<>();
        db.<Void>execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    eta.add(rs.getDouble(1));
                    if (withParameterID) {
                        parameterID.add(rs.getInt(2));
                    }
                }
            }
            return null;
        });
        data.gridEta = eta.stream().mapToDouble(Double::doubleValue).toArray();
        if (withParameterID) {
            data.gridParameterID = parameterID.stream().mapToInt(Integer::intValue).toArray();
        }
    }

    private static List<SwrcParams> loadSwrcParameters( ADb db ) throws Exception {
        String sql = "SELECT " + Whetgeo1DOutputSchema.COL_ID + ", " + Whetgeo1DOutputSchema.COL_THETA_S + ", "
                + Whetgeo1DOutputSchema.COL_THETA_R + ", " + Whetgeo1DOutputSchema.COL_KS + ", "
                + Whetgeo1DOutputSchema.COL_N + ", " + Whetgeo1DOutputSchema.COL_ALPHA + " FROM "
                + Whetgeo1DOutputSchema.TABLE_OUTPUT_SWRC_PARAMETERS + " ORDER BY " + Whetgeo1DOutputSchema.COL_ID;
        List<SwrcParams> params = new ArrayList<>();
        db.<Void>execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                while( rs.next() ) {
                    params.add(new SwrcParams(rs.getInt(1), rs.getDouble(2), rs.getDouble(3), rs.getDouble(4),
                            rs.getDouble(5), rs.getDouble(6)));
                }
            }
            return null;
        });
        return params;
    }

    private static void loadBCTypes( ADb db, GeospaceStateChartData data ) throws Exception {
        String sql = "SELECT " + Whetgeo1DOutputSchema.COL_TOP_BC_TYPE + ", "
                + Whetgeo1DOutputSchema.COL_BOTTOM_BC_TYPE + " FROM " + Whetgeo1DOutputSchema.TABLE_OUTPUT_METADATA;
        db.<Void>execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    data.topBCType = rs.getString(1);
                    data.bottomBCType = rs.getString(2);
                }
            }
            return null;
        });
    }

    /**
     * Loads {@code geoframe_geoet_output_results}: the combined {@code evapo_transpiration} column
     * always exists when the table does (every ET model writes it - see GEOET's own
     * {@code GeoetOutputsHandler}), while {@code evaporation}/{@code transpiration} are only
     * present for models that split the total, checked independently via {@code hasColumn} the
     * same way every other optional column in this loader is.
     */
    private static GeospaceStateChartData.EtSeries loadEtSeries( ADb db ) throws Exception {
        GeospaceStateChartData.EtSeries series = new GeospaceStateChartData.EtSeries();
        boolean withEvaporation = hasColumn(db, TABLE_GEOET_RESULTS, COL_EVAPORATION);
        boolean withTranspiration = hasColumn(db, TABLE_GEOET_RESULTS, COL_TRANSPIRATION);

        StringBuilder sql = new StringBuilder(
                "SELECT " + Whetgeo1DOutputSchema.COL_TIMESTAMP + ", " + COL_EVAPO_TRANSPIRATION);
        if (withEvaporation) {
            sql.append(", ").append(COL_EVAPORATION);
        }
        if (withTranspiration) {
            sql.append(", ").append(COL_TRANSPIRATION);
        }
        sql.append(" FROM ").append(TABLE_GEOET_RESULTS).append(" ORDER BY ")
                .append(Whetgeo1DOutputSchema.COL_TIMESTAMP);

        List<Long> times = new ArrayList<>();
        List<Double> et = new ArrayList<>();
        List<Double> evaporation = new ArrayList<>();
        List<Double> transpiration = new ArrayList<>();
        db.<Void>execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql.toString())) {
                while( rs.next() ) {
                    int col = 1;
                    times.add(rs.getLong(col++));
                    et.add(rs.getDouble(col++));
                    if (withEvaporation) {
                        evaporation.add(rs.getDouble(col++));
                    }
                    if (withTranspiration) {
                        transpiration.add(rs.getDouble(col++));
                    }
                }
            }
            return null;
        });
        series.times = times.stream().mapToLong(Long::longValue).toArray();
        series.evapoTranspiration = et.stream().mapToDouble(Double::doubleValue).toArray();
        if (withEvaporation) {
            series.evaporation = evaporation.stream().mapToDouble(Double::doubleValue).toArray();
        }
        if (withTranspiration) {
            series.transpiration = transpiration.stream().mapToDouble(Double::doubleValue).toArray();
        }
        return series;
    }

    private static ScalarSeries loadScalarSeries( ADb db, String column ) throws Exception {
        String sql = "SELECT " + Whetgeo1DOutputSchema.COL_TIMESTAMP + ", " + column + " FROM "
                + Whetgeo1DOutputSchema.TABLE_OUTPUT_SCALARS + " ORDER BY " + Whetgeo1DOutputSchema.COL_TIMESTAMP;
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

    private static DepthSeries loadDepthSeries( ADb db, String table, String timestampColumn, String etaColumn,
            String valueColumn, String name, String axisLabel ) throws Exception {
        DepthSeries series = new DepthSeries(name, axisLabel);
        String sql = "SELECT " + timestampColumn + ", " + etaColumn + ", " + valueColumn + " FROM " + table
                + " ORDER BY " + timestampColumn + ", " + etaColumn;
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
