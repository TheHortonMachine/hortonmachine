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
package org.jgrasstools.gears.io.timedependent;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Documentation;
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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description("Utility class for writing a id2values map to a OMS formatted csv file.")
@Documentation("TimeSeriesIteratorWriter.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, Writing")
@Label(JGTConstants.HASHMAP_WRITER)
@Name("tsitwriter")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class TimeSeriesIteratorWriter {
    @Description("The csv file to write to.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;

    @Description("The table name.")
    @In
    public String inTablename = "table";

    @Description("The hashmap of ids and values to write.")
    @In
    public HashMap<Integer, double[]> inData;

    @Description("The start date. If available time is added as first column.")
    @In
    public String tStart;

    @Description("The timestep. If available time is added as first column.")
    @In
    public int tTimestep = -1;

    @Description("The novalue to use in the file (default is -9999.0).")
    @In
    public String fileNovalue = "-9999.0";

    private MemoryTable memoryTable;

    private DateTimeFormatter formatter = JGTConstants.utcDateFormatterYYYYMMDDHHMM;
    private String formatterPattern = JGTConstants.utcDateFormatterYYYYMMDDHHMM_string;

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
                if (JGTConstants.isNovalue(value)) {
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
        DataIO.print(memoryTable, new File(file));
    }
}
