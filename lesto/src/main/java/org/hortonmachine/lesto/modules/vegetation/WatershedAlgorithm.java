package org.hortonmachine.lesto.modules.vegetation;

import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;

import java.awt.image.WritableRaster;
import java.util.List;

import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
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

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.lesto.modules.vegetation.watershed.WatershedFIFO;
import org.hortonmachine.lesto.modules.vegetation.watershed.WatershedPixel;
import org.hortonmachine.lesto.modules.vegetation.watershed.WatershedStructure;

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
@Description("Calculates the watershed of a 8-bit images. It uses the immersion algorithm written by Vincent and Soille (1991)")
@Documentation("http://rsbweb.nih.gov/ij/plugins/watershed.html")
@Author(name = "Christopher Mei, Lee Vincent and Pierre Soille 1991, Andrea Antonello (hm port)", contact = "christopher.mei@sophia.inria.fr")
@Keywords("IMAGE-PROC SKELETON SEGMENTATION GIS")
@Label(HMConstants.LESTO + "/vegetation")
@Name("contourscrowner")
@Status(Status.EXPERIMENTAL)
@License("GPL2")
public class WatershedAlgorithm extends HMModel {

    @Description("An elevation raster")
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inRaster;

    @Description("If true, the borders of the watershed are set, else the content.")
    @In
    public boolean doBorders = true;

    @Description("Watershed raster")
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outRaster;

    @Execute
    public void process() throws Exception {
        checkNull(inRaster, outRaster);

        GridCoverage2D inRasterGC = getRaster(inRaster);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRasterGC);
        int nCols = regionMap.getCols();
        int nRows = regionMap.getRows();

        byte[] input = CoverageUtilities.renderedImage2ByteArray(inRasterGC.getRenderedImage(), true);

        /** First step : the pixels are sorted according to increasing grey values **/
        WatershedStructure watershedStructure = new WatershedStructure(input, nCols, nRows, pm);

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

        WritableRaster outWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        pm.beginTask("Setting watersheds...", watershedStructure.size());

        for( int pixelIndex = 0; pixelIndex < watershedStructure.size(); pixelIndex++ ) {
            WatershedPixel p = watershedStructure.get(pixelIndex);
            int c = p.getX();
            int r = p.getY();
            if (p.isLabelWSHED() && !p.allNeighboursAreWSHED()) {
                if (doBorders)
                    outIter.setSample(c, r, 0, 1.0);
            } else {
                if (!doBorders)
                    outIter.setSample(c, r, 0, 1.0);
            }
            pm.worked(1);
        }
        pm.done();
        outIter.done();

        GridCoverage2D outRasterGC = CoverageUtilities.buildCoverage("normalized", outWR, regionMap,
                inRasterGC.getCoordinateReferenceSystem());
        dumpRaster(outRasterGC, outRaster);

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
