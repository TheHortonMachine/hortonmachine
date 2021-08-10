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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.baseflow.OmsBaseflowWaterVolume.*;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.baseflow.OmsBaseflowWaterVolume.inInf_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.baseflow.OmsBaseflowWaterVolume.inNetInf_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.baseflow.OmsBaseflowWaterVolume.inNet_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.baseflow.OmsBaseflowWaterVolume.outBaseflow_DESCRIPTION;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.baseflow.OmsBaseflowWaterVolume;

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

@Description(OmsBaseflowWaterVolume.DESCRIPTION)
@Author(name = OmsBaseflowWaterVolume.AUTHORNAMES, contact = OmsBaseflowWaterVolume.AUTHORCONTACTS)
@Keywords(OmsBaseflowWaterVolume.KEYWORDS)
@Label(OmsBaseflowWaterVolume.LABEL)
@Name(OmsBaseflowWaterVolume.NAME)
@Status(OmsBaseflowWaterVolume.STATUS)
@License(OmsBaseflowWaterVolume.LICENSE)
public class BaseflowWaterVolume extends HMModel {
    @Description(inInf_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inInfiltration = null;

    @Description(inNetInf_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNetInfiltration = null;

    @Description(inNet_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inNet = null;

    @Description(inFlowdirections_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inFlowdirections = null;

    @Description(outLsum_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outLsum = null;

    @Description(outB_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outB = null;

    @Description(outVri_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outVri = null;

    @Description(outQb_DESCRIPTION)
    @Out
    public Double outQb = null;

    @Description(outVriSum_DESCRIPTION)
    @Out
    public Double outVriSum = null;

    @Description(outBaseflow_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outBaseflow = null;

    @Execute
    public void process() throws Exception {
        OmsBaseflowWaterVolume bf = new OmsBaseflowWaterVolume();
        bf.pm = pm;
        bf.inInfiltration = getRaster(inInfiltration);
        bf.inNetInfiltration = getRaster(inNetInfiltration);
        bf.inNet = getRaster(inNet);
        bf.inFlowdirections = getRaster(inFlowdirections);
        bf.process();

        dumpRaster(bf.outBaseflow, outBaseflow);
        dumpRaster(bf.outLsum, outLsum);
        dumpRaster(bf.outVri, outVri);
        dumpRaster(bf.outB, outB);

        outQb = bf.outQb;
        outVriSum = bf.outVriSum;

    }

}