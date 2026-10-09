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

import static java.lang.Math.max;
import static org.hortonmachine.gears.libs.modules.HMConstants.*;
import static org.hortonmachine.gears.utils.math.NumericsUtilities.dEq;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

@Description(OmsHazardClassifier.DESCRIPTION)
@Documentation(OmsHazardClassifier.DOCUMENTATION)
@Author(name = OmsHazardClassifier.AUTHORS, contact = OmsHazardClassifier.CONTACT)
@Keywords(OmsHazardClassifier.KEYWORDS)
@Label(OmsHazardClassifier.LABEL)
@Name(OmsHazardClassifier.NAME)
@Status(OmsHazardClassifier.STATUS)
@License(OmsHazardClassifier.LICENSE)
public class OmsHazardClassifier extends HMModel {

    @Description(inIntensityTr200_DESCR)
    @In
    public GridCoverage2D inIntensityTr200;

    @Description(inIntensityTr100_DESCR)
    @In
    public GridCoverage2D inIntensityTr100;

    @Description(inIntensityTr30_DESCR)
    @In
    public GridCoverage2D inIntensityTr30;

    @Description(outHazardIP1_DESCR)
    @Out
    public GridCoverage2D outHazardIP1 = null;

    @Description(outHazardIP2_DESCR)
    @Out
    public GridCoverage2D outHazardIP2 = null;

    // VARS DOC START
    public static final String LICENSE = "General Public License Version 3 (GPLv3)";
    public static final int STATUS = 5;
    public static final String LABEL = "HortonMachine/Hydro-Geomorphology";
    public static final String NAME = "hazardclassifier";
    public static final String KEYWORDS = "Raster, Flooding, Hazard";
    public static final String CONTACT = "http://www.hydrologis.com";
    public static final String AUTHORS = "Silvia Franceschi, Andrea Antonello";
    public static final String DESCRIPTION = "Classifies the hazard from the intensity maps of the events of 30, 100 and 200 years of return period: two hazard maps, IP1 and IP2, with classes from 2 (low) to 4 (high).";
    public static final String DOCUMENTATION = """
            The intensity maps are the ones of IntensityClassifierFlood or IntensityClassifierDebrisFlowTN, with the classes 1 low, 2 medium and 3 high. A cell without a value in a map is not reached by that event.

            Each intensity of each return period gets a score, higher for the more intense and the more frequent events:

            | Intensity | Tr 30 | Tr 100 | Tr 200 |
            |---|---|---|---|
            | high (3) | 9 | 8 | 7 |
            | medium (2) | 6 | 5 | 4 |
            | low (1) | 3 | 2 | 1 |

            The highest score of the three events gives the hazard of the cell, in two classifications that differ only in where the classes change:

            | Score | 1 | 2 | 3 | 4 | 5 | 6 | 7 | 8 | 9 |
            |---|---|---|---|---|---|---|---|---|---|
            | IP1 | 2 | 3 | 3 | 3 | 3 | 4 | 4 | 4 | 4 |
            | IP2 | 2 | 2 | 3 | 3 | 3 | 3 | 4 | 4 | 4 |

            So with IP1 a medium intensity of the event of 30 years is already a high hazard, and only a low intensity of the event of 200 years is a low hazard; with IP2 a high hazard needs a high intensity, and the low intensities of the events of 100 and 200 years are a low hazard.
            """;
    public static final String outHazardIP2_DESCR = "The hazard map IP2: 2 low, 3 medium, 4 high.";
    public static final String outHazardIP1_DESCR = "The hazard map IP1: 2 low, 3 medium, 4 high.";
    public static final String inIntensityTr30_DESCR = "The intensity map of the event of 30 years of return period.";
    public static final String inIntensityTr100_DESCR = "The intensity map of the event of 100 years of return period.";
    public static final String inIntensityTr200_DESCR = "The intensity map of the event of 200 years of return period.";
    // VARS DOC END

    public static final double INTENSITY_HIGH = 3.0;
    public static final double INTENSITY_MEDIUM = 2.0;
    public static final double INTENSITY_LOW = 1.0;

    @Execute
    public void process() throws Exception {

        checkNull(inIntensityTr100, inIntensityTr200, inIntensityTr30);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inIntensityTr30);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        RandomIter tr200Iter = CoverageUtilities.getRandomIterator(inIntensityTr200);
        RandomIter tr100Iter = CoverageUtilities.getRandomIterator(inIntensityTr100);
        RandomIter tr30Iter = CoverageUtilities.getRandomIterator(inIntensityTr30);

        WritableRaster outIP1WR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter outIP1Iter = RandomIterFactory.createWritable(outIP1WR, null);
        WritableRaster outIP2WR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter outIP2Iter = RandomIterFactory.createWritable(outIP2WR, null);

        pm.beginTask("Processing map...", nRows);
        for( int r = 0; r < nRows; r++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int c = 0; c < nCols; c++ ) {
                double tr30 = tr30Iter.getSampleDouble(c, r, 0);
                double tr100 = tr100Iter.getSampleDouble(c, r, 0);
                double tr200 = tr200Iter.getSampleDouble(c, r, 0);
                if (isNovalue(tr30) && isNovalue(tr100) && isNovalue(tr200)) {
                    continue;
                }

                double tmpTr30;
                if (isNovalue(tr30)) {
                    tmpTr30 = Double.NEGATIVE_INFINITY;
                } else if (dEq(tr30, INTENSITY_LOW)) {
                    tmpTr30 = 3.0;
                } else if (dEq(tr30, INTENSITY_MEDIUM)) {
                    tmpTr30 = 6.0;
                } else if (dEq(tr30, INTENSITY_HIGH)) {
                    tmpTr30 = 9.0;
                } else {
                    throw new ModelsIllegalargumentException("Unknown tr30 value: " + tr30, this, pm);
                }
                double tmpTr100;
                if (isNovalue(tr100)) {
                    tmpTr100 = Double.NEGATIVE_INFINITY;
                } else if (dEq(tr100, INTENSITY_LOW)) {
                    tmpTr100 = 2.0;
                } else if (dEq(tr100, INTENSITY_MEDIUM)) {
                    tmpTr100 = 5.0;
                } else if (dEq(tr100, INTENSITY_HIGH)) {
                    tmpTr100 = 8.0;
                } else {
                    throw new ModelsIllegalargumentException("Unknown tr100 value: " + tr100, this, pm);
                }
                double tmpTr200;
                if (isNovalue(tr200)) {
                    tmpTr200 = Double.NEGATIVE_INFINITY;
                } else if (dEq(tr200, INTENSITY_LOW)) {
                    tmpTr200 = 1.0;
                } else if (dEq(tr200, INTENSITY_MEDIUM)) {
                    tmpTr200 = 4.0;
                } else if (dEq(tr200, INTENSITY_HIGH)) {
                    tmpTr200 = 7.0;
                } else {
                    throw new ModelsIllegalargumentException("Unknown tr200 value: " + tr200, this, pm);
                }

                int maxValue = (int) max(tmpTr30, max(tmpTr100, tmpTr200));
                double[] reclassIP1 = {Double.NaN, //
                        2, // 1
                        3, // 2
                        3, // 3
                        3, // 4
                        3, // 5
                        4, // 6
                        4, // 7
                        4, // 8
                        4 // 9
                };
                double[] reclassIP2 = {Double.NaN, //
                        2, // 1
                        2, // 2
                        3, // 3
                        3, // 4
                        3, // 5
                        3, // 6
                        4, // 7
                        4, // 8
                        4 // 9
                };

                if (maxValue < 1 || maxValue > (reclassIP1.length - 1)) {
                    throw new ModelsIllegalargumentException("Unknown max value from tr30/100/200: " + maxValue, this, pm);
                }
                double ip1 = reclassIP1[(int) maxValue];
                double ip2 = reclassIP2[(int) maxValue];

                outIP1Iter.setSample(c, r, 0, ip1);
                outIP2Iter.setSample(c, r, 0, ip2);
            }
            pm.worked(1);
        }
        pm.done();

        outHazardIP1 = CoverageUtilities.buildCoverage("ip1", outIP1WR, regionMap,
                inIntensityTr100.getCoordinateReferenceSystem());
        outHazardIP2 = CoverageUtilities.buildCoverage("ip2", outIP2WR, regionMap,
                inIntensityTr100.getCoordinateReferenceSystem());

    }
}
