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
package org.hortonmachine.hmachine.modules.network.netnumbering;

import static org.hortonmachine.gears.libs.modules.HMConstants.NETWORK;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_KEYWORDS;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_LABEL;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_LICENSE;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_NAME;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_STATUS;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.io.vectorreader.OmsVectorReader;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.modules.NetLink;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.files.FileUtilities;

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

@Description(OMSNETNUMBERING_DESCRIPTION)
@Author(name = OMSNETNUMBERING_AUTHORNAMES, contact = OMSNETNUMBERING_AUTHORCONTACTS)
@Keywords(OMSNETNUMBERING_KEYWORDS)
@Label(OMSNETNUMBERING_LABEL)
@Name(OMSNETNUMBERING_NAME)
@Status(OMSNETNUMBERING_STATUS)
@License(OMSNETNUMBERING_LICENSE)
public class OmsNetNumbering extends HMModel {

    @Description(OMSNETNUMBERING_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSNETNUMBERING_inTca_DESCRIPTION)
    @In
    public GridCoverage2D inTca = null;

    @Description(OMSNETNUMBERING_inNet_DESCRIPTION)
    @In
    public GridCoverage2D inNet = null;

    @Description(OMSNETNUMBERING_inPoints_DESCRIPTION)
    @In
    public SimpleFeatureCollection inPoints = null;

    @Description(OMSNETNUMBERING_outNetnum_DESCRIPTION)
    @Out
    public GridCoverage2D outNetnum = null;

    @Description(OMSNETNUMBERING_outBasins_DESCRIPTION)
    @Out
    public GridCoverage2D outBasins = null;

    @Description(OMSNETNUMBERING_outMindmap_DESCRIPTION)
    @Out
    public String outMindmap = null;

    public static final String OMSNETNUMBERING_DESCRIPTION = "Assigns the numbers to the network's links.";
    public static final String OMSNETNUMBERING_DOCUMENTATION = "OmsNetNumbering.html";
    public static final String OMSNETNUMBERING_KEYWORDS = "Network, SplitSubbasins";
    public static final String OMSNETNUMBERING_LABEL = NETWORK;
    public static final String OMSNETNUMBERING_NAME = "netnum";
    public static final int OMSNETNUMBERING_STATUS = 40;
    public static final String OMSNETNUMBERING_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSNETNUMBERING_AUTHORNAMES = "Antonello Andrea, Franceschi Silvia, Rigon Riccardo";
    public static final String OMSNETNUMBERING_AUTHORCONTACTS = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSNETNUMBERING_inFlow_DESCRIPTION = "The map of flowdirections.";
    public static final String OMSNETNUMBERING_inTca_DESCRIPTION = "The map of total contributing area.";
    public static final String OMSNETNUMBERING_inNet_DESCRIPTION = "The map of the network.";
    public static final String OMSNETNUMBERING_inPoints_DESCRIPTION = "The monitoringpoints vector map.";
    public static final String OMSNETNUMBERING_fPointId_DESCRIPTION = "The name of the node id field in mode 2.";
    public static final String OMSNETNUMBERING_outNetnum_DESCRIPTION = "The map of netnumbering";
    public static final String OMSNETNUMBERING_outBasins_DESCRIPTION = "The map of subbasins";
    public static final String OMSNETNUMBERING_outMindmap_DESCRIPTION = "Output mindmap (plantuml).";

    @Execute
    public void process() throws Exception {
        if (!concatOr(outNetnum == null, doReset)) {
            return;
        }
        checkNull(inFlow, inNet);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2IntWritableRaster(flowRI, true);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);
        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);

        WritableRandomIter netNumIter = null;
        try {
            List<NetLink> nodesList = new ArrayList<NetLink>();
            WritableRaster netNumWR = ModelsEngine.netNumbering(inFlow, inNet, inTca, inPoints, nodesList, pm);

            for( NetLink nl1 : nodesList ) {
                for( NetLink nl2 : nodesList ) {
                    if (!nl1.equals(nl2)) {
                        nl1.connect(nl2);
                    }
                }
            }
            

            List<NetLink> rootNetLink = nodesList.stream().filter(n -> n.downStreamLink == null)
                    .collect(Collectors.toList());
            if (rootNetLink.size() > 1) {
                throw new ModelsRuntimeException("More than one link found to be root link. Check the dataset.", this);
            }
            NetLink rootLink = rootNetLink.get(0);
            StringBuilder sb = new StringBuilder();
            sb.append("@startmindmap\n");
            String level = "";
            printLink(rootLink, level, sb);
            sb.append("@endmindmap\n");

            outMindmap = sb.toString();

            netNumIter = RandomIterFactory.createWritable(netNumWR, null);
            WritableRaster basinWR = ModelsEngine.extractSubbasins(flowIter, netIter, netNumIter, nRows, nCols, pm);

            outNetnum = CoverageUtilities.buildCoverage("netnum", netNumWR, regionMap, inFlow.getCoordinateReferenceSystem());
            outBasins = CoverageUtilities.buildCoverage("subbasins", basinWR, regionMap, inFlow.getCoordinateReferenceSystem());
        } finally {
            flowIter.done();
            netIter.done();
            if (netNumIter != null)
                netNumIter.done();
        }
    }

    private void printLink( NetLink node, String previousLevel, StringBuilder sb ) {
        String level = previousLevel + "*";
        sb.append(level).append(" ").append(node.toString()).append("\n");
        for( NetLink upNode : node.upStreamLinks ) {
            printLink(upNode, level, sb);
        }
    }

    public static void main( String[] args ) throws Exception {
        String folder = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/brenta_medium/";
        String inFlow = folder + "brenta_drain.asc";
        String inTca = folder + "brenta_tca.asc";
        String inNet = folder + "brenta_net_10000.asc";
        String inPoints = null;// folder + "";
        String outNetnum = folder + "mytest_netnum.asc";
        String outBasins = folder + "mytest_basins.asc";
        String outMM = folder + "mytest_mindmap.txt";
        OmsNetNumbering omsnetnumbering = new OmsNetNumbering();
        omsnetnumbering.inFlow = OmsRasterReader.readRaster(inFlow);
        omsnetnumbering.inTca = OmsRasterReader.readRaster(inTca);
        omsnetnumbering.inNet = OmsRasterReader.readRaster(inNet);
        if (inPoints != null) {
            omsnetnumbering.inPoints = OmsVectorReader.readVector(inPoints);
        }
        omsnetnumbering.process();

        FileUtilities.writeFile(omsnetnumbering.outMindmap, new File(outMM));

        OmsRasterWriter.writeRaster(outNetnum, omsnetnumbering.outNetnum);
        OmsRasterWriter.writeRaster(outBasins, omsnetnumbering.outBasins);

//        @startmindmap
//        * <b>basin1</b>\n<i>coordinate:1,2</i>
//        ** Ubuntu
//        *** Linux Mint
//        *** Kubuntu
//        *** Lubuntu
//        *** KDE Neon
//        ** LMDE
//        ** SolydXK
//        ** SteamOS
//        ** Raspbian with a very long name
//        *** <s>Raspmbc</s> => OSMC
//        *** <s>Raspyfi</s> => Volumio
//        @endmindmap
    }

}