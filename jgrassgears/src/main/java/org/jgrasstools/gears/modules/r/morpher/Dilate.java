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
 * Dilate is an algorithm to dilate a binary image using a square kernel.
 * 
 * http://homepages.inf.ed.ac.uk/rbf/HIPR2/dilate.htm
 * 
 * @author Simon Horne
 * @author Craig Strachan
 * @author Judy Robertson, SELLIC Online
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class Dilate extends MorpherHelp {

    /**
     * Takes a BinaryFast representation of an image and a kernel
     * and applies a single iteration of the dilate algorithm.
     *
     * @param binary The BinaryFast representation of the input image.
     * @param kernel The array representing the kernel.
     * @return The BinaryFast representation of the new image after dilation.
     */
    private BinaryFast dilateSingleIteration( BinaryFast binary, int[] kernel ) {
        List<Point> result = new ArrayList<Point>();

        Iterator<Point> it = binary.getBackgroundEdgePixels().iterator();
        Point p;
        if (!kernelAll1s(kernel)) {
            while( it.hasNext() ) {
                p = new Point((Point) it.next());
                if (kernelMatch(p, binary.getPixels(), binary.getWidth(), binary.getHeight(), kernel, BinaryFast.FOREGROUND)) {
                    binary.getForegroundEdgePixels().add(p);
                    result.add(p);
                }
            }
        } else {
            while( it.hasNext() ) {
                p = new Point((Point) it.next());
                binary.getForegroundEdgePixels().add(p);
                result.add(p);
            }
        }
        it = result.iterator();
        while( it.hasNext() ) {
            binary.addPixel((Point) it.next());
        }
        binary.generateBackgroundEdgeFromForegroundEdge();
        return binary;
    }

    /**
     * Takes a BinaryFast image, a kernel and the number of iterations
     * and performs the necessary number of dilations on the image.
     *
     * @param binary The BinaryFast representation of the input image.
     * @param kernel The array representing the kernel.  If <code>null</code>, all 1 are used.
     * @param iterations The requested number of iterations.
     */
    public void process( BinaryFast binary, int[] kernel, int iterations ) {
        if (kernel == null) {
            kernel = DEFAULT3X3KERNEL;
        }

        int i = 0;
        int center = getCenterIndex(kernel);
        kernel[center] = 1;// Ignore centre pixel value in kernel (stops whiteout).
        while( i < iterations ) {
            binary = dilateSingleIteration(binary, kernel);
            ++i;
        }
        binary.generateForegroundEdgeFromBackgroundEdge();
    }

}
