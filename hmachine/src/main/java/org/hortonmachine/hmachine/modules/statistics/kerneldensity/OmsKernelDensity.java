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
package org.hortonmachine.hmachine.modules.statistics.kerneldensity;

import static org.hortonmachine.hmachine.modules.statistics.kerneldensity.OmsKernelDensity.OMSKERNELDENSITY_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.statistics.kerneldensity.OmsKernelDensity.OMSKERNELDENSITY_AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.statistics.kerneldensity.OmsKernelDensity.OMSKERNELDENSITY_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.statistics.kerneldensity.OmsKernelDensity.OMSKERNELDENSITY_KEYWORDS;
import static org.hortonmachine.hmachine.modules.statistics.kerneldensity.OmsKernelDensity.OMSKERNELDENSITY_LABEL;
import static org.hortonmachine.hmachine.modules.statistics.kerneldensity.OmsKernelDensity.OMSKERNELDENSITY_LICENSE;
import static org.hortonmachine.hmachine.modules.statistics.kerneldensity.OmsKernelDensity.OMSKERNELDENSITY_NAME;
import static org.hortonmachine.hmachine.modules.statistics.kerneldensity.OmsKernelDensity.OMSKERNELDENSITY_STATUS;

import java.io.IOException;
import java.util.stream.IntStream;

import org.eclipse.imagen.KernelImageN;
import org.eclipse.imagen.media.kernel.KernelFactory;
import org.eclipse.imagen.media.kernel.KernelFactory.ValueType;
import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.exceptions.ModelsRuntimeException;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.modules.HMRaster;

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

@Description(OMSKERNELDENSITY_DESCRIPTION)
@Author(name = OMSKERNELDENSITY_AUTHORNAMES, contact = OMSKERNELDENSITY_AUTHORCONTACTS)
@Keywords(OMSKERNELDENSITY_KEYWORDS)
@Label(OMSKERNELDENSITY_LABEL)
@Name(OMSKERNELDENSITY_NAME)
@Status(OMSKERNELDENSITY_STATUS)
@License(OMSKERNELDENSITY_LICENSE)
public class OmsKernelDensity extends HMModel {

    @Description(OMSKERNELDENSITY_inMap_DESCRIPTION)
    @In
    public GridCoverage2D inMap = null;

    @Description(OMSKERNELDENSITY_pKernel_DESCRIPTION)
    @In
    public int pKernel = 3;

    @Description(OMSKERNELDENSITY_pRadius_DESCRIPTION)
    @In
    public int pRadius = 10;

    @Description(OMSKERNELDENSITY_doConstant_DESCRIPTION)
    @In
    public boolean doConstant = false;

    @Description(OMSKERNELDENSITY_outDensity_DESCRIPTION)
    @Out
    public GridCoverage2D outDensity = null;
    
    
    public static final String OMSKERNELDENSITY_DESCRIPTION = "Kernel Density Estimator (based on the Jaitools project).";
    public static final String OMSKERNELDENSITY_DOCUMENTATION = "";
    public static final String OMSKERNELDENSITY_KEYWORDS = "Kernel Density, Raster";
    public static final String OMSKERNELDENSITY_LABEL = "Raster Processing";
    public static final String OMSKERNELDENSITY_NAME = "kerneldenisty";
    public static final int OMSKERNELDENSITY_STATUS = 5;
    public static final String OMSKERNELDENSITY_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSKERNELDENSITY_AUTHORNAMES = "Andrea Antonello, Diego Bengocheapaz";
    public static final String OMSKERNELDENSITY_AUTHORCONTACTS = "http://jaitools.org, www.hydrologis.com";
    public static final String OMSKERNELDENSITY_inMap_DESCRIPTION = "The input map.";
    public static final String OMSKERNELDENSITY_pKernel_DESCRIPTION = "The kernel to use.";
    public static final String OMSKERNELDENSITY_pRadius_DESCRIPTION = "The kernel radius to use in cells (default = 10).";
    public static final String OMSKERNELDENSITY_doConstant_DESCRIPTION = "Use a constant value for the existing input map values instead of the real map value (default = false).";
    public static final String OMSKERNELDENSITY_outDensity_DESCRIPTION = "The kernel density estimation.";


    private volatile boolean errorOccurred = false;
    private volatile String errorMessage;
    
    
    @Execute
    public void process() throws Exception {
        checkNull(inMap);

        try (HMRaster inRaster = HMRaster.fromGridCoverage(inMap)) {

            int cols = inRaster.getCols();
            int rows = inRaster.getRows();

            ValueType type = KernelFactory.ValueType.EPANECHNIKOV;
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

            KernelImageN kernel = KernelFactory.createCircle(pRadius, type);

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
                            outputValue = outputValue + kernelData[k++] * value;
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

            outDensity = outputRaster.buildCoverage();
        }
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
