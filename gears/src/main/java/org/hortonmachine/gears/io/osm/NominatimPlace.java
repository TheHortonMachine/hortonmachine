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
package org.hortonmachine.gears.io.osm;

import org.locationtech.jts.geom.Envelope;

/**
 * A place found by the {@link NominatimClient}.
 *
 * @param displayName the full name of the place, as "Roma, Lazio, Italia".
 * @param osmType the type of the OpenStreetMap element: node, way or relation.
 * @param osmId the id of the OpenStreetMap element.
 * @param category the category of the place, as boundary or place.
 * @param type the type of the place in its category, as administrative or city.
 * @param lon the longitude of the place.
 * @param lat the latitude of the place.
 * @param bbox the bounding box of the place in longitude/latitude, or <code>null</code>.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public record NominatimPlace( String displayName, String osmType, long osmId, String category, String type, double lon,
        double lat, Envelope bbox ) {

    /** The offset of the ids of the Overpass areas made from relations. */
    public static final long OVERPASS_RELATION_AREA_OFFSET = 3_600_000_000L;
    /** The offset of the ids of the Overpass areas made from closed ways. */
    public static final long OVERPASS_WAY_AREA_OFFSET = 2_400_000_000L;

    /**
     * @return the id of the place as an Overpass area, to use in queries as
     *          <code>area(id)-&gt;.a;</code>, or -1 for a node, which is no area.
     */
    public long overpassAreaId() {
        if ("relation".equals(osmType)) {
            return OVERPASS_RELATION_AREA_OFFSET + osmId;
        }
        if ("way".equals(osmType)) {
            return OVERPASS_WAY_AREA_OFFSET + osmId;
        }
        return -1;
    }
}
