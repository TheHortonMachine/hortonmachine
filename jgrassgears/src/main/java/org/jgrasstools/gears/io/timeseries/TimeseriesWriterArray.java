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
package org.jgrasstools.gears.io.timeseries;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import oms3.annotations.Author;
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
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormatter;

@Description("Utility class for writing a set of timestamps and an array of values to an OMS formatted csv file.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Writing")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TimeseriesWriterArray {
    @Description("The csv file to write to.")
    @In
    public String file = null;

    @Description("The table name.")
    @In
    public String tablename = "table";

    @Description("The list of timestamps to write.")
    @In
    public List<DateTime> timestamps;

    @Description("The list of arrays of values to write.")
    @In
    public List<double[]> data;

    @Description("A switch that defines whether to write the timestamps as dates or as intervals of seconds if a date doesn't make sense.")
    @In
    public boolean doDates = true;

    @Description("The comma separated list of column names.")
    @In
    public String columns = null;

    @Description("A map of lists of metadata that can be attached to the column of the csv file.")
    @In
    public HashMap<String, List<String>> metadata = null;

    private MemoryTable memoryTable;

    private DateTimeFormatter formatter = JGTConstants.utcDateFormatterYYYYMMDDHHMM;
    private String formatterPattern = JGTConstants.utcDateFormatterYYYYMMDDHHMM_string;

    private void ensureOpen() throws IOException {
        if (memoryTable == null) {
            memoryTable = new MemoryTable();
            memoryTable.setName(tablename);
            memoryTable.getInfo().put("Created", new DateTime().toString(formatter));
            memoryTable.getInfo().put("Author", "HortonMachine library");
        }
    }

    @Execute
    public void write() throws IOException {
        ensureOpen();
        
        int cols = data.get(0).length + 1;
        if (columns != null) {
            String[] colNames = columns.split(",");
            for( int i = 0; i < colNames.length; i++ ) {
                colNames[i] = colNames[i].trim();
            }
            memoryTable.setColumns(colNames);
        } else {
            String[] colNames = new String[cols];
            colNames[0] = "date";
            for( int i = 1; i < colNames.length; i++ ) {
                colNames[i] = "value_" + i;
            }
            memoryTable.setColumns(colNames);
        }

        if (metadata != null && metadata.size() > 0) {
            Set<String> metadataNames = metadata.keySet();
            for( String metadataName : metadataNames ) {
                List<String> metadataList = metadata.get(metadataName);
                for( int i = 0; i < metadataList.size(); i++ ) {
                    memoryTable.getColumnInfo(i + 1).put(metadataName, metadataList.get(i));
                }
            }
        }
        if (doDates) {
            // add date metadata if they are not already provided
            boolean hasFormat = false;
            boolean hasType = false;
            if (metadata != null && metadata.size() > 0) {
                List<String> list = metadata.get("Format");
                if (list != null)
                    hasFormat = true;
                list = metadata.get("Type");
                if (list != null)
                    hasType = true;
            }
            if (!hasFormat) {
                memoryTable.getColumnInfo(1).put("Format", formatterPattern);
                for( int i = 2; i <= cols; i++ ) {
                    memoryTable.getColumnInfo(i).put("Format", "");
                }
            }
            if (!hasType) {
                memoryTable.getColumnInfo(1).put("Type", "Date");
                for( int i = 2; i <= cols; i++ ) {
                    memoryTable.getColumnInfo(i).put("Type", "");
                }
            }
        }

        DateTime firstDate = timestamps.get(0);
        for( int i = 0; i < timestamps.size(); i++ ) {
            Object[] valuesRow = new Object[cols];

            DateTime dateTime = timestamps.get(i);
            if (doDates) {
                valuesRow[0] = dateTime.toString(formatter);
            } else {
                Interval interval = new Interval(firstDate, dateTime);
                long dt = interval.toDuration().getStandardSeconds();
                valuesRow[0] = dt;
            }

            double[] valuesArray = data.get(i);
            for( int j = 0; j < valuesArray.length; j++ ) {
                valuesRow[j + 1] = valuesArray[j];
            }
            memoryTable.addRow(valuesRow);
        }
    }
    @Finalize
    public void close() throws IOException {
        DataIO.print(memoryTable, new PrintWriter(new File(file)));
    }
}
