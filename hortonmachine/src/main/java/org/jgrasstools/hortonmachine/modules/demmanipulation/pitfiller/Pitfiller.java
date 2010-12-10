/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.hortonmachine.modules.demmanipulation.pitfiller;

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;
@Description("Straight port of the pitfiller correction model found in the TARDEM suite.")
@Author(name = "David Tarboton, Andrea Antonello", contact = "http://www.neng.usu.edu/cee/faculty/dtarb/tardem.html#programs, www.hydrologis.com")
@Keywords("Dem manipulation, Geomorphology")
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Pitfiller extends JGTModel {
    @Description("The digital elevation model (DEM) on which to perform pitfilling.")
    @In
    public GridCoverage2D inDem;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The depitted digital elevation model.")
    @Out
    public GridCoverage2D outPit = null;

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
    private int i1;
    private int i2;
    private int n1;
    private int n2;
    /**
     * The number of unresolved pixel in dir matrix (which haven't a drainage direction).
     */
    private int nis;
    /**
     * Dimension of the temporary vectors which allow to resolve the undrainage pixel.
     */
    private int istack;
    /**
     * Dimension of the temporary vectors which allow to resolve the undrainage pixel.
     */
    private int pstack;
    private int nf;
    private int pooln;
    private int npool;
    /**
     * Used to memorise the index of the "pixel pool".
     */
    private int[] ipool;
    /**
     * Used to memorise the index of the "pixel pool".
     */
    private int[] jpool;
    /**
     * Vector where the program memorizes the index of the elevation matrix whose point doesn't drain
     * in any D8 cells.
     */
    private int[] is;
    /**
     * Vector where the program memorizes the index of the elevation matrix whose point doesn't drain
     * in any D8 cells.
     */
    private int[] js;
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

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inDem);
        nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        elevationIter = CoverageUtilities.getRandomIterator(inDem);

        // output raster
        WritableRaster pitRaster = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, null);
        pitIter = CoverageUtilities.getWritableRandomIterator(pitRaster);

        for( int i = 0; i < nRows; i++ ) {
            if (isCanceled(pm)) {
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
        if (isCanceled(pm)) {
            return;
        }

        for( int i = 0; i < nRows; i++ ) {
            if (isCanceled(pm)) {
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

        outPit = CoverageUtilities.buildCoverage("pitfiller", pitRaster, regionMap, inDem.getCoordinateReferenceSystem());
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
        istack = (int) (nCols * nRows * 0.1);
        pstack = istack;
        dn = new int[istack];
        is = new int[istack];
        js = new int[istack];
        ipool = new int[pstack];
        jpool = new int[pstack];
        i1 = 0;
        i2 = 0;
        n1 = nCols;
        n2 = nRows;

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
     * @param d1 the vector which contains all the possible first components drainage direction.
     * @param d2 the vector which contains all the possible second components drainage direction.
     * @throws Exception
     */
    private void setdf() throws Exception {

        int nflat;
        int ni;
        int n;
        int ip;
        int imin;
        int jn;
        int in;
        int np1;
        int nt;
        float per = 1;
        // direction factor, where the components are 1/length
        double[] fact = calculateDirectionFactor(xRes, yRes);

        dir = new int[nCols][nRows];
        apool = new int[nCols][nRows];

        pm.message(msg.message("pitfiller.initbound"));

        /* Initialize boundaries */
        for( int i = i1; i < n1; i++ ) {
            dir[i][i2] = -1;
            dir[i][n2 - 1] = -1;
        }
        for( int i = i2; i < n2; i++ ) {
            dir[i1][i] = -1;
            dir[n1 - 1][i] = -1;
        }
        pm.message(msg.message("pitfiller.initpointers"));

        /*
         * Initialise internal pointers, if the point is an invalid value then set the dir value to
         * -1 else to 0
         */
        for( int i = (i2 + 1); i < (n2 - 1); i++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int j = (i1 + 1); j < (n1 - 1); j++ ) {
                if (isNovalue(pitIter.getSampleDouble(j, i, 0))) {
                    dir[j][i] = -1;
                } else {
                    dir[j][i] = 0;
                }
            }
        }

        pm.message(msg.message("pitfiller.setpos"));

        /* Set positive slope directions - store unresolved on stack */
        nis = 0;
        for( int i = (i2 + 1); i < (n2 - 1); i++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int j = (i1 + 1); j < (n1 - 1); j++ ) {
                if (!isNovalue(pitIter.getSampleDouble(j, i, 0))) {
                    // set the value in the dir matrix (D8 matrix)
                    set(i, j, dir, fact);
                }
                /*
                 * Put unresolved pixels (which have, in the dir matrix, 0 as value) on stack
                 * addstack method increased nis by one unit
                 */
                if (dir[j][i] == 0) {
                    addstack(i, j);
                }
            }
        }

        nflat = nis;
        /* routine to drain flats to neighbors */
        imin = vdn(nflat);
        n = nis;
        pm.message(msg.message("pitfiller.numpit") + n);
        np1 = n;
        nt = (int) (np1 * 1 - per / 100);

        /* initialize apool to zero */
        for( int i = i2; i < n2; i++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int j = i1; j < n1; j++ ) {
                apool[j][i] = 0;
            }
        }

        pm.message(msg.message("pitfiller.main"));
        pm.message(msg.message("pitfiller.perc"));
        pm.message("0%");
        /* store unresolved stack location in apool for easy deletion */
        int i = 0, j = 0;
        while( nis > 0 ) {
            if (isCanceled(pm)) {
                return;
            }
            // set the index to the lowest point in the map, during the
            // iteration, which filled the elevation map, the lowest point will
            // changed
            i = is[imin];
            j = js[imin];
            pooln = 1;
            npool = 0;
            nf = 0;/* reset flag to that new min elev is found */
            // calculate recursively the pool
            pool(i, j); /*
                         * Recursive call on unresolved point with lowest elevation
                         */

            /*
             * Find the pour point of the pool: the lowest point on the edge of the pool
             */
            for( ip = 1; ip <= npool; ip++ ) {
                if (isCanceled(pm)) {
                    return;
                }
                i = ipool[ip];
                j = jpool[ip];
                for( int k = 1; k <= 8; k++ ) {
                    jn = j + DIR_WITHFLOW_EXITING_INVERTED[k][0];
                    in = i + DIR_WITHFLOW_EXITING_INVERTED[k][1];
                    // if the point isn't in this pool but on the edge then
                    // check the minimun elevation edge
                    if (apool[jn][in] != pooln) {
                        et = max2(pitIter.getSampleDouble(j, i, 0), pitIter.getSampleDouble(jn, in, 0));
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
                if (isCanceled(pm)) {
                    return;
                }
                i = ipool[k];
                j = jpool[k];
                if (pitIter.getSampleDouble(j, i, 0) <= emin) {
                    if (dir[j][i] > 0) { /* Can be in pool, but not flat */
                        dir[j][i] = 0;
                        addstack(i, j);
                    }

                    for( ip = 1; ip <= 8; ip++ ) {
                        jn = j + DIR_WITHFLOW_EXITING_INVERTED[ip][0];
                        in = i + DIR_WITHFLOW_EXITING_INVERTED[ip][1];
                        if ((pitIter.getSampleDouble(jn, in, 0) > pitIter.getSampleDouble(j, i, 0)) && (dir[jn][in] > 0)) {
                            /*
                             * Only zero direction of neighbors that are higher - because lower or
                             * equal may be a pour point in a pit that must not be disrupted
                             */
                            dir[jn][in] = 0;
                            addstack(in, jn);
                        }
                    }
                    pitIter.setSample(j, i, 0, emin);
                }
                apool[j][i] = 0;
            }

            /* reset unresolved stack */
            ni = 0;
            for( ip = 1; ip <= nis; ip++ ) {
                if (isCanceled(pm)) {
                    return;
                }
                set(is[ip], js[ip], dir, fact);

                if (dir[js[ip]][is[ip]] == 0) {
                    ni++;
                    is[ni] = is[ip];
                    js[ni] = js[ip];
                }
            }

            n = nis;

            imin = vdn(ni);
            // System.out.println(nis);
            if (nis < nt) {
                if (per % 10 == 0)
                    pm.message((int) per + "%");
                per = per + 1;
                nt = (int) (np1 * (1 - per / 100));
            }
        }
        pm.message("Pitfiller finished...");
    }

    /**
     * Routine to add entry to is, js stack, enlarging if necessary
     * @param i
     * @param j
     */
    private void addstack( int i, int j ) {
        /* Routine to add entry to is, js stack, enlarging if necessary */
        nis = nis + 1;
        if (nis >= istack) {
            /* Try enlarging */
            istack = (int) (istack + nCols * nRows * .1) + 2;

            is = realloc(is, istack);
            js = realloc(js, istack);
            dn = realloc(dn, istack);

        }

        is[nis] = i;
        js[nis] = j;
        // out.println(" i = " + i + "nis = " + nis);
    }

    private int[] realloc( int[] is2, int istack2 ) {

        int[] resized = new int[istack2];
        for( int i = 0; i < is2.length; i++ ) {
            resized[i] = is2[i];
        }

        return resized;
    }

    /**
     * Try to find a drainage direction for undefinite cell.
     * 
     * <p> If the drainage direction is found
     * then put it in dir else kept its index in is and js. N.B. in the set method the drainage
     * directions is set only if the slope between two pixel is positive. At this step the dir
     * value is set also the slope is equal to zero.</p>
     * 
     * @param n the number of indefinite cell in the dir matrix
     * @return imin or the number of unresolved pixel after have run the method
     */
    private int vdn( int n ) {
        int imin;
        double ed;
        nis = n;

        do {
            if (isCanceled(pm)) {
                return -1;
            }
            n = nis;
            nis = 0;
            for( int ip = 1; ip <= n; ip++ ) {
                dn[ip] = 0;
            }

            for( int k = 1; k <= 8; k++ ) {
                for( int ip = 1; ip <= n; ip++ ) {

                    ed = pitIter.getSampleDouble(js[ip], is[ip], 0)
                            - pitIter.getSampleDouble(js[ip] + DIR_WITHFLOW_EXITING_INVERTED[k][0], is[ip]
                                    + DIR_WITHFLOW_EXITING_INVERTED[k][1], 0);
                    if ((ed >= 0.)
                            && ((dir[js[ip] + DIR_WITHFLOW_EXITING_INVERTED[k][0]][is[ip] + DIR_WITHFLOW_EXITING_INVERTED[k][1]] != 0) && (dn[ip] == 0)))
                        dn[ip] = k;

                }
            }
            imin = 1; /* location of point on stack with lowest elevation */
            for( int ip = 1; ip <= n; ip++ ) {
                if (dn[ip] > 0) {
                    dir[js[ip]][is[ip]] = dn[ip];
                } else {
                    nis++;
                    is[nis] = is[ip];
                    js[nis] = js[ip];
                    if (pitIter.getSampleDouble(js[nis], is[nis], 0) < pitIter.getSampleDouble(js[imin], is[imin], 0))
                        imin = nis;
                }
            }
            // out.println("vdn n = " + n + "nis = " + nis);
        } while( nis < n );

        return imin;
    }

    /**
     * function to compute pool recursively and at the same time determine the minimum elevation of
     * the edge.
     */
    private void pool( int i, int j ) {
        int in;
        int jn;
        if (apool[j][i] <= 0) { /* not already part of a pool */
            if (dir[j][i] != -1) {/* check only dir since dir was initialized */
                /* not on boundary */
                apool[j][i] = pooln;/* apool assigned pool number */
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

                ipool[npool] = i;
                jpool[npool] = j;

                for( int k = 1; k <= 8; k++ ) {
                    in = i + DIR_WITHFLOW_EXITING_INVERTED[k][1];
                    jn = j + DIR_WITHFLOW_EXITING_INVERTED[k][0];
                    /* test if neighbor drains towards cell excluding boundaries */
                    if (((dir[jn][in] > 0) && ((dir[jn][in] - k == 4) || (dir[jn][in] - k == -4)))
                            || ((dir[jn][in] == 0) && (pitIter.getSampleDouble(jn, in, 0) >= pitIter.getSampleDouble(j, i, 0)))) {
                        /* so that adjacent flats get included */
                        pool(in, jn);
                    }
                }

            }
        }

    }

    private double max2( double e1, double e2 ) {
        double em;
        em = e1;
        if (e2 > em)
            em = e2;
        return em;
    }

    /**
     * Calculate the drainage direction with D8 method. Find the direction which have the maximum
     * slope and set it as the drainage directionthe in the cell (i,j) in dir matrix. Is used in
     * some horton like pitfiller, floe,...
     * 
     * @param i <b>j</b> are the position index of the cell in the matrix.
     * @param dir is the drainage direction matrix, a cell contains an int value in the range 0 to 8
     *        (or 10 if it is an outlet point).
     *@param elevation is the DEM.
     *@param fact is the direction factor (1/lenght).
     */
    private void set( int i, int j, int[][] dir, double[] fact ) {
        double slope = 0;
        double smax;
        int in;
        int jn;
        dir[j][i] = 0; /* This necessary for repeat passes after level raised */
        smax = 0.0;

        for( int k = 1; k <= 8; k++ ) // examine adjacent cells first
        {
            jn = j + DIR_WITHFLOW_EXITING_INVERTED[k][0];
            in = i + DIR_WITHFLOW_EXITING_INVERTED[k][1];
            if (isNovalue(pitIter.getSampleDouble(jn, in, 0))) {
                dir[j][i] = -1;
                break;
            }

            if (dir[j][i] != -1) {
                slope = fact[k] * (pitIter.getSampleDouble(j, i, 0) - pitIter.getSampleDouble(jn, in, 0));

                if (slope > smax) {
                    smax = slope;
                    dir[j][i] = k;
                }
            }
        }

    }

    /**
     * Calculate the drainage direction factor (is used in some horton machine like pitfiller,
     * flow,...)
     * 
     * @param dx is the resolution of a raster map in the x direction.
     * @param dy is the resolution of the raster map in the y direction.
     * @return <b>fact</b> the direction factor or 1/lenght where lenght is the distance of the
     *         pixel from the central poxel.
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
