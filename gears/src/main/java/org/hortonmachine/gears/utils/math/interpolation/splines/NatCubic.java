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
package org.hortonmachine.gears.utils.math.interpolation.splines;
import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;

/**
 * This is adapted from: http://www.cse.unsw.edu.au/~lambert/splines/natcubic.html
 * 
 * @author Tim Lambert (http://www.cse.unsw.edu.au/~lambert/)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class NatCubic extends ControlCurve {

    /* calculates the natural cubic spline that interpolates
    y[0], y[1], ... y[n]
    The first segment is returned as
    C[0].a + C[0].b*u + C[0].c*u^2 + C[0].d*u^3 0<=u <1
    the other segments are in C[1], C[2], ...  C[n-1] */

    public Cubic[] calcNaturalCubic( int n, double[] x ) {
        double[] gamma = new double[n + 1];
        double[] delta = new double[n + 1];
        double[] D = new double[n + 1];
        int i;
        /* We solve the equation
           [2 1       ] [D[0]]   [3(x[1] - x[0])  ]
           |1 4 1     | |D[1]|   |3(x[2] - x[0])  |
           |  1 4 1   | | .  | = |      .         |
           |    ..... | | .  |   |      .         |
           |     1 4 1| | .  |   |3(x[n] - x[n-2])|
           [       1 2] [D[n]]   [3(x[n] - x[n-1])]
           
           by using row operations to convert the matrix to upper triangular
           and then back sustitution.  The D[i] are the derivatives at the knots.
           */

        gamma[0] = 1.0f / 2.0f;
        for( i = 1; i < n; i++ ) {
            gamma[i] = 1 / (4 - gamma[i - 1]);
        }
        gamma[n] = 1 / (2 - gamma[n - 1]);

        delta[0] = 3 * (x[1] - x[0]) * gamma[0];
        for( i = 1; i < n; i++ ) {
            delta[i] = (3 * (x[i + 1] - x[i - 1]) - delta[i - 1]) * gamma[i];
        }
        delta[n] = (3 * (x[n] - x[n - 1]) - delta[n - 1]) * gamma[n];

        D[n] = delta[n];
        for( i = n - 1; i >= 0; i-- ) {
            D[i] = delta[i] - gamma[i] * D[i + 1];
        }

        /* now compute the coefficients of the cubics */
        Cubic[] C = new Cubic[n];
        for( i = 0; i < n; i++ ) {
            C[i] = new Cubic((double) x[i], D[i], 3 * (x[i + 1] - x[i]) - 2 * D[i] - D[i + 1], 2 * (x[i] - x[i + 1]) + D[i]
                    + D[i + 1]);
        }
        return C;
    }

    final int STEPS = 12;

    /* draw a cubic spline */
    public List<Coordinate> getInterpolated() {
        List<Coordinate> p = new ArrayList<Coordinate>();
        if (pts.size() >= 2) {
            double[] xs = new double[pts.size()];
            double[] ys = new double[pts.size()];
            for( int i = 0; i < xs.length; i++ ) {
                Coordinate coordinate = pts.get(i);
                xs[i] = coordinate.x;
                ys[i] = coordinate.y;
            }

            Cubic[] X = calcNaturalCubic(pts.size() - 1, xs);
            Cubic[] Y = calcNaturalCubic(pts.size() - 1, ys);

            /* very crude technique - just break each segment up into steps lines */
            p.add(new Coordinate(X[0].eval(0), Y[0].eval(0)));
            for( int i = 0; i < X.length; i++ ) {
                for( int j = 1; j <= STEPS; j++ ) {
                    double u = j / (double) STEPS;
                    p.add(new Coordinate(X[i].eval(u), Y[i].eval(u)));
                }
            }
        }
        return p;
    }
}
