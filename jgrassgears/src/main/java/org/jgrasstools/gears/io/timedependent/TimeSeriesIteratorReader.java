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

import static org.jgrasstools.gears.libs.modules.JGTConstants.doubleNovalue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Initialize;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.io.TableIterator;

import org.jgrasstools.gears.libs.exceptions.ModelsIllegalargumentException;
import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description("Utility class for reading data from a OMS formatted csv file. The file needs a metadata line containing the id of the station. The table is supposed to have a first column of timestamp and all olther columns of data related to the ids defined.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Reading")
@Label(JGTConstants.HASHMAP_READER)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TimeSeriesIteratorReader extends JGTModel {
    @Description("The csv file to read from.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description("The id metadata field.")
    @In
    public String idfield = "ID";

    @Description("The file novalue to be translated into the internal novalue. Can be a string also")
    @In
    public String fileNovalue = "-9999.0";

    @Description("The internal novalue to use (usually not changed).")
    @In
    public double novalue = JGTConstants.doubleNovalue;

    @Description("The number of rows to aggregate (default is 1, i.e. no aggregation).")
    @In
    public int pNum = 1;

    @Description("The aggregation type to use (0 = sum, 1 = avg).")
    @In
    public int pAggregation = 0;

    @Description("The time at which start to read (format: yyyy-MM-dd HH:mm ).")
    @In
    @Out
    public String tStart;

    @Description("The time at which end to read (format: yyyy-MM-dd HH:mm ).")
    @In
    @Out
    public String tEnd;

    @Description("The reading timestep in minutes.")
    @In
    @Out
    public int tTimestep;

    @Description("The current time read (format: yyyy-MM-dd HH:mm ).")
    @Out
    public String tCurrent;

    @Description("The previous time read (format: yyyy-MM-dd HH:mm ).")
    @Out
    public String tPrevious;

    @Description("The read map of ids and values.")
    @Out
    public HashMap<Integer, double[]> data;

    private TableIterator<String[]> rowsIterator;

    private CSTable table;

    private DateTimeFormatter formatter = JGTConstants.utcDateFormatterYYYYMMDDHHMM;

    private DateTime expectedTimestamp = null;

    @Initialize
    public void initProcess() {
        // activate time
        doProcess = true;
    }

    private void ensureOpen() throws IOException {
        if (table == null) {
            table = DataIO.table(new File(file), null);
            rowsIterator = (TableIterator<String[]>) table.rows().iterator();
        }
    }

    @Execute
    public void nextRecord() throws IOException {
        ensureOpen();
        if (tCurrent == null) {
            tPrevious = null;
            tCurrent = tStart.trim();
            expectedTimestamp = formatter.parseDateTime(tCurrent);
        } else {
            tPrevious = tCurrent;
            expectedTimestamp = expectedTimestamp.plusMinutes(tTimestep);
            tCurrent = expectedTimestamp.toString(formatter);
        }
        data = new HashMap<Integer, double[]>();

        int columnCount = table.getColumnCount();
        List<Integer> idList = new ArrayList<Integer>();
        for( int i = 2; i <= columnCount; i++ ) {
            String id = table.getColumnInfo(i).get(idfield);
            try {
                Integer idInteger = new Integer(id);
                idList.add(idInteger);
            } catch (Exception e) {
                throw new ModelsIllegalargumentException("The id value doesn't seem to be an integer.", this.getClass()
                        .getSimpleName());
            }
        }

        if (rowsIterator.hasNext()) {
            String[] row = getExpectedRow(rowsIterator, expectedTimestamp);

            for( int i = 2; i < row.length; i++ ) {
                Integer id = idList.get(i - 2);
                double[] value = new double[1];
                if (row[i] == null || row[i].length() == 0) {
                    value[0] = novalue;
                } else {
                    String valueStr = row[i].trim();
                    if (valueStr.equals(fileNovalue)) {
                        value[0] = novalue;
                    } else {
                        value[0] = Double.parseDouble(valueStr);
                    }
                }
                data.put(id, value);
            }
        } else {
            data = null;
        }

        // time ran out
        if (tEnd != null && tCurrent.equals(tEnd)) {
            doProcess = false;
        }
        // data ran out
        if (!rowsIterator.hasNext()) {
            doProcess = false;
        }
    }

    /**
     * Get the needed datarow from the table.
     * 
     * @param tableRowIterator
     * @return the row that is aligned with the expected timestep.
     * @throws IOException if the expected timestep is < than the current.
     */
    private String[] getExpectedRow( TableIterator<String[]> tableRowIterator, DateTime expectedDT ) throws IOException {
        while( tableRowIterator.hasNext() ) {
            String[] row = tableRowIterator.next();
            DateTime currentTimestamp = formatter.parseDateTime(row[1]);
            if (currentTimestamp.equals(expectedDT)) {
                if (pNum == 1) {
                    return row;
                } else {
                    String[][] allRows = new String[pNum][];
                    allRows[0] = row;
                    int rowNum = 1;
                    for( int i = 1; i < pNum; i++ ) {
                        if (tableRowIterator.hasNext()) {
                            String[] nextRow = tableRowIterator.next();
                            allRows[i] = nextRow;
                            rowNum++;
                        }
                    }
                    // now aggregate
                    String[] aggregatedRow = new String[row.length];
                    // date is the one of the first instant
                    aggregatedRow[0] = allRows[0][0];
                    aggregatedRow[1] = allRows[0][1];
                    for( int col = 2; col < allRows[0].length; col++ ) {

                        boolean hasOne = false;
                        switch( pAggregation ) {
                        case 0:
                            double sum = 0;
                            for( int j = 0; j < rowNum; j++ ) {
                                String valueStr = allRows[j][col];
                                if (!valueStr.equals(fileNovalue)) {
                                    double value = Double.parseDouble(valueStr);
                                    sum = sum + value;
                                    hasOne = true;
                                }
                            }
                            if (!hasOne) {
                                sum = doubleNovalue;
                            }
                            aggregatedRow[col] = String.valueOf(sum);
                            break;
                        case 1:
                            double avg = 0;
                            for( int j = 0; j < rowNum; j++ ) {
                                String valueStr = allRows[j][col];
                                if (!valueStr.equals(fileNovalue)) {
                                    double value = Double.parseDouble(valueStr);
                                    avg = avg + value;
                                    hasOne = true;
                                }
                            }
                            if (!hasOne) {
                                avg = doubleNovalue;
                            } else {
                                avg = avg / pNum;
                            }
                            aggregatedRow[col] = String.valueOf(avg);
                            break;

                        default:
                            break;
                        }

                    }
                    return aggregatedRow;
                }
            } else if (currentTimestamp.isBefore(expectedDT)) {
                // browse until the instant is found
                continue;
            } else if (currentTimestamp.isAfter(expectedDT)) {
                /*
                 * lost the moment, for now throw exception.
                 * Could be enhanced in future.
                 */
                throw new IOException("The data are not aligned with the simulation interval. Check your data file: " + file);
            }

        }
        return null;
    }

    @Finalize
    public void close() throws IOException {
        rowsIterator.close();
    }
}
