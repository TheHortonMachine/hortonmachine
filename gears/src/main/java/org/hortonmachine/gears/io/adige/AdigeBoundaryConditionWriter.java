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
package org.hortonmachine.gears.io.adige;

import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_AUTHORCONTACTS;
import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_AUTHORNAMES;
import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_DATA_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_FILE_DESCRIPTION;
import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_KEYWORDS;
import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_LABEL;
import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_LICENSE;
import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_NAME;
import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_STATUS;
import static org.hortonmachine.gears.i18n.GearsMessages.ADIGEBOUNDARYCONDITIONWRITER_TABLENAME_DESCRIPTION;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
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
import oms3.annotations.Status;
import oms3.annotations.UI;
import oms3.io.DataIO;
import oms3.io.MemoryTable;

import org.hortonmachine.gears.libs.modules.HMConstants;
import org.hortonmachine.gears.libs.modules.HMModel;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;

@Description(ADIGEBOUNDARYCONDITIONWRITER_DESCRIPTION)
@Author(name = ADIGEBOUNDARYCONDITIONWRITER_AUTHORNAMES, contact = ADIGEBOUNDARYCONDITIONWRITER_AUTHORCONTACTS)
@Keywords(ADIGEBOUNDARYCONDITIONWRITER_KEYWORDS)
@Label(ADIGEBOUNDARYCONDITIONWRITER_LABEL)
@Name(ADIGEBOUNDARYCONDITIONWRITER_NAME)
@Status(ADIGEBOUNDARYCONDITIONWRITER_STATUS)
@License(ADIGEBOUNDARYCONDITIONWRITER_LICENSE)
public class AdigeBoundaryConditionWriter extends HMModel {

    @Description(ADIGEBOUNDARYCONDITIONWRITER_FILE_DESCRIPTION)
    @UI(HMConstants.FILEIN_UI_HINT_CSV)
    @In
    public String file = null;

    @Description(ADIGEBOUNDARYCONDITIONWRITER_TABLENAME_DESCRIPTION)
    @In
    public String tablename = "table";

    @Description(ADIGEBOUNDARYCONDITIONWRITER_DATA_DESCRIPTION)
    @In
    public Map<Integer, AdigeBoundaryCondition> data;

    private MemoryTable memoryTable;

    private DateTimeFormatter formatter = HMConstants.dateTimeFormatterYYYYMMDDHHMM;

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
            valuesRow[0] = condition.getBasinId();
            valuesRow[1] = condition.getDischarge();
            valuesRow[2] = condition.getDischargeSub();
            valuesRow[3] = condition.getS1();
            valuesRow[4] = condition.getS2();
            memoryTable.addRow(valuesRow);
        }
    }

    @Finalize
    public void close() throws IOException {
        DataIO.print(memoryTable, new PrintWriter(new File(file)));
    }
}
