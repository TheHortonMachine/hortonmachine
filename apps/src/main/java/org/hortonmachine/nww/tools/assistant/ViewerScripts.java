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

import java.util.LinkedHashMap;
import java.util.Map;

import org.hortonmachine.geoscript.console.ConsoleExamples;

/**
 * What the script assistant of the viewer is told: the imports, the objects bound for the
 * scripts and the example scripts.
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class ViewerScripts {

    /** The name of the {@link ViewerApi} in the scripts. */
    public static final String VIEWER_VARIABLE = "viewer";
    /** The name of the {@link OsmApi} in the scripts. */
    public static final String OSM_VARIABLE = "osm";

    /**
     * The imports the scripts of the viewer usually start with.
     */
    public static final String IMPORTS = ConsoleExamples.HM_IMPORTS
            + "import org.hortonmachine.gears.io.osm.* // OpenStreetMap: OverpassResult, NominatimPlace\n";

    /**
     * Where the scripts run and the objects bound for them, the same for every request.
     */
    public static final String HOST_REFERENCE = """
            The scripts run in the HortonMachine 3D viewer, not in the plain console: what they load is shown
            on the globe. Two objects are ready for them, never create or define them:

            viewer, the map:
            	void addLayer( SimpleFeatureCollection features, String name ) - show features as a layer, in any CRS
            	void addResult( OverpassResult result, String name ) - show the polygons, lines and points of an
            	    Overpass result, one layer each
            	void zoomTo( SimpleFeatureCollection features ), zoomTo( OverpassResult result ),
            	    zoomTo( ReferencedEnvelope envelope ), zoomTo( Envelope lonLatEnvelope )
            	ReferencedEnvelope visibleArea() - the area shown, in lon/lat, null when the whole globe is visible
            	String overpassBbox() - the area shown as an Overpass bbox "(south,west,north,east)"
            	void loadFile( String path ) - load a file as a layer: shapefile, geopackage, geotiff, las...

            osm, the OpenStreetMap services:
            	OverpassResult query( String overpassQl ) - run an Overpass QL query on the server chosen in the viewer
            	List<NominatimPlace> search( String place ) - find places by name, best first
            	NominatimPlace place( String name ) - the best place for a name, an error if there is none

            OverpassResult: getPoints(), getLines(), getPolygons() - SimpleFeatureCollections in lon/lat with
            the attributes osm_type, osm_id and the tags as strings; getWarnings() - List<String>; isEmpty()
            NominatimPlace (a record): displayName(), osmType(), osmId(), category(), type(), lon(), lat(),
            bbox() - an Envelope in lon/lat, overpassAreaId()

            Overpass QL:
            - start with [out:json][timeout:90]; the answer must be json
            - end with out geom; so that the ways and relations have their geometry
            - write the query in a triple quoted GString, since it has double quotes
            - inside a place: area(${osm.place("Roma").overpassAreaId()})->.a; then add (area.a) to the filters
            - in the visible area: add ${viewer.overpassBbox()} to the filters, as node["amenity"="bench"]${viewer.overpassBbox()};
            - administrative boundaries are relations with boundary=administrative and an admin_level that
              depends on the country: in Italy 8 is the comune, 9 to 11 the parts of the large cities (the 15
              municipi of Rome are admin_level 10); when unsure try the next level
            - villages and fractions (frazioni) are mostly place=village, hamlet or suburb nodes: points, not areas
            - ask for small areas: the public servers are shared, slow and refuse too big queries
            - print the warnings of the result and tell when it is empty
            """;

    private ViewerScripts() {
    }

    /**
     * @return the example scripts of the viewer, by title, the most generic first.
     */
    public static Map<String, String> examples() {
        Map<String, String> examples = new LinkedHashMap<>();
        examples.put("Load the administrative subdivisions of a city from OpenStreetMap, as the municipi of Rome", """
                import org.hortonmachine.gears.io.osm.*

                def place = osm.place("Roma")
                println place.displayName()
                def query = \"""[out:json][timeout:90];
                area(${place.overpassAreaId()})->.city;
                relation["boundary"="administrative"]["admin_level"="10"](area.city);
                out geom;\"""
                def result = osm.query(query)
                result.warnings.each { println it }
                if (result.polygons.isEmpty()) {
                    println "No subdivisions with admin_level 10, try 9 or 11"
                } else {
                    viewer.addResult(result, "Municipi of Roma")
                    viewer.zoomTo(result)
                    println "Loaded ${result.polygons.size()} subdivisions"
                }
                """);
        examples.put("Load OpenStreetMap points of interest in the visible area of the map, as drinking water fountains", """
                import org.hortonmachine.gears.io.osm.*

                def query = \"""[out:json][timeout:90];
                node["amenity"="drinking_water"]${viewer.overpassBbox()};
                out geom;\"""
                def result = osm.query(query)
                result.warnings.each { println it }
                if (result.isEmpty()) {
                    println "No drinking water in the visible area"
                } else {
                    viewer.addResult(result, "Drinking water")
                    println "Loaded ${result.points.size()} fountains"
                }
                """);
        examples.put("Load the buildings and roads of a place from OpenStreetMap", """
                import org.hortonmachine.gears.io.osm.*

                def place = osm.place("Bellagio, Como")
                def bb = place.bbox()
                def bbox = "(${bb.minY},${bb.minX},${bb.maxY},${bb.maxX})"
                def query = \"""[out:json][timeout:90];
                (
                  way["building"]${bbox};
                  way["highway"]${bbox};
                );
                out geom;\"""
                def result = osm.query(query)
                result.warnings.each { println it }
                // the buildings are polygons, the roads lines
                viewer.addResult(result, "Bellagio")
                viewer.zoomTo(result)
                println "Buildings: ${result.polygons.size()}, roads: ${result.lines.size()}"
                """);
        examples.put("Zoom the map to a place by name", """
                import org.hortonmachine.gears.io.osm.*

                osm.search("Trento").each { println it.displayName() }
                def place = osm.place("Trento")
                viewer.zoomTo(place.bbox())
                """);
        return examples;
    }
}
