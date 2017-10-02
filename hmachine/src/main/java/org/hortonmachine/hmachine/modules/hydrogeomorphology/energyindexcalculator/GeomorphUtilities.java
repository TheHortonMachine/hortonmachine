/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.hortonmachine.hmachine.modules.hydrogeomorphology.energyindexcalculator;

import static java.lang.Math.floor;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.tan;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;

import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;

/**
 * @author Stefano Endrizzi
 */
public class GeomorphUtilities {

    public void orizzonte1( double delta, int quadrata, double beta, double alfa, RandomIter elevImageIterator,
            WritableRaster curvatureImage, int[][] shadow ) {
        int rows = curvatureImage.getHeight();
        int cols = curvatureImage.getWidth();

        /*=====================*/
        int y, I, J;
        double zenith;
        /*======================*/

        if (beta != 0) {
            for( int j = 0; j < quadrata; j++ ) {
                I = -1;
                J = -1;
                y = 0;
                for( int jj = j; jj >= 0; jj-- ) {
                    for( int i = (int) floor(1 / tan(beta) * (j - jj)); i <= (int) floor(1 / tan(beta) * (j - jj + 1)) - 1
                            && i < rows; i++ ) {
                        if (jj < cols && !isNovalue(elevImageIterator.getSampleDouble(jj, i, 0))) {
                            /*shadow->element[i][jj]=j;}}}}}*/
                            if (curvatureImage.getSampleDouble(jj, i, 0) == 1 && I == -1) {
                                I = i;
                                J = jj;
                                y = 1;
                            } else if (curvatureImage.getSampleDouble(jj, i, 0) == 1 && I != -1) {
                                zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator
                                        .getSampleDouble(jj, i, 0))
                                        / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                                + pow((double) (J - jj) * (double) delta, (double) 2));
                                if (zenith <= tan(alfa)) {
                                    shadow[i][jj] = 0;
                                    I = i;
                                    J = jj;
                                } else {
                                    shadow[i][jj] = 1;
                                }
                            } else if (curvatureImage.getSampleDouble(jj, i, 0) == 0 && y == 1) {
                                zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator
                                        .getSampleDouble(jj, i, 0))
                                        / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                                + pow((double) (J - jj) * (double) delta, (double) 2));
                                if (zenith <= tan(alfa)) {
                                    shadow[i][jj] = 0;
                                    y = 0;
                                } else {
                                    shadow[i][jj] = 1;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for( int j = 0; j < cols; j++ ) {
                I = -1;
                J = -1;
                y = 0;
                for( int i = 0; i < rows; i++ ) {
                    if (!isNovalue(elevImageIterator.getSampleDouble(j, i, 0))) {
                        if (curvatureImage.getSampleDouble(j, i, 0) == 1 && I == -1) {
                            I = i;
                            J = j;
                            y = 1;
                        } else if (curvatureImage.getSampleDouble(j, i, 0) == 1 && I != -1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, i, 0))
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][j] = 0;
                                I = i;
                                J = j;
                            } else {
                                shadow[i][j] = 1;
                            }
                        } else if (curvatureImage.getSampleDouble(j, i, 0) == 0 && y == 1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, i, 0))
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][j] = 0;
                                y = 0;
                            } else {
                                shadow[i][j] = 1;
                            }
                        }
                    }
                }
            }
        }
    }

    /*----------------------------------------------------------------------------------------------------------*/
    public void orizzonte2( double delta, int quadrata, double beta, double alfa, RandomIter elevImageIterator,
            WritableRaster curvatureImage, int[][] shadow ) {
        int rows = curvatureImage.getHeight();
        int cols = curvatureImage.getWidth();
        /*=====================*/

        int y, I, J;
        double zenith;

        /*======================*/

        if (beta != 0) {
            for( int i = quadrata; i >= 0; i-- ) {
                I = -1;
                J = -1;
                y = 0;
                for( int ii = i; ii < quadrata; ii++ ) {
                    for( int j = cols - (int) floor(1 / tan(beta) * (ii - i)) - 1; j >= cols
                            - (int) floor(1 / tan(beta) * (ii - i + 1)) - 1
                            && j >= 0; j-- ) {
                        if (ii >= (rows + 2 * cols)
                                && !isNovalue(elevImageIterator.getSampleDouble(j, ii - (rows + 2 * cols), 0))) {
                            /*shadow->element[ii-(Z0->nrh+2*Z0->nch)][j]=i}}}}}*/
                            if (curvatureImage.getSampleDouble(j, ii - (rows + 2 * cols), 0) == 1 && I == -1) {
                                I = ii - (rows + 2 * cols);
                                J = j;
                                y = 1;
                            } else if (curvatureImage.getSampleDouble(j, ii - (rows + 2 * cols), 0) == 1 && I != -1) {
                                zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, ii
                                        - (rows + 2 * cols), 0))
                                        / sqrt(Math.pow((double) (I - (ii - (rows + 2 * cols))) * (double) delta, (double) 2)
                                                + pow((double) (J - j) * (double) delta, (double) 2));
                                if (zenith <= tan(alfa)) {
                                    shadow[ii - (rows + 2 * cols)][j] = 0;
                                    I = ii - (rows + 2 * cols);
                                    J = j;
                                } else {
                                    shadow[ii - (rows + 2 * cols)][j] = 1;
                                }
                            } else if (curvatureImage.getSampleDouble(j, ii - (rows + 2 * cols), 0) == 0 && y == 1) {
                                zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, ii
                                        - (rows + 2 * cols), 0))
                                        / sqrt(Math.pow((double) (I - (ii - (rows + 2 * cols))) * (double) delta, (double) 2)
                                                + pow((double) (J - j) * (double) delta, (double) 2));
                                if (zenith <= tan(alfa)) {
                                    shadow[ii - (rows + 2 * cols)][j] = 0;
                                    y = 0;
                                } else {
                                    shadow[ii - (rows + 2 * cols)][j] = 1;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for( int i = 0; i < rows; i++ ) {
                I = -1;
                J = -1;
                y = 0;
                for( int j = cols - 1; j >= 0; j-- ) {
                    if (!isNovalue(elevImageIterator.getSampleDouble(j, i, 0))) {
                        if (curvatureImage.getSampleDouble(j, i, 0) == 1 && I == -1) {
                            I = i;
                            J = j;
                            y = 1;
                        } else if (curvatureImage.getSampleDouble(j, i, 0) == 1 && I != -1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, i, 0))
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][j] = 0;
                                I = i;
                                J = j;
                            } else {
                                shadow[i][j] = 1;
                            }
                        } else if (curvatureImage.getSampleDouble(j, i, 0) == 0 && y == 1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, i, 0))
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][j] = 0;
                                y = 0;
                            } else {
                                shadow[i][j] = 1;
                            }
                        }
                    }
                }
            }
        }

    }

    /*----------------------------------------------------------------------------------------------------------*/
    public void orizzonte3( double delta, int quadrata, double beta, double alfa, RandomIter elevImageIterator,
            WritableRaster curvatureImage, int[][] shadow ) {
        int rows = curvatureImage.getHeight();
        int cols = curvatureImage.getWidth();
        /*=====================*/

        int y, I, J;
        double zenith;

        /*======================*/

        for( int i = 0; i < quadrata; i++ ) {
            I = -1;
            J = -1;
            y = 0;
            for( int ii = i; ii >= 0; ii-- ) {
                for( int j = cols - (int) floor(1.0 / tan(beta) * (i - ii)) - 1; j >= cols
                        - (int) floor(1.0 / tan(beta) * (i - ii + 1)) - 1
                        && j >= 0; j-- ) {
                    if (ii < rows && !isNovalue(elevImageIterator.getSampleDouble(j, ii, 0))) {
                        /*shadow->element[ii][j]=i;}}}}*/
                        if (curvatureImage.getSampleDouble(j, ii, 0) == 1 && I == -1) {
                            I = ii;
                            J = j;
                            y = 1;
                        } else if (curvatureImage.getSampleDouble(j, ii, 0) == 1 && I != -1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, ii, 0))
                                    / sqrt(pow((double) (I - ii) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[ii][j] = 0;
                                I = ii;
                                J = j;
                            } else {
                                shadow[ii][j] = 1;
                            }
                        } else if (curvatureImage.getSampleDouble(j, ii, 0) == 0 && I != -1 && y == 1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, ii, 0))
                                    / sqrt(pow((double) (I - ii) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[ii][j] = 0;
                                y = 0;
                            } else {
                                shadow[ii][j] = 1;
                            }
                        }
                        // System.out.println(ii + " " + j + "       " + shadow[ii][j]);
                    }
                }
            }
        }

    }

    public void orizzonte3tmp( double delta, int quadrata, double beta, double alfa, double[][] Z0, int[][] curv, int[][] shadow,
            double novalue ) {
        int i, j, ii, y, I, J;
        double zenith;

        for( i = 0; i < quadrata; i++ ) {
            I = -1;
            J = -1;
            y = 0;
            for( ii = i; ii >= 0; ii-- ) {
                for( j = Z0[0].length - 1 - (int) floor(1.0 / tan(beta) * (double) (i - ii)); j >= Z0[0].length - 1
                        - floor(1.0 / tan(beta) * (i - ii + 1))
                        && j >= 0; j-- ) {
                    if (ii < Z0.length && Z0[ii][j] != novalue) {
                        /*shadow.element[ii][j]=i;}}}}*/
                        if (curv[ii][j] == 1 && I == -1) {
                            I = ii;
                            J = j;
                            y = 1;
                        } else if (curv[ii][j] == 1 && I != -1) {
                            zenith = (Z0[I][J] - Z0[ii][j])
                                    / sqrt(pow((double) (I - ii) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[ii][j] = 0;
                                I = ii;
                                J = j;
                            } else {
                                shadow[ii][j] = 1;
                            }
                        } else if (curv[ii][j] == 0 && I != 0 && y == 1) {
                            zenith = (Z0[I][J] - Z0[ii][j])
                                    / sqrt(pow((double) (I - ii) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[ii][j] = 0;
                                y = 0;
                            } else {
                                shadow[ii][j] = 1;
                            }
                        }

                    }
                }
            }
        }
    } /*----------------------------------------------------------------------------------------------------------*/

    public void orizzonte4( double delta, int quadrata, double beta, double alfa, RandomIter elevImageIterator,
            WritableRaster curvatureImage, int[][] shadow ) {
        int rows = curvatureImage.getHeight();
        int cols = curvatureImage.getWidth();
        /*=====================*/

        int y, I, J;
        double zenith;

        /*======================*/

        if (beta != 0) {
            for( int j = 0; j < quadrata; j++ ) {
                I = -1;
                J = -1;
                y = 0;
                for( int jj = j; jj >= 0; jj-- ) {
                    for( int i = rows - (int) floor(1 / tan(beta) * (j - jj)) - 1; i >= rows
                            - (int) floor(1 / tan(beta) * (j - jj + 1)) - 1
                            && i >= 0; i-- ) {
                        if (jj < cols && !isNovalue(elevImageIterator.getSampleDouble(jj, i, 0))) {
                            /*shadow.element[i][jj]=j;}}}}}*/
                            if (curvatureImage.getSampleDouble(jj, i, 0) == 1 && I == -1) {
                                I = i;
                                J = jj;
                                y = 1;
                            } else if (curvatureImage.getSampleDouble(jj, i, 0) == 1 && I != -1) {
                                zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator
                                        .getSampleDouble(jj, i, 0))
                                        / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                                + pow((double) (J - jj) * (double) delta, (double) 2));
                                if (zenith <= tan(alfa)) {
                                    shadow[i][jj] = 0;
                                    I = i;
                                    J = jj;
                                } else {
                                    shadow[i][jj] = 1;
                                }
                            } else if (curvatureImage.getSampleDouble(jj, i, 0) == 0 && y == 1) {
                                zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator
                                        .getSampleDouble(jj, i, 0))
                                        / sqrt((double) pow((I - i) * (double) delta, (double) 2)
                                                + pow((double) (J - jj) * (double) delta, (double) 2));
                                if (zenith <= tan(alfa)) {
                                    shadow[i][jj] = 0;
                                    y = 0;
                                } else {
                                    shadow[i][jj] = 1;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for( int j = 0; j < cols; j++ ) {
                I = -1;
                J = -1;
                y = 0;
                for( int i = rows - 1; i >= 0; i-- ) {
                    if (!isNovalue(elevImageIterator.getSampleDouble(j, i, 0))) {
                        if (curvatureImage.getSampleDouble(j, i, 0) == 1 && I == -1) {
                            I = i;
                            J = j;
                            y = 1;
                        } else if (curvatureImage.getSampleDouble(j, i, 0) == 1 && I != -1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, i, 0))
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][j] = 0;
                                I = i;
                                J = j;
                            } else {
                                shadow[i][j] = 1;
                            }
                        } else if (curvatureImage.getSampleDouble(j, i, 0) == 0 && y == 1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, i, 0))
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][j] = 0;
                                y = 0;
                            } else {
                                shadow[i][j] = 1;
                            }
                        }
                    }
                }
            }
        }

    }

    /*----------------------------------------------------------------------------------------------------------*/
    public void orizzonte5( double delta, int quadrata, double beta, double alfa, RandomIter elevImageIterator,
            WritableRaster curvatureImage, int[][] shadow ) {
        int rows = curvatureImage.getHeight();
        int cols = curvatureImage.getWidth();
        /*=====================*/

        int y, I, J;
        double zenith;

        /*======================*/

        for( int j = quadrata; j >= 0; j-- ) {
            I = -1;
            J = -1;
            y = 0;
            for( int jj = j; jj < quadrata; jj++ ) {
                for( int i = rows - (int) floor(1 / tan(beta) * (jj - j)) - 1; i >= rows
                        - (int) floor(1 / tan(beta) * (jj - j + 1)) - 1
                        && i >= 0; i-- ) {
                    if (jj >= quadrata - cols && !isNovalue(elevImageIterator.getSampleDouble(jj - (quadrata - cols), i, 0))) {
                        /*shadow.element[i][jj-(quadrata-Z0.nch)]=j;}}}}*/
                        if (curvatureImage.getSampleDouble(jj - (quadrata - cols), i, 0) == 1 && I == -1) {
                            I = i;
                            J = jj - (quadrata - cols);
                            y = 1;
                        } else if (curvatureImage.getSampleDouble(jj - (quadrata - cols), i, 0) == 1 && I != -1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(jj
                                    - (quadrata - cols), i, 0))
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - (jj - (quadrata - cols))) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][jj - (quadrata - cols)] = 0;
                                I = i;
                                J = jj - (quadrata - cols);
                            } else {
                                shadow[i][jj - (quadrata - cols)] = 1;
                            }
                        } else if (curvatureImage.getSampleDouble(jj - (quadrata - cols), i, 0) == 0 && y == 1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(jj
                                    - (quadrata - cols), i, 0))
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - (jj - (quadrata - cols))) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][jj - (quadrata - cols)] = 0;
                                y = 0;
                            } else {
                                shadow[i][jj - (quadrata - cols)] = 1;
                            }
                        }
                    }
                }
            }
        }

    }

    /*----------------------------------------------------------------------------------------------------------*/
    public void orizzonte6( double delta, int quadrata, double beta, double alfa, RandomIter elevImageIterator,
            WritableRaster curvatureImage, int[][] shadow ) {
        int rows = curvatureImage.getHeight();
        int cols = curvatureImage.getWidth();
        /*=====================*/

        int y, I, J;
        double zenith;

        /*======================*/

        if (beta != 0) {
            for( int i = 0; i < quadrata; i++ ) {
                I = -1;
                J = -1;
                y = 0;
                for( int ii = i; ii >= 0; ii-- ) {
                    for( int j = (int) floor(1 / tan(beta) * (i - ii)); j <= (int) floor(1 / tan(beta) * (i - ii + 1)) - 1
                            && j < cols; j++ ) {
                        if (ii < rows && !isNovalue(elevImageIterator.getSampleDouble(j, ii, 0))) {
                            /*shadow.element[ii][j]=i;}}}}}*/
                            if (curvatureImage.getSampleDouble(j, ii, 0) == 1 && I == -1) {
                                I = ii;
                                J = j;
                                y = 1;
                            } else if (curvatureImage.getSampleDouble(j, ii, 0) == 1 && I != -1) {
                                zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator
                                        .getSampleDouble(j, ii, 0))
                                        / sqrt(pow((double) (I - ii) * (double) delta, (double) 2)
                                                + pow((double) (J - j) * (double) delta, (double) 2));
                                if (zenith <= tan(alfa)) {
                                    shadow[ii][j] = 0;
                                    I = ii;
                                    J = j;
                                } else {
                                    shadow[ii][j] = 1;
                                }
                            } else if (curvatureImage.getSampleDouble(j, ii, 0) == 0 && y == 1) {
                                zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator
                                        .getSampleDouble(j, ii, 0))
                                        / sqrt((double) pow((I - ii) * (double) delta, (double) 2)
                                                + pow((double) (J - j) * (double) delta, (double) 2));
                                if (zenith <= tan(alfa)) {
                                    shadow[ii][j] = 0;
                                    y = 0;
                                } else {
                                    shadow[ii][j] = 1;
                                }
                            }
                        }
                    }
                }
            }
        } else {
            for( int i = 0; i < rows; i++ ) {
                I = -1;
                J = -1;
                y = 0;
                for( int j = 0; j < cols; j++ ) {
                    if (!isNovalue(elevImageIterator.getSampleDouble(j, i, 0))) {
                        if (curvatureImage.getSampleDouble(j, i, 0) == 1 && I == -1) {
                            I = i;
                            J = j;
                            y = 1;
                        } else if (curvatureImage.getSampleDouble(j, i, 0) == 1 && I != -1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, i, 0))
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][j] = 0;
                                I = i;
                                J = j;
                            } else {
                                shadow[i][j] = 1;
                            }
                        } else if (curvatureImage.getSampleDouble(j, i, 0) == 0 && y == 1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, i, 0))
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][j] = 0;
                                y = 0;
                            } else {
                                shadow[i][j] = 1;
                            }
                        }
                    }
                }
            }
        }

    }

    /*----------------------------------------------------------------------------------------------------------*/
    public void orizzonte7( double delta, int quadrata, double beta, double alfa, RandomIter elevImageIterator,
            WritableRaster curvatureImage, int[][] shadow ) {
        int rows = curvatureImage.getHeight();
        int cols = curvatureImage.getWidth();
        /*=====================*/

        int y, I, J;
        double zenith;

        /*======================*/

        for( int i = quadrata - 1; i >= 0; i-- ) {
            I = -1;
            J = -1;
            y = 0;
            for( int ii = i; ii < quadrata - 1; ii++ ) {
                for( int j = (int) floor(1 / tan(beta) * (ii - i)); j <= (int) floor(1 / tan(beta) * (ii - i + 1)) - 1
                        && j < cols; j++ ) {
                    if (ii >= (rows + 2 * cols) && !isNovalue(elevImageIterator.getSampleDouble(j, ii - (rows + 2 * cols), 0))) {
                        /*shadow.element[ii-(Z0.nrh+2*Z0.nch)][j]=i;}}}}*/
                        if (curvatureImage.getSampleDouble(j, ii - (rows + 2 * cols), 0) == 1 && I == -1) {
                            I = ii - (rows + 2 * cols);
                            J = j;
                            y = 1;
                        } else if (curvatureImage.getSampleDouble(j, ii - (rows + 2 * cols), 0) == 1 && I != -1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, ii
                                    - (rows + 2 * cols), 0))
                                    / sqrt(pow((double) (I - (ii - (rows + 2 * cols))) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[ii - (rows + 2 * cols)][j] = 0;
                                I = ii - (rows + 2 * cols);
                                J = j;
                            } else {
                                shadow[ii - (rows + 2 * cols)][j] = 1;
                            }
                        } else if (curvatureImage.getSampleDouble(j, ii - (rows + 2 * cols), 0) == 0 && y == 1) {
                            zenith = (elevImageIterator.getSampleDouble(J, I, 0) - elevImageIterator.getSampleDouble(j, ii
                                    - (rows + 2 * cols), 0))
                                    / sqrt(pow((double) (I - (ii - (rows + 2 * cols))) * (double) delta, (double) 2)
                                            + pow((double) (J - j) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[ii - (rows + 2 * cols)][j] = 0;
                                y = 0;
                            } else {
                                shadow[ii - (rows + 2 * cols)][j] = 1;
                            }
                        }
                    }
                }
            }
        }

    }

    /*----------------------------------------------------------------------------------------------------------*/
    public void orizzonte8( double delta, int quadrata, double beta, double alfa, double[][] Z0, int[][] curv, int[][] shadow,
            double novalue ) {
        /*=====================*/

        int y, I, J;
        double zenith;

        /*======================*/

        for( int j = quadrata; j >= 0; j-- ) {
            I = -1;
            J = -1;
            y = 0;
            for( int jj = j; jj < quadrata; jj++ ) {
                for( int i = (int) floor(1 / tan(beta) * (jj - j)) + 1; i <= (int) floor(1 / tan(beta) * (jj - j + 1))
                        && i <= Z0.length; i++ ) {
                    if (jj > quadrata - Z0[0].length && Z0[i][jj - (quadrata - Z0[0].length)] != novalue) {
                        /*shadow.element[i][jj-(quadrata-Z0.nch)]=j;}}}}*/
                        if (curv[i][jj] == 1 && I == -1) {
                            I = i;
                            J = jj - (quadrata - Z0[0].length);
                            y = 1;
                        } else if (curv[i][jj - (quadrata - Z0[0].length)] == 1 && I != 1) {
                            zenith = (Z0[I][J] - Z0[i][jj - (quadrata - Z0[0].length)])
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - (jj - (quadrata - Z0[0].length))) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][jj - (quadrata - Z0[0].length)] = 0;
                                I = i;
                                J = jj - (quadrata - Z0[0].length);
                            } else {
                                shadow[i][jj - (quadrata - Z0[0].length)] = 1;
                            }
                        } else if (curv[i][jj - (quadrata - Z0[0].length)] == 0 && y == 1) {
                            zenith = (Z0[I][J] - Z0[i][jj - (quadrata - Z0[0].length)])
                                    / sqrt(pow((double) (I - i) * (double) delta, (double) 2)
                                            + pow((double) (J - (jj - (quadrata - Z0[0].length))) * (double) delta, (double) 2));
                            if (zenith <= tan(alfa)) {
                                shadow[i][jj - (quadrata - Z0[0].length)] = 0;
                                y = 0;
                            } else {
                                shadow[i][jj - (quadrata - Z0[0].length)] = 1;
                            }
                        }
                    }
                }
            }
        }
    }

}
