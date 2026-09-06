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

import java.awt.Component;
import java.util.Collections;
import java.util.List;

import javax.swing.Action;
import javax.swing.Icon;

import org.hortonmachine.database.spi.IDbViewerActionProvider;
import org.hortonmachine.dbs.compat.ADb;
import org.hortonmachine.dbs.compat.objects.TableLevel;
import org.hortonmachine.gears.io.geoframe.whetgeo.Whetgeo1DOutputSchema;
import org.hortonmachine.gui.utils.ImageCache;

/**
 * Contributes the GEOSPACE 1D state chart action to the database viewer's
 * table context menu, for databases recognized as GEOSPACE output
 * GeoPackages.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class GeospaceActionProvider implements IDbViewerActionProvider {

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
        return Collections.singletonList(new GeospaceStateChartAction(db, parent));
    }

    @Override
    public Icon getIcon() {
        return ImageCache.get(ImageCache.GEOFRAME32);
    }
}
