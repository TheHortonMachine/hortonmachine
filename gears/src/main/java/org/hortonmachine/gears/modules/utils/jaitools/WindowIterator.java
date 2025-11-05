/* 
 *  Copyright (c) 2011, Michael Bedward. All rights reserved. 
 *   
 *  Redistribution and use in source and binary forms, with or without modification, 
 *  are permitted provided that the following conditions are met: 
 *   
 *  - Redistributions of source code must retain the above copyright notice, this  
 *    list of conditions and the following disclaimer. 
 *   
 *  - Redistributions in binary form must reproduce the above copyright notice, this 
 *    list of conditions and the following disclaimer in the documentation and/or 
 *    other materials provided with the distribution.   
 *   
 *  THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND 
 *  ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED 
 *  WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE 
 *  DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR 
 *  ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES 
 *  (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; 
 *  LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON 
 *  ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT 
 *  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS 
 *  SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE. 
 */   

package org.hortonmachine.gears.modules.utils.jaitools;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.util.Arrays;

/**
 * An image iterator that passes a moving window over an image.
 * <p>
 * Example of use:
 * <pre><code>
 * RenderedImage myImage = ...
 * // Pass a 3x3 window, with the key element at pos (1,1), over band 0
 * // of the image
 * WindowIter iter = new WindowIter(myImage(myImage, null, new Dimension(3,3), new Point(1,1));
 * int[][] dataWindow = new int[3][3];
 * do {
 *     iter.getWindow(dataWindow);
 *     // do something with data
 * } while (iter.next());
 * </code></pre>
 * 
 * As with the JAI {@code RectIter.getSample} methods, alternative {@code getWindow} methods
 * are provided to return values as either integers, floats or doubles, optionally for a 
 * specified image band.
 * <p>
 * Note that control of the iterator position is different to the {@code RectIter} class which
 * has separate methods to advance and reset pixel, line and band position:
 * <ul>
 * <li>
 * The iterator is advanced with the {@link #next} method which handles movement in both
 * X and Y directions.
 * </li>
 * <li>
 * The iterator can be configured to move more than a single pixel / line via the {@code xstep}
 * and {@code ystep} arguments to the full constructor. If the step distance is larger than
 * the corresponding window dimension then some target image pixels will be absent from
 * the data windows returned by the iterator.
 * </li>
 * <li>
 * It is always safe to call the {@code next} method speculatively, although the 
 * {@link #hasNext} method is also provided for convenience.
 * </li>
 * <li>
 * The iterator's position is defined as the coordinates of the target image pixel 
 * at the data window's key element. The current position can be retrieved using the 
 * {@link #getPos} method.
 * </li>
 * </ul>
 * When the moving window is positioned over an edge of the image, those data window cells
 * beyond the image will be filled with a specified outside value. By default this is zero
 * but an alternative value can be provided via the {@code outsideValue} argument to the full
 * constructor.
 * 
 * @author Michael Bedward
 * @since 1.2
 * @version $Id$
 */
public class WindowIterator {

    private static final Number DEFAULT_OUTSIDE_VALUE = Integer.valueOf(0);

    private final Dimension windowDim;
    private final int leftPadding;
    private final int rightPadding;
    private final int topPadding;
    private final int bottomPadding;

    // data buffer dimensions: band, line, pixel
    private final Number[][][] buffers;
    
    private final Number[][] destBuffer;
    private final int bufferWidth;
    
    private final Rectangle iterBounds;
    private final int numImageBands;
    private final int xstep;
    private final int ystep;

    private final Point mainPos;
    private final Point lowerRightPos;
    
    private final SimpleIterator delegate;
    
    // Value to use for out-of-bounds parts of the data window
    private Number outsideValue;

    /**
     * Creates a new iterator. The iterator will advance one pixel at each
     * step and parts of the data window which are outside the image bounds
     * will be filled with zeroes.
     * 
     * @param image the target image
     * @param bounds the bounds for this iterator or {@code null} for the whole image
     * @param windowDim the dimensions of the data window
     * @param keyElement the position of the key element in the data window
     * 
     * @throws IllegalArgumentException if any arguments other than bounds are {@code null};
     *         or if {@code keyElement} does not lie within {@code windowDim}
     */
    public WindowIterator(RenderedImage image, Rectangle bounds, 
            Dimension windowDim, Point keyElement) {
        this(image, bounds, windowDim, keyElement, DEFAULT_OUTSIDE_VALUE);
    }

    /**
     * Creates a new iterator. The iterator will advance one pixel at each
     * step and parts of the data window which are outside the image bounds
     * will be filled with the specified outside value.
     * 
     * @param image the target image
     * @param bounds the bounds for this iterator or {@code null} for the whole image
     * @param windowDim the dimensions of the data window
     * @param keyElement the position of the key element in the data window
     * @param outsideValue value to return for any parts of the data window that are
     *     beyond the bounds of the image
     * 
     * @throws IllegalArgumentException if any arguments other than bounds are {@code null};
     *         or if {@code keyElement} does not lie within {@code windowDim}
     */
    public WindowIterator(RenderedImage image, Rectangle bounds, 
            Dimension windowDim, Point keyElement, Number outsideValue) {
        this(image, bounds, windowDim, keyElement, 1, 1, outsideValue);
    }

    /**
     * Creates a new iterator. 
     * 
     * @param image the target image
     * @param bounds the bounds for this iterator or {@code null} for the whole image
     * @param windowDim the dimensions of the data window
     * @param keyElement the position of the key element in the data window
     * @param xstep step distance in X-direction (pixels)
     * @param ystep step distance in Y-direction (lines)
     * @param outsideValue value to return for any parts of the data window that are
     *     beyond the bounds of the image
     * 
     * @throws IllegalArgumentException if any arguments other than bounds are {@code null};
     *         or if {@code keyElement} does not lie within {@code windowDim};
     *         or if either step distance is less than 1
     */
    public WindowIterator(RenderedImage image, Rectangle bounds, 
            Dimension windowDim, Point keyElement,
            int xstep, int ystep, Number outsideValue) {

        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        if (windowDim == null) {
            throw new IllegalArgumentException("windowDim must not be null");
        }
        if (keyElement == null) {
            throw new IllegalArgumentException("keyElement must not be null");
        }
        if (keyElement.x < 0 || keyElement.x >= windowDim.width ||
            keyElement.y < 0 || keyElement.y >= windowDim.height) {
            throw new IllegalArgumentException(String.format(
                    "The supplied key element position (%d, %d) is invalid for"
                  + "data window dimensions: width=%d height=%d",
                    keyElement.x, keyElement.y, windowDim.width, windowDim.height));
        }
        if (xstep < 1 || ystep < 1) {
            throw new IllegalArgumentException(
                    "The value of both xstep and ystep must be 1 or greater");
        }
        if (outsideValue == null) {
            throw new IllegalArgumentException("outsideValue must not be null");
        }

        if (bounds == null) {
            this.iterBounds = new Rectangle(
                    image.getMinX(), image.getMinY(), image.getWidth(), image.getHeight());
        } else {
            this.iterBounds = new Rectangle(bounds);
        }

        leftPadding = keyElement.x;
        rightPadding = windowDim.width - keyElement.x - 1;
        topPadding = keyElement.y;
        bottomPadding = windowDim.height - keyElement.y - 1;

        // The delegate terator's bounds take into account the position of
        // the ke element in the data window
        Rectangle delegateBounds = new Rectangle(
                iterBounds.x - leftPadding, iterBounds.y - topPadding,
                iterBounds.width + leftPadding + rightPadding,
                iterBounds.height + topPadding + bottomPadding);

        this.delegate = new SimpleIterator(
                image, delegateBounds, outsideValue, SimpleIterator.Order.IMAGE_X_Y);

        this.windowDim = new Dimension(windowDim);
        this.outsideValue = outsideValue;

        this.numImageBands = image.getSampleModel().getNumBands();

        bufferWidth = iterBounds.width + leftPadding + rightPadding;
        buffers = new Number[numImageBands][][];

        for (int b = 0; b < numImageBands; b++) {
            Number[][] bandBuffer = new Number[windowDim.height][];
            for (int i = 0; i < windowDim.height; i++) {
                Number[] ar = new Number[bufferWidth];
                Arrays.fill(ar, this.outsideValue);
                bandBuffer[i] = ar;
            }
            buffers[b] = bandBuffer;
        }

        this.destBuffer = new Number[numImageBands][];
        for (int b = 0; b < numImageBands; b++) {
            destBuffer[b] = new Number[windowDim.width * windowDim.height];
        }

        this.xstep = xstep;
        this.ystep = ystep;
        
        readData(0);
        mainPos = new Point(iterBounds.x, iterBounds.y);
        lowerRightPos = new Point(
                iterBounds.x + iterBounds.width - 1,
                iterBounds.y + iterBounds.height - 1);
    }

    /**
     * Gets the target image coordinates of the pixel currently at the 
     * window key element position. Note that when the iterator has
     * finished this method returns {@code null}.
     * 
     * @return the pixel coordinates
     */
    public Point getPos() {
        return new Point(mainPos);
    }

    /**
     * Tests if this iterator has more data.
     * 
     * @return {@code true} if more data are available; {@code false} otherwise
     */
    public boolean hasNext() {
        return (mainPos.x + xstep <= lowerRightPos.x || mainPos.y + ystep <= lowerRightPos.y );
    }

    /**
     * Advances the iterator using the specified X and Y step distances. 
     * When the right-hand edge of bound rectangle is reached the iterator
     * automatically increments its Y (line) position. If the iterator is already
     * at the end of bounding rectangle this method safely returns {@code false}.
     * 
     * @return {@code true} if the iterator was advanced; {@code false} if it was
     *         already finished
     */
    public boolean next() {
        if (hasNext()) {
            mainPos.x += xstep;
            if (mainPos.x > lowerRightPos.x) {
                mainPos.x = iterBounds.x;
                mainPos.y += ystep;
                readNextData();
            }

            return true;
        }
        
        return false;
    }

    /**
     * Gets the data window at the current iterator position in image band 0 as Number values.
     * If {@code dest} is {@code null} or not equal in size to the data window
     * dimensions a new array will be allocated, otherwise the provided array
     * is filled. In either case, the destination array is returned for convenience.
     * 
     * @param dest destination array or {@code null}
     * @return the filled destination array
     */
    public Number[][] getWindow(Number[][] dest) {
        return getWindow(dest, 0);
    }

    /**
     * Gets the data window at the current iterator position and specified image band 
     * as Number values.
     * If {@code dest} is {@code null} or not equal in size to the data window
     * dimensions a new array will be allocated, otherwise the provided array
     * is filled. In either case, the destination array is returned for convenience.
     * 
     * @param dest destination array or {@code null}
     * @param band the image band from which to retrieve data
     * @return the filled destination array
     */
    public Number[][] getWindow(Number[][] dest, int band) {
        checkBandArg(band);
        
        if (dest == null || dest.length != windowDim.height || dest[0].length != windowDim.width) {
            dest = new Number[windowDim.height][windowDim.width];
        }

        loadDestBuffer(band);
        int k = 0;
        for (int y = 0; y < windowDim.height; y++) {
            for (int x = 0; x < windowDim.width; x++) {
                dest[y][x] = destBuffer[band][k++];
            }
        }
        return dest;
    }

    /**
     * Gets the data window at the current iterator position in image band 0 as integer values.
     * If {@code dest} is {@code null} or not equal in size to the data window
     * dimensions a new array will be allocated, otherwise the provided array
     * is filled. In either case, the destination array is returned for convenience.
     * 
     * @param dest destination array or {@code null}
     * @return the filled destination array
     */
    public int[][] getWindowInt(int[][] dest) {
        return getWindowInt(dest, 0);
    }

    /**
     * Gets the data window at the current iterator position and specified image band 
     * as integer values.
     * If {@code dest} is {@code null} or not equal in size to the data window
     * dimensions a new array will be allocated, otherwise the provided array
     * is filled. In either case, the destination array is returned for convenience.
     * 
     * @param dest destination array or {@code null}
     * @param band the image band from which to retrieve data
     * @return the filled destination array
     */
    public int[][] getWindowInt(int[][] dest, int band) {
        checkBandArg(band);
        
        if (dest == null || dest.length != windowDim.height || dest[0].length != windowDim.width) {
            dest = new int[windowDim.height][windowDim.width];
        }

        loadDestBuffer(band);
        int k = 0;
        for (int y = 0; y < windowDim.height; y++) {
            for (int x = 0; x < windowDim.width; x++) {
                dest[y][x] = destBuffer[band][k++].intValue();
            }
        }
        return dest;
    }

    /**
     * Gets the data window at the current iterator position in image band 0 as float values.
     * If {@code dest} is {@code null} or not equal in size to the data window
     * dimensions a new array will be allocated, otherwise the provided array
     * is filled. In either case, the destination array is returned for convenience.
     * 
     * @param dest destination array or {@code null}
     * @return the filled destination array
     */
    public float[][] getWindowFloat(float[][] dest) {
        return getWindowFloat(dest, 0);
    }

    /**
     * Gets the data window at the current iterator position and specified image band 
     * as float values.
     * If {@code dest} is {@code null} or not equal in size to the data window
     * dimensions a new array will be allocated, otherwise the provided array
     * is filled. In either case, the destination array is returned for convenience.
     * 
     * @param dest destination array or {@code null}
     * @param band the image band from which to retrieve data
     * @return the filled destination array
     */
    public float[][] getWindowFloat(float[][] dest, int band) {
        checkBandArg(band);
        
        if (dest == null || dest.length != windowDim.height || dest[0].length != windowDim.width) {
            dest = new float[windowDim.height][windowDim.width];
        }

        loadDestBuffer(band);
        int k = 0;
        for (int y = 0; y < windowDim.height; y++) {
            for (int x = 0; x < windowDim.width; x++) {
                dest[y][x] = destBuffer[band][k++].floatValue();
            }
        }
        return dest;
    }

    /**
     * Gets the data window at the current iterator position in image band 0 as double values.
     * If {@code dest} is {@code null} or not equal in size to the data window
     * dimensions a new array will be allocated, otherwise the provided array
     * is filled. In either case, the destination array is returned for convenience.
     * 
     * @param dest destination array or {@code null}
     * @return the filled destination array
     */
    public double[][] getWindowDouble(double[][] dest) {
        return getWindowDouble(dest, 0);
    }

    /**
     * Gets the data window at the current iterator position and specified image band 
     * as double values.
     * If {@code dest} is {@code null} or not equal in size to the data window
     * dimensions a new array will be allocated, otherwise the provided array
     * is filled. In either case, the destination array is returned for convenience.
     * 
     * @param dest destination array or {@code null}
     * @param band the image band from which to retrieve data
     * @return the filled destination array
     */
    public double[][] getWindowDouble(double[][] dest, int band) {
        checkBandArg(band);
        
        if (dest == null || dest.length != windowDim.height || dest[0].length != windowDim.width) {
            dest = new double[windowDim.height][windowDim.width];
        }

        loadDestBuffer(band);
        int k = 0;
        for (int y = 0; y < windowDim.height; y++) {
            for (int x = 0; x < windowDim.width; x++) {
                dest[y][x] = destBuffer[band][k++].doubleValue();
            }
        }
        return dest;
    }

    /**
     * Helper for the getWindow methods. Loads the destination buffer from
     * the line buffers.
     */
    private void loadDestBuffer(int band) {
        final int minx = mainPos.x - iterBounds.x;
        final int bufx = minx + leftPadding;
        final int maxx = bufx + rightPadding;

        int k = 0;
        for (int y = 0; y < windowDim.height; y++) {
            for (int x = minx, winX = 0; x <= maxx; x++, winX++) {
                destBuffer[band][k++] = buffers[band][y][x];
            }
        }
    }

    private void readNextData() {
        moveLinesUp();
        skipImageLines();
        int topBufferLine = Math.max(windowDim.height - ystep, 0);
        readData(topBufferLine);
    }

    private void readData(int topBufferLine) {
        for (int line = topBufferLine; line < windowDim.height; line++) {
            for (int x = 0; x < bufferWidth; x++) {
                for (int b = 0; b < numImageBands; b++) {
                    buffers[b][line][x] = delegate.getSample(b);
                }
                delegate.next();
            }
        }
    }

    private void skipImageLines() {
        int nlines = ystep - windowDim.height;
        if (nlines > 0) {
            Point pos = delegate.getPos();
            delegate.setPos(pos.x, pos.y + nlines);
        }
    }
    
    /**
     * Moves lines up in the data buffer by ystep.
     */
    private void moveLinesUp() {
        for (int b = 0; b < numImageBands; b++) {
            if (ystep >= windowDim.height) {
                // just fill lines with the outside value
                for (int y = 0; y < windowDim.height; y++) {
                    Arrays.fill(buffers[b][y], outsideValue);
                }
            } else {
                // shuffle lines up, avoiding cost of allocating new memory
                for (int y = ystep, ynew = 0; y < windowDim.height; y++, ynew++) {
                    Number[] temp = buffers[b][ynew];
                    buffers[b][ynew] = buffers[b][y];
                    Arrays.fill(temp, outsideValue);
                    buffers[b][y] = temp;
                }
            }
        }
    }

    /**
     * Helper method to check that a band value is valid.
     * 
     * @param band band value
     */
    private void checkBandArg(int band) {
        if (band < 0 || band >= numImageBands) {
            throw new IllegalArgumentException( String.format(
                    "band argument (%d) is out of range: number of image bands is %d",
                    band, numImageBands) );
        }
    }

}
