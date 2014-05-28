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

import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.jgrasstools.gears.utils.geometry.GeometryUtilities;
import org.geotools.data.simple.SimpleFeatureCollection;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.operation.union.CascadedPolygonUnion;

@Description("Merges the adjacent bankfull polygons in a single geometry for further processing.")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("network, vector, union")
@Label("HortonMachine/Hydro-Geomorphology/LWRecruitment")
@Name("LW01_ChannelPolygonMerger")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class LW01_ChannelPolygonMerger extends JGTModel {

    @Description("The input polygon layer of the bankfull area")
    @In
    public SimpleFeatureCollection inBankfull = null;
    
    @Description("The output polygon of the bankfull area")
    @Out
    public SimpleFeatureCollection outBankfull = null;
    
    
    @Execute
    public void process() throws Exception {
        checkNull(inBankfull);
        
        List<Geometry> geoms = FeatureUtilities.featureCollectionToGeometriesList(inBankfull, true, null);
        
        //creates a unique feature with multipolygons
        Geometry union = CascadedPolygonUnion.union(geoms);
        
        //makes a buffer of each geometry in the feature and merges the touching geometries
        Geometry buffer = union.buffer(0.05);

        //splits the remaining geometries (not touching)
        List<Geometry> newGeoms = new ArrayList<Geometry>();
        for( int i = 0; i < buffer.getNumGeometries(); i++ ) {
            Geometry geometryN = buffer.getGeometryN(i);
            if (geometryN instanceof Polygon) {
                newGeoms.add(geometryN);
            }
        }

        outBankfull = FeatureUtilities.featureCollectionFromGeometry(inBankfull.getBounds()
                .getCoordinateReferenceSystem(), newGeoms.toArray(GeometryUtilities.TYPE_POLYGON));

    
    }
    /**
     * @param args
     * @throws Exception 
     */
    public static void main( String[] args ) throws Exception {

        String inBankfull = "D:/lavori_tmp/gsoc/channeledited.shp";
        String outBankfull = "D:/lavori_tmp/gsoc/channeledited_merged.shp";
        LW01_ChannelPolygonMerger channelPolygonMerger = new LW01_ChannelPolygonMerger();
        channelPolygonMerger.inBankfull = OmsVectorReader.readVector(inBankfull);
        
        channelPolygonMerger.process();
        
        SimpleFeatureCollection outBankfullFC = channelPolygonMerger.outBankfull;
        
        OmsVectorWriter.writeVector(outBankfull, outBankfullFC);
        
    }

}
