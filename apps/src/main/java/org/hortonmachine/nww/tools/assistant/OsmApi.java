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
package org.hortonmachine.nww.tools.assistant;

import java.util.List;
import java.util.function.Supplier;

import org.hortonmachine.gears.io.osm.NominatimClient;
import org.hortonmachine.gears.io.osm.NominatimPlace;
import org.hortonmachine.gears.io.osm.OverpassClient;
import org.hortonmachine.gears.io.osm.OverpassResult;

/**
 * The OpenStreetMap services for the scripts of the viewer, bound as <code>osm</code>.
 *
 * @author Andrea Antonello (https://g-ant.eu)
 */
public class OsmApi {
    private static final int SEARCH_LIMIT = 5;

    private final Supplier<String> overpassUrl;

    /**
     * @param overpassUrl gives the url of the Overpass server chosen in the viewer.
     */
    public OsmApi( Supplier<String> overpassUrl ) {
        this.overpassUrl = overpassUrl;
    }

    /**
     * Run an Overpass QL query on the server chosen in the viewer.
     *
     * @param overpassQl the query, with json output.
     * @return the features of the answer.
     * @throws Exception
     */
    public OverpassResult query( String overpassQl ) throws Exception {
        return new OverpassClient(overpassUrl.get()).query(overpassQl);
    }

    /**
     * Find places by name.
     *
     * @param place the name of the place.
     * @return the places found, the best first.
     * @throws Exception
     */
    public List<NominatimPlace> search( String place ) throws Exception {
        return new NominatimClient().search(place, SEARCH_LIMIT);
    }

    /**
     * @param name the name of the place.
     * @return the best place found for the name.
     * @throws IllegalArgumentException if none is found.
     * @throws Exception
     */
    public NominatimPlace place( String name ) throws Exception {
        List<NominatimPlace> places = search(name);
        if (places.isEmpty()) {
            throw new IllegalArgumentException("No place found for: " + name);
        }
        return places.get(0);
    }

    @Override
    public String toString() {
        return "osm: OpenStreetMap services, Overpass at " + overpassUrl.get();
    }
}
