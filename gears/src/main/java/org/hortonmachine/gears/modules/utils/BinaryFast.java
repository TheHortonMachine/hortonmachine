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
package org.hortonmachine.gears.modules.utils;

import java.awt.Point;
import java.awt.image.RenderedImage;
import java.util.HashSet;
import java.util.Iterator;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

/**
 * Class BinaryFast is a representation of a binary image storing
 * the foreground and background edge pixels in hash tables for efficiency.
 *
 * @author Simon Horne.
 * @author Andrea Antonello (www.hydrologis.com)
 */
public class BinaryFast {

    /**
     * Background is black.
     */
    public static final int BACKGROUND = 0;
    /**
     * Foreground is white.
     */
    public static final int FOREGROUND = 1;
    /**
     * Width of the image.
     */
    private int width;
    /**
     * Height of the image.
     */
    private int height;
    /**
     * Size of the image (w*h), number of pixels.
     */
    private int size;
    /**
     * The 2D array of all pixels.
     */
    private int[][] pixels;
    /**
     * The hash set storing positions of foreground edge pixels as Points.
     */
    private HashSet<Point> foregroundEdgePixels = new HashSet<Point>();
    /**
     * The hash set storing positions of background edge pixels as Points.
     */
    private HashSet<Point> backgroundEdgePixels = new HashSet<Point>();

    /**
     * Create a {@link BinaryFast} object based on a data matrix.
     * 
     * <p>
     * Note that the matrix cycles over matrix[i][j], where i = columns 
     * and j = rows.
     * </p>
     * 
     * @param data the data matrix already in {@link #FOREGROUND}, {@link #BACKGROUND} mode.
     */
    public BinaryFast( int[][] data ) {
        width = data.length;
        height = data[0].length;
        pixels = data; // take care, this matrix is in cols/rows mode
        size = width * height;

        generateForegroundEdge();
        generateBackgroundEdgeFromForegroundEdge();
    }

    public BinaryFast( RenderedImage renderedImage ) {
        width = renderedImage.getWidth();
        height = renderedImage.getHeight();
        pixels = new int[width][height]; // take care, this matrix is in cols/rows mode
        size = width * height;

        RandomIter iter = RandomIterFactory.create(renderedImage, null);
        for( int i = 0; i < pixels.length; i++ ) {
            for( int j = 0; j < pixels[0].length; j++ ) {
                double sample = iter.getSampleDouble(i, j, 0);
                if (sample == 1) {
                    pixels[i][j] = 1;
                } else {
                    pixels[i][j] = 0;
                }
            }
        }

        generateForegroundEdge();
        generateBackgroundEdgeFromForegroundEdge();
    }

    /**
     * Removes a foreground pixel from the 2D array by setting its value
     * to background.
     *
     * @param p The point to be removed.
     */
    public void removePixel( Point p ) {
        pixels[p.x][p.y] = BACKGROUND;
    }

    /**
     * Adds a foreground pixel to the 2D array by setting its value
     * to foreground.
     *
     * @param p The point to be added.
     */
    public void addPixel( Point p ) {
        pixels[p.x][p.y] = FOREGROUND;
    }

    /**
     * Converts the 2D array into a 1D array of pixel values.
     *
     * @return The 1D array of pixel values.
     */
    public int[] convertToArray() {
        int[] p = new int[size];
        for( int j = 0; j < height; ++j ) {
            for( int i = 0; i < width; ++i ) {
                p[(j * width) + i] = pixels[i][j];
            }
        }
        return p;
    }

    /**
     * Generates a new 2D array of pixels from a hash set of 
     * foreground pixels.
     *
     * @param pix The hash set of foreground pixels.
     */
    public void generatePixels( HashSet<Point> pix ) {
        // Reset all pixels to background
        for( int j = 0; j < height; ++j ) {
            for( int i = 0; i < width; ++i ) {
                pixels[i][j] = BACKGROUND;
            }
        }
        convertToPixels(pix);
    }

    /**
     * Adds the pixels from a hash set to the 2D array of pixels.
     *
     * @param pix The hash set of foreground pixels to be added.
     */
    public void convertToPixels( HashSet<Point> pix ) {
        Iterator<Point> it = pix.iterator();
        while( it.hasNext() ) {
            Point p = it.next();
            pixels[p.x][p.y] = FOREGROUND;
        }
    }

    /**
     * Generates the foreground edge hash set from the 2D array of pixels.
     */
    public void generateForegroundEdge() {
        foregroundEdgePixels.clear();
        Point p;
        for( int n = 0; n < height; ++n ) {
            for( int m = 0; m < width; ++m ) {
                if (pixels[m][n] == FOREGROUND) {
                    p = new Point(m, n);
                    for( int j = -1; j < 2; ++j ) {
                        for( int i = -1; i < 2; ++i ) {
                            if ((p.x + i >= 0) && (p.x + i < width) && (p.y + j >= 0)
                                    && (p.y + j < height)) {
                                if ((pixels[p.x + i][p.y + j] == BACKGROUND)
                                        && (!foregroundEdgePixels.contains(p))) {
                                    foregroundEdgePixels.add(p);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates the background edge hash set from the foreground edge
     * hash set and the 2D array of pixels.
     */
    public void generateBackgroundEdgeFromForegroundEdge() {
        backgroundEdgePixels.clear();
        Point p, p2;
        Iterator<Point> it = foregroundEdgePixels.iterator();
        while( it.hasNext() ) {
            p = new Point(it.next());
            for( int j = -1; j < 2; ++j ) {
                for( int i = -1; i < 2; ++i ) {
                    if ((p.x + i >= 0) && (p.x + i < width) && (p.y + j >= 0) && (p.y + j < height)) {
                        p2 = new Point(p.x + i, p.y + j);
                        if (pixels[p2.x][p2.y] == BACKGROUND) {
                            backgroundEdgePixels.add(p2);
                        }
                    }
                }
            }
        }
    }

    /**
     * Generates the foreground edge hash set from the background edge hash
     * set and the 2D array of pixels.
     */
    public void generateForegroundEdgeFromBackgroundEdge() {
        foregroundEdgePixels.clear();
        Point p, p2;
        Iterator<Point> it = backgroundEdgePixels.iterator();
        while( it.hasNext() ) {
            p = new Point(it.next());
            for( int j = -1; j < 2; ++j ) {
                for( int i = -1; i < 2; ++i ) {
                    if ((p.x + i >= 0) && (p.x + i < width) && (p.y + j >= 0) && (p.y + j < height)) {
                        p2 = new Point(p.x + i, p.y + j);
                        if (pixels[p2.x][p2.y] == FOREGROUND) {
                            foregroundEdgePixels.add(p2);
                        }
                    }
                }
            }
        }
    }

    /** 
     * Returns the int [] values of the Binary Fast image
     * @return int[] the greylevel array of the image
     */
    public int[] getValues() {
        int[] graylevel = new int[size];
        int[] values1D = convertToArray();
        for( int i = 0; i < size; i++ ) {
            graylevel[i] = values1D[i] & 0x000000ff;
        }
        return graylevel;
    }

    public HashSet<Point> getForegroundEdgePixels() {
        return foregroundEdgePixels;
    }

    public HashSet<Point> getBackgroundEdgePixels() {
        return backgroundEdgePixels;
    }

    public int[][] getPixels() {
        return pixels;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
