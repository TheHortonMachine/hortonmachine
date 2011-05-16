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

import java.util.Properties;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;

@Description("The time related parameters for a TrentoP simulation")
@Author(name = "Daniele Andreis")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TrentoPParametersTime extends JGTModel {
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
            if (duration != null) {
                outProperties.put(TrentoPTimeParameterCodes.DURATION.getKey(), duration + MIN);
            } else {
                outProperties.put(TrentoPTimeParameterCodes.DURATION.getKey(), 0 + MIN);
            }
            if (hydraulicTimestep != null) {
                outProperties.put(TrentoPTimeParameterCodes.HYDSTEP.getKey(), hydraulicTimestep + MIN);
            }
            if (patternTimestep != null) {
                outProperties.put(TrentoPTimeParameterCodes.PATTERNSTEP.getKey(), patternTimestep + MIN);
            }
            if (patternStart != null) {
                outProperties.put(TrentoPTimeParameterCodes.PATTERNSTART.getKey(), patternStart + MIN);
            }
            if (reportTimestep != null) {
                outProperties.put(TrentoPTimeParameterCodes.REPORTSTEP.getKey(), reportTimestep + MIN);
            }
            if (reportStart != null) {
                outProperties.put(TrentoPTimeParameterCodes.REPORTSTART.getKey(), reportStart + MIN);
            }
            if (startClockTime != null) {
                outProperties.put(TrentoPTimeParameterCodes.STARTCLOCKTIME.getKey(), startClockTime);
            }
            if (statistic != null) {
                outProperties.put(TrentoPTimeParameterCodes.STATISTIC.getKey(), statistic);
            }
        }
    }

    /**
     * Create a {@link EpanetParametersTime} from a {@link HashMap} of values.
     * 
     * @param options the {@link HashMap} of values. The keys have to be from {@link TimeParameterCodes}.
     * @return the created {@link EpanetParametersTime}.
     * @throws Exception 
     */
    public static TrentoPParametersTime createFromMap( HashMap<TimeParameterCodes, String> options ) throws Exception {
        TrentoPParametersTime epTime = new TrentoPParametersTime();
        String duration = options.get(TimeParameterCodes.DURATION);
        epTime.duration = NumericsUtilities.isNumber(duration, Double.class);
        String hydrTiStep = options.get(TimeParameterCodes.HYDSTEP);
        epTime.hydraulicTimestep = NumericsUtilities.isNumber(hydrTiStep, Double.class);
        String pattTimeStep = options.get(TimeParameterCodes.PATTERNSTEP);
        epTime.patternTimestep = NumericsUtilities.isNumber(pattTimeStep, Double.class);
        String patternStart = options.get(TimeParameterCodes.PATTERNSTART);
        epTime.patternStart = NumericsUtilities.isNumber(patternStart, Double.class);
        String reportTimeStep = options.get(TimeParameterCodes.REPORTSTEP);
        epTime.reportTimestep = NumericsUtilities.isNumber(reportTimeStep, Double.class);
        String reportStart = options.get(TimeParameterCodes.REPORTSTART);
        epTime.reportStart = NumericsUtilities.isNumber(reportStart, Double.class);
        String startClockTime = options.get(TimeParameterCodes.STARTCLOCKTIME);
        epTime.startClockTime = startClockTime;
        String statistic = options.get(TimeParameterCodes.STATISTIC);
        epTime.statistic = statistic;
        epTime.process();
        return epTime;
    }
}
