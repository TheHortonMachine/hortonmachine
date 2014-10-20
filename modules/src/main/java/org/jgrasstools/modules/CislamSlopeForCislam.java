/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been contributed by Marco Foi (www.mcfoi.it) and Cristiano Lanni
 * 
 * "CI-slam module" is free software: you can redistribute it and/or modify
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

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSLOPEFORCISLAM_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSLOPEFORCISLAM_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSLOPEFORCISLAM_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSLOPEFORCISLAM_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSSLOPEFORCISLAM_outSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pMinSlope_DESCRIPTION;
import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Range;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models.OmsSlopeForCislam;

@Description(OMSCISLAM_OMSSLOPEFORCISLAM_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSSLOPEFORCISLAM_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSSLOPEFORCISLAM_NAME)
@Status(OMSCISLAM_OMSSLOPEFORCISLAM_STATUS)
@License(OMSCISLAM_LICENSE)
public class CislamSlopeForCislam extends JGTModel {

    @Description(OMSCISLAM_inSlope_DESCRIPTION)
    @Unit("m/m")
    @In
    public String inSlope = null;

    @Description(OMSCISLAM_inPit_DESCRIPTION)
    @Unit("m")
    @In
    public String inPit = null;
    
    @Description(OMSCISLAM_pMinSlope_DESCRIPTION)
    @Unit("m/m")
    @Range(min = 0.000001, max = 58.0)
    @In
    public double pMinSlope = 0.0090;

    @Description(OMSCISLAM_OMSSLOPEFORCISLAM_outSlope_DESCRIPTION)
    @Unit("m/m")
    @Out
    public String outSlope = null;

    @Execute
    public void process() throws Exception {
    	
    	OmsSlopeForCislam omsModel = new OmsSlopeForCislam();
		omsModel.inSlope = getRaster(inSlope);
		omsModel.inPit = getRaster(inPit);
		omsModel.pMinSlope = pMinSlope;
		omsModel.process();
    	dumpRaster(omsModel.outSlope, outSlope);        
    }
}