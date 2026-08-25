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

import java.awt.Component;
import java.util.List;

import org.hortonmachine.dbs.compat.ADb;

/**
 * Action that loads the {@code station_data} of the first available station
 * and opens it as a chart with one sub-plot per environmental variable
 * (precipitation as a bar chart, everything else as a time series), letting
 * the user then switch which station is shown.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class ErmStationChartAction extends AbstractErmVariableChartAction {
    private static final long serialVersionUID = 1L;

    public ErmStationChartAction( ADb db, Component parent ) {
        super("Open Station Data Chart", db, parent);
    }

    @Override
    protected List<ErmEntityItem> loadEntities() throws Exception {
        return ErmStationChartDataLoader.loadStationsWithData(db);
    }

    @Override
    protected ErmVariableChartData loadData( int entityId ) throws Exception {
        return ErmStationChartDataLoader.load(db, entityId);
    }

    @Override
    protected String entityLabel() {
        return "Station";
    }

    @Override
    protected String dialogTitle() {
        return "Station Data Chart";
    }

    @Override
    protected String loadingText() {
        return "Loading GeoFrame station data chart...";
    }

    @Override
    protected String noDataMessage() {
        return "No station has data in the '" + ErmSchema.STATION_DATA_TABLE + "' table.";
    }
}
