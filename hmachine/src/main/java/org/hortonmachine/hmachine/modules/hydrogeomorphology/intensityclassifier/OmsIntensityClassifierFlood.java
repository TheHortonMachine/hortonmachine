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
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.intensityclassifier.OmsIntensityClassifierFlood.*;

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
public class OmsIntensityClassifierFlood extends HMModel {

    @Description(OMSINTENSITYCLASSIFIER_inWaterDepth_DESCRIPTION)
    @Unit("m")
    @In
    public GridCoverage2D inWaterDepth;

    @Description(OMSINTENSITYCLASSIFIER_inVelocity_DESCRIPTION)
    @Unit("m/s")
    @In
    public GridCoverage2D inVelocity;

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
    @Out
    public GridCoverage2D outIntensity = null;

    // VARS DOC START
    public static final String OMSINTENSITYCLASSIFIER_DESCRIPTION = "Module for the calculation of the flooding intensity.";
    public static final String OMSINTENSITYCLASSIFIER_DOCUMENTATION = "";
    public static final String OMSINTENSITYCLASSIFIER_KEYWORDS = "Raster, Flooding";
    public static final String OMSINTENSITYCLASSIFIER_LABEL = HYDROGEOMORPHOLOGY;
    public static final String OMSINTENSITYCLASSIFIER_NAME = "intensityclassifier";
    public static final int OMSINTENSITYCLASSIFIER_STATUS = 5;
    public static final String OMSINTENSITYCLASSIFIER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSINTENSITYCLASSIFIER_AUTHORNAMES = "Silvia Franceschi, Andrea Antonello";
    public static final String OMSINTENSITYCLASSIFIER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSINTENSITYCLASSIFIER_inWaterDepth_DESCRIPTION = "The map of the water depth.";
    public static final String OMSINTENSITYCLASSIFIER_inVelocity_DESCRIPTION = "The map of the water velocity.";
    public static final String OMSINTENSITYCLASSIFIER_pUpperThresWaterdepth_DESCRIPTION = "The upper threshold value for the water depth.";
    public static final String OMSINTENSITYCLASSIFIER_pUpperThresVelocityWaterdepth_DESCRIPTION = "The upper threshold value for the product of water depth and velocity.";
    public static final String OMSINTENSITYCLASSIFIER_pLowerThresWaterdepth_DESCRIPTION = "The lower threshold value for the water depth.";
    public static final String OMSINTENSITYCLASSIFIER_pLowerThresVelocityWaterdepth_DESCRIPTION = "The lower threshold value for the product of water depth and velocity.";
    public static final String OMSINTENSITYCLASSIFIER_outIntensity_DESCRIPTION = "The map of flooding intensity.";
    // VARS DOC END

    @Execute
    public void process() throws Exception {
        if (!concatOr(outIntensity == null, doReset)) {
            return;
        }

        checkNull(inWaterDepth, inVelocity, pUpperThresVelocityWaterdepth, pUpperThresWaterdepth, pLowerThresVelocityWaterdepth,
                pLowerThresWaterdepth);

        // do autoboxing only once
        double maxWD = pUpperThresWaterdepth;
        double maxVWD = pUpperThresVelocityWaterdepth;
        double minWD = pLowerThresWaterdepth;
        double minVWD = pLowerThresVelocityWaterdepth;

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inWaterDepth);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RandomIter waterdepthIter = CoverageUtilities.getRandomIterator(inWaterDepth);
        RandomIter velocityIter = CoverageUtilities.getRandomIterator(inVelocity);

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

                if (isNovalue(h) && isNovalue(v)) {
                    continue;
                } else if (!isNovalue(h) && !isNovalue(v)) {
                    double value = 0.0;
                    double vh = v * h;
                    if (h > maxWD || vh > maxVWD) {
                        value = INTENSITY_HIGH;
                    } else if ((h > minWD && h <= maxWD) || (vh > minVWD && vh <= maxVWD)) {
                        value = INTENSITY_MEDIUM;
                    } else if (h <= minWD && vh <= minVWD) {
                        value = INTENSITY_LOW;
                    } else {
                        throw new ModelsIllegalargumentException("No intensity could be calculated for h = " + h + " and v = "
                                + v, this, pm);
                    }
                    outIter.setSample(c, r, 0, value);
                } else {
                    pm.errorMessage("WARNING: a cell was found in which one of velocity and water depth are novalue, while the other not. /nThe maps should be covering the exact same cells. /nGoing on ignoring the cell: "
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
