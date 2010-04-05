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
package eu.hydrologis.jgrass.hortonmachine.io.timeseries;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Role;
import oms3.annotations.Status;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.io.TableIterator;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

import eu.hydrologis.jgrass.hortonmachine.libs.models.HMConstants;
import eu.hydrologis.jgrass.hortonmachine.libs.models.HMModel;

@Description("Utility class for reading data from a OMS formatted csv file. The data is assumed to be first col a date and then al numbers.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Reading")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class TimeseriesReaderArray extends HMModel {
    @Description("The csv file to read from.")
    @In
    public String file = null;

    @Role(Role.PARAMETER)
    @Description("The file novalue to be translated into the internal novalue. Can be a string also")
    @In
    public String fileNovalue = "-9999.0";

    @Role(Role.PARAMETER)
    @Description("The internal novalue to use (usually not changed).")
    @In
    public double novalue = HMConstants.doubleNovalue;

    @Description("The list of timestamps read.")
    @Out
    public List<DateTime> timestamps;

    @Description("The list of arrays representing the values in the rows.")
    @Out
    public List<double[]> data;

    private TableIterator<String[]> rowsIterator;

    private CSTable table;

    private DateTimeFormatter formatter = HMConstants.utcDateFormatterYYYYMMDDHHMM;

    private void ensureOpen() throws IOException {
        if (table == null) {
            table = DataIO.table(new File(file), null);
            rowsIterator = (TableIterator<String[]>) table.rows().iterator();
            timestamps = new ArrayList<DateTime>();
        }
    }

    @Execute
    public void read() throws IOException {
        if (!concatOr(data == null, doReset)) {
            return;
        }
        ensureOpen();
        data = new ArrayList<double[]>();
        while( rowsIterator.hasNext() ) {
            String[] row = rowsIterator.next();
            timestamps.add(formatter.parseDateTime(row[1]));
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
            data.add(record);
        }
    }

    @Finalize
    public void close() throws IOException {
        rowsIterator.close();
    }
}
