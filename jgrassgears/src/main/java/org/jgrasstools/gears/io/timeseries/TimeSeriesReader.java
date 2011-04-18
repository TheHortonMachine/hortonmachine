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
import java.util.LinkedHashMap;

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

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description("Utility class for reading data from a OMS formatted csv file. The data is assumed to be first col a date and then al numbers.")
@Documentation("TimeSeriesReader.html")
@Author(name = "Andrea Antonello", contact = "http://www.hydrologis.com")
@Keywords("IO, Reading")
@Label(JGTConstants.HASHMAP_READER)
@Name("tsreader")
@Status(Status.CERTIFIED)
@License("General Public License Version 3 (GPLv3)")
public class TimeSeriesReader extends JGTModel {
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

    @Description("The hashmap of read data.")
    @Out
    public LinkedHashMap<DateTime, double[]> outData;

    private TableIterator<String[]> rowsIterator;

    private CSTable table;

    private DateTimeFormatter formatter = JGTConstants.utcDateFormatterYYYYMMDDHHMM;

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
