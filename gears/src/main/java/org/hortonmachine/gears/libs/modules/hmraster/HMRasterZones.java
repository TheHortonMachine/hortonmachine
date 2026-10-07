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
package org.hortonmachine.gears.libs.modules.hmraster;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;

import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.utils.RegionMap;

/**
 * Zones (ex. the polygons of sub-basins) rasterized on a grid: the zone id of each cell.
 *
 * <p>Created by {@link HMRaster#rasterizeZones}, they can be reused to compute the
 * statistics of any raster on the same grid with {@link HMRaster#getZonalStats(
 * org.hortonmachine.gears.libs.monitor.IHMProgressMonitor, HMRasterZones)}, without
 * rasterizing the zones again.</p>
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public final class HMRasterZones {
    /** The id of the cells that are in no zone. */
    public static final int NO_ZONE = -1;

    private final RegionMap region;
    private final int[] cellZones;
    private final SortedSet<Integer> zoneIds;

    /**
     * @param region the grid of the zones.
     * @param cellZones the zone id of each cell, row-major, {@link #NO_ZONE} for the cells in no zone.
     */
    public HMRasterZones( RegionMap region, int[] cellZones ) {
        if (cellZones.length != region.getCols() * region.getRows()) {
            throw new IllegalArgumentException("The cells don't match the size of the grid.");
        }
        this.region = region;
        this.cellZones = cellZones;
        TreeSet<Integer> ids = new TreeSet<>();
        for( int zone : cellZones ) {
            if (zone != NO_ZONE) {
                ids.add(zone);
            }
        }
        zoneIds = Collections.unmodifiableSortedSet(ids);
    }

    /**
     * @return the grid of the zones.
     */
    public RegionMap getRegion() {
        return region;
    }

    /**
     * @param col the col of the zones grid.
     * @param row the row of the zones grid.
     * @return the zone id of the cell, or {@link #NO_ZONE}.
     */
    public int getZone( int col, int row ) {
        return cellZones[row * region.getCols() + col];
    }

    /**
     * @return the ids of the zones that cover at least one cell.
     */
    public SortedSet<Integer> getZoneIds() {
        return zoneIds;
    }
}
