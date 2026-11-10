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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hortonmachine.gears.utils.geometry.EPrecision;
import org.hortonmachine.gears.utils.geometry.HMPrecision;
import org.json.JSONArray;
import org.json.JSONObject;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.linemerge.LineMerger;

/**
 * The features read from an answer of the Overpass API, in longitude/latitude.
 *
 * <ul>
 * <li>the points are the nodes with tags;</li>
 * <li>the lines are the open ways with tags, and the closed ways that are no areas, as
 * roundabouts;</li>
 * <li>the polygons are the closed ways that are areas, as buildings, and the relations of type
 * multipolygon and boundary, whose outer rings, without the inner ones, are assembled from
 * their member ways.</li>
 * </ul>
 *
 * <p>The ways and nodes without tags only give their geometry to the others. The attributes are
 * <code>osm_type</code>, <code>osm_id</code> and the tags found, as strings: the
 * {@value #MAX_TAG_COLUMNS} most frequent of each geometry type, while the others go into
 * <code>other_tags</code>, as <code>key=value;key=value</code>.</p>
 *
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class OverpassResult {
    /** The maximum number of tags that get their own column. */
    public static final int MAX_TAG_COLUMNS = 50;

    public static final String GEOMETRY_FIELD = "the_geom";
    public static final String OSM_TYPE_FIELD = "osm_type";
    public static final String OSM_ID_FIELD = "osm_id";
    public static final String OTHER_TAGS_FIELD = "other_tags";

    /**
     * The keys of the tags that make a closed way an area.
     */
    private static final Set<String> AREA_KEYS = Set.of("building", "landuse", "leisure", "amenity", "boundary", "place",
            "water", "aeroway", "man_made", "shop", "tourism", "historic", "military", "office", "craft", "parking",
            "building:part", "area:highway", "landcover", "wetland", "golf", "public_transport");
    /**
     * The values of natural that are lines also when closed.
     */
    private static final Set<String> NATURAL_LINES = Set.of("coastline", "cliff", "ridge", "arete", "tree_row");

    private static final GeometryFactory GF = new GeometryFactory();

    private final SimpleFeatureCollection points;
    private final SimpleFeatureCollection lines;
    private final SimpleFeatureCollection polygons;
    private final List<String> warnings;
    private final String remark;

    private OverpassResult( SimpleFeatureCollection points, SimpleFeatureCollection lines, SimpleFeatureCollection polygons,
            List<String> warnings, String remark ) {
        this.points = points;
        this.lines = lines;
        this.polygons = polygons;
        this.warnings = Collections.unmodifiableList(warnings);
        this.remark = remark;
    }

    /**
     * @return the nodes with tags.
     */
    public SimpleFeatureCollection getPoints() {
        return points;
    }

    /**
     * @return the ways with tags that are no areas.
     */
    public SimpleFeatureCollection getLines() {
        return lines;
    }

    /**
     * @return the areas, as multipolygons.
     */
    public SimpleFeatureCollection getPolygons() {
        return polygons;
    }

    /**
     * @return what could not be read, as the relations of other types or with broken rings, and
     *          the remark of the server.
     */
    public List<String> getWarnings() {
        return warnings;
    }

    /**
     * @return the remark of the server, as the message of a query stopped by a timeout, or
     *          <code>null</code>.
     */
    public String getRemark() {
        return remark;
    }

    /**
     * @return <code>true</code> if no features were read.
     */
    public boolean isEmpty() {
        return points.isEmpty() && lines.isEmpty() && polygons.isEmpty();
    }

    /**
     * Read the features of an answer of the Overpass API.
     *
     * @param json the answer, in json.
     * @return the features.
     */
    public static OverpassResult parse( String json ) {
        JSONObject root = new JSONObject(json);
        JSONArray elements = root.optJSONArray("elements");
        if (elements == null) {
            elements = new JSONArray();
        }
        List<String> warnings = new ArrayList<>();
        String remark = root.has("remark") ? root.optString("remark") : null;
        if (remark != null) {
            warnings.add("Overpass: " + remark);
        }

        // the geometries of all nodes and ways, also of the ones without tags
        Map<Long, Coordinate> nodeCoordinates = new HashMap<>();
        Map<Long, JSONObject> waysById = new HashMap<>();
        for( int i = 0; i < elements.length(); i++ ) {
            JSONObject element = elements.getJSONObject(i);
            String type = element.optString("type");
            if ("node".equals(type) && element.has("lat")) {
                nodeCoordinates.put(element.getLong("id"), new Coordinate(element.getDouble("lon"), element.getDouble("lat")));
            } else if ("way".equals(type)) {
                waysById.put(element.getLong("id"), element);
            }
        }

        List<OsmFeature> pointFeatures = new ArrayList<>();
        List<OsmFeature> lineFeatures = new ArrayList<>();
        List<OsmFeature> polygonFeatures = new ArrayList<>();
        for( int i = 0; i < elements.length(); i++ ) {
            JSONObject element = elements.getJSONObject(i);
            Map<String, String> tags = readTags(element);
            if (tags.isEmpty()) {
                continue;
            }
            String type = element.optString("type");
            long id = element.getLong("id");
            switch( type ) {
            case "node":
                Coordinate coordinate = nodeCoordinates.get(id);
                if (coordinate != null) {
                    pointFeatures.add(new OsmFeature(type, id, GF.createPoint(coordinate), tags));
                }
                break;
            case "way":
                Coordinate[] coordinates = wayCoordinates(element, nodeCoordinates);
                if (coordinates == null || coordinates.length < 2) {
                    warnings.add("way " + id + ": incomplete geometry, skipped");
                } else if (coordinates.length >= 4 && coordinates[0].equals2D(coordinates[coordinates.length - 1])
                        && isArea(tags)) {
                    Polygon polygon = GF.createPolygon(coordinates);
                    polygonFeatures.add(new OsmFeature(type, id, GF.createMultiPolygon(new Polygon[]{polygon}), tags));
                } else {
                    lineFeatures.add(new OsmFeature(type, id, GF.createLineString(coordinates), tags));
                }
                break;
            case "relation":
                String relationType = tags.getOrDefault("type", "");
                if (!relationType.equals("multipolygon") && !relationType.equals("boundary")) {
                    warnings.add("relation " + id + ": type '" + relationType + "' not read, only multipolygon and boundary are");
                    break;
                }
                MultiPolygon multiPolygon = assembleRelation(element, id, waysById, nodeCoordinates, warnings);
                if (multiPolygon != null) {
                    polygonFeatures.add(new OsmFeature(type, id, multiPolygon, tags));
                }
                break;
            default:
                break;
            }
        }

        return new OverpassResult(toFeatureCollection("osm_points", Point.class, pointFeatures),
                toFeatureCollection("osm_lines", LineString.class, lineFeatures),
                toFeatureCollection("osm_polygons", MultiPolygon.class, polygonFeatures), warnings, remark);
    }

    private static Map<String, String> readTags( JSONObject element ) {
        JSONObject tagsObject = element.optJSONObject("tags");
        if (tagsObject == null) {
            return Collections.emptyMap();
        }
        Map<String, String> tags = new LinkedHashMap<>();
        for( String key : tagsObject.keySet() ) {
            tags.put(key, tagsObject.optString(key));
        }
        return tags;
    }

    /**
     * @return the coordinates of a way, from its geometry (out geom) or from its nodes, or
     *          <code>null</code> if some are missing.
     */
    private static Coordinate[] wayCoordinates( JSONObject way, Map<Long, Coordinate> nodeCoordinates ) {
        JSONArray geometry = way.optJSONArray("geometry");
        if (geometry != null) {
            return geometryCoordinates(geometry);
        }
        JSONArray nodes = way.optJSONArray("nodes");
        if (nodes == null) {
            return null;
        }
        Coordinate[] coordinates = new Coordinate[nodes.length()];
        for( int i = 0; i < nodes.length(); i++ ) {
            Coordinate coordinate = nodeCoordinates.get(nodes.getLong(i));
            if (coordinate == null) {
                return null;
            }
            coordinates[i] = new Coordinate(coordinate);
        }
        return coordinates;
    }

    /**
     * @return the coordinates of a geometry array of points with lat and lon, or
     *          <code>null</code> if some are missing, as outside of the bounds of the output.
     */
    private static Coordinate[] geometryCoordinates( JSONArray geometry ) {
        Coordinate[] coordinates = new Coordinate[geometry.length()];
        for( int i = 0; i < geometry.length(); i++ ) {
            JSONObject point = geometry.optJSONObject(i);
            if (point == null || !point.has("lat")) {
                return null;
            }
            coordinates[i] = new Coordinate(point.getDouble("lon"), point.getDouble("lat"));
        }
        return coordinates;
    }

    private static boolean isArea( Map<String, String> tags ) {
        String area = tags.get("area");
        if ("yes".equals(area)) {
            return true;
        }
        if ("no".equals(area)) {
            return false;
        }
        String natural = tags.get("natural");
        if (natural != null && !NATURAL_LINES.contains(natural)) {
            return true;
        }
        if ("riverbank".equals(tags.get("waterway"))) {
            return true;
        }
        for( String key : tags.keySet() ) {
            if (AREA_KEYS.contains(key)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Assemble the polygons of a multipolygon or boundary relation: the rings are merged from the
     * member ways, the outer ones are united and the inner ones are cut out of them.
     *
     * @return the polygons, or <code>null</code> if there is no closed outer ring.
     */
    private static MultiPolygon assembleRelation( JSONObject relation, long id, Map<Long, JSONObject> waysById,
            Map<Long, Coordinate> nodeCoordinates, List<String> warnings ) {
        JSONArray members = relation.optJSONArray("members");
        if (members == null) {
            warnings.add("relation " + id + ": no members, skipped");
            return null;
        }
        LineMerger outerMerger = new LineMerger();
        LineMerger innerMerger = new LineMerger();
        int incompleteWays = 0;
        for( int i = 0; i < members.length(); i++ ) {
            JSONObject member = members.getJSONObject(i);
            if (!"way".equals(member.optString("type"))) {
                // as the admin_centre and label nodes of the boundaries
                continue;
            }
            Coordinate[] coordinates = null;
            JSONArray geometry = member.optJSONArray("geometry");
            if (geometry != null) {
                coordinates = geometryCoordinates(geometry);
            } else {
                JSONObject way = waysById.get(member.getLong("ref"));
                if (way != null) {
                    coordinates = wayCoordinates(way, nodeCoordinates);
                }
            }
            if (coordinates == null || coordinates.length < 2) {
                incompleteWays++;
                continue;
            }
            String role = member.optString("role", "");
            if (role.equals("inner")) {
                innerMerger.add(GF.createLineString(coordinates));
            } else if (role.equals("outer") || role.isEmpty()) {
                outerMerger.add(GF.createLineString(coordinates));
            }
            // other roles, as subarea, are no part of the area
        }
        if (incompleteWays > 0) {
            warnings.add("relation " + id + ": " + incompleteWays + " member ways without geometry");
        }

        List<Polygon> outers = closedRings(outerMerger, id, "outer", warnings);
        if (outers.isEmpty()) {
            warnings.add("relation " + id + ": no closed outer ring, skipped");
            return null;
        }
        List<Polygon> inners = closedRings(innerMerger, id, "inner", warnings);

        HMPrecision precision = new HMPrecision(EPrecision.MM, DefaultGeographicCRS.WGS84);
        try {
            Geometry area = precision.union(outers);
            if (!inners.isEmpty()) {
                area = precision.difference(area, precision.union(inners));
            }
            return toMultiPolygon(area);
        } catch (Exception e) {
            warnings.add("relation " + id + ": the rings could not be assembled (" + e.getMessage() + "), skipped");
            return null;
        }
    }

    /**
     * @return the polygons of the closed rings merged from the lines, telling of the open ones.
     */
    @SuppressWarnings("unchecked")
    private static List<Polygon> closedRings( LineMerger merger, long id, String role, List<String> warnings ) {
        List<Polygon> polygons = new ArrayList<>();
        int open = 0;
        for( LineString line : (Collection<LineString>) merger.getMergedLineStrings() ) {
            if (line.isClosed() && line.getNumPoints() >= 4) {
                polygons.add(GF.createPolygon(line.getCoordinates()));
            } else {
                open++;
            }
        }
        if (open > 0) {
            warnings.add("relation " + id + ": " + open + " " + role + " rings not closed, left out");
        }
        return polygons;
    }

    private static MultiPolygon toMultiPolygon( Geometry geometry ) {
        if (geometry instanceof MultiPolygon) {
            return (MultiPolygon) geometry;
        }
        List<Polygon> polygons = new ArrayList<>();
        for( int i = 0; i < geometry.getNumGeometries(); i++ ) {
            Geometry part = geometry.getGeometryN(i);
            if (part instanceof Polygon && !part.isEmpty()) {
                polygons.add((Polygon) part);
            }
        }
        return GF.createMultiPolygon(polygons.toArray(new Polygon[0]));
    }

    /**
     * Create the features of a geometry type, with the most frequent tags as columns.
     */
    private static SimpleFeatureCollection toFeatureCollection( String typeName, Class< ? extends Geometry> geometryClass,
            List<OsmFeature> osmFeatures ) {
        // the tags by frequency, name first
        Map<String, Integer> frequencies = new HashMap<>();
        for( OsmFeature osmFeature : osmFeatures ) {
            for( String key : osmFeature.tags.keySet() ) {
                frequencies.merge(key, 1, Integer::sum);
            }
        }
        List<String> keys = new ArrayList<>(frequencies.keySet());
        keys.sort(( a, b ) -> {
            if (a.equals("name") != b.equals("name")) {
                return a.equals("name") ? -1 : 1;
            }
            int byFrequency = Integer.compare(frequencies.get(b), frequencies.get(a));
            return byFrequency != 0 ? byFrequency : a.compareTo(b);
        });
        List<String> columns = keys.subList(0, Math.min(MAX_TAG_COLUMNS, keys.size()));
        boolean hasOtherTags = keys.size() > columns.size();

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName(typeName);
        typeBuilder.setCRS(DefaultGeographicCRS.WGS84);
        typeBuilder.add(GEOMETRY_FIELD, geometryClass);
        typeBuilder.add(OSM_TYPE_FIELD, String.class);
        typeBuilder.add(OSM_ID_FIELD, Long.class);
        // the attribute names of the tags, which could be the same as the fixed ones
        Map<String, String> columnNames = new LinkedHashMap<>();
        for( String key : columns ) {
            String name = key;
            if (name.equals(GEOMETRY_FIELD) || name.equals(OSM_TYPE_FIELD) || name.equals(OSM_ID_FIELD)
                    || name.equals(OTHER_TAGS_FIELD)) {
                name = "tag_" + name;
            }
            columnNames.put(key, name);
            typeBuilder.add(name, String.class);
        }
        if (hasOtherTags) {
            typeBuilder.add(OTHER_TAGS_FIELD, String.class);
        }
        SimpleFeatureType featureType = typeBuilder.buildFeatureType();

        List<SimpleFeature> features = new ArrayList<>();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(featureType);
        for( OsmFeature osmFeature : osmFeatures ) {
            builder.set(GEOMETRY_FIELD, osmFeature.geometry);
            builder.set(OSM_TYPE_FIELD, osmFeature.type);
            builder.set(OSM_ID_FIELD, osmFeature.id);
            StringBuilder otherTags = new StringBuilder();
            for( Map.Entry<String, String> tag : osmFeature.tags.entrySet() ) {
                String column = columnNames.get(tag.getKey());
                if (column != null) {
                    builder.set(column, tag.getValue());
                } else {
                    if (otherTags.length() > 0) {
                        otherTags.append(';');
                    }
                    otherTags.append(tag.getKey()).append('=').append(tag.getValue());
                }
            }
            if (hasOtherTags && otherTags.length() > 0) {
                builder.set(OTHER_TAGS_FIELD, otherTags.toString());
            }
            features.add(builder.buildFeature(osmFeature.type + "/" + osmFeature.id));
        }
        return new ListFeatureCollection(featureType, features);
    }

    /**
     * An element read, before the columns are known.
     */
    private record OsmFeature( String type, long id, Geometry geometry, Map<String, String> tags ) {
    }
}
