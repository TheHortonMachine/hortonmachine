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
package org.hortonmachine.gears.utils.math;

import static org.hortonmachine.gears.libs.modules.ModelsEngine.calculateNthMoment;
import static org.hortonmachine.gears.libs.modules.ModelsEngine.split2realvectors;
import static org.hortonmachine.gears.libs.modules.ModelsEngine.vectorizeDoubleMatrix;

import java.awt.image.RenderedImage;

import org.hortonmachine.gears.i18n.GearsMessageHandler;
import org.hortonmachine.gears.libs.modules.SplitVectors;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.sorting.QuickSortAlgorithm;

/**
 * <p>
 * The cb model (coupledfieldmoments). 
 * 
 * <p>It
 * calculates the histogram of a set of data contained in a matrix with respect
 * to the set of data contained in another matrix. In substance, a map of
 * R<SUP>2</SUP> &#8658; R<SUP>2</SUP>, in which each point of a bidimensional
 * system (identified by the values contained in a matrix) is mapped in a second
 * bidimensional system, is produced. The data of the first set are then grouped
 * in a prefixed number of intervals and the mean value of the independent
 * variable for each interval is calculated. To every interval corresponds a
 * certain set of values of the second set, of which the mean value is
 * calculated, and a designate number of moments which can be either centered,
 * if the functioning mode is &#8242;histogram&#8242;, or non-centered, if the
 * mode is &#8242;moments&#8242;. If the number of intervals assigned is lesser
 * than one, the data are subdivided in classes of data having the same
 * abscissa. <BR>
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG><BR>
 * </DT>
 * <OL>
 * <LI>the file containing the data of the independent variable;</LI>
 * <LI>the file containing the data which will be used as dependent variable;</LI>
 * <LI>the first moment to calculate;</LI>
 * <LI>the last moment to calculate;</LI>
 * <LI>the insertion of an optional comment is also requested;</LI>
 * </OL>
 * <P>
 * </DD>
 * <DT><STRONG>Returns:</STRONG><BR>
 * </DT>
 * <DD>
 * <OL>
 * <LI>file containing: 1) the number of elements in each interval; 2) the mean
 * value of the data in abscissa; 3) the mean value of the data in ordinate;
 * n+2) the n-esimal moment of the data in ordinate.</LI>
 * </OL>
 * <P></DD>
 * </p>
 * <p>
 * Note: The program uses the memory intensely. Therefore if we decide to have
 * so many intervals as the data in abscissa, the program could not function
 * correctly. Moreover the program assumes that the real data are preceded by
 * two arrays, like in the files derived from a DEM.
 * </p>
 */
public class CoupledFieldsMoments {
    public double[][] process( RenderedImage map1RI, RenderedImage map2RI, int pBins, int pFirst, int pLast,
            IHMProgressMonitor pm, int binmode ) {
        if (map2RI == null) {
            map2RI = map1RI;
        }
        GearsMessageHandler msg = GearsMessageHandler.getInstance();

        pm.message(msg.message("cb.vectorize"));
        double[] U = vectorizeDoubleMatrix(map1RI);
        double[] T = null;
        QuickSortAlgorithm t = new QuickSortAlgorithm(pm);
        if (map2RI == null) {
            T = U;
            t.sort(U, (double[]) null);
        } else {
            T = vectorizeDoubleMatrix(map2RI);
            t.sort(U, T);
        }

        SplitVectors theSplit = new SplitVectors();
        int num_max = 1000;
        /*
         * if (bintype == 1) {
         */
        pm.message(msg.message("cb.splitvector"));
        split2realvectors(U, T, theSplit, pBins, num_max, pm);
        /*
         * } else { delta = FluidUtils.exponentialsplit2realvectors(U, T,
         * theSplit, N, num_max, base); }
         */

        pm.message(msg.message("cb.creatematrix"));
        double[][] outCb = new double[theSplit.splitIndex.length][pLast - pFirst + 3];
        binmode = 1; // kept for future expansion
        if (binmode == 1) // always true for now, other modes not implemented yet
        {
            for( int h = 0; h < theSplit.splitIndex.length; h++ ) {
                outCb[h][0] = calculateNthMoment(theSplit.splitValues1[h], (int) theSplit.splitIndex[h], 0.0, 1.0, pm);
                outCb[h][1] = theSplit.splitIndex[h];
                outCb[h][2] = calculateNthMoment(theSplit.splitValues2[h], (int) theSplit.splitIndex[h], 0.0, 1.0, pm);
                if (pFirst == 1)
                    pFirst++;
                for( int k = pFirst; k <= pLast; k++ ) {
                    outCb[h][k - pFirst + 3] = calculateNthMoment(theSplit.splitValues2[h], (int) theSplit.splitIndex[h],
                            outCb[h][1], (double) k, pm);
                }
            }
        }
        // else if (binmode == 2) // why is this exactly the same as the mode
        // // 'H' ???
        // {
        // for( int h = 0; h < theSplit.splittedindex.length; h++ ) {
        // moments[h][0] =
        // FluidUtils.double_n_moment(theSplit.splittedvalues1[h],
        // (int) theSplit.splittedindex[h], 0.0, 1.0);
        // moments[h][1] =
        // FluidUtils.double_n_moment(theSplit.splittedvalues2[h],
        // (int) theSplit.splittedindex[h], 0.0, 1.0);
        //
        // if (firstmoment == 1)
        // firstmoment++;
        // for( int k = firstmoment; k <= secondmoment; k++ ) {
        // moments[h][k - firstmoment + 2] = FluidUtils.double_n_moment(
        // theSplit.splittedvalues2[h], (int) theSplit.splittedindex[h],
        // moments[h][k - firstmoment + 1], (double) k);
        // }
        // }
        // }

        return outCb;
    }
}
