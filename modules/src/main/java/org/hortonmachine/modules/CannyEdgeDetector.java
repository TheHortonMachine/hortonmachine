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

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.modules.r.edgedetection.OmsCannyEdgeDetector;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Name;
import oms3.annotations.UI;

@Name("canny")
public class CannyEdgeDetector extends OmsCannyEdgeDetector {

    @Description("The map on which to perform edge detection.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    public String inMap = null;

    @Description("The resulting map.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outMap = null;

    @Execute
    public void process() throws Exception {
        OmsCannyEdgeDetector cannyedgedetector = new OmsCannyEdgeDetector();
        cannyedgedetector.inMap = getRaster(inMap);
        cannyedgedetector.pLowthres = pLowthres;
        cannyedgedetector.pHighthres = pHighthres;
        cannyedgedetector.pRadiusgauss = pRadiusgauss;
        cannyedgedetector.pWidthgauss = pWidthgauss;
        cannyedgedetector.doNormcontrast = doNormcontrast;
        cannyedgedetector.pm = pm;
        cannyedgedetector.process();
        dumpRaster(cannyedgedetector.outMap, outMap);
    }

}
