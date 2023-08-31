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

import java.io.IOException;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.FlowNode;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.gears.libs.modules.ModelsSupporter;
import org.hortonmachine.gears.utils.BitMatrix;
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

        try (HMRaster pitRaster = HMRaster.fromGridCoverage(inPit); HMRaster flowRaster = HMRaster.fromGridCoverage(inFlow)) {
            cols = pitRaster.getCols();
            rows = pitRaster.getRows();
            xRes = pitRaster.getXRes();
            yRes = pitRaster.getYRes();
            dxySqrt = Math.sqrt(xRes * xRes + yRes * yRes);

            // create new matrix
            double[] orderedelev = new double[cols * rows];
            int[] indexes = new int[cols * rows];

            int nelev = 0;
            for( int r = 0; r < rows; r++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                for( int c = 0; c < cols; c++ ) {
                    double pitValue = pitRaster.getValue(c, r);
                    int pos = (r * cols) + c;
                    orderedelev[pos] = pitValue;
                    indexes[pos] = pos + 1;
                    if (!pitRaster.isNovalue(pitValue)) {
                        nelev = nelev + 1;
                    }
                }
            }

            QuickSortAlgorithm t = new QuickSortAlgorithm(pm);
            t.sort(orderedelev, indexes);

            pm.message(msg.message("draindir.initializematrix"));

            // Initialize new RasterData and set value
            try (HMRaster tcaRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inFlow).setDoInteger(true).build();
                    HMRaster dirRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inFlow).setDoShort(true).build();
                    HMRaster deviationRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inPit).build();) {
                // it contains the analyzed cells
                BitMatrix analizedMatrix = new BitMatrix(cols, rows);

                if (doLad) {
                    orlandiniD8LAD(indexes, deviationRaster, analizedMatrix, pitRaster, flowRaster, tcaRaster, dirRaster, nelev);
                } else {
                    orlandiniD8LTD(indexes, deviationRaster, analizedMatrix, pitRaster, flowRaster, tcaRaster, dirRaster, nelev);
                    if (pm.isCanceled()) {
                        return;
                    }
                    // only if required executes this method
                    if (inFlownet != null) {
                        newDirections(pitRaster, dirRaster);
                    }
                }
                if (pm.isCanceled()) {
                    return;
                }
                outFlow = dirRaster.buildCoverage();
                outTca = tcaRaster.buildCoverage();
            }
        }

    }

    private void orlandiniD8LAD( int[] indexes, HMRaster deviationsWR, BitMatrix analizedMatrix, HMRaster pitWR, HMRaster flowWR,
            HMRaster tcaWR, HMRaster dirWR, int nelev ) {

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
                if (!pitWR.isNovalue(pitWR.getValue(col, row)) && !flowWR.isNovalue(flowWR.getShortValue(col, row))) {
                    ncelle = ncelle + 1;
                    double[] maxSlopeData = calculateMaximumSlope(analizedMatrix, pitWR, tcaWR, col, row);

                    if (maxSlopeData[1] > 0) {
                        double dev1 = maxSlopeData[2];
                        double dev2 = ((PI / 4) - maxSlopeData[2]);

                        if (maxSlopeData[9] == 1) {
                            dev2 = -dev2;
                        } else {
                            dev1 = -dev1;
                        }
                        calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationsWR, pitWR, tcaWR, dirWR, i, i);

                        double sumdev = maxSlopeData[6];
                        double sumdev1 = dev1 + (pLambda * sumdev);
                        double sumdev2 = dev2 + (pLambda * sumdev);
                        if ((Math.abs(sumdev1) <= Math.abs(sumdev2)) && ((maxSlopeData[3] - maxSlopeData[4]) > 0.0)) {
                            dirWR.setValue(col, row, maxSlopeData[7]);
                            deviationsWR.setValue(col, row, sumdev1);
                        } else if (Math.abs(sumdev1) > Math.abs(sumdev2) || (maxSlopeData[3] - maxSlopeData[5]) > 0.0) {
                            dirWR.setValue(col, row, maxSlopeData[8]);
                            deviationsWR.setValue(col, row, sumdev2);
                        } else {
                            break;
                        }
                    } else if (maxSlopeData[1] == 0) {
                        if (ncelle == nelev) {
                            /* sono all'uscita */
                            calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationsWR, pitWR, tcaWR, dirWR, cols,
                                    rows);
                            dirWR.setValue(col, row, FlowNode.OUTLET);
                            deviationsWR.setValue(col, row, pLambda * maxSlopeData[6]);

                            pm.done();
                            return;
                        } else {
                            calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationsWR, pitWR, tcaWR, dirWR, cols,
                                    rows);
                            double sumdev = pLambda * maxSlopeData[6];
                            dirWR.setValue(col, row, flowWR.getShortValue(col, row));
                            int flow = dirWR.getShortValue(col, row);
                            int nr = row + order[flow][0];
                            int nc = col + order[flow][1];
                            while( analizedMatrix.isMarked(nc, nr) ) {
                                tcaWR.setValue(nc, nr, tcaWR.getIntValue(nc, nr) + tcaWR.getIntValue(col, row));
                                flow = dirWR.getShortValue(nc, nr);
                                nr = nr + order[(int) flow][0];
                                nc = nc + order[(int) flow][1];
                            }
                            deviationsWR.setValue(col, row, sumdev);
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

    }

    private void orlandiniD8LTD( int[] indexes, HMRaster deviationsWR, BitMatrix analizedMatrix, HMRaster pitWR, HMRaster flowWR,
            HMRaster tcaWR, HMRaster dirWR, int nelev ) throws IOException {

        int ncelle = 0;
        pm.beginTask(msg.message("draindir.orlandiniltd"), rows * cols);
        for( int i = rows * cols - 1; i >= 0; i-- ) {
            if (pm.isCanceled()) {
                return;
            }
            double count = indexes[i] - 1;
            int row = (int) Math.floor(count / cols);
            int col = (int) (count % cols);

            if (!pitWR.isNovalue(pitWR.getValue(col, row)) && !flowWR.isNovalue(flowWR.getShortValue(col, row))) {
                ncelle = ncelle + 1;

                double[] maxSlopeData = calculateMaximumSlope(analizedMatrix, pitWR, tcaWR, col, row);

                if (maxSlopeData[1] > 0) {
                    double dev1 = (xRes * Math.sin(maxSlopeData[2]));
                    double dev2 = (xRes * Math.sqrt(2.0) * Math.sin(PI / 4 - maxSlopeData[2]));
                    if (maxSlopeData[9] == 1) {
                        dev2 = -dev2;
                    } else {
                        dev1 = -dev1;
                    }
                    calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationsWR, pitWR, tcaWR, dirWR, cols, rows);
                    double sumdev = maxSlopeData[6];
                    double sumdev1 = dev1 + pLambda * sumdev;
                    double sumdev2 = dev2 + pLambda * sumdev;
                    if (Math.abs(sumdev1) <= Math.abs(sumdev2) && (maxSlopeData[3] - maxSlopeData[4]) > 0.0) {
                        dirWR.setValue(col, row, (int) maxSlopeData[7]);
                        deviationsWR.setValue(col, row, sumdev1);
                    } else if (Math.abs(sumdev1) > Math.abs(sumdev2) || (maxSlopeData[3] - maxSlopeData[5]) > 0.0) {
                        dirWR.setValue(col, row, (int) maxSlopeData[8]);
                        deviationsWR.setValue(col, row, sumdev2);
                    } else {
                        break;
                    }
                } else if (maxSlopeData[1] == 0) {
                    if (ncelle == nelev) {
                        /* sono all'uscita */
                        calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationsWR, pitWR, tcaWR, dirWR, cols,
                                rows);
                        dirWR.setValue(col, row, FlowNode.OUTLET);
                        deviationsWR.setValue(col, row, pLambda * maxSlopeData[6]);

                        pm.done();
                        return;
                    } else {
                        calculateDrainageArea(row, col, maxSlopeData, analizedMatrix, deviationsWR, pitWR, tcaWR, dirWR, cols,
                                rows);
                        double sumdev = pLambda * maxSlopeData[6];
                        dirWR.setValue(col, row, flowWR.getShortValue(col, row));
                        int flow = dirWR.getShortValue(col, row);
                        int nr = row + order[flow][0];
                        int nc = col + order[flow][1];
                        while( analizedMatrix.isMarked(nc, nr) ) {
                            tcaWR.setValue(nc, nr, (tcaWR.getIntValue(nc, nr) + tcaWR.getIntValue(col, row)));
                            flow = dirWR.getShortValue(nc, nr);
                            nr = nr + order[flow][0];
                            nc = nc + order[flow][1];
                        }
                        deviationsWR.setValue(col, row, sumdev);
                    }
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    private void calculateDrainageArea( int row, int col, double[] dati, BitMatrix analizedMatrix, HMRaster deviation,
            HMRaster pit, HMRaster tca, HMRaster dir, int nCols, int nRows ) throws IOException {
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
                if (!pit.isNovalue(pit.getValue(col + order[n][1], row + order[n][0])) || conta <= nRows * nCols) {
                    short outdir = dir.getShortValue(col + order[n][1], row + order[n][0]);
                    /*
                     * verifico se la cella che sto considerando drena nel pixel
                     * centrale
                     */
                    if (outdir - n == 4 || outdir - n == -4) {
                        ninflow = ninflow + 1;
                        tca.setValue(col, row, tca.getIntValue(col, row) + tca.getIntValue(col + order[n][1], row + order[n][0]));
                        dev[ninflow] = deviation.getValue(col + order[n][1], row + order[n][0]);
                        are[ninflow] = tca.getIntValue(col + order[n][1], row + order[n][0]);
                    }
                }
            }
        }

        for( int i = 1; i <= ninflow; i++ ) {
            sumdev = sumdev + are[i] * dev[i] / tca.getIntValue(col, row);
        }
        dati[6] = sumdev;

    }

    /**
     * Calculates the max slope data as an array: [maxslope,maxdir,elevation,e1,e2,sumdev, dirdren1,dirdren2,sigma]
     * @throws IOException 
     */
    private double[] calculateMaximumSlope( BitMatrix analizedMatrix, HMRaster pit, HMRaster tca, int col, int row )
            throws IOException {
        double[] maxSlopeData = new double[10];
        int n = 1, m = 1;

        double dirmax = 0f, e1min = -9999f, e2min = -9999f;

        analizedMatrix.mark(col, row);
        tca.setValue(col, row, 1);
        double pendmax = 0f;
        maxSlopeData[3] = pit.getValue(col, row);
        /*
         * per ogni triangolo calcolo la pendenza massima e la direzione di
         * deflusso reale.
         */
        for( int j = 0; j <= 7; j++ ) {
            n = tri[j][0];
            m = tri[j][1];

            maxSlopeData[4] = pit.getValue(col + order[n][1], row + order[n][0]);
            maxSlopeData[5] = pit.getValue(col + order[m][1], row + order[m][0]);
            /*
             * verifico che i punti attorno al pixel considerato non siano
             * novalue. In questo caso trascuro il triangolo.
             */
            if (!pit.isNovalue(maxSlopeData[4]) && !pit.isNovalue(maxSlopeData[5])) {
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
     * @throws IOException
     */
    private void newDirections( HMRaster pit, HMRaster dir ) throws Exception {
        int[][] odir = {{0, 0, 0}, {0, 1, 1}, {-1, 1, 2}, {-1, 0, 3}, {-1, -1, 4}, {0, -1, 5}, {1, -1, 6}, {1, 0, 7}, {1, 1, 8},
                {0, 0, 9}, {0, 0, 10}};
        int[] flow = new int[2], nflow = new int[2];

        
        
        try (HMRaster modflowRandomIter = new HMRaster.HMRasterWritableBuilder().setTemplate(inFlow).setDoInteger(true)
                .build(); HMRaster flowFixed = HMRaster.fromGridCoverage(inFlownet)) {
            pm.beginTask("Correcting drainage directions...", rows);
            for( int j = 0; j < rows; j++ ) {
                if (pm.isCanceled()) {
                    return;
                }
                for( int i = 0; i < cols; i++ ) {
                    if (!flowFixed.isNovalue(flowFixed.getIntValue(i, j))) {
                        flow[0] = i;
                        flow[1] = j;
                        for( int k = 1; k <= 8; k++ ) {
                            nflow[0] = flow[0] + odir[k][1];
                            nflow[1] = flow[1] + odir[k][0];
                            if (modflowRandomIter.getIntValue(nflow[0], nflow[1]) == 0
                                    && flowFixed.isNovalue(flowFixed.getIntValue(nflow[0], nflow[1]))) {
                                double elev = pit.getValue(nflow[0] + odir[1][1], nflow[1] + odir[1][0]);
                                for( int n = 2; n <= 8; n++ ) {
                                    if (nflow[0] + odir[n][0] >= 0 && nflow[0] + odir[n][1] < rows && nflow[1] + odir[n][0] >= 0
                                            && nflow[1] + odir[n][0] < cols) {
                                        double tmpElev = pit.getValue(nflow[0] + odir[n][1],
                                                nflow[1] + odir[n][0]);
                                        if (tmpElev >= elev) {
                                            elev = tmpElev;
                                            dir.setValue(nflow[0], nflow[1], odir[n][2]);
                                        }
                                    }
                                }
                                for( int s = 1; s <= 8; s++ ) {
                                    if (nflow[0] + odir[s][0] >= 0 && nflow[0] + odir[s][0] < rows && nflow[1] + odir[s][1] >= 0
                                            && nflow[1] + odir[s][1] < cols) {
                                        if (!flowFixed.isNovalue(
                                                flowFixed.getIntValue(nflow[0] + odir[s][1], nflow[1] + odir[s][0]))) {
                                            double tmpElev = pit.getValue(nflow[0] + odir[s][1],
                                                    nflow[1] + odir[s][0]);
                                            if (tmpElev <= elev) {
                                                elev = tmpElev;
                                                dir.setValue(nflow[0], nflow[1], odir[s][2]);
                                            }
                                        }
                                    }
                                }
                                modflowRandomIter.setValue(nflow[0], nflow[1], 1);
                            }

                        }
                    }
                    if (!flowFixed.isNovalue(flowFixed.getIntValue(i, j))) {
                        dir.setValue(i, j, flowFixed.getValue(i, j));
                    }
                }
                pm.worked(1);
            }
            pm.done();

        }
    }

}
