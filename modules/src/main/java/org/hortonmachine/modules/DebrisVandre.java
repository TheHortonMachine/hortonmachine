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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_doWholenet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inObstacles_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inSlope_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inSoil_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_inTriggers_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_outIndexedTriggers_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_outPaths_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_outSoil_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_pDistance_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISVANDRE_pMode_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.debrisvandre.OmsDebrisVandre;

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

@Description(OMSDEBRISVANDRE_DESCRIPTION)
@Author(name = OMSDEBRISVANDRE_AUTHORNAMES, contact = OMSDEBRISVANDRE_AUTHORCONTACTS)
@Keywords(OMSDEBRISVANDRE_KEYWORDS)
@Label(OMSDEBRISVANDRE_LABEL)
@Name("_" + OMSDEBRISVANDRE_NAME)
@Status(OMSDEBRISVANDRE_STATUS)
@License(OMSDEBRISVANDRE_LICENSE)
public class DebrisVandre extends HMModel {

    @Description(OMSDEBRISVANDRE_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSDEBRISVANDRE_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSDEBRISVANDRE_inSlope_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("degree")
    @In
    public String inSlope = null;

    @Description(OMSDEBRISVANDRE_inTriggers_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inTriggers = null;

    @Description(OMSDEBRISVANDRE_inSoil_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSoil = null;

    @Description(OMSDEBRISVANDRE_inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(OMSDEBRISVANDRE_doWholenet_DESCRIPTION)
    @In
    public boolean doWholenet = false;

    @Description(OMSDEBRISVANDRE_pDistance_DESCRIPTION)
    @In
    @Unit("m")
    public double pDistance = 100.0;

    @Description(OMSDEBRISVANDRE_inObstacles_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inObstacles = null;

    @Description(OMSDEBRISVANDRE_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSDEBRISVANDRE_outPaths_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outPaths = null;

    @Description(OMSDEBRISVANDRE_outIndexedTriggers_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outIndexedTriggers = null;

    @Description(OMSDEBRISVANDRE_outSoil_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outSoil = null;

    @Execute
    public void process() throws Exception {
        OmsDebrisVandre debrisvandre = new OmsDebrisVandre();
        debrisvandre.inElev = getRaster(inElev);
        debrisvandre.inFlow = getRaster(inFlow);
        debrisvandre.inSlope = getRaster(inSlope);
        debrisvandre.inTriggers = getRaster(inTriggers);
        debrisvandre.inSoil = getRaster(inSoil);
        debrisvandre.inNet = getRaster(inNet);
        debrisvandre.doWholenet = doWholenet;
        debrisvandre.pDistance = pDistance;
        debrisvandre.inObstacles = getVector(inObstacles);
        debrisvandre.pMode = pMode;
        debrisvandre.pm = pm;
        debrisvandre.doProcess = doProcess;
        debrisvandre.doReset = doReset;
        debrisvandre.process();
        dumpVector(debrisvandre.outPaths, outPaths);
        dumpVector(debrisvandre.outIndexedTriggers, outIndexedTriggers);
        dumpRaster(debrisvandre.outSoil, outSoil);
    }

}
