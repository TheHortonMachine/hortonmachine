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

import java.util.List;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * What the STAC items of the catalog have in common.
 *
 * <p>Developed with the assistance of an AI coding agent;
 * design, review and validation by the author.</p>
 *
 * @author Andrea Antonello - https://g-ant.eu
 */
public class StacItems {
    public static final String STAC_VERSION = "1.1.0";
    /** The extensions the items use. */
    public static final List<String> EXTENSIONS = List.of( //
            "https://stac-extensions.github.io/projection/v2.0.0/schema.json", //
            "https://stac-extensions.github.io/raster/v1.1.0/schema.json", //
            "https://stac-extensions.github.io/file/v2.1.0/schema.json", //
            "https://stac-extensions.github.io/timestamps/v1.1.0/schema.json");

    private StacItems() {
    }

    /**
     * Put the geometry and the bbox of an item, both null if it has no footprint.
     */
    static void putGeometry( ObjectNode item, Geometry footprint ) {
        if (footprint == null || footprint.isEmpty()) {
            item.putNull("geometry");
            return;
        }
        item.set("geometry", GeoJson.toGeoJson(footprint));
        Envelope envelope = footprint.getEnvelopeInternal();
        item.putArray("bbox").add(envelope.getMinX()).add(envelope.getMinY()).add(envelope.getMaxX()).add(envelope.getMaxY());
    }
}
