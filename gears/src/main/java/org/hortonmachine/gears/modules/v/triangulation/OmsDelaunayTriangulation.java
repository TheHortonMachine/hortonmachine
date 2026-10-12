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
import java.util.List;

import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.EGeometryType;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

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

@Description(OmsDelaunayTriangulation.DESCRIPTION)
@Documentation(OmsDelaunayTriangulation.DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsDelaunayTriangulation.KEYWORDS)
@Label(HMConstants.VECTORPROCESSING)
@Name("_" + OmsDelaunayTriangulation.NAME)
@Status(OMSHYDRO_CERTIFIED)
@License(OMSHYDRO_LICENSE)
public class OmsDelaunayTriangulation extends HMModel {

    @Description(inMap_DESCR)
    @In
    public SimpleFeatureCollection inMap = null;

    @Description(fElev_DESCR)
    @In
    public String fElev = null;

    @Description(outMap_DESCR)
    @Out
    public SimpleFeatureCollection outMap = null;

    // start variable docs
    public static final String NAME = "delaunaytriangulation";
    public static final String KEYWORDS = "vector, delaunay, triangulation, TIN";
    public static final String DESCRIPTION = "Creates the Delaunay triangulation of a set of points: the triangles that join them without crossing, as close to equilateral as possible.";
    public static final String DOCUMENTATION = """
            The triangles are those of a Delaunay triangulation: the circle through the three vertices of a \
            triangle contains no other point. Of all the triangulations of the points, it is the one with the \
            largest smallest angle, so the triangles are as little slivered as the points allow, which makes it \
            the usual base of a TIN (triangulated irregular network). The triangulation covers the convex hull \
            of the points. Points in the same position are used once.

            With an elevation, from the field fElev or else from the z of the points, the vertices of the \
            triangles have it as z, and each triangle gets the lowest and the highest elevation of its vertices \
            in the fields minelev and maxelev. The output has only these fields: the triangles don't belong to a \
            single point, so the other attributes of the points are not kept.
            """;
    public static final String outMap_DESCR = "The output triangles map.";
    public static final String fElev_DESCR = "The optional field with the elevation of the points. If not set, the z of the points is used, if they have one.";
    public static final String inMap_DESCR = "The input points map.";
    // end variable docs

    /**
     * The field with the lowest elevation of the vertices of a triangle.
     */
    public static final String MINELEV = "minelev";
    /**
     * The field with the highest elevation of the vertices of a triangle.
     */
    public static final String MAXELEV = "maxelev";

    @Execute
    public void process() throws Exception {
        checkNull(inMap);

        if (!EGeometryType.isPoint(inMap.getSchema().getGeometryDescriptor())) {
            throw new ModelsIllegalargumentException("The input geometry needs to be points.", this, pm);
        }

        String elevField = null;
        if (fElev != null) {
            elevField = FeatureUtilities.findAttributeName(inMap.getSchema(), fElev);
            if (elevField == null) {
                throw new ModelsIllegalargumentException("Couldn't find field: " + fElev, this);
            }
        }

        CoordinateReferenceSystem crs = inMap.getSchema().getCoordinateReferenceSystem();
        List<SimpleFeature> fList = FeatureUtilities.featureCollectionToList(inMap);

        pm.beginTask("Processing...", fList.size());
        List<Coordinate> cList = new ArrayList<Coordinate>();
        for( SimpleFeature f : fList ) {
            Geometry geometry = (Geometry) f.getDefaultGeometry();
            // a copy, so the input points are not changed
            Coordinate c = new Coordinate(geometry.getCoordinate());
            if (elevField != null) {
                Object elev = f.getAttribute(elevField);
                c.z = elev instanceof Number ? ((Number) elev).doubleValue() : Double.NaN;
            }
            cList.add(c);
            pm.worked(1);
        }
        pm.done();

        DelaunayTriangulationBuilder b = new DelaunayTriangulationBuilder();
        b.setSites(cList);
        Geometry triangles = b.getTriangles(gf);

        boolean hasElev = cList.stream().anyMatch(c -> !Double.isNaN(c.z));

        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("triangles");
        typeBuilder.setCRS(crs);
        typeBuilder.add("the_geom", Polygon.class);
        if (hasElev) {
            typeBuilder.add(MINELEV, Double.class);
            typeBuilder.add(MAXELEV, Double.class);
        }
        SimpleFeatureType type = typeBuilder.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        DefaultFeatureCollection outFC = new DefaultFeatureCollection();
        for( int i = 0; i < triangles.getNumGeometries(); i++ ) {
            Geometry triangle = triangles.getGeometryN(i);
            builder.add(triangle);
            if (hasElev) {
                double min = Double.POSITIVE_INFINITY;
                double max = Double.NEGATIVE_INFINITY;
                for( Coordinate coordinate : triangle.getCoordinates() ) {
                    min = Math.min(min, coordinate.z);
                    max = Math.max(max, coordinate.z);
                }
                builder.add(min);
                builder.add(max);
            }
            outFC.add(builder.buildFeature(null));
        }
        outMap = outFC;
    }

}
