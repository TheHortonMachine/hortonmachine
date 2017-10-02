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
package org.hortonmachine.gears.io.timeseries;

import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_COLUMNS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_DOCUMENTATION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_DO_DATES_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_IN_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_IN_META_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESWRITER_TABLE_NAME_DESCRIPTION;

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

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;
import org.joda.time.Interval;
import org.joda.time.format.DateTimeFormatter;

@Description(OMSTIMESERIESWRITER_DESCRIPTION)
@Documentation(OMSTIMESERIESWRITER_DOCUMENTATION)
@Author(name = OMSTIMESERIESWRITER_AUTHORNAMES, contact = OMSTIMESERIESWRITER_AUTHORCONTACTS)
@Keywords(OMSTIMESERIESWRITER_KEYWORDS)
@Label(OMSTIMESERIESWRITER_LABEL)
@Name(OMSTIMESERIESWRITER_NAME)
@Status(OMSTIMESERIESWRITER_STATUS)
@License(OMSTIMESERIESWRITER_LICENSE)
public class OmsTimeSeriesWriter extends HMModel {

    @Description(OMSTIMESERIESWRITER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;

    @Description(OMSTIMESERIESWRITER_TABLE_NAME_DESCRIPTION)
    @In
    public String tablename = "table";

    @Description(OMSTIMESERIESWRITER_IN_DATA_DESCRIPTION)
    @In
    public HashMap<DateTime, double[]> inData;

    @Description(OMSTIMESERIESWRITER_DO_DATES_DESCRIPTION)
    @In
    public boolean doDates = true;

    @Description(OMSTIMESERIESWRITER_COLUMNS_DESCRIPTION)
    @In
    public String columns = null;

    @Description(OMSTIMESERIESWRITER_IN_META_DATA_DESCRIPTION)
    @In
    public List<List<String>> inMetadata = null;

    private MemoryTable memoryTable;

    private DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;
    private String formatterPattern = HMConstants.utcDateFormatterYYYYMMDDHHMM_string;

    private void ensureOpen() throws IOException {
        if (memoryTable == null) {
            memoryTable = new MemoryTable();
            memoryTable.setName(tablename);
            memoryTable.getInfo().put("Created", new DateTime().toString(formatter));
            memoryTable.getInfo().put("Author", "HortonMachine");
        }
    }

    @Execute
    public void write() throws IOException {
        ensureOpen();

        Set<Entry<DateTime, double[]>> entrySet = inData.entrySet();
        if (entrySet.isEmpty()) {
            throw new ModelsIllegalargumentException("The data to write are empty.", this, pm);
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
