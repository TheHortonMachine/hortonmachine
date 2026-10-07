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
package org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow;

import static org.hortonmachine.gears.libs.modules.HMConstants.HYDROGEOMORPHOLOGY;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_KEYWORDS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_LABEL;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_LICENSE;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_NAME;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.OmsPeakflow.OMSPEAKFLOW_STATUS;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.eclipse.imagen.iterator.RandomIter;
import org.eclipse.imagen.iterator.RandomIterFactory;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.hortonmachine.gears.utils.math.interpolation.LinearListInterpolator;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.discharge.QReal;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.discharge.QStatistic;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.iuh.IUHCalculator;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.iuh.IUHDiffusion;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.iuh.IUHKinematic;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.jeff.RealJeff;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.peakflow.core.jeff.StatisticJeff;
import org.hortonmachine.hmachine.modules.statistics.cb.OmsCb;
import org.joda.time.DateTime;

import oms3.annotations.Author;
import oms3.annotations.Bibliography;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.Unit;

@Description(OMSPEAKFLOW_DESCRIPTION)
@Bibliography({OmsPeakflow.BIBLIOGRAPHY_RIGON_2011, OmsPeakflow.BIBLIOGRAPHY_RODRIGUEZ_ITURBE_1979})
@Author(name = OMSPEAKFLOW_AUTHORNAMES, contact = OMSPEAKFLOW_AUTHORCONTACTS)
@Keywords(OMSPEAKFLOW_KEYWORDS)
@Label(OMSPEAKFLOW_LABEL)
@Name(OMSPEAKFLOW_NAME)
@Status(OMSPEAKFLOW_STATUS)
@License(OMSPEAKFLOW_LICENSE)
public class OmsPeakflow extends HMModel {

    @Description(OMSPEAKFLOW_pA_DESCRIPTION)
    @Unit("mm/h^n")
    @In
    public double pA = -1.0;

    @Description(OMSPEAKFLOW_pN_DESCRIPTION)
    @In
    public double pN = -1.0;

    @Description(OMSPEAKFLOW_pCelerity_DESCRIPTION)
    @Unit("m/s")
    @In
    public double pCelerity = -1.0;

    @Description(OMSPEAKFLOW_pDiffusionSup_DESCRIPTION)
    @Unit("m²/s")
    @In
    public double pDiffusionSup = -9999.0;

    @Description(OMSPEAKFLOW_pDiffusionSubSup_DESCRIPTION)
    @Unit("m²/s")
    @In
    public double pDiffusionSubSup = -9999.0;

    @Description(OMSPEAKFLOW_pSat_DESCRIPTION)
    @Unit("%")
    @In
    public double pSat = -1.0;
    
    @Description(OMSPEAKFLOW_pOutputStepArg_DESCRIPTION)
    @Unit("s")
    @In
    public double pOutputStepArg = 60;

    @Description(OMSPEAKFLOW_inTopindex_DESCRIPTION)
    @In
    public GridCoverage2D inTopindex = null;

    @Description(OMSPEAKFLOW_inSat_DESCRIPTION)
    @In
    public GridCoverage2D inSat = null;

    @Description(OMSPEAKFLOW_inRescaledsup_DESCRIPTION)
    @In
    public GridCoverage2D inRescaledsup = null;

    @Description(OMSPEAKFLOW_inRescaledsub_DESCRIPTION)
    @In
    public GridCoverage2D inRescaledsub = null;

    @Description(OMSPEAKFLOW_inRainfall_DESCRIPTION)
    @Unit("mm/h")
    @In
    public HashMap<DateTime, double[]> inRainfall;

    @Description(OMSPEAKFLOW_outDischarge_DESCRIPTION)
    @Unit("m³/s")
    @Out
    public HashMap<DateTime, double[]> outDischarge;

    public static final String OMSPEAKFLOW_DESCRIPTION = "Computes the flood hydrograph at the outlet of a basin with the geomorphological instantaneous unit hydrograph (GIUH), built from the width functions of the surface and of the subsurface flow. With the parameters a and n of the rainfall depth-duration-frequency curve, it finds the rainfall duration that gives the largest discharge, and its peak; with a measured rainfall event, it computes the hydrograph of the event. The surface runoff comes from the saturated areas of the basin, defined by the topographic index or by a map.";
    public static final String BIBLIOGRAPHY_RIGON_2011 = "Rigon, R., D'Odorico, P., Bertoldi, G. (2011). The geomorphic structure of the runoff peak. Hydrology and Earth System Sciences, 15(6), 1853-1863. doi:10.5194/hess-15-1853-2011";
    public static final String BIBLIOGRAPHY_RODRIGUEZ_ITURBE_1979 = "Rodríguez-Iturbe, I., Valdés, J. B. (1979). The geomorphologic structure of hydrologic response. Water Resources Research, 15(6), 1409-1420.";
    public static final String OMSPEAKFLOW_DOCUMENTATION = "";
    public static final String OMSPEAKFLOW_KEYWORDS = "Peakflow, Discharge, Flood, GIUH, Width function, Hydrologic";
    public static final String OMSPEAKFLOW_LABEL = HYDROGEOMORPHOLOGY;
    public static final String OMSPEAKFLOW_NAME = "peakflow";
    public static final int OMSPEAKFLOW_STATUS = 40;
    public static final String OMSPEAKFLOW_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSPEAKFLOW_AUTHORNAMES = "Silvia Franceschi, Andrea Antonello, Riccardo Rigon";
    public static final String OMSPEAKFLOW_AUTHORCONTACTS = "http://www.hydrologis.com, http://www.ing.unitn.it/dica/hp/?user=rigon";
    public static final String OMSPEAKFLOW_pA_DESCRIPTION = "The coefficient a of the rainfall depth-duration-frequency curve h = a·t^n, with the depth h in mm and the duration t in hours, for the chosen return period. With n, it selects the statistical rainfall.";
    public static final String OMSPEAKFLOW_pN_DESCRIPTION = "The exponent n of the rainfall depth-duration-frequency curve h = a·t^n, between 0 and 1.";
    public static final String OMSPEAKFLOW_pCelerity_DESCRIPTION = "The mean velocity of the flow in the channels.";
    public static final String OMSPEAKFLOW_pDiffusionSup_DESCRIPTION = "The hydrodynamic diffusion of the surface flow: below 10 the kinematic unit hydrograph, with no diffusion, is used.";
    public static final String OMSPEAKFLOW_pDiffusionSubSup_DESCRIPTION = "The hydrodynamic diffusion of the subsurface flow.";
    public static final String OMSPEAKFLOW_pSat_DESCRIPTION = "The percentage of the basin area that is saturated, used with the topographic index: in floods, usually 40 to 60%, more in small basins.";
    public static final String OMSPEAKFLOW_pOutputStepArg_DESCRIPTION = "The interval between the discharge values written to the output; the model computes them every second.";
    public static final String OMSPEAKFLOW_inTopindex_DESCRIPTION = "The map of the topographic index: the cells with the highest values, as many as the saturated percentage, are the saturated areas that give surface runoff.";
    public static final String OMSPEAKFLOW_inSat_DESCRIPTION = "The map of the saturated areas, used instead of the topographic index: the cells with a value are saturated, the no-data cells are not.";
    public static final String OMSPEAKFLOW_inRescaledsup_DESCRIPTION = "The map of the rescaled distance from the outlet for the surface flow, with a low ratio between the channel and the hillslope velocities (5 to 20).";
    public static final String OMSPEAKFLOW_inRescaledsub_DESCRIPTION = "The optional map of the rescaled distance from the outlet for the subsurface flow, with a high ratio between the channel and the hillslope velocities (50 to 200).";
    public static final String OMSPEAKFLOW_inRainfall_DESCRIPTION = "The measured rainfall intensity, at a constant time step, used when a and n are not given.";
    public static final String OMSPEAKFLOW_outDischarge_DESCRIPTION = "The discharge at the outlet.";

    

    // private int basinStatus = 0; // dry/normal/wet
    // private double phi = -1d;
    // private double celerityRatio = -1d;

    private double xRes;
    private double yRes;
    /*
     * width functions
     */
    public double[][] widthFunctionSuperficial;
    public double[][] widthFunctionSubSuperficial;
    public double[][] volumeCheckMatrix;

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

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRescaledsup);
        cols = regionMap.cols;
        rows = regionMap.rows;
        xRes = regionMap.xres;
        yRes = regionMap.yres;

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
                    "At least one of the topindex or the saturation map have to be available to proceed.", this, pm);
        }

        GridCoverage2D widthfunctionSupCoverage = CoverageUtilities.buildCoverage("sup", supRescaledWR, regionMap,
                inRescaledsup.getCoordinateReferenceSystem());
        double[][] widthfunctionSupCb = doCb(widthfunctionSupCoverage);

        double[][] widthfunctionSubCb = null;
        if (inRescaledsub != null) {
            GridCoverage2D widthfunctionSubCoverage = CoverageUtilities.buildCoverage("sub", subRescaledWR, regionMap,
                    inRescaledsup.getCoordinateReferenceSystem());
            widthfunctionSubCb = doCb(widthfunctionSubCoverage);
        }

        setSuperficialWidthFunction(widthfunctionSupCb);
        if (inRescaledsub != null) {
            setSubSuperficialWidthFunction(widthfunctionSubCb);
        }

        // check the case
        if (pA != -1 && pN != -1 && widthfunctionSupCb != null && pCelerity != -1 && pDiffusionSup != -1) {
            pm.message("Peakflow launched in statistic mode...");
            isStatistics = true;
            isReal = false;
        } else if (widthfunctionSupCb != null && pCelerity != -1 && pDiffusionSup != -1 && inRainfall != null) {
            pm.message("Peakflow launched with real rain...");
            isStatistics = false;
            isReal = true;
        } else {
            throw new ModelsIllegalargumentException(
                    "Problems occurred in parsing the command arguments. Please check your arguments.", this, pm);
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
        parameterBox.setDiffusionParameterSup(pDiffusionSup);
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
            parameterBox.setDiffusionParameterSubSup(pDiffusionSubSup);
            parameterBox.setDelta_sub(deltaSub);
            parameterBox.setNpixel_sub(pixelTotalSub);
            parameterBox.setTime_sub(timeSubArray);
            parameterBox.setArea_sub(areaSub);
            parameterBox.setPxl_sub(pixelSubArray);
            parameterBox.setResid_time(residentTime);

            effectsBox.setAmpi_sub(widthFunctionSubSuperficial);
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

            if (pDiffusionSup < 10) {
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
            volumeCheckMatrix = qtotal.getVolumeCheck();

            pm.message("Maximum rainfall duration: " + qtotal.getTpMax());
            pm.message("Maximum discharge value: " + qtotal.calculateQmax());

            for( int i = 0; i < calculateQ.length; i++ ) {
                if (i % pOutputStepArg != 0)
                    continue;
                DateTime tmpDate = dummyDate.plusSeconds((int) calculateQ[i][0]);
                double[] value = new double[1];
                value[0] = calculateQ[i][1];
                outDischarge.put(tmpDate, value);
            }
        } else if (isReal) {
            IUHCalculator iuhC = null;

            if (pDiffusionSup < 10) {
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
            volumeCheckMatrix = qtotal.getVolumeCheck();

            // pm.message("Maximum rainfall duration: " + qtotal.getTpMax());
            // pm.message("Maximum discharge value: " + qtotal.calculateQmax());
            DateTime firstDate = jeffC.getFirstDate();
            for( int i = 0; i < calculateQ.length; i++ ) {
                if (i % pOutputStepArg != 0)
                    continue;
                DateTime tmpDate = firstDate.plusSeconds((int) calculateQ[i][0]);
                double[] value = new double[1];
                value[0] = calculateQ[i][1];
                outDischarge.put(tmpDate, value);
            }
        } else {
            throw new ModelsIllegalargumentException("Statistic and real rain are implemented only.",
                    this.getClass().getSimpleName(), pm);
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
        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
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

        for( int r = 0; r < rows; r++ ) {
            for( int c = 0; c < cols; c++ ) {
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

    private void setSubSuperficialWidthFunction( double[][] widthfunctionSubCb ) {
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
        double cum = 0f;
        for( int i = 0; i < widthFunctionLength; i++ ) {
            widthFunctionSubSuperficial[i][0] = timeSubArray[i] / pCelerity;
            widthFunctionSubSuperficial[i][1] = pixelSubArray[i] * xRes * yRes / deltaSub * pCelerity;
            cum = cum + pixelSubArray[i] / pixelTotalSub;
            widthFunctionSubSuperficial[i][2] = cum;
        }
    }

    private double[][] doCb( GridCoverage2D coverage ) throws Exception {
        OmsCb topindexCb = new OmsCb();
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
