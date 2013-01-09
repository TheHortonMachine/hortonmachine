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

import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_AUTHORCONTACTS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_AUTHORNAMES;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_DOCUMENTATION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_KEYWORDS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_LABEL;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_LICENSE;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_NAME;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_STATUS;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_fileNovalue_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_file_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_novalue_DESCRIPTION;
import static org.jgrasstools.gears.i18n.GearsMessages.OMSTIMESERIESREADER_outData_DESCRIPTION;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
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

@Description(OMSTIMESERIESREADER_DESCRIPTION)
@Documentation(OMSTIMESERIESREADER_DOCUMENTATION)
@Author(name = OMSTIMESERIESREADER_AUTHORNAMES, contact = OMSTIMESERIESREADER_AUTHORCONTACTS)
@Keywords(OMSTIMESERIESREADER_KEYWORDS)
@Label(OMSTIMESERIESREADER_LABEL)
@Name(OMSTIMESERIESREADER_NAME)
@Status(OMSTIMESERIESREADER_STATUS)
@License(OMSTIMESERIESREADER_LICENSE)
public class OmsTimeSeriesReader extends JGTModel {

    @Description(OMSTIMESERIESREADER_file_DESCRIPTION)
    @UI(JGTConstants.FILEIN_UI_HINT)
    @In
    public String file = null;

    @Description(OMSTIMESERIESREADER_fileNovalue_DESCRIPTION)
    @In
    public String fileNovalue = "-9999.0";

    @Description(OMSTIMESERIESREADER_novalue_DESCRIPTION)
    @In
    public double novalue = JGTConstants.doubleNovalue;

    @Description(OMSTIMESERIESREADER_outData_DESCRIPTION)
    @Out
    public HashMap<DateTime, double[]> outData;

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
