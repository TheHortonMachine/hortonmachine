/*
 * GNU GPL v3 License
 *
 * Copyright 2016 Marialaura Bancheri
 *
 * This program is free software: you can redistribute it and/or modify
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

package org.hortonmachine.hmachine.modules.statistics.kriging.variogram.theoretical;

public class Bessel implements Model {

    double dist;
    double sill;
    double range;
    double nug;

    public Bessel( double dist, double sill, double range, double nug ) {
        this.dist = dist;
        this.sill = sill;
        this.range = range;
        this.nug = nug;
    }

    @Override
    public double computeSemivariance() {
        double hr;
        double MIN_BESS = 1.0e-3;
        double result = 0;

        hr = dist / range;
        if (hr > MIN_BESS) {
            result = nug + sill * (1.0 - hr * bessk1(hr));
        }

        return result;
    }

    static double bessk1( double x ) {
        double y, ans;

        if (x <= 2.0) {
            y = x * x / 4.0;
            ans = (Math.log(x / 2.0) * bessi1(x)) + (1.0 / x) * (1.0 + y * (0.15443144
                    + y * (-0.67278579 + y * (-0.18156897 + y * (-0.1919402e-1 + y * (-0.110404e-2 + y * (-0.4686e-4)))))));
        } else {
            y = 2.0 / x;
            ans = (Math.exp(-x) / Math.sqrt(x)) * (1.25331414 + y * (0.23498619
                    + y * (-0.3655620e-1 + y * (0.1504268e-1 + y * (-0.780353e-2 + y * (0.325614e-2 + y * (-0.68245e-3)))))));
        }
        return (double) ans;
    }

    static double bessi1( double x ) {
        double ax, ans;
        double y;

        if ((ax = Math.abs(x)) < 3.75) {
            y = x / 3.75;
            y *= y;
            ans = ax * (0.5 + y * (0.87890594
                    + y * (0.51498869 + y * (0.15084934 + y * (0.2658733e-1 + y * (0.301532e-2 + y * 0.32411e-3))))));
        } else {
            y = 3.75 / ax;
            ans = 0.2282967e-1 + y * (-0.2895312e-1 + y * (0.1787654e-1 - y * 0.420059e-2));
            ans = 0.39894228 + y * (-0.3988024e-1 + y * (-0.362018e-2 + y * (0.163801e-2 + y * (-0.1031555e-1 + y * ans))));
            ans *= (Math.exp(ax) / Math.sqrt(ax));
        }
        return (double) x < 0.0 ? -ans : ans;
    }

}
