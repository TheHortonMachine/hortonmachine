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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.skyview;

import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.ModelsEngine.calcInverseSunVector;
import static org.hortonmachine.gears.libs.modules.ModelsEngine.calcNormalSunVector;
import static org.hortonmachine.gears.libs.modules.ModelsEngine.scalarProduct;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSKYVIEW_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSKYVIEW_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSKYVIEW_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSKYVIEW_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSKYVIEW_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSKYVIEW_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSKYVIEW_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSKYVIEW_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSKYVIEW_inElev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSSKYVIEW_outSky_DESCRIPTION;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.RasterFactory;

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
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.hmachine.i18n.HortonMessageHandler;

@Description(OMSSKYVIEW_DESCRIPTION)
@Author(name = OMSSKYVIEW_AUTHORNAMES, contact = OMSSKYVIEW_AUTHORCONTACTS)
@Keywords(OMSSKYVIEW_KEYWORDS)
@Label(OMSSKYVIEW_LABEL)
@Name(OMSSKYVIEW_NAME)
@Status(OMSSKYVIEW_STATUS)
@License(OMSSKYVIEW_LICENSE)
public class OmsSkyview extends HMModel {

    @Description(OMSSKYVIEW_inElev_DESCRIPTION)
    @In
    public GridCoverage2D inElev = null;

    @Description(OMSSKYVIEW_outSky_DESCRIPTION)
    @Out
    public GridCoverage2D outSky;

    private HortonMessageHandler msg = HortonMessageHandler.getInstance();

    private double maxSlope;
    private double azimuth;
    private double elevation;
    private int minX = 0;
    private int minY = 0;
    private int rows = 0;
    private int cols = 0;
    private WritableRaster normalVectorWR;

    @Execute
    public void process() throws Exception {
        checkNull(inElev);
        // extract some attributes of the map
        HashMap<String, Double> attribute = CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        double dx = attribute.get(CoverageUtilities.XRES);
        CoverageUtilities.getRegionParamsFromGridCoverage(inElev);
        // extract the raster.
        RenderedImage pitTmpRI = inElev.getRenderedImage();
        WritableRaster pitWR = CoverageUtilities.replaceNovalue(pitTmpRI, -9999.0);
        pitTmpRI = null;
        minX = pitWR.getMinX();
        minY = pitWR.getMinY();
        rows = pitWR.getHeight();
        cols = pitWR.getWidth();

        WritableRaster skyWR = skyviewfactor(pitWR, dx);

        int maxY = minY + rows;
        int maxX = minX + cols;
        for( int y = minY + 2; y < maxY - 2; y++ ) {
            for( int x = minX + 2; x < maxX - 2; x++ ) {
                if (pitWR.getSampleDouble(x, y, 0) == -9999.0) {
                    skyWR.setSample(x, y, 0, doubleNovalue);
                }
            }
        }
        for( int y = minY; y < maxY; y++ ) {
            skyWR.setSample(0, y, 0, doubleNovalue);
            skyWR.setSample(1, y, 0, doubleNovalue);
            skyWR.setSample(cols - 2, y, 0, doubleNovalue);
            skyWR.setSample(cols - 1, y, 0, doubleNovalue);
        }

        for( int x = minX + 2; x < maxX - 2; x++ ) {
            skyWR.setSample(x, 0, 0, doubleNovalue);
            skyWR.setSample(x, 1, 0, doubleNovalue);
            skyWR.setSample(x, rows - 2, 0, doubleNovalue);
            skyWR.setSample(x, rows - 1, 0, doubleNovalue);
        }
        outSky = CoverageUtilities.buildCoverage("skyview factor", skyWR, attribute, inElev.getCoordinateReferenceSystem());

    }

    protected WritableRaster normalVector( WritableRaster pitWR, double res ) {

        /*
         * Initialize the Image of the normal vector in the central point of the
         * cells, which have 3 components so the Image have 3 bands..
         */
        SampleModel sm = RasterFactory.createBandedSampleModel(5, cols, rows, 3);
        WritableRaster tmpNormalVectorWR = CoverageUtilities.createWritableRaster(cols, rows, null, sm, 0.0);
        /*
         * apply the corripio's formula (is the formula (3) in the article)
         */
        int maxY = minX + rows;
        int maxX = minX + cols;
        for( int y = minY; y < maxY - 1; y++ ) {
            for( int x = minX; x < maxX - 1; x++ ) {
                double zij = pitWR.getSampleDouble(x, y, 0);
                double zidxj = pitWR.getSampleDouble(x + 1, y, 0);
                double zijdy = pitWR.getSampleDouble(x, y + 1, 0);
                double zidxjdy = pitWR.getSampleDouble(x + 1, y + 1, 0);
                double firstComponent = 0.5 * res * (zij - zidxj + zijdy - zidxjdy);
                double secondComponent = 0.5 * res * (zij + zidxj - zijdy - zidxjdy);
                double thirthComponent = res * res;
                tmpNormalVectorWR.setPixel(x, y, new double[]{firstComponent, secondComponent, thirthComponent});

            }
        }

        /*
         * Evaluate the value of the normal vector at the node as the mean of
         * the four value around, and normalize it.
         */
        WritableRaster normalVectorWR = CoverageUtilities.createWritableRaster(cols, rows, null, sm, 0.0);
        maxSlope = 3.13 / 2.0;
        for( int y = minY; y < maxY; y++ ) {
            for( int x = minX; x < maxX; x++ ) {
                normalVectorWR.setSample(x, y, 0, 1.0);
                normalVectorWR.setSample(x, y, 1, 1.0);
                normalVectorWR.setSample(x, y, 2, 1.0);

            }
        }
        for( int y = minY; y < maxY; y++ ) {
            for( int x = minX; x < maxX; x++ ) {
                double area = 0;
                double mean[] = new double[3];
                boolean isValidValue = true;
                for( int k = 0; k < 3; k++ ) {
                    double g00 = 1;
                    double g10 = 1;
                    double g01 = 1;
                    double g11 = 1;
                    if (y > 0 && x > 0) {
                        g00 = tmpNormalVectorWR.getSampleDouble(x - 1, y - 1, k);
                        g10 = tmpNormalVectorWR.getSampleDouble(x, y - 1, k);
                        g01 = tmpNormalVectorWR.getSampleDouble(x - 1, y, k);
                        g11 = tmpNormalVectorWR.getSampleDouble(x, y, k);
                    }

                    if (!isNovalue(g00) && !isNovalue(g01) && !isNovalue(g10) && !isNovalue(g11)) {
                        mean[k] = 1. / 4. * (g00 + g01 + g10 + g11);
                    } else {
                        isValidValue = false;
                        break;
                    }
                    area = area + mean[k] * mean[k];

                }
                if (isValidValue) {
                    area = Math.sqrt(area);
                    for( int k = 0; k < 3; k++ ) {
                        normalVectorWR.setSample(x, y, k, mean[k] / area);
                        if (x > minX + 1 && x < cols - 2 && y > minY + 1 && y < rows - 2 && k == 2) {
                            if (mean[k] / area < maxSlope)
                                maxSlope = mean[k] / area;
                        }
                    }

                }
            }
        }

        maxSlope = (int) (Math.acos(maxSlope) * 180.0 / Math.PI);

        return normalVectorWR;

    }

    /**
     * Calculate the skyview factor.
     * 
     * @param pitWR
     *            the dem ( the map of elevation).
     * @param res the resolution of the map.
     * @return the map of sky view factor.
     */
    private WritableRaster skyviewfactor( WritableRaster pitWR, double res ) {

        /*
         * evalutating the normal vector (in the center of the square compound
         * of 4 pixel.
         */

        normalVectorWR = normalVector(pitWR, res);

        WritableRaster skyviewFactorWR = CoverageUtilities.createWritableRaster(cols, rows, null, pitWR.getSampleModel(),
                0.0);
        pm.beginTask(msg.message("skyview.calculating"), 35);
        for( int i = 0; i < 360 - 10; i = i + 10 ) {
            azimuth = Math.toRadians(i * 1.0);
            WritableRaster skyViewWR = CoverageUtilities.createWritableRaster(cols, rows, null, pitWR.getSampleModel(),
                    Math.toRadians(maxSlope));
            for( int j = (int) maxSlope; j >= 0; j-- ) {

                elevation = Math.toRadians(j * 1.0);
                double[] sunVector = calcSunVector();
                double[] inverseSunVector = calcInverseSunVector(sunVector);
                double[] normalSunVector = calcNormalSunVector(sunVector);
                calculateFactor(rows, cols, sunVector, inverseSunVector, normalSunVector, pitWR, skyViewWR, res);

            }
            for( int t = normalVectorWR.getMinY(); t < normalVectorWR.getMinY() + normalVectorWR.getHeight(); t++ ) {
                for( int k = normalVectorWR.getMinX(); k < normalVectorWR.getMinX() + normalVectorWR.getWidth(); k++ ) {
                    double tmp = skyViewWR.getSampleDouble(k, t, 0);
                    skyViewWR.setSample(k, t, 0, Math.cos(tmp) * Math.cos(tmp) * 10.0 / 360.0);
                }
            }

            for( int q = 0; q < skyviewFactorWR.getWidth(); q++ ) {
                for( int k = 0; k < skyviewFactorWR.getHeight(); k++ ) {
                    double tmp = skyviewFactorWR.getSampleDouble(q, k, 0);
                    skyviewFactorWR.setSample(q, k, 0, tmp + skyViewWR.getSampleDouble(q, k, 0));
                }
            }
            pm.worked(1);
        }
        pm.done();
        return skyviewFactorWR;
    }

    /**
     * Calculate the angle.
     * 
     * @param x the x index.
     * @param y the y index.
     * @param tmpWR the sky map.
     * @param pitWR the elevation map.
     * @param res the resolution of the map.
     * @param normalSunVector
     * @param inverseSunVector
     * @param sunVector
     */
    protected WritableRaster shadow( int x, int y, WritableRaster tmpWR, WritableRaster pitWR, double res,
            double[] normalSunVector, double[] inverseSunVector, double[] sunVector ) {
        int n = 0;
        double zcompare = -Double.MAX_VALUE;
        double dx = (inverseSunVector[0] * n);
        double dy = (inverseSunVector[1] * n);
        int nCols = tmpWR.getWidth();
        int nRows = tmpWR.getHeight();
        int idx = (int) (x + dx);
        int jdy = (int) (y + dy);
        double vectorToOrigin[] = new double[3];
        while( idx >= 0 && idx <= nCols - 1 && jdy >= 0 && jdy <= nRows - 1 ) {
            vectorToOrigin[0] = dx * res;
            vectorToOrigin[1] = dy * res;
            vectorToOrigin[2] = pitWR.getSampleDouble(idx, jdy, 0);
            double zprojection = scalarProduct(vectorToOrigin, normalSunVector);
            double nGrad[] = normalVectorWR.getPixel(idx, jdy, new double[3]);
            double cosinc = scalarProduct(sunVector, nGrad);
            double elevRad = elevation;
            if ((cosinc >= 0) && (zprojection > zcompare)) {
                tmpWR.setSample(idx, jdy, 0, elevRad);
                zcompare = zprojection;
            }
            n = n + 1;
            dy = (inverseSunVector[1] * n);
            dx = (inverseSunVector[0] * n);
            idx = (int) Math.round(x + dx);
            jdy = (int) Math.round(y + dy);
        }
        return tmpWR;

    }

    protected void calculateFactor( int h, int w, double[] sunVector, double[] inverseSunVector, double[] normalSunVector,
            WritableRaster demWR, WritableRaster skyViewWR, double dx ) {

        double casx = 1e6 * sunVector[0];
        double casy = 1e6 * sunVector[1];
        int f_i = 0;
        int f_j = 0;

        if (casx <= 0) {
            f_i = 0;
        } else {
            f_i = w - 1;
        }

        if (casy <= 0) {
            f_j = 0;
        } else {
            f_j = h - 1;
        }

        int j = f_j;
        for( int i = 0; i < skyViewWR.getWidth(); i++ ) {
            shadow(i, j, skyViewWR, demWR, dx, normalSunVector, inverseSunVector, sunVector);
        }
        int i = f_i;
        for( int k = 0; k < skyViewWR.getHeight(); k++ ) {
            shadow(i, k, skyViewWR, demWR, dx, normalSunVector, inverseSunVector, sunVector);
        }

    }

    protected double[] calcSunVector() {
        return new double[]{sin(azimuth) * cos(elevation), -cos(azimuth) * cos(elevation), sin(elevation)};
    }

}
