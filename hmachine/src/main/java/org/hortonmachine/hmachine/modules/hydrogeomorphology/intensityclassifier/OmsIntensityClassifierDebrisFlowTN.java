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

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.RandomIterFactory;
import org.eclipse.imagen.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
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
@Documentation(OMSINTENSITYCLASSIFIER_DOCUMENTATION)
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
    public static final String OMSINTENSITYCLASSIFIER_DESCRIPTION = "Classifies the intensity of a debris flow, from the maps of its depth, velocity, deposits and erosion, following the guidelines of the Province of Trento (TN): 1 low, 2 medium, 3 high.";
    public static final String OMSINTENSITYCLASSIFIER_DOCUMENTATION = """
            The intensity of each cell is given by four quantities, each with a lower and an upper threshold: the depth of the flow, its velocity outside of the river bed, the thickness of the deposits and the depth of the erosion. The intensity is:

            - high (3), where any of them is over its upper threshold;
            - medium (2), where any of them is over its lower threshold, and none over the upper one;
            - low (1), where all are within their lower thresholds.

            The four maps have to cover the same cells: the cells where only some of them have a value are skipped, with a warning. The cells with all values 0 are classified as low intensity: to keep the cells not reached by the flow out of the map, set them to novalue.

            The intensity maps of the events of different return periods are the input of HazardClassifier.
            """;
    public static final String OMSINTENSITYCLASSIFIER_KEYWORDS = "Raster, Debris flow";
    public static final String OMSINTENSITYCLASSIFIER_LABEL = HYDROGEOMORPHOLOGY;
    public static final String OMSINTENSITYCLASSIFIER_NAME = "intensityclassifierdebrisflowtn";
    public static final int OMSINTENSITYCLASSIFIER_STATUS = 5;
    public static final String OMSINTENSITYCLASSIFIER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSINTENSITYCLASSIFIER_AUTHORNAMES = "Silvia Franceschi, Andrea Antonello";
    public static final String OMSINTENSITYCLASSIFIER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSINTENSITYCLASSIFIER_inWaterDepth_DESCRIPTION = "The map of the depth of the flow.";
    public static final String OMSINTENSITYCLASSIFIER_pUpperThresWaterdepth_DESCRIPTION = "The depth over which the intensity is high.";
    public static final String OMSINTENSITYCLASSIFIER_pLowerThresWaterdepth_DESCRIPTION = "The depth over which the intensity is at least medium.";

    public static final String OMSINTENSITYCLASSIFIER_inVelocity_DESCRIPTION = "The map of the velocity of the flow outside of the river bed.";
    public static final String OMSINTENSITYCLASSIFIER_pUpperThresVelocity_DESCRIPTION = "The velocity over which the intensity is high.";
    public static final String OMSINTENSITYCLASSIFIER_pLowerThresVelocity_DESCRIPTION = "The velocity over which the intensity is at least medium.";

    public static final String OMSINTENSITYCLASSIFIER_inDepositsThickness_DESCRIPTION = "The map of the thickness of the deposits outside of the river bed.";
    public static final String OMSINTENSITYCLASSIFIER_pUpperThresDepositsThickness_DESCRIPTION = "The thickness of the deposits over which the intensity is high.";
    public static final String OMSINTENSITYCLASSIFIER_pLowerThresDepositsThickness_DESCRIPTION = "The thickness of the deposits over which the intensity is at least medium.";

    public static final String OMSINTENSITYCLASSIFIER_inErosionDepth_DESCRIPTION = "The map of the depth of the erosion.";
    public static final String OMSINTENSITYCLASSIFIER_pUpperThresErosionDepth_DESCRIPTION = "The depth of the erosion over which the intensity is high.";
    public static final String OMSINTENSITYCLASSIFIER_pLowerThresErosionDepth_DESCRIPTION = "The depth of the erosion over which the intensity is at least medium.";

    public static final String OMSINTENSITYCLASSIFIER_outIntensity_DESCRIPTION = "The map of the debris flow intensity: 1 low, 2 medium, 3 high.";
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
                    pm.errorMessage("WARNING: a cell was found in which one of velocity, water depth, erosion depth or deposit thickness are novalue, while the other not. \nThe maps should be covering the exact same cells. \nGoing on ignoring the cell: "
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
