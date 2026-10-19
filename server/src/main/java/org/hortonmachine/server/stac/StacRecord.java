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

/**
 * An item of the STAC catalog as kept in the index of the database, with the columns the searches use.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class StacRecord {
    /** The id of the item, unique in its collection. */
    public final String id;
    public final String collection;
    /** The area of the user that can see the item, its username, <code>null</code> for the items all users see. */
    public final String ownerId;
    /** The path in the workspace of the main file of the item, as data/flanginec/dem/dtm.tif, unique in the catalog. */
    public final String path;
    /** The size and the time of the last change of the files when the item was made, to see if it is still current. */
    public final long size;
    public final long modified;
    /** The bounds in WGS84, all <code>null</code> for the items without geometry. */
    public final Double minLon;
    public final Double minLat;
    public final Double maxLon;
    public final Double maxLat;
    /** The time range of the item, in milliseconds since the epoch. */
    public final Long startTime;
    public final Long endTime;
    /** The STAC item, as JSON, with the hrefs of the assets relative to the path. */
    public final String json;

    public StacRecord( String id, String collection, String ownerId, String path, long size, long modified, double[] bbox,
            Long startTime, Long endTime, String json ) {
        this.id = id;
        this.collection = collection;
        this.ownerId = ownerId;
        this.path = path;
        this.size = size;
        this.modified = modified;
        this.minLon = bbox != null ? bbox[0] : null;
        this.minLat = bbox != null ? bbox[1] : null;
        this.maxLon = bbox != null ? bbox[2] : null;
        this.maxLat = bbox != null ? bbox[3] : null;
        this.startTime = startTime;
        this.endTime = endTime;
        this.json = json;
    }

    /**
     * @return the bounds as minLon, minLat, maxLon, maxLat, or <code>null</code>.
     */
    public double[] getBbox() {
        return minLon != null ? new double[]{minLon, minLat, maxLon, maxLat} : null;
    }
}
