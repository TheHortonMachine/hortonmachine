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
package org.jgrasstools.hortonmachine.modules.geomorphology.draindir;

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
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.i18n.MessageHandler;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.modules.ModelsSupporter;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.sorting.QuickSortAlgorithm;
/**
 * <p>
 * The openmi compliant representation of the draindir model. It calculates the
 * drainage directions minimizing the deviation from the real flow. The
 * deviation is calculated using a triangular construction and it could be given
 * in degrees (D8 LAD method) or as trasversal distance (D8 LTD method). The
 * deviation could be cumulated along the path using the &#955; parameter, and
 * when it assumes a limit value the flux is redirect to the real direction. In
 * certain cases, for example in the plains areas or where there are manmade
 * constructions, it can happen that the extracted channel network does not
 * coincide with the real channel network. The fixed network method allows you
 * to assign a known channel network and to then correct the drainage
 * directions.
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG><BR>
 * </DT>
 * <DD>
 * <OL>
 * <LI>the depitted map (-pit)</LI>
 * <LI>the old drainage direction map (-flow)</LI>
 * <LI>the &#955; parameter (a value in the range 0 - 1) (-lambda)</LI>
 * <LI>the method choosen: LAD (angular deviation) and LTD (trasversal
 * distance)(-mode)</LI>
 * </OL>
 * <P></DD>
 * <DT><STRONG>Returns:</STRONG><BR>
 * </DT>
 * <DD>
 * <OL>
 * <LI>the map with the new drainage directions (-dir)</LI>
 * <LI>the map with the total contributing areas calculated with this drainage
 * directions (-tca)</LI>
 * </OL>
 * <P></DD> Usage method LAD: h.draindir --mode 1 --igrass-pit pit --igrass-flow
 * flow --lambda lambda --ograss-dir dir --ograss-tca tca
 * </p>
 * <p>
 * Usage method LTD: h.draindir --mode 2 --igrass-pit pit --igrass-flow flow
 * --lambda lambda --ograss-dir dir --ograss-tca tca
 * </p>
 * <p>
 * Usage method FLOW FIXED: h.draindir --mode 1-2 --flowfixed 1 --igrass-pit pit
 * --igrass-flow flow --igrass-flowfixed flowfixed --lambda lambda --ograss-dir
 * dir
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Antonello Andrea, Cozzini
 *         Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo
 */
@Description("It calculates the drainage directions minimizing the deviation from the real flow")
@Author(name = "Andrea Antonello, Franceschi Silvia, Erica Ghesla, Rigon Riccardo, Pisoni Silvano", contact = "www.hydrologis.com")
@Keywords("Geomorphology")
@Status(Status.TESTED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class DrainDir extends JGTModel {

    /*
     * EXTERNAL VARIABLES
     */
    @Description("The depitted elevation model.")
    @In
    public GridCoverage2D inPit = null;

    @Description("The map of flowdirections.")
    @In
    public GridCoverage2D inFlow = null;

    @Description("The flow map on the network pixels.")
    @In
    public GridCoverage2D inFlownet = null;

    @Role(Role.PARAMETER)
    @Description("The direction correction factor.")
    @In
    public double pLambda = 1.0;

    @Role(Role.PARAMETER)
    @Description("Switch for the mode to use: true = LAD (default), false = LTD)).")
    @In
    public boolean doLad = true;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The map of drainage directions.")
    @Out
    public GridCoverage2D outFlow = null;

    @Description("The map of total contributing areas.")
    @Out
    public GridCoverage2D outTca = null;

    /*
     * INTERNAL VARIABLES
     */
    private MessageHandler msg = MessageHandler.getInstance();

    private static final double PI = Math.PI;

    private static final double NaN = doubleNovalue;

    /*
     * indicates the position of the triangle's vertexes
     */
    private int[][] order = ModelsSupporter.DIR;

    private int cols;
    private int rows;
    private double xRes;
    private double yRes;

    /**
     * Calculates new drainage directions
     * 
     * @throws Exception
     */
    @Execute
    public void process() throws Exception {
        if (!concatOr(outFlow == null, doReset)) {
            return;
        }

        double[] orderedelev, indexes;
        int nelev;

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        cols = regionMap.get(CoverageUtilities.COLS).intValue();
        rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage pitfillerRI = inPit.getRenderedImage();
        WritableRaster pitfillerWR = CoverageUtilities.renderedImage2WritableRaster(pitfillerRI, true);
        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2WritableRaster(flowRI, true);

        RandomIter pitRandomIter = RandomIterFactory.create(pitfillerWR, null);

        // create new matrix
        orderedelev = new double[cols * rows];
        indexes = new double[cols * rows];

        nelev = 0;
        for( int j = 0; j < rows; j++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int i = 0; i < cols; i++ ) {
                orderedelev[((j) * cols) + i] = pitRandomIter.getSampleDouble(i, j, 0);
                indexes[((j) * cols) + i] = ((j) * cols) + i + 1;
                if (!isNovalue(pitRandomIter.getSampleDouble(i, j, 0))) {
                    nelev = nelev + 1;
                }
            }
        }

        QuickSortAlgorithm t = new QuickSortAlgorithm(pm);
        t.sort(orderedelev, indexes);

        pm.message(msg.message("draindir.initializematrix"));

        // Initialize new RasterData and set value
        WritableRaster tcaWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, NaN);
        WritableRaster dirWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, NaN);

        // it contains the analyzed cells
        WritableRaster analyzeWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);
        WritableRaster deviationsWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null, null, null);

        if (doLad) {
            OrlandiniD8_LAD(indexes, deviationsWR, analyzeWR, pitfillerWR, flowWR, tcaWR, dirWR, nelev);
        } else {
            OrlandiniD8_LTD(indexes, deviationsWR, analyzeWR, pitfillerWR, flowWR, tcaWR, dirWR, nelev);
            // only if required executes this method
            if (inFlownet != null) {
                newDirections(pitfillerWR, dirWR);
            }
        }
        if (isCanceled(pm)) {
            return;
        }
        outFlow = CoverageUtilities.buildCoverage("draindir", dirWR, regionMap, inPit.getCoordinateReferenceSystem());
        outTca = CoverageUtilities.buildCoverage("tca", tcaWR, regionMap, inPit.getCoordinateReferenceSystem());
    }

    /**
     * routine that defines the draining directions
     * 
     * @param indexes
     *            vector containing the order of elevation
     * @param deviationsImage
     *            the map containing the deviation
     * @param analyzeImage
     * @param nelev
     * @return
     */
    private void OrlandiniD8_LAD( double[] indexes, WritableRaster deviationsImage, WritableRaster analyzeImage,
            WritableRaster pitImage, WritableRaster flowImage, WritableRaster tcaImage, WritableRaster dirImage, int nelev ) {
        int row, col, ncelle, nr, nc;
        double dev1, dev2, sumdev1, sumdev2, sumdev;
        double[] dati = new double[10]; /*
                                         * it contains:
                                         * pend,dir,e0,e1,e2,sumdev,
                                         * didren1,dirdren2,sigma
                                         */
        double count, flow;

        double[] u = {xRes, yRes};
        double[] v = {NaN, NaN};
        // get rows and cols from the active region

        ncelle = 0;
        RandomIter pitRandomIter = RandomIterFactory.create(pitImage, null);
        RandomIter flowRandomIter = RandomIterFactory.create(flowImage, null);

        WritableRandomIter tcaRandomIter = RandomIterFactory.createWritable(tcaImage, null);
        WritableRandomIter analyseRandomIter = RandomIterFactory.createWritable(analyzeImage, null);
        WritableRandomIter deviationRandomIter = RandomIterFactory.createWritable(deviationsImage, null);
        WritableRandomIter dirRandomIter = RandomIterFactory.createWritable(dirImage, null);

        pm.beginTask(msg.message("draindir.orlandinilad"), rows * cols);
        for( int i = rows * cols - 1; i >= 0; i-- ) {
            if (isCanceled(pm)) {
                return;
            }
            count = indexes[i];
            col = (int) count % cols - 1;
            row = (int) count / cols;
            if (!isNovalue(pitRandomIter.getSampleDouble(col, row, 0)) && !isNovalue(flowRandomIter.getSampleDouble(col, row, 0))) {
                ncelle = ncelle + 1;
                compose(analyseRandomIter, pitRandomIter, tcaRandomIter, dati, u, v, col, row);

                if (dati[1] > 0) {
                    dev1 = dati[2];
                    dev2 = (PI / 4) - dati[2];

                    if (dati[9] == 1) {
                        dev2 = -dev2;
                    } else {
                        dev1 = -dev1;
                    }
                    calcarea(row, col, dati, v, analyseRandomIter, deviationRandomIter, pitRandomIter, tcaRandomIter,
                            dirRandomIter, i, i);

                    sumdev = dati[6];
                    sumdev1 = dev1 + (pLambda * sumdev);
                    sumdev2 = dev2 + (pLambda * sumdev);
                    if ((Math.abs(sumdev1) <= Math.abs(sumdev2)) && ((dati[3] - dati[4]) > 0.0)) {
                        dirRandomIter.setSample(col, row, 0, dati[7]);
                        deviationRandomIter.setSample(col, row, 0, sumdev1);
                    } else if (Math.abs(sumdev1) > Math.abs(sumdev2) || (dati[3] - dati[5]) > 0.0) {
                        dirRandomIter.setSample(col, row, 0, dati[8]);
                        deviationRandomIter.setSample(col, row, 0, sumdev2);
                    } else {
                        break;
                    }
                } else if (dati[1] == 0) {
                    if (ncelle == nelev) {
                        /* sono all'uscita */
                        calcarea(row, col, dati, v, analyseRandomIter, deviationRandomIter, pitRandomIter, tcaRandomIter,
                                dirRandomIter, cols, rows);
                        dirRandomIter.setSample(col, row, 0, 10);
                        deviationRandomIter.setSample(col, row, 0, pLambda * dati[6]);

                        if (tcaRandomIter.getSampleDouble(col, row, 0) != ncelle) {
                            pm.done();
                            return;
                        } else {
                            pm.done();
                            return;
                        }
                    } else {
                        calcarea(row, col, dati, v, analyseRandomIter, deviationRandomIter, pitRandomIter, tcaRandomIter,
                                dirRandomIter, cols, rows);
                        sumdev = pLambda * dati[6];
                        dirRandomIter.setSample(col, row, 0, flowRandomIter.getSampleDouble(col, row, 0));
                        flow = dirRandomIter.getSampleDouble(col, row, 0);
                        nr = row + order[(int) flow][0];
                        nc = col + order[(int) flow][1];
                        while( analyseRandomIter.getSampleDouble(nc, nr, 0) == 1 ) {
                            tcaRandomIter.setSample(nc, nr, 0, tcaRandomIter.getSampleDouble(nc, nr, 0)
                                    + tcaRandomIter.getSampleDouble(col, row, 0));
                            flow = dirRandomIter.getSampleDouble(nc, nr, 0);
                            nr = nr + order[(int) flow][0];
                            nc = nc + order[(int) flow][1];
                        }
                        deviationRandomIter.setSample(col, row, 0, sumdev);
                    }
                }
            } else if (isNovalue(pitRandomIter.getSampleDouble(col, row, 0))) {
                break;
            }
            pm.worked(1);
        }
        pm.done();

        dirRandomIter.done();
        pitRandomIter.done();
        flowRandomIter.done();
        deviationRandomIter.done();
        analyseRandomIter.done();
        tcaRandomIter.done();
    }

    /**
     * routine that defines the draining directions
     * 
     * @param indexes
     *            vector containing the order of elevation
     * @param deviationsImage
     *            the map containing the deviation
     * @param analyzeImage
     * @param nelev
     * @return
     */
    private void OrlandiniD8_LTD( double[] indexes, WritableRaster deviationsImage, WritableRaster analyzeImage,
            WritableRaster pitImage, WritableRaster flowImage, WritableRaster tcaImage, WritableRaster dirImage, int nelev ) {

        int row, col, ncelle, nr, nc;
        double dx, dev1, dev2, sumdev1, sumdev2, sumdev;
        double[] dati = new double[10]; /*
                                         * it contains:
                                         * pend,dir,e0,e1,e2,sumdev,
                                         * didren1,dirdren2,sigma
                                         */
        double count, flow;
        /*
         * it indicates the position of the triangle's vertexes
         */
        ncelle = 0;
        RandomIter pitRandomIter = RandomIterFactory.create(pitImage, null);
        RandomIter flowRandomIter = RandomIterFactory.create(flowImage, null);

        WritableRandomIter tcaRandomIter = RandomIterFactory.createWritable(tcaImage, null);
        WritableRandomIter analyseRandomIter = RandomIterFactory.createWritable(analyzeImage, null);
        WritableRandomIter deviationRandomIter = RandomIterFactory.createWritable(deviationsImage, null);
        WritableRandomIter dirRandomIter = RandomIterFactory.createWritable(dirImage, null);
        double[] u = {xRes, yRes};
        double[] v = {NaN, NaN};
        dx = u[0];
        // get rows and cols from the active region
        rows = pitImage.getHeight();
        cols = pitImage.getWidth();

        ncelle = 0;
        pm.beginTask(msg.message("draindir.orlandiniltd"), rows * cols);
        for( int i = rows * cols - 1; i >= 0; i-- ) {
            if (isCanceled(pm)) {
                return;
            }
            count = indexes[i];
            col = (int) count % cols - 1;
            row = (int) count / cols;

            if (!isNovalue(pitRandomIter.getSampleDouble(col, row, 0)) && !isNovalue(flowRandomIter.getSampleDouble(col, row, 0))) {
                ncelle = ncelle + 1;

                compose(analyseRandomIter, pitRandomIter, tcaRandomIter, dati, u, v, col, row);

                if (dati[1] > 0) {
                    dev1 = dx * Math.sin(dati[2]);
                    dev2 = dx * Math.sqrt(2.0) * Math.sin(PI / 4 - dati[2]);
                    if (dati[9] == 1) {
                        dev2 = -dev2;
                    } else {
                        dev1 = -dev1;
                    }
                    calcarea(row, col, dati, v, analyseRandomIter, deviationRandomIter, pitRandomIter, tcaRandomIter,
                            dirRandomIter, cols, rows);
                    sumdev = dati[6];
                    sumdev1 = dev1 + pLambda * sumdev;
                    sumdev2 = dev2 + pLambda * sumdev;
                    if (Math.abs(sumdev1) <= Math.abs(sumdev2) && (dati[3] - dati[4]) > 0.0) {
                        dirRandomIter.setSample(col, row, 0, dati[7]);
                        deviationRandomIter.setSample(col, row, 0, sumdev1);
                    } else if (Math.abs(sumdev1) > Math.abs(sumdev2) || (dati[3] - dati[5]) > 0.0) {
                        dirRandomIter.setSample(col, row, 0, dati[8]);
                        deviationRandomIter.setSample(col, row, 0, sumdev2);
                    } else {
                        break;
                    }
                } else if (dati[1] == 0) {
                    if (ncelle == nelev) {
                        /* sono all'uscita */
                        calcarea(row, col, dati, v, analyseRandomIter, deviationRandomIter, pitRandomIter, tcaRandomIter,
                                dirRandomIter, cols, rows);
                        dirRandomIter.setSample(col, row, 0, 10);
                        deviationRandomIter.setSample(col, row, 0, pLambda * dati[6]);

                        if (tcaRandomIter.getSampleDouble(col, row, 0) != ncelle) {
                            pm.done();
                            return;
                        } else {
                            pm.done();
                            return;
                        }
                    } else {
                        calcarea(row, col, dati, v, analyseRandomIter, deviationRandomIter, pitRandomIter, tcaRandomIter,
                                dirRandomIter, cols, rows);
                        sumdev = pLambda * dati[6];
                        dirRandomIter.setSample(col, row, 0, flowRandomIter.getSampleDouble(col, row, 0));
                        flow = dirRandomIter.getSampleDouble(col, row, 0);
                        nr = row + order[(int) flow][0];
                        nc = col + order[(int) flow][1];
                        while( analyseRandomIter.getSampleDouble(nc, nr, 0) == 1 ) {
                            tcaRandomIter.setSample(nc, nr, 0, (tcaRandomIter.getSampleDouble(nc, nr, 0) + tcaRandomIter
                                    .getSampleDouble(col, row, 0)));
                            flow = dirRandomIter.getSampleDouble(nc, nr, 0);
                            nr = nr + order[(int) flow][0];
                            nc = nc + order[(int) flow][1];
                        }
                        deviationRandomIter.setSample(col, row, 0, sumdev);
                    }
                }
            } else if (!isNovalue(pitRandomIter.getSampleDouble(col, row, 0))) {
                break;
            }
            pm.worked(1);
        }
        pm.done();

        dirRandomIter.done();
        pitRandomIter.done();
        flowRandomIter.done();
        deviationRandomIter.done();
        analyseRandomIter.done();
        tcaRandomIter.done();
    }

    /**
     * It calculates the drainage area for a cell[rows][cols]
     * 
     * @param row
     * @param col
     * @param dati
     * @param v
     * @param analyse
     * @param deviation
     */
    private void calcarea( int row, int col, double[] dati, double[] v, WritableRandomIter analyse, WritableRandomIter deviation,
            RandomIter pitRandomIter, WritableRandomIter tcaRandomIter, WritableRandomIter dirRandomIter, int nCols, int nRows ) {
        int conta, ninflow;
        int outdir;
        double sumdev;
        double[] dev = new double[8];
        double[] are = new double[8];

        ninflow = 0;
        sumdev = 0;
        for( int n = 1; n <= 8; n++ ) {
            conta = (col + order[n][1] - 1) * nCols + row + order[n][0];
            /*
             * verifico se la cella che sto considerando e' stata gia' processata
             */
            if (analyse.getSampleDouble(col + order[n][1], row + order[n][0], 0) == 1) {
                if (!isNovalue(pitRandomIter.getSampleDouble(col + order[n][1], row + order[n][0], 0)) || conta <= nRows * nCols) {
                    outdir = (int) dirRandomIter.getSampleDouble(col + order[n][1], row + order[n][0], 0);
                    /*
                     * verifico se la cella che sto considerando drena nel pixel
                     * centrale
                     */
                    if (outdir - n == 4 || outdir - n == -4) {
                        ninflow = ninflow + 1;
                        tcaRandomIter.setSample(col, row, 0, tcaRandomIter.getSampleDouble(col, row, 0)
                                + tcaRandomIter.getSampleDouble(col + order[n][1], row + order[n][0], 0));
                        dev[ninflow] = deviation.getSampleDouble(col + order[n][1], row + order[n][0], 0);
                        are[ninflow] = tcaRandomIter.getSampleDouble(col + order[n][1], row + order[n][0], 0);
                    }
                }
            }
        }

        for( int i = 1; i <= ninflow; i++ ) {
            sumdev = sumdev + are[i] * dev[i] / tcaRandomIter.getSampleDouble(col, row, 0);
        }
        dati[6] = sumdev;

    }

    /**
     * It calculates the direction of maximun slope.
     * 
     * @param analyse
     * @param dati
     * @param u
     * @param v
     * @param col
     * @param row
     */
    private void compose( WritableRandomIter analyse, RandomIter pitRandomIter, WritableRandomIter tcaRandomIter, double[] dati,
            double[] u, double[] v, int col, int row ) {
        int n = 1, m = 1;

        double pendmax, dirmax = 0.0, e1min = -9999.0, e2min = -9999.0;
        int[][] tri = {{1, 2, 1}, /* tri 012 */
        {3, 2, -1}, /* tri 023 |4|3|2| */
        {3, 4, 1}, /* tri 034 |5|0|1| drainage direction. */
        {5, 4, -1}, /* tri 045 |6|7|8| */
        {5, 6, 1}, /*
                    * tri 056 indico direzioni di drenaggio corrispondenti ai
                    * verici
                    */
        {7, 6, -1}, /*
                     * tri 067 dei triangoli (colonne 1,2) e il segno (sigma)
                     * associato
                     */
        {7, 8, 1}, /* tri 078 al triangolo stesso (colonna 3). */
        {1, 8, -1} /* tri 089 */
        };

        analyse.setSample(col, row, 0, 1.0);
        tcaRandomIter.setSample(col, row, 0, 1.0);
        pendmax = 0.0;
        dati[3] = pitRandomIter.getSampleDouble(col, row, 0);
        /*
         * per ogni triangolo calcolo la pendenza massima e la direzione di
         * deflusso reale.
         */
        for( int j = 0; j <= 7; j++ ) {
            n = tri[j][0];
            m = tri[j][1];

            dati[4] = pitRandomIter.getSampleDouble(col + order[n][1], row + order[n][0], 0);
            dati[5] = pitRandomIter.getSampleDouble(col + order[m][1], row + order[m][0], 0);
            /*
             * verifico che i punti attorno al pixel considerato non siano
             * novalue. In questo caso trascuro il triangolo.
             */
            if (!isNovalue(dati[4]) && !isNovalue(dati[5])) {

                triangoli(u, dati);
                if (dati[1] > pendmax) {
                    dirmax = dati[2];
                    pendmax = dati[1];
                    dati[7] = tri[j][0]; /* - direzione cardinale */
                    dati[8] = tri[j][1]; /* - direzione diagonale */
                    dati[9] = tri[j][2]; /* - segno del triangolo */
                    e1min = dati[4]; /*
                                      * - quote del triangolo avente pendenza
                                      * maggiore
                                      */
                    e2min = dati[5]; /*
                                      * non necessariamente sono le quote
                                      * minime.
                                      */
                }
            }
        }
        dati[1] = pendmax;
        dati[2] = dirmax;
        dati[4] = e1min;
        dati[5] = e2min;

    }

    /**
     * Calcola per ogni triangolo la direzione e la pendenza massima.
     * 
     * @param u
     * @param dati
     */
    private void triangoli( double[] u, double[] dati ) {
        double pend1, pend2, sp, sd, dx, dy;
        /* definsco le dim. del pixel */
        dx = u[0];
        dy = u[1];

        pend1 = (dati[3] - dati[4]) / dy;
        pend2 = (dati[4] - dati[5]) / dx;
        if (pend1 == 0.0) {
            if (pend2 >= 0.0) {
                dati[2] = +PI / 2;
            } else {
                dati[2] = -PI / 2;
            }
        } else {
            dati[2] = Math.atan(pend2 / pend1);
        }
        sp = Math.sqrt(pend1 * pend1 + pend2 * pend2);
        sd = (dati[3] - dati[5]) / Math.sqrt(dx * dx + dy * dy);

        if (dati[2] >= 0 && dati[2] <= PI / 4 && pend1 >= 0) {
            dati[1] = sp;
        } else {
            if (pend1 > sd) {
                dati[1] = pend1;
                dati[2] = 0;
            } else {
                dati[1] = sd;
                dati[2] = PI / 4;
            }
        }
    }

    /**
     * The fixed network method allows you to assign a known channel network and
     * to then correct the drainage directions.
     */
    private void newDirections( WritableRaster pitWR, WritableRaster dirWR ) {
        int[][] odir = {{0, 0, 0}, {0, 1, 1}, {-1, 1, 2}, {-1, 0, 3}, {-1, -1, 4}, {0, -1, 5}, {1, -1, 6}, {1, 0, 7}, {1, 1, 8},
                {0, 0, 9}, {0, 0, 10}};
        double elev = 0.0;
        int[] flow = new int[2], nflow = new int[2];
        RandomIter pitRandomIter = RandomIterFactory.create(pitWR, null);

        RenderedImage flowFixedRI = inFlownet.getRenderedImage();
        WritableRaster flowFixedWR = CoverageUtilities.renderedImage2WritableRaster(flowFixedRI, true);
        RandomIter flowFixedIter = RandomIterFactory.create(flowFixedWR, null);

        WritableRandomIter dirRandomIter = RandomIterFactory.createWritable(dirWR, null);

        WritableRaster modflowImage = CoverageUtilities.createDoubleWritableRaster(pitWR.getWidth(), pitWR.getHeight(), null,
                null, null);
        WritableRandomIter modflowRandomIter = RandomIterFactory.createWritable(modflowImage, null);

        pm.beginTask("new directions...", rows);
        for( int j = 0; j <= rows; j++ ) {
            if (isCanceled(pm)) {
                return;
            }
            for( int i = 0; i <= cols; i++ ) {
                if (!isNovalue(flowFixedIter.getSampleDouble(i, j, 0))) {
                    flow[0] = i;
                    flow[1] = j;
                    for( int k = 1; k <= 8; k++ ) {
                        nflow[0] = flow[0] + odir[k][1];
                        nflow[1] = flow[1] + odir[k][0];
                        if (modflowRandomIter.getSampleDouble(nflow[0], nflow[1], 0) == 0
                                && isNovalue(flowFixedIter.getSampleDouble(nflow[0], nflow[1], 0))) {
                            elev = pitRandomIter.getSampleDouble(nflow[0] + odir[1][1], nflow[1] + odir[1][0], 0);
                            for( int n = 2; n <= 8; n++ ) {
                                if (nflow[0] + odir[n][0] >= 0 && nflow[0] + odir[n][1] <= rows && nflow[1] + odir[n][0] >= 0
                                        && nflow[1] + odir[n][0] <= cols) {
                                    if (pitRandomIter.getSampleDouble(nflow[0] + odir[n][1], nflow[1] + odir[n][0], 0) >= elev) {
                                        elev = pitRandomIter.getSampleDouble(nflow[0] + odir[n][1], nflow[1] + odir[n][0], 0);
                                        dirRandomIter.setSample(nflow[0], nflow[1], 0, odir[n][2]);
                                    }
                                }
                            }
                            for( int s = 1; s <= 8; s++ ) {
                                if (nflow[0] + odir[s][0] >= 0 && nflow[0] + odir[s][0] <= rows && nflow[1] + odir[s][1] >= 0
                                        && nflow[1] + odir[s][1] <= cols) {
                                    if (!isNovalue(flowFixedIter.getSampleDouble(nflow[0] + odir[s][1], nflow[1] + odir[s][0], 0))) {

                                        if (pitRandomIter.getSampleDouble(nflow[0] + odir[s][1], nflow[1] + odir[s][0], 0) <= elev) {
                                            elev = pitRandomIter.getSampleDouble(nflow[0] + odir[s][1], nflow[1] + odir[s][0], 0);
                                            dirRandomIter.setSample(nflow[0], nflow[1], 0, odir[s][2]);
                                        }
                                    }
                                }
                            }
                            modflowRandomIter.setSample(nflow[0], nflow[1], 0, 1);
                        }

                    }
                }
                if (!isNovalue(flowFixedIter.getSampleDouble(i, j, 0))) {
                    dirRandomIter.setSample(i, j, 0, flowFixedIter.getSampleDouble(i, j, 0));
                }
            }
            pm.worked(1);
        }
        pm.done();

        dirRandomIter.done();
        pitRandomIter.done();
        modflowRandomIter.done();
        flowFixedIter.done();

    }

}
