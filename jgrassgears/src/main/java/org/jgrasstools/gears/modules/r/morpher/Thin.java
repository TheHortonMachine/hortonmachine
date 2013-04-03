/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * 
 * JGrasstools is free software: you can redistribute it and/or modify
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
package org.jgrasstools.gears.modules.r.morpher;

import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;

import org.jgrasstools.gears.modules.utils.BinaryFast;


/*
 
 R. B. Fisher, K. Koryllos,
``Interactive Textbooks; Embedding Image Processing Operator Demonstrations
in Text'', Int. J. of Pattern Recognition and Artificial Intelligence,
Vol 12, No 8, pp 1095-1123, 1998.
 
 */


/**
 * Thin is an algorithm to thin a binary image using a 3x3 kernel.
 * @author Simon Horne.
 */

public class Thin {

    /**
     * The default constructor with no parameters. 
     */
    public Thin() {
    }

    /**
     * Takes an image and a kernel and thins it once.
     *
     * @param b the BinaryFast input image
     * @param kernel the thinning kernel
     * @return the thinned BinaryFast image
     */
    private BinaryFast thinBinaryRep( BinaryFast b, int[] kernel ) {
        HitMiss hitMiss = new HitMiss();
        Point p;
        HashSet<Point> inputHashSet = new HashSet<Point>();
        int[][] pixels = b.getPixels();
        if (hitMiss.kernelNo0s(kernel)) {
            for( int j = 0; j < b.getHeight(); ++j ) {
                for( int i = 0; i < b.getWidth(); ++i ) {
                    if (pixels[i][j] == BinaryFast.FOREGROUND) {
                        inputHashSet.add(new Point(i, j));
                    }
                }
            }
        } else {
            Iterator<Point> it = b.getForegroundEdgePixels().iterator();
            while( it.hasNext() ) {
                inputHashSet.add(it.next());
            }
        }
        HashSet<Point> result = hitMiss.hitMissHashSet(b, inputHashSet, kernel);
        Iterator<Point> it = result.iterator();
        while( it.hasNext() ) {
            p = new Point(it.next());
            // make p a background pixel and update the edge sets
            b.removePixel(p);
            b.getForegroundEdgePixels().remove(p);
            b.getBackgroundEdgePixels().add(p);
            // check if new foreground pixels are exposed as edges
            for( int j = -1; j < 2; ++j ) {
                for( int k = -1; k < 2; ++k ) {
                    if (p.x + j >= 0 && p.y + k > 0 && p.x + j < b.getWidth()
                            && p.y + k < b.getHeight()
                            && pixels[p.x + j][p.y + k] == BinaryFast.FOREGROUND) {
                        Point p2 = new Point(p.x + j, p.y + k);
                        b.getForegroundEdgePixels().add(p2);
                    }
                }
            }
        }
        return b;
    }

    /**
     * Takes an image and a kernel and thins it the specified number of times.
     *
     * @param b the BinaryFast input image
     * @param kernel the array of thinning kernels
     * @return the thinned BinaryFast image
     */
    public void process( BinaryFast binary, int[][] kernel ) {
        if (kernel == null) {
            kernel = new int[][]{{0, 0, 0, 2, 1, 2, 1, 1, 1}, {2, 0, 0, 1, 1, 0, 2, 1, 2},
                    {1, 2, 0, 1, 1, 0, 1, 2, 0}, {2, 1, 2, 1, 1, 0, 2, 0, 0},
                    {1, 1, 1, 2, 1, 2, 0, 0, 0}, {2, 1, 2, 0, 1, 1, 0, 0, 2},
                    {0, 2, 1, 0, 1, 1, 0, 2, 1}, {0, 0, 2, 0, 1, 1, 2, 1, 2}};
        }

        int oldForeEdge = 0;
        int oldBackEdge = 0;
        while( !(binary.getForegroundEdgePixels().size() == oldForeEdge && binary
                .getBackgroundEdgePixels().size() == oldBackEdge) ) {
            oldForeEdge = binary.getForegroundEdgePixels().size();
            oldBackEdge = binary.getBackgroundEdgePixels().size();
            for( int i = 0; i < 8; ++i ) {
                binary = thinBinaryRep(binary, kernel[i]);
                binary.generateBackgroundEdgeFromForegroundEdge();
            }
        }
    }
}
