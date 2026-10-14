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
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYQuickflow;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

@Description(OmsPotentialEvapotranspiredWaterVolume.DESCRIPTION)
@Documentation(OmsPotentialEvapotranspiredWaterVolume.DOCUMENTATION)
@Bibliography({OmsPotentialEvapotranspiredWaterVolume.BIBLIOGRAPHY_DROOGERS_ALLEN_2002, OmsSWYQuickflow.BIBLIOGRAPHY_INVEST_SWY})
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
    @Unit("°C")
    @In
    public GridCoverage2D inMaxTemp = null;

    @Description(inMinTemp_DESCRIPTION)
    @Unit("°C")
    @In
    public GridCoverage2D inMinTemp = null;

    @Description(inAtmosTemp_DESCRIPTION)
    @Unit("°C")
    @In
    public GridCoverage2D inAtmosphericTemp = null;

    @Description(inSolarRadiation_DESCRIPTION)
    @Unit("MJ/m2/day")
    @In
    public GridCoverage2D inSolarRadiation = null;

    @Description(inRainfall_DESCRIPTION)
    @Unit("mm")
    @In
    public GridCoverage2D inRainfall;

    @Description(inReferenceEtp_DESCRIPTION)
    @Unit("mm")
    @In
    public GridCoverage2D inReferenceEtp = null;

    @Description(outputPet_DESCRIPTION)
    @Unit("mm")
    @Out
    public GridCoverage2D outputPet;

    // VARS DOC START
    public static final String DESCRIPTION = "The potential evapotranspiration of a crop or land cover, as crop coefficient times the reference evapotranspiration, given or calculated with the modified Hargreaves equation (as in InVEST).";
    public static final String DOCUMENTATION = """
            The potential evapotranspiration of each cell is its crop coefficient (Kc) times the reference \
            evapotranspiration. The reference evapotranspiration can be given as a map, or calculated with the \
            modified Hargreaves equation of Droogers and Allen (2002), as suggested by InVEST when only temperatures \
            and precipitation are available:

            ET0 = 0.0013 · 0.408 · RA · (Tavg + 17) · (Tmax - Tmin - 0.0123 · P)^0.76 · days

            with RA the mean daily extraterrestrial radiation in MJ/m2/day (0.408 converts it to mm of water), the \
            mean daily temperatures in °C, P the precipitation of the timestep in mm and days the days of the \
            timestep. In wet timesteps with a small temperature range, where the precipitation term exceeds the \
            range, the reference evapotranspiration is 0.
            """;
    public static final String BIBLIOGRAPHY_DROOGERS_ALLEN_2002 = "Droogers, P., Allen, R. G. (2002). Estimating reference evapotranspiration under inaccurate data conditions. Irrigation and Drainage Systems, 16(1), 33-45.";
    public static final String KEYWORDS = "potential, evapotranspiration";
    public static final String LABEL = HYDROGEOMORPHOLOGY;
    public static final String NAME = "PotentialEvapotranspiredWaterVolume";
    public static final int STATUS = 5;
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String AUTHORNAMES = "The klab team.";
    public static final String AUTHORCONTACTS = "www.integratedmodelling.org";

    public static final String pDaysInTimestep_DESCRIPTION = "The days of the timestep, used with the Hargreaves equation (if not set, 30, for a month).";
    public static final String inCropCoefficient_DESCRIPTION = "The map of the crop coefficient (Kc).";
    public static final String inReferenceEtp_DESCRIPTION = "The map of the reference evapotranspiration of the timestep (if set, only the crop coefficient is needed).";
    public static final String inMaxTemp_DESCRIPTION = "The map of the mean daily maximum temperature of the timestep, for the Hargreaves equation.";
    public static final String inMinTemp_DESCRIPTION = "The map of the mean daily minimum temperature of the timestep, for the Hargreaves equation.";
    public static final String inAtmosTemp_DESCRIPTION = "The map of the mean daily temperature of the timestep, for the Hargreaves equation.";
    public static final String inSolarRadiation_DESCRIPTION = "The map of the mean daily extraterrestrial radiation of the timestep, for the Hargreaves equation.";
    public static final String inRainfall_DESCRIPTION = "The map of the precipitation of the timestep, for the Hargreaves equation.";

    public static final String outputPet_DESCRIPTION = "The potential evapotranspiration of the timestep.";

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
                            || HMConstants.isNovalue(rainfall, rainNv) || HMConstants.isNovalue(solarRad, solarNv)) {
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
     * Calculate the reference evapotranspiration with the modified Hargreaves equation
     * (Droogers and Allen, 2002), as in InVEST.
     *
     * <p>Where the precipitation term exceeds the temperature range, the result would not be
     * defined (power of a negative number): there the evapotranspiration is 0.</p>
     *
     * @param tMax the mean daily maximum temperature [°C].
     * @param tMin the mean daily minimum temperature [°C].
     * @param tAvg the mean daily temperature [°C].
     * @param rainfall the precipitation of the timestep [mm].
     * @param solarRad the mean daily extraterrestrial radiation [MJ/m2/day].
     * @param daysInTs the count of days in the considered timestep.
     * @return the reference evapotranspiration of the timestep [mm].
     */
    public static double calculateReferenceEtp( double tMax, double tMin, double tAvg, double rainfall, double solarRad,
            double daysInTs ) {
        double range = Math.max(tMax - tMin - 0.0123 * rainfall, 0);
        double referenceET = 0.0013 * 0.408 * solarRad * (tAvg + 17) * Math.pow(range, 0.76) * daysInTs;
        return referenceET;
    }

    /**
     * Calculate pet using the reference etp.
     * 
     * <p>Kc adjusts the reference PET to represent the potential ET of a 
     * specific crop or land cover under well-watered conditions.
     * 
     * @param kc value of crop coefficient.
     * @param referenceET the reference ETP.
     * @return the potential etp.
     */
    private static double calculatePotentialEtp( double kc, double referenceET ) {
        return kc * referenceET;
    }

}