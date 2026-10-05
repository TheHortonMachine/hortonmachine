/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) Andrea Antonello - https://g-ant.eu
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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.KEYWORDS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.LABEL;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.LICENSE;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.LITHOLOGY_NAMES_COMBO;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.NAME;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.STATUS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.UNCLASSIFIED;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.inLithologyTable_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.inLithology_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.inSources_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.inStopAreas_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.outFriction_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.outNormalRestitution_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.outSources_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.outTangentialRestitution_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.pBouldersPerSource_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.pDefaultLithology_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs.pSourceSlope_DESCRIPTION;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStone;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.stone.OmsStoneInputs;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

@Description(DESCRIPTION)
@Author(name = AUTHORNAMES, contact = AUTHORCONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name("_" + NAME)
@Status(STATUS)
@License(LICENSE)
@Bibliography({OmsStone.BIBLIOGRAPHY_ALVIOLI_ET_AL_2021})
public class StoneInputs extends HMModel {
    @Description(inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev;

    @Description(inSources_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inSources;

    @Description(pSourceSlope_DESCRIPTION)
    @In
    public double pSourceSlope = 45.0;

    @Description(pBouldersPerSource_DESCRIPTION)
    @In
    public int pBouldersPerSource = 1;

    @Description(inStopAreas_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inStopAreas;

    @Description(inLithology_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inLithology;

    @Description(inLithologyTable_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inLithologyTable;

    @Description(pDefaultLithology_DESCRIPTION)
    @UI("combo:" + LITHOLOGY_NAMES_COMBO)
    @In
    public String pDefaultLithology = UNCLASSIFIED;

    @Description(outSources_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outSources;

    @Description(outNormalRestitution_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outNormalRestitution;

    @Description(outTangentialRestitution_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTangentialRestitution;

    @Description(outFriction_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outFriction;

    @Execute
    public void process() throws Exception {
        checkNull(inElev, outSources, outNormalRestitution, outTangentialRestitution, outFriction);
        OmsStoneInputs inputs = new OmsStoneInputs();
        inputs.pm = pm;
        inputs.doProcess = doProcess;
        inputs.doReset = doReset;
        inputs.inElev = getRaster(inElev);
        inputs.inSources = getRaster(inSources);
        inputs.inStopAreas = getRaster(inStopAreas);
        inputs.inLithology = getRaster(inLithology);
        inputs.inLithologyTable = inLithologyTable;
        inputs.pSourceSlope = pSourceSlope;
        inputs.pBouldersPerSource = pBouldersPerSource;
        inputs.pDefaultLithology = pDefaultLithology;
        inputs.process();
        dumpRaster(inputs.outSources, outSources);
        dumpRaster(inputs.outNormalRestitution, outNormalRestitution);
        dumpRaster(inputs.outTangentialRestitution, outTangentialRestitution);
        dumpRaster(inputs.outFriction, outFriction);
    }
}
