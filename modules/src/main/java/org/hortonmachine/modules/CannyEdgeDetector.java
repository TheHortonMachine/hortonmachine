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

import static org.hortonmachine.gears.modules.r.edgedetection.OmsCannyEdgeDetector.*;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.edgedetection.OmsCannyEdgeDetector;

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

@Description(OMSCANNY_DESCRIPTION)
@Documentation(OMSCANNY_DOCUMENTATION)
@Author(name = "Andrea Antonello", contact = "https://g-ant.eu")
@Keywords(OMSCANNY_KEYWORDS)
@Label(HMConstants.RASTERPROCESSING)
@Name("canny")
@Status(OMSCANNY_STATUS)
@License(HMConstants.GPL3_LICENSE)
@Bibliography(BIBLIOGRAPHY_CANNY_1986)
public class CannyEdgeDetector extends HMModel {

    @Description(OMSCANNY_inMap_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inMap = null;

    @Description(OMSCANNY_pLowthres_DESCRIPTION)
    @In
    public Double pLowthres = 0.1;

    @Description(OMSCANNY_pHighthres_DESCRIPTION)
    @In
    public Double pHighthres = 0.2;

    @Description(OMSCANNY_pRadiusgauss_DESCRIPTION)
    @Unit("cells")
    @In
    public Double pRadiusgauss = 2.0;

    @Description(OMSCANNY_pWidthgauss_DESCRIPTION)
    @Unit("cells")
    @In
    public Integer pWidthgauss = 16;

    @Description(OMSCANNY_outMap_DESCRIPTION)
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
        cannyedgedetector.pm = pm;
        cannyedgedetector.process();
        dumpRaster(cannyedgedetector.outMap, outMap);
    }

}
