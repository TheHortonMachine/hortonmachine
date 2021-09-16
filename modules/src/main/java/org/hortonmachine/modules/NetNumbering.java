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
package org.hortonmachine.modules;

import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_KEYWORDS;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_LABEL;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_LICENSE;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_NAME;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_STATUS;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_desiredAreaDelta_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_desiredArea_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_inPoints_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_inTca_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_outBasinsInfo_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_outBasins_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_outDesiredBasins_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_outGeoframeTopology_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_outMindmapDesired_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_outMindmap_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering.OMSNETNUMBERING_outNetnum_DESCRIPTION;

import java.io.File;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.files.FileUtilities;
import org.hortonmachine.hmachine.modules.network.netnumbering.OmsNetNumbering;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description(OMSNETNUMBERING_DESCRIPTION)
@Author(name = OMSNETNUMBERING_AUTHORNAMES, contact = OMSNETNUMBERING_AUTHORCONTACTS)
@Keywords(OMSNETNUMBERING_KEYWORDS)
@Label(OMSNETNUMBERING_LABEL)
@Name("_" + OMSNETNUMBERING_NAME)
@Status(OMSNETNUMBERING_STATUS)
@License(OMSNETNUMBERING_LICENSE)
public class NetNumbering extends HMModel {

    @Description(OMSNETNUMBERING_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSNETNUMBERING_inTca_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTca = null;

    @Description(OMSNETNUMBERING_inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(OMSNETNUMBERING_inPoints_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inPoints = null;

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
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetnum = null;

    @Description(OMSNETNUMBERING_outBasins_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outBasins = null;

    @Description(OMSNETNUMBERING_outDesiredBasins_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDesiredBasins = null;

    @Description(OMSNETNUMBERING_outMindmap_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMindmap = null;

    @Description(OMSNETNUMBERING_outMindmapDesired_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outDesiredMindmap = null;

    @Description(OMSNETNUMBERING_outGeoframeTopology_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outGeoframeTopology = null;

    @Description(OMSNETNUMBERING_outBasinsInfo_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outBasinsInfo = null;

    @Execute
    public void process() throws Exception {
        OmsNetNumbering omsnetnumbering = new OmsNetNumbering();
        omsnetnumbering.inFlow = getRaster(inFlow);
        omsnetnumbering.inTca = getRaster(inTca);
        omsnetnumbering.inNet = getRaster(inNet);
        omsnetnumbering.inPoints = getVector(inPoints);
        omsnetnumbering.pDesiredArea = pDesiredArea;
        omsnetnumbering.pDesiredAreaDelta = pDesiredAreaDelta;
        // omsnetnumbering.pMaxAllowedConfluences = pMaxAllowedConfluences;
        omsnetnumbering.outMindmap = outMindmap;
        omsnetnumbering.doProcess = doProcess;
        omsnetnumbering.doReset = doReset;
        omsnetnumbering.process();
        dumpRaster(omsnetnumbering.outNetnum, outNetnum);
        dumpRaster(omsnetnumbering.outBasins, outBasins);
        dumpRaster(omsnetnumbering.outDesiredBasins, outDesiredBasins);

        if (outMindmap != null && outMindmap.trim().length() > 0) {
            FileUtilities.writeFile(omsnetnumbering.outMindmap, new File(outMindmap));
        }
        if (pDesiredArea != null && outDesiredMindmap != null && outDesiredMindmap.trim().length() > 0) {
            FileUtilities.writeFile(omsnetnumbering.outDesiredMindmap, new File(outDesiredMindmap));
        }
        if (outGeoframeTopology != null && omsnetnumbering.outGeoframeTopology != null
                && omsnetnumbering.outGeoframeTopology.trim().length() > 0) {
            FileUtilities.writeFile(omsnetnumbering.outGeoframeTopology, new File(outGeoframeTopology));
        }
        if (outBasinsInfo != null && omsnetnumbering.outBasinsInfo != null && omsnetnumbering.outBasinsInfo.trim().length() > 0) {
            FileUtilities.writeFile(omsnetnumbering.outBasinsInfo, new File(outBasinsInfo));
        }
    }

    public static void main( String[] args ) throws Exception {
        String folder = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_projects/15_uniTN_basins/brenta/brenta_063basins_5M_20/";
        String inPoints = null;// folder + "idroStation.shp";

        Double desiredArea = 5_000_000.0;
        Double desiredDelta = 20.0;
        // int maxChannels = -1;

        String inFlow = folder + "brenta_drain.asc";
        String inTca = folder + "brenta_tca.asc";
        String inNet = folder + "brenta_net_10000.asc";
        String withPoints = inPoints == null ? "" : "_pts";

        String outNetnum = folder + "mytest" + withPoints + "_netnum.asc";
        String outBasins = folder + "mytest" + withPoints + "_basins.asc";
        String extra = "";
        if (desiredArea != null) {
            extra = "_desiredbasins_" + desiredArea + "_" + desiredDelta;
        }

        String outDesireredBasins = folder + "mytest" + withPoints + extra + ".asc";
        String outMM = folder + "mytest" + withPoints + "_mindmap.txt";
        String outDesMM = folder + "mytest" + withPoints + "_mindmap" + extra + ".txt";
        String outGeoframe = folder + "mytest" + withPoints + "_geoframe.txt";
        String outBasinInfo = folder + "mytest" + withPoints + "_basininfo.txt";
        NetNumbering omsnetnumbering = new NetNumbering();
        omsnetnumbering.inFlow = inFlow;
        omsnetnumbering.inTca = inTca;
        omsnetnumbering.inNet = inNet;
        omsnetnumbering.pDesiredArea = desiredArea;
        omsnetnumbering.pDesiredAreaDelta = desiredDelta;
        // omsnetnumbering.pMaxAllowedConfluences = maxChannels;

        if (inPoints != null) {
            omsnetnumbering.inPoints = inPoints;
        }
        omsnetnumbering.outMindmap = outMM;
        omsnetnumbering.outDesiredMindmap = outDesMM;
        omsnetnumbering.outGeoframeTopology = outGeoframe;
        omsnetnumbering.outBasinsInfo = outBasinInfo;
        omsnetnumbering.outBasins = outBasins;
        omsnetnumbering.outDesiredBasins = outDesireredBasins;
        omsnetnumbering.outNetnum = outNetnum;
        omsnetnumbering.process();

    }
}