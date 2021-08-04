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
package org.hortonmachine.hmachine.modules.geomorphology.draindir;

import static org.hortonmachine.gears.libs.modules.HMConstants.GEOMORPHOLOGY;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsSupporter;
import org.hortonmachine.gears.utils.BitMatrix;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.sorting.QuickSortAlgorithm;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

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

@Description(OmsDrainDir.OMSDRAINDIR_DESCRIPTION)
@Documentation(OmsDrainDir.OMSDRAINDIR_DOCUMENTATION)
@Author(name = OmsDrainDir.OMSDRAINDIR_AUTHORNAMES, contact = OmsDrainDir.OMSDRAINDIR_AUTHORCONTACTS)
@Keywords(OmsDrainDir.OMSDRAINDIR_KEYWORDS)
@Label(OmsDrainDir.OMSDRAINDIR_LABEL)
@Name(OmsDrainDir.OMSDRAINDIR_NAME)
@Status(OmsDrainDir.OMSDRAINDIR_STATUS)
@License(OmsDrainDir.OMSDRAINDIR_LICENSE)
public class OmsDrainDir extends HMModel {

    @Description(OMSDRAINDIR_inPit_DESCRIPTION)
    @In
    public GridCoverage2D inPit = null;

    @Description(OMSDRAINDIR_inFlow_DESCRIPTION)
    @In
    public GridCoverage2D inFlow = null;

    @Description(OMSDRAINDIR_inFlownet_DESCRIPTION)
    @In
    public GridCoverage2D inFlownet = null;

    @Description(OMSDRAINDIR_pLambda_DESCRIPTION)
    @In
    public double pLambda = 1.0;

    @Description(OMSDRAINDIR_doLad_DESCRIPTION)
    @In
    public boolean doLad = true;

    @Description(OMSDRAINDIR_outFlow_DESCRIPTION)
    @Out
    public GridCoverage2D outFlow = null;

    @Description(OMSDRAINDIR_outTca_DESCRIPTION)
    @Out
    public GridCoverage2D outTca = null;

    public static final String OMSDRAINDIR_DESCRIPTION = "It calculates the drainage directions minimizing the deviation from the real flow";
    public static final String OMSDRAINDIR_DOCUMENTATION = "OmsDrainDir.html";
    public static final String OMSDRAINDIR_KEYWORDS = "Geomorphology, Pitfiller, OmsFlowDirections";
    public static final String OMSDRAINDIR_LABEL = GEOMORPHOLOGY;
    public static final String OMSDRAINDIR_NAME = "draindir";
    public static final int OMSDRAINDIR_STATUS = 40;
    public static final String OMSDRAINDIR_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSDRAINDIR_AUTHORNAMES = "Andrea Antonello, Franceschi Silvia, Erica Ghesla, Rigon Riccardo";
    public static final String OMSDRAINDIR_AUTHORCONTACTS = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSDRAINDIR_inPit_DESCRIPTION = "The depitted elevation model.";
    public static final String OMSDRAINDIR_inFlow_DESCRIPTION = "The map of flowdirections.";
    public static final String OMSDRAINDIR_inFlownet_DESCRIPTION = "The map of flowdirections on the network pixels (considered only in case of LTD method). Remember that in the case of fixed flow calculation the tca has to be recalculated afterwards; the tca output in this case is not corrected.";
    public static final String OMSDRAINDIR_pLambda_DESCRIPTION = "The direction correction factor.";
    public static final String OMSDRAINDIR_doLad_DESCRIPTION = "Switch for the mode to use: true = LAD (default), false = LTD)).";
    public static final String OMSDRAINDIR_outFlow_DESCRIPTION = "The map of drainage directions.";
    public static final String OMSDRAINDIR_outTca_DESCRIPTION = "The map of total contributing areas.";

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private static final double PI = Math.PI;

    /*
     * indicates the position of the triangle's vertexes
     */
    private int[][] order = ModelsSupporter.DIR;

    private int[][] tri = {{1, 2, 1}, /* tri 012 */
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

    private int cols;
    private int rows;
    private double xRes;
    private double yRes;
    private double dxySqrt;

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
        checkNull(inFlow, inPit);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inPit);
        cols = regionMap.getCols();
        rows = regionMap.getRows();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();
        dxySqrt = Math.sqrt(xRes * xRes + yRes * yRes);

        RenderedImage pitfillerRI = inPit.getRenderedImage();
        WritableRaster pitfillerWR = CoverageUtilities.renderedImage2DoubleWritableRaster(pitfillerRI, true);
        RenderedImage flowRI = inFlow.getRenderedImage();
        WritableRaster flowWR = CoverageUtilities.renderedImage2ShortWritableRaster(flowRI, true);

        RandomIter pitRandomIter = RandomIterFactory.create(pitfillerWR, null);

        // create new matrix
        double[] orderedelev = new double[cols * rows];
        int[] indexes = new int[cols * rows];

        int nelev = 0;
        for( int r = 0; r < rows; r++ ) {
            if (pm.isCanceled()) {
                return;
            }
            for( int c = 0; c < cols; c++ ) {
                double pitValue = pitRandomIter.getSampleDouble(c, r, 0);
                int pos = (r * cols) + c;
                orderedelev[pos] = pitValue;
                indexes[pos] = pos + 1;
                if (!isNovalue(pitValue)) {
                    nelev = nelev + 1;
                }
            }
        }

        QuickSortAlgorithm t = new QuickSortAlgorithm(pm);
        t.sort(orderedelev, indexes);

        pm.message(msg.message("draindir.initializematrix"));

        // Initialize new RasterData and set value
        WritableRaster tcaWR = CoverageUtilities.createWritableRaster(cols, rows, Integer.class, null, HMConstants.intNovalue);
        WritableRaster dirWR = CoverageUtilities.createWritableRaster(cols, rows, Short.class, null, HMConstants.shortNovalue);

        // it contains the analyzed cells
        WritableRaster deviationsWR = CoverageUtilities.createWritableRaster(cols, rows, Double.class, null, null);
        BitMatrix analizedMatrix = new BitMatrix(cols, rows);

        if (doLad) {
            orlandiniD8LAD(indexes, deviationsWR, analizedMatrix, pitfillerWR, flowWR, tcaWR, dirWR, nelev);
        } else {
            orlandiniD8LTD(indexes, deviationsWR, analizedMatrix, pitfillerWR, flowWR, tcaWR, dirWR, nelev);
            if (pm.isCanceled()) {
                return;
            }
            // only if required executes this method
            if (inFlownet != null) {
                newDirections(pitfillerWR, dirWR);
            }
        }
        if (pm.isCanceled()) {
            return;
        }
        outFlow = CoverageUtilities.buildCoverageWithNovalue("draindir", dirWR, regionMap, inPit.getCoordinateReferenceSystem(), HMConstants.intNovalue);
        outTca = CoverageUtilities.buildCoverageWithNovalue("tca", tcaWR, regionMap, inPit.getCoordinateReferenceSystem(), HMConstants.shortNovalue);
    }

    private void orlandiniD8LAD( int[] indexes, WritableRaster deviationsWR, BitMatrix analizedMatrix, WritableRaster pitWR,
            WritableRaster flowWR, WritableRaster tcaWR, WritableRaster dirWR, int nelev ) {
        RandomIter pitRandomIter = RandomIterFactory.create(pitWR, null);
        RandomIter flowRandomIter = RandomIterFactory.create(flowWR, null);

        WritableRandomIter tcaRandomIter = RandomIterFactory.createWritable(tcaWR, null);
        WritableRandomIter deviationRandomIter = RandomIterFactory.createWritable(deviationsWR, null);
        WritableRandomIter dirRandomIter = RandomIterFactory.createWritable(dirWR, null);

        try {
            int ncelle = 0;
            pm.beginTask(msg.message("draindir.orlandinilad"), rows * cols);
            for( int i = rows * cols - 1; i >= 0; i-- ) {
                if (pm.isCanceled()) {
                    return;
                }
                double count = indexes[i] - 1;
                int row = (int) Math.floor(count / cols);
                int col = (int) (count % cols);
                try {
                    if (!isNovalue(pitRandomIter.getSampleDouble(col, row, 0)) && !isNovalue(flowRandomIter.getSample(col, row, 0))) {
                        ncelle = ncelle + 1;
                        double[] maxSlopeData = calculateMaximumSlope(analizedMatrix, pitRandomIter, tcaRandomIter, col, row);

                        if (maxSlopeData[1] > 0) {
                            double dev1 = maxSlopeData[2];
                            double dev2 = ((PI / 4) - maxSlopeData[2]);

                            if (maxSlopeData[9] == 1) {
                                dev2 = -dev2;
                            } else {
                                dev1 = -dev1;
                            }
                            calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationRandomIter, pitRandomIter,
                                    tcaRandomIter, dirRandomIter, i, i);

                            double sumdev = maxSlopeData[6];
                            double sumdev1 = dev1 + (pLambda * sumdev);
                            double sumdev2 = dev2 + (pLambda * sumdev);
                            if ((Math.abs(sumdev1) <= Math.abs(sumdev2)) && ((maxSlopeData[3] - maxSlopeData[4]) > 0.0)) {
                                dirRandomIter.setSample(col, row, 0, maxSlopeData[7]);
                                deviationRandomIter.setSample(col, row, 0, sumdev1);
                            } else if (Math.abs(sumdev1) > Math.abs(sumdev2) || (maxSlopeData[3] - maxSlopeData[5]) > 0.0) {
                                dirRandomIter.setSample(col, row, 0, maxSlopeData[8]);
                                deviationRandomIter.setSample(col, row, 0, sumdev2);
                            } else {
                                break;
                            }
                        } else if (maxSlopeData[1] == 0) {
                            if (ncelle == nelev) {
                                /* sono all'uscita */
                                calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationRandomIter, pitRandomIter,
                                        tcaRandomIter, dirRandomIter, cols, rows);
                                dirRandomIter.setSample(col, row, 0, FlowNode.OUTLET);
                                deviationRandomIter.setSample(col, row, 0, pLambda * maxSlopeData[6]);

                                pm.done();
                                return;
                            } else {
                                calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationRandomIter, pitRandomIter,
                                        tcaRandomIter, dirRandomIter, cols, rows);
                                double sumdev = pLambda * maxSlopeData[6];
                                dirRandomIter.setSample(col, row, 0, flowRandomIter.getSample(col, row, 0));
                                int flow = dirRandomIter.getSample(col, row, 0);
                                int nr = row + order[flow][0];
                                int nc = col + order[flow][1];
                                while( analizedMatrix.isMarked(nc, nr) ) {
                                    tcaRandomIter.setSample(nc, nr, 0,
                                            tcaRandomIter.getSample(nc, nr, 0) + tcaRandomIter.getSample(col, row, 0));
                                    flow = dirRandomIter.getSample(nc, nr, 0);
                                    nr = nr + order[(int) flow][0];
                                    nc = nc + order[(int) flow][1];
                                }
                                deviationRandomIter.setSample(col, row, 0, sumdev);
                            }
                        }
                    }
                } catch (Exception e) {
                    pm.errorMessage("Error in col: " + col + " row: " + row);
                    e.printStackTrace();
                }
                pm.worked(1);
            }
            pm.done();

        } finally {
            dirRandomIter.done();
            pitRandomIter.done();
            flowRandomIter.done();
            deviationRandomIter.done();
            tcaRandomIter.done();
        }
    }

    private void orlandiniD8LTD( int[] indexes, WritableRaster deviationsWR, BitMatrix analizedMatrix, WritableRaster pitWR,
            WritableRaster flowWR, WritableRaster tcaWR, WritableRaster dirWR, int nelev ) {

        /*
         * it indicates the position of the triangle's vertexes
         */
        // ncelle = 0;
        RandomIter pitRandomIter = RandomIterFactory.create(pitWR, null);
        RandomIter flowRandomIter = RandomIterFactory.create(flowWR, null);

        WritableRandomIter tcaRandomIter = RandomIterFactory.createWritable(tcaWR, null);
        WritableRandomIter deviationRandomIter = RandomIterFactory.createWritable(deviationsWR, null);
        WritableRandomIter dirRandomIter = RandomIterFactory.createWritable(dirWR, null);
        try {
            int ncelle = 0;
            pm.beginTask(msg.message("draindir.orlandiniltd"), rows * cols);
            for( int i = rows * cols - 1; i >= 0; i-- ) {
                if (pm.isCanceled()) {
                    return;
                }
                double count = indexes[i] - 1;
                int row = (int) Math.floor(count / cols);
                int col = (int) (count % cols);

                if (!isNovalue(pitRandomIter.getSampleDouble(col, row, 0)) && !isNovalue(flowRandomIter.getSample(col, row, 0))) {
                    ncelle = ncelle + 1;

                    double[] maxSlopeData = calculateMaximumSlope(analizedMatrix, pitRandomIter, tcaRandomIter, col, row);

                    if (maxSlopeData[1] > 0) {
                        double dev1 = (xRes * Math.sin(maxSlopeData[2]));
                        double dev2 = (xRes * Math.sqrt(2.0) * Math.sin(PI / 4 - maxSlopeData[2]));
                        if (maxSlopeData[9] == 1) {
                            dev2 = -dev2;
                        } else {
                            dev1 = -dev1;
                        }
                        calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationRandomIter, pitRandomIter,
                                tcaRandomIter, dirRandomIter, cols, rows);
                        double sumdev = maxSlopeData[6];
                        double sumdev1 = dev1 + pLambda * sumdev;
                        double sumdev2 = dev2 + pLambda * sumdev;
                        if (Math.abs(sumdev1) <= Math.abs(sumdev2) && (maxSlopeData[3] - maxSlopeData[4]) > 0.0) {
                            dirRandomIter.setSample(col, row, 0, (int) maxSlopeData[7]);
                            deviationRandomIter.setSample(col, row, 0, sumdev1);
                        } else if (Math.abs(sumdev1) > Math.abs(sumdev2) || (maxSlopeData[3] - maxSlopeData[5]) > 0.0) {
                            dirRandomIter.setSample(col, row, 0, (int) maxSlopeData[8]);
                            deviationRandomIter.setSample(col, row, 0, sumdev2);
                        } else {
                            break;
                        }
                    } else if (maxSlopeData[1] == 0) {
                        if (ncelle == nelev) {
                            /* sono all'uscita */
                            calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationRandomIter, pitRandomIter,
                                    tcaRandomIter, dirRandomIter, cols, rows);
                            dirRandomIter.setSample(col, row, 0, FlowNode.OUTLET);
                            deviationRandomIter.setSample(col, row, 0, pLambda * maxSlopeData[6]);

                            pm.done();
                            return;
                        } else {
                            calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationRandomIter, pitRandomIter,
                                    tcaRandomIter, dirRandomIter, cols, rows);
                            double sumdev = pLambda * maxSlopeData[6];
                            dirRandomIter.setSample(col, row, 0, flowRandomIter.getSample(col, row, 0));
                            int flow = dirRandomIter.getSample(col, row, 0);
                            int nr = row + order[flow][0];
                            int nc = col + order[flow][1];
                            while( analizedMatrix.isMarked(nc, nr) ) {
                                tcaRandomIter.setSample(nc, nr, 0,
                                        (tcaRandomIter.getSample(nc, nr, 0) + tcaRandomIter.getSample(col, row, 0)));
                                flow = dirRandomIter.getSample(nc, nr, 0);
                                nr = nr + order[flow][0];
                                nc = nc + order[flow][1];
                            }
                            deviationRandomIter.setSample(col, row, 0, sumdev);
                        }
                    }
                }
                pm.worked(1);
            }
            pm.done();
        } finally {
            dirRandomIter.done();
            pitRandomIter.done();
            flowRandomIter.done();
            deviationRandomIter.done();
            tcaRandomIter.done();
        }
    }

    private void calculateDrainageArea( int row, int col, double[] dati, BitMatrix analizedMatrix, WritableRandomIter deviation,
            RandomIter pitRandomIter, WritableRandomIter tcaRandomIter, WritableRandomIter dirRandomIter, int nCols, int nRows ) {
        double[] dev = new double[8];
        int[] are = new int[8];

        int ninflow = 0;
        double sumdev = 0;
        for( int n = 1; n <= 8; n++ ) {
            int conta = (col + order[n][1] - 1) * nCols + row + order[n][0];
            /*
             * verifico se la cella che sto considerando e' stata gia' processata
             */
            if (analizedMatrix.isMarked(col + order[n][1], row + order[n][0])) {
                if (!isNovalue(pitRandomIter.getSampleDouble(col + order[n][1], row + order[n][0], 0))
                        || conta <= nRows * nCols) {
                    int outdir = dirRandomIter.getSample(col + order[n][1], row + order[n][0], 0);
                    /*
                     * verifico se la cella che sto considerando drena nel pixel
                     * centrale
                     */
                    if (outdir - n == 4 || outdir - n == -4) {
                        ninflow = ninflow + 1;
                        tcaRandomIter.setSample(col, row, 0, tcaRandomIter.getSample(col, row, 0)
                                + tcaRandomIter.getSample(col + order[n][1], row + order[n][0], 0));
                        dev[ninflow] = deviation.getSampleDouble(col + order[n][1], row + order[n][0], 0);
                        are[ninflow] = tcaRandomIter.getSample(col + order[n][1], row + order[n][0], 0);
                    }
                }
            }
        }

        for( int i = 1; i <= ninflow; i++ ) {
            sumdev = sumdev + are[i] * dev[i] / tcaRandomIter.getSample(col, row, 0);
        }
        dati[6] = sumdev;

    }

    /**
     * Calculates the max slope data as an array: [maxslope,maxdir,elevation,e1,e2,sumdev, dirdren1,dirdren2,sigma]
     */
    private double[] calculateMaximumSlope( BitMatrix analizedMatrix, RandomIter pitRandomIter, WritableRandomIter tcaRandomIter,
            int col, int row ) {
        double[] maxSlopeData = new double[10];
        int n = 1, m = 1;

        double dirmax = 0f, e1min = -9999f, e2min = -9999f;

        analizedMatrix.mark(col, row);
        tcaRandomIter.setSample(col, row, 0, 1);
        double pendmax = 0f;
        maxSlopeData[3] = pitRandomIter.getSampleDouble(col, row, 0);
        /*
         * per ogni triangolo calcolo la pendenza massima e la direzione di
         * deflusso reale.
         */
        for( int j = 0; j <= 7; j++ ) {
            n = tri[j][0];
            m = tri[j][1];

            maxSlopeData[4] = pitRandomIter.getSampleDouble(col + order[n][1], row + order[n][0], 0);
            maxSlopeData[5] = pitRandomIter.getSampleDouble(col + order[m][1], row + order[m][0], 0);
            /*
             * verifico che i punti attorno al pixel considerato non siano
             * novalue. In questo caso trascuro il triangolo.
             */
            if (!isNovalue(maxSlopeData[4]) && !isNovalue(maxSlopeData[5])) {
                calculateMaxSlopeAndDirection4Triangles(maxSlopeData);
                if (maxSlopeData[1] > pendmax) {
                    dirmax = maxSlopeData[2];
                    pendmax = maxSlopeData[1];
                    /* - direzione cardinale */
                    maxSlopeData[7] = tri[j][0];
                    /* - direzione diagonale */
                    maxSlopeData[8] = tri[j][1];
                    /* - segno del triangolo */
                    maxSlopeData[9] = tri[j][2];
                    /*
                     * - quote del triangolo avente pendenza
                     * maggiore
                     */
                    e1min = maxSlopeData[4];
                    /*
                     * non necessariamente sono le quote
                     * minime.
                     */
                    e2min = maxSlopeData[5];
                }
            }
        }
        maxSlopeData[1] = pendmax;
        maxSlopeData[2] = dirmax;
        maxSlopeData[4] = e1min;
        maxSlopeData[5] = e2min;

        return maxSlopeData;
    }

    private void calculateMaxSlopeAndDirection4Triangles( double[] dati ) {
        /* definsco le dim. del pixel */

        double pend1 = (dati[3] - dati[4]) / yRes;
        double pend2 = (dati[4] - dati[5]) / xRes;
        if (pend1 == 0.0) {
            if (pend2 >= 0.0) {
                dati[2] = (+PI / 2);
            } else {
                dati[2] = (-PI / 2);
            }
        } else {
            dati[2] = Math.atan(pend2 / pend1);
        }
        double sp = Math.sqrt(pend1 * pend1 + pend2 * pend2);
        double sd = ((dati[3] - dati[5]) / dxySqrt);

        if (dati[2] >= 0 && dati[2] <= PI / 4 && pend1 >= 0) {
            dati[1] = sp;
        } else {
            if (pend1 > sd) {
                dati[1] = pend1;
                dati[2] = 0;
            } else {
                dati[1] = sd;
                dati[2] = (PI / 4);
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
        int[] flow = new int[2], nflow = new int[2];
        RandomIter pitRandomIter = RandomIterFactory.create(pitWR, null);

        RandomIter flowFixedIter = CoverageUtilities.getRandomIterator(inFlownet);
        WritableRandomIter dirRandomIter = RandomIterFactory.createWritable(dirWR, null);
        WritableRaster modifiedFlowWR = CoverageUtilities.createWritableRaster(pitWR.getWidth(), pitWR.getHeight(), Integer.class,
                null, null);
        WritableRandomIter modflowRandomIter = RandomIterFactory.createWritable(modifiedFlowWR, null);

        try {
            pm.beginTask("Correcting drainage directions...", rows);
            for( int j = 0; j < rows; j++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                for( int i = 0; i < cols; i++ ) {
                    if (!isNovalue(flowFixedIter.getSample(i, j, 0))) {
                        flow[0] = i;
                        flow[1] = j;
                        for( int k = 1; k <= 8; k++ ) {
                            nflow[0] = flow[0] + odir[k][1];
                            nflow[1] = flow[1] + odir[k][0];
                            if (modflowRandomIter.getSample(nflow[0], nflow[1], 0) == 0
                                    && isNovalue(flowFixedIter.getSample(nflow[0], nflow[1], 0))) {
                                double elev = pitRandomIter.getSampleDouble(nflow[0] + odir[1][1], nflow[1] + odir[1][0], 0);
                                for( int n = 2; n <= 8; n++ ) {
                                    if (nflow[0] + odir[n][0] >= 0 && nflow[0] + odir[n][1] < rows && nflow[1] + odir[n][0] >= 0
                                            && nflow[1] + odir[n][0] < cols) {
                                        double tmpElev = pitRandomIter.getSampleDouble(nflow[0] + odir[n][1],
                                                nflow[1] + odir[n][0], 0);
                                        if (tmpElev >= elev) {
                                            elev = tmpElev;
                                            dirRandomIter.setSample(nflow[0], nflow[1], 0, odir[n][2]);
                                        }
                                    }
                                }
                                for( int s = 1; s <= 8; s++ ) {
                                    if (nflow[0] + odir[s][0] >= 0 && nflow[0] + odir[s][0] < rows && nflow[1] + odir[s][1] >= 0
                                            && nflow[1] + odir[s][1] < cols) {
                                        if (!isNovalue(
                                                flowFixedIter.getSample(nflow[0] + odir[s][1], nflow[1] + odir[s][0], 0))) {
                                            double tmpElev = pitRandomIter.getSampleDouble(nflow[0] + odir[s][1],
                                                    nflow[1] + odir[s][0], 0);
                                            if (tmpElev <= elev) {
                                                elev = tmpElev;
                                                dirRandomIter.setSample(nflow[0], nflow[1], 0, odir[s][2]);
                                            }
                                        }
                                    }
                                }
                                modflowRandomIter.setSample(nflow[0], nflow[1], 0, 1);
                            }

                        }
                    }
                    if (!isNovalue(flowFixedIter.getSample(i, j, 0))) {
                        dirRandomIter.setSample(i, j, 0, flowFixedIter.getSample(i, j, 0));
                    }
                }
                pm.worked(1);
            }
            pm.done();

        } finally {
            dirRandomIter.done();
            pitRandomIter.done();
            modflowRandomIter.done();
            flowFixedIter.done();
        }
    }

}
