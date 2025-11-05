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

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.RectIter;


/**
 * Base class for image iterators with row-column (line-pixel) movement.
 * 
 * @author michael
 */
public abstract class AbstractSimpleIterator {

    /** 
     * Constants defining the visiting order that the iterator will
     * follow when moved with the {@code next()} method. Choices are:
     * <ul>
     * <li>{@linkplain Order#IMAGE_X_Y}
     * <li>{@linkplain Order#TILE_X_Y}
     * </ul>
     */
    public static enum Order {
        /** 
         * The iterator will move by X (pixel) then Y (line) across
         * the whole image.
         */
        IMAGE_X_Y,

        /** 
         * The iterator will move by X (pixel) then Y (line) within
         * each tile of the image in turn. The tiles are visited in 
         * X (tile grid column) then Y (tile grid row) order. This
         * movement order is most efficient when dealing with large
         * images because it minimizes tile-swapping in memory.
         */ 
        TILE_X_Y;
    }

    /**
     * This is implemented by sub-classes to pass a method back to
     * this class to create the delegate iterator. This allows the
     * delegate to be a final field.
     */
    protected static interface DelegateHelper {
        
        /**
         * Creates the delegate iterator.
         * 
         * @param image the target image
         * @param bounds the iterator bounds
         * 
         * @return the new iterator
         */
        RectIter create(RenderedImage image, Rectangle bounds);
    }
    
    
    /** A weak reference to the target image. */
    protected final WeakReference<RenderedImage> imageRef;

    /** The data type of the target image (value of a DataBuffer constant). */
    protected final int imageDataType;

    /** The bounds of this iterator */
    protected final Rectangle iterBounds;

    /** The delegate iterator. */
    protected final RectIter delegateIter;


    // number of bands in the target image
    private final int numImageBands;
    
    // visiting order for this iterator
    private final Order order;

    // the value to return when the iterator is positioned beyond
    // the bounds of the target image; three types are created to
    // save time a little in the getSample method
    private final Integer outsideValue_Integer;
    private final Float outsideValue_Float;
    private final Double outsideValue_Double;

    // list of sub-bounds (a single rectangle for image-wise iteration or
    // a series of tile portions for tile-wise iteration)
    private final List<Rectangle> subBoundList;

    // index of the current sub-bound being processed
    private int   currentSubBound;

    // index of the final sub-bound to process
    private final int lastSubBound;
    
    // start position in the current sub-bounds
    private final Point startSubPos;

    // end position in the current sub-bounds
    private final Point endSubPos;

    // this iterator's current position (will differ from delegate
    // iterator's position when outside target image bounds)
    private final Point mainPos;

    // bounds of the delegate iterator (intersection of iterBounds
    // and the bounds of the target image)
    private final Rectangle delegateBounds;

    // the current delegate position
    private final Point delegatePos;

    
    /**
     * Creates a new instance. The helper object is provided by a sub-class 
     * to create the delegate iterator that will then be held by this class as
     * a final field. The iterator bounds are allowed to extend beyond the 
     * target image bounds. When the iterator is positioned outside the target
     * image area it returns the specified outside value.
     * 
     * @param helper a helper provided by sub-class to create the delegate iterator
     * @param image the target image
     * @param bounds the bounds for this iterator; {@code null} means to use the
     *     target image bounds
     * @param outsideValue value to return when positioned outside the bounds of
     *     the target image
     * @param order order of movement for this iterator
     * 
     * @throws IllegalArgumentException if the image argument is {@code null}
     */
    public AbstractSimpleIterator(DelegateHelper helper, RenderedImage image, 
            Rectangle bounds, Number outsideValue, Order order) {
        
        if (image == null) {
            throw new IllegalArgumentException("image must not be null");
        }
        if (order == null) {
            throw new IllegalArgumentException("order must not be null");
        }
        
        imageRef = new WeakReference<RenderedImage>(image);
        imageDataType = image.getSampleModel().getDataType();
        numImageBands = image.getSampleModel().getNumBands();
        
        final Rectangle imageBounds = new Rectangle(image.getMinX(), image.getMinY(),
                image.getWidth(), image.getHeight());
        
        if (bounds == null) {
            iterBounds = imageBounds;
        } else {
            iterBounds = new Rectangle(bounds);
        }

        delegateBounds = imageBounds.intersection(iterBounds);
        if (delegateBounds.isEmpty()) {
            delegatePos = null;
            delegateIter = null;
            
        } else {
            delegatePos = new Point(delegateBounds.x, delegateBounds.y);
            delegateIter = helper.create(image, delegateBounds);
        }
        
        mainPos = new Point(iterBounds.x, iterBounds.y);
        
        this.outsideValue_Integer = outsideValue == null ? null : outsideValue.intValue();
        this.outsideValue_Float = outsideValue == null ? null : outsideValue.floatValue();
        this.outsideValue_Double = outsideValue == null ? null : outsideValue.doubleValue();
        
        this.order = order;
        this.startSubPos = new Point();
        this.endSubPos = new Point();
        subBoundList = buildSubBoundList(image);
        lastSubBound = subBoundList.size() - 1;

        setCurrentSubBound(0);
    }

    /**
     * Returns the image that this iterator is working with. Note
     * that the iterator only maintains a {@linkplain WeakReference} to
     * the image so it is possible for this method to return {@code null}.
     * 
     * @return the image that this iterator is working with
     */
    public RenderedImage getImage() {
        return imageRef.get();
    }

    /**
     * Gets the bounds of this iterator. Note that these may extend
     * beyond the bounds of the target image.
     * 
     * @return the iterator bounds
     */
    public Rectangle getBounds() {
        return new Rectangle(iterBounds);
    }
    
    /**
     * Gets the list of sub-bounds for this iterator. 
     * When the iterator is working in {@linkplain Order#TILE_X_Y} processing order
     * it creates a list of sub-bound rectangles by intersecting the iterator's overall 
     * bounds with the bounds of the image tiles. The sub-bounds are held in the list
     * in the order in which they will be visited by the iterator (if advanced with
     * the {@linkplain #next()} method).
     * <p>
     * When working in the default {@linkplain Order#IMAGE_X_Y} processing order, this
     * method will return a list containing a single rectangle, identical to that returned
     * by {@linkplain #getBounds()}.
     * <p>
     * The returned {@code List} and the {@code Rectangles} within it are copies, so the 
     * iterator will not be affected by subsequent modifications to them.
     * 
     * @return list of sub-bounds in visiting order
     */
    public List<Rectangle> getSubBounds() {
        List<Rectangle> copy = new ArrayList<Rectangle>();
        for (Rectangle r : subBoundList) {
            copy.add(new Rectangle(r));
        }
        
        return copy;
    }

    /**
     * Gets the starting position for this iterator. This is the upper-left
     * point of the iterator's bounding rectangle. Note that it may lie
     * outside the target image.
     * 
     * @return the starting position
     */
    public Point getStartPos() {
        return new Point(iterBounds.x, iterBounds.y);
    }

    /**
     * Gets the final position for this iterator. This is the lower-right
     * point of the iterator's bounding rectangle. Note that it may lie
     * outside the target image.
     * 
     * @return the end position
     */
    public Point getEndPos() {
        return new Point(iterBounds.x + iterBounds.width - 1, iterBounds.y + iterBounds.height - 1);
    }

    /**
     * Tests whether the iterator is currently positioned within the bounds of 
     * the target image.
     * 
     * @return {@code true} if within the target image; {@code false} otherwise
     */
    public boolean isWithinImage() {
        return delegatePos != null && mainPos.equals(delegatePos);
    }

    /**
     * Tests if this iterator can be advanced further, ie. if a call to 
     * {@link #next()} would return {@code true}.
     * 
     * @return {@code true} if the iterator can be advanced; 
     *     {@code false} if it is at the end of its bounds
     */
    public boolean hasNext() {
        return (currentSubBound < lastSubBound || mainPos.x < endSubPos.x || mainPos.y < endSubPos.y);
    }

    /**
     * Advances the iterator to the next position. The iterator moves by
     * column (pixel), then row (line). It is always safe to call this
     * method speculatively.
     * 
     * @return {@code true} if the iterator was successfully advanced;
     *     {@code false} if it was already at the end of its bounds
     */
    public boolean next() {
        if (hasNext()) {
            mainPos.x++ ;
            if (mainPos.x > endSubPos.x) {
                mainPos.x = startSubPos.x;
                mainPos.y++ ;
                
                if (mainPos.y > endSubPos.y) {
                    setCurrentSubBound(currentSubBound + 1);
                    mainPos.setLocation(startSubPos);
                }
            }
            setDelegatePosition();
            return true;
        }

        return false;
    }

    /**
     * Resets the iterator to its first position.
     */
    public void reset() {
        setPos(iterBounds.x, iterBounds.y);
    }

    /**
     * Gets the current iterator position. It is always safe to call
     * this method.
     * 
     * @return current position
     */
    public Point getPos() {
        return getPos(null);
    }
    
    /**
     * Gets the current iterator position. If {@code dest} is not {@code null}
     * it will be set to the current position and returned, otherwise a new
     * Point instance is returned. It is always safe to call this method.
     * 
     * @return current position
     */
    public Point getPos(Point dest) {
        if (dest == null) {
            dest = new Point(mainPos);
        } else {
            dest.setLocation(mainPos);
        }
        
        return dest;
    }

    /**
     * Sets the iterator to a new position. Note that a return value of
     * {@code true} indicates that the new position was valid. If the new position
     * is equal to the iterator's current position this method will still
     * return {@code true} even though the iterator has not moved.
     * <p>
     * If the new position is outside this iterator's bounds, the position remains
     * unchanged and {@code false} is returned.
     * 
     * @param pos the new position
     * @return {@code true} if the new position was valid; {@code false}
     *     otherwise
     * 
     * @throws IllegalArgumentException if {@code pos} is {@code null}
     */
    public boolean setPos(Point pos) {
        if (pos == null) {
            throw new IllegalArgumentException("pos must not be null");
        }
        return setPos(pos.x, pos.y);
    }

    /**
     * Sets the iterator to a new position. Note that a return value of
     * {@code true} indicates that the new position was valid. If the new position
     * is equal to the iterator's current position this method will still
     * return {@code true} even though the iterator has not moved.
     * <p>
     * If the new position is outside this iterator's bounds, the position remains
     * unchanged and {@code false} is returned.
     * 
     * @param x image X position
     * @param y image Y position
     * @return {@code true} if the new position was valid; {@code false}
     *     otherwise
     */
    public boolean setPos(int x, int y) {
        if (iterBounds.contains(x, y)) {
            int index = findSubBound(x, y);
            setCurrentSubBound(index);
            mainPos.setLocation(x, y);
            setDelegatePosition();
            return true;
        }
        
        return false;
    }

    /**
     * Returns the value from the first band of the image at the current position,
     * or the outside value if the iterator is positioned beyond the image bounds.
     * 
     * @return image or outside value
     */
    public Number getSample() {
        return getSample(0);
    }

    /**
     * Returns the value from the first band of the image at the specified position,
     * If the position is within the iterator's bounds, but outside the target
     * image bounds, the outside value is returned. After calling this method, the
     * iterator will be set to the specified position.
     * <p>
     * If the position is outside the iterator's bounds, {@code null} is returned
     * and the iterator's position will remain unchanged.
     * 
     * @param pos the position to sample
     * @return image, outside value or {@code null}
     * 
     * @throws IllegalArgumentException if {@code pos} is {@code null}
     */
    public Number getSample(Point pos) {
        if (pos == null) {
            throw new IllegalArgumentException("pos must not be null");
        }
        return getSample(pos.x, pos.y);
    }

    /**
     * Returns the value from the first band of the image at the specified position,
     * If the position is within the iterator's bounds, but outside the target
     * image bounds, the outside value is returned. After calling this method, the
     * iterator will be set to the specified position.
     * <p>
     * If the position is outside the iterator's bounds, {@code null} is returned
     * and the iterator's position will remain unchanged.
     * 
     * @param x sampling position X-ordinate
     * @param y sampling position Y-ordinate
     * @return image, outside value or {@code null}
     */
    public Number getSample(int x, int y) {
        if (setPos(x, y)) {
            return getSample();
        } else {
            return null;
        }
    }

    /**
     * Returns the value from the specified band of the image at the current position,
     * or the outside value if the iterator is positioned beyond the image bounds.
     * 
     * @param band image band
     * @return image or outside value
     * @throws IllegalArgumentException if {@code band} is out of range for the the
     *     target image
     */
    public Number getSample(int band) {
        RenderedImage image = imageRef.get();
        if (image == null) {
            throw new IllegalStateException("Target image has been deleted");
        }

        final boolean inside = delegateBounds.contains(mainPos);
        Number value;

        switch (imageDataType) {
            case DataBuffer.TYPE_DOUBLE:
                if (inside) {
                    value = new Double(delegateIter.getSampleDouble(band));
                } else {
                    value = outsideValue_Double;
                }
                break;

            case DataBuffer.TYPE_FLOAT:
                if (inside) {
                    value = new Float(delegateIter.getSampleFloat(band));
                } else {
                    value = outsideValue_Float;
                }
                break;

            default:
                if (inside) {
                    value = Integer.valueOf(delegateIter.getSample(band));
                } else {
                    value = outsideValue_Integer;
                }
        }

        return value;
    }

    /**
     * Returns the value from the specified band of the image at the specified position,
     * If the position is within the iterator's bounds, but outside the target
     * image bounds, the outside value is returned. After calling this method, the
     * iterator will be set to the specified position.
     * <p>
     * If the position is outside the iterator's bounds, {@code null} is returned
     * and the iterator's position will remain unchanged.
     * 
     * @param pos the position to sample
     * @param band image band
     * @return image, outside value or {@code null}
     * 
     * @throws IllegalArgumentException if {@code pos} is {@code null} or
     *     {@code band} is out of range
     */
    public Number getSample(Point pos, int band) {
        if (pos == null) {
            throw new IllegalArgumentException("pos must not be null");
        }
        return getSample(pos.x, pos.y, band);
    }

    /**
     * Returns the value from the specified band of the image at the specified position,
     * If the position is within the iterator's bounds, but outside the target
     * image bounds, the outside value is returned. After calling this method, the
     * iterator will be set to the specified position.
     * <p>
     * If the position is outside the iterator's bounds, {@code null} is returned
     * and the iterator's position will remain unchanged.
     * 
     * @param x sampling position X-ordinate
     * @param y sampling position Y-ordinate
     * @param band image band
     * @return image, outside value or {@code null}
     * 
     * @throws IllegalArgumentException if {@code band} is out of range
     */
    public Number getSample(int x, int y, int band) {
        if (setPos(x, y)) {
            return getSample(band);
        } else {
            return null;
        }
    }
    
    /**
     * Closes this iterator and frees resources including the iterator's 
     * reference to the source image. Attempting to use the iterator after
     * calling this method will result in an exception being thrown.
     */
    public void done() {
        imageRef.clear();
        if (delegateIter instanceof RandomIter) {
            ((RandomIter) delegateIter).done();
        }
    }

    /**
     * Sets the delegate iterator position. If {@code newPos} is outside
     * the target image bounds, the delegate iterator does not move.
     */
    protected void setDelegatePosition() {
        if (isInsideDelegateBounds()) {
            int dy = mainPos.y - delegatePos.y;
            if (dy < 0) {
                delegateIter.startLines();
                delegatePos.y = delegateBounds.y;
                dy = mainPos.y - delegateBounds.y;
            }

            while (dy > 0) {
                delegateIter.nextLineDone();
                delegatePos.y++ ;
                dy--;
            }

            int dx = mainPos.x - delegatePos.x;
            if (dx < 0) {
                delegateIter.startPixels();
                delegatePos.x = delegateBounds.x;
                dx = mainPos.x - delegateBounds.x;
            }

            while (dx > 0) {
                delegateIter.nextPixelDone();
                delegatePos.x++ ;
                dx--;
            }
        }
    }

    /**
     * Tests if the iterator is currently positioned inside the delegate
     * iterator's area (which must lie within the image bounds).
     * 
     * @return {@code true} if the current position is inside the delegate's bounds;
     *     {@code false} otherwise
     */
    protected boolean isInsideDelegateBounds() {
        return delegateIter != null && delegateBounds.contains(mainPos);
    }

    /**
     * Helper method to check that a band value is valid.
     * 
     * @param band band value
     */
    protected void checkBandArg(int band) {
        if (band < 0 || band >= numImageBands) {
            throw new IllegalArgumentException( String.format(
                    "band argument (%d) is out of range: number of image bands is %d",
                    band, numImageBands) );
        }
    }

    /**
     * Builds the list of sub-bounds, each of which is a Rectangle to (possibly)
     * be processed by this iterator.
     * 
     * @param image the target image
     * @return the list of sub-bounds
     */
    private List<Rectangle> buildSubBoundList(RenderedImage image) {
        List<Rectangle> boundsList = new ArrayList<Rectangle>();
        
        switch (order) {
            case IMAGE_X_Y:
                boundsList.add(iterBounds);
                break;
                
            case TILE_X_Y:
                getIntersectingTileBounds(image, boundsList);
                break;
                
            default:
                throw new IllegalArgumentException("Unrecognized iterator order: " + order);
        }

        return boundsList;
    }

    /**
     * Sets the sub-bound that the iterator is to process next.
     * 
     * @param index position in the list of sub-bounds
     */
    private void setCurrentSubBound(int index) {
        Rectangle r = subBoundList.get(index);
        startSubPos.setLocation(r.x, r.y);
        endSubPos.setLocation(r.x + r.width - 1, r.y + r.height - 1);
        currentSubBound = index;
    }

    /**
     * Finds the iterator sub-bound which contains the given pixel position.
     * Note: the position might lie outside the image bounds.
     * 
     * @param x pixel X ordinate
     * @param y pixel Y ordinate
     * @return the corresponding sub-bound or -1 if the position is outside
     *     any sub-bound
     */
    private int findSubBound(int x, int y) {
        for (int i = 0; i < subBoundList.size(); i++) {
            if (subBoundList.get(i).contains(x, y)) {
                return i;
            }
        }

        return -1;
    }
    
    /**
     * Builds a list of Rectangles, each of which is the intersection of the iterator
     * bounds and a tile's bounds. These are termed sub-bounds.
     * <p>
     * If the iterator bounds extend beyond the image bounds some of the resulting 
     * rectangles may be for non-existent tiles or tile parts.
     * 
     * @param image the target image
     * @param destList the list to receive the sub-bounds
     */
    private void getIntersectingTileBounds(RenderedImage image, List<Rectangle> destList) {
        final int tileWidth = image.getTileWidth();
        final int tileHeight = image.getTileHeight();
        final int xOffset = image.getTileGridXOffset();
        final int yOffset = image.getTileGridYOffset();
        
        // Min and max tile indices may differ from those of the image 
        final int minTileX = pixelToTile(iterBounds.x, xOffset, tileWidth);
        final int minTileY = pixelToTile(iterBounds.y, yOffset, tileHeight);
        final int maxTileX = pixelToTile(iterBounds.x + iterBounds.width - 1, xOffset, tileWidth);
        final int maxTileY = pixelToTile(iterBounds.y + iterBounds.height - 1, yOffset, tileHeight);

        for (int ty = minTileY; ty <= maxTileY; ty++) {
            int y = ty * tileHeight + yOffset;
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                int x = tx * tileWidth + xOffset;
                Rectangle tileRect = new Rectangle(x, y, tileWidth, tileHeight);
                Rectangle within = tileRect.intersection(iterBounds);
                if (!within.isEmpty()) {
                    destList.add(within);
                }
            }
        }
    }

    /**
     * Converts a pixel X or Y ordinate to the corresponding tile X or Y ordinate.
     * 
     * @param ordinate the pixel ordinate
     * @param offset the pixel offset (origin tile's upper-left pixel ordinate)
     * @param dim tile width (for X ordinate) or height (for Y ordinate)
     * @return tile ordinate
     */
    private int pixelToTile(int ordinate, int offset, int dim) {
        ordinate -= offset;
        if (ordinate < 0) {
            ordinate += 1 - dim;
        }
        return ordinate / dim;
    }

}
