/* This file is part of HortonMachine (http://www.hortonmachine.org)
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
package org.hortonmachine.hmachine.modules.statistics.kriging.old;

import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_fStationsZ_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_fStationsid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_inData_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_inStations_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_outResult_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_pCutoff_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSVARIOGRAM_pPath_DESCRIPTION;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.ModelsEngine;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;
import org.opengis.feature.simple.SimpleFeature;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

@Description(OMSVARIOGRAM_DESCRIPTION)
@Author(name = OMSVARIOGRAM_AUTHORNAMES, contact = OMSVARIOGRAM_AUTHORCONTACTS)
@Keywords(OMSVARIOGRAM_KEYWORDS)
@Label(OMSVARIOGRAM_LABEL)
@Name(OMSVARIOGRAM_NAME)
@Status(OMSVARIOGRAM_STATUS)
@License(OMSVARIOGRAM_LICENSE)
public class OmsVariogram extends HMModel {

    @Description(OMSVARIOGRAM_inStations_DESCRIPTION)
    @In
    public SimpleFeatureCollection inStations = null;

    @Description(OMSVARIOGRAM_fStationsid_DESCRIPTION)
    @In
    public String fStationsid = null;

    @Description(OMSVARIOGRAM_fStationsZ_DESCRIPTION)
    @In
    public String fStationsZ = null;

    @Description(OMSVARIOGRAM_inData_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inData = null;

    @Description(OMSVARIOGRAM_pPath_DESCRIPTION)
    @In
    public String pPath = null;

    @Description(OMSVARIOGRAM_pCutoff_DESCRIPTION)
    @In
    public double pCutoff;

    @Description(OMSVARIOGRAM_outResult_DESCRIPTION)
    @Out
    public double[][] outResult = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    @Execute
    public void process() throws Exception {

        List<Double> xStationList = new ArrayList<Double>();
        List<Double> yStationList = new ArrayList<Double>();
        List<Double> zStationList = new ArrayList<Double>();
        List<Double> hStationList = new ArrayList<Double>();

        /*
         * counter for the number of station with measured value !=0.
         */
        int n1 = 0;
        /*
         * Store the station coordinates and measured data in the array.
         */
        FeatureIterator<SimpleFeature> stationsIter = inStations.features();
        try {
            while( stationsIter.hasNext() ) {
                SimpleFeature feature = stationsIter.next();
                int id = ((Number) feature.getAttribute(fStationsid)).intValue();
                double z = 0;
                if (fStationsZ != null) {
                    try {
                        z = ((Number) feature.getAttribute(fStationsZ)).doubleValue();
                    } catch (NullPointerException e) {
                        pm.errorMessage(msg.message("kriging.noStationZ"));
                        throw new Exception(msg.message("kriging.noStationZ"));

                    }
                }
                Coordinate coordinate = ((Geometry) feature.getDefaultGeometry()).getCentroid().getCoordinate();
                double[] h = inData.get(id);
                if (h == null || isNovalue(h[0])) {
                    /*
                     * skip data for non existing stations, they are allowed.
                     * Also skip novalues.
                     */
                    continue;
                }
                if (Math.abs(h[0]) >= 0.0) { // TOLL
                    xStationList.add(coordinate.x);
                    yStationList.add(coordinate.y);
                    zStationList.add(z);
                    hStationList.add(h[0]);
                    n1 = n1 + 1;
                }

            }
        } finally {
            stationsIter.close();
        }

        int nStaz = xStationList.size();
        /*
         * The coordinates of the station points plus in last position a place
         * for the coordinate of the point to interpolate.
         */
        double[] xStation = new double[nStaz];
        double[] yStation = new double[nStaz];
        double[] zStation = new double[nStaz];
        double[] hStation = new double[nStaz];
        boolean areAllEquals = true;
        if (nStaz != 0) {
            xStation[0] = xStationList.get(0);
            yStation[0] = yStationList.get(0);
            zStation[0] = zStationList.get(0);
            hStation[0] = hStationList.get(0);
            double previousValue = hStation[0];

            for( int i = 1; i < nStaz; i++ ) {

                double xTmp = xStationList.get(i);
                double yTmp = yStationList.get(i);
                double zTmp = zStationList.get(i);
                double hTmp = hStationList.get(i);
                boolean doubleStation = ModelsEngine.verifyDoubleStation(xStation, yStation, zStation, hStation, xTmp, yTmp,
                        zTmp, hTmp, i, false);
                if (!doubleStation) {
                    xStation[i] = xTmp;
                    yStation[i] = yTmp;
                    zStation[i] = zTmp;
                    hStation[i] = hTmp;
                    if (areAllEquals && hStation[i] != previousValue) {
                        areAllEquals = false;
                    }
                    previousValue = hStation[i];
                }
            }
        }

        outResult = processAlgorithm(xStation, yStation, hStation, pCutoff);
        if (pPath != null && pPath.length() > 0) {
            FileWriter Rstatfile = new FileWriter(pPath);
            PrintWriter errestat = new PrintWriter(Rstatfile);
            for( int i = 0; i < outResult.length; i++ ) {
                for( int j = 0; j < outResult[0].length; j++ ) {
                    if (i == 0) {
                        errestat.print("Np" + " " + "Dist" + " " + "Gamma" + " " + "Moran" + " " + "Geary");
                        break;
                    }
                    // errestat.print(i+" ");
                    // System.out.print(i+" ");
                    // errestat.print(j+" ");
                    // System.out.print(j+" ");
                    errestat.print(outResult[i - 1][j] + " ");

                }
                errestat.println();
                System.out.println();
            }

            Rstatfile.close();
        }

    }

    public static double[][] processAlgorithm( double[] xcord, double ycoord[], double[] values, double Cutoffinput ) {

        double x1, x2, y1, y2;
        double dDifX, dDifY;
        double value;
        double mean = 0;
        double maxDist = 0;

        int Cutoff_divide = 15;
        double Cutoff;
        int iCount = xcord.length;
        double d[][] = new double[iCount][iCount];
        double x_max = xcord[0], y_max = ycoord[0], diagonale;
        double x_min = xcord[0], y_min = ycoord[0];
        for( int i = 1; i < iCount; i++ ) {
            // System.out.println(values[i]);
            // System.out.println(xcord[i]);
            // System.out.println(ycoord[i]);
            x_min = Math.min(x_min, xcord[i]);
            y_min = Math.min(y_min, ycoord[i]);
            x_max = Math.max(x_max, xcord[i]);
            y_max = Math.max(y_max, ycoord[i]);

        }

        diagonale = Math.sqrt((x_max - x_min) * (x_max - x_min) + (y_max - y_min) * (y_max - y_min));

        if (Cutoffinput == 0) {
            Cutoff = diagonale / 3;
        } else
            Cutoff = Cutoffinput;
        // System.out.println(Cutoff);
        for( int i = 0; i < iCount; i++ ) {
            x1 = xcord[i];
            y1 = ycoord[i];
            value = values[i];

            mean += value;

            for( int j = 0; j < iCount; j++ ) {

                x2 = xcord[j];
                y2 = ycoord[j];

                dDifX = x2 - x1;
                dDifY = y2 - y1;
                d[i][j] = Math.sqrt(dDifX * dDifX + dDifY * dDifY); // Teor
                                                                    // Pitagora

                maxDist = Math.max(maxDist, d[i][j]);

            }
        }

        mean /= (double) iCount; // media dei valori di pioggia
        double[][] risultato = calculate(Cutoff_divide, Cutoff, d, values, mean, maxDist);

        return risultato;

    } // chiusura metodo

    public static double[][] calculate( int num, double cutoff, double[][] matricedelledistanze, double[] values, double media,
            double maxdistanza ) {
        int i, j;
        int iClasses;
        int iClass;
        int[] iPointsInClass;
        double dSemivar;
        boolean bIsInClass[];
        double binAmplitude; // definisco binAmplitude
        double[] dDen;

        binAmplitude = cutoff / num;
        iClasses = (int) (maxdistanza / binAmplitude + 2); // numero di distanze
                                                           // e per ogni dist
                                                           // calcolo la
                                                           // semivar
        // System.out.println(binAmplitude);
        double[] m_dMoran = new double[iClasses];
        double[] m_dGeary = new double[iClasses];
        dDen = new double[iClasses]; // vettori che per ogni distanza
                                     // conterranno
        double[] m_dSemivar = new double[iClasses]; // la varianza, covarianza,
                                                    // semivarianza, numero
        iPointsInClass = new int[iClasses]; // numero di punti nella classe spec
                                            // di dist
        bIsInClass = new boolean[iClasses];
        double[] m_ddist = new double[iClasses];

        for( i = 0; i < matricedelledistanze.length; i++ ) {
            Arrays.fill(bIsInClass, false); // riempio il vettore buleano
                                            // bisinclass di false
            double value1 = values[i]; // valori di pioggia del primo ciclo

            for( j = i + 1; j < matricedelledistanze.length; j++ ) {
                if (matricedelledistanze[i][j] > 0 && matricedelledistanze[i][j] < cutoff) {

                    iClass = (int) Math.floor((matricedelledistanze[i][j]) / binAmplitude); // ritorna
                                                                                            // a
                                                                                            // quale
                                                                                            // classe
                                                                                            // di
                                                                                            // distanza
                                                                                            // appartiene
                                                                                            // quella
                                                                                            // in
                                                                                            // oggetto

                    iPointsInClass[iClass]++; // conta i numeri di distanze per
                                              // ogni tipo di distanze
                    double value2 = values[j]; // val di pioggia del secondo
                                               // ciclo
                    dSemivar = Math.pow((value1 - value2), 2.); // calcolo la
                                                                // semivarianza

                    m_dSemivar[iClass] += dSemivar; // la varianza va a sommare
                                                    // tutte le varianze della
                                                    // classe di distanza
                    m_dMoran[iClass] += (value1 - media) * (value2 - media); // somma
                                                                             // delle
                                                                             // covarianze
                                                                             // della
                                                                             // classe
                                                                             // di
                                                                             // distanza
                    m_dGeary[iClass] = m_dSemivar[iClass]; // inserita la
                                                           // somma delle
                                                           // semivarianze
                                                           // della distanza
                    bIsInClass[iClass] = true; // per la dist in questione si
                                               // inserisce true

                    m_ddist[iClass] += matricedelledistanze[i][j]; // +
                                                                   // binAmplitude
                                                                   // / 2. /
                                                                   // binAmplitude;

                }
            }

            for( j = 0; j < iClasses; j++ ) {
                if (bIsInClass[j]) { // se alla dist j stato dato true
                    dDen[j] += Math.pow(value1 - media, 2.); // calcolo la
                                                             // varianza e
                                                             // alla dist j
                                                             // ne sommo e
                                                             // assegno tutte

                }
            }

        }
        double[][] result = new double[iClasses][5];
        int contaNONzero = 0;
        for( i = 0; i < iClasses; i++ ) {
            if (dDen[i] != 0) {
                contaNONzero += 1;
                m_dMoran[i] /= dDen[i]; // divide la somma delle covarianze
                                        // della dist i con la somma delle
                                        // varianze
                m_dGeary[i] *= ((iPointsInClass[i] - 1) / (2. * iPointsInClass[i] * dDen[i])); // n-1/2n(var)
                                                                                               // n=numero
                                                                                               // di
                                                                                               // punti
                                                                                               // che
                                                                                               // hanno
                                                                                               // la
                                                                                               // dist
                                                                                               // i
                m_dSemivar[i] /= (2. * iPointsInClass[i]); // semivarianza

                m_ddist[i] /= iPointsInClass[i];

                result[i][0] = iPointsInClass[i];
                result[i][1] = m_ddist[i];
                result[i][2] = m_dSemivar[i];
                result[i][3] = m_dMoran[i];
                result[i][4] = m_dGeary[i];

            }
        }
        double results[][] = new double[contaNONzero][5];
        for( int ii = 0; ii < contaNONzero; ii++ ) {
            results[ii][0] = result[ii][0];
            results[ii][1] = result[ii][1];
            results[ii][2] = result[ii][2];
            results[ii][3] = result[ii][3];
            results[ii][4] = result[ii][4];

        }
        return results;

    }

}