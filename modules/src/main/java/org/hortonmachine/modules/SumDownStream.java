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

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_inFlow_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_inToSum_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_outSummed_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_pLowerThres_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSUMDOWNSTREAM_pUpperThres_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.statistics.sumdownstream.OmsSumDownStream;

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

@Description(OMSSUMDOWNSTREAM_DESCRIPTION)
@Author(name = OMSSUMDOWNSTREAM_AUTHORNAMES, contact = OMSSUMDOWNSTREAM_AUTHORCONTACTS)
@Keywords(OMSSUMDOWNSTREAM_KEYWORDS)
@Label(OMSSUMDOWNSTREAM_LABEL)
@Name("_" + OMSSUMDOWNSTREAM_NAME)
@Status(OMSSUMDOWNSTREAM_STATUS)
@License(OMSSUMDOWNSTREAM_LICENSE)
public class SumDownStream extends HMModel {

    @Description(OMSSUMDOWNSTREAM_inFlow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlow = null;

    @Description(OMSSUMDOWNSTREAM_inToSum_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inToSum = null;

    @Description(OMSSUMDOWNSTREAM_pUpperThres_DESCRIPTION)
    @In
    public Double pUpperThres = null;

    @Description(OMSSUMDOWNSTREAM_pLowerThres_DESCRIPTION)
    @In
    public Double pLowerThres = null;

    @Description(OMSSUMDOWNSTREAM_outSummed_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outSummed = null;

    @Execute
    public void process() throws Exception {
        OmsSumDownStream sumdownstream = new OmsSumDownStream();
        sumdownstream.inFlow = getRaster(inFlow);
        sumdownstream.inToSum = getRaster(inToSum);
        sumdownstream.pUpperThres = pUpperThres;
        sumdownstream.pLowerThres = pLowerThres;
        sumdownstream.pm = pm;
        sumdownstream.doProcess = doProcess;
        sumdownstream.doReset = doReset;
        sumdownstream.process();
        dumpRaster(sumdownstream.outSummed, outSummed);
    }
}
