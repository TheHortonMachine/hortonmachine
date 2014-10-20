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
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_inPsiInitAtBedrock_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSV0_outV0_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inAlfaVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inNVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inSoilThickness_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inTheta_r_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inTheta_s_DESCRIPTION;
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
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models.OmsSafetyFactorsWorstCaseComposer;

@Description(OMSCISLAM_OMSV0_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSV0_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSV0_NAME)
@Status(OMSCISLAM_OMSV0_STATUS)
@License(OMSCISLAM_LICENSE)
public class CislamV0 extends JGTModel {
	@Description(OMSCISLAM_inSoilThickness_DESCRIPTION)
	@Unit("m")
	@In
	public String inSoilThickness = null;
	
	@Description(OMSCISLAM_inTheta_s_DESCRIPTION)
    @In
    public String inTheta_s = null;

    @Description(OMSCISLAM_inTheta_r_DESCRIPTION)
    @In
    public String inTheta_r = null;

	@Description(OMSCISLAM_OMSV0_inPsiInitAtBedrock_DESCRIPTION)
	@In
	public String inPsiInitAtBedrock = null;

	@Description(OMSCISLAM_inAlfaVanGen_DESCRIPTION)
	@In
	public String inAlfaVanGen = null;

	@Description(OMSCISLAM_inNVanGen_DESCRIPTION)
	@In
	public String inNVanGen = null;

	@Description(OMSCISLAM_OMSV0_outV0_DESCRIPTION)
	@Out
	public String outV0 = null;

	@Execute
	public void process() throws Exception {
		
    	OmsSafetyFactorsWorstCaseComposer omsModel = new OmsSafetyFactorsWorstCaseComposer();
		omsModel.inSlope = getRaster(inSlope);
		omsModel.inSoilThickness = getRaster(inSoilThickness);
		omsModel.inCohesion = getRaster(inCohesion);
		omsModel.inPhi = getRaster(inPhi);
		omsModel.inGammaSoil = getRaster(inGammaSoil);
		omsModel.inKsat = getRaster(inKsat);
		omsModel.inTheta_s = getRaster(inTheta_s);
		omsModel.inTheta_r = getRaster(inTheta_r);
		omsModel.process();
    	dumpRaster(omsModel.outSafetyactorGeoMechanic, outSafetyactorGeoMechanic);
		
	}
}
