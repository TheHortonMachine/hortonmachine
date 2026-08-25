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
package org.hortonmachine.database.addons.erm;

import java.awt.Component;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;

import org.hortonmachine.database.spi.IDbViewerActionProvider;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.objects.TableLevel;

/**
 * Contributes the GeoFrame ERM (water budget) simulation chart actions
 * (per-run discharge, station data, basin data) to the database viewer's
 * table context menu, for databases recognized as GeoFrame ERM simulation
 * databases (see {@link ErmSchema}). The WHETGEO-1D state chart action is a
 * separate concern, see
 * {@code org.hortonmachine.database.addons.whetgeo.WhetgeoActionProvider}.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ErmActionProvider implements IDbViewerActionProvider {

    @Override
    public boolean supportsDatabase( ADb db ) {
        try {
            return db.hasTable(ErmSchema.TOPOLOGY_TABLE) || db.hasTable(ErmSchema.STATION_DATA_TABLE)
                    || db.hasTable(ErmSchema.BASIN_DATA_TABLE);
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public List<Action> getTableActions( ADb db, TableLevel tableLevel, Component parent ) {
        String tableName = tableLevel.tableName.getName();
        try {
            if (ErmSchema.isSimulationDischargeTable(tableName)) {
                return Collections.singletonList(new ErmChartAction(db, tableName, parent));
            }
            if (ErmSchema.STATION_DATA_TABLE.equals(tableName) && db.hasTable(ErmSchema.STATION_TABLE)) {
                return Collections.singletonList(new ErmStationChartAction(db, parent));
            }
            if (ErmSchema.BASIN_DATA_TABLE.equals(tableName) && db.hasTable(ErmSchema.BASIN_TABLE)) {
                return Collections.singletonList(new ErmBasinChartAction(db, parent));
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new ArrayList<>();
    }
}
