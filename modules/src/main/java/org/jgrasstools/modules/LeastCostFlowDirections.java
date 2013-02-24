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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_doAspect_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_doSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_doTca_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_outAspect_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_outFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_outSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSLEASTCOSTFLOWDIRECTIONS_outTca_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.geomorphology.flow.OmsLeastCostFlowDirections;

@Description(OMSLEASTCOSTFLOWDIRECTIONS_DESCRIPTION)
@Author(name = OMSLEASTCOSTFLOWDIRECTIONS_AUTHORNAMES, contact = OMSLEASTCOSTFLOWDIRECTIONS_AUTHORCONTACTS)
@Keywords(OMSLEASTCOSTFLOWDIRECTIONS_KEYWORDS)
@Label(OMSLEASTCOSTFLOWDIRECTIONS_LABEL)
@Name("_" + OMSLEASTCOSTFLOWDIRECTIONS_NAME)
@Status(OMSLEASTCOSTFLOWDIRECTIONS_STATUS)
@License(OMSLEASTCOSTFLOWDIRECTIONS_LICENSE)
public class LeastCostFlowDirections extends JGTModel {
    @Description(OMSLEASTCOSTFLOWDIRECTIONS_inElev_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inElev = null;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_doTca_DESCRIPTION)
    @In
    public boolean doTca = true;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_doSlope_DESCRIPTION)
    @In
    public boolean doSlope = true;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_doAspect_DESCRIPTION)
    @In
    public boolean doAspect = true;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_outFlow_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outFlow = null;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_outTca_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outTca = null;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_outAspect_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outAspect = null;

    @Description(OMSLEASTCOSTFLOWDIRECTIONS_outSlope_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outSlope = null;

    @Execute
    public void process() throws Exception {
        OmsLeastCostFlowDirections leastcostflowdirections = new OmsLeastCostFlowDirections();
        leastcostflowdirections.inElev = getRaster(inElev);
        leastcostflowdirections.doTca = outTca != null;
        leastcostflowdirections.doSlope = outSlope != null;
        leastcostflowdirections.doAspect = outAspect != null;
        leastcostflowdirections.pm = pm;
        leastcostflowdirections.doProcess = doProcess;
        leastcostflowdirections.doReset = doReset;
        leastcostflowdirections.process();
        dumpRaster(leastcostflowdirections.outFlow, outFlow);
        dumpRaster(leastcostflowdirections.outTca, outTca);
        dumpRaster(leastcostflowdirections.outAspect, outAspect);
        dumpRaster(leastcostflowdirections.outSlope, outSlope);
    }
}
