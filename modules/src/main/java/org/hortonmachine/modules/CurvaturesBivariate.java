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

import org.hortonmachine.hmachine.modules.geomorphology.curvatures.OmsCurvaturesBivariate;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Name;

@Name("curvaturesbivariate")
public class CurvaturesBivariate extends OmsCurvaturesBivariate {
    @Description("The map of the digital elevation model (DEM or pit).")
    @In
    public String inElev = null;

    @Description(OMSCURVATURES_outProf_DESCRIPTION)
    @In
    public String outProf = null;

    @Description(OMSCURVATURES_outPlan_DESCRIPTION)
    @In
    public String outPlan = null;

    @Description("The map of slope.")
    @In
    public String outSlope = null;

    @Description("The map of aspect")
    @In
    public String outAspect = null;

    @Execute
    public void process() throws Exception {
        OmsCurvaturesBivariate curvaturesbivariate = new OmsCurvaturesBivariate();
        curvaturesbivariate.inElev = getRaster(inElev);
        curvaturesbivariate.pCells = pCells;
        curvaturesbivariate.pm = pm;
        curvaturesbivariate.process();
        dumpRaster(curvaturesbivariate.outProf, outProf);
        dumpRaster(curvaturesbivariate.outPlan, outPlan);
        dumpRaster(curvaturesbivariate.outSlope, outSlope);
        dumpRaster(curvaturesbivariate.outAspect, outAspect);
    }
}
