/* This file is part of HortonMachine (http://www.hortonmachine.org)
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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_fStationsZ_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_fStationsid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_inData_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_inStations_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_outResult_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_pCutoff_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_pPath_DESCRIPTION;

import java.util.HashMap;

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
import oms3.annotations.UI;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.statistics.kriging.old.OmsVariogram;

@Description(OMSVARIOGRAM_DESCRIPTION)
@Author(name = OMSVARIOGRAM_AUTHORNAMES, contact = OMSVARIOGRAM_AUTHORCONTACTS)
@Keywords(OMSVARIOGRAM_KEYWORDS)
@Label(OMSVARIOGRAM_LABEL)
@Name("_" + OMSVARIOGRAM_NAME)
@Status(OMSVARIOGRAM_STATUS)
@License(OMSVARIOGRAM_LICENSE)
@UI(HMConstants.HIDE_UI_HINT)
public class Variogram extends HMModel {

    @Description(OMSVARIOGRAM_inStations_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inStations = null;

    @Description(OMSVARIOGRAM_fStationsid_DESCRIPTION)
    @In
    public String fStationsid = null;

    @Description(OMSVARIOGRAM_fStationsZ_DESCRIPTION)
    @In
    public String fStationsZ = null;

    @Description(OMSVARIOGRAM_inData_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inData = null;

    @Description(OMSVARIOGRAM_pPath_DESCRIPTION)
    @In
    public String pPath = null;

    @Description(OMSVARIOGRAM_pCutoff_DESCRIPTION)
    @In
    public double pCutoff;

    @Description(OMSVARIOGRAM_outResult_DESCRIPTION)
    @Out
    public double[][] outResult = null;

    @Execute
    public void process() throws Exception {
        OmsVariogram variogram = new OmsVariogram();
        variogram.inStations = getVector(inStations);
        variogram.fStationsid = fStationsid;
        variogram.fStationsZ = fStationsZ;
        variogram.inData = inData;
        variogram.pPath = pPath;
        variogram.pCutoff = pCutoff;
        variogram.pm = pm;
        variogram.doProcess = doProcess;
        variogram.doReset = doReset;
        variogram.process();
        outResult = variogram.outResult;

    }

}