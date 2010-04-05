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
package eu.hydrologis.jgrass.hortonmachine.modules.statistics.cb;

import java.awt.image.RenderedImage;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;

import eu.hydrologis.jgrass.hortonmachine.i18n.MessageHandler;
import eu.hydrologis.jgrass.hortonmachine.libs.models.HMModel;
import eu.hydrologis.jgrass.hortonmachine.libs.models.ModelsEngine;
import eu.hydrologis.jgrass.hortonmachine.libs.monitor.DummyProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.libs.monitor.IHMProgressMonitor;
import eu.hydrologis.jgrass.hortonmachine.utils.sorting.QuickSortAlgorithm;

/**
 * <p>
 * The openmi compliant representation of the cb model (coupledfieldmoments). It
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
 * Usage: h.cb --igrass-map1 map1 --igrass-map2 map2 --otable-file1
 * nvalues#meanx#meany#mom_...#/file_path/#file1
 * " --otable-file2 meanx#tbins#/file_path/#file2" --firstmoment value
 * --lastmoment value --numbins value --binmode value
 * </p>
 * <p>
 * Note: The program uses the memory intensely. Therefore if we decide to have
 * so many intervals as the data in abscissa, the program could not function
 * correctly. Moreover the program assumes that the real data are preceded by
 * two arrays, like in the files derived from a DEM.
 * </p>
 */
@Description("Calculates the histogram of a set of data contained in a matrix " +
             "with respect to the set of data contained in another matrix")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("Histogram, Geomorphology, Statistic")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Cb extends HMModel {
    @Description("The first coverage to analyse.")
    @In
    public GridCoverage2D inMap1 = null;

    @Description("The second coverage to analyse.")
    @In
    public GridCoverage2D inMap2 = null;

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new DummyProgressMonitor();

    @Role(Role.PARAMETER)
    @Description("The number of bins into which divide the data range.")
    @In
    public int pBins;

    @Role(Role.PARAMETER)
    @Description("The first moment to consider.")
    @In
    public int pFirst;

    @Role(Role.PARAMETER)
    @Description("The last moment to consider.")
    @In
    public int pLast;

    @Description("A matrix containing " +
                 "1) the mean value of the data in abscissa; " +
                 "2) the number of elements in each interval; " +
                 "3) the mean value of the data in ordinate; " +
                 "n+2) the n-esimal moment of the data in ordinate.")
    @Out
    public double[][] outCb;

    private MessageHandler msg = MessageHandler.getInstance();

    private int binmode = 1;

    // private int bintype;
    // private float base;

    private SplitVectors theSplit;

    private ModelsEngine modelsEngine = new ModelsEngine();

    @Execute
    public void process() throws Exception {
        if (!concatOr(outCb == null, doReset)) {
            return;
        }
        
        RenderedImage map1RI = inMap1.getRenderedImage();
        RenderedImage map2RI = null;
        if (inMap2 == null) {
            map2RI = map1RI;
        }else{
            map2RI = inMap2.getRenderedImage();
        }

        pm.message(msg.message("cb.vectorize"));
        double[] U = modelsEngine.vectorizeDoubleMatrix(map1RI);
        double[] T = null;
        QuickSortAlgorithm t = new QuickSortAlgorithm(pm);
        if (inMap2 == null) {
            T = U;
            t.sort(U, null);
        }else{
            T = modelsEngine.vectorizeDoubleMatrix(map2RI);
            t.sort(U, T);
        }

        theSplit = new SplitVectors();

        int num_max = 1000;
        /*
         * if (bintype == 1) {
         */
        pm.message(msg.message("cb.splitvector"));
        modelsEngine.split2realvectors(U, T, theSplit, pBins, num_max, pm);
        /*
         * } else { delta = FluidUtils.exponentialsplit2realvectors(U, T,
         * theSplit, N, num_max, base); }
         */

        pm.message(msg.message("cb.creatematrix"));
        outCb = new double[theSplit.splitIndex.length][pLast - pFirst + 3];
        if (binmode == 1) // always true for now, other modes not implemented yet
        {
            for( int h = 0; h < theSplit.splitIndex.length; h++ ) {
                outCb[h][0] = modelsEngine.doubleNMoment(theSplit.splitValues1[h], (int) theSplit.splitIndex[h], 0.0, 1.0, pm);
                outCb[h][1] = theSplit.splitIndex[h];
                outCb[h][2] = modelsEngine.doubleNMoment(theSplit.splitValues2[h], (int) theSplit.splitIndex[h], 0.0, 1.0, pm);
                if (pFirst == 1)
                    pFirst++;
                for( int k = pFirst; k <= pLast; k++ ) {
                    outCb[h][k - pFirst + 3] = modelsEngine.doubleNMoment(theSplit.splitValues2[h], (int) theSplit.splitIndex[h], outCb[h][1], (double) k, pm);
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
    }

}
