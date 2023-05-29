package org.hortonmachine.hmachine.modules.statistics.kerneldensity;

import oms3.annotations.Execute;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;
import org.jaitools.media.jai.kernel.KernelFactory;

import javax.media.jai.KernelJAI;
import java.io.IOException;
import java.util.stream.IntStream;

public class OmsStructuralSimilarity extends HMModel {


    public GridCoverage2D inMap1 = null;

    public GridCoverage2D inMap2 = null;

    public double pK1 = 0.01;
    public double pK2 = 0.03;

    public double pRelevanceMean = 1;
    public double pRelevanceVariance = 1;
    public double pRelevancePattern = 1;

    public int pKernel = 3;

    public int pRadius = 10;

    public boolean doConstant = false;

    public GridCoverage2D outStructuralSimilarity = null;

    public GridCoverage2D outMeanSimilarity = null;

    public GridCoverage2D outVarianceSimilarity = null;

    public GridCoverage2D outPatternSimilarity = null;


    private volatile boolean errorOccurred = false;
    private volatile String errorMessage;


    @Execute
    public void process() throws Exception {

        GridCoverage2D meanMap1 = windowMean(inMap1);
        GridCoverage2D meanMap2 = windowMean(inMap2);

        GridCoverage2D varianceMap1 = windowVariance(inMap1,meanMap1);
        GridCoverage2D varianceMap2 = windowVariance(inMap2,meanMap2);

        GridCoverage2D covarianceMap = windowCovariance(inMap1,inMap2,meanMap1,meanMap2);

        try (HMRaster meanRaster1 = HMRaster.fromGridCoverage(meanMap1);
             HMRaster meanRaster2 = HMRaster.fromGridCoverage(meanMap2);
             HMRaster varianceRaster1 = HMRaster.fromGridCoverage(varianceMap1);
             HMRaster varianceRaster2 = HMRaster.fromGridCoverage(varianceMap2);
             HMRaster covarianceRaster = HMRaster.fromGridCoverage(covarianceMap)
        ) {
            int cols = meanRaster1.getCols();
            int rows = meanRaster1.getRows();

            // TODO: replace 0.0 by the value range among the two comparable rasters.
            double c1 = Math.pow(pK1 * 0.0,2);
            double c2 = Math.pow(pK2 * 0.0,2);
            double c3 = 0.5*c2;

            HMRaster outputSSRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inMap1).build();
            HMRaster outputMSRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inMap1).build();
            HMRaster outputVSRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inMap1).build();
            HMRaster outputPSRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inMap1).build();

            pm.beginTask("Estimating structural similarity...", cols );

            IntStream.range(0, rows).parallel().forEach(r -> {
                if(errorOccurred) {
                    return;
                }
                for( int c = 0; c < cols; c++ ) {

                    double meanValue1 = meanRaster1.getValue(c, r);
                    double meanValue2 = meanRaster2.getValue(c, r);
                    double varianceValue1 = varianceRaster1.getValue(c, r);
                    double varianceValue2 = varianceRaster2.getValue(c, r);
                    double covarianceValue = covarianceRaster.getValue(c, r);

                    if (meanRaster1.isNovalue(meanValue1) || meanRaster2.isNovalue(meanValue2) || varianceRaster1.isNovalue(varianceValue1) || varianceRaster2.isNovalue(varianceValue2) || covarianceRaster.isNovalue(covarianceValue)) {
                        continue;
                    }

                    double meanSimilarity = (2*meanValue1*meanValue2 + c1)/(meanValue1*meanValue1 + meanValue2*meanValue2 + c1 );
                    double varianceSimilarity = (2*Math.sqrt(varianceValue1)*Math.sqrt(varianceValue2) +c2)/(varianceValue1 + varianceValue2 + c2);
                    double patternSimilarity = (covarianceValue + c3)/(Math.sqrt(varianceValue1)*Math.sqrt(varianceValue2) + c3);
                    double structuralSimilarity = Math.pow(meanSimilarity,pRelevanceMean)*Math.pow(varianceSimilarity,pRelevanceVariance)*Math.pow(patternSimilarity,pRelevancePattern);

                    try {
                        outputSSRaster.setValue(c, r, structuralSimilarity);
                        outputMSRaster.setValue(c, r, meanSimilarity);
                        outputVSRaster.setValue(c, r, varianceSimilarity);
                        outputPSRaster.setValue(c, r, patternSimilarity);
                    } catch (IOException e) {
                        errorOccurred = true;
                        errorMessage = e.getLocalizedMessage();
                    }
                }
                pm.worked(1);
            });

            pm.done();

            if (errorOccurred) {
                throw new ModelsRuntimeException(errorMessage, this);
            }

            outStructuralSimilarity = outputSSRaster.buildCoverage();
            outMeanSimilarity = outputVSRaster.buildCoverage();
            outVarianceSimilarity = outputVSRaster.buildCoverage();
            outPatternSimilarity = outputPSRaster.buildCoverage();
        }

    }


    private GridCoverage2D windowCovariance(GridCoverage2D inMap1, GridCoverage2D inMap2, GridCoverage2D meanMap1, GridCoverage2D meanMap2) throws Exception {

        checkNull(inMap1);
        checkNull(inMap2);
        checkNull(meanMap1);
        checkNull(meanMap2);

        try (HMRaster inRaster1 = HMRaster.fromGridCoverage(inMap1);
             HMRaster meanRaster1 = HMRaster.fromGridCoverage(meanMap1);
             HMRaster inRaster2 = HMRaster.fromGridCoverage(inMap2);
             HMRaster meanRaster2 = HMRaster.fromGridCoverage(meanMap2)
             ) {

            int cols = inRaster1.getCols();
            int rows = inRaster1.getRows();

            KernelFactory.ValueType type = getKernelType();
            KernelJAI kernel = KernelFactory.createCircle(pRadius, type);
            HMRaster outputRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inMap1).build();
            float[] kernelData = kernel.getKernelData();

            pm.beginTask("Estimating kernel density...", cols - 2 * pRadius);

            IntStream.range(pRadius, rows - pRadius).parallel().forEach(r -> {
                if(errorOccurred) {
                    return;
                }
                for( int c = pRadius; c < cols - pRadius; c++ ) {

                    double inputValue1 = inRaster1.getValue(c, r);
                    if (inRaster1.isNovalue(inputValue1)) {
                        continue;
                    }
                    double inputValue2 = inRaster2.getValue(c, r);
                    if (inRaster2.isNovalue(inputValue2)) {
                        continue;
                    }

                    double meanValue1 = meanRaster1.getValue(c,r);
                    double meanValue2 = meanRaster2.getValue(c,r);
                    if (meanRaster1.isNovalue(meanValue1) || meanRaster2.isNovalue(meanValue2)) {
                        continue;
                    }

                    if (doConstant) {
                        inputValue1 = 1.0;
                        inputValue2 = 1.0;
                    }
                    int k = 0;
                    double outputValue = 0.0;
                    for( int kr = -pRadius; kr <= pRadius; kr++ ) {
                        for( int kc = -pRadius; kc <= pRadius; kc++ ) {
                            double value1 = inRaster1.getValue(c + kc, r + kr);
                            if (inRaster1.isNovalue(value1)) {
                                value1 = 0;
                            }
                            double value2 = inRaster2.getValue(c + kc, r + kr);
                            if (inRaster2.isNovalue(value2)) {
                                value2 = 0;
                            }
                            try {
                                outputValue = outputValue + kernelData[k++] * (value1 - meanValue1) * (value2 - meanValue2);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    try {
                        outputRaster.setValue(c, r, outputValue);
                    } catch (IOException e) {
                        errorOccurred = true;
                        errorMessage = e.getLocalizedMessage();
                    }
                }
                pm.worked(1);
            });
            pm.done();

            if (errorOccurred) {
                throw new ModelsRuntimeException(errorMessage, this);
            }

            return outputRaster.buildCoverage();
        }
    }

    private GridCoverage2D windowVariance(GridCoverage2D inMap, GridCoverage2D meanMap) throws Exception {

        checkNull(inMap);
        checkNull(meanMap);

        try (HMRaster inRaster = HMRaster.fromGridCoverage(inMap); HMRaster meanRaster = HMRaster.fromGridCoverage(meanMap)) {

            int cols = inRaster.getCols();
            int rows = inRaster.getRows();

            KernelFactory.ValueType type = getKernelType();
            KernelJAI kernel = KernelFactory.createCircle(pRadius, type);
            HMRaster outputRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inMap).build();
            float[] kernelData = kernel.getKernelData();

            pm.beginTask("Estimating kernel density...", cols - 2 * pRadius);

            IntStream.range(pRadius, rows - pRadius).parallel().forEach(r -> {
                if(errorOccurred) {
                    return;
                }
                for( int c = pRadius; c < cols - pRadius; c++ ) {
                    double inputValue = inRaster.getValue(c, r);
                    if (inRaster.isNovalue(inputValue)) {
                        continue;
                    }

                    double meanValue = meanRaster.getValue(c,r);
                    if (meanRaster.isNovalue(meanValue)) {
                        continue;
                    }

                    if (doConstant)
                        inputValue = 1.0;

                    int k = 0;
                    double outputValue = 0.0;
                    for( int kr = -pRadius; kr <= pRadius; kr++ ) {
                        for( int kc = -pRadius; kc <= pRadius; kc++ ) {
                            double value = inRaster.getValue(c + kc, r + kr);
                            if (inRaster.isNovalue(value)) {
                                value = 0;
                            }
                            try {
                                outputValue = outputValue + kernelData[k++] * Math.pow((value - meanValue),2);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    try {
                        outputRaster.setValue(c, r, outputValue);
                    } catch (IOException e) {
                        errorOccurred = true;
                        errorMessage = e.getLocalizedMessage();
                    }
                }
                pm.worked(1);
            });
            pm.done();

            if (errorOccurred) {
                throw new ModelsRuntimeException(errorMessage, this);
            }

            return outputRaster.buildCoverage();
        }
    }


    private GridCoverage2D windowMean(GridCoverage2D inMap) throws Exception {

        checkNull(inMap);

        try (HMRaster inRaster = HMRaster.fromGridCoverage(inMap)) {

            int cols = inRaster.getCols();
            int rows = inRaster.getRows();

            KernelFactory.ValueType type = getKernelType();
            KernelJAI kernel = KernelFactory.createCircle(pRadius, type);
            HMRaster outputRaster = new HMRaster.HMRasterWritableBuilder().setTemplate(inMap).build();
            float[] kernelData = kernel.getKernelData();

            pm.beginTask("Estimating kernel density...", cols - 2 * pRadius);

            IntStream.range(pRadius, rows - pRadius).parallel().forEach(r -> {
                if(errorOccurred) {
                    return;
                }
                for( int c = pRadius; c < cols - pRadius; c++ ) {
                    double inputValue = inRaster.getValue(c, r);
                    if (inRaster.isNovalue(inputValue)) {
                        continue;
                    }

                    if (doConstant)
                        inputValue = 1.0;

                    int k = 0;
                    double outputValue = 0.0;
                    for( int kr = -pRadius; kr <= pRadius; kr++ ) {
                        for( int kc = -pRadius; kc <= pRadius; kc++ ) {
                            double value = inRaster.getValue(c + kc, r + kr);
                            if (inRaster.isNovalue(value)) {
                                value = 0;
                            }
                            try {
                                outputValue = outputValue + kernelData[k++] * value;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    try {
                        outputRaster.setValue(c, r, outputValue);
                    } catch (IOException e) {
                        errorOccurred = true;
                        errorMessage = e.getLocalizedMessage();
                    }
                }
                pm.worked(1);
            });
            pm.done();

            if (errorOccurred) {
                throw new ModelsRuntimeException(errorMessage, this);
            }

            return outputRaster.buildCoverage();
        }
    }

    private KernelFactory.ValueType getKernelType(){
        KernelFactory.ValueType type = KernelFactory.ValueType.EPANECHNIKOV;
        switch( pKernel ) {
            case 0:
                type = KernelFactory.ValueType.BINARY;
                break;
            case 1:
                type = KernelFactory.ValueType.COSINE;
                break;
            case 2:
                type = KernelFactory.ValueType.DISTANCE;
                break;
            case 4:
                type = KernelFactory.ValueType.GAUSSIAN;
                break;
            case 5:
                type = KernelFactory.ValueType.INVERSE_DISTANCE;
                break;
            case 6:
                type = KernelFactory.ValueType.QUARTIC;
                break;
            case 7:
                type = KernelFactory.ValueType.TRIANGULAR;
                break;
            case 8:
                type = KernelFactory.ValueType.TRIWEIGHT;
                break;
        }
        return type;
    }





    public static int getCodeForType( KernelFactory.ValueType type ) {
        switch( type ) {
            case BINARY:
                return 0;
            case COSINE:
                return 1;
            case DISTANCE:
                return 2;
            case EPANECHNIKOV:
                return 3;
            case GAUSSIAN:
                return 4;
            case INVERSE_DISTANCE:
                return 5;
            case QUARTIC:
                return 6;
            case TRIANGULAR:
                return 7;
            case TRIWEIGHT:
                return 8;
            default:
                throw new ModelsIllegalargumentException("No kernel type: " + type, "OmsKernelDensity");
        }
    }


}
