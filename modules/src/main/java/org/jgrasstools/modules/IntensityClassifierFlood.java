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

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierFlood.*;
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
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierFlood;

@Description(OMSINTENSITYCLASSIFIER_DESCRIPTION)
@Author(name = OMSINTENSITYCLASSIFIER_AUTHORNAMES, contact = OMSINTENSITYCLASSIFIER_AUTHORCONTACTS)
@Keywords(OMSINTENSITYCLASSIFIER_KEYWORDS)
@Label(OMSINTENSITYCLASSIFIER_LABEL)
@Name("_" + OMSINTENSITYCLASSIFIER_NAME)
@Status(OMSINTENSITYCLASSIFIER_STATUS)
@License(OMSINTENSITYCLASSIFIER_LICENSE)
public class IntensityClassifierFlood extends JGTModel {

    @Description(OMSINTENSITYCLASSIFIER_inWaterDepth_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @Unit("m")
    @In
    public String inWaterDepth;

    @Description(OMSINTENSITYCLASSIFIER_inVelocity_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @Unit("m/s")
    @In
    public String inVelocity;

    @Description(OMSINTENSITYCLASSIFIER_pUpperThresWaterdepth_DESCRIPTION)
    @Unit("m")
    @In
    public Double pUpperThresWaterdepth = 1.0;

    @Description(OMSINTENSITYCLASSIFIER_pUpperThresVelocityWaterdepth_DESCRIPTION)
    @Unit("m2/s")
    @In
    public Double pUpperThresVelocityWaterdepth = 1.0;

    @Description(OMSINTENSITYCLASSIFIER_pLowerThresWaterdepth_DESCRIPTION)
    @Unit("m")
    @In
    public Double pLowerThresWaterdepth = 0.5;

    @Description(OMSINTENSITYCLASSIFIER_pLowerThresVelocityWaterdepth_DESCRIPTION)
    @Unit("m2/s")
    @In
    public Double pLowerThresVelocityWaterdepth = 0.5;

    @Description(OMSINTENSITYCLASSIFIER_outIntensity_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outIntensity = null;

    @Execute
    public void process() throws Exception {
        OmsIntensityClassifierFlood intensityclassifier = new OmsIntensityClassifierFlood();
        intensityclassifier.inWaterDepth = getRaster(inWaterDepth);
        intensityclassifier.inVelocity = getRaster(inVelocity);
        intensityclassifier.pUpperThresWaterdepth = pUpperThresWaterdepth;
        intensityclassifier.pUpperThresVelocityWaterdepth = pUpperThresVelocityWaterdepth;
        intensityclassifier.pLowerThresWaterdepth = pLowerThresWaterdepth;
        intensityclassifier.pLowerThresVelocityWaterdepth = pLowerThresVelocityWaterdepth;
        intensityclassifier.pm = pm;
        intensityclassifier.doProcess = doProcess;
        intensityclassifier.doReset = doReset;
        intensityclassifier.process();
        dumpRaster(intensityclassifier.outIntensity, outIntensity);
    }
}
