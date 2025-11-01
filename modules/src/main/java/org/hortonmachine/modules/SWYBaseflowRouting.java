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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYBaseflowRouting.*;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.swy.OmsSWYBaseflowRouting;

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

@Description(OmsSWYBaseflowRouting.DESCRIPTION)
@Author(name = OmsSWYBaseflowRouting.AUTHORNAMES, contact = OmsSWYBaseflowRouting.AUTHORCONTACTS)
@Keywords(OmsSWYBaseflowRouting.KEYWORDS)
@Label(OmsSWYBaseflowRouting.LABEL)
@Name(OmsSWYBaseflowRouting.NAME)
@Status(OmsSWYBaseflowRouting.STATUS)
@License(OmsSWYBaseflowRouting.LICENSE)
public class SWYBaseflowRouting extends HMModel {
    @Description(inRecharge_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRecharge = null;

    @Description(inAvailableRecharge_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inAvailableRecharge = null;

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
        OmsSWYBaseflowRouting bf = new OmsSWYBaseflowRouting();
        bf.pm = pm;
        bf.inRecharge = getRaster(inRecharge);
        bf.inAvailableRecharge = getRaster(inAvailableRecharge);
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