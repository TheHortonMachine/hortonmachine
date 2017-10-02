/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org (C) HydroloGIS -
 * www.hydrologis.com
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
package org.hortonmachine.gears.utils.math.functions;

import static java.lang.Math.exp;
import static java.lang.Math.log;

import org.hortonmachine.gears.i18n.GearsMessageHandler;

/**
 * 
 * Mathematical function R_F.
 * 
 * <p>
 * An equation that it is used to estimate the value of the rain time that generate the maximum discharge.
 * </p>
 * <p>
 * The parameters required by the functions in {@link #setParameters(double...)}
 * are:
 * <ul>
 * <li><b>known</b> the know value.</li>
 * <li><b>exponent</b> the value used as exponent.</li>
 * </ul>
 * 
 * @author Daniele Andreis
 */
public final class R_F implements ISingleArgmentFunction {

    private double known;
    private double exponent;
    /**
     * Message handler.
     */
    private final GearsMessageHandler msg = GearsMessageHandler.getInstance();

    /*
     * Valuta la funzione in forma implicita che lega n a r*, dove r = tp / k-
     * Mi valuta la 1 - n - f(r,No) = 0 che lega n, a No=L/k*c e r=tp/k, al fine
     * di determinare il tp* che da la massima tra le portate massimie.- La
     * funzione da errore se i parametri immessi non sono fisicamente
     * ammmissibili, ossia r < 0, No <= 0, n <= 0 o n >= 1.
     */
    public double getValue( double x ) {
        if (x >= 0 && exponent > 0 && known > 0 && known < 1) {
            return ((1 - known) - x * (1 - exp(x) / (exp(exponent) + exp(x) - 1))
                    / (exponent + x - log(exp(exponent) + exp(x) - 1)));
        } else {
            throw new ArithmeticException(msg.message("rf.error.wrongSign"));
        }
    }

    public void setParameters( double... params ) {
        known = params[0];
        exponent = params[1];
    }

}