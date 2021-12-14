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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.etp;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.IDataLoopFunction;
import org.hortonmachine.gears.libs.modules.MultiRasterLoopProcessor;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.Unit;

@Description(OmsPotentialEvapotranspiredWaterVolume.DESCRIPTION)
@Author(name = OmsPotentialEvapotranspiredWaterVolume.AUTHORNAMES, contact = OmsPotentialEvapotranspiredWaterVolume.AUTHORCONTACTS)
@Keywords(OmsPotentialEvapotranspiredWaterVolume.KEYWORDS)
@Label(OmsPotentialEvapotranspiredWaterVolume.LABEL)
@Name(OmsPotentialEvapotranspiredWaterVolume.NAME)
@Status(OmsPotentialEvapotranspiredWaterVolume.STATUS)
@License(OmsPotentialEvapotranspiredWaterVolume.LICENSE)
public class OmsPotentialEvapotranspiredWaterVolume extends HMModel {
    @Description(pDaysInTimestep_DESCRIPTION)
    @In
    public Double pDaysInTimestep = null;

    @Description(inCropCoefficient_DESCRIPTION)
    @In
    public GridCoverage2D inCropCoefficient = null;

    @Description(inMaxTemp_DESCRIPTION)
    @In
    public GridCoverage2D inMaxTemp = null;

    @Description(inMinTemp_DESCRIPTION)
    @In
    public GridCoverage2D inMinTemp = null;

    @Description(inAtmosTemp_DESCRIPTION)
    @In
    public GridCoverage2D inAtmosphericTemp = null;

    @Description(inSolarRadiation_DESCRIPTION)
    @Unit("MJ/m2")
    @In
    public GridCoverage2D inSolarRadiation = null;

    @Description(inRainfall_DESCRIPTION)
    @In
    public GridCoverage2D inRainfall;

    @Description(inReferenceEtp_DESCRIPTION)
    @In
    public GridCoverage2D inReferenceEtp = null;

    @Description(outputPet_DESCRIPTION)
    @In
    public GridCoverage2D outputPet;

    // VARS DOC START
    public static final String DESCRIPTION = "The Potential Evapotranspired Watervolume model (from INVEST).";
    public static final String DOCUMENTATION = "";
    public static final String KEYWORDS = "potential, evapotranspiration";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "PotentialEvapotranspiredWaterVolume";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "The klab team.";
    public static final String AUTHORCONTACTS = "www.integratedmodelling.org";

    public static final String pDaysInTimestep_DESCRIPTION = "The days contained in the used timestep (for a month that would be 30).";
    public static final String inCropCoefficient_DESCRIPTION = "The map of crop coefficient.";
    public static final String inReferenceEtp_DESCRIPTION = "The map of reference evapotraspiration (optional, excludes all but the crop coefficient).";
    public static final String inMaxTemp_DESCRIPTION = "The map of maximum temperature.";
    public static final String inMinTemp_DESCRIPTION = "The map of minimum temperature.";
    public static final String inAtmosTemp_DESCRIPTION = "The map of atmospheric temperature.";
    public static final String inSolarRadiation_DESCRIPTION = "The map of solar radiation.";
    public static final String inRainfall_DESCRIPTION = "The rainfall volume.";

    public static final String outputPet_DESCRIPTION = "The output potential evapotranspired watervolume.";

    public static final String pRainfall_UNIT = "mm";
    // VARS DOC END

    @Execute
    public void process() throws Exception {
        checkNull(inCropCoefficient);// , inMaxTemp, inMinTemp, inAtmosphericTemp, inSolarRadiation,
                                     // inRainfall);

        double kcNovalue = HMConstants.getNovalue(inCropCoefficient);
        if (inReferenceEtp != null) {
            double etpNovalue = HMConstants.getNovalue(inReferenceEtp);

            MultiRasterLoopProcessor processor = new MultiRasterLoopProcessor("Calculating PET...", pm);
            IDataLoopFunction funct = new IDataLoopFunction(){
                @Override
                public double process( double... values ) {
                    double kc = values[0];
                    double refEtp = values[1];
                    if (HMConstants.isNovalue(kc, kcNovalue) || HMConstants.isNovalue(refEtp, etpNovalue)) {
                        return etpNovalue;
                    }

                    return calculatePotentialEtp(kc, refEtp);
                }
            };
            outputPet = processor.loop(funct, etpNovalue, inCropCoefficient, inReferenceEtp);
        } else {
            checkNull(inMaxTemp, inMinTemp, inAtmosphericTemp, inSolarRadiation, inRainfall);

            double daysInTs = 30.0;
            if (pDaysInTimestep != null) {
                daysInTs = pDaysInTimestep;
            } else {
                pm.errorMessage("No days count in timestamp available, setting it to 30 (assuming 1 month timestep).");
            }
            double _daysInTs = daysInTs;

            double rainNv = HMConstants.getNovalue(inRainfall);
            double maxNv = HMConstants.getNovalue(inMaxTemp);
            double minNv = HMConstants.getNovalue(inMinTemp);
            double tempNv = HMConstants.getNovalue(inAtmosphericTemp);
            double solarNv = HMConstants.getNovalue(inSolarRadiation);

            MultiRasterLoopProcessor processor = new MultiRasterLoopProcessor("Calculating PET...", pm);
            IDataLoopFunction funct = new IDataLoopFunction(){
                @Override
                public double process( double... values ) {
                    double kc = values[0];
                    double tMax = values[1];
                    double tMin = values[2];
                    double tAvg = values[3];
                    double rainfall = values[4];
                    double solarRad = values[5];
                    if (HMConstants.isNovalue(kc, kcNovalue) || HMConstants.isNovalue(tMax, maxNv)
                            || HMConstants.isNovalue(tMin, minNv) || HMConstants.isNovalue(tAvg, tempNv)
                            || HMConstants.isNovalue(solarRad, solarNv)) {
                        return rainNv;
                    }
                    double refEtp = calculateReferenceEtp(tMax, tMin, tAvg, rainfall, solarRad, _daysInTs);
                    return calculatePotentialEtp(kc, refEtp);
                }
            };
            outputPet = processor.loop(funct, rainNv, inCropCoefficient, inMaxTemp, inMinTemp, inAtmosphericTemp, inRainfall,
                    inSolarRadiation);
        }

    }

    /**
     * Calculate pet using the solar radiation.
     *  
     * @param tMax value of maximum temperature.
     * @param tMin value of minimum temperature.
     * @param tAvg value of average temperature.
     * @param rainfall value of rainfall.
     * @param solarRad the solar radiation.
     * @param daysInTs the count of days in the considered timestep.
     * @return the reference ETP.
     */
    public static double calculateReferenceEtp( double tMax, double tMin, double tAvg, double rainfall, double solarRad,
            double daysInTs ) {
        double referenceET = 0.0013 * 0.408 * solarRad * (tAvg + 17) * Math.pow((tMax - tMin - 0.0123 * rainfall), 0.76)
                * daysInTs;
        return referenceET;
    }

    /**
     * Calculate pet using the reference etp.
     * 
     * @param kc value of crop coefficient.
     * @param referenceET the reference ETP.
     * @return the potential etp.
     */
    private static double calculatePotentialEtp( double kc, double referenceET ) {
        return kc * referenceET;
    }

}