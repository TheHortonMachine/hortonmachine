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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.lwrecruitment;

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
import org.geotools.feature.DefaultFeatureCollection;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

@Description(OmsLW08_NetworBufferMergerHolesRemover.DESCRIPTION)
@Author(name = OmsLW08_NetworBufferMergerHolesRemover.AUTHORS, contact = OmsLW08_NetworBufferMergerHolesRemover.CONTACTS)
@Keywords(OmsLW08_NetworBufferMergerHolesRemover.KEYWORDS)
@Label(OmsLW08_NetworBufferMergerHolesRemover.LABEL)
@Name("_" + OmsLW08_NetworBufferMergerHolesRemover.NAME)
@Status(OmsLW08_NetworBufferMergerHolesRemover.STATUS)
@License(OmsLW08_NetworBufferMergerHolesRemover.LICENSE)
public class OmsLW08_NetworBufferMergerHolesRemover extends JGTModel {

    @Description(inInundationArea_DESCR)
    @In
    public SimpleFeatureCollection inInundationArea = null;

    @Description(outInundationArea_DESCR)
    @Out
    public SimpleFeatureCollection outInundationArea = null;

    // VARS DOC START
    public static final String outInundationArea_DESCR = "The output polygon layer with the merged and without holes inundation polygons.";
    public static final String inInundationArea_DESCR = "The input polygon layer with the inundation polygons.";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw08_networbuffermergerholesremover";
    public static final String LABEL = JGTConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "network, vector, bankflull, inundation";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Merge the inundated polygons to avoid strange perimeters.";
    // VARS DOC END

    @Execute
    public void process() {

        // create a geometry list of the input polygons
        List<Geometry> inInundationGeomsList = FeatureUtilities.featureCollectionToGeometriesList(inInundationArea, false, null);

        // make the union and merge of the polygons
        Geometry union = CascadedPolygonUnion.union(inInundationGeomsList);
        List<Geometry> removedHoles = removeHoles(union);

        // store the results in the output feature collection
        outInundationArea = new DefaultFeatureCollection();
        SimpleFeatureCollection outMergedAreaFC = FeatureUtilities.featureCollectionFromGeometry(inInundationArea.getBounds()
                .getCoordinateReferenceSystem(), removedHoles.toArray(GeometryUtilities.TYPE_POLYGON));

        ((DefaultFeatureCollection) outInundationArea).addAll(outMergedAreaFC);
    }

    /*
     * remove holes in merged polygons
     */
    private List<Geometry> removeHoles( Geometry cleanPolygon ) {
        ArrayList<Geometry> gl = new ArrayList<Geometry>();
        for( int i = 0; i < cleanPolygon.getNumGeometries(); i++ ) {
            Polygon geometryN = (Polygon) cleanPolygon.getGeometryN(i);
            LineString exteriorRing = geometryN.getExteriorRing();
            Coordinate[] ringCoordinates = exteriorRing.getCoordinates();
            Polygon polygon = gf.createPolygon(ringCoordinates);
            gl.add(polygon);
        }
        return gl;
    }

}
