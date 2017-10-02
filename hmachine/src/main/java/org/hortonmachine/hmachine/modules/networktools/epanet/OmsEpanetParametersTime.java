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
package org.hortonmachine.hmachine.modules.networktools.epanet;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_duration_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_hydraulicTimestep_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_inFile_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_outProperties_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_patternStart_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_patternTimestep_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_reportStart_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_reportTimestep_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_startClockTime_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSEPANETPARAMETERSTIME_statistic_DESCRIPTION;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Properties;

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

import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.utils.math.NumericsUtilities;
import org.hortonmachine.hmachine.modules.networktools.epanet.core.TimeParameterCodes;

@Description(OMSEPANETPARAMETERSTIME_DESCRIPTION)
@Author(name = OMSEPANETPARAMETERSTIME_AUTHORNAMES, contact = OMSEPANETPARAMETERSTIME_AUTHORCONTACTS)
@Keywords(OMSEPANETPARAMETERSTIME_KEYWORDS)
@Label(OMSEPANETPARAMETERSTIME_LABEL)
@Name(OMSEPANETPARAMETERSTIME_NAME)
@Status(OMSEPANETPARAMETERSTIME_STATUS)
@License(OMSEPANETPARAMETERSTIME_LICENSE)
public class OmsEpanetParametersTime extends HMModel {

    @Description(OMSEPANETPARAMETERSTIME_duration_DESCRIPTION)
    @In
    public Double duration = null;

    @Description(OMSEPANETPARAMETERSTIME_hydraulicTimestep_DESCRIPTION)
    @In
    public Double hydraulicTimestep = null;

    @Description(OMSEPANETPARAMETERSTIME_patternTimestep_DESCRIPTION)
    @In
    public Double patternTimestep = null;

    @Description(OMSEPANETPARAMETERSTIME_patternStart_DESCRIPTION)
    @In
    public Double patternStart = null;

    @Description(OMSEPANETPARAMETERSTIME_reportTimestep_DESCRIPTION)
    @In
    public Double reportTimestep = null;

    @Description(OMSEPANETPARAMETERSTIME_reportStart_DESCRIPTION)
    @In
    public Double reportStart = null;

    @Description(OMSEPANETPARAMETERSTIME_startClockTime_DESCRIPTION)
    @In
    public String startClockTime = null;

    @Description(OMSEPANETPARAMETERSTIME_statistic_DESCRIPTION)
    @In
    public String statistic = null;

    @Description(OMSEPANETPARAMETERSTIME_inFile_DESCRIPTION)
    @In
    public String inFile = null;

    @Description(OMSEPANETPARAMETERSTIME_outProperties_DESCRIPTION)
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
     * Create a {@link OmsEpanetParametersTime} from a {@link HashMap} of values.
     * 
     * @param options the {@link HashMap} of values. The keys have to be from {@link TimeParameterCodes}.
     * @return the created {@link OmsEpanetParametersTime}.
     * @throws Exception 
     */
    public static OmsEpanetParametersTime createFromMap( HashMap<TimeParameterCodes, String> options ) throws Exception {
        OmsEpanetParametersTime epTime = new OmsEpanetParametersTime();
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
