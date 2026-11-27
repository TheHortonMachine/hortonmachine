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
import java.util.Objects;

import org.hortonmachine.database.addons.geospace.GeospaceGridData.Layer;
import org.hortonmachine.database.addons.geospace.GeospaceGridData.PseudoCell;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.IHMResultSet;
import org.hortonmachine.dbs.compat.IHMStatement;
import org.hortonmachine.gears.io.geoframe.whetgeo.Whetgeo1DOutputSchema;

/**
 * Loads a {@link GeospaceGridData} out of a WHETGEO 1D input or output gpkg.
 *
 * <p>
 * The input tables are read the way WHETGEO-1D's {@code Whetgeo1DInputsHandler}
 * reads them (its table and column names are repeated here, since apps does not
 * depend on WHETGEO-1D), so the cells drawn are the cells the solver gets.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GeospaceGridDataLoader {

    public static final String TABLE_INPUT_GRID = "grid_geometry";
    public static final String TABLE_INPUT_IC = "initial_condition";
    public static final String TABLE_INPUT_PARAMETERS = "swrc_parameters";
    public static final String TABLE_INPUT_PARAMETER_TYPES = "swrc_parameter_types";
    public static final String TABLE_INPUT_PSEUDO_CELL = "pseudo_cell";

    /** The no-data value of the WHETGEO 1D tables. */
    public static final double NODATA = -9999.0;

    private static final double EPS = 1e-9;

    private GeospaceGridDataLoader() {
    }

    /**
     * @return true if the db holds the tables of a WHETGEO 1D input gpkg.
     */
    public static boolean isInputDb( ADb db ) throws Exception {
        return db.hasTable(TABLE_INPUT_GRID) && db.hasTable(TABLE_INPUT_IC) && db.hasTable(TABLE_INPUT_PARAMETERS);
    }

    /**
     * Read the column from an input gpkg.
     */
    public static GeospaceGridData loadInput( ADb db ) throws Exception {
        GeospaceGridData data = new GeospaceGridData();
        data.source = "input " + TABLE_INPUT_GRID;

        // grid_geometry, surface first: each row with k > 0 is a layer reaching down
        // to the eta of the next row, the last row (k <= 0) marks the bottom
        List<Object[]> rows = query(db, "SELECT eta, k, equationStateID, parameterID FROM " + TABLE_INPUT_GRID
                + " ORDER BY eta DESC");
        List<Double> eta = new ArrayList<>();
        List<Double> cv = new ArrayList<>();
        List<Integer> parameterID = new ArrayList<>();
        List<Integer> equationStateID = new ArrayList<>();
        List<Double> etaDual = new ArrayList<>();
        for( int i = 0; i < rows.size(); i++ ) {
            Object[] row = rows.get(i);
            double etaTop = toDouble(row[0]);
            int k = toInt(row[1]);
            if (k <= 0) {
                continue;
            }
            if (i + 1 >= rows.size()) {
                data.warnings.add("The layer at eta=" + etaTop + " has no row below it to mark its bottom: it is ignored.");
                continue;
            }
            double etaBottom = toDouble(rows.get(i + 1)[0]);
            double cellSize = (etaTop - etaBottom) / k;
            int esID = toInt(row[2]);
            int pID = toInt(row[3]);
            if (etaDual.isEmpty()) {
                etaDual.add(etaTop);
            }
            for( int j = 0; j < k; j++ ) {
                eta.add(etaTop - (j + 0.5) * cellSize);
                cv.add(cellSize);
                equationStateID.add(esID);
                parameterID.add(pID);
                etaDual.add(etaTop - (j + 1) * cellSize);
            }
            Layer layer = new Layer(etaTop, etaBottom, k, cellSize, cellSize, esID, pID, 1);
            // consecutive rows with the same ids are one layer to the eye, ex. a grid
            // written one row per cell to get irregular cell sizes
            int last = data.layers.size() - 1;
            if (last >= 0 && data.layers.get(last).sameKindAs(layer)) {
                data.layers.set(last, data.layers.get(last).mergedWith(layer));
            } else {
                data.layers.add(layer);
            }
        }
        data.eta = toDoubles(eta);
        data.controlVolume = toDoubles(cv);
        data.etaDual = toDoubles(etaDual);
        data.parameterID = toInts(parameterID);
        data.equationStateID = toInts(equationStateID);

        if (db.hasTable(TABLE_INPUT_PSEUDO_CELL)) {
            boolean withNote = hasColumn(db, TABLE_INPUT_PSEUDO_CELL, "note");
            for( Object[] row : query(db, "SELECT stackOrder, equationStateID, parameterID, controlVolume"
                    + (withNote ? ", note" : "") + " FROM " + TABLE_INPUT_PSEUDO_CELL + " ORDER BY stackOrder ASC") ) {
                data.pseudoCells.add(new PseudoCell(toInt(row[0]), toInt(row[1]), toInt(row[2]), toDouble(row[3]),
                        withNote && row[4] != null ? row[4].toString() : ""));
            }
        }

        loadInputInitialConditions(db, data);
        loadParameterSets(db, TABLE_INPUT_PARAMETERS, "id", data);
        if (db.hasTable(TABLE_INPUT_PARAMETER_TYPES)) {
            for( Object[] row : query(db, "SELECT val1, val2 FROM " + TABLE_INPUT_PARAMETER_TYPES) ) {
                if (row[0] != null && row[1] != null) {
                    data.parameterSlots.put(row[0].toString(), row[1].toString());
                }
            }
        }
        checkParameterIDs(data);
        return data;
    }

    /**
     * Same as {@code Whetgeo1DInputsHandler}: only the first two points (surface
     * first) are used, with a linear interpolation in eta between them, extrapolated
     * beyond them. The pseudo cells, at the surface, get the surface value.
     */
    private static void loadInputInitialConditions( ADb db, GeospaceGridData data ) throws Exception {
        for( Object[] row : query(db, "SELECT eta, psi0, t0 FROM " + TABLE_INPUT_IC + " ORDER BY eta DESC") ) {
            data.icPoints.add(new double[]{toDouble(row[0]), toDouble(row[1]), toDouble(row[2])});
        }
        if (data.icPoints.size() < 2) {
            data.warnings.add(TABLE_INPUT_IC + " has " + data.icPoints.size()
                    + " rows, but two are needed: the initial condition is not shown.");
            return;
        }
        if (data.icPoints.size() > 2) {
            data.warnings.add(TABLE_INPUT_IC + " has " + data.icPoints.size()
                    + " rows, but the solver only interpolates between the first two (eta " + data.icPoints.get(0)[0]
                    + " and " + data.icPoints.get(1)[0] + "): the others are ignored.");
        }
        double[] top = data.icPoints.get(0);
        double[] bottom = data.icPoints.get(1);
        int n = data.cellCount();
        data.psiIC = new double[n];
        data.temperatureIC = new double[n];
        for( int i = 0; i < n; i++ ) {
            double t = (data.eta[i] - top[0]) / (bottom[0] - top[0]);
            data.psiIC[i] = top[1] + t * (bottom[1] - top[1]);
            data.temperatureIC[i] = top[2] + t * (bottom[2] - top[2]);
        }
    }

    /**
     * Read the column from an output gpkg. Layers are runs of cells with the same
     * {@code parameter_id}, or one layer if that column was not written.
     */
    public static GeospaceGridData loadOutput( ADb db ) throws Exception {
        GeospaceGridData data = new GeospaceGridData();
        data.source = "output " + Whetgeo1DOutputSchema.TABLE_OUTPUT_GRID;

        String table = Whetgeo1DOutputSchema.TABLE_OUTPUT_GRID;
        boolean withParameterID = hasColumn(db, table, Whetgeo1DOutputSchema.COL_PARAMETER_ID);
        boolean withPsi = hasColumn(db, table, Whetgeo1DOutputSchema.COL_PSI);
        boolean withTemperature = hasColumn(db, table, Whetgeo1DOutputSchema.COL_TEMPERATURE_IC);
        String sql = "SELECT " + Whetgeo1DOutputSchema.COL_ETA + ", " + Whetgeo1DOutputSchema.COL_CONTROL_VOLUME + ", "
                + (withPsi ? Whetgeo1DOutputSchema.COL_PSI : "NULL") + ", "
                + (withTemperature ? Whetgeo1DOutputSchema.COL_TEMPERATURE_IC : "NULL") + ", "
                + (withParameterID ? Whetgeo1DOutputSchema.COL_PARAMETER_ID : "NULL") + " FROM " + table + " ORDER BY "
                + Whetgeo1DOutputSchema.COL_ETA + " DESC";
        List<Object[]> rows = query(db, sql);

        // pseudo cells have no thickness and sit on the top face of the first real cell
        int firstReal = 0;
        while( firstReal + 1 < rows.size() ) {
            double etaHere = toDouble(rows.get(firstReal)[0]);
            double topFaceBelow = toDouble(rows.get(firstReal + 1)[0]) + toDouble(rows.get(firstReal + 1)[1]) / 2.0;
            if (Math.abs(etaHere - topFaceBelow) > EPS) {
                break;
            }
            firstReal++;
        }
        for( int i = firstReal - 1, order = 0; i >= 0; i--, order++ ) {
            Object[] row = rows.get(i);
            data.pseudoCells.add(new PseudoCell(order, null, row[4] == null ? null : toInt(row[4]), toDouble(row[1]), ""));
        }

        int n = rows.size() - firstReal;
        data.eta = new double[n];
        data.controlVolume = new double[n];
        data.psiIC = withPsi ? new double[n] : null;
        data.temperatureIC = withTemperature ? new double[n] : null;
        data.parameterID = withParameterID ? new int[n] : null;
        for( int i = 0; i < n; i++ ) {
            Object[] row = rows.get(firstReal + i);
            data.eta[i] = toDouble(row[0]);
            data.controlVolume[i] = toDouble(row[1]);
            if (withPsi) {
                data.psiIC[i] = toDouble(row[2]);
            }
            if (withTemperature) {
                data.temperatureIC[i] = toDouble(row[3]);
            }
            if (withParameterID) {
                data.parameterID[i] = toInt(row[4]);
            }
        }
        data.etaDual = new double[n + 1];
        if (n > 0) {
            data.etaDual[0] = data.eta[0] + data.controlVolume[0] / 2.0;
            for( int i = 0; i < n; i++ ) {
                data.etaDual[i + 1] = data.etaDual[i] - data.controlVolume[i];
            }
        }

        // layers: runs of equal parameter_id
        int start = 0;
        for( int i = 1; i <= n; i++ ) {
            boolean split = i == n || (withParameterID && data.parameterID[i] != data.parameterID[start]);
            if (!split) {
                continue;
            }
            double minDz = Double.POSITIVE_INFINITY;
            double maxDz = 0;
            for( int j = start; j < i; j++ ) {
                minDz = Math.min(minDz, data.controlVolume[j]);
                maxDz = Math.max(maxDz, data.controlVolume[j]);
            }
            data.layers.add(new Layer(data.etaDual[start], data.etaDual[i], i - start, minDz, maxDz, null,
                    withParameterID ? data.parameterID[start] : null, 0));
            start = i;
        }

        if (!withParameterID) {
            data.warnings.add("The output was written without " + Whetgeo1DOutputSchema.COL_PARAMETER_ID
                    + ": layers and soil parameters are unknown, open the input gpkg's " + TABLE_INPUT_GRID
                    + " for them.");
        } else if (db.hasTable(Whetgeo1DOutputSchema.TABLE_OUTPUT_SWRC_PARAMETERS)) {
            loadParameterSets(db, Whetgeo1DOutputSchema.TABLE_OUTPUT_SWRC_PARAMETERS, Whetgeo1DOutputSchema.COL_ID, data);
            // the output snapshot stores the first two retention curve slots under these names
            data.parameterSlots.put(Whetgeo1DOutputSchema.COL_N, "par1");
            data.parameterSlots.put(Whetgeo1DOutputSchema.COL_ALPHA, "par2");
            checkParameterIDs(data);
        }
        return data;
    }

    /** All columns of a parameter table, by set id; NULL and no-data values are kept as null. */
    private static void loadParameterSets( ADb db, String table, String idColumn, GeospaceGridData data )
            throws Exception {
        db.<Void>execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement();
                    IHMResultSet rs = stmt.executeQuery("SELECT * FROM " + table + " ORDER BY " + idColumn)) {
                int columns = rs.getMetaData().getColumnCount();
                List<String> names = new ArrayList<>();
                for( int c = 1; c <= columns; c++ ) {
                    names.add(rs.getMetaData().getColumnName(c));
                }
                while( rs.next() ) {
                    Map<String, Double> set = new LinkedHashMap<>();
                    Integer id = null;
                    for( int c = 1; c <= columns; c++ ) {
                        Object value = rs.getObject(c);
                        String name = names.get(c - 1);
                        if (name.equalsIgnoreCase(idColumn)) {
                            id = value == null ? null : toInt(value);
                            continue;
                        }
                        Double v = value instanceof Number ? ((Number) value).doubleValue() : null;
                        set.put(name, v == null || v == NODATA ? null : v);
                    }
                    if (id != null) {
                        data.parameterSets.put(id, set);
                    }
                }
            }
            return null;
        });
    }

    private static void checkParameterIDs( GeospaceGridData data ) {
        List<Integer> used = new ArrayList<>();
        for( Layer layer : data.layers ) {
            used.add(layer.parameterID);
        }
        for( PseudoCell cell : data.pseudoCells ) {
            used.add(cell.parameterID);
        }
        used.stream().filter(Objects::nonNull).distinct().filter(id -> !data.parameterSets.containsKey(id))
                .forEach(id -> data.warnings.add("Parameter set " + id + " is used by the grid but missing in the parameters table."));
    }

    private static List<Object[]> query( ADb db, String sql ) throws Exception {
        List<Object[]> rows = new ArrayList<>();
        db.<Void>execOnConnection(connection -> {
            try (IHMStatement stmt = connection.createStatement(); IHMResultSet rs = stmt.executeQuery(sql)) {
                int columns = rs.getMetaData().getColumnCount();
                while( rs.next() ) {
                    Object[] row = new Object[columns];
                    for( int c = 1; c <= columns; c++ ) {
                        row[c - 1] = rs.getObject(c);
                    }
                    rows.add(row);
                }
            }
            return null;
        });
        return rows;
    }

    private static boolean hasColumn( ADb db, String table, String column ) throws Exception {
        for( Object[] row : query(db, "PRAGMA table_info(\"" + table + "\")") ) {
            if (column.equalsIgnoreCase(String.valueOf(row[1]))) {
                return true;
            }
        }
        return false;
    }

    private static double toDouble( Object value ) {
        return value == null ? Double.NaN : ((Number) value).doubleValue();
    }

    private static int toInt( Object value ) {
        return value == null ? (int) NODATA : ((Number) value).intValue();
    }

    private static double[] toDoubles( List<Double> list ) {
        return list.stream().mapToDouble(Double::doubleValue).toArray();
    }

    private static int[] toInts( List<Integer> list ) {
        return list.stream().mapToInt(Integer::intValue).toArray();
    }
}
