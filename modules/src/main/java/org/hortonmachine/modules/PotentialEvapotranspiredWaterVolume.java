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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.evapotrans.OmsPotentialEvapotranspiredWaterVolume.inAtmosTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.evapotrans.OmsPotentialEvapotranspiredWaterVolume.inCropCoefficient_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.evapotrans.OmsPotentialEvapotranspiredWaterVolume.inMaxTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.evapotrans.OmsPotentialEvapotranspiredWaterVolume.inMinTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.evapotrans.OmsPotentialEvapotranspiredWaterVolume.inRainfall_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.evapotrans.OmsPotentialEvapotranspiredWaterVolume.inSolarRadiation_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.evapotrans.OmsPotentialEvapotranspiredWaterVolume.outputPet_DESCRIPTION;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.IDataLoopFunction;
import org.hortonmachine.gears.libs.modules.RasterLoopProcessor;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.evapotrans.OmsPotentialEvapotranspiredWaterVolume;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;

@Description(OmsPotentialEvapotranspiredWaterVolume.DESCRIPTION)
@Author(name = OmsPotentialEvapotranspiredWaterVolume.AUTHORNAMES, contact = OmsPotentialEvapotranspiredWaterVolume.AUTHORCONTACTS)
@Keywords(OmsPotentialEvapotranspiredWaterVolume.KEYWORDS)
@Label(OmsPotentialEvapotranspiredWaterVolume.LABEL)
@Name(OmsPotentialEvapotranspiredWaterVolume.NAME)
@Status(OmsPotentialEvapotranspiredWaterVolume.STATUS)
@License(OmsPotentialEvapotranspiredWaterVolume.LICENSE)
public class PotentialEvapotranspiredWaterVolume extends HMModel {
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
    @In
    public GridCoverage2D inSolarRadiation = null;

    @Description(inRainfall_DESCRIPTION)
    @In
    public GridCoverage2D inRainfall;

    @Description(outputPet_DESCRIPTION)
    @In
    public GridCoverage2D outputPet;

    @Execute
    public void process() throws Exception {
        checkNull(inCropCoefficient, inMaxTemp, inMinTemp, inAtmosphericTemp, inSolarRadiation, inRainfall);

        RasterLoopProcessor processor = new RasterLoopProcessor("Calculating PET...", pm);
        IDataLoopFunction funct = new IDataLoopFunction(){
            @Override
            public double process( double... values ) {
                boolean isValid = true;
                for( double d : values ) {
                    isValid = isValid && !HMConstants.isNovalue(d);
                }
                if (isValid) {
                    return calculateRunoff(values[0], values[1], values[2], values[3], values[4], values[5]);

                } else {
                    return Double.NaN;
                }
            }
        };
        processor.process(funct, inCropCoefficient, inMaxTemp, inMinTemp, inAtmosphericTemp, inRainfall, inSolarRadiation);

    }

    /**
     * Calculate pet.
     *  
     */
    public static double calculateRunoff( double kc, double tMax, double tMin, double tAvg, double rainfall, double solarRad ) {
        double referenceET = 0.0013 * 0.408 * solarRad * (tAvg + 17) * Math.pow((tMax - tMin - 0.0123 * rainfall), 0.76);
        double pet = kc * referenceET;
        return pet;
    }

}