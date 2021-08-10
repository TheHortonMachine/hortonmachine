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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume.inAtmosTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume.inCropCoefficient_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume.inMaxTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume.inMinTemp_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume.inRainfall_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume.inReferenceEtp_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume.inSolarRadiation_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume.outputPet_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.etp.OmsPotentialEvapotranspiredWaterVolume;

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

@Description(OmsPotentialEvapotranspiredWaterVolume.DESCRIPTION)
@Author(name = OmsPotentialEvapotranspiredWaterVolume.AUTHORNAMES, contact = OmsPotentialEvapotranspiredWaterVolume.AUTHORCONTACTS)
@Keywords(OmsPotentialEvapotranspiredWaterVolume.KEYWORDS)
@Label(OmsPotentialEvapotranspiredWaterVolume.LABEL)
@Name(OmsPotentialEvapotranspiredWaterVolume.NAME)
@Status(OmsPotentialEvapotranspiredWaterVolume.STATUS)
@License(OmsPotentialEvapotranspiredWaterVolume.LICENSE)
public class PotentialEvapotranspiredWaterVolume extends HMModel {
    @Description(inCropCoefficient_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inCropCoefficient = null;

    @Description(inMaxTemp_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inMaxTemp = null;

    @Description(inMinTemp_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inMinTemp = null;

    @Description(inAtmosTemp_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inAtmosphericTemp = null;

    @Description(inSolarRadiation_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSolarRadiation = null;

    @Description(inRainfall_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRainfall;

    @Description(inReferenceEtp_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inReferenceEtp = null;

    @Description(outputPet_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outputPet;

    @Execute
    public void process() throws Exception {
        OmsPotentialEvapotranspiredWaterVolume pet = new OmsPotentialEvapotranspiredWaterVolume();
        pet.inCropCoefficient = getRaster(inCropCoefficient);
        pet.inMaxTemp = getRaster(inMaxTemp);
        pet.inMinTemp = getRaster(inMinTemp);
        pet.inAtmosphericTemp = getRaster(inAtmosphericTemp);
        pet.inRainfall = getRaster(inRainfall);
        pet.inSolarRadiation = getRaster(inSolarRadiation);
        pet.inReferenceEtp = getRaster(inReferenceEtp);
        pet.pm = pm;
        pet.process();

        dumpRaster(pet.outputPet, outputPet);
    }

}