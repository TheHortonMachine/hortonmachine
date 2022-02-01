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

import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_FILE_NOVALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_ID_FIELD_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_NOVALUE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_OUT_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_P_AGGREGATION_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_P_NUM_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_T_CURRENT_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_T_END_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_T_PREVIOUS_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_T_START_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.OMSTIMESERIESITERATORREADER_T_TIMESTEP_DESCRIPTION;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;

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
import oms3.annotations.Name;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.io.TableIterator;

import org.hortonmachine.gears.libs.exceptions.ModelsIllegalargumentException;
import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description(OMSTIMESERIESITERATORREADER_DESCRIPTION)
@Author(name = OMSTIMESERIESITERATORREADER_AUTHORNAMES, contact = OMSTIMESERIESITERATORREADER_AUTHORCONTACTS)
@Keywords(OMSTIMESERIESITERATORREADER_KEYWORDS)
@Label(OMSTIMESERIESITERATORREADER_LABEL)
@Name(OMSTIMESERIESITERATORREADER_NAME)
@Status(OMSTIMESERIESITERATORREADER_STATUS)
@License(OMSTIMESERIESITERATORREADER_LICENSE)
public class OmsTimeSeriesIteratorReader extends HMModel {

    @Description(OMSTIMESERIESITERATORREADER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String file = null;

    @Description(OMSTIMESERIESITERATORREADER_ID_FIELD_DESCRIPTION)
    @In
    public String idfield = "ID";

    @Description(OMSTIMESERIESITERATORREADER_FILE_NOVALUE_DESCRIPTION)
    @In
    public String fileNovalue = "-9999.0";

    @Description(OMSTIMESERIESITERATORREADER_NOVALUE_DESCRIPTION)
    @In
    public double novalue = HMConstants.doubleNovalue;

    @Description(OMSTIMESERIESITERATORREADER_P_NUM_DESCRIPTION)
    @In
    public int pNum = 1;

    @Description(OMSTIMESERIESITERATORREADER_P_AGGREGATION_DESCRIPTION)
    @In
    public int pAggregation = 0;

    @Description(OMSTIMESERIESITERATORREADER_T_START_DESCRIPTION)
    @In
    @Out
    public String tStart;

    @Description(OMSTIMESERIESITERATORREADER_T_END_DESCRIPTION)
    @In
    @Out
    public String tEnd;

    @Description(OMSTIMESERIESITERATORREADER_T_TIMESTEP_DESCRIPTION)
    @In
    @Out
    public int tTimestep;

    @Description(OMSTIMESERIESITERATORREADER_T_CURRENT_DESCRIPTION)
    @Out
    public String tCurrent;

    @Description(OMSTIMESERIESITERATORREADER_T_PREVIOUS_DESCRIPTION)
    @Out
    public String tPrevious;

    @Description(OMSTIMESERIESITERATORREADER_OUT_DATA_DESCRIPTION)
    @Out
    public HashMap<Integer, double[]> outData;

    private TableIterator<String[]> rowsIterator;

    private CSTable table;

    private DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;

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
            /*
             * If tStart is null then the reader try to read all the value in the file, nb time step constant.
             */
            if (tStart == null) {
                String secondTime = null;
                // get the first time in the file.
                if (rowsIterator.hasNext()) {
                    String[] row = rowsIterator.next();
                    tStart = row[1];
                }
                // get the time of the second row in the file.
                if (rowsIterator.hasNext()) {
                    String[] row = rowsIterator.next();
                    secondTime = row[1];
                    // the dt is equal to the difference of the time of 2 rows.
                    tTimestep = formatter.parseDateTime(secondTime).getMinuteOfDay()
                            - formatter.parseDateTime(tStart).getMinuteOfDay();
                }
                // close and reopen to read the row.
                rowsIterator.close();
                rowsIterator = (TableIterator<String[]>) table.rows().iterator();
            }

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
        outData = new HashMap<Integer, double[]>();

        int columnCount = table.getColumnCount();
        List<Integer> idList = new ArrayList<Integer>();
        List<Integer> idCountList = new ArrayList<Integer>();
        int count = 0;
        Integer previousIdInteger = null;
        for( int i = 2; i <= columnCount; i++ ) {
            String id = table.getColumnInfo(i).get(idfield);
            try {
                Integer idInteger = Integer.valueOf(id);
                idList.add(idInteger);
                if (previousIdInteger == null) {
                    count++;
                } else {
                    if (idInteger.intValue() == previousIdInteger.intValue()) {
                        count++;
                    } else {
                        idCountList.add(count);
                        count = 1;
                    }
                }
                if (i == columnCount) {
                    idCountList.add(count);
                }
                previousIdInteger = idInteger;
            } catch (Exception e) {
                throw new ModelsIllegalargumentException("The id value doesn't seem to be an integer.", this.getClass()
                        .getSimpleName(), pm);
            }
        }

        if (rowsIterator.hasNext()) {
            String[] row = getExpectedRow(rowsIterator, expectedTimestamp);

            int idCountIndex = 0;
            for( int i = 2; i < row.length; i++ ) {
                Integer id = idList.get(i - 2);
                Integer idCount = idCountList.get(idCountIndex);
                double[] values = outData.get(id);
                if (values == null) {
                    values = new double[idCount];
                    outData.put(id, values);
                }
                for( int j = 0; j < idCount; j++, i++ ) {
                    if (row[i] == null || row[i].length() == 0) {
                        values[j] = novalue;
                    } else {
                        String valueStr = row[i].trim();
                        if (valueStr.equals(fileNovalue)) {
                            values[j] = novalue;
                        } else {
                            values[j] = Double.parseDouble(valueStr);
                        }
                    }
                }
                idCountIndex++;
                i--;
            }
        } else {
            outData = null;
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
                String message = "The data are not aligned with the simulation interval (" + currentTimestamp + "/" + expectedDT
                        + "). Check your data file: " + file;
                throw new IOException(message);
            }

        }
        return null;
    }

    @Finalize
    public void close() throws IOException {
        rowsIterator.close();
    }
}
