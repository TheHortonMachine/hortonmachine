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

import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.*;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures;

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

@Description(OMSCURVATURES_DESCRIPTION)
@Author(name = OMSCURVATURES_AUTHORNAMES, contact = OMSCURVATURES_AUTHORCONTACTS)
@Keywords(OMSCURVATURES_KEYWORDS)
@Label(OMSCURVATURES_LABEL)
@Name("_" + OMSCURVATURES_NAME)
@Status(OMSCURVATURES_STATUS)
@License(OMSCURVATURES_LICENSE)
public class Curvatures extends HMModel {
    @Description(OMSCURVATURES_inElev_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    // output
    @Description(OMSCURVATURES_outProf_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outProf = null;

    @Description(OMSCURVATURES_outPlan_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outPlan = null;

    @Description(OMSCURVATURES_outTang_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outTang = null;

    @Execute
    public void process() throws Exception {
        OmsCurvatures curv = new OmsCurvatures();
        curv.pm = pm;
        curv.inElev = getRaster(inElev);
        curv.process();
        dumpRaster(curv.outProf, outProf);
        dumpRaster(curv.outPlan, outPlan);
        dumpRaster(curv.outTang, outTang);
    }
}
