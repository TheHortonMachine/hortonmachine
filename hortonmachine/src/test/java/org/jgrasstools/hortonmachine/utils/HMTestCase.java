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
package org.jgrasstools.hortonmachine.utils;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.util.Locale;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.RectIter;
import javax.media.jai.iterator.RectIterFactory;

import org.jgrasstools.gears.libs.monitor.PrintStreamProgressMonitor;

import junit.framework.TestCase;
@SuppressWarnings("nls")
public class HMTestCase extends TestCase {

    protected static final double DELTA = 0.0000001;

    static {
        Locale.setDefault(Locale.ENGLISH);
    }

    /**
     * The progress monitor to be usedd by testcases.
     */
    protected PrintStreamProgressMonitor pm = new PrintStreamProgressMonitor(System.out, System.err);

    public void testDummy() {
        // done to not make the maven test fail
    }

    protected void printImage( RenderedImage image ) {
        RectIter rectIter = RectIterFactory.create(image, null);
        do {
            do {
                double value = rectIter.getSampleDouble();
                System.out.print(value + " ");
            } while( !rectIter.nextPixelDone() );
            rectIter.startPixels();
            System.out.println();
        } while( !rectIter.nextLineDone() );
    }

    protected void printMatrix( double[][] matrix ) {
        for( int j = 0; j < matrix.length; j++ ) {
            for( int i = 0; i < matrix[0].length; i++ ) {
                System.out.print(matrix[j][i] + " ");
            }
            System.out.println();
        }
    }

    protected void checkMatrixEqual( RenderedImage image, double[][] matrix, double delta ) {
        RectIter rectIter = RectIterFactory.create(image, null);
        int y = 0;
        do {
            int x = 0;
            do {
                double value = rectIter.getSampleDouble();
                double expectedResult = matrix[y][x];
                if (isNovalue(value)) {
                    assertTrue(x + " " + y, isNovalue(expectedResult));
                } else {
                    assertEquals(x + " " + y, expectedResult, value, delta);
                }
                x++;
            } while( !rectIter.nextPixelDone() );
            rectIter.startPixels();
            y++;
        } while( !rectIter.nextLineDone() );
    }

    protected void checkEqualsSinlgeValue( RenderedImage image, double expectedResult, double delta ) {
        RectIter rectIter = RectIterFactory.create(image, null);
        int y = 0;
        do {
            int x = 0;
            do {
                double value = rectIter.getSampleDouble();
                if (isNovalue(value)) {
                    assertTrue(x + " " + y, isNovalue(expectedResult));
                } else {
                    assertEquals(x + " " + y, expectedResult, value, delta);
                }
                x++;
            } while( !rectIter.nextPixelDone() );
            rectIter.startPixels();
            y++;
        } while( !rectIter.nextLineDone() );
    }

    protected void checkMatrixEqual( RenderedImage image, double[][] matrix ) {
        RectIter rectIter = RectIterFactory.create(image, null);
        int y = 0;
        do {
            int x = 0;
            do {
                double value = rectIter.getSampleDouble();
                double expectedResult = matrix[y][x];
                if (isNovalue(value)) {
                    assertTrue("Difference at position: " + x + " " + y + " expected NaN, got " + expectedResult,
                            isNovalue(expectedResult));
                } else {
                    assertEquals("Difference at position: " + x + " " + y, expectedResult, value);
                }
                x++;
            } while( !rectIter.nextPixelDone() );
            rectIter.startPixels();
            y++;
        } while( !rectIter.nextLineDone() );

    }

    protected void checkMatrixEqual( Raster image, double[][] matrix ) {
        assertEquals("different dimension", image.getHeight(), matrix.length);
        assertEquals("different dimension", image.getWidth(), matrix[0].length);

        RandomIter randomIter = RandomIterFactory.create(image, null);
        int minX = image.getMinX();
        int minY = image.getMinY();

        for( int j = minY; j < minY + image.getHeight(); j++ ) {
            for( int i = minX; i < minX + image.getWidth(); i++ ) {
                double expectedResult = matrix[i - minX][j - minY];
                double value = randomIter.getSampleDouble(i, j, 0);
                if (isNovalue(value)) {
                    assertTrue("Difference at position: " + i + " " + j, isNovalue(expectedResult));
                } else {
                    assertEquals("Difference at position: " + i + " " + j, expectedResult, value);
                }
            }
        }

    }

    /**
     * Method to translate resources names from class-test path to src resources.
     * 
     * @param classesTestFile the file to translate.
     * @return the resource in the src test folder.
     */
    protected File classesTestFile2srcTestResourcesFile( File classesTestFile ) {
        String classesTestPath = classesTestFile.getAbsolutePath();
        classesTestPath = classesTestPath.replaceFirst("target", "src" + File.separator + File.separator + "test");
        classesTestPath = classesTestPath.replaceFirst("test-classes", "resources");

        File srcTestResourcesFile = new File(classesTestPath);
        return srcTestResourcesFile;
    }

    /**
     * Verifiy a Matrix result.
     * 
     * @param matrix
     * @param expectedMatrix
     * @param tolerance
     */
    protected void checkMatrixEqual( double[][] matrix, double[][] expectedMatrix, double tolerance ) {
        // assertEquals("different dimension", matrix.length,
        // expectedMatrix.length);
        // assertEquals("different dimension", matrix[0].length,
        // expectedMatrix[0].length);

        for( int j = 0; j < matrix.length; j++ ) {
            for( int i = 0; i < matrix[0].length; i++ ) {
                double expectedResult = expectedMatrix[j][i];
                double value = matrix[j][i];
                if (isNovalue(value)) {
                    assertTrue("Difference at position: " + i + " " + j, isNovalue(expectedResult));
                } else {
                    assertEquals("Difference at position: " + i + " " + j, expectedResult, value, tolerance);
                }
            }
        }

    }
}
