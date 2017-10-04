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
package org.hortonmachine.modules;

import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_AUTHORCONTACTS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_AUTHORNAMES;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_KEYWORDS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_LABEL;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_LICENSE;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_NAME;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_STATUS;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_defaultDtday_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_defaultDtmonth_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_defaultRh_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_defaultTolltmax_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_defaultTolltmin_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_defaultW_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_fBasinid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_fStationelev_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_fStationid_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_inAltimetry_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_inAreas_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_inInterpolate_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_inMeteo_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_inStations_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_outInterpolatedBand_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_outInterpolated_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_pBins_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_pHtmax_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_pHtmin_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_pNum_DESCRIPTION;
import static org.hortonmachine.hmachine.i18n.HortonMessages.OMSJAMI_pType_DESCRIPTION;

import java.util.HashMap;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.annotations.Unit;

import org.hortonmachine.gears.io.eicalculator.EIAltimetry;
import org.hortonmachine.gears.io.eicalculator.EIAreas;
import org.hortonmachine.gears.io.eicalculator.OmsEIAltimetryReader;
import org.hortonmachine.gears.io.eicalculator.OmsEIAreasReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorReader;
import org.hortonmachine.gears.io.timedependent.OmsTimeSeriesIteratorWriter;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.hmachine.modules.statistics.jami.OmsJami;

@Description(OMSJAMI_DESCRIPTION)
@Author(name = OMSJAMI_AUTHORNAMES, contact = OMSJAMI_AUTHORCONTACTS)
@Keywords(OMSJAMI_KEYWORDS)
@Label(OMSJAMI_LABEL)
@Name("_" + OMSJAMI_NAME)
@Status(OMSJAMI_STATUS)
@License(OMSJAMI_LICENSE)
public class Jami extends HMModel {

    @Description(OMSJAMI_inStations_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inStations;

    @Description(OMSJAMI_fStationid_DESCRIPTION)
    @In
    public String fStationid;

    @Description(OMSJAMI_fStationelev_DESCRIPTION)
    @In
    public String fStationelev;

    @Description(OMSJAMI_pBins_DESCRIPTION)
    @In
    public int pBins = 4;

    @Description(OMSJAMI_pNum_DESCRIPTION)
    @In
    public int pNum = 3;

    @Description(OMSJAMI_inInterpolate_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inInterpolate;

    @Description(OMSJAMI_fBasinid_DESCRIPTION)
    @In
    public String fBasinid;

    @Description(OMSJAMI_pType_DESCRIPTION)
    @In
    public int pType = -1;

    @Description(OMSJAMI_defaultRh_DESCRIPTION)
    @Unit("%")
    @In
    public double defaultRh = 70.0;

    @Description(OMSJAMI_defaultW_DESCRIPTION)
    @Unit("m/s")
    @In
    public double defaultW = 1.0;

    @Description(OMSJAMI_pHtmin_DESCRIPTION)
    @Unit("hours")
    @In
    public double pHtmin = 5.0;

    @Description(OMSJAMI_pHtmax_DESCRIPTION)
    @Unit("hours")
    @In
    public double pHtmax = 13.0;

    @Description(OMSJAMI_defaultDtday_DESCRIPTION)
    @Unit("celsius degrees")
    @In
    public double defaultDtday = 7.0;

    @Description(OMSJAMI_defaultDtmonth_DESCRIPTION)
    @Unit("celsius degrees")
    @In
    public double defaultDtmonth = 7.0;

    @Description(OMSJAMI_defaultTolltmin_DESCRIPTION)
    @Unit("hours")
    @In
    public double defaultTolltmin = 2.0;

    @Description(OMSJAMI_defaultTolltmax_DESCRIPTION)
    @Unit("hours")
    @In
    public double defaultTolltmax = 2.0;

    @Description(OMSJAMI_inAltimetry_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inAltimetry = null;

    @Description(OMSJAMI_inAreas_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inAreas = null;

    @Description(OMSJAMI_inMeteo_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inMeteo = null;

    @Description(OMSJAMI_outInterpolatedBand_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outInterpolatedBand = null;

    @Description(OMSJAMI_outInterpolated_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outInterpolated = null;

    @Execute
    public void process() throws Exception {

        OmsEIAltimetryReader altim = new OmsEIAltimetryReader();
        altim.file = inAltimetry;
        altim.pSeparator = ",";
        altim.pm = pm;
        altim.read();
        List<EIAltimetry> altimList = altim.outAltimetry;
        altim.close();

        OmsEIAreasReader areas = new OmsEIAreasReader();
        areas.file = inAreas;
        areas.pSeparator = ",";
        areas.pm = pm;
        areas.read();
        List<EIAreas> areasList = areas.outAreas;
        areas.close();

        OmsTimeSeriesIteratorReader dataReader = new OmsTimeSeriesIteratorReader();
        dataReader.file = inMeteo;
        dataReader.fileNovalue = "-9999";
        dataReader.idfield = fStationid;
        // dataReader.tStart = "2005-05-01 00:00";
        // dataReader.tTimestep = 60;
        // dataReader.tEnd = "2005-05-01 03:00";
        dataReader.initProcess();

        OmsJami omsjami = new OmsJami();
        omsjami.inStations = getVector(inStations);
        omsjami.fStationid = fStationid;
        omsjami.fStationelev = fStationelev;
        omsjami.pBins = pBins;
        omsjami.pNum = pNum;
        omsjami.inInterpolate = getVector(inInterpolate);
        omsjami.fBasinid = fBasinid;
        omsjami.pType = pType;
        omsjami.defaultRh = defaultRh;
        omsjami.defaultW = defaultW;
        omsjami.pHtmin = pHtmin;
        omsjami.pHtmax = pHtmax;
        omsjami.defaultDtday = defaultDtday;
        omsjami.defaultDtmonth = defaultDtmonth;
        omsjami.defaultTolltmin = defaultTolltmin;
        omsjami.defaultTolltmax = defaultTolltmax;
        omsjami.inAltimetry = altimList;
        omsjami.inAreas = areasList;
        omsjami.pm = pm;
        omsjami.doProcess = doProcess;
        omsjami.doReset = doReset;

        OmsTimeSeriesIteratorWriter writerInterpolated = null;
        OmsTimeSeriesIteratorWriter writerInterpolatedBand = null;

        pm.beginTask("Processing...", IHMProgressMonitor.UNKNOWN);
        while( dataReader.doProcess ) {
            dataReader.nextRecord();

            if (writerInterpolated == null && outInterpolated != null) {
                writerInterpolated = new OmsTimeSeriesIteratorWriter();
                writerInterpolated.file = outInterpolated;
                writerInterpolated.tStart = dataReader.tStart;
                writerInterpolated.tTimestep = dataReader.tTimestep;
            }
            if (writerInterpolatedBand == null && outInterpolatedBand != null) {
                writerInterpolatedBand = new OmsTimeSeriesIteratorWriter();
                writerInterpolatedBand.file = outInterpolatedBand;
                writerInterpolatedBand.tStart = dataReader.tStart;
                writerInterpolatedBand.tTimestep = dataReader.tTimestep;
            }
            pm.message("timestep: " + dataReader.tCurrent);

            omsjami.tCurrent = dataReader.tCurrent;
            HashMap<Integer, double[]> id2ValueMap = dataReader.outData;
            omsjami.inMeteo = id2ValueMap;
            omsjami.process();

            // write csv data
            writerInterpolated.inData = omsjami.outInterpolated;
            writerInterpolated.writeNextLine();

            writerInterpolatedBand.inData = omsjami.outInterpolatedBand;
            writerInterpolatedBand.writeNextLine();
        }
        pm.done();

        dataReader.close();
        if (writerInterpolated != null)
            writerInterpolated.close();
        if (writerInterpolatedBand != null)
            writerInterpolatedBand.close();
    }

}
