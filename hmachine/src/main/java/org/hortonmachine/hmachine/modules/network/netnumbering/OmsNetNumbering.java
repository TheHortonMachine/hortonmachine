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
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.modules.NetLink;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

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
import oms3.annotations.Unit;

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

    @Description(OMSNETNUMBERING_desiredArea_DESCRIPTION)
    @Unit("m2")
    @In
    public Double pDesiredArea = null;

    @Description(OMSNETNUMBERING_desiredAreaDelta_DESCRIPTION)
    @Unit("%")
    @In
    public Double pDesiredAreaDelta = null;

    @Description(OMSNETNUMBERING_outNetnum_DESCRIPTION)
    @Out
    public GridCoverage2D outNetnum = null;

    @Description(OMSNETNUMBERING_outBasins_DESCRIPTION)
    @Out
    public GridCoverage2D outBasins = null;

    @Description(OMSNETNUMBERING_outDesiredBasins_DESCRIPTION)
    @Out
    public GridCoverage2D outDesiredBasins = null;

    @Description(OMSNETNUMBERING_outMindmap_DESCRIPTION)
    @Out
    public String outMindmap = null;

    @Description(OMSNETNUMBERING_outMindmapDesired_DESCRIPTION)
    @Out
    public String outDesiredMindmap = null;

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
    public static final String OMSNETNUMBERING_outDesiredBasins_DESCRIPTION = "The map of desired size subbasins";
    public static final String OMSNETNUMBERING_outMindmap_DESCRIPTION = "Output mindmap (plantuml).";
    public static final String OMSNETNUMBERING_outMindmapDesired_DESCRIPTION = "Output desired mindmap (plantuml).";
    public static final String OMSNETNUMBERING_desiredArea_DESCRIPTION = "The desired basins area size.";
    public static final String OMSNETNUMBERING_desiredAreaDelta_DESCRIPTION = "The allowed variance for the desired area.";

    private int nCols;

    private int nRows;

    private double xres;

    private double yres;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outNetnum == null, doReset)) {
            return;
        }
        checkNull(inFlow, inNet);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        nCols = regionMap.getCols();
        nRows = regionMap.getRows();
        xres = regionMap.getXres();
        yres = regionMap.getYres();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2IntWritableRaster(flowRI, true);
        WritableRandomIter flowIter = RandomIterFactory.createWritable(flowWR, null);
        RandomIter netIter = CoverageUtilities.getRandomIterator(inNet);

        WritableRandomIter netNumIter = null;
        try {
            List<NetLink> nodesList = new ArrayList<NetLink>();
            WritableRaster netNumWR = ModelsEngine.netNumbering(inFlow, inNet, inTca, inPoints, nodesList, pm);
            outNetnum = CoverageUtilities.buildCoverage("netnum", netNumWR, regionMap, inFlow.getCoordinateReferenceSystem());

            netNumIter = RandomIterFactory.createWritable(netNumWR, null);
            WritableRaster basinWR = ModelsEngine.extractSubbasins(flowIter, netIter, netNumIter, nRows, nCols, pm);
            outBasins = CoverageUtilities.buildCoverage("subbasins", basinWR, regionMap, inFlow.getCoordinateReferenceSystem());

            RandomIter subbasinIter = CoverageUtilities.getRandomIterator(outBasins);
            int validCount = 0;
            int invalidCount = 0;
            try {
                for( int r = 0; r < nRows; r++ ) {
                    for( int c = 0; c < nCols; c++ ) {
                        int value = subbasinIter.getSample(c, r, 0);
                        if (!isNovalue(value)) {
                            validCount++;
                        } else {
                            invalidCount++;
                        }
                    }
                }

                // now handle basin hierarchy
                for( NetLink nl1 : nodesList ) {
                    for( NetLink nl2 : nodesList ) {
                        if (!nl1.equals(nl2)) {
                            nl1.connect(nl2);
                        }
                    }
                }

                List<NetLink> rootNetLink = nodesList.stream().filter(n -> n.getDownStreamLink() == null)
                        .collect(Collectors.toList());
                if (rootNetLink.size() > 1) {
                    throw new ModelsRuntimeException("More than one link found to be root link. Check the dataset.", this);
                }

                // create mindmap
                NetLink rootLink = rootNetLink.get(0);
                StringBuilder sb = new StringBuilder();
                sb.append("@startmindmap\n");
                sb.append("* <b>basin stats</b>\\nvalid basin cells: " + validCount + "\\ntotal cells: "
                        + (invalidCount + validCount) + "\\ntotal basin area: " + (validCount * xres * yres) + "\\nx res: " + xres
                        + "\\ny res: " + yres + "\n");
                String level = "*";
                printLinkAsMindMap(rootLink, level, sb);
                sb.append("@endmindmap\n");
                outMindmap = sb.toString();

                if (pDesiredArea != null) {
                    HashMap<Integer, Integer> conversionMap = new HashMap<>();
                    int convertedSize;
                    int postConvertedSize;
                    do {
                        convertedSize = conversionMap.size();
                        List<NetLink> links = Arrays.asList(rootLink);
                        aggregateBasins(links, conversionMap);
                        postConvertedSize = conversionMap.size();
                    } while( postConvertedSize - convertedSize > 0 );

                    sb = new StringBuilder();
                    sb.append("@startmindmap\n");
                    sb.append("* <b>basin stats</b>\\nvalid basin cells: " + validCount + "\\ntotal cells: "
                            + (invalidCount + validCount) + "\\ntotal basin area: " + (validCount * xres * yres) + "\\nx res: "
                            + xres + "\\ny res: " + yres + "\n");
                    level = "*";
                    printLinkAsMindMap(rootLink, level, sb);
                    sb.append("@endmindmap\n");
                    outDesiredMindmap = sb.toString();

                    WritableRaster desiredSubbasinsWR = CoverageUtilities.createWritableRaster(nCols, nRows, Integer.class, null,
                            HMConstants.intNovalue);
                    WritableRandomIter desiredSubbasinsWIter = RandomIterFactory.createWritable(desiredSubbasinsWR, null);
                    for( int r = 0; r < nRows; r++ ) {
                        for( int c = 0; c < nCols; c++ ) {
                            int value = subbasinIter.getSample(c, r, 0);
                            if (!isNovalue(value)) {
                                Integer convertedBasinNum = conversionMap.get(value);
                                if (convertedBasinNum != null) {
                                    desiredSubbasinsWIter.setSample(c, r, 0, convertedBasinNum);
                                } else {
                                    desiredSubbasinsWIter.setSample(c, r, 0, value);
                                }
                            }
                        }
                    }
                    desiredSubbasinsWIter.done();
                    outDesiredBasins = CoverageUtilities.buildCoverage("desiredsubbasins", desiredSubbasinsWR, regionMap,
                            inFlow.getCoordinateReferenceSystem());
                }

            } finally {
                subbasinIter.done();
            }

        } finally {
            flowIter.done();
            netIter.done();
            if (netNumIter != null)
                netNumIter.done();
        }
    }

    private void aggregateBasins( List<NetLink> netLinks, HashMap<Integer, Integer> conversionMap ) throws Exception {
        // TODO COMMENTS
        // * same level aggregations are not supported (only into a parent), since one would have to
        // move the outlet into a different basin (two outlets need to be joined into a dowstream single
        // outlet)

        double desArea = pDesiredArea;
        double minArea = desArea - desArea * pDesiredAreaDelta / 100.0;
        // double maxArea = desArea + desArea * pDesiredAreaDelta / 100.0;

        for( NetLink netLink : netLinks ) {
            List<NetLink> ups =  netLink.getUpStreamLinks();

            double area = getLinkOnlyArea(netLink);
            if (area < minArea) {
                if (!ups.isEmpty()) {
                    while( area < minArea ) {
                        // find nearest to area
                        double minDelta = Double.POSITIVE_INFINITY;
                        NetLink minDeltaLink = null;
                        double minAddArea = 0;
                        boolean hadOne = false;
                        for( NetLink nl : ups ) {
                            // check if it wasn't used already (i.e. it has no parent)
                            if (nl.desiredChainNetLink == null) {
                                hadOne = true;

                                double nlArea = getLinkOnlyArea(nl);
                                double delta = Math.abs(desArea - (area + nlArea));
                                if (delta < minDelta) {
                                    minDelta = delta;
                                    minDeltaLink = nl;
                                    minAddArea = nlArea;
                                }
                            }
                        }
                        if (!hadOne) {
                            // seems like the basins are not enough
                            break;
                        }
                        if (minDeltaLink != null) {
                            minDeltaLink.desiredChainNetLink = netLink.num;
                            area += minAddArea;

                            // adjust the upstream channel end and area
                            conversionMap.put(minDeltaLink.num, netLink.num);

                            ups.remove(minDeltaLink);
                            ups.addAll(minDeltaLink.getUpStreamLinks());
                        }
                    }
                } else {
                    // if the last basin is too small, let's aggregate it with the parent
                    NetLink parentLink = netLink.getDownStreamLink();
                    netLink.desiredChainNetLink = parentLink.num;
                    conversionMap.put(netLink.num, parentLink.num);
                    parentLink.getUpStreamLinks().remove(netLink);
                }
            } else {
                // go up one level
                if (!ups.isEmpty()) {
                    aggregateBasins(ups, conversionMap);
                }
            }
        }
    }

    private double getLinkOnlyArea( NetLink netLink ) {
        int tca = getLinkOnlyTca(netLink);
        double area = tca * xres * yres;
        return area;
    }

    private int getLinkOnlyTca( NetLink netLink ) {
        int upLinksTca = 0;
        for( NetLink nl : netLink.getUpStreamLinks() ) {
            upLinksTca += nl.getTca();
        }
        int tca = netLink.getTca() - upLinksTca;
        return tca;
    }

    private void printLinkAsMindMap( NetLink node, String previousLevel, StringBuilder sb ) {
        String level = previousLevel + "*";
        sb.append(level).append(" ").append(node.toMindMapString()).append("\n");
        for( NetLink upNode : node.getUpStreamLinks() ) {
            printLinkAsMindMap(upNode, level, sb);
        }
    }

}