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
package org.hortonmachine.hmachine.modules.geomorphology.flow;

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_DOCUMENTATION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_inPit_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSFLOWDIRECTIONS_outFlow_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

@Description(OMSFLOWDIRECTIONS_DESCRIPTION)
@Documentation(OMSFLOWDIRECTIONS_DOCUMENTATION)
@Author(name = OMSFLOWDIRECTIONS_AUTHORNAMES, contact = OMSFLOWDIRECTIONS_AUTHORCONTACTS)
@Keywords(OMSFLOWDIRECTIONS_KEYWORDS)
@Label(OMSFLOWDIRECTIONS_LABEL)
@Name(OMSFLOWDIRECTIONS_NAME)
@Status(OMSFLOWDIRECTIONS_STATUS)
@License(OMSFLOWDIRECTIONS_LICENSE)
public class OmsFlowDirections extends HMModel {
    @Description(OMSFLOWDIRECTIONS_inPit_DESCRIPTION)
    @In
    public GridCoverage2D inPit = null;

    @Description(OMSFLOWDIRECTIONS_outFlow_DESCRIPTION)
    @Out
    public GridCoverage2D outFlow = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    /**
     * The novalue needed by OmsFlowDirections.
     */
    public static final double FLOWNOVALUE = -1.0;

    private RandomIter pitfillerIter;

    // the hydrologic variables
    /* define directions */
    private int[] d1 = new int[]{(int) FLOWNOVALUE, 0, -1, -1, -1, 0, 1, 1, 1};
    private int[] d2 = new int[]{(int) FLOWNOVALUE, 1, 1, 0, -1, -1, -1, 0, 1};

    private int i1, i2, n1, n2, nx, ny;

    private int[] is, js, dn;

    private int[][] dir;

    private int ccheck, useww;

    private int[][] arr;

    private double[][] areaw, weight;

    private final double ndv = FLOWNOVALUE;

    private double dx, dy;

    private double[][] elevations;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outFlow == null, doReset)) {
            return;
        }
        checkNull(inPit);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        nx = regionMap.get(CoverageUtilities.COLS).intValue();
        ny = regionMap.get(CoverageUtilities.ROWS).intValue();
        dx = regionMap.get(CoverageUtilities.XRES);
        dy = regionMap.get(CoverageUtilities.YRES);

        RenderedImage pitfillerRI = inPit.getRenderedImage();
        // input iterator
        pitfillerIter = RandomIterFactory.create(pitfillerRI, null);

        i1 = 0;
        i2 = 0;
        n1 = nx;
        n2 = ny;

        elevations = new double[nx][ny];

        for( int row = 0; row < ny; row++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int col = 0; col < nx; col++ ) {
                double pitValue = pitfillerIter.getSampleDouble(col, row, 0);
                if (!isNovalue(pitValue)) {
                    elevations[col][row] = pitValue;
                } else {
                    elevations[col][row] = FLOWNOVALUE;
                }
            }
        }

        setdfnoflood();

        // it is necessary to transpose the dir matrix and than it's possible to
        // write the output
        double[][] transposedFlow = new double[dir[0].length][dir.length];
        for( int i = 0; i < dir[0].length; i++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int j = 0; j < dir.length; j++ ) {
                if (dir[j][i] == 0) {
                    return;
                }
                if (dir[j][i] != FLOWNOVALUE) {
                    transposedFlow[i][j] = dir[j][i];
                } else {
                    transposedFlow[i][j] = doubleNovalue;
                }
            }
        }

        outFlow = CoverageUtilities.buildCoverage("flowdirections", transposedFlow, regionMap,
                inPit.getCoordinateReferenceSystem(), true);
    }

    /**
     * 
     */
    private void setdfnoflood() {
        int n;
        double[] fact = new double[9];

        dir = new int[nx][ny];

        pm.message(msg.message("flow.initbound"));

        /* Initialize boundaries */
        for( int i = i1; i < n1; i++ ) {
            dir[i][i2] = -1;
            dir[i][n2 - 1] = -1;
        }

        for( int i = i2; i < n2; i++ ) {
            dir[i1][i] = -1;
            dir[n1 - 1][i] = -1;
        }
        pm.message(msg.message("flow.initpointers"));
        /* initialize internal pointers */
        for( int col = (i2 + 1); col < (n2 - 1); col++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int row = (i1 + 1); row < (n1 - 1); row++ ) {

                if (doesntTouchNovalue(col, row)) {
                    dir[row][col] = 0;
                } else {
                    dir[row][col] = -1;
                }

                // if (elevations[row][col] <= FLOWNOVALUE) {
                // dir[row][col] = -1;
                // } else {
                // dir[row][col] = 0;
                // }
            }
        }

        /* Direction factors */
        for( int k = 1; k <= 8; k++ ) {
            fact[k] = 1.0 / (Math.sqrt(d1[k] * dy * d1[k] * dy + d2[k] * d2[k] * dx * dx));
        }

        // Compute contrib area using overlayed directions for direction setting
        ccheck = 0; // dont worry about edge contamination
        useww = 0; // dont worry about weights

        arr = new int[n2][n1];
        for( int i = 0; i < arr.length; i++ ) {
            for( int j = 0; j < arr[0].length; j++ ) {
                arr[i][j] = 0;
            }
        }

        for( int i = (i2 + 1); i < (n2 - 1); i++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int j = (i1 + 1); j < (n1 - 1); j++ ) {
                // This allows for a stream overlay
                if (dir[j][i] > 0)
                    darea(i, j);
            }
        }

        pm.message(msg.message("flow.setpos"));
        /* Set positive slope directions */
        n = 0;
        for( int i = (i2 + 1); i < (n2 - 1); i++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int j = (i1 + 1); j < (n1 - 1); j++ ) {
                if (dir[j][i] == 0) {
                    if (elevations[j][i] > FLOWNOVALUE) {
                        set(i, j, fact);
                        if (dir[j][i] == 0) {
                            n++;
                        }
                    }
                }
            }
        }

        pm.message(msg.message("flow.solveflats"));
        /*
         * Now resolve flats following the Procedure of Garbrecht and Martz, Journal of Hydrology,
         * 1997.
         */

        /*
         * Memory is utilized as follows is, js, dn, s and elev2 are unidimensional arrays storing
         * information for flats. sloc is a indirect addressing array for accessing these - used
         * during recursive iteration spos is a grid of pointers for accessing these to facilitate
         * finding neighbors The routine flatrout is recursive and at each recursion allocates a new
         * sloc for addressing these arrays and a new elev for keeping track of the elevations for
         * that recursion level.
         */
        if (n > 0) {
            int iter = 1;
            int[][] spos = new int[nx][ny];
            dn = new int[n];
            is = new int[n];
            js = new int[n];
            int[] s = new int[n];
            int[] sloc = new int[n];
            double[] elev2 = new double[n];

            /* Put unresolved pixels on stack */
            int ip = 0;
            for( int i = i2; i < n2; i++ ) {
                if (isCanceled(pm)) {
                    return;
                }
                for( int j = i1; j < n1; j++ ) {
                    spos[j][i] = -1; /* Initialize stack position */
                    if (dir[j][i] == 0) {
                        is[ip] = i;
                        js[ip] = j;
                        dn[ip] = 0;
                        sloc[ip] = ip;
                        /* Initialize the stage 1 array for flat routing */
                        s[ip] = 1;
                        spos[j][i] = ip; /* pointer for back tracking */
                        ip++;
                    }
                }
            }

            flatrout(n, sloc, s, spos, iter, elev2, elev2, fact, n);
            /* The direction 19 was used to flag pits. Set these to 0 */
            for( int i = i2; i < n2; i++ ) {
                if (isCanceled(pm)) {
                    return;
                }
                for( int j = i1; j < n1; j++ ) {
                    if (dir[j][i] == 19)
                        dir[j][i] = 0;
                }
            }
        }
    }

    private boolean doesntTouchNovalue( int col, int row ) {
        int rows = elevations.length;
        int cols = elevations[0].length;
        for( int i = -1; i <= 1; i++ ) {
            for( int j = -1; j <= 1; j++ ) {
                int r = row + i;
                int c = col + j;
                if (r >= 0 && r < rows && c >= 0 && c < cols) {
                    if (elevations[r][c] <= FLOWNOVALUE) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    private void flatrout( int n, int[] sloc, int[] s, int[][] spos, int iter, double[] elev1, double[] elev2, double[] fact,
            int ns ) {
        int nu, ipp;
        int[] sloc2;
        double[] elev3;

        incfall(n, elev1, s, spos, iter, sloc);
        for( int ip = 0; ip < n; ip++ ) {
            elev2[sloc[ip]] = (s[sloc[ip]]);
            s[sloc[ip]] = 0; /* Initialize for pass 2 */
        }

        incrise(n, elev1, s, spos, iter, sloc);
        for( int ip = 0; ip < n; ip++ ) {
            elev2[sloc[ip]] += (s[sloc[ip]]);
        }

        nu = 0;
        for( int ip = 0; ip < n; ip++ ) {
            set2(is[sloc[ip]], js[sloc[ip]], fact, elev1, elev2, iter, spos, s);
            if (dir[js[sloc[ip]]][is[sloc[ip]]] == 0)
                nu++;
        }

        if (nu > 0) {
            /* Iterate Recursively */
            /*
             * Now resolve flats following the Procedure of Garbrecht and Martz, Journal of
             * Hydrology, 1997.
             */
            iter = iter + 1;
            // printf("Resolving %d Flats, Iteration: %d \n",nu,iter);
            sloc2 = new int[nu];
            elev3 = new double[ns];

            /* Initialize elev3 */
            for( int ip = 0; ip < ns; ip++ ) {
                elev3[ip] = 0.;
            }
            /* Put unresolved pixels on new stacks - keeping in same positions */
            ipp = 0;
            for( int ip = 0; ip < n; ip++ ) {
                if (isCanceled(pm)) {
                    return;
                }
                if (dir[js[sloc[ip]]][is[sloc[ip]]] == 0) {
                    sloc2[ipp] = sloc[ip];
                    /* Initialize the stage 1 array for flat routing */
                    s[sloc[ip]] = 1;
                    ipp++;
                    // if(ipp > nu)printf("PROBLEM - Stack logic\n");
                } else {
                    s[sloc[ip]] = -1; /*
                                         * Used to designate out of remaining flat on higher
                                         * iterations
                                         */
                }
                dn[sloc[ip]] = 0; /* Reinitialize for next time round. */
            }
            flatrout(nu, sloc2, s, spos, iter, elev2, elev3, fact, ns);
        } /* end if nu > 0 */

    }

    /**
     * @param i
     * @param j
     * @param fact
     * @param elev1
     * @param elev2
     * @param iter
     * @param spos
     * @param s
     */
    private void set2( int i, int j, double[] fact, double[] elev1, double[] elev2, int iter, int[][] spos, int[] s ) {
        /*
         * This function sets directions based upon secondary elevations for assignment of flow
         * directions across flats according to Garbrecht and Martz scheme. There are two
         * possibilities: A. The neighbor is outside the flat set B. The neighbor is in the flat
         * set. In the case of A the elevation of the neighbor is set to 0 for the purposes of
         * computing slope. Since the incremental elevations are all positive there is always a
         * downwards slope to such neighbors, and if the previous elevation increment had 0 slope
         * then a flow direction can be assigned.
         */

        double slope, slope2, smax, ed;
        int spn, sp;
        int in, jn;
        smax = 0.;
        sp = spos[j][i];
        for( int k = 1; k <= 8; k++ ) {
            jn = j + d2[k];
            in = i + d1[k];
            spn = spos[jn][in];
            if (iter <= 1) {
                ed = elevations[j][i] - elevations[jn][in];
            } else {
                ed = elev1[sp] - elev1[spn];
            }
            slope = fact[k] * ed;
            if (spn < 0 || s[spn] < 0) {
                /* The neighbor is outside the flat set. */
                ed = 0.;
            } else {
                ed = elev2[spn];
            }
            slope2 = fact[k] * (elev2[sp] - ed);
            if (slope2 > smax && slope >= 0.) /*
                                                 * Only if latest iteration slope is positive and
                                                 * previous iteration slope flat
                                                 */
            {
                smax = slope2;
                dir[j][i] = k;
            }
        } /* End of for */

    }

    /**
     * @param n
     * @param elev1
     * @param s
     * @param spos
     * @param iter
     * @param sloc
     */
    private void incrise( int n, double[] elev1, int[] s2, int[][] spos, int iter, int[] sloc ) {
        /*
         * This routine implements stage 2 drainage away from higher ground dn is used to flag
         * pixels still being incremented
         */
        int done = 0, ninc, nincold, spn;
        double ed;
        int i, j, in, jn;
        nincold = 0;

        while( done < 1 ) {
            if (isCanceled(pm)) {
                return;
            }
            done = 1;
            ninc = 0;
            for( int ip = 0; ip < n; ip++ ) {
                if (isCanceled(pm)) {
                    return;
                }
                for( int k = 1; k <= 8; k++ ) {
                    j = js[sloc[ip]];
                    i = is[sloc[ip]];
                    jn = j + d2[k];
                    in = i + d1[k];
                    spn = spos[jn][in];

                    if (iter <= 1) {
                        ed = elevations[j][i] - elevations[jn][in];
                    } else {
                        ed = elev1[sloc[ip]] - elev1[spn];
                    }
                    if (ed < 0.) {
                        dn[sloc[ip]] = 1;
                    }
                    if (spn >= 0) {
                        if (s2[spn] > 0) {
                            dn[sloc[ip]] = 1;
                        }
                    }
                }
            }
            for( int ip = 0; ip < n; ip++ ) {
                if (isCanceled(pm)) {
                    return;
                }
                s2[sloc[ip]] = s2[sloc[ip]] + dn[sloc[ip]];
                ninc = ninc + dn[sloc[ip]];
                if (dn[sloc[ip]] == 0) {
                    done = 0; /*
                                 * if still some not being incremented continue looping
                                 */
                }

            }
            // printf("incrise %d %d\n",ninc,n);
            if (ninc == nincold) {
                done = 1;
            } /*
                 * If there are no new cells incremented stop - this is the case when a flat has no
                 * higher ground around it.
                 */
            nincold = ninc;
        }

    }

    /**
     * @param n
     * @param elev1
     * @param s
     * @param spos
     * @param iter
     * @param sloc
     */
    private void incfall( int n, double[] elev1, int[] s1, int[][] spos, int iter, int[] sloc ) {
        /* This routine implements drainage towards lower areas - stage 1 */
        int done = 0, donothing, ninc, nincold, spn;
        int st = 1, i, j, in, jn;
        double ed;
        nincold = -1;

        while( done < 1 ) {
            if (isCanceled(pm)) {
                return;
            }
            done = 1;
            ninc = 0;
            for( int ip = 0; ip < n; ip++ ) {
                /*
                 * if adjacent to same level or lower that drains or adjacent to pixel with s1 < st
                 * and dir not set do nothing
                 */
                donothing = 0;
                j = js[sloc[ip]];
                i = is[sloc[ip]];
                for( int k = 1; k <= 8; k++ ) {
                    jn = j + d2[k];
                    in = i + d1[k];
                    spn = spos[jn][in];
                    if (iter <= 1) {
                        ed = elevations[j][i] - elevations[jn][in];
                    } else {
                        ed = elev1[sloc[ip]] - elev1[spn];
                    }
                    if (ed >= 0. && dir[jn][in] != 0)
                        donothing = 1; /* If neighbor drains */
                    if (spn >= 0) /* if neighbor is in flat */
                    {
                        /* If neighbor is not being */
                        if (s1[spn] >= 0 && s1[spn] < st && dir[jn][in] == 0) {
                            donothing = 1; /* Incremented */
                        }
                    }
                }

                if (donothing == 0) {
                    s1[sloc[ip]]++;
                    ninc++;
                    done = 0;
                }
            } /* End of loop over all flats */
            st = st + 1;
            // printf("Incfall %d %d \n",ninc,n);
            if (ninc == nincold) {
                done = 1;
                // printf("There are pits remaining, direction will not be
                // set\n");
                /* Set the direction of these pits to 19 to flag them */
                for( int ip = 0; ip < n; ip++ ) /* loop 2 over all flats */
                {
                    /*
                     * if adjacent to same level or lower that drains or adjacent to pixel with s1 <
                     * st and dir not set do nothing
                     */
                    donothing = 0;
                    j = js[sloc[ip]];
                    i = is[sloc[ip]];
                    for( int k = 1; k <= 8; k++ ) {
                        jn = j + d2[k];
                        in = i + d1[k];
                        spn = spos[jn][in];
                        if (iter <= 1) {
                            ed = elevations[j][i] - elevations[jn][in];
                        } else {
                            ed = elev1[sloc[ip]] - elev1[spn];
                        }
                        if (ed >= 0. && dir[jn][in] != 0)
                            donothing = 1; /* If neighbor drains */
                        if (spn >= 0) /* if neighbor is in flat */
                        {
                            /* If neighbor is not being */
                            if (s1[spn] >= 0 && s1[spn] < st && dir[jn][in] == 0)
                                donothing = 1; /* Incremented */
                        }
                    }
                    if (donothing == 0) {
                        dir[j][i] = 19;
                        /* printf("%d %d\n",i,j); */
                    }
                } /* End of loop 2 over all flats */
            }
            nincold = ninc;
        } /* End of while done loop */

    }

    /**
     * @param i
     * @param j
     */
    private void darea( int i, int j ) {
        int in, jn, con = 0;
        /*
         * con is a flag that signifies possible contaminatin of area due to edge effects
         */
        if (i != 0 && i != ny - 1 && j != 0 && j != nx - 1 && dir[j][i] > -1)
        /* not on boundary */
        {
            if (arr[j][i] == 0) // not touched yet
            {
                arr[j][i] = 1;
                if (useww == 1)
                    areaw[j][i] = weight[j][i];
                for( int k = 1; k <= 8; k++ ) {
                    in = i + d1[k];
                    jn = j + d2[k];

                    /*
                     * test if neighbor drains towards cell excluding boundaryies
                     */
                    if (dir[jn][in] > 0 && (dir[jn][in] - k == 4 || dir[jn][in] - k == -4)) {
                        darea(in, jn);
                        if (arr[jn][in] < 0)
                            con = -1;
                        else
                            arr[j][i] = arr[j][i] + arr[jn][in];
                        if (useww == 1) {
                            if (areaw[jn][in] <= ndv || areaw[j][i] <= ndv) {
                                areaw[j][i] = ndv;
                            } else
                                areaw[j][i] = areaw[j][i] + areaw[jn][in];
                        }
                    }
                    if (dir[jn][in] < 0)
                        con = -1;
                }
                if (con == -1 && ccheck == 1) {
                    arr[j][i] = -1;
                    if (useww == 1)
                        areaw[j][i] = ndv;
                }
            }
        } else
            arr[j][i] = -1;
    }

    /**
     * @param i
     * @param j
     * @param fact
     */
    private void set( int i, int j, double[] fact ) {
        double slope, smax;
        int amax, in, jn, aneigh = -1;

        dir[j][i] = 0; /* This necessary for repeat passes after level raised */
        smax = 0.;
        amax = 0;

        for( int k = 1; k <= 8; k = k + 2 ) // examine adjacent cells first
        {
            in = i + d1[k];
            jn = j + d2[k];
            if (elevations[jn][in] <= FLOWNOVALUE) {
                continue;
            }

            if (dir[j][i] != -1) {
                slope = fact[k] * (elevations[j][i] - elevations[jn][in]);

                if (aneigh > amax && slope >= 0.) {
                    amax = aneigh;
                    if (Math.abs(dir[jn][in] - k) != 4)
                        dir[j][i] = k; // Dont set opposing pointers
                } else if (slope > smax && amax <= 0) {
                    smax = slope;
                    dir[j][i] = k;

                }
            }
        }

        for( int k = 2; k <= 8; k = k + 2 ) // examine diagonal cells
        {
            in = i + d1[k];
            jn = j + d2[k];
            /* if(elev[jn][in] <= mval) dir[j][i] = -1; */
            if (elevations[jn][in] <= FLOWNOVALUE) {
                continue;
            }
            if (dir[j][i] != -1) {
                slope = fact[k] * (elevations[j][i] - elevations[jn][in]);
                if (slope > smax && amax <= 0) // still need amax check to
                // prevent crossing
                {
                    smax = slope;
                    dir[j][i] = k;

                }
            }
        }

    }

}
