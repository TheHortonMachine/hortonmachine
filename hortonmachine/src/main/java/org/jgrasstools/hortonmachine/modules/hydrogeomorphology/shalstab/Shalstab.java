/*
 * JGrass - Free Open Source Java GIS http://www.jgrass.org 
 * (C) HydroloGIS - www.hydrologis.com 
 * 
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Library General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option) any
 * later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Library General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU Library General Public License
 * along with this library; if not, write to the Free Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307 USA
 */
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.shalstab;

import static java.lang.Math.atan;
import static java.lang.Math.pow;
import static java.lang.Math.sin;
import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;
import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.HashMap;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Out;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.ConstantRandomIter;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
/**
 * <p>
 * The openmi compliant representation of the Shalstab model. It calculates the
 * the proneness to instability of each pixel based on an infinite slope model
 * with steady hydrologic conditions. The output is composed of two maps: the
 * map of the potentially unstable pixels and the map of the minimum steady
 * state rainfall to cause instability. The formula used is the following:
 * </p>
 * <p>
 * a/b >= (T * sin&#952; / q) * &#961; * [1 - (tg&#952; / tg&#934;) + C * (1 +
 * tg&#952;^2) / (tg&#934; * &#961;s * g * z)]
 * </p>
 * <p>
 * where:
 * <li>a (m^2) is the contributing area draining across</li>
 * <li>b (m) the contour length of the lower bound</li>
 * <li>T (m^2 / day) is the soil transmissivity when saturated</li>
 * <li>&#952; (degrees) is the local slope</li>
 * <li>&#961; is the ratio between soil bulk density is the friction angle</li>
 * <li>q (mm/day) the net rainfall rate</li>
 * <li>g is the gravitational acceleration</li>
 * <li>z (m) is the soil thickness</li>
 * <li>C (Pa) is the effective soil cohesion</li>
 * </p>
 * <p>
 * The output is a map of values with the following meaning:
 * <li>1 : unconditionally unstable;</li>
 * <li>2 : unconditionally stable;</li>
 * <li>3 : stable;</li>
 * <li>4 : unstable;</li>
 * <li>8888 : pixel characterized by rock (if soil thickness < 0.01)</li>
 * </p>
 * <p>
 * Minimum rainfall to instability, the formula used is the following:
 * </p>
 * <p>
 * qcrit >= (T * sin&#952; / (a/b)) * &#961; * [1 - (tg&#952; / tg&#934;) + C *
 * (1 + tg&#952;^2) / (tg&#934; * &#961;s * g * z)]
 * </p>
 * <p>
 * The output is a map of values with the following meaning:
 * <li>1 : 0 <= qcrit < 50</li>
 * <li>2 : 50 <= qcrit < 100</li>
 * <li>3 : 100 <= qcrit < 200</li>
 * <li>4 : qcrit >= 200</li>
 * <li>5 : unconditionally unstable</li>
 * <li>0 : unconditionally stable</li>
 * </p>
 * <p>
 * <DT><STRONG>Inputs:</STRONG></DT>
 * <DD>
 * <OL>
 * <LI>the map of slope (-slopemap);</LI>
 * <LI>the map of a/b (-abmap);
 * <LI>the map of trasmissivity (-trasmissivitymap);</LI>
 * <LI>the map of cohesion (-cohesionmap);</LI>
 * <LI>the map of soil thickness(-hsmap);</LI>
 * <LI>the map of tg&#952; (-tgphimap);</LI>
 * <LI>the map of the ratio between soil bulk density is the friction angle
 * (-rhomap);</LI>
 * <LI>the map of the net rainfall rate (-qmap);</LI>
 * </LI>
 * </OL>
 * <P></DD>
 * <DT><STRONG>Returns:</STRONG></DT>
 * <DD>
 * <OL>
 * <LI>the map of the minimum rainfall to instability (-qcritmap);</LI>
 * <LI>the map of classes (-classimap);</LI>
 * </OL>
 * <P></DD>
 * Usage: h.shalstab --igrass-slopemap slope --igrass-abmap ab
 * --igrass-trasmissivitymap trasmissivity --igrass-cohesionmap cohesion
 * --igrass-hsmap hs --igrass-tgphimap tgphi --igrass-rhomap rho --igrass-qmap q
 * --ograss-qcritmap qcrit --ograss-classimap classi
 * </p>
 * <p>
 * Usage: h.shalstab --igrass-slopemap slope --igrass-abmap ab
 * --trasmissivityconst trasmissivity --cohesionconst cohesion--hsconst hs
 * --tgphiconst tgphi --rhoconst rho --qconst q --ograss-qcritmap qcrit
 * --ograss-classimap classi --ocats-catsqcrit qcrit map name --ocats-catsclass
 * class map name
 * </p>
 * <P>
 * Usage with categories: h.shalstab --igrass-slopemap slope --igrass-abmap ab
 * --igrass-trasmissivitymap trasmissivity --igrass-cohesionmap cohesion
 * --igrass-hsmap hs --igrass-tgphimap tgphi --igrass-rhomap rho --igrass-qmap q
 * --ograss-qcritmap qcrit --ograss-classimap classi --ocats-catsqcrit qcrit map
 * name --ocats-catsclass class map name
 * </p>
 * <p>
 * Usage with categories: h.shalstab --igrass-slopemap slope --igrass-abmap ab
 * --trasmissivityconst trasmissivity --cohesionconst cohesion--hsconst hs
 * --tgphiconst tgphi --rhoconst rho --qconst q --ograss-qcritmap qcrit
 * --ograss-classimap classi
 * </p>
 * <p>
 * Note: It is possible to use a map or a constant value for trasmissivity,
 * tgphi, cohesion, hs, q, rho
 * </p>
 * <p>
 * <DT><STRONG>References:</STRONG></DT>
 * <LI>R. Montgomery, W.E. Dietrich. A physically based model for the
 * topographic control on shallow landsliding, Water Resources Research, Vol. 30
 * NO.4, Pages. 1153-1171, 1994</LI>
 * <LI>R. Montgomery, K. Sullivan and H. Greenberg. Regional test of a model for
 * shallow landsliding, Hydrological Processes, 12 , Pages. 943-955, 1998</LI>
 * </p>
 * 
 * @author Erica Ghesla - erica.ghesla@ing.unitn.it, Matteo Dall’Amico, Silvano
 *         Pisoni, Andrea Antonello, Riccardo Rigon
 */
public class Shalstab extends JGTModel {

    @Description("The map of slope.")
    @In
    public GridCoverage2D inSlope = null;

    @Description("The map of contributing area.")
    @In
    public GridCoverage2D inTca = null;

    @Description("The map of trasmissivity.")
    @In
    public GridCoverage2D inTrasmissivity = null;

    @Description("A constant of trasmissivity to use instead of the map.")
    @In
    public double pTrasmissivity = -1.0;

    @Description("The map of the friction tangent angle.")
    @In
    public GridCoverage2D inTgphi = null;

    @Description("A constant of friction tangent angle to use instead of the map.")
    @In
    public double pTgphi = -1.0;

    @Description("The map of cohesion.")
    @In
    public GridCoverage2D inCohesion = null;

    @Description("A constant of cohesion to use instead of the map.")
    @In
    public double pCohesion = -1.0;

    @Description("The map of soil depth.")
    @In
    public GridCoverage2D inSdepth = null;

    @Description("A constant of soil depth to use instead of the map.")
    @In
    public double pSdepth = -1.0;

    @Description("The map of effective precipitation.")
    @In
    public GridCoverage2D inQ = null;

    @Description("A constant of effective precipitation to use instead of the map.")
    @In
    public double pQ = -1.0;

    @Description("The map of rho.")
    @In
    public GridCoverage2D inRho = null;

    @Description("A constant of rho to use instead of the map.")
    @In
    public double pRho = -1.0;

    @Description("A value for slope for rock.")
    @In
    public double pRock = -9999.0;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("The map of qcrit.")
    @Out
    public GridCoverage2D outQcrit = null;

    @Description("The map of classi.")
    @Out
    public GridCoverage2D outShalstab = null;

    public final double EPS = 0.01;

    /**
     * Value to be given to pixels if <code>h_s < eps</code>.
     */
    private final double ROCK = 8888.0;

    @Execute
    public void process() throws Exception {
        if (!concatOr(outShalstab == null, doReset)) {
            return;
        }
        
        if (pRock == -9999.0)
            pRock = 5.67;

        RenderedImage slopeRI = inSlope.getRenderedImage();
        RenderedImage abRI = inTca.getRenderedImage();

        RandomIter trasmissivityIter = null;
        if (inTrasmissivity != null) {
            RenderedImage trasmissivityRI = inTrasmissivity.getRenderedImage();
            trasmissivityIter = RandomIterFactory.create(trasmissivityRI, null);
        } else {
            trasmissivityIter = new ConstantRandomIter(pTrasmissivity);
        }

        RandomIter tghiIter = null;
        if (inTgphi != null) {
            RenderedImage tgphiRI = inTgphi.getRenderedImage();
            tghiIter = RandomIterFactory.create(tgphiRI, null);
        } else {
            tghiIter = new ConstantRandomIter(pTgphi);
        }

        RandomIter cohesionIter = null;
        if (inCohesion != null) {
            RenderedImage cohesionRI = inCohesion.getRenderedImage();
            cohesionIter = RandomIterFactory.create(cohesionRI, null);
        } else {
            cohesionIter = new ConstantRandomIter(pCohesion);
        }

        RandomIter hsIter = null;
        if (inSdepth != null) {
            RenderedImage hsRI = inSdepth.getRenderedImage();
            hsIter = RandomIterFactory.create(hsRI, null);
        } else {
            hsIter = new ConstantRandomIter(pSdepth);
        }

        RandomIter qIter = null;
        if (inQ != null) {
            RenderedImage qRI = inQ.getRenderedImage();
            qIter = RandomIterFactory.create(qRI, null);
        } else {
            qIter = new ConstantRandomIter(pQ);
        }

        RandomIter rhoIter = null;
        if (inRho != null) {
            RenderedImage rhoRI = inRho.getRenderedImage();
            rhoIter = RandomIterFactory.create(rhoRI, null);
        } else {
            rhoIter = new ConstantRandomIter(pRho);
        }

        qcrit(slopeRI, abRI, trasmissivityIter, tghiIter, cohesionIter, hsIter, qIter, rhoIter);
    }

    /**
     * Calculates the trasmissivity in every pixel of the map.
     */
    private void qcrit( RenderedImage slope, RenderedImage ab, RandomIter trasmissivityRI,
            RandomIter frictionRI, RandomIter cohesionRI, RandomIter soildRI,
            RandomIter effectiveRI, RandomIter densityRI ) {
        HashMap<String, Double> regionMap = CoverageUtilities
                .getRegionParamsFromGridCoverage(inSlope);
        int cols = regionMap.get(CoverageUtilities.COLS).intValue();
        int rows = regionMap.get(CoverageUtilities.ROWS).intValue();

        RandomIter slopeRI = RandomIterFactory.create(slope, null);
        RandomIter abRI = RandomIterFactory.create(ab, null);

        WritableRaster qcritWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null,
                null, null);
        WritableRandomIter qcritIter = RandomIterFactory.createWritable(qcritWR, null);
        WritableRaster classiWR = CoverageUtilities.createDoubleWritableRaster(cols, rows, null,
                null, null);
        WritableRandomIter classiIter = RandomIterFactory.createWritable(classiWR, null);

        pm.beginTask("Creating qcrit map...", rows);
        for( int j = 0; j < rows; j++ ) {
            pm.worked(1);
            for( int i = 0; i < cols; i++ ) {
                double slopeValue = slopeRI.getSampleDouble(i, j, 0);
                double tanPhiValue = frictionRI.getSampleDouble(i, j, 0);
                double cohValue = cohesionRI.getSampleDouble(i, j, 0);
                double rhoValue = densityRI.getSampleDouble(i, j, 0);
                double hsValue = soildRI.getSampleDouble(i, j, 0);
                if (!isNovalue(slopeValue) && !isNovalue(tanPhiValue) && !isNovalue(cohValue)
                        && !isNovalue(rhoValue)) {
                    if (hsValue <= EPS || slopeValue > pRock) {
                        qcritIter.setSample(i, j, 0, ROCK);
                    } else {
                        double checkUnstable = tanPhiValue + cohValue
                                / (9810.0 * rhoValue * hsValue) * (1 + pow(slopeValue, 2));
                        if (slopeValue >= checkUnstable) {
                            /*
                             * uncond unstable
                             */
                            qcritIter.setSample(i, j, 0, 5);
                        } else {
                            double checkStable = tanPhiValue * (1 - 1 / rhoValue) + cohValue
                                    / (9810 * rhoValue * hsValue) * (1 + pow(slopeValue, 2));
                            if (slopeValue < checkStable) {
                                /*
                                 * uncond. stable
                                 */
                                qcritIter.setSample(i, j, 0, 0);
                            } else {
                                double qCrit = trasmissivityRI.getSampleDouble(i, j, 0)
                                        * sin(atan(slopeValue))
                                        / abRI.getSampleDouble(i, j, 0)
                                        * rhoValue
                                        * (1 - slopeValue / tanPhiValue + cohValue
                                                / (9810 * rhoValue * hsValue * tanPhiValue)
                                                * (1 + pow(slopeValue, 2))) * 1000;
                                qcritIter.setSample(i, j, 0, qCrit);
                                /*
                                 * see the Qcrit (critical effective
                                 * precipitation) that leads the slope to
                                 * instability (see article of Montgomery et Al,
                                 * Hydrological Processes, 12, 943-955, 1998)
                                 */
                                double value = qcritIter.getSampleDouble(i, j, 0);
                                if (value > 0 && value < 50)
                                    qcritIter.setSample(i, j, 0, 1);
                                if (value >= 50 && value < 100)
                                    qcritIter.setSample(i, j, 0, 2);
                                if (value >= 100 && value < 200)
                                    qcritIter.setSample(i, j, 0, 3);
                                if (value >= 200)
                                    qcritIter.setSample(i, j, 0, 4);
                            }
                        }
                    }
                } else {
                    qcritIter.setSample(i, j, 0, doubleNovalue);
                }
            }
        }
        pm.done();

        /*
         * build the class matrix 1=inc inst 2=inc stab 3=stab 4=instab
         * rock=presence of rock
         */
        pm.beginTask("Creating stability map...", rows);
        double Tq = 0;
        for( int j = 0; j < rows; j++ ) {
            pm.worked(1);
            for( int i = 0; i < cols; i++ ) {
                Tq = trasmissivityRI.getSampleDouble(i, j, 0)
                        / effectiveRI.getSampleDouble(i, j, 0) / 1000.0;
                double slopeValue = slopeRI.getSampleDouble(i, j, 0);
                double abValue = abRI.getSampleDouble(i, j, 0);
                double tangPhiValue = frictionRI.getSampleDouble(i, j, 0);
                double cohValue = cohesionRI.getSampleDouble(i, j, 0);
                double rhoValue = densityRI.getSampleDouble(i, j, 0);
                double hsValue = soildRI.getSampleDouble(i, j, 0);

                if (!isNovalue(slopeValue) && !isNovalue(abValue) && !isNovalue(tangPhiValue)
                        && !isNovalue(cohValue) && !isNovalue(rhoValue)) {
                    if (hsValue <= EPS || slopeValue > pRock) {
                        classiIter.setSample(i, j, 0, ROCK);
                    } else {
                        double checkUncondUnstable = tangPhiValue + cohValue
                                / (9810 * rhoValue * hsValue) * (1 + pow(slopeValue, 2));
                        double checkUncondStable = tangPhiValue * (1 - 1 / rhoValue) + cohValue
                                / (9810 * rhoValue * hsValue) * (1 + pow(slopeValue, 2));
                        double checkStable = Tq
                                * sin(atan(slopeValue))
                                * rhoValue
                                * (1 - slopeValue / tangPhiValue + cohValue
                                        / (9810 * rhoValue * hsValue * tangPhiValue)
                                        * (1 + pow(slopeValue, 2)));
                        if (slopeValue >= checkUncondUnstable) {
                            classiIter.setSample(i, j, 0, 1);
                        } else if (slopeValue < checkUncondStable) {
                            classiIter.setSample(i, j, 0, 2);
                        } else if (abValue < checkStable
                                && classiIter.getSampleDouble(i, j, 0) != 1
                                && classiIter.getSampleDouble(i, j, 0) != 2) {
                            classiIter.setSample(i, j, 0, 3);
                        } else {
                            classiIter.setSample(i, j, 0, 4);
                        }
                    }
                } else {
                    classiIter.setSample(i, j, 0, doubleNovalue);
                }
            }
        }
        pm.done();

        outQcrit = CoverageUtilities.buildCoverage("qcrit", qcritWR, regionMap, inSlope
                .getCoordinateReferenceSystem());
        outShalstab = CoverageUtilities.buildCoverage("classi", classiWR, regionMap,
                inSlope.getCoordinateReferenceSystem());

    }

}
