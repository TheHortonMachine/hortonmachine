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

import java.awt.Component;
import java.util.List;

import javax.swing.Action;

import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.objects.TableLevel;

/**
 * Action that loads the {@code station_data} of the first available station
 * and opens it as a chart with one sub-plot per environmental variable
 * (precipitation as a bar chart, everything else as a time series), letting
 * the user then switch which station is shown.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeStationChartAction extends AbstractGeoframeVariableChartAction {
    private static final long serialVersionUID = 1L;

    public GeoframeStationChartAction( ADb db, Component parent ) {
        super("Open Station Data Chart", db, parent);
    }

    /**
     * Recognizes whether {@code selectedTable} is the GeoFrame station data table and, if so,
     * appends a separator and a {@link GeoframeStationChartAction} to {@code actions}.
     */
    public static void addIfApplicable( ADb db, TableLevel selectedTable, List<Action> actions, Component parent ) {
        if (!GeoframeSchema.STATION_DATA_TABLE.equals(selectedTable.tableName.getName())) {
            return;
        }
        try {
            if (db.hasTable(GeoframeSchema.STATION_TABLE)) {
                actions.add(null); // separator
                actions.add(new GeoframeStationChartAction(db, parent));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    @Override
    protected List<GeoframeEntityItem> loadEntities() throws Exception {
        return GeoframeStationChartDataLoader.loadStationsWithData(db);
    }

    @Override
    protected GeoframeVariableChartData loadData( int entityId ) throws Exception {
        return GeoframeStationChartDataLoader.load(db, entityId);
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
        return "No station has data in the '" + GeoframeSchema.STATION_DATA_TABLE + "' table.";
    }
}
