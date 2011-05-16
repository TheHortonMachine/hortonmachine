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
package org.jgrasstools.hortonmachine.modules.networktools.trento_p;

import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TDTP;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TMAX;
import static org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.Constants.DEFAULT_TPMIN;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Range;
import oms3.annotations.Status;
import oms3.annotations.Unit;

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.EpanetParametersTime;
import org.jgrasstools.hortonmachine.modules.networktools.epanet.core.TimeParameterCodes;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.CalibrationTimeParameterCodes;
import org.jgrasstools.hortonmachine.modules.networktools.trento_p.utils.ProjectTimeParameterCodes;

@Description("The time related parameters for a TrentoP simulation")
@Author(name = "Daniele Andreis")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TrentoPParametersTime extends JGTModel {

    @Description(" Use mode, 0=project, 1=verify.")
    @In
    public short pTest;

    @Description("Time step to calculate the discharge in project mode.")
    @Unit("-")
    @Range(min = 0.015)
    @In
    public double tDTp = DEFAULT_TDTP;

    @Description("Minimum Rain Time step to calculate the discharge.")
    @Unit("-")
    @Range(min = 5)
    @In
    public double tpMin = DEFAULT_TPMIN;

    @Description("Maximum Rain Time step to calculate the discharge.")
    @Unit("-")
    @Range(min = 30)
    @In
    public double tpMax = DEFAULT_TMAX;

    @Description("Max number of time step.")
    @Unit("-")
    @In
    public double tMax = DEFAULT_TMAX;

    @Description("Time step, if pMode=1, in minutes. Is the step used to calculate the discharge. If it's not setted then it's equal to the rain time step.")
    @Unit("minutes")
    @In
    public Integer dt;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("Properties file containing the time options.")
    @In
    public String inFile = null;

    @Description("The Properties needed for trentop.")
    @Out
    public Properties outProperties = new Properties();

    private static final String MIN = " MIN"; //$NON-NLS-1$

    /**
     * The title of the time section in the inp file.
     */
    public static final String TIMESECTION = "[TIMES]"; //$NON-NLS-1$

    @Execute
    public void process() throws Exception {
        if (inFile != null) {
            File file = new File(inFile);
            outProperties.load(new FileReader(file));
        } else {
            if (pTest == 1) {
                if (dt != null) {
                    outProperties.put(CalibrationTimeParameterCodes.STEP.getKey(), dt + MIN);
                }
                outProperties.put(CalibrationTimeParameterCodes.MAXIMUM_TIME.getKey(), tMax);
            } else if (pTest == 0) {
                outProperties.put(ProjectTimeParameterCodes.STEP.getKey(), tDTp);
                outProperties.put(ProjectTimeParameterCodes.MINIMUM_TIME, tpMin);
                outProperties.put(CalibrationTimeParameterCodes.MAXIMUM_TIME.getKey(), tpMax);
            }

        }
    }

    /**
     * Create a {@link TrentoPParametersTime} from a {@link HashMap} of values.
     * 
     * @param options the {@link HashMap} of values. The keys have to be from {@link ProjectTimeParameterCodes} or {@link CalibrationTimeParameterCodes} .
     * @return the created {@link EpanetParametersTime}.
     * @throws Exception 
     */
    public static TrentoPParametersTime createFromMap( HashMap<TimeParameterCodes, String> options, int test ) throws Exception {
        TrentoPParametersTime trentoPTime = new TrentoPParametersTime();

        if (test == 1) {

            String step = options.get(CalibrationTimeParameterCodes.STEP);
            trentoPTime.dt = NumericsUtilities.isNumber(step, Integer.class);
            String maxT = options.get(CalibrationTimeParameterCodes.MAXIMUM_TIME);
            trentoPTime.tMax = NumericsUtilities.isNumber(maxT, Integer.class);

        } else if (test == 0) {

            String step = options.get(ProjectTimeParameterCodes.STEP);
            trentoPTime.tDTp = NumericsUtilities.isNumber(step, Double.class);
            String maxT = options.get(ProjectTimeParameterCodes.MAXIMUM_TIME);
            trentoPTime.tpMax = NumericsUtilities.isNumber(maxT, Double.class);
            String minT = options.get(ProjectTimeParameterCodes.MINIMUM_TIME);
            trentoPTime.tpMin = NumericsUtilities.isNumber(minT, Double.class);

        }

        trentoPTime.process();
        return trentoPTime;
    }
}
