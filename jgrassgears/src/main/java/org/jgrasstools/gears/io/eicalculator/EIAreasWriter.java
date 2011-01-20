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
package org.jgrasstools.gears.io.eicalculator;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

import oms3.annotations.Author;
import oms3.annotations.Label;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Role;
import oms3.annotations.Status;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.jgrasstools.gears.libs.modules.JGTModel;
import org.jgrasstools.gears.libs.monitor.LogProgressMonitor;
import org.jgrasstools.gears.libs.monitor.IJGTProgressMonitor;

@Description("Utility class for writing area data (for EICalculator) to csv files.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Writing")
@Label(JGTConstants.GENERICWRITER)
@Status(Status.CERTIFIED)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class EIAreasWriter extends JGTModel {
    @Description("The data to write.")
    @In
    public List<EIAreas> inAreas;

    @Description("The csv file to write to.")
    @Label(JGTConstants.FILE_LABEL)
    @In
    public String file = null;

    @Role(Role.PARAMETER)
    @Description("The csv separator.")
    @In
    public String pSeparator = ",";

    @Description("The progress monitor.")
    @In
    public IJGTProgressMonitor pm = new LogProgressMonitor();

    private BufferedWriter csvWriter;

    private void ensureOpen() throws IOException {
        if (csvWriter == null)
            csvWriter = new BufferedWriter(new FileWriter(file));
    }

    @Finalize
    public void close() throws IOException {
        csvWriter.close();
    }

    @Execute
    public void write() throws IOException {
        ensureOpen();

        csvWriter.write("# EIAreas writer output\n");
        for( EIAreas areas : inAreas ) {
            StringBuilder sb = new StringBuilder();
            sb.append(areas.basinId);
            sb.append(pSeparator);
            sb.append(areas.altimetricBandId);
            sb.append(pSeparator);
            sb.append(areas.energyBandId);
            sb.append(pSeparator);
            sb.append(areas.areaValue);
            sb.append("\n");
            csvWriter.write(sb.toString());
        }
    }
}
