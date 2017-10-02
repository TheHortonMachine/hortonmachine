/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
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
package org.hortonmachine.gears.modules.r.morpher;

import java.awt.Point;
import java.util.HashSet;
import java.util.Iterator;

import org.hortonmachine.gears.modules.utils.BinaryFast;

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
     * Takes an image and a kernel and thins it once.
     *
     * @param b the BinaryFast input image
     * @param kernel the thinning kernel
     * @return the thinned BinaryFast image
     */
    private BinaryFast thinBinaryRep( BinaryFast b, int[] kernel ) {
        Point p;
        HashSet<Point> inputHashSet = new HashSet<Point>();
        int[][] pixels = b.getPixels();
        if (kernelNo0s(kernel)) {
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
        HashSet<Point> result = hitMissHashSet(b, inputHashSet, kernel);
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
                    if (p.x + j >= 0 && p.y + k > 0 && p.x + j < b.getWidth() && p.y + k < b.getHeight()
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
     * @param binary the BinaryFast input image.
     * @param kernel the kernel to apply.
     */
    public void processSkeleton( BinaryFast binary, int[][] kernel ) {
        int oldForeEdge = 0;
        int oldBackEdge = 0;
        while( !(binary.getForegroundEdgePixels().size() == oldForeEdge && binary.getBackgroundEdgePixels().size() == oldBackEdge) ) {
            oldForeEdge = binary.getForegroundEdgePixels().size();
            oldBackEdge = binary.getBackgroundEdgePixels().size();
            for( int i = 0; i < kernel.length; ++i ) {
                binary = thinBinaryRep(binary, kernel[i]);
                binary.generateBackgroundEdgeFromForegroundEdge();
            }
        }
    }

    public void processPruning( BinaryFast binary, int iterations, int[][] kernels ) {
        for( int j = 0; j < iterations; j++ ) {
            for( int i = 0; i < kernels.length; ++i ) {
                binary = thinBinaryRep(binary, kernels[i]);
                binary.generateBackgroundEdgeFromForegroundEdge();
            }
        }
    }

    public void processLineendings( BinaryFast binary, int[][] kernels ) {
        for( int i = 0; i < kernels.length; ++i ) {
            binary = thinBinaryRep(binary, kernels[i]);
            binary.generateBackgroundEdgeFromForegroundEdge();
        }
    }

    /**
     *Returns true if the 8 neighbours of p match the kernel
     *0 is background
     *1 is foreground
     *2 is don't care.
     *
     * @param p the point at the centre of the 
     * 9 pixel neighbourhood
     * @param pixels the 2D array of the image
     * @param w the width of the image
     * @param h the height of the image
     * @param kernel the array of the kernel values
     * @return True if the kernel and image match.
     */
    private boolean kernelMatch( Point p, int[][] pixels, int w, int h, int[] kernel ) {
        int matched = 0;
        for( int j = -1; j < 2; ++j ) {
            for( int i = -1; i < 2; ++i ) {
                if (kernel[((j + 1) * 3) + (i + 1)] == 2) {
                    ++matched;
                } else if ((p.x + i >= 0)
                        && (p.x + i < w)
                        && (p.y + j >= 0)
                        && (p.y + j < h)
                        && (((pixels[p.x + i][p.y + j] == BinaryFast.FOREGROUND) && (kernel[((j + 1) * 3) + (i + 1)] == 1)) || ((pixels[p.x
                                + i][p.y + j] == BinaryFast.BACKGROUND) && (kernel[((j + 1) * 3) + (i + 1)] == 0)))) {
                    ++matched;
                }
            }
        }
        if (matched == 9) {
            return true;
        } else
            return false;
    }

    /**
     * Applies the hitmiss operation to a set of pixels
     * stored in a hash table.
     *
     * @param b the BinaryFast input image
     * @param input the set of pixels requiring matching
     * @param kernel the kernel to match them with
     * @return A hash table containing all the successful matches.
     */
    private HashSet<Point> hitMissHashSet( BinaryFast b, HashSet<Point> input, int[] kernel ) {
        HashSet<Point> output = new HashSet<Point>();
        Iterator<Point> it = input.iterator();
        while( it.hasNext() ) {
            Point p = it.next();
            if (kernelMatch(p, b.getPixels(), b.getWidth(), b.getHeight(), kernel)) {
                // System.out.println("Match "+p.x+" "+p.y);
                output.add(p);
            }
        }
        // System.out.println(output.size());
        return output;
    }

    /**
     * Returns true if the kernel has no 0s.
     *
     * @param kernel the array storing the kernel values.
     * @return True if no 0s (false otherwise)
     */
    private boolean kernelNo0s( int[] kernel ) {
        for( int i = 0; i < kernel.length; ++i ) {
            if (kernel[i] == 0)
                return false;
        }
        return true;
    }
}
