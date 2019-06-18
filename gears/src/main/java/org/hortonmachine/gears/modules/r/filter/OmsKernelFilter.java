package org.hortonmachine.gears.modules.r.filter;
import static org.hortonmachine.gears.libs.modules.HMConstants.isNovalue;
import static org.hortonmachine.gears.libs.modules.Variables.BINARY;
import static org.hortonmachine.gears.libs.modules.Variables.COSINE;
import static org.hortonmachine.gears.libs.modules.Variables.DISTANCE;
import static org.hortonmachine.gears.libs.modules.Variables.EPANECHNIKOV;
import static org.hortonmachine.gears.libs.modules.Variables.GAUSSIAN;
import static org.hortonmachine.gears.libs.modules.Variables.INVERSE_DISTANCE;
import static org.hortonmachine.gears.libs.modules.Variables.QUARTIC;
import static org.hortonmachine.gears.libs.modules.Variables.TRIANGULAR;
import static org.hortonmachine.gears.libs.modules.Variables.TRIWEIGHT;

import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;

import javax.media.jai.KernelJAI;
import javax.media.jai.iterator.RandomIter;
import javax.media.jai.iterator.RandomIterFactory;
import javax.media.jai.iterator.WritableRandomIter;

import oms3.annotations.Author;
import oms3.annotations.Description;
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
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.RegionMap;
import org.hortonmachine.gears.utils.coverage.CoverageUtilities;
import org.jaitools.media.jai.kernel.KernelFactory;
import org.jaitools.media.jai.kernel.KernelFactory.ValueType;

@Description("A Kernel based filter.")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("kernel, filter, raster")
@Label(HMConstants.RASTERPROCESSING)
@Name("kernelfilter")
@Status(Status.EXPERIMENTAL)
@License(HMConstants.GPL3_LICENSE)
public class OmsKernelFilter extends HMModel {
    @Description("An input raster")
    @In
    public GridCoverage2D inRaster;

    @Description("The kernel to use.")
    @UI("combo:" + BINARY + "," + COSINE + "," + DISTANCE + "," //
            + EPANECHNIKOV + "," + GAUSSIAN + "," + INVERSE_DISTANCE + "," //
            + QUARTIC + "," + TRIANGULAR + "," + TRIWEIGHT)
    @In
    public String pKernel = EPANECHNIKOV;

    @Description("The kernel radius to use in cells (default = 10).")
    @In
    public int pRadius = 10;

    @Description("Filtered raster")
    @Out
    public GridCoverage2D outRaster;

    public void process() throws Exception {
        checkNull(inRaster);

        RegionMap regionMap = CoverageUtilities.getRegionParamsFromGridCoverage(inRaster);
        int cols = regionMap.getCols();
        int rows = regionMap.getRows();

        ValueType type = getKernelType(pKernel);

        KernelJAI kernel = KernelFactory.createCircle(pRadius, type);

        RenderedImage inImg = inRaster.getRenderedImage();
        RandomIter inIter = RandomIterFactory.create(inImg, null);

        WritableRaster outWR = CoverageUtilities.createWritableRaster(cols, rows, null, null, HMConstants.doubleNovalue);
        WritableRandomIter outIter = RandomIterFactory.createWritable(outWR, null);

        float[] kernelData = kernel.getKernelData();
        pm.beginTask("Processing...", cols - 2 * pRadius);
        for( int r = pRadius; r < rows - pRadius; r++ ) {
            for( int c = pRadius; c < cols - pRadius; c++ ) {
                double kernelSum = 0.0;
                int k = 0;
                double outputValue = 0.0;
                for( int kr = -pRadius; kr <= pRadius; kr++, k++ ) {
                    for( int kc = -pRadius; kc <= pRadius; kc++ ) {
                        double value = inIter.getSampleDouble(c + kc, r + kr, 0);
                        if (!isNovalue(value)) {
                            outputValue = outputValue + value * kernelData[k];
                            kernelSum = kernelSum + kernelData[k];
                        }
                    }
                }
                outIter.setSample(c, r, 0, outputValue / kernelSum);
            }
            pm.worked(1);
        }
        pm.done();

        outRaster = CoverageUtilities.buildCoverage("filtered", outWR, regionMap, inRaster.getCoordinateReferenceSystem());
    }

    private static ValueType getKernelType( String pKernel2 ) {
        ValueType type = null;
        pKernel2 = pKernel2.trim();
        if (pKernel2.equals(BINARY)) {
            type = KernelFactory.ValueType.BINARY;
        } else if (pKernel2.equals(COSINE)) {
            type = KernelFactory.ValueType.COSINE;
        } else if (pKernel2.equals(DISTANCE)) {
            type = KernelFactory.ValueType.DISTANCE;
        } else if (pKernel2.equals(GAUSSIAN)) {
            type = KernelFactory.ValueType.GAUSSIAN;
        } else if (pKernel2.equals(INVERSE_DISTANCE)) {
            type = KernelFactory.ValueType.INVERSE_DISTANCE;
        } else if (pKernel2.equals(QUARTIC)) {
            type = KernelFactory.ValueType.QUARTIC;
        } else if (pKernel2.equals(TRIANGULAR)) {
            type = KernelFactory.ValueType.TRIANGULAR;
        } else if (pKernel2.equals(TRIWEIGHT)) {
            type = KernelFactory.ValueType.TRIWEIGHT;
        } else if (pKernel2.equals(EPANECHNIKOV)) {
            type = KernelFactory.ValueType.EPANECHNIKOV;
        } else {
            throw new ModelsIllegalargumentException("Kernel type not recognised: " + pKernel2, "OmsKernelFilter");
        }
        return type;
    }
    /**
     * Smooth an array of values with a gaussian blur.
     * 
     * @param values the values to smooth.
     * @param kernelRadius the radius of the kernel to use.
     * @return the smoothed values.
     * @throws Exception
     */
    public static double[] gaussianSmooth( double[] values, int kernelRadius ) throws Exception {
        int size = values.length;
        double[] newValues = new double[values.length];
        double[] kernelData2D = makeGaussianKernel(kernelRadius);
        for( int i = 0; i < kernelRadius; i++ ) {
            newValues[i] = values[i];
        }
        for( int r = kernelRadius; r < size - kernelRadius; r++ ) {
            double kernelSum = 0.0;
            double outputValue = 0.0;
            int k = 0;
            for( int kc = -kernelRadius; kc <= kernelRadius; kc++, k++ ) {
                double value = values[r + kc];
                if (!isNovalue(value)) {
                    outputValue = outputValue + value * kernelData2D[k];
                    kernelSum = kernelSum + kernelData2D[k];
                }
            }
            newValues[r] = outputValue / kernelSum;
        }
        for( int i = size - kernelRadius; i < size; i++ ) {
            newValues[i] = values[i];
        }
        return newValues;
    }

    /**
     * Smooth an array of values with an averaging moving window.
     * 
     * @param values the values to smooth.
     * @param lookAhead the size of half of the window. 
     * @return the smoothed values.
     * @throws Exception
     */
    public static double[] averageSmooth( double[] values, int lookAhead ) throws Exception {
        int size = values.length;
        double[] newValues = new double[values.length];
        for( int i = 0; i < lookAhead; i++ ) {
            newValues[i] = values[i];
        }
        for( int i = lookAhead; i < size - lookAhead; i++ ) {
            double sum = 0.0;
            int k = 0;
            for( int l = -lookAhead; l <= lookAhead; l++ ) {
                double value = values[i + l];
                if (!isNovalue(value)) {
                    sum = sum + value;
                    k++;
                }
            }
            newValues[i] = sum / k;
        }
        for( int i = size - lookAhead; i < size; i++ ) {
            newValues[i] = values[i];
        }
        return newValues;
    }

    /**
     * Make a Gaussian blur kernel.
     */
    public static double[] makeGaussianKernel( int radius ) {
        int rows = radius * 2 + 1;
        double r = (double) radius;
        double[] matrix = new double[rows];
        double sigma = r / 3;
        double sigma22 = 2 * sigma * sigma;
        double sigmaPi2 = 2 * Math.PI * sigma;
        double sqrtSigmaPi2 = Math.sqrt(sigmaPi2);
        double radius2 = r * r;
        double total = 0;
        int index = 0;
        for( int row = -radius; row <= radius; row++ ) {
            double distance = row * row;
            if (distance > radius2)
                matrix[index] = 0;
            else
                matrix[index] = (double) Math.exp(-(distance) / sigma22) / sqrtSigmaPi2;
            total += matrix[index];
            index++;
        }
        for( int i = 0; i < rows; i++ )
            matrix[i] /= total;

        return matrix;
    }

}
