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

/**
 * HitMiss is an algorithm to 'hit and miss' a 
 * binary image using a 3x3 kernel.
 *
 * @author: Simon Horne
 */
public class HitMiss {

    /**
     * Default no-args constructor.
     */
    public HitMiss() {
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
                        && (((pixels[p.x + i][p.y + j] == BinaryFast.FOREGROUND) && (kernel[((j + 1) * 3)
                                + (i + 1)] == 1)) || ((pixels[p.x + i][p.y + j] == BinaryFast.BACKGROUND) && (kernel[((j + 1) * 3)
                                + (i + 1)] == 0)))) {
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
    public HashSet<Point> hitMissHashSet( BinaryFast b, HashSet<Point> input, int[] kernel ) {
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

    // /**
    // * Returns true if the 3x3 kernel consists of 9 1s.
    // *
    // * @param kernel the array storing the 9 values
    // * @return True if all 1s (false otherwise)
    // */
    // private boolean kernelAll1s( int[] kernel ) {
    // for( int i = 0; i < 9; ++i ) {
    // if (kernel[i] == 0)
    // return false;
    // }
    // return true;
    // }

    // /**
    // * Returns true if the 3x3 kernel consists of 9 0s.
    // *
    // * @param kernel the array storing the 9 values
    // * @return True if all 0s (false otherwise)
    // */
    // private boolean kernelAll0s( int[] kernel ) {
    // for( int i = 0; i < 9; ++i ) {
    // if (kernel[i] == 1)
    // return false;
    // }
    // return true;
    // }
    /**
     * Returns true if the 3x3 kernel has no 0s.
     *
     * @param kernel the array storing the 9 values
     * @return True if no 0s (false otherwise)
     */
    public boolean kernelNo0s( int[] kernel ) {
        for( int i = 0; i < 9; ++i ) {
            if (kernel[i] == 0)
                return false;
        }
        return true;
    }
    // /**
    // * Returns true if the 3x3 kernel has no 1s.
    // *
    // * @param kernel the array storing the 9 values
    // * @return True if no 1s (false otherwise)
    // */
    // private boolean kernelNo1s( int[] kernel ) {
    // for( int i = 0; i < 9; ++i ) {
    // if (kernel[i] == 1)
    // return false;
    // }
    // return true;
    // }

    // /**
    // * Takes a BinaryFast image representation and a kernel and
    // * applies the hitmiss algorithm to the image.
    // *
    // * @param b the image in BinaryFast representation
    // * @param kernel the kernel in 1D array form
    // * @return The new BinaryFast image after hitmissing
    // */
    // private BinaryFast hitmiss_image( BinaryFast b, int[] kernel ) {
    // HashSet<Point> input = new HashSet<Point>();
    // HashSet<Point> result = new HashSet<Point>();
    //
    // // if kernel is all1s then simply remove foreEdge from fore
    // // if kernel is all0s then flip image
    // // if kernel is all1sAndAnys [4] 1 then add all fore
    // // if kernel is all0sAndAnys [4] 0 then make input all back pixels
    // // if kernel is mixed 1s0s (and/or anys) and [4] is 1 then use fore edge
    // // if kernel is mixed 1s0s and [4] is 0 then use back edge
    // // if kernel [4] is any then use all pixels
    // int[][] pixels = b.getPixels();
    // if (kernelNo1s(kernel) && kernelNo0s(kernel)) {
    // for( int j = 0; j < b.getHeight(); ++j ) {
    // for( int i = 0; i < b.getWidth(); ++i ) {
    // pixels[i][j] = BinaryFast.FOREGROUND;
    // }
    // }
    // } else {
    // Point p;
    // if (kernel[4] == 1) {
    // for( int j = 0; j < b.getHeight(); ++j ) {
    // for( int i = 0; i < b.getWidth(); ++i ) {
    // if (pixels[i][j] == BinaryFast.FOREGROUND) {
    // p = new Point(i, j);
    // input.add(p);
    // }
    // }
    // }
    // } else if (kernel[4] == 0) {
    // for( int j = 0; j < b.getHeight(); ++j ) {
    // for( int i = 0; i < b.getWidth(); ++i ) {
    // if (pixels[i][j] == BinaryFast.BACKGROUND) {
    // p = new Point(i, j);
    // input.add(p);
    // }
    // }
    // }
    // } else {
    // for( int j = 0; j < b.getHeight(); ++j ) {
    // for( int i = 0; i < b.getWidth(); ++i ) {
    // p = new Point(i, j);
    // input.add(p);
    // }
    // }
    // }
    // result = new HashSet<Point>(HitMissHashSet(b, input, kernel));
    // // System.out.println("Test");
    // // System.out.println(result.size());
    // b.generatePixels(result);
    // }
    // return b;
    // }
}
