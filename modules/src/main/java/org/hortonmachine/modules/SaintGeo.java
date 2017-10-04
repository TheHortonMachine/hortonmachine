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

import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.AUTHORCONTACTS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.AUTHORNAMES;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.KEYWORDS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.LABEL;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.LICENSE;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.NAME;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.STATUS;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inConfluenceId2DischargeMap_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inDischarge_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inDownstreamLevel_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inLateralId2DischargeMap_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inRiver_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inSectionPoints_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.inSections_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.outputDischargeFile_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.outputLevelFile_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.pDeltaTMillis_DESCRIPTION;
import static org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo.pDeltaTMillis_UNIT;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.io.TableIterator;

import org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.hmachine.modules.hydrogeomorphology.saintgeo.OmsSaintGeo;
import org.joda.time.DateTime;

@Description(DESCRIPTION)
@Author(name = AUTHORNAMES, contact = AUTHORCONTACTS)
@Keywords(KEYWORDS)
@Label(LABEL)
@Name(NAME)
@Status(STATUS)
@License(LICENSE)
public class SaintGeo extends HMModel {
    @Description(inRiver_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inRiverPoints = null;

    @Description(inSections_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inSections = null;

    @Description(inSectionPoints_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_VECTOR)
    @In
    public String inSectionPoints = null;

    @Description(inDischarge_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inDischarge;

    @Description(inDownstreamLevel_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inDownstreamLevel;

    @Description(inLateralId2DischargeMap_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inLateralId2Discharge;

    @Description(inConfluenceId2DischargeMap_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String inConfluenceId2Discharge;

    @Description(pDeltaTMillis_DESCRIPTION)
    @Unit(pDeltaTMillis_UNIT)
    @In
    public long pDeltaTMillis = 5000;

    @Description(outputLevelFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String outputLevelFile;

    @Description(outputDischargeFile_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
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
        
        if (inLateralId2Discharge!=null) {
            HashMap<Integer, double[]> lateralData = readIdData(inLateralId2Discharge);
            saintGeo.inLateralId2DischargeMap = lateralData;
        }
        if (inConfluenceId2Discharge!=null) {
            HashMap<Integer, double[]> confluenceData = readIdData(inConfluenceId2Discharge);
            saintGeo.inConfluenceId2DischargeMap = confluenceData;
        }

        saintGeo.pDeltaTMillis = pDeltaTMillis;
        saintGeo.outputLevelFile = outputLevelFile;
        saintGeo.outputDischargeFile = outputDischargeFile;
        saintGeo.process();
    }

    private HashMap<Integer, double[]> readIdData( String path ) throws Exception {
        CSTable table = DataIO.table(new File(path), null);
        HashMap<Integer, List<Double>> dataMap = new HashMap<>();
        int columnCount = table.getColumnCount();
        int[] ids = new int[columnCount - 1]; // minus type and timestamp
        for( int i = 2; i <= columnCount; i++ ) {
            Map<String, String> columnInfo = table.getColumnInfo(i);
            String idStr = columnInfo.get("id");
            int id = Integer.parseInt(idStr);
            ids[i - 2] = id;
        }
        TableIterator<String[]> rowsIterator = (TableIterator<String[]>) table.rows().iterator();
        while( rowsIterator.hasNext() ) {
            String[] row = rowsIterator.next();
            for( int i = 2; i < row.length; i++ ) {
                List<Double> dataList = dataMap.get(ids[i - 2]);
                if (dataList == null) {
                    dataList = new ArrayList<>();
                    dataMap.put(ids[i - 2], dataList);
                }
                double value = -1;
                if (row[i] == null || row[i].length() == 0) {
                    value = HMConstants.doubleNovalue;
                } else {
                    String valueStr = row[i];
                    value = Double.parseDouble(valueStr);
                }
                dataList.add(value);
            }
        }

        HashMap<Integer, double[]> outDataMap = new HashMap<>();
        for( Entry<Integer, List<Double>> entry : dataMap.entrySet() ) {
            Integer id = entry.getKey();
            List<Double> valueList = entry.getValue();
            double[] values = new double[valueList.size()];
            for( int i = 0; i < values.length; i++ ) {
                values[i] = valueList.get(i);
            }
            outDataMap.put(id, values);
        }

        return outDataMap;
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
        String inLateral = base + "q_lateral_offtakes.csv";
        String outLevelFile = base + "saintgeo_level_out_offtakes.csv";
        String outDischargeFile = base + "saintgeo_discharge_out_offtakes.csv";

        SaintGeo sg = new SaintGeo();
        sg.inRiverPoints = inRiv;
        sg.inSectionPoints = inSecP;
        sg.inSections = inSec;
        sg.inDischarge = inDischarge;
        sg.inDownstreamLevel = inLevel;
        sg.inLateralId2Discharge = inLateral;

        sg.pDeltaTMillis = 5000;

        sg.outputLevelFile = outLevelFile;
        sg.outputDischargeFile = outDischargeFile;

        sg.process();

    }

}