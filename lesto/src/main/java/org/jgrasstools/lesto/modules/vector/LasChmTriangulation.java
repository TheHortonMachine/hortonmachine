/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.lesto.modules.vector;

import static java.lang.Math.abs;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_DRAFT;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jgrasstools.gears.io.las.ALasDataManager;
import org.jgrasstools.gears.io.las.core.LasRecord;
import org.jgrasstools.gears.io.las.index.LasIndexer;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.DelaunayTriangulationBuilder;

@Description("Module that creates a CHM triangulation from point clouds.")
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords("triangulation, lidar, chm")
@Label(JGTConstants.LESTO + "/vector")
@Name("laschmtriangulation")
@Status(OMSHYDRO_DRAFT)
@License(OMSHYDRO_LICENSE)
@SuppressWarnings("nls")
public class LasChmTriangulation extends JGTModel {

    @Description("Las file or folder index path.")
    @In
    public String inLasFile = null;

    @Description("A dtm raster to use for the area of interest.")
    @In
    public GridCoverage2D inDtm;

    @Description("Elevation threshold for triangles.")
    @In
    public double pElevThres = 0.5;

    @Description("The filtered triangles.")
    @Out
    public ArrayList<Geometry> trianglesList;

    @Execute
    public void process() throws Exception {
        checkNull(inLasFile);

        CoordinateReferenceSystem crs = null;
        Polygon polygon = null;
        if (inDtm != null) {
            polygon = CoverageUtilities.getRegionPolygon(inDtm);
            crs = inDtm.getCoordinateReferenceSystem();
        }

        List<Coordinate> lasCoordinates = new ArrayList<Coordinate>();
        try (ALasDataManager lasData = ALasDataManager.getDataManager(new File(inLasFile), inDtm, 0.0, crs)) {
            pm.beginTask("Reading data...", -1);
            lasData.open();
            if (polygon == null) {
                ReferencedEnvelope overallEnvelope = lasData.getOverallEnvelope();
                polygon = LasIndexer.envelopeToPolygon(overallEnvelope);
            }
            List<LasRecord> lasPoints = lasData.getPointsInGeometry(polygon, true);
            if (inDtm != null) {
                for( LasRecord lasRecord : lasPoints ) {
                    lasCoordinates.add(new Coordinate(lasRecord.x, lasRecord.y, lasRecord.groundElevation));
                }
            } else {
                for( LasRecord lasRecord : lasPoints ) {
                    lasCoordinates.add(new Coordinate(lasRecord.x, lasRecord.y, lasRecord.z));
                }
            }
            pm.done();
        }
        pm.beginTask("Triangulate data...", -1);
        DelaunayTriangulationBuilder triangulationBuilder = new DelaunayTriangulationBuilder();
        triangulationBuilder.setSites(lasCoordinates);
        Geometry triangles = triangulationBuilder.getTriangles(gf);
        pm.done();

        int numTriangles = triangles.getNumGeometries();
        pm.beginTask("Extracting triangles based on threshold...", numTriangles);
        trianglesList = new ArrayList<Geometry>();
        for( int i = 0; i < numTriangles; i++ ) {
            pm.worked(1);
            Geometry geometryN = triangles.getGeometryN(i);
            Coordinate[] coordinates = geometryN.getCoordinates();
            double diff1 = abs(coordinates[0].z - coordinates[1].z);
            if (diff1 > pElevThres) {
                continue;
            }
            double diff2 = abs(coordinates[0].z - coordinates[2].z);
            if (diff2 > pElevThres) {
                continue;
            }
            double diff3 = abs(coordinates[1].z - coordinates[2].z);
            if (diff3 > pElevThres) {
                continue;
            }
            trianglesList.add(geometryN);
        }
        pm.done();

        int newNumTriangles = trianglesList.size();
        int removedNum = numTriangles - newNumTriangles;
        pm.message("Original triangles: " + numTriangles);
        pm.message("New triangles: " + newNumTriangles);
        pm.message("Removed triangles: " + removedNum);

    }

}
