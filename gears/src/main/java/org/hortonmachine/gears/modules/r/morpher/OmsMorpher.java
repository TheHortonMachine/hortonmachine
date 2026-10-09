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

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.Variables.*;

import java.awt.image.WritableRaster;

import org.eclipse.imagen.iterator.RandomIterFactory;
import org.eclipse.imagen.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
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
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.GridNode;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.modules.utils.BinaryFast;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;

@Description(OmsMorpher.DESCRIPTION)
@Documentation(OmsMorpher.DOCUMENTATION)
@Author(name = "Simon Horne, Andrea Antonello", contact = "http://homepages.inf.ed.ac.uk/rbf/HIPR2/, www.hydrologis.com")
@Keywords("Dilation, Erosion, Skeletonize, Open, Close, Raster")
@Label(HMConstants.RASTERPROCESSING)
@Name("omsmorpher")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class OmsMorpher extends HMModel {

    public static final String DESCRIPTION = "Morphological operations on rasters, where the valid cells are the objects and the novalues the background: dilation, erosion, opening and closing of the objects, their thinning to lines one cell wide (skeletonize) and the pruning of the short branches, and the search of the ends and of the junctions of the lines.";
    public static final String DOCUMENTATION = """
            The dilation gives to the novalue cells next to an object the largest value of their neighbours within the kernel, so the objects grow by one ring of cells. The erosion sets to novalue the cells of an object that have a novalue within the kernel, and gives the others the smallest value of their neighbours, so the objects shrink by one ring. The opening, an erosion followed by a dilation, removes the small objects and the thin parts; the closing, a dilation followed by an erosion, fills the small holes and gaps. The kernel is a square of 0 and 1, by default the 3x3 of all 1.

            The other operations work on the shapes only, all the valid cells being 1. The skeletonize ones thin the objects to lines of one cell, with four variants of the thinning kernels; the pruning removes from the lines the branches shorter than the number of iterations. The line endings and junctions give 1 on the cells where the lines end and where they meet, as the sources and the confluences of a network of channels.

            The operations are the ones of the Hypermedia Image Processing Reference (HIPR2), ported from the work of Simon Horne.
            """;

    @Description("The map to morph: the valid cells are the objects, the novalues the background.")
    @In
    public GridCoverage2D inMap = null;

    @Description("The kernel of the dilation and erosion, as the rows of a square of 0 and 1 one after the other: 1,1,1,1,1,1,1,1,1 is the default 3x3 one.")
    @In
    public int[] pKernel = null;

    @Description("If true, the dilation is faster but uses only the first valid value of each row of the kernel, so it gives the largest neighbour value only for maps of a single value, as masks.")
    @In
    public boolean doBinary = true;

    @Description("The morphological operation.")
    @UI("combo:" + DILATE + "," + ERODE + "," + SKELETONIZE1 + "," + SKELETONIZE2 + "," + SKELETONIZE2VAR + "," + SKELETONIZE3
            + "," + PRUNE + "," + OPEN + "," + CLOSE + "," + LINEENDINGS + "," + LINEJUNCTIONS)
    @In
    public String pMode = DILATE;

    @Description("The number of iterations of the pruning: the branches shorter than this number of cells are removed.")
    @In
    public int pIterations = 3;

    @Description("The morphed map: the values of the objects for dilate, erode, open and close, 1 for the other operations.")
    @Out
    public GridCoverage2D outMap = null;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outMap == null, doReset)) {
            return;
        }

        if (pKernel == null) {
            pKernel = MorpherHelp.DEFAULT3X3KERNEL;
        }

        double inNovalue = HMConstants.getNovalue(inMap);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inMap);
        WritableRaster inWR = CoverageUtilities.renderedImage2WritableRaster(inMap.getRenderedImage(), false);
        WritableRaster[] wrHolder = new WritableRaster[1];
        outMap = CoverageUtilities.createCoverageFromTemplate(inMap, doubleNovalue, wrHolder);
        WritableRaster outWR = wrHolder[0];
        if (pMode.equals(SKELETONIZE1)) {
            skeletonize(inWR, regionMap, outWR, MorpherHelp.SKELETON1_KERNEL);
        } else if (pMode.equals(SKELETONIZE2)) {
            skeletonize(inWR, regionMap, outWR, MorpherHelp.SKELETON2_KERNEL);
        } else if (pMode.equals(SKELETONIZE2VAR)) {
            skeletonize(inWR, regionMap, outWR, MorpherHelp.SKELETON2VARIANT_KERNEL);
        } else if (pMode.equals(SKELETONIZE3)) {
            skeletonize(inWR, regionMap, outWR, MorpherHelp.SKELETON3_KERNEL);
        } else if (pMode.equals(LINEENDINGS)) {
            lineendings(inWR, regionMap, outWR, MorpherHelp.LINEEND_KERNEL);
        } else if (pMode.equals(LINEJUNCTIONS)) {
            lineendings(inWR, regionMap, outWR, MorpherHelp.LINEJUNCTIONS_KERNEL);
        } else if (pMode.equals(PRUNE)) {
            if (pIterations == 0) {
                throw new ModelsIllegalargumentException("Number of iterations has to be > 0.", this, pm);
            }
            prune(inWR, regionMap, outWR, MorpherHelp.DEFAULT_PRUNE_KERNEL, pIterations);
        } else {
            if (pMode.equals(DILATE)) {
                dilate(inWR, inNovalue, regionMap, outWR, pKernel, doBinary, pm);
            } else if (pMode.equals(ERODE)) {
                erode(inWR, inNovalue, regionMap, outWR, pKernel, pm);
            } else if (pMode.equals(OPEN)) {
                open(inWR, inNovalue, regionMap, outWR, pKernel, doBinary, pm);
            } else if (pMode.equals(CLOSE)) {
                close(inWR, inNovalue, regionMap, outWR, pKernel, doBinary, pm);
            } else {
                throw new ModelsIllegalargumentException("Could not recognize mode.", this, pm);
            }
        }

    }

    /**
     * Morphologically dilates an input raster by a given kernel. 
     * 
     * @param inWR the input raster.
     * @param regionMap the {@link RegionMap}.
     * @param outWR the raster to modify.
     * @param kernelArray the kernel to use.
     * @param binary if <code>true</code>, binary mode is used.
     * @param pm 
     */
    public static void dilate( WritableRaster inWR, double inNovalue, RegionMap regionMap, WritableRaster outWR,
            int[] kernelArray, boolean binary, IHMProgressMonitor pm ) {
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xres = regionMap.getXres();
        double yres = regionMap.getYres();

        WritableRandomIter inIter = RandomIterFactory.createWritable(inWR, null);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        int[][] kernel = MorpherHelp.getSquareKernelMatrix(kernelArray);

        pm.beginTask("Perform dilation...", cols);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                GridNode node = new GridNode(inIter, cols, rows, xres, yres, c, r, inNovalue);
                if (!node.isValid()) {
                    double[][] nodeNeighbours = node.getWindow(kernel.length, false);
                    // apply the kernel
                    boolean set = false;
                    boolean doBreak = false;
                    double max = Double.NEGATIVE_INFINITY;
                    for( int kr = 0; kr < kernel.length; kr++ ) {
                        for( int kc = 0; kc < kernel[0].length; kc++ ) {
                            if (kernel[kr][kc] == 1) {
                                // valid kernel value
                                if (!isNovalue(nodeNeighbours[kr][kc]) && nodeNeighbours[kr][kc] > max) {
                                    max = nodeNeighbours[kr][kc];
                                    set = true;
                                    if (binary) {
                                        break;
                                    }
                                }
                            }
                        }
                        if (doBreak) {
                            break;
                        }
                    }
                    if (set) {
                        node.setDoubleValueInMap(outIter, max);
                    } else {
                        node.setDoubleValueInMap(outIter, HMConstants.doubleNovalue);
                    }
                    continue;
                } else {
                    node.setDoubleValueInMap(outIter, node.elevation);
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * Morphologically erodes an input raster by a given kernel. 
     * 
     * @param inWR the input raster.
     * @param regionMap the {@link RegionMap}.
     * @param outWR the raster to modify.
     * @param kernelArray the kernel to use.
     */
    public static void erode( WritableRaster inWR, double inNovalue, RegionMap regionMap, WritableRaster outWR, int[] kernelArray,
            IHMProgressMonitor pm ) {
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        double xres = regionMap.getXres();
        double yres = regionMap.getYres();

        WritableRandomIter inIter = RandomIterFactory.createWritable(inWR, null);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        int[][] kernel = MorpherHelp.getSquareKernelMatrix(kernelArray);

        pm.beginTask("Perform erosion...", cols);
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                GridNode node = new GridNode(inIter, cols, rows, xres, yres, c, r, inNovalue);
                if (node.isValid()) {
                    double[][] nodeNeighbours = node.getWindow(kernel.length, false);
                    // apply the kernel
                    boolean set = false;
                    double min = Double.POSITIVE_INFINITY;
                    for( int kr = 0; kr < kernel.length; kr++ ) {
                        for( int kc = 0; kc < kernel[0].length; kc++ ) {
                            if (kernel[kr][kc] == 1) {
                                // valid kernel value
                                if (isNovalue(nodeNeighbours[kr][kc])) {
                                    // novalue is the absolute min
                                    min = doubleNovalue;
                                    set = true;
                                    break;
                                } else if (nodeNeighbours[kr][kc] < min) {
                                    min = nodeNeighbours[kr][kc];
                                    set = true;
                                }
                            }
                        }
                        if (isNovalue(min)) {
                            break;
                        }
                    }
                    if (set) {
                        node.setDoubleValueInMap(outIter, min);
                    } else {
                        node.setDoubleValueInMap(outIter, node.elevation);
                    }
                    continue;
                }
            }
            pm.worked(1);
        }
        pm.done();
    }

    /**
     * Morphologically opens an input raster by a given kernel. 
     * 
     * @param inWR the input raster.
     * @param regionMap the {@link RegionMap}.
     * @param outWR the raster to modify.
     * @param kernelArray the kernel to use.
     * @param binary if <code>true</code>, binary mode is used.
     */
    public static void open( WritableRaster inWR, double inNovalue, RegionMap regionMap, WritableRaster outWR, int[] kernelArray,
            boolean binary, IHMProgressMonitor pm ) {
        erode(inWR, inNovalue, regionMap, outWR, kernelArray, pm);
        inWR.setDataElements(0, 0, outWR);
        clearRaster(regionMap, outWR);
        dilate(inWR, inNovalue, regionMap, outWR, kernelArray, binary, pm);
    }

    /**
     * Morphologically closes an input raster by a given kernel. 
     * 
     * @param inWR the input raster.
     * @param regionMap the {@link RegionMap}.
     * @param outWR the raster to modify.
     * @param kernelArray the kernel to use.
     * @param binary if <code>true</code>, binary mode is used.
     */
    public static void close( WritableRaster inWR, double inNovalue, RegionMap regionMap, WritableRaster outWR, int[] kernelArray,
            boolean binary, IHMProgressMonitor pm ) {
        dilate(inWR, inNovalue, regionMap, outWR, kernelArray, binary, pm);
        inWR.setDataElements(0, 0, outWR);
        clearRaster(regionMap, outWR);
        erode(inWR, inNovalue, regionMap, outWR, kernelArray, pm);
    }

    public static void skeletonize( WritableRaster inWR, RegionMap regionMap, WritableRaster outWR, int[][] kernels ) {
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        WritableRandomIter inIter = RandomIterFactory.createWritable(inWR, null);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);
        BinaryFast binaryData = toBinaryFast(cols, rows, inIter);

        new Thin().processSkeleton(binaryData, kernels);

        fromBinaryFast(cols, rows, outIter, binaryData);
    }

    public static void prune( WritableRaster inWR, RegionMap regionMap, WritableRaster outWR, int[][] kernels, int iterations ) {
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        WritableRandomIter inIter = RandomIterFactory.createWritable(inWR, null);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);
        BinaryFast binaryData = toBinaryFast(cols, rows, inIter);

        new Thin().processPruning(binaryData, iterations, kernels);

        fromBinaryFast(cols, rows, outIter, binaryData);
    }

    public static void lineendings( WritableRaster inWR, RegionMap regionMap, WritableRaster outWR, int[][] kernels ) {
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();
        WritableRandomIter inIter = RandomIterFactory.createWritable(inWR, null);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);
        BinaryFast binaryData = toBinaryFast(cols, rows, inIter);

        new Thin().processLineendings(binaryData, kernels);

        int[] values = binaryData.getValues();
        int index = 0;
        for( int y = 0; y < rows; y++ ) {
            for( int x = 0; x < cols; x++ ) {
                double value = (double) values[index];
                if (value == 0) {
                    value = doubleNovalue;
                }
                double origValue = inIter.getSampleDouble(x, y, 0);
                if (!isNovalue(origValue) && isNovalue(value)) {
                    outIter.setSample(x, y, 0, 1);
                }
                index++;
            }
        }
    }

    private static void clearRaster( RegionMap regionMap, WritableRaster outWR ) {
        // clear raster
        for( int r = 0; r < regionMap.getRows(); r++ ) {
            for( int c = 0; c < regionMap.getCols(); c++ ) {
                outWR.setSample(c, r, 0, doubleNovalue);
            }
        }
    }

    public static void fromBinaryFast( int cols, int rows, WritableRandomIter outIter, BinaryFast binaryData ) {
        int[] values = binaryData.getValues();
        int index = 0;
        for( int y = 0; y < rows; y++ ) {
            for( int x = 0; x < cols; x++ ) {
                double value = (double) values[index];
                if (value == 0) {
                    value = doubleNovalue;
                }
                outIter.setSample(x, y, 0, value);
                index++;
            }
        }
    }

    public static BinaryFast toBinaryFast( int cols, int rows, WritableRandomIter inIter ) {
        int[][] data = new int[cols][rows];
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
                double value = inIter.getSampleDouble(c, r, 0);
                data[c][r] = BinaryFast.BACKGROUND;
                if (isNovalue(value)) {
                    continue;
                } else {
                    data[c][r] = BinaryFast.FOREGROUND;
                }
            }
        }

        BinaryFast binaryData = new BinaryFast(data);
        return binaryData;
    }

}
