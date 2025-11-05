/* 
 *  Copyright (c) 2010, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package org.hortonmachine.gears.modules.v.smoothing;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.imagen.media.contour.AbstractSmoother;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Polygon;



/**
 * Polygon smoothing by interpolation with cubic Bazier curves.
 * This code implements an algorithm which was devised by Maxim Shemanarev
 * and described by him at:
 * <a href="http://www.antigrain.com/research/bezier_interpolation/index.html">
 * http://www.antigrain.com/research/bezier_interpolation/index.html</a>.
 * <p>
 * Note: the code here is <b>not</b> that written by Maxim to accompany his
 * algorithm description. Rather, it is an original implementation and any
 * errors are my fault.
 * 
 * @author Michael Bedward
 * @since 1.1
 * @version $Id$
 */
public class PolygonSmoother extends AbstractSmoother {

    /**
     * Default constructor.
     */
    public PolygonSmoother() {
        super(new GeometryFactory());
    }

    /**
     * Constructor.
     * 
     * @param geomFactory an instance of {@code GeometryFactory} to use when
     *        creating smoothed {@code Geometry} objects
     * 
     * @throws IllegalArgumentException if {@code geomFactory} is {@code null}
     */
    public PolygonSmoother(GeometryFactory geomFactory) {
        super(geomFactory);
    }
    
    /**
     * Calculates a pair of Bezier control points for each vertex in an
     * array of {@code Coordinates}.
     * 
     * @param coords input vertices
     * @param N number of coordinates in {@coords} to use
     * @param alpha tightness of fit
     * 
     * @return 2D array of {@code Coordinates} for positions of each pair of
     *         control points per input vertex
     */
    private Coordinate[][] getControlPoints(Coordinate[] coords, int N, double alpha) {
        if (alpha < 0.0 || alpha > 1.0) {
            throw new IllegalArgumentException("alpha must be a value between 0 and 1 inclusive");
        }

        Coordinate[][] ctrl = new Coordinate[N][2];

        Coordinate[] v = new Coordinate[3];

        Coordinate[] mid = new Coordinate[2];
        mid[0] = new Coordinate();
        mid[1] = new Coordinate();

        Coordinate anchor = new Coordinate();
        double[] vdist = new double[2];
        double mdist;

        v[1] = coords[N - 1];
        v[2] = coords[0];
        mid[1].x = (v[1].x + v[2].x) / 2.0;
        mid[1].y = (v[1].y + v[2].y) / 2.0;
        vdist[1] = v[1].distance(v[2]);

        for (int i = 0; i < N; i++) {
            v[0] = v[1];
            v[1] = v[2];
            v[2] = coords[(i + 1) % N];

            mid[0].x = mid[1].x;
            mid[0].y = mid[1].y;
            mid[1].x = (v[1].x + v[2].x) / 2.0;
            mid[1].y = (v[1].y + v[2].y) / 2.0;

            vdist[0] = vdist[1];
            vdist[1] = v[1].distance(v[2]);

            double p = vdist[0] / (vdist[0] + vdist[1]);
            anchor.x = mid[0].x + p * (mid[1].x - mid[0].x);
            anchor.y = mid[0].y + p * (mid[1].y - mid[0].y);

            double xdelta = anchor.x - v[1].x;
            double ydelta = anchor.y - v[1].y;

            ctrl[i][0] = new Coordinate(
                    alpha*(v[1].x - mid[0].x + xdelta) + mid[0].x - xdelta,
                    alpha*(v[1].y - mid[0].y + ydelta) + mid[0].y - ydelta);

            ctrl[i][1] = new Coordinate(
                    alpha*(v[1].x - mid[1].x + xdelta) + mid[1].x - xdelta,
                    alpha*(v[1].y - mid[1].y + ydelta) + mid[1].y - ydelta);
        }

        return ctrl;
    }
    
    
    /**
     * Creates a new {@code Polygon} whose exterior shell is a smoothed
     * version of the input {@code Polygon}.
     * <p>
     * Note: this method presently ignores holes.
     * 
     * @param inputPoly the input {@code Polygon}
     * 
     * @param alpha a value between 0 and 1 (inclusive) specifying the tightness
     *        of fit of the smoothed boundary (0 is loose)
     * 
     * @return the smoothed {@code Polygon}
     */
    public Polygon smooth(Polygon inputPoly, double alpha) {
        Coordinate[] coords = inputPoly.getExteriorRing().getCoordinates();
        final int N = coords.length - 1;  // first coord == last coord
        final int LAST = N - 1;
        
        Coordinate[][] controlPoints = getControlPoints(coords, N, alpha);
        
        List<Coordinate> smoothCoords = new ArrayList<>();
        double dist;
        for (int i = 0; i < N; i++) {
            int next = (i + 1) % N;
            
            dist = coords[i].distance(coords[next]);
            if (dist < control.getMinLength()) {
                // segment too short - just copy input coordinate
                smoothCoords.add(new Coordinate(coords[i]));

                // if this was the last vertex we also need to add
                // the closing vertex
                if (i == LAST) {
                    smoothCoords.add(new Coordinate(coords[0]));
                }
                
            } else {
                int smoothN = control.getNumVertices(dist);
                Coordinate[] segment = cubicBezier(
                        coords[i], coords[next],
                        controlPoints[i][1], controlPoints[next][0],
                        smoothN);
            
                int copyN = i == LAST ? segment.length : segment.length - 1;
                for (int k = 0; k < copyN; k++) {
                    smoothCoords.add(segment[k]);
                }
            }
        }
        
        LinearRing shell = geomFactory.createLinearRing(smoothCoords.toArray(new Coordinate[0]));
        Polygon smoothedPoly = geomFactory.createPolygon(shell, null);
        
        // Preserve user data from the input
        smoothedPoly.setUserData(inputPoly.getUserData());
        return smoothedPoly;
    }
    
}
