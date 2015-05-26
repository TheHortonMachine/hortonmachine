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
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

@Description(OmsLW03_NetworkHierarchyToPointsSplitter.DESCRIPTION)
@Author(name = OmsLW03_NetworkHierarchyToPointsSplitter.AUTHORS, contact = OmsLW03_NetworkHierarchyToPointsSplitter.CONTACTS)
@Keywords(OmsLW03_NetworkHierarchyToPointsSplitter.KEYWORDS)
@Label(OmsLW03_NetworkHierarchyToPointsSplitter.LABEL)
@Name("_" + OmsLW03_NetworkHierarchyToPointsSplitter.NAME)
@Status(OmsLW03_NetworkHierarchyToPointsSplitter.STATUS)
@License(OmsLW03_NetworkHierarchyToPointsSplitter.LICENSE)
public class OmsLW03_NetworkHierarchyToPointsSplitter extends JGTModel implements LWFields {
    @Description(inNet_DESCR)
    @In
    public SimpleFeatureCollection inNet = null;

    @Description(outNetPoints_DESCR)
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    // VARS DOC START
    public static final String outNetPoints_DESCR = "The output points network layer";
    public static final String inNet_DESCR = "The input hierarchy network layer";
    public static final int STATUS = Status.EXPERIMENTAL;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String NAME = "lw03_networkhierarchytopointssplitter";
    public static final String LABEL = JGTConstants.HYDROGEOMORPHOLOGY + "/LWRecruitment";
    public static final String KEYWORDS = "network, vector, point";
    public static final String CONTACTS = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Creates the equivalent point shapefile for the input hierarchic line shapefile of the network.";
    // VARS DOC END

    @Execute
    public void process() throws Exception {
        checkNull(inNet);

        // Creates the list of contained features
        List<SimpleFeature> netList = FeatureUtilities.featureCollectionToList(inNet);

        // Creates the output feature collection
        DefaultFeatureCollection outNetPointsFC = new DefaultFeatureCollection();

        // Creates the structure of the output point layer
        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setName("net");
        b.setCRS(inNet.getBounds().getCoordinateReferenceSystem());
        b.add("the_geom", Point.class);
        b.add(PFAF, String.class);
        b.add(LINKID, Integer.class);
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        /*for each line generates the points geometries of the contained  
         * vertexes with attributes -> it is assumed that the input network is
         * the output of the tool NetworkAttributesBuilder  
         */
        for( SimpleFeature netLineFeature : netList ) {
            int count = 1;
            Object pfaf = netLineFeature.getAttribute(PFAF);
            Geometry netLine = (Geometry) netLineFeature.getDefaultGeometry();
            for( int i = 0; i < netLine.getNumGeometries(); i++ ) {
                LineString net = (LineString) netLine.getGeometryN(i);
                Coordinate[] coordinates = net.getCoordinates();
                for( int j = 0; j < coordinates.length - 1; j++ ) {

                    Point point = gf.createPoint(coordinates[j]);

                    Object[] values = new Object[]{point, pfaf, count};
                    builder.addAll(values);
                    SimpleFeature feature = builder.buildFeature(null);

                    outNetPointsFC.add(feature);
                    count++;
                }
            }

        }
        outNetPoints = outNetPointsFC;
    }

}
