/*
 * This file is part of JGrasstools (http://www.jgrasstools.org)
 * (C) HydroloGIS - www.hydrologis.com 
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
package org.jgrasstools.hortonmachine.modules.geomorphology.gradient;

import static java.lang.Math.atan;
import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.toDegrees;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_AUTHORNAMES;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_DOCUMENTATION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_KEYWORDS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_LABEL;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_LICENSE;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_NAME;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_STATUS;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_doDegrees_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_inElev_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_outSlope_DESCRIPTION;
import static org.jgrasstools.hortonmachine.i18n.HortonMessages.OMSGRADIENT_pMode_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

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

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.hortonmachine.i18n.HortonMessageHandler;


@Description(OMSGRADIENT_DESCRIPTION)
@Documentation(OMSGRADIENT_DOCUMENTATION)
@Author(name = OMSGRADIENT_AUTHORNAMES, contact = OMSGRADIENT_AUTHORCONTACTS)
@Keywords(OMSGRADIENT_KEYWORDS)
@Label(OMSGRADIENT_LABEL)
@Name(OMSGRADIENT_NAME)
@Status(OMSGRADIENT_STATUS)
@License(OMSGRADIENT_LICENSE)
public class OmsGradient extends JGTModel {
    @Description(OMSGRADIENT_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSGRADIENT_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description(OMSGRADIENT_doDegrees_DESCRIPTION)
    @In
    public boolean doDegrees = false;

    @Description(OMSGRADIENT_outSlope_DESCRIPTION)
    @Out
    public GridCoverage2D outSlope = null;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private int nCols;

    private double xRes;

    private int nRows;

    private double yRes;

    @Execute
    public void process() {
        if (!concatOr(outSlope == null, doReset)) {
            return;
        }
        checkNull(inElev);
        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        nCols = regionMap.get(CoverageUtilities.COLS).intValue();
        nRows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage elevationRI = inElev.getRenderedImage();
        RandomIter elevationIter = RandomIterFactory.create(elevationRI, null);
        WritableRaster gradientWR = null;
        if (pMode == 1) {
            pm.message("Using Horn formula");
            gradientWR = gradientHorn(elevationIter);
        } else if (pMode == 2) {
            pm.message("Using Evans formula");
            gradientWR = gradientEvans(elevationIter);
        } else {
            pm.message("Using finite differences");
            gradientWR = gradientDiff(elevationIter);
        }
        outSlope = CoverageUtilities.buildCoverage("gradient", gradientWR, regionMap, inElev.getCoordinateReferenceSystem());
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
    */
    private WritableRaster gradientHorn( RandomIter elevationIter ) {

        WritableRaster gradientWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        pm.beginTask(msg.message("gradient.working"), nRows);
        for( int y = 1; y < nRows - 1; y++ ) {
            if (isCanceled(pm)) {
                return null;
            }
            for( int x = 1; x < nCols - 1; x++ ) {
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

                if (isNovalue(elev5) || isNovalue(elev1) || isNovalue(elev2) || isNovalue(elev3) || isNovalue(elev4)
                        || isNovalue(elev6) || isNovalue(elev7) || isNovalue(elev8) || isNovalue(elev9)) {
                    gradientWR.setSample(x, y, 0, doubleNovalue);
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
                    gradientWR.setSample(x, y, 0, grad);
                }
            }
            pm.worked(1);
        }
        pm.done();

        return gradientWR;
    }

    /**
     * Transform the gradient value into degrees.
     * 
     * @param value the radiant based gradient.
     * @return the degree gradient.
     */
    private double transform( double value ) {
        return toDegrees(atan(value));
    }

    /**
     * Estimate the gradient (p=f_{x}^{2}+f_{y}^{2}) with a finite difference formula:
     * 
     * <pre>
     *  p=&radic{f<sub>x</sub>&sup2;+f<sub>y</sub>&sup2;}
     * f<sub>x</sub>=(f(x+1,y)-f(x-1,y))/(2 &#916 x) 
     * f<sub>y</sub>=(f(x,y+1)-f(x,y-1))/(2 &#916 y)
     * </pre>
     * 
    */
    private WritableRaster gradientDiff( RandomIter elevationIter ) {

        WritableRaster gradientWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        pm.beginTask(msg.message("gradient.working"), nRows);
        for( int y = 1; y < nRows - 1; y++ ) {

            for( int x = 1; x < nCols - 1; x++ ) {
                // extract the value to use for the algoritm. It is the finite difference approach.
                double elevIJ = elevationIter.getSampleDouble(x, y, 0);
                double elevIJipre = elevationIter.getSampleDouble(x - 1, y, 0);
                double elevIJipost = elevationIter.getSampleDouble(x + 1, y, 0);
                double elevIJjpre = elevationIter.getSampleDouble(x, y - 1, 0);
                double elevIJjpost = elevationIter.getSampleDouble(x, y + 1, 0);
                if (isNovalue(elevIJ) || isNovalue(elevIJipre) || isNovalue(elevIJipost) || isNovalue(elevIJjpre)
                        || isNovalue(elevIJjpost)) {
                    gradientWR.setSample(x, y, 0, doubleNovalue);
                } else if (!isNovalue(elevIJ) && !isNovalue(elevIJipre) && !isNovalue(elevIJipost) && !isNovalue(elevIJjpre)
                        && !isNovalue(elevIJjpost)) {
                    double xGrad = 0.5 * (elevIJipost - elevIJipre) / xRes;
                    double yGrad = 0.5 * (elevIJjpre - elevIJjpost) / yRes;
                    double grad = sqrt(pow(xGrad, 2) + pow(yGrad, 2));
                    if (doDegrees) {
                        grad = transform(grad);
                    }
                    gradientWR.setSample(x, y, 0, grad);
                } else {
                    throw new ModelsIllegalargumentException("Error in gradient", this);
                }
            }
            pm.worked(1);
        }
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
     *
     */
    private WritableRaster gradientEvans( RandomIter elevationIter ) {

        WritableRaster gradientWR = CoverageUtilities.createDoubleWritableRaster(nCols, nRows, null, null, doubleNovalue);

        pm.beginTask(msg.message("gradient.working"), nRows);
        for( int y = 1; y < nRows - 1; y++ ) {

            for( int x = 1; x < nCols - 1; x++ ) {

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

                if (isNovalue(elev5) || isNovalue(elev1) || isNovalue(elev2) || isNovalue(elev3) || isNovalue(elev4)
                        || isNovalue(elev6) || isNovalue(elev7) || isNovalue(elev8) || isNovalue(elev9)) {
                    gradientWR.setSample(x, y, 0, doubleNovalue);
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
                    gradientWR.setSample(x, y, 0, grad);
                }
            }

            pm.worked(1);
        }
        pm.done();

        return gradientWR;
    }

}
