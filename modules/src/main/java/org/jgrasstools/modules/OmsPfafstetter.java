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
package org.jgrasstools.modules;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.libs.modules.ModelsEngine.go_downstream;
import static org.jgrasstools.gears.libs.modules.ModelsEngine.net2ShapeGeometries;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_inChannel_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_inChannelfeatures_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_inHackstream_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_inNetnum_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_outPfaf_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSPFAFSTETTER_pMode_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoordinates2D;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollections;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.DirectPosition2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
import org.jgrasstools.hortonmachine.modules.network.pfafstetter.ChannelInfo;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.referencing.operation.TransformException;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiLineString;

@Description(OMSPFAFSTETTER_DESCRIPTION)
@Author(name = OMSPFAFSTETTER_AUTHORNAMES, contact = OMSPFAFSTETTER_AUTHORCONTACTS)
@Keywords(OMSPFAFSTETTER_KEYWORDS)
@Label(OMSPFAFSTETTER_LABEL)
@Name(OMSPFAFSTETTER_NAME)
@Status(OMSPFAFSTETTER_STATUS)
@License(OMSPFAFSTETTER_LICENSE)
public class OmsPfafstetter extends JGTModel {

    @Description(OMSPFAFSTETTER_inPit_DESCRIPTION)
    @In
    public GridCoverage2D inPit = null;

    @Description(OMSPFAFSTETTER_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSPFAFSTETTER_inHackstream_DESCRIPTION)
    @In
    public GridCoverage2D inHackstream = null;

    @Description(OMSPFAFSTETTER_inNetnum_DESCRIPTION)
    @In
    public GridCoverage2D inNetnum = null;

    @Description(OMSPFAFSTETTER_inChannel_DESCRIPTION)
    @In
    public GridCoverage2D inChannel = null;

    @Description(OMSPFAFSTETTER_inChannelfeatures_DESCRIPTION)
    @In
    public SimpleFeatureCollection inChannelfeatures = null;

    @Description(OMSPFAFSTETTER_pMode_DESCRIPTION)
    @In
    public double pMode = 0;

    @Description(OMSPFAFSTETTER_outPfaf_DESCRIPTION)
    @Out
    public SimpleFeatureCollection outPfaf = null;

    /*
     * INTERNAL VARIABLES
     */

    private static final String CHANNEL_NUM = "channelNum"; //$NON-NLS-1$
    private static final String ELEVLASTPOINT = "elevlastpoint"; //$NON-NLS-1$
    private static final String ELEVFIRSTPOINT = "elevfirstpoint"; //$NON-NLS-1$
    private static final String PFAFSTETTER = "pfafstetter"; //$NON-NLS-1$
    private static final String NET_NUM = "netnum"; //$NON-NLS-1$

    private static final String NULL = "null"; //$NON-NLS-1$
    private static final String ID = "id"; //$NON-NLS-1$

    private List<ChannelInfo> channelList = null;
    private List<Geometry> geomVect = null;
    private List<HashMap<String, ? >> attributeVect = null;
    private List<MultiLineString> newRiverGeometriesList = null;
    private List<String> attributeName = null;
    private List<Class< ? >> attributeClass = null;
    private List<Object[]> attributesList = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int cols;
    private int rows;

    private WritableRandomIter flowIter;

    private RandomIter hackIter;

    private RandomIter pitIter;

    private RandomIter netnumIter;

    private RandomIter channelIter;

    private int[][] dir = ModelsSupporter.DIR_WITHFLOW_ENTERING;

    private int maxHackOrder = 0;
    private int[] numberOfStreams = new int[1];

    private GridGeometry2D gridGeometry;

    @Finalize
    public void close() throws Exception {
        flowIter.done();
        hackIter.done();
        pitIter.done();
        netnumIter.done();
        if (channelIter != null)
            channelIter.done();
    }

    @Execute
    public void process() throws Exception {
        if (!concatOr(outPfaf == null, doReset)) {
            return;
        }
        checkNull(inFlow, inHackstream, inPit, inNetnum);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inFlow);
        cols = regionMap.get(CoverageUtilities.COLS).intValue();
        rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        gridGeometry = inFlow.getGridGeometry();

        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);
        flowIter = RandomIterFactory.createWritable(flowWR, null);

        RenderedImage hackRI = inHackstream.getRenderedImage();
        hackIter = RandomIterFactory.create(hackRI, null);

        RenderedImage pitRI = inPit.getRenderedImage();
        pitIter = RandomIterFactory.create(pitRI, null);

        RenderedImage netnumRI = inNetnum.getRenderedImage();
        netnumIter = RandomIterFactory.create(netnumRI, null);

        if (inChannel != null) {
            RenderedImage channelRI = inChannel.getRenderedImage();
            channelIter = RandomIterFactory.create(channelRI, null);

            HashMap<String, Object> geomMap = null;
            List<String> key = new ArrayList<String>();
            SimpleFeatureType channelFeatureTypeft = inChannelfeatures.getSchema();
            for( int i = 0; i < channelFeatureTypeft.getAttributeCount(); i++ ) {
                AttributeType at = channelFeatureTypeft.getType(i);
                key.add(at.getName().toString());
            }
            geomVect = new ArrayList<Geometry>();
            attributeVect = new ArrayList<HashMap<String, ? >>();
            FeatureIterator<SimpleFeature> fIterator = inChannelfeatures.features();
            while( fIterator.hasNext() ) {
                SimpleFeature feature = fIterator.next();
                geomMap = new HashMap<String, Object>();
                for( int i = 0; i < feature.getAttributeCount(); i++ ) {
                    Object attribute = feature.getAttribute(i);
                    if (attribute != null) {
                        feature.getAttribute(i).getClass();
                        if (!(attribute instanceof Geometry))
                            geomMap.put(key.get(i), attribute);
                    } else {
                        geomMap.put(key.get(i), NULL);
                    }
                }
                geomMap.put(ID, feature.getID());
                geomVect.add((Geometry) feature.getDefaultGeometry());
                attributeVect.add(geomMap);
                geomMap = null;
            }
            fIterator.close();
        }

        pm.message(msg.message("pfafstetter.channelinfo")); //$NON-NLS-1$
        createChannelInfo();
        pm.message(msg.message("pfafstetter.calc")); //$NON-NLS-1$
        pfafstetter();
        pm.message(msg.message("pfafstetter.geom")); //$NON-NLS-1$
        createGeometries();
        pm.message(msg.message("pfafstetter.att")); //$NON-NLS-1$
        if (pMode == 0) {
            createsAttributeVect();
        } else {
            createsAttributeVectWithChannelNum();
        }

        if (newRiverGeometriesList.size() < 1 || attributesList.size() < 1) {
            throw new ModelsIllegalargumentException(msg.message("pfafstetter.err_emptygeom"), this); //$NON-NLS-1$
        }

        SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
        b.setCRS(inFlow.getCoordinateReferenceSystem());
        b.setName("pfafstetternet"); //$NON-NLS-1$
        b.add("the_geom", MultiLineString.class); //$NON-NLS-1$
        for( int j = 0; j < attributeName.size(); j++ ) {
            b.add(attributeName.get(j), attributeClass.get(j));
        }
        SimpleFeatureType type = b.buildFeatureType();
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        outPfaf = FeatureCollections.newCollection();
        int recordsNum = attributesList.get(0).length;
        for( int j = 0; j < recordsNum; j++ ) {
            List<Object> valuesList = new ArrayList<Object>();
            valuesList.add(newRiverGeometriesList.get(j));
            for( int i = 0; i < attributesList.size(); i++ ) {
                valuesList.add(attributesList.get(i)[j]);
            }
            builder.addAll(valuesList);
            SimpleFeature feature = builder.buildFeature(type.getTypeName() + "." + j); //$NON-NLS-1$
            outPfaf.add(feature);
        }
    }

    /**
     * Creates an Object ChannelInfo for every channel (a channel is composed by stream having the
     * same hack's order)
     */
    private void createChannelInfo() {
        ChannelInfo channel = null;
        int[] flow = new int[2];
        int f, netNumValue;

        pm.message(msg.message("working") + " OmsPfafstetter"); //$NON-NLS-1$//$NON-NLS-2$
        pm.message(msg.message("working12")); //$NON-NLS-1$

        // creates a vector of object ChannelInfo
        channelList = new ArrayList<ChannelInfo>();
        List<Integer> netNumList = new ArrayList<Integer>();
        List<Integer> netNumListAll = new ArrayList<Integer>();
        int num = 0;
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                if (!isNovalue(netnumIter.getSampleDouble(i, j, 0)) && flowIter.getSampleDouble(i, j, 0) != 10.0) {
                    if (netNumListAll.size() == 0) {
                        netNumListAll.add((int) netnumIter.getSampleDouble(i, j, 0));
                    }
                    // counts the streams
                    for( int k = 0; k < netNumListAll.size(); k++ ) {
                        if (netnumIter.getSampleDouble(i, j, 0) == netNumListAll.get(k)) {
                            num++;
                        }
                    }
                    if (num == 0) {
                        netNumListAll.add((int) netnumIter.getSampleDouble(i, j, 0));
                    }
                    num = 0;
                    f = 0;
                    // looks for the surce...
                    for( int k = 1; k <= 8; k++ ) {
                        if (flowIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) == dir[k][2]
                                && !isNovalue(netnumIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0))) {
                            break;
                        } else
                            f++;
                    }
                    // if the pixel is a source...set the parameters in
                    // ChannelInfo
                    if (f == 8) {
                        // creates new object ChannelInfo for this channel
                        channel = new ChannelInfo();
                        channel.setHackOrder((int) hackIter.getSampleDouble(i, j, 0));
                        // set the max order of hack
                        if (hackIter.getSampleDouble(i, j, 0) > maxHackOrder) {
                            maxHackOrder = (int) hackIter.getSampleDouble(i, j, 0);
                        }
                        netNumValue = (int) netnumIter.getSampleDouble(i, j, 0);
                        channel.addNetNumComp(netNumValue);
                        netNumList.add(netNumValue);
                        if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return;
                        // while the channels have the same order add properties
                        // the ChannelInfo
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && hackIter.getSampleDouble(flow[0], flow[1], 0) == hackIter.getSampleDouble(i, j, 0)
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10 ) {
                            if (netnumIter.getSampleDouble(flow[0], flow[1], 0) != netNumValue) {
                                netNumValue = (int) netnumIter.getSampleDouble(flow[0], flow[1], 0);
                                channel.addNetNumComp(netNumValue);
                                netNumList.add(netNumValue);
                                channel.setIsTrim(true);
                            }
                            if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return;
                        }
                        channel.setChannelParentNum((int) netnumIter.getSampleDouble(flow[0], flow[1], 0));
                        // adds new channel to channelVect
                        channelList.add(channel);
                        channel = null;
                    }
                }
            }
        }

        List<Integer> netNumDiff = new ArrayList<Integer>();
        num = 0;
        if (netNumList.size() != netNumListAll.size()) {
            for( Integer integer : netNumListAll ) {
                for( Integer integer2 : netNumList ) {
                    if (integer.equals(integer2)) {
                        num++;
                    }
                }
                if (num == 0) {
                    netNumDiff.add(integer);
                }
                num = 0;
            }
        }
        numberOfStreams[0] = netNumListAll.size();
        netNumList = null;
        netNumListAll = null;
        for( Integer integer : netNumDiff ) {
            for( int j = 0; j < rows; j++ ) {
                for( int i = 0; i < cols; i++ ) {
                    if (netnumIter.getSampleDouble(j, i, 0) == integer) {
                        flow[0] = i;
                        flow[1] = j;
                        channel = new ChannelInfo();
                        channel.setHackOrder((int) hackIter.getSampleDouble(i, j, 0));
                        if (hackIter.getSampleDouble(i, j, 0) > maxHackOrder) {
                            maxHackOrder = (int) hackIter.getSampleDouble(i, j, 0);
                        }
                        netNumValue = (int) netnumIter.getSampleDouble(i, j, 0);
                        channel.addNetNumComp(netNumValue);
                        if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return;
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && hackIter.getSampleDouble(flow[0], flow[1], 0) == hackIter.getSampleDouble(i, j, 0)
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10 ) {
                            if (netnumIter.getSampleDouble(flow[0], flow[1], 0) != netNumValue) {
                                netNumValue = (int) netnumIter.getSampleDouble(flow[0], flow[1], 0);
                                channel.addNetNumComp(netNumValue);
                                channel.setIsTrim(true);
                            }
                            if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return;
                        }
                        if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return;
                        channel.setChannelParentNum((int) netnumIter.getSampleDouble(flow[0], flow[1], 0));
                        channelList.add(channel);
                        channel = null;
                        integer = 0;
                    }
                }
            }
        }
    }

    /**
     * Assignes the number of pfafstetter to every streams
     */
    private boolean pfafstetter() {
        pm.message(msg.message("working22")); //$NON-NLS-1$

        int pfafNum = 1;
        int index = 0;
        String pfaf = ""; //$NON-NLS-1$
        for( int i = 1; i <= maxHackOrder; i++ ) {
            pm.message("Channel order: " + i); //$NON-NLS-1$
            for( ChannelInfo chanTemp : channelList ) {
                if (chanTemp.getHackOrder() == i) {
                    // first step assigns the number to the main channel
                    if (i == 1) {
                        // if the main channel has tributary channels (isTrim =
                        // true)...
                        if (chanTemp.getIsTrim() == true) {
                            // assigns uneven number to the streams in the main
                            // net
                            for( int j = 0; j < chanTemp.getNetNumComp().size(); j++ ) {
                                chanTemp.addPfafValue(String.valueOf(pfafNum));
                                pfafNum = pfafNum + 2;
                            }
                            // for every channel in channelVect assigns the
                            // pfafstetter parent to the corresponding channel
                            // (it is necessary to numbering the other channels)
                            for( int k = 0; k < channelList.size(); k++ ) {
                                if (k != index) {
                                    for( int j = 0; j < chanTemp.getNetNumComp().size(); j++ ) {
                                        if (channelList.get(k).getChannelParentNum() == chanTemp.getNetNumComp().get(j)) {
                                            channelList.get(k).setPfafParent(chanTemp.getPfafValue().get(j));
                                        }
                                    }
                                }
                            }
                        } else {
                            // if the basin has only a channel "main stream" its
                            // number of pfafstetter is equal to 0
                            chanTemp.addPfafValue("0"); //$NON-NLS-1$
                        }
                    } else {
                        // assigns a number to the other channels
                        pfafNum = 1;
                        // if the main channel has tributary channels (isTrim =
                        // true)...
                        if (chanTemp.getIsTrim() == true) {
                            for( int j = 0; j < chanTemp.getNetNumComp().size(); j++ ) {
                                String pfafParent = chanTemp.getPfafParent();
                                StringTokenizer st = new StringTokenizer(pfafParent, "."); //$NON-NLS-1$
                                int order = st.countTokens();
                                // if the pfafstetter of the parent net is
                                // composed from a single number ex. "10", adds
                                // ex. ".1", ".3"... the number of this channel
                                // is ex. "10.1", "10.3"...
                                if (order == 1) {
                                    pfaf = String.valueOf(Integer.valueOf(chanTemp.getPfafParent()) + 1)
                                            + "." + String.valueOf(pfafNum); //$NON-NLS-1$
                                    chanTemp.addPfafValue(pfaf);
                                    pfafNum = pfafNum + 2;
                                } else {
                                    // if the pfafstetter parent has number like
                                    // "10.1" its pafstetter number is "10.1.1"
                                    int token = 1;
                                    String pfafToken = ""; //$NON-NLS-1$
                                    while( st.hasMoreTokens() ) {
                                        if (token < order) {
                                            pfafToken += st.nextToken() + "."; //$NON-NLS-1$
                                        } else {
                                            StringBuilder sb = new StringBuilder();
                                            sb.append(String.valueOf(Integer.valueOf(st.nextToken()) + 1));
                                            sb.append("."); //$NON-NLS-1$
                                            sb.append(String.valueOf(pfafNum));
                                            pfafToken += sb.toString();
                                            pfafNum = pfafNum + 2;
                                        }
                                        token++;
                                    }
                                    chanTemp.addPfafValue(pfafToken);
                                }
                            }
                        } else {
                            // if the channel has isTrim = false...
                            String pfafParent = chanTemp.getPfafParent();
                            StringTokenizer st = new StringTokenizer(pfafParent, "."); //$NON-NLS-1$
                            int order = st.countTokens();
                            if (order == 1 && Integer.valueOf(chanTemp.getPfafParent()) % 2 != 0) {
                                pfaf = String.valueOf(Integer.valueOf(chanTemp.getPfafParent()) + 1);
                                chanTemp.addPfafValue(pfaf);
                                pfafNum = pfafNum + 2;
                            } else if (order == 1 && Integer.valueOf(chanTemp.getPfafParent()) % 2 == 0) {
                                pfaf = chanTemp.getPfafParent() + ".1"; //$NON-NLS-1$
                                chanTemp.addPfafValue(pfaf);
                            } else {
                                int token = 1;
                                String pfafToken = ""; //$NON-NLS-1$
                                while( st.hasMoreTokens() ) {
                                    if (token < order) {
                                        pfafToken += st.nextToken() + "."; //$NON-NLS-1$
                                    } else {
                                        pfafToken += String.valueOf(Integer.valueOf(st.nextToken()) + 1);
                                    }
                                    token++;
                                }
                                chanTemp.addPfafValue(pfafToken);
                            }
                        }
                        // for every channel in channelVect assigs the
                        // pfafstetter parent to the corresponding channel
                        // (it is necessary to numbering the other channels)
                        for( int k = 0; k < channelList.size(); k++ ) {
                            if (k != index) {
                                for( int j = 0; j < chanTemp.getNetNumComp().size(); j++ ) {
                                    if (channelList.get(k).getChannelParentNum() == chanTemp.getNetNumComp().get(j)) {
                                        channelList.get(k).setPfafParent(chanTemp.getPfafValue().get(j));
                                    }
                                }
                            }
                        }
                    }
                }
                index++;
                pfafNum = 1;
            }
        }
        return true;
    }

    /**
     * Creates geometries for every channel in the network
     * @throws TransformException 
     * @throws IOException 
     */
    private void createGeometries() throws IOException, TransformException {
        newRiverGeometriesList = net2ShapeGeometries(flowIter, netnumIter, numberOfStreams, inFlow.getGridGeometry(), pm);
    }

    /**
     * Creates attribuetes for every geometries (at the moment netNum of the channel and
     * pfafstetter) h.pfafstetter --igrass-flow dirnetm --igrass-hacks hacksm --igrass-pit pit
     * --igrass-netnumber netnumberm --oshapefile-netshapeout "/home/davide/s"
     * @throws Exception 
     */
    private void createsAttributeVect() throws Exception {

        // extracts netNumber and pfafstetter for every streams
        int index = 0;
        HashMap<Double, String> numAndPfafHash = new HashMap<Double, String>();
        String pfafValue = ""; //$NON-NLS-1$
        for( ChannelInfo channelTemp : channelList ) {
            for( double numTemp : channelTemp.getNetNumComp() ) {
                pfafValue = channelTemp.getPfafValue().get(index);
                numAndPfafHash.put(numTemp, pfafValue);
                index++;
            }
            index = 0;
        }
        List<Double> first = new ArrayList<Double>();
        List<Double> last = new ArrayList<Double>();

        pm.message(msg.message("pfafstetter.extractelev")); //$NON-NLS-1$
        for( int i = 0; i < newRiverGeometriesList.size(); i++ ) {
            pm.message(msg.message("pfafstetter.processing_link") + i); //$NON-NLS-1$
            Geometry geom = newRiverGeometriesList.get(i);
            Coordinate[] coordinates = null;
            try {
                coordinates = geom.getCoordinates();
            } catch (Exception e) {
                coordinates = new Coordinate[0];
            }
            if (coordinates.length < 2) {
                pm.errorMessage(msg.message("pfafstetter.err_found_emptygeom") + (i + 1)); //$NON-NLS-1$
                first.add(doubleNovalue);
                last.add(doubleNovalue);
            } else {
                GridCoordinates2D worldToGrid = gridGeometry
                        .worldToGrid(new DirectPosition2D(coordinates[0].x, coordinates[0].y));
                int[] rowColFirst = new int[]{worldToGrid.y, worldToGrid.x};
                worldToGrid = gridGeometry.worldToGrid(new DirectPosition2D(coordinates[coordinates.length - 1].x,
                        coordinates[coordinates.length - 1].y));
                int[] rowColLast = new int[]{worldToGrid.y, worldToGrid.x};

                first.add(pitIter.getSampleDouble(rowColFirst[1], rowColFirst[0], 0));
                last.add(pitIter.getSampleDouble(rowColLast[1], rowColLast[0], 0));
            }
        }

        // ATTRIBUTES
        // create a vector of strings (it contains the name of the attributes)
        attributeName = new ArrayList<String>();
        // creates a vector of class
        attributeClass = new ArrayList<Class< ? >>();
        // creates a vector of object
        attributesList = new ArrayList<Object[]>();

        attributeName.add(NET_NUM);
        attributeName.add(PFAFSTETTER);
        attributeName.add(ELEVFIRSTPOINT);
        attributeName.add(ELEVLASTPOINT);

        // adds netNum attribute...
        Object[] netnumAttribute = new Object[numAndPfafHash.size()];
        for( int j = 0; j < numAndPfafHash.size(); j++ ) {
            netnumAttribute[j] = j + 1;
        }
        attributesList.add(netnumAttribute);
        attributeClass.add(netnumAttribute[0].getClass());

        // adds pfafstetter attribute...
        double indexDouble = 0;
        Object[] pfaffstetterAttribute = new Object[numAndPfafHash.size()];
        for( int j = 0; j < numAndPfafHash.size(); j++ ) {
            pfaffstetterAttribute[j] = numAndPfafHash.get(indexDouble + 1);
            indexDouble++;
        }
        attributesList.add(pfaffstetterAttribute);
        attributeClass.add(pfaffstetterAttribute[0].getClass());

        // adds first point attribute...
        Object[] firstpointAttribute = new Object[numAndPfafHash.size()];
        for( int j = 0; j < numAndPfafHash.size(); j++ ) {
            firstpointAttribute[j] = first.get(j);
        }
        attributesList.add(firstpointAttribute);
        attributeClass.add(firstpointAttribute[0].getClass());

        // adds last point attribute...
        Object[] lastpointAttribute = new Object[numAndPfafHash.size()];
        for( int j = 0; j < numAndPfafHash.size(); j++ ) {
            lastpointAttribute[j] = last.get(j);
        }
        attributesList.add(lastpointAttribute);
        attributeClass.add(lastpointAttribute[0].getClass());

    }
    /**
     * Creates attribuetes for every geometries (at the moment netNum of the channel, channelNumber
     * and pfafstetter)
     * @throws Exception 
     */
    private void createsAttributeVectWithChannelNum() throws Exception {

        int[] flow = new int[2];
        int f = 0;
        double numValue = 0;
        double channelValue = 0;
        pm.message(msg.message("pfafstetter.extract_netnum_channel")); //$NON-NLS-1$
        // extracts netNumber and channel number for every streams
        HashMap<Double, Double> netNumAndChannelHash = new HashMap<Double, Double>();
        for( int j = 0; j < rows; j++ ) {
            for( int i = 0; i < cols; i++ ) {
                flow[0] = i;
                flow[1] = j;
                if (!isNovalue(netnumIter.getSampleDouble(j, i, 0)) && flowIter.getSampleDouble(j, i, 0) != 10.0) {
                    f = 0;
                    // looks for the source...
                    for( int k = 1; k <= 8; k++ ) {
                        if (flowIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0) == dir[k][2]
                                && !isNovalue(netnumIter.getSampleDouble(flow[0] + dir[k][0], flow[1] + dir[k][1], 0))) {
                            break;
                        } else
                            f++;
                    }
                    // if the pixel is a source...
                    if (f == 8) {
                        numValue = netnumIter.getSampleDouble(flow[0], flow[1], 0);
                        channelValue = channelIter.getSampleDouble(flow[0], flow[1], 0);
                        if (netNumAndChannelHash.get(netnumIter.getSampleDouble(flow[0], flow[1], 0)) == null) {
                            netNumAndChannelHash.put(netnumIter.getSampleDouble(flow[0], flow[1], 0),
                                    channelIter.getSampleDouble(flow[0], flow[1], 0));
                        }
                        // insert netNum and channelNum in the HashMap
                        if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                            return;
                        while( !isNovalue(flowIter.getSampleDouble(flow[0], flow[1], 0))
                                && flowIter.getSampleDouble(flow[0], flow[1], 0) != 10 ) {
                            if (netNumAndChannelHash.get(netnumIter.getSampleDouble(flow[0], flow[1], 0)) == null) {
                                netNumAndChannelHash.put(netnumIter.getSampleDouble(flow[0], flow[1], 0),
                                        channelIter.getSampleDouble(flow[0], flow[1], 0));
                            }
                            if (channelValue != channelIter.getSampleDouble(flow[0], flow[1], 0)
                                    && numValue == netnumIter.getSampleDouble(flow[0], flow[1], 0)) {
                                netNumAndChannelHash.remove(netnumIter.getSampleDouble(flow[0], flow[1], 0));
                                netNumAndChannelHash.put(netnumIter.getSampleDouble(flow[0], flow[1], 0),
                                        channelIter.getSampleDouble(flow[0], flow[1], 0));
                            }
                            if (!go_downstream(flow, flowIter.getSampleDouble(flow[0], flow[1], 0)))
                                return;
                            if (!isNovalue(netnumIter.getSampleDouble(flow[0], flow[1], 0))
                                    && !isNovalue(channelIter.getSampleDouble(flow[0], flow[1], 0))) {
                                numValue = netnumIter.getSampleDouble(flow[0], flow[1], 0);
                                channelValue = channelIter.getSampleDouble(flow[0], flow[1], 0);
                            }
                        }
                    }
                }
            }
        }

        // extracts netNumber and pfafstetter for every streams
        int index = 0;
        HashMap<Double, String> numAndPfafHash = new HashMap<Double, String>();
        String pfafValue = ""; //$NON-NLS-1$
        pm.message(msg.message("pfafstetter.extract_netnum_pfaf")); //$NON-NLS-1$
        for( ChannelInfo channelTemp : channelList ) {
            for( double numTemp : channelTemp.getNetNumComp() ) {
                pfafValue = channelTemp.getPfafValue().get(index);
                numAndPfafHash.put(numTemp, pfafValue);
                index++;
            }
            if (channelTemp.getOrigNetNumValue() != 0) {
                netNumAndChannelHash.put(channelTemp.getNetNumComp().get(0), (double) channelTemp.getOrigNetNumValue());
            }
            index = 0;
        }
        List<Double> first = new ArrayList<Double>();
        List<Double> last = new ArrayList<Double>();
        List<Geometry> geometryVectorLine = new ArrayList<Geometry>();
        for( int i = 0; i < newRiverGeometriesList.size(); i++ ) {
            geometryVectorLine.add((Geometry) newRiverGeometriesList.toArray()[i]);
        }
        // for( Geometry geom : geometryVectorLine ) {
        pm.message(msg.message("pfafstetter.extractelev")); //$NON-NLS-1$
        for( int i = 0; i < newRiverGeometriesList.size(); i++ ) {
            pm.message(msg.message("pfafstetter.processing_link") + i); //$NON-NLS-1$
            Geometry geom = newRiverGeometriesList.get(i);
            Coordinate[] coordinates = null;
            try {
                coordinates = geom.getCoordinates();
            } catch (Exception e) {
                coordinates = new Coordinate[0];
            }
            if (coordinates.length < 2) {
                pm.errorMessage(msg.message("pfafstetter.err_found_emptygeom") + (i + 1)); //$NON-NLS-1$
                first.add(doubleNovalue);
                last.add(doubleNovalue);
            } else {
                GridCoordinates2D worldToGrid = gridGeometry
                        .worldToGrid(new DirectPosition2D(coordinates[0].x, coordinates[0].y));
                int[] rowColFirst = new int[]{worldToGrid.y, worldToGrid.x};
                worldToGrid = gridGeometry.worldToGrid(new DirectPosition2D(coordinates[coordinates.length - 1].x,
                        coordinates[coordinates.length - 1].y));
                int[] rowColLast = new int[]{worldToGrid.y, worldToGrid.x};
                first.add(pitIter.getSampleDouble(rowColFirst[1], rowColFirst[0], 0));
                last.add(pitIter.getSampleDouble(rowColLast[1], rowColLast[0], 0));
            }
        }

        // ATTRIBUTES
        // create a vector of strings (it contains the name of the attributes)
        attributeName = new ArrayList<String>();
        // creates a vector of class
        attributeClass = new ArrayList<Class< ? >>();
        // creates a vector of object
        attributesList = new ArrayList<Object[]>();

        attributeName.add(NET_NUM);
        attributeName.add(CHANNEL_NUM);
        attributeName.add(PFAFSTETTER);
        attributeName.add(ELEVFIRSTPOINT);
        attributeName.add(ELEVLASTPOINT);

        // adds netNum attribute...
        Object[] netnumAttribute = new Object[numAndPfafHash.size()];
        for( int j = 0; j < numAndPfafHash.size(); j++ ) {
            netnumAttribute[j] = j + 1;
        }
        attributesList.add(netnumAttribute);
        attributeClass.add(netnumAttribute[0].getClass());

        // adds channelNumber attribute...
        Object[] channelnumAttribute = new Object[numAndPfafHash.size()];
        double indexDouble = 0;
        for( int j = 0; j < netNumAndChannelHash.size(); j++ ) {
            channelnumAttribute[j] = netNumAndChannelHash.get(indexDouble + 1);
            indexDouble++;
        }
        attributesList.add(channelnumAttribute);
        attributeClass.add(channelnumAttribute[0].getClass());

        // adds pfafstetter attribute...
        Object[] pfafstetterAttribute = new Object[numAndPfafHash.size()];
        indexDouble = 0;
        for( int j = 0; j < numAndPfafHash.size(); j++ ) {
            pfafstetterAttribute[j] = numAndPfafHash.get(indexDouble + 1);
            indexDouble++;
        }
        attributesList.add(pfafstetterAttribute);
        attributeClass.add(pfafstetterAttribute[0].getClass());

        // adds first point attribute...
        Object[] firstpointAttribute = new Object[numAndPfafHash.size()];
        for( int j = 0; j < numAndPfafHash.size(); j++ ) {
            firstpointAttribute[j] = first.get(j);
        }
        attributesList.add(firstpointAttribute);
        attributeClass.add(firstpointAttribute[0].getClass());

        // adds last point attribute...
        Object[] lastpointAttribute = new Object[numAndPfafHash.size()];
        for( int j = 0; j < numAndPfafHash.size(); j++ ) {
            lastpointAttribute[j] = last.get(j);
        }
        attributesList.add(lastpointAttribute);
        attributeClass.add(lastpointAttribute[0].getClass());

    }

}
