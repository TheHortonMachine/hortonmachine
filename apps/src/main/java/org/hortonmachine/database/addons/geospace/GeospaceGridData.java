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

/**
 * The geometry and the parameters of a WHETGEO 1D column, read either from an
 * input gpkg ({@code grid_geometry} and friends) or from an output gpkg
 * ({@code geoframe_whetgeo1d_output_grid}), in a form the grid view can draw.
 *
 * <p>
 * Cell arrays are surface first and hold only the real (thick) cells; the
 * zero-thickness pseudo cells stacked on the surface are in {@link #pseudoCells}.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GeospaceGridData {

    /** Where the data came from, ex. "input grid_geometry". */
    public String source = "";

    /** Cell centre elevation [m], surface first. */
    public double[] eta = new double[0];
    /** Cell thickness [m], same order as {@link #eta}. */
    public double[] controlVolume = new double[0];
    /** Initial water suction [m] per cell, or null if unknown. */
    public double[] psiIC;
    /** Initial temperature [K] per cell, or null if unknown. */
    public double[] temperatureIC;
    /** Parameter set id per cell, or null if unknown. */
    public int[] parameterID;
    /** Equation state id per cell, or null if unknown. */
    public int[] equationStateID;

    /** Cell interfaces [m], surface first, one more than the cells. */
    public double[] etaDual = new double[0];

    public List<Layer> layers = new ArrayList<>();
    public List<PseudoCell> pseudoCells = new ArrayList<>();

    /** Parameter sets by id, each a column name to value map, in table column order. */
    public Map<Integer, Map<String, Double>> parameterSets = new LinkedHashMap<>();
    /** Retention curve slot of a parameter column, ex. alpha -&gt; par2 (input only). */
    public Map<String, String> parameterSlots = new LinkedHashMap<>();

    /** The raw initial condition points (eta, psi0, t0), surface first (input only). */
    public List<double[]> icPoints = new ArrayList<>();

    /** Things worth telling the user about the setup, ex. ignored rows. */
    public List<String> warnings = new ArrayList<>();

    /** A run of cells with the same parameters, ex. one row of {@code grid_geometry}. */
    public static class Layer {
        public final double topEta;
        public final double bottomEta;
        public final int cells;
        /** Smallest and largest cell thickness in the layer [m]. */
        public final double minDz;
        public final double maxDz;
        /** Equation state id, or null if unknown. */
        public final Integer equationStateID;
        /** Parameter set id, or null if unknown. */
        public final Integer parameterID;
        /** How many {@code grid_geometry} rows were merged into this layer (0 for an output grid). */
        public final int sourceRows;

        public Layer( double topEta, double bottomEta, int cells, double minDz, double maxDz, Integer equationStateID,
                Integer parameterID, int sourceRows ) {
            this.topEta = topEta;
            this.bottomEta = bottomEta;
            this.cells = cells;
            this.minDz = minDz;
            this.maxDz = maxDz;
            this.equationStateID = equationStateID;
            this.parameterID = parameterID;
            this.sourceRows = sourceRows;
        }

        /** @return true if the next layer below has the same ids and can be shown as one with this. */
        public boolean sameKindAs( Layer other ) {
            return java.util.Objects.equals(equationStateID, other.equationStateID)
                    && java.util.Objects.equals(parameterID, other.parameterID);
        }

        /** @return this layer extended down over the given one, right below it. */
        public Layer mergedWith( Layer below ) {
            return new Layer(topEta, below.bottomEta, cells + below.cells, Math.min(minDz, below.minDz),
                    Math.max(maxDz, below.maxDz), equationStateID, parameterID, sourceRows + below.sourceRows);
        }
    }

    /** A zero-thickness control volume stacked on the surface, ex. a ponding reservoir. */
    public static class PseudoCell {
        public final int stackOrder;
        public final Integer equationStateID;
        public final Integer parameterID;
        public final double controlVolume;
        public final String note;

        public PseudoCell( int stackOrder, Integer equationStateID, Integer parameterID, double controlVolume,
                String note ) {
            this.stackOrder = stackOrder;
            this.equationStateID = equationStateID;
            this.parameterID = parameterID;
            this.controlVolume = controlVolume;
            this.note = note;
        }
    }

    public int cellCount() {
        return eta.length;
    }

    public double topEta() {
        return etaDual.length > 0 ? etaDual[0] : 0;
    }

    public double bottomEta() {
        return etaDual.length > 0 ? etaDual[etaDual.length - 1] : 0;
    }
}
