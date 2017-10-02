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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo;

/**
 * Ported from C fluidturtles.
 *
 */
public class LinearAlgebra {

    private final double NRANSI = -1;
    private final double THRESH = 0;
    private final int ITOL = 3;
    private final double TOL = 0.00001;
    private final int ITMAX = 1000;
    private final double EPS = 1.0e-14;
    private final double TINY = 1.0E-20;

    public void ris_sistema( double d[], double ds[], double di[], double b[], double x[], int n ) {
        /* Alloco una matrice normale a partire da tre vettori, uno per diagonale */
        double[][] A = vett_mat(d, ds, di, n);
        /*
         * Alloco i vettori che memorizzano la matrice tridiagonale come una matrice sparsa alla
         * maniera di N.R.
         */
        double[] sa = new double[((3 * n) - 1)];
        int[] ija = new int[((3 * n) - 1)];

        /* Definisco gli elementi dei vettori sa[] e ija[] */
        sprsin(A, THRESH, sa, ija);

        /* Calcolo la soluzione del sistema lineare */
        linbcg(n, b, x, ITOL, TOL, ITMAX, sa, ija);
    }

    /**
     * Questa funzione converte tre vettori in una matrice quadrata tridiagonale.
     * 
     * @param d elementi delle diagonale principale
     * @param ds elementi della diagonale superiore
     * @param di elementi della diagonale inferiore
     * @param n dimensione della matrice che si vuole generare
     * @return matrix
     */
    private double[][] vett_mat( double[] d, double[] ds, double[] di, int n ) {
        double[][] A = new double[n][n];
        for( int i = 0; i < n; i++ ) {
            for( int j = 0; j < n; j++ )
                A[i][j] = 0.0;
        }
        A[0][1] = ds[0];
        A[n - 1][n - 2] = di[n - 2];
        for( int i = 0; i < n; i++ )
            A[i][i] = d[i];
        for( int i = 1; i < n - 1; i++ ) {
            A[i][i + 1] = ds[i];
            A[i][i - 1] = di[i - 1];
        }
        return A;
    }

    /**
     * <pre>
     *     Questa funzione converte una matrice memorizzata nel modo convenzionale
     *     in un vettore sa[] che contiene solo i valori non nulli della matrice
     *     e in un vettore ija[] che permette di individuare la posizione originale
     *     degli elementi di sa[].
     *     
     *     La funzione richiede:
     *     - **a, un puntatore agli elementi della matrice originale;
     *     - n, dimensione della matrice;
     *     - thresh, gli elementi della matrice minori di thresh non vengono
     *               letti;
     *     - nmax, la lunghezza dei vettori sa[] e ija[].
     * </pre>
     */
    public void sprsin( double[][] a, double thresh, double[] sa, int[] ija ) {
        int k;
        int n = a.length;
        int nmax = (3 * n) - 1;
        for( int j = 0; j < n; j++ )
            sa[j] = a[j][j];
        ija[0] = n + 1;
        k = n;
        for( int i = 0; i < n; i++ ) {
            for( int j = 0; j < n; j++ ) {
                if (Math.abs(a[i][j]) > thresh && i != j) {
                    if (++k > nmax)
                        System.out.println("sprsin: nmax too small"); //$NON-NLS-1$
                    sa[k] = a[i][j];
                    ija[k] = j;
                }
            }
            ija[i + 1] = k + 1;
        }
    }

    /**
     * <pre>
     *  Questa funzione consente di risolvere di risolvere un sistema lineare del tipo
     *     A x = b con il metodo iterativo del gradiente coniugato.
     *     
     *     Alla funzione devono essere passati i seguenti argomenti:
     *     - n: dimensione del sistema;
     *     - sa[] e ija[]: vettori generati dalla funzione sprssin() che memorizzano la 
     *       matrice;
     *     - b[]: elementi del vettore dei termini noti;
     *     - x[]: elementi del vettore soluzione (in ingresso questo vettore deve contenere
     *       una soluzione di primo tentativo);
     *     - itol, tol, itmax: parametri gli definiti sopra.
     *     
     *     Oltre alla soluzione la funzione calcola anche il numero di iterazione effetuate
     *     ( iter ) e l'errore commesso ( err ).
     * </pre>
     */
    private void linbcg( int n, double b[], double x[], int itol, double tol, int itmax, double sa[], int ija[] ) {
        // FIXME check itol and those numbers that start from 1 in here
        double ak, akden, bk, bkden = 0, bknum, bnrm = 0, dxnrm, xnrm, zm1nrm, znrm = 0;

        double[] p = new double[n];
        double[] pp = new double[n];
        double[] r = new double[n];
        double[] rr = new double[n];
        double[] z = new double[n];
        double[] zz = new double[n];

        int iter = 0;
        double err = 0;
        atimes(n, x, r, false, sa, ija);
        for( int j = 0; j < n; j++ ) {
            r[j] = b[j] - r[j];
            rr[j] = r[j];
        }
        if (itol == 1) {
            bnrm = snrm(n, b, itol);
            asolve(n, r, z, sa);
        } else if (itol == 2) {
            asolve(n, b, z, sa);
            bnrm = snrm(n, z, itol);
            asolve(n, r, z, sa);
        } else if (itol == 3 || itol == 4) {
            asolve(n, b, z, sa);
            bnrm = snrm(n, z, itol);
            asolve(n, r, z, sa);
            znrm = snrm(n, z, itol);
        } else
            System.out.println("illegal itol in linbcg"); //$NON-NLS-1$

        while( iter <= itmax ) {
            ++iter;
            asolve(n, rr, zz, sa);
            bknum = 0.0;
            for( int j = 0; j < n; j++ )
                bknum += z[j] * rr[j];
            if (iter == 1) {
                for( int j = 0; j < n; j++ ) {
                    p[j] = z[j];
                    pp[j] = zz[j];
                }
            } else {
                bk = bknum / bkden;
                for( int j = 0; j < n; j++ ) {
                    p[j] = bk * p[j] + z[j];
                    pp[j] = bk * pp[j] + zz[j];
                }
            }
            bkden = bknum;
            atimes(n, p, z, false, sa, ija);
            akden = 0.0;
            for( int j = 0; j < n; j++ )
                akden += z[j] * pp[j];
            ak = bknum / akden;
            atimes(n, pp, zz, true, sa, ija);
            for( int j = 0; j < n; j++ ) {
                x[j] += ak * p[j];
                r[j] -= ak * z[j];
                rr[j] -= ak * zz[j];
            }
            asolve(n, r, z, sa);
            if (itol == 1)
                err = snrm(n, r, itol) / bnrm;
            else if (itol == 2)
                err = snrm(n, z, itol) / bnrm;
            else if (itol == 3 || itol == 4) {
                zm1nrm = znrm;
                znrm = snrm(n, z, itol);
                if (Math.abs(zm1nrm - znrm) > EPS * znrm) {
                    dxnrm = Math.abs(ak) * snrm(n, p, itol);
                    err = znrm / Math.abs(zm1nrm - znrm) * dxnrm;
                } else {
                    err = znrm / bnrm;
                    continue;
                }
                xnrm = snrm(n, x, itol);
                if (err <= 0.5 * xnrm)
                    err /= xnrm;
                else {
                    err = znrm / bnrm;
                    continue;
                }
            }
            if (err <= tol)
                break;
        }
    }
    /**
     * Questa funzione calcola la norma di un vettore con la modalita' specificata dal parametro
     * itol
     */
    private double snrm( int n, double sx[], int itol ) {
        int isamax;
        double ans;

        if (itol <= 3) {
            ans = 0.0;
            for( int i = 0; i < n; i++ )
                ans += sx[i] * sx[i];
            return Math.sqrt(ans);
        } else {
            isamax = 0;
            for( int i = 0; i < n; i++ ) {
                if (Math.abs(sx[i]) > Math.abs(sx[isamax]))
                    isamax = i;
            }
            return Math.abs(sx[isamax]);
        }
    }

    /**
     * DA NUMERICAL RECEPIS IN C. (Second Edition - Cambridge Univ. Press). pag 88
     * _________________________________________________________________________________
     * ================================================================================= FUNZIONE
     * atimes _________________________________________________________________________________
     */
    private void atimes( int n, double x[], double r[], boolean itrnsp, double sa[], int[] ija ) {
        if (itrnsp)
            dsprstx(sa, ija, x, r, n);
        else
            dsprsax(sa, ija, x, r, n);
    }

    /**
     * DA NUMERICAL RECEPIS IN C. (Second Edition - Cambridge Univ. Press). pag 89 FUNZIONE asolve
     */
    private void asolve( int n, double b[], double x[], double sa[] ) {
        for( int i = 0; i < n; i++ )
            x[i] = (sa[i] != 0.0 ? b[i] / sa[i] : b[i]);
    }

    /**
     * Questa funzione moltiplica una matrice, memoririzzata alla maniera di N.R., per un vettore
     * x[]. Il risultato e' un vettore b[].
     */
    private void dsprsax( double sa[], int ija[], double x[], double b[], int n ) {
        if (ija[0] != n + 1)
            System.out.println("dsprsax: mismatched vector and matrix"); //$NON-NLS-1$
        for( int i = 0; i < n; i++ ) {
            b[i] = sa[i] * x[i];
            for( int k = ija[i]; k <= ija[i + 1] - 1; k++ ) {
                b[i] = b[i] + sa[k] * x[ija[k]];
            }
        }
    }

    /*
     * Questa funzione moltiplica la trasposta di una matrice, memoririzzata alla maniera di N.R.,
     * per un vettore x[]. Il risultato e' un vettore b[].
     */
    private void dsprstx( double sa[], int ija[], double x[], double b[], int n ) {
        if (ija[0] != n + 1)
            System.out.println("mismatched vector and matrix in dsprstx"); //$NON-NLS-1$
        for( int i = 0; i < n; i++ )
            b[i] = sa[i] * x[i];
        for( int i = 0; i < n; i++ ) {
            for( int k = ija[i]; k <= ija[i + 1] - 1; k++ ) {
                int j = ija[k];
                b[j] = b[j] + sa[k] * x[i];
            }
        }
    }
}