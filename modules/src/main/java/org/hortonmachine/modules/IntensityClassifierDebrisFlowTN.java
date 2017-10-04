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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_KEYWORDS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_LABEL;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_LICENSE;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_NAME;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_STATUS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_inDepositsThickness_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_inErosionDepth_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_inVelocity_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_inWaterDepth_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_outIntensity_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_pLowerThresDepositsThickness_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_pLowerThresErosionDepth_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_pLowerThresVelocity_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_pLowerThresWaterdepth_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_pUpperThresDepositsThickness_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_pUpperThresErosionDepth_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_pUpperThresVelocity_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.OMSINTENSITYCLASSIFIER_pUpperThresWaterdepth_DESCRIPTION;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.rasterreader.OmsRasterReader;
import org.hortonmachine.gears.io.rasterwriter.OmsRasterWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.modules.r.rasternull.OmsRasterNull;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN;

@Description(OMSINTENSITYCLASSIFIER_DESCRIPTION)
@Author(name = OMSINTENSITYCLASSIFIER_AUTHORNAMES, contact = OMSINTENSITYCLASSIFIER_AUTHORCONTACTS)
@Keywords(OMSINTENSITYCLASSIFIER_KEYWORDS)
@Label(OMSINTENSITYCLASSIFIER_LABEL)
@Name("_" + OMSINTENSITYCLASSIFIER_NAME)
@Status(OMSINTENSITYCLASSIFIER_STATUS)
@License(OMSINTENSITYCLASSIFIER_LICENSE)
public class IntensityClassifierDebrisFlowTN extends HMModel {

    @Description(OMSINTENSITYCLASSIFIER_inWaterDepth_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("m")
    @In
    public String inWaterDepth;

    @Description(OMSINTENSITYCLASSIFIER_pUpperThresWaterdepth_DESCRIPTION)
    @Unit("m")
    @In
    public Double pUpperThresWaterdepth = 1.0;

    @Description(OMSINTENSITYCLASSIFIER_pLowerThresWaterdepth_DESCRIPTION)
    @Unit("m")
    @In
    public Double pLowerThresWaterdepth = 0.5;

    @Description(OMSINTENSITYCLASSIFIER_inVelocity_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("m/s")
    @In
    public String inVelocity;

    @Description(OMSINTENSITYCLASSIFIER_pUpperThresVelocity_DESCRIPTION)
    @Unit("m/s")
    @In
    public Double pUpperThresVelocity = 1.0;

    @Description(OMSINTENSITYCLASSIFIER_pLowerThresVelocity_DESCRIPTION)
    @Unit("m/s")
    @In
    public Double pLowerThresVelocity = 0.5;

    @Description(OMSINTENSITYCLASSIFIER_inDepositsThickness_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("m")
    @In
    public String inDepositsThickness;

    @Description(OMSINTENSITYCLASSIFIER_pUpperThresDepositsThickness_DESCRIPTION)
    @Unit("m")
    @In
    public Double pUpperThresDepositsThickness = 1.0;

    @Description(OMSINTENSITYCLASSIFIER_pLowerThresDepositsThickness_DESCRIPTION)
    @Unit("m")
    @In
    public Double pLowerThresDepositsThickness = 0.5;

    @Description(OMSINTENSITYCLASSIFIER_inErosionDepth_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @Unit("m")
    @In
    public String inErosionDepth;

    @Description(OMSINTENSITYCLASSIFIER_pUpperThresErosionDepth_DESCRIPTION)
    @Unit("m")
    @In
    public Double pUpperThresErosionDepth = 2.0;

    @Description(OMSINTENSITYCLASSIFIER_pLowerThresErosionDepth_DESCRIPTION)
    @Unit("m")
    @In
    public Double pLowerThresErosionDepth = 0.5;

    @Description(OMSINTENSITYCLASSIFIER_outIntensity_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outIntensity = null;

    @Execute
    public void process() throws Exception {
        OmsIntensityClassifierDebrisFlowTN intensityclassifier = new OmsIntensityClassifierDebrisFlowTN();
        intensityclassifier.inWaterDepth = getRaster(inWaterDepth);
        intensityclassifier.inVelocity = getRaster(inVelocity);
        intensityclassifier.inDepositsThickness = getRaster(inDepositsThickness);
        intensityclassifier.inErosionDepth = getRaster(inErosionDepth);
        intensityclassifier.pUpperThresWaterdepth = pUpperThresWaterdepth;
        intensityclassifier.pLowerThresWaterdepth = pLowerThresWaterdepth;
        intensityclassifier.pUpperThresVelocity = pUpperThresVelocity;
        intensityclassifier.pLowerThresVelocity = pLowerThresVelocity;
        intensityclassifier.pUpperThresDepositsThickness = pUpperThresDepositsThickness;
        intensityclassifier.pLowerThresDepositsThickness = pLowerThresDepositsThickness;
        intensityclassifier.pUpperThresErosionDepth = pUpperThresErosionDepth;
        intensityclassifier.pLowerThresErosionDepth = pLowerThresErosionDepth;
        intensityclassifier.pm = pm;
        intensityclassifier.doProcess = doProcess;
        intensityclassifier.doReset = doReset;
        intensityclassifier.process();
        dumpRaster(intensityclassifier.outIntensity, outIntensity);
    }

}
