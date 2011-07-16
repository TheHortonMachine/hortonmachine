/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.jgrasstools.gears.utils;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.PrintStream;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

/**
 * Utility class to print data out.
 * 
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class PrintUtilities {

    private static String separator = " ";
    private static PrintStream printer = System.out;

    /**
     * Print data of a {@link GridCoverage2D}.
     * 
     * @param coverage the coverage.
     */
    public static void printCoverageData( GridCoverage2D coverage ) {
        RenderedImage renderedImage = coverage.getRenderedImage();
        RandomIter renderedImageIterator = RandomIterFactory.create(renderedImage, null);
        int[] colsRows = CoverageUtilities.getRegionColsRows(coverage);
        for( int c = 0; c < colsRows[0]; c++ ) {
            for( int r = 0; r < colsRows[1]; r++ ) {
                printer.print(renderedImageIterator.getSampleDouble(c, r, 0));
                printer.print(separator);
            }
            printer.println();
        }
    }

    /**
     * Print data from a {@link RenderedImage}.
     * 
     * @param renderedImage the image.
     */
    public static void printRenderedImageData( RenderedImage renderedImage ) {
        RandomIter netIter = RandomIterFactory.create(renderedImage, null);
        int cols = renderedImage.getWidth();
        int rows = renderedImage.getHeight();
        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
                printer.print(netIter.getSampleDouble(c, r, 0));
                printer.print(separator);
            }
            printer.println();
        }
    }

    /**
     * Print data from a {@link WritableRaster}.
     * 
     * @param writableRaster the image.
     */
    public static void printWritableRasterData( WritableRaster writableRaster ) {
        RandomIter netIter = RandomIterFactory.create(writableRaster, null);
        int cols = writableRaster.getWidth();
        int rows = writableRaster.getHeight();
        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
                printer.print(netIter.getSampleDouble(c, r, 0));
                printer.print(separator);
            }
            printer.println();
        }
    }

    /**
     * Print data from a matrix.
     * 
     * @param matrix the matrix.
     */
    public static void printMatrixData( double[][] matrix ) {
        int cols = matrix[0].length;
        int rows = matrix.length;
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                printer.print(matrix[r][c]);
                printer.print(separator);
            }
            printer.println();
        }
    }

}
