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
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Set;
import java.util.Map.Entry;

import oms3.annotations.Author;
import oms3.annotations.Category;
import oms3.annotations.Description;
import oms3.annotations.Execute;
import oms3.annotations.Finalize;
import oms3.annotations.In;
import oms3.annotations.Keywords;
import oms3.annotations.License;
import oms3.annotations.Status;
import oms3.io.DataIO;
import oms3.io.MemoryTable;

import org.jgrasstools.gears.libs.modules.JGTConstants;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description("Utility class for writing the final boundary conditions of the model adige to an OMS formatted csv file.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Writing")
@Status(Status.CERTIFIED)
@Category(JGTConstants.GENERICWRITER)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class AdigeBoundaryConditionWriter {
    @Description("The csv file to write to.")
    @In
    public String file = null;

    @Description("The table name.")
    @In
    public String tablename = "table";

    @Description("The list of boundary conditions to write.")
    @In
    public HashMap<Integer, AdigeBoundaryCondition> data;

    private MemoryTable memoryTable;

    private DateTimeFormatter formatter = JGTConstants.dateTimeFormatterYYYYMMDDHHMM;

    private void ensureOpen() throws IOException {
        if (memoryTable == null) {
            memoryTable = new MemoryTable();
            memoryTable.setName(tablename);
            memoryTable.getInfo().put("Created", new DateTime().toString(formatter));
        }
    }

    @Execute
    public void write() throws IOException {
        ensureOpen();

        String[] colNames = new String[]{"basinid", "discharge", "sub-discharge", "S1", "S2"};
        memoryTable.setColumns(colNames);

        Set<Entry<Integer, AdigeBoundaryCondition>> entrySet = data.entrySet();
        for( Entry<Integer, AdigeBoundaryCondition> entry : entrySet ) {
            AdigeBoundaryCondition condition = entry.getValue();
            Object[] valuesRow = new Object[colNames.length];
            valuesRow[0] = condition.basinId;
            valuesRow[1] = condition.discharge;
            valuesRow[2] = condition.dischargeSub;
            valuesRow[3] = condition.S1;
            valuesRow[4] = condition.S2;
            memoryTable.addRow(valuesRow);
        }
    }

    @Finalize
    public void close() throws IOException {
        DataIO.print(memoryTable, new PrintWriter(new File(file)));
    }
}
