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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.jeff;

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.ParameterBox;
/**
 * @author Silvia Franceschi (www.hydrologis.com)
 * @author Andrea Antonello (www.hydrologis.com)
 * 
 * @deprecated THIS IS NOT WORKING!!! It is here just for future reference.
 */
public class SCSJeff {

    private double[][] Jeff = null;

    /**
    * 
    */
    public SCSJeff( double[][] cnMapData, float[][] raindata, float deltat, ParameterBox paramBox,
            double nsres, double ewres ) {
        double[][] cnMapnew = new double[cnMapData.length][cnMapData[0].length];
        double[][] Smap = new double[cnMapData.length][cnMapData[0].length];

        double phi = paramBox.getPhi();

        for( int i = 0; i < cnMapData.length; i++ ) {
            for( int j = 0; j < cnMapData[0].length; j++ )

            {
                if (!isNovalue(cnMapData[i][j])) {
                    // in base alla scelta fatta dall'utente mi calcolo la nuova
                    // matrice dei CN,chiamata cnMapnew
                    switch( paramBox.getBasinstate() ) {
                    case 1:
                        cnMapnew[i][j] = ((4.2 * cnMapData[i][j]) / (10.0 - 0.058 * cnMapData[i][j]));
                        break;
                    case 2:
                        cnMapnew[i][j] = cnMapData[i][j];
                        break;
                    case 3:
                        cnMapnew[i][j] = ((23.0 * cnMapData[i][j]) / (10.0 + 0.13 * cnMapData[i][j]));
                        break;
                    default:
                        break;
                    }

                    Smap[i][j] = (1000.0 / cnMapnew[i][j] - 10.0) * 25.4;
                } else {
                    cnMapnew[i][j] = doubleNovalue;
                    Smap[i][j] = doubleNovalue;
                }
            }
        }

        // definisco i vettori dell'altezza di pioggia dell'intensità e
        // dello scorrimento superficiale.
        float[][] Jtot = new float[raindata.length][2];

        /*
         * Jtot[0][0] = 0f; Jtot[0][1] = 0f;
         */

        double[][] RsActual = new double[cnMapData.length][cnMapData[0].length];
        double[][] RsPrec = new double[cnMapData.length][cnMapData[0].length];
        double[][] JeffSup = new double[cnMapData.length][cnMapData[0].length];
        double[][] JeffSub = new double[cnMapData.length][cnMapData[0].length];
        double[][] Psup = new double[cnMapData.length][cnMapData[0].length];
        double[][] Psub = new double[cnMapData.length][cnMapData[0].length];
        Jeff = new double[raindata.length][3];

        double converter = 1 / (1000.0 * 3600.0);

        float t = raindata[1][0];
        for( int k = 0; k < raindata.length; k++, t = t + deltat ) {
            double j1 = 0.0;
            double j2 = 0.0;
            int activecells = 0;

            Jtot[k][0] = raindata[k][0];
            if (k == 0) {
                Jtot[k][1] = raindata[k][1] / deltat;
            } else {
                Jtot[k][1] = Jtot[k - 1][1] + (raindata[k][1] / deltat);
            }

            for( int i = 0; i < cnMapnew.length; i++ ) {
                for( int j = 0; j < cnMapnew[0].length; j++ ) {
                    if (!isNovalue(cnMapnew[i][j])) {
                        // Impongo le condizioni affinchè il calcolo sia positivo.
                        if ((Jtot[k][1] * deltat) <= (0.2 * Smap[i][j])) {
                            RsActual[i][j] = 0.0;
                        } else {
                            RsActual[i][j] = (Math.pow((Jtot[k][1] * deltat - 0.2 * Smap[i][j]),
                                    2.0) / (Jtot[k][1] * deltat + 0.8 * Smap[i][j]));
                        }

                        JeffSup[i][j] = (RsActual[i][j] - RsPrec[i][j]) / deltat;
                        JeffSub[i][j] = ((raindata[k][1] / deltat) - JeffSup[i][j]) * phi;
                        /*
                         * if (raindata[k][1] == 0 && JeffSup[i][j] != 0) {
                         * System.out.println("bau"); }
                         */

                        Psup[i][j] = JeffSup[i][j] * converter;
                        Psub[i][j] = JeffSub[i][j] * converter;

                        j1 = j1 + Psup[i][j];
                        j2 = j2 + Psub[i][j];
                        // j3 = j3 + raindata[k][1] * converter;
                        activecells++;
                    }
                }
            }

            Jeff[k][0] = k * 3600;

            Jeff[k][1] = j1 / activecells;
            Jeff[k][2] = j2 / activecells;

            /*
             * if ((Jeff[k][1] + Jeff[k][2]) > 0.000009 || (Jeff[k][1] + Jeff[k][2]) >
             * (raindata[k][1] / (deltat activecells))) { JOptionPane.showMessageDialog((JFrame)
             * GrassEnvironmentManager .getInstance().getGuiParentFrame(),
             * "Error in Jeff sum: Jeffsup + Jeffsub = Jefftot   gives " + Jeff[k][1] + " + " +
             * Jeff[k][2] + " > " + (raindata[k][1] / activecells) + "!", "Jeff Error",
             * JOptionPane.ERROR_MESSAGE); }
             */

            for( int i = 0; i < RsActual.length; i++ ) {
                for( int j = 0; j < RsActual[0].length; j++ ) {
                    RsPrec[i][j] = RsActual[i][j];
                }
            }
        }

        // if (paramBox.getFileToDump() != null) {
        // TODO dump to file
        // DataSource outfile1 = new DataSource("file:" + paramBox.getOutputFile() +
        // "_Jeff_sup");
        // DataSource outfile2 = new DataSource("file:" + paramBox.getOutputFile() +
        // "_Jeff_sub");
        // OutputStreamWriter writeoutfile1 = outfile1.getOutputStreamWriter();
        // OutputStreamWriter writeoutfile2 = outfile2.getOutputStreamWriter();
        // try {
        // for( int m = 0; m < Jeff.length; m++ ) {
        // writeoutfile1.write(Jeff[m][0] + "       " + Jeff[m][1] + "\n");
        // writeoutfile2.write(Jeff[m][0] + "       " + Jeff[m][2] + "\n");
        // }
        // writeoutfile1.close();
        // writeoutfile2.close();
        // } catch (IOException e) {
        // // TODO Auto-generated catch block
        // e.printStackTrace();
        // }
        // }
    }

    /*
     * (non-Javadoc)
     * @see bsh.commands.h.peakflow.jeff.JeffCalculator#calculateJeff()
     */
    public double[][] calculateJeff() {
        return Jeff;
    }

}
