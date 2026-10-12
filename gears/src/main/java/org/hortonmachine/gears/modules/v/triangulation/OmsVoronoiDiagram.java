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
package org.hortonmachine.gears.modules.v.triangulation;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_CERTIFIED;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureGeometrySubstitutor;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;

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

@Description(OmsVoronoiDiagram.DESCRIPTION)
@Documentation(OmsVoronoiDiagram.DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsVoronoiDiagram.KEYWORDS)
@Label(HMConstants.VECTORPROCESSING)
@Name("_" + OmsVoronoiDiagram.NAME)
@Status(OMSHYDRO_CERTIFIED)
@License(OMSHYDRO_LICENSE)
public class OmsVoronoiDiagram extends HMModel {

    @Description(inMap_DESCR)
    @In
    public SimpleFeatureCollection inMap = null;

    @Description(outMap_DESCR)
    @Out
    public SimpleFeatureCollection outMap = null;

    // start variable docs
    public static final String NAME = "voronoidiagram";
    public static final String KEYWORDS = "vector, voronoi, thiessen";
    public static final String DESCRIPTION = "Creates the Voronoi diagram, or Thiessen polygons, of a set of points: around each point, the cell of the places closer to it than to any other point.";
    public static final String DOCUMENTATION = """
            Each cell takes the attributes of its point, so the diagram gives the area of influence of each \
            point, as in the Thiessen method for the mean rainfall from the rain gauges. The cells are the dual \
            of the Delaunay triangulation: their borders are the axes of the sides of the triangles.

            The outer cells are open on one side, so the diagram is cut on a rectangle larger than the points: \
            their bounds, enlarged on each side by their diagonal. Points in the same position get one cell, \
            with the attributes of the first of them.
            """;
    public static final String outMap_DESCR = "The output voronoi map, with the attributes of the points.";
    public static final String inMap_DESCR = "The input points map.";
    // end variable docs

    @Execute
    public void process() throws Exception {
        checkNull(inMap);

        if (!EGeometryType.isPoint(inMap.getSchema().getGeometryDescriptor())) {
            throw new ModelsIllegalargumentException("The input geometry needs to be points.", this, pm);
        }

        List<SimpleFeature> fList = FeatureUtilities.featureCollectionToList(inMap);

        pm.beginTask("Processing...", fList.size());
        // the points by position, to give each cell the attributes of its point
        Map<Coordinate, SimpleFeature> site2Feature = new HashMap<>();
        List<Coordinate> cList = new ArrayList<Coordinate>();
        for( SimpleFeature f : fList ) {
            Geometry geometry = (Geometry) f.getDefaultGeometry();
            Coordinate c = new Coordinate(geometry.getCoordinate());
            if (site2Feature.putIfAbsent(c, f) == null) {
                cList.add(c);
            }
            pm.worked(1);
        }
        pm.done();

        VoronoiDiagramBuilder b = new VoronoiDiagramBuilder();
        b.setSites(cList);
        Geometry diagram = b.getDiagram(gf);

        FeatureGeometrySubstitutor substitutor = new FeatureGeometrySubstitutor(inMap.getSchema(), Polygon.class);
        DefaultFeatureCollection outFC = new DefaultFeatureCollection();
        for( int i = 0; i < diagram.getNumGeometries(); i++ ) {
            Geometry cell = diagram.getGeometryN(i);
            // the builder puts the point of the cell in the user data
            SimpleFeature site = site2Feature.get((Coordinate) cell.getUserData());
            outFC.add(substitutor.substituteGeometry(site, cell));
        }
        outMap = outFC;
    }

}
