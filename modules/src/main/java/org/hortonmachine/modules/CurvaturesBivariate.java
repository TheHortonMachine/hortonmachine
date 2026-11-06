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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSHYDRO_LICENSE;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_LABEL;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_outPlan_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvatures.OMSCURVATURES_outProf_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvaturesBivariate.*;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvaturesBivariate;

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
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

@Description(OMSCURVATURESBIVARIATE_DESCRIPTION)
@Documentation(OMSCURVATURESBIVARIATE_DOCUMENTATION)
@Author(name = OMSHYDRO_AUTHORNAMES, contact = OMSHYDRO_AUTHORCONTACTS)
@Keywords(OMSCURVATURESBIVARIATE_KEYWORDS)
@Label(OMSCURVATURES_LABEL)
@Name("curvaturesbivariate")
@Status(OMSCURVATURESBIVARIATE_STATUS)
@License(OMSHYDRO_LICENSE)
@Bibliography({BIBLIOGRAPHY_WOOD_1996, BIBLIOGRAPHY_WILSON_2007})
public class CurvaturesBivariate extends HMModel {
    @Description(OMSCURVATURESBIVARIATE_inElev_DESCRIPTION)
    @Unit("m")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inElev = null;

    @Description(OMSCURVATURESBIVARIATE_pCells_DESCRIPTION)
    @Unit("cells")
    @In
    public int pCells = 3;

    @Description(OMSCURVATURES_outProf_DESCRIPTION)
    @Unit("1/m")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outProf = null;

    @Description(OMSCURVATURES_outPlan_DESCRIPTION)
    @Unit("1/m")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outPlan = null;

    @Description(OMSCURVATURESBIVARIATE_outSlope_DESCRIPTION)
    @Unit("degree")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outSlope = null;

    @Description(OMSCURVATURESBIVARIATE_outAspect_DESCRIPTION)
    @Unit("degree")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outAspect = null;

    @Execute
    public void process() throws Exception {
        OmsCurvaturesBivariate curvatures = new OmsCurvaturesBivariate();
        curvatures.inElev = getRaster(inElev);
        curvatures.pCells = pCells;
        curvatures.pm = pm;
        curvatures.process();
        dumpRaster(curvatures.outProf, outProf);
        dumpRaster(curvatures.outPlan, outPlan);
        dumpRaster(curvatures.outSlope, outSlope);
        dumpRaster(curvatures.outAspect, outAspect);
    }
}
