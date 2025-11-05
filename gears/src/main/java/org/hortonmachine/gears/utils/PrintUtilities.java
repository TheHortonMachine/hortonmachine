/*
 * This file is part of HortonMachine (http://www.hortonmachine.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.hortonmachine.gears.utils;

import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.PrintStream;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.RandomIterFactory;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridEnvelope2D;
import org.geotools.coverage.grid.GridGeometry2D;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.geometry.GeometryUtilities;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;

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
     * @param coverage
     *            the coverage.
     */
    public static void printCoverageData( GridCoverage2D coverage ) {
        RenderedImage renderedImage = coverage.getRenderedImage();
        RandomIter renderedImageIterator = RandomIterFactory.create(renderedImage, null);
        int[] colsRows = CoverageUtilities.getRegionColsRows(coverage);
        for( int r = 0; r < colsRows[1]; r++ ) {
            for( int c = 0; c < colsRows[0]; c++ ) {
                printer.print(renderedImageIterator.getSampleDouble(c, r, 0));
                printer.print(separator);
            }
            printer.println();
        }
    }

    /**
     * Print data of a {@link GridCoverage2D} as java matrix definition.
     * 
     * @param coverage
     *            the coverage.
     */
    public static void printCoverageDataAsMatrix( GridCoverage2D coverage ) {
        printer.println("double[][] matrix = new double[][]{//");
        RenderedImage renderedImage = coverage.getRenderedImage();
        RandomIter renderedImageIterator = RandomIterFactory.create(renderedImage, null);
        int[] colsRows = CoverageUtilities.getRegionColsRows(coverage);
        for( int r = 0; r < colsRows[1]; r++ ) {
            if (r == 0) {
                printer.print("/*    */{");
            } else {
                printer.print("{");
            }
            for( int c = 0; c < colsRows[0]; c++ ) {
                printer.print(renderedImageIterator.getSampleDouble(c, r, 0));
                if (c < colsRows[0] - 1) {
                    printer.print(", ");
                }
            }
            if (r < colsRows[1] - 1) {
                printer.println("}, //");
            } else {
                printer.println("} //");
            }
        }
        printer.println("};");
        renderedImageIterator.done();
    }

    /**
     * Print data from a {@link RenderedImage}.
     * 
     * @param renderedImage
     *            the image.
     */
    public static void printRenderedImageData( RenderedImage renderedImage ) {
        RandomIter iter = RandomIterFactory.create(renderedImage, null);
        int cols = renderedImage.getWidth();
        int rows = renderedImage.getHeight();
        int numBands = renderedImage.getSampleModel().getNumBands();
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                for( int b = 0; b < numBands; b++ ) {
                    if (b > 0) {
                        printer.print("/");
                    }
                    printer.print(iter.getSampleDouble(c, r, b));
                }
                printer.print(separator);
            }
            printer.println();
        }
        iter.done();
    }

    /**
     * Print data from a {@link Raster}.
     * 
     * @param raster
     *            the image.
     */
    public static void printWritableRasterData( Raster raster ) {
        RandomIter iter = RandomIterFactory.create(raster, null);
        int cols = raster.getWidth();
        int rows = raster.getHeight();
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                printer.print(iter.getSampleDouble(c, r, 0));
                printer.print(separator);
            }
            printer.println();
        }
        iter.done();
    }

    /**
     * Print data from a matrix.
     * 
     * @param matrix
     *            the matrix.
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

    /**
     * Print the envelope as WKT.
     * 
     * @param env
     *            the {@link org.locationtech.jts.geom.Envelope}.
     * @return the WKT string.
     */
    public static String envelope2WKT( org.locationtech.jts.geom.Envelope env ) {
        GeometryFactory gf = GeometryUtilities.gf();
        Geometry geometry = gf.toGeometry(env);
        return geometry.toText();
    }

    public static String toString( GridCoverage2D coverage ) {
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(coverage);
        StringBuilder sb = new StringBuilder();
        sb.append(regionMap.toStringJGT()).append("\n");
        var envelope2d = coverage.getEnvelope2D();
        String envelope2wkt = envelope2WKT(envelope2d);
        sb.append("WKT bounds: \n");
        sb.append(envelope2wkt);
        return sb.toString();
    }

    public static String getRegionPrint( GridCoverage2D coverage ) {
        var envelope = coverage.getEnvelope();

        var lowerCorner = envelope.getLowerCorner();
        double[] westSouth = lowerCorner.getCoordinate();
        var upperCorner = envelope.getUpperCorner();
        double[] eastNorth = upperCorner.getCoordinate();

        GridGeometry2D gridGeometry = coverage.getGridGeometry();
        GridEnvelope2D gridRange = gridGeometry.getGridRange2D();

        String numberSpace = "                ";
        String numberRest = "                  ";

        StringBuilder sb = new StringBuilder();
        sb.append("        +----------------------   ").append(String.format("%17.8f", eastNorth[1]))
                .append("   ----------------------+").append("\n");
        sb.append("        +                         ").append(String.format("%17.8f", gridRange.getMinY()))
                .append("                         +").append("\n");
        sb.append("        +                         ").append(numberSpace).append("                          +").append("\n");
        sb.append("        +                         ").append(numberSpace).append("                          +").append("\n");
        sb.append(String.format("%17.8f", westSouth[0])).append(numberRest).append(numberSpace).append(numberRest)
                .append(String.format("%17.8f", eastNorth[0])).append("\n");
        sb.append(String.format("%17.8f", gridRange.getMinX())).append(numberRest).append(numberSpace).append(numberRest)
                .append(String.format("%17.8f", gridRange.getMaxX())).append("\n");
        sb.append("        +                         ").append(numberSpace).append("                          +").append("\n");
        sb.append("        +                         ").append(numberSpace).append("                          +").append("\n");
        sb.append("        +                         ").append(String.format("%17.8f", gridRange.getMaxY()))
                .append("                         +").append("\n");
        sb.append("        +----------------------   ").append(String.format("%17.8f", westSouth[1]))
                .append("   ----------------------+").append("\n");

        return sb.toString();
    }

}
