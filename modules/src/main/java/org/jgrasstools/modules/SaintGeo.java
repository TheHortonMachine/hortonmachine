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
package org.jgrasstools.modules;

import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.AUTHORCONTACTS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.AUTHORNAMES;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.KEYWORDS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.LABEL;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.LICENSE;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.NAME;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.STATUS;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inConfluenceId2DischargeMap_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inDischarge_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inDownstreamLevel_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inLateralId2DischargeMap_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inRiver_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inSectionPoints_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inSections_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.outputLevelFile_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.outputDischargeFile_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.pDeltaTMillis_DESCRIPTION;
import static org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.pDeltaTMillis_UNIT;

import java.io.IOException;
import java.util.HashMap;

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

import org.jgrasstools.gears.io.timeseries.OmsTimeSeriesReader;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.hortonmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo;
import org.joda.time.DateTime;

@Description(DESCRIPTION)
@Author(name = AUTHORNAMES, contact = AUTHORCONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class SaintGeo extends JGTModel {
    @Description(inRiver_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inRiverPoints = null;

    @Description(inSections_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inSections = null;

    @Description(inSectionPoints_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inSectionPoints = null;

    @Description(inDischarge_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inDischarge;

    @Description(inDownstreamLevel_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String inDownstreamLevel;

    @Description(inLateralId2DischargeMap_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inLateralId2DischargeMap;

    @Description(inConfluenceId2DischargeMap_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inConfluenceId2DischargeMap;

    @Description(pDeltaTMillis_DESCRIPTION)
    @Unit(pDeltaTMillis_UNIT)
    @In
    public long pDeltaTMillis = 5000;

    @Description(outputLevelFile_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outputLevelFile;
    
    @Description(outputDischargeFile_DESCRIPTION)
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String outputDischargeFile;

    @Execute
    public void process() throws Exception {
        checkNull(inRiverPoints, inSectionPoints, inSections, inDischarge);

        OmsSaintGeo saintGeo = new OmsSaintGeo();
        saintGeo.inRiverPoints = getVector(inRiverPoints);
        saintGeo.inSectionPoints = getVector(inSectionPoints);
        saintGeo.inSections = getVector(inSections);

        double[] discharge = readToArray(inDischarge);
        saintGeo.inDischarge = discharge;

        if (inDownstreamLevel != null) {
            double[] level = readToArray(inDownstreamLevel);
            saintGeo.inDownstreamLevel = level;
        }

        saintGeo.pDeltaTMillis = pDeltaTMillis;
        saintGeo.outputLevelFile = outputLevelFile;
        saintGeo.outputDischargeFile = outputDischargeFile;
        saintGeo.process();
    }

    private double[] readToArray( String file ) throws IOException {
        OmsTimeSeriesReader reader = new OmsTimeSeriesReader();
        reader.file = file;
        reader.fileNovalue = "-9999";
        reader.read();
        HashMap<DateTime, double[]> outData = reader.outData;
        double[] data = new double[outData.size()];
        int counter = 0;
        for( double[] values : outData.values() ) {
            data[counter++] = values[0];
        }
        return data;
    }

    public static void main( String[] args ) throws Exception {
        String base = "D:/Dropbox/hydrologis/lavori/2015_phd_bz/gSoC2015/data/data_adige_test/";

        String inSec = base + "sections_adige_75_rev.shp";
        String inSecP = base + "sectionpoints_adige_75_rev.shp";
        String inRiv = base + "riverpoints_adige_75_rev.shp";
        String inDischarge = base + "head_discharge.csv";
        String inLevel = base + "downstream_waterlevel.csv";
        String outLevelFile = base + "saintgeo_level_out.csv";
        String outDischargeFile = base + "saintgeo_discharge_out.csv";

        SaintGeo sg = new SaintGeo();
        sg.inRiverPoints = inRiv;
        sg.inSectionPoints = inSecP;
        sg.inSections = inSec;
        sg.inDischarge = inDischarge;
        sg.inDownstreamLevel = inLevel;

        sg.pDeltaTMillis = 5000;

        sg.outputLevelFile = outLevelFile;
        sg.outputDischargeFile = outDischargeFile;

        sg.process();

    }

}