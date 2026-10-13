package org.hortonmachine.lesto.modules.vegetation;

import java.util.List;

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.hortonmachine.lesto.modules.vegetation.watershed.WatershedFIFO;
import org.hortonmachine.lesto.modules.vegetation.watershed.WatershedPixel;
import org.hortonmachine.lesto.modules.vegetation.watershed.WatershedStructure;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;

/**
 *  This algorithm is an implementation of the watershed immersion algorithm
 *  written by Vincent and Soille (1991).
 *
 *  @Article{Vincent/Soille:1991,
 *   author =       "Lee Vincent and Pierre Soille",
 *   year =         "1991",
 *   keywords =     "IMAGE-PROC SKELETON SEGMENTATION GIS",
 *   institution =  "Harvard/Paris+Louvain",
 *   title =        "Watersheds in digital spaces: An efficient algorithm
 *                   based on immersion simulations",
 *   journal =      "IEEE PAMI, 1991",
 *   volume =       "13",
 *   number =       "6",
 *   pages =        "583--598",
 *   annote =       "Watershed lines (e.g. the continental divide) mark the
 *                  boundaries of catchment regions in a topographical map.
 *                  The height of a point on this map can have a direct
 *                  correlation to its pixel intensity. WIth this analogy,
 *                  the morphological operations of closing (or opening)
 *                  can be understood as smoothing the ridges (or filling
 *                  in the valleys). Develops a new algorithm for obtaining
 *                  the watershed lines in a graph, and then uses this in
 *                  developing a new segmentation approach based on the
 *                  {"}depth of immersion{"}.",
 *  }
 *
 *  A review of Watershed algorithms can be found at :
 *  http://www.cs.rug.nl/~roe/publications/parwshed.pdf
 *
 *  @Article{RoeMei00,
 *   author =       "Roerdink and Meijster",
 *   title =        "The Watershed Transform: Definitions, Algorithms and
 *                   Parallelization Strategies",
 *   journal =      "FUNDINF: Fundamenta Informatica",
 *   volume =       "41",
 *   publisher =    "IOS Press",
 *   year =         "2000",
 *  }
 *  
 *  Taken from: http://rsbweb.nih.gov/ij/plugins/watershed.html
 *  
 **/
@Description("Divides a raster into the basins of its minima, or of its maxima if inverted, as the crowns of the trees of a canopy height model, with the immersion algorithm of Vincent and Soille (1991).")
@Documentation(WatershedAlgorithm.DOCUMENTATION)
@Author(name = "Christopher Mei, Lee Vincent and Pierre Soille 1991, Andrea Antonello (hm port)", contact = "christopher.mei@sophia.inria.fr")
@Keywords("watershed, segmentation, crowns, lidar")
@Label(HMConstants.LESTO + "/vegetation")
@Name("contourscrowner")
@Status(Status.EXPERIMENTAL)
@License("GPL2")
@Bibliography(WatershedAlgorithm.BIBLIOGRAPHY_VINCENT_SOILLE_1991)
public class WatershedAlgorithm extends HMModel {

    @Description("The raster to divide, as a canopy height model.")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description("If true, the raster is flooded upside down, from its maxima: the basins are then the crowns around the tops of a canopy height model.")
    @In
    public boolean doInvert = false;

    @Description("If true, the output has 1 on the watershed lines between the basins, else the number of the basin of each cell.")
    @In
    public boolean doBorders = true;

    @Description("The watershed lines or the basins.")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    public static final String BIBLIOGRAPHY_VINCENT_SOILLE_1991 = "Vincent, L., Soille, P. (1991). Watersheds in digital spaces: an efficient algorithm based on immersion simulations. IEEE Transactions on Pattern Analysis and Machine Intelligence, 13(6), 583-598.";
    public static final String DOCUMENTATION = """
            The raster is seen as a surface that is flooded from its minima: the water rises level by level, each minimum fills its own basin, and where the waters of two basins meet the cells become watershed lines. The algorithm is the immersion of Vincent and Soille (1991), from the ImageJ plugin of Christopher Mei.

            The algorithm works on 256 levels: the valid values are stretched linearly from their minimum to their maximum on the levels 0 to 254, so that on a canopy height model of 40 m a level is about 16 cm. Cells closer than a level are flat for the algorithm. The cells with no value are left out, they are no basin and no line.

            Inverted, the levels are turned upside down and the flood starts from the maxima: on a canopy height model each basin is then the crown around a top. Every small bump of the surface is a top of its own and makes a basin, so that a raw canopy model, noisy from cell to cell, is split in many small pieces: smoothed, as with a gaussian KernelDensity of a couple of cells, it gives fewer crowns, closer to the trees. The ground and the low vegetation make basins too: setting them to no value first, as with Mapcalc, leaves them out.

            Where the waters of different basins meet on a flat area, the whole area can become watershed line, not only a line one cell wide.

            With the borders, the cells of the watershed lines next to a basin have the value 1. Without, each cell has the number of its basin, from 1, and the watershed lines have no value.
            """;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, outRaster);

        int nCols;
        int nRows;
        byte[] input;
        boolean[] valid;
        HMRaster outHMRaster;
        try (HMRaster inHMRaster = HMRaster.fromFile(inRaster)) {
            nCols = inHMRaster.getCols();
            nRows = inHMRaster.getRows();

            /** The valid values are stretched on the grey levels 0-254, the novalues are left out **/
            double min = Double.POSITIVE_INFINITY;
            double max = Double.NEGATIVE_INFINITY;
            for( int r = 0; r < nRows; r++ ) {
                for( int c = 0; c < nCols; c++ ) {
                    double value = inHMRaster.getValue(c, r);
                    if (!inHMRaster.isNovalue(value)) {
                        min = Math.min(min, value);
                        max = Math.max(max, value);
                    }
                }
            }
            if (min > max) {
                throw new ModelsIllegalargumentException("The raster has no valid values.", this);
            }
            double range = max - min;
            input = new byte[nCols * nRows];
            valid = new boolean[nCols * nRows];
            int index = 0;
            for( int r = 0; r < nRows; r++ ) {
                for( int c = 0; c < nCols; c++ ) {
                    double value = inHMRaster.getValue(c, r);
                    if (!inHMRaster.isNovalue(value)) {
                        int level = range > 0 ? (int) Math.round((value - min) / range * 254.0) : 0;
                        if (doInvert) {
                            level = 254 - level;
                        }
                        input[index] = (byte) level;
                        valid[index] = true;
                    }
                    index++;
                }
            }
            outHMRaster = new HMRaster.HMRasterWritableBuilder().setName("watershed").setTemplate(inHMRaster)
                    .setNoValue(HMConstants.doubleNovalue).build();
        }

        /** First step : the pixels are sorted according to increasing grey values **/
        WatershedStructure watershedStructure = new WatershedStructure(input, valid, nCols, nRows, pm);

        /** Start flooding **/
        WatershedFIFO queue = new WatershedFIFO();
        int curlab = 0;

        int heightIndex1 = 0;
        int heightIndex2 = 0;

        for( int h = 0; h < 256; h++ ) /*Geodesic SKIZ of level h-1 inside level h */{

            for( int pixelIndex = heightIndex1; pixelIndex < watershedStructure.size(); pixelIndex++ ) /*mask all pixels at level h*/{
                WatershedPixel p = watershedStructure.get(pixelIndex);

                if (p.getIntHeight() != h) {
                    /** This pixel is at level h+1 **/
                    heightIndex1 = pixelIndex;
                    break;
                }

                p.setLabelToMASK();

                List<WatershedPixel> neighbours = p.getNeighbours();
                for( int i = 0; i < neighbours.size(); i++ ) {
                    WatershedPixel q = (WatershedPixel) neighbours.get(i);

                    if (q.getLabel() >= 0) {/*Initialise queue with neighbours at level h of current basins or watersheds*/
                        p.setDistance(1);
                        queue.fifo_add(p);
                        break;
                    } // end if
                } // end for
            } // end for

            int curdist = 1;
            queue.fifo_add_FICTITIOUS();

            while( true ) /** extend basins **/
            {
                WatershedPixel p = queue.fifo_remove();

                if (p.isFICTITIOUS())
                    if (queue.fifo_empty())
                        break;
                    else {
                        queue.fifo_add_FICTITIOUS();
                        curdist++;
                        p = queue.fifo_remove();
                    }

                List<WatershedPixel> neighbours = p.getNeighbours();
                for( int i = 0; i < neighbours.size(); i++ ) /* Labelling p by inspecting neighbours */{
                    WatershedPixel q = (WatershedPixel) neighbours.get(i);

                    /* Original algorithm : 
                       if( (q.getDistance() < curdist) &&
                       (q.getLabel()>0 || q.isLabelWSHED()) ) {*/
                    if ((q.getDistance() <= curdist) && (q.getLabel() >= 0)) {
                        /* q belongs to an existing basin or to a watershed */

                        if (q.getLabel() > 0) {
                            if (p.isLabelMASK())
                                // Removed from original algorithm || p.isLabelWSHED() )
                                p.setLabel(q.getLabel());
                            else if (p.getLabel() != q.getLabel())
                                p.setLabelToWSHED();
                        } // end if lab>0
                        else if (p.isLabelMASK())
                            p.setLabelToWSHED();
                    } else if (q.isLabelMASK() && (q.getDistance() == 0)) {

                        q.setDistance(curdist + 1);
                        queue.fifo_add(q);
                    }
                } // end for, end processing neighbours

            } // end while (loop)

            /* Detect and process new minima at level h */
            for( int pixelIndex = heightIndex2; pixelIndex < watershedStructure.size(); pixelIndex++ ) {
                WatershedPixel p = watershedStructure.get(pixelIndex);

                if (p.getIntHeight() != h) {
                    /** This pixel is at level h+1 **/
                    heightIndex2 = pixelIndex;
                    break;
                }

                p.setDistance(0); /* Reset distance to zero */

                if (p.isLabelMASK()) { /* the pixel is inside a new minimum */
                    curlab++;
                    p.setLabel(curlab);
                    queue.fifo_add(p);

                    while( !queue.fifo_empty() ) {
                        WatershedPixel q = queue.fifo_remove();

                        List<WatershedPixel> neighbours = q.getNeighbours();

                        for( int i = 0; i < neighbours.size(); i++ ) /* inspect neighbours of p2*/{
                            WatershedPixel r = (WatershedPixel) neighbours.get(i);

                            if (r.isLabelMASK()) {
                                r.setLabel(curlab);
                                queue.fifo_add(r);
                            }
                        }
                    } // end while
                } // end if
            } // end for
        }
        /** End of flooding **/

        try (outHMRaster) {
            pm.beginTask("Setting watersheds...", watershedStructure.size());
            for( int pixelIndex = 0; pixelIndex < watershedStructure.size(); pixelIndex++ ) {
                WatershedPixel p = watershedStructure.get(pixelIndex);
                int c = p.getX();
                int r = p.getY();
                if (doBorders) {
                    if (p.isLabelWSHED() && !p.allNeighboursAreWSHED())
                        outHMRaster.setValue(c, r, 1.0);
                } else {
                    if (p.getLabel() > 0)
                        outHMRaster.setValue(c, r, (double) p.getLabel());
                }
                pm.worked(1);
            }
            pm.done();
            outHMRaster.writeToFile(outRaster);
        }

    }

    // public static void main( String[] args ) throws Exception {
    // GridCoverage2D inRaster =
    // getRaster("/home/moovida/data-mega/unibz_aurino/testarea_dtm_dsm/dsm_reverse_blurred_5_0.01.tif");
    //
    // OmsWatershedAlgorithm w = new OmsWatershedAlgorithm();
    // w.inRaster = inRaster;
    // w.process();
    // GridCoverage2D outRaster2 = w.outRaster;
    // dumpRaster(outRaster2,
    // "/home/moovida/data-mega/unibz_aurino/testarea_dtm_dsm/dsm_reverse_watershed_gauss.tif");
    // }

}
