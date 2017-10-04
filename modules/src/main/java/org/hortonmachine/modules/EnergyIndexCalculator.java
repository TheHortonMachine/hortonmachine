/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.modules;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_inAspect_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_inBasins_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_inCurvatures_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_inSlope_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_outAltimetry_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_outArea_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_outEnergy_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_pDt_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_pEi_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSENERGYINDEXCALCULATOR_pEs_DESCRIPTION;

import java.util.List;

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

import org.hortonmachine.gears.io.eicalculator.EIAltimetry;
import org.hortonmachine.gears.io.eicalculator.EIAreas;
import org.hortonmachine.gears.io.eicalculator.EIEnergy;
import org.hortonmachine.gears.io.eicalculator.OmsEIAltimetryWriter;
import org.hortonmachine.gears.io.eicalculator.OmsEIAreasWriter;
import org.hortonmachine.gears.io.eicalculator.OmsEIEnergyWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.energyindexcalculator.OmsEnergyIndexCalculator;

@Description(OMSENERGYINDEXCALCULATOR_DESCRIPTION)
@Author(name = OMSENERGYINDEXCALCULATOR_AUTHORNAMES, contact = OMSENERGYINDEXCALCULATOR_AUTHORCONTACTS)
@Keywords(OMSENERGYINDEXCALCULATOR_KEYWORDS)
@Label(OMSENERGYINDEXCALCULATOR_LABEL)
@Name("_" + OMSENERGYINDEXCALCULATOR_NAME)
@Status(OMSENERGYINDEXCALCULATOR_STATUS)
@License(OMSENERGYINDEXCALCULATOR_LICENSE)
public class EnergyIndexCalculator extends HMModel {

    @Description(OMSENERGYINDEXCALCULATOR_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSENERGYINDEXCALCULATOR_inBasins_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inBasins = null;

    @Description(OMSENERGYINDEXCALCULATOR_inCurvatures_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inCurvatures = null;

    @Description(OMSENERGYINDEXCALCULATOR_inAspect_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inAspect = null;

    @Description(OMSENERGYINDEXCALCULATOR_inSlope_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSlope = null;

    @Description(OMSENERGYINDEXCALCULATOR_pEs_DESCRIPTION)
    @In
    public int pEs = -1;

    @Description(OMSENERGYINDEXCALCULATOR_pEi_DESCRIPTION)
    @In
    public int pEi = -1;

    @Description(OMSENERGYINDEXCALCULATOR_pDt_DESCRIPTION)
    @In
    public double pDt = -1;

    @Description(OMSENERGYINDEXCALCULATOR_outAltimetry_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outAltimetry;

    @Description(OMSENERGYINDEXCALCULATOR_outEnergy_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outEnergy;

    @Description(OMSENERGYINDEXCALCULATOR_outArea_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outArea;

    @Execute
    public void process() throws Exception {

        OmsEnergyIndexCalculator energyindexcalculator = new OmsEnergyIndexCalculator();
        energyindexcalculator.inElev = getRaster(inElev);
        energyindexcalculator.inBasins = getRaster(inBasins);
        energyindexcalculator.inCurvatures = getRaster(inCurvatures);
        energyindexcalculator.inAspect = getRaster(inAspect);
        energyindexcalculator.inSlope = getRaster(inSlope);
        energyindexcalculator.pEs = pEs;
        energyindexcalculator.pEi = pEi;
        energyindexcalculator.pDt = pDt;
        energyindexcalculator.pm = pm;
        energyindexcalculator.process();
        List<EIAltimetry> outAltimetryObj = energyindexcalculator.outAltimetry;
        List<EIEnergy> outEnergyObj = energyindexcalculator.outEnergy;
        List<EIAreas> outAreaObj = energyindexcalculator.outArea;

        OmsEIAltimetryWriter altimetryWriter = new OmsEIAltimetryWriter();
        altimetryWriter.inAltimetry = outAltimetryObj;
        altimetryWriter.file = outAltimetry;
        altimetryWriter.write();
        altimetryWriter.close();

        OmsEIEnergyWriter energyWriter = new OmsEIEnergyWriter();
        energyWriter.inEnergy = outEnergyObj;
        energyWriter.file = outEnergy;
        energyWriter.write();
        energyWriter.close();

        OmsEIAreasWriter areasWriter = new OmsEIAreasWriter();
        areasWriter.inAreas = outAreaObj;
        areasWriter.file = outArea;
        areasWriter.write();
        areasWriter.close();

    }

}
