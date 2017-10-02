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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsHazardClassifier.*;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierDebrisFlowTN.*;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

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
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

@Description(OMSINTENSITYCLASSIFIER_DESCRIPTION)
@Author(name = OMSINTENSITYCLASSIFIER_AUTHORNAMES, contact = OMSINTENSITYCLASSIFIER_AUTHORCONTACTS)
@Keywords(OMSINTENSITYCLASSIFIER_KEYWORDS)
@Label(OMSINTENSITYCLASSIFIER_LABEL)
@Name(OMSINTENSITYCLASSIFIER_NAME)
@Status(OMSINTENSITYCLASSIFIER_STATUS)
@License(OMSINTENSITYCLASSIFIER_LICENSE)
public class OmsIntensityClassifierDebrisFlowTN extends HMModel {

    @Description(OMSINTENSITYCLASSIFIER_inWaterDepth_DESCRIPTION)
    @Unit("m")
    @In
    public GridCoverage2D inWaterDepth;

    @Description(OMSINTENSITYCLASSIFIER_pUpperThresWaterdepth_DESCRIPTION)
    @Unit("m")
    @In
    public Double pUpperThresWaterdepth = 1.0;

    @Description(OMSINTENSITYCLASSIFIER_pLowerThresWaterdepth_DESCRIPTION)
    @Unit("m")
    @In
    public Double pLowerThresWaterdepth = 0.5;

    @Description(OMSINTENSITYCLASSIFIER_inVelocity_DESCRIPTION)
    @Unit("m/s")
    @In
    public GridCoverage2D inVelocity;

    @Description(OMSINTENSITYCLASSIFIER_pUpperThresVelocity_DESCRIPTION)
    @Unit("m/s")
    @In
    public Double pUpperThresVelocity = 1.0;

    @Description(OMSINTENSITYCLASSIFIER_pLowerThresVelocity_DESCRIPTION)
    @Unit("m/s")
    @In
    public Double pLowerThresVelocity = 0.5;

    @Description(OMSINTENSITYCLASSIFIER_inDepositsThickness_DESCRIPTION)
    @Unit("m")
    @In
    public GridCoverage2D inDepositsThickness;

    @Description(OMSINTENSITYCLASSIFIER_pUpperThresDepositsThickness_DESCRIPTION)
    @Unit("m")
    @In
    public Double pUpperThresDepositsThickness = 1.0;

    @Description(OMSINTENSITYCLASSIFIER_pLowerThresDepositsThickness_DESCRIPTION)
    @Unit("m")
    @In
    public Double pLowerThresDepositsThickness = 0.5;

    @Description(OMSINTENSITYCLASSIFIER_inErosionDepth_DESCRIPTION)
    @Unit("m")
    @In
    public GridCoverage2D inErosionDepth;

    @Description(OMSINTENSITYCLASSIFIER_pUpperThresErosionDepth_DESCRIPTION)
    @Unit("m")
    @In
    public Double pUpperThresErosionDepth = 2.0;

    @Description(OMSINTENSITYCLASSIFIER_pLowerThresErosionDepth_DESCRIPTION)
    @Unit("m")
    @In
    public Double pLowerThresErosionDepth = 0.5;

    @Description(OMSINTENSITYCLASSIFIER_outIntensity_DESCRIPTION)
    @Out
    public GridCoverage2D outIntensity = null;

    // VARS DOC START
    public static final String OMSINTENSITYCLASSIFIER_DESCRIPTION = "Module for the calculation of the debris flow intensity following TN guidelines.";
    public static final String OMSINTENSITYCLASSIFIER_DOCUMENTATION = "";
    public static final String OMSINTENSITYCLASSIFIER_KEYWORDS = "Raster, Flooding";
    public static final String OMSINTENSITYCLASSIFIER_LABEL = HYDROGEOMORPHOLOGY;
    public static final String OMSINTENSITYCLASSIFIER_NAME = "intensityclassifierdebrisflowtn";
    public static final int OMSINTENSITYCLASSIFIER_STATUS = 5;
    public static final String OMSINTENSITYCLASSIFIER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSINTENSITYCLASSIFIER_AUTHORNAMES = "Silvia Franceschi, Andrea Antonello";
    public static final String OMSINTENSITYCLASSIFIER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSINTENSITYCLASSIFIER_inWaterDepth_DESCRIPTION = "The map of the water depth.";
    public static final String OMSINTENSITYCLASSIFIER_pUpperThresWaterdepth_DESCRIPTION = "The upper threshold value for the water depth.";
    public static final String OMSINTENSITYCLASSIFIER_pLowerThresWaterdepth_DESCRIPTION = "The lower threshold value for the water depth.";

    public static final String OMSINTENSITYCLASSIFIER_inVelocity_DESCRIPTION = "The map of the water velocity outside of the river bed.";
    public static final String OMSINTENSITYCLASSIFIER_pUpperThresVelocity_DESCRIPTION = "The upper threshold value of the water velocity outside of the river bed.";
    public static final String OMSINTENSITYCLASSIFIER_pLowerThresVelocity_DESCRIPTION = "The lower threshold value of the water velocity outside of the river bed.";

    public static final String OMSINTENSITYCLASSIFIER_inDepositsThickness_DESCRIPTION = "The map of the thickness of the deposits outside the bed of the river.";
    public static final String OMSINTENSITYCLASSIFIER_pUpperThresDepositsThickness_DESCRIPTION = "The upper threshold value for the deposits thickness.";
    public static final String OMSINTENSITYCLASSIFIER_pLowerThresDepositsThickness_DESCRIPTION = "The lower threshold value for the deposits thickness.";

    public static final String OMSINTENSITYCLASSIFIER_inErosionDepth_DESCRIPTION = "The map of erosion depth.";
    public static final String OMSINTENSITYCLASSIFIER_pUpperThresErosionDepth_DESCRIPTION = "The upper threshold value for the erosion depth.";
    public static final String OMSINTENSITYCLASSIFIER_pLowerThresErosionDepth_DESCRIPTION = "The lower threshold value for the erosion depth.";

    public static final String OMSINTENSITYCLASSIFIER_outIntensity_DESCRIPTION = "The map of flooding intensity.";
    // VARS DOC END

    @Execute
    public void process() throws Exception {
        if (!concatOr(outIntensity == null, doReset)) {
            return;
        }

        checkNull(inVelocity, pUpperThresVelocity, pLowerThresVelocity,//
                inWaterDepth, pUpperThresWaterdepth, pLowerThresWaterdepth, //
                inErosionDepth, pUpperThresErosionDepth, pLowerThresErosionDepth, //
                inDepositsThickness, pUpperThresDepositsThickness, pLowerThresDepositsThickness //
        );

        // do autoboxing only once
        double maxWD = pUpperThresWaterdepth;
        double minWD = pLowerThresWaterdepth;
        double maxV = pUpperThresVelocity;
        double minV = pLowerThresVelocity;
        double maxED = pUpperThresErosionDepth;
        double minED = pLowerThresErosionDepth;
        double maxDT = pUpperThresDepositsThickness;
        double minDT = pLowerThresDepositsThickness;

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inWaterDepth);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RandomIter waterdepthIter = CoverageUtilities.getRandomIterator(inWaterDepth);
        RandomIter velocityIter = CoverageUtilities.getRandomIterator(inVelocity);
        RandomIter erosiondepthIter = CoverageUtilities.getRandomIterator(inErosionDepth);
        RandomIter depositThicknessIter = CoverageUtilities.getRandomIterator(inDepositsThickness);

        WritableRaster outWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        pm.beginTask("Processing map...", nRows);
        for( int r = 0; r < nRows; r++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int c = 0; c < nCols; c++ ) {

                double h = waterdepthIter.getSampleDouble(c, r, 0);
                double v = velocityIter.getSampleDouble(c, r, 0);
                double ed = erosiondepthIter.getSampleDouble(c, r, 0);
                double dt = depositThicknessIter.getSampleDouble(c, r, 0);

                if (isNovalue(h) && isNovalue(v) && isNovalue(ed) && isNovalue(dt)) {
                    continue;
                } else if (!isNovalue(h) && !isNovalue(v) && !isNovalue(ed) && !isNovalue(dt)) {
                    double value = 0.0;

                    if (h > maxWD || v > maxV || dt > maxDT || ed > maxED) {
                        value = INTENSITY_HIGH;
                    } else if ((h <= maxWD && h > minWD) || //
                            (v <= maxV && v > minV) || //
                            (dt <= maxDT && dt > minDT) || //
                            (ed <= maxED && ed > minED)) {
                        value = INTENSITY_MEDIUM;
                    } else if (h <= minWD || v <= minV || dt <= minDT || ed <= minED) {
                        value = INTENSITY_LOW;
                    } else {
                        throw new ModelsIllegalargumentException("No intensity could be calculated for h = " + h + " and v = "
                                + v + " and ed = " + ed + " and dt = " + dt, this, pm);
                    }

                    outIter.setSample(c, r, 0, value);
                } else {
                    pm.errorMessage("WARNING: a cell was found in which one of velocity, water depth, erosion depth or deposit thickness are novalue, while the other not. /nThe maps should be covering the exact same cells. /nGoing on ignoring the cell: "
                            + c + "/" + r);
                }
            }
            pm.worked(1);
        }
        pm.done();

        outIntensity = CoverageUtilities
                .buildCoverage("intensity", outWR, regionMap, inWaterDepth.getCoordinateReferenceSystem());

    }
}
