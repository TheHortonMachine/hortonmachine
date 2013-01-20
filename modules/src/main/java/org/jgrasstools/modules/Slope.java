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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_doHandleNegativeSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSSLOPE_outSlope_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.geomorphology.slope.OmsSlope;

@Description(OMSSLOPE_DESCRIPTION)
@Author(name = OMSSLOPE_AUTHORNAMES, contact = OMSSLOPE_AUTHORCONTACTS)
@Keywords(OMSSLOPE_KEYWORDS)
@Label(OMSSLOPE_LABEL)
@Name("_" + OMSSLOPE_NAME)
@Status(OMSSLOPE_STATUS)
@License(OMSSLOPE_LICENSE)
public class Slope extends JGTModel {
    @Description(OMSSLOPE_inPit_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inPit = null;

    @Description(OMSSLOPE_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description(OMSSLOPE_doHandleNegativeSlope_DESCRIPTION)
    @In
    public boolean doHandleNegativeSlope;

    @Description(OMSSLOPE_outSlope_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outSlope = null;

    @Execute
    public void process() throws Exception {
        OmsSlope slope = new OmsSlope();
        slope.inPit = getRaster(inPit);
        slope.inFlow = getRaster(inFlow);
        slope.doHandleNegativeSlope = doHandleNegativeSlope;
        slope.pm = pm;
        slope.doProcess = doProcess;
        slope.doReset = doReset;
        slope.process();
        dumpRaster(slope.outSlope, outSlope);
    }
}
