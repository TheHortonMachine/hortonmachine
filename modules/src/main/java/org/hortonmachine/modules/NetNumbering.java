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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_fPointId_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_inPoints_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_inTca_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_outBasins_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_outNetnum_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSNETNUMBERING_pThres_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
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

    @Description(OMSNETNUMBERING_pThres_DESCRIPTION)
    @In
    public int pThres = 0;

    @Description(OMSNETNUMBERING_fPointId_DESCRIPTION)
    @In
    public String fPointId = null;

    @Description(OMSNETNUMBERING_outNetnum_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNetnum = null;

    @Description(OMSNETNUMBERING_outBasins_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outBasins = null;

    @Execute
    public void process() throws Exception {
        OmsNetNumbering omsnetnumbering = new OmsNetNumbering();
        omsnetnumbering.inFlow = getRaster(inFlow);
        omsnetnumbering.inTca = getRaster(inTca);
        omsnetnumbering.inNet = getRaster(inNet);
        omsnetnumbering.inPoints = getVector(inPoints);
        omsnetnumbering.pThres = pThres;
        omsnetnumbering.pm = pm;
        omsnetnumbering.doProcess = doProcess;
        omsnetnumbering.doReset = doReset;
        omsnetnumbering.process();
        dumpRaster(omsnetnumbering.outNetnum, outNetnum);
        dumpRaster(omsnetnumbering.outBasins, outBasins);
    }
}