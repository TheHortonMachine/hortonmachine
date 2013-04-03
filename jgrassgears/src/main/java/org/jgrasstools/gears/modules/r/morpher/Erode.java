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
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.jgrasstools.gears.modules.utils.BinaryFast;

/**
 * Erode is an algorithm to erode a binary image using a 3x3 kernel.
 *
 * @author Simon Horne
 * @author Craig Strachan
 * @author Judy Robertson, SELLIC Online
 */
public class Erode {

    /**
     * Default no-args constructor.
     */
    public Erode() {
    }

    /**
     * Returns true if the kernel matches the area of image centred on
     * the given point.
     *
     * @param p The centre point identifying the pixel neighbourhood.
     * @param pixels The 2D array representing the image.
     * @param w The width of the image.
     * @param h The height of the image.
     * @param kernel The array representing the kernel.
     * @return True or false (true - the kernel and image match).
     */
    private boolean kernelMatch( Point p, int[][] pixels, int w, int h, int[] kernel ) {
        for( int j = -1; j < 2; ++j ) {
            for( int i = -1; i < 2; ++i ) {
                if (kernel[((j + 1) * 3) + (i + 1)] == 1) {
                    if ((p.x + i >= 0) && (p.x + i < w) && (p.y + j >= 0) && (p.y + j < h)) {
                        if (pixels[p.x + i][p.y + j] == BinaryFast.BACKGROUND) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Returns true if the kernel consists of 9 1s.
     *
     * @param kernel The array representing the kernel.
     * @return True or false (true - kernel is all 1s).
     */

    private boolean kernelAll1s( int[] kernel ) {
        for( int i = 0; i < 9; ++i ) {
            if (kernel[i] == 0)
                return false;
        }
        return true;
    }

    /**
     * Applies a single iteration of the erode algorithm to the image.
     *
     * @param binary The BinaryFast representation of the input image.
     * @param kernel The array representing the kernel.
     * @return The BinaryFast representation of the new eroded image.
     */
    private BinaryFast erodeSingleIteration( BinaryFast binary, int[] kernel ) {
        List<Point> result = new ArrayList<Point>();

        Iterator<Point> it = binary.getForegroundEdgePixels().iterator();
        Point p;
        if (!kernelAll1s(kernel)) {
            while( it.hasNext() ) {
                p = new Point(it.next());
                if (kernelMatch(p, binary.getPixels(), binary.getWidth(), binary.getHeight(),
                        kernel)) {
                    binary.getBackgroundEdgePixels().add(p);
                    result.add(p);
                }
            }
        } else {
            while( it.hasNext() ) {
                p = new Point(it.next());
                binary.getBackgroundEdgePixels().add(p);
                result.add(p);
            }
        }
        it = result.iterator();
        while( it.hasNext() ) {
            binary.removePixel(it.next());
        }
        binary.generateForegroundEdgeFromBackgroundEdge();
        return binary;
    }

    /**
     * Applies several iterations of the erode algorithm to an image.
     *
     * @param binary The BinaryFast representation of the input image.
     * @param kernel The array representing the kernel. If <code>null</code>, all 1 are used.
     * @param iterations The number of iterations to be applied.
     */
    public void process( BinaryFast binary, int[] kernel, int iterations ) {
        if (kernel == null) {
            kernel = new int[]{1, 1, 1, 1, 1, 1, 1, 1, 1};
        }

        int i = 0;
        kernel[4] = 1;// Ignore centre pixel value in kernel (stops whiteout)
        while( i < iterations ) {
            binary = erodeSingleIteration(binary, kernel);
            ++i;
        }
        binary.generateBackgroundEdgeFromForegroundEdge();
    }

}
