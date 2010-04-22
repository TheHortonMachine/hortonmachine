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
import oms3.annotations.Status;
import oms3.io.CSTable;
import oms3.io.DataIO;
import oms3.io.TableIterator;

import org.jgrasstools.gears.libs.modules.JGTModel;

@Description("Utility class for reading the initial boundary conditions of the model adige from an OMS formatted csv file.")
@Author(name = "Andrea Antonello", contact = "www.hydrologis.com")
@Keywords("IO, Reading")
@Status(Status.DRAFT)
@License("http://www.gnu.org/licenses/gpl-3.0.html")
public class AdigeBoundaryConditionReader extends JGTModel {
    @Description("The csv file to read from.")
    @In
    public String file = null;

    @Description("The list initial boundary conditions.")
    @Out
    public List<AdigeBoundaryCondition> data;

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
        data = new ArrayList<AdigeBoundaryCondition>();
        while( rowsIterator.hasNext() ) {
            String[] row = rowsIterator.next();

            AdigeBoundaryCondition condition = new AdigeBoundaryCondition();
            int i = 1;
            condition.basinId = (int) Double.parseDouble(row[i++]);
            condition.discharge = Double.parseDouble(row[i++]);
            condition.dischargeSub = Double.parseDouble(row[i++]);
            condition.S1 = Double.parseDouble(row[i++]);
            condition.S2 = Double.parseDouble(row[i]);

            data.add(condition);
        }
    }

    @Finalize
    public void close() throws IOException {
        rowsIterator.close();
    }
}
