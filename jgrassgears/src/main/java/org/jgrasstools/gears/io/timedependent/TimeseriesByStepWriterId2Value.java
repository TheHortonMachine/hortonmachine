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
package org.jgrasstools.gears.io.timedependent;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Set;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Status;
import oms3.io.DataIO;
import oms3.io.MemoryTable;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description("Utility class for writing a id2value map to a OMS formatted csv file.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Writing")
@Label(JGTConstants.GENERICWRITER)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TimeseriesByStepWriterId2Value {
    @Description("The csv file to write to.")
    @Label("file")
    @In
    public String file = null;

    @Description("The table name.")
    @In
    public String tablename = "table";

    @Description("The map of ids and values to write.")
    @In
    public HashMap<Integer, double[]> data;

    @Description("The start date. If available time is added as first column")
    @In
    public String tStart;

    @Description("The timestep. If available time is added as first column")
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

    private Set<Integer> idsSet;

    private void ensureOpen() throws IOException {
        if (memoryTable == null) {
            memoryTable = new MemoryTable();
            memoryTable.setName(tablename);
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
        if (!columnNamesAreSet) {
            idsSet = data.keySet();

            // column names
            String[] columnNames = null;
            int index = 0;
            if (runningDateTime == null) {
                columnNames = new String[idsSet.size()];
            } else {
                columnNames = new String[idsSet.size() + 1];
                columnNames[0] = "timestamp";
                index = 1;
            }

            for( Integer id : idsSet ) {
                columnNames[index++] = "value_" + id;
            }
            memoryTable.setColumns(columnNames);

            // ids
            index = 0;
            if (runningDateTime != null) {
                memoryTable.getColumnInfo(1).put("ID", "");
                index = 1;
            }
            int k = 0;
            for( Integer id : idsSet ) {
                memoryTable.getColumnInfo(k + 1 + index).put("ID", String.valueOf(id));
                k++;
            }

            // data types
            index = 0;
            if (runningDateTime != null) {
                memoryTable.getColumnInfo(1).put("Type", "Date");
                index = 1;
            }
            for( int j = 0; j < idsSet.size(); j++ ) {
                memoryTable.getColumnInfo(j + 1 + index).put("Type", "Double");
            }

            // data formats
            index = 0;
            if (runningDateTime != null) {
                memoryTable.getColumnInfo(1).put("Format", formatterPattern);
                index = 1;
            }
            for( int j = 0; j < idsSet.size(); j++ ) {
                memoryTable.getColumnInfo(j + 1 + index).put("Format", "");
            }

        }

        Object[] valuesRow = null;
        int index = 0;
        if (runningDateTime != null) {
            valuesRow = new Object[idsSet.size() + 1];
            valuesRow[0] = runningDateTime.toString(formatter);
            index = 1;
        } else {
            valuesRow = new Object[idsSet.size()];
        }
        for( Integer id : idsSet ) {
            double[] dataArray = data.get(id);
            double value = dataArray[0];
            if(JGTConstants.isNovalue(value)){
                valuesRow[index++] = fileNovalue;
            }else{
                valuesRow[index++] = String.valueOf(value);
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
