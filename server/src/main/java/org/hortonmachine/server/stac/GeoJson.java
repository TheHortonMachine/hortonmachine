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

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * The footprints of the items as GeoJSON geometries: polygons, or multipolygons for the items with
 * footprints apart.
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class GeoJson {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private GeoJson() {
    }

    /**
     * @param footprint a polygonal geometry, the other parts of a collection are left out.
     * @return the GeoJSON geometry.
     */
    public static ObjectNode toGeoJson( Geometry footprint ) {
        ObjectNode geometry = MAPPER.createObjectNode();
        if (footprint instanceof Polygon) {
            geometry.put("type", "Polygon");
            putRings(geometry.putArray("coordinates"), (Polygon) footprint);
            return geometry;
        }
        geometry.put("type", "MultiPolygon");
        ArrayNode polygons = geometry.putArray("coordinates");
        for( int i = 0; i < footprint.getNumGeometries(); i++ ) {
            Geometry part = footprint.getGeometryN(i);
            if (part instanceof Polygon && !part.isEmpty()) {
                putRings(polygons.addArray(), (Polygon) part);
            }
        }
        return geometry;
    }

    private static void putRings( ArrayNode rings, Polygon polygon ) {
        // exterior counterclockwise, holes clockwise, as RFC 7946 asks
        Polygon oriented = (Polygon) polygon.norm().reverse();
        putRing(rings.addArray(), oriented.getExteriorRing());
        for( int i = 0; i < oriented.getNumInteriorRing(); i++ ) {
            putRing(rings.addArray(), oriented.getInteriorRingN(i));
        }
    }

    private static void putRing( ArrayNode ring, LineString line ) {
        for( Coordinate coordinate : line.getCoordinates() ) {
            ring.addArray().add(round(coordinate.x)).add(round(coordinate.y));
        }
    }

    /**
     * @return the degrees with 7 decimals, about 1 cm.
     */
    private static double round( double degrees ) {
        return Math.round(degrees * 1e7) / 1e7;
    }
}
