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
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.Point;

@Description("Creates the equivalent point shapefile for the input hierarchic line shapefile of the network.")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("network, vector, point")
@Label("HortonMachine/Hydro-Geomorphology/LWRecruitment")
@Name("LW03_NetworkHierarchyToPointsSplitter")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class LW03_NetworkHierarchyToPointsSplitter extends JGTModel {

    @Description("The input hierarchy network layer")
    @In
    public SimpleFeatureCollection inNet = null;
    
    @Description("The output points network layer")
    @Out
    public SimpleFeatureCollection outNetPoints = null;
    
    
    @Execute
    public void process() throws Exception {
        checkNull(inNet);
        
        String LINKID = "linkid";
        String PFAF = "pfaf";
        
        //Creates the list of contained features
        List<SimpleFeature> netList = FeatureUtilities.featureCollectionToList(inNet);
        
        //Creates the output feature collection
        DefaultFeatureCollection outNetPointsFC = new DefaultFeatureCollection();
        
        //Creates the structure of the output point layer
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
    
    
    /**
     * @param args
     * @throws Exception 
     */
    public static void main( String[] args ) throws Exception {

        String inNet = "D:/lavori_tmp/gsoc/net_attribute.shp";
        String outNetPoint = "D:/lavori_tmp/gsoc/netpoints.shp";
        LW03_NetworkHierarchyToPointsSplitter networkHierarchyToPointSplitter = new LW03_NetworkHierarchyToPointsSplitter();
        networkHierarchyToPointSplitter.inNet = OmsVectorReader.readVector(inNet);
        
        networkHierarchyToPointSplitter.process();
        
        SimpleFeatureCollection outNetPointFC = networkHierarchyToPointSplitter.outNetPoints;
        
        OmsVectorWriter.writeVector(outNetPoint, outNetPointFC);
        
    }

}
