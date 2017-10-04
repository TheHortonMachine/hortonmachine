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


import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller.OMSPITFILLER_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller.OMSPITFILLER_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller.OMSPITFILLER_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller.OMSPITFILLER_KEYWORDS;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller.OMSPITFILLER_LABEL;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller.OMSPITFILLER_LICENSE;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller.OMSPITFILLER_NAME;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller.OMSPITFILLER_STATUS;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller.OMSPITFILLER_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller.OMSPITFILLER_outPit_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.demmanipulation.pitfiller.OmsPitfiller;

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

@Description(OMSPITFILLER_DESCRIPTION)
@Author(name = OMSPITFILLER_AUTHORNAMES, contact = OMSPITFILLER_AUTHORCONTACTS)
@Keywords(OMSPITFILLER_KEYWORDS)
@Label(OMSPITFILLER_LABEL)
@Name("_" + OMSPITFILLER_NAME)
@Status(OMSPITFILLER_STATUS)
@License(OMSPITFILLER_LICENSE)
public class Pitfiller extends HMModel {
    @Description(OMSPITFILLER_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev;

    @Description(OMSPITFILLER_outPit_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outPit = null;

    @Execute
    public void process() throws Exception {
        OmsPitfiller pitfiller = new OmsPitfiller();
        pitfiller.inElev = getRaster(inElev);
        pitfiller.pm = pm;
        pitfiller.doProcess = doProcess;
        pitfiller.doReset = doReset;
        pitfiller.process();
        dumpRaster(pitfiller.outPit, outPit);
    }
}
