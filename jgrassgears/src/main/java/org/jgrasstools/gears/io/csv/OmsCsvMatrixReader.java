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
package org.jgrasstools.gears.io.csv;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.io.TableIterator;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

@Description("Utility class for reading data from a OMS formatted csv file to a double matrix (dates are saved as longs).")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, Reading, csv")
@Label(JGTConstants.MATRIXREADER)
@Name("csvmatrixreader")
@UI(JGTConstants.HIDE_UI_HINT)
@Status(Status.EXPERIMENTAL)
@License("General Public License Version 3 (GPLv3)")
public class OmsCsvMatrixReader extends JGTModel {
    @Description("The csv file to read from.")
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description("The file novalue to be translated into the internal novalue (defaults to -9999.0). Can be also a string.")
    @In
    public String fileNovalue = "-9999.0";

    @Description("The internal novalue to use (defaults to NaN).")
    @In
    public double novalue = JGTConstants.doubleNovalue;

    @Description("The matrix of read data.")
    @Out
    public double[][] outData;

    @Description("The data title.")
    @Out
    public String outTitle;

    @Description("The data subtitle.")
    @Out
    public String outSubTitle;

    @Description("The data series names.")
    @Out
    public String[] outIds;

    @Description("The data labels or null.")
    @Out
    public String[] outLabels;

    @Description("The data formats (dates and numeric formatting patterns) or null.")
    @Out
    public String[] outFormats;

    @Description("The data types (dates or numerics like double, int) or null.")
    @Out
    public String[] outTypes;

    private TableIterator<String[]> rowsIterator;

    private CSTable table;

    private int columnCount;

    private List<String> outIdsList = new ArrayList<String>();
    private List<String> outLabelsList = new ArrayList<String>();
    private List<String> outFormatsList = new ArrayList<String>();
    private List<String> outTypesList = new ArrayList<String>();

    private List<double[]> outDataList = new ArrayList<double[]>();

    private DateTimeFormatter dateFormatter;

    private void ensureOpen() throws IOException {
        if (table == null) {
            table = DataIO.table(new File(file), null);

            outTitle = table.getName();
            Map<String, String> info = table.getInfo();
            Set<Entry<String, String>> entrySet = info.entrySet();
            for( Entry<String, String> entry : entrySet ) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key.toLowerCase().equals("subtitle")) {
                    outSubTitle = value;
                }
            }

            columnCount = table.getColumnCount();

            for( int i = 1; i <= columnCount; i++ ) {
                String columnName = table.getColumnName(i);
                if (i > 1)
                    outIdsList.add(columnName);

                Map<String, String> columnInfo = table.getColumnInfo(i);
                Set<Entry<String, String>> entrySet1 = columnInfo.entrySet();
                for( Entry<String, String> entry : entrySet1 ) {
                    String key = entry.getKey();
                    String value = entry.getValue();

                    if (key.toLowerCase().equals("label")) {
                        outLabelsList.add(value);
                    }
                    if (key.toLowerCase().equals("format")) {
                        outFormatsList.add(value);
                    }
                    if (key.toLowerCase().equals("type")) {
                        value = value.toLowerCase().trim();
                        if (value.length() == 0) {
                            value = "double";
                        }
                        outTypesList.add(value);
                    }
                }
            }

            if (outIdsList.size() > 0) {
                outIds = outIdsList.toArray(new String[0]);
            }
            if (outLabelsList.size() > 0) {
                outLabels = outLabelsList.toArray(new String[0]);
            }
            if (outFormatsList.size() > 0) {
                outFormats = outFormatsList.toArray(new String[0]);
            }

            if (outTypesList.size() == 0) {
                for( int i = 1; i <= columnCount; i++ ) {
                    outTypesList.add("double");
                }
            }
            outTypes = outTypesList.toArray(new String[0]);

            rowsIterator = (TableIterator<String[]>) table.rows().iterator();
        }
    }

    @Execute
    public void read() throws IOException {
        ensureOpen();
        while( rowsIterator.hasNext() ) {
            String[] row = rowsIterator.next();
            double[] record = new double[columnCount];
            for( int i = 1; i <= columnCount; i++ ) {
                if (i == 0 && outTypes[i].equals("date") && outFormats.length > i) {
                    if (dateFormatter == null)
                        dateFormatter = DateTimeFormat.forPattern(outFormats[i]);

                    DateTime dateTime = dateFormatter.parseDateTime(row[i]);
                    record[i - 1] = dateTime.getMillis();
                } else {
                    double value = Double.parseDouble(row[i]);
                    record[i - 1] = value;
                }
            }
            outDataList.add(record);
        }

        outData = new double[outDataList.size()][];
        int index = 0;
        for( double[] record : outDataList ) {
            outData[index++] = record;
        }
    }
    @Finalize
    public void close() throws IOException {
        rowsIterator.close();
    }

}
