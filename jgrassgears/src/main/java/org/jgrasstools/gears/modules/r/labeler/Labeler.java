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
package org.jgrasstools.gears.modules.r.labeler;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.modules.utils.BinaryFast;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Connected components labeling operation")
@Author(name = "Simon Horne, Andrea Antonello", contact = "http://homepages.inf.ed.ac.uk/rbf/HIPR2/, www.hydrologis.com")
@Keywords("Labeling, Raster")
@Label(JGTConstants.RASTERPROCESSING)
@Name("labeler")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Labeler extends JGTModel {

    @Description("The map to label.")
    @In
    public GridCoverage2D inMap = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The resulting map.")
    @Out
    public GridCoverage2D outMap = null;

    private int d_w;
    private int d_h;
    private int[] dest_1d;
    private int labels[];
    private int numberOfLabels;
    // private boolean labelsValid = false;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }

        final RenderedImage renderedImage = inMap.getRenderedImage();
        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();
        int[] data = new int[width * height];
        RandomIter iter = RandomIterFactory.create(renderedImage, null);
        int index = 0;
        for( int r = 0; r < height; r++ ) {
            for( int c = 0; c < width; c++ ) {
                double value = iter.getSampleDouble(c, r, 0);
                if (isNovalue(value)) {
                    data[index] = BinaryFast.BACKGROUND;
                } else {
                    data[index] = BinaryFast.FOREGROUND;
                }
                index++;
            }
        }

        int[] labelsArray = doLabel(data, width, height);

        WritableRaster dataWR = CoverageUtilities.createWritableRasterFromArray(width, height,
                labelsArray);
        HashMap<String, Double> regionMap = CoverageUtilities
                .getRegionParamsFromGridCoverage(inMap);
        outMap = CoverageUtilities.buildCoverage(
                "morphed", dataWR, regionMap, inMap.getCoordinateReferenceSystem()); //$NON-NLS-1$
    }

    /**
     *doLabel applies the Labeling alogrithm plus offset and scaling
     *The input image is expected to be 8-bit mono 0=black everything else=white
     *@param src1_1d The input pixel array
     *@param width width of the destination image in pixels
     *@param height height of the destination image in pixels
     *@return A pixel array containing the labelled image
     */
    private int[] doLabel( int[] src1_1d, int width, int height ) {

        int nextlabel = 1;
        int nbs[] = new int[4];
        int nbls[] = new int[4];

        // Get size of image and make 1d_arrays
        d_w = width;
        d_h = height;

        dest_1d = new int[d_w * d_h];
        labels = new int[d_w * d_h / 2]; // the most labels there can be is 1/2 of the points in
        // checkerboard

        int result = 0;
        // labelsValid = false; // only set to true once we've complete the task
        // initialise labels
        for( int i = 0; i < labels.length; i++ )
            labels[i] = i;

        int count;
        // now Label the image
        for( int i = 0; i < src1_1d.length; i++ ) {

            int src1rgb = src1_1d[i] & 0x000000ff;

            if (src1rgb == 0) {
                result = 0; // nothing here
            } else {

                // The 4 visited neighbours
                nbs[0] = getNeighbours(src1_1d, i, -1, 0);
                nbs[1] = getNeighbours(src1_1d, i, 0, -1);
                nbs[2] = getNeighbours(src1_1d, i, -1, -1);
                nbs[3] = getNeighbours(src1_1d, i, 1, -1);

                // Their corresponding labels
                nbls[0] = getNeighbourd(dest_1d, i, -1, 0);
                nbls[1] = getNeighbourd(dest_1d, i, 0, -1);
                nbls[2] = getNeighbourd(dest_1d, i, -1, -1);
                nbls[3] = getNeighbourd(dest_1d, i, 1, -1);

                // label the point
                if ((nbs[0] == nbs[1]) && (nbs[1] == nbs[2]) && (nbs[2] == nbs[3]) && (nbs[0] == 0)) {
                    // all neighbours are 0 so gives this point a new label
                    result = nextlabel;
                    nextlabel++;
                } else { // one or more neighbours have already got labels
                    count = 0;
                    int found = -1;
                    for( int j = 0; j < 4; j++ ) {
                        if (nbs[j] != 0) {
                            count += 1;
                            found = j;
                        }
                    }
                    if (count == 1) {
                        // only one neighbour has a label, so assign the same label to this.
                        result = nbls[found];
                    } else {
                        // more than 1 neighbour has a label
                        result = nbls[found];
                        // Equivalence the connected points
                        for( int j = 0; j < 4; j++ ) {
                            if ((nbls[j] != 0) && (nbls[j] != result)) {
                                associate(nbls[j], result);
                            }
                        }
                    }
                }
            }
            dest_1d[i] = result;
        }

        // reduce labels ie 76=23=22=3 -> 76=3
        // done in reverse order to preserve sorting
        for( int i = labels.length - 1; i > 0; i-- ) {
            labels[i] = reduce(i);
        }

        /*now labels will look something like 1=1 2=2 3=2 4=2 5=5.. 76=5 77=5
          this needs to be condensed down again, so that there is no wasted
          space eg in the above, the labels 3 and 4 are not used instead it jumps
          to 5.
          */
        int condensed[] = new int[nextlabel]; // cant be more than nextlabel labels

        count = 0;
        for( int i = 0; i < nextlabel; i++ ) {
            if (i == labels[i])
                condensed[i] = count++;
        }
        // Record the number of labels
        numberOfLabels = count - 1;

        // now run back through our preliminary results, replacing the raw label
        // with the reduced and condensed one, and do the scaling and offsets too

        // Now generate an array of colours which will be used to label the image
        int[] labelColors = new int[numberOfLabels + 1];

        // Variable used to check if the color generated is acceptable
        // boolean acceptColor = false;

        for( int i = 0; i < labelColors.length; i++ ) {
            // acceptColor = false;
            // while( !acceptColor ) {
            // double tmp = Math.random();
            // labelColors[i] = (int) (tmp * 16777215);
            // if (((labelColors[i] & 0x000000ff) < 200) && (((labelColors[i] & 0x0000ff00) >> 8) <
            // 64) && (((labelColors[i] & 0x00ff0000) >> 16) < 64)) {
            // // Color to be rejected so don't set acceptColor
            // } else {
            // acceptColor = true;
            // }
            // }
            // if (i == 0)
            // labelColors[i] = 0;
            labelColors[i] = i;
        }

        for( int i = 0; i < src1_1d.length; i++ ) {
            result = condensed[labels[dest_1d[i]]];
            // result = (int) ( scale * (float) result + oset );
            // truncate if necessary
            // if( result > 255 ) result = 255;
            // if( result < 0 ) result = 0;
            // produce grayscale
            // dest_1d[i] = 0xff000000 | (result + (result << 16) + (result << 8));
            dest_1d[i] = labelColors[result];// + 0xff000000;
        }

        // labelsValid = true; // only set to true now we've complete the task
        return dest_1d;
    }

    /**
     * getNeighbours will get the pixel value of i's neighbour that's ox and oy
     * away from i, if the point is outside the image, then 0 is returned.
     * This version gets from source image.
     */

    private int getNeighbours( int[] src1d, int i, int ox, int oy ) {
        int x, y, result;

        x = (i % d_w) + ox; // d_w and d_h are assumed to be set to the
        y = (i / d_w) + oy; // width and height of scr1d

        if ((x < 0) || (x >= d_w) || (y < 0) || (y >= d_h)) {
            result = 0;
        } else {
            result = src1d[y * d_w + x] & 0x000000ff;
        }
        return result;
    }

    /**
     * getNeighbourd will get the pixel value of i's neighbour that's ox and oy
     * away from i, if the point is outside the image, then 0 is returned.
     * This version gets from destination image.
     */

    private int getNeighbourd( int[] src1d, int i, int ox, int oy ) {
        int x, y, result;

        x = (i % d_w) + ox; // d_w and d_h are assumed to be set to the
        y = (i / d_w) + oy; // width and height of scr1d

        if ((x < 0) || (x >= d_w) || (y < 0) || (y >= d_h)) {
            result = 0;
        } else {
            result = src1d[y * d_w + x];
        }
        return result;
    }

    /**
     * Associate(equivalence) a with b.
     *  a should be less than b to give some ordering (sorting)
     * if b is already associated with some other value, then propagate
     * down the list.
      */
    private void associate( int a, int b ) {

        if (a > b) {
            associate(b, a);
            return;
        }
        if ((a == b) || (labels[b] == a))
            return;
        if (labels[b] == b) {
            labels[b] = a;
        } else {
            associate(labels[b], a);
            if (labels[b] > a) { // ***rbf new
                labels[b] = a;
            }
        }
    }
    /**
     * Reduces the number of labels.
     */
    private int reduce( int a ) {

        if (labels[a] == a) {
            return a;
        } else {
            return reduce(labels[a]);
        }
    }

    // /**
    // *getColours
    // *@return the number of unique, non zero colours. -1 if not valid
    // */
    // private int getColours() {
    //
    // if (labelsValid) {
    //
    // return numberOfLabels;
    // } else {
    // return -1;
    // }
    // }
    //
    // /**
    // * Returns the number of labels.
    // */
    // private int getNumberOfLabels() {
    // return numberOfLabels;
    // }

}
