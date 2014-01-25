/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.modules;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Name;
import oms3.annotations.UI;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.modules.r.edgedetection.OmsCannyEdgeDetector;

@Name("canny")
public class CannyEdgeDetector extends OmsCannyEdgeDetector {

    @Description("The map on which to perform edge detection.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    public String inMap = null;

    @Description("The resulting map.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
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
