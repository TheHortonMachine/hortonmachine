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

import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_KEYWORDS;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_LABEL;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_LICENSE;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_NAME;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_STATUS;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_doLad_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_inFlownet_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_inPit_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_outFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_outTca_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir.OMSDRAINDIR_pLambda_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.geomorphology.draindir.OmsDrainDir;

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

@Description(OMSDRAINDIR_DESCRIPTION)
@Author(name = OMSDRAINDIR_AUTHORNAMES, contact = OMSDRAINDIR_AUTHORCONTACTS)
@Keywords(OMSDRAINDIR_KEYWORDS)
@Label(OMSDRAINDIR_LABEL)
@Name("_" + OMSDRAINDIR_NAME)
@Status(OMSDRAINDIR_STATUS)
@License(OMSDRAINDIR_LICENSE)
public class DrainDir extends HMModel {

    @Description(OMSDRAINDIR_inPit_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inPit = null;

    @Description(OMSDRAINDIR_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSDRAINDIR_inFlownet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlownet = null;

    @Description(OMSDRAINDIR_pLambda_DESCRIPTION)
    @In
    public float pLambda = 1f;

    @Description(OMSDRAINDIR_doLad_DESCRIPTION)
    @In
    public boolean doLad = true;

    @Description(OMSDRAINDIR_outFlow_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFlow = null;

    @Description(OMSDRAINDIR_outTca_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTca = null;

    @Execute
    public void process() throws Exception {
        OmsDrainDir draindir = new OmsDrainDir();
        draindir.inPit = getRaster(inPit);
        draindir.inFlow = getRaster(inFlow);
        draindir.inFlownet = getRaster(inFlownet);
        draindir.pLambda = pLambda;
        draindir.doLad = doLad;
        draindir.pm = pm;
        draindir.doProcess = doProcess;
        draindir.doReset = doReset;
        draindir.process();
        dumpRaster(draindir.outFlow, outFlow);
        dumpRaster(draindir.outTca, outTca);
    }
    
    public static void main( String[] args ) throws Exception {
        DrainDir d = new DrainDir();
        d.inPit = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_klab/hydrology/INVEST/testGura/evapotranspiration_toni/depit_gura.tif";
        d.inFlow = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_klab/hydrology/INVEST/testGura/evapotranspiration_toni/deflow_gura.tif";
        d.outFlow = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_klab/hydrology/INVEST/testGura/evapotranspiration_toni/dedrain_gura.tif";
        d.outTca = "/Users/hydrologis/Dropbox/hydrologis/lavori/2020_klab/hydrology/INVEST/testGura/evapotranspiration_toni/detca_gura.tif";
        d.process();
    }

}
