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

/**
 * Holds the time series data needed to render the GeoFrame water budget chart:
 * precipitation and temperature for the most downstream basin, plus its
 * simulated and observed discharge.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeChartData {
    public int basinId;

    public long[] precipitationTimes = new long[0];
    public double[] precipitationValues = new double[0];

    public long[] temperatureTimes = new long[0];
    public double[] temperatureValues = new double[0];

    public long[] simulatedDischargeTimes = new long[0];
    public double[] simulatedDischargeValues = new double[0];

    public long[] observedDischargeTimes = new long[0];
    public double[] observedDischargeValues = new double[0];
}
