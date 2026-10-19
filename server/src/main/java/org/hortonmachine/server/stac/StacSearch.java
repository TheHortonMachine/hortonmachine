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
package org.hortonmachine.server.stac;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * A search of the items of the STAC catalog, always limited to the items the requester can see: the shared data and
 * the own area, all of them for the administrators.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class StacSearch {
    /** The area of the user searching, its username. */
    public final String ownArea;
    public final boolean isAdmin;
    /** The collections to search, all if empty. */
    public final Set<String> collections = new LinkedHashSet<>();
    /** The ids of the items, all if empty. */
    public final Set<String> ids = new LinkedHashSet<>();
    /** The bounds the items must intersect, as minLon, minLat, maxLon, maxLat, or <code>null</code>. */
    public double[] bbox;
    /** The time range the items must intersect, in milliseconds, <code>null</code> for open. */
    public Long start;
    public Long end;
    public int limit = 10;
    public int offset = 0;

    public StacSearch( String ownArea, boolean isAdmin ) {
        this.ownArea = ownArea;
        this.isAdmin = isAdmin;
    }
}
