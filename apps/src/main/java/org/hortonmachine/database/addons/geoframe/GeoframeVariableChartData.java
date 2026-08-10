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

/**
 * Holds the time series data of a single entity's (station or basin) data rows, split by
 * environmental variable (var_id), for the variable chart shared by the station data and basin
 * data chart actions.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeVariableChartData {
    public int entityId;
    /** Chart title, eg. "Station 3" or "Basin 5". */
    public String title;

    public List<VariableSeries> variableSeries = new ArrayList<>();

    /** One time series per environmental variable (var_id) found for the entity. */
    public static class VariableSeries {
        public int varId;
        public String name;
        public String unit;
        public long[] times = new long[0];
        public double[] values = new double[0];
    }
}
