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

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.gears.libs.modules.Variables.CLOSE;
import static org.jgrasstools.gears.libs.modules.Variables.DILATE;
import static org.jgrasstools.gears.libs.modules.Variables.ERODE;
import static org.jgrasstools.gears.libs.modules.Variables.OPEN;
import static org.jgrasstools.gears.libs.modules.Variables.SKELETONIZE;

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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.modules.utils.BinaryFast;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;

@Description("Mophologic binary operations")
@Author(name = "Simon Horne, Andrea Antonello", contact = "http://homepages.inf.ed.ac.uk/rbf/HIPR2/, www.hydrologis.com")
@Keywords("Dilation, Erosion, Skeletonize, Open, Close, Raster")
@Label(JGTConstants.RASTERPROCESSING)
@Name("morpher")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class Morpher extends JGTModel {

    @Description("The map to morph.")
    @In
    public GridCoverage2D inMap = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The ranges of data to consider as valid values. Inside these ranges processing occurres.")
    @In
    public double[] pValidranges = null;

    @Description("The value to be considered as valid. This is used if pValidranges is not considered (defaults to 0).")
    @In
    public double pValid = 0.0;

    @Description("A kernel to use instead of the default.")
    @In
    public int[] pKernel = null;

    @Description("The operation type to perform (dilate, erode, skeletonize, open, close)")
    @UI("combo:" + DILATE + "," + ERODE + "," + SKELETONIZE + "," + OPEN + "," + CLOSE)
    @In
    public String pMode = DILATE;

    @Description("The number of iterations to perform (default is 1)")
    @In
    public int pIter = 1;

    @Description("The resulting map.")
    @Out
    public GridCoverage2D outMap = null;

    private double[][] ranges = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }

        if (pValidranges == null) {
            ranges = new double[][]{{pValid, pValid}};
        } else {
            ranges = new double[pValidranges.length / 2][2];
            int index = 0;
            for( int i = 0; i < ranges.length; i = i + 2 ) {
                ranges[index][0] = pValidranges[i];
                ranges[index][1] = pValidranges[i + 1];
                index++;
            }
        }

        final RenderedImage renderedImage = inMap.getRenderedImage();
        int width = renderedImage.getWidth();
        int height = renderedImage.getHeight();
        int[][] data = new int[width][height];
        RandomIter iter = RandomIterFactory.create(renderedImage, null);
        for( int c = 0; c < width; c++ ) {
            for( int r = 0; r < height; r++ ) {
                double value = iter.getSampleDouble(c, r, 0);
                data[c][r] = BinaryFast.BACKGROUND;
                if (isNovalue(value)) {
                    continue;
                } else {
                    for( double[] range : ranges ) {
                        if (value >= range[0] && value <= range[1]) {
                            data[c][r] = BinaryFast.FOREGROUND;
                        }
                    }
                }
            }
        }

        BinaryFast binaryData = new BinaryFast(data);

        if (pMode.equals(DILATE)) {
            dilate(binaryData);
        } else if (pMode.equals(ERODE)) {
            erode(binaryData);
        } else if (pMode.equals(SKELETONIZE)) {
            skeletonize(binaryData);
        } else if (pMode.equals(OPEN)) {
            open(binaryData);
        } else if (pMode.equals(CLOSE)) {
            close(binaryData);
        } else {
            throw new ModelsIllegalargumentException("Could not recognize mode.", this);
        }

        int[] values = binaryData.getValues();
        WritableRaster dataWR = CoverageUtilities.createWritableRasterFromArray(width, height, values);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inMap);
        outMap = CoverageUtilities.buildCoverage("morphed", dataWR, regionMap, inMap.getCoordinateReferenceSystem()); //$NON-NLS-1$
    }

    private void dilate( BinaryFast binaryData ) {
        new Dilate().process(binaryData, pKernel, pIter);
    }

    private void erode( BinaryFast binaryData ) {
        new Erode().process(binaryData, null, pIter);
    }

    private void skeletonize( BinaryFast binaryData ) {
        new Thin().process(binaryData, null);
    }

    private void open( BinaryFast binaryData ) {
        new Dilate().process(binaryData, null, pIter);
        new Erode().process(binaryData, null, pIter);
    }

    private void close( BinaryFast binaryData ) {
        new Erode().process(binaryData, null, pIter);
        new Dilate().process(binaryData, null, pIter);
    }

}
