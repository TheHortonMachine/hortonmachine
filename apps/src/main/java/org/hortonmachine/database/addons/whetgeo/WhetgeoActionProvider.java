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

import java.awt.Component;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;

import org.hortonmachine.database.spi.IDbViewerActionProvider;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.gears.io.geoframe.whetgeo.Whetgeo1DOutputSchema;

/**
 * Contributes the WHETGEO-1D state chart action to the database viewer's
 * table context menu, for databases recognized as WHETGEO-1D output
 * GeoPackages (see {@link Whetgeo1DOutputSchema}). The GeoFrame ERM
 * simulation chart actions are a separate concern, see
 * {@code org.hortonmachine.database.addons.erm.ErmActionProvider}.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class WhetgeoActionProvider implements IDbViewerActionProvider {

    @Override
    public boolean supportsDatabase( ADb db ) {
        try {
            return db.hasTable(Whetgeo1DOutputSchema.TABLE_OUTPUT_STATE) && db.hasTable(Whetgeo1DOutputSchema.TABLE_OUTPUT_GRID);
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public List<Action> getTableActions( ADb db, TableLevel tableLevel, Component parent ) {
        if (!Whetgeo1DOutputSchema.TABLE_OUTPUT_STATE.equals(tableLevel.tableName.getName())) {
            return Collections.emptyList();
        }
        return Collections.singletonList(new WhetgeoStateChartAction(db, parent));
    }
}
