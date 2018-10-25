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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.lwrecruitment;

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
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.operation.union.CascadedPolygonUnion;

@Description(OmsLW01_ChannelPolygonMerger.DESCRIPTION)
@Author(name = OmsLW01_ChannelPolygonMerger.AUTHORS, contact = OmsLW01_ChannelPolygonMerger.CONTACTS)
@Keywords(OmsLW01_ChannelPolygonMerger.KEYWORDS)
@Label(OmsLW01_ChannelPolygonMerger.LABEL)
@Name("_" + OmsLW01_ChannelPolygonMerger.NAME)
@Status(OmsLW01_ChannelPolygonMerger.STATUS)
@License(OmsLW01_ChannelPolygonMerger.LICENSE)
public class OmsLW01_ChannelPolygonMerger extends HMModel {

    @Description(inBankfull_DESCR)
    @In
    public SimpleFeatureCollection inBankfull = null;

    @Description(outBankfull_DESCR)
    @Out
    public SimpleFeatureCollection outBankfull = null;

    // VARS DOCS START
    public static final String outBankfull_DESCR = "The output polygon of the bankfull area";
    public static final String inBankfull_DESCR = "The input polygon layer of the bankfull area";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw01_channelpolygonmerger";
    public static final String LABEL = HMConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "network, vector, union";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Merges the adjacent bankfull polygons in a single geometry for further processing.";
    // VARS DOC END

    @Execute
    public void process() throws Exception {
        checkNull(inBankfull);

        List<Geometry> geoms = FeatureUtilities.featureCollectionToGeometriesList(inBankfull, true, null);

        // creates a unique feature with multipolygons
        Geometry union = CascadedPolygonUnion.union(geoms);

        // makes a buffer of each geometry in the feature and merges the touching geometries
        Geometry buffer = union.buffer(0.05);

        // splits the remaining geometries (not touching)
        List<Geometry> newGeoms = new ArrayList<Geometry>();
        for( int i = 0; i < buffer.getNumGeometries(); i++ ) {
            Geometry geometryN = buffer.getGeometryN(i);
            if (geometryN instanceof Polygon) {
                newGeoms.add(geometryN);
            }
        }

        outBankfull = FeatureUtilities.featureCollectionFromGeometry(inBankfull.getBounds().getCoordinateReferenceSystem(),
                newGeoms.toArray(GeometryUtilities.TYPE_POLYGON));

    }

}
