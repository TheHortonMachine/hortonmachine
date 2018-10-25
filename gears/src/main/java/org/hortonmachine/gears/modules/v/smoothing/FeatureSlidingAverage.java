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
package org.hortonmachine.gears.modules.v.smoothing;

import java.util.ArrayList;
import java.util.List;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;

/**
 * Applies a sliding average on linear geometries for smoothing.
 * 
 * <p>
 * See: http://grass.osgeo.org/wiki/V.generalize_tutorial
 * </p>
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class FeatureSlidingAverage {

    private static final double DELTA = 0.00001;
    private final Geometry geometry;

    public FeatureSlidingAverage( Geometry geometry ) {
        this.geometry = geometry;
    }

    public List<Coordinate> smooth( int lookAhead, boolean considerZ, double slide ) {

        double sc;
        List<Coordinate> res = new ArrayList<Coordinate>();

        Coordinate[] coordinates = geometry.getCoordinates();
        int n = coordinates.length; 

        if (n < 4 * lookAhead) {
            /*
             * if lookahead is too large, lets put it as the
             * 20% of the number of coordinates
             */
            lookAhead = (int) Math.floor(n * 0.2d);
        }

        if (lookAhead % 2 == 0) {
            lookAhead++;
        }
        if (lookAhead < 3)
            return null;

        int halfLookAhead = lookAhead / 2;
        if (halfLookAhead > coordinates.length) {
            throw new RuntimeException();
        }

        int padding = 0;
        if (coordinates[0].distance(coordinates[n - 1]) < DELTA) {
            // we have a ring, extend it for smoothing
            int tmpN = lookAhead / 2;
            if (tmpN > n / 2) {
                tmpN = n / 2;
            }
            padding = tmpN;
            Coordinate[] ringCoordinates = new Coordinate[n + 2 * tmpN];
            for( int i = 0; i < tmpN; i++ ) {
                ringCoordinates[i] = coordinates[n - (tmpN - i) - 1];
            }
            System.arraycopy(coordinates, 0, ringCoordinates, tmpN, n);
            int index = 1;
            for( int i = ringCoordinates.length - padding; i < ringCoordinates.length; i++ ) {
                ringCoordinates[i] = coordinates[index++];
            }
            coordinates = ringCoordinates;
        }
        n = n + 2 * padding;

        for( int j = 0; j < n; j++ ) {
            Coordinate tmp = new Coordinate();
            res.add(tmp);
        }

        sc = (double) 1.0 / (double) lookAhead;

        Coordinate pCoord = new Coordinate();
        Coordinate sCoord = new Coordinate();
        pointAssign(coordinates, 0, considerZ, pCoord);
        for( int i = 1; i < lookAhead; i++ ) {
            Coordinate tmpCoord = new Coordinate();
            pointAssign(coordinates, i, considerZ, tmpCoord);
            pointAdd(pCoord, tmpCoord, pCoord);
        }

        // progressive smooth the first part
        // int tmpHalf = 0;
        // for( int i = 0; i < halfLookAhead; i++ ) {
        // if (i < 1) {
        // continue;
        // }
        //
        // tmpHalf = i - 1;
        //
        // Coordinate tmpCoord = new Coordinate();
        // pointAssign(coordinates, i, considerZ, sCoord);
        // pointScalar(sCoord, 1.0 - slide, sCoord);
        // pointScalar(pCoord, sc * slide, tmpCoord);
        // pointAdd(tmpCoord, sCoord, res.get(i));
        // if (i + tmpHalf + 1 < n) {
        // pointAssign(coordinates, i - tmpHalf, considerZ, tmpCoord);
        // pointSubtract(pCoord, tmpCoord, pCoord);
        // pointAssign(coordinates, i + tmpHalf + 1, considerZ, tmpCoord);
        // pointAdd(pCoord, tmpCoord, pCoord);
        // }
        // }

        /* and calculate the average of remaining points */
        for( int i = halfLookAhead; i + halfLookAhead < n; i++ ) {
            Coordinate tmpCoord = new Coordinate();
            pointAssign(coordinates, i, considerZ, sCoord);
            pointScalar(sCoord, 1.0 - slide, sCoord);
            pointScalar(pCoord, sc * slide, tmpCoord);
            pointAdd(tmpCoord, sCoord, res.get(i));
            if (i + halfLookAhead + 1 < n) {
                pointAssign(coordinates, i - halfLookAhead, considerZ, tmpCoord);
                pointSubtract(pCoord, tmpCoord, pCoord);
                pointAssign(coordinates, i + halfLookAhead + 1, considerZ, tmpCoord);
                pointAdd(pCoord, tmpCoord, pCoord);
            }
        }

        // progressive smooth the last part
        // tmpHalf = 0;
        // for( int i = n - halfLookAhead; i < n - 1; i++ ) {
        //
        // tmpHalf = n - 1 - i;
        //
        // Coordinate tmpCoord = new Coordinate();
        // pointAssign(coordinates, i, considerZ, sCoord);
        // pointScalar(sCoord, 1.0 - slide, sCoord);
        // pointScalar(pCoord, sc * slide, tmpCoord);
        // pointAdd(tmpCoord, sCoord, res.get(i));
        // if (i + tmpHalf <= n) {
        // pointAssign(coordinates, i - tmpHalf, considerZ, tmpCoord);
        // pointSubtract(pCoord, tmpCoord, pCoord);
        // pointAssign(coordinates, i + tmpHalf, considerZ, tmpCoord);
        // pointAdd(pCoord, tmpCoord, pCoord);
        // }
        // }
        //
        // Coordinate c = res.get(0);
        // c.x = coordinates[0].x;
        // c.y = coordinates[0].y;
        // c.z = coordinates[0].z;
        // c = res.get(res.size() - 1);
        // c.x = coordinates[n - 1].x;
        // c.y = coordinates[n - 1].y;
        // c.z = coordinates[n - 1].z;
        for( int i = 0; i < halfLookAhead; i++ ) {
            Coordinate coordinate = res.get(i);
            coordinate.x = coordinates[i].x;
            coordinate.y = coordinates[i].y;
            coordinate.z = coordinates[i].z;
        }
        for( int i = n - halfLookAhead - 1; i < n; i++ ) {
            Coordinate coordinate = res.get(i);
            coordinate.x = coordinates[i].x;
            coordinate.y = coordinates[i].y;
            coordinate.z = coordinates[i].z;
        }
        // for( i = half; i + half < n; i++ ) {
        // coordinates[i].x = res.get(i).x;
        // coordinates[i].y = res.get(i).y;
        // coordinates[i].z = res.get(i).z;
        // }

        // return Points->n_points;

        if (padding != 0) {
            res = res.subList(padding, n - padding - 1);
            res.add(res.get(0));
        }

        return res;
    }
    private void pointAssign( Coordinate[] coordinates, int index, boolean considerZ,
            Coordinate newAssignedCoordinate ) {
        Coordinate coordinate = coordinates[index];
        newAssignedCoordinate.x = coordinate.x;
        newAssignedCoordinate.y = coordinate.y;
        if (considerZ) {
            newAssignedCoordinate.z = coordinate.z;
        } else {
            newAssignedCoordinate.z = 0;
        }
        return;
    }

    private void pointAdd( Coordinate a, Coordinate b, Coordinate res ) {
        res.x = a.x + b.x;
        res.y = a.y + b.y;
        res.z = a.z + b.z;
    }

    private void pointSubtract( Coordinate a, Coordinate b, Coordinate res ) {
        res.x = a.x - b.x;
        res.y = a.y - b.y;
        res.z = a.z - b.z;
    }

    private void pointScalar( Coordinate a, double k, Coordinate res ) {
        res.x = a.x * k;
        res.y = a.y * k;
        res.z = a.z * k;
    }

}
