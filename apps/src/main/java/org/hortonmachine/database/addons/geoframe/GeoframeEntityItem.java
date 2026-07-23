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
 * A selectable entity (a station or a basin) shown in the variable chart's picker combo box,
 * carrying its numeric id and a display label.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class GeoframeEntityItem {
    public final int id;
    public final String label;

    public GeoframeEntityItem( int id, String label ) {
        this.id = id;
        this.label = label;
    }

    @Override
    public String toString() {
        return label;
    }
}
