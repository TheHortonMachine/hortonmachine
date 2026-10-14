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
package org.hortonmachine.gears.modules.v.polygonize;

import static org.hortonmachine.gears.libs.modules.HMConstants.VECTORPROCESSING;
import static org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer.OMSLINESPOLYGONIZER_AUTHORCONTACTS;
import static org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer.OMSLINESPOLYGONIZER_AUTHORNAMES;
import static org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer.OMSLINESPOLYGONIZER_DESCRIPTION;
import static org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer.OMSLINESPOLYGONIZER_DOCUMENTATION;
import static org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer.OMSLINESPOLYGONIZER_KEYWORDS;
import static org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer.OMSLINESPOLYGONIZER_LABEL;
import static org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer.OMSLINESPOLYGONIZER_LICENSE;
import static org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer.OMSLINESPOLYGONIZER_NAME;
import static org.hortonmachine.gears.modules.v.polygonize.OmsLinesPolygonizer.OMSLINESPOLYGONIZER_STATUS;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.util.LineStringExtracter;
import org.locationtech.jts.operation.polygonize.Polygonizer;
import org.locationtech.jts.operation.union.UnaryUnionOp;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(OMSLINESPOLYGONIZER_DESCRIPTION)
@Documentation(OMSLINESPOLYGONIZER_DOCUMENTATION)
@Author(name = OMSLINESPOLYGONIZER_AUTHORNAMES, contact = OMSLINESPOLYGONIZER_AUTHORCONTACTS)
@Keywords(OMSLINESPOLYGONIZER_KEYWORDS)
@Label(OMSLINESPOLYGONIZER_LABEL)
@Name(OMSLINESPOLYGONIZER_NAME)
@Status(OMSLINESPOLYGONIZER_STATUS)
@License(OMSLINESPOLYGONIZER_LICENSE)
public class OmsLinesPolygonizer extends HMModel {

    @Description(OMSLINESPOLYGONIZER_IN_MAP_DESCRIPTION)
    @In
    public SimpleFeatureCollection inMap = null;

    @Description(OMSLINESPOLYGONIZER_P_MODE_DESCRIPTION)
    @UI("combo:" + MODE_FACES + "," + MODE_JOIN)
    @In
    public String pMode = MODE_FACES;

    @Description(OMSLINESPOLYGONIZER_F_GROUP_DESCRIPTION)
    @In
    public String fGroup = null;

    @Description(OMSLINESPOLYGONIZER_IN_POINTS_DESCRIPTION)
    @In
    public SimpleFeatureCollection inPoints = null;

    @Description(OMSLINESPOLYGONIZER_F_ID_DESCRIPTION)
    @In
    public String fId = null;

    @Description(OMSLINESPOLYGONIZER_F_NEW_ID_DESCRIPTION)
    @In
    public String fNewId = "id";

    @Description(OMSLINESPOLYGONIZER_OUT_MAP_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outMap = null;

    public static final String MODE_FACES = "faces";
    public static final String MODE_JOIN = "join";

    public static final String OMSLINESPOLYGONIZER_DESCRIPTION = "Builds polygons from lines, as the faces the lines enclose or as one polygon for each group of lines.";
    public static final String OMSLINESPOLYGONIZER_DOCUMENTATION = """
            In the **faces** mode the lines are first noded, cut where they cross, so lines \
            that cross without sharing a vertex still close a face. Every face the network \
            encloses becomes a polygon, also the ones between other faces: a line that \
            closes onto itself makes a polygon, a line that ends in nothing (a dangle) \
            or only connects two faces (a cut edge) does not. The number of the lines \
            that were not used is reported in the log.

            In the **join** mode the lines of each group, all the lines when no group \
            field is given, are chained into one ring: from the end of a line the chain \
            goes on with the line that has the nearest end, reversing it if needed, \
            and the last line is joined to the first. Gaps between the lines are \
            closed with a straight segment, which suits outlines digitized in pieces. \
            The groups whose ring crosses itself are reported and left out.

            The id of a polygon is taken from the point of the points layer that lies \
            in it, if a points layer is given, else from the group in the join mode, \
            else it is a progressive number from 1. Polygons with no point get no id.
            """;
    public static final String OMSLINESPOLYGONIZER_KEYWORDS = "Vector, Polygonize";
    public static final String OMSLINESPOLYGONIZER_LABEL = VECTORPROCESSING;
    public static final String OMSLINESPOLYGONIZER_NAME = "linespolygonizer";
    public static final int OMSLINESPOLYGONIZER_STATUS = 40;
    public static final String OMSLINESPOLYGONIZER_LICENSE = "http://www.gnu.org/licenses/gpl-3.0.html";
    public static final String OMSLINESPOLYGONIZER_AUTHORNAMES = "Antonio Falciano, Andrea Antonello";
    public static final String OMSLINESPOLYGONIZER_AUTHORCONTACTS = "afalciano@yahoo.it, http://blog.spaziogis.it/, http://www.hydrologis.com";
    public static final String OMSLINESPOLYGONIZER_IN_MAP_DESCRIPTION = "The lines to polygonize.";
    public static final String OMSLINESPOLYGONIZER_P_MODE_DESCRIPTION = "How to build the polygons: faces, the areas enclosed by the lines, or join, one polygon chaining the lines of each group.";
    public static final String OMSLINESPOLYGONIZER_F_GROUP_DESCRIPTION = "The field of the lines that groups them into polygons in the join mode (optional, all the lines make one polygon if not set).";
    public static final String OMSLINESPOLYGONIZER_IN_POINTS_DESCRIPTION = "The points whose attribute gives the id of the polygon they lie in (optional).";
    public static final String OMSLINESPOLYGONIZER_F_ID_DESCRIPTION = "The field of the points with the id of the polygons (necessary if the points are given).";
    public static final String OMSLINESPOLYGONIZER_F_NEW_ID_DESCRIPTION = "The field of the polygons for the id.";
    public static final String OMSLINESPOLYGONIZER_OUT_MAP_DESCRIPTION = "The polygons.";

    @Execute
    public void process() throws Exception {
        checkNull(inMap, pMode, fNewId);

        EGeometryType geometryType = EGeometryType.forGeometryDescriptor(inMap.getSchema().getGeometryDescriptor());
        switch( geometryType ) {
        case LINESTRING:
        case MULTILINESTRING:
            break;
        default:
            throw new ModelsIllegalargumentException("The module only works with line layers.", this, pm);
        }

        // the polygons with the id they get if there are no points
        List<Polygon> polygonsList = new ArrayList<>();
        List<Object> idsList = new ArrayList<>();
        Class< ? > idClass = Integer.class;
        if (pMode.equals(MODE_FACES)) {
            List<Geometry> linesList = FeatureUtilities.featureCollectionToGeometriesList(inMap, true, null);
            Geometry nodedLines = UnaryUnionOp.union(linesList);

            Polygonizer polygonizer = new Polygonizer();
            if (nodedLines != null) {
                polygonizer.add(nodedLines);
            }
            @SuppressWarnings("unchecked")
            Collection<Polygon> polygons = polygonizer.getPolygons();
            int index = 1;
            for( Polygon polygon : polygons ) {
                polygonsList.add(polygon);
                idsList.add(index++);
            }

            // the same piece can be reported more times
            Set<Geometry> unused = new HashSet<>();
            unused.addAll(polygonizer.getDangles());
            unused.addAll(polygonizer.getCutEdges());
            unused.addAll(polygonizer.getInvalidRingLines());
            if (!unused.isEmpty()) {
                pm.message("Pieces of lines not used for any polygon (dangles, cut edges, invalid rings): " + unused.size());
            }
        } else if (pMode.equals(MODE_JOIN)) {
            String groupField = null;
            if (fGroup != null && !fGroup.isBlank()) {
                groupField = FeatureUtilities.findAttributeName(inMap.getSchema(), fGroup);
                if (groupField == null) {
                    throw new ModelsIllegalargumentException("The group field is not in the lines: " + fGroup, this, pm);
                }
                idClass = inMap.getSchema().getDescriptor(groupField).getType().getBinding();
            }

            Map<Object, List<LineString>> group2Lines = new LinkedHashMap<>();
            try (SimpleFeatureIterator iterator = inMap.features()) {
                while( iterator.hasNext() ) {
                    SimpleFeature feature = iterator.next();
                    Geometry geometry = (Geometry) feature.getDefaultGeometry();
                    if (geometry == null) {
                        continue;
                    }
                    Object group = groupField == null ? 1 : feature.getAttribute(groupField);
                    List<LineString> lines = group2Lines.computeIfAbsent(group, k -> new ArrayList<>());
                    for( Object line : LineStringExtracter.getLines(geometry) ) {
                        LineString lineString = (LineString) line;
                        if (!lineString.isEmpty()) {
                            lines.add(lineString);
                        }
                    }
                }
            }

            List<Object> invalidGroups = new ArrayList<>();
            for( Map.Entry<Object, List<LineString>> entry : group2Lines.entrySet() ) {
                List<LineString> lines = entry.getValue();
                Polygon polygon = null;
                if (!lines.isEmpty()) {
                    try {
                        polygon = GeometryUtilities.lines2Polygon(true, lines.toArray(LineString[]::new));
                    } catch (IllegalArgumentException e) {
                        // a ring of less than 4 points
                    }
                }
                if (polygon == null) {
                    invalidGroups.add(entry.getKey());
                } else {
                    polygonsList.add(polygon);
                    idsList.add(entry.getKey());
                }
            }
            if (!invalidGroups.isEmpty()) {
                pm.errorMessage("Groups left out because their ring crosses itself or has too few points: " + invalidGroups);
            }
        } else {
            throw new ModelsIllegalargumentException("The mode can only be " + MODE_FACES + " or " + MODE_JOIN + ": " + pMode,
                    this, pm);
        }

        List<Geometry> pointGeometries = null;
        if (inPoints != null) {
            checkNull(fId);
            String idField = FeatureUtilities.findAttributeName(inPoints.getSchema(), fId);
            if (idField == null) {
                throw new ModelsIllegalargumentException("The id field is not in the points: " + fId, this, pm);
            }
            idClass = inPoints.getSchema().getDescriptor(idField).getType().getBinding();
            pointGeometries = FeatureUtilities.featureCollectionToGeometriesList(inPoints, false, idField);
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("polygonized");
        b.setCRS(inMap.getSchema().getCoordinateReferenceSystem());
        b.add("the_geom", Polygon.class);
        b.add(fNewId, idClass);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        DefaultFeatureCollection outFC = new DefaultFeatureCollection();
        pm.beginTask("Generating polygon features...", polygonsList.size());
        for( int i = 0; i < polygonsList.size(); i++ ) {
            Polygon polygon = polygonsList.get(i);
            Object id = idsList.get(i);
            if (pointGeometries != null) {
                id = null;
                for( Geometry point : pointGeometries ) {
                    if (polygon.contains(point)) {
                        id = point.getUserData();
                        break;
                    }
                }
            }
            builder.addAll(new Object[]{polygon, id});
            outFC.add(builder.buildFeature(null));
            pm.worked(1);
        }
        pm.done();
        outMap = outFC;
    }
}
