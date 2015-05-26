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
package org.jgrasstools.gears.modules.v.triangulation;

import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_CERTIFIED;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

@Description(OmsVoronoiDiagram.DESCRIPTION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OmsVoronoiDiagram.KEYWORDS)
@Label(JGTConstants.VECTORPROCESSING)
@Name("_" + OmsVoronoiDiagram.NAME)
@Status(OMSHYDRO_CERTIFIED)
@License(OMSHYDRO_LICENSE)
public class OmsVoronoiDiagram extends JGTModel {

    @Description(inMap_DESCR)
    @In
    public SimpleFeatureCollection inMap = null;

    @Description(fElev_DESCR)
    @In
    public String fElev = "elev";

    @Description(outMap_DESCR)
    @Out
    public SimpleFeatureCollection outMap = null;

    // start variable docs
    public static final String NAME = "voronoidiagram";
    public static final String KEYWORDS = "vector, voronoi";
    public static final String DESCRIPTION = "Creates a Voronoi diagram from a set of points.";
    public static final String outMap_DESCR = "The output voronoi map.";
    public static final String fElev_DESCR = "The optional name of the field containing the elevation value.";
    public static final String inMap_DESCR = "The input points map.";
    // end variable docs

    @Execute
    public void process() throws Exception {
        checkNull(inMap);

        if (!GeometryUtilities.isPoint(inMap.getSchema().getGeometryDescriptor())) {
            throw new ModelsIllegalargumentException("The input geometry needs to be points.", this, pm);
        }

        if (fElev != null) {
            fElev = FeatureUtilities.findAttributeName(inMap.getSchema(), fElev);
            if (fElev == null) {
                throw new ModelsIllegalargumentException("Couldn't find field: " + fElev, this);
            }
        }

        CoordinateReferenceSystem crs = inMap.getBounds().getCoordinateReferenceSystem();
        List<SimpleFeature> fList = FeatureUtilities.featureCollectionToList(inMap);

        pm.beginTask("Processing...", fList.size());
        VoronoiDiagramBuilder b = new VoronoiDiagramBuilder();
        List<Coordinate> cList = new ArrayList<Coordinate>();
        for( SimpleFeature f : fList ) {
            Geometry geometry = (Geometry) f.getDefaultGeometry();
            double elev = 0.0;
            if (fElev != null)
                elev = (Double) f.getAttribute(fElev);

            Coordinate c = geometry.getCoordinate();
            c.z = elev;
            cList.add(c);
            pm.worked(1);
        }
        pm.done();

        b.setSites(cList);

        List<Geometry> geosList = new ArrayList<Geometry>();
        Geometry diagram = b.getDiagram(gf);
        for( int i = 0; i < diagram.getNumGeometries(); i++ ) {
            Geometry geometryN = diagram.getGeometryN(i);
            Coordinate[] coordinates = geometryN.getCoordinates();
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for( Coordinate coordinate : coordinates ) {
                min = Math.min(min, coordinate.z);
                max = Math.max(max, coordinate.z);
            }
            geometryN.setUserData(new String[]{"" + min, "" + max});
            geosList.add(geometryN);
        }

        outMap = FeatureUtilities.featureCollectionFromGeometry(crs, geosList.toArray(new Geometry[0]));
    }

}
