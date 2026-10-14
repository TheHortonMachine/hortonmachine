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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYRechargeRouting.*;

import java.io.File;
import java.util.ArrayList;
import java.util.TreeMap;

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYQuickflow;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYRechargeRouting;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Documentation;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description(OmsSWYRechargeRouting.DESCRIPTION)
@Documentation(OmsSWYRechargeRouting.DOCUMENTATION)
@Bibliography({OmsSWYQuickflow.BIBLIOGRAPHY_INVEST_SWY, OmsSWYQuickflow.BIBLIOGRAPHY_HAMEL_ET_AL_2020})
@Author(name = OmsSWYRechargeRouting.AUTHORNAMES, contact = OmsSWYRechargeRouting.AUTHORCONTACTS)
@Keywords(OmsSWYRechargeRouting.KEYWORDS)
@Label(OmsSWYRechargeRouting.LABEL)
@Name(OmsSWYRechargeRouting.NAME)
@Status(OmsSWYRechargeRouting.STATUS)
@License(OmsSWYRechargeRouting.LICENSE)
public class SWYRechargeRouting extends HMModel {
    @Description(inPet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("mm")
    @In
    public String inPet = null;

    @Description(inPrecipitation_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("mm")
    @In
    public String inPrecipitation;

    @Description(inFlowdirections_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlowdirections = null;

    /**
     * @deprecated not used, stream cells follow the same water balance as the other cells.
     */
    @Deprecated
    @Description(inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(inQuickflow_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("mm")
    @In
    public String inQuickflow = null;

    @Description(pAlpha_DESCRIPTION)
    @In
    public double pAlpha = 1.0;

    @Description(pBeta_DESCRIPTION)
    @In
    public double pBeta = 1.0;

    @Description(pGamma_DESCRIPTION)
    @In
    public double pGamma = 1.0;

    @Description(inPetFolder_DESCRIPTION)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inPetFolder = null;

    @Description(pPetPrefix_DESCRIPTION)
    @In
    public String pPetPrefix = null;

    @Description(inPrecipitationFolder_DESCRIPTION)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inPrecipitationFolder = null;

    @Description(pPrecipitationPrefix_DESCRIPTION)
    @In
    public String pPrecipitationPrefix = null;

    @Description(inQuickflowFolder_DESCRIPTION)
    @UI(HMConstants.FOLDERIN_UI_HINT)
    @In
    public String inQuickflowFolder = null;

    @Description(pQuickflowPrefix_DESCRIPTION)
    @In
    public String pQuickflowPrefix = null;

    @Description(pAlphas_DESCRIPTION)
    @In
    public String pAlphas = null;

    @Description(outAet_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Unit("mm")
    @In
    public String outAet = null;

    @Description(outRecharge_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Unit("mm")
    @In
    public String outRecharge = null;

    @Description(outUpslopeSubsidy_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Unit("mm")
    @In
    public String outUpslopeSubsidy = null;

    @Description(outAvailableRecharge_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Unit("mm")
    @In
    public String outAvailableRecharge = null;


    @Execute
    @SuppressWarnings("deprecation")
    public void process() throws Exception {
        OmsSWYRechargeRouting inf = new OmsSWYRechargeRouting();
        if (inPetFolder != null) {
            checkNull(pPetPrefix, inPrecipitationFolder, pPrecipitationPrefix, inQuickflowFolder, pQuickflowPrefix);
            TreeMap<Integer, File> pets = getIntervalRasters(new File(inPetFolder), pPetPrefix);
            TreeMap<Integer, File> precipitations = getIntervalRasters(new File(inPrecipitationFolder), pPrecipitationPrefix);
            TreeMap<Integer, File> quickflows = getIntervalRasters(new File(inQuickflowFolder), pQuickflowPrefix);
            if (pets.isEmpty() || !pets.keySet().equals(precipitations.keySet())
                    || !pets.keySet().equals(quickflows.keySet())) {
                throw new ModelsIllegalargumentException("The pet, precipitation and quickflow folders need rasters for the same intervals, found: pet "
                        + pets.keySet() + ", precipitation " + precipitations.keySet() + ", quickflow " + quickflows.keySet(), this,
                        pm);
            }
            inf.inPetList = new ArrayList<>();
            inf.inPrecipitationList = new ArrayList<>();
            inf.inQuickflowList = new ArrayList<>();
            for( Integer interval : pets.keySet() ) {
                pm.message("Interval " + interval + ": " + pets.get(interval).getName() + ", "
                        + precipitations.get(interval).getName() + ", " + quickflows.get(interval).getName());
                inf.inPetList.add(getRaster(pets.get(interval).getAbsolutePath()));
                inf.inPrecipitationList.add(getRaster(precipitations.get(interval).getAbsolutePath()));
                inf.inQuickflowList.add(getRaster(quickflows.get(interval).getAbsolutePath()));
            }
            if (pAlphas != null && !pAlphas.isBlank()) {
                String[] split = pAlphas.split(",");
                double[] alphas = new double[split.length];
                for( int i = 0; i < split.length; i++ ) {
                    alphas[i] = Double.parseDouble(split[i].trim());
                }
                inf.pAlphaList = alphas;
            }
        } else {
            inf.inPet = getRaster(inPet);
            inf.inPrecipitation = getRaster(inPrecipitation);
            inf.inQuickflow = getRaster(inQuickflow);
        }
        inf.inFlowdirections = getRaster(inFlowdirections);
        inf.inNet = getRaster(inNet);
        inf.pAlpha = pAlpha;
        inf.pBeta = pBeta;
        inf.pGamma = pGamma;
        inf.pm = pm;
        inf.process();

        dumpRaster(inf.outRecharge, outRecharge);
        dumpRaster(inf.outAvailableRecharge, outAvailableRecharge);
        dumpRaster(inf.outAet, outAet);
        dumpRaster(inf.outUpslopeSubsidy, outUpslopeSubsidy);
    }
}