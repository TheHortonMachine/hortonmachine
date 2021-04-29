/* This file is part of HortonMachine (http://www.hortonmachine.org)
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
package org.hortonmachine.modules;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_doIncludezero_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_doLogarithmic_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_fInterpolateid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_fPointZ_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_fStationsZ_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_fStationsid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_inData_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_inInterpolate_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_inInterpolationGrid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_inStations_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_outData_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_outGrid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pA_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pIntegralscale_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pMode_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pNug_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pS_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pSemivariogramType_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSKRIGING_pVariance_DESCRIPTION;

import java.util.HashMap;

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
import oms3.annotations.UI;

import org.geotools.coverage.grid.GridCoverage2D;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.kriging.old.OmsKriging;

@Description(OMSKRIGING_DESCRIPTION)
@Author(name = OMSKRIGING_AUTHORNAMES, contact = OMSKRIGING_AUTHORCONTACTS)
@Keywords(OMSKRIGING_KEYWORDS)
@Label(OMSKRIGING_LABEL)
@Name("_" + OMSKRIGING_NAME)
@Status(OMSKRIGING_STATUS)
@License(OMSKRIGING_LICENSE)
public class Kriging extends HMModel {

    @Description(OMSKRIGING_inStations_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inStations = null;

    @Description(OMSKRIGING_inData_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inData = null;

    @Description(OMSKRIGING_inInterpolate_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inInterpolate = null;

    @Description(OMSKRIGING_inInterpolationGrid_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_RASTER)
    @In
    public String inInterpolationGrid = null;

    @Description(OMSKRIGING_fStationsid_DESCRIPTION)
    @In
    public String fStationsid = null;

    @Description(OMSKRIGING_fStationsZ_DESCRIPTION)
    @In
    public String fStationsZ = null;

    @Description(OMSKRIGING_fInterpolateid_DESCRIPTION)
    @In
    public String fInterpolateid = null;

    @Description(OMSKRIGING_fPointZ_DESCRIPTION)
    @In
    public String fPointZ = null;

    @Description(OMSKRIGING_pMode_DESCRIPTION)
    @In
    public int pMode = 0;

    @Description("The integral scale as comma separated values.")
    @In
    public String pIntegralscale = null;

    @Description(OMSKRIGING_pVariance_DESCRIPTION)
    @In
    public double pVariance = 0;

    @Description(OMSKRIGING_doLogarithmic_DESCRIPTION)
    @In
    public boolean doLogarithmic = false;

    public int defaultVariogramMode = 0;

    @Description(OMSKRIGING_pSemivariogramType_DESCRIPTION)
    @In
    public double pSemivariogramType = 0;

    @Description(OMSKRIGING_doIncludezero_DESCRIPTION)
    @In
    public boolean doIncludezero = true;

    @Description(OMSKRIGING_pA_DESCRIPTION)
    @In
    public double pA;

    @Description(OMSKRIGING_pS_DESCRIPTION)
    @In
    public double pS;

    @Description(OMSKRIGING_pNug_DESCRIPTION)
    @In
    public double pNug;

    @Description(OMSKRIGING_outGrid_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outGrid = null;

    @Description(OMSKRIGING_outData_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @Out
    public String outData = null;

    @Execute
    public void process() throws Exception {

        OmsTimeSeriesIteratorReader reader = new OmsTimeSeriesIteratorReader();
        reader.file = inData;
        reader.idfield = fStationsid;
        // reader.tStart = "2000-01-01 00:00";
        // reader.tTimestep = 60;
        // reader.tEnd = "2000-01-01 00:00";
        reader.fileNovalue = "-9999";
        reader.initProcess();

        OmsKriging kriging = new OmsKriging();
        kriging.inStations = getVector(inStations);
        kriging.fStationsid = fStationsid;
        kriging.fStationsZ = fStationsZ;
        kriging.inInterpolate = getVector(inInterpolate);
        kriging.fInterpolateid = fInterpolateid;
        kriging.fPointZ = fPointZ;
        kriging.pMode = pMode;
        if (pIntegralscale!=null && pIntegralscale.trim().length()>0) {
            String[] split = pIntegralscale.split(",");
            double[] integralScaleDouble = new double[split.length];
            for( int i = 0; i < split.length; i++ ) {
                try {
                    integralScaleDouble[i] = Double.parseDouble(split[i]);
                } catch (Exception e) {
                    throw new ModelsIllegalargumentException("Problems with integral scale: " + pIntegralscale, this, pm);
                }
            }
            kriging.pIntegralscale = integralScaleDouble;
        }
        kriging.pVariance = pVariance;
        kriging.doLogarithmic = doLogarithmic;
        GridCoverage2D interpolationGrid = getRaster(inInterpolationGrid);
        if (interpolationGrid != null) {
            kriging.inInterpolationGrid = interpolationGrid.getGridGeometry();
        }
        kriging.defaultVariogramMode = defaultVariogramMode;
        kriging.pSemivariogramType = pSemivariogramType;
        kriging.doIncludezero = doIncludezero;
        kriging.pA = pA;
        kriging.pS = pS;
        kriging.pNug = pNug;
        kriging.pm = pm;
        kriging.doProcess = doProcess;
        kriging.doReset = doReset;

        OmsTimeSeriesIteratorWriter writer = null;

        pm.beginTask("Processing...", IHMProgressMonitor.UNKNOWN);
        while( reader.doProcess ) {
            reader.nextRecord();

            if (writer == null) {
                writer = new OmsTimeSeriesIteratorWriter();
                writer.file = outData;
                writer.tStart = reader.tStart;
                writer.tTimestep = reader.tTimestep;
            }
            pm.message("timestep: " + reader.tCurrent);

            HashMap<Integer, double[]> id2ValueMap = reader.outData;
            kriging.inData = id2ValueMap;
            kriging.process();

            // write csv data
            writer.inData = kriging.outData;
            writer.writeNextLine();

            // write raster data
            dumpRaster(kriging.outGrid, outGrid);
        }
        pm.done();

        reader.close();
        writer.close();
    }


}
