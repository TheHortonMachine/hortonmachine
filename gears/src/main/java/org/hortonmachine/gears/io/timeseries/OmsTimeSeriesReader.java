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

import static org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader.OMSTIMESERIESREADER_AUTHORCONTACTS;
import static org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader.OMSTIMESERIESREADER_AUTHORNAMES;
import static org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader.OMSTIMESERIESREADER_DESCRIPTION;
import static org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader.OMSTIMESERIESREADER_DOCUMENTATION;
import static org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader.OMSTIMESERIESREADER_KEYWORDS;
import static org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader.OMSTIMESERIESREADER_LABEL;
import static org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader.OMSTIMESERIESREADER_LICENSE;
import static org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader.OMSTIMESERIESREADER_NAME;
import static org.hortonmachine.gears.io.timeseries.OmsTimeSeriesReader.OMSTIMESERIESREADER_STATUS;
import static org.hortonmachine.gears.libs.modules.HMConstants.HASHMAP_READER;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

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
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.io.TableIterator;

@Description(OMSTIMESERIESREADER_DESCRIPTION)
@Documentation(OMSTIMESERIESREADER_DOCUMENTATION)
@Author(name = OMSTIMESERIESREADER_AUTHORNAMES, contact = OMSTIMESERIESREADER_AUTHORCONTACTS)
@Keywords(OMSTIMESERIESREADER_KEYWORDS)
@Label(OMSTIMESERIESREADER_LABEL)
@Name(OMSTIMESERIESREADER_NAME)
@Status(OMSTIMESERIESREADER_STATUS)
@License(OMSTIMESERIESREADER_LICENSE)
public class OmsTimeSeriesReader extends HMModel {

    @Description(OMSTIMESERIESREADER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String file = null;

    @Description(OMSTIMESERIESREADER_FILE_NOVALUE_DESCRIPTION)
    @In
    public String fileNovalue = "-9999.0";

    @Description(OMSTIMESERIESREADER_NOVALUE_DESCRIPTION)
    @In
    public double novalue = HMConstants.doubleNovalue;

    @Description(OMSTIMESERIESREADER_OUT_DATA_DESCRIPTION)
    @Out
    public HashMap<DateTime, double[]> outData;

    public static final String OMSTIMESERIESREADER_DESCRIPTION = "Utility class for reading data from a OMS formatted csv file. The data is assumed to be first col a date and then al numbers.";
    public static final String OMSTIMESERIESREADER_DOCUMENTATION = "OmsTimeSeriesReader.html";
    public static final String OMSTIMESERIESREADER_KEYWORDS = "IO, Reading";
    public static final String OMSTIMESERIESREADER_LABEL = HASHMAP_READER;
    public static final String OMSTIMESERIESREADER_NAME = "tsreader";
    public static final int OMSTIMESERIESREADER_STATUS = 40;
    public static final String OMSTIMESERIESREADER_LICENSE = "General Public License Version 3 (GPLv3)";
    public static final String OMSTIMESERIESREADER_AUTHORNAMES = "Andrea Antonello and Silvia Franceschi";
    public static final String OMSTIMESERIESREADER_AUTHORCONTACTS = "http://www.hydrologis.com";
    public static final String OMSTIMESERIESREADER_FILE_DESCRIPTION = "The csv file to read from.";
    public static final String OMSTIMESERIESREADER_FILE_NOVALUE_DESCRIPTION = "The file novalue to be translated into the internal novalue (defaults to -9999.0). Can be also a string.";
    public static final String OMSTIMESERIESREADER_NOVALUE_DESCRIPTION = "The internal novalue to use (defaults to NaN).";
    public static final String OMSTIMESERIESREADER_OUT_DATA_DESCRIPTION = "The sorted hashmap of read data.";

    private TableIterator<String[]> rowsIterator;

    private CSTable table;

    private DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;

    private void ensureOpen() throws IOException {
        if (table == null) {
            table = DataIO.table(new File(file), null);
            rowsIterator = (TableIterator<String[]>) table.rows().iterator();
            outData = new LinkedHashMap<DateTime, double[]>();
        }
    }

    @Execute
    public void read() throws IOException {
        ensureOpen();
        while( rowsIterator.hasNext() ) {
            String[] row = rowsIterator.next();
            double[] record = new double[row.length - 2];
            for( int i = 2; i < row.length; i++ ) {
                double value = -1;
                if (row[i] == null || row[i].length() == 0) {
                    value = novalue;
                } else {
                    String valueStr = row[i];
                    if (valueStr.trim().equals(fileNovalue)) {
                        value = novalue;
                    } else {
                        value = Double.parseDouble(valueStr);
                    }
                }
                record[i - 2] = value;
            }

            outData.put(formatter.parseDateTime(row[1]), record);
        }
    }

    @Finalize
    public void close() throws IOException {
        rowsIterator.close();
    }
}
