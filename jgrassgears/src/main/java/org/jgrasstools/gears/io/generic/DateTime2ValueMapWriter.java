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
package org.jgrasstools.gears.io.generic;

import static org.jgrasstools.gears.libs.modules.JGTConstants.isNovalue;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
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
import oms3.annotations.Status;
import oms3.annotations.UI;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.joda.time.DateTime;

@Description("Utility class for writing data to csv file that have the form: time1 value1[] time2 value2[] ... timen valuen[].")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Writing")
@Label(JGTConstants.HASHMAP_WRITER)
@UI(JGTConstants.HIDE_UI_HINT)
@Status(Status.EXPERIMENTAL)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class DateTime2ValueMapWriter {
    @Description("The csv file to write to.")
    @UI(JGTConstants.FILEOUT_UI_HINT)
    @In
    public String file = null;

    @Description("The csv separator.")
    @In
    public String pSeparator = ",";

    @Description("The file novalue.")
    @In
    public String fileNovalue = "-9999.0";

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    @Description("The map of ids and values arrays to write.")
    @In
    public HashMap<DateTime, double[]> data;

    private BufferedWriter csvWriter;

    private void ensureOpen() throws IOException {
        if (csvWriter == null)
            csvWriter = new BufferedWriter(new FileWriter(file));
    }

    private double novalue = -9999.0;

    @Execute
    public void writeNextLine() throws IOException {
        ensureOpen();

        novalue = Double.parseDouble(fileNovalue);

        Set<Entry<DateTime, double[]>> entrySet = data.entrySet();
        for( Entry<DateTime, double[]> entry : entrySet ) {
            DateTime id = entry.getKey();
            double[] values = entry.getValue();

            csvWriter.write(id.toString(JGTConstants.dateTimeFormatterYYYYMMDDHHMMSS));
            for( int i = 0; i < values.length; i++ ) {
                csvWriter.write(pSeparator);
                double value = values[i];
                if (isNovalue(value)) {
                    value = novalue;
                }
                csvWriter.write(String.valueOf(value));
            }
            csvWriter.write("\n");
        }
    }

    @Finalize
    public void close() throws IOException {
        csvWriter.close();
    }
}
