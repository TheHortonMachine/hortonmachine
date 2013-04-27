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

/**
 * http://homepages.inf.ed.ac.uk/rbf/HIPR2/dilate.htm
 * 
 * @author Simon Horne
 * @author Andrea Antonello (www.hydrologis.com)
 *
 */
public class MorpherHelp {
    public static int[] DEFAULT3X3KERNEL = new int[]{//
    /*    */1, 1, 1, //
            1, 1, 1, //
            1, 1, 1//
    };

    protected int getSquareKernelSide( int[] kernel ) {
        double side = Math.sqrt(kernel.length);
        if (side % (int) side != 0) {
            throw new IllegalArgumentException("The kernel has to be square.");
        }
        return (int) side;
    }

    /**
     * Returns true if the kernel consists of all 1s.
     *
     * @param kernel The array representing the kernel.
     * @return True or false (true - kernel all 1s).
     */
    protected boolean kernelAll1s( int[] kernel ) {
        for( int i = 0; i < kernel.length; ++i ) {
            if (kernel[i] != 1)
                return false;
        }
        return true;
    }

    /**
     * Takes a point and a 2D array representing an image and a kernel, if the 
     * area around the point matches the kernel then the method returns true.
     *
     * @param p The point in the centre of the neighbourhood to be checked.
     * @param pixels The 2D array representing the image.
     * @param w The width of the image.
     * @param h The height of the image.
     * @param kernel The kernel used to match with the image.
     * @return True or false (true - the kernel and image match).
     */
    protected boolean kernelMatch( Point p, int[][] pixels, int w, int h, int[] kernel, int matchValue ) {
        int start = -1;
        int end = getSquareKernelSide(kernel) - 1;
        for( int j = start; j < end; ++j ) {
            for( int i = start; i < end; ++i ) {
                if (kernel[((j + 1) * 3) + (i + 1)] == 1) {
                    if ((p.x + i >= 0) && (p.x + i < w) && (p.y + j >= 0) && (p.y + j < h)) {
                        if (pixels[p.x + i][p.y + j] == matchValue) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    protected int getCenterIndex( int[] kernel ) {
        return (int) Math.floor(kernel.length / 2.0);
    }

}
