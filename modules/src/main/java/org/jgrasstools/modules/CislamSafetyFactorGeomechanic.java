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
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_outSafetyactorGeoMechanic_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_SUBMODULES_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inCohesion_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inGamma_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inKsat_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inPhi_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inSlope_DESCRIPTION;
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
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.utility_models.OmsSafetyFactorGeomechanic;

@Description(OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_KEYWORDS)
@Label(OMSCISLAM_SUBMODULES_LABEL)
@Name(OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_NAME)
@Status(OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_STATUS)
@License(OMSCISLAM_LICENSE)
public class CislamSafetyFactorGeomechanic extends JGTModel {

	@Description(OMSCISLAM_inSlope_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inSlope = null;

	@Description(OMSCISLAM_inSoilThickness_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@Unit("m")
	@In
	public String inSoilThickness = null;

	// #############################################
	// Geo-Techical parameters
	// #############################################
	@Description(OMSCISLAM_inCohesion_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@Unit("kPa")
	@In
	public String inCohesion = null;

	@Description(OMSCISLAM_inPhi_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inPhi = null;

	@Description(OMSCISLAM_inGamma_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inGammaSoil = null;

	@Description(OMSCISLAM_inKsat_DESCRIPTION)
	@Unit("m/s")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inKsat = null;

	@Description(OMSCISLAM_inTheta_s_DESCRIPTION)
	@Unit("-")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inTheta_s = null;

	@Description(OMSCISLAM_inTheta_r_DESCRIPTION)
	@Unit("-")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inTheta_r = null;

	@Description(OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_outSafetyactorGeoMechanic_DESCRIPTION)
	@Unit("-")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@Out
	public String outSafetyactorGeoMechanic = null;

	@Execute
	public void process() throws Exception {

		OmsSafetyFactorGeomechanic omsModel = new OmsSafetyFactorGeomechanic();
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
