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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.debrisflow;

import static java.lang.Math.abs;
import static java.lang.Math.pow;
import static java.lang.Math.round;
import static java.lang.Math.sqrt;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.utils.math.NumericsUtilities.dEq;
import static org.hortonmachine.gears.utils.math.NumericsUtilities.isBetween;
import static org.hortonmachine.gears.utils.math.NumericsUtilities.pythagoras;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_outDepo_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_outMcs_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pDcoeff_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pEasting_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pMcoeff_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pMontecarlo_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pNorthing_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSDEBRISFLOW_pVolume_DESCRIPTION;

import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.TreeSet;

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
import org.geotools.coverage.grid.GridGeometry2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

import org.locationtech.jts.geom.Coordinate;

@Description(OMSDEBRISFLOW_DESCRIPTION)
@Author(name = OMSDEBRISFLOW_AUTHORNAMES, contact = OMSDEBRISFLOW_AUTHORCONTACTS)
@Keywords(OMSDEBRISFLOW_KEYWORDS)
@Label(OMSDEBRISFLOW_LABEL)
@Name(OMSDEBRISFLOW_NAME)
@Status(OMSDEBRISFLOW_STATUS)
@License(OMSDEBRISFLOW_LICENSE)
public class OmsDebrisFlow extends HMModel {
    @Description(OMSDEBRISFLOW_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSDEBRISFLOW_pVolume_DESCRIPTION)
    @Unit("m2")
    @In
    public double pVolume = 4000;

    @Description(OMSDEBRISFLOW_pMcoeff_DESCRIPTION)
    @Unit("-")
    @In
    public double pMcoeff = 52;

    @Description(OMSDEBRISFLOW_pDcoeff_DESCRIPTION)
    @Unit("-")
    @In
    public double pDcoeff = 0.06;

    @Description(OMSDEBRISFLOW_pEasting_DESCRIPTION)
    @Unit("m")
    @In
    public double pEasting = 143;

    @Description(OMSDEBRISFLOW_pNorthing_DESCRIPTION)
    @Unit("m")
    @In
    public double pNorthing = 604;

    @Description(OMSDEBRISFLOW_pMontecarlo_DESCRIPTION)
    @In
    public int pMontecarlo = 50;

    @Description(OMSDEBRISFLOW_outMcs_DESCRIPTION)
    @Out
    public GridCoverage2D outMcs = null;

    @Description(OMSDEBRISFLOW_outDepo_DESCRIPTION)
    @Out
    public GridCoverage2D outDepo = null;

    @Execute
    public void process() throws Exception {
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xRes = regionMap.getXres();
        double yRes = regionMap.getYres();
        double west = regionMap.getWest();
        double east = regionMap.getEast();
        double south = regionMap.getSouth();
        double north = regionMap.getNorth();
        if (!isBetween(pEasting, west, east) || !isBetween(pNorthing, south, north)) {
            throw new ModelsIllegalargumentException("Input coordinates have to be within the map boundaries.", this, pm);
        }

        double thresArea = pMcoeff * pow(pVolume, (2.0 / 3.0));

        GridGeometry2D gridGeometry = inElev.getGridGeometry();
        int[] colRow = CoverageUtilities.colRowFromCoordinate(new Coordinate(pEasting, pNorthing), gridGeometry, null);

        RandomIter elevIter = CoverageUtilities.getRandomIterator(inElev);
        int startCol = colRow[0];
        int startRow = colRow[1];
        double startValue = elevIter.getSampleDouble(startCol, startRow, 0);
        if (isNovalue(startValue)) {
            throw new ModelsIllegalargumentException("Input coordinates are on a novalue elevation point.", this, pm);
        }

        WritableRaster mcsWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        WritableRandomIter probIter = RandomIterFactory.createWritable(mcsWR, null);

        Random flatRnd = new Random();
        int processedMc = 0;
        for( int mc = 0; mc < pMontecarlo; mc++ ) {
            pm.message("Montecarlo n." + mc);
            processedMc = mc;
            /*
             * for every Montecarlo loop, get a flow path
             */
            int centerCol = startCol;
            int centerRow = startRow;

            TreeSet<Point> touchedPoints = new TreeSet<Point>();
            touchedPoints.add(new Point(centerCol, centerRow));

            boolean doStop = false;
            // cicle over every cell neighbours along the way
            Random randomGenerator = new Random();// -System.currentTimeMillis());
            do {
                // System.out.println(centerCol + "/" + centerRow + " --- " + cols + "/" + rows);
                double centerValue = elevIter.getSampleDouble(centerCol, centerRow, 0);

                List<SlopeProbability> spList = new ArrayList<SlopeProbability>();
                double slopeSum = 0;
                for( int x = -1; x <= 1; x++ ) {
                    for( int y = -1; y <= 1; y++ ) {
                        if (x == 0 && y == 0) {
                            continue;
                        }

                        int tmpCol = centerCol + x;
                        int tmpRow = centerRow + y;

                        if (touchedPoints.contains(new Point(tmpCol, tmpRow))) {
                            continue;
                        }

                        // if point is outside jump it
                        if (!isBetween(tmpCol, 0, cols - 1) || !isBetween(tmpRow, 0, rows - 1)) {
                            continue;
                        }

                        // if point is novalue, jump it
                        double nextValue = elevIter.getSampleDouble(tmpCol, tmpRow, 0);
                        if (isNovalue(nextValue)) {
                            continue;
                        }

                        double distance = pythagoras(abs((tmpCol - centerCol) * xRes), abs((tmpRow - centerRow) * yRes));
                        double slope = (nextValue - centerValue) / distance;

                        // System.out.println(sp);

                        // we take only negative and 0 slope, downhill
                        if (slope > 0) {
                            continue;
                        }

                        slope = abs(slope);

                        SlopeProbability sp = new SlopeProbability();
                        sp.fromCol = centerCol;
                        sp.fromRow = centerRow;
                        sp.fromElev = centerValue;
                        sp.toCol = tmpCol;
                        sp.toRow = tmpRow;
                        sp.toElev = nextValue;
                        sp.slope = slope;
                        slopeSum = slopeSum + slope;
                        spList.add(sp);
                    }
                }

                if (spList.size() == 0) {
                    /*
                     * touched border or slope is not negative
                     */
                    doStop = true;
                } else {

                    // get a random number between 0 and 1
                    double random = randomGenerator.nextDouble();

                    if (spList.size() == 1) {
                        // direction is only one
                        SlopeProbability sp = spList.get(0);
                        centerCol = sp.toCol;
                        centerRow = sp.toRow;
                    } else {
                        Collections.sort(spList);

                        /*
                         * case in which the slopes are all 0
                         */
                        if (dEq(slopeSum, 0.0)) {
                            // choose a random and go on
                            int size = spList.size();

                            double rnd = flatRnd.nextDouble();
                            int index = (int) round(rnd * size) - 1;
                            if (index < 0)
                                index = 0;
                            SlopeProbability sp = spList.get(index);
                            centerCol = sp.toCol;
                            centerRow = sp.toRow;
                        }
                        /*
                         * normal case in which the slopes have a value
                         */
                        else {
                            // cumulate the probability
                            for( int i = 0; i < spList.size(); i++ ) {
                                SlopeProbability sp = spList.get(i);
                                double p = sp.slope / slopeSum;
                                sp.probability = p;

                                if (i != 0) {
                                    SlopeProbability tmpSp = spList.get(i - 1);
                                    sp.probability = sp.probability + tmpSp.probability;
                                }
                            }

                            for( int i = 1; i < spList.size(); i++ ) {
                                SlopeProbability sp1 = spList.get(i - 1);
                                SlopeProbability sp2 = spList.get(i);

                                // if (random < sp1.probability) {
                                if (random < sp1.probability) {
                                    centerCol = sp1.toCol;
                                    centerRow = sp1.toRow;
                                    break;
                                    // } else if (random >= sp1.probability && random <
                                    // sp2.probability)
                                    // {
                                } else if (random >= sp1.probability && random < sp2.probability) {
                                    centerCol = sp2.toCol;
                                    centerRow = sp2.toRow;
                                    break;
                                }
                            }
                        }
                    }
                    touchedPoints.add(new Point(centerCol, centerRow));
                    double outValue = probIter.getSampleDouble(centerCol, centerRow, 0);
                    if (isNovalue(outValue)) {
                        outValue = 0.0;
                    }
                    probIter.setSample(centerCol, centerRow, 0, outValue + 1.0);
                }
            } while( !doStop );

            /*
             * check if the max area is flooded
             */
            int floodedCellNum = 0;
            for( int r = 0; r < rows; r++ ) {
                for( int c = 0; c < cols; c++ ) {
                    double value = probIter.getSampleDouble(c, r, 0);
                    if (isNovalue(value)) {
                        continue;
                    }
                    floodedCellNum++;
                }
            }

            double floodedArea = floodedCellNum * xRes * yRes;
            if (thresArea <= floodedArea) {
                break;
            }

        }

        double probSum = 0.0;
        double validCells = 0.0;
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double prob = probIter.getSampleDouble(c, r, 0);
                if (isNovalue(prob)) {
                    continue;
                }
                double newProb = prob / (processedMc - 1);
                probIter.setSample(c, r, 0, newProb);
                probSum = probSum + sqrt(newProb);
                validCells++;
            }
        }

        /*
         * calculate deposition
         */
        double avgProb = probSum / validCells;
        double avgHeight = pDcoeff * pow(pVolume, 1.0 / 3.0);

        WritableRaster depoWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        WritableRandomIter depoIter = RandomIterFactory.createWritable(depoWR, null);

        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double probValue = probIter.getSampleDouble(c, r, 0);
                if (isNovalue(probValue)) {
                    continue;
                }
                double depoValue = avgHeight * sqrt(probValue) / avgProb;
                depoIter.setSample(c, r, 0, depoValue);
            }
        }

        outMcs = CoverageUtilities.buildCoverage("mcs", mcsWR, regionMap, inElev.getCoordinateReferenceSystem());
        outDepo = CoverageUtilities.buildCoverage("depo", depoWR, regionMap, inElev.getCoordinateReferenceSystem());

    }

    public class Point implements Comparable<Point> {
        public int col;
        public int row;

        public Point( int col, int row ) {
            this.col = col;
            this.row = row;
        }

        public int compareTo( Point o ) {
            if (col == o.col && row == o.row) {
                return 0;
            }
            return 1;
        }

    }
}
