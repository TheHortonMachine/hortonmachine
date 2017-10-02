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
package org.hortonmachine.hmachine.modules.geomorphology.gradient;

import static java.lang.Math.atan;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static org.hortonmachine.gears.libs.modules.HMConstants.GEOMORPHOLOGY;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.Variables.EVANS;
import static org.hortonmachine.gears.libs.modules.Variables.FINITE_DIFFERENCES;
import static org.hortonmachine.gears.libs.modules.Variables.HORN;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.multiprocessing.GridMultiProcessing;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

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

@Description(OmsGradient.OMSGRADIENT_DESCRIPTION)
@Documentation(OmsGradient.OMSGRADIENT_DOCUMENTATION)
@Author(name = OmsGradient.OMSGRADIENT_AUTHORNAMES, contact = OmsGradient.OMSGRADIENT_AUTHORCONTACTS)
@Keywords(OmsGradient.OMSGRADIENT_KEYWORDS)
@Label(OmsGradient.OMSGRADIENT_LABEL)
@Name(OmsGradient.OMSGRADIENT_NAME)
@Status(OmsGradient.OMSGRADIENT_STATUS)
@License(OmsGradient.OMSGRADIENT_LICENSE)
public class OmsGradient extends GridMultiProcessing {
    @Description(OMSGRADIENT_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSGRADIENT_pMode_DESCRIPTION)
    @UI("combo:" + FINITE_DIFFERENCES + "," + HORN + "," + EVANS)
    @In
    public String pMode = FINITE_DIFFERENCES;

    @Description(OMSGRADIENT_doDegrees_DESCRIPTION)
    @In
    public boolean doDegrees = false;

    @Description(OMSGRADIENT_outSlope_DESCRIPTION)
    @Out
    public GridCoverage2D outSlope = null;

    public static final String OMSGRADIENT_DESCRIPTION = "Calculates the gradient in each point of the map.";
    public static final String OMSGRADIENT_DOCUMENTATION = "OmsGradient.html";
    public static final String OMSGRADIENT_KEYWORDS = "Geomorphology, OmsDrainDir, OmsFlowDirections, OmsSlope, OmsCurvatures";
    public static final String OMSGRADIENT_LABEL = GEOMORPHOLOGY;
    public static final String OMSGRADIENT_NAME = "gradient";
    public static final int OMSGRADIENT_STATUS = 40;
    public static final String OMSGRADIENT_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSGRADIENT_AUTHORNAMES = "Daniele Andreis, Antonello Andrea, Erica Ghesla, Cozzini Andrea, Franceschi Silvia, Pisoni Silvano, Rigon Riccardo";
    public static final String OMSGRADIENT_AUTHORCONTACTS = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSGRADIENT_inElev_DESCRIPTION = "The map of the digital elevation model (DEM or pit).";
    public static final String OMSGRADIENT_pMode_DESCRIPTION = "The gradient formula mode.";
    public static final String OMSGRADIENT_doDegrees_DESCRIPTION = "The output type, if false = tan of the angle (default), if true = degrees";
    public static final String OMSGRADIENT_outSlope_DESCRIPTION = "The map of gradient.";

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int nCols;

    private double xRes;

    private int nRows;

    private double yRes;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outSlope == null, doReset)) {
            return;
        }

        checkNull(inElev);
        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        nCols = regionMap.getCols();
        nRows = regionMap.getRows();
        xRes = regionMap.getXres();
        yRes = regionMap.getYres();

        RenderedImage elevationRI = inElev.getRenderedImage();
        RandomIter elevationIter = RandomIterFactory.create(elevationRI, null);

        WritableRaster gradientWR;
        try {
            gradientWR = null;
            if (pMode.equals(HORN)) {
                gradientWR = gradientHorn(elevationIter);
            } else if (pMode.equals(EVANS)) {
                gradientWR = gradientEvans(elevationIter);
            } else {
                gradientWR = gradientDiff(elevationIter);
            }
            outSlope = CoverageUtilities.buildCoverage("gradient", gradientWR, regionMap, inElev.getCoordinateReferenceSystem());
        } finally {
            elevationIter.done();
        }
    }

    /**
    * Computes the gradient algorithm. p=f_{x}^{2}+f_{y}^{2}
    *  
    * The derivatives can be calculate with the  the horn formula:
    * <p>
    * f<sub>x</sub>=(2*f<sub>(x+1,y)</sub>+f<sub>(x+1,y-1)</sub>+
    *   f<sub>(x+1,y+1)</sub>-2*f<sub>(x-1,y)</sub>-f<sub>(x-1,y+1)</sub>-
    *   f<sub>(x-1,y-1)</sub>)/(8 &#916 x)
    * <br> 
    * f<sub>y</sub>=(2*f<sub>(x,y+1)</sub>+f<sub>(x+1,y+1)</sub>+
    * f<sub>(x-1,y+1)</sub>-2*f<sub>(x,y-1)</sub>-f<sub>(x+1,y-1)</sub>+
    * f<sub>(x-1,y-1)</sub>)/(8 &#916 y)
    * <p>
    * The kernel is compound of 9 cell (8 around the central pixel) and the numeration is:
    * <pre>
    * 1   2   3
    * 4   5   6
    * 7   8   9
    * </pre>
    * 
    * <p>
    * This numeration is used to extract the appropriate elevation value (es elev1 an so on)
     * @throws Exception 
    */
    private WritableRaster gradientHorn( RandomIter elevationIter ) throws Exception {
        WritableRaster gradientWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        pm.beginTask(msg.message("gradient.working") + " (" + HORN + ")", nRows * nCols);
        processGrid(nCols, nRows, true, ( c, r ) -> {
            if (pm.isCanceled()) {
                return;
            }
            // extract the value to use for the algoritm. It is the finite difference approach.
            double value = doGradientHornOnCell(elevationIter, c, r, xRes, yRes, doDegrees);
            gradientWR.setSample(c, r, 0, value);
            pm.worked(1);
        });
        pm.done();
        return gradientWR;
    }

    /**
     * Estimate the gradient (p=f_{x}^{2}+f_{y}^{2}) with a finite difference formula:
     * 
     * <pre>
     *  p=&radic{f<sub>x</sub>&sup2;+f<sub>y</sub>&sup2;}
     * f<sub>x</sub>=(f(x+1,y)-f(x-1,y))/(2 &#916 x) 
     * f<sub>y</sub>=(f(x,y+1)-f(x,y-1))/(2 &#916 y)
     * </pre>
     * @throws Exception 
     * 
    */
    private WritableRaster gradientDiff( RandomIter elevationIter ) throws Exception {
        WritableRaster gradientWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        pm.beginTask(msg.message("gradient.working") + " (" + FINITE_DIFFERENCES + ")", nRows * nCols);
        processGrid(nCols, nRows, true, ( c, r ) -> {
            if (pm.isCanceled()) {
                return;
            }
            double value = doGradientDiffOnCell(elevationIter, c, r, xRes, yRes, doDegrees);
            gradientWR.setSample(c, r, 0, value);
            pm.worked(1);
        });
        pm.done();
        return gradientWR;
    }

    /** estimate the gradient using the Horn formula.
     * <p>
     * Where the gradient is:
     * </p>
     * <pre>
     *  p=&radic{f<sub>x</sub>&sup2;+f<sub>y</sub>&sup2;}
     *  
     *  and the derivatives can be calculate with the  the Evans formula:
     * f<sub>x</sub>=(f(x+1,y)+f(x+1,y-1)+f(x+1,y+1)-f(x-1,y)-f(x-1,y+1)-f(x-1,y-1))/(6 &#916 x) 
     * f<sub>y</sub>=(f(x,y+1)+f(x+1,y+1)+f(x-1,y+1)-f(x,y-1)-f(x+1,y-1)+f(x-1,y-1))/(6 &#916 y)
     * <p>
     * The kernel is compound of 9 cell (8 around the central pixel) and the numeration is:
     * 
     * 1   2   3
     * 4   5   6
     * 7   8   9
     * 
     * This enumeration is used to extract the appropriate elevation value (es elev1 an so on)
     * 
     * </p>
     * @throws Exception 
     */
    private WritableRaster gradientEvans( RandomIter elevationIter ) throws Exception {
        WritableRaster gradientWR = CoverageUtilities.createWritableRaster(nCols, nRows, null, null, doubleNovalue);
        pm.beginTask(msg.message("gradient.working") + " (" + EVANS + ")", nRows * nCols);
        processGrid(nCols, nRows, true, ( c, r ) -> {
            if (pm.isCanceled()) {
                return;
            }
            double value = doGradientEvansOnCell(elevationIter, c, r, xRes, yRes, doDegrees);
            gradientWR.setSample(c, r, 0, value);
            pm.worked(1);
        });
        pm.done();
        return gradientWR;
    }

    public static double doGradientEvansOnCell( RandomIter elevationIter, int x, int y, double xRes, double yRes,
            boolean doDegrees ) {
        // extract the value to use for the algoritm. It is the finite difference approach.
        double elev5 = elevationIter.getSampleDouble(x, y, 0);
        double elev4 = elevationIter.getSampleDouble(x - 1, y, 0);
        double elev6 = elevationIter.getSampleDouble(x + 1, y, 0);
        double elev2 = elevationIter.getSampleDouble(x, y - 1, 0);
        double elev8 = elevationIter.getSampleDouble(x, y + 1, 0);
        double elev9 = elevationIter.getSampleDouble(x + 1, y + 1, 0);
        double elev1 = elevationIter.getSampleDouble(x - 1, y - 1, 0);
        double elev3 = elevationIter.getSampleDouble(x + 1, y - 1, 0);
        double elev7 = elevationIter.getSampleDouble(x - 1, y + 1, 0);

        if (isNovalue(elev5) || isNovalue(elev1) || isNovalue(elev2) || isNovalue(elev3) || isNovalue(elev4) || isNovalue(elev6)
                || isNovalue(elev7) || isNovalue(elev8) || isNovalue(elev9)) {
            return doubleNovalue;
        } else {
            double fu = elev6 + elev9 + elev3;
            double fd = elev4 + elev7 + elev1;
            double xGrad = (fu - fd) / (6 * xRes);
            fu = elev8 + elev7 + elev9;
            fd = elev2 + elev1 + elev3;

            double yGrad = (fu - fd) / (6 * yRes);
            double grad = sqrt(pow(xGrad, 2) + pow(yGrad, 2));
            if (doDegrees) {
                grad = transform(grad);
            }
            return grad;
        }
    }

    public static double doGradientHornOnCell( RandomIter elevationIter, int x, int y, double xRes, double yRes,
            boolean doDegrees ) {
        double elev5 = elevationIter.getSampleDouble(x, y, 0);
        double elev4 = elevationIter.getSampleDouble(x - 1, y, 0);
        double elev6 = elevationIter.getSampleDouble(x + 1, y, 0);
        double elev2 = elevationIter.getSampleDouble(x, y - 1, 0);
        double elev8 = elevationIter.getSampleDouble(x, y + 1, 0);
        double elev9 = elevationIter.getSampleDouble(x + 1, y + 1, 0);
        double elev1 = elevationIter.getSampleDouble(x - 1, y - 1, 0);
        double elev3 = elevationIter.getSampleDouble(x + 1, y - 1, 0);
        double elev7 = elevationIter.getSampleDouble(x - 1, y + 1, 0);

        if (isNovalue(elev5) || isNovalue(elev1) || isNovalue(elev2) || isNovalue(elev3) || isNovalue(elev4) || isNovalue(elev6)
                || isNovalue(elev7) || isNovalue(elev8) || isNovalue(elev9)) {
            return doubleNovalue;
        } else {
            double fu = 2 * elev6 + elev9 + elev3;
            double fd = 2 * elev4 + elev7 + elev1;
            double xGrad = (fu - fd) / (8 * xRes);
            fu = 2 * elev8 + elev7 + elev9;
            fd = 2 * elev2 + elev1 + elev3;
            double yGrad = (fu - fd) / (8 * yRes);
            double grad = sqrt(pow(xGrad, 2) + pow(yGrad, 2));
            if (doDegrees) {
                grad = transform(grad);
            }
            return grad;
        }
    }

    /**
     * Transform the gradient value into degrees.
     * 
     * @param value the radiant based gradient.
     * @return the degree gradient.
     */
    private static double transform( double value ) {
        return toDegrees(atan(value));
    }

    public static double doGradientDiffOnCell( RandomIter elevationIter, int x, int y, double xRes, double yRes,
            boolean doDegrees ) {
        // extract the value to use for the algoritm. It is the finite difference approach.
        double elevIJ = elevationIter.getSampleDouble(x, y, 0);
        double elevIJipre = elevationIter.getSampleDouble(x - 1, y, 0);
        double elevIJipost = elevationIter.getSampleDouble(x + 1, y, 0);
        double elevIJjpre = elevationIter.getSampleDouble(x, y - 1, 0);
        double elevIJjpost = elevationIter.getSampleDouble(x, y + 1, 0);
        if (isNovalue(elevIJ) || isNovalue(elevIJipre) || isNovalue(elevIJipost) || isNovalue(elevIJjpre)
                || isNovalue(elevIJjpost)) {
            return doubleNovalue;
        } else if (!isNovalue(elevIJ) && !isNovalue(elevIJipre) && !isNovalue(elevIJipost) && !isNovalue(elevIJjpre)
                && !isNovalue(elevIJjpost)) {
            double xGrad = 0.5 * (elevIJipost - elevIJipre) / xRes;
            double yGrad = 0.5 * (elevIJjpre - elevIJjpost) / yRes;
            double grad = sqrt(pow(xGrad, 2) + pow(yGrad, 2));
            if (doDegrees) {
                grad = transform(grad);
            }
            return grad;
        } else {
            throw new ModelsIllegalargumentException("Error in gradient", "GRADIENT");
        }
    }
}
