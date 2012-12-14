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
package org.jgrasstools.gears.io.timeseries;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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

import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormatter;

@Description("Utility class for writing a set of timestamps and an array of values to an OMS formatted csv file.")
@Documentation("TimeSeriesWriter.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, Writing")
@Label(JGTConstants.HASHMAP_WRITER)
@Name("tswriter")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class TimeSeriesWriter {
    @Description("The csv file to write to.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;

    @Description("The table name.")
    @In
    public String tablename = "table";

    @Description("The hashmap of data to write. IMPORTANT: The hashmap is assumed to be sorted.")
    @In
    public HashMap<DateTime, double[]> inData;

    @Description("A switch that defines whether to write the timestamps as dates or as intervals of seconds if a date doesn't make sense.")
    @In
    public boolean doDates = true;

    @Description("The comma separated list of column names.")
    @In
    public String columns = null;

    @Description("A list of lists of metadata that can be attached to the column of the csv file.")
    @In
    public List<List<String>> inMetadata = null;

    private MemoryTable memoryTable;

    private DateTimeFormatter formatter = JGTConstants.utcDateFormatterYYYYMMDDHHMM;
    private String formatterPattern = JGTConstants.utcDateFormatterYYYYMMDDHHMM_string;

    private void ensureOpen() throws IOException {
        if (memoryTable == null) {
            memoryTable = new MemoryTable();
            memoryTable.setName(tablename);
            memoryTable.getInfo().put("Created", new DateTime().toString(formatter));
            memoryTable.getInfo().put("Author", "JGrasstools");
        }
    }

    @Execute
    public void write() throws IOException {
        ensureOpen();

        Set<Entry<DateTime, double[]>> entrySet = inData.entrySet();
        if (entrySet.isEmpty()) {
            throw new ModelsIllegalargumentException("The data to write are empty.", this);
        }
        Entry<DateTime, double[]> firstItem = entrySet.iterator().next();

        int cols = firstItem.getValue().length + 1;
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

        if (inMetadata != null && inMetadata.size() > 0) {
            for( List<String> metadataRecord : inMetadata ) {
                String metadataName = metadataRecord.get(0);
                for( int i = 1; i < metadataRecord.size(); i++ ) {
                    memoryTable.getColumnInfo(i).put(metadataName, metadataRecord.get(i - 1));
                }
            }
        }
        if (doDates) {
            // add date metadata if they are not already provided
            boolean hasFormat = false;
            boolean hasType = false;
            if (inMetadata != null && inMetadata.size() > 0) {
                hasFormat = false;
                for( List<String> metadataRecord : inMetadata ) {
                    if (metadataRecord.contains("Format")) {
                        hasFormat = true;
                        break;
                    }
                }
                hasType = false;
                for( List<String> metadataRecord : inMetadata ) {
                    if (metadataRecord.contains("Type")) {
                        hasType = true;
                        break;
                    }
                }
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

        for( Entry<DateTime, double[]> entry : entrySet ) {
            Object[] valuesRow = new Object[cols];

            DateTime dateTime = entry.getKey();
            if (doDates) {
                valuesRow[0] = dateTime.toString(formatter);
            } else {
                Interval interval = new Interval(firstItem.getKey(), dateTime);
                long dt = interval.toDuration().getStandardSeconds();
                valuesRow[0] = dt;
            }

            double[] valuesArray = entry.getValue();
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
