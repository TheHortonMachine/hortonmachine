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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.cislam;

import static org.jgrasstools.gears.libs.modules.JGTConstants.HYDROGEOMORPHOLOGY;

/**
 * Messages for the Horton machine.
 * 
 * ###############################
 * THE CONSTANTS HERE DEFINED HAVE TO BE MOVED INSIDE
 * 
 * org.jgrasstools.hortonmachine.i18n.HortonMessages.java
 * 
 * BEFORE PULL REQUEST
 * ###############################
 * 
 * @author Marco Foi (www.mcfoi.it)
 */
public class HortonMessages {
    
    public static final int OMSCISLAM_STATUS = oms3.annotations.Status.VALIDATED;
    
    public static final String OMSCISLAM_NAME = "cislam";
    public static final String OMSCISLAM_DESCRIPTION = "A version of the OmsCislam model.";
    public static final String OMSCISLAM_DOCUMENTATION = "OmsCislam.html";
    public static final String OMSCISLAM_KEYWORDS = "OmsCislam, Hydrology";
    public static final String OMSCISLAM_LABEL = HYDROGEOMORPHOLOGY + "/CI-slam";
    public static final String OMSCISLAM_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSCISLAM_AUTHORNAMES = "Marco Foi, Cristiano Lanni, Antonello Andrea, Franceschi Silvia, Rigon Riccardo";
    public static final String OMSCISLAM_AUTHORCONTACTS = "http://www.mcfoi.it, http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSCISLAM_SUBMODULES_LABEL = OMSCISLAM_LABEL + "/Utility Models";
    
    // Sub-models - Utility-Models DEFINITIONS
    public static final String OMSCISLAM_OMSSLOPEFORCISLAM_DESCRIPTION = "A model used internally to the OmsCislam one for computing a tailored Slope map out of the default one from the OsmSlope model. The model actually just replaces all zero values in the Slope map, with a non-zero positive value provided by the user. This is done to allow processing of flow paths with simple algorithms.";
    public static final String OMSCISLAM_OMSSLOPEFORCISLAM_NAME = "slopeforcislam";
    public static final int OMSCISLAM_OMSSLOPEFORCISLAM_STATUS = oms3.annotations.Status.EXPERIMENTAL;
    public static final String OMSCISLAM_OMSSLOPEFORCISLAM_KEYWORDS = "OmsSlopeForCislam, OmsCislam, Hydrology";
    
    public static final String OMSCISLAM_OMSSOILTHICKNESS_DESCRIPTION = "A model for providing a testing soil thickness map to the OmsCislam model. It implements the specific soilthickness-slope relation:<br><br>SoilThickness=1.006-0.85*slope<br><br>For this reason it SHOULD NOT be used as default source of soil-thickness maps. Instead a specific map should be created for each basin, evenutually relying on the OmsMapcalc model.";
    public static final String OMSCISLAM_OMSSOILTHICKNESS_NAME = "soilthickness";
    public static final int OMSCISLAM_OMSSOILTHICKNESS_STATUS = oms3.annotations.Status.EXPERIMENTAL;
    public static final String OMSCISLAM_OMSSOILTHICKNESS_KEYWORDS = "OmsSoilThickness, OmsCislam, Hydrology";
    
    public static final String OMSCISLAM_OMSVANGENUCHMAPGEN_DESCRIPTION = "A model for providing van Genuchten parameter maps, geotechnical maps and phisical parameter maps to run TESTS of the OmsCislam model.";
    public static final String OMSCISLAM_OMSVANGENUCHMAPGEN_NAME = "vangenuchtenmapsgenerator";
    public static final int OMSCISLAM_OMSVANGENUCHMAPGEN_STATUS = oms3.annotations.Status.EXPERIMENTAL;
    public static final String OMSCISLAM_OMSVANGENUCHMAPGEN_KEYWORDS = "OmsVanGenuchtenMapsGenerator, OmsCislam, Hydrology";
    
    public static final String OMSCISLAM_OMSVWT_DESCRIPTION = "A model to compute the soil mosture volume needed to produce a perched water table, so the saturation, at the soil-bedrock interface due to vertical infiltration, during a rainfall event (no lateral flow considered). The model is used as a component within the OmsCislam model.";
    public static final String OMSCISLAM_OMSVWT_NAME = "soilmoisturetowatertable";
    public static final int OMSCISLAM_OMSVWT_STATUS = oms3.annotations.Status.TESTED;
    public static final String OMSCISLAM_OMSVWT_KEYWORDS = "OmsVwt, OmsCislam, Hydrology";

    public static final String OMSCISLAM_OMSPSIINITATBEDROCK_DESCRIPTION = "A model to produce a map defining the initial suction head (psi) at soil-bedrock interface before any rainfall event. It computes a map basing on what the user provides:<ul><li>If a soil thickness map is provided the map will be computed according to the logic:<br /><em>psi = 1 - soil_thickness</em></li><li>If a <em>constant</em> > 0 is passed then psi will have the value:<br /><em>psi = constant</em></li><li>If nothing is provided, the whole basin will get a constant psi value:<br /><em>psi = 0.05</em></li></ul>";
    public static final String OMSCISLAM_OMSPSIINITATBEDROCK_NAME = "psiinitatbedrock";
    public static final int OMSCISLAM_OMSPSIINITATBEDROCK_STATUS = oms3.annotations.Status.TESTED;
    public static final String OMSCISLAM_OMSPSIINITATBEDROCK_KEYWORDS = "OmsPsiInitAtBedrock, OmsCislam, Hydrology";
    
    public static final String OMSCISLAM_OMSV0_DESCRIPTION = "A model to compute the volume of soil mosture initally contained through the soil profile before any rainfall event. The model is used as a component within the OmsCislam model.";
    public static final String OMSCISLAM_OMSV0_NAME = "soilmoisturebeforerainfall";
    public static final int OMSCISLAM_OMSV0_STATUS = oms3.annotations.Status.TESTED;
    public static final String OMSCISLAM_OMSV0_KEYWORDS = "OmsV0, OmsCislam, Hydrology";
    
    public static final String OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_DESCRIPTION = "A model to produce a geo-technical Safety Factor map. This calculation does not take into account hydrologic factors that may negatively affect the result. This means that map areas that do not even stand the test of this geo-technical safety factor will surely result in not stable areas even when hydrology is taken into account.";
    public static final String OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_NAME = "safetyfactorgeomechanic";
    public static final int OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_STATUS = oms3.annotations.Status.TESTED;
    public static final String OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_KEYWORDS = "OmsSafetyFactorGeomechanic, OmsCislam, Hydrology";
    
    public static final String OMSCISLAM_OMSSAFETYFACTORCOMPOSER_DESCRIPTION = "A model to compose a set of Safety Factor maps created for the same Return Time but for different rainfallon durations. The model uses a worst-case logic so that the lowest values from each map is set in the output, cell by cell.";
    public static final String OMSCISLAM_OMSSAFETYFACTORCOMPOSER_NAME = "safetyfactorsworstcasecomposer";
    public static final int OMSCISLAM_OMSSAFETYFACTORCOMPOSER_STATUS = oms3.annotations.Status.TESTED;
    public static final String OMSCISLAM_OMSSAFETYFACTORCOMPOSER_KEYWORDS = "OmsSafetyFactorsWorstCaseComposer, OmsCislam, Hydrology";
    
    
    // Output-related parameters
    public static final String OMSCISLAM_pOutFolder_DESCRIPTION = "The folder in which results will be deployed along with by-products, if specified.";
    public static final String OMSCISLAM_doSaveByproducts_DESCRIPTION = "Save processing byproducts as ASCII grids.";
    public static final String OMSCISLAM_doCalculateGeoMechanicSafetyFactor_DESCRIPTION = "Calculate (and save) geo-mechanic only Safety Factor (uses OmsSafetyFactorGeomechanic model).";
    public static final String OMSCISLAM_doCalculateInitialSafetyFactor_DESCRIPTION = "Calculate (and save) initial hydrologic Safety Factor.";
    
    // Input PARAMETERS
    public static final String OMSCISLAM_pMinSlope_DESCRIPTION = "The non-zero value to replace in the zero cells.";
    public static final String OMSCISLAM_pReturnTimes_DESCRIPTION = "List of comma-separated return times (e.g.: 30,100,200 ).";
    public static final String OMSCISLAM_pRainfallDurations_DESCRIPTION = "List of comma-separated simulated rainfall durations (e.g.: 1,3,6,12,25 ).";
    
    // Input MAPS
    public static final String OMSCISLAM_inSoilThickness_DESCRIPTION = "Map of Soil Thickness. (A sample map can be computed with the experimental OmsSoilThickness model but should not be used for any basin as it implements a specific soil-slope relation suitable just for specific environments).";
    public static final String OMSCISLAM_inPsiInitAtBedrock_DESCRIPTION = "Map of initial Psi value (suction head at soil-bedrock interface). Can be created with OmsPsiInitAtBedrock model.";
    public static final String OMSCISLAM_inSlope_DESCRIPTION = "Map of Slope (from OmsSlope model).";
    public static final String OMSCISLAM_inPit_DESCRIPTION = "Map of Depitted Elevations (from OmsPit model).";
    public static final String OMSCISLAM_inFlow_DESCRIPTION = "Map of Flow Directions  (from OmsFlowDirections model).";
    public static final String OMSCISLAM_inAb_DESCRIPTION = "Map of Area per Length (from OmsAb model).";

    // van Genuchten parameters/maps
    public static final String OMSCISLAM_inGeo_DESCRIPTION = "Map of classified Geological/Pedological areas.";
    public static final String OMSCISLAM_inCohesion_DESCRIPTION = "Map of Cohesion.";
    public static final String OMSCISLAM_outCohesion_DESCRIPTION = OMSCISLAM_inCohesion_DESCRIPTION;
    public static final String OMSCISLAM_pCohesion_DESCRIPTION = "A Cohesion value, to be used as a constant for the whole basin (alternative to map).";
    public static final String OMSCISLAM_inPhi_DESCRIPTION = "Map of effective frictional angle (Phi).";
    public static final String OMSCISLAM_outPhi_DESCRIPTION = OMSCISLAM_inPhi_DESCRIPTION;
    public static final String OMSCISLAM_pPhi_DESCRIPTION = "An effective frictional angle value (Phi), to be used as a constant for the whole basin (alternative to map).";
    public static final String OMSCISLAM_inGamma_DESCRIPTION = "Map of Soil Gamma (density).";
    public static final String OMSCISLAM_outGamma_DESCRIPTION = OMSCISLAM_inGamma_DESCRIPTION;
    public static final String OMSCISLAM_pGamma_DESCRIPTION = "A Soil Gamma value (density), to be used as a constant for the whole basin (alternative to map).";
    public static final String OMSCISLAM_inKsat_DESCRIPTION = "Map of Saturated Hydraulic Conductivity (Ksat)";
    public static final String OMSCISLAM_outKsat_DESCRIPTION = OMSCISLAM_inKsat_DESCRIPTION;
    public static final String OMSCISLAM_pKsat_DESCRIPTION = "A Saturated Hydraulic Conductivity (Ksat) value, to be used as a constant for the whole basin (alternative to map).";
    public static final String OMSCISLAM_inTheta_s_DESCRIPTION = "Map of saturated water content (Theta S): saturated water content.";
    public static final String OMSCISLAM_outTheta_s_DESCRIPTION = OMSCISLAM_inTheta_s_DESCRIPTION;
    public static final String OMSCISLAM_pTheta_s_DESCRIPTION = "A Theta S value, to be used as a constant for the whole basin (alternative to map).";
    public static final String OMSCISLAM_inTheta_r_DESCRIPTION = "Map of residual water content (Theta R): residual water content.";
    public static final String OMSCISLAM_outTheta_r_DESCRIPTION = OMSCISLAM_inTheta_r_DESCRIPTION;
    public static final String OMSCISLAM_pTheta_r_DESCRIPTION = "A residual water content value (Theta R) , to be used as a constant for the whole basin (alternative to map).";
    public static final String OMSCISLAM_inAlfaVanGen_DESCRIPTION = "Map of Alfa (van Genuchten parm.).";
    public static final String OMSCISLAM_outAlfaVanGen_DESCRIPTION = OMSCISLAM_inAlfaVanGen_DESCRIPTION;
    public static final String OMSCISLAM_pAlfaVanGen_DESCRIPTION = "An Alfa value (van Genuchten), to be used as a constant for the whole basin (alternative to map).";
    public static final String OMSCISLAM_inNVanGen_DESCRIPTION = "Map of n (van Genuchten parm.).";
    public static final String OMSCISLAM_outNVanGen_DESCRIPTION = OMSCISLAM_inNVanGen_DESCRIPTION;
    public static final String OMSCISLAM_pNVanGen_DESCRIPTION = "An n value (van Genuchten), to be used as a constant for the whole basin (alternative to map).";
    
    // Rainfall statistical parameters
    public static final String OMSCISLAM_pSigma1_DESCRIPTION = "Sigma1: parameter derived by linear regression of expectations of rainfall depth vs. duration.";
    public static final String OMSCISLAM_pmF_DESCRIPTION = "mF: parameter derived by linear regression of expectations of rainfall depth vs. duration.";
    public static final String OMSCISLAM_pCV_DESCRIPTION = "CV: Coefficient of Variation of rainfalls.";
    
    // Submodels Inputs-Outputs
    public static final String OMSCISLAM_OMSV0_inPsiInitAtBedrock_DESCRIPTION = "Map of initial Psi value (suction head at soil-bedrock interface). (Can be created with OmsPsiInitAtBedrock model)";
    public static final String OMSCISLAM_OMSV0_outV0_DESCRIPTION = "Map of initial soil moisture volume throgh the soil profile before a rainfall event.";
    
    public static final String OMSCISLAM_OMSVWT_outVwt_DESCRIPTION = "Map of soil moisture volume needed to produce a perched water table - zero-pressure head - at the soil-bedrock interface.";
    
    public static final String OMSCISLAM_OMSSLOPEFORCISLAM_inSlope_DESCRIPTION = "Map of Slope with no zero values and recomputed border (from OmsSlopeForCislam model).";
    public static final String OMSCISLAM_OMSSLOPEFORCISLAM_outSlope_DESCRIPTION = "Map of Slope for OsmCislam model (no zero values and with value-bearing border).";
    public static final String OMSCISLAM_OMSSOILTHICKNESS_outSoilThickness_DESCRIPTION = "Map of Soil thickness.";
    
    public static final String OMSCISLAM_OMSPSIINITATBEDROCK_pPsiInitAtBedrockConstant_DESCRIPTION = "An initial suction head value at soil-bedrock interface (psi), alternative to the soil thickness map, to be used as a constant for the whole basin.";
    public static final String OMSCISLAM_OMSPSIINITATBEDROCK_outPsiInitAtBedrock_DESCRIPTION = "Map of initial Psi value (suction head at soil-bedrock interface).";
    
    public static final String OMSCISLAM_OMSAFETYFACTORGEOMECHANIC_outSafetyactorGeoMechanic_DESCRIPTION = "Map of Safety Factor taking into account just geo-mechanical parameter. No suction head due to soil moisture or rainfall effect are considered.";
    
    public static final String OMSCISLAM_OMSSAFETYFACTORCOMPOSER_inRasters_DESCRIPTION = "List of Safety Factor maps computed for the same Return Time but different Rainfall Durations";
    public static final String OMSCISLAM_OMSSAFETYFACTORCOMPOSER_pReturnTime_DESCRIPTION = "Return Time for which the computation was run.";
    public static final String OMSCISLAM_OMSSAFETYFACTORCOMPOSER_outSafetyFactorTotal_DESCRIPTION = "Worst-case composed Safety Factor map.";

}
