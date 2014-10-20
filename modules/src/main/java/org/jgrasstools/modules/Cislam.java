/*
 * This file is part of the "CI-slam module": an addition to JGrassTools
 * It has been entirely contributed by Marco Foi (www.mcfoi.it)
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

import org.jgrasstools.gears.libs.modules.JGTModel;

import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inAb_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inAlfaVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inCohesion_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inFlow_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inGamma_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inKsat_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inNVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inPhi_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inPit_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inPsiInitAtBedrock_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inSoilThickness_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inTheta_r_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_inTheta_s_DESCRIPTION;
//import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pAlfaVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pCV_DESCRIPTION;
//import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pCohesion_DESCRIPTION;
//import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pGamma_DESCRIPTION;
//import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pKsat_DESCRIPTION;
//import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pNVanGen_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pOutFolder_DESCRIPTION;
//import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pPhi_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pRainfallDurations_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pReturnTimes_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_doSaveByproducts_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pSigma1_DESCRIPTION;
//import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pTheta_r_DESCRIPTION;
//import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pTheta_s_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSCISLAM_pmF_DESCRIPTION;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam.OmsCislam;

/**
 * 
 * @author Marco Foi (www.mcfoi.it)
 * 
 */
@Description(OMSCISLAM_DESCRIPTION)
@Author(name = OMSCISLAM_AUTHORNAMES, contact = OMSCISLAM_AUTHORCONTACTS)
@Keywords(OMSCISLAM_KEYWORDS)
@Label(OMSCISLAM_LABEL)
@Name("_" + OMSCISLAM_NAME)
@Status(OMSCISLAM_STATUS)
@License(OMSCISLAM_LICENSE)
public class Cislam extends JGTModel {

	// Default minimum slope value used as a substitute for Slope Map values
	// equal to zero. Required to avoid division by zero.
	public static final double MINIMM_ALLOWED_SLOPE = 0.009;
	// Default rainfall durations used to compute maps of times to develop water
	// table.
	private static final String RAINFALL_DURATIONS_ARRAY = "1, 3, 6, 12, 24";

	@Description(OMSCISLAM_pReturnTimes_DESCRIPTION)
	@Unit("Years")
	@In
	public String pReturnTimes = new String("30, 100, 200");

	@Description(OMSCISLAM_pRainfallDurations_DESCRIPTION)
	@Unit("Hours")
	@In
	public String pRainfallDurations = RAINFALL_DURATIONS_ARRAY;

	// Input files
	@Description(OMSCISLAM_inPit_DESCRIPTION)
	@Unit("m")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inPit = null;

	@Description(OMSCISLAM_inFlow_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inFlow = null;

	@Description(OMSCISLAM_inSlope_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inSlope = null;

	@Description(OMSCISLAM_inAb_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inAb = null;

	@Description(OMSCISLAM_inSoilThickness_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@Unit("m")
	@In
	public String inSoilThickness = null;

	// #############################################
	// Rainfall-statistics related parameters
	// #############################################
	@Description(OMSCISLAM_pSigma1_DESCRIPTION)
	@In
	public double pSigma1 = 11.82;

	@Description(OMSCISLAM_pmF_DESCRIPTION)
	@In
	public double pmF = 0.54;

	@Description(OMSCISLAM_pCV_DESCRIPTION)
	@In
	public double pCV = 0.23;

	// #############################################
	// Hydrologic parameters
	// #############################################
	@Description(OMSCISLAM_inPsiInitAtBedrock_DESCRIPTION)
	@Unit("m")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inPsiInitAtBedrock = null;

	// #############################################
	// van Genuchten parameters
	// #############################################
	@Description(OMSCISLAM_inCohesion_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@Unit("kPa")
	@In
	public String inCohesion = null;
	/*
	 * @Description(OMSCISLAM_pCohesion_DESCRIPTION)
	 * 
	 * @Unit("Pa")
	 * 
	 * @In public double pCohesion = -1.0;
	 */
	@Description(OMSCISLAM_inPhi_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inPhi = null;
	/*
	 * @Description(OMSCISLAM_pPhi_DESCRIPTION)
	 * 
	 * @In public double pPhi = 30.0;
	 */
	@Description(OMSCISLAM_inGamma_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inGammaSoil = null;
	/*
	 * @Description(OMSCISLAM_pGamma_DESCRIPTION)
	 * 
	 * @In public double pGamma = 19.0;
	 */
	@Description(OMSCISLAM_inKsat_DESCRIPTION)
	@Unit("m/s")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inKsat = null;
	/*
	 * @Description(OMSCISLAM_pKsat_DESCRIPTION)
	 * 
	 * @In public double pKsat_ = 10 ^ (-6);
	 */
	@Description(OMSCISLAM_inTheta_s_DESCRIPTION)
	@Unit("-")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inTheta_s = null;
	/*
	 * @Description(OMSCISLAM_pTheta_s_DESCRIPTION)
	 * 
	 * @In public double pTheta_s = 0.3;
	 */
	@Description(OMSCISLAM_inTheta_r_DESCRIPTION)
	@Unit("-")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inTheta_r = null;
	/*
	 * @Description(OMSCISLAM_pTheta_r_DESCRIPTION)
	 * 
	 * @In public double pTheta_r = 0.03;
	 */
	@Description(OMSCISLAM_inAlfaVanGen_DESCRIPTION)
	@Unit("1/m")
	@UI(JGTConstants.FILEIN_UI_HINT)
	@In
	public String inAlfaVanGen = null;
	/*
	 * @Description(OMSCISLAM_pAlfaVanGen_DESCRIPTION)
	 * 
	 * @In public double pAlfaVanGen = 6.5;
	 */
	@Description(OMSCISLAM_inNVanGen_DESCRIPTION)
	@UI(JGTConstants.FILEIN_UI_HINT)
	@Unit("-")
	@In
	public String inNVanGen = null;
	/*
	 * @Description(OMSCISLAM_pNVanGen_DESCRIPTION)
	 * 
	 * @Unit("-")
	 * 
	 * @In public double pNVanGen = 1.36;
	 */

	// #############################################
	// Save by-products to file
	// #############################################
	@Description(OMSCISLAM_doSaveByproducts_DESCRIPTION)
	@In
	public boolean doSaveByProducts = false;

	@Description(OMSCISLAM_pOutFolder_DESCRIPTION)
	@UI(JGTConstants.FOLDEROUT_UI_HINT)
	@In
	public String pOutFolder = System.getProperty("java.io.tmpdir");

	@Execute
	public void process() throws Exception {
		OmsCislam omscislam = new OmsCislam();

		omscislam.pOutFolder = pOutFolder;

		omscislam.doSaveByProducts = doSaveByProducts;
		
		omscislam.pReturnTimes = pReturnTimes;
		omscislam.pRainfallDurations = pRainfallDurations;

		omscislam.inPit = getRaster(inPit);
		omscislam.inFlow = getRaster(inFlow);
		omscislam.inSlope = getRaster(inSlope);
		omscislam.inAb = getRaster(inAb);
		omscislam.inSoilThickness = getRaster(inSoilThickness);
		omscislam.inTheta_s = getRaster(inTheta_s);
		omscislam.inTheta_r = getRaster(inTheta_r);
		omscislam.inPsiInitAtBedrock = getRaster(inPsiInitAtBedrock);
		omscislam.inAlfaVanGen = getRaster(inAlfaVanGen);
		omscislam.inNVanGen = getRaster(inNVanGen);
		omscislam.inKsat = getRaster(inKsat);
		omscislam.inGammaSoil = getRaster(inGammaSoil);
		omscislam.inCohesion = getRaster(inCohesion);
		omscislam.inPhi = getRaster(inPhi);

		omscislam.pSigma1 = pSigma1;
		omscislam.pmF = pmF;
		omscislam.pCV = pCV;

		omscislam.process();
	}
}
