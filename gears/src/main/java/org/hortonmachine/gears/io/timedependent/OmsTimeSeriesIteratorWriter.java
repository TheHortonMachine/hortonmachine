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
package org.hortonmachine.gears.io.timedependent;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_FILE_NOVALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_IN_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_IN_TABLENAME_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_T_START_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORWRITER_T_TIMESTEP_DESCRIPTION;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Name;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.io.DataIO;
import oms3.io.MemoryTable;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description(OMSTIMESERIESITERATORWRITER_DESCRIPTION)
@Author(name = OMSTIMESERIESITERATORWRITER_AUTHORNAMES, contact = OMSTIMESERIESITERATORWRITER_AUTHORCONTACTS)
@Keywords(OMSTIMESERIESITERATORWRITER_KEYWORDS)
@Label(OMSTIMESERIESITERATORWRITER_LABEL)
@Name(OMSTIMESERIESITERATORWRITER_NAME)
@Status(OMSTIMESERIESITERATORWRITER_STATUS)
@License(OMSTIMESERIESITERATORWRITER_LICENSE)
public class OmsTimeSeriesIteratorWriter extends HMModel {

    @Description(OMSTIMESERIESITERATORWRITER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;

    @Description(OMSTIMESERIESITERATORWRITER_IN_TABLENAME_DESCRIPTION)
    @In
    public String inTablename = "table";

    @Description(OMSTIMESERIESITERATORWRITER_IN_DATA_DESCRIPTION)
    @In
    public HashMap<Integer, double[]> inData;

    @Description(OMSTIMESERIESITERATORWRITER_T_START_DESCRIPTION)
    @In
    public String tStart;

    @Description(OMSTIMESERIESITERATORWRITER_T_TIMESTEP_DESCRIPTION)
    @In
    public int tTimestep = -1;

    @Description(OMSTIMESERIESITERATORWRITER_FILE_NOVALUE_DESCRIPTION)
    @In
    public String fileNovalue = "-9999.0";

    private MemoryTable memoryTable;

    private DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;
    private String formatterPattern = HMConstants.utcDateFormatterYYYYMMDDHHMM_string;

    private DateTime runningDateTime;

    private boolean columnNamesAreSet = false;

    private void ensureOpen() throws IOException {
        if (memoryTable == null) {
            memoryTable = new MemoryTable();
            memoryTable.setName(inTablename);
            memoryTable.getInfo().put("Created", new DateTime().toString(formatter));
            memoryTable.getInfo().put("Author", "HortonMachine library");

            if (tStart != null && tTimestep != -1) {
                // add time column
                runningDateTime = formatter.parseDateTime(tStart);
            }
        }
    }

    @Execute
    public void writeNextLine() throws IOException {
        ensureOpen();
        List<Integer> idsList = new ArrayList<Integer>();
        List<String> columnNamesList = new ArrayList<String>();
        List<Integer> uniqueIdsList = new ArrayList<Integer>();
        if (!columnNamesAreSet) {
            Set<Entry<Integer, double[]>> inDataSet = inData.entrySet();
            for( Entry<Integer, double[]> inDataEntry : inDataSet ) {
                Integer id = inDataEntry.getKey();
                double[] values = inDataEntry.getValue();

                if (values.length == 1) {
                    StringBuilder sb = new StringBuilder();
                    sb.append("value_");
                    sb.append(id);
                    columnNamesList.add(sb.toString());
                    idsList.add(id);
                } else {
                    for( int i = 0; i < values.length; i++ ) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("value_");
                        sb.append(id);
                        sb.append("_");
                        sb.append(i);
                        columnNamesList.add(sb.toString());
                        idsList.add(id);
                    }
                }
            }

            for( Integer tmpId : idsList ) {
                if (!uniqueIdsList.contains(tmpId)) {
                    uniqueIdsList.add(tmpId);
                }
            }

            // column names
            int index = 0;
            if (runningDateTime != null) {
                columnNamesList.add(0, "timestamp");
                index = 1;
            }

            String[] columnNames = columnNamesList.toArray(new String[0]);
            memoryTable.setColumns(columnNames);

            // ids
            index = 0;
            if (runningDateTime != null) {
                memoryTable.getColumnInfo(1).put("ID", "");
                index = 1;
            }
            int k = 0;
            for( Integer id : idsList ) {
                memoryTable.getColumnInfo(k + 1 + index).put("ID", String.valueOf(id));
                k++;
            }

            // data types
            index = 0;
            if (runningDateTime != null) {
                memoryTable.getColumnInfo(1).put("Type", "Date");
                index = 1;
            }
            for( int j = 0; j < idsList.size(); j++ ) {
                memoryTable.getColumnInfo(j + 1 + index).put("Type", "Double");
            }

            // data formats
            index = 0;
            if (runningDateTime != null) {
                memoryTable.getColumnInfo(1).put("Format", formatterPattern);
                index = 1;
            }
            for( int j = 0; j < idsList.size(); j++ ) {
                memoryTable.getColumnInfo(j + 1 + index).put("Format", "");
            }

        }

        Object[] valuesRow = null;
        int index = 0;
        if (runningDateTime != null) {
            valuesRow = new Object[columnNamesList.size()];
            valuesRow[0] = runningDateTime.toString(formatter);
            index = 1;
        } else {
            valuesRow = new Object[columnNamesList.size() - 1];
        }
        for( Integer id : uniqueIdsList ) {
            double[] dataArray = inData.get(id);
            for( double value : dataArray ) {
                if (HMConstants.isNovalue(value)) {
                    valuesRow[index++] = fileNovalue;
                } else {
                    valuesRow[index++] = String.valueOf(value);
                }
            }
        }
        memoryTable.addRow(valuesRow);

        if (runningDateTime != null) {
            runningDateTime = runningDateTime.plusMinutes(tTimestep);
        }
    }

    @Finalize
    public void close() throws IOException {
        DataIO.print(memoryTable, new PrintWriter(new File(file)));
    }
}
