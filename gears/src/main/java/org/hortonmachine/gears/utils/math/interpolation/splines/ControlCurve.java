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
 * This class represents a curve defined by a sequence of control points 
 * 
 * @author Tim Lambert (http://www.cse.unsw.edu.au/~lambert/)
 * @author Andrea Antonello (www.hydrologis.com)
 */
public abstract class ControlCurve {

    public static final int EPSILON = 36; /* square of distance for picking */
    protected List<Coordinate> pts;
    protected int selection = -1;

    public ControlCurve() {
        pts = new ArrayList<Coordinate>();
    }

    /** 
     * return index of control point near to (x,y) or -1 if nothing near 
     */
    public int selectPoint( double x, double y ) {
        double mind = Double.POSITIVE_INFINITY;
        selection = -1;
        for( int i = 0; i < pts.size(); i++ ) {
            Coordinate coordinate = pts.get(i);

            double d = sqr(coordinate.x - x) + sqr(coordinate.y - y);
            if (d < mind && d < EPSILON) {
                mind = d;
                selection = i;
            }
        }
        return selection;
    }

    static double sqr( double x ) {
        return x * x;
    }

    /** add a control point, return index of new control point */
    public double addPoint( double x, double y ) {
        pts.add(new Coordinate(x, y));
        return selection = pts.size() - 1;
    }

    /** set selected control point */
    public void setPoint( double x, double y ) {
        if (selection >= 0) {
            Coordinate coordinate = new Coordinate(x, y);
            pts.set(selection, coordinate);
        }
    }

    /** remove selected control point */
    public void removePoint() {
        if (selection >= 0) {

            pts.remove(selection);
            // pts.npoints--;
            // for( int i = selection; i < pts.npoints; i++ ) {
            // pts.xpoints[i] = pts.xpoints[i + 1];
            // pts.ypoints[i] = pts.ypoints[i + 1];
            // }
        }
    }
    
    public abstract List<Coordinate> getInterpolated();

    public String toString() {
        StringBuffer result = new StringBuffer();
        for( int i = 0; i < pts.size(); i++ ) {
            result.append(" " + pts.get(i));
        }
        return result.toString();
    }
}
