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
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSVANGENUCHMAPGEN_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSVANGENUCHMAPGEN_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSVANGENUCHMAPGEN_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSVANGENUCHMAPGEN_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inGeo_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_outAlfaVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_outCohesion_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_outGamma_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_outKsat_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_outNVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_outPhi_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_outTheta_r_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_outTheta_s_DESCRIPTION;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models.OmsVanGenuchtenMapsGenerator;

@Description(OMSCISLAM_OMSVANGENUCHMAPGEN_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSVANGENUCHMAPGEN_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSVANGENUCHMAPGEN_NAME)
@Status(OMSCISLAM_OMSVANGENUCHMAPGEN_STATUS)
@License(OMSCISLAM_LICENSE)
public class CislamVanGenuchtenMapsGenerator extends JGTModel {

    @Description(OMSCISLAM_inGeo_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inGeo = null;

    @Description(OMSCISLAM_outCohesion_DESCRIPTION)
    @Out
    public String outCohesion = null;

    @Description(OMSCISLAM_outPhi_DESCRIPTION)
    @Out
    public String outPhi = null;

    @Description(OMSCISLAM_outGamma_DESCRIPTION)
    @Out
    public String outGamma = null;

    @Description(OMSCISLAM_outKsat_DESCRIPTION)
    @Out
    public String outKsat = null;

    @Description(OMSCISLAM_outTheta_s_DESCRIPTION)
    @Out
    public String outTheta_s = null;

    @Description(OMSCISLAM_outTheta_r_DESCRIPTION)
    @Out
    public String outTheta_r = null;

    @Description(OMSCISLAM_outAlfaVanGen_DESCRIPTION)
    @Out
    public String outAlfaVanGen = null;

    @Description(OMSCISLAM_outNVanGen_DESCRIPTION)
    @Out
    public String outNVanGen = null;

    @Execute
    public void process() throws Exception {
    	
    	OmsVanGenuchtenMapsGenerator omsModel = new OmsVanGenuchtenMapsGenerator();
		omsModel.inGeo = getRaster(inGeo);
		omsModel.process();
    	dumpRaster(omsModel.outCohesion, outCohesion);
    	dumpRaster(omsModel.outPhi, outPhi);
    	dumpRaster(omsModel.outGamma, outGamma);
    	dumpRaster(omsModel.outKsat, outKsat);
    	dumpRaster(omsModel.outTheta_s, outTheta_s);
    	dumpRaster(omsModel.outTheta_r, outTheta_r);
    	dumpRaster(omsModel.outAlfaVanGen, outAlfaVanGen);
    	dumpRaster(omsModel.outNVanGen, outNVanGen);     
    }
}
