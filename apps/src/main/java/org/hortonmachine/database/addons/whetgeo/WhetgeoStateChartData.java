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
import java.util.List;

/**
 * Holds the data needed to render the WHETGEO 1D state (Hovmoller-style) chart:
 * the static grid depth coordinates, the top/bottom boundary condition forcing
 * timeseries (if the output was written with them), and one depth/time/value
 * series per state variable actually present in the output (theta is always
 * present; water_suction/internal_energy/ice_content only if that column
 * exists — see {@link WhetgeoStateChartDataLoader}).
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WhetgeoStateChartData {
    public double[] gridEta = new double[0];
    /** Per-cell parameter-set label, same order as {@link #gridEta}; empty if the output
     *  wasn't written with {@code parameter_id} (see {@code Whetgeo1DOutputsHandler}). */
    public int[] gridParameterID = new int[0];

    public long[] topBCTimes = new long[0];
    public double[] topBCValues = new double[0];
    /** e.g. "TOP_COUPLED"; null if the output wasn't written with one
     *  (see {@code Whetgeo1DOutputsHandler.TABLE_OUTPUT_METADATA}). */
    public String topBCType;

    public long[] bottomBCTimes = new long[0];
    public double[] bottomBCValues = new double[0];
    public String bottomBCType;

    public List<DepthSeries> depthSeries = new ArrayList<>();

    /** SWRC parameter snapshot per parameter set id, if the output was written with one
     *  (see {@code Whetgeo1DOutputsHandler.TABLE_OUTPUT_SWRC_PARAMETERS}); empty otherwise. */
    public List<SwrcParams> swrcParameters = new ArrayList<>();

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
