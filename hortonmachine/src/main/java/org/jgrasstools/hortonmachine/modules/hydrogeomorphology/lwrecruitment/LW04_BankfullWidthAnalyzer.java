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

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.media.jai.operator.BandSelectDescriptor;

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
import org.jgrasstools.gears.utils.features.FeatureUtilities;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import com.sun.xml.internal.bind.v2.TODO;

@Description("Extracts the bankfull width for each section of the channels and adds it as an attribute to the input layer.")
@Author(name = "Silvia Franceschi, Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("network, vector, point, bankflull, width")
@Label("HortonMachine/Hydro-Geomorphology/LWRecruitment")
@Name("LW04_BankfullWidthAnalyzer")
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class LW04_BankfullWidthAnalyzer extends JGTModel {

    @Description("The input polygon layer of the bankfull area.")
    @In
    public SimpleFeatureCollection inBankfull = null;

    @Description("The input hierarchy point network layer.")
    @In
    public SimpleFeatureCollection inNetPoints = null;

    @Description("The output points network layer with the additional attribute of bankfull width.")
    @Out
    public SimpleFeatureCollection outNetPoints = null;

    @Description("The output points layer highlighting the position of the problematic sections.")
    @Out
    public SimpleFeatureCollection outProblemPoints = null;

    @Description("The output layer with the sections lines where the bankfull width has been calculated.")
    @Out
    public SimpleFeatureCollection outBankfullSections = null;

    @Execute
    public void process() throws Exception {

        int NEW_NETWORK_ATTRIBUTES_NUM = 2;

        // error messages
        String NEAREST_CHANNEL_POLYGON_TOO_FAR_FROM_POINT = "nearest channeledit polygon is too far from point";
        String NO_CHANNELEDIT_POLYGON_FOUND = "no channeledit polygon found";
        String FOUND_INVALID_NETWORK_WIDTH_LARGE = "invalid network width (too large)";
        String FOUND_INVALID_NETWORK_WIDTH_SMALL = "invalid network width (too small)";
        String NO_PROPER_INTERSECTION_WITH_CHANNELEDIT = "no proper intersection with channeledit";

        CoordinateReferenceSystem crs;

        double MAX_DISTANCE_FROM_NETPOINT = 100.0;
        /**
         * The maximum distance that a point can have from the nearest polygon. If
         * distance is major, then the netpoint is ignored and identified as outside of the 
         * region of interest.  
         */
        double MAX_NETWORK_WIDTH = 100;
        double MIN_NETWORK_WIDTH = 0.5;

        //Creates the output points hashmap
        ConcurrentHashMap<SimpleFeature, double[]> allNetPointsMap = new ConcurrentHashMap<SimpleFeature, double[]>();

        crs = inNetPoints.getBounds().getCoordinateReferenceSystem();
        
        //Insert the points in the final hashmap
        List<SimpleFeature> netFeatures = FeatureUtilities.featureCollectionToList(inNetPoints);
        for( SimpleFeature netFeature : netFeatures ) {
            allNetPointsMap.put(netFeature, new double[NEW_NETWORK_ATTRIBUTES_NUM]);
        }

        //Generates supporting variables
        ConcurrentHashMap<SimpleFeature, String> problemPointsMap = new ConcurrentHashMap<SimpleFeature, String>();
        ConcurrentHashMap<SimpleFeature, double[]> validPointsMap = new ConcurrentHashMap<SimpleFeature, double[]>();
        ConcurrentLinkedQueue<Object[]> validPointsLineList = new ConcurrentLinkedQueue<Object[]>();
        handleChannelEdited(inBankfull, allNetPointsMap, validPointsMap, problemPointsMap, validPointsLineList);


    }

    private void handleChannelEdited( SimpleFeatureCollection channeleditedFC,
            ConcurrentHashMap<SimpleFeature, double[]> netPointsMap, ConcurrentHashMap<SimpleFeature, double[]> validPointsMap,
            ConcurrentHashMap<SimpleFeature, String> problemPointsMap, ConcurrentLinkedQueue<Object[]> validPointsLineList ) {
        
        // TODO
        
        
    }
    
    /**
     * @param args
     * @throws Exception 
     */
    public static void main( String[] args ) throws Exception {
        String inBankfull = "D:/lavori_tmp/gsoc/channeledited_merged.shp";
        String inNetPoints = "D:/lavori_tmp/gsoc/netpoints.shp";

        String outNetPoints = "D:/lavori_tmp/gsoc/netpoints_width.shp";
        String outProblemPoints = "D:/lavori_tmp/gsoc/netpoints_problems.shp";
        String outBankfullSections = "D:/lavori_tmp/gsoc/bankfull_sections.shp";

        LW04_BankfullWidthAnalyzer bankfullWidthAnalyzer = new LW04_BankfullWidthAnalyzer();
        bankfullWidthAnalyzer.inBankfull = OmsVectorReader.readVector(inBankfull);
        bankfullWidthAnalyzer.inNetPoints = OmsVectorReader.readVector(inNetPoints);

        bankfullWidthAnalyzer.process();

        SimpleFeatureCollection outNetPointFC = bankfullWidthAnalyzer.outNetPoints;
        SimpleFeatureCollection outProblemPointsFC = bankfullWidthAnalyzer.outProblemPoints;
        SimpleFeatureCollection outBankfullSectionsFC = bankfullWidthAnalyzer.outBankfullSections;

        OmsVectorWriter.writeVector(outNetPoints, outNetPointFC);
        OmsVectorWriter.writeVector(outProblemPoints, outProblemPointsFC);
        OmsVectorWriter.writeVector(outBankfullSections, outBankfullSectionsFC);

    }

}
