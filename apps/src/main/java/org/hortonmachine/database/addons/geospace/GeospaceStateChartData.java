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
import java.util.List;

/**
 * Holds the data needed to render the GEOSPACE state (Hovmoller-style) chart.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GeospaceStateChartData {
    public double[] gridEta = new double[0];
    /** Per-cell parameter-set label, same order as {@link #gridEta}; empty if the output
     *  wasn't written with {@code parameter_id} (see WHETGEO-1D's own output handler). */
    public int[] gridParameterID = new int[0];

    public long[] topBCTimes = new long[0];
    public double[] topBCValues = new double[0];
    /** e.g. "TOP_COUPLED"; null if the output wasn't written with one
     *  (see {@code Whetgeo1DOutputSchema.TABLE_OUTPUT_METADATA}). */
    public String topBCType;

    public long[] bottomBCTimes = new long[0];
    public double[] bottomBCValues = new double[0];
    public String bottomBCType;

    public List<DepthSeries> depthSeries = new ArrayList<>();

    /** SWRC parameter snapshot per parameter set id, if the output was written with one
     *  (see {@code Whetgeo1DOutputSchema.TABLE_OUTPUT_SWRC_PARAMETERS}); empty otherwise. */
    public List<SwrcParams> swrcParameters = new ArrayList<>();

    /** GEOET's evapotranspiration, if this run coupled through GEOSPACE-1D and wrote {@code
     *  geoframe_geoet_output_results}; null otherwise. Root water uptake (GEOSPACE-1D's own {@code
     *  geoframe_geospace_output_uptake}, if present) is a per-depth series like any other state
     *  variable, so it's folded directly into {@link #depthSeries} rather than kept separately. */
    public EtSeries etSeries;

    /**
     * One state variable's full (timestamp, eta) -&gt; value grid, flattened into
     * three parallel arrays (one triple per row of {@code output_state}).
     */
    public static class DepthSeries {
        public final String name;
        public final String axisLabel;
        public long[] times = new long[0];
        public double[] eta = new double[0];
        public double[] values = new double[0];

        public DepthSeries( String name, String axisLabel ) {
            this.name = name;
            this.axisLabel = axisLabel;
        }
    }

    /**
     * GEOET's evapotranspiration timeseries out of {@code geoframe_geoet_output_results}: the
     * combined {@code evapo_transpiration} column is always populated when the table exists, since
     * every ET model writes it; {@code evaporation}/{@code transpiration} are only non-empty for
     * models that actually split the total (e.g. Penman-Monteith, Prospero) - both stay empty for
     * a model that only ever produces the combined figure (e.g. Priestley-Taylor).
     */
    public static class EtSeries {
        public long[] times = new long[0];
        public double[] evapoTranspiration = new double[0];
        public double[] evaporation = new double[0];
        public double[] transpiration = new double[0];
    }

    /** One row of {@code output_swrc_parameters}: the soil properties for one parameter set. */
    public static class SwrcParams {
        public final int id;
        public final double thetaS;
        public final double thetaR;
        public final double ks;
        public final double n;
        public final double alpha;

        public SwrcParams( int id, double thetaS, double thetaR, double ks, double n, double alpha ) {
            this.id = id;
            this.thetaS = thetaS;
            this.thetaR = thetaR;
            this.ks = ks;
            this.n = n;
            this.alpha = alpha;
        }
    }
}
