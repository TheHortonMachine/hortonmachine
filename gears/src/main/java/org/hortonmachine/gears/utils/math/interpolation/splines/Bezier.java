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
 * This is adapted from: http://www.cse.unsw.edu.au/~lambert/splines/Bezier.html
 * 
 * @author Tim Lambert (http://www.cse.unsw.edu.au/~lambert/)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Bezier extends ControlCurve {

    // the basis function for a Bezier spline
    private static double b( int i, double t ) {
        switch( i ) {
        case 0:
            return (1 - t) * (1 - t) * (1 - t);
        case 1:
            return 3 * t * (1 - t) * (1 - t);
        case 2:
            return 3 * t * t * (1 - t);
        case 3:
            return t * t * t;
        }
        return 0; // we only get here if an invalid i is specified
    }

    // evaluate a point on the B spline
    private Coordinate p( int i, double t ) {
        double px = 0;
        double py = 0;
        for( int j = 0; j <= 3; j++ ) {
            Coordinate c = pts.get(i + j);
            px += b(j, t) * c.x;
            py += b(j, t) * c.y;
        }
        return new Coordinate(px, py);
    }

    final int STEPS = 12;

    public List<Coordinate> getInterpolated() {
        List<Coordinate> coordList = new ArrayList<Coordinate>();
        Coordinate q = p(0, 0);
        coordList.add(q);
        for( int i = 0; i < pts.size() - 3; i += 3 ) {
            for( int j = 1; j <= STEPS; j++ ) {
                q = p(i, j / (double) STEPS);
                coordList.add(q);
            }
        }
        return coordList;
    }

}
