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
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_outDistance_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSDISTANCETOOUTLET_pMode_DESCRIPTION;
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
import org.jgrasstools.hortonmachine.modules.network.distancetooutlet.OmsDistanceToOutlet;

@Description(OMSDISTANCETOOUTLET_DESCRIPTION)
@Author(name = OMSDISTANCETOOUTLET_AUTHORNAMES, contact = OMSDISTANCETOOUTLET_AUTHORCONTACTS)
@Keywords(OMSDISTANCETOOUTLET_KEYWORDS)
@Label(OMSDISTANCETOOUTLET_LABEL)
@Name("_" + OMSDISTANCETOOUTLET_NAME)
@Status(OMSDISTANCETOOUTLET_STATUS)
@License(OMSDISTANCETOOUTLET_LICENSE)
public class DistanceToOutlet extends JGTModel {

    @Description(OMSDISTANCETOOUTLET_inPit_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inPit = null;

    @Description(OMSDISTANCETOOUTLET_inFlow_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inFlow = null;

    @Description(OMSDISTANCETOOUTLET_pMode_DESCRIPTION)
    @In
    public int pMode;

    @Description(OMSDISTANCETOOUTLET_outDistance_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outDistance = null;

    @Execute
    public void process() throws Exception {
        OmsDistanceToOutlet distancetooutlet = new OmsDistanceToOutlet();
        distancetooutlet.inPit = getRaster(inPit);
        distancetooutlet.inFlow = getRaster(inFlow);
        distancetooutlet.pMode = pMode;
        distancetooutlet.pm = pm;
        distancetooutlet.doProcess = doProcess;
        distancetooutlet.doReset = doReset;
        distancetooutlet.process();
        dumpRaster(distancetooutlet.outDistance, outDistance);
    }

}
