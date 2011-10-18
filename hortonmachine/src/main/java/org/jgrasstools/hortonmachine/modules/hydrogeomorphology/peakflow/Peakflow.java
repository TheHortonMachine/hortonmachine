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
package org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow;

import static org.jgrasstools.gears.libs.modules.JGTConstants.*;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;

import oms3.annotations.Author;
import oms3.annotations.Documentation;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.geotools.coverage.grid.GridCoverage2D;
import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.coverage.CoverageUtilities;
import org.jgrasstools.gears.utils.math.interpolation.LinearListInterpolator;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow.core.discharge.QReal;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow.core.discharge.QStatistic;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow.core.iuh.IUHCalculator;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow.core.iuh.IUHDiffusion;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow.core.iuh.IUHKinematic;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow.core.jeff.RealJeff;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.peakflow.core.jeff.StatisticJeff;
import org.jgrasstools.hortonmachine.modules.statistics.cb.Cb;
import org.joda.time.DateTime;

@Description("The Peakflow semidistributed hydrologic model.")
@Documentation("Peakflow.html")
@Author(name = "Silvia Franceschi, Andrea Antonello, Riccardo Rigon", contact = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon")
@Keywords("Peakflow, Discharge, Hydrologic, Cb, RescaledDistance")
@Label(JGTConstants.HYDROGEOMORPHOLOGY)
@Name("peakflow")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class Peakflow extends JGTModel {

    @Description("The a parameter for statistic rain calculations.")
    @Unit("mm/h^m")
    @In
    public double pA = -1f;

    @Description("The n parameter for statistic rain calculations.")
    @In
    public double pN = -1f;

    @Description("The channel celerity parameter.")
    @Unit("m/s")
    @In
    public double pCelerity = -1f;

    @Description("The diffusion parameter.")
    @Unit("m2/s")
    @In
    public double pDiffusion = -1f;

    @Description("The saturation percentage.")
    @Unit("%")
    @In
    public double pSat = -1f;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of Topindex.")
    @In
    public GridCoverage2D inTopindex = null;

    @Description("Optional map of saturation.")
    @In
    public GridCoverage2D inSat = null;

    @Description("The map of superficial rescaled distance.")
    @In
    public GridCoverage2D inRescaledsup = null;

    @Description("The map of sub-superficial rescaled distance.")
    @In
    public GridCoverage2D inRescaledsub = null;

    @Description("The sorted hasmap of rainfall data per timestep.")
    @In
    public HashMap<DateTime, double[]> inRainfall;

    @Description("The sorted hashmap of peakflow output per timestep.")
    @Out
    public HashMap<DateTime, double[]> outDischarge;

    public double oututstepArg = 100;

    // private int basinStatus = 0; // dry/normal/wet
    // private double phi = -1d;
    // private double celerityRatio = -1d;

    private double xRes;
    private double yRes;
    /*
     * width functions
     */
    private double[][] widthFunctionSuperficial;
    private double[][] widthFunctionSubSuperficialHelper;
    private double[][] widthFunctionSubSuperficial;

    private double residentTime = -1;
    private double[] timeSubArray;
    private double[] timeSupArray;
    private double areaSup;
    private double deltaSup;
    private double pixelTotalSup;
    private double[] pixelSupArray;
    private double areaSub;
    private double deltaSub;
    private double[] pixelSubArray;
    private double pixelTotalSub;

    private ParameterBox parameterBox = new ParameterBox();
    private EffectsBox effectsBox = new EffectsBox();

    private boolean isReal = false;
    private boolean isStatistics = false;

    private int cols;

    private int rows;

    @Execute
    public void process() throws Exception {
        checkNull(inRescaledsup);

        HashMap<String, Double> regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRescaledsup);
        cols = regionMap.get(CoverageUtilities.COLS).intValue();
        rows = regionMap.get(CoverageUtilities.ROWS).intValue();
        xRes = regionMap.get(CoverageUtilities.XRES);
        yRes = regionMap.get(CoverageUtilities.YRES);

        RenderedImage supRescaledRI = inRescaledsup.getRenderedImage();
        WritableRaster supRescaledWR = CoverageUtilities.renderedImage2WritableRaster(supRescaledRI, false);

        WritableRaster subRescaledWR = null;
        if (inRescaledsub != null) {
            RenderedImage subRescaledRI = inRescaledsub.getRenderedImage();
            subRescaledWR = CoverageUtilities.renderedImage2WritableRaster(subRescaledRI, false);
        }

        if (inTopindex != null) {
            processWithTopIndex(supRescaledWR, subRescaledWR);
        } else if (inSat != null) {
            processWithSaturation(inSat, supRescaledWR, subRescaledWR);
        } else {
            throw new ModelsIllegalargumentException(
                    "At least one of the topindex or the saturation map have to be available to proceed.", this);
        }

        GridCoverage2D widthfunctionSupCoverage = CoverageUtilities.buildCoverage("sup", supRescaledWR, regionMap,
                inTopindex.getCoordinateReferenceSystem());
        double[][] widthfunctionSupCb = doCb(widthfunctionSupCoverage);

        double[][] widthfunctionSubCb = null;
        if (inRescaledsub != null) {
            GridCoverage2D widthfunctionSubCoverage = CoverageUtilities.buildCoverage("sub", subRescaledWR, regionMap,
                    inTopindex.getCoordinateReferenceSystem());
            widthfunctionSubCb = doCb(widthfunctionSubCoverage);
        }

        setSuperficialWidthFunction(widthfunctionSupCb);
        if (inRescaledsub != null) {
            setSubSuperficialAmplitude(widthfunctionSubCb);
        }

        // check the case
        if (pA != -1 && pN != -1 && widthfunctionSupCb != null && pCelerity != -1 && pDiffusion != -1) {
            pm.message("Peakflow launched in statistic mode...");
            isStatistics = true;
            isReal = false;
        } else if (widthfunctionSupCb != null && pCelerity != -1 && pDiffusion != -1 && inRainfall != null) {
            pm.message("Peakflow launched with real rain...");
            isStatistics = false;
            isReal = true;
        } else {
            throw new ModelsIllegalargumentException(
                    "Problems occurred in parsing the command arguments. Please check your arguments.", this);
        }

        // the internal timestep is always 1 second
        double timestep = 1f;

        // /*
        // * Calculate the tcorr as the one calculated for the superficial discharge if we have
        // * only Superficial flow, an for the subsuperficial discharge otherwise
        // */
        // double tcorr = 0f;
        // if (timeSubArray != null) {
        // tcorr = timeSubArray[timeSubArray.length - 1] / channelCelerity;
        // } else {
        // tcorr = timeSupArray[timeSupArray.length - 1] / channelCelerity;
        // }

        /*
         * prepare all the needed parameters by the core algorithms
         */

        /*
         * this needs to be integrated into the interface
         */
        parameterBox.setN_idf(pN);
        parameterBox.setA_idf(pA);
        parameterBox.setArea(areaSup);
        parameterBox.setTimestep(timestep);
        parameterBox.setDiffusionparameter(pDiffusion);
        parameterBox.setVc(pCelerity);
        parameterBox.setDelta(deltaSup);
        parameterBox.setXres(xRes);
        parameterBox.setYres(yRes);
        parameterBox.setNpixel(pixelTotalSup);
        parameterBox.setSize(widthfunctionSupCb.length);
        parameterBox.setTime(timeSupArray);
        parameterBox.setPxl(pixelSupArray);

        effectsBox.setAmpi(widthFunctionSuperficial);

        if (timeSubArray != null) {
            parameterBox.setSubsuperficial(true);
            parameterBox.setDelta_sub(deltaSub);
            parameterBox.setNpixel_sub(pixelTotalSub);
            parameterBox.setTime_sub(timeSubArray);
            parameterBox.setArea_sub(areaSub);
            parameterBox.setPxl_sub(pixelSubArray);
            parameterBox.setResid_time(residentTime);

            effectsBox.setAmpi_sub(widthFunctionSubSuperficial);
            effectsBox.setAmpi_help_sub(widthFunctionSubSuperficialHelper);
        }

        // if (isScs) {
        // parameterBox.setVcvv(celerityRatio);
        // parameterBox.setBasinstate(basinStatus);
        // parameterBox.setPhi(phi);
        // parameterBox.setScs(true);
        // }

        effectsBox.setRainDataExists(inRainfall != null ? true : false);
        outDischarge = new LinkedHashMap<DateTime, double[]>();
        if (isStatistics) {
            DateTime dummyDate = new DateTime();
            IUHCalculator iuhC = null;

            if (pDiffusion < 10) {
                pm.message("IUH Kinematic...");
                iuhC = new IUHKinematic(effectsBox, parameterBox, pm);
            } else {
                pm.message("IUH Diffusion...");
                iuhC = new IUHDiffusion(effectsBox, parameterBox, pm);
            }
            pm.message("Statistic Jeff...");
            StatisticJeff jeffC = new StatisticJeff(parameterBox, iuhC.getTpMax(), pm);
            pm.message("Q calculation...");
            QStatistic qtotal = new QStatistic(parameterBox, iuhC, jeffC, pm);
            double[][] calculateQ = qtotal.calculateQ();

            pm.message("Maximum rainfall duration: " + qtotal.getTpMax());
            pm.message("Maximum discharge value: " + qtotal.calculateQmax());

            for( int i = 0; i < calculateQ.length; i++ ) {
                if (i % oututstepArg != 0)
                    continue;
                DateTime tmpDate = dummyDate.plusSeconds((int) calculateQ[i][0]);
                double[] value = new double[1];
                value[0] = calculateQ[i][1];
                outDischarge.put(tmpDate, value);
            }
        } else if (isReal) {
            IUHCalculator iuhC = null;

            if (pDiffusion < 10) {
                pm.message("IUH Kinematic...");
                iuhC = new IUHKinematic(effectsBox, parameterBox, pm);
            } else {
                pm.message("IUH Diffusion...");
                iuhC = new IUHDiffusion(effectsBox, parameterBox, pm);
            }
            pm.message("Read rain data...");

            pm.message("Real Jeff...");
            RealJeff jeffC = new RealJeff(inRainfall);
            pm.message("Q calculation...");
            QReal qtotal = new QReal(parameterBox, iuhC, jeffC, pm);
            double[][] calculateQ = qtotal.calculateQ();

            // pm.message("Maximum rainfall duration: " + qtotal.getTpMax());
            // pm.message("Maximum discharge value: " + qtotal.calculateQmax());
            DateTime firstDate = jeffC.getFirstDate();
            for( int i = 0; i < calculateQ.length; i++ ) {
                if (i % oututstepArg != 0)
                    continue;
                DateTime tmpDate = firstDate.plusSeconds((int) calculateQ[i][0]);
                double[] value = new double[1];
                value[0] = calculateQ[i][1];
                outDischarge.put(tmpDate, value);
            }
        } else {
            throw new ModelsIllegalargumentException("Statistic and real rain are implemented only.", this.getClass()
                    .getSimpleName());
        }

        /*
         * here two ways can be taken 1) standard peakflow theory 2) peakflow hybrid with SCS
         */
        // if (isStatistics || isReal) {
        // if (!peakflowStandard()) {
        // // throw some
        // }
        // } else if (isScs) {
        // if (!peakflowScs()) {
        // // throw some
        // }
        // }
    }

    private void processWithSaturation( GridCoverage2D sat, WritableRaster supRescaledWR, WritableRaster subRescaledWR ) {
        RandomIter satIter = CoverageUtilities.getRandomIterator(sat);
        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
                double saturation = satIter.getSampleDouble(c, r, 0);
                if (!isNovalue(saturation)) {
                    if (subRescaledWR != null) {
                        subRescaledWR.setSample(c, r, 0, doubleNovalue);
                    }
                } else {
                    supRescaledWR.setSample(c, r, 0, doubleNovalue);
                }
            }
        }
    }

    private void processWithTopIndex( WritableRaster supRescaledWR, WritableRaster subRescaledWR ) throws Exception {
        double[][] topindexCb = doCb(inTopindex);

        // cumulate topindex
        for( int i = 0; i < topindexCb.length; i++ ) {
            if (i > 0) {
                topindexCb[i][1] = topindexCb[i][1] + topindexCb[i - 1][1];
            }
        }
        double max = topindexCb[topindexCb.length - 1][1];
        // normalize
        for( int i = 0; i < topindexCb.length; i++ ) {
            topindexCb[i][1] = topindexCb[i][1] / max;
        }

        List<Double> meanValueList = new ArrayList<Double>();
        List<Double> cumulatedValueList = new ArrayList<Double>();
        for( int i = 0; i < topindexCb.length; i++ ) {
            meanValueList.add(topindexCb[i][0]);
            cumulatedValueList.add(topindexCb[i][1]);
        }

        LinearListInterpolator interpolator = new LinearListInterpolator(meanValueList, cumulatedValueList);
        double topindexThreshold = interpolator.linearInterpolateX(1 - pSat / 100);

        RenderedImage topindexRI = inTopindex.getRenderedImage();
        RandomIter topindexIter = RandomIterFactory.create(topindexRI, null);

        for( int c = 0; c < cols; c++ ) {
            for( int r = 0; r < rows; r++ ) {
                double topindex = topindexIter.getSampleDouble(c, r, 0);
                if (topindex >= topindexThreshold) {
                    if (subRescaledWR != null) {
                        subRescaledWR.setSample(c, r, 0, doubleNovalue);
                    }
                } else {
                    supRescaledWR.setSample(c, r, 0, doubleNovalue);
                }
            }
        }
    }

    private void setSuperficialWidthFunction( double[][] widthfunctionSupCb ) {
        int widthFunctionLength = widthfunctionSupCb.length;
        pixelTotalSup = 0.0;
        double timeTotalNum = 0.0;

        timeSupArray = new double[widthFunctionLength];
        pixelSupArray = new double[widthFunctionLength];

        for( int i = 0; i < widthfunctionSupCb.length; i++ ) {
            timeSupArray[i] = widthfunctionSupCb[i][0];
            pixelSupArray[i] = widthfunctionSupCb[i][1];

            pixelTotalSup = pixelTotalSup + pixelSupArray[i];
            timeTotalNum = timeTotalNum + timeSupArray[i];
        }

        areaSup = pixelTotalSup * xRes * yRes;
        deltaSup = (timeSupArray[widthFunctionLength - 1] - timeSupArray[0]) / (widthFunctionLength - 1);
        // double avgTime = timeTotalNum / amplitudeFunctionLength;
        widthFunctionSuperficial = new double[widthFunctionLength][3];
        double cum = 0.0;
        for( int i = 0; i < widthFunctionLength; i++ ) {
            widthFunctionSuperficial[i][0] = timeSupArray[i] / pCelerity;
            widthFunctionSuperficial[i][1] = pixelSupArray[i] * xRes * yRes / deltaSup * pCelerity;
            double tmpSum = pixelSupArray[i] / pixelTotalSup;
            cum = cum + tmpSum;
            widthFunctionSuperficial[i][2] = cum;
        }
    }

    private void setSubSuperficialAmplitude( double[][] widthfunctionSubCb ) {
        int widthFunctionLength = widthfunctionSubCb.length;

        pixelTotalSub = 0;
        double timeTotalNum = 0;
        timeSubArray = new double[widthFunctionLength];
        pixelSubArray = new double[widthFunctionLength];

        for( int i = 0; i < widthfunctionSubCb.length; i++ ) {
            timeSubArray[i] = widthfunctionSubCb[i][0];
            pixelSubArray[i] = widthfunctionSubCb[i][1];

            pixelTotalSub = pixelTotalSub + pixelSubArray[i];
            timeTotalNum = timeTotalNum + timeSubArray[i];
        }

        areaSub = pixelTotalSub * xRes * yRes;
        deltaSub = (timeSubArray[widthFunctionLength - 1] - timeSubArray[0]) / (widthFunctionLength - 1);
        double avgTime = timeTotalNum / widthFunctionLength;

        residentTime = avgTime / pCelerity;

        widthFunctionSubSuperficial = new double[widthFunctionLength][3];
        widthFunctionSubSuperficialHelper = new double[widthFunctionLength][3];
        double cum = 0f;
        for( int i = 0; i < widthFunctionLength; i++ ) {
            widthFunctionSubSuperficialHelper[i][0] = timeSubArray[i] / pCelerity;
            widthFunctionSubSuperficialHelper[i][1] = pixelSubArray[i] * xRes * yRes / deltaSub * pCelerity;
            cum = cum + pixelSubArray[i] / pixelTotalSub;
            widthFunctionSubSuperficialHelper[i][2] = cum;
        }
    }

    private double[][] doCb( GridCoverage2D coverage ) throws Exception {
        Cb topindexCb = new Cb();
        topindexCb.inRaster1 = coverage;
        topindexCb.pFirst = 1;
        topindexCb.pLast = 2;
        topindexCb.pBins = 100;
        topindexCb.pm = pm;
        topindexCb.process();
        double[][] moments = topindexCb.outCb;
        return moments;
    }

}
