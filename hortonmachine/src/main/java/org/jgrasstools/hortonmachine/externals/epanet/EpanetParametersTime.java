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
package org.jgrasstools.hortonmachine.externals.epanet;

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
import oms3.annotations.Status;

import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.DummyProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.utils.math.NumericsUtilities;
import org.jgrasstools.hortonmachine.externals.epanet.core.TimeParameterCodes;

@Description("The time related parameters of the epanet inp file")
@Author(name = "Andrea Antonello, Silvia Franceschi", contact = "www.hydrologis.com")
@Keywords("Epanet")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class EpanetParametersTime extends JGTModel {

    @Description("The duration of the simulation in minutes. Default is 0.")
    @In
    public Double duration = null;

    @Description("Defines how often a new hydraulic state of the network is computed. In minutes. Default is 60 minutes.")
    @In
    public Double hydraulicTimestep = null;

    @Description("The interval between time periods in all time patterns. Default is 60 minutes.")
    @In
    public Double patternTimestep = null;

    @Description("The time offset in minutes at which all patterns will start.")
    @In
    public Double patternStart = null;

    @Description("Sets the timestep interval of the report in minutes. Default is 60 minutes.")
    @In
    public Double reportTimestep = null;

    @Description("The time offset in minutes at which the report will start.")
    @In
    public Double reportStart = null;

    @Description("The time of the day at which the simulation begins. format is: [HH:MM AM/PM]")
    @In
    public String startClockTime = null;

    @Description("Kind of postprocessing that should be done on time series.")
    @In
    public String statistic = null;

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new DummyProgressMonitor();

    @Description("Properties file containing the time options.")
    @In
    public String inFile = null;

    @Description("The Properties needed for epanet.")
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
                outProperties.put(TimeParameterCodes.DURATION.getKey(), duration + MIN);
            } else {
                outProperties.put(TimeParameterCodes.DURATION.getKey(), 0 + MIN);
            }
            if (hydraulicTimestep != null) {
                outProperties.put(TimeParameterCodes.HYDSTEP.getKey(), hydraulicTimestep + MIN);
            }
            if (patternTimestep != null) {
                outProperties.put(TimeParameterCodes.PATTERNSTEP.getKey(), patternTimestep + MIN);
            }
            if (patternStart != null) {
                outProperties.put(TimeParameterCodes.PATTERNSTART.getKey(), patternStart + MIN);
            }
            if (reportTimestep != null) {
                outProperties.put(TimeParameterCodes.REPORTSTEP.getKey(), reportTimestep + MIN);
            }
            if (reportStart != null) {
                outProperties.put(TimeParameterCodes.REPORTSTART.getKey(), reportStart + MIN);
            }
            if (startClockTime != null) {
                outProperties.put(TimeParameterCodes.STARTCLOCKTIME.getKey(), startClockTime);
            }
            if (statistic != null) {
                outProperties.put(TimeParameterCodes.STATISTIC.getKey(), statistic);
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
    public static EpanetParametersTime createFromMap( HashMap<TimeParameterCodes, String> options ) throws Exception {
        EpanetParametersTime epTime = new EpanetParametersTime();
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
