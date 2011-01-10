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
package org.jgrasstools.gears.io.adige;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Out;
import oms3.annotations.Status;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.io.TableIterator;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("Utility class for reading data from a OMS formatted csv file assumed to be containing the Vegetation library data.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Reading")
@Label(JGTConstants.GENERICREADER)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class VegetationLibraryReader extends JGTModel {
    @Description("The csv file to read from.")
    @In
    public String file = null;

    @Description("The list of arrays representing the values in the rows.")
    @Out
    public HashMap<Integer, VegetationLibraryRecord> data;

    private TableIterator<String[]> rowsIterator;

    private CSTable table;

    private void ensureOpen() throws IOException {
        if (table == null) {
            table = DataIO.table(new File(file), null);
            rowsIterator = (TableIterator<String[]>) table.rows().iterator();
        }
    }

    @Execute
    public void read() throws IOException {
        if (!concatOr(data == null, doReset)) {
            return;
        }
        ensureOpen();
        data = new HashMap<Integer, VegetationLibraryRecord>();
        while( rowsIterator.hasNext() ) {
            String[] row = rowsIterator.next();

            int i = 1;
            int vegetationIndex = (int) Double.parseDouble(row[i++]);
            double architectural_resistance = Double.parseDouble(row[i++]);
            double min_stomatal_resistanc = Double.parseDouble(row[i++]);

            double[] laiMonths = new double[12];
            for( int j = 0; j < laiMonths.length; j++ ) {
                laiMonths[j] = Double.parseDouble(row[i++]);
            }
            double[] albedoMonths = new double[12];
            for( int j = 0; j < albedoMonths.length; j++ ) {
                albedoMonths[j] = Double.parseDouble(row[i++]);
            }
            double[] roughMonths = new double[12];
            for( int j = 0; j < roughMonths.length; j++ ) {
                roughMonths[j] = Double.parseDouble(row[i++]);
            }
            double[] displMonths = new double[12];
            for( int j = 0; j < displMonths.length; j++ ) {
                displMonths[j] = Double.parseDouble(row[i++]);
            }

            double wind_height = Double.parseDouble(row[i++]);
            double rgl = Double.parseDouble(row[i++]);
            double rad_atten = Double.parseDouble(row[i++]);
            double wind_atten = Double.parseDouble(row[i++]);
            double trunk_ratio = Double.parseDouble(row[i]);

            VegetationLibraryRecord vegetation = new VegetationLibraryRecord(vegetationIndex,
                    architectural_resistance, min_stomatal_resistanc, laiMonths, albedoMonths,
                    roughMonths, displMonths, wind_height, wind_atten, rgl, rad_atten, trunk_ratio);

            data.put(vegetationIndex, vegetation);
        }
    }

    @Finalize
    public void close() throws IOException {
        rowsIterator.close();
    }
}
