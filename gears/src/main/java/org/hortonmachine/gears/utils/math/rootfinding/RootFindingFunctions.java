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
package org.hortonmachine.gears.utils.math.rootfinding;

import org.hortonmachine.gears.i18n.GearsMessageHandler;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.utils.math.functions.ISingleArgmentFunction;

/**
 * Methods to find roots of a function.
 * 
 * @author Daniele Andreis
 */
public class RootFindingFunctions {
    
    private static GearsMessageHandler msg = GearsMessageHandler.getInstance();
    
    /**
     * 
     * Evaluate the root of a function.
     * 
     * <p>
     * 
     * Use the bisection method. note that to use before to use this method it's
     * necessary to have a special function object which is defined by the
     * interface {@link Function}.
     * 
     * <ol>
     * <li>Implements the bisection method an return the solution with a
     * particular accuracy.
     * <li>return an error if the function doesn't converge to the solution in
     * JMAX loop.
     * </ol>
     * 
     * @param function is the function examined.
     * @param bottomLimit bottom left value of the x.
     * @param upperLimit upper right value of the x.
     * @param accuracy accuracy to use in the evaluation.
     * @param maxIterationNumber maximum number of iteration.
     * @throws ArithmeticException if the method haven't achieve the solution in
     *         jMax steeps.
     * 
     * @return the solution of the equation.
     */
    public static double bisectionRootFinding( ISingleArgmentFunction function, double bottomLimit, double upperLimit, double accuracy, double maxIterationNumber, IHMProgressMonitor pm ) {
        /* Numero di iterazioni eseguite col metodo delle bisezioni */
        long j;
        /* Ampiezza dell'intervallo di ricerca della radice */
        double dx;
        /*
         * Funzione di cui si cerca la radice valutata in corrispondenza
         * dell'estremo inferiore, fisso.
         */
        double f;
        /*
         * Funzione di cui si cerca la radice valutata in corrispondenza
         * dell'estremo superiore mobile.
         */
        double fmid;
        /*
         * Estremo superiore mobile dell'intervallo in cui si cercano le radici
         * cercate.
         */
        double xmid;
        /* La radice calcolata con la precisione richiesta */
        double rtb;
        /*
         * chiamata alla funzione di cui si cerca la radice,nell'estremo
         * inferiore.
         */
        f = function.getValue(bottomLimit);
        /*
         * chiamata alla funzione di cui si cerca la radice nell'estremo
         * superiore del bracketing.
         */
        fmid = function.getValue(upperLimit);
        /* verifica se il bracketing della radice e corretto */
        if (f * fmid >= 0) {
            pm.errorMessage(msg.message("trentoP.error.braketed"));
            throw new ArithmeticException(msg.message("trentoP.error.braketed"));
        }
        if (f < 0) {
            dx = upperLimit - bottomLimit;
            rtb = bottomLimit;
        } else {
            dx = bottomLimit - upperLimit;
            rtb = upperLimit;
        }
        /*
         * se f< 0, mantengo il primo estremo fisso in x1, e sposto il secondo
         * estremo a sisnitra finche la f in tale punto non e minore di 0.
         * Altrimenti fisso il secondo estremo e muovo il primo estremo a destra
         * finche la f diventa negativa.
         */

        for( j = 1; j <= maxIterationNumber; j++ ) {
            fmid = function.getValue(xmid = rtb + (dx *= 0.5));
            /*
             * quando la f cambia segno rispetto all'estremo di riferimento, il
             * valore corrente dell'estremo mobile e la radice cercata
             */
            if (fmid <= 0)
                rtb = xmid;
            /*
             * restituisci la rdice se l'intervallo di ricerca e minore della
             * tolleranza prefissata, oppure se f=0
             */
            if (Math.abs(dx) < accuracy || fmid == 0)
                return rtb;
        }
        /*
         * Se il numero delle bisezioni supera il numero massimo ammesso (40),
         * senza arrivare a una convergenza dei risultati, il programma da
         * errore
         */
        pm.errorMessage(msg.message("trentoP.error.bisection"));
        throw new ArithmeticException(msg.message("trentoP.error.bisection"));

    }

}
