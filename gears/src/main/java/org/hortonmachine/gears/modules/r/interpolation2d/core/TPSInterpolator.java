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
package org.hortonmachine.gears.modules.r.interpolation2d.core;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.geotools.referencing.operation.matrix.GeneralMatrix;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.locationtech.jts.geom.Coordinate;

/**
 * Implementation of TPS Interpolation based on thin plate spline (TPS) algorithm
 *
 * <p>TPS developed following: http://elonen.iki.fi/code/tpsdemo/index.html
 *  
 * <p>The implementation is meant to be threadsafe.
 * 
 * <p><b>Note that this implementation works only with metric data.</b>
 *
 * @author jezekjan
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class TPSInterpolator implements ISurfaceInterpolator {

    private double maxDistance;
    private double minDistance;
    private boolean useBuffer = false;

    public TPSInterpolator() {
    }

    public TPSInterpolator( double minDistance, double maxDistance ) {
        this.minDistance = minDistance;
        this.maxDistance = maxDistance;
        useBuffer = true;
    }

    public double getValue( Coordinate[] controlPoints, Coordinate interpolated ) {
        Stream<Coordinate> stream = Stream.of(controlPoints);
        if (useBuffer) {
            stream = stream.filter(c -> isValidDistance(c.distance(interpolated)));
        }
        List<Coordinate> controlPointsUsed = stream.collect(Collectors.toList());
        return getValueInternal(interpolated, controlPointsUsed);
    }

    public double getValue( List<Coordinate> controlPoints, Coordinate interpolated ) {
        Stream<Coordinate> stream = controlPoints.stream();
        if (useBuffer) {
            stream = stream.filter(c -> isValidDistance(c.distance(interpolated)));
        }
        List<Coordinate> controlPointsUsed = stream.collect(Collectors.toList());
        return getValueInternal(interpolated, controlPointsUsed);
    }

    private boolean isValidDistance( double distance ) {
        if (useBuffer && (distance > maxDistance || distance < minDistance)) {
            return false;
        }
        return true;
    }

    private double getValueInternal( Coordinate interpolated, List<Coordinate> controlPointsUsed ) {
        int controlPointsNum = controlPointsUsed.size();
        GeneralMatrix v = null;
        try {
            v = makeMatrix(controlPointsUsed);
        } catch (Exception e) {
            return HMConstants.doubleNovalue;
        }
        double a1 = v.getElement(v.getNumRow() - 3, 0);
        double a2 = v.getElement(v.getNumRow() - 2, 0);
        double a3 = v.getElement(v.getNumRow() - 1, 0);

        double sum = 0;
        for( int i = 0; i < controlPointsNum; i++ ) {
            double dist = interpolated.distance(controlPointsUsed.get(i));
            sum = sum + (v.getElement(i, 0) * functionU(dist));
        }

        double value = (a1 + (a2 * interpolated.x) + (a3 * interpolated.y) + sum);
        interpolated.z = value;
        return value;
    }

    private GeneralMatrix makeMatrix( List<Coordinate> controlPoints ) {
        int pointsNum = controlPoints.size();
        GeneralMatrix L = new GeneralMatrix(pointsNum + 3, pointsNum + 3);

        fillKsubMatrix(controlPoints, L);
        fillPsubMatrix(controlPoints, L);
        fillOsubMatrix(controlPoints, L);
        L.invert();
        GeneralMatrix V = fillVMatrix(0, controlPoints);
        GeneralMatrix result = new GeneralMatrix(pointsNum + 3, 1);
        result.mul(L, V);
        return result;
    }

    /**
     * Calculates U function for distance.
     * 
     * @param distance distance
     * @return log(distance)*distance<sup>2</sup> or 0 if distance = 0
     */
    private double functionU( double distance ) {
        if (distance == 0) {
            return 0;
        }

        return distance * distance * Math.log(distance);
    }

    /**
     * Calculates U function where distance = ||p<sub>i</sub>, p<sub>j</sub>|| (from source points)
     * 
     * @param p_i p_i
     * @param p_j p_j
     * @return log(distance)*distance<sub>2</sub> or 0 if distance = 0
     */
    private double calculateFunctionU( Coordinate p_i, Coordinate p_j ) {
        double distance = p_i.distance(p_j);
        return functionU(distance);
    }

    /**
     * Fill K submatrix (<a href="http://elonen.iki.fi/code/tpsdemo/index.html"> see more here</a>)
     * 
     * @param controlPoints 
     * @param L
     */
    private void fillKsubMatrix( List<Coordinate> controlPoints, GeneralMatrix L ) {
        double alfa = 0;
        int controlPointsNum = controlPoints.size();
        for( int i = 0; i < controlPointsNum; i++ ) {
            for( int j = i + 1; j < controlPointsNum; j++ ) {
                double u = calculateFunctionU(controlPoints.get(i), controlPoints.get(j));
                L.setElement(i, j, u);
                L.setElement(j, i, u);
                alfa = alfa + (u * 2); // same for upper and lower part
            }
        }

        alfa = alfa / (controlPointsNum * controlPointsNum);
    }

    /**
     * Fill L submatrix (<a href="http://elonen.iki.fi/code/tpsdemo/index.html"> see more here</a>)
     */
    private void fillPsubMatrix( List<Coordinate> controlPoints, GeneralMatrix L ) {
        int controlPointsNum = controlPoints.size();
        for( int i = 0; i < controlPointsNum; i++ ) {
            L.setElement(i, i, 0);

            Coordinate c = controlPoints.get(i);
            L.setElement(i, controlPointsNum + 0, 1);
            L.setElement(i, controlPointsNum + 1, c.x);
            L.setElement(i, controlPointsNum + 2, c.y);

            L.setElement(controlPointsNum + 0, i, 1);
            L.setElement(controlPointsNum + 1, i, c.x);
            L.setElement(controlPointsNum + 2, i, c.y);
        }
    }

    /**
     * Fill O submatrix (<a href="http://elonen.iki.fi/code/tpsdemo/index.html"> see more here</a>)
     */
    private void fillOsubMatrix( List<Coordinate> controlPoints, GeneralMatrix L ) {
        int controlPointsNum = controlPoints.size();
        for( int i = controlPointsNum; i < (controlPointsNum + 3); i++ ) {
            for( int j = controlPointsNum; j < (controlPointsNum + 3); j++ ) {
                L.setElement(i, j, 0);
            }
        }
    }

    /**
     * Fill V matrix (matrix of target values).
     * 
     * @param dim 0 for dx, 1 for dy.
     * @return V Matrix
     */
    private GeneralMatrix fillVMatrix( int dim, List<Coordinate> controlPoints ) {
        int controlPointsNum = controlPoints.size();
        GeneralMatrix V = new GeneralMatrix(controlPointsNum + 3, 1);

        for( int i = 0; i < controlPointsNum; i++ ) {
            V.setElement(i, 0, controlPoints.get(i).z);
        }

        V.setElement(V.getNumRow() - 3, 0, 0);
        V.setElement(V.getNumRow() - 2, 0, 0);
        V.setElement(V.getNumRow() - 1, 0, 0);

        return V;
    }
}
