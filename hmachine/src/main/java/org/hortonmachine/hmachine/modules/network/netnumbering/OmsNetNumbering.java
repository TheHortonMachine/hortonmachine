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
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.gears.libs.modules.NetLink;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.features.FeatureUtilities;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

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

    // @Description(OMSNETNUMBERING_pMaxAllowedConfluences_DESCRIPTION)
    // @In
    // public int pMaxAllowedConfluences = -1;

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

    @Description(OMSNETNUMBERING_outGeoframeTopology_DESCRIPTION)
    @Out
    public String outGeoframeTopology = null;

    @Description(OMSNETNUMBERING_outBasinsInfo_DESCRIPTION)
    @Out
    public String outBasinsInfo = null;

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
    public static final String OMSNETNUMBERING_pMaxAllowedConfluences_DESCRIPTION = "The maximum number of channels that can converge into 1 node (-1 = no limit). Works only in desired area mode.";
    public static final String OMSNETNUMBERING_outGeoframeTopology_DESCRIPTION = "The optional geoframe topology output file.";
    public static final String OMSNETNUMBERING_outBasinsInfo_DESCRIPTION = "The optional basins info output file.";

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

        List<Geometry> pointsList = null;
        if (inPoints != null) {
            pointsList = FeatureUtilities.featureCollectionToGeometriesList(inPoints, true, null);
        }

        WritableRandomIter netNumIter = null;
        try {
            List<NetLink> linksList = new ArrayList<NetLink>();
            WritableRaster netNumWR = ModelsEngine.netNumbering(inFlow, inNet, inTca, pointsList, linksList, pm);
            outNetnum = CoverageUtilities.buildCoverage("netnum", netNumWR, regionMap, inFlow.getCoordinateReferenceSystem());

            netNumIter = RandomIterFactory.createWritable(netNumWR, null);
            int novalue = HMConstants.getIntNovalue(inFlow);
            WritableRaster basinWR = ModelsEngine.extractSubbasins(flowIter, novalue, netIter, netNumIter, nRows, nCols, pm);
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
                for( NetLink nl1 : linksList ) {
                    for( NetLink nl2 : linksList ) {
                        if (!nl1.equals(nl2)) {
                            nl1.connect(nl2);
                        }
                    }
                }

                List<NetLink> rootNetLink = linksList.stream().filter(n -> n.getDownStreamLink() == null)
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

                    double desArea = pDesiredArea;
                    double minArea = desArea - desArea * pDesiredAreaDelta / 100.0;

                    for( NetLink netLink : linksList ) {
                        // first handle the most upstream ones and aggregate them with their parents
                        List<NetLink> upStreamLinks = netLink.getUpStreamLinks();

                        if (!netLink.isFixed() && upStreamLinks.isEmpty()) {
                            double area = getLinkOnlyArea(netLink);
                            if (area < minArea) {
                                NetLink parentLink = netLink.getDownStreamLink();
                                if (parentLink != null) {
                                    netLink.desiredChainNetLink = parentLink.num;
                                    conversionMap.put(netLink.num, parentLink.num);
                                    parentLink.getUpStreamLinks().remove(netLink);
                                }
                            }
                        }
                        // also consider basins that have 1 fixed upstream basin (case of monitoring
                        // point)
                        if (upStreamLinks.size() == 1 && upStreamLinks.get(0).isFixed()) {
                            NetLink fixed = upStreamLinks.get(0);
                            double area = getLinkOnlyArea(netLink);
                            if (area < minArea && !netLink.isFixed()) {
                                // if the area is too small we need to merge it downstream,
                                // but only if the basin itself is not fixed. In that case
                                // it just can't be changed in size, period.
                                NetLink parentLink = netLink.getDownStreamLink();
                                if (parentLink != null) {
                                    netLink.desiredChainNetLink = parentLink.num;
                                    conversionMap.put(netLink.num, parentLink.num);
                                    parentLink.getUpStreamLinks().remove(netLink);
                                    parentLink.getUpStreamLinks().add(fixed);
                                }
                            }
                        }
                    }

                    int convertedSize;
                    int postConvertedSize;
                    do {
                        convertedSize = conversionMap.size();
                        List<NetLink> links = Arrays.asList(rootLink);
                        aggregateBasins(links, conversionMap, minArea, desArea, 0);
                        postConvertedSize = conversionMap.size();
                    } while( postConvertedSize - convertedSize > 0 );

                    // if (pMaxAllowedConfluences > 0) {
                    // // review the tree and solve junctions with more than that number of
                    // upstreams
                    // do {
                    // convertedSize = conversionMap.size();
                    // List<NetLink> links = Arrays.asList(rootLink);
                    // fixManyChannels(links, conversionMap);
                    // postConvertedSize = conversionMap.size();
                    // } while( postConvertedSize - convertedSize > 0 );
                    // }

                    sb = new StringBuilder();
                    sb.append("@startmindmap\n");
                    sb.append("* <b>basin stats</b>\\nvalid basin cells: " + validCount + "\\ntotal cells: "
                            + (invalidCount + validCount) + "\\ntotal basin area: " + (validCount * xres * yres) + "\\nx res: "
                            + xres + "\\ny res: " + yres + "\\ndesired cells: " + (desArea / xres / yres) + "\n");
                    level = "*";
                    printLinkAsMindMap(rootLink, level, sb);
                    sb.append("@endmindmap\n");
                    outDesiredMindmap = sb.toString();

                    // build basins info
                    StringBuilder basinsInfoSb = new StringBuilder();
                    basinsInfoSb.append("basinid;outletX;outletY;area\n");
                    printLinkAsIdOutletUpstreamArea(rootLink, inTca, inTca.getGridGeometry(), basinsInfoSb);
                    outBasinsInfo = basinsInfoSb.toString();

                    WritableRaster desiredSubbasinsWR = CoverageUtilities.createWritableRaster(nCols, nRows, Integer.class, null,
                            HMConstants.intNovalue);
                    WritableRandomIter desiredSubbasinsWIter = RandomIterFactory.createWritable(desiredSubbasinsWR, null);
                    for( int r = 0; r < nRows; r++ ) {
                        for( int c = 0; c < nCols; c++ ) {
                            int value = subbasinIter.getSample(c, r, 0);
                            if (!isNovalue(value)) {
                                Integer convertedBasinNum = conversionMap.get(value);
                                if (convertedBasinNum != null) {
                                    // check if the converted has been converted also in some
                                    // different thread
                                    Integer convertedBasinNumTmp = conversionMap.get(convertedBasinNum);
                                    while( convertedBasinNumTmp != null ) {
                                        convertedBasinNum = convertedBasinNumTmp;
                                        convertedBasinNumTmp = conversionMap.get(convertedBasinNumTmp);
                                    }
                                    desiredSubbasinsWIter.setSample(c, r, 0, convertedBasinNum);
                                } else {
                                    desiredSubbasinsWIter.setSample(c, r, 0, value);
                                }
                            }
                        }
                    }
                    desiredSubbasinsWIter.done();
                    outDesiredBasins = CoverageUtilities.buildCoverageWithNovalue("desiredsubbasins", desiredSubbasinsWR,
                            regionMap, inFlow.getCoordinateReferenceSystem(), HMConstants.intNovalue);
                }

                // build geoframe topology input
                StringBuilder geoframeSb = new StringBuilder();
                geoframeSb.append(rootLink.num).append(" ").append(0).append("\n");
                printLinkAsGeoframe(rootLink, geoframeSb);
                outGeoframeTopology = geoframeSb.toString();

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

    private void aggregateBasins( List<NetLink> currentLevelLinks, HashMap<Integer, Integer> conversionMap, double minArea,
            double desArea, int level ) throws Exception {

        for( NetLink netLink : currentLevelLinks ) {
            List<NetLink> ups = netLink.getUpStreamLinks();

            double area = getLinkOnlyArea(netLink);
            if (area < minArea) {
                if (!ups.isEmpty()) {
                    while( area < minArea ) {
                        // find nearest to area
                        double minDelta = Double.POSITIVE_INFINITY;
                        NetLink minDeltaLink = null;
                        double minAddArea = 0;
                        boolean hadOne = false;
                        int fixedCounts = 0;
                        for( NetLink nl : ups ) {
                            if (nl.isFixed()) {
                                // fixed ones can't be joined to downstream
                                fixedCounts++;
                                continue;
                            }
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
                        if (fixedCounts == ups.size() && fixedCounts != 0) {
                            if (!netLink.isFixed()) {
                                // all upstream are fixed and area is small, aggregate downstream
                                // if the last basin is too small, let's aggregate it with the
                                // parent
                                NetLink parentLink = netLink.getDownStreamLink();
                                netLink.desiredChainNetLink = parentLink.num;
                                conversionMap.put(netLink.num, parentLink.num);
                                parentLink.getUpStreamLinks().remove(netLink);
                                for( NetLink nl : ups ) {
                                    nl.setDownStreamLink(parentLink);
                                }
                                parentLink.getUpStreamLinks().addAll(ups);
                                break;
                            } else {
                                // nothing to do here, go up one level and break out
                                aggregateBasins(ups, conversionMap, minArea, desArea, level + 1);
                                break;
                            }
                        } else {
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
                                List<NetLink> minDeltaUpstreamLinks = minDeltaLink.getUpStreamLinks();
                                for( NetLink nl : minDeltaUpstreamLinks ) {
                                    nl.setDownStreamLink(netLink);
                                }
                                ups.addAll(minDeltaUpstreamLinks);
                            }
                        }
                    }
                } else {
                    // if the last basin is too small, let's aggregate it with the parent
                    if (!netLink.isFixed()) {
                        NetLink parentLink = netLink.getDownStreamLink();
                        netLink.desiredChainNetLink = parentLink.num;
                        conversionMap.put(netLink.num, parentLink.num);
                        parentLink.getUpStreamLinks().remove(netLink);
                    }
                }
            } else {
                // go up one level
                if (!ups.isEmpty()) {
                    aggregateBasins(ups, conversionMap, minArea, desArea, level + 1);
                }
            }
        }
    }

    // private void fixManyChannels( List<NetLink> currentLevelLinks, HashMap<Integer, Integer>
    // conversionMap ) throws Exception {

    // for( NetLink netLink : currentLevelLinks ) {
    // List<NetLink> ups = netLink.getUpStreamLinks();

    // int upsSize = ups.size();
    // if (upsSize > pMaxAllowedConfluences) {
    // // merge down the smalles up basins into the current
    // int toRemove = upsSize - pMaxAllowedConfluences;

    // List<NetLink> sortedUps = ups.stream().sorted(( nl1, nl2 ) -> {
    // double a1 = getLinkOnlyArea(nl1);
    // double a2 = getLinkOnlyArea(nl2);
    // if (a1 > a2) {
    // return 1;
    // } else if (a2 > a1) {
    // return -1;
    // } else {
    // return 0;
    // }
    // }).collect(Collectors.toList());

    // int removedCount = 0;
    // int iter = 0;
    // while( removedCount < toRemove ) {
    // NetLink upToRemove = sortedUps.get(iter);
    // if (!upToRemove.isFixed()) {
    // upToRemove.desiredChainNetLink = netLink.num;
    // conversionMap.put(upToRemove.num, netLink.num);
    // netLink.getUpStreamLinks().remove(upToRemove);

    // List<NetLink> upUps = upToRemove.getUpStreamLinks();
    // for( NetLink nl : upUps ) {
    // nl.setDownStreamLink(netLink);
    // }
    // netLink.getUpStreamLinks().addAll(upUps);
    // removedCount++;
    // }
    // iter++;
    // if (iter == upsSize) {
    // break;
    // }
    // }

    // if (removedCount < toRemove) {
    // throw new ModelsIllegalargumentException(
    // "Unable to remove converging channel to the desired parameter, due to presence of fixed
    // channels.
    // Check your input data.",
    // this);
    // }
    // } else {
    // fixManyChannels(ups, conversionMap);
    // }
    // }
    // }

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

    private void printLinkAsGeoframe( NetLink node, StringBuilder sb ) {
        for( NetLink upNode : node.getUpStreamLinks() ) {
            sb.append(upNode.num).append(" ").append(node.num).append("\n");
            printLinkAsGeoframe(upNode, sb);
        }
    }

    private void printLinkAsIdOutletUpstreamArea( NetLink node, GridCoverage2D inTca, GridGeometry2D gridGeometry2D,
            StringBuilder sb ) {
        Coordinate coordinate = CoverageUtilities.coordinateFromColRow(node.downCol, node.downRow, gridGeometry2D);

        int tca = node.getTca();
        double area = xres * yres * tca;
        sb.append(node.num).append(";").append(coordinate.x).append(";").append(coordinate.y).append(";").append(area)
                .append("\n");
        for( NetLink upNode : node.getUpStreamLinks() ) {
            printLinkAsIdOutletUpstreamArea(upNode, inTca, gridGeometry2D, sb);
        }
    }

}