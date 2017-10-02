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
package org.hortonmachine.hmachine.modules.demmanipulation.pitfiller;

import static org.hortonmachine.gears.libs.modules.HMConstants.DEMMANIPULATION;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsSupporter;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

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

@Description(OmsPitfiller.OMSPITFILLER_DESCRIPTION)
@Author(name = OmsPitfiller.OMSPITFILLER_AUTHORNAMES, contact = OmsPitfiller.OMSPITFILLER_AUTHORCONTACTS)
@Keywords(OmsPitfiller.OMSPITFILLER_KEYWORDS)
@Label(OmsPitfiller.OMSPITFILLER_LABEL)
@Name(OmsPitfiller.OMSPITFILLER_NAME)
@Status(OmsPitfiller.OMSPITFILLER_STATUS)
@License(OmsPitfiller.OMSPITFILLER_LICENSE)
public class OmsPitfiller extends HMModel {
    @Description(OMSPITFILLER_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev;

    @Description(OMSPITFILLER_outPit_DESCRIPTION)
    @Out
    public GridCoverage2D outPit = null;
    
    public static final String OMSPITFILLER_DESCRIPTION = "It fills the depression points present within a DEM.";
    public static final String OMSPITFILLER_DOCUMENTATION = "OmsPitfiller.html";
    public static final String OMSPITFILLER_KEYWORDS = "Dem manipulation, Geomorphology, OmsDrainDir";
    public static final String OMSPITFILLER_LABEL = DEMMANIPULATION;
    public static final String OMSPITFILLER_NAME = "pit";
    public static final int OMSPITFILLER_STATUS = 40;
    public static final String OMSPITFILLER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSPITFILLER_AUTHORNAMES = "David Tarboton, Andrea Antonello";
    public static final String OMSPITFILLER_AUTHORCONTACTS = "http://www.neng.usu.edu/cee/faculty/dtarb/tardem.html#programs, http://www.hydrologis.com";
    public static final String OMSPITFILLER_inElev_DESCRIPTION = "The map of digital elevation model (DEM).";
    public static final String OMSPITFILLER_outPit_DESCRIPTION = "The depitted elevation map.";

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    /**
     * The novalue needed by PitFiller.
     */
    public static final double PITNOVALUE = -1.0;
    private WritableRandomIter pitIter;
    private RandomIter elevationIter = null;

    private int nCols;
    private int nRows;
    private double xRes;
    private double yRes;

    // private final static String modelParameters = eu.hydrologis.libs.messages.help.Messages
    // .getString("h_pitfiller.usage");
    /**
     * i1, i2, n1, n2, are the minimum and maximum index for the activeRegion
     * matrix (from 0 to nColumns or nRows).
     */
    private int firstCol;
    private int firstRow;
    private int lastCol;
    private int lastRow;
    /**
     * The number of unresolved pixel in dir matrix (which haven't a drainage direction).
     */
    private int currentPitsCount;
    /**
     * Vector where the program memorizes the index of the elevation matrix whose point doesn't drain
     * in any D8 cells.
     */
    private int[] currentPitRows;
    /**
     * Vector where the program memorizes the index of the elevation matrix whose point doesn't drain
     * in any D8 cells.
     */
    private int[] currentPitCols;

    /**
     * Dimension of the temporary vectors which allow to resolve the undrainage pixel.
     */
    private int pitsStackSize;
    /**
     * Dimension of the temporary vectors which allow to resolve the undrainage pixel.
     */
    private int pstack;

    private int nf;
    private int pooln;
    /**
     * Used to memorise the index of the "pixel pool".
     */
    private int[] ipool;
    /**
     * Used to memorise the index of the "pixel pool".
     */
    private int[] jpool;

    private int[] dn;

    private int[][] dir, apool;
    private int[][] DIR_WITHFLOW_EXITING_INVERTED = ModelsSupporter.DIR_WITHFLOW_EXITING_INVERTED;
    private double et, emin;

    /**
     * The pitfiller algorithm.
     * 
     * @throws Exception 
     **/
    @Execute
    public void process() throws Exception {
        if (!concatOr(outPit == null, doReset)) {
            return;
        }
        checkNull(inElev);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        elevationIter = CoverageUtilities.getRandomIterator(inElev);

        // output raster
        WritableRaster pitRaster = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, null);
        pitIter = CoverageUtilities.getWritableRandomIterator(pitRaster);

        for( int i = 0; i < nRows; i++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int j = 0; j < nCols; j++ ) {
                double value = elevationIter.getSampleDouble(j, i, 0);
                if (!isNovalue(value)) {
                    pitIter.setSample(j, i, 0, value);
                } else {
                    pitIter.setSample(j, i, 0, PITNOVALUE);
                }
            }
        }

        flood();
        if (pm.isCanceled()) {
            return;
        }

        for( int i = 0; i < nRows; i++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int j = 0; j < nCols; j++ ) {
                if (dir[j][i] == 0) {
                    return;
                }
                double value = pitIter.getSampleDouble(j, i, 0);
                if (value == PITNOVALUE || isNovalue(value)) {
                    pitIter.setSample(j, i, 0, doubleNovalue);
                }
            }
        }
        pitIter.done();

        outPit = CoverageUtilities.buildCoverage("pitfiller", pitRaster, regionMap, inElev.getCoordinateReferenceSystem());
    }

    /**
     * Takes the elevation matrix and calculate a matrix with pits filled, using the flooding
     * algorithm.
     * 
     * @throws Exception 
     */
    private void flood() throws Exception {

        /* define directions */
        // Initialise the vector to a supposed dimension, if the number of
        // unresolved pixel overload the vector there are a method which resized
        // the vectors.
        pitsStackSize = (int) (nCols * nRows * 0.1);
        pstack = pitsStackSize;
        dn = new int[pitsStackSize];
        currentPitRows = new int[pitsStackSize];
        currentPitCols = new int[pitsStackSize];
        ipool = new int[pstack];
        jpool = new int[pstack];
        firstCol = 0;
        firstRow = 0;
        lastCol = nCols;
        lastRow = nRows;

        setdf();

    }

    /**
     * Initialise the dir matrix and then call the set method to obtain a D8 matrix
     * <p>
     * It's possible summarise the logical pitfiller stream like:
     * <ol>
     * <li>Initialise DIR to 0 (no flood) if the elevation value is valid and isn't apixel on the
     * edge</li>
     * <li>If the pixel have as DIR[j][i] a valid value then check if the adjacent pixels are valid
     * values:
     * <dl>
     * <dt>Yes, there are only valid value
     * <dd>then set the DIR value to the drainage direction (if slope is greater than 0) or to 0 if
     * there aren't.
     * <dt>No, there are an invalid value
     * <dd>set DIR to -1 (impossible value)
     * </dl>
     * </li>
     * <li>If DIR=0 then keep in temporary vector the index of this pixel.</li>
     * <li>Call <b>vdn</b> method, which assign the drainage value if the slope is greater or equal
     * to 0. Notice that neighbour pixel are valid pixel if DIR=0. And then calculate the minimum
     * elevation point (where there are a pool)</li>
     * <li>Start a conditioning cycle (while there are a cell which have as a drainage direction 0
     * (is a pool);<ol type=a>
     * <li>Find if there are a pool and check the pixels which belong to</li>
     *<li>Check the lowest point of the edge</li>
     *<li>Set the pixels pool elevation to the lowest point of the edge</li>
     *<li>Call <b>vdn</b> to recalculate unresolved pixels
     * <li>-
     *<li>return to the begin</li>
     * </ol>
     * </li> </ol>
     * 
     * @throws Exception
     */
    private void setdf() throws Exception {

        float per = 1;
        // direction factor, where the components are 1/length
        double[] directionFactor = calculateDirectionFactor(xRes, yRes);

        dir = new int[nCols][nRows];
        apool = new int[nCols][nRows];

        /*
         * Initialise internal pointers, if the point is an invalid value then set the dir value to
         * -1 else to 0
         */
        pm.message(msg.message("pitfiller.initpointers"));
        for( int r = firstRow; r < lastRow; r++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int c = firstCol; c < lastCol; c++ ) {
                if (r == firstRow || r == lastRow - 1 || c == firstCol || c == lastCol - 1
                        || isNovalue(pitIter.getSampleDouble(c, r, 0))) {
                    dir[c][r] = -1;
                } else {
                    dir[c][r] = 0;
                }
            }
        }

        pm.message(msg.message("pitfiller.setpos"));

        /* Set positive slope directions - store unresolved on stack */
        currentPitsCount = 0;
        for( int r = (firstRow + 1); r < (lastRow - 1); r++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int c = (firstCol + 1); c < (lastCol - 1); c++ ) {
                double pitValue = pitIter.getSampleDouble(c, r, 0);
                if (!isNovalue(pitValue)) {
                    // set the value in the dir matrix (D8 matrix)
                    setDirection(pitValue, r, c, dir, directionFactor);
                }
                /*
                 * Put unresolved pixels (which have, in the dir matrix, 0 as value) on stack
                 * addstack method increased nis by one unit
                 */
                if (dir[c][r] == 0) {
                    addPitToStack(r, c);
                }
            }
        }

        /* routine to drain flats to neighbors */
        int stillPitsCount = resolveFlats(currentPitsCount);
        if (stillPitsCount == -1) {
            return;
        }

        // for( int i = 0; i < currentPitRows.length; i++ ) {
        // int r = currentPitRows[i];
        // int c = currentPitCols[i];
        // System.out.println("row/cols = " + r + "/" + c);
        // }

        int n = currentPitsCount;
        pm.message(msg.message("pitfiller.numpit") + n);
        int np1 = n;
        int nt = (int) (np1 * 1 - per / 100);

        /* initialize apool to zero */
        // for( int i = firstRow; i < lastRow; i++ ) {
        // if (pm.isCanceled()) {
        // return;
        // }
        // for( int j = firstCol; j < lastCol; j++ ) {
        // apool[j][i] = 0;
        // }
        // }

        pm.message(msg.message("pitfiller.main"));
        pm.message(msg.message("pitfiller.perc"));
        pm.message("0%");
        /* store unresolved stack location in apool for easy deletion */
        while( currentPitsCount > 0 ) {
            if (pm.isCanceled()) {
                return;
            }
            // set the index to the lowest point in the map, during the
            // iteration, which filled the elevation map, the lowest point will
            // changed
            int r = currentPitRows[stillPitsCount];
            int c = currentPitCols[stillPitsCount];
            pooln = 1;
            nf = 0;/* reset flag to that new min elev is found */
            // calculate recursively the pool
            int npool = pool(r, c, 0);

            /*
             * Find the pour point of the pool: the lowest point on the edge of the pool
             */
            for( int ip = 1; ip <= npool; ip++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                r = ipool[ip];
                c = jpool[ip];
                for( int k = 1; k <= 8; k++ ) {
                    int jn = c + DIR_WITHFLOW_EXITING_INVERTED[k][0];
                    int in = r + DIR_WITHFLOW_EXITING_INVERTED[k][1];
                    // if the point isn't in this pool but on the edge then
                    // check the minimun elevation edge
                    if (apool[jn][in] != pooln) {
                        et = max2(pitIter.getSampleDouble(c, r, 0), pitIter.getSampleDouble(jn, in, 0));
                        if (nf == 0) {
                            emin = et;
                            nf = 1;
                        } else {
                            if (emin > et) {
                                emin = et;
                            }
                        }
                    }
                }
            }

            /* Fill the pool */
            for( int k = 1; k <= npool; k++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                r = ipool[k];
                c = jpool[k];
                if (pitIter.getSampleDouble(c, r, 0) <= emin) {
                    if (dir[c][r] > 0) { /* Can be in pool, but not flat */
                        dir[c][r] = 0;
                        addPitToStack(r, c);
                    }

                    for( int ip = 1; ip <= 8; ip++ ) {
                        int jn = c + DIR_WITHFLOW_EXITING_INVERTED[ip][0];
                        int in = r + DIR_WITHFLOW_EXITING_INVERTED[ip][1];
                        if ((pitIter.getSampleDouble(jn, in, 0) > pitIter.getSampleDouble(c, r, 0)) && (dir[jn][in] > 0)) {
                            /*
                             * Only zero direction of neighbors that are higher - because lower or
                             * equal may be a pour point in a pit that must not be disrupted
                             */
                            dir[jn][in] = 0;
                            addPitToStack(in, jn);
                        }
                    }
                    pitIter.setSample(c, r, 0, emin);
                }
                apool[c][r] = 0;
            }

            /* reset unresolved stack */
            int ni = 0;
            for( int ip = 1; ip <= currentPitsCount; ip++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                setDirection(pitIter.getSampleDouble(currentPitCols[ip], currentPitRows[ip], 0), currentPitRows[ip],
                        currentPitCols[ip], dir, directionFactor);

                if (dir[currentPitCols[ip]][currentPitRows[ip]] == 0) {
                    ni++;
                    currentPitRows[ni] = currentPitRows[ip];
                    currentPitCols[ni] = currentPitCols[ip];
                }
            }

            n = currentPitsCount;

            stillPitsCount = resolveFlats(ni);
            if (stillPitsCount == -1) {
                return;
            }
            // System.out.println(nis);
            if (currentPitsCount < nt) {
                if (per % 10 == 0)
                    pm.message((int) per + "%");
                per = per + 1;
                nt = (int) (np1 * (1 - per / 100));
            }
        }
        pm.message("OmsPitfiller finished...");
    }

    /**
     * Adds a pit position to the stack.
     * 
     * @param row the row of the pit.
     * @param col the col of the pit.
     */
    private void addPitToStack( int row, int col ) {
        currentPitsCount = currentPitsCount + 1;
        if (currentPitsCount >= pitsStackSize) {
            pitsStackSize = (int) (pitsStackSize + nCols * nRows * .1) + 2;
            currentPitRows = realloc(currentPitRows, pitsStackSize);
            currentPitCols = realloc(currentPitCols, pitsStackSize);
            dn = realloc(dn, pitsStackSize);
        }

        currentPitRows[currentPitsCount] = row;
        currentPitCols[currentPitsCount] = col;
    }

    private int[] realloc( int[] arrayToExpand, int newSize ) {
        int[] resized = new int[newSize];
        System.arraycopy(arrayToExpand, 0, resized, 0, arrayToExpand.length);
        return resized;
    }

    /**
     * Try to find a drainage direction for undefinite cell.
     * 
     * <p> If the drainage direction is found
     * then puts it in the dir matrix else keeps its index in is and js. 
     * </p>
     * <p>N.B. in the {@link #setDirection(double, int, int, int[][], double[])} method the drainage
     * directions is set only if the slope between two pixel is positive.<b>At this step the dir
     * value is set also if the slope is equal to zero.</b></p>
     * 
     * @param pitsCount the number of indefinite cell in the dir matrix.
     * @return the number of unresolved pixel (still pits) after running the method or -1 if the process has been cancelled.
     */
    private int resolveFlats( int pitsCount ) {
        int stillPitsCount;
        currentPitsCount = pitsCount;

        do {
            if (pm.isCanceled()) {
                return -1;
            }
            pitsCount = currentPitsCount;
            currentPitsCount = 0;
            for( int ip = 1; ip <= pitsCount; ip++ ) {
                dn[ip] = 0;
            }

            for( int k = 1; k <= 8; k++ ) {
                for( int pitIndex = 1; pitIndex <= pitsCount; pitIndex++ ) {
                    double elevDelta = pitIter.getSampleDouble(currentPitCols[pitIndex], currentPitRows[pitIndex], 0)
                            - pitIter.getSampleDouble(currentPitCols[pitIndex] + DIR_WITHFLOW_EXITING_INVERTED[k][0],
                                    currentPitRows[pitIndex] + DIR_WITHFLOW_EXITING_INVERTED[k][1], 0);
                    if ((elevDelta >= 0.)
                            && ((dir[currentPitCols[pitIndex] + DIR_WITHFLOW_EXITING_INVERTED[k][0]][currentPitRows[pitIndex]
                                    + DIR_WITHFLOW_EXITING_INVERTED[k][1]] != 0) && (dn[pitIndex] == 0)))
                        dn[pitIndex] = k;
                }
            }
            stillPitsCount = 1; /* location of point on stack with lowest elevation */
            for( int pitIndex = 1; pitIndex <= pitsCount; pitIndex++ ) {
                if (dn[pitIndex] > 0) {
                    dir[currentPitCols[pitIndex]][currentPitRows[pitIndex]] = dn[pitIndex];
                } else {
                    currentPitsCount++;
                    currentPitRows[currentPitsCount] = currentPitRows[pitIndex];
                    currentPitCols[currentPitsCount] = currentPitCols[pitIndex];
                    if (pitIter.getSampleDouble(currentPitCols[currentPitsCount], currentPitRows[currentPitsCount], 0) < pitIter
                            .getSampleDouble(currentPitCols[stillPitsCount], currentPitRows[stillPitsCount], 0))
                        stillPitsCount = currentPitsCount;
                }
            }
            // out.println("vdn n = " + n + "nis = " + nis);
        } while( currentPitsCount < pitsCount );

        return stillPitsCount;
    }

    /**
     * function to compute pool recursively and at the same time determine the minimum elevation of
     * the edge.
     */
    private int pool( int row, int col, int prevNPool ) {
        int in;
        int jn;
        int npool = prevNPool;
        if (apool[col][row] <= 0) { /* not already part of a pool */
            if (dir[col][row] != -1) {/* check only dir since dir was initialized */
                /* not on boundary */
                apool[col][row] = pooln;/* apool assigned pool number */
                npool = npool + 1;// the number of pixel in the pool
                if (npool >= pstack) {
                    if (pstack < nCols * nRows) {
                        pstack = (int) (pstack + nCols * nRows * .1);
                        if (pstack > nCols * nRows) {
                            /* Pool stack too large */
                        }

                        ipool = realloc(ipool, pstack);
                        jpool = realloc(jpool, pstack);
                    }

                }

                ipool[npool] = row;
                jpool[npool] = col;

                for( int k = 1; k <= 8; k++ ) {
                    in = row + DIR_WITHFLOW_EXITING_INVERTED[k][1];
                    jn = col + DIR_WITHFLOW_EXITING_INVERTED[k][0];
                    /* test if neighbor drains towards cell excluding boundaries */
                    if (((dir[jn][in] > 0) && ((dir[jn][in] - k == 4) || (dir[jn][in] - k == -4))) || ((dir[jn][in] == 0)
                            && (pitIter.getSampleDouble(jn, in, 0) >= pitIter.getSampleDouble(col, row, 0)))) {
                        /* so that adjacent flats get included */
                        npool = pool(in, jn, npool);
                    }
                }

            }
        }
        return npool;
    }

    private double max2( double e1, double e2 ) {
        double em;
        em = e1;
        if (e2 > em)
            em = e2;
        return em;
    }

    /**
     * Calculate the drainage direction with the D8 method. 
     * 
     * <p>Find the direction that has the maximum
     * slope and set it as the drainage direction the in the cell (r,c) 
     * in the dir matrix. 
     * 
     * @param pitValue the value of pit in row/col. 
     * @param row row of the cell in the matrix.
     * @param col col of the cell in the matrix.
     * @param dir the drainage direction matrix to set the dircetion in. The cell contains an int value in the range 0 to 8
     *        (or 10 if it is an outlet point).
     * @param fact is the direction factor (1/lenght).
     */
    private void setDirection( double pitValue, int row, int col, int[][] dir, double[] fact ) {
        dir[col][row] = 0; /* This necessary to repeat passes after level raised */
        double smax = 0.0;

        // examine adjacent cells first
        for( int k = 1; k <= 8; k++ ) {
            int cn = col + DIR_WITHFLOW_EXITING_INVERTED[k][0];
            int rn = row + DIR_WITHFLOW_EXITING_INVERTED[k][1];
            double pitN = pitIter.getSampleDouble(cn, rn, 0);
            if (isNovalue(pitN)) {
                dir[col][row] = -1;
                break;
            }
            if (dir[col][row] != -1) {
                double slope = fact[k] * (pitValue - pitN);
                if (slope > smax) {
                    smax = slope;
                    // maximum slope gives the drainage direction
                    dir[col][row] = k;
                }
            }
        }

    }

    /**
     * Calculate the drainage direction factor.
     * 
     * @param dx is the resolution of the raster map in the x direction.
     * @param dy is the resolution of the raster map in the y direction.
     * @return <b>fact</b> the direction factor (or 1/length) where length is the distance of the
     *         pixel from the central pixel.
     */
    private double[] calculateDirectionFactor( double dx, double dy ) {
        // direction factor, where the components are 1/length
        double[] fact = new double[9];
        for( int k = 1; k <= 8; k++ ) {
            fact[k] = 1.0 / (Math.sqrt(DIR_WITHFLOW_EXITING_INVERTED[k][0] * dy * DIR_WITHFLOW_EXITING_INVERTED[k][0] * dy
                    + DIR_WITHFLOW_EXITING_INVERTED[k][1] * DIR_WITHFLOW_EXITING_INVERTED[k][1] * dx * dx));
        }
        return fact;
    }

}
