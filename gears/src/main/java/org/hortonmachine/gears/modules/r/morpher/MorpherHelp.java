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

/**
 * Some help methods.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class MorpherHelp {
    public static int[] DEFAULT3X3KERNEL = new int[]{//
    /*    */1, 1, 1, //
            1, 1, 1, //
            1, 1, 1//
    };

    /**
     * See: http://www.imagemagick.org/Usage/morphology/#skeleton1
     */
    public static int[][] SKELETON1_KERNEL = new int[][]{//
    /*    */{0, 2, 1, //
            0, 1, 1, //
            0, 2, 1}, //

            /*    */{0, 0, 2, //
                    0, 1, 1, //
                    2, 1, 1},
            //
            /*    */{0, 0, 0, //
                    2, 1, 2, //
                    1, 1, 1}, //

            /*    */{2, 0, 0, //
                    1, 1, 0, //
                    1, 1, 2}, //

            /*    */{1, 2, 0, //
                    1, 1, 0, //
                    1, 2, 0}, //

            /*    */{1, 1, 2, //
                    1, 1, 0, //
                    2, 0, 0}, //

            /*    */{1, 1, 1, //
                    2, 1, 2, //
                    0, 0, 0}, //

            /*    */{2, 1, 1,//
                    0, 1, 1, //
                    0, 0, 2} //
    };

    /**
     * See: http://www.imagemagick.org/Usage/morphology/#skeleton2
     */
    public static int[][] SKELETON2_KERNEL = new int[][]{//
    /*    */{0, 2, 1, //
            0, 1, 1, //
            0, 2, 1}, //

            /*    */{0, 0, 2, //
                    0, 1, 1, //
                    2, 1, 2}, //

            /*    */{0, 0, 0, //
                    2, 1, 2, //
                    1, 1, 1}, //

            /*    */{2, 0, 0, //
                    1, 1, 0, //
                    2, 1, 2}, //

            /*    */{1, 2, 0, //
                    1, 1, 0, //
                    1, 2, 0}, //

            /*    */{2, 1, 2, //
                    1, 1, 0, //
                    2, 0, 0}, //

            /*    */{1, 1, 1, //
                    2, 1, 2, //
                    0, 0, 0}, //

            /*    */{2, 1, 2,//
                    0, 1, 1, //
                    0, 0, 2} //

    };

    /**
     * See: http://www.imagemagick.org/Usage/morphology/#skeleton2
     */
    public static int[][] SKELETON2VARIANT_KERNEL = new int[][]{//

    /*    */{0, 2, 1, //
            0, 1, 1, //
            0, 2, 1}, //

            /*    */{1, 2, 0, //
                    1, 1, 0, //
                    1, 2, 0}, //

            /*    */{1, 1, 1, //
                    2, 1, 2, //
                    0, 0, 0}, //

            /*    */{0, 0, 0, //
                    2, 1, 2, //
                    1, 1, 1}, //

            // corners
            /*    */{2, 1, 2,//
                    0, 1, 1, //
                    0, 0, 2}, //

            /*    */{0, 0, 2, //
                    0, 1, 1, //
                    2, 1, 2}, //

            /*    */{2, 0, 0, //
                    1, 1, 0, //
                    2, 1, 2}, //

            /*    */{2, 1, 2, //
                    1, 1, 0, //
                    2, 0, 0} //
    };

    /**
     * See: http://www.imagemagick.org/Usage/morphology/#skeleton3
     */
    public static int[][] SKELETON3_KERNEL = new int[][]{//
    /*    */{2, 2, 1, //
            0, 1, 1, //
            2, 2, 1}, //

            /*    */{2, 0, 1, //
                    0, 1, 1, //
                    2, 0, 2}, //

            /*    */{2, 0, 0, //
                    0, 1, 1, //
                    2, 2, 1}, //

            /*    */{1, 2, 2, //
                    1, 1, 0, //
                    1, 2, 2}, //

            /*    */{2, 0, 2, //
                    1, 1, 0, //
                    1, 2, 2}, //

            /*    */{1, 2, 2, //
                    1, 1, 0, //
                    2, 0, 2}, //

            /*    */{1, 1, 1, //
                    2, 1, 2, //
                    2, 0, 2}, //

            /*    */{1, 1, 2,//
                    2, 1, 0, //
                    2, 0, 2}, //

            /*    */{2, 1, 1,//
                    0, 1, 2, //
                    2, 0, 2}, //

            /*    */{2, 0, 2,//
                    2, 1, 2, //
                    1, 1, 1}, //

            /*    */{2, 0, 2,//
                    0, 1, 2, //
                    2, 1, 1}, //

            /*    */{2, 0, 2,//
                    2, 1, 0, //
                    1, 1, 2} //
    };

    public static int[][] DEFAULT_PRUNE_KERNEL = new int[][]{//
    /*    */{0, 0, 0, //
            0, 1, 0, //
            0, 2, 2}, //

            /*    */{0, 0, 0, //
                    2, 1, 0, //
                    2, 0, 0}, //

            /*    */{2, 2, 0, //
                    0, 1, 0, //
                    0, 0, 0}, //

            /*    */{0, 0, 2, //
                    0, 1, 2, //
                    0, 0, 0}, //

            // and reverse way
            /*    */{0, 0, 0, //
                    0, 1, 0, //
                    2, 2, 0}, //

            /*    */{2, 0, 0, //
                    2, 1, 0, //
                    0, 0, 0}, //

            /*    */{0, 2, 2, //
                    0, 1, 0, //
                    0, 0, 0}, //

            /*    */{0, 0, 0, //
                    0, 1, 2, //
                    0, 0, 2} //
    };

    public static int[][] LINEEND_KERNEL = new int[][]{//
    /*    */{0, 0, 2, //
            0, 1, 1, //
            0, 0, 2}, //

            /*    */{0, 0, 0, //
                    0, 1, 0, //
                    2, 1, 2}, //

            /*    */{2, 0, 0, //
                    1, 1, 0, //
                    2, 0, 0}, //

            /*    */{2, 1, 2, //
                    0, 1, 0, //
                    0, 0, 0}, //

            /*    */{0, 0, 0, //
                    0, 1, 0, //
                    0, 0, 1}, //

            /*    */{0, 0, 0, //
                    0, 1, 0, //
                    1, 0, 0}, //

            /*    */{1, 0, 0, //
                    0, 1, 0, //
                    0, 0, 0}, //

            /*    */{0, 0, 1, //
                    0, 1, 0, //
                    0, 0, 0}, //
    };

    public static int[][] LINEJUNCTIONS_KERNEL = new int[][]{//
    /*    */{1, 2, 1, //
            2, 1, 2, //
            2, 1, 2}, //

            /*    */{2, 1, 2, //
                    2, 1, 1, //
                    1, 2, 2}, //

            /*    */{2, 2, 1, //
                    1, 1, 2, //
                    2, 2, 1}, //

            /*    */{1, 2, 2, //
                    2, 1, 1, //
                    2, 1, 2}, //

            /*    */{2, 1, 2, //
                    2, 1, 2, //
                    1, 2, 1}, //

            /*    */{2, 2, 1, //
                    1, 1, 2, //
                    2, 1, 2}, //

            /*    */{1, 2, 2, //
                    2, 1, 1, //
                    1, 2, 2}, //

            /*    */{2, 1, 2, //
                    1, 1, 2, //
                    2, 2, 1}, //

            /*    */{1, 2, 2, //
                    2, 1, 2, //
                    1, 2, 1}, //

            /*    */{1, 2, 1, //
                    2, 1, 2, //
                    1, 2, 2}, //

            /*    */{1, 2, 1, //
                    2, 1, 2, //
                    2, 2, 1}, //

            /*    */{2, 2, 1, //
                    2, 1, 2, //
                    1, 2, 1} //

    };

    public static int getSquareKernelSide( int[] kernel ) {
        double side = Math.sqrt(kernel.length);
        if (side % (int) side != 0) {
            throw new IllegalArgumentException("The kernel has to be square.");
        }
        return (int) side;
    }

    public static int getArrayCenterIndex( int[] kernel ) {
        return (int) Math.floor(kernel.length / 2.0);
    }

    public static int getMatrixCenterIndex( int[][] kernel ) {
        return (int) Math.floor(kernel.length / 2.0);
    }

    public static int[][] getSquareKernelMatrix( int[] squareKernelArray ) {
        int squareKernelSide = getSquareKernelSide(squareKernelArray);
        int index = 0;
        int[][] kernelMatrix = new int[squareKernelSide][squareKernelSide];
        for( int r = 0; r < squareKernelSide; r++ ) {
            for( int c = 0; c < squareKernelSide; c++ ) {
                kernelMatrix[r][c] = squareKernelArray[index++];
            }
        }
        return kernelMatrix;
    }

}
