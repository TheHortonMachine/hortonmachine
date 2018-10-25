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
 * This is adapted from: http://www.cse.unsw.edu.au/~lambert/splines/Bspline.html
 * 
 * @author Tim Lambert (http://www.cse.unsw.edu.au/~lambert/)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Bspline extends ControlCurve {

    // the basis function for a cubic B spline
    double b( int i, double t ) {
        switch( i ) {
        case -2:
            return (((-t + 3) * t - 3) * t + 1) / 6;
        case -1:
            return (((3 * t - 6) * t) * t + 4) / 6;
        case 0:
            return (((-3 * t + 3) * t + 3) * t + 1) / 6;
        case 1:
            return (t * t * t) / 6;
        }
        return 0; // we only get here if an invalid i is specified
    }

    // evaluate a point on the B spline
    private Coordinate p( int i, double t ) {
        double px = 0;
        double py = 0;
        for( int j = -2; j <= 1; j++ ) {
            Coordinate coordinate = pts.get(i + j);
            px += b(j, t) * coordinate.x;
            py += b(j, t) * coordinate.y;
        }
        return new Coordinate(px, py);
    }

    final int STEPS = 12;

    public List<Coordinate> getInterpolated() {
        List<Coordinate> interpolatedCoordinates = new ArrayList<Coordinate>();
        Coordinate q = p(2, 0);
        interpolatedCoordinates.add(q);
        for( int i = 2; i < pts.size() - 1; i++ ) {
            for( int j = 1; j <= STEPS; j++ ) {
                q = p(i, j / (double) STEPS);
                interpolatedCoordinates.add(q);
            }
        }
        return interpolatedCoordinates;
    }

}
