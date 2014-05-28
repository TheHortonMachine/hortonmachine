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
import org.jgrasstools.gears.io.vectorreader.OmsVectorReader;
import org.jgrasstools.gears.io.vectorwriter.OmsVectorWriter;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("Creates the equivalent point shapefile for the input hierarchic line shapefile of the network.")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("network, vector, point")
@Label("HortonMachine/Hydro-Geomorphology/LWRecruitment")
@Name("LW02_NetworkHierarchyToPointsSplitter")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class LW02_NetworkHierarchyToPointsSplitter extends JGTModel {

    @Description("The input hierarchy network layer")
    @In
    public SimpleFeatureCollection inNet = null;
    
    @Description("")
    @Out
    public SimpleFeatureCollection outNetPoints = null;
    
    
    @Execute
    public void process() throws Exception {
        checkNull(inNet);
        
        
        
        outNetPoints = null;
    }
    
    
    /**
     * @param args
     * @throws Exception 
     */
    public static void main( String[] args ) throws Exception {

        String inNet = "D:/lavori_tmp/gsoc/channeledited_merged.shp";
        String outNetPoint = "D:/lavori_tmp/gsoc/netpoints.shp";
        LW02_NetworkHierarchyToPointsSplitter networkHierarchyToPointSplitter = new LW02_NetworkHierarchyToPointsSplitter();
        networkHierarchyToPointSplitter.inNet = OmsVectorReader.readVector(inNet);
        
        networkHierarchyToPointSplitter.process();
        
        SimpleFeatureCollection outNetPointFC = networkHierarchyToPointSplitter.outNetPoints;
        
        OmsVectorWriter.writeVector(outNetPoint, outNetPointFC);
        
    }

}
