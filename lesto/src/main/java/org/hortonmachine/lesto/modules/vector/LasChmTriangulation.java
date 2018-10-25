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
package org.hortonmachine.lesto.modules.vector;

import static java.lang.Math.min;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.hortonmachine.gears.io.las.ALasDataManager;
import org.hortonmachine.gears.io.las.core.LasRecord;
import org.hortonmachine.gears.io.las.index.LasIndexer;
import org.hortonmachine.gears.io.las.utils.LasUtils;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.index.strtree.STRtree;
import org.locationtech.jts.triangulate.DelaunayTriangulationBuilder;

@Description("Module that creates a CHM triangulation from point clouds.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("triangulation, lidar, chm")
@Label(HMConstants.LESTO + "/vector")
@Name("laschmtriangulation")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@UI(HMConstants.HIDE_UI_HINT)
@SuppressWarnings("nls")
public class LasChmTriangulation extends HMModel {

    @Description("Las file or folder index path.")
    @In
    public String inLasFile = null;

    @Description("A dtm raster to use for the area of interest.")
    @In
    public GridCoverage2D inDtm;

    @Description("Elevation threshold for triangles.")
    @In
    public Double pElevThres = 0.5;

    @Description("The filtered triangles.")
    @Out
    public List<Geometry> trianglesList;

    @Execute
    public void process() throws Exception {
        checkNull(inLasFile);

        CoordinateReferenceSystem crs = null;
        Polygon polygon = null;
        if (inDtm != null) {
            polygon = CoverageUtilities.getRegionPolygon(inDtm);
            crs = inDtm.getCoordinateReferenceSystem();
        }

        try (ALasDataManager lasData = ALasDataManager.getDataManager(new File(inLasFile), inDtm, 0.0, crs)) {
            pm.beginTask("Reading data...", -1);
            lasData.open();
            if (polygon == null) {
                ReferencedEnvelope overallEnvelope = lasData.getOverallEnvelope();
                polygon = LasIndexer.envelopeToPolygon(overallEnvelope);
            }
            List<LasRecord> lasPoints = lasData.getPointsInGeometry(polygon, false);
            pm.done();
//            trianglesList = blanket(lasPoints, inDtm != null, pm);
             trianglesList = LasUtils.triangulate(lasPoints, pElevThres, inDtm != null, pm);

            // int newNumTriangles = trianglesList.size();
            // pm.message("Created triangles: " + newNumTriangles);
        }

    }

    public List<Geometry> blanket( List<LasRecord> lasPoints, boolean useGround, IHMProgressMonitor pm ) {
        pm.beginTask("Triangulation...", -1);
        List<Coordinate> lasCoordinates = new ArrayList<Coordinate>();
        for( LasRecord lasRecord : lasPoints ) {
            lasCoordinates.add(new Coordinate(lasRecord.x, lasRecord.y, useGround ? lasRecord.groundElevation : lasRecord.z));
        }
        DelaunayTriangulationBuilder triangulationBuilder = new DelaunayTriangulationBuilder();
        triangulationBuilder.setSites(lasCoordinates);
        Geometry triangles = triangulationBuilder.getTriangles(gf);
        pm.done();

        ArrayList<Geometry> trianglesList = new ArrayList<Geometry>();
        int numTriangles = triangles.getNumGeometries();
        for( int i = 0; i < numTriangles; i++ ) {
            Geometry geometryN = triangles.getGeometryN(i);
            trianglesList.add(geometryN);
        }

        STRtree tree = GeometryUtilities.geometriesToSRTree(trianglesList);

        boolean[] toRemove = new boolean[numTriangles];
        pm.beginTask("Blanket creation...", toRemove.length);
        for( int i = 0; i < toRemove.length; i++ ) {
            Geometry triangle = trianglesList.get(i);
            Coordinate[] coordinates = triangle.getCoordinates();
            double minZ = min(coordinates[0].z, min(coordinates[1].z, coordinates[2].z));

            Coordinate c = new Coordinate(725740, 5205762);
            Envelope env = new Envelope(c);
            env.expandBy(3);

            // Envelope env = triangle.getEnvelopeInternal();
            List<Geometry> result = tree.query(env);
            if (true)
                return result;

            for( Geometry tmpTriangle : result ) {
                if (triangle.equalsExact(tmpTriangle)) {
                    continue;
                }

                // GeometryCollection gc = new GeometryCollection(result.toArray(new Geometry[0]),
                // gf);

                tmpTriangle = tmpTriangle.buffer(-0.001);
                if (triangle.intersects(tmpTriangle)) {
                    // check if it is higher
                    Coordinate[] tmpCoordinates = tmpTriangle.getCoordinates();
                    double tmpMinZ = min(tmpCoordinates[0].z, min(tmpCoordinates[1].z, tmpCoordinates[2].z));
                    if (tmpMinZ > minZ) {
                        // mark for removal
                        toRemove[i] = true;
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();

        ArrayList<Geometry> finalTrianglesList = new ArrayList<Geometry>();
        for( int i = 0; i < toRemove.length; i++ ) {
            if (!toRemove[i]) {
                Geometry triangle = trianglesList.get(i);
                finalTrianglesList.add(triangle);
            }
        }

        System.out.println("Triangles: " + trianglesList.size());
        System.out.println("Final Triangles: " + finalTrianglesList.size());

        return finalTrianglesList;
    }

}
