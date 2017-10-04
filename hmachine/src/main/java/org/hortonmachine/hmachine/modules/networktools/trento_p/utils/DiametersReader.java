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
package org.hortonmachine.hmachine.modules.networktools.trento_p.utils;
import static org.hortonmachine.gears.libs.modules.HMConstants.doubleNovalue;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.hortonmachine.gears.libs.monitor.IHMProgressMonitor;
import org.hortonmachine.gears.libs.monitor.LogProgressMonitor;

import oms3.annotations.Author;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.Label;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Role;
import oms3.annotations.Status;
import oms3.annotations.UI;
@Description("Utility class for reading data from csv file that have the form: id1 value1[] id2 value2[] ... idn valuen[].")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Reading")
@Label(HMConstants.HASHMAP_READER)
@UI(HMConstants.HIDE_UI_HINT)
@Status(Status.EXPERIMENTAL)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class DiametersReader extends HMModel {
    @Description("The csv file to read from.")
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String file = null;

    @Role(Role.PARAMETER)
    @Description("The number of columns of the array.")
    @In
    public int pCols = -1;

    @Role(Role.PARAMETER)
    @Description("The csv separator.")
    @In
    public String pSeparator = ",";

    @Role(Role.PARAMETER)
    @Description("The file novalue.")
    @In
    public String fileNovalue = "-9999.0";

    @Description("The progress monitor.")
    @In
    public IHMProgressMonitor pm = new LogProgressMonitor();

    @Description("The read List values arrays.")
    @Out
    public List<double[]> data;

    private BufferedReader csvReader;

    private void ensureOpen() throws IOException {
        if (csvReader == null)
            csvReader = new BufferedReader(new FileReader(file));
    }

    @Execute
    public void readFile() throws IOException {
        ensureOpen();
        data = new ArrayList<double[]>();
        String line = null;
        while( (line = csvReader.readLine()) != null ) {
            String[] lineSplit = line.trim().split(pSeparator);
            double[] values = new double[pCols];
            for( int j = 0; j < pCols; j++ ) {
                double value = Double.parseDouble(lineSplit[j]);
                if (fileNovalue != null) {
                    if (lineSplit[j].equals(fileNovalue)) {
                        // set to internal novalue
                        value = doubleNovalue;
                    }
                }
                values[j] = value;
            }
            data.add(values);

        }

    }

    @Finalize
    public void close() throws IOException {
        csvReader.close();
    }
}
